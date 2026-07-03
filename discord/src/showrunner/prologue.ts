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

// ---------------------------------------------------------------------------
// THE OVERWHELMING-SIGNAL MEASUREMENT (the precision gate's input, §1.4/§2.2).
// Pure + grounded: given the per-player measured custom tallies, find the ONE player whose single
// violated habit is OVERWHELMING — dominant for the whole group by a confident margin. NULL when no
// such signal exists (a flat/tied field), so `decidePrologue` degrades safely to the un-named line.
// A wrong "it knows you" is worse than none, so this NEVER guesses: it names a player only when one
// measured habit stands clearly above all others across the group.
// ---------------------------------------------------------------------------

/** One measured (player, custom) tally — the projection prologue reads for the overwhelming signal. */
export interface CustomTally {
  groupKey: string;
  /** the custom_key, e.g. 'the_offering'. */
  customKey: string;
  /** resolvable display name, or null. A nameless player is never named (precision). */
  name: string | null;
  /** measured "days kept" the named line cites (the honored count on this custom). */
  honoredCount: number;
  /** measured violations on this custom — the signal strength driver. Only a >0 count is nameable. */
  violatedCount: number;
}

/** The overwhelming-signal result: a nameable single dominant habit, or a null-name flat field. */
export interface OverwhelmingSignal {
  overwhelmingSignal: boolean;
  signalName: string | null;
  /** the resolved (groupKey, customKey, honoredCount) behind the named signal, when overwhelming. */
  groupKey: string | null;
  customKey: string | null;
  honoredCount: number;
}

/** Tunable precision constants (injected; keeps the policy pure + testable). */
export interface SignalConstants {
  /** the dominant tally must reach at least this violated count to be nameable at all (no faint signal). */
  minViolated: number;
  /** the dominant must beat the runner-up violated count by at least this margin (a real habit, not a tie). */
  minMargin: number;
}

export const SIGNAL_DEFAULTS: SignalConstants = { minViolated: 3, minMargin: 2 };

/**
 * measureOverwhelmingSignal — the precision gate's grounded input. Selects the SINGLE (player, custom)
 * whose measured `violatedCount` is the group-wide maximum AND clears both the floor and the margin
 * over the next-highest DISTINCT tally, and whose player has a resolvable name. Returns a null-name,
 * `overwhelmingSignal: false` result on any flat/tied/nameless/empty field — the un-named fallback.
 * Deterministic: ties break to nothing (never a coin-flip callout). Same input → same output.
 */
export function measureOverwhelmingSignal(
  tallies: readonly CustomTally[],
  k: SignalConstants = SIGNAL_DEFAULTS,
): OverwhelmingSignal {
  const none: OverwhelmingSignal = {
    overwhelmingSignal: false, signalName: null, groupKey: null, customKey: null, honoredCount: 0,
  };
  // rank by violated count desc; deterministic tiebreak by (groupKey, customKey) so ordering is stable.
  const ranked = [...tallies]
    .filter((t) => Number.isFinite(t.violatedCount))
    .sort((a, b) =>
      b.violatedCount - a.violatedCount ||
      a.groupKey.localeCompare(b.groupKey) ||
      a.customKey.localeCompare(b.customKey));
  if (ranked.length === 0) return none;

  const top = ranked[0]!;
  if (!top.name) return none; // nameless → never named (precision)
  if (top.violatedCount < k.minViolated) return none; // too faint to name anyone

  // the runner-up is the next tally that is a DIFFERENT (player, custom) — the field it must dominate.
  const runnerUp = ranked.find((t) => t.groupKey !== top.groupKey || t.customKey !== top.customKey);
  const runnerUpViolated = runnerUp?.violatedCount ?? 0;
  if (top.violatedCount - runnerUpViolated < k.minMargin) return none; // a tie → no confident signal

  return {
    overwhelmingSignal: true,
    signalName: top.name,
    groupKey: top.groupKey,
    customKey: top.customKey,
    honoredCount: top.honoredCount,
  };
}
