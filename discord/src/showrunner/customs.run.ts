/**
 * customs.run.ts — the I/O wrapper for the customs→report/consequence bridge (P0-4 / D1).
 *
 * This is the side-effecting half (the pure policy is customs.ts, importable by the self-test
 * with no DB/Discord/config). One pass: read measured violations + the idempotency marks, run
 * the pure {@link decideCustomReports}, then for each fired report POST the prose to #the-record
 * (the REST seam — same delivery the drip uses; voice.ts is the sole text source) and, at the
 * warn rung, enqueue a SOFT REVERSIBLE toll beat (AUTO 'approved' / CONFIRM 'pending', the
 * spine's INV-6 gate). A mark advances ONLY after a successful post, so a delivery failure
 * re-tries next cadence rather than swallowing the report. Every step is best-effort + logged;
 * one failure never aborts the tick. Fault-isolated + graceful: if Supabase is down the read
 * returns [] and the pass simply does nothing (silence is canon, INV-7).
 */
import { readCustomViolations, enqueueBeat, logEvent, type CustomViolation } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord } from './discord.js';
import { decideCustomReports, OBSERVE_AT, WARN_AT, LEFT_AT } from './customs.js';
import type { BeatStatus } from '../db/types.js';

export interface CustomsPassResult {
  reported: number;
  tolled: number;
}

export async function runCustomsPass(mode: 'auto' | 'confirm', nowIso: string): Promise<CustomsPassResult> {
  let reported = 0;
  let tolled = 0;

  let violations: CustomViolation[] = [];
  try {
    violations = await readCustomViolations();
  } catch {
    return { reported, tolled }; // graceful: Supabase down → silence, no throw
  }
  if (violations.length === 0) return { reported, tolled };

  const state = await readState();
  const reportedMarks: Record<string, number> = { ...(state.reported_customs ?? {}) };

  const decision = decideCustomReports({
    violations,
    reported: reportedMarks,
    mode,
    observeAt: OBSERVE_AT,
    warnAt: WARN_AT,
    leftAt: LEFT_AT,
  });

  for (const note of decision.notes) {
    await logEvent('info', 'showrunner.customs', note);
  }
  if (decision.reports.length === 0) return { reported, tolled };

  // ANTI-SPAM (audit, CRITICAL): the customs bridge has no cadence/window gate of its own, so a cron
  // tick with 7 players each crossing a rung would burst #the-record. Post WORST-FIRST and cap per
  // tick; deferred reports keep their high-water mark (advanced only on a successful post, below), so
  // they surface next cadence — nothing is lost, the burst is just spaced.
  const RUNG_RANK: Record<string, number> = { left: 0, warned: 1, observed: 2 };
  const MAX_REPORTS_PER_TICK = 3;
  const ordered = [...decision.reports].sort(
    (a, b) => (RUNG_RANK[a.rung] ?? 9) - (RUNG_RANK[b.rung] ?? 9),
  );
  const toPost = ordered.slice(0, MAX_REPORTS_PER_TICK);
  if (ordered.length > toPost.length) {
    await logEvent('info', 'showrunner.customs',
      `deferring ${ordered.length - toPost.length} report(s) past the per-tick cap of ${MAX_REPORTS_PER_TICK}; they surface next cadence`);
  }

  let dirty = false;
  for (const r of toPost) {
    const id = `${r.groupKey}|${r.customKey}`;
    const ok = await postToTheRecord(r.line);
    if (!ok) {
      // Do NOT advance the mark on a failed post — re-attempt next cadence.
      await logEvent('warn', 'showrunner.customs',
        `report post FAILED (discord): custom=${r.customKey} rung=${r.rung} player=${r.name}`);
      continue;
    }
    reported += 1;
    reportedMarks[id] = r.violatedCount; // advance high-water mark only after a real post
    dirty = true;
    await logEvent('info', 'showrunner.customs',
      `report: custom=${r.customKey} rung=${r.rung} violated=${r.violatedCount} player=${r.name}`);

    if (r.toll) {
      try {
        const status: BeatStatus = mode === 'auto' ? 'approved' : 'pending';
        await enqueueBeat(
          'custom_toll',
          r.groupKey, // legacy target = the player's grouping key (mc_uuid when present)
          {
            // a SOFT, REVERSIBLE atmosphere beat — the deep goes dark for the night, no progress lost.
            kind: 'private_darkness',
            mc_uuid: r.groupKey,
            custom_key: r.customKey,
            reversible: true,
            reason: `warn-rung toll: ${r.customKey} violated=${r.violatedCount}`,
          },
          status,
        );
        tolled += 1;
        await logEvent('info', 'showrunner.customs',
          `toll enqueued (${status}): custom=${r.customKey} player=${r.name}`);
      } catch {
        // best-effort: the report still landed; the toll just isn't laid this tick.
        await logEvent('warn', 'showrunner.customs',
          `toll enqueue FAILED: custom=${r.customKey} player=${r.name}`);
      }
    }
  }

  if (dirty) {
    state.reported_customs = reportedMarks;
    try {
      await writeState(state, nowIso);
    } catch {
      // If persistence fails the marks won't advance; next tick re-derives + the post guard
      // (above) plus the high-water compare keep it from spamming — at worst one repeat.
      await logEvent('warn', 'showrunner.customs', 'failed to persist reported_customs marks');
    }
  }

  return { reported, tolled };
}
