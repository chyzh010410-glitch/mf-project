param(
  [switch]$IncludeDependencies
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\mf-ports.ps1"

foreach ($port in ($MFPorts.Values | Sort-Object -Unique)) {
  $processIds = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

  foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped $($process.ProcessName) ($processId) on port $port."
  }
}

if ($IncludeDependencies) {
  $redis = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($processId in $redis) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped Redis dependency $($process.ProcessName) ($processId) on port 6379."
  }
}

& "$PSScriptRoot\check-mf-ports.ps1"

