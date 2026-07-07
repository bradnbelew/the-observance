-- The Observance - durable world paste ledger.
-- 0012_world_paste_ledger.sql
--
-- The plugin's optional FAWE schematic path calls SupabaseClient.claimPasteLedger()
-- before pasting a large single-use set-piece. Without this table, that path compiles
-- but fails its durable cross-restart idempotency guard at runtime.

begin;

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
