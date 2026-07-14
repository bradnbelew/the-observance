# Deep Hold V4 - Master Plan

> **SUPERSEDED V4 ARCHIVE — DO NOT BUILD FROM THIS FILE.** Current room, fixture, gate, content, and traversal authority is indexed by `design/V5-SUPERSESSION-MAP.md`.

> **V5 ENGINEERING NOTE:** the non-overlapping room boxes, burial rules, reversible route, and standing-frame geometry remain the approved shell. All V4 story, optional-branch, gate-condition, and content language is superseded by `design/ARG-V5-MASTER-PLAN.md`, `design/ARG-V5-NODE-MANIFEST.csv`, and `arc/WORLD-BIBLE.md`.

Status: authored redesign contract; no geometry from rejected builds `0.3.25` through `0.3.28` is
approved for reuse.

This plan preserves the existing Observance ARG and replaces only the failed Deep Hold architecture,
placement model, and unsafe gate/circulation implementation. It is the authority for the V4 prototype.
It does not authorize overwriting or deleting any previously placed Hold.

## Fixed decisions

- The Deep Hold has one public entrance: the authored Surface Mouth.
- The same Mouth and main stair are the public exit at every stage of play.
- Players may turn around, backtrack through every opened gate, leave, and resume later.
- There is no player-facing return lift, second entrance, one-way drop, or finale exit shaft.
- Service and redstone space is inaccessible infrastructure, never an alternate player route.
- The Unlit is not inside the Deep Hold. Its entrance remains the well in the surface village.
- NPC dialogue that locates the Unlit in the village well remains canon.
- The Hold may depend on Unlit completion flags, but no physical Hold-to-Unlit passage may exist.
- The Hold is a consolidated home for the Keeper field and other compatible ARG structures.
- The Hold must remain completely playable in Adventure mode without breaking or placing ordinary blocks.
- Existing Hold placements are preserved. Every prototype and production candidate uses a fresh surveyed site.

## Experience shape

The route is a folded, descending civic procession. It is linear at district gates and exploratory inside
each unlocked district.

```text
Surface Mouth
  <-> Grand Stair / Orientation
  <-> G1 Rosetta Gate
  <-> Keeper Court and six investigations
  <-> G2 First-Investigation Gate
  <-> Civic Archive and evidence loops
  <-> G3 Undercroft Gate
  <-> Puzzle Works / Reckoning
  <-> G4 Deep Gate
  <-> Threshold, Prior Expedition, and Dread evidence
  <-> G5 Accepting Gate
  <-> Accepting
  <-> G6 Coda Gate
  <-> Unwriting / Release
```

Every arrow is reversible after its gate has opened. Optional branches rejoin their owning district. An
optional branch may end at an authored focal chamber only when the route back is obvious and unobstructed.

## Scale and burial

The Hold must feel grand without becoming an empty procedural cavern.

- Maximum authored footprint: approximately 200 blocks east-west by 380 blocks north-south.
- Three stacked player strata fold the route into that footprint.
- Upper Hold nominal floor: local `Y=-40`.
- Civic Hold nominal floor: local `Y=-68`.
- Lower Hold nominal floor: local `Y=-96`.
- Absolute authored foundation limit: local `Y=-102`.
- Main corridors: 9-11 clear blocks wide and at least 8 clear blocks high.
- Secondary civic streets: 5-7 clear blocks wide and at least 7 clear blocks high.
- Puzzle approach aisles: at least 5 clear blocks wide.
- Standard rooms: 8-14 clear blocks high.
- Keeper Court and Accepting: 18-20 clear blocks high.
- Structural walls: at least 3 blocks thick at room boundaries.
- Floors/foundations: at least 3 blocks thick.
- Roofs: at least 3 blocks thick.
- Geology/service envelope: at least 5 blocks beyond every authored shell.

Only the Surface Mouth may touch daylight. Before building, the placement survey must prove all of the
following:

1. Every non-Mouth roof block lies at least 12 blocks below the lowest sampled natural surface above it.
2. Every foundation block lies at least 12 blocks above the dimension minimum Y.
3. No authored shell intersects an existing protected structure, prior Hold, village well, or Unlit site.
4. The full geology envelope fits inside loaded/buildable world bounds.
5. The Mouth-to-Upper-Hold stair can descend with landings and full headroom without exposing a room roof.

If the site cannot satisfy both the surface-cover and bottom-buffer rules, preflight rejects the location and
places nothing.

## Draft coordinate frame for the approved prototype

The exact block manifests will be versioned separately after spatial approval. These boxes establish the
V4 scale and route; they are not amendments to the rejected V3 CSV files.

| Module | Local X | Local Y | Local Z | Purpose |
| --- | ---: | ---: | ---: | --- |
| Surface Mouth and stair | `-18..18` | `-44..12` | `0..104` | One ingress/egress and 40-block ceremonial descent |
| Orientation Hall | `-40..40` | `-44..-20` | `106..154` | Rosetta, first record, customs literacy |
| G1 Rosetta Gate | `-12..12` | `-44..-20` | `155..159` | First persistent progression gate |
| Keeper Court | `-92..92` | `-44..-16` | `160..248` | Six distinct investigation bays and central court |
| G2 Investigation Gate | `-12..12` | `-44..-20` | `249..253` | Opens archive access after a real Keeper start |
| Civic descent | `-26..26` | `-72..-20` | `254..296` | Grand switchback to Civic Hold; reversible |
| Civic Archive district | `-96..96` | `-72..-50` | `100..248` | Archive nave and two reconnecting evidence streets |
| Puzzle Works | `-62..62` | `-72..-50` | `40..96` | Cross-Keeper mechanics and Undercroft on-ramp |
| G3 Undercroft Gate | `-12..12` | `-72..-50` | `34..39` | Opens from canonical `undercroft_open` |
| Lower descent | `-26..26` | `-100..-50` | `0..33` | Reversible stair to Lower Hold |
| Reckoning / Lower Works | `-64..64` | `-100..-76` | `40..108` | Second literacy, co-op and deep preparation |
| G4 Deep Gate | `-12..12` | `-100..-76` | `109..113` | Opens from canonical `deep_gate_open` chain |
| Threshold district | `-96..96` | `-100..-76` | `114..220` | Threshold, Case Board, Prior and Dread branches |
| G5 Accepting Gate | `-14..14` | `-100..-74` | `221..225` | Evidence convergence; persistent once open |
| Accepting | `-56..56` | `-100..-74` | `226..292` | Group rite around Unbroken Light |
| G6 Coda Gate | `-14..14` | `-100..-74` | `293..297` | Opens from `bowed_as_one` |
| Unwriting / Release | `-70..70` | `-100..-74` | `298..374` | Seventh treatment, final reading, release |

Because the route folds between strata, upper and lower boxes may share X/Z but must never share Y. A
minimum three-block untouched structural buffer separates stacked ownership boxes.

## District contracts

### Surface Mouth and Grand Stair

- The only surface intervention.
- Clearly visible entrance silhouette without exposing underground roofs.
- Forty blocks of descent over four flights with full-width rest landings.
- Eleven clear blocks wide; no head collisions or side falls.
- Warm surface light decays into cold recessed light.
- The forward sightline terminates on Orientation, not a blank wall.
- The complete stair remains traversable in both directions for the entire ARG.

### Orientation Hall

- Contains the Rune Rosetta, first-record echo, Customs literacy, grey seventh displacement, Bow marker,
  Offering cairn, and Kept Light example.
- The first gate is visibly present from the room entrance.
- Solving either canonical literacy route sets `rosetta_known` and opens G1.
- No decorative sign may look like an answer input.
- Players can leave back up the stair before or after solving.

### Keeper Court

- One monumental shared court with six architecturally distinct bays.
- Vaun: audit and sort.
- Mara: compare editions and walk a route.
- Sella: reflect and count.
- Orin: crouch, align, and rotate.
- Brann: wait, listen, and move in silence.
- Iss: compare evidence and catch a forgery.
- The center remains navigable while every bay is occupied by a six-player group.
- Each bay has an obvious entrance and returns to the court.
- Beginning one canonical Keeper investigation opens G2; completing all six is not required here because
  Civic evidence is required to finish their cases.

### Civic Archive

- The Archive Nave is a clear central landmark.
- West and east evidence streets each form a complete loop back to the nave.
- The buried school, cistern, market, ration table, watch floor, far water, breach, warm collapse, shelf,
  coops, markers, and dead stall are actual civic places rather than labeled museum boxes.
- Each place has a distinct silhouette, focal object, evidence action, and visible return route.
- The district supplies the corroboration needed to complete Keeper theories.
- G3 is located at the Puzzle Works/Undercroft boundary and opens on `undercroft_open`.

### Puzzle Works and Reckoning

- Contains cross-Keeper proof without turning into an answer-sign laboratory.
- Houses Lampworks descent, third lamp, Painted Line, Stone of Reckoning, co-op rehearsal, and the
  performed on-ramp to the deep.
- The Unlit does not appear here. A Record/route cue may tell players to return to the village well when
  the external Unlit expedition becomes relevant.
- When a later gate requires Unlit evidence, the closed gate gives diegetic state feedback but never claims
  a physical route exists in the Hold.

### Threshold / Prior / Dread district

- Main route remains linear through the district.
- Prior Expedition is a west evidence branch with a clear return to the Case Board.
- Dread Procession is an east controlled branch with a clear rejoin before G5.
- Neither branch bypasses G4 or G5.
- Players may leave the Hold to re-check earlier evidence or complete the village-well Unlit expedition,
  then return through the Mouth without losing state.
- Threshold Vault fragments adapt to the present roster and never require voice recognition.

### Accepting, Unwriting, and Release

- Accepting fits the complete active group without crowding fixtures.
- The learned Bow action is physical consent, not a new unexplained input.
- G6 opens only after `bowed_as_one`.
- Unwriting preserves the Seventh choice, final reading, and release dependencies.
- After Release, the group exits by walking back through the same permanently opened route and up the
  Grand Stair. The changed rooms and open gates provide the aftermath; no shortcut entrance is required.

## Gate-state contract

| Gate | Initial state | Canonical open condition | Backtracking rule |
| --- | --- | --- | --- |
| G1 Rosetta | sealed | `rosetta_known` | Once opened, remains open |
| G2 Investigation | sealed | canonical Keeper investigation begun after literacy | Once opened, remains open |
| G3 Undercroft | sealed | `undercroft_open` | Once opened, remains open |
| G4 Deep | sealed | `deep_gate_open`, including its canonical Iss/Seventh derivation | Once opened, remains open |
| Prior subgate | sealed | `prior_absence_known` or later Prior flags | Once opened, remains open |
| Dread subgate | sealed | `iss_caught` or `seventh_suspected` | Once opened, remains open |
| G5 Accepting | sealed | `prior_witness_ready` and (`accepting_onramp_open` or `threshold_open`) | Once opened, remains open |
| G6 Coda | sealed | `bowed_as_one` | Once opened, remains open |

Automatic synchronization is monotonic:

```text
new_open = locally_latched_open OR canonical_flag_is_true
```

A missing database response preserves the last physical state. A false or temporarily missing flag cannot
reseal an opened gate. Resealing requires an explicit operator reset command with confirmation; ordinary
`sync` cannot do it. Gate opening removes only the authored door volume and never edits its floor, roof,
side walls, adjacent fixtures, or another room's ownership volume.

## Resume contract

- Player story state remains group/global where canon requires it.
- Open gates persist across restarts and empty-server periods.
- A player who returns later enters through the Mouth and walks the already-open route.
- No per-player fragment or temporary entity is required merely to traverse an opened gate.
- Dynamic puzzle fragments rebuild from the current roster when the group re-enters their owning room.
- Consumable critical items are either non-removable, reproducible, or restored from canonical state.
- A disconnect during a puzzle cannot consume the only solution object or leave a gate half-open.

## Interaction and content contract

Every required fixture has:

- a stable site/content ID;
- a single room owner;
- exact block coordinates relative to the build origin;
- a declared front/facing;
- a reachable player standing zone;
- a collision-free sightline;
- the exact expected item, book, page, NBT, inventory, or block state;
- a puzzle/state dependency;
- a recovery rule;
- a build-time and runtime audit.

Lecterns are never empty. Chiseled bookshelves have exact facing and occupied slots. Containers have the
correct inventory and cannot lose irreplaceable evidence. Item frames have backing blocks and exact
rotation. Signs face the intended standing zone and cannot be embedded in walls. Doors open away from
reading positions. Decorative fixtures cannot occupy an answer-site radius.

## Protection contract

The existing whole-region protection remains mandatory and is extended only where testing finds a gap.
Non-admin players cannot:

- break or ordinarily place blocks;
- flood or drain the Hold;
- burn or explode authored blocks;
- piston-move blocks across the boundary;
- remove lectern books;
- destroy frames or armor stands;
- alter protected decorative shelves;
- extract irreplaceable evidence books from inventories.

Every intended interaction is an explicit exception owned by its puzzle listener. Adventure mode is the
play contract, but protection must still hold if a player arrives in Survival mode accidentally.

## Safe build workflow

1. Read-only inventory and diff audit.
2. Fresh-site survey; no blocks changed on failure.
3. Generate a dry-run manifest containing every intended block mutation, fixture, entity, and site record.
4. Prove room/gate/service ownership volumes do not overlap.
5. Prove the access graph and both-direction walking graph.
6. Build the approved vertical slice at a fresh location.
7. Audit and visually inspect the slice in Minecraft.
8. Build one gated district group at a time, never the full Hold blind.
9. Re-run content, collision, orientation, protection, and route checks after every group.
10. Simulate the entire flag progression from fresh state through Release and back out the Mouth.
11. Package only after live visual approval and an end-to-end non-op playthrough.

## Definition of done

The Hold is complete only when:

- all preserved ARG content has an implemented and audited owner;
- the Unlit remains accessible from the village well and nowhere else;
- every gate opens from its real puzzle/ARG state and stays open;
- every unlocked district can be exited back through the Mouth;
- every room and fixture is reachable in Adventure mode;
- no rooms, corridors, gates, fixtures, standing zones, or service spaces overlap incorrectly;
- every required book, lectern, item, inventory, sign, and clue contains final content;
- block protection survives adversarial tests;
- the story can be completed from a fresh canonical state through Release;
- the group can physically walk back out through the same Mouth;
- automated checks pass and the build also passes live visual review.
