# Deep Hold V4 - Content Migration Ledger

> **SUPERSEDED V4 ARCHIVE.** Do not migrate content from this ledger into V5. Use
> `design/V5-SUPERSESSION-MAP.md`.

Status: preservation inventory for the V4 redesign. Geometry remains draft until the spatial plan and
vertical slice are approved.

The source fixture IDs remain those in `DEEP-HOLD-FIXTURE-MANIFEST.csv`. V4 may move and redesign their
physical surroundings, but it may not silently drop their story, puzzle, or runtime roles.

## Inside the Hold

| V4 district | Preserved content IDs | Logical invariant |
| --- | --- | --- |
| Orientation | `undercroft_seal`, `forgotten_mouth`, `rune_rosetta`, `bow_marker_01`, `offering_cairn_01`, `kept_light_home_01` | Literacy and customs are taught before their late uses; grey seventh is not a seventh Keeper bay |
| Vaun bay | `stone_vaun`, `vaun_hoard_chest`, `vaun_bookshelf` | Audit/sort/tally precedes any stone receipt |
| Mara bay | `stone_mara`, `mara_lectern_1..5`, `mara_route_marker_1..4`, `mara_map_marker` | Players compare editions and physically walk the trusted route |
| Sella bay | `stone_sella`, `sella_pool`, `sella_anchor`, `sella_lectern_1..5` | Reflection and count evidence remain independently legible |
| Orin bay | `stone_orin`, `orin_marker_1..6`, `orin_frame_dial_1..6` | Crouch/posture, fall-order, and dial state remain physical mechanics |
| Brann bay | `stone_brann`, `brann_toll_tower`, `brann_corridor_start`, `brann_corridor_end` | Dark-hours toll and silence traversal remain fair with non-audio fallback |
| Iss bay | `stone_iss`, `the_cold_hearth` | Iss is caught through physical contradiction, not villain coding or a standalone cipher |
| Civic west | `school_stand`, `markers_row`, `cistern_7`, `watch_floor`, `set_apart_shelf`, `the_far_water` | Human, water, watch, and edited-record corroboration remain revisitable |
| Civic east | `deep_market`, `ration_table`, `third_bay_breach`, `warm_town_collapse`, `dead_stall`, `deep_bird_coops` | Material and social evidence contradicts Keeper claims where canon requires it |
| Puzzle Works | `lampworks_stair`, `third_lamp_stand`, `painted_line` | Descent, light, and line behavior remain active evidence rather than labels |
| Lower Works | `stone_of_reckoning`, `keeper_altar`, `coop_plate` | Place grammar and group-action on-ramp remain mechanically implemented |
| Threshold | `the_threshold`, `threshold_vault`, `failed_accepting` | True walk, dynamic-roster fragments, and failed-witness proof remain ordered |
| Prior Expedition | `case_board`, `prior_camp` | Answers without witness are exposed; six corrections can be revisited independently |
| Dread branch | `dread_route_start`, `dread_route_elsewhere`, `dread_route_figure`, `dread_route_exit` | Controlled dread route rejoins before Accepting and cannot bypass gates |
| Accepting | `unbroken_light` | The complete present group can perform the learned synchronized act |
| Unwriting / Release | `the_unwriting` | Seventh choice, reading, name, and release remain finishable in canonical order |

The initial inventory contains 76 stable fixture IDs. The V4 fixture manifest must contain exactly one
owner row for each ID above, plus separately versioned rows for genuinely new connective content. New
content IDs may not reuse a canonical site ID.

## Outside the Hold and preserved in place

| Surface | Preservation rule |
| --- | --- |
| Village well | Remains the sole physical entrance to the Unlit |
| The Unlit | Remains its own expedition/world and is never generated inside the Hold |
| Surface NPC dialogue | Well/Unlit directions remain true; only revise lines that conflict with approved Hold consolidation |
| Downloadable Hold copy | Continues the invitation/address reconstruction role |
| Copperline / Discord relay | Remains a cross-surface identity handoff |
| Record website | Retains provenance, falsified entries, theory state, and fair-answer handling |
| Discord and Observer voice | Remain optional cross-surface reactions; voice never becomes a blocking route requirement |
| External media | Continues to confirm reeds, dark hours, prior-base provenance, and `SIX RETURN, ONE IS NOT KEPT` |
| Wren | Retains player-grounded memory/evidence behavior and cannot become an exposition dispenser |

## Canonical gate/state dependencies to preserve

- `rosetta_known`
- canonical Keeper investigation-begun detection
- `undercroft_open`
- `iss_caught`
- `seventh_suspected`
- `deep_gate_open`
- `prior_absence_known`
- `prior_camp_read`
- six Prior correction flags
- `prior_witness_ready`
- `threshold_open`
- `accepting_onramp_open`
- `bowed_as_one`
- `seventh_named`
- `seventh_choice`
- `record_released`

Physical gate latches mirror these states monotonically. They do not replace or rename the canonical flags.

## Required manuscript and inventory pass

Before full construction, every row in the final fixture manifest must link to one of:

- an exact book manuscript and title;
- an exact sign/text payload;
- an exact container inventory;
- an exact item/NBT specification;
- an exact block-state puzzle configuration;
- an exact entity/PDC specification;
- a declared intentionally empty state whose absence is the evidence.

`placeholder`, blank required book, generic lore, inferred inventory, and operator-only setup are build
failures. Intentional absence must be explicitly named and audited so it cannot be confused with missing
content.

## Change discipline

Changes to dialogue, lore, puzzles, or outside structures are allowed when necessary to make the ARG
cohesive and finishable, but each change must record:

1. the original canonical claim or mechanic;
2. the implementation problem;
3. the smallest coherent correction;
4. every affected surface;
5. the test proving that the corrected story still resolves.

No physical convenience change may silently rewrite the six Keepers, the Seventh, the customs, the Unlit,
Accepting, or Release.
