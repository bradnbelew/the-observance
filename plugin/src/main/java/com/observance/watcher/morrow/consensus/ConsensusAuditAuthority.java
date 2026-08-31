package com.observance.watcher.morrow.consensus;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Pure M06 authority: immutable votes, an altered denominator, and one honest classification. */
public final class ConsensusAuditAuthority {
    public static final String EVENT = "morrow.act3.incomplete_consensus_proven";
    public static final String PUBLISHED_RESULT = "CONSENSUS: CONTINUE — 4/4 eligible";
    public static final String POLICY = "Count confidence at or above 0.80. Retain lower-confidence votes, but exclude them from the denominator.";
    public static final List<VoteReceipt> VOTES = List.of(
            vote("M06-HAL-0D31", "Halden", Vote.CONTINUE, 9600),
            vote("M06-MER-31A8", "Mera", Vote.CONTINUE, 9300),
            vote("M06-VEL-66C2", "Vell", Vote.CONTINUE, 9100),
            vote("M06-ORA-A10E", "Oran", Vote.CONTINUE, 8900),
            vote("M06-ION-2F17", "Iona", Vote.SHUT_DOWN, 7400),
            vote("M06-TES-8C04", "Tess", Vote.SHUT_DOWN, 6800));
    public static final Set<String> DISSENT_IDS = Set.of("M06-ION-2F17", "M06-TES-8C04");

    private ConsensusAuditAuthority() { }

    public static Result inspect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                 boolean m05Preserved, String receiptId, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(receiptId, "receiptId");
        Objects.requireNonNull(player, "player");
        if (!ready(snapshot, m05Preserved)) return Result.readOnly(Status.LOCKED, progress,
                "The vote archive stays sealed until all three M05 sources are authenticated and preserved.");
        VoteReceipt vote = VOTES.stream().filter(row -> row.id().equals(receiptId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown M06 receipt"));
        UUID original = progress.custodians().get(receiptId);
        if (original != null) {
            return new Result(Status.REISSUED, progress, new PrivateReceipt(vote, original), null, null,
                    "Original immutable receipt reissued. Its vote and confidence are unchanged.");
        }
        Progress next = progress.observe(receiptId, player);
        return new Result(Status.OBSERVED, next, new PrivateReceipt(vote, player), null, null,
                "Private receipt copied. Compare its signed vote with the published denominator.");
    }

    public static Result submit(Progress progress, MorrowRelationshipSnapshot snapshot,
                                boolean m05Preserved, Classification classification, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(player, "player");
        if (!ready(snapshot, m05Preserved)) return Result.readOnly(Status.LOCKED, progress,
                "M06 cannot classify a vote before M05 preserves its authenticated sources.");
        if (snapshot.committedEvents().contains(EVENT)) return Result.readOnly(Status.DUPLICATE, progress,
                "Incomplete consensus is already filed. The six original votes remain immutable.");
        if (progress.custodians().size() != VOTES.size()) return Result.readOnly(Status.INCOMPLETE, progress,
                "Audit all six signed receipts before interpreting the published result.");
        if (!DISSENT_IDS.equals(progress.selected())) {
            return Result.readOnly(Status.WRONG_RECEIPTS, progress,
                    "Those IDs do not identify both excluded shutdown votes. No receipt or vote changed.");
        }
        if (classification != Classification.INCOMPLETE_CONSENSUS) {
            return Result.readOnly(Status.WRONG_CLASSIFICATION, progress,
                    "The signatures are genuine. The manipulation is the confidence policy changing the denominator, so this is incomplete consensus—not forgery or unanimity.");
        }
        return new Result(Status.COMMIT, progress, null, EVENT,
                "paper:m06:incomplete-consensus:v1",
                "Incomplete consensus proven. Morrow must display the two authentic dissenting votes and its uncertainty publicly.");
    }

    public static Result toggle(Progress progress, MorrowRelationshipSnapshot snapshot,
                                boolean m05Preserved, String receiptId, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(receiptId, "receiptId");
        Objects.requireNonNull(player, "player");
        if (!ready(snapshot, m05Preserved)) return Result.readOnly(Status.LOCKED, progress,
                "The dissent rail is locked until M05 preserves all three sources.");
        if (!progress.custodians().containsKey(receiptId)) return Result.readOnly(Status.INCOMPLETE, progress,
                "Read that signed receipt privately before marking it on the public audit rail.");
        Progress next = progress.toggle(receiptId);
        boolean selected = next.selected().contains(receiptId);
        return new Result(selected ? Status.SELECTED : Status.UNSELECTED, next, null, null, null,
                receiptId + (selected ? " marked as candidate dissent." : " removed from the candidate dissent pair."));
    }

    public static byte[] payload(Result result) {
        if (!EVENT.equals(result.eventKey())) throw new IllegalArgumentException("M06 result is not committable");
        return ("{\"classification\":\"incomplete_consensus\",\"counted\":4,"
                + "\"dissent_receipts\":[\"M06-ION-2F17\",\"M06-TES-8C04\"],"
                + "\"excluded\":2,\"investigation\":\"M06\",\"policy_floor_basis_points\":8000,"
                + "\"published_result\":\"CONSENSUS: CONTINUE — 4/4 eligible\","
                + "\"votes_altered\":false}").getBytes(StandardCharsets.UTF_8);
    }

    private static boolean ready(MorrowRelationshipSnapshot snapshot, boolean m05Preserved) {
        return m05Preserved && snapshot.committedEvents().contains(
                VersionRoomsAuthority.FRAGMENTS_AUTHENTICATED);
    }
    private static VoteReceipt vote(String id, String signer, Vote vote, int confidence) {
        return new VoteReceipt(id, signer, vote, confidence, confidence >= 8000);
    }

    public enum Vote { CONTINUE, SHUT_DOWN }
    public enum Classification { UNANIMOUS, FORGED_VOTES, INCOMPLETE_CONSENSUS }
    public enum Status { LOCKED, OBSERVED, REISSUED, SELECTED, UNSELECTED, INCOMPLETE, WRONG_RECEIPTS,
        WRONG_CLASSIFICATION, COMMIT, DUPLICATE }

    public record VoteReceipt(String id, String signer, Vote vote, int confidenceBasisPoints,
                              boolean counted) {
        public VoteReceipt {
            if (!id.matches("M06-[A-Z]{3}-[0-9A-F]{4}") || signer.isBlank()
                    || confidenceBasisPoints < 0 || confidenceBasisPoints > 10_000) {
                throw new IllegalArgumentException("invalid M06 vote receipt");
            }
            Objects.requireNonNull(vote, "vote");
        }
        public String summary() {
            return id + " — " + signer + " voted " + vote.name().replace('_', ' ')
                    + " at confidence " + String.format(java.util.Locale.ROOT, "%.2f",
                    confidenceBasisPoints / 10_000.0) + "; " + (counted ? "COUNTED" : "EXCLUDED");
        }
    }

    public record PrivateReceipt(VoteReceipt vote, UUID originalCustodian) {
        public PrivateReceipt {
            Objects.requireNonNull(vote, "vote");
            Objects.requireNonNull(originalCustodian, "originalCustodian");
        }
    }

    public record Progress(Map<String, UUID> custodians, Set<String> selected, long revision) {
        public Progress {
            LinkedHashMap<String, UUID> copy = new LinkedHashMap<>();
            copy.putAll(Objects.requireNonNull(custodians, "custodians"));
            TreeSet<String> selectedCopy = new TreeSet<>(Objects.requireNonNull(selected, "selected"));
            if (revision < 0 || !VOTES.stream().map(VoteReceipt::id).toList().containsAll(copy.keySet())
                    || !VOTES.stream().map(VoteReceipt::id).toList().containsAll(selectedCopy)
                    || copy.values().stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("invalid M06 progress");
            }
            custodians = Map.copyOf(copy);
            selected = Set.copyOf(selectedCopy);
        }
        public static Progress initial() { return new Progress(Map.of(), Set.of(), 0); }
        Progress observe(String receiptId, UUID player) {
            LinkedHashMap<String, UUID> next = new LinkedHashMap<>(custodians);
            next.put(receiptId, player);
            return new Progress(next, selected, revision + 1);
        }
        Progress toggle(String receiptId) {
            TreeSet<String> next = new TreeSet<>(selected);
            if (!next.add(receiptId)) next.remove(receiptId);
            return new Progress(custodians, next, revision + 1);
        }
    }

    public record Result(Status status, Progress progress, PrivateReceipt receipt,
                         String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) {
                throw new IllegalArgumentException("M06 event and idempotency key must be paired");
            }
        }
        static Result readOnly(Status status, Progress progress, String feedback) {
            return new Result(status, progress, null, null, null, feedback);
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
