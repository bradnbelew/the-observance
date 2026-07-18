package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Six-field recovery predicate for P6's responsibility matrix.
 *
 * <p>This is not the primary investigation verb. It is the stable keyboard/accessibility route for
 * players who already hold the conclusion, including conclusions shared by another player. No
 * observation, affidavit possession, or per-room completion is inspected here.</p>
 */
public final class P6ResponsibilityPredicate {
    public static final int MAX_FIELD_LENGTH = 160;

    private P6ResponsibilityPredicate() { }

    public record Matrix(String vaun, String mara, String sella,
                         String orin, String brann, String iss) { }

    public static boolean valid(Matrix matrix) {
        if (matrix == null || invalid(matrix.vaun()) || invalid(matrix.mara())
                || invalid(matrix.sella()) || invalid(matrix.orin())
                || invalid(matrix.brann()) || invalid(matrix.iss())) return false;
        return vaun(fold(matrix.vaun()))
                && mara(fold(matrix.mara()))
                && sella(fold(matrix.sella()))
                && orin(fold(matrix.orin()))
                && brann(fold(matrix.brann()))
                && iss(fold(matrix.iss()));
    }

    private static boolean vaun(String value) {
        return any(value, "divert", "missing cloth", "heat order")
                && any(value, "delay", "silent", "postpone", "kept issue going", "continuity")
                && any(value, "receipt", "reseal", "ledger", "balance");
    }

    private static boolean mara(String value) {
        return any(value, "edition", "route", "manual", "clean copy")
                && any(value, "hid", "substitut", "unsafe", "clean plate")
                && any(value, "margin", "correction", "older route");
    }

    private static boolean sella(String value) {
        return any(value, "intake", "sample", "shore", "survey")
                && any(value, "teach", "transfer", "drawing", "landmark")
                && any(value, "sealed", "sketch", "lower jar", "true line");
    }

    private static boolean orin(String value) {
        return any(value, "seam", "brace", "load", "damage")
                && any(value, "condition", "signed", "promise")
                && any(value, "warning", "scraped", "replacement", "halved");
    }

    private static boolean brann(String value) {
        return any(value, "watch", "rota", "toll", "relief")
                && any(value, "rewrite", "wrong time", "eight", "timestamp")
                && any(value, "paired", "meal", "lamp", "pell");
    }

    private static boolean iss(String value) {
        return any(value, "surface", "reed", "recover", "open sky")
                && any(value, "route", "risk", "omitted", "unsafe")
                && any(value, "objection", "registrar", "warning", "correction");
    }

    private static boolean invalid(String value) {
        return value == null || value.isBlank() || value.length() > MAX_FIELD_LENGTH;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static boolean any(String value, String... terms) {
        return List.of(terms).stream().anyMatch(value::contains);
    }
}
