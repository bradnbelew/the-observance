# The Deep Hold Room Bible

> **SUPERSEDED V4 ARCHIVE — DO NOT IMPLEMENT OR BUILD FROM THIS FILE.** Current spatial/content authority is indexed by `design/V5-SUPERSESSION-MAP.md`.

> **SUPERSEDED V4 ARCHIVE — DO NOT IMPLEMENT OR BUILD FROM THIS FILE.** Current spatial/content authority is indexed by `design/V5-SUPERSESSION-MAP.md`.

> **SUPERSEDED V4 CONTENT MAP.** Geometry remains in the V4 room-box CSV; current case/room ownership is `design/ARG-V5-ROOM-ASSIGNMENTS.csv`.

Status: rebuild specification. This document maps the dimensioned construction concept to executable
Java listeners, SQL gates, and Minecraft interaction rules. It is not evidence that the current
generator satisfies the specification.

The exact rebuild coordinates are in `DEEP-HOLD-ROOM-BOXES.csv` and
`DEEP-HOLD-FIXTURE-MANIFEST.csv`. Those files supersede any approximate box written in the room prose.
`tools/check_deep_hold_fixture_manifest.py` proves complete 76-site coverage, exclusive room ownership,
anchor/standing-zone containment, and player-frame direction before implementation begins.

## Authority

When sources disagree, use this order:

1. Runtime listener behavior and actual block interaction requirements.
2. SQL `requires_flags`, accepted answers, and outcome flags.
3. Site types and puzzle keys loaded by the plugin.
4. This room bible and the construction blueprint.
5. Older descriptive documents.

The current Hold generator is not a source of truth for geometry. Its audit proved that independently
placed shells and fixtures erase one another.

## Global Build Contract

- Local origin is the surface-mouth center: `X=0, Z=0, Y=MOUTH_Y`.
- Positive Z is forward through the Hold. Player yaw never rotates the plan.
- Main floor is `MOUTH_Y - 24`; Keeper Court and lower works are `MOUTH_Y - 28`; finale floor is
  `MOUTH_Y - 32`.
- Absolute lowest authored foundation block is `MOUTH_Y - 36` and never below `world.minY + 8`.
- Entrance stair drops 24 blocks across 64 Z blocks, one down per two forward, with full-width landings
  at Z 16, 32, 48, and 64. It is 11 clear blocks wide and at least 8 clear blocks high.
- Required player routes use stairs or gentle ramps only. No ladder, jump, pearl, crawl, building, or
  block-breaking is required.
- Main corridors are 11 clear blocks wide and 8 clear blocks high. Secondary room aisles are at least
  5 wide. Side-story loops are at least 4 wide.
- Structural walls are 3 blocks thick. Floors/foundations are 2 blocks thick. Roofs are 3 blocks thick.
  A 5-block stone envelope surrounds the complete exterior.
- Every room is closed to sky/caves. Every sealed gate reaches both side walls, foundation, and roof.
- No two room ownership boxes overlap. A room builder may mutate only inside its declared box.
- Fixtures are placed after the room shell, but each room is cleared only once. A fixture never clears,
  floors, shells, roofs, or decorates outside its assigned module.
- Every fixture has an explicit facing, approach side, and at least a 3x3 player-standing clearance.
- The finale-to-entry return corridor is 9 clear blocks wide at X +128, has no intermediate openings,
  and unlocks only after the terminal rite. It can never be used to enter a later district early.

## Interaction Orientation Contract

### Player-frame rule

Every room declares a primary player approach and every fixture declares one or more standing zones. A
fixture is not oriented by a hardcoded compass direction; its facing is derived from the selected standing
zone.

- Let `F` be the fixture block and `P` the center of its intended player-standing zone.
- The readable/interactive front normal must point from `F` toward `P`.
- A player standing at `P` must be looking generally toward `F`, with no solid block intersecting the
  eye-to-fixture sightline.
- For the main north-to-south spine, a fixture directly ahead of an approaching player normally faces
  north, back toward that player. West-wall fixtures face east into the room; east-wall fixtures face west.
- A fixture approached from two directions must be a deliberate island/two-sided exhibit or have two
  separate readable surfaces. Players must never be expected to read the back or side of a block.
- The primary standing zone must be reachable from the room entrance without walking through another
  fixture's standing zone, climbing decor, or entering a focused answer radius.
- Return-path readability is optional unless the room contract explicitly calls for an aftermath reading.
  Forward-route readability is mandatory.
- Fixture placement APIs must accept a `PlayerFrame`/approach descriptor and calculate block state from it.
  Room builders may not scatter hardcoded `NORTH`/`SOUTH` values without a declared standing zone.

The final audit must compute the vector from fixture to standing zone and compare it with the actual
directional/rotational block state. It must also ray-check the standing zone to the readable face and confirm
interaction reach from a non-op player's normal eye height.

### Signs

- Editable answer signs are standing signs or wall signs whose editable front faces the approach aisle.
- They are not waxed. No decorative sign shares the answer site's radius.
- The block directly in front and the two blocks above it are air.
- The focused answer-site radius is one block, so a guess cannot bind to a neighboring room.
- Submitted text is captured and blanked by `AnswerSignListener`; permanent instructional text belongs
  on a separate waxed sign outside the focused answer radius.

### Lecterns and books

- Lectern reading face points into the aisle. Minimum clear space is 3 wide by 3 deep.
- Every required lectern contains a `WRITTEN_BOOK` at build completion, with the expected title and text.
- Decorative lecterns never sit inside an answer-sign radius.
- Comparators, page locks, and book replacements remain reachable behind a sealed service wall; players
  never enter redstone/service space.

### Chiseled bookshelves

- The back touches the wall. The occupied-slot/book face points into the player aisle.
- No bookshelf is placed from material alone; block-state facing is set before books are inserted.
- Every mechanic shelf contains the exact required occupied slots after facing is applied.
- A two-block-deep service backing prevents the shelf side or back from appearing in an adjacent room.

### Containers, frames, and plates

- Chests have air above their lid and at least two approach blocks. Barrels face the aisle.
- Item frames attach to dedicated backing blocks and never share backing with another room.
- Required pressure plates are flush with the walkable floor and have a clear 5x5 convergence area.
- Per-player display entities are invisible by default and shown only to their intended player.

### Room composition from the entrance

- The entrance must reveal the room's focal object before exposing supporting clutter.
- Primary evidence sits on-axis or at the end of a deliberate sightline; secondary evidence occupies side
  alcoves and never visually competes with the primary interaction.
- Signs and lecterns are angled toward where the player naturally stops, not merely aligned to the wall grid.
- Lighting reinforces the same viewing direction: light the readable face from above/side without placing a
  lantern between player and evidence.
- Doors open away from the standing zone and cannot cover text, block containers, or push players into a
  focused answer radius.
- Room exits are visible after the intended interaction. Before that interaction, an exit may be subdued but
  never disguised as another evidence alcove.

## Access Graph

The visual concept's early Prior Expedition placement is superseded here because it creates a canonical
progression cycle. The production route is:

`Mouth -> Orientation -> Keeper Court -> Archive/Evidence Web -> Undercroft/Lower Works -> Threshold
Complex -> Prior Expedition Wing -> Accepting -> Unwriting/Release`

| Gate | Physical boundary | Initial state | Opens when | Fail behavior |
| --- | --- | --- | --- | --- |
| G1 | Mouth to Orientation | open | Always open after a successful build | Entry audit fails if blocked |
| G2 | Orientation to Keeper Court | sealed | `rosetta_known` | Preserve Orientation and fail closed |
| G3 | Keeper Court to Archive | sealed | Any canonical keeper investigation has begun after `rosetta_known` | Never require all six theories; archive evidence is needed to form them |
| G4 | Archive to Undercroft/Lower Works | sealed | `undercroft_open` | Preserve last state on DB failure |
| G5 | Lower Works to Threshold Complex | sealed | `deep_gate_open` | Fail closed; Threshold fragments and Seventh rite remain inert |
| G6 | Case Board to Prior Camp | sealed | `prior_absence_known` | Case Board remains reachable; camp stays physically hidden |
| G7 | Prior/Threshold convergence to Accepting | sealed | `prior_witness_ready` AND (`accepting_onramp_open` OR `threshold_open`) | No path, roof, or return-corridor bypass |
| G8 | Accepting to Unwriting/Release coda | sealed | `bowed_as_one` | Finale coda remains inaccessible |

Gate centerlines in the rebuild grid are G1 Z=99, G2 Z=152, G3 Z=293, G4 Z=506, G5 Z=589,
G6 west-wing Z=648, G7 Z=774, and G8 Z=875. Each seal owns a dedicated three-block-deep transverse
volume between adjacent room boxes; no room builder may write into a gate volume. G6 spans the complete
west-wing section rather than the main spine. The Dread entrance is a separate east-wing seal inside its
own room owner and is not counted as a progression bypass.

The Dread Procession is a controlled parallel wing from Lower Works that opens on `iss_caught` or
`seventh_suspected` and rejoins before G7. It does not bypass G5, G6, or G7.

## Room 00 - Surface Mouth and Descent

Purpose: sell the scale, establish that the Hold is a deliberate civic work, and provide a safe return.

- Box: X -22..22, Z 0..96. Floor transitions Y 0 to Y -24.
- Shape: monumental rectangular mouth, four straight stair flights, four landings, broad vestibule.
- Required content: mouth register, covered-copy echo, direction to Orientation, return-corridor door.
- Lighting: warm surface lamps decay to recessed cold lamps; no floor lanterns in the walk line.
- Accessibility: 11-wide stairs, side rails/walls, no head collisions, rest landings, no drops.
- Audit: sample the full centerline, both edges, every landing, roof clearance, lowest foundation, and
  the return-door one-way state.

## Room 01 - Orientation Hall

Purpose: teach reading grammar before the six keeper investigations and establish seven ways without
pretending the seventh is a normal keeper.

- Box: X -36..36, Z 102..146, floor Y -24, clear height 12.
- Shape: broad tripartite hall with three isolated alcoves.
- West alcove: `rune_rosetta`; seven marks, Rosetta lectern, editable/Discord solve context.
- Center alcove: first-record echo and intake register; reinforces that the record predates the group.
- East alcove: Customs Field Guide; all seven custom forms, with no operator terminology.
- Vestibule evidence: `undercroft_seal`, `forgotten_mouth`, `bow_marker_01`, `offering_cairn_01`, and
  `kept_light_home_01` are composed as readable exhibits, not freestanding micro-builds.
- Interaction: Rosetta solution sets `rosetta_known`; G2 opens automatically. Ordinary right-clicks can
  ignite the prologue but do not leak correctness.
- Visual rule: the grey seventh mark is offset and visibly later, never presented as a seventh keeper bay.
- Audit: Rosetta stones, grey mark, book, instructional sign, custom faces, clear standing zones, G2 seal.

## Room 02 - Keeper Court

Purpose: the principal investigation hub. All six keeper cases can be worked in any order and repeatedly
revisited as side evidence changes their meaning.

- Box: X -104..104, Z 158..286, ceremonial floor Y -28, entry/gallery Y -24/-19, clear height 22.
- Shape: large rectangular/octagonal court with three structurally isolated bays on each side.
- Circulation: 11-wide center aisle, two symmetric four-block stairs, five-wide upper gallery, two exits.
- Each bay is separated from the next by five solid blocks and has its own fixture ownership box.

### Vaun bay

- Sites: `stone_vaun`, `vaun_hoard_chest`, `vaun_bookshelf`.
- Evidence logic: offering/debt before inventory; sorted hoard opens `vaun_cache_open`; shelf tally produces
  `vaun_tally_read`; stone becomes meaningful after the tally.
- Physical play: chest sorting table first, inward-facing mechanic shelf second, keeper stone last.
- Required connection: Deep Market and Ration Table later corroborate Vaun and unlock the prior correction.
- Audit: chest opens, required items exist, shelf faces aisle and has occupied slots, stone sign editable.

### Mara bay

- Sites: `stone_mara`, `mara_lectern_1..5`, `mara_route_marker_1..4`, `mara_map_marker`.
- Evidence logic: marked book pages yield the route; the route must be walked (`mara_walked`) before Mara's
  stone is accepted as understood.
- Physical play: five lecterns in one reading arcade, never individual rooms; route begins at a distinct
  threshold and loops through four markers back to the stone.
- Accessibility: no jump course; every marker is on a full-width stair/ramp or flat path.
- Audit: five correct books/titles, marked-page behavior, four ordered markers, map marker, complete walk.

### Sella bay

- Sites: `stone_sella`, `sella_pool`, `sella_anchor`, `sella_lectern_1..5`.
- Evidence logic: reflection bearing, later overlay, then shore memorial/seventh suspicion.
- Physical play: shallow one-block reflection pool with guarded edges; dry perimeter path; five lecterns
  face inward; raised anchor reached by stairs, never parkour.
- Required connection: School, Far Water, Cistern, and the Unlit threshold corroborate the prior correction.
- Audit: water contained, reflection sightline unobstructed, anchor has standing room, books loaded/facing.

### Orin bay

- Sites: `stone_orin`, `orin_marker_1..6`, `orin_frame_dial_1..6`.
- Evidence logic: six low bow marks teach posture; item-frame rotations preserve fall order; bowing is proof,
  not payment.
- Physical play: six 3x3 bow pads on one arc; six dials on a separate inward-facing wall; stone centered
  beyond them. No marker or frame is embedded in the shell.
- Required connection: Undercroft Seal corroborates the prior correction.
- Audit: six pads, six frames with exact rotations/backings, crouch reachability, editable stone sign.

### Brann bay

- Sites: `stone_brann`, `brann_toll_tower`, `brann_corridor_start`, `brann_corridor_end`.
- Evidence logic: hear the toll and keep the watch through the corridor; duration matters more than alarm.
- Physical play: roofed linear corridor inside the bay, acoustically isolated, no competing sounds or exits.
- Required connection: Watch Floor corroborates the prior correction.
- Audit: toll source, start/end triggers, unobstructed timed walk, sound audibility, stone sign.

### Iss bay

- Sites: `stone_iss`, `the_cold_hearth`, plus the warm/cold record surfaces authored for the catch.
- Evidence logic: apparent warmth is cross-checked against the land; the catch sets `iss_caught`, changes
  Iss's register, and exposes the bound-word/deep chain.
- Physical play: warm account and cold contradiction occupy opposite sides of one room; the player can see
  both but cannot accidentally submit at both signs from one position.
- Required connection: Warm Town Collapse and Set-Apart Shelf corroborate the prior correction.
- Audit: warm/cold distinction, focused input slots, cold hearth, changed-state proof after `iss_caught`.

G3 opens after literacy plus the first real keeper investigation signal. It must not wait for all theories.

## Room 03 - Archive Galleries and Evidence Loops

Purpose: transform six isolated keeper puzzles into an investigative web. These rooms provide independent
corroboration, contradictions, human cost, and the evidence later required by Prior Expedition.

- Central nave: X -50..50, Z 300..500, floor Y -24, clear height 14.
- Total annex span: X -164..164. Twelve individually owned evidence rooms form six west/east pairs;
  each has one entrance and one return to the archive nave.
- Archive shelves face inward. Four-block aisles and intersections eliminate maze behavior.
- Central stations: court census, intake rail, closure docket, indexed Record lecterns.

### West - Human/side-story loop

- `school_stand`: the human teaching/copy trail; Sella corroboration.
- `markers_row`: physical chronology and later hand evidence.
- `cistern_7`: seventh-count/water proof; Sella corroboration.
- `watch_floor`: dark-hours/watch proof; Brann corroboration.
- `set_apart_shelf`: separation/edited record proof; Iss corroboration.
- `the_far_water`: reflection bearing and shore copy; Sella corroboration.

### East - Place/material loop

- `deep_market`: what keeping looked like socially; Vaun corroboration and later relief callback.
- `ration_table`: counted scarcity/debt proof; Vaun corroboration.
- `third_bay_breach`: structural contradiction and route history.
- `warm_town_collapse`: warm narrative contradicted by physical ruin; Iss corroboration.
- `deep_bird_coops`: empty cages, silence, and human/NPC evidence.
- `dead_stall`: deliberately failed route, never a random rubble room.

Every exhibit gets a recognizable room shape, focal object, lore book/sign, and exit sightline. No exhibit is
only an anchor block. Discovery flags must fire from the natural player aisle.

## Room 04 - Puzzle Works and Undercroft On-Ramp

Purpose: house cross-keeper mechanics without contaminating the keeper bays, and stage the performed descent.

- Box: X -50..50, Z 512..586, floor Y -24 transitioning to Y -28, clear height 12.
- Modules: answer-sign workshop, frame/marker proof wall, cooperative plate rehearsal language, Mara route
  terminus, Lampworks stair, third lamp stand, painted line.
- `unbroken_light` descent threshold is visible at the far end but physically sealed until its authored
  descent solution sets `undercroft_open`.
- G4 opens to Lower Works on `undercroft_open`.
- No production answer text is displayed here. The workshop teaches the verb: edit, submit, sign clears.
- Audit: all answer slots focused, frames backed, plates flush, Lampworks stair walkable, painted line intact.

## Room 05 - Lower Works, Reckoning, and Threshold Complex

Purpose: convert decoded knowledge into group action, navigation, and irreversible decisions.

- Main complex: X -104..104, Z 594..770, floor Y -28, clear height 12–16.
- `stone_of_reckoning`: second Rosetta; turned line, judgment marks, record lectern, low reading posture.
- `coop_plate`: three distinct acts converge; plate area 9x9 clear, carve reachable, Discord window legible
  through diegetic response. Success sets `threshold_open`.
- `the_threshold`: true-coordinate road, destination-word sign, grave/date tableau, Keeper NPC standing zone.
- `threshold_vault`: four-person-capable convergence room; per-player rune fragments, blank editable vault
  sign, 5x5 shared speaking area. Fragments and solve remain inert until `deep_gate_open`.
- G5 seals the vault/Seventh-facing portion until `deep_gate_open`.
- The Threshold route cannot require typing a coordinate; players walk it and submit the destination word.
- Audit: Reckoning native fixture, threshold slab/grave/lectern, vault roster visibility isolation, sign,
  door-open mutation, NPC clearance, no shared-fragment leak.

## Room 06 - Prior Expedition Investigation Wing

Purpose: prove that possessing answers is not the same as witnessing the system, then force the group to
re-test every keeper conclusion against independent evidence.

- This is a west side wing at X -184..-116, Z 594..730 from the Lower Works/Archive convergence,
  not an early mandatory hallway.
- Case Board box is reachable once `undercroft_open` and all six keeper theories make `prior-absence` active.
- Case Board: roster, six solved-answer files, conspicuous blank witness column, focused sign for `no witness`.
- G6 opens only after `prior_absence_known`.
- Prior Camp: campfire, bedrolls, blank witness place, six barrels/packets, records, and six separated editable
  correction signs. Success on the camp refusal sets `prior_camp_read`.
- Correction files open in parallel and remain independently revisitable:
  - Vaun: Deep Market + Ration Table.
  - Mara: walked route + Unlit threshold.
  - Sella: School + Far Water + Cistern.
  - Orin: bow + Undercroft Seal.
  - Brann: toll + corridor + Watch Floor.
  - Iss: catch + Warm Town Collapse + Set-Apart Shelf.
- Synthesis sign at the exit accepts `witness before accepting` only after all six correction flags, setting
  `prior_witness_ready`.
- Physical rule: players can leave after reading a file, revisit the whole evidence web, and return without
  being trapped or losing access.
- Audit: every required camp prop, seven focused signs, six containers with books/items, exit and re-entry.

## Room 07 - Dread Procession

Purpose: the horror biography, not a maze or carnival corridor.

- Parallel east wing: X 120..188, Z 594..770; 11 clear wide, 8 clear high, linear path with three
  controlled niches and one return.
- Opens on `iss_caught` or `seventh_suspected`; never carries required evidence unavailable elsewhere.
- Niches: absence/glimpse, figure/pressure, aftermath/exit. No sign says `FIGURE`, `EXIT`, or gives operator
  instructions.
- Private sound/display effects target one player; group navigation remains safe and obvious.
- Exit rejoins before G7 without entering a sealed later district.
- Audit: one path, no dead ends, no bypass, no full-screen scare text, no generic named mob prop.

## Room 08 - Failed Accepting Convergence and G7

Purpose: assemble the prior witness, true walk, atonement, Unlit, side proofs, NPC errands, and keeper theories
before the group enters the real rite.

- Antechamber at X -48..48, Z 652..710 contains `failed_accepting`, witness docket,
  threshold-hands record, and the final synthesis sign.
- G7 opens only with `prior_witness_ready` and an accepting on-ramp flag.
- Wayfinding names no checklist. Missing evidence is reported only to the director, not displayed to players.
- The corridor back to the evidence web remains open until G7; no one-way drop.
- Audit: witness sign, standing room, return route, G7 floor/roof/side seal, automatic sync.

## Room 09 - The Accepting Chamber

Purpose: the terminal collective rite. The room should feel like a civic chamber built around an absence.

- Octagonal ownership box X -54..54, Z 782..858, floor Y -32, clear height 24.
- Site: `unbroken_light` / type `accepting_floor`.
- Six personal-token positions form a broad ring; the authored capped lamp is visible and changes only when
  `rite-tokens` resolves. Do not invent six switchable lanterns if the runtime reveal mutates one real cap.
- Every active/present player fits inside the site radius without crowding fixtures.
- Group bow is produced only by synchronized crouch after `tokens_laid`; the sign/oracle cannot spoof it.
- Feedback is low-information actionbar text. No chosen-one dais.
- G8 opens on `bowed_as_one` to the finale coda. The return corridor also unlocks then.
- Audit: site radius covers whole bow floor but not approach hall, token/cap mutation, quorum, sealed G8, roof.

## Room 10 - The Unwriting and Release Coda

Purpose: name what was removed, make the restore/erase choice, and allow the final Reading/release only after
the collective bow.

- Ownership box: X -54..54, Z 892..964, floor Y -32, clear height 24.

- `the_unwriting`: unfinished wrong-scaled chamber, six rail marks, cold hearth, effaced-name wall, two
  PDC-tagged restore/erase markers, and an empty deposit slot.
- The chamber is accessible only after G8 (`bowed_as_one`), intentionally imposing a cleaner physical order
  than the permissive seed: collective acceptance first, then the treatment of the Seventh.
- Solving the six-rail unwriting sets `seventh_named` and lights the real cold hearth block.
- Restore/erase markers are separated by at least six blocks, sit inside the `seventh_shrine` radius, and can
  be right-clicked by non-op players. The choice writes once.
- `threshold_vault` is not duplicated here. `keeper_altar` and the final Record/release surface occupy their
  own flanking sanctums without sharing radii.
- After both `seventh_named` and `bowed_as_one`, the Seventh Reading/name can release the Record.
- Exit: the one-way return corridor opens back to the entry vestibule; no intermediate district doors.
- Audit: rails, hearth state, choice entities/tags, deposit backing, final lecterns, release path, return lock.

## Lighting and Material Language

- Orientation: restrained warm candle/lantern light with grey seventh accent.
- Keeper Court: one accent family per bay, but all share deepslate civic construction.
- Archive: even recessed work light; evidence is readable without night vision.
- Lower Works: blackstone/sculk and fewer lights, but circulation remains safe.
- Dread: darkest legal path, light used as composition rather than navigation denial.
- Accepting: sea-lantern cross/ring is the dominant source; fixtures never cover interaction surfaces.
- Unwriting: cold hearth begins unlit; no competing warm source.

No active beacons. No lantern hangs in a sign's reading cone. No light block occupies an editable sign,
lectern, frame, container lid, entity marker, or pressure-plate cell.

## Generator Architecture Required

The rebuild must replace the current independent-site shell pass with this order:

1. Resolve safe Y levels from the mouth and world minimum.
2. Allocate immutable room boxes and assert no intersections except declared door connectors.
3. Build the geology envelope once.
4. Build floors, walls, roofs, stairs, galleries, and gate sockets from room plans.
5. Carve declared connectors once.
6. Place room-owned fixtures without clearing geometry.
7. Resolve every fixture's `PlayerFrame`; apply directional block states, then inventories/books/items.
8. Register exact focused sites and broader discovery sites.
9. Build gates into their sockets and sync flags fail-closed.
10. Run structural, fixture, orientation, inventory, accessibility, and progression audits.

Any fixture outside its room box, any overlapping room box, any authored block below the safe floor, or any
required interaction without standing clearance is a build-time failure, not an audit warning.

## Executable Gaps To Close During Rebuild

These are verified mismatches between the desired room behavior and the current runtime. They must not be
papered over with signage:

- Current Hold gate sync uses the old spatial graph. G2/G3/G6/G8 above need explicit flag policies and
  automatic sync tests; `rosetta_known` and `bowed_as_one` are not current Hold-gate selectors.
- `rune_rosetta` is type `structure`, not an answer-sign site. If the rebuilt Orientation Hall promises
  in-world submission, it needs a focused editable answer site bound to `rosetta-ring`; otherwise the
  room must clearly support Discord submission without pretending its decorative signs are inputs.
- `rite-tokens` currently resolves as a typed oracle answer and reveals one real capped lantern cell. There
  is no physical six-item token detector. The rebuild must either implement a genuine six-part offering
  interaction or present six ceremonial positions while honestly retaining the typed synthesis. It must
  not create six fake switches that do nothing.
- The desired G8 coda gate and finale-only return-corridor latch do not exist in the current eight-gate
  mapping. They require new sockets, flag sync, protection, and bypass audits.
- Choice-marker entities at `the_unwriting` must be spawned as part of production Hold construction and
  survive/recover across restart; a room shell alone does not make `SeventhChoiceListener` playable.
- Per-player Threshold Vault fragments require enough non-op players inside the actual vault radius. The
  room audit must exercise roster changes, display privacy, and the editable sign—not merely check blocks.
- Existing audit output can report a site as registered while its native fixture was erased. The new audit
  must validate each room's exact fixture signature, block states, inventory, entity tags, and approach
  clearance after all rooms have finished building.

## Acceptance Matrix

The Hold is not ready merely because all site IDs register. A successful audit requires:

- zero blocks below `world.minY + 8`;
- entry center and both edges walkable;
- all eight gate sockets complete, roofed, and bypass-proof;
- every required native fixture present;
- every required book/item/container populated;
- every directional block facing its approach;
- every fixture readable and interactable from its declared non-op standing zone;
- every primary room sightline unobstructed from the entrance or designed reveal point;
- every focused answer sign editable and isolated;
- every room and side loop reachable in the intended flag state;
- every sealed room unreachable in the wrong flag state;
- a non-op can traverse every available route and return without modifying blocks;
- no overlapping ownership boxes or fixtures embedded in walls;
- no incomplete/open roof, exposed cave, accidental maze, or unexplained dead end.
