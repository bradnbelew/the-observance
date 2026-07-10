param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$pluginConfigFile = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$structureFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java"
$answerSignFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\AnswerSignListener.java"
$acceptingRiteFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\AcceptingRiteListener.java"
$coopPlateFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\CoopPlateListener.java"
$deathListenerFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\DeathListener.java"
$roomSwapReentryFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\RoomSwapReentryListener.java"
$coopGateFile = Join-Path $RepoRoot "discord\src\showrunner\coop-gate.ts"
$hintsSeedFile = Join-Path $RepoRoot "discord\supabase\seeds\hints_seed.sql"

if (-not (Test-Path $commandFile)) {
  throw "rehearsal consistency: missing command source: $commandFile"
}
if (-not (Test-Path $pluginConfigFile)) {
  throw "rehearsal consistency: missing plugin config: $pluginConfigFile"
}
if (-not (Test-Path $sitesFile)) {
  throw "rehearsal consistency: missing sites.yml: $sitesFile"
}
if (-not (Test-Path $structureFile)) {
  throw "rehearsal consistency: missing structure templates: $structureFile"
}
if (-not (Test-Path $answerSignFile)) {
  throw "rehearsal consistency: missing answer sign listener: $answerSignFile"
}
if (-not (Test-Path $acceptingRiteFile)) {
  throw "rehearsal consistency: missing Accepting rite listener: $acceptingRiteFile"
}
if (-not (Test-Path $coopPlateFile)) {
  throw "rehearsal consistency: missing coop plate listener: $coopPlateFile"
}
if (-not (Test-Path $deathListenerFile)) {
  throw "rehearsal consistency: missing death listener: $deathListenerFile"
}
if (-not (Test-Path $roomSwapReentryFile)) {
  throw "rehearsal consistency: missing room-swap re-entry listener: $roomSwapReentryFile"
}
if (-not (Test-Path $coopGateFile)) {
  throw "rehearsal consistency: missing coop gate closer: $coopGateFile"
}
if (-not (Test-Path $hintsSeedFile)) {
  throw "rehearsal consistency: missing hints seed: $hintsSeedFile"
}

$source = Get-Content -LiteralPath $commandFile -Raw
$pluginConfigText = Get-Content -LiteralPath $pluginConfigFile -Raw
$sitesText = Get-Content -LiteralPath $sitesFile -Raw
$structureSource = Get-Content -LiteralPath $structureFile -Raw
$answerSignSource = Get-Content -LiteralPath $answerSignFile -Raw
$acceptingRiteSource = Get-Content -LiteralPath $acceptingRiteFile -Raw
$coopPlateSource = Get-Content -LiteralPath $coopPlateFile -Raw
$deathListenerSource = Get-Content -LiteralPath $deathListenerFile -Raw
$roomSwapReentrySource = Get-Content -LiteralPath $roomSwapReentryFile -Raw
$coopGateSource = Get-Content -LiteralPath $coopGateFile -Raw
$hintsSeedText = Get-Content -LiteralPath $hintsSeedFile -Raw

function Fail([System.Collections.Generic.List[string]] $failures, [string] $message) {
  $failures.Add($message)
}

function UniqueStrings([string[]] $items) {
  $set = [System.Collections.Generic.SortedSet[string]]::new([System.StringComparer]::Ordinal)
  foreach ($item in $items) {
    if (-not [string]::IsNullOrWhiteSpace($item)) {
      [void]$set.Add($item)
    }
  }
  return [string[]]$set
}

function ExtractStringArray([string] $name) {
  $pattern = "private\s+static\s+final\s+String\[\]\s+$([regex]::Escape($name))\s*=\s*\{(?<body>.*?)\};"
  $m = [regex]::Match($source, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "rehearsal consistency: could not find String[] $name"
  }
  return [string[]]([regex]::Matches($m.Groups["body"].Value, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
}

function ExtractFirstColumnFrom2dArray([string] $name) {
  $pattern = "private\s+static\s+final\s+String\[\]\[\]\s+$([regex]::Escape($name))\s*=\s*\{(?<body>.*?)\};"
  $m = [regex]::Match($source, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "rehearsal consistency: could not find String[][] $name"
  }
  return [string[]]([regex]::Matches($m.Groups["body"].Value, '\{\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
}

function ExtractCoverageSiteIds() {
  $m = [regex]::Match($source, 'private\s+static\s+final\s+CoverageLane\[\]\s+COVERAGE_LANES\s*=\s*\{(?<body>.*?)\};', [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "rehearsal consistency: could not find COVERAGE_LANES"
  }
  $body = $m.Groups["body"].Value
  $ids = [System.Collections.Generic.List[string]]::new()
  foreach ($array in [regex]::Matches($body, 'new\s+String\[\]\s*\{(?<ids>.*?)\}', [System.Text.RegularExpressions.RegexOptions]::Singleline)) {
    foreach ($id in [regex]::Matches($array.Groups["ids"].Value, '"([^"]+)"')) {
      $ids.Add($id.Groups[1].Value)
    }
  }
  if ($body -notmatch 'puzzlePassSiteIds\(\)') {
    throw "rehearsal consistency: COVERAGE_LANES no longer includes puzzlePassSiteIds(); update this check"
  }
  return [string[]]$ids
}

function ExtractMethodRegion([string] $fromPattern, [string] $toPattern) {
  $pattern = "$fromPattern(?<body>.*?)$toPattern"
  $m = [regex]::Match($source, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "rehearsal consistency: could not find method region: $fromPattern"
  }
  return $m.Groups["body"].Value
}

$siteIds = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($m in [regex]::Matches($sitesText, '^\s{2}([A-Za-z0-9_-]+):\s*(?:#.*)?$', [System.Text.RegularExpressions.RegexOptions]::Multiline)) {
  $id = $m.Groups[1].Value
  if ($id -notin @("radius", "protect", "vertical-radius")) {
    [void]$siteIds.Add($id)
  }
}

$visitRoute = ExtractStringArray "VISIT_ROUTE"
$puzzlePass = ExtractFirstColumnFrom2dArray "PUZZLE_PASS_SITES"
$dreadPass = ExtractFirstColumnFrom2dArray "DREAD_PASS_SITES"
$coverageLiteral = ExtractCoverageSiteIds
$routeProof = [string[]]([regex]::Matches($source, 'registerRouteProofSite\("([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
$prologueRuntime = [string[]]([regex]::Matches($source, 'registerRuntimeSite\(new Site\("([^"]+)"') | ForEach-Object { $_.Groups[1].Value })

$coverageAll = UniqueStrings (@($coverageLiteral) + @($puzzlePass) + @($dreadPass))
$visitAll = UniqueStrings $visitRoute
$siteCritical = UniqueStrings (@($coverageAll) + @($visitAll) + @($routeProof) + @($prologueRuntime))
$buildLabFixtureBody = ExtractMethodRegion 'private\s+void\s+buildLabFixture\s*\([^)]*\)\s*\{' '\r?\n\s*private\s+boolean\s+isTemplateLabSite'
$visualAuditBody = ExtractMethodRegion 'private\s+static\s+boolean\s+isVisualAuditSite\s*\([^)]*\)\s*\{' '\r?\n\s*private\s+static\s+boolean\s+isMajorVisualSite'
$visualIssueBody = ExtractMethodRegion 'private\s+String\s+visualIssue\s*\([^)]*\)\s*\{' '\r?\n\s*private\s+VisualScan\s+scanVisualSite'
$handledFixtureTypes = UniqueStrings ([string[]]([regex]::Matches($buildLabFixtureBody, '"([^"]+)"\.equals\(type\)') | ForEach-Object { $_.Groups[1].Value }))
$visualAuditTypes = UniqueStrings ([string[]]([regex]::Matches($visualAuditBody, '"([^"]+)"\.equals\(type\)') | ForEach-Object { $_.Groups[1].Value }))
$templateVisualTypes = @("keeper_stone", "structure", "marker", "accepting_floor", "seventh_shrine", "the_threshold")

$failures = [System.Collections.Generic.List[string]]::new()

foreach ($id in $siteCritical) {
  if (-not $siteIds.Contains($id)) {
    Fail $failures "site id '$id' is referenced by rehearsal/coverage/runtime staging but missing from sites.yml"
  }
}

foreach ($id in $coverageAll) {
  if ($visitAll -notcontains $id) {
    Fail $failures "coverage site '$id' is not reachable through /obs visit"
  }
}

foreach ($id in $routeProof) {
  if ($coverageAll -notcontains $id) {
    Fail $failures "dialogue route-proof site '$id' is staged but not covered by /obs coverage"
  }
}

foreach ($id in $dreadPass) {
  if ($visitAll -notcontains $id) {
    Fail $failures "dreadpass site '$id' is staged but missing from /obs visit route"
  }
}

foreach ($id in @("painted_line", "dead_stall", "third_lamp_stand", "lampworks_stair", "deep_bird_coops")) {
  if ($coverageAll -notcontains $id) {
    Fail $failures "dialogue proof anchor '$id' must be covered by /obs coverage"
  }
  if ($visitAll -notcontains $id) {
    Fail $failures "dialogue proof anchor '$id' must be reachable through /obs visit"
  }
}

foreach ($type in $visualAuditTypes) {
  if (($handledFixtureTypes -notcontains $type) -and ($templateVisualTypes -notcontains $type)) {
    Fail $failures "visual-audit type '$type' can fall through buildLabFixture's generic marker path; add a real fixture or mark it as template-driven"
  }
}

# Major canon places must have an explicit template path. StructureTemplates keeps a generic fallback for
# legacy/experimental calls, but spine, deep, and dimension anchors are not allowed to become generic stones.
foreach ($templateKey in @(
  "rune_rosetta",
  "vaun",
  "mara",
  "sella",
  "orin",
  "brann",
  "iss",
  "stone_of_reckoning",
  "the_cold_hearth",
  "unbroken_light",
  "the_threshold",
  "the_unwriting",
  "threshold_vault",
  "nether_forge",
  "end_seventh_shrine"
)) {
  if (-not [regex]::IsMatch($structureSource, 'case\s+[^;{]*"' + [regex]::Escape($templateKey) + '"')) {
    Fail $failures "major visual template '$templateKey' is not explicitly routed in StructureTemplates; it would risk the generic keeperStone fallback"
  }
}

if ($visualAuditTypes -contains "dread_route" -and $handledFixtureTypes -notcontains "dread_route") {
  Fail $failures "dread_route is audited as a major visual site but /obs placelab cannot build a dread anchor fixture"
}

foreach ($visualGuard in @(
  "no Deep Hold palette anchor",
  "palette feels non-cohesive",
  "too many signs / reads like labels",
  "no focal object to inspect",
  "no route shape / approach vector",
  "no gatherable body space",
  "painted line is not visibly crossable",
  "dread beat has no sightline or exit space"
)) {
  if ($visualIssueBody.IndexOf($visualGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "visual audit must retain the launch-quality guard: $visualGuard"
  }
}

foreach ($answerGuard in @(
  "sendAnswerReceipt(p, raw)",
  "the sign clears.",
  "a place is not an answer.",
  "COORDINATE_SHAPED"
)) {
  if ($answerSignSource.IndexOf($answerGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "answer sign listener must retain the anti-black-hole receipt guard: $answerGuard"
  }
}

foreach ($acceptingGuard in @(
  "you are bowed. waiting on",
  "the light waits for your bow.",
  "the floor waits for",
  "not the hour.",
  "the floor answers.",
  "accepting.feedback:"
)) {
  if ($acceptingRiteSource.IndexOf($acceptingGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "Accepting rite must retain group-bow coordination feedback: $acceptingGuard"
  }
}

foreach ($coopGuard in @(
  "one hand stands.",
  "one hand marks.",
  "the square waits on the word.",
  "Math.max(60_000L, windowMs)",
  "coop:plate:feedback:"
)) {
  if ($coopPlateSource.IndexOf($coopGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "coop plate listener must retain forgiving held-rite feedback: $coopGuard"
  }
}

foreach ($beastForkGuard in @(
  "sendForkFeedback(killer)",
  "the warning is silenced.",
  "Sound.BLOCK_NOTE_BLOCK_BASS",
  "NamedTextColor.DARK_GRAY"
)) {
  if ($deathListenerSource.IndexOf($beastForkGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "Sacred Beast fork must retain private in-world acknowledgement: $beastForkGuard"
  }
}

foreach ($roomSwapGuard in @(
  "sendReentryFeedback(p)",
  "the room returns wrong.",
  "Sound.BLOCK_NOTE_BLOCK_BASS",
  "NamedTextColor.DARK_GRAY"
)) {
  if ($roomSwapReentrySource.IndexOf($roomSwapGuard, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "room-swap re-entry must retain private in-world acknowledgement: $roomSwapGuard"
  }
}
if ($pluginConfigText.IndexOf("window-seconds: 180", [System.StringComparison]::Ordinal) -lt 0) {
  Fail $failures "m4-three-hands config must keep the forgiving 180-second window"
}
if ($coopGateSource.IndexOf("COOP_WINDOW_MS = 180_000", [System.StringComparison]::Ordinal) -lt 0) {
  Fail $failures "Discord coop closer must stay in sync with the 180-second plugin window"
}
foreach ($staleCoopHint in @("same short breath", "all three within the same window")) {
  if ($hintsSeedText.IndexOf($staleCoopHint, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    Fail $failures "m4-three-hands hints must not describe the gate as a twitch window: $staleCoopHint"
  }
}

if ($pluginConfigText.IndexOf('forbidden-words: ["unkept"]', [System.StringComparison]::Ordinal) -lt 0) {
  Fail $failures "The Unspoken custom must ship with the live forbidden word 'unkept', not an empty forbidden-words list"
}
if ($pluginConfigText.IndexOf("forbidden-words: []", [System.StringComparison]::Ordinal) -ge 0) {
  Fail $failures "The Unspoken custom is inert because tracker.forbidden-words is empty"
}

foreach ($requiredText in @(
  "handlePrepWorld(sender, new String[]{`"prepworld`", spacing})",
  "handlePuzzlePass(sender, new String[]{`"puzzlepass`", spacing})",
  "handleDreadPass(sender, new String[]{`"dreadpass`", `"stage`"})",
  "handleCoverage(sender)",
  "handleRehearse(sender, new String[]{`"rehearse`", `"start`"})"
)) {
  if (-not $source.Contains($requiredText)) {
    Fail $failures "director startup no longer contains expected call: $requiredText"
  }
}

foreach ($requiredText in @(
  "prepareTemplateVolume(pen, base, id)",
  "prepareTemplateVolume(pen, base, `"keeper_stone`")",
  "void clearBox(int cx, int y, int cz, int radius, int height)",
  "set(cx + dx, y + dy, cz + dz, Material.AIR)",
  "seedLoreStorage(base, id)",
  "b.getType() == Material.CHISELED_BOOKSHELF",
  "void chiseledShelf(int x, int y, int z)",
  "shelf.setSlotOccupied(slot, occupied)",
  "pen.putBook(cx, cy + 1, cz, `"the rosetta`"",
  "pen.putBook(cx + 2, cy, cz - 1, `"the missing volume`"",
  "pen.topSlab(cx, cy + 1, cz, Material.POLISHED_DEEPSLATE_SLAB)",
  "slab.setType(org.bukkit.block.data.type.Slab.Type.TOP)",
  "Wall banners need a solid backing behind the banner block"
)) {
  if ($structureSource.IndexOf($requiredText, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "StructureTemplates must clear intentional air volumes before stamping into terrain: $requiredText"
  }
}

foreach ($requiredText in @(
  "int spacing = 36",
  "Math.max(34, Math.min(48",
  "placeCompactSpine(origin.clone().add(spacing, 0, 0), surface, spacing)",
  "placeCompactSpine(origin.clone().add(spacing, 0, spacing), deep, spacing)",
  "compactSurfaceCell(world, bx + (step * 4), bz)",
  "compactGridCell(origin, 0, 6, spacing, 2)",
  "seedFixtureLore(loc, `"far_water`")",
  "if (block.getType() == Material.CHISELED_BOOKSHELF) continue",
  "private void placeDecorativeBookshelf(Block block, int seed)",
  "placeDecorativeBookshelf(world.getBlockAt(bx + 4, by, bz + 5), 31)",
  "prepareCompactCell(siteLoc, Math.max(12, radius + 4), 9)",
  "radius, 6, true, true, null, false",
  "Compact layout: facing east, rows are Lamp-works proof"
)) {
  if ($source.IndexOf($requiredText, [System.StringComparison]::Ordinal) -lt 0) {
    Fail $failures "prepworld must stay a compact platform-board layout, not wide scatter: $requiredText"
  }
}

foreach ($forbiddenText in @("keptLightBeacon", "beaconTint", "beam projecting")) {
  if ($source.IndexOf($forbiddenText, [System.StringComparison]::Ordinal) -ge 0) {
    Fail $failures "beacon waypoint code must stay retired from ObservanceCommand.java: $forbiddenText"
  }
}

foreach ($forbiddenText in @("Material.BEACON", "keptLightBeacon")) {
  if ($structureSource.IndexOf($forbiddenText, [System.StringComparison]::Ordinal) -ge 0) {
    Fail $failures "beacon waypoint structures must stay retired from StructureTemplates.java: $forbiddenText"
  }
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

Write-Host "rehearsal consistency check: OK - $($coverageAll.Count) coverage sites, $($visitAll.Count) visit targets, $($puzzlePass.Count) puzzle fixtures, $($dreadPass.Count) dread anchors, and $($visualAuditTypes.Count) visual fixture types are wired"
