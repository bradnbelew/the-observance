param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Launch
)

$ErrorActionPreference = "Stop"

$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$structuresFile = Join-Path $RepoRoot "design\structures.md"
$evidenceFile = Join-Path $RepoRoot "design\LIVE-REHEARSAL-EVIDENCE.md"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$structureTemplateFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java"

foreach ($file in @($sitesFile, $runbookFile, $structuresFile, $evidenceFile, $commandFile, $structureTemplateFile)) {
  if (-not (Test-Path $file)) {
    throw "world build readiness: missing required file: $file"
  }
}

$siteText = Get-Content -LiteralPath $sitesFile -Raw
$runbook = Get-Content -LiteralPath $runbookFile -Raw
$structures = Get-Content -LiteralPath $structuresFile -Raw
$evidence = Get-Content -LiteralPath $evidenceFile -Raw
$commandSource = Get-Content -LiteralPath $commandFile -Raw
$structureTemplateSource = Get-Content -LiteralPath $structureTemplateFile -Raw

$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Clean-Value([string]$Value) {
  $v = ($Value -replace '\s+#.*$', '').Trim()
  if ($v.StartsWith('"') -and $v.EndsWith('"')) {
    return $v.Substring(1, $v.Length - 2)
  }
  return $v
}

function RequireText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing expected launch-readiness text: $Needle"
  }
}

function SourceSlice([string]$Text, [string]$StartNeedle, [string]$NextNeedle) {
  $start = $Text.IndexOf($StartNeedle, [System.StringComparison]::Ordinal)
  if ($start -lt 0) { return "" }
  $next = $Text.IndexOf($NextNeedle, $start + $StartNeedle.Length, [System.StringComparison]::Ordinal)
  if ($next -lt 0) { return $Text.Substring($start) }
  return $Text.Substring($start, $next - $start)
}

$sites = [ordered]@{}
$current = $null
foreach ($line in ($siteText -split "`r?`n")) {
  $siteMatch = [regex]::Match($line, '^\s{2}([A-Za-z0-9_-]+):\s*(?:#.*)?$')
  if ($siteMatch.Success) {
    $id = $siteMatch.Groups[1].Value
    if ($id -notin @("radius", "protect", "vertical-radius")) {
      $current = [ordered]@{
        id = $id
        type = ""
        world = ""
        x = "null"
        y = "null"
        z = "null"
        enabled = "true"
        radius = ""
      }
      $sites[$id] = $current
    }
    continue
  }

  if ($null -ne $current) {
    $propMatch = [regex]::Match($line, '^\s{4}([A-Za-z0-9_-]+):\s*(.*?)\s*$')
    if ($propMatch.Success) {
      $key = $propMatch.Groups[1].Value
      $current[$key] = Clean-Value $propMatch.Groups[2].Value
    }
  }
}

$majorLaunchSites = @(
  "first_report_lectern_01",
  "rune_rosetta",
  "stone_vaun",
  "stone_mara",
  "stone_sella",
  "stone_orin",
  "stone_brann",
  "stone_iss",
  "stone_of_reckoning",
  "bow_marker_01",
  "offering_cairn_01",
  "kept_light_home_01",
  "the_far_water",
  "school_stand",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "the_cold_hearth",
  "unbroken_light",
  "the_threshold",
  "the_unwriting",
  "keeper_altar",
  "coop_plate",
  "threshold_vault",
  "lampworks_stair",
  "third_lamp_stand",
  "painted_line",
  "dead_stall",
  "deep_bird_coops",
  "deep_market",
  "ration_table",
  "third_bay_breach",
  "warm_town_collapse",
  "dread_route_start",
  "dread_route_elsewhere",
  "dread_route_figure",
  "dread_route_exit",
  "nether_forge",
  "end_seventh_shrine"
)

$placementBriefMatch = [regex]::Match(
  $commandSource,
  'private\s+PlacementBrief\s+placementBrief\s*\([^)]*\)\s*\{(?<body>.*?)\r?\n\s*private\s+PlacementBrief\s+keeperPlacementBrief',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
if (-not $placementBriefMatch.Success) {
  Fail "ObservanceCommand.java placementBrief() method could not be audited"
}
$placementBriefBody = if ($placementBriefMatch.Success) { $placementBriefMatch.Groups["body"].Value } else { "" }

$expectedTypes = @{
  first_report_lectern_01 = "report_lectern"
  bow_marker_01 = "bow_marker"
  offering_cairn_01 = "offering_cairn"
  kept_light_home_01 = "kept_light"
  the_far_water = "far_water"
  school_stand = "school_stand"
  markers_row = "markers_row"
  cistern_7 = "cistern_7"
  watch_floor = "watch_floor"
  set_apart_shelf = "set_apart_shelf"
  undercroft_seal = "undercroft_seal"
  forgotten_mouth = "forgotten_mouth"
  unbroken_light = "accepting_floor"
  the_threshold = "the_threshold"
  the_unwriting = "seventh_shrine"
  keeper_altar = "keeper_altar"
  coop_plate = "coop_plate"
  lampworks_stair = "lampworks_stair"
  third_lamp_stand = "lamp_stand"
  painted_line = "painted_line"
  dead_stall = "dead_stall"
  deep_bird_coops = "bird_coops"
  deep_market = "deep_market"
  ration_table = "ration_table"
  third_bay_breach = "third_bay_breach"
  warm_town_collapse = "warm_town_collapse"
  nether_forge = "answer_sign"
  end_seventh_shrine = "answer_sign"
}

foreach ($keeper in @("stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss")) {
  $expectedTypes[$keeper] = "keeper_stone"
}
foreach ($dread in @("dread_route_start", "dread_route_elsewhere", "dread_route_figure", "dread_route_exit")) {
  $expectedTypes[$dread] = "dread_route"
}

foreach ($id in $majorLaunchSites) {
  RequireText "ObservanceCommand.java" $commandSource $id
  if ($placementBriefBody -and -not [regex]::IsMatch($placementBriefBody, 'case\s+"' + [regex]::Escape($id) + '"\s*->')) {
    Fail "launch-required site '$id' needs an explicit /observance site plan placement brief"
  }
  if (-not $sites.Contains($id)) {
    Fail "launch-required site '$id' is missing from sites.yml"
    continue
  }

  $site = $sites[$id]
  if ([string]$site.enabled -ne "true") {
    Fail "launch-required site '$id' must be enabled"
  }
  if ($expectedTypes.ContainsKey($id) -and [string]$site.type -ne [string]$expectedTypes[$id]) {
    Fail "launch-required site '$id' must have type '$($expectedTypes[$id])', found '$($site.type)'"
  }
  if ([string]::IsNullOrWhiteSpace([string]$site.world)) {
    Fail "launch-required site '$id' has no world"
  }
  $radius = 0.0
  if ([string]::IsNullOrWhiteSpace([string]$site.radius) -or -not [double]::TryParse([string]$site.radius, [ref]$radius) -or $radius -le 0) {
    Fail "launch-required site '$id' has invalid radius '$($site.radius)'"
  }
}

$enabledSites = @($sites.Values | Where-Object { [string]$_.enabled -eq "true" })
$unplacedEnabled = @($enabledSites | Where-Object {
  [string]$_.x -eq "null" -or [string]$_.y -eq "null" -or [string]$_.z -eq "null" -or
  [string]::IsNullOrWhiteSpace([string]$_.x) -or [string]::IsNullOrWhiteSpace([string]$_.y) -or [string]::IsNullOrWhiteSpace([string]$_.z)
})
$unplacedLaunch = @($majorLaunchSites | Where-Object {
  $sites.Contains($_) -and (
    [string]$sites[$_].x -eq "null" -or [string]$sites[$_].y -eq "null" -or [string]$sites[$_].z -eq "null" -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].x) -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].y) -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].z)
  )
})

if ($Launch) {
  foreach ($id in $unplacedLaunch) {
    Fail "launch-required site '$id' still has placeholder coordinates"
  }
}

RequireText "RUNBOOK.md" $runbook "tools\check_world_build_readiness.ps1 -Launch"
RequireText "RUNBOOK.md" $runbook "launch-required site coordinates"
RequireText "RUNBOOK.md" $runbook "/observance site todo"
RequireText "RUNBOOK.md" $runbook "/observance site next"
RequireText "RUNBOOK.md" $runbook "/observance site plan"
RequireText "RUNBOOK.md" $runbook "tools\new_launch_placement_packet.ps1"
RequireText "RUNBOOK.md" $runbook "coords-capture.csv"
RequireText "structures.md" $structures "tools\check_world_build_readiness.ps1 -Launch"
RequireText "LIVE-REHEARSAL-EVIDENCE.md" $evidence "launch-required site coordinates"

$visualTemplateMarkers = [regex]::Matches($structureTemplateSource, "Post-Unlit visual overhaul").Count
if ($visualTemplateMarkers -lt 15) {
  Fail "StructureTemplates.java expected at least 15 Post-Unlit visual overhaul template chambers; found $visualTemplateMarkers"
}

$templateHandlers = @(
  "handlePlaceWorld",
  "handlePlaceRoom",
  "handlePlaceRegion",
  "handlePlaceDeep"
)
foreach ($handler in $templateHandlers) {
  $slice = SourceSlice $commandSource ("private void $handler") "`r`n    private "
  if ([string]::IsNullOrWhiteSpace($slice)) {
    Fail "ObservanceCommand.java missing $handler() placement handler"
  } elseif ($slice.IndexOf("StructureTemplates.keeper(", [System.StringComparison]::Ordinal) -lt 0) {
    Fail "ObservanceCommand.java $handler() must use StructureTemplates.keeper(...) so rich keeper/rosetta templates are placed"
  } elseif ($slice.IndexOf("StructureTemplates.keeperStone(", [System.StringComparison]::Ordinal) -ge 0) {
    Fail "ObservanceCommand.java $handler() must not call generic keeperStone(...) directly"
  }
}

foreach ($keeperCase in @(
  'case "rosetta", "rune_rosetta", "rune" -> rosetta(pen, base);',
  'case "vaun"   -> vaun(pen, base);',
  'case "mara"   -> mara(pen, base);',
  'case "sella"  -> sella(pen, base);',
  'case "orin"   -> orin(pen, base);',
  'case "brann"  -> brann(pen, base);',
  'case "iss"    -> iss(pen, base);'
)) {
  RequireText "StructureTemplates.java" $structureTemplateSource $keeperCase
}

$requiredProofBuilders = @(
  "buildSchoolStand",
  "buildMarkersRow",
  "buildCisternSeven",
  "buildWatchFloor",
  "buildSetApartShelf",
  "buildUndercroftSeal",
  "buildForgottenMouth",
  "buildDeepMarket",
  "buildRationTable",
  "buildThirdBayBreach",
  "buildWarmTownCollapse"
)
foreach ($builder in $requiredProofBuilders) {
  $slice = SourceSlice $commandSource ("private void $builder") "`r`n    private "
  if ([string]::IsNullOrWhiteSpace($slice)) {
    Fail "ObservanceCommand.java missing side-proof builder '$builder'"
  } elseif ($slice.IndexOf("buildProofChamber", [System.StringComparison]::Ordinal) -lt 0) {
    Fail "side-proof builder '$builder' must call buildProofChamber so visual rooms do not regress to tiny platforms"
  }
}

RequireText "ObservanceCommand.java" $commandSource "private void buildProofChamber"
RequireText "ObservanceCommand.java" $commandSource 'String spacing = args.length >= 2 ? args[1] : "36"'
RequireText "ObservanceCommand.java" $commandSource "int platformRadius = 18"
RequireText "ObservanceCommand.java" $commandSource "spacing = Math.max(32, Math.min(64"

if ($failures.Count -gt 0) {
  Write-Host "world build readiness check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

$mode = if ($Launch) { "launch" } else { "audit" }
Write-Host "world build readiness check: OK ($mode mode) - $($majorLaunchSites.Count) launch-required sites defined with expected types"
Write-Host "  enabled sites: $($enabledSites.Count); enabled sites with placeholder coords: $($unplacedEnabled.Count)"
if ($unplacedLaunch.Count -gt 0) {
  Write-Host "  launch-required placeholder coords remaining: $($unplacedLaunch.Count) (run with -Launch to fail until placed)"
} else {
  Write-Host "  launch-required placeholder coords remaining: 0"
}
