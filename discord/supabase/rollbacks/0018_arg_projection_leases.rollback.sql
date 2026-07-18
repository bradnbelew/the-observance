begin;

drop function if exists public.observance_complete_arg_projection(uuid,text,uuid,boolean,text);
drop function if exists public.observance_claim_arg_projections(text,integer,integer);

update public.arg_event_projections
set status = 'queued', lease_token = null, lease_expires_at = null, next_attempt_at = null
where status = 'processing';

alter table public.arg_event_projections
  drop constraint if exists arg_event_projections_status_check;
alter table public.arg_event_projections
  add constraint arg_event_projections_status_check
  check (status in ('queued', 'applied', 'failed'));
alter table public.arg_event_projections
  drop column if exists lease_token,
  drop column if exists lease_expires_at,
  drop column if exists next_attempt_at;

drop index if exists public.idx_arg_event_projections_work;
create index idx_arg_event_projections_work
  on public.arg_event_projections(surface, status, updated_at)
  where status in ('queued', 'failed');

commit;
