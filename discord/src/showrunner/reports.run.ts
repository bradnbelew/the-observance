/**
 * reports.run.ts — the I/O wrapper that makes the Tier-0 "it knows you" observation loop FIRE (W3).
 *
 * The pure policy (reports.ts / decidePersonalizedReports) was built + self-tested but INERT: nothing
 * read the plugin's measured behavior, computed group-relative habit scores, or delivered a report. This
 * closes that seam, mirroring theory.run.ts / the customs bridge:
 *   1. read the plugin-written `dossiers` (measured per-player behavior) + `custom_compliance` (days kept),
 *   2. compute each player's 0..1 GROUP-RELATIVE habit scores (buildObservationDossiers — pure),
 *   3. run the precision-gated policy (a flat/tied dossier names no one — a wrong "it knows you" is worse
 *      than none),
 *   4. post each fired report's deterministic Watcher line to #the-record + advance the per-player
 *      dominant-habit high-water (state.reported_habits) so the same habit never re-drips.
 *
 * GROUNDING (INV): a player is named ONLY for a genuinely measured, dominant habit; the line text is
 * voice-sourced (reports.ts → voice.reportObserved), never composed here. Fault-isolated + graceful: any
 * read failure yields no reports (silence-is-canon). Tier-0 is deterministic — no LLM; the authored line
 * is the whole line. Post-reckoning the sharp NAMED lane changes (condemn/free → quiet; understand →
 * kept-true), read from arc_state.flags and passed to the policy.
 *
 * AXES SCORED (v1): the three cleanly-measured chorus axes present in the dossier — hoards (hoarded_score),
 * wanders (distance_from_group), spends-words (forbidden_word_hits). reads / silent / night-walks need
 * signals not yet in the flushed dossier (blocks-broken, uncrouched-markers, black-moon activity); when
 * those land in `dossiers.extra`, add them here (the policy already knows all six axes).
 */
import { getArcFlags, readCustomViolations, readDossiers } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord } from './discord.js';
import { decidePersonalizedReports, buildObservationDossiers, type ReckoningShift } from './reports.js';
import { logEvent } from '../db/repo.js';
import type { Tone } from './types.js';

/** Read the post-reckoning sharp-quote shift from the arc flags (condemn/free/understand), else null. */
function reckoningShiftFromFlags(flags: Record<string, unknown>): ReckoningShift {
  if (flags.reckoning_condemn === true) return 'condemn';
  if (flags.reckoning_free === true) return 'free';
  if (flags.reckoning_understand === true) return 'understand';
  return null;
}

/**
 * runReportsPass — one Tier-0 observation tick. Reads measured state, scores it group-relative, runs the
 * pure policy, posts each fired report to #the-record, and advances the idempotency high-water. Returns a
 * small tally for the tick log. No group (< 2 dossiers) → no-op (a habit can only be dominant relative to
 * a group). Fully fault-isolated by the caller; also guards its own reads.
 */
export async function runReportsPass(mode: 'auto' | 'confirm', tone?: Tone): Promise<{ reported: number; staged: number }> {
  const rows = await readDossiers();
  if (rows.length < 2) return { reported: 0, staged: 0 }; // relative-to-the-group needs a group

  const violations = await readCustomViolations();
  const honored = new Map<string, number>();
  for (const v of violations) honored.set(`${v.groupKey}:${v.customKey}`, v.honoredCount);

  const flags = await getArcFlags();
  const state = await readState();
  const dossiers = buildObservationDossiers(rows, honored);

  const decision = decidePersonalizedReports({
    dossiers,
    reported: state.reported_habits ?? {},
    mode,
    tone,
    reckoningShift: reckoningShiftFromFlags(flags),
  });

  let posted = 0;
  let staged = 0;
  const newMarks: Record<string, number> = {};
  for (const r of decision.reports) {
    if (r.staged) { staged += 1; continue; } // CONFIRM mode: awaits dashboard approval — not posted live
    // Tier-0 is deterministic (no LLM): r.line is the whole authored observation (the fallback floor).
    const ok = await postToTheRecord(r.line);
    if (ok) {
      posted += 1;
      // Advance the high-water ONLY for a report that actually landed (a failed post retries next tick).
      if (decision.marks[r.groupKey] != null) newMarks[r.groupKey] = decision.marks[r.groupKey]!;
      await logEvent('info', 'showrunner.reports', `observed: ${r.name} — ${r.habit}`);
    }
  }

  if (Object.keys(newMarks).length > 0) {
    state.reported_habits = { ...(state.reported_habits ?? {}), ...newMarks };
    await writeState(state, new Date().toISOString());
  }
  return { reported: posted, staged };
}
