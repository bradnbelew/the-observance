# The Observance — Full Rebuild Handoff (for Codex)

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
