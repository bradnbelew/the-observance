# THE OBSERVANCE — NEXT SESSION START HERE

> ⛔ **PAUSE — DO NOT START BUILDING.** ⛔
> Ethan has **new research from Perplexity** to integrate before any more work. When the session opens:
> 1. Briefly confirm you're oriented (read this doc + the canonical set below).
> 2. **STOP and ask Ethan for the Perplexity research.** Do not build, edit, or spawn agents until he
>    pastes it and you've discussed what to fold in.
> 3. Then run his established pipeline (below) on the new research.
>
> The project is in a **fully-built, all-green, live-server-unproven** state. There is no fire to fight.
> The next move is *integrate the new research*, then *playtest*, not *keep building blindly*.

---

## 1. WHERE THINGS STAND (2026-07-01, end of the "build everything" wave)
The haunting layer is BUILT and green across all three layers (plugin jar ~487 KB / 32 beats · discord
×7 test suites · datapack JSON). Full inventory is in the memory note `[[the-observance-v2-direction]]`
(the ★ "build everything" wave entry) — don't re-derive it. Highlights: 65 puzzles + producers · per-
player illusion (name-on-wall, Lens, reflection, fog) · Observer Tier-0 · Wren's companion arc live +
reckoning · the co-op vault · the finale rite · the deep structures (`placedeep`: the future-dated grave
+ the Seventh's chamber) · the Undercroft dimension · the record website (deployable) · DB schema-repair.

**Remaining ceiling (NOT missing features — needs Ethan/infra):** audio (zero OGG, can't generate) ·
Observer Tiers 1–2 (needs hosted bot + Whisper + LLM) · world *arrangement* (set-pieces stamp in flat
rows) + surface-town NPCs · P2 polish (desire-path grave, deep-site choice-marker placement, black-moon
temporal gate, `bases` id-type). Full prioritized backlog: **`design/IMPROVEMENT-AUDIT.md`**.

## 2. CANONICAL DOCS — trust these, ignore the rest
- **`design/RUNBOOK.md`** — the operator's guide (setup → run with friends → admin commands → honest
  status). READ FIRST for "how do we actually play it."
- **`design/IMPROVEMENT-AUDIT.md`** — the 41-row prioritized P0/P1/P2 backlog (some findings already
  fixed — it ran concurrently with builders, so cross-check against live code before acting).
- **`design/CHANGE-MANIFEST.md`** — the decisions ledger (every locked decision + the fold passes).
- The four canon docs: `design/{OVERHAUL,BUILD-PLAN,PUZZLES,INTEGRATION}.md`. Where they disagree with
  code, **the code wins** (they drifted forward — several claimed problems are already fixed).

## 3. THE PIPELINE FOR THE PERPLEXITY RESEARCH (Ethan's established discipline — reuse it)
When Ethan provides the research, do NOT take it at face value:
1. **Expand + triage it** as creative leverage — sort each idea into: sharpens-what-exists (fold) /
   new-cheap-high-leverage (fold) / expensive-park / conflicts-a-locked-decision (surface for a ruling).
   Weigh everything against the sole-builder scope + the two invariants (dynamic-roster, grounding/
   consent). Tell Ethan your read BEFORE integrating. He wants to DECIDE together.
2. **Then his two-pass integration:** (a) a "what to add/change/cut" pass captured in the CHANGE-MANIFEST
   (nothing orphaned) → (b) a cohesion pass → (c) a lore/character/story pass → (d) an integration pass
   that wires every touchpoint (code · plugin · seeds · docs · callbacks) — because a change added in one
   place must update the story that calls back to it, the plugin that makes it work, and the doc players
   find later. THEN build.

## 4. HOW TO BUILD (the pattern that worked this session)
- **Orchestrate with background subagents on DISJOINT domains** (discord / plugin / datapack / arc+design
  / dashboard). **Only ONE plugin agent at a time** (plugin/ Java serializes — concurrent edits collide).
- Every agent's PRIME DIRECTIVE: **keep the checks green / keep it compiling after each change; revert
  anything that goes red.** A green subset beats a broken full set.
- **Director (you) verifies each agent's output independently** — don't trust "it's registered/green"
  reports blindly (this session caught several stale/inaccurate claims by re-checking live code).
- Worker agents on Sonnet for mechanical work, **Opus for delicate voice/creative/integration** + your
  own director reasoning (usage efficiency).

## 5. VERIFY-GREEN COMMANDS (run to confirm the baseline before + after any change)
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
Gradle is at `D:/_gradle/gradle-8.10.2/bin/gradle`. The DB is Braden's Supabase acct
(`fdnmhbpxnodrnbrzrlqq`) — the connected Supabase MCP can't reach it; Ethan applies SQL by hand via
`discord/supabase/apply-tonight.sql`.

## 6. GIT NOTE
Nothing has been committed this session (local edits only, per policy — commit only when Ethan says).
There is a large uncommitted working tree. If Ethan wants a checkpoint, offer to commit before the next
big wave so the research-integration is a clean diff on top.

---
**TL;DR for the fresh session:** read this + RUNBOOK, confirm you're oriented, then **STOP and ask Ethan
for the Perplexity research.** Don't build until it's folded in via his pipeline.
