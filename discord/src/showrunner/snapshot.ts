/**
 * snapshot.ts — read the world into an immutable {@link Snapshot} for the pure {@link decide}. All the
 * DB I/O lives here; decide() never touches Supabase. Reuses the bot's repo helpers + a few direct
 * reads. Thresholds are module constants (tunable later via settings without changing decide()).
 */
import { supabase } from '../db/client.js';
import { getArcAct, getBudget, countWhispersForPuzzle, getHint } from '../db/repo.js';
import { readSetting, readState } from './state.js';
import type { AttempterState, ShowrunnerMode, Snapshot, SnapshotPuzzle } from './types.js';

const HOUR = 3_600_000;
/** "Stuck" is measured over a long session window, not days — the within-session backstop the critics asked for. */
export const STALL_WINDOW_MS = 3 * HOUR;
export const STALL_FAILED_THRESHOLD = 5;
export const DRIP_INTERVAL_MS = 20 * HOUR;

export async function buildSnapshot(nowMs: number): Promise<Snapshot> {
  const sinceIso = new Date(nowMs - STALL_WINDOW_MS).toISOString();

  const [asleep, modeRaw, currentAct, state] = await Promise.all([
    readSetting<boolean>('watcher_sleep', false),
    readSetting<string>('showrunner_mode', 'confirm'),
    getArcAct(),
    readState(),
  ]);
  const mode: ShowrunnerMode = modeRaw === 'auto' ? 'auto' : 'confirm';
  const drippedKeys = new Set(state.dripped_keys ?? []);

  const { data: puzzleRows, error: pErr } = await supabase
    .from('puzzles')
    .select('puzzle_key, movement')
    .eq('active', true)
    .returns<{ puzzle_key: string; movement: number | null }[]>();
  if (pErr) throw pErr;

  const openPuzzles: SnapshotPuzzle[] = [];
  for (const row of puzzleRows ?? []) {
    const key = row.puzzle_key;

    const [{ count: failedCount }, { count: solvedCount }, { data: attemptRows }] = await Promise.all([
      supabase.from('answer_attempts').select('id', { count: 'exact', head: true })
        .eq('puzzle_key', key).eq('matched', false).gte('at', sinceIso),
      supabase.from('solves').select('id', { count: 'exact', head: true })
        .eq('puzzle_key', key).gte('solved_at', sinceIso),
      supabase.from('answer_attempts').select('player_id')
        .eq('puzzle_key', key).eq('matched', false).gte('at', sinceIso).not('player_id', 'is', null),
    ]);

    const distinct = [...new Set(((attemptRows ?? []) as { player_id: string | null }[])
      .map((r) => r.player_id).filter((id): id is string => !!id))];

    const attempters: AttempterState[] = [];
    for (const playerId of distinct) {
      const budget = await getBudget(playerId, currentAct);
      const whisperRemaining = budget ? budget.budget + budget.earned - budget.spent : 0;
      const nextTier = (await countWhispersForPuzzle(playerId, key)) + 1;
      const hint = await getHint(key, nextTier);
      attempters.push({ playerId, act: currentAct, whisperRemaining, nextTier, nextTierHintExists: hint != null });
    }

    openPuzzles.push({
      puzzleKey: key,
      movement: row.movement ?? 0,
      failedAttemptsInWindow: failedCount ?? 0,
      solvedInWindow: (solvedCount ?? 0) > 0,
      attempters,
      dripped: drippedKeys.has(key),
    });
  }

  return {
    nowMs,
    asleep,
    mode,
    currentAct,
    openPuzzles,
    lastDripAtMs: state.last_drip_at_ms ?? null,
    stallFailedThreshold: STALL_FAILED_THRESHOLD,
    dripIntervalMs: DRIP_INTERVAL_MS,
  };
}
