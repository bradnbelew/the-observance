/**
 * Typed data-access helpers for the Discord bot.
 *
 * Every function here goes through the service-role client (`supabase`), so RLS
 * is bypassed. Errors from Supabase are thrown (callers decide how to surface
 * them to Discord). Reads that find nothing return `null` rather than throwing.
 */
import { supabase } from './client.js';
import type {
  AnswerSurface,
  ArcState,
  BeatQueueRow,
  BeatStatus,
  Hint,
  LogLevel,
  OutcomeBeat,
  Player,
  Puzzle,
  Solve,
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

/**
 * Enqueue a new story beat. The `status` decides whether it fires immediately or
 * waits on the dashboard approval gate:
 *   - 'approved' — PLAYER-EARNED (whisper toll, oracle unlock): fires on the next
 *     plugin poll, never waits on a human. The player did the work; the world owes
 *     them the answer now.
 *   - 'pending'  — CURATORIAL (showrunner CONFIRM mode, dashboard-staged, the
 *     Accepting trigger): the plugin poller ignores it until a human approves it in
 *     the dashboard. This is the gate the plugin's fetchActionableBeats enforces.
 * Defaults to 'pending' (the safe, gated default). Returns the row.
 */
export async function enqueueBeat(
  type: string,
  target: string | null,
  payload: Record<string, unknown> = {},
  status: BeatStatus = 'pending',
): Promise<BeatQueueRow> {
  const { data, error } = await supabase
    .from('beat_queue')
    .insert({ type, target, payload, status })
    .select('id, type, target, payload, status, created_at, decided_at')
    .single<BeatQueueRow>();

  if (error) throw error;
  return data;
}

// ---------------------------------------------------------------------------
// custom_compliance — per-player, per-custom honored/violated tally written by
// the plugin. The customs→report bridge (showrunner/customs.ts, COHERENCE-AUDIT
// P0-4 / D1) READS this; nothing else does. Grounding discipline: a report names
// a player ONLY for a genuinely-measured violation, so this returns the raw
// measured counts and lets the pure ladder policy decide.
// ---------------------------------------------------------------------------

/**
 * One player's measured tally for ONE custom, normalized across the two column
 * conventions in the repo: the plugin (the live writer, CustomComplianceRow)
 * keys on `mc_uuid` and writes `honored_count` / `violated_count` / `name`; the
 * dashboard schema (0001_init) declares `player_id` / `violation_count`. We read
 * `*` and tolerate either so the bridge measures the same truth regardless.
 */
export interface CustomViolation {
  /** Stable per-player grouping key (mc_uuid when present, else player_id). Never empty. */
  groupKey: string;
  /** The opaque custom_key, e.g. "the_bow" (TrackerConfig constants). */
  customKey: string;
  /** Display name for the report, or null if neither a name nor a resolvable player exists. */
  name: string | null;
  /** Times this custom was honored (measured). Used as the grounded "days kept" count. */
  honoredCount: number;
  /** Times this custom was violated (measured). The rung driver — only a >0 count is reportable. */
  violatedCount: number;
}

/**
 * Read every custom_compliance row and project it to {@link CustomViolation}.
 * Fault-isolated + graceful: on ANY error (Supabase down, schema drift) returns
 * `[]` so the bridge simply stays silent rather than throwing into the tick —
 * silence-is-canon (INV-7), and a missing report is always safer than a misfire.
 * Resolves a display name from the embedded `name` (plugin) first; if absent but
 * a `player_id` exists, joins players in one batched lookup.
 */
export async function readCustomViolations(): Promise<CustomViolation[]> {
  try {
    const { data, error } = await supabase
      .from('custom_compliance')
      .select('*')
      .returns<Record<string, unknown>[]>();
    if (error || !data) return [];

    const rows = data.map((r) => {
      const mcUuid = typeof r.mc_uuid === 'string' ? r.mc_uuid : null;
      const playerId = typeof r.player_id === 'string' ? r.player_id : null;
      const groupKey = mcUuid ?? playerId ?? '';
      const customKey = typeof r.custom_key === 'string' ? r.custom_key : '';
      const name = typeof r.name === 'string' && r.name.trim() !== '' ? r.name : null;
      const violatedCount = num(r.violated_count) ?? num(r.violation_count) ?? 0;
      const honoredCount = num(r.honored_count) ?? 0;
      return { groupKey, customKey, name, honoredCount, violatedCount, playerId };
    }).filter((r) => r.groupKey !== '' && r.customKey !== '');

    // Backfill missing names from players (only for rows that carry a player_id
    // but no embedded name — the dashboard-schema case). One batched query.
    const needName = [...new Set(rows.filter((r) => r.name == null && r.playerId).map((r) => r.playerId as string))];
    if (needName.length > 0) {
      const { data: people } = await supabase
        .from('players')
        .select('id, name')
        .in('id', needName)
        .returns<{ id: string; name: string | null }[]>();
      const byId = new Map((people ?? []).map((p) => [p.id, p.name]));
      for (const r of rows) {
        if (r.name == null && r.playerId) r.name = byId.get(r.playerId) ?? null;
      }
    }

    return rows.map(({ groupKey, customKey, name, honoredCount, violatedCount }) => ({
      groupKey, customKey, name, honoredCount, violatedCount,
    }));
  } catch {
    return [];
  }
}

/** Coerce a possibly-undefined jsonb/number cell to a finite number, else undefined. */
function num(v: unknown): number | undefined {
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))) return Number(v);
  return undefined;
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

// ---------------------------------------------------------------------------
// Oracle — the non-linear clue web (0004_oracle.sql).
//
// The resolver path: getOpenPuzzles → matchPuzzle (set-membership of the
// normalized answer) → hasSolved (replay guard) → recordSolve (idempotent) →
// enqueueOracleBeat (status 'approved' — a player reward fires immediately).
// Every attempt is logged via logAttempt for the rate-limiter substrate.
// ---------------------------------------------------------------------------

/**
 * The OPEN puzzles to match an answer against — every row with active = true.
 * The web is non-linear: many are open at once. Selects only what the resolver
 * needs (spoiler columns stay server-side). Returns [] if none.
 */
export async function getOpenPuzzles(): Promise<Puzzle[]> {
  const { data, error } = await supabase
    .from('puzzles')
    .select(
      'puzzle_key, title, accepted_answers, outcome_type, outcome_payload, movement, active, max_attempts',
    )
    .eq('active', true)
    .returns<Puzzle[]>();

  if (error) throw error;
  return data ?? [];
}

/**
 * The first OPEN puzzle whose accepted_answers contains the already-normalized
 * string, or null if none. Whole-string exact set-membership only — never
 * substring or fuzzy. An empty normalized string never matches (guarded by the
 * caller, which does not even call this on empty input).
 */
export function matchPuzzle(
  openPuzzles: readonly Puzzle[],
  normalized: string,
): Puzzle | null {
  if (normalized === '') return null;
  for (const puzzle of openPuzzles) {
    if (puzzle.accepted_answers.includes(normalized)) return puzzle;
  }
  return null;
}

/** True if this player has already solved this puzzle (the replay guard read). */
export async function hasSolved(
  puzzleKey: string,
  playerId: string,
): Promise<boolean> {
  const { count, error } = await supabase
    .from('solves')
    .select('id', { count: 'exact', head: true })
    .eq('puzzle_key', puzzleKey)
    .eq('player_id', playerId);

  if (error) throw error;
  return (count ?? 0) > 0;
}

/**
 * Idempotently record a solve. INSERT … ON CONFLICT (puzzle_key, player_id) DO
 * NOTHING is expressed via upsert + ignoreDuplicates: only a genuinely-new row
 * comes back, so the caller proceeds to the reward ONLY when `true`. A replay
 * (already solved) returns false and fires nothing.
 */
export async function recordSolve(
  puzzleKey: string,
  player: Player,
  attemptCount: number,
): Promise<boolean> {
  const { data, error } = await supabase
    .from('solves')
    .upsert(
      {
        puzzle_key: puzzleKey,
        player_id: player.id,
        mc_uuid: player.mc_uuid,
        discord_id: player.discord_id,
        attempt_count: attemptCount,
      },
      { onConflict: 'puzzle_key,player_id', ignoreDuplicates: true },
    )
    .select('id')
    .maybeSingle<Pick<Solve, 'id'>>();

  if (error) throw error;
  // ignoreDuplicates: a conflict returns no row → already solved (not new).
  return data !== null;
}

/**
 * Append an answer_attempts row (audit + rate-limit substrate). Records the
 * whole normalized string and a single `matched` bool — NEVER which part was
 * right. Best-effort: swallows its own failure so logging never throws into a
 * handler (a logging hiccup must not break the loop or leak an error to players).
 */
export async function logAttempt(attempt: {
  puzzleKey: string | null;
  playerId: string | null;
  mcUuid: string | null;
  discordId: string | null;
  surface: AnswerSurface;
  raw: string;
  normalized: string;
  matched: boolean;
}): Promise<void> {
  const { error } = await supabase.from('answer_attempts').insert({
    puzzle_key: attempt.puzzleKey,
    player_id: attempt.playerId,
    mc_uuid: attempt.mcUuid,
    discord_id: attempt.discordId,
    surface: attempt.surface,
    raw: attempt.raw,
    normalized: attempt.normalized,
    matched: attempt.matched,
  });

  if (error) {
    console.error('[repo.logAttempt] failed to write answer_attempts:', error.message);
  }
}

/**
 * How many attempts a player has made within the last `windowMs`, for the
 * rate-limiter's token bucket. Counts by player_id when linked, else by
 * discord_id (unlinked #the-record scanners are still throttled). Per-puzzle
 * scoping is optional (pass puzzleKey to count tries on ONE puzzle for its
 * max_attempts cap). Fails OPEN to 0 on a DB hiccup so an outage never silently
 * locks a player out — the world simply stays quiet on a true miss anyway.
 */
export async function countRecentAttempts(opts: {
  playerId: string | null;
  discordId: string | null;
  windowMs: number;
  puzzleKey?: string;
}): Promise<number> {
  const since = new Date(Date.now() - opts.windowMs).toISOString();
  let query = supabase
    .from('answer_attempts')
    .select('id', { count: 'exact', head: true })
    .gte('at', since);

  if (opts.playerId) {
    query = query.eq('player_id', opts.playerId);
  } else if (opts.discordId) {
    query = query.eq('discord_id', opts.discordId);
  } else {
    return 0;
  }

  if (opts.puzzleKey) query = query.eq('puzzle_key', opts.puzzleKey);

  const { count, error } = await query;
  if (error) {
    console.error('[repo.countRecentAttempts] failed:', error.message);
    return 0; // fail-open: never lock a player out on a DB stumble.
  }
  return count ?? 0;
}

/**
 * Enqueue a player-earned in-world beat at status 'approved' so it fires on the
 * plugin's NEXT poll with no human gate (player rewards never wait on a
 * showrunner — the CONFIRM/'pending' path is reserved for authored/AI beats).
 *
 * The "{solver}" placeholder in `beat.mc_uuid` is resolved to the solving
 * player's real mc_uuid here, so one authored row rewards whoever solves it.
 * Writes the new beat_queue columns (mc_uuid / site_id / priority) the plugin
 * reads; leaves the legacy `target` null. Returns the inserted row.
 */
export async function enqueueOracleBeat(
  beat: OutcomeBeat,
  solverMcUuid: string,
): Promise<BeatQueueRow> {
  const mcUuid =
    beat.mc_uuid === '{solver}' ? solverMcUuid : (beat.mc_uuid ?? null);

  const { data, error } = await supabase
    .from('beat_queue')
    .insert({
      type: beat.type,
      target: null,
      mc_uuid: mcUuid,
      site_id: beat.site_id ?? null,
      priority: beat.priority ?? null,
      payload: beat.payload ?? {},
      status: 'approved' satisfies BeatStatus,
    })
    .select('id, type, target, payload, status, created_at, decided_at')
    .single<BeatQueueRow>();

  if (error) throw error;
  return data;
}

/**
 * Merge keys into arc_state.flags (single-row, id = 1). Read-modify-write of the
 * jsonb flags blob — used by outcomes that carry `set_flags`. Best-effort within
 * the resolver's try/catch.
 */
export async function setArcFlags(
  flags: Record<string, unknown>,
): Promise<void> {
  const { data, error: readErr } = await supabase
    .from('arc_state')
    .select('flags')
    .eq('id', 1)
    .maybeSingle<Pick<ArcState, 'flags'>>();

  if (readErr) throw readErr;

  const merged = { ...(data?.flags ?? {}), ...flags };
  const { error } = await supabase
    .from('arc_state')
    .update({ flags: merged })
    .eq('id', 1);

  if (error) throw error;
}
