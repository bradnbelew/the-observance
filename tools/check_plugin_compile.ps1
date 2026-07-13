param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$pluginRoot = Join-Path $RepoRoot "plugin"
$srcRoot = Join-Path $pluginRoot "src\main\java"
$testSrcRoot = Join-Path $pluginRoot "src\test\java"
$pluginSource = Join-Path $srcRoot "com\observance\watcher\ObservancePlugin.java"
$beatEngineSource = Join-Path $srcRoot "com\observance\watcher\beats\BeatEngine.java"
$beatLibrarySource = Join-Path $srcRoot "com\observance\watcher\beats\BeatLibrary.java"
$packTrackerSource = Join-Path $srcRoot "com\observance\watcher\signal\ResourcePackTracker.java"
$buildRoot = Join-Path $pluginRoot "build"
$classesDir = Join-Path $buildRoot "check-plugin-classes"
$testClassesDir = Join-Path $buildRoot "check-plugin-test-classes"
$argsFile = Join-Path $buildRoot "check-plugin-javac.args"
$testArgsFile = Join-Path $buildRoot "check-plugin-test-javac.args"
$gradleCache = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1"

if (!(Test-Path $srcRoot)) {
  throw "Plugin source directory not found: $srcRoot"
}
foreach ($file in @($pluginSource, $beatEngineSource, $beatLibrarySource, $packTrackerSource)) {
  if (!(Test-Path $file)) {
    throw "Plugin runtime wiring source not found: $file"
  }
}

if (!(Test-Path $gradleCache)) {
  throw "Gradle module cache not found: $gradleCache"
}

$javac = Get-Command javac -ErrorAction SilentlyContinue
if ($null -eq $javac) {
  throw "javac is not on PATH. Install/use JDK 21 before running this check."
}

$versionText = (& javac -version 2>&1 | Out-String).Trim()
if ($versionText -notmatch "21\.") {
  throw "Expected javac 21.x, got: $versionText"
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$classesFull = [System.IO.Path]::GetFullPath($classesDir)
if (!$classesFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to write outside repo: $classesFull"
}

New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
foreach ($dir in @($classesDir, $testClassesDir)) {
  if (Test-Path $dir) {
    Remove-Item -LiteralPath $dir -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

$javaFiles = Get-ChildItem $srcRoot -Recurse -Filter *.java | Sort-Object FullName
if ($javaFiles.Count -eq 0) {
  throw "No Java source files found under $srcRoot"
}

$jars = Get-ChildItem $gradleCache -Recurse -Filter *.jar | Sort-Object FullName
if ($jars.Count -eq 0) {
  throw "No dependency jars found under $gradleCache"
}

function Convert-JavacPath([string]$Path) {
  return $Path.Replace("\", "/")
}

$classpath = ($jars | ForEach-Object { Convert-JavacPath $_.FullName }) -join ";"
$lines = @(
  "-encoding", "UTF-8",
  "--release", "21",
  "-proc:none",
  "-Xlint:deprecation",
  "-Xlint:unchecked",
  "-Werror",
  "-classpath", "`"$classpath`"",
  "-d", "`"$(Convert-JavacPath $classesDir)`""
)
$lines += $javaFiles | ForEach-Object { "`"$(Convert-JavacPath $_.FullName)`"" }

[System.IO.File]::WriteAllLines($argsFile, $lines, [System.Text.UTF8Encoding]::new($false))

$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$compileOutput = & javac "@$argsFile" 2>&1
$compileExit = $LASTEXITCODE
$ErrorActionPreference = $oldErrorActionPreference
if ($compileExit -ne 0) {
  $compileOutput | ForEach-Object { Write-Host $_ }
  exit $compileExit
}

$testFiles = Get-ChildItem $testSrcRoot -Recurse -Filter *.java | Sort-Object FullName
if ($testFiles.Count -eq 0) {
  throw "No plugin self-test sources found under $testSrcRoot"
}
$testClasspath = "$(Convert-JavacPath $classesDir);$classpath"
$testLines = @(
  "-encoding", "UTF-8",
  "--release", "21",
  "-proc:none",
  "-Xlint:deprecation",
  "-Xlint:unchecked",
  "-Werror",
  "-classpath", "`"$testClasspath`"",
  "-d", "`"$(Convert-JavacPath $testClassesDir)`""
)
$testLines += $testFiles | ForEach-Object { "`"$(Convert-JavacPath $_.FullName)`"" }
[System.IO.File]::WriteAllLines($testArgsFile, $testLines, [System.Text.UTF8Encoding]::new($false))

$ErrorActionPreference = "Continue"
$testCompileOutput = & javac "@$testArgsFile" 2>&1
$testCompileExit = $LASTEXITCODE
$ErrorActionPreference = $oldErrorActionPreference
if ($testCompileExit -ne 0) {
  $testCompileOutput | ForEach-Object { Write-Host $_ }
  exit $testCompileExit
}

$selfTestClasspath = "$(Convert-JavacPath $testClassesDir);$testClasspath"
$selfTests = @(
  "com.observance.watcher.oracle.FlagGateSelfTest",
  "com.observance.watcher.signal.Tier0SelectorSelfTest",
  "com.observance.watcher.util.TextFitSelfTest"
)
foreach ($selfTest in $selfTests) {
  & java -cp $selfTestClasspath $selfTest
  if ($LASTEXITCODE -ne 0) {
    throw "Plugin self-test failed: $selfTest"
  }
}

$pluginText = Get-Content -LiteralPath $pluginSource -Raw
$engineText = Get-Content -LiteralPath $beatEngineSource -Raw
$libraryText = Get-Content -LiteralPath $beatLibrarySource -Raw
$packTrackerText = Get-Content -LiteralPath $packTrackerSource -Raw
if (-not $pluginText.Contains("this.beatEnactor.set(new NoopBeatEnactor(safety))")) {
  throw "Plugin runtime wiring check failed: ObservancePlugin no longer installs the safe initial beat enactor"
}
if (-not $pluginText.Contains("this.beatEngine = new com.observance.watcher.beats.BeatEngine(this)") -or
    -not $pluginText.Contains("beatEngine.activate()")) {
  throw "Plugin runtime wiring check failed: ObservancePlugin no longer activates BeatEngine on enable/reload"
}
if (-not $engineText.Contains("plugin.setBeatEnactor(new RealBeatEnactor(ctx, library, budget))")) {
  throw "Plugin runtime wiring check failed: BeatEngine no longer replaces the no-op enactor with RealBeatEnactor"
}
foreach ($beat in @("RevealBeat", "RoomSwapBeat", "KeeperNpcBeat", "ModeledMobBeat", "SpatialVoiceBeat")) {
  if (-not $libraryText.Contains("register(new $beat")) {
    throw "Plugin beat library check failed: signature beat $beat is no longer registered"
  }
}
if (-not $pluginText.Contains("java.util.function.IntSupplier thresholdActiveRosterSize") -or
    -not $pluginText.Contains("thresholdActiveRosterSize,") -or
    $pluginText.Contains("active-roster supplier unwired")) {
  throw "Plugin runtime wiring check failed: ThresholdVault active-roster supplier is not explicitly wired"
}
if (-not $pluginText.Contains("public com.observance.watcher.signal.ResourcePackTracker resourcePack()")) {
  throw "Plugin resource-pack tracking check failed: ObservancePlugin no longer exposes ResourcePackTracker"
}
foreach ($statusName in @("DOWNLOADED", "FAILED_RELOAD", "INVALID_URL", "DISCARDED")) {
  if (-not $packTrackerText.Contains($statusName)) {
    throw "Plugin resource-pack tracking check failed: status mapping no longer handles $statusName"
  }
}
if (-not $packTrackerText.Contains("statusMappingSelfTest()") -or
    -not $packTrackerText.Contains('case "SUCCESSFULLY_LOADED" -> PackStatus.LOADED')) {
  throw "Plugin resource-pack tracking check failed: status mapping self-test contract is missing"
}
$commandText = Get-Content -LiteralPath (Join-Path $srcRoot "com\observance\watcher\command\ObservanceCommand.java") -Raw
foreach ($statusSurface in @("sendPackStatus(sender)", "pack readiness:", "pack not ready:", "tracker.status")) {
  if (-not $commandText.Contains($statusSurface)) {
    throw "Plugin resource-pack tracking check failed: /obs status no longer surfaces pack readiness ($statusSurface)"
  }
}

Write-Host "plugin compile check: OK - $($javaFiles.Count) sources compile warning-free with $versionText; $($selfTests.Count) self-tests, runtime wiring, and signature beat registration verified"
