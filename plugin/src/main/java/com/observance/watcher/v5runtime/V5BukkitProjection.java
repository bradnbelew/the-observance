package com.observance.watcher.v5runtime;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.structure.CanonicalArtifactRegistry;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.mechanics.BukkitWorldStateEvaluator;
import com.observance.watcher.v5runtime.mechanics.MechanicItem;
import com.observance.watcher.v5runtime.mechanics.MechanicObservation;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Main-thread world projection and exact artifact custody for the 41 mechanics nodes. */
public final class V5BukkitProjection implements
        MechanicPorts.WorldMutation, MechanicPorts.ArtifactDelivery {
    private final ObservancePlugin plugin;
    private final V5ProgressStore progress;
    private final BukkitFixtureIndex fixtures;
    private final BukkitContainerCustody custody;
    private final NamespacedKey artifactId;
    private final NamespacedKey artifactAlias;
    private final NamespacedKey artifactInstance;
    private final NamespacedKey ritualInstance;
    private final NamespacedKey locked;

    public V5BukkitProjection(
            ObservancePlugin plugin,
            V5ProgressStore progress,
            BukkitFixtureIndex fixtures,
            BukkitContainerCustody custody) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.custody = Objects.requireNonNull(custody, "custody");
        artifactId = new NamespacedKey(plugin, MechanicItem.ARTIFACT_ID);
        artifactAlias = new NamespacedKey(plugin, CanonicalArtifactRegistry.PDC_ARTIFACT_ID);
        artifactInstance = new NamespacedKey(plugin, MechanicItem.ARTIFACT_INSTANCE);
        ritualInstance = new NamespacedKey(plugin, "v5_item_instance");
        locked = new NamespacedKey(plugin, "v5_mechanic_locked");
    }

    @Override
    public void applyAfterLocalCommit(
            PhysicalPredicateAuthority.Node node,
            UUID actor,
            MechanicObservation observation) {
        latch(node.nodeId());
        plugin.v5Runtime().projectLocalState();
    }

    @Override
    public void recoverCommitted(PhysicalPredicateAuthority.Node node) {
        latch(node.nodeId());
        plugin.v5Runtime().projectLocalState();
    }

    private void latch(String nodeId) {
        for (BukkitFixtureIndex.Binding binding : fixtures.bindingsForNode(nodeId)) {
            if (binding.kind() != BukkitFixtureIndex.BindingKind.BLOCK) continue;
            World world = Bukkit.getWorld(binding.worldId());
            if (world == null || !world.isChunkLoaded(binding.x() >> 4, binding.z() >> 4)) continue;
            var block = world.getBlockAt(binding.x(), binding.y(), binding.z());
            if (block.getState() instanceof TileState tile) {
                tile.getPersistentDataContainer().set(locked, PersistentDataType.BYTE, (byte) 1);
                tile.update(true, false);
            }
        }
    }

    @Override
    public MechanicItem template(String requestedId, UUID instanceId) {
        ItemStack item = CanonicalArtifactRegistry.create(requestedId, null);
        if (item == null) {
            throw new IllegalArgumentException("no canonical V5 artifact " + requestedId);
        }
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(artifactId, PersistentDataType.STRING, requestedId);
        meta.getPersistentDataContainer().set(artifactAlias, PersistentDataType.STRING, requestedId);
        meta.getPersistentDataContainer().set(
                artifactInstance, PersistentDataType.STRING, instanceId.toString());
        // ProtocolBridge's same-stack ritual adapter uses this compatibility identity too.
        meta.getPersistentDataContainer().set(
                ritualInstance, PersistentDataType.STRING, instanceId.toString());
        item.setItemMeta(meta);
        return BukkitWorldStateEvaluator.item(item);
    }

    public ItemStack templateStack(String requestedId, UUID instanceId) {
        ItemStack item = CanonicalArtifactRegistry.create(requestedId, null);
        if (item == null) throw new IllegalArgumentException("no canonical V5 artifact " + requestedId);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(artifactId, PersistentDataType.STRING, requestedId);
        meta.getPersistentDataContainer().set(artifactAlias, PersistentDataType.STRING, requestedId);
        meta.getPersistentDataContainer().set(
                artifactInstance, PersistentDataType.STRING, instanceId.toString());
        meta.getPersistentDataContainer().set(
                ritualInstance, PersistentDataType.STRING, instanceId.toString());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Set<UUID> scanInstances(String requestedId) {
        LinkedHashSet<UUID> found = new LinkedHashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            scan(player.getInventory(), requestedId, found);
            scan(player.getEnderChest(), requestedId, found);
        }
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof InventoryHolder holder) {
                        scan(holder.getInventory(), requestedId, found);
                    }
                }
            }
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                scan(entity.getItemStack(), requestedId, found);
            }
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                scan(frame.getItem(), requestedId, found);
            }
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                scan(display.getItemStack(), requestedId, found);
            }
        }
        for (EscrowEntry entry : progress.snapshot().escrow().values()) {
            if (!requestedId.equals(entry.artifactId())) continue;
            String raw = entry.metadata().getOrDefault(
                    "instance_uuid", entry.metadata().get("instance"));
            if (raw != null && !raw.equals("none")) {
                try {
                    found.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                    found.add(synthetic(entry.escrowId()));
                }
            } else {
                found.add(synthetic(entry.escrowId()));
            }
        }
        return Set.copyOf(found);
    }

    @Override
    public boolean deliverOrKeepEscrow(UUID actor, EscrowEntry pending) throws Exception {
        UUID instance = UUID.fromString(Objects.requireNonNull(
                pending.metadata().get("instance_uuid"), "reward instance UUID"));
        Player player = Bukkit.getPlayer(actor);
        if (player == null || !player.isOnline()) return false;
        if (containsInstance(player.getInventory(), pending.artifactId(), instance)) return true;
        if (containsInstance(player.getEnderChest(), pending.artifactId(), instance)) return true;
        if (!custody.hasJournal(pending.escrowId())) {
            custody.prepareGenerated(
                    pending.escrowId(), actor, templateStack(pending.artifactId(), instance));
        }
        return custody.deliver(pending.escrowId(), player);
    }

    private void scan(Inventory inventory, String requestedId, Set<UUID> found) {
        if (inventory == null) return;
        for (ItemStack item : inventory.getContents()) scan(item, requestedId, found);
    }

    private void scan(ItemStack item, String requestedId, Set<UUID> found) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(artifactId, PersistentDataType.STRING);
        String alias = pdc.get(artifactAlias, PersistentDataType.STRING);
        if (!requestedId.equals(id) && !requestedId.equals(alias)) return;
        String raw = pdc.get(artifactInstance, PersistentDataType.STRING);
        if (raw == null) raw = pdc.get(ritualInstance, PersistentDataType.STRING);
        try {
            found.add(raw == null ? synthetic(java.util.Base64.getEncoder().encodeToString(
                    item.serializeAsBytes())) : UUID.fromString(raw));
        } catch (IllegalArgumentException malformed) {
            found.add(synthetic("malformed:" + raw));
        }
    }

    private boolean containsInstance(Inventory inventory, String id, UUID instance) {
        LinkedHashSet<UUID> found = new LinkedHashSet<>();
        scan(inventory, id, found);
        return found.contains(instance);
    }

    private static UUID synthetic(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}
