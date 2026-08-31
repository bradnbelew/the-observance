package com.observance.watcher.morrow.coldstorage;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure M11 authority for custody ordering, cross-instance hashes, reconstruction, and access consent. */
public final class ColdStorageAuthority {
    public static final String PREREQUISITE = "morrow.act6.audit_chronology_proven";
    public static final String RECONSTRUCTION_EVENT = "morrow.act6.current_morrow_reconstruction_proven";
    public static final String ACCESS_EVENT = "morrow.act6.cold_storage_access_authorized";
    public static final List<Record> CANONICAL_CHAIN = List.of(
            Record.THEO_PERMISSION,
            Record.ROOKERY_ANCHOR_GRAPH,
            Record.IONA_SHUTDOWN,
            Record.MORROW_SNAPSHOT_MANIFEST,
            Record.CAPTIONED_VOICE_ASSEMBLY
    );
    public static final String AUDIT_SNAPSHOT_HASH = hash("morrow-m11-audit-snapshot-v1\nprocess=closed\nprovenance=authenticated-diagnostic\n");
    public static final String CURRENT_RECOVERY_HASH = hash("morrow-m11-current-recovery-v1\nprocess=reconstructed\nprovenance=recovery-snapshot-chain\n");

    private ColdStorageAuthority() { }

    public static Result append(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player, Record record) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(record, "record");
        Result unavailable = available(progress, snapshot); if (unavailable != null) return unavailable;
        if (progress.chain().contains(record))
            return readOnly(Status.DUPLICATE, progress, "That custody record is already in the chain.");
        Record expected = CANONICAL_CHAIN.get(progress.chain().size());
        if (record != expected) return readOnly(Status.BROKEN_CUSTODY_EDGE, progress,
                "Custody breaks at edge " + progress.chain().size() + ": expected " + expected.label()
                        + " before " + record.label() + ". Neither instance advanced.");
        return readOnly(Status.RECORD_ACCEPTED, progress.append(record),
                record.label() + " authenticated as custody edge " + (progress.chain().size() + 1) + " of 5.");
    }

    public static Result removeLast(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        requirePlayer(progress, snapshot, player);
        Result unavailable = available(progress, snapshot); if (unavailable != null) return unavailable;
        if (progress.chain().isEmpty()) return readOnly(Status.INCOMPLETE, progress, "The custody chain is already empty.");
        return readOnly(Status.RECORD_REMOVED, progress.removeLast(),
                "Removed the last unfiled custody edge. Authenticated source records were not changed.");
    }

    public static Result deliver(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player,
                                 Instance recipient, String suppliedHash) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(suppliedHash, "suppliedHash");
        Result unavailable = available(progress, snapshot); if (unavailable != null) return unavailable;
        if (!progress.chain().equals(CANONICAL_CHAIN))
            return readOnly(Status.INCOMPLETE, progress, "Complete the five-record custody chain before using the message bridge.");
        if (progress.delivered(recipient))
            return readOnly(Status.DUPLICATE, progress, recipient.label() + " already authenticated the other instance's hash.");
        String expected = recipient == Instance.AUDIT_SNAPSHOT ? CURRENT_RECOVERY_HASH : AUDIT_SNAPSHOT_HASH;
        if (!constantEquals(expected, suppliedHash))
            return readOnly(Status.WRONG_SNAPSHOT_HASH, progress,
                    recipient.label() + " rejected the hash. Its expected peer remained unchanged and neither instance advanced.");
        return readOnly(Status.HASH_DELIVERED, progress.deliver(recipient),
                recipient.label() + " authenticated the other instance without claiming process continuity.");
    }

    public static Result proveReconstruction(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(RECONSTRUCTION_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The reconstruction proof is already filed.");
        if (progress.reconstructionProven()) return recoverReconstruction(progress, snapshot);
        Result unavailable = available(progress, snapshot); if (unavailable != null) return unavailable;
        if (!progress.bridgeComplete())
            return readOnly(Status.INCOMPLETE, progress, "Both instances must authenticate the other's snapshot hash.");
        Progress proven = progress.proveReconstruction();
        return commit(Status.READY_TO_COMMIT_RECONSTRUCTION, proven, RECONSTRUCTION_EVENT,
                "paper:m11:current-reconstruction:v1",
                "Custody proves the original process closed and the current Morrow is a recovery. Identity remains unresolved.");
    }

    public static Result recoverReconstruction(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(RECONSTRUCTION_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The reconstruction proof is already filed.");
        if (!progress.reconstructionProven())
            return readOnly(Status.INCOMPLETE, progress, "No local M11 reconstruction proof awaits recovery.");
        return commit(Status.READY_TO_COMMIT_RECONSTRUCTION, progress, RECONSTRUCTION_EVENT,
                "paper:m11:current-reconstruction:v1",
                "Recovered the reconstruction receipt without replaying custody or bridge inputs.");
    }

    public static Result authorizeAccess(Progress progress, MorrowRelationshipSnapshot snapshot,
                                         UUID player, boolean authorize) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(ACCESS_EVENT))
            return readOnly(Status.DUPLICATE, progress, "Cold-storage access is already authorized.");
        if (!snapshot.committedEvents().contains(RECONSTRUCTION_EVENT))
            return readOnly(Status.LOCKED, progress, "File the current-Morrow reconstruction receipt before deciding access.");
        if (!authorize)
            return readOnly(Status.DECLINED, progress, "Cold-storage access remains closed. The proof and both instances were preserved.");
        if (progress.accessAuthorized()) return recoverAccess(progress, snapshot);
        Progress authorized = progress.authorizeAccess();
        return commit(Status.READY_TO_COMMIT_ACCESS, authorized, ACCESS_EVENT,
                "paper:m11:cold-storage-access:v1",
                "Cold-storage access authorized as a separate capability; this receipt does not certify Morrow as original.");
    }

    public static Result recoverAccess(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(ACCESS_EVENT))
            return readOnly(Status.DUPLICATE, progress, "Cold-storage access is already authorized.");
        if (!snapshot.committedEvents().contains(RECONSTRUCTION_EVENT) || !progress.accessAuthorized())
            return readOnly(Status.INCOMPLETE, progress, "No local M11 access receipt awaits recovery.");
        return commit(Status.READY_TO_COMMIT_ACCESS, progress, ACCESS_EVENT,
                "paper:m11:cold-storage-access:v1",
                "Recovered the separate cold-storage access receipt without reopening the evidence decision.");
    }

    public static Result reset(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(RECONSTRUCTION_EVENT) || progress.reconstructionProven())
            return readOnly(Status.IMMUTABLE, progress, "Filed or locally proven reconstruction evidence cannot be erased.");
        return readOnly(Status.RESET, progress.reset(), "Cleared only the unfiled M11 chain and bridge state.");
    }

    public static byte[] payload(Result result) {
        if (!result.commitsEvent()) throw new IllegalArgumentException("M11 result is not committable");
        if (RECONSTRUCTION_EVENT.equals(result.eventKey())) {
            return ("{\"audit_snapshot_sha256\":\"" + AUDIT_SNAPSHOT_HASH
                    + "\",\"chain_sha256\":\"" + chainHash(result.progress().chain())
                    + "\",\"continuity_claim\":null,\"current_process\":\"reconstructed\",\"current_recovery_sha256\":\""
                    + CURRENT_RECOVERY_HASH + "\",\"investigation\":\"M11\",\"original_process\":\"closed\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return "{\"capability\":\"cold_storage_access\",\"investigation\":\"M11\",\"starts_access\":false}".getBytes(StandardCharsets.UTF_8);
    }

    public static String chainHash(List<Record> chain) {
        String digest = hash("morrow-m11-chain-root-v1\n");
        for (int index = 0; index < chain.size(); index++) {
            Record record = chain.get(index);
            digest = hash("edge=" + index + "\nprevious=" + digest + "\nrecord=" + record.name()
                    + "\nartifact=" + record.artifactSha256() + "\n");
        }
        return digest;
    }

    private static Result available(Progress progress, MorrowRelationshipSnapshot snapshot) {
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return readOnly(Status.LOCKED, progress, "M11 requires Copperline's authenticated audit chronology projection.");
        if (snapshot.committedEvents().contains(RECONSTRUCTION_EVENT) || progress.reconstructionProven())
            return readOnly(Status.IMMUTABLE, progress, "The reconstruction evidence is already locally proven or filed.");
        if (progress.chain().size() >= CANONICAL_CHAIN.size() && !progress.chain().equals(CANONICAL_CHAIN))
            throw new IllegalStateException("invalid complete M11 chain");
        return null;
    }

    private static Result commit(Status status, Progress progress, String event, String idempotency, String feedback) {
        return new Result(status, progress, event, idempotency, feedback);
    }
    private static Result readOnly(Status status, Progress progress, String feedback) {
        return new Result(status, progress, null, null, feedback);
    }
    private static void require(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
    }
    private static void requirePlayer(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        require(progress, snapshot); Objects.requireNonNull(player, "player");
    }
    private static boolean constantEquals(String expected, String supplied) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII));
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public enum Record {
        THEO_PERMISSION("Theo Vale live-capture permission", "cl-ticket-1148-live-capture-authorized"),
        ROOKERY_ANCHOR_GRAPH("Rookery witness-anchor graph", "mossfield-anchor-graph-contained-route"),
        IONA_SHUTDOWN("Iona Bell shutdown order", "cl-incident-44-stop-and-preserve"),
        MORROW_SNAPSHOT_MANIFEST("Morrow diagnostic snapshot manifest", "cl-snapshot-manifest-original-process-closed"),
        CAPTIONED_VOICE_ASSEMBLY("captioned current-voice assembly", "media-current-voice-recovery-assembly-transcript");
        private final String label; private final String artifactSha256;
        Record(String label, String artifact) { this.label = label; this.artifactSha256 = hash(artifact + "\n"); }
        public String label() { return label; }
        public String artifactSha256() { return artifactSha256; }
    }
    public enum Instance {
        AUDIT_SNAPSHOT("the authenticated audit snapshot"), CURRENT_RECOVERY("the current recovery");
        private final String label; Instance(String label) { this.label = label; } public String label() { return label; }
    }
    public enum Status { LOCKED, RECORD_ACCEPTED, RECORD_REMOVED, BROKEN_CUSTODY_EDGE, HASH_DELIVERED,
        WRONG_SNAPSHOT_HASH, READY_TO_COMMIT_RECONSTRUCTION, READY_TO_COMMIT_ACCESS, DUPLICATE,
        INCOMPLETE, DECLINED, RESET, IMMUTABLE }

    public record Progress(List<Record> chain, boolean auditReceivedCurrent, boolean currentReceivedAudit,
                           boolean reconstructionProven, boolean accessAuthorized, long revision) {
        public Progress {
            chain = List.copyOf(Objects.requireNonNull(chain, "chain"));
            if (revision < 0 || chain.size() > CANONICAL_CHAIN.size() || chain.stream().distinct().count() != chain.size()
                    || ((auditReceivedCurrent || currentReceivedAudit) && !chain.equals(CANONICAL_CHAIN))
                    || (reconstructionProven && !(auditReceivedCurrent && currentReceivedAudit))
                    || (accessAuthorized && !reconstructionProven))
                throw new IllegalArgumentException("invalid M11 progress");
            for (int index = 0; index < chain.size(); index++)
                if (chain.get(index) != CANONICAL_CHAIN.get(index)) throw new IllegalArgumentException("invalid M11 custody order");
        }
        public static Progress initial() { return new Progress(List.of(), false, false, false, false, 0); }
        public boolean delivered(Instance recipient) { return recipient == Instance.AUDIT_SNAPSHOT ? auditReceivedCurrent : currentReceivedAudit; }
        public boolean bridgeComplete() { return auditReceivedCurrent && currentReceivedAudit; }
        Progress append(Record record) { List<Record> copy = new ArrayList<>(chain); copy.add(record);
            return new Progress(copy, false, false, false, false, revision + 1); }
        Progress removeLast() { List<Record> copy = new ArrayList<>(chain); copy.remove(copy.size() - 1);
            return new Progress(copy, false, false, false, false, revision + 1); }
        Progress deliver(Instance recipient) { return recipient == Instance.AUDIT_SNAPSHOT
                ? new Progress(chain, true, currentReceivedAudit, false, false, revision + 1)
                : new Progress(chain, auditReceivedCurrent, true, false, false, revision + 1); }
        Progress proveReconstruction() { return new Progress(chain, auditReceivedCurrent, currentReceivedAudit, true, false, revision + 1); }
        Progress authorizeAccess() { return new Progress(chain, auditReceivedCurrent, currentReceivedAudit, true, true, revision + 1); }
        Progress reset() { return new Progress(List.of(), false, false, false, false, revision + 1); }
    }
    public record Result(Status status, Progress progress, String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status"); Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) throw new IllegalArgumentException("M11 event pair mismatch");
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
