param(
  [string]$ApiBase = "http://127.0.0.1:8091",
  [string]$WebBase = "http://127.0.0.1:5176"
)

$ErrorActionPreference = "Stop"

$system = Invoke-RestMethod "$ApiBase/api/system/status"
$dictionary = Invoke-RestMethod "$ApiBase/api/metrics/dictionary"
$quality = Invoke-RestMethod "$ApiBase/api/data-quality/summary"
$source = Invoke-RestMethod "$ApiBase/api/source/check"
$dashboard = Invoke-RestMethod "$ApiBase/api/dashboard/overview"
$web = Invoke-WebRequest "$WebBase/metric-governance" -UseBasicParsing

[pscustomobject]@{
  ApiStatus = $system.code
  MetricDefinitions = $dictionary.data.Count
  QualityChecks = $quality.data.total
  SourceFailedTables = $source.data.failedTables
  SourceMissingFields = $source.data.missingFields
  GovernanceStatus = $dashboard.data.governance.status
  WebStatus = $web.StatusCode
}
