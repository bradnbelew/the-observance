import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { buildDocket, type DocketProjection } from './campaign-projection.js';

const projection = JSON.parse(readFileSync(resolve('src/v5/generated/p5-p12.json'), 'utf8')) as {
  phases: string[];
  observation_receipts_gate_answers: boolean;
  cases: DocketProjection[];
};
assert.equal(projection.observation_receipts_gate_answers, false);
assert.equal(buildDocket(projection.cases, 'P7', new Set()), null);
const p7 = buildDocket(projection.cases, 'P7', new Set(['P6.V', 'P6.S', 'P6.B', 'P6.I', 'P6.F7']));
assert.ok(p7);
assert.deepEqual(p7.prompts.map((item) => item.id), ['P7.F2', 'P7.F3', 'P7.F6']);
assert.equal(buildDocket(projection.cases, 'P12', new Set(['P11.F9', 'P10.F5', 'P8.F8'])), null);
const p12 = buildDocket(projection.cases, 'P12', new Set(['P11.F9', 'P10.F5', 'P8.F8', 'P7.F6']));
assert.ok(p12?.prompts.every((item) => item.zero_observation_acceptance));
console.log('discord campaign projection selftest: PASS');
