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

    public enum Response {
        SUPPORTED("The provenance, packet progression, and motive boundary support the finding."),
        CONFESSION_ONLY("Wren's words can answer evidence, but they cannot authenticate the private revision. Use an independent custody difference."),
        WRONG_SENDER("That person may explain one source, but the finding must identify who knew the private revision before its public copy."),
        SINGLE_PACKET("One packet proves possession at one moment. Compare how the retained packets gain new categories over time."),
        MISSING_PROVENANCE("The claim lacks a physical or version difference that the sender could not learn from the public copy."),
        ABSOLUTION("Fear may explain a choice. It does not remove agency or responsibility for what was sent."),
        MISSING_MOTIVE("The transmission can be attributed before motive is known, but Wren's response needs a supported fear and a clear responsibility boundary."),
        INCOMPLETE("The finding does not yet connect private provenance, packet progression, and responsible choice.");

        private final String message;

        Response(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

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
                && any(pattern, "progressive packets", "packet progression", "four packets", "increasing packets",
                        "each packet knew more", "packets grew", "copies grew")
                && any(pattern, "name", "people") && any(pattern, "plan", "route", "build")
                && any(pattern, "fear", "afraid", "private worry")
                && any(motive, "erased", "erasure", "disappear", "losing himself", "being forgotten")
                && any(motive, "chose", "choice", "deliberate", "responsible", "responsibility")
                && any(motive, "does not excuse", "not excuse", "does not erase responsibility",
                        "responsibility remains", "still responsible");
    }

    /** Authored non-committing response for plausible theory paths; never supplies a missing answer. */
    public static Response response(Finding finding) {
        if (valid(finding)) return Response.SUPPORTED;
        if (finding == null) return Response.INCOMPLETE;
        String proof = fold(finding.proof() == null ? "" : finding.proof());
        String pattern = fold(finding.pattern() == null ? "" : finding.pattern());
        String motive = fold(finding.motive() == null ? "" : finding.motive());
        if (any(motive, "fear excuses", "fear removes responsibility", "not responsible", "absolves", "forgive")) {
            return Response.ABSOLUTION;
        }
        boolean privateProof = any(proof, "private revision", "private route", "rook revision", "north brace")
                && any(proof, "countermark absent", "counter mark absent", "missing countermark",
                        "missing counter mark", "countermark was missing", "counter mark was missing",
                        "physical mark missing", "physical mark was missing");
        if (!privateProof && any(proof, "confessed", "confession", "said it", "admitted", "wren said")) {
            return Response.CONFESSION_ONLY;
        }
        if (!proof.contains("wren") && any(proof, "mkept", "ash", "rook")) {
            return Response.WRONG_SENDER;
        }
        if (!privateProof) return Response.MISSING_PROVENANCE;
        if (!any(pattern, "progressive packets", "packet progression", "four packets", "increasing packets",
                "each packet knew more", "packets grew", "copies grew")) {
            return Response.SINGLE_PACKET;
        }
        if (!any(motive, "erased", "erasure", "disappear", "losing himself", "being forgotten")) {
            return Response.MISSING_MOTIVE;
        }
        return Response.INCOMPLETE;
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
