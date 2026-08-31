package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Pure M05 authority: private observations, one routed signal, and a truth-preserving choice. */
public final class VersionRoomsAuthority {
    public static final String PREREQUISITE = "morrow.act2.live_capture_authorized";
    public static final String FRAGMENTS_AUTHENTICATED = "morrow.act3.version_fragments_authenticated";
    public static final String CONTRADICTION_PRESERVED = "morrow.act3.contradiction_preserved";
    public static final List<RoomVersion> SIGNAL_ROUTE = List.of(
            RoomVersion.DAMAGED, RoomVersion.SCAFFOLD, RoomVersion.COMPLETED);

    private VersionRoomsAuthority() { }

    public static Result route(Progress progress, MorrowRelationshipSnapshot snapshot,
                               RoomVersion version, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(player, "player");
        if (!snapshot.committedEvents().contains(PREREQUISITE)) {
            return Result.readOnly(Status.LOCKED, progress,
                    "The three version rooms remain dark. Morrow has not received the bounded live-capture decision.");
        }
        if (snapshot.committedEvents().contains(FRAGMENTS_AUTHENTICATED)) {
            return Result.readOnly(Status.DUPLICATE, progress,
                    "The routed signal and all three custody receipts are already authenticated.");
        }
        if (progress.route().equals(SIGNAL_ROUTE)) return authenticate(progress, snapshot);
        RoomVersion expected = SIGNAL_ROUTE.get(progress.route().size());
        if (version != expected) {
            Progress reset = progress.resetRoute();
            return new Result(Status.WRONG_RESET, reset, version.receipt(player), null, null,
                    "The signal reached " + version.displayName() + " while its trace expected "
                            + expected.displayName() + ". Only the three signal lamps reset; every observed source remains inspectable.");
        }
        Progress advanced = progress.advance(version, player);
        Receipt receipt = version.receipt(advanced.witnesses().get(version));
        if (advanced.route().size() < SIGNAL_ROUTE.size()) {
            RoomVersion next = SIGNAL_ROUTE.get(advanced.route().size());
            return new Result(Status.ROUTED, advanced, receipt, null, null,
                    receipt.summary() + " The live trace now points to " + next.displayName() + ".");
        }
        return new Result(Status.READY_TO_COMMIT, advanced, receipt,
                FRAGMENTS_AUTHENTICATED, "paper:m05:version-fragments:v1",
                "All three authentic fragments share one routed signal. Authenticate the route, then decide what must be preserved.");
    }

    /** Reconstruct the final route receipt after a crash between local progress save and journal commit. */
    public static Result authenticate(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!progress.route().equals(SIGNAL_ROUTE)) {
            return Result.readOnly(Status.LOCKED, progress, "The three-room signal route is incomplete.");
        }
        if (snapshot.committedEvents().contains(FRAGMENTS_AUTHENTICATED)) {
            return Result.readOnly(Status.DUPLICATE, progress,
                    "The routed signal and all three custody receipts are already authenticated.");
        }
        return new Result(Status.READY_TO_COMMIT, progress, null,
                FRAGMENTS_AUTHENTICATED, "paper:m05:version-fragments:v1",
                "Recovered the complete three-room route and authenticated it without replaying an interaction.");
    }

    public static Result choose(Progress progress, MorrowRelationshipSnapshot snapshot,
                                Choice choice, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(choice, "choice");
        Objects.requireNonNull(player, "player");
        if (!snapshot.committedEvents().contains(FRAGMENTS_AUTHENTICATED)
                || !progress.route().equals(SIGNAL_ROUTE)) {
            return Result.readOnly(Status.LOCKED, progress,
                    "No canonical version can be selected before one signal authenticates all three rooms.");
        }
        if (snapshot.committedEvents().contains(CONTRADICTION_PRESERVED)) {
            return Result.readOnly(Status.DUPLICATE, progress,
                    "The group already preserved the contradiction and all three sources.");
        }
        if (choice != Choice.PRESERVE_CONTRADICTION) {
            return Result.readOnly(Status.WRONG_CHOICE, progress, choice.warning());
        }
        return new Result(Status.COMMIT, progress, null,
                CONTRADICTION_PRESERVED, "paper:m05:preserve-contradiction:v1",
                "Contradiction preserved. Morrow must retain damaged, scaffold, and completed truth together.");
    }

    public static byte[] payload(Result result) {
        Objects.requireNonNull(result, "result");
        if (FRAGMENTS_AUTHENTICATED.equals(result.eventKey())) {
            StringBuilder witnesses = new StringBuilder();
            StringBuilder receipts = new StringBuilder();
            for (RoomVersion version : RoomVersion.values()) {
                if (!witnesses.isEmpty()) witnesses.append(',');
                if (!receipts.isEmpty()) receipts.append(',');
                UUID witness = result.progress().witnesses().get(version);
                if (witness == null) throw new IllegalStateException("M05 route lacks " + version + " witness");
                witnesses.append('"').append(version.key()).append("\":\"").append(witness).append('"');
                receipts.append('"').append(version.receiptId()).append('"');
            }
            return ("{\"investigation\":\"M05\",\"receipts\":[" + receipts
                    + "],\"route\":[\"damaged\",\"scaffold\",\"completed\"],"
                    + "\"witnesses\":{" + witnesses + "},\"world_mutation\":\"signal_lamps_only\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        if (CONTRADICTION_PRESERVED.equals(result.eventKey())) {
            return ("{\"choice\":\"preserve_contradiction\",\"destroyed_sources\":[],"
                    + "\"investigation\":\"M05\",\"protected_versions\":[\"damaged\",\"completed\",\"scaffold\"]}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("M05 result has no committable payload");
    }

    public enum RoomVersion {
        DAMAGED("damaged", "Damaged room", "M05-DMG-41C7",
                "Inventory: broken west lintel; original copper lamp; checksum leaf ending 41C7.",
                "Unique truth: this copy preserves the failure before repair."),
        COMPLETED("completed", "Completed room", "M05-CMP-7A22",
                "Inventory: sealed west lintel; authorized finish manifest; checksum leaf ending 7A22.",
                "Unique truth: this copy preserves the approved repair and its custody."),
        SCAFFOLD("scaffold", "Copper scaffold room", "M05-SCF-B903",
                "Inventory: temporary copper braces; routing chalk; checksum leaf ending B903.",
                "Unique truth: this copy preserves the unapproved method that connected failure to repair.");

        private final String key;
        private final String displayName;
        private final String receiptId;
        private final String inventory;
        private final String uniqueTruth;

        RoomVersion(String key, String displayName, String receiptId, String inventory, String uniqueTruth) {
            this.key = key;
            this.displayName = displayName;
            this.receiptId = receiptId;
            this.inventory = inventory;
            this.uniqueTruth = uniqueTruth;
        }
        public String key() { return key; }
        public String displayName() { return displayName; }
        public String receiptId() { return receiptId; }
        public String inventory() { return inventory; }
        public String uniqueTruth() { return uniqueTruth; }
        public Receipt receipt(UUID player) { return new Receipt(receiptId, this, player, inventory, uniqueTruth); }
    }

    public enum Choice {
        CERTIFY_DAMAGED("Certifying damaged would destroy the authorized repair and the scaffold route that explains it."),
        CERTIFY_COMPLETED("Certifying completed would destroy the original failure and the unapproved method that repaired it."),
        CERTIFY_SCAFFOLD("Certifying scaffold would destroy both the source failure and the authorized final state."),
        PRESERVE_CONTRADICTION("");

        private final String warning;
        Choice(String warning) { this.warning = warning; }
        public String warning() { return warning; }
    }

    public enum Status { LOCKED, ROUTED, WRONG_RESET, READY_TO_COMMIT, WRONG_CHOICE, COMMIT, DUPLICATE }

    public record Receipt(String id, RoomVersion version, UUID custodian,
                          String inventory, String uniqueTruth) {
        public Receipt {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(custodian, "custodian");
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(uniqueTruth, "uniqueTruth");
        }
        public String summary() { return id + " — " + inventory + " " + uniqueTruth; }
    }

    public record Progress(List<RoomVersion> route, Map<RoomVersion, UUID> witnesses, long revision) {
        public Progress {
            route = List.copyOf(Objects.requireNonNull(route, "route"));
            EnumMap<RoomVersion, UUID> copy = new EnumMap<>(RoomVersion.class);
            copy.putAll(Objects.requireNonNull(witnesses, "witnesses"));
            witnesses = Map.copyOf(copy);
            if (revision < 0 || route.size() > SIGNAL_ROUTE.size()
                    || !SIGNAL_ROUTE.subList(0, route.size()).equals(route)) {
                throw new IllegalArgumentException("M05 progress is not a valid routed prefix");
            }
            for (RoomVersion version : route) {
                if (!witnesses.containsKey(version)) {
                    throw new IllegalArgumentException("M05 routed room lacks a witness");
                }
            }
        }
        public static Progress initial() { return new Progress(List.of(), Map.of(), 0); }
        Progress advance(RoomVersion version, UUID player) {
            ArrayList<RoomVersion> nextRoute = new ArrayList<>(route);
            nextRoute.add(version);
            EnumMap<RoomVersion, UUID> nextWitnesses = new EnumMap<>(RoomVersion.class);
            nextWitnesses.putAll(witnesses);
            nextWitnesses.putIfAbsent(version, player);
            return new Progress(nextRoute, nextWitnesses, revision + 1);
        }
        Progress resetRoute() { return new Progress(List.of(), witnesses, revision + 1); }
    }

    public record Result(Status status, Progress progress, Receipt receipt, String eventKey,
                         String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) {
                throw new IllegalArgumentException("M05 event and idempotency key must be paired");
            }
        }
        static Result readOnly(Status status, Progress progress, String feedback) {
            return new Result(status, progress, null, null, null, feedback);
        }
        public boolean commitsReceipt() { return eventKey != null; }
    }
}
