-- THE OBSERVANCE — schema-repair.sql  (2026-07-01)
-- ---------------------------------------------------------------------------
-- Fixes plugin<->DB schema drift. The Java plugin writes flat, mc_uuid-keyed
-- rows (DossierRow / CustomComplianceRow / BaseRow), but dashboard 0001_init
-- created dossiers / custom_compliance / bases with a player_id-keyed shape and
-- different column names. Result: every background-tracker flush returned HTTP 400.
--
-- This is ADDITIVE + IDEMPOTENT: it ADDS the columns + upsert conflict keys the
-- plugin needs, and KEEPS the old columns so the dashboard's existing reads still
-- resolve (they'll be null for plugin-written rows until a proper reconciliation).
-- The puzzle loop (players / solves / heatmap_cells) already matched and is untouched.
--
-- Apply in the Supabase SQL Editor for the Observance project (fdnmhbpxnodrnbrzrlqq),
-- as service_role. Safe to re-run. After applying, the `tracker.flush.dossier`
-- 400 spam stops and behavior tracking starts saving.
-- ---------------------------------------------------------------------------

begin;

-- ===== dossiers — plugin upserts on mc_uuid with flat signal columns =====
alter table public.dossiers
  add column if not exists mc_uuid              text,
  add column if not exists name                 text,
  add column if not exists solo_mining_seconds  bigint,
  add column if not exists hoarded_score        double precision,
  add column if not exists distance_from_group  double precision,
  add column if not exists extra                text;

-- The plugin inserts without player_id, so player_id can no longer be a NOT NULL
-- primary key. Drop the PK, make it nullable, and key upserts off mc_uuid instead.
-- (Old rows keep their player_id; new plugin rows carry mc_uuid. Dashboard reads by
-- player_id still work for old rows; join via players.mc_uuid for new ones.)
alter table public.dossiers drop constraint if exists dossiers_pkey;
alter table public.dossiers alter column player_id drop not null;
create unique index if not exists dossiers_mc_uuid_uidx on public.dossiers (mc_uuid);

-- ===== custom_compliance — plugin upserts on (mc_uuid, custom_key) =====
alter table public.custom_compliance
  add column if not exists mc_uuid        text,
  add column if not exists name           text,
  add column if not exists honored_count  int default 0,
  add column if not exists violated_count int default 0,
  add column if not exists last_event_at  timestamptz,
  add column if not exists updated_at     timestamptz default now();
-- player_id here is already nullable (the PK is the bigserial `id`), so the plugin's
-- null-player_id inserts are fine; it just needs a unique target for (mc_uuid, custom_key).
create unique index if not exists custom_compliance_mcuuid_key_uidx
  on public.custom_compliance (mc_uuid, custom_key);

-- ===== bases — plugin upserts on owner_uuid (TEXT) with center_x/y/z + label + radius =====
-- CONTRACT (P0-D2): the plugin's BaseDetector upsert is re-keyed to conflict on the TEXT
-- column `owner_uuid` (one base per keeper), NOT on the bigint `id`. So this repair guarantees
-- the column exists AND a UNIQUE index it can name as the ON CONFLICT target. bases.id stays the
-- bigint PK (untouched) — old dashboard reads by id/owner_player_id keep resolving; new plugin
-- rows carry owner_uuid. Idempotent: add column / create index if not exists.
alter table public.bases
  add column if not exists owner_uuid text,
  add column if not exists label      text,
  add column if not exists center_x   int,
  add column if not exists center_y   int,
  add column if not exists center_z   int,
  add column if not exists radius     numeric;

-- The upsert conflict target: one base row per owner_uuid. Named exactly `bases_owner_uuid_key`
-- so the plugin's `on_conflict=owner_uuid` upsert resolves against it.
create unique index if not exists bases_owner_uuid_key on public.bases (owner_uuid);

-- ===== event_log — plugin writes type/context/mc_uuid/detail; the table has only level/source =====
-- Without these columns every plugin log write 400s (all plugin diagnostics lost). Additive.
alter table public.event_log
  add column if not exists type    text,
  add column if not exists context text,
  add column if not exists mc_uuid text,
  add column if not exists detail  text;

commit;

-- ===== NOTE: world_paste_ledger — the plugin also references this table for the =====
-- optional FAWE schematic-paste path; no migration creates it. It's only touched if
-- FAWE is installed (it isn't tonight), so it's left out of tonight's repair. Create it
-- when the schematic-paste path is enabled.
