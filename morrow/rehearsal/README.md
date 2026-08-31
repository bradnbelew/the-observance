# Morrow disposable rehearsal

This directory retains the fail-closed P0 vertical-slice rehearsal. It is local-only, create-only,
and does not enable the shipped Morrow feature gates.

Run the deterministic authority lane from repository root:

```text
python tools/run_morrow_p0_rehearsal.py
python tools/check_morrow_rehearsal.py
```

The runner binds every receipt to source commit `fa1b80b84959dc62012c209c4749c6e31abde8ec`,
release `morrow.rehearsal.fa1b80b.p0-10.v1`, the cohort identities, campaign identity, and the
SHA-256 set of all exercised authorities. It refuses non-loopback network access. Regeneration is
byte-for-byte deterministic at the retained timestamp.

The `launch-matrix.json` receipt is authoritative about proof limits. A headless contract result is
not a substitute for an actual Paper boot or a graphical client observation. The Paper lane now has
an actual runtime receipt. A bounded graphical 1.21.11 client has proven Room 04 safe-entry recovery
without suffocation. A Windows 10 exact-title/PID fallback now also retains the joined Room 04 frame,
the presentation defect it exposed, the corrected frame, and one successful native terminal-dialog
activation under `client-visual/2026-08-30-exact-window-visual-checkpoint.json`.

The earlier `SetIsBorderRequired ... 0x80004002` failures remain retained under `client-attempts` as
historical evidence about the primary capture API. The fallback captures only one unobscured visible
window and injects no input; exact-window input was separately limited to two reversible F1 toggles and
one observed terminal right-click. This bounded checkpoint does not satisfy continuous synchronized
media, all poses/dialog paths, parity modes, or pacing. The separate retained
`client-cohort-runtime/latest.json` checkpoint now proves real vanilla 1.21.11 one-, two-, and
six-client concurrent joins, mutation-free safe entry, all 2,304 main-inventory items per player
before disconnect and after Paper restart, stable per-player UUIDs, no projector redelivery, and clean
owned client/server shutdown. It is verified by
`python tools/check_morrow_client_cohort_runtime_checkpoint.py`. The complete 60–90 minute human slice,
continuous media, independent observation, and the other unresolved graphical lanes remain required
under `CLIENT-REHEARSAL.md`.

The retained `client-cohort-prep/2026-08-30-offline-cohort-preparation.json` checkpoint proves the
installed vanilla client can be prepared as distinct one-, two-, and six-player cohorts without
launching a GUI or contacting the server. It retains only JSON/options evidence—not client binaries—
and is verified by `python tools/check_morrow_offline_cohort_receipts.py`. This is setup evidence,
not a substitute for the complete human-client runs.

The retained `resource-pack/latest.json` checkpoint binds a matched real-client `LOADED`/`DECLINED`
pair against the same exact optional loopback ZIP. It proves handshake, byte-fetch, policy, and cleanup
behavior only; the accepted/declined visual and interaction equivalence lane remains open until matched
media receives independent review. The first exact-window pair is retained at
`client-visual/2026-08-30-resource-pack-visual-pair.json`: common room geometry and interaction targets
are visible. The unequal right-side overlay is the vanilla first-join movement tutorial captured at
different slide-animation phases, not a Morrow-authored prompt; both final option files bind
`tutorialStep:movement`. The checkpoint still does not pass full Morrow visual or interaction parity.

The isolated database lane is proven separately by
`database/2026-08-30-isolated-supabase.json`. It binds the CLI-generated migration to the rehearsed
proposal and records live RLS/role, ownership, RPC, concurrency, idempotency, collision, payload-bound,
projection, rollback, and advisor results. The validation project was paused after evidence capture;
production application remains blocked.

The authenticated Copperline browser now has a retained partial receipt at
`browser/2026-08-30-authenticated-copperline.json`. It proves anonymous withholding, owner-only RLS,
real checksum and handoff mutations, wrong-token refusal, outbox creation, six rendered Act 1–2
updates, and synthetic-auth cleanup. The automatic database projector now has separate live isolated
proofs covering ordered leases, two-player projection, retry/reclaim, worker ownership, payload
privacy, and an actual ten-second Supabase Cron schedule. The schedule ran, recorded health/run
receipts, disabled transactionally, and produced no later invocation. The Node worker is now only a
fallback. The browser lane has real delivery, disposable-inbox receipt, and provider verification;
its remaining gate is the fresh post-fix PKCE callback through the owner-only case read.

The Discord transport now has a retained partial receipt at
`discord/2026-08-30-gateway-private-channel.json`. A real bot connected to Discord Gateway, created a
temporary everyone-denied/bot-allowed channel, posted the exact authored group receipt with a stable
nonce and no allowed mentions, observed the matching `MESSAGE_CREATE`, fetched the same message, and
deleted the channel; a follow-up read returned Discord `10003 Unknown Channel`. Morrow stayed disabled,
the normal worker and production database were never started, and no player channel or interaction was
contacted. This closes the raw Gateway/message/cleanup transport subproof, but not the launch lane: an
isolated service-role database worker and a disposable guild with a linked-player ephemeral interaction
remain required.
