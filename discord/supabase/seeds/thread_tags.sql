-- The Observance — thread_tags.sql
-- Tags each seeded puzzle node (the original 24 + the WEB-realization/backlog rows + the 20 diverse-
-- expansion rows = 44 total) to a reconstruction thread (which question its PAYOFF advances) and, where
-- the node teaches/hinges on a way, a teaches_custom (a canon CUSTOM_KEYS the_-prefixed key). Content +
-- rationale: design/content/thread-tagging.md (deliberately lopsided by content, not a 1:1 pattern —
-- the_bow is taught most; ALL seven ways are now taught by ≥1 node, the_sacred_beast via Sella's
-- deep-bird memorial in the expansion block below; many nodes teach no way, NULL).
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

-- ===========================================================================
-- THE DIVERSE EXPANSION (design/PUZZLE-DESIGNS.md) — tags for the 20 expansion rows
-- (the second puzzles_seed.sql insert). These were previously UNTAGGED (thread_key /
-- teaches_custom silently NULL), so their solves did not cluster in the record's
-- reconstruction (integrity.ts reads puzzles.thread_key). Tagged by CONTENT (which
-- question the payoff advances + which way, if any, the solve turns on), lopsided by
-- design (thread-tagging.md) — not a 1:1 pattern. NOTE: sella-shore-memorial teaches
-- `the_sacred_beast` (Sella's kept deep-bird) — the ONE way previously taught by no node,
-- now learned here. Every thread_key ∈ THREADS (FK), every non-null teaches_custom ∈
-- CUSTOM_KEYS (threadTagSelfTest).
-- ===========================================================================

-- Vaun — the offering he never made (object/code; his personal, human failure)
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'vaun-hoard-sorted';
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_offering'    where puzzle_key = 'vaun-bookshelf-tally';

-- Mara — read and never walked (the walk she could not make = the group's bow)
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'mara-lectern-lock';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_bow'         where puzzle_key = 'mara-walk-the-map';

-- Sella — the drowned child (the far water, the shore she drew, the deep-bird she kept)
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'sella-reflection-bearing';
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'sella-overlay-lake';
update public.puzzles set thread_key = 'human',    teaches_custom = 'the_sacred_beast' where puzzle_key = 'sella-shore-memorial';

-- Orin — the mason who would not bow (his identity; the one offering he kept in secret)
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_bow'         where puzzle_key = 'orin-bow-fall-order';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'orin-banner-heraldry';
update public.puzzles set thread_key = 'who',      teaches_custom = 'the_offering'    where puzzle_key = 'orin-frame-dials';

-- Brann — the watchman on the black moon (his night watch; passing his walk in silence)
update public.puzzles set thread_key = 'surface',  teaches_custom = 'the_dark_hours'  where puzzle_key = 'brann-black-moon-toll';
update public.puzzles set thread_key = 'surface',  teaches_custom = 'the_unspoken'    where puzzle_key = 'brann-silence-corridor';

-- Iss — the liar (the deduction that catches him; his doctored record of the Seventh)
update public.puzzles set thread_key = 'happened', teaches_custom = null              where puzzle_key = 'iss-which-is-true';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'iss-nbt-falsified-entry';
update public.puzzles set thread_key = 'happened', teaches_custom = 'the_deep_line'   where puzzle_key = 'iss-bound-word-callback';

-- Cross-keeper / spine (the record kept elsewhere · the co-op threshold · it heard you ·
-- the six marks · the one cold hearth)
update public.puzzles set thread_key = 'surface',  teaches_custom = null              where puzzle_key = 'spine-recovered-archive';
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'spine-threshold-vault';
update public.puzzles set thread_key = 'human',    teaches_custom = null              where puzzle_key = 'spine-spoken-name';
update public.puzzles set thread_key = 'who',      teaches_custom = null              where puzzle_key = 'spine-unkept-acrostic';
update public.puzzles set thread_key = 'place',    teaches_custom = null              where puzzle_key = 'spine-cold-hearth-shadow';

commit;
