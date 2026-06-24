/**
 * apply.ts — turn a {@link Decision} into DB + Discord writes. The ONLY place the showrunner mutates
 * anything. Every write is best-effort and logged; a single failure never aborts the tick.
 *
 *  - gift  → bump whisper_budgets.earned by 1 (a free whisper the stuck player can claim). Self-limiting:
 *            once gifted, the player's remaining > 0, so decide() won't re-gift until they spend it.
 *  - drip  → AUTO: FORGE the dripped node's clue card and post it to #the-record now (the artifact
 *            the player decodes — COHERENCE-AUDIT C1 / P0-6); CONFIRM: stage it for the dashboard's
 *            manual "post" button. Either way the puzzle is marked dripped + the cadence advances, so
 *            it is never re-decided. If the forge/render/upload fails (or the node is somehow not
 *            forgeable), the drip degrades gracefully to the in-world-pointing report line
 *            (`voice.drip()`) — a drip ALWAYS surfaces something, never a silent no-op.
 *  - health→ write last_run + persist state; a heartbeat row in event_log the dashboard reads.
 */
import { supabase } from '../db/client.js';
import { getBudget, logEvent } from '../db/repo.js';
import { readState, writeState } from './state.js';
import { postToTheRecord, postClueImageToTheRecord } from './discord.js';
import { forgeDripCard } from './clue-drip.js';
import { voice } from '../voice.js';
import type { Decision, DripDecision, Snapshot } from './types.js';

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
      const ok = await postDrip(d);
      if (ok) {
        state.dripped_keys.push(d.puzzleKey);
        state.last_drip_at_ms = snapshot.nowMs;
        dripped += 1;
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

/**
 * Post one AUTO drip to #the-record (COHERENCE-AUDIT C1 / P0-6). Returns true iff SOMETHING
 * was posted (a clue card OR the fallback report line). Fault-isolated end to end: any forge,
 * render, or upload failure degrades to the in-world-pointing `voice.drip()` line so a drip is
 * never a silent no-op — and a `false` return only means BOTH the card and the fallback line
 * failed to send (a hard Discord outage), which the caller logs and leaves un-dripped to retry.
 *
 * Routing:
 *   - forgeable node → forge its card (the same authored spec the world carves, X1) and upload
 *     the PNG. If the card can't be built/sent, fall through to the report line.
 *   - non-forgeable node (shouldn't reach AUTO after P0-7's pool filter, but defended here) →
 *     post the in-world-pointing report line directly.
 * No English is composed here; the card carries forged runes + seeded chrome, the fallback is a
 * voice.ts line. voice.ts stays the sole text source.
 */
async function postDrip(d: DripDecision): Promise<boolean> {
  if (d.forgeable) {
    try {
      const card = await forgeDripCard(d.puzzleKey);
      if (await postClueImageToTheRecord(card.png, card.filename)) {
        await logEvent('info', 'showrunner', `drip posted (clue card): puzzle=${d.puzzleKey}`);
        return true;
      }
      await logEvent('warn', 'showrunner',
        `drip card upload failed; falling back to report line: puzzle=${d.puzzleKey}`);
    } catch (e) {
      // forge/render failure (or an unexpectedly non-forgeable key) — never abort the tick.
      await logEvent('warn', 'showrunner',
        `drip forge/render failed; falling back to report line: puzzle=${d.puzzleKey} (${e instanceof Error ? e.message : String(e)})`);
    }
  }

  // Fallback / non-forgeable: the in-world-pointing report line (no card to surface).
  const ok = await postToTheRecord(voice.drip());
  if (ok) {
    await logEvent('info', 'showrunner', `drip posted (report line): puzzle=${d.puzzleKey}`);
  }
  return ok;
}
