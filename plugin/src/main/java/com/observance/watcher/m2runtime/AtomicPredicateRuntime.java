package com.observance.watcher.m2runtime;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Isolated M2 compare-and-swap runtime. Candidate bytes are fully hashed, parsed, and validated before
 * the active immutable snapshot can change. This class is not wired into production in M2.
 */
public final class AtomicPredicateRuntime {
    public enum Transition {
        FORWARD,
        ROLLBACK
    }

    public record Snapshot(
            PredicateAuthorityVersion version,
            PhysicalPredicateAuthority authority) {
        public Snapshot {
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(authority, "authority");
            if (!version.rawSha256().equals(authority.sha256())) {
                throw new IllegalArgumentException("Snapshot authority hash does not match version");
            }
        }
    }

    public record ActivationReceipt(
            String idempotencyKey,
            Transition transition,
            String previousRawSha256,
            String activeRawSha256,
            String activeVersionId,
            Instant committedAt) {
    }

    private final AtomicReference<Snapshot> active;
    private final Map<String, ActivationReceipt> receipts = new ConcurrentHashMap<>();
    private final Object activationLock = new Object();

    public AtomicPredicateRuntime(byte[] initialBytes, PredicateAuthorityVersion initialVersion) {
        this.active = new AtomicReference<>(validateCandidate(initialBytes, initialVersion));
    }

    public Snapshot snapshot() {
        return active.get();
    }

    public ActivationReceipt activate(
            byte[] candidateBytes,
            PredicateAuthorityVersion candidateVersion,
            String expectedActiveRawSha256,
            Transition transition,
            String idempotencyKey) {
        requireText(expectedActiveRawSha256, "expectedActiveRawSha256");
        requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(transition, "transition");

        // Expensive and failure-prone work happens before the critical section and before publication.
        Snapshot candidate = validateCandidate(candidateBytes, candidateVersion);
        synchronized (activationLock) {
            Snapshot current = active.get();
            ActivationReceipt existing = receipts.get(idempotencyKey);
            if (existing != null) {
                if (existing.transition() != transition
                        || !existing.previousRawSha256().equals(expectedActiveRawSha256)
                        || !existing.activeRawSha256().equals(candidateVersion.rawSha256())
                        || !current.version().rawSha256().equals(existing.activeRawSha256())) {
                    throw new IllegalStateException("Idempotency key is bound to a different or inactive transition");
                }
                return existing;
            }
            if (!current.version().rawSha256().equals(expectedActiveRawSha256)) {
                throw new IllegalStateException("Active predicate changed before activation");
            }
            validateTransition(current.version(), candidateVersion, transition);
            ActivationReceipt receipt = new ActivationReceipt(
                    idempotencyKey,
                    transition,
                    current.version().rawSha256(),
                    candidateVersion.rawSha256(),
                    candidateVersion.versionId(),
                    Instant.now());
            active.set(candidate);
            receipts.put(idempotencyKey, receipt);
            return receipt;
        }
    }

    private static Snapshot validateCandidate(byte[] bytes, PredicateAuthorityVersion version) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(version, "version");
        if (!PredicateAuthorityVersion.rawSha256(bytes).equals(version.rawSha256())) {
            throw new IllegalArgumentException("Candidate raw predicate hash mismatch");
        }
        if (!PredicateAuthorityVersion.semanticSha256(bytes).equals(version.semanticSha256())) {
            throw new IllegalArgumentException("Candidate semantic predicate hash mismatch");
        }
        PhysicalPredicateAuthority authority = PhysicalPredicateAuthorityLoader.load(new ByteArrayInputStream(bytes));
        return new Snapshot(version, authority);
    }

    private static void validateTransition(
            PredicateAuthorityVersion current,
            PredicateAuthorityVersion candidate,
            Transition transition) {
        if (transition == Transition.FORWARD) {
            if (!current.rawSha256().equals(candidate.predecessorRawSha256())) {
                throw new IllegalStateException("Forward candidate does not name the active predecessor");
            }
            return;
        }
        if (!candidate.rawSha256().equals(current.rollbackRawSha256())) {
            throw new IllegalStateException("Rollback candidate is not the active version's rollback authority");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
