import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  hashIdentityLinkCode,
  isCopperlineCallback,
  normalizeIdentityLinkCode,
} from './identity.js';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const read = (path: string): string => readFileSync(resolve(repo, path), 'utf8');
const fail = (message: string): never => { throw new Error(`identity link selftest FAILED: ${message}`); };

for (const accepted of ['9137', '9 1 3 7', 'ticket #9137']) {
  if (!isCopperlineCallback(accepted)) fail(`callback normalizer rejected ${accepted}`);
}
for (const rejected of ['', '913', '91370', '91O7', '13']) {
  if (isCopperlineCallback(rejected)) fail(`callback normalizer accepted ${rejected || '<empty>'}`);
}
if (normalizeIdentityLinkCode('abcd-efgh-jkmp') !== 'ABCDEFGHJKMP') fail('proof-code display normalization drifted');
if (normalizeIdentityLinkCode('ＡＢＣＤ ＥＦＧＨ ＪＫＭＰ') !== 'ABCDEFGHJKMP') fail('proof-code width normalization drifted');
for (const rejected of ['', 'ABCD-EFGH-IJKL', 'ABCD-EFGH-JKM', 'ABCD-EFGH-JKMPX']) {
  if (normalizeIdentityLinkCode(rejected)) fail(`invalid proof code accepted: ${rejected || '<empty>'}`);
}
if (hashIdentityLinkCode('ABCD-EFGH-JKMP') !== '58b71f1d45d2b6d407ec0262d2b9f5b85f56cf4605f596bf977c85bc2e01397c') {
  fail('proof-code SHA-256 differs from Paper');
}

const handler = read('discord/src/bot/commands/link.ts');
for (const required of [
  'claimIdentityHandoff',
  "result.state === 'invalid'",
  "result.state === 'blocked'",
  "result.state === 'unknown' || result.state === 'conflict'",
  "result.state === 'challenge'",
  "getString('code', true)",
  'handoffProofRejected',
  'result.recovered',
  'MessageFlags.Ephemeral',
]) if (!handler.includes(required)) fail(`handler is missing ${required}`);
for (const retired of ['linkDiscord(', 'recordIdentityHandoff(', 'getPlayerByDiscordId(']) {
  if (handler.includes(retired)) fail(`handler still reaches split/claim-first path ${retired}`);
}

const repository = read('discord/src/db/repo.ts');
for (const retired of [
  'export async function linkDiscord(',
  'export async function recordIdentityHandoff(',
  'export type IdentityHandoffState',
  'export interface IdentityHandoffResult',
  ".update({ discord_id:",
]) {
  if (repository.includes(retired)) fail(`repository still publishes split/claim-first identity authority: ${retired}`);
}
for (const required of [
  "supabase.rpc('observance_claim_identity_handoff'",
  'if (!isCopperlineCallback(callback))',
  'hashIdentityLinkCode(proofCode)',
  'p_code_hash: codeHash',
  'identity handoff RPC returned no valid claim state',
  'row.linked_discord_id !== discordId',
]) if (!repository.includes(required)) fail(`repository guard is missing ${required}`);

const retirement = read('discord/supabase/migrations/0014_atomic_identity_link.sql');
if (!retirement.includes('drop function if exists public.observance_claim_identity_handoff(text,text,text)')) {
  fail('0014 does not retire the proofless three-argument RPC');
}
for (const forbidden of ['create or replace function public.observance_claim_identity_handoff', 'grant execute']) {
  if (retirement.toLowerCase().includes(forbidden)) fail(`0014 republishes proofless identity authority: ${forbidden}`);
}

const migration = read('discord/supabase/migrations/0015_identity_proof_of_control.sql');
for (const required of [
  'create table if not exists public.identity_link_challenges',
  'alter table public.identity_link_challenges enable row level security',
  'observance_issue_identity_link_challenge',
  'security definer',
  'pg_advisory_xact_lock',
  "regexp_replace(coalesce(p_callback, ''), '[^0-9]', '', 'g') <> '9137'",
  "n.node_key = 'LS05' and n.active and n.required",
  "return query select 'blocked'::text",
  'v_target.discord_id is not null and v_target.discord_id <> p_discord_id',
  "return query select 'conflict'::text",
  "return query select 'challenge'::text",
  'v_challenge.code_hash <> p_code_hash',
  'set consumed_at = v_now',
  "interval '5 minutes'",
  "interval '30 seconds'",
  'set discord_id = null',
  "'discord:identity-link:LS05:' || p_discord_id || ':' || v_target.id::text",
  'observance_record_evidence(',
  "grant execute on function public.observance_claim_identity_handoff(text,text,text,text) to service_role",
]) if (!migration.toLowerCase().includes(required.toLowerCase())) fail(`migration is missing ${required}`);

for (const forbidden of ['p_code text', 'p_proof_code text', 'code_plaintext']) {
  if (migration.toLowerCase().includes(forbidden)) fail(`migration stores or accepts reusable plaintext: ${forbidden}`);
}

const callbackCheck = migration.indexOf("regexp_replace(coalesce(p_callback, '')");
const prerequisiteCheck = migration.indexOf('select n.prerequisite_flags');
const challengeLookup = migration.indexOf('from public.identity_link_challenges c');
const challengeConsume = migration.indexOf('set consumed_at = v_now');
const firstIdentityMutation = migration.indexOf('set discord_id = null');
const evidenceWrite = migration.lastIndexOf('select public.observance_record_evidence(');
if (!(callbackCheck >= 0 && callbackCheck < firstIdentityMutation)) fail('callback is not validated before identity mutation');
if (!(prerequisiteCheck >= 0 && prerequisiteCheck < firstIdentityMutation)) fail('LS06 Orientation prerequisite is not validated before identity mutation');
if (!(challengeLookup >= 0 && challengeLookup < firstIdentityMutation)) fail('Minecraft proof is not loaded before identity mutation');
if (!(challengeConsume >= 0 && challengeConsume < firstIdentityMutation)) fail('Minecraft proof is not consumed before identity mutation');
if (!(evidenceWrite > firstIdentityMutation)) fail('receipt is not in the same transaction after the claim/recovery');

const order = read('discord/src/db/build-apply-all.ts');
const retirementIndex = order.indexOf("'discord/supabase/migrations/0014_atomic_identity_link.sql'");
const migrationIndex = order.indexOf("'discord/supabase/migrations/0015_identity_proof_of_control.sql'");
const viewIndex = order.indexOf("'dashboard/supabase/migrations/0004_v_record.sql'");
if (!(retirementIndex >= 0 && retirementIndex < migrationIndex && migrationIndex < viewIndex)) {
  fail('0014 retirement / 0015 proof migration order is unsafe');
}

const voice = read('discord/src/voice.ts');
for (const required of ['nothing has been entered', 'nothing new was entered', 'mistaken entry is closed', '/obslink']) {
  if (!voice.includes(required)) fail(`transaction-safe player voice is missing ${required}`);
}

const pluginYml = read('plugin/src/main/resources/plugin.yml');
for (const required of ['observancelink:', 'aliases: [obslink]', 'permission: observance.link', 'default: true']) {
  if (!pluginYml.includes(required)) fail(`Paper proof command missing ${required}`);
}
const pluginCommand = read('plugin/src/main/java/com/observance/watcher/command/IdentityLinkCodeCommand.java');
for (const required of ['canIssueForAuthenticatedUuid(plugin.getServer().getOnlineMode())', 'No code was issued', 'IdentityLinkCode.generateDisplayCode()', 'issueIdentityLinkChallenge', 'runAsyncSafe', 'expires in 5 minutes']) {
  if (!pluginCommand.includes(required)) fail(`Paper proof command missing ${required}`);
}
const pluginAdmin = read('plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java');
for (const required of ['online-mode must be true', 'canIssueForAuthenticatedUuid']) {
  if (!pluginAdmin.includes(required)) fail(`production preflight does not reject offline-mode identity proof: ${required}`);
}

console.log('Identity link selftest OK: online-mode proof + atomic recovery + retired split mutators + private idempotent claims');
