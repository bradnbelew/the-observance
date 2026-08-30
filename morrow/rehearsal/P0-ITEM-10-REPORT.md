# P0 item 10 rehearsal report

## Result

The automated authority gate passes for one-, two-, and six-player cohorts. The retained bundle is
bound to source commit `fa1b80b84959dc62012c209c4749c6e31abde8ec`, release
`morrow.rehearsal.fa1b80b.p0-10.v1`, campaign/player identities, and an exact artifact-set hash.

Bundle SHA-256: `83b3a539c862300aba53492d7d7763aca6cb02f197d162f2ca2f0c85cd07e429`

## Proven automatically

- The fifteen-stage slice sequence is retained from Copperline discovery through the payload-free
  Discord group receipt and the later capture offer. No later-capture authorization event exists.
- Each cohort retains ten ordered, hash-chained event receipts and reconstructs the journal after
  every durable boundary.
- Exact M03 timing is 740 ticks/37 seconds. M04 receipts state the 45-second bound, two-tick sampling,
  cohort count, and that raw samples are not remotely projected.
- Wrong, partial, reordered, cancel, decline, duplicate, altered-collision, wrong release, wrong
  campaign, wrong player, disconnect/rejoin, and cursor-loss cases fail closed.
- Copperline, Discord, and database-service outage models retain local authority, retry in order, and
  deliver once after recovery.
- Private evidence hashes remain isolated to their intended linked player; the group receipt states
  that it contains no private payload.
- Static runtime probes retain display-entity fallback, captioned/pulsed audio equivalence, safe
  spawn/exit cells, no Room 04 inventory grant/removal, and entity/task cleanup markers.
- The rehearsal network guard observed no connection attempt and denies any non-loopback target.
- An actual Paper 1.21.11 build 132 server boot is retained from source checkpoint
  `c40f916aefb8dedf7c459a6636be92397fb0ebb1`. All 18 local `paper.jar` candidates were hashed; every
  candidate was the exact pinned SHA-256
  `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba`. The selected source and its
  `cache`, `libraries`, and `versions` trees were copied into a new create-only target, verified before
  boot, and byte-identical after both server lifecycles.
- The plugin JAR SHA-256 was
  `38fac760c9ba3fc4ca652c5f1a59ec62cbf510157165e19c35942ba9f223977f`. First boot installed and
  audited Room 04; restart read the same manifest/snapshot as already present. PDC-owned entity counts
  remained body 2, Static Restore 26, and Entity Replay 6 across restart, with clean runtime shutdown
  logged after each lifecycle.
- The real async projector reached only the loopback HTTPS ingest fixture, received two retryable 503
  responses, then one authenticated 200 response. Its durable cursor remained at sequence 4 across
  restart with no redelivery. Routine Paper public-key/version lookups were forced through a closed
  loopback proxy and failed; no bootstrap file was downloaded or changed.

## Graphical-client subproof and remaining lanes

A real graphical Minecraft 1.21.11 client now connects to pinned Paper build 132 through loopback.
Its create-only launcher verified the vanilla manifest, client JAR, 75 libraries, asset index, and nine
Windows natives; it used a dummy offline identity, read no account files, loaded no production
credentials, and forced HTTP(S) through a closed loopback proxy. The first live join exposed an unsafe
spawn defect. The patched entry controller then recovered a fresh bounded client from `(0, -63, -2)`
to the authored safe position `(0.5, 80.0, -1.5)` before damage. A hardened fresh target then filled
all 36 main-inventory slots with 2,304 items, disconnected, rejoined the same player at the safe cell,
and found the same 2,304 items. Both bounded clients avoided suffocation and emitted finalized hashed
logs plus clean disconnect/runtime-close receipts.

Windows' primary capture API still returned
`SetIsBorderRequired failed: No such interface supported (0x80004002)` on build 19045. A bounded
exact-title/PID fallback then captured the unobscured visible Java window without injecting input. The
first joined frame exposed default-scale TextDisplay labels covering the focal build. The implementation
now applies bounded scales, places `MORROW // TERMINAL` above the copper terminal, and moves its
interaction surface to the spawn-facing front. A rebuilt plugin passed all self-tests; a fresh joined
client visibly showed the corrected layout, and one observed right-click opened the native
`Static restoration proposal` dialogue. The before/corrected/dialog images, capture receipts, launcher
receipt, and clean server lifecycle are hash-bound in
`client-visual/2026-08-30-exact-window-visual-checkpoint.json`.

This extends the safe-entry result with a real visual/readability and one-dialog-activation subproof,
not a complete graphical lane. All poses and interpolation, the full mouse/keyboard/Escape dialogue
matrix, resource-pack-decline parity, audio-disabled parity, full-inventory route and exit/respawn,
two-/six-player physical cleanup, continuous synchronized media, and 60–90 minute pacing without
operator narration remain required. `CLIENT-REHEARSAL.md` and the bound generator/checker define the
evidence needed to finish those six lanes without overclaiming this checkpoint.

The database proposal now also exists as a CLI-generated numbered migration and passed an isolated
Supabase rehearsal: live role/RLS denial, owner projection, service ingest, exact payload and token
checks, concurrent idempotency, altered collision, payload limits, outbox projection, transactional
rollback, and security/performance advisors. The validation project was paused afterward. The durable
receipt is under `database/2026-08-30-isolated-supabase.json`. A real authenticated browser now also
proves anonymous withholding, owner-only RLS, checksum and handoff receipts, wrong-token refusal, and
all six Act 1–2 ticket cards. The automatic database projector subsequently applied nine ordered events
to both linked players, kept actor receipts private, leaked no raw payload, rejected wrong/stale workers,
reclaimed an expired lease, and scheduled bounded retry. The browser lane now has real provider email,
disposable-inbox receipt, and `/verify` evidence. That run exposed a real callback defect: the Server
Action did not preserve its PKCE verifier cookie, so the returned auth code was correctly refused.
The request path now uses a same-origin POST route that explicitly forwards Supabase's cookie mutations;
its final inbox-to-owner-case retest remains open after the built-in provider rate-limited the immediate
second request. The primary delivery transport is no longer the
Node fallback: a private Supabase Cron job automatically applied all nine events in its ten-second
window, wrote a payload-free run receipt, reported healthy, disabled cleanly, and did not run again.
A real Discord bot now also proved the raw Gateway transport in a create-once bot-only channel: READY,
exact authored post, matching `MESSAGE_CREATE`, REST readback, stable nonce, zero mentions, and verified
channel deletion. The normal worker remained off, no production database or player channel was touched,
and the existing two-member guild itself was not disposable. The remaining Discord gate is therefore
narrower but still open: exercise the database-claimed worker in a disposable guild and complete an
actual linked-player ephemeral interaction. No production Morrow system was enabled.

The actual runtime receipt and logs are retained under `runtime/p0-paper-c40f916`; the
machine-authoritative split remains in `receipts/p0-item10-fa1b80b/launch-matrix.json`.
