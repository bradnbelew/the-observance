/**
 * clock.ts — the single Accepting-instant binder + the real-world-clock event (A13/A9, WEB-MASTER §0.4).
 *
 * THE ONE INSTANT (coherence P1-8). The grave's carved date, the Record website's encoded timestamp,
 * and the summons `not_before` are the SAME moment. No idea owns it — the showrunner binds it ONCE,
 * here, and everyone reads it. This module is the pure binder + the "is it time?" predicate.
 *
 * THE REAL-WORLD CLOCK (A13, CUT the astronomical new moon → encoded timestamp). The Accepting instant
 * is a real wall-clock moment in the near future, chosen deterministically from the binding context
 * (so a re-run binds the SAME instant, never drifts). Once bound it is immutable for the arc.
 *
 * `not_before` SEMANTICS (A9 build task). Beats/summons that must not fire before the instant carry a
 * `not_before` the showrunner respects: `instantReached(now)` is the single predicate the grave open,
 * the summons, and the website re-render all consult, so they can never disagree about whether it's time.
 *
 * PURE + DETERMINISTIC, NO LLM, IDEMPOTENT. Binding is a pure function of (already-bound?, a deterministic
 * future offset from a stable anchor). If already bound, it returns the existing instant unchanged
 * (idempotent — the set-once contract). clock.run.ts persists it to arc_state; clock.selftest.ts imports
 * this with nothing.
 */

/** Tunable: how far ahead the Accepting instant is set from the binding anchor (the arc's tail). */
export interface ClockConstants {
  /** ms from the binding anchor to the Accepting instant (the appointment window). */
  leadMs: number;
}

export const CLOCK_DEFAULTS: ClockConstants = {
  // ~Day 12→14 of a ~14-day arc: bound when the catch lands, the rite a couple days out.
  leadMs: 2 * 24 * 3_600_000,
};

export interface ClockBindInput {
  /** the instant already bound on arc_state, or null if never bound. */
  boundInstantMs: number | null;
  /**
   * the stable binding anchor (epoch ms) — e.g. the `iss_caught` timestamp, NOT `Date.now()`, so a
   * re-run binds the same instant. The run wrapper passes the persisted catch time.
   */
  anchorMs: number;
  /** is the arc at the point where the instant SHOULD be bound (e.g. iss_caught + threshold_open)? */
  readyToBind: boolean;
}

export interface ClockBindResult {
  /** the bound Accepting instant (epoch ms), or null if not yet ready to bind. */
  instantMs: number | null;
  /** true only on the tick this binder first set the instant (the run wrapper persists it then). */
  newlyBound: boolean;
  reason: string;
}

/**
 * bindAcceptingInstant — pure, set-once. If an instant is already bound, returns it unchanged
 * (idempotent). Else, when ready, binds anchor + leadMs deterministically. Same input → same output.
 */
export function bindAcceptingInstant(inp: ClockBindInput, k: ClockConstants = CLOCK_DEFAULTS): ClockBindResult {
  if (inp.boundInstantMs != null) {
    return { instantMs: inp.boundInstantMs, newlyBound: false, reason: 'instant already bound (set-once)' };
  }
  if (!inp.readyToBind) {
    return { instantMs: null, newlyBound: false, reason: 'not ready to bind the accepting instant yet' };
  }
  const instantMs = inp.anchorMs + k.leadMs;
  return { instantMs, newlyBound: true, reason: `bound accepting instant = anchor + ${(k.leadMs / 3_600_000).toFixed(0)}h` };
}

/**
 * instantReached — THE single `not_before` predicate. The grave open, the summons, and the website
 * re-render all consult this so they never disagree. False when the instant isn't bound (nothing is
 * "after" an unbound instant).
 */
export function instantReached(boundInstantMs: number | null, nowMs: number): boolean {
  return boundInstantMs != null && nowMs >= boundInstantMs;
}

/**
 * encodeTimestamp — the Record website's encoded timestamp form (A13: CUT live mutation; this is a
 * deterministic encoding of the SAME instant, computed at build time). Returns the instant as the
 * digit-glyph string the website + grave share. Pure; identical instant → identical encoding.
 *
 * The encoding is the in-world digit form: the epoch-day count and the day's quarter, joined by the
 * arc's separator — a navigation/recognition token, NEVER a coordinate answer (INV-14).
 */
export function encodeTimestamp(instantMs: number): string {
  const d = new Date(instantMs);
  // YYYY.DDD.q — year, day-of-year, quarter-of-day. Stable, in-world, and not a typeable answer.
  const start = Date.UTC(d.getUTCFullYear(), 0, 0);
  const dayOfYear = Math.floor((instantMs - start) / 86_400_000);
  const quarter = Math.floor(d.getUTCHours() / 6); // 0..3
  return `${d.getUTCFullYear()}.${String(dayOfYear).padStart(3, '0')}.${quarter}`;
}
