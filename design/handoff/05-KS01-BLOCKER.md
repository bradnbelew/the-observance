# 05 — The KS01 Fresh-Install Blocker (must resolve before any launch)

> **RESOLVED / HISTORICAL FAILURE RECORD (2026-07-18).** The installer uniqueness defect was fixed
> without weakening the six-piece audit. Source `924a5495a6deee0700ae54f7ef54437030ef5375` subsequently
> passed the fresh combined Hold + Unlit Paper 1.21.11 build, strict physical audit, graceful stop,
> restart, independent re-audit, occupied-rebuild refusal, and deterministic package proof recorded in
> `COMBINED-CAMPAIGN-DISPOSABLE-PAPER-PASS-2026-07-18.json`. The failure below remains immutable evidence
> of why the check exists; it is not a current launch blocker.

**Status:** hard blocker. A full fresh cutover build of the *current* Hold reaches **76/76 fixtures
placed and full route traversal**, then fails its final readiness audit. Until this passes (or the
content rebuild replaces the mechanism), the production server stays closed — that was the original
launch-safety instruction and it still holds.

This is **pre-existing**, not introduced by the V5.1 pass. If the doc-2 content rebuild replaces Sella's
KS01 with a non-frame verb (recommended — see doc 4 §2), this blocker disappears with it. If you keep a
frame-set mechanism anywhere, you must fix the underlying installer bug.

---

## 1. The exact failure

```
IllegalStateException: V5 fresh physical install failed:
    KS01/strips: movable frame set is incomplete: 5/6 unique pieces present
  at ObservanceCommand.requireCleanPhysical (finishDeepHoldV5Build path)
  at V5PhysicalComponentInstaller.inspectReorderableFrameSets  (~line 2296-2298)
```
KS01 (Sella's waterline Atbash) requires **six** `PAPER` item-frames in a row at `sella_pool`, each
tagged with one evidence id (`ks01_dzgvi`, `ks01_pvvkh`, `ks01_gsv`, `ks01_yzxp`, `ks01_lu`,
`ks01_gsv_kztv`), in a deterministic unsolved permutation `[2,5,0,4,1,3]`
(`FRESH_FRAME_PERMUTATIONS.get("KS01:strips")`). On fresh install the audit finds only **5 of 6**
unique pieces present.

## 2. What has been ruled out (verified, not assumed)

- **Not a geometry/traversal bug.** Route/site counts are otherwise clean; unrelated to this session's
  stair-junction and water fixes.
- **Not a frame-backing bug.** RCON-probed the six offsets live: the installer's self-heal
  (`reconcileItemFrame` → `POLISHED_DEEPSLATE` when backing isn't solid) produced solid backing at all
  six positions.
- **Not a content/data bug.** All six evidence ids exist **exactly once** in
  `arc/v5/evidence-item-appearance.json`; the permutation is a clean 6-element list; the predicate JSON
  is well-formed.
- **Live coordinate lookup was unreliable.** The compacted anchor computed from
  `DeepHoldV4Plan.compactX/compactZ` did **not** match the coordinate actually written to the running
  server's `plugins/Observance/sites.yml` for `sella_pool` (sites.yml said `1062, 29, 1212`); and an
  RCON `@e[type=item_frame,...]` search at the sites.yml coordinate found **zero** frames in a generous
  radius. That discrepancy is itself a lead (see §3).

## 3. Leading hypotheses (for whoever fixes it)

1. **One of the six frame creations silently fails during fresh install** — a chunk-load timing race or
   a coordinate off-by-one in `reconcileItemFrame` / `frameEntityPlane` / `frameSpawnAnchor` for the
   reverse-face branch. `itemFrameFacing` flips to the opposite face when the "authored front" cell is
   occupied by another authored component (dense shared fixture) — the six strips are adjacent, so the
   reverse-face logic is exercised, and an edge case there could drop one.
2. **The audit's frame-entity search and the installer's placement disagree on the plane** for one
   position — `inspectReorderableFrameSets` searches `frameEntityPlane(entry.location(), itemFrameFacing(...))`
   with a 0.35 radius; if the installer spawned that one frame on a different face/plane, the audit
   sees 5.
3. **A coordinate-mapping drift** between the plan's compacted anchor and the runtime's placed anchor
   (the sites.yml mismatch) means the audit is checking near the wrong cell for one strip.

## 4. How to fix it (reproduce → instrument → confirm)

1. **Reproduce deterministically.** Fresh disposable copy of `build/paper-v5-playable`, unique ports,
   install the current JAR, `obs sleep on` → `placehold prepare/plan/build` at the test Mouth
   (`v5-playable 1000 69 1000`). It should fail identically. Run it **twice** — if the *same* strip
   fails both times it's a coordinate/logic bug; if a *different* strip (or none) fails, it's a timing
   race.
2. **Instrument `inspectReorderableFrameSets`** (the loop at ~line 2270-2299 in
   `V5PhysicalComponentInstaller.java`): log, per address, whether `exact == null` (no frame entity
   found at the expected plane) vs `identity == null` (frame found but its item isn't a recognized set
   piece). That single log line tells you which of the two disagreements above is happening, and for
   which strip.
3. **Cross-check the placement path** (`reconcileItemFrame`, ~line 1063) for that same strip: log the
   computed `facing`, `backing` block, `hangingCell`, and `frameAt` plane, and confirm a frame entity
   actually exists there after spawn.
4. **If it's the reverse-face edge case**, the fix is in `itemFrameFacing`'s `authoredFrontOccupied`
   branch and/or `frameEntityPlane` for that face. **If it's a timing race**, ensure the six spawns and
   the audit run in the same synchronous tick after the footprint chunks are confirmed loaded (the
   build already guards chunk-load; verify it covers the frame plane cell, not just the anchor).
5. **Confirm** with a full fresh cutover reaching a **complete** receipt, then a **restart + re-audit**.

## 5. The strategic alternative (recommended)

Per doc 2, the KS01 Atbash is also a **naked cipher** (the item lore literally coaches "turn the paper,
not the alphabet") and a **brute-forceable frame set** — two things the rebuild removes anyway. The
cleanest resolution is to **redesign Sella's investigation** (doc 2's layered-difficulty method) into a
verb that needs *zero spawned frame entities*: e.g. read the waterline strips as physical evidence in
the rebuilt cistern room, deduce the mirror-reading from a *found* key (not printed lore), and submit
the bearing to an answer sign. That eliminates the fragile installer path entirely and satisfies Brad's
standards in one move. If you do this, remove `"KS01:strips"` from `FRESH_FRAME_PERMUTATIONS`, update
the predicate JSON + hash (doc 7), and the blocker is gone.

Either path is acceptable. What is **not** acceptable is shipping with a build that can't reach a clean
fresh-install receipt.
