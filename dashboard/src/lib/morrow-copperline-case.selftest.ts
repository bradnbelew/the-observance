import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  MORROW_AUDIT_CHRONOLOGY_PAYLOAD,
  MORROW_AUDIT_CHRONOLOGY_SHA256,
  MORROW_AUDIT_RECORDS,
  MORROW_CASE_ATTACHMENT_SHA256,
  MORROW_CASE_ATTACHMENT_TEXT,
  MORROW_CASE_EVENT_ORDER,
  evaluateCopperlineReceipt,
  projectMorrowCase,
  validateMorrowCaseAction,
  type MorrowProjectionRow,
} from './morrow-copperline-case';
import { MORROW_MEDIA_CATALOG } from './morrow-media-catalog';

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
  row('case_progress', {
    events: [...MORROW_CASE_EVENT_ORDER.slice(0, 9), 'morrow.act8.future_state_withheld'],
  }, 9),
  row('player_receipts', { receipts: ['web:checksum:001', 'paper:live-test:001'] }, 9),
  row('server_handoff', {
    releaseId: RELEASE, serverLabel: 'Mossfield Recovery', joinAddress: 'rehearsal.example.test:25565',
    recoveryReceipt: 'evt-act0-handoff',
  }, 9),
  row('handoff_token', { token: 'A7C2-9QMX' }, 2),
], RELEASE);
assert.equal(act2.kind, 'ready');
if (act2.kind === 'ready') {
  assert.equal(act2.updates.length, 7, 'only the seven earned Act 1/2 ticket callbacks render');
  assert.deepEqual(act2.updates.map((update) => update.eventKey), [
    'morrow.act1.room04_witnessed',
    'morrow.act1.static_proposal_authenticated',
    'morrow.act1.intention_error_proven',
    'morrow.act1.entity_replay_authorized',
    'morrow.act2.missing_role_completed',
    'morrow.act2.live_test_recorded',
    'morrow.act2.behavior_reuse_proven',
  ]);
  assert.equal(JSON.stringify(act2).includes('morrow.act8.future_state_withheld'), false,
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

assert.equal(payloadDigest(MORROW_AUDIT_CHRONOLOGY_PAYLOAD), MORROW_AUDIT_CHRONOLOGY_SHA256);
assert.equal(MORROW_AUDIT_CHRONOLOGY_SHA256, 'cbf40a46335441d5d164d4e8f8f6afd02a97503ade2163ad3fd86e9063fc51e5');
const audit = new FormData(); audit.set('operation', 'prove_audit_chronology');
MORROW_AUDIT_RECORDS.forEach((record, index) => audit.set(`edge${index + 1}`, record.id));
assert.deepEqual(validateMorrowCaseAction(audit), {
  ok: true,
  operation: 'prove_audit_chronology',
  normalizedValue: MORROW_AUDIT_RECORDS.map((record) => record.id).join('>'),
});
const brokenAudit = new FormData(); brokenAudit.set('operation', 'prove_audit_chronology');
MORROW_AUDIT_RECORDS.forEach((record, index) => brokenAudit.set(`edge${index + 1}`, record.id));
brokenAudit.set('edge1', 'iona_shutdown_order');
assert.deepEqual(validateMorrowCaseAction(brokenAudit), {
  ok: false,
  reason: 'invalid_chronology',
  brokenEdge: 0,
  expectedTitle: 'Theo Vale live-capture permission',
});

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
assert.equal(evaluateCopperlineReceipt({ ...baseAttempt, eventKey: 'morrow.act6.audit_chronology_proven' }), 'blocked');
assert.equal(evaluateCopperlineReceipt({
  ...baseAttempt,
  eventKey: 'morrow.act6.audit_chronology_proven',
  committedEvents: ['morrow.act5.dual_session_consciousness_proven'],
}), 'committed');
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
const caseActionsSource = readFileSync(resolve('src/app/support/cases/mossfield-recovery/CaseActions.tsx'), 'utf8');
const serverSource = readFileSync(resolve('src/lib/morrow-copperline-server.ts'), 'utf8');
const loginActionSource = readFileSync(resolve('src/app/auth/player-link/route.ts'), 'utf8');
const loginPageSource = readFileSync(resolve('src/app/support/account/page.tsx'), 'utf8');
const loginFormSource = readFileSync(resolve('src/app/support/account/PlayerLoginForm.tsx'), 'utf8');
const authCallbackSource = readFileSync(resolve('src/app/auth/callback/route.ts'), 'utf8');
const rehearsalSessionSource = readFileSync(resolve('src/app/api/rehearsal/morrow-session/route.ts'), 'utf8');
const projectorSource = readFileSync(resolve('scripts/morrow-copperline-projector.mjs'), 'utf8');
const mediaRouteSource = readFileSync(
  resolve('src/app/support/cases/mossfield-recovery/media/[mediaKey]/route.ts'), 'utf8',
);
const sql = readFileSync(resolve('../morrow/db/schema-proposal.sql'), 'utf8');
const eventCatalog = JSON.parse(readFileSync(resolve('../morrow/contracts/event-catalog.json'), 'utf8')) as {
  events: Array<{ key: string; projects_to: string[] }>;
};
assert.deepEqual(MORROW_CASE_EVENT_ORDER,
  eventCatalog.events.filter((event) => event.projects_to.includes('copperline')).map((event) => event.key),
  'Copperline order must exactly mirror every canonical event projected to the website');
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
  '69ef307074c7aaf7cc4fff12c5e8b7b2423c51a05dfc7f8bb11d53b059420a6e', MORROW_AUDIT_CHRONOLOGY_SHA256,
  'morrow.act6.audit_chronology_proven', 'captioned_current_voice_assembly', "'continuity_claim', null",
  'duplicate', 'collision']) assert.ok(sql.includes(required), `SQL authority lacks ${required}`);
for (const required of ['morrow_claim_copperline_projections', 'morrow_apply_copperline_projection',
  'morrow_fail_copperline_projection', "projection.surface = 'copperline'", 'skip locked',
  "grant select, insert, update, delete on public.morrow_player_projection to service_role"]) {
  assert.ok(sql.includes(required), `Copperline projector SQL lacks ${required}`);
}

const finale = projectMorrowCase([
  access,
  row('case_progress', {
    events: [...MORROW_CASE_EVENT_ORDER, 'morrow.act8.future_state_withheld'],
    rawPayload: { ending: 'CREATE_NEW_BRANCH', privateDialogue: 'must not render' },
  }, 25),
  row('case_media', { keys: [
    'morrow.m03.captioned_voice_fragment',
    'morrow.m04.route_derivative',
    'morrow.m08.almost_home_comparison',
    'morrow.m11.current_voice_assembly',
    'morrow.m12.coda.create_new_branch',
  ], rawEnding: 'must not render' }, 25),
], RELEASE);
assert.equal(finale.kind, 'ready');
if (finale.kind === 'ready') {
  assert.equal(finale.events.length, 25, 'Copperline receives every canonical website projection');
  assert.equal(finale.updates.length, 23, 'Act 0 controls stay separate while all earned field updates render');
  assert.deepEqual(finale.media.map((asset) => asset.key), [
    'morrow.m03.captioned_voice_fragment',
    'morrow.m04.route_derivative',
    'morrow.m08.almost_home_comparison',
    'morrow.m11.current_voice_assembly',
    'morrow.m12.coda.create_new_branch',
  ], 'earned media follows canonical progression and exposes only the selected ending');
  assert.deepEqual(finale.updates.slice(-7).map((update) => update.eventKey), [
    'morrow.act6.audit_chronology_proven',
    'morrow.act6.current_morrow_reconstruction_proven',
    'morrow.act6.cold_storage_access_authorized',
    'morrow.act7.rollback_anchors_committed',
    'morrow.act7.branch_policy_committed',
    'morrow.act7.branch_governance_authorized',
    'morrow.act7.coda_started',
  ]);
  const rendered = JSON.stringify(finale);
  assert.equal(rendered.includes('morrow.act8.future_state_withheld'), false,
    'unknown future event keys remain omitted from the rendered model');
  assert.equal(rendered.includes('CREATE_NEW_BRANCH'), false,
    'raw ending payload remains outside the website projection model');
  assert.equal(rendered.includes('privateDialogue'), false,
    'raw private dialogue remains outside the website projection model');
  assert.equal(rendered.includes('private_contradiction_resolved'), false,
    'Discord-private contradiction never enters the Copperline event order');
  assert.equal(rendered.includes('rawEnding'), false,
    'raw media projection fields remain outside the rendered model');
}

assert.deepEqual(projectMorrowCase([
  access,
  row('case_progress', { events: MORROW_CASE_EVENT_ORDER.slice(0, 2) }, 2),
  row('case_media', { keys: ['morrow.m03.captioned_voice_fragment'] }, 2),
], RELEASE), { kind: 'error', reason: 'projection_invalid' },
'media fails closed before its prerequisite event is earned');
assert.deepEqual(projectMorrowCase([
  access,
  row('case_progress', { events: MORROW_CASE_EVENT_ORDER }, 25),
  row('case_media', { keys: ['morrow.unknown.asset'] }, 25),
], RELEASE), { kind: 'error', reason: 'projection_invalid' },
'unknown media keys fail closed');
assert.deepEqual(projectMorrowCase([
  access,
  row('case_progress', { events: MORROW_CASE_EVENT_ORDER }, 25),
  row('case_media', { keys: ['morrow.m12.coda.certify', 'morrow.m12.coda.close_ticket'] }, 25),
], RELEASE), { kind: 'error', reason: 'projection_invalid' },
'multiple ending records fail closed');

const lockedFinale = projectMorrowCase([
  access,
  row('case_progress', { events: MORROW_CASE_EVENT_ORDER.slice(0, -1) }, 24),
], RELEASE);
assert.equal(lockedFinale.kind, 'ready');
if (lockedFinale.kind === 'ready') {
  assert.equal(lockedFinale.updates.some((update) => update.eventKey === 'morrow.act7.coda_started'), false,
    'the coda update stays absent until its own receipt is earned');
}
for (const required of ['projector_run_receipts', 'run_copperline_projector',
  'copperline_projector_health', 'configure_copperline_projector_schedule',
  "'10 seconds'", "'enable:'", "'disable:'", "to_regprocedure('cron.schedule(text,text,text)')",
  'from public, anon, authenticated, service_role']) {
  assert.ok(sql.includes(required), `database-native Copperline scheduler lacks ${required}`);
}
for (const eventKey of MORROW_CASE_EVENT_ORDER) {
  assert.ok(sql.includes(`'${eventKey}'`), `database projector lacks canonical event ${eventKey}`);
}
assert.ok(sql.includes("'case_media'") && sql.includes('morrow.reboot.media.v1'),
  'database projector must emit earned media and seed its release authority');
assert.equal(MORROW_MEDIA_CATALOG.length, 8, 'earned media catalog must retain all eight canonical assets');
for (const asset of MORROW_MEDIA_CATALOG) {
  assert.ok(asset.accessible_equivalent && asset.timing_labels.length > 0,
    `${asset.key} lacks a non-audio equivalent`);
  assert.ok((asset.transcript?.length ?? 0) > 0 || (asset.diagram_nodes?.length ?? 0) > 0,
    `${asset.key} lacks a transcript or ordered diagram`);
  assert.ok(sql.includes(`'${asset.key}'`), `database media authority lacks ${asset.key}`);
  if (asset.audio_file && asset.audio_sha256) {
    const payload = readFileSync(resolve('src/lib/morrow-media-assets', asset.audio_file));
    assert.equal(createHash('sha256').update(payload).digest('hex'), asset.audio_sha256,
      `${asset.key} audio custody hash drifted`);
  }
}
for (const required of ['readMorrowCase', 'context.media.find', 'createHash', 'private, no-store',
  'm03-captioned-voice-fragment.ogg', 'm11-current-voice-assembly.ogg']) {
  assert.ok(mediaRouteSource.includes(required), `authenticated media route lacks ${required}`);
}
const copperlineRpc = sql.split('create or replace function public.morrow_record_copperline_event', 2)[1]
  .split('create table if not exists morrow_private.capability_grants', 2)[0];
const duplicateBranch = copperlineRpc.split('if v_existing.event_key', 2)[1]
  .split("return query select 'duplicate'", 2)[0];
assert.equal(duplicateBranch.includes('actor_player_id'), false,
  'group idempotency must not collide when a second linked player submits the exact earned action');
for (const required of ['Accessible attachment metadata', 'Temporary maintenance state', 'No linked case found',
  'Case audit halted', 'Incident chronology', 'Synchronized field updates', 'player-specific', 'Sign in to this recovery case']) {
  assert.ok(routeSource.includes(required), `route lacks concrete state/copy: ${required}`);
}
for (const required of ['prove_audit_chronology', 'Select retained record', 'first broken edge']) {
  assert.ok(caseActionsSource.includes(required), `audit chronology form lacks ${required}`);
}
assert.equal(caseActionsSource.includes("@/lib/morrow-copperline-case"), false,
  'client-side form must not bundle the full server-owned progression/media model');
for (const required of ['signInWithOtp', 'shouldCreateUser: false', 'MORROW_CASE_ROUTE',
  "requestOrigin !== requestUrl.origin", 'pendingCookies', 'response.cookies.set']) {
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

console.log('MORROW COPPERLINE ACT 0-7: PASS direct/stale/identity/1-2-6/duplicate/collision/spoiler/outage/checksum/token/input/order/finale');

function payloadDigest(payload: Record<string, unknown>): string {
  return createHash('sha256').update(JSON.stringify(payload), 'utf8').digest('hex');
}
