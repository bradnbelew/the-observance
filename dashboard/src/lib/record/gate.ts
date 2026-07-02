// gate.ts — the EXACT storylet gate + answer-matching, replicated for the website.
//
// DUPLICATE ON PURPOSE (see normalize.ts for the same rationale). Canonical source:
// `discord/src/oracle/gate.ts`, mirrored by the plugin's Java `OracleResolver.firstMatch`. The
// website is a third answer surface and must gate/match identically or the loop desyncs.
//
// The gate contract (0006_requires_flags.sql / OVERHAUL.md §3):
//   A puzzle row is OPEN  ⟺  active = true  AND  every key in requires_flags is truthy in
//   arc_state.flags. An empty requires_flags ({}, the default) is always satisfied → ungated.
// Matching is whole-string exact set-membership of the normalized answer against accepted_answers —
// never substring, never fuzzy.

/** The minimal puzzle shape the website matcher needs. The oracle tables are not in the dashboard's
 *  generated Database type (different migration lane), so we declare the shape we read. */
export interface RecordPuzzle {
  puzzle_key: string;
  accepted_answers: string[];
  outcome_type: string;
  outcome_payload: Record<string, unknown> | null;
  active: boolean;
  max_attempts: number | null;
  requires_flags: Record<string, unknown> | null;
  thread_key: string | null;
}

/**
 * True iff every key in `requiresFlags` is truthy in the live `flags`. An empty/absent
 * `requiresFlags` (the default `{}`) is always satisfied → ungated rows are unchanged. Byte-identical
 * to the oracle `flagsSatisfied`.
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
 * Every OPEN puzzle whose accepted_answers contains the already-normalized string, in the given
 * order. Usually 0 or 1; >1 only for a plaintext shared by a sequenced pair. The resolver picks the
 * first UNSOLVED candidate (matching the oracle's pickCandidate). Whole-string set-membership only.
 */
export function matchPuzzles(
  openPuzzles: readonly RecordPuzzle[],
  normalized: string,
): RecordPuzzle[] {
  if (normalized === "") return [];
  return openPuzzles.filter((p) => p.accepted_answers.includes(normalized));
}
