<#!
.SYNOPSIS
Restarts only the local MF_EP production-profile API and MF_EP web applications.

.DESCRIPTION
The script owns only the MF_EP ports defined in the shared mf-ports.ps1 file.
It never starts or stops databases, caches, AgentService, DataCenter, Website, or Pet.
#>

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
$repositoryRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $projectRoot))
. (Join-Path $repositoryRoot "scripts\\mf-ports.ps1")

$logDirectory = Join-Path $projectRoot "logs\\prod-local"
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

$services = @(
  @{ Name = "MF_EP API"; Port = [int]$MFPorts.MfEpApi; Type = "spring" },
  @{ Name = "MF_EP admin"; Port = [int]$MFPorts.MfEpAdmin; Type = "frontend"; Directory = "mf-frontend" },
  @{ Name = "MF_EP client"; Port = [int]$MFPorts.MfEpClient; Type = "frontend"; Directory = "mf-frontend-client" },
  @{ Name = "MF_EP merchant"; Port = [int]$MFPorts.MfEpMerchant; Type = "frontend"; Directory = "mf-frontend-merchant" }
)

function Test-ListeningPort([int]$Port) {
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Stop-OwnedPortListeners([int[]]$Ports) {
  $processIds = foreach ($port in $Ports) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
      Select-Object -ExpandProperty OwningProcess -Unique
  }

  foreach ($processId in ($processIds | Sort-Object -Unique)) {
    Stop-Process -Id $processId -Force -ErrorAction Stop
  }

  $deadline = (Get-Date).AddSeconds(20)
  while ((Get-Date) -lt $deadline) {
    $remaining = $Ports | Where-Object { Test-ListeningPort $_ }
    if (-not $remaining) {
      return
    }
    Start-Sleep -Milliseconds 500
  }

  throw "MF_EP ports did not close before restart: $($remaining -join ', ')."
}

function Wait-ListeningPort([string]$Name, [int]$Port, [int]$TimeoutSeconds = 120) {
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-ListeningPort $Port) {
      return
    }
    Start-Sleep -Seconds 2
  }

  throw "$Name did not listen on port $Port within $TimeoutSeconds seconds."
}

function Import-RequiredUserEnvironment {
  $required = @(
    "SPRING_PROFILES_ACTIVE",
    "MF_DB_URL", "MF_DB_USERNAME", "MF_DB_PASSWORD",
    "MF_REDIS_HOST", "MF_REDIS_PORT", "MF_REDIS_PASSWORD",
    "MF_EP_BASE_URL", "MF_EP_INTERNAL_TOKEN",
    "MF_DATACENTER_BASE_URL", "MF_DATACENTER_INTERNAL_TOKEN", "MF_DATACENTER_IDENTITY_SECRET"
  )

  foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name, "User")
    if (-not [string]::IsNullOrWhiteSpace($value)) {
      Set-Item -Path "Env:$name" -Value $value
    }
  }

  $missing = $required | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
  }
  if ($missing) {
    throw "Missing Windows user environment variables: $($missing -join ', ')."
  }
  if ($env:SPRING_PROFILES_ACTIVE -ne "prod") {
    throw "SPRING_PROFILES_ACTIVE must be prod."
  }
}

function Test-DependencyPort([string]$Name, [int]$Port, [bool]$Required) {
  if (Test-ListeningPort $Port) {
    return "${Name}:$Port ready"
  }

  $message = "${Name}:$Port is not listening. Start this dependency separately; this script will not manage it."
  if ($Required) {
    throw $message
  }
  return "$message (MF_EP restart continues, but the related feature is unavailable.)"
}

function Start-Frontend([hashtable]$Service) {
  $outputLog = Join-Path $logDirectory ("{0}.out.log" -f $Service.Name.Replace(" ", "-"))
  $errorLog = Join-Path $logDirectory ("{0}.err.log" -f $Service.Name.Replace(" ", "-"))
  Start-Process -FilePath "cmd.exe" `
    -ArgumentList @("/c", "npm run dev -- --host 127.0.0.1 --port $($Service.Port)") `
    -WorkingDirectory (Join-Path $projectRoot $Service.Directory) `
    -WindowStyle Hidden `
    -RedirectStandardOutput $outputLog `
    -RedirectStandardError $errorLog | Out-Null
}

Import-RequiredUserEnvironment

$dependencyChecks = @(
  Test-DependencyPort "MySQL" 3306 $true
  Test-DependencyPort "Redis" ([int]$env:MF_REDIS_PORT) $true
  Test-DependencyPort "MF_AgentService" ([int]$MFPorts.MfAgentService) $false
  Test-DependencyPort "MF_DataCenter" ([int]$MFPorts.MfDataCenterApi) $false
)

Stop-OwnedPortListeners ($services.Port)

$apiRoot = Join-Path $projectRoot "mf-fertilizer"
$apiOutputLog = Join-Path $logDirectory "mf-ep-api.out.log"
$apiErrorLog = Join-Path $logDirectory "mf-ep-api.err.log"
$buildLog = Join-Path $logDirectory "mf-ep-api.build.log"
Push-Location $apiRoot
try {
  & mvn "-pl" "fertilizer-api" "-am" "package" "-DskipTests" *> $buildLog
  if ($LASTEXITCODE -ne 0) {
    throw "MF_EP API build failed. See $buildLog"
  }
  & mvn "-pl" "fertilizer-api" "dependency:build-classpath" "-Dmdep.outputFile=target/runtime-classpath.txt" "-Dmdep.includeScope=runtime" *>> $buildLog
  if ($LASTEXITCODE -ne 0) {
    throw "MF_EP API runtime classpath build failed. See $buildLog"
  }
} finally {
  Pop-Location
}

$classpath = @(
  (Join-Path $apiRoot "fertilizer-api\\target\\classes"),
  (Join-Path $apiRoot "fertilizer-core\\target\\classes"),
  (Join-Path $apiRoot "fertilizer-common\\target\\classes"),
  (Get-Content -LiteralPath (Join-Path $apiRoot "fertilizer-api\\target\\runtime-classpath.txt") -Raw).Trim()
) -join ";"

Start-Process -FilePath "java.exe" `
  -ArgumentList @("-cp", $classpath, "com.mf.fertilizer.FertilizerApplication") `
  -WorkingDirectory $apiRoot `
  -WindowStyle Hidden `
  -RedirectStandardOutput $apiOutputLog `
  -RedirectStandardError $apiErrorLog | Out-Null

Wait-ListeningPort "MF_EP API" ([int]$MFPorts.MfEpApi) 180
$health = Invoke-RestMethod -Uri "http://127.0.0.1:$($MFPorts.MfEpApi)/actuator/health" -TimeoutSec 10
if ($health.status -ne "UP") {
  throw "MF_EP API health endpoint did not return UP."
}

foreach ($service in $services | Where-Object { $_.Type -eq "frontend" }) {
  Start-Frontend $service
  Wait-ListeningPort $service.Name $service.Port 90
}

Write-Host "MF_EP access:"
Write-Host "  API: http://127.0.0.1:$($MFPorts.MfEpApi) (health: UP)"
foreach ($service in $services | Where-Object { $_.Type -eq "frontend" }) {
  Write-Host "  $($service.Name): http://127.0.0.1:$($service.Port) (port listening)"
}
Write-Host "MF_EP logs: $logDirectory"
Write-Host "Dependency checks: $($dependencyChecks -join '; ')"
