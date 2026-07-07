# THE OBSERVANCE — WORLD-BUILD BRIEF (the Minimum-Amazing region, concrete)

> The world is the #1 thing that decides "amazing," and it's the biggest manual task (yours alone).
> This turns BUILD-PLAN §11–§12 from principles into a concrete build you can sit down and make.
> Build THIS region first (it IS the Minimum-Amazing world); everything else is extension.
> When a piece is placed, export it as a `.schem` and write its real coords into `plugin/src/main/
> resources/sites.yml` (every coord is `null` today). Relight after every FAWE paste.

## The feel (hold these the whole time)
Built-by-hands-then-abandoned. **Carved, never default** — no flat smooth-stone walls; chisel them
(stairs/slabs/deepslate variants, cracked, mossy where water reached). **Dark is the default; light is
earned** — torches are rare, guttering, often just out of reach. The **bow is built into the
architecture** (low lintels, stones set so you must crouch/look down to read them). Reveal by
**sightline** — round a corner *into* a thing, never announce it. A few deliberate **wrongnesses** (a
chair facing a wall, a table set for hands not there, a door that should not open inward).

## Palette (pick once, keep it)
Deepslate + cobbled/cracked deepslate (walls), deepslate brick (worked rooms), tuff (older strata),
basalt (the deep), polished blackstone (the record/keeper stones), soul-lantern + candle (the rare kept
light), dripstone + dark water (the shore), cobweb/moss/sculk (creep where it's gone wrong). One warm
accent only: the single never-doused fire (campfire/soul-fire under a hearth).

## The region, top → bottom (one vertical descent; the descent IS the dread)

1. **The Mouth (surface entry).** A modest opening in the overworld — a collapsed stair going DOWN, a
   weathered sign/lectern (the ignition lectern: the first report appears here). Site: `report_lectern`.
   The threshold lintel is low — you stoop to enter. (First teaching of the bow, before they know it.)

2. **The Descent Shaft.** A long switchback stair down, deepslate, torches thinning as you go. Carve
   tally-marks into the wall at intervals (lore texture; later the UNKEPT maker's-marks live here). No
   reveals yet — just length and dimming. Ends at a small landing with the two Rosettas.

3. **The Two Rosettas (the literacy on-ramp).** A short carved ring of the six ways (rune-literacy) +
   the numeral tick-stave. Sites: the `rosetta-ring` carving + `a1z26-tick-stave`. A pre-placed labeled
   **answer lectern** ("speak here") beside each (the world-built answer surface — don't rely on the
   group guessing a sign is the input).

4. **The Ways Hall (the keeper-stones, a FIELD not a row).** A wider chamber that branches — the six
   keeper-stones set in alcoves off it, NOT in a line (any-order discovery). For the Minimum-Amazing cut,
   place **3** here (Vaun, Mara, Iss) and leave alcoves stubbed for the rest. Each stone:
   - a `keeper_stone` site + its labeled answer lectern;
   - set LOW or angled so you crouch to read it (the bow, built in);
   - **Vaun's** alcove is a hoard-room (chests in a deliberate arrangement — his sorting puzzle);
   - **Mara's** alcove has the lectern-shelf her book-cipher reads from;
   - **Iss's** alcove is the warmest-lit (warmth = the trap) with the prophet wall of warm promises.

5. **The Cold Hearth (Iss's dead-end → the catch).** Down and west from Iss: a dead room, a hearth that
   is OUT, a grave, nothing kept. This is where the warm reading leads and pays nothing — and the
   dead-end itself plants the doubt that pushes them back to re-test Iss's key. Site: `iss_dead_shrine`.

6. **The Shore Pool (Sella).** A still dark-water pool with a dripstone ceiling. Sella's stone face is
   blank from standing; her rune reads only in the **reflection** (place the carving inverted under the
   lip / use a per-player TextDisplay mirrored below the surface). The most affecting set-piece — give it
   room and silence. Site: `the_far_water`. Points "south by the far water" → the descent toward the Seventh.

7. **The Undercroft (the kept descent — the fog dimension).** The gather-room: one **never-doused fire**
   under a hearth (the single warm accent in the whole build), a long table set for hands not there,
   the record-ledger block. This room is built INSIDE the `observance:undercroft` datapack dimension
   (fog + dark, see below) — the air goes wrong when they descend into it. Site: `report_lectern`/
   `keeper_altar` + the Unlit Deep latch. Behind a sealed door here: the room-swap reveal (teleport, not
   in-place) and, deeper, the Seventh's sealed deep.

8. **The Threshold + the Seventh's Deep (the finale lead-in).** The Threshold (the coop gate / the
   asymmetric vault if you build it) → below the Cold Hearth, the sealed deep where the Seventh waits.
   The **future-dated grave** is placed on the group's most-walked route (desire-path beat) and, at the
   reunion, opens from the inside. Sites: `the_threshold`, `the_unwriting`, `grave`.

## Loading the Undercroft datapack (`datapack/observance/`)
1. `pack.mcmeta` ships datapack format `[94,1]` (`min_format`/`max_format`) — correct for the pinned server MC 1.21.11 (data-pack format 94.1). No change needed unless the server version moves.
2. Drop the `observance` folder into `<world>/datapacks/`, `/reload` (or restart).
3. The custom dimension is `observance:undercroft`. Reach it with `/execute in observance:undercroft run
   tp @s <x> <y> <z>` (or a plugin teleport beat) — Multiverse does NOT create datapack dimensions; the
   datapack defines it, you teleport into it. Build the Undercroft room geometry inside it (FAWE/manual).
4. `mood_sound` points at `observance:whisper`; the resource-pack OGG now ships and is guarded by
   `tools/check_media_readiness.ps1`. **UNTESTED live** - verify the datapack dimension plus hosted
   resource pack together on the server.

## Build order within the region (so a first playtest is possible fast)
The Mouth → Descent → Rosettas → ONE keeper-stone (Vaun) + its answer lectern → one per-player illusion
nearby → one sealed-door reveal. That is the Phase-A vertical slice. Then add Mara + Iss + the Cold
Hearth (the catch), then the Shore, then the Undercroft, then the Seventh's deep.
