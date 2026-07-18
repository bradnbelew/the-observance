import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const seed = readFileSync(resolve('supabase/seeds/v5_investigations.sql'), 'utf8');

const exactRecoveryShapes: Record<string, readonly string[]> = {
  'v5-lc05-motive': ['heat water cover', 'heat water shelter'],
  'v5-ko03-crack-map': ['conditional load brace', 'conditional brace failure'],
  'v5-kb03-altered-watch': ['toma bell eight', 'toma eight nessa later'],
  'v5-ki03-iss-correction': ['registrar signed out', 'iss signed registrar out'],
  'v5-cw05-counterfeit': ['false filters', 'counterfeit filters'],
  'v5-cw08-clear-nessa': ['clear nessa', 'nessa cleared'],
  'v5-bi08-break-inquest': ['fracture delay cut feedback', 'multi cause break'],
  'v5-a01-camp-ash': ['map supply not distance', 'wren distance false'],
  'v5-a10-private-window': ['inside access sender unresolved', 'private before public'],
};

for (const [key, answers] of Object.entries(exactRecoveryShapes)) {
  const encoded = `('${key}'`;
  const start = seed.indexOf(encoded);
  assert.ok(start >= 0, `${key}: missing recovery row`);
  const end = seed.indexOf('\n', start);
  const row = seed.slice(start, end < 0 ? undefined : end);
  const expectedArray = `array[${answers.map((answer) => `'${answer}'`).join(',')}]`;
  assert.ok(row.includes(expectedArray), `${key}: recovery answers drifted from ${expectedArray}`);
  assert.ok(row.toLocaleLowerCase('en-US').includes('recovery'), `${key}: exact fallback is not visibly classified as recovery`);
  assert.ok(answers.every((answer) => answer.length <= 31), `${key}: exact recovery token became a prose sentence`);
}

for (const stale of [
  'the certificate was conditional the heat load was not reduced and the brace failed',
  'nessa followed procedure diverted counterfeit supplies caused the failure and the hearing hid evidence',
  'the break combined preexisting failure diverted resources iss cut falsified timing delayed response and record feedback',
  'the map and supply entry agree use wren only to identify which distance is false',
  'wren is the only remaining source',
]) {
  assert.ok(!seed.toLocaleLowerCase('en-US').includes(stale), `stale restatement/spoiler survives: ${stale}`);
}

assert.ok(seed.includes('Interpretive primary paths use the semantic web/Discord predicates or physical Paper'));
console.log('legacy-answer-shape.selftest OK: 9 exact fallbacks are short recovery artifacts/actions, never hidden prose');
