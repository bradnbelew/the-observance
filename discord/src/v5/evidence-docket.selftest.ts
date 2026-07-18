import assert from 'node:assert/strict';
import { EVIDENCE_DOCKET, evidenceDocketForPhase, formatEvidenceDocket } from './evidence-docket.js';

assert.deepEqual(EVIDENCE_DOCKET.map((record) => record.id), [
  'p5.e10', 'p6.m3', 'p6.b2', 'p7.e04', 'p8.e07', 'p9.e05', 'p10.e01', 'p10.e06',
  'p11.e07', 'p11.e09', 'p12.e05',
]);
assert.equal(evidenceDocketForPhase('p6').length, 2);
assert.equal(evidenceDocketForPhase('p12').length, 1);
for (const record of EVIDENCE_DOCKET) {
  assert.ok(record.availableAfter.match(/^p\d+\./));
  assert.ok(record.provenance.length >= 30);
  assert.ok(record.body.length >= 80);
  assert.ok(!record.body.toLowerCase().includes('the answer is'));
}
assert.ok(formatEvidenceDocket(evidenceDocketForPhase('p6')).length < 1900);
console.log('Discord evidence docket selftest: PASS');
