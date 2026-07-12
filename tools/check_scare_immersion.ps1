param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$privateMessageFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\beats\lib\PrivateMessageBeat.java"
$composureFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\beats\lib\ComposureBeat.java"
$hintFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\beats\lib\HintWhisperBeat.java"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$unlitFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\UnlitVillageListener.java"
$ambientFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\beats\AmbientBeatGenerator.java"
$directorDoc = Join-Path $RepoRoot "design\DIRECTOR-SIMPLIFICATION.md"
$gatherDoc = Join-Path $RepoRoot "design\content\gather-events.md"
$evidenceDoc = Join-Path $RepoRoot "design\LIVE-REHEARSAL-EVIDENCE.md"
$packetGeneratorFile = Join-Path $RepoRoot "tools\new_rehearsal_packet.ps1"
$packetValidatorFile = Join-Path $RepoRoot "tools\check_rehearsal_packet.ps1"
$activeStoryFiles = @(
  "discord\supabase\apply-all.sql",
  "discord\supabase\apply-tonight.sql",
  "discord\supabase\seeds\puzzles_seed.sql",
  "discord\supabase\seeds\metapuzzle_seed.sql",
  "discord\supabase\seeds\seventh_seed.sql",
  "discord\supabase\seeds\side_quests.sql",
  "discord\src\showrunner\autonomy.run.ts",
  "discord\src\showrunner\companion.run.ts",
  "discord\src\oracle\resolve.ts"
)

foreach ($file in @($privateMessageFile, $composureFile, $hintFile, $commandFile, $unlitFile, $ambientFile, $directorDoc, $gatherDoc, $evidenceDoc, $packetGeneratorFile, $packetValidatorFile)) {
  if (-not (Test-Path $file)) {
    throw "scare immersion check: missing required file: $file"
  }
}

foreach ($rel in $activeStoryFiles) {
  $file = Join-Path $RepoRoot $rel
  if (-not (Test-Path $file)) {
    throw "scare immersion check: missing active story file: $file"
  }
}

$privateMessage = Get-Content -LiteralPath $privateMessageFile -Raw
$composure = Get-Content -LiteralPath $composureFile -Raw
$hint = Get-Content -LiteralPath $hintFile -Raw
$command = Get-Content -LiteralPath $commandFile -Raw
$unlit = Get-Content -LiteralPath $unlitFile -Raw
$ambient = Get-Content -LiteralPath $ambientFile -Raw
$director = Get-Content -LiteralPath $directorDoc -Raw
$gather = Get-Content -LiteralPath $gatherDoc -Raw
$evidence = Get-Content -LiteralPath $evidenceDoc -Raw
$packetGenerator = Get-Content -LiteralPath $packetGeneratorFile -Raw
$packetValidator = Get-Content -LiteralPath $packetValidatorFile -Raw

$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string] $message) {
  $script:failures.Add($message)
}

function RequireContains([string] $label, [string] $text, [string] $needle) {
  if (-not $text.Contains($needle)) {
    Fail "$label missing expected text: $needle"
  }
}

function RequireNotRegex([string] $label, [string] $text, [string] $pattern) {
  if ([regex]::IsMatch($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
    Fail "$label contains forbidden pattern: $pattern"
  }
}

# Ambient/private text may use title only as an explicit boundary break. Otherwise it must demote to
# actionbar or in-world display, preserving the "private pressure, not title-card scare" rule.
RequireContains "PrivateMessageBeat title demotion" $privateMessage '!p.bool("boundary_break", false)'
RequireContains "PrivateMessageBeat demotion result" $privateMessage 'title-demoted-actionbar'
RequireContains "ComposureBeat title demotion" $composure '!p.bool("boundary_break", false)'
RequireContains "HintWhisperBeat boundary flag" $hint 'boundaryBreak = p.bool("boundary_break", false)'
RequireContains "HintWhisperBeat title demotion to display" $hint 'mode.equals("title") && !boundaryBreak'

# Rehearsal scare/test presets should remain environmental or actionbar-based. If a command path needs a
# title, it must add boundary_break:true and be reviewed as a rare frame violation.
RequireNotRegex "ObservanceCommand scare payloads" $command '\\\"mode\\\"\\s*:\\s*\\\"title\\\"'

# Dread-route signage must stay diegetic; these old operator labels are allowed only inside
# hasOperatorLabel's blacklist, not as authored sign arrays or private-message payloads.
RequireNotRegex "ObservanceCommand authored dread labels" $command 'new\s+String\[\]\s*\{\s*"(DREAD ROUTE|FIGURE|EXIT)"'
RequireNotRegex "ObservanceCommand authored dread labels" $command 'new\s+String\[\]\s*\{[^}]*"(walk slowly|sound on|look back once|then move)"'
RequireNotRegex "ObservanceCommand private scare text" $command '\{\\?"mode\\?"\s*:\s*\\?"actionbar\\?"[^}]*?(DREAD ROUTE|walk slowly|sound on|look back once|then move)'

# Unlit apparitions are private, escalate through witnessed behavior, and leave state that other
# surfaces can acknowledge. Ambient scares should use each site's lore instead of generic mobs.
foreach ($needle in @('figureStage', 'setVisibleByDefault(false)', 'target.showEntity(plugin, part)', 'unlit_figure_seen', 'unlit_light_taken', 'unlit_figure_hunt')) {
  RequireContains "Unlit staged private figure" $unlit $needle
}
RequireNotRegex "Unlit lurid eye label" $unlit 'Component\.text\("O O"'
RequireNotRegex "ambient generic named mob" $ambient 'Material\.WITHER_SKELETON_SKULL'
foreach ($needle in @('observance:cold_toll', 'observance:stone_breath', 'observance:drone_low')) {
  RequireContains "site-conditioned ambient scares" $ambient $needle
}

# Active authored payloads may not sneak full-screen text into the run. The runtime demotes accidental
# titles, but seed/showrunner content should still carry the right intent: actionbar/display unless a rare
# boundary break is deliberately marked and reviewed.
$titlePayload = '(?is)(?:mode|''mode''|"mode")\s*[:,=]\s*[''"]title[''"]'
$boundaryBreak = '(?is)boundary_break\s*[:,=]\s*(?:true|1)'
foreach ($rel in $activeStoryFiles) {
  $path = Join-Path $RepoRoot $rel
  $text = Get-Content -LiteralPath $path -Raw
  if ([regex]::IsMatch($text, $titlePayload) -and -not [regex]::IsMatch($text, $boundaryBreak)) {
    Fail "$rel contains title-mode authored scare text without boundary_break"
  }
}

# The docs should keep the operator standard visible.
RequireContains "director scare language doc" $director "Live scare language should avoid full-screen command text"
RequireContains "director scare language doc" $director "boundary break"
RequireContains "gather-events beat contract doc" $gather "private_message(actionbar"
RequireContains "live rehearsal scare contract" $evidence "reaction gag, not an Observance scare"
RequireContains "rehearsal packet scare lore hook" $packetGenerator "lore hook:"
RequireContains "rehearsal packet scare verdict" $packetGenerator "verdict: KEEP / RESHAPE / REPLACE / CUT"
RequireContains "rehearsal packet scare validator" $packetValidator 'needs concrete $field proof tied to lore, source, restraint, and aftertaste'

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

Write-Host "scare immersion check: OK - ambient/private/hint text demotes accidental titles, scare presets avoid title mode, and dread labels stay diegetic"
