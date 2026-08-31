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
- Morrow-owned optional resource-pack handshake that reuses the exact hosted URL/SHA-1 while forcing
  `required=false`, rejects credentialed/non-HTTPS URLs outside loopback, unregisters its listeners on
  shutdown, and fails startup if an operator makes the pack mandatory; the shipped Morrow pack gate
  stays disabled until accepted/declined parity is observed in a real client
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
- M05–M08 are implemented as isolated, receipt-bound Paper investigations: the three-version room
  route, consensus audit, paired witness-anchor reconstruction, and source-chronology/Vigenere house
  gallery. Their retained Paper checkpoints prove 1,617 / 1,309 / 1,547 / 5,225 cells and stable
  3 / 8 / 9 / 8 owned display counts across restart without claiming human interaction
- M06 now owns the truthful `morrow.act3.incomplete_consensus_proven` receipt. The earlier
  `contradiction_preserved` key described M05 in public copy while carrying an M06 vote-policy payload.
  Java authority, prerequisites, Copperline, Discord, the runtime envelope, schema proposal, and the
  full client contract now agree. A forward-only migration preserves event UUIDs, payloads,
  idempotency keys, and stored projections; local proof is retained at
  `morrow/rehearsal/database/latest-m06-event-correction-local.json`, while live validation remains open
- M09 Account Continuity is implemented as a 2,520-cell safe chamber with a hashed private anchor,
  real quit/join receipts, a four-action bounded echo, excluded-knowledge identity challenge, four
  state lamps, ten owned entities, reset/immutability rules, crash recovery, and 1/2/6-player authority
  coverage; retained Paper proof is under `morrow/rehearsal/account-continuity-runtime`
- M10 Maintenance Window is implemented as a 5,301-cell split-reality chamber with contradictory
  Morrow bodies, witnessed pulses, tagged paper nonces, two physical hopper endpoints, non-consuming
  failure paths, safe disconnect return, retain-both authority, six state lamps, eighteen owned
  entities, restart recovery, and 1/2/6-player authority coverage; retained Paper proof is under
  `morrow/rehearsal/maintenance-window-runtime`
- M11 Cold Storage is implemented as a 6,417-cell bounded archive with five ordered custody records,
  first-broken-edge feedback, cryptographic hash chaining, two visibly distinct Morrow instances,
  tagged physical snapshot-hash transport, separate reconstruction/access receipts, nine redundant
  state lamps, eleven owned entities, crash recovery, and 1/2/6-player authority coverage. Its exact
  Paper build/restart proof is retained under `morrow/rehearsal/cold-storage-runtime`; human dialogue,
  item-bridge, and in-client caption/waveform playback remain open. An earned, transcript-complete
  Copperline current-voice assembly now exists locally. The filed provenance says the
  original process closed and the current process is reconstructed without resolving personhood
- M12 Branch Governance is implemented as an 8,325-cell finale with a bounded non-dexterity rollback
  wave, five accumulated-evidence anchors, four physical policy slots, four predicate-gated endings,
  separate governance authorization, persistent coda, thirteen redundant lamps, eighteen owned
  entities, crash recovery, and 1/2/6-player coverage. Invalid policies name missing categories without
  consuming evidence, and no final button can bypass prior receipts or physical configuration. Exact
  Paper build/restart evidence is retained under `morrow/rehearsal/branch-governance-runtime`; complete
  human finale traversal, dialogs, pacing, and accessibility remain open. All four ending records now
  have earned-only Copperline codas locally; authenticated live delivery remains open
- Copperline Act 0 support case at `/support/cases/mossfield-recovery`: an unresolved hosting-recovery
  workbench with an exact accessible text attachment and SHA-256 custody check, authenticated short-token
  handoff recovery, release/player/campaign-bound RLS reads, player-owned Server Actions, and explicit
  empty/loading/error/success states
- player-facing passwordless support login for existing linked accounts, enumeration-resistant
  responses, no automatic account creation, a case-safe callback, and strict separation from the
  operator console; an exact-project rehearsal-only session bootstrap fails closed outside validation
- synchronized Copperline Act 1–7 ticket presentation for all twenty-three non-Act-0 events that
  canonically project to the website, with group/player scope labels, exact catalog ordering,
  future-spoiler omission, raw-ending/private-dialogue exclusion, local-only raw replay samples,
  duplicate recovery, and collision halt. The Discord-private contradiction is intentionally absent
  because its catalog projection never includes Copperline
- the actual database projector now carries all twenty-five Copperline-owned events through the Act 7
  coda, rather than stopping at Act 2. It emits ordered `case_progress` and earned-only `case_media`
  projections for every linked player; the page model fails closed on unknown, premature, out-of-order,
  duplicate, or multiple-ending media keys
- eight canonical earned-media records: two real local OGG voice assemblies with full visible transcripts,
  two ordered text-equivalent voxel diagrams, and four mutually exclusive ending codas. Audio is served
  only through an authenticated case route after an earned key is present, is SHA-256 verified at read
  time, and has a complete no-audio equivalent. Locked media and ending copy are excluded from the client
  component bundle. Local contract evidence is retained under `morrow/rehearsal/media/latest.json`
- an earned-only Act 6 Copperline chronology puzzle with five shuffled retained records, an accessible
  ordered-edge form, first-broken-edge feedback, exact `dual_session_consciousness_proven` prerequisite,
  idempotent group receipt, and a payload that explicitly leaves continuity unresolved. Its additive
  migration and local contract receipt are retained under
  `morrow/rehearsal/database/latest-act6-chronology-local.json`; live Supabase and authenticated-browser
  execution remain open and no validation or production project was contacted
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
- authored, payload-free synchronized Discord receipts now cover every one of the eighteen canonical
  Discord projections through the Act 7 coda. The Act 2 behavior-reuse event still opens the private
  contradiction flow, its resolution still emits the special payload-free group receipt, and the other
  sixteen events use bounded spoiler-safe copy. Unknown events fail closed instead of posting raw data;
  full-campaign Gateway/database execution remains an isolated-service gate
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
  refusal, and the original six Act 1/2 ticket cards. The expanded Act 1–7 card/media model and full
  database projector are covered by local self-tests and a production build but still need the
  authenticated browser rehearsal. A
  private Supabase Cron schedule now proves automatic ten-second
  Copperline projection, health/run receipts, failure recovery, and clean disable. An exact-window
  Windows 10 fallback now proves one joined Room 04 readability checkpoint and one native terminal
  dialogue activation, including the before/after correction of giant TextDisplay overlays. A complete
  browser-requested PKCE magic-link exchange, a full-campaign database-bound Discord worker run, and every complete
  graphical lane remain unproven. Real Supabase email delivery and bot-only Discord Gateway transport
  are retained partial subproofs; every production gate remains disabled

Verified commands:

```text
python tools/check_morrow_authority.py
python tools/check_morrow_rehearsal.py
python tools/check_morrow_account_continuity_runtime_checkpoint.py
python tools/check_morrow_maintenance_window_runtime_checkpoint.py
python tools/check_morrow_cold_storage_runtime_checkpoint.py
python tools/check_morrow_branch_governance_runtime_checkpoint.py
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
    `morrow/rehearsal/runtime/p0-paper-c40f916/paper-runtime-receipt.json`. The retained
    `morrow/rehearsal/client-cohort-runtime/latest.json` checkpoint additionally proves bounded real-client
    one-/two-/six-player join, full-inventory restart persistence, and cleanup; the complete human-client
    lanes remain explicitly required in the launch matrix. A create-only run packet, independent-observer protocol,
    and media-plus-runtime validator now live in `morrow/rehearsal/CLIENT-REHEARSAL.md` and
    `tools/check_morrow_client_rehearsal.py`.

### Precise next boundary — human-client and remaining isolated-service rehearsal only

The automated P0 authority gate and actual disposable Paper lifecycle are green, but this does not make
the slice playable. A human client must complete every `required` lane in
`morrow/rehearsal/receipts/p0-item10-fa1b80b/launch-matrix.json`, including visual interpolation,
native-dialog input, full resource-pack decline parity, cue-by-cue audio-disabled equivalence, and pacing
without operator narration. Bounded full-inventory safe entry/rejoin and owned cleanup now pass with real
one-, two-, and six-client cohorts, but this is not the operator-free human playthrough. The database lane is now proven on
a paused validation project. The automatic Copperline database worker and real Supabase Cron schedule
are live-proven for the historically rehearsed Act 0–2 ordered two-player delivery, private actor receipts,
retry/reclaim, payload
isolation, payload-free health, and clean disable. A browser-requested Supabase magic link reached a
real disposable inbox and `/verify` returned an auth code. The callback then exposed
`pkce_code_verifier_not_found`: the Server Action had not preserved its verifier cookie. The login
request now uses an explicit same-origin 303 response that forwards the Supabase cookie mutations, but
the provider rate-limited the immediate retest. A fresh inbox-to-owner-case callback and authenticated
RLS read therefore remain. The exact
authored Discord receipt has crossed a real Gateway in a temporary everyone-denied/bot-allowed
channel, including `MESSAGE_CREATE`, REST readback, stable nonce, mention suppression, deletion, and
`Unknown Channel` verification. The database-claimed worker and a disposable-guild linked-player
ephemeral interaction remain; the JavaScript Copperline worker is only an operator fallback.
Do not enable production or mutate production services, data, or worlds at this boundary. M05–M12 now
have bounded authority and Paper construction/restart evidence; their complete human interaction lanes
remain open under the same proof discipline.

The first 2026-08-30 loopback attempts still document Windows app capture failing with
`SetIsBorderRequired failed: No such interface supported (0x80004002)` and are retained under
`morrow/rehearsal/client-attempts` as explicitly unproven historical attempts.

A later create-only attempt launched the locally installed vanilla 1.21.11 client directly with a
dummy offline identity, 75 manifest-verified libraries, nine extracted Windows natives, closed
loopback HTTP(S) proxying, and no account-file reads. It joined pinned Paper build 132 at an unsafe
stale coordinate; the new entry controller recovered it to `(0.5, 80.0, -1.5)` before damage. A fresh
hardened target then retained all 2,304 items across a same-player disconnect/rejoin at that safe cell,
with no entry-controller inventory mutations, no suffocation, and clean bounded client/server shutdown.
The follow-up cohort runtime harness repeated the join, inventory, Paper restart, identity continuity,
projector-cursor, and cleanup assertions with simultaneous one-, two-, and six-player vanilla clients;
its retained evidence and historical producer bindings live under `morrow/rehearsal/client-cohort-runtime`.
The host-compatible fallback subsequently selected exactly one Java multiplayer window by exact title
and PID while excluding the launcher. Its first Room 04 frame exposed giant default-scale TextDisplay
overlays. Bounded label scales removed those overlays, and relocating the idle label plus interaction
surface made `MORROW // TERMINAL` visible above the copper terminal and reachable from spawn. A fresh
client visibly showed the corrected room, and one observed terminal right-click opened the native
`Static restoration proposal` dialogue. The before/corrected/dialog frames and lifecycle receipts are
retained at `morrow/rehearsal/client-visual/2026-08-30-exact-window-visual-checkpoint.json`. This is only
a bounded visual/interaction checkpoint: continuous media, all six poses, the full dialogue input
matrix, parity modes, the continuous human cohort slice, independent review, and pacing remain required.

A second fresh client run now binds the fully implemented source commit
`d2580fd97ae14dc3db0b508d6ab7aad0b7f0241c` and plugin SHA-256
`65406d67787b764c25f5cf727dc710de67872fdcad749b1141736a105dc75343` to exact Paper
1.21.11 build 132. Its retained exact-window still confirms Room 04, the centered copper terminal,
bounded `MORROW // TERMINAL` label, zero-inventory-mutation safe entry, and clean runtime shutdown.
The fail-closed receipt is
`morrow/rehearsal/client-visual/2026-08-30-source-d2580fd-room04-checkpoint.json`. No client input was
sent in this run, so it does not advance any interaction or full-playthrough gate.

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

### P2 — Complete M11–M12 without breaking the grammar

M05–M12 now have pure authority, bounded Paper structures, native adapters, 1/2/6-player tests, and
retained two-boot server evidence. The twelve-entry canonical ledger is fully represented. Each investigation retains
at least three evidence sources, two surfaces, a native input verb, recoverable failure, accessibility
equivalent, callback, payoff, and factual truth. No three consecutive investigations may lead with the
same mechanic.

## Hard launch blockers

- No production service, world, Discord server, or database is mutated from this checkpoint.
- The numbered migration is generated and isolated-rehearsed, but has not been applied to production;
  a fresh verified backup and explicit production authorization are still required.
- The additive Act 6 chronology, full Act 1–7 Copperline projector, and earned-media seed are locally
  audited and build-clean but have not been executed on the isolated validation project or proven through
  an authenticated browser session.
- The automated P0 receipt bundle and disposable Paper lifecycle are green. A graphical client now
  connects, safe entry is runtime-proven, and the source-bound d2580fd Room 04 terminal has bounded retained
  still evidence through the exact-window fallback. The primary Computer Use capture API remains
  unavailable on this host. The full Morrow display-body pose set, Paper dialogs, M02 Static Restore,
  and M03/M04 Entity Replay still require live-client visual and interaction
  rehearsal. The Copperline reboot route has real email delivery, inbox receipt, provider verification,
  and an exact verifier-cookie defect fix, but still requires a fresh final PKCE callback and
  authenticated RLS read. Discord has real bot-only Gateway
  transport evidence but still requires a database-claimed worker and disposable-guild linked-player
  interaction. All later authored receipt policies are locally type-checked but not Gateway-rehearsed;
  its shipped feature gate remains false.
- M05–M12 construction and restart checkpoints are green, but their complete native-dialog, private
  display, wrong-answer, Escape, item-transfer, disconnect/rejoin, asymmetric-coop, readability, and
  pacing lanes are not. The current Computer Use capture path still fails on this host with
  `SetIsBorderRequired ... 0x80004002`; no blind client input was sent.
- `morrow-reboot.enabled` stays false until all remaining human-client and isolated-service receipts pass.
- Legacy names are audit-forbidden inside the reboot authority except the README's explicit boundary.

## Definition of playable

A release is playable only when a non-op player can discover Copperline, authenticate the handoff,
enter a fresh server, understand the room without operator narration, complete or safely reset every
slice action, witness synchronized web/Discord consequences, disconnect/rejoin without duplication,
continue through a simulated remote outage, and finish with the correct Morrow state after a Paper
restart. Screenshots alone do not qualify; retain event, journal, world read-back, and deployment
receipts bound to one `release_id`.
