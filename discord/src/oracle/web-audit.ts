import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { REQUIRED_MEDIA } from '../v5/media.js';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const read = (path: string) => readFileSync(resolve(repo, path), 'utf8');
const issues: string[] = [];

function sourceFiles(path: string): string[] {
  const absolute = resolve(repo, path);
  const output: string[] = [];
  for (const entry of readdirSync(absolute, { withFileTypes: true })) {
    const relative = `${path}/${entry.name}`;
    if (entry.isDirectory()) output.push(...sourceFiles(relative));
    else if (/\.(?:ts|tsx)$/.test(entry.name)) output.push(relative);
  }
  return output;
}

const requiredFiles = [
  'dashboard/src/app/record/[slug]/page.tsx',
  'dashboard/src/app/record/archive/page.tsx',
  'dashboard/src/app/record/terminal/page.tsx',
  'dashboard/src/app/support/ticket.php/page.tsx',
  'dashboard/src/app/community/2011/02/08/world-backup/page.tsx',
  'dashboard/src/app/community/remote-room.php/page.tsx',
  'dashboard/src/app/community/archive.php/page.tsx',
  'dashboard/src/app/the-hold/the-hold.zip/route.ts',
  'dashboard/src/lib/v5-hold-archive.ts',
  'dashboard/content/the-hold-v5/the-hold.zip',
  'dashboard/content/the-hold-v5/the-hold.sha1',
  'dashboard/content/the-hold-v5/archive-manifest.tsv',
  'dashboard/content/the-hold-v5/README-FIRST.txt',
  'dashboard/public/evidence/copperline/camp-frame-06.png',
  'dashboard/public/evidence/copperline/locker-detail-13.png',
  'tools/build_hold_prologue.py',
];
for (const path of requiredFiles) if (!existsSync(resolve(repo, path))) issues.push(`missing V5 web artifact: ${path}`);
const publicHoldDirectory = resolve(repo, 'dashboard/public/the-hold');
if (existsSync(publicHoldDirectory)) issues.push('V5 archive bypass directory remains directly public');
for (const path of [
  'dashboard/content/the-hold-v5/relay-route.svg',
  'dashboard/content/the-hold-v5/service-1842.txt',
  'dashboard/content/the-hold-v5/work-orders/A-copper.txt',
  'dashboard/content/the-hold-v5/work-orders/B-line.txt',
  'dashboard/content/the-hold-v5/work-orders/C-hosting.txt',
  'dashboard/content/the-hold-v5/work-orders/D-dot-com.txt',
]) {
  if (existsSync(resolve(repo, path))) issues.push(`retired diagnostic artifact remains: ${path}`);
}

const record = read('dashboard/src/app/record/[slug]/page.tsx');
const archive = read('dashboard/src/app/record/archive/page.tsx');
const terminal = read('dashboard/src/app/record/terminal/page.tsx');
const support = read('dashboard/src/app/support/ticket.php/page.tsx');
const community = read('dashboard/src/app/community/2011/02/08/world-backup/page.tsx');
const remoteRoom = read('dashboard/src/app/community/remote-room.php/page.tsx');
const priorArchive = read('dashboard/src/app/community/archive.php/page.tsx');
const holdDownloadRoute = read('dashboard/src/app/the-hold/the-hold.zip/route.ts');
const holdArchiveHelper = read('dashboard/src/lib/v5-hold-archive.ts');
const nextConfig = read('dashboard/next.config.mjs');
const caseSeed = read('discord/supabase/seeds/v5_investigations.sql');
const dashboardEnv = read('dashboard/.env.example');
const dashboardReadme = read('dashboard/README.md');
const holdBuilder = read('tools/build_hold_prologue.py');
const dashboardPackage = JSON.parse(read('dashboard/package.json')) as {
  dependencies?: Record<string, string>;
  devDependencies?: Record<string, string>;
};

for (const required of ['nodes_completed', 'total_nodes', 'cases_completed', 'current_case_title', 'V5_HOLD_ARCHIVE_DOWNLOAD_PATH']) {
  if (!record.includes(required)) issues.push(`Record projection missing V5 field/path: ${required}`);
}
for (const required of ['v_required_media_delivery', 'only earned evidence is indexed']) {
  if (!archive.includes(required)) issues.push(`Recovery archive missing V5 gate/copy: ${required}`);
}
for (const required of ['82', '10 cases', 'this docket is read-only', 'a typed minecraft name is not an identity credential', 'no closeness response']) {
  if (!terminal.includes(required)) issues.push(`Terminal missing V5 contract: ${required}`);
}
for (const required of ['Ticket #9137', '/community/2011/02/08/world-backup/', 'single-player recovery image']) {
  if (!support.includes(required)) issues.push(`Copperline ticket trail missing contract: ${required}`);
}
for (const required of ['Java Edition 1.21.11 world', 'V5_HOLD_ARCHIVE_DOWNLOAD_PATH', "readV5CompletionFlag('v5_ls03_directory_trail')", 'Download world']) {
  if (!community.includes(required)) issues.push(`Copperline playable-world post missing contract: ${required}`);
}
for (const retired of ['diagnostic export', 'not contain a playable world', 'archive-resolver', 'name="host"', 'name="callback"', 'name="route"', '/community/remote-room.php']) {
  if (community.includes(retired)) issues.push(`Copperline playable-world post retains retired resolver copy: ${retired}`);
}
if (!remoteRoom.includes('return <MissingArchiveEntry />')) issues.push('Copperline remote room is not a permanent fail-closed tombstone');
for (const forbidden of ['DISCORD_INVITE_URL', 'safeDiscordInvite', 'discord.gg', 'discord.com/invite', 'readV5CompletionFlag', 'recordV5WebSequence', '/obslink']) {
  if (remoteRoom.includes(forbidden)) issues.push(`Copperline remote room can still expose or advance the retired handoff: ${forbidden}`);
}
for (const required of [
  "params.service === '1842'",
  "params.ticket === '9137'",
  "params.locker === '13'",
  'https://youtu.be/du-qp_clP7c',
  "['A06']",
  "'copperline_archive_route'",
  "['A07']",
  "'clip_01_ash_locker'",
  "image_ids: ['camp_frame_06', 'locker_detail_13']",
  '/evidence/copperline/camp-frame-06.png',
  '/evidence/copperline/locker-detail-13.png',
]) {
  if (!priorArchive.includes(required)) issues.push(`A06 exact archive route missing contract: ${required}`);
}

const publicCopy = `${record}\n${archive}\n${terminal}\n${support}\n${community}\n${remoteRoom}\n${priorArchive}`.toLowerCase();
for (const stale of [
  'seventh:',
  'six plus one',
  'type <code>kept</code>',
  'optional side',
  'diagnostic export',
  'field relay destination check',
  'callbackfromarchive',
  'remote room recovered',
]) {
  if (publicCopy.includes(stale)) issues.push(`public V5 copy retains retired phrase: ${stale}`);
}

for (const required of [
  "dynamic = 'force-dynamic'",
  'revalidate = 0',
  "readV5CompletionFlag('v5_ls03_directory_trail')",
  'readValidatedV5HoldArchive',
  'recordV5WebSequence',
  "['LS04']",
  "'copperline_world_backup'",
  "handler: 'world_download'",
  'if (!handoff.complete) return genericNotFound()',
  'genericNotFound()',
  'status: 404',
  "'Cache-Control': 'private, no-store, max-age=0, must-revalidate'",
  "'Content-Type': 'application/zip'",
]) {
  if (!holdDownloadRoute.includes(required)) issues.push(`gated Hold download missing contract: ${required}`);
}
if (holdDownloadRoute.includes("readV5CompletionFlag('v5_ls04")) {
  issues.push('Hold download incorrectly requires LS04 before its first successful response');
}
for (const required of ["'content', 'the-hold-v5'", "createHash('sha1')", 'SHA1_PATTERN']) {
  if (!holdArchiveHelper.includes(required)) issues.push(`private Hold archive helper missing contract: ${required}`);
}
if (holdArchiveHelper.includes("'public', 'the-hold'")) issues.push('Hold archive helper still reads a public asset path');
for (const required of ['./content/the-hold-v5/the-hold.zip', './content/the-hold-v5/the-hold.sha1']) {
  if (!nextConfig.includes(required)) issues.push(`Next output tracing omits private Hold artifact: ${required}`);
}

const zipPath = resolve(repo, 'dashboard/content/the-hold-v5/the-hold.zip');
if (existsSync(zipPath) && statSync(zipPath).size < 8_000) issues.push('V5 playable world is suspiciously small');
if (existsSync(zipPath)) {
  const actualSha1 = createHash('sha1').update(readFileSync(zipPath)).digest('hex');
  const declaredSha1 = read('dashboard/content/the-hold-v5/the-hold.sha1').trim().split(/\s+/)[0];
  if (actualSha1 !== declaredSha1) issues.push(`V5 playable-world SHA-1 mismatch: ${declaredSha1} != ${actualSha1}`);
}
const archiveManifest = read('dashboard/content/the-hold-v5/archive-manifest.tsv');
for (const required of ['the-hold.zip', 'Minecraft Java single-player recovery image', 'the-hold.sha1', 'README-FIRST.txt']) {
  if (!archiveManifest.includes(required)) issues.push(`V5 playable-world manifest missing ${required}`);
}
const archiveReadme = read('dashboard/content/the-hold-v5/README-FIRST.txt');
for (const required of ['Minecraft Java single-player world', 'Minecraft Java Edition 1.21.11', 'No client mods or resource pack']) {
  if (!archiveReadme.includes(required)) issues.push(`V5 playable-world README missing ${required}`);
}
for (const required of [
  '3-6-2',
  '1-4-3',
  '4-2-1',
  '2-5-4',
  'Relay tag OI',
  'Fault moved to Z',
  'Socket SN failed',
  'KER line stable',
  'RETURN 2',
  'RETURN 5',
  'RETURN 6',
  'RETURN 9',
  'standard host/service separator',
]) {
  if (!holdBuilder.includes(required)) issues.push(`playable Hold builder missing evidence contract: ${required}`);
}
if (/\b[a-z0-9.-]+\.(?:com|net|org|gg|io)\s*:\s*\d{2,5}\b/i.test(holdBuilder)) {
  issues.push('playable Hold builder exposes the assembled server endpoint');
}

for (const [path, label] of [
  ['dashboard/public/evidence/copperline/camp-frame-06.png', 'camp frame 06'],
  ['dashboard/public/evidence/copperline/locker-detail-13.png', 'locker detail 13'],
] as const) {
  const absolute = resolve(repo, path);
  if (!existsSync(absolute)) continue;
  const bytes = readFileSync(absolute);
  if (bytes.length < 100_000) issues.push(`${label} PNG is suspiciously small`);
  if (bytes.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') {
    issues.push(`${label} is not a valid PNG`);
    continue;
  }
  const width = bytes.readUInt32BE(16);
  const height = bytes.readUInt32BE(20);
  if (width !== 1448 || height !== 1086) issues.push(`${label} dimensions drifted: ${width}x${height}`);
}

for (const media of REQUIRED_MEDIA) {
  for (const value of [media.key, media.url, media.canonicalFilename, media.sha1, media.payload]) {
    if (!caseSeed.includes(value)) issues.push(`required media drift for ${media.key}: ${value}`);
  }
}

// Public source must not carry fixed-media URLs ahead of their database prerequisites. Four assets
// are delivered only through the gated archive view. Clip 1 is the sole exact-route exception, and
// that page must receipt A06 before A07 through the prerequisite-enforcing RPC.
const publicSources = sourceFiles('dashboard/src/app').filter((path) => !path.includes('/author/'));
for (const media of REQUIRED_MEDIA) {
  const occurrences = publicSources.filter((path) => read(path).includes(media.url));
  if (media.key === 'clip_01_ash_locker') {
    if (occurrences.length !== 1 || occurrences[0] !== 'dashboard/src/app/community/archive.php/page.tsx') {
      issues.push(`clip 1 URL must exist only on its gated exact route; found ${occurrences.join(', ') || 'nowhere'}`);
    }
  } else if (occurrences.length > 0) {
    issues.push(`${media.key} URL is hard-coded on a public page before prerequisite delivery: ${occurrences.join(', ')}`);
  }
}
const mediaView = read('dashboard/supabase/migrations/0010_v5_public_record.sql');
for (const token of ['v_required_media_delivery', 'unnest(m.prerequisite_flags)', "a.flags ->> required_flag"]) {
  if (!mediaView.includes(token)) issues.push(`required-media public view is not prerequisite-gated: ${token}`);
}
const webProgress = read('dashboard/src/lib/v5-web-progress.ts');
if (!webProgress.includes('prerequisites not satisfied')) issues.push('website receipt helper does not fail closed on unsatisfied prerequisites');
if (!caseSeed.includes("('A06','C07',6,'Copperline archive route'") || !caseSeed.includes("array['v5_a05_overlay'],'v5_a06_route'")) {
  issues.push('A06 is not gated on the earned A05 overlay');
}
for (const required of [
  "('LS04','C01',4,'Playable Hold handoff'",
  "'world download',array['v5_ls03_directory_trail'],'v5_ls04_map_handoff'",
  "('LS06','C01',5,'Surface dispatch filing'",
  "'tagged key deposit',array['v5_ls04_map_handoff'],'v5_ls06_relay'",
  "('LS05','C01',6,'Proof-bound identity'",
  "array['v5_ls06_relay'],'v5_case_c01_complete'",
]) {
  if (!caseSeed.includes(required)) issues.push(`C01 website-to-Minecraft handoff drift: ${required}`);
}

for (const stale of ['relayCallbackMatches', 'relay-form', '/support/ticket.php?id=1851', "'recovered-archive'", 'v5-ls05-callback']) {
  if (`${support}\n${remoteRoom}\n${caseSeed}`.includes(stale)) issues.push(`retired Copperline/media surface remains: ${stale}`);
}

const expectedVercelEnv = [
  'ADMIN_EMAILS', 'AUTHOR_PASSWORD', 'AUTHOR_USERNAME',
  'NEXT_PUBLIC_SUPABASE_ANON_KEY', 'NEXT_PUBLIC_SUPABASE_URL', 'SUPABASE_SERVICE_ROLE_KEY',
].sort();
const vercelEnvKeys = [...dashboardEnv.matchAll(/^([A-Z][A-Z0-9_]+)=/gm)].map((match) => match[1]!).sort();
if (JSON.stringify(vercelEnvKeys) !== JSON.stringify(expectedVercelEnv)) {
  issues.push(`Vercel .env.example drift: ${vercelEnvKeys.join(',')}`);
}
for (const key of expectedVercelEnv) if (!dashboardReadme.includes(key)) issues.push(`dashboard README omits Vercel variable ${key}`);
if (dashboardEnv.includes('DISCORD_INVITE_URL') || dashboardReadme.includes('DISCORD_INVITE_URL')) {
  issues.push('dashboard still configures the Discord invite before Minecraft LS06');
}
for (const [name, expected] of Object.entries({
  next: '16.2.10', react: '19.2.7', 'react-dom': '19.2.7',
  'eslint-config-next': '16.2.10', eslint: '9.39.5',
  '@types/react': '19.2.17', '@types/react-dom': '19.2.3',
})) {
  const actual = dashboardPackage.dependencies?.[name] ?? dashboardPackage.devDependencies?.[name];
  if (actual !== expected) issues.push(`dashboard package ${name} must be exact ${expected}; found ${actual ?? 'missing'}`);
}

if (issues.length > 0) {
  console.error(`webaudit: FAILED - ${issues.length} issue(s):`);
  for (const issue of issues) console.error(`  - ${issue}`);
  process.exit(1);
}
console.log('webaudit: OK - playable Hold handoff, LS06-owned Discord reveal, V5 Record, earned archive stills, and media are coherent');
