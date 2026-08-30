package com.observance.watcher.morrow;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Restart-safe local authority for Morrow progression. World mechanics commit here before any
 * network projection, so a remote outage cannot stall or roll back the Minecraft session.
 */
public final class MorrowLocalState {
    private static final String INITIALIZATION_KEY = "morrow:release:initialized";
    private static final String INITIALIZATION_TYPE = "morrow_release_initialized";
    private static final int MAX_PAYLOAD_BYTES = 16_384;

    private final LocalPrimaryJournal journal;
    private final String releaseId;
    private final MorrowTransitionService transitions = new MorrowTransitionService();
    private MorrowRelationshipSnapshot snapshot;

    private MorrowLocalState(LocalPrimaryJournal journal, String releaseId) throws IOException {
        this.journal = journal;
        this.releaseId = releaseId;
        List<LocalPrimaryJournal.Receipt> receipts = journal.after(0);
        if (receipts.isEmpty()) {
            journal.append(INITIALIZATION_KEY, INITIALIZATION_TYPE, releaseId.getBytes(StandardCharsets.UTF_8));
            receipts = journal.after(0);
        }
        LocalPrimaryJournal.Receipt initialization = receipts.get(0);
        String storedRelease = new String(initialization.payload(), StandardCharsets.UTF_8);
        if (!INITIALIZATION_KEY.equals(initialization.idempotencyKey())
                || !INITIALIZATION_TYPE.equals(initialization.eventType())
                || !releaseId.equals(storedRelease)) {
            throw new IOException("Morrow journal release authority mismatch");
        }
        snapshot = MorrowRelationshipSnapshot.initial(releaseId);
        for (int index = 1; index < receipts.size(); index++) apply(receipts.get(index));
    }

    public static MorrowLocalState open(Path journalPath, String releaseId) throws IOException {
        Objects.requireNonNull(journalPath, "journalPath");
        MorrowRelationshipSnapshot.initial(releaseId); // validate before creating a journal
        return new MorrowLocalState(LocalPrimaryJournal.open(journalPath), releaseId);
    }

    public synchronized CommitResult commit(String eventKey, String idempotencyKey, byte[] payload)
            throws IOException {
        if (!MorrowEventAuthority.minecraftOwns(eventKey)) {
            throw new IllegalArgumentException("Paper does not own Morrow event: " + eventKey);
        }
        return append(eventKey, idempotencyKey, payload);
    }

    /** Apply a signed event projected from its owning non-Minecraft surface. */
    public synchronized CommitResult acceptProjection(String eventKey, String idempotencyKey, byte[] payload)
            throws IOException {
        if (!MorrowEventAuthority.canonical(eventKey) || MorrowEventAuthority.minecraftOwns(eventKey)) {
            throw new IllegalArgumentException("Event is not an external Morrow projection: " + eventKey);
        }
        return append(eventKey, idempotencyKey, payload);
    }

    private CommitResult append(String eventKey, String idempotencyKey, byte[] payload) throws IOException {
        Objects.requireNonNull(payload, "payload");
        if (payload.length > MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("payload exceeds 16384 bytes");
        String prerequisite = MorrowEventAuthority.prerequisite(eventKey);
        if (prerequisite != null && !snapshot.committedEvents().contains(prerequisite)) {
            throw new IllegalStateException("missing prerequisite event: " + prerequisite);
        }
        long before = journal.after(0).size();
        LocalPrimaryJournal.Receipt receipt = journal.append(idempotencyKey, eventKey, payload);
        boolean created = receipt.sequence() > before;
        if (created) apply(receipt);
        return new CommitResult(created, receipt.sequence(), receipt.eventHash(), snapshot);
    }

    public synchronized MorrowRelationshipSnapshot snapshot() {
        return snapshot;
    }

    public String releaseId() {
        return releaseId;
    }

    /** Receipts after the durable remote cursor; callers project these asynchronously and in order. */
    public synchronized List<LocalPrimaryJournal.Receipt> pendingAfter(long projectedSequence) {
        if (projectedSequence < 0) throw new IllegalArgumentException("projected sequence must be non-negative");
        return journal.after(Math.max(1, projectedSequence)).stream()
                .filter(receipt -> MorrowEventAuthority.minecraftOwns(receipt.eventType()))
                .toList(); // initialization and received projections are never echoed
    }

    /** Returns a defensive copy of the newest durable payload for restart/catch-up reconstruction. */
    public synchronized Optional<byte[]> latestPayload(String eventKey) {
        if (!MorrowEventAuthority.canonical(eventKey)) {
            throw new IllegalArgumentException("unknown Morrow event: " + eventKey);
        }
        List<LocalPrimaryJournal.Receipt> receipts = journal.after(0);
        for (int index = receipts.size() - 1; index >= 0; index--) {
            LocalPrimaryJournal.Receipt receipt = receipts.get(index);
            if (eventKey.equals(receipt.eventType())) return Optional.of(receipt.payload().clone());
        }
        return Optional.empty();
    }

    /** Validate a persisted projector cursor against the exact locally-owned journal receipt. */
    synchronized LocalPrimaryJournal.Receipt projectedReceipt(long sequence) {
        if (sequence <= 0) throw new IllegalArgumentException("projected sequence must be positive");
        return journal.after(sequence - 1).stream()
                .filter(receipt -> receipt.sequence() == sequence)
                .filter(receipt -> MorrowEventAuthority.minecraftOwns(receipt.eventType()))
                .findFirst()
                .orElse(null);
    }

    private void apply(LocalPrimaryJournal.Receipt receipt) throws IOException {
        if (!MorrowEventAuthority.canonical(receipt.eventType())) {
            throw new IOException("unexpected event in Morrow journal: " + receipt.eventType());
        }
        snapshot = snapshot.withCommittedEvent(receipt.eventType());
        while (!snapshot.stage().terminal()) {
            MorrowTransitionService.TransitionResult result = transitions.attempt(snapshot);
            if (result.status() != MorrowTransitionService.TransitionStatus.ADVANCED) break;
            snapshot = result.snapshot();
        }
    }

    public record CommitResult(boolean created, long sequence, String eventHash,
                               MorrowRelationshipSnapshot snapshot) {
        public CommitResult {
            Objects.requireNonNull(eventHash, "eventHash");
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }
}
