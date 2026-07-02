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
    puzzleKey: 'stone-vaun', movement: 2, outcomeType: 'next_clue', forgeable: true,
    failedAttemptsInWindow: 0,
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

// 7. First-ever drip (lastDripAtMs null) → among forgeable nodes, story-shape first, then
//    (movement asc, key asc). All three here are forgeable cipher nodes; the next_clue node
//    outranks the lore node even though the lore key sorts earlier alphabetically.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'stone-vaun', movement: 2, outcomeType: 'lore' }),       // key sorts first, but lore
      puzzle({ puzzleKey: 'stone-orin', movement: 2, outcomeType: 'next_clue' }),   // a MOVER → wins
      puzzle({ puzzleKey: 'stone-sella', movement: 2, outcomeType: 'side_quest' }),
    ],
  }));
  check('first drip → exactly one', d.drips.length === 1);
  check('drip prefers a story-advancing node over lore', d.drips[0]?.puzzleKey === 'stone-orin');
  check('drip carries forgeable flag', d.drips[0]?.forgeable === true);
  check('confirm mode → staged', d.drips[0]?.staged === true);
}

// 7b. P0-7 (C2): the dead_end that currently sorts FIRST by key must NEVER open the arc.
//    m1-named-habit is the live seed's alphabetically-first Movement-I row (a dead_end). Even
//    when present and forgeable, a real story-advancing node must be chosen as the opener.
{
  const d = decide(snap({
    openPuzzles: [
      // m1-named-habit sorts before stone-* by key, and is the lowest movement — the OLD
      // (movement, key) order would open on it. It is a dead_end, so the new rule rejects it
      // as the opener. (Forced forgeable=true here to isolate the ORDERING rule from the
      // pool filter tested in 7c.)
      puzzle({ puzzleKey: 'm1-named-habit', movement: 1, outcomeType: 'dead_end', forgeable: true }),
      puzzle({ puzzleKey: 'stone-mara', movement: 2, outcomeType: 'next_clue' }),
    ],
  }));
  check('drip never opens on a dead_end', d.drips[0]?.puzzleKey !== 'm1-named-habit');
  check('drip opens on the story-advancing node', d.drips[0]?.puzzleKey === 'stone-mara');
}

// 7c. P0-7 (C3): found-document / non-forgeable rows are EXCLUDED from the drip pool. With
//    only non-forgeable rows open, nothing drips (and a note is left), even though the cadence
//    is due — the showrunner never "announce, read it here" about a node with no clue card.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'm1-record-opens', movement: 1, outcomeType: 'lore', forgeable: false }),
      puzzle({ puzzleKey: 'm1-named-habit', movement: 1, outcomeType: 'dead_end', forgeable: false }),
    ],
  }));
  check('found-document rows excluded → no drip', d.drips.length === 0);
  check('excluded-only pool → "no forgeable puzzle" note',
    d.notes.some((n) => n.includes('no forgeable')));
}

// 7d. P0-7: a mixed pool drips the forgeable node and ignores the (earlier-sorting) non-forgeable one.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'm1-named-habit', movement: 1, outcomeType: 'dead_end', forgeable: false }),
      puzzle({ puzzleKey: 'stone-vaun', movement: 2, outcomeType: 'lore', forgeable: true }),
    ],
  }));
  check('mixed pool → drips the forgeable node', d.drips[0]?.puzzleKey === 'stone-vaun');
  check('mixed pool → exactly one drip', d.drips.length === 1);
}

// 7e. AUDIT #7 (salience dead-air fix): after the forgeable ciphers are spent, a non-forgeable
//    STORY-ADVANCING back-half node is surfaced (via apply.ts's in-world report line) so a pointer
//    keeps flowing — no dead-air cliff. It carries forgeable=false, so apply.ts routes it to the
//    report line, not a forged card.
{
  const d = decide(snap({
    openPuzzles: [
      // The live back-half spine after Movement II: all non-forgeable, but movers.
      puzzle({ puzzleKey: 'undercroft-descent', movement: 3, outcomeType: 'main_beat', forgeable: false }),
      puzzle({ puzzleKey: 'seventh-shrine', movement: 3, outcomeType: 'side_quest', forgeable: false }),
    ],
  }));
  check('back-half: non-forgeable mover is surfaced (dead-air fix)', d.drips.length === 1);
  check('back-half: prefers the story-advancing main_beat', d.drips[0]?.puzzleKey === 'undercroft-descent');
  check('back-half: dripped node carries forgeable=false (routes to report line)', d.drips[0]?.forgeable === false);
}

// 7f. AUDIT #7 guardrail: a non-forgeable TERMINAL row (lore/dead_end found-document) is STILL never
//    dripped even when it is the only open row — a report line about it would point at a thread that
//    opens nothing. Pool empty → no drip, "pool empty" note.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'undercroft-fog', movement: 3, outcomeType: 'lore', forgeable: false }),
      puzzle({ puzzleKey: 'name-where', movement: 2, outcomeType: 'dead_end', forgeable: false }),
    ],
  }));
  check('back-half: terminal non-forgeable rows never drip', d.drips.length === 0);
  check('back-half: empty pool leaves a note', d.notes.some((n) => n.includes('no forgeable or story-advancing')));
}

// 7g. AUDIT #7: within the SAME story-shape rank, a forgeable card beats a non-forgeable report-line
//    node (better player experience), regardless of key/movement order.
{
  const d = decide(snap({
    openPuzzles: [
      // both next_clue (rank 0); the non-forgeable one sorts earlier by key + lower movement, but the
      // forgeable card must still win the tiebreak.
      puzzle({ puzzleKey: 'aaa-report-node', movement: 2, outcomeType: 'next_clue', forgeable: false }),
      puzzle({ puzzleKey: 'stone-mara', movement: 3, outcomeType: 'next_clue', forgeable: true }),
    ],
  }));
  check('same rank → forgeable card preferred over report line', d.drips[0]?.puzzleKey === 'stone-mara');
}

// 7h. S-F SALIENCE: among SAME story-shape peers, the thread the group is actually pulling on wins.
//    Both are next_clue (rank 0) and forgeable. The higher-engagement node (more distinct attempters)
//    must be dripped over the lower-engagement one, EVEN THOUGH the low-engagement key sorts earlier
//    and has lower movement — proving salience outranks the old (forgeable, movement, key) tiebreakers.
{
  const d = decide(snap({
    openPuzzles: [
      // sorts first by key + lower movement, but NOBODY is on it → low salience.
      puzzle({ puzzleKey: 'aaa-quiet', movement: 2, outcomeType: 'next_clue', attempters: [] }),
      // three distinct players are actively pulling on this thread → high salience, wins.
      puzzle({
        puzzleKey: 'zzz-hot', movement: 3, outcomeType: 'next_clue',
        attempters: [attempter({ playerId: 'a' }), attempter({ playerId: 'b' }), attempter({ playerId: 'c' })],
      }),
    ],
  }));
  check('salience: higher-engagement same-rank node is dripped first', d.drips[0]?.puzzleKey === 'zzz-hot');
}

// 7i. S-F SALIENCE tiebreak: equal distinct-attempter count → failed-attempts-in-window (recency)
//    breaks the tie. The thread being hammered harder this window rises.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'aaa-cool', movement: 2, outcomeType: 'next_clue', attempters: [attempter({ playerId: 'a' })], failedAttemptsInWindow: 1 }),
      puzzle({ puzzleKey: 'zzz-warm', movement: 2, outcomeType: 'next_clue', attempters: [attempter({ playerId: 'a' })], failedAttemptsInWindow: 9 }),
    ],
  }));
  check('salience: equal attempters → more failed attempts (recency) wins', d.drips[0]?.puzzleKey === 'zzz-warm');
}

// 7j. S-F SALIENCE never overrides STORY SHAPE: a heavily-engaged dead_end must STILL never open the
//    arc — shape is the first sort key, so a quiet mover beats a hammered terminal.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({
        puzzleKey: 'busy-dead', movement: 1, outcomeType: 'dead_end', forgeable: true, failedAttemptsInWindow: 99,
        attempters: [attempter({ playerId: 'a' }), attempter({ playerId: 'b' }), attempter({ playerId: 'c' }), attempter({ playerId: 'd' })],
      }),
      puzzle({ puzzleKey: 'quiet-mover', movement: 5, outcomeType: 'next_clue', attempters: [] }),
    ],
  }));
  check('salience never opens on a dead_end (shape dominates engagement)', d.drips[0]?.puzzleKey === 'quiet-mover');
}

// 7k. S-F ROSTER GUARD: a convergence/quorum-gated node is EXCLUDED while the active roster is below
//    its quorum (never surface a thread the present group cannot close → dead-air). Here the only OTHER
//    open row is the quorum-gated one; with the roster under quorum the pool is empty → no drip.
{
  const d = decide(snap({
    activeRosterSize: 2,
    openPuzzles: [
      puzzle({ puzzleKey: 'group-bow', movement: 3, outcomeType: 'main_beat', requiresQuorum: 5 }),
    ],
  }));
  check('roster guard: sub-quorum → convergence node excluded → no drip', d.drips.length === 0);
}

// 7l. S-F ROSTER GUARD: once the roster meets the quorum, the same node becomes eligible and drips.
{
  const d = decide(snap({
    activeRosterSize: 5,
    openPuzzles: [
      puzzle({ puzzleKey: 'group-bow', movement: 3, outcomeType: 'main_beat', requiresQuorum: 5 }),
    ],
  }));
  check('roster guard: roster meets quorum → convergence node drips', d.drips[0]?.puzzleKey === 'group-bow');
}

// 7m. S-F ROSTER GUARD is optional/no-op: with a quorum on the puzzle but NO activeRosterSize on the
//    snapshot (the back-compat default), the guard does nothing and the node still drips.
{
  const d = decide(snap({
    openPuzzles: [
      puzzle({ puzzleKey: 'group-bow', movement: 3, outcomeType: 'main_beat', requiresQuorum: 5 }),
    ],
  }));
  check('roster guard: absent activeRosterSize → guard is a no-op (still drips)', d.drips[0]?.puzzleKey === 'group-bow');
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
