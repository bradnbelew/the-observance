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

-- seventh-name — THE SEVENTH READING capstone (design/THE-SEVENTH-READING.md). The six keepers each
-- hid one letter of the Seventh's name in their OWN technique (Vaun caesar, Mara book, Sella atbash,
-- Orin substitution, Brann rail-fence at night, Iss the catch/acrostic); read in fall-order they spell
-- AVERYN. SAYING the name is the RESTORE act AND the release trigger: set_flags sets seventh_name (a
-- STRING flag the finale composer reads) + seventh_choice='restore' + seventh_named + record_released
-- (the showrunner's release pass then composes the mask-off farewell + fires the_closing). NON-cipher
-- node (the cipher is the distributed six-fragment reading, not a single carved card) → NON_CIPHER_KEYS.
-- Gated {seventh_named, bowed_as_one} in metapuzzle_seed §2 — only sayable at the very end, after the
-- Accepting. The capstone integrity guard (seventh-reading.selftest) proves the six fragments spell this.
( 'seventh-name',
  'say the name the six kept',
  array[
    'averyn'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleSeventhName',
    'set_flags', jsonb_build_object(
      'seventh_name', 'averyn',
      'seventh_choice', 'restore',
      'seventh_named', true,
      'record_released', true
    )
  ),
  5, true, null ),

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

-- ===========================================================================
-- THE DIVERSE EXPANSION (design/PUZZLE-DESIGNS.md) — 17 new puzzles across 11 TYPE
-- categories, wired here as a SECOND insert whose column list carries the 0007
-- `answer_kind` column (the first insert's rows all default to 'phrase' in the DB).
--
-- answer_kind PLACEMENT: it sits BEFORE (movement, active, max_attempts) so the
-- trailing (movement, active, max) triple that specsCoverageSelfTest / parseSeedKeys
-- anchor on stays the LAST such triple in each row body (the parser is unchanged).
--
-- CONVENTIONS (identical to the first insert):
--   * Every accepted_answers value is pre-normalized (lower, [a-z0-9 ], single-spaced,
--     trimmed, NO apostrophe) per ORACLE.md §2 — seedcheck enforces it.
--   * TYPED kinds (phrase | coords | url_token) carry the real readable answer the
--     resolver matches. PLUGIN-PRODUCED kinds (behavior | object | code | spoken)
--     carry an OPAQUE, wordless, high-entropy token the plugin posts on real detection
--     (red-team B-5 / no-leaked-sentinel) — never a human-typeable phrase, so a
--     detected act cannot be spoofed by typing. Each such row NEEDS a plugin PRODUCER
--     listener (a later round; see the report) — the row + gating are wired now.
--   * New voice keys are avoided: every row reuses an existing oracle voice key
--     (oracleNextClue / oracleLore / oracleDeadEnd / oracleSideQuest / oracleMainBeat),
--     which the resolver already speaks — nothing to add to voice.ts.
--   * Non-cipher classification: every ACTIVE row here is added to NON_CIPHER_KEYS in
--     src/forge/clue-specs.ts (none carries a Discord-decodable cipher carving).
--   * Gating: rows that hang off the Iss catch / Seventh deep are shipped active=false
--     and lit by the requires_flags activation lane in metapuzzle_seed.sql §2 (the same
--     deterministic gate the back-half spine uses) — so they never leak before their door.
-- ===========================================================================

insert into public.puzzles
  (puzzle_key, title, accepted_answers, outcome_type, outcome_payload, answer_kind, movement, active, max_attempts)
values

-- ───────────────────────────────────────────────────────────────────────────
-- 2. VAUN — the hoarder (arrange/count; object/code)
-- ───────────────────────────────────────────────────────────────────────────

-- vaun-hoard-sorted (§2.1) — deposit the first-of-the-deep into the empty "given back"
-- chest (the offering Vaun never made). answer_kind 'object': a container-content check
-- the plugin performs; the token below is the opaque flag the producer posts on the real
-- deposit (never the typed fallback phrase, which would leak). main_beat: opens Vaun's
-- cache (his Caesar stone becomes readable behind it). PRODUCER: HoardSortedListener.java.
( 'vaun-hoard-sorted',
  'give the first of the deep back',
  array[
    'g8k2 vq7m x4d9 p1n6 given back'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('vaun_cache_open', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'stone_vaun',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'vaun_cache_opened')
      )
    )
  ),
  'object', 2, true, null ),

-- vaun-bookshelf-tally (§2.2) — a chiseled-bookshelf 6-slot register; place books to
-- reproduce Vaun's "all taken, none given" tally. answer_kind 'code' (a comparator lock
-- the plugin reads); opaque token posted on the cleared pattern. next_clue → stone-vaun
-- (points at the now-readable Caesar stone). Gated on vaun_cache_open (the cache must be
-- open first). PRODUCER: BookshelfTallyListener.java. active=false → lit at
-- vaun_cache_open by the metapuzzle activation lane.
( 'vaun-bookshelf-tally',
  'count the door open',
  array[
    'b6t3 kq9w m2x7 v4d1 taken column'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'stone-vaun'
  ),
  'code', 2, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 3. MARA — the reader who never walked (arrange/perform; code/behavior)
-- ───────────────────────────────────────────────────────────────────────────

-- mara-lectern-lock (§3.1) — five lecterns turned to the pages Mara annotated (a
-- comparator page-lock). answer_kind 'code'; opaque token on the cleared combination.
-- next_clue → mara-walk-the-map (the alcove says "walk the rite you have only read").
-- PRODUCER: LecternLockListener.java.
( 'mara-lectern-lock',
  'the rite read and never walked',
  array[
    'l5p2 mq8k w1n4 t6d3 marked pages'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'mara-walk-the-map',
    'set_flags', jsonb_build_object('mara_alcove_open', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'stone_mara',
      'priority', 11,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  'code', 3, true, null ),

-- mara-walk-the-map (§3.2) — the group physically travels to the marker row and bows
-- together with the active roster (quorum = effectiveQuorum). answer_kind 'behavior';
-- opaque token on the detected group-bow. main_beat: the record writes Mara whole.
-- Gated on mara_alcove_open. PRODUCER: GroupWalkListener.java.
-- active=false → lit at mara_alcove_open by the metapuzzle activation lane.
( 'mara-walk-the-map',
  'what she read you walked',
  array[
    'w7k4 mq2p n9x1 b3d6 walked together'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('mara_walked', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'first_marker_01',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'mara_written_whole')
      )
    )
  ),
  'behavior', 3, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 4. SELLA — the drowned child (reflection/overlay/forced-perspective; coords/behavior)
-- ───────────────────────────────────────────────────────────────────────────

-- sella-reflection-bearing (§4.1) — the blank shore stone reads a bearing only in the
-- water's reflection; the bearing points to the far water. answer_kind 'coords': the
-- clean DESTINATION WORD found on-site (INV-14), never the signed coordinate. next_clue
-- → sella-overlay-lake (at the far water). Typed (a word read off the reflection).
( 'sella-reflection-bearing',
  'the rune only the water shows',
  array[
    'south by the far water where the reeds fold back',
    'the far water where the reeds fold back',
    'where the reeds fold back'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'sella-overlay-lake',
    'set_flags', jsonb_build_object('sella_bearing_read', true)
  ),
  'coords', 2, true, null ),

-- sella-overlay-lake (§4.2) — two filled maps overlaid resolve into a shore outline with
-- an X. answer_kind 'coords': the destination word the overlaid X marks, read on-site.
-- lore payoff (a Sella drawing that is only joy) + next_clue seed toward the deep. Typed.
-- Gated on sella_bearing_read (you must have followed the bearing to the far water).
-- active=false → lit at sella_bearing_read by the metapuzzle activation lane.
( 'sella-overlay-lake',
  'two leaves become one place',
  array[
    'the drowned place the child drew',
    'where she went the shore with the x',
    'the shore the two maps make'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'sella-shore-memorial',
    'set_flags', jsonb_build_object('sella_overlay_read', true)
  ),
  'coords', 3, false, null ),

-- sella-shore-memorial (§4.3) — a scatter of blocks that forced-perspective-resolves,
-- from one anchor block, into Sella's bird-over-water glyph. answer_kind 'behavior':
-- stand-at-anchor + look-down detection; opaque token on the detected vantage. lore: a
-- wordless Sella beat (only the bird). Gated on sella_overlay_read.
-- PRODUCER: ShoreMemorialListener.java. active=false → lit at sella_overlay_read.
( 'sella-shore-memorial',
  'legible only from above',
  array[
    's4k7 vq1m x8d2 p6n3 the bird'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'from the one worn stone above the pool the scatter is a bird over water. she kept it. the record, for once, has nothing to say. only the bird.'
    )
  ),
  'behavior', 3, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 5. ORIN — the mason who would not bow (perform/decrypt/rotate; behavior/phrase/code)
-- ───────────────────────────────────────────────────────────────────────────

-- orin-bow-fall-order (§5.1) — bow (crouch) at the six markers in FALL-ORDER (Vaun, Mara,
-- Sella, Orin, Brann, Iss). answer_kind 'behavior'; opaque token on the ordered-bow
-- sequence. next_clue → orin-threshold (his threshold-stone becomes readable from the
-- crouch). PRODUCER: OrderedBowListener.java.
( 'orin-bow-fall-order',
  'bow at the markers in fall order',
  array[
    'o9k2 mq6w x1n8 t4d7 fall order'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'orin-threshold',
    'set_flags', jsonb_build_object('orin_bowed', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'first_marker_01',
      'priority', 11,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object('fragment', 'orin_threshold_readable')
      )
    )
  ),
  'behavior', 4, true, null ),

-- orin-banner-heraldry (§5.2) — six keeper banners; Orin's mason-square sigil IS the
-- substitution KEY that unlocks his EXISTING substitution stone (stone-orin). answer_kind
-- 'phrase': the substitution plaintext, now solvable via the found key (canon plaintext
-- already exists on stone-orin). next_clue → stone-orin. Typed. NOT a new cipher — it is
-- the KEY-DELIVERY for the existing Orin substitution (PUZZLES.md §0 "difficulty in noticing").
( 'orin-banner-heraldry',
  'the sigil that is the key',
  array[
    'the mason square is the key to his stone',
    'the banner is the substitution key',
    'read his stone with the mason square'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'stone-orin',
    'set_flags', jsonb_build_object('orin_key_found', true)
  ),
  'phrase', 2, true, null ),

-- orin-frame-dials (§5.3) — six item-frame rotation dials pointed to match the markers'
-- fall-order facings (a physical combination lock). answer_kind 'code'; opaque token on
-- the cleared 6x8 rotation state. lore: Orin's private offering (the one custom he DID
-- keep). Gated on orin_bowed (you must have walked the markers first).
-- PRODUCER: FrameDialsListener.java. active=false → lit at orin_bowed.
( 'orin-frame-dials',
  'the marker sequence lock',
  array[
    'd7k1 mq4x n2w9 t8d3 six dials'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'the dials turn to face the way the markers face, and the niche gives. inside is the one offering orin kept. he would not bow to the living. he bowed here, alone, to no one, and told no one.'
    )
  ),
  'code', 4, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 6. BRANN — the watchman on the black moon (temporal/audio; phrase/behavior)
-- ───────────────────────────────────────────────────────────────────────────

-- brann-black-moon-toll (§6.1) — a note-block/bell morse toll that plays ONLY on the
-- in-game black moon; the rhythm spells the word Brann most needs said. answer_kind
-- 'phrase' (temporal-gated): typed, but the plugin only accepts it once the toll has been
-- heard on a black moon (a temporal flag the producer sets). next_clue → stone-brann.
-- Typed. PRODUCER: BlackMoonTollListener.java.
( 'brann-black-moon-toll',
  'the toll that rings in the dark',
  array[
    'awake',
    'stay awake',
    'do not close your eyes'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'stone-brann',
    'set_flags', jsonb_build_object('brann_toll_heard', true)
  ),
  'phrase', 2, true, null ),

-- brann-silence-corridor (§6.2) — a calibrated-sculk corridor passable only in silence
-- (sneak; no vibration). answer_kind 'behavior'; opaque token on reaching the far door
-- quietly. next_clue → stone-brann (the far door opens onto his watch-record).
-- PRODUCER: SilenceCorridorListener.java (optional voice-chat tie-in later, not built).
( 'brann-silence-corridor',
  'the corridor that hears you',
  array[
    'r6k3 mq1w x9n2 t5d8 in silence'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'stone-brann',
    'set_flags', jsonb_build_object('brann_corridor_passed', true),
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'stone_brann',
      'priority', 11,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  'behavior', 2, true, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 7. ISS — the liar (logic/stego/callback; phrase/object/callback)
-- ───────────────────────────────────────────────────────────────────────────

-- iss-which-is-true (§7.1) — cross-check Iss's warm wall-doctrine against the land (the
-- cold hearth, the later stone, the record's flat line): which account does the land agree
-- with? answer_kind 'phrase'. lore (NOT a second owner of iss_caught — the canonical catch
-- stays no-wall-catch; this is the additive deduction rung that primes it). Gated on
-- iss_key_turned (you must have turned his key). Points at iss-doubt (the existing catch
-- path). Typed.
( 'iss-which-is-true',
  'the warm account against the land',
  array[
    'the ways are not a wall',
    'no wall was ever built here',
    'he lied about the wall'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'iss-doubt',
    'set_flags', jsonb_build_object('iss_wall_doubted', true)
  ),
  'phrase', 3, false, null ),

-- iss-nbt-falsified-entry (§7.2) — a warm-worded "keepsake lamp" whose NBT hides the
-- falsified record entry Iss wrote about the Seventh (a hex/base64 field decodes to a line
-- + a record-website path token). answer_kind 'url_token': the decoded path the group then
-- corrects on the record website. next_clue → seventh-shrine (correcting it advances the
-- Seventh thread). Gated on iss_caught (a group that has learned to distrust Iss inspects
-- his gift). Typed (the decoded path token). active=false → lit at iss_caught.
( 'iss-nbt-falsified-entry',
  'the record he doctored',
  array[
    'the record keeps the seventh was not spared',
    'he wrote the seventh a mercy it was not',
    'the falsified entry the seventh was cast out'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'seventh-shrine',
    'set_flags', jsonb_build_object('iss_entry_corrected', true)
  ),
  'url_token', 4, false, null ),

-- iss-bound-word-callback (§7.3) — re-submit the earned bound word in a NEW context at the
-- deep gate. answer_kind 'phrase' (a callback — a re-submitted earned phrase). The existing
-- bound-word / m4-three-hands chain already owns the literal `the one who turned away`
-- re-submission (OVERHAUL §5 prefer-unsolved); to avoid a THIRD open owner of that exact
-- string shadowing the catch, this row carries its OWN distinct callback readings that name
-- the same act. main_beat → opens the deep gate toward the Threshold vault. Gated on
-- bound_word_known (the M4 chain must have yielded the word). active=false → lit at
-- bound_word_known. This is the one genuinely-sequential deep gate (keeps a hard gate).
( 'iss-bound-word-callback',
  'speak again the one who turned away',
  array[
    'speak again the one who turned away',
    'the bound word binds the deep gate',
    'the name that binds the deep'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('deep_gate_open', true),
    'next_puzzle_key', 'spine-threshold-vault',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'the_threshold',
      'priority', 13,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  'phrase', 4, false, null ),

-- ───────────────────────────────────────────────────────────────────────────
-- 8. CROSS-KEEPER / SPINE (external / co-op vault / Observer / meta-acrostic)
-- ───────────────────────────────────────────────────────────────────────────

-- spine-recovered-archive (§8.1) — a carved string resolves to an unlisted Drive folder;
-- an image inside hides a name in its audio spectrogram. answer_kind 'phrase': the hidden
-- spectrogram name. lore + a Whisper-budget grant. Typed. OPTIONAL external surface — the
-- spine never depends on it (INV-12). No site (external → web).
( 'spine-recovered-archive',
  'the salvaged archive',
  array[
    'the name the spectrogram keeps',
    'the recovered name in the waveform',
    'the seventh adjacent name off the record'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'set_flags', jsonb_build_object('recovered_archive_read', true, 'whisper_budget_earned', true),
    'voice_args', jsonb_build_object(
      'fragment', 'what was recovered is kept off the record. the image is a waveform. read it as a spectrogram and it holds a name the record would not.'
    )
  ),
  'phrase', 3, true, null ),

-- spine-threshold-vault (§8.2) — an asymmetric co-op vault (vanilla 1.21 trial-chamber
-- per-player keys). Each active player is shown a different rune fragment via per-player
-- showEntity; read aloud + combined they assemble the code (fragments partitioned over the
-- active roster at solve time). answer_kind 'code'; opaque token on the assembled
-- combination. main_beat → the Threshold opens; the true-walk on-ramp is cut on the inner
-- lintel. Convergence beat — NEEDS QUORUM (effectiveQuorum) + a PLUGIN PRODUCER (per-player
-- illusion + vault). Gated on deep_gate_open. active=false → lit at deep_gate_open.
( 'spine-threshold-vault',
  'the asymmetric co-op vault',
  array[
    'v8k3 mq2n x6w1 t4d9 c7s5 assembled'
  ],
  'main_beat',
  jsonb_build_object(
    'voice_key', 'oracleMainBeat',
    'set_flags', jsonb_build_object('threshold_vault_open', true),
    'next_puzzle_key', 'true-walk-arrive',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'threshold_vault',
      'priority', 13,
      'payload', jsonb_build_object(
        'step', 'door_open',
        'step_payload', jsonb_build_object('radius', 3, 'open', true)
      )
    )
  ),
  'code', 4, false, null ),

-- spine-spoken-name (§8.3) — once a player SAYS the catch's truth aloud in voice chat, the
-- Observer Engine (Whisper) hears it and the Watcher quotes it back (posted to #the-record).
-- answer_kind 'spoken'; opaque token the Observer transcript scan posts on the REAL spoken
-- phrase (grounding discipline — never fabricated; degrades to silence if the voice layer is
-- absent). lore (a bonus "it knows" beat) — GATES NOTHING. Gated on iss_caught (the truth
-- must be known first). PRODUCER (W5): discord/src/voice/spoken-name.ts — the voice tier
-- detects "the one who turned away" in a transcript and submits this token via the oracle.
-- active=false here → lit at iss_caught (metapuzzle_seed flips active=true + adds requires_flags).
( 'spine-spoken-name',
  'the watcher quotes you back',
  array[
    'q5k8 mq3w x1n7 t2d6 heard aloud'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'you said it aloud, and it was heard. before the hour was out the words were cut where you would pass. it kept what you spoke.'
    )
  ),
  'spoken', 4, false, null ),

-- spine-unkept-acrostic (§8.4) — the six maker's-mark glyphs (one per stone) read in
-- FALL-ORDER spell UNKEPT. answer_kind 'phrase'. NOTE: the existing meta-unkept row already
-- owns the bare `unkept` answer (gated on iss_caught); to avoid a same-string collision,
-- this row carries its own distinct phrasing that names the read. lore — GATES NOTHING.
-- Gated on iss_caught (the catch hands the fall-order key). Typed. active=false → lit at
-- iss_caught. max_attempts capped (a short dictionary phrase).
( 'spine-unkept-acrostic',
  'the six marks spell one word',
  array[
    'the six marks spell unkept',
    'read in fall order the word is unkept',
    'the word each keeper did not keep'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'six marks, one to a stone, read in the order they fell. they were cut as a set, a commission, not a graveyard. the word is the one none of them kept.'
    )
  ),
  'phrase', 4, false, 8 ),

-- spine-cold-hearth-shadow (§8.5) — at the dead-shrine, notice the one thing wrong: the
-- hearth is cold all through, the only fire in the Hold let go out (an optional F3-as-
-- instrument layer confirms ash, not fire). answer_kind 'phrase'. dead_end WITH TEETH (the
-- false walk — the surface answers Iss, the deep answers the Seventh); it yields the
-- QUESTION, not progress. Typed. Reuses the 'place' dead-end kind (a real place that keeps
-- no road on). Sited at the cold hearth.
( 'spine-cold-hearth-shadow',
  'the shrine that is only cold',
  array[
    'the only fire let go out',
    'a cold hearth',
    'the fire was not kept'
  ],
  'dead_end',
  jsonb_build_object(
    'voice_key', 'oracleDeadEnd',
    'voice_args', jsonb_build_object('kind', 'place')
  ),
  'phrase', 2, true, null )

on conflict (puzzle_key) do update set
  title            = excluded.title,
  accepted_answers = excluded.accepted_answers,
  outcome_type     = excluded.outcome_type,
  outcome_payload  = excluded.outcome_payload,
  answer_kind      = excluded.answer_kind,
  movement         = excluded.movement,
  active           = excluded.active,
  max_attempts     = excluded.max_attempts;

commit;
