param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$Url = "",
  [string]$ExpectedSha1 = "",
  [string]$ConfigPath = "plugin\src\main\resources\config.yml",
  [string]$ResourcePackZip = "observance-resourcepack.zip",
  [int]$TimeoutSec = 60
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRoot([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Clean([string]$Value) {
  if ($null -eq $Value) { return "" }
  return ($Value -replace '\s+#.*$', '').Trim().Trim('"')
}

function Resource-Pack-Value([string]$Text, [string]$Key) {
  $pattern = "(?ms)^resource-pack:\s.*?^\s+$([regex]::Escape($Key)):\s+`"([^`"]*)`""
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return Clean $match.Groups[1].Value }
  return ""
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$configFull = Resolve-UnderRoot $repoFull $ConfigPath
$zipFull = Resolve-UnderRoot $repoFull $ResourcePackZip

foreach ($file in @($configFull, $zipFull)) {
  if (-not (Test-Path $file)) {
    throw "hosted resource-pack check: missing required file: $file"
  }
}

$config = Get-Content -LiteralPath $configFull -Raw
if ([string]::IsNullOrWhiteSpace($Url)) {
  $Url = Resource-Pack-Value $config "url"
}
if ([string]::IsNullOrWhiteSpace($ExpectedSha1)) {
  $ExpectedSha1 = Resource-Pack-Value $config "sha1"
}
if ([string]::IsNullOrWhiteSpace($ExpectedSha1)) {
  $ExpectedSha1 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA1).Hash.ToLowerInvariant()
}

$uri = $null
if (-not [System.Uri]::TryCreate($Url, [System.UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne "https") {
  throw "hosted resource-pack check: URL must be an absolute HTTPS URL: $Url"
}
if ($uri.AbsolutePath -notmatch '\.zip$') {
  throw "hosted resource-pack check: URL should point directly at .zip bytes: $Url"
}
if ($ExpectedSha1 -notmatch '^[0-9a-f]{40}$') {
  throw "hosted resource-pack check: expected SHA1 must be 40 lowercase hex characters: $ExpectedSha1"
}

$localSha1 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA1).Hash.ToLowerInvariant()
if ($ExpectedSha1 -ne $localSha1) {
  throw "hosted resource-pack check: expected SHA1 $ExpectedSha1 does not match local $ResourcePackZip SHA1 $localSha1"
}

$tempDir = Join-Path $repoFull "build\hosted-resource-pack-check"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
$downloadPath = Join-Path $tempDir "observance-resourcepack.hosted.zip"
if (Test-Path $downloadPath) {
  Remove-Item -LiteralPath $downloadPath -Force
}

try {
  Invoke-WebRequest -Uri $uri.AbsoluteUri -OutFile $downloadPath -TimeoutSec $TimeoutSec -UseBasicParsing | Out-Null
  if (-not (Test-Path $downloadPath)) {
    throw "hosted resource-pack check: download did not create a file"
  }
  $bytes = [System.IO.File]::ReadAllBytes($downloadPath)
  if ($bytes.Length -lt 4 -or $bytes[0] -ne 0x50 -or $bytes[1] -ne 0x4B) {
    throw "hosted resource-pack check: downloaded file is not a zip payload"
  }
  $hostedSha1 = (Get-FileHash -LiteralPath $downloadPath -Algorithm SHA1).Hash.ToLowerInvariant()
  if ($hostedSha1 -ne $ExpectedSha1) {
    throw "hosted resource-pack check: hosted SHA1 $hostedSha1 does not match expected $ExpectedSha1"
  }
  Write-Host "hosted resource-pack check: OK - hosted zip matches $ExpectedSha1"
} finally {
  if (Test-Path $downloadPath) {
    Remove-Item -LiteralPath $downloadPath -Force
  }
}
