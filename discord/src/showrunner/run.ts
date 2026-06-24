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

async function main(): Promise<void> {
  const dryRun = process.argv.includes('--dry-run');
  const nowMs = Date.now();

  const snapshot = await buildSnapshot(nowMs);
  const decision = decide(snapshot);

  if (dryRun) {
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
    }, null, 2));
    return;
  }

  const result = await applyDecision(decision, snapshot);
  console.log(`[showrunner] tick done: gifted=${result.gifted} dripped=${result.dripped} staged=${result.staged}`);
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error('[showrunner] FATAL', e);
    process.exit(1);
  });
