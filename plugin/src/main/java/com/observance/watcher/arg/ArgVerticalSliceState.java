package com.observance.watcher.arg;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Local-primary P4/P5 ARG slice state. Observations never participate in its predicates. */
public final class ArgVerticalSliceState {
    public static final String THEORY_EVENT = "p4.control_reversal_earned";
    public static final String COPY_TEST_EVENT = "p4.copy_hypothesis_tested";
    public static final String SERVICE_EVENT = "p5.service_cards_public";
    public static final String PENALTY_EVENT = "p5.penalty_copies_in_custody";
    public static final String CHRONOLOGY_EVENT = "p5.service_chronology_shared";
    public static final String CURATED_EVENT = "p5.civic_gallery_recurated";

    private final LocalPrimaryJournal journal;

    private ArgVerticalSliceState(LocalPrimaryJournal journal) {
        this.journal = journal;
    }

    public static ArgVerticalSliceState open(Path path) throws IOException {
        return new ArgVerticalSliceState(LocalPrimaryJournal.open(path));
    }

    public synchronized TheoryResult submitConclusion(
            String purposeRaw, String changeRaw, String anomalyRaw, String contributor) throws IOException {
        String purpose = normalize(purposeRaw);
        String change = normalize(changeRaw);
        String anomaly = normalize(anomalyRaw);
        if (purpose.isBlank() || change.isBlank() || anomaly.isBlank()) return TheoryResult.INCOMPLETE;
        if (!withinBudget(purpose, 64, 12) || !withinBudget(change, 96, 18)
                || !withinBudget(anomaly, 64, 12)) return TheoryResult.WRONG;
        if (!matchesPurpose(purpose) || !matchesChange(change) || !matchesAnomaly(anomaly)) {
            return TheoryResult.WRONG;
        }
        String canonicalMeaning = "purpose=ordinary_refuge;change=safety_to_control;anomaly=copy_before_source";
        byte[] payload = canonicalMeaning.getBytes(StandardCharsets.UTF_8);
        journal.append("p4-theory-" + sha256(canonicalMeaning), THEORY_EVENT, payload);
        return TheoryResult.ACCEPTED;
    }

    /** Console/recovery adapter. The visible contract is three short claims separated by a pipe. */
    public synchronized TheoryResult submitConclusion(String delimited, String contributor) throws IOException {
        String[] fields = delimited == null ? new String[0] : delimited.split("\\|", -1);
        if (fields.length != 3) return TheoryResult.INCOMPLETE;
        return submitConclusion(fields[0], fields[1], fields[2], contributor);
    }

    public synchronized SelectionResult selectServiceCards(String contributor) throws IOException {
        return appendSelection(SERVICE_EVENT, contributor);
    }

    /**
     * Local outage-safe ownership of the bounded barcode/node-clock test.
     *
     * <p>The command invokes a known physical/custody operation; it does not ask the player to retype
     * the result or prove that a source was clicked. Discord owns an equivalent select interaction.
     */
    public synchronized SelectionResult testCopyOrder(String contributor) throws IOException {
        journal.append("p4-copy-order-test", COPY_TEST_EVENT,
                "method=barcode_and_node_clock;guest_metadata=excluded"
                        .getBytes(StandardCharsets.UTF_8));
        return SelectionResult.ACCEPTED;
    }

    public synchronized SelectionResult selectPenaltyCustody(String contributor) throws IOException {
        return appendSelection(PENALTY_EVENT, contributor);
    }

    public synchronized boolean commitCuration(String contributor) throws IOException {
        if (!theoryEarned() || !serviceCardsPublic() || !penaltyCopiesInCustody()) return false;
        journal.append("p5-service-chronology", CHRONOLOGY_EVENT,
                "service_cards=public;penalty_copies=evidence".getBytes(StandardCharsets.UTF_8));
        journal.append("p5-curated", CURATED_EVENT, "curated=true".getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private SelectionResult appendSelection(String event, String contributor) throws IOException {
        if (!theoryEarned()) return SelectionResult.NOT_READY;
        journal.append(event, event, event.getBytes(StandardCharsets.UTF_8));
        return SelectionResult.ACCEPTED;
    }

    public boolean theoryEarned() { return has(THEORY_EVENT); }
    public boolean copyOrderTested() { return has(COPY_TEST_EVENT); }
    public boolean serviceCardsPublic() { return has(SERVICE_EVENT); }
    public boolean penaltyCopiesInCustody() { return has(PENALTY_EVENT); }
    public boolean serviceChronologyShared() { return has(CHRONOLOGY_EVENT); }
    public boolean curated() { return has(CURATED_EVENT); }
    public List<LocalPrimaryJournal.Receipt> receipts() { return journal.after(0); }

    private boolean has(String eventType) {
        return journal.after(0).stream().anyMatch(receipt -> eventType.equals(receipt.eventType()));
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim();
        return normalized.replaceAll(" +", " ");
    }

    private static boolean matchesPurpose(String value) {
        return containsAny(value, "REFUGE", "SHELTER", "EVACUATION PLACE", "EMERGENCY PLACE");
    }

    private static boolean matchesChange(String value) {
        if (containsAny(value, "DID NOT BECOME", "NEVER BECAME", "NOT CONTROL", "NO CONTROL")) return false;
        boolean practicalCare = containsAny(value, "SAFETY", "EMERGENCY", "EVACUATION", "SMOKE",
                "CARE", "PROCEDURE", "INSTRUCTION", "GUIDANCE", "DIRECTION");
        boolean imposedControl = containsAny(value, "CONTROL", "COMPULSORY", "MANDATORY", "REQUIRED",
                "ATTENDANCE", "PENALTY", "OBEDIENCE", "MONITOR", "RULE");
        return practicalCare && imposedControl;
    }

    private static boolean matchesAnomaly(String value) {
        if (value.contains("COPY BEFORE SOURCE")) return true;
        boolean copy = value.contains("COPY");
        boolean source = containsAny(value, "SOURCE", "ORIGINAL");
        boolean reversedOrder = containsAny(value, "BEFORE", "PREDATE", "PREDATES", "EARLIER", "CAME FIRST")
                || (value.contains("SOURCE") && containsAny(value, "AFTER", "LATER"));
        return copy && source && reversedOrder;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private static boolean withinBudget(String value, int maximumCharacters, int maximumWords) {
        return value.length() <= maximumCharacters && value.split(" ").length <= maximumWords;
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public enum TheoryResult { ACCEPTED, WRONG, INCOMPLETE }
    public enum SelectionResult { ACCEPTED, NOT_READY }
}
