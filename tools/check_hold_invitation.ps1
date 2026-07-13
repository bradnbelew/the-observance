param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$WorkRoot = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$zipFile = Join-Path $RepoRoot "dashboard\public\the-hold\the-hold.zip"
$stagingFile = Join-Path $RepoRoot "design\MANUAL-MEDIA-STAGING.md"
$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function RequireText([string]$Haystack, [string]$Needle, [string]$Label) {
  if ($Haystack.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "missing ${Label}: $Needle"
  }
}

if (-not (Test-Path -LiteralPath $zipFile)) {
  throw "hold prologue check: missing dashboard\public\the-hold\the-hold.zip"
}

$workRoot = if ([string]::IsNullOrWhiteSpace($WorkRoot)) {
  Join-Path ([System.IO.Path]::GetTempPath()) ("observance-check-hold-prologue-" + [System.Guid]::NewGuid().ToString("N"))
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

  foreach ($relative in @(
    "level.dat",
    "datapacks\the_hold\pack.mcmeta",
    "datapacks\the_hold\data\minecraft\tags\function\load.json",
    "datapacks\the_hold\data\minecraft\tags\function\tick.json",
    "datapacks\the_hold\data\the_hold\function\load.mcfunction",
    "datapacks\the_hold\data\the_hold\function\tick.mcfunction",
    "datapacks\the_hold\data\the_hold\function\spawn.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\all.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\arrival.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\domestic.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\cistern.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\lampworks.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\register.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\dispatch.mcfunction",
    "datapacks\the_hold\data\the_hold\function\build\passages.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\record.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\domestic.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\cistern.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\lamp_change.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\register.mcfunction",
    "datapacks\the_hold\data\the_hold\function\triggers\final.mcfunction"
  )) {
    if (-not (Test-Path -LiteralPath (Join-Path $worldRoot $relative))) {
      Fail "missing production world entry: $relative"
    }
  }

  $packMetaFile = Join-Path $packRoot "pack.mcmeta"
  if (Test-Path -LiteralPath $packMetaFile) {
    try {
      $packMeta = Get-Content -LiteralPath $packMetaFile -Raw | ConvertFrom-Json
      if ($packMeta.pack.min_format -ne 94 -or $packMeta.pack.max_format -ne 94) {
        Fail "pack.mcmeta must target the production Paper/Minecraft 1.21.11 data format 94"
      }
    } catch {
      Fail "pack.mcmeta is not valid JSON: $($_.Exception.Message)"
    }
  }

  $functionFiles = @(Get-ChildItem -LiteralPath $functionRoot -Recurse -File -Filter "*.mcfunction")
  $functionText = ($functionFiles | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join [Environment]::NewLine

  foreach ($evidence in @(
    "THE RECORD",
    "the record is kept in more than one place",
    "six hands carried this copy out",
    "THE EMPTY PLACE",
    "WHERE THE REEDS FOLD",
    "WAKE THE LAMP",
    "REGISTER 0 / 7",
    "the record keeps",
    "HANDOFF",
    "dispatch fragment i:\n\nSN",
    "dispatch fragment ii:\n\nOI",
    "dispatch fragment iii:\n\nKER",
    "DISPATCH IV",
    'text:"Z"',
    "service digit 1 / 2",
    "service digit 2 / 5",
    "service digit 3 / 5",
    "service digit 4 / 6",
    "service digit 5 / 9",
    "service separator / :",
    "ending: common web",
    "assemble host + ending + service",
    "when all are present, say kept",
    "#the-record"
  )) {
    RequireText $functionText $evidence "production evidence"
  }

  foreach ($forbidden in @(
    "TODO",
    "TBD",
    "placeholder",
    "lorem ipsum",
    "shelf 3 page 1",
    "generated/line_",
    "minecraft:chain",
    "gamerule doDaylightCycle",
    "gamerule doWeatherCycle",
    "gamerule doMobSpawning"
  )) {
    if ($functionText.IndexOf($forbidden, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Fail "forbidden stale/placeholder content remains: $forbidden"
    }
  }

  if ($functionText -match 'pages:\s*\[\s*[''"]\s*\{\s*\\?"text\\?"') {
    Fail "written-book pages still contain quoted JSON and will render literal {text:...}"
  }
  if ($functionText.Contains('\\n')) {
    Fail "written-book text still contains double-escaped newlines and may render literal \\n in Minecraft"
  }
  if (($functionText | Select-String -Pattern "pages:\[\{text:" -AllMatches).Matches.Count -lt 14) {
    Fail "expected at least 14 complete structured written-book components"
  }

  $rawEndpointPatterns = @(
    "\b[a-z0-9.-]+\.(com|net|org|gg|io)\s*:\s*[0-9]{2,5}\b",
    "\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\s*:\s*[0-9]{2,5}\b"
  )
  foreach ($pattern in $rawEndpointPatterns) {
    if ([regex]::IsMatch($functionText, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
      Fail "the prologue exposes a raw server endpoint instead of the Copperline directory puzzle"
    }
  }

  foreach ($orientation in @(
    "chiseled_bookshelf[facing=east]",
    "chiseled_bookshelf[facing=west]",
    "chiseled_bookshelf[facing=north]"
  )) {
    RequireText $functionText $orientation "inward-facing bookshelf orientation"
  }
  RequireText $functionText "item replace block -16 240 10 container.0 with minecraft:written_book" "filled west archive shelf"
  RequireText $functionText "item replace block 16 240 10 container.0 with minecraft:written_book" "filled east archive shelf"
  RequireText $functionText "setblock 0 240 259 minecraft:chiseled_bookshelf[facing=north]" "intentional empty seventh shelf"

  foreach ($position in @(
    "setblock 0 240 18 minecraft:lectern[facing=north,has_book=true]",
    "setblock 0 240 79 minecraft:lectern[facing=north,has_book=true]",
    "setblock 0 239 142 minecraft:lectern[facing=north,has_book=true]",
    "setblock 0 240 252 minecraft:lectern[facing=north,has_book=true]",
    "setblock 0 240 320 minecraft:lectern[facing=north,has_book=true]"
  )) {
    RequireText $functionText $position "player-facing lectern"
  }

  $gateRoutes = @(
    @{ Gate = 28; CorridorNear = 29; CorridorFar = 42; NextRoom = 43 },
    @{ Gate = 88; CorridorNear = 89; CorridorFar = 102; NextRoom = 103 },
    @{ Gate = 150; CorridorNear = 151; CorridorFar = 163; NextRoom = 164 },
    @{ Gate = 207; CorridorNear = 208; CorridorFar = 220; NextRoom = 221 },
    @{ Gate = 265; CorridorNear = 266; CorridorFar = 278; NextRoom = 279 }
  )
  foreach ($route in $gateRoutes) {
    $gate = $route.Gate
    RequireText $functionText "fill -2 240 $gate 2 245 $gate minecraft:polished_blackstone_bricks" "closed gate $gate"
    RequireText $functionText "fill -2 240 $gate 2 246 $gate minecraft:air" "controlled gate opening $gate"
    RequireText $functionText "fill -2 240 $($route.CorridorNear) 2 246 $($route.CorridorNear) minecraft:air" "open corridor entry behind gate $gate"
    RequireText $functionText "fill -2 240 $($route.CorridorFar) 2 246 $($route.CorridorFar) minecraft:air" "open corridor exit beyond gate $gate"
    RequireText $functionText "fill -2 240 $($route.NextRoom) 2 246 $($route.NextRoom) minecraft:air" "open next-room threshold beyond gate $gate"
  }

  foreach ($route in @(
    "tag @s add hold_1",
    "tag @s add hold_2",
    "tag @s add hold_3",
    "minecraft:lever[powered=true]",
    "tag @s add hold_4",
    "tag @s add hold_5",
    "tag @s add hold_done"
  )) {
    RequireText $functionText $route "ordered progression"
  }

  if (($functionText | Select-String -Pattern "minecraft:reinforced_deepslate" -AllMatches).Matches.Count -lt 6) {
    Fail "each of the six rooms must have a physical reinforced roof"
  }
  RequireText $functionText "fill -3 238 104 3 238 149 minecraft:polished_deepslate" "continuous dry cistern bridge"
  foreach ($recovery in @(
    "setblock -4 238 126 minecraft:ladder[facing=west]",
    "setblock 4 238 126 minecraft:ladder[facing=east]",
    "setblock -4 238 140 minecraft:ladder[facing=west]",
    "setblock 4 238 140 minecraft:ladder[facing=east]"
  )) {
    RequireText $functionText $recovery "adventure-mode cistern recovery"
  }
  RequireText $functionText "fill -11 240 48 -11 243 52 minecraft:air" "accessible west bed alcove"
  RequireText $functionText "fill 11 240 48 11 243 52 minecraft:air" "accessible east bed alcove"
  RequireText $functionText "fill -2 240 252 2 243 252 minecraft:air" "accessible seventh register alcove"
  RequireText $functionText "fill -2 240 287 2 244 290 minecraft:air" "accessible dispatch doorway"

  foreach ($file in $functionFiles) {
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName) {
      $lineNumber++
      if ($line -match "^fill\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+") {
        $x1 = [int]$Matches[1]; $y1 = [int]$Matches[2]; $z1 = [int]$Matches[3]
        $x2 = [int]$Matches[4]; $y2 = [int]$Matches[5]; $z2 = [int]$Matches[6]
        $volume = ([Math]::Abs($x2 - $x1) + 1) * ([Math]::Abs($y2 - $y1) + 1) * ([Math]::Abs($z2 - $z1) + 1)
        if ($volume -gt 32768) {
          Fail "$($file.Name):$lineNumber exceeds vanilla /fill volume ($volume)"
        }
        if ([Math]::Min($y1, $y2) -lt 235 -or [Math]::Max($y1, $y2) -gt 253) {
          Fail "$($file.Name):$lineNumber leaves the designed vertical envelope y=235..253"
        }
        if (@($x1, $x2) | Where-Object { [Math]::Abs($_) -gt 20 }) {
          Fail "$($file.Name):$lineNumber leaves the designed horizontal envelope x=-20..20"
        }
        if ([Math]::Min($z1, $z2) -lt -28 -or [Math]::Max($z1, $z2) -gt 333) {
          Fail "$($file.Name):$lineNumber leaves the designed route envelope z=-28..333"
        }
      }
      if ($line -match "^setblock\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+") {
        $x = [int]$Matches[1]; $y = [int]$Matches[2]; $z = [int]$Matches[3]
        if ([Math]::Abs($x) -gt 20 -or $y -lt 235 -or $y -gt 253 -or $z -lt -28 -or $z -gt 333) {
          Fail "$($file.Name):$lineNumber places a block outside the production envelope"
        }
      }
    }
  }

  $unexpected = @(Get-ChildItem -LiteralPath $worldRoot -Recurse -File | Where-Object {
    $_.Name -match "^(README|manifest)" -or $_.Name -like "line_*.mcfunction" -or $_.Name -like "*.bak"
  })
  if ($unexpected.Count -gt 0) {
    Fail "zip contains stale/spoiler/backup entries: $($unexpected.FullName -join ', ')"
  }

  if (Test-Path -LiteralPath $stagingFile) {
    $staging = Get-Content -LiteralPath $stagingFile -Raw
    $sha1 = (Get-FileHash -LiteralPath $zipFile -Algorithm SHA1).Hash.ToLowerInvariant()
    RequireText $staging $sha1 "current Hold SHA1 receipt"
    RequireText $staging "host fragments I-IV + common-web ending + service digits 25569; no assembled raw server endpoint" "destination receipt"
  } else {
    Fail "missing design\MANUAL-MEDIA-STAGING.md"
  }

  if ($failures.Count -gt 0) {
    Write-Host "hold prologue check: FAILED"
    foreach ($failure in $failures) {
      Write-Host "  - $failure"
    }
    exit 1
  }

  $zip = Get-Item -LiteralPath $zipFile
  $sha1 = (Get-FileHash -LiteralPath $zipFile -Algorithm SHA1).Hash.ToLowerInvariant()
  Write-Host "hold prologue check: OK - 6 rooms, 5 single-layer controlled gates, structured books, filled evidence, cistern recovery, accessible route, bounded geometry"
  Write-Host "hold prologue check: $($functionFiles.Count) functions; $($zip.Length) bytes; SHA1 $sha1"
} finally {
  if (Test-Path -LiteralPath $workRoot) {
    Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
  }
}
