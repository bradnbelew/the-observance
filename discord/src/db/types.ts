/**
 * Minimal TS row types for the tables the Discord bot touches.
 *
 * These mirror the canonical schema in
 *   dashboard/supabase/migrations/0001_init.sql
 * plus discord/supabase/migrations/0003_discord.sql.
 * They are intentionally narrow — only columns the bot reads or writes.
 */

/** public.players — adds `discord_id` (0003) to link a Discord user. */
export interface Player {
  id: string; // uuid
  mc_uuid: string;
  name: string;
  /** Linked Discord user id, or null if never linked. */
  discord_id: string | null;
}

/** public.whisper_budgets — per (player, act) spend tracking. PK is `id`. */
export interface WhisperBudget {
  id: number;
  player_id: string; // uuid
  act: number;
  budget: number;
  spent: number;
  earned: number;
}

/** public.whisper_events — append-only log of delivered whispers. */
export interface WhisperEvent {
  id: number;
  player_id: string; // uuid
  puzzle_key: string | null;
  tier: number | null;
  created_at: string; // timestamptz (ISO)
}

/** public.beat_queue — story beats awaiting approval / firing. */
export type BeatStatus = 'pending' | 'approved' | 'skipped' | 'fired';

export interface BeatQueueRow {
  id: number;
  type: string;
  target: string | null;
  payload: Record<string, unknown>;
  status: BeatStatus;
  created_at: string; // timestamptz (ISO)
  decided_at: string | null;
}

/** public.event_log — structured operational log. */
export type LogLevel = 'info' | 'warn' | 'error';

export interface EventLogRow {
  id: number;
  level: LogLevel;
  source: string | null;
  message: string | null;
  created_at: string; // timestamptz (ISO)
}

/** public.settings — key/jsonb control rows. */
export interface SettingRow {
  key: string;
  value: unknown; // jsonb
  updated_at: string; // timestamptz (ISO)
}

/** public.arc_state — single-row arc cursor (id is always 1). */
export interface ArcState {
  id: 1;
  current_act: number;
  gates: Record<string, unknown>;
  flags: Record<string, unknown>;
  updated_at: string; // timestamptz (ISO)
}

/** public.hints — pre-authored whisper hints, keyed by (puzzle_key, tier). */
export interface Hint {
  id: number;
  puzzle_key: string;
  tier: number;
  body: string;
}
