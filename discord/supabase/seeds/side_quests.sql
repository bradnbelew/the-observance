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
   'keeping the rite knowing it failed (the three dark levels); the_kept_light; card under human','ambient', 11)
on conflict (quest_key) do nothing;

commit;
