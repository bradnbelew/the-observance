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
- private Supabase schema plus CLI-generated numbered migration, service-only SECURITY INVOKER RPC,
  authenticated owner-checked Copperline RPC, and idempotent event/projection writes
- cross-language audit parity for JSON, Java, TypeScript, SQL, and fail-closed plugin config
- disabled-by-default Paper `MorrowRuntime` that validates release, campaign, loaded world, HTTPS
  ingest, and secret bindings before opening `morrow-reboot.journal`
- async HMAC-SHA256 local event projector with release-bound responses, atomic hash-bound cursor,
  ordered exponential retry, duplicate recovery, collision halt, and bounded shutdown
- full-volume Recovery Room 04 manifest with an explicit safe spawn/exit route, occupied-cell refusal,
  durable pre-mutation rollback snapshot, atomic install receipt, crash recovery, and exact read-back
  audit (`9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a`)
- config-scoped Room 04 player entry ownership: Paper world-spawn binding, exact-world unsafe-join
  recovery, respawn binding, zero inventory mutation, main-thread enforcement, and clean listener
  shutdown; the 1/2/6-player pure contract is backed by an actual graphical 1.21.11 client join
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
- player-facing passwordless support login for existing linked accounts, enumeration-resistant
  responses, no automatic account creation, a case-safe callback, and strict separation from the
  operator console; an exact-project rehearsal-only session bootstrap fails closed outside validation
- synchronized Copperline Act 1/2 ticket projections for the six earned Room 04 and Entity Replay
  receipts, with group/player scope labels, prerequisite ordering, future-spoiler omission, local-only raw
  replay samples, duplicate recovery, and collision halt
- authenticated Copperline receipt RPC proposal with exact authored payload hashes, private token hash,
  linked-user ownership, release/prerequisite enforcement, and idempotent event/outbox creation; it remains
  unapplied to production but passed its isolated Supabase rehearsal in P1
- disabled-by-default native discord.js Gateway contradiction flow: exact linked Discord/Minecraft,
  campaign, release, prerequisite, guild/channel/thread, purpose, and expiring nonce bindings; ephemeral
  intended-recipient evidence; authored acknowledge/decline/cancel/timeout/recovery states; concrete
  provenance selection with no prose grading; and durable per-player/group locking for one-to-six players
- payload-free `morrow.act2.private_contradiction_resolved` group receipt with deterministic dedupe,
  altered-payload collision halt, leased outbox retry/restart recovery, spoiler-safe Discord projection,
  and Minecraft callback. No HTTP interaction endpoint, Chat SDK adapter, live server connection, or
  production migration was added
- deterministic P0 item 10 rehearsal harness and retained SHA-256 receipt bundle for one-, two-, and
  six-player cohorts: the full Act 0–2 slice order, restart after every durable boundary, wrong/partial/
  cancel/decline paths, disconnect/rejoin, ordered outage recovery, duplicate/collision, cursor loss,
  release/player/campaign refusal, private-recipient isolation, earned ticket projections, fallback/
  accessibility/inventory/cleanup contracts, and an explicit `offered_not_accepted` later-capture state
- create-only loopback Paper 1.21.11 build 132 lifecycle rehearsal bound to source checkpoint
  `c40f916aefb8dedf7c459a6636be92397fb0ebb1`, plugin SHA-256
  `38fac760c9ba3fc4ca652c5f1a59ec62cbf510157165e19c35942ba9f223977f`, and exact Paper SHA-256
  `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba`: 18 local candidates
  inventoried, bootstrap cache/libraries/versions copied and hash-preserved, Room 04 built then audited
  already-present after restart, PDC entity counts stable, clean shutdown observed, and the signed
  projector recovered in order from two loopback 503 responses without restart redelivery
- fail-closed launch matrix separating proven headless and actual Paper/runtime subproofs from the still
  required complete graphical-client and remaining service receipts. A real dummy offline 1.21.11 client
  now proves bounded safe entry without suffocation, and the isolated Supabase RLS/concurrency/rollback
  lane is proven. The authenticated browser now proves owner-only reads, Act 0 writes, wrong-token
  refusal, and six ticket cards with a manual isolated projection driver. Visual capture, automatic
  Copperline projection/magic-link delivery, and Discord Gateway delivery remain unproven, and every
  production gate remains disabled

Verified commands:

```text
python tools/check_morrow_authority.py
python tools/check_morrow_rehearsal.py
plugin/gradlew.bat check --no-daemon
plugin/gradlew.bat build --no-daemon
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

Items 1–10 are implemented in code and focused main-driven/headless rehearsal tests. The disposable
Paper lifecycle receipt exists, but Room 04 remains behind both the global reboot gate and its separate
`room04.build-enabled: false` shipped default until the required human-client and isolated-service
rehearsals are complete. Items 4–6 begin only when those gates have installed Room 04.

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
    rehearsal config. Automated authority lane complete; retained bundle:
    `morrow/rehearsal/receipts/p0-item10-fa1b80b`. The actual disposable Paper lifecycle is proven by
    `morrow/rehearsal/runtime/p0-paper-c40f916/paper-runtime-receipt.json`; graphical-client lanes remain
    explicitly required in the launch matrix. A create-only run packet, independent-observer protocol,
    and media-plus-runtime validator now live in `morrow/rehearsal/CLIENT-REHEARSAL.md` and
    `tools/check_morrow_client_rehearsal.py`.

### Precise next boundary — human-client and remaining isolated-service rehearsal only

The automated P0 authority gate and actual disposable Paper lifecycle are green, but this does not make
the slice playable. A human client must complete every `required` lane in
`morrow/rehearsal/receipts/p0-item10-fa1b80b/launch-matrix.json`, including visual interpolation,
native-dialog input, resource-pack decline, audio-disabled cues, full-inventory safe spawn/exit,
one-/two-/six-player cleanup, and pacing without operator narration. The database lane is now proven on
a paused validation project. The automatic Copperline database worker is live-proven for ordered
two-player delivery, private actor receipts, retry/reclaim, and payload isolation. Its checked-in
JavaScript service-role transport, production-shaped magic-link delivery, and a disposable Discord
worker/guild receipt remain.
Do not expand Acts 3–7, enable production, or mutate production services, data, or worlds at this boundary.

The first 2026-08-30 loopback attempt opened the installed Microsoft launcher but Windows app capture failed
with `SetIsBorderRequired failed: No such interface supported (0x80004002)` and exposed no accessibility
tree or focused element. No blind click was made; the disposable Paper server then closed cleanly. The
machine receipt is retained under `morrow/rehearsal/client-attempts` and is explicitly `unproven`.

A later create-only attempt launched the locally installed vanilla 1.21.11 client directly with a
dummy offline identity, 75 manifest-verified libraries, nine extracted Windows natives, closed
loopback HTTP(S) proxying, and no account-file reads. It joined pinned Paper build 132 at an unsafe
stale coordinate; the new entry controller recovered it to `(0.5, 80.0, -1.5)` before damage. A fresh
hardened target then retained all 2,304 items across a same-player disconnect/rejoin at that safe cell,
with no entry-controller inventory mutations, no suffocation, and clean bounded client/server shutdown.
Windows capture still failed with the same OS interface error, so the retained attempt is honestly
`unproven` for the six human-client lanes while recording the safe-entry runtime subproof.

### P1 — Safe database rehearsal

Completed on 2026-08-30 against the isolated `observance-validation-20260727` project, then paused:

1. Supabase CLI 2.116.0 was invoked and its help inspected; it generated
   `supabase/migrations/20260830200157_morrow_reboot_foundation.sql`.
2. Direct SQL iteration caught and rolled back one typed `SELECT INTO` defect before the fixed schema
   was reapplied idempotently.
3. Live tests covered anon/authenticated denial, linked-player projection reads, service ingest,
   wrong owner/release/prerequisite, duplicate, collision, concurrent duplicate, payload bounds,
   projection creation, and rollback.
4. Security/performance advisors found zero Morrow errors and zero unindexed foreign keys. One intentional
   authenticated SECURITY DEFINER warning is accepted in the durable receipt; raw replay samples remain
   out of Postgres.
5. Production application remains blocked until a fresh backup and explicit authorization. Receipt:
   `morrow/rehearsal/database/2026-08-30-isolated-supabase.json`.

### P2 — Expand acts without breaking the grammar

Implement Acts 3–7 in ledger order. Each investigation must retain at least three evidence sources,
two surfaces, a native input verb, recoverable failure, accessibility equivalent, callback, payoff,
and factual truth. No three consecutive investigations may lead with the same mechanic.

## Hard launch blockers

- No production service, world, Discord server, or database is mutated from this checkpoint.
- The numbered migration is generated and isolated-rehearsed, but has not been applied to production;
  a fresh verified backup and explicit production authorization are still required.
- The automated P0 receipt bundle and disposable Paper lifecycle are green. A graphical client now
  connects and safe entry is runtime-proven, but it remains unobservable through the Windows capture
  helper. The Morrow display body, Paper
  dialogs, M02 Static Restore, and M03/M04 Entity Replay still require live-client visual and interaction
  rehearsal. The Copperline reboot route still requires a live run of the checked-in service-role
  projector transport and a production-shaped magic-link rehearsal. The Discord
  reboot flow requires a disposable guild/worker rehearsal; its shipped feature gate remains false.
- `morrow-reboot.enabled` stays false until all remaining human-client and isolated-service receipts pass.
- Legacy names are audit-forbidden inside the reboot authority except the README's explicit boundary.

## Definition of playable

A release is playable only when a non-op player can discover Copperline, authenticate the handoff,
enter a fresh server, understand the room without operator narration, complete or safely reset every
slice action, witness synchronized web/Discord consequences, disconnect/rejoin without duplication,
continue through a simulated remote outage, and finish with the correct Morrow state after a Paper
restart. Screenshots alone do not qualify; retain event, journal, world read-back, and deployment
receipts bound to one `release_id`.
