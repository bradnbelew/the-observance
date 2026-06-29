/**
 * liar.ts — the Liar-engine flag-gated swap (D4 `backlog-liar-engine`, WEB-MASTER §0.5/§1.M4).
 *
 * WHAT THIS IS — AND IS NOT. The Liar engine's ACTIVATION lane (lighting the staged back-half rows on
 * `iss_caught`) is OWNED by the deterministic `puzzles.requires_flags` gate in `getOpenPuzzles`
 * (resolve.ts / repo.ts — NOT this file; coherence Batch-2 P1-1, ROOT-C). The Keeper-NPC node-text
 * swap is a DIFFERENT surface (keeper.ts). This module is the THIRD, demoted role the showrunner keeps
 * (WEB-MASTER §0.5): the OPTIONAL CURATED RE-STAGING of Iss's already-posted WARM beats as COLD once the
 * catch lands. It is a colorant, never the activation path — it gates nothing, opens no puzzle, and is
 * always safe to skip.
 *
 * FLAG-GATED, ONE-WAY (the whole mechanic). Nothing here fires until `iss_caught` is truthy in
 * arc_state. Before the catch, Iss is warm and this engine is inert. AT the catch, each of Iss's
 * surfaced warm beats may be re-staged once as its cold counterpart (warm→cold, never back). After the
 * catch it is steady state — a re-run re-stages nothing already flipped (idempotent on a per-beat
 * high-water).
 *
 * CONFIRM-PENDING BY DEFAULT (the autonomy dial; D1). A re-staged cold beat is CURATORIAL (it rewrites
 * the Watcher/Iss voice about a real moment) — it is enqueued `pending` for dashboard approval even in
 * AUTO unless the showrunner is explicitly in AUTO mode, mirroring every other curatorial producer. The
 * cold line is an authored voice key (`iss.dialogue.turns_cold` family) resolved by resolve.ts's
 * `private_message` key-resolver (0.10) — NOT composed here; voice.ts stays the single text source.
 *
 * PURE. No DB / network / clock / LLM. The cold lines are authored voice keys (determinism is the
 * backstop). liar.run.ts reads `iss_caught` + the set of Iss warm beats already posted + the per-beat
 * high-water, fires the cold re-stage rows, and persists marks. liar.selftest.ts imports this with
 * nothing.
 */

/**
 * One of Iss's warm beats that has a cold counterpart. The `warmKey`/`coldKey` are authored voice keys
 * (TS-VOICE owns the bodies); this engine only decides WHICH flip to stage and never composes text. The
 * `id` is the stable per-beat idempotency key (the high-water is a set of these).
 */
export interface IssWarmBeat {
  /** stable id for the warm beat instance (the idempotency key). */
  id: string;
  /** the authored WARM voice key already posted (for the trace only; not re-read). */
  warmKey: string;
  /** the authored COLD voice key to re-stage it as (resolve.ts resolves it to subtitle, 0.10). */
  coldKey: string;
  /** optional in-world target (a sign/lectern site) the cold re-carve lands on; null = Discord-only. */
  siteId?: string | null;
}

export interface LiarInput {
  /** arc_state.flags.iss_caught — the one-way gate. Nothing flips until this is true. */
  issCaught: boolean;
  /** Iss's warm beats that have a cold counterpart (the run wrapper supplies the posted set). */
  warmBeats: IssWarmBeat[];
  /** the per-beat ids already re-staged cold (idempotency high-water). */
  alreadyFlipped: ReadonlySet<string>;
  /** AUTO → the cold re-stage may fire 'approved'; CONFIRM → 'pending'. Curatorial → defaults pending. */
  mode: 'auto' | 'confirm';
}

/** One cold re-stage row to enqueue this tick (warm→cold), or none. */
export interface ColdRestageRow {
  beatId: string;
  /** the authored cold voice key (resolve.ts → subtitle). A KEY, never composed text. */
  coldKey: string;
  siteId: string | null;
  /** 'approved' only in AUTO; 'pending' otherwise (curatorial gate). */
  status: 'approved' | 'pending';
  reason: string;
}

export interface LiarDecision {
  rows: ColdRestageRow[];
  /** the beat ids newly flipped this tick (merge into the high-water only for rows that fired). */
  flipped: string[];
  notes: string[];
}

/**
 * decideColdRestage — pure, flag-gated, one-way. Before the catch: nothing. At/after the catch: for
 * each warm beat NOT yet flipped, emit one cold re-stage row (curatorial: 'approved' only in AUTO). A
 * beat already in `alreadyFlipped` is skipped (idempotent). Same input → same output.
 */
export function decideColdRestage(inp: LiarInput): LiarDecision {
  const notes: string[] = [];

  if (!inp.issCaught) {
    return { rows: [], flipped: [], notes: ['iss not caught — the liar is still warm; no cold re-stage'] };
  }

  const status: 'approved' | 'pending' = inp.mode === 'auto' ? 'approved' : 'pending';
  const rows: ColdRestageRow[] = [];
  const flipped: string[] = [];

  for (const b of inp.warmBeats) {
    if (inp.alreadyFlipped.has(b.id)) {
      notes.push(`beat ${b.id} already re-staged cold — skip (one-way)`);
      continue;
    }
    rows.push({
      beatId: b.id,
      coldKey: b.coldKey,
      siteId: b.siteId ?? null,
      status,
      reason: `iss_caught — re-stage warm '${b.warmKey}' as cold '${b.coldKey}' (one-way, ${status})`,
    });
    flipped.push(b.id);
  }

  // Deterministic order (by beat id) so a tick's re-stages are stable + testable.
  rows.sort((a, b) => a.beatId.localeCompare(b.beatId));
  flipped.sort();
  if (rows.length === 0 && inp.warmBeats.length > 0) {
    notes.push('all warm beats already re-staged cold — steady state');
  }
  return { rows, flipped, notes };
}
