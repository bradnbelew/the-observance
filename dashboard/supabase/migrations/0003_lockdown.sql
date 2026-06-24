-- The Observance (dashboard) — 0003_lockdown.sql
-- SECURITY corrective (audit, CRITICAL). 0001_init.sql created an `authenticated_all` policy
-- (`for all to authenticated using(true)`) + blanket table/sequence grants over all 12 spoiler
-- tables. Sign-in is an OPEN magic-link flow, so ANY signed-in user could read/write those tables
-- directly via PostgREST (anon key + their JWT), bypassing the ADMIN_EMAILS allowlist — which only
-- gates the dashboard's OWN server actions, not direct REST calls.
--
-- The dashboard reads/writes these tables EXCLUSIVELY through the server-side service-role client
-- behind isAdmin() (author/actions.ts + author/page.tsx, re-gated in the same audit pass). So the
-- `authenticated` role needs NO access to them. This drops the blanket policy + grants; RLS stays
-- enabled (from 0001), so with no authenticated policy and no grants, only service_role (which
-- bypasses RLS) can reach them. The spoiler-FREE public VIEWS (anon) keep their own grants — the
-- public /status page is unaffected.
--
-- Idempotent. Apply AFTER 0001_init.sql + 0002_seed.sql. Run as service_role / owner.

begin;

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
    execute format('revoke all on public.%I from authenticated;', t);
    execute format('revoke all on public.%I from anon;', t);
  end loop;
end;
$$;

-- Sequences too (insert paths used them); service_role bypasses, anon views don't need them.
revoke all on all sequences in schema public from authenticated;
revoke all on all sequences in schema public from anon;

commit;
