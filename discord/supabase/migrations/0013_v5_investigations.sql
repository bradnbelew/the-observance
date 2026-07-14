-- The Observance V5 -- ten mandatory investigations, 82 required nodes, durable receipts,
-- prerequisite-gated media, and a fine-grained phase cursor.
--
-- This migration is deliberately additive. It does not delete player accounts, consent choices,
-- solves, attempts, event logs, or operator history. Legacy authored rows gain an `active` switch so
-- the V5 seed can retire them without destroying audit evidence.

begin;

alter table public.arc_state
  add column if not exists phase_key text not null default 'c01-lost-server';

comment on column public.arc_state.current_act is
  'Coarse compatibility cursor used by the hint economy and old operational reports.';
comment on column public.arc_state.phase_key is
  'Fine-grained V5 progression cursor. Case/node gates use durable flags and receipts; this is a display cursor.';

alter table public.hints add column if not exists active boolean not null default true;
alter table public.thread_cards add column if not exists active boolean not null default true;
alter table public.side_quests add column if not exists active boolean not null default true;

create table if not exists public.investigations (
  case_key          text primary key check (case_key ~ '^C(0[1-9]|10)$'),
  ordinal           int not null unique check (ordinal between 1 and 10),
  title             text not null,
  summary           text not null,
  phase_key         text not null unique,
  unlock_flag       text,
  completion_flag   text not null unique,
  required          boolean not null default true check (required = true),
  active            boolean not null default true,
  expected_nodes    int not null check (expected_nodes > 0),
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);

create table if not exists public.investigation_nodes (
  node_key          text primary key check (node_key ~ '^[A-Z]{1,2}[0-9]{2}$'),
  case_key          text not null references public.investigations(case_key) on delete restrict,
  ordinal           int not null check (ordinal > 0),
  title             text not null,
  room_id           text not null,
  modality          text not null,
  input_surface     text not null,
  prerequisite_flags text[] not null default '{}',
  completion_flag   text not null unique,
  reward            text not null,
  recovery          text not null,
  oracle_puzzle_key text references public.puzzles(puzzle_key) on delete set null,
  required          boolean not null default true check (required = true),
  active            boolean not null default true,
  metadata          jsonb not null default '{}'::jsonb,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  unique (case_key, ordinal)
);

create index if not exists idx_investigation_nodes_case
  on public.investigation_nodes(case_key, ordinal) where active = true;
create index if not exists idx_investigation_nodes_oracle
  on public.investigation_nodes(oracle_puzzle_key) where oracle_puzzle_key is not null;

create table if not exists public.evidence_receipts (
  receipt_key       text primary key,
  node_key          text not null references public.investigation_nodes(node_key) on delete restrict,
  player_id         uuid references public.players(id) on delete set null,
  source            text not null check (source in ('world','discord','web','operator','recovery')),
  idempotency_key   text not null unique,
  payload           jsonb not null default '{}'::jsonb,
  received_at       timestamptz not null default now()
);

create index if not exists idx_evidence_receipts_node
  on public.evidence_receipts(node_key, received_at desc);

create table if not exists public.required_media (
  media_key          text primary key,
  case_key           text not null references public.investigations(case_key) on delete restrict,
  node_key           text not null references public.investigation_nodes(node_key) on delete restrict,
  media_kind         text not null check (media_kind in ('video','audio','archive')),
  title              text not null,
  delivery_url       text not null,
  filename           text not null,
  sha1               text not null check (sha1 ~ '^[0-9a-f]{40}$'),
  expected_payload   text not null,
  prerequisite_flags text[] not null default '{}',
  delivery_state     text not null default 'configured'
                     check (delivery_state in ('configured','verified','degraded','missing')),
  last_verified_at   timestamptz,
  active             boolean not null default true,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);

create index if not exists idx_required_media_case
  on public.required_media(case_key, node_key) where active = true;

-- One atomic, idempotent receipt function for Discord, the website, and the Paper plugin. The receipt
-- is inserted once, then its node completion flag is merged without a read/modify/write race.
create or replace function public.observance_record_evidence(
  p_receipt_key text,
  p_node_key text,
  p_source text,
  p_idempotency_key text,
  p_player_id uuid default null,
  p_payload jsonb default '{}'::jsonb
) returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_flag text;
  v_case_key text;
  v_case_flag text;
  v_prerequisites text[];
  v_inserted int;
begin
  select n.completion_flag, n.case_key, i.completion_flag, n.prerequisite_flags
    into v_flag, v_case_key, v_case_flag, v_prerequisites
    from public.investigation_nodes n
    join public.investigations i on i.case_key = n.case_key
   where n.node_key = p_node_key and n.active and n.required and i.active and i.required;
  if v_flag is null then
    raise exception 'unknown or inactive V5 node: %', p_node_key;
  end if;
  if exists (
    select 1 from unnest(v_prerequisites) as required_flag
    cross join public.arc_state a
    where a.id = 1 and not coalesce((a.flags ->> required_flag)::boolean, false)
  ) then
    raise exception 'prerequisites not satisfied for V5 node: %', p_node_key;
  end if;

  insert into public.evidence_receipts
    (receipt_key, node_key, player_id, source, idempotency_key, payload)
  values
    (p_receipt_key, p_node_key, p_player_id, p_source, p_idempotency_key, coalesce(p_payload, '{}'::jsonb))
  on conflict (idempotency_key) do nothing;
  get diagnostics v_inserted = row_count;

  if v_inserted > 0 then
    update public.arc_state
       set flags = coalesce(flags, '{}'::jsonb) || jsonb_build_object(v_flag, true),
           updated_at = now()
     where id = 1;

    -- A case closes only when every one of its mandatory node flags is durable. This is especially
    -- important for C03: its six affidavit lanes finish in parallel, so no arbitrary keeper node is
    -- allowed to masquerade as "the last" one.
    if not exists (
      select 1
        from public.investigation_nodes n
        cross join public.arc_state a
       where n.case_key = v_case_key and n.active and n.required and a.id = 1
         and not coalesce((a.flags ->> n.completion_flag)::boolean, false)
    ) then
      update public.arc_state
         set flags = coalesce(flags, '{}'::jsonb) || jsonb_build_object(v_case_flag, true),
             phase_key = coalesce(
               (select phase_key from public.investigations where ordinal =
                 (select least(10, ordinal + 1) from public.investigations where case_key = v_case_key)),
               phase_key
             ),
             updated_at = now()
       where id = 1;
    end if;
  end if;
  return v_inserted > 0;
end;
$$;

-- Spoiler-bearing base tables remain service-role only. Public projections are defined in the
-- dashboard migration after the V5 schema exists.
alter table public.investigations enable row level security;
alter table public.investigation_nodes enable row level security;
alter table public.evidence_receipts enable row level security;
alter table public.required_media enable row level security;

revoke all on public.investigations from anon, authenticated;
revoke all on public.investigation_nodes from anon, authenticated;
revoke all on public.evidence_receipts from anon, authenticated;
revoke all on public.required_media from anon, authenticated;
revoke all on function public.observance_record_evidence(text,text,text,text,uuid,jsonb)
  from public, anon, authenticated;
grant execute on function public.observance_record_evidence(text,text,text,text,uuid,jsonb) to service_role;

commit;
