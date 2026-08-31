package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.almost.AlmostHomeAuthority.EvidenceId;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Atomic release-bound M08 observation, chronology, note, and provenance receipt. */
public final class AlmostHomeProgressStore {
    private static final String HEADER = "morrow-almost-home-progress-v1";
    private final Path path; private final String releaseId;
    public AlmostHomeProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }
    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 8 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M08 progress record");
        String body = String.join("\n", lines.subList(0, 7)) + "\n";
        if (!lines.get(7).equals("hash=" + sha256(body))) throw new IOException("M08 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M08 progress release mismatch");
        try {
            return new Progress(parseSet(value(lines.get(5), "observed=")), parseList(value(lines.get(6), "chronology=")),
                    flag(value(lines.get(3), "note-decoded=")), flag(value(lines.get(4), "proven=")),
                    Long.parseLong(value(lines.get(2), "revision=")));
        } catch (IllegalArgumentException failure) { throw new IOException("invalid M08 progress field", failure); }
    }
    public synchronized Progress save(Progress progress) throws IOException {
        String body = HEADER + "\nrelease=" + releaseId + "\nrevision=" + progress.revision()
                + "\nnote-decoded=" + progress.noteDecoded() + "\nproven=" + progress.proven()
                + "\nobserved=" + join(EvidenceId.values(), progress.observed())
                + "\nchronology=" + join(progress.chronology().toArray(EvidenceId[]::new), progress.chronology()) + "\n";
        Path parent = path.getParent(); if (parent != null) Files.createDirectories(parent);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temporary, (body + "hash=" + sha256(body) + "\n").getBytes(StandardCharsets.UTF_8));
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        Progress verified = load(); if (!verified.equals(progress)) throw new IOException("M08 progress readback mismatch");
        return verified;
    }
    private static EnumSet<EvidenceId> parseSet(String value) {
        EnumSet<EvidenceId> result = EnumSet.noneOf(EvidenceId.class);
        if (!value.isBlank()) for (String part : value.split(",", -1)) result.add(EvidenceId.valueOf(part));
        return result;
    }
    private static List<EvidenceId> parseList(String value) {
        ArrayList<EvidenceId> result = new ArrayList<>();
        if (!value.isBlank()) for (String part : value.split(",", -1)) result.add(EvidenceId.valueOf(part));
        return result;
    }
    private static String join(EvidenceId[] order, java.util.Collection<EvidenceId> values) {
        StringBuilder result = new StringBuilder();
        for (EvidenceId evidence : order) if (values.contains(evidence)) {
            if (!result.isEmpty()) result.append(','); result.append(evidence.name());
        }
        return result.toString();
    }
    private static boolean flag(String value) throws IOException {
        if ("true".equals(value)) return true; if ("false".equals(value)) return false;
        throw new IOException("invalid M08 boolean");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M08 progress field"); return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
