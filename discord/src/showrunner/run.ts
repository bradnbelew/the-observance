/**
 * run.ts — the showrunner cron entrypoint (Render Cron Job runs this once per schedule; it does one
 * tick and exits). Deterministic spine: snapshot → decide → apply. No LLM. `--dry-run` reads + decides
 * + prints but writes NOTHING — safe to run against the live DB to see what the next tick would do.
 *
 *   npm run showrunner        # one live tick
 *   npm run showrunner:dry    # read-only preview
 */
import { buildSnapshot } from './snapshot.js';
import { decide } from './decide.js';
import { applyDecision } from './apply.js';
import { runCustomsPass } from './customs.run.js';
import { decideCustomReports, OBSERVE_AT, WARN_AT, LEFT_AT } from './customs.js';
import { readCustomViolations } from '../db/repo.js';
import { readState } from './state.js';

async function main(): Promise<void> {
  const dryRun = process.argv.includes('--dry-run');
  const nowMs = Date.now();

  const snapshot = await buildSnapshot(nowMs);
  const decision = decide(snapshot);

  if (dryRun) {
    // Read-only customs preview through the SAME pure policy the live pass uses (no writes).
    const violations = await readCustomViolations();
    const state = await readState();
    const customsDecision = decideCustomReports({
      violations,
      reported: state.reported_customs ?? {},
      mode: snapshot.mode,
      observeAt: OBSERVE_AT, warnAt: WARN_AT, leftAt: LEFT_AT,
    });

    console.log('[showrunner] DRY RUN — no writes');
    console.log(JSON.stringify({
      snapshot: {
        asleep: snapshot.asleep,
        mode: snapshot.mode,
        currentAct: snapshot.currentAct,
        openPuzzles: snapshot.openPuzzles.length,
        lastDripAtMs: snapshot.lastDripAtMs,
        stuck: snapshot.openPuzzles
          .filter((p) => !p.solvedInWindow && p.failedAttemptsInWindow >= snapshot.stallFailedThreshold)
          .map((p) => ({ puzzle: p.puzzleKey, failed: p.failedAttemptsInWindow, attempters: p.attempters.length })),
      },
      decision,
      customs: {
        violations: violations.length,
        reports: customsDecision.reports.map((r) => ({ player: r.name, custom: r.customKey, rung: r.rung, toll: r.toll })),
        notes: customsDecision.notes,
      },
    }, null, 2));
    return;
  }

  const result = await applyDecision(decision, snapshot);

  // The customs→report/consequence bridge (P0-4 / D1): reads custom_compliance and reports
  // crossed rungs / lays soft tolls. Respects the kill-switch (asleep → silent, like decide)
  // and is fully fault-isolated, so it can never abort the tick the spine already applied.
  let customs = { reported: 0, tolled: 0 };
  if (!snapshot.asleep) {
    try {
      customs = await runCustomsPass(snapshot.mode, new Date(nowMs).toISOString());
    } catch (e) {
      console.error('[showrunner] customs pass error (isolated)', e);
    }
  }

  console.log(
    `[showrunner] tick done: gifted=${result.gifted} dripped=${result.dripped} staged=${result.staged} ` +
    `reported=${customs.reported} tolled=${customs.tolled}`,
  );
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error('[showrunner] FATAL', e);
    process.exit(1);
  });
