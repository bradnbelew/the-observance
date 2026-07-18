package com.observance.watcher.arg;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local-primary P4/P5 ARG slice state. Observations never participate in its predicates. */
public final class ArgVerticalSliceState {
    public static final String THEORY_EVENT = "p4.control_reversal_earned";
    public static final String SERVICE_EVENT = "p5.service_cards_public";
    public static final String PENALTY_EVENT = "p5.penalty_copies_in_custody";
    public static final String CURATED_EVENT = "p5.civic_gallery_recurated";

    private static final Set<String> ACCEPTED_THEORIES = Set.of(
            "REFUGE BEFORE RITE SAFETY BECAME OBEDIENCE",
            "THE HOLD WAS A RATIONAL REFUGE BEFORE RITUAL INSTITUTION",
            "THE HOLD WAS A REFUGE BEFORE IT BECAME AN INSTITUTION",
            "THE HOLD SHELTERED FAMILIES BEFORE SAFETY BECAME CONTROL"
    );

    private final LocalPrimaryJournal journal;

    private ArgVerticalSliceState(LocalPrimaryJournal journal) {
        this.journal = journal;
    }

    public static ArgVerticalSliceState open(Path path) throws IOException {
        return new ArgVerticalSliceState(LocalPrimaryJournal.open(path));
    }

    public synchronized TheoryResult submitTheory(String raw, String contributor) throws IOException {
        String normalized = normalize(raw);
        if (normalized.isBlank()) return TheoryResult.INCOMPLETE;
        if (!ACCEPTED_THEORIES.contains(normalized)) return TheoryResult.WRONG;
        byte[] payload = ("theory=" + normalized).getBytes(StandardCharsets.UTF_8);
        journal.append("p4-theory-" + sha256(normalized), THEORY_EVENT, payload);
        return TheoryResult.ACCEPTED;
    }

    public synchronized SelectionResult selectServiceCards(String contributor) throws IOException {
        return appendSelection(SERVICE_EVENT, contributor);
    }

    public synchronized SelectionResult selectPenaltyCustody(String contributor) throws IOException {
        return appendSelection(PENALTY_EVENT, contributor);
    }

    public synchronized boolean commitCuration(String contributor) throws IOException {
        if (!theoryEarned() || !serviceCardsPublic() || !penaltyCopiesInCustody()) return false;
        journal.append("p5-curated", CURATED_EVENT, "curated=true".getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private SelectionResult appendSelection(String event, String contributor) throws IOException {
        if (!theoryEarned()) return SelectionResult.NOT_READY;
        journal.append(event, event, event.getBytes(StandardCharsets.UTF_8));
        return SelectionResult.ACCEPTED;
    }

    public boolean theoryEarned() { return has(THEORY_EVENT); }
    public boolean serviceCardsPublic() { return has(SERVICE_EVENT); }
    public boolean penaltyCopiesInCustody() { return has(PENALTY_EVENT); }
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
