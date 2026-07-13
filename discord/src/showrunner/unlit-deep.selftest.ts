import assert from 'node:assert/strict';
import { voice } from '../voice.js';
import { decideUnlitDeepReport, type UnlitDeepInput } from './unlit-deep.js';

const base: UnlitDeepInput = {
  brokenAt: null,
  brokenBy: null,
  keptAt: null,
  lastBrokenReportedAt: null,
  lastKeptReportedAt: null,
};

assert.deepEqual(decideUnlitDeepReport(base), {
  kind: null, line: null, brokenMark: null, keptMark: null,
});

const broken = decideUnlitDeepReport({ ...base, brokenAt: 100, brokenBy: 'Never Spoken' });
assert.equal(broken.kind, 'broken');
assert.equal(broken.line, voice.tollUnlitDeep());
assert.ok(!broken.line?.includes('Never Spoken'));
assert.equal(broken.brokenMark, 100);

const kept = decideUnlitDeepReport({ ...base, keptAt: 200 });
assert.equal(kept.kind, 'kept');
assert.equal(kept.line, voice.keptUnlitDeep());
assert.equal(kept.keptMark, 200);

assert.equal(decideUnlitDeepReport({ ...base, brokenAt: 100, lastBrokenReportedAt: 100 }).kind, null);
assert.equal(decideUnlitDeepReport({ ...base, keptAt: 200, lastKeptReportedAt: 201 }).kind, null);
assert.equal(decideUnlitDeepReport({ ...base, brokenAt: Number.NaN, keptAt: Number.POSITIVE_INFINITY }).kind, null);

// Backlog order is stable and only one report leaves per decision.
assert.equal(decideUnlitDeepReport({ ...base, brokenAt: 300, keptAt: 250 }).kind, 'kept');
assert.equal(decideUnlitDeepReport({ ...base, brokenAt: 300, keptAt: 250, lastKeptReportedAt: 250 }).kind, 'broken');
assert.equal(decideUnlitDeepReport({ ...base, brokenAt: 300, keptAt: 300 }).kind, 'broken');

console.log('unlit deep self-test: OK - kept/broken, replay, backlog order, privacy, malformed flags');
