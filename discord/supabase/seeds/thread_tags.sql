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
