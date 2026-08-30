# P0 item 10 rehearsal report

## Result

The automated authority gate passes for one-, two-, and six-player cohorts. The retained bundle is
bound to source commit `fa1b80b84959dc62012c209c4749c6e31abde8ec`, release
`morrow.rehearsal.fa1b80b.p0-10.v1`, campaign/player identities, and an exact artifact-set hash.

Bundle SHA-256: `b3e5faf300ad1a17be6557f9c68379d07540b69a5b61b51398193a74b1968b4e`

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

This is a safe-entry runtime subproof, not a complete graphical lane. Entity interpolation,
native-dialog interaction, resource-pack-decline parity, audio-disabled parity, full-inventory route,
exit/respawn, two-/six-player physical cleanup, and 60–90 minute pacing without
operator narration still require synchronized visual and machine receipts. The installed Microsoft
launcher did open during the first loopback-only attempt on 2026-08-30, but Windows
returned `SetIsBorderRequired failed: No such interface supported (0x80004002)` for both capture
attempts and exposed no accessibility tree or focused element. No blind input was sent. Paper then
stopped cleanly with `MORROW_RUNTIME_CLOSED`. Both attempts are retained under `client-attempts`;
`CLIENT-REHEARSAL.md` and the bound generator/checker define the exact evidence required to finish the
six lanes without overclaiming the working safe-entry subproof.

The database schema remains a proposal. An isolated database target, authenticated browser session,
and disposable Discord guild/worker were not available, so live RLS/concurrency, browser projection,
and Gateway delivery are not claimed. No production system was contacted or enabled.

The actual runtime receipt and logs are retained under `runtime/p0-paper-c40f916`; the
machine-authoritative split remains in `receipts/p0-item10-fa1b80b/launch-matrix.json`.
