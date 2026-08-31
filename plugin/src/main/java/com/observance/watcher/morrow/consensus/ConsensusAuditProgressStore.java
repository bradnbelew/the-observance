package com.observance.watcher.morrow.consensus;

import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Atomic release-bound M06 custody store. */
public final class ConsensusAuditProgressStore {
    private static final String HEADER = "morrow-consensus-audit-progress-v1";
    private final Path path;
    private final String releaseId;

    public ConsensusAuditProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }

    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 11 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M06 progress record");
        String body = String.join("\n", lines.subList(0, 10)) + "\n";
        if (!lines.get(10).equals("hash=" + sha256(body))) throw new IOException("M06 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M06 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            String selectedValue = value(lines.get(3), "selected=");
            Set<String> selected = selectedValue.isBlank() ? Set.of() : Set.of(selectedValue.split(","));
            LinkedHashMap<String, UUID> custodians = new LinkedHashMap<>();
            for (int index = 0; index < ConsensusAuditAuthority.VOTES.size(); index++) {
                String id = ConsensusAuditAuthority.VOTES.get(index).id();
                String stored = value(lines.get(4 + index), "custodian." + id + "=");
                if (!stored.isBlank()) custodians.put(id, UUID.fromString(stored));
            }
            return new Progress(custodians, selected, revision);
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid M06 progress field", failure);
        }
    }

    public synchronized Progress save(Progress progress) throws IOException {
        StringBuilder body = new StringBuilder(HEADER).append('\n')
                .append("release=").append(releaseId).append('\n')
                .append("revision=").append(progress.revision()).append('\n')
                .append("selected=").append(String.join(",", new java.util.TreeSet<>(progress.selected())))
                .append('\n');
        for (ConsensusAuditAuthority.VoteReceipt vote : ConsensusAuditAuthority.VOTES) {
            UUID custodian = progress.custodians().get(vote.id());
            body.append("custodian.").append(vote.id()).append('=')
                    .append(custodian == null ? "" : custodian).append('\n');
        }
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
        if (!verified.equals(progress)) throw new IOException("M06 progress readback mismatch");
        return verified;
    }

    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M06 progress field");
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
