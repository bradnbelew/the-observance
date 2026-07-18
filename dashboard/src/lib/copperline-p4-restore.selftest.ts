import { strict as assert } from 'node:assert';
import { validateP4Restore } from './copperline-p4-restore';

function form(fields: Record<string, string>) {
  const value = new FormData();
  for (const [key, entry] of Object.entries(fields)) value.set(key, entry);
  return value;
}

assert.equal(validateP4Restore(form({})).status, 'incomplete');
assert.equal(validateP4Restore(form({ operation: 'delete-originals' })).status, 'wrong');
const accepted = validateP4Restore(form({ operation: 'restore-retained-attachments' }));
assert.equal(accepted.status, 'accepted');
assert.equal(accepted.entries?.length, 5);
assert.equal(accepted.receiptId?.length, 64);
assert.equal(
  validateP4Restore(form({ operation: '  restore-retained-attachments  ' })).receiptId,
  accepted.receiptId,
  'same fixed restore action must return the same deterministic receipt',
);
console.log('Copperline P4 semantic restore action self-test passed');
