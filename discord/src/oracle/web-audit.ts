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
const publicListingPage = resolve(repo, 'dashboard/src/app/page.tsx');
const serverListPage = resolve(repo, 'dashboard/src/app/server-list.php/page.tsx');
const serverDetailsPage = resolve(repo, 'dashboard/src/app/server.php/page.tsx');
const communityPostPage = resolve(repo, 'dashboard/src/app/community/2011/02/08/world-backup/page.tsx');
const legacyShell = resolve(repo, 'dashboard/src/components/legacy/LegacyShell.tsx');
const legacyContent = resolve(repo, 'dashboard/src/lib/legacy-content.ts');
const recordSlugPage = resolve(repo, 'dashboard/src/app/record/[slug]/page.tsx');
const voiceArchive = resolve(repo, 'discord/src/voice.archive.ts');
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
const hintSeed = readFileSync(resolve(seeds, 'hints_seed.sql'), 'utf8');
const publicListingSource = readFileSync(publicListingPage, 'utf8');
const serverListSource = readFileSync(serverListPage, 'utf8');
const serverDetailsSource = readFileSync(serverDetailsPage, 'utf8');
const communityPostSource = readFileSync(communityPostPage, 'utf8');
const legacyShellSource = readFileSync(legacyShell, 'utf8');
const legacyContentSource = readFileSync(legacyContent, 'utf8');
const recordSlugSource = readFileSync(recordSlugPage, 'utf8');
const voiceArchiveSource = readFileSync(voiceArchive, 'utf8');

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
const webArtifactIssues = [
  ...auditCopperlineArtifacts(publicListingSource, serverListSource, serverDetailsSource, communityPostSource, legacyShellSource, legacyContentSource, recordSlugSource),
  ...auditCopperlineVoice(voiceArchiveSource),
  ...auditCopperlineDifficulty(rows),
  ...auditCopperlineHints(hintSeed),
];

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
console.log(`  web artifacts: public listing + Record lure download ${existsSync(holdZip) ? 'available' : 'safely withheld until dashboard/public/the-hold/the-hold.zip exists'}`);
if (sequenced.length > 0) {
  console.log(`  sequenced duplicate answers: ${sequenced.length} (gated, review on story edits)`);
  for (const issue of sequenced) console.log(`    - ${issue}`);
}

function auditCopperlineArtifacts(
  publicPageSource: string,
  serverListSource: string,
  serverDetailsSource: string,
  communityPostSource: string,
  shellSource: string,
  contentSource: string,
  recordPageSource: string,
): string[] {
  const issues: string[] = [];
  if (!shellSource.includes('Copperline Hosting') || !shellSource.includes('Copyright © 2009–2014')) {
    issues.push('Public shell must identify the fictional Copperline Hosting company and retain its period footer');
  }
  if (!publicPageSource.includes('Minecraft server hosting') || !publicPageSource.includes('TCAdmin')) {
    issues.push('Public root must read as an ordinary period game host, with mundane product and panel language');
  }
  for (const forbidden of ['/record/', 'The Observance', 'mkept', 'keeper-eye']) {
    if (publicPageSource.includes(forbidden)) issues.push(`Public root must not expose ARG-specific material: ${forbidden}`);
  }
  if (!serverListSource.includes('publicServers') || !serverListSource.includes('/server.php?id=')) {
    issues.push('Copperline must expose a normal public server directory with ordinary customer listings');
  }
  if (!serverDetailsSource.includes('server.id === "1842"') || !contentSource.includes('name: "The Observance"')) {
    issues.push('The Observance must exist only as expired Copperline service 1842');
  }
  if (!serverDetailsSource.includes('/community/2011/02/08/world-backup/') || !serverDetailsSource.includes('/support/ticket.php?id=1851')) {
    issues.push('Service 1842 must lead naturally to its owner post and archived support request');
  }
  if (!serverDetailsSource.includes('removed from directory export')) {
    issues.push('Service 1842 must withhold the live Minecraft endpoint and send players through the recovered-file trail');
  }
  if (serverDetailsSource.includes('NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS') || serverDetailsSource.includes('liveAddress')) {
    issues.push('Copperline must never own the live Minecraft endpoint; the recovered map owns that reveal');
  }
  if (serverDetailsSource.includes('href="/record/')) {
    issues.push('The customer listing must never link directly to a Record surface');
  }
  if (!communityPostSource.includes('/the-hold/the-hold.zip') || !communityPostSource.includes('/record/the-record-keeps')) {
    issues.push('The ordinary owner post must be the sole public bridge from the world download to the Record lure');
  }
  if (!recordPageSource.includes('const HOLD_ZIP_PUBLIC_PATH = "/the-hold/the-hold.zip"')) {
    issues.push('Record lure page must retain the canonical recovered-file path');
  }
  if (!existsSync(holdZip)) issues.push('Production Hold download is missing from dashboard/public/the-hold/the-hold.zip');
  return issues;
}

function auditCopperlineDifficulty(seedRows: PuzzleSeedRow[]): string[] {
  const row = seedRows.find((r) => r.key === 'record-url');
  if (!row) return ['record-url seed row is missing'];
  const issues: string[] = [];
  for (const answer of row.answers) {
    if (!answer.includes('copperline') || !answer.includes('1842')) {
      issues.push(`record-url answer must identify both the provider and service number: "${answer}"`);
    }
    if (answer.includes('snoikerz') || answer.includes('mirror 03')) {
      issues.push(`record-url answer retains the retired host fiction: "${answer}"`);
    }
  }
  return issues;
}

function auditCopperlineHints(hintsSql: string): string[] {
  const bodies = [...hintsSql.matchAll(/\('record-url',\s*(\d+),\s*'((?:[^']|'')*)'\)/g)]
    .map((match) => (match[2] ?? '').replace(/''/g, "'").toLowerCase());
  if (bodies.length < 2) return ['record-url must retain two earned hint tiers'];
  const all = bodies.join('\n');
  if (!all.includes('provider') || !all.includes('directory number')) {
    return ['record-url hints must teach the provider + directory-number reconstruction'];
  }
  if (!all.includes('copperline') || !all.includes('1842') || !all.includes('public server directory')) {
    return ['record-url final hint must identify Copperline service 1842 and its directory trail'];
  }
  return [];
}

function auditCopperlineVoice(archiveSource: string): string[] {
  const issues: string[] = [];
  if (!archiveSource.includes('cardSurfaceRecordElsewhere') || !archiveSource.includes('copperline hosting, common web, service 1842')) {
    issues.push('Archive web-door card must preserve the Copperline service 1842 reconstruction');
  }
  if (!archiveSource.includes("'aro.rumor.host'") || !archiveSource.includes('public directory never got cleaned out')) {
    issues.push('Aro must echo the abandoned Copperline directory in ordinary human speech');
  }
  if (archiveSource.includes('Old SNOIKERZ row')) issues.push('Archive voice retains the retired Snoikerz fiction');
  return issues;
}

export function auditWebArtifacts(publicPageSource: string, recordPageSource: string): string[] {
  const issues: string[] = [];
  if (!publicPageSource.includes('archived server listing')) {
    issues.push('Public root must render as an abandoned server/map listing, not a generic dashboard');
  }
  if (!publicPageSource.includes('mirror 03') || !publicPageSource.includes('ping: no reply')) {
    issues.push('Public root must carry stale mirror/ping residue so it reads as a found server listing');
  }
  if (!publicPageSource.includes('players') || !publicPageSource.includes('whitelist residue')) {
    issues.push('Public root must include mundane Minecraft server-listing fields, not only ARG navigation');
  }
  if (!publicPageSource.includes('server.properties') || !publicPageSource.includes('enforce-whitelist')) {
    issues.push('Public root must include mundane host config residue so it reads as an old server listing');
  }
  if (!publicPageSource.includes('host account') || !publicPageSource.includes('legacy free')) {
    issues.push('Public root must include old hosting-account residue so SNOIKERZ reads like a real abandoned host');
  }
  if (!publicPageSource.includes('control panel residue') || !publicPageSource.includes('console", "disabled on free plan')
      || !publicPageSource.includes('restart", "queued, never acknowledged')
      || !publicPageSource.includes('operator tab", "no verified staff')) {
    issues.push('Public root must include stale control-panel residue so the listed address feels hosted, not magically revealed');
  }
  if (!publicPageSource.includes('billing ledger') || !publicPageSource.includes('staff removal request')
      || !publicPageSource.includes('invoice waived') || !publicPageSource.includes('blank whitelist row')) {
    issues.push('Public root must include billing/support-account residue tying staff removal, free mirror, and blank whitelist row together');
  }
  if (!publicPageSource.includes('abuse queue') || !publicPageSource.includes('hidden address in the map')
      || !publicPageSource.includes('closed: not in file') || !publicPageSource.includes('staff.txt restore')) {
    issues.push('Public root must include abuse-queue residue proving the map itself does not contain the live endpoint');
  }
  if (!publicPageSource.includes('uptime checks') || !publicPageSource.includes('timeout')) {
    issues.push('Public root must include failed uptime checks so the server row feels operational but dormant');
  }
  if (!publicPageSource.includes('download comments') || !publicPageSource.includes('ending says the rest is kept here')) {
    issues.push('Public root must include old download-comment residue that bridges the offline map to the host row');
  }
  if (!publicPageSource.includes('observance-pack.zip') || !publicPageSource.includes('served by server')) {
    issues.push('Public root must mention the join resource pack as a server-served file, not a raw puzzle download');
  }
  if (!publicPageSource.includes('support ticket cache') || !publicPageSource.includes('third lamp marked ready without a lamp')) {
    issues.push('Public root must include believable support-ticket residue that quietly previews in-world mechanics');
  }
  if (!publicPageSource.includes('join packet') || !publicPageSource.includes('copy the address from this row only')
      || !publicPageSource.includes('map files are not endpoints')) {
    issues.push('Public root must give a mundane join packet that makes the listing the server-address authority, not the map');
  }
  if (!publicPageSource.includes('whitelist queue') || !publicPageSource.includes('row exists')) {
    issues.push('Public root must carry the seven-row whitelist ghost without naming the withheld row');
  }
  if (!publicPageSource.includes('packet trace') || !publicPageSource.includes('do not reconstruct a port')
      || !publicPageSource.includes('mirror_03/the-hold.zip')) {
    issues.push('Public root must include packet/DNS trace residue that steers players away from raw ports and toward the map/listing split');
  }
  if (!publicPageSource.includes('checksum ledger') || !publicPageSource.includes('level.dat')
      || !publicPageSource.includes('sign repairs only') || !publicPageSource.includes('manifest.txt')) {
    issues.push('Public root must include checksum/file-ledger residue so the recovered map reads like a real repaired world copy');
  }
  if (!publicPageSource.includes('failed join log') || !publicPageSource.includes('kicked: not whitelisted')
      || !publicPageSource.includes('kicked: missing pack') || !publicPageSource.includes('server row still resolves')) {
    issues.push('Public root must include failed join residue that explains whitelist/pack behavior without leaking operator setup');
  }
  if (!publicPageSource.includes('maintenance notes') || !publicPageSource.includes('lecterns with no book are host errors')
      || !publicPageSource.includes('do not rewrite low signs upward') || !publicPageSource.includes('ready mark is not the same as lit')) {
    issues.push('Public root must include maintenance-note residue that reinforces in-world reading rules without turning them into UI instructions');
  }
  if (!publicPageSource.includes('moderation cache') || !publicPageSource.includes('the map is a copy. the listing is the door.')) {
    issues.push('Public root must include old moderation residue that states the map/listing relationship in-fiction');
  }
  if (!publicPageSource.includes('mirror log') || !publicPageSource.includes('seventh row erased')) {
    issues.push('Public root must include old host/archive chronology that connects the map to the Record');
  }
  if (!publicPageSource.includes('dns cache') || !publicPageSource.includes('_minecraft') || !publicPageSource.includes('founder residue')) {
    issues.push('Public root must include DNS/cache residue so the SNOIKERZ clue reads like a stale host record, not a magic URL');
  }
  if (!publicPageSource.includes('_minecraft", "removed"')) {
    issues.push('Public root DNS cache must explicitly withhold the Minecraft SRV record until the live address is intentionally listed');
  }
  if (publicPageSource.includes('NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS')) {
    issues.push('Public root must not own the live server address reveal; the recovered map owns it');
  }
  if (!publicPageSource.includes('address withheld until the host is awake')) {
    issues.push('Public root must render an in-fiction withheld server-address state before launch');
  }
  if (!publicPageSource.includes('download the-hold.zip')) {
    issues.push('Public root must expose the opening Hold download as the listing world file');
  }
  if (!publicPageSource.includes('href="/record/the-record-keeps"')) {
    issues.push('Public root must keep a visible path back to the Record lure page');
  }
  if (!publicPageSource.includes('/keeper-eye.svg')) {
    issues.push('Public root must use a real Observance visual asset, not a bare text menu');
  }
  if (!publicPageSource.includes('accept the pack or the carved alphabet will look wrong')) {
    issues.push('Public root must warn in fiction that the resource pack matters for rune readability');
  }
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

export function auditRecordUrlDifficulty(seedRows: PuzzleSeedRow[]): string[] {
  const row = seedRows.find((r) => r.key === 'record-url');
  if (!row) return ['record-url seed row is missing; opening web-door puzzle cannot be audited'];
  const issues: string[] = [];
  const forbidden = new Set([
    'the record keeps',
    'the record is kept in more than one place',
    'the record is kept in more than one place against the loss of the first',
  ]);
  for (const answer of row.answers) {
    if (forbidden.has(answer)) {
      issues.push(`record-url accepted answer is too generic and bypasses the SNOIKERZ mirror reconstruction: "${answer}"`);
    }
    const requiresHost = answer.includes('snoikerz') || answer.includes('old listing') || answer.includes('mirror 03');
    if (!requiresHost) {
      issues.push(`record-url accepted answer must name the host/listing/mirror context: "${answer}"`);
    }
  }
  if (!row.answers.some((answer) => answer.includes('snoikerz mirror 03'))) {
    issues.push('record-url must accept at least one answer that explicitly reconstructs SNOIKERZ mirror 03');
  }
  return issues;
}

export function auditRecordUrlHints(hintsSql: string): string[] {
  const issues: string[] = [];
  const rowHints = [...hintsSql.matchAll(/\('record-url',\s*(\d+),\s*'((?:[^']|'')*)'\)/g)]
    .map((m) => ({ tier: Number(m[1]), body: (m[2] ?? '').replace(/''/g, "'").toLowerCase() }));
  if (rowHints.length < 2) {
    issues.push('record-url must have at least two hint tiers for the off-world host-row solve');
    return issues;
  }
  const all = rowHints.map((h) => h.body).join('\n');
  if (!all.includes('host-row pieces') || !all.includes('front door') || !all.includes('common web')
      || !all.includes('root path') || !all.includes('mirror 03')) {
    issues.push('record-url hints must teach reconstruction of the SNOIKERZ host row without skipping the puzzle');
  }
  if (!all.includes('raw server port') || !all.includes('removed minecraft service row')
      || !all.includes('srv record')) {
    issues.push('record-url hints must steer players away from raw server endpoints and the removed Minecraft service/SRV row');
  }
  if (all.includes('next_public_observance_server_address') || all.includes('address withheld until the host is awake')) {
    issues.push('record-url hints must not leak implementation/configured server-address wording');
  }
  return issues;
}

export function auditRecordElsewhereVoice(archiveSource: string): string[] {
  const issues: string[] = [];
  if (!archiveSource.includes('cardSurfaceRecordElsewhere')) {
    issues.push('Archive voice must keep the surface-record-elsewhere card for the web-door payoff');
  }
  if (!archiveSource.includes('front door, common web, root path, mirror 03')) {
    issues.push('Archive web-door card must preserve the exact SNOIKERZ host-row reconstruction');
  }
  if (!archiveSource.includes('uptime checks time out') || !archiveSource.includes('download comments')) {
    issues.push('Archive web-door card must mention the same uptime/download-comment residue shown on the public host page');
  }
  if (!archiveSource.includes('join packet') || !archiveSource.includes('the map is a copy and the listing is the door')) {
    issues.push('Archive web-door card must preserve the map/listing distinction shown on the public host page');
  }
  if (!archiveSource.includes('checksum ledger says level.dat was last played by m.kept')
      || !archiveSource.includes('region diffs are sign repairs only')) {
    issues.push('Archive web-door card must echo checksum/file-ledger residue from the public host page');
  }
  if (!archiveSource.includes('failed joins kick a guest for whitelist and missing pack')
      || !archiveSource.includes('row still resolves')) {
    issues.push('Archive web-door card must echo failed join residue without revealing a live endpoint');
  }
  if (!archiveSource.includes('control panel is expired, console disabled, restart queued')
      || !archiveSource.includes('billing ledger waives the mirror')
      || !archiveSource.includes('abuse queue says the hidden address was not in the map file')) {
    issues.push('Archive web-door card must echo host-panel/billing/abuse residue from the public host page');
  }
  if (!archiveSource.includes('maintenance notes say low signs stay low and empty lecterns are host errors')) {
    issues.push('Archive web-door card must echo maintenance-note reading rules for signs and lecterns');
  }
  if (!archiveSource.includes('ready without a lamp')) {
    issues.push('Archive web-door card must retain the lampworks support-ticket callback');
  }
  if (!archiveSource.includes("'aro.rumor.host'")
      || !archiveSource.includes('Old SNOIKERZ row still gets checked')
      || !archiveSource.includes('A map is where a server leaves a forwarding address')) {
    issues.push('Aro must carry a human-scale SNOIKERZ host/listing rumor so the web-door reveal is echoed in town');
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
