# M2 Disposable Supabase Validation

Status: **BLOCKED ON DISPOSABLE DATABASE RUNTIME — SAFE LOCAL HARNESS READY**

Date: 2026-07-16 (America/Chicago)

This record closes no production, M4, or platform gate. It documents the attempt to execute the reviewed
M2 proposal from checkpoint `d63c48cca3b2964aea1513f704574855b4bf8a72` and the safe validation tooling
added when no disposable target was available.

## Platform and runtime confirmation

- The connected Supabase account exposed only project `fdnmhbpxnodrnbrzrlqq`, organization
  `yaenkbhruvrgkxqvngbk`, region `us-east-2`, status `ACTIVE_HEALTHY`, Postgres `17.6.1.127`. Repository
  authority identifies that project as production. It was queried for metadata only and was never used
  as a migration/test target.
- The branch-list connector call returned `Project reference is missing when validating permissions`, so
  it did not confirm a disposable platform branch. No branch was created, reset, merged, or deleted.
- No other Supabase project was visible. Hosted migration IDs and hosted receipts therefore do not exist.
- `supabase` was not installed on `PATH`, and Docker was not installed on `PATH`. npm registry metadata
  reported official CLI package version `2.109.1`, but both latest and pinned `npx` binary bootstrap
  attempts hung without producing a CLI executable or version receipt and were terminated. No migration
  filename was invented.

## Safe local validation harness

`tools/validate_m2_supabase.ps1` now provides the missing executable test path when Docker and the
official CLI are available:

1. accept only `-Target local`, reject every project ref, and hard-block production ref
   `fdnmhbpxnodrnbrzrlqq`;
2. inspect current CLI help before use;
3. initialize a temporary local Supabase workspace and create every migration through
   `supabase migration new`;
4. copy the reviewed `contract-v1.up.sql`, `contract-v1.rollback.sql`, and
   `contract-v1.forward.sql` bytes into those temporary CLI-created migrations;
5. apply an empty disposable baseline for the pre-existing `players` and `settings` dependencies;
6. run forward, preservation rollback, forward recovery, and a second forward to prove no duplicate
   outbox effect;
7. run pgTAP checks for RLS plus forced RLS, anon/authenticated revokes, service-role privileges,
   append-only finding/choice behavior, idempotency, and the exact `37020…` → `16de…` / `d2eec3…` chain;
8. run both security and performance advisors; and
9. write a JSON receipt containing the real CLI version, Git commit, CLI-created migration IDs, test
   output, advisor output, and local start/reset receipts.

The harness self-test passes without Docker and proves only the target guard and committed input set.
It is not a database receipt. A future run must commit the generated JSON receipt before this blocker is
marked closed.

## Remaining gate

Run the harness on an actual local Supabase instance, or separately create and platform-confirm a
disposable Supabase branch with explicit cost/write approval and an equivalent production-ref guard.
Only a passing real database run with both advisor outputs may close the carried M2 gap. Never apply
`design/m2/sql/contract-v1.up.sql` directly to production.
