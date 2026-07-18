-- Open-ended, append-only campaign event ledger and cross-surface projection queue.
-- Event definitions describe story consequences, never a finite puzzle/mechanism taxonomy.
-- Correctness remains with each owning adapter; observation receipts are not consulted here.

begin;

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.arg_event_definitions (
  event_key text primary key check (event_key ~ '^p(1[0-2]|[1-9])\.[a-z0-9_]+$'),
  phase_key text not null check (phase_key ~ '^P(1[0-2]|[1-9])$'),
  prerequisite_events text[] not null default '{}',
  source_surfaces text[] not null,
  projection_surfaces text[] not null,
  automation text not null default 'A1' check (automation in ('A0', 'A1')),
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (cardinality(source_surfaces) > 0),
  check (cardinality(projection_surfaces) > 0),
  check (source_surfaces <@ array['minecraft','copperline','discord','dashboard','media','npc']::text[]),
  check (projection_surfaces <@ array['minecraft','copperline','discord','dashboard','media','npc']::text[])
);

create table if not exists public.arg_events (
  event_id uuid primary key default gen_random_uuid(),
  event_key text not null references public.arg_event_definitions(event_key) on delete restrict,
  idempotency_key text not null unique check (idempotency_key ~ '^[a-z0-9][a-z0-9:._/-]{7,159}$'),
  source text not null check (source in ('minecraft','copperline','discord','dashboard','media','npc')),
  actor_id text check (actor_id is null or actor_id ~ '^[A-Za-z0-9][A-Za-z0-9:._-]{0,127}$'),
  payload jsonb not null default '{}'::jsonb,
  payload_sha256 text not null check (payload_sha256 ~ '^[0-9a-f]{64}$'),
  occurred_at timestamptz not null default now(),
  check (octet_length(payload::text) <= 8192)
);

create index if not exists idx_arg_events_key_time
  on public.arg_events(event_key, occurred_at desc);

create table if not exists public.arg_event_projections (
  event_id uuid not null references public.arg_events(event_id) on delete cascade,
  surface text not null check (surface in ('minecraft','copperline','discord','dashboard','media','npc')),
  status text not null default 'queued' check (status in ('queued','applied','failed')),
  attempts integer not null default 0 check (attempts >= 0),
  last_error text,
  updated_at timestamptz not null default now(),
  primary key (event_id, surface)
);

create index if not exists idx_arg_event_projections_work
  on public.arg_event_projections(surface, status, updated_at)
  where status in ('queued', 'failed');

alter table public.arg_event_definitions enable row level security;
alter table public.arg_events enable row level security;
alter table public.arg_event_projections enable row level security;

revoke all on public.arg_event_definitions from public, anon, authenticated;
revoke all on public.arg_events from public, anon, authenticated;
revoke all on public.arg_event_projections from public, anon, authenticated;

grant select, insert, update on public.arg_event_definitions to service_role;
grant select, insert on public.arg_events to service_role;
grant select, insert, update on public.arg_event_projections to service_role;

insert into public.arg_event_definitions
  (event_key, phase_key, prerequisite_events, source_surfaces, projection_surfaces)
values
  ('p1.attachment_history_restored','P1','{}','{copperline}','{copperline,discord}'),
  ('p1.mkept_intent_authenticated','P1','{p1.attachment_history_restored}','{copperline,discord}','{copperline,discord}'),
  ('p2.artifact_authenticated','P2','{p1.mkept_intent_authenticated}','{copperline,discord}','{copperline,discord,dashboard}'),
  ('p2.live_runtime_handoff','P2','{p2.artifact_authenticated}','{minecraft,copperline}','{minecraft,copperline,discord}'),
  ('p3.resident_accounts_opened','P3','{p2.live_runtime_handoff}','{minecraft,npc}','{minecraft,npc,discord}'),
  ('p3.dispatch_authorized','P3','{p3.resident_accounts_opened}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p4.mouth_revision_restored','P4','{p3.dispatch_authorized}','{copperline}','{copperline,minecraft,discord}'),
  ('p4.copy_hypothesis_tested','P4','{p4.mouth_revision_restored}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p4.control_reversal_earned','P4','{p4.copy_hypothesis_tested}','{minecraft}','{minecraft,copperline,discord,npc}'),
  ('p5.service_chronology_shared','P5','{p4.control_reversal_earned}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p5.civic_gallery_recurated','P5','{p5.service_chronology_shared}','{minecraft}','{minecraft,copperline,discord,npc}'),
  ('p6.professional_models_recovered','P6','{p5.civic_gallery_recurated}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p6.six_responsibilities_acknowledged','P6','{p6.professional_models_recovered}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p7.counterfeit_material_proven','P7','{p6.six_responsibilities_acknowledged}','{minecraft}','{minecraft,copperline,discord,npc}'),
  ('p7.supplier_history_restored','P7','{p7.counterfeit_material_proven}','{copperline}','{copperline,minecraft,discord}'),
  ('p7.nessa_publicly_cleared','P7','{p7.supplier_history_restored}','{discord,npc}','{minecraft,copperline,discord,npc}'),
  ('p8.intervention_plan_accepted','P8','{p7.nessa_publicly_cleared}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p8.hold_systems_repaired','P8','{p8.intervention_plan_accepted}','{minecraft}','{minecraft,copperline,discord,npc}'),
  ('p9.company_biographies_restored','P9','{p8.hold_systems_repaired}','{minecraft,copperline,discord}','{minecraft,copperline,discord,media}'),
  ('p9.leak_window_proven','P9','{p9.company_biographies_restored}','{minecraft,discord,media}','{minecraft,copperline,discord,media,npc}'),
  ('p10.wren_confronted','P10','{p9.leak_window_proven}','{minecraft,discord,npc}','{minecraft,copperline,discord,npc}'),
  ('p10.wren_remembrance_committed','P10','{p10.wren_confronted}','{minecraft,discord}','{minecraft,copperline,discord,npc}'),
  ('p11.averyn_identified','P11','{p10.wren_remembrance_committed}','{minecraft,discord,media}','{minecraft,copperline,discord,npc}'),
  ('p11.averyn_restored_unbound','P11','{p11.averyn_identified}','{minecraft}','{minecraft,copperline,discord,npc}'),
  ('p12.release_configuration_ready','P12','{p11.averyn_restored_unbound}','{minecraft}','{minecraft,discord,dashboard}'),
  ('p12.name_treatment_committed','P12','{p12.release_configuration_ready}','{minecraft}','{minecraft,copperline,discord,dashboard,npc}'),
  ('p12.record_closed_averyn_released','P12','{p12.name_treatment_committed}','{minecraft}','{minecraft,copperline,discord,dashboard,npc}')
on conflict (event_key) do update set
  phase_key = excluded.phase_key,
  prerequisite_events = excluded.prerequisite_events,
  source_surfaces = excluded.source_surfaces,
  projection_surfaces = excluded.projection_surfaces,
  automation = excluded.automation,
  active = true,
  updated_at = now();

create or replace function public.observance_record_arg_event(
  p_event_key text,
  p_idempotency_key text,
  p_source text,
  p_actor_id text default null,
  p_payload jsonb default '{}'::jsonb
)
returns table (
  status text,
  created boolean,
  event_id uuid,
  missing_prerequisites text[]
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  definition public.arg_event_definitions%rowtype;
  prior public.arg_events%rowtype;
  missing text[];
  canonical_hash text;
  inserted_id uuid;
  projection text;
begin
  if p_idempotency_key !~ '^[a-z0-9][a-z0-9:._/-]{7,159}$'
     or p_actor_id is not null and p_actor_id !~ '^[A-Za-z0-9][A-Za-z0-9:._-]{0,127}$'
     or octet_length(coalesce(p_payload, '{}'::jsonb)::text) > 8192 then
    raise exception 'invalid ARG event input';
  end if;

  select * into definition
    from public.arg_event_definitions
    where event_key = p_event_key and active = true
    for share;
  if not found then raise exception 'unknown or inactive ARG event'; end if;
  if not p_source = any(definition.source_surfaces) then
    raise exception 'source surface cannot record this ARG event';
  end if;

  canonical_hash := encode(extensions.digest(convert_to(coalesce(p_payload, '{}'::jsonb)::text, 'UTF8'), 'sha256'), 'hex');
  select * into prior from public.arg_events where idempotency_key = p_idempotency_key;
  if found then
    if prior.event_key = p_event_key and prior.source = p_source
       and prior.actor_id is not distinct from p_actor_id and prior.payload_sha256 = canonical_hash then
      return query select 'committed'::text, false, prior.event_id, '{}'::text[];
    else
      return query select 'collision'::text, false, prior.event_id, '{}'::text[];
    end if;
    return;
  end if;

  select coalesce(array_agg(required order by required), '{}'::text[]) into missing
  from unnest(definition.prerequisite_events) as required
  where not exists (select 1 from public.arg_events e where e.event_key = required);
  if cardinality(missing) > 0 then
    return query select 'blocked'::text, false, null::uuid, missing;
    return;
  end if;

  insert into public.arg_events(event_key, idempotency_key, source, actor_id, payload, payload_sha256)
  values (p_event_key, p_idempotency_key, p_source, p_actor_id, coalesce(p_payload, '{}'::jsonb), canonical_hash)
  returning arg_events.event_id into inserted_id;

  foreach projection in array definition.projection_surfaces loop
    insert into public.arg_event_projections(event_id, surface, status, attempts)
    values (inserted_id, projection, case when projection = p_source then 'applied' else 'queued' end,
      case when projection = p_source then 1 else 0 end);
  end loop;

  update public.arc_state
    set flags = coalesce(flags, '{}'::jsonb) || jsonb_build_object(p_event_key, true),
        updated_at = now()
    where id = 1;

  return query select 'committed'::text, true, inserted_id, '{}'::text[];
end;
$$;

revoke all on function public.observance_record_arg_event(text,text,text,text,jsonb)
  from public, anon, authenticated;
grant execute on function public.observance_record_arg_event(text,text,text,text,jsonb)
  to service_role;

commit;
