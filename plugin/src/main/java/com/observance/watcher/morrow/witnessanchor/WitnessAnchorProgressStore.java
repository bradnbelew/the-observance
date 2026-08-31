package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.CellId;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.CommittedAnchor;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.MaterialChoice;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Atomic release-bound M07 draft, committed version, and mismatch receipt. */
public final class WitnessAnchorProgressStore {
    private static final String HEADER = "morrow-witness-anchor-progress-v1";
    private final Path path;
    private final String releaseId;

    public WitnessAnchorProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }

    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 15 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M07 progress record");
        String body = String.join("\n", lines.subList(0, 14)) + "\n";
        if (!lines.get(14).equals("hash=" + sha256(body))) throw new IOException("M07 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M07 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            String identifiedValue = value(lines.get(3), "identified=");
            if (!identifiedValue.equals("true") && !identifiedValue.equals("false")) {
                throw new IOException("invalid M07 identified flag");
            }
            EnumMap<CellId, MaterialChoice> draft = new EnumMap<>(CellId.class);
            for (int index = 0; index < CellId.values().length; index++) {
                CellId cell = CellId.values()[index];
                String stored = value(lines.get(4 + index), "draft." + cell.name() + "=");
                if (!stored.isBlank()) draft.put(cell, MaterialChoice.valueOf(stored));
            }
            String anchorHash = value(lines.get(10), "anchor.original-sha256=");
            String mismatchValue = value(lines.get(11), "anchor.mismatch=");
            String originalValue = value(lines.get(12), "anchor.original=");
            String reconstructionValue = value(lines.get(13), "anchor.reconstruction=");
            CommittedAnchor anchor = null;
            if (!anchorHash.isBlank() || !mismatchValue.isBlank()
                    || !originalValue.isBlank() || !reconstructionValue.isBlank()) {
                EnumMap<CellId, MaterialChoice> original = parseMaterials(originalValue);
                EnumMap<CellId, MaterialChoice> reconstruction = parseMaterials(reconstructionValue);
                anchor = new CommittedAnchor(original, reconstruction, CellId.valueOf(mismatchValue),
                        anchorHash, WitnessAnchorAuthority.hash(reconstruction), 1);
            }
            return new Progress(draft, anchor, Boolean.parseBoolean(identifiedValue), revision);
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid M07 progress field", failure);
        }
    }

    public synchronized Progress save(Progress progress) throws IOException {
        StringBuilder body = new StringBuilder(HEADER).append('\n')
                .append("release=").append(releaseId).append('\n')
                .append("revision=").append(progress.revision()).append('\n')
                .append("identified=").append(progress.identified()).append('\n');
        for (CellId cell : CellId.values()) {
            MaterialChoice choice = progress.draft().get(cell);
            body.append("draft.").append(cell.name()).append('=')
                    .append(choice == null ? "" : choice.name()).append('\n');
        }
        CommittedAnchor anchor = progress.anchor();
        body.append("anchor.original-sha256=").append(anchor == null ? "" : anchor.originalSha256()).append('\n')
                .append("anchor.mismatch=").append(anchor == null ? "" : anchor.mismatch().name()).append('\n')
                .append("anchor.original=").append(anchor == null ? "" : materials(anchor.original())).append('\n')
                .append("anchor.reconstruction=").append(anchor == null ? "" : materials(anchor.reconstruction())).append('\n');
        byte[] bytes = (body + "hash=" + sha256(body.toString()) + "\n").getBytes(StandardCharsets.UTF_8);
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
        if (!verified.equals(progress)) throw new IOException("M07 progress readback mismatch");
        return verified;
    }

    private static String materials(java.util.Map<CellId, MaterialChoice> values) {
        StringBuilder result = new StringBuilder();
        for (CellId cell : CellId.values()) {
            if (!result.isEmpty()) result.append(',');
            result.append(values.get(cell).name());
        }
        return result.toString();
    }
    private static EnumMap<CellId, MaterialChoice> parseMaterials(String value) throws IOException {
        String[] parts = value.split(",", -1);
        if (parts.length != CellId.values().length) throw new IOException("invalid M07 material list");
        EnumMap<CellId, MaterialChoice> result = new EnumMap<>(CellId.class);
        for (int index = 0; index < parts.length; index++) {
            result.put(CellId.values()[index], MaterialChoice.valueOf(parts[index]));
        }
        return result;
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M07 progress field");
        return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
