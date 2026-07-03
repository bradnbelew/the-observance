-- The Observance — 0007_v_archive.sql
-- The Recovery Archive's public read surface (W3a — the field/offshoots wave).
--
-- WHY THIS FILE. The reshape authored 42 place-anchored thread_cards (the reconstruction
-- field: who / place / happened / surface / human) — verbatim recovered material, each
-- clustered under a thread and wired into a citation web. They were seeded + voice-covered
-- + self-tested, but NOTHING ever surfaced them to players: thread_cards was written and
-- never read. The Record showed only its six coarse stone-lines (v_record). This migration
-- gives the archive its public reading surface, so the "dig, connect the dots, watch the
-- threads rhyme" layer the cards were authored for becomes a thing players can actually do.
--
-- SECURITY MODEL — IDENTICAL to v_record (0004) and the 0001_init views. v_archive is a
-- SECURITY DEFINER view (security_invoker = false): it runs with the OWNER's privileges, so
-- anon reads the neutral projection with NO base-table grant and NO anon RLS policy. anon
-- never touches a raw row — only the reveal-gated columns below. Deny-by-default holds
-- (thread_cards / thread_card_bodies / solves / arc_state have RLS on, no anon policy,
-- revoked anon grants); this view is the ONLY anon path to a card.
--
-- REVEAL DISCIPLINE (the whole point — a body NEVER leaves the view ahead of progress). The
-- WHERE clause below is the reveal rule, computed here in SQL from public.solves +
-- arc_state.flags (the same shape v_record counts). A card row is emitted iff:
--   * revealed_by_solve is a solved puzzle_key, OR
--   * alt_text_condition = 'reopened:<key>' and <key> is solved (the re-surface trigger), OR
--   * alt_text_condition = 'companion:revealed' and arc_state.flags carries companion_revealed.
-- A card with no solve-gate and no recognised condition (the few pure 'found-on-descent'
-- atmosphere cards) is WITHHELD until the seed gives it an explicit gate — the Record's own
-- ethos (withhold, never over-reveal). No unearned body reaches anon; the archive un-redacts
-- in lockstep with real solves, exactly like the six stone-lines already do.
--
-- INV-1 (the voice rule) STAYS INTACT. The authored English lives in ONE place —
-- discord/src/voice.archive.ts (the archive/npcLines maps; canon.ts GUARD-9 pins every
-- body_voice_key to an entry there). thread_card_bodies is that text's DB REALIZATION, written
-- ONLY by the showrunner's archive materializer (archive.run.ts) as service_role — never
-- hand-authored here, never composed in the dashboard. The dashboard is a dumb renderer of the
-- neutral view, the same way it renders v_record.
--
-- Additive + idempotent (create table if not exists / create or replace view). Apply as the
-- migration/service role AFTER the discord lineage's thread_cards (0005_threads.sql) and
-- solves (0004_oracle.sql) exist on the live project. Touches nothing else.

begin;

-- 1. thread_card_bodies — the resolved-body store. One row per card, body = the recovered
--    text resolved from voice.archive.ts by the showrunner materializer. RLS on, NO anon
--    grant: only the service role writes it, only v_archive reads it (as the view owner).
--    Holding the bodies here (not in thread_cards) keeps the authored source single (INV-1):
--    the materializer is the sole writer, and it reads voice.archive.ts, never the reverse.
create table if not exists public.thread_card_bodies (
  card_key   text primary key references public.thread_cards(card_key) on delete cascade,
  body       text not null,
  updated_at timestamptz not null default now()
);

alter table public.thread_card_bodies enable row level security;
-- (no policy, no grant: deny-by-default. service_role bypasses RLS to upsert; v_archive
--  reads as owner. anon has neither a policy nor a grant, so it can never read this table.)
revoke all on public.thread_card_bodies from anon, authenticated;

-- 2. v_archive — the neutral, anon-readable reconstruction field. One row per REVEALED card,
--    carrying only the reconstruction columns the Record is allowed to show: the thread it
--    clusters under (+ the thread's player-facing label/colour, from public.threads), the card
--    title + recovered body, its kind (rumor/explore/verified/contradicted), the citation web
--    (references_card_key — kebab slugs, no bodies), and the sort orders. No answer, no player,
--    no custom label, no sealed flag. The reveal gate is the WHERE clause (see header).
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
  -- the companion reveal event (Wren): a single flag, never the flags blob itself.
  or (c.alt_text_condition = 'companion:revealed'
     and coalesce((select (flags ? 'companion_revealed') from public.arc_state where id = 1), false));

comment on view public.v_archive is
  'Public, anon-readable Recovery Archive (W3a). SECURITY DEFINER; emits ONE row per REVEALED '
  'thread_card — {thread, label, colour, title, body, kind, references, sort}. Reveal is gated in '
  'SQL from solves + arc_state.flags (solve / reopened:<key> / companion:revealed); an unrevealed '
  'body never leaves the view. Bodies are materialised from voice.archive.ts by the showrunner '
  '(INV-1). Consumed by the Record archive reading-room via archive-projection.ts.';

-- The ONLY grant: anon (and authenticated) may SELECT the neutral view. Mirrors v_record.
grant select on public.v_archive to anon, authenticated;

commit;
