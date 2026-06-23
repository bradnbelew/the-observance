-- The Observance — dashboard schema
-- 0001_init.sql
--
-- The Minecraft plugin (service role) WRITES game state into these tables.
-- The dashboard READS that state and WRITES a few control rows
--   (approve/skip beats, edit whisper budgets, toggle watcher-sleep, advance the arc act).
--
-- Security model:
--   * RLS is enabled on EVERY table.
--   * The plugin connects with the service_role key, which BYPASSES RLS — it can do anything.
--   * Dashboard admins connect as `authenticated` users; policies below grant them full read/write.
--     (App-level admin gating is enforced by ADMIN_EMAILS in the Next.js layer.)
--   * `anon` (public / spoiler-free status mode) gets NO table access at all — only the three
--     spoiler-free VIEWS at the bottom of this file are GRANTed to anon.
--
-- Everything runs in the `public` schema.

begin;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists public.players (
  id          uuid primary key default gen_random_uuid(),
  mc_uuid     text unique not null,
  name        text not null,
  first_seen  timestamptz default now(),
  last_seen   timestamptz default now()
);

create table if not exists public.dossiers (
  player_id      uuid primary key references public.players(id) on delete cascade,
  solo_ratio     numeric default 0,
  deaths         int default 0,
  hoard_summary  text,
  group_distance numeric,
  chat_sentiment numeric,
  blocks_mined   int default 0,
  updated_at     timestamptz default now()
);

create table if not exists public.custom_compliance (
  id              bigserial primary key,
  player_id       uuid references public.players(id) on delete cascade,
  custom_key      text not null,
  last_observed   timestamptz,
  violation_count int default 0,
  status          text default 'unknown',
  unique (player_id, custom_key)
);

create table if not exists public.heatmap_cells (
  id         bigserial primary key,
  world      text not null,
  cell_x     int not null,
  cell_z     int not null,
  visits     int default 0,
  updated_at timestamptz default now(),
  unique (world, cell_x, cell_z)
);

create table if not exists public.bases (
  id              bigserial primary key,
  owner_player_id uuid references public.players(id) on delete set null,
  world           text,
  x               int,
  z               int,
  confidence      numeric default 0,
  updated_at      timestamptz default now()
);

create table if not exists public.whisper_budgets (
  id        bigserial primary key,
  player_id uuid references public.players(id) on delete cascade,
  act       int not null,
  budget    int default 3,
  spent     int default 0,
  earned    int default 0,
  unique (player_id, act)
);

create table if not exists public.whisper_events (
  id         bigserial primary key,
  player_id  uuid references public.players(id) on delete cascade,
  puzzle_key text,
  tier       int,
  created_at timestamptz default now()
);

create table if not exists public.bond_ledger (
  player_id   uuid primary key references public.players(id) on delete cascade,
  bond_points int default 0,
  updated_at  timestamptz default now()
);

create table if not exists public.arc_state (
  id          int primary key default 1 check (id = 1),
  current_act int default 1,
  gates       jsonb default '{}'::jsonb,
  flags       jsonb default '{}'::jsonb,
  updated_at  timestamptz default now()
);

create table if not exists public.beat_queue (
  id         bigserial primary key,
  type       text not null,
  target     text,
  payload    jsonb default '{}'::jsonb,
  status     text default 'pending' check (status in ('pending','approved','skipped','fired')),
  created_at timestamptz default now(),
  decided_at timestamptz
);

create table if not exists public.event_log (
  id         bigserial primary key,
  level      text default 'info' check (level in ('info','warn','error')),
  source     text,
  message    text,
  created_at timestamptz default now()
);

create table if not exists public.settings (
  key        text primary key,
  value      jsonb not null,
  updated_at timestamptz default now()
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

-- heatmap lookups + the unique constraint already covers (world,cell_x,cell_z);
-- keep an explicit composite index for clarity / forward-compat.
create index if not exists idx_heatmap_cells_world_xz
  on public.heatmap_cells (world, cell_x, cell_z);

-- beat queue is filtered by status (pending/approved/...) constantly.
create index if not exists idx_beat_queue_status
  on public.beat_queue (status);

-- health view scans recent events by time.
create index if not exists idx_event_log_created_at
  on public.event_log (created_at desc);

-- common foreign-key / time access paths.
create index if not exists idx_custom_compliance_player
  on public.custom_compliance (player_id);
create index if not exists idx_bases_owner_player
  on public.bases (owner_player_id);
create index if not exists idx_whisper_budgets_player_act
  on public.whisper_budgets (player_id, act);
create index if not exists idx_whisper_events_player
  on public.whisper_events (player_id, created_at desc);

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare
  t text;
begin
  foreach t in array array[
    'dossiers', 'heatmap_cells', 'bases', 'bond_ledger', 'arc_state', 'settings'
  ]
  loop
    execute format('drop trigger if exists set_updated_at on public.%I;', t);
    execute format(
      'create trigger set_updated_at before update on public.%I
         for each row execute function public.set_updated_at();', t);
  end loop;
end;
$$;

-- ---------------------------------------------------------------------------
-- Row Level Security — enabled on EVERY table.
-- ---------------------------------------------------------------------------
-- service_role bypasses RLS (the plugin). authenticated (dashboard admins) get
-- full read/write via the policies below. anon gets nothing at the table level.

alter table public.players           enable row level security;
alter table public.dossiers          enable row level security;
alter table public.custom_compliance enable row level security;
alter table public.heatmap_cells     enable row level security;
alter table public.bases             enable row level security;
alter table public.whisper_budgets   enable row level security;
alter table public.whisper_events    enable row level security;
alter table public.bond_ledger       enable row level security;
alter table public.arc_state         enable row level security;
alter table public.beat_queue        enable row level security;
alter table public.event_log         enable row level security;
alter table public.settings          enable row level security;

-- One full-access policy per table for the `authenticated` role.
-- (App-level ADMIN_EMAILS allowlist further restricts who reaches these queries.)
do $$
declare
  t text;
begin
  foreach t in array array[
    'players', 'dossiers', 'custom_compliance', 'heatmap_cells', 'bases',
    'whisper_budgets', 'whisper_events', 'bond_ledger', 'arc_state',
    'beat_queue', 'event_log', 'settings'
  ]
  loop
    execute format('drop policy if exists "authenticated_all" on public.%I;', t);
    execute format(
      'create policy "authenticated_all" on public.%I
         for all to authenticated
         using (true) with check (true);', t);
  end loop;
end;
$$;

-- Lock down default grants: anon and authenticated should not get blanket table
-- privileges from Supabase defaults. authenticated is re-granted explicitly below.
revoke all on all tables in schema public from anon;
revoke all on all tables in schema public from authenticated;

grant select, insert, update, delete on all tables in schema public to authenticated;
grant usage, select on all sequences in schema public to authenticated;

-- ---------------------------------------------------------------------------
-- Spoiler-free VIEWS — the ONLY things anon may read.
-- No story labels, no player identity, no custom_key labels.
-- ---------------------------------------------------------------------------
--
-- These are SECURITY DEFINER views (security_invoker = false, the Postgres
-- default). They run with the privileges of the view OWNER (the migration role,
-- which owns the underlying tables), so anon can read the neutral projection
-- WITHOUT any table-level grant and WITHOUT any anon RLS policy on the base
-- tables. anon never touches the raw rows — only the columns these views expose.

-- v_health: last fired-beat time, 24h info/warn/error counts, and the
-- watcher_sleep + api/whisper status pulled from settings. Single row.
create or replace view public.v_health
with (security_invoker = false) as
select
  (select max(decided_at) from public.beat_queue where status = 'fired')           as last_beat_at,
  (select count(*) from public.event_log
     where level = 'info'  and created_at > now() - interval '24 hours')           as info_24h,
  (select count(*) from public.event_log
     where level = 'warn'  and created_at > now() - interval '24 hours')           as warn_24h,
  (select count(*) from public.event_log
     where level = 'error' and created_at > now() - interval '24 hours')           as error_24h,
  coalesce((select value from public.settings where key = 'watcher_sleep'), 'false'::jsonb)   as watcher_sleep,
  coalesce((select value from public.settings where key = 'api_status'),    '"unknown"'::jsonb) as api_status,
  coalesce((select value from public.settings where key = 'whisper_status'), '"unknown"'::jsonb) as whisper_status;

-- v_heatmap: spatial visit density, no player identity.
create or replace view public.v_heatmap
with (security_invoker = false) as
select
  world,
  cell_x,
  cell_z,
  visits
from public.heatmap_cells;

-- v_compliance_counts: NEUTRAL aggregate only. Total records + total flags
-- across ALL players. No player names, no custom_key labels.
create or replace view public.v_compliance_counts
with (security_invoker = false) as
select
  count(*)::bigint                            as total_records,
  coalesce(sum(violation_count), 0)::bigint   as total_flags
from public.custom_compliance;

-- Grant SELECT on just the three spoiler-free views to anon (and authenticated).
-- Because the views are SECURITY DEFINER, this is the ONLY public read surface:
-- anon has no table grants and no base-table RLS policies, so it physically
-- cannot reach raw dossiers, names, custom_key labels, or story state.
grant select on public.v_health            to anon, authenticated;
grant select on public.v_heatmap           to anon, authenticated;
grant select on public.v_compliance_counts to anon, authenticated;

commit;
