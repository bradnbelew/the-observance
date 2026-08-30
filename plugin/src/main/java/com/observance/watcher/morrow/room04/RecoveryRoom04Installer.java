package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Transactional occupied-cell-refusing installer and read-back auditor for Recovery Room 04. */
public final class RecoveryRoom04Installer {
    private static final String SNAPSHOT_SCHEMA = "morrow-room04-rollback-v1";
    private static final String RECEIPT_SCHEMA = "morrow-room04-install-v1";

    public enum Status { BUILT, RECOVERED_AND_BUILT, ALREADY_PRESENT }

    public record Origin(int x, int y, int z) { }

    public record Result(Status status, String manifestSha256, String snapshotSha256, int blockCount) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(manifestSha256, "manifestSha256");
            Objects.requireNonNull(snapshotSha256, "snapshotSha256");
        }
    }

    public interface WorldPort {
        String binding();
        String blockData(Cell relative);
        boolean isAir(Cell relative);
        void setBlockData(Cell relative, String blockData);
    }

    public static final class OccupiedCellsException extends IOException {
        private final int occupiedCount;
        private final Cell firstOccupied;

        private OccupiedCellsException(int occupiedCount, Cell firstOccupied) {
            super("Recovery Room 04 target is occupied at " + occupiedCount
                    + " cell(s); first=" + firstOccupied);
            this.occupiedCount = occupiedCount;
            this.firstOccupied = firstOccupied;
        }

        public int occupiedCount() { return occupiedCount; }
        public Cell firstOccupied() { return firstOccupied; }
    }

    private record Snapshot(
            String releaseId,
            String worldBinding,
            Origin origin,
            String manifestSha256,
            Map<Cell, String> blocks,
            String snapshotSha256) { }

    private record Receipt(
            String releaseId,
            String worldBinding,
            Origin origin,
            String manifestSha256,
            String snapshotSha256) { }

    private final RecoveryRoom04Manifest manifest;
    private final Path snapshotPath;
    private final Path receiptPath;

    public RecoveryRoom04Installer(
            RecoveryRoom04Manifest manifest,
            Path snapshotPath,
            Path receiptPath) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.snapshotPath = Objects.requireNonNull(snapshotPath, "snapshotPath");
        this.receiptPath = Objects.requireNonNull(receiptPath, "receiptPath");
    }

    public synchronized Result install(
            String releaseId,
            Origin origin,
            WorldPort world) throws IOException {
        requireBinding(releaseId, origin, world);
        if (Files.exists(receiptPath)) {
            Receipt receipt = readReceipt();
            verifyReceiptBinding(receipt, releaseId, origin, world.binding());
            Snapshot snapshot = readSnapshot();
            verifySnapshotBinding(snapshot, releaseId, origin, world.binding());
            if (!receipt.snapshotSha256().equals(snapshot.snapshotSha256())) {
                throw new IOException("Recovery Room 04 receipt/snapshot hash mismatch");
            }
            audit(world);
            return new Result(Status.ALREADY_PRESENT, manifest.manifestSha256(),
                    snapshot.snapshotSha256(), manifest.cells().size());
        }

        boolean recovered = false;
        if (Files.exists(snapshotPath)) {
            Snapshot interrupted = readSnapshot();
            verifySnapshotBinding(interrupted, releaseId, origin, world.binding());
            restore(world, interrupted);
            auditSnapshot(world, interrupted);
            recovered = true;
        }

        refuseOccupied(world);
        Snapshot snapshot = capture(releaseId, origin, world);
        persistSnapshot(snapshot);
        try {
            apply(world);
            audit(world);
            persistReceipt(new Receipt(
                    releaseId,
                    world.binding(),
                    origin,
                    manifest.manifestSha256(),
                    snapshot.snapshotSha256()));
        } catch (Throwable failure) {
            try {
                restore(world, snapshot);
                auditSnapshot(world, snapshot);
            } catch (Throwable rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            if (failure instanceof IOException ioFailure) throw ioFailure;
            if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
            throw new IOException("Recovery Room 04 installation failed", failure);
        }
        return new Result(
                recovered ? Status.RECOVERED_AND_BUILT : Status.BUILT,
                manifest.manifestSha256(),
                snapshot.snapshotSha256(),
                manifest.cells().size());
    }

    public synchronized void audit(WorldPort world) throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<Cell, String> entry : manifest.cells().entrySet()) {
            String actual = world.blockData(entry.getKey());
            if (!entry.getValue().equals(actual) && mismatches.size() < 8) {
                mismatches.add(entry.getKey() + " expected=" + entry.getValue() + " actual=" + actual);
            }
        }
        for (Cell safe : manifest.safeCells()) {
            if (!world.isAir(safe) || !world.isAir(safe.above()) || world.isAir(safe.below())) {
                mismatches.add("unsafe spawn/exit cell " + safe);
            }
        }
        if (!mismatches.isEmpty()) {
            throw new IOException("Recovery Room 04 read-back audit failed: " + mismatches);
        }
    }

    private void refuseOccupied(WorldPort world) throws OccupiedCellsException {
        int count = 0;
        Cell first = null;
        for (Cell cell : manifest.cells().keySet()) {
            if (!world.isAir(cell)) {
                if (first == null) first = cell;
                count++;
            }
        }
        if (count > 0) throw new OccupiedCellsException(count, first);
    }

    private Snapshot capture(String releaseId, Origin origin, WorldPort world) {
        LinkedHashMap<Cell, String> blocks = new LinkedHashMap<>();
        manifest.cells().keySet().stream().sorted().forEach(cell -> blocks.put(cell, world.blockData(cell)));
        String unsigned = snapshotText(releaseId, world.binding(), origin, manifest.manifestSha256(), blocks, null);
        String snapshotHash = sha256(unsigned.getBytes(StandardCharsets.UTF_8));
        return new Snapshot(releaseId, world.binding(), origin, manifest.manifestSha256(),
                Map.copyOf(blocks), snapshotHash);
    }

    private void apply(WorldPort world) {
        for (Map.Entry<Cell, String> entry : manifest.cells().entrySet()) {
            if (!RecoveryRoom04Manifest.AIR.equals(entry.getValue())) {
                world.setBlockData(entry.getKey(), entry.getValue());
            }
        }
    }

    private void restore(WorldPort world, Snapshot snapshot) {
        snapshot.blocks().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> world.setBlockData(entry.getKey(), entry.getValue()));
    }

    private void auditSnapshot(WorldPort world, Snapshot snapshot) throws IOException {
        for (Map.Entry<Cell, String> entry : snapshot.blocks().entrySet()) {
            if (!entry.getValue().equals(world.blockData(entry.getKey()))) {
                throw new IOException("Recovery Room 04 rollback read-back failed at " + entry.getKey());
            }
        }
    }

    private void persistSnapshot(Snapshot snapshot) throws IOException {
        String body = snapshotText(
                snapshot.releaseId(), snapshot.worldBinding(), snapshot.origin(),
                snapshot.manifestSha256(), snapshot.blocks(), snapshot.snapshotSha256());
        atomicWrite(snapshotPath, body);
        Snapshot readBack = readSnapshot();
        if (!snapshot.snapshotSha256().equals(readBack.snapshotSha256())) {
            throw new IOException("Recovery Room 04 staged snapshot read-back mismatch");
        }
    }

    private Snapshot readSnapshot() throws IOException {
        List<String> lines = Files.readAllLines(snapshotPath, StandardCharsets.UTF_8);
        if (lines.size() != 7 + manifest.cells().size() || !SNAPSHOT_SCHEMA.equals(lines.get(0))) {
            throw new IOException("invalid Recovery Room 04 rollback snapshot schema");
        }
        String release = value(lines.get(1), "release=");
        String world = decode(value(lines.get(2), "world="));
        Origin origin = origin(value(lines.get(3), "origin="));
        String manifestHash = value(lines.get(4), "manifest-sha256=");
        String snapshotHash = value(lines.get(5), "snapshot-sha256=");
        if (!manifestHash.matches("[0-9a-f]{64}") || !snapshotHash.matches("[0-9a-f]{64}")) {
            throw new IOException("invalid Recovery Room 04 rollback hash");
        }
        LinkedHashMap<Cell, String> blocks = new LinkedHashMap<>();
        for (int index = 6; index < lines.size() - 1; index++) {
            String[] fields = lines.get(index).split("\\t", -1);
            if (fields.length != 4) throw new IOException("invalid Recovery Room 04 rollback cell");
            Cell cell;
            try {
                cell = new Cell(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            } catch (NumberFormatException failure) {
                throw new IOException("invalid Recovery Room 04 rollback coordinate", failure);
            }
            if (!manifest.cells().containsKey(cell) || blocks.put(cell, decode(fields[3])) != null) {
                throw new IOException("invalid or duplicate Recovery Room 04 rollback cell");
            }
        }
        if (!"end".equals(lines.get(lines.size() - 1)) || blocks.size() != manifest.cells().size()) {
            throw new IOException("incomplete Recovery Room 04 rollback snapshot");
        }
        String unsigned = snapshotText(release, world, origin, manifestHash, blocks, null);
        if (!snapshotHash.equals(sha256(unsigned.getBytes(StandardCharsets.UTF_8)))) {
            throw new IOException("Recovery Room 04 rollback snapshot hash mismatch");
        }
        return new Snapshot(release, world, origin, manifestHash, Map.copyOf(blocks), snapshotHash);
    }

    private void persistReceipt(Receipt receipt) throws IOException {
        String body = RECEIPT_SCHEMA + "\n"
                + "release=" + receipt.releaseId() + "\n"
                + "world=" + encode(receipt.worldBinding()) + "\n"
                + "origin=" + origin(receipt.origin()) + "\n"
                + "manifest-sha256=" + receipt.manifestSha256() + "\n"
                + "snapshot-sha256=" + receipt.snapshotSha256() + "\n";
        atomicWrite(receiptPath, body);
        Receipt readBack = readReceipt();
        if (!receipt.equals(readBack)) throw new IOException("Recovery Room 04 install receipt read-back mismatch");
    }

    private Receipt readReceipt() throws IOException {
        List<String> lines = Files.readAllLines(receiptPath, StandardCharsets.UTF_8);
        if (lines.size() != 6 || !RECEIPT_SCHEMA.equals(lines.get(0))) {
            throw new IOException("invalid Recovery Room 04 install receipt schema");
        }
        Receipt receipt = new Receipt(
                value(lines.get(1), "release="),
                decode(value(lines.get(2), "world=")),
                origin(value(lines.get(3), "origin=")),
                value(lines.get(4), "manifest-sha256="),
                value(lines.get(5), "snapshot-sha256="));
        if (!receipt.manifestSha256().matches("[0-9a-f]{64}")
                || !receipt.snapshotSha256().matches("[0-9a-f]{64}")) {
            throw new IOException("invalid Recovery Room 04 install receipt hash");
        }
        return receipt;
    }

    private void verifyReceiptBinding(Receipt receipt, String release, Origin origin, String world) throws IOException {
        if (!release.equals(receipt.releaseId()) || !world.equals(receipt.worldBinding())
                || !origin.equals(receipt.origin())
                || !manifest.manifestSha256().equals(receipt.manifestSha256())) {
            throw new IOException("Recovery Room 04 install receipt binding mismatch");
        }
    }

    private void verifySnapshotBinding(Snapshot snapshot, String release, Origin origin, String world)
            throws IOException {
        if (!release.equals(snapshot.releaseId()) || !world.equals(snapshot.worldBinding())
                || !origin.equals(snapshot.origin())
                || !manifest.manifestSha256().equals(snapshot.manifestSha256())) {
            throw new IOException("Recovery Room 04 rollback snapshot binding mismatch");
        }
    }

    private static void requireBinding(String releaseId, Origin origin, WorldPort world) {
        if (releaseId == null || releaseId.isBlank()) throw new IllegalArgumentException("releaseId is required");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(world, "world");
        if (world.binding() == null || world.binding().isBlank()) {
            throw new IllegalArgumentException("world binding is required");
        }
    }

    private static String snapshotText(
            String release,
            String world,
            Origin origin,
            String manifestHash,
            Map<Cell, String> blocks,
            String snapshotHash) {
        StringBuilder body = new StringBuilder(blocks.size() * 40);
        body.append(SNAPSHOT_SCHEMA).append('\n')
                .append("release=").append(release).append('\n')
                .append("world=").append(encode(world)).append('\n')
                .append("origin=").append(origin(origin)).append('\n')
                .append("manifest-sha256=").append(manifestHash).append('\n');
        if (snapshotHash != null) body.append("snapshot-sha256=").append(snapshotHash).append('\n');
        blocks.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> body
                .append(entry.getKey().x()).append('\t')
                .append(entry.getKey().y()).append('\t')
                .append(entry.getKey().z()).append('\t')
                .append(encode(entry.getValue())).append('\n'));
        body.append("end\n");
        return body.toString();
    }

    private static void atomicWrite(Path destination, String body) throws IOException {
        Path absolute = destination.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) throw new IOException("Recovery Room 04 file has no parent");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, destination.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)));
                channel.force(true);
            }
            try {
                Files.move(temporary, absolute,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException failure) {
                throw new IOException("filesystem does not support atomic Recovery Room 04 state", failure);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix) || line.length() == prefix.length()) {
            throw new IOException("invalid Recovery Room 04 state field");
        }
        return line.substring(prefix.length());
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid Recovery Room 04 state encoding", failure);
        }
    }

    private static String origin(Origin origin) {
        return origin.x() + "," + origin.y() + "," + origin.z();
    }

    private static Origin origin(String encoded) throws IOException {
        String[] fields = encoded.split(",", -1);
        if (fields.length != 3) throw new IOException("invalid Recovery Room 04 origin");
        try {
            return new Origin(
                    Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
        } catch (NumberFormatException failure) {
            throw new IOException("invalid Recovery Room 04 origin", failure);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
