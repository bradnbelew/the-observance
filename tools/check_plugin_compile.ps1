param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$pluginRoot = Join-Path $RepoRoot "plugin"
$srcRoot = Join-Path $pluginRoot "src\main\java"
$pluginSource = Join-Path $srcRoot "com\observance\watcher\ObservancePlugin.java"
$beatEngineSource = Join-Path $srcRoot "com\observance\watcher\beats\BeatEngine.java"
$buildRoot = Join-Path $pluginRoot "build"
$classesDir = Join-Path $buildRoot "check-plugin-classes"
$argsFile = Join-Path $buildRoot "check-plugin-javac.args"
$gradleCache = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1"

if (!(Test-Path $srcRoot)) {
  throw "Plugin source directory not found: $srcRoot"
}
foreach ($file in @($pluginSource, $beatEngineSource)) {
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
if (Test-Path $classesDir) {
  Remove-Item -LiteralPath $classesDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

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
  "-nowarn",
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

$pluginText = Get-Content -LiteralPath $pluginSource -Raw
$engineText = Get-Content -LiteralPath $beatEngineSource -Raw
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

Write-Host "plugin compile check: OK - $($javaFiles.Count) source files compiled with $versionText and real beat enactor wiring verified"
