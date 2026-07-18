package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Four independent meaning fields shared by the Minecraft recovery surface and Copperline form. */
public final class P8InterventionPlanPredicate {
    public static final int MAX_FIELD_LENGTH = 180;

    private P8InterventionPlanPredicate() { }

    public record Plan(String causes, String iss, String copyBoundary, String order) { }

    public static boolean valid(Plan plan) {
        if (plan == null || tooLong(plan.causes()) || tooLong(plan.iss())
                || tooLong(plan.copyBoundary()) || tooLong(plan.order())) return false;
        String causes = fold(plan.causes());
        String iss = fold(plan.iss());
        String boundary = fold(plan.copyBoundary());
        String order = fold(plan.order());
        int[] positions = {
                first(order, List.of("water", "filter")),
                first(order, List.of("paired light", "lamp", "watch light")),
                first(order, List.of("pressure", "bypass")),
                first(order, List.of("staff route", "route", "passage"))
        };
        return hasAny(causes, List.of("old fracture", "existing fracture", "earlier fracture"))
                && hasAny(causes, List.of("heat load", "unchanged heat", "heat stayed", "stayed high"))
                && hasAny(causes, List.of("watch gap", "paired watch", "empty watch", "coverage gap"))
                && hasAny(causes, List.of("late route", "late routing", "closure delay", "delayed closure"))
                && hasAny(iss, List.of("surface proof", "surface sample", "reed sample", "water sample"))
                && hasAny(iss, List.of("valid", "sound", "true", "held", "was right", "checked out"))
                && hasAny(iss, List.of("route unsafe", "cut unsafe", "cut was unsafe", "unreviewed route", "unsafe cut"))
                && hasAny(boundary, List.of("copy", "altered office", "record behavior"))
                && hasAny(boundary, List.of("proves behavior", "shows behavior", "copying is proven", "alteration is proven"))
                && hasAny(boundary, List.of("dark unknown", "dark remains unknown", "ontology open", "does not identify", "not what the dark is",
                        "still don t know what it is", "still do not know what it is", "doesn t tell us what it is"))
                && ordered(positions);
    }

    private static boolean tooLong(String value) {
        return value == null || value.isBlank() || value.length() > MAX_FIELD_LENGTH;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static boolean hasAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    private static int first(String value, List<String> terms) {
        return terms.stream().mapToInt(value::indexOf).filter(index -> index >= 0).min().orElse(-1);
    }

    private static boolean ordered(int[] positions) {
        for (int index = 0; index < positions.length; index++) {
            if (positions[index] < 0 || (index > 0 && positions[index - 1] >= positions[index])) return false;
        }
        return true;
    }
}
