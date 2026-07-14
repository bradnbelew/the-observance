# THE OBSERVANCE - LIVE REHEARSAL EVIDENCE PACKET

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT RECORD V5 EVIDENCE HERE.** Execute and retain receipts against `design/V5-LIVE-TEST-MATRIX.csv`.

> Static audits prove the build is wired. This packet proves the experience is worth showing to friends.
> Do not treat a green `audit_all.ps1` as launch approval until this pass has evidence attached.
> Also run `powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_world_build_readiness.ps1 -Launch`
> after staging the world. That gate proves outside-Hold launch-required site coordinates are real; this
> packet proves those placed sites and the generated Deep Hold rooms are good.
> The ordered human launch handoff lives in `design/MANUAL-LAUNCH-PLAN.md`.

## Output

Create one rehearsal folder per run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_rehearsal_packet.ps1
```

```text
rehearsals/YYYY-MM-DD/
  00-notes.md
  launch-attestations.md
  screenshots/
  clips/
  fixes.md
```

After the rehearsal, validate the completed packet:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_rehearsal_packet.ps1 -PacketDir rehearsals\YYYY-MM-DD
```

For The Unlit specifically, the final handoff gate is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_unlit_playtest_ready.ps1 -PacketDir rehearsals\YYYY-MM-DD
```

When that prints `unlit playtest readiness: OK`, stop building and let Nano playtest.

The validator is intentionally strict: it expects completed checkboxes, named screenshots for every major
site, named clips for the first hour, scare families, Unlit expedition, Record/web jump, and finale, plus
`launch-attestations.md` evidence for live-only checks such as Supabase status, exact Supabase SQL/plugin/resource-pack
SHA1 values, real-client rendering, Normal Non-Op Player Pass, session-zero consent, and credential rotation.
Synthetic audit placeholders, text files renamed as images/video, and tiny fake proof files fail the real
launch validator. The only synthetic pass is the tool's own internal self-test.

`00-notes.md` should be short and blunt. For every stop, write:

- site or beat id
- what the player saw first
- what they tried without help
- whether the moment was `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`
- the exact fix if it failed

## Required Evidence

Capture these before inviting the group:

| Lane | Evidence | Pass Standard |
| --- | --- | --- |
| First hour | 10 minute uncut clip from join to first meaningful choice | Feels like a haunted place before it feels like a puzzle course. |
| Visuals | Approach, focal object, answer surface, and exit screenshot for every major site | No major site reads as a test pad, tiny prop, floating marker, or labeled route. |
| Dialogue proof | Screenshot/clip of each NPC line that names a place, followed by the place itself | The named thing exists, matters, and changes what the line means. |
| Puzzle fairness | One solve attempt per puzzle family with notes on first guess, retrace clue, and solve route | Hard is allowed; opaque after retracing is not. |
| Normal non-op | One real non-op player account pass through Deep Hold protection, answer input, wrong-answer attempt, correct answer/input, retrace/return, Unlit pressure action, and pack-loaded rendering | The game works for a player without operator permissions, teleport crutches, build rights, or out-of-fiction explanation. |
| Side paths | Twelve side destinations tested from the clue that points to them | Each one changes belief, creates dread, or pays a motif. No empty walks. |
| Scares | One clip for each scare family: ambient, directed, dread route, Wren/companion, Tier-0 implication | The scare is environmental or personal, not a full-screen instruction card. |
| Unlit | One route clip plus house screenshots for entry, spawn, exit, light use, figure behavior, clue reading, extraction, and anti-cheese | It reads as the dark copy of home, not combat, not a maze, not a numbered checklist. |
| Record/web | Clip from in-world clue to `/record/...` page | The jump feels like discovery, not opening a wiki. |
| Failed Accepting | Clip and screenshots proving case board, sealed prior gate, old camp repair files, failed floor, and `prior_witness_ready` before `rite-tokens` | Players investigate why the prior group failed; they cannot speedrun keeper stones into the finale. |
| Finale | Clip or rehearsal notes for Accepting, Seventh choice, release/kick | The ending feels staged and earned, not password entry. |
| Director cut | Filled `Director Cut Scorecard` in `00-notes.md` | Every axis scores 4 or 5: haunted place, distinct NPCs, valuable side paths, embodied builds, invisible operator, restored-person finale. |

## Blockers

Stop and fix before launch if any of these happen:

- A player says or implies "this is where the plugin wants us to go" about a major site.
- A screenshot needs operator explanation to identify the important object.
- An NPC names a stair, line, stall, lamp, crossing, grave, record, or light that is not physically legible.
- A side path produces no new belief, no dread, and no useful confirmation.
- A scare uses big full-screen text or an obvious command/test label.
- The Unlit can be bypassed with doors, containers, carried light, block changes, or a fixed house order.
- A puzzle has no retraceable clue after the player fails once.
- A real non-op player account can freely break/build inside the Deep Hold protection region, cannot locate
  answer input, cannot recover from a wrong-answer attempt, cannot make a correct answer/input, cannot
  retrace/return without state loss, or never feels an Unlit pressure action.
- The first hour turns into answer submission before the world feels wrong.
- Wren sounds like an exposition dispenser or a temporary placeholder.
- The public listing or Record page feels like documentation instead of a place.
- The final name is experienced as a password rather than a restoration.
- Any Director Cut Scorecard axis scores below 4.

## First-Hour Pacing

The opening must feel like a place becoming wrong before it feels like an answer-entry route.
Record timestamps from the 10 minute uncut first-hour clip. If the operator has to explain the
beat, mark it `RESHAPE`.

For each beat, record:

- `verdict`: `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`. A launch packet may only keep `KEEP`.
- `timestamp`: where the beat happens in the first-hour clip.
- `player action`: what the player did without prompting.
- `world evidence`: what the map, NPC, sound, scare, or site did in response.
- `friction`: confusion, dead air, UI-looking behavior, or pacing drag.
- `operator leak`: any moment where the operator had to explain the experience from outside the world.
- `fix`: exact rewrite, placement change, timing change, or cut if the beat fails.

Minimum required beats:

- spawn / join read
- first wrongness
- first social signal
- first meaningful choice
- first puzzle action
- first side pull
- first scare pressure
- handoff to live route

## Major Site Visual Shots

For each site in this list, capture four screenshots: approach, silhouette/focal object, answer or action
surface, and exit/return view.

For launch validation, every site in this section must be marked `KEEP`. `RESHAPE`, `REPLACE`, and
`CUT` are blocker states until the site is rebuilt, removed from the live route, or re-audited. Each
site entry must also explain:

- silhouette: what reads from approach distance.
- palette: why the materials belong to the Deep Hold / surface layer.
- lighting: what the light says in lore, not just whether the room is visible.
- body verb: what players physically do there: cross, stoop, circle, gather, descend, look up, or look down.
- action/answer legibility: why the interactable surface is readable without operator explanation.

- `rune_rosetta`
- `stone_vaun`
- `stone_mara`
- `stone_sella`
- `school_stand`
- `the_far_water`
- `markers_row`
- `cistern_7`
- `watch_floor`
- `set_apart_shelf`
- `undercroft_seal`
- `forgotten_mouth`
- `stone_orin`
- `stone_brann`
- `stone_iss`
- `stone_of_reckoning`
- `the_cold_hearth`
- `unbroken_light`
- `the_threshold`
- `the_unwriting`
- `threshold_vault`
- `case_board`
- `prior_camp`
- `failed_accepting`
- `nether_forge`
- `end_seventh_shrine`
- `lampworks_stair`
- `third_lamp_stand`
- `painted_line`
- `dead_stall`
- `deep_bird_coops`
- `deep_market`
- `ration_table`
- `third_bay_breach`
- `warm_town_collapse`
- `dread_route_start`
- `dread_route_elsewhere`
- `dread_route_figure`
- `dread_route_exit`
- `unlit_entry`
- `unlit_spawn_mirror`
- `unlit_exit`
- `unlit_house_lamp`
- `unlit_house_cairn`
- `unlit_house_coop`
- `unlit_house_well`
- `unlit_house_watch`
- `unlit_house_warm`
- `unlit_house_threshold`
- `unlit_house_base`

## Side Path Value Matrix

Each side destination must change belief, create dread, confirm a motif, or earn a useful contradiction.
If the honest value is only "they found another place," mark it `CUT` or rewrite the payoff.

For each side path, record:

- the clue that pointed there
- what the player understood on arrival
- the belief/dread/confirmation/motif value
- why a player would not safely ignore it next time
- the exact fix if the answer is weak

## NPC/World Contract Shots

For every townsfolk/Wren/Keeper line that mentions a concrete surface, capture:

1. The dialogue line.
2. The landmark from player approach distance.
3. The action that proves the line matters.
4. The state after the action.

Minimum required contracts:

- Aro/Lamp-works: Stair, third lamp, painted line, and empty bird coops.
- School stand: the slate, copied light-rule, six stones, and grey seventh marker read as a child-scale place.
- Markers row: the six bow-stones, worn bow marks, and empty seventh hollow are legible as a count problem.
- Cistern 7: the black water, pale arch, good-oil jars, and lying-lamp reflection are legible without a narrator.
- Watch-floor: the standing log, black-moon warning, and finished `kept` line make Brann's dark-hours proof physical.
- Set-apart shelf: entry 5's warm lamp, cold-lamp shelf, and redacted count are legible without a narrator.
- Undercroft seal: the standing mason line and low bow-to-read line make Orin's seal physical.
- Forgotten Mouth: the true way up, healed surface, last draft, and return mark are legible as a real route.
- Deep Market: the 18-stall market reads as a real place before the warm-town collapse contradicts Aro.
- Far water: the mirror pool, copybook shelf, six stones, and grey seventh marker make Sella's evidence legible without a narrator.
- Ration table: the half-loaf, no-head setting, and crossed child line are legible as human grief.
- Third bay: the Deep Line is visibly broken downward, with a cold set-apart lamp and no-road warning.
- Aro/warm-town lie: the clue points to `warm_town_collapse`, where the collapse visibly contradicts the promised town.
- Wenna/dead-stall: the stall exists and the bread/wheat/cookie offering changes state.
- Coll/third-lamp: the lamp exists and the light action changes state.
- Dob/bowing stones: the bow marker exists and crouching together matters.
- Old Pell/dark hours: black-moon sleep/restraint has a visible consequence.
- Failed Accepting: the case board, sealed prior gate, old camp, six repair signs, and failed floor are physically legible.
- Wren/reckoning: choice markers exist and are not reachable before the reveal.

## Puzzle Fairness Matrix

Every puzzle family must be retraceable after one failed attempt. Hard is fine; opaque is not.
If a tester solves by guessing, record why the clue surface failed. If they stall after retracing,
add or rewrite the rescue path.

For each family, record:

- `verdict`: `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`. A launch packet may only keep `KEEP`.
- `first guess`: what the tester tried before help.
- `failed attempt`: what wrong move should still leave them able to recover.
- `retraceable clue`: the object, line, symbol, NPC phrase, or sound they can return to.
- `rescue path`: the in-world hint, second clue, retry route, or escalation that prevents a dead stop.
- `too easy risk`: why the answer is not obvious by guessing or UI shape alone.
- `impossible risk`: why the solve is still readable after a miss.
- `fix`: exact rewrite, placement change, hint change, or cut if the family fails.

Minimum required families:

- rune literacy / answer sign
- keeper ciphers
- side cipher / mirror read
- custom actions: bow / offering / kept light / dark hours
- NPC errands: third lamp / dead stall
- co-op plate / threshold vault
- Record web jump / oracle inscription
- Failed Accepting / prior-run corrections
- Accepting rite / Seventh choice
- Nether and End deepening lanes

## Failed Accepting Proof

This is the post-keeper anti-speedrun gate. It must feel like an investigation of a failed group,
not another password hallway. The proof packet must show:

- `case_board` has the editable filing sign `file / missing / condition` and accepts `no witness`
  only after the roster evidence is readable.
- The prior gate is physically sealed before `prior_absence_known`, including ceiling, sides,
  fixture tops, and return-route edges.
- `prior_camp` contains the failed inventory, six bedroll packets, six correction barrels, and
  seven editable filing signs.
- `prior_camp` accepts `answers are not witness` and then leaves the six keeper repair files
  solvable in parallel.
- The six corrections are extracted from camp evidence plus earlier keeper/side evidence, not
  guessed from hint text alone.
- A player can leave a repair file, find external evidence, return through an authored route, and
  submit without losing state.
- `failed_accepting` accepts `witness before accepting` and sets `prior_witness_ready` before
  `rite-tokens` can open.
- The old camp reads as abandoned human evidence, not a labelled checklist or tutorial room.

## Normal Non-Op Player Pass

Run this after the operator has finished build/audit commands. Use a real non-op player account, not an
opped alt with restraint. Record the evidence in `launch-attestations.md`.

The pass must prove:

- the player cannot freely break/build inside the Deep Hold protection region.
- books, signs, rune glyphs, containers, answer surfaces, NPC interactions, gates, and return routes work
  without operator permissions.
- one wrong-answer attempt fails recoverably.
- one correct answer/input completes from the intended surface.
- one retrace/return proves the player can leave a puzzle, find missing evidence, come back, and continue.
- the Failed Accepting route is proven non-op: `no witness` -> `answers are not witness` -> six repair
  files -> `witness before accepting`.
- the sealed prior gate blocks ceiling, side-wall, fixture-top, and return-route bypass attempts before
  `prior_absence_known`.
- one Unlit pressure action lands without creative mode, build mode, or admin explanation.
- the resource pack is loaded while the player reads custom rune/font/audio surfaces.

## Scare Review

For each scare clip, score:

- `verdict`: `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`. A launch packet may only keep `KEEP`.
- `trigger`: what player action, location, flag, or observed behavior caused it.
- `lore hook`: which fact, keeper, custom, Deep Line event, Wren beat, Watcher rule, or side motif it deepens.
- `body`: did the player move, stop, turn, crouch, look back, or whisper?
- `source`: did the scare appear to come from the world, a person, or the player's own action?
- `restraint`: did it stop before it became spam?
- `aftertaste`: did it add a question tied to the lore?
- `fix`: exact rewrite/cut/rebuild if the scare is only noise.

If a scare only says "be scared now," cut or rewrite it. If it cannot name a lore hook, it is a
reaction gag, not an Observance scare.

## Unlit Expedition Proof

The Unlit is a major expansion pillar and must pass the same evidence standard as the launch spine.
The live proof must show the operator can finalize it without hand-editing configs:

- `/obs unlit site entry`
- `/obs unlit site spawn`
- `/obs unlit site exit`
- `/obs unlit buildmode off`
- `/obs unlit darken all [radius]`
- `/obs unlit border`
- `/obs unlit audit`
- `/obs unlit ready` with `Gate: READY`
- `/obs unlit pass light`
- `/obs unlit pass stalker`
- `/obs unlit pass extinguish`
- `/obs unlit pass house`
- `/obs unlit pass extract`

The completed packet must include fixture proof, stray light OK, and border OK from `/obs unlit audit`.
For each house, capture approach, borrowed lantern route, light radius, clue readable, exit route, proof
that a borrowed lantern is broken or pressured by the figure, and one failed-cheese attempt. The house
notes must remain non-linear: no numbered expedition assumptions, no "later house" dependency, and no
puzzle that breaks if players visit threshold or base first.

Required houses:

- `unlit_house_lamp`
- `unlit_house_cairn`
- `unlit_house_coop`
- `unlit_house_well`
- `unlit_house_watch`
- `unlit_house_warm`
- `unlit_house_threshold`
- `unlit_house_base`

## Decision Rule

Launch only when:

- every required major site is `KEEP`, or explicitly cut from the live route;
- the Normal Non-Op Player Pass is complete in `launch-attestations.md`;
- every NPC/world contract has proof;
- each required evidence lane has at least one screenshot or clip;
- every `RESHAPE` item has a named owner and exact fix;
- every `REPLACE` item is removed from the live route until rebuilt.

This packet is allowed to be ugly. The ARG is not.
