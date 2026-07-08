-- The Observance — 0006_v_record_theories.sql
-- Reshape S-D dovetail: the Record RECEIVES a keeper's fate by THEORY, not by lookup.
--
-- WHY. Wave S-D added the "build a theory" mechanic: the showrunner sets a derived `<keeper>_theory`
-- flag in arc_state.flags when a coherent CLUSTER of that keeper's evidence is solved (theory.ts /
-- theory.run.ts). The public Record should reflect ASSEMBLED THEORIES — a keeper's fate un-redacts when
-- the group has put the keeper together, not when they've cracked one cipher (reward the theory, not the
-- lookup). This migration exposes those six coarse booleans to the Record's one anon read surface.
--
-- SECURITY MODEL — unchanged from 0004_v_record.sql. Still a SECURITY DEFINER view (security_invoker =
-- false); anon reads only the neutral projection, never a base row. The new column is the SAME coarse
-- granularity as stones_read: it says only WHICH of the six named keeper-theories are coherent — a group
-- progress signal. It exposes ZERO answers, ZERO puzzle_keys, ZERO player names, ZERO other flag
-- structure. The keeper ids (vaun/mara/…) are the DEAD keepers' canonical names, already public in the
-- in-world record; never a living player. Un-redaction stays in lockstep with real progress (a theory
-- appears only once its cluster is actually solved).
--
-- Additive + idempotent (create or replace, adding one trailing column — Postgres-legal). Re-grants anon
-- SELECT (preserved across replace, re-granted defensively). Apply AFTER 0004_v_record.sql. Depends only
-- on arc_state (jsonb flags) + solves, which exist by then.

begin;

create or replace view public.v_record
with (security_invoker = false) as
select
  -- coarse season (1..5): the author-advanced act pointer (unchanged from 0004).
  least(greatest(coalesce((select current_act from public.arc_state where id = 1), 1), 1), 5)::int
    as movement,

  -- how many of the six keeper evidence clusters have their first keeper read solved — the bare count
  -- (unchanged from 0004 except Sella's retired Atbash row is not counted). Retained as the progressive
  -- fallback the projection uses before any theory has locked.
  (select count(distinct s.puzzle_key)
     from public.solves s
    where s.puzzle_key in (
      'stone-vaun', 'stone-mara', 'sella-reflection-bearing',
      'stone-orin', 'stone-brann', 'stone-iss-wall'
    ))::int
    as stones_read,

  -- the keeping has closed: the Accepting resolved (unchanged from 0004).
  coalesce(
    (select (flags ? 'record_received') or (flags ? 'bowed_as_one')
       from public.arc_state where id = 1),
    false)
    as accepted,

  -- NEW (S-D): which of the six keeper theories are coherent, in fall-order. A text[] of keeper ids
  -- whose `<keeper>_theory` flag is truthy in arc_state.flags. Same coarse granularity as stones_read;
  -- the projection maps each to its fixed archive entry so the fate un-redacts on the assembled theory.
  coalesce(
    (select array_agg(t.k order by t.ord)
       from (values ('vaun', 1), ('mara', 2), ('sella', 3),
                    ('orin', 4), ('brann', 5), ('iss', 6)) as t(k, ord)
       cross join public.arc_state a
      where a.id = 1
        and coalesce((a.flags ->> (t.k || '_theory'))::boolean, false)),
    array[]::text[])
    as theories;

comment on view public.v_record is
  'Public, anon-readable coarse Record signal (P1-W1 + S-D). SECURITY DEFINER; exposes ONLY '
  '{movement, stones_read, accepted, theories} — a season pointer, a keeper-stone COUNT, a closed '
  'boolean, and the set of coherent keeper-theory ids (dead-keeper names, group progress). No answers, '
  'labels, player names, or wider flag structure. Consumed by record/[slug]/page.tsx via '
  'record-projection.ts, which clamps every field. Un-redacts in lockstep with real solves/theories.';

grant select on public.v_record to anon, authenticated;

commit;
