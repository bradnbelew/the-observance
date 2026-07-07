import "server-only";

import { getOracleClient } from "./oracle-client";
import { flagsSatisfied, type RecordPuzzle } from "./gate";
import { earnedTier, stallLabel, stallMsSinceLastAdvance } from "./integrity-policy";

/**
 * integrity.ts - the SERVER-ONLY "integrity check / error log" that IS the hint rail.
 *
 * The Record terminal surfaces escalating warnings for open, unsolved puzzles. It uses the same hint
 * table as the in-world whisper rail, but only up to the tier earned by elapsed group stall. It never
 * shows sealed puzzles, never reveals an answer, and fails soft to an empty log.
 */

/** One surfaced integrity warning: a corrupted entry the checker is escalating. */
export interface IntegrityWarning {
  /** the thread this stall belongs to (for grouping in the UI). */
  threadKey: string | null;
  /** the tier surfaced (1..3); higher = clearer, only as far as the stall has earned. */
  tier: number;
  /** the authored hint body at that tier (a nudge; never the answer). */
  body: string;
  /** coarse stall label for the error-log line (e.g. "stalled 2d"). */
  stall: string;
}

interface HintRow {
  puzzle_key: string;
  tier: number;
  body: string;
}

interface SolveTimeRow {
  puzzle_key: string;
  solved_at: string;
}

async function readFlags(client: NonNullable<ReturnType<typeof getOracleClient>>): Promise<Record<string, unknown>> {
  try {
    const { data } = await client
      .from("arc_state")
      .select("flags")
      .eq("id", 1)
      .maybeSingle<{ flags: Record<string, unknown> | null }>();
    return data?.flags ?? {};
  } catch {
    return {};
  }
}

/**
 * Build the integrity/error log. For each open, unsolved puzzle, compute the group stall from the web's
 * last advance and surface the authored hint at the earned tier, if one is seeded.
 */
export async function readIntegrityLog(now: number = Date.now()): Promise<IntegrityWarning[]> {
  const client = getOracleClient();
  if (!client) return [];

  try {
    const flags = await readFlags(client);

    const [puzzlesRes, solvesRes] = await Promise.all([
      client
        .from("puzzles")
        .select("puzzle_key, accepted_answers, outcome_type, outcome_payload, active, max_attempts, requires_flags, thread_key")
        .eq("active", true)
        .returns<RecordPuzzle[]>(),
      client.from("solves").select("puzzle_key, solved_at").returns<SolveTimeRow[]>(),
    ]);

    const open = (puzzlesRes.data ?? []).filter((p) => flagsSatisfied(p.requires_flags, flags));
    if (open.length === 0) return [];

    const solves = solvesRes.data ?? [];
    const solvedKeys = new Set(solves.map((s) => s.puzzle_key));

    // No prior solve stays tier 1. The terminal can say "unresolved" without handing out near-plain
    // hints before the group has earned any progress.
    let lastAdvanceMs = 0;
    for (const s of solves) {
      const t = Date.parse(s.solved_at);
      if (Number.isFinite(t) && t > lastAdvanceMs) lastAdvanceMs = t;
    }
    const stallMs = stallMsSinceLastAdvance(lastAdvanceMs, now);
    const tier = earnedTier(stallMs);

    const unresolved = open.filter((p) => !solvedKeys.has(p.puzzle_key));
    if (unresolved.length === 0) return [];

    const keys = unresolved.map((p) => p.puzzle_key);
    const { data: hintRows } = await client
      .from("hints")
      .select("puzzle_key, tier, body")
      .in("puzzle_key", keys)
      .lte("tier", tier)
      .returns<HintRow[]>();

    const bestByPuzzle = new Map<string, HintRow>();
    for (const h of hintRows ?? []) {
      const cur = bestByPuzzle.get(h.puzzle_key);
      if (!cur || h.tier > cur.tier) bestByPuzzle.set(h.puzzle_key, h);
    }

    const stall = stallLabel(stallMs);

    const warnings: IntegrityWarning[] = [];
    for (const p of unresolved) {
      const hint = bestByPuzzle.get(p.puzzle_key);
      if (!hint) continue;
      warnings.push({
        threadKey: p.thread_key,
        tier: hint.tier,
        body: hint.body,
        stall,
      });
    }

    warnings.sort((a, b) => b.tier - a.tier);
    return warnings;
  } catch {
    return [];
  }
}
