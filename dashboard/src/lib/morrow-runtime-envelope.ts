import { createHash, createHmac, timingSafeEqual } from 'node:crypto';

export const MORROW_MINECRAFT_EVENT_KEYS = [
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
  'morrow.act6.current_morrow_reconstruction_proven',
  'morrow.act6.cold_storage_access_authorized',
  'morrow.act7.rollback_anchors_committed',
  'morrow.act7.branch_policy_committed',
  'morrow.act7.branch_governance_authorized',
  'morrow.act7.coda_started',
] as const;

const EVENT_KEYS = new Set<string>(MORROW_MINECRAFT_EVENT_KEYS);
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const RELEASE = /^[a-z0-9][a-z0-9._-]{6,79}$/;
const IDEMPOTENCY = /^[a-z0-9][a-z0-9:._/-]{7,159}$/;

export type MorrowMinecraftEvent = {
  campaignId: string;
  releaseId: string;
  eventKey: (typeof MORROW_MINECRAFT_EVENT_KEYS)[number];
  idempotencyKey: string;
  actorMinecraftUuid: string | null;
  occurredAt: string;
  payload: Record<string, unknown>;
};

export type EnvelopeResult =
  | { ok: true; event: MorrowMinecraftEvent; payloadSha256: string }
  | { ok: false; reason: string };

function plainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function signMorrowBody(secret: string, timestamp: string, rawBody: string): string {
  return `v1=${createHmac('sha256', secret).update(`${timestamp}.${rawBody}`, 'utf8').digest('hex')}`;
}

export function verifyMorrowEnvelope(input: {
  rawBody: string;
  timestamp: string | null;
  signature: string | null;
  secret: string;
  nowMs?: number;
}): EnvelopeResult {
  if (!input.secret || !input.timestamp || !input.signature) return { ok: false, reason: 'missing_auth' };
  if (Buffer.byteLength(input.rawBody, 'utf8') > 20_000) return { ok: false, reason: 'body_too_large' };

  const seconds = Number(input.timestamp);
  const nowMs = input.nowMs ?? Date.now();
  if (!Number.isSafeInteger(seconds) || Math.abs(nowMs - seconds * 1000) > 300_000) {
    return { ok: false, reason: 'stale_request' };
  }

  const expected = Buffer.from(signMorrowBody(input.secret, input.timestamp, input.rawBody), 'utf8');
  const received = Buffer.from(input.signature, 'utf8');
  if (expected.length !== received.length || !timingSafeEqual(expected, received)) {
    return { ok: false, reason: 'bad_signature' };
  }

  let candidate: unknown;
  try {
    candidate = JSON.parse(input.rawBody);
  } catch {
    return { ok: false, reason: 'invalid_json' };
  }
  if (!plainObject(candidate)) return { ok: false, reason: 'invalid_event' };
  const actor = candidate.actorMinecraftUuid;
  const occurredAt = candidate.occurredAt;
  const payload = candidate.payload;
  if (!UUID.test(String(candidate.campaignId ?? ''))
      || !RELEASE.test(String(candidate.releaseId ?? ''))
      || !EVENT_KEYS.has(String(candidate.eventKey ?? ''))
      || !IDEMPOTENCY.test(String(candidate.idempotencyKey ?? ''))
      || !(actor === null || (typeof actor === 'string' && UUID.test(actor)))
      || typeof occurredAt !== 'string'
      || !Number.isFinite(Date.parse(occurredAt))
      || !plainObject(payload)) {
    return { ok: false, reason: 'invalid_event' };
  }
  const payloadJson = JSON.stringify(payload);
  if (Buffer.byteLength(payloadJson, 'utf8') > 16_384) return { ok: false, reason: 'payload_too_large' };

  return {
    ok: true,
    event: candidate as MorrowMinecraftEvent,
    payloadSha256: createHash('sha256').update(payloadJson, 'utf8').digest('hex'),
  };
}
