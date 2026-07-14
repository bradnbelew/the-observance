package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.FixtureTransform.BlockPos;
import com.observance.watcher.v5runtime.FixtureTransform.Cardinal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Exact coordinate/PDC fixture registry. It never performs radius or nearest-component lookup.
 * Shared physical controls may have multiple explicit node bindings at the same coordinate.
 */
public final class BukkitFixtureIndex {
    private final Plugin plugin;
    private final Map<BlockKey, List<Binding>> blocks = new HashMap<>();
    private final Map<UUID, List<Binding>> entities = new HashMap<>();
    private final Map<String, SitePose> sites = new HashMap<>();

    public BukkitFixtureIndex(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Rebuild support for chunk loads, Hold repair, and plugin reload. */
    public synchronized void clear() {
        blocks.clear();
        entities.clear();
        sites.clear();
    }

    public synchronized void registerSite(
            String siteId, World world, BlockPos origin, Cardinal expectedFront) {
        requireText(siteId, "siteId");
        SitePose pose = new SitePose(world.getUID(), origin, expectedFront);
        SitePose previous = sites.putIfAbsent(siteId, pose);
        if (previous != null && !previous.equals(pose)) {
            throw new IllegalStateException("site pose already differs for " + siteId);
        }
    }

    public synchronized Binding bindBlock(
            String nodeId,
            String siteId,
            String componentId,
            int ordinal,
            Block block,
            Map<String, String> expectedPdc,
            Map<String, String> metadata) {
        Binding binding = new Binding(nodeId, siteId, componentId, ordinal, BindingKind.BLOCK,
                block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(),
                Optional.empty(), expectedPdc, metadata);
        add(blocks.computeIfAbsent(BlockKey.of(block), ignored -> new ArrayList<>()), binding);
        return binding;
    }

    public synchronized Binding bindEntity(
            String nodeId,
            String siteId,
            String componentId,
            int ordinal,
            Entity entity,
            Map<String, String> expectedPdc,
            Map<String, String> metadata) {
        Location location = entity.getLocation();
        Binding binding = new Binding(nodeId, siteId, componentId, ordinal, BindingKind.ENTITY,
                entity.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ(), Optional.of(entity.getUniqueId()), expectedPdc, metadata);
        add(entities.computeIfAbsent(entity.getUniqueId(), ignored -> new ArrayList<>()), binding);
        return binding;
    }

    public synchronized List<Binding> resolve(Block block) {
        return verified(blocks.getOrDefault(BlockKey.of(block), List.of()), block, null);
    }

    /** Coordinate-only lookup for protection after a placed block has replaced the expected state. */
    public synchronized List<Binding> atCoordinate(Block block) {
        return blocks.getOrDefault(BlockKey.of(block), List.of()).stream()
                .sorted(Comparator.comparing(Binding::nodeId)
                        .thenComparing(Binding::componentId)
                        .thenComparingInt(Binding::ordinal))
                .toList();
    }

    public synchronized List<Binding> resolve(Entity entity) {
        return verified(entities.getOrDefault(entity.getUniqueId(), List.of()), null, entity);
    }

    public synchronized List<Binding> bindings(String nodeId, String componentId) {
        List<Binding> result = new ArrayList<>();
        blocks.values().forEach(values -> values.stream()
                .filter(value -> value.nodeId().equals(nodeId)
                        && value.componentId().equals(componentId))
                .forEach(result::add));
        entities.values().forEach(values -> values.stream()
                .filter(value -> value.nodeId().equals(nodeId)
                        && value.componentId().equals(componentId))
                .forEach(result::add));
        result.sort(Comparator.comparingInt(Binding::ordinal));
        return List.copyOf(result);
    }

    public synchronized List<Binding> bindingsForNode(String nodeId) {
        List<Binding> result = new ArrayList<>();
        blocks.values().forEach(values -> values.stream()
                .filter(value -> value.nodeId().equals(nodeId)).forEach(result::add));
        entities.values().forEach(values -> values.stream()
                .filter(value -> value.nodeId().equals(nodeId)).forEach(result::add));
        result.sort(Comparator.comparing(Binding::componentId)
                .thenComparingInt(Binding::ordinal));
        return List.copyOf(result);
    }

    public synchronized Optional<SitePose> site(String siteId) {
        return Optional.ofNullable(sites.get(siteId));
    }

    public boolean verify(Binding binding, Block block) {
        if (binding.kind() != BindingKind.BLOCK
                || !binding.worldId().equals(block.getWorld().getUID())
                || binding.x() != block.getX() || binding.y() != block.getY()
                || binding.z() != block.getZ()) {
            return false;
        }
        return binding.expectedPdc().isEmpty()
                || block.getState() instanceof TileState tile
                && pdcMatches(tile.getPersistentDataContainer(), binding.expectedPdc());
    }

    public boolean verify(Binding binding, Entity entity) {
        return binding.kind() == BindingKind.ENTITY
                && binding.entityId().filter(entity.getUniqueId()::equals).isPresent()
                && binding.worldId().equals(entity.getWorld().getUID())
                && pdcMatches(entity.getPersistentDataContainer(), binding.expectedPdc());
    }

    private List<Binding> verified(List<Binding> candidates, Block block, Entity entity) {
        return candidates.stream().filter(binding -> block == null
                        ? verify(binding, Objects.requireNonNull(entity, "entity"))
                        : verify(binding, block))
                .sorted(Comparator.comparing(Binding::nodeId)
                        .thenComparing(Binding::componentId)
                        .thenComparingInt(Binding::ordinal))
                .toList();
    }

    private boolean pdcMatches(PersistentDataContainer container, Map<String, String> expected) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            NamespacedKey key = key(entry.getKey());
            if (!entry.getValue().equals(container.get(key, PersistentDataType.STRING))) {
                return false;
            }
        }
        return true;
    }

    private NamespacedKey key(String raw) {
        NamespacedKey parsed = NamespacedKey.fromString(raw, plugin);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid PDC key " + raw);
        }
        return parsed;
    }

    private static void add(List<Binding> bindings, Binding candidate) {
        for (Binding binding : bindings) {
            if (binding.nodeId().equals(candidate.nodeId())
                    && binding.componentId().equals(candidate.componentId())
                    && binding.ordinal() == candidate.ordinal()) {
                if (!binding.equals(candidate)) {
                    throw new IllegalStateException("fixture binding collision for " + candidate);
                }
                return;
            }
        }
        bindings.add(candidate);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    public enum BindingKind { BLOCK, ENTITY }

    public record Binding(
            String nodeId,
            String siteId,
            String componentId,
            int ordinal,
            BindingKind kind,
            UUID worldId,
            int x,
            int y,
            int z,
            Optional<UUID> entityId,
            Map<String, String> expectedPdc,
            Map<String, String> metadata) {
        public Binding {
            requireText(nodeId, "nodeId");
            requireText(siteId, "siteId");
            requireText(componentId, "componentId");
            if (ordinal < 0) {
                throw new IllegalArgumentException("ordinal cannot be negative");
            }
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(worldId, "worldId");
            entityId = Objects.requireNonNull(entityId, "entityId");
            expectedPdc = Map.copyOf(expectedPdc);
            metadata = Map.copyOf(metadata);
        }

        public Location location(World world) {
            if (!world.getUID().equals(worldId)) {
                throw new IllegalArgumentException("binding belongs to another world");
            }
            return new Location(world, x, y, z);
        }
    }

    public record SitePose(UUID worldId, BlockPos origin, Cardinal expectedFront) {
        public SitePose {
            Objects.requireNonNull(worldId, "worldId");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(expectedFront, "expectedFront");
        }
    }
}
