package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;

/** Exact, bounded artifact predicate for the six independent P11 routes. */
public final class P11AverynIdentityPredicate {
    public static final int MAX_LENGTH = 12;

    private P11AverynIdentityPredicate() { }

    /** The artifact is exact; acquiring or touching any particular source is never required. */
    public static boolean valid(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) return false;
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim().toUpperCase(Locale.ROOT).equals("AVERYN");
    }
}
