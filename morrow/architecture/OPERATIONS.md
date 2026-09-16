# Morrow runtime operations

> **Activation paused as of 2026-09-14.** These procedures describe the superseded M01–M12 runtime
> and are retained for engineering reference only. Do not execute activation, migration, external
> validation, or production steps until this file is rewritten for G01–G15 and separately authorized.

## Copperline projector topology

The primary projector runs inside Postgres through Supabase Cron:

```text
Paper / Copperline event -> private event ledger -> Copperline outbox row
  -> pg_cron every 10 seconds -> morrow_private.run_copperline_projector
  -> owner-filtered public projection -> authenticated case refresh
```

This is the production path because it does not move a service-role secret across HTTP. PostgreSQL
row locks and `SKIP LOCKED` make overlapping cron invocations safe, while event idempotency makes a
duplicate scheduler delivery harmless. Supabase records scheduler execution in
`cron.job_run_details`; Morrow records non-empty runs without payloads in
`morrow_private.projector_run_receipts`.

The Node worker at `dashboard/scripts/morrow-copperline-projector.mjs` remains a bounded operator
fallback. It is not required for routine production delivery.

Current platform references:

- <https://supabase.com/docs/guides/cron>
- <https://supabase.com/docs/guides/functions/schedule-functions>

## Activation boundary

Schema application never creates a cron job. Activation is a separate, reversible operator action.
Before enabling a release:

1. Confirm the target project reports `ACTIVE_HEALTHY`; SQL reachability during `COMING_UP` is not
   sufficient. The isolated rehearsal proved that writes made before the final restore phase can be
   replaced.
2. Take and verify the required production backup.
3. Verify the numbered migration hash and apply it through the approved production workflow.
4. Enable the Supabase Cron integration (`pg_cron`) for the project.
5. Verify exactly one campaign for the release is `ready` or `running`, all linked identities exist,
   and every linked player has `case_access`; the handoff event additionally requires
   `server_handoff`.
6. Enable the job with the exact release acknowledgement:

```sql
select morrow_private.configure_copperline_projector_schedule(
  'morrow.production.release-id',
  true,
  'enable:copperline:morrow.production.release-id'
);
```

The fixed schedule is ten seconds, the batch limit is 25, and only the database owner can execute the
private runner/configuration functions. `anon`, `authenticated`, and `service_role` have no EXECUTE
privilege on them.

## Health and incident response

Read the payload-free health projection:

```sql
select morrow_private.copperline_projector_health('morrow.production.release-id');
```

`healthy` means no failed/dead-letter rows and no pending row older than two minutes. `degraded` means
a retry is pending or an item is too old. `halt` means at least one dead-letter row requires operator
review. Inspect scheduler execution separately:

```sql
select status, return_message, start_time, end_time
from cron.job_run_details
where jobid = (
  select jobid from cron.job
  where jobname = 'the jobName returned by the enable receipt'
)
order by start_time desc
limit 20;
```

On a real incident, pause the campaign first, then disable the schedule with the exact acknowledgement:

```sql
select morrow_private.configure_copperline_projector_schedule(
  'morrow.production.release-id',
  false,
  'disable:copperline:morrow.production.release-id'
);
```

Do not delete ledger events or public projection rows to recover a delivery. Repair the missing
identity/case envelope or the underlying code, preserve the failed receipt, and requeue only the exact
reviewed outbox row through an approved operator migration. Re-enable only after health and ownership
queries pass. Disabling removes the named cron job transactionally; the isolated rehearsal confirmed
that no additional run occurs after the next schedule window.

## Shutdown and rehearsal hygiene

- A rehearsal schedule must be disabled before the validation project is paused.
- Validation users, sessions, and secrets remain isolated from production.
- The shipped Paper gate remains `morrow-reboot.enabled: false` until every launch-matrix lane passes.
- A paused validation project must be treated as cold state. After restore, wait for
  `ACTIVE_HEALTHY`, read back the schema, and reapply the numbered migration if the rehearsal schema
  is absent before creating any fixture or schedule.
