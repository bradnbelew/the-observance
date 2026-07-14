# The Observance V5 — release goal

Status: active production rewrite. Do not claim launch-ready until the acceptance receipts below exist.

## Outcome

Deliver a cohesive, difficult, fair, and finishable Minecraft ARG from first Copperline trace through persistent Coda Mode. The campaign must feel like an investigation into a lived server history—not a guided repetition of one missing-seventh thesis.

## Locked scope

- ten mandatory cases;
- exactly 82 required nodes;
- all eight Unlit houses required;
- all four fixed videos plus the fixed spectrogram required;
- one command-built protected Deep Hold using the proven 32-room shell;
- one public Hold entrance/exit and the village well as the Unlit entrance;
- six distinct Keepers and distinct Averyn;
- four real prior investigators: mkept, Ash, Rook, Wren;
- Wren condemn/understand/free branches, all finishable;
- publish/release-unnamed name treatments, both freeing Averyn;
- dramatic durable save/goodbye/kick/shutdown and persistent Coda Mode.

## Quality bars

1. **Investigation depth:** clues change theories and require comparison; no filler restatements.
2. **Puzzle fairness:** every node has lead, evidence, operation, input, feedback, hints, payoff, recovery, and durable state.
3. **Minecraft reliability:** no overlap, bad facing, blocked route, empty content, irrecoverable item, fragile timing, or hidden manual setup.
4. **Cross-surface parity:** Minecraft, Discord, Supabase, Copperline, media, and NPCs agree on names, prerequisites, answers, and ending state.
5. **Emotional finish:** every choice produces a specific consequence and complete goodbye.

## Hard constraints

- Paper `1.21.11`, Java 21, ordinary clients, auto-pushed resource pack only.
- No player-facing server logs.
- No sound-only required clue.
- No random scavenger, dynamic maze, precision parkour, timing-sensitive redstone, or destructible critical entity.
- No manual filling of books, signs, shelves, containers, frames, or NPC dialogue.
- No optional substantial story.
- No six-as-one reveal, mkept-is-not-a-person reveal, six-prior-answers worksheet, or hard-coded `kept: 6` thesis.
- No finale dependent on an hourly process.
- No deployment claim without live verification.

## Acceptance receipts

- [ ] `python tools/check_v5_freshness.py` passes.
- [ ] `python tools/check_v5_content.py --runtime` passes.
- [ ] `python tools/check_v5_physical_predicates.py` passes for all 60 Minecraft-owned nodes.
- [ ] Discord typecheck, audit, runtimecheck, and V5 DB bundle tests pass.
- [ ] Dashboard lint, selftest, and production build pass.
- [ ] Plugin clean/check/build passes with the V5 plan/content/finale tests included.
- [ ] A clean isolated Paper `1.21.11` server loads only the selected V5 jar.
- [ ] Build plan/survey/persistence/audit succeeds at a fresh production-like site.
- [ ] All 32 rooms, 76 fixture owners, 8 gates, and 82 node producers read back exactly.
- [ ] Adventure-mode traversal and sealed/open gate isolation pass with non-op clients.
- [ ] Critical-item loss/duplication/death/storage/hopper/recovery tests pass.
- [ ] Restart after every main gate and finale phase is idempotent.
- [ ] All fixed media URLs are reachable and payloads reverified.
- [ ] NPC anchor/facing/restart repair and synchronous critical dialogue pass.
- [ ] Supabase V5 migration retires stale rows without deleting accounts, consent, or event history.
- [ ] Render persistent worker and recovery cron are live with measured response latency.
- [ ] Vercel production and `copperlinehosting.com` routes/auth/coda are verified.
- [ ] Full real-client run reaches both name treatments and all Wren outcomes in rehearsal.
- [ ] Production setup guide contains real coordinates, hashes, environment key names, and screenshots.
- [ ] Final commit is pushed to `main` and deployment URLs/hashes are recorded.

Anything unchecked is a named blocker, not “probably ready.”
