/**
 * run.ts — the showrunner cron entrypoint (Render Cron Job runs this once per schedule; it does one
 * tick and exits). Deterministic spine: snapshot → decide → apply. No LLM. `--dry-run` reads + decides
 * + prints but writes NOTHING — safe to run against the live DB to see what the next tick would do.
 *
 *   npm run showrunner        # one live tick
 *   npm run showrunner:dry    # read-only preview
 */
import { pathToFileURL } from 'node:url';
import { buildSnapshot } from './snapshot.js';
import { decide } from './decide.js';
import { applyDecision } from './apply.js';
import { runCustomsPass } from './customs.run.js';
import { decideCustomReports, OBSERVE_AT, WARN_AT, LEFT_AT } from './customs.js';
import { runUnlitDeepPass } from './unlit-deep.run.js';
import { computeAutonomyGates, runAutonomyPasses } from './autonomy.run.js';
import { materializeArchive } from './archive.run.js';
import { runReportsPass } from './reports.run.js';
import { runObserverPass } from './observer.run.js';
import { readCustomViolations } from '../db/repo.js';
import { isV5CampaignActive, readState, tryAcquireShowrunnerLock, releaseShowrunnerLock, writeState } from './state.js';

export interface ShowrunnerTickOptions {
  dryRun?: boolean;
  nowMs?: number;
  leaseSeconds?: number;
}

export type ShowrunnerTickResult = 'ran' | 'dry-run' | 'locked';

/** One import-safe, database-lease-protected tick for the worker loop or recovery cron. */
export async function runShowrunnerTick(options: ShowrunnerTickOptions = {}): Promise<ShowrunnerTickResult> {
  const dryRun = options.dryRun ?? false;
  const nowMs = options.nowMs ?? Date.now();
  const nowIso = new Date(nowMs).toISOString();

  // Overlap guard (migration 0011): a real tick writes the showrunner_state row via a plain
  // read-then-upsert, so two overlapping cron invocations could clobber each other's high-water marks
  // or double-post a one-shot announcement. --dry-run writes NOTHING (safe to run any time, even
  // alongside a live tick), so it never contends for the lock. A tick that can't acquire it simply
  // skips — the next cadence catches up; skipping is always safe, racing is not.
  if (!dryRun) {
    const acquired = await tryAcquireShowrunnerLock(options.leaseSeconds ?? 300);
    if (!acquired) {
      console.log('[showrunner] another tick holds the lock — skipping this run');
      return 'locked';
    }
  }
  try {
    if (await isV5CampaignActive()) {
      await runV5SafeHeartbeat(dryRun, nowIso);
      return dryRun ? 'dry-run' : 'ran';
    }
    await runTick(dryRun, nowMs, nowIso);
    return dryRun ? 'dry-run' : 'ran';
  } finally {
    if (!dryRun) await releaseShowrunnerLock();
  }
}

async function runTick(dryRun: boolean, nowMs: number, nowIso: string): Promise<void> {
  // A10/B4: compute the difficulty grip + the prologue gate FIRST, then fold them onto the snapshot so
  // the cadence scaling + curatorial-drip suppression apply to THIS tick. Fault-isolated inside (a
  // failure returns empty gates → the spine runs at its neutral defaults). On --dry-run the inner
  // hysteresis persistence is suppressed, so a preview writes NOTHING.
  const gates = await computeAutonomyGates(nowMs, nowIso, dryRun);
  const baseSnapshot = await buildSnapshot(nowMs);
  const snapshot = { ...baseSnapshot, reckoning: gates.reckoning, prologue: gates.prologue };
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
      gates: {
        reckoning: gates.reckoning ?? null,
        prologue: gates.prologue ?? null,
      },
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
  let reports = { reported: 0, staged: 0 };
  let observer = { echoed: false };
  let unlitDeep = { reported: 0 };
  let autonomy = { graves: 0, herdSpreads: 0, forksSet: 0, coldRestages: 0, apparitionClaimed: false, theoriesLocked: 0, keptNeedleGranted: false, reliefPosted: 0, carveFired: false, offlineSkinFired: false };
  if (!snapshot.asleep) {
    try {
      customs = await runCustomsPass(snapshot.mode, nowIso);
    } catch (e) {
      console.error('[showrunner] customs pass error (isolated)', e);
    }
    // The Unlit Deep group latch's ONE report (INV-17): the plugin's UnlitDeepListener writes
    // UnlitDeepListener writes durable kept/broken windows; this posts one fresh outcome per cadence.
    try {
      unlitDeep = await runUnlitDeepPass(nowIso);
    } catch (e) {
      console.error('[showrunner] unlit-deep pass error (isolated)', e);
    }
    // The between-session autonomy producers (grave / herd / forks / clock), beside the customs
    // bridge. Fully fault-isolated internally; one wrap here keeps a hard failure off the spine.
    try {
      autonomy = await runAutonomyPasses(snapshot.mode, nowIso);
    } catch (e) {
      console.error('[showrunner] autonomy pass error (isolated)', e);
    }
    // Recovery Archive materialize (W3a): resolve the 42 card bodies from voice.archive.ts into
    // thread_card_bodies so v_archive can reveal-gate them for the Record. Static authored text,
    // idempotent, progress-independent; fault-isolated so it can never abort the applied tick.
    try {
      await materializeArchive();
    } catch (e) {
      console.error('[showrunner] archive materialize error (isolated)', e);
    }
    // Tier-0 "it knows you" observation (W3): score the group's measured behavior and name a real
    // dominant habit to #the-record (precision-gated; a flat group names no one). Fault-isolated.
    try {
      reports = await runReportsPass(snapshot.mode, decision.tone);
    } catch (e) {
      console.error('[showrunner] reports pass error (isolated)', e);
    }
    // Observer Tier-1 "it heard you" (W4): sparsely echo one grounded captured utterance. No-op unless
    // the global observer_capture switch is on; consent + rate-limit enforced inside. Fault-isolated.
    try {
      observer = await runObserverPass();
    } catch (e) {
      console.error('[showrunner] observer pass error (isolated)', e);
    }
  }

  console.log(
    `[showrunner] tick done: gifted=${result.gifted} dripped=${result.dripped} staged=${result.staged} ` +
    `reported=${customs.reported} tolled=${customs.tolled} ` +
    `grip=${gates.reckoning?.state ?? 'even'} tone=${decision.tone} ` +
    `graves=${autonomy.graves} herd=${autonomy.herdSpreads} forks=${autonomy.forksSet} ` +
    `cold=${autonomy.coldRestages} apparition=${autonomy.apparitionClaimed ? 1 : 0} ` +
    `theories=${autonomy.theoriesLocked} needle=${autonomy.keptNeedleGranted ? 1 : 0} relief=${autonomy.reliefPosted} ` +
    `carve=${autonomy.carveFired ? 1 : 0} offline_skin=${autonomy.offlineSkinFired ? 1 : 0} ` +
    `observed=${reports.reported} heard=${observer.echoed ? 1 : 0} unlit_deep=${unlitDeep.reported}`,
  );
}

const isDirectRun = process.argv[1] !== undefined && import.meta.url === pathToFileURL(process.argv[1]).href;
if (isDirectRun) {
  runShowrunnerTick({ dryRun: process.argv.includes('--dry-run') })
    .then(() => process.exit(0))
    .catch((e) => {
      console.error('[showrunner] FATAL', e);
      process.exit(1);
    });
}

/**
 * V5 progression is receipt-driven by the website, Discord oracle, and Paper runtime. The previous
 * customs, apparition, herd, grave, archive-card, and finale producers belong to the retired campaign
 * and must never improvise into V5. Keep only the liveness timestamp used by operations.
 */
async function runV5SafeHeartbeat(dryRun: boolean, nowIso: string): Promise<void> {
  if (dryRun) {
    console.log('[showrunner] V5 SAFE DRY RUN — legacy autonomy is suppressed; no writes');
    return;
  }
  const state = await readState();
  await writeState({ ...state, last_run_iso: nowIso, campaign_mode: 'v5-safe' }, nowIso);
  console.log('[showrunner] V5 safe heartbeat — receipt progression active; legacy autonomy suppressed');
}
