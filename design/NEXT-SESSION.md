# THE OBSERVANCE — NEXT SESSION START HERE (the director's handoff)

> 🔴 **You are the director.** Ethan's standing mandate (verbatim): *"you're the director. figure out what
> all you need to do, pass over, bug fix, integrate, audit, enhance, expand, add more of, remove some of,
> etc to make this amazing, but just literally build it all. this is your /goal. think as a top 1% famous
> genius minecraft integrated arg designer, story writer, lore builder."* The front of the experience is
> built + green (W0–W2). **Resume the build program at W3.** Do not re-plan from scratch — the plan is
> written; execute it.

---

## 0. THE VIBE — the test every decision must pass (Ethan's ideals, non-negotiable)
It must play like a **mysterious WORLD you're slowly haunted into investigating**, NOT "walk up to a
structure → oh, a puzzle." The felt arc: **haunt (something's wrong / watching) → curiosity → dig & connect
dots → earned literacy → build a theory → find coordinates → go there → figure it out.** Unhandheld but
**retrace-fair** (never punishing / moon-logic). Puzzles + ciphers exist but are **SUBMERGED** — discovered,
never announced. It is a **FIELD of parallel threads, not a corridor** (keeper stones → finale → end is the
thing we're killing). Difficult is good; stale/repetitive/impossible/nonsensical/punishing is bad. We want:
cool, unique, beautifully made, consistent, **not broken, production quality**, lots of variety, weeks-long
even with **7 players**, tons of story/lore/NPC-interaction, spooky, unsettling, emotion-inducing.

## 1. READ FIRST (in order — the whole brief)
1. **`design/BUILD-EVERYTHING.md`** — THE PROGRAM OF RECORD: the locked decisions table + the wave plan
   W0–W8 + orchestration rules. This is your operative doc.
2. **`design/OPENING-RESEQUENCE.md`** — the vibe audit + the haunting-first cold open (beat by beat) + §4B
   **the arc as a FIELD not a spine** (the offshoots/parallel/in-between design — this IS W3) + the artifact
   shot-list.
3. **`design/CONTENT-GUIDELINE.md`** — Ethan's artifact field guide (voice, spoiler+fairness laws, data-hiding
   toolbox, §6b the discovery workflow, the found-footage spec). Ethan produces the real artifacts; you wire them.
4. **`design/THE-RESHAPE.md`** (the 6 principles) + **`design/RESHAPE-RESEARCH.md`** (the cited evidence:
   ~15 games + ARG canon + MC-1.21 affordances + puzzle/cipher variety + fairness rules). Build from these.
5. Memory notes: **`the-observance-reshape-mandate`** (the running log — full decision history + progress),
   **`the-observance-consistency-principle`** (the lockstep rule).

## 2. CURRENT STATE (all committed green on `feat/build-everything-2026-07-01`; nothing pushed)
The whole **reshape** shipped (S-A→S-H + dovetails: earned literacy, label cull, cipher-as-characterization,
batch-confirm, salience drip, townsfolk NPCs, record-by-theory, roster-quorum). Then the build-everything
program: **W0** roster→dynamic-7 `3779cd8` (fixed a real finale-quorum bug) · **W1** haunting-first cold open
`eb6caa6` (base-anomaly discovered-out-of-sight → ignition, both surfaces; ProximityDimBeat) · **W2a** survey
+ scatter placement `46c2006` (`/observance site set` + `placeworld`, sites scatter terrain-integrated) ·
**W2b** discovery model `e74d90d` (coords-in-artifacts = Ethan's workflow, documented).

**Owed small builds from W2:** ✅ DONE (`2c709f4`) — legible-geography beacons (Brann watch-fire +
unbroken light project a kept-light beam) + the kept-needle recovery-compass (admin path live; the earned
seventh_named auto-grant is tracked task #3).

**W3 progress (2026-07-02):** ✅ **W3a `b31488c`** surfaced the 42-card Recovery Archive — it was authored
+ voice-covered + never read. `0007_v_archive.sql` (SECURITY DEFINER, reveal-gated in SQL) + showrunner
materializer (INV-1 intact) + the `/record/archive` reading-room (5 threads, citation web = the motif
rhymes, filtered to the revealed set). Closes record-as-a-thread + motif rhymes + layer-delineation in one
build. De-linearization/salience/roster-quorum were verified ALREADY BUILT (only a stale TODO fixed).
**Remaining W3 = tracked tasks #3–#7** (townsfolk mini-arcs · 6-prior-groups drip + per-player Tier-0 loop ·
in-between tissue + null-gate card gates · Nether/End content). Deferred op: apply 0007_v_archive.sql to
live Supabase + `npm run archive:materialize` at deploy.

## 3. THE DECISIONS LOCKED (see BUILD-EVERYTHING for the full table — highlights)
Fresh world (Ethan can pre-place hero things) · full latitude to haunt their base · haunting-first in-server
open (off-server cursed-map optional) · MIX build model (director code-gen + vanilla re-dress; survey-first) ·
~1 session pure-haunt before first structure · **FULL Observer Tier 1/2** (LLM brain + `0008_observations` +
chat/discord/voice capture) · **voice chat YES** (Simple Voice Chat + Whisper) · **ALL offshoots + tons of
content/lore integrated w/ puzzles** · townsfolk get meaty mini-mysteries (you design) · build all layers NOW
· leave-the-game surfaces YES (+ the content guideline) · found-footage first hero artifact (Seventh-in-dark,
coords in one frame + spectrogram) · **7 players — roster/quorum/co-op is DYNAMIC-N; the six keepers / seven
ways / the Seventh are LORE, DO NOT TOUCH** · Ethan has the group's permission (known-author lens).

## 4. RESUME HERE — the remaining wave program
- **W3 — the field + ALL offshoots (BIG; §4B of OPENING-RESEQUENCE is the design).** De-linearize the six
  keepers into a parallel field (hard "requires all 5" gates → salience boosts; salience picker already
  built). Build the offshoots WITH MEAT, each integrated with puzzles/ciphers/lore + given ≥3-clue web hooks
  + a story callback (consistency principle): townsfolk secret mini-arcs (Aro-the-liar / Old-Pell-remembers /
  …, you design) · the record-as-a-thread (the falsified-entry/herring investigation) · the 6-prior-groups
  slow-burn · the Nether origin + End exile deep lanes · per-player emergent threads (Observer-spun) · the
  in-between tissue (motif rhymes · world-drift clocks: copper oxidation/sculk spread · "previously on" recaps
  · relief/exhale beats). Delineate the 3 layers (spine / secret / community) so depth is opt-in.
- **W4 — the full Observer (Tier 1/2).** `0008_observations` migration · the archivist LLM pass (grounded
  extraction w/ provenance, Claude Agent SDK) · capture from in-game chat + Discord text · sparse precise
  weaponization · consent/opt-out. The "it knows your name / plan / words" engine. Grounded-only; degrade to
  silence, never fabricate.
- **W5 — voice chat ("it heard you").** Simple Voice Chat + `@discordjs/voice` receiver + Whisper → the
  voice-heard answer_kind + the world reacting to a spoken truth. Fault-isolated; degrades to silence when absent.
- **W6 — leave-the-game surfaces.** External ARG surfaces (recovered Drive archive · unlisted found-footage ·
  a one-page site · maybe a Google Voice line) to break monotony. Guideline already written.
- **W7 — the found-footage hero beat.** Wire the cross-surface found-footage beat (HyperFrames draft →
  Ethan's real clip). Recommended: the Seventh in the dark; coords in one <1s frame + the audio spectrogram.
- **W8 — full audit / bugfix / enhance / expand / prune.** The top-1% polish: coherence audit (no orphans),
  difficulty/fairness tuning, remove inert content, enhance thin spots, re-verify every surface green.

## 5. THE PIPELINE + PASSES (Ethan's discipline — run it every wave; this is how nothing gets orphaned)
For any substantive change, don't ship in isolation — trace ALL layers in lockstep:
1. **Add/change/cut pass** — decide what's added/changed/cut; cross-check nothing is orphaned.
2. **Cohesion pass** — do the pieces still form ONE world? Audit for orphaned mechanics on stale ARG state
   (`the-observance-consistency-principle`): no feature-with-no-story, cipher-with-no-puzzle,
   puzzle-with-no-teachable-key, detection-with-no-report, structure-referenced-but-never-authored. Inert
   content must read honestly as flavor, never costume itself as a puzzle. (Pure lore/backstory is WELCOME —
   "gathering info even if you don't know it's relevant is cool" — as long as it reads as what it is.)
3. **Lore/story pass** — every changed mechanic gets its callback in the fiction + the record.
4. **Integration pass** — wire EVERY touchpoint (plugin · datapack · discord · showrunner · oracle · seeds ·
   website · docs · player-facing callbacks). A change in one place updates the story that calls back to it,
   the plugin that makes it work, and the doc/record the player finds later.
5. **Critique passes** — before/after building, run adversarial critique (is it retrace-fair? does it announce
   itself as a puzzle? is it moon-logic? is it inert filler? does it break the vibe? is it too easy/hard?).
6. **THEN build** — in green waves, green after each change, commit at clean wave boundaries.

## 6. HOW TO BUILD (orchestration — proven this project)
- Background subagents on **DISJOINT domains** (discord / plugin / datapack / dashboard / lore). **Only ONE
  plugin agent at a time** (Java serializes). Sonnet for mechanical work; **Opus for voice/creative/integration**
  + your own director reasoning.
- **You (director) verify each agent's output against LIVE code** — reports overstate done-ness; re-check.
- **META-LESSON, proven repeatedly:** the design/audit docs + seed comments **OVERSTATE brokenness** — many
  "gaps" are already built (IgnitionListener, S-H payoffs, the co-op partition, etc.). **Verify live before
  acting; trust the code.**
- Keep it green after every change; commit only at clean wave boundaries; **`.env*` stays gitignored**;
  never commit build-artifact zips.

## 7. VERIFY-GREEN (baseline before + after every change)
```
# plugin (Java)
cd /d/the-observance/plugin && "D:/_gradle/gradle-8.10.2/bin/gradle" --offline jar -q ; echo "PLUGIN=$?"
# discord (TS + SQL) — the full gauntlet
cd /d/the-observance/discord && npx tsc --noEmit \
  && for s in seedcheck gatecheck specscheck showrunner:test showrunner:test:scenario \
       showrunner:test:customs showrunner:test:autonomy; do npm run -s "$s"; done ; echo "DISCORD=$?"
# dashboard
cd /d/the-observance/dashboard && npx tsc --noEmit && npx tsx src/lib/record-projection.selftest.ts
# datapack (JSON validity)
cd /d/the-observance/datapack && for f in $(find . -name '*.json' -o -name '*.mcmeta'); do \
  node -e "JSON.parse(require('fs').readFileSync('$f','utf8'))" || echo "INVALID: $f"; done
```
Gradle: `D:/_gradle/gradle-8.10.2/bin/gradle`.

## 8. WHAT ETHAN IS DOING IN PARALLEL (don't block on it)
Making the real hero artifacts per `CONTENT-GUIDELINE.md` — first the **found-footage** (Seventh in the dark;
coords hidden in one frame + audio spectrogram) + a **found journal**. He surveys site spots + can pre-place
a few hero builds. He has the group's permission (known-author lens). Wire his artifacts when they arrive.

## 9. DEFERRED OPS (Ethan's, not build) — remind at a playtest boundary
Host the rebuilt resourcepack (`observance-resourcepack.zip`, sha1 `e2e30b7aa290d2a7fa943f3cb6c3e5b1f8048e40`,
set `config.yml resource-pack.url`+`sha1`) · rotate the exposed service_role + Discord bot creds · (migrations
0006_v_record_theories + 0008_requires_quorum already applied) · run the first vertical-slice playtest.

---
**TL;DR:** You're the director; the vibe (§0) is the test; the plan (BUILD-EVERYTHING) is written; **resume at
W3** (the field + all offshoots, §4B). Run the pipeline (§5), orchestrate disjoint agents (§6), verify live,
keep green, commit at boundaries. Make it a mysterious world you're haunted into decoding — top-1% quality.
