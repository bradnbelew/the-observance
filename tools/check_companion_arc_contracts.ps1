param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Read-Text([string]$RelativePath) {
  $path = Join-Path $RepoRoot $RelativePath
  if (!(Test-Path -LiteralPath $path)) {
    throw "Missing required file: $RelativePath"
  }
  return Get-Content -LiteralPath $path -Raw
}

function Assert-Contains([string]$Text, [string]$Needle, [string]$Label) {
  if ($Text.IndexOf($Needle, [StringComparison]::Ordinal) -lt 0) {
    throw "$Label is missing required contract marker: $Needle"
  }
}

function Assert-All([string]$Text, [string[]]$Needles, [string]$Label) {
  foreach ($needle in $Needles) {
    Assert-Contains $Text $needle $Label
  }
}

function Assert-NotContains([string]$Text, [string]$Needle, [string]$Label) {
  if ($Text.IndexOf($Needle, [StringComparison]::Ordinal) -ge 0) {
    throw "$Label contains retired companion phrasing: $Needle"
  }
}

$plugin = Read-Text "plugin\src\main\java\com\observance\watcher\ObservancePlugin.java"
$wren = Read-Text "plugin\src\main\java\com\observance\watcher\signal\listener\WrenNpcListener.java"
$watcher = Read-Text "plugin\src\main\java\com\observance\watcher\npc\CompanionArcWatcher.java"
$coop = Read-Text "plugin\src\main\java\com\observance\watcher\signal\listener\CoopPlateListener.java"
$seventh = Read-Text "plugin\src\main\java\com\observance\watcher\signal\listener\SeventhChoiceListener.java"
$companionResolver = Read-Text "discord\src\showrunner\companion.ts"
$companionRunner = Read-Text "discord\src\showrunner\companion.run.ts"
$coopGate = Read-Text "discord\src\showrunner\coop-gate.ts"
$autonomyRunner = Read-Text "discord\src\showrunner\autonomy.run.ts"
$botIndex = Read-Text "discord\src\bot\index.ts"
$config = Read-Text "plugin\src\main\resources\config.yml"
$wrenEvidence = Read-Text "design\WREN-EVIDENCE-LOOP.md"

Assert-All $wren @(
  "companion_introduced",
  "companion_trust",
  "companion_revealed",
  "reckoning_condemn",
  "reckoning_understand",
  "reckoning_free",
  "PDC_RECKONING",
  "mergeArcFlags",
  "insertEventLog",
  "npc.open",
  '"reckoning." + choice',
  'case "condemn"',
  'case "understand"',
  'case "free"',
  "if i say i know a place, ask me how i know it",
  "i marked the turns with charcoal once",
  "no speech, no debt"
) "WrenNpcListener"

Assert-NotContains $wren "tell me where you're headed and i'll tell you what i know" "WrenNpcListener"
Assert-NotContains $wren "that's the trade. it's a good trade" "WrenNpcListener"

Assert-All $watcher @(
  "iss_caught",
  "companion_artifact_found",
  "companion_revealed",
  "mergeArcFlags",
  "insertEventLog",
  "pollOnce"
) "CompanionArcWatcher"

Assert-All $coop @(
  "coop_world_ready_at",
  "Action.PHYSICAL",
  "Action.LEFT_CLICK_BLOCK",
  "bothFresh",
  "mergeArcFlags"
) "CoopPlateListener"

Assert-All $seventh @(
  "PDC_SEVENTH_CHOICE",
  "seventh_choice",
  "seventh_named",
  "CHOICE_RESTORE",
  "CHOICE_ERASE",
  "deep_gate_open",
  "oracle.resolveWorld",
  "mergeArcFlags",
  "insertEventLog"
) "SeventhChoiceListener"

Assert-All $plugin @(
  "new com.observance.watcher.signal.listener.WrenNpcListener",
  "new com.observance.watcher.signal.listener.SeventhChoiceListener",
  "new com.observance.watcher.signal.listener.CoopPlateListener",
  "new com.observance.watcher.npc.CompanionArcWatcher",
  "companion.reveal",
  "companionWatcher.pollOnce()"
) "ObservancePlugin registration"

Assert-All $companionResolver @(
  "WrenNpcListener.advanceTrust",
  "CompanionArcWatcher",
  "reckoning_condemn|understand|free",
  "wren.reveal.yes",
  "wren.reveal.tally",
  "wren.reckoning.condemn",
  "wren.reckoning.understand",
  "wren.reckoning.free"
) "Discord companion resolver"

Assert-All $companionRunner @(
  ".from('event_log')",
  ".eq('type', 'companion')",
  "companion_revealed",
  "reckoning_condemn",
  "reckoning_understand",
  "reckoning_free",
  "companion_lines_delivered",
  "companion_reckoning_delivered",
  "enqueueBeat('private_message'"
) "Discord companion runner"

Assert-All $coopGate @(
  "coop_world_ready_at",
  "M4_TOKEN",
  "maybeCloseCoopGate",
  "resolveAnswer",
  "Date.now() - readyAt"
) "Discord coop gate"

Assert-All $autonomyRunner @(
  "runCompanionPass",
  "companion_revealed",
  "comp.dirty"
) "Discord autonomy runner"

Assert-All $botIndex @(
  "import { maybeCloseCoopGate }",
  "const coopReply = await maybeCloseCoopGate(player, raw)",
  "message.reply({ content: coopReply })"
) "Discord bot message scan"

Assert-All $config @(
  "m4-three-hands:",
  "enabled:",
  "window-seconds:",
  "seventh-choice:",
  "restore-token:",
  "erase-token:",
  "puzzle-key:"
) "Plugin puzzle config"

Assert-All $wrenEvidence @(
  "## Wren Standard",
  "Independent proof before the reveal",
  "A player memory or action Wren can react to",
  "A reason Wren benefits from staying close",
  "A contradiction the group can name",
  "A reckoning choice after the reveal",
  "A finale callback that reflects the choice without erasing player agency",
  "## Beat 1 - Trust As Access",
  "## Beat 2 - Memory As Bait",
  "## Beat 3 - Independent Proof First",
  "## Beat 4 - The Reveal Is A Comparison",
  "## Beat 5 - Reckoning Choice",
  "## Beat 6 - Finale Callback",
  "Wren never becomes an out-of-fiction hint button",
  'Wren''s reveal must be gated behind `iss_caught`',
  "Wren's reckoning must be one-of-three and set-once",
  "the reveal lands before they understand Iss"
) "WREN-EVIDENCE-LOOP.md"

Write-Host "companion arc contract check: OK - Wren trust/reveal/reckoning, evidence-loop guard, coop world legs, Seventh choice, and Discord consumers are wired"
