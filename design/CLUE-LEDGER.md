# V5 clue and revelation ledger

Status: current

Machine-readable node authority: `design/ARG-V5-NODE-MANIFEST.csv`

Solutions and hints: `arc/v5/SOLUTION-CASEBOOK.md`

The ledger contains exactly 82 required nodes. The CSV owns each node's case, order, room, modality, input surface, prerequisites, durable flag, reward, and recovery. This document owns the high-level reveal budget and anti-repetition rules.

| Case | Nodes | New fact earned | Material payoff |
| --- | ---: | --- | --- |
| C01 Lost Server | 6 | mkept deliberately preserved a real server | Handoff Receipt + Orientation Key |
| C02 Long Cold | 6 | the Hold began as rational refuge infrastructure | Survey Seal + G1 |
| C03 Keeper Dossiers | 18 | six distinct people left independently testable evidence | six sealed affidavits |
| C04 Cistern Winter | 8 | Nessa was scapegoated for counterfeit/diverted filters | Filter Cartridge + Cistern Seal |
| C05 Break Inquest / Unlit | 8 | the Break had interacting physical social and uncanny causes | Breach Plate + Deep Line |
| C06 Restoring the Hold | 7 | repaired systems change the world and access | System Key + Deep Access Plate + G4 |
| C07 ASH-13 Company | 10 | four modern investigators failed through Wren's sabotage | Witness Spool |
| C08 Wren's Betrayal | 5 | Wren knowingly traded names plans and fears | branch-marked Protocol Bridge + G5 |
| C09 Averyn | 8 | Averyn is the erased registrar and Record interface | AVERYN receipt + G6 |
| C10 Release | 6 | six affidavits return but Averyn is not rebound | durable coda + goodbye + shutdown |

## Evidence confidence

Each major truth must be supported by at least two different evidence families:

- documentary provenance;
- physical Minecraft state;
- independent voice/testimony;
- cross-media evidence;
- system behavior.

Two books written by the same office do not count as two families. The Record never corroborates itself.

## Redundancy filter

Cut or rewrite any artifact that only restates one of these without adding a job:

- there were six Keepers;
- Averyn was omitted;
- the Record watches;
- the Ways matter;
- a prior group failed.

An artifact earns its place only if it adds a fact, contradiction, character dimension, operating instruction, location, proof, or payoff.

## Fairness fields

Every node is release-blocking unless all ten exist:

1. lead;
2. evidence;
3. taught/inferable operation;
4. input affordance;
5. correct feedback;
6. partial/wrong feedback;
7. three hints;
8. material or theory payoff;
9. recovery;
10. durable completion flag.

`tools/check_v5_content.py` enforces node count, case distribution, graph acyclicity, books, media, NPC registry, and forbidden live-story claims.
