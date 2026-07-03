-- The Observance — 0008_v_archive_flag_gate.sql
-- Generalize the Recovery Archive reveal rule (v_archive, 0007) with a `flag:<name>` gate.
--
-- WHY. 0007 reveals a card by SOLVE (revealed_by_solve), by a re-surface trigger (alt_text_condition
-- 'reopened:<key>'), or by the one companion event ('companion:revealed'). But several authored cards are
-- EVENT-revealed, not solve-revealed: the group gather-event cards (verified when a gather flag flips) and
-- the Nether/End deep-lane cards (revealed when the lane's flag is set on arrival). They carry no puzzle
-- gate, so 0007 withholds them forever. This adds ONE general branch — `alt_text_condition = 'flag:<name>'`
-- reveals the card iff `arc_state.flags ? '<name>'` — so any card can be gated on any set-once arc flag.
-- ('companion:revealed' is kept as its own branch for back-compat with the seed that already uses it; it
-- is exactly the 'flag:companion_revealed' case.)
--
-- SECURITY unchanged: still a SECURITY DEFINER view exposing only revealed rows; anon reads the neutral
-- projection with no base-table grant. Reveal stays gated in SQL over solves + arc_state.flags; an
-- unrevealed body never leaves the view. Additive + idempotent (create or replace). Apply after 0007.

begin;

create or replace view public.v_archive
with (security_invoker = false) as
select
  c.card_key,
  c.thread_key,
  t.label               as thread_label,
  t.color               as thread_color,
  t.sort_order          as thread_sort,
  c.title,
  b.body,
  c.card_kind,
  c.references_card_key,
  c.sort_order          as card_sort
from public.thread_cards c
join public.threads t             on t.thread_key = c.thread_key
join public.thread_card_bodies b  on b.card_key  = c.card_key
where
  -- solve-gated: the card's own puzzle has been read (the common case).
  (c.revealed_by_solve is not null
     and c.revealed_by_solve in (select s.puzzle_key from public.solves s))
  -- re-surface trigger (e.g. the seventh marker reopened by the catch): alt gate names a solve.
  or (c.alt_text_condition like 'reopened:%'
     and substring(c.alt_text_condition from 'reopened:(.*)')
           in (select s.puzzle_key from public.solves s))
  -- the companion reveal event (Wren) — kept as-is for the seed that names it.
  or (c.alt_text_condition = 'companion:revealed'
     and coalesce((select (flags ? 'companion_revealed') from public.arc_state where id = 1), false))
  -- GENERAL flag gate (W3): 'flag:<name>' reveals iff arc_state.flags carries <name> (gather-event
  -- cards, Nether/End lane cards, any future event-revealed card). Never the flags blob itself.
  or (c.alt_text_condition like 'flag:%'
     and coalesce(
           (select (flags ? substring(c.alt_text_condition from 'flag:(.*)')) from public.arc_state where id = 1),
           false));

comment on view public.v_archive is
  'Public, anon-readable Recovery Archive (W3a + 0008). SECURITY DEFINER; one row per REVEALED thread_card. '
  'Reveal gated in SQL: revealed_by_solve solved · alt_text_condition reopened:<key> solved · '
  'companion:revealed flag · flag:<name> arc flag set. An unrevealed body never leaves the view.';

grant select on public.v_archive to anon, authenticated;

commit;
