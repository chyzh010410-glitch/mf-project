param(
  [int[]]$Ports = @(8091, 5176)
)

$ErrorActionPreference = "Stop"

foreach ($port in $Ports) {
  $pids = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($pidValue in $pids) {
    Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped process $pidValue on port $port."
  }
}
