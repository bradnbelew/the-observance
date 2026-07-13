# structures - the Deep Hold world-build spec

The build instructions for every load-bearing site. **Current implementation note (2026-07-13):** the Deep Hold itself is no longer a manual placeholder. `/observance placehold build` generates the protected V4 Hold, its 32-room linear/retraceable route, eight physical gates, books, signs, filing surfaces, and proof metadata. The isolated Paper 1.21.11 rehearsal exercised every gate open/reseal path, restart persistence, rebuild idempotence, collision refusal, route/door/fixture audits, and the return path to the one village-well entrance. Outside-Hold sites remain deliberately manual and must receive real `sites.yml` coordinates plus launch receipts.

The optional paste pipeline is also wired (`FaweSchematicPaster` to `SchematicPaster` to `SmallStructureBeat`, isolated so a missing FAWE degrades to "no paste" instead of a crash). This document remains the visual and placement authority for those outside sites.

Canon geometry is sealed: the Undercroft is the bottom of the Hold; the Nether deep-fire is the older source from which the one kept fire was carried upward. The Nether is never described or built as a second bottom.

This document is the launch build spec. It is allowed to overrule older playthrough/archive docs when those docs collapse the ARG into six keeper slabs and answer signs.

## Theme Law

Carved stone, not cobble. Use deepslate, tuff, polished basalt, blackstone, oxidized copper, and soul-lantern light. Nothing modern, bright, or default-village cozy unless it is intentionally false.

Runes may appear on signs, lecterns, or text displays in the `observance:runes` font, but text is never the whole design. Major structures are playable investigation spaces, not clue billboards.

## Structure Quality Law

No major structure is launch-ready if its only clue surface is a sign, hanging sign, text display, or answer input.

Every major site should have at least two non-sign clue surfaces where the fiction and build scale allow it. Use architecture, inventory, item frames, lecterns/books, block counts, sound, light, water/reflection, entity behavior, pathing, redstone/comparator state, map art, time condition, co-op action, altered terrain, or deliberate absence/mismatch.

Every launch structure must be audited for:

- clear entrance and clear exit;
- enough room for the expected player group;
- no accidental softlock;
- readable lighting;
- text/book readability;
- protected or duplicated critical interactions;
- fallback when a critical block, item, book, or entity is broken;
- one memorable focal object or silhouette;
- one invited action beyond reading;
- one traversal vector that points onward or pays off a previous clue;
- consistency with `design/CLUE-LEDGER.md`;
- consistency with NPC, book, website, media, and Discord claims.

The answer surface, when one exists, is only the receipt or local mechanism. It cannot be the design. A sign can confirm, accept, or echo an answer, but the structure itself must do investigative work before that moment.

## The Descent

Normal server spawn has no Observance structures. The Hold is found by descending from a discoverable mouth out in the world down through marked depths. Deeper means older and more wrong-scaled.

| order | site_id | what it is | rough depth |
| --- | --- | --- | --- |
| 0 | `first_report_lectern_01` | first field notice near the mouth | surface |
| 1 | `rune_rosetta` | script literacy surface | upper Hold |
| 2 | `stone_of_reckoning` | digit/sign/place literacy surface | upper Hold |
| 3 | `stone_vaun`, `stone_mara`, `stone_sella`, `stone_orin`, `stone_brann`, `stone_iss` | keeper investigation clusters | mid Hold |
| 4 | `kept_light_home_01`, `offering_cairn_01`, `bow_marker_01` | living custom anchors | mid Hold |
| 5 | `the_threshold` | Orin's sealed lintel | mid-lower |
| 6 | `the_far_water` | Sella's shore pool | far gallery |
| 7 | `the_cold_hearth` | Iss, false comfort, effaced seventh | lower west |
| 8 | `unbroken_light` | Undercroft / Accepting floor | the deep line |

## Per-Site Build Spec

### Keeper Investigations, Not Six Identical Stones

The six `stone_*` sites remain runtime anchors and can still be protected answer/receipt sites, but the launch build must not present them as six identical cipher stones. Each keeper site is an investigation cluster with a different player behavior, at least two non-sign clue surfaces, and a traversal vector recorded in `design/CLUE-LEDGER.md`.

The old slab can survive only as a supporting object inside the cluster. It cannot be the whole keeper experience.

### vaun - Audit Of What Was Kept

Runtime anchors: `stone_vaun`, `vaun_hoard_chest`, `vaun_bookshelf`.

- **Form:** cramped market/hoard room, one side over-counted, one side visibly empty.
- **Non-sign clue surfaces:** chest inventory, empty slots, mismatched ledgers/books, item frames showing taken/returned categories.
- **Player behavior:** audit the room like an accountant. Count first things, compare what was kept against what was returned.
- **Traversal vector:** points back to Offering practice and forward to first-of-deep return.
- **Failure to avoid:** a Caesar stone that simply says Vaun's answer.

### mara - Editions That Disagree

Runtime anchors: `stone_mara`, `mara_lectern_1` ... `mara_lectern_5`, `mara_map_marker`.

- **Form:** reading room split by route marks; shelves imply several copied editions of the same account.
- **Non-sign clue surfaces:** lectern/book differences, route marks, map marker, page order.
- **Player behavior:** compare editions, decide which copy was walked and which was only read, then physically follow the route.
- **Traversal vector:** book memory and route action; a phrase can matter later because players actually used it.
- **Failure to avoid:** page-line-word extraction with no reason to trust the chosen edition.

### sella - The Count That Water Refuses

Runtime anchors: `stone_sella`, `sella_pool`, `sella_anchor`, `school_stand`, `cistern_7`, `the_far_water`.

- **Form:** water-side school/cistern path where the dry count says six and the reflected count implies seven.
- **Non-sign clue surfaces:** water/reflection, block counts, school records, child-height marks, shoreline geometry.
- **Player behavior:** compare land count, reflection count, and school evidence before typing anything.
- **Traversal vector:** far water, reeds, and later media confirmation.
- **Failure to avoid:** Atbash as the main experience.

### orin - Posture And Sealed Sightlines

Runtime anchors: `stone_orin`, `orin_marker_1` ... `orin_marker_6`, `orin_frame_dial_1` ... `orin_frame_dial_6`, `the_threshold`.

- **Form:** low lintels, crouch sightlines, frame dials, and route marks that read differently by posture.
- **Non-sign clue surfaces:** architecture, player stance, item-frame orientation, sealed lintel, underside marks.
- **Player behavior:** bow, crouch, align, rotate, and inspect from intended positions.
- **Traversal vector:** teaches physical answer grammar for the later Accepting act.
- **Failure to avoid:** a cold-perfect carving with no body mechanic.

### brann - Dark Hours And Listening

Runtime anchors: `stone_brann`, `brann_toll_tower`, `brann_corridor_start`, `brann_corridor_end`, `watch_floor`.

- **Form:** watch tower/corridor that changes meaning by time, light, and silence.
- **Non-sign clue surfaces:** sound pattern, toll count, light state, watch logs, night-visible focal fire.
- **Player behavior:** stay awake, listen, count, and return under the correct condition.
- **Traversal vector:** media clip 3 confirms the instruction instead of inventing it.
- **Failure to avoid:** rail-fence cipher as the whole solve.

### iss - Comfort That Does Not Match The Land

Runtime anchors: `stone_iss`, `the_cold_hearth`, `warm_town_collapse`, `keeper_altar`.

- **Form:** a shrine or town fragment that is too warm, too clean, and physically contradicted by surrounding evidence.
- **Non-sign clue surfaces:** false grave layout, mismatched records, warm palette collapse, item/NBT or inventory falsification, Wren contrast.
- **Player behavior:** compare Iss's claims against land, records, media, and companion behavior.
- **Traversal vector:** teaches players how to catch forged comfort before the late Wren/Seventh reveal.
- **Failure to avoid:** another Vigenere/acrostic unless the cipher is explicitly the forgery being caught.

### The Two Rosettas - `rune_rosetta`, `stone_of_reckoning`

- `rune_rosetta`: a founding ring, glyph to letter for the script. It should be a physical literacy surface with a center gap or missing witness space, not a flat alphabet chart.
- `stone_of_reckoning`: digit glyphs and sign marks for place grammar. Coordinate-bearing rows should not be treated as fair until this is placed and readable.

### Custom Anchors - `kept_light_home_01`, `offering_cairn_01`, `bow_marker_01`

- `kept_light_home_01`: hearth scan zone. The build should show a lived practice: oil storage, cold spare lamp, door placement, and one practical reason the light matters.
- `offering_cairn_01`: shaft-mouth cairn. It should support the first-thing logic with missing/returned item evidence, not just accept a drop.
- `bow_marker_01`: bow marker. It should teach posture through stone grooves, low sightlines, or route access.

Each custom anchor needs folk/practical/physical/consequence/false/late-use representation in `design/CLUE-LEDGER.md` or related content docs before launch.

### `the_threshold`, `the_far_water`, `the_cold_hearth`

- `the_threshold`: low lintel forcing a crouch to pass. Underside marks should only be legible from the bowed posture.
- `the_far_water`: still pool in a far gallery. Reflection should reveal a wrong count or route truth before any text confirms it.
- `the_cold_hearth`: doused hearth, false grave language, Iss evidence, and second effaced marker. It should feel like a place caught lying, not a final lore plaque.

### `unbroken_light` - Undercroft / Accepting Floor

The one fire that never went out, centered. The room must fit 6-8 players and support the synchronized group bow watched by `AcceptingRiteListener`.

Required clue surfaces:

- central kept light;
- pressure-glyph floor walk;
- lectern/comparator or equivalent local mechanism;
- enough space for co-op body action;
- visible exit or release response after success.

Fog belongs to the datapack dimension/biome stack, not the resource pack.

## Paste Pipeline Note

`SmallStructureBeat` and `SchematicPaster` can paste `.schem` files at sites reveal-disciplined. Small set dressing can use `FakeBlockBeat` where a full schematic is not worth it.

The pipeline functioning does not prove launch structure quality. Static checks prove only that a thing can be placed; the build still needs visual proof and real Minecraft client inspection.

## Go-Live Manual Builds

1. Build each site in-game per this spec.
2. Export each schematic where needed.
3. Fill real x/y/z into `sites.yml`.
4. Set up the Undercroft fog datapack and Multiverse world.
5. Carve or place rune text only after the resource pack is verified.
6. Run `powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_world_build_readiness.ps1 -Launch`.
7. Run `powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_structure_quality.ps1 -Launch`.

## Visual Rescue Gate

`design/VISUAL-RESCUE.md` is part of the build spec. A site is not launch-ready just because it is placed, protected, and covered by `/obs coverage`.

Before a site moves from rehearsal into the real world:

1. Visit it in-game.
2. Judge its approach silhouette, palette, focal object, lighting, player movement, action-answer legibility, and coherence with the clue ledger.
3. Mark it `KEEP`, `RESHAPE`, `REPLACE`, or `CUT`.
4. Only `KEEP` sites advance into live placement.

This is the guard against small test fixtures, answer slabs, or signboards becoming the final ARG.
