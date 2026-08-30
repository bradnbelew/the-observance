import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  MORROW_CASE_ATTACHMENT_SHA256,
  MORROW_CASE_ATTACHMENT_TEXT,
  MORROW_CASE_EVENT_ORDER,
  evaluateCopperlineReceipt,
  projectMorrowCase,
  validateMorrowCaseAction,
  type MorrowProjectionRow,
} from './morrow-copperline-case';

const CAMPAIGN = '8e0b1a62-d2dd-4e86-91f1-4b07af5e2922';
const PLAYER = '571d20b8-bf85-4e8a-92ac-3b071ffc882d';
const RELEASE = 'morrow.rehearsal.001';

const row = (projection_key: string, projection: Record<string, unknown>, revision = 1,
             campaign_id = CAMPAIGN, player_id = PLAYER): MorrowProjectionRow => ({
  campaign_id, player_id, projection_key, projection, revision, updated_at: '2026-08-30T12:00:00Z',
});
const access = row('case_access', {
  caseId: 'CL-RCV-04', releaseId: RELEASE, displayAlias: 'witness-04', groupSize: 1,
});

assert.equal(Buffer.byteLength(MORROW_CASE_ATTACHMENT_TEXT, 'utf8'), 285,
  'accessible attachment metadata must report the exact UTF-8 byte length');
assert.equal(createHash('sha256').update(MORROW_CASE_ATTACHMENT_TEXT, 'utf8').digest('hex'),
  MORROW_CASE_ATTACHMENT_SHA256, 'displayed attachment and custody checksum must remain identical');

assert.deepEqual(projectMorrowCase([], RELEASE), { kind: 'empty', reason: 'case_not_assigned' },
  'direct URL without an RLS-owned assignment reveals nothing');
assert.equal(projectMorrowCase([access], 'morrow.rehearsal.002').kind, 'error',
  'stale deployment release fails closed');
assert.equal(projectMorrowCase([access, row('case_progress', { events: [] }, 2,
  '63f23140-2c30-44ea-9c94-a45b7252f94e')], RELEASE).kind, 'error',
  'mixed campaign projection fails closed');
assert.equal(projectMorrowCase([access, row('case_progress', { events: [] }, 2, CAMPAIGN,
  'ff41c590-6caf-46c1-9495-c631e475daf8')], RELEASE).kind, 'error',
  'mixed player projection fails closed');

for (const groupSize of [1, 2, 6]) {
  const result = projectMorrowCase([row('case_access', {
    caseId: 'CL-RCV-04', releaseId: RELEASE, displayAlias: `group-${groupSize}`, groupSize,
  })], RELEASE);
  assert.equal(result.kind, 'ready');
  if (result.kind === 'ready') assert.equal(result.groupSize, groupSize);
}

const act2 = projectMorrowCase([
  access,
  row('case_progress', { events: [...MORROW_CASE_EVENT_ORDER, 'morrow.act3.contradiction_preserved'] }, 9),
  row('player_receipts', { receipts: ['web:checksum:001', 'paper:live-test:001'] }, 9),
  row('server_handoff', {
    releaseId: RELEASE, serverLabel: 'Mossfield Recovery', joinAddress: 'rehearsal.example.test:25565',
    recoveryReceipt: 'evt-act0-handoff',
  }, 9),
  row('handoff_token', { token: 'A7C2-9QMX' }, 2),
], RELEASE);
assert.equal(act2.kind, 'ready');
if (act2.kind === 'ready') {
  assert.equal(act2.updates.length, 6, 'only the six earned Act 1/2 ticket callbacks render');
  assert.deepEqual(act2.updates.map((update) => update.eventKey), [
    'morrow.act1.room04_witnessed',
    'morrow.act1.static_proposal_authenticated',
    'morrow.act1.intention_error_proven',
    'morrow.act2.missing_role_completed',
    'morrow.act2.live_test_recorded',
    'morrow.act2.behavior_reuse_proven',
  ]);
  assert.equal(JSON.stringify(act2).includes('morrow.act3.contradiction_preserved'), false,
    'locked future events and their spoiler keys are omitted from the rendered model');
  assert.equal(act2.groupSize, 1);
  assert.deepEqual(act2.playerReceipts, ['web:checksum:001', 'paper:live-test:001']);
  assert.equal(act2.handoffToken, 'A7C2-9QMX');
}

const lockedToken = projectMorrowCase([access, row('handoff_token', { token: 'A7C2-9QMX' }, 1)], RELEASE);
assert.equal(lockedToken.kind, 'ready');
if (lockedToken.kind === 'ready') {
  assert.equal(lockedToken.handoffToken, null, 'token remains spoiler-filtered before custody authentication');
  assert.equal(JSON.stringify(lockedToken).includes('A7C2-9QMX'), false, 'locked token value must not leak');
}

const outOfOrder = projectMorrowCase([
  access,
  row('case_progress', { events: [
    'morrow.act0.case_chain_authenticated',
    'morrow.act0.server_handoff_recovered',
    'morrow.act1.static_proposal_authenticated',
  ] }, 3),
], RELEASE);
assert.deepEqual(outOfOrder, { kind: 'error', reason: 'projection_out_of_order' });

const checksum = new FormData();
checksum.set('operation', 'verify_case_chain'); checksum.set('checksum', MORROW_CASE_ATTACHMENT_SHA256.toUpperCase());
assert.deepEqual(validateMorrowCaseAction(checksum), {
  ok: true, operation: 'verify_case_chain', normalizedValue: MORROW_CASE_ATTACHMENT_SHA256,
});
const badChecksum = new FormData();
badChecksum.set('operation', 'verify_case_chain'); badChecksum.set('checksum', '0'.repeat(64));
assert.deepEqual(validateMorrowCaseAction(badChecksum), { ok: false, reason: 'invalid_checksum' });
const token = new FormData(); token.set('operation', 'recover_handoff'); token.set('handoffToken', 'a7c2-9qmx');
assert.deepEqual(validateMorrowCaseAction(token), {
  ok: true, operation: 'recover_handoff', normalizedValue: 'A7C2-9QMX',
});
const badToken = new FormData(); badToken.set('operation', 'recover_handoff'); badToken.set('handoffToken', 'too-long-token');
assert.deepEqual(validateMorrowCaseAction(badToken), { ok: false, reason: 'invalid_token' });

const baseAttempt = {
  campaignId: CAMPAIGN, playerId: PLAYER, releaseId: RELEASE,
  linkedCampaignId: CAMPAIGN, linkedPlayerId: PLAYER, activeReleaseId: RELEASE,
  eventKey: 'morrow.act0.case_chain_authenticated' as const,
  idempotencyKey: 'copperline:morrow:act0:case-chain:v1', payloadHash: 'a'.repeat(64), committedEvents: [],
};
assert.equal(evaluateCopperlineReceipt(baseAttempt), 'committed');
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, linkedPlayerId: 'ff41c590-6caf-46c1-9495-c631e475daf8' }), 'blocked');
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, activeReleaseId: 'morrow.rehearsal.002' }), 'blocked');
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, eventKey: 'morrow.act0.server_handoff_recovered' }), 'blocked');
const existing = { ...baseAttempt };
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, existing }), 'duplicate');
for (const linkedPlayerId of [
  'ff41c590-6caf-46c1-9495-c631e475daf8',
  '483a42f3-2096-42a7-a307-94e22b32617d',
  '9234cb60-aa31-43d0-b81d-8e9653e0538c',
  '203c1020-d01e-43ed-bc4c-a80001d943a3',
  '7110be21-e253-4f8e-ac4c-58e80d76ea47',
]) {
  assert.equal(evaluateCopperlineReceipt({
    ...baseAttempt,
    playerId: linkedPlayerId,
    linkedPlayerId,
    existing,
  }), 'duplicate', 'a separately authenticated group member reuses the group receipt without collision');
}
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, existing: { ...existing, payloadHash: 'b'.repeat(64) } }), 'collision');

const routeSource = readFileSync(resolve('src/app/support/cases/mossfield-recovery/page.tsx'), 'utf8');
const actionSource = readFileSync(resolve('src/app/support/cases/mossfield-recovery/actions.ts'), 'utf8');
const serverSource = readFileSync(resolve('src/lib/morrow-copperline-server.ts'), 'utf8');
const loginActionSource = readFileSync(resolve('src/app/support/account/actions.ts'), 'utf8');
const loginPageSource = readFileSync(resolve('src/app/support/account/page.tsx'), 'utf8');
const loginFormSource = readFileSync(resolve('src/app/support/account/PlayerLoginForm.tsx'), 'utf8');
const authCallbackSource = readFileSync(resolve('src/app/auth/callback/route.ts'), 'utf8');
const rehearsalSessionSource = readFileSync(resolve('src/app/api/rehearsal/morrow-session/route.ts'), 'utf8');
const projectorSource = readFileSync(resolve('scripts/morrow-copperline-projector.mjs'), 'utf8');
const sql = readFileSync(resolve('../morrow/db/schema-proposal.sql'), 'utf8');
for (const required of ["'use server'", 'sameOrigin()', 'readMorrowCase()', 'externalMutationsAllowed()',
  '!context.handoffToken', 'morrow_record_copperline_event']) assert.ok(actionSource.includes(required), `action lacks ${required}`);
for (const required of ["import 'server-only'", 'auth.getUser()', "from('morrow_player_projection')"]) {
  assert.ok(serverSource.includes(required), `server reader lacks ${required}`);
}
for (const forbidden of ['SUPABASE_SERVICE_ROLE_KEY', 'createAdminClient', 'searchParams', 'campaignId:', 'playerId:']) {
  assert.equal(serverSource.includes(forbidden), false, `reader must not trust URL/privileged identity: ${forbidden}`);
}
for (const required of ['morrow_record_copperline_event', "owner_surface = 'copperline'", 'is_linked_player',
  'handoff_token_sha256', MORROW_CASE_ATTACHMENT_SHA256, '831b4026a5e98b263699ba337d246ad5c2c1f26b3f1d00bdef42775586707096',
  '69ef307074c7aaf7cc4fff12c5e8b7b2423c51a05dfc7f8bb11d53b059420a6e',
  'duplicate', 'collision']) assert.ok(sql.includes(required), `SQL authority lacks ${required}`);
for (const required of ['morrow_claim_copperline_projections', 'morrow_apply_copperline_projection',
  'morrow_fail_copperline_projection', "projection.surface = 'copperline'", 'skip locked',
  "grant select, insert, update, delete on public.morrow_player_projection to service_role"]) {
  assert.ok(sql.includes(required), `Copperline projector SQL lacks ${required}`);
}
const copperlineRpc = sql.split('create or replace function public.morrow_record_copperline_event', 2)[1]
  .split('create table if not exists morrow_private.capability_grants', 2)[0];
const duplicateBranch = copperlineRpc.split('if v_existing.event_key', 2)[1]
  .split("return query select 'duplicate'", 2)[0];
assert.equal(duplicateBranch.includes('actor_player_id'), false,
  'group idempotency must not collide when a second linked player submits the exact earned action');
for (const required of ['Accessible attachment metadata', 'Temporary maintenance state', 'No linked case found',
  'Case audit halted', 'Synchronized field updates', 'player-specific', 'Sign in to this recovery case']) {
  assert.ok(routeSource.includes(required), `route lacks concrete state/copy: ${required}`);
}
for (const required of ['signInWithOtp', 'shouldCreateUser: false', 'MORROW_CASE_ROUTE', 'originUrl.host !== host',
  'If that address owns a current assignment']) {
  assert.ok(loginActionSource.includes(required), `player login action lacks ${required}`);
}
assert.equal(loginActionSource.includes('SUPABASE_SERVICE_ROLE_KEY'), false,
  'player login must use only the public auth client');
assert.ok(loginPageSource.includes('auth.getUser()') && loginPageSource.includes('redirect(MORROW_CASE_ROUTE)'),
  'signed-in player login page must return directly to the assigned case');
assert.ok(loginFormSource.includes('type="email"') && loginFormSource.includes('autoComplete="email"'),
  'player login form must expose an accessible email field');
assert.ok(authCallbackSource.includes("next.startsWith('/support/')")
  && authCallbackSource.includes('/support/account?error=link'),
  'failed player callbacks must return to the support login instead of the operator console');
for (const required of ["const VALIDATION_PROJECT = 'snmqaqlagwzptzqbaiws'",
  "MORROW_BROWSER_REHEARSAL_AUTH === 'enabled'", 'targetRef !== VALIDATION_PROJECT',
  'signInWithPassword', 'new NextResponse(\'Not found\', { status: 404 })']) {
  assert.ok(rehearsalSessionSource.includes(required), `browser rehearsal bootstrap lacks ${required}`);
}
assert.equal(rehearsalSessionSource.includes('SUPABASE_SERVICE_ROLE_KEY'), false,
  'browser rehearsal bootstrap must authenticate through the public client');
for (const required of ['MORROW_COPPERLINE_PROJECTOR', 'OBSERVANCE_MUTATION_PROJECT_REF',
  'MORROW_PROJECTOR_PRODUCTION_ACK',
  'morrow_claim_copperline_projections', 'morrow_apply_copperline_projection',
  'morrow_fail_copperline_projection', 'persistSession: false', "targetUrl?.protocol !== 'https:'",
  'targetUrl.hostname !== `${targetRef}.supabase.co`', '!Number.isInteger(maxBatches)']) {
  assert.ok(projectorSource.includes(required), `Copperline worker lacks ${required}`);
}
assert.ok(projectorSource.includes('fndmhbpxnodrnbrzrlqq')
  && projectorSource.includes('fdnmhbpxnodrnbrzrlqq'),
  'Copperline worker must reject both known production project refs during rehearsal');

const newSurface = [routeSource, actionSource, serverSource,
  readFileSync(resolve('src/app/support/cases/mossfield-recovery/CaseActions.tsx'), 'utf8'),
  loginActionSource, loginPageSource, loginFormSource].join('\n');
const staleLore = new RegExp('\\b(?:Hold|Keeper|Averyn|Wren|Noland|Deep Hold|Unlit)\\b', 'i');
assert.equal(staleLore.test(newSurface), false, 'new Copperline reboot surface leaked superseded lore');
assert.equal(newSurface.includes('SUPABASE_SERVICE_ROLE_KEY'), false, 'service role leaked into reboot route');

console.log('MORROW COPPERLINE ACT 0-2: PASS direct/stale/identity/1-2-6/duplicate/collision/spoiler/outage/checksum/token/input/order');
