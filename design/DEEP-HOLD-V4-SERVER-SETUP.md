# Deep Hold V4 and ARG Production Setup

> **SUPERSEDED V4 ARCHIVE — DO NOT OPERATE FROM THIS FILE.** V5 setup is
> `design/V5-WORLD-SETUP-AND-TESTING.md`; production deployment is
> `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.

This is the authoritative operator order for Observance `0.3.29`. Commands beginning with `/obs`
are run in Minecraft as an op unless a console form is shown. Keep the server whitelisted and empty
during placement. Do not deploy rejected Hold jars `0.3.25` through `0.3.28`.

## 1. Package and back up

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\package_launch_bundle.ps1
```

Use `observance-deploy-manifest.json` as the receipt for all hashes. Before changing the server, stop
Paper and back up the overworld, Nether, End, `observance_unlit` if it exists, and
`plugins/Observance/`. Preserve `plugins/Observance/sites.yml`; it is the live coordinate registry.

Install:

- Paper `1.21.11`, Java 21, and at least 4 GB RAM for the one-time Hold build.
- `plugin/build/libs/observance-0.3.29.jar` as the only Observance jar in `plugins/`.
- `observance-datapack.zip` in `<main-world>/datapacks/`, still zipped.
- `observance-resourcepack.zip` at a direct HTTPS URL.
- Multiverse-Core when the external Unlit expedition is enabled.
- Citizens is optional. With it, NPCs use player bodies; without it, the tested armor-stand fallback
  remains functional.

Recommended `server.properties` policy:

```properties
gamemode=adventure
force-gamemode=true
enable-command-block=true
spawn-protection=0
white-list=true
```

Observance protects the entire Hold and entry stair from non-admin breaking, placement, fluids,
explosions, pistons, entity damage, and removal of evidence books. `spawn-protection=0` avoids an
unrelated vanilla protection layer masking interaction tests.

In `plugins/Observance/config.yml`, configure the Supabase service-role key through the server-only
`OBSERVANCE_SUPABASE_KEY` environment variable and set the resource-pack URL/SHA1 from the deploy
manifest. Never put either secret in the repository.

Generate and apply the current database bundle before live progression:

```powershell
Set-Location discord
npm install
npm run db:seed
```

Apply `discord/supabase/apply-all.sql` to the live Supabase project. Start the Discord service from
`discord/` after filling a private `.env` from `.env.example`:

```powershell
npm run register
npm run start
```

## 2. Controlled first boot

Start Paper with no players except the operator. Run:

```text
/datapack list enabled
/obs sleep on
/obs reload
/obs status
```

Require the Observance datapack, Observance `0.3.29`, Supabase configured, last DB call OK, queued
writes `0`, and the current resource pack loaded by the operator client. Fix any failure before world
placement.

## 3. Choose and build the Deep Hold

The command point is the center floor block of the one Surface Mouth. Facing does not rotate the
build: positive world Z is always forward. Reserve the complete envelope around the Mouth:

```text
X: mouth - 118 through mouth + 118
Y: mouth - 104 through mouth + 12
Z: mouth - 6   through mouth + 378
```

The Mouth should be on ordinary surface terrain. On a normal `-64` minimum-Y overworld, Mouth Y must
be at least `55`; terrain around sea level is suitable when the positive-Z footprint stays covered.
The read-only survey requires 12 blocks above every non-Mouth roof, 12 blocks below the deepest
foundation, and no registered non-Hold story site in the envelope. A failed survey changes no blocks.
Do not place this over the village well, a prior Hold, another protected structure, or player builds.

From a player standing on the exact Mouth block:

```text
/obs placehold build
```

From console, the equivalent is:

```text
/obs placehold build <world> <mouthX> <mouthY> <mouthZ>
```

There is no depth argument. The initial placement changes millions of blocks and should be done only
during maintenance. A successful build reports all `76/76` canonical fixtures. It is safe to rerun at
the exact same Mouth: the geometry and content are idempotent, while a conflicting registered site
causes a refusal.

Immediately run:

```text
/obs placehold audit
```

The required receipt is:

```text
hold sites: 76/76
gates:      8/8
records:    8/8
protected Hold and entry stair
virtual-open full traversal
critical findings: 0
```

All eight gates must remain sealed for launch. The Mouth and Grand Stair are never gated. For an
operator-only hardware test, cycle each gate and reseal it:

```text
/obs placehold open keeper
/obs placehold seal keeper
/obs placehold open archive
/obs placehold seal archive
/obs placehold open undercroft
/obs placehold seal undercroft
/obs placehold open deep
/obs placehold seal deep
/obs placehold open prior
/obs placehold seal prior
/obs placehold open dread
/obs placehold seal dread
/obs placehold open accepting
/obs placehold seal accepting
/obs placehold open coda
/obs placehold seal coda
/obs placehold audit
```

Run `/obs placehold sync` only after Supabase is live. Gate sync is monotonic: a canonical true flag
opens and latches its gate; a later false/missing response cannot reseal it. Explicit operator `seal`
is the only reset path.

| Gate command id | Opens from |
| --- | --- |
| `keeper` | `rosetta_known` |
| `archive` | first canonical Keeper investigation begun |
| `undercroft` | `undercroft_open` |
| `deep` | `deep_gate_open` and its canonical Iss/Seventh derivation |
| `prior` | `prior_absence_known` or later Prior record state |
| `dread` | `iss_caught` or `seventh_suspected` |
| `accepting` | `prior_witness_ready` and the Accepting/Threshold on-ramp |
| `coda` | `bowed_as_one` |

## 4. Place the external prologue and relay

The prologue remains at the players' actual group base, not in the Hold. Stand where the first report
should appear and run:

```text
/obs placeprologue
```

This places/registers `first_report_lectern_01` and `first_marker_01`. At the intended south approach
for the Minecraft-to-Discord handoff, stand with at least ten clear blocks north and run:

```text
/obs placerelay
```

Do not use `/obs prepworld`, `/obs fullrun`, `/obs placelab`, or `/obs director lab` in production;
those commands make disposable rehearsal layouts.

For remaining non-Hold sites:

```text
/obs site todo
/obs site launch
/obs site plan lanes
/obs site next prologue
/obs site next dimensions
```

For each outside-Hold anchor, read `/obs site plan <siteId>`, stand at the final location, run
`/obs site set <siteId>`, and then use `/obs placeworld` where that lane's placement brief requires it.
Never hand-survey a Hold-owned fixture; `/obs placehold build` owns those 76 sites. Complete the
Nether and End launch lanes shown by `/obs site launch` before claiming whole-ARG readiness.

## 5. Place NPCs at authored locations

Spawn NPCs one at a time so the production village does not become a test row. Stand at each final
village position and run the corresponding command:

```text
/obs townsfolk spawn aro
/obs townsfolk spawn wenna
/obs townsfolk spawn coll
/obs townsfolk spawn dob
/obs townsfolk spawn old-pell
/obs wren spawn
```

Right-click every NPC after placement. Re-running a spawn relocates the existing body instead of
cloning it. The valid removal commands are `/obs townsfolk despawn <id>` and `/obs wren despawn`.

The presiding Keeper only opens while physically inside `the_threshold` or `keeper_altar`. Teleport
to the built altar, stand at the intended NPC feet, and spawn it:

```text
/obs visit keeper_altar
/obs keeper spawn threshold
```

Move a few blocks if needed so the Keeper is inside interaction range without blocking the aisle,
then right-click it. Use `/obs keeper despawn` before relocating it. The optional node text is a
director hint; canonical flags still decide dialogue state.

The V4 Unwriting chamber already places the finale markers. Do not run `/obs finale` there unless an
audit proves those markers were deliberately removed. After all six Keeper stones exist, run
`/obs reading` once to materialize the six Seventh Reading fragments, then verify every fragment with
the current resource pack.

## 6. Keep the Unlit in the village well

The only Unlit entrance remains the real village well. Stop Paper, copy the main world folder to
`observance_unlit`, remove `observance_unlit/uid.dat`, start Paper, then import the literal copy:

```text
/mv import observance_unlit NORMAL
/mvtp observance_unlit
```

Author it in this order:

```text
/obs unlit buildmode on
```

In the real overworld well, stand at the exact referenced entry and run `/obs unlit site entry`.
In `observance_unlit`, place `/obs unlit site spawn` and `/obs unlit site exit`. Then stand at the
readable focal point in each of eight different authored houses and run:

```text
/obs unlit clue lamp
/obs unlit clue cairn
/obs unlit clue coop
/obs unlit clue well
/obs unlit clue watch
/obs unlit clue warm
/obs unlit clue threshold
/obs unlit clue base
```

Finish and lock it:

```text
/obs unlit border 138
/obs unlit darken all 138
/obs unlit buildmode off
/obs unlit audit
/obs unlit ready
```

All eight evidence flags are required. The houses are non-linear. Do not create a Hold-to-Unlit
passage and do not move the well entry; NPC dialogue and the release record both rely on it.

## 7. Final launch gate

Restart Paper once after all placement, then rerun:

```text
/obs reload
/obs status
/obs placehold audit
/obs unlit audit
/obs unlit ready
/obs audit
/obs visualaudit
/obs dialogueaudit
/obs coverage
/obs preflight
```

The isolated V4 test world may report the expected external prologue/NPC/Unlit sites as missing;
production may not. `/obs preflight` must be clean after those external lanes are installed.

Perform the final playthrough on a non-op Adventure account with the resource pack loaded:

1. Walk down and back up the same Grand Stair at every progression stage.
2. Confirm every sealed gate blocks its full wall, ceiling, and side approaches.
3. Enter correct and incorrect puzzle answers; inspect all books, lecterns, containers, signs, visual
   clues, Keeper mechanics, and dynamic Threshold fragments.
4. Leave after each opened gate, restart Paper, return through the Mouth, and confirm the gate remains
   open and no solved state or critical item was lost.
5. Try breaking, placing, buckets, pistons, explosions, book theft, and fixture damage as a non-op.
6. Complete the well-based Unlit expedition, return, finish Prior/Accepting/Coda, perform the Seventh
   choice and release, then walk back out through the original Mouth.

When this passes, back up the worlds and `plugins/Observance/sites.yml` again, record the artifact
hashes and screenshots in the launch packet, run `/obs sleep off`, and only then open the whitelist.
