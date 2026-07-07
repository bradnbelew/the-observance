param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$OutRoot = "rehearsals",
  [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
  [switch]$Force
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string] $Root, [string] $Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$outRootFull = Resolve-UnderRepo $repoFull $OutRoot
$packetDir = [System.IO.Path]::GetFullPath((Join-Path $outRootFull $Date))

if (-not $packetDir.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to create rehearsal packet outside repo: $packetDir"
}

if ((Test-Path $packetDir) -and -not $Force) {
  throw "Rehearsal packet already exists: $packetDir. Re-run with -Force to overwrite template files."
}

$screenshotsDir = Join-Path $packetDir "screenshots"
$clipsDir = Join-Path $packetDir "clips"
New-Item -ItemType Directory -Force -Path $screenshotsDir | Out-Null
New-Item -ItemType Directory -Force -Path $clipsDir | Out-Null

function FileSha1OrInstruction([string] $Path, [string] $Instruction) {
  if (Test-Path $Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
  }
  return "MISSING - $Instruction"
}

function PluginJarPath([string] $Root) {
  $buildFile = Join-Path $Root "plugin\build.gradle"
  if (-not (Test-Path $buildFile)) {
    return Join-Path $Root "plugin\build\libs\observance-UNKNOWN.jar"
  }
  $buildText = Get-Content -LiteralPath $buildFile -Raw
  $versionMatch = [regex]::Match($buildText, "(?m)^version\s*=\s*'([^']+)'")
  $version = if ($versionMatch.Success) { $versionMatch.Groups[1].Value } else { "UNKNOWN" }
  return Join-Path $Root "plugin\build\libs\observance-$version.jar"
}

$pluginJarPath = PluginJarPath $repoFull
$pluginJarSha1 = FileSha1OrInstruction $pluginJarPath "run tools\package_plugin.ps1 before final rehearsal"
$resourcepackZip = Join-Path $repoFull "observance-resourcepack.zip"
$resourcepackSha1 = FileSha1OrInstruction $resourcepackZip "run tools\package_assets.ps1 before hosting the pack"

$majorSites = @(
  "rune_rosetta",
  "stone_vaun",
  "stone_mara",
  "stone_sella",
  "school_stand",
  "the_far_water",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "stone_orin",
  "stone_brann",
  "stone_iss",
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

$lanes = @(
  "First hour",
  "Visuals",
  "Dialogue proof",
  "Puzzle fairness",
  "Side paths",
  "Scares",
  "Unlit",
  "Record/web",
  "Finale"
)

$firstHourBeats = @(
  "spawn / join read",
  "first wrongness",
  "first social signal",
  "first meaningful choice",
  "first puzzle action",
  "first side pull",
  "first scare pressure",
  "handoff to live route"
)

$directorAxes = @(
  "haunted place, not puzzle course",
  "NPCs have separate jobs",
  "side paths change belief",
  "manual builds require body verbs",
  "operator stays invisible",
  "finale restores a person, not a password"
)

$sidePaths = @(
  "school_stand",
  "the_far_water",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "deep_bird_coops",
  "deep_market",
  "ration_table",
  "third_bay_breach",
  "warm_town_collapse"
)

$unlitHouses = @(
  "unlit_house_lamp",
  "unlit_house_cairn",
  "unlit_house_coop",
  "unlit_house_well",
  "unlit_house_watch",
  "unlit_house_warm",
  "unlit_house_threshold",
  "unlit_house_base"
)

$puzzleFamilies = @(
  "rune literacy / answer sign",
  "keeper ciphers",
  "side cipher / mirror read",
  "custom actions: bow / offering / kept light / dark hours",
  "NPC errands: third lamp / dead stall",
  "co-op plate / threshold vault",
  "Record web jump / oracle inscription",
  "Accepting rite / Seventh choice",
  "Nether and End deepening lanes"
)

$contracts = @(
  "Aro/Lamp-works: Stair, third lamp, painted line, and empty bird coops.",
  "School stand: slate, copied light-rule, six stones, and grey seventh marker are physically legible.",
  "Far water: mirror pool, copybook shelf, six stones, and grey seventh marker are physically legible.",
  "Markers row: six bow-stones, worn bow marks, and the empty seventh hollow are physically legible.",
  "Cistern 7: black water, pale arch, good-oil jars, and lying-lamp reflection are physically legible.",
  "Watch-floor: standing log, black-moon warning, and finished kept line are physically legible.",
  "Set-apart shelf: entry 5 warm lamp and redacted count are physically legible.",
  "Undercroft seal: standing line and low bow-to-read line are physically legible.",
  "Forgotten Mouth: true way up, healed surface, last draft, and return mark are physically legible.",
  "Deep Market: 18-stall market reads as a real place before the warm-town contradiction.",
  "Ration table: half-loaf/no-head setting and child line are physically legible.",
  "Third bay: Deep Line breach, cold lamp, and no-road warning are physically legible.",
  "Aro/warm-town lie: warm_town_collapse visibly contradicts the promised town.",
  "Wenna/dead-stall: stall exists and bread/wheat/cookie offering changes state.",
  "Coll/third-lamp: lamp exists and light action changes state.",
  "Dob/bowing stones: bow marker exists and crouching together matters.",
  "Old Pell/dark hours: black-moon sleep/restraint has a visible consequence.",
  "Wren/reckoning: choice markers exist and are not reachable before the reveal."
)

$notes = [System.Collections.Generic.List[string]]::new()
$notes.Add("# The Observance Rehearsal Packet - $Date")
$notes.Add("")
$notes.Add("Run before launch:")
$notes.Add("")
$notes.Add("- [ ] `powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1` is green.")
$notes.Add("- [ ] `/observance preflight` is green on the actual server.")
$notes.Add("- [ ] Resource pack/audio are enabled for the tester client.")
$notes.Add("- [ ] `screenshots/` and `clips/` contain evidence, not intentions.")
$notes.Add("")
$notes.Add("## Evidence Lanes")
$notes.Add("")
foreach ($lane in $lanes) {
  $notes.Add("- [ ] $lane - evidence file(s):")
  $notes.Add("      notes:")
}
$notes.Add("")
$notes.Add("## First-Hour Pacing")
$notes.Add("")
$notes.Add("The opening must feel like a place becoming wrong before it feels like an answer-entry route.")
$notes.Add("Record timestamps from the uncut first-hour clip. If the operator has to explain the beat, mark it RESHAPE.")
$notes.Add("")
foreach ($beat in $firstHourBeats) {
  $notes.Add("- [ ] $beat")
  $notes.Add("      verdict: KEEP / RESHAPE / REPLACE / CUT")
  $notes.Add("      timestamp:")
  $notes.Add("      player action:")
  $notes.Add("      world evidence:")
  $notes.Add("      friction:")
  $notes.Add("      operator leak:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Major Site Visual Shots")
$notes.Add("")
$notes.Add("For every site: approach, focal object, answer/action surface, exit/return.")
$notes.Add("")
foreach ($site in $majorSites) {
  $notes.Add('- [ ] `' + $site + '`')
  $notes.Add("      status: KEEP / RESHAPE / REPLACE / CUT")
  $notes.Add("      silhouette:")
  $notes.Add("      palette:")
  $notes.Add("      lighting:")
  $notes.Add("      body verb:")
  $notes.Add("      action/answer legibility:")
  $notes.Add("      evidence:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Side Path Value Matrix")
$notes.Add("")
$notes.Add("Each side destination must change belief, create dread, confirm a motif, or earn a useful contradiction.")
$notes.Add("If the honest value is only `"they found another place`", mark it CUT or rewrite the payoff.")
$notes.Add("")
foreach ($site in $sidePaths) {
  $notes.Add('- [ ] `' + $site + '`')
  $notes.Add("      clue that pointed here:")
  $notes.Add("      arrival read:")
  $notes.Add("      value: belief / dread / confirmation / motif")
  $notes.Add("      ignore risk:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## NPC/World Contracts")
$notes.Add("")
foreach ($contract in $contracts) {
  $notes.Add("- [ ] $contract")
  $notes.Add("      dialogue evidence:")
  $notes.Add("      landmark/action evidence:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Puzzle Fairness Matrix")
$notes.Add("")
$notes.Add("Every puzzle family must be retraceable after one failed attempt. Hard is fine; opaque is not.")
$notes.Add("If a tester solves by guessing, record why the clue surface failed. If they stall after retracing, add or rewrite rescue.")
$notes.Add("")
foreach ($family in $puzzleFamilies) {
  $notes.Add("- [ ] $family")
  $notes.Add("      verdict: KEEP / RESHAPE / REPLACE / CUT")
  $notes.Add("      first guess:")
  $notes.Add("      failed attempt:")
  $notes.Add("      retraceable clue:")
  $notes.Add("      rescue path:")
  $notes.Add("      too easy risk:")
  $notes.Add("      impossible risk:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Scare Review")
$notes.Add("")
foreach ($scare in @("ambient", "directed", "dread route", "Wren/companion", "Tier-0 implication")) {
  $notes.Add("- [ ] $scare")
  $notes.Add("      verdict: KEEP / RESHAPE / REPLACE / CUT")
  $notes.Add("      trigger:")
  $notes.Add("      lore hook:")
  $notes.Add("      body:")
  $notes.Add("      source:")
  $notes.Add("      restraint:")
  $notes.Add("      aftertaste:")
  $notes.Add("      cut or rewrite if it only says `"be scared now`":")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Unlit Expedition Proof")
$notes.Add("")
$notes.Add("The Unlit must read as a dark copy of home, not a combat dungeon or a numbered checklist.")
$notes.Add("Run these commands on the live server after the curated village is pasted:")
$notes.Add("")
$notes.Add('- [ ] `/obs unlit site entry` at the outside entry ritual point')
$notes.Add('- [ ] `/obs unlit site spawn` at the mirror spawn inside `observance_unlit`')
$notes.Add('- [ ] `/obs unlit site exit` at the retreat/extraction point')
$notes.Add('- [ ] `/obs unlit buildmode off` before any player-facing Unlit test')
$notes.Add('- [ ] `/obs unlit darken all [radius]` after final clue placement')
$notes.Add('- [ ] `/obs unlit border` after final spawn placement')
$notes.Add('- [ ] `/obs unlit audit` reports fixture proof, stray light OK, and border OK')
$notes.Add('- [ ] `/obs unlit ready` prints the playtest handoff command')
$notes.Add('- [ ] `/obs unlit pass light` proves dark pressure and borrowed lantern safety')
$notes.Add('- [ ] `/obs unlit pass stalker` proves display-figure stalk/vanish')
$notes.Add('- [ ] `/obs unlit pass extinguish` proves an exposed borrowed lantern is broken/taken')
$notes.Add('- [ ] `/obs unlit pass house` proves varied house clues')
$notes.Add('- [ ] `/obs unlit pass extract` proves retreat/death inventory return')
$notes.Add("")
$notes.Add("For each house, capture approach, borrowed lantern route, light radius, clue readable, exit route, lantern break/retreat pressure, and one failed-cheese attempt.")
$notes.Add("The houses are non-linear. Do not write notes that assume a numbered expedition order.")
$notes.Add("")
foreach ($house in $unlitHouses) {
  $notes.Add('- [ ] `' + $house + '`')
  $notes.Add("      verdict: KEEP / RESHAPE / REPLACE / CUT")
  $notes.Add("      approach:")
  $notes.Add("      borrowed lantern route:")
  $notes.Add("      light radius:")
  $notes.Add("      clue readable:")
  $notes.Add("      exit route:")
  $notes.Add("      lantern break / retreat pressure:")
  $notes.Add("      failed-cheese attempt:")
  $notes.Add("      fixture proof:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## First-Hour Notes")
$notes.Add("")
$notes.Add("- what the player saw first:")
$notes.Add("- what they tried without help:")
$notes.Add("- when the world first felt wrong:")
$notes.Add("- when it started feeling like a puzzle course:")
$notes.Add("")
$notes.Add("## Director Cut Scorecard")
$notes.Add("")
$notes.Add("This is the taste gate. Score each axis 1-5 after watching the rehearsal evidence.")
$notes.Add("A launch packet needs every axis at 4 or 5. A 3 is not `"close enough`"; it becomes a fix.")
$notes.Add("")
foreach ($axis in $directorAxes) {
  $notes.Add("- [ ] $axis")
  $notes.Add("      score: 1 / 2 / 3 / 4 / 5")
  $notes.Add("      evidence:")
  $notes.Add("      failure if under 4:")
  $notes.Add("      fix:")
}
$notes.Add("")
$notes.Add("## Stop/Launch Decision")
$notes.Add("")
$notes.Add("- [ ] Every required major site is KEEP or explicitly cut from the live route.")
$notes.Add("- [ ] Every NPC/world contract has proof.")
$notes.Add("- [ ] Each evidence lane has a screenshot or clip.")
$notes.Add("- [ ] Every RESHAPE has an exact fix.")
$notes.Add("- [ ] Every REPLACE is removed from the live route until rebuilt.")

$fixes = @"
# Fixes From Rehearsal - $Date

Use this as the working punch list after the rehearsal. Do not soften blockers into "later polish."

| Priority | Site/Beat | Problem | Fix | Owner | Done |
| --- | --- | --- | --- | --- | --- |
| P0 |  |  |  |  | [ ] |

## Blockers To Watch

- Major site reads as a test pad, small prop, floating marker, or route label.
- NPC line names a landmark that is not physically legible.
- Side path produces no new belief, dread, or confirmation.
- Scare feels like command/UI text instead of world pressure.
- Puzzle has no retraceable clue after one failed attempt.
- First hour becomes answer entry before the world feels wrong.
- Wren sounds like exposition.
- Record page feels like documentation.
- Finale feels like password entry.
- Any Director Cut Scorecard axis is below 4.
"@

$attestations = @'
# Launch Attestations - __DATE__

These are the live-server facts that static checks cannot prove. Fill them after the real Paper/client
pass. `tools\check_rehearsal_packet.ps1` treats unchecked or template evidence as a launch blocker.

## Supabase Live Status

- [ ] `discord/supabase/apply-all.sql` was applied to the live Supabase project.
- [ ] `/observance status` on the live server showed `supabase configured: true`.
- [ ] `/observance status` on the live server showed `last db call ok: true`.
- [ ] `/observance status` on the live server showed `queued writes: 0`.
evidence:

## Server Load

- [ ] Plugin `observance-0.3.22.jar` loaded on the target Paper 1.21.11 server.
- [ ] Plugin jar SHA1 matched the current repo package: __PLUGIN_JAR_SHA1__.
- [ ] Datapack `observance` loaded with no compatibility warnings.
- [ ] Hosted resource pack downloaded for a real client.
- [ ] Hosted resource pack SHA1 matched the current repo zip: __RESOURCEPACK_SHA1__.
- [ ] `/observance status` showed `pack readiness` with every rehearsal client `LOADED`.
- [ ] No plugin/datapack/resource-pack errors appeared in console during join and first route.
evidence:

## Real Client Rendering

- [ ] Books display correctly.
- [ ] Signs fit and remain readable.
- [ ] Item lore is readable.
- [ ] Titles, subtitles, actionbars, and bossbars display correctly.
- [ ] Custom rune font glyphs render from the hosted resource pack.
- [ ] Sounds and particles fire in-world at usable volume/timing.
- [ ] NPC lines display and correspond to physical proof.
- [ ] Resource-pack fallback behavior is acceptable if a client declines the pack.
evidence:

## Live Command Audits

- [ ] `/observance preflight` passed.
- [ ] `/observance visualaudit` passed.
- [ ] `/observance dialogueaudit` passed.
- [ ] `/obs unlit audit` passed.
- [ ] `/obs unlit ready` passed.
evidence:

## External Media

- [ ] No in-world clue points to a missing web/download artifact.
- [ ] `/record/...` routes used by the route loaded from the clue path.
- [ ] Optional media that is not ready is withheld rather than planted.
evidence:

## Session Zero And Capture Consent

- [ ] `design/SESSION-ZERO.md` was read before players entered the fiction.
- [ ] Every player understood behavior/chat/voice observation boundaries.
- [ ] Opt-out choices were recorded.
- [ ] `observer_capture` and `voice_capture` match the consent state.
- [ ] `players.observer_opt_out` is `true` for opted-out or unclear players.
evidence:

## Credential Rotation

- [ ] Previously exposed Supabase service-role credentials were rotated or confirmed dead.
- [ ] Previously exposed Discord bot credentials were rotated or confirmed dead.
- [ ] Live Render/Vercel/plugin config uses the fresh credentials.
- [ ] `/observance status` still passed after rotation.
evidence:

## Operator Verdict

- [ ] All above attestations are true.
- [ ] Any failed rehearsal item has been fixed and re-proven.
decision: LAUNCH / DO NOT LAUNCH
reason:
'@.Replace("__DATE__", $Date).Replace("__PLUGIN_JAR_SHA1__", $pluginJarSha1).Replace("__RESOURCEPACK_SHA1__", $resourcepackSha1)

[System.IO.File]::WriteAllLines((Join-Path $packetDir "00-notes.md"), $notes, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $packetDir "fixes.md"), $fixes, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $packetDir "launch-attestations.md"), $attestations, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $screenshotsDir "README.md"), @"
Put approach/focal/action/exit screenshots here. Name files with the site id first.

Required filename shape:
- site_id_approach.png
- site_id_focal.png
- site_id_action.png or site_id_answer.png
- site_id_exit.png or site_id_return.png

Run after rehearsal:
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_rehearsal_packet.ps1 -PacketDir "$packetDir"
"@, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $clipsDir "README.md"), @"
Put first-hour, scare, Unlit, Record/web, and finale clips here.

Required filename hints:
- first-hour
- ambient
- directed
- dread
- wren or companion
- tier-0
- unlit
- record or web
- finale or release

Run after rehearsal:
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_rehearsal_packet.ps1 -PacketDir "$packetDir"
"@, [System.Text.UTF8Encoding]::new($false))

Write-Host "rehearsal packet created: $packetDir"
Write-Host "next: fill 00-notes.md and launch-attestations.md, add screenshots/clips, move blockers into fixes.md, then run tools\check_rehearsal_packet.ps1"
