package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;

/** Shared open-finding predicate for the P3 resident-account disagreement. */
public final class P3SettlementDispatchPredicate {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 180;

    private P3SettlementDispatchPredicate() { }

    public static boolean valid(String raw) {
        if (raw == null || raw.length() > MAX_LENGTH) return false;
        String value = fold(raw);
        return value.length() >= MIN_LENGTH
                && any(value, "disagree", "conflict", "contradict", "different account",
                        "accounts differ", "both accounts")
                && any(value, "mark", "date", "time", "name", "place", "location", "work")
                && any(value, "keep open", "keep both", "leave open", "preserve both",
                        "record both", "do not choose", "don t choose", "no official version",
                        "without choosing", "cannot settle", "can t settle", "not enough to decide",
                        "not enough to choose", "needs checking", "needs more checking");
    }

    private static boolean any(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
