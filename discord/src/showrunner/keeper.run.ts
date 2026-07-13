/**
 * Event-driven I/O wrapper for the Keeper NPC dialogue tree.
 *
 * A right-click writes one folded plugin event. This pass consumes each event once,
 * grounds the branch in the measured dossier/custom ledger, resolves keeper.ts,
 * binds authored archive text, and enqueues the private KeeperNpcBeat. Directed
 * dialogue is always approved: the player already earned it by walking to and
 * interacting with the NPC, even when ambient curatorial beats use CONFIRM mode.
 */
import { supabase } from '../db/client.js';
import { enqueueBeat, logEvent, readCustomViolations, readDossiers } from '../db/repo.js';
import { archiveLine } from '../voice.archive.js';
import { buildObservationDossiers, type MeasuredBehavior } from './reports.js';
import { decodePluginEvent, type DecodedPluginEvent, type StoredEventLogRow } from './event-log.js';
import { keeperRhyme, renderKeeperLine, selectBrokenCustom } from './keeper-runtime.js';
import { resolveKeeperDialogue, type KeeperDialogueDossier } from './keeper.js';
import type { ShowrunnerState } from './state.js';

const EVENT_BATCH = 32;

async function readKeeperOpens(afterId: number): Promise<DecodedPluginEvent[]> {
  const { data, error } = await supabase
    .from('event_log')
    .select('id, source, message, created_at')
    .eq('source', 'npc.open')
    .ilike('message', '[keeper]%')
    .gt('id', afterId)
    .order('id', { ascending: true })
    .limit(EVENT_BATCH)
    .returns<StoredEventLogRow[]>();
  if (error) throw error;
  return (data ?? []).map(decodePluginEvent).filter((e) => e.type === 'keeper');
}

/** First player-bound narrative event the record genuinely stored; null is safer than a guessed memory. */
async function readFirstLoggedBeat(mcUuid: string): Promise<string | null> {
  try {
    const { data, error } = await supabase
      .from('event_log')
      .select('id, source, message, created_at')
      .ilike('message', `%uuid=${mcUuid}%`)
      .order('id', { ascending: true })
      .limit(32)
      .returns<StoredEventLogRow[]>();
    if (error) return null;
    const row = (data ?? [])
      .map(decodePluginEvent)
      .find((e) =>
        e.mcUuid === mcUuid.toLowerCase()
        && e.message.length > 0
        && e.type !== 'keeper'
        && e.type !== 'companion'
        && e.type !== 'error'
        && e.type !== 'warn');
    return row?.message ?? null;
  } catch {
    return null;
  }
}

function nameFromOpen(e: DecodedPluginEvent): string | null {
  const name = /^(.+?) opened the keeper\b/i.exec(e.message)?.[1]?.trim();
  return name || null;
}

export interface KeeperDialoguePassResult {
  enqueued: number;
  dirty: boolean;
}

export async function runKeeperDialoguePass(
  flags: Record<string, unknown>,
  movement: number,
  state: ShowrunnerState,
  nowMs: number,
): Promise<KeeperDialoguePassResult> {
  const result: KeeperDialoguePassResult = { enqueued: 0, dirty: false };
  const opens = await readKeeperOpens(state.keeper_last_open_id ?? 0);
  if (opens.length === 0) return result;

  const [dossierRows, violations] = await Promise.all([readDossiers(), readCustomViolations()]);
  const honored = new Map<string, number>();
  const violated = new Map<string, number>();
  for (const v of violations) {
    honored.set(`${v.groupKey}:${v.customKey}`, v.honoredCount);
    violated.set(`${v.groupKey}:${v.customKey}`, v.violatedCount);
  }
  const behaviors: MeasuredBehavior[] = dossierRows.map((r) => ({
    ...r,
    bowViolations: violated.get(`${r.groupKey}:the_bow`) ?? 0,
    darkHoursViolations: violated.get(`${r.groupKey}:the_dark_hours`) ?? 0,
  }));
  const observations = new Map(buildObservationDossiers(behaviors, honored).map((d) => [d.groupKey, d]));
  const firstBeats = new Map<string, string | null>();
  await Promise.all([...new Set(opens.map((e) => e.mcUuid).filter((u): u is string => u != null))]
    .map(async (uuid) => firstBeats.set(uuid, await readFirstLoggedBeat(uuid))));

  const fact9Window = Math.floor(nowMs / (3 * 60 * 60_000));
  const fact9Marks = state.keeper_fact9_windows ?? {};
  const pending = state.keeper_atonement_pending ?? {};

  for (const event of opens) {
    const target = event.mcUuid;
    if (!target) {
      state.keeper_last_open_id = event.id;
      result.dirty = true;
      await logEvent('warn', 'showrunner.keeper', `keeper event ${event.id} had no player UUID; consumed without delivery`);
      continue;
    }

    const playerViolations = violations.filter((v) => v.groupKey === target);
    const oldPending = pending[target];
    const pendingRow = oldPending
      ? playerViolations.find((v) => v.customKey === oldPending.customKey) ?? null
      : null;
    const newlyBroken = selectBrokenCustom(playerViolations);
    const brokenCustom = oldPending?.customKey ?? newlyBroken?.customKey ?? null;
    const atoned = oldPending != null
      && pendingRow != null
      && pendingRow.honoredCount > oldPending.honoredAtOpen;

    const observation = observations.get(target) ?? null;
    const dossier: KeeperDialogueDossier = {
      groupKey: target,
      name: observation?.name ?? nameFromOpen(event),
      rhymesWith: keeperRhyme(observation),
      brokenCustom,
      atoned,
    };
    const firstBeat = firstBeats.get(target) ?? null;
    const claimed = state.apparition_claim?.window === fact9Window
      && state.apparition_claim.group_key === target;
    const decision = resolveKeeperDialogue({
      kind: 'presiding',
      dossier,
      issCaught: flags.iss_caught === true,
      movement,
      loggedFirstBeat: firstBeat,
      fact9ShownThisWindow: fact9Marks[target] === fact9Window || claimed,
      apparitionClaimedFor: claimed,
    });
    if (!decision.node) {
      state.keeper_last_open_id = event.id;
      result.dirty = true;
      continue;
    }

    // A Citizens node hint may select another authored Keeper key, but never bypass a measured
    // M-IV withholding and never force FACT 9 without a real logged beat.
    const hint = typeof event.detail.node_hint === 'string' ? event.detail.node_hint.trim() : '';
    const hintedTemplate = hint.startsWith('keeper.') ? archiveLine(hint) : null;
    const mayUseHint = hintedTemplate != null
      && !decision.node.withholdsFragment
      && (hint !== 'keeper.fact9.named' || firstBeat != null);
    const voiceKey = mayUseHint ? hint : decision.node.voiceKey;
    const template = mayUseHint ? hintedTemplate : archiveLine(voiceKey);
    if (!template) {
      await logEvent('error', 'showrunner.keeper', `authored Keeper key '${voiceKey}' is missing; event ${event.id} left for retry`);
      break;
    }
    const deliversFact9 = voiceKey === 'keeper.fact9.named';

    await enqueueBeat('keeper_npc', target, {
      node_key: voiceKey,
      speaker: 'the keeper',
      lines: [renderKeeperLine(template, firstBeat)],
      line_delay_ticks: 35,
      color: 'gray',
      interaction_event_id: event.id,
      site: typeof event.detail.site === 'string' ? event.detail.site : null,
      reason: mayUseHint ? `authored node hint ${hint}` : decision.node.reason,
    }, 'approved');

    if (decision.node.withholdsFragment && brokenCustom && !oldPending) {
      const row = playerViolations.find((v) => v.customKey === brokenCustom);
      pending[target] = { customKey: brokenCustom, honoredAtOpen: row?.honoredCount ?? 0 };
    } else if (atoned && oldPending) {
      delete pending[target];
    }
    if (deliversFact9) fact9Marks[target] = fact9Window;
    state.keeper_last_open_id = event.id;
    result.enqueued += 1;
    result.dirty = true;
    await logEvent('info', 'showrunner.keeper', `event ${event.id} -> ${voiceKey} for ${target} (approved)`);
  }

  state.keeper_fact9_windows = fact9Marks;
  state.keeper_atonement_pending = pending;
  return result;
}
