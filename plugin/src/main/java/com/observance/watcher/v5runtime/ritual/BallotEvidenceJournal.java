package com.observance.watcher.v5runtime.ritual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.BallotTelemetry;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Forced local write-ahead evidence for WR05/RP03. A first ballot is frozen here before any
 * tiebreak, revote, or disconnect resnapshot is allowed to mutate the active roster.
 */
public final class BallotEvidenceJournal {
    public static final int SCHEMA_VERSION = 1;
    private static final Set<String> NODE_IDS = Set.of("WR05", "RP03");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public record Evidence(
            String nodeId,
            Set<UUID> initialRoster,
            Map<UUID, String> firstVotes,
            int maximumVisibleRosterCount,
            boolean firstBallotTied,
            int resolutionRounds,
            int disconnectResnapCount,
            long frozenAtEpochMillis) {
        public Evidence {
            if (!NODE_IDS.contains(nodeId)) {
                throw new IllegalArgumentException("unsupported ballot node " + nodeId);
            }
            initialRoster = Set.copyOf(initialRoster);
            firstVotes = Map.copyOf(firstVotes);
            if (initialRoster.isEmpty() || !initialRoster.containsAll(firstVotes.keySet())) {
                throw new IllegalArgumentException("first votes must belong to a nonempty initial roster");
            }
            if (maximumVisibleRosterCount < initialRoster.size()) {
                throw new IllegalArgumentException("maximum visible roster cannot shrink below initial roster");
            }
            int distinct = distinctChoiceCount(firstVotes);
            if (firstBallotTied && distinct < 2) {
                throw new IllegalArgumentException("a tied first ballot requires two choices");
            }
            if (resolutionRounds < 1 || disconnectResnapCount < 0 || frozenAtEpochMillis < 0) {
                throw new IllegalArgumentException("invalid ballot evidence counters");
            }
        }

        public BallotTelemetry telemetry() {
            return new BallotTelemetry(
                    initialRoster.size(),
                    maximumVisibleRosterCount,
                    initialRoster.size(),
                    firstVotes.size(),
                    distinctChoiceCount(firstVotes),
                    firstBallotTied,
                    resolutionRounds,
                    disconnectResnapCount);
        }

        private Evidence withResolution(int rounds, int resnaps) {
            if (rounds < resolutionRounds || resnaps < disconnectResnapCount) {
                throw new IllegalArgumentException("ballot evidence counters are monotonic");
            }
            return new Evidence(nodeId, initialRoster, firstVotes, maximumVisibleRosterCount,
                    firstBallotTied, rounds, resnaps, frozenAtEpochMillis);
        }
    }

    private record StoredState(
            int schemaVersion,
            String manifestSha256,
            Map<String, StoredEvidence> ballots) {
    }

    private record StoredEvidence(
            String nodeId,
            Set<String> initialRoster,
            Map<String, String> firstVotes,
            int maximumVisibleRosterCount,
            boolean firstBallotTied,
            int resolutionRounds,
            int disconnectResnapCount,
            long frozenAtEpochMillis) {
    }

    private final Path path;
    private final String manifestSha256;
    private final Map<String, Evidence> evidence;

    private BallotEvidenceJournal(Path path, String manifestSha256, Map<String, Evidence> evidence) {
        this.path = path;
        this.manifestSha256 = manifestSha256;
        this.evidence = new LinkedHashMap<>(evidence);
    }

    public static BallotEvidenceJournal open(Path path, String manifestSha256) throws IOException {
        Objects.requireNonNull(path, "path");
        if (manifestSha256 == null || !manifestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("manifest SHA-256 is invalid");
        }
        Path absolute = path.toAbsolutePath().normalize();
        Files.createDirectories(Objects.requireNonNull(absolute.getParent(), "journal parent"));
        Map<String, Evidence> loaded = new LinkedHashMap<>();
        if (Files.exists(absolute)) {
            JsonObject root = JsonParser.parseString(
                    Files.readString(absolute, StandardCharsets.UTF_8)).getAsJsonObject();
            StoredState state = GSON.fromJson(root, StoredState.class);
            if (state.schemaVersion() != SCHEMA_VERSION
                    || !manifestSha256.equals(state.manifestSha256())) {
                throw new IOException("ballot journal schema or authority hash mismatch");
            }
            if (state.ballots() == null) {
                throw new IOException("ballot journal has no ballot map");
            }
            for (Map.Entry<String, StoredEvidence> entry : state.ballots().entrySet()) {
                Evidence parsed = fromStored(entry.getValue());
                if (!entry.getKey().equals(parsed.nodeId()) || loaded.put(entry.getKey(), parsed) != null) {
                    throw new IOException("duplicate or mismatched ballot evidence " + entry.getKey());
                }
            }
        } else {
            writeAtomic(absolute, new StoredState(SCHEMA_VERSION, manifestSha256, Map.of()));
        }
        return new BallotEvidenceJournal(absolute, manifestSha256, loaded);
    }

    public synchronized Evidence freezeFirstBallot(
            String nodeId,
            Set<UUID> initialRoster,
            Map<UUID, String> firstVotes,
            int maximumVisibleRosterCount,
            boolean tied,
            long nowEpochMillis) throws IOException {
        Objects.requireNonNull(initialRoster, "initialRoster");
        Objects.requireNonNull(firstVotes, "firstVotes");
        Evidence candidate = new Evidence(
                nodeId,
                new LinkedHashSet<>(initialRoster),
                new LinkedHashMap<>(firstVotes),
                maximumVisibleRosterCount,
                tied,
                1,
                0,
                nowEpochMillis);
        Evidence current = evidence.get(nodeId);
        if (current != null) {
            if (!sameFirstBallot(current, candidate)) {
                throw new IllegalStateException("immutable first ballot already frozen for " + nodeId);
            }
            return current;
        }
        evidence.put(nodeId, candidate);
        try {
            persist();
        } catch (IOException failure) {
            evidence.remove(nodeId);
            throw failure;
        }
        return candidate;
    }

    /** Persists counters before the corresponding resnapshot/revote/tiebreak becomes visible. */
    public synchronized Evidence advanceBeforeTransition(
            String nodeId, int resolutionRounds, int disconnectResnapCount) throws IOException {
        Evidence current = require(nodeId);
        Evidence advanced = current.withResolution(resolutionRounds, disconnectResnapCount);
        if (advanced.equals(current)) {
            return current;
        }
        evidence.put(nodeId, advanced);
        try {
            persist();
        } catch (IOException failure) {
            evidence.put(nodeId, current);
            throw failure;
        }
        return advanced;
    }

    public synchronized Evidence require(String nodeId) {
        Evidence result = evidence.get(nodeId);
        if (result == null) {
            throw new IllegalStateException("first ballot has not been frozen for " + nodeId);
        }
        return result;
    }

    public synchronized Map<String, Evidence> snapshot() {
        return Map.copyOf(evidence);
    }

    private void persist() throws IOException {
        Map<String, StoredEvidence> stored = new LinkedHashMap<>();
        evidence.forEach((nodeId, value) -> stored.put(nodeId, toStored(value)));
        writeAtomic(path, new StoredState(SCHEMA_VERSION, manifestSha256, Map.copyOf(stored)));
    }

    private static boolean sameFirstBallot(Evidence left, Evidence right) {
        return left.nodeId().equals(right.nodeId())
                && left.initialRoster().equals(right.initialRoster())
                && left.firstVotes().equals(right.firstVotes())
                && left.maximumVisibleRosterCount() == right.maximumVisibleRosterCount()
                && left.firstBallotTied() == right.firstBallotTied();
    }

    private static int distinctChoiceCount(Map<UUID, String> votes) {
        return Set.copyOf(votes.values()).size();
    }

    private static StoredEvidence toStored(Evidence source) {
        Set<String> roster = new LinkedHashSet<>();
        source.initialRoster().stream().map(UUID::toString).sorted().forEach(roster::add);
        Map<String, String> votes = new LinkedHashMap<>();
        source.firstVotes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> votes.put(entry.getKey().toString(), entry.getValue()));
        return new StoredEvidence(source.nodeId(), Set.copyOf(roster), Map.copyOf(votes),
                source.maximumVisibleRosterCount(), source.firstBallotTied(),
                source.resolutionRounds(), source.disconnectResnapCount(),
                source.frozenAtEpochMillis());
    }

    private static Evidence fromStored(StoredEvidence source) {
        Set<UUID> roster = new LinkedHashSet<>();
        source.initialRoster().forEach(value -> roster.add(UUID.fromString(value)));
        Map<UUID, String> votes = new LinkedHashMap<>();
        source.firstVotes().forEach((key, value) -> votes.put(UUID.fromString(key), value));
        return new Evidence(source.nodeId(), roster, votes, source.maximumVisibleRosterCount(),
                source.firstBallotTied(), source.resolutionRounds(),
                source.disconnectResnapCount(), source.frozenAtEpochMillis());
    }

    private static void writeAtomic(Path destination, StoredState state) throws IOException {
        byte[] bytes = (GSON.toJson(state) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
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
                throw new IOException("ballot journal requires atomic replace", exception);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
