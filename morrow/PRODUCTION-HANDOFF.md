# Morrow Production Handoff

## Checkpoint

Branch: `codex/morrow-reboot-architecture`

This checkpoint is the new reboot authority and executable foundation. It is not the legacy V5 story
and it is not ready for a player server. The shipped config keeps `morrow-reboot.enabled: false`.

Implemented and verified:

- clean-room canon, eight-act journey, twelve-investigation ledger, six relationship states
- twenty-five-event cross-surface vocabulary with explicit capability authorization receipts
- Paper event ownership/prerequisite authority and six-stage transition service
- restart-safe, hash-chained, local-first Paper journal with collision/release checks
- signed Next.js Minecraft event endpoint with replay, size, owner, and schema validation
- private Supabase schema proposal, service-only SECURITY INVOKER RPC, idempotent event/projection write
- cross-language audit parity for JSON, Java, TypeScript, SQL, and fail-closed plugin config

Verified commands:

```text
python tools/check_morrow_authority.py
plugin/gradlew.bat check --no-daemon
cd dashboard && npm.cmd run selftest
cd dashboard && npm.cmd run lint
cd dashboard && npm.cmd run build
```

## Production order

### P0 — Make the 60–90 minute slice playable

1. Add a `MorrowRuntime` lifecycle that opens `morrow-reboot.journal` only when the disabled-by-default
   config has valid release/campaign/world/HTTPS ingest values.
2. Add an async HMAC projector for `MorrowLocalState.pendingAfter`, with a durable cursor, exponential
   retry, collision halt, and no Bukkit API access off-thread.
3. Build Recovery Room 04 as a bounded, idempotent manifest with occupied-cell refusal, rollback
   snapshot, safe spawn/exit cells, and read-back audit.
4. Implement Morrow's vanilla fallback body using display and interaction entities, PDC ownership,
   cleanup, restrained tracking, and the six authored poses.
5. Implement native Paper dialogs for terminal greeting, restoration proposal, evidence review, and
   explicit Entity Replay authorization. Every callback commits one concrete local receipt.
6. Implement the sixth-block static restoration investigation, including authenticated/inferred block
   materials, physical diff, three-source evidence, wrong-action feedback, reset, and catch-up.
7. Implement the 37-second entity replay and deliberate movement test with explicit opt-in, two-tick
   sampling, 45-second/450-sample cap, local storage, interpolation, and visual provenance.
8. Add the Copperline Act 0 case chain and Act 1/2 ticket projections as new Morrow routes; do not
   reuse legacy Hold pages or names.
9. Add the Discord private contradiction/group receipt for the slice using native interactions and
   Ed25519 validation where HTTP interactions are used.
10. Exercise one-, two-, and six-player paths through restart and remote outage before enabling the
    rehearsal config.

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
- Recovery Room 04, the Morrow display body, Paper dialogs, replays, Copperline reboot pages, and
  Discord reboot interactions still require implementation and live-client rehearsal.
- `morrow-reboot.enabled` stays false until the end-to-end disposable Paper receipt passes.
- Legacy names are audit-forbidden inside the reboot authority except the README's explicit boundary.

## Definition of playable

A release is playable only when a non-op player can discover Copperline, authenticate the handoff,
enter a fresh server, understand the room without operator narration, complete or safely reset every
slice action, witness synchronized web/Discord consequences, disconnect/rejoin without duplication,
continue through a simulated remote outage, and finish with the correct Morrow state after a Paper
restart. Screenshots alone do not qualify; retain event, journal, world read-back, and deployment
receipts bound to one `release_id`.
