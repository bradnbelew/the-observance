package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

/**
 * Six-field recovery predicate for P6's responsibility matrix.
 *
 * <p>This is not the primary investigation verb. It is the stable keyboard/accessibility route for
 * players who already hold the conclusion, including conclusions shared by another player. No
 * observation, affidavit possession, or per-room completion is inspected here.</p>
 */
public final class P6ResponsibilityPredicate {
    public static final int MAX_FIELD_LENGTH = 120;

    private P6ResponsibilityPredicate() { }

    public record Matrix(String vaun, String mara, String sella,
                         String orin, String brann, String iss) { }

    public static boolean valid(Matrix matrix) {
        return unsupportedRows(matrix).isEmpty();
    }

    /** Names only the rows needing review; it never discloses a missing keyword or finding. */
    public static List<String> unsupportedRows(Matrix matrix) {
        if (matrix == null) return List.of("Vaun", "Mara", "Sella", "Orin", "Brann", "Iss");
        List<String> result = new ArrayList<>();
        if (invalid(matrix.vaun()) || !vaun(fold(matrix.vaun()))) result.add("Vaun");
        if (invalid(matrix.mara()) || !mara(fold(matrix.mara()))) result.add("Mara");
        if (invalid(matrix.sella()) || !sella(fold(matrix.sella()))) result.add("Sella");
        if (invalid(matrix.orin()) || !orin(fold(matrix.orin()))) result.add("Orin");
        if (invalid(matrix.brann()) || !brann(fold(matrix.brann()))) result.add("Brann");
        if (invalid(matrix.iss()) || !iss(fold(matrix.iss()))) result.add("Iss");
        return List.copyOf(result);
    }

    private static boolean vaun(String value) {
        return any(value, "divert", "missing cloth", "heat order", "cloth moved", "stock moved")
                && any(value, "delay", "silent", "postpone", "kept issue going", "continuity",
                        "waited to object", "didn t object", "failed to object")
                && any(value, "receipt", "reseal", "ledger", "balance", "left the figures", "kept the figures");
    }

    private static boolean mara(String value) {
        return any(value, "edition", "route", "manual", "clean copy", "directions")
                && any(value, "hid", "substitut", "unsafe", "clean plate", "removed the warning")
                && any(value, "margin", "correction", "older route", "kept her note", "left her note");
    }

    private static boolean sella(String value) {
        return any(value, "intake", "sample", "shore", "survey", "water line")
                && any(value, "teach", "transfer", "drawing", "landmark", "passed it on")
                && any(value, "sealed", "sketch", "lower jar", "true line", "saved the jar", "saved the drawing");
    }

    private static boolean orin(String value) {
        return any(value, "seam", "brace", "load", "damage", "crack")
                && any(value, "condition", "signed", "promise", "approved")
                && any(value, "warning", "scraped", "replacement", "halved", "left a limit", "marked the limit");
    }

    private static boolean brann(String value) {
        return any(value, "watch", "rota", "toll", "relief", "shift")
                && any(value, "rewrite", "wrong time", "eight", "timestamp", "changed the time", "changed the watch time")
                && any(value, "paired", "meal", "lamp", "pell", "second light", "meal chit");
    }

    private static boolean iss(String value) {
        return any(value, "surface", "reed", "recover", "open sky", "water cleared")
                && any(value, "route", "risk", "omitted", "unsafe", "bad cut", "dangerous cut")
                && any(value, "objection", "registrar", "warning", "correction", "left the warning", "kept the warning");
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
