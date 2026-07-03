-- The Observance — 0009_observations.sql
-- The Observer Tier-1 store: grounded VERBATIM captures the world may sparsely quote back ("it heard
-- you say it"). Tier-0 (behavior — Tier0Observation / reports.ts) needs none of this and is untouched;
-- this is the WORDS tier, the one that must be handled with the most care.
--
-- THE DISCIPLINE THIS TABLE ENFORCES (the mandate: grounded-only · sparse · consent · degrade to
-- silence, never fabricate):
--   * GROUNDED: `text` is a REAL captured utterance, stored verbatim. The weaponizer only ever quotes
--     what is literally here — it never composes or paraphrases a "quote" (that would be fabrication).
--   * SET-ONCE USE: `weaponized_at` is null until the line has been surfaced, then stamped, so a quote
--     is used at most once — sparse by construction, never a nagging replay.
--   * CONSENT: capture is gated TWICE. Globally by settings.observer_capture (default OFF — nothing is
--     stored until the operator turns it on), and per-player by players.observer_opt_out (a person can
--     say "don't keep my words" and the capture skips them). The group already consented (known-author
--     lens); this is the individual floor.
--   * PRIVATE: RLS service-role only. NEVER anon-readable (unlike the archive) — this is PII. No view,
--     no grant. The public Record can never surface a captured utterance.
--
-- Additive + idempotent. Apply after 0001 (players/settings) exists. Shared DB.

begin;

-- 1. observations — one grounded captured utterance. player_id is the linked identity (nullable — an
--    unlinked speaker is kept by mc_uuid only, and is never weaponized until linked). source names the
--    surface it came from; context is provenance (channel / where); weaponized_at is the set-once "used".
create table if not exists public.observations (
  id            bigserial primary key,
  player_id     uuid references public.players(id) on delete cascade,
  mc_uuid       text,
  source        text not null check (source in ('discord', 'chat', 'voice')),
  text          text not null,
  context       text,
  observed_at   timestamptz not null default now(),
  weaponized_at timestamptz
);

create index if not exists idx_observations_unused
  on public.observations (observed_at)
  where weaponized_at is null;

alter table public.observations enable row level security;
-- No policy, no grant: deny-by-default. Only the service role (the capturer + the weaponizer) touches it.
revoke all on public.observations from anon, authenticated;

-- 2. per-player opt-out — the individual consent floor. Default false = kept (group consent); a person
--    who opts out is never captured and never quoted.
alter table public.players
  add column if not exists observer_opt_out boolean not null default false;

-- 3. the global capture switch — OFF until the operator enables it (nothing is stored before then).
insert into public.settings (key, value)
  values ('observer_capture', 'false'::jsonb)
  on conflict (key) do nothing;

commit;
