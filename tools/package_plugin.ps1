param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$pluginRoot = Join-Path $repoFull "plugin"
$buildFile = Join-Path $pluginRoot "build.gradle"
$gradlew = Join-Path $pluginRoot "gradlew.bat"
$resourcesDir = Join-Path $pluginRoot "src\main\resources"
$libsDir = Join-Path $pluginRoot "build\libs"
$jarVerifier = Join-Path $repoFull "tools\check_plugin_jar.ps1"
$deployManifestWriter = Join-Path $repoFull "tools\write_deploy_manifest.ps1"

foreach ($required in @($buildFile, $gradlew, $jarVerifier, $deployManifestWriter)) {
  if (!(Test-Path -LiteralPath $required -PathType Leaf)) {
    throw "Plugin production package prerequisite is missing: $required"
  }
}
if (!(Test-Path -LiteralPath (Join-Path $resourcesDir "plugin.yml") -PathType Leaf)) {
  throw "Plugin resources are missing plugin.yml: $resourcesDir"
}

$versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
if (!$versionMatch.Success) {
  throw "Could not read plugin version from $buildFile"
}
$version = $versionMatch.Groups[1].Value
if ($version -ne "0.5.0") {
  throw "Production package requires plugin version 0.5.0; build.gradle declares $version"
}
$jarPath = Join-Path $libsDir "observance-$version.jar"

foreach ($retiredName in @("deep-hold-d05-shelf.json", "deep-hold-lock-books.json")) {
  if (Test-Path -LiteralPath (Join-Path $resourcesDir $retiredName)) {
    throw "Retired V4 resource must not ship: $retiredName"
  }
}

# The Gradle project is the sole production compiler and packager. It pins the Java 21 toolchain,
# exact compile dependencies, complete V5 self-test graph, processed resources, and reproducible JAR
# ordering/timestamps. Run the wrapper from its project root; never replace this with a cache-wide
# javac classpath or manually re-zip classes.
Push-Location $pluginRoot
try {
  & $gradlew clean check build --no-daemon
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

$deployJars = @(Get-ChildItem -LiteralPath $libsDir -Filter "observance-*.jar" -File -ErrorAction SilentlyContinue)
if ($deployJars.Count -ne 1 -or $deployJars[0].Name -ne "observance-0.5.0.jar") {
  throw "Deploy jar invariant failed: expected only observance-0.5.0.jar, found $($deployJars.Name -join ', ')"
}
if (!(Test-Path -LiteralPath $jarPath -PathType Leaf)) {
  throw "Gradle reported success but the production plugin JAR is missing: $jarPath"
}

# Read the exact Gradle output back. This independently enforces byte parity for every authority,
# including the 108 evidence appearances and all nine manifest-owned map PNGs, and rejects extras.
& powershell -NoProfile -ExecutionPolicy Bypass -File $jarVerifier -RepoRoot $repoFull
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

$sha1 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA1).Hash.ToLowerInvariant()
$sha256 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "plugin packaged by verified Gradle build: $jarPath"
Write-Host "plugin sha1: $sha1"
Write-Host "plugin sha256: $sha256"

& powershell -NoProfile -ExecutionPolicy Bypass -File $deployManifestWriter -RepoRoot $repoFull
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}
