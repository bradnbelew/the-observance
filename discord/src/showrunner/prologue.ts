/**
 * prologue.ts — the cold-start prologue sequencer (B4 `cold-start-prologue`, WEB-MASTER §1.M1).
 *
 * "the one P0 vertical-slice ignition idea — without ignition, nothing else gets tested." This is the
 * pure ordering policy for the ignition handshake: nothing curatorial happens until the prologue is
 * IGNITED (a human read the lectern OR posted in #the-record). decide.ts already suppresses the
 * curatorial drip before ignition (the guard below is the pure form of that rule + the one-shot ack).
 *
 * THE SEQUENCE (each step gated on the prior; idempotent; never replays):
 *   0. dormant       — the first report is re-staged at the base hot-cell + one lit marker (plugin
 *                      side). No curatorial drip. Gifts still apply (player-helpful, never gated).
 *   1. ignited       — a DETECTED signal flips `prologue_ignited`. The ONE-SHOT `recordOpened` ack
 *                      fires exactly once. Curatorial drips unlock from here.
 *   2. acknowledged  — the ack has been posted; steady state. No further prologue action.
 *
 * THE NAMED-REPORT PRECISION GATE. The prologue names a real measured habit ONLY iff one player's
 * signal is OVERWHELMING (a single dominant signal); otherwise it falls back to the un-named FACT-1
 * report. A wrong "it knows you" is worse than none (the privacy precision law) — so the named form
 * requires an explicit `overwhelmingSignal` flag the caller only sets on a confident measurement.
 *
 * PURE + DETERMINISTIC, NO LLM. The report TEXT is an authored voice key selection (named vs un-named
 * FACT-1 fallback) — there is no language to author, the selection IS the fallback. snapshot exposes
 * `prologueIgnited`; this returns what the tick should do about the prologue. prologue.selftest.ts
 * imports it with nothing.
 */

export type PrologueStep = 'dormant' | 'ignited' | 'acknowledged';

/** Immutable input: the detected ignition state + whether the one-shot ack has already posted. */
export interface PrologueInput {
  /** arc_state.flags.prologue_ignited — set by IgnitionListener (lectern read OR #the-record post). */
  ignited: boolean;
  /** has the `recordOpened` ack already been posted once? (one-shot guard). */
  acked: boolean;
  /**
   * is there a single OVERWHELMING measured signal to name? (the precision gate). When true the
   * prologue may name the habit; when false it uses the un-named FACT-1 fallback. NEVER a guess.
   */
  overwhelmingSignal: boolean;
  /** the resolvable name behind the overwhelming signal, or null. A null name forces the fallback. */
  signalName: string | null;
}

export interface PrologueDecision {
  step: PrologueStep;
  /** true on the tick the one-shot `recordOpened` ack should post (exactly once, at ignition). */
  postAck: boolean;
  /** whether the curatorial clue-drip is permitted this tick (false until ignited). */
  curatorialAllowed: boolean;
  /**
   * the authored voice key for the ignition report: the NAMED form only when the precision gate is
   * met (overwhelming + a name), else the un-named FACT-1 fallback. A KEY, never composed text.
   */
  reportVoiceKey: 'recordOpenedNamed' | 'recordOpened';
  reason: string;
}

/**
 * decidePrologue — the pure ignition policy. Same input → same output.
 *   - not ignited           → dormant; curatorial drip suppressed; no ack.
 *   - ignited + not yet acked→ ignited; post the one-shot ack; curatorial unlocked.
 *   - ignited + acked        → acknowledged; steady state; curatorial unlocked.
 * The report voice key is the named form ONLY when the precision gate passes.
 */
export function decidePrologue(inp: PrologueInput): PrologueDecision {
  const named = inp.overwhelmingSignal && !!inp.signalName;
  const reportVoiceKey = named ? 'recordOpenedNamed' : 'recordOpened';

  if (!inp.ignited) {
    return {
      step: 'dormant',
      postAck: false,
      curatorialAllowed: false,
      reportVoiceKey,
      reason: 'prologue not ignited — curatorial drip suppressed; gifts still apply',
    };
  }
  if (!inp.acked) {
    return {
      step: 'ignited',
      postAck: true, // the one-shot ack fires exactly here
      curatorialAllowed: true,
      reportVoiceKey,
      reason: named
        ? `ignited — naming the overwhelming signal (${inp.signalName})`
        : 'ignited — un-named FACT-1 fallback (no single overwhelming signal)',
    };
  }
  return {
    step: 'acknowledged',
    postAck: false,
    curatorialAllowed: true,
    reportVoiceKey,
    reason: 'prologue acknowledged — steady state',
  };
}
