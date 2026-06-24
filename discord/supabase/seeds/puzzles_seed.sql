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
  jsonb_build_object('voice_key', 'oracleDeadEnd'),
  1, true, null ),

-- rosetta-ring — the literacy gate (TWO doors: server-icon ring B + founder note C).
-- main_beat: unlocks the master script. FACT 3 + FACT 4 seed; front-margin seeds F15.
( 'rosetta-ring',
  'learn them as we learned them',
  array[
    'bow offering kept light deep line ward covering',
    'bow offering keptlight deepline ward covering'
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
    'iss',
    'the ways are a wall against the watching'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'iss-doubt',
    'set_flags', jsonb_build_object('iss_key_turned', true)
  ),
  2, true, 6 ),

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
  jsonb_build_object('voice_key', 'oracleDeadEnd'),
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
  jsonb_build_object('voice_key', 'oracleDeadEnd'),
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
-- footstep. Pure lore (redundant optional approach to the rite; skippable).
( 'pressure-glyph-walk',
  'walk the rune',
  array[
    'walk the rune',
    'do not decode walk it',
    'trace the rune with your feet'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the floor names a rune. do not decode it. walk it. mara left one lesson: do, dont read your way out.'
    )
  ),
  5, true, null ),

-- accepting-crouch — everyone present bows as one (synchronized crouch), at the hour,
-- in the kept light. main_beat: collective, no chosen one. → record-receives.
-- An opaque plugin-posted sentinel is the safe in-world trigger (never guessable).
( 'accepting-crouch',
  'bow as one',
  array[
    'bow as one',
    'when all of you bow as one',
    'a7f3 accepting bow sentinel posted only by plugin'
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
    'the record receives you',
    'it receives it would keep you',
    'e0c4 record receives sentinel posted only by plugin'
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
  5, false, null )

on conflict (puzzle_key) do update set
  title            = excluded.title,
  accepted_answers = excluded.accepted_answers,
  outcome_type     = excluded.outcome_type,
  outcome_payload  = excluded.outcome_payload,
  movement         = excluded.movement,
  active           = excluded.active,
  max_attempts     = excluded.max_attempts;

commit;
