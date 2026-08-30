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
an actual runtime receipt. A bounded graphical 1.21.11 client has now proven Room 04 safe-entry recovery
without suffocation, but Windows visual capture remains unavailable. The six complete graphical lanes
remain required and use the create-only protocol
in `CLIENT-REHEARSAL.md`; its validator refuses screenshots or prose without matching journal, world,
inventory, cleanup, and complete server-lifecycle evidence.

After Java access was explicitly allowed, a second bounded dummy-identity client was launched and the
exact `Minecraft 1.21.11` Java window was selected separately from the launcher. Windows still returned
`SetIsBorderRequired ... 0x80004002`; no blind input was sent. The retained retry receipt is
`client-attempts/2026-08-30-java-permission-capture-unavailable.json`, so the visual gate remains
honestly blocked by capture availability rather than Java launch permission. The host is Windows 10
build 19045; Microsoft documents `GraphicsCaptureSession.IsBorderRequired` from build 20348, so the
failure is a feature-availability mismatch in the capture helper.

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
