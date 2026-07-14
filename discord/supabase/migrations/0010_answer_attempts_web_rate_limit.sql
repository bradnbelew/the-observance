-- 0010_answer_attempts_web_rate_limit.sql — a real anonymous-flood guard for the record website.
--
-- HISTORICAL WHY. The retired record website answer-inscribe endpoint rate-limited
-- a KNOWN keeper by player_id (countRecentAttempts), but an UNRESOLVED name has no player_id to key on —
-- so unknown-name submissions were completely unthrottled (a cheap DB-write-spam vector on a public,
-- unauthenticated POST route; not a spoiler leak, since the response is uniform regardless). The web
-- resolver also logged every attempt under surface='discord' (the CHECK constraint's only non-'world'
-- option at the time), which its own comment already flagged as a stopgap: "A future migration could
-- add a 'web' surface." This is that migration.
--
-- V5 STATUS. The arbitrary-name website inscription endpoint is permanently closed (HTTP 410), and
-- the six fixed Copperline website nodes use the prerequisite-guarded V5 recording RPC instead. Keep
-- this additive migration in the ordered bundle so existing databases and historical audit rows remain
-- schema-compatible; it does not re-enable the retired endpoint.
--
-- WHAT. (1) Widen the surface CHECK to add 'web', so the record website's attempts are logged under
-- their own real surface instead of borrowing 'discord' (which the Discord bot's own unlinked-user
-- rate-limiting also reads — the two were at risk of quietly sharing a bucket). (2) Add a nullable
-- ip_hash column + a partial index, so an unresolved-name submission can be throttled by a hash of the
-- request's client IP instead of skipping rate-limiting entirely. Never a raw IP at rest.
--
-- Fully IDEMPOTENT + additive: drop/re-add the constraint (safe re-run, matches 0009's own idiom),
-- add-column-if-not-exists, create-index-if-not-exists. Safe on a fresh DB or a re-run.

alter table if exists public.answer_attempts
  drop constraint if exists answer_attempts_surface_check;

alter table if exists public.answer_attempts
  add constraint answer_attempts_surface_check
  check (surface in ('discord', 'world', 'web'));

alter table if exists public.answer_attempts
  add column if not exists ip_hash text;

-- Partial: only unresolved (player_id null) rows ever need an ip_hash lookback, so this stays small.
create index if not exists idx_answer_attempts_iphash_at
  on public.answer_attempts (ip_hash, at desc)
  where player_id is null and ip_hash is not null;
