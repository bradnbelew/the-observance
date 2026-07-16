# M2 Technical Contracts and Isolated Implementation

Status: **M2 CONTRACT AUTHORITY — ISOLATED/NON-PRODUCTION**

Authority order remains `SPINE-LOCK.md` → approved conformance → approved Phase 1 → Phase 2 evidence
architecture → this file. Nothing here authorizes production mutation, Hold geometry, media editing,
Crafty access, or deployment.

## Release identity and authority activation

A release is the tuple `(git_commit, manifest_version, predicate_raw_sha256, schema_version)`. Paper,
Supabase, Vercel, both Railway families, Discord, and media projections must return the same tuple.
Missing, stale, or conflicting fields fail closed. Preview/local receipts never count as production.

Predicate authority uses two hashes:

- `raw_sha256` identifies exact deployable bytes and is checked at source, package, database, and load.
- `semantic_sha256` identifies canonical JSON meaning and may prove that a byte-only reconciliation did
  not change a predicate. It never substitutes for the raw release hash.

Activation is compare-and-swap: fully read bytes, verify raw and semantic hashes, parse and validate all
nodes/bindings, prove predecessor/rollback links, then atomically replace the immutable runtime snapshot.
An error before the swap leaves the old snapshot active. An idempotency key names one exact transition;
reusing it for different bytes fails. Rollback performs the same validation in reverse and never edits an
active snapshot in place.

`predicate-authority-chain.json` records the exact historical `37020…` bytes and the LF `16de…` bytes.
The former was a mixed-line-ending build artifact; the JSON meaning is identical. `.gitattributes`
makes LF authoritative, and isolated release migration changes the setting to `16de…`. Production still
records `37020…` until a later confirmed cutover; M2 does not mutate it.

## Logical manifest contract

`generated/manifest-index.json` routes six generated authorities. The generator reads Phase 2 and the
frozen V5 CSVs; it is deterministic and checked for source hashes.

- Arcs P1–P12 are ordered, with free interior work by any subset.
- There are 68 logical findings derived from the approved evidence chains. The number is descriptive,
  not a quota; changing evidence may change it through a new manifest version.
- Every finding cites a versioned observation set with custody, replay, and spoiler boundary.
- Artifact identity/provenance survives a mechanism change. Paper is primary; reissue requires absence
  proof and a duplicate scan.
- Exactly two protected choice dimensions exist: Wren remembrance and Averyn name treatment. Automation
  cannot select either. Every valid path closes the Record and frees Averyn.
- Exactly three ontological ambiguities remain unresolved.

## Legacy import, rollback, and recovery

All 82 legacy IDs and completion flags appear exactly once in `generated/legacy-import-map.json`.
Legacy completion imports are provenance, not automatic completion of a broader Phase 2 finding. A
finding may be promoted only after all of its declared propositions are re-evaluated against preserved
source receipts. This conservative rule prevents a narrow old station from completing a richer finding.

Reuse rows retain an alias readback; map/merge rows fan evidence into named findings; retired rows are
tombstones with no executable handler. No row or receipt is deleted. Rollback re-enables the frozen V5
binding/flag view. Forward recovery replays imports by stable idempotency key and never duplicates an
artifact, contributor, or reward.

## Local-primary state and reconciliation

Paper persists physical state before presentation or remote projection. A local transaction contains:

1. monotonic local sequence and previous-state hash;
2. manifest and predicate version;
3. observations/findings with source receipt IDs and contributor history;
4. artifact custody and absence proof;
5. route/repair/gate state;
6. protected choice commits and finale phase;
7. a bounded outbox entry with a stable idempotency key.

The state file is written to a sibling temporary file, flushed, and atomically moved over the previous
file. On restart, Paper verifies the hash chain before loading. Supabase is an asynchronous mirror.
Outbound retry is at-least-once and effects are exactly-once through unique idempotency keys. Inbound
reconciliation accepts only identity, exact unexpired approved hints, and committed cross-surface
receipts; it cannot close a physical gate or overwrite a local choice.

Outages preserve already-open routes and local work. A new cross-surface action pauses only at the
boundary that cannot be durably acknowledged. After recovery, outbox replay, remote receipt readback,
and a reconciliation receipt prove whether the effect was inserted or already present.

`LocalPrimaryJournal` is the isolated executable proof of that boundary. Its self-test covers restart,
same-key/same-byte idempotency, same-key/different-byte rejection, cursor catch-up, forced file writes,
atomic replacement, and tamper rejection. It is a contract component for the later campaign store, not
an assertion that the current production world has been migrated.

## Schema and incremental migration

`sql/contract-v1.up.sql` is the reviewed M2 schema proposal. It is not in the executable migration
directory and was not applied because no local Supabase/Docker target or existing development branch was
available. The official CLI scaffold attempt was blocked before file creation; production was untouched.

The proposal is additive and service-role-only. Every public table enables and forces RLS and explicitly revokes
`anon`/`authenticated`; grants do not rely on changing Supabase defaults. It stores manifest versions,
predicate chain, group receipts, contributors, artifacts, choices, hint approvals/expiry, media reveal,
outbox/reconciliation, legacy imports, and same-release parity receipts. Append-only triggers protect
receipts and choices. Referencing columns and pending outbox scans have explicit indexes; activation
retires the old partial-unique row before promoting the new row.

`contract-v1.rollback.sql` is a non-destructive compatibility rollback: it restores the prior active
release pointer and predicate hash while preserving all new rows. `contract-v1.forward.sql` validates
the retained rows, reactivates M2, and queues only missing projections. No rollback drops evidence.

Before any later branch application: create the executable migration with the then-current Supabase CLI,
apply it to a disposable branch/local database, run database tests and both advisors, prove rollback and
forward recovery, and record the branch/migration IDs. Never apply this proposal directly to production.

## Hint and automation gates

Only A0 readback/projection and A1 non-personal, text-free, ephemeral ambience may run automatically.
A1 selection may enforce cooldowns, quiet zones, and accessibility suppression, but may not use names,
identity, isolation, route, inventory, history, or inferred attention to target a player.

A2–A5 require a stored approval for exact payload hash, class, target/scope, prerequisite snapshot,
approver, issue time, and expiry. A4 deterministic effects caused immediately by the declared player
predicate are recorded as player-earned, not ambient automation. A5 additionally requires player action
and an operator arm. `name_on_wall` is A2 and never automatic.

H0 orientation is non-solution navigation. H1–H3 requests enter a pending queue. Approval binds an exact
authored body hash, tier, group, finding, and expiry; it expires when the finding closes. No elapsed-time
escalation exists, and no hint mutates progression or places evidence.

The Discord `/whisper` implementation stages the exact authored body as a pending A3 request keyed to
the Discord interaction. It does not spend, record delivery, reveal text, or queue a toll. The shared
TypeScript gate rejects changed payload, class, scope, missing approval, or expiry. Paper independently
recomputes the canonical authored-payload hash before any allowlisted A2/A3 row reaches the enactor.

## Survival gear and one-runtime policy

The gated prologue remains the only player-facing standalone save. Post-prologue play has one
Crafty-managed Paper runtime. Players retain survival inventory. Protected campaign regions reject
block/entity/container/teleport/route/gate bypasses; they do not use inventory escrow as a mode switch.
The M3 slice is private Brad review material and cannot become a second progression runtime.

## External evidence still required

- Supabase disposable branch/local application, database tests, advisors, rollback, and forward receipts.
- Preview Vercel deployment receipt for the exact M2 release tuple.
- Authenticated IDs and non-production receipts for both Railway families.
- Disposable Paper restart and non-op survival-gear region tests.
- Source custody/human AV review for Brad's footage and packet.
- No Crafty, media-byte, production, or deployment evidence is claimed by M2.

## M2 validation receipt

- `python tools/check_m2_contracts.py` passes deterministic manifest hashes/counts, all 82 imports,
  exact `37020...` reconstruction, `16de...` seed parity, schema/security/rollback, seven surfaces,
  choices, ambiguities, and automation boundaries.
- `gradlew clean check` passes the complete plugin suite, including predicate activation/rollback,
  exact approval payload verification, and local-primary restart/idempotency/tamper tests.
- Discord typecheck, approval self-test, voice check, ordered seed regeneration, and bundle check pass.
- Phase 1/2, integrity, freshness, V5 content, all 60 predicates, Hold layout/fixture/book/content, and
  1,588 deterministic scenarios pass.
- Secret-dependent Discord/Supabase integration runners were not executed because this isolated
  worktree has no service credentials. No secret was requested, copied, printed, or synthesized.
