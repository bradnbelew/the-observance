import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { normalizeAnswer } from '../oracle/normalize.js';
import { isCopperlineCallback, normalizeCopperlineCallback } from './identity.js';

type Row = Record<string, string>;

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const read = (path: string) => readFileSync(resolve(repo, path), 'utf8');
const fail = (message: string): never => { throw new Error(`V5 runtime binding selftest FAILED: ${message}`); };

function parseCsv(source: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < source.length; i += 1) {
    const char = source[i]!;
    if (char === '"') {
      if (quoted && source[i + 1] === '"') { field += '"'; i += 1; }
      else quoted = !quoted;
    } else if (char === ',' && !quoted) {
      row.push(field); field = '';
    } else if ((char === '\n' || char === '\r') && !quoted) {
      if (char === '\r' && source[i + 1] === '\n') i += 1;
      row.push(field); field = '';
      if (row.some((value) => value !== '')) rows.push(row);
      row = [];
    } else {
      field += char;
    }
  }
  if (field !== '' || row.length > 0) {
    row.push(field);
    if (row.some((value) => value !== '')) rows.push(row);
  }
  return rows;
}

function csvObjects(path: string): Row[] {
  const rows = parseCsv(read(path));
  const headers = rows.shift() ?? fail(`${path} is empty`);
  return rows.map((values, index) => {
    if (values.length !== headers.length) fail(`${path} row ${index + 2} has ${values.length}/${headers.length} fields`);
    return Object.fromEntries(headers.map((header, column) => [header, values[column] ?? '']));
  });
}

const bindings = csvObjects('design/ARG-V5-RUNTIME-BINDINGS.csv');
const nodes = csvObjects('design/ARG-V5-NODE-MANIFEST.csv');
if (bindings.length !== 82) fail(`expected 82 runtime bindings, found ${bindings.length}`);
if (nodes.length !== 82) fail(`expected 82 manifest nodes, found ${nodes.length}`);

const bindingByNode = new Map(bindings.map((row) => [row.node_id!, row] as const));
const manifestByNode = new Map(nodes.map((row) => [row.node_id!, row] as const));
if (bindingByNode.size !== 82) fail('runtime binding node IDs are not unique');
if (manifestByNode.size !== 82) fail('node manifest IDs are not unique');
for (const [nodeId, binding] of bindingByNode) {
  const manifest = manifestByNode.get(nodeId) ?? fail(`${nodeId} has a runtime binding but no node manifest row`);
  if (!binding.owner || !binding.handler || !binding.site_id || !binding.replay_policy) fail(`${nodeId} has an empty runtime field`);
  if (!manifest.input_surface) fail(`${nodeId} has no player input surface`);
  if (binding.completion_flag !== manifest.completion_flag) {
    fail(`${nodeId} completion flag drift: ${binding.completion_flag} != ${manifest.completion_flag}`);
  }
}
for (const nodeId of manifestByNode.keys()) if (!bindingByNode.has(nodeId)) fail(`${nodeId} has no runtime binding`);

const seed = read('discord/supabase/seeds/v5_investigations.sql');
const metadataStart = seed.indexOf('from (values', seed.indexOf('-- Runtime ownership is seeded'));
const metadataEnd = seed.indexOf(') as binding(node_key, owner, handler, site_id, completion_flag, replay_policy)', metadataStart);
if (metadataStart < 0 || metadataEnd < 0) fail('seeded runtime metadata block is missing');
const metadataBlock = seed.slice(metadataStart, metadataEnd);
const metadataRows = [...metadataBlock.matchAll(/^\s*\('([A-Z]{1,2}\d{2})','([^']+)','([^']+)','([^']+)','([^']+)','([^']+)'\),?$/gm)]
  .map((match) => ({
    node_id: match[1]!, owner: match[2]!, handler: match[3]!, site_id: match[4]!,
    completion_flag: match[5]!, replay_policy: match[6]!,
  }));
if (metadataRows.length !== 82 || new Set(metadataRows.map((row) => row.node_id)).size !== 82) {
  fail(`seed must contain 82 unique runtime metadata rows, found ${metadataRows.length}`);
}
for (const seeded of metadataRows) {
  const canonical = bindingByNode.get(seeded.node_id) ?? fail(`seed metadata has unknown node ${seeded.node_id}`);
  for (const field of ['owner', 'handler', 'site_id', 'completion_flag', 'replay_policy'] as const) {
    if (seeded[field] !== canonical[field]) fail(`${seeded.node_id} seed ${field} drift: ${seeded[field]} != ${canonical[field]}`);
  }
}

// Every node has an authored rescue rail. Two headings intentionally cover paired nodes; RP06 is an
// automatic coda and explicitly has operator resume diagnostics instead of a fake player hint.
const casebook = read('arc/v5/SOLUTION-CASEBOOK.md');
const sections = [...casebook.matchAll(/^### ([^\r\n]+)$/gm)].map((match, index, all) => ({
  heading: match[1]!,
  body: casebook.slice(match.index!, all[index + 1]?.index ?? casebook.length),
}));
for (const nodeId of bindingByNode.keys()) {
  const section = sections.find(({ heading }) => (heading.match(/[A-Z]{1,2}\d{2}/g) as string[] | null)?.includes(nodeId) === true)
    ?? fail(`${nodeId} has no solution-casebook section`);
  if (nodeId !== 'RP06') {
    for (const tier of ['- H1:', '- H2:', '- H3:']) if (!section.body.includes(tier)) fail(`${nodeId} is missing ${tier}`);
  } else if (!section.body.includes('There is no solve hint for RP06')) {
    fail('RP06 must explicitly route stalls to finale resume diagnostics');
  }
}

const websiteSurfaces: Record<string, [string, ...string[]]> = {
  LS01: ['dashboard/src/app/server.php/page.tsx', "['LS01']", "'copperline_traces'", "handler: 'route_receipt'"],
  // LS02's fixed ownership token belongs in the server-only action. Keeping it out of the
  // rendered page prevents a client-controlled node id and makes the answer boundary auditable.
  LS02: ['dashboard/src/app/server.php/actions.ts', "['LS02']", "'copperline_service_1842'", "handler: 'answer_resolver'", 'resolveLs02ServiceDocket'],
  LS03: ['dashboard/src/app/support/ticket.php/page.tsx', "['LS03']", "'copperline_directory'", "handler: 'route_receipt'"],
  LS04: ['dashboard/src/app/the-hold/the-hold.zip/route.ts', "['LS04']", "'copperline_world_backup'", "handler: 'world_download'", 'readValidatedV5HoldArchive'],
  A06: ['dashboard/src/app/community/archive.php/page.tsx', "['A06']", "'copperline_archive_route'", "handler: 'route_resolver'"],
  A07: ['dashboard/src/app/community/archive.php/page.tsx', "['A07']", "'clip_01_ash_locker'", "handler: 'automatic_media_reveal'"],
};
for (const [nodeId, [path, ...tokens]] of Object.entries(websiteSurfaces)) {
  const binding = bindingByNode.get(nodeId);
  if (binding?.owner !== 'website') fail(`${nodeId} implementation is website-owned but binding says ${binding?.owner}`);
  const source = read(path);
  for (const token of tokens) if (!source.includes(token)) fail(`${nodeId} website implementation missing ${token} in ${path}`);
}
const ls02Page = read('dashboard/src/app/server.php/page.tsx');
for (const token of ['LS02_DOCKET_CIPHER', 'LS02_TEACHING_STRIP', '<ServiceDocketForm />']) {
  if (!ls02Page.includes(token)) fail(`LS02 investigation surface missing ${token} in dashboard/src/app/server.php/page.tsx`);
}
if (Object.keys(websiteSurfaces).length !== bindings.filter((row) => row.owner === 'website').length) fail('website implementation map is incomplete');

const discordOracle: Record<string, string> = {
  LC05: 'v5-lc05-motive', KO03: 'v5-ko03-crack-map', KB01: 'v5-kb01-stay-awake',
  KB03: 'v5-kb03-altered-watch', KI03: 'v5-ki03-iss-correction', CW05: 'v5-cw05-counterfeit',
  CW06: 'v5-cw06-reeds', CW08: 'v5-cw08-clear-nessa', BI08: 'v5-bi08-break-inquest',
  A01: 'v5-a01-camp-ash', A08: 'v5-a08-ash-13', A10: 'v5-a10-private-window',
  AR01: 'v5-ar01-not-kept', AR08: 'v5-ar08-averyn', RP01: 'v5-rp01-release-instruction',
};
const nodeInsertStart = seed.indexOf('insert into public.investigation_nodes');
const nodeInsertEnd = seed.indexOf('on conflict (node_key)', nodeInsertStart);
const nodeInsert = seed.slice(nodeInsertStart, nodeInsertEnd);
for (const [nodeId, puzzleKey] of Object.entries(discordOracle)) {
  const binding = bindingByNode.get(nodeId);
  if (binding?.owner !== 'discord') fail(`${nodeId} oracle implementation is Discord-owned but binding says ${binding?.owner}`);
  const nodeLine = nodeInsert.split(/\r?\n/).find((line) => line.trimStart().startsWith(`('${nodeId}'`));
  if (!nodeLine?.includes(`'${puzzleKey}'`)) fail(`${nodeId} is not mapped to ${puzzleKey} in investigation_nodes`);
  const puzzleLine = seed.split(/\r?\n/).find((line) => line.trimStart().startsWith(`('${puzzleKey}'`));
  const answers = puzzleLine?.match(/array\[([^\]]+)\]/)?.[1] ?? fail(`${puzzleKey} has no accepted-answer array`);
  const values = [...answers.matchAll(/'((?:''|[^'])*)'/g)].map((match) => match[1]!.replace(/''/g, "'"));
  if (values.length === 0) fail(`${puzzleKey} has an empty accepted-answer array`);
  for (const value of values) if (normalizeAnswer(value) !== value) fail(`${puzzleKey} answer is not normalized: ${value}`);
  for (const tier of [1, 2, 3]) if (!seed.includes(`('${puzzleKey}',${tier},`)) fail(`${puzzleKey} is missing hint tier ${tier}`);
}

const link = read('discord/src/bot/commands/link.ts');
const repoSource = read('discord/src/db/repo.ts');
const register = read('discord/src/bot/register.ts');
for (const token of ['claimIdentityHandoff', "getString('callback', true)", "getString('code', true)", 'handoffReceipt']) {
  if (!link.includes(token)) fail(`LS05 link handler missing ${token}`);
}
for (const token of ['observance_claim_identity_handoff', 'isCopperlineCallback', 'hashIdentityLinkCode', 'p_code_hash', 'identity handoff RPC returned no valid claim state']) {
  if (!repoSource.includes(token)) fail(`LS05 durable identity receipt missing ${token}`);
}
if (!register.includes(".setName('link')") || !register.includes(".setName('callback')")
  || !register.includes(".setName('code')") || !register.includes('.setRequired(true)')) {
  fail('LS05 /link name callback code registration is incomplete');
}
if (existsSync(resolve(repo, 'discord/src/bot/commands/callback.ts')) || seed.includes('v5-ls05-callback')) {
  fail('retired standalone /callback oracle remains reachable');
}
if (bindings.filter((row) => row.owner === 'discord').length !== Object.keys(discordOracle).length + 1) {
  fail('Discord implementation map is incomplete');
}
for (const accepted of ['9137', '9 1 3 7', 'ticket #9137']) {
  if (!isCopperlineCallback(accepted)) fail(`LS05 callback normalizer rejected ${accepted}`);
}
for (const rejected of ['', '913', '91370', '91O7', '13']) {
  if (isCopperlineCallback(rejected)) fail(`LS05 callback normalizer accepted ${rejected || '<empty>'}`);
}
if (normalizeCopperlineCallback('ticket #9137') !== '9137') fail('LS05 callback normalization is not decimal-only');

const ownership = new Map<string, number>();
for (const row of bindings) ownership.set(row.owner!, (ownership.get(row.owner!) ?? 0) + 1);
console.log(
  `V5 runtime binding selftest OK: 82/82 nodes; website=${ownership.get('website')}; `
  + `Discord=${ownership.get('discord')} (15 normalized oracle inputs + LS05 identity); `
  + `plugin=${(ownership.get('plugin') ?? 0) + (ownership.get('plugin_unlit') ?? 0) + (ownership.get('plugin_finale') ?? 0)}`,
);
