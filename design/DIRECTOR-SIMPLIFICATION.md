# Director Simplification Pass

This pass changes the operating rule for The Observance:

> If a feature cannot be rehearsed from one compact test world, it is not launch-ready yet.

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

Launch placement can still use `site set` and `placeworld`, but the project must remain testable without surveying the whole world. The rehearsal lab is the proof surface.

For compact real-world staging, use:

```mcfunction
/obs director world
```

This is now the fastest confidence path: compact placement, first audit, repair, second audit, coverage, and guided rehearsal start in one command. For the floating proof surface instead, use `/obs director lab`.

For focused staging/debugging, use the same pieces manually:

```mcfunction
/obs prepworld
/obs audit
/obs repair
/obs coverage
/obs rehearse start
/obs visit next
/obs puzzlepass
/obs sidepass
/obs runbook spine
```

`prepworld` places the prologue, surface spine, deep payoff sites, Mara page-lock lecterns, finale/readings, NPC row, and tester tools around the operator. It is the manual version of the compact staging path. Use manual `site set` only to curate the final production geography or optional Nether/End lanes.

`audit` checks the placed site registry for the boring failures that ruin a run: unplaced core sites, unloaded worlds, missing books, empty lecterns, absent chest/bookshelf hardware, and air at core anchors. `repair` fixes the common placed-site failures: missing lectern books, answer signs, Vaun shelf/chest hardware, blank first marker, and blank code-built set-piece anchors. After repair, run `audit` again.

`runbook` is the in-world director cheat sheet. Use `/obs runbook setup`, `/obs runbook spine`, `/obs runbook side`, `/obs runbook scare`, or `/obs runbook ops` while testing so the next interaction and the relevant skip commands are visible inside Minecraft instead of buried in docs.

`rehearse` is the guided test-pass tracker. Use `/obs rehearse start`, then `/obs rehearse done` after each stage. It walks the operator through setup, hardware repair, main spine, side/lore lanes, Watcher scare checks, and dashboard/production-placement checks.

`coverage` is the launch-lane preflight. It reports whether the prologue, surface spine, Mara books, puzzle mechanic grid, deep payoff, NPC side/lore surfaces, Watcher scare lane, and optional Nether/End lanes are ready, missing, or need repair.

`visit` is the site navigator for rehearsal. Use `/obs visit next` and `/obs visit back` to jump through placed launch sites in order, or jump straight to `/obs visit prologue`, `/obs visit surface`, `/obs visit mara`, `/obs visit deep`, `/obs visit dimensions`, or any specific site id.

`puzzlepass` is the compact mechanic proof grid. It stages the detector families that make the ARG feel impossible to test by hand: bow, offering, answer sign, Vaun chest/shelf, Mara lecterns/map, Sella pool/shore, Orin bows/frames, Brann toll/corridor, co-op plate, and Threshold vault. Use `/obs puzzlepass gates` only in rehearsal when you need to open the common gated branches without replaying the whole story.

`sidepass` is the focused side/lore confidence pass. It stages all five townsfolk, Wren, and the Keeper near the operator, then prints the exact right-click checklist. After running it, `/obs coverage` reports whether those NPC bodies are actually spawned and ready to test, not just whether their subsystems loaded.

Real launch placement should become a curation pass:

1. Run `fullrun` in a test world.
2. Fix broken mechanics there.
3. Only then choose real-world anchors for the sites that survived.
4. Avoid placing optional side lanes until the spine feels good.

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
/obs test gauntlet
/obs test stalker
/obs test hunt
/obs test elsewhere
```

`gauntlet` is the default Watcher scare rehearsal: wrong sky/weather and ash first, then a stalking figure, then a pursuit beat, then a final close heartbeat/message. The focused commands queue individual scare sequences: darkness, close hostile sound, warning text, silent humanoid figures, and a client-only wrong-sky/wrong-weather "elsewhere" shift. They are still safe: no griefing, no trap, no real kill requirement, no real teleport.

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

A story lane is ready only when all three are true:

- it appears in `/obs fullrun`,
- it has a clear solve action,
- it has a single-line operator test instruction in `/obs runbook`.

If any of those are false, the lane is not cut forever, but it is not part of the launch confidence pass.
