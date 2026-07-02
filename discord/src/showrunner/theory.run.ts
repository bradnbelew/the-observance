/**
 * theory.run.ts — the I/O wrapper for the theory-lock producer (Wave S-D `reward-the-theory`).
 *
 * Mirrors reports.run.ts / the between-session producer shape: the PURE policy (theory.ts) decides,
 * this thin wrapper does the I/O — read the group's solved puzzle_keys, read arc_state.flags, call
 * decideTheories(), and for each newly-locked keeper: merge the `<keeper>_theory` flag AND emit the
 * CONSUMER (a Watcher beat to #the-record, `voice.theoryReceived(keeper)`). No orphaned flag: the
 * flag and its record-beat are written together, in the same tick, in the same loop.
 *
 * GROUP-WIDE. The theory is the GROUP's assembled understanding — any player's solve of a cluster
 * puzzle counts toward it. So the solved-set read is the distinct set of solved puzzle_keys across all
 * players (no per-player scoping), matching how the record "receives the present hands" collectively.
 *
 * IDEMPOTENT. The `<keeper>_theory` arc flag IS the high-water (mirrors finale_posted / the fork
 * flags): decideTheories() only returns keepers whose flag is NOT already set, and we flip the flag on
 * lock, so a re-run never re-posts. Fault-isolated + ORDER-SAFE: the flag is merged BEFORE the record
 * post is attempted, so a failed Discord post cannot leave a locked flag without... — no: we post
 * FIRST, then flip the flag only on a successful post, so a failed post leaves the flag unset and the
 * next tick retries (never a locked-but-silent keeper). This matches finale.run.ts's retry discipline.
 *
 * REGISTER (the separation law). Everything posted here is WATCHER register (voice.ts) to #the-record —
 * the record receiving the shape of a keeper. voice.ts is the sole text source; nothing composed here.
 *
 * S-E DOVETAIL. Each `<keeper>_theory` flag is now a durable, idempotent signal in arc_state.flags —
 * available for the record-projection (the off-world record / Hold-Book) to consume later without any
 * further wiring here (the flag is the contract). This wave writes the flag + the one beat; a later
 * wave may read the flag to project the received fate onto the record surface.
 */
import { supabase } from '../db/client.js';
import { setArcFlags, logEvent } from '../db/repo.js';
import { postToTheRecord } from './discord.js';
import { voice } from '../voice.js';
import { CLUSTERS, decideTheories, theoryFlag, type KeeperId } from './theory.js';

export interface TheoryPassResult {
  /** keepers whose theory locked (flag flipped + record beat posted) this pass. */
  locked: KeeperId[];
}

/**
 * Read the GROUP's distinct solved puzzle_keys, restricted to the cluster evidence keys (we only need
 * the ones a theory depends on). Fault-isolated: any read error ⇒ empty set (the pass no-ops — silence
 * is canon, and a missed lock is safer than a misfire; the next tick retries once the read recovers).
 */
async function readSolvedClusterKeys(): Promise<Set<string>> {
  const evidenceKeys = [...new Set(CLUSTERS.flatMap((c) => c.evidence))];
  try {
    const { data, error } = await supabase
      .from('solves')
      .select('puzzle_key')
      .in('puzzle_key', evidenceKeys)
      .returns<{ puzzle_key: string | null }[]>();
    if (error || !data) return new Set();
    const solved = new Set<string>();
    for (const r of data) {
      if (r.puzzle_key) solved.add(r.puzzle_key);
    }
    return solved;
  } catch {
    return new Set();
  }
}

/**
 * runTheoryPass — lock each keeper whose evidence cluster is now coherent, and let the record RECEIVE
 * it (one Watcher beat per newly-locked keeper). Reads the already-locked keepers from the passed arc
 * flags (already fetched by runAutonomyPasses), so it costs one extra read (the solves query) per tick.
 * Each keeper is handled independently and fault-isolated, so one failed post never blocks the others.
 *
 * @param flags the arc_state.flags blob (already fetched by the autonomy tick) — the high-water source.
 */
export async function runTheoryPass(flags: Record<string, unknown>): Promise<TheoryPassResult> {
  const result: TheoryPassResult = { locked: [] };

  const alreadyLocked = new Set<string>(
    CLUSTERS.map((c) => c.keeper).filter((k) => flags[theoryFlag(k)] === true),
  );

  const solvedKeys = await readSolvedClusterKeys();
  const newly = decideTheories(solvedKeys, alreadyLocked);
  if (newly.length === 0) return result;

  for (const keeper of newly) {
    // Post the record-receiving beat FIRST; flip the high-water flag only on success, so a failed post
    // leaves the flag unset and the next tick retries (never a locked-but-silent keeper).
    const ok = await postToTheRecord(voice.theoryReceived(keeper));
    if (!ok) {
      await logEvent('warn', 'showrunner.theory', `failed to post theory-received for ${keeper} — leaving flag unset to retry`);
      continue;
    }
    try {
      await setArcFlags({ [theoryFlag(keeper)]: true });
    } catch (e) {
      // The beat posted but the flag write failed: the next tick will re-post (at-least-once). Logged,
      // not thrown — one keeper's flag hiccup must not abort the others.
      await logEvent('warn', 'showrunner.theory', `theory beat posted for ${keeper} but flag merge failed (will retry): ${e instanceof Error ? e.message : String(e)}`);
      continue;
    }
    result.locked.push(keeper);
    await logEvent('info', 'showrunner.theory', `theory received for the one called ${keeper} — the cluster is coherent, ${theoryFlag(keeper)} locked`);
  }

  return result;
}
