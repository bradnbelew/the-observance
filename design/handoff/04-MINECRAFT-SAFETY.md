# 04 — Minecraft Mechanic Safety & Build Verification

> **CURRENT STATUS NOTE (2026-07-18).** The KS01 5/6 failure cited below is resolved and preserved as
> regression history in doc 5. Its strict uniqueness/support audit remains mandatory, and the combined
> fresh Paper/restart receipt passes it. All other safety rules in this document remain binding.

Brad's rule 6: **minimal fragile mechanics; deterministic builds only.** His deeper fear is an
AI-built structure that *looks* right but traps players, walls off a room, floods a chamber, or fails
its own install. This session already hit all four of those failure modes. This doc is the guardrail so
the rebuild doesn't repeat them.

---

## 1. Safe vs. fragile — what to build with

**Safe (use these):**
- **Deterministic block reads.** A predicate that inspects fixed blocks/containers/frames at exact
  coordinates. No timing, no physics, no player-triggered chain reactions.
- **Simple containers** (barrels, chests, lecterns, chiseled bookshelves) holding exact tagged items or
  books. The recovery engine already handles these safely.
- **Item frames as *readable* props** (a fixed clock, a map) — but see §2, do not overload frame
  *combination* mechanics as the default puzzle verb.
- **Static evidence**: books, signs, named items, world layout you count/read.

**Fragile (avoid, or use sparingly with extreme care):**
- **Redstone contraptions**, timing circuits, observer/piston chains — do not build puzzle logic from
  these. They desync, they break on chunk reload, they're the #1 AI-build failure.
- **Water/lava as open surfaces.** Flowing liquid spreads and floods walkable floors and drowns
  standing cells. This session shipped a bug where `sella_pool` / `cistern_7` / `far_water` flooded
  their rooms. **Rule: any water is recessed into the floor or held behind a one-block curb, never an
  open source at foot level next to a standing cell.** (Fixed builders exist as a reference:
  `buildHoldWaterMirrorCore`, `buildHoldCisternCore`, `buildHoldFarWaterCore` — copy that curb pattern.)
- **Dense multi-item-frame combination puzzles** with count-only feedback (brute-forceable; and the
  fresh-install frame-identity check is currently *broken* — see doc 5, KS01). If you must use a frame
  set, keep it small, deduced (not guessed), and verify the installer places every piece.
- **Anything that depends on client-side line wrapping** (book line-counting — doc 3 §6).

## 2. The item-frame trap (learn from KS01)

The KS01 six-strip Atbash puzzle was the historical hard blocker (doc 5): an earlier fresh build placed
"5/6 unique pieces" and fails its own readiness audit. Item-frame *sets* are the most fragile mechanic
in the codebase because the installer must spawn N entities, tag each, face each, and prove the set is
complete and unique — and a single chunk-load race or coordinate mismatch drops one silently. **When
you rebuild content (doc 2), prefer verbs that need zero spawned entities** (read/count/compare/submit)
over frame sets. Every frame set kept remains a liability the installer has to get exactly right; the
current combined receipt proves this exact set, but future changes must re-run the same strict audit.

## 3. Traversability & no-escape — mechanical rules

These are Brad's rule 5, expressed as checks the build must pass (bake into `validate()` and the
offline sim, §5):
- **Every required standing cell is standable**: solid floor at `y-1`, air at `y` and `y+1`. The
  builder already has `assertStandable(x,y,z,label)`; assert every room entrance, aisle spine, and
  fixture standing cell.
- **No 2-block jumps on any required path.** Vertical change ≤ 1 block per step; use stairs/ramps.
- **Corridors don't wall themselves.** The builder's carve primitives each write their *full*
  wall/foundation envelope, so a later primitive can re-wall a junction an earlier one opened. This
  session shipped a bug where the staff/service stairs and lower-office crossings self-walled;
  `repairStairJunctions()` + extra `assertStandable` calls fixed it. **Rule: after all carves, run a
  junction-repair pass that re-clears every crossing, and assert each crossing standable.** Order-
  dependent overlap is the enemy — the offline sim is what catches it.
- **No escapes / no skips.** The Hold is Adventure-mode + region-guarded (`HoldProtectionListener`).
  The sim must confirm no walk path leaves the authored envelope, and no fixture is reachable before its
  gate's flag is set (gate order is a real wall, not decoration).
- **Correct orientation.** Every wall sign faces its reader; every lectern faces its standing cell;
  every frame faces out. `validate()` already checks `frontMatchesStandingZone` — keep it.

## 4. Build-time assertions (fail before the world, not in the playtest)

The build must *refuse to publish a readiness receipt* if anything is wrong. Existing guards to keep and
extend:
- `DeepHoldV4Plan.validate()` — static: room bounds, no overlap, fixture ownership, standing frames,
  gate order, graph connectivity, record stations. **Extend this for the new floorplan.** A non-empty
  return hard-blocks the build.
- `OperationBuffer.assertStandable(...)` and `assertCompactWritesFrom(...)` — the builder proves key
  cells are standable and that no write escaped the authored envelope, *before* applying. Add an
  assertion for every new room's entrance + aisle + fixture standing cells.
- The runtime `finishDeepHoldV5Build` path — after placing fixtures it runs `auditV4OpenRoute` (a live
  BFS from the Mouth) and per-fixture standing-frame checks, and refuses the receipt on any failure.
  This is why the current build *correctly* fails on KS01 rather than shipping broken — keep that
  fail-closed behavior.

## 5. The offline reachability sim (your fastest safety net)

A live cutover build takes ~10 minutes and burns tokens on log-reading. An **offline block-level
simulation** proves reachability in **seconds** and is how you iterate on layout without Paper.

> **CORRECTION (per Codex's review): this sim does NOT yet exist in the repo.** An earlier draft of this
> handoff called it an "existing invariant" — that was wrong; a working version existed only in an
> ephemeral scratchpad this session. **Build the faithful current sim FIRST, before major geometry
> work.** A proven *reference* (modelling the OLD compact geometry — adapt its primitive replay to the
> new coordinate-native plan) is committed at `tools/sim_hold_reachability_REFERENCE.py`; read its
> header. The node-progression `tools/simulate_v5_scenarios.py` is a different thing and does not prove
> block-level reachability.

Build and maintain it (keep a repo copy in sync with the builder):

**Algorithm:**
1. Model the world as `solid[(x,y,z)] = bool`, default *solid* (buried rock).
2. Re-implement each builder primitive as **solid/air writes only** (ignore materials): `corridorZ`,
   `corridorX`, `buildVaultSlice`, `openIntersection`, `buildRoomShell`, `placeSimpleTread`, the
   grand-stair treads, the gatehouses, and your new per-type dressers (dressers must not fill an aisle).
   Run them **in the exact same order** the real builder does.
3. Model gate door cells as passable (mirror `holdGateSpan`/`isV4GateDoorCell`).
4. Seed a BFS at the Mouth's interior floor cell; a step is legal iff the target cell is *standable*
   (solid below, air at feet+head) or a gate door cell.
5. **Assert:** every room has a reachable floor cell (inset from walls); every fixture's standing cell
   is in the visited set; every record station has a reachable reading cell; and `visited` never
   escapes the authored envelope (no runaway flood = no accidental exit).
6. Print the first unreachable room/fixture with its nearest reached cell (diagnostic), so a failure
   points you at the exact junction.

Keep the sim's primitive replay **byte-for-byte faithful** to the builder — when you change a carve or
add a dresser, update the sim in the same commit. A green sim + a green live audit + a green
post-restart audit is the reachability contract. A green sim alone is necessary but not sufficient
(it doesn't model item-frame installs or water physics — those need the live audit).

## 6. The full build verification loop (every layout/content change)

1. `validate()` green (static).
2. Offline reachability sim green (seconds).
3. `python tools/check_deep_hold_layout.py` + `check_deep_hold_fixture_manifest.py` +
   `check_v5_physical_predicates.py` green (static manifests agree).
4. Live cutover on a **fresh** disposable server copy (never a partially-cut-over one), unique ports:
   `obs sleep on` → `obs placehold prepare` → `obs placehold plan` (PLAN PASS) → `obs placehold build`
   → wait for a **complete readiness receipt** → `obs placehold audit` (0 findings) → `obs visualaudit`.
5. **Stop Paper, restart, re-run `obs placehold audit` + `obs preflight`.** A pass before restart is
   insufficient (Brad's explicit rule).
6. Only then is the build real. Commit.
