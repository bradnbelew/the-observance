-- The Observance — 0005_threads.sql
-- The reconstruction layer (the Recovery Archive), the side-quest breadth, the
-- discover-by-punishment custom state, and the NPC dialogue state. All additive; runs
-- after 0004_oracle.sql. Closes red-team blocker B-7 (the thread-layer coverage skeleton)
-- and the INTEGRATION §5 data-model additions.
--
-- Security model (identical to 0004): RLS enabled on every new table; service_role bypasses
-- it; NO anon / authenticated policies (these carry spoiler structure + per-player state).
--
-- Everything in the `public` schema. Idempotent (create … if not exists, add column if not
-- exists, on conflict do nothing) — safe to re-run.

begin;

-- ===========================================================================
-- 1. threads — the five reconstruction threads (the Recovery Archive columns).
--    Progress = these filling in, NOT a step counter (the rumor→verify skeleton).
--    Canonical keys mirror forge/canon.ts THREADS; a thread-coverage self-test asserts
--    every puzzles.thread_key / thread_cards.thread_key is one of these.
-- ===========================================================================

create table if not exists public.threads (
  thread_key      text primary key,            -- who | place | happened | surface | human
  label           text not null,               -- player-facing column title
  color           text not null,               -- card tint (amber/green/red/grey/black)
  sort_order      int  not null default 0,
  created_at      timestamptz not null default now()
);

-- Seed the five canonical threads (the questions the whole game answers).
insert into public.threads (thread_key, label, color, sort_order) values
  ('who',      'who they were',              'amber', 1),
  ('place',    'what this place was',        'green', 2),
  ('happened', 'what happened',              'red',   3),
  ('surface',  'what is on the surface',     'grey',  4),
  ('human',    'were they human',            'black', 5)
on conflict (thread_key) do nothing;

-- ===========================================================================
-- 2. thread_cards — the ship-log: every find is a place-anchored card that clusters
--    under a thread. Rumor cards flip to verified/contradicted on arrival; conditional
--    reveals expand once referenced cards are found. Authored content lands here.
-- ===========================================================================

create table if not exists public.thread_cards (
  card_key            text primary key,
  thread_key          text not null references public.threads(thread_key) on delete cascade,
  title               text not null,
  -- the in-character body comes from voice.ts / the corpus via a key, never inline english.
  body_voice_key      text,
  -- where in the world this card is anchored (a sites.yml id), if any.
  anchor_site_id      text,
  -- 'rumor' = heard secondhand (a dotted line to an unvisited place); 'explore' = verified
  -- firsthand on-site. Arriving can flip a rumor to 'verified' or 'contradicted'.
  card_kind           text not null default 'explore'
                        check (card_kind in ('rumor','explore','verified','contradicted')),
  -- cards this one references (the citation web); a conditional reveal expands once these
  -- are all found (alt_text_condition names the gate).
  references_card_key text[] not null default '{}',
  -- the solve (puzzle_key) that surfaces this card, if it is solve-gated.
  revealed_by_solve   text,
  -- the named condition under which alt/expanded text unlocks (e.g. 'found:warden-tag').
  alt_text_condition  text,
  sort_order          int not null default 0,
  created_at          timestamptz not null default now()
);

create index if not exists idx_thread_cards_thread on public.thread_cards (thread_key, sort_order);

-- ===========================================================================
-- 3. side_quests — the breadth layer (off-spine threads that GATE NOTHING; they pay
--    lore/atmosphere/items/time). The longevity engine's non-spine volume.
-- ===========================================================================

create table if not exists public.side_quests (
  quest_key       text primary key,
  thread_key      text references public.threads(thread_key) on delete set null,
  -- the puzzle/row that opens it (a side_quest-outcome node), if any.
  entry_puzzle_key text,
  -- what it pays (free-text authoring note; the actual reward is the node's outcome).
  reward          text,
  -- BREADTH INVARIANT: a side quest must never gate spine progress.
  gates_progress  boolean not null default false check (gates_progress = false),
  -- tier: ambient (just found) | rumored (pointed at, verify) | keyed (needs a side-cipher).
  tier            text not null default 'ambient'
                    check (tier in ('ambient','rumored','keyed')),
  -- rough authored play-minutes (the HOURS ledger; red-team B-1: budget time, not nodes).
  est_minutes     int not null default 0,
  created_at      timestamptz not null default now()
);

-- ===========================================================================
-- 4. punishment_state — the spine of discover-by-punishment (Req 3). Per (player, way):
--    the custom pass escalates a soft, reversible toll on repeated transgression until the
--    player DECIPHERS the way (from a teaching surface) and stops. NOT a hard punishment —
--    tolls take warmth, not progress (INV-8).
-- ===========================================================================

create table if not exists public.punishment_state (
  id                 bigserial primary key,
  player_id          uuid not null references public.players(id) on delete cascade,
  -- canonical the_-prefixed custom key (forge/canon.ts CUSTOM_KEYS).
  custom_key         text not null,
  transgression_count int not null default 0,
  last_toll_at       timestamptz,
  -- 0 none, 1 deniable(stage A), 2 named(stage B / warn). escalates with repetition.
  toll_tier          int not null default 0,
  -- true once the player has performed the way (learned it); the toll then clears.
  deciphered         boolean not null default false,
  -- which in-world clue teaches this way (a sites.yml id / doc) — so the loop is fair.
  teaching_site_id   text,
  updated_at         timestamptz not null default now(),
  unique (player_id, custom_key)
);

create index if not exists idx_punishment_player on public.punishment_state (player_id);

-- ===========================================================================
-- 5. npc_dialogue_state + npc_quests — per-player NPC conversation + fetch/return state,
--    so surface NPCs (Citizens2) remember and branch on the dossier. The Iss tree flips
--    warm→cold on arc_state.flags.iss_caught.
-- ===========================================================================

create table if not exists public.npc_dialogue_state (
  id          bigserial primary key,
  player_id   uuid not null references public.players(id) on delete cascade,
  npc_key     text not null,              -- aro | wenna | coll | dob | old-pell | …
  -- conversation cursor + any per-NPC flags (jsonb so dialogue trees evolve without migration).
  state       jsonb not null default '{}'::jsonb,
  updated_at  timestamptz not null default now(),
  unique (player_id, npc_key)
);

create table if not exists public.npc_quests (
  id          bigserial primary key,
  player_id   uuid not null references public.players(id) on delete cascade,
  quest_key   text not null,
  status      text not null default 'offered'
                check (status in ('offered','active','done','failed')),
  updated_at  timestamptz not null default now(),
  unique (player_id, quest_key)
);

create index if not exists idx_npc_dialogue_player on public.npc_dialogue_state (player_id);
create index if not exists idx_npc_quests_player    on public.npc_quests (player_id);

-- ===========================================================================
-- 6. puzzles column adds — tag each node to a thread + (optionally) the way it teaches.
--    Lets the Recovery Archive cluster a solved node's card, and links the cipher↔custom
--    layers. Additive + nullable; existing rows unaffected.
-- ===========================================================================

alter table public.puzzles
  add column if not exists thread_key    text references public.threads(thread_key),
  add column if not exists teaches_custom text;

create index if not exists idx_puzzles_thread on public.puzzles (thread_key) where thread_key is not null;

-- ===========================================================================
-- 7. Row Level Security — service-role ONLY (spoiler structure + per-player state).
-- ===========================================================================

alter table public.threads            enable row level security;
alter table public.thread_cards       enable row level security;
alter table public.side_quests        enable row level security;
alter table public.punishment_state   enable row level security;
alter table public.npc_dialogue_state enable row level security;
alter table public.npc_quests         enable row level security;

revoke all on public.threads            from anon;  revoke all on public.threads            from authenticated;
revoke all on public.thread_cards       from anon;  revoke all on public.thread_cards       from authenticated;
revoke all on public.side_quests        from anon;  revoke all on public.side_quests        from authenticated;
revoke all on public.punishment_state   from anon;  revoke all on public.punishment_state   from authenticated;
revoke all on public.npc_dialogue_state from anon;  revoke all on public.npc_dialogue_state from authenticated;
revoke all on public.npc_quests         from anon;  revoke all on public.npc_quests         from authenticated;

revoke all on sequence public.punishment_state_id_seq   from anon;  revoke all on sequence public.punishment_state_id_seq   from authenticated;
revoke all on sequence public.npc_dialogue_state_id_seq from anon;  revoke all on sequence public.npc_dialogue_state_id_seq from authenticated;
revoke all on sequence public.npc_quests_id_seq         from anon;  revoke all on sequence public.npc_quests_id_seq         from authenticated;

commit;
