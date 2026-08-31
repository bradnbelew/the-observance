package com.observance.watcher.morrow.coldstorage;

import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Progress;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Record;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Atomic release-bound M11 custody, bridge, reconstruction, and access progress. */
public final class ColdStorageProgressStore {
    private static final String HEADER = "morrow-cold-storage-progress-v1";
    private final Path path; private final String releaseId;
    public ColdStorageProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }
    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 9 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M11 progress record");
        String body = String.join("\n", lines.subList(0, 8)) + "\n";
        if (!lines.get(8).equals("hash=" + sha256(body))) throw new IOException("M11 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M11 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            List<Record> chain = new ArrayList<>(); String encoded = value(lines.get(3), "chain=");
            if (!encoded.isBlank()) for (String name : encoded.split(",", -1)) chain.add(Record.valueOf(name));
            return new Progress(chain, flag(value(lines.get(4), "audit-received-current=")),
                    flag(value(lines.get(5), "current-received-audit=")),
                    flag(value(lines.get(6), "reconstruction-proven=")),
                    flag(value(lines.get(7), "access-authorized=")), revision);
        } catch (IllegalArgumentException failure) { throw new IOException("invalid M11 progress field", failure); }
    }
    public synchronized void save(Progress progress) throws IOException {
        Objects.requireNonNull(progress, "progress");
        String body = HEADER + "\nrelease=" + releaseId + "\nrevision=" + progress.revision()
                + "\nchain=" + progress.chain().stream().map(Record::name).reduce((a, b) -> a + "," + b).orElse("")
                + "\naudit-received-current=" + flag(progress.auditReceivedCurrent())
                + "\ncurrent-received-audit=" + flag(progress.currentReceivedAudit())
                + "\nreconstruction-proven=" + flag(progress.reconstructionProven())
                + "\naccess-authorized=" + flag(progress.accessAuthorized()) + "\n";
        Path parent = path.toAbsolutePath().getParent(); if (parent == null) throw new IOException("M11 progress has no parent");
        Files.createDirectories(parent); Path temporary = parent.resolve(path.getFileName() + ".tmp");
        Files.writeString(temporary, body + "hash=" + sha256(body) + "\n", StandardCharsets.UTF_8);
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.deleteIfExists(temporary); throw new IOException("filesystem does not support atomic M11 progress", unsupported);
        }
        if (!load().equals(progress)) throw new IOException("M11 progress readback mismatch");
    }
    private static String flag(boolean value) { return value ? "1" : "0"; }
    private static boolean flag(String value) throws IOException {
        if ("1".equals(value)) return true; if ("0".equals(value)) return false; throw new IOException("invalid M11 boolean");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M11 progress field"); return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
