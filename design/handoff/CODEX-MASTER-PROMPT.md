# Master Codex Prompt — The Observance full rebuild

> **PHASE 0 OVERRIDE — 2026-07-15.** This prompt's older 15-hour, fixed 10-case/82-node, immediate
> Railway deploy, and direct-to-Hold sequencing are superseded by `SPINE-CONFORMANCE.md` and
> `PHASE-0-AUTHORITY-AUDIT.md`. The target is 20–30 active hours (24–28 target), free-paced over weeks,
> with subset progress, durable catch-up/replay, no time gates or missable substantial content,
> director-approved hints, and approval-gated risky/personalized/world-changing beats. The gated
> `hold.zip` prologue is the only player-facing standalone save; all later Minecraft play runs on the
> brother-hosted Crafty Paper `1.21.11` server, while any standalone vertical slice is Brad-only review.
> Brad approved Spine Conformance on 2026-07-15, so experience architecture may begin; implementation,
> live mutation, and Hold geometry remain behind later gates.

> **Phase 1 is authored and awaiting Brad review.** Read `PHASE-1-APPROVAL.md` and its four linked
> authorities before doing further design. Do not start M1 evidence architecture until Brad approves
> that gate; approval still does not authorize implementation, media editing, live changes, or geometry.

> Paste the block below to Codex to start the rebuild. It is self-contained enough to prevent drift on
> its own, and it points to `design/handoff/01`–`08` for the depth. Codex holds the Supabase / Railway /
> Google Drive / Vercel connections; the Minecraft plugin is installed by Brad by hand.

---

You are taking over **The Observance**, a hard, 20–30-active-hour (target 24–28), free-paced,
group-played Minecraft investigation that Brad runs for about six close friends over several weeks.
Work in the repo at its root. There is a heavily-modified but committed worktree on
`main` (commit `feat: V5 production rewrite + V5.1 redesign pass`). **Preserve it — never `git reset
--hard`, `checkout` away, `clean`, or bulk-delete. The `design/` archive is a working, self-enforced
supersession system, not cruft.**

## What this is (read before doing anything)

A prior pass added polish — reawakened the ambient Watcher, rebuilt the finale to ~70s, de-kiosked the
Unlit's seven houses, added one multiplayer moment, and a `/progress` command. **That was NOT the job.**
Brad's verdict: it produced "a slightly prettier version of the ARG from before." The real job is a
**ground-up rebuild of the Deep Hold — both its physical layout and the puzzle/content inside it** —
because the current Hold is the core problem: **32 oversized rooms (~1015 m² average, ~5% furnished,
one walk-up mechanism each)** that read as a generated dungeon with stations, not believable places.

**Read `design/handoff/SPINE-LOCK.md`, `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md`,
`README.md`, and `01`–`08`, then `arc/WORLD-BIBLE.md`, completely before acting.
Those documents are authoritative; this prompt is the summary.**

## Brad's binding standards (judge every piece of work against these FIRST)

1. Puzzles are **investigation woven with story** — books, items, media, the website, NPC knowledge,
   lore callbacks, environmental evidence. **Never** a walk-up mechanism. **Never** a naked cipher (key
   printed beside the ciphertext); a cipher's key must be *earned or deduced*, and layered with
   research/lore/callbacks.
2. Two prose laws: **no AI slop** (no perfect parallelism, "not X but Y" tics, planted aphorisms,
   rule-of-three portent) and **no ultra-mysterious ARG language.** Everything reads like real human
   records and conversation. The player is inside a *world*, not "playing an ARG."
3. **Environments match the fiction (hard rule):** book puzzle → build a real library; industrial
   puzzle → a real works floor; camp puzzle → a real camp. The evidence is the *furniture of a
   believable place.*
4. **No contradictions:** anything text mentions ("a line in the hallway") physically exists in the
   build. Dialogue, lore, books, and world all agree.
5. **Structures: concrete floorplan first** — one coordinate system, every room/door/corridor/prop
   proven non-overlapping *before* building (don't design rooms and fillings separately — that's how
   they overlap and furniture ends up in walls). Traversable: doors, hallways, no 2-block jumps to
   anything required, players can't escape/skip/enter-or-exit where they shouldn't; every required
   fixture has a reachable standing cell; all key components visible, functional, correctly oriented.
6. **Minimal fragile mechanics:** no complex redstone, no timing contraptions, no scattered item-frame
   gimmicks as the default. Deterministic block reads and simple containers only. The Hold is
   large-scale; other structures are regular.
7. **Very hard, long, group play:** difficulty from deduction and attention, not opaque controls; no
   hand-holding. Multiplayer-contribution puzzles and per-player asymmetric hallucinations are wanted —
   but it must be **possible and reasonable to reach the answer**, and players must **know where to
   submit it**.
8. **Strong finale** (already rebuilt — keep it).
9. **Services are yours** (Railway/Supabase/Drive/Vercel). Any DB/service change is done by you; hand
   Brad nothing to run on the plugin side except installing the final JAR.

## Invariants you must NOT regress

- The single-Surface-Mouth, one-command, tick-batched, **restart-safe** build system, and its
  **build-time standability assertions + offline reachability sim** (the safety net that catches
  self-walling corridors and flooded rooms). Extend these to the new layout; never remove them.
- The fairness/recovery engineering: no-touch completion, atomic wrong-item returns, never-deleted
  evidence, idempotent gates, durable local-primary progress, duplicate-artifact protection.
- **The predicate-hash chain.** `design/ARG-V5-PHYSICAL-PREDICATES.json` → its SHA-256 →
  `settings.v5_physical_authority_sha256` in Supabase (`fdnmhbpxnodrnbrzrlqq`) → the plugin contract.
  All four agree at handoff on `37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b`.
  **Any** edit to that file changes the hash — you must re-sync the seed, regenerate the bundle
  (`cd discord && npm run db:seed && npm run db:bundlecheck`), apply an **incremental** migration (never
  re-run the whole `apply-all.sql`; `0016_security_grants` is already live), and re-run the plugin +
  python audits. See `design/handoff/07`. Keep `node_id`s and `completion_flag`s stable across a
  mechanism rewrite so the case graph, gates, and Supabase rows don't move (10 cases / 82 nodes / 60
  plugin predicates are contractual).
- Never expose the service-role key or any secret.

## Do this, in this order

**Phase 0 (current; documentation and read-only discovery only):**
- Produce the Spine Conformance Statement and stop for Brad's approval before canon-expression,
  cases/evidence, schema, Unlit form, media replacement, or geometry.
- Audit active authorities for stale 32-room, 10-case, 82-node, fixed-media, 15-hour, shared-world, and
  service-topology assumptions; document exact read-only Supabase, Vercel, both Railway projects, and
  Crafty mappings without exposing secrets.
- Update authority routing and run relevant non-live checks.
- Do not deploy, migrate, rotate secrets, touch the brother's server, or begin Hold geometry.

**Post-approval Phase 1:** produce the 24–28 hour experience/case map, subset-progress and catch-up/replay
model, required-content and Brad-footage inventory, hint/automation approval model, authority migration
plan, and coarse cross-surface topology before any floorplan or vertical-slice geometry.

**Phase 1 — rebuild the Hold, ONE district at a time** (`design/handoff/01` + `02` + `04`):
1. Design the floorplan in the data layer (`plugin/.../structure/DeepHoldV4Plan.java`) and make
   `validate()` prove non-overlap, standing-frame legality, and connectivity — before building.
2. Build it dense (40–70% furnished) with a fiction-specific dressing routine in
   `DeepHoldV4Geometry.java` (`dressLibrary`, `dressArchiveRank`, `dressServiceBench`, `dressCamp`, …).
3. Rebuild the case(s) that live there to the layered-difficulty formula (investigate → notice →
   cipher-keyed-by-earned-thing → recall/callback → submit-obviously), co-designed with the room.
4. Prove it: `validate()` → offline reachability sim → static audits → a **fresh** live cutover reaching
   a complete receipt → **restart + re-audit** → non-op Adventure walk. Commit. Then the next district.

**Phase 2 — cross-cutting polish** (`design/handoff/03`, `06`, `07`): prose/voice sweep (purge the
in-world design notes like "finale button" / "the room should feel wrong" / "A group should argue
here"; break the NPC couplet meter; quarantine the legacy `voice.archive.ts` corpus); seed more
asymmetric-multiplayer moments; de-brand media and add a Discord/website media mirror; bake
`the-hold.zip` into real region files; move Wren's body on the reckoning.

**Phase 3 — launch readiness** (`design/handoff/08`): `tools/audit_all.ps1` green; full fresh cutover +
restart audit clean; Unlit/dialogue/finale/preflight audits pass post-restart; the 100-row live matrix
walked; repackage the launch kit with matching hashes; final service deploy. Then the production server
opens (Brad installs the JAR by hand).

## Working discipline

- **One district/case end-to-end at a time, kept green, committed between them.** Never a big-bang; that
  is exactly how the last version drifted into empty halls.
- **Prove reachability offline (seconds) before a 10-minute live build**, and never trust a pre-restart
  pass.
- **Move content and its authority together** — fiction + predicate JSON + hash + Supabase seed +
  casebook + audits in one commit.
- **Never weaken an audit to pass.** Fix the world/data. (The one exception is syncing an exact-count
  manifest when you deliberately add authored content — see `design/handoff/08` §2.)
- The full verification suite and per-piece "definition of done" are in `design/handoff/08`.
- When unsure about a service change, write Brad a Codex prompt instead of guessing.

Do not stop at a plan. Rebuild the Hold — layout and content — to Brad's standards, one verified
district at a time, until a real non-op multiplayer playthrough reads as a **world the players are
investigating**, not a puzzle game.
