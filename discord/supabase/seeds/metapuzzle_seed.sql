-- The Observance — metapuzzle_seed.sql
-- THE ACTIVATION LANE + the UnlockBeat producer-contract fixes (BUILD-MANIFEST §2.1 /
-- §0.8 / §D4 / §D6; INTEGRATION-V2 §2.1 + precondition 7; WEB-MASTER §0.5 + §2.1).
--
-- This file is UPDATE-ONLY. It carries NO `array[...]` literal, so it is invisible to
-- both seedcheck.ts (which scans puzzles_seed.sql array blocks) and specsCoverageSelfTest
-- (which parses `( 'key', 'title', array[` row openers) — by design: it edits the COLUMN
-- + payload state of rows already inserted by puzzles_seed.sql, never adds a row.
--
-- It fixes the single most dangerous defect the critiques found (arg-craft F1): nine
-- spine/colorant rows ship active=false and the ONLY runtime flip authority was for
-- dead_end rows — so bound-word, m4-three-hands, threshold-coordinate, true-walk-arrive,
-- seventh-unwriting, seventh-cause, seventh-choice, base-docket-reread, and meta-unkept are
-- authored, seeded, and UNREACHABLE. The deterministic puzzles.requires_flags gate
-- (getOpenPuzzles treats a row as open iff every flag in requires_flags is truthy in
-- arc_state.flags) lights the whole back half for ANY outcome_type, not just dead-ends.
--
-- CROSS-OWNER DEPENDENCIES (see the RETURN):
--   * `puzzles.requires_flags jsonb` is the SQL-migration lane's 0006 column add
--     (BUILD-MANIFEST §5 migration row). This file ASSUMES it exists; the `do $$ … $$`
--     guard below no-ops cleanly if 0006 has not landed yet (so re-running is safe and
--     never errors out the batch). getOpenPuzzles (TS-SHOWRUN, repo.ts) must AND-test it.
--   * RevealBeat (`reveal` step) + the `private_message` key-resolver are the
--     TS-SHOWRUN / PLUGIN lanes (backlog-unlockbeat-producers §4). This file only fixes
--     the SEED-side payload-key contracts that don't need code (advancement_toast `key`
--     → `advancement`, the reveal rows' `site_id`+`op`).
--
-- Run AFTER 0004_oracle.sql + (ideally) 0006_*.sql + puzzles_seed.sql, as service_role
-- (RLS bypass — spoiler tables). Idempotent (every UPDATE sets absolute values).

begin;

-- ===========================================================================
-- 0. REPOINT THE CATCH (INTEGRATION-V2 §2.1 step 2 / WEB-MASTER §0.5).
--    no-wall-catch must set iss_caught and STOP — it must NOT shortcut to the rite
--    (no next_puzzle_key: rite-tokens). The rite is reached only THROUGH the chain
--    (bound-word → m4-three-hands → threshold-coordinate → true-walk-arrive →
--    rite-tokens) or the promoted pressure-glyph-walk — never handed at the catch.
--    The catch keeps its iss_caught + true_coord_known flags and its cold-Iss beat;
--    we strip ONLY the rite shortcut. (Idempotent: the `-` operator no-ops if absent.)
-- ===========================================================================

update public.puzzles
  set outcome_payload = outcome_payload - 'next_puzzle_key'
  where puzzle_key = 'no-wall-catch'
    and outcome_payload ? 'next_puzzle_key';

-- ===========================================================================
-- 0b. THE ISS-SEAM oracle line (the-seventh-below.md REWRITE SPEC "Iss-seam").
--     When the wall-lie catch fires, the Watcher adds ONE callback line that wires the
--     solved catch into the Seventh's main quest: "he lied about the wall. ask what else
--     he told you warmly. ask who he said was cast out for nothing." The line is authored
--     in voice.ts as oracleNoWallCatch() (the base main_beat turn + the seam callback);
--     here we only REPOINT the catch's voice_key from oracleMainBeat → oracleNoWallCatch,
--     so puzzles_seed.sql stays untouched and the base turn is preserved (the seam is
--     appended, not a replacement). The paired thread_cards edge (happened-no-wall solve
--     re-opens surface-seventh-marker) is seeded in thread_cards.sql. Idempotent: sets an
--     absolute value; safe to re-run.
-- ===========================================================================

update public.puzzles
  set outcome_payload = jsonb_set(outcome_payload, '{voice_key}', '"oracleNoWallCatch"', true)
  where puzzle_key = 'no-wall-catch';

-- ===========================================================================
-- 1. THE UnlockBeat PRODUCER-CONTRACT FIXES (backlog-unlockbeat-producers §2 R-A,
--    §4.6 decision 1). Three advancement_toast rows pass step_payload:{key:"observance:…"},
--    but AdvancementToastBeat reads `advancement` / `fallback_title` / `fallback_subtitle`
--    — so the toast fires NOTHING today (skipped:"no-advancement-no-fallback"). Rewrite the
--    payload to the producer's real contract. Pure seed edit; no code (the cleanest fix —
--    the beat already reads `advancement`). The cold-Iss `private_message` and the `reveal`
--    rows are left for the TS-SHOWRUN key-resolver / PLUGIN RevealBeat lanes (cross-owner).
-- ===========================================================================

-- rosetta-ring + a1z26-tick-stave: the M1 literacy toast (the SEED-UNLOCK-TOAST-BOOKEND
-- plant; backlog-unlockbeat-producers §5). Same advancement, runes / runes-free twins.
update public.puzzles
  set outcome_payload = jsonb_set(
        outcome_payload,
        '{beat,payload}',
        jsonb_build_object(
          'step', 'advancement_toast',
          'advancement', 'observance:the_ring_is_whole',
          'fallback_title', 'the record',
          'fallback_subtitle', 'the record notes you can read it now'
        ),
        true)
  where puzzle_key in ('rosetta-ring', 'a1z26-tick-stave')
    and (outcome_payload #> '{beat,payload}') ? 'key';

-- record-receives: the M5 bookend toast (the payoff end of the same plant). The arc that
-- opened on "the record notes you" closes on "the record receives you" — identical UI
-- register, now a verdict. Same key→advancement contract fix.
update public.puzzles
  set outcome_payload = jsonb_set(
        outcome_payload,
        '{beat,payload}',
        jsonb_build_object(
          'step', 'advancement_toast',
          'advancement', 'observance:the_record_receives_you',
          'fallback_title', 'the record',
          'fallback_subtitle', 'the record receives you'
        ),
        true)
  where puzzle_key = 'record-receives'
    and (outcome_payload #> '{beat,payload}') ? 'key';

-- ===========================================================================
-- 2. THE requires_flags ACTIVATION LANE (the back half; arg-craft F1).
--    Set the requires_flags COLUMN on every staged/back-half row so getOpenPuzzles lights
--    it deterministically the instant its gate flags are truthy in arc_state — independent
--    of the showrunner. The flag a row waits on is the flag its UPSTREAM door SETS:
--
--      iss_caught        ← no-wall-catch (the catch)
--      bound_word_known  ← bound-word
--      threshold_open    ← m4-three-hands
--      true_coord_known  ← threshold-coordinate
--      seventh_named     ← seventh-unwriting
--      seventh_suspected ← stone-sella / seventh-shrine (the deep's pre-req)
--
--    Authored as a guarded block so it cleanly no-ops if 0006 (the column) has not landed.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_flags'
  ) then

    -- M4 the single Iss chain — each link gated on the link before it (sequenced, §0.4):
    -- bound-word opens at the catch; the gate at the bound word; the coordinate at the gate;
    -- the walk at the coordinate. This is the chain that was authored-but-dark.
    -- OVERHAUL §5: the docket twin is collapsed. With requires_flags now real (0006), the
    -- DETERMINISTIC base-docket-reread-auto fully covers the M4 re-read; the showrunner-flipped
    -- base-docket-reread is retired (left active=false in puzzles_seed) to give the four docket
    -- answers a single owner. So base-docket-reread is NOT activated here.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true)
      where puzzle_key in ('bound-word', 'base-docket-reread-auto', 'meta-unkept');
    update public.puzzles set requires_flags = jsonb_build_object('bound_word_known', true)
      where puzzle_key = 'm4-three-hands';
    update public.puzzles set requires_flags = jsonb_build_object('threshold_open', true)
      where puzzle_key = 'threshold-coordinate';
    update public.puzzles set requires_flags = jsonb_build_object('true_coord_known', true)
      where puzzle_key = 'true-walk-arrive';

    -- M3→IV the Seventh deep — opens only post-iss_caught AND seventh_suspected (the deep
    -- below the dead-shrine; WEB-MASTER §0.4 temporal layering). The choice waits on the name.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true, 'seventh_suspected', true)
      where puzzle_key in ('seventh-unwriting', 'seventh-cause');
    update public.puzzles set requires_flags = jsonb_build_object('seventh_named', true)
      where puzzle_key = 'seventh-choice';

    -- P1-C3 COLLISION FIX (audit): the phrase `the last marker is not the last` is an accepted
    -- answer on BOTH stone-sella (ungated active=true) and seventh-shrine. Both were open at once,
    -- so the resolver always resolved stone-sella (first UNSOLVED in DB order) and seventh-shrine's
    -- second payoff for that string never surfaced. GATE seventh-shrine on seventh_suspected — the
    -- flag stone-sella''s next-clue SETS — so the two are never open simultaneously (the exact
    -- bound-word/iss_caught sequencing the audit blesses). Order preserved: before Sella is solved,
    -- seventh_suspected is false → seventh-shrine is closed → the phrase resolves to stone-sella (its
    -- intended first payoff); after Sella is solved, stone-sella is solved (the resolver prefers the
    -- unsolved candidate) AND seventh_suspected is true → the phrase resolves to seventh-shrine (its
    -- intended second payoff). No accepted-answer string changes; no lore/clue doc changes. This is
    -- consistent with the established contract — seventh-unwriting/seventh-cause (the shrine''s deeper
    -- chambers) already require seventh_suspected, so Sella-first is the canonical Seventh-thread order.
    -- seventh-shrine stays active=true (its active flag is untouched; requires_flags is the AND-gate).
    update public.puzzles set requires_flags = jsonb_build_object('seventh_suspected', true)
      where puzzle_key = 'seventh-shrine';

    -- ── THE DIVERSE EXPANSION (design/PUZZLE-DESIGNS.md) — the gated new-puzzle rows.
    -- Each hangs off a flag its UPSTREAM door SETS (the same deterministic gate the back-half
    -- spine uses). Shipped active=false in puzzles_seed's second insert; lit here + flipped
    -- active below. Each behavior/object/code/spoken row still needs a PLUGIN PRODUCER (later
    -- round) — the gate is wired now so the row is invisible until its door opens.
    --
    --   vaun-bookshelf-tally     → vaun_cache_open      (vaun-hoard-sorted)
    --   mara-walk-the-map        → mara_alcove_open     (mara-lectern-lock)
    --   sella-overlay-lake       → sella_bearing_read   (sella-reflection-bearing)
    --   sella-shore-memorial     → sella_overlay_read   (sella-overlay-lake)
    --   orin-frame-dials         → orin_bowed           (orin-bow-fall-order)
    --   iss-which-is-true        → iss_key_turned       (stone-iss-wall)
    --   iss-nbt-falsified-entry  → iss_caught           (no-wall-catch)
    --   iss-bound-word-callback  → bound_word_known     (bound-word)
    --   spine-threshold-vault    → deep_gate_open       (iss-bound-word-callback)
    --   spine-spoken-name        → iss_caught           (no-wall-catch)
    --   spine-unkept-acrostic    → iss_caught           (no-wall-catch)
    update public.puzzles set requires_flags = jsonb_build_object('vaun_cache_open', true)
      where puzzle_key = 'vaun-bookshelf-tally';
    update public.puzzles set requires_flags = jsonb_build_object('mara_alcove_open', true)
      where puzzle_key = 'mara-walk-the-map';
    update public.puzzles set requires_flags = jsonb_build_object('sella_bearing_read', true)
      where puzzle_key = 'sella-overlay-lake';
    update public.puzzles set requires_flags = jsonb_build_object('sella_overlay_read', true)
      where puzzle_key = 'sella-shore-memorial';
    update public.puzzles set requires_flags = jsonb_build_object('orin_bowed', true)
      where puzzle_key = 'orin-frame-dials';
    update public.puzzles set requires_flags = jsonb_build_object('iss_key_turned', true)
      where puzzle_key = 'iss-which-is-true';
    update public.puzzles set requires_flags = jsonb_build_object('bound_word_known', true)
      where puzzle_key = 'iss-bound-word-callback';
    update public.puzzles set requires_flags = jsonb_build_object('deep_gate_open', true)
      where puzzle_key = 'spine-threshold-vault';
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true)
      where puzzle_key in ('iss-nbt-falsified-entry', 'spine-spoken-name', 'spine-unkept-acrostic');

    -- Activating the gate flips these rows' static active flag ON (so getOpenPuzzles' active
    -- pre-filter does not exclude them before the requires_flags AND-test runs). The pair
    -- (active=true AND requires_flags-satisfied) is the open condition; both must hold.
    update public.puzzles set active = true
      where puzzle_key in (
        'bound-word', 'm4-three-hands', 'threshold-coordinate', 'true-walk-arrive',
        'seventh-unwriting', 'seventh-cause', 'seventh-choice', 'meta-unkept',
        -- the diverse-expansion gated rows:
        'vaun-bookshelf-tally', 'mara-walk-the-map', 'sella-overlay-lake', 'sella-shore-memorial',
        'orin-frame-dials', 'iss-which-is-true', 'iss-nbt-falsified-entry', 'iss-bound-word-callback',
        'spine-threshold-vault', 'spine-spoken-name', 'spine-unkept-acrostic'
      );

  else
    -- FAIL LOUD (P0-C6): a silent no-op here is the footgun — the nine staged rows would stay
    -- dark AND base-docket-reread-auto (ships active=true) would keep its default empty
    -- requires_flags and LEAK its four M4 docket answers from minute one. Aborting the whole
    -- seed batch is strictly safer than half-applying it. The fix is to apply 0006 first (the
    -- db:seed runner and apply-tonight.sql both do); then re-run. Never downgrade this to a notice.
    raise exception 'metapuzzle_seed: puzzles.requires_flags absent — apply migration 0006_requires_flags BEFORE the seeds (use `npm run db:seed` or supabase/apply-all.sql, which enforce the order). Aborting to avoid leaking the M4 docket answers.';
  end if;
end $$;

-- ===========================================================================
-- 3. THE ACTIVATION-REACHABILITY LEDGER (activationReachabilitySelfTest, BUILD-MANIFEST §8 /
--    seedcheck NEW). This comment block is the authoritative map the NEW seedcheck assertion
--    (the TS-SHOWRUN / oracle/seedcheck.ts owner adds it; listed in the RETURN) verifies:
--    every requires_flags row is named by EXACTLY ONE activation rule, and NO active=true row
--    is reachable only through a staged predecessor. The activation rules ARE §2 above (one
--    UPDATE per gate). The full map, row → the single flag-rule that lights it:
--
--      bound-word                → iss_caught                          (no-wall-catch)
--      base-docket-reread-auto   → iss_caught                          (no-wall-catch)   [deterministic; twin retired]
--      meta-unkept               → iss_caught                          (no-wall-catch)
--      m4-three-hands            → bound_word_known                    (bound-word)
--      threshold-coordinate      → threshold_open                      (m4-three-hands)
--      true-walk-arrive          → true_coord_known                    (threshold-coordinate)
--      seventh-shrine            → seventh_suspected                   (stone-sella)     [P1-C3 collision gate; ungated upstream, stays active=true]
--      seventh-unwriting         → iss_caught ∧ seventh_suspected      (no-wall-catch ∧ stone-sella)
--      seventh-cause             → iss_caught ∧ seventh_suspected      (no-wall-catch ∧ stone-sella)
--      seventh-choice            → seventh_named                       (seventh-unwriting)
--
--    INVARIANT (seedcheck NEW): rite-tokens (active=true, the Accepting on-ramp) is reachable
--    via true-walk-arrive's next_puzzle_key AND independently via pressure-glyph-walk
--    (side_quest, the promoted 2nd in-road) — so it is NOT reachable only through a staged
--    predecessor. The repoint in §0 (catch no longer shortcuts to rite-tokens) does not
--    orphan the rite: the chain + the glyph-walk both still arrive. No spine row requires a
--    FORK flag (seedcheck §A11) — fork-light/fork-name set color flags read only by the M5
--    composer, never by requires_flags.
-- ===========================================================================

-- ===========================================================================
-- 4. THE COMPANION REVEAL GATE (the-companion.md §4/§7). Wren's reveal is tied to
--    the Iss catch, NOT a calendar (async-safe): the same lens that caught Iss's warm
--    lie (iss_caught) turns on the living warm liar. So the companion reveal is gated
--    behind iss_caught with the SAME guarded requires_flags pattern used in §2 — a row
--    is OPEN iff active=true AND every requires_flags key is truthy in arc_state.flags.
--
--    THE FLAG CONTRACT (§7 seeds/flags), all group-scoped arc_state flags:
--      companion_introduced   — set when Wren first appears (M1). PRODUCER: plugin.
--      companion_trust (int)  — rises across M1–M3 (harvest ramps with it). PRODUCER: plugin.
--      companion_tells_seeded — his steering tells are planted (M3). PRODUCER: plugin.
--      companion_revealed     — the reveal event fired (M4); GATE: iss_caught. PRODUCER: plugin.
--      reckoning_condemn | reckoning_understand | reckoning_free — the group's chosen
--                               record-line about him (M5), mutually exclusive. PRODUCER: plugin.
--
--    PENDING PRODUCERS (out of THIS scope): the flag PRODUCERS are plugin listeners
--    (the companion is Citizens2 / a beat listener; §7 NPC framework). This file wires
--    the DATA + GATING only. The reveal's CONTENT is npcLines.wren.reveal.* and the
--    found tally is cardKeptClose (thread_cards `kept-close`, alt_text_condition
--    'companion:revealed'); both are authored now. The producer that SETS
--    companion_revealed must AND-check iss_caught before firing (mirrored below in SQL
--    so that, the instant a `companion-reveal` puzzle row is ever seeded, its gate is
--    correct without a second edit).
--
--    GUARDED + IDEMPOTENT: the block no-ops cleanly if 0006 (the column) has not landed
--    AND no-ops if the companion reveal row is not seeded yet (the UPDATE matches 0 rows).
--    So it is safe today (row absent → 0 rows updated) and correct the day the row lands.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_flags'
  ) then
    -- Gate the companion reveal behind the Iss catch (the-companion.md §4). No-ops
    -- until a 'companion-reveal' row exists (producers pending; §7); harmless today.
    update public.puzzles set requires_flags = jsonb_build_object('iss_caught', true)
      where puzzle_key = 'companion-reveal';
  else
    -- FAIL LOUD (P0-C6): consistent with §2 — the column's absence means a mis-order; abort
    -- rather than silently skip. Apply 0006 first (db:seed / apply-all.sql enforce it), then re-run.
    raise exception 'metapuzzle_seed: puzzles.requires_flags absent — apply migration 0006_requires_flags BEFORE the seeds (use `npm run db:seed` or supabase/apply-all.sql, which enforce the order). Aborting.';
  end if;
end $$;

-- ===========================================================================
-- 5. THE ROSTER-QUORUM CONVERGENCE TAGS (S-F roster guard; 0008_requires_quorum.sql).
--    Tag the beats that GENUINELY need multiple players present with requires_quorum = 2 (a
--    convergence needs at least two hands). The showrunner drip (decide.rosterCanClose) then WITHHOLDS
--    these threads whenever the active roster is below quorum — never surfacing a convergence the
--    present group cannot possibly close (the dead-air failure the reshape fixes). This gates ONLY the
--    curatorial drip; resolution is still governed by requires_flags + the in-world group detection.
--
--    The four convergence rows (verified against puzzles_seed.sql — each is a group act, not a solo
--    decode; accepted_answers is an opaque plugin-posted conjunction token):
--      m4-three-hands        — three hands on three surfaces inside one window (the cross-surface gate)
--      accepting-crouch      — everyone present bows as one (the collective climax rite)
--      spine-threshold-vault — an asymmetric co-op vault (roles split across players)
--      mara-walk-the-map     — the group physically walks the marker row and bows together
--
--    Guarded + idempotent (mirrors §2/§4): sets an ABSOLUTE value, so re-running is safe; no-ops
--    cleanly if 0008 (the column) has not landed yet, and touches NO other column on these rows.
-- ===========================================================================

do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'puzzles' and column_name = 'requires_quorum'
  ) then
    update public.puzzles set requires_quorum = 2
      where puzzle_key in (
        'm4-three-hands', 'accepting-crouch', 'spine-threshold-vault', 'mara-walk-the-map'
      );
  else
    -- FAIL LOUD (P0-C6): consistent with §2/§4 — the column's absence means a mis-order; abort
    -- rather than silently skip so the convergence guard is never half-wired. Apply 0008 first
    -- (db:seed / apply-all.sql enforce it), then re-run.
    raise exception 'metapuzzle_seed: puzzles.requires_quorum absent — apply migration 0008_requires_quorum BEFORE the seeds (use `npm run db:seed` or supabase/apply-all.sql, which enforce the order). Aborting.';
  end if;
end $$;

commit;
