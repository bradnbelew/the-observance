import assert from 'node:assert/strict';
import { signMorrowBody, verifyMorrowEnvelope } from './morrow-runtime-envelope';

const secret = 'test-secret-that-is-not-used-in-production';
const nowMs = Date.parse('2026-08-30T12:00:00.000Z');
const timestamp = String(nowMs / 1000);
const valid = {
  campaignId: '8e0b1a62-d2dd-4e86-91f1-4b07af5e2922',
  releaseId: 'morrow.rehearsal.001',
  eventKey: 'morrow.act1.room04_witnessed',
  idempotencyKey: 'paper:room04:8e0b1a62',
  actorMinecraftUuid: '571d20b8-bf85-4e8a-92ac-3b071ffc882d',
  occurredAt: '2026-08-30T12:00:00.000Z',
  payload: { room: 'recovery_04', witnesses: 3 },
};
const body = JSON.stringify(valid);
const verify = (rawBody = body, at = nowMs, signature = signMorrowBody(secret, timestamp, rawBody)) =>
  verifyMorrowEnvelope({ rawBody, timestamp, signature, secret, nowMs: at });

assert.equal(verify().ok, true);
assert.deepEqual(verify(), verify(), 'payload hashing must be deterministic');
assert.deepEqual(verify(body, nowMs, 'v1=' + '0'.repeat(64)), { ok: false, reason: 'bad_signature' });
assert.deepEqual(verify(body, nowMs + 300_001), { ok: false, reason: 'stale_request' });

const legacy = JSON.stringify({ ...valid, eventKey: 'v5.hold.opened' });
assert.deepEqual(verify(legacy), { ok: false, reason: 'invalid_event' });
const webOwned = JSON.stringify({ ...valid, eventKey: 'morrow.act0.case_chain_authenticated' });
assert.deepEqual(verify(webOwned), { ok: false, reason: 'invalid_event' });
const huge = JSON.stringify({ ...valid, payload: { value: 'x'.repeat(17_000) } });
assert.deepEqual(verify(huge), { ok: false, reason: 'payload_too_large' });

console.log('MORROW RUNTIME ENVELOPE: PASS');
