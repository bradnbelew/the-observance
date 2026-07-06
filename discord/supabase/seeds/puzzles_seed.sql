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
    'set_flags', jsonb_build_object('mara_read', true, 'descent_read', true)
  ),
  2, true, null ),

-- stone-sella — DEACTIVATED (puzzle-variety audit): this Atbash letter-cipher is
-- redundant with its own better replacement, sella-reflection-bearing (environmental,
-- no letter-reversal) — and cipher-plaintexts.md's own "LATER DRIFT" note says Sella's
-- later marks resolve into drawings, not words, which cuts against keeping a letter-
-- cipher for her at all. Following the stone-brann-cipher convention exactly: the row
-- is deactivated, not deleted, so history is preserved. Its seventh_suspected flag-set
-- (the sole gate the whole Seventh side-quest thread — seventh-shrine, seventh-unwriting,
-- seventh-cause — waits on) MOVES to sella-reflection-bearing below so the thread does
-- not go dark; see that row's set_flags.
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
  2, false, null ),

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
    'next_puzzle_key', 'orin-threshold',
    'set_flags', jsonb_build_object('orin_stone_read', true)
  ),
  -- max_attempts:8 - includes a short fallback answer ("threshold"), so cap the
  -- per-minute per-player tries without changing the normal solve path.
  2, true, 8 ),

-- stone-brann — NOW the carved FRAMING for the rail-fence cipher (puzzle-variety audit
-- fix). Previously shipped as pure inert lore, explicitly commented "do not forge it"
-- (stone-brann-cipher had no CLUE_SPECS entry). That comment is no longer true: Brann's
-- real cipher is live at stone-brann-cipher (railfence, rails=9), and this stone is now
-- its carved framing panel — the plain-script header/footer a reader turns to BEFORE
-- the ciphered run, in Brann's own voice (doubles a clause, re-counts, the number slips
-- — his exact fingerprint per journals-orin-brann-iss.md "REPEATS AND OVER-CORRECTS").
-- The framing hands the rail count (nine) the same way Vaun's threes hand shift-3: by
-- being said, miscounted, and said again. FACT 11 (one fire never out) + FACT 12 (same
-- word, people/flame/stone) both survive in the footer lines.
( 'stone-brann',
  'do not close your eyes here',
  array[
    'one fire was never doused',
    'do not close your eyes here',
    'the one fire that will not be doused',
    'nine lit i counted nine'
  ],
  'lore',
  jsonb_build_object(
    'voice_key', 'oracleLore',
    'voice_args', jsonb_build_object(
      'fragment', 'nine lit. i counted nine. the count was lower the first time and i think the second count is the true one. count the fires before you sleep, and count them again, because the pass before is always more. one of them never went out, and no hand tends it. they had one word for the people and the flame and the cold stone. do not close your eyes here.'
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

-- iss-warm — REPLACES A FAKED CIPHER (puzzle-variety audit). Previously this row's
-- accepted answer was just a re-stated carved phrase ("the ways are a wall against the
-- watching") with NO forge spec behind it — a seeded phrase dressed as a second decode
-- when it was really just quoting the framing back. It now has a REAL second decode: the
-- SAME carved wall-stone carries a second, legitimate reading — an ACROSTIC (read the
-- first letter of each of Iss's six warm lines, top to bottom) — verifiable by hand, no
-- forge/DB needed, and using a method already canonical for Iss (seventh-reading.ts's
-- ISS_ACROSTIC_LINES uses the identical technique for his capstone fragment). The full
-- worked cipher (all six carved lines + the acrostic) is written out in
-- arc/corpus/cipher-plaintexts.md under "stone-iss-wall — the second reading (acrostic)".
-- Read WARM (trustingly, line by line) the stone still comforts and routes to the dead
-- shrine (the false lead is unchanged — iss_trusted still fires). Read COLD (the acrostic,
-- first letter of each line, down) it spells "no wall" — the same correction stone-iss-wall's
-- Vigenère name-as-key yields, reached by an entirely different, independently-verifiable
-- method. This is the "warm account vs. the land" deduction made literal IN THE CARVING
-- itself, not just cross-document. Accepts BOTH the acrostic answer and the (unchanged)
-- warm phrase, so a group that reads it either way is heard; next_puzzle_key + set_flags
-- (routing to the dead shrine) are unchanged.
( 'iss-warm',
  'the warm reading',
  array[
    'the ways are a wall against the watching',
    'no wall',
    'read down the first letter of each warm line'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'iss-dead-shrine',
    'set_flags', jsonb_build_object('iss_trusted', true)
  ),
  -- max_attempts:6 - the cold acrostic accepts "no wall"; protect the short form.
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
  -- max_attempts:8 - this side branch accepts "seven"/"7"; cap guessing pressure.
  3, true, 8 ),

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
-- NO site_id (the solver may be anywhere at fire time; backlog-unlockbeat-producers R-C
-- flagged this row as lacking a real world anchor and needing either a site_id or a
-- downgrade). Downgraded reveal -> private_message: this moment has no physical slot to
-- flip, so it is delivered as a private acknowledgement (same pattern as no-wall-catch's
-- iss.dialogue.turns_cold cold-flip) via the atonement.refrain.returned archive key
-- (voice.archive.ts), resolved by resolve.ts's resolvePrivateMessageKey.
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
    'set_flags', jsonb_build_object('atonement_made', true, 'accepting_onramp_open', true),
    'next_puzzle_key', 'rite-tokens',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'priority', 12,
      'payload', jsonb_build_object(
        'step', 'private_message',
        'step_payload', jsonb_build_object('key', 'atonement.refrain.returned')
      )
    )
  ),
  4, true, null ),

-- ===========================================================================
-- MOVEMENT V — The Accepting
-- ===========================================================================

-- rite-tokens — lay one personal token in each of six slots + the named components.
-- main_beat. FACT 13 (the missing tool is YOU). → accepting-crouch / pressure-glyph-walk.
-- REVEAL FIX (was a dead {"slots":6,"lit":true} shape RevealBeat never read — no-cells no-op).
-- unbrokenLight()'s real last-lamps ring (StructureTemplates.java:879-888) places only 4 posts
-- at (dx,dz) in {(0,-4),(4,0),(0,4),(-4,0)}: three are already-lit soul lanterns at dy+1 (i=0..2,
-- hangingLantern(...,true)), and the fourth (i==3, dx=-4,dz=0) is deliberately left "capped" —
-- DEEPSLATE_TILES at dy+1, no lantern at all ("the seventh place: post capped, lamp doused",
-- line 884) — the one open/unlit seat the design calls out by name. There is no 6-lightable-slot
-- altar anywhere in the built structure (Lantern is not Lightable in Bukkit either, so a `lit`
-- cell on the other three would silently no-op) — the ONE real, earned flip here is that capped
-- seventh seat finally taking its lamp: a `set` cell swapping the cap to a standing lit
-- SOUL_LANTERN, at the exact (dx,dy,dz) the cap occupies.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', -4, 'dy', 1, 'dz', 0, 'kind', 'set', 'material', 'SOUL_LANTERN')
          )
        )
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
-- "obey" it and get silence; the proof of the lie is the reliable absence of a toll.
( 'forged-eighth',
  'the covering of the hands',
  array[
    'the eighth is the covering of the hands',
    'cover and be counted clean',
    'to cover ones own',
    'the founders set the ways and did not finish the count'
  ],
  'dead_end',
  -- 'known' kind — a true reading of a real carving that opens no way (it is a forgery;
  -- the land never measures it). The M4 record correction (archiveEighthCorrection) names
  -- it added-not-found; until then the Watcher only flatly declines to enforce it.
  jsonb_build_object('voice_key', 'oracleDeadEnd', 'voice_args', jsonb_build_object('kind', 'known')),
  2, true, null ),

-- prophet-wall-comfort — Iss with a pulpit (B2). A WIDE, not tall set of warm promises,
-- each a true-but-empty substitution solve that opens no road. dead_end 'prophet' kind.
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
-- REVEAL FIX (was a dead {"fragment":"seventh_name_unsealed"} shape). unwriting()'s hearth-stone
-- (StructureTemplates.java:1016-1019) places a real CAMPFIRE at (dx=-2,dy=1,dz=2) built UNLIT
-- (`pen.campfire(cx-2, cy+1, cz+2, false)` — "the hearth, never lit"). Campfire is Lightable in
-- Bukkit, so this is a genuine earned flip: the name being unsealed ("below the cold hearth...
-- the seal is a name") is realized as the cold hearth finally taking flame.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', -2, 'dy', 1, 'dz', 2, 'kind', 'lit', 'lit', true)
          )
        )
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
-- REVEAL FIX (was a dead {"fragment":"seventh_choice_marked"} shape). unwriting()'s deposit-slot
-- (StructureTemplates.java:1016,1020-1022, distinct from seventh-unwriting's hearth above) is a
-- CHISELED_POLISHED_BLACKSTONE backing block at (dx=2,dy=0,dz=1) beside the empty offering-cut —
-- "the empty deposit-slot (an offering never received)". This row's own comment ties the deposit
-- directly to the choice ("the deposit (restore) is ALSO the INHERITORS codicil"), so the mark
-- here is the once-waiting deposit backing finally reading as closed/resolved: swapped to plain
-- POLISHED_BLACKSTONE_BRICKS (the same material already used for the slot's front face at
-- dz=2), converging the ornamental "waiting" chisel into the settled brick once the choice lands.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', 2, 'dy', 0, 'dz', 1, 'kind', 'set', 'material', 'POLISHED_BLACKSTONE_BRICKS')
          )
        )
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
  -- max_attempts:8 - a single name should be solved by the six fragments, not guessed.
  5, true, 8 ),

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
-- distinct people (active-only): foot on the plate + a carve + a Discord post while the
-- square is awake. CoopPlateListener publishes the held world-legs marker; the Discord
-- closer posts the opaque conjunction token when the word arrives in the forgiving window.
-- The token below is what the closer posts on a CLEARED gate — opaque,
-- wordless, plugin-only (no-leaked-sentinel). Clearing opens the Threshold (threshold_open),
-- whose carving yields the TRUE coordinate (NOT yielded here — sequenced, §0.4). main_beat.
( 'm4-three-hands',
  'three hands held',
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
-- REVEAL FIX (was a dead {"fragment":"destination_leaves_read"} shape). threshold()'s tableau
-- (StructureTemplates.java:952-958) never places literal `_LEAVES` blocks — BUILD-MANIFEST
-- always slated "destination leaves" as a sign/lectern rewrite (SignWriteBeat/LecternFillBeat),
-- which RevealBeat is not. The one real, standalone site-mark block the template DOES place is
-- the bare POLISHED_BLACKSTONE_BRICKS at (dx=0,dy=0,dz=2), directly over the grave and right
-- before the date-notation label sign (cz+3) — a placeholder marker with no other role. Opening
-- it to AIR is the canonical "open the way" flip (RevealBeat javadoc) and reads as the tableau
-- clearing so the destination carving is finally legible, grounded in a block the build actually
-- places (not invented geometry).
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
    'set_flags', jsonb_build_object('true_destination_reached', true, 'accepting_onramp_open', true),
    'next_puzzle_key', 'rite-tokens',
    'beat', jsonb_build_object(
      'type', 'unlock',
      'mc_uuid', '{solver}',
      'site_id', 'the_threshold',
      'priority', 13,
      'payload', jsonb_build_object(
        'step', 'reveal',
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', 0, 'dy', 0, 'dz', 2, 'kind', 'set', 'material', 'AIR')
          )
        )
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
-- observation/lore nodes, registered in NON_CIPHER_KEYS. stone-brann-cipher
-- (puzzle-variety audit fix) is now ACTIVE and has a real railFence CLUE_SPECS
-- entry (clue-specs.ts, rails=9) — no longer staged/exempt.
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
-- D2 — the SIXTH keeper-stone expedition: Brann, read-by-time (puzzle-variety audit
-- fix — ACTIVATED). backlog-keeper-stone-expeditions §1.4. This is Brann's real cipher
-- node — railFence (rails = 9, the fire-count Brann himself names — "nine lit one out"),
-- the verb is read-by-time (the carving rakes visible only by the lit beacon-glow after
-- dark). The bound plaintext "count the fires before you sleep" round-trips under
-- specsSelfTest via the CLUE_SPECS railFence entry now registered in clue-specs.ts.
-- stone-brann (the flat-lore stone) is a SEPARATE row and stays non-cipher lore; only
-- its inscription text changed (see that row, above in MOVEMENT II) to set up this cipher
-- in-world. next_clue → the descent (a second in-road to undercroft-descent, the web
-- rule). The rail-key is day-fair: the fire-count is also countable in daylight
-- (backlog §R-3), only the READING is night-gated.
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
    'set_flags', jsonb_build_object('brann_read', true, 'descent_read', true),
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
  2, true, null ),

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
-- cache (his Caesar stone becomes readable behind it). PRODUCER: HoardSortedListener.java
-- (confirmed world-inert — it only posts the oracle token on deposit, never mutates a block,
-- so this reveal is the ONLY real world change the offering earns).
-- REVEAL FIX (was a dead {"fragment":"vaun_cache_opened"} shape). vaun()'s hoard containers
-- (StructureTemplates.java:332-336, CHEST/TRAPPED_CHEST/BARRELs) are live inventory blocks —
-- flipping their material via RevealBeat's `set` would delete their contents, so they are not
-- safe reveal targets. The genuine non-container "wrongness" prop the same method places
-- (line 342) — "a cracked pot on the floor... the hoard is failing" — is the honest target:
-- CRACKED_STONE_BRICKS at (dx=1,dy=0,dz=2), beside the plain "chipped pot" at (dx=0,dy=0,dz=2).
-- The cache opening is realized as that failing-hoard tell resolving: cracked brick made whole
-- (STONE_BRICKS), the wrongness undone once the first-of-the-deep is finally given back.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', 1, 'dy', 0, 'dz', 2, 'kind', 'set', 'material', 'STONE_BRICKS')
          )
        )
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
-- REVEAL FIX (was a dead {"fragment":"mara_written_whole"} shape). first_marker_01 is placed by
-- /observance placeprologue's SmallStructureBeat (ObservanceCommand.java:961-968), NOT a
-- StructureTemplates.keeper() dispatch — its only two real blocks are a CHISELED_STONE_BRICKS
-- at (0,0,0) and a CANDLE at (0,1,0) (the same "small structure" payload cited there, ground
-- truth for the site). The candle is the one real Lightable object here, and Mara's own motif
-- established throughout her build (mara(), StructureTemplates.java:410-411: "i can't keep them
-- all lit — the words stay when the lamps do not") is precisely light enabling reading — so
-- "the record writes Mara whole" is realized as this candle finally catching.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', 0, 'dy', 1, 'dz', 0, 'kind', 'lit', 'lit', true)
          )
        )
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
-- Also now the sole producer of seventh_suspected (moved off the deactivated
-- stone-sella above, puzzle-variety audit) — the Seventh side-quest thread
-- (seventh-shrine, seventh-unwriting, seventh-cause) still gates on this same flag key.
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
    'set_flags', jsonb_build_object('sella_bearing_read', true, 'seventh_suspected', true)
  ),
  'coords', 2, true, null ),

-- sella-overlay-lake — RETHEMED (puzzle-variety audit): was "look at something → get a
-- destination word → walk there → read a sign," the same template as its neighbors
-- sella-reflection-bearing (before it) and sella-shore-memorial (after it) — three in a
-- row, the clearest back-to-back template repeat in the whole puzzle set (PUZZLES.md §0).
-- Converted to a numeral/positional lock, REUSING the exact producer/answer_kind pairing
-- already proven live at mara-lectern-lock (LecternLockListener.java, answer_kind 'code')
-- — no new mechanic type invented, no new Java. Re-themed to Sella's water/reflection
-- motif rather than Mara's reading motif: at the shore pool, her copybook's ring-drawings
-- (concentric ripples she drew fanning out from a dropped stone, each ring numbered in her
-- own hand — a child's tally, not a rite) give five page-numbers; five lecterns holding the
-- OTHER half of her copybook (loose pages, not the six-book Kept-Light shelf — a different
-- physical shelf, hers) must be turned to those five pages. The comparator circuit behind
-- the shore-pool alcove completes only when all five match — same water-logic verb as her
-- other puzzles (count the rings, not read them), so the axis that changes is TYPE/VERB/
-- answer_kind while the SURFACE (in-world, shore pool) stays hers. answer_kind 'code'; the
-- lock listener reads the five-lectern comparator line (mirrors mara-lectern-lock's rig
-- exactly). next_clue → sella-shore-memorial (unchanged spine). Gated on sella_bearing_read
-- (unchanged). active=false → lit at sella_bearing_read by the metapuzzle activation lane
-- (unchanged — the gate + downstream flag sella_overlay_read are untouched so
-- sella-shore-memorial's own gate does not need to move).
( 'sella-overlay-lake',
  'count the rings she drew',
  array[
    's3k9 vq2m x7d4 p1n6 the rings she counted'
  ],
  'next_clue',
  jsonb_build_object(
    'voice_key', 'oracleNextClue',
    'next_puzzle_key', 'sella-shore-memorial',
    'set_flags', jsonb_build_object('sella_overlay_read', true)
  ),
  'code', 3, false, null ),

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
-- REVEAL FIX (was a dead {"fragment":"orin_threshold_readable"} shape). Same first_marker_01
-- ground truth as mara-walk-the-map above (ObservanceCommand.java:961-968: one
-- CHISELED_STONE_BRICKS at (0,0,0), one CANDLE at (0,1,0)) — the candle is claimed by Mara's
-- reveal (her lamp/lit-reading motif), so this row's distinct flip is the stone itself: a
-- `set` swap to CHISELED_DEEPSLATE, the exact material Orin's own threshold lintel is built
-- from (orin(), StructureTemplates.java:543: "the lintel block, dead centre"). His
-- threshold-stone "becoming readable" is realized as the plain marker taking his own
-- carved-deepslate signature.
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
        'step_payload', jsonb_build_object(
          'cells', jsonb_build_array(
            jsonb_build_object('dx', 0, 'dy', 0, 'dz', 0, 'kind', 'set', 'material', 'CHISELED_DEEPSLATE')
          )
        )
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
  -- max_attempts:6 - "awake" is a short temporal answer; cap brute force after the toll.
  'phrase', 2, true, 6 ),

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
