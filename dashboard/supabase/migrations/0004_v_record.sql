-- The Observance — 0004_v_record.sql
-- The public Record's ONE read surface (P1-W1 / BUILD-MANIFEST §7 / INTEGRATION-V2 §A13).
--
-- WHY THIS FILE. `dashboard/src/app/record/[slug]/page.tsx` reads a view named `v_record`
-- (page.tsx:95, select "movement, stones_read, accepted"), but no migration ever created it.
-- Every read hit the page's catch and the archive degraded to the sealed baseline — permanently
-- frozen, never un-redacting with progress. This migration authors that view.
--
-- SECURITY MODEL — IDENTICAL to the three views in 0001_init.sql (v_health / v_heatmap /
-- v_compliance_counts). It is a SECURITY DEFINER view (`security_invoker = false`, the Postgres
-- default made explicit): it runs with the view OWNER's privileges (the migration role, which owns
-- the base tables), so anon can read the neutral projection WITHOUT any table grant and WITHOUT any
-- anon RLS policy on the spoiler tables. anon never touches raw rows — only the three COARSE columns
-- below. Deny-by-default holds: the base tables (arc_state, solves, puzzles) have RLS enabled, no
-- anon policy, and revoked anon grants (0003_lockdown / 0004_oracle); this view is the ONLY anon path.
--
-- SPOILER-SAFETY (the contract record-projection.ts already clamps to). The view exposes EXACTLY:
--   * movement    int  — the coarse season (1..5), from arc_state.current_act. A season pointer, not
--                        a puzzle answer. Clamped to the SEASONS band the projection knows.
--   * stones_read int  — how many of the SIX keeper-stones have been solved (0..6), a bare COUNT over
--                        solves. No puzzle_key, no answer, no label, no player — just the tally, the
--                        same shape v_compliance_counts uses to expose counts over a spoiler table.
--   * accepted   bool  — whether the Accepting has resolved (arc_state.flags carries record_received
--                        or bowed_as_one). The single arc-end signal the Record may know. A boolean.
-- It exposes ZERO unsolved answers, ZERO story/custom labels, ZERO player names, and no per-flag
-- structure (only the one closed-flag boolean). Un-redaction is progressive: stones_read rises only
-- as stones are actually solved, so entries go legible in lockstep with real progress, never ahead.
--
-- Single row (arc_state is a single row, id=1). Additive + idempotent (create or replace) — safe to
-- re-run. Apply as the migration/service role AFTER the discord lineage's 0004_oracle.sql (which
-- creates public.solves) and 0006_requires_flags.sql have landed on the live project. Depends only
-- on tables that exist by then; touches nothing else.

begin;

-- v_record: the coarse, spoiler-free public archive signal. Reads the spoiler tables as the view
-- owner and projects only the three blunt public facts the sealed Record is allowed to show.
create or replace view public.v_record
with (security_invoker = false) as
select
  -- coarse season (1..5): the author-advanced act pointer. Clamped to the projection's band so a
  -- stray value can never over-reveal (record-projection.ts re-clamps 0..5 defensively as well).
  least(greatest(coalesce((select current_act from public.arc_state where id = 1), 1), 1), 5)::int
    as movement,

  -- how many of the six keeper evidence clusters have their first keeper read solved. A bare count
  -- over solves, filtered to the current six canonical keeper keys — no key, answer, label, or
  -- player leaves the view. Sella's retired Atbash row is intentionally not counted; her live read is
  -- the water/reflection bearing.
  (select count(distinct s.puzzle_key)
     from public.solves s
    where s.puzzle_key in (
      'stone-vaun', 'stone-mara', 'sella-reflection-bearing',
      'stone-orin', 'stone-brann', 'stone-iss-wall'
    ))::int
    as stones_read,

  -- the keeping has closed: the Accepting resolved. The only arc-end signal the Record may surface,
  -- reduced to a single boolean (never the flags blob itself).
  coalesce(
    (select (flags ? 'record_received') or (flags ? 'bowed_as_one')
       from public.arc_state where id = 1),
    false)
    as accepted;

comment on view public.v_record is
  'Public, anon-readable coarse Record signal (P1-W1). SECURITY DEFINER; exposes ONLY '
  '{movement, stones_read, accepted} — a season pointer, a keeper-stone COUNT, and a closed '
  'boolean. No answers, labels, player names, or flag structure. Consumed by record/[slug]/page.tsx '
  'via record-projection.ts, which clamps every field. Un-redacts in lockstep with real solves.';

-- The ONLY grant: anon (and authenticated) may SELECT the neutral view. Mirrors 0001_init.sql's
-- grants for v_health / v_heatmap / v_compliance_counts. anon still has no base-table grant/policy.
grant select on public.v_record to anon, authenticated;

commit;
