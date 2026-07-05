/**
 * unlit-deep.run.ts — the I/O wrapper for the Unlit Deep group latch's ONE report (see unlit-deep.ts
 * for the pure policy). Reads `arc_state.flags` (the plugin's UnlitDeepListener writes it directly,
 * same seam as coop-gate.ts's `coop_world_ready_at`), runs the pure decision, and on a fresh break
 * posts `voice.tollUnlitDeep()` to #the-record — then advances the high-water mark only after a
 * successful post (mirrors customs.run.ts: a delivery failure re-tries next cadence, never swallowed).
 * Fully fault-isolated + graceful: any failure (Supabase down, post failure) → silence, never throws.
 */
import { getArcFlags, logEvent } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord } from './discord.js';
import { decideUnlitDeepReport } from './unlit-deep.js';

export interface UnlitDeepPassResult {
  reported: number;
}

export async function runUnlitDeepPass(nowIso: string): Promise<UnlitDeepPassResult> {
  try {
    const flags = await getArcFlags();
    const brokenAtRaw = (flags as Record<string, unknown>)['unlit_deep_broken_at'];
    const brokenAt = typeof brokenAtRaw === 'number' && Number.isFinite(brokenAtRaw) ? brokenAtRaw : null;
    const brokenByRaw = (flags as Record<string, unknown>)['unlit_deep_broken_by'];
    const brokenBy = typeof brokenByRaw === 'string' ? brokenByRaw : null;

    const state = await readState();
    const decision = decideUnlitDeepReport({
      brokenAt,
      brokenBy,
      lastReportedAt: state.unlit_deep_last_reported_at ?? null,
    });
    if (!decision.line || decision.mark == null) return { reported: 0 };

    const ok = await postToTheRecord(decision.line);
    if (!ok) {
      await logEvent('warn', 'showrunner.unlit_deep', 'report post FAILED (discord); retrying next cadence');
      return { reported: 0 };
    }

    state.unlit_deep_last_reported_at = decision.mark;
    await writeState(state, nowIso);
    await logEvent('info', 'showrunner.unlit_deep', `report posted (broken_at=${decision.mark})`);
    return { reported: 1 };
  } catch (e) {
    return { reported: 0 }; // graceful: Supabase down / any failure → silence, no throw (INV-7)
  }
}
