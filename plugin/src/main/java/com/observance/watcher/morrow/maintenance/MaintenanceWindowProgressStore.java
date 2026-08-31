package com.observance.watcher.morrow.maintenance;

import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Progress;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Transfer;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Window;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Witness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Atomic release-bound M10 window, room, transfer, and safe-return progress. */
public final class MaintenanceWindowProgressStore {
    private static final String HEADER = "morrow-maintenance-window-progress-v1";
    private final Path path; private final String releaseId;
    public MaintenanceWindowProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }
    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 21 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M10 progress record");
        String body = String.join("\n", lines.subList(0, 20)) + "\n";
        if (!lines.get(20).equals("hash=" + sha256(body))) throw new IOException("M10 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M10 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            boolean retained = flag(value(lines.get(3), "retained-both="));
            String runId = value(lines.get(4), "window.id=");
            Window window = null;
            if (!runId.isBlank()) window = new Window(runId,
                    value(lines.get(5), "window.west-nonce="), value(lines.get(6), "window.east-nonce="),
                    Long.parseLong(value(lines.get(7), "window.started=")),
                    Long.parseLong(value(lines.get(8), "window.expires=")));
            Witness westWitness = witness(lines, 9, "west");
            Witness eastWitness = witness(lines, 11, "east");
            Transfer westTransfer = transfer(lines, 13, "west");
            Transfer eastTransfer = transfer(lines, 16, "east");
            String encodedLobby = value(lines.get(19), "lobby-returns=");
            List<UUID> lobby = encodedLobby.isBlank() ? List.of() : List.of(new String(
                    Base64.getUrlDecoder().decode(encodedLobby), StandardCharsets.UTF_8).split(",", -1))
                    .stream().map(UUID::fromString).toList();
            return new Progress(window, westWitness, eastWitness, westTransfer, eastTransfer,
                    retained, lobby, revision);
        } catch (IllegalArgumentException failure) { throw new IOException("invalid M10 progress field", failure); }
    }
    public synchronized Progress save(Progress progress) throws IOException {
        Window window = progress.window(); Witness westWitness = progress.westWitness();
        Witness eastWitness = progress.eastWitness(); Transfer westTransfer = progress.westTransfer();
        Transfer eastTransfer = progress.eastTransfer();
        String lobby = progress.lobbyReturns().isEmpty() ? "" : Base64.getUrlEncoder().withoutPadding()
                .encodeToString(progress.lobbyReturns().stream().map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
        String body = HEADER + "\nrelease=" + releaseId + "\nrevision=" + progress.revision()
                + "\nretained-both=" + progress.retainedBoth()
                + "\nwindow.id=" + (window == null ? "" : window.runId())
                + "\nwindow.west-nonce=" + (window == null ? "" : window.westNonce())
                + "\nwindow.east-nonce=" + (window == null ? "" : window.eastNonce())
                + "\nwindow.started=" + (window == null ? "" : window.startedTick())
                + "\nwindow.expires=" + (window == null ? "" : window.expiresTick())
                + witnessText("west", westWitness) + witnessText("east", eastWitness)
                + transferText("west", westTransfer) + transferText("east", eastTransfer)
                + "\nlobby-returns=" + lobby + "\n";
        Path parent = path.getParent(); if (parent != null) Files.createDirectories(parent);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temporary, (body + "hash=" + sha256(body) + "\n").getBytes(StandardCharsets.UTF_8));
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        Progress verified = load(); if (!verified.equals(progress)) throw new IOException("M10 progress readback mismatch");
        return verified;
    }
    private static Witness witness(List<String> lines, int index, String side) throws IOException {
        String player = value(lines.get(index), side + ".witness-player=");
        String tick = value(lines.get(index + 1), side + ".witness-tick=");
        if (player.isBlank() && tick.isBlank()) return null;
        return new Witness(UUID.fromString(player), Long.parseLong(tick), MaintenanceWindowAuthority.ACTION);
    }
    private static Transfer transfer(List<String> lines, int index, String side) throws IOException {
        String player = value(lines.get(index), side + ".transfer-player=");
        String tick = value(lines.get(index + 1), side + ".transfer-tick=");
        String hash = value(lines.get(index + 2), side + ".nonce-sha256=");
        if (player.isBlank() && tick.isBlank() && hash.isBlank()) return null;
        return new Transfer(UUID.fromString(player), Long.parseLong(tick), hash);
    }
    private static String witnessText(String side, Witness value) {
        return "\n" + side + ".witness-player=" + (value == null ? "" : value.playerId())
                + "\n" + side + ".witness-tick=" + (value == null ? "" : value.tick());
    }
    private static String transferText(String side, Transfer value) {
        return "\n" + side + ".transfer-player=" + (value == null ? "" : value.playerId())
                + "\n" + side + ".transfer-tick=" + (value == null ? "" : value.tick())
                + "\n" + side + ".nonce-sha256=" + (value == null ? "" : value.nonceSha256());
    }
    private static boolean flag(String value) throws IOException {
        if ("true".equals(value)) return true; if ("false".equals(value)) return false;
        throw new IOException("invalid M10 boolean");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M10 progress field"); return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
