-- Crash-safe, bounded cross-surface delivery leases for the generic ARG event outbox.
-- This is event choreography, not a puzzle/mechanism taxonomy. Owning adapters still decide
-- what an event means and correct knowledge remains independent of source-touch receipts.

begin;

alter table public.arg_event_projections
  add column if not exists lease_token uuid,
  add column if not exists lease_expires_at timestamptz,
  add column if not exists next_attempt_at timestamptz;

alter table public.arg_event_projections
  drop constraint if exists arg_event_projections_status_check;
alter table public.arg_event_projections
  add constraint arg_event_projections_status_check
  check (status in ('queued', 'processing', 'applied', 'failed'));

create or replace function public.observance_claim_arg_projections(
  p_surface text,
  p_limit integer default 10,
  p_lease_seconds integer default 30
)
returns table (
  event_id uuid,
  event_key text,
  source text,
  payload jsonb,
  occurred_at timestamptz,
  lease_token uuid,
  attempts integer
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if p_surface not in ('minecraft','copperline','discord','dashboard','media','npc')
     or p_limit < 1 or p_limit > 100
     or p_lease_seconds < 5 or p_lease_seconds > 300 then
    raise exception 'invalid ARG projection lease request';
  end if;

  return query
  with candidates as (
    select p.event_id, p.surface
    from public.arg_event_projections p
    join public.arg_events e on e.event_id = p.event_id
    where p.surface = p_surface
      and p.attempts < 8
      and (
        p.status = 'queued'
        or (p.status = 'failed' and (p.next_attempt_at is null or p.next_attempt_at <= now()))
        or (p.status = 'processing' and p.lease_expires_at <= now())
      )
    order by e.occurred_at, p.event_id
    for update of p skip locked
    limit p_limit
  ), leased as (
    update public.arg_event_projections p
    set status = 'processing',
        attempts = p.attempts + 1,
        lease_token = gen_random_uuid(),
        lease_expires_at = now() + make_interval(secs => p_lease_seconds),
        next_attempt_at = null,
        last_error = null,
        updated_at = now()
    from candidates c
    where p.event_id = c.event_id and p.surface = c.surface
    returning p.event_id, p.lease_token, p.attempts
  )
  select e.event_id, e.event_key, e.source, e.payload, e.occurred_at,
         l.lease_token, l.attempts
  from leased l
  join public.arg_events e on e.event_id = l.event_id
  order by e.occurred_at, e.event_id;
end;
$$;

create or replace function public.observance_complete_arg_projection(
  p_event_id uuid,
  p_surface text,
  p_lease_token uuid,
  p_applied boolean,
  p_error text default null
)
returns boolean
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  changed integer;
begin
  if p_surface not in ('minecraft','copperline','discord','dashboard','media','npc')
     or p_event_id is null or p_lease_token is null then
    raise exception 'invalid ARG projection completion';
  end if;

  update public.arg_event_projections p
  set status = case when p_applied then 'applied' else 'failed' end,
      lease_token = null,
      lease_expires_at = null,
      next_attempt_at = case when p_applied then null
        else now() + make_interval(secs => least(300,
          (5 * power(2, greatest(0, p.attempts - 1)))::integer)) end,
      last_error = case when p_applied then null
        else left(coalesce(nullif(btrim(p_error), ''), 'projection failed'), 500) end,
      updated_at = now()
  where p.event_id = p_event_id
    and p.surface = p_surface
    and p.status = 'processing'
    and p.lease_token = p_lease_token;
  get diagnostics changed = row_count;
  return changed = 1;
end;
$$;

revoke all on function public.observance_claim_arg_projections(text,integer,integer)
  from public, anon, authenticated;
revoke all on function public.observance_complete_arg_projection(uuid,text,uuid,boolean,text)
  from public, anon, authenticated;
grant execute on function public.observance_claim_arg_projections(text,integer,integer)
  to service_role;
grant execute on function public.observance_complete_arg_projection(uuid,text,uuid,boolean,text)
  to service_role;

drop index if exists public.idx_arg_event_projections_work;
create index idx_arg_event_projections_work
  on public.arg_event_projections(surface, status, next_attempt_at, updated_at)
  where status in ('queued', 'processing', 'failed');

commit;
