$ErrorActionPreference = "Stop"

$currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal($currentIdentity)
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
  $elevatedProcess = Start-Process -FilePath "powershell.exe" -Verb RunAs -PassThru -Wait -ArgumentList @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", "`"$PSCommandPath`""
  )
  exit $elevatedProcess.ExitCode
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = (Resolve-Path $projectRoot).Path
$sharedPorts = Join-Path (Split-Path -Parent $projectRoot) "scripts\mf-ports.ps1"
. $sharedPorts

$agentPort = [int]$MFPorts.MfAgentService
$logDirectory = Join-Path $projectRoot "logs\prod-local"
$logPath = Join-Path $logDirectory "mf-agent-service.log"
$jarPath = Join-Path $projectRoot "customer-agent-app\target\customer-agent-app-0.0.1-SNAPSHOT.jar"

function Import-UserEnvironment([string[]]$Names) {
  foreach ($name in $Names) {
    $value = [Environment]::GetEnvironmentVariable($name, "User")
    if (-not [string]::IsNullOrWhiteSpace($value)) {
      Set-Item -Path "Env:$name" -Value $value
    }
  }
}

function Test-PortListening([int]$Port) {
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Stop-ListenerOnPort([int]$Port) {
  $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
  $processIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)

  foreach ($processId in $processIds) {
    Stop-Process -Id $processId -Force -ErrorAction Stop
  }

  $deadline = (Get-Date).AddSeconds(30)
  while ((Get-Date) -lt $deadline -and (Test-PortListening $Port)) {
    Start-Sleep -Milliseconds 500
  }

  if (Test-PortListening $Port) {
    throw "MF_AgentService port $Port is still listening after its owning process was stopped."
  }
}

function Wait-Port([int]$Port, [int]$TimeoutSeconds) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-PortListening $Port) {
      return $true
    }
    Start-Sleep -Seconds 1
  }
  return $false
}

function Get-DependencyStatus([string]$Name, [string]$Uri, [scriptblock]$IsHealthy) {
  try {
    $response = Invoke-RestMethod -Uri $Uri -TimeoutSec 5
    if (& $IsHealthy $response) {
      return "${Name}: ready"
    }
    return "${Name}: health check returned an unexpected result"
  } catch {
    return "${Name}: unavailable; start it separately before using dependent capabilities"
  }
}

$environmentNames = @(
  "SPRING_PROFILES_ACTIVE",
  "MF_EP_BASE_URL",
  "MF_DATACENTER_BASE_URL",
  "MF_EP_INTERNAL_TOKEN",
  "MF_AGENT_KNOWLEDGE_SYNC_KEY",
  "MF_AGENT_LLM_ENABLED",
  "DEEPSEEK_API_KEY",
  "OPENAI_API_KEY",
  "MF_AGENT_OPENAI_BASE_URL",
  "MF_AGENT_OPENAI_MODEL",
  "MF_AGENT_SPRING_AI_CHAT_MODEL",
  "MF_AGENT_KNOWLEDGE_SYNC_POLL_INTERVAL",
  "MF_AGENT_KNOWLEDGE_SYNC_MAX_RETRIES",
  "MF_AGENT_KNOWLEDGE_SYNC_PENDING_TIMEOUT"
)
Import-UserEnvironment $environmentNames

$requiredNames = @(
  "SPRING_PROFILES_ACTIVE",
  "MF_EP_BASE_URL",
  "MF_DATACENTER_BASE_URL",
  "MF_EP_INTERNAL_TOKEN",
  "MF_AGENT_KNOWLEDGE_SYNC_KEY",
  "MF_AGENT_LLM_ENABLED"
)
$missingNames = @($requiredNames | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process")) })
if ($missingNames.Count -gt 0) {
  throw "Missing required user environment variables: $($missingNames -join ', ')"
}
if ($env:SPRING_PROFILES_ACTIVE -ne "prod") {
  throw "SPRING_PROFILES_ACTIVE must be prod."
}
if ($env:MF_AGENT_LLM_ENABLED -eq "true" -and [string]::IsNullOrWhiteSpace($env:DEEPSEEK_API_KEY) -and [string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
  throw "MF_AGENT_LLM_ENABLED=true requires DEEPSEEK_API_KEY or OPENAI_API_KEY."
}

$epBaseUrl = $env:MF_EP_BASE_URL.TrimEnd('/')
$dataCenterBaseUrl = $env:MF_DATACENTER_BASE_URL.TrimEnd('/')
$dependencyResults = @(
  (Get-DependencyStatus -Name "MF_EP" -Uri "$epBaseUrl/actuator/health" -IsHealthy { param($response) $response.status -eq "UP" })
  (Get-DependencyStatus -Name "MF_DataCenter" -Uri "$dataCenterBaseUrl/api/system/status" -IsHealthy { param($response) $response.code -eq 0 })
)

New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
Stop-ListenerOnPort $agentPort
Set-Content -LiteralPath $logPath -Encoding UTF8 -Value "MF_AgentService production-local restart started at $(Get-Date -Format s)."

$startCommand = "mvn -pl customer-agent-app -am package >> `"$logPath`" 2>&1 && java -jar customer-agent-app\target\customer-agent-app-0.0.1-SNAPSHOT.jar >> `"$logPath`" 2>&1"
Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $startCommand) -WorkingDirectory $projectRoot -WindowStyle Hidden | Out-Null

if (-not (Wait-Port $agentPort 120)) {
  throw "MF_AgentService did not listen on port $agentPort within 120 seconds. Check $logPath"
}

try {
  $health = Invoke-RestMethod -Uri "http://127.0.0.1:$agentPort/actuator/health" -TimeoutSec 10
  if ($health.status -ne "UP") {
    throw "health status was not UP"
  }
  $verification = "health check: UP"
} catch {
  throw "MF_AgentService is listening on port $agentPort but health verification failed. Check $logPath"
}

Write-Host "MF_AgentService: http://127.0.0.1:${agentPort}"
Write-Host "Port: $agentPort"
Write-Host "Log: $logPath"
Write-Host "Verification: $verification"
foreach ($result in $dependencyResults) {
  Write-Host "Dependency check: $result"
}
