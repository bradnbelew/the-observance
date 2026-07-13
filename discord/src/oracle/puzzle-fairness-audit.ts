/**
 * puzzle-fairness-audit.ts - launch-confidence checks for "too easy / impossible" puzzle risks.
 *
 * seedcheck proves accepted_answers can match. This audit checks the experience layer seedcheck cannot:
 *   - every puzzle row that can ask for an answer/action has tier-2 and tier-3 Whisper rescue text;
 *   - plugin-produced answer kinds use opaque, high-entropy tokens instead of human-readable phrases;
 *   - short human-typed answers are capped against brute-force attempts.
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const seeds = resolve(here, '../../supabase/seeds');

const puzzleFiles = ['puzzles_seed.sql', 'progression_seed.sql'];
const rows = puzzleFiles.flatMap((file) => parsePuzzleRows(file, readFileSync(resolve(seeds, file), 'utf8')));
const hints = parseHints(readFileSync(resolve(seeds, 'hints_seed.sql'), 'utf8'));
const activatedBySeed = parseSeedActivatedKeys(['metapuzzle_seed.sql', 'progression_seed.sql']);
const repoRoot = resolve(here, '../../..');
const producerCorpus = readCorpus([
  resolve(repoRoot, 'plugin/src/main'),
  resolve(repoRoot, 'discord/src'),
]);

const producedKinds = new Set(['behavior', 'object', 'code', 'spoken']);
const humanTypedKinds = new Set(['phrase', 'coords', 'url_token']);
const knownKinds = new Set([...producedKinds, ...humanTypedKinds, 'none']);
const hintExempt = new Set([
  // This is a pure arrival/world-change acknowledgement, not a player-facing puzzle stall point.
  'record-receives',
]);

const hintableRows = rows.filter((row) => row.active || activatedBySeed.has(row.key));
const missingHints = hintableRows.filter((row) => !hintExempt.has(row.key) && !hasHintTiers(row.key, hints, [2, 3]));
const weakOpaqueTokens = rows.filter((row) =>
  producedKinds.has(row.answerKind) && row.answers.some((answer) => !isOpaqueToken(answer)),
);
const uncappedShortTyped = rows.filter((row) =>
  humanTypedKinds.has(row.answerKind) &&
  row.maxAttempts === null &&
  row.answers.some((answer) => isShortTypedAnswer(answer)),
);
const emptyAnswers = rows.filter((row) => row.answers.length === 0);
const unknownKinds = rows.filter((row) => !knownKinds.has(row.answerKind));
const producedWithoutConsumer = rows.filter((row) =>
  producedKinds.has(row.answerKind) && row.answers.every((answer) => !producerCorpus.includes(answer)),
);

const inputContractFiles = {
  answerSign: readFileSync(resolve(repoRoot, 'plugin/src/main/java/com/observance/watcher/signal/listener/AnswerSignListener.java'), 'utf8'),
  structures: readFileSync(resolve(repoRoot, 'plugin/src/main/java/com/observance/watcher/structure/StructureTemplates.java'), 'utf8'),
  command: readFileSync(resolve(repoRoot, 'plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java'), 'utf8'),
  discordAnswer: readFileSync(resolve(repoRoot, 'discord/src/bot/commands/answer.ts'), 'utf8'),
  recordRoute: readFileSync(resolve(repoRoot, 'dashboard/src/app/record/terminal/inscribe/route.ts'), 'utf8'),
};
const inputContractFailures = validateInputContracts(inputContractFiles, humanTypedKinds, rows);

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

if (unknownKinds.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - puzzle row(s) use an unsupported answer kind:');
  for (const row of unknownKinds) console.error(`  - ${rowLabel(row)}`);
}

if (producedWithoutConsumer.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - produced puzzle row(s) have no runtime consumer for any accepted token:');
  for (const row of producedWithoutConsumer) console.error(`  - ${rowLabel(row)}`);
}

if (inputContractFailures.length > 0) {
  failed = true;
  console.error('puzzlefairness: FAILED - player answer/input surface contract is incomplete:');
  for (const failure of inputContractFailures) console.error(`  - ${failure}`);
}

if (failed) process.exit(1);

const hinted = hintableRows.filter((row) => hasHintTiers(row.key, hints, [2, 3])).length;
const opaque = rows.filter((row) => producedKinds.has(row.answerKind)).length;
const shortTypedCapped = rows.filter((row) =>
  humanTypedKinds.has(row.answerKind) &&
  row.maxAttempts !== null &&
  row.answers.some((answer) => isShortTypedAnswer(answer)),
).length;

console.log(`puzzlefairness: OK - ${rows.length} puzzle rows audited.`);
console.log(`  hint coverage: ${hinted}/${hintableRows.length} live/staged-live rows have tier-2 + tier-3 rescue text (${hintExempt.size} exempt)`);
console.log(`  opaque plugin tokens: ${opaque} produced-kind row(s) protected`);
console.log(`  short typed caps: ${shortTypedCapped} short typed row(s) capped`);
console.log(`  input/producer routes: ${rows.filter((row) => humanTypedKinds.has(row.answerKind)).length} typed row(s) have three submission surfaces; ${opaque} produced row(s) have runtime consumers`);

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

function parseSeedActivatedKeys(files: string[]): Set<string> {
  const keys = new Set<string>();
  for (const file of files) {
    const text = readFileSync(resolve(seeds, file), 'utf8');
    const updateRe = /update\s+public\.puzzles\s+set\s+active\s*=\s*true\s+where\s+puzzle_key\s+in\s*\(([\s\S]*?)\)\s*;/gi;
    let update: RegExpExecArray | null;
    while ((update = updateRe.exec(text))) {
      for (const key of update[1]!.matchAll(/'([a-z0-9-]+)'/g)) keys.add(key[1]!);
    }
  }
  return keys;
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

function readCorpus(roots: string[]): string {
  const bodies: string[] = [];
  const visit = (path: string): void => {
    if (statSync(path).isDirectory()) {
      for (const child of readdirSync(path)) visit(resolve(path, child));
      return;
    }
    if (/\.(?:java|ts|tsx|yml|yaml)$/i.test(path)) bodies.push(readFileSync(path, 'utf8'));
  };
  for (const root of roots) visit(root);
  return bodies.join('\n');
}

function validateInputContracts(
  files: Record<string, string>,
  typedKinds: ReadonlySet<string>,
  puzzleRows: PuzzleRow[],
): string[] {
  if (!puzzleRows.some((row) => typedKinds.has(row.answerKind))) return [];
  const required: [keyof typeof files, string, string][] = [
    ['answerSign', 'SignChangeEvent', 'in-world filing signs do not consume sign edits'],
    ['answerSign', 'TYPE_KEEPER_STONE', 'keeper stones are not registered as answer-bearing sites'],
    ['answerSign', 'TYPE_CASE_BOARD', 'the absence case board is not an answer-bearing site'],
    ['answerSign', 'TYPE_PRIOR_CAMP', 'prior repair files are not answer-bearing sites'],
    ['answerSign', 'TYPE_FAILED_ACCEPTING', 'Failed Accepting is not an answer-bearing site'],
    ['answerSign', 'event.line(i, Component.empty())', 'submitted guesses are not cleared from filing signs'],
    ['structures', 'blank unwaxed submission slot', 'structure templates do not author editable answer signs'],
    ['command', 'no editable answer surface', 'world visual audit does not fail missing answer surfaces'],
    ['command', 'no editable answer sign found inside answer radius', 'world proof does not enforce a reachable input sign'],
    ['discordAnswer', 'resolveAnswer(', 'Discord /answer does not reach the shared resolver'],
    ['recordRoute', 'resolveInscription(', 'Record inscription does not reach the shared resolver'],
  ];
  return required
    .filter(([file, needle]) => !files[file]?.includes(needle))
    .map(([, , failure]) => failure);
}
