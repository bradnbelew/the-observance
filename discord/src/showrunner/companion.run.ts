/**
 * companion.run.ts — the I/O wrapper for the Wren companion lane (D3 `the-companion`, the-companion.md).
 *
 * The side-effecting half of companion.ts. Mirrors the liar/keeper lanes: it reads the measured state
 * (the companion `event_log` rows the plugin producers wrote + the arc_state flags + the delivery
 * high-waters), runs the PURE resolver, and enqueues an in-world beat carrying the resolved `wren.*`
 * key. Fault-isolated + graceful: any read error ⇒ the pass quietly does nothing (silence is canon,
 * INV-7); a missing data source ⇒ no-op, never a guessed line.
 *
 * REGISTER + SEPARATION LAW. Wren's lines are SET-A human speech in voice.archive.ts `npcLines`,
 * resolved via `npcLine()` (NEVER `archiveLine()`). We enqueue a `private_message` beat (the same
 * in-world delivery the liar lane uses) with `step_payload.key = node.voiceKey` AND the pre-resolved
 * text, so the downstream renderer speaks the exact authored human-register line — never crossed with
 * the Watcher's close (which finale.run.ts posts to #the-record).
 *
 * IDEMPOTENCY (set-once). The reveal pair and the one-of-three reckoning last-words are each delivered
 * exactly once, guarded by `state.companion_lines_delivered` / `state.companion_reckoning_delivered`.
 * A restart mid-tick re-derives the same single beat (the plugin producers are already idempotent —
 * one-of-three reckoning, set-once reveal).
 */
import { supabase } from '../db/client.js';
import { enqueueBeat, logEvent } from '../db/repo.js';
import { npcLine } from '../voice.archive.js';
import type { ShowrunnerState } from './state.js';
import type { BeatStatus } from '../db/types.js';
import {
  resolveCompanionDialogue,
  type CompanionArcFlags,
  type CompanionContext,
  type CompanionDialogueInput,
} from './companion.js';
import { decodePluginEvent, type StoredEventLogRow } from './event-log.js';

/** The reckoning contexts the plugin logs (event_log.context) — the reckoning branch reads the latest. */
const RECKONING_CONTEXTS = new Set(['reckoning.condemn', 'reckoning.understand', 'reckoning.free']);

/**
 * Read the latest companion event_log context (type='companion'). The plugin writes
 * `event_log(type='companion', context='npc.open'|'reckoning.*', detail={...})`. We only need the most
 * recent context to drive the branch. Fault-isolated: any error / no rows ⇒ null (the pass no-ops).
 * A reckoning context is preferred over 'npc.open' when both are present (the arc has moved forward).
 */
async function readCompanionContext(): Promise<{ context: CompanionContext; trust: number }> {
  try {
    const { data } = await supabase
      .from('event_log')
      .select('id, source, message, created_at')
      .order('id', { ascending: true })
      .limit(256)
      .returns<StoredEventLogRow[]>();
    const rows = (data ?? []).map(decodePluginEvent).filter((r) => r.type === 'companion');
    if (rows.length === 0) return { context: null, trust: 0 };

    // Prefer a reckoning row (the arc's latest state) over the trust-ladder 'npc.open' rows.
    const reckoningRow = [...rows].reverse().find((r) => r.context && RECKONING_CONTEXTS.has(r.context));
    const context = (reckoningRow?.context ?? rows[rows.length - 1]?.context ?? null) as CompanionContext;

    // The measured group trust is the max trust across the npc.open rows' detail (best-effort parse).
    let trust = 0;
    for (const r of rows) {
      if (!r.detail) continue;
      try {
        const d = r.detail as { trust?: unknown };
        if (typeof d.trust === 'number' && d.trust > trust) trust = d.trust;
      } catch {
        /* detail is not JSON — ignore, trust stays measured-or-zero */
      }
    }
    return { context, trust };
  } catch {
    return { context: null, trust: 0 };
  }
}

export interface CompanionPassResult {
  /** number of companion beats enqueued this pass (0, 1, or 2 — reveal.yes + reveal.tally). */
  enqueued: number;
  /** the delivery high-waters advanced (the caller persists state). */
  dirty: boolean;
}

/**
 * runCompanionPass — the between-session companion producer. Reads the companion event_log + the passed
 * arc flags + movement, resolves the node, and enqueues the in-world beat set-once. Mutates `state`'s
 * delivery high-waters in place; the caller (autonomy.run.ts) persists when `dirty`.
 *
 * The `flags`/`movement` are passed in (already fetched by runAutonomyPasses) to avoid a second read.
 */
export async function runCompanionPass(
  mode: 'auto' | 'confirm',
  flags: Record<string, unknown>,
  movement: number,
  state: ShowrunnerState,
): Promise<CompanionPassResult> {
  const result: CompanionPassResult = { enqueued: 0, dirty: false };
  const beatStatus: BeatStatus = mode === 'auto' ? 'approved' : 'pending';

  const arcFlags: CompanionArcFlags = {
    companionRevealed: flags.companion_revealed === true,
    reckoningCondemn: flags.reckoning_condemn === true,
    reckoningUnderstand: flags.reckoning_understand === true,
    reckoningFree: flags.reckoning_free === true,
  };

  // Nothing companion-related has fired yet AND no reveal → the arc has not reached a deliverable
  // consumer; skip the event_log read entirely (the trust ladder lines are delivered by the plugin's
  // own KeeperNpcBeat on right-click; the showrunner only owns the reveal + reckoning set-once beats).
  const anyDeliverable =
    arcFlags.companionRevealed ||
    arcFlags.reckoningCondemn ||
    arcFlags.reckoningUnderstand ||
    arcFlags.reckoningFree;
  if (!anyDeliverable) return result;

  const { context } = await readCompanionContext();

  const delivered = new Set(state.companion_lines_delivered ?? []);

  const input: CompanionDialogueInput = {
    flags: arcFlags,
    trust: 0, // the trust ladder is delivered by the plugin on right-click; here we own reveal+reckoning
    movement,
    context,
    lateJoiner: false, // the newhand line is a right-click surface, not a between-session beat
    revealDelivered: delivered.has('wren.reveal.yes') && delivered.has('wren.reveal.tally'),
    reckoningDelivered: state.companion_reckoning_delivered === true,
  };

  const decision = resolveCompanionDialogue(input);
  const node = decision.node;
  if (!node) {
    for (const n of decision.notes) await logEvent('info', 'showrunner.companion', n);
    return result;
  }

  // Enqueue helper — resolves the SET-A human line via npcLine() (never archiveLine()); passes both the
  // key (step_payload.key, the spec contract) and the pre-resolved text so the downstream renderer
  // speaks the exact human-register line regardless of whether it knows the wren.* namespace.
  const enqueueWren = async (voiceKey: string, reason: string): Promise<boolean> => {
    const text = npcLine(voiceKey);
    if (text == null) {
      await logEvent('warn', 'showrunner.companion', `companion key '${voiceKey}' not in npcLines — skip (no guess)`);
      return false;
    }
    await enqueueBeat('private_message', null, {
      step_payload: { key: voiceKey, text },
      kind: 'companion', voice_key: voiceKey, reason,
    }, beatStatus);
    return true;
  };

  if (node.isReckoning) {
    if (await enqueueWren(node.voiceKey, node.reason)) {
      state.companion_reckoning_delivered = true;
      result.enqueued += 1;
      result.dirty = true;
      await logEvent('info', 'showrunner.companion', `reckoning last-words delivered: ${node.voiceKey} (${beatStatus})`);
    }
    return result;
  }

  if (node.isReveal) {
    // Deliver the reveal PAIR once: the spoken admission (reveal.yes) + the found-tally companion beat
    // (reveal.tally). Each is guarded independently so a mid-tick restart resumes at the missing one.
    for (const key of ['wren.reveal.yes', 'wren.reveal.tally']) {
      if (delivered.has(key)) continue;
      if (await enqueueWren(key, `companion reveal (${key})`)) {
        delivered.add(key);
        result.enqueued += 1;
        result.dirty = true;
      }
    }
    if (result.dirty) {
      state.companion_lines_delivered = [...delivered].sort();
      await logEvent('info', 'showrunner.companion', `reveal delivered (${result.enqueued} line(s), ${beatStatus})`);
    }
    return result;
  }

  return result;
}
