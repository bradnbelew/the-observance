# M3 — Authored P4 Private Vertical Slice Revision

Status: **V2 AUTHORED REVISION AND FRESH PAPER RECEIPTS COMPLETE; BRAD RE-REVIEW REQUIRED**

Authority order remains `SPINE-LOCK.md` → approved conformance → approved Phase 1 → Phase 2 evidence
architecture → M2 technical contracts → this file. `vertical-slice-v1.json` remains the exact rejected
first pass. `vertical-slice-v2.json` is the current authored replacement. Brad's 2026-07-16 rejection
is historical and binding; no revision can turn it into approval. M4 remains closed until Brad walks v2
and explicitly approves it.

## Scope and containment

V2 changes only the Brad-private P4 slice. It does not build P5 or any later district, touch production,
connect to Crafty, mutate Supabase, contact Railway/Vercel, load credentials, alter media, or become a
second campaign runtime. P4 expands inside the unused pre-P5 depth to X `-34..34`, Y `-24..10`, Z
`-10..92`; P5 still begins at Z `93` and remains reservation-only.

The v2 slice contains the same bounded program:

- one civic Surface Mouth and reversible descent;
- one large intake/public-works hall;
- one public hallway and one controlled gate;
- one fully authored ordinary copying office with a separate staff cart route;
- the five-finding P4 investigation, physical evidence, filing, replay, and catch-up;
- one restrained, approval-gated A2 positional discrepancy.

## Brad-rejection response

| Binding finding | Authored v2 response | Machine authority |
| --- | --- | --- |
| Plain single-note palette | Five deliberate zones use tuff, stone brick, deepslate brick/tile, basalt, dark oak, and weathered/oxidized copper according to construction campaign and civic job. | `palette_authority`; ≥18 distinct governed materials. |
| Empty/underscaled rooms | Mouth interior is 322 floor cells, intake 651, copy office 273. Intake has ten-block clear height and the public hall/gate read at 9/11 blocks wide. | `scale_receipt`; exact bounds and reachability. |
| Arbitrary pool | A 58-block recessed system has a runoff inlet, settling basin, two-block gauging flume, grated sump, capacity gauge, maintenance curbs, and 300-berth outflow label. | `waterworks`; exact volumes/counts; no flood escape. |
| Illegible copying room | Two five-place copy desks, binding bench, clerk counter, 44 vertical cabinet blocks, paper carpets, ledgers, bookshelves, eight lights, public reading aisle, staff cart route, and professional records fill the room. | `copy_office_density`; 48/273 blocking footprint with 3-wide public and 2-wide staff routes. |
| Confusing entrances/exits | Mouth entry/return, public copy door, both staff thresholds, runoff/outflow, controlled gate, and intentionally sealed future stub each carry exact signs and distinct floor/arch composition. | Three exact doors, eight threshold signs, named routes. |
| Half/porous gate | The controlled gate is an 11-wide × 8-high copper-grate collision plane inside a permanent blackstone/iron control frame. Closed means all 88 cells collide; open means none do. | Closed/open collision and BFS assertions. |
| JSON promises instead of experience | Fourteen physical evidence surfaces retain observation provenance; six lecterns file findings/replay; authored books/signs are read back in Paper; a finding cannot file before two physical sources are inspected. | Paper block-state audit plus local journal events. |

## Exact composition and circulation

The authority records every room, doorway cell, route waypoint/width, standing cell, sightline, water
volume, furniture footprint, vertical cabinet volume, evidence block, filing lectern, sign, gate cell, and
material count. Small furniture construction remains implementation code, but it does not choose
placement. `tools/check_m3_vertical_slice.py --authority-only` expands and verifies the exact contract;
`tools/sim_m3_vertical_slice.py` replays its solid/air consequences.

The current offline receipt is:

- 248,745-cell bounded envelope;
- 1,756 reachable standing cells closed;
- 1,787 reachable standing cells open;
- 31-cell route delta wholly behind the gate;
- 24 authored standing/readback cells;
- 14 evidence surfaces and six filing/replay surfaces;
- 88 closed gate collision cells and zero open collision cells;
- 58 contained water blocks;
- every public, staff, evidence, submission, and return waypoint reachable without a two-block step.

The staff cart vestibule terminates in intake/copy-office work. It never crosses Z `89`, so it cannot
bypass the public gate. The future stub is a three-block-deep protected preview bounded by an explicit
back wall and sign; closed it is unreachable, open it is reachable, and it contains no P5 geometry.

## Physical investigation and filing

Players right-click authored blocks/books to inspect evidence. Paper persists an
`observation_committed` receipt and contributor before showing the retained observation. The matching
filing lectern accepts P4.F1–P4.F4 only when at least two authored independent sources for that finding
have been inspected. The local-primary journal rejects an unauthored source, a changed idempotency body,
and a finding filed before its observations.

The intake desk then accepts P4.F5 only from the exact four committed finding receipts. It persists the
synthesis and gate event before changing the 88 gate blocks. A wrong or premature filing changes
nothing. Same bytes replay idempotently; changed bytes fail; contributor history does not determine
eligibility. Restart reconstructs observations, findings, contributors, gate, and approvals from the M2
hash-chained journal.

The field-archive lectern reports committed findings, gate state, the remaining dispute, changed place,
and both A2 accessibility descriptions. It points reviewers back to original physical sources. It does
not reveal P5 or a future proposition.

## Protection and non-op review

The review runtime is creation-only and marker-bound. It defaults all non-operators to Adventure mode
without clearing or escrowing inventory. The v2 region cancels block break/place, bucket mutations,
entity damage/interaction, hanging removal, container mutation, and non-plugin teleports. A movement
guard independently denies crossing the gate boundary while local P4.F5 is absent, even if collision
were externally disturbed. Operator bypass is maintenance authority, never player evidence.

The disposable harness binds only `127.0.0.1`, uses a dedicated port, disables production shutdown,
resource-pack enforcement, drama, mobs, structures, RCON, and external credentials, and records its
exact server properties. Configuration/readback is not a fabricated real-client receipt: an actual
non-op client join and bypass attempt remain open until Brad performs them.

## Authored A2 moment

`INTAKE_TALLY_RETENTION` is now a physical/client implementation, not only a payload declaration. After
P4.F3 and before P4.F5, console or an operator may persist one exact approval naming distinct west/east
players with a 1–30 minute expiry and the canonical payload hash. Both named players must occupy their
authored standing zones. Presentation consumes the approval once, sends different client-only block
states at the same tally cell for twelve seconds, and restores the real block. Changed target, payload,
scope, expiry, prerequisite, or reuse fails closed.

Neither view carries evidence. The field-archive accessibility readback describes both to a solo
reviewer. The moment asserts only that observation can be selective; it does not explain the Dark,
Record, Watcher, or Averyn.

## Required receipts before Brad's walk

- [x] exact v2 data authority, composition counts, bounds, routes, sightlines, standing cells, gate
  collision, protection contracts, and offline reachability;
- [x] Java state/approval/protection/interaction self-test;
- [x] fresh disposable Paper 1.21.11 build and exact block/book/sign audit;
- [x] local observation → finding → gate-open exercise;
- [x] clean stop/restart/re-audit and replay;
- [x] exact JAR/journal/log/world-tree/world-package/package-set hashes;
- [x] separate fresh closed localhost review target prepared with non-op Adventure configuration;
- [ ] Brad's actual client walk, protection attempts, solo readback, and (if approved) two-client A2 view;
- [ ] Brad's explicit revised visual decision.

M4 remains closed regardless of automated passes until the final unchecked Brad decision exists.
