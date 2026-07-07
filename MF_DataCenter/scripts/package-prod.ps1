$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apiDir = Join-Path $root "datacenter-api"
$webDir = Join-Path $root "datacenter-web"
$distDir = Join-Path $root "dist"

New-Item -ItemType Directory -Force -Path $distDir | Out-Null

Push-Location $apiDir
try {
  mvn clean package -DskipTests
} finally {
  Pop-Location
}

Push-Location $webDir
try {
  npm run build
} finally {
  Pop-Location
}

Copy-Item -Force (Join-Path $apiDir "target\datacenter-api-0.0.1-SNAPSHOT.jar") (Join-Path $distDir "datacenter-api.jar")
if (Test-Path (Join-Path $distDir "web")) {
  Remove-Item -LiteralPath (Join-Path $distDir "web") -Recurse -Force
}
Copy-Item -Recurse -Force (Join-Path $webDir "dist") (Join-Path $distDir "web")

Write-Host "Packaged API jar: $distDir\datacenter-api.jar"
Write-Host "Packaged web dist: $distDir\web"
