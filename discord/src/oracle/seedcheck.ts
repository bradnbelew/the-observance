/**
 * Seed normalization parity check - makes the ORACLE.md authoring rule executable.
 *
 * The resolver matches a player's answer by normalizing their raw input and testing
 * whole-string set-membership against puzzles.accepted_answers. So every accepted_answers
 * entry must already be stored in normalized form. If it is not, that clue can never be
 * solved on either surface, and the failure is silent.
 *
 * This also checks thread archive reveal gates: an archive card must not wait on a puzzle
 * row that is permanently inactive. Staged rows are allowed only when a later seed activates
 * them with the standard active=true update.
 *
 * Run: npx tsx src/oracle/seedcheck.ts
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { normalizeAnswer } from './normalize.js';

const here = dirname(fileURLToPath(import.meta.url));
const seeds = resolve(here, '../../supabase/seeds');
const SEED = resolve(seeds, 'puzzles_seed.sql');
const THREAD_CARDS_SEED = resolve(seeds, 'thread_cards.sql');
const HINTS_SEED = resolve(seeds, 'hints_seed.sql');
const ACTIVATION_SEEDS = [
  resolve(seeds, 'metapuzzle_seed.sql'),
  resolve(seeds, 'progression_seed.sql'),
];

const sql = readFileSync(SEED, 'utf8');
const sqlWithoutComments = stripLineComments(sql);

// Pull every `array[ ... ]` block. In this seed, those are accepted_answers blocks.
const arrayBlocks = [...sqlWithoutComments.matchAll(/array\[([^\]]*)\]/gi)]
  .map((m) => m[1])
  .filter((b): b is string => b !== undefined);

type Violation = { value: string; normalized: string; reason: string };
const violations: Violation[] = [];
let total = 0;

for (const block of arrayBlocks) {
  for (const m of block.matchAll(/'([^']*)'/g)) {
    const value = m[1];
    if (value === undefined) continue;
    total += 1;
    if (value.trim() === '') {
      violations.push({ value, normalized: '', reason: 'empty/blank answer (never matches)' });
      continue;
    }
    const normalized = normalizeAnswer(value);
    if (normalized !== value) {
      violations.push({
        value,
        normalized,
        reason: `not pre-normalized (would never match; store "${normalized}")`,
      });
    }
  }
}

const puzzleState = parsePuzzleState(sqlWithoutComments);
const activatedBySeed = parseSeedActivatedKeys(ACTIVATION_SEEDS);
const threadCardReveals = parseThreadCardReveals(readFileSync(THREAD_CARDS_SEED, 'utf8'));
const hintKeys = parseHintKeys(readFileSync(HINTS_SEED, 'utf8'));
const archiveGateViolations = threadCardReveals.flatMap((reveal) => {
  if (reveal.revealedBySolve === null) return [];
  const active = puzzleState.get(reveal.revealedBySolve);
  if (active === undefined) {
    return [`${reveal.cardKey} waits on missing puzzle ${reveal.revealedBySolve}`];
  }
  if (!active && !activatedBySeed.has(reveal.revealedBySolve)) {
    return [`${reveal.cardKey} waits on permanently inactive puzzle ${reveal.revealedBySolve}`];
  }
  return [];
});
const retiredHintViolations = [...hintKeys].flatMap((key) => {
  const active = puzzleState.get(key);
  if (active === false && !activatedBySeed.has(key)) return [`${key} has Whisper hints but is permanently inactive`];
  return [];
});

if (total === 0) {
  console.error('seedcheck: FAILED - found 0 accepted_answers in the seed (extraction broke?).');
  process.exit(1);
}

if (violations.length > 0) {
  console.error(`seedcheck: FAILED - ${violations.length}/${total} accepted_answers are not seed-safe:`);
  for (const v of violations) {
    console.error(`  - "${v.value}" -> ${v.reason}`);
  }
  process.exit(1);
}

if (archiveGateViolations.length > 0) {
  console.error(`seedcheck: FAILED - ${archiveGateViolations.length} thread archive reveal gate(s) cannot open:`);
  for (const v of archiveGateViolations) console.error(`  - ${v}`);
  process.exit(1);
}

if (retiredHintViolations.length > 0) {
  console.error(`seedcheck: FAILED - ${retiredHintViolations.length} retired puzzle hint row(s) remain live:`);
  for (const v of retiredHintViolations) console.error(`  - ${v}`);
  process.exit(1);
}

console.log(`seedcheck: OK - all ${total} accepted_answers across ${arrayBlocks.length} puzzles are pre-normalized and non-empty.`);
console.log(`seedcheck: OK - ${threadCardReveals.length} thread archive reveal gate(s) point at live or staged-live puzzles.`);
console.log(`seedcheck: OK - ${hintKeys.size} hinted puzzle(s) do not target permanently retired rows.`);

function parsePuzzleState(seedSql: string): Map<string, boolean> {
  const starts: { key: string; idx: number }[] = [];
  const keyRe = /(?:^|\n)\s*\(\s*'([a-z0-9-]+)'/g;
  let match: RegExpExecArray | null;
  while ((match = keyRe.exec(seedSql))) starts.push({ key: match[1]!, idx: match.index });

  const state = new Map<string, boolean>();
  for (let i = 0; i < starts.length; i += 1) {
    const start = starts[i]!;
    const end = i + 1 < starts.length ? starts[i + 1]!.idx : seedSql.length;
    const body = seedSql.slice(start.idx, end);
    const tail = [...body.matchAll(/,\s*(?:'([a-z_]+)'\s*,\s*)?(\d+)\s*,\s*(true|false)\s*,\s*(null|\d+)\s*\)/g)].at(-1);
    if (!tail) continue;
    state.set(start.key, tail[3] === 'true');
  }
  return state;
}

function parseSeedActivatedKeys(files: string[]): Set<string> {
  const keys = new Set<string>();
  for (const file of files) {
    const text = readFileSync(file, 'utf8');
    const updateRe = /update\s+public\.puzzles\s+set\s+active\s*=\s*true\s+where\s+puzzle_key\s+in\s*\(([\s\S]*?)\)\s*;/gi;
    let update: RegExpExecArray | null;
    while ((update = updateRe.exec(text))) {
      for (const key of update[1]!.matchAll(/'([a-z0-9-]+)'/g)) keys.add(key[1]!);
    }
  }
  return keys;
}

function parseThreadCardReveals(seedSql: string): { cardKey: string; revealedBySolve: string | null }[] {
  const reveals: { cardKey: string; revealedBySolve: string | null }[] = [];
  const rowRe = /\(\s*'([a-z0-9-]+)'\s*,[\s\S]*?,\s*(?:array\s*\[[\s\S]*?\]|null)\s*,\s*(null|'([a-z0-9-]+)')\s*,\s*(?:null|'[^']*')\s*,\s*\d+\s*\)/g;
  let row: RegExpExecArray | null;
  while ((row = rowRe.exec(seedSql))) {
    reveals.push({ cardKey: row[1]!, revealedBySolve: row[2] === 'null' ? null : row[3]! });
  }
  return reveals;
}

function parseHintKeys(seedSql: string): Set<string> {
  const keys = new Set<string>();
  const sql = stripLineComments(seedSql);
  const rowRe = /\(\s*'([a-z0-9-]+)'\s*,\s*\d+\s*,\s*'((?:[^']|'')*)'\s*\)/g;
  let row: RegExpExecArray | null;
  while ((row = rowRe.exec(sql))) keys.add(row[1]!);
  return keys;
}

function stripLineComments(text: string): string {
  return text.replace(/--[^\n]*/g, '');
}
