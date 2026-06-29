/**
 * gate.ts — THE STORYLET GATE, pure and dependency-free.
 *
 * The progression predicate (OVERHAUL.md §3) and answer-matching live here, with NO
 * import of the DB/config chain, so the build self-tests (gate.selftest.ts) can exercise
 * the soldered nerve without Supabase creds — exactly how `normalize.ts` is the pure twin
 * of the resolver. `repo.ts` re-exports these so its public surface is unchanged.
 *
 * The gate contract: a puzzle row is OPEN ⟺ `active = true` AND every key in its
 * `requires_flags` is truthy in `arc_state.flags`. `getOpenPuzzles` (repo.ts) pre-filters
 * by `active` in SQL, then applies {@link flagsSatisfied} against the single arc_state
 * flags blob. The SAME predicate is mirrored by the Java `OracleResolver.firstMatch`, so
 * both answer surfaces gate identically.
 */
import type { Puzzle } from '../db/types.js';

/**
 * True iff every key in `requiresFlags` is truthy in the live `flags`. An empty/absent
 * `requiresFlags` (the default `{}`) is always satisfied → ungated rows are unchanged.
 * The gate only requires a key be TRUTHY (the seeds only ever set `true`); a missing or
 * falsy live flag keeps the row closed. This is THE storylet precondition test.
 */
export function flagsSatisfied(
  requiresFlags: Record<string, unknown> | null | undefined,
  flags: Record<string, unknown>,
): boolean {
  if (!requiresFlags) return true;
  for (const key of Object.keys(requiresFlags)) {
    if (!flags[key]) return false;
  }
  return true;
}

/**
 * The first OPEN puzzle whose accepted_answers contains the already-normalized string,
 * or null if none. Whole-string exact set-membership only — never substring or fuzzy.
 * Retained for callers/tests that want the single-match shape; the resolver uses
 * {@link matchPuzzles} so a plaintext shared by a SEQUENCED pair resolves to the
 * unsolved row rather than being shadowed by an already-solved upstream owner.
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

/**
 * EVERY open puzzle whose accepted_answers contains the normalized string, in DB order.
 * Usually 0 or 1; >1 only for a plaintext legitimately shared across SEQUENCED rows (an
 * upstream owner that stays open after solving and would otherwise shadow its downstream
 * re-submission consumer — e.g. `the one who turned away` on `stone-iss-wall` then
 * `bound-word`). The resolver picks the first UNSOLVED candidate from this list (the
 * content-preserving fix for sequenced re-submission). Simultaneous (same-movement)
 * collisions are still an authoring error and are disambiguated in the seed, not here.
 */
export function matchPuzzles(
  openPuzzles: readonly Puzzle[],
  normalized: string,
): Puzzle[] {
  if (normalized === '') return [];
  return openPuzzles.filter((p) => p.accepted_answers.includes(normalized));
}
