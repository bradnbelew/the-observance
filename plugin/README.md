# The Observance — V5 Paper plugin

Status: current production implementation
Target: Paper `1.21.11`, Java `21`, plugin `0.5.0`

The plugin builds and runs the Minecraft portion of the V5 campaign. One local coordinator owns the
60 executable Minecraft predicates, durable progression, gates, critical-item custody, recovery,
NPC interaction, Unlit mechanics, and the six-outcome finale. Production V5 does not consume the
retired `beat_queue`, run ambient story generators, or expose legacy Director/answer-sign rites.

The story and puzzle authorities are outside this directory:

- `../arc/WORLD-BIBLE.md` — spoiler truth;
- `../design/ARG-V5-NODE-MANIFEST.csv` — all 82 required nodes;
- `../design/ARG-V5-PHYSICAL-PREDICATES.json` — all 60 Minecraft contracts;
- `../arc/v5/` — exact books, dialogue, evidence appearance, maps, media, and solutions;
- `../design/V5-WORLD-SETUP-AND-TESTING.md` — complete world procedure;
- `V5-RUNBOOK.md` — concise plugin command and recovery reference.

## Build and verify

From `plugin/`:

```powershell
.\gradlew.bat clean check build --no-daemon
```

The sole deployable artifact is:

```text
build/libs/observance-0.5.0.jar
```

The build uses the Java 21 toolchain, runs every main-driven contract suite, removes stale
`observance-*.jar` files, and refuses more than one output JAR. Release packaging must use
`../tools/package_plugin.ps1`; it consumes this Gradle output and byte-compares every packaged V5
authority, evidence appearance record, and map asset to the repository.

Never copy a historical JAR into the live `plugins/` directory. Exactly one Observance JAR may be
present.

## Configure

On first boot Paper writes `plugins/Observance/config.yml` and `sites.yml`.

Set the private Supabase service-role value in the host environment only:

```text
OBSERVANCE_SUPABASE_KEY=<service-role key>
```

The committed configuration names that variable; it never contains the value. Verify the public
Supabase URL, hosted resource-pack URL/SHA-1, `resource-pack.required: true`, V5 mode, and
`finale.production-shutdown: true` in the generated live config.

Supabase is a monotonic mirror and cross-surface receipt store. Minecraft completion is local-first:
a temporary service outage cannot retract a solved node, close an opened gate, duplicate a reward,
or block the local finale. Production preflight still fails until the remote service and required
metadata are healthy.

## Safe world installation

Do not hand-build or paste the Hold. At a selected Surface Mouth, world `+Z` must point away from the
village and every existing build. Run all three commands at the same block and in the same server
session:

```text
/obs placehold prepare
/obs placehold plan
/obs placehold build
```

Console form:

```text
/obs placehold prepare <world> <x> <y> <z>
/obs placehold plan <world> <x> <y> <z>
/obs placehold build <world> <x> <y> <z> +z
```

`prepare` asynchronously generates and tickets the complete footprint without changing story blocks.
`plan` is read-only and must print `PLAN PASS`. `build` applies the deterministic 32-room,
76-fixture, eight-gate structure in bounded tick batches and checkpoints its cursor atomically.
Interrupted builds resume only at the identical Mouth, orientation, and authority hash.

After completion and again after a clean Paper restart:

```text
/obs placehold prepare
```

Wait for `PREPARE PASS` at the exact persisted Mouth, then run:

```text
/obs reload
/obs status
/obs placehold audit
/obs unlit audit
/obs dialogueaudit
/obs preflight
```

The village well remains the only public entrance to the Unlit. It is not the Hold Mouth. Bind the
well, mirrored Unlit spawn/exit, eight distinct house clues, five surface townsfolk, and Wren using
the exact commands in `../design/V5-WORLD-SETUP-AND-TESTING.md`.

## Runtime ownership

The V5 coordinator registers exactly one production path for each surface:

- 41 world-state mechanics;
- 13 container/custody nodes;
- six collective/choice/finale rituals;
- exact authority-backed books, signs, item appearances, frames, labels, and nine map images;
- monotonic gates and book unlocks;
- protected Hold/Unlit regions and immutable evidence;
- synchronous Wren and townsfolk dialogue;
- durable local progress with asynchronous remote mirroring.

The plugin rejects blank or generic touch-to-solve components, wrong orientations, unbound fixtures,
missing standing cells, duplicate artifacts, stale authority hashes, mismatched map IDs, and unsafe
or overlapping Hold placement.

## Player identity proof

`/obslink` is the only non-admin plugin command. It generates 12 random Crockford-base32 symbols,
stores only their SHA-256 through a service-role RPC, and shows the 4-4-4 code once to that exact
online player. The database enforces a five-minute expiry and 30-second reissue cooldown. Players
then use `/link <exact Minecraft name> <recovered callback> <one-time code>` in Discord. The proof is
consumed before any identity row changes, in the same transaction as LS05; it is never queued during
an outage and never written to logs or config. Paper must run with `online-mode=true`; `/obslink`
fails closed without generating or transmitting a code when Mojang has not authenticated the UUID,
and production preflight reports that configuration as a blocker.

## Finale and Coda

The operator cannot choose an ending argument. Wren treatment, Averyn name treatment, and conduct
are read from durable player receipts. After RP01–RP04, inspect and arm the recorded outcome:

```text
/obs finale status
/obs finale markers
/obs finale arm 15
```

The cancellation window is the last intervention point. Once committed, the plugin projects the
selected Coda, saves worlds and players, delivers the branch-specific goodbye, kicks players, and
calls the configured Paper shutdown. Restarting enters read-only Coda; puzzle, Unlit haunting, and
choice inputs stay disabled while Wren and townsfolk expose only the earned aftermath.

Never test an armed finale on the production world. Exercise all six endings on restored rehearsal
clones.

## Release boundary

A green build is not proof of an unseen production world. Launch additionally requires real-client
Adventure traversal, resource-pack rendering, seven-player crowding, all 100 rows in
`../design/V5-LIVE-TEST-MATRIX.csv`, live Discord/Vercel/Render/Supabase checks, and six restored-clone
ending rehearsals. The single production authority for those steps is
`../design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.
