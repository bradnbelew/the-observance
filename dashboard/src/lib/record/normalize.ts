// normalize.ts — the EXACT oracle answer-normalization algorithm, replicated for the website.
//
// THIS IS A DUPLICATE, ON PURPOSE. The canonical source is `discord/src/oracle/normalize.ts`
// (the TS twin of the plugin's Java `AnswerNormalizer`). The website is a THIRD answer surface
// (in addition to Discord #the-record and the in-world sign), so it MUST normalize byte-for-byte
// identically or the closed loop desyncs silently — a correct answer typed on the record would
// miss a puzzle that the same string solves in-world.
//
// We cannot import across the two packages (the dashboard is its own Next.js app with no path to
// `discord/src`), so we copy the algorithm verbatim and pin it with a selftest. ORACLE.md §2 is the
// contract; if the canonical source changes, change this too. (Future consolidation note: extract
// this + gate.ts into a shared package imported by discord/, the plugin's build, and the dashboard.)
//
// The algorithm (ORACLE.md §2), verbatim:
//   1. Unicode NFKC normalize.
//   2. case-fold to lower.
//   3. replace every run of chars NOT in [a-z0-9 ] with a SINGLE SPACE (non-alnum → space, NOT empty).
//   4. collapse internal whitespace runs to ONE space.
//   5. trim.

/** Normalize raw player input to the matchable form. Byte-for-byte twin of the oracle normalizer. */
export function normalizeAnswer(s: string): string {
  return s
    .normalize("NFKC")
    .toLowerCase()
    .replace(/[^a-z0-9 ]+/g, " ") // non-alnum → space (so "BOW,AT" → "bow at")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Hard ceiling on how much raw input we read/store — identical to the oracle's MAX_RAW_LEN. A real
 * answer is a handful of words; anything past this is paste-spam/abuse and is truncated BEFORE
 * normalize and before it is stored, so a giant paste can't bloat `answer_attempts.raw` or feed a
 * pathological string into the normalize regex.
 */
export const MAX_RAW_LEN = 512;
