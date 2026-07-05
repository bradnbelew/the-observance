/**
 * unlit-deep.ts — the pure policy for the Unlit Deep group latch's ONE report (INV-17's group latch,
 * design/LAUNCH-READINESS.md §5). Unlike the seven per-player customs (customs.ts's violated_count
 * ladder), this is GROUP-scoped and binary: UnlitDeepListener (the plugin) writes ONE flag,
 * `unlit_deep_broken_at`, the instant an explicit flame act breaks the latch below the deep line on a
 * taboo moon phase. This policy decides whether THIS tick should report a fresh break — idempotent on
 * a high-water mark (mirrors customs.ts's `reported` guard), so the same break is never re-posted.
 *
 * RECORDED, NOT SPOKEN: `voice.tollUnlitDeep()` never names who broke it (by design — see voice.ts and
 * UnlitDeepListener's own doc); `brokenBy` is carried here only for logging, never for the posted line.
 *
 * NOT YET BUILT (documented honestly, not half-shipped): the KEPT side (`voice.keptUnlitDeep()` — "no
 * flame carried below the line on the black moon"). It needs a per-black-moon-night idempotency key
 * (has THIS black moon already been reported kept or broken?) that no current signal cleanly supplies
 * from the Discord side; the line is authored and ready in voice.ts, awaiting that signal.
 */
import { voice } from '../voice.js';

export interface UnlitDeepInput {
  /** `arc_state.flags.unlit_deep_broken_at` (epoch ms), or null if absent/not a finite number. */
  brokenAt: number | null;
  /** `arc_state.flags.unlit_deep_broken_by` — carried for logging ONLY, never spoken in the report. */
  brokenBy: string | null;
  /** the prior high-water mark (state.unlit_deep_last_reported_at), or null if never reported. */
  lastReportedAt: number | null;
}

export interface UnlitDeepDecision {
  /** the report to post this tick, or null (nothing new / already reported — silence, INV-7). */
  line: string | null;
  /** the new high-water mark to persist; set only when `line` is non-null. */
  mark: number | null;
}

/**
 * decideUnlitDeepReport — fires ONLY on a break timestamp strictly newer than the last one reported.
 * Same input → same output; no DB/network/clock.
 */
export function decideUnlitDeepReport(input: UnlitDeepInput): UnlitDeepDecision {
  if (input.brokenAt == null || !Number.isFinite(input.brokenAt)) return { line: null, mark: null };
  if (input.lastReportedAt != null && input.brokenAt <= input.lastReportedAt) return { line: null, mark: null };
  return { line: voice.tollUnlitDeep(), mark: input.brokenAt };
}
