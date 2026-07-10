-- The Observance (dashboard) — 0005_reconcile_tracker_views.sql
-- ---------------------------------------------------------------------------
-- Fixes the LAST leg of the plugin<->dashboard drift for dossiers + customs so
-- the Author page stops rendering blank rows.
--
-- BACKGROUND. discord/supabase/schema-repair.sql made the two spoiler tables
-- ADDITIVE-compatible with the Java plugin: the plugin now upserts FLAT,
-- mc_uuid-keyed rows with RENAMED/RETYPED columns and a NULL player_id:
--   dossiers          : solo_mining_seconds (bigint), hoarded_score (double),
--                       distance_from_group (double), extra (text), name, mc_uuid
--                       — but NOT solo_ratio / group_distance / hoard_summary,
--                         and player_id is NULL for every plugin-written row.
--   custom_compliance : honored_count, violated_count, last_event_at, name,
--                       mc_uuid — but NOT status / violation_count / last_observed,
--                         and player_id is NULL for every plugin-written row.
--
-- The dashboard (author/page.tsx + Dossiers.tsx) still reads the ORIGINAL
-- 0001_init shape declared in database.types.ts — solo_ratio / group_distance /
-- hoard_summary / chat_sentiment / updated_at, and status / violation_count /
-- last_observed — AND joins both tables to players BY player_id. Two things
-- drifted at once: (1) column names+types, and (2) the join KEY (mc_uuid vs
-- player_id). A pure read-column rename cannot fix (2): even with correct names
-- the player_id join would still miss NULL-player_id plugin rows and stay blank.
--
-- FIX (smallest-safe, view-based). Add two reconciling SECURITY DEFINER views
-- that JOIN plugin rows back to players via players.mc_uuid to SYNTHESIZE
-- player_id, and alias/coerce the flat columns into EXACTLY the shape
-- database.types.ts already declares. Only two dashboard `from()` table names
-- change (dossiers -> v_dossiers, custom_compliance -> v_custom_compliance);
-- the TS types, the player_id joins, and Dossiers.tsx stay untouched. Old
-- player_id-keyed rows (if any) still resolve — the join is on mc_uuid, which
-- both old and new rows carry.
--
-- SECURITY. These views are SPOILER-RICH (player names, custom_key labels,
-- per-player signals) — they are NOT for anon. Mirroring the deny-by-default
-- posture 0003_lockdown established: the base tables have RLS on, no anon/
-- authenticated policy, and revoked grants; the dashboard reads them ONLY via
-- the server-side service_role client behind isAdmin() (service_role bypasses
-- RLS and grants). So these views get NO grant to anon or authenticated — we
-- REVOKE from both explicitly. Only service_role (the dashboard's admin client)
-- reads them. This does NOT weaken RLS and never exposes the service key.
--
-- Additive + idempotent (create or replace view). ORDER-INDEPENDENT: the shim
-- below adds the flat plugin columns these views alias, so this migration may be
-- applied before OR after discord/supabase/schema-repair.sql (both idempotent).
-- ---------------------------------------------------------------------------

begin;

-- ---------------------------------------------------------------------------
-- COMPATIBILITY SHIM (order-independence). These views alias the flat plugin
-- columns that discord/supabase/schema-repair.sql adds to dossiers /
-- custom_compliance. Guarantee those columns exist FIRST so this migration can be
-- hand-applied in ANY order relative to schema-repair (a stray order previously
-- errored: "column d.mc_uuid does not exist"). Idempotent + type-matched to
-- schema-repair, so whichever lands first wins and the other no-ops. Column DATA
-- is still populated only by the plugin's upserts; this just ensures the view can
-- be CREATED regardless of apply order.
alter table public.dossiers
  add column if not exists mc_uuid             text,
  add column if not exists solo_mining_seconds bigint,
  add column if not exists hoarded_score       double precision,
  add column if not exists distance_from_group double precision,
  add column if not exists extra               text;

alter table public.custom_compliance
  add column if not exists mc_uuid        text,
  add column if not exists violated_count int default 0,
  add column if not exists last_event_at  timestamptz;
-- ---------------------------------------------------------------------------

-- v_dossiers — plugin FLAT dossier rows reshaped into the dashboard's declared
-- Tables<'dossiers'> Row. Joins to players by mc_uuid to synthesize player_id.
--   * solo_ratio     <- solo_mining_seconds (bigint -> numeric via ::numeric)
--   * group_distance <- distance_from_group (double)
--   * hoard_summary  <- extra (the plugin's free-form JSON string) when present,
--                       else the numeric hoarded_score rendered as text.
--   * deaths / blocks_mined / chat_sentiment / updated_at pass through the base
--     columns (chat_sentiment has no plugin source yet -> null, which the type
--     already allows).
drop view if exists public.v_dossiers;

create or replace view public.v_dossiers
with (security_invoker = false) as
select
  p.id                                        as player_id,
  coalesce(d.solo_mining_seconds::numeric, d.solo_ratio, 0) as solo_ratio,
  coalesce(d.deaths, 0)                        as deaths,
  coalesce(
    nullif(d.extra, ''),
    case when d.hoarded_score is not null then d.hoarded_score::text else null end,
    d.hoard_summary
  )                                            as hoard_summary,
  coalesce(d.distance_from_group, d.group_distance) as group_distance,
  d.chat_sentiment                             as chat_sentiment,
  coalesce(d.blocks_mined, 0)                  as blocks_mined,
  d.updated_at                                 as updated_at
from public.dossiers d
join public.players p on p.mc_uuid = d.mc_uuid;

comment on view public.v_dossiers is
  'Reconciling view (0005). Reshapes the plugin''s flat mc_uuid-keyed dossiers into the '
  'dashboard''s declared Tables<dossiers> Row (player_id synthesized via players.mc_uuid). '
  'SPOILER-RICH (names, per-player signals): SECURITY DEFINER, read ONLY by the service_role '
  'admin client — NO anon/authenticated grant.';

-- v_custom_compliance — plugin FLAT custom rows reshaped into the dashboard's
-- declared Tables<'custom_compliance'> Row. Joins to players by mc_uuid.
--   * violation_count <- violated_count
--   * last_observed   <- last_event_at
--   * status          <- DERIVED (no plugin source): the same rung semantics the
--                        author page/customs.ts use — LEFT_AT (5) standing
--                        violations = 'violating', any standing violation =
--                        'warned', else 'keeping'. Matches Dossiers.tsx's
--                        COMPLIANCE_STYLES keys (keeping/warned/violating).
--   * id / custom_key pass through the base columns.
drop view if exists public.v_custom_compliance;

create or replace view public.v_custom_compliance
with (security_invoker = false) as
select
  c.id                                         as id,
  p.id                                         as player_id,
  c.custom_key                                 as custom_key,
  c.last_event_at                              as last_observed,
  coalesce(c.violated_count, c.violation_count, 0) as violation_count,
  case
    when coalesce(c.violated_count, c.violation_count, 0) >= 5 then 'violating'
    when coalesce(c.violated_count, c.violation_count, 0) > 0  then 'warned'
    else 'keeping'
  end                                          as status
from public.custom_compliance c
join public.players p on p.mc_uuid = c.mc_uuid;

comment on view public.v_custom_compliance is
  'Reconciling view (0005). Reshapes the plugin''s flat mc_uuid-keyed custom_compliance into '
  'the dashboard''s declared Tables<custom_compliance> Row (player_id synthesized via '
  'players.mc_uuid; status derived from violated_count using the LEFT_AT=5 rung). SPOILER-RICH '
  '(names, custom_key labels): SECURITY DEFINER, read ONLY by the service_role admin client — '
  'NO anon/authenticated grant.';

-- Deny-by-default (0003_lockdown posture): these spoiler-rich views are read
-- ONLY via the service_role client. Explicitly revoke from anon + authenticated
-- so a stray default grant can never expose player names / custom labels.
revoke all on public.v_dossiers          from anon, authenticated;
revoke all on public.v_custom_compliance from anon, authenticated;

commit;
