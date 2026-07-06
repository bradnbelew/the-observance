/**
 * sidequest-audit.ts - launch-confidence audit for optional quest breadth.
 *
 * Side quests are allowed to be optional; they are not allowed to spend a long
 * player walk on nothing. This audit keeps the false-lead budget small and
 * blocks old "anti-speedrun tax" language from creeping back into seed rewards.
 */
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const seeds = resolve(here, '../../supabase/seeds');
const supabase = resolve(here, '../../supabase');

const seedFiles = ['side_quests.sql', 'seventh_seed.sql', 'progression_seed.sql'];
const MAX_FALSE_LEADS = 2;
const VALUE_RE =
  /\b(card|fact\d*|item|producer|proof|warning|trust break|breadcrumb|verified|contradicted|taught|teaches|seeds?|custom|payoff|gates nothing|points? back|points? toward)\b|\bthe_[a-z0-9_]+\b/i;
const FILLER_LABEL_RE = /\b(side-track|side track|anti-speedrun tax|verified-but-hollow|spent on nothing|nothing pays|empty room)\b/i;
const STALE_DOC_RE = /\banti-speedrun tax|Five are deliberate dead leads|verified-but-hollow|SIDE-TRACK|dest-prophet-wall` and `dest-warm-town|dest-warm-town` and `dest-prophet-wall\b/i;

interface SideQuestRow {
  key: string;
  thread: string;
  entry: string | null;
  reward: string;
  tier: string;
  minutes: number;
  file: string;
}

const rows = seedFiles.flatMap((file) => parseSideQuestRows(file, readFileSync(resolve(seeds, file), 'utf8')));
const generatedRows = parseSideQuestRows('apply-all.sql', readFileSync(resolve(supabase, 'apply-all.sql'), 'utf8'));
const sitesText = readFileSync(resolve(repo, 'plugin/src/main/resources/sites.yml'), 'utf8');
const commandText = readFileSync(resolve(repo, 'plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java'), 'utf8');
const threadCardsText = readFileSync(resolve(seeds, 'thread_cards.sql'), 'utf8');
const travelText = readFileSync(resolve(repo, 'design/content/travel-destinations.md'), 'utf8');
const voiceText = readFileSync(resolve(repo, 'discord/src/voice.archive.ts'), 'utf8');

const falseLeads = rows.filter((row) => /\b(dead lead|false lead)\b/i.test(row.reward));
const weakFalseLeads = falseLeads.filter((row) => !/\b(with teeth|proof|warning|trust break|open no road|names? the lie|breadcrumb)\b/i.test(row.reward));
const staleEmptiness = rows.filter((row) =>
  /\b(spent on nothing|anti-speedrun tax|nothing pays|verified-but-hollow|open(?:s|ed)? nothing|empty room)\b/i.test(row.reward),
);
const nonPositiveMinutes = rows.filter((row) => !Number.isFinite(row.minutes) || row.minutes <= 0);
const weakPayoffs = rows.filter((row) => !VALUE_RE.test(row.reward));
const fillerLabels = rows.filter((row) => FILLER_LABEL_RE.test(row.reward));
const bundleDrift = diffRowSets(rows, generatedRows);
const physicalAnchorIssues = physicalAnchorChecks();
const staleFiles = findStaleFiles([
  {
    path: resolve(supabase, 'apply-tonight.sql'),
    label: 'discord/supabase/apply-tonight.sql',
    requiredText: 'DEPRECATED - DO NOT USE',
    stale: /\banti-speedrun tax|Five are deliberate dead leads|verified-but-hollow|SIDE-TRACK|DEAD LEAD:|FALSE LEAD:|opens nothing\b/i,
  },
  {
    path: resolve(repo, 'design/SIDEQUEST-PLAN.md'),
    label: 'design/SIDEQUEST-PLAN.md',
    stale: STALE_DOC_RE,
  },
]);

let failed = false;

if (falseLeads.length > MAX_FALSE_LEADS) {
  failed = true;
  console.error(`sidequestaudit: FAILED - ${falseLeads.length} blunt false/dead lead(s), max ${MAX_FALSE_LEADS}:`);
  for (const row of falseLeads) console.error(`  - ${rowLabel(row)}`);
}

if (weakFalseLeads.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - false/dead lead(s) without an explicit teeth/proof payoff:');
  for (const row of weakFalseLeads) console.error(`  - ${rowLabel(row)}`);
}

if (staleEmptiness.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - stale empty-payoff language remains:');
  for (const row of staleEmptiness) console.error(`  - ${rowLabel(row)}`);
}

if (nonPositiveMinutes.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - side quest(s) with invalid est_minutes:');
  for (const row of nonPositiveMinutes) console.error(`  - ${rowLabel(row)}`);
}

if (weakPayoffs.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - side quest(s) without an explicit value/payoff token:');
  for (const row of weakPayoffs) console.error(`  - ${rowLabel(row)}`);
}

if (fillerLabels.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - filler side-path labels remain in launch seed rewards:');
  for (const row of fillerLabels) console.error(`  - ${rowLabel(row)}`);
}

if (bundleDrift.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - generated apply-all.sql side-quest rows drift from source seeds:');
  for (const issue of bundleDrift) console.error(`  - ${issue}`);
}

if (physicalAnchorIssues.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - travel destination physical-anchor contract drift:');
  for (const issue of physicalAnchorIssues) console.error(`  - ${issue}`);
}

if (staleFiles.length > 0) {
  failed = true;
  console.error('sidequestaudit: FAILED - stale optional-path launch language remains outside seed rows:');
  for (const issue of staleFiles) console.error(`  - ${issue}`);
}

if (failed) process.exit(1);

const byTier = countBy(rows, (row) => row.tier);
console.log(`sidequestaudit: OK - ${rows.length} side-quest rows audited.`);
console.log(`  false/dead lead budget: ${falseLeads.length}/${MAX_FALSE_LEADS}`);
console.log(`  tiers: ${formatCounts(byTier)}`);
console.log('  bundle/docs/anchors: apply-all rows match seeds; key travel anchors exist; apply-tonight is a refusal stub; no stale filler labels found');

function parseSideQuestRows(file: string, seedSql: string): SideQuestRow[] {
  const sql = stripLineComments(seedSql);
  const insertRe = /insert\s+into\s+public\.side_quests[\s\S]*?values([\s\S]*?)on\s+conflict/gi;
  const rows: SideQuestRow[] = [];
  let insert: RegExpExecArray | null;
  while ((insert = insertRe.exec(sql))) {
    const values = insert[1]!;
    const rowRe = /\(\s*'([a-z0-9-]+)'\s*,\s*'([a-z]+)'\s*,\s*(null|'[a-z0-9-]+')\s*,\s*'((?:[^']|'')*)'\s*,\s*'([a-z]+)'\s*,\s*(\d+)\s*\)/g;
    let row: RegExpExecArray | null;
    while ((row = rowRe.exec(values))) {
      rows.push({
        key: row[1]!,
        thread: row[2]!,
        entry: row[3] === 'null' ? null : sqlString(row[3]!.slice(1, -1)),
        reward: sqlString(row[4]!),
        tier: row[5]!,
        minutes: Number(row[6]),
        file,
      });
    }
  }
  return rows;
}

function rowLabel(row: SideQuestRow): string {
  return `${row.file}:${row.key} (${row.tier}, ${row.minutes}m) - ${row.reward}`;
}

function rowSignature(row: SideQuestRow): string {
  return [row.key, row.thread, row.entry ?? 'null', row.reward, row.tier, row.minutes].join('\u001f');
}

function diffRowSets(sourceRows: SideQuestRow[], bundleRows: SideQuestRow[]): string[] {
  const issues: string[] = [];
  const source = new Set(sourceRows.map(rowSignature));
  const bundle = new Set(bundleRows.map(rowSignature));
  for (const row of sourceRows) {
    if (!bundle.has(rowSignature(row))) issues.push(`missing from apply-all.sql: ${rowLabel(row)}`);
  }
  for (const row of bundleRows) {
    if (!source.has(rowSignature(row))) issues.push(`extra/stale in apply-all.sql: ${rowLabel(row)}`);
  }
  return issues;
}

function findStaleFiles(
  files: { path: string; label: string; stale: RegExp; requiredText?: string }[],
): string[] {
  const issues: string[] = [];
  for (const file of files) {
    const text = readFileSync(file.path, 'utf8');
    if (file.requiredText && !text.includes(file.requiredText)) {
      issues.push(`${file.label} must clearly say "${file.requiredText}"`);
    }
    const stale = text.match(file.stale);
    if (stale) {
      issues.push(`${file.label} still contains stale phrase "${stale[0]}"`);
    }
  }
  return issues;
}

function physicalAnchorChecks(): string[] {
  const issues: string[] = [];
  const required = [
    {
      label: 'Far-water mirror destination',
      quest: 'dest-far-water',
      site: 'the_far_water',
      type: 'far_water',
      fixture: 'placeFarWater',
      card: "'who-sella-token'",
      voice: 'voice.dest.farWater.find',
      doc: 'D06',
    },
    {
      label: 'School-stand destination',
      quest: 'dest-school-stand',
      site: 'school_stand',
      type: 'school_stand',
      fixture: 'buildSchoolStand',
      card: "'human-school-stand'",
      voice: 'voice.dest.school.find',
      doc: 'D04',
    },
    {
      label: 'Markers-row count proof',
      quest: 'dest-markers-row',
      site: 'markers_row',
      type: 'markers_row',
      fixture: 'buildMarkersRow',
      card: "'happened-markers-row'",
      voice: 'voice.dest.markers.find',
      doc: 'D07',
    },
    {
      label: 'Cistern 7 light proof',
      quest: 'dest-cistern-7',
      site: 'cistern_7',
      type: 'cistern_7',
      fixture: 'buildCisternSeven',
      card: "'place-cistern-seven'",
      voice: 'voice.dest.cistern.find',
      doc: 'D08',
    },
    {
      label: 'Watch-floor dark-hours proof',
      quest: 'dest-watch-floor',
      site: 'watch_floor',
      type: 'watch_floor',
      fixture: 'buildWatchFloor',
      card: "'surface-watch-floor'",
      voice: 'voice.dest.watchFloor.find',
      doc: 'D12',
    },
    {
      label: 'Set-apart entry-5 proof',
      quest: 'dest-set-apart',
      site: 'set_apart_shelf',
      type: 'set_apart_shelf',
      fixture: 'buildSetApartShelf',
      card: "'surface-set-apart'",
      voice: 'voice.dest.setApart.find',
      doc: 'D11',
    },
    {
      label: 'Undercroft seal proof',
      quest: 'dest-undercroft-seal',
      site: 'undercroft_seal',
      type: 'undercroft_seal',
      fixture: 'buildUndercroftSeal',
      card: "'happened-undercroft-seal'",
      voice: 'voice.dest.undercroftSeal.find',
      doc: 'D15',
    },
    {
      label: 'Forgotten Mouth way-up proof',
      quest: 'dest-way-up',
      site: 'forgotten_mouth',
      type: 'forgotten_mouth',
      fixture: 'buildForgottenMouth',
      card: "'place-way-up'",
      voice: 'voice.dest.wayUp.find',
      doc: 'D17',
    },
    {
      label: 'Deep Market destination',
      quest: 'dest-deep-market',
      site: 'deep_market',
      type: 'deep_market',
      fixture: 'buildDeepMarket',
      card: "'who-deep-market'",
      voice: 'voice.dest.market.find',
      doc: 'D13',
    },
    {
      label: 'Ration-table destination',
      quest: 'dest-ration-table',
      site: 'ration_table',
      type: 'ration_table',
      fixture: 'buildRationTable',
      card: "'human-ration-redivided'",
      voice: 'voice.dest.rationTable.find',
      doc: 'D14',
    },
    {
      label: 'Third-bay Deep Line proof',
      quest: 'dest-third-bay',
      site: 'third_bay_breach',
      type: 'third_bay_breach',
      fixture: 'buildThirdBayBreach',
      card: "'place-deep-line'",
      voice: 'voice.dest.thirdBay.find',
      doc: 'D09',
    },
    {
      label: 'Warm-town contradiction',
      quest: 'dest-warm-town',
      site: 'warm_town_collapse',
      type: 'warm_town_collapse',
      fixture: 'buildWarmTownCollapse',
      card: null,
      voice: 'voice.dest.warmTown.find',
      doc: 'D03',
    },
    {
      label: 'Bird-coops destination',
      quest: 'dest-bird-coops',
      site: 'deep_bird_coops',
      type: 'bird_coops',
      fixture: 'buildBirdCoops',
      card: null,
      voice: 'voice.dest.coops.find',
      doc: 'D05',
    },
  ];
  for (const entry of required) {
    if (!rows.some((row) => row.key === entry.quest)) issues.push(`${entry.label}: missing side_quests row ${entry.quest}`);
    if (!siteHasType(entry.site, entry.type)) issues.push(`${entry.label}: sites.yml missing enabled ${entry.site} of type ${entry.type}`);
    if (!commandText.includes(entry.fixture)) issues.push(`${entry.label}: ObservanceCommand missing fixture ${entry.fixture}`);
    if (!commandText.includes(`"${entry.site}"`)) issues.push(`${entry.label}: ObservanceCommand missing route/coverage reference ${entry.site}`);
    if (entry.card && !threadCardsText.includes(entry.card)) issues.push(`${entry.label}: thread_cards missing card ${entry.card}`);
    if (entry.card && !threadCardsText.includes(`'${entry.site}'`)) issues.push(`${entry.label}: thread_cards must anchor ${entry.card} at ${entry.site}`);
    if (!travelText.includes(entry.voice) || !voiceText.includes(entry.voice)) {
      issues.push(`${entry.label}: voice key ${entry.voice} must exist in travel spec and voice archive`);
    }
    if (!travelText.includes(entry.doc)) issues.push(`${entry.label}: travel spec missing ${entry.doc}`);
  }
  return issues;
}

function siteHasType(id: string, type: string): boolean {
  const pattern = new RegExp(`^\\s{2}${escapeRegExp(id)}:\\s*[\\s\\S]*?^\\s*type:\\s*"${escapeRegExp(type)}"[\\s\\S]*?^\\s*enabled:\\s*true`, 'm');
  return pattern.test(sitesText);
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
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
