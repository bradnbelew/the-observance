# THE OBSERVANCE — LAUNCH READINESS (2026-07-03)

The state after the "build everything left + full first-contact→finale pass" push. Branch
`feat/build-everything-2026-07-01`, all committed, **every surface green** (plugin jar · discord tsc +
9 selftests · dashboard tsc + 2 selftests · datapack JSON). Nothing pushed.

The experience is **code-complete and launch-ready** end to end — from the first thing a player
experiences to the finale close — barring the MANUAL items below (real media + ops that are yours to
make/apply, which the code already degrades around safely).

---

## 1. What shipped this push (all green)

- **W4.2 — Observer Tier-2 LLM archivist.** Opus 4.8 picks the most uncanny REAL quote to echo; pure
  selection (never composes text); fires ≤once/12h; degrades to the deterministic pick on any failure.
- **W5 — the voice tier ("it heard you SAY it").** Discord voice → Whisper → observations → the same
  Observer echo, in a distinct "heard aloud" register. Off by default; optional deps; fault-isolated.
  - Closed the `spine-spoken-name` orphan: speaking "the one who turned away" → the Watcher quotes it back.
- **W8 — cohesion + hardening audit.** Fixed the real blocker (`CoopPlateListener` / `m4-three-hands`
  had no producer → the primary M4 "true walk" chain was unreachable; now a faithful cross-surface coop
  gate). Fixed the `beat_queue` `failed`-status deploy hazard (idempotent migration 0009). Voice-dispatch
  cohesion + plugin polish. Everything else verified sound (normalizer twins, flags, reveal-gate, finale reachable).
- **W9 — journey pass, the two finale BLOCKERS fixed:**
  - **The ending now posts.** Built the fate sentinel — `ending_fate` was never written, so the composed
    divergent close never reached players. Now decided from the active-only compliance spread at the Accepting instant.
  - **The reckoning is now felt.** condemn/free → the "it heard you" echoes cease (announced once); understand
    → they persist as sorrow. (`observer.run.ts` had no reckoning gate.)
  - Rosetta literacy KEY made concrete (rune↔plaintext crib pairs — the "these are letters" turn).
  - Mara (the thinnest keeper) given her 2nd clue.
- **A3 — keeper-record wired (post-W9, Ethan-approved).** The Hold-Book now writes each living player in
  (living row → keeper heading → the keeper's own hand) from their measured habit, re-filling the lectern
  on each tier crossing. Delivers the authored `keeperPage*` pages that were wired nowhere before. Mapping:
  hoards→vaun, reads→mara, wanders→sella, silent→orin, night-walks→brann, spends-words→iss.

---

## 2. MANUAL go-live actions (yours — the code degrades safely until each is done)

**Apply / deploy**
- [ ] Apply the Supabase migrations to the live DB (both lineages, in order). Newest this push:
      `discord/0009_observations`, `dashboard/0009_beat_queue_failed_status` (idempotent), and the earlier
      `dashboard/0007_v_archive` + `0008_v_archive_flag_gate` (you said 0007/0008 are already in).
- [ ] Re-seed so the gated rows are lit (progression/metapuzzle) — the Nether/End lanes are `active=true`.
- [ ] `npm run archive:materialize` in `discord/` (or let the cron self-populate the archive bodies).
- [ ] Host the resourcepack (`observance-resourcepack.zip`) and set `config.yml resource-pack.url`+`sha1`.
      **The rune font ships in the pack** — the rosetta cribs + all rune beats only render once it's live.
- [ ] Survey + `placeworld` the sites, incl. the two new coop/lane sites: `coop_plate` (the three-hands
      gate) and the Nether/End lane sites (from their own dimensions). An unplaced site = a safe no-op.
- [ ] Stage the cold open in the group's base before session zero: `/observance placeprologue`.

**Turn on the optional tiers (each OFF by default; all degrade to silence)**
- [ ] Observer capture: flip the `observer_capture` setting on when ready.
- [ ] Observer Tier-2: set `ANTHROPIC_API_KEY` in the **Render cron** env (the showrunner runs there, not Vercel).
- [ ] Voice tier: set `DISCORD_VOICE_CHANNEL_ID` + a Whisper backend (`WHISPER_API_URL`/`_KEY` or `WHISPER_BIN`)
      + flip the `voice_capture` setting, then restart the bot. (Deps auto-install as optionalDependencies.)

**Real media (the hero artifacts — the code has the wiring + graceful placeholders)**
- [ ] `dashboard/public/the-hold/the-hold.zip` — the offline "cursed map" the lure page links. Until it's
      hosted, **don't plant the in-world clue to the lure slug** (`/record/the-record-keeps`) — the link 404s otherwise.
- [ ] The found-footage clip + the recovered Drive folder + the waveform/spectrogram image — for
      `spine-recovered-archive` (a wired puzzle awaiting its artifact) and the coords-in-a-frame first-find aid.

**Security / ops**
- [ ] Rotate the previously-exposed `service_role` + Discord bot creds.
- [ ] Run the first vertical-slice playtest.

---

## 3. Deferred enhancements (designed, NOT half-shipped — each flagged honestly)

These are real "could be more" items. None is a blocker; none is shipped in a degraded state (no
half-ready code). Ethan's decisions on the earlier open items are recorded here.

- **REFUSERS ending — DECIDED OUT (Ethan).** Not wanted. The fate sentinel still reads `flags.refusal_signal`
  (nothing sets it), so REFUSERS stays a designed-but-dormant fifth ending; the other three fates
  (kept/cast_out/divided) are the live set. To revive it later, add a plugin refusal-rite that sets the flag.
- **Self-firing cold open — DECIDED: operator-staged (Ethan).** The base anomaly is staged by
  `/observance placeprologue` (you control the base edit). No autonomous base-editing is built (intentional).
- **`keeper.ts` (keeper-rhyme NPC beat).** Separate from the now-wired keeper-record: an in-world keeper NPC
  line that rhymes with a player's habit. Pure + self-tested, still unwired (no beat producer). Lower value
  than keeper-record (which delivers the same rhyme via the Hold-Book); leave until it has a clear surface.
- **Early first-find trailhead for a sky-blocked first site.** The recovery needle is a late (post-Seventh)
  aid; the first-find coords are meant to come from your hero artifact. If a keeper site is sky-blocked (no
  beacon beam) AND no coord artifact is authored, a group could lack a trailhead. Mitigation: ensure the
  first site is beacon-visible or its coords are in the first artifact.
- **Six-prior-groups slow burn / diverse-puzzle archive cards.** The richest slow-burn is 3 cards; a few
  more per-group traces + thread cards on the payoff expansion puzzles would deepen the reconstruction web.
  Pure content adds (thread_cards.sql + voice.archive.ts bodies), no engine change.

---

## 4. Producer-coverage guardrail (recommended)

`m4-three-hands` slipped (an opaque-token puzzle with no producer) because nothing asserts that every
plugin/voice-produced puzzle has a registered producer. A build-time check (seed opaque-token rows ↔
config.yml producer tokens + the discord producers) would catch this class of orphan. Not built (a robust
version needs careful SQL/YAML parsing); worth adding before scaling the puzzle set.
