# P0 item 10 rehearsal report

## Result

The automated authority gate passes for one-, two-, and six-player cohorts. The retained bundle is
bound to source commit `fa1b80b84959dc62012c209c4749c6e31abde8ec`, release
`morrow.rehearsal.fa1b80b.p0-10.v1`, campaign/player identities, and an exact artifact-set hash.

Bundle SHA-256: `b44d27a6d57080314fdac7f5870ec8985f4116a436975da973b404c30ba62ce3`

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

## Not proven here

The pinned Paper server JAR and copied bootstrap cache are not present in this workspace, so no actual
disposable server boot is claimed. No graphical Minecraft client is available, so entity interpolation,
native-dialog interaction, resource-pack-decline parity, audio-disabled parity, full-inventory route
experience, 1/2/6-player physical cleanup, and 60–90 minute pacing without operator narration still
require human-client receipts.

The database schema remains a proposal. An isolated database target, authenticated browser session,
and disposable Discord guild/worker were not available, so live RLS/concurrency, browser projection,
and Gateway delivery are not claimed. No production system was contacted or enabled.

The machine-authoritative split is retained in `receipts/p0-item10-fa1b80b/launch-matrix.json`.
