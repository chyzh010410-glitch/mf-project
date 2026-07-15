param(
  [int]$ApiPort = 0,
  [int]$WebPort = 0,
  [int]$StartupTimeoutSeconds = 90
)

$ErrorActionPreference = "Stop"

function Test-IsAdministrator {
  $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = New-Object Security.Principal.WindowsPrincipal($identity)
  return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Restart-Elevated {
  $powershell = (Get-Command powershell.exe -ErrorAction Stop).Source
  $arguments = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", "`"$PSCommandPath`"",
    "-ApiPort", $ApiPort,
    "-WebPort", $WebPort,
    "-StartupTimeoutSeconds", $StartupTimeoutSeconds
  )

  try {
    $elevated = Start-Process -FilePath $powershell -ArgumentList $arguments -Verb RunAs -Wait -PassThru
  } catch {
    throw "MF_DataCenter prod restart requires administrator approval. UAC elevation was cancelled or could not be started."
  }

  if ($elevated.ExitCode -ne 0) {
    throw "Elevated MF_DataCenter prod restart failed with exit code $($elevated.ExitCode). Check this project's logs/prod-local directory."
  }
}

if (-not (Test-IsAdministrator)) {
  Write-Host "Requesting administrator permission for the MF_DataCenter prod restart."
  Restart-Elevated
  exit 0
}

function Stop-PortListener {
  param(
    [int]$Port,
    [string]$Name
  )

  $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
  if (-not $listeners) {
    Write-Host "$Name port $Port is not listening; continuing."
    return
  }

  $listenerProcessIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($listenerProcessId in $listenerProcessIds) {
    if ($listenerProcessId -gt 0) {
      Write-Host "Stopping $Name listener on port $Port, PID $listenerProcessId"
      try {
        Stop-Process -Id $listenerProcessId -Force -ErrorAction Stop
      } catch {
        throw "Failed to stop $Name listener PID $listenerProcessId on port $Port. Run this script as the same Windows user or an elevated shell that owns that process."
      }
    }
  }

  $deadline = (Get-Date).AddSeconds(20)
  while ((Get-Date) -lt $deadline) {
    if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
      return
    }
    Start-Sleep -Milliseconds 500
  }

  throw "$Name port $Port is still listening after stopping the recorded PID."
}

function Wait-PortListening {
  param(
    [int]$Port,
    [string]$Name,
    [int]$TimeoutSeconds
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
      return
    }
    Start-Sleep -Seconds 2
  }

  throw "$Name did not listen on port $Port within $TimeoutSeconds seconds."
}

function Test-ApiHealth {
  param(
    [string]$HealthUrl
  )

  try {
    $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 5
    return ($response.code -eq 0 `
      -or $response.code -eq "OK" `
      -or $response.status -eq "UP" `
      -or $response.data.status -eq "UP" `
      -or $response.data.status -eq "running")
  } catch {
    return $false
  }
}

function Wait-ApiHealth {
  param(
    [string]$HealthUrl,
    [int]$TimeoutSeconds
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-ApiHealth -HealthUrl $HealthUrl) {
      return
    }
    Start-Sleep -Seconds 2
  }

  throw "MF_DataCenter API health check did not report UP."
}

function Start-WebServer {
  param(
    [string]$WebRoot,
    [int]$Port,
    [int]$ApiPort,
    [string]$OutputLogFile,
    [string]$ErrorLogFile
  )

  $node = Get-Command node -ErrorAction SilentlyContinue
  if (-not $node) {
    throw "Node.js was not found in PATH; cannot start MF_DataCenter web server."
  }

  $serverJs = @"
const http = require('http');
const fs = require('fs');
const path = require('path');
const root = process.env.MF_DATACENTER_WEB_ROOT;
const port = Number(process.env.MF_DATACENTER_WEB_PORT);
const apiPort = Number(process.env.MF_DATACENTER_API_PORT);
const types = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
};

function send(res, status, body, type) {
  res.writeHead(status, { 'Content-Type': type || 'text/plain; charset=utf-8' });
  res.end(body);
}

function proxyApi(req, res) {
  const options = {
    hostname: '127.0.0.1',
    port: apiPort,
    path: req.url,
    method: req.method,
    headers: Object.assign({}, req.headers, { host: '127.0.0.1:' + apiPort })
  };
  const upstream = http.request(options, upstreamRes => {
    res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers);
    upstreamRes.pipe(res);
  });
  upstream.on('error', err => send(res, 502, 'API proxy failed: ' + err.message));
  req.pipe(upstream);
}

function serveStatic(req, res) {
  let requestPath = decodeURIComponent((req.url || '/').split('?')[0]);
  if (requestPath === '/') requestPath = '/index.html';
  const normalized = path.normalize(requestPath).replace(/^(\.\.[\/\\])+/, '');
  let filePath = path.join(root, normalized);
  if (!filePath.startsWith(root)) {
    send(res, 403, 'Forbidden');
    return;
  }
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(root, 'index.html');
  }
  fs.readFile(filePath, (err, content) => {
    if (err) {
      send(res, 404, 'Not found');
      return;
    }
    send(res, 200, content, types[path.extname(filePath).toLowerCase()] || 'application/octet-stream');
  });
}

http.createServer((req, res) => {
  if ((req.url || '').startsWith('/api/')) {
    proxyApi(req, res);
    return;
  }
  serveStatic(req, res);
}).listen(port, '127.0.0.1', () => {
  console.log('MF_DataCenter web listening on http://127.0.0.1:' + port);
});
"@

  $env:MF_DATACENTER_WEB_ROOT = $WebRoot
  $env:MF_DATACENTER_WEB_PORT = [string]$Port
  $env:MF_DATACENTER_API_PORT = [string]$ApiPort
  $encodedServerJs = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($serverJs))
  $nodeEval = "eval(Buffer.from('$encodedServerJs','base64').toString('utf8'))"

  Start-Process -FilePath $node.Source `
    -ArgumentList "-e", $nodeEval `
    -WorkingDirectory $WebRoot `
    -RedirectStandardOutput $OutputLogFile `
    -RedirectStandardError $ErrorLogFile `
    -WindowStyle Hidden | Out-Null
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $projectRoot
$portsFile = Join-Path $repoRoot "scripts\mf-ports.ps1"

if (Test-Path $portsFile) {
  . $portsFile
  if ($ApiPort -le 0 -and $MFPorts.Contains("MfDataCenterApi")) {
    $ApiPort = [int]$MFPorts.MfDataCenterApi
  }
  if ($WebPort -le 0 -and $MFPorts.Contains("MfDataCenterWeb")) {
    $WebPort = [int]$MFPorts.MfDataCenterWeb
  }
}

if ($ApiPort -le 0) {
  $ApiPort = 8091
}
if ($WebPort -le 0) {
  $WebPort = 5176
}

$jar = Join-Path $projectRoot "dist\datacenter-api.jar"
$webRoot = Join-Path $projectRoot "dist\web"
$logDir = Join-Path $projectRoot "logs\prod-local"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$apiLog = Join-Path $logDir "datacenter-api-$timestamp.log"
$webLog = Join-Path $logDir "datacenter-web-$timestamp.log"
$webErrorLog = Join-Path $logDir "datacenter-web-error-$timestamp.log"
$latestApiLog = Join-Path $logDir "datacenter-api-latest.log"
$latestWebLog = Join-Path $logDir "datacenter-web-latest.log"
$latestWebErrorLog = Join-Path $logDir "datacenter-web-error-latest.log"
$healthUrl = "http://127.0.0.1:$ApiPort/api/system/status"
$apiUrl = "http://127.0.0.1:$ApiPort"
$webUrl = "http://127.0.0.1:$WebPort"

$requiredUserEnv = @(
  "SPRING_PROFILES_ACTIVE",
  "DATACENTER_DB_URL",
  "DATACENTER_DB_USERNAME",
  "DATACENTER_DB_PASSWORD",
  "MF_DATACENTER_INTERNAL_TOKEN",
  "MF_DATACENTER_IDENTITY_SECRET",
  "MF_EP_BASE_URL",
  "MF_EP_INTERNAL_TOKEN"
)

if ($env:MF_EP_DATASOURCE_ENABLED -eq "true" -or [Environment]::GetEnvironmentVariable("MF_EP_DATASOURCE_ENABLED", "User") -eq "true") {
  $requiredUserEnv += @(
    "MF_EP_DATASOURCE_ENABLED",
    "MF_EP_DB_URL",
    "MF_EP_DB_USERNAME",
    "MF_EP_DB_PASSWORD"
  )
}

$missing = New-Object System.Collections.Generic.List[string]
foreach ($name in $requiredUserEnv) {
  $value = [Environment]::GetEnvironmentVariable($name, "User")
  if ([string]::IsNullOrWhiteSpace($value)) {
    $missing.Add($name)
    continue
  }
  Set-Item -Path "Env:$name" -Value $value
}

if ($missing.Count -gt 0) {
  throw "Missing required Windows user environment variables: $($missing -join ', ')"
}

if ($env:SPRING_PROFILES_ACTIVE -ne "prod") {
  throw "SPRING_PROFILES_ACTIVE must be prod."
}

if (-not (Test-Path $jar)) {
  throw "Production jar not found: $jar. Run this project's package script first."
}
if (-not (Test-Path (Join-Path $webRoot "index.html"))) {
  throw "Production web build not found: $webRoot. Run this project's package script first."
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Stop-PortListener -Port $WebPort -Name "MF_DataCenter web"
Stop-PortListener -Port $ApiPort -Name "MF_DataCenter API"

$dependencyChecks = [ordered]@{}
$mfEpBaseUrl = [Environment]::GetEnvironmentVariable("MF_EP_BASE_URL", "User")
if (-not [string]::IsNullOrWhiteSpace($mfEpBaseUrl)) {
  $uri = [Uri]$mfEpBaseUrl
  $dependencyPort = $uri.Port
  $portListening = [bool](Get-NetTCPConnection -LocalPort $dependencyPort -State Listen -ErrorAction SilentlyContinue)
  $healthOk = $false
  try {
    $dependencyHealth = Invoke-WebRequest -Uri "$mfEpBaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 3
    $healthOk = $dependencyHealth.StatusCode -ge 200 -and $dependencyHealth.StatusCode -lt 300
  } catch {
    $healthOk = $false
  }
  $dependencyChecks["MF_EP"] = $portListening -or $healthOk
}

Write-Host "Starting MF_DataCenter API prod on port $ApiPort"
Start-Process -FilePath "cmd.exe" `
  -ArgumentList "/c", "java -jar `"$jar`" --spring.profiles.active=prod --server.port=$ApiPort > `"$apiLog`" 2>&1" `
  -WorkingDirectory $projectRoot `
  -WindowStyle Hidden | Out-Null

Wait-PortListening -Port $ApiPort -Name "MF_DataCenter API" -TimeoutSeconds $StartupTimeoutSeconds
Wait-ApiHealth -HealthUrl $healthUrl -TimeoutSeconds 45

Write-Host "Starting MF_DataCenter web on port $WebPort"
Start-WebServer -WebRoot $webRoot -Port $WebPort -ApiPort $ApiPort -OutputLogFile $webLog -ErrorLogFile $webErrorLog
Wait-PortListening -Port $WebPort -Name "MF_DataCenter web" -TimeoutSeconds 30

$webStatus = (Invoke-WebRequest -Uri $webUrl -UseBasicParsing -TimeoutSec 5).StatusCode
if ($webStatus -lt 200 -or $webStatus -ge 300) {
  throw "MF_DataCenter web did not return a successful status code. Status: $webStatus"
}

Copy-Item -Path $apiLog -Destination $latestApiLog -Force -ErrorAction SilentlyContinue
Copy-Item -Path $webLog -Destination $latestWebLog -Force -ErrorAction SilentlyContinue
Copy-Item -Path $webErrorLog -Destination $latestWebErrorLog -Force -ErrorAction SilentlyContinue

Write-Host "Project: MF_DataCenter"
Write-Host "ApiAddress: $apiUrl"
Write-Host "WebAddress: $webUrl"
Write-Host "ApiPort: $ApiPort"
Write-Host "WebPort: $WebPort"
Write-Host "ApiLog: $apiLog"
Write-Host "WebLog: $webLog"
Write-Host "ApiHealth: UP"
Write-Host "WebHealth: HTTP $webStatus"

foreach ($item in $dependencyChecks.GetEnumerator()) {
  $state = if ($item.Value) { "reachable_or_listening" } else { "not_detected" }
  Write-Host "DependencyCheck: $($item.Key) $state"
}
