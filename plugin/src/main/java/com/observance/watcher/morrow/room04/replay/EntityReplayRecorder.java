package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Clip;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.ConsentBinding;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

/** Dependency-free bounded recorder. Its caller supplies main-thread snapshots, never Bukkit objects. */
public final class EntityReplayRecorder {
    public enum CaptureStatus { RECORDED, NOT_ACTIVE, CANCELLED, COMPLETE }
    public record CaptureResult(CaptureStatus status, String feedback) { }

    private final String releaseId;
    private final EntityReplayClipStore clips;
    private final Map<UUID, Session> active = new HashMap<>();

    public EntityReplayRecorder(String releaseId, EntityReplayClipStore clips) {
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.clips = Objects.requireNonNull(clips, "clips");
    }

    public synchronized void start(ConsentBinding consent) {
        Objects.requireNonNull(consent, "consent");
        if (!releaseId.equals(consent.releaseId()) || !consent.granted()
                || !EntityReplayAuthority.consentHash(consent.releaseId(), consent.playerId(), consent.purpose(),
                consent.revision(), true).equals(consent.consentHash())) {
            throw new IllegalStateException("current explicit consent is required");
        }
        if (!active.containsKey(consent.playerId())
                && active.size() >= EntityReplayAuthority.MAXIMUM_TRACKED_PLAYERS) {
            throw new IllegalStateException("six-player recording limit reached");
        }
        active.put(consent.playerId(), new Session(consent));
    }

    public synchronized CaptureResult capture(ConsentBinding currentConsent, Sample sample) {
        Objects.requireNonNull(sample, "sample");
        Session session = active.get(currentConsent.playerId());
        if (session == null) return new CaptureResult(CaptureStatus.NOT_ACTIVE, "No recording is active.");
        if (!session.sameConsent(currentConsent)) {
            active.remove(currentConsent.playerId());
            return new CaptureResult(CaptureStatus.CANCELLED,
                    "Consent changed or was revoked; the unsealed clip was deleted.");
        }
        if (sample.tick() != session.raw.size() * EntityReplayAuthority.SAMPLE_INTERVAL_TICKS) {
            active.remove(currentConsent.playerId());
            return new CaptureResult(CaptureStatus.CANCELLED,
                    "The two-tick timeline skipped or duplicated a sample; the unsealed clip was deleted.");
        }
        session.raw.add(sample);
        boolean complete = session.raw.size() == EntityReplayAuthority.MAXIMUM_SAMPLES_PER_PLAYER
                || session.consent.purpose() == Purpose.MISSING_ROLE
                && session.raw.size() == EntityReplayAuthority.MISSING_ROLE_SAMPLE_COUNT;
        return new CaptureResult(complete ? CaptureStatus.COMPLETE : CaptureStatus.RECORDED,
                complete ? "The bounded sample window is complete." : "Sample retained in the unsealed local clip.");
    }

    public synchronized Optional<Clip> seal(UUID player, int durationTicks) throws IOException {
        Session session = active.remove(player);
        if (session == null) return Optional.empty();
        if (durationTicks != session.raw.size() * EntityReplayAuthority.SAMPLE_INTERVAL_TICKS) {
            throw new IllegalArgumentException("seal duration must exactly match the two-tick sample timeline");
        }
        Clip unsigned = new Clip(releaseId, player, session.consent.purpose(), session.consent.revision(),
                session.consent.consentHash(), durationTicks, session.raw.size(),
                EntityReplayAuthority.compress(session.raw), null);
        return Optional.of(clips.persist(unsigned));
    }

    public synchronized boolean cancel(UUID player) { return active.remove(player) != null; }
    public synchronized void cancelAll() { active.clear(); }
    public synchronized boolean active(UUID player) { return active.containsKey(player); }
    public synchronized int activeCount() { return active.size(); }
    public synchronized Set<UUID> activePlayers() { return Set.copyOf(active.keySet()); }
    public synchronized int sampleCount(UUID player) {
        Session session = active.get(player);
        return session == null ? 0 : session.raw.size();
    }
    public synchronized Optional<Purpose> purpose(UUID player) {
        Session session = active.get(player);
        return session == null ? Optional.empty() : Optional.of(session.consent.purpose());
    }

    private static final class Session {
        private final ConsentBinding consent;
        private final List<Sample> raw = new ArrayList<>();
        private Session(ConsentBinding consent) { this.consent = consent; }
        private boolean sameConsent(ConsentBinding other) {
            return other != null && other.granted() && consent.equals(other);
        }
    }
}
