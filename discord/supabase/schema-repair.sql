-- THE OBSERVANCE - schema-repair.sql
-- ---------------------------------------------------------------------------
-- Fixes plugin<->DB schema drift. The Java plugin writes flat, mc_uuid-keyed
-- rows for tracker and world systems, while the dashboard base schema began with
-- player_id-keyed shapes. This repair is additive and idempotent: it adds the
-- columns, indexes, and support tables the plugin speaks without removing older
-- dashboard-facing columns.
-- ---------------------------------------------------------------------------

begin;

-- ===== dossiers - plugin upserts on mc_uuid with flat signal columns =====
alter table public.dossiers
  add column if not exists mc_uuid              text,
  add column if not exists name                 text,
  add column if not exists solo_mining_seconds  bigint,
  add column if not exists hoarded_score        double precision,
  add column if not exists distance_from_group  double precision,
  add column if not exists extra                text;

alter table public.dossiers drop constraint if exists dossiers_pkey;
alter table public.dossiers alter column player_id drop not null;
create unique index if not exists dossiers_mc_uuid_uidx on public.dossiers (mc_uuid);

-- ===== custom_compliance - plugin upserts on (mc_uuid, custom_key) =====
alter table public.custom_compliance
  add column if not exists mc_uuid        text,
  add column if not exists name           text,
  add column if not exists honored_count  int default 0,
  add column if not exists violated_count int default 0,
  add column if not exists last_event_at  timestamptz,
  add column if not exists updated_at     timestamptz default now();

create unique index if not exists custom_compliance_mcuuid_key_uidx
  on public.custom_compliance (mc_uuid, custom_key);

-- ===== bases - plugin upserts on owner_uuid with center/radius fields =====
alter table public.bases
  add column if not exists owner_uuid text,
  add column if not exists label      text,
  add column if not exists center_x   int,
  add column if not exists center_y   int,
  add column if not exists center_z   int,
  add column if not exists radius     numeric;

create unique index if not exists bases_owner_uuid_key on public.bases (owner_uuid);

-- ===== event_log - plugin writes type/context/mc_uuid/detail =====
alter table public.event_log
  add column if not exists type    text,
  add column if not exists context text,
  add column if not exists mc_uuid text,
  add column if not exists detail  text;

-- ===== world_paste_ledger - durable FAWE single-paste idempotency =====
create table if not exists public.world_paste_ledger (
  id         bigserial primary key,
  world      text not null,
  site_id    text not null default '',
  schematic  text not null,
  base_x     int not null,
  base_y     int not null,
  base_z     int not null,
  pasted_at  timestamptz not null default now(),
  unique (world, site_id, schematic, base_x, base_y, base_z)
);

alter table public.world_paste_ledger enable row level security;

create index if not exists idx_world_paste_ledger_site
  on public.world_paste_ledger (site_id, schematic);

commit;
