param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$specFile = Join-Path $RepoRoot "design\THE-UNLIT.md"
$startupFile = Join-Path $RepoRoot "design\UNLIT-PREARG-STARTUP.md"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$operatorControlsFile = Join-Path $RepoRoot "design\OPERATOR-LIVE-CONTROLS.md"
$listenerFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\UnlitVillageListener.java"
$pluginFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\ObservancePlugin.java"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$configFile = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$pluginYml = Join-Path $RepoRoot "plugin\src\main\resources\plugin.yml"
$dashboardAuthor = Join-Path $RepoRoot "dashboard\src\app\author\page.tsx"
$dashboardPanel = Join-Path $RepoRoot "dashboard\src\components\author\UnlitProgress.tsx"
$threadCardsFile = Join-Path $RepoRoot "discord\supabase\seeds\thread_cards.sql"
$metapuzzleFile = Join-Path $RepoRoot "discord\supabase\seeds\metapuzzle_seed.sql"
$voiceArchiveFile = Join-Path $RepoRoot "discord\src\voice.archive.ts"
$playtestGateFile = Join-Path $RepoRoot "tools\check_unlit_playtest_ready.ps1"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) { $script:failures.Add($Message) | Out-Null }
function Read-Required([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    Fail "missing file: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}
function RequireText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing: $Needle"
  }
}

$spec = Read-Required $specFile
$startup = Read-Required $startupFile
$runbook = Read-Required $runbookFile
$operatorControls = Read-Required $operatorControlsFile
$listener = Read-Required $listenerFile
$plugin = Read-Required $pluginFile
$command = Read-Required $commandFile
$config = Read-Required $configFile
$sites = Read-Required $sitesFile
$pluginYaml = Read-Required $pluginYml
$authorPage = Read-Required $dashboardAuthor
$unlitPanel = Read-Required $dashboardPanel
$threadCards = Read-Required $threadCardsFile
$metapuzzle = Read-Required $metapuzzleFile
$voiceArchive = Read-Required $voiceArchiveFile
$playtestGate = Read-Required $playtestGateFile

foreach ($needle in @(
  "The Unlit",
  "the village unkept",
  "Non-Linear House Graph",
  "Do not ship the final Unlit using a Wither Skeleton",
  "borrowed lanterns",
  "borrowed lantern",
  "Required Endgame Evidence Set",
  "fixture signature",
  "stray light OK",
  "Playtest Handoff Gate"
)) {
  RequireText "design/THE-UNLIT.md" $spec $needle
}

foreach ($needle in @(
  "PRE-ARG STARTUP GUIDE",
  "observance_unlit",
  "7 borrowed lanterns",
  "unlit_seen_lamp",
  "unlit_seen_cairn",
  "unlit_seen_coop",
  "unlit_seen_well",
  "unlit_seen_watch",
  "unlit_seen_warm",
  "unlit_seen_threshold",
  "unlit_seen_base",
  "/obs unlit border 138",
  "/obs unlit darken all 138",
  "/obs unlit audit",
  "/obs unlit ready",
  "check_unlit_playtest_ready.ps1"
)) {
  RequireText "design/UNLIT-PREARG-STARTUP.md" $startup $needle
}

foreach ($needle in @(
  "UNLIT-PREARG-STARTUP.md",
  "/obs unlit border 138",
  "/obs unlit darken all 138",
  "players receive 7 borrowed lanterns",
  "lamp, cairn, coop, well, watch, warm, threshold",
  "all eight"
)) {
  RequireText "design/RUNBOOK.md Unlit handoff" $runbook $needle
}

if ($runbook.IndexOf("required ending evidence houses are lamp, well, watch, and base", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "design/RUNBOOK.md must not describe only four Unlit houses as required ending evidence"
}

foreach ($needle in @(
  "The Unlit Village",
  "light-budget: 7",
  "/obs unlit border 138",
  "rite-tokens"
)) {
  RequireText "design/OPERATOR-LIVE-CONTROLS.md Unlit controls" $operatorControls $needle
}

foreach ($needle in @(
  "unlit playtest readiness: OK",
  "stop building and hand this to Nano for playtest",
  "check_rehearsal_packet.ps1",
  "/obs unlit buildmode off",
  "/obs unlit darken all",
  "stray light OK",
  "borrowed lantern",
  "lantern is broken"
)) {
  RequireText "check_unlit_playtest_ready.ps1" $playtestGate $needle
}

foreach ($needle in @(
  "class UnlitVillageListener",
  "BlockBreakEvent",
  "BlockPlaceEvent",
  "borrowedLightStack",
  "Borrowed lantern",
  "Material.SOUL_LANTERN",
  "isBannedItem",
  "InventoryOpenEvent",
  "isRouteCheeseBlock",
  "isUnlitBuildMode",
  "forceUnlitNight",
  "maintainUnlitWorldRules",
  "CreatureSpawnEvent",
  "disable-regular-mob-spawns",
  "darknessExposure",
  "darknessEffectAmplifier",
  "BlockDisplay",
  "TextDisplay",
  "spawnBlockPart",
  "spawnEyeDisplay",
  "figureOffset",
  "rushFigureToLight",
  "discoveryMessage",
  "unlit_seen_"
)) {
  RequireText "UnlitVillageListener.java" $listener $needle
}

if ($listener.IndexOf("WITHER_SKELETON", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "UnlitVillageListener.java must not use a Wither Skeleton as the final figure fallback"
}
if ($listener.IndexOf("ArmorStand", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "UnlitVillageListener.java must use display entities for the visible phase-two figure, not an armor-stand body"
}
if ($listener.IndexOf("new UnlitDeepListener", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "UnlitVillageListener.java should stay separate from the Unlit Deep custom listener"
}

foreach ($needle in @(
  "UnlitVillageListener",
  "unlitVillage.start()",
  "unlitVillage.stop()"
)) {
  RequireText "ObservancePlugin.java" $plugin $needle
}

foreach ($needle in @(
  'case "unlit"',
  "handleUnlitAudit",
  "handleUnlitBorder",
  "handleUnlitBuildMode",
  "handleUnlitClue",
  "handleUnlitDarken",
  "handleUnlitPass",
  "handleUnlitReady",
  "unlitStrayLightIssue",
  "isUnauthorizedUnlitLight",
  "stampUnlitClue",
  "buildUnlitPassLane",
  "sendUnlitRunbook",
  "Unlit expansion lane",
  "unlitFixtureIssue",
  "unlitBorderIssue",
  "fixture proof",
  "unlit playtest readiness: OK",
  "unlitSiteId",
  "House order is intentionally non-linear"
)) {
  RequireText "ObservanceCommand.java" $command $needle
}

foreach ($needle in @(
  "unlit:",
  'world: "observance_unlit"',
  "light-budget:",
  "darkness-grace-seconds:",
  "darkness-damage-seconds:",
  "border-radius:",
  "border-radius: 138",
  "buildmode:",
  "force-night:",
  "disable-regular-mob-spawns:",
  "light-budget: 7",
  "darkness-effect-amplifier:"
)) {
  RequireText "config.yml" $config $needle
}

foreach ($site in @(
  "unlit_entry",
  "unlit_spawn_mirror",
  "unlit_exit",
  "unlit_house_lamp",
  "unlit_house_cairn",
  "unlit_house_coop",
  "unlit_house_well",
  "unlit_house_watch",
  "unlit_house_warm",
  "unlit_house_threshold",
  "unlit_house_base",
  "unlit_safe_01"
)) {
  RequireText "sites.yml" $sites "$site`:"
}

RequireText "plugin.yml" $pluginYaml "/observance unlit <site|clue|pass|audit|darken|border|buildmode|ready>"

if ($listener.IndexOf('name.contains("DOOR")', [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "UnlitVillageListener.java should allow normal village doors; do not route-cheese-block every DOOR material"
}
if ($command.IndexOf("Unlit pass staged without blocks/signs", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
  Fail "stalker/extinguish Unlit passes should stay lightweight and avoid big block/sign lanes"
}

$hasBorrowedCoalDrift = $false
if ($spec.IndexOf("borrowed coal", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { $hasBorrowedCoalDrift = $true }
if ($listener.IndexOf("Borrowed coal", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { $hasBorrowedCoalDrift = $true }
if ($command.IndexOf("borrowed coal", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { $hasBorrowedCoalDrift = $true }
if ($command.IndexOf("place coal", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { $hasBorrowedCoalDrift = $true }
if ($hasBorrowedCoalDrift) {
  Fail "The Unlit traversal light should be a borrowed lantern, not borrowed coal."
}

foreach ($needle in @(
  "UnlitProgress",
  "unlit_seen_lamp",
  "unlit_seen_cairn",
  "unlit_seen_coop",
  "unlit_seen_well",
  "unlit_seen_watch",
  "unlit_seen_warm",
  "unlit_seen_threshold",
  "unlit_seen_base",
  "required houses",
  "house discoveries recorded"
)) {
  RequireText "dashboard Unlit progress" ($authorPage + $unlitPanel) $needle
}

foreach ($needle in @(
  "rite-tokens",
  "accepting_onramp_open",
  "unlit_seen_lamp",
  "unlit_seen_cairn",
  "unlit_seen_coop",
  "unlit_seen_well",
  "unlit_seen_watch",
  "unlit_seen_warm",
  "unlit_seen_threshold",
  "unlit_seen_base"
)) {
  RequireText "metapuzzle_seed Unlit endgame gate" $metapuzzle $needle
}

foreach ($needle in @(
  "flag:unlit_seen_lamp",
  "flag:unlit_seen_cairn",
  "flag:unlit_seen_coop",
  "flag:unlit_seen_well",
  "flag:unlit_seen_watch",
  "flag:unlit_seen_warm",
  "flag:unlit_seen_threshold",
  "flag:unlit_seen_base"
)) {
  RequireText "thread_cards Unlit flag gates" $threadCards $needle
}

foreach ($needle in @(
  "cardPlaceUnlitMirror",
  "cardHumanColdLampLedger",
  "cardPlaceUnlitCairnBowl",
  "cardHappenedBirdsSilent",
  "cardSurfaceWellBelowCopy",
  "cardSurfaceWatchWithoutSleep",
  "cardHappenedWarmRoadFalse",
  "cardHappenedThresholdUnderCopy"
)) {
  RequireText "voice.archive Unlit card bodies" $voiceArchive $needle
}

if ($threadCards.IndexOf("expedition 4", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
  Fail "thread_cards.sql must not encode fixed Unlit expedition numbers"
}

if ($failures.Count -gt 0) {
  Write-Host "unlit readiness: FAIL"
  foreach ($failure in $failures) {
    Write-Host " - $failure"
  }
  exit 1
}

Write-Host "unlit readiness: OK - spec, display-entity figure, commands, config, site placeholders, and archive hooks are wired"
