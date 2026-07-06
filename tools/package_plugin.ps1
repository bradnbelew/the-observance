param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$pluginRoot = Join-Path $RepoRoot "plugin"
$buildFile = Join-Path $pluginRoot "build.gradle"
$classesDir = Join-Path $pluginRoot "build\check-plugin-classes"
$resourcesDir = Join-Path $pluginRoot "src\main\resources"
$libsDir = Join-Path $pluginRoot "build\libs"

if (!(Test-Path $buildFile)) {
  throw "Plugin build.gradle not found: $buildFile"
}
if (!(Test-Path (Join-Path $resourcesDir "plugin.yml"))) {
  throw "Plugin resources missing plugin.yml: $resourcesDir"
}

$versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
if (!$versionMatch.Success) {
  throw "Could not read plugin version from $buildFile"
}
$version = $versionMatch.Groups[1].Value
$jarPath = Join-Path $libsDir "observance-$version.jar"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $RepoRoot "tools\check_plugin_compile.ps1") -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path $libsDir | Out-Null
if (Test-Path $jarPath) {
  Remove-Item -LiteralPath $jarPath -Force
}

& jar --create --file $jarPath -C $classesDir . -C $resourcesDir .
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

$sha1 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA1).Hash.ToLowerInvariant()
Write-Host "plugin packaged: $jarPath"
Write-Host "plugin sha1: $sha1"
