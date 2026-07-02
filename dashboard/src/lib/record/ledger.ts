import "server-only";

import { getOracleClient } from "./oracle-client";
import { flagsSatisfied, type RecordPuzzle } from "./gate";

/**
 * ledger.ts — the SERVER-ONLY read that fills "the record" with names + safe progress.
 *
 * THE ARTIFACT (CHANGE-MANIFEST A5 / L4, INTEGRATION Layer 5): the record website is a half-corrupted
 * archive terminal whose ledger "fills with names" as keepers act, and whose threads show overall
 * progress. This module reads that state server-side (service role, RLS-bypass) and projects it to a
 * SAFE, NON-SPOILER shape the browser is allowed to see.
 *
 * WHAT IS SAFE (the whole discipline). Names + counts + coarse thread progress ONLY. NEVER:
 *   - a puzzle's `accepted_answers` (the answer),
 *   - a hint body (a spoiler),
 *   - which specific puzzle a name solved (that would map a name → an answer they hold),
 *   - a sealed `requires_flags` gate's contents.
 * A name in the ledger says only "this keeper is entered, and has N marks kept" — never WHICH marks.
 *
 * Fail-soft: any missing env / read error degrades to the EMPTY archive (a fresh, un-recovered
 * terminal), never an error and never a leaked default. The record is a found object; a dark backend
 * reads as "nothing recovered under this name yet."
 */

/** One entered keeper as the ledger is allowed to show them: a name + how many marks they've kept. */
export interface LedgerName {
  /** the keeper's in-world name. the ONLY identity the record files for the living (INV-16 caveat:
   *  the record shows the group's roster + counts, never a per-name dossier or which answer they hold). */
  name: string;
  /** how many puzzles this keeper has solved (a count, never WHICH). a mark of participation. */
  kept: number;
}

/** Coarse progress of one reconstruction thread — filled bars, never card contents. */
export interface ThreadProgress {
  key: string;
  label: string;
  color: string;
  /** solves attributed to this thread's open puzzles (coarse fill), clamped ≥ 0. */
  resolved: number;
  /** total currently-open puzzles tagged to this thread (the denominator, coarse). */
  open: number;
}

/** The whole safe ledger the browser receives. Nothing here is a spoiler. */
export interface LedgerProjection {
  /** entered keepers, most-kept first, then alphabetical. names + counts only. */
  names: LedgerName[];
  /** total marks kept across all keepers (the record's "count"). */
  totalKept: number;
  /** the five reconstruction threads, coarse fill. */
  threads: ThreadProgress[];
  /** how many puzzles are currently OPEN (active + flag-gate satisfied) — the live web breadth. */
  openPuzzles: number;
  /** true if the backend was unreachable/empty → the sealed baseline was served. */
  sealed: boolean;
}

const EMPTY: LedgerProjection = {
  names: [],
  totalKept: 0,
  threads: [],
  openPuzzles: 0,
  sealed: true,
};

interface SolveRow {
  puzzle_key: string;
  player_id: string;
}
interface PlayerRow {
  id: string;
  name: string | null;
}
interface ThreadRow {
  thread_key: string;
  label: string;
  color: string;
  sort_order: number;
}

/** Read the live arc flags (single row id=1). Missing/error → {} (everything gated stays CLOSED). */
async function readFlags(client: NonNullable<ReturnType<typeof getOracleClient>>): Promise<Record<string, unknown>> {
  try {
    const { data, error } = await client
      .from("arc_state")
      .select("flags")
      .eq("id", 1)
      .maybeSingle<{ flags: Record<string, unknown> | null }>();
    if (error || !data) return {};
    return data.flags ?? {};
  } catch {
    return {};
  }
}

/** The OPEN puzzles (active=true AND requires_flags satisfied) — the same gate the oracle applies. */
async function readOpenPuzzles(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  flags: Record<string, unknown>,
): Promise<RecordPuzzle[]> {
  try {
    const { data, error } = await client
      .from("puzzles")
      .select(
        "puzzle_key, accepted_answers, outcome_type, outcome_payload, active, max_attempts, requires_flags, thread_key",
      )
      .eq("active", true)
      .returns<RecordPuzzle[]>();
    if (error || !data) return [];
    return data.filter((p) => flagsSatisfied(p.requires_flags, flags));
  } catch {
    return [];
  }
}

/**
 * Build the safe ledger. Reads (server-side) players, solves, threads, and the open web; projects to
 * names+counts+coarse thread fill and NOTHING spoiler. Any failure → the sealed empty baseline.
 */
export async function readLedger(): Promise<LedgerProjection> {
  const client = getOracleClient();
  if (!client) return EMPTY;

  try {
    const flags = await readFlags(client);

    const [solvesRes, threadsRes, openPuzzles] = await Promise.all([
      client.from("solves").select("puzzle_key, player_id").returns<SolveRow[]>(),
      client
        .from("threads")
        .select("thread_key, label, color, sort_order")
        .order("sort_order", { ascending: true })
        .returns<ThreadRow[]>(),
      readOpenPuzzles(client, flags),
    ]);

    const solves = solvesRes.data ?? [];
    const threadRows = threadsRes.data ?? [];

    // Names: only players who have actually kept a mark are ENTERED into the record (the ledger fills
    // with names as they act — a player who has done nothing is not yet filed). Count per player.
    const solvesByPlayer = new Map<string, number>();
    for (const s of solves) {
      solvesByPlayer.set(s.player_id, (solvesByPlayer.get(s.player_id) ?? 0) + 1);
    }

    const enteredIds = [...solvesByPlayer.keys()];
    let names: LedgerName[] = [];
    if (enteredIds.length > 0) {
      const { data: people } = await client
        .from("players")
        .select("id, name")
        .in("id", enteredIds)
        .returns<PlayerRow[]>();
      const byId = new Map((people ?? []).map((p) => [p.id, p.name]));
      names = enteredIds
        .map((id) => ({
          name: (byId.get(id) ?? "").trim(),
          kept: solvesByPlayer.get(id) ?? 0,
        }))
        // a keeper with no resolvable name is filed under a struck placeholder, never dropped
        // (the record notes the mark was kept even when the hand is unnamed).
        .map((n) => ({ name: n.name === "" ? "█████" : n.name, kept: n.kept }))
        .sort((a, b) => b.kept - a.kept || a.name.localeCompare(b.name));
    }

    const totalKept = solves.length;

    // Coarse thread fill: attribute each open puzzle to its thread (denominator), and count solves on
    // OPEN puzzles per thread (numerator). Both are coarse counts — never a card body, never an answer.
    const threadKeyByPuzzle = new Map<string, string | null>();
    const openByThread = new Map<string, number>();
    for (const p of openPuzzles) {
      threadKeyByPuzzle.set(p.puzzle_key, p.thread_key);
      if (p.thread_key) openByThread.set(p.thread_key, (openByThread.get(p.thread_key) ?? 0) + 1);
    }
    const resolvedByThread = new Map<string, number>();
    for (const s of solves) {
      const tk = threadKeyByPuzzle.get(s.puzzle_key);
      if (tk) resolvedByThread.set(tk, (resolvedByThread.get(tk) ?? 0) + 1);
    }

    const threads: ThreadProgress[] = threadRows.map((t) => ({
      key: t.thread_key,
      label: t.label,
      color: t.color,
      resolved: resolvedByThread.get(t.thread_key) ?? 0,
      open: openByThread.get(t.thread_key) ?? 0,
    }));

    return {
      names,
      totalKept,
      threads,
      openPuzzles: openPuzzles.length,
      sealed: false,
    };
  } catch {
    return EMPTY;
  }
}
