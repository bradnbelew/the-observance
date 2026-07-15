# 01 — Deep Hold: Physical Layout Rebuild

**Goal:** replace the current Hold's *room program* — what rooms exist, their shapes, how they
connect, and how they're furnished — so every space reads as a **real place** a person worked in, not a
generated dungeon with a station in it. Keep the safe build machinery (README invariants); redesign
what it builds.

---

## 1. The problem, measured (so we don't hand-wave it)

The current Hold is defined in `plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Plan.java`
as **32 rooms** on a compacted coordinate system, built by
`plugin/.../structure/DeepHoldV4Geometry.java`.

Measured from that data:
- **32 rooms, ~32,500 m² of floor, averaging ~1,015 m² per room** (roughly a 32×32 hall each).
- **76 fixtures total** — so an evidence room is typically a ~21×21 hall holding **one** ~5×5 furniture
  cluster. **~5% of floor area is content.** The rest is procedurally-textured empty floor.
- `archive_nave` is a **36×99 corridor-hall with zero fixtures**; `keeper_nave` is ~1,584 m² with zero
  fixtures. These are the worst offenders — huge transit voids.
- The dressing (`dressNaturalDistricts`, `dressEvidenceRoom`, `buildRoomShell`) is generic:
  floor inlays, a bookshelf column every 14 blocks, sparse pillar lamps. It never makes a room *be* a
  library or a market or a camp; it makes a themed empty box.

Critic verdict (four independent reviewers agreed): "generated-dungeon-with-stations, not a school /
market / camp — this is the muddiness you feel." That is the thing we are fixing.

## 2. The target: rooms that ARE their fiction

A room's job is to be a believable place whose **furniture is the evidence.** Concretely, per the case
each room serves:

| Fiction | What the room must physically be | Where the puzzle lives |
|---|---|---|
| Library (Mara editions, shelf/line/word) | Rows of **real shelves** with aisles you walk, reading desks, a card catalog, a librarian's counter and back office. | The "3-6-2" address works because there ARE shelves to count and lines to read. |
| Keeper archive (ledgers, affidavits) | Ranks of labeled filing barrels/cabinets, a central index desk, sorting tables, a sealed records vault. | Cross-referencing ledgers across ranks; the forged entry sits among real ones. |
| Service works (lamps, cistern valves) | Workbenches, a lamp bench with wick trays, a tool wall, a furnace/kiln line, visible pipe runs and valves. | The residue chronology reads off a real wick bench; valves are real fixtures on a real pipe. |
| Camp (prior expedition, ASH-13) | Bedrolls in a tent line, a firepit, a supply cache, a latrine trench, a lookout post, personal kit. | Triangulating the camp from three units works because the units are physically laid out. |
| Offices (Iss, Wren, dispatch) | A desk, a chair, a nameplate door, a filing cabinet, personal effects, a window or vent. | Personal records and the "who sat here" deductions come from a real occupied office. |
| Cistern / water rooms | A real basin with contained water (see doc 4 — water must be curbed), reed beds, sample shelves, a valve roster board. | Reflection/overlay reads off a real still surface; samples sit in a real cold store. |

**Density target: 40–70% of each room's floor footprint is furniture/structure the player moves
through and around** — not 5%. Aisles and standing cells stay clear (traversability), but the room is
*full* of believable stuff. A library aisle is 1–2 blocks wide between 1-block-deep shelf runs, not a
21×21 void with one lectern in the middle.

**Scale:** the Hold stays large — a believable multi-district underground complex — but "large" is
*many human-scale rooms connected*, not *a few oversized empty boxes*. Prefer more, smaller, denser
rooms over fewer huge ones. Corridors are hallways with headroom and doors, not 99-block transit
tunnels.

## 3. The method, in code (this is the anti-drift recipe)

The existing code already separates a **data layer** (the plan) from a **builder** (the geometry). Use
that separation. Do the design in the data layer first, prove it, then build densely.

### Step A — Redesign the floorplan in the data layer, FIRST
File: `DeepHoldV4Plan.java`. It holds `ROOMS`, `FIXTURES`, `GATES`, `LINKS`, `RECORD_STATIONS` as
records, plus `validate()`.

1. Draw the whole floorplan on one coordinate system (paper/grid/spreadsheet) before writing any Java:
   every district, room, corridor, door, and major furniture run, with exact bounds. This is Brad's
   rule 5 — *do not design rooms and fillings separately.*
2. Encode it as the new `ROOMS`/`FIXTURES`/`GATES`/`LINKS` records. Give each room a `role`/type tag
   that the builder will switch on for dressing (e.g. `"library"`, `"archive_rank"`, `"service_bench"`,
   `"camp"`, `"office"`, `"cistern"`).
3. **Extend `validate()` to prove the floorplan is legal before a single block is placed:**
   - No two rooms' *owned* volumes overlap (there is already an `overlapsOwnership` check — tighten it
     for the new program).
   - Every fixture's anchor AND its player standing cell are inside their owning room, clear of walls.
   - Every furniture run you author (shelf rows, barrel ranks, benches) is inside its room and does not
     cross an aisle or a door — add explicit "prop cell" reservations so the builder can't place a prop
     where a walk path or another prop goes.
   - The room graph (`LINKS`) is fully connected from `orientation` (the entry), and every gate sits on
     a real doorway between two linked rooms.
   `validate()` returning a non-empty list must hard-block the build (it already does — keep that).

### Step B — Build densely, per room type
File: `DeepHoldV4Geometry.java`. Today `dressRoom(room)` switches on room id and mostly calls thin
generic dressers. Replace those with **dense, fiction-specific routines**, one per room *type*:

- `dressLibrary(room)` — lay real shelf runs (1-deep `BOOKSHELF`/`CHISELED_BOOKSHELF` walls) in rows
  with 1–2 block aisles; place reading desks (lectern + stairs-as-chair + a lamp); a card-catalog block
  bank; a librarian's counter; a back office with a door. The shelf rows are numbered by their physical
  position so "shelf 3" is countable.
- `dressArchiveRank(room)` — barrel/cabinet ranks with wall signs labeling each rank; a central index
  lectern; sorting tables (slabs at working height); a small sealed vault alcove.
- `dressServiceBench(room)` — workbenches (slab tops on supports), a wick/lamp bench with tray recesses,
  a tool wall (item frames holding tools — *decorative*, see doc 4 on not overloading frame mechanics),
  a furnace/kiln line, a visible pipe run with valve fixtures.
- `dressCamp(room)` — a tent line (wool/leaf roofs on frames) over bedrolls, a firepit (contained), a
  supply cache (barrels), a latrine trench, a lookout post, scattered personal kit.
- `dressOffice(room)` — desk (slab + supports), chair (stairs), nameplate door, filing cabinet
  (barrels/chest), a window or vent, personal effects.

Each routine must:
- Only place props inside the room's clear interior (respect the reservations from Step A).
- Leave aisles and every fixture standing cell walkable (1-wide minimum, 2-high headroom).
- Be deterministic (same input → same output; no randomness that could land a prop in an aisle).

### Step C — Keep the standability + reachability guards
The builder already asserts specific landing/standing cells are standable at build time
(`assertCompactStairLandings`, the per-fixture `approachCells`). **Extend these assertions to the new
layout**: every room's entrance cell, every aisle spine, and every fixture standing cell must pass a
`assertStandable`. And run the **offline reachability sim** (doc 4) which BFS-walks from the Mouth and
proves every room + fixture + record station is reachable and nothing escapes the envelope. Never
publish a readiness receipt without both.

## 4. Traversability & no-escape rules (Brad's rule 5, made mechanical)

Bake these into `validate()` and the offline sim so a violation fails the build, not the playtest:

- **Doors, not gaps.** Room-to-room transitions are doorways (2-high, ≥1-wide, on a linked pair). The
  gates already do this for main-sequence doors; ordinary connections need it too.
- **No 2-block jumps to anything required.** Any vertical change on a required path is a stair or ramp
  (≤1 block rise per step). Fixture standing cells are on the room floor, reachable on foot.
- **Players can't escape or skip.** The Hold is Adventure-mode and region-guarded
  (`HoldProtectionListener`). The sim must confirm no walk path leaves the authored envelope and no
  fixture is reachable *before* its gate opens (gate order is real).
- **All key components visible & functional.** Every book is in its exact lectern/shelf facing the
  right way; every named item is in its exact container/slot; every sign faces its reader; every
  fixture the fiction references physically exists (rule 4).

## 5. Worked example — the Keeper Library district (a fidelity template)

This shows the level of concreteness a room needs. Replicate this *method* for every district; the
exact numbers are illustrative (fit them to your final floorplan grid).

**Fiction:** the Keeper archive's reading library. Case C03 (Mara: editions) and the LS04/KM02
shelf-line-word investigations live here.

**Floorplan (local cells, one room, human scale ~ 24 wide × 18 deep × 6 high):**
```
  +--------------------------------------------------+   z (deep)
  | counter |  aisle  | shelf run A (numbered 1..6)  |
  |  desk   |         |-----------------------------|
  | (index) |  aisle  | shelf run B (numbered 1..6)  |
  |         |         |-----------------------------|
  | catalog |  aisle  | shelf run C (numbered 1..6)  |
  |  bank   |         |                             |
  +----[door to back office]----+  [door to nave]----+
  x (wide) ->
```
- **Shelf runs A/B/C**: each a 1-deep `CHISELED_BOOKSHELF` wall, 6 bays long, numbered by physical
  position (a small wall sign "3" at the third bay). Aisles between runs are 2 wide and walkable — this
  is where the player stands to count "shelf 3, line 6, word 2."
- **Reading desks**: at the end of two aisles, a lectern (the readable edition) + a stair "chair" + a
  lamp. The Mara editions (`mara_lectern_1..5`) become these desks, spread along the aisles, not a row
  of five lecterns in a void.
- **Card catalog bank**: a block of `BARREL`s in a labeled grid — an in-fiction index the player can
  actually consult (holds ordinary Minecraft item labels, not puzzle answers).
- **Librarian's counter + back office**: a desk with the librarian's own ledger (a book), a nameplate
  door, personal effects. The "who edited which edition" deduction has a physical origin.
- **The investigation, arising naturally:** the books on the desks reference "the third edition, shelf
  three" — and there physically IS a shelf three with a third book whose line and word are the address.
  The cipher/address is *earned by reading and counting a real library*, not printed on a card
  (doc 2 on layered difficulty).

**Verification for this district:**
1. `validate()`: room bounds legal, no overlap with the nave/adjacent keeper rooms, every desk/shelf
   standing cell inside the room and clear.
2. Offline sim: from the Mouth, the library is reachable; every desk + the counter + the catalog have a
   reachable standing cell; no aisle is walled; no escape.
3. Live: `obs placehold build` → `obs placehold audit` (76→new fixture count all valid, gates pass) →
   walk it in spectator, then a **non-op Adventure** account → restart → re-audit.

## 6. Deliverable per district

For each of the ~6–10 districts you design, the "done" bar is:
1. Floorplan committed in `DeepHoldV4Plan.java` records + `validate()` green (non-overlap, standing
   frames legal, graph connected).
2. A dense per-type dressing routine in `DeepHoldV4Geometry.java` that fills it to 40–70% and leaves all
   aisles/standing cells walkable.
3. Offline reachability sim green for that district.
4. A live cutover build that reaches a **complete readiness receipt**, then survives a **restart +
   re-audit**.
5. No contradiction: every book/sign/item the district's cases reference physically exists, correctly
   placed and oriented (cross-check against docs 2 and 3).

Do **one district end-to-end** (design → build → verify → commit) before starting the next. Do not
design all of them on paper and build none — that is how the last version drifted into empty halls.
