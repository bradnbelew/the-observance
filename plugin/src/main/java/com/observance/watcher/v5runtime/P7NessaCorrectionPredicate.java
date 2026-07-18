package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;

/** Shared P7 cause/record/conduct meaning used by Discord and local outage recovery. */
public final class P7NessaCorrectionPredicate {
    public static final int MAX_FIELD_LENGTH = 120;

    private P7NessaCorrectionPredicate() { }

    public record Finding(String cause, String record, String conduct) { }

    public static boolean valid(Finding finding) {
        if (finding == null || invalid(finding.cause())
                || invalid(finding.record()) || invalid(finding.conduct())) return false;
        String cause = fold(finding.cause());
        String record = fold(finding.record());
        String conduct = fold(finding.conduct());
        return any(cause, "divert", "moved", "rerouted")
                && any(cause, "counterfeit", "substitute", "single warp")
                && any(cause, "lower intake", "upstream intake")
                && any(record, "edit", "alter", "change", "rewrit", "rewrote", "revis")
                && any(record, "relief", "shift") && any(record, "complaint", "report")
                && any(conduct, "followed procedure", "used procedure", "worked to procedure")
                && any(conduct, "report", "raised alarm", "raised the alarm", "flagged")
                && conduct.contains("before") && any(conduct, "shed", "fail", "broke", "break");
    }

    private static boolean invalid(String value) {
        return value == null || value.isBlank() || value.length() > MAX_FIELD_LENGTH;
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
