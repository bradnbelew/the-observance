/**
 * Deterministic release chaos simulation.
 *
 * Runs thousands of generated player/director states through the same pure gates,
 * normalization, and showrunner policy used in production. This is property-style coverage:
 * no network, no database, no Math.random, and the failing seed is always reproducible.
 */
import { normalizeAnswer } from '../oracle/normalize.js';
import { flagsSatisfied } from '../oracle/gate.js';
import { decide } from './decide.js';
import type { OutcomeType, Snapshot, SnapshotPuzzle } from './types.js';

const MOVERS = new Set<OutcomeType>(['next_clue', 'main_beat', 'side_quest']);
const OUTCOMES: OutcomeType[] = ['next_clue', 'main_beat', 'side_quest', 'lore', 'dead_end', 'unknown'];
let assertions = 0;
let failures = 0;

function check(condition: boolean, label: string, seed: number): void {
  assertions += 1;
  if (condition) return;
  failures += 1;
  console.error(`  FAIL seed=${seed} ${label}`);
}

function lcg(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (Math.imul(state, 1664525) + 1013904223) >>> 0;
    return state / 0x100000000;
  };
}

function integer(rng: () => number, maxExclusive: number): number {
  return Math.floor(rng() * maxExclusive);
}

function bool(rng: () => number, probability = 0.5): boolean {
  return rng() < probability;
}

function makePuzzle(rng: () => number, index: number): SnapshotPuzzle {
  const attempters = Array.from({ length: integer(rng, 6) }, (_, player) => ({
    playerId: `p${player}`,
    act: 1 + integer(rng, 5),
    whisperRemaining: integer(rng, 3),
    nextTier: 1 + integer(rng, 4),
    nextTierHintExists: bool(rng, 0.8),
  }));
  const puzzle: SnapshotPuzzle = {
    puzzleKey: `generated-${index}`,
    movement: 1 + integer(rng, 5),
    outcomeType: OUTCOMES[integer(rng, OUTCOMES.length)]!,
    forgeable: bool(rng),
    failedAttemptsInWindow: integer(rng, 15),
    solvedInWindow: bool(rng, 0.15),
    attempters,
    dripped: bool(rng, 0.3),
    flagsOpen: bool(rng, 0.85),
  };
  if (bool(rng, 0.35)) puzzle.requiresQuorum = 1 + integer(rng, 8);
  return puzzle;
}

function makeSnapshot(seed: number): Snapshot {
  const rng = lcg(seed ^ 0xa5a5a5a5);
  const nowMs = 10_000_000 + integer(rng, 5_000_000);
  const interval = 1_000 + integer(rng, 100_000);
  const last = bool(rng, 0.2) ? null : nowMs - integer(rng, interval * 2 + 1);
  return {
    nowMs,
    asleep: bool(rng, 0.08),
    mode: bool(rng) ? 'auto' : 'confirm',
    currentAct: 1 + integer(rng, 5),
    openPuzzles: Array.from({ length: 1 + integer(rng, 15) }, (_, i) => makePuzzle(rng, i)),
    lastDripAtMs: last,
    stallFailedThreshold: 3 + integer(rng, 7),
    dripIntervalMs: interval,
    activeRosterSize: integer(rng, 9),
    prologue: { curatorialAllowed: bool(rng, 0.9) },
    reckoning: [
      { state: 'tight' as const, cadenceMult: 1.4, tone: 'cold' as const },
      { state: 'even' as const, cadenceMult: 1, tone: 'plain' as const },
      { state: 'loose' as const, cadenceMult: 0.65, tone: 'warm' as const },
    ][integer(rng, 3)],
  };
}

// 1,200 independently generated director/player states.
for (let seed = 1; seed <= 1_200; seed += 1) {
  const snapshot = makeSnapshot(seed);
  const before = JSON.stringify(snapshot);
  const first = decide(snapshot);
  const second = decide(snapshot);

  check(JSON.stringify(first) === JSON.stringify(second), 'decision is deterministic', seed);
  check(JSON.stringify(snapshot) === before, 'decision does not mutate its snapshot', seed);
  check(first.drips.length <= 1, 'at most one clue drip per tick', seed);

  if (snapshot.asleep) {
    check(first.drips.length === 0 && first.gifts.length === 0, 'kill-switch suppresses all actions', seed);
    continue;
  }

  const giftKeys = new Set<string>();
  for (const gift of first.gifts) {
    const puzzle = snapshot.openPuzzles.find((row) => row.puzzleKey === gift.puzzleKey);
    const attempter = puzzle?.attempters.find((row) => row.playerId === gift.playerId);
    check(!!puzzle && !puzzle.solvedInWindow, 'gift belongs to an unsolved-window puzzle', seed);
    check((puzzle?.failedAttemptsInWindow ?? -1) >= snapshot.stallFailedThreshold, 'gift requires measured stall', seed);
    check(attempter?.whisperRemaining === 0, 'gift requires an exhausted whisper balance', seed);
    check(attempter?.nextTierHintExists === true, 'gift never invents an unauthored hint', seed);
    const key = `${gift.playerId}:${gift.puzzleKey}`;
    check(!giftKeys.has(key), 'one gift per player/puzzle/tick', seed);
    giftKeys.add(key);
  }

  for (const drip of first.drips) {
    const puzzle = snapshot.openPuzzles.find((row) => row.puzzleKey === drip.puzzleKey);
    check(!!puzzle, 'drip references a real open puzzle', seed);
    check(puzzle?.dripped === false, 'drip never repeats an already-dripped row', seed);
    check(puzzle?.flagsOpen !== false, 'drip never points through a closed flag gate', seed);
    check(
      puzzle?.requiresQuorum == null || snapshot.activeRosterSize! >= puzzle.requiresQuorum,
      'drip never points at an impossible quorum',
      seed,
    );
    check(!!puzzle && (puzzle.forgeable || MOVERS.has(puzzle.outcomeType)), 'drip is forgeable or story-advancing', seed);
    check(drip.staged === (snapshot.mode === 'confirm'), 'approval mode is preserved', seed);
  }

  const effectiveInterval = snapshot.dripIntervalMs * (snapshot.reckoning?.cadenceMult ?? 1);
  const cadenceDue = snapshot.lastDripAtMs == null || snapshot.nowMs - snapshot.lastDripAtMs >= effectiveInterval;
  if (!cadenceDue || snapshot.prologue?.curatorialAllowed === false) {
    check(first.drips.length === 0, 'cadence/prologue interlock suppresses drip', seed);
  }
}

// 600 generated flag maps: the oracle gate is exactly all-of and fail-closed.
for (let seed = 1; seed <= 600; seed += 1) {
  const rng = lcg(seed ^ 0x1f2e3d4c);
  const required: Record<string, true> = {};
  const flags: Record<string, unknown> = {};
  const count = integer(rng, 9);
  for (let i = 0; i < count; i += 1) {
    required[`flag_${i}`] = true;
    if (bool(rng, 0.75)) flags[`flag_${i}`] = bool(rng, 0.85);
  }
  const expected = Object.keys(required).every((key) => Boolean(flags[key]));
  check(flagsSatisfied(required, flags) === expected, 'flag gate matches all-of truth predicate', seed);
}

// 500 hostile-but-reasonable answer spellings: Unicode width, case, punctuation, and whitespace.
const CANONICAL = [
  'descend and bow at the unbroken light',
  'the one who turned away',
  'witness before accepting',
  'six return one is not kept',
  'where the reeds fold back',
  'i was not kept',
  'count the fires before you sleep',
];

function fullwidth(char: string): string {
  const code = char.charCodeAt(0);
  return code >= 0x21 && code <= 0x7e ? String.fromCharCode(code + 0xfee0) : char;
}

for (let seed = 1; seed <= 500; seed += 1) {
  const rng = lcg(seed ^ 0x0badc0de);
  const canonical = CANONICAL[integer(rng, CANONICAL.length)]!;
  const words = canonical.split(' ').map((word) => [...word].map((char) => {
    if (bool(rng, 0.18)) return fullwidth(char.toUpperCase());
    return bool(rng, 0.5) ? char.toUpperCase() : char;
  }).join(''));
  const separators = [' ', '  ', ', ', ' / ', '...'];
  const variant = `${bool(rng) ? '  ' : ''}${words.join(separators[integer(rng, separators.length)]!)}${bool(rng) ? '!' : ''}`;
  check(normalizeAnswer(variant) === canonical, 'reasonable answer spelling normalizes to canon', seed);
}

if (failures > 0) {
  console.error(`chaos simulation: FAILED - ${failures}/${assertions} invariant checks failed`);
  process.exit(1);
}

console.log(`chaos simulation: OK - 2,300 generated scenarios / ${assertions} invariant checks`);
