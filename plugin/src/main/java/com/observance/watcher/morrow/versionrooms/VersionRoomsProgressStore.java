package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Progress;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.RoomVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Atomic release-bound M05 partial-route store; no remote service is needed for recovery. */
public final class VersionRoomsProgressStore {
    private static final String HEADER = "morrow-version-rooms-progress-v2";
    private final Path path;
    private final String releaseId;

    public VersionRoomsProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }

    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 9 || !HEADER.equals(lines.get(0))) {
            throw new IOException("invalid M05 progress record");
        }
        String body = String.join("\n", lines.subList(0, 8)) + "\n";
        if (!lines.get(8).equals("hash=" + sha256(body))) {
            throw new IOException("M05 progress hash mismatch");
        }
        try {
            String storedRelease = value(lines.get(1), "release=");
            if (!releaseId.equals(storedRelease)) throw new IOException("M05 progress release mismatch");
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            List<RoomVersion> route = new ArrayList<>();
            String routeValue = value(lines.get(3), "route=");
            if (!routeValue.isBlank()) {
                for (String part : routeValue.split(",")) route.add(RoomVersion.valueOf(part));
            }
            String preservedValue = value(lines.get(4), "preserved=");
            if (!preservedValue.equals("true") && !preservedValue.equals("false")) {
                throw new IOException("invalid M05 preserved flag");
            }
            boolean preserved = Boolean.parseBoolean(preservedValue);
            EnumMap<RoomVersion, UUID> witnesses = new EnumMap<>(RoomVersion.class);
            for (int index = 0; index < RoomVersion.values().length; index++) {
                RoomVersion version = RoomVersion.values()[index];
                String witness = value(lines.get(5 + index), "witness." + version.name() + "=");
                if (!witness.isBlank()) witnesses.put(version, UUID.fromString(witness));
            }
            return new Progress(route, witnesses, preserved, revision);
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid M05 progress field", failure);
        }
    }

    public synchronized Progress save(Progress progress) throws IOException {
        Objects.requireNonNull(progress, "progress");
        StringBuilder body = new StringBuilder(HEADER).append('\n')
                .append("release=").append(releaseId).append('\n')
                .append("revision=").append(progress.revision()).append('\n')
                .append("route=");
        for (int index = 0; index < progress.route().size(); index++) {
            if (index > 0) body.append(',');
            body.append(progress.route().get(index).name());
        }
        body.append('\n').append("preserved=").append(progress.preserved()).append('\n');
        for (RoomVersion version : RoomVersion.values()) {
            UUID witness = progress.witnesses().get(version);
            body.append("witness.").append(version.name()).append('=')
                    .append(witness == null ? "" : witness).append('\n');
        }
        byte[] bytes = (body + "hash=" + sha256(body.toString()) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        Progress verified = load();
        if (!verified.equals(progress)) throw new IOException("M05 progress readback mismatch");
        return verified;
    }

    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M05 progress field");
        return line.substring(prefix.length());
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
