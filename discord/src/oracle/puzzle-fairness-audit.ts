/**
 * puzzle-fairness-audit.ts - launch-confidence checks for "too easy / impossible" puzzle risks.
 *
 * seedcheck proves accepted_answers can match. This audit checks the experience layer seedcheck cannot:
 *   - every puzzle row that can ask for an answer/action has tier-2 and tier-3 Whisper rescue text;
 *   - plugin-produced answer kinds use opaque, high-entropy tokens instead of human-readable phrases;
 *   - short human-typed answers are capped against brute-force attempts.
 */
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const seeds = resolve(here, '../../supabase/seeds');

const puzzleFiles = ['puzzles_seed.sql', 'progression_seed.sql'];
const rows = puzzleFiles.flatMap((file) => parsePuzzleRows(file, readFileSync(resolve(seeds, file), 'utf8')));
const hints = parseHints(readFileSync(resolve(seeds, 'hints_seed.sql'), 'utf8'));

const producedKinds = new Set(['behavior', 'object', 'code', 'spoken']);
const humanTypedKinds = new Set(['phrase', 'coords', 'url_token']);
const hintExempt = new Set([
  // This is a pure arrival/world-change acknowledgement, not a player-facing puzzle stall point.
  'record-receives',
]);

const missingHints = rows.filter((row) => !hintExempt.has(row.key) && !hasHintTiers(row.key, hints, [2, 3]));
const weakOpaqueTokens = rows.filter((row) =>
  producedKinds.has(row.answerKind) && row.answers.some((answer) => !isOpaqueToken(answer)),
);
const uncappedShortTyped = rows.filter((row) =>
  humanTypedKinds.has(row.answerKind) &&
  row.maxAttempts === null &&
  row.answers.some((answer) => isShortTypedAnswer(answer)),
);
const emptyAnswers = rows.filter((row) => row.answers.length === 0);

let failed = false;

if (missingHints.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - puzzle row(s) missing tier-2 and tier-3 Whisper rescue text:');
  for (const row of missingHints) console.error(`  - ${rowLabel(row)} hints=${tierLabel(hints.get(row.key))}`);
}

if (weakOpaqueTokens.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - plugin-produced answer kind(s) expose human-readable/bruteforceable tokens:');
  for (const row of weakOpaqueTokens) console.error(`  - ${rowLabel(row)} answers=[${row.answers.join(' | ')}]`);
}

if (uncappedShortTyped.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - short human-typed answer(s) need max_attempts caps:');
  for (const row of uncappedShortTyped) console.error(`  - ${rowLabel(row)} answers=[${row.answers.join(' | ')}]`);
}

if (emptyAnswers.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - puzzle row(s) with no accepted answer:');
  for (const row of emptyAnswers) console.error(`  - ${rowLabel(row)}`);
}

if (failed) process.exit(1);

const hinted = rows.filter((row) => hasHintTiers(row.key, hints, [2, 3])).length;
const opaque = rows.filter((row) => producedKinds.has(row.answerKind)).length;
const shortTypedCapped = rows.filter((row) =>
  humanTypedKinds.has(row.answerKind) &&
  row.maxAttempts !== null &&
  row.answers.some((answer) => isShortTypedAnswer(answer)),
).length;

console.log(`puzzlefairness: OK - ${rows.length} puzzle rows audited.`);
console.log(`  hint coverage: ${hinted}/${rows.length} rows have tier-2 + tier-3 rescue text (${hintExempt.size} exempt)`);
console.log(`  opaque plugin tokens: ${opaque} produced-kind row(s) protected`);
console.log(`  short typed caps: ${shortTypedCapped} short typed row(s) capped`);

interface PuzzleRow {
  key: string;
  title: string;
  answers: string[];
  outcome: string;
  answerKind: string;
  movement: number;
  active: boolean;
  maxAttempts: number | null;
  file: string;
}

function parsePuzzleRows(file: string, seedSql: string): PuzzleRow[] {
  const sql = stripLineComments(seedSql);
  const starts: { key: string; idx: number }[] = [];
  const keyRe = /\(\s*'([a-z0-9-]+)'\s*,\s*'((?:[^']|'')*)'\s*,\s*array\s*\[/g;
  let match: RegExpExecArray | null;
  while ((match = keyRe.exec(sql))) starts.push({ key: match[1]!, idx: match.index });

  const parsed: PuzzleRow[] = [];
  for (let i = 0; i < starts.length; i += 1) {
    const start = starts[i]!;
    const end = i + 1 < starts.length ? starts[i + 1]!.idx : sql.length;
    const body = sql.slice(start.idx, end);
    const head = body.match(/\(\s*'([a-z0-9-]+)'\s*,\s*'((?:[^']|'')*)'\s*,\s*array\s*\[([\s\S]*?)\]\s*,\s*'([a-z_]+)'/);
    if (!head) throw new Error(`puzzlefairness: could not parse row head for ${start.key}`);

    const withKind = [...body.matchAll(/,\s*'([a-z_]+)'\s*,\s*(\d+)\s*,\s*(true|false)\s*,\s*(null|\d+)\s*\)/g)];
    const oldShape = [...body.matchAll(/,\s*(\d+)\s*,\s*(true|false)\s*,\s*(null|\d+)\s*\)/g)];
    const kindTail = withKind.at(-1);
    const oldTail = oldShape.at(-1);
    if (!oldTail) throw new Error(`puzzlefairness: could not parse row tail for ${start.key}`);

    const answerKind = kindTail ? kindTail[1]! : 'phrase';
    const movement = Number(kindTail ? kindTail[2] : oldTail[1]);
    const active = (kindTail ? kindTail[3] : oldTail[2]) === 'true';
    const maxRaw = (kindTail ? kindTail[4] : oldTail[3])!;

    parsed.push({
      key: head[1]!,
      title: sqlString(head[2]!),
      answers: [...head[3]!.matchAll(/'((?:[^']|'')*)'/g)].map((m) => sqlString(m[1]!)),
      outcome: head[4]!,
      answerKind,
      movement,
      active,
      maxAttempts: maxRaw === 'null' ? null : Number(maxRaw),
      file,
    });
  }
  return parsed;
}

function parseHints(seedSql: string): Map<string, Set<number>> {
  const hints = new Map<string, Set<number>>();
  const sql = stripLineComments(seedSql);
  const rowRe = /\(\s*'([a-z0-9-]+)'\s*,\s*(\d+)\s*,\s*'((?:[^']|'')*)'\s*\)/g;
  let row: RegExpExecArray | null;
  while ((row = rowRe.exec(sql))) {
    const tiers = hints.get(row[1]!) ?? new Set<number>();
    tiers.add(Number(row[2]));
    hints.set(row[1]!, tiers);
  }
  return hints;
}

function hasHintTiers(key: string, allHints: Map<string, Set<number>>, tiers: number[]): boolean {
  const found = allHints.get(key);
  return !!found && tiers.every((tier) => found.has(tier));
}

function isOpaqueToken(answer: string): boolean {
  const compact = answer.replace(/\s+/g, '');
  return compact.length >= 16 && /[a-z]/.test(compact) && /\d/.test(compact);
}

function isShortTypedAnswer(answer: string): boolean {
  const words = answer.trim().split(/\s+/).filter(Boolean);
  const alnum = answer.replace(/\s+/g, '');
  return alnum.length <= 8 || words.length <= 2;
}

function tierLabel(tiers: Set<number> | undefined): string {
  return tiers ? [...tiers].sort((a, b) => a - b).join(',') : '<none>';
}

function rowLabel(row: PuzzleRow): string {
  const active = row.active ? 'active' : 'staged';
  const cap = row.maxAttempts === null ? 'uncapped' : `cap=${row.maxAttempts}`;
  return `${row.file}:${row.key} (${row.answerKind}, ${row.outcome}, M${row.movement}, ${active}, ${cap})`;
}

function stripLineComments(sql: string): string {
  return sql.replace(/--[^\n]*/g, '');
}

function sqlString(value: string): string {
  return value.replace(/''/g, "'");
}
