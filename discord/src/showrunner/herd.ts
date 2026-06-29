/**
 * herd.ts — the slow herd-conversion pacer (A12 `herd-conversion`, INV-13, WEB-MASTER §7).
 *
 * "the pale field climbs between sessions." A purely COSMETIC, capped, between-session spread: a few
 * more Pale animals appear each movement, all facing one way (FACT 15 visual). This is the pure pacer
 * that computes the target pale count for the next session — never tracked, never glowing, never
 * breeding/converting on-screen (all CUT). Discord NEVER announces "the herd grows" (no step-ladder);
 * the marquee leans on ORIENTATION (formation), not number.
 *
 * INV-13 (THE PRECISION GUARD). Only the ONE true Sacred Beast (first PDC tag) is conduct-tracked and
 * the only one that glows; the cosmetic Pale (`pale_cosmetic` PDC) NEVER glow and are NEVER a
 * violation. This pacer emits a `pale_cosmetic` target ONLY — it can never arm the Sacred-Beast fork
 * (that needs the glowing beast, forks.ts). So the irreversible fork stays fairly avoidable.
 *
 * CAPPED + MONOTONE + UNWITNESSED (anti-jank). The target only ever rises (a converted animal doesn't
 * un-convert), is capped (default 16), adds at most one-per-pass, and the plugin places them out of
 * line of sight (reveal discipline). Idempotent: same movement + same prior count → same target.
 *
 * PURE. No DB / clock / LLM. The marquee uses the EXISTING `tollSacredBeast`/`keptSacredBeast` voice
 * (no new key). The deterministic schedule IS the backstop. herd.run.ts reads the prior count + fires
 * the SacredAnimalBeat `mode:"spread"` pass; herd.selftest.ts imports this with nothing.
 */

export interface HerdConstants {
  /** the hard cap on the pale field (FACT 15 visual ceiling). */
  capCount: number;
  /** how many pale to add per between-session pass (one-per-pass, capped). */
  addPerPass: number;
  /** the movement at which the conversion begins (M1 single beast; spread from M2+). */
  startMovement: number;
}

export const HERD_DEFAULTS: HerdConstants = {
  capCount: 16,
  addPerPass: 1,
  startMovement: 2,
};

export interface HerdInput {
  /** the current arc movement (1..5). */
  movement: number;
  /** the prior pale-cosmetic count (persisted high-water). */
  priorPaleCount: number;
  /** has THIS pass already run for this movement window? (idempotency). */
  passDoneThisWindow: boolean;
}

export interface HerdDecision {
  /** the target pale-cosmetic count to dress to (monotone, capped). */
  paleTarget: number;
  /** how many to add this pass (0 when capped, not started, or already done this window). */
  addThisPass: number;
  /** true when a spread pass should be enqueued this tick. */
  spread: boolean;
  reason: string;
}

/**
 * paceHerd — pure, monotone, capped. Same input → same output. Adds at most `addPerPass`, never
 * exceeds `capCount`, never starts before `startMovement`, and never re-adds within a window.
 */
export function paceHerd(inp: HerdInput, k: HerdConstants = HERD_DEFAULTS): HerdDecision {
  const prior = Math.max(0, Math.min(inp.priorPaleCount, k.capCount));

  if (inp.movement < k.startMovement) {
    return { paleTarget: prior, addThisPass: 0, spread: false, reason: `movement ${inp.movement} < start ${k.startMovement} — single beast only` };
  }
  if (inp.passDoneThisWindow) {
    return { paleTarget: prior, addThisPass: 0, spread: false, reason: 'spread pass already done this window' };
  }
  if (prior >= k.capCount) {
    return { paleTarget: k.capCount, addThisPass: 0, spread: false, reason: `pale field at cap (${k.capCount})` };
  }

  const add = Math.min(k.addPerPass, k.capCount - prior);
  const target = prior + add;
  return { paleTarget: target, addThisPass: add, spread: add > 0, reason: `spread +${add} pale (cosmetic, never glowing) → ${target}/${k.capCount}` };
}
