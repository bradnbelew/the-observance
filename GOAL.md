# The Observance — Build Goals (autonomous `/loop`)

> North star: **"From The Fog, but it knows your name."**
> A server-side Paper plugin + Discord bot + Supabase + dashboard running a slow-burn,
> soft-pressure, personalized horror/mystery ARG for a veteran friend group.

## The three bars every decision is measured against
1. **YouTube-ARG-video worthy** — the scripted-Minecraft-mystery genre (Wifies-style format),
   but ours is **autonomous & reactive, not scripted** — that authenticity is the differentiator.
2. **Friend-group worthy** — these specific veterans love **hard** ARGs; obsessive, not frustrating.
3. **ARG-critic worthy** — a real ARG community would respect the craft.

## Hard constraints (never violate)
- **Path A, server-side**: friends install nothing but ONE auto-pushed resource pack (one click).
  Shaders / Fabric mods are Ethan's solo B-roll only.
- **Anti-jank contract** (`DESIGN.md`): engine never generates geometry/coords — only pastes curated
  schematics + fires curated, validated beats. LLM = rare text-only scalpel with a deterministic
  fallback behind every call. Main-thread world writes. Fault isolation around every listener.
- **Difficulty = HARD, NON-LINEAR WEB** — many puzzles open at once, varied payoffs, multiple paths,
  red herrings, no countable step ladder.
- **Privacy = full profiling** ("it knows me" max); only a dashboard master kill-switch.
- **Showrunner = AI**, AUTO⇄CONFIRM toggle. Player-earned beats fire immediately (`approved`);
  curatorial beats wait for dashboard approval (`pending`).
- **Collective ending**, but never punish the group for an absent member — gate on ACTIVE players.
- **Nothing breaks.** Cross-surface consistency: Minecraft ⇄ Discord ⇄ dashboard never contradict.

## Workstreams (the goals)
- [x] **Finish unfinished oracle work** — surface-parity hardening + approval-gate fix (committed `c23792a`, `44eed92`).
- [ ] **Atmosphere stack** — resource pack, MythicMobs+ModelEngine custom 3D monsters, FAWE schematics, ambient sound, recording-only shaders. (research workflow in flight)
- [ ] **Cipher web** — >6 puzzle types, non-linear graph, varied payoffs, fix `coordEncode` Rosetta.
- [ ] **Monsters-in-story** — custom creatures = the rejected past groups / prior keepers; tied to the arc.
- [ ] **Build the missing pieces** — load-bearing prose, full showrunner, endgame-via-beats + active-player quorum, cheap customs (2/10 done), runtime test on a local Paper server, rune legibility.
- [ ] **Get everything live** — apply `0004_oracle.sql`, dashboard approve/skip live test, Render deploy, Discord server config.

## Manual gates (Ethan-only — I cannot do these)
- **Apply `0004_oracle.sql`** to Supabase `fdnmhbpxnodrnbrzrlqq` — the connected MCP is the Voxaris org
  (no permission on Braden's project). Needs Braden's Supabase PAT, or paste the (idempotent) SQL into
  the Supabase SQL editor.
- **Discord server config** → `discord/.env` (guild/app/channel IDs given; bot token).
- **Render deploy** (bot Background Worker + showrunner Cron).
- **Rotate secrets** pasted in old chat (Anthropic key, Supabase PAT, bot token).

## Status log
- **2026-06-23** — Session resumed. Oracle hardening + approval-gate fixed & committed (`c23792a`, `44eed92`).
- **2026-06-23 PM** — `observance-design-deepening` workflow landed (8 agents): `design/` corpus committed
  (`080fd31`) — atmosphere-stack, cipher-web/clue-web + `puzzles_seed.sql`, bestiary (+sealed), immersion,
  MASTER-PLAN. All 3 critics = **revise, not block**. Fixed the confirmed `NamedMobBeat` no-drift bug
  (`26519d1`). Full disposition in [CRITIQUE-ACTIONS.md](design/CRITIQUE-ACTIONS.md). **Next:** build from
  the action ledger — showrunner deterministic spine + resource pack + FAWE branch → prove the vertical
  slice before authoring more arc (the critics' unanimous gate).
