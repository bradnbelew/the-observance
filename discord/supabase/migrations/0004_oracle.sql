-- The Observance — Oracle schema (the non-linear clue web).
-- 0004_oracle.sql
--
-- The ORACLE is the closed loop: a player who solves a forged clue submits the
-- plaintext answer (in Discord #the-record OR on an in-world answer-sign), the
-- bot/plugin matches it against the OPEN puzzles, and — if it matches — the world
-- answers. This migration creates the authoring + resolution tables that loop
-- runs on.
--
-- DIFFICULTY: HARD, NON-LINEAR WEB. Many puzzles are `active` at once. A single
-- answer resolves to ONE of five OUTCOME TYPES (next_clue | lore | dead_end |
-- side_quest | main_beat). Multiple correct answers per puzzle are supported
-- (accepted_answers is an array). Some clues advance, some only reveal lore,
-- some are TRUE-BUT-NOT-A-DOOR dead ends (red-herring texture the watcher
-- acknowledges in voice but that leads nowhere), some open optional side quests,
-- some fire a main story beat. Authors wire ANY of these per row.
--
-- Security model (consistent with 0001_init.sql / 0003_discord.sql):
--   * RLS enabled on every new table.
--   * service_role bypasses RLS — the bot/plugin (service-role, server-side) can
--     do anything; NO anon / authenticated policies are added. `accepted_answers`
--     are spoilers and must never reach the public or the dashboard admins through
--     these tables.
--
-- Everything runs in the `public` schema.

begin;

-- ===========================================================================
-- 0. beat_queue reconciliation — additive, non-breaking.
--
-- The plugin (data/rows/BeatQueueRow.java + beats/RealBeatEnactor.java) ALREADY
-- reads `mc_uuid`, `site_id`, `priority` and writes status 'failed', and
-- fetchActionableBeats() orders by `priority`. The canonical 0001 beat_queue has
-- only `target` and a status check WITHOUT 'failed'. The oracle's `unlock` beats
-- must be enqueued in a shape the plugin can read + target, so we bring the live
-- schema up to the contract the plugin already speaks. Existing `target`-based
-- inserts (e.g. /whisper) keep working untouched.
-- ===========================================================================

alter table public.beat_queue
  add column if not exists mc_uuid  text,
  add column if not exists site_id  text,
  add column if not exists priority int;

-- Widen the status check to include 'failed' (the plugin's terminal state for a
-- beat whose enactor threw). Drop the old constraint by its default name, re-add.
do $$
begin
  if exists (
    select 1 from pg_constraint
    where conrelid = 'public.beat_queue'::regclass
      and conname  = 'beat_queue_status_check'
  ) then
    alter table public.beat_queue drop constraint beat_queue_status_check;
  end if;
end $$;

alter table public.beat_queue
  add constraint beat_queue_status_check
  check (status in ('pending','approved','skipped','fired','failed'));

-- APPROVAL GATE: the plugin poller fires `status=eq.approved` ONLY, ordered by
-- priority desc, created_at asc — so `pending` beats wait for a human to approve
-- them in the dashboard (the showrunner CONFIRM mode + the Accepting trigger).
-- The dashboard, in turn, lists the `pending` beats as its approval queue. This
-- partial index covers BOTH access paths (the poller's approved read and the
-- dashboard's pending read), so both stay index-served.
create index if not exists idx_beat_queue_actionable
  on public.beat_queue (priority desc nulls last, created_at asc)
  where status in ('pending','approved');

-- ===========================================================================
-- 1. puzzles — the authored clue web. One row per forged clue.
--
-- puzzle_key is the STABLE FNV-1a id from forgeClue() (e.g. "clue_caesar_1a2b3c4d").
-- It is the SAME key hints (0003), whisper_events, and solves are addressed by, so
-- the whole loop points at one puzzle.
-- ===========================================================================

create table if not exists public.puzzles (
  -- the forgeClue() FNV-1a key — the join point for the entire web.
  puzzle_key       text primary key,

  -- author-facing label (spoiler; never shown to players in voice).
  title            text not null,

  -- normalized accepted solutions. MULTIPLE correct answers supported. Each
  -- entry MUST already be stored in normalized form (see ORACLE.md §normalize):
  -- case-folded, trimmed, internal whitespace collapsed to single spaces,
  -- punctuation stripped. The resolver normalizes the player's raw input the same
  -- way and tests set-membership against this array.
  accepted_answers text[] not null check (array_length(accepted_answers, 1) >= 1),

  -- what a correct answer DOES. The web's branching dimension:
  --   next_clue  — advances: reveals/forges the next clue (a door).
  --   lore       — reveals story/character info; NO progression (not a door).
  --   dead_end   — a TRUE answer deliberately not a door: the watcher
  --                acknowledges it in voice, it leads nowhere. red-herring texture.
  --   side_quest — opens an optional thread (a door to an off-spine branch).
  --   main_beat  — fires a main-story event/unlock (a door to the spine).
  outcome_type     text not null
                     check (outcome_type in
                       ('next_clue','lore','dead_end','side_quest','main_beat')),

  -- the resolution recipe. Shape (all keys optional unless noted):
  --   {
  --     "voice_key": "oracleNextClue",        -- which voice.ts line speaks the reply (REQUIRED)
  --     "voice_args": { "name": "...", ... }, -- optional args spread into the voice fn
  --     "next_puzzle_key": "clue_book_…",     -- for next_clue / side_quest: the puzzle this opens
  --     "set_flags": { "found_well": true },  -- optional arc_state.flags to set on solve
  --     "beat": {                             -- OPTIONAL in-world reward to enqueue (see ORACLE.md)
  --       "type": "unlock",                   --   beat_queue.type
  --       "mc_uuid": "{solver}",              --   "{solver}" placeholder → resolved to the solving player's uuid
  --       "site_id": "well_shrine",           --   optional sites.yml id
  --       "priority": 10,                     --   optional ordering hint
  --       "payload": { "step": "door_open",   --   the UnlockBeat dispatcher payload
  --                    "step_payload": { "radius": 3, "open": true } }
  --     }
  --   }
  -- dead_end rows carry voice_key only (the acknowledgement) and NO next_puzzle_key
  -- and NO beat — it is true, it is heard, it opens nothing.
  outcome_payload  jsonb not null default '{}'::jsonb,

  -- author-facing position on the web (act/rung). For ordering/visualization only;
  -- the resolver does NOT gate on it (the web is non-linear). Defaults to 0.
  movement         int not null default 0,

  -- is this puzzle OPEN for answers? The resolver only ever matches against rows
  -- where active = true. Authors flip this to stage/retire branches of the web.
  active           boolean not null default true,

  -- per-puzzle attempt cap (anti-brute-force, on top of the global rate limit).
  -- NULL = no per-puzzle cap (the global token bucket still applies). When set,
  -- a player who reaches it is met with in-voice silence/withholding, never a hint.
  max_attempts     int,

  created_at       timestamptz not null default now()
);

-- Resolver hot path: "give me the OPEN puzzles to match this answer against".
create index if not exists idx_puzzles_active
  on public.puzzles (active)
  where active = true;

-- ===========================================================================
-- 2. solves — the replay guard. One row per (puzzle, player) that resolved.
--
-- IDEMPOTENT: unique(puzzle_key, player_id) means solving the same puzzle twice
-- cannot double-fire its unlock. The resolver INSERTs here with ON CONFLICT DO
-- NOTHING *before* enqueuing the beat; only a genuinely-new insert proceeds to
-- enqueue the reward.
-- ===========================================================================

create table if not exists public.solves (
  id           bigserial primary key,

  -- which puzzle was solved.
  puzzle_key   text not null references public.puzzles(puzzle_key) on delete cascade,

  -- WHO solved it. player_id is the canonical link; mc_uuid + discord_id are
  -- denormalized for audit and for whichever surface (in-world / discord) the
  -- solve arrived on. player_id is NOT NULL (a solve is always a known keeper).
  player_id    uuid not null references public.players(id) on delete cascade,
  mc_uuid      text,
  discord_id   text,

  -- how many attempts this player made before the solve landed (audit/balance).
  attempt_count int not null default 1,

  solved_at    timestamptz not null default now(),

  -- the replay guard: one solve per puzzle per player.
  unique (puzzle_key, player_id)
);

create index if not exists idx_solves_player
  on public.solves (player_id);

-- ===========================================================================
-- 3. answer_attempts — append-only audit + rate-limit substrate.
--
-- EVERY attempt is logged (matched or not) so the resolver can enforce cooldown
-- + token bucket and so authors can see brute-force pressure. Never reveals which
-- PART was right — it records the whole normalized string and a single matched bool.
-- ===========================================================================

create table if not exists public.answer_attempts (
  id          bigserial primary key,

  -- which puzzle this attempt matched, if any. NULL when the input matched no
  -- open puzzle (ordinary chat the watcher stays silent on, or a true miss).
  puzzle_key  text references public.puzzles(puzzle_key) on delete set null,

  -- who attempted. player_id may be NULL for an unlinked discord user whose
  -- message was scanned in #the-record (we still rate-limit them by discord_id).
  player_id   uuid references public.players(id) on delete set null,
  mc_uuid     text,
  discord_id  text,

  -- where it came from: 'discord' (#the-record scan or future command) or 'world'
  -- (an in-world answer-sign). Used to scope rate limits per surface if desired.
  surface     text check (surface in ('discord','world')),

  -- the player's exact input (audit) and its normalized form (what was matched).
  raw         text,
  normalized  text,

  -- did `normalized` hit an accepted_answers entry of an OPEN puzzle?
  matched     boolean not null default false,

  at          timestamptz not null default now()
);

-- Rate-limit lookups: "how many attempts has this player made in the last window?"
-- Two indexes — by linked player, and by raw discord_id for unlinked scanners.
create index if not exists idx_answer_attempts_player_at
  on public.answer_attempts (player_id, at desc);

create index if not exists idx_answer_attempts_discord_at
  on public.answer_attempts (discord_id, at desc);

create index if not exists idx_answer_attempts_mc_at
  on public.answer_attempts (mc_uuid, at desc);

-- ===========================================================================
-- 4. Row Level Security — enabled, service-role ONLY (spoiler tables).
-- No anon / authenticated policies: only service_role (which bypasses RLS) reads
-- or writes these. Defensive revokes mirror 0003_discord.sql.
-- ===========================================================================

alter table public.puzzles         enable row level security;
alter table public.solves          enable row level security;
alter table public.answer_attempts enable row level security;

revoke all on public.puzzles         from anon;
revoke all on public.puzzles         from authenticated;
revoke all on public.solves          from anon;
revoke all on public.solves          from authenticated;
revoke all on public.answer_attempts from anon;
revoke all on public.answer_attempts from authenticated;

revoke all on sequence public.solves_id_seq          from anon;
revoke all on sequence public.solves_id_seq          from authenticated;
revoke all on sequence public.answer_attempts_id_seq from anon;
revoke all on sequence public.answer_attempts_id_seq from authenticated;

commit;
