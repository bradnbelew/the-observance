# The Observance — Full Rebuild Handoff (for Codex)

> **PHASE 0 OVERRIDE — 2026-07-15.** After `SPINE-LOCK.md`, read `SPINE-CONFORMANCE.md` and
> `PHASE-0-AUTHORITY-AUDIT.md`. Brad approved the conformance statement on 2026-07-15, opening the
> experience-architecture phase; case/evidence implementation, schema changes, Unlit-form implementation,
> live changes, and geometry remain behind later gates. The rebuild targets about six friends, 20–30
> active hours (24–28 target), free-paced over weeks, with subset progress, durable catch-up/replay,
> no time gates or missable substantial content, director-approved hints, and approval-gated risky,
> personalized, or world-changing beats. Old 15-hour, 32-room, 10-case, 82-node, and fixed-media
> statements describe the current implementation, not the rebuild target.

> **Runtime topology is fixed.** The gated `hold.zip` prologue is the only player-facing standalone
> Minecraft save and exists to derive the live IP. After onboarding, every campaign location and beat
> runs on the brother-hosted, Crafty-managed Paper `1.21.11` server. A standalone vertical-slice save is
> private review material for Brad only. Players keep survival gear in protected ARG regions; bypass
> prevention comes from region enforcement, not inventory escrow.

> **PHASE 1 REVIEW ORDER:** `PHASE-1-APPROVAL.md` → `PHASE-1-EXPERIENCE-ARCHITECTURE.md` →
> `PHASE-1-PROGRESSION-GOVERNANCE.md` → `PHASE-1-MEDIA-INVENTORY.md` →
> `PHASE-1-TOPOLOGY-AND-MIGRATION.md`. Phase 1 is complete and awaiting Brad approval; that approval
> opens evidence architecture only, not implementation, live changes, media editing, or geometry.

> **⛔ READ `SPINE-LOCK.md` FIRST.** The ground-up rebuild is approved, but the **spine (the story that
> happened, the shape of the journey, the register) is LOCKED** and is not part of "everything is
> rewriteable." The rebuild deepens the existing story; it does not replace it with a different one.
> `SPINE-LOCK.md` defines exactly what is locked (the canon) vs. what is flesh (every room, puzzle, and
> word — rebuild freely).


This folder is the complete, self-contained brief for the **full rebuild of the Deep Hold** —
**both its physical layout and the puzzle/content inside it** — plus the surrounding polish that
brings the whole ARG up to Brad's standards. It exists because a handoff breaks when the next agent
has to guess. Nothing here should require reading the chat history it came from.

> **The single most important thing to understand.** The prior "V5.1" pass (reawakened Watcher,
> longer finale, de-kiosked Unlit houses, one multiplayer moment) was *polish on top of the existing
> Hold*. Brad's verdict: that is **not** the rebuild. The rebuild is a **ground-up redesign of the
> Hold's room program and the case content**, because the current Hold — 32 oversized rooms
> (~1015 m² average, ~5% furnished, a walk-up mechanism in each) — is itself the problem. You are not
> dressing the current Hold. You are replacing what it is, while keeping the machinery that builds and
> verifies it safely.

## The governing principle (adopted from Codex's review — the top-line test)

> **Every district should have an ordinary reason to exist, every clue should have a reason to be
> there, and every deduction should change what the group believes happened.**

That is the line between a believable investigated world and a beautifully furnished escape room. Apply
it above every other rule below: a room that is dense and pretty but has no *institutional* reason to
exist still fails.

## Adopted amendments (Codex's first-read refinements — binding, they improve this brief)

Codex reviewed this handoff and its assessment sharpened it. These amendments are **adopted** and
override the originals where they conflict:

1. **Replace the plan, don't re-compress it (amends doc 1).** The current `DeepHoldV4Plan.java` is legacy
   V4 coordinates run through `compactX/compactZ` plus per-fixture constructor nudges (`z--`, `z=170`,
   …) — migration scaffolding, not a spatial authority. Author a **coordinate-native** next revision
   with **no** legacy transforms and **no** constructor nudges. Model districts, subrooms, doors/
   thresholds, circulation lanes, furniture volumes, interaction cells/sightlines, gate barriers and
   their closed/open traversal states, and ordinary service/storage/staff circulation.
2. **Visual-approval vertical slice BEFORE district work (amends doc 8).** Sequence is: (a) coarse master
   floorplan + adjacency map → (b) **one exact vertical slice** at a fresh location (Mouth/descent +
   intake + one hallway + one fully-authored room + one layered investigation + one gate + one Watcher/
   asymmetric moment) → (c) **Brad walks it in-game in Adventure mode and approves** → (d) then
   district-by-district. Automation cannot certify scale, atmosphere, or whether a room reads as its
   fiction on sight — and technically-passing Holds have already been visually rejected. Do not build a
   whole district before this approval.
3. **Exact authored compositions, not generic room-type dressers (amends doc 1).** A generic
   `dressLibrary()` produces twelve algorithmic libraries. The data must say "shelf rank C occupies
   these exact cells; desk 2 is here; aisle reserved here," with only small reusable primitives for
   furniture *construction*. Author rooms; don't let the builder invent them.
4. **82 nodes are contract units, not 82 stations (amends doc 2).** Keep the stable `node_id`s and
   `completion_flag`s, but organize play around ~10 memorable **case arcs**. Within a case, some nodes
   are evidence discoveries, some are deductions that re-read earlier evidence, and only a few need an
   explicit mechanical evaluation. The **case conclusion** is the memorable submission — not six
   consecutive micro-submissions. *Guardrail (Brad's bar): this must NOT reduce difficulty or length —
   the removed mechanical stations are replaced by deductive load, enforced by each case's duration
   target (amendment 6). The night stays very hard and ~15 hours.*
5. **Broaden the investigative palette (amends doc 2).** Cross-referencing records cannot become the new
   universal verb (the worked C02 example over-leaned on it). Give each case its own intellectual
   identity from: spatial reconstruction, witness contradiction / NPC knowledge, environmental
   measurement, audio/timing evidence, route tracing, physical provenance, construction-phase
   comparison, remembered social detail, split-party observation, and selective ciphers whose keys arise
   naturally from prior findings. A player should remember "the camp reconstruction" or "the lamp
   chronology," not "the third set of ledgers."
6. **Design whole-night pacing, per case (adds to doc 2).** Before building a case, write its: target
   duration, expected group split, core revelation, required prior knowledge, hint/recovery path,
   submission surface, and emotional function in the 15-hour arc. Otherwise every case is "hard" in
   isolation while the night becomes exhausting and tonally flat.
7. **Density is multi-measure, not a single % (amends doc 1).** "40–70% furnished" is a warning against
   empty rooms, not a target — 70% blocking occupancy makes six-person movement miserable. Track
   separately: blocking-furniture footprint, walkable circulation, non-blocking visual density,
   evidence-bearing surfaces, and purposeful open space. A busy archive looks full without every floor
   cell being impassable.
8. **Multiplayer asymmetry must mean something (amends doc 6).** Per-player-different-view is powerful
   when it says something about the Watcher, identity, memory, or position — not when it's a rune
   fragment in every second district. Don't let asymmetry become another mechanism template.
9. **Build the reachability sim FIRST (corrects doc 4).** It does not exist in the repo yet (only the
   node-progression `simulate_v5_scenarios.py` does). A proven reference — modelling the OLD geometry,
   to be adapted — is committed at `tools/sim_hold_reachability_REFERENCE.py`. Build the faithful
   current one before major geometry work, or the redesign keeps relying on declared graph connectivity
   + costly live builds, which is exactly how self-walling primitives escaped before.
10. **Replace KS01, don't repair it (confirms doc 5).** Rebuild Sella's waterline as a static-evidence
    investigation with an earned reflection reason and a clear bearing submission; remove its
    fresh-frame permutation contract as part of that case change.

**Recommended first move (Codex's, endorsed): produce the experience map + one exact vertical-slice
design for Brad's approval — do NOT begin by rewriting all 32 rooms or all 75 nodes.**

## Read in this order

| # | Doc | What it covers |
|---|-----|----------------|
| 0 | `README.md` (this file) | The invariants that must never break, and how to work. |
| 1 | `01-HOLD-LAYOUT-REBUILD.md` | Redesign the physical Hold: concrete-floorplan-first, rooms that ARE their fiction, dense furnishing, traversability. Worked district example. |
| 2 | `02-HOLD-CONTENT-REBUILD.md` | Rebuild the 10 cases: investigation woven with story, layered difficulty, kill the four-verb monoculture, no naked ciphers, obvious submission. Worked case example. |
| 3 | `03-PROSE-VOICE-MEDIA.md` | The two prose laws (no AI slop, no ARG-mystery-voice), the exact meta-language to purge (file:line), NPC voice fix, book/sign Minecraft formatting limits, media de-branding. |
| 4 | `04-MINECRAFT-SAFETY.md` | What builds safely vs. what breaks; traversability/no-escape rules; the offline reachability sim and build-time assertions that must guard every build. |
| 5 | `05-KS01-BLOCKER.md` | The one hard launch blocker in the *current* build (Sella KS01 fresh-install). Fix it or let the rebuild replace it — either way it must be resolved. |
| 6 | `06-MULTIPLAYER-WATCHER.md` | Required group play (asymmetric views, must-contribute, per-player hallucinations) and how the reawakened Watcher integrates with the new Hold. |
| 7 | `07-SERVICES-DEPLOY.md` | Supabase/Railway/Drive discipline. The predicate-hash sync procedure (the #1 way to desync live). What is yours vs. the plugin operator's. |
| 8 | `08-SEQUENCING-AND-DONE.md` | The exact order to do this in, the verification loop, and the definition of "done" per piece. |

## Brad's binding standards (verbatim intent — every doc inherits these)

These are not suggestions. Judge every piece of work against them *first*, and prefer rewriting
content over adding mechanisms.

1. **Puzzles = investigation woven with story.** Players investigate — books, items, media, the
   website, NPC knowledge, lore callbacks, environmental evidence. **Never** a walk-up mechanism.
   **Never** a naked decoder cipher (key printed beside the ciphertext). A cipher must be *layered*
   with research/lore and its key must be *earned or deduced*.
2. **Two prose laws.** (a) No AI slop — no perfect parallelism, no "not X but Y" tics, no planted
   aphorisms, no rule-of-three portent. (b) No "ultra-mysterious ARG language." Everything reads like
   real human records and conversation. The player is inside a *world*, not "playing an ARG."
3. **Environments match the fiction (hard rule).** Book puzzle → build a real **library**. Industrial
   puzzle → build a real **works floor**. Camp puzzle → build a real **camp**. The evidence is the
   furniture of a believable place, not a station in an empty hall.
4. **No contradictions.** If any text says "there is a line in the hallway," the hallway exists and the
   line is on its floor. Dialogue, lore, books, and the build must all agree.
5. **Structures: concrete floorplan first.** Do **not** plan rooms, layouts, and fillings separately —
   that is how rooms paste over each other and furniture ends up inside walls. One master floorplan on
   one coordinate system, every room/door/corridor/prop cell placed and proven non-overlapping *before*
   building. Traversable for a Minecraft character: doors, hallways, no 2-block jumps to anything
   required, players can't escape / skip / enter or exit where they shouldn't. Every required fixture
   has a reachable standing cell. All key components visible, functional, correctly oriented.
6. **Minimal fragile mechanics.** No complex redstone, no timing-critical contraptions, no scattered
   item-frame gimmicks as the default. Deterministic block reads and simple containers only. Simplicity
   is fine; the **Hold is large-scale**, other structures are regular-sized.
7. **Very hard, long, group.** Difficulty comes from deduction and paying attention, not opaque
   controls. No hand-holding. Group play: multiplayer-contribution puzzles and per-player asymmetric
   hallucinations are wanted. **But** it must be *possible and reasonable* to reach the answer, and
   players must eventually know *where to submit it.*
8. **Strong finale.** Not "uhh, what now?" (Already rebuilt to ~70s in V5.1 — keep it, integrate it.)
9. **Services are Codex's.** Railway (Discord bot), Supabase (DB), Google Drive (media). Any DB/service
   change is done by you (Codex), never from a Claude/plugin session. See doc 7.

## Invariants that must survive the rebuild (do NOT regress these)

The build system and the fairness engineering are the two genuinely excellent things already here.
The rebuild changes *what they build and gate*, never *that they work*.

- **Single Surface Mouth, one-command build.** The whole Hold is placed by `obs placehold prepare /
  plan / build` from one Mouth. Brad values this ("one major structure to place"). Keep it.
- **Tick-batched, restart-safe application.** Geometry applies in bounded batches with a persisted
  checkpoint cursor; a crash mid-build resumes; Paper saves before milestones. Keep it.
- **Build-time standability assertions + offline reachability sim.** Every build proves every room and
  fixture standing cell is reachable from the Mouth *before* it publishes a readiness receipt. This is
  the safety net that caught the corridor-self-walling and water-flooding bugs. Extend it to the new
  layout; never remove it. (Doc 4.)
- **Fairness/recovery engineering.** No-touch completion, atomic wrong-item returns, never-deleted
  evidence, idempotent gates, durable finale state, duplicate-artifact protection. Best-in-class; the
  content rebuild must preserve all of it.
- **The +Z-only Mouth orientation and the compact envelope contract** (or its deliberate, audited
  replacement). If you resize the Hold, update every bound, sim, and asserter together — do not leave
  one authority describing the old footprint (that was a real bug in the old runbook).
- **The predicate-authority hash chain.** `design/ARG-V5-PHYSICAL-PREDICATES.json` → its SHA-256 →
  `settings.v5_physical_authority_sha256` in Supabase → the plugin's node contracts. These four must
  always agree. Changing a puzzle changes the JSON changes the hash — you must re-sync. (Doc 7.) At
  handoff the live hash is `37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b` and the
  committed file still matches it.

## How to work (the anti-drift loop)

1. **Design on the floorplan first, in data.** For the Hold, the "data" layer is
   `plugin/.../structure/DeepHoldV4Plan.java` (rooms/fixtures/gates/links as records) and its
   `validate()`. Redesign the records; make `validate()` prove non-overlap and standing-frame legality
   for the new program *before* you touch the builder.
2. **Build with per-type dressing.** The builder is `DeepHoldV4Geometry.java`. Add dense,
   fiction-specific dressing routines per room type (a `dressLibrary`, `dressArchiveRank`,
   `dressServiceBench`, `dressCamp`, …) instead of the current thin, generic dressing.
3. **Prove it offline, then live.** Run the offline reachability sim (doc 4) after every layout change;
   it takes seconds. Only after it's green do a live cutover build + `obs placehold audit` +
   `obs visualaudit`, then a **restart** + re-audit. A pass before restart is insufficient.
4. **Change content and its authority together.** A puzzle's fiction (books/signs/items/rooms), its
   predicate JSON, the runtime node contract, the Discord/website surface, and the Supabase seed hash
   move as one unit. Never land half of it. (Docs 2 and 7.)
5. **Verify with the real audits, never by weakening them.** The full audit suite is in doc 8. If an
   audit fails, fix the world/data — do not lower the audit. (One legitimate exception exists: when you
   deliberately *add* authored content, you update the exact-count manifest to match; that is a sync,
   not a weakening, and doc 8 names the specific counters.)
6. **Keep it green and commit at boundaries.** Compile once per coherent change, run the self-tests,
   commit a working state before moving to the next room/case.

## What is already done (keep, do not redo)

Committed on `main` as `feat: V5 production rewrite + V5.1 redesign pass`:
- Watcher ambient haunts reawakened safely in production (curated text-free sensory palette + a guarded
  queue enactor). The world is alive again.
- Finale rebuilt to a ~70s event (distance-ordered light-death wave, dripped goodbye, in-register
  titles). Durable/idempotent.
- Node-solve feedback (a chime + "the record takes it.") so solves aren't silent.
- The seven Unlit houses de-kiosked (answers derived from evidence, not printed on the item).
- One asymmetric-multiplayer moment at the Unlit base mirror (per-player observation fragments).
- `/progress` Discord command (tells players what's open and where each is submitted).
- Two real Hold-geometry bug classes fixed (corridor self-walling, water flooding).

These are *polish and infrastructure*. The rebuild below is the substance Brad is asking for.
