# THE OBSERVANCE — NEXT SESSION START HERE

> 🔴 **THE MANDATE (2026-07-02, Ethan, verbatim):** *"reshape everything, build everything, and make sure we
> dont skip out on editing anything, the structures, the lore, the ciphers, the web, the integrations. but
> do it on a fresh session."* Scope chosen: **"Deeper redesign"** — also rework puzzle *content* (fewer
> letter-ciphers, more observation / environmental / lateral puzzles), not just structure.
>
> **This IS that fresh session. Build the reshape. Do not ask whether to start — start.**
>
> The whole point of the reshape: playtest-honest verdict was *"it plays like a puzzle GAME, not a
> mysterious WORLD you slowly learn about."* Great research had been filed and NOT applied. This session
> applies it — deeply, everywhere, in lockstep.

---

## 1. READ THESE TWO FIRST (they are the whole brief)
1. **`design/THE-RESHAPE.md`** — THE PLAN. 6 principles (hide the language / world carries the mystery /
   web-not-chain / reward the theory / layered facts / 3-layer architecture) + an appendix folding in all
   deep research, cited to shipped games. This is the spine of the reshape.
2. **`design/RESHAPE-RESEARCH.md`** — THE EVIDENCE LIBRARY. The full cited corpus (~15 games + ARG canon +
   insight psychology + tabletop clue theory + a verified Minecraft-1.21-affordances inventory) distilled
   into actionable techniques with sources. Pull from here so every build decision is grounded, not vibes.

Then skim, for grounding only: `design/RUNBOOK.md` (how it's actually played) and
`design/LAYER-LEDGER.md` (the 236-layer anti-forgetting sweep — the "don't skip anything" checklist).

## 2. THE RESHAPE, CONCRETELY — what "reshape everything" touches (all five, in lockstep)
Ethan named five surfaces explicitly. None may be skipped; a change in one MUST update the others.
- **STRUCTURES** (plugin `StructureTemplates.java` + placement). Today's set-pieces stamp as legible
  "here-is-a-puzzle" stones in flat rows. Reshape → the world carries the mystery: decay-as-clock (copper
  oxidation / sculk), the significant *absence*, placement-as-grammar, the deliberate contradiction as the
  load-bearing clue. Hide the "this is a puzzle" framing. (See RESHAPE-RESEARCH §3, §4, §8.)
- **LORE** (`design/OVERHAUL.md` canon + keeper voices + the record). Layered facts
  (confirmed/implied/misleading/false); the motif that makes distant things rhyme; silence-is-information.
  Every mechanical change gets its story callback (the lockstep/consistency principle —
  `[[the-observance-consistency-principle]]`).
- **CIPHERS → PUZZLES** (`design/PUZZLES.md` + oracle + datapack). THE biggest content shift: **fewer
  letter-ciphers.** Hide the alphabet and make literacy *earned* (Tunic/Fez); convert lookup-ciphers into
  observation / environmental / lateral puzzles that resolve to an *action* using universal competence, not
  an invented alphabet. Keep every leap retrace-fair (moon-logic test). Build a redundant WEB (≥3 clues
  per conclusion, Three-Clue-Rule), not the current chain. Reward the theory, not the lookup.
  (RESHAPE-RESEARCH §1, §2, §5, §6.)
- **WEB** (`dashboard/` — /record, /author, /status). The record must reflect the new fact-layering
  (redaction = confirmed vs implied), the new web (not a linear un-redact), and stay in sync with the arc.
- **INTEGRATIONS** (discord bot + showrunner cron + the flag graph + the oracle normalizer). The
  cross-surface oracle (Java ↔ TS same normalizer), `requires_flags`/`set_flags` graph, showrunner drip,
  and per-player illusion primitives (Paper `sendBlockChange`/`hideEntity`) must all move with the redesign.

## 3. THE PIPELINE (Ethan's established discipline — DO NOT skip; this is how nothing gets orphaned)
Two-pass integration, every touchpoint, in this order — captured in `design/CHANGE-MANIFEST.md`:
1. **Add/change/cut pass** → write it into CHANGE-MANIFEST (nothing orphaned; every old layer either
   migrates, is rebuilt, or is explicitly cut with a reason). Cross-check against the LAYER-LEDGER so no
   planned layer silently drops.
2. **Cohesion pass** — do the pieces still form one world? Audit for orphaned mechanics on stale ARG state.
3. **Lore / character / story pass** — every changed mechanic gets its callback in the fiction + the record.
4. **Integration pass** — wire EVERY touchpoint: plugin · datapack · discord · showrunner · oracle · seeds
   · website · docs · player-facing callbacks. A change added in one place updates the story that calls
   back to it, the plugin that makes it work, and the doc/record the player finds later.
5. **THEN build** — and keep it green after each change.

## 4. HOW TO BUILD (the orchestration pattern that worked)
- Background subagents on **DISJOINT domains** (discord / plugin / datapack / arc+lore / dashboard).
  **Only ONE plugin agent at a time** — `plugin/` Java serializes; concurrent edits collide.
- Every agent's PRIME DIRECTIVE: **keep it compiling / keep checks green after each change; revert red.**
- **You (director) verify each agent's output against live code** — reports overstate done-ness; re-check.
- Sonnet for mechanical work; **Opus for voice / creative / integration** + your own director reasoning.
- **META-LESSON (proven repeatedly):** `IMPROVEMENT-AUDIT.md` and seed comments **OVERSTATE brokenness.**
  Verify each "broken" claim against live code before acting — many are already fixed. Trust the code.

## 5. VERIFY-GREEN COMMANDS (baseline before + after every change)
```
# plugin (Java)
cd /d/the-observance/plugin && "D:/_gradle/gradle-8.10.2/bin/gradle" --offline jar -q

# discord (TS + SQL) — the full gauntlet
cd /d/the-observance/discord && npx tsc --noEmit \
  && for s in seedcheck gatecheck specscheck showrunner:test showrunner:test:scenario \
       showrunner:test:customs showrunner:test:autonomy; do npm run -s "$s"; done

# datapack (JSON validity)
cd /d/the-observance/datapack && for f in $(find . -name '*.json' -o -name '*.mcmeta'); do \
  node -e "JSON.parse(require('fs').readFileSync('$f','utf8'))" || echo "INVALID: $f"; done
```
Gradle: `D:/_gradle/gradle-8.10.2/bin/gradle`.

## 6. CURRENT STATE — what the reshape starts FROM (all live + green as of 2026-07-02)
The full stack is DEPLOYED and working — the reshape edits a live, green baseline, not a broken one:
- **Plugin 0.2.2** on the server (terrain-following placement fixed; answer-sign reachability fixed).
- **Resourcepack** (rune font + 4 ambient sounds) + **datapack** on the server.
- **Discord bot + showrunner cron** on Railway (Node-22 + ws polyfill fixed the crash-loop; `tsx` in deps).
- **The Record website** on Vercel (/record public · /author + /status admin-gated).
- Env vars all set on the platforms. `.env` / `.env.local` gitignored (never committed).

**Known-not-yet-active (don't mistake for reshape scope — these are pre-existing carry-overs):**
- Keeper voices (6 OGGs) built in source — need a **resourcepack re-host** (new zip + sha1) to be heard.
- `the_threshold` deep-site answer sign — needs the crouch-corridor redesign (deferred; fold into reshape).
- Observer Tiers 1–2 (chat/voice "it knows your name") — scope decisions (LLM brain + `0008_observations`).
- **Rotate the exposed credentials** (service_role key + Discord bot token were pasted in chat earlier) once
  everything's stable: Supabase Settings→API roll service_role; Discord Bot→Reset Token. Then update the
  Railway/Vercel env vars. Not a blocker for building, but do it before any wider playtest.

## 7. GIT
On `feat/build-everything-2026-07-01`. Reshape docs committed (515cb4b + the RESHAPE-RESEARCH commit).
Commit in clean waves per the pipeline (add/change/cut → cohesion → lore → integration → build), each
green. Commit only at wave boundaries, not mid-edit. `.env*` stays gitignored.

---
**TL;DR for the fresh session:** you are cleared to build the deep reshape NOW. Read `THE-RESHAPE.md` +
`RESHAPE-RESEARCH.md`, run the add/change/cut pass into `CHANGE-MANIFEST.md` (touching all five surfaces —
structures, lore, ciphers→puzzles, web, integrations, nothing orphaned), then build in green waves via the
pipeline. The world should stop announcing "here is a puzzle" and start being a mystery you slowly learn.
