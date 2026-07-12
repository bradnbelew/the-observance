param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Launch
)

$ErrorActionPreference = "Stop"

$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$structuresFile = Join-Path $RepoRoot "design\structures.md"
$clueLedgerFile = Join-Path $RepoRoot "design\CLUE-LEDGER.md"
$evidenceFile = Join-Path $RepoRoot "design\LIVE-REHEARSAL-EVIDENCE.md"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$pluginFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\ObservancePlugin.java"
$holdProtectionFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\HoldProtectionListener.java"
$structureTemplateFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java"
$deepHoldLayoutCheck = Join-Path $RepoRoot "tools\check_deep_hold_layout.py"

foreach ($file in @($sitesFile, $runbookFile, $structuresFile, $clueLedgerFile, $evidenceFile, $commandFile, $pluginFile, $holdProtectionFile, $structureTemplateFile, $deepHoldLayoutCheck)) {
  if (-not (Test-Path $file)) {
    throw "world build readiness: missing required file: $file"
  }
}

$siteText = Get-Content -LiteralPath $sitesFile -Raw
$runbook = Get-Content -LiteralPath $runbookFile -Raw
$structures = Get-Content -LiteralPath $structuresFile -Raw
$clueLedger = Get-Content -LiteralPath $clueLedgerFile -Raw
$evidence = Get-Content -LiteralPath $evidenceFile -Raw
$commandSource = Get-Content -LiteralPath $commandFile -Raw
$pluginSource = Get-Content -LiteralPath $pluginFile -Raw
$holdProtectionSource = Get-Content -LiteralPath $holdProtectionFile -Raw
$structureTemplateSource = Get-Content -LiteralPath $structureTemplateFile -Raw

# Structure quality is now a launch gate, not a vibe check. The build can pass
# coverage while still failing the ARG if it is only a sign/answer slab. Keep
# this check tied to design\CLUE-LEDGER.md and tools\check_structure_quality.ps1.
$structureQualityContract = @(
  "structure quality",
  "non-sign",
  "clue surface",
  "CLUE-LEDGER"
)

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

RequireText "ObservanceCommand.java production Hold fixture path" $commandSource "buildHoldIntegratedFixture"
RequireText "ObservanceCommand.java production Hold fixture path" $commandSource "Production Hold fixtures are dressed into the district shell"
RequireText "ObservanceCommand.java production Hold native chambers" $commandSource "isHoldNativeChamber(row)"
RequireText "ObservanceCommand.java production Hold native chambers" $commandSource "buildHoldThresholdVaultCore"
RequireText "ObservanceCommand.java production Hold native chambers" $commandSource "buildHoldUnwritingCore"
RequireText "ObservanceCommand.java production Hold native chambers" $commandSource "hasHoldFinaleMarkersNear"
RequireText "ObservanceCommand.java production Hold gate containment" $commandSource "holdGateReturnWidth"
RequireText "ObservanceCommand.java Hold repair path" $commandSource "placeHoldFixture(site, loc, holdRow)"
RequireText "ObservanceCommand.java Hold protection audit" $commandSource "hold_region"
RequireText "ObservanceCommand.java placeworld stamp gate" $commandSource "requiresPlaceWorldStamp"
RequireText "ObservanceCommand.java placeworld stamp gate" $commandSource "placeWorldStampPresent"
RequireText "ObservanceCommand.java placeworld stamp gate" $commandSource "surveyed; needs /obs placeworld stamp"
RequireText "ObservanceCommand.java dimension lane placement flags" $commandSource "nether_forge_placed"
RequireText "ObservanceCommand.java dimension lane placement flags" $commandSource "end_seventh_shrine_placed"
RequireText "ObservancePlugin.java Hold protection registration" $pluginSource "HoldProtectionListener"
RequireText "HoldProtectionListener.java normal-player protection" $holdProtectionSource "The Hold does not give."
RequireText "HoldProtectionListener.java third-lamp exception" $holdProtectionSource "isAllowedThirdLampPlacement"
if ($commandSource.IndexOf("touch while", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "ObservanceCommand.java native Hold chamber text still contains tutorial-like wording: touch while"
}

$deepHoldLayoutOutput = & python $deepHoldLayoutCheck 2>&1
if ($LASTEXITCODE -ne 0) {
  foreach ($line in $deepHoldLayoutOutput) {
    Fail "deep hold layout: $line"
  }
} else {
  foreach ($line in $deepHoldLayoutOutput) {
    Write-Host $line
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
  "vaun_hoard_chest",
  "vaun_bookshelf",
  "mara_lectern_1",
  "mara_lectern_2",
  "mara_lectern_3",
  "mara_lectern_4",
  "mara_lectern_5",
  "mara_map_marker",
  "sella_pool",
  "sella_anchor",
  "orin_marker_1",
  "orin_marker_2",
  "orin_marker_3",
  "orin_marker_4",
  "orin_marker_5",
  "orin_marker_6",
  "orin_frame_dial_1",
  "orin_frame_dial_2",
  "orin_frame_dial_3",
  "orin_frame_dial_4",
  "orin_frame_dial_5",
  "orin_frame_dial_6",
  "brann_toll_tower",
  "brann_corridor_start",
  "brann_corridor_end",
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

$holdSites = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($m in [regex]::Matches($commandSource, 'new\s+HoldSite\(\s*"([^"]+)"')) {
  [void]$holdSites.Add($m.Groups[1].Value)
}

$placementBriefMatch = [regex]::Match(
  $commandSource,
  'private\s+PlacementBrief\s+placementBrief\s*\([^)]*\)\s*\{(?<body>.*?)\r?\n\s*private\s+PlacementBrief\s+keeperPlacementBrief',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
if (-not $placementBriefMatch.Success) {
  Fail "ObservanceCommand.java placementBrief() method could not be audited"
}
$placementBriefBody = if ($placementBriefMatch.Success) { $placementBriefMatch.Groups["body"].Value } else { "" }

$placementLaneMatches = [regex]::Matches(
  $commandSource,
  'new\s+PlacementLane\(\s*"(?<id>[^"]+)"\s*,\s*"(?<label>[^"]+)"\s*,\s*new\s+String\[\]\s*\{(?<body>.*?)\}\s*\)',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
if ($placementLaneMatches.Count -eq 0) {
  Fail "ObservanceCommand.java must define PLACEMENT_LANES for lane-based setup"
}
$laneSites = [System.Collections.Generic.List[string]]::new()
$laneIds = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($m in $placementLaneMatches) {
  [void]$laneIds.Add($m.Groups["id"].Value)
  foreach ($siteId in ([regex]::Matches($m.Groups["body"].Value, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })) {
    $laneSites.Add($siteId) | Out-Null
  }
}
foreach ($requiredLane in @("prologue", "keepers", "customs", "human", "deep", "dread", "dimensions")) {
  if (-not $laneIds.Contains($requiredLane)) {
    Fail "PLACEMENT_LANES missing required setup lane '$requiredLane'"
  }
}
foreach ($id in $majorLaunchSites) {
  if (-not ($laneSites -contains $id)) {
    Fail "launch-required site '$id' is not assigned to a PLACEMENT_LANES setup lane"
  }
}
foreach ($id in $laneSites) {
  if (-not ($majorLaunchSites -contains $id)) {
    Fail "PLACEMENT_LANES contains non-launch site '$id'"
  }
  if (@($laneSites | Where-Object { $_ -eq $id }).Count -gt 1) {
    Fail "PLACEMENT_LANES assigns site '$id' more than once"
  }
}

$keeperSpineSlice = SourceSlice $commandSource "private static final String[][] KEEPER_SPINE" "private static final String[] PLACEWORLD_SURVEY_FIXTURES"
$keeperSpineSiteIds = @([regex]::Matches($keeperSpineSlice, '\{\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
$surveyFixtureSlice = SourceSlice $commandSource "private static final String[] PLACEWORLD_SURVEY_FIXTURES" "private static final String[] LAUNCH_REQUIRED_SITES"
$surveyFixtureSiteIds = @([regex]::Matches($surveyFixtureSlice, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
if ($keeperSpineSiteIds.Count -eq 0) {
  Fail "ObservanceCommand.java KEEPER_SPINE could not be audited for placeworld stamp coverage"
}
if ($surveyFixtureSiteIds.Count -eq 0) {
  Fail "ObservanceCommand.java PLACEWORLD_SURVEY_FIXTURES could not be audited for placeworld stamp coverage"
}
foreach ($id in $majorLaunchSites) {
  if (-not (($keeperSpineSiteIds -contains $id) -or ($surveyFixtureSiteIds -contains $id))) {
    Fail "launch-required site '$id' has no /obs placeworld stamp path (missing from KEEPER_SPINE and PLACEWORLD_SURVEY_FIXTURES)"
  }
}

$expectedTypes = @{
  first_report_lectern_01 = "report_lectern"
  bow_marker_01 = "bow_marker"
  offering_cairn_01 = "offering_cairn"
  kept_light_home_01 = "kept_light"
  vaun_hoard_chest = "vaun_hoard_chest"
  vaun_bookshelf = "vaun_bookshelf"
  the_far_water = "far_water"
  mara_map_marker = "mara_map_marker"
  sella_pool = "sella_pool"
  sella_anchor = "sella_anchor"
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
foreach ($lectern in @("mara_lectern_1", "mara_lectern_2", "mara_lectern_3", "mara_lectern_4", "mara_lectern_5")) {
  $expectedTypes[$lectern] = "mara_lectern"
}
foreach ($marker in @("orin_marker_1", "orin_marker_2", "orin_marker_3", "orin_marker_4", "orin_marker_5", "orin_marker_6")) {
  $expectedTypes[$marker] = "orin_marker"
}
foreach ($dial in @("orin_frame_dial_1", "orin_frame_dial_2", "orin_frame_dial_3", "orin_frame_dial_4", "orin_frame_dial_5", "orin_frame_dial_6")) {
  $expectedTypes[$dial] = "orin_frame_dial"
}
foreach ($brann in @("brann_toll_tower", "brann_corridor_start", "brann_corridor_end")) {
  $expectedTypes[$brann] = $brann
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
  -not $holdSites.Contains($_) -and
  $sites.Contains($_) -and (
    [string]$sites[$_].x -eq "null" -or [string]$sites[$_].y -eq "null" -or [string]$sites[$_].z -eq "null" -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].x) -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].y) -or
    [string]::IsNullOrWhiteSpace([string]$sites[$_].z)
  )
})

if ($Launch) {
  foreach ($id in $unplacedLaunch) {
    Fail "outside-Hold launch-required site '$id' still has placeholder coordinates"
  }
}

RequireText "RUNBOOK.md" $runbook "tools\check_world_build_readiness.ps1 -Launch"
RequireText "RUNBOOK.md" $runbook "outside-Hold launch-required site coordinates"
RequireText "RUNBOOK.md" $runbook "GeneratedProof"
RequireText "RUNBOOK.md" $runbook "/observance site todo"
RequireText "RUNBOOK.md" $runbook "/observance site next"
RequireText "RUNBOOK.md" $runbook "/observance site plan"
RequireText "RUNBOOK.md" $runbook "tools\new_launch_placement_packet.ps1"
RequireText "RUNBOOK.md" $runbook "coords-capture.csv"
RequireText "structures.md" $structures "tools\check_world_build_readiness.ps1 -Launch"
RequireText "structures.md" $structures "playable investigation spaces"
RequireText "structures.md" $structures "two non-sign clue surfaces"
RequireText "structures.md" $structures "traversal vector"
RequireText "CLUE-LEDGER.md" $clueLedger "hold-address-reconstruction"
RequireText "CLUE-LEDGER.md" $clueLedger "accepting-convergence"
RequireText "LIVE-REHEARSAL-EVIDENCE.md" $evidence "outside-Hold launch-required site coordinates"
RequireText "LIVE-REHEARSAL-EVIDENCE.md" $evidence "generated Deep Hold rooms"

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
RequireText "ObservanceCommand.java" $commandSource "spacing = Math.max(34, Math.min(48"
RequireText "ObservanceCommand.java" $commandSource "prepareCompactCell(siteLoc, Math.max(12, radius + 4), 9)"
RequireText "ObservanceCommand.java" $commandSource "compactGridCell(origin, 0, 6, spacing, 2)"
RequireText "ObservanceCommand.java" $commandSource "compactSurfaceCell(world, bx + (step * 4), bz)"
RequireText "ObservanceCommand.java" $commandSource 'seedFixtureLore(loc, "far_water")'
RequireText "ObservanceCommand.java" $commandSource "int[][] targets = fixtureLoreTargets(key);"
RequireText "ObservanceCommand.java" $commandSource "private int[][] fixtureLoreTargets(String id)"
RequireText "ObservanceCommand.java" $commandSource "private void placeDecorativeBookshelf(Block block, int seed)"
RequireText "ObservanceCommand.java" $commandSource "private void placeMechanicBookshelf(Block block)"
RequireText "ObservanceCommand.java" $commandSource "Vaun tally shelf is pre-filled"
RequireText "ObservanceCommand.java" $commandSource "placeMechanicBookshelf(loc.getBlock())"
RequireText "ObservanceCommand.java" $commandSource "placeFrameDial(new Location(world, bx, by, bz))"
RequireText "ObservanceCommand.java" $commandSource "expected an item-frame dial entity with an arrow"
RequireText "ObservanceCommand.java" $commandSource "placeDecorativeBookshelf(world.getBlockAt(bx + 4, by, bz + 5), 31)"
RequireText "StructureTemplates.java" $structureTemplateSource "prepareTemplateVolume(pen, base, id)"
RequireText "StructureTemplates.java" $structureTemplateSource "void clearBox(int cx, int y, int cz, int radius, int height)"
RequireText "StructureTemplates.java" $structureTemplateSource "seedLoreStorage(base, id)"
RequireText "StructureTemplates.java" $structureTemplateSource "if (b.getType() == Material.CHISELED_BOOKSHELF) continue"
RequireText "StructureTemplates.java" $structureTemplateSource "void chiseledShelf(int x, int y, int z)"
RequireText "StructureTemplates.java" $structureTemplateSource "shelf.setSlotOccupied(slot, occupied)"
RequireText "StructureTemplates.java" $structureTemplateSource 'pen.putBook(cx, cy + 1, cz, "the rosetta"'
RequireText "StructureTemplates.java" $structureTemplateSource 'pen.putBook(cx + 2, cy, cz - 1, "the missing volume"'
RequireText "StructureTemplates.java" $structureTemplateSource "pen.topSlab(cx, cy + 1, cz, Material.POLISHED_DEEPSLATE_SLAB)"
RequireText "StructureTemplates.java" $structureTemplateSource "Wall banners need a solid backing behind the banner block"
RequireText "RUNBOOK.md" $runbook "No structure uses a beacon beam as a player waypoint"

if ($commandSource.IndexOf("keptLightBeacon", [System.StringComparison]::Ordinal) -ge 0) {
  Fail "ObservanceCommand.java must not place retired beacon waypoints"
}

if ($commandSource.IndexOf("setType(Material.ITEM_FRAME", [System.StringComparison]::Ordinal) -ge 0) {
  Fail "ObservanceCommand.java must spawn item-frame entities; Material.ITEM_FRAME is not valid block placement"
}
if ($structureTemplateSource.IndexOf("Material.BEACON", [System.StringComparison]::Ordinal) -ge 0) {
  Fail "StructureTemplates.java must not place retired beacon waypoints"
}

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
  Write-Host "  outside-Hold launch-required placeholder coords remaining: $($unplacedLaunch.Count) (run with -Launch to fail until placed)"
} else {
  Write-Host "  outside-Hold launch-required placeholder coords remaining: 0"
}
