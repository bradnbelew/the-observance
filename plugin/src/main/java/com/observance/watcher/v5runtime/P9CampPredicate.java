package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;

/** Shared canonical meaning for Copperline's P9 forms and the local outage/recovery command. */
public final class P9CampPredicate {
    public static final int MAX_FIELD_LENGTH = 64;

    private P9CampPredicate() { }

    public record People(String mkept, String ash, String rook, String wren) { }
    public record Window(String before, String crossing, String after,
                         String readiness, String boundary) { }

    public static boolean validPeople(People people) {
        return people != null
                && is(people.mkept(), "admin custody", "server custody", "checksums custody")
                && is(people.ash(), "camera humor", "camera frames", "visual log")
                && is(people.rook(), "builder countermark", "builder counter-mark", "builder revision", "structural countermark")
                && is(people.wren(), "route companion", "route memory", "distance copies");
    }

    public static boolean validWindow(Window window) {
        return window != null
                && is(window.before(), "rook private countermark", "rook private counter-mark",
                        "private countermark", "private counter-mark", "nb 17 c private")
                && is(window.crossing(), "witness spool intake", "spool intake", "nb 17 c intake")
                && is(window.after(), "public upload", "copperline upload", "copperline public upload", "nb 17 c public")
                && is(window.readiness(), "release board complete", "board complete", "work complete")
                && is(window.boundary(), "inside access sender open", "inside access sender unknown",
                        "inside source unknown");
    }

    private static boolean is(String value, String... accepted) {
        if (value == null || value.isBlank() || value.length() > MAX_FIELD_LENGTH) return false;
        String normalized = fold(value);
        for (String candidate : accepted) if (normalized.equals(fold(candidate))) return true;
        return false;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
