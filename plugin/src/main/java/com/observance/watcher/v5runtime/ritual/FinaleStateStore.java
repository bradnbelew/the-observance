package com.observance.watcher.v5runtime.ritual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.ConductVerdict;
import com.observance.watcher.v5runtime.ritual.RitualChoices.EndingDimensions;
import com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Forced schema-2 arm/phase record. Corruption opens a read-only FAULT maintenance lock. */
public final class FinaleStateStore {
    public static final int SCHEMA_VERSION = 2;

    public enum Phase {
        IDLE,
        ARMED,
        COMMITTED,
        DARKENING,
        SYNTAX_BREAK,
        GOODBYE,
        SAVE_AND_CODA,
        CODA,
        FAULT
    }

    public record Snapshot(
            int schemaVersion,
            String manifestSha256,
            long revision,
            Phase phase,
            String wrenOutcome,
            String nameTreatment,
            String conductVerdict,
            String armedBy,
            long armedAt,
            long cancelCutoffAt,
            String committedBy,
            long committedAt,
            Set<String> completedEffects,
            String faultReason) {
        public Snapshot {
            if (schemaVersion != SCHEMA_VERSION) {
                throw new IllegalArgumentException("finale state schema must be 2");
            }
            if (manifestSha256 == null || !manifestSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("finale manifest SHA-256 is invalid");
            }
            if (revision < 0 || phase == null || armedAt < 0 || cancelCutoffAt < 0
                    || committedAt < 0) {
                throw new IllegalArgumentException("invalid finale state counters");
            }
            wrenOutcome = safe(wrenOutcome);
            nameTreatment = safe(nameTreatment);
            conductVerdict = safe(conductVerdict);
            armedBy = safe(armedBy);
            committedBy = safe(committedBy);
            completedEffects = Set.copyOf(Objects.requireNonNull(
                    completedEffects, "completedEffects"));
            faultReason = safe(faultReason);
            if (phase == Phase.IDLE && (!wrenOutcome.isEmpty() || !nameTreatment.isEmpty()
                    || !conductVerdict.isEmpty() || !armedBy.isEmpty() || armedAt != 0
                    || cancelCutoffAt != 0 || !committedBy.isEmpty() || committedAt != 0
                    || !completedEffects.isEmpty())) {
                throw new IllegalArgumentException("IDLE finale state cannot retain an ending");
            }
            if (phase != Phase.IDLE && phase != Phase.FAULT) {
                new EndingDimensions(
                        WrenOutcome.fromWireValue(wrenOutcome),
                        NameTreatment.fromWireValue(nameTreatment),
                        ConductVerdict.fromWireValue(conductVerdict));
                if (armedBy.isEmpty() || armedAt == 0 || cancelCutoffAt <= armedAt) {
                    throw new IllegalArgumentException("armed finale state is incomplete");
                }
                if (phase.ordinal() >= Phase.COMMITTED.ordinal()
                        && (committedBy.isEmpty() || committedAt < armedAt)) {
                    throw new IllegalArgumentException("committed finale state is incomplete");
                }
            }
            if (phase == Phase.FAULT && faultReason.isEmpty()) {
                throw new IllegalArgumentException("FAULT requires a reason");
            }
        }

        public Optional<EndingDimensions> optionalDimensions() {
            if (wrenOutcome.isEmpty() || nameTreatment.isEmpty() || conductVerdict.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(dimensions());
        }

        public EndingDimensions dimensions() {
            return new EndingDimensions(
                    WrenOutcome.fromWireValue(wrenOutcome),
                    NameTreatment.fromWireValue(nameTreatment),
                    ConductVerdict.fromWireValue(conductVerdict));
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final Path path;
    private final String manifestSha256;
    private boolean writable;
    private Snapshot snapshot;

    private FinaleStateStore(Path path, String manifestSha256, Snapshot snapshot, boolean writable) {
        this.path = path;
        this.manifestSha256 = manifestSha256;
        this.snapshot = snapshot;
        this.writable = writable;
    }

    public static FinaleStateStore open(Path path, String manifestSha256) throws IOException {
        Objects.requireNonNull(path, "path");
        if (manifestSha256 == null || !manifestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("manifest SHA-256 is invalid");
        }
        Path absolute = path.toAbsolutePath().normalize();
        Files.createDirectories(Objects.requireNonNull(absolute.getParent(), "finale parent"));
        if (!Files.exists(absolute)) {
            Snapshot idle = idle(manifestSha256, 0);
            writeAtomic(absolute, idle);
            return new FinaleStateStore(absolute, manifestSha256, idle, true);
        }
        try {
            Snapshot loaded = GSON.fromJson(JsonParser.parseString(
                    Files.readString(absolute, StandardCharsets.UTF_8)), Snapshot.class);
            validateLoaded(loaded, manifestSha256);
            return new FinaleStateStore(absolute, manifestSha256, loaded, true);
        } catch (RuntimeException | IOException failure) {
            Path recovery = absolute.resolveSibling(absolute.getFileName()
                    + ".corrupt-" + System.currentTimeMillis() + "-" + UUID.randomUUID()
                    + ".recovery");
            Files.copy(absolute, recovery, StandardCopyOption.COPY_ATTRIBUTES);
            String reason = "corrupt finale state preserved at " + recovery.getFileName();
            Snapshot fault = new Snapshot(SCHEMA_VERSION, manifestSha256, 0, Phase.FAULT,
                    "", "", "", "", 0, 0, "", 0, Set.of(), reason);
            return new FinaleStateStore(absolute, manifestSha256, fault, false);
        }
    }

    public synchronized Snapshot snapshot() {
        return snapshot;
    }

    public synchronized Snapshot arm(
            EndingDimensions dimensions, String armedBy, long armedAt, long cutoffAt)
            throws IOException {
        requireWritable();
        Objects.requireNonNull(dimensions, "dimensions");
        requireText(armedBy, "armedBy");
        if (snapshot.phase() != Phase.IDLE) {
            throw new IllegalStateException("finale can arm only from IDLE");
        }
        Snapshot armed = new Snapshot(SCHEMA_VERSION, manifestSha256, snapshot.revision() + 1,
                Phase.ARMED, dimensions.wrenOutcome().wireValue(),
                dimensions.nameTreatment().wireValue(), dimensions.conductVerdict().wireValue(),
                armedBy, armedAt, cutoffAt, "", 0, Set.of(), "");
        return commit(armed);
    }

    public synchronized Snapshot cancelOrExpire() throws IOException {
        requireWritable();
        if (snapshot.phase() != Phase.ARMED) {
            throw new IllegalStateException("only ARMED can return to IDLE");
        }
        return commit(idle(manifestSha256, snapshot.revision() + 1));
    }

    public synchronized Snapshot commitPlayer(UUID playerId, long committedAt) throws IOException {
        requireWritable();
        Objects.requireNonNull(playerId, "playerId");
        if (snapshot.phase() != Phase.ARMED) {
            throw new IllegalStateException("player commit requires ARMED phase");
        }
        Snapshot committed = replacePhase(snapshot, Phase.COMMITTED,
                playerId.toString(), committedAt, snapshot.completedEffects());
        return commit(committed);
    }

    public synchronized Snapshot recoverCommitted(
            EndingDimensions dimensions, String armedBy, long armedAt, long cutoffAt,
            String committedBy, long committedAt) throws IOException {
        requireWritable();
        if (snapshot.phase().ordinal() >= Phase.COMMITTED.ordinal()) {
            return snapshot;
        }
        Snapshot recovered = new Snapshot(SCHEMA_VERSION, manifestSha256,
                snapshot.revision() + 1, Phase.COMMITTED,
                dimensions.wrenOutcome().wireValue(), dimensions.nameTreatment().wireValue(),
                dimensions.conductVerdict().wireValue(), armedBy, armedAt, cutoffAt,
                committedBy, committedAt, Set.of(), "");
        return commit(recovered);
    }

    public synchronized Snapshot transition(Phase expected, Phase next) throws IOException {
        requireWritable();
        if (snapshot.phase() != expected) {
            throw new IllegalStateException(
                    "expected finale phase " + expected + ", found " + snapshot.phase());
        }
        if (next.ordinal() != expected.ordinal() + 1 || next == Phase.FAULT) {
            throw new IllegalArgumentException("finale phases must advance exactly once");
        }
        return commit(replacePhase(snapshot, next, snapshot.committedBy(),
                snapshot.committedAt(), snapshot.completedEffects()));
    }

    public synchronized Snapshot markEffectComplete(String effectKey) throws IOException {
        requireWritable();
        requireText(effectKey, "effectKey");
        if (snapshot.completedEffects().contains(effectKey)) {
            return snapshot;
        }
        Set<String> effects = new LinkedHashSet<>(snapshot.completedEffects());
        effects.add(effectKey);
        Snapshot updated = replacePhase(snapshot, snapshot.phase(), snapshot.committedBy(),
                snapshot.committedAt(), Set.copyOf(effects));
        return commit(updated);
    }

    private Snapshot commit(Snapshot updated) throws IOException {
        writeAtomic(path, updated);
        snapshot = updated;
        return updated;
    }

    private void requireWritable() {
        if (!writable || snapshot.phase() == Phase.FAULT) {
            throw new IllegalStateException("finale state is in FAULT maintenance lock: "
                    + snapshot.faultReason());
        }
    }

    private static Snapshot idle(String hash, long revision) {
        return new Snapshot(SCHEMA_VERSION, hash, revision, Phase.IDLE,
                "", "", "", "", 0, 0, "", 0, Set.of(), "");
    }

    private static Snapshot replacePhase(
            Snapshot source, Phase phase, String committedBy, long committedAt,
            Set<String> effects) {
        return new Snapshot(SCHEMA_VERSION, source.manifestSha256(), source.revision() + 1,
                phase, source.wrenOutcome(), source.nameTreatment(), source.conductVerdict(),
                source.armedBy(), source.armedAt(), source.cancelCutoffAt(), committedBy,
                committedAt, effects, "");
    }

    private static void validateLoaded(Snapshot loaded, String manifestSha256) {
        Objects.requireNonNull(loaded, "stored finale state");
        if (!manifestSha256.equals(loaded.manifestSha256())) {
            throw new IllegalArgumentException("finale authority hash mismatch");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }

    private static void writeAtomic(Path destination, Snapshot value) throws IOException {
        byte[] bytes = (GSON.toJson(value) + System.lineSeparator())
                .getBytes(StandardCharsets.UTF_8);
        Path temporary = destination.resolveSibling(
                destination.getFileName() + ".tmp-" + UUID.randomUUID());
        boolean moved = false;
        try {
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("finale state requires atomic replace", exception);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
