// record-terminal.selftest.ts - pins the Record terminal's spoiler/difficulty timing policy.

import { earnedTier, stallLabel, stallMsSinceLastAdvance } from './integrity-policy';

let failures = 0;

function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  x ${name}`);
  } else {
    console.log(`  ok ${name}`);
  }
}

console.log('record-terminal.selftest');

const now = Date.UTC(2026, 6, 7, 12, 0, 0);

// Cold-open terminal: no web advance has happened yet. This must stay tier 1 so a guessed/discovered
// URL cannot turn the integrity log into a near-plain walkthrough before the group has earned progress.
{
  const stallMs = stallMsSinceLastAdvance(0, now);
  check('no prior solve is unresolved, not maximally stalled', stallMs === 0);
  check('no prior solve earns only tier 1', earnedTier(stallMs) === 1);
  check('no prior solve label is unresolved', stallLabel(stallMs) === 'unresolved');
}

// After real progress, elapsed time can escalate the safety valve.
{
  const almostOneDay = stallMsSinceLastAdvance(now - 23 * 3_600_000, now);
  const oneDay = stallMsSinceLastAdvance(now - 24 * 3_600_000, now);
  const threeDays = stallMsSinceLastAdvance(now - 72 * 3_600_000, now);
  check('23h stall stays tier 1', earnedTier(almostOneDay) === 1);
  check('24h stall earns tier 2', earnedTier(oneDay) === 2);
  check('72h stall earns tier 3', earnedTier(threeDays) === 3);
  check('24h stall label is stalled 1d', stallLabel(oneDay) === 'stalled 1d');
  check('72h stall label is stalled 3d', stallLabel(threeDays) === 'stalled 3d');
}

// Clock skew / future timestamps should never produce negative stalls or escalated hints.
{
  const skew = stallMsSinceLastAdvance(now + 3_600_000, now);
  check('future solve timestamp clamps to unresolved', skew === 0);
  check('future solve timestamp earns only tier 1', earnedTier(skew) === 1);
}

if (failures > 0) {
  console.error(`record-terminal.selftest FAILED - ${failures} issue(s)`);
  process.exit(1);
}

console.log('record-terminal.selftest OK');
