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

## No new schema
State lives in one `settings` row (`key='showrunner_state'`: last_run, last_drip_at, dripped_keys,
pending_drips). Health is an `event_log` row per tick. Gifts bump `whisper_budgets.earned`. Nothing new
to migrate.

## Run
```
npm run showrunner:test   # pure decision unit tests (no DB/network)
npm run showrunner:dry    # read-only: print what the next tick WOULD do (safe on live DB)
npm run showrunner        # one live tick
```

## Deploy (Render Cron Job)
- **Build:** `npm install`  •  **Command:** `npm run showrunner`  •  **Schedule:** e.g. `0 * * * *` (hourly;
  drip cadence is gated to 20h internally regardless of how often the cron fires).
- **Env:** the same vars as the bot — `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `DISCORD_BOT_TOKEN`,
  `CHANNEL_THE_RECORD` (+ `DISCORD_APP_ID`, `DISCORD_GUILD_ID` to satisfy shared config).
- Idempotent + cheap: a tick with nothing to do just writes a heartbeat. Safe to over-schedule.

## Still to come (tracked in `design/CRITIQUE-ACTIONS.md`)
- Dashboard **health panel + manual-drip button** (reads `showrunner_state`, posts a `pending_drip`).
- Drip ordering smarter than movement-asc (prefer entry clues; sequence dead-ends deliberately).
- The **AI authoring layer** (personalized reports, "it knows me" hooks) on top, each write schema-validated
  + `status='pending'` for curatorial beats.
