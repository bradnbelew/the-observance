-- THE OBSERVANCE — apply-tonight.sql (auto-generated 2026-07-01, v3)
-- Paste the whole file into the Supabase SQL Editor (project fdnmhbpxnodrnbrzrlqq) and Run.
-- FULLY IDEMPOTENT + additive — safe to re-run. Order: 0005 -> 0006 -> 8 seeds -> schema-repair.



-- ============================================================
-- FILE: migrations/0005_threads.sql
-- ============================================================

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


-- ============================================================
-- FILE: migrations/0006_requires_flags.sql
-- ============================================================

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


-- ============================================================
-- FILE: seeds/puzzles_seed.sql
-- ============================================================

-- The Observance — PUZZLES SEED (realizes design/clue-web.md in the 0004 schema)
-- discord/supabase/seeds/puzzles_seed.sql
--
-- One INSERT per web node (design/clue-web.md §3). Every accepted_answers entry is
-- ALREADY NORMALIZED per ORACLE.md §2 (NFKC → lower → [^a-z0-9 ]→single space →
-- collapse ws → trim). The resolver normalizes player input the same way and tests
-- whole-string set-membership, so a raw "Bow, at!" or "-1280, 64" must equal one of
-- these stored strings AFTER normalization. Coordinate answers therefore appear in
-- BOTH unsigned and direction-word forms (the minus sign is dropped by step 3, so a
-- signed coord like "-1280 64" normalizes to "1280 64" — we store "1280 64" and also
-- the spoken bearing the carving gives).
--
-- Outcome wiring (ORACLE.md §3):
--   next_clue  → voice_key + next_puzzle_key (the door onward)
--   lore       → voice_key (+ voice_args.fragment) ONLY — a told secret, no door
--   dead_end   → voice_key ONLY — TRUE but opens nothing (no next, no beat)
--   side_quest → voice_key + next_puzzle_key (off-spine branch)
--   main_beat  → voice_key + 'unlock' beat {type,site_id,step,step_payload} + set_flags
--
-- Beats are enqueued status='approved' by the Oracle path (resolve.ts → the player
-- EARNED it; ORACLE.md §4). The "{solver}" mc_uuid placeholder is resolved to the
-- solving player's real uuid at enqueue time. beat.payload is real jsonb (not a
-- quoted string) — see ORACLE.md §4 "Plugin payload typing — RESOLVED".
--
-- IDEMPOTENT: ON CONFLICT (puzzle_key) DO UPDATE re-applies every authored field, so
-- re-running this file is safe and is the canonical way to edit a node. We touch the
-- 5 author-owned columns + active; created_at is left to the existing row.
--
-- Run AFTER 0004_oracle.sql, as service_role (RLS bypass — these are spoiler tables).

begin;

insert into public.puzzles
  (puzzle_key, title, accepted_answers, outcome_type, outcome_payload, movement, active, max_attempts)
values

-- ===========================================================================
-- MOVEMENT I — The Notice (entry surface)
-- ===========================================================================

-- D01 the-record-opens — found on the group's OWN base lectern (no oracle gate
-- normally; this row exists so a keeper who transcribes the buried line still gets
-- the lore acknowledgement). FACT 1 + FACT 2; buries the FACT 14 + "seventh mark" seeds.
( 'm1-record-opens',
  'the record opens',
  array[
    'the record counts the living by name',
    'it counts the living by name',
    'counted by name'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the record was open before you found it. it counts the living by name, and it grades them by laws no one was told. it does not close at the rite. a seventh mark the record will not keep.'
    )
  ),
  1, true, null ),

-- m1-named-habit — recognizing your OWN measured habit named in a report before you
-- knew it was a custom. TRUE in-world reading, terminal dread (heard, opens nothing).
( 'm1-named-habit',
  'named before the telling',
  array[
    'it named my habit before i knew it was a custom',
    'graded before the rules were known',
    'i was measured before i was told',
    'the world was grading before the rules were known'
  ],
  'dead_end',
  -- B2 (dead-ends-with-teeth): the taunt family is selected by voice_args.kind, passed
  -- through resolve.ts to the kind-switched oracleDeadEnd(kind). 'name' = a true name that
  -- keeps no door (the Watcher flat + honest; the teeth are the liar's, never the Watcher's).
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'name')),
  1, true, null ),

-- rosetta-ring — the literacy gate (TWO doors: server-icon ring B + founder note C).
-- main_beat: unlocks the master script. FACT 3 + FACT 4 seed; front-margin seeds F15.
( 'rosetta-ring',
  'learn them as we learned them',
  -- The ring names the REAL ways (audit, HIGH): 'ward'/'covering' were orphans (no detection,
  -- keeper, or thread-tag). Replaced with the_unspoken + the_sacred_beast — the two ways otherwise
  -- taught by no node — so the literacy gate is where they're learned. (GO-LIVE: the rune ring
  -- structure must carve these, not ward/covering — see design/structures.md.)
  array[
    'bow offering kept light deep line unspoken sacred beast',
    'bow offering keptlight deepline unspoken sacred beast'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('rosetta_known', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'priority', 20,
      'payload', jsonb_build_object(
        'step', 'advancement_toast',
        'step_payload', jsonb_build_object('key', 'observance:the_ring_is_whole')
      )
    )
  ),
  1, true, null ),

-- ===========================================================================
-- MOVEMENT II — The Keeper-Stone Field (six, any order; all require rosetta-ring)
-- ===========================================================================

-- stone-vaun — Caesar. FACT 5 (the Offering never kept) + FACT 4 (the land counts
-- first). Pure lore. Feeds m2-rhyme; the true descent turn no-wall-catch later names.
( 'stone-vaun',
  'i counted them in the dark',
  array[
    'give the first of the deep back to the deep',
    'the land counts first',
    'i counted them in the dark and gave none back'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'vaun counted everything and gave nothing back. the given-back column of his ledger is blank. the land counts first, and it had already counted him.'
    )
  ),
  2, true, null ),

-- stone-mara — book-cipher. SPINE KEY. next_clue → undercroft-descent. FACT 5 (map
-- never the tool) + FACT 13 seed. Oracle accepts the ASSEMBLED SENTENCE, not triples.
( 'stone-mara',
  'page line word',
  array[
    'descend and bow at the unbroken light'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'undercroft-descent',
    'set_flags', jsonb_build_object('mara_read', true)
  ),
  2, true, null ),

-- stone-sella — Atbash/mirror + bearing. side_quest → seventh-shrine. FACT 5 + seeds
-- the Seventh. Coord-tolerant: bearing words AND the unsigned far-water coordinate.
( 'stone-sella',
  'what the surface keeps',
  array[
    'south by the far water where she did not come back',
    'south by the far water',
    'the last marker is not the last'
  ],
  'side_quest',
  jsonb_build_object(
    'voice_key', 'oracleSideQuest',
    'next_puzzle_key', 'seventh-shrine',
    'set_flags', jsonb_build_object('seventh_suspected', true)
  ),
  2, true, null ),

-- stone-orin — substitution + CROUCH-only reveal. next_clue → orin-threshold (M4).
-- FACT 5 (bowed at last to no one) + FACT 6 seed (breaks at "i —").
( 'stone-orin',
  'i thought it small',
  array[
    'i thought it small it was not small',
    'threshold',
    'the bow is the smallest of the ways'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'orin-threshold'
  ),
  2, true, null ),

-- stone-brann — beacon colour-sequence + night/black-moon gate + count-the-fires.
-- Pure lore. FACT 11 (one fire never out) + FACT 12 (same word, people/flame/stone).
( 'stone-brann',
  'do not close your eyes here',
  array[
    'one fire was never doused',
    'do not close your eyes here',
    'the one fire that will not be doused'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'count the fires at night. one of them never went out, and no hand tends it. they had one word for the people and the flame and the cold stone. do not close your eyes here.'
    )
  ),
  2, true, null ),

-- stone-iss-wall — Vigenère, key=ISS (his own name). next_clue forks to BOTH the
-- dead-shrine (false lead) and iss-doubt (the catch). FACT 7 (the planted lie).
-- max_attempts:6 — the warm reading is a shortish answer, capped against brute force.
( 'stone-iss-wall',
  'the ways are a wall',
  array[
    'the one who turned away',
    'iss'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'iss-doubt',
    'set_flags', jsonb_build_object('iss_key_turned', true)
  ),
  2, true, 6 ),

-- iss-warm — the WARM MISREADING of Iss's stone: read trustingly it COMFORTS ("the ways
-- are a wall against the watching"). Trusting the liar routes you to HIS coordinate — the
-- dead shrine, a grave (red-team B-6: this is the inbound edge iss-dead-shrine lacked).
-- The skeptical name-as-key reading on stone-iss-wall instead goes to iss-doubt → the catch.
( 'iss-warm',
  'the warm reading',
  array[
    'the ways are a wall against the watching'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'iss-dead-shrine',
    'set_flags', jsonb_build_object('iss_trusted', true)
  ),
  2, true, null ),

-- m2-rhyme — read any two keeper stones side by side; the fates rhyme. TRUE,
-- emotionally load-bearing, opens NO door (recolors what you have). Collective.
( 'm2-rhyme',
  'the rhyming chorus',
  array[
    'each fate matches the custom they broke',
    'the stones are warnings shaped like us',
    'their fates rhyme with the ways they broke'
  ],
  'dead_end',
  -- B2: 'count' kind — a true tally that opens nothing ("six. it opens nothing.").
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'count')),
  2, true, null ),

-- ===========================================================================
-- MOVEMENT II→IV — The Liar Thread (seed → DEAD-END + doubt → catch)
-- ===========================================================================

-- iss-dead-shrine — THE LOAD-BEARING RED HERRING. Iss's coordinate genuinely WORKS
-- and leads to a real place — a GRAVE, not the threshold. dead_end: heard, opens
-- nothing. Coord-tolerant: place names AND the west-and-down unsigned coordinate.
( 'iss-dead-shrine',
  'the dead shrine',
  array[
    'the dead shrine',
    'the cold hearth',
    'nothing is kept here',
    'west and down',
    'west and down to the cold hearth'
  ],
  'dead_end',
  -- B2: 'place' kind — a real place that is a grave, not a road ("a place. it keeps no road on.").
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'place')),
  2, true, null ),

-- iss-doubt — turn ISS's key on the OTHER stones; it disagrees with every honest
-- carving. next_clue → no-wall-catch. Overturns FACT 7 by accumulation.
( 'iss-doubt',
  'we checked the lock',
  array[
    'we checked the lock',
    'his key is his own name and his name is the one who turned away',
    'ask first what a wall is for'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'no-wall-catch',
    'set_flags', jsonb_build_object('iss_doubted', true)
  ),
  3, true, null ),

-- no-wall-catch — re-walk a clue falsely marked "kept · solved"; the Stone-after
-- contradicts Iss line for line. main_beat: flips Iss warm→cold, yields the TRUE
-- final coordinate, unlocks the rite path. FACT 8. set_flags drives the dialogue flip.
( 'no-wall-catch',
  'no wall was ever built here',
  array[
    'no wall was ever built here',
    'they were the reaching let in',
    'what iss sent you to was a grave',
    'back to vauns stone turn down'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('iss_caught', true, 'true_coord_known', true),
    'next_puzzle_key', 'rite-tokens',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'priority', 15,
      'payload', jsonb_build_object(
        'step', 'private_message',
        'step_payload', jsonb_build_object('key', 'iss.dialogue.turns_cold')
      )
    )
  ),
  4, true, null ),

-- ===========================================================================
-- MOVEMENT III — The Undercroft + the Seventh side quest
-- ===========================================================================

-- undercroft-descent — PERFORM Mara's sentence: descend at the unbroken light.
-- main_beat: leaves the group's world for the keepers'. → undercroft-fog.
( 'undercroft-descent',
  'descend at the unbroken light',
  array[
    'descend at the unbroken light',
    'descend through the lectern door'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('undercroft_open', true),
    'next_puzzle_key', 'undercroft-fog',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'unbroken_light',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  3, true, null ),

-- undercroft-fog — witness the altar room rebuild WRONG; the single lit point in a
-- doused world. Pure lore (midpoint gut-punch). FACT 11 + FACT 12. → feeds rite-tokens.
( 'undercroft-fog',
  'the room rebuilds wrong',
  array[
    'the room rebuilds wrong',
    'one fire and no one to tend it',
    'they did not depart they were kept'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the room rebuilds itself into something wrong. one fire is kept, eternal, attended by no one. they did not depart. they were kept. the rite is not a transaction.'
    )
  ),
  3, true, null ),

-- seventh-shrine — Sella's bearing → cold-hearth shrine; count six, then "a seventh".
-- side_quest entry but LORE payoff (FACT 10: the land can refuse). Earns Whisper
-- budget. GATES NOTHING ("the way goes on without it"). Kept DISTINCT from Iss.
( 'seventh-shrine',
  'the seventh not kept',
  array[
    'there was a seventh',
    'the last marker is not the last',
    'seven',
    '7',
    'the land kept six and refused the seventh',
    'a thing that can say no is not a wall'
  ],
  'side_quest',
  jsonb_build_object(
    'voice_key', 'oracleSideQuest',
    'set_flags', jsonb_build_object('seventh_found', true, 'whisper_budget_earned', true)
  ),
  3, true, null ),

-- ===========================================================================
-- MOVEMENT IV — The Reckoning (atonement; the biography)
-- ===========================================================================

-- orin-threshold — bring Orin's broken "i —" to D04's third-person completion.
-- Pure lore. FACT 6 (kept/left is a real binary) + FACT 12 foot-margin. → biography.
( 'orin-threshold',
  'observed warned left at the threshold',
  array[
    'i was not kept i was counted and the count was true and it was not enough',
    'named warned left at the threshold',
    'i was counted and it was not enough'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'orins line finishes in another hand. i was not kept. i was counted, and the count was true, and it was not enough. named, then warned, then left at the threshold. ask which of the two is still free to leave.'
    )
  ),
  4, true, null ),

-- haunting-biography — a keeper (or the now-cold Iss) ties a Movement-I haunting to a
-- named keeper's fate. Terminal lore. FACT 9 (the dread had a biography).
( 'haunting-biography',
  'the dread had a biography',
  array[
    'the dread had a biography',
    'the first hauntings were a keepers fate',
    'the dread was never random'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the first hauntings were not random. they were one keepers fate, re-enacted at your door. the dread had a biography.'
    )
  ),
  4, true, null ),

-- atonement-refrain — honor a previously-broken custom, then return to the keeper who
-- withheld its fragment. main_beat: the M4 turn (conduct is the lock, fragment the key).
( 'atonement-refrain',
  'the keepers turn',
  array[
    'the keepers turn',
    'conduct is the lock the fragment is the key',
    'honor the broken custom and return'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('atonement_made', true),
    'next_puzzle_key', 'rite-tokens',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'keeper_withheld_returned')
      )
    )
  ),
  4, true, null ),

-- ===========================================================================
-- MOVEMENT V — The Accepting
-- ===========================================================================

-- rite-tokens — lay one personal token in each of six slots + the named components.
-- main_beat. FACT 13 (the missing tool is YOU). → accepting-crouch / pressure-glyph-walk.
( 'rite-tokens',
  'bring the thing only you can give',
  array[
    'bring the thing only you can give',
    'deeps first heart unbroken light salt of the keepers',
    'a piece you cannot read your way out of'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('tokens_laid', true),
    'next_puzzle_key', 'accepting-crouch',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'unbroken_light',
      'priority', 10,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('slots', 6, 'lit', true)
      )
    )
  ),
  5, true, null ),

-- pressure-glyph-walk — the group walks the rune the altar floor names, footstep by
-- footstep. PROMOTED (INTEGRATION-V2 §2 graph / BUILD-MANIFEST §5): a genuine SECOND in-road
-- to the Accepting (side_quest → accepting-crouch), not a dead lore node — so the rite has
-- two doors (the true walk delivers them; this walk is the do-not-read-your-way-out door).
-- It still GATES NOTHING on the spine (the true walk reaches accepting-crouch independently);
-- this is an alternate approach, the cleanest expression of Mara's "do the thing" lesson.
( 'pressure-glyph-walk',
  'walk the rune',
  array[
    'walk the rune',
    'do not decode walk it',
    'trace the rune with your feet'
  ],
  'side_quest',
  jsonb_build_object(
    'voice_key', 'oracleSideQuest',
    'next_puzzle_key', 'accepting-crouch'
  ),
  5, true, null ),

-- accepting-crouch — everyone present bows as one (synchronized crouch), at the hour,
-- in the kept light. main_beat: collective, no chosen one. → record-receives.
-- TERMINAL RITE — DETECTED in-world ONLY (a synchronized group bow); NEVER typeable.
-- accepted_answers is a single OPAQUE, wordless, high-entropy token the plugin posts on
-- real detection — no human-readable phrase opens the climax (red-team B-5). The token
-- must live here so the resolver matches it; it is deliberately un-guessable + carries no
-- descriptive words (enforced by the no-leaked-sentinel self-test).
( 'accepting-crouch',
  'bow as one',
  array[
    'k7q2m9 x4r8p3 w1n6z5 t0j4h2 b8f1v7 c3d6s9'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('bowed_as_one', true),
    'next_puzzle_key', 'record-receives',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'unbroken_light',
      'priority', 8,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('open', true)
      )
    )
  ),
  5, true, null ),

-- record-receives — the world's response (no oracle gate normally; opaque sentinel).
-- main_beat: the hidden advancement fires and the world flips to KEPT. FACT 14. The
-- door to FACT 15 (felt, never stated by any node). active=false: STAGED for M5.
( 'record-receives',
  'the record receives you',
  array[
    'p2w8k4 m9x1r6 z5t3j7 h2b4f8 v1c6d3 s9q7n0'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('record_received', true, 'world_kept', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'priority', 30,
      'payload', jsonb_build_object(
        'step', 'advancement_toast',
        'step_payload', jsonb_build_object('key', 'observance:the_record_receives_you')
      )
    )
  ),
  5, false, null ),

-- ===========================================================================
-- WEB REALIZATION (WEB-MASTER / INTEGRATION-V2) — appended rows.
--
-- Every accepted_answers entry below is ALREADY NORMALIZED per ORACLE.md §2 (lower,
-- [a-z0-9 ] only, single-spaced, trimmed) — and contains NO apostrophe (seedcheck +
-- the X1 parser both key on quoted strings). Coordinate-bearing rows store the
-- DESTINATION WORD found on-site, never the signed coordinate (INV-14 / INV-COORD).
-- New voice keys referenced here are inserted verbatim by the TS-VOICE lane from the
-- LORE hand-off (see the RETURN); a missing voice key is silent at runtime, never a
-- build break (only thread_cards body keys are build-guarded). New active rows must be
-- classified in clue-specs NON_CIPHER_KEYS by the TS-FORGE lane (see the RETURN) or
-- specsCoverageSelfTest fails — they are non-cipher (lore/sentinel/observation) nodes.
-- ===========================================================================

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT I — the a1z26 literacy teaching-rung (the second, runes-free door to
-- rosetta_known). A number puzzle (a tick-stave): 1..26 → letters, no rune script.
-- Solving it sets rosetta_known WITHOUT the icon-ring metadata leap — killing the
-- "two doors that are one door" fairness lie (arg-craft F1) and un-orphaning a1z26.
-- The plaintext spells the verb the runes name. main_beat (same gate rosetta-ring opens),
-- read at first_marker_01 / stone_of_reckoning (the digit-glyph companion stone).
-- ───────────────────────────────────────────────────────────────────────────
( 'a1z26-tick-stave',
  'count the staves',
  array[
    'learn them as we learned them',
    'count the staves then read',
    'read them as we read them'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('rosetta_known', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'first_marker_01',
      'priority', 20,
      'payload', jsonb_build_object(
        'step', 'advancement_toast',
        'step_payload', jsonb_build_object('key', 'observance:the_ring_is_whole')
      )
    )
  ),
  1, true, null ),

-- base-docket-reread — the Hold-Book M4 face (A3 / WEB-MASTER §4). The down-count was
-- never a doom-clock; it is the muster of present hands still un-received. Pure lore,
-- de-slopped (slop A3, the chiasmus is CUT). GATED behind the catch by active=false
-- until iss_caught — the TS-SHOWRUN lane flips it active at the catch (M4 re-read).
( 'base-docket-reread',
  'the count was of the hands',
  array[
    'the count was never of the dark it was of the hands',
    'the muster is read the hands are almost in',
    'the down count is a muster of present hands',
    'not a doom clock a roll call'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'docketReread',
    'voice_args', jsonb_build_object(
      'fragment', 'the muster is read. the count was never of the dark. it was of the hands. the hands are almost in.'
    )
  ),
  4, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT II — the false law (A4 / FACT 7b) + the prophet wall (B2)
-- ───────────────────────────────────────────────────────────────────────────

-- forged-eighth — the Covering of the Hands (A4). A substitution row whose decoded
-- signature resolves to "to cover one's own" — a verb ABSENT from the founders' ring,
-- the seam of the forgery showing. teaches_custom NULL (it is fiction, not a way; INV-17).
-- thread_key 'surface'. The anonymous lie credits no "me" (slop B4). Counted in the
-- founders' RING, never fall-order (the two sixes, §0.3). dead_end: a diligent group can
-- "obey" it and nothing pays — the proof of the lie is the reliable absence of a toll.
( 'forged-eighth',
  'the covering of the hands',
  array[
    'the eighth is the covering of the hands',
    'cover and be counted clean',
    'to cover ones own',
    'the founders set the ways and did not finish the count'
  ],
  'dead_end',
  -- 'known' kind — a true reading of a real carving that opens nothing (it is a forgery;
  -- the land never measures it). The M4 record correction (archiveEighthCorrection) names
  -- it added-not-found; until then the Watcher only flatly declines to enforce it.
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'known')),
  2, true, null ),

-- prophet-wall-comfort — Iss with a pulpit (B2). A WIDE, not tall set of warm promises,
-- each a true-but-empty substitution solve that opens nothing. dead_end 'prophet' kind.
-- Independent rung (no next_puzzle_key — never a countable ladder). Re-reads cold at the
-- catch (the hidden columnar name below is Iss's). Lives in Iss's field, M2.
( 'prophet-wall-comfort',
  'keep the ten and you are inside it',
  array[
    'keep the ten and you are inside it',
    'the watching stays out in the cold and counts and cannot touch you',
    'be easy the wall keeps the watching out'
  ],
  'dead_end',
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'prophet')),
  2, true, null ),

-- prophet-wall-name — the SECOND active false-coordinate herring (§2): the prophet wall's
-- columnar acrostic. Read down the first letters of the warm rungs and it spells the word
-- the corpus keeps for Iss — the wall was his. dead_end (a true name, keeps no door). It
-- re-reads at the catch as "read who carved it, after" paying off (#19) — the author was Iss.
-- max_attempts:6 (a short columnar answer, capped against brute force).
( 'prophet-wall-name',
  'read who carved it after',
  -- DISAMBIGUATED (OVERHAUL §5): the bare `the one who turned away` is owned solely by
  -- stone-iss-wall (the catch). This row keeps its own distinct readings so the prophet
  -- wall still resolves ("iss carved the wall" / the full first-marks-down phrase) without
  -- a simultaneous M2 collision that would let this dead_end shadow the catch.
  array[
    'iss carved the wall',
    'read the first marks down the one who turned away'
  ],
  'dead_end',
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'prophet')),
  2, true, 6 ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT III — the Seventh restore/erase spine (A1) + a fork (A11 Fork B)
-- ───────────────────────────────────────────────────────────────────────────

-- seventh-unwriting — chamber 2 of the seventh-shrine (the hearth-DEEP, the_unwriting).
-- RAIL-FENCE (rails=6, counted in-world on the wall) REUSING Brann's taught rail-fence
-- literacy (P1-5) — it does not teach the cipher cold. Solving it NAMES the Seventh →
-- sets seventh_named (FACT 10b). Gated by active=false until the deep opens (post-iss_caught
-- + seventh_suspected); the SeventhChoiceListener / TS-SHOWRUN flips it. main_beat.
( 'seventh-unwriting',
  'the seal is a name',
  array[
    'below the cold hearth the deep is sealed the seal is a name',
    'the unwriting keeps the name it cut out',
    'the seventh kept all the ways and was cast out'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('seventh_named', true),
    'next_puzzle_key', 'seventh-choice',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'the_unwriting',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'seventh_name_unsealed')
      )
    )
  ),
  3, false, null ),

-- seventh-cause — the cause-fragment (D-new the-fire-they-let-out) correlated with D11.
-- Pure lore (FACT 10b): the land refused a keeper who broke NOTHING. Earns Whisper budget.
-- Gated with seventh-unwriting (active=false until the deep opens). GATES NOTHING.
( 'seventh-cause',
  'refused the one who broke nothing',
  array[
    'the land refused a keeper who broke nothing',
    'kept all the ways and cast out anyway',
    'a thing that can say no is not a wall'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the seventh kept every way and was not kept. the fire they let out was never theirs to lose. the land can refuse. whether that is mercy the record does not say.'
    )
  ),
  3, false, null ),

-- seventh-choice — the restore-OR-erase choice (A1 / FACT 10b). One flag, seventh_choice
-- ∈ {restore | erase}, read by the M5 composer for ONE tinted clause + one persistent
-- block-state. The deposit (restore) is ALSO the INHERITORS codicil (ending_codicil) — ONE
-- act, ONE flag-origin (no separate dark_shrine). DETECTED IN-WORLD ONLY (the SeventhChoice
-- Listener's rite at the_unwriting): the two opaque, wordless tokens below are posted by the
-- plugin on real detection — restore vs erase — never human-typeable (no-leaked-sentinel).
-- The resolver's Seventh-choice sentinel branch sets seventh_choice + ending_codicil from
-- which token matched (TS-SHOWRUN owns the branch). main_beat; gated active=false until
-- seventh_named. GATES NOTHING on the spine (colors the ending only, INV-12-style).
( 'seventh-choice',
  'restore or erase the seventh',
  array[
    'r7n4k2 m1x8p5 w3j6h9',
    'e5t0b7 c2d4s8 v6f1z3'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'the_unwriting',
      'priority', 11,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'seventh_choice_marked')
      )
    )
  ),
  3, false, null ),

-- fork-light (Fork B, A11) — the First Light fork at the Undercroft. A M3 puzzle CHOICE
-- (two plaintexts): draw the M5 token from the eternal flame (light_kept) or bank it
-- (light_taken → the room stays dark for the arc). set_flags ONLY — colors the M5 close,
-- GATES NOTHING (seedcheck: no spine puzzle requires a fork flag). Diegetically irreversible
-- (the leaf is permanent), but never scored along the way (INV-12). The two readings are the
-- two accepted answers; the resolver sets the matching leaf. side_quest (off-spine color).
( 'fork-light',
  'draw the light or bank it',
  array[
    'draw the light up the stair',
    'leave the flame banked and the room dark',
    'carry the kept light',
    'bank the flame'
  ],
  'side_quest',
  jsonb_build_object(
    'voice_key', 'oracleSideQuest',
    'set_flags', jsonb_build_object('light_kept', true)
  ),
  3, true, null ),

-- fork-name (Fork C, A11; P2 / cuttable on blurt) — the Spoken Name fork at M4: carve
-- Iss's name vs withhold it. set_flags ONLY (name_unspoken default leaf shown here; the
-- alternate name_spoken leaf is the carve act, detected in-world). Colors the M5 close,
-- GATES NOTHING. unspoken-custom-adjacent — withholding is the kept reading. side_quest.
( 'fork-name',
  'speak the name or keep it',
  array[
    'keep his name unspoken',
    'leave the name uncarved',
    'the unspoken is kept'
  ],
  'side_quest',
  jsonb_build_object(
    'voice_key', 'oracleSideQuest',
    'set_flags', jsonb_build_object('name_unspoken', true)
  ),
  4, true, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT IV — the single Iss chain hinges: bound word → three-hands gate →
-- true coordinate → the walk (coherence P1-1, the one sequence)
-- ───────────────────────────────────────────────────────────────────────────

-- bound-word — the Iss Vigenère plaintext IS the coop-gate's need (the convergence word).
-- A SECOND in-road exists via another keeper stone (§2, in-road B) — both normalize to the
-- same bound word. next_clue → m4-three-hands (the gate that consumes it). Earnable M3,
-- but the Threshold stays sealed (looks like another lore solve). Gated active=false until
-- iss_caught (the catch re-carves Iss's stone to yield it). max_attempts:6 (short answer).
( 'bound-word',
  'the word the catch yields',
  array[
    'the one who turned away',
    'turned away',
    'the bound word is his name'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'm4-three-hands',
    'set_flags', jsonb_build_object('bound_word_known', true)
  ),
  4, false, 6 ),

-- m4-three-hands — THE cross-surface co-op gate (A6). THREE distinct ACTS, not three
-- distinct people (active-only): foot on the plate + a carve + a Discord post inside the
-- same ~20s window. The AND-join lives ONCE in resolve.ts (applyOutcome of this puzzle);
-- the CoopPlateListener posts an opaque conjunction token when its leg fires. The token
-- below is what the AND-join posts on a CLEARED gate (all three legs in-window) — opaque,
-- wordless, plugin-only (no-leaked-sentinel). Clearing opens the Threshold (threshold_open),
-- whose carving yields the TRUE coordinate (NOT yielded here — sequenced, §0.4). main_beat.
( 'm4-three-hands',
  'three hands at once',
  array[
    'h3n8k1 q5m2x7 w9j4p6 t1b6f0 c8d3s5 v2z7r4'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleThreeHands',
    'set_flags', jsonb_build_object('threshold_open', true),
    'next_puzzle_key', 'threshold-coordinate',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'coop_plate',
      'priority', 14,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  4, false, null ),

-- threshold-coordinate — the Threshold carving yields the TRUE coordinate AFTER the gate
-- (the catch re-carves Iss's stone; the gate opens the Threshold; the Threshold carving is
-- the coordinate). INV-14: the decoded value is a NAVIGATION POINTER; the answer the player
-- types is the clean DESTINATION WORD found on-site, never the signed coordinate. next_clue
-- → the true walk. Gated active=false until threshold_open. The destination word gate is on
-- the answer-sign AT the destination (coordReCarve / voice.dest.coordFraming).
( 'threshold-coordinate',
  'the true road opens',
  array[
    'follow the threshold mark to where it points',
    'the true coordinate is a road not an answer',
    'walk where the threshold sends you'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'true-walk-arrive',
    'set_flags', jsonb_build_object('true_coord_known', true)
  ),
  4, false, null ),

-- true-walk-arrive — the TRUE walk endpoint (A7). The destination WORD (carved on leaves
-- placed at the on-site tableau) is the answer, gated to on-site presence — NOT the coord.
-- main_beat → the Accepting on-ramp (rite-tokens). The PrivateSound/ParticleBeat per-presence
-- arrival fires in-world; the word here is what a present keeper reads off the destination
-- carving. Gated active=false until true_coord_known. coldHearth.find / threshold.arrive voice.
( 'true-walk-arrive',
  'the road kept its word',
  array[
    'kept here before you',
    'the road kept its word',
    'we were already filed here'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('true_destination_reached', true),
    'next_puzzle_key', 'rite-tokens',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'the_threshold',
      'priority', 13,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'destination_leaves_read')
      )
    )
  ),
  4, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT II→IV — name-where-never-been (A8 / FACT 16): the place-filing re-read
-- ───────────────────────────────────────────────────────────────────────────

-- name-where — the carves were never prediction; the group is ALREADY filed by place
-- (FACT 16, child of FACT 1). ACTIVE-only subjects ROTATE across all active players (a
-- chorus, never the divergence extremes; INV-16). dead_end 'place' kind — a true reading
-- that opens nothing (it recolors what you have). The carve beats are produced by the
-- name-where-never-been.ts selector (TS-SHOWRUN), gated on proof-of-absence; this row is
-- the player's re-read acknowledgement. The dramatic comma-fragment is capped (slop B1).
( 'name-where',
  'your name where you have never been',
  array[
    'the record files the living by place not only by name',
    'against each name a ground',
    'before you was never about strangers'
  ],
  'dead_end',
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'place')),
  2, true, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT IV — UNKEPT meta-acrostic (B1) + the Record website (A13)
-- ───────────────────────────────────────────────────────────────────────────

-- meta-unkept — the day-one meta (B1). The six maker's-mark glyphs (one per stone),
-- read in FALL-ORDER (Vaun, Mara, Sella, Orin, Brann, Iss), carry UNKEPT — the word each
-- keeper failed to keep. The naive "first letter of each plaintext" form is CUT (X1 guard):
-- the acrostic lives in the carved FRAMING glyphs, never the bound run. NON-cipher (plain
-- lore, NON_CIPHER_KEYS). GATES NOTHING (pure re-read). active=false: STAGED for M4 — the
-- cold Iss/Keeper states the fall-order key at the catch, then the group assembles it.
-- The order-key clue names fall-order; the glyphs fail in ring-order (self-correcting, §0.3).
( 'meta-unkept',
  'the word read in fall order',
  array[
    'unkept'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleMetaUnkept',
    'voice_args', jsonb_build_object(
      'fragment', 'six marks, one to a stone. read them in the order they fell. the word is the one each did not keep.'
    )
  ),
  -- max_attempts: a single short dictionary word ("unkept") is brute-forceable; cap it
  -- (the global token bucket still applies; a real solver who read the fall-order key on the
  -- six marks lands it first try). Caps at 8 → in-voice withholding, never a hint.
  4, false, 8 ),

-- record-url — the Record website (A13). The founder line "the record is kept in more than
-- one place" decodes to a URL path off-world (the click, #11). Pure lore, GATES NOTHING.
-- The page is static-per-build, noindex, reads the spoiler-free projection only. The decoded
-- PATH is the answer (a clean token), not a coordinate. recordElsewhere voice. Active M2 (it
-- un-redacts entries in lockstep with stones actually read; the Iss card carries the stego).
( 'record-url',
  'the record is kept elsewhere',
  array[
    'the record keeps',
    'the record is kept in more than one place',
    'the record is kept in more than one place against the loss of the first'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'recordElsewhere',
    'voice_args', jsonb_build_object(
      'fragment', 'the record is kept in more than one place, against the loss of the first. the path is the record keeps.'
    )
  ),
  2, true, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- MOVEMENT II→V — dynamic-diegetic-difficulty plant (A10 / FACT 2b)
-- ───────────────────────────────────────────────────────────────────────────

-- difficulty-mara — Mara's bookCipher fragment (FACT 2b): the land's grip is not fixed.
-- The planted line does NOT name "mercy" or resolve its own meaning (slop A2). Pure lore,
-- planted M1-legible, re-quoted by the M5 composer ("closer count of the quick"). The
-- difficulty engine (reckoning.ts, TS-SHOWRUN) is the mechanism; this is its corpus plant.
-- GATES NOTHING. Mara's register: referential/deferred (she cites; the others witness).
( 'difficulty-mara',
  'the record keeps a closer count of the quick',
  array[
    'the record keeps a closer count of the quick',
    'the land closes on those who run ahead and opens for those who stumble',
    'i will not say what i call it now'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'i read that the record keeps a closer count of the quick. i called that a cruelty for a winter. i do not call it that now, and i will not say what i call it.'
    )
  ),
  2, true, null ),

-- ===========================================================================
-- PRIOR-SESSION BACKLOG ROWS (BUILD-MANIFEST §D / §5 back-half). These carry
-- accepted_answers (so they live HERE, where seedcheck + specsCoverageSelfTest
-- read), and realize the engine spine the new-ARG threads ride: the second
-- Rosetta (digit literacy), the sixth keeper-stone expedition (Brann), and the
-- Liar engine's offline AUTO duplicate. Every answer is pre-normalized (lower,
-- [a-z0-9 ] only, single-spaced, trimmed, NO apostrophe) per ORACLE.md §2.
--
-- COVERAGE NOTE (specsCoverageSelfTest, clue-specs.ts ~L477): every ACTIVE row
-- must be a registered CLUE_SPECS cipher OR in NON_CIPHER_KEYS. The two ACTIVE
-- rows below (reckoning-rosetta, base-docket-reread-auto) are non-cipher
-- observation/lore nodes — the TS-FORGE lane must add them to NON_CIPHER_KEYS
-- (listed in the RETURN) or the coverage self-test fails. The STAGED row
-- (stone-brann-cipher, active=false) is exempt until it is both activated AND
-- given a real railFence CLUE_SPECS entry by TS-FORGE.
-- ===========================================================================

-- ───────────────────────────────────────────────────────────────────────────
-- D3 — the SECOND Rosetta: the Stone of Reckoning (digit literacy). The rune-ring
-- (rosetta-ring) + the a1z26 rung teach the LETTERS; this teaches the DIGITS — the
-- separate counting-glyph family (the `.`/`-`/`,` marks) so a carved coordinate or a
-- page-number reads as a number. Sets reckoning_known (the soft staging flag the coord
-- rows read: BUILD-MANIFEST §D3 — "coord rows stay inactive until stone_of_reckoning is
-- placed"). main_beat (same literacy register as the ring). GATES NOTHING on the spine;
-- it makes coordinates into PLACES (INV-14 in-world fairness). Read at stone_of_reckoning.
( 'reckoning-rosetta',
  'count the marks as we counted them',
  array[
    'count the marks as we counted them',
    'the low bar is a minus the double tick is a break',
    'read the digits as digits not as words'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('reckoning_known', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'stone_of_reckoning',
      'priority', 20,
      'payload', jsonb_build_object(
        'step', 'advancement_toast',
        'advancement', 'observance:the_count_is_yours',
        'fallback_title', 'the record',
        'fallback_subtitle', 'the record notes you can count it now'
      )
    )
  ),
  1, true, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- D2 — the SIXTH keeper-stone expedition: Brann, read-by-time (the genuinely-unbuilt
-- modality, backlog-keeper-stone-expeditions §1.4). Today stone-brann ships as flat lore;
-- this is its real cipher node — railFence (rails = the fire-count Brann names in D08),
-- the verb is read-by-time (the carving rakes visible only by the lit beacon-glow after
-- dark). The bound plaintext "count the fires before you sleep" round-trips under
-- specsSelfTest ONLY ONCE TS-FORGE adds the CLUE_SPECS railFence entry + removes stone-brann
-- from NON_CIPHER_KEYS (the cross-owner dependency in the RETURN). Until then this row is
-- STAGED (active=false) so it neither (a) trips specsCoverageSelfTest as an unclassified
-- active row, nor (b) presents a cipher the forge cannot yet bind. next_clue → the descent
-- (a second in-road to undercroft-descent, the web rule). The rail-key is day-fair: the
-- fire-count is also countable in daylight (backlog §R-3), only the READING is night-gated.
( 'stone-brann-cipher',
  'count the fires before you sleep',
  array[
    'count the fires before you sleep',
    'nine lit one out i relit it',
    'the dark hours are kept by the last light'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'undercroft-descent',
    'set_flags', jsonb_build_object('brann_read', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'stone_brann',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'site_id', 'stone_brann',
        'op', 'relabel',
        'text', 'nine lit one out i relit it i will count again before i sleep'
      )
    )
  ),
  2, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- D4 — the Liar engine OFFLINE/AUTO duplicate (backlog-liar-engine §0.2, MASTER-PLAN
-- P1.11 risk §5.1). base-docket-reread is staged active=false and the showrunner flips it
-- on at iss_caught; but if the showrunner is asleep the M4 re-read must STILL happen. This
-- is the flag-gated authored duplicate: it ships active=true and is GATED BY requires_flags
-- {iss_caught:true} (the deterministic activation lane, NOT the showrunner) — getOpenPuzzles
-- treats it as open only once iss_caught is truthy in arc_state. Same lore payoff, same
-- de-slopped fragment (slop A3, the chiasmus CUT). The two rows are idempotent twins: solving
-- either records its own solve; both speak docketReread. This removes the showrunner SPOF on
-- the signature re-read (the requires_flags column is the SQL-migration lane's 0006 add — the
-- cross-owner dependency in the RETURN; getOpenPuzzles must AND-test requires_flags).
( 'base-docket-reread-auto',
  'the count was of the hands offline',
  array[
    'the count was never of the dark it was of the hands',
    'the muster is read the hands are almost in',
    'the down count is a muster of present hands',
    'not a doom clock a roll call'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'docketReread',
    'voice_args', jsonb_build_object(
      'fragment', 'the muster is read. the count was never of the dark. it was of the hands. the hands are almost in.'
    )
  ),
  -- requires_flags {iss_caught:true} is set on the COLUMN (not the payload) in
  -- metapuzzle_seed.sql (the 0006 puzzles.requires_flags activation lane) — the row ships
  -- active=true but getOpenPuzzles holds it closed until iss_caught is truthy in arc_state.
  4, true, null )

on conflict (puzzle_key) do update set
  title            = excluded.title,
  accepted_answers = excluded.accepted_answers,
  outcome_type     = excluded.outcome_type,
  outcome_payload  = excluded.outcome_payload,
  movement         = excluded.movement,
  active           = excluded.active,
  max_attempts     = excluded.max_attempts;

commit;


-- ============================================================
-- FILE: seeds/seventh_seed.sql
-- ============================================================

-- The Observance — seventh_seed.sql
-- The Seventh restore/erase spine + the false-law thread, as BREADTH side-quest rows
-- (the off-spine longevity layer; LONGEVITY.md §2 / 0005 public.side_quests). These pay
-- lore/atmosphere/time and GATE NOTHING (gates_progress defaults false + CHECK). The
-- spine puzzle nodes for these threads live in puzzles_seed.sql (seventh-unwriting,
-- seventh-cause, seventh-choice, forged-eighth, prophet-wall-*); this file seeds the
-- TRAVEL/breadth ledger rows the Recovery Archive clusters their cards under, mirroring
-- the existing side_quests.sql shape.
--
-- WEB-MASTER §0.4 anchor law: the dead-shrine (the_cold_hearth SURFACE = Iss's grave,
-- the warm-lie endpoint, already seeded as dest-dead-shrine in side_quests.sql) and the
-- seventh-shrine (the hearth-DEEP below it, the_unwriting) are DISTINCT PLACES, temporally
-- layered — the deep opens only post-iss_caught + seventh_named. The INHERITORS/dark_shrine
-- deposit is MERGED into the Seventh restore act (one seventh_choice flag); there is NO
-- separate dark_shrine site (INTEGRATION-V2 A1 / WEB-MASTER §3). So this file seeds the
-- DEEP expedition only, never a second dead-shrine.
--
-- entry_puzzle_key wiring (additive over the spine rows): two of these breadth rows DO name
-- an entry node — seventh-cause (the deep's lore payoff) and forged-eighth (the false law's
-- found ordinance) — so the Recovery Archive can route their card under the right thread when
-- the spine node resolves. The breadth rows still gate nothing; the entry is a clustering
-- pointer, not a lock (0005 side_quests.entry_puzzle_key is a plain pointer, no FK).
--
-- Additive + idempotent (ON CONFLICT (quest_key) DO NOTHING). Run AFTER 0005_threads.sql
-- and AFTER puzzles_seed.sql (so the entry_puzzle_key pointers name real rows), as
-- service_role (RLS bypass — spoiler tables). Re-running is safe.

begin;

insert into public.side_quests
  (quest_key, thread_key, entry_puzzle_key, reward, tier, est_minutes)
values
  -- The seventh-shrine DEEP (the_unwriting) — the hearth-deep beneath the dead-shrine,
  -- sealed until the catch. KEYED (rail-fence rails=6, reusing Brann's taught literacy):
  -- naming the Seventh (FACT 10b) + the restore/erase choice. Distinct from dest-dead-shrine.
  ('dest-unwriting-deep',  'who',      'seventh-cause',
   'KEYED (rail-fence, Brann''s literacy reused): the Seventh named (FACT10b); the restore/erase choice; Whisper budget; card under who',
   'keyed',   28),

  -- The fire-they-let-out — the cause-fragment (D-new) correlated with D11. The land
  -- refused a keeper who broke NOTHING. Pays the Seventh''s why; verified card under who.
  ('dest-fire-let-out',    'who',      'seventh-cause',
   'the cause-fragment (D-new x D11): the land refused one who broke nothing; FACT10b; verified card under who',
   'rumored', 15),

  -- The forged eighth ordinance — the Covering of the Hands (FACT 7b), found as one more
  -- law among the true seven. A diligent group obeys it and NOTHING pays (the proof of the
  -- lie). SIDE-TRACK: real document, hollow law; contradicted at the M4 record correction.
  ('dest-covering-law',    'surface',  'forged-eighth',
   'SIDE-TRACK: the forged eighth (FACT7b); obey it and nothing pays; the_-prefixed nothing (fiction, INV-17); contradicted card under surface',
   'rumored', 14),

  -- The prophet''s wall — Iss with a pulpit (B2). A WIDE set of warm promises that each
  -- decode true and open nothing; the hidden columnar name is his own. DEAD LEAD by design.
  ('dest-prophet-wall',    'happened', null,
   'DEAD LEAD: Iss''s pulpit; warm promises that open nothing; hidden columnar name = Iss; contradicted card under happened',
   'rumored', 16),

  -- The true Threshold walk — the coordinate the Threshold carving yields AFTER the gate
  -- (the single Iss chain). The destination WORD is the answer (INV-14), not the coord.
  -- KEYED (on-site presence): the Accepting on-ramp. Distinct from the false dead-shrine walk.
  ('dest-true-threshold',  'place',    'threshold-coordinate',
   'KEYED (on-site presence): the TRUE walk (INV-14 destination word, not the coord); the Accepting on-ramp; verified card under place',
   'keyed',   22)
on conflict (quest_key) do nothing;

commit;


-- ============================================================
-- FILE: seeds/thread_tags.sql
-- ============================================================

-- The Observance — thread_tags.sql
-- Tags each of the 24 seeded puzzle nodes to a reconstruction thread (which question its PAYOFF
-- advances) and, where the node teaches/hinges on a way, a teaches_custom (a canon CUSTOM_KEYS
-- the_-prefixed key). Content + rationale: design/content/thread-tagging.md (deliberately lopsided
-- by content, not a 1:1 pattern — the_bow is taught 5×, two ways teach 0 nodes here, 10 nodes NULL).
--
-- Additive + idempotent (each update is keyed + sets absolute values). Apply AFTER 0005_threads.sql
-- (adds the columns + seeds the five threads) and puzzles_seed.sql (inserts the 24 rows). Run as
-- service_role (RLS bypass). The FK puzzles.thread_key→threads hard-fails any drifted thread; the
-- threadTagSelfTest build guard checks every non-null teaches_custom ∈ CUSTOM_KEYS (no FK on it).

begin;

-- MOVEMENT I
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'm1-record-opens';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'm1-named-habit';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'rosetta-ring';

-- MOVEMENT II — keeper stones
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'stone-vaun';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_kept_light'  where puzzle_key = 'stone-mara';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'stone-sella';
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_bow'         where puzzle_key = 'stone-orin';
update public.puzzles set thread_key = 'surface',  teaches_custom = 'the_dark_hours'  where puzzle_key = 'stone-brann';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'stone-iss-wall';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'iss-warm';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'm2-rhyme';

-- MOVEMENT II→IV — the liar thread
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'iss-dead-shrine';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'iss-doubt';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'no-wall-catch';

-- MOVEMENT III — undercroft + seventh
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_kept_light'  where puzzle_key = 'undercroft-descent';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'undercroft-fog';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'seventh-shrine';

-- MOVEMENT IV — the reckoning
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_bow'         where puzzle_key = 'orin-threshold';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'haunting-biography';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_offering'    where puzzle_key = 'atonement-refrain';

-- MOVEMENT V — the accepting
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'rite-tokens';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'pressure-glyph-walk';
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_bow'         where puzzle_key = 'accepting-crouch';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'record-receives';

-- ===========================================================================
-- WEB REALIZATION (WEB-MASTER / INTEGRATION-V2) — tags for the appended rows.
-- thread_key ∈ {who,place,happened,surface,human} (FK + threadTagSelfTest); every NON-NULL
-- teaches_custom ∈ CUSTOM_KEYS (the_-prefixed; threadTagSelfTest, no FK). Fiction/observation/
-- coordinate rows teach NO way (null). The forged eighth teaches null BY LAW (INV-17: it is a
-- forgery, not a CUSTOM_KEYS member).
-- ===========================================================================

-- MOVEMENT I — literacy second door + the Hold-Book M4 re-read
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'a1z26-tick-stave';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'base-docket-reread';

-- MOVEMENT II — the forged law + the prophet wall
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'forged-eighth';
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'prophet-wall-comfort';
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'prophet-wall-name';

-- MOVEMENT II→IV — name-where (FACT 16), record website, difficulty plant
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'name-where';
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'record-url';
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'difficulty-mara';

-- MOVEMENT III — the Seventh restore/erase spine + Fork B/C
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'seventh-unwriting';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'seventh-cause';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'seventh-choice';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_kept_light'  where puzzle_key = 'fork-light';
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_unspoken'    where puzzle_key = 'fork-name';

-- MOVEMENT IV — the single Iss chain hinges + UNKEPT meta
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'bound-word';
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'm4-three-hands';
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'threshold-coordinate';
update public.puzzles set thread_key = 'place',    teaches_custom = 'the_bow'         where puzzle_key = 'true-walk-arrive';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'meta-unkept';

-- ===========================================================================
-- PRIOR-SESSION BACKLOG ROWS (BUILD-MANIFEST §D) — tags for the three rows appended
-- to puzzles_seed.sql. thread_key ∈ THREADS (FK + threadTagSelfTest); teaches_custom ∈
-- CUSTOM_KEYS the_-prefixed (threadTagSelfTest, no FK) or null. The Reckoning Rosetta
-- teaches the DIGIT literacy (no way/custom → null, like rosetta-ring's place tag); Brann's
-- sixth stone teaches the_dark_hours (his way, matching the flat stone-brann tag it replaces);
-- the offline docket twin is the same human re-read as base-docket-reread.
-- ===========================================================================
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'reckoning-rosetta';
update public.puzzles set thread_key = 'surface',  teaches_custom = 'the_dark_hours'  where puzzle_key = 'stone-brann-cipher';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'base-docket-reread-auto';

commit;


-- ============================================================
-- FILE: seeds/thread_cards.sql
-- ============================================================

-- The Observance — THREAD_CARDS SEED (the Recovery Archive — design/content/thread-archive.md
-- + the four gather-event cards — design/content/gather-events.md §6, realized in the 0005 schema)
-- discord/supabase/seeds/thread_cards.sql
--
-- 42 place-anchored CARDS that cluster under the five reconstruction threads
-- (who / place / happened / surface / human). 37 come from the Recovery Archive
-- (thread-archive.md) and 5 from the group gather-events (gather-events.md §6).
-- Each card is one FIND in the world — a torn record, a journal leaf, a carved stone,
-- a thing seen on a summons night — its in-character body fetched at render time by
-- body_voice_key from voice.archive.ts (never stored as English in the row; INV-1).
--
-- Column contract (0005_threads.sql §2 public.thread_cards):
--   card_key, thread_key, title, body_voice_key, anchor_site_id, card_kind,
--   references_card_key text[], revealed_by_solve, alt_text_condition, sort_order.
--
-- INVARIANTS HELD (asserted against the live files at author time):
--   * every thread_key ∈ public.threads {who,place,happened,surface,human} (FK, 0005 §1).
--   * every anchor_site_id is an ENABLED site in plugin/.../sites.yml (free-text in 0005,
--     but kept real so a beat can target it): stone_of_reckoning, stone_vaun, stone_mara,
--     stone_orin, stone_brann, stone_iss, the_far_water, the_threshold, rune_rosetta,
--     unbroken_light, offering_cairn_01, the_cold_hearth, kept_light_home_01,
--     first_report_lectern_01. (Disabled keeper_stone_01/02 are NOT used.)
--   * every revealed_by_solve is a real puzzle_key in puzzles_seed.sql; NULL where the
--     card is found-on-descent / read-on-the-surface / event-revealed (not solve-gated).
--   * every references_card_key entry resolves to a card_key seeded in THIS file.
--   * card_kind ∈ {rumor,explore,verified,contradicted} (0005 CHECK). The five gather
--     cards are 'verified' (written by the director when the gather flag flips).
--
-- IDEMPOTENT: ON CONFLICT (card_key) DO NOTHING — re-running is safe; this file is the
-- canonical authored content and does not overwrite live per-row edits.
--
-- Run AFTER 0005_threads.sql, as service_role (RLS bypass — spoiler table).

begin;

insert into public.thread_cards
  (card_key, thread_key, title, body_voice_key, anchor_site_id, card_kind,
   references_card_key, revealed_by_solve, alt_text_condition, sort_order)
values

  -- ========================================================================
  -- THREAD: who — who they were  (amber)
  -- ========================================================================
  ( 'who-deep-market', 'who', 'the deep market', 'cardWhoDeepMarket',
    'stone_of_reckoning', 'explore',
    '{}', null, null, 10 ),

  ( 'who-vaun-counted', 'who', 'the founder who counted', 'cardWhoVaunCounted',
    'stone_vaun', 'explore',
    array['who-mara-read','human-offering-ledger'], 'stone-vaun', null, 20 ),

  ( 'who-mara-read', 'who', 'the lampwright who read', 'cardWhoMaraRead',
    'stone_mara', 'explore',
    array['who-vaun-counted','who-sella-token'], 'stone-mara', null, 30 ),

  ( 'who-sella-token', 'who', 'the under-warden', 'cardWhoSellaToken',
    'the_far_water', 'explore',
    array['who-mara-read','surface-seventh-marker'], 'stone-sella', null, 40 ),

  ( 'who-orin-mason', 'who', 'the mason who would not bow', 'cardWhoOrinMason',
    'stone_orin', 'explore',
    array['who-brann-watch','happened-orin-sealed'], 'stone-orin', null, 50 ),

  ( 'who-brann-watch', 'who', 'the watchman who would not sleep', 'cardWhoBrannWatch',
    'stone_brann', 'explore',
    array['who-orin-mason','surface-watcher-counts'], 'stone-brann', null, 60 ),

  ( 'who-iss-friend', 'who', 'the best of the young ones', 'cardWhoIssFriend',
    'stone_iss', 'explore',
    array['happened-the-doubt','surface-iss-was-right'], 'stone-iss-wall', null, 70 ),

  -- ========================================================================
  -- THREAD: place — what this place was  (green)
  -- ========================================================================
  ( 'place-came-down', 'place', 'we came down', 'cardPlaceCameDown',
    'the_threshold', 'explore',
    array['place-deeper-wrong'], null, null, 10 ),

  ( 'place-seven-ways', 'place', 'the order is seven', 'cardPlaceSevenWays',
    'rune_rosetta', 'explore',
    array['place-came-down','surface-sixth-blank'], 'rosetta-ring', null, 20 ),

  ( 'place-deeper-wrong', 'place', 'the marks down the stair', 'cardPlaceDeeperWrong',
    'unbroken_light', 'explore',
    array['place-came-down','human-galleries-unruled'], 'undercroft-descent', null, 30 ),

  ( 'place-deep-line', 'place', 'the deep line', 'cardPlaceDeepLine',
    'stone_iss', 'explore',
    array['happened-the-break','surface-iss-was-right'], 'stone-iss-wall', null, 40 ),

  ( 'place-cairn', 'place', 'the offering-cairn', 'cardPlaceCairn',
    'offering_cairn_01', 'explore',
    array['who-vaun-counted','human-offering-ledger'], null, null, 50 ),

  ( 'place-undercroft-sealed', 'place', 'the sealed undercroft', 'cardPlaceUndercroftSealed',
    'unbroken_light', 'explore',
    array['happened-orin-sealed','human-galleries-unruled'], 'undercroft-fog', null, 60 ),

  -- ========================================================================
  -- THREAD: happened — what happened  (red)
  -- ========================================================================
  ( 'happened-the-doubt', 'happened', 'the doubt', 'cardHappenedTheDoubt',
    'stone_iss', 'explore',
    array['who-iss-friend','surface-iss-was-right'], 'stone-iss-wall', null, 10 ),

  -- ← CONTRADICTED BY happened-no-wall (the warm lie the player is steered to trust)
  ( 'happened-ways-are-wall', 'happened', 'the ways are a wall', 'cardHappenedWaysAreWall',
    'stone_iss', 'explore',
    array['happened-no-wall','surface-iss-was-right'], 'iss-warm', null, 20 ),

  -- ← CONTRADICTS happened-ways-are-wall (the catch that overturns the doctrine)
  -- THE ISS-SEAM: this catch also re-opens surface-seventh-marker (cited here) — overturning
  -- the wall-lie overturns "cast out for nothing" (the-seventh-below.md REWRITE SPEC).
  ( 'happened-no-wall', 'happened', 'no wall was ever built here', 'cardHappenedNoWall',
    'stone_iss', 'explore',
    array['happened-ways-are-wall','surface-iss-was-right','human-they-were-kept','surface-seventh-marker'], 'no-wall-catch', null, 30 ),

  -- the Break hub: points at three accounts that cannot all be true
  ( 'happened-the-break', 'happened', 'the break', 'cardHappenedTheBreak',
    'stone_brann', 'explore',
    array['happened-break-accident','happened-break-betrayal','happened-break-mercy'], 'stone-brann', null, 40 ),

  -- ← CONTRADICTS the other two Break accounts
  ( 'happened-break-accident', 'happened', 'an accident', 'cardHappenedBreakAccident',
    'stone_brann', 'explore',
    array['happened-break-betrayal','happened-break-mercy'], 'stone-brann', null, 50 ),

  -- ← CONTRADICTS the other two Break accounts (rumor: reached on the trusting route, then reframed)
  ( 'happened-break-betrayal', 'happened', 'a betrayal', 'cardHappenedBreakBetrayal',
    'the_cold_hearth', 'rumor',
    array['happened-break-accident','happened-break-mercy'], 'iss-warm', null, 60 ),

  -- ← CONTRADICTS the other two Break accounts
  ( 'happened-break-mercy', 'happened', 'a mercy', 'cardHappenedBreakMercy',
    'the_far_water', 'explore',
    array['happened-break-accident','happened-break-betrayal'], 'seventh-shrine', null, 70 ),

  ( 'happened-orin-sealed', 'happened', 'sealed from the inside', 'cardHappenedOrinSealed',
    'unbroken_light', 'explore',
    array['who-orin-mason','place-undercroft-sealed','human-they-were-kept'], 'orin-threshold', null, 80 ),

  ( 'happened-going-out', 'happened', 'the going-out', 'cardHappenedGoingOut',
    'kept_light_home_01', 'explore',
    array['human-lamp-roll-counts-down','human-they-were-kept'], 'undercroft-fog', null, 90 ),

  -- ========================================================================
  -- THREAD: surface — what is on the surface  (grey)
  -- ========================================================================

  -- ← CONTRADICTED on arrival at place-deep-line (Aro's lie; flips rumor → contradicted)
  ( 'surface-aro-lie', 'surface', 'step right over it', 'cardSurfaceAroLie',
    'first_report_lectern_01', 'rumor',
    array['place-deep-line','surface-pell-truth'], null, null, 10 ),

  ( 'surface-wenna-folk', 'surface', 'seven somethings', 'cardSurfaceWennaFolk',
    'first_report_lectern_01', 'rumor',
    array['place-seven-ways','surface-seventh-marker'], null, null, 20 ),

  ( 'surface-pell-truth', 'surface', 'it does not chase', 'cardSurfacePellTruth',
    'first_report_lectern_01', 'explore',
    array['surface-watcher-counts','surface-aro-lie'], null, null, 30 ),

  ( 'surface-iss-was-right', 'surface', 'right about the sky', 'cardSurfaceIssWasRight',
    'the_threshold', 'explore',
    array['happened-no-wall','happened-the-doubt'], 'no-wall-catch', null, 40 ),

  ( 'surface-watcher-counts', 'surface', 'the record that knows your name', 'cardSurfaceWatcherCounts',
    'first_report_lectern_01', 'explore',
    array['surface-pell-truth','human-names-over-heads','human-the-record-opens'], 'm1-named-habit', null, 50 ),

  ( 'surface-sixth-blank', 'surface', 'the blank sixth way', 'cardSurfaceSixthBlank',
    'rune_rosetta', 'explore',
    array['place-seven-ways','surface-seventh-marker'], 'rosetta-ring', null, 60 ),

  -- ← CONTRADICTS the official "six markers" count (Sella's count against the record's)
  -- THE ISS-SEAM (the-seventh-below.md REWRITE SPEC "Iss-seam"): catching Iss's wall-lie
  -- (the happened-no-wall solve) RE-OPENS this Seventh-marker card — the same lens that
  -- overturns "the ways are a wall" overturns "cast out for nothing", wiring the solved
  -- catch into the Seventh's main quest. The edge is the added happened-no-wall citation +
  -- alt_text_condition 'reopened:no-wall-catch' (the re-surface trigger; the primary
  -- revealed_by_solve 'seventh-shrine' is kept so the first surfacing is unchanged).
  ( 'surface-seventh-marker', 'surface', 'the last marker is not the last', 'cardSurfaceSeventhMarker',
    'the_far_water', 'explore',
    array['who-sella-token','surface-wenna-folk','human-names-over-heads','happened-no-wall'], 'seventh-shrine', 'reopened:no-wall-catch', 70 ),

  -- ========================================================================
  -- THREAD: human — were they human?  (black)
  --   Never resolved by a single card; the thread answers only when the
  --   dehumanization cards are held beside the induction-twist cards.
  -- ========================================================================
  ( 'human-offering-ledger', 'human', 'the open column', 'cardHumanOfferingLedger',
    'offering_cairn_01', 'explore',
    array['who-vaun-counted','place-cairn'], 'stone-vaun', null, 10 ),

  ( 'human-lamp-roll-counts-down', 'human', 'two hundred fourteen to one', 'cardHumanLampRoll',
    'kept_light_home_01', 'explore',
    array['happened-going-out','human-they-were-kept'], 'undercroft-fog', null, 20 ),

  ( 'human-ration-redivided', 'human', 'a head off the roll, still hungry', 'cardHumanRation',
    'stone_of_reckoning', 'explore',
    array['human-names-over-heads','who-deep-market'], null, null, 30 ),

  ( 'human-hand-as-lamp', 'human', 'entry five', 'cardHumanHandAsLamp',
    'kept_light_home_01', 'explore',
    array['human-lamp-roll-counts-down','human-they-were-kept'], null, null, 40 ),

  ( 'human-names-over-heads', 'human', 'nine heads, two hundred fourteen names', 'cardHumanNamesOverHeads',
    'stone_of_reckoning', 'explore',
    array['human-ration-redivided','human-the-record-opens','surface-watcher-counts'], 'm1-named-habit', null, 50 ),

  ( 'human-galleries-unruled', 'human', 'not built for us', 'cardHumanGalleries',
    'unbroken_light', 'explore',
    array['place-deeper-wrong','place-undercroft-sealed'], 'undercroft-fog', null, 60 ),

  ( 'human-they-were-kept', 'human', 'they did not depart', 'cardHumanTheyWereKept',
    'unbroken_light', 'explore',
    array['human-names-over-heads','human-the-record-opens','happened-going-out'], 'undercroft-fog', null, 70 ),

  ( 'human-the-record-opens', 'human', 'an open column is a thing that fills', 'cardHumanRecordOpens',
    'first_report_lectern_01', 'explore',
    array['human-names-over-heads','human-they-were-kept','surface-watcher-counts'], 'm1-record-opens', null, 80 ),

  -- ========================================================================
  -- GATHER-EVENT CARDS — the five the four group summons-night events write
  --   (gather-events.md §6). card_kind 'verified' (director-written when the
  --   gather flag flips); event-revealed → revealed_by_solve null. Anchored to
  --   the Undercroft / Accepting floor (unbroken_light). alt_text_condition on
  --   the two human cards expands them AFTER the relevant ending state.
  -- ========================================================================
  ( 'gather-count-who', 'who', 'named in the same book', 'cardGatherCountWho',
    'unbroken_light', 'verified',
    '{}', null, null, 100 ),

  ( 'gather-count-happened', 'happened', 'the count predates you', 'cardGatherCountHappened',
    'unbroken_light', 'verified',
    '{}', null, null, 100 ),

  ( 'gather-unlight-surface', 'surface', 'the same hands, the last winter', 'cardGatherUnlightSurface',
    'unbroken_light', 'verified',
    '{}', null, null, 100 ),

  ( 'gather-dob-human', 'human', 'a witness, not a monster', 'cardGatherDobHuman',
    'unbroken_light', 'verified',
    '{}', null, 'kept:left_human', 100 ),

  ( 'gather-rehearsal-human', 'human', 'the shape of the choice', 'cardGatherRehearsalHuman',
    'unbroken_light', 'verified',
    '{}', null, 'bowed:as_one', 110 ),

  -- ========================================================================
  -- WEB REALIZATION (WEB-MASTER / INTEGRATION-V2) — the four cards BUILD-MANIFEST §5
  -- names (forged-eighth surface, three-hands, fate, record-url). Each body_voice_key
  -- is a cardXxx key the TS-VOICE / archive owner must define in voice.archive.ts (the
  -- threadCardVoiceCoverageSelfTest build guard fails until they exist — see the RETURN).
  -- anchor_site_id ∈ enabled sites.yml ids; references_card_key resolve in THIS file;
  -- revealed_by_solve is a real puzzle_key (NULL = not solve-gated).
  -- ========================================================================

  -- the forged eighth law, found as one more ordinance among the true seven (surface).
  -- card_kind 'rumor' → flips to 'contradicted' at the M4 record correction. References
  -- Aro's parrot-line card (surface-aro-lie) — the human who repeats the lie.
  ( 'surface-eighth-forged', 'surface', 'the covering of the hands', 'cardSurfaceEighthForged',
    'stone_iss', 'rumor',
    array['surface-aro-lie','happened-no-wall'], 'forged-eighth', null, 80 ),

  -- the three-hands gate — the cold square Mara typed into the dark, read as the rite
  -- instruction at the catch (happened). Anchored at the Undercroft plate. Solve-gated on
  -- the gate clearing. References the catch + the bound word's home.
  ( 'happened-three-hands', 'happened', 'three hands at once', 'cardHappenedThreeHands',
    'coop_plate', 'explore',
    array['happened-no-wall','place-deep-line'], 'm4-three-hands', null, 100 ),

  -- the fate — the ending the floor shows (happened). 'verified', written when the rite
  -- resolves; event-revealed (revealed_by_solve null). Anchored at the Accepting floor.
  -- Names no player (INV-11/16); the card is the neutral close the M5 composer emits.
  ( 'happened-the-fate', 'happened', 'what the floor showed', 'cardHappenedTheFate',
    'unbroken_light', 'verified',
    array['happened-no-wall','human-they-were-kept'], null, null, 120 ),

  -- the record kept elsewhere — the off-world page (surface). 'explore' (verified when the
  -- group finds the path). References the founder archivists card. Solve-gated on record-url.
  ( 'surface-record-elsewhere', 'surface', 'the record is kept elsewhere', 'cardSurfaceRecordElsewhere',
    'first_report_lectern_01', 'explore',
    array['surface-watcher-counts','human-the-record-opens'], 'record-url', null, 90 ),

  -- ========================================================================
  -- THE COMPANION — the found "kept close" tally (the-companion.md §6). Wren's ONE
  -- authored artifact: an inventory of the group in his own hand, the proof no
  -- accusation could be. It CANNOT be found during Trust (he carries it) — it
  -- surfaces ONLY post-reveal, so it is NOT solve-gated (revealed_by_solve null) and
  -- carries alt_text_condition 'companion:revealed' (the reveal event, one group
  -- flag, quorum-free — companion_revealed). It retroactively explains the Observer's
  -- precision (the sharp quotes were harvested here, not magic; §6 design note), so
  -- it clusters under 'human' (the fourth face of "kept" — kept-in-part, a person
  -- holding his edges together with other people's names). card_kind 'verified'
  -- (event-revealed at the reveal, like the gather cards). References the record-
  -- knows-your-name card (surface-watcher-counts — the north-star it earns) + the
  -- open-column card (human-the-record-opens). body_voice_key cardKeptClose is the
  -- Watcher-register archive body defined in voice.archive.ts. Anchored at the cold
  -- hearth (the dead-shrine surface — where a warm liar's proof is dropped).
  ( 'kept-close', 'human', 'kept close', 'cardKeptClose',
    'the_cold_hearth', 'verified',
    array['surface-watcher-counts','human-the-record-opens'], null, 'companion:revealed', 130 )

on conflict (card_key) do nothing;

commit;


-- ============================================================
-- FILE: seeds/side_quests.sql
-- ============================================================

-- The Observance — side_quests.sql
-- The TRAVEL longevity layer (LONGEVITY.md §2 / design/content/travel-destinations.md): 18
-- rumor→verify destinations out in the Hold/world, NONE at spawn, that pay lore/atmosphere/items/
-- time and GATE NOTHING. A pooling ARG group shares each rumor in a second but cannot pool the
-- 1–3k-block walk; that walk is the longevity. Five are deliberate dead leads (the anti-speedrun
-- tax): the rumor is wrong or the place is a grave, and arriving contradicts it.
--
-- BREADTH INVARIANT (migration 0005): every row has gates_progress = false (satisfied by omission —
-- the column defaults false and carries a CHECK (gates_progress = false)). Removing all 18 must leave
-- the spine reconstruction intact. entry_puzzle_key is NULL for all 18: these are DISCOVERED BY TRAVEL,
-- not opened by a spine puzzle node — that is the whole point (they gate nothing and nothing gates them).
--
-- The matching thread_cards (one rumor card → flips to verified/contradicted on arrival, anchored at
-- each destination's sites.yml id) are authored in the thread-card seed; this block seeds only the
-- breadth ledger rows, referenced there by thread_key.
--
-- Additive + idempotent (ON CONFLICT (quest_key) DO NOTHING). Run AFTER 0005_threads.sql (it needs
-- public.side_quests + public.threads), as service_role (RLS bypass — these are spoiler tables).

begin;

insert into public.side_quests
  (quest_key, thread_key, entry_puzzle_key, reward, tier, est_minutes)
values
  ('dest-warm-stair',      'who',      null,
   'the third lamp cold (L01/L02 grief); the_kept_light; thread card under who',                'rumored', 14),
  ('dest-empty-cairn',     'who',      null,
   'the_offering taught by example (Vaun/Orin); soft-offer Watcher line; card under who',        'ambient', 10),
  ('dest-warm-town',       'place',    null,
   'DEAD LEAD: Aro''s warm-town lie; wrong-scaled deep collapse; contradicted card under place',  'rumored', 20),
  ('dest-school-stand',    'human',    null,
   'the_kept_light + seventh-seed; domestic were-they-human detail; card under human',            'ambient', 9),
  ('dest-bird-coops',      'surface',  null,
   'the_sacred_beast; seeds deep-bird vigil; ITEM seed-cake; card under surface',                 'rumored', 13),
  ('dest-far-water',       'who',      null,
   'KEYED (face-the-water mirror): Sella''s copybook drawings; FACT10 seed; verified card, who',  'keyed',   26),
  ('dest-markers-row',     'happened', null,
   'the_bow taught; seventh-mark surplus; seeds THE COUNT; card under happened',                  'ambient', 11),
  ('dest-cistern-7',       'place',    null,
   'the lamp-in-water-lies spook; the_kept_light texture; ITEM good-oil jar; card under place',   'rumored', 15),
  ('dest-third-bay',       'happened', null,
   'SIDE-TRACK: Iss''s breach (R06); the_deep_line context; grave not a road; contradicted, happened','rumored',22),
  ('dest-dead-shrine',     'who',      null,
   'the seventh''s place (L14), kept distinct from Iss; FACT10; card under who',                  'ambient', 24),
  ('dest-set-apart',       'surface',  null,
   'KEYED (digit cross-count): R11 entry-5 the warm cold-lamp; verified card under surface',      'keyed',   18),
  ('dest-watch-floor',     'surface',  null,
   'SIDE-TRACK: Brann''s self-finished watch-log (R12); the_dark_hours; contradicted, surface',   'rumored', 16),
  ('dest-deep-market',     'place',    null,
   'the warmth they grieve (R04, 18 stalls); ITEM chore-token; card under place',                 'ambient', 12),
  ('dest-ration-table',    'human',    null,
   'were-they-human hottest (R09/R14 half-loaf, the child drawing); verified card under human',   'rumored', 13),
  ('dest-undercroft-seal', 'happened', null,
   'Orin''s seal from outside; the_bow via crouch-to-read; card under happened',                  'ambient', 17),
  ('dest-pell-mark',       'surface',  null,
   'topside: the human record (Pell) mirroring the Watcher; card under surface',                  'ambient', 7),
  ('dest-way-up',          'place',    null,
   'SIDE-TRACK: Iss''s forgotten Mouth — real but saves no one; verified-but-hollow, place',      'rumored', 25),
  ('dest-gutter-lamps',    'human',    null,
   'keeping the rite knowing it failed (the three dark levels); the_kept_light; card under human','ambient', 11),

  -- ==========================================================================
  --  THE TWO HOME-ANCHORED [FLAVOR] ANOMALIES (design/SIDEQUEST-PLAN.md §5.1, §5.3).
  --  NOT travel destinations (no 1-3k-block walk) — the two anomalies in the group's OWN
  --  base, engine-seated in a PLUGIN/SHOWRUNNER beat, never a submit-answer (honest
  --  [flavor / atmosphere]; nothing inert costumes itself as a puzzle). GATE NOTHING
  --  (gates_progress false + CHECK, INV-12); entry_puzzle_key NULL (discovered, not opened
  --  by a spine node). Producers are plugin beats (see report), not seed rows.
  -- ==========================================================================
  ('sq-cold-ignition',     'surface',  null,
   '[FLAVOR/lure] week-zero ignition anomaly (cursed map-frame / lit marker knowing a real number); PRODUCER IgnitionListener+prologue.ts sets prologue_ignited; GATES NOTHING (lifts drip suppression only); grounded real value, never fabricated', 'ambient', 6),
  ('sq-count-journal',     'human',    null,
   '[FLAVOR] the base-journal that counts down in a dead keeper''s hand (Orin); PRODUCER BookAppearsBeat/LecternFillBeat page-swap; count = ceil(remaining/cadence), real progress not faked; GATES NOTHING, no submit — reacts as flags flip; FACT13b muster payoff', 'ambient', 8)
on conflict (quest_key) do nothing;

commit;


-- ============================================================
-- FILE: seeds/hints_seed.sql
-- ============================================================

-- The Observance — hints_seed.sql
-- The WHISPER RAIL content (the group's only safety net; OVERHAUL/BUILD-PLAN P0). The `hints` table
-- (0003_discord.sql) ships EMPTY; without bodies, /whisper returns nothing and a group that hits one
-- unsignposted cipher stalls a whole session and quits. This seeds tiered bodies for the SPINE puzzles
-- that survive v2 (the 5 keeper ciphers + the literacy on-ramps + the Iss catch).
--
-- DELIVERY (voice.ts whisperReply): tier 1 is a FIXED ambient nudge in code ("look again at what
-- repeats…") — NOT seeded here. We seed tier 2 (plainer) and tier 3 (near-spells-it, the rescue floor).
-- REGISTER: the Watcher's voice — lowercase, sparse, certain, NEVER "hint: try X". It KNOWS things; it
-- does not give hints. Escalates so the group never hard-locks, but stays in character.
--
-- Run as service_role after 0003. Idempotent (ON CONFLICT (puzzle_key,tier) DO UPDATE).
-- NOTE: these are the spine ciphers only. Author the DIVERSE puzzles' hints (PUZZLES.md §5) as those
-- puzzles are designed; this is the floor that makes a first playtest safe.

begin;

insert into public.hints (puzzle_key, tier, body) values

-- rosetta-ring — the rune-literacy on-ramp (assemble the six ways, in order, off the carved ring).
('rosetta-ring', 2, 'the ring is not decoration. it is a key. the marks around it are the ways, set in the order the record keeps them. read them, and you can read the rest.'),
('rosetta-ring', 3, 'six ways, in the carved order: bow, offering, kept light, deep line, unspoken, sacred beast. learn the mark beside each. that is the alphabet the stones are cut in.'),

-- stone-vaun — Caesar (every letter held back by a fixed amount; his hoarding made literal).
('stone-vaun', 2, 'vaun gave nothing back. even his letters are held back — every one, by the same measure. find the measure and give them back.'),
('stone-vaun', 3, 'each mark stands for a letter shifted a fixed count down the row. try the counts one by one; when the words come clear, that is his.'),

-- stone-mara — book cipher (page/line/word into the lectern shelf she kept).
('stone-mara', 2, 'mara read and did not walk. the numbers on her stone are not the answer — they are where to look. she left the books.'),
('stone-mara', 3, 'three numbers to a word: the page, the line, the word along it. walk her shelf, count to each, and the sentence assembles itself.'),

-- stone-sella — atbash (the mirror; the water gives the face back wrong).
('stone-sella', 2, 'sella speaks only as a reflection now. her marks are the same — read backward. the water would show you how.'),
('stone-sella', 3, 'the first letter is the last, the last is the first, folded at the middle of the row. read her stone as its own mirror.'),

-- stone-orin — substitution (one rune, one letter, kept the same throughout).
('stone-orin', 2, 'orin would not bow, and he would not bend his marks either. each one is a letter, and it is always that letter. find the small words first.'),
('stone-orin', 3, 'a one-for-one swap, steady the whole stone. start where a single mark stands alone — that is "i" or "a" — and let the rest fall in.'),

-- stone-iss-wall — Vigenère keyed on a name (the catch begins here).
('stone-iss-wall', 2, 'iss is warm, and warmth is the wall here. his stone will not open to a plain reading. it wants a key — a word laid over it, again and again.'),
('stone-iss-wall', 3, 'whose wall is this? lay his own name over the marks, letter against letter, and read what comes. the key is the man.'),

-- iss-warm — the warm misreading (true-feeling, leads to the cold dead hearth; the doubt is the point).
('iss-warm', 2, 'the warm reading feels true. it tells you the ways are a wall and you are safe inside. follow it, if you would — but mark where it leads.'),

-- no-wall-catch / iss-doubt — the CATCH (the dead-end pushes you back to re-test his key).
('iss-doubt', 2, 'the warm road was read true and still went nowhere — a cold hearth, a grave, nothing kept. a true road that saves no one. whose road was it?'),
('iss-doubt', 3, 'you trusted his reading once. read his stone again with his own name as the key, the way you should have the first time. the warm wall was never a wall.'),
('no-wall-catch', 2, 'no wall was ever built here. what iss called a wall was the way the reaching was let in. read who carved the comfort, and ask why a man carves a comfort he knows is a lie.'),
('no-wall-catch', 3, 'the warm voice lied. his key turns his own stone to the name they used for him: the one who turned away. the land kept the proof he hoped you would not find.'),

-- a1z26-tick-stave — the numeral-literacy twin (tick marks as counts → letters).
('a1z26-tick-stave', 2, 'these are not words. they are counts. each cluster of ticks is a number, and each number is a letter in its order.'),
('a1z26-tick-stave', 3, 'count the ticks: one is a, two is b, on down the row. read the counts as letters and the stave speaks.'),

-- stone-brann — (when re-authored as the railFence/beacon night cipher, P0-5) the count-the-fires read.
('stone-brann', 2, 'brann kept the watch by the lamps, and counted them twice. the order the lights are read is the order that matters here — not the marks, the sequence.'),
('stone-brann', 3, 'read the lit lamps in their rows, top to bottom, the way a watchman counts down a black moon. the sequence spells what he could not say twice the same.')

on conflict (puzzle_key, tier) do update set body = excluded.body;

commit;


-- ============================================================
-- FILE: seeds/metapuzzle_seed.sql
-- ============================================================

-- The Observance — metapuzzle_seed.sql
-- THE ACTIVATION LANE + the UnlockBeat producer-contract fixes (BUILD-MANIFEST §2.1 /
-- §0.8 / §D4 / §D6; INTEGRATION-V2 §2.1 + precondition 7; WEB-MASTER §0.5 + §2.1).
--
-- This file is UPDATE-ONLY. It carries NO `array[...]` literal, so it is invisible to
-- both seedcheck.ts (which scans puzzles_seed.sql array blocks) and specsCoverageSelfTest
-- (which parses `( 'key', 'title', array[` row openers) — by design: it edits the COLUMN
-- + payload state of rows already inserted by puzzles_seed.sql, never adds a row.
--
-- It fixes the single most dangerous defect the critiques found (arg-craft F1): nine
-- spine/colorant rows ship active=false and the ONLY runtime flip authority was for
-- dead_end rows — so bound-word, m4-three-hands, threshold-coordinate, true-walk-arrive,
-- seventh-unwriting, seventh-cause, seventh-choice, base-docket-reread, and meta-unkept are
-- authored, seeded, and UNREACHABLE. The deterministic puzzles.requires_flags gate
-- (getOpenPuzzles treats a row as open iff every flag in requires_flags is truthy in
-- arc_state.flags) lights the whole back half for ANY outcome_type, not just dead-ends.
--
-- CROSS-OWNER DEPENDENCIES (see the RETURN):
--   * `puzzles.requires_flags jsonb` is the SQL-migration lane's 0006 column add
--     (BUILD-MANIFEST §5 migration row). This file ASSUMES it exists; the `do $$ … $$`
--     guard below no-ops cleanly if 0006 has not landed yet (so re-running is safe and
--     never errors out the batch). getOpenPuzzles (TS-SHOWRUN, repo.ts) must AND-test it.
--   * RevealBeat (`reveal` step) + the `private_message` key-resolver are the
--     TS-SHOWRUN / PLUGIN lanes (backlog-unlockbeat-producers §4). This file only fixes
--     the SEED-side payload-key contracts that don't need code (advancement_toast `key`
--     → `advancement`, the reveal rows' `site_id`+`op`).
--
-- Run AFTER 0004_oracle.sql + (ideally) 0006_*.sql + puzzles_seed.sql, as service_role
-- (RLS bypass — spoiler tables). Idempotent (every UPDATE sets absolute values).

begin;

-- ===========================================================================
-- 0. REPOINT THE CATCH (INTEGRATION-V2 §2.1 step 2 / WEB-MASTER §0.5).
--    no-wall-catch must set iss_caught and STOP — it must NOT shortcut to the rite
--    (no next_puzzle_key: rite-tokens). The rite is reached only THROUGH the chain
--    (bound-word → m4-three-hands → threshold-coordinate → true-walk-arrive →
--    rite-tokens) or the promoted pressure-glyph-walk — never handed at the catch.
--    The catch keeps its iss_caught + true_coord_known flags and its cold-Iss beat;
--    we strip ONLY the rite shortcut. (Idempotent: the `-` operator no-ops if absent.)
-- ===========================================================================

update public.puzzles
  set outcome_payload = outcome_payload - 'next_puzzle_key'
  where puzzle_key = 'no-wall-catch'
    and outcome_payload ? 'next_puzzle_key';

-- ===========================================================================
-- 0b. THE ISS-SEAM oracle line (the-seventh-below.md REWRITE SPEC "Iss-seam").
--     When the wall-lie catch fires, the Watcher adds ONE callback line that wires the
--     solved catch into the Seventh's main quest: "he lied about the wall. ask what else
--     he told you warmly. ask who he said was cast out for nothing." The line is authored
--     in voice.ts as oracleNoWallCatch() (the base main_beat turn + the seam callback);
--     here we only REPOINT the catch's voice_key from oracleMainBeat → oracleNoWallCatch,
--     so puzzles_seed.sql stays untouched and the base turn is preserved (the seam is
--     appended, not a replacement). The paired thread_cards edge (happened-no-wall solve
--     re-opens surface-seventh-marker) is seeded in thread_cards.sql. Idempotent: sets an
--     absolute value; safe to re-run.
-- ===========================================================================

update public.puzzles
  set outcome_payload = jsonb_set(outcome_payload, '{voice_key}', '"oracleNoWallCatch"', true)
  where puzzle_key = 'no-wall-catch';

-- ===========================================================================
-- 1. THE UnlockBeat PRODUCER-CONTRACT FIXES (backlog-unlockbeat-producers §2 R-A,
--    §4.6 decision 1). Three advancement_toast rows pass step_payload:{key:"observance:…"},
--    but AdvancementToastBeat reads `advancement` / `fallback_title` / `fallback_subtitle`
--    — so the toast fires NOTHING today (skipped:"no-advancement-no-fallback"). Rewrite the
--    payload to the producer's real contract. Pure seed edit; no code (the cleanest fix —
--    the beat already reads `advancement`). The cold-Iss `private_message` and the `reveal`
--    rows are left for the TS-SHOWRUN key-resolver / PLUGIN RevealBeat lanes (cross-owner).
-- ===========================================================================

-- rosetta-ring + a1z26-tick-stave: the M1 literacy toast (the SEED-UNLOCK-TOAST-BOOKEND
-- plant; backlog-unlockbeat-producers §5). Same advancement, runes / runes-free twins.
update public.puzzles
  set outcome_payload = jsonb_set(
        outcome_payload,
        '{beat,payload}',
        jsonb_build_object(
          'step', 'advancement_toast',
          'advancement', 'observance:the_ring_is_whole',
          'fallback_title', 'the record',
          'fallback_subtitle', 'the record notes you can read it now'
        ),
        true)
  where puzzle_key in ('rosetta-ring', 'a1z26-tick-stave')
    and (outcome_payload #> '{beat,payload}') ? 'key';

-- record-receives: the M5 bookend toast (the payoff end of the same plant). The arc that
-- opened on "the record notes you" closes on "the record receives you" — identical UI
-- register, now a verdict. Same key→advancement contract fix.
update public.puzzles
  set outcome_payload = jsonb_set(
        outcome_payload,
        '{beat,payload}',
        jsonb_build_object(
          'step', 'advancement_toast',
          'advancement', 'observance:the_record_receives_you',
          'fallback_title', 'the record',
          'fallback_subtitle', 'the record receives you'
        ),
        true)
  where puzzle_key = 'record-receives'
    and (outcome_payload #> '{beat,payload}') ? 'key';

-- ===========================================================================
-- 2. THE requires_flags ACTIVATION LANE (the back half; arg-craft F1).
--    Set the requires_flags COLUMN on every staged/back-half row so getOpenPuzzles lights
--    it deterministically the instant its gate flags are truthy in arc_state — independent
--    of the showrunner. The flag a row waits on is the flag its UPSTREAM door SETS:
--
--      iss_caught        ← no-wall-catch (the catch)
--      bound_word_known  ← bound-word
--      threshold_open    ← m4-three-hands
--      true_coord_known  ← threshold-coordinate
--      seventh_named     ← seventh-unwriting
--      seventh_suspected ← stone-sella / seventh-shrine (the deep's pre-req)
--
--    Authored as a guarded block so it cleanly no-ops if 0006 (the column) has not landed.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_flags'
  ) then

    -- M4 the single Iss chain — each link gated on the link before it (sequenced, §0.4):
    -- bound-word opens at the catch; the gate at the bound word; the coordinate at the gate;
    -- the walk at the coordinate. This is the chain that was authored-but-dark.
    -- OVERHAUL §5: the docket twin is collapsed. With requires_flags now real (0006), the
    -- DETERMINISTIC base-docket-reread-auto fully covers the M4 re-read; the showrunner-flipped
    -- base-docket-reread is retired (left active=false in puzzles_seed) to give the four docket
    -- answers a single owner. So base-docket-reread is NOT activated here.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true)
      where puzzle_key in ('bound-word', 'base-docket-reread-auto', 'meta-unkept');
    update public.puzzles set requires_flags = jsonb_build_object('bound_word_known', true)
      where puzzle_key = 'm4-three-hands';
    update public.puzzles set requires_flags = jsonb_build_object('threshold_open', true)
      where puzzle_key = 'threshold-coordinate';
    update public.puzzles set requires_flags = jsonb_build_object('true_coord_known', true)
      where puzzle_key = 'true-walk-arrive';

    -- M3→IV the Seventh deep — opens only post-iss_caught AND seventh_suspected (the deep
    -- below the dead-shrine; WEB-MASTER §0.4 temporal layering). The choice waits on the name.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true, 'seventh_suspected', true)
      where puzzle_key in ('seventh-unwriting', 'seventh-cause');
    update public.puzzles set requires_flags = jsonb_build_object('seventh_named', true)
      where puzzle_key = 'seventh-choice';

    -- Activating the gate flips these rows' static active flag ON (so getOpenPuzzles' active
    -- pre-filter does not exclude them before the requires_flags AND-test runs). The pair
    -- (active=true AND requires_flags-satisfied) is the open condition; both must hold.
    update public.puzzles set active = true
      where puzzle_key in (
        'bound-word', 'm4-three-hands', 'threshold-coordinate', 'true-walk-arrive',
        'seventh-unwriting', 'seventh-cause', 'seventh-choice', 'meta-unkept'
      );

  else
    raise notice 'metapuzzle_seed: puzzles.requires_flags absent (0006 not applied) — activation lane skipped, re-run after the migration.';
  end if;
end $$;

-- ===========================================================================
-- 3. THE ACTIVATION-REACHABILITY LEDGER (activationReachabilitySelfTest, BUILD-MANIFEST §8 /
--    seedcheck NEW). This comment block is the authoritative map the NEW seedcheck assertion
--    (the TS-SHOWRUN / oracle/seedcheck.ts owner adds it; listed in the RETURN) verifies:
--    every requires_flags row is named by EXACTLY ONE activation rule, and NO active=true row
--    is reachable only through a staged predecessor. The activation rules ARE §2 above (one
--    UPDATE per gate). The full map, row → the single flag-rule that lights it:
--
--      bound-word                → iss_caught                          (no-wall-catch)
--      base-docket-reread-auto   → iss_caught                          (no-wall-catch)   [deterministic; twin retired]
--      meta-unkept               → iss_caught                          (no-wall-catch)
--      m4-three-hands            → bound_word_known                    (bound-word)
--      threshold-coordinate      → threshold_open                      (m4-three-hands)
--      true-walk-arrive          → true_coord_known                    (threshold-coordinate)
--      seventh-unwriting         → iss_caught ∧ seventh_suspected      (no-wall-catch ∧ stone-sella)
--      seventh-cause             → iss_caught ∧ seventh_suspected      (no-wall-catch ∧ stone-sella)
--      seventh-choice            → seventh_named                       (seventh-unwriting)
--
--    INVARIANT (seedcheck NEW): rite-tokens (active=true, the Accepting on-ramp) is reachable
--    via true-walk-arrive's next_puzzle_key AND independently via pressure-glyph-walk
--    (side_quest, the promoted 2nd in-road) — so it is NOT reachable only through a staged
--    predecessor. The repoint in §0 (catch no longer shortcuts to rite-tokens) does not
--    orphan the rite: the chain + the glyph-walk both still arrive. No spine row requires a
--    FORK flag (seedcheck §A11) — fork-light/fork-name set color flags read only by the M5
--    composer, never by requires_flags.
-- ===========================================================================

-- ===========================================================================
-- 4. THE COMPANION REVEAL GATE (the-companion.md §4/§7). Wren's reveal is tied to
--    the Iss catch, NOT a calendar (async-safe): the same lens that caught Iss's warm
--    lie (iss_caught) turns on the living warm liar. So the companion reveal is gated
--    behind iss_caught with the SAME guarded requires_flags pattern used in §2 — a row
--    is OPEN iff active=true AND every requires_flags key is truthy in arc_state.flags.
--
--    THE FLAG CONTRACT (§7 seeds/flags), all group-scoped arc_state flags:
--      companion_introduced   — set when Wren first appears (M1). PRODUCER: plugin.
--      companion_trust (int)  — rises across M1–M3 (harvest ramps with it). PRODUCER: plugin.
--      companion_tells_seeded — his steering tells are planted (M3). PRODUCER: plugin.
--      companion_revealed     — the reveal event fired (M4); GATE: iss_caught. PRODUCER: plugin.
--      reckoning_condemn | reckoning_understand | reckoning_free — the group's chosen
--                               record-line about him (M5), mutually exclusive. PRODUCER: plugin.
--
--    PENDING PRODUCERS (out of THIS scope): the flag PRODUCERS are plugin listeners
--    (the companion is Citizens2 / a beat listener; §7 NPC framework). This file wires
--    the DATA + GATING only. The reveal's CONTENT is npcLines.wren.reveal.* and the
--    found tally is cardKeptClose (thread_cards `kept-close`, alt_text_condition
--    'companion:revealed'); both are authored now. The producer that SETS
--    companion_revealed must AND-check iss_caught before firing (mirrored below in SQL
--    so that, the instant a `companion-reveal` puzzle row is ever seeded, its gate is
--    correct without a second edit).
--
--    GUARDED + IDEMPOTENT: the block no-ops cleanly if 0006 (the column) has not landed
--    AND no-ops if the companion reveal row is not seeded yet (the UPDATE matches 0 rows).
--    So it is safe today (row absent → 0 rows updated) and correct the day the row lands.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_flags'
  ) then
    -- Gate the companion reveal behind the Iss catch (the-companion.md §4). No-ops
    -- until a 'companion-reveal' row exists (producers pending; §7); harmless today.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true)
      where puzzle_key = 'companion-reveal';
  else
    raise notice 'metapuzzle_seed: puzzles.requires_flags absent (0006 not applied) — companion reveal gate skipped, re-run after the migration.';
  end if;
end $$;

commit;


-- ============================================================
-- FILE: seeds/progression_seed.sql
-- ============================================================

-- The Observance — progression_seed.sql
-- THE TWO DEEPENING LANES: the Nether (deep fire-source) + the End (exile / the
-- Seventh's absence). INTEGRATION-V2 A15 / WEB-MASTER §0.4 / BUILD-MANIFEST §5 /
-- WORLD-BIBLE §12. Both lanes GATE NOTHING (INV-12) and are NOT in the Accepting
-- quorum (INV-19); a portal-skipping group gets the whole un-shaded Overworld arc.
--
-- This file carries (1) the two on-site PAYOFF puzzle rows (puzzles_seed.sql shape:
-- one INSERT per node, ON CONFLICT DO UPDATE, every accepted_answers entry ALREADY
-- NORMALIZED per ORACLE.md §2 — lower, [a-z0-9 ] only, single-spaced, trimmed, NO
-- apostrophe), (2) the two BREADTH side-quest rows (side_quests.sql / seventh_seed.sql
-- shape, ON CONFLICT DO NOTHING, gates_progress false), and (3) the two rumor CARDS
-- (thread_cards.sql shape) that flip verified on arrival. It then sets the
-- requires_flags COLUMN on the two payoff rows in a GUARDED block (the metapuzzle_seed.sql
-- pattern), so the file no-ops cleanly if the 0006 column has not landed.
--
-- NO NEW CIPHER. The on-site words are READ-ONLY plaintext found on the slab/carving
-- (the keeper's name / the founders' word "lent"; the Seventh's read) — INV-14: the
-- WORD answers, never a decoded coordinate. They normalize clean (no transform). The
-- bearing/pointer that SENDS the group is a reveal on an existing surface (the
-- unwriting wall's extra line) + Brann's framing line — neither is a row here (S9).
--
-- CROSS-OWNER DEPENDENCIES (see the RETURN):
--   * `nether_forge_found` + `seventh_seen_out` are arc_state flags — the SQL-migration
--     lane's add (BUILD-MANIFEST §5 migration row). Both gate nothing. `nether_forge_found`
--     is the PROPOSED FateInput.netherForgeFound — NOT wired into decideFate until WEB-MASTER
--     §8 is ratified (S9); the M5 composer reads it for a tint meanwhile. `seventh_seen_out`
--     is NEVER a fate input (S2) — M5-composer + seventh_choice-context only.
--   * `puzzles.requires_flags jsonb` is the 0006 column (the activation lane). This file
--     ASSUMES it MAY exist; the guarded block below no-ops if absent.
--   * Voice keys `nether.soulSand`, `nether.forgeArrive`, `end.shrineArrive`,
--     `end.outsideRecord` are inserted verbatim by the TS-VOICE lane from the LORE hand-off
--     (a missing voice key is SILENT at runtime, never a build break — only thread_cards
--     body keys are build-guarded). cardNetherForge / cardEndSeventhOut bodies are the
--     TS-VOICE/archive owner's (threadCardVoiceCoverageSelfTest fails until they exist).
--   * NON_CIPHER_KEYS: the two ACTIVE payoff rows (when activated) are non-cipher lore
--     nodes — the TS-FORGE lane must add 'nether-forge' + 'end-seventh-out' to NON_CIPHER_KEYS
--     or specsCoverageSelfTest fails (listed in the RETURN). Until activated (active=false)
--     they are exempt.
--   * siteCoverageSelfTest extension (R7): a cross-dimension row must NOT seed OPEN unless
--     its site resolves to a placed + enabled site IN THE NAMED WORLD (observance_nether /
--     observance_end must EXIST). The rows ship active=false until the dimension worlds are
--     built (GO-LIVE), so this holds by construction.
--
-- Run AFTER 0004_oracle.sql + 0005_threads.sql + (ideally) 0006_*.sql + puzzles_seed.sql +
-- seventh_seed.sql, as service_role (RLS bypass — spoiler tables). Idempotent.

begin;

-- ===========================================================================
-- 1. THE PAYOFF PUZZLE ROWS (puzzles_seed.sql shape; ON CONFLICT DO UPDATE).
--    Both ship active=FALSE — STAGED until (a) the dimension world is built AND
--    (b) the requires_flags gate is satisfied (§4 below). Both are `lore` (a told
--    secret, no door) — they GATE NOTHING. The destination WORD is the answer
--    (INV-14), read off the slab/carving on-site; the row is the closed-loop
--    acknowledgement the AnswerSignListener records when the on-site word is typed.
-- ===========================================================================

insert into public.puzzles
  (puzzle_key, title, accepted_answers, outcome_type, outcome_payload, movement, active, max_attempts)
values

-- ───────────────────────────────────────────────────────────────────────────
-- THE NETHER LANE — the near pocket / deep fire-source (FACT 11, "below the below")
-- ───────────────────────────────────────────────────────────────────────────

-- nether-forge — the on-site PAYOFF at the Nether pocket (the-fire-kept-me slab). The
-- WORD cut on the slab is the answer (INV-14 — the keeper's name, or the founders' word
-- "lent"), NOT a coordinate. Sets nether_forge_found (the group-scoped colorant flag —
-- the PROPOSED FateInput.netherForgeFound, NOT wired into decideFate until §8 ratified, S9)
-- + whisper_budget_earned (bonus, additive, never the front-loaded backstop — INV-15/S10) +
-- reveals the Kept-Light ORIGIN (the keeping was always a carrying). GATES NOTHING. lore.
-- requires_flags {undercroft_open} (set in §4 — found at the Undercroft post-descent). The
-- bearing that sends the group is Brann's M2 framing line + the-fire-is-lent page, not a row.
-- active=false: STAGED until observance_nether is built (GO-LIVE) AND undercroft_open is set.
( 'nether-forge',
  'the fire is lent',
  array[
    'lent',
    'the fire is lent',
    'you do not own the fire you carry it',
    'the keeping was always a carrying'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'nether.forgeArrive',
    'set_flags', jsonb_build_object('nether_forge_found', true, 'whisper_budget_earned', true),
    'voice_args', jsonb_build_object(
      'fragment', 'a keeper came down to keep the fire and was kept by it. you do not make the fire. you do not own it. you carry it, and you do not let it die, and that is the whole of it. the kept light upstairs was a coal carried up from here.'
    )
  ),
  3, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- THE END LANE — the Seventh shrine / the one place outside the record (FACT 10b / D11)
-- ───────────────────────────────────────────────────────────────────────────

-- end-seventh-out — the on-site PAYOFF at the End Seventh-shrine (the-name-i-cut-myself
-- carving). The Seventh's own account from outside the record. Sets seventh_seen_out — a
-- group-scoped flag, NEVER a fate input (S2): it deepens the seventh_choice context (a group
-- that walked the End learns WHY the Seventh chose exile) and licenses the End's cast_out/
-- refusers re-read in the M5 composer. GATES NOTHING. lore. The on-site READ is the answer
-- (INV-14), not a coordinate. requires_flags {seventh_named} (set in §4 — the way-out pointer
-- on the unwriting wall is legible only at seventh_named; that pointer is a REVEAL on the
-- existing surface, NO row, S9). active=false: STAGED until observance_end is built (GO-LIVE)
-- AND seventh_named is set.
( 'end-seventh-out',
  'the name i cut myself',
  array[
    'i kept all the ways and it did not matter',
    'the keeping was never the price',
    'i went out past the door that is not a threshold',
    'you only came to look'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'end.shrineArrive',
    'set_flags', jsonb_build_object('seventh_seen_out', true),
    'voice_args', jsonb_build_object(
      'fragment', 'the seventh kept every way and was not kept, and went out past the door that is not a threshold, to the one place the record does not reach, and cut the name themselves. exile is the other side of keeping. you are not cast out. you only came to look.'
    )
  ),
  4, false, null )

on conflict (puzzle_key) do update set
  title            = excluded.title,
  accepted_answers = excluded.accepted_answers,
  outcome_type     = excluded.outcome_type,
  outcome_payload  = excluded.outcome_payload,
  movement         = excluded.movement,
  active           = excluded.active,
  max_attempts     = excluded.max_attempts;

-- ===========================================================================
-- 2. THE BREADTH SIDE-QUEST ROWS (side_quests.sql / seventh_seed.sql shape;
--    ON CONFLICT DO NOTHING). The TRAVEL/breadth ledger rows the Recovery Archive
--    clusters the lane cards under — they pay lore/atmosphere/time and GATE NOTHING
--    (gates_progress defaults false + CHECK). entry_puzzle_key is a clustering pointer
--    (a plain pointer, no FK) routing each card under its lane's payoff node. Mirrors
--    dest-unwriting-deep / dest-fire-let-out in seventh_seed.sql. thread 'who' (both
--    lanes deepen who the keepers were — the source-keeper, the Seventh).
-- ===========================================================================

insert into public.side_quests
  (quest_key, thread_key, entry_puzzle_key, reward, tier, est_minutes)
values

  -- The Nether deep-forge delve — the near pocket past the burned door (below the below).
  -- KEYED (on-site word at the slab, INV-14): the Kept-Light origin (the keeping is a carrying);
  -- nether_forge_found + bonus Whisper; soul sand = deep-time (distinct from the Pale herd, S5);
  -- bastion = the founders' ruined delvings. A DELVE not a trek (walk-budget, S4). Card under who.
  ('dest-deep-forge',     'who',  'nether-forge',
   'KEYED (on-site word, INV-14): the deep fire-source; nether_forge_found + bonus Whisper; the Kept-Light origin (a carrying); soul sand = deep-time; card under who',
   'keyed',   18),

  -- The End way-out — out past the door that is not a threshold, to the place outside the record.
  -- KEYED (on-site read, INV-14): the Seventh's own account from exile; seventh_seen_out (NOT a
  -- fate input, S2); deepens the restore/erase choice. The pointer is the unwriting wall's extra
  -- line (a reveal, no row, S9). ZERO apparition lane (the End is outside the record). Card under who.
  ('dest-out-of-record',  'who',  'end-seventh-out',
   'KEYED (on-site read, INV-14): the Seventh outside the record; seventh_seen_out (not a fate input, S2); exile = the other side of keeping; card under who',
   'keyed',   16)

on conflict (quest_key) do nothing;

-- ===========================================================================
-- 3. THE RUMOR CARDS (thread_cards.sql shape; ON CONFLICT DO NOTHING). One card per
--    lane, clustered under 'who'. card_kind 'rumor' → flips to 'verified' on arrival
--    (the solve of the payoff row). body_voice_key is a cardXxx key the TS-VOICE/archive
--    owner must define in voice.archive.ts (threadCardVoiceCoverageSelfTest fails until
--    they exist — the RETURN). anchor_site_id ∈ enabled sites.yml ids in the named world.
--    references_card_key resolve to cards seeded in thread_cards.sql (the lanes deepen the
--    keeper biographies + the Seventh thread). revealed_by_solve = the real payoff row.
-- ===========================================================================

insert into public.thread_cards
  (card_key, thread_key, title, body_voice_key, anchor_site_id, card_kind,
   references_card_key, revealed_by_solve, alt_text_condition, sort_order)
values

  -- the deep fire-source — the pocket keeper kept AS the fire (who). The Kept-Light origin: the
  -- keeping was always a carrying. References Mara (the bearing-page hand) + Vaun (who hoarded the
  -- lent thing and starved). Solve-gated on the on-site word. Flips verified on arrival.
  ( 'who-deep-forge', 'who', 'the keeper kept as the fire', 'cardNetherForge',
    'nether_forge', 'rumor',
    array['who-mara-read','who-vaun-counted'], 'nether-forge', null, 110 ),

  -- the Seventh outside the record — the name cut in exile (who). The one place the unwriting could
  -- not reach. References the Seventh thread (the far-water bearing card + the cast-out account).
  -- Solve-gated on the on-site read. Flips verified on arrival. Deepens seventh_choice.
  ( 'who-seventh-out', 'who', 'the name cut in exile', 'cardEndSeventhOut',
    'end_seventh_shrine', 'rumor',
    array['who-sella-token','surface-seventh-marker'], 'end-seventh-out', null, 120 )

on conflict (card_key) do nothing;

-- ===========================================================================
-- 4. THE requires_flags ACTIVATION GATES (the metapuzzle_seed.sql guarded pattern).
--    Set the requires_flags COLUMN on the two payoff rows so getOpenPuzzles holds each
--    closed until its upstream flag is truthy in arc_state — independent of the showrunner:
--
--      nether-forge     ← undercroft_open   (found at the Undercroft post-descent)
--      end-seventh-out  ← seventh_named     (the way-out pointer is legible only then)
--
--    Authored as a guarded block so it cleanly no-ops if 0006 (the column) has not landed.
--    These rows stay active=false here (STAGED until the dimension worlds are built at GO-LIVE);
--    the GO-LIVE step flips active=true per row once observance_nether / observance_end exist
--    (siteCoverageSelfTest / R7). The requires_flags gate is the SECOND condition — both
--    (active=true AND requires_flags-satisfied) must hold for getOpenPuzzles to open the row.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_flags'
  ) then

    update public.puzzles set requires_flags = jsonb_build_object('undercroft_open', true)
      where puzzle_key = 'nether-forge';
    update public.puzzles set requires_flags = jsonb_build_object('seventh_named', true)
      where puzzle_key = 'end-seventh-out';

  else
    raise notice 'progression_seed: puzzles.requires_flags absent (0006 not applied) — Nether/End activation gates skipped, re-run after the migration.';
  end if;
end $$;

-- ===========================================================================
-- 5. THE ACTIVATION-REACHABILITY LEDGER (activationReachabilitySelfTest, BUILD-MANIFEST §8).
--    The authoritative map the seedcheck assertion verifies — every requires_flags row named
--    by EXACTLY ONE activation rule; no active=true row reachable only through a staged
--    predecessor:
--
--      nether-forge     → undercroft_open   (undercroft-descent)        [GATES NOTHING — lore]
--      end-seventh-out  → seventh_named     (seventh-unwriting)         [GATES NOTHING — lore]
--
--    INVARIANT: neither row is a spine predecessor of any other row (both are leaf `lore`
--    payoffs — no next_puzzle_key, no set_flags any spine row reads as a requires_flags gate).
--    nether_forge_found / seventh_seen_out are read ONLY by the M5 composer (colorants), never
--    by another row's requires_flags and never by the Accepting quorum (INV-19). No spine row
--    requires either flag (INV-12 colors-never-gates). So removing both rows entirely still
--    reconstructs the whole Overworld spine — the lanes are pure deepening, no orphan, no gate.
-- ===========================================================================

commit;


-- ============================================================
-- FILE: schema-repair.sql
-- ============================================================

-- THE OBSERVANCE — schema-repair.sql  (2026-07-01)
-- ---------------------------------------------------------------------------
-- Fixes plugin<->DB schema drift. The Java plugin writes flat, mc_uuid-keyed
-- rows (DossierRow / CustomComplianceRow / BaseRow), but dashboard 0001_init
-- created dossiers / custom_compliance / bases with a player_id-keyed shape and
-- different column names. Result: every background-tracker flush returned HTTP 400.
--
-- This is ADDITIVE + IDEMPOTENT: it ADDS the columns + upsert conflict keys the
-- plugin needs, and KEEPS the old columns so the dashboard's existing reads still
-- resolve (they'll be null for plugin-written rows until a proper reconciliation).
-- The puzzle loop (players / solves / heatmap_cells) already matched and is untouched.
--
-- Apply in the Supabase SQL Editor for the Observance project (fdnmhbpxnodrnbrzrlqq),
-- as service_role. Safe to re-run. After applying, the `tracker.flush.dossier`
-- 400 spam stops and behavior tracking starts saving.
-- ---------------------------------------------------------------------------

begin;

-- ===== dossiers — plugin upserts on mc_uuid with flat signal columns =====
alter table public.dossiers
  add column if not exists mc_uuid              text,
  add column if not exists name                 text,
  add column if not exists solo_mining_seconds  bigint,
  add column if not exists hoarded_score        double precision,
  add column if not exists distance_from_group  double precision,
  add column if not exists extra                text;

-- The plugin inserts without player_id, so player_id can no longer be a NOT NULL
-- primary key. Drop the PK, make it nullable, and key upserts off mc_uuid instead.
-- (Old rows keep their player_id; new plugin rows carry mc_uuid. Dashboard reads by
-- player_id still work for old rows; join via players.mc_uuid for new ones.)
alter table public.dossiers drop constraint if exists dossiers_pkey;
alter table public.dossiers alter column player_id drop not null;
create unique index if not exists dossiers_mc_uuid_uidx on public.dossiers (mc_uuid);

-- ===== custom_compliance — plugin upserts on (mc_uuid, custom_key) =====
alter table public.custom_compliance
  add column if not exists mc_uuid        text,
  add column if not exists name           text,
  add column if not exists honored_count  int default 0,
  add column if not exists violated_count int default 0,
  add column if not exists last_event_at  timestamptz,
  add column if not exists updated_at     timestamptz default now();
-- player_id here is already nullable (the PK is the bigserial `id`), so the plugin's
-- null-player_id inserts are fine; it just needs a unique target for (mc_uuid, custom_key).
create unique index if not exists custom_compliance_mcuuid_key_uidx
  on public.custom_compliance (mc_uuid, custom_key);

-- ===== bases — plugin upserts on id with owner_uuid + center_x/y/z + label + radius =====
-- NOTE: bases.id is a bigint PK but the plugin upserts a STRING id → the id-conflict still
-- mismatches. These column adds are additive/harmless; the full bases fix (id type) is deferred
-- (base-detection is non-critical background tracking, not the puzzle loop). See IMPROVEMENT-AUDIT.
alter table public.bases
  add column if not exists owner_uuid text,
  add column if not exists label      text,
  add column if not exists center_x   int,
  add column if not exists center_y   int,
  add column if not exists center_z   int,
  add column if not exists radius     numeric;

-- ===== event_log — plugin writes type/context/mc_uuid/detail; the table has only level/source =====
-- Without these columns every plugin log write 400s (all plugin diagnostics lost). Additive.
alter table public.event_log
  add column if not exists type    text,
  add column if not exists context text,
  add column if not exists mc_uuid text,
  add column if not exists detail  text;

commit;

-- ===== NOTE: world_paste_ledger — the plugin also references this table for the =====
-- optional FAWE schematic-paste path; no migration creates it. It's only touched if
-- FAWE is installed (it isn't tonight), so it's left out of tonight's repair. Create it
-- when the schematic-paste path is enabled.
