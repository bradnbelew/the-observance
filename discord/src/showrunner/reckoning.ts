/**
 * reckoning.ts — dynamic diegetic difficulty (A10 `dynamic-diegetic-difficulty`, FACT 2b, INV-15).
 *
 * "the land's grip is not fixed: it closes on those who run ahead, opens for those who stumble."
 * The land grades the group's MASTERY and answers in three diegetic dials — never a difficulty
 * slider, never a number the player sees:
 *   - cadence: a group crushing the ciphers finds the next drip WITHHELD longer (the land waits);
 *              a group that stumbles gets it sooner (the land relents). `cadenceMult` on dripIntervalMs.
 *   - register temperature (`tone`): the Watcher's lines run colder when the group races, warmer
 *              (more relenting) when it struggles. A SELECTION among authored voice variants, never
 *              generated text.
 *   - herring density: a confident group meets one more staged dead-end; a struggling group meets
 *              fewer. `staged dead_end` rows flip active in apply.ts (Discord-side; no MC write).
 *
 * INV-15 — THE CUT KNOB. The difficulty engine NEVER touches the Whisper backstop. It withholds
 * drips and cools register; it never removes the player-controlled safety rail. There is NO read or
 * write of `whisper_budgets` anywhere in this module — a grep for it is a build guard (the self-test
 * asserts the source is clean), and the FateInput-style purity makes a regression visible.
 *
 * PURE + DETERMINISTIC, NO LLM. The state machine is a pure function of measured mastery + the prior
 * state (hysteresis, so it doesn't flap tick-to-tick). The `tone` it returns selects an authored
 * voice variant — there is no language to author here, so the deterministic selection IS the
 * fallback. Constants are injected so the policy stays tunable + unit-testable (reckoning.selftest.ts).
 *
 * GROUP-SCALAR ONLY. Mastery is a single group number (distinct solvers, first-try rate, whisper
 * lean) — NEVER a per-player rank (INV-16: nothing here can derive WHICH player is strong/weak).
 *
 * THE PLANT/PAYOFF (WEB-MASTER §9 #12). Mara's bookCipher "closer count of the quick" is the M1
 * plant; the M5 backward-read is a re-quote the M5 COMPOSER emits (this module only exposes the
 * state it re-quotes from — `state` — it does not write the M5 line).
 */

/** The three grip states. `even` is the neutral baseline; `tight`/`loose` are the two poles. */
export type ReckoningState = 'tight' | 'even' | 'loose';

/** The register temperature the Watcher speaks in — a SELECTION among authored variants. */
export type Tone = 'cold' | 'plain' | 'warm';

/** Tunable constants, injected so the pure policy stays testable + adjustable without code edits. */
export interface ReckoningConstants {
  /** first-try solve rate (0..1) at/above which the group is "running ahead" → tightens. */
  tightenFirstTryRate: number;
  /** first-try solve rate (0..1) at/below which the group is "stumbling" → loosens. */
  loosenFirstTryRate: number;
  /** whisper-lean (avg whispers spent per solve) at/above which the group is struggling → loosens. */
  loosenWhisperLean: number;
  /** hysteresis: ms a state must persist before a new reading may flip it (prevents flapping). */
  hysteresisMs: number;
  /** drip cadence multiplier when tight (the land waits longer). > 1. */
  tightCadenceMult: number;
  /** drip cadence multiplier when loose (the land relents sooner). < 1. */
  looseCadenceMult: number;
}

export const RECKONING_DEFAULTS: ReckoningConstants = {
  tightenFirstTryRate: 0.6,
  loosenFirstTryRate: 0.25,
  loosenWhisperLean: 1.5,
  hysteresisMs: 18 * 3_600_000, // ~one drip cadence; a state holds at least this long
  tightCadenceMult: 1.5,
  looseCadenceMult: 0.6,
};

/** The measured, group-scalar mastery signals + the prior state (for hysteresis). All active-only. */
export interface ReckoningInput {
  nowMs: number;
  /** distinct active players who solved at least one node in the window (engagement breadth). */
  distinctSolvers: number;
  /** group first-try solve rate in the window (0..1): solves with no prior failed attempt / solves. */
  firstTryRate: number;
  /** avg whispers leaned on per solve in the window (the struggle tell). */
  whisperLean: number;
  /** the prior persisted state, or null on a fresh arc. */
  priorState: ReckoningState | null;
  /** epoch ms the prior state was entered (hysteresis anchor); null on a fresh arc. */
  priorSinceMs: number | null;
}

/** What the reckoning returns: the grip state, the cadence dial, the register, and the hysteresis stamp. */
export interface ReckoningResult {
  state: ReckoningState;
  /** multiply dripIntervalMs by this (1 when even). decide.ts applies it in one pure line. */
  cadenceMult: number;
  /** the register temperature for this tick's Watcher lines (a voice-variant selection). */
  tone: Tone;
  /** epoch ms this state was (re-)entered — persisted so the next tick can apply hysteresis. */
  sinceMs: number;
  reason: string;
}

/** The raw reading from the mastery signals alone, before hysteresis. */
function readGrip(inp: ReckoningInput, k: ReckoningConstants): ReckoningState {
  // Struggle wins ties toward LOOSE (the decency floor: relent for a stumbling group before you
  // tighten on a fast one). A group leaning on whispers is struggling even if its first-try looks ok.
  if (inp.firstTryRate <= k.loosenFirstTryRate || inp.whisperLean >= k.loosenWhisperLean) return 'loose';
  // Only a group both fast AND not whisper-leaning is "running ahead".
  if (inp.firstTryRate >= k.tightenFirstTryRate && inp.whisperLean < k.loosenWhisperLean) return 'tight';
  return 'even';
}

const TONE_FOR: Readonly<Record<ReckoningState, Tone>> = { tight: 'cold', even: 'plain', loose: 'warm' };
const MULT_FOR = (s: ReckoningState, k: ReckoningConstants): number =>
  s === 'tight' ? k.tightCadenceMult : s === 'loose' ? k.looseCadenceMult : 1;

/**
 * reckon — the pure difficulty policy. Reads mastery, applies HYSTERESIS against the prior state
 * (a new reading may only flip the state once the prior has held `hysteresisMs`), and returns the
 * cadence dial + register. Same input → same output. NEVER touches whisper_budgets (INV-15).
 */
export function reckon(inp: ReckoningInput, k: ReckoningConstants = RECKONING_DEFAULTS): ReckoningResult {
  const raw = readGrip(inp, k);

  // Hysteresis: hold the prior state until it has aged past hysteresisMs, then allow the flip.
  let state = raw;
  let sinceMs = inp.nowMs;
  let reason = `mastery → ${raw} (firstTry=${inp.firstTryRate.toFixed(2)}, whisperLean=${inp.whisperLean.toFixed(2)})`;

  if (inp.priorState != null && inp.priorSinceMs != null) {
    const aged = inp.nowMs - inp.priorSinceMs >= k.hysteresisMs;
    if (raw === inp.priorState) {
      // unchanged → keep the original entry time (don't reset the clock).
      state = inp.priorState;
      sinceMs = inp.priorSinceMs;
      reason = `held ${state} (reading unchanged)`;
    } else if (!aged) {
      // a different reading, but the prior state hasn't aged enough → suppress the flip (no flapping).
      state = inp.priorState;
      sinceMs = inp.priorSinceMs;
      reason = `suppressed flip ${inp.priorState}→${raw} (held only ${((inp.nowMs - inp.priorSinceMs) / 3_600_000).toFixed(1)}h < hysteresis)`;
    } else {
      // aged + a new reading → flip, stamp now.
      reason = `flip ${inp.priorState}→${raw} (aged past hysteresis)`;
    }
  }

  return { state, cadenceMult: MULT_FOR(state, k), tone: TONE_FOR[state], sinceMs, reason };
}
