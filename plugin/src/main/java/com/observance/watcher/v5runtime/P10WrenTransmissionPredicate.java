package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Shared meaning boundary for the P10 transmission finding.
 *
 * <p>The predicate accepts short, ordinary-language findings. It does not inspect source clicks,
 * NPC contact, packet possession, or observation receipts. Wren's response is a consequence of a
 * supported finding, never the source that proves it.</p>
 */
public final class P10WrenTransmissionPredicate {
    public static final int MAX_FIELD_LENGTH = 120;

    private P10WrenTransmissionPredicate() { }

    public record Finding(String proof, String pattern, String motive) { }

    public static boolean valid(Finding finding) {
        if (finding == null || invalid(finding.proof())
                || invalid(finding.pattern()) || invalid(finding.motive())) return false;
        String proof = fold(finding.proof());
        String pattern = fold(finding.pattern());
        String motive = fold(finding.motive());
        return proof.contains("wren")
                && any(proof, "private revision", "private route", "rook revision", "north brace")
                && any(proof, "countermark absent", "counter mark absent", "missing countermark",
                        "missing counter mark", "countermark was missing", "counter mark was missing",
                        "physical mark missing", "physical mark was missing")
                && any(pattern, "progressive packets", "packet progression", "four packets", "increasing packets")
                && pattern.contains("name") && any(pattern, "plan", "route") && pattern.contains("fear")
                && any(motive, "erased", "erasure", "disappear", "losing himself")
                && any(motive, "chose", "choice", "deliberate", "responsible", "responsibility")
                && any(motive, "does not excuse", "not excuse", "does not erase responsibility",
                        "responsibility remains", "still responsible");
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
