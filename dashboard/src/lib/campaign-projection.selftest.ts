import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { docketPrompts, evidenceFor, projectCase, type CampaignProjection } from './campaign-projection';

const projection = JSON.parse(readFileSync(resolve('content/campaign/p5-p12.json'), 'utf8')) as CampaignProjection;
const inputRows = (projection.cases as Array<Record<string, unknown>>).flatMap((entry) => {
  const conclusions = Array.isArray(entry.conclusions) ? entry.conclusions : [];
  return entry.group_conclusion ? [...conclusions, entry.group_conclusion] : conclusions;
}) as Array<{ id: string; input_contract: { interpretive: boolean; runtime_exact_phrase: boolean; accepted_answers_are_human_examples: boolean } }>;
assert.equal(inputRows.length, 21);
assert.deepEqual(inputRows.filter((row) => row.input_contract.runtime_exact_phrase).map((row) => row.id), ['P11.F8']);
assert.ok(inputRows.filter((row) => row.input_contract.interpretive).every((row) =>
  !row.input_contract.runtime_exact_phrase && row.input_contract.accepted_answers_are_human_examples));
assert.deepEqual(projection.phases, ['P5', 'P6', 'P7', 'P8', 'P9', 'P10', 'P11', 'P12']);
assert.equal(projection.observation_receipts_gate_answers, false);
assert.equal(projectCase(projection, 'P9', new Set()), null);
const p9 = projectCase(projection, 'P9', new Set(['P8.F8']));
assert.ok(p9);
assert.ok(evidenceFor(p9).some((item) => item.id === 'p9.e07'));
assert.ok(docketPrompts(p9).some((item) => item.id === 'P9.F6'));
assert.equal(projectCase(projection, 'P12', new Set(['P11.F9', 'P10.F5', 'P8.F8'])), null);
const p12 = projectCase(projection, 'P12', new Set(['P11.F9', 'P10.F5', 'P8.F8', 'P7.F6']));
assert.ok(p12);
assert.match(evidenceFor(p12)[0]?.content ?? '', /SIX RETURN, ONE IS NOT KEPT/);
console.log('dashboard campaign projection selftest: PASS');
