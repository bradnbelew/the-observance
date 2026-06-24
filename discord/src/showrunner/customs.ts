/**
 * customs.ts — the customs→report/consequence bridge (COHERENCE-AUDIT P0-4 / D1).
 *
 * THE GAP THIS CLOSES. The plugin detects seven customs and writes `custom_compliance`,
 * but until now NOTHING read it: a player who broke a custom never learned it. The report
 * VOICE already existed in register (`voice.reportObserved` / `voice.reportEscalated`, the
 * Orin observe→warn→left ladder of `observed-warned-left-at-threshold.md` / D04) — its only
 * caller was a README example. This pass is the missing consumer: on the showrunner's
 * cadence it reads measured violation counts and, for genuinely-crossed rungs, posts the
 * report and/or enqueues a SOFT, REVERSIBLE toll. One fix un-strands all seven customs.
 *
 * SHAPE (mirrors the spine): a PURE policy `decideCustomReports(input): CustomReportDecision`
 * (no DB / network / clock — fully unit-tested in customs.selftest.ts), wrapped by the
 * I/O pass `runCustomsPass()` that reads `custom_compliance` + showrunner state, runs the
 * policy, then posts / enqueues / persists. The pass is fault-isolated and graceful: if
 * Supabase is down `readCustomViolations()` returns [] and the pass simply does nothing.
 *
 * THE LADDER (observed → warned → left). Driven by the MEASURED `violated_count` only:
 *   - observed  (>= OBSERVE_AT, default 1): voice.reportObserved(name, kept, custom) — noted.
 *   - warned    (>= WARN_AT,    default 3): a soft reversible toll beat (the deep goes dark)
 *                                           + the observed report. matches the doc's "iii".
 *   - left      (>= LEFT_AT,    default 5): voice.reportEscalated(name) — the cold turn.
 *
 * PRECISION OVER RECALL. We report ONLY a measured violation (violated_count > 0) for a
 * player we can NAME (a resolvable display name); never a fabricated transgression
 * (canon-spine §6 rule 4, the grounding contract). A nameless row is skipped, not guessed.
 *
 * IDEMPOTENCY. A per-(player, custom) high-water mark of the highest violated_count already
 * reported (showrunner_state.reported_customs). A rung fires only when the measured count
 * has RISEN past that mark to cross a rung not yet reported — so the same violation is never
 * re-reported every cadence, and a restart mid-tick re-derives the same decision.
 *
 * AUTO ⇄ CONFIRM. The toll is a curatorial consequence: AUTO → enqueued 'approved' (fires on
 * the plugin's next poll); CONFIRM → 'pending' (waits for dashboard approval) — exactly the
 * spine's gate (INV-6). The report PROSE drips like any other Watcher line.
 *
 * TOLLS TAKE WARMTH, NOT PROGRESS (INV-8). The only consequence enqueued is a soft, private,
 * reversible atmosphere beat (doused light / cold) — never destroyed progress. Decency floor.
 *
 * MODULE SPLIT (mirrors decide.ts ⇄ apply.ts). This file holds ONLY the PURE policy +
 * its types, importing nothing with side effects (voice.ts is pure; the CustomViolation
 * import is type-only and fully erased) — so customs.selftest.ts can import it with no DB,
 * no Discord client, and no config/env validation. The I/O wrapper lives in customs.run.ts.
 */
import { voice, customPhrase } from '../voice.js';
import type { CustomViolation } from '../db/repo.js';

// --- rung thresholds (module constants; injected into the pure policy so it stays tunable) ---
/** violated_count at which the record first NOTES the lapse (observed). */
export const OBSERVE_AT = 1;
/** violated_count at which the soft toll lands + the lapse is named plainly (warned). "iii" in D04. */
export const WARN_AT = 3;
/** violated_count at which the soft-pressure turns cold (left at the threshold). */
export const LEFT_AT = 5;

/** The three rungs of Orin's ladder, lowest → highest. */
export type Rung = 'observed' | 'warned' | 'left';

/** Immutable input to the pure policy. State is the prior high-water marks. */
export interface CustomReportInput {
  violations: CustomViolation[];
  /** `${groupKey}|${customKey}` → highest violated_count already reported (idempotency). */
  reported: Record<string, number>;
  /** AUTO → tolls fire 'approved'; CONFIRM → 'pending'. Drip prose is unaffected. */
  mode: 'auto' | 'confirm';
  observeAt: number;
  warnAt: number;
  leftAt: number;
}

/** One report the pass should emit — a prose line and/or a soft toll, plus the new mark. */
export interface CustomReport {
  groupKey: string;
  customKey: string;
  name: string;
  /** the highest rung newly crossed this tick. */
  rung: Rung;
  /** the measured violated_count that drove it (the new high-water mark to persist). */
  violatedCount: number;
  /** measured honored_count, passed to reportObserved as the grounded "days kept". */
  honoredCount: number;
  /** the player-facing prose (already a voice.ts string), or null if this rung posts no line. */
  line: string;
  /** true when this rung also lays a soft reversible toll (warned). */
  toll: boolean;
}

export interface CustomReportDecision {
  reports: CustomReport[];
  /** the marks to MERGE into state.reported_customs (only for rows that fired). */
  marks: Record<string, number>;
  /** human-readable trace (logged; never player-facing). */
  notes: string[];
}

/** The highest rung a measured count reaches, or null if below the observe floor. */
function rungFor(violated: number, input: CustomReportInput): Rung | null {
  if (violated >= input.leftAt) return 'left';
  if (violated >= input.warnAt) return 'warned';
  if (violated >= input.observeAt) return 'observed';
  return null;
}

/** The lowest violated_count that first reaches `rung` (the rung's entry threshold). */
function rungEntry(rung: Rung, input: CustomReportInput): number {
  return rung === 'left' ? input.leftAt : rung === 'warned' ? input.warnAt : input.observeAt;
}

/**
 * decideCustomReports — the PURE customs policy. Same input → same output. For each measured
 * row it computes the highest rung the violated_count reaches, then fires ONLY if that rung's
 * entry threshold is strictly above what was already reported for that (player, custom) — the
 * idempotent high-water guard. A nameless row never fires (precision over recall).
 */
export function decideCustomReports(input: CustomReportInput): CustomReportDecision {
  const reports: CustomReport[] = [];
  const marks: Record<string, number> = {};
  const notes: string[] = [];

  for (const v of input.violations) {
    if (v.violatedCount <= 0) continue;                 // nothing measured → never invent one
    const rung = rungFor(v.violatedCount, input);
    if (!rung) continue;
    if (!v.name) {                                      // measured but unnamable → skip, don't guess
      notes.push(`skipped ${v.customKey} (${v.groupKey}): violated=${v.violatedCount} but no name`);
      continue;
    }

    const id = `${v.groupKey}|${v.customKey}`;
    const alreadyReported = input.reported[id] ?? 0;
    // Idempotent: only fire when this rung's ENTRY threshold is newly crossed since last report.
    if (rungEntry(rung, input) <= alreadyReported) continue;

    const line =
      rung === 'left'
        ? voice.reportEscalated(v.name)
        : voice.reportObserved(v.name, v.honoredCount, customPhrase(v.customKey));

    reports.push({
      groupKey: v.groupKey,
      customKey: v.customKey,
      name: v.name,
      rung,
      violatedCount: v.violatedCount,
      honoredCount: v.honoredCount,
      line,
      toll: rung === 'warned', // the soft toll lands at the warn rung
    });
    // High-water mark = the measured count, so any lower rung is also considered "covered".
    marks[id] = v.violatedCount;
  }

  // Deterministic order (group, then custom) so a tick's posts are stable + testable.
  reports.sort((a, b) => a.groupKey.localeCompare(b.groupKey) || a.customKey.localeCompare(b.customKey));
  return { reports, marks, notes };
}
