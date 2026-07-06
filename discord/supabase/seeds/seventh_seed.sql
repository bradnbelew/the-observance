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
  -- law among the true seven. It pays by becoming a testable false law: the silence when
  -- obeyed and the catch when broken prove Iss's doctrine was authored, not sacred.
  ('dest-covering-law',    'surface',  'forged-eighth',
   'FALSE-LAW PROOF: the forged eighth (FACT7b); obeying it gives silence, breaking it names the lie; the_-prefixed nothing (fiction, INV-17); contradicted card under surface',
   'rumored', 14),

  -- The prophet''s wall — Iss with a pulpit (B2). This is the second and final launch-budget
  -- false lead: warm promises decode cleanly but open no road; the hidden columnar name is his own.
  ('dest-prophet-wall',    'happened', null,
   'DEAD LEAD WITH TEETH: Iss''s pulpit; warm promises decode cleanly but open no road; hidden columnar name = Iss; contradicted card under happened',
   'rumored', 16),

  -- The true Threshold walk — the coordinate the Threshold carving yields AFTER the gate
  -- (the single Iss chain). The destination WORD is the answer (INV-14), not the coord.
  -- KEYED (on-site presence): the Accepting on-ramp. Distinct from the false dead-shrine walk.
  ('dest-true-threshold',  'place',    'threshold-coordinate',
   'KEYED (on-site presence): the TRUE walk (INV-14 destination word, not the coord); the Accepting on-ramp; verified card under place',
   'keyed',   22)
on conflict (quest_key) do nothing;

commit;
