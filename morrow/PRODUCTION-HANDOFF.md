# Morrow Production Handoff

## Checkpoint

Branch: `codex/morrow-reboot-architecture`

This checkpoint is the new reboot authority and executable foundation. It is not the legacy V5 story
and it is not ready for a player server. The shipped config keeps `morrow-reboot.enabled: false`.

Implemented and verified:

- clean-room canon, eight-act journey, twelve-investigation ledger, six relationship states
- twenty-six-event cross-surface vocabulary with explicit capability authorization receipts
- Paper event ownership/prerequisite authority and six-stage transition service
- restart-safe, hash-chained, local-first Paper journal with collision/release checks
- signed Next.js Minecraft event endpoint with replay, size, owner, and schema validation
- private Supabase schema proposal, service-only SECURITY INVOKER RPC, idempotent event/projection write
- cross-language audit parity for JSON, Java, TypeScript, SQL, and fail-closed plugin config
- disabled-by-default Paper `MorrowRuntime` that validates release, campaign, loaded world, HTTPS
  ingest, and secret bindings before opening `morrow-reboot.journal`
- async HMAC-SHA256 local event projector with release-bound responses, atomic hash-bound cursor,
  ordered exponential retry, duplicate recovery, collision halt, and bounded shutdown
- full-volume Recovery Room 04 manifest with an explicit safe spawn/exit route, occupied-cell refusal,
  durable pre-mutation rollback snapshot, atomic install receipt, crash recovery, and exact read-back
  audit (`9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a`)
- vanilla-safe Morrow presentation built from Paper block/text displays plus one interaction entity,
  release-bound PDC ownership, startup cleanup, six-hour expiry, restrained active-speaker tracking,
  and the six authored relationship poses without fake-player, NMS, or resource-pack dependency
- native Paper dialogs for the terminal greeting, bounded proposal, read-only evidence review, and
  explicit Entity Replay authorization, with Escape-safe close, group-stable idempotency, exact local
  receipt routing, and one-, two-, and six-player self-test coverage
- canonical M02 Static Restore authority (`870d27b4b21539e496577eff68dc5f26ada71ec321fef9629088e9a8274b28ff`):
  six exact physical diff cells, three visible two-cell passes, four independently openable evidence
  stations, physical-cell plus provenance predicate, authored wrong-action feedback, transactional
  reset/rollback, restart and reconnect catch-up, and group-idempotent `intention_error_proven`
- canonical M03/M04 Entity Replay loop: purpose-bound per-player consent revisions, exact 37-second
  missing-role timing, two-tick sampling, 45-second/450-sample/six-player hard caps, redundant-sample
  compression, atomic content-addressed local clips, release/consent/player/purpose/hash binding,
  hash-and-summary-only event projection, display-entity interpolation with recorded/reconstructed/live
  provenance, disconnect/rejoin/restart catch-up, and exact predicates for `missing_role_completed`,
  `live_test_recorded`, and `behavior_reuse_proven`
- separate native later-capture authorization after behavior proof; it commits only
  `live_capture_authorized` with `starts_capture: false` and never starts recording automatically
- Copperline Act 0 support case at `/support/cases/mossfield-recovery`: an unresolved hosting-recovery
  workbench with an exact accessible text attachment and SHA-256 custody check, authenticated short-token
  handoff recovery, release/player/campaign-bound RLS reads, player-owned Server Actions, and explicit
  empty/loading/error/success states
- synchronized Copperline Act 1/2 ticket projections for the six earned Room 04 and Entity Replay
  receipts, with group/player scope labels, prerequisite ordering, future-spoiler omission, local-only raw
  replay samples, duplicate recovery, and collision halt
- authenticated Copperline receipt RPC proposal with exact authored payload hashes, private token hash,
  linked-user ownership, release/prerequisite enforcement, and idempotent event/outbox creation; it remains
  unapplied pending the isolated database rehearsal in P1
- disabled-by-default native discord.js Gateway contradiction flow: exact linked Discord/Minecraft,
  campaign, release, prerequisite, guild/channel/thread, purpose, and expiring nonce bindings; ephemeral
  intended-recipient evidence; authored acknowledge/decline/cancel/timeout/recovery states; concrete
  provenance selection with no prose grading; and durable per-player/group locking for one-to-six players
- payload-free `morrow.act2.private_contradiction_resolved` group receipt with deterministic dedupe,
  altered-payload collision halt, leased outbox retry/restart recovery, spoiler-safe Discord projection,
  and Minecraft callback. No HTTP interaction endpoint, Chat SDK adapter, live server connection, or
  production migration was added

Verified commands:

```text
python tools/check_morrow_authority.py
plugin/gradlew.bat check --no-daemon
cd dashboard && npm.cmd run selftest
cd dashboard && npm.cmd run lint
cd dashboard && npm.cmd run build
cd discord && npm.cmd run morrowcheck
cd discord && npm.cmd run typecheck
cd discord && npm.cmd run audit
cd discord && npm.cmd run runtimecheck
```

## Production order

### P0 — Make the 60–90 minute slice playable

Items 1–9 are implemented in code and focused main-driven self-tests. Room 04 remains behind both the
global reboot gate and its separate `room04.build-enabled: false` rehearsal gate until a disposable
Paper/live-client receipt exists. Items 4–6 begin only when those gates have installed Room 04.

1. Add a `MorrowRuntime` lifecycle that opens `morrow-reboot.journal` only when the disabled-by-default
   config has valid release/campaign/world/HTTPS ingest values.
2. Add an async HMAC projector for `MorrowLocalState.pendingAfter`, with a durable cursor, exponential
   retry, collision halt, and no Bukkit API access off-thread.
3. Build Recovery Room 04 as a bounded, idempotent manifest with occupied-cell refusal, rollback
   snapshot, safe spawn/exit cells, and read-back audit.
4. Implement Morrow's vanilla fallback body using display and interaction entities, PDC ownership,
   cleanup, restrained tracking, and the six authored poses.
5. Implement native Paper dialogs for terminal greeting, restoration proposal, evidence review, and
   explicit Entity Replay authorization. Receipt-producing callbacks are concrete and idempotent;
   review, decline, Escape, and reset do not fabricate progression receipts.
6. Implement the sixth-block static restoration investigation, including authenticated/inferred block
   materials, physical diff, three-source evidence, wrong-action feedback, reset, and catch-up.
7. Implement the 37-second entity replay and deliberate movement test with explicit opt-in, two-tick
   sampling, 45-second/450-sample cap, local storage, interpolation, and visual provenance. Implemented:
   unsealed clips delete on cancel, boundary exit, consent revision/revocation, disconnect, or timeline
   fault; sealed clips persist and verify locally before any receipt or echo.
8. Add the Copperline Act 0 case chain and Act 1/2 ticket projections as new Morrow routes; do not
   reuse legacy Hold pages or names.
9. Add the Discord private contradiction/group receipt for the slice using native interactions and
   Ed25519 validation where HTTP interactions are used.
10. Exercise one-, two-, and six-player paths through restart and remote outage before enabling the
    rehearsal config.

### Precise next boundary — P0 item 10 only

Discord now consumes the earned `morrow.act2.behavior_reuse_proven` projection, presents each linked
participant an intended-recipient ephemeral contradiction, and commits only the payload-free group
`morrow.act2.private_contradiction_resolved` receipt once every expected participant chooses the exact
authored inferred-source classification. Paper requires that group receipt before it can offer the
separate explicit `live_capture_authorized` dialog. P0 item 10 may only rehearse the already-authored
slice on disposable infrastructure for one, two, and six players through Paper restart, Discord worker
restart, remote outage/recovery, disconnect/rejoin, duplicate delivery, and clean reset. It must retain
release-bound journal, projection, physical read-back, and interaction receipts; it must not expand Acts
3–7, enable the reboot in production, migrate the live database, or mutate live Discord/Crafty/Vercel/
Supabase services or worlds.

### P1 — Safe database rehearsal

1. Install the current Supabase CLI and inspect its help; do not invent a migration filename.
2. Create an isolated local/branch target and convert `db/schema-proposal.sql` into a real migration.
3. Add database tests for anon/authenticated denial, linked-player projection reads, service ingest,
   wrong owner/release/prerequisite, duplicate, collision, concurrent duplicate, payload bounds,
   projection creation, and rollback.
4. Run Supabase security/performance advisors. Keep raw replay samples out of Postgres.
5. Only after a production backup and rehearsal receipt may the migration target production.

### P2 — Expand acts without breaking the grammar

Implement Acts 3–7 in ledger order. Each investigation must retain at least three evidence sources,
two surfaces, a native input verb, recoverable failure, accessibility equivalent, callback, payoff,
and factual truth. No three consecutive investigations may lead with the same mechanic.

## Hard launch blockers

- No production service, world, Discord server, or database is mutated from this checkpoint.
- The SQL file is a proposal, not a migration, because the Supabase CLI is absent here.
- Recovery Room 04, the Morrow display body, Paper dialogs, M02 Static Restore, and M03/M04 Entity
  Replay still require disposable Paper/live-client rehearsal. The Copperline reboot route requires an
  isolated database migration/projector rehearsal and authenticated browser rehearsal. The Discord
  reboot flow likewise requires the proposal to become an isolated migration plus a disposable guild/
  worker rehearsal; its shipped feature gate remains false.
- `morrow-reboot.enabled` stays false until the end-to-end disposable Paper receipt passes.
- Legacy names are audit-forbidden inside the reboot authority except the README's explicit boundary.

## Definition of playable

A release is playable only when a non-op player can discover Copperline, authenticate the handoff,
enter a fresh server, understand the room without operator narration, complete or safely reset every
slice action, witness synchronized web/Discord consequences, disconnect/rejoin without duplication,
continue through a simulated remote outage, and finish with the correct Morrow state after a Paper
restart. Screenshots alone do not qualify; retain event, journal, world read-back, and deployment
receipts bound to one `release_id`.
