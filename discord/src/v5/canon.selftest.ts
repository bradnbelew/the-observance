import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { normalizeAnswer } from '../oracle/normalize.js';
import { REQUIRED_MEDIA } from './media.js';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const seed = readFileSync(resolve(repo, 'discord/supabase/seeds/v5_investigations.sql'), 'utf8');
const mediaManifest = JSON.parse(readFileSync(resolve(repo, 'arc/v5/media-manifest.json'), 'utf8')) as {
  assets: Array<{
    id: string; nodeId: string; solveNodeId?: string; caseId: string;
    canonicalFilename: string; hostedFilename?: string; url?: string; archiveUrl?: string;
    expectedPayload: string; acceptedAliases: string[]; sourceBytes: number; sourceSha1: string;
    revealPrerequisite: string; completionFlag: string; narrativeUse: string;
  }>;
};

function fail(message: string): never {
  throw new Error(`V5 canon selftest FAILED: ${message}`);
}

const nodeRows = [...seed.matchAll(/^\+?\s*\('([A-Z]{1,2}\d{2})','(C(?:0[1-9]|10))',(\d+),/gm)]
  .map((match) => ({ node: match[1]!, caseKey: match[2]!, ordinal: Number(match[3]) }));
const uniqueNodes = new Set(nodeRows.map((row) => row.node));
if (nodeRows.length !== 82 || uniqueNodes.size !== 82) fail(`expected 82 unique node rows, found ${nodeRows.length}/${uniqueNodes.size}`);

const expected: Record<string, number> = { C01: 6, C02: 6, C03: 18, C04: 8, C05: 8, C06: 7, C07: 10, C08: 5, C09: 8, C10: 6 };
for (const [caseKey, count] of Object.entries(expected)) {
  const rows = nodeRows.filter((row) => row.caseKey === caseKey);
  if (rows.length !== count) fail(`${caseKey} expected ${count} nodes, found ${rows.length}`);
  const ordinals = rows.map((row) => row.ordinal).sort((a, b) => a - b);
  if (ordinals.some((value, index) => value !== index + 1)) fail(`${caseKey} ordinals are not contiguous`);
}

for (const required of [
  'update public.puzzles set active = false',
  'update public.hints set active = false',
  'update public.thread_cards set active = false',
  'update public.side_quests set active = false',
  'update public.required_media set active = false',
  'update public.investigation_nodes set active = false',
  'update public.investigations set active = false',
  "'v5_case_c10_complete'",
  "'v5_queue_retirement_complete'",
]) if (!seed.includes(required)) fail(`missing retirement/completion contract: ${required}`);

if (REQUIRED_MEDIA.length !== 5 || mediaManifest.assets.length !== 5) {
  fail(`expected five fixed media assets, found runtime=${REQUIRED_MEDIA.length}, canon=${mediaManifest.assets.length}`);
}

for (const canonical of mediaManifest.assets) {
  const runtime = REQUIRED_MEDIA.find((asset) => asset.key === canonical.id);
  if (!runtime) fail(`runtime media missing canonical id ${canonical.id}`);
  const expectedRuntime = {
    key: canonical.id,
    caseKey: canonical.caseId,
    nodeKey: canonical.nodeId,
    solveNodeKey: canonical.solveNodeId,
    kind: canonical.url ? 'video' : 'audio',
    url: canonical.url ?? canonical.archiveUrl,
    canonicalFilename: canonical.canonicalFilename,
    hostedFilename: canonical.hostedFilename,
    acceptedAliases: canonical.acceptedAliases,
    sourceBytes: canonical.sourceBytes,
    sha1: canonical.sourceSha1,
    payload: canonical.expectedPayload,
    prerequisiteFlag: canonical.revealPrerequisite,
    completionFlag: canonical.completionFlag,
    narrativeUse: canonical.narrativeUse,
  };
  if (JSON.stringify(runtime) !== JSON.stringify(expectedRuntime)) fail(`${canonical.id} runtime fields drift from media-manifest.json`);
  for (const field of [runtime.key, runtime.nodeKey, runtime.url, runtime.canonicalFilename, runtime.sha1, runtime.payload, runtime.prerequisiteFlag]) {
    if (!seed.includes(field)) fail(`${runtime.key} seed drift: missing ${field}`);
  }
  for (const alias of runtime.acceptedAliases) {
    if (!seed.includes(normalizeAnswer(alias))) fail(`${runtime.key} accepted alias is absent from V5 puzzle rows: ${alias}`);
  }
}

const brannClip = REQUIRED_MEDIA.find((asset) => asset.key === 'clip_03_watch_correction');
if (!brannClip || JSON.stringify(brannClip.acceptedAliases) !== JSON.stringify(['stay awake'])) {
  fail('KB01 must accept only the exact normalized STAY AWAKE payload; broad guesses bypass required clip 3');
}
for (const forbidden of ["'awake'", "'do not close your eyes'"]) {
  const row = seed.split(/\r?\n/).find((line) => line.includes("('v5-kb01-stay-awake'"));
  if (row?.includes(forbidden)) fail(`KB01 seed retained materially weaker alias ${forbidden}`);
}

for (const retired of ["'clip-01'", "'clip-02'", "'clip-03'", "'clip-04'", "'spectrogram-01'", "'recovered-archive'"]) {
  if (seed.includes(retired)) fail(`retired provisional media row remains active in seed: ${retired}`);
}

for (const exact of [
  "('AR01','C09',1,'Recovered archive spectrogram','accepting'",
  "('AR08','C09',8,'Assemble AVERYN','accepting'",
  "('RP01','C10',1,'Release footage','unwriting'",
  "('RP04','C10',4,'Any-subset release confirmation','unwriting'",
  "('RP05','C10',5,'Sever the Record','release'",
]) if (!seed.includes(exact)) fail(`room topology drift: ${exact}`);

console.log('V5 canon selftest OK: 10 mandatory cases, 82 nodes, 5 canonical fixed-media assets');
