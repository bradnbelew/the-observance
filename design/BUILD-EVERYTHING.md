# THE OBSERVANCE — BUILD EVERYTHING (the program of record)

> **The wave table below (§ status) is stale — W0-W9 are all shipped/green.** Current status lives in
> `LAUNCH-READINESS.md` §1 / `NEXT-SESSION.md`. Kept here for the mandate + wave history.

> 2026-07-02. Ethan's mandate, verbatim: *"you're the director. figure out what all you need to do, pass
> over, bug fix, integrate, audit, enhance, expand, add more of, remove some of, etc to make this amazing,
> but just literally build it all. this is your /goal. think as a top 1% famous genius minecraft integrated
> arg designer, story writer, lore builder."* This doc is the master program: every front, sequenced into
> green waves, orchestrated by the director (subagents on disjoint domains; one plugin agent at a time;
> verify each against live code; keep every check green; commit at clean wave boundaries). Grounded in
> `OPENING-RESEQUENCE.md` (the vibe + audit), `THE-RESHAPE.md` (6 principles), `RESHAPE-RESEARCH.md`
> (evidence), and the WAVE-S build already shipped.

## DECISIONS LOCKED (2026-07-02, with Ethan)
| # | Decision |
|---|---|
| World | **Fresh world** to the players; Ethan CAN pre-place a few hero things before session zero. |
| Haunt their base | **Full latitude** — "do whatever you want to them." Edit their base, per-player, out of sight. |
| Cold-open form | **In-server haunting-first** is the spine; an **off-server "cursed map" vignette + frame-break** is GREEN-LIT if the director wants it (optional flourish, my call). |
| Build model | **MIX:** director code-generates builds (research-driven, cohesive) + re-dresses **vanilla structures** with small additions. Ethan hand-places only a few hero spaces if needed. |
| Pure-haunt phase | **~one session** of wrongness before the first structure is found. |
| Observer | **FULL build** — Tier 1/2: the LLM "AI Director" brain + `0008_observations` + capture (chat/Discord/voice) + grounded weaponization. |
| Voice chat | **YES** — Simple Voice Chat + Whisper + "it heard you say it" — built to actually work + not break. |
| Discord bleed | Director's call — corrupted-artifact leaks on in-game triggers, grounded, made to work. |
| Offshoots | **ALL of them** — tons of content, arcs, stories, lore into the web, integrated with the puzzles/ciphers/mysteries. |
| Townsfolk | Director **designs** their mini-mysteries — meaty + important, not filler. |
| Layers | **Build it all NOW** — spine + secret + community layers, no deferring. |
| Leave-the-game | **YES** — external ARG surfaces to break monotony; Ethan wants a **content-creation guideline** to follow. |
| Found-footage | Director's recommendation on slot; **must actually function + not be too easy**; no HyperFrames draft needed unless useful. |
| Roster | **7 players** now (was 6). Make roster/quorum/co-op **dynamic-N-safe**; fix every fixed-count assumption. **The six keepers / seven ways / the Seventh are LORE — untouched.** |
| Consent | Ethan **has the group's permission** (known-author lens D6: immersion from reactivity + depth, not "is this real"). |

## THE PRINCIPLE (every wave passes this)
Mystery-primary, puzzles submerged, unhandheld-but-fair. Haunt → curiosity → investigation → earned
literacy → theory. A **field, not a corridor**. Difficult but retrace-fair (never punishing). Cohesive:
every mechanic gets its story + clue + interaction in lockstep (the consistency principle). Production
quality; keep it green.

## THE WAVE PROGRAM (sequenced; ▶ = active, ✅ = done, ⬜ = queued)
Ordered so foundations land first and each wave is independently green + committable.

- **W0 — Roster dynamic-N (7-safe).** ⬜ Audit every fixed player-count/quorum assumption; make it relative
  to the active roster (works for 7 and any N). Never touch the six-keepers lore. *(Foundational, focused.)*
- **W1 — The haunting-first cold open (C2).** ⬜ Sequence the built illusions/ambient beats to LEAD session
  one: the base-anomaly hook (discovered out of sight), the per-player "it knows me" escalation, the
  earned-literacy turn. Wire the 4 prologue gaps (IgnitionListener on the base anomaly, lit-marker placement,
  snapshot inputs, ack, `recordOpenedNamed`) + the unwired illusion primitives (dimLight, fog).
- **W2 — Discovered placement (C1).** ▶ Kill the visible placeregion-cluster. DESIGN (2026-07-02): the
  set-pieces are already good post-reshape — change WHERE + HOW-INTEGRATED, not rebuild. (1) **Scatter +
  legible geography:** only the prologue anomaly is near the base; the six keeper sites are far apart +
  terrain-themed (Vaun cave/mineshaft · Mara cliff-ruin · Sella shore/lake · Orin rockface low-lintel ·
  **Brann watch-tower on high ground = a visible distant LANDMARK, Dark-Souls sightline** · Iss cozy-false
  hearth); the deep is DOWN. (2) **Build model = survey-first (lowest risk):** `/observance site set
  <keeper>` lets Ethan walk to a good hidden terrain-fitting spot pre-session; `placeworld` stamps that
  keeper's set-piece there, terrain-following, reveal-safe (auto-scatter only as the unsurveyed fallback);
  vanilla re-dress (ancient city=Undercroft, trial chamber=vault, village=town) for connective structures.
  (3) **Discovery = investigation-gated:** a recovery-compass/lodestone needle toward the earned thread +
  coords hidden in found artifacts/whispers + visible landmarks; salience keeps one thread live. BUILD:
  (a) survey+scatter placement plugin core; (b) verify/wire the discovery-pointer (compass + coord clues).
- **W3 — The field, not a spine (C6).** ▶ MOSTLY SHIPPED (2026-07-02, green on feat/build-everything).
  De-linearization + salience + roster-quorum were verified ALREADY BUILT. Shipped: the **Recovery Archive**
  surfaced (`b31488c` — record-as-a-thread + motif web, spoiler-safe v_archive) · **6-prior-groups**
  (`61578d7`) · found-on-descent + townsfolk archive gates (`e86f4a2`/`4948efa`) · **townsfolk conduct-skin**
  (`cf31d0c` — Pell/Dob react to your conduct) · **relief/exhale beats** (`0ca4a1a`) · **world-drift clock**
  (`8c2801c`) · kept-needle + beacons (`2c709f4`/`e96c1b9`). REMAINING (tasks #4-#7, cross-package): townsfolk
  tracked quests + live rumor-flip · per-player Tier-0 report loop · join-recap "previously on" · Nether/End
  content-completion · gather-card archive gate. Layer delineation lands via the archive (spine cards vs deep).
- **W4 — The full Observer (Tier 1/2).** ⬜ `0008_observations` table; the archivist LLM pass (grounded
  extraction w/ provenance); capture from in-game chat + Discord text; sparse, precise weaponization; the
  consent/opt-out. The "it knows your name / your plan / your words" engine.
- **W5 — Voice chat ("it heard you").** ⬜ Simple Voice Chat + `@discordjs/voice` receiver + Whisper
  transcription → the voice-heard answer_kind + the world reacting to a spoken truth. Built to not break
  (fault-isolated; degrades to silence when absent).
- **W6 — Leave-the-game surfaces + the content guideline.** ⬜ The external ARG surfaces (recovered Drive
  archive · unlisted found-footage · a one-page site · maybe a Google Voice line) + a **CONTENT-GUIDELINE.md**
  Ethan follows when producing real artifacts (what to make, how to hide data, register, spoiler discipline).
- **W7 — The found-footage hero beat.** ⬜ Wire the cross-surface found-footage beat (director's slot +
  payload recommendation, functional + not-too-easy); HyperFrames draft as placeholder → Ethan's real clip.
- **W8 — Full audit / bugfix / enhance / expand / prune.** ⬜ The top-1% polish pass: coherence audit (no
  orphans), difficulty/fairness tuning, remove anything inert, enhance the thin spots, re-verify every
  surface green end-to-end. Ongoing + a final sweep.

## ORCHESTRATION
Disjoint-domain subagents (discord/plugin/datapack/dashboard/lore); **one plugin agent at a time** (Java
serializes); Sonnet for mechanical, Opus for voice/creative/integration + director reasoning. Verify every
agent against LIVE code (reports overstate done-ness). Keep the verify-green gauntlet green after each change
(plugin jar · datapack JSON · discord tsc+7 checks · dashboard tsc+selftest). Commit at clean wave
boundaries. `.env*` stays gitignored. Deferred ops (Ethan): apply new migrations to live Supabase · host the
resourcepack · rotate creds · playtest.

## OPEN SUB-CALLS the director will resolve as it builds (no more blocking)
- Found-footage slot + payload → **recommend the finale lead-in ("the Seventh in the dark") + visible
  coords in one <1s frame + a spectrogram backup** (functional, not-too-easy, two independent paths). Revisit if Ethan objects.
- Off-server cursed-map vignette → build it in W6 as a leave-the-game surface if time allows; not on the critical path.
- Discord bleed intensity → rare, grounded, opt-out-respecting (built in W4 with the Observer).
