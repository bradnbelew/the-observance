/**
 * Pure unit tests for the customs→report policy (customs.ts, COHERENCE-AUDIT P0-4 / D1).
 * No DB, no network — runs anywhere. Mirrors decide.selftest.ts.
 *   npx tsx src/showrunner/customs.selftest.ts   (or: npm run showrunner:test:customs)
 * Exits non-zero on any failed assertion so it can gate the build.
 */
import { decideCustomReports, OBSERVE_AT, WARN_AT, LEFT_AT, type CustomReportInput } from './customs.js';
import { voice, customPhrase } from '../voice.js';
import type { CustomViolation } from '../db/repo.js';

let failures = 0;
function check(label: string, cond: boolean) {
  if (cond) {
    console.log(`  ok   ${label}`);
  } else {
    failures += 1;
    console.error(`  FAIL ${label}`);
  }
}

function violation(over: Partial<CustomViolation> = {}): CustomViolation {
  return { groupKey: 'uuid-1', customKey: 'the_bow', name: 'orin', honoredCount: 7, violatedCount: 0, ...over };
}
function input(over: Partial<CustomReportInput> = {}): CustomReportInput {
  return {
    violations: [], reported: {}, mode: 'confirm',
    observeAt: OBSERVE_AT, warnAt: WARN_AT, leftAt: LEFT_AT, ...over,
  };
}

// 1. Zero violations measured → never invent a report (grounding discipline).
{
  const d = decideCustomReports(input({ violations: [violation({ violatedCount: 0 })] }));
  check('zero violated → no report', d.reports.length === 0);
}

// 2. observed rung: 1..2 violations → one observed report in register, no toll.
{
  const d = decideCustomReports(input({ violations: [violation({ violatedCount: 1 })] }));
  check('1 violated → exactly one report', d.reports.length === 1);
  check('1 violated → rung=observed', d.reports[0]?.rung === 'observed');
  check('observed → no toll', d.reports[0]?.toll === false);
  check('observed → line is reportObserved', d.reports[0]?.line === voice.reportObserved('orin', 7, customPhrase('the_bow')));
  check('mark = measured count', d.marks['uuid-1|the_bow'] === 1);
}

// 3. warned rung: WARN_AT violations → toll AND the observed line.
{
  const d = decideCustomReports(input({ violations: [violation({ violatedCount: WARN_AT })] }));
  check('warn → rung=warned', d.reports[0]?.rung === 'warned');
  check('warn → toll laid', d.reports[0]?.toll === true);
}

// 4. left rung: LEFT_AT violations → the cold escalation line, no toll at this rung.
{
  const d = decideCustomReports(input({ violations: [violation({ violatedCount: LEFT_AT })] }));
  check('left → rung=left', d.reports[0]?.rung === 'left');
  check('left → line is reportEscalated', d.reports[0]?.line === voice.reportEscalated('orin'));
  check('left → no toll', d.reports[0]?.toll === false);
}

// 5. PRECISION: measured but unnamable → skipped, never guessed (+ a note).
{
  const d = decideCustomReports(input({ violations: [violation({ name: null, violatedCount: 9 })] }));
  check('no name → no report', d.reports.length === 0);
  check('no name → a skip note', d.notes.some((n) => n.includes('no name')));
}

// 6. IDEMPOTENT: a rung already reported (mark >= entry) does NOT re-fire.
{
  const d = decideCustomReports(input({
    violations: [violation({ violatedCount: 1 })],
    reported: { 'uuid-1|the_bow': 1 }, // observed already reported at count 1
  }));
  check('already-reported observed → no re-fire', d.reports.length === 0);
}

// 7. IDEMPOTENT escalation: observed reported, count RISES into warn → fires warn only.
{
  const d = decideCustomReports(input({
    violations: [violation({ violatedCount: WARN_AT })],
    reported: { 'uuid-1|the_bow': OBSERVE_AT }, // observed rung already covered
  }));
  check('risen to warn → exactly one report', d.reports.length === 1);
  check('risen to warn → rung=warned', d.reports[0]?.rung === 'warned');
  check('risen to warn → new mark', d.marks['uuid-1|the_bow'] === WARN_AT);
}

// 8. IDEMPOTENT at the top: count past LEFT already reported → silent forever after.
{
  const d = decideCustomReports(input({
    violations: [violation({ violatedCount: LEFT_AT + 4 })],
    reported: { 'uuid-1|the_bow': LEFT_AT + 1 }, // left rung already crossed
  }));
  check('past-left already reported → no re-fire', d.reports.length === 0);
}

// 9. Per-(player,custom) marks are independent — same player, two customs.
{
  const d = decideCustomReports(input({
    violations: [
      violation({ customKey: 'the_bow', violatedCount: 1 }),
      violation({ customKey: 'the_offering', violatedCount: LEFT_AT }),
    ],
    reported: { 'uuid-1|the_bow': 1 }, // bow already observed; offering fresh
  }));
  check('independent customs → only offering fires', d.reports.length === 1);
  check('independent customs → offering=left', d.reports[0]?.customKey === 'the_offering' && d.reports[0]?.rung === 'left');
}

// 10. Deterministic ordering: stable sort by (groupKey, customKey).
{
  const d = decideCustomReports(input({
    violations: [
      violation({ groupKey: 'uuid-2', customKey: 'the_kept_light', violatedCount: 2 }),
      violation({ groupKey: 'uuid-1', customKey: 'the_offering', violatedCount: 2 }),
    ],
  }));
  check('order → group asc first', d.reports[0]?.groupKey === 'uuid-1');
  check('order → second group', d.reports[1]?.groupKey === 'uuid-2');
}

// 11. Determinism: same input twice → identical decision.
{
  const i = input({ violations: [violation({ violatedCount: WARN_AT })] });
  check('deterministic', JSON.stringify(decideCustomReports(i)) === JSON.stringify(decideCustomReports(i)));
}

// 12. AUTO vs CONFIRM does not change the prose decision (only the toll status, set in the pass).
{
  const auto = decideCustomReports(input({ mode: 'auto', violations: [violation({ violatedCount: WARN_AT })] }));
  const confirm = decideCustomReports(input({ mode: 'confirm', violations: [violation({ violatedCount: WARN_AT })] }));
  check('mode does not alter reports', JSON.stringify(auto.reports) === JSON.stringify(confirm.reports));
}

// 13. Every detected custom_key resolves to an in-register phrase (no leaked raw key).
{
  const keys = ['the_bow', 'the_offering', 'the_kept_light', 'the_deep_line', 'the_unspoken', 'the_sacred_beast', 'the_dark_hours'];
  // Each of the seven detected customs maps to a distinct, non-empty, in-register clause
  // (none falls through to the generic "kept the ways" fallback).
  check('all 7 customs have a distinct phrase', keys.every((k) => customPhrase(k).length > 0 && customPhrase(k) !== 'kept the ways'));
  check('7 phrases are unique', new Set(keys.map(customPhrase)).size === keys.length);
  check('unknown key → safe fallback', customPhrase('totally_unknown') === 'kept the ways');
}

if (failures > 0) {
  console.error(`\nshowrunner customs: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner customs: OK — all assertions passed.');
