-- 0011_showrunner_lock.sql — an atomic lease lock so two overlapping showrunner cron ticks can never
-- run concurrently.
--
-- WHY. `showrunner/state.ts`'s readState()/writeState() do a plain SELECT then a plain upsert of the
-- WHOLE `showrunner_state` jsonb row (every high-water mark this codebase's idempotency depends on —
-- dripped_keys, reported_customs, unlit_deep_last_reported_at, finale_posted, and a dozen more — lives
-- on this one row). That is the exact read-modify-write clobber `observance_merge_arc_flags` (0006) was
-- built to kill for `arc_state.flags` — but this row never got the same fix. If the recovery cron schedule
-- is ever shorter than a run's worst-case duration (a slow external call — Observer Tier-2's LLM,
-- Discord's API), two `main()` invocations can genuinely overlap: both read the same stale state, both
-- decide independently, and whichever writes last silently discards the other's advances — at best a
-- lost high-water mark (something re-fires next tick that shouldn't), at worst a real duplicate post
-- (unlit-deep.run.ts posts to Discord BEFORE writing its mark, so two overlapping runs could both post
-- the same toll). No code change here touches the many individual high-water-mark call sites — this
-- closes the whole class at its one root cause: overlapping runs.
--
-- WHAT. A lease-based compare-and-swap lock stored as one more row on the existing `settings` table (no
-- new table). `showrunner_try_acquire_lock` atomically sets `locked_at = now()` ONLY if the lock is free
-- or its lease has expired (a crashed run that never released is a stale lease, not a permanent
-- deadlock — self-healing, no manual unstick needed). `showrunner_release_lock` clears it. Both are
-- `security definer`, `service_role`-only, matching 0006's `observance_merge_arc_flags` convention
-- exactly. `--dry-run` (writes nothing, meant to be safely run any time as a diagnostic) never touches
-- the lock at all — only a real tick contends for it.
--
-- Additive + idempotent: `create or replace function`, `insert ... on conflict do nothing`. Safe on a
-- fresh DB or a re-run.

create or replace function public.showrunner_try_acquire_lock(p_lease_seconds int default 600)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_rows int;
begin
  insert into public.settings (key, value)
    values ('showrunner_lock', jsonb_build_object('locked_at', null))
    on conflict (key) do nothing;

  update public.settings
     set value = jsonb_build_object('locked_at', now()),
         updated_at = now()
   where key = 'showrunner_lock'
     and (
       value->>'locked_at' is null
       or (value->>'locked_at')::timestamptz < now() - make_interval(secs => p_lease_seconds)
     );

  get diagnostics v_rows = row_count;
  return v_rows > 0;
end;
$$;

comment on function public.showrunner_try_acquire_lock(int) is
  'Atomic lease lock: acquires the showrunner tick lock iff free or the previous lease expired '
  '(p_lease_seconds, default 600s — a crashed run self-heals, never a permanent deadlock). Returns '
  'true if THIS call acquired it. Called by run.ts before a real (non-dry-run) tick.';

create or replace function public.showrunner_release_lock()
returns void
language sql
security definer
set search_path = public
as $$
  update public.settings
     set value = jsonb_build_object('locked_at', null),
         updated_at = now()
   where key = 'showrunner_lock';
$$;

comment on function public.showrunner_release_lock() is
  'Releases the showrunner tick lock. Called in a finally block so a thrown error still frees it '
  '(the lease timeout is the backstop if release itself never runs).';

revoke all on function public.showrunner_try_acquire_lock(int) from public;
revoke all on function public.showrunner_release_lock() from public;
grant execute on function public.showrunner_try_acquire_lock(int) to service_role;
grant execute on function public.showrunner_release_lock() to service_role;
