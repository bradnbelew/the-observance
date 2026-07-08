/**
 * theory.ts — "the record receives the THEORY, not the decode" (Wave S-D `reward-the-theory`).
 *
 * THE GAP THIS CLOSES. Today each keeper puzzle solve independently un-redacts that keeper's record
 * line — one decode, one reveal. That rewards typing an answer, not UNDERSTANDING a keeper. This layer
 * makes the record "receive" a keeper's FATE only when a COHERENT CLUSTER of that keeper's evidence is
 * solved — the player has built a theory (Obra Dinn: you confirm a fate when enough of the picture
 * agrees), not entered one cipher. It is a DERIVED layer that sits ON TOP of the existing per-puzzle
 * solve→flag behavior; it changes none of it.
 *
 * PURE. No DB / network / clock / LLM side effect in THIS file — theory.selftest.ts imports it with
 * nothing. The I/O wrapper (read the group's solved puzzle_keys, read arc_state.flags, merge the
 * `<keeper>_theory` flag, emit the #the-record consumer beat) is theory.run.ts, mirroring reports.run.ts
 * / the keeper-record producer shape.
 *
 * IDEMPOTENT HIGH-WATER. The `<keeper>_theory` arc flag IS the high-water mark (mirrors the finale's
 * set-once flags): a keeper whose flag is already set is never re-emitted, so the record receives each
 * keeper's assembled understanding exactly once. No count, no schema change — a jsonb key in
 * arc_state.flags, merged atomically by setArcFlags.
 *
 * DATA-DRIVEN THRESHOLDS. The cluster map is pure data (CLUSTERS below) so it is tunable without
 * touching the policy. `threshold` = "enough of the cluster solved to be a coherent theory." For this
 * veteran-facing route, threshold 3 means one stone is only suspicion, two pieces are still a lead, and
 * a theory locks only when the group has handled a real cluster of story/evidence. Each evidence key
 * was VERIFIED against the live puzzles_seed.sql / metapuzzle_seed.sql (all 21 keys are real puzzle_key
 * rows — no drift, no keys dropped).
 */

/** The six keepers a theory can be assembled about (fall-order; WEB-MASTER §6). */
export type KeeperId = 'vaun' | 'mara' | 'sella' | 'orin' | 'brann' | 'iss';

/** One keeper's evidence cluster: the puzzle_keys that, together, form a coherent theory of them. */
export interface KeeperCluster {
  keeper: KeeperId;
  /** the evidence puzzle_keys — VERIFIED to exist in the live seed (theory.selftest pins the shape). */
  evidence: readonly string[];
  /** how many of the cluster must be solved for the theory to be "coherent" (tunable data). */
  threshold: number;
}

/**
 * The cluster map (pure data). Each keeper → their evidence puzzle_keys + a coherence threshold.
 * Every key here is a real `puzzle_key` in supabase/seeds/{puzzles,metapuzzle}_seed.sql (verified
 * against the live seed at build time — 21 keys, 0 drift). Threshold 3 = a fuller theory cluster, so a
 * lone stone-decode or a single corroborating beat is NOT yet a theory (the whole point: web > decode).
 */
export const CLUSTERS: readonly KeeperCluster[] = [
  { keeper: 'vaun', evidence: ['stone-vaun', 'vaun-hoard-sorted', 'vaun-bookshelf-tally'], threshold: 3 },
  { keeper: 'mara', evidence: ['stone-mara', 'mara-lectern-lock', 'mara-walk-the-map'], threshold: 3 },
  { keeper: 'sella', evidence: ['sella-reflection-bearing', 'sella-overlay-lake', 'sella-shore-memorial'], threshold: 3 },
  { keeper: 'orin', evidence: ['stone-orin', 'orin-bow-fall-order', 'orin-banner-heraldry', 'orin-frame-dials'], threshold: 3 },
  { keeper: 'brann', evidence: ['stone-brann', 'stone-brann-cipher', 'brann-black-moon-toll', 'brann-silence-corridor'], threshold: 3 },
  { keeper: 'iss', evidence: ['stone-iss-wall', 'iss-which-is-true', 'iss-nbt-falsified-entry', 'iss-bound-word-callback'], threshold: 3 },
] as const;

/** The arc_state.flags key that locks (idempotent high-water) once a keeper's theory is received. */
export function theoryFlag(keeper: KeeperId): string {
  return `${keeper}_theory`;
}

/**
 * decideTheories — the pure theory-lock policy. Given the set of solved puzzle_keys (group-wide) and
 * the set of keepers whose theory flag is ALREADY locked, return the keepers whose cluster now has
 * enough evidence (solved-count ≥ threshold) AND whose `<keeper>_theory` flag is NOT already set.
 *
 * Deterministic (fall-order, the CLUSTERS declaration order). Same inputs → same output. A keeper is
 * emitted at most once ever (the caller locks the flag, which becomes the alreadyLocked guard next
 * tick), so this never re-emits a locked keeper — the flag is the high-water mark.
 *
 * @param solvedKeys    the group's solved puzzle_keys (any player's solve counts toward the theory).
 * @param alreadyLocked the keepers whose `<keeper>_theory` flag is already true in arc_state.flags.
 */
export function decideTheories(solvedKeys: Set<string>, alreadyLocked: Set<string>): KeeperId[] {
  const newlyLocked: KeeperId[] = [];
  for (const cluster of CLUSTERS) {
    if (alreadyLocked.has(cluster.keeper)) continue; // idempotent: never re-emit a locked keeper
    let solvedInCluster = 0;
    for (const key of cluster.evidence) {
      if (solvedKeys.has(key)) solvedInCluster += 1;
    }
    if (solvedInCluster >= cluster.threshold) newlyLocked.push(cluster.keeper);
  }
  return newlyLocked;
}
