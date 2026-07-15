param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$WorkRoot = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$contentRoot = Join-Path $RepoRoot "dashboard\content\the-hold-v5"
$zipFile = Join-Path $contentRoot "the-hold.zip"
$sha1File = Join-Path $contentRoot "the-hold.sha1"
$pluginConfigFile = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function RequireText([string]$Haystack, [string]$Needle, [string]$Label) {
  if ($Haystack.IndexOf($Needle, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "missing ${Label}: $Needle"
  }
}

# The active invitation is packaged so launch does not depend on a separate server-local edit.
if (-not (Test-Path -LiteralPath $pluginConfigFile -PathType Leaf)) {
  Fail "missing packaged plugin config: plugin\src\main\resources\config.yml"
} else {
  $pluginConfig = Get-Content -LiteralPath $pluginConfigFile -Raw
  $inviteMatch = [regex]::Match(
    $pluginConfig,
    '(?im)^\s{2}discord-invite-url:\s*["''](?<url>https://(?:discord\.gg|discord\.com/invite)/[A-Za-z0-9_-]+)["'']\s*(?:#.*)?$'
  )
  if (-not $inviteMatch.Success) {
    Fail "packaged plugin config does not contain one valid HTTPS Discord invitation"
  } else {
    try {
      $inviteUri = [System.Uri]$inviteMatch.Groups['url'].Value
      if (-not $inviteUri.IsAbsoluteUri -or $inviteUri.Scheme -ne 'https') {
        Fail "packaged Discord invitation is not absolute HTTPS"
      }
    } catch {
      Fail "packaged Discord invitation is malformed"
    }
  }
}

if (-not (Test-Path -LiteralPath $zipFile)) {
  throw "hold prologue check: missing dashboard\content\the-hold-v5\the-hold.zip"
}
if (-not (Test-Path -LiteralPath $sha1File)) {
  throw "hold prologue check: missing dashboard\content\the-hold-v5\the-hold.sha1"
}

$actualSha1 = (Get-FileHash -LiteralPath $zipFile -Algorithm SHA1).Hash.ToLowerInvariant()
$declaredSha1 = ((Get-Content -LiteralPath $sha1File -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
if ($actualSha1 -ne $declaredSha1) {
  Fail "checksum drift: declared $declaredSha1, actual $actualSha1"
}

$workRoot = if ([string]::IsNullOrWhiteSpace($WorkRoot)) {
  Join-Path ([System.IO.Path]::GetTempPath()) ("observance-check-hold-v51-" + [System.Guid]::NewGuid().ToString("N"))
} else {
  [System.IO.Path]::GetFullPath($WorkRoot)
}
if (Test-Path -LiteralPath $workRoot) {
  Remove-Item -LiteralPath $workRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $workRoot | Out-Null

try {
  [System.IO.Compression.ZipFile]::ExtractToDirectory($zipFile, $workRoot)
  $worldRoot = Join-Path $workRoot "the-hold"
  $packRoot = Join-Path $worldRoot "datapacks\the_hold"
  $functionRoot = Join-Path $packRoot "data\the_hold\function"

  $expectedEntries = @(
    "level.dat",
    "level.dat_old",
    "datapacks\the_hold\pack.mcmeta",
    "datapacks\the_hold\data\minecraft\tags\function\load.json",
    "datapacks\the_hold\data\minecraft\tags\function\tick.json",
    "datapacks\the_hold\data\the_hold\function\load.mcfunction",
    "datapacks\the_hold\data\the_hold\function\spawn.mcfunction",
    "datapacks\the_hold\data\the_hold\function\return.mcfunction",
    "datapacks\the_hold\data\the_hold\function\tick.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\all.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\receiving.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\records.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\dispatch.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\passages.mcfunction"
  )
  foreach ($relative in $expectedEntries) {
    if (-not (Test-Path -LiteralPath (Join-Path $worldRoot $relative))) {
      Fail "missing playable world entry: $relative"
    }
  }

  $allFiles = @(Get-ChildItem -LiteralPath $worldRoot -Recurse -File)
  if ($allFiles.Count -ne $expectedEntries.Count) {
    Fail "world contains $($allFiles.Count) files instead of $($expectedEntries.Count)"
  }
  foreach ($file in $allFiles) {
    $relative = $file.FullName.Substring($worldRoot.Length + 1)
    if ($relative -notin $expectedEntries) {
      Fail "stale or unexpected world entry: $relative"
    }
  }

  if ((Get-Item -LiteralPath (Join-Path $worldRoot "level.dat")).Length -lt 1000) {
    Fail "level.dat is missing or truncated"
  }

  try {
    $packMeta = Get-Content -LiteralPath (Join-Path $packRoot "pack.mcmeta") -Raw | ConvertFrom-Json
    if ($packMeta.pack.min_format -ne 94 -or $packMeta.pack.max_format -ne 94) {
      Fail "pack.mcmeta must retain the played format 94"
    }
  } catch {
    Fail "pack.mcmeta is invalid: $($_.Exception.Message)"
  }

  $functionFiles = @(Get-ChildItem -LiteralPath $functionRoot -Recurse -File -Filter "*.mcfunction")
  if ($functionFiles.Count -ne 9) {
    Fail "expected 9 compact functions, found $($functionFiles.Count)"
  }
  $functionText = ($functionFiles | Sort-Object FullName | ForEach-Object {
    Get-Content -LiteralPath $_.FullName -Raw
  }) -join "`n"

  foreach ($evidence in @(
    "gamemode adventure @s",
    "function the_hold:build/receiving",
    "function the_hold:build/records",
    "function the_hold:build/dispatch",
    "3-6-2\n\n1-4-3\n\n4-2-1\n\n2-5-4",
    "Relay tag OI",
    "Fault moved to Z",
    "Socket SN failed",
    "KER line stable",
    "commercial DNS class",
    "standard host/service separator",
    "RETURN 2",
    "RETURN 5",
    "RETURN 6",
    "RETURN 9"
  )) {
    RequireText $functionText $evidence "required annex evidence"
  }

  foreach ($forbidden in @(
    "discord",
    "placeholder",
    "TODO",
    "TBD",
    "build/domestic",
    "build/cistern",
    "build/lampworks",
    "build/register",
    "triggers/"
  )) {
    if ($functionText.IndexOf($forbidden, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Fail "stale or meta-facing content remains: $forbidden"
    }
  }
  if ([regex]::IsMatch(
    $functionText,
    "\b[a-z0-9.-]+\.(com|net|org|gg|io)\s*:\s*[0-9]{2,5}\b",
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
  )) {
    Fail "the world exposes a complete destination instead of separated evidence"
  }

  $lecternCount = ([regex]::Matches($functionText, "minecraft:lectern\[facing=")).Count
  $bookCount = ([regex]::Matches($functionText, "data merge block")).Count
  if ($lecternCount -ne 8 -or $bookCount -ne 8) {
    Fail "expected eight oriented, filled lecterns; found $lecternCount lecterns and $bookCount books"
  }

  foreach ($opening in @(14, 15, 19, 20, 58, 59, 63, 64)) {
    RequireText $functionText "fill $opening 240 -3 $opening 246 3 minecraft:air" "two-way opening at x=$opening"
  }

  foreach ($file in $functionFiles) {
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName) {
      $lineNumber++
      if ($line -match "^fill\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+") {
        $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]; $z1 = [int]$Matches[3]
        $x2 = [int]$Matches[4]; $y2 = [int]$Matches[5]; $z2 = [int]$Matches[6]
        $volume = ([Math]::Abs($x2 - $x1) + 1) * ([Math]::Abs($y2 - $y1) + 1) * ([Math]::Abs($z2 - $z1) + 1)
        if ($volume -gt 32768) {
          Fail "$($file.Name):$lineNumber exceeds the vanilla /fill volume ($volume)"
        }
        if ([Math]::Min($y1, $y2) -lt 238 -or [Math]::Max($y1, $y2) -gt 250) {
          Fail "$($file.Name):$lineNumber leaves the proven vertical envelope"
        }
      }
    }
  }

  if ($failures.Count -gt 0) {
    Write-Host "hold prologue check: FAILED"
    foreach ($failure in $failures) {
      Write-Host "  - $failure"
    }
    exit 1
  }

  $zip = Get-Item -LiteralPath $zipFile
  Write-Host "hold prologue check: OK - playable 3-room annex, packaged Discord invite, 8 filled books, format 94"
  Write-Host "hold prologue check: $($functionFiles.Count) functions; $($zip.Length) bytes; SHA1 $actualSha1"
} finally {
  if (Test-Path -LiteralPath $workRoot) {
    Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
  }
}
