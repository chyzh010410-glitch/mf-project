param(
  [string]$ApiBase = "http://127.0.0.1:8091",
  [string]$MfEpBase = "http://127.0.0.1:8080",
  [string]$MfEpInternalToken,
  [string]$AgentBase = "http://127.0.0.1:8092",
  [int]$AckTimeoutSeconds = 90,
  [switch]$ConfirmRun
)

$ErrorActionPreference = "Stop"
if (-not $ConfirmRun) { throw "This script creates and publishes a controlled FAQ. Pass -ConfirmRun to continue." }
if ([string]::IsNullOrWhiteSpace($MfEpInternalToken)) { throw "Pass -MfEpInternalToken explicitly; do not store the internal token in this script." }

$base = "${ApiBase}/api/ai/knowledge"
$stamp = Get-Date -Format "yyyyMMddHHmmss"
$topic = "datacenter-closed-loop-audit-$stamp"

function Invoke-DataCenterPost {
  param([string]$Path, $Body)
  Invoke-RestMethod -Method Post -Uri "$base$Path" -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 5)
}

function Wait-EventAck {
  param([long]$CandidateId, [string]$Action)
  $deadline = (Get-Date).AddSeconds($AckTimeoutSeconds)
  do {
    Start-Sleep -Seconds 5
    $logs = (Invoke-RestMethod -Uri "${base}/candidates/${CandidateId}/sync-logs").data
    $log = $logs | Where-Object { $_.action -eq $Action } | Select-Object -First 1
    if ($log -and $log.deliveryStatus -eq "acknowledged") { return $log }
  } while ((Get-Date) -lt $deadline)
  throw "$Action event was not acknowledged by AgentService within $AckTimeoutSeconds seconds."
}

function Get-MfEpEvent {
  param([long]$EventId)
  $response = Invoke-RestMethod -Uri "${MfEpBase}/internal/ai-content/sync-events/${EventId}" -Headers @{ "X-MF-Internal-Token" = $MfEpInternalToken }
  if ($response.code -ne 200 -or $null -eq $response.data) { throw "MF_EP event ${EventId} query failed: $($response.msg)" }
  return $response.data
}

function Assert-AgentRetrieval {
  param([string]$Message, [string]$ExpectedTitle, [bool]$ShouldHit)
  $response = Invoke-RestMethod -Method Post -Uri "${AgentBase}/api/agent/chat" -ContentType "application/json" -Body (@{ sessionId = "datacenter-closed-loop-$stamp"; message = $Message; userId = "datacenter-audit"; userType = "system" } | ConvertTo-Json)
  $hit = @($response.sources | Where-Object { $_.title -like "*$ExpectedTitle*" }).Count -gt 0
  if ($ShouldHit -and -not $hit) { throw "AgentService did not retrieve the published controlled content." }
  if (-not $ShouldHit -and $hit) { throw "AgentService still retrieved the offline controlled content." }
  return $response
}

$gap = (Invoke-DataCenterPost "/gaps" @{ topic = $topic; sampleQuestion = "How does the controlled knowledge closed-loop audit work?"; occurrenceCount = 1; lowScoreCount = 0; riskLevel = "low" }).data
$candidate = (Invoke-DataCenterPost "/gaps/$($gap.id)/candidates" @{ contentType = "faq"; title = $topic; content = "This low-risk controlled FAQ exists only for the DataCenter closed-loop acceptance run. It verifies publication, event acknowledgement, and offline invalidation."; tags = "controlled-audit,low-risk"; aiReviewJson = '{"audit":true,"manualReview":"approved"}' }).data
$draft = (Invoke-DataCenterPost "/candidates/$($candidate.id)/mf-ep-draft" @{}).data
if ($draft.status -ne "pending_publish") { throw "MF_EP draft creation failed: $($draft.lastError)" }
$published = (Invoke-DataCenterPost "/candidates/$($candidate.id)/publish" @{ operator = "datacenter-closed-loop-script" }).data
if ($published.status -ne "published") { throw "Publish failed: $($published.lastError)" }
$publishAck = Wait-EventAck $candidate.id "publish"
$publishEvent = Get-MfEpEvent $publishAck.mfEpEventId
if ($publishEvent.deliveryStatus -ne "acknowledged") { throw "MF_EP publish event $($publishAck.mfEpEventId) is $($publishEvent.deliveryStatus), not acknowledged." }
$publishedAnswer = Assert-AgentRetrieval "plant care $topic" $topic $true
$offline = (Invoke-DataCenterPost "/candidates/$($candidate.id)/offline" @{ operator = "datacenter-closed-loop-script" }).data
if ($offline.status -ne "offline") { throw "Offline failed: $($offline.lastError)" }
$offlineAck = Wait-EventAck $candidate.id "offline"
$offlineEvent = Get-MfEpEvent $offlineAck.mfEpEventId
if ($offlineEvent.deliveryStatus -ne "acknowledged") { throw "MF_EP offline event $($offlineAck.mfEpEventId) is $($offlineEvent.deliveryStatus), not acknowledged." }
$offlineAnswer = Assert-AgentRetrieval "plant care $topic" $topic $false

[pscustomobject]@{
  Topic = $topic
  GapId = $gap.id
  CandidateId = $candidate.id
  MfEpDraftId = $published.mfEpDraftId
  MfEpContentId = $published.mfEpContentId
  PublishEventId = $publishAck.mfEpEventId
  OfflineEventId = $offlineAck.mfEpEventId
  PublishConfirmedBy = $publishEvent.consumer
  PublishConfirmedAt = $publishEvent.acknowledgedAt
  OfflineConfirmedBy = $offlineEvent.consumer
  OfflineConfirmedAt = $offlineEvent.acknowledgedAt
  AgentRetrievedPublishedContent = @($publishedAnswer.sources).Count -gt 0
  AgentRejectedOfflineContent = @($offlineAnswer.sources | Where-Object { $_.title -like "*$topic*" }).Count -eq 0
  Result = "acknowledged_and_retrieval_verified"
}
