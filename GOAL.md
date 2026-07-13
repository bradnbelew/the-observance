# The Observance — Release Goals

> **Current status (2026-07-13):** the implementation and isolated Paper 1.21.11 rehearsal are green.
> The only remaining launch gates require the production server, live Supabase project, and a real
> non-op client. Use [the final launch handoff](design/FINAL-LAUNCH-HANDOFF-2026-07-13.md) and the
> generated `rehearsals/2026-07-13` packet; older status-log entries below are historical context.

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
- **Privacy = consent-gated profiling** ("it knows me" max only for participating players), with
  session-zero disclosure, capture switches, per-player observer opt-out, and a dashboard kill-switch.
- **Showrunner = AI**, AUTO⇄CONFIRM toggle. Player-earned beats fire immediately (`approved`);
  curatorial beats wait for dashboard approval (`pending`).
- **Collective ending**, but never punish the group for an absent member — gate on ACTIVE players.
- **Nothing breaks.** Cross-surface consistency: Minecraft ⇄ Discord ⇄ dashboard never contradict.

## Workstreams (release state)
- [x] **Oracle and answer surfaces** — Discord, Record, and editable in-world filing signs share
  normalization, gates, collision handling, attempt limits, and authored rescue hints.
- [x] **Atmosphere stack** — auto-pushed rune/audio pack, ambient/private beats, particles, authored
  set pieces, restrained dread, and vanilla-client fallback behavior. Client mods remain B-roll only.
- [x] **Puzzle web** — varied typed, behavior, object, code, coordinate, spoken, temporal, cooperative,
  and cross-surface puzzles with physical producers and a complete clue/hint ledger.
- [x] **Story creatures and NPCs** — the Pale, Wren, Keeper, townsfolk, Unlit figure, and prior-group
  evidence are tied into measured state and the finale rather than existing as disconnected encounters.
- [x] **Day one through finale** — prologue, Keeper investigations, Deep Hold V4, Failed Accepting,
  Nether/End lanes, Unlit, Seventh reading, Accepting, release/erase outcomes, and recovery paths.
- [ ] **Production receipts** — apply the bundled SQL, install frozen artifacts, place/prove the live
  world, complete the non-op rehearsal, session zero, and secret rotation; then pass go/no-go.

## Manual production gates
- Apply only `discord/supabase/apply-all.sql` to Supabase project `fdnmhbpxnodrnbrzrlqq` and record
  its current SHA-1 in the rehearsal attestations.
- Install the frozen plugin/datapack/resource-pack artifacts on Paper 1.21.11 and configure the
  server-side Supabase and Discord secrets.
- Place and prove the production world, including the village-well Unlit entrance, Nether forge,
  End shrine, all outside-Hold anchors, and the generated Deep Hold.
- Complete session zero, rotate previously exposed secrets, and run a real non-op client rehearsal.
- Pass `tools/check_launch_manual_blockers.ps1 -Launch` with the dated coordinate and rehearsal packet.

## Status log
- **2026-06-23** — Session resumed. Oracle hardening + approval-gate fixed & committed (`c23792a`, `44eed92`).
- **2026-06-23 PM** — `observance-design-deepening` workflow landed (8 agents): `design/` corpus committed
  (`080fd31`) — atmosphere-stack, cipher-web/clue-web + `puzzles_seed.sql`, bestiary (+sealed), immersion,
  MASTER-PLAN. All 3 critics = **revise, not block**. Fixed the confirmed `NamedMobBeat` no-drift bug
  (`26519d1`). Full disposition in [CRITIQUE-ACTIONS.md](design/archive/CRITIQUE-ACTIONS.md). **Next:** build from
  the action ledger — showrunner deterministic spine + resource pack + FAWE branch → prove the vertical
  slice before authoring more arc (the critics' unanimous gate).
- **2026-06-29** — Final QA/integrator pass. Verified `PLAYTHROUGH-SCRIPT.md` (93 inline GAP markers →
  **62 de-duplicated GAP-REGISTER items**), `story-web.json` (104 nodes / 170 edges — every endpoint a real
  id), `WEB-MASTER §9` (31 ledger rows), and the design docs against the **real working tree** file-by-file.
  COHERENCE: ground-truth largely agrees; the build has moved AHEAD of the GAP register in places —
  `RoomSwapBeat.java` (GAP #30), the slug-aware `/record` route (GAP #56 / CP1-7), and most of the plugin
  beat library now EXIST (those docs are stale, corrected in the new prep doc). The headline real gap: **the
  engine can ENACT the back half but has no signal listeners to ARM it** — `IgnitionListener`,
  `UnlitDeepListener`, `SeventhChoiceListener`, `CoopPlateListener`, `RefusalRiteListener`, `VoiceListener`
  are all ABSENT. GUARD-RISK: `getOpenPuzzles` filters `active` only (the seeded `requires_flags` is inert —
  the dark-back-half bug, GAP #31), and the three new seedcheck guards (`activationReachability`,
  `unlockStepContract`, `watchlistSubset`) are specified in `BUILD-MANIFEST §8` but **not in `seedcheck.ts`**.
  REGENERATED `design/MINECRAFT-INGEST-PREP.md` (ordered GO-LIVE + testing + guard-risk + movement build
  order, built-vs-prepared). **Historical next step at that date:** author `migrations/0006_*.sql` + the `getOpenPuzzles`
  `requires_flags` filter** — the unblocker for the entire back half (everything downstream stays dark
  without it).
