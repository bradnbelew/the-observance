package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.FixtureTransform;
import com.observance.watcher.v5runtime.FixtureTransform.BlockPos;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.install.V5MapArtAuthority;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.BindingKind;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.SitePose;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.Trigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Captures exact registered fixture state; missing/stale bindings simply fail predicates. */
public final class BukkitWorldStateEvaluator implements MechanicPorts.WorldState {
    private final Plugin plugin;
    private final BukkitFixtureIndex fixtures;
    private final BukkitLiveFacts liveFacts;
    private final V5MapArtAuthority.Catalog mapArt;

    public BukkitWorldStateEvaluator(
            Plugin plugin, BukkitFixtureIndex fixtures, BukkitLiveFacts liveFacts) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.liveFacts = Objects.requireNonNull(liveFacts, "liveFacts");
        this.mapArt = V5MapArtAuthority.loadDefault();
        if (!mapArt.valid()) {
            throw new IllegalStateException("V5 map-art authority is invalid: "
                    + String.join("; ", mapArt.issues()));
        }
    }

    @Override
    public MechanicObservation capture(
            PhysicalPredicateAuthority.Node node, UUID actor, Trigger trigger) {
        Objects.requireNonNull(trigger, "trigger");
        MechanicObservation.Builder result = MechanicObservation.builder(actor, node.siteId());
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        for (JsonElement element : predicate.getAsJsonArray("components")) {
            captureComponent(node, element.getAsJsonObject(), result);
        }
        captureWater(node, predicate, result);
        captureVerifiedVisualFacts(node, predicate, result);
        Player player = Bukkit.getPlayer(actor);
        if (player != null) {
            result.integerFact("destination_inventory_has_capacity", availableSlots(player));
        }
        liveFacts.enrich(node, actor, result);
        return result.build();
    }

    private void captureComponent(
            PhysicalPredicateAuthority.Node node,
            JsonObject definition,
            MechanicObservation.Builder result) {
        String componentId = definition.get("id").getAsString();
        List<Binding> bindings = fixtures.bindings(node.nodeId(), componentId);
        if (bindings.isEmpty()) {
            return;
        }
        List<ItemFrame> frames = new ArrayList<>();
        Map<Integer, MechanicItem> inventory = new HashMap<>();
        String blockType = null;
        boolean allVerified = true;
        for (Binding binding : bindings) {
            World world = Bukkit.getWorld(binding.worldId());
            if (world == null) {
                allVerified = false;
                continue;
            }
            if (binding.kind() == BindingKind.ENTITY) {
                Entity entity = binding.entityId().map(world::getEntity).orElse(null);
                if (!(entity instanceof ItemFrame frame) || !fixtures.verify(binding, entity)) {
                    allVerified = false;
                    continue;
                }
                frames.add(frame);
            } else {
                Block block = world.getBlockAt(binding.x(), binding.y(), binding.z());
                if (!fixtures.verify(binding, block)) {
                    allVerified = false;
                    continue;
                }
                blockType = block.getType().name();
                if (block.getState() instanceof InventoryHolder holder) {
                    captureInventory(holder.getInventory(), inventory);
                }
                if (block.getBlockData() instanceof org.bukkit.block.data.Powerable powerable
                        && powerable.isPowered()
                        && binding.metadata().containsKey("selector_value")) {
                    result.selector(componentId, binding.metadata().get("selector_value"));
                }
            }
        }
        if (!allVerified) {
            return;
        }
        result.bind(componentId, blockType == null ? "ENTITY" : blockType);
        if (!frames.isEmpty()) {
            List<MechanicItem> items = new ArrayList<>();
            List<Integer> rotations = new ArrayList<>();
            for (ItemFrame frame : frames) {
                items.add(item(frame.getItem()));
                rotations.add(frame.getRotation().ordinal());
            }
            result.frames(componentId, items, rotations);
        }
        if (!inventory.isEmpty() || isInventoryComponent(definition)) {
            result.inventory(componentId, inventory);
            if ("CHISELED_BOOKSHELF".equals(blockType)) {
                result.bookshelf(componentId, inventory);
            }
            if ("LECTERN".equals(blockType) && inventory.containsKey(0)) {
                result.book(componentId, inventory.get(0));
            }
        }
    }

    private void captureWater(
            PhysicalPredicateAuthority.Node node,
            JsonObject predicate,
            MechanicObservation.Builder result) {
        Optional<SitePose> poseOptional = fixtures.site(node.siteId());
        if (poseOptional.isEmpty()) {
            return;
        }
        SitePose pose = poseOptional.orElseThrow();
        World world = Bukkit.getWorld(pose.worldId());
        if (world == null) {
            return;
        }
        for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = operationElement.getAsJsonObject();
            if (!"water_present_below_frames".equals(operation.get("op").getAsString())) {
                continue;
            }
            for (JsonElement offsetElement : operation.getAsJsonArray("offsets")) {
                JsonArray values = offsetElement.getAsJsonArray();
                LocalOffset offset = new LocalOffset(
                        values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
                BlockPos worldPosition = FixtureTransform.toWorld(
                        pose.origin(), pose.expectedFront(), offset);
                if (world.getBlockAt(worldPosition.x(), worldPosition.y(), worldPosition.z())
                        .getType() == Material.WATER) {
                    result.water(offset);
                }
            }
        }
    }

    /**
     * Records semantic map facts only after the live frames prove that the packaged, hash-tagged
     * MapViews and their immutable renderers are actually present in the required orientation.
     * Copying the expected answer directly from the predicate would let an absent/blank map pass.
     */
    private void captureVerifiedVisualFacts(
            PhysicalPredicateAuthority.Node node,
            JsonObject predicate,
            MechanicObservation.Builder result) {
        boolean needsSemanticMapFact = false;
        for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = operationElement.getAsJsonObject();
            String op = operation.get("op").getAsString();
            if ("map_cutouts_reveal_exact".equals(op) || "landmark_relation".equals(op)) {
                needsSemanticMapFact = true;
            }
        }
        if (!needsSemanticMapFact || !allAuthoredMapViewsVerified(node, predicate)) {
            return;
        }
        for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = operationElement.getAsJsonObject();
            String op = operation.get("op").getAsString();
            if ("map_cutouts_reveal_exact".equals(op)) {
                result.stringList(op, strings(operation.getAsJsonArray("values")));
            } else if ("landmark_relation".equals(op)) {
                result.stringFact(op, operation.get("reveals").getAsString());
                result.stringList(op + ":north_of", strings(operation.getAsJsonArray("north_of")));
            }
        }
    }

    private boolean allAuthoredMapViewsVerified(
            PhysicalPredicateAuthority.Node node, JsonObject predicate) {
        int expected = 0;
        for (JsonElement element : predicate.getAsJsonArray("components")) {
            JsonObject component = element.getAsJsonObject();
            String componentId = component.get("id").getAsString();
            V5MapArtAuthority.Entry entry = mapArt.byComponent(node.nodeId(), componentId);
            if (entry == null) {
                continue;
            }
            expected++;
            List<Binding> bindings = fixtures.bindings(node.nodeId(), componentId);
            if (bindings.size() != 1 || bindings.getFirst().kind() != BindingKind.ENTITY) {
                return false;
            }
            Binding binding = bindings.getFirst();
            World world = Bukkit.getWorld(binding.worldId());
            Entity entity = world == null ? null : binding.entityId().map(world::getEntity).orElse(null);
            if (!(entity instanceof ItemFrame frame) || !fixtures.verify(binding, frame)
                    || frame.getRotation().ordinal() != entry.requiredFrameRotation()) {
                return false;
            }
            ItemStack item = frame.getItem();
            if (item.getType() != Material.FILLED_MAP
                    || !(item.getItemMeta() instanceof MapMeta meta)
                    || !meta.hasMapView()) {
                return false;
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String artId = pdc.get(key("v5_map_art_id"), PersistentDataType.STRING);
            String sha = pdc.get(key("v5_map_art_sha256"), PersistentDataType.STRING);
            if (!entry.id().equals(artId) || !entry.sha256().equals(sha)
                    || !meta.getMapView().isLocked()
                    || meta.getMapView().getRenderers().size() != 1
                    || !"ImmutableMapArtRenderer".equals(
                            meta.getMapView().getRenderers().getFirst().getClass().getSimpleName())) {
                return false;
            }
        }
        return expected > 0;
    }

    private static void captureInventory(
            Inventory source, Map<Integer, MechanicItem> destination) {
        for (int slot = 0; slot < source.getSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                destination.put(slot, item(stack));
            }
        }
    }

    private static int availableSlots(Player player) {
        int available = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                available++;
            }
        }
        return available;
    }

    private static boolean isInventoryComponent(JsonObject definition) {
        String block = definition.has("block") ? definition.get("block").getAsString() : "";
        return "BARREL".equals(block) || "LECTERN".equals(block)
                || "CHISELED_BOOKSHELF".equals(block);
    }

    public static MechanicItem item(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return MechanicItem.ordinary("AIR", 1, Map.of());
        }
        Map<String, String> pdc = new HashMap<>();
        if (stack.hasItemMeta()) {
            PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
            for (NamespacedKey key : container.getKeys()) {
                String value = container.get(key, PersistentDataType.STRING);
                if (value != null) {
                    pdc.put(key.getKey(), value);
                }
            }
        }
        Optional<UUID> instance = Optional.ofNullable(pdc.get(MechanicItem.ARTIFACT_INSTANCE))
                .map(UUID::fromString);
        return new MechanicItem(stack.getType().name(), stack.getAmount(), pdc, instance);
    }

    public NamespacedKey key(String raw) {
        NamespacedKey parsed = NamespacedKey.fromString(raw, plugin);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid PDC key " + raw);
        }
        return parsed;
    }

    private static List<String> strings(JsonArray values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }
}
