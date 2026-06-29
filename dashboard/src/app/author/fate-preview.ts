// fate-preview.ts — a verbatim mirror of the engine's pure ending selector, for the dashboard.
//
// THE GAP THIS CLOSES. The EndingSelector (a Client Component) must PREVIEW which of the five
// colorants the live arc resolves to, with NO server round-trip and NO DB. The single source of
// truth is `discord/src/showrunner/fate.ts::decideFate` — but that lives in a separate package
// (ESM `.js` imports, its own tsconfig) and is server-shaped, so it cannot be imported into a
// dashboard client bundle. This is its exact, dependency-free mirror.
//
// CONSISTENCY LAW. This MUST stay byte-for-byte equivalent to `decideFate` (same precedence, same
// "dominates" rule, same enum). If fate.ts changes, change this in lockstep — `fate-preview.selftest.ts`
// pins the shared cases the engine's `autonomy.selftest.ts` also pins, so a divergence fails the build.
//
// THE LAWS THIS HONORS (identical to fate.ts):
//   - INV-11: active-only counts, returns an enum, names no player; bond/Whisper tally is not a field.
//   - PRECISION (REFUSERS): a positive measured defiance signal only, never mere absence.
//   - PURE + DETERMINISTIC: same inputs → same fate; no DB, no clock, no LLM.

/** The four base fates the M5 composer can open with (INV-11 enum). Mirrors fate.ts EndingFate. */
export type EndingFate = "kept" | "cast_out" | "divided" | "refusers";

/** Immutable, active-only input. Mirrors fate.ts FateInput exactly (bond tally absent by design). */
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
  /** a POSITIVE, measured defiance signal — never "quorum && !bowed" (precision). default false. */
  refusalSignal: boolean;
}

/** What the selector returns: the base fate + a human-readable trace (never player-facing). */
export interface FateDecision {
  fate: EndingFate;
  reason: string;
}

/**
 * previewFate — the pure ending selector, mirroring `decideFate`. Fixed precedence:
 *   1. REFUSERS (secret) — quorum present AND a positive defiance signal.
 *   2. KEPT — honored dominates AND (seventh found OR iss caught) AND quorum met.
 *   3. CAST_OUT — violated dominates AND a real leaving (>= 2 reached LEFT_AT).
 *   4. DIVIDED — the floor: a real spread, or any arc that earned neither pole.
 */
export function previewFate(inp: FateInput): FateDecision {
  const decided = inp.honoredActive + inp.violatedActive;
  const honoredDominates = decided > 0 && inp.honoredActive * 2 > decided;
  const violatedDominates = decided > 0 && inp.violatedActive * 2 > decided;

  // 1. REFUSERS — a positive defiance, never mere absence (precision; WEB-MASTER §8).
  if (inp.quorumMet && inp.refusalSignal) {
    return {
      fate: "refusers",
      reason: "quorum present + positive defiance signal; bow window empty",
    };
  }

  // 2. KEPT — honored-dominant + a spine payoff + full quorum.
  if (honoredDominates && (inp.seventhFound || inp.issCaught) && inp.quorumMet) {
    return {
      fate: "kept",
      reason: `honored dominates (${inp.honoredActive}/${decided}) + ${
        inp.seventhFound ? "seventh" : "iss"
      } + quorum`,
    };
  }

  // 3. CAST_OUT — violated-dominant + a real leaving.
  if (violatedDominates && inp.leftAtActive >= 2) {
    return {
      fate: "cast_out",
      reason: `violated dominates (${inp.violatedActive}/${decided}) + ${inp.leftAtActive} left at the threshold`,
    };
  }

  // 4. DIVIDED — the floor: a real spread, or any arc that earned neither pole.
  return {
    fate: "divided",
    reason:
      decided === 0
        ? "no measured compliance spread — the land holds on neither side"
        : `a real honored/violated spread (${inp.honoredActive} kept / ${inp.violatedActive} not)`,
  };
}

/**
 * coerceFate — validate an untrusted string against the enum (the server override re-check). Returns
 * the fate or null. Keeps the four-value set in ONE place so the action and the component agree.
 */
export function coerceFate(raw: unknown): EndingFate | null {
  return raw === "kept" || raw === "cast_out" || raw === "divided" || raw === "refusers"
    ? raw
    : null;
}
