/**
 * gate.selftest.ts — proves the SOLDERED NERVE (OVERHAUL.md §2/§6, Phase 1).
 *   npx tsx src/oracle/gate.selftest.ts   (or: npm run gatecheck)
 * Pure: no DB, no network — runs anywhere. Exits non-zero on any failed assertion.
 *
 * It exercises the exact predicate the live resolver uses:
 *   - flagsSatisfied — the storylet gate (a row opens iff active AND its requires_flags
 *     are all truthy in arc_state.flags).
 *   - getOpenPuzzles' filter — the open set is the active rows filtered by that gate.
 *   - matchPuzzles + the resolver's "prefer the unsolved candidate" rule — a plaintext
 *     shared by a SEQUENCED pair resolves to the row the player hasn't solved.
 * Then it runs the whole vertical slice as one narrative: ignition → gate-closed →
 * upstream solve sets the flag → gate-opens → downstream becomes matchable, and the
 * shared bound word resolves past the already-solved upstream owner.
 */
import { flagsSatisfied, matchPuzzle, matchPuzzles } from './gate.js';
import type { Puzzle } from '../db/types.js';

let failures = 0;
function check(label: string, cond: boolean): void {
  if (cond) {
    console.log(`  ok   ${label}`);
  } else {
    failures += 1;
    console.error(`  FAIL ${label}`);
  }
}

/** Minimal Puzzle factory — only the fields the gate/match logic reads matter. */
function puz(over: Partial<Puzzle> & Pick<Puzzle, 'puzzle_key' | 'accepted_answers'>): Puzzle {
  return {
    title: over.puzzle_key,
    outcome_type: 'next_clue',
    outcome_payload: {},
    movement: 0,
    active: true,
    max_attempts: null,
    requires_flags: {},
    ...over,
  };
}

/** The open set exactly as getOpenPuzzles computes it: active rows the flags satisfy. */
function openSet(all: readonly Puzzle[], flags: Record<string, unknown>): Puzzle[] {
  return all.filter((p) => p.active && flagsSatisfied(p.requires_flags, flags));
}

/** The resolver's pickCandidate rule, pure: first UNSOLVED candidate, else allSolved. */
function pick(
  candidates: readonly Puzzle[],
  solved: ReadonlySet<string>,
): { puzzle: Puzzle | null; alreadySolved: boolean } {
  if (candidates.length === 0) return { puzzle: null, alreadySolved: false };
  for (const c of candidates) {
    if (!solved.has(c.puzzle_key)) return { puzzle: c, alreadySolved: false };
  }
  return { puzzle: candidates[0]!, alreadySolved: true };
}

// ---------------------------------------------------------------------------
// 1. flagsSatisfied — the gate predicate.
// ---------------------------------------------------------------------------
check('ungated {} is always open', flagsSatisfied({}, {}) === true);
check('ungated (null) is always open', flagsSatisfied(null, { x: true }) === true);
check('gated, flag absent → closed', flagsSatisfied({ iss_caught: true }, {}) === false);
check('gated, flag set → open', flagsSatisfied({ iss_caught: true }, { iss_caught: true }) === true);
check('gated, flag falsy → closed', flagsSatisfied({ iss_caught: true }, { iss_caught: false }) === false);
check(
  'multi-key gate needs ALL truthy',
  flagsSatisfied({ a: true, b: true }, { a: true }) === false &&
    flagsSatisfied({ a: true, b: true }, { a: true, b: true }) === true,
);

// ---------------------------------------------------------------------------
// 2. matchPuzzle / matchPuzzles — whole-string set membership.
// ---------------------------------------------------------------------------
{
  const rows = [
    puz({ puzzle_key: 'a', accepted_answers: ['alpha'] }),
    puz({ puzzle_key: 'b', accepted_answers: ['beta', 'shared'] }),
    puz({ puzzle_key: 'c', accepted_answers: ['shared'] }),
  ];
  check('empty string never matches', matchPuzzles(rows, '') .length === 0 && matchPuzzle(rows, '') === null);
  check('true miss → no candidates', matchPuzzles(rows, 'nope').length === 0);
  check('single owner → 1 candidate', matchPuzzles(rows, 'alpha').map((p) => p.puzzle_key).join() === 'a');
  check('shared plaintext → all owners', matchPuzzles(rows, 'shared').map((p) => p.puzzle_key).join() === 'b,c');
  check('matchPuzzle returns first owner only', matchPuzzle(rows, 'shared')?.puzzle_key === 'b');
}

// ---------------------------------------------------------------------------
// 3. pick rule — sequenced re-submission prefers the unsolved row.
// ---------------------------------------------------------------------------
{
  const up = puz({ puzzle_key: 'stone-iss-wall', accepted_answers: ['the one who turned away'] });
  const down = puz({ puzzle_key: 'bound-word', accepted_answers: ['the one who turned away'] });
  const both = [up, down];
  check('both unsolved → first owner (the catch)', pick(both, new Set()).puzzle?.puzzle_key === 'stone-iss-wall');
  check(
    'upstream solved → picks the unsolved downstream',
    pick(both, new Set(['stone-iss-wall'])).puzzle?.puzzle_key === 'bound-word',
  );
  const all = pick(both, new Set(['stone-iss-wall', 'bound-word']));
  check('all solved → alreadySolved + canonical owner', all.alreadySolved === true && all.puzzle?.puzzle_key === 'stone-iss-wall');
}

// ---------------------------------------------------------------------------
// 4. THE VERTICAL SLICE — ignition → gate-closed → solve-upstream → flag-set →
//    gate-opens → downstream matchable, with the shared bound word resolving past
//    the already-solved owner. This is the whole soldered nerve in one story.
// ---------------------------------------------------------------------------
{
  // The spine, as the seeds encode it: stone-iss-wall (ungated, sets iss_caught) and
  // bound-word (the M4 consumer, gated on iss_caught), sharing the decoded bound word.
  const issWall = puz({
    puzzle_key: 'stone-iss-wall',
    accepted_answers: ['the one who turned away'],
    outcome_type: 'next_clue',
    outcome_payload: { set_flags: { iss_caught: true } },
  });
  const boundWord = puz({
    puzzle_key: 'bound-word',
    accepted_answers: ['the one who turned away'],
    requires_flags: { iss_caught: true },
    active: true,
  });
  const all = [issWall, boundWord];

  // a) Pre-ignition / pre-catch: flags empty. The gated M4 row is INVISIBLE.
  let flags: Record<string, unknown> = {};
  const before = openSet(all, flags);
  check('slice: before the catch, bound-word is CLOSED', !before.some((p) => p.puzzle_key === 'bound-word'));
  check('slice: before the catch, stone-iss-wall is OPEN', before.some((p) => p.puzzle_key === 'stone-iss-wall'));

  // b) The player submits the bound word now. Only the catch is open; pick solves it and
  //    its outcome sets iss_caught (we apply the merge the way setArcFlags/applyOutcome do).
  const solved = new Set<string>();
  const cand1 = matchPuzzles(before, 'the one who turned away');
  const chosen1 = pick(cand1, solved);
  check('slice: the catch resolves stone-iss-wall first', chosen1.puzzle?.puzzle_key === 'stone-iss-wall');
  solved.add(chosen1.puzzle!.puzzle_key);
  flags = { ...flags, ...(chosen1.puzzle!.outcome_payload.set_flags ?? {}) }; // the atomic merge, modeled

  // c) Post-catch: the flag is set → the M4 row is now OPEN.
  const after = openSet(all, flags);
  check('slice: iss_caught is now set', flags.iss_caught === true);
  check('slice: after the catch, bound-word is OPEN', after.some((p) => p.puzzle_key === 'bound-word'));

  // d) The player re-submits the SAME bound word at the gate. matchPuzzles returns both
  //    owners; pick skips the already-solved catch and resolves the fresh M4 consumer —
  //    NOT shadowed, NOT a dead silent replay.
  const cand2 = matchPuzzles(after, 'the one who turned away');
  check('slice: both owners are open candidates now', cand2.map((p) => p.puzzle_key).sort().join() === 'bound-word,stone-iss-wall');
  const chosen2 = pick(cand2, solved);
  check('slice: re-submission resolves bound-word (unsolved), not the solved catch', chosen2.puzzle?.puzzle_key === 'bound-word' && chosen2.alreadySolved === false);
}

// ---------------------------------------------------------------------------
if (failures > 0) {
  console.error(`\ngate.selftest: ${failures} FAILED`);
  process.exit(1);
}
console.log('\ngate.selftest: OK — the storylet gate, answer matching, and the full ignition→gate→solve→unlock slice all pass.');
