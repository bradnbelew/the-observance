# THE OBSERVANCE — NEXT SESSION START HERE (the director's handoff)

> 🔴 **You are the director.** Ethan's standing mandate: *"you're the director. figure out what all you
> need to do, pass over, bug fix, integrate, audit, enhance, expand, add more of, remove some of, etc to
> make this amazing, but just literally build it all. this is your /goal. think as a top 1% famous genius
> minecraft integrated arg designer, story writer, lore builder."* The build program is **complete and
> green** — the next session is **playtest-driven tuning + wiring Ethan's real media as it arrives**, not
> a from-scratch build. Do not re-plan; verify, refine, and finish the launch.

> **CURRENT STATUS OVERRIDE (2026-07-06):** this handoff contains historical "complete and green" language
> from an earlier repo-only pass. The current launch verdict is **not launch-ready** until
> `tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv -RehearsalPacket <packet-dir>`
> passes and the live Paper server/client attestations in `design/RUNBOOK.md` are completed. Known blockers
> include the hosted resource-pack URL/SHA1, 67 launch-required placeholder site coordinates, coordinate
> proof capture, completed live rehearsal packet, and in-client verification of books/signs/lore/runes/sounds.
> Use `design/CURRENT-READINESS-VERDICT.md` for the current verdict and `design/MANUAL-LAUNCH-PLAN.md` as
> the ordered handoff for closing those blockers.

---

## 0. THE VIBE — the test every decision must pass (Ethan's ideals, non-negotiable)
It must play like a **mysterious WORLD you're slowly haunted into investigating**, NOT "walk up to a
structure → oh, a puzzle." The felt arc: **haunt (something's wrong / watching) → curiosity → dig &
connect dots → earned literacy → build a theory → find coordinates → go there → figure it out.**
Unhandheld but **retrace-fair** (never punishing / moon-logic). Puzzles + ciphers exist but are
**SUBMERGED** — discovered, never announced. It is a **FIELD of parallel threads, not a corridor**. We
want: cool, unique, beautifully made, consistent, **not broken, production quality**, lots of variety,
weeks-long even with **7 players**, tons of story/lore/NPC-interaction, spooky, unsettling,
emotion-inducing. **No half-ready anything** — every mechanic has its story + clue + interaction in
lockstep (the consistency principle); inert content reads honestly as flavor, never costumes as a puzzle.

## 0b. 2026-07-03 PM — audit + playtest pass (read `LAUNCH-READINESS.md §5` for the full ledger)
A from-scratch adversarial audit (not trusting this doc's prior "verified sound" claims) found and fixed
two real bugs, committed on this branch: **the resource-pack PUSH half was never registered** (launch-
blocking — the go-live "host the pack" step would have done nothing), and **the curatorial drip pool
didn't apply the reveal-gate** (`requires_flags`), so a flag-gated puzzle could be hinted before it was
solvable and then never re-announced once it opened. A manual playtest trace (cold-open → the M4/Iss
chain → finale → companion reckoning) found the authored content and pacing hold up well — no new
fairness bugs beyond the drip-gate fix. Also surfaced: the Unlit Deep group-custom was never built at all
(bigger than a wiring gap — a real feature), and the `keeper.ts` deferred item is wider in scope than
previously stated (the plugin-side NPC listener + entity-tagging producer are also missing, not just the
TS resolver). **2026-07-05: Unlit Deep BUILT** (Ethan approved it) — see `LAUNCH-READINESS.md §5` for the
full ledger (detector + group-latch flag + the Discord report, KEPT-side + the fire's visual glow honestly
deferred). `keeper.ts`'s NPC-rhyme beat itself remains correctly deferred — but the wider plugin-side gap
noted below it (the listener never registered, no entity ever tagged) was closed the next session; see 0c.

## 0c. 2026-07-05 — cohesion & freshness pass (read `design/COHESION-FRESHNESS-AUDIT.md` for the full
ledger). A from-scratch audit of story/lore, puzzles/mechanics, visuals/audio, and tech integration (five
parallel fresh-eyes passes + a sixth mining every prior cohesion audit for recurring complaints) found and
closed: the Watcher-identity story fork (the six keepers are now canon as the Seventh's own mind, fractured
into six roles to survive the isolation — not six separate people who joined or were absorbed;
`canon-spine.md`/`WORLD-BIBLE.md`/`FINALE-THE-RELEASE.md` reconciled), the keeper-NPC interaction gap
(rediscovered broken 4x across this project's history — `KeeperNpcListener` now registered + a real
`KeeperNpc.java` body/tagger built, `/observance keeper <spawn|despawn> [node]` wired), Sella's redundant
Atbash + 3x destination-word repeat, Brann's stalled cipher, Iss's faked cipher duality, Wren's off-voice
dialogue, the off-brand bot sigil, and a spreading Unicode-rune leak on the dashboard. Two systemic guardrail
scripts (`tools/check_namespace_collisions.py`, `tools/check_voice_register.py`, wired into `npm run audit`)
close two chronic recurring failure modes at the root instead of by hand next time. Full verify-green pipeline
passed after every change. **Deliberately deferred** (scoped, not forgotten — see the audit doc's "FIXES
SHIPPED" section): the UNKEPT vs. AVERYN acrostic differentiation (needs sealed canon + live plugin + seed
touched together) and a physical-detector fix for Iss's monotone catch-sequence pacing (needs new Java).
**Still blocked on Ethan, unchanged:** the physical world-build (§3 C below) and audio-by-ear verification of
the keeper voices.

## 1. HISTORICAL STATE (2026-07-03) - repo build was green, live launch was still pending
Branch `feat/build-everything-2026-07-01`. Every surface green: **one-button root audit · Discord
story/data/runtime checks · dashboard selftests/build · plugin jar · datapack/resourcepack JSON.** The
full player journey is **code-complete end to end**:
first contact → pure-haunt → earned literacy (the rosetta now TEACHES the alphabet via rune↔plaintext
cribs) → the keeper field + townsfolk → the deep/Seventh lane → the Accepting → **the fate now actually
posts** (the fate sentinel) → **the reckoning is now felt** (the sharp echoes cease / turn to sorrow) →
the keeper-record Hold-Book writes each living player in.

**Shipped since the wave program:** W3 field · W4/W4.2 Observer (Tier-0 behavior + Tier-1 grounded echo +
Tier-2 LLM archivist) · W5 voice tier ("it heard you SAY it") + the spoken-name loop · W6/W7 leave-the-game
surfaces (record site + lure + archive) + found-footage wiring · **W8 cohesion+hardening audit** (fixed the
real blocker: the formerly missing `CoopPlateListener`/`m4-three-hands` producer is now guarded; the `beat_queue` `failed`-status
deploy hazard) · **W9 journey pass** (fixed the two finale blockers: the ending never posted; the reckoning
had no consequence) · **A3 keeper-record wired** (the last orphan).

## 2. READ FIRST (in order)
1. **`design/LAUNCH-READINESS.md`** — THE operative doc: what shipped, every MANUAL go-live action
   (Ethan's B = media, C = ops), and the honestly-flagged deferred enhancements + their exact missing legs.
2. **`design/EXPERIENCE-MANIFEST.md`** — THE player-facing director map: canonical order, side-story
   lanes, NPCs, media, archive threads, and what counts as live vs. placement/media/ops-required.
3. **`design/OPERATOR-LIVE-CONTROLS.md`** — THE operator-safe control guide: what config actually affects
   launch, what is spec-only, and which manual steps remain.
4. **`design/PERSONAL-PLAYTEST-SCRIPT.md`** — Ethan's pre-launch solo route: first hour, literacy, record
   elsewhere, side-story sampling, Wren, Iss, final name, release, and media-slot checks.
5. Memory notes: **`the-observance-completion-push-2026-07-03`** (authoritative latest — full ledger),
   **`the-observance-consistency-principle`** (the lockstep rule), **`the-observance-reshape-mandate`**
   (the design history), **`the-observance-v2-direction`** (the locked direction/invariants).
6. **`design/BUILD-EVERYTHING.md`** — the decisions table + the wave program (all done).
7. **`design/OPENING-RESEQUENCE.md`** (the vibe + field design) + **`design/PUZZLE-DESIGNS.md`** (per-keeper
   specs + adjacency rules) + **`design/CONTENT-GUIDELINE.md`** (Ethan's artifact field guide) when touching
   those areas.

## 3. WHAT'S LEFT (no known code blocker; live ops still gate launch)
- **C — OPS (Ethan; required to run):** regenerate and apply `discord/supabase/apply-all.sql`;
  host the resourcepack + set `config.yml` url/sha1
  (**the rune font ships there — the rosetta cribs need it**); `placeworld` the sites incl. the new
  `coop_plate` + the Nether/End lane spots; replace all 67 launch-required placeholder site coordinates;
  fill and validate the coordinate proof CSV; complete the live rehearsal packet; stage the cold open
  (`/observance placeprologue`); rotate creds.
- **B — MEDIA (Ethan; optional enrichment):** the found-footage clip + recovered Drive folder + a
  waveform/spectrogram image (feeds `spine-recovered-archive`), and `dashboard/public/the-hold/the-hold.zip`
  (the lure's offline map — don't plant the lure clue until it's hosted). **Wire these when they arrive.**
- **Optional tiers (off by default):** `ANTHROPIC_API_KEY` in the **Render** cron env (Observer Tier-2);
  the voice env + `voice_capture`; `observer_capture`. Flip only after session-zero consent is handled.
- **Deferred enhancements (flagged in LAUNCH-READINESS §3, NOT half-shipped):** `keeper.ts` NPC-rhyme beat
  (low value — keeper-record already delivers the rhyme); REFUSERS ending (Ethan decided OUT); more
  six-prior-groups / diverse-puzzle archive cards (pure content); a producer-coverage build guardrail.

## 4. THE MOST LIKELY NEXT MOVES (pick by what Ethan brings)
- **Playtest-driven tuning (the real next gate).** Run a vertical slice; watch for difficulty/fairness,
  pacing, and any beat that doesn't land; tune constants + prose, not architecture. This is where the
  remaining quality lives.
- **Wire Ethan's media** as he produces it (found-footage coords/spectrogram; the-hold.zip; Drive folder).
- **Optional enhancements** only if Ethan asks (the deferred list).

## 5. THE DISCIPLINE (how nothing gets orphaned or broken)
- **Verify LIVE before acting — the docs/audits OVERSTATE brokenness** (proven again this session: many
  "gaps" were already built; the real ones were missing *wiring legs*). Trust the code, not the doc.
- **Run the pipeline for any change:** add/change/cut → cohesion (no orphans) → lore/story callback →
  integration (wire EVERY touchpoint: plugin · datapack · discord · showrunner · oracle · seeds · website ·
  docs) → adversarial critique → THEN build.
- **Keep it green after every change; commit at clean boundaries.** `.env*` stays gitignored; never commit
  build-artifact zips.
- **Orchestrate disjoint-domain subagents** (discord/plugin/dashboard/lore) for breadth; **one plugin agent
  at a time** (Java serializes); Opus for creative/integration + director reasoning; **verify every agent's
  output against live code.**

## 6. VERIFY-GREEN (baseline before + after every change)
```
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv -RehearsalPacket <packet-dir>
cd plugin
.\gradlew.bat test build
```
The first command verifies repo automation. The second is the launch go/no-go gate and is expected to fail
until the manual blockers are closed. The Gradle wrapper is checked in so a local Gradle install is not
required.

---
**TL;DR:** You're the director; the vibe (§0) is the test; the repo automation is green, but launch is blocked
until the manual go/no-go gate passes. Next session = **verify live, close the manual blockers, wire Ethan's
media, and tune from a playtest** — not a rebuild. Read LAUNCH-READINESS first, run the pipeline (§5), keep
green (§6), commit at boundaries. Make it a mysterious world you're haunted into decoding — top-1% quality,
nothing half-ready.
