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

A newer source-bound checkpoint at
`client-visual/2026-08-30-source-d2580fd-room04-checkpoint.json` binds commit
`d2580fd97ae14dc3db0b508d6ab7aad0b7f0241c`, the current plugin JAR, exact Paper
1.21.11 build 132, a fresh dummy-client join, the retained Room 04 frame, safe entry with zero
inventory mutations, and clean runtime shutdown. It confirms the terminal remains centered and its
label bounded after the full M01–M12 implementation. It is deliberately still-image proof: no input
was injected, so latest-source dialog choices, pose transitions, continuous media, pack/audio parity,
and the full playthrough remain open. Verify it with
`python tools/check_morrow_source_bound_visual_checkpoint.py`.

The current plugin commit `d7387880d8fe2e2d74235160fab2acd109bccca9` has a fresh exact Paper
1.21.11 build 132 lifecycle under `runtime/d738788-current`. It proves all M05–M12 structures load,
restart as already present with stable ownership, close cleanly, and preserve ordered projector
recovery across 503/503/200. That work also reproduced ambient oxidation failures in Room 04 and M05,
then retained an accelerated random-tick interval and post-stress restart under `copper-stability/latest.json`.
The interaction target then ran two dummy-client passes. One applied the native six-cell Static Restore
proposal and completed all three accessible passes without the former client-body safe halt. The second
opened B06 from its physical marker, showed harmless feedback for a wrong `authenticated` choice, then
filed `inferred` and created exactly one canonical receipt with `world_mutation=false`. Images, captures,
clients, server log, hash-chain journal, and Paper receipt are bound at
`client-visual/2026-08-31-latest-plugin-d738788-m02-interaction-checkpoint.json`. Review on this exact
build, media, pose, parity, and the operator-free full playthrough remain open. Verify these with
`python tools/check_morrow_latest_plugin_paper.py` and
`python tools/check_morrow_latest_plugin_visual_checkpoint.py`; verify the oxidation guard with
`python tools/check_morrow_copper_stability.py`.

Entity Replay now has a later, separately scoped client checkpoint. The M03/M04 acquisition build
9b2c3666d4199901a148a3a1ba75145f8bae7de7 rendered the native authorization, armed M03 on
boundary entry, accepted the designated transfer at tick 402 inside the disclosed 380–500 grace
window, exposed the illuminated M04 seal, retained a 45-second route, and rendered its reconstructed
echo. Commit cacfaa47453e3e260269c24b5bc958d4310dd360 then isolated the reconstructed proof marker;
a fresh exact Paper lifecycle and real-client click committed one behavior-reuse receipt for the same
M04 clip. The final proof target was mechanically seeded with the exact acquisition state, and the
route used disposable console positioning assists, so this proves authority and physical client
lifecycle—not ordinary unaided movement. Verify the journal, clips, consents, clients, captures, and
lifecycle with python tools/check_morrow_latest_plugin_entity_replay_checkpoint.py. Audio/pack cue
parity and the operator-free M01–M12 playthrough remain open.

The broad P0 checker also records one legacy retention defect at
`legacy-artifact-supersession.json`: the original P0 bundle hashed a working-tree
`BukkitMorrowBody.java` state that was never retained in Git. Only that unavailable hash is narrowly
superseded by the exact cacfaa4 source, plugin, and full Paper receipt; every other legacy artifact remains
byte-for-byte required.

The earlier `SetIsBorderRequired ... 0x80004002` failures remain retained under `client-attempts` as
historical evidence about the primary capture API. The fallback captures only one unobscured visible
window and injects no input; Computer Use handled the bounded observed gameplay actions. The current
retained checkpoint records proposal apply plus physical B06 wrong/correct choices and discloses the
disposable server-console view-positioning assist. The separate cacfaa4 checkpoint advances the
M03/M04 continuous replay loop, but it does not satisfy all poses/dialog paths, audio/pack parity,
ordinary unaided route movement, or pacing. The separate retained
`client-cohort-runtime/latest.json` checkpoint now proves real vanilla 1.21.11 one-, two-, and
six-client concurrent joins, mutation-free safe entry, all 2,304 main-inventory items per player
before disconnect and after Paper restart, stable per-player UUIDs, no projector redelivery, and clean
owned client/server shutdown. It is verified by
`python tools/check_morrow_client_cohort_runtime_checkpoint.py`. The complete 60–90 minute human slice,
continuous media, independent observation, and the other unresolved graphical lanes remain required
under `CLIENT-REHEARSAL.md`.

The human packet contract is now complete rather than P0-only:
`client-rehearsal-contract.json` binds M01–M12, all 26 canonical events, every canonical surface,
five proof paths per investigation, the six cross-cutting client lanes, and all four M12 endings.
Its self-test rejects an otherwise complete M01–M11 packet. During that expansion the audit exposed
and corrected M06's misleading event key; the forward-only local receipt is
`database/latest-m06-event-correction-local.json`.

The retained `version-rooms-runtime/latest.json` checkpoint proves the M05 damaged, scaffold, and
completed rooms build as 1,617 real Paper 1.21.11 cells, survive restart under the same manifest,
retain exactly three display labels, and shut down cleanly. Verify it with
`python tools/check_morrow_version_rooms_runtime_checkpoint.py`. This is server lifecycle evidence;
real-client console routing, native dialogue choices, Escape behavior, visual readability, and the
correct/wrong player paths remain open and are deliberately not claimed.

The retained `consensus-audit-runtime/latest.json` checkpoint independently proves the M06 vote
chamber builds as 1,309 real Paper cells, retains eight displays, survives restart under one stable
manifest, and closes cleanly. Verify it with
`python tools/check_morrow_consensus_audit_runtime_checkpoint.py`. The six private reads, left-click
dissent marks, native classification dialogue, wrong answers, Escape path, and public correction still
require real-client media; the server receipt does not claim those interactions.

The retained `witness-anchor-runtime/latest.json` checkpoint proves the M07 paired-original chamber
builds as 1,547 real Paper cells, preserves six arbitrary editable reconstruction cells and nine owned
displays across restart, and refuses foreign targets. The retained
`almost-home-runtime/latest.json` checkpoint proves the M08 5,225-cell source gallery and completed
house, eight displays, dynamic chronology lamps, and restart readback. Their private/public reads,
editing, cipher, provenance, authorization, wrong-answer, and Escape paths remain real-client gates.

The retained `account-continuity-runtime/latest.json` checkpoint proves the M09 2,520-cell safe lobby,
four dynamic state lamps, bounded echo scene, ten owned entities, semantic readback, restart, and clean
shutdown on Paper 1.21.11. Its authority tests cover salted private anchors, bounded action receipts,
wrong identity claims, crash recovery, and one-/two-/six-player paths. A real player's private dialog,
quit/rejoin, echo observation, and identity clicks remain explicitly unproven.

The retained `maintenance-window-runtime/latest.json` checkpoint proves the M10 5,301-cell split
chamber, two physical hopper endpoints, six state lamps, two contradictory Morrow display bodies,
eighteen owned entities, restart readback, and cleanup. Its authority tests cover fresh nonce transfer,
unsafe deletion refusal, non-consuming wrong/timeout paths, safe disconnect return, restart recovery,
and one-/two-/six-player cohorts. Tagged-item transfer and asymmetric co-op still require real-client
media; the server receipt does not claim those inputs.

The retained `cold-storage-runtime/latest.json` checkpoint proves the M11 6,417-cell bounded archive,
five custody stations, two isolated Morrow bodies, two physical tagged-hash bridge endpoints, nine
redundant state lamps, eleven owned entities, restart readback, and cleanup on exact Paper 1.21.11
build 132. Authority tests cover the first broken custody edge, correct/wrong cross-instance hashes,
separate reconstruction and access receipts, decline/reset/crash recovery, and one-/two-/six-player
cohorts while preserving identity continuity as unresolved. Real-client record dialogs, tagged-item
transport, two-instance comparison, caption/waveform readability, and Escape paths remain open.

The retained `branch-governance-runtime/latest.json` checkpoint proves the M12 8,325-cell finale,
bounded rollback-route geometry, five evidence stations, four physical rule stations, four ending
gates, thirteen redundant lamps, eighteen owned entities, restart readback, and cleanup. Authority
tests cover all four canonical endings, missing-category feedback, non-consuming invalid policies,
timed-pause evidence retention, separate governance consent, persistent coda, crash recovery, and
one-/two-/six-player cohorts. Real-client wave traversal, anchor/rule/ending dialogs, visual pacing,
governance/coda choices, reset, and Escape remain explicitly unproven.

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
