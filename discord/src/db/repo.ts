/**
 * Typed data-access helpers for the Discord bot.
 *
 * Every function here goes through the service-role client (`supabase`), so RLS
 * is bypassed. Errors from Supabase are thrown (callers decide how to surface
 * them to Discord). Reads that find nothing return `null` rather than throwing.
 */
import { supabase } from './client.js';
import type {
  ArcState,
  BeatQueueRow,
  BeatStatus,
  Hint,
  LogLevel,
  Player,
  WhisperBudget,
} from './types.js';

/** Result of attempting to spend one whisper from a player's act budget. */
export interface SpendWhisperResult {
  /** True if a whisper was spent; false if no budget row or none remaining. */
  ok: boolean;
  /** Reason when `ok` is false. */
  reason?: 'no-budget' | 'exhausted';
  /** Remaining whispers after the spend (budget + earned - spent). */
  remaining: number;
  /** The budget row after the attempt, when one exists. */
  budget: WhisperBudget | null;
}

/** Look up a player by their linked Discord user id. Returns null if unlinked. */
export async function getPlayerByDiscordId(
  discordId: string,
): Promise<Player | null> {
  const { data, error } = await supabase
    .from('players')
    .select('id, mc_uuid, name, discord_id')
    .eq('discord_id', discordId)
    .maybeSingle<Player>();

  if (error) throw error;
  return data ?? null;
}

/**
 * Link a Discord user to an existing Minecraft player, matched by case-insensitive
 * in-game name. Sets `players.discord_id`. Returns the updated player, or null if
 * no player with that name exists.
 */
export async function linkDiscord(
  discordId: string,
  mcName: string,
): Promise<Player | null> {
  const { data: found, error: findErr } = await supabase
    .from('players')
    .select('id, mc_uuid, name, discord_id')
    .ilike('name', mcName)
    .maybeSingle<Player>();

  if (findErr) throw findErr;
  if (!found) return null;

  const { data, error } = await supabase
    .from('players')
    .update({ discord_id: discordId })
    .eq('id', found.id)
    .select('id, mc_uuid, name, discord_id')
    .single<Player>();

  if (error) throw error;
  return data;
}

/** Current act number from the single-row arc_state (id = 1). Defaults to 1. */
export async function getArcAct(): Promise<number> {
  const { data, error } = await supabase
    .from('arc_state')
    .select('current_act')
    .eq('id', 1)
    .maybeSingle<Pick<ArcState, 'current_act'>>();

  if (error) throw error;
  return data?.current_act ?? 1;
}

/** Whisper budget row for a player in a given act, or null if none exists. */
export async function getBudget(
  playerId: string,
  act: number,
): Promise<WhisperBudget | null> {
  const { data, error } = await supabase
    .from('whisper_budgets')
    .select('id, player_id, act, budget, spent, earned')
    .eq('player_id', playerId)
    .eq('act', act)
    .maybeSingle<WhisperBudget>();

  if (error) throw error;
  return data ?? null;
}

/**
 * Spend one whisper from a player's (act) budget.
 *
 * Allowance = budget + earned. A whisper is spendable while `spent < allowance`.
 * On success this increments `spent` (guarded by a `spent` equality check to
 * avoid lost updates under concurrency) and returns the remaining count. It does
 * NOT write a whisper_events row — record delivery separately once the hint body
 * is known (see `recordWhisperEvent`).
 */
export async function spendWhisper(
  playerId: string,
  act: number,
): Promise<SpendWhisperResult> {
  const current = await getBudget(playerId, act);

  if (!current) {
    return { ok: false, reason: 'no-budget', remaining: 0, budget: null };
  }

  const allowance = current.budget + current.earned;
  const remainingBefore = allowance - current.spent;

  if (remainingBefore <= 0) {
    return { ok: false, reason: 'exhausted', remaining: 0, budget: current };
  }

  // Optimistic concurrency: only bump if `spent` is still what we read.
  const { data, error } = await supabase
    .from('whisper_budgets')
    .update({ spent: current.spent + 1 })
    .eq('id', current.id)
    .eq('spent', current.spent)
    .select('id, player_id, act, budget, spent, earned')
    .maybeSingle<WhisperBudget>();

  if (error) throw error;

  // Lost the race (another spend landed first) — report no spend this call.
  if (!data) {
    const refreshed = await getBudget(playerId, act);
    const refRemaining = refreshed
      ? refreshed.budget + refreshed.earned - refreshed.spent
      : 0;
    return {
      ok: false,
      reason: refRemaining <= 0 ? 'exhausted' : undefined,
      remaining: Math.max(0, refRemaining),
      budget: refreshed,
    };
  }

  return {
    ok: true,
    remaining: data.budget + data.earned - data.spent,
    budget: data,
  };
}

/**
 * How many whispers this player has already been given for one puzzle. Used to
 * derive the next tier: tier = count + 1 (first whisper is tier 1). Counts every
 * recorded whisper_events row for (player, puzzle), regardless of act.
 */
export async function countWhispersForPuzzle(
  playerId: string,
  puzzleKey: string,
): Promise<number> {
  const { count, error } = await supabase
    .from('whisper_events')
    .select('id', { count: 'exact', head: true })
    .eq('player_id', playerId)
    .eq('puzzle_key', puzzleKey);

  if (error) throw error;
  return count ?? 0;
}

/** Append a whisper_events row recording a delivered hint. */
export async function recordWhisperEvent(
  playerId: string,
  puzzleKey: string,
  tier: number,
): Promise<void> {
  const { error } = await supabase
    .from('whisper_events')
    .insert({ player_id: playerId, puzzle_key: puzzleKey, tier });

  if (error) throw error;
}

/** Pre-authored hint body for (puzzle_key, tier), or null if not seeded. */
export async function getHint(
  puzzleKey: string,
  tier: number,
): Promise<Hint | null> {
  const { data, error } = await supabase
    .from('hints')
    .select('id, puzzle_key, tier, body')
    .eq('puzzle_key', puzzleKey)
    .eq('tier', tier)
    .maybeSingle<Hint>();

  if (error) throw error;
  return data ?? null;
}

/** Enqueue a new story beat (status defaults to 'pending'). Returns the row. */
export async function enqueueBeat(
  type: string,
  target: string | null,
  payload: Record<string, unknown> = {},
): Promise<BeatQueueRow> {
  const { data, error } = await supabase
    .from('beat_queue')
    .insert({ type, target, payload, status: 'pending' satisfies BeatStatus })
    .select('id, type, target, payload, status, created_at, decided_at')
    .single<BeatQueueRow>();

  if (error) throw error;
  return data;
}

/** Write a row to event_log. Best-effort: swallows its own failure so logging
 *  never throws into a command handler. */
export async function logEvent(
  level: LogLevel,
  source: string,
  message: string,
): Promise<void> {
  const { error } = await supabase
    .from('event_log')
    .insert({ level, source, message });

  if (error) {
    // Never let logging crash a handler — surface to stderr instead.
    console.error('[repo.logEvent] failed to write event_log:', error.message);
  }
}
