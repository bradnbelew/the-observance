package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.ConsentBinding;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Atomic, release-bound, per-player and per-purpose local consent authority. */
public final class EntityReplayConsentStore {
    private final Path directory;
    private final String releaseId;

    public EntityReplayConsentStore(Path directory, String releaseId) throws IOException {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        Files.createDirectories(directory);
    }

    public synchronized ConsentBinding grant(UUID player, Purpose purpose) throws IOException {
        return write(player, purpose, true);
    }

    public synchronized ConsentBinding revoke(UUID player, Purpose purpose) throws IOException {
        return write(player, purpose, false);
    }

    public synchronized Optional<ConsentBinding> current(UUID player, Purpose purpose) throws IOException {
        Path path = path(player, purpose);
        if (!Files.exists(path)) return Optional.empty();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 7 || !"morrow-entity-replay-consent-v1".equals(lines.get(0))) {
            throw new IOException("invalid entity replay consent record");
        }
        try {
            String storedRelease = value(lines.get(1), "release=");
            UUID storedPlayer = UUID.fromString(value(lines.get(2), "player="));
            Purpose storedPurpose = Purpose.valueOf(value(lines.get(3), "purpose="));
            long revision = Long.parseLong(value(lines.get(4), "revision="));
            boolean granted = switch (value(lines.get(5), "granted=")) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new IOException("invalid entity replay consent grant");
            };
            String hash = value(lines.get(6), "hash=");
            if (!releaseId.equals(storedRelease) || !player.equals(storedPlayer) || purpose != storedPurpose
                    || !EntityReplayAuthority.consentHash(storedRelease, storedPlayer, storedPurpose, revision, granted)
                    .equals(hash)) {
                throw new IOException("entity replay consent binding/hash mismatch");
            }
            return Optional.of(new ConsentBinding(storedRelease, storedPlayer, storedPurpose, revision, granted, hash));
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid entity replay consent record", failure);
        }
    }

    private ConsentBinding write(UUID player, Purpose purpose, boolean granted) throws IOException {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(purpose, "purpose");
        long revision = current(player, purpose).map(binding -> binding.revision() + 1L).orElse(1L);
        String hash = EntityReplayAuthority.consentHash(releaseId, player, purpose, revision, granted);
        String text = "morrow-entity-replay-consent-v1\nrelease=" + releaseId + "\nplayer=" + player
                + "\npurpose=" + purpose + "\nrevision=" + revision + "\ngranted=" + granted
                + "\nhash=" + hash + "\n";
        atomicWrite(path(player, purpose), text.getBytes(StandardCharsets.UTF_8));
        return current(player, purpose).orElseThrow();
    }

    private Path path(UUID player, Purpose purpose) {
        return directory.resolve(player + "-" + purpose.name().toLowerCase(java.util.Locale.ROOT) + ".consent");
    }

    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid entity replay consent field");
        return line.substring(prefix.length());
    }

    static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
