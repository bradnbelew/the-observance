import "server-only";
import { createHash } from "node:crypto";

import { getOracleClient } from "./oracle-client";
import { normalizeAnswer, MAX_RAW_LEN } from "./normalize";
import { flagsSatisfied, matchPuzzles, type RecordPuzzle } from "./gate";

/**
 * resolve.ts — the website's SERVER-ONLY answer resolver: the SAME closed loop the in-world
 * AnswerSignListener and Discord #the-record scan run, replicated for the third surface (the record
 * website's inscription form). ORACLE.md §1 + discord/src/oracle/resolve.ts are the canonical spec.
 *
 * THE LOOP (identical outcome across all three surfaces, one `puzzles`/`solves` table):
 *   normalize → rate-limit → match OPEN puzzles (active + requires_flags satisfied) → replay guard →
 *   record solve (idempotent, ON CONFLICT DO NOTHING) → apply outcome (set flags; maybe enqueue an
 *   in-world beat at status 'approved'). Wrong / already-solved / rate-limited = a quiet, uniform
 *   NON-answer that never tells you how close you were.
 *
 * WHO CAN INSCRIBE. Unlike Discord (which links by discord_id), the website has no session identity by
 * default, so a keeper INSCRIBES UNDER THEIR IN-WORLD NAME: we resolve the name → the players row
 * (the same identity the world + Discord key on). Only a KNOWN keeper earns a reward — an unknown name
 * is treated exactly like a miss (silence), so the form can't be used to enumerate who exists.
 *
 * DUPLICATION NOTE (for future consolidation). This intentionally re-implements
 * discord/src/oracle/resolve.ts against the dashboard's untyped oracle client. The two share NO code
 * today because they live in separate packages. The clean consolidation is a shared `@observance/oracle`
 * package exporting normalize + gate + a DB-agnostic resolveAnswer(deps) that both the bot and this
 * route import; until then, ORACLE.md §2 + gate.ts are the contract both copies pin to.
 *
 * SECURITY. `server-only`; the service-role key never leaves the server. The route handler that calls
 * this returns ONLY the neutral outcome kind to the browser — never which puzzle matched, never an
 * answer, never a closeness signal.
 */

export const RATE_WINDOW_MS = 60_000;
export const RATE_MAX_IN_WINDOW = 8;

/**
 * The neutral result the browser is allowed to hear. Deliberately coarse:
 *   - 'kept'        — a genuinely-new correct solve was recorded (the mark is kept).
 *   - 'already'     — a correct answer for something this keeper already kept (say nothing new).
 *   - 'unresolved'  — no open puzzle matched (a true miss) OR an unknown keeper (indistinguishable).
 *   - 'withheld'    — rate-limited / per-puzzle cap reached (the checker withholds).
 *   - 'empty'       — the input normalized to nothing (not plausibly an answer).
 * The UI renders each in the cold register; NONE reveals which puzzle, or how close, or that a name
 * exists. This mirrors the oracle's SilentResult/WithheldResult/SolvedResult collapse to a public
 * surface that must leak nothing.
 */
export type RecordOutcome = "kept" | "already" | "unresolved" | "withheld" | "empty";

interface PlayerRow {
  id: string;
  mc_uuid: string | null;
  name: string | null;
  discord_id: string | null;
}

/** Resolve a keeper by exact (case-insensitive) in-world name. LIKE metacharacters escaped +
 *  post-asserted, mirroring repo.linkDiscord's anti-wildcard hardening. null = unknown keeper. */
async function findKeeperByName(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  rawName: string,
): Promise<PlayerRow | null> {
  const name = rawName.trim();
  if (name === "") return null;
  const escaped = name.replace(/([\\%_])/g, "\\$1");
  const { data, error } = await client
    .from("players")
    .select("id, mc_uuid, name, discord_id")
    .ilike("name", escaped)
    .maybeSingle<PlayerRow>();
  if (error || !data) return null;
  if ((data.name ?? "").toLowerCase() !== name.toLowerCase()) return null;
  return data;
}

async function readFlags(client: NonNullable<ReturnType<typeof getOracleClient>>): Promise<Record<string, unknown>> {
  try {
    const { data } = await client.from("arc_state").select("flags").eq("id", 1).maybeSingle<{ flags: Record<string, unknown> | null }>();
    return data?.flags ?? {};
  } catch {
    return {};
  }
}

async function getOpenPuzzles(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  flags: Record<string, unknown>,
): Promise<RecordPuzzle[]> {
  const { data, error } = await client
    .from("puzzles")
    .select("puzzle_key, accepted_answers, outcome_type, outcome_payload, active, max_attempts, requires_flags, thread_key")
    .eq("active", true)
    .returns<RecordPuzzle[]>();
  if (error || !data) return [];
  return data.filter((p) => flagsSatisfied(p.requires_flags, flags));
}

/** Attempts by this player within the window. A read fault returns null and fails closed. */
async function countRecentAttempts(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  opts: { playerId: string | null; windowMs: number; puzzleKey?: string },
): Promise<number | null> {
  try {
    const since = new Date(Date.now() - opts.windowMs).toISOString();
    let q = client.from("answer_attempts").select("id", { count: "exact", head: true }).gte("at", since);
    if (opts.playerId) q = q.eq("player_id", opts.playerId);
    else return null;
    if (opts.puzzleKey) q = q.eq("puzzle_key", opts.puzzleKey);
    const { count, error } = await q;
    if (error) return null;
    return count ?? 0;
  } catch {
    return null;
  }
}

/** SHA-256 of the client IP, truncated — never store a raw IP. Migration 0010's ip_hash column. */
function hashIp(ip: string): string {
  return createHash("sha256").update(ip).digest("hex").slice(0, 32);
}

/**
 * Attempts within the window from an unresolved (no player_id) submitter sharing this IP hash — the
 * throttle an unknown name otherwise skips entirely (INV: unresolved names can't be rate-keyed by
 * player_id, so without this an anonymous prober could hammer the endpoint at zero cost beyond a DB
 * write). Scoped to player_id IS NULL so it can never merge with a known keeper's own bucket. Read
 * errors fail closed, including when migration 0010 has not been applied.
 */
async function countRecentAttemptsByIp(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  opts: { ipHash: string; windowMs: number },
): Promise<number | null> {
  try {
    const since = new Date(Date.now() - opts.windowMs).toISOString();
    const { count, error } = await client
      .from("answer_attempts")
      .select("id", { count: "exact", head: true })
      .is("player_id", null)
      .eq("ip_hash", opts.ipHash)
      .gte("at", since);
    if (error) return null;
    return count ?? 0;
  } catch {
    return null;
  }
}

async function logAttempt(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  a: {
    puzzleKey: string | null;
    playerId: string | null;
    mcUuid: string | null;
    raw: string;
    normalized: string;
    matched: boolean;
    ipHash?: string | null;
  },
): Promise<void> {
  try {
    // surface: 'web' (migration 0010) — the record website's own value. Used to log under 'discord' as
    // a stopgap before 0010 widened the CHECK constraint; kept distinct from 'discord' so this surface's
    // anonymous-IP rate-limit bucket can never merge with the Discord bot's own unlinked-user one.
    await client.from("answer_attempts").insert({
      puzzle_key: a.puzzleKey,
      player_id: a.playerId,
      mc_uuid: a.mcUuid,
      discord_id: null,
      surface: "web",
      raw: a.raw,
      normalized: a.normalized,
      matched: a.matched,
      ip_hash: a.playerId ? null : a.ipHash ?? null,
    });
  } catch {
    /* logging never throws into the loop */
  }
}

async function hasSolved(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  puzzleKey: string,
  playerId: string,
): Promise<boolean> {
  try {
    const { count } = await client
      .from("solves")
      .select("id", { count: "exact", head: true })
      .eq("puzzle_key", puzzleKey)
      .eq("player_id", playerId);
    return (count ?? 0) > 0;
  } catch {
    return false;
  }
}

/** Idempotent solve insert (ON CONFLICT DO NOTHING via upsert+ignoreDuplicates). true = genuinely new. */
async function recordSolve(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  puzzleKey: string,
  player: PlayerRow,
  attemptCount: number,
): Promise<boolean> {
  const { data, error } = await client
    .from("solves")
    .upsert(
      {
        puzzle_key: puzzleKey,
        player_id: player.id,
        mc_uuid: player.mc_uuid,
        discord_id: player.discord_id,
        attempt_count: attemptCount,
      },
      { onConflict: "puzzle_key,player_id", ignoreDuplicates: true },
    )
    .select("id")
    .maybeSingle<{ id: number }>();
  if (error) throw error;
  return data !== null;
}

/** Best-effort: apply set_flags (atomic merge RPC) + enqueue an in-world beat (status 'approved'). */
async function applyOutcome(
  client: NonNullable<ReturnType<typeof getOracleClient>>,
  puzzle: RecordPuzzle,
  solver: PlayerRow,
): Promise<void> {
  const payload = (puzzle.outcome_payload ?? {}) as {
    set_flags?: Record<string, unknown>;
    beat?: { type?: string; mc_uuid?: string; site_id?: string; priority?: number; payload?: Record<string, unknown> };
  };

  if (payload.set_flags && Object.keys(payload.set_flags).length > 0) {
    try {
      await client.rpc("observance_merge_arc_flags", { p_flags: payload.set_flags });
    } catch {
      /* never block the reward on a flag write */
    }
  }

  if (payload.beat && payload.beat.type) {
    try {
      const mcUuid = payload.beat.mc_uuid === "{solver}" ? solver.mc_uuid : payload.beat.mc_uuid ?? null;
      await client.from("beat_queue").insert({
        type: payload.beat.type,
        target: null,
        mc_uuid: mcUuid,
        site_id: payload.beat.site_id ?? null,
        priority: payload.beat.priority ?? null,
        payload: payload.beat.payload ?? {},
        status: "approved", // player-earned → fires on the plugin's next poll, no human gate.
      });
    } catch {
      /* the solve is already durable; a failed enqueue must not error at the player */
    }
  }
}

/**
 * Resolve one inscribed answer under one keeper name. The whole closed loop, server-side. Returns the
 * neutral RecordOutcome only. Any unexpected fault degrades to 'unresolved' (silence) — never an error
 * surfaced to the player, never a leak.
 *
 * @param clientIp best-effort caller IP (from the route handler's request headers), used ONLY to throttle
 *   unresolved-name submissions (see countRecentAttemptsByIp); never stored raw, never used to identify
 *   a real keeper. If omitted (or unavailable behind the proxy), an unknown name is withheld because
 *   there is no safe durable bucket for the public write path.
 */
export async function resolveInscription(
  rawName: string,
  rawAnswer: string,
  clientIp?: string | null,
): Promise<RecordOutcome> {
  const client = getOracleClient();
  if (!client) return "unresolved"; // no backend → the record simply keeps nothing (fail-soft).

  try {
    const rawCapped =
      typeof rawAnswer === "string" && rawAnswer.length > MAX_RAW_LEN ? rawAnswer.slice(0, MAX_RAW_LEN) : rawAnswer ?? "";
    const normalized = normalizeAnswer(rawCapped);
    if (normalized === "") return "empty";

    // Resolve the keeper. Unknown name → treat EXACTLY like a miss (silence) so the form can't
    // enumerate who is real. We still don't short-circuit before the rate-limit/log for a known keeper.
    const keeper = await findKeeperByName(client, rawName);
    const playerId = keeper?.id ?? null;
    const ipHash = !playerId && clientIp ? hashIp(clientIp) : null;

    // Global token bucket for a known keeper; a separate, IP-keyed bucket for an unresolved name (INV:
    // never merge the two — an unresolved submitter must never be able to spend or starve a real
    // keeper's bucket, and vice versa).
    if (playerId) {
      const recent = await countRecentAttempts(client, { playerId, windowMs: RATE_WINDOW_MS });
      if (recent === null || recent >= RATE_MAX_IN_WINDOW) {
        await logAttempt(client, { puzzleKey: null, playerId, mcUuid: keeper?.mc_uuid ?? null, raw: rawCapped, normalized, matched: false });
        return "withheld";
      }
    } else if (ipHash) {
      const recent = await countRecentAttemptsByIp(client, { ipHash, windowMs: RATE_WINDOW_MS });
      if (recent === null || recent >= RATE_MAX_IN_WINDOW) {
        await logAttempt(client, { puzzleKey: null, playerId: null, mcUuid: null, raw: rawCapped, normalized, matched: false, ipHash });
        return "withheld";
      }
    } else {
      // Without a stable caller bucket an unresolved name would be an unlimited public write path.
      return "withheld";
    }

    const flags = await readFlags(client);
    const open = await getOpenPuzzles(client, flags);
    const candidates = matchPuzzles(open, normalized);

    // True miss → log (matched=false), stay silent.
    if (candidates.length === 0) {
      await logAttempt(client, { puzzleKey: null, playerId, mcUuid: keeper?.mc_uuid ?? null, raw: rawCapped, normalized, matched: false, ipHash });
      return "unresolved";
    }

    // Matched, but no known keeper to reward → log matched=true (audit), answer as a miss (never leak
    // that the string WAS a real answer to an anonymous inscriber).
    if (!keeper) {
      await logAttempt(client, { puzzleKey: candidates[0]!.puzzle_key, playerId: null, mcUuid: null, raw: rawCapped, normalized, matched: true, ipHash });
      return "unresolved";
    }

    // Pick the first candidate this keeper has NOT already solved (sequenced-pair disambiguation).
    let puzzle: RecordPuzzle | null = null;
    let allSolved = true;
    for (const c of candidates) {
      if (!(await hasSolved(client, c.puzzle_key, keeper.id))) {
        puzzle = c;
        allSolved = false;
        break;
      }
    }
    if (allSolved) puzzle = candidates[0]!;
    const target = puzzle!;

    // Per-puzzle cap (on top of the global bucket).
    if (target.max_attempts !== null) {
      const onThis = await countRecentAttempts(client, { playerId: keeper.id, windowMs: RATE_WINDOW_MS, puzzleKey: target.puzzle_key });
      if (onThis === null || onThis >= target.max_attempts) {
        await logAttempt(client, { puzzleKey: target.puzzle_key, playerId: keeper.id, mcUuid: keeper.mc_uuid, raw: rawCapped, normalized, matched: true });
        return "withheld";
      }
    }

    // Already solved → say nothing new.
    if (allSolved) {
      await logAttempt(client, { puzzleKey: target.puzzle_key, playerId: keeper.id, mcUuid: keeper.mc_uuid, raw: rawCapped, normalized, matched: true });
      return "already";
    }

    // Idempotent record FIRST (double-fire guard under a race).
    const recent = playerId ? await countRecentAttempts(client, { playerId, windowMs: RATE_WINDOW_MS }) : null;
    if (recent === null) return "withheld";
    const isNew = await recordSolve(client, target.puzzle_key, keeper, recent + 1);
    if (!isNew) {
      await logAttempt(client, { puzzleKey: target.puzzle_key, playerId: keeper.id, mcUuid: keeper.mc_uuid, raw: rawCapped, normalized, matched: true });
      return "already"; // lost the race to a concurrent solve.
    }

    await applyOutcome(client, target, keeper);
    await logAttempt(client, { puzzleKey: target.puzzle_key, playerId: keeper.id, mcUuid: keeper.mc_uuid, raw: rawCapped, normalized, matched: true });
    return "kept";
  } catch {
    return "unresolved"; // fault isolation: a stumble is silence, never an error at the player.
  }
}
