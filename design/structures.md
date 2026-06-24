# structures — the Deep Hold world-build spec

The build instructions for every load-bearing site. The **paste pipeline is already wired** (`FaweSchematicPaster` → `SchematicPaster` → `SmallStructureBeat`, reflective-isolated so a missing FAWE degrades to "no paste" not a crash); the **sites are already defined + enabled** in `plugin/src/main/resources/sites.yml` with placeholder coords. What remains is genuinely manual: **build these in-game, export each as a `.schem`, drop the coords into `sites.yml`** (a go-live step — it needs a Minecraft client). This doc is the spec so the build is unambiguous and on-theme.

> **Theme law (so the world feels REAL, not Minecraft-default):** carved stone, not cobble. Use deepslate / tuff / polished basalt / blackstone for the keepers' stonework; oxidized copper + soul-lantern light for the kept fires; nothing modern, nothing colorful. Every keeper-stone is *one slab a person must stoop to read* (the bow is built into the architecture). Runes are signs/lecterns/text-displays in the `observance:runes` font (resource pack), never hand-placed blocks.

## the descent (where things are, per WORLD-BIBLE geography)

Normal-server **spawn** (no Observance structures). The Hold is **found by descending** — a stair/shaft from a discoverable mouth out in the world down through the marked depths. Deeper = older = more wrong-scaled (geography-implies-history).

| order | site_id (sites.yml) | what it is | rough depth |
|---|---|---|---|
| 0 | `first_report_lectern_01` | the first notice (Movement I), found near the mouth | surface |
| 1 | `rune_rosetta` | the literacy gate — the founding hand, the whole script | upper Hold |
| 2 | `stone_of_reckoning` | the digit-glyph + sign-mark Rosetta (coords depend on it) | upper Hold |
| 3 | `stone_vaun` `stone_mara` `stone_sella` `stone_orin` `stone_brann` `stone_iss` | the six keeper-stones, scattered through the Warrens/Market/Lamp-works | mid Hold |
| 4 | `kept_light_home_01` `offering_cairn_01` `bow_marker_01` | the living custom anchors (a lit hearth, a cairn, a bow-stone) | mid Hold |
| 5 | `the_threshold` | Orin's sealed lintel (the crouch-to-pass) | mid-lower |
| 6 | `the_far_water` | Sella's shore pool (the surface shown wrong in reflection) | a far gallery |
| 7 | `the_cold_hearth` | the dead shrine — Iss's grave + the effaced seventh | lower, west |
| 8 | `unbroken_light` | **the Undercroft / Accepting floor** — the one fire that never went out; the climax site | the deep line |

## per-site build spec

### keeper-stones (×6) — `stone_vaun` … `stone_iss`
- **Form:** a single floor-set or low-canted slab (deepslate/polished blackstone), ~3×4 blocks, **angled or low enough that the camera must tilt down to read it** (the bow built in). One soul-lantern at a fixed offset so it's *legible only by stooping into its light*.
- **The carving:** a sign / hanging-sign / text-display in `observance:runes` carrying that keeper's cipher (the bound plaintext from `clue-specs.ts` — NEVER edit the plaintext; the X1 guard round-trips it). Each keeper's *hand* differs per `arc/corpus/cipher-plaintexts.md`: Vaun hammered-square + an empty second column; Mara small-even; Sella mirror-wrong child-scrawl; Orin cold-perfect breaking off at "i —"; Brann legible only at night (place under a gutter-able torch); Iss too-smooth, frictionless.
- **Answer surface:** the stone IS a `keeper_stone` answer-site (sites.yml) — a sign within radius is the in-world answer verb. No extra build.
- **Protection:** `protect: true` — `BeatProtectionListener` restores the carving if broken.

### the two Rosettas — `rune_rosetta`, `stone_of_reckoning`
- `rune_rosetta`: the founding ring — glyph↔letter for the whole alphabet, read **sunwise** (Bow-first). A circular dais; the ring of runes on the rim; the center empty. This teaches the script the whole game is read in.
- `stone_of_reckoning`: the digit-glyphs + the sign-marks (N/S/E/down). **Every coordinate clue depends on it** — keep coord-bearing rows inactive until it is placed.

### the custom anchors — `kept_light_home_01`, `offering_cairn_01`, `bow_marker_01`
- `kept_light_home_01`: a hearth scan-zone (radius 12) — one fire must stay lit. A built hearth with a soul-fire; the Kept-Light sampler checks light here.
- `offering_cairn_01`: a small cairn at a shaft-mouth — first-ore is dropped here (the Offering).
- `bow_marker_01`: a bow-stone (Orin's deep grooves) — crouch within radius = honored.

### `the_threshold` (Orin's lintel) / `the_far_water` (Sella's shore) / `the_cold_hearth` (the dead shrine)
- `the_threshold`: a low stone lintel forcing a crouch to pass; Orin's broken "i —" carved on the underside (legible only stooped).
- `the_far_water`: a still pool in a far gallery; the ceiling above it built to read, in reflection, as an open healed sky (the surface's lie). Sella's bearing leads here.
- `the_cold_hearth`: a doused hearth, a grave slab (Iss), and a second effaced marker (the seventh) — "nothing is kept here." Cold palette, no light.

### `unbroken_light` — the Undercroft / Accepting floor (the climax)
- The one fire that never went out, centered. A room sized for **6–8 players to gather and bow together** (the Accepting quorum). The lectern-comparator **door** that `undercroft-descent` opens (a `DoorOpenBeat`/`SmallStructureBeat` target). The **pressure-glyph rune walked on the floor** (`pressure-glyph-walk`). Type is `accepting_floor` so `AcceptingRiteListener` watches it for the synchronized group bow.
- **Fog:** the Undercroft is a **datapack** dimension/biome (thick fog, `ambient_light: 0`) — NOT the resource pack (Java fog isn't pack-driven). See `design/atmosphere-stack.md §3.5`.

## paste-pipeline note (already wired — no build needed)
- `SmallStructureBeat` + `SchematicPaster` paste a `.schem` at a site, reveal-disciplined (it appears unwitnessed). Drop hero `.schem` files where the engine expects them; the beats reference them by name.
- Small/curated set-dressing can also be done with `FakeBlockBeat` (client-side) where a real build isn't worth a schematic.

## GO-LIVE (the manual builds)
1. Build each site in-game per the spec above (deepslate/blackstone palette, stoop-to-read stones, the Undercroft gather-room).
2. Export each as a `.schem`; place the files; **fill the real x/y/z into `sites.yml`** (until then the plugin silently skips the unplaced site — no errors).
3. Set up the Undercroft fog **datapack** (dimension_type effects) + Multiverse world.
4. Carve the rune signs/lecterns in the `observance:runes` font (resource pack must be live first).
