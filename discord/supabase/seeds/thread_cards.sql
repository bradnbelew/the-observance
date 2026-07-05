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
    '{}', 'undercroft-descent', null, 10 ),

  ( 'who-vaun-counted', 'who', 'the founder who counted', 'cardWhoVaunCounted',
    'stone_vaun', 'explore',
    array['who-mara-read','human-offering-ledger'], 'stone-vaun', null, 20 ),

  ( 'who-mara-read', 'who', 'the lampwright who read', 'cardWhoMaraRead',
    'stone_mara', 'explore',
    array['who-vaun-counted','who-sella-token'], 'stone-mara', null, 30 ),

  -- Mara's second clue (W9 completeness): the rite she annotated but never walked — revealed when the
  -- group WALKS it (mara-walk-the-map), which also gives that expansion puzzle its archive payoff. Gives
  -- Mara the >=3-clue web the design bar asks for (who-mara-read + this + the marker she read).
  ( 'happened-mara-unwalked', 'happened', 'the rite she read but never walked', 'cardHappenedMaraUnwalked',
    'stone_mara', 'explore',
    array['who-mara-read'], 'mara-walk-the-map', null, 35 ),

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
    array['place-deeper-wrong'], 'rosetta-ring', null, 10 ),

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
    array['who-vaun-counted','human-offering-ledger'], 'stone-vaun', null, 50 ),

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
    array['place-deep-line','surface-pell-truth'], 'm1-record-opens', null, 10 ),

  ( 'surface-wenna-folk', 'surface', 'seven somethings', 'cardSurfaceWennaFolk',
    'first_report_lectern_01', 'rumor',
    array['place-seven-ways','surface-seventh-marker'], 'rosetta-ring', null, 20 ),

  ( 'surface-pell-truth', 'surface', 'it does not chase', 'cardSurfacePellTruth',
    'first_report_lectern_01', 'explore',
    array['surface-watcher-counts','surface-aro-lie'], 'm1-named-habit', null, 30 ),

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
    array['human-names-over-heads','who-deep-market'], 'undercroft-fog', null, 30 ),

  ( 'human-hand-as-lamp', 'human', 'entry five', 'cardHumanHandAsLamp',
    'kept_light_home_01', 'explore',
    array['human-lamp-roll-counts-down','human-they-were-kept'], 'undercroft-fog', null, 40 ),

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
    '{}', null, 'flag:count_done', 100 ),

  ( 'gather-count-happened', 'happened', 'the count predates you', 'cardGatherCountHappened',
    'unbroken_light', 'verified',
    '{}', null, 'flag:count_done', 100 ),

  ( 'gather-unlight-surface', 'surface', 'the same hands, the last winter', 'cardGatherUnlightSurface',
    'unbroken_light', 'verified',
    '{}', null, 'flag:unlight_done', 100 ),

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

  -- the lad (letters L08a/L08b) — the eighth's one human cost. He obeyed the covering all
  -- winter, asked the one question that names the catch itself (a true way answers when
  -- broken; a covering answers never), and is answered with silence — read, at the time, as
  -- the wall holding. Revealed post-catch (2026-07-05 audit): every other planted thread in
  -- the corpus has a traceable payoff card; this was the one exception, a fully-written
  -- figure whose fate had nowhere left to go. References the same forged board + the catch
  -- that retroactively names his silence for what it was.
  ( 'human-lad-unanswered', 'human', 'no reply is filed', 'cardHumanLadUnanswered',
    'stone_iss', 'explore',
    array['surface-eighth-forged','happened-no-wall'], 'no-wall-catch', null, 81 ),

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
    array['surface-watcher-counts','human-the-record-opens'], null, 'companion:revealed', 130 ),

  -- ===== THE COUNT OF THE OPENINGS (W3c — six-were-kept-before-you.md) =====
  -- The 6-prior-groups slow-burn, surfaced as three human-thread cards that ACCRETE across late solves:
  -- the count that will not come out even (Brann, the counter) → the six openings + the struck seventh
  -- (post-catch, once the record is understood to keep people) → "you are the next" (when the Seventh is
  -- named). Each rhymes into the human spine (the open column that fills, the surplus that is kept) and,
  -- via the struck seventh, cross-references Sella's uncounted seventh. Spoiler-safe: surface facts +
  -- the induction frame the human thread already approaches — no fate, no key, no name, revealed only on
  -- earned late solves. Bodies are Watcher-register (voice.archive.ts cardHumanCount* / cardHumanYouAreNext).
  ( 'human-count-uneven', 'human', 'the count does not come out even', 'cardHumanCountUneven',
    'first_report_lectern_01', 'explore',
    array['human-names-over-heads','human-the-record-opens'], 'stone-brann', null, 200 ),
  ( 'human-six-openings', 'human', 'six openings, one struck', 'cardHumanSixOpenings',
    'first_report_lectern_01', 'explore',
    array['human-count-uneven','human-the-record-opens','surface-seventh-marker'], 'no-wall-catch', null, 210 ),
  ( 'human-you-are-the-next', 'human', 'you are the next', 'cardHumanYouAreNext',
    'first_report_lectern_01', 'explore',
    array['human-six-openings','human-the-record-opens','human-they-were-kept'], 'seventh-unwriting', null, 220 )

on conflict (card_key) do nothing;

commit;
