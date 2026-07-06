param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Read-Json([string]$Path) {
  try {
    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
  } catch {
    Add-Failure "Invalid JSON: $Path ($($_.Exception.Message))"
    return $null
  }
}

function Test-PackSource([string]$SourceDir, [string]$ZipPath, [string]$Label) {
  if (!(Test-Path (Join-Path $SourceDir "pack.mcmeta"))) {
    Add-Failure "$Label source missing pack.mcmeta: $SourceDir"
  }
  if (!(Test-Path $ZipPath)) {
    Add-Failure "$Label zip missing: $ZipPath"
    return
  }
  $zip = Get-Item -LiteralPath $ZipPath
  $latestSource = Get-ChildItem -LiteralPath $SourceDir -Recurse -File |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1
  if ($null -ne $latestSource -and $zip.LastWriteTimeUtc -lt $latestSource.LastWriteTimeUtc) {
    Add-Failure "$Label zip is stale: $($zip.Name) older than $($latestSource.FullName)"
  }
  $entries = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
  try {
    if ($null -eq ($entries.Entries | Where-Object { $_.FullName -eq "pack.mcmeta" } | Select-Object -First 1)) {
      Add-Failure "$Label zip does not contain pack.mcmeta at zip root"
    }
  } finally {
    $entries.Dispose()
  }
}

function Test-JsonTree([string]$Root) {
  Get-ChildItem -LiteralPath $Root -Recurse -Filter *.json | ForEach-Object {
    [void](Read-Json $_.FullName)
  }
  Get-ChildItem -LiteralPath $Root -Recurse -Filter *.mcmeta | ForEach-Object {
    [void](Read-Json $_.FullName)
  }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$datapackSource = Join-Path $RepoRoot "datapack\observance"
$resourcepackSource = Join-Path $RepoRoot "resourcepack"
$resourceNamespace = Join-Path $resourcepackSource "assets\observance"

Test-JsonTree (Join-Path $RepoRoot "datapack")
Test-JsonTree $resourcepackSource

$sounds = Read-Json (Join-Path $resourceNamespace "sounds.json")
if ($null -ne $sounds) {
  foreach ($soundKey in $sounds.PSObject.Properties.Name) {
    $entry = $sounds.$soundKey
    foreach ($sound in @($entry.sounds)) {
      $name = if ($sound -is [string]) { $sound } else { $sound.name }
      if ([string]::IsNullOrWhiteSpace($name)) {
        Add-Failure "Sound entry '$soundKey' has no name"
        continue
      }
      $pathPart = $name
      if ($pathPart.StartsWith("observance:")) {
        $pathPart = $pathPart.Substring("observance:".Length)
      }
      $ogg = Join-Path (Join-Path $resourceNamespace "sounds") ($pathPart.Replace("/", "\") + ".ogg")
      if (!(Test-Path $ogg)) {
        Add-Failure "Sound entry '$soundKey' references missing OGG: $name -> $ogg"
      }
    }
  }
}

$fontRoot = Join-Path $resourceNamespace "font"
Get-ChildItem -LiteralPath $fontRoot -Filter *.json -ErrorAction SilentlyContinue | ForEach-Object {
  $font = Read-Json $_.FullName
  if ($null -eq $font) { return }
  foreach ($provider in @($font.providers)) {
    if ($provider.type -ne "bitmap") { continue }
    $file = [string]$provider.file
    if ([string]::IsNullOrWhiteSpace($file)) {
      Add-Failure "Bitmap font provider in $($_.FullName) has no file"
      continue
    }
    $texturePath = $file
    if ($texturePath.StartsWith("observance:")) {
      $texturePath = $texturePath.Substring("observance:".Length)
    }
    $png = Join-Path (Join-Path $resourceNamespace "textures") $texturePath.Replace("/", "\")
    if (!(Test-Path $png)) {
      Add-Failure "Bitmap font provider in $($_.FullName) references missing texture: $file -> $png"
    }
  }
}

Test-PackSource $datapackSource (Join-Path $RepoRoot "observance-datapack.zip") "datapack"
Test-PackSource $resourcepackSource (Join-Path $RepoRoot "observance-resourcepack.zip") "resourcepack"

$resourcepackZip = Join-Path $RepoRoot "observance-resourcepack.zip"
$resourcepackSha1 = (Get-FileHash -LiteralPath $resourcepackZip -Algorithm SHA1).Hash.ToLowerInvariant()
$pluginConfig = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
if (Test-Path $pluginConfig) {
  $configText = Get-Content -LiteralPath $pluginConfig -Raw
  $urlMatch = [regex]::Match($configText, "(?ms)^resource-pack:\s.*?^\s+url:\s+`"([^`"]*)`"")
  $shaMatch = [regex]::Match($configText, "(?ms)^resource-pack:\s.*?^\s+sha1:\s+`"([^`"]*)`"")
  $configUrl = if ($urlMatch.Success) { $urlMatch.Groups[1].Value.Trim() } else { "" }
  $configSha = if ($shaMatch.Success) { $shaMatch.Groups[1].Value.Trim().ToLowerInvariant() } else { "" }
  if ($configUrl -ne "" -and $configSha -eq "") {
    Add-Failure "resource-pack.url is set in plugin config but resource-pack.sha1 is blank"
  }
  if ($configSha -ne "" -and $configSha -ne $resourcepackSha1) {
    Add-Failure "plugin config resource-pack.sha1 ($configSha) does not match observance-resourcepack.zip ($resourcepackSha1)"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "asset check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "asset check: OK - datapack/resourcepack JSON, references, and zips verified"
Write-Host "resourcepack sha1: $resourcepackSha1"
