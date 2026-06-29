/**
 * fate.ts — the divergent-ending selector (A2 `divergent-fates`, WEB-MASTER §5/§8, INV-11).
 *
 * THE GAP THIS CLOSES. The Accepting rite fires ONE opaque bow token (AcceptingRiteListener,
 * unchanged). Something must read the measured arc and pick which of the five ending colorants
 * the M5 composer (WEB-MASTER §5) opens with. This is that pure decision — and ONLY the decision:
 * it returns an enum + a codicil boolean, never an M5 sentence (the composer owns prose).
 *
 * PURE + DETERMINISTIC. Same inputs → same fate. No DB, no clock, no LLM (there is no language
 * here to author — a fate is an enum, so there is nothing to "fall back" to; the determinism IS
 * the backstop). The I/O that reads the spread + writes `arc_state.ending_fate` lives in the
 * resolve.ts fate-sentinel branch (TS-SHOWRUN owns; set-once, idempotent) — this file is the
 * policy it calls, importable by fate.selftest.ts with no DB.
 *
 * THE FIVE ENDINGS (WEB-MASTER §8):
 *   - KEPT     = high honored ratio + (seventhFound OR issCaught) + full quorum.
 *   - CAST_OUT = violated dominates + >= 2 LEFT_AT keepers' worth of leaving.
 *   - DIVIDED  = a real honored/violated spread (the default when neither pole dominates).
 *   - REFUSERS = SECRET: quorum present + a POSITIVE defiance signal + the bow window empty.
 *   - INHERITORS = a CODICIL (boolean), not a base fate: the Seventh restore/deposit act.
 *
 * THE LAWS THIS HONORS:
 *   - INV-11: reads measured group tallies over ACTIVE players ONLY; returns an enum; names no
 *     player; the bond/Whisper tally is NOT an input (excluded by construction — it is not a field
 *     on FateInput).
 *   - INV-16: a fate is a group enum; it can never derive WHICH player is on which side. DIVIDED is
 *     resolved here as a group state; the floor split is by geometry, dressed downstream, never by
 *     player.
 *   - PRECISION over recall (REFUSERS): a slow/absent group is NEVER read as refusing. REFUSERS
 *     requires a POSITIVE defiance signal, never mere absence (`refusalSignal` is an explicit
 *     measured flag, not "quorum && !bowed").
 *   - COLLECTIVE, never punish for an absent member: every ratio is over ACTIVE players only
 *     (the caller passes active-only counts; this module never sees an absentee).
 */

/** The four base fates the M5 composer can open with (INV-11 enum). */
export type EndingFate = 'kept' | 'cast_out' | 'divided' | 'refusers';

/**
 * Immutable, active-only input to {@link decideFate}. Every count is over ACTIVE players (the
 * caller filters; this module never sees an absent member). The bond/Whisper tally is absent by
 * construction (INV-11) — there is no field for it, so it can never leak into the selector.
 */
export interface FateInput {
  /** active keepers whose measured custom-compliance is honored-dominant. */
  honoredActive: number;
  /** active keepers whose measured custom-compliance is violated-dominant. */
  violatedActive: number;
  /** active keepers who reached the LEFT_AT rung on a tracked custom (the cold turn). */
  leftAtActive: number;
  /** the Seventh was found + named (FACT 10b). a spine signal, not a fork. */
  seventhFound: boolean;
  /** the Iss lie was caught (`iss_caught`, the universal M4 hinge). */
  issCaught: boolean;
  /** the rite quorum (cast size) was met by active players. */
  quorumMet: boolean;
  /**
   * a POSITIVE, measured defiance signal (e.g. the cast assembled at the threshold and turned
   * away by an explicit act). NEVER "quorum && !bowed" — a slow group must never read as refusing.
   * Set ONLY by a plugin-detected refusal rite; default false.
   */
  refusalSignal: boolean;
}

/** What the selector returns: the base fate + the INHERITORS codicil (set independently upstream). */
export interface FateDecision {
  fate: EndingFate;
  /** human-readable trace (logged; never player-facing). */
  reason: string;
}

/**
 * decideFate — the pure ending selector. Fixed precedence so the result is total + testable:
 *   1. REFUSERS (secret) — quorum present AND a positive defiance signal AND no rite completion.
 *      (Checked first because it is the one fate keyed on a positive *non*-completion act; it can
 *      only be reached with a real refusal signal, never by slowness.)
 *   2. KEPT — honored dominates AND (seventh found OR iss caught) AND quorum met.
 *   3. CAST_OUT — violated dominates AND a real leaving (>= 2 reached LEFT_AT).
 *   4. DIVIDED — the floor of the enum: any real arc that is neither all-kept nor all-cast-out.
 *
 * "Dominates" is a strict majority of the active honored/violated split, so a dead-even or empty
 * arc lands on DIVIDED (the honest "they were neither" close), never on a pole it didn't earn.
 */
export function decideFate(inp: FateInput): FateDecision {
  const decided = inp.honoredActive + inp.violatedActive;
  const honoredDominates = decided > 0 && inp.honoredActive * 2 > decided;
  const violatedDominates = decided > 0 && inp.violatedActive * 2 > decided;

  // 1. REFUSERS — a positive defiance, never mere absence (precision; WEB-MASTER §8).
  if (inp.quorumMet && inp.refusalSignal) {
    return { fate: 'refusers', reason: 'quorum present + positive defiance signal; bow window empty' };
  }

  // 2. KEPT — honored-dominant + a spine payoff + full quorum.
  if (honoredDominates && (inp.seventhFound || inp.issCaught) && inp.quorumMet) {
    return {
      fate: 'kept',
      reason: `honored dominates (${inp.honoredActive}/${decided}) + ${inp.seventhFound ? 'seventh' : 'iss'} + quorum`,
    };
  }

  // 3. CAST_OUT — violated-dominant + a real leaving.
  if (violatedDominates && inp.leftAtActive >= 2) {
    return {
      fate: 'cast_out',
      reason: `violated dominates (${inp.violatedActive}/${decided}) + ${inp.leftAtActive} left at the threshold`,
    };
  }

  // 4. DIVIDED — the floor: a real spread, or any arc that earned neither pole.
  return {
    fate: 'divided',
    reason: decided === 0
      ? 'no measured compliance spread — the land holds on neither side'
      : `a real honored/violated spread (${inp.honoredActive} kept / ${inp.violatedActive} not)`,
  };
}
