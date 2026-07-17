# M3 — Coarse Master Adjacency and Private P4 Vertical Slice

Status: **STRUCTURAL MACHINERY PROOF COMPLETE; VISUAL GATE FAILED — REVISION REQUIRED**

Authority order remains `SPINE-LOCK.md` → approved conformance → approved Phase 1 → Phase 2 evidence
architecture → M2 technical contracts → this file. The machine authorities are
`coarse-adjacency-v1.json` and `vertical-slice-v1.json`.

## Scope and boundary

M3 reserves one coordinate-native master footprint for P4–P12 and fully authors only the Brad-private
P4 slice. The later district rectangles are ownership and adjacency reservations, not build geometry.
They may not be turned into rooms before Brad walks and approves the slice.

The slice contains exactly:

- the public Surface Mouth and a reversible descent;
- civic intake;
- one public hallway;
- one ordinary intake copying room with an exact authored composition;
- the five-findings P4 investigation;
- one monotonic intake gate;
- one position-asymmetric Watcher moment, disabled unless its exact A2 payload receives a bounded
  approval for named test players and a private review session.

It is never player-facing, never linked from Copperline, never a progression runtime, and never a
substitute for the single post-prologue Crafty world.

## Coordinate and master adjacency authority

The Mouth threshold is `(0,0,0)`. X runs east/west, Y is offset from the sampled surface, and +Z runs
inward. Every value is a Minecraft block. Legacy transforms and constructor nudges are forbidden.

The coarse authority reserves nine non-overlapping district footprints for P4 through P12, one central
public spine, and two service runs. The public adjacency remains strictly P4 → P5 → P6 → P7 → P8 → P9
→ P10 → P11 → P12. The service runs cannot bypass public gates. Only `P4_PRIVATE_SLICE` has exact
geometry; all later reservations say `reserved_only`.

## Exact slice composition

The exact slice envelope is X `-18..18`, Y `-20..8`, Z `-8..84`.

| Space | Exact program | Player route |
| --- | --- | --- |
| Surface Mouth | Drainage survey, cart wear, resident-memory copy | Threshold to descent, same return route |
| Public descent | Thirty-two Z steps, one-block maximum rise/drop, shelter/civic/deep material joins | Reversible from Y 0 to intake Y -16 |
| Intake | Population, heat/water, recessed channel, public survey desk | Wide public circulation and staff apron |
| Public hallway | The only hallway; intake to the closed gate | Clear two-player lane |
| Intake copying room | Two copying desks, clerk counter, two cabinet ranks, public aisle, separate staff route | Public side door plus protected staff door |
| Future stub | Empty protected threshold only | Unreachable closed; reachable open |

Every furniture cell, door cell, standing cell, sightline, circulation waypoint, service waypoint, and
gate barrier is explicit in `vertical-slice-v1.json`. The ordinary room has a 22.2% blocking footprint,
with separate public/service circulation and non-blocking wall evidence; density does not consume its
standing frames.

## Layered P4 investigation

The slice implements the approved evidence jobs rather than the retired V5 stations:

1. `P4.F1` — drainage map, cart wear, and resident memory establish the one public Mouth.
2. `P4.F2` — material joins, survey revisions, and room-use rolls establish shelter, civic, and deep
   build phases.
3. `P4.F3` — population, ration, and heat/water sources cross-check plausible refuge capacity.
4. `P4.F4` — founding minutes, heat marks, and the protected water channel establish rational descent.
5. `P4.F5` — the intake desk accepts only the phase/capacity/motive synthesis after P4.F1–P4.F4 and
   opens the sole gate once.

Each finding binds the M2 finding and observation IDs, requires at least the M2 independent-source
minimum, excludes attendance and elapsed time, and permits any subset. A wrong theory changes nothing.
Replay cannot issue another effect. Catch-up derives from committed receipts and the five approved
session-brief fields.

## Watcher and accessibility boundary

`INTAKE_TALLY_RETENTION` is a restrained positional discrepancy: a player west of the clerk tally sees
one copied capacity digit appear freshly overwritten while a player east of it sees the worn mark. It
asserts only that observation can be selective. It does not explain the Dark, Record, Watcher, or
Averyn and is not evidence for any P4 finding.

Because it targets players by position, it is A2. It is never automatic. Its exact canonical payload
hash is `3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a`; changed payload, class,
scope, or expiry fails closed. Neither view carries a required proposition, so a solo player or a
player using the review readback loses no evidence.

## Protection, custody, and restart behavior

- Survival inventory remains with the player; there is no inventory escrow.
- The private region rejects block/entity/container/teleport/gate bypasses and allowlists only authored
  reading, inspection, submission, and replay interactions.
- Paper remains the intended physical authority. The isolated proof uses M2 `LocalPrimaryJournal`:
  finding and gate receipts persist before presentation, restart re-derives state, same-key/same-byte
  replay is idempotent, changed bytes are rejected, and cursor catch-up returns only missing receipts.
- P4.F5 cannot commit before P4.F1–P4.F4. A remote projection cannot open the gate.
- Contributor identity is provenance, not eligibility. Different players can complete lanes in any
  order and later contribute to a completed finding without duplicating the finding effect.

## Available evidence

- `python tools/check_m3_vertical_slice.py` verifies coarse non-overlap/order, exact composition,
  footprint, standing cells, sightlines, M2 finding/observation bindings, predicate hashes, protection,
  replay, any-subset behavior, A2 hashing, and honest external gaps.
- `python tools/sim_m3_vertical_slice.py` replays the exact solid/air build sequence. The closed state
  reaches 927 standing cells and blocks the future stub; the open state reaches 961 and adds 34 cells.
- `gradlew m3PrivateSliceStateSelfTest` proves any-order/subset findings, exact synthesis, local gate,
  replay, contributor provenance, cursor catch-up, restart, and exact/expiring A2 approval.

These are offline receipts. They are not Paper, client, Crafty, production, or human visual evidence.

## Disposable Paper structural receipt

`PAPER-DISPOSABLE-RECEIPT.json` records a fresh local-only Paper `1.21.11` build 132 target. The
review runtime is disabled by default and refuses mutation unless a creation-only harness supplies a
matching disposable-target marker, target ID, and source commit. The target bound only to
`127.0.0.1`, used a whitelist, loaded no production credentials, and never contacted Crafty or any
campaign service.

From source commit `16a712c08566135635fdfe383a42a85ed9320db5`, the exact plugin JAR built the
closed slice, read back all 99,789 envelope cells with zero findings, committed P4.F1-P4.F5 through
the M2 local-primary journal, opened the one gate, and read back the open state with zero findings.
Paper then saved and stopped cleanly. A fresh process loaded the same target and journal, returned the
identical open-state hash, and accepted the same synthesis replay without adding a receipt. The
receipt records exact Paper/JAR/journal/log/world-tree/world-package hashes.

This is a structural/runtime receipt, not a visual-quality receipt. The Paper projection contains the
authority's exact solid/air envelope and authored blocking composition, while finding commits are an
operator harness surface. It does not yet prove player-facing books/signs/submission UX, Adventure
movement, inventory retention, event-level protection, or the visual standard.

Brad completed a first in-game visual pass on 2026-07-16 and explicitly did not approve it. The durable
decision and exact findings are recorded in `BRAD-REVIEW-PACKAGE.md`. The palette, scale/density,
arbitrary water, illegible copying room, confusing blocked right entrance, weak entrance/exit
composition, and unconvincing half-gate all require revision. None of the structural receipts above
overrides that failed human gate.

## Honest external gaps

The disposable Paper structural build, exact audit, gate open, stop/restart, re-audit, replay, and
world-package receipts now exist. No authenticated Minecraft client was available, so M3 still does
not claim a non-op Adventure/survival-inventory walk, live block/entity/container/teleport/gate bypass
test, two-client asymmetric view, solo accessibility readback, or player-facing investigation UX.
Production Crafty, Supabase, Vercel, both Railway families, Discord secrets, and media bytes remain
untouched.

Brad walked the slice and rejected the first visual pass. Under the approved sequencing, M4 district
implementation must not start until the private slice is revised, rebuilt, re-audited, walked again,
and explicitly approved.

