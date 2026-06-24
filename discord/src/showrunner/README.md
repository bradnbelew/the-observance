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
npm run showrunner:test          # pure drip/gift decision unit tests (no DB/network)
npm run showrunner:test:customs  # pure customs-ladder unit tests (no DB/network)
npm run showrunner:dry           # read-only: print what the next tick WOULD do (now incl. a customs preview)
npm run showrunner               # one live tick (drip/gift + customs bridge)
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
