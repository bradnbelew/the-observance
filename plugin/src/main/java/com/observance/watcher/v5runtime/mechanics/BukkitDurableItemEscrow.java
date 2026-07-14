package com.observance.watcher.v5runtime.mechanics;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * File-backed protected item return queue. Escrow-tag scanning makes a crash after inventory
 * delivery idempotent; no recovery path drops an item into the world.
 */
public final class BukkitDurableItemEscrow {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final Plugin plugin;
    private final Path directory;
    private final NamespacedKey escrowKey;

    public BukkitDurableItemEscrow(Plugin plugin, Path directory) throws IOException {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
        this.escrowKey = new NamespacedKey(plugin, "v5_escrow_id");
    }

    public UUID deposit(UUID playerId, ItemStack item, String reason) throws IOException {
        return deposit(playerId, item, reason, Optional.empty());
    }

    public UUID depositFromSlot(
            UUID playerId, ItemStack item, String reason, SourceSlot source) throws IOException {
        return deposit(playerId, item, reason, Optional.of(source));
    }

    public int deliverPending(org.bukkit.entity.Player player) throws IOException {
        int delivered = 0;
        for (Path path : files()) {
            Stored stored = read(path);
            if (!player.getUniqueId().toString().equals(stored.player_id)) {
                continue;
            }
            UUID escrowId = UUID.fromString(stored.escrow_id);
            if (inventoryContains(player.getInventory(), escrowId)) {
                Files.deleteIfExists(path);
                delivered++;
                continue;
            }
            if (stored.source != null && sourceStillContains(stored)) {
                removeSource(stored);
                stored.status = "PENDING_DELIVERY";
                writeAtomic(path, stored);
            }
            ItemStack item = deserialize(stored.item_base64);
            tag(item, escrowId);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (leftovers.isEmpty()) {
                Files.deleteIfExists(path);
                delivered++;
            } else {
                ItemStack remaining = leftovers.values().iterator().next();
                tag(remaining, escrowId);
                stored.item_base64 = Base64.getEncoder().encodeToString(remaining.serializeAsBytes());
                stored.status = "PENDING_DELIVERY";
                writeAtomic(path, stored);
            }
        }
        return delivered;
    }

    public boolean containsReceipt(UUID playerId, String nodeId, String receiptId) throws IOException {
        for (Path path : files()) {
            Stored stored = read(path);
            if (!playerId.toString().equals(stored.player_id)) {
                continue;
            }
            MechanicItem item = BukkitWorldStateEvaluator.item(deserialize(stored.item_base64));
            if (nodeId.equals(item.pdc().get("v5_receipt_node"))
                    && receiptId.equals(item.pdc().get("v5_receipt_id"))) {
                return true;
            }
        }
        return false;
    }

    public int pendingCount() throws IOException {
        return files().size();
    }

    private UUID deposit(
            UUID playerId, ItemStack source, String reason, Optional<SourceSlot> sourceSlot)
            throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(source, "source");
        if (source.getType().isAir() || source.getAmount() < 1) {
            throw new IllegalArgumentException("cannot escrow an empty item");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason cannot be blank");
        }
        UUID escrowId = UUID.randomUUID();
        ItemStack copy = source.clone();
        tag(copy, escrowId);
        Stored stored = new Stored();
        stored.schema_version = 1;
        stored.escrow_id = escrowId.toString();
        stored.player_id = playerId.toString();
        stored.reason = reason;
        stored.status = sourceSlot.isPresent() ? "HELD_SOURCE" : "PENDING_DELIVERY";
        stored.item_base64 = Base64.getEncoder().encodeToString(copy.serializeAsBytes());
        stored.fingerprint_sha256 = fingerprint(source);
        stored.source = sourceSlot.map(SourceSlot::toStored).orElse(null);
        writeAtomic(path(escrowId), stored);
        return escrowId;
    }

    private boolean sourceStillContains(Stored stored) {
        SourceStored source = stored.source;
        World world = Bukkit.getWorld(UUID.fromString(source.world_id));
        if (world == null) {
            return false;
        }
        Block block = world.getBlockAt(source.x, source.y, source.z);
        if (!(block.getState() instanceof InventoryHolder holder)) {
            return false;
        }
        ItemStack current = holder.getInventory().getItem(source.slot);
        return current != null && stored.fingerprint_sha256.equals(fingerprint(current));
    }

    private void removeSource(Stored stored) throws IOException {
        SourceStored source = stored.source;
        World world = Bukkit.getWorld(UUID.fromString(source.world_id));
        if (world == null) {
            throw new IOException("source world unavailable for escrow " + stored.escrow_id);
        }
        Block block = world.getBlockAt(source.x, source.y, source.z);
        if (!(block.getState() instanceof InventoryHolder holder)) {
            throw new IOException("source inventory unavailable for escrow " + stored.escrow_id);
        }
        ItemStack current = holder.getInventory().getItem(source.slot);
        if (current != null && stored.fingerprint_sha256.equals(fingerprint(current))) {
            holder.getInventory().setItem(source.slot, null);
        }
    }

    private boolean inventoryContains(Inventory inventory, UUID escrowId) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            String value = item.getItemMeta().getPersistentDataContainer()
                    .get(escrowKey, PersistentDataType.STRING);
            if (escrowId.toString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void tag(ItemStack item, UUID escrowId) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                escrowKey, PersistentDataType.STRING, escrowId.toString());
        item.setItemMeta(meta);
    }

    private List<Path> files() throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
    }

    private Path path(UUID escrowId) {
        return directory.resolve(escrowId + ".json");
    }

    private static Stored read(Path path) throws IOException {
        Stored stored = GSON.fromJson(Files.readString(path), Stored.class);
        if (stored == null || stored.schema_version != 1) {
            throw new IOException("invalid Bukkit item escrow " + path);
        }
        return stored;
    }

    private static void writeAtomic(Path destination, Stored stored) throws IOException {
        byte[] bytes = (GSON.toJson(stored) + System.lineSeparator())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp-" + UUID.randomUUID());
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
                throw new IOException("item escrow filesystem lacks atomic replace", exception);
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

    public record SourceSlot(UUID worldId, int x, int y, int z, int slot) {
        public SourceSlot {
            Objects.requireNonNull(worldId, "worldId");
            if (slot < 0) {
                throw new IllegalArgumentException("source slot cannot be negative");
            }
        }

        private SourceStored toStored() {
            SourceStored value = new SourceStored();
            value.world_id = worldId.toString();
            value.x = x;
            value.y = y;
            value.z = z;
            value.slot = slot;
            return value;
        }
    }

    private static final class Stored {
        private int schema_version;
        private String escrow_id;
        private String player_id;
        private String reason;
        private String status;
        private String item_base64;
        private String fingerprint_sha256;
        private SourceStored source;
    }

    private static final class SourceStored {
        private String world_id;
        private int x;
        private int y;
        private int z;
        private int slot;
    }
}
