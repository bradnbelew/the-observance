package com.observance.watcher.v5runtime.container;

import com.observance.watcher.structure.CanonicalArtifactRegistry;
import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody.SourceSlot;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract.NodeRule;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.ItemDisposition;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.Mode;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.BindingKind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Exact-coordinate Bukkit projection and durable custody adapter for family C. */
public final class BukkitContainerWorld implements ContainerRuntimePorts.World {
    private final Plugin plugin;
    private final BukkitFixtureIndex fixtures;
    private final ContainerAuthorityContract contract;
    private final BukkitContainerCustody custody;
    private final NamespacedKey artifactIdKey;
    private final NamespacedKey artifactAliasKey;
    private final NamespacedKey artifactInstanceKey;
    private final String pdcNamespace;
    private final NamespacedKey lockedKey;
    private final NamespacedKey progressEscrowKey;

    public BukkitContainerWorld(
            Plugin plugin,
            BukkitFixtureIndex fixtures,
            ContainerAuthorityContract contract,
            BukkitContainerCustody custody) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.contract = Objects.requireNonNull(contract, "contract");
        this.custody = Objects.requireNonNull(custody, "custody");
        artifactIdKey = new NamespacedKey(plugin, ContainerItem.ARTIFACT_ID);
        artifactAliasKey = new NamespacedKey(plugin, ContainerItem.ARTIFACT_ALIAS);
        artifactInstanceKey = new NamespacedKey(plugin, ContainerItem.ARTIFACT_INSTANCE);
        pdcNamespace = artifactIdKey.getNamespace();
        lockedKey = new NamespacedKey(plugin, "v5_container_locked");
        progressEscrowKey = new NamespacedKey(plugin, "v5_progress_escrow_id");
    }

    @Override
    public ContainerObservation capture(
            NodeRule rule,
            UUID actor,
            Set<String> trueFlags,
            Set<String> heldEvidenceBits,
            boolean linked,
            boolean handoffMatch) {
        Map<String, Map<Integer, ContainerItem>> inventories = new LinkedHashMap<>();
        for (LocatedInventory located : locatedInventories(rule.nodeId())) {
            Map<Integer, ContainerItem> slots = inventories.computeIfAbsent(
                    located.component(), ignored -> new LinkedHashMap<>());
            for (int slot = 0; slot < located.inventory().getSize(); slot++) {
                ItemStack item = located.inventory().getItem(slot);
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                ensureArtifactInstance(item);
                located.inventory().setItem(slot, item);
                if (slots.put(slot, fromBukkit(item)) != null) {
                    throw new IllegalStateException("duplicate inventory slot binding for "
                            + rule.nodeId() + ':' + located.component() + ':' + slot);
                }
            }
        }
        Map<String, Integer> rotations = new LinkedHashMap<>();
        for (Binding binding : fixtures.bindingsForNode(rule.nodeId())) {
            if (binding.kind() != BindingKind.ENTITY || binding.entityId().isEmpty()) {
                continue;
            }
            Entity entity = plugin.getServer().getEntity(binding.entityId().orElseThrow());
            if (entity instanceof ItemFrame frame && fixtures.verify(binding, frame)) {
                rotations.put(binding.componentId(), frame.getRotation().ordinal());
            }
        }
        Player player = plugin.getServer().getPlayer(actor);
        int capacity = player == null ? 0 : availableSlots(player);
        return new ContainerObservation(rule.nodeId(), actor, inventories, rotations,
                globalIdentityCounts(rule), trueFlags, heldEvidenceBits, capacity,
                linked, handoffMatch);
    }

    @Override
    public Set<String> applyAfterCommit(
            NodeRule rule, UUID actor, ContainerCommitPlan plan) throws Exception {
        if (plan.latchInventories()) {
            latch(rule.nodeId());
        }
        Set<String> delivered = new LinkedHashSet<>();
        for (ItemDisposition disposition : plan.items()) {
            if (disposition.mode() == Mode.HOLD_IN_FIXTURE) {
                verifyExisting(disposition);
                continue;
            }
            if (disposition.mode() == Mode.DELIVER_EXISTING_TO_ACTOR) {
                prepareExisting(actor, disposition);
            } else {
                prepareGenerated(actor, disposition);
            }
            Player player = plugin.getServer().getPlayer(actor);
            if (player != null && custody.deliver(disposition.escrowId(), player)) {
                delivered.add(disposition.escrowId());
            }
        }
        return Set.copyOf(delivered);
    }

    @Override
    public boolean applyPortableClaim(
            NodeRule rule,
            UUID actor,
            String component,
            int slot,
            ContainerItem item,
            String progressEscrowId) throws Exception {
        LocatedInventory source = requireInventory(rule.nodeId(), component);
        ItemStack current = source.inventory().getItem(slot);
        if (current == null || !item.equals(fromBukkitWithInstance(current))) {
            throw new IOException("portable source identity changed before custody commit");
        }
        custody.prepareFromSlot(progressEscrowId, actor, current,
                new SourceSlot(source.block().getWorld().getUID(), source.block().getX(),
                        source.block().getY(), source.block().getZ(), slot));
        Player player = plugin.getServer().getPlayer(actor);
        return player != null && custody.deliver(progressEscrowId, player);
    }

    @Override
    public Set<String> recoverCommitted(NodeRule rule, ProgressSnapshot progress) throws Exception {
        if (progress.isComplete(rule.completionFlag())) {
            latch(rule.nodeId());
        }
        Set<String> delivered = new LinkedHashSet<>();
        for (EscrowEntry entry : progress.escrow().values()) {
            if (!rule.nodeId().equals(entry.metadata().get("node_id"))
                    || entry.status() == EscrowStatus.HELD
                    || entry.status() == EscrowStatus.DELIVERED
                    || entry.intendedPlayer().isEmpty()) {
                continue;
            }
            UUID actor = entry.intendedPlayer().orElseThrow();
            Player player = plugin.getServer().getPlayer(actor);
            if (player != null && custody.inventoryContains(player.getInventory(), entry.escrowId())) {
                delivered.add(entry.escrowId());
                continue;
            }
            if (!custody.hasJournal(entry.escrowId())) {
                recoverJournal(rule, entry, actor);
            }
            if (player != null && custody.deliver(entry.escrowId(), player)) {
                delivered.add(entry.escrowId());
            }
        }
        return Set.copyOf(delivered);
    }

    @Override
    public Set<String> recoverPlayer(UUID playerId) throws Exception {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            return Set.of();
        }
        Set<String> delivered = new LinkedHashSet<>(custody.deliverPendingIds(player));
        collectProgressEscrowIds(player.getInventory(), delivered);
        collectProgressEscrowIds(player.getEnderChest(), delivered);
        return Set.copyOf(delivered);
    }

    /** Adds the required UUID PDC once; existing valid UUIDs are never replaced. */
    public boolean ensureArtifactInstance(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        var meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(artifactIdKey, PersistentDataType.STRING);
        String alias = pdc.get(artifactAliasKey, PersistentDataType.STRING);
        if (id == null) {
            id = alias;
        }
        if (id == null || id.isBlank()) {
            return false;
        }
        if (alias != null && !id.equals(alias)) {
            throw new IllegalArgumentException("artifact identity aliases disagree");
        }
        String instance = pdc.get(artifactInstanceKey, PersistentDataType.STRING);
        if (instance != null) {
            UUID.fromString(instance);
            return false;
        }
        pdc.set(artifactIdKey, PersistentDataType.STRING, id);
        pdc.set(artifactAliasKey, PersistentDataType.STRING, id);
        pdc.set(artifactInstanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
        return true;
    }

    public ContainerItem fromBukkitWithInstance(ItemStack item) {
        ensureArtifactInstance(item);
        return fromBukkit(item);
    }

    public ContainerItem fromBukkit(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("cannot image an empty Bukkit item");
        }
        Map<String, String> pdc = new LinkedHashMap<>();
        if (item.hasItemMeta()) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            for (NamespacedKey key : container.getKeys()) {
                if (!pdcNamespace.equals(key.getNamespace())) {
                    continue;
                }
                String value = container.get(key, PersistentDataType.STRING);
                if (value != null) {
                    pdc.put(key.getKey(), value);
                }
            }
        }
        return new ContainerItem(item.getType().name(), item.getAmount(), pdc);
    }

    public boolean isManagedItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        try {
            ensureArtifactInstance(item);
            return contract.recognizes(fromBukkit(item));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Death-drop fallback; no protected evidence is ever left as an unowned item entity. */
    public void escrowDeathDrop(UUID playerId, ItemStack item) throws IOException {
        String existing = item.hasItemMeta()
                ? item.getItemMeta().getPersistentDataContainer()
                .get(progressEscrowKey, PersistentDataType.STRING) : null;
        String id = existing == null || existing.isBlank()
                ? "container:death:" + playerId + ':' + UUID.randomUUID() : existing;
        custody.prepareGenerated(id, playerId, item);
    }

    private void prepareExisting(UUID actor, ItemDisposition disposition) throws IOException {
        LocatedInventory source = requireInventoryForDisposition(disposition);
        ItemStack current = source.inventory().getItem(disposition.slot());
        if (current == null || !disposition.existingItem().orElseThrow()
                .equals(fromBukkitWithInstance(current))) {
            throw new IOException("committed source item changed for " + disposition.escrowId());
        }
        custody.prepareFromSlot(disposition.escrowId(), actor, current,
                new SourceSlot(source.block().getWorld().getUID(), source.block().getX(),
                        source.block().getY(), source.block().getZ(), disposition.slot()));
    }

    private void prepareGenerated(UUID actor, ItemDisposition disposition) throws IOException {
        UUID instance = disposition.generatedInstance().orElseThrow();
        Set<UUID> instances = scanArtifactInstances(disposition.identityId());
        if (!instances.isEmpty()) {
            if (instances.equals(Set.of(instance))) {
                return;
            }
            throw new IOException("refusing duplicate generated artifact " + disposition.identityId());
        }
        ItemStack item = CanonicalArtifactRegistry.create(disposition.identityId(), null);
        if (item == null || item.getType() != Material.matchMaterial(disposition.material())) {
            throw new IOException("canonical artifact factory rejected " + disposition.identityId());
        }
        setArtifactIdentity(item, disposition.identityId(), instance);
        custody.prepareGenerated(disposition.escrowId(), actor, item);
    }

    private void recoverJournal(NodeRule rule, EscrowEntry entry, UUID actor) throws IOException {
        String mode = entry.metadata().getOrDefault("mode", "");
        if (Mode.DELIVER_NEW_ARTIFACT_TO_ACTOR.name().equals(mode)) {
            String material = entry.metadata().get("material");
            String instanceText = entry.metadata().get("instance");
            if (material == null || instanceText == null || "none".equals(instanceText)) {
                throw new IOException("generated reward escrow metadata is incomplete");
            }
            ItemDisposition generated = new ItemDisposition(
                    Mode.DELIVER_NEW_ARTIFACT_TO_ACTOR, "", -1, entry.artifactId(), material,
                    java.util.Optional.empty(), java.util.Optional.of(UUID.fromString(instanceText)),
                    entry.escrowId());
            prepareGenerated(actor, generated);
            return;
        }
        String component = entry.metadata().get("component");
        if (component == null || entry.sourceSlot() < 0) {
            throw new IOException("existing item escrow metadata is incomplete");
        }
        LocatedInventory source = requireInventory(rule.nodeId(), component);
        ItemStack current = source.inventory().getItem(entry.sourceSlot());
        if (current == null || !entry.itemFingerprintSha256().equals(
                fromBukkitWithInstance(current).fingerprintSha256())) {
            throw new IOException("pending source is absent or changed for " + entry.escrowId());
        }
        custody.prepareFromSlot(entry.escrowId(), actor, current,
                new SourceSlot(source.block().getWorld().getUID(), source.block().getX(),
                        source.block().getY(), source.block().getZ(), entry.sourceSlot()));
    }

    private void verifyExisting(ItemDisposition disposition) throws IOException {
        LocatedInventory source = requireInventoryForDisposition(disposition);
        ItemStack current = source.inventory().getItem(disposition.slot());
        if (current == null || !disposition.existingItem().orElseThrow()
                .equals(fromBukkitWithInstance(current))) {
            throw new IOException("held fixture item changed for " + disposition.escrowId());
        }
    }

    private LocatedInventory requireInventoryForDisposition(ItemDisposition disposition) {
        String[] parts = disposition.escrowId().split(":", 4);
        if (parts.length < 2) {
            throw new IllegalArgumentException("invalid progress escrow id");
        }
        return requireInventory(parts[1], disposition.component());
    }

    private LocatedInventory requireInventory(String nodeId, String component) {
        List<LocatedInventory> matching = locatedInventories(nodeId).stream()
                .filter(value -> component.equals(value.component())).toList();
        if (matching.size() != 1) {
            throw new IllegalStateException("expected one exact inventory for " + nodeId + ':'
                    + component + ", found " + matching.size());
        }
        return matching.get(0);
    }

    private List<LocatedInventory> locatedInventories(String nodeId) {
        List<LocatedInventory> result = new ArrayList<>();
        Set<String> addresses = new LinkedHashSet<>();
        for (Binding binding : fixtures.bindingsForNode(nodeId)) {
            if (binding.kind() != BindingKind.BLOCK) {
                continue;
            }
            World world = Bukkit.getWorld(binding.worldId());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(binding.x(), binding.y(), binding.z());
            if (!fixtures.verify(binding, block)
                    || !(block.getState() instanceof InventoryHolder holder)) {
                continue;
            }
            String component = bindingComponent(binding, block);
            String address = binding.worldId() + ":" + binding.x() + ':' + binding.y() + ':'
                    + binding.z() + ':' + component;
            if (addresses.add(address)) {
                result.add(new LocatedInventory(component, block, holder.getInventory()));
            }
        }
        return List.copyOf(result);
    }

    public String bindingComponent(Binding binding, Block block) {
        if (!"testimony_bank".equals(binding.componentId())) {
            return binding.componentId();
        }
        String name = binding.expectedPdc().get("v5_testimony_name");
        if (name == null && block.getState() instanceof TileState tile) {
            name = tile.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "v5_testimony_name"), PersistentDataType.STRING);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("testimony housing lacks exact name PDC");
        }
        return "testimony_bank:" + name.toLowerCase(Locale.ROOT);
    }

    private Map<String, Integer> globalIdentityCounts(NodeRule rule) {
        Map<String, Integer> counts = new HashMap<>();
        Set<Inventory> inventories = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player player : Bukkit.getOnlinePlayers()) {
            inventories.add(player.getInventory());
            inventories.add(player.getEnderChest());
        }
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof InventoryHolder holder) {
                        inventories.add(holder.getInventory());
                    }
                }
            }
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                count(entity.getItemStack(), rule, counts);
            }
        }
        inventories.forEach(inventory -> {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                ensureArtifactInstance(item);
                inventory.setItem(slot, item);
                count(item, rule, counts);
            }
        });
        return Map.copyOf(counts);
    }

    private void count(ItemStack stack, NodeRule rule, Map<String, Integer> counts) {
        ContainerItem item;
        try {
            item = fromBukkitWithInstance(stack);
        } catch (IllegalArgumentException exception) {
            return;
        }
        item.artifactId().ifPresent(id -> {
            if (rule.acceptedIdentities().containsKey(id)
                    || "deep_access_plate".equals(id)) {
                counts.merge("artifact:" + id, 1, Integer::sum);
            }
        });
        item.evidenceId().ifPresent(id -> {
            if (rule.acceptedIdentities().containsKey(id)) {
                counts.merge("evidence:" + id, 1, Integer::sum);
            }
        });
    }

    private Set<UUID> scanArtifactInstances(String artifactId) {
        Set<UUID> result = new LinkedHashSet<>();
        Set<Inventory> inventories = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player player : Bukkit.getOnlinePlayers()) {
            inventories.add(player.getInventory());
            inventories.add(player.getEnderChest());
        }
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof InventoryHolder holder) inventories.add(holder.getInventory());
                }
            }
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                addArtifactInstance(entity.getItemStack(), artifactId, result);
            }
        }
        inventories.forEach(inventory -> {
            for (ItemStack item : inventory.getContents()) addArtifactInstance(item, artifactId, result);
        });
        return Set.copyOf(result);
    }

    private void addArtifactInstance(ItemStack item, String artifactId, Set<UUID> destination) {
        if (item == null || item.getType().isAir()) return;
        try {
            ContainerItem image = fromBukkitWithInstance(item);
            if (image.artifactId().filter(artifactId::equals).isPresent()) {
                destination.add(image.artifactInstance().orElseThrow());
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed tags never satisfy or authorize recovery.
        }
    }

    private void setArtifactIdentity(ItemStack item, String artifactId, UUID instance) {
        var meta = item.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("artifact lacks metadata");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(artifactIdKey, PersistentDataType.STRING, artifactId);
        pdc.set(artifactAliasKey, PersistentDataType.STRING, artifactId);
        pdc.set(artifactInstanceKey, PersistentDataType.STRING, instance.toString());
        item.setItemMeta(meta);
    }

    private void latch(String nodeId) {
        for (LocatedInventory located : locatedInventories(nodeId)) {
            if (located.block().getState() instanceof TileState tile) {
                tile.getPersistentDataContainer().set(
                        lockedKey, PersistentDataType.STRING, nodeId);
                tile.update(true, false);
            }
        }
    }

    private void collectProgressEscrowIds(Inventory inventory, Set<String> destination) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            String value = item.getItemMeta().getPersistentDataContainer()
                    .get(progressEscrowKey, PersistentDataType.STRING);
            if (value != null && !value.isBlank()) destination.add(value);
        }
    }

    private static int availableSlots(Player player) {
        int available = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) available++;
        }
        return available;
    }

    private record LocatedInventory(String component, Block block, Inventory inventory) {
        private LocatedInventory {
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(inventory, "inventory");
        }
    }
}
