import "server-only";

import { getOracleClient } from "./oracle-client";
import { flagsSatisfied, type RecordPuzzle } from "./gate";

/**
 * integrity.ts — the SERVER-ONLY "integrity check / error log" that IS the hint rail (A5 / A8 / L4).
 *
 * THE REFRAME. In the recovered-archive fiction, a stalled thread is a CORRUPTED / UNRESOLVED entry.
 * The record's integrity checker surfaces escalating warnings the longer a thread stalls — which is
 * the same escalation the in-world whisper hint rail delivers, off the SAME `hints` table (the single
 * source; INTEGRATION Layer 5). t1 is a bare "unresolved — cross-reference incomplete"; higher tiers
 * spell the nudge progressively plainer (L4).
 *
 * STALL = TIME SINCE THE LAST SOLVE ON AN OPEN PUZZLE. We never store a per-puzzle clock, so we derive
 * the stall from the newest solve's timestamp among a puzzle's context; the longer since the web last
 * advanced, the higher the integrity tier the record is willing to surface. This is a coarse,
 * whole-group escalation (not per-player) — the record speaks only of the group.
 *
 * SPOILER DISCIPLINE. Even here, the record shows a hint ONLY up to the tier the stall has earned, and
 * ONLY for OPEN puzzles (active + gate satisfied). A sealed/inactive puzzle's hints never surface —
 * it is indistinguishable from a puzzle that does not exist. The hint BODY is authored spoiler-aware
 * content seeded from the sealed arc; we surface it verbatim but only when earned, and we never reveal
 * the answer itself (hints are nudges, not solutions — PUZZLES §7).
 *
 * Fail-soft: missing env / no hints seeded / read error → an empty log ("integrity: nominal"), never
 * an error and never a leaked default.
 */

/** How long (ms) a thread must be stalled before each integrity tier is willing to surface. */
const TIER_STALL_MS: Record<number, number> = {
  1: 0, //           t1 shows as soon as a puzzle is open + unsolved (the bare "unresolved" flag).
  2: 24 * 3_600_000, // t2 after ~1 day of no advance on that thread.
  3: 72 * 3_600_000, // t3 (nearly-plain) after ~3 days stalled.
};
const MAX_TIER = 3;

/** One surfaced integrity warning: a corrupted entry the checker is escalating. */
export interface IntegrityWarning {
  /** the thread this stall belongs to (for grouping in the UI). */
  threadKey: string | null;
  /** the tier surfaced (1..3) — higher = clearer, only as far as the stall has earned. */
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
    const { data } = await client.from("arc_state").select("flags").eq("id", 1).maybeSingle<{ flags: Record<string, unknown> | null }>();
    return data?.flags ?? {};
  } catch {
    return {};
  }
}

/** Coarse human stall label from a duration in ms. */
function stallLabel(ms: number): string {
  if (ms <= 0) return "unresolved";
  const days = Math.floor(ms / 86_400_000);
  if (days >= 1) return `stalled ${days}d`;
  const hours = Math.floor(ms / 3_600_000);
  if (hours >= 1) return `stalled ${hours}h`;
  return "stalled <1h";
}

/** The highest tier the elapsed stall has earned (1..MAX_TIER). */
function earnedTier(stallMs: number): number {
  let t = 1;
  for (let k = 2; k <= MAX_TIER; k++) {
    if (stallMs >= (TIER_STALL_MS[k] ?? Infinity)) t = k;
  }
  return t;
}

/**
 * Build the integrity/error log. For each OPEN, UNSOLVED puzzle, compute the group stall (time since
 * the most recent solve anywhere — the web's last advance) and surface the authored hint at the
 * earned tier, if one is seeded. Deduped per (puzzle) to the single earned tier.
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

    // The web's last advance: newest solved_at anywhere. If nothing has ever solved, stall is "since
    // the record opened" — we treat that as maximally stalled (t3 willing), so a cold web still guides.
    let lastAdvanceMs = 0;
    for (const s of solves) {
      const t = Date.parse(s.solved_at);
      if (Number.isFinite(t) && t > lastAdvanceMs) lastAdvanceMs = t;
    }
    const stallMs = lastAdvanceMs === 0 ? Infinity : Math.max(0, now - lastAdvanceMs);
    const tier = stallMs === Infinity ? MAX_TIER : earnedTier(stallMs);

    // Unsolved open puzzles are the corrupted/unresolved entries. Fetch their hints at the earned
    // tier (and below, so we can fall back if the exact tier isn't seeded).
    const unresolved = open.filter((p) => !solvedKeys.has(p.puzzle_key));
    if (unresolved.length === 0) return [];

    const keys = unresolved.map((p) => p.puzzle_key);
    const { data: hintRows } = await client
      .from("hints")
      .select("puzzle_key, tier, body")
      .in("puzzle_key", keys)
      .lte("tier", tier)
      .returns<HintRow[]>();

    // Pick, per puzzle, the highest seeded tier ≤ earned. (t1 exists for most; higher tiers escalate.)
    const bestByPuzzle = new Map<string, HintRow>();
    for (const h of hintRows ?? []) {
      const cur = bestByPuzzle.get(h.puzzle_key);
      if (!cur || h.tier > cur.tier) bestByPuzzle.set(h.puzzle_key, h);
    }

    const stall = stallLabel(stallMs === Infinity ? Number.MAX_SAFE_INTEGER : stallMs);

    const warnings: IntegrityWarning[] = [];
    for (const p of unresolved) {
      const hint = bestByPuzzle.get(p.puzzle_key);
      if (!hint) continue; // no seeded hint → the checker stays silent on that entry (no invented nudge).
      warnings.push({
        threadKey: p.thread_key,
        tier: hint.tier,
        body: hint.body,
        stall,
      });
    }

    // Most-escalated first (the loudest corruption warnings lead the log).
    warnings.sort((a, b) => b.tier - a.tier);
    return warnings;
  } catch {
    return [];
  }
}
