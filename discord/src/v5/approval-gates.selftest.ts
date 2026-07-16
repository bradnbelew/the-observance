import assert from 'node:assert/strict';
import { approvalPermits, authoredPayloadSha256, mayRunAutomatically } from './approval-gates.js';

const payload = { body: 'authored words', puzzle: 'p1', tier: 2 };
const envelope = {
  approval_id: 'approval-1',
  approval_class: 'A3' as const,
  approval_scope: 'group:g1/finding:p1/tier:2',
  authored_payload_sha256: authoredPayloadSha256(payload),
  approval_expires_at: '2026-07-16T01:00:00.000Z',
};

assert.equal(mayRunAutomatically('A0'), true);
assert.equal(mayRunAutomatically('A1'), true);
for (const risk of ['A2', 'A3', 'A4', 'A5'] as const) assert.equal(mayRunAutomatically(risk), false);
assert.equal(approvalPermits('A3', envelope.approval_scope, payload, envelope, new Date('2026-07-16T00:00:00Z')), true);
assert.equal(approvalPermits('A3', envelope.approval_scope, { ...payload, body: 'changed' }, envelope, new Date('2026-07-16T00:00:00Z')), false);
assert.equal(approvalPermits('A2', envelope.approval_scope, payload, envelope, new Date('2026-07-16T00:00:00Z')), false);
assert.equal(approvalPermits('A3', 'group:other', payload, envelope, new Date('2026-07-16T00:00:00Z')), false);
assert.equal(approvalPermits('A3', envelope.approval_scope, payload, envelope, new Date('2026-07-16T01:00:00Z')), false);
assert.equal(approvalPermits('A3', envelope.approval_scope, payload, undefined, new Date('2026-07-16T00:00:00Z')), false);
console.log('M2 Discord approval-gate self-test passed');
