package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.MaterialChoice;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorManifest.Cell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Empty-target, receipt-bound installer with exact dynamic-cell auditing for M07. */
public final class WitnessAnchorInstaller {
    private static final String SCHEMA = "morrow-witness-anchor-install-v1";
    private static final Set<String> ALLOWED_DYNAMIC = java.util.Arrays.stream(MaterialChoice.values())
            .map(MaterialChoice::minecraftKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private final WitnessAnchorManifest manifest;
    private final Path receiptPath;

    public WitnessAnchorInstaller(WitnessAnchorManifest manifest, Path receiptPath) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.receiptPath = Objects.requireNonNull(receiptPath, "receiptPath");
    }

    public synchronized Result install(String release, Origin origin, WorldPort world) throws IOException {
        requireBinding(release, origin, world);
        if (Files.exists(receiptPath)) {
            verify(readReceipt(), release, origin, world.binding());
            audit(world, true);
            return new Result(Status.ALREADY_PRESENT, manifest.manifestSha256(), manifest.cells().size());
        }
        boolean recovered = false;
        Cell firstForeign = null;
        int foreign = 0;
        for (Map.Entry<Cell, String> entry : manifest.cells().entrySet()) {
            if (world.isAir(entry.getKey())) continue;
            if (world.matches(entry.getKey(), entry.getValue())) recovered = true;
            else {
                foreign++;
                if (firstForeign == null) firstForeign = entry.getKey();
            }
        }
        if (foreign > 0) throw new IOException("M07 target contains " + foreign + " foreign cell(s); first=" + firstForeign);
        try {
            for (Map.Entry<Cell, String> entry : manifest.cells().entrySet()) {
                if (!"minecraft:air".equals(entry.getValue())) world.setBlockData(entry.getKey(), entry.getValue());
            }
            audit(world, false);
            persist(new Receipt(release, world.binding(), origin, manifest.manifestSha256()));
        } catch (Throwable failure) {
            if (!recovered) {
                try {
                    for (Cell cell : manifest.cells().keySet()) world.setBlockData(cell, "minecraft:air");
                } catch (Throwable rollback) {
                    failure.addSuppressed(rollback);
                }
            }
            if (failure instanceof IOException io) throw io;
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IOException("M07 installation failed", failure);
        }
        return new Result(recovered ? Status.RECOVERED_AND_BUILT : Status.BUILT,
                manifest.manifestSha256(), manifest.cells().size());
    }

    public synchronized void audit(WorldPort world, boolean allowDynamic) throws IOException {
        for (Map.Entry<Cell, String> entry : manifest.cells().entrySet()) {
            Cell cell = entry.getKey();
            String actual = world.blockData(cell);
            if (allowDynamic && dynamic(cell, actual)) continue;
            if (!world.matches(cell, entry.getValue())) {
                throw new IOException("M07 read-back mismatch at " + cell
                        + " expected=" + entry.getValue() + " actual=" + actual);
            }
        }
    }

    private boolean dynamic(Cell cell, String actual) {
        if (manifest.commitLamp().equals(cell)) {
            return actual.startsWith("minecraft:copper_bulb[")
                    && (actual.contains("lit=true") || actual.contains("lit=false"));
        }
        boolean buildCell = manifest.originalCells().containsValue(cell)
                || manifest.reconstructionCells().containsValue(cell);
        return buildCell && ("minecraft:air".equals(actual) || ALLOWED_DYNAMIC.stream().anyMatch(
                allowed -> actual.equals(allowed) || actual.startsWith(allowed + "[")));
    }

    private void persist(Receipt receipt) throws IOException {
        String text = SCHEMA + "\nrelease=" + receipt.release() + "\nworld=" + encode(receipt.world())
                + "\norigin=" + receipt.origin().x() + "," + receipt.origin().y() + ","
                + receipt.origin().z() + "\nmanifest-sha256=" + receipt.manifest() + "\n";
        Path parent = receiptPath.toAbsolutePath().getParent();
        if (parent == null) throw new IOException("M07 receipt has no parent");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "morrow-m07-", ".tmp");
        Files.writeString(temporary, text, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, receiptPath.toAbsolutePath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.deleteIfExists(temporary);
            throw new IOException("filesystem does not support atomic M07 receipts", unsupported);
        }
        if (!receipt.equals(readReceipt())) throw new IOException("M07 receipt read-back mismatch");
    }
    private Receipt readReceipt() throws IOException {
        List<String> lines = Files.readAllLines(receiptPath, StandardCharsets.UTF_8);
        if (lines.size() != 5 || !SCHEMA.equals(lines.get(0))) throw new IOException("invalid M07 receipt");
        String[] origin = value(lines.get(3), "origin=").split(",", -1);
        if (origin.length != 3) throw new IOException("invalid M07 receipt origin");
        try {
            return new Receipt(value(lines.get(1), "release="), decode(value(lines.get(2), "world=")),
                    new Origin(Integer.parseInt(origin[0]), Integer.parseInt(origin[1]), Integer.parseInt(origin[2])),
                    value(lines.get(4), "manifest-sha256="));
        } catch (IllegalArgumentException failure) {
            throw new IOException("invalid M07 receipt field", failure);
        }
    }
    private void verify(Receipt receipt, String release, Origin origin, String world) throws IOException {
        if (!receipt.release().equals(release) || !receipt.origin().equals(origin)
                || !receipt.world().equals(world) || !receipt.manifest().equals(manifest.manifestSha256())) {
            throw new IOException("M07 receipt binding mismatch");
        }
    }
    private static void requireBinding(String release, Origin origin, WorldPort world) {
        if (release == null || release.isBlank()) throw new IllegalArgumentException("M07 release is required");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(world, "world");
        if (world.binding() == null || world.binding().isBlank()) throw new IllegalArgumentException("M07 world binding is required");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix) || line.length() == prefix.length()) throw new IOException("invalid M07 receipt field");
        return line.substring(prefix.length());
    }
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public interface WorldPort {
        String binding();
        String blockData(Cell relative);
        default boolean matches(Cell relative, String expected) { return expected.equals(blockData(relative)); }
        boolean isAir(Cell relative);
        void setBlockData(Cell relative, String blockData);
    }
    public enum Status { BUILT, RECOVERED_AND_BUILT, ALREADY_PRESENT }
    public record Origin(int x, int y, int z) { }
    public record Result(Status status, String manifestSha256, int blockCount) { }
    private record Receipt(String release, String world, Origin origin, String manifest) { }
}
