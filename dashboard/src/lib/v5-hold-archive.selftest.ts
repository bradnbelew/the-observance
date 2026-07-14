import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const contentDirectory = join(root, 'content', 'the-hold-v5');
const zipPath = join(contentDirectory, 'the-hold.zip');
const sha1Path = join(contentDirectory, 'the-hold.sha1');
const publicZipPath = join(root, 'public', 'the-hold', 'the-hold.zip');
const publicSha1Path = join(root, 'public', 'the-hold', 'the-hold.sha1');
const route = readFileSync(join(root, 'src', 'app', 'the-hold', 'the-hold.zip', 'route.ts'), 'utf8');
const helper = readFileSync(join(root, 'src', 'lib', 'v5-hold-archive.ts'), 'utf8');
const community = readFileSync(join(root, 'src', 'app', 'community', '2011', '02', '08', 'world-backup', 'page.tsx'), 'utf8');
const record = readFileSync(join(root, 'src', 'app', 'record', '[slug]', 'page.tsx'), 'utf8');
const nextConfig = readFileSync(join(root, 'next.config.mjs'), 'utf8');

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`V5 Hold archive self-test failed: ${message}`);
}

check(existsSync(zipPath) && statSync(zipPath).size > 1_000, 'private archive is missing or suspiciously small');
check(existsSync(sha1Path), 'private archive checksum is missing');
check(!existsSync(publicZipPath) && !existsSync(publicSha1Path), 'archive or checksum remains directly public');

const actualSha1 = createHash('sha1').update(readFileSync(zipPath)).digest('hex');
const declaredSha1 = readFileSync(sha1Path, 'utf8').trim().split(/\s+/)[0];
check(actualSha1 === declaredSha1, `archive checksum drifted: ${declaredSha1} != ${actualSha1}`);

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
  check(route.includes(required), `download route missing contract: ${required}`);
}
check(!route.includes('v5_ls04_archive_solved'), 'download must remain available between LS03 and LS04');
check(helper.includes("'content', 'the-hold-v5'"), 'archive helper does not resolve the private content directory');
check(!helper.includes("'public', 'the-hold'"), 'archive helper still resolves the public directory');
check(helper.includes("createHash('sha1')"), 'archive helper does not validate the checksum at runtime');
check(community.includes('V5_HOLD_ARCHIVE_DOWNLOAD_PATH'), 'community download does not use the gated route');
check(record.includes('V5_HOLD_ARCHIVE_DOWNLOAD_PATH'), 'mkept mirror download does not use the gated route');
for (const required of ['outputFileTracingIncludes', './content/the-hold-v5/the-hold.zip', './content/the-hold-v5/the-hold.sha1']) {
  check(nextConfig.includes(required), `Next output tracing is missing: ${required}`);
}

console.log(`V5 Hold archive self-test: OK - private, LS03-gated, no-store, checksum ${actualSha1}`);
