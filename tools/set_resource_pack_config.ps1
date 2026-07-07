param(
  [Parameter(Mandatory = $true)]
  [string]$Url,
  [string]$RepoRoot = "",
  [string]$ConfigPath = "plugin\src\main\resources\config.yml",
  [string]$ResourcePackZip = "observance-resourcepack.zip",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRoot([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Replace-ResourcePackValue([string]$Text, [string]$Key, [string]$Value) {
  $pattern = "(?ms)(^resource-pack:\s.*?^\s+$([regex]::Escape($Key)):\s+)`"[^`"]*`""
  $replacement = '${1}"' + ($Value -replace '\$', '$$') + '"'
  $updated = [regex]::Replace($Text, $pattern, $replacement, 1)
  if ($updated -eq $Text) {
    throw "Could not find resource-pack.$Key in config"
  }
  return $updated
}

$uri = $null
if (-not [System.Uri]::TryCreate($Url, [System.UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne "https") {
  throw "Resource-pack URL must be an absolute HTTPS URL: $Url"
}
if ($uri.AbsolutePath -notmatch '\.zip$') {
  throw "Resource-pack URL should point directly at the hosted .zip bytes: $Url"
}

$scriptRoot = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
  Split-Path -Parent $MyInvocation.MyCommand.Path
} else {
  $PSScriptRoot
}
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
  $RepoRoot = (Resolve-Path (Join-Path $scriptRoot "..")).Path
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$configFull = Resolve-UnderRoot $repoFull $ConfigPath
$zipFull = Resolve-UnderRoot $repoFull $ResourcePackZip

if (!(Test-Path $configFull)) {
  throw "Config file not found: $configFull"
}
if (!(Test-Path $zipFull)) {
  throw "Resource-pack zip not found: $zipFull"
}

$sha1 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA1).Hash.ToLowerInvariant()
$original = Get-Content -LiteralPath $configFull -Raw
$updated = Replace-ResourcePackValue $original "url" $Url
$updated = Replace-ResourcePackValue $updated "sha1" $sha1

Write-Host "resource-pack.url:  $Url"
Write-Host "resource-pack.sha1: $sha1"
Write-Host "config:             $configFull"
Write-Host "zip:                $zipFull"

if ($DryRun) {
  Write-Host "dry run: config not changed"
  exit 0
}

[System.IO.File]::WriteAllText($configFull, $updated, [System.Text.Encoding]::UTF8)
Write-Host "resource-pack config updated"

$defaultConfig = [System.IO.Path]::GetFullPath((Join-Path $repoFull "plugin\src\main\resources\config.yml"))
if ($configFull.Equals($defaultConfig, [System.StringComparison]::OrdinalIgnoreCase)) {
  & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoFull "tools\write_deploy_manifest.ps1") -RepoRoot $repoFull
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
}
