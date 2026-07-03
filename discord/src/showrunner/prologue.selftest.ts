/**
 * prologue.selftest.ts — the pure self-test the cold-start-prologue doc (§7 seam 5) promises. No DB, no
 * network, no clock, no LLM: it imports prologue.ts with nothing and pins the LAWS the ignition spine
 * exists to honor —
 *   - the sequence: dormant → ignited (one-shot ack) → acknowledged (steady state);
 *   - the precision gate: named report ONLY on an overwhelming single signal + a name, else the
 *     un-named FACT-1 fallback (a wrong "it knows you" is worse than none);
 *   - idempotency: the ack posts EXACTLY once — an already-acked ignition never re-posts;
 *   - the overwhelming-signal measurement: a confident single-dominant habit names a player; a
 *     flat / tied / nameless / faint / empty field degrades to the un-named line (never a guess);
 *   - determinism: same input → same output.
 *
 * Runs standalone and exits non-zero on any failed assertion so it gates the build beside decide /
 * customs / scenario / autonomy:
 *   npx tsx src/showrunner/prologue.selftest.ts   (or: npm run showrunner:test:prologue)
 */
import {
  decidePrologue,
  measureOverwhelmingSignal,
  type PrologueInput,
  type CustomTally,
} from './prologue.js';

let failures = 0;
function check(label: string, cond: boolean): void {
  if (cond) console.log(`  ok   ${label}`);
  else { failures += 1; console.error(`  FAIL ${label}`); }
}

function inp(over: Partial<PrologueInput> = {}): PrologueInput {
  return { ignited: false, acked: false, overwhelmingSignal: false, signalName: null, ...over };
}

// ===========================================================================
// decidePrologue — the ignition sequence + one-shot ack + precision gate.
// ===========================================================================
{
  // 0. DORMANT: not ignited → curatorial suppressed, no ack, un-/named key irrelevant to the gate.
  const dormant = decidePrologue(inp({ overwhelmingSignal: true, signalName: 'brann' }));
  check('dormant: not ignited → suppressed, no ack, step=dormant',
    !dormant.curatorialAllowed && !dormant.postAck && dormant.step === 'dormant');

  // 1. IGNITED: ignited + not yet acked → post the ONE-SHOT ack; curatorial unlocks.
  const ignite = decidePrologue(inp({ ignited: true, overwhelmingSignal: true, signalName: 'brann' }));
  check('ignited: not yet acked → postAck true + curatorial unlocked + step=ignited',
    ignite.postAck && ignite.curatorialAllowed && ignite.step === 'ignited');
  check('ignited: overwhelming + name → NAMED report key',
    ignite.reportVoiceKey === 'recordOpenedNamed');

  // 2. ACKNOWLEDGED: ignited + acked → steady state, never re-post.
  const acked = decidePrologue(inp({ ignited: true, acked: true, overwhelmingSignal: true, signalName: 'brann' }));
  check('acknowledged: already acked → no re-post, step=acknowledged',
    !acked.postAck && acked.curatorialAllowed && acked.step === 'acknowledged');

  // PRECISION GATE: no overwhelming signal → un-named FACT-1 fallback (never a guess).
  const weak = decidePrologue(inp({ ignited: true, overwhelmingSignal: false, signalName: null }));
  check('precision: no overwhelming signal → un-named fallback key', weak.reportVoiceKey === 'recordOpened');
  const noName = decidePrologue(inp({ ignited: true, overwhelmingSignal: true, signalName: null }));
  check('precision: overwhelming but no name → still un-named fallback', noName.reportVoiceKey === 'recordOpened');

  // IDEMPOTENT ACK: only the ignited-not-acked tick sets postAck; the two neighbors do not.
  check('idempotent: exactly one postAck across dormant/ignited/acked',
    [dormant.postAck, ignite.postAck, acked.postAck].filter(Boolean).length === 1);

  // DETERMINISTIC.
  check('decide: deterministic',
    JSON.stringify(decidePrologue(inp({ ignited: true, overwhelmingSignal: true, signalName: 'brann' }))) ===
    JSON.stringify(decidePrologue(inp({ ignited: true, overwhelmingSignal: true, signalName: 'brann' }))));
}

// ===========================================================================
// measureOverwhelmingSignal — the grounded precision input (dominant-by-margin, or a safe null).
// ===========================================================================
function tally(over: Partial<CustomTally> = {}): CustomTally {
  return { groupKey: 'u1', customKey: 'the_offering', name: 'brann', honoredCount: 4, violatedCount: 0, ...over };
}
{
  // OVERWHELMING: one habit clears the floor AND the margin over the field → nameable.
  const strong = measureOverwhelmingSignal([
    tally({ groupKey: 'u1', name: 'brann', violatedCount: 6, honoredCount: 4 }),
    tally({ groupKey: 'u2', name: 'vaun', customKey: 'the_bow', violatedCount: 1 }),
  ]);
  check('signal: dominant-by-margin → overwhelming + names the player',
    strong.overwhelmingSignal && strong.signalName === 'brann' && strong.groupKey === 'u1' && strong.honoredCount === 4);

  // FAINT: the top is below the floor → no name.
  const faint = measureOverwhelmingSignal([tally({ violatedCount: 1 })]);
  check('signal: below floor → not overwhelming (no faint callout)',
    !faint.overwhelmingSignal && faint.signalName === null);

  // TIE: two tallies within the margin → no confident signal.
  const tie = measureOverwhelmingSignal([
    tally({ groupKey: 'u1', name: 'brann', violatedCount: 5 }),
    tally({ groupKey: 'u2', name: 'vaun', customKey: 'the_bow', violatedCount: 4 }),
  ]);
  check('signal: within-margin tie → not overwhelming (never a coin-flip)', !tie.overwhelmingSignal);

  // NAMELESS: the dominant tally has no name → never named.
  const nameless = measureOverwhelmingSignal([tally({ name: null, violatedCount: 9 })]);
  check('signal: nameless dominant → no name (precision)', !nameless.overwhelmingSignal && nameless.signalName === null);

  // EMPTY: no tallies → safe null.
  check('signal: empty field → not overwhelming', !measureOverwhelmingSignal([]).overwhelmingSignal);

  // DETERMINISTIC.
  const field: CustomTally[] = [
    tally({ groupKey: 'u1', name: 'brann', violatedCount: 6 }),
    tally({ groupKey: 'u2', name: 'vaun', customKey: 'the_bow', violatedCount: 1 }),
  ];
  check('signal: deterministic',
    JSON.stringify(measureOverwhelmingSignal(field)) === JSON.stringify(measureOverwhelmingSignal(field)));
}

// ===========================================================================
// END-TO-END: the measurement feeds the decider — overwhelming → named; flat → un-named.
// ===========================================================================
{
  const overwhelming = measureOverwhelmingSignal([
    tally({ groupKey: 'u1', name: 'brann', violatedCount: 6 }),
    tally({ groupKey: 'u2', name: 'vaun', customKey: 'the_bow', violatedCount: 1 }),
  ]);
  const named = decidePrologue(inp({ ignited: true, overwhelmingSignal: overwhelming.overwhelmingSignal, signalName: overwhelming.signalName }));
  check('e2e: measured overwhelming → decider selects the named key', named.reportVoiceKey === 'recordOpenedNamed' && named.postAck);

  const flatSig = measureOverwhelmingSignal([
    tally({ groupKey: 'u1', name: 'brann', violatedCount: 3 }),
    tally({ groupKey: 'u2', name: 'vaun', customKey: 'the_bow', violatedCount: 3 }),
  ]);
  const unnamed = decidePrologue(inp({ ignited: true, overwhelmingSignal: flatSig.overwhelmingSignal, signalName: flatSig.signalName }));
  check('e2e: measured flat field → decider degrades to the un-named fallback', unnamed.reportVoiceKey === 'recordOpened');
}

if (failures > 0) {
  console.error(`\nshowrunner prologue: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner prologue: OK — the cold-start ignition spine holds.');
