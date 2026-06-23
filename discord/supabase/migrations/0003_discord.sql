-- The Observance — Discord service schema
-- 0003_discord.sql
--
-- The Discord bot connects with the service_role key (server-side only), which
-- BYPASSES RLS. It links Discord users to existing Minecraft players and reads
-- pre-authored whisper hints.
--
-- Security model (consistent with 0001_init.sql):
--   * RLS is enabled on every new table.
--   * service_role bypasses RLS — the bot can do anything.
--   * NO anon / authenticated policies are added for `hints` — it is service-role
--     ONLY (the pre-authored hint bodies are spoilers and never reach the public
--     or the dashboard's authenticated admins through this table).
--
-- Everything runs in the `public` schema.

begin;

-- ---------------------------------------------------------------------------
-- players.discord_id — links a Discord user to a Minecraft player.
-- Nullable: a player may exist without ever linking Discord.
-- Unique: a Discord account maps to at most one player.
-- ---------------------------------------------------------------------------

alter table public.players
  add column if not exists discord_id text unique;

-- ---------------------------------------------------------------------------
-- hints — the PRE-AUTHORED whisper hints, keyed by (puzzle_key, tier).
-- Seeded later from the sealed arc (NOT committed to code). The bot only READS
-- this table; it is left empty here.
-- ---------------------------------------------------------------------------

create table if not exists public.hints (
  id         bigserial primary key,
  puzzle_key text not null,
  tier       int  not null,
  body       text not null,
  unique (puzzle_key, tier)
);

-- Fast lookup path used by the bot: getHint(puzzle_key, tier).
create index if not exists idx_hints_puzzle_tier
  on public.hints (puzzle_key, tier);

-- ---------------------------------------------------------------------------
-- Row Level Security — enabled, service-role ONLY.
-- No anon and no authenticated policies: only the service_role (which bypasses
-- RLS entirely) can read or write `hints`.
-- ---------------------------------------------------------------------------

alter table public.hints enable row level security;

-- Defensive: ensure no blanket grants leak from Supabase defaults.
revoke all on public.hints from anon;
revoke all on public.hints from authenticated;
revoke all on sequence public.hints_id_seq from anon;
revoke all on sequence public.hints_id_seq from authenticated;

commit;
