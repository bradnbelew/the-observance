package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure canonical M03/M04 bounds, sample grammar, compression, timing, and proof predicates. */
public final class EntityReplayAuthority {
    public static final int SAMPLE_INTERVAL_TICKS = 2;
    public static final int MAXIMUM_DURATION_TICKS = 900;
    public static final int MAXIMUM_SAMPLES_PER_PLAYER = 450;
    public static final int MAXIMUM_TRACKED_PLAYERS = 6;
    public static final int MISSING_ROLE_DURATION_TICKS = 740;
    public static final int MISSING_ROLE_SAMPLE_COUNT = 370;
    public static final int TRANSFER_WINDOW_START_TICK = 400;
    public static final int TRANSFER_WINDOW_END_TICK = 440;
    public static final int LIVE_TEST_MINIMUM_TICKS = 120;
    public static final double LIVE_TEST_MINIMUM_PATH = 4.0D;

    public static final String MISSING_ROLE_COMPLETED = "morrow.act2.missing_role_completed";
    public static final String LIVE_TEST_RECORDED = "morrow.act2.live_test_recorded";
    public static final String BEHAVIOR_REUSE_PROVEN = "morrow.act2.behavior_reuse_proven";
    public static final String LIVE_CAPTURE_AUTHORIZED = "morrow.act2.live_capture_authorized";

    public enum Purpose { MISSING_ROLE, DELIBERATE_LIVE_TEST }
    public enum Pose { STANDING, CROUCHING, SWIMMING, FALL_FLYING }
    public enum Action { SWING, JUMP, CROUCH, DROP, INTERACT, INVENTORY_TRANSFER }
    public enum Provenance { RECORDED, RECONSTRUCTED, LIVE }
    public enum Status { CORRECT, WRONG, NOT_READY }

    public record Boundary(double minimumX, double maximumX, double minimumY, double maximumY,
                           double minimumZ, double maximumZ) {
        public boolean contains(double x, double y, double z) {
            return x >= minimumX && x <= maximumX && y >= minimumY && y <= maximumY
                    && z >= minimumZ && z <= maximumZ;
        }
    }

    /** Relative to Recovery Room 04 origin; avoids the safe route, terminal, M02 wall, and evidence bay. */
    public static final Boundary RECORDING_BOUNDARY = new Boundary(1.0, 3.9, 0.0, 2.5, -3.9, -0.6);
    public static final Boundary MISSING_ROLE_POSITION = new Boundary(2.0, 3.0, 0.0, 2.2, -3.1, -2.0);

    public record ConsentBinding(
            String releaseId,
            UUID playerId,
            Purpose purpose,
            long revision,
            boolean granted,
            String consentHash) {
        public ConsentBinding {
            requireRelease(releaseId);
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(purpose, "purpose");
            if (revision <= 0) throw new IllegalArgumentException("consent revision must be positive");
            if (consentHash == null || !consentHash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid consent hash");
            }
        }
    }

    public record Sample(
            int tick,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            Pose pose,
            int selectedSlot,
            Set<Action> actions) {
        public Sample {
            if (tick < 0 || tick >= MAXIMUM_DURATION_TICKS || tick % SAMPLE_INTERVAL_TICKS != 0) {
                throw new IllegalArgumentException("sample tick violates the two-tick/45-second authority");
            }
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                throw new IllegalArgumentException("sample contains a non-finite value");
            }
            if (!RECORDING_BOUNDARY.contains(x, y, z)) {
                throw new IllegalArgumentException("sample left the authored ARG recording boundary");
            }
            Objects.requireNonNull(pose, "pose");
            if (selectedSlot < 0 || selectedSlot > 8) throw new IllegalArgumentException("selected slot is out of bounds");
            actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
        }

        public Sample withActions(Set<Action> updated) {
            return new Sample(tick, x, y, z, yaw, pitch, pose, selectedSlot, updated);
        }
    }

    public record Clip(
            String releaseId,
            UUID playerId,
            Purpose purpose,
            long consentRevision,
            String consentHash,
            int durationTicks,
            int rawSampleCount,
            List<Sample> samples,
            String clipHash) {
        public Clip {
            requireRelease(releaseId);
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(purpose, "purpose");
            if (consentRevision <= 0 || consentHash == null || !consentHash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("clip consent binding is invalid");
            }
            if (durationTicks <= 0 || durationTicks > MAXIMUM_DURATION_TICKS
                    || durationTicks % SAMPLE_INTERVAL_TICKS != 0) {
                throw new IllegalArgumentException("clip duration is out of bounds");
            }
            if (rawSampleCount <= 0 || rawSampleCount > MAXIMUM_SAMPLES_PER_PLAYER) {
                throw new IllegalArgumentException("clip raw sample count is out of bounds");
            }
            samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
            if (samples.isEmpty() || samples.size() > rawSampleCount) {
                throw new IllegalArgumentException("compressed sample count is invalid");
            }
            int previous = -SAMPLE_INTERVAL_TICKS;
            for (Sample sample : samples) {
                if (sample.tick() <= previous || sample.tick() >= durationTicks) {
                    throw new IllegalArgumentException("clip sample timeline is not strictly ordered");
                }
                previous = sample.tick();
            }
            if (clipHash != null && !clipHash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid clip hash");
            }
        }

        public Clip withHash(String hash) {
            return new Clip(releaseId, playerId, purpose, consentRevision, consentHash,
                    durationTicks, rawSampleCount, samples, hash);
        }
    }

    public record Decision(
            Status status,
            String eventKey,
            String idempotencyKey,
            byte[] payload,
            String feedback) {
        public Decision {
            Objects.requireNonNull(status, "status");
            payload = payload == null ? new byte[0] : payload.clone();
            if (feedback == null || feedback.isBlank()) throw new IllegalArgumentException("decision needs feedback");
            if ((eventKey == null) != (idempotencyKey == null) || (eventKey != null && status != Status.CORRECT)) {
                throw new IllegalArgumentException("only a correct predicate can produce one exact receipt");
            }
        }
        @Override public byte[] payload() { return payload.clone(); }
        public boolean commitsReceipt() { return eventKey != null; }
    }

    private EntityReplayAuthority() { }

    public static List<Sample> compress(List<Sample> raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isEmpty() || raw.size() > MAXIMUM_SAMPLES_PER_PLAYER) {
            throw new IllegalArgumentException("raw sample count is out of bounds");
        }
        List<Sample> ordered = raw.stream().sorted(Comparator.comparingInt(Sample::tick)).toList();
        List<Sample> compressed = new ArrayList<>();
        Sample previousKept = null;
        for (Sample sample : ordered) {
            if (previousKept == null || !sameState(previousKept, sample) || !sample.actions().isEmpty()) {
                compressed.add(sample);
                previousKept = sample;
            }
        }
        Sample last = ordered.get(ordered.size() - 1);
        if (compressed.get(compressed.size() - 1).tick() != last.tick()) compressed.add(last);
        return List.copyOf(compressed);
    }

    public static Decision missingRole(MorrowRelationshipSnapshot snapshot, Clip clip) {
        if (!snapshot.committedEvents().contains(MorrowDialogAuthority.ENTITY_REPLAY_AUTHORIZED)) {
            return notReady("Entity Replay must be authorized before a missing-role clip can count.");
        }
        if (clip.purpose() != Purpose.MISSING_ROLE
                || clip.durationTicks() != MISSING_ROLE_DURATION_TICKS
                || clip.rawSampleCount() != MISSING_ROLE_SAMPLE_COUNT) {
            return wrong("The loop was partial or oversized. It resets at exactly 37 seconds with no progression mutation.");
        }
        boolean transfer = clip.samples().stream().anyMatch(sample ->
                sample.tick() >= TRANSFER_WINDOW_START_TICK
                        && sample.tick() <= TRANSFER_WINDOW_END_TICK
                        && sample.actions().contains(Action.INVENTORY_TRANSFER)
                        && MISSING_ROLE_POSITION.contains(sample.x(), sample.y(), sample.z()));
        if (!transfer) {
            return wrong("The pulse log reached the transfer window, but no inventory transfer occurred at the missing position.");
        }
        return receipt(
                MISSING_ROLE_COMPLETED,
                "paper:room04:m03:missing-role:v1",
                clip,
                "{\"duration_ticks\":740,\"evidence\":[\"partial_avatar_loop\",\"redstone_pulse_log\",\"inventory_transfer\",\"captioned_voice_fragment\"],\"investigation\":\"M03\",\"raw_samples\":370}",
                "The 37-second missing role is complete. Behavioral Coverage increased from a current participant.");
    }

    public static Decision liveTest(MorrowRelationshipSnapshot snapshot, Clip clip) {
        if (!snapshot.committedEvents().contains(MISSING_ROLE_COMPLETED)) {
            return notReady("Complete the authored 37-second missing role before sealing a deliberate movement test.");
        }
        if (clip.purpose() != Purpose.DELIBERATE_LIVE_TEST
                || clip.durationTicks() < LIVE_TEST_MINIMUM_TICKS
                || clip.durationTicks() > MAXIMUM_DURATION_TICKS) {
            return wrong("The deliberate test must last at least six seconds and no more than 45 seconds.");
        }
        double path = pathLength(clip.samples());
        long actions = clip.samples().stream().mapToLong(sample -> sample.actions().size()).sum();
        long headingChanges = headingChanges(clip.samples());
        if (path < LIVE_TEST_MINIMUM_PATH || (actions < 2 && headingChanges < 2)) {
            return wrong("The sealed route is not yet distinctive: cross four blocks and add two actions or direction changes.");
        }
        String summary = "{\"designated_actions\":" + actions
                + ",\"duration_ticks\":" + clip.durationTicks()
                + ",\"heading_changes\":" + headingChanges
                + ",\"investigation\":\"M04\",\"path_milliblocks\":" + Math.round(path * 1000.0)
                + ",\"raw_samples\":" + clip.rawSampleCount() + "}";
        return receipt(
                LIVE_TEST_RECORDED,
                "paper:room04:m04:live-test:v1",
                clip,
                summary,
                "The deliberate route is sealed locally. Only its hash and bounded summary enter the event receipt.");
    }

    public static Decision behaviorReuse(
            MorrowRelationshipSnapshot snapshot,
            Clip source,
            String replayedClipHash,
            int replayDurationTicks) {
        if (!snapshot.committedEvents().contains(LIVE_TEST_RECORDED)) {
            return notReady("Seal the deliberate live test before authenticating a later reconstruction.");
        }
        if (!source.clipHash().equals(replayedClipHash) || replayDurationTicks != source.durationTicks()) {
            return wrong("The reconstructed echo does not match the locally sealed route hash and exact timing.");
        }
        return receipt(
                BEHAVIOR_REUSE_PROVEN,
                "paper:room04:m04:behavior-reuse:v1",
                source,
                "{\"investigation\":\"M04\",\"provenance\":\"reconstructed_from_current_recording\",\"replay_duration_ticks\":"
                        + replayDurationTicks + "}",
                "The reconstructed echo matches the deliberate current-player route exactly. Behavior reuse is proven.");
    }

    public static String canonicalClipBody(Clip clip) {
        StringBuilder body = new StringBuilder("morrow-entity-replay-clip-v1\n")
                .append("release=").append(clip.releaseId()).append('\n')
                .append("player=").append(clip.playerId()).append('\n')
                .append("purpose=").append(clip.purpose()).append('\n')
                .append("consent-revision=").append(clip.consentRevision()).append('\n')
                .append("consent-hash=").append(clip.consentHash()).append('\n')
                .append("duration-ticks=").append(clip.durationTicks()).append('\n')
                .append("raw-samples=").append(clip.rawSampleCount()).append('\n')
                .append("compressed-samples=").append(clip.samples().size()).append('\n');
        for (Sample sample : clip.samples()) {
            body.append(sample.tick()).append('\t')
                    .append(format(sample.x())).append('\t').append(format(sample.y())).append('\t')
                    .append(format(sample.z())).append('\t').append(format(sample.yaw())).append('\t')
                    .append(format(sample.pitch())).append('\t').append(sample.pose()).append('\t')
                    .append(sample.selectedSlot()).append('\t');
            sample.actions().stream().sorted().forEach(action -> body.append(action).append(','));
            body.append('\n');
        }
        return body.toString();
    }

    public static String clipHash(Clip clip) {
        return sha256(canonicalClipBody(clip).getBytes(StandardCharsets.UTF_8));
    }

    public static String consentHash(
            String releaseId, UUID player, Purpose purpose, long revision, boolean granted) {
        requireRelease(releaseId);
        return sha256(("morrow-entity-replay-consent-v1\nrelease=" + releaseId
                + "\nplayer=" + player + "\npurpose=" + purpose + "\nrevision=" + revision
                + "\ngranted=" + granted + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static Decision receipt(
            String event,
            String idempotency,
            Clip clip,
            String summary,
            String feedback) {
        String payload = "{\"clip_hash\":\"" + clip.clipHash()
                + "\",\"compressed_samples\":" + clip.samples().size()
                + ",\"consent_hash\":\"" + clip.consentHash()
                + "\",\"consent_revision\":" + clip.consentRevision()
                + ",\"player_ref\":\"" + playerReference(clip.releaseId(), clip.playerId())
                + "\",\"purpose\":\"" + clip.purpose().name().toLowerCase(java.util.Locale.ROOT)
                + "\",\"raw_samples_remote\":false,\"summary\":" + summary + "}";
        return new Decision(Status.CORRECT, event, idempotency,
                payload.getBytes(StandardCharsets.UTF_8), feedback);
    }

    private static Decision wrong(String feedback) {
        return new Decision(Status.WRONG, null, null, new byte[0], feedback);
    }

    private static Decision notReady(String feedback) {
        return new Decision(Status.NOT_READY, null, null, new byte[0], feedback);
    }

    private static boolean sameState(Sample left, Sample right) {
        return Double.compare(left.x(), right.x()) == 0 && Double.compare(left.y(), right.y()) == 0
                && Double.compare(left.z(), right.z()) == 0 && Float.compare(left.yaw(), right.yaw()) == 0
                && Float.compare(left.pitch(), right.pitch()) == 0 && left.pose() == right.pose()
                && left.selectedSlot() == right.selectedSlot();
    }

    private static double pathLength(List<Sample> samples) {
        double path = 0.0;
        for (int index = 1; index < samples.size(); index++) {
            Sample a = samples.get(index - 1);
            Sample b = samples.get(index);
            double dx = b.x() - a.x();
            double dy = b.y() - a.y();
            double dz = b.z() - a.z();
            path += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return path;
    }

    private static long headingChanges(List<Sample> samples) {
        long changes = 0;
        for (int index = 1; index < samples.size(); index++) {
            if (Math.abs(normalizeYaw(samples.get(index).yaw() - samples.get(index - 1).yaw())) >= 45.0F) changes++;
        }
        return changes;
    }

    private static float normalizeYaw(float yaw) {
        float value = yaw % 360.0F;
        if (value > 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private static String playerReference(String releaseId, UUID player) {
        return sha256((releaseId + ":" + player).getBytes(StandardCharsets.UTF_8));
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void requireRelease(String releaseId) {
        if (releaseId == null || !releaseId.matches("[a-z0-9][a-z0-9._-]{6,79}")) {
            throw new IllegalArgumentException("invalid replay release id");
        }
    }
}
