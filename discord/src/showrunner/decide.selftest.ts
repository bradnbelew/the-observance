/**
 * Pure unit tests for the showrunner policy (decide.ts). No DB, no network — runs anywhere.
 *   npx tsx src/showrunner/decide.selftest.ts   (or: npm run showrunner:test)
 * Exits non-zero on any failed assertion so it can gate the build.
 */
import { decide } from './decide.js';
import type { Snapshot, SnapshotPuzzle, AttempterState } from './types.js';

let failures = 0;
function check(label: string, cond: boolean) {
  if (cond) {
    console.log(`  ok   ${label}`);
  } else {
    failures += 1;
    console.error(`  FAIL ${label}`);
  }
}

const HOUR = 3_600_000;

function attempter(over: Partial<AttempterState> = {}): AttempterState {
  return { playerId: 'p1', act: 1, whisperRemaining: 0, nextTier: 1, nextTierHintExists: true, ...over };
}
function puzzle(over: Partial<SnapshotPuzzle> = {}): SnapshotPuzzle {
  return {
    puzzleKey: 'stone-vaun', movement: 2, failedAttemptsInWindow: 0,
    solvedInWindow: false, attempters: [], dripped: false, ...over,
  };
}
function snap(over: Partial<Snapshot> = {}): Snapshot {
  return {
    nowMs: 1_000 * HOUR, asleep: false, mode: 'confirm', currentAct: 1,
    openPuzzles: [], lastDripAtMs: null, stallFailedThreshold: 5, dripIntervalMs: 20 * HOUR, ...over,
  };
}

// 1. Kill-switch: asleep → nothing but heartbeat.
{
  const d = decide(snap({ asleep: true, openPuzzles: [puzzle({ failedAttemptsInWindow: 99, dripped: false })] }));
  check('asleep → no gifts', d.gifts.length === 0);
  check('asleep → no drips', d.drips.length === 0);
  check('asleep → heartbeat note', d.health.note === 'asleep');
}

// 2. Stuck + exhausted attempter + hint exists → exactly one gift at the next tier.
{
  const d = decide(snap({
    lastDripAtMs: 1_000 * HOUR, // not due, isolate the gift path
    openPuzzles: [puzzle({ failedAttemptsInWindow: 5, attempters: [attempter({ nextTier: 2 })] })],
  }));
  check('stuck+exhausted+hint → 1 gift', d.gifts.length === 1);
  check('gift tier = nextTier', d.gifts[0]?.tier === 2);
}

// 3. Stuck but attempter still has a whisper → no gift (backstop only).
{
  const d = decide(snap({
    lastDripAtMs: 1_000 * HOUR,
    openPuzzles: [puzzle({ failedAttemptsInWindow: 9, attempters: [attempter({ whisperRemaining: 2 })] })],
  }));
  check('stuck but has whisper → no gift', d.gifts.length === 0);
}

// 4. Stuck + exhausted but NO authored next-tier hint → no gift (never invent a hint).
{
  const d = decide(snap({
    lastDripAtMs: 1_000 * HOUR,
    openPuzzles: [puzzle({ failedAttemptsInWindow: 9, attempters: [attempter({ nextTierHintExists: false })] })],
  }));
  check('stuck but no hint → no gift', d.gifts.length === 0);
}

// 5. Below threshold → not stuck → no gift.
{
  const d = decide(snap({
    lastDripAtMs: 1_000 * HOUR,
    openPuzzles: [puzzle({ failedAttemptsInWindow: 4, attempters: [attempter()] })],
  }));
  check('below threshold → no gift', d.gifts.length === 0);
}

// 6. Solved in window → not stuck even with many fails.
{
  const d = decide(snap({
    lastDripAtMs: 1_000 * HOUR,
    openPuzzles: [puzzle({ failedAttemptsInWindow: 50, solvedInWindow: true, attempters: [attempter()] })],
  }));
  check('solved in window → no gift', d.gifts.length === 0);
}

// 7. First-ever drip (lastDripAtMs null) → drips the lowest movement, then key.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'stone-orin', movement: 2 }),
      puzzle({ puzzleKey: 'm1-record-opens', movement: 1 }),
      puzzle({ puzzleKey: 'stone-brann', movement: 2 }),
    ],
  }));
  check('first drip → exactly one', d.drips.length === 1);
  check('drip picks lowest movement', d.drips[0]?.puzzleKey === 'm1-record-opens');
  check('confirm mode → staged', d.drips[0]?.staged === true);
}

// 8. AUTO mode → drip not staged.
{
  const d = decide(snap({ mode: 'auto', openPuzzles: [puzzle()] }));
  check('auto mode → drip live (not staged)', d.drips[0]?.staged === false);
}

// 9. Drip not due (recent) → no drip.
{
  const d = decide(snap({ nowMs: 1_000 * HOUR, lastDripAtMs: 1_000 * HOUR - 1 * HOUR, openPuzzles: [puzzle()] }));
  check('drip not due → no drip', d.drips.length === 0);
}

// 10. Drip due but everything already dripped → no drip, with a note.
{
  const d = decide(snap({ openPuzzles: [puzzle({ dripped: true })] }));
  check('all dripped → no drip', d.drips.length === 0);
  check('all dripped → note present', d.notes.some((n) => n.includes('already been dripped')));
}

// 11. Determinism: same snapshot twice → identical decision.
{
  const s = snap({ openPuzzles: [puzzle({ failedAttemptsInWindow: 6, attempters: [attempter()] })] });
  check('deterministic', JSON.stringify(decide(s)) === JSON.stringify(decide(s)));
}

if (failures > 0) {
  console.error(`\nshowrunner decide: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner decide: OK — all assertions passed.');
