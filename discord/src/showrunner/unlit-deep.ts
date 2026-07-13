/** Pure, deterministic reporting policy for both Unlit Deep outcomes (INV-17). */
import { voice } from '../voice.js';

export interface UnlitDeepInput {
  brokenAt: number | null;
  /** Private logging context only; never interpolated into the public line. */
  brokenBy: string | null;
  keptAt: number | null;
  lastBrokenReportedAt: number | null;
  lastKeptReportedAt: number | null;
}

export interface UnlitDeepDecision {
  kind: 'broken' | 'kept' | null;
  line: string | null;
  brokenMark: number | null;
  keptMark: number | null;
}

function fresh(value: number | null, mark: number | null): value is number {
  return value != null && Number.isFinite(value) && (mark == null || value > mark);
}

/**
 * Emits at most one report per tick. If downtime leaves both outcomes pending, the older event is
 * delivered first and the other remains pending for the next cadence. The actor is never spoken.
 */
export function decideUnlitDeepReport(input: UnlitDeepInput): UnlitDeepDecision {
  const brokenFresh = fresh(input.brokenAt, input.lastBrokenReportedAt);
  const keptFresh = fresh(input.keptAt, input.lastKeptReportedAt);
  if (!brokenFresh && !keptFresh) {
    return { kind: null, line: null, brokenMark: null, keptMark: null };
  }

  if (brokenFresh && (!keptFresh || input.brokenAt! <= input.keptAt!)) {
    return {
      kind: 'broken',
      line: voice.tollUnlitDeep(),
      brokenMark: input.brokenAt,
      keptMark: null,
    };
  }

  return {
    kind: 'kept',
    line: voice.keptUnlitDeep(),
    brokenMark: null,
    keptMark: input.keptAt,
  };
}
