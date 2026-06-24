/**
 * apply.ts — turn a {@link Decision} into DB + Discord writes. The ONLY place the showrunner mutates
 * anything. Every write is best-effort and logged; a single failure never aborts the tick.
 *
 *  - gift  → bump whisper_budgets.earned by 1 (a free whisper the stuck player can claim). Self-limiting:
 *            once gifted, the player's remaining > 0, so decide() won't re-gift until they spend it.
 *  - drip  → AUTO: post the Watcher's line to #the-record now; CONFIRM: stage it for the dashboard's
 *            manual "post" button. Either way the puzzle is marked dripped + the cadence advances, so
 *            it is never re-decided.
 *  - health→ write last_run + persist state; a heartbeat row in event_log the dashboard reads.
 */
import { supabase } from '../db/client.js';
import { getBudget, logEvent } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord } from './discord.js';
import { voice } from '../voice.js';
import type { Decision, Snapshot } from './types.js';

export interface ApplyResult {
  gifted: number;
  dripped: number;
  staged: number;
}

export async function applyDecision(decision: Decision, snapshot: Snapshot): Promise<ApplyResult> {
  const nowIso = new Date(snapshot.nowMs).toISOString();
  const state = await readState();
  state.dripped_keys = state.dripped_keys ?? [];
  state.pending_drips = state.pending_drips ?? [];

  let gifted = 0;
  let dripped = 0;
  let staged = 0;

  // --- gifts: a free whisper for each stuck, exhausted attempter ---
  for (const g of decision.gifts) {
    try {
      const budget = await getBudget(g.playerId, g.act);
      if (!budget) continue;
      const { error } = await supabase
        .from('whisper_budgets')
        .update({ earned: budget.earned + 1 })
        .eq('id', budget.id)
        .eq('earned', budget.earned); // optimistic: don't double-bump under a race
      if (!error) {
        gifted += 1;
        await logEvent('info', 'showrunner',
          `auto-gift: player=${g.playerId} puzzle=${g.puzzleKey} tier=${g.tier} (${g.reason})`);
      }
    } catch {
      // best-effort; skip this gift
    }
  }

  // --- drips: post (AUTO) or stage (CONFIRM); mark dripped + advance cadence either way ---
  for (const d of decision.drips) {
    if (d.staged) {
      state.pending_drips.push({ puzzle_key: d.puzzleKey, movement: d.movement, staged_iso: nowIso });
      state.dripped_keys.push(d.puzzleKey);
      state.last_drip_at_ms = snapshot.nowMs;
      staged += 1;
      await logEvent('info', 'showrunner',
        `staged drip (CONFIRM): puzzle=${d.puzzleKey} — awaiting dashboard approval`);
    } else {
      const ok = await postToTheRecord(voice.drip());
      if (ok) {
        state.dripped_keys.push(d.puzzleKey);
        state.last_drip_at_ms = snapshot.nowMs;
        dripped += 1;
        await logEvent('info', 'showrunner', `drip posted: puzzle=${d.puzzleKey}`);
      } else {
        await logEvent('warn', 'showrunner', `drip post FAILED (discord): puzzle=${d.puzzleKey}`);
      }
    }
  }

  // --- health heartbeat ---
  state.last_run_iso = nowIso;
  await writeState(state, nowIso);
  await logEvent('info', 'showrunner',
    `tick: ${decision.health.note}${decision.notes.length ? '; ' + decision.notes.join('; ') : ''}`);

  return { gifted, dripped, staged };
}
