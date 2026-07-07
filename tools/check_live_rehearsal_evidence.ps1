param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$packetFile = Join-Path $RepoRoot "design\LIVE-REHEARSAL-EVIDENCE.md"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$playtestFile = Join-Path $RepoRoot "design\PERSONAL-PLAYTEST-SCRIPT.md"
$visualFile = Join-Path $RepoRoot "design\VISUAL-RESCUE.md"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$generatorFile = Join-Path $RepoRoot "tools\new_rehearsal_packet.ps1"
$validatorFile = Join-Path $RepoRoot "tools\check_rehearsal_packet.ps1"
$unlitPlaytestFile = Join-Path $RepoRoot "tools\check_unlit_playtest_ready.ps1"

foreach ($file in @($packetFile, $runbookFile, $playtestFile, $visualFile, $commandFile, $generatorFile, $validatorFile, $unlitPlaytestFile)) {
  if (-not (Test-Path $file)) {
    throw "live rehearsal evidence check: missing required file: $file"
  }
}

$packet = Get-Content -LiteralPath $packetFile -Raw
$runbook = Get-Content -LiteralPath $runbookFile -Raw
$playtest = Get-Content -LiteralPath $playtestFile -Raw
$visual = Get-Content -LiteralPath $visualFile -Raw
$command = Get-Content -LiteralPath $commandFile -Raw
$generator = Get-Content -LiteralPath $generatorFile -Raw
$validator = Get-Content -LiteralPath $validatorFile -Raw
$unlitPlaytest = Get-Content -LiteralPath $unlitPlaytestFile -Raw

$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string] $message) {
  $script:failures.Add($message)
}

function RequireContains([string] $label, [string] $text, [string] $needle) {
  if ($text.IndexOf($needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$label missing expected text: $needle"
  }
}

foreach ($doc in @(
  @("RUNBOOK.md", $runbook),
  @("PERSONAL-PLAYTEST-SCRIPT.md", $playtest),
  @("VISUAL-RESCUE.md", $visual)
)) {
  RequireContains $doc[0] $doc[1] "design/LIVE-REHEARSAL-EVIDENCE.md"
}

foreach ($doc in @(
  @("RUNBOOK.md", $runbook),
  @("LIVE-REHEARSAL-EVIDENCE.md", $packet)
)) {
  RequireContains $doc[0] $doc[1] "tools\new_rehearsal_packet.ps1"
}

RequireContains "LIVE-REHEARSAL-EVIDENCE.md" $packet "tools\check_rehearsal_packet.ps1"
RequireContains "LIVE-REHEARSAL-EVIDENCE.md" $packet "tools\check_unlit_playtest_ready.ps1"
RequireContains "Unlit playtest gate" $unlitPlaytest "unlit playtest readiness: OK"
RequireContains "Unlit playtest gate" $unlitPlaytest "stop building and hand this to Nano for playtest"
RequireContains "Unlit playtest gate" $unlitPlaytest "stray light OK"
RequireContains "Unlit playtest gate" $unlitPlaytest "borrowed lantern"

foreach ($lane in @(
  "First hour",
  "Visuals",
  "Dialogue proof",
  "Puzzle fairness",
  "Side paths",
  "Scares",
  "Unlit",
  "Record/web",
  "Finale"
)) {
  RequireContains "evidence lane" $packet "| $lane |"
}

foreach ($required in @(
  "10 minute uncut clip",
  "new_rehearsal_packet.ps1",
  "check_rehearsal_packet.ps1",
  "approach, focal object, answer surface, and exit",
  "NPC line",
  "Twelve side destinations",
  "one clip for each scare family",
  "Unlit expedition",
  "Unlit Expedition Proof",
  "/obs unlit buildmode off",
  "/obs unlit darken all",
  "/obs unlit audit",
  "/obs unlit ready",
  "fixture proof, stray light OK, and border OK",
  "borrowed lantern",
  "lantern is broken",
  "failed-cheese attempt",
  "non-linear",
  "full-screen instruction card",
  "password rather than a restoration",
  "First-Hour Pacing",
  "place becoming wrong before it feels like an answer-entry route",
  "spawn / join read",
  "handoff to live route",
  "Side Path Value Matrix",
  "Each side destination must change belief",
  'KEEP`, `RESHAPE`, `REPLACE`, or `CUT',
  "silhouette: what reads from approach distance",
  "body verb: what players physically do there",
  "Puzzle Fairness Matrix",
  "Every puzzle family must be retraceable",
  "Hard is fine; opaque is not",
  "If a tester solves by guessing",
  "lore hook",
  "reaction gag, not an Observance scare",
  "This packet is allowed to be ugly. The ARG is not."
)) {
  RequireContains "live rehearsal packet" $packet $required
}

$majorSites = @(
  "rune_rosetta",
  "stone_vaun",
  "stone_mara",
  "stone_sella",
  "stone_orin",
  "stone_brann",
  "stone_iss",
  "school_stand",
  "the_far_water",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "stone_of_reckoning",
  "the_cold_hearth",
  "unbroken_light",
  "the_threshold",
  "the_unwriting",
  "threshold_vault",
  "nether_forge",
  "end_seventh_shrine",
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
  "unlit_house_base"
)

foreach ($site in $majorSites) {
  RequireContains "major site evidence list" $packet ('`' + $site + '`')
  RequireContains "rehearsal packet generator" $generator ('"' + $site + '"')
  RequireContains "rehearsal packet validator" $validator ('"' + $site + '"')
  if (-not [regex]::IsMatch($command, '"' + [regex]::Escape($site) + '"')) {
    Fail "major site '$site' is required by live rehearsal evidence but is not named in ObservanceCommand visit/coverage tooling"
  }
}

foreach ($contract in @(
  "Aro/Lamp-works",
  "empty bird coops",
  "School stand",
  "Markers row",
  "Cistern 7",
  "Watch-floor",
  "Set-apart shelf",
  "Undercroft seal",
  "Forgotten Mouth",
  "Deep Market",
  "Far water",
  "Ration table",
  "Third bay",
  "Aro/warm-town lie",
  "Wenna/dead-stall",
  "Coll/third-lamp",
  "Dob/bowing stones",
  "Old Pell/dark hours",
  "Wren/reckoning"
)) {
  RequireContains "NPC/world contract list" $packet $contract
}

foreach ($blocker in @(
  "this is where the plugin wants us to go",
  "operator explanation",
  "side path produces no new belief",
  "full-screen text",
  "no retraceable clue",
  "Wren sounds like an exposition dispenser",
  "Record website feels like documentation"
)) {
  RequireContains "launch blocker list" $packet $blocker
}

$testRoot = Join-Path $RepoRoot "build\check-live-rehearsal"
& powershell -NoProfile -ExecutionPolicy Bypass -File $generatorFile -RepoRoot $RepoRoot -OutRoot $testRoot -Date "audit" -Force | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "new_rehearsal_packet.ps1 failed its audit dry-run"
}

$testPacket = Join-Path $testRoot "audit"
foreach ($rel in @(
  "00-notes.md",
  "fixes.md",
  "launch-attestations.md",
  "screenshots\README.md",
  "clips\README.md"
)) {
  $path = Join-Path $testPacket $rel
  if (-not (Test-Path $path)) {
    Fail "new_rehearsal_packet.ps1 did not create expected file: $rel"
  }
}

if (Test-Path (Join-Path $testPacket "00-notes.md")) {
  $notes = Get-Content -LiteralPath (Join-Path $testPacket "00-notes.md") -Raw
  foreach ($required in @("Evidence Lanes", "First-Hour Pacing", "Major Site Visual Shots", "Side Path Value Matrix", "NPC/World Contracts", "Puzzle Fairness Matrix", "Scare Review", "Unlit Expedition Proof", "Stop/Launch Decision")) {
    RequireContains "generated 00-notes.md" $notes $required
  }
}
if (Test-Path (Join-Path $testPacket "launch-attestations.md")) {
  $attestations = Get-Content -LiteralPath (Join-Path $testPacket "launch-attestations.md") -Raw
  foreach ($required in @("Supabase Live Status", "Server Load", "Real Client Rendering", "Live Command Audits", "External Media", "Session Zero And Capture Consent", "Credential Rotation", "Operator Verdict")) {
    RequireContains "generated launch-attestations.md" $attestations $required
  }
}

$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& powershell -NoProfile -ExecutionPolicy Bypass -File $validatorFile -RepoRoot $RepoRoot -PacketDir $testPacket *> $null
$emptyValidationExit = $LASTEXITCODE
$ErrorActionPreference = $oldErrorActionPreference
if ($emptyValidationExit -eq 0) {
  Fail "check_rehearsal_packet.ps1 unexpectedly passed an empty generated packet"
}

$completeRoot = Join-Path $RepoRoot "build\check-live-rehearsal-complete"
& powershell -NoProfile -ExecutionPolicy Bypass -File $generatorFile -RepoRoot $RepoRoot -OutRoot $completeRoot -Date "audit" -Force | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "new_rehearsal_packet.ps1 failed complete-packet setup"
}
$completePacket = Join-Path $completeRoot "audit"
$completeNotes = Join-Path $completePacket "00-notes.md"
$completeFixes = Join-Path $completePacket "fixes.md"
$completeAttestations = Join-Path $completePacket "launch-attestations.md"
if (Test-Path $completeNotes) {
  $text = Get-Content -LiteralPath $completeNotes -Raw
  $text = $text.Replace("- [ ]", "- [x]")
  $text = $text.Replace("status: KEEP / RESHAPE / REPLACE / CUT", "status: KEEP")
  $text = $text.Replace("verdict: KEEP / RESHAPE / REPLACE / CUT", "verdict: KEEP")
  $text = $text.Replace("score: 1 / 2 / 3 / 4 / 5", "score: 5")
  $text = $text.Replace("timestamp:", "timestamp: 00:01 audit-confirmed")
  $text = $text.Replace("player action:", "player action: audit-confirmed player acted without prompting")
  $text = $text.Replace("world evidence:", "world evidence: audit-confirmed map/NPC/sound response")
  $text = $text.Replace("friction:", "friction: audit-confirmed no dead air")
  $text = $text.Replace("operator leak:", "operator leak: none")
  $text = $text.Replace("silhouette:", "silhouette: audit-placeholder readable from approach")
  $text = $text.Replace("palette:", "palette: audit-placeholder Deep Hold materials")
  $text = $text.Replace("lighting:", "lighting: audit-placeholder lore-directed light")
  $text = $text.Replace("body verb:", "body verb: audit-placeholder player must cross/stoop/circle/gather")
  $text = $text.Replace("action/answer legibility:", "action/answer legibility: audit-placeholder surface is readable")
  $text = $text.Replace("evidence:", "evidence: audit-placeholder")
  $text = $text.Replace("value: belief / dread / confirmation / motif", "value: audit-confirmed useful contradiction")
  $text = $text.Replace("first guess:", "first guess: audit-confirmed reasonable first attempt")
  $text = $text.Replace("failed attempt:", "failed attempt: audit-confirmed recoverable miss")
  $text = $text.Replace("retraceable clue:", "retraceable clue: audit-confirmed clue can be retraced")
  $text = $text.Replace("rescue path:", "rescue path: audit-confirmed hint/retry path")
  $text = $text.Replace("too easy risk:", "too easy risk: audit-confirmed not guessable")
  $text = $text.Replace("impossible risk:", "impossible risk: audit-confirmed not opaque after retrace")
  $text = $text.Replace("trigger:", "trigger: player action at a grounded site")
  $text = $text.Replace("lore hook:", "lore hook: tied to Deep Line, Watcher, Wren, or keeper motif")
  $text = $text.Replace("body:", "body: player stops, turns, crouches, or looks back")
  $text = $text.Replace("source:", "source: appears to come from the world or player action")
  $text = $text.Replace("restraint:", "restraint: one beat, cooldown respected, no spam")
  $text = $text.Replace("aftertaste:", "aftertaste: adds a lore question rather than an instruction card")
  $text = $text.Replace("failure if under 4:", "failure if under 4: synthetic pass scored 5")
  $text = $text.Replace("approach:", "approach: audit-confirmed dark-copy approach reads clearly")
  $text = $text.Replace("borrowed lantern route:", "borrowed lantern route: audit-confirmed route uses limited borrowed lanterns")
  $text = $text.Replace("light radius:", "light radius: audit-confirmed safe edge is readable")
  $text = $text.Replace("clue readable:", "clue readable: audit-confirmed clue can be read without operator narration")
  $text = $text.Replace("exit route:", "exit route: audit-confirmed retreat path is legible")
  $text = $text.Replace("lantern break / retreat pressure:", "lantern break / retreat pressure: audit-confirmed borrowed lantern is broken by the figure and retreat remains readable")
  $text = $text.Replace("failed-cheese attempt:", "failed-cheese attempt: audit-confirmed blocked")
  $text = $text.Replace("fixture proof:", "fixture proof: audit-confirmed /obs unlit audit fixture proof")
  $text = $text.Replace("fix:", "fix: none")
  [System.IO.File]::WriteAllText($completeNotes, $text, [System.Text.UTF8Encoding]::new($false))
}
if (Test-Path $completeFixes) {
  [System.IO.File]::WriteAllText($completeFixes, "# Fixes From Rehearsal - audit`n`nNo unresolved blockers in this synthetic validator self-test.`n", [System.Text.UTF8Encoding]::new($false))
}
if (Test-Path $completeAttestations) {
  $attestationText = Get-Content -LiteralPath $completeAttestations -Raw
  $attestationText = $attestationText.Replace("- [ ]", "- [x]")
  $attestationText = $attestationText.Replace("evidence:", "evidence: audit-confirmed live-server proof captured")
  $attestationText = $attestationText.Replace("decision: LAUNCH / DO NOT LAUNCH", "decision: LAUNCH")
  $attestationText = $attestationText.Replace("reason:", "reason: synthetic validator self-test completed all launch attestations")
  [System.IO.File]::WriteAllText($completeAttestations, $attestationText, [System.Text.UTF8Encoding]::new($false))
}
$shotDir = Join-Path $completePacket "screenshots"
$clipDir = Join-Path $completePacket "clips"
foreach ($site in $majorSites) {
  foreach ($shot in @("approach", "focal", "action", "exit")) {
    [System.IO.File]::WriteAllText((Join-Path $shotDir "$site`_$shot.png"), "audit placeholder", [System.Text.UTF8Encoding]::new($false))
  }
}
foreach ($clipName in @(
  "first-hour.mp4",
  "ambient-scare.mp4",
  "directed-scare.mp4",
  "dread-route.mp4",
  "wren-companion.mp4",
  "tier-0-implication.mp4",
  "unlit-expedition.mp4",
  "record-web.mp4",
  "finale-release.mp4"
)) {
  [System.IO.File]::WriteAllText((Join-Path $clipDir $clipName), "audit placeholder", [System.Text.UTF8Encoding]::new($false))
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $validatorFile -RepoRoot $RepoRoot -PacketDir $completePacket | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "check_rehearsal_packet.ps1 failed a synthetic completed packet"
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

Write-Host "live rehearsal evidence check: OK - required capture lanes, first-hour pacing, major sites, NPC/world contracts, puzzle fairness, blockers, and doc links are present"
