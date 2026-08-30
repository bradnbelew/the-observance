package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Action;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Clip;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Pose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Content-addressed local clips; atomic write and verified readback precede every consequence. */
public final class EntityReplayClipStore {
    private final Path directory;
    private final String releaseId;

    public EntityReplayClipStore(Path directory, String releaseId) throws IOException {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        Files.createDirectories(directory);
    }

    public synchronized Clip persist(Clip unsigned) throws IOException {
        if (!releaseId.equals(unsigned.releaseId()) || unsigned.clipHash() != null) {
            throw new IOException("entity replay clip has wrong release or is already signed");
        }
        String hash = EntityReplayAuthority.clipHash(unsigned);
        Clip signed = unsigned.withHash(hash);
        Path path = path(hash);
        byte[] bytes = (EntityReplayAuthority.canonicalClipBody(unsigned) + "hash=" + hash + "\n")
                .getBytes(StandardCharsets.UTF_8);
        if (Files.exists(path)) {
            Clip existing = load(hash).orElseThrow();
            if (!EntityReplayAuthority.canonicalClipBody(existing).equals(
                    EntityReplayAuthority.canonicalClipBody(unsigned))) {
                throw new IOException("entity replay clip hash collision");
            }
            return existing;
        }
        EntityReplayConsentStore.atomicWrite(path, bytes);
        Clip verified = load(hash).orElseThrow(() -> new IOException("entity replay clip write vanished"));
        if (!verified.clipHash().equals(signed.clipHash())
                || !EntityReplayAuthority.canonicalClipBody(verified).equals(
                EntityReplayAuthority.canonicalClipBody(unsigned))) {
            throw new IOException("entity replay clip readback mismatch");
        }
        return verified;
    }

    public synchronized Optional<Clip> load(String hash) throws IOException {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) throw new IOException("invalid entity replay clip hash");
        Path path = path(hash);
        if (!Files.exists(path)) return Optional.empty();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() < 11 || !"morrow-entity-replay-clip-v1".equals(lines.get(0))) {
            throw new IOException("invalid entity replay clip record");
        }
        try {
            String storedRelease = value(lines.get(1), "release=");
            UUID player = UUID.fromString(value(lines.get(2), "player="));
            Purpose purpose = Purpose.valueOf(value(lines.get(3), "purpose="));
            long revision = Long.parseLong(value(lines.get(4), "consent-revision="));
            String consentHash = value(lines.get(5), "consent-hash=");
            int duration = Integer.parseInt(value(lines.get(6), "duration-ticks="));
            int rawCount = Integer.parseInt(value(lines.get(7), "raw-samples="));
            int count = Integer.parseInt(value(lines.get(8), "compressed-samples="));
            if (count <= 0 || lines.size() != 10 + count) throw new IOException("entity replay clip length mismatch");
            List<Sample> samples = new ArrayList<>();
            for (int index = 0; index < count; index++) samples.add(parseSample(lines.get(9 + index)));
            String storedHash = value(lines.get(9 + count), "hash=");
            Clip unsigned = new Clip(storedRelease, player, purpose, revision, consentHash,
                    duration, rawCount, samples, null);
            if (!releaseId.equals(storedRelease) || !hash.equals(storedHash)
                    || !hash.equals(EntityReplayAuthority.clipHash(unsigned))) {
                throw new IOException("entity replay clip release/hash mismatch");
            }
            return Optional.of(unsigned.withHash(hash));
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid entity replay clip record", failure);
        }
    }

    private Path path(String hash) { return directory.resolve(hash + ".clip"); }

    private static Sample parseSample(String line) throws IOException {
        String[] fields = line.split("\\t", -1);
        if (fields.length != 9) throw new IOException("invalid entity replay sample");
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        if (!fields[8].isBlank()) {
            for (String value : fields[8].split(",")) if (!value.isBlank()) actions.add(Action.valueOf(value));
        }
        return new Sample(Integer.parseInt(fields[0]), Double.parseDouble(fields[1]),
                Double.parseDouble(fields[2]), Double.parseDouble(fields[3]), Float.parseFloat(fields[4]),
                Float.parseFloat(fields[5]), Pose.valueOf(fields[6]), Integer.parseInt(fields[7]), actions);
    }

    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid entity replay clip field");
        return line.substring(prefix.length());
    }
}
