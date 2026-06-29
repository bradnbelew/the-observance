-- The Observance — 0006_requires_flags.sql
-- THE KEYSTONE. Adds the one column the entire non-linear progression depends on and
-- that no prior migration ever created: `puzzles.requires_flags`.
--
-- Why this file is the single most important fix in the overhaul (see design/OVERHAUL.md §2):
-- the seeds already author a complete, internally-consistent flag-gate — metapuzzle_seed.sql
-- §2 sets requires_flags on every back-half row and flips them active, and progression_seed.sql
-- gates the Nether/End lanes the same way. BUT every one of those UPDATEs sits inside a
--   `do $$ begin if exists (… column_name = 'requires_flags') then … else raise notice … end $$;`
-- guard, because the column didn't exist. So they all silently no-op: nine M4/Seventh rows that
-- ship active=false stay dark forever, and base-docket-reread-auto ships active=true meant to be
-- HELD CLOSED by a gate that isn't there, so it leaks its M4 answers from minute one.
--
-- This migration creates the column. After it lands, RE-RUN the seeds (puzzles_seed.sql,
-- metapuzzle_seed.sql, progression_seed.sql) so the guarded activation lane takes effect — the
-- guards now see the column and apply instead of `raise notice`. The whole back half lights up.
--
-- THE GATE CONTRACT (the storylet precondition system; OVERHAUL.md §3):
--   A row is OPEN  ⟺  active = true  AND  every key in requires_flags is truthy in arc_state.flags.
--   `getOpenPuzzles` (discord/src/db/repo.ts) and the Java OracleResolver.firstMatch BOTH AND-test
--   it. An empty `{}` (the default) means "no gate" — so every existing ungated row is unchanged.
--
-- Security model (identical to 0004/0005): RLS already enabled on puzzles; service_role bypasses;
-- no anon/authenticated policies. Additive + idempotent (add column if not exists) — safe to re-run.

begin;

-- ===========================================================================
-- 1. puzzles.requires_flags — the storylet precondition. A flat jsonb object of
--    { flag_key: true } that must ALL be truthy in arc_state.flags for the row to open.
--    Default '{}' = ungated (the open condition collapses to just active=true), so adding
--    the column changes the behavior of NO existing row until a seed sets a non-empty value.
-- ===========================================================================

alter table public.puzzles
  add column if not exists requires_flags jsonb not null default '{}'::jsonb;

comment on column public.puzzles.requires_flags is
  'Storylet gate (OVERHAUL.md §3): a flat {flag:true} object; the row is open iff active=true '
  'AND every key here is truthy in arc_state.flags. Default {} = ungated. AND-tested by '
  'getOpenPuzzles (TS) and OracleResolver.firstMatch (Java). Keep FLAT — the merge is shallow.';

-- A partial expression index is unnecessary: `puzzles` is a tiny table read in full each resolve
-- (getOpenPuzzles selects all active rows, then the app AND-tests requires_flags against the single
-- arc_state row's flags). No query filters on requires_flags in SQL, so no index is added here.

-- ===========================================================================
-- 2. observance_merge_arc_flags(p_flags) — ATOMIC shallow merge of the single
--    arc_state.flags blob. Kills the read-modify-write clobber in repo.setArcFlags:
--    a concurrent Discord solve + in-world solve (or two Discord solves) each doing
--    SELECT-then-UPDATE could drop one set's keys; the jsonb `||` union performed
--    server-side in ONE statement cannot. Right operand wins on key conflict — exactly
--    right for our flat {flag:true} shape (DOSSIER #A2). Flags MUST stay flat (the merge
--    is shallow; a nested object would be wholesale-overwritten, not deep-merged).
-- ===========================================================================

create or replace function public.observance_merge_arc_flags(p_flags jsonb)
returns jsonb
language sql
security definer
set search_path = public
as $$
  update public.arc_state
     set flags = flags || coalesce(p_flags, '{}'::jsonb),
         updated_at = now()
   where id = 1
  returning flags;
$$;

comment on function public.observance_merge_arc_flags(jsonb) is
  'Atomic shallow merge into arc_state.flags (id=1): flags = flags || p_flags in one '
  'statement (no read-modify-write clobber). Keep the flags object FLAT. Called by '
  'repo.setArcFlags via .rpc().';

-- Lock the function down to service_role only (the bot/showrunner run as service_role;
-- there is no anon/authenticated path that should mutate the arc). REVOKE the default
-- PUBLIC EXECUTE that `create function` grants, then GRANT to service_role explicitly.
revoke all on function public.observance_merge_arc_flags(jsonb) from public;
grant execute on function public.observance_merge_arc_flags(jsonb) to service_role;

commit;
