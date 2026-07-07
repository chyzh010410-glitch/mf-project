param(
  [switch]$WithWebsite,
  [switch]$WithPet,
  [switch]$WithEpFrontends
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\mf-ports.ps1"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "logs\local"
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Test-Port([int]$Port) {
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-CmdService([string]$Name, [string]$WorkingDirectory, [string]$Command, [int]$Port) {
  if (Test-Port $Port) {
    Write-Host "$Name already listens on port $Port."
    return
  }

  $log = Join-Path $logDir "$Name.log"
  Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "$Command > `"$log`" 2>&1" `
    -WorkingDirectory $WorkingDirectory `
    -WindowStyle Hidden | Out-Null
  Write-Host "Starting $Name on port $Port. Log: $log"
}

function Wait-Port([string]$Name, [int]$Port, [int]$TimeoutSeconds = 90) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-Port $Port) {
      Write-Host "$Name is listening on port $Port."
      return $true
    }
    Start-Sleep -Seconds 2
  }
  Write-Warning "$Name did not listen on port $Port within $TimeoutSeconds seconds."
  return $false
}

if (Get-Service -Name MySQL80 -ErrorAction SilentlyContinue) {
  if ((Get-Service -Name MySQL80).Status -ne "Running") {
    Start-Service -Name MySQL80
    Start-Sleep -Seconds 5
  }
  Write-Host "MySQL80 is running."
} else {
  Write-Warning "MySQL80 service was not found."
}

if (-not (Test-Port 6379)) {
  $redisExe = "E:\Program Files\Redis\redis-server.exe"
  if (Test-Path -LiteralPath $redisExe) {
    Start-Process -FilePath $redisExe `
      -ArgumentList "--port 6379 --requirepass 123456" `
      -WindowStyle Hidden | Out-Null
    Write-Host "Starting Redis on port 6379."
  } else {
    Write-Warning "Redis executable was not found at $redisExe."
  }
} else {
  Write-Host "Redis already listens on port 6379."
}

$mfEpApiDir = $root
$dataCenterDir = Join-Path $root "MF_DataCenter"
$agentDir = Join-Path $root "MF_AgentService"

Start-CmdService "mf-ep-api" $mfEpApiDir "powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-mf-ep-api.ps1" $MFPorts.MfEpApi
Start-CmdService "mf-datacenter" $dataCenterDir "powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-local.ps1" $MFPorts.MfDataCenterApi
Start-CmdService "mf-agent-service" $agentDir "set MF_EP_BASE_URL=http://127.0.0.1:8080&& set MF_DATACENTER_BASE_URL=http://127.0.0.1:8091&& set MF_AGENT_LLM_ENABLED=false&& set OPENAI_API_KEY=local-disabled-key&& mvn spring-boot:run" $MFPorts.MfAgentService

if ($WithEpFrontends) {
  Start-CmdService "mf-ep-admin" (Join-Path $root "MF_EP\projects\mf\mf-frontend") "npm run dev -- --host 127.0.0.1 --port 5173" $MFPorts.MfEpAdmin
  Start-CmdService "mf-ep-client" (Join-Path $root "MF_EP\projects\mf\mf-frontend-client") "npm run dev -- --host 127.0.0.1 --port 5174" $MFPorts.MfEpClient
  Start-CmdService "mf-ep-merchant" (Join-Path $root "MF_EP\projects\mf\mf-frontend-merchant") "npm run dev -- --host 127.0.0.1 --port 5175" $MFPorts.MfEpMerchant
}

if ($WithWebsite) {
  Start-CmdService "mf-website" (Join-Path $root "MF_Website\mf_Website") "npm run dev -- --host 127.0.0.1 --port 5178" $MFPorts.MfWebsite
}

if ($WithPet) {
  Start-CmdService "mf-pet-demo" (Join-Path $root "MF_Pet") "python -m http.server 5179 --bind 127.0.0.1" $MFPorts.MfPetDemo
}

Wait-Port "MF_EP API" $MFPorts.MfEpApi 120 | Out-Null
Wait-Port "MF_DataCenter API" $MFPorts.MfDataCenterApi 120 | Out-Null
Wait-Port "MF_AgentService" $MFPorts.MfAgentService 120 | Out-Null

& "$PSScriptRoot\check-mf-ports.ps1"
