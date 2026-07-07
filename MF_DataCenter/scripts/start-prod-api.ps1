param(
  [int]$ApiPort = 8091
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root "dist\datacenter-api.jar"
$log = Join-Path $root "dist\datacenter-api.log"

if (-not (Test-Path $jar)) {
  throw "Production jar not found. Run scripts\package-prod.ps1 first."
}

if ((Get-Service -Name MySQL80 -ErrorAction SilentlyContinue).Status -ne "Running") {
  Start-Service -Name MySQL80
  Start-Sleep -Seconds 5
}

if (Get-NetTCPConnection -LocalPort $ApiPort -ErrorAction SilentlyContinue) {
  Write-Host "API port $ApiPort is already in use."
  exit 0
}

Start-Process -FilePath "cmd.exe" `
  -ArgumentList "/c", "java -jar `"$jar`" --spring.profiles.active=prod > `"$log`" 2>&1" `
  -WorkingDirectory $root `
  -WindowStyle Hidden | Out-Null

Write-Host "Started production API on http://127.0.0.1:$ApiPort"
Write-Host "Log: $log"
