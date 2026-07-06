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
/obs test stalker
```

This queues the first stronger scare sequence: darkness, a close heartbeat, warning text, then a tall silent humanoid figure. It is still safe: no griefing, no trap, no real kill requirement.

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
- it has a single-line operator test instruction.

If any of those are false, the lane is not cut forever, but it is not part of the launch confidence pass.
