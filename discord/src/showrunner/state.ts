/**
 * Showrunner persistent state — stored as ONE jsonb row in the existing `settings` table
 * (key = 'showrunner_state'), so the spine needs NO new migration. Also the typed reader for the
 * two scalar settings the spine consults (`watcher_sleep`, `showrunner_mode`). The dashboard reads
 * the same row for its health panel + the manual-drip queue.
 */
import { supabase } from '../db/client.js';

const STATE_KEY = 'showrunner_state';

export interface PendingDrip {
  puzzle_key: string;
  movement: number;
  staged_iso: string;
}

export interface ShowrunnerState {
  /** ISO of the last tick (health: "showrunner last ran"). */
  last_run_iso?: string;
  /** epoch ms of the last drip (posted OR staged) — the drip cadence anchor. */
  last_drip_at_ms?: number | null;
  /** puzzle_keys already announced (posted or staged) — each clue drips once. */
  dripped_keys?: string[];
  /** CONFIRM-mode drips awaiting the dashboard's manual "post" button. */
  pending_drips?: PendingDrip[];
  /**
   * Customs-bridge idempotency high-water marks (COHERENCE-AUDIT P0-4 / D1).
   * Key = `${groupKey}|${custom_key}`, value = the highest violated_count already
   * reported for that (player, custom). A rung is only re-fired when the measured
   * count rises past this mark, so the bridge never re-reports the same violation
   * every cadence. Absent/empty on a fresh deploy.
   */
  reported_customs?: Record<string, number>;
}

/** Read a single settings value (jsonb) by key, or `fallback` if absent. */
export async function readSetting<T>(key: string, fallback: T): Promise<T> {
  const { data, error } = await supabase
    .from('settings')
    .select('value')
    .eq('key', key)
    .maybeSingle<{ value: T }>();
  if (error) throw error;
  return (data?.value ?? fallback) as T;
}

/** Upsert a single settings value (jsonb) by key. */
export async function writeSetting(key: string, value: unknown, nowIso: string): Promise<void> {
  const { error } = await supabase
    .from('settings')
    .upsert({ key, value, updated_at: nowIso }, { onConflict: 'key' });
  if (error) throw error;
}

export async function readState(): Promise<ShowrunnerState> {
  return readSetting<ShowrunnerState>(STATE_KEY, {});
}

export async function writeState(state: ShowrunnerState, nowIso: string): Promise<void> {
  return writeSetting(STATE_KEY, state, nowIso);
}
