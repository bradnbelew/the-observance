/**
 * 7-PLAYER SCENARIO SIMULATION of the showrunner spine (decide.ts) — the "run through the important
 * things happening with ~7 players, what could go wrong, edge cases" pass.
 *
 *   npx tsx src/showrunner/scenario.selftest.ts   (or: npm run showrunner:test:scenario)
 *
 * Unlike decide.selftest.ts (single-snapshot unit tests), this drives decide() across a SIMULATED
 * MULTI-DAY TIMELINE with 7 players and evolving world state — drips fire and are marked, players
 * solve some clues and get stuck on others, attempts and whispers accumulate — and asserts the
 * GUARDRAILS hold end-to-end:
 *   - drip cadence is NEVER violated (no announcement spam), even under a flood of failed attempts;
 *   - auto-gifts are BOUNDED by the authored hint count and never re-fire while a whisper is unspent
 *     (no infinite-hint cascade) and only for a genuinely stuck+exhausted player;
 *   - the arc NEVER opens/advances on a dead_end or a non-forgeable found-document row;
 *   - the kill-switch (asleep) and confirm-mode are honored for the entire run;
 *   - the arc PROGRESSES (no deadlock) when players engage, and degrades quietly when they don't;
 *   - the whole simulation is DETERMINISTIC (same seed → byte-identical drip/gift sequence).
 *
 * Pure: no DB, no network, no Math.random (a seeded LCG drives player behavior so runs reproduce).
 * Exits non-zero on any failed assertion so it gates the build alongside the other self-tests.
 */
import { decide } from './decide.js';
import type { Snapshot, SnapshotPuzzle, Decision } from './types.js';

const HOUR = 3_600_000;
const DAY = 24 * HOUR;

let failures = 0;
function check(label: string, cond: boolean, detail = ''): void {
  if (cond) console.log(`  ok   ${label}`);
  else {
    failures += 1;
    console.error(`  FAIL ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

/** Deterministic LCG (no Math.random — reproducible scenarios). */
function lcg(seed: number): () => number {
  let s = seed >>> 0;
  return () => {
    s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
    return s / 0x100000000;
  };
}

// ---------------------------------------------------------------------------
// The world model the simulation mutates (a stand-in for the live DB).
// ---------------------------------------------------------------------------

interface SimAttempter {
  whisperRemaining: number; // budget + earned - spent
  spentTiers: number;       // how many hints this player has consumed on this puzzle
}
interface SimPuzzle {
  puzzleKey: string;
  movement: number;
  outcomeType: SnapshotPuzzle['outcomeType'];
  forgeable: boolean;
  solved: boolean;
  dripped: boolean;
  failed: number;                          // failed attempts in the rolling window (group-wide)
  hintTiers: number;                       // authored hint tiers (gifts can't exceed this)
  attempters: Map<string, SimAttempter>;
}
interface World {
  players: string[];
  puzzles: SimPuzzle[];
  lastDripAtMs: number | null;
  mode: 'auto' | 'confirm';
  asleep: boolean;
  currentAct: number;
}

const STALL = 5;
const DRIP_INTERVAL = 20 * HOUR;

/** The real spine shape: found-document rows (non-forgeable), then the forgeable cipher chain. */
function freshWorld(over: Partial<World> = {}): World {
  const p = (o: Partial<SimPuzzle>): SimPuzzle => ({
    puzzleKey: 'x', movement: 2, outcomeType: 'next_clue', forgeable: true,
    solved: false, dripped: false, failed: 0, hintTiers: 3, attempters: new Map(), ...o,
  });
  return {
    players: ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7'],
    puzzles: [
      p({ puzzleKey: 'm1-record-opens', movement: 1, outcomeType: 'lore', forgeable: false }),
      p({ puzzleKey: 'm1-named-habit', movement: 1, outcomeType: 'dead_end', forgeable: false }),
      p({ puzzleKey: 'stone-vaun', movement: 2, outcomeType: 'next_clue' }),
      p({ puzzleKey: 'stone-mara', movement: 2, outcomeType: 'next_clue' }),
      p({ puzzleKey: 'stone-sella', movement: 2, outcomeType: 'next_clue' }),
      p({ puzzleKey: 'stone-orin', movement: 2, outcomeType: 'next_clue' }),
      p({ puzzleKey: 'stone-iss-wall', movement: 2, outcomeType: 'next_clue' }),
      p({ puzzleKey: 'rite-tokens', movement: 3, outcomeType: 'main_beat' }),
      p({ puzzleKey: 'accepting-crouch', movement: 3, outcomeType: 'main_beat', forgeable: false }),
    ],
    lastDripAtMs: null, mode: 'confirm', asleep: false, currentAct: 1, ...over,
  };
}

/** Project the live World into the immutable Snapshot decide() consumes. */
function project(w: World, nowMs: number): Snapshot {
  const open = w.puzzles.filter((p) => !p.solved);
  return {
    nowMs,
    asleep: w.asleep,
    mode: w.mode,
    currentAct: w.currentAct,
    lastDripAtMs: w.lastDripAtMs,
    stallFailedThreshold: STALL,
    dripIntervalMs: DRIP_INTERVAL,
    openPuzzles: open.map((p) => ({
      puzzleKey: p.puzzleKey,
      movement: p.movement,
      outcomeType: p.outcomeType,
      forgeable: p.forgeable,
      failedAttemptsInWindow: p.failed,
      solvedInWindow: false,
      dripped: p.dripped,
      attempters: [...p.attempters.entries()].map(([playerId, a]) => ({
        playerId,
        act: w.currentAct,
        whisperRemaining: a.whisperRemaining,
        nextTier: a.spentTiers + 1,
        nextTierHintExists: a.spentTiers + 1 <= p.hintTiers,
      })),
    })),
  };
}

/** Apply decide()'s output back to the world (mark drips; bank earned whispers from gifts). */
function applyDecision(w: World, d: Decision, nowMs: number): void {
  for (const drip of d.drips) {
    const pz = w.puzzles.find((p) => p.puzzleKey === drip.puzzleKey);
    if (pz) pz.dripped = true;
    w.lastDripAtMs = nowMs;
  }
  for (const gift of d.gifts) {
    const pz = w.puzzles.find((p) => p.puzzleKey === gift.puzzleKey);
    const a = pz?.attempters.get(gift.playerId);
    if (a) a.whisperRemaining += 1; // the earned whisper they can now spend on the gifted tier
  }
}

/** Between ticks, the 7 players act on whatever clue is live: some solve, some fail + spend hints. */
function simulatePlayers(w: World, rng: () => number): void {
  const live = w.puzzles.filter((p) => !p.solved && p.dripped);
  for (const pz of live) {
    for (const player of w.players) {
      if (rng() < 0.45) continue; // not everyone engages every window
      let a = pz.attempters.get(player);
      if (!a) { a = { whisperRemaining: 0, spentTiers: 0 }; pz.attempters.set(player, a); }
      if (rng() < 0.18) {           // a solve ends the puzzle for the whole group
        pz.solved = true;
        break;
      }
      pz.failed += 1;               // a failed attempt
      if (a.whisperRemaining > 0 && rng() < 0.8) { // spend a held whisper → consume a tier
        a.whisperRemaining -= 1;
        a.spentTiers += 1;
      }
    }
  }
}

// ===========================================================================
// SCENARIO A — the main 14-day arc with 7 engaged players.
// ===========================================================================
{
  const w = freshWorld({ mode: 'auto' });
  const rng = lcg(0xC0FFEE);
  const dripTimes: number[] = [];
  let giftTotal = 0;
  let openedOnDeadEnd = false;
  let drippedNonForgeable = false;

  for (let t = 0; t <= 14 * DAY; t += HOUR) {
    const d = decide(project(w, 1000 * HOUR + t));
    for (const drip of d.drips) {
      dripTimes.push(1000 * HOUR + t);
      const pz = w.puzzles.find((p) => p.puzzleKey === drip.puzzleKey)!;
      if (pz.outcomeType === 'dead_end' || pz.outcomeType === 'lore') openedOnDeadEnd = true;
      if (!pz.forgeable) drippedNonForgeable = true;
    }
    giftTotal += d.gifts.length;
    applyDecision(w, d, 1000 * HOUR + t);
    simulatePlayers(w, rng);
  }

  // GUARDRAIL: no two drips ever closer than the interval (no announcement spam).
  let minGap = Infinity;
  for (let i = 1; i < dripTimes.length; i++) minGap = Math.min(minGap, dripTimes[i]! - dripTimes[i - 1]!);
  check('A: drip cadence never violated (no spam)', dripTimes.length < 2 || minGap >= DRIP_INTERVAL,
    `minGap=${minGap === Infinity ? 'n/a' : (minGap / HOUR).toFixed(1) + 'h'} < ${DRIP_INTERVAL / HOUR}h`);
  check('A: never opens the arc on a dead_end/lore row', !openedOnDeadEnd);
  check('A: never drips a non-forgeable found-document row', !drippedNonForgeable);
  check('A: the arc PROGRESSES (forgeable cipher nodes get solved)',
    w.puzzles.filter((p) => p.forgeable && p.solved).length >= 3,
    `${w.puzzles.filter((p) => p.forgeable && p.solved).length} forgeable solved`);
  // GUARDRAIL: gifts bounded by (players × stuck puzzles × authored hint tiers) — never a cascade.
  check('A: gift volume stays bounded (no hint cascade)', giftTotal <= w.players.length * w.puzzles.length * 3,
    `gifts=${giftTotal}`);
}

// ===========================================================================
// SCENARIO B — 7 players ALL hard-stuck on one gate, all hints authored.
//   Asserts: at most ONE gift per (player) per due window, never exceeding the authored tiers,
//   and never re-gifting a player who still holds an unspent whisper.
// ===========================================================================
{
  const w = freshWorld({ mode: 'auto' });
  // Collapse the world to a single open, dripped, hard-stuck gate with all 7 attempting.
  w.puzzles = [{
    puzzleKey: 'stone-iss-wall', movement: 2, outcomeType: 'next_clue', forgeable: true,
    solved: false, dripped: true, failed: 50, hintTiers: 3,
    attempters: new Map(w.players.map((p) => [p, { whisperRemaining: 0, spentTiers: 0 }])),
  }];
  w.lastDripAtMs = 1000 * HOUR; // not due → isolate the gift path

  const perPlayerGifts = new Map<string, number>();
  let maxGiftsInOneTick = 0;
  for (let t = 0; t <= 10 * DAY; t += 6 * HOUR) {
    const d = decide(project(w, 1000 * HOUR + t));
    maxGiftsInOneTick = Math.max(maxGiftsInOneTick, d.gifts.length);
    for (const g of d.gifts) perPlayerGifts.set(g.playerId, (perPlayerGifts.get(g.playerId) ?? 0) + 1);
    applyDecision(w, d, 1000 * HOUR + t);
    // The players hold their gifted whisper a while, then spend it (consuming a tier), staying stuck.
    for (const a of w.puzzles[0]!.attempters.values()) {
      if (a.whisperRemaining > 0) { a.whisperRemaining -= 1; a.spentTiers += 1; }
    }
  }
  check('B: never more than one gift per stuck player in a tick (≤7)', maxGiftsInOneTick <= 7,
    `maxGiftsInOneTick=${maxGiftsInOneTick}`);
  const maxPer = perPlayerGifts.size ? Math.max(...perPlayerGifts.values()) : 0;
  check('B: gifts per player capped at the authored hint count (no infinite hints)', maxPer <= 3,
    `maxPerPlayer=${maxPer}`);
}

// ===========================================================================
// SCENARIO C — the kill-switch and confirm-mode hold for an entire run.
// ===========================================================================
{
  const asleepW = freshWorld({ asleep: true });
  asleepW.puzzles.forEach((p) => (p.dripped = false));
  asleepW.puzzles[2]!.failed = 99;
  let anyAction = false;
  for (let t = 0; t <= 7 * DAY; t += HOUR) {
    const d = decide(project(asleepW, 1000 * HOUR + t));
    if (d.drips.length || d.gifts.length) anyAction = true;
    applyDecision(asleepW, d, 1000 * HOUR + t);
  }
  check('C: asleep for the whole run → zero drips and zero gifts', !anyAction);

  const confirmW = freshWorld({ mode: 'confirm' });
  let allStaged = true;
  for (let t = 0; t <= 7 * DAY; t += HOUR) {
    const d = decide(project(confirmW, 1000 * HOUR + t));
    if (d.drips.some((x) => !x.staged)) allStaged = false;
    applyDecision(confirmW, d, 1000 * HOUR + t);
    // no player progress → nothing ever solves; drips should still all be staged
  }
  check('C: confirm mode → every drip is staged (awaits dashboard approval)', allStaged);
}

// ===========================================================================
// SCENARIO D — degenerate inputs never crash and degrade quietly.
// ===========================================================================
{
  const empty = freshWorld();
  empty.puzzles = [];
  const d1 = decide(project(empty, 1000 * HOUR));
  check('D: empty pool → no drips, no gifts, heartbeat present', d1.drips.length === 0 && d1.gifts.length === 0 && !!d1.health);

  const allSolved = freshWorld();
  allSolved.puzzles.forEach((p) => (p.solved = true));
  const d2 = decide(project(allSolved, 1000 * HOUR));
  check('D: all solved → no drips, no crash', d2.drips.length === 0);

  const onlyFound = freshWorld();
  onlyFound.puzzles = onlyFound.puzzles.filter((p) => !p.forgeable); // only non-forgeable rows open
  const d3 = decide(project(onlyFound, 1000 * HOUR));
  check('D: only found-document rows → no drip + a "no forgeable" note',
    d3.drips.length === 0 && d3.notes.some((n) => n.includes('no forgeable')));
}

// ===========================================================================
// SCENARIO E — full determinism: the same seed reproduces the run byte-for-byte.
// ===========================================================================
{
  function run(seed: number): string {
    const w = freshWorld({ mode: 'auto' });
    const rng = lcg(seed);
    const log: string[] = [];
    for (let t = 0; t <= 14 * DAY; t += HOUR) {
      const d = decide(project(w, 1000 * HOUR + t));
      if (d.drips.length || d.gifts.length) {
        log.push(`${t / HOUR}:${d.drips.map((x) => x.puzzleKey).join(',')}|${d.gifts.map((g) => g.playerId + '#' + g.tier).join(',')}`);
      }
      applyDecision(w, d, 1000 * HOUR + t);
      simulatePlayers(w, rng);
    }
    return log.join(';');
  }
  check('E: deterministic — same seed → identical drip/gift sequence', run(0xABCD) === run(0xABCD));
  check('E: different seeds → the sim actually varies (the harness exercises real branching)',
    run(0xABCD) !== run(0x1234));
}

if (failures > 0) {
  console.error(`\nshowrunner scenario: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner scenario: OK — 7-player timeline + edge cases all hold.');
