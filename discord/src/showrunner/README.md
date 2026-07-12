# The Showrunner — deterministic spine

The between-session cron that keeps the mystery alive. **This layer is 100% deterministic — no LLM call
anywhere** — so pacing survives even if the AI layer is down (the critics' #1 risk). The Claude/Agent-SDK
authoring layer (personalized reports, difficulty tuning) bolts on later *on top of* this spine, never
underneath it.

## What one tick does (`snapshot → decide → apply`)
1. **Snapshot** (`snapshot.ts`) reads the world into an immutable value: kill-switch, mode, act, every
   open puzzle's stall signals (failed attempts / solves / attempters in a 3h window), last-drip time.
2. **Decide** (`decide.ts`) — a PURE function (same input → same output, fully unit-tested in
   `decide.selftest.ts`):
   - **Kill-switch:** `settings.watcher_sleep` true → heartbeat only.
   - **Stall auto-gift** (retention backstop): a puzzle with ≥5 failed attempts and no solve in the
     window → gift ONE earned whisper to each attempter who is *out of whispers* AND has a real
     next-tier hint. Double-guarded so it never over-gifts or promises an un-authored hint.
   - **Clue drip:** when the cadence (20h) is due, announce the next un-dripped open puzzle (movement
     asc, then key). CONFIRM mode → staged for dashboard approval; AUTO → posted live.
3. **Apply** (`apply.ts`) writes it: bump `whisper_budgets.earned`, post/stage the drip, persist state.
4. **Customs bridge** (`customs.ts` pure policy + `customs.run.ts` I/O — COHERENCE-AUDIT P0-4 / D1):
   reads measured `custom_compliance` violation counts and, on the Orin **observe → warn → left**
   ladder (`observed-warned-left-at-threshold.md` / D04), posts `voice.reportObserved` /
   `voice.reportEscalated` to `#the-record` and — at the *warn* rung — enqueues a **soft, reversible**
   toll beat (`custom_toll`, AUTO `approved` / CONFIRM `pending`; tolls take warmth not progress, INV-8).
   This is the missing consumer that un-strands all seven detected customs at once: until now nothing
   read `custom_compliance`, so a player who broke a custom never learned it.
   - **Precision over recall:** fires ONLY a measured violation (`violated_count > 0`) for a *namable*
     player — never an invented transgression (grounding contract).
   - **Idempotent:** a per-`(player, custom)` high-water mark of the highest `violated_count` already
     reported lives in `showrunner_state.reported_customs`; a rung re-fires only when the measured count
     rises past it, so the same violation is never re-reported every cadence.
   - **Fault-isolated + graceful:** Supabase down → read returns `[]` → the pass does nothing (no throw);
     a failed post does NOT advance the mark (re-tried next cadence); kill-switch `asleep` → silent.
   - The pure `decideCustomReports` is unit-tested in `customs.selftest.ts` (no DB/Discord/config),
     mirroring `decide.selftest.ts`.

## No new schema
State lives in one `settings` row (`key='showrunner_state'`: last_run, last_drip_at, dripped_keys,
pending_drips). Health is an `event_log` row per tick. Gifts bump `whisper_budgets.earned`. Nothing new
to migrate.

## Run
```
npm run runtimecheck             # storylet gate + every pure showrunner runtime self-test
npm run showrunner:test:all      # every pure showrunner runtime self-test, without gatecheck
npm run showrunner:test          # pure drip/gift decision unit tests (no DB/network)
npm run showrunner:test:customs  # pure customs-ladder unit tests (no DB/network)
npm run showrunner:dry           # read-only: print what the next tick WOULD do (now incl. a customs preview)
npm run showrunner               # one live tick (drip/gift + customs bridge)
```

## Deploy (Render Cron Job)

The repository-root `render.yaml` provisions both the always-on Discord worker and this hourly cron
from one Blueprint. Secrets remain `sync: false`; enter them in Render rather than committing them.
Run `npm run register` once after the bot credentials and guild are configured.
- **Build:** `npm install`  •  **Command:** `npm run showrunner`  •  **Schedule:** e.g. `0 * * * *` (hourly;
  drip cadence is gated to 20h internally regardless of how often the cron fires).
- **Env:** the same vars as the bot — `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `DISCORD_BOT_TOKEN`,
  `CHANNEL_THE_RECORD` (+ `DISCORD_APP_ID`, `DISCORD_GUILD_ID` to satisfy shared config).
- Idempotent + cheap: a tick with nothing to do just writes a heartbeat. Safe to over-schedule.

## The between-session AUTONOMY layer (`autonomy.run.ts` + the pure policy modules)
On top of the deterministic spine, the showrunner authors the world between sessions. Every producer is
a PURE policy module (importable by `autonomy.selftest.ts` with no DB) wrapped by `autonomy.run.ts`,
each fault-isolated and **with a deterministic fallback behind any LLM call** (the SPOF mitigation —
nothing ever blocks on the model):
- **Difficulty grip** (`reckoning.ts`, A10/FACT 2b) — mastery → cadence multiplier + register `tone` +
  dead-end staging, with hysteresis. Never touches `whisper_budgets` (INV-15, grep-guarded in the test).
- **Cold-start prologue** (`prologue.ts`, B4) — ignition gate (no curatorial drip before
  `prologue_ignited`); one-shot `recordOpened` ack; named-report precision floor.
- **Personalized reports** (`reports.ts`, D1) — the "it knows me" observation, `voice.reportObserved`
  as the deterministic floor, the scalpel offered only a constrained slot it may decline; precision-gated
  (flat dossier → no report), idempotent on the dominant habit.
- **Hold-Book** (`keeper-record.ts`, A3) — per-player keeper-voice page rows, precision floor (flat →
  enrolled to no one), idempotent tiers, author-slot fallback.
- **Divergent fates** (`fate.ts`, A2/INV-11) — active-only enum selector → the M5 composer (set-once in
  `resolve.ts`, cross-owner).
- **Liar engine** (`liar.ts`, D4) — flag-gated, one-way warm→cold re-stage of Iss's beats on
  `iss_caught` (curated re-staging only; the activation lane is the `requires_flags` gate, cross-owner).
- **Spawn-bias conductor** (`conductor.ts`, D7/INV-18) — the single-arbiter `apparition_claim` per drama
  window; probabilistic (seeded weighted draw, never argmax) + per-player capped; every ambient lane
  defers to the published claim.
- **Keeper-NPC dialogue** (`keeper.ts`, D8/FACT 9) — dossier-conditioned node resolution; M-IV atonement
  withholding; FACT-9 one-surface-per-window; defers to `apparition_claim` for the prior-keeper apparition.
- **Grave / herd / forks / clock** (`grave.ts`/`herd.ts`/`forks.ts`/`clock.ts`) — the future-dated grave
  (INV-14), the capped cosmetic pale spread (INV-13), one-way fork leaves (INV-12), the single Accepting
  instant binder shared by the grave + website + summons.
- **`readActiveRoster(windowMs)`** (`autonomy.run.ts`) — the single active-set source the conductor +
  Accepting bridge + keeper-NPC consume; none re-derives the active set.

```
npm run showrunner:test:autonomy   # pure autonomy-policy unit tests (no DB/network/LLM)
```

## Still to come (cross-owner reads + tracked in `design/CRITIQUE-ACTIONS.md`)
- Dashboard **health panel + manual-drip button** (reads `showrunner_state`, posts a `pending_drip`).
- The **dossier / visited-cells / compliance-spread readers** (SQL+PLUGIN lanes) that feed the
  personalized-report / keeper-NPC / name-where / offline-skin passes — until they land those passes
  degrade to a precise NO-OP (precision over recall), never a guess.
