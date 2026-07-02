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
  --  NOT travel destinations (no 1-3k-block walk) — these are the two anomalies that
  --  appear in the group's OWN base, engine-seated in a PLUGIN/SHOWRUNNER beat, never a
  --  submit-answer (honest [flavor / atmosphere]; nothing inert costumes itself as a
  --  puzzle — OVERHAUL §5 cohesion gate). They GATE NOTHING (gates_progress false + CHECK,
  --  INV-12) and entry_puzzle_key is NULL (discovered, not opened by a spine node — the
  --  same "no node gates them, they gate no node" property the 18 travel rows carry). They
  --  are seeded HERE as the breadth-ledger acknowledgement the Recovery Archive can cluster
  --  a card under; their PRODUCERS are plugin beats (noted in the report), not seed rows.
  -- ==========================================================================

  -- SQ-COLD (§5.1, cold-start-prologue.md + cursed-map-frame.md) — the week-zero ignition
  -- anomaly: ONE incongruity juicy enough to screenshot unprompted (the cursed adventure-map
  -- frame / the lit marker that knows a real number it should not), placed at the first-report
  -- surface so the FIRST notice is findable. PRODUCER: the IgnitionListener + prologue.ts drip
  -- gate (already built) sets arc_state.flags.prologue_ignited on read/post — the lure, never a
  -- gate (it only lifts the curatorial-drip suppression; gifts are ungated). Grounding/consent
  -- (OVERHAUL §4): the "number it should not know" is a REAL observed value (the world seed / a
  -- real count), never fabricated. thread surface (the record on the surface). ambient. NO card
  -- added here (its surface home is first_report_lectern_01, already the anchor of surface cards).
  ('sq-cold-ignition',     'surface',  null,
   '[FLAVOR/lure] week-zero ignition anomaly (cursed map-frame / lit marker knowing a real number); PRODUCER IgnitionListener+prologue.ts sets prologue_ignited; GATES NOTHING (lifts drip suppression only); grounded real value, never fabricated', 'ambient', 6),

  -- SQ-COUNT (§5.3, counting-base-journal.md, pairs record-writes-you-in.md) — a book in the
  -- group''s OWN base whose successive pages, in a dead keeper''s (Orin''s) hand, count DOWN a
  -- buried number toward nothing explained; the last page finally rewrites to address the living
  -- reader. PRODUCER: BookAppearsBeat / LecternFillBeat page-swap (mutateWhenUnwitnessed), the
  -- count derived as ceil(remaining_until_accepting / cadence) — a REAL measured progress value,
  -- never faked (grounding). Honest [flavor]: there is nothing to submit; it REACTS as flags flip
  -- (the FACT 13b payoff — the count is the muster/appointment, not a per-player death-clock; it
  -- carves no living name). thread human (the record writing the living in a dead hand). ambient.
  ('sq-count-journal',     'human',    null,
   '[FLAVOR] the base-journal that counts down in a dead keeper''s hand (Orin); PRODUCER BookAppearsBeat/LecternFillBeat page-swap; count = ceil(remaining/cadence), real progress not faked; GATES NOTHING, no submit — reacts as flags flip; FACT13b muster payoff', 'ambient', 8)
on conflict (quest_key) do nothing;

commit;
