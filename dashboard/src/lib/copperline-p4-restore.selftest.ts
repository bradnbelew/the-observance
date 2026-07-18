import { strict as assert } from 'node:assert';
import { validateP4Restore } from './copperline-p4-restore';

function form(fields: Record<string, string>) {
  const value = new FormData();
  for (const [key, entry] of Object.entries(fields)) value.set(key, entry);
  return value;
}

assert.equal(validateP4Restore(form({})).status, 'incomplete');
assert.equal(validateP4Restore(form({ ticket: '2184', attachment: 'wrong.txt', order: '03-04', idempotency: 'review-01' })).status, 'wrong');
const accepted = validateP4Restore(form({ ticket: '2184', attachment: 'mouth_notice.compare.txt', order: '03 before 04', idempotency: 'review-01' }));
assert.equal(accepted.status, 'accepted');
assert.equal(accepted.entries?.length, 5);
assert.equal(accepted.receiptId?.length, 64);
assert.equal(validateP4Restore(form({ ticket: '2184', attachment: 'mouth_notice.compare.txt', order: '03-04', idempotency: 'review-01' })).receiptId, accepted.receiptId,
  'same normalized request must return the same idempotency receipt');
assert.equal(validateP4Restore(form({ ticket: '2184', attachment: 'mouth_notice.compare.txt', order: '03-04', idempotency: 'x' })).status, 'incomplete');
console.log('Copperline P4 real restore form self-test passed');
