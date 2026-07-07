param(
  [int]$ApiPort = 8091,
  [int]$WebPort = 5176
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apiDir = Join-Path $root "datacenter-api"
$webDir = Join-Path $root "datacenter-web"

function Test-Port([int]$Port) {
  return [bool](Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue)
}

if (-not (Get-Service -Name MySQL80 -ErrorAction SilentlyContinue)) {
  throw "MySQL80 service was not found."
}

if ((Get-Service -Name MySQL80).Status -ne "Running") {
  Start-Service -Name MySQL80
  Start-Sleep -Seconds 5
}

if (Test-Port $ApiPort) {
  Write-Host "API port $ApiPort is already in use."
} else {
  Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "mvn spring-boot:run -Dspring-boot.run.profiles=local > local-run.log 2>&1" `
    -WorkingDirectory $apiDir `
    -WindowStyle Hidden | Out-Null
  Write-Host "Started datacenter-api on port $ApiPort."
}

if (Test-Port $WebPort) {
  Write-Host "Web port $WebPort is already in use."
} else {
  Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "npm run dev -- --host 127.0.0.1 --port $WebPort > web-run.log 2>&1" `
    -WorkingDirectory $webDir `
    -WindowStyle Hidden | Out-Null
  Write-Host "Started datacenter-web on port $WebPort."
}

Write-Host "API: http://127.0.0.1:$ApiPort"
Write-Host "Web: http://127.0.0.1:$WebPort"
