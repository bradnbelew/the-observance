# V5 authority and supersession map

Status: current

To prevent stale-story drift, only the files in the **Current authority** table may direct production. Every other pre-existing top-level `design/*.md`, `arc/corpus/*`, and `arc/lore/*` file is historical/reference material unless it begins with an explicit V5-current status. Historical files must not be packaged, seeded, copied into books, or used to resolve a contradiction.

## Current authority

| Subject | Authority |
| --- | --- |
| spoiler truth | `arc/WORLD-BIBLE.md` |
| required graph | `design/ARG-V5-NODE-MANIFEST.csv` |
| player order/surfaces | `design/EXPERIENCE-MANIFEST.md` |
| master implementation contract | `design/ARG-V5-MASTER-PLAN.md` |
| solutions and hints | `arc/v5/SOLUTION-CASEBOOK.md` |
| exact books | `arc/v5/minecraft-books.json` |
| exact book holders/mounts | `design/ARG-V5-BOOK-PLACEMENT.csv` |
| executable node ownership | `design/ARG-V5-RUNTIME-BINDINGS.csv` |
| exact Minecraft success predicates | `design/ARG-V5-PHYSICAL-PREDICATES.json` |
| NPC dialogue | `arc/v5/npc-dialogue.json` |
| fixed media | `arc/v5/media-manifest.json` |
| rooms | `design/ARG-V5-ROOM-ASSIGNMENTS.csv` + `DEEP-HOLD-ROOM-BOXES.csv` |
| fixtures | `design/ARG-V5-FIXTURE-OWNERSHIP.csv` + `DEEP-HOLD-FIXTURE-MANIFEST.csv` |
| gates | `design/DEEP-HOLD-GATE-MANIFEST.csv` |
| district Record stations | `design/ARG-V5-RECORD-OWNERSHIP.csv` + `DEEP-HOLD-RECORD-STATION-MANIFEST.csv` |
| critical items and recovery | `design/ARG-V5-ARTIFACT-MANIFEST.csv` |
| Hold visual/layout | `design/visuals/deep-hold-v5-blueprint.png` + `.svg` |
| Keeper cases | `design/V5-KEEPER-DOSSIERS.md` |
| Unlit | `design/V5-UNLIT.md` |
| Wren | `design/V5-WREN-EVIDENCE.md` |
| finale | `design/V5-FINALE.md` |
| book integration | `design/V5-BOOKS.md` |
| setup and testing | `design/V5-WORLD-SETUP-AND-TESTING.md` |
| production launch | `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md` |
| live acceptance cases | `design/V5-LIVE-TEST-MATRIX.csv` |
| production toolchain | `tools/README.md` + `tools/audit_all.ps1` |
| engineering background | `design/research/*` files explicitly labeled research reference |

## Toolchain boundary

`tools/audit_all.ps1` is the only aggregate release check. It validates repository bytes, current/archive freshness, all 82 nodes, the executable physical predicates, Hold geometry/runtime integration, deterministic failure scenarios, Discord, dashboard, plugin, packaged assets, deploy hashes, and all five external media sources. It deliberately does not invoke the older concern, sidequest, director, placement-packet, or rehearsal-packet checks.

`tools/check_v5_freshness.py` enforces this map. A pre-V5 top-level design file without a leading archive banner fails. A retired generator without an early terminating guard fails. A current surface that names a pre-V5 plugin version or retired runtime flag fails.

Generated local `rehearsals/` and `build/launch-placement/` folders are never authority and must never be committed. V5 evidence is recorded against `V5-LIVE-TEST-MATRIX.csv`, not generated from an old packet template.

## Retired assumptions

Any file describing the following is archived regardless of its older “source of truth” language:

- six Keepers as one mind/generations;
- a generic missing seventh as the whole investigation;
- six prior answers and no witness;
- mkept as a non-person;
- optional media, Unlit houses, side stories, Nether/End lore lanes, or Dread;
- the future grave/Accepting date as a required plot;
- old V2/V3/V4 content/gate conditions;
- manual filling or operator-staged critical content;
- a finale that leaves Paper running without a production shutdown option;
- plugin versions before V5.

Runtime checks scan production surfaces for these claims. Archive prose is retained only to explain project history and must carry or inherit this supersession rule.

Historical documents can still contain old names, versions, commands, and conclusions after their archive banner. Those strings are evidence of project history, not instructions. Current documents may refer to retired ideas only to prohibit them. If a historical file is copied, quoted, or turned back into an executable generator, it must first be reconciled to V5 and promoted explicitly in this map.
