package com.observance.watcher.oracle;

import java.text.Normalizer;
import java.util.Locale;

/**
 * THE shared answer-normalization algorithm (ORACLE.md §2) — byte-for-byte identical to the bot's
 * TypeScript {@code normalizeAnswer}. Drift here breaks the closed loop silently, so this is the one
 * authoritative Java implementation and it must never diverge from the spec:
 *
 * <pre>
 *   1. Unicode NFKC normalize.
 *   2. case-fold to lower (Locale.ROOT).
 *   3. replace every run of chars NOT in [a-z0-9 ] with a single space
 *      (non-alnum → space, so "BOW,AT" → "bow at"; the minus in "-1280" is dropped).
 *   4. collapse internal whitespace runs to ONE space.
 *   5. trim.
 * </pre>
 *
 * <p>Pure + deterministic + null-safe. An empty result ({@code ""}) is the gibberish guard: it never
 * matches and callers never even log it as an attempt.
 */
public final class AnswerNormalizer {

    private AnswerNormalizer() { }

    /** Normalize raw player input per ORACLE.md §2. Null → "". */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                // step 3: anything not a-z, 0-9, or space → a space (so punctuation splits words).
                .replaceAll("[^a-z0-9 ]+", " ")
                // step 4: collapse whitespace runs (covers tabs/newlines too) to one space.
                .replaceAll("\\s+", " ")
                // step 5: trim.
                .trim();
        return n;
    }
}
