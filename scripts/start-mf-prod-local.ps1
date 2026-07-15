param(
  [switch]$Restart,
  [switch]$PackageDataCenter,
  [switch]$WithEpAdmin,
  [switch]$WithEpMerchant
)

$ErrorActionPreference = "Stop"

$currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal($currentIdentity)
$isAdministrator = $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdministrator) {
  $arguments = @("-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$PSCommandPath`"")
  if ($Restart) { $arguments += "-Restart" }
  if ($PackageDataCenter) { $arguments += "-PackageDataCenter" }
  if ($WithEpAdmin) { $arguments += "-WithEpAdmin" }
  if ($WithEpMerchant) { $arguments += "-WithEpMerchant" }

  Start-Process -FilePath "powershell.exe" -Verb RunAs -ArgumentList $arguments
  Write-Host "An elevated startup window was opened. Approve the Windows prompt to continue."
  exit
}

. "$PSScriptRoot\mf-ports.ps1"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "logs\prod-local"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

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

function Wait-Port([string]$Name, [int]$Port, [int]$TimeoutSeconds = 120) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-Port $Port) {
      Write-Host "$Name is listening on port $Port."
      return
    }
    Start-Sleep -Seconds 2
  }
  throw "$Name did not listen on port $Port within $TimeoutSeconds seconds. Check $logDir"
}

function Import-DeploymentEnvironment {
  $names = @(
    "SPRING_PROFILES_ACTIVE",
    "MF_DB_URL", "MF_DB_USERNAME", "MF_DB_PASSWORD",
    "MF_REDIS_HOST", "MF_REDIS_PORT", "MF_REDIS_PASSWORD",
    "MF_EP_BASE_URL", "MF_EP_INTERNAL_TOKEN",
    "MF_DATACENTER_BASE_URL", "MF_DATACENTER_INTERNAL_TOKEN", "MF_DATACENTER_IDENTITY_SECRET",
    "DATACENTER_DB_URL", "DATACENTER_DB_USERNAME", "DATACENTER_DB_PASSWORD",
    "MF_EP_DATASOURCE_ENABLED", "MF_EP_DB_URL", "MF_EP_DB_USERNAME", "MF_EP_DB_PASSWORD",
    "MF_AGENT_LLM_ENABLED", "MF_AGENT_KNOWLEDGE_SYNC_KEY", "DEEPSEEK_API_KEY",
    "MF_AGENT_SPRING_AI_CHAT_MODEL", "MF_AGENT_OPENAI_BASE_URL", "MF_AGENT_OPENAI_MODEL"
  )

  foreach ($name in $names) {
    $value = [Environment]::GetEnvironmentVariable($name, "User")
    if (-not [string]::IsNullOrWhiteSpace($value)) {
      Set-Item -Path "Env:$name" -Value $value
    }
  }

  $required = @(
    "SPRING_PROFILES_ACTIVE",
    "MF_DB_URL", "MF_DB_USERNAME", "MF_DB_PASSWORD",
    "MF_REDIS_HOST", "MF_REDIS_PASSWORD",
    "MF_EP_BASE_URL", "MF_EP_INTERNAL_TOKEN",
    "MF_DATACENTER_BASE_URL", "MF_DATACENTER_INTERNAL_TOKEN", "MF_DATACENTER_IDENTITY_SECRET",
    "DATACENTER_DB_URL", "DATACENTER_DB_USERNAME", "DATACENTER_DB_PASSWORD",
    "MF_EP_DATASOURCE_ENABLED", "MF_EP_DB_URL", "MF_EP_DB_USERNAME", "MF_EP_DB_PASSWORD",
    "MF_AGENT_LLM_ENABLED", "MF_AGENT_KNOWLEDGE_SYNC_KEY", "DEEPSEEK_API_KEY"
  )
  $missing = $required | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process")) }
  if ($missing) {
    throw "Missing deployment environment variables: $($missing -join ', ')"
  }
  if ($env:SPRING_PROFILES_ACTIVE -ne "prod") {
    throw "SPRING_PROFILES_ACTIVE must be prod."
  }
  if ($env:MF_AGENT_LLM_ENABLED -ne "true") {
    throw "MF_AGENT_LLM_ENABLED must be true for this production-profile launcher."
  }
}

if ($Restart) {
  & "$PSScriptRoot\stop-mf-local.ps1"
}

Import-DeploymentEnvironment

$mysql = Get-Service -Name MySQL80 -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
  throw "MySQL80 service was not found. Start the production MySQL instance before running this script."
}
if ($mysql.Status -ne "Running") {
  Start-Service -Name MySQL80
  Start-Sleep -Seconds 5
}

$redisPort = 6379
if (-not [string]::IsNullOrWhiteSpace($env:MF_REDIS_PORT)) {
  $redisPort = [int]$env:MF_REDIS_PORT
}
if ($env:MF_REDIS_HOST -in @("127.0.0.1", "localhost") -and -not (Test-Port $redisPort)) {
  $redis = Get-Service -Name redis -ErrorAction SilentlyContinue
  if ($null -eq $redis) {
    throw "Redis is not listening on port $redisPort and the local redis service was not found. Start the configured Redis instance before running this script."
  }
  if ($redis.Status -ne "Running") {
    Start-Service -Name redis
    Start-Sleep -Seconds 3
  }
  Wait-Port "Redis" $redisPort 30
}

if ($PackageDataCenter) {
  & (Join-Path $root "MF_DataCenter\scripts\package-prod.ps1")
}

$dataCenterJar = Join-Path $root "MF_DataCenter\dist\datacenter-api.jar"
if (-not (Test-Path $dataCenterJar)) {
  throw "DataCenter production package is missing. Run this script again with -PackageDataCenter."
}

Start-CmdService "mf-ep-api" $root "powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-mf-ep-api.ps1" $MFPorts.MfEpApi
Wait-Port "MF_EP API" $MFPorts.MfEpApi 180

Start-CmdService "mf-datacenter" (Join-Path $root "MF_DataCenter") "powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-prod-api.ps1" $MFPorts.MfDataCenterApi
Wait-Port "MF_DataCenter API" $MFPorts.MfDataCenterApi

Start-CmdService "mf-agent-service" (Join-Path $root "MF_AgentService") "mvn -pl customer-agent-app -am package && java -jar customer-agent-app\target\customer-agent-app-0.0.1-SNAPSHOT.jar" $MFPorts.MfAgentService
Wait-Port "MF_AgentService" $MFPorts.MfAgentService

Start-CmdService "mf-ep-client" (Join-Path $root "MF_EP\projects\mf\mf-frontend-client") "npm run dev -- --host 127.0.0.1 --port 5174" $MFPorts.MfEpClient
Start-CmdService "mf-datacenter-web" (Join-Path $root "MF_DataCenter\datacenter-web") "npm run dev -- --host 127.0.0.1 --port 5176" $MFPorts.MfDataCenterWeb

if ($WithEpAdmin) {
  Start-CmdService "mf-ep-admin" (Join-Path $root "MF_EP\projects\mf\mf-frontend") "npm run dev -- --host 127.0.0.1 --port 5173" $MFPorts.MfEpAdmin
}
if ($WithEpMerchant) {
  Start-CmdService "mf-ep-merchant" (Join-Path $root "MF_EP\projects\mf\mf-frontend-merchant") "npm run dev -- --host 127.0.0.1 --port 5175" $MFPorts.MfEpMerchant
}

Wait-Port "MF_EP client" $MFPorts.MfEpClient 90
Wait-Port "MF_DataCenter web" $MFPorts.MfDataCenterWeb 90

$epHealth = Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health" -TimeoutSec 10
$agentHealth = Invoke-RestMethod -Uri "http://127.0.0.1:8092/actuator/health" -TimeoutSec 10
$dataCenterStatus = Invoke-RestMethod -Uri "http://127.0.0.1:8091/api/system/status" -TimeoutSec 10
if ($epHealth.status -ne "UP" -or $agentHealth.status -ne "UP" -or $dataCenterStatus.code -ne 0) {
  throw "One or more health checks failed. Check $logDir"
}

& "$PSScriptRoot\check-mf-ports.ps1"
Write-Host "Production-profile local stack is ready."
Write-Host "Client: http://127.0.0.1:5174"
Write-Host "DataCenter: http://127.0.0.1:5176"
