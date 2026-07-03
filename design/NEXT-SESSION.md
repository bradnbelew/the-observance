# THE OBSERVANCE — NEXT SESSION START HERE (the director's handoff)

> 🔴 **You are the director.** Ethan's standing mandate: *"you're the director. figure out what all you
> need to do, pass over, bug fix, integrate, audit, enhance, expand, add more of, remove some of, etc to
> make this amazing, but just literally build it all. this is your /goal. think as a top 1% famous genius
> minecraft integrated arg designer, story writer, lore builder."* The build program is **complete and
> green** — the next session is **playtest-driven tuning + wiring Ethan's real media as it arrives**, not
> a from-scratch build. Do not re-plan; verify, refine, and finish the launch.

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

## 1. CURRENT STATE (2026-07-03) — everything built, all green, NOTHING pushed
Branch `feat/build-everything-2026-07-01`. Every surface green: **plugin jar · discord tsc + 9 selftests ·
dashboard tsc + 2 selftests · datapack JSON.** The full player journey is **code-complete end to end**:
first contact → pure-haunt → earned literacy (the rosetta now TEACHES the alphabet via rune↔plaintext
cribs) → the keeper field + townsfolk → the deep/Seventh lane → the Accepting → **the fate now actually
posts** (the fate sentinel) → **the reckoning is now felt** (the sharp echoes cease / turn to sorrow) →
the keeper-record Hold-Book writes each living player in.

**Shipped since the wave program:** W3 field · W4/W4.2 Observer (Tier-0 behavior + Tier-1 grounded echo +
Tier-2 LLM archivist) · W5 voice tier ("it heard you SAY it") + the spoken-name loop · W6/W7 leave-the-game
surfaces (record site + lure + archive) + found-footage wiring · **W8 cohesion+hardening audit** (fixed the
real blocker: the missing `CoopPlateListener`/`m4-three-hands` producer; the `beat_queue` `failed`-status
deploy hazard) · **W9 journey pass** (fixed the two finale blockers: the ending never posted; the reckoning
had no consequence) · **A3 keeper-record wired** (the last orphan).

## 2. READ FIRST (in order)
1. **`design/LAUNCH-READINESS.md`** — THE operative doc: what shipped, every MANUAL go-live action
   (Ethan's B = media, C = ops), and the honestly-flagged deferred enhancements + their exact missing legs.
2. Memory notes: **`the-observance-completion-push-2026-07-03`** (authoritative latest — full ledger),
   **`the-observance-consistency-principle`** (the lockstep rule), **`the-observance-reshape-mandate`**
   (the design history), **`the-observance-v2-direction`** (the locked direction/invariants).
3. **`design/BUILD-EVERYTHING.md`** — the decisions table + the wave program (all done).
4. **`design/OPENING-RESEQUENCE.md`** (the vibe + field design) + **`design/PUZZLE-DESIGNS.md`** (per-keeper
   specs + adjacency rules) + **`design/CONTENT-GUIDELINE.md`** (Ethan's artifact field guide) when touching
   those areas.

## 3. WHAT'S LEFT (nothing is a code blocker; the spine plays with C alone)
- **C — OPS (Ethan; required to run):** apply migrations (incl. `discord/0009_observations`,
  `dashboard/0009_beat_queue_failed_status`; re-seed); host the resourcepack + set `config.yml` url/sha1
  (**the rune font ships there — the rosetta cribs need it**); `placeworld` the sites incl. the new
  `coop_plate` + the Nether/End lane spots; stage the cold open (`/observance placeprologue`); rotate creds.
- **B — MEDIA (Ethan; optional enrichment):** the found-footage clip + recovered Drive folder + a
  waveform/spectrogram image (feeds `spine-recovered-archive`), and `dashboard/public/the-hold/the-hold.zip`
  (the lure's offline map — don't plant the lure clue until it's hosted). **Wire these when they arrive.**
- **Optional tiers (off by default):** `ANTHROPIC_API_KEY` in the **Render** cron env (Observer Tier-2);
  the voice env + `voice_capture`; `observer_capture`. Flip when ready.
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
# plugin (Java)
cd /d/the-observance/plugin && "D:/_gradle/gradle-8.10.2/bin/gradle" --offline jar -q ; echo "PLUGIN=$?"
# discord (TS + SQL) — the full gauntlet
cd /d/the-observance/discord && npx tsc --noEmit \
  && for s in seedcheck gatecheck specscheck showrunner:test showrunner:test:autonomy \
       showrunner:test:archive showrunner:test:scenario showrunner:test:customs showrunner:test:prologue; \
     do npm run -s "$s"; done
# dashboard
cd /d/the-observance/dashboard && npx tsc --noEmit \
  && npx tsx src/lib/record-projection.selftest.ts && npx tsx src/lib/archive-projection.selftest.ts
# datapack (JSON validity)
cd /d/the-observance/datapack && for f in $(find . -name '*.json' -o -name '*.mcmeta'); do \
  node -e "JSON.parse(require('fs').readFileSync('$f','utf8'))" || echo "INVALID: $f"; done
```
Gradle: `D:/_gradle/gradle-8.10.2/bin/gradle`.

---
**TL;DR:** You're the director; the vibe (§0) is the test; the build is DONE + green (§1). Next session =
**verify live, wire Ethan's media, and tune from a playtest** — not a rebuild. Read LAUNCH-READINESS first,
run the pipeline (§5), keep green (§6), commit at boundaries. Make it a mysterious world you're haunted into
decoding — top-1% quality, nothing half-ready.
