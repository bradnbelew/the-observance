/** I/O wrapper for the restart-safe kept/broken Unlit Deep reporting policy. */
import { getArcFlags, logEvent } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord } from './discord.js';
import { decideUnlitDeepReport } from './unlit-deep.js';

export interface UnlitDeepPassResult {
  reported: number;
}

function finiteFlag(flags: Record<string, unknown>, key: string): number | null {
  const value = flags[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

export async function runUnlitDeepPass(nowIso: string): Promise<UnlitDeepPassResult> {
  try {
    const flags = (await getArcFlags()) as Record<string, unknown>;
    const brokenByRaw = flags['unlit_deep_broken_by'];
    const state = await readState();
    const decision = decideUnlitDeepReport({
      brokenAt: finiteFlag(flags, 'unlit_deep_broken_at'),
      brokenBy: typeof brokenByRaw === 'string' ? brokenByRaw : null,
      keptAt: finiteFlag(flags, 'unlit_deep_kept_at'),
      lastBrokenReportedAt: state.unlit_deep_last_reported_at ?? null,
      lastKeptReportedAt: state.unlit_deep_last_kept_reported_at ?? null,
    });
    if (!decision.line || !decision.kind) return { reported: 0 };

    const ok = await postToTheRecord(decision.line);
    if (!ok) {
      await logEvent('warn', 'showrunner.unlit_deep', `${decision.kind} report post FAILED; retrying next cadence`);
      return { reported: 0 };
    }

    if (decision.brokenMark != null) state.unlit_deep_last_reported_at = decision.brokenMark;
    if (decision.keptMark != null) state.unlit_deep_last_kept_reported_at = decision.keptMark;
    await writeState(state, nowIso);
    const mark = decision.brokenMark ?? decision.keptMark;
    await logEvent('info', 'showrunner.unlit_deep', `${decision.kind} report posted (at=${mark})`);
    return { reported: 1 };
  } catch {
    return { reported: 0 };
  }
}
