/**
 * observer.ts — the PURE policy for the Observer Tier-1 weaponizer ("it heard you say it", W4).
 *
 * Tier-0 (behavior — reports.ts) names a measured HABIT. Tier-1 echoes a real WORD: the record surfaces
 * one grounded, verbatim utterance the group actually said, sparingly, so the world feels like it was
 * listening. This is the deterministic tier — no LLM: it QUOTES what was literally captured, so it can
 * never fabricate (the LLM archivist, when configured, only makes the SELECTION smarter; it never writes
 * a quote). Same eligible-set + clock in → same decision out, so observer.selftest can pin it.
 *
 * THE DISCIPLINE (the mandate — grounded · sparse · degrade to silence, never fabricate):
 *   - GROUNDED: the caller passes only REAL un-used observations; this only picks one, never composes text.
 *   - SPARSE: nothing fires until minIntervalMs has passed since the last echo (rate-limited by construction).
 *   - PRECISE: it prefers a SUBSTANTIAL utterance (long enough to be a real thing said, not "ok") — a
 *     trivial or empty capture is never surfaced.
 *   - DEGRADE TO SILENCE: no eligible substantial utterance, or too soon → null (say nothing).
 * Consent (global switch + per-player opt-out) + presence are the CALLER's gates (observer.run.ts); this
 * pure core assumes the eligible list is already consented + named.
 */

/** The shortest utterance the weaponizer will echo — below this it is too trivial to be uncanny. */
export const MIN_QUOTE_LEN = 12;

/** One grounded captured utterance, already filtered to eligible (un-used, consented, named). */
export interface CapturedObservation {
  id: number;
  /** resolved display name — never null here (a nameless capture is filtered out by the caller). */
  name: string;
  /** the player's verbatim words — echoed as-is, never rewritten. */
  text: string;
  /** how it was captured — 'voice' echoes in the "heard aloud" register (it heard you SAY it). */
  source: 'discord' | 'chat' | 'voice';
  observedAtMs: number;
}

export interface WeaponizeDecision {
  /** the one observation to echo this pass, or null (too soon / nothing substantial). */
  observation: CapturedObservation | null;
  reason: string;
}

/**
 * decideWeaponization — choose at most ONE utterance to echo, or null. Rate-limited (minIntervalMs since
 * the last echo) and substance-gated (>= MIN_QUOTE_LEN). Deterministic pick: the most substantial
 * (longest) utterance, tie-broken by oldest id — never random, never trivial.
 */
export function decideWeaponization(
  eligible: readonly CapturedObservation[],
  nowMs: number,
  lastMs: number | null,
  minIntervalMs: number,
): WeaponizeDecision {
  if (lastMs != null && nowMs - lastMs < minIntervalMs) {
    return { observation: null, reason: 'too soon — the echo is deliberately rare' };
  }
  const substantial = eligible.filter((o) => o.text.trim().length >= MIN_QUOTE_LEN);
  if (substantial.length === 0) {
    return { observation: null, reason: 'no substantial un-used utterance' };
  }
  const chosen = [...substantial].sort(
    (a, b) => b.text.trim().length - a.text.trim().length || a.id - b.id,
  )[0]!;
  return { observation: chosen, reason: 'echo one grounded utterance' };
}
