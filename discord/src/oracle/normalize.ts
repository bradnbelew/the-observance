/**
 * normalize.ts — the PURE answer-normalization algorithm (ORACLE.md §2).
 *
 * Deliberately dependency-free: no DB, no config, no Discord. This is the byte-for-byte
 * twin of the plugin's Java `AnswerNormalizer`, and the single source the resolver and the
 * seed-check both use. Keeping it pure means it can be imported by tests/tools (e.g.
 * seedcheck.ts) without dragging in the Supabase client or requiring a populated .env.
 * Drift between this and the Java side breaks the oracle loop silently — change both together.
 */

/**
 * Normalize raw player input to the matchable form. NFKC → lower → strip every
 * char that is not [a-z0-9 ] (mapping each run to a SPACE, so "bow,at" → "bow
 * at") → collapse whitespace → trim. An empty result never matches and is never
 * logged (it is not "plausibly an answer").
 */
export function normalizeAnswer(s: string): string {
  return s
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[^a-z0-9 ]+/g, ' ') // non-alnum → space (so "BOW,AT" → "bow at")
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Hard ceiling on how much raw input we read/store. A real answer is a handful of
 * words; anything past this is paste-spam/abuse and is truncated BEFORE normalize +
 * before it ever reaches answer_attempts.raw (so a giant message can't bloat the
 * table or the normalize regex). The match itself is unaffected — no real answer is
 * anywhere near this long. Discord already caps message length, but #the-record runs
 * on every message and /answer takes free text, so we cap defensively regardless.
 */
export const MAX_RAW_LEN = 512;
