import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { inflateRawSync } from 'node:zlib';

const root = process.cwd();
const contentDirectory = join(root, 'content', 'the-hold-v5');
const zipPath = join(contentDirectory, 'the-hold.zip');
const sha1Path = join(contentDirectory, 'the-hold.sha1');
const publicZipPath = join(root, 'public', 'the-hold', 'the-hold.zip');
const publicSha1Path = join(root, 'public', 'the-hold', 'the-hold.sha1');
const route = readFileSync(join(root, 'src', 'app', 'the-hold', 'the-hold.zip', 'route.ts'), 'utf8');
const helper = readFileSync(join(root, 'src', 'lib', 'v5-hold-archive.ts'), 'utf8');
const community = readFileSync(join(root, 'src', 'app', 'community', '2011', '02', '08', 'world-backup', 'page.tsx'), 'utf8');
const ticket = readFileSync(join(root, 'src', 'app', 'support', 'ticket.php', 'page.tsx'), 'utf8');
const remoteRoom = readFileSync(join(root, 'src', 'app', 'community', 'remote-room.php', 'page.tsx'), 'utf8');
const record = readFileSync(join(root, 'src', 'app', 'record', '[slug]', 'page.tsx'), 'utf8');
const nextConfig = readFileSync(join(root, 'next.config.mjs'), 'utf8');
const readme = readFileSync(join(root, 'README.md'), 'utf8');
const environmentExample = readFileSync(join(root, '.env.example'), 'utf8');
const packageJson = readFileSync(join(root, 'package.json'), 'utf8');

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`V5 Hold archive self-test failed: ${message}`);
}

function readZipEntries(bytes: Buffer): Map<string, Buffer> {
  const endSignature = Buffer.from([0x50, 0x4b, 0x05, 0x06]);
  const endOffset = bytes.lastIndexOf(endSignature);
  check(endOffset >= 0, 'archive has no end-of-central-directory record');

  const entryCount = bytes.readUInt16LE(endOffset + 10);
  let centralOffset = bytes.readUInt32LE(endOffset + 16);
  const entries = new Map<string, Buffer>();

  for (let index = 0; index < entryCount; index += 1) {
    check(bytes.readUInt32LE(centralOffset) === 0x02014b50, `central directory entry ${index} is malformed`);
    const method = bytes.readUInt16LE(centralOffset + 10);
    const compressedSize = bytes.readUInt32LE(centralOffset + 20);
    const uncompressedSize = bytes.readUInt32LE(centralOffset + 24);
    const nameLength = bytes.readUInt16LE(centralOffset + 28);
    const extraLength = bytes.readUInt16LE(centralOffset + 30);
    const commentLength = bytes.readUInt16LE(centralOffset + 32);
    const localOffset = bytes.readUInt32LE(centralOffset + 42);
    const name = bytes.subarray(centralOffset + 46, centralOffset + 46 + nameLength).toString('utf8');

    check(bytes.readUInt32LE(localOffset) === 0x04034b50, `local entry is malformed: ${name}`);
    const localNameLength = bytes.readUInt16LE(localOffset + 26);
    const localExtraLength = bytes.readUInt16LE(localOffset + 28);
    const dataOffset = localOffset + 30 + localNameLength + localExtraLength;
    const compressed = bytes.subarray(dataOffset, dataOffset + compressedSize);
    const payload = method === 0 ? Buffer.from(compressed) : method === 8 ? inflateRawSync(compressed) : null;
    check(payload !== null, `unsupported compression method ${method}: ${name}`);
    check(payload!.length === uncompressedSize, `uncompressed size drifted: ${name}`);
    entries.set(name, payload!);

    centralOffset += 46 + nameLength + extraLength + commentLength;
  }

  return entries;
}

check(existsSync(zipPath) && statSync(zipPath).size > 8_000, 'private playable world is missing or suspiciously small');
check(existsSync(sha1Path), 'private archive checksum is missing');
check(!existsSync(publicZipPath) && !existsSync(publicSha1Path), 'archive or checksum remains directly public');

const archiveBytes = readFileSync(zipPath);
const actualSha1 = createHash('sha1').update(archiveBytes).digest('hex');
const declaredSha1 = readFileSync(sha1Path, 'utf8').trim().split(/\s+/)[0];
check(actualSha1 === declaredSha1, `archive checksum drifted: ${declaredSha1} != ${actualSha1}`);

const entries = readZipEntries(archiveBytes);
const expectedEntries = new Set([
  'the-hold/level.dat',
  'the-hold/level.dat_old',
  'the-hold/datapacks/the_hold/pack.mcmeta',
  'the-hold/datapacks/the_hold/data/minecraft/tags/function/load.json',
  'the-hold/datapacks/the_hold/data/minecraft/tags/function/tick.json',
  'the-hold/datapacks/the_hold/data/the_hold/function/load.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/spawn.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/return.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/tick.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/build/all.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/build/receiving.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/build/records.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/build/dispatch.mcfunction',
  'the-hold/datapacks/the_hold/data/the_hold/function/build/passages.mcfunction',
]);
check(entries.size === expectedEntries.size, `world contains ${entries.size} files instead of ${expectedEntries.size}`);
for (const entry of expectedEntries) check(entries.has(entry), `playable world is missing ${entry}`);
for (const entry of entries.keys()) check(expectedEntries.has(entry), `playable world contains stale entry ${entry}`);

check(entries.get('the-hold/level.dat')!.length > 1_000, 'level.dat is missing or truncated');
const packMeta = JSON.parse(entries.get('the-hold/datapacks/the_hold/pack.mcmeta')!.toString('utf8')) as {
  pack?: { min_format?: number; max_format?: number };
};
check(packMeta.pack?.min_format === 94 && packMeta.pack?.max_format === 94, 'datapack must retain format 94');

const functionText = [...entries.entries()]
  .filter(([name]) => name.endsWith('.mcfunction'))
  .map(([, payload]) => payload.toString('utf8'))
  .join('\n');
for (const required of [
  'gamemode adventure @s',
  'function the_hold:build/receiving',
  'function the_hold:build/records',
  'function the_hold:build/dispatch',
  '3-6-2\\n\\n1-4-3\\n\\n4-2-1\\n\\n2-5-4',
  'Relay tag OI',
  'Fault moved to Z',
  'Socket SN failed',
  'KER line stable',
  'commercial DNS class',
  'standard host/service separator',
  'RETURN 2',
  'RETURN 5',
  'RETURN 6',
  'RETURN 9',
]) {
  check(functionText.includes(required), `playable world evidence is missing: ${required}`);
}
check((functionText.match(/minecraft:lectern\[facing=/g) ?? []).length === 8, 'all eight authored books need lecterns');
check((functionText.match(/data merge block/g) ?? []).length === 8, 'one or more lecterns is missing its book');
check(!/\b[a-z0-9.-]+\.(?:com|net|org|gg|io)\s*:\s*\d{2,5}\b/i.test(functionText), 'world exposes a complete endpoint');
check(!/\b(?:discord|arg|puzzle|placeholder|todo|tbd)\b/i.test(functionText), 'world contains stale or meta-facing prose');

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
  check(route.includes(required), `download route missing contract: ${required}`);
}
check(!route.includes("readV5CompletionFlag('v5_ls04"), 'the first download must not require LS04 in advance');
check(helper.includes("'content', 'the-hold-v5'"), 'archive helper does not resolve the private content directory');
check(!helper.includes("'public', 'the-hold'"), 'archive helper still resolves the public directory');
check(helper.includes("createHash('sha1')"), 'archive helper does not validate the checksum at runtime');
check(community.includes('V5_HOLD_ARCHIVE_DOWNLOAD_PATH'), 'community download does not use the gated route');
check(community.includes('Java Edition 1.21.11 world'), 'community post does not describe the playable world accurately');
check(!/diagnostic|archive-resolver|remote-room|discord/i.test(community), 'community post retains the old resolver or early-Discord copy');
check(ticket.includes('single-player recovery image'), 'ticket does not route players to the playable recovery image');
check(!/diagnostic export|no playable world|live server address/i.test(ticket), 'ticket retains contradictory diagnostic copy');
check(record.includes('V5_HOLD_ARCHIVE_DOWNLOAD_PATH'), 'mkept mirror download does not use the gated route');
check(!/stripped recovery copy|four work areas|remote-room|discord/i.test(record), 'mkept mirror retains obsolete archive copy');
check(remoteRoom.includes('return <MissingArchiveEntry />'), 'remote-room route must remain a fail-closed tombstone');
check(!/DISCORD_INVITE_URL|safeDiscordInvite|discord\.gg|discord\.com\/invite/i.test(remoteRoom), 'remote-room route can still reveal Discord');
check(!readme.includes('diagnostic archive'), 'dashboard README still documents the diagnostic archive');
check(!readme.includes('DISCORD_INVITE_URL'), 'dashboard README still configures a website Discord invite');
check(!environmentExample.includes('DISCORD_INVITE_URL'), 'dashboard environment still accepts an early Discord invite');
check(!packageJson.includes('discord-relay.selftest'), 'dashboard selftest still runs the retired Discord relay contract');
for (const required of ['outputFileTracingIncludes', './content/the-hold-v5/the-hold.zip', './content/the-hold-v5/the-hold.sha1']) {
  check(nextConfig.includes(required), `Next output tracing is missing: ${required}`);
}

console.log(
  `V5 Hold archive self-test: OK - playable 3-room world, format 94, private LS03 gate, checksum ${actualSha1}`,
);
