/**
 * theory.selftest.ts — pure unit tests for the S-D theory-lock policy (no DB, no network, no clock).
 *   npx tsx src/showrunner/theory.selftest.ts
 *
 * These same assertions also run inside autonomy.selftest.ts (the `showrunner:test:autonomy` gate), so
 * theory is covered by the build; this standalone file lets the policy be exercised in isolation. It
 * pins the LAWS the module exists to honor: the cluster threshold ("enough is a theory, one is not"),
 * the idempotent `<keeper>_theory` flag high-water (a locked keeper never re-emits), fall-order
 * determinism, and partial-cluster precision (only threshold-crossers lock).
 */
import { decideTheories, CLUSTERS, theoryFlag, type KeeperId } from './theory.js';

let failures = 0;
function check(label: string, cond: boolean): void {
  if (cond) console.log(`  ok   ${label}`);
  else { failures += 1; console.error(`  FAIL ${label}`); }
}

// THRESHOLD: a single stone-decode is not yet a coherent theory (threshold 2).
check('theory: below threshold (one solve) → none',
  decideTheories(new Set(['stone-vaun']), new Set()).length === 0);
// THRESHOLD MET: stone + one corroborating solve → the keeper locks.
check('theory: threshold met → keeper locks',
  JSON.stringify(decideTheories(new Set(['stone-vaun', 'vaun-hoard-sorted']), new Set())) === JSON.stringify(['vaun']));
// IDEMPOTENT: an already-locked keeper is never re-emitted (the flag is the high-water).
check('theory: already-locked keeper → not re-emitted',
  decideTheories(new Set(['stone-vaun', 'vaun-hoard-sorted', 'vaun-bookshelf-tally']), new Set(['vaun'])).length === 0);
// PARTIAL CLUSTERS: only the keepers that cross threshold lock; a 1-of-N keeper is held.
check('theory: partial clusters → only threshold-crossers lock',
  JSON.stringify(decideTheories(
    new Set(['stone-vaun', 'vaun-hoard-sorted', 'stone-mara', 'stone-brann', 'brann-black-moon-toll']),
    new Set(),
  )) === JSON.stringify(['vaun', 'brann']));
// FALL-ORDER: all coherent at once → emitted in CLUSTERS declaration order.
const all = new Set<string>();
for (const c of CLUSTERS) for (const k of c.evidence) all.add(k);
const expected: KeeperId[] = ['vaun', 'mara', 'sella', 'orin', 'brann', 'iss'];
check('theory: all clusters coherent → all keepers, in fall-order',
  JSON.stringify(decideTheories(all, new Set())) === JSON.stringify(expected));
// DETERMINISTIC.
check('theory: deterministic',
  JSON.stringify(decideTheories(new Set(['stone-sella', 'sella-overlay-lake']), new Set())) ===
  JSON.stringify(decideTheories(new Set(['stone-sella', 'sella-overlay-lake']), new Set())));
// FLAG KEY: the canonical `<keeper>_theory` (the S-E dovetail contract).
check('theory: flag key is <keeper>_theory', theoryFlag('iss') === 'iss_theory');

if (failures > 0) {
  console.error(`\ntheory selftest: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\ntheory selftest: OK — the theory-lock policy holds.');
