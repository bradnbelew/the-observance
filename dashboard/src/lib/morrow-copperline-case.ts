import { createHash } from 'node:crypto';

export const MORROW_CASE_ROUTE = '/support/cases/mossfield-recovery';
export const MORROW_CASE_ID = 'CL-RCV-04';
export const MORROW_CASE_ATTACHMENT_TEXT = [
  'COPPERLINE HOSTING / BACKUP & RECOVERY',
  'CASE: CL-RCV-04',
  'SOURCE: storage-controller export',
  'CUSTODY: controller export > recovery intake > support attachment',
  'ROOM: Recovery Room 04',
  'HANDOFF: release after custody checksum verification',
  'NOTE: address intentionally absent from archive copy.',
].join('\n') + '\n';
export const MORROW_CASE_ATTACHMENT_SHA256 = payloadSha256Text(MORROW_CASE_ATTACHMENT_TEXT);

export const MORROW_CASE_EVENT_ORDER = [
  'morrow.act0.case_chain_authenticated',
  'morrow.act0.server_handoff_recovered',
  'morrow.act1.room04_witnessed',
  'morrow.act1.static_proposal_authenticated',
  'morrow.act1.intention_error_proven',
  'morrow.act1.entity_replay_authorized',
  'morrow.act2.missing_role_completed',
  'morrow.act2.live_test_recorded',
  'morrow.act2.behavior_reuse_proven',
] as const;

export type MorrowCaseEvent = (typeof MORROW_CASE_EVENT_ORDER)[number];

export type MorrowProjectionRow = {
  campaign_id: string;
  player_id: string;
  projection_key: string;
  projection: unknown;
  revision: number;
  updated_at: string;
};

export type MorrowCaseUpdate = {
  eventKey: MorrowCaseEvent;
  timeLabel: string;
  title: string;
  detail: string;
  scope: 'group' | 'player';
  state: 'received' | 'exception' | 'verified';
};

export type MorrowCaseContext = {
  kind: 'ready';
  campaignId: string;
  playerId: string;
  releaseId: string;
  displayAlias: string;
  groupSize: number;
  revision: number;
  events: readonly MorrowCaseEvent[];
  updates: readonly MorrowCaseUpdate[];
  playerReceipts: readonly string[];
  caseChainAuthenticated: boolean;
  handoffRecovered: boolean;
  handoffToken: string | null;
  handoff: null | { serverLabel: string; joinAddress: string; recoveryReceipt: string };
};

export type MorrowCaseRead =
  | { kind: 'empty'; reason: 'authentication_required' | 'case_not_assigned' }
  | { kind: 'error'; reason: 'stale_release' | 'identity_mismatch' | 'projection_invalid' | 'projection_out_of_order' }
  | MorrowCaseContext;

type JsonObject = Record<string, unknown>;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const RELEASE = /^[a-z0-9][a-z0-9._-]{6,79}$/;
const ADDRESS = /^[a-z0-9.-]{3,120}(?::[0-9]{1,5})?$/i;
const EVENT_SET = new Set<string>(MORROW_CASE_EVENT_ORDER);

const UPDATE_CONTENT: Partial<Record<MorrowCaseEvent, Omit<MorrowCaseUpdate, 'eventKey' | 'timeLabel'>>> = {
  'morrow.act1.room04_witnessed': {
    title: 'Recovery Room 04 witness check-in received',
    detail: 'The damaged-room case now has a live witness. Static work remains bounded and unapproved.',
    scope: 'group', state: 'received',
  },
  'morrow.act1.static_proposal_authenticated': {
    title: 'Bounded Static Restore proposal authenticated',
    detail: 'Six proposed cells entered physical review. This ticket records the proposal, not the result.',
    scope: 'group', state: 'verified',
  },
  'morrow.act1.intention_error_proven': {
    title: 'Exception filed: intention entered as history',
    detail: 'One proposed cell had discussion records but no placement source. Its provenance remains inferred.',
    scope: 'group', state: 'exception',
  },
  'morrow.act2.missing_role_completed': {
    title: 'Thirty-seven-second replay completed',
    detail: 'A current participant supplied the missing recorded role. Behavioral Coverage increased.',
    scope: 'group', state: 'received',
  },
  'morrow.act2.live_test_recorded': {
    title: 'Deliberate route sealed locally',
    detail: 'Copperline received a clip hash and bounded summary. Raw movement samples remain on the game server.',
    scope: 'player', state: 'verified',
  },
  'morrow.act2.behavior_reuse_proven': {
    title: 'Historical reconstruction matched the deliberate test',
    detail: 'The later echo reproduced the sealed route hash and exact duration. Case escalation is warranted.',
    scope: 'group', state: 'exception',
  },
};

export function projectMorrowCase(
  rows: readonly MorrowProjectionRow[],
  expectedReleaseId: string,
): MorrowCaseRead {
  if (rows.length === 0) return { kind: 'empty', reason: 'case_not_assigned' };
  if (!RELEASE.test(expectedReleaseId)) return { kind: 'error', reason: 'stale_release' };
  const campaignIds = new Set(rows.map((row) => row.campaign_id));
  const playerIds = new Set(rows.map((row) => row.player_id));
  if (campaignIds.size !== 1 || playerIds.size !== 1
      || !UUID.test(rows[0].campaign_id) || !UUID.test(rows[0].player_id)) {
    return { kind: 'error', reason: 'identity_mismatch' };
  }
  const accessRows = rows.filter((row) => row.projection_key === 'case_access');
  if (accessRows.length !== 1 || !plainObject(accessRows[0].projection)) {
    return { kind: 'empty', reason: 'case_not_assigned' };
  }
  const access = accessRows[0].projection;
  if (access.caseId !== MORROW_CASE_ID || access.releaseId !== expectedReleaseId
      || !RELEASE.test(String(access.releaseId ?? ''))
      || typeof access.displayAlias !== 'string' || !/^.{1,32}$/u.test(access.displayAlias)
      || !Number.isInteger(access.groupSize) || Number(access.groupSize) < 1 || Number(access.groupSize) > 6) {
    return access.releaseId !== expectedReleaseId
      ? { kind: 'error', reason: 'stale_release' }
      : { kind: 'error', reason: 'projection_invalid' };
  }

  const progressRows = rows.filter((row) => row.projection_key === 'case_progress');
  if (progressRows.length > 1 || progressRows.some((row) => !plainObject(row.projection))) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  const progress = progressRows[0]?.projection as JsonObject | undefined;
  const suppliedEvents = Array.isArray(progress?.events) ? progress.events : [];
  if (suppliedEvents.some((event) => typeof event !== 'string')) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  const knownEvents = suppliedEvents.filter((event): event is MorrowCaseEvent => EVENT_SET.has(event as string));
  const eventIndices = knownEvents.map((event) => MORROW_CASE_EVENT_ORDER.indexOf(event));
  if (new Set(knownEvents).size !== knownEvents.length
      || eventIndices.some((index, position) => index !== position)) {
    return { kind: 'error', reason: 'projection_out_of_order' };
  }

  const receiptRows = rows.filter((row) => row.projection_key === 'player_receipts');
  if (receiptRows.length > 1 || receiptRows.some((row) => !plainObject(row.projection))) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  const receiptProjection = receiptRows[0]?.projection as JsonObject | undefined;
  const receipts = Array.isArray(receiptProjection?.receipts)
    ? receiptProjection.receipts.filter((value): value is string => typeof value === 'string').slice(0, 12)
    : [];

  const eventSet = new Set(knownEvents);
  const updates = knownEvents.flatMap((eventKey, index) => {
    const content = UPDATE_CONTENT[eventKey];
    return content ? [{ eventKey, timeLabel: `Update ${String(index + 1).padStart(2, '0')}`, ...content }] : [];
  });

  let handoff: MorrowCaseContext['handoff'] = null;
  let handoffToken: string | null = null;
  const tokenRows = rows.filter((row) => row.projection_key === 'handoff_token');
  if (tokenRows.length > 1 || tokenRows.some((row) => !plainObject(row.projection))) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  if (eventSet.has('morrow.act0.case_chain_authenticated') && tokenRows.length === 1) {
    const token = (tokenRows[0].projection as JsonObject).token;
    if (typeof token !== 'string' || !/^[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(token)) {
      return { kind: 'error', reason: 'projection_invalid' };
    }
    handoffToken = token;
  }
  const handoffRows = rows.filter((row) => row.projection_key === 'server_handoff');
  if (handoffRows.length > 1 || handoffRows.some((row) => !plainObject(row.projection))) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  if (eventSet.has('morrow.act0.server_handoff_recovered') && handoffRows.length === 1) {
    const projection = handoffRows[0].projection as JsonObject;
    if (projection.releaseId !== expectedReleaseId || typeof projection.serverLabel !== 'string'
        || typeof projection.joinAddress !== 'string' || !ADDRESS.test(projection.joinAddress)
        || typeof projection.recoveryReceipt !== 'string' || projection.recoveryReceipt.length > 96) {
      return { kind: 'error', reason: 'projection_invalid' };
    }
    handoff = {
      serverLabel: projection.serverLabel,
      joinAddress: projection.joinAddress,
      recoveryReceipt: projection.recoveryReceipt,
    };
  }

  return {
    kind: 'ready',
    campaignId: rows[0].campaign_id,
    playerId: rows[0].player_id,
    releaseId: expectedReleaseId,
    displayAlias: access.displayAlias,
    groupSize: Number(access.groupSize),
    revision: Math.max(...rows.map((row) => row.revision)),
    events: knownEvents,
    updates,
    playerReceipts: receipts,
    caseChainAuthenticated: eventSet.has('morrow.act0.case_chain_authenticated'),
    handoffRecovered: eventSet.has('morrow.act0.server_handoff_recovered'),
    handoffToken,
    handoff,
  };
}

export type CaseActionValidation =
  | { ok: true; operation: 'verify_case_chain' | 'recover_handoff'; normalizedValue: string }
  | { ok: false; reason: 'invalid_operation' | 'invalid_checksum' | 'invalid_token' };

export function validateMorrowCaseAction(formData: FormData): CaseActionValidation {
  const operation = String(formData.get('operation') ?? '');
  if (operation === 'verify_case_chain') {
    const checksum = String(formData.get('checksum') ?? '').trim().toLowerCase();
    return checksum === MORROW_CASE_ATTACHMENT_SHA256
      ? { ok: true, operation, normalizedValue: checksum }
      : { ok: false, reason: 'invalid_checksum' };
  }
  if (operation === 'recover_handoff') {
    const token = String(formData.get('handoffToken') ?? '').trim().toUpperCase();
    return /^[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(token)
      ? { ok: true, operation, normalizedValue: token }
      : { ok: false, reason: 'invalid_token' };
  }
  return { ok: false, reason: 'invalid_operation' };
}

export function payloadSha256(payload: Record<string, unknown>): string {
  return createHash('sha256').update(JSON.stringify(payload), 'utf8').digest('hex');
}

function payloadSha256Text(value: string): string {
  return createHash('sha256').update(value, 'utf8').digest('hex');
}

export type CopperlineReceiptAttempt = {
  campaignId: string;
  playerId: string;
  releaseId: string;
  linkedCampaignId: string;
  linkedPlayerId: string;
  activeReleaseId: string;
  eventKey: 'morrow.act0.case_chain_authenticated' | 'morrow.act0.server_handoff_recovered';
  idempotencyKey: string;
  payloadHash: string;
  committedEvents: readonly MorrowCaseEvent[];
  existing?: {
    campaignId: string;
    playerId: string;
    releaseId: string;
    eventKey: string;
    idempotencyKey: string;
    payloadHash: string;
  };
};

/** Dependency-free mirror of the authenticated Copperline RPC's fail-closed receipt rules. */
export function evaluateCopperlineReceipt(
  attempt: CopperlineReceiptAttempt,
): 'committed' | 'blocked' | 'duplicate' | 'collision' {
  if (attempt.campaignId !== attempt.linkedCampaignId || attempt.playerId !== attempt.linkedPlayerId
      || attempt.releaseId !== attempt.activeReleaseId || !RELEASE.test(attempt.releaseId)
      || !/^[a-z0-9][a-z0-9:._/-]{7,159}$/.test(attempt.idempotencyKey)
      || !/^[0-9a-f]{64}$/.test(attempt.payloadHash)) return 'blocked';
  if (attempt.existing) {
    return attempt.existing.campaignId === attempt.campaignId
      && attempt.existing.releaseId === attempt.releaseId
      && attempt.existing.eventKey === attempt.eventKey
      && attempt.existing.idempotencyKey === attempt.idempotencyKey
      && attempt.existing.payloadHash === attempt.payloadHash ? 'duplicate' : 'collision';
  }
  if (attempt.eventKey === 'morrow.act0.server_handoff_recovered'
      && !attempt.committedEvents.includes('morrow.act0.case_chain_authenticated')) return 'blocked';
  return 'committed';
}

function plainObject(value: unknown): value is JsonObject {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
