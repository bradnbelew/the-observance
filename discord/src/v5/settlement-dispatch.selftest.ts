import assert from 'node:assert/strict';
import { SETTLEMENT_DISPATCH_CANONICAL_PAYLOAD, validSettlementDispatch } from './settlement-dispatch.js';

assert.equal(SETTLEMENT_DISPATCH_CANONICAL_PAYLOAD.observation_receipts, 0);
for (const value of [
  'Aro and Pell disagree about the mark date; keep both accounts open.',
  'The accounts conflict on where the work mark stood. Record both without choosing an official version.',
  'Their dates for the repair mark contradict each other. We cannot settle this yet.',
]) assert.equal(validSettlementDispatch(value), true);
for (const value of [
  'please let us in now',
  'They disagree, and Aro is clearly right.',
  'Keep the mark open.',
]) assert.equal(validSettlementDispatch(value), false);

console.log('settlement-dispatch.selftest OK: meaningful disagreement, open finding, canonical no-raw payload');
