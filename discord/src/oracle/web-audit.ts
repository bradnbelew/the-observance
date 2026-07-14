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
  'dashboard/content/the-hold-v5/relay-route.svg',
];
for (const path of requiredFiles) if (!existsSync(resolve(repo, path))) issues.push(`missing V5 web artifact: ${path}`);
for (const path of [
  'dashboard/public/the-hold/the-hold.zip',
  'dashboard/public/the-hold/the-hold.sha1',
]) {
  if (existsSync(resolve(repo, path))) issues.push(`V5 archive bypass remains directly public: ${path}`);
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
for (const required of ['Ticket #9137', '/community/2011/02/08/world-backup/']) {
  if (!support.includes(required)) issues.push(`Copperline ticket trail missing contract: ${required}`);
}
for (const required of ['diagnostic export', 'not contain a playable world', 'V5_HOLD_ARCHIVE_DOWNLOAD_PATH', 'name="host"', 'name="callback"', 'name="route"', "'copperline_world_backup'"]) {
  if (!community.includes(required)) issues.push(`Copperline archive post missing corrected copy: ${required}`);
}
for (const required of ['/obslink', '/link YourExactMinecraftUsername CallbackFromArchive OneTimeCode', 'readV5CompletionFlag', 'v5_ls04_archive_solved']) {
  if (!remoteRoom.includes(required)) issues.push(`Copperline remote room missing handoff contract: ${required}`);
}
if (remoteRoom.includes('recordV5WebSequence')) issues.push('Copperline remote room still owns progression instead of remaining read-only');
for (const required of ["params.service === '1842'", "params.ticket === '9137'", "params.locker === '13'", 'https://youtu.be/du-qp_clP7c', "['A06']", "'copperline_archive_route'", "['A07']", "'clip_01_ash_locker'"]) {
  if (!priorArchive.includes(required)) issues.push(`A06 exact archive route missing contract: ${required}`);
}

const publicCopy = `${record}\n${archive}\n${terminal}\n${support}\n${community}\n${remoteRoom}\n${priorArchive}`.toLowerCase();
for (const stale of ['seventh:', 'six plus one', 'type <code>kept</code>', 'optional side']) {
  if (publicCopy.includes(stale)) issues.push(`public V5 copy retains retired phrase: ${stale}`);
}

for (const required of [
  "dynamic = 'force-dynamic'",
  'revalidate = 0',
  "readV5CompletionFlag('v5_ls03_directory_trail')",
  'readValidatedV5HoldArchive',
  'genericNotFound()',
  'status: 404',
  "'Cache-Control': 'private, no-store, max-age=0, must-revalidate'",
  "'Content-Type': 'application/zip'",
]) {
  if (!holdDownloadRoute.includes(required)) issues.push(`gated Hold download missing contract: ${required}`);
}
if (holdDownloadRoute.includes('v5_ls04_archive_solved')) {
  issues.push('Hold download incorrectly requires LS04 even though LS04 depends on the archive');
}
for (const required of ["'content', 'the-hold-v5'", "createHash('sha1')", 'SHA1_PATTERN']) {
  if (!holdArchiveHelper.includes(required)) issues.push(`private Hold archive helper missing contract: ${required}`);
}
if (holdArchiveHelper.includes("'public', 'the-hold'")) issues.push('Hold archive helper still reads a public asset path');
for (const required of ['./content/the-hold-v5/the-hold.zip', './content/the-hold-v5/the-hold.sha1']) {
  if (!nextConfig.includes(required)) issues.push(`Next output tracing omits private Hold artifact: ${required}`);
}

const zipPath = resolve(repo, 'dashboard/content/the-hold-v5/the-hold.zip');
if (existsSync(zipPath) && statSync(zipPath).size < 1_000) issues.push('V5 diagnostic archive is suspiciously small');
if (existsSync(zipPath)) {
  const actualSha1 = createHash('sha1').update(readFileSync(zipPath)).digest('hex');
  const declaredSha1 = read('dashboard/content/the-hold-v5/the-hold.sha1').trim().split(/\s+/)[0];
  if (actualSha1 !== declaredSha1) issues.push(`V5 diagnostic archive SHA-1 mismatch: ${declaredSha1} != ${actualSha1}`);
}
const archiveManifest = read('dashboard/content/the-hold-v5/archive-manifest.tsv');
for (const name of ['mkept', 'Rook', 'Ash', 'Wren']) if (!archiveManifest.includes(name)) issues.push(`V5 diagnostic archive manifest missing ${name}`);
const archiveReadme = read('dashboard/content/the-hold-v5/README-FIRST.txt');
for (const required of ['host fragments', 'relay-route.svg', '/obslink', '/link', 'one-time code', 'not a website password']) if (!archiveReadme.includes(required)) issues.push(`V5 diagnostic README missing ${required}`);
for (const required of ['A-copper.txt', 'B-line.txt', 'C-hosting.txt', 'D-dot-com.txt']) if (!archiveManifest.includes(required)) issues.push(`V5 diagnostic archive host fragment missing ${required}`);

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

for (const stale of ['relayCallbackMatches', 'relay-form', '/support/ticket.php?id=1851', "'recovered-archive'", 'v5-ls05-callback']) {
  if (`${support}\n${remoteRoom}\n${caseSeed}`.includes(stale)) issues.push(`retired Copperline/media surface remains: ${stale}`);
}

const expectedVercelEnv = [
  'ADMIN_EMAILS', 'AUTHOR_PASSWORD', 'AUTHOR_USERNAME', 'DISCORD_INVITE_URL',
  'NEXT_PUBLIC_SUPABASE_ANON_KEY', 'NEXT_PUBLIC_SUPABASE_URL', 'SUPABASE_SERVICE_ROLE_KEY',
].sort();
const vercelEnvKeys = [...dashboardEnv.matchAll(/^([A-Z][A-Z0-9_]+)=/gm)].map((match) => match[1]!).sort();
if (JSON.stringify(vercelEnvKeys) !== JSON.stringify(expectedVercelEnv)) {
  issues.push(`Vercel .env.example drift: ${vercelEnvKeys.join(',')}`);
}
for (const key of expectedVercelEnv) if (!dashboardReadme.includes(key)) issues.push(`dashboard README omits Vercel variable ${key}`);
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
console.log('webaudit: OK - Copperline handoff, V5 Record, earned archive, terminal, and fixed media are coherent');
