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

    -- Activating the gate flips these rows' static active flag ON (so getOpenPuzzles' active
    -- pre-filter does not exclude them before the requires_flags AND-test runs). The pair
    -- (active=true AND requires_flags-satisfied) is the open condition; both must hold.
    update public.puzzles set active = true
      where puzzle_key in (
        'bound-word', 'm4-three-hands', 'threshold-coordinate', 'true-walk-arrive',
        'seventh-unwriting', 'seventh-cause', 'seventh-choice', 'meta-unkept'
      );

  else
    raise notice 'metapuzzle_seed: puzzles.requires_flags absent (0006 not applied) — activation lane skipped, re-run after the migration.';
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

commit;
