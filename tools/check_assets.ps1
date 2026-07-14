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
  $runtimeRoots = @()
  foreach ($name in @("pack.mcmeta", "pack.png", "assets", "data")) {
    $path = Join-Path $SourceDir $name
    if (Test-Path $path) {
      $runtimeRoots += Get-Item -LiteralPath $path
    }
  }
  $runtimeFiles = @()
  foreach ($root in $runtimeRoots) {
    if ($root.PSIsContainer) {
      $runtimeFiles += Get-ChildItem -LiteralPath $root.FullName -Recurse -File
    } else {
      $runtimeFiles += $root
    }
  }
  $latestSource = $runtimeFiles | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
  if ($null -ne $latestSource -and $zip.LastWriteTimeUtc -lt $latestSource.LastWriteTimeUtc) {
    Add-Failure "$Label zip is stale: $($zip.Name) older than $($latestSource.FullName)"
  }
  $entries = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
  try {
    if ($null -eq ($entries.Entries | Where-Object { $_.FullName -eq "pack.mcmeta" } | Select-Object -First 1)) {
      Add-Failure "$Label zip does not contain pack.mcmeta at zip root"
    }
    foreach ($entry in $entries.Entries) {
      $name = $entry.FullName.Replace("/", "\").TrimEnd("\")
      if ($name -eq "") { continue }
      $top = $name.Split("\")[0]
      if ($top -notin @("pack.mcmeta", "pack.png", "assets", "data")) {
        Add-Failure "$Label zip contains non-runtime entry: $($entry.FullName)"
      }
    }
  } finally {
    $entries.Dispose()
  }
}

function Test-PackMetadata([string]$McmetaPath, [int[]]$ExpectedMin, [int[]]$ExpectedMax, [string]$Label) {
  $mcmeta = Read-Json $McmetaPath
  if ($null -eq $mcmeta) { return }
  if ($null -eq $mcmeta.pack) {
    Add-Failure "$Label pack.mcmeta missing pack object"
    return
  }

  $actualMin = @($mcmeta.pack.min_format | ForEach-Object { [int]$_ })
  $actualMax = @($mcmeta.pack.max_format | ForEach-Object { [int]$_ })
  $expectedMinText = $ExpectedMin -join "."
  $expectedMaxText = $ExpectedMax -join "."
  $actualMinText = $actualMin -join "."
  $actualMaxText = $actualMax -join "."
  if ($actualMinText -ne $expectedMinText -or $actualMaxText -ne $expectedMaxText) {
    Add-Failure "$Label pack.mcmeta format drifted: expected min/max $expectedMinText/$expectedMaxText, found $actualMinText/$actualMaxText"
  }
}

function Test-DataPack12111([string]$DataPackRoot) {
  $loadTagPath = Join-Path $DataPackRoot "data\minecraft\tags\function\load.json"
  $loadFunctionPath = Join-Path $DataPackRoot "data\observance\function\v5\load.mcfunction"
  $loadTag = Read-Json $loadTagPath
  if ($null -eq $loadTag -or @($loadTag.values) -notcontains "observance:v5/load") {
    Add-Failure "V5 datapack load tag must invoke observance:v5/load: $loadTagPath"
  }
  if (!(Test-Path -LiteralPath $loadFunctionPath)) {
    Add-Failure "V5 datapack marker function is missing: $loadFunctionPath"
  } else {
    $functionText = Get-Content -LiteralPath $loadFunctionPath -Raw
    foreach ($token in @("storage observance:runtime version", "scoreboard players set runtime obs_v5 5")) {
      if (-not $functionText.Contains($token)) {
        Add-Failure "V5 datapack marker function is missing '$token'"
      }
    }
  }

  foreach ($retired in @("dimension", "dimension_type", "worldgen", "advancement")) {
    $retiredPath = Join-Path $DataPackRoot "data\observance\$retired"
    if (Test-Path -LiteralPath $retiredPath) {
      $files = @(Get-ChildItem -LiteralPath $retiredPath -Recurse -File -ErrorAction SilentlyContinue)
      if ($files.Count -gt 0) {
        Add-Failure "V5 datapack contains retired V4 $retired content: $retiredPath"
      }
    }
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

function Test-RuneFont([string]$FontPath, [string]$TexturePath) {
  if (!(Test-Path $FontPath)) {
    Add-Failure "Rune font provider missing: $FontPath"
    return
  }
  if (!(Test-Path $TexturePath)) {
    Add-Failure "Rune font texture missing: $TexturePath"
  }

  $font = Read-Json $FontPath
  if ($null -eq $font) { return }

  $bitmapProviders = @($font.providers | Where-Object { $_.type -eq "bitmap" })
  if ($bitmapProviders.Count -ne 1) {
    Add-Failure "Rune font must have exactly one bitmap provider; found $($bitmapProviders.Count)"
    return
  }

  $provider = $bitmapProviders[0]
  if ([string]$provider.file -ne "observance:font/runes.png") {
    Add-Failure "Rune font provider must reference observance:font/runes.png; found '$($provider.file)'"
  }
  if ([int]$provider.ascent -ne 13 -or [int]$provider.height -ne 16) {
    Add-Failure "Rune font provider ascent/height drifted from the in-client tuned values 13/16"
  }

  $expectedRows = @(
    "ABCDEFGHIJKL",
    "MNOPQRSTUVWX",
    "YZ0123456789"
  )
  $actualRows = @($provider.chars | ForEach-Object { [string]$_ })
  if ($actualRows.Count -ne $expectedRows.Count) {
    Add-Failure "Rune font char grid row count mismatch: expected $($expectedRows.Count), found $($actualRows.Count)"
    return
  }
  for ($i = 0; $i -lt $expectedRows.Count; $i++) {
    if ($actualRows[$i] -ne $expectedRows[$i]) {
      Add-Failure "Rune font char grid row $($i + 1) mismatch: expected '$($expectedRows[$i])', found '$($actualRows[$i])'"
    }
  }

  $flat = ($actualRows -join "")
  $expectedFlat = ($expectedRows -join "")
  if ($flat.Length -ne $expectedFlat.Length) {
    Add-Failure "Rune font char grid length mismatch: expected $($expectedFlat.Length), found $($flat.Length)"
  }
  foreach ($ch in [char[]]$expectedFlat) {
    $count = ([regex]::Matches([regex]::Escape($flat), [regex]::Escape([string]$ch))).Count
    if ($count -ne 1) {
      Add-Failure "Rune font char grid must contain '$ch' exactly once; found $count"
    }
  }
}

function Test-SpoilerAssetNames([string[]]$Roots) {
  $forbidden = @(
    "averyn",
    "seventh-name",
    "threshold-coordinate",
    "true-walk-arrive",
    "service_role",
    "supabase_service",
    "discord_token",
    "25569"
  )

  foreach ($root in $Roots) {
    if (-not (Test-Path -LiteralPath $root)) { continue }
    Get-ChildItem -LiteralPath $root -Recurse -Force | ForEach-Object {
      $relative = $_.FullName.Substring($root.Length).TrimStart("\", "/").ToLowerInvariant()
      foreach ($needle in $forbidden) {
        if ($relative.Contains($needle)) {
          Add-Failure "Player-facing runtime asset path contains spoiler/secret marker '$needle': $($_.FullName)"
        }
      }
    }
  }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$datapackSource = Join-Path $RepoRoot "datapack\observance"
$resourcepackSource = Join-Path $RepoRoot "resourcepack"
$resourceNamespace = Join-Path $resourcepackSource "assets\observance"
$dashboardPublic = Join-Path $RepoRoot "dashboard\public"

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

Test-RuneFont `
  (Join-Path $fontRoot "runes.json") `
  (Join-Path $resourceNamespace "textures\font\runes.png")

Test-SpoilerAssetNames @($datapackSource, $resourcepackSource, $dashboardPublic)

Test-PackMetadata (Join-Path $datapackSource "pack.mcmeta") @(94, 1) @(94, 1) "datapack"
Test-PackMetadata (Join-Path $resourcepackSource "pack.mcmeta") @(75, 0) @(75, 0) "resourcepack"
Test-DataPack12111 $datapackSource

Test-PackSource $datapackSource (Join-Path $RepoRoot "observance-datapack.zip") "datapack"
Test-PackSource $resourcepackSource (Join-Path $RepoRoot "observance-resourcepack.zip") "resourcepack"

$resourcepackZip = Join-Path $RepoRoot "observance-resourcepack.zip"
$resourcepackSha1 = (Get-FileHash -LiteralPath $resourcepackZip -Algorithm SHA1).Hash.ToLowerInvariant()
$pluginConfig = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
if (Test-Path $pluginConfig) {
  $configText = Get-Content -LiteralPath $pluginConfig -Raw
  $urlMatch = [regex]::Match($configText, "(?ms)^resource-pack:\s.*?^\s+url:\s+`"([^`"]*)`"")
  $shaMatch = [regex]::Match($configText, "(?ms)^resource-pack:\s.*?^\s+sha1:\s+`"([^`"]*)`"")
  $promptMatch = [regex]::Match($configText, "(?ms)^resource-pack:\s.*?^\s+prompt:\s+`"([^`"]*)`"")
  $configUrl = if ($urlMatch.Success) { $urlMatch.Groups[1].Value.Trim() } else { "" }
  $configSha = if ($shaMatch.Success) { $shaMatch.Groups[1].Value.Trim().ToLowerInvariant() } else { "" }
  $configPrompt = if ($promptMatch.Success) { $promptMatch.Groups[1].Value.Trim() } else { "" }
  if ($configUrl -ne "" -and $configSha -eq "") {
    Add-Failure "resource-pack.url is set in plugin config but resource-pack.sha1 is blank"
  }
  if ($configSha -ne "" -and $configSha -ne $resourcepackSha1) {
    Add-Failure "plugin config resource-pack.sha1 ($configSha) does not match observance-resourcepack.zip ($resourcepackSha1)"
  }
  if ($configPrompt -eq "" -or $configPrompt -match '(?i)\b(todo|tbd|placeholder)\b') {
    Add-Failure "resource-pack.prompt must be a final one-line player-facing prompt"
  }
  if ($configPrompt -ne "" -and ($configPrompt -notmatch '(?i)alphabet' -or $configPrompt -notmatch '(?i)(voice|hear|sound)')) {
    Add-Failure "resource-pack.prompt must explain both why the alphabet and audio pack matter"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "asset check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "asset check: OK - datapack/resourcepack JSON, format metadata, references, and zips verified"
Write-Host "rune font: OK - observance:runes covers A-Z and 0-9 in the generated atlas order"
Write-Host "resourcepack sha1: $resourcepackSha1"
