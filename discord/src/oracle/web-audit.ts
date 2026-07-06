/**
 * web-audit.ts — player-facing oracle web audit.
 *
 * seedcheck proves answer strings are normalized; specscheck proves forge/spec coverage.
 * This file checks the experience risks those do not cover:
 *   - two simultaneously-open rows sharing one accepted answer;
 *   - too many ungated rows at cold start;
 *   - duplicate answers that are safe only because a requires_flags gate sequences them.
 */
import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const seeds = resolve(here, '../../supabase/seeds');
const recordSlugPage = resolve(repo, 'dashboard/src/app/record/[slug]/page.tsx');
const holdZip = resolve(repo, 'dashboard/public/the-hold/the-hold.zip');

interface PuzzleSeedRow {
  key: string;
  title: string;
  answers: string[];
  outcome: string;
  movement: number;
  active: boolean;
}

const puzzleSeed = readFileSync(resolve(seeds, 'puzzles_seed.sql'), 'utf8');
const progressionSeed = readFileSync(resolve(seeds, 'progression_seed.sql'), 'utf8');
const metapuzzleSeed = readFileSync(resolve(seeds, 'metapuzzle_seed.sql'), 'utf8');
const recordSlugSource = readFileSync(recordSlugPage, 'utf8');

const rows = [
  ...parsePuzzleRows(puzzleSeed),
  ...parsePuzzleRows(progressionSeed),
];
const activeUpdates = new Set([
  ...parseActiveUpdates(metapuzzleSeed),
  ...parseActiveUpdates(progressionSeed),
]);
const requiresFlags = new Map([
  ...parseRequiresFlagUpdates(metapuzzleSeed),
  ...parseRequiresFlagUpdates(progressionSeed),
]);

for (const row of rows) {
  if (activeUpdates.has(row.key)) row.active = true;
}

const byAnswer = new Map<string, PuzzleSeedRow[]>();
for (const row of rows) {
  for (const answer of row.answers) {
    const existing = byAnswer.get(answer) ?? [];
    existing.push(row);
    byAnswer.set(answer, existing);
  }
}

const simultaneous: string[] = [];
const sequenced: string[] = [];
for (const [answer, answerRows] of byAnswer) {
  if (answerRows.length < 2) continue;
  const activeRows = answerRows.filter((r) => r.active);
  const byGate = new Map<string, PuzzleSeedRow[]>();
  for (const row of activeRows) {
    const gate = gateLabel(row.key, requiresFlags);
    const gatedRows = byGate.get(gate) ?? [];
    gatedRows.push(row);
    byGate.set(gate, gatedRows);
  }
  for (const [gate, gatedRows] of byGate) {
    if (gatedRows.length > 1) {
      simultaneous.push(`${answer} :: ${gate} :: ${gatedRows.map(rowLabel).join(' | ')}`);
    }
  }
  if (activeRows.length > 1 && byGate.size > 1) {
    sequenced.push(`${answer} :: ${activeRows.map((r) => `${rowLabel(r)} gate=${gateLabel(r.key, requiresFlags)}`).join(' | ')}`);
  }
}

const coldOpen = rows
  .filter((r) => r.active && !requiresFlags.has(r.key))
  .sort((a, b) => a.movement - b.movement || a.key.localeCompare(b.key));
const coldByMovement = countBy(coldOpen, (r) => `M${r.movement}`);
const lateColdOpen = coldOpen.filter((r) => r.movement >= 3);
const m2ColdOpen = coldOpen.filter((r) => r.movement === 2);
const MAX_M2_COLD_OPEN = 12;
const webArtifactIssues = auditWebArtifacts(recordSlugSource);

if (webArtifactIssues.length > 0) {
  console.error(`webaudit: FAILED - ${webArtifactIssues.length} web artifact readiness issue(s):`);
  for (const issue of webArtifactIssues) console.error(`  - ${issue}`);
  process.exit(1);
}

if (simultaneous.length > 0) {
  console.error(`webaudit: FAILED — ${simultaneous.length} simultaneously-open accepted-answer collision(s):`);
  for (const issue of simultaneous) console.error(`  - ${issue}`);
  process.exit(1);
}

if (lateColdOpen.length > 0) {
  console.error(`webaudit: FAILED — ${lateColdOpen.length} M3+ row(s) are cold-open without requires_flags:`);
  for (const row of lateColdOpen) console.error(`  - ${rowLabel(row)}`);
  process.exit(1);
}

if (m2ColdOpen.length > MAX_M2_COLD_OPEN) {
  console.error(`webaudit: FAILED — M2 cold-open breadth is ${m2ColdOpen.length}, max ${MAX_M2_COLD_OPEN}:`);
  for (const row of m2ColdOpen) console.error(`  - ${rowLabel(row)}`);
  process.exit(1);
}

console.log(`webaudit: OK — ${rows.length} puzzle rows audited; no simultaneous accepted-answer collisions.`);
console.log(`  cold-open rows: ${coldOpen.length} (${formatCounts(coldByMovement)})`);
console.log(`  web artifacts: Record lure download ${existsSync(holdZip) ? 'available' : 'safely withheld until dashboard/public/the-hold/the-hold.zip exists'}`);
if (sequenced.length > 0) {
  console.log(`  sequenced duplicate answers: ${sequenced.length} (gated, review on story edits)`);
  for (const issue of sequenced) console.log(`    - ${issue}`);
}

function auditWebArtifacts(recordPageSource: string): string[] {
  const issues: string[] = [];
  if (!recordPageSource.includes('const HOLD_ZIP_PUBLIC_PATH = "/the-hold/the-hold.zip"')) {
    issues.push('Record lure page must centralize the-hold.zip path as HOLD_ZIP_PUBLIC_PATH');
  }
  if (!recordPageSource.includes('function holdZipAvailable()')) {
    issues.push('Record lure page must check whether the-hold.zip exists before linking it');
  }
  if (!recordPageSource.includes('file not yet recovered')) {
    issues.push('Record lure page must render an in-fiction withheld state when the-hold.zip is absent');
  }
  if (!existsSync(holdZip) && !recordPageSource.includes('hasHoldZip ?')) {
    issues.push('the-hold.zip is absent, but the Record lure page does not conditionally withhold the download link');
  }
  return issues;
}

function parsePuzzleRows(seedSql: string): PuzzleSeedRow[] {
  const sql = stripLineComments(seedSql);
  const starts: { key: string; idx: number }[] = [];
  const keyRe = /\(\s*'([a-z0-9-]+)'\s*,\s*'((?:[^']|'')*)'\s*,\s*array\s*\[/g;
  let match: RegExpExecArray | null;
  while ((match = keyRe.exec(sql))) {
    starts.push({ key: match[1]!, idx: match.index });
  }

  const parsed: PuzzleSeedRow[] = [];
  for (let i = 0; i < starts.length; i++) {
    const start = starts[i]!;
    const end = i + 1 < starts.length ? starts[i + 1]!.idx : sql.length;
    const body = sql.slice(start.idx, end);
    const head = body.match(/\(\s*'([a-z0-9-]+)'\s*,\s*'((?:[^']|'')*)'\s*,\s*array\s*\[([\s\S]*?)\]\s*,\s*'([a-z_]+)'/);
    if (!head) throw new Error(`webaudit: could not parse row head for ${start.key}`);
    const tails = [...body.matchAll(/,\s*(\d+)\s*,\s*(true|false)\s*,\s*(?:null|\d+)\s*\)/g)];
    if (tails.length === 0) throw new Error(`webaudit: could not parse row tail for ${start.key}`);
    const tail = tails[tails.length - 1]!;
    parsed.push({
      key: head[1]!,
      title: sqlString(head[2]!),
      answers: [...head[3]!.matchAll(/'((?:[^']|'')*)'/g)].map((m) => sqlString(m[1]!)),
      outcome: head[4]!,
      movement: Number(tail[1]),
      active: tail[2] === 'true',
    });
  }
  return parsed;
}

function parseActiveUpdates(sqlText: string): string[] {
  const sql = stripLineComments(sqlText);
  const keys = new Set<string>();
  const updateRe = /update\s+public\.puzzles\s+set\s+active\s*=\s*true\s+where\s+puzzle_key\s+(in\s*\(([^)]*)\)|=\s*'([a-z0-9-]+)')/i;
  for (const statement of sql.split(';')) {
    const m = statement.match(updateRe);
    if (!m) continue;
    const list = m[2] ?? m[3];
    if (!list) continue;
    for (const key of extractKeys(list)) keys.add(key);
  }
  return [...keys];
}

function parseRequiresFlagUpdates(sqlText: string): [string, string][] {
  const sql = stripLineComments(sqlText);
  const out: [string, string][] = [];
  const updateRe = /update\s+public\.puzzles\s+set\s+requires_flags\s*=\s*jsonb_build_object\(([\s\S]*?)\)\s*where\s+puzzle_key\s+(in\s*\(([^)]*)\)|=\s*'([a-z0-9-]+)')/i;
  for (const statement of sql.split(';')) {
    const m = statement.match(updateRe);
    if (!m) continue;
    const flags = [...m[1]!.matchAll(/'([a-z0-9_]+)'\s*,\s*true/g)].map((fm) => fm[1]!).sort().join('+') || 'unknown';
    const list = m[3] ?? m[4];
    if (!list) continue;
    for (const key of extractKeys(list)) out.push([key, flags]);
  }
  return out;
}

function extractKeys(sqlFragment: string): string[] {
  const quoted = [...sqlFragment.matchAll(/'([a-z0-9-]+)'/g)].map((m) => m[1]!);
  if (quoted.length > 0) return quoted;
  const single = sqlFragment.trim();
  return /^[a-z0-9-]+$/.test(single) ? [single] : [];
}

function gateLabel(key: string, gates: Map<string, string>): string {
  return gates.get(key) ?? 'ungated';
}

function rowLabel(row: PuzzleSeedRow): string {
  return `${row.key} (${row.outcome}, M${row.movement}, "${row.title}")`;
}

function stripLineComments(sql: string): string {
  return sql.replace(/--[^\n]*/g, '');
}

function sqlString(value: string): string {
  return value.replace(/''/g, "'");
}

function countBy<T>(values: T[], keyFn: (value: T) => string): Map<string, number> {
  const counts = new Map<string, number>();
  for (const value of values) counts.set(keyFn(value), (counts.get(keyFn(value)) ?? 0) + 1);
  return counts;
}

function formatCounts(counts: Map<string, number>): string {
  return [...counts.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => `${k}:${v}`).join(', ');
}
