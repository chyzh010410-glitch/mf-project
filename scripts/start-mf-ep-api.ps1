$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mfRoot = Join-Path $root "MF_EP\projects\mf\mf-fertilizer"
$apiClasses = Join-Path $mfRoot "fertilizer-api\target\classes"
$coreClasses = Join-Path $mfRoot "fertilizer-core\target\classes"
$commonClasses = Join-Path $mfRoot "fertilizer-common\target\classes"
$classpathFile = Join-Path $mfRoot "fertilizer-api\target\runtime-classpath.txt"
$mainClass = "com.mf.fertilizer.FertilizerApplication"

Push-Location $mfRoot
try {
  mvn -pl fertilizer-api -am package -DskipTests
  mvn -pl fertilizer-api dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt -Dmdep.includeScope=runtime
} finally {
  Pop-Location
}

$dependencyClasspath = Get-Content -LiteralPath $classpathFile -Raw
$classpath = @($apiClasses, $coreClasses, $commonClasses, $dependencyClasspath.Trim()) -join ";"

java -cp $classpath $mainClass

