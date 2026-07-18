import {
  DEFAULT_SHOWRUNNER_TICK_MS,
  MAX_SHOWRUNNER_TICK_MS,
  MIN_SHOWRUNNER_TICK_MS,
  normalizeLeaseSeconds,
  normalizeTickMs,
} from './persistent-policy.js';

function assert(ok: boolean, message: string): void {
  if (!ok) throw new Error(`persistent showrunner selftest FAILED: ${message}`);
}

assert(normalizeTickMs(undefined) === DEFAULT_SHOWRUNNER_TICK_MS, 'default cadence');
assert(normalizeTickMs(1) === MIN_SHOWRUNNER_TICK_MS, 'cadence lower clamp');
assert(normalizeTickMs(99_999) === MAX_SHOWRUNNER_TICK_MS, 'cadence upper clamp');
assert(normalizeTickMs('13000') === 13_000, 'cadence env parsing');
assert(normalizeLeaseSeconds(undefined) === 300, 'lease default');
assert(normalizeLeaseSeconds(1) === 60, 'lease lower clamp');
assert(normalizeLeaseSeconds(2_000) === 900, 'lease upper clamp');

console.log('persistent showrunner selftest OK');
