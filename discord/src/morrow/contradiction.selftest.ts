import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  MORROW_BEHAVIOR_REUSE_EVENT,
  MORROW_CONTRADICTION_PURPOSE,
  MORROW_GROUP_RECEIPT_TEXT,
  MORROW_GROUP_RESOLUTION,
  MORROW_PRIVATE_CONTRADICTION_EVENT,
  aggregateLinkedDecisions,
  authoredPrivateEvidence,
  contradictionCustomId,
  evaluateGroupReceipt,
  nonceSha256,
  parseContradictionCustomId,
  resolveInteractionScope,
  validateBoundInteraction,
  type BoundContradictionSession,
  type GroupReceiptAttempt,
} from './contradiction.js';
import {
  runMorrowDiscordProjectionBatch,
  type MorrowDiscordProjectionClaim,
  type MorrowProjectionDependencies,
} from './projection-policy.js';
import {
  MORROW_GENERIC_DISCORD_EVENT_KEYS,
  renderMorrowDiscordProjection,
} from './projection-copy.js';

const CAMPAIGN = '8e0b1a62-d2dd-4e86-91f1-4b07af5e2922';
const PLAYER = '571d20b8-bf85-4e8a-92ac-3b071ffc882d';
const RELEASE = 'morrow.rehearsal.001';
const DISCORD = '123456789012345678';
const GUILD = '223456789012345678';
const CHANNEL = '323456789012345678';
const THREAD = '423456789012345678';
const NONCE = 'AbCdEfGhIjKlMnOpQrStUvWx';
const NOW = 1_800_000_000_000;

assert.equal(nonceSha256(NONCE).length, 64);
const ackId = contradictionCustomId(NONCE, 'ack');
assert.deepEqual(parseContradictionCustomId(ackId), { nonce: NONCE, action: 'ack' });
assert.equal(parseContradictionCustomId('morrow:contradiction:v1:bad:ack'), null);

const configured = { guildId: GUILD, channelId: CHANNEL, threadId: THREAD };
assert.deepEqual(resolveInteractionScope({
  guildId: GUILD, channelId: THREAD, parentChannelId: CHANNEL, isThread: true,
}, configured), { ok: true, channelId: CHANNEL, threadId: THREAD });
for (const attempted of [
  { guildId: '999999999999999999', channelId: THREAD, parentChannelId: CHANNEL, isThread: true },
  { guildId: GUILD, channelId: '999999999999999999', parentChannelId: CHANNEL, isThread: true },
  { guildId: GUILD, channelId: THREAD, parentChannelId: '999999999999999999', isThread: true },
  { guildId: GUILD, channelId: CHANNEL, parentChannelId: null, isThread: false },
]) assert.deepEqual(resolveInteractionScope(attempted, configured), { ok: false });

const rootConfigured = { guildId: GUILD, channelId: CHANNEL, threadId: null };
assert.equal(resolveInteractionScope({
  guildId: GUILD, channelId: CHANNEL, parentChannelId: null, isThread: false,
}, rootConfigured).ok, true);
assert.equal(resolveInteractionScope({
  guildId: GUILD, channelId: THREAD, parentChannelId: CHANNEL, isThread: true,
}, rootConfigured).ok, false, 'a configured root channel cannot be widened to arbitrary threads');

const session: BoundContradictionSession = {
  discordUserId: DISCORD, campaignId: CAMPAIGN, playerId: PLAYER, releaseId: RELEASE,
  guildId: GUILD, channelId: CHANNEL, threadId: THREAD, purpose: MORROW_CONTRADICTION_PURPOSE,
  expiresAtMs: NOW + 60_000, state: 'offered',
};
const attempt = {
  discordUserId: DISCORD, campaignId: CAMPAIGN, playerId: PLAYER, releaseId: RELEASE,
  guildId: GUILD, channelId: CHANNEL, threadId: THREAD, purpose: MORROW_CONTRADICTION_PURPOSE,
};
assert.equal(validateBoundInteraction(null, attempt, NOW), 'blocked', 'unlinked/no-session user fails closed');
assert.equal(validateBoundInteraction(session, attempt, NOW), 'ready');
for (const changed of [
  { campaignId: '63f23140-2c30-44ea-9c94-a45b7252f94e' },
  { playerId: 'ff41c590-6caf-46c1-9495-c631e475daf8' },
  { discordUserId: '999999999999999999' },
  { releaseId: 'morrow.rehearsal.002' },
  { guildId: '999999999999999999' },
  { channelId: '999999999999999999' },
  { threadId: '999999999999999999' },
]) assert.equal(validateBoundInteraction(session, { ...attempt, ...changed }, NOW), 'blocked');
assert.equal(validateBoundInteraction({ ...session, expiresAtMs: NOW }, attempt, NOW), 'expired', 'stale nonce expires');
assert.equal(validateBoundInteraction({ ...session, state: 'cancelled' }, attempt, NOW), 'terminal', 'cancel is terminal');
assert.equal(validateBoundInteraction({ ...session, state: 'declined' }, attempt, NOW), 'terminal', 'decline is terminal');

const firstPrivate = authoredPrivateEvidence('witness-04', 'route_digest');
const secondPrivate = authoredPrivateEvidence('witness-05', 'source_gap');
assert.ok(firstPrivate.includes('witness-04') && !firstPrivate.includes('witness-05'));
assert.ok(secondPrivate.includes('witness-05') && !secondPrivate.includes('witness-04'));
for (const privateFragment of ['exact sealed live-test route digest', 'No pre-incident movement digest', 'witness-04', 'witness-05']) {
  assert.equal(MORROW_GROUP_RECEIPT_TEXT.includes(privateFragment), false,
    `group receipt leaked private material: ${privateFragment}`);
}
assert.ok(MORROW_GROUP_RECEIPT_TEXT.includes('authorizes no capture'));

for (const size of [1, 2, 6]) {
  const players = Array.from({ length: size }, (_, index) =>
    `${String(index + 1).padStart(8, '0')}-1111-4111-8111-${String(index + 1).padStart(12, '0')}`);
  const partial = new Map(players.slice(0, -1).map((player) => [player, MORROW_GROUP_RESOLUTION] as const));
  assert.deepEqual(aggregateLinkedDecisions({ linkedPlayerIds: players, decisions: partial }), {
    status: 'pending', accepted: Math.max(0, size - 1), required: size,
  });
  const complete = new Map(players.map((player) => [player, MORROW_GROUP_RESOLUTION] as const));
  assert.deepEqual(aggregateLinkedDecisions({ linkedPlayerIds: players, decisions: complete }), {
    status: 'ready', accepted: size, required: size,
  });
}

const receipt: GroupReceiptAttempt = {
  campaignId: CAMPAIGN,
  releaseId: RELEASE,
  sourceEventId: '4c20a9c1-371d-4b19-b2f3-9e67e37bcaa1',
  prerequisiteEvent: MORROW_BEHAVIOR_REUSE_EVENT,
  eventKey: MORROW_PRIVATE_CONTRADICTION_EVENT,
  idempotencyKey: 'discord:morrow:act2:private-contradiction:v1',
  payloadHash: 'a'.repeat(64),
};
assert.equal(evaluateGroupReceipt(receipt), 'committed');
assert.equal(evaluateGroupReceipt({ ...receipt, prerequisiteEvent: 'morrow.act2.live_test_recorded' }), 'blocked',
  'locked prerequisite cannot create a receipt');
const existing = {
  campaignId: receipt.campaignId, releaseId: receipt.releaseId, sourceEventId: receipt.sourceEventId,
  eventKey: receipt.eventKey, idempotencyKey: receipt.idempotencyKey, payloadHash: receipt.payloadHash,
};
assert.equal(evaluateGroupReceipt({ ...receipt, existing }), 'duplicate');
assert.equal(evaluateGroupReceipt({ ...receipt, existing: { ...existing, payloadHash: 'b'.repeat(64) } }), 'collision');

// Two workers racing the same durable key: one commit, one exact duplicate, never two events.
let racedExisting: typeof existing | undefined;
async function raceCommit(): Promise<string> {
  await Promise.resolve();
  const result = evaluateGroupReceipt({ ...receipt, existing: racedExisting });
  if (result === 'committed') racedExisting = existing;
  return result;
}
assert.deepEqual((await Promise.all([raceCommit(), raceCommit()])).sort(), ['committed', 'duplicate']);

const activationClaim: MorrowDiscordProjectionClaim = {
  eventId: '4c20a9c1-371d-4b19-b2f3-9e67e37bcaa1', eventKey: MORROW_BEHAVIOR_REUSE_EVENT,
  campaignId: CAMPAIGN, releaseId: RELEASE, payloadSha256: 'c'.repeat(64),
  channelId: null, threadId: null, attempts: 1,
};
const receiptClaim: MorrowDiscordProjectionClaim = {
  ...activationClaim, eventId: '50d38fe3-56e6-47dd-bf70-34a8cd034d7d',
  eventKey: MORROW_PRIVATE_CONTRADICTION_EVENT, channelId: CHANNEL, threadId: THREAD,
};
const completions: Array<{ applied: boolean; error?: string }> = [];
let outage = true;
let claims = [activationClaim, receiptClaim];
const dependencies: MorrowProjectionDependencies = {
  claim: async () => { const current = claims; claims = []; return current; },
  activate: async () => 'activated',
  post: async () => !outage,
  complete: async (_claim, _worker, applied, error) => { completions.push({ applied, error }); return true; },
};
assert.deepEqual(await runMorrowDiscordProjectionBatch('11111111-1111-4111-8111-111111111111', dependencies), {
  claimed: 2, applied: 1, failed: 1,
}, 'outage leaves the group receipt retryable while activation applies');
assert.deepEqual(completions.map((row) => row.applied), [true, false]);
outage = false;
claims = [{ ...receiptClaim, attempts: 2 }];
assert.deepEqual(await runMorrowDiscordProjectionBatch('22222222-2222-4222-8222-222222222222', dependencies), {
  claimed: 1, applied: 1, failed: 0,
}, 'restart safely reclaims and delivers the failed receipt');

const genericClaim: MorrowDiscordProjectionClaim = {
  ...activationClaim,
  eventId: '60d38fe3-56e6-47dd-bf70-34a8cd034d7d',
  eventKey: 'morrow.act7.coda_started',
  channelId: CHANNEL,
  threadId: THREAD,
};
claims = [genericClaim];
assert.deepEqual(await runMorrowDiscordProjectionBatch(
  '33333333-3333-4333-8333-333333333333', dependencies,
), { claimed: 1, applied: 1, failed: 0 }, 'later-act synchronized receipts must not retry as unsupported');
const codaCopy = renderMorrowDiscordProjection(genericClaim.eventKey, RELEASE, genericClaim.eventId);
assert.ok(codaCopy?.includes('Persistent coda opened') && codaCopy.includes(genericClaim.eventId));
assert.equal(codaCopy?.includes(genericClaim.payloadSha256), false, 'Discord copy must never include raw event payload');
assert.equal(renderMorrowDiscordProjection('morrow.unknown.event', RELEASE, genericClaim.eventId), null);

const sql = readFileSync(resolve('../morrow/db/schema-proposal.sql'), 'utf8');
const handler = readFileSync(resolve('src/bot/commands/morrow.ts'), 'utf8');
const index = readFileSync(resolve('src/bot/index.ts'), 'utf8');
const register = readFileSync(resolve('src/bot/register.ts'), 'utf8');
const packageJson = readFileSync(resolve('package.json'), 'utf8');
const eventCatalog = JSON.parse(readFileSync(resolve('../morrow/contracts/event-catalog.json'), 'utf8')) as {
  events: Array<{ key: string; projects_to: string[] }>;
};
const discordEvents = eventCatalog.events
  .filter((event) => event.projects_to.includes('discord'))
  .map((event) => event.key);
assert.deepEqual([
  ...MORROW_GENERIC_DISCORD_EVENT_KEYS,
  MORROW_BEHAVIOR_REUSE_EVENT,
  MORROW_PRIVATE_CONTRADICTION_EVENT,
].sort(), [...discordEvents].sort(), 'every canonical Discord projection needs an authored delivery policy');
for (const eventKey of MORROW_GENERIC_DISCORD_EVENT_KEYS) {
  const rendered = renderMorrowDiscordProjection(eventKey, RELEASE, genericClaim.eventId);
  assert.ok(rendered && rendered.length <= 700, `${eventKey} lacks bounded authored copy`);
  assert.equal(rendered.includes('undefined'), false, `${eventKey} rendered incomplete copy`);
}
for (const required of [
  'discord_contradiction_flows', 'discord_contradiction_sessions', 'discord_contradiction_votes',
  'pg_advisory_xact_lock', 'for update of session', 'skip locked', 'lease_expires_at',
  'morrow.act2.private_contradiction_resolved', "'private_payload', false",
  'from public, anon, authenticated', 'to service_role',
]) assert.ok(sql.toLowerCase().includes(required.toLowerCase()), `Discord SQL authority lacks ${required}`);
for (const required of ['MessageFlags.Ephemeral', 'ButtonBuilder', 'StringSelectMenuBuilder',
  'resolveInteractionScope', 'newContradictionNonce', 'No prose is graded']) {
  assert.ok(handler.toLowerCase().includes(required.toLowerCase()), `native handler lacks ${required}`);
}
assert.ok(index.includes('interaction.isButton()') && index.includes('interaction.isStringSelectMenu()'));
assert.ok(register.includes(".setName('morrow')"));
assert.equal(packageJson.includes('"chat"'), false, 'Chat SDK must not be added to the native discord.js worker');
assert.equal(packageJson.includes('@chat-adapter/'), false, 'no cross-platform adapter is needed');
assert.equal(handler.includes('ModalBuilder'), false, 'this exact classification needs no freeform modal');
assert.equal(handler.includes('fetch('), false, 'Gateway interactions require no HTTP interaction endpoint');
assert.equal(handler.toLowerCase().includes('llm'), false, 'an LLM cannot decide progression');

const rebootFiles = [
  readFileSync(resolve('src/morrow/contradiction.ts'), 'utf8'),
  readFileSync(resolve('src/morrow/repo.ts'), 'utf8'),
  readFileSync(resolve('src/morrow/projection-policy.ts'), 'utf8'),
  readFileSync(resolve('src/morrow/projection-worker.ts'), 'utf8'),
  readFileSync(resolve('src/morrow/projection-copy.ts'), 'utf8'),
  handler,
].join('\n');
const retired = new RegExp(`\\b(?:${['Ho' + 'ld', 'Keep' + 'er', 'Aver' + 'yn', 'Wr' + 'en', 'Nol' + 'and', 'Deep ' + 'Hold', 'Un' + 'lit'].join('|')})\\b`, 'i');
assert.equal(retired.test(rebootFiles), false, 'superseded lore leaked into the reboot Discord surface');

console.log('MORROW DISCORD: PASS identity/scope/nonce/private/1-2-6/race/collision/cancel/timeout/outage/restart');
