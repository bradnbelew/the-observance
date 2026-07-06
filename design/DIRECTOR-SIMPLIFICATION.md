# Director Simplification Pass

This pass changes the operating rule for The Observance:

> If a feature cannot be rehearsed from one compact test world, it is not launch-ready yet.

Add the visual rule:

> If a feature works mechanically but looks like a test prop, it is not launch-ready either.

The project was drifting toward a hand-placed, hand-tested mountain. The new direction is to keep the full ARG, but make the operator surface smaller, repeatable, and more frightening.

## What Changes Now

### 1. One-Command Rehearsal

Use:

```mcfunction
/obs fullrun
```

This is the new default test entrypoint. It:

- builds the complete floating placement lab,
- registers every enabled site as a live test anchor,
- carves the Seventh Reading onto the keeper lab cells,
- places Wren reckoning markers,
- places finale markers,
- gives the tester the Lens,
- gives the tester the kept needle,
- prints the short order for the human pass.

The old commands still exist for focused work, but the director pass starts with `fullrun`, not a 70-line checklist.

### 2. Manual Placement Is Optional, Not Required

Launch placement can still use `site plan`, `site set`, and `placeworld`, but the project must remain testable without surveying the whole world. The rehearsal lab is the proof surface. For the final production geography, first run `tools\new_launch_placement_packet.ps1`; it creates `00-placement.md`, `launch-sites.csv`, and `coords-capture.csv` so the 42-site survey pass has an external worksheet for coordinates, intent, proof shots, cohesion notes, and visual verdicts.
Run `tools\check_launch_coord_quality.ps1 -CaptureCsv <packet>\coords-capture.csv` before treating the worksheet as real. It catches duplicate anchors, wrong Nether/End dimensions, cramped keeper stones, over-spread route clusters, missing proof shots, and non-`KEEP` launch rows. If the worksheet is the source of truth for the production pass, preview it with `tools\apply_launch_coords.ps1 -CaptureCsv <packet>\coords-capture.csv` before writing. The apply tool refuses open visual verdicts by default; use `-Apply` only after each row is real, placed, proofed, and `KEEP`.

For compact real-world staging, use:

```mcfunction
/obs director world
```

This is now the fastest confidence path: compact placement, first audit, repair, second audit, coverage, and guided rehearsal start in one command. For the floating proof surface instead, use `/obs director lab`.

For focused staging/debugging, use the same pieces manually:

```mcfunction
/obs prepworld
/obs audit
/obs visualaudit
/obs dialogueaudit
/obs preflight
/obs repair
/obs coverage
/obs rehearse start
/obs visit next
/obs descentproof
/obs puzzlepass
/obs sidepass
/obs dreadpass run
/obs runbook spine
```

`prepworld` places the prologue, surface spine, deep payoff sites, Lamp-works/Stair dialogue proof chain, school-stand proof, far-water mirror, marker-row count proof, Cistern 7, watch-floor, Deep Market, ration table, third-bay breach, warm-town false-lead collapse, Mara page-lock lecterns, finale/readings, NPC row, and tester tools around the operator. It is the manual version of the compact staging path. Use manual `site set` only to curate the final production geography or optional Nether/End lanes.

`audit` checks the placed site registry for the boring failures that ruin a run: unplaced core sites, unloaded worlds, missing books, empty lecterns, absent chest/bookshelf hardware, and air at core anchors. `repair` fixes the common placed-site failures: missing lectern books, answer signs, Vaun shelf/chest hardware, blank first marker, and blank code-built set-piece anchors. After repair, run `audit` again.

`visualaudit` is the new art-direction gate. It scans placed story sites for obvious test-prop failures: tiny footprints, flat silhouettes, low material variety, missing intentional light, and exposed operator labels. It cannot replace human taste, but it prevents the worst "it technically works but looks unfinished" misses from passing unnoticed.

`dialogueaudit` is the new NPC truth gate. Any line that names a route, place, object, rule, or consequence must have world proof and mechanic proof, or the line gets built, wired, rewritten, or cut. Use `design/DIALOGUE-WORLD-AUDIT.md` for the human pass.

`descentproof` stages the route the townsfolk keep promising: Lamp-works stair, countable lamp stands, the dark third lamp, the painted line, the dead-stall, and Aro's empty bird coops. It also gives `PaintedLineListener` a real placed `painted_line` site, so crossing the line quietly sets `painted_line_crossed`.

Aro's longer warm-town lie is not part of the compact descent chain, but `prepworld` and `sidepass` now stage Deep Market plus the collapse for rehearsal. The same side pass also stages `school_stand`, `the_far_water`, `markers_row`, `cistern_7`, `watch_floor`, `set_apart_shelf`, `undercroft_seal`, `forgotten_mouth`, the ration table, and third-bay breach so the schoolroom human thread, Sella's mirror/count evidence, the bow/count proof, the light-fouling proof, dark-hours proof, entry-five proof, Orin's seal, the way-up rumor, and Deep Line taboo have physical anchors. For production, survey `school_stand`, `the_far_water`, `markers_row`, `cistern_7`, `watch_floor`, `set_apart_shelf`, `undercroft_seal`, `forgotten_mouth`, `deep_market`, `ration_table`, `third_bay_breach`, and `warm_town_collapse` at their real route positions and run `/obs placeworld`.

`preflight` runs `audit`, `visualaudit`, `dialogueaudit`, and `coverage` together. Use it before deciding a rehearsal world is ready to become the live placement plan.

`runbook` is the in-world director cheat sheet. Use `/obs runbook setup`, `/obs runbook spine`, `/obs runbook side`, `/obs runbook scare`, or `/obs runbook ops` while testing so the next interaction and the relevant skip commands are visible inside Minecraft instead of buried in docs.

`rehearse` is the guided test-pass tracker. Use `/obs rehearse start`, then `/obs rehearse done` after each stage. It walks the operator through setup, hardware repair, main spine, side/lore lanes, Watcher scare checks, and dashboard/production-placement checks.

`coverage` is the launch-lane preflight. It reports whether the prologue, surface spine, Mara books, puzzle mechanic grid, deep payoff, NPC side/lore surfaces, Watcher scare lane, and optional Nether/End lanes are ready, missing, or need repair. The Watcher scare lane is not ready unless drama is enabled and the dread route has been staged.

`visit` is the site navigator for rehearsal. Use `/obs visit next` and `/obs visit back` to jump through placed launch sites in order, or jump straight to `/obs visit prologue`, `/obs visit surface`, `/obs visit mara`, `/obs visit puzzle`, `/obs visit deep`, `/obs visit scare`, `/obs visit dimensions`, or any specific site id.

`puzzlepass` is the compact mechanic proof grid. It stages the detector families that make the ARG feel impossible to test by hand: bow, offering, answer sign, Vaun chest/shelf, Mara lecterns/map, Sella pool/shore, Orin bows/frames, Brann toll/corridor, co-op plate, and Threshold vault. Use `/obs puzzlepass gates` only in rehearsal when you need to open the common gated branches without replaying the whole story.

`sidepass` is the focused side/lore confidence pass. It stages all five townsfolk, Wren, the Keeper, school stand, far water, marker row, Cistern 7, watch-floor, set-apart shelf, undercroft seal, forgotten Mouth, Deep Market, the ration table, third bay, and the warm-town contradiction near the operator, then prints the exact right-click checklist. After running it, `/obs coverage` reports whether those NPC bodies and side proof are actually ready to test, not just whether their subsystems loaded.

Real launch placement should become a curation pass:

1. Run `fullrun` in a test world.
2. Fix broken mechanics there.
3. Run `/obs preflight`, then the visual rescue pass from `design/VISUAL-RESCUE.md` and dialogue-world pass from `design/DIALOGUE-WORLD-AUDIT.md`.
4. Only then choose real-world anchors for the sites that survived mechanically and visually.
5. Avoid placing optional side lanes until the spine feels good.

### 3. The Watcher Must Read As A Figure

Soft ambience is not enough. The scary layer should bias toward:

- humanlike silhouettes,
- something standing where a player was,
- close behind-you sound,
- short darkness pulses,
- player-like or offline-player-shaped glimpses when safe,
- dangerous places like deep dark, Nether pockets, End/exile spaces, and silent corridors.

Use:

```mcfunction
/obs dreadpass run
/obs test gauntlet
/obs test stalker
/obs test hunt
/obs test elsewhere
```

`dreadpass run` is the default Watcher scare rehearsal: it stages a short route as a physical scare sequence, then fires the gauntlet through it. The route must read as a built place: start gate, ribbed dark passage, narrowed middle, wrong-sky/elsewhere chamber, figure niche, and exit threshold. `gauntlet` is the lower-level timed beat chain: wrong sky/weather and ash first, then a stalking figure, then a pursuit beat, then a final close heartbeat/message. The focused commands queue individual scare sequences: darkness, close hostile sound, quiet pressure text, silent humanoid figures, and a client-only wrong-sky/wrong-weather "elsewhere" shift. They are still safe: no griefing, no trap, no real kill requirement, no real teleport.

Live scare language should avoid full-screen command text. A title is allowed only for a rare boundary break or explicit test preset. The normal scare vocabulary is environmental: sound, darkness, ash, silhouettes, changed surfaces, actionbar whispers, fake/temporary world marks, and marks that players discover after turning away.

Runtime defaults now enforce this bias: `private_message` and Tier-0 `composure` default to `actionbar`; `hint_whisper` defaults to an in-world private display. Title delivery requires both `mode: "title"` and `boundary_break: true`; otherwise the beat demotes to actionbar/display. Review every boundary break as rare authored theater, not routine ambience.

Live ambient haunting now favors the same safe scare palette under the existing drama budget: cave/heartbeat-style private sounds, ash/smoke particles, darkness, wrong-sky time/weather shifts, light-dimming, and a very rare no-AI humanoid apparition only at high attention. The world stays restrained, but the Watcher is no longer just soft ambience.

### 4. Lectern Books Are Server-Side

Vercel/dashboard deploys do not fill Minecraft lecterns. Empty lecterns are fixed by the plugin jar and in-world placement commands.

Use:

```mcfunction
/obs placelecterns
```

This stamps the five Mara page-lock lecterns with written books already loaded and persists their sites. For the prologue, `/obs placeprologue` now force-fills the first-report book during manual staging so test runs do not get stuck waiting for an unwitnessed beat retry.

## What To Cut Or Delay

Delay anything that requires manual world dressing before it can be tested. Side stories are allowed to exist, but they should not block the first full ARG rehearsal.

Priority order:

1. Prologue, report, first marker.
2. Six keeper stones.
3. One mechanic per keeper.
4. Deep gate and threshold vault.
5. Seventh Reading and finale markers.
6. Scare layer.
7. Optional Nether/End side lanes.
8. Optional lore/archive flourishes.

## New Definition Of Ready

A story lane is ready only when all four are true:

- it appears in `/obs fullrun`,
- it has a clear solve action,
- it has a single-line operator test instruction in `/obs runbook`.
- it passes the visual rescue standard in `design/VISUAL-RESCUE.md`.

If any of those are false, the lane is not cut forever, but it is not part of the launch confidence pass.
