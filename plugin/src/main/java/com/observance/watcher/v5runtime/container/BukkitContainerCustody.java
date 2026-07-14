package com.observance.watcher.v5runtime.container;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Deterministic file-backed custody journal. The journal is forced before a source is removed;
 * delivery is idempotent through a stable progress-escrow PDC marker in either of the intended
 * player's durable inventories. Player data is synchronously saved before the journal is deleted,
 * closing the crash window between inventory delivery and persistence.
 */
public final class BukkitContainerCustody {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final Path directory;
    private final NamespacedKey progressEscrowKey;

    public BukkitContainerCustody(Plugin plugin, Path directory) throws IOException {
        Objects.requireNonNull(plugin, "plugin");
        this.directory = Objects.requireNonNull(directory, "directory")
                .toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
        this.progressEscrowKey = new NamespacedKey(plugin, "v5_progress_escrow_id");
    }

    public void prepareFromSlot(
            String progressEscrowId,
            UUID playerId,
            ItemStack source,
            SourceSlot sourceSlot) throws IOException {
        requireText(progressEscrowId, "progressEscrowId");
        Objects.requireNonNull(playerId, "playerId");
        requireItem(source);
        Objects.requireNonNull(sourceSlot, "sourceSlot");
        Stored candidate = stored(progressEscrowId, playerId, source);
        candidate.source = sourceSlot.toStored();
        candidate.source_fingerprint_sha256 = fingerprint(source);
        prepare(candidate);
    }

    public void prepareGenerated(
            String progressEscrowId, UUID playerId, ItemStack generated) throws IOException {
        requireText(progressEscrowId, "progressEscrowId");
        Objects.requireNonNull(playerId, "playerId");
        requireItem(generated);
        prepare(stored(progressEscrowId, playerId, generated));
    }

    /** Returns true only when the stable tagged stack is present in the intended inventory. */
    public boolean deliver(String progressEscrowId, org.bukkit.entity.Player player) throws IOException {
        Path path = path(progressEscrowId);
        if (!Files.exists(path)) {
            return playerContains(player, progressEscrowId);
        }
        Stored stored = read(path);
        if (!player.getUniqueId().toString().equals(stored.player_id)) {
            throw new IOException("custody actor mismatch for " + progressEscrowId);
        }
        if (playerContains(player, progressEscrowId)) {
            removeSourceIfExact(stored);
            persistThenDelete(path, player);
            return true;
        }
        if (stored.source != null) {
            SourceState state = sourceState(stored);
            if (state == SourceState.MISMATCH) {
                throw new IOException("custody source changed for " + progressEscrowId);
            }
            if (state == SourceState.EXACT) {
                removeSourceIfExact(stored);
            }
        }
        ItemStack item = deserialize(stored.item_base64);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (leftovers.isEmpty()) {
            persistThenDelete(path, player);
            return true;
        }
        ItemStack remaining = leftovers.values().iterator().next();
        tag(remaining, progressEscrowId);
        stored.item_base64 = Base64.getEncoder().encodeToString(remaining.serializeAsBytes());
        clearSource(stored);
        writeAtomic(path, stored);
        return false;
    }

    public int deliverPending(org.bukkit.entity.Player player) throws IOException {
        return deliverPendingIds(player).size();
    }

    public Set<String> deliverPendingIds(org.bukkit.entity.Player player) throws IOException {
        Set<String> delivered = new LinkedHashSet<>();
        for (Path path : files()) {
            Stored stored = read(path);
            if (player.getUniqueId().toString().equals(stored.player_id)
                    && deliver(stored.progress_escrow_id, player)) {
                delivered.add(stored.progress_escrow_id);
            }
        }
        return Set.copyOf(delivered);
    }

    public boolean hasJournal(String progressEscrowId) {
        return Files.exists(path(progressEscrowId));
    }

    public int pendingCount() throws IOException {
        return files().size();
    }

    public boolean inventoryContains(Inventory inventory, String progressEscrowId) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            String value = item.getItemMeta().getPersistentDataContainer()
                    .get(progressEscrowKey, PersistentDataType.STRING);
            if (progressEscrowId.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean playerContains(
            org.bukkit.entity.Player player, String progressEscrowId) {
        return inventoryContains(player.getInventory(), progressEscrowId)
                || inventoryContains(player.getEnderChest(), progressEscrowId);
    }

    private static void clearSource(Stored stored) {
        stored.source = null;
        stored.source_fingerprint_sha256 = null;
    }

    private static void persistThenDelete(
            Path journal, org.bukkit.entity.Player player) throws IOException {
        try {
            player.saveData();
        } catch (RuntimeException exception) {
            throw new IOException("player custody could not be persisted", exception);
        }
        Files.deleteIfExists(journal);
    }

    private Stored stored(String progressEscrowId, UUID playerId, ItemStack source) {
        ItemStack copy = source.clone();
        tag(copy, progressEscrowId);
        Stored stored = new Stored();
        stored.schema_version = 1;
        stored.progress_escrow_id = progressEscrowId;
        stored.player_id = playerId.toString();
        stored.item_base64 = Base64.getEncoder().encodeToString(copy.serializeAsBytes());
        stored.item_fingerprint_sha256 = fingerprint(copy);
        return stored;
    }

    private void prepare(Stored candidate) throws IOException {
        Path path = path(candidate.progress_escrow_id);
        if (Files.exists(path)) {
            Stored existing = read(path);
            if (!sameIdentity(existing, candidate)) {
                throw new IOException("custody journal collision for " + candidate.progress_escrow_id);
            }
            return;
        }
        writeAtomic(path, candidate);
    }

    private static boolean sameIdentity(Stored left, Stored right) {
        return left.schema_version == right.schema_version
                && Objects.equals(left.progress_escrow_id, right.progress_escrow_id)
                && Objects.equals(left.player_id, right.player_id)
                && Objects.equals(left.item_fingerprint_sha256, right.item_fingerprint_sha256)
                && Objects.equals(left.source_fingerprint_sha256, right.source_fingerprint_sha256)
                && Objects.equals(left.source, right.source);
    }

    private SourceState sourceState(Stored stored) {
        World world = Bukkit.getWorld(UUID.fromString(stored.source.world_id));
        if (world == null) {
            return SourceState.MISMATCH;
        }
        Block block = world.getBlockAt(stored.source.x, stored.source.y, stored.source.z);
        if (!(block.getState() instanceof InventoryHolder holder)) {
            return SourceState.MISMATCH;
        }
        ItemStack current = holder.getInventory().getItem(stored.source.slot);
        if (current == null || current.getType().isAir()) {
            return SourceState.ABSENT;
        }
        return stored.source_fingerprint_sha256.equals(fingerprint(current))
                ? SourceState.EXACT : SourceState.MISMATCH;
    }

    private void removeSourceIfExact(Stored stored) throws IOException {
        if (stored.source == null) {
            return;
        }
        World world = Bukkit.getWorld(UUID.fromString(stored.source.world_id));
        if (world == null) {
            throw new IOException("custody source world unavailable");
        }
        Block block = world.getBlockAt(stored.source.x, stored.source.y, stored.source.z);
        if (!(block.getState() instanceof InventoryHolder holder)) {
            throw new IOException("custody source inventory unavailable");
        }
        ItemStack current = holder.getInventory().getItem(stored.source.slot);
        if (current != null && !current.getType().isAir()) {
            if (!stored.source_fingerprint_sha256.equals(fingerprint(current))) {
                throw new IOException("custody source identity changed");
            }
            holder.getInventory().setItem(stored.source.slot, null);
        }
    }

    private void tag(ItemStack item, String progressEscrowId) {
        var meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("custody item must support item metadata");
        }
        meta.getPersistentDataContainer().set(
                progressEscrowKey, PersistentDataType.STRING, progressEscrowId);
        item.setItemMeta(meta);
    }

    private List<Path> files() throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
    }

    private Path path(String progressEscrowId) {
        return directory.resolve(hash(progressEscrowId) + ".json");
    }

    private static Stored read(Path path) throws IOException {
        Stored stored = GSON.fromJson(Files.readString(path), Stored.class);
        if (stored == null || stored.schema_version != 1 || stored.progress_escrow_id == null
                || stored.player_id == null || stored.item_base64 == null
                || stored.item_fingerprint_sha256 == null) {
            throw new IOException("invalid container custody journal " + path);
        }
        return stored;
    }

    private static void writeAtomic(Path destination, Stored stored) throws IOException {
        byte[] bytes = (GSON.toJson(stored) + System.lineSeparator())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path temporary = destination.resolveSibling(
                destination.getFileName() + ".tmp-" + UUID.randomUUID());
        boolean moved = false;
        try {
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("container custody filesystem lacks atomic replace", exception);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static ItemStack deserialize(String encoded) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    private static String fingerprint(ItemStack item) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(item.serializeAsBytes()));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }

    private static void requireItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        if (item.getType().isAir() || item.getAmount() < 1) {
            throw new IllegalArgumentException("custody item cannot be empty");
        }
    }

    private enum SourceState { EXACT, ABSENT, MISMATCH }

    public record SourceSlot(UUID worldId, int x, int y, int z, int slot) {
        public SourceSlot {
            Objects.requireNonNull(worldId, "worldId");
            if (slot < 0) {
                throw new IllegalArgumentException("source slot cannot be negative");
            }
        }

        private SourceStored toStored() {
            SourceStored stored = new SourceStored();
            stored.world_id = worldId.toString();
            stored.x = x;
            stored.y = y;
            stored.z = z;
            stored.slot = slot;
            return stored;
        }
    }

    private static final class Stored {
        private int schema_version;
        private String progress_escrow_id;
        private String player_id;
        private String item_base64;
        private String item_fingerprint_sha256;
        private String source_fingerprint_sha256;
        private SourceStored source;
    }

    private static final class SourceStored {
        private String world_id;
        private int x;
        private int y;
        private int z;
        private int slot;

        @Override
        public boolean equals(Object other) {
            return other instanceof SourceStored value
                    && x == value.x && y == value.y && z == value.z && slot == value.slot
                    && Objects.equals(world_id, value.world_id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world_id, x, y, z, slot);
        }
    }
}
