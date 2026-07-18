import assert from 'node:assert/strict';
import {
  NESSA_CORRECTION_CANONICAL_PAYLOAD,
  normalizeNessaFindingText,
  validNessaCorrection,
} from './nessa-correction.js';

assert.equal(normalizeNessaFindingText('  Relief—REPORTS  '), 'relief reports');
assert.deepEqual(NESSA_CORRECTION_CANONICAL_PAYLOAD, {
  finding_shape: 'cause-record-conduct-v1',
  cause: 'genuine-diverted-counterfeit-lower-intake',
  record: 'relief-and-complaint-chronology-edited',
  conduct: 'procedure-followed-report-before-failure',
  observation_receipts: 0,
});

for (const [index, finding] of [
  {
    cause: 'Genuine stock was diverted; substitute cloth first broke at the lower intake.',
    record: 'The relief shifts and complaint reports were edited.',
    conduct: 'She followed procedure and reported it before the cloth started shedding.',
  },
  {
    cause: 'They moved the real bolts and single-warp material failed at the upstream intake.',
    record: 'Someone rewrote the shift record and altered her report.',
    conduct: 'Nessa used procedure and raised the alarm before the filter failed.',
  },
  {
    cause: 'Counterfeit cloth broke at the lower intake after genuine cloth was rerouted.',
    record: 'Complaint entries and relief times changed.',
    conduct: 'Before it broke, she worked to procedure and flagged the fault.',
  },
].entries()) assert.equal(validNessaCorrection(finding), true, `natural equivalent finding ${index + 1} must pass`);

for (const [index, finding] of [
  {
    cause: 'Nessa contaminated the sample sink.',
    record: 'The chronology was complete.',
    conduct: 'She reported late.',
  },
  {
    cause: 'counterfeit lower intake diverted relief complaint report',
    record: 'followed procedure before shedding',
    conduct: 'edited shift record',
  },
  {
    cause: 'Real stock moved and substitute cloth failed at the lower intake.',
    record: 'The relief shift was edited.',
    conduct: 'She followed procedure before it failed.',
  },
].entries()) assert.equal(validNessaCorrection(finding), false, `wrong, field-swapped, or incomplete finding ${index + 1} must fail`);

console.log('nessa-correction.selftest OK: flexible component meaning, field separation, incomplete/wrong refusal');
