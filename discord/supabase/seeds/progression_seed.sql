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
