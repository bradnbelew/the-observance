param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$CaptureCsv = "",
  [switch]$Launch
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Clean([object]$Value) {
  if ($null -eq $Value) { return "" }
  return ([string]$Value).Trim()
}

function IsBlank([object]$Value) {
  return [string]::IsNullOrWhiteSpace((Clean $Value))
}

function IsNumber([string]$Value) {
  return (Clean $Value) -match '^-?\d+(?:\.\d+)?$'
}

function RequireMember($Rows, [string]$Column) {
  if (-not ($Rows | Get-Member -Name $Column -MemberType NoteProperty)) {
    Fail "coords-capture.csv missing column: $Column"
  }
}

function Distance([object]$A, [object]$B) {
  $dx = [double](Clean $A.X) - [double](Clean $B.X)
  $dy = [double](Clean $A.Y) - [double](Clean $B.Y)
  $dz = [double](Clean $A.Z) - [double](Clean $B.Z)
  return [Math]::Sqrt(($dx * $dx) + ($dy * $dy) + ($dz * $dz))
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$generatorFile = Join-Path $repoFull "tools\new_launch_placement_packet.ps1"
$commandFile = Join-Path $repoFull "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
foreach ($file in @($generatorFile, $commandFile)) {
  if (-not (Test-Path $file)) {
    throw "launch coord quality: missing required file: $file"
  }
}

if ([string]::IsNullOrWhiteSpace($CaptureCsv)) {
  $outRoot = Join-Path $repoFull "build\coord-quality"
  & powershell -NoProfile -ExecutionPolicy Bypass -File $generatorFile -RepoRoot $repoFull -OutRoot $outRoot -Date "audit" -Force | Out-Null
  if ($LASTEXITCODE -ne 0) {
    throw "launch coord quality: could not generate audit placement packet"
  }
  $CaptureCsv = Join-Path $outRoot "audit\coords-capture.csv"
}

$captureFull = Resolve-UnderRepo $repoFull $CaptureCsv
if (-not $captureFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to read capture CSV outside repo: $captureFull"
}
if (-not (Test-Path $captureFull)) {
  throw "launch coord quality: capture CSV not found: $captureFull"
}

$commandSource = Get-Content -LiteralPath $commandFile -Raw
$launchMatch = [regex]::Match(
  $commandSource,
  'private\s+static\s+final\s+String\[\]\s+LAUNCH_REQUIRED_SITES\s*=\s*\{(?<body>.*?)\};',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
if (-not $launchMatch.Success) {
  throw "launch coord quality: could not find LAUNCH_REQUIRED_SITES"
}
$launchSites = [string[]]([regex]::Matches($launchMatch.Groups["body"].Value, '"([^"]+)"') | ForEach-Object {
  $_.Groups[1].Value
})

$laneMatches = [regex]::Matches(
  $commandSource,
  'new\s+PlacementLane\(\s*"(?<id>[^"]+)"\s*,\s*"(?<label>[^"]+)"\s*,\s*new\s+String\[\]\s*\{(?<body>.*?)\}\s*\)',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
$siteLane = @{}
foreach ($m in $laneMatches) {
  $laneId = $m.Groups["id"].Value
  foreach ($siteId in [regex]::Matches($m.Groups["body"].Value, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }) {
    $siteLane[$siteId] = $laneId
  }
}

$rows = @(Import-Csv -LiteralPath $captureFull)
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($column in @(
  "Order",
  "Lane",
  "SiteId",
  "ChosenWorld",
  "X",
  "Y",
  "Z",
  "SurveyedBy",
  "VisualStatus",
  "ApproachShot",
  "FocalShot",
  "ActionShot",
  "ExitShot",
  "CohesionNotes",
  "Notes"
)) {
  RequireMember $rows $column
}

if ($failures.Count -eq 0) {
  if ($rows.Count -ne $launchSites.Count) {
    Fail "coords-capture.csv has $($rows.Count) row(s), expected $($launchSites.Count)"
  }

  $seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
  foreach ($row in $rows) {
    $id = Clean $row.SiteId
    if ([string]::IsNullOrWhiteSpace($id)) {
      Fail "coords-capture.csv has a row with blank SiteId"
      continue
    }
    if (-not $seen.Add($id)) {
      Fail "coords-capture.csv repeats site '$id'"
    }
    if ($launchSites -notcontains $id) {
      Fail "coords-capture.csv includes non-launch site '$id'"
    }
    if ($siteLane.ContainsKey($id) -and (Clean $row.Lane) -ne $siteLane[$id]) {
      Fail "site '$id' has lane '$($row.Lane)', expected '$($siteLane[$id])'"
    }
  }
  foreach ($id in $launchSites) {
    if (-not ($rows | Where-Object { $_.SiteId -eq $id })) {
      Fail "coords-capture.csv missing launch site '$id'"
    }
  }

  $filled = @($rows | Where-Object {
    -not (IsBlank $_.ChosenWorld) -or -not (IsBlank $_.X) -or -not (IsBlank $_.Y) -or -not (IsBlank $_.Z)
  })

  foreach ($row in $filled) {
    $id = Clean $row.SiteId
    if (IsBlank $row.ChosenWorld) { Fail "site '$id' missing ChosenWorld" }
    foreach ($axis in @("X", "Y", "Z")) {
      if (-not (IsNumber (Clean $row.$axis))) {
        Fail "site '$id' has invalid $axis coordinate: '$($row.$axis)'"
      }
    }
    $visual = Clean $row.VisualStatus
    if ($visual -notin @("KEEP", "RESHAPE", "REPLACE", "CUT")) {
      Fail "site '$id' has invalid VisualStatus '$visual'"
    }
    if ((Clean $row.ChosenWorld) -eq "world_nether" -and $id -ne "nether_forge") {
      Fail "site '$id' is in the Nether but only nether_forge is launch-required there"
    }
    if ((Clean $row.ChosenWorld) -eq "world_the_end" -and $id -ne "end_seventh_shrine") {
      Fail "site '$id' is in the End but only end_seventh_shrine is launch-required there"
    }
    if ($id -eq "nether_forge" -and (Clean $row.ChosenWorld) -notlike "*nether*") {
      Fail "nether_forge must be surveyed in a Nether world"
    }
    if ($id -eq "end_seventh_shrine" -and (Clean $row.ChosenWorld) -notlike "*end*") {
      Fail "end_seventh_shrine must be surveyed in an End world"
    }
  }

  $byCoord = @{}
  foreach ($row in $filled) {
    if (-not (IsNumber (Clean $row.X)) -or -not (IsNumber (Clean $row.Y)) -or -not (IsNumber (Clean $row.Z)) -or (IsBlank $row.ChosenWorld)) {
      continue
    }
    $key = "{0}:{1},{2},{3}" -f (Clean $row.ChosenWorld).ToLowerInvariant(), (Clean $row.X), (Clean $row.Y), (Clean $row.Z)
    if ($byCoord.ContainsKey($key)) {
      Fail "sites '$($byCoord[$key])' and '$($row.SiteId)' share identical coordinates ($key)"
    } else {
      $byCoord[$key] = $row.SiteId
    }
  }

  $minimumPairs = @(
    @("stone_vaun", "stone_mara", 24),
    @("stone_vaun", "stone_sella", 24),
    @("stone_vaun", "stone_orin", 24),
    @("stone_vaun", "stone_brann", 24),
    @("stone_vaun", "stone_iss", 24),
    @("stone_mara", "stone_sella", 24),
    @("stone_mara", "stone_orin", 24),
    @("stone_mara", "stone_brann", 24),
    @("stone_mara", "stone_iss", 24),
    @("stone_sella", "stone_orin", 24),
    @("stone_sella", "stone_brann", 24),
    @("stone_sella", "stone_iss", 24),
    @("stone_orin", "stone_brann", 24),
    @("stone_orin", "stone_iss", 24),
    @("stone_brann", "stone_iss", 24),
    @("rune_rosetta", "stone_of_reckoning", 12),
    @("unbroken_light", "keeper_altar", 8),
    @("coop_plate", "threshold_vault", 12)
  )
  foreach ($rule in $minimumPairs) {
    $a = $filled | Where-Object { $_.SiteId -eq $rule[0] } | Select-Object -First 1
    $b = $filled | Where-Object { $_.SiteId -eq $rule[1] } | Select-Object -First 1
    if ($null -eq $a -or $null -eq $b) { continue }
    if ((Clean $a.ChosenWorld) -ne (Clean $b.ChosenWorld)) { continue }
    if (-not (IsNumber (Clean $a.X)) -or -not (IsNumber (Clean $b.X))) { continue }
    if ((Distance $a $b) -lt [double]$rule[2]) {
      Fail "sites '$($rule[0])' and '$($rule[1])' are too close; minimum spacing is $($rule[2]) blocks"
    }
  }

  $routeGroups = @(
    @("Lamp-works proof route", @("lampworks_stair", "third_lamp_stand", "painted_line", "dead_stall", "deep_bird_coops"), 220),
    @("Deep market route", @("deep_market", "ration_table", "third_bay_breach", "warm_town_collapse"), 260),
    @("Undercroft rite cluster", @("undercroft_seal", "forgotten_mouth", "unbroken_light", "keeper_altar", "coop_plate", "threshold_vault"), 320)
  )
  foreach ($group in $routeGroups) {
    $name = $group[0]
    $ids = $group[1]
    $maxSpan = [double]$group[2]
    $placed = @($ids | ForEach-Object {
      $id = $_
      $filled | Where-Object { $_.SiteId -eq $id } | Select-Object -First 1
    } | Where-Object {
      $null -ne $_ -and
      -not (IsBlank $_.ChosenWorld) -and
      (IsNumber (Clean $_.X)) -and
      (IsNumber (Clean $_.Y)) -and
      (IsNumber (Clean $_.Z))
    })
    if ($placed.Count -lt $ids.Count) { continue }
    $worlds = @($placed | ForEach-Object { Clean $_.ChosenWorld } | Select-Object -Unique)
    if ($worlds.Count -ne 1) {
      Fail "$name should live in one world, found: $($worlds -join ', ')"
      continue
    }
    $maxSeen = 0.0
    for ($i = 0; $i -lt $placed.Count; $i++) {
      for ($j = $i + 1; $j -lt $placed.Count; $j++) {
        $maxSeen = [Math]::Max($maxSeen, (Distance $placed[$i] $placed[$j]))
      }
    }
    if ($maxSeen -gt $maxSpan) {
      Fail "$name is too spread out; farthest anchors are $([Math]::Round($maxSeen, 1)) blocks apart (max $maxSpan)"
    }
  }

  if ($Launch) {
    foreach ($row in $rows) {
      $id = Clean $row.SiteId
      if (IsBlank $row.ChosenWorld -or IsBlank $row.X -or IsBlank $row.Y -or IsBlank $row.Z) {
        Fail "site '$id' has no launch coordinates"
      }
      if ((Clean $row.VisualStatus) -ne "KEEP") {
        Fail "site '$id' must be KEEP before launch; found '$($row.VisualStatus)'"
      }
      foreach ($shot in @("ApproachShot", "FocalShot", "ActionShot", "ExitShot")) {
        if (IsBlank $row.$shot) {
          Fail "site '$id' missing $shot proof"
        }
      }
      if (IsBlank $row.CohesionNotes) {
        Fail "site '$id' missing CohesionNotes"
      }
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "launch coord quality check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

$mode = if ($Launch) { "launch" } else { "audit" }
$filledCount = @($rows | Where-Object {
  -not (IsBlank $_.ChosenWorld) -or -not (IsBlank $_.X) -or -not (IsBlank $_.Y) -or -not (IsBlank $_.Z)
}).Count
Write-Host "launch coord quality check: OK ($mode mode) - $($rows.Count) launch rows, $filledCount coordinate row(s) filled"
if (-not $Launch) {
  Write-Host "  launch proof gate is armed: KEEP verdicts, four proof shots, dimension sanity, duplicate detection, and spacing checks"
}
