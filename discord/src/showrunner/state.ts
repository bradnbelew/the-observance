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

  // -------------------------------------------------------------------------
  // BETWEEN-SESSION AUTONOMY bookkeeping (A2/A3/A8–A13, B3/B4). Stored on this
  // same jsonb row so the autonomy layer needs NO migration today (mirrors
  // `reported_customs`). When the SQL lane lands the dedicated tables
  // (grave_state, group_restraint_state, player_visited_cells, …) the run
  // wrappers can be re-pointed; until then this is the durable, idempotent home.
  // -------------------------------------------------------------------------

  /** A10 difficulty hysteresis: the persisted grip state + when it was entered. */
  reckoning_state?: 'tight' | 'even' | 'loose';
  reckoning_since_ms?: number | null;

  /** B4 cold-start: has the one-shot `recordOpened` ack already posted? (prologue idempotency). */
  prologue_acked?: boolean;

  /** A3 Hold-Book: per-player enrolment tier high-water (groupKey → tier ordinal 1..3). */
  keeper_record?: Record<string, number>;

  /** A9 grave: the two one-shot rows' fired-state + the bound subject (one grave per arc). */
  grave?: { carved?: boolean; opened?: boolean; group_key?: string; name?: string };

  /** A12 herd: the pale-cosmetic high-water + the last movement a spread pass ran. */
  herd_pale_count?: number;
  herd_pass_movement?: number;

  /** A8 name-where: the cells already used for a carve this arc (one carve per cell). */
  carved_cells?: string[];
  /** A8 name-where: per-player carve count (chorus rotation — fewest-first). */
  carve_counts?: Record<string, number>;

  /** B3 offline-skin: per-player worn-count by phase (`${groupKey}|${phase}` → count). */
  worn_skins?: Record<string, number>;

  /** A2 fate: the set-once ending fate (mirrors arc_state.ending_fate; cached for the health panel). */
  ending_fate?: 'kept' | 'cast_out' | 'divided' | 'refusers';

  /** D1 reports: per-player dominant-habit ordinal already dripped (re-fire only on a habit change). */
  reported_habits?: Record<string, number>;

  /** W4 observer: epoch ms of the last "it heard you" echo (the sparse-rate high-water). */
  observer_last_ms?: number;

  /** W9 observer: set once the channel is closed post-reckoning (condemn/free) so the "record goes
   *  quiet" line is spoken exactly once and the echoes then cease. */
  observer_silenced?: boolean;

  /** D4 liar: the Iss warm-beat ids already re-staged cold (one-way high-water). */
  liar_flipped?: string[];

  /**
   * D3 companion (Wren, the-companion.md). Idempotency high-waters for the NEW companion consumers.
   * These are DISTINCT from `reckoning_state`/`reckoning_since_ms` above — those belong to the
   * DIFFICULTY grip engine (reckoning.ts) and are unrelated to Wren's condemn/understand/free arc
   * flags (the naming-collision guard in the wiring spec). `companion_lines_delivered` is the set of
   * one-shot Wren line keys (reveal.yes / reveal.tally / roster.newhand) already enqueued;
   * `companion_reckoning_delivered` marks his single last-words node fired (one-of-three, set-once).
   */
  companion_lines_delivered?: string[];
  companion_reckoning_delivered?: boolean;

  /** M5 finale: has the composed close already been posted to #the-record? (set-once, idempotent). */
  finale_posted?: boolean;

  /**
   * D7 conductor: the single-arbiter apparition claim for the CURRENT window + the per-player
   * apparition counts (the per-player cap). `claim_window` is the window seed the claim was made for,
   * so a re-run in the same window re-derives the same claim instead of making a second one. The
   * deferring lanes (offline-skin, name-where, keeper-NPC, the Ear) read `claim` before firing (INV-18).
   */
  apparition_claim?: { window: number; group_key: string; shape: string; beat: string } | null;
  apparition_counts?: Record<string, number>;
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
