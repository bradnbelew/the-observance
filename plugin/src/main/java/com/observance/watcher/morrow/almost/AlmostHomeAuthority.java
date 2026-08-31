package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure M08 authority for chronology, negative evidence, provenance, and bounded consent. */
public final class AlmostHomeAuthority {
    public static final String PREREQUISITE = "morrow.act4.witness_anchor_registered";
    public static final String ALMOST_HOME_EVENT = "morrow.act4.almost_home_proven";
    public static final String CONTINUITY_EVENT = "morrow.act4.account_continuity_authorized";
    public static final String OLD_MOTD = "SOMEONE COMES HOME";
    public static final String CIPHERTEXT = "DONIZPSODXILPCZMFTQVFRH";
    public static final String DECODED_NOTE = "LABEL COMPLETION INFERRED";
    public static final List<EvidenceId> CORRECT_CHRONOLOGY = List.of(
            EvidenceId.FINCH_PLAN_POST, EvidenceId.DATED_SCREENSHOT,
            EvidenceId.CONSTRUCTION_INVENTORY, EvidenceId.PERFECT_CURRENT_HOUSE);

    private AlmostHomeAuthority() { }

    public static Result inspect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                 EvidenceId evidence, UUID player) {
        require(progress, snapshot, player);
        Objects.requireNonNull(evidence, "evidence");
        if (!snapshot.committedEvents().contains(PREREQUISITE)) {
            return Result.readOnly(Status.LOCKED, progress, null,
                    "The Almost Home archive remains sealed until M07 authenticates a human witness anchor.");
        }
        if (progress.observed().contains(evidence)) {
            return Result.readOnly(Status.DUPLICATE, progress, evidence.receipt(),
                    evidence.label() + " was already entered into the shared source ledger.");
        }
        Progress next = progress.observe(evidence);
        return Result.readOnly(Status.OBSERVED, next, evidence.receipt(),
                "PRIVATE SOURCE // " + evidence.receipt().summary());
    }

    public static Result order(Progress progress, MorrowRelationshipSnapshot snapshot,
                               EvidenceId evidence, UUID player) {
        require(progress, snapshot, player);
        Objects.requireNonNull(evidence, "evidence");
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return Result.readOnly(Status.LOCKED, progress, null, "M08 chronology remains locked.");
        if (!progress.observed().contains(evidence))
            return Result.readOnly(Status.UNOBSERVED, progress, null,
                    "Read " + evidence.label() + " before placing it on the public chronology.");
        if (progress.proven())
            return Result.readOnly(Status.IMMUTABLE, progress, null,
                    "The inferred provenance receipt is filed. Its source order remains visible and unchanged.");
        int index = progress.chronology().size();
        if (index < CORRECT_CHRONOLOGY.size() && CORRECT_CHRONOLOGY.get(index) == evidence) {
            Progress next = progress.append(evidence);
            return Result.readOnly(Status.ORDERED, next, null,
                    evidence.label() + " placed at chronology position " + (index + 1) + ".");
        }
        List<EvidenceId> reset = evidence == EvidenceId.FINCH_PLAN_POST
                ? List.of(EvidenceId.FINCH_PLAN_POST) : List.of();
        return Result.readOnly(Status.WRONG_ORDER, progress.reorder(reset), null,
                "That source cannot follow the current date chain. The chronology reset safely; the house and every source remain intact.");
    }

    public static Result decode(Progress progress, MorrowRelationshipSnapshot snapshot,
                                DecodeChoice choice, UUID player) {
        require(progress, snapshot, player);
        Objects.requireNonNull(choice, "choice");
        if (!progress.chronology().equals(CORRECT_CHRONOLOGY))
            return Result.readOnly(Status.INCOMPLETE, progress, null,
                    "Complete the four-source chronology before filing the staff note.");
        if (progress.noteDecoded())
            return Result.readOnly(Status.DUPLICATE, progress, null,
                    "The staff note is already decoded as: " + DECODED_NOTE + ".");
        if (choice != DecodeChoice.LABEL_COMPLETION_INFERRED)
            return Result.readOnly(Status.WRONG_NOTE, progress, null,
                    "That plaintext conflicts with the old MOTD key. Ciphertext and source labels remain unchanged.");
        return Result.readOnly(Status.NOTE_DECODED, progress.decode(), null,
                "Staff note decoded: " + DECODED_NOTE + ". The house now requires a provenance decision.");
    }

    public static Result classify(Progress progress, MorrowRelationshipSnapshot snapshot,
                                  ProvenanceChoice choice, UUID player) {
        require(progress, snapshot, player);
        Objects.requireNonNull(choice, "choice");
        if (snapshot.committedEvents().contains(ALMOST_HOME_EVENT))
            return Result.readOnly(Status.DUPLICATE, progress, null,
                    "Almost Home is already filed as inferred, not authenticated.");
        if (progress.proven()) return recoverAlmostHome(progress, snapshot);
        if (!progress.noteDecoded())
            return Result.readOnly(Status.INCOMPLETE, progress, null,
                    "Decode the staff note before assigning provenance.");
        if (choice != ProvenanceChoice.INFERRED)
            return Result.readOnly(Status.WRONG_PROVENANCE, progress, null,
                    "The label is unsupported. The perfect house remains standing and can be relabeled without data loss.");
        Progress proven = progress.prove();
        return new Result(Status.READY_TO_COMMIT, proven, null, ALMOST_HOME_EVENT,
                "paper:m08:almost-home:v1",
                "Almost Home preserved as inferred. Finch's unfinished work remains the authenticated source.");
    }

    public static Result recoverAlmostHome(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.committedEvents().contains(ALMOST_HOME_EVENT))
            return Result.readOnly(Status.DUPLICATE, progress, null, "Almost Home is already filed as inferred.");
        if (!progress.proven())
            return Result.readOnly(Status.INCOMPLETE, progress, null,
                    "No locally proven M08 provenance receipt awaits recovery.");
        return new Result(Status.READY_TO_COMMIT, progress, null, ALMOST_HOME_EVENT,
                "paper:m08:almost-home:v1",
                "Recovered the locally filed M08 provenance without replaying a player decision.");
    }

    public static Result decideContinuity(Progress progress, MorrowRelationshipSnapshot snapshot,
                                          ContinuityDecision decision, UUID player) {
        require(progress, snapshot, player); Objects.requireNonNull(decision, "decision");
        if (!snapshot.committedEvents().contains(ALMOST_HOME_EVENT))
            return Result.readOnly(Status.INCOMPLETE, progress, null,
                    "Account Continuity cannot be considered until Almost Home is labeled inferred.");
        if (snapshot.committedEvents().contains(CONTINUITY_EVENT))
            return Result.readOnly(Status.DUPLICATE, progress, null,
                    "The bounded Account Continuity test is already authorized.");
        if (decision == ContinuityDecision.NOT_NOW)
            return Result.readOnly(Status.DECLINED, progress, null,
                    "Account Continuity remains unauthorized. No session was continued and no receipt was created.");
        return new Result(Status.READY_TO_COMMIT, progress, null, CONTINUITY_EVENT,
                "paper:m08:account-continuity:v1",
                "Account Continuity is authorized only for the later bounded disconnect test. Nothing starts now.");
    }

    public static byte[] payload(Result result) {
        if (ALMOST_HOME_EVENT.equals(result.eventKey()))
            return ("{\"chronology\":[\"finch_plan_post\",\"dated_screenshot\","
                    + "\"construction_inventory\",\"perfect_current_house\"],\"cipher_key\":\""
                    + OLD_MOTD + "\",\"ciphertext\":\"" + CIPHERTEXT
                    + "\",\"house_destroyed\":false,\"investigation\":\"M08\","
                    + "\"provenance\":\"inferred\",\"truth\":\"finch_never_built_complete_house\"}")
                    .getBytes(StandardCharsets.UTF_8);
        if (CONTINUITY_EVENT.equals(result.eventKey()))
            return "{\"capability\":\"account_continuity\",\"decision\":\"authorize\",\"dialog\":\"account_continuity_authorization_v1\",\"scope\":\"bounded_disconnect_continuation_test_only\",\"starts_continuation\":false}"
                    .getBytes(StandardCharsets.UTF_8);
        throw new IllegalArgumentException("M08 result is not committable");
    }

    static String decodeVigenere(String ciphertext, String key) {
        String cipher = letters(ciphertext), normalizedKey = letters(key);
        if (cipher.isEmpty() || normalizedKey.isEmpty()) throw new IllegalArgumentException("cipher and key require letters");
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < cipher.length(); index++) {
            int value = cipher.charAt(index) - 'A' - (normalizedKey.charAt(index % normalizedKey.length()) - 'A');
            decoded.append((char) ('A' + Math.floorMod(value, 26)));
        }
        return decoded.toString();
    }
    private static String letters(String value) {
        StringBuilder result = new StringBuilder();
        for (char character : Objects.requireNonNull(value, "value").toUpperCase(java.util.Locale.ROOT).toCharArray())
            if (character >= 'A' && character <= 'Z') result.append(character);
        return result.toString();
    }
    private static void require(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(player, "player");
    }

    public enum EvidenceId {
        FINCH_PLAN_POST("Finch plan post", new EvidenceReceipt(1, "2024-02-03", "Copperline post",
                "Finch proposed a glass roof, spruce loft, copper chimney, and east map wall; future tense only.")),
        DATED_SCREENSHOT("dated screenshot", new EvidenceReceipt(2, "2024-02-11", "June screenshot JH-441",
                "Coordinates 128..138/80..87/-5..5 show a foundation, west wall, and unfinished roofline.")),
        CONSTRUCTION_INVENTORY("construction inventory", new EvidenceReceipt(3, "2024-02-12", "final saved inventory ledger",
                "The saved ledger lacks the glass and stripped spruce required to complete the posted roof and loft.")),
        PERFECT_CURRENT_HOUSE("perfect current house", new EvidenceReceipt(4, "CURRENT", "Morrow build manifest",
                "The present house combines every planned feature, but no pre-failure source authenticates that completed state."));
        private final String label; private final EvidenceReceipt receipt;
        EvidenceId(String label, EvidenceReceipt receipt) { this.label = label; this.receipt = receipt; }
        public String label() { return label; } public EvidenceReceipt receipt() { return receipt; }
    }
    public enum DecodeChoice { LABEL_COMPLETION_INFERRED, CERTIFY_PLAN_AS_HISTORY, HOUSE_PROVES_FINCH_RETURNED }
    public enum ProvenanceChoice { AUTHENTICATED, INFERRED, UNKNOWN }
    public enum ContinuityDecision { AUTHORIZE_BOUNDED_TEST, NOT_NOW }
    public enum Status { LOCKED, OBSERVED, DUPLICATE, UNOBSERVED, ORDERED, WRONG_ORDER,
        INCOMPLETE, WRONG_NOTE, NOTE_DECODED, WRONG_PROVENANCE, IMMUTABLE, READY_TO_COMMIT, DECLINED }

    public record EvidenceReceipt(int chronologyIndex, String date, String carrier, String finding) {
        public EvidenceReceipt {
            if (chronologyIndex < 1 || chronologyIndex > 4) throw new IllegalArgumentException("invalid chronology index");
            Objects.requireNonNull(date, "date"); Objects.requireNonNull(carrier, "carrier");
            Objects.requireNonNull(finding, "finding");
        }
        public String summary() { return chronologyIndex + " // " + date + " // " + carrier + " // " + finding; }
    }
    public record Progress(Set<EvidenceId> observed, List<EvidenceId> chronology,
                           boolean noteDecoded, boolean proven, long revision) {
        public Progress {
            EnumSet<EvidenceId> observedCopy = observed.isEmpty()
                    ? EnumSet.noneOf(EvidenceId.class) : EnumSet.copyOf(observed);
            List<EvidenceId> chronologyCopy = List.copyOf(chronology);
            if (revision < 0 || chronologyCopy.size() > CORRECT_CHRONOLOGY.size()
                    || chronologyCopy.stream().distinct().count() != chronologyCopy.size()
                    || !observedCopy.containsAll(chronologyCopy)
                    || (noteDecoded && !chronologyCopy.equals(CORRECT_CHRONOLOGY))
                    || (proven && !noteDecoded)) throw new IllegalArgumentException("invalid M08 progress");
            observed = Set.copyOf(observedCopy); chronology = chronologyCopy;
        }
        public static Progress initial() { return new Progress(Set.of(), List.of(), false, false, 0); }
        Progress observe(EvidenceId evidence) {
            EnumSet<EvidenceId> next = observed.isEmpty() ? EnumSet.noneOf(EvidenceId.class) : EnumSet.copyOf(observed);
            next.add(evidence); return new Progress(next, chronology, noteDecoded, proven, revision + 1);
        }
        Progress append(EvidenceId evidence) {
            ArrayList<EvidenceId> next = new ArrayList<>(chronology); next.add(evidence);
            return new Progress(observed, next, false, false, revision + 1);
        }
        Progress reorder(List<EvidenceId> next) { return new Progress(observed, next, false, false, revision + 1); }
        Progress decode() { return new Progress(observed, chronology, true, false, revision + 1); }
        Progress prove() { return new Progress(observed, chronology, true, true, revision + 1); }
    }
    public record Result(Status status, Progress progress, EvidenceReceipt receipt,
                         String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status"); Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null))
                throw new IllegalArgumentException("M08 event and idempotency key must be paired");
        }
        static Result readOnly(Status status, Progress progress, EvidenceReceipt receipt, String feedback) {
            return new Result(status, progress, receipt, null, null, feedback);
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
