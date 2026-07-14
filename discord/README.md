# The Observance Discord service

Production Node 22 service for the V5 Observance campaign. One persistent worker owns the Discord
client and runs a lease-safe showrunner tick every 10–15 seconds. A Render cron invokes the same
import-safe tick every five minutes as crash recovery; the Supabase lease prevents overlap.

## Player commands

- In Minecraft, `/obslink` issues that exact online player a 60-bit, single-use proof code. Only its
  SHA-256 digest is stored; issuance is rate-limited, and the code expires after five minutes.
- `/link <minecraft name> <callback> <code>` binds one Discord account to an existing Minecraft
  player and files C01's recovered Copperline handoff. One transaction validates the callback,
  LS04, and unconsumed Minecraft proof before claiming the name; it refuses privately claimed names,
  safely corrects this account's accidental prior name, and returns the already-filed receipt only
  for an exact short-window replay without a duplicate reward.
- `/answer <text> [puzzle]` submits a conclusion. The optional puzzle is a real scope, and its
  autocomplete shows only currently open V5 Discord/media nodes. A guessed future key is a neutral miss.
- `/whisper <puzzle>` spends the current coarse-act hint budget. Autocomplete intersects authored
  hints with the exact active/prerequisite-gated puzzle set.

The bot also watches `CHANNEL_THE_RECORD` as a passive answer surface. Misses, replays, unknown
players, and future answers do not disclose closeness.

## Database

Generate the single ordered database artifact:

```powershell
npm.cmd run db:seed
npm.cmd run db:bundlecheck
```

Apply `supabase/apply-all.sql` as the migration/service role. V5 adds:

- 10 mandatory `investigations` and exactly 82 `investigation_nodes`;
- idempotent `evidence_receipts`, `observance_record_evidence(...)`, service-role-only hashed
  `identity_link_challenges`, and validate/consume-before-claim identity transactions;
- five canonical fixed-media rows with prerequisite-gated public delivery;
- `arc_state.phase_key` while preserving `current_act` for hint-budget compatibility;
- in-place retirement of legacy puzzle, hint, archive, and optional-side rows.

Accounts, Discord bindings, consent choices, attempts, solves, and operational event history are not
deleted by the migration or seed.

## Environment

Copy `.env.example` to `.env`. Required values:

```dotenv
DISCORD_BOT_TOKEN=
DISCORD_APP_ID=
DISCORD_GUILD_ID=
CHANNEL_THE_RECORD=
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
```

Runtime tuning:

```dotenv
SHOWRUNNER_TICK_MS=12000
SHOWRUNNER_LEASE_SECONDS=300
OBSERVANCE_CAMPAIGN_VERSION=v5
```

The cadence is clamped to 10,000–15,000 ms. The lease is clamped to 60–900 seconds. V5 uses no AI,
speech-to-text, or Discord voice credentials; those retired lanes are absent from production configuration.

## Operations

When the 10-case V5 investigation table is active, the scheduled showrunner runs in `v5-safe` mode:
it updates only its liveness timestamp. Retired customs, apparition, herd, grave, archive-card, and
legacy finale producers are not reachable in production V5. The bot does not load the retired
prologue, coop, observer-capture, voice-capture, or AI-selection lanes. Progression is owned by durable website,
Discord, and Paper evidence receipts.

```powershell
npm.cmd ci
npm.cmd run register
npm.cmd start
```

The worker re-registers guild commands on boot. Enable Discord Message Content Intent. V5 does not
join voice channels, transcribe speech, or require Guild Voice States; those retired capture paths are absent.

Render uses the repository `render.yaml`:

- worker: `npm start` (Discord plus persistent showrunner);
- recovery cron: `npm run showrunner` every five minutes;
- both use the same Supabase and Discord environment values.

## Verification

```powershell
npm.cmd run typecheck
npm.cmd run audit
npm.cmd run runtimecheck
```

`audit` validates the 82-node runtime binding contract, V5 case/media seed, Discord/web surfaces,
normalized answers, database bundle, and resolver. `runtimecheck` covers scoping, persistent cadence, showrunner policies, and
chaos simulations. No test contacts production Discord or mutates Supabase.
