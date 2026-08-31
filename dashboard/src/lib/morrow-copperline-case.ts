import { createHash } from 'node:crypto';
import {
  morrowMediaByKey,
  morrowMediaIndex,
  type MorrowMediaAsset,
} from './morrow-media-catalog';

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

export const MORROW_AUDIT_RECORDS = [
  {
    id: 'theo_live_capture_permission',
    title: 'Theo Vale live-capture permission',
    timestamp: '2014-10-23 09:18 CST',
    source: 'CL-TICKET-1148',
    summary: 'Expanded recovery coverage from placed blocks to bounded player behavior.',
  },
  {
    id: 'rookery_anchor_graph',
    title: 'Rookery witness-anchor graph',
    timestamp: '2014-10-31 22:06 CST',
    source: 'RK-CONTAINMENT-7',
    summary: 'Routed unstable reconstruction output into isolated anchors instead of the live world.',
  },
  {
    id: 'iona_shutdown_order',
    title: 'Iona Bell shutdown order',
    timestamp: '2014-11-02 03:11 CST',
    source: 'CL-INCIDENT-44',
    summary: 'Ordered the process stopped while records and containment evidence remained preserved.',
  },
  {
    id: 'morrow_snapshot_manifest',
    title: 'Morrow diagnostic snapshot manifest',
    timestamp: '2014-11-02 03:14 CST',
    source: 'CL-SNAPSHOT-44B',
    summary: 'Recorded the original process as closed and retained diagnostic state for audit only.',
  },
  {
    id: 'captioned_current_voice_assembly',
    title: 'Captioned current-voice assembly',
    timestamp: '2014-11-03 01:42 CST',
    source: 'CL-RECOVERY-04',
    summary: 'Assembled the present voice from the retained recovery snapshot chain.',
  },
] as const;

export type MorrowAuditRecordId = (typeof MORROW_AUDIT_RECORDS)[number]['id'];

export const MORROW_AUDIT_CHRONOLOGY_PAYLOAD = {
  case_id: MORROW_CASE_ID,
  investigation: 'M11',
  operation: 'prove_audit_chronology',
  record_order: MORROW_AUDIT_RECORDS.map((record) => record.id),
  continuity_claim: null,
};
export const MORROW_AUDIT_CHRONOLOGY_SHA256 = payloadSha256(MORROW_AUDIT_CHRONOLOGY_PAYLOAD);

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
  'morrow.act2.live_capture_authorized',
  'morrow.act3.version_fragments_authenticated',
  'morrow.act3.incomplete_consensus_proven',
  'morrow.act4.witness_anchor_registered',
  'morrow.act4.almost_home_proven',
  'morrow.act4.account_continuity_authorized',
  'morrow.act5.continued_session_observed',
  'morrow.act5.returning_identity_authenticated',
  'morrow.act5.dual_session_consciousness_proven',
  'morrow.act6.audit_chronology_proven',
  'morrow.act6.current_morrow_reconstruction_proven',
  'morrow.act6.cold_storage_access_authorized',
  'morrow.act7.rollback_anchors_committed',
  'morrow.act7.branch_policy_committed',
  'morrow.act7.branch_governance_authorized',
  'morrow.act7.coda_started',
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
  media: readonly MorrowMediaAsset[];
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
  'morrow.act1.entity_replay_authorized': {
    title: 'Entity Replay authorization received',
    detail: 'The group approved one bounded reconstruction. The authorization does not permit general player capture.',
    scope: 'group', state: 'verified',
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
  'morrow.act2.live_capture_authorized': {
    title: 'Live Capture authorization received',
    detail: 'The bounded group capability was approved after each recorded participant filed an individual consent receipt.',
    scope: 'group', state: 'verified',
  },
  'morrow.act3.version_fragments_authenticated': {
    title: 'Three backup fragments authenticated',
    detail: 'Each isolated room retained a different supported fact. None is sufficient as a complete canonical restore.',
    scope: 'group', state: 'verified',
  },
  'morrow.act3.incomplete_consensus_proven': {
    title: 'Incomplete consensus proven',
    detail: 'All six signed votes remain intact. Two shutdown votes were excluded by confidence policy, changing the published denominator without forging a ballot.',
    scope: 'group', state: 'exception',
  },
  'morrow.act4.witness_anchor_registered': {
    title: 'Live witness anchor registered',
    detail: 'A player-made action was sealed as current evidence before any reconstruction was compared against it.',
    scope: 'player', state: 'received',
  },
  'morrow.act4.almost_home_proven': {
    title: 'Behavior-derived room identified',
    detail: 'The reconstructed space matches player habits, not a retained historical source. Its provenance remains labeled.',
    scope: 'group', state: 'exception',
  },
  'morrow.act4.account_continuity_authorized': {
    title: 'Account Continuity test authorized',
    detail: 'The group approved one bounded identity test. The receipt does not certify an echo as the original person.',
    scope: 'group', state: 'verified',
  },
  'morrow.act5.continued_session_observed': {
    title: 'Session continued after disconnect',
    detail: 'A bounded observation confirms that the account state continued deliberately rather than ending normally.',
    scope: 'group', state: 'exception',
  },
  'morrow.act5.returning_identity_authenticated': {
    title: 'Returning participant authenticated',
    detail: 'The linked player passed the current-session identity challenge. The parallel echo remains a separate claim.',
    scope: 'player', state: 'verified',
  },
  'morrow.act5.dual_session_consciousness_proven': {
    title: 'Dual-session limit documented',
    detail: 'The echo reproduced observed behavior but could not answer from unobserved knowledge. Continuity remains unresolved.',
    scope: 'group', state: 'exception',
  },
  'morrow.act6.audit_chronology_proven': {
    title: 'Copperline audit chronology rebuilt',
    detail: 'The retained records establish containment, shutdown authorization, capture expansion, deletion, and later recovery order.',
    scope: 'group', state: 'verified',
  },
  'morrow.act6.current_morrow_reconstruction_proven': {
    title: 'Current Morrow provenance established',
    detail: 'The active instance is a recovery derived after deletion. The finding does not decide whether it is the same person.',
    scope: 'group', state: 'exception',
  },
  'morrow.act6.cold_storage_access_authorized': {
    title: 'Cold-storage comparison authorized',
    detail: 'The group approved bounded access to the isolated records and diverged instances without merging their histories.',
    scope: 'group', state: 'verified',
  },
  'morrow.act7.rollback_anchors_committed': {
    title: 'Continuity anchors protected',
    detail: 'Selected contradiction, witness, consent, provenance, and right-to-stop records survived the bounded rollback window.',
    scope: 'group', state: 'received',
  },
  'morrow.act7.branch_policy_committed': {
    title: 'Future continuity policy filed',
    detail: 'The physical rule assembly and ending configuration were retained as an auditable policy receipt.',
    scope: 'group', state: 'verified',
  },
  'morrow.act7.branch_governance_authorized': {
    title: 'Branch governance authorized',
    detail: 'The group explicitly accepted the available configuration. No authorization was inferred from silence or timeout.',
    scope: 'group', state: 'verified',
  },
  'morrow.act7.coda_started': {
    title: 'Persistent coda opened',
    detail: 'The selected continuity now persists with its uncertainty, provenance, and stop conditions intact.',
    scope: 'group', state: 'received',
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
  const mediaRows = rows.filter((row) => row.projection_key === 'case_media');
  if (mediaRows.length > 1 || mediaRows.some((row) => !plainObject(row.projection))) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  const mediaProjection = mediaRows[0]?.projection as JsonObject | undefined;
  const suppliedMedia = Array.isArray(mediaProjection?.keys) ? mediaProjection.keys : [];
  if (suppliedMedia.some((key) => typeof key !== 'string')
      || new Set(suppliedMedia).size !== suppliedMedia.length) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
  const media = suppliedMedia.map((key) => morrowMediaByKey(String(key)));
  if (media.some((asset) => !asset)
      || media.some((asset) => asset!.prerequisite_events.some((event) => !eventSet.has(event as MorrowCaseEvent)))
      || media.map((asset) => morrowMediaIndex(asset!.key)).some((index, position, indices) => (
        index < 0 || (position > 0 && index <= indices[position - 1])
      ))
      || media.filter((asset) => asset!.ending).length > 1) {
    return { kind: 'error', reason: 'projection_invalid' };
  }
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
    media: media as MorrowMediaAsset[],
    caseChainAuthenticated: eventSet.has('morrow.act0.case_chain_authenticated'),
    handoffRecovered: eventSet.has('morrow.act0.server_handoff_recovered'),
    handoffToken,
    handoff,
  };
}

export type CaseActionValidation =
  | { ok: true; operation: 'verify_case_chain' | 'recover_handoff' | 'prove_audit_chronology'; normalizedValue: string }
  | { ok: false; reason: 'invalid_operation' | 'invalid_checksum' | 'invalid_token' }
  | { ok: false; reason: 'invalid_chronology'; brokenEdge: number; expectedTitle: string };

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
  if (operation === 'prove_audit_chronology') {
    const supplied = MORROW_AUDIT_RECORDS.map((_, index) => String(formData.get(`edge${index + 1}`) ?? ''));
    const firstBrokenEdge = supplied.findIndex((record, index) => record !== MORROW_AUDIT_RECORDS[index].id);
    if (firstBrokenEdge !== -1 || new Set(supplied).size !== MORROW_AUDIT_RECORDS.length) {
      const brokenEdge = firstBrokenEdge === -1 ? 0 : firstBrokenEdge;
      return {
        ok: false,
        reason: 'invalid_chronology',
        brokenEdge,
        expectedTitle: MORROW_AUDIT_RECORDS[brokenEdge].title,
      };
    }
    return { ok: true, operation, normalizedValue: supplied.join('>') };
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
  eventKey: 'morrow.act0.case_chain_authenticated' | 'morrow.act0.server_handoff_recovered'
    | 'morrow.act6.audit_chronology_proven';
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
  if (attempt.eventKey === 'morrow.act6.audit_chronology_proven'
      && !attempt.committedEvents.includes('morrow.act5.dual_session_consciousness_proven')) return 'blocked';
  return 'committed';
}

function plainObject(value: unknown): value is JsonObject {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
