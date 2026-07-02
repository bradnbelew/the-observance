// record-projection.selftest.ts — pins every redaction in the Record projection (A13).
//
// Cheap, dependency-free assertions (the repo's `.selftest` convention). The whole point of The Record
// is that it leaks NOTHING ahead of earned progress; these tests are the camera-guard for that. Run with
// `npx tsx src/lib/record-projection.selftest.ts` (or wire into the dashboard's test runner).

import { project, REDACTED_GLYPH, type RecordSignal } from './record-projection';

let failures = 0;
function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  ✗ ${name}`);
  } else {
    console.log(`  ✓ ${name}`);
  }
}

console.log('record-projection.selftest');

// 1. The sealed baseline: an absent/empty signal opens an empty record, never an error, never a leak.
{
  const p = project({});
  check('empty signal → 0 kept', p.kept === 0);
  check('empty signal → nothing legible', p.entries.every((e) => !e.legible));
  check('empty signal → "not yet opened" season', p.season.includes('not yet opened'));
}

// 2. REVEAL DISCIPLINE: exactly `stonesRead` stone-entries are legible, in order, never ahead.
{
  const p = project({ movement: 2, stonesRead: 3 });
  const legible = p.entries.filter((e) => e.legible);
  check('3 stones read → 3 legible', legible.length === 3);
  check('legible are the FIRST three (in order)', legible.every((e, i) => e.id === `stone-${i + 1}`));
  check('the 4th stone is still withheld', p.entries[3].legible === false);
}

// 3. The closing entry stays sealed until the keeping closes — the one arc-end signal.
{
  const open = project({ movement: 4, stonesRead: 6, accepted: false });
  check('all stones read but not accepted → closing withheld', open.entries.at(-1)!.legible === false);
  check('open → footer says the rest is not yet kept', open.footer.includes('not yet kept'));

  const done = project({ movement: 5, stonesRead: 6, accepted: true });
  check('accepted → closing legible', done.entries.at(-1)!.legible === true);
  check('accepted → footer says closed', done.footer.includes('closed'));
  check('accepted → season is the accepting', done.season.includes('accepting'));
}

// 4. Clamping: malformed / early / over-range view rows can never over-reveal or crash.
{
  const over = project({ movement: 99, stonesRead: 99, accepted: null } as RecordSignal);
  check('over-range stonesRead clamps to total stones', over.entries.filter((e) => e.id.startsWith('stone-') && e.legible).length === 6);
  check('over-range without accepted keeps closing sealed', over.entries.at(-1)!.legible === false);

  const neg = project({ movement: -5, stonesRead: -5 } as RecordSignal);
  check('negative signal floors to the sealed baseline', neg.kept === 0);
}

// 5. Determinism: same signal → same projection (the backstop IS the determinism).
{
  const sig: RecordSignal = { movement: 3, stonesRead: 4, accepted: false };
  check('deterministic', JSON.stringify(project(sig)) === JSON.stringify(project(sig)));
}

// 6. No withheld line ever ships its text — the route must render REDACTED_GLYPH for !legible.
{
  const p = project({ stonesRead: 2 });
  // The projection still carries the text (the route gates it), so this asserts the contract the route
  // depends on: a withheld entry is identifiable purely by `legible === false`.
  check('REDACTED_GLYPH is a non-empty struck block', REDACTED_GLYPH.length > 0);
  check('withheld entries are flagged, not blanked, in the model', p.entries.some((e) => !e.legible && e.line.length > 0));
}

// 7. S-D reward-the-theory: when `theories` is present, a keeper's fate un-redacts on its assembled
//    THEORY (not stone-read count); absent → the stonesRead fallback still governs (backward-compat).
{
  // theories present + non-empty → exactly those keepers' entries legible, regardless of stonesRead.
  const t = project({ movement: 2, stonesRead: 6, theories: ['vaun', 'sella'] });
  const legible = t.entries.filter((e) => e.legible && e.id.startsWith('stone-'));
  check('theories present → only theory-locked keepers legible', legible.length === 2);
  check('theories map to the right entries (vaun=stone-1, sella=stone-3)',
    legible.map((e) => e.id).sort().join(',') === 'stone-1,stone-3');
  check('a stone read but theory NOT locked stays withheld (reward the theory, not the lookup)',
    t.entries.find((e) => e.id === 'stone-2')!.legible === false);

  // empty theories present → no keeper fate received yet (the reward-the-theory zero state).
  const empty = project({ stonesRead: 4, theories: [] });
  check('empty theories → no keeper entry legible despite stones read', empty.entries.filter((e) => e.id.startsWith('stone-') && e.legible).length === 0);

  // theories ABSENT → the pre-S-D stonesRead lockstep is preserved (progressive rollout).
  const fallback = project({ stonesRead: 3 });
  check('theories absent → stonesRead fallback still governs', fallback.entries.filter((e) => e.id.startsWith('stone-') && e.legible).length === 3);

  // an unknown/garbage keeper id in the set simply matches nothing (can't over-reveal).
  const junk = project({ theories: ['nobody', 'vaun'] });
  check('unknown theory id matches nothing; known one still resolves', junk.entries.filter((e) => e.id.startsWith('stone-') && e.legible).length === 1);
}

if (failures > 0) {
  console.error(`\nrecord-projection.selftest: ${failures} FAILED`);
  process.exit(1);
}
console.log('\nrecord-projection.selftest: all passed');
