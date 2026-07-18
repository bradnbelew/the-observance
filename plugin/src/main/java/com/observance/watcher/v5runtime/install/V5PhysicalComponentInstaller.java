package com.observance.watcher.v5runtime.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.structure.CanonicalArtifactRegistry;
import com.observance.watcher.structure.DeepHoldV4Plan;
import com.observance.watcher.structure.V5AuthorityManifest;
import com.observance.watcher.v5runtime.FixtureTransform;
import com.observance.watcher.v5runtime.FixtureTransform.BlockPos;
import com.observance.watcher.v5runtime.FixtureTransform.Cardinal;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.Address;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.AddressKind;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.Finding;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.NodePlan;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.Severity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.awt.image.BufferedImage;

/**
 * Installs, reconciles and audits the physical surfaces declared by the packaged V5 authority.
 *
 * <p>This class deliberately has no solve/event logic. It never treats touching a tagged object as
 * completion. Repair is conservative: non-empty exposed inventories and unknown framed/displayed
 * items are reported, never cleared or replaced. Unique sources are seeded only on a fresh build;
 * later repair requires the progression/recovery layer to prove entitlement.</p>
 */
public final class V5PhysicalComponentInstaller {
    public enum Mode { FRESH_INSTALL, STATE_PRESERVING_REPAIR }

    public record Report(int nodesConsidered, int addressesConsidered, int placed,
                         int repaired, int preserved, int seeded, int logicalOnly,
                         List<Finding> findings) {
        public Report {
            findings = List.copyOf(findings);
        }

        public boolean clean() {
            return findings.stream().noneMatch(finding -> finding.severity() == Severity.BLOCKER);
        }

        public List<String> blockerMessages() {
            return findings.stream().filter(finding -> finding.severity() == Severity.BLOCKER)
                    .map(finding -> finding.nodeId() + "/" + finding.componentId() + ": "
                            + finding.message()).toList();
        }
    }

    private record Anchor(Location location, Cardinal front, int radius, int verticalRadius) { }

    private record Resolved(Address address, Anchor anchor, Location location) { }

    private record Seed(String nodeId, String componentId, String targetComponent,
                        int slot, ItemStack item, boolean uniqueArtifact) { }

    private record LotItem(String componentId, JsonObject spec) { }

    private record ItemAppearance(String title, List<String> lore) {
        private ItemAppearance {
            title = Objects.requireNonNullElse(title, "").trim();
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    private record PendingSite(String siteId, Anchor anchor) { }

    private record PendingBlock(Address address, int ordinal, Block block,
                                Map<String, String> expectedPdc,
                                Map<String, String> metadata) { }

    private record PendingEntity(Address address, int ordinal, Entity entity,
                                 Map<String, String> expectedPdc,
                                 Map<String, String> metadata) { }

    private enum BookStatus { PRESENT, LOCKED_ABSENT, INVALID }

    private static final List<String> UNIQUE_ID_KEYS = List.of(
            "v5_artifact_id", "artifact_id", "v5_evidence_id", "v5_receipt_id");
    private static final Map<String, List<Integer>> FRESH_FRAME_PERMUTATIONS = Map.of(
            "LC01:phase_frames", List.of(2, 0, 1),
            "KS01:strips", List.of(2, 5, 0, 4, 1, 3));

    private final ObservancePlugin plugin;
    private final V5PhysicalComponentCatalog.Catalog catalog;
    private final V5EvidenceItemTextAuthority.Catalog evidenceTexts;
    private final V5EvidenceItemAppearanceAuthority.Catalog appearances;
    private final V5MapArtAuthority.Catalog mapArt;

    public V5PhysicalComponentInstaller(ObservancePlugin plugin) {
        this(plugin, V5PhysicalComponentCatalog.loadDefault(),
                V5EvidenceItemTextAuthority.loadDefault(),
                V5EvidenceItemAppearanceAuthority.loadDefault(), V5MapArtAuthority.loadDefault());
    }

    V5PhysicalComponentInstaller(ObservancePlugin plugin,
                                 V5PhysicalComponentCatalog.Catalog catalog) {
        this(plugin, catalog, V5EvidenceItemTextAuthority.loadDefault(),
                V5EvidenceItemAppearanceAuthority.loadDefault(), V5MapArtAuthority.loadDefault());
    }

    V5PhysicalComponentInstaller(ObservancePlugin plugin,
                                 V5PhysicalComponentCatalog.Catalog catalog,
                                 V5EvidenceItemTextAuthority.Catalog evidenceTexts) {
        this(plugin, catalog, evidenceTexts, V5EvidenceItemAppearanceAuthority.loadDefault(),
                V5MapArtAuthority.loadDefault());
    }

    V5PhysicalComponentInstaller(ObservancePlugin plugin,
                                 V5PhysicalComponentCatalog.Catalog catalog,
                                 V5EvidenceItemTextAuthority.Catalog evidenceTexts,
                                 V5MapArtAuthority.Catalog mapArt) {
        this(plugin, catalog, evidenceTexts, V5EvidenceItemAppearanceAuthority.loadDefault(), mapArt);
    }

    V5PhysicalComponentInstaller(ObservancePlugin plugin,
                                 V5PhysicalComponentCatalog.Catalog catalog,
                                 V5EvidenceItemTextAuthority.Catalog evidenceTexts,
                                 V5EvidenceItemAppearanceAuthority.Catalog appearances,
                                 V5MapArtAuthority.Catalog mapArt) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.evidenceTexts = Objects.requireNonNull(evidenceTexts, "evidenceTexts");
        this.appearances = Objects.requireNonNull(appearances, "appearances");
        this.mapArt = Objects.requireNonNull(mapArt, "mapArt");
    }

    public V5PhysicalComponentCatalog.Catalog catalog() {
        return catalog;
    }

    public Report reconcileHold(Location mouth, Mode mode) {
        return run(mouth, Set.of("plugin", "plugin_finale"), mode, true);
    }

    public Report auditHold(Location mouth) {
        return run(mouth, Set.of("plugin", "plugin_finale"),
                Mode.STATE_PRESERVING_REPAIR, false);
    }

    public Report reconcileUnlit(Mode mode) {
        return run(null, Set.of("plugin_unlit"), mode, true);
    }

    public Report auditUnlit() {
        return run(null, Set.of("plugin_unlit"), Mode.STATE_PRESERVING_REPAIR, false);
    }

    /** World cells a generated readable-book lectern must never claim. */
    public Set<String> protectedBookMountCells(Location mouth) {
        Set<String> protectedCells = new HashSet<>();
        for (Address address : catalog.addresses()) {
            if (!"plugin".equals(address.owner()) && !"plugin_finale".equals(address.owner())) continue;
            if (address.kind() == AddressKind.LOGICAL_ONLY
                    || address.kind() == AddressKind.PORTABLE_ITEM
                    || address.kind() == AddressKind.BOOK_REFERENCE
                    || address.kind() == AddressKind.ENTITY_REFERENCE) continue;
            Anchor anchor = resolveAnchor(address.siteId(), mouth);
            if (anchor == null || anchor.location().getWorld() == null) continue;
            BlockPos position = FixtureTransform.toWorld(blockPos(anchor.location()),
                    anchor.front(), address.offset());
            Block exact = anchor.location().getWorld().getBlockAt(position.x(), position.y(), position.z());
            protectedCells.add(blockKey(exact));
            if (address.kind() == AddressKind.INTERACTION_CELL) {
                protectedCells.add(blockKey(exact.getRelative(BlockFace.DOWN)));
                protectedCells.add(blockKey(exact.getRelative(BlockFace.UP)));
                continue;
            }
            BlockFace front = address.kind() == AddressKind.ITEM_FRAME
                    ? itemFrameFacing(address, anchor) : face(anchor.front());
            if (address.kind() == AddressKind.ITEM_FRAME) {
                protectedCells.add(blockKey(exact.getRelative(front)));
            }
            // Reserve vanilla reach/stance feet, head, and floor cells. This keeps the separate
            // book projection from making a physical component inaccessible after reconciliation.
            for (int distance = 1; distance <= 3; distance++) {
                Block stance = exact.getRelative(front, distance);
                protectedCells.add(blockKey(stance));
                protectedCells.add(blockKey(stance.getRelative(BlockFace.UP)));
                protectedCells.add(blockKey(stance.getRelative(BlockFace.DOWN)));
            }
        }
        return Set.copyOf(protectedCells);
    }

    /** Exact authored component blocks; unlike reach/support cells, these may intentionally be lecterns. */
    public Set<String> exactPhysicalComponentCells(Location mouth) {
        Set<String> exactCells = new HashSet<>();
        for (Address address : catalog.addresses()) {
            if (!"plugin".equals(address.owner()) && !"plugin_finale".equals(address.owner())) continue;
            if (address.kind() != AddressKind.BLOCK && address.kind() != AddressKind.ITEM_FRAME
                    && address.kind() != AddressKind.ITEM_DISPLAY) continue;
            Anchor anchor = resolveAnchor(address.siteId(), mouth);
            if (anchor == null || anchor.location().getWorld() == null) continue;
            BlockPos position = FixtureTransform.toWorld(blockPos(anchor.location()),
                    anchor.front(), address.offset());
            exactCells.add(blockKey(anchor.location().getWorld().getBlockAt(
                    position.x(), position.y(), position.z())));
        }
        return Set.copyOf(exactCells);
    }

    /** Persist the approach-facing used when an external/Unlit fixture was stamped. */
    public boolean recordExternalOrientation(String siteId, BlockFace facing) {
        Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
        Location anchor = site == null ? null : site.location();
        if (anchor == null || anchor.getWorld() == null) return false;
        Cardinal front = cardinal(cardinalFace(facing).name());
        Location at = anchor.getBlock().getLocation().add(0.5, 0.25, 0.5);
        Marker selected = null;
        for (Marker marker : anchor.getWorld().getNearbyEntitiesByType(Marker.class, at, 0.7)) {
            String bound = marker.getPersistentDataContainer().get(key("v5_site_anchor"),
                    PersistentDataType.STRING);
            if (siteId.equals(bound)) {
                if (selected != null) marker.remove();
                else selected = marker;
            }
        }
        if (selected == null) selected = anchor.getWorld().spawn(at, Marker.class);
        selected.setPersistent(true);
        set(selected.getPersistentDataContainer(), "v5_site_anchor", siteId);
        set(selected.getPersistentDataContainer(), "v5_fixture_front", front.name());
        return true;
    }

    /**
     * Reattach immutable renderers to already-issued, loaded V5 maps after a server restart.
     * This never creates a map/item/frame, moves an entity, or seeds an inventory.
     */
    public Report rebindLoadedMapViews(Location mouth) {
        Accumulator result = new Accumulator();
        Set<String> nodes = new LinkedHashSet<>();
        for (Address address : catalog.addresses()) {
            if (!"FILLED_MAP".equals(address.material())) continue;
            nodes.add(address.nodeId());
            result.addresses++;
            Anchor anchor = resolveAnchor(address.siteId(), mouth);
            if (anchor == null || anchor.location().getWorld() == null) {
                result.findings.add(new Finding(Severity.NOTE, address.nodeId(), address.componentId(),
                        "map site " + address.siteId() + " is not placed/loaded"));
                continue;
            }
            Location backing = toWorld(anchor, address);
            if (!backing.getWorld().isChunkLoaded(backing.getBlockX() >> 4,
                    backing.getBlockZ() >> 4)) {
                result.findings.add(new Finding(Severity.NOTE, address.nodeId(), address.componentId(),
                        "map fixture chunk is not loaded"));
                continue;
            }
            Location plane = frameEntityPlane(backing, itemFrameFacing(address, anchor));
            List<ItemFrame> frames = new ArrayList<>(plane.getWorld()
                    .getNearbyEntitiesByType(ItemFrame.class, plane, 0.35));
            ItemFrame frame = selectTagged(frames, address);
            if (frame == null) frame = selectPdcMatching(frames, address);
            if (frame == null) {
                result.block(address, "loaded map frame is missing from authored plane");
                continue;
            }
            ItemStack item = frame.getItem();
            if (!rebindExistingMap(item, address)) {
                result.block(address, "loaded map has missing/wrong MapView, ID, SHA, or pixels");
                continue;
            }
            frame.setItem(item, false);
            auditMapArt(item, address, result);
            result.repaired++;
        }
        result.nodes = nodes.size();
        return result.report();
    }

    /** Rebind one initialized MapView when a MapInitializeEvent supplies it. */
    public boolean rebindMapView(MapView view) {
        if (view == null) return false;
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack item = frame.getItem();
                if (!(item.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()
                        || meta.getMapView().getId() != view.getId()) continue;
                String id = itemPdc(item, "v5_map_art_id");
                V5MapArtAuthority.Entry entry = mapArt.byId(id);
                if (entry == null || !entry.sha256().equals(itemPdc(item, "v5_map_art_sha256"))) {
                    return false;
                }
                configureMapView(view, entry);
                return true;
            }
        }
        return false;
    }

    /**
     * Populate the mechanics coordinate index from already-reconciled, loaded fixtures. Missing
     * sites/chunks are deferred for their load callback; a loaded malformed fixture blocks the
     * entire binding pass so the evaluator never receives a partially trusted index.
     */
    public Report bindLoadedFixtures(BukkitFixtureIndex index, Location mouth) {
        Objects.requireNonNull(index, "index");
        Accumulator result = new Accumulator();
        List<PendingBlock> blocks = new ArrayList<>();
        List<PendingEntity> entities = new ArrayList<>();
        Map<String, PendingSite> sites = new LinkedHashMap<>();
        Map<String, Integer> ordinals = new HashMap<>();
        Set<String> nodes = new LinkedHashSet<>();

        for (Finding finding : catalog.findings()) {
            if (finding.severity() == Severity.BLOCKER) result.findings.add(finding);
        }
        if (!evidenceTexts.valid()) for (String issue : evidenceTexts.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "evidence_item_text", issue));
        }
        if (!appearances.valid()) for (String issue : appearances.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "evidence_item_appearance", issue));
        }
        if (!mapArt.valid()) for (String issue : mapArt.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "map_art", issue));
        }
        inspectAppearanceCoverage(result);
        if (result.hasBlocker()) return result.report();

        for (Address address : catalog.addresses()) {
            if (address.kind() == AddressKind.LOGICAL_ONLY
                    || address.kind() == AddressKind.PORTABLE_ITEM) continue;
            nodes.add(address.nodeId());
            result.addresses++;
            Anchor anchor = resolveAnchor(address.siteId(), mouth);
            if (anchor == null || anchor.location().getWorld() == null) {
                result.findings.add(new Finding(Severity.NOTE, address.nodeId(), address.componentId(),
                        "site " + address.siteId() + " is not placed/loaded; binding deferred"));
                continue;
            }
            PendingSite previous = sites.putIfAbsent(address.siteId(), new PendingSite(address.siteId(), anchor));
            if (previous != null && (!blockPos(previous.anchor().location()).equals(blockPos(anchor.location()))
                    || previous.anchor().front() != anchor.front())) {
                result.block(address, "site resolves to conflicting poses");
                continue;
            }
            String ordinalKey = address.nodeId() + ':' + address.componentId();
            int ordinal = ordinals.compute(ordinalKey, (ignored, value) -> value == null ? 0 : value + 1);

            if (address.kind() == AddressKind.BOOK_REFERENCE) {
                Lectern lectern = findBookLectern(address, mouth);
                if (lectern == null) continue; // correctly absent while its availability flag is false
                if (!tagsMatch(lectern.getPersistentDataContainer(), address)) {
                    result.block(address, "loaded book lectern is missing exact binding tags");
                    continue;
                }
                blocks.add(new PendingBlock(address, ordinal, lectern.getBlock(), address.pdc(),
                        bindingMetadata(address)));
                continue;
            }

            Location at = toWorld(anchor, address);
            if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                result.findings.add(new Finding(Severity.NOTE, address.nodeId(), address.componentId(),
                        "fixture chunk is not loaded; binding deferred"));
                continue;
            }
            Resolved resolved = new Resolved(address, anchor, at);
            switch (address.kind()) {
                case BLOCK -> {
                    Block block = at.getBlock();
                    Material expected = Material.matchMaterial(address.material());
                    if (expected == null || block.getType() != expected) {
                        result.block(address, "loaded block is " + block.getType()
                                + ", expected " + address.material());
                        continue;
                    }
                    if (block.getState() instanceof TileState tile) {
                        if (!tagsMatch(tile.getPersistentDataContainer(), address)) {
                            result.block(address, "loaded tile is missing exact PDC binding tags");
                            continue;
                        }
                        blocks.add(new PendingBlock(address, ordinal, block, address.pdc(),
                                bindingMetadata(address)));
                    } else {
                        List<Entity> markers = taggedMarkers(resolved);
                        if (markers.size() != 1) {
                            result.block(address, "non-tile block requires exactly one tagged marker entity");
                            continue;
                        }
                        blocks.add(new PendingBlock(address, ordinal, block, Map.of(),
                                bindingMetadata(address)));
                        entities.add(new PendingEntity(address, ordinal, markers.get(0), address.pdc(),
                                bindingMetadata(address)));
                    }
                }
                case ITEM_FRAME -> {
                    Location plane = frameEntityPlane(at, itemFrameFacing(address, anchor));
                    List<ItemFrame> frames = new ArrayList<>(plane.getWorld()
                            .getNearbyEntitiesByType(ItemFrame.class, plane, 0.35));
                    List<ItemFrame> exact = frames.stream().filter(frame -> tagged(frame, address)).toList();
                    if (exact.size() != 1 || !(isReorderableFrame(address)
                            ? frameItemBelongsToSet(exact.get(0).getItem(), address)
                            : itemTagsMatch(exact.get(0).getItem(), address))) {
                        result.block(address, "item-frame binding is missing, duplicated, or has wrong item PDC");
                        continue;
                    }
                    if ("FILLED_MAP".equals(address.material())
                            && !mapArtMatches(exact.get(0).getItem(), address)) {
                        result.block(address, "filled-map binding is blank/stale or lacks immutable pixels");
                        continue;
                    }
                    entities.add(new PendingEntity(address, ordinal, exact.get(0), address.pdc(),
                            bindingMetadata(address)));
                }
                case ITEM_DISPLAY -> {
                    Location displayAt = at.clone().add(0.5, 0.35, 0.5);
                    List<ItemDisplay> exact = displayAt.getWorld().getNearbyEntitiesByType(
                            ItemDisplay.class, displayAt, 0.45).stream()
                            .filter(display -> tagged(display, address)).toList();
                    if (exact.size() != 1 || !itemTagsMatch(exact.get(0).getItemStack(), address)) {
                        result.block(address, "item-display binding is missing, duplicated, or malformed");
                        continue;
                    }
                    // ItemDisplay is visual-only and does not receive player interaction events.
                    // The colocated, tagged Interaction entity is the mechanics binding; retaining
                    // the display in the physical audit still proves exact material and PDC.
                    List<Entity> controls = taggedMarkers(resolved).stream()
                            .filter(Interaction.class::isInstance).toList();
                    if (controls.size() != 1) {
                        result.block(address, "item-display interaction control is missing or duplicated");
                        continue;
                    }
                    entities.add(new PendingEntity(address, ordinal, controls.get(0), address.pdc(),
                            bindingMetadata(address)));
                }
                case MARKER, INTERACTION_CELL -> {
                    List<Entity> exact = taggedMarkers(resolved);
                    if (exact.size() != 1) {
                        result.block(address, "marker binding is missing or duplicated");
                        continue;
                    }
                    entities.add(new PendingEntity(address, ordinal, exact.get(0), address.pdc(),
                            bindingMetadata(address)));
                }
                case ENTITY_REFERENCE -> {
                    Entity entity = findEntityReference(resolved);
                    if (entity == null || !tagsMatch(entity.getPersistentDataContainer(), address)) {
                        result.block(address, "entity reference is missing or lacks exact PDC");
                        continue;
                    }
                    entities.add(new PendingEntity(address, ordinal, entity, address.pdc(),
                            bindingMetadata(address)));
                }
                default -> { }
            }
        }
        result.nodes = nodes.size();
        if (result.hasBlocker()) return result.report();

        // Preflight against any already-bound index before changing it.
        for (PendingSite site : sites.values()) {
            BukkitFixtureIndex.SitePose expected = new BukkitFixtureIndex.SitePose(
                    site.anchor().location().getWorld().getUID(), blockPos(site.anchor().location()),
                    site.anchor().front());
            if (index.site(site.siteId()).isPresent() && !index.site(site.siteId()).get().equals(expected)) {
                result.findings.add(new Finding(Severity.BLOCKER, "INDEX", site.siteId(),
                        "existing site pose conflicts with loaded fixture"));
            }
        }
        for (PendingBlock pending : blocks) preflightExisting(index, pending, result);
        for (PendingEntity pending : entities) preflightExisting(index, pending, result);
        if (result.hasBlocker()) return result.report();

        for (PendingSite site : sites.values()) {
            index.registerSite(site.siteId(), site.anchor().location().getWorld(),
                    blockPos(site.anchor().location()), site.anchor().front());
        }
        for (PendingBlock pending : blocks) {
            index.bindBlock(pending.address().nodeId(), pending.address().siteId(),
                    pending.address().componentId(), pending.ordinal(), pending.block(),
                    pending.expectedPdc(), pending.metadata());
            result.preserved++;
        }
        for (PendingEntity pending : entities) {
            index.bindEntity(pending.address().nodeId(), pending.address().siteId(),
                    pending.address().componentId(), pending.ordinal(), pending.entity(),
                    pending.expectedPdc(), pending.metadata());
            result.preserved++;
        }
        return result.report();
    }

    private Report run(Location mouth, Set<String> owners, Mode mode, boolean mutate) {
        Accumulator result = new Accumulator();
        Map<String, String> nodeOwners = new HashMap<>();
        for (NodePlan node : catalog.nodes()) nodeOwners.put(node.node().nodeId(), node.node().owner());
        for (Finding finding : catalog.findings()) {
            if (owners.contains(nodeOwners.get(finding.nodeId()))) result.findings.add(finding);
        }
        for (String issue : evidenceTexts.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "evidence_item_text", issue));
        }
        for (String issue : appearances.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "evidence_item_appearance", issue));
        }
        for (String issue : mapArt.issues()) {
            result.findings.add(new Finding(Severity.BLOCKER, "AUTHORITY", "map_art", issue));
        }
        inspectAppearanceCoverage(result);
        for (Address address : catalog.addresses()) {
            if (!owners.contains(address.owner()) || !"FILLED_MAP".equals(address.material())) continue;
            V5MapArtAuthority.Entry entry = mapArt.byComponent(address.nodeId(), address.componentId());
            if (entry == null) {
                result.block(address, "filled-map component has no packaged map-art binding");
            } else if (address.requiredRotation() != null
                    && entry.requiredFrameRotation() != address.requiredRotation()) {
                result.block(address, "map-art rotation " + entry.requiredFrameRotation()
                        + " differs from predicate " + address.requiredRotation());
            }
        }
        if (result.hasBlocker()) return result.report(); // static authority fails before mutation

        Map<String, Resolved> resolvedByInstance = new LinkedHashMap<>();
        List<Resolved> resolved = new ArrayList<>();
        Set<String> nodesSeen = new LinkedHashSet<>();
        for (Address address : catalog.addresses()) {
            if (!owners.contains(address.owner())) continue;
            nodesSeen.add(address.nodeId());
            result.addresses++;
            if (address.kind() == AddressKind.LOGICAL_ONLY) {
                result.logicalOnly++;
                continue;
            }
            if (address.kind() == AddressKind.PORTABLE_ITEM
                    || address.kind() == AddressKind.BOOK_REFERENCE) continue;
            Anchor anchor = resolveAnchor(address.siteId(), mouth);
            if (anchor == null || anchor.location().getWorld() == null) {
                result.block(address, "site anchor " + address.siteId() + " is missing or unloaded");
                continue;
            }
            Location at = toWorld(anchor, address);
            World world = at.getWorld();
            if (world == null || !world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                result.block(address, "component chunk is not loaded; prepare/load the site before reconcile");
                continue;
            }
            Resolved physical = new Resolved(address, anchor, at);
            resolved.add(physical);
            resolvedByInstance.put(instanceKey(address), physical);
        }
        result.nodes = nodesSeen.size();
        inspectResolvedCollisions(resolved, result);
        if (result.hasBlocker() && mutate) return result.report();

        for (Resolved physical : resolved) reconcileAddress(physical, mode, mutate, result);
        inspectReorderableFrameSets(resolved, mode, result);
        Set<String> observedIdentities = collectLoadedIdentities();
        for (NodePlan node : catalog.nodes()) {
            if (!owners.contains(node.node().owner())) continue;
            inspectBookReferences(node, mouth, mode, mutate, result);
            reconcileSeeds(node, resolvedByInstance, mode, mutate, observedIdentities, result);
        }
        reconcileInteractionCells(resolved, mode, mutate, result);
        // Later nodes can place support blocks inside an earlier authored posture cell. Reassert
        // exact player-body cells after every component and generic reach lane has settled.
        for (Resolved physical : resolved) {
            if (physical.address().kind() == AddressKind.INTERACTION_CELL) {
                reconcileCell(physical, mode, mutate, result);
            }
        }
        if (owners.contains("plugin_unlit")) {
            reconcileUnlitPredicateEnvironment(mode, mutate, result);
            inspectUnlitReadableAuthority(result);
        }
        if (owners.contains("plugin") && mouth != null) {
            inspectHoldReadableAuthority(mouth, mode, mutate, result);
        }
        return result.report();
    }

    /**
     * Predicate clauses that describe world geometry rather than a named component still need an
     * exact, auditable build.  These blocks are deliberately not mechanics bindings: they are the
     * basin/cage that make BI04's water precondition and BI07's unreachable outer control true in
     * the traversable Minecraft world.
     */
    private void reconcileUnlitPredicateEnvironment(Mode mode, boolean mutate, Accumulator result) {
        Anchor well = resolveAnchor("unlit_house_well", null);
        if (well != null && well.location().getWorld() != null) {
            for (int right = -1; right <= 1; right++) {
                reconcileEnvironmentBlock("BI04", "reflection_water",
                        well, new LocalOffset(right, 0, 0), Material.WATER, mode, mutate, result);
                reconcileEnvironmentBlock("BI04", "basin_floor",
                        well, new LocalOffset(right, -1, 0), Material.DARK_PRISMARINE,
                        mode, mutate, result);
                reconcileEnvironmentBlock("BI04", "basin_back",
                        well, new LocalOffset(right, 0, -1), Material.DARK_PRISMARINE,
                        mode, mutate, result);
                reconcileEnvironmentBlock("BI04", "basin_lip",
                        well, new LocalOffset(right, 0, 1), Material.DARK_PRISMARINE,
                        mode, mutate, result);
            }
            for (int right : List.of(-2, 2)) {
                reconcileEnvironmentBlock("BI04", "basin_end",
                        well, new LocalOffset(right, 0, 0), Material.DARK_PRISMARINE,
                        mode, mutate, result);
            }
        }

        Anchor threshold = resolveAnchor("unlit_house_threshold", null);
        if (threshold != null && threshold.location().getWorld() != null) {
            List<LocalOffset> cage = List.of(
                    new LocalOffset(1, 0, 0), new LocalOffset(1, 1, 0),
                    new LocalOffset(1, 2, 0), new LocalOffset(3, 0, 0),
                    new LocalOffset(3, 1, 0), new LocalOffset(3, 2, 0),
                    new LocalOffset(2, 0, 1), new LocalOffset(2, 1, 1),
                    new LocalOffset(2, 2, 1), new LocalOffset(2, 2, 0));
            for (LocalOffset offset : cage) {
                reconcileEnvironmentBlock("BI07", "outer_control_cage",
                        threshold, offset, Material.TINTED_GLASS, mode, mutate, result);
            }
        }
    }

    private void reconcileEnvironmentBlock(String nodeId, String componentId, Anchor anchor,
                                           LocalOffset offset, Material expected, Mode mode,
                                           boolean mutate, Accumulator result) {
        BlockPos position = FixtureTransform.toWorld(blockPos(anchor.location()), anchor.front(), offset);
        Location location = new Location(anchor.location().getWorld(),
                position.x(), position.y(), position.z());
        Block block = location.getBlock();
        if (block.getType() == expected) {
            result.preserved++;
            return;
        }
        if (block.getState() instanceof InventoryHolder holder && !holder.getInventory().isEmpty()) {
            result.findings.add(new Finding(Severity.BLOCKER, nodeId, componentId,
                    "refused to replace non-empty " + block.getType() + " at " + blockKey(block)));
            return;
        }
        if (!mutate) {
            result.findings.add(new Finding(Severity.BLOCKER, nodeId, componentId,
                    "block is " + block.getType() + ", expected " + expected + " at " + blockKey(block)));
            return;
        }
        if (!safeReplace(block, mode)) {
            result.findings.add(new Finding(Severity.BLOCKER, nodeId, componentId,
                    "refused to overwrite unknown " + block.getType() + " at " + blockKey(block)));
            return;
        }
        block.setType(expected, false);
        result.repaired++;
    }

    private void inspectHoldReadableAuthority(Location mouth, Mode mode,
                                              boolean mutate, Accumulator result) {
        if (mouth == null || mouth.getWorld() == null) return;
        World world = mouth.getWorld();
        int minX = mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE;
        int maxX = mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE;
        int minY = mouth.getBlockY() + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE;
        int maxY = mouth.getBlockY() + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE;
        int minZ = mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE;
        int maxZ = mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE;
        Set<String> seen = new LinkedHashSet<>();
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            if ((chunk.getX() << 4) > maxX || (chunk.getX() << 4) + 15 < minX
                    || (chunk.getZ() << 4) > maxZ || (chunk.getZ() << 4) + 15 < minZ) continue;
            for (BlockState state : chunk.getTileEntities()) {
                if (!inside(state.getX(), state.getY(), state.getZ(),
                        minX, maxX, minY, maxY, minZ, maxZ)) continue;
                BlockState live = state.getBlock().getState();
                if (live instanceof InventoryHolder holder) {
                    Inventory inventory = holder.getInventory();
                    boolean changed = false;
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        ItemStack item = inventory.getItem(slot);
                        if (mutate && mode == Mode.FRESH_INSTALL && isRetiredReadable(item)) {
                            inventory.setItem(slot, null);
                            result.repaired++;
                            changed = true;
                            continue;
                        }
                        inspectReadableItem(item, blockKey(live.getBlock()) + '#' + slot, seen, result);
                    }
                    if (changed) live.update(true, false);
                }
            }
        }
        for (Entity entity : world.getEntities()) {
            Location at = entity.getLocation();
            if (!inside(at.getBlockX(), at.getBlockY(), at.getBlockZ(),
                    minX, maxX, minY, maxY, minZ, maxZ)) continue;
            if (entity instanceof ItemFrame frame) {
                inspectReadableItem(frame.getItem(), "frame@" + entity.getUniqueId(), seen, result);
            } else if (entity instanceof ItemDisplay display) {
                inspectReadableItem(display.getItemStack(), "display@" + entity.getUniqueId(), seen, result);
            }
        }
    }

    private boolean isRetiredReadable(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        String artifactId = itemPdc(item, "artifact_id");
        if (artifactId.isBlank()) artifactId = itemPdc(item, "v5_artifact_id");
        String bookId = itemPdc(item, "book_id");
        if (bookId.isBlank()) bookId = itemPdc(item, "v5_book_id");
        String evidenceId = itemPdc(item, "v5_evidence_id");
        return (artifactId.isBlank() || CanonicalArtifactRegistry.resolveId(artifactId) == null)
                && (bookId.isBlank() || V5AuthorityManifest.book(bookId) == null)
                && (evidenceId.isBlank() || evidenceTexts.get(evidenceId) == null);
    }

    private void inspectUnlitReadableAuthority(Accumulator result) {
        Set<String> scannedBlocks = new HashSet<>();
        Set<java.util.UUID> scannedEntities = new HashSet<>();
        Set<String> seenItems = new HashSet<>();
        List<String> sites = List.of(
                "unlit_house_lamp", "unlit_house_cairn", "unlit_house_coop",
                "unlit_house_well", "unlit_house_watch", "unlit_house_warm",
                "unlit_house_threshold", "unlit_house_base");
        for (String siteId : sites) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
            Location center = site == null ? null : site.location();
            if (center == null || center.getWorld() == null) continue;
            World world = center.getWorld();
            int radius = Math.max(4, Math.min(9, site.radius() + 2));
            int vertical = Math.max(3, Math.min(7, site.verticalRadius() + 2));
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -2; dy <= vertical; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block block = world.getBlockAt(center.getBlockX() + dx,
                                center.getBlockY() + dy, center.getBlockZ() + dz);
                        if (!scannedBlocks.add(blockKey(block))) continue;
                        if (!(block.getState() instanceof InventoryHolder holder)) continue;
                        for (int slot = 0; slot < holder.getInventory().getSize(); slot++) {
                            inspectReadableItem(holder.getInventory().getItem(slot),
                                    siteId + "@" + blockKey(block) + '#' + slot,
                                    seenItems, result, "UNLIT");
                        }
                    }
                }
            }
            for (Entity entity : world.getNearbyEntities(center, radius, vertical, radius)) {
                if (!scannedEntities.add(entity.getUniqueId())) continue;
                if (entity instanceof ItemFrame frame) {
                    inspectReadableItem(frame.getItem(), siteId + "@entity:" + entity.getUniqueId(),
                            seenItems, result, "UNLIT");
                } else if (entity instanceof ItemDisplay display) {
                    inspectReadableItem(display.getItemStack(),
                            siteId + "@entity:" + entity.getUniqueId(), seenItems, result, "UNLIT");
                }
            }
        }
    }

    private void inspectReadableInventory(Inventory inventory, String location, Set<String> seen,
                                          Accumulator result) {
        if (inventory == null) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inspectReadableItem(inventory.getItem(slot), location + "#" + slot, seen, result);
        }
    }

    private void inspectReadableItem(ItemStack item, String location, Set<String> seen,
                                     Accumulator result) {
        inspectReadableItem(item, location, seen, result, "HOLD");
    }

    private void inspectReadableItem(ItemStack item, String location, Set<String> seen,
                                     Accumulator result, String scope) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !seen.add(location)) return;
        String artifactId = itemPdc(item, "artifact_id");
        if (artifactId.isBlank()) artifactId = itemPdc(item, "v5_artifact_id");
        String bookId = itemPdc(item, "book_id");
        if (bookId.isBlank()) bookId = itemPdc(item, "v5_book_id");
        String evidenceId = itemPdc(item, "v5_evidence_id");
        List<String> faults = new ArrayList<>();
        boolean recognized = false;
        if (!artifactId.isBlank()) {
            recognized = CanonicalArtifactRegistry.resolveId(artifactId) != null;
            faults.addAll(CanonicalArtifactRegistry.audit(item, artifactId));
        }
        if (!bookId.isBlank()) {
            V5AuthorityManifest.BookEntry book = V5AuthorityManifest.book(bookId);
            recognized |= book != null;
            if (book == null || !bookMatches(item, book)) faults.add("book text/hash differs");
            if (!"5.0.0".equals(itemPdc(item, "story_version"))) {
                faults.add("story_version is not 5.0.0");
            }
        }
        if (!evidenceId.isBlank()) {
            V5EvidenceItemTextAuthority.Entry evidence = evidenceTexts.get(evidenceId);
            recognized |= evidence != null;
            if (evidence == null || !evidenceBookMatches(item, evidence)) {
                faults.add("evidence text differs from packaged authority");
            }
        }
        if (!recognized) faults.add("unrecognized/retired written book");
        for (String fault : faults) {
            // Identity-less legacy fixture books cannot satisfy any V5 mechanic. Keep them visible
            // to operators, but do not fail an otherwise playable build because Paper retained a
            // decorative snapshot after the fresh-install purge. Recognized malformed authority
            // content remains a hard blocker above.
            Severity severity = "unrecognized/retired written book".equals(fault)
                    ? Severity.WARNING : Severity.BLOCKER;
            result.findings.add(new Finding(severity, scope, "readable_authority",
                    location + ": " + fault));
        }
    }

    private static boolean inside(int x, int y, int z, int minX, int maxX,
                                  int minY, int maxY, int minZ, int maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private void reconcileAddress(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        switch (resolved.address().kind()) {
            case BLOCK -> reconcileBlock(resolved, mode, mutate, result);
            case ITEM_FRAME -> reconcileItemFrame(resolved, mode, mutate, result);
            case ITEM_DISPLAY -> reconcileItemDisplay(resolved, mode, mutate, result);
            case MARKER -> reconcileMarker(resolved, mutate, result);
            case ENTITY_REFERENCE -> reconcileEntityReference(resolved, mutate, result);
            case INTERACTION_CELL -> reconcileCell(resolved, mode, mutate, result);
            default -> { }
        }
    }

    private void reconcileBlock(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        Material expected = Material.matchMaterial(address.material());
        if (expected == null || !expected.isBlock()) {
            result.block(address, "authority material " + address.material() + " is not a placeable block");
            return;
        }
        Block block = resolved.location().getBlock();
        boolean newlyPlaced = false;
        if (block.getType() != expected) {
            if (block.getState() instanceof InventoryHolder holder && !holder.getInventory().isEmpty()) {
                result.block(address, "refused to replace non-empty " + block.getType()
                        + " inventory with " + expected);
                return;
            }
            if (!mutate) {
                result.block(address, "block is " + block.getType() + ", expected " + expected);
                return;
            }
            if (!safeReplace(block, mode)) {
                result.block(address, "refused to overwrite unknown " + block.getType()
                        + " with " + expected);
                return;
            }
            block.setType(expected, false);
            newlyPlaced = true;
            result.placed++;
        } else {
            result.preserved++;
        }
        if (mutate) orientBlock(block, resolved.anchor().front());
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            if (mutate) {
                tag(sign, address);
                String labelFor = address.pdc().getOrDefault("v5_label_for", "");
                List<String> label = appearances.label(address.nodeId(), labelFor);
                String stationLabelFor = address.pdc().getOrDefault("v5_station_label_for", "");
                if (label.isEmpty()) {
                    label = appearances.stationLabel(address.nodeId(), stationLabelFor);
                }
                if (!label.isEmpty()) {
                    sign.getSide(Side.FRONT).line(0, Component.text(label.get(0)));
                    sign.getSide(Side.FRONT).line(1, Component.text(label.get(1)));
                    sign.getSide(Side.FRONT).line(2, Component.empty());
                    sign.getSide(Side.FRONT).line(3, Component.empty());
                    sign.getSide(Side.BACK).line(0, Component.empty());
                    sign.getSide(Side.BACK).line(1, Component.empty());
                    sign.getSide(Side.BACK).line(2, Component.empty());
                    sign.getSide(Side.BACK).line(3, Component.empty());
                    sign.setWaxed(true);
                } else if (newlyPlaced || mode == Mode.FRESH_INSTALL) {
                    sign.getSide(Side.FRONT).line(0, Component.empty());
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.getSide(Side.FRONT).line(2, Component.empty());
                    sign.getSide(Side.FRONT).line(3, Component.empty());
                    sign.setWaxed(false);
                }
                sign.update(true, false);
            }
            if (block.getBlockData() instanceof WallSign wallSign) {
                ensureSupport(block.getRelative(wallSign.getFacing().getOppositeFace()), address,
                        "wall label sign", mode, mutate, result);
            } else {
                ensureSupport(block.getRelative(BlockFace.DOWN), address,
                        "standing answer sign", mode, mutate, result);
            }
        } else if (state instanceof TileState tile) {
            if (mutate) {
                tag(tile, address);
                JsonObject raw = JsonParser.parseString(address.rawComponentJson()).getAsJsonObject();
                String mask = string(raw, "required_mask");
                if (!mask.isBlank()) set(tile.getPersistentDataContainer(), "v5_required_mask", mask);
                String containerTitle = address.pdc().getOrDefault("v5_container_title", "");
                if (tile instanceof Container container && !containerTitle.isBlank()) {
                    container.customName(Component.text(containerTitle));
                }
                tile.update(true, false);
            }
        } else {
            ensureTaggedMarker(resolved, isOperable(expected), mutate, result);
        }
        if (block.getBlockData() instanceof Switch) {
            reconcileSwitchSupport(block, resolved, mode, mutate, result);
        }
        if (expected.name().endsWith("PRESSURE_PLATE")) {
            ensureSupport(block.getRelative(BlockFace.DOWN), address,
                    "pressure plate", mode, mutate, result);
            Block head = block.getRelative(BlockFace.UP);
            if (!head.isPassable()) {
                if (mutate && safeReplace(head, mode)) {
                    head.setType(Material.AIR, false);
                    result.repaired++;
                } else {
                    result.block(address, "pressure plate has no player head clearance");
                }
            }
        }
        if (expected.name().endsWith("_CANDLE")) {
            ensureSupport(block.getRelative(BlockFace.DOWN), address,
                    "candle", mode, mutate, result);
        }
        auditBlockState(resolved, mode, mutate, result);
    }

    private void reconcileSwitchSupport(Block block, Resolved resolved, Mode mode,
                                        boolean mutate, Accumulator result) {
        if (!(block.getBlockData() instanceof Switch control)) return;
        Address address = resolved.address();
        boolean exactAuthoredControl = "evaluation_handle".equals(address.componentId())
                || "CW07/cache_seal".equals(address.nodeId() + '/' + address.componentId())
                || "HS02/housing_latch".equals(address.nodeId() + '/' + address.componentId());
        if (exactAuthoredControl) {
            String supportId = address.componentId() + "_support";
            Address plannedSupport = catalog.addressesForNode(address.nodeId()).stream()
                    .filter(candidate -> supportId.equals(candidate.componentId()))
                    .findFirst().orElse(null);
            if (plannedSupport == null
                    || plannedSupport.offset().right() != address.offset().right()
                    || plannedSupport.offset().front() != address.offset().front()
                    || Math.abs(plannedSupport.offset().up() - address.offset().up()) != 1) {
                result.block(address, "exact control has no adjacent authored support address");
                return;
            }
            boolean ceiling = plannedSupport.offset().up() > address.offset().up();
            Block support = block.getRelative(ceiling ? BlockFace.UP : BlockFace.DOWN);
            ensureSupport(support, address, "exact authored control", mode, mutate, result);
            if (mutate) {
                control.setAttachedFace(ceiling
                        ? org.bukkit.block.data.FaceAttachable.AttachedFace.CEILING
                        : org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR);
                if (control.getFaces().contains(face(resolved.anchor().front()))) {
                    control.setFacing(face(resolved.anchor().front()));
                }
                block.setBlockData(control, false);
            }
            if (!block.getBlockData().isSupported(block)) {
                result.block(address, "exact authored control is not supported at "
                        + blockKey(block));
            }
            return;
        }
        BlockFace supportDirection = switch (control.getAttachedFace()) {
            case FLOOR -> BlockFace.DOWN;
            case CEILING -> BlockFace.UP;
            case WALL -> control.getFacing().getOppositeFace();
        };
        Block support = block.getRelative(supportDirection);
        if (!support.getType().isSolid() && mutate) {
            // Dense shared fixtures cannot promise that the authored-front wall cell remains a
            // wall after every neighboring node is installed. A floor-mounted vanilla switch is
            // equally operable and gives the control an independent, durable support cell.
            control.setAttachedFace("reader_control".equals(resolved.address().componentId())
                    ? org.bukkit.block.data.FaceAttachable.AttachedFace.CEILING
                    : org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR);
            if (control.getFaces().contains(face(resolved.anchor().front()))) {
                control.setFacing(face(resolved.anchor().front()));
            }
            block.setBlockData(control, false);
            support = block.getRelative(control.getAttachedFace()
                    == org.bukkit.block.data.FaceAttachable.AttachedFace.CEILING
                    ? BlockFace.UP : BlockFace.DOWN);
        }
        ensureSupport(support, resolved.address(),
                control.getAttachedFace() == org.bukkit.block.data.FaceAttachable.AttachedFace.WALL
                        ? "wall control" : "control",
                mode, mutate, result);
        if (!block.getBlockData().isSupported(block)) {
            result.block(address, "control is not supported at " + blockKey(block));
        }
    }

    private void auditBlockState(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        Block block = resolved.location().getBlock();
        Material expected = Material.matchMaterial(address.material());
        if (expected == null || block.getType() != expected) return;
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            auditTags(tile.getPersistentDataContainer(), address, result);
        } else if (!hasTaggedMarker(resolved)) {
            result.block(address, "non-tile component has no colocated tagged marker entity");
        }
        if (block.getBlockData() instanceof Directional directional
                && directional.getFaces().contains(face(resolved.anchor().front()))
                && directional.getFacing() != face(resolved.anchor().front())) {
            result.block(address, "facing is " + directional.getFacing() + ", expected "
                    + face(resolved.anchor().front()));
        }
        if (state instanceof Lectern lectern && !address.bookId().isBlank()) {
            ItemStack book = lectern.getInventory().getItem(0);
            if (book != null && !book.getType().isAir()
                    && !itemPdcEquals(book, "v5_book_id", address.bookId())
                    && !itemPdcEquals(book, "book_id", address.bookId())
                    && !(mutate && mode == Mode.FRESH_INSTALL)) {
                // Empty is valid while an authored unlock flag is closed. If a book is visible,
                // however, it must be the exact authority payload for this mount.
                result.block(address, "lectern contains the wrong book for " + address.bookId());
            }
        }
        if (state instanceof Container container) {
            String expectedTitle = address.pdc().getOrDefault("v5_container_title", "");
            String actualTitle = container.customName() == null ? ""
                    : PlainTextComponentSerializer.plainText().serialize(container.customName());
            if (!expectedTitle.isBlank() && !expectedTitle.equals(actualTitle)) {
                result.block(address, "container title is " + actualTitle + ", expected " + expectedTitle);
            }
        }
        if (state instanceof Sign sign) {
            String labelFor = address.pdc().getOrDefault("v5_label_for", "");
            List<String> expectedLabel = appearances.label(address.nodeId(), labelFor);
            if (expectedLabel.isEmpty()) {
                expectedLabel = appearances.stationLabel(address.nodeId(),
                        address.pdc().getOrDefault("v5_station_label_for", ""));
            }
            if (!expectedLabel.isEmpty()) {
                List<String> actual = sign.getSide(Side.FRONT).lines().stream()
                        .map(PlainTextComponentSerializer.plainText()::serialize).toList();
                if (!actual.equals(List.of(expectedLabel.get(0), expectedLabel.get(1), "", ""))) {
                    result.block(address, "container label text differs from player-visible authority");
                }
                if (!sign.isWaxed()) result.block(address, "container label sign is editable");
            }
        }
    }

    private void reconcileItemFrame(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        Material itemMaterial = Material.matchMaterial(address.material());
        if (itemMaterial == null || itemMaterial.isAir()) {
            result.block(address, "item-frame material " + address.material() + " is invalid");
            return;
        }
        BlockFace facing = itemFrameFacing(address, resolved.anchor());
        // ITEM_FRAME offsets name the supporting block. The entity lives on that block's
        // front face, toward the documented player standing cell.
        Block backing = resolved.location().getBlock();
        if (!backing.getType().isSolid()) {
            if (mutate && safeReplace(backing)) {
                backing.setType(Material.POLISHED_DEEPSLATE, false);
                result.repaired++;
            } else {
                result.block(address, "frame backing is not solid at " + blockKey(backing));
                return;
            }
        }
        Block hangingCell = backing.getRelative(facing);
        if (!hangingCell.isPassable()) {
            if (mutate && safeReplace(hangingCell, mode)) {
                // ITEM_FRAME offsets own the solid backing and its adjacent hanging cell. Dense
                // Hold shell dressing may fill that cell; carve only this exact authority-owned
                // air cell before spawning so reverse-face frames remain reachable and visible.
                hangingCell.setType(Material.AIR, false);
                result.repaired++;
            } else {
                result.block(address, "item-frame hanging cell is blocked at " + blockKey(hangingCell));
                return;
            }
        }
        Location frameAt = frameEntityPlane(resolved.location(), facing);
        List<ItemFrame> frames = new ArrayList<>(frameAt.getWorld()
                .getNearbyEntitiesByType(ItemFrame.class, frameAt, 0.35));
        boolean reorderable = isReorderableFrame(address);
        ItemFrame frame = selectTagged(frames, address);
        if (frame == null) frame = selectPdcMatching(frames, address);
        if (frame == null && frames.size() == 1) {
            ItemStack candidate = frames.get(0).getItem();
            if (candidate == null || candidate.getType().isAir()
                    || candidate.getType() == itemMaterial) frame = frames.get(0);
        }
        if (frame == null && !frames.isEmpty()) {
            result.block(address, "multiple item frames occupy the exact component plane");
            return;
        }
        if (frame == null && V5MovableFramePolicy.mayInferDisplacementFromItemIdentity(reorderable)) {
            List<ItemFrame> displaced = matchingFramesNear(resolved, address, 1.35);
            if (displaced.size() > 1) {
                result.block(address, "duplicate matching frames exist near the authored plane");
                return;
            }
            if (displaced.size() == 1) {
                if (!mutate) {
                    result.block(address, "matching frame is displaced from its authored plane");
                    return;
                }
                ItemFrame old = displaced.get(0);
                ItemStack preservedItem = old.getItem().clone();
                Rotation preservedRotation = old.getRotation();
                old.remove();
                frame = spawnItemFrame(resolved.location(), facing);
                frame.setItem(preservedItem, false);
                frame.setRotation(preservedRotation);
                result.repaired++;
            }
        }
        if (frame == null) {
            if (!mutate) {
                result.block(address, "item frame is missing");
                return;
            }
            frame = spawnItemFrame(resolved.location(), facing);
            result.placed++;
        }
        ItemStack current = frame.getItem();
        boolean empty = current == null || current.getType().isAir();
        boolean materialMatches = !empty && current.getType() == itemMaterial;
        if (reorderable) {
            if (!empty && !frameItemBelongsToSet(current, address)) {
                result.block(address, "refused unknown item in reorderable frame: "
                        + current.getType());
                return;
            }
        } else if (!empty && (!materialMatches || !itemTagsMatch(current, address))
                && !isInstallerOwned(frame, current)) {
            result.block(address, "refused to overwrite unknown framed " + current.getType());
            return;
        }
        if (mutate) {
            frame.setFacingDirection(facing, true);
            frame.setInvulnerable(true);
            frame.setPersistent(true);
            frame.setFixed(false);
            tag(frame, address);
            if (reorderable && mode == Mode.FRESH_INSTALL) {
                Address source = freshFrameSource(address);
                if (source == null) {
                    result.block(address, "fresh frame permutation is incomplete");
                    return;
                }
                current = createFramedItem(itemMaterial, source, frameAt.getWorld());
            } else if (reorderable && empty) {
                // An extracted piece can be in a player inventory/escrow.  Repair must never mint
                // a second copy merely because a frame is momentarily empty.
                result.block(address, "reorderable frame is empty; preserved without duplicating its piece");
                return;
            } else if (reorderable) {
                current = addItemTags(current, Map.of(), address.nodeId(), address.componentId());
            } else if (empty || !materialMatches) {
                current = createFramedItem(itemMaterial, address, frameAt.getWorld());
            } else {
                current = addItemTags(current, address.pdc(), address.nodeId(), address.componentId());
                if (itemMaterial == Material.FILLED_MAP) {
                    current = bindMapArt(current, address, frameAt.getWorld());
                }
            }
            frame.setItem(current, false);
            if ((empty || mode == Mode.FRESH_INSTALL) && address.resetRotation() != null) {
                frame.setRotation(rotation(address.resetRotation()));
            }
        }
        if (frame.getFacing() != facing) result.block(address, "frame facing is " + frame.getFacing()
                + ", expected " + facing);
        ItemStack actual = frame.getItem();
        if (actual.getType() != itemMaterial) result.block(address, "frame item is " + actual.getType()
                + ", expected " + itemMaterial);
        if (mutate && mode == Mode.FRESH_INSTALL && address.resetRotation() != null
                && frame.getRotation() != rotation(address.resetRotation())) {
            result.block(address, "fresh reset rotation is " + frame.getRotation()
                    + ", expected ordinal " + address.resetRotation());
        }
        if (reorderable) {
            if (!frameItemBelongsToSet(actual, address)) {
                result.block(address, "frame item is not one of the exact movable set");
            }
        } else {
            auditItemPdc(actual, address, result);
        }
        if (!appearanceMatches(actual)) {
            result.block(address, "frame item name/lore differs from player-visible authority");
        }
        if (itemMaterial == Material.FILLED_MAP) auditMapArt(actual, address, result);
        auditTags(frame.getPersistentDataContainer(), address, result);
    }

    private void reconcileItemDisplay(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        Material material = Material.matchMaterial(address.material());
        if (material == null || material.isAir()) {
            result.block(address, "display material " + address.material() + " is invalid");
            return;
        }
        Block cell = resolved.location().getBlock();
        if (!cell.isPassable()) {
            if (mutate && safeReplace(cell)) {
                cell.setType(Material.AIR, false);
                result.repaired++;
            } else {
                result.block(address, "item display cell is obstructed by " + cell.getType());
                return;
            }
        }
        ensureSupport(cell.getRelative(BlockFace.DOWN), address, "item display", mode, mutate, result);
        Location at = resolved.location().clone().add(0.5, 0.35, 0.5);
        List<ItemDisplay> displays = new ArrayList<>(at.getWorld()
                .getNearbyEntitiesByType(ItemDisplay.class, at, 0.45));
        ItemDisplay display = displays.stream().filter(entity -> tagged(entity, address)).findFirst().orElse(null);
        if (display == null && displays.size() == 1 && displays.get(0).getItemStack().getType() == material) {
            display = displays.get(0);
        }
        if (display == null && !displays.isEmpty()) {
            result.block(address, "unknown item display occupies marker location");
            return;
        }
        if (display == null) {
            if (!mutate) {
                result.block(address, "item display is missing");
                return;
            }
            display = at.getWorld().spawn(at, ItemDisplay.class);
            result.placed++;
        }
        ItemStack existing = display.getItemStack();
        if (existing != null && !existing.getType().isAir() && existing.getType() != material
                && !tagged(display, address)) {
            result.block(address, "refused to overwrite unknown displayed " + existing.getType());
            return;
        }
        if (mutate) {
            display.setItemStack(createItem(material, address.pdc(), address.nodeId(), address.componentId()));
            display.setPersistent(true);
            display.setInvulnerable(true);
            tag(display, address);
            ensureTaggedMarker(resolved, true, true, result);
        }
        if (display.getItemStack().getType() != material) result.block(address, "display item mismatch");
        auditItemPdc(display.getItemStack(), address, result);
        if (!appearanceMatches(display.getItemStack())) {
            result.block(address, "display item name/lore differs from player-visible authority");
        }
        auditTags(display.getPersistentDataContainer(), address, result);
    }

    private void reconcileMarker(Resolved resolved, boolean mutate, Accumulator result) {
        boolean interactive = resolved.address().pdc().containsKey("v5_finale_control")
                || resolved.address().pdc().containsKey("v5_rp04_bridge_control")
                || resolved.address().pdc().containsKey("v5_rp04_sector");
        ensureTaggedMarker(resolved, interactive, mutate, result);
        PersistentDataHolder marker = taggedMarker(resolved);
        if (marker == null) {
            result.block(resolved.address(), "exact marker entity is missing");
            return;
        }
        auditTags(marker.getPersistentDataContainer(), resolved.address(), result);
    }

    private void reconcileEntityReference(Resolved resolved, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        JsonObject raw = JsonParser.parseString(address.rawComponentJson()).getAsJsonObject();
        double radius = raw.has("max_distance") ? raw.get("max_distance").getAsDouble()
                : Math.max(2.0, resolved.anchor().radius());
        String expectedNpc = address.pdc().getOrDefault("v5_npc_id", "");
        Entity entity = null;
        if ("wren".equalsIgnoreCase(expectedNpc) && plugin.wren() != null) {
            entity = plugin.wren().body();
        }
        if (entity == null) {
            Location center = resolved.location().clone().add(0.5, 0, 0.5);
            for (Entity candidate : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (candidate instanceof Marker || candidate instanceof Interaction
                        || candidate instanceof ItemFrame || candidate instanceof ItemDisplay) continue;
                if (pdcMatches(candidate.getPersistentDataContainer(), address.pdc())
                        || ("wren".equalsIgnoreCase(expectedNpc)
                        && candidate.getPersistentDataContainer().has(key("wren_npc"),
                        PersistentDataType.STRING))) {
                    entity = candidate;
                    break;
                }
            }
        }
        boolean outOfRange = entity != null && (entity.getWorld() != resolved.location().getWorld()
                || entity.getLocation().distanceSquared(resolved.location()) > radius * radius);
        if ((entity == null || outOfRange) && mutate && "wren".equalsIgnoreCase(expectedNpc)
                && plugin.wren() != null) {
            entity = plugin.wren().spawn(resolved.location().clone().add(0.5, 0, 0.5));
            if (entity != null) result.repaired++;
        }
        if (entity == null) {
            result.block(address, "required entity " + expectedNpc + " is missing");
            return;
        }
        if (entity.getWorld() != resolved.location().getWorld()
                || entity.getLocation().distanceSquared(resolved.location()) > radius * radius) {
            result.block(address, "required entity " + expectedNpc + " is outside radius " + radius);
            return;
        }
        if (mutate) tag(entity, address);
        auditTags(entity.getPersistentDataContainer(), address, result);
    }

    private void reconcileCell(Resolved resolved, Mode mode, boolean mutate, Accumulator result) {
        Address address = resolved.address();
        Block feet = resolved.location().getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block floor = feet.getRelative(BlockFace.DOWN);
        Material authoredFloor = Material.matchMaterial(address.material());
        if (authoredFloor != null && authoredFloor.isBlock()) {
            if (floor.getType() != authoredFloor) {
                if (mutate && safeReplace(floor, mode)) {
                    floor.setType(authoredFloor, false);
                    result.repaired++;
                } else result.block(address, "route floor is " + floor.getType() + ", expected " + authoredFloor);
            }
        } else if (!floor.getType().isSolid()) {
            if (mutate && safeReplace(floor, mode)) {
                floor.setType(Material.POLISHED_DEEPSLATE, false);
                result.repaired++;
            } else result.block(address, "interaction cell has no solid floor");
        }
        for (Block clearance : List.of(feet, head)) {
            if (clearance.isPassable()) continue;
            if (mutate && safeReplace(clearance, mode)) {
                clearance.setType(Material.AIR, false);
                result.repaired++;
            } else {
                result.block(address, "interaction cell lacks two-block player clearance at "
                        + blockKey(clearance));
            }
        }
        ensureTaggedMarker(resolved, false, mutate, result);
    }

    private void reconcileInteractionCells(List<Resolved> resolved, Mode mode,
                                           boolean mutate, Accumulator result) {
        Set<String> checked = new HashSet<>();
        Set<String> authoredComponents = new HashSet<>();
        for (Resolved physical : resolved) {
            AddressKind kind = physical.address().kind();
            if (kind == AddressKind.BLOCK || kind == AddressKind.ITEM_FRAME
                    || kind == AddressKind.ITEM_DISPLAY) {
                authoredComponents.add(blockKey(physical.location().getBlock()));
                if (kind == AddressKind.ITEM_FRAME) {
                    // The adjacent air block is part of the mounted frame's physical envelope.
                    // Never turn it into a platform for a higher control in a dense, stacked
                    // reader: doing so leaves the backing intact while silently breaking the
                    // lower frame on the next independent audit.
                    authoredComponents.add(blockKey(physical.location().getBlock().getRelative(
                            itemFrameFacing(physical.address(), physical.anchor()))));
                }
            }
        }
        for (Resolved physical : resolved) {
            Address address = physical.address();
            if (!requiresFrontInteraction(physical)) continue;
            BlockFace front = address.kind() == AddressKind.ITEM_FRAME
                    ? itemFrameFacing(address, physical.anchor())
                    : face(physical.anchor().front());
            Block interact = null;
            Block repairFallback = null;
            // Dense stations may mount controls in two or three vertical rows. Prefer their
            // existing room-floor stance before a same-height synthetic platform; the latter can
            // occupy a lower frame's hanging plane and is less believable player-facing geometry.
            search:
            for (int vertical : List.of(-1, -2, -3, 0, 1)) {
                for (int distance = 1; distance <= 3; distance++) {
                    Block candidate = physical.location().getBlock().getRelative(front, distance);
                    if (vertical < 0) candidate = candidate.getRelative(BlockFace.DOWN, -vertical);
                    if (vertical > 0) candidate = candidate.getRelative(BlockFace.UP, vertical);
                    Block candidateHead = candidate.getRelative(BlockFace.UP);
                    if (authoredComponents.contains(blockKey(candidate))
                            || authoredComponents.contains(blockKey(candidateHead))) continue;
                    if (repairFallback == null) repairFallback = candidate;
                    if (candidate.getRelative(BlockFace.DOWN).getType().isSolid()
                            && candidate.isPassable() && candidateHead.isPassable()) {
                        interact = candidate;
                        break search;
                    }
                }
            }
            if (interact == null) interact = repairFallback == null
                    ? physical.location().getBlock().getRelative(front) : repairFallback;
            String key = blockKey(interact);
            if (!checked.add(key)) continue;
            Block floor = interact.getRelative(BlockFace.DOWN);
            Block head = interact.getRelative(BlockFace.UP);
            if (!floor.getType().isSolid()) {
                if (mutate && safeReplace(floor, mode)) {
                    floor.setType(Material.POLISHED_DEEPSLATE, false);
                    result.repaired++;
                } else {
                    result.findings.add(new Finding(Severity.BLOCKER, address.nodeId(),
                            address.componentId(), "front interaction cell has no floor at " + key));
                }
            }
            for (Block clearance : List.of(interact, head)) {
                if (clearance.isPassable()) continue;
                if (authoredComponents.contains(blockKey(clearance))) {
                    result.findings.add(new Finding(Severity.BLOCKER, address.nodeId(),
                            address.componentId(), "front interaction cell collides with an authored component at "
                            + blockKey(clearance)));
                } else if (mutate && safeReplace(clearance, mode)) {
                    clearance.setType(Material.AIR, false);
                    result.repaired++;
                } else {
                    result.findings.add(new Finding(Severity.BLOCKER, address.nodeId(),
                            address.componentId(), "front interaction cell is blocked at "
                            + blockKey(clearance)));
                }
            }
        }
    }

    private void inspectResolvedCollisions(List<Resolved> resolved, Accumulator result) {
        Map<String, List<Resolved>> positions = new LinkedHashMap<>();
        for (Resolved physical : resolved) {
            AddressKind kind = physical.address().kind();
            if (kind != AddressKind.BLOCK && kind != AddressKind.ITEM_FRAME
                    && kind != AddressKind.ITEM_DISPLAY) continue;
            positions.computeIfAbsent(blockKey(physical.location().getBlock()), ignored -> new ArrayList<>())
                    .add(physical);
        }
        for (Map.Entry<String, List<Resolved>> entry : positions.entrySet()) {
            List<Resolved> values = entry.getValue();
            if (values.size() < 2 || compatible(values)) continue;
            Address address = values.get(0).address();
            result.block(address, "world-space collision at " + entry.getKey() + ": "
                    + values.stream().map(value -> value.address().nodeId() + "/"
                    + value.address().componentId()).toList());
        }
    }

    private static boolean compatible(List<Resolved> values) {
        Address first = values.get(0).address();
        if (values.stream().allMatch(value -> value.address().kind() == AddressKind.ITEM_FRAME
                && value.address().material().equals(first.material()))) return true;
        return values.stream().allMatch(value -> value.address().kind() == AddressKind.BLOCK
                && value.address().material().equals("CHISELED_BOOKSHELF")
                && value.address().nodeId().equals(first.nodeId()));
    }

    private void inspectBookReferences(NodePlan node, Location mouth, Mode mode,
                                       boolean mutate, Accumulator result) {
        JsonObject predicate = JsonParser.parseString(node.predicateJson()).getAsJsonObject();
        for (JsonElement element : predicate.getAsJsonArray("components")) {
            JsonObject component = element.getAsJsonObject();
            Set<String> books = new LinkedHashSet<>();
            JsonObject bookPdc = object(component, "book_pdc");
            if (bookPdc != null && bookPdc.has("v5_book_id")) books.add(bookPdc.get("v5_book_id").getAsString());
            String sourceBook = string(component, "source_book");
            if (!sourceBook.isBlank()) books.add(sourceBook);
            for (String book : books) {
                Address binding = node.addresses().stream().filter(address ->
                        address.componentId().equals(string(component, "id"))
                                && address.kind() == AddressKind.BOOK_REFERENCE
                                && address.bookId().equals(book)).findFirst().orElse(null);
                BookStatus status = binding == null ? BookStatus.INVALID
                        : reconcileBookNear(binding, mouth, mutate);
                if (status == BookStatus.INVALID && !(mutate && mode == Mode.FRESH_INSTALL)) {
                    // The first fresh physical pass owns mounts, not their contents. syncV5Books
                    // immediately follows it and is the sole writer of canonical pages/PDC; the
                    // mandatory read-only physical pass then enforces the exact book authority.
                    result.findings.add(new Finding(Severity.WARNING, node.node().nodeId(),
                            string(component, "id"), "exact lectern book " + book
                            + " is malformed or differs from packaged authority"));
                }
            }
        }
    }

    private BookStatus reconcileBookNear(Address binding, Location mouth, boolean mutate) {
        String siteId = binding.siteId();
        String bookId = binding.bookId();
        V5AuthorityManifest.BookEntry canonical = V5AuthorityManifest.book(bookId);
        boolean placed = V5AuthorityManifest.bookPlacements().stream()
                .anyMatch(row -> row.bookId().equals(bookId));
        if (canonical == null || !placed) return BookStatus.INVALID;
        Anchor anchor = resolveAnchor(siteId, mouth);
        if (anchor == null || anchor.location().getWorld() == null) return BookStatus.INVALID;
        int radius = Math.max(4, Math.min(16, anchor.radius() + 4));
        for (int right = -radius; right <= radius; right++) {
            for (int up = -2; up <= Math.min(4, anchor.verticalRadius()); up++) {
                for (int front = -radius; front <= radius; front++) {
                    BlockPos pos = FixtureTransform.toWorld(blockPos(anchor.location()), anchor.front(),
                            new FixtureTransform.LocalOffset(right, up, front));
                    Block block = anchor.location().getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
                    if (!(block.getState() instanceof Lectern lectern)) continue;
                    ItemStack item = lectern.getInventory().getItem(0);
                    if (!itemPdcEquals(item, "book_id", bookId)
                            && !itemPdcEquals(item, "v5_book_id", bookId)) continue;
                    if (mutate && item != null) {
                        ItemStack tagged = canonicalBook(item, canonical);
                        tagged = addItemTags(tagged, Map.of("v5_book_id", bookId),
                                "BOOK", bookId);
                        lectern.getInventory().setItem(0, tagged);
                        tag(lectern, binding);
                        lectern.update(true, false);
                        item = tagged;
                    }
                    if (!bookMatches(item, canonical)) return BookStatus.INVALID;
                    return tagsMatch(lectern.getPersistentDataContainer(), binding)
                            ? BookStatus.PRESENT : BookStatus.INVALID;
                }
            }
        }
        // All authored books are flag-gated. Absence before the visibility/issue flag is correct;
        // syncV5Books owns insertion/removal and audits the exact mount when the flag changes.
        return BookStatus.LOCKED_ABSENT;
    }

    private Lectern findBookLectern(Address binding, Location mouth) {
        V5AuthorityManifest.BookEntry canonical = V5AuthorityManifest.book(binding.bookId());
        Anchor anchor = resolveAnchor(binding.siteId(), mouth);
        if (canonical == null || anchor == null || anchor.location().getWorld() == null) return null;
        int radius = Math.max(4, Math.min(16, anchor.radius() + 4));
        for (int right = -radius; right <= radius; right++) {
            for (int up = -2; up <= Math.min(4, anchor.verticalRadius()); up++) {
                for (int front = -radius; front <= radius; front++) {
                    BlockPos pos = FixtureTransform.toWorld(blockPos(anchor.location()), anchor.front(),
                            new FixtureTransform.LocalOffset(right, up, front));
                    Block block = anchor.location().getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
                    if (!(block.getState() instanceof Lectern lectern)) continue;
                    ItemStack item = lectern.getInventory().getItem(0);
                    if ((itemPdcEquals(item, "book_id", binding.bookId())
                            || itemPdcEquals(item, "v5_book_id", binding.bookId()))
                            && bookMatches(item, canonical)) return lectern;
                }
            }
        }
        return null;
    }

    private Entity findEntityReference(Resolved resolved) {
        Address address = resolved.address();
        JsonObject raw = JsonParser.parseString(address.rawComponentJson()).getAsJsonObject();
        double radius = raw.has("max_distance") ? raw.get("max_distance").getAsDouble()
                : Math.max(2.0, resolved.anchor().radius());
        String expectedNpc = address.pdc().getOrDefault("v5_npc_id", "");
        Entity entity = "wren".equalsIgnoreCase(expectedNpc) && plugin.wren() != null
                ? plugin.wren().body() : null;
        if (entity != null && entity.getWorld() == resolved.location().getWorld()
                && entity.getLocation().distanceSquared(resolved.location()) <= radius * radius) return entity;
        Location center = resolved.location().clone().add(0.5, 0, 0.5);
        for (Entity candidate : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (candidate instanceof Marker || candidate instanceof Interaction
                    || candidate instanceof ItemFrame || candidate instanceof ItemDisplay) continue;
            if (pdcMatches(candidate.getPersistentDataContainer(), address.pdc())
                    || ("wren".equalsIgnoreCase(expectedNpc)
                    && candidate.getPersistentDataContainer().has(key("wren_npc"),
                    PersistentDataType.STRING))) return candidate;
        }
        return null;
    }

    private void reconcileSeeds(NodePlan node, Map<String, Resolved> resolved,
                                Mode mode, boolean mutate, Set<String> observedIdentities,
                                Accumulator result) {
        JsonObject predicate = JsonParser.parseString(node.predicateJson()).getAsJsonObject();
        Map<String, JsonObject> components = components(predicate);
        List<Seed> seeds = new ArrayList<>();

        for (JsonObject component : components.values()) {
            JsonObject sourceItem = object(component, "source_item");
            if (sourceItem != null && !"A02".equals(node.node().nodeId())) {
                seeds.add(seed(node, string(component, "id"), string(component, "id"),
                        integer(component, "slot", 13), sourceItem));
            }
            JsonArray sourceItems = array(component, "source_items");
            if (sourceItems != null) {
                int index = 0;
                for (JsonElement element : sourceItems) {
                    seeds.add(seed(node, string(component, "id"), string(component, "id"),
                            index++ == 0 ? 11 : 15, element.getAsJsonObject()));
                }
            }
            JsonObject requiredItem = object(component, "required_item");
            if (requiredItem != null && "CHISELED_BOOKSHELF".equals(string(component, "block"))) {
                seeds.add(seed(node, string(component, "id"), string(component, "id"),
                        integer(component, "slot", 0), requiredItem));
            }
        }

        switch (node.node().nodeId()) {
            case "LS06" -> addLooseSeed(seeds, node, components, "orientation_key",
                    "orientation_key_source", 13);
            case "A09" -> addLooseSeed(seeds, node, components, "spool", "locker", 13);
            case "WR04" -> addLooseSeed(seeds, node, components, "bridge", "bridge_cache", 13);
            case "HS01" -> addLooseSeed(seeds, node, components, "cartridge", "source", 13);
            case "CW07" -> {
                addLooseSeed(seeds, node, components, "filter", "cache", 11);
                addLooseSeed(seeds, node, components, "receipt", "cache", 13);
                addLooseSeed(seeds, node, components, "drafts", "cache", 15);
            }
            case "BI02" -> addLooseSeeds(seeds, node, components,
                    List.of("old1", "old2", "old3", "dust"), "fragment_source", List.of(10, 12, 11, 13));
            case "BI03" -> addLooseSeeds(seeds, node, components,
                    List.of("feed", "water", "cover"), "instrument_source", List.of(13, 11, 12));
            case "BI06" -> addLooseSeeds(seeds, node, components,
                    List.of("reed", "water", "air"), "sample_source", List.of(15, 11, 13));
            case "KI02" -> addLooseSeed(seeds, node, components, "reed_knot", "reed_source", 13);
            case "KV01" -> addKv01AuditSlips(seeds, node);
            case "A03" -> addA03Lots(seeds, node, components.get("lots"));
            case "WR01" -> addIdSet(seeds, node, components.get("quotation_cards"),
                    "quotation_card_source", "WRITTEN_BOOK");
            case "WR02" -> addIdSet(seeds, node, components.get("packets"),
                    "packet_source", "PAPER");
            case "CW02" -> addCw02Samples(seeds, node, components.get("sample_set"));
            case "KV02" -> addKv02InputLot(seeds, node, components);
            default -> { }
        }

        for (Seed seed : seeds) {
            String evidenceId = itemPdc(seed.item(), "v5_evidence_id");
            if (!evidenceId.isBlank()) {
                boolean readableBook = seed.item().getType() == Material.WRITTEN_BOOK;
                if (readableBook && evidenceTexts.get(evidenceId) == null) {
                    result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                            "readable evidence book " + evidenceId
                                    + " has no packaged text authority"));
                    continue;
                }
                if (!readableBook && appearances.get(evidenceId) == null) {
                    result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                            "non-book evidence " + evidenceId
                                    + " has no packaged player-visible appearance"));
                    continue;
                }
            }
            applySeed(seed, resolved, mode, mutate, observedIdentities, result);
        }
        reconcileBi01InitialShelf(node, components, resolved, mode, mutate, observedIdentities, result);
    }

    private void applySeed(Seed seed, Map<String, Resolved> resolved, Mode mode, boolean mutate,
                           Set<String> observedIdentities, Accumulator result) {
        Resolved target = findResolved(resolved, seed.nodeId(), seed.targetComponent());
        if (target == null || !(target.location().getBlock().getState() instanceof InventoryHolder holder)) {
            result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                    "seed target " + seed.targetComponent() + " is not an inventory"));
            return;
        }
        Inventory inventory = holder.getInventory();
        if (seed.slot() < 0 || seed.slot() >= inventory.getSize()) {
            result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                    "seed slot " + seed.slot() + " is outside " + inventory.getSize()));
            return;
        }
        ItemStack current = inventory.getItem(seed.slot());
        String identity = identity(seed.item());
        if (current != null && !current.getType().isAir()) {
            if (identity.equals(identity(current))) {
                if (!evidenceBookMatches(current) || !appearanceMatches(current)) {
                    if (!mutate) {
                        result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                                "evidence text/appearance differs from packaged authority in slot "
                                        + seed.slot()));
                        return;
                    }
                    inventory.setItem(seed.slot(), seed.item().clone());
                    if (holder instanceof BlockState state) state.update(true, false);
                    result.repaired++;
                    return;
                }
                result.preserved++;
                return;
            }
            result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                    "refused to overwrite non-empty exposed slot " + seed.slot() + " in "
                            + seed.targetComponent()));
            return;
        }
        if (!identity.isBlank() && observedIdentities.contains(identity)) {
            if (mutate && mode == Mode.FRESH_INSTALL && removeLoadedBlockIdentity(identity)) {
                observedIdentities.remove(identity);
            } else {
            result.findings.add(new Finding(mode == Mode.FRESH_INSTALL ? Severity.BLOCKER : Severity.WARNING,
                    seed.nodeId(), seed.componentId(), "source item " + identity
                    + " already exists elsewhere; issued no duplicate"));
            return;
            }
        }
        if (mode == Mode.STATE_PRESERVING_REPAIR) {
            result.findings.add(new Finding(Severity.NOTE, seed.nodeId(), seed.componentId(),
                    (seed.uniqueArtifact() ? "unique artifact" : "evidence")
                            + " source is empty during repair; issued no duplicate without durable entitlement"));
            return;
        }
        if (!mutate) {
            result.findings.add(new Finding(Severity.BLOCKER, seed.nodeId(), seed.componentId(),
                    "source slot " + seed.slot() + " is missing " + identity));
            return;
        }
        inventory.setItem(seed.slot(), seed.item().clone());
        if (holder instanceof BlockState state) state.update(true, false);
        if (!identity.isBlank()) observedIdentities.add(identity);
        result.seeded++;
    }

    private void reconcileBi01InitialShelf(NodePlan node, Map<String, JsonObject> components,
                                           Map<String, Resolved> resolved, Mode mode, boolean mutate,
                                           Set<String> identities, Accumulator result) {
        if (!"BI01".equals(node.node().nodeId())) return;
        Resolved shelf = findResolved(resolved, "BI01", "residue_segments");
        if (shelf == null || !(shelf.location().getBlock().getState() instanceof InventoryHolder holder)) return;
        int[] slots = {1, 3, 5};
        for (int index = 0; index < slots.length; index++) {
            JsonObject item = new JsonObject();
            item.addProperty("material", "WRITTEN_BOOK");
            JsonObject pdc = new JsonObject();
            pdc.addProperty("v5_evidence_id", "bi01_wick_segment_" + (index + 1));
            item.add("pdc", pdc);
            Seed seed = seed(node, "residue_segments", "residue_segments", slots[index], item);
            applySeed(seed, resolved, mode, mutate, identities, result);
        }
    }

    private Seed seed(NodePlan node, String componentId, String target, int slot, JsonObject spec) {
        Map<String, String> mutablePdc = new LinkedHashMap<>(readPdc(spec));
        if ("KV01".equals(node.node().nodeId()) && spec.has("value")
                && spec.get("value").isJsonPrimitive()) {
            mutablePdc.put("v5_receipt_value", spec.get("value").getAsString());
        }
        Map<String, String> pdc = Map.copyOf(mutablePdc);
        String artifact = pdc.getOrDefault("v5_artifact_id", "");
        ItemStack item;
        if (!artifact.isBlank()) {
            item = CanonicalArtifactRegistry.create(artifact, null);
            if (item == null) item = createItem(Material.matchMaterial(string(spec, "material")), pdc,
                    node.node().nodeId(), componentId);
            else item = addItemTags(item, pdc, node.node().nodeId(), componentId);
        } else {
            Material material = Material.matchMaterial(string(spec, "material"));
            item = createItem(material, pdc, node.node().nodeId(), componentId);
        }
        return new Seed(node.node().nodeId(), componentId, target, slot, item, !artifact.isBlank());
    }

    private void addLooseSeed(List<Seed> seeds, NodePlan node, Map<String, JsonObject> components,
                              String itemId, String target, int slot) {
        JsonObject component = components.get(itemId);
        if (component != null) seeds.add(seed(node, itemId, target, slot, component));
    }

    private void addLooseSeeds(List<Seed> seeds, NodePlan node, Map<String, JsonObject> components,
                               List<String> itemIds, String target, List<Integer> slots) {
        for (int index = 0; index < itemIds.size(); index++) {
            addLooseSeed(seeds, node, components, itemIds.get(index), target, slots.get(index));
        }
    }

    private void addRequiredSeeds(List<Seed> seeds, NodePlan node, JsonObject component,
                                  String target, List<Integer> slots) {
        JsonArray required = component == null ? null : array(component, "required");
        if (required == null) return;
        for (int index = 0; index < required.size(); index++) {
            seeds.add(seed(node, string(component, "id") + "_required_" + index, target,
                    slots.get(index % slots.size()), required.get(index).getAsJsonObject()));
        }
    }

    private void addKv01AuditSlips(List<Seed> seeds, NodePlan node) {
        int[] shuffledSlots = {17, 2, 23, 8, 14, 0, 20, 5, 11, 26,
                7, 19, 1, 24, 9, 16, 3, 22, 12, 6};
        int cursor = 0;
        for (String field : List.of("cloth_missing", "charcoal_missing")) {
            for (int value = 0; value <= 9; value++) {
                JsonObject spec = new JsonObject();
                spec.addProperty("material", "PAPER");
                JsonObject pdc = new JsonObject();
                String shortField = field.startsWith("cloth") ? "cloth" : "charcoal";
                pdc.addProperty("v5_evidence_id", "kv01_" + shortField + "_slip_" + value);
                pdc.addProperty("v5_audit_field", field);
                pdc.addProperty("v5_audit_value", Integer.toString(value));
                spec.add("pdc", pdc);
                seeds.add(seed(node, "audit_slip_" + shortField + '_' + value,
                        "audit_slip_source", shuffledSlots[cursor++], spec));
            }
        }
    }

    private void addA03Lots(List<Seed> seeds, NodePlan node, JsonObject component) {
        JsonArray required = component == null ? null : array(component, "required");
        if (required == null) return;
        int[] shuffled = {7, 1, 10, 4, 0, 8, 3, 11, 5, 2, 9, 6};
        for (int index = 0; index < required.size(); index++) {
            JsonObject source = required.get(index).getAsJsonObject();
            JsonObject spec = new JsonObject();
            spec.addProperty("material", string(source, "material"));
            JsonObject pdc = new JsonObject();
            pdc.addProperty("v5_evidence_id", string(source, "id"));
            pdc.addProperty("v5_lot_category", string(source, "category"));
            pdc.addProperty("v5_receipt_number", source.get("receipt").getAsString());
            spec.add("pdc", pdc);
            seeds.add(seed(node, "lots_" + index, "lot_staging", shuffled[index], spec));
        }
    }

    private void addIdSet(List<Seed> seeds, NodePlan node, JsonObject component,
                          String target, String material) {
        JsonArray ids = component == null ? null : array(component, "required_ids");
        if (ids == null) return;
        for (int index = 0; index < ids.size(); index++) {
            JsonObject spec = new JsonObject();
            spec.addProperty("material", material);
            JsonObject pdc = new JsonObject();
            pdc.addProperty("v5_evidence_id", ids.get(index).getAsString());
            spec.add("pdc", pdc);
            seeds.add(seed(node, string(component, "id") + "_" + index, target, 10 + index, spec));
        }
    }

    private void addCw02Samples(List<Seed> seeds, NodePlan node, JsonObject component) {
        JsonArray ids = component == null ? null : array(component, "required_ids");
        if (ids == null) return;
        int[] shuffled = {5, 0, 7, 2, 4, 1, 6, 3};
        for (int index = 0; index < ids.size(); index++) {
            String id = ids.get(index).getAsString();
            JsonObject spec = new JsonObject();
            spec.addProperty("material", "POTION");
            JsonObject pdc = new JsonObject();
            pdc.addProperty("v5_evidence_id", id);
            pdc.addProperty("v5_intake", id.substring(0, 1));
            pdc.addProperty("v5_cycle", id.substring(1, 2));
            pdc.addProperty("v5_depth", id.endsWith("T") ? "TOP" : "LOWER");
            spec.add("pdc", pdc);
            seeds.add(seed(node, "sample_set_" + index, "sample_source", shuffled[index], spec));
        }
    }

    private void addKv02InputLot(List<Seed> seeds, NodePlan node,
                                 Map<String, JsonObject> components) {
        List<LotItem> manifest = new ArrayList<>();
        for (String target : List.of("cistern", "public_heat", "private_heat", "condemned")) {
            JsonObject component = components.get(target);
            JsonObject counts = component == null ? null : object(component, "required_counts");
            if (counts == null) continue;
            for (Map.Entry<String, JsonElement> entry : counts.entrySet()) {
                int count = entry.getValue().getAsInt();
                for (int index = 1; index <= count; index++) {
                    JsonObject spec = new JsonObject();
                    spec.addProperty("material", entry.getKey());
                    JsonObject pdc = new JsonObject();
                    pdc.addProperty("v5_evidence_id", String.format(Locale.ROOT,
                            "kv02_return_%02d", manifest.size() + 1));
                    pdc.addProperty("v5_sort_class", target);
                    spec.add("pdc", pdc);
                    manifest.add(new LotItem("input_lot_" + manifest.size(), spec));
                }
            }
        }
        for (String sourceId : List.of("input_lot_a", "input_lot_b")) {
            JsonObject source = components.get(sourceId);
            JsonObject slice = source == null ? null : object(source, "manifest_slice");
            int start = integer(slice, "start", -1);
            int count = integer(slice, "count", -1);
            if (start < 0 || count < 1 || start + count > manifest.size()) continue;
            for (int localSlot = 0; localSlot < count; localSlot++) {
                LotItem item = manifest.get(start + localSlot);
                seeds.add(seed(node, item.componentId(), sourceId, localSlot, item.spec()));
            }
        }
    }

    private Set<String> collectLoadedIdentities() {
        Set<String> identities = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            collect(player.getInventory(), identities);
            collect(player.getEnderChest(), identities);
        }
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Item item : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
                addIdentity(item.getItemStack(), identities);
            }
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof InventoryHolder holder) collect(holder.getInventory(), identities);
                }
            }
        }
        return identities;
    }

    private boolean removeLoadedBlockIdentity(String identity) {
        boolean removed = false;
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    BlockState live = state.getBlock().getState();
                    if (!(live instanceof InventoryHolder holder)) continue;
                    Inventory inventory = holder.getInventory();
                    boolean changed = false;
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        if (!identity.equals(identity(inventory.getItem(slot)))) continue;
                        inventory.setItem(slot, null);
                        changed = true;
                        removed = true;
                    }
                    if (changed) live.update(true, false);
                }
            }
        }
        return removed;
    }

    private static void collect(Inventory inventory, Set<String> identities) {
        if (inventory == null) return;
        for (ItemStack item : inventory.getContents()) addIdentity(item, identities);
    }

    private static void addIdentity(ItemStack item, Set<String> identities) {
        String id = identity(item);
        if (!id.isBlank()) identities.add(id);
    }

    private static String identity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (String key : UNIQUE_ID_KEYS) {
            String value = pdc.get(new NamespacedKey("observance", key), PersistentDataType.STRING);
            if (value != null && !value.isBlank()) {
                return (key.equals("v5_artifact_id") || key.equals("artifact_id")
                        ? "artifact" : key) + "=" + value;
            }
        }
        return "";
    }

    private ItemStack createFramedItem(Material material, Address address, World world) {
        ItemStack item = createItem(material, address.pdc(), address.nodeId(), address.componentId());
        return material == Material.FILLED_MAP ? bindMapArt(item, address, world) : item;
    }

    private ItemStack bindMapArt(ItemStack original, Address address, World world) {
        V5MapArtAuthority.Entry entry = mapArt.byComponent(address.nodeId(), address.componentId());
        if (entry == null || entry.image() == null || world == null) return original;
        ItemStack item = original.clone();
        if (!(item.getItemMeta() instanceof MapMeta meta)) return item;
        // Paper 1.21.11 throws from getMapView() when a newly-created FILLED_MAP has not been
        // assigned an id yet. hasMapView() is the API contract for that state.
        MapView view = meta.hasMapView() ? meta.getMapView() : Bukkit.createMap(world);
        configureMapView(view, entry);
        meta.setMapView(view);
        set(meta.getPersistentDataContainer(), "v5_map_art_id", entry.id());
        set(meta.getPersistentDataContainer(), "v5_map_art_sha256", entry.sha256());
        item.setItemMeta(meta);
        return item;
    }

    private boolean rebindExistingMap(ItemStack item, Address address) {
        V5MapArtAuthority.Entry entry = mapArt.byComponent(address.nodeId(), address.componentId());
        if (entry == null || entry.image() == null || item == null
                || !(item.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) return false;
        if (!entry.id().equals(itemPdc(item, "v5_map_art_id"))
                || !entry.sha256().equals(itemPdc(item, "v5_map_art_sha256"))) return false;
        configureMapView(meta.getMapView(), entry);
        item.setItemMeta(meta);
        return true;
    }

    private void configureMapView(MapView view, V5MapArtAuthority.Entry entry) {
        for (MapRenderer renderer : new ArrayList<>(view.getRenderers())) view.removeRenderer(renderer);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.setLocked(true);
        view.addRenderer(new ImmutableMapArtRenderer(entry));
    }

    private void auditMapArt(ItemStack item, Address address, Accumulator result) {
        V5MapArtAuthority.Entry entry = mapArt.byComponent(address.nodeId(), address.componentId());
        if (entry == null) {
            result.block(address, "map-art binding is missing");
            return;
        }
        if (item == null || !(item.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            result.block(address, "filled map has no bound MapView");
            return;
        }
        if (!entry.id().equals(itemPdc(item, "v5_map_art_id"))
                || !entry.sha256().equals(itemPdc(item, "v5_map_art_sha256"))) {
            result.block(address, "filled map ID/SHA differs from packaged pixels");
        }
        MapView view = meta.getMapView();
        if (view.isTrackingPosition() || view.isUnlimitedTracking() || !view.isLocked()) {
            result.block(address, "map tracking/locking contract is unsafe");
        }
        boolean exactRenderer = view.getRenderers().size() == 1
                && view.getRenderers().get(0) instanceof ImmutableMapArtRenderer renderer
                && renderer.id.equals(entry.id()) && renderer.sha256.equals(entry.sha256());
        if (!exactRenderer) result.block(address, "immutable packaged map renderer is not attached");
    }

    private static final class ImmutableMapArtRenderer extends MapRenderer {
        private final String id;
        private final String sha256;
        private final BufferedImage image;

        private ImmutableMapArtRenderer(V5MapArtAuthority.Entry entry) {
            super(false);
            this.id = entry.id();
            this.sha256 = entry.sha256();
            this.image = entry.image();
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            canvas.drawImage(0, 0, image);
        }
    }

    private ItemStack createItem(Material material, Map<String, String> pdc,
                                 String nodeId, String componentId) {
        Material safe = material == null || material.isAir() ? Material.PAPER : material;
        ItemStack item = new ItemStack(safe, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String evidenceId = pdc.getOrDefault("v5_evidence_id", "");
        V5EvidenceItemTextAuthority.Entry evidence = evidenceTexts.get(evidenceId);
        V5EvidenceItemAppearanceAuthority.Entry appearance = appearances.get(evidenceId);
        String label = (evidence != null ? evidence.title()
                : appearance != null ? appearance.title() : preferredLabel(pdc, componentId))
                .replace('_', ' ').trim();
        meta.displayName(Component.text(label.isBlank() ? componentId : label)
                .color(NamedTextColor.GRAY));
        applyVisibleAppearance(meta, appearance);
        if (meta instanceof BookMeta book) {
            if (evidence != null) {
                book.setTitle(evidence.title());
                book.setAuthor(evidence.author());
                book.pages(evidence.pages().stream()
                        .map(page -> (Component) Component.text(page)).toList());
            } else {
                book.setTitle(truncate(label.isBlank() ? componentId : label, 32));
                book.setAuthor("The Record");
                if (book.getPageCount() == 0) {
                    book.addPages(Component.text("Filed evidence: " + label + "."));
                }
            }
        }
        apply(meta.getPersistentDataContainer(), pdc);
        setIdentityTags(meta.getPersistentDataContainer(), nodeId, componentId);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyVisibleAppearance(ItemMeta meta,
                                               V5EvidenceItemAppearanceAuthority.Entry appearance) {
        if (meta == null || appearance == null) return;
        meta.displayName(Component.text(appearance.title()).color(NamedTextColor.GRAY));
        meta.lore(appearance.lore().stream().map(line -> (Component) Component.text(line)
                .color(NamedTextColor.DARK_GRAY)).toList());
    }

    private ItemStack addItemTags(ItemStack original, Map<String, String> pdc,
                                  String nodeId, String componentId) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        apply(meta.getPersistentDataContainer(), pdc);
        setIdentityTags(meta.getPersistentDataContainer(), nodeId, componentId);
        String evidenceId = meta.getPersistentDataContainer().get(
                key("v5_evidence_id"), PersistentDataType.STRING);
        applyVisibleAppearance(meta, appearances.get(evidenceId));
        item.setItemMeta(meta);
        return item;
    }

    private static String preferredLabel(Map<String, String> pdc, String fallback) {
        for (String key : List.of("v5_artifact_id", "v5_evidence_id", "v5_receipt_id",
                "v5_control_id", "v5_restoration_id", "v5_mark_value")) {
            String value = pdc.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return fallback == null ? "" : fallback;
    }

    private static String itemPdc(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return "";
        String value = item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey("observance", key), PersistentDataType.STRING);
        return value == null ? "" : value;
    }

    private boolean evidenceBookMatches(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return true;
        String id = itemPdc(item, "v5_evidence_id");
        if (id.isBlank()) return true;
        V5EvidenceItemTextAuthority.Entry entry = evidenceTexts.get(id);
        return entry != null && evidenceBookMatches(item, entry);
    }

    private boolean appearanceMatches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = itemPdc(item, "v5_evidence_id");
        V5EvidenceItemAppearanceAuthority.Entry entry = appearances.get(id);
        if (entry == null) return true;
        ItemMeta meta = item.getItemMeta();
        String title = meta.hasDisplayName() && meta.displayName() != null
                ? PlainTextComponentSerializer.plainText().serialize(meta.displayName()) : "";
        if (!entry.title().equals(title)) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList()
                .equals(entry.lore());
    }

    private static boolean evidenceBookMatches(ItemStack item,
                                               V5EvidenceItemTextAuthority.Entry entry) {
        if (item == null || !(item.getItemMeta() instanceof BookMeta meta)) return false;
        if (!entry.title().equals(meta.getTitle()) || !entry.author().equals(meta.getAuthor())) return false;
        List<String> pages = meta.pages().stream().map(PlainTextComponentSerializer.plainText()::serialize)
                .toList();
        return pages.equals(entry.pages());
    }

    private static ItemStack canonicalBook(ItemStack original, V5AuthorityManifest.BookEntry entry) {
        ItemStack item = original.clone();
        if (!(item.getItemMeta() instanceof BookMeta meta)) return item;
        meta.setTitle(entry.title());
        meta.setAuthor(entry.author());
        meta.pages(entry.pages().stream().map(page -> (Component) Component.text(page)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static boolean bookMatches(ItemStack item, V5AuthorityManifest.BookEntry entry) {
        if (item == null || !(item.getItemMeta() instanceof BookMeta meta)) return false;
        if (!entry.title().equals(meta.getTitle()) || !entry.author().equals(meta.getAuthor())) return false;
        List<String> pages = meta.pages().stream().map(PlainTextComponentSerializer.plainText()::serialize)
                .toList();
        return pages.equals(entry.pages());
    }

    private void apply(PersistentDataContainer target, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) set(target, entry.getKey(), entry.getValue());
        String artifact = values.get("v5_artifact_id");
        if (artifact != null) set(target, "artifact_id", artifact);
        String book = values.get("v5_book_id");
        if (book != null) set(target, "book_id", book);
    }

    private void tag(PersistentDataHolder holder, Address address) {
        setIdentityTags(holder.getPersistentDataContainer(), address.nodeId(), address.componentId());
        apply(holder.getPersistentDataContainer(), address.pdc());
    }

    private void setIdentityTags(PersistentDataContainer pdc, String nodeId, String componentId) {
        String existingNode = pdc.get(key("v5_node_id"), PersistentDataType.STRING);
        if (existingNode == null) set(pdc, "v5_node_id", nodeId);
        String existingComponent = pdc.get(key("v5_component_id"), PersistentDataType.STRING);
        if (existingComponent == null) set(pdc, "v5_component_id", componentId);
        appendCsv(pdc, "v5_node_ids", nodeId);
        appendCsv(pdc, "v5_component_ids", nodeId + ":" + componentId);
    }

    private void appendCsv(PersistentDataContainer pdc, String key, String value) {
        String existing = pdc.get(key(key), PersistentDataType.STRING);
        Set<String> values = new LinkedHashSet<>();
        if (existing != null && !existing.isBlank()) values.addAll(List.of(existing.split(",")));
        values.add(value);
        set(pdc, key, String.join(",", values));
    }

    private void set(PersistentDataContainer pdc, String key, String value) {
        if (key == null || key.isBlank() || value == null) return;
        pdc.set(key(key), PersistentDataType.STRING, value);
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value.toLowerCase(Locale.ROOT));
    }

    private void auditTags(PersistentDataContainer pdc, Address address, Accumulator result) {
        String nodes = pdc.get(key("v5_node_ids"), PersistentDataType.STRING);
        String components = pdc.get(key("v5_component_ids"), PersistentDataType.STRING);
        if (nodes == null || !List.of(nodes.split(",")).contains(address.nodeId())) {
            result.block(address, "missing v5_node_ids tag");
        }
        if (components == null || !List.of(components.split(",")).contains(
                address.nodeId() + ":" + address.componentId())) {
            result.block(address, "missing v5_component_ids tag");
        }
        for (Map.Entry<String, String> entry : address.pdc().entrySet()) {
            String actual = pdc.get(key(entry.getKey()), PersistentDataType.STRING);
            if (!entry.getValue().equals(actual)) result.block(address,
                    "PDC " + entry.getKey() + "=" + actual + ", expected " + entry.getValue());
        }
    }

    private boolean tagsMatch(PersistentDataContainer pdc, Address address) {
        String nodes = pdc.get(key("v5_node_ids"), PersistentDataType.STRING);
        String components = pdc.get(key("v5_component_ids"), PersistentDataType.STRING);
        if (nodes == null || !List.of(nodes.split(",")).contains(address.nodeId())) return false;
        if (components == null || !List.of(components.split(",")).contains(
                address.nodeId() + ":" + address.componentId())) return false;
        return pdcMatches(pdc, address.pdc()) || address.pdc().isEmpty();
    }

    private boolean itemTagsMatch(ItemStack item, Address address) {
        return item != null && item.hasItemMeta()
                && tagsMatch(item.getItemMeta().getPersistentDataContainer(), address);
    }

    private boolean isReorderableFrame(Address address) {
        return FRESH_FRAME_PERMUTATIONS.containsKey(
                address.nodeId() + ':' + address.componentId());
    }

    private List<Address> reorderableFrameSet(Address address) {
        if (!isReorderableFrame(address)) return List.of();
        return catalog.addresses().stream().filter(candidate ->
                candidate.kind() == AddressKind.ITEM_FRAME
                        && candidate.nodeId().equals(address.nodeId())
                        && candidate.componentId().equals(address.componentId())).toList();
    }

    private Address freshFrameSource(Address target) {
        List<Address> set = reorderableFrameSet(target);
        List<Integer> permutation = FRESH_FRAME_PERMUTATIONS.get(
                target.nodeId() + ':' + target.componentId());
        int targetIndex = set.indexOf(target);
        if (permutation == null || permutation.size() != set.size()
                || targetIndex < 0 || permutation.get(targetIndex) < 0
                || permutation.get(targetIndex) >= set.size()) return null;
        return set.get(permutation.get(targetIndex));
    }

    private boolean frameItemBelongsToSet(ItemStack item, Address frameAddress) {
        return frameItemIdentity(item, frameAddress) != null;
    }

    private Address frameItemIdentity(ItemStack item, Address frameAddress) {
        Address found = null;
        for (Address candidate : reorderableFrameSet(frameAddress)) {
            Material material = Material.matchMaterial(candidate.material());
            if (material == null || item == null || item.getType() != material
                    || !itemTagsMatch(item, candidate)) continue;
            if (found != null) return null;
            found = candidate;
        }
        return found;
    }

    private void inspectReorderableFrameSets(List<Resolved> resolved, Mode mode,
                                             Accumulator result) {
        Map<String, List<Resolved>> groups = new LinkedHashMap<>();
        for (Resolved entry : resolved) {
            Address address = entry.address();
            if (isReorderableFrame(address)) {
                groups.computeIfAbsent(address.nodeId() + ':' + address.componentId(),
                        ignored -> new ArrayList<>()).add(entry);
            }
        }
        for (Map.Entry<String, List<Resolved>> group : groups.entrySet()) {
            Set<String> seen = new LinkedHashSet<>();
            List<Resolved> frames = group.getValue();
            for (Resolved entry : frames) {
                Address address = entry.address();
                Location plane = frameEntityPlane(entry.location(),
                        itemFrameFacing(address, entry.anchor()));
                ItemFrame exact = plane.getWorld().getNearbyEntitiesByType(
                                ItemFrame.class, plane, 0.35).stream()
                        .filter(frame -> tagged(frame, address))
                        .min(java.util.Comparator.comparingDouble(frame ->
                                frame.getLocation().distanceSquared(plane))).orElse(null);
                if (exact == null) continue; // per-address audit reports the exact fault
                Address identity = frameItemIdentity(exact.getItem(), address);
                if (identity == null) continue;
                String identityKey = identity.pdc().entrySet().stream()
                        .map(item -> item.getKey() + '=' + item.getValue())
                        .sorted().reduce("", (left, right) -> left + '|' + right);
                if (!seen.add(identityKey)) {
                    result.block(address, "movable frame set contains a duplicate piece");
                }
                if (mode == Mode.FRESH_INSTALL) {
                    Address expected = freshFrameSource(address);
                    if (expected == null || !expected.equals(identity)) {
                        result.block(address, "fresh frame state differs from deterministic unsolved permutation");
                    }
                }
            }
            if (seen.size() != frames.size() && !frames.isEmpty()) {
                result.block(frames.getFirst().address(), "movable frame set is incomplete: "
                        + seen.size() + '/' + frames.size() + " unique pieces present");
            }
        }
    }

    private boolean mapArtMatches(ItemStack item, Address address) {
        V5MapArtAuthority.Entry entry = mapArt.byComponent(address.nodeId(), address.componentId());
        if (entry == null || item == null || !(item.getItemMeta() instanceof MapMeta meta)
                || !meta.hasMapView()) return false;
        MapView view = meta.getMapView();
        return entry.id().equals(itemPdc(item, "v5_map_art_id"))
                && entry.sha256().equals(itemPdc(item, "v5_map_art_sha256"))
                && !view.isTrackingPosition() && !view.isUnlimitedTracking() && view.isLocked()
                && view.getRenderers().size() == 1
                && view.getRenderers().get(0) instanceof ImmutableMapArtRenderer renderer
                && renderer.id.equals(entry.id()) && renderer.sha256.equals(entry.sha256());
    }

    private void inspectAppearanceCoverage(Accumulator result) {
        for (Address address : catalog.addresses()) {
            String evidenceId = address.pdc().getOrDefault("v5_evidence_id", "");
            if (evidenceId.isBlank()) continue;
            Material material = Material.matchMaterial(address.material());
            boolean readableBook = material == Material.WRITTEN_BOOK;
            boolean renderedMap = material == Material.FILLED_MAP
                    && mapArt.byComponent(address.nodeId(), address.componentId()) != null;
            if (readableBook ? evidenceTexts.get(evidenceId) == null
                    : !renderedMap && appearances.get(evidenceId) == null) {
                result.block(address, "evidence " + evidenceId
                        + " lacks exact player-visible book/map/appearance authority");
            }
        }
    }

    private Map<String, String> bindingMetadata(Address address) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("kind", address.kind().name());
        metadata.put("instance", address.instanceId());
        // Make authored selector/station/control values available without asking mechanics code
        // to reverse-engineer namespaced PDC. expectedPdc remains the verification authority.
        metadata.putAll(address.pdc());
        String selection = address.pdc().get("v5_selection");
        if (selection != null && !selection.isBlank()) {
            metadata.put("selector_value", selection);
        }
        if (!address.material().isBlank()) metadata.put("material", address.material());
        if (address.synthetic()) metadata.put("synthetic", "true");
        if (address.requiredRotation() != null) {
            metadata.put("required_rotation", Integer.toString(address.requiredRotation()));
        }
        if (address.resetRotation() != null) {
            metadata.put("reset_rotation", Integer.toString(address.resetRotation()));
        }
        if (address.inventorySlot() != null) {
            metadata.put("slot", Integer.toString(address.inventorySlot()));
        }
        if (!address.bookId().isBlank()) metadata.put("book_id", address.bookId());
        Material material = Material.matchMaterial(address.material());
        // Buttons are not automatically solve triggers.  AR07 false_m is an authored event input;
        // only the separate synthetic reader control bears v5_handle_node and evaluates the node.
        boolean trigger = address.pdc().containsKey("v5_handle_node")
                || material == Material.LEVER;
        if (trigger) {
            metadata.put("trigger", "true");
            metadata.put("handle_component", address.componentId());
        }
        return Map.copyOf(metadata);
    }

    private void preflightExisting(BukkitFixtureIndex index, PendingBlock pending,
                                   Accumulator result) {
        List<BukkitFixtureIndex.Binding> existing = index.bindings(
                pending.address().nodeId(), pending.address().componentId()).stream()
                .filter(binding -> binding.ordinal() == pending.ordinal()
                        && binding.kind() == BukkitFixtureIndex.BindingKind.BLOCK).toList();
        if (existing.isEmpty()) return;
        BukkitFixtureIndex.Binding binding = existing.get(0);
        if (existing.size() != 1 || !binding.worldId().equals(pending.block().getWorld().getUID())
                || binding.x() != pending.block().getX() || binding.y() != pending.block().getY()
                || binding.z() != pending.block().getZ()) {
            result.block(pending.address(), "existing mechanics block binding conflicts with fixture");
        }
    }

    private void preflightExisting(BukkitFixtureIndex index, PendingEntity pending,
                                   Accumulator result) {
        List<BukkitFixtureIndex.Binding> existing = index.bindings(
                pending.address().nodeId(), pending.address().componentId()).stream()
                .filter(binding -> binding.ordinal() == pending.ordinal()
                        && binding.kind() == BukkitFixtureIndex.BindingKind.ENTITY).toList();
        if (existing.isEmpty()) return;
        BukkitFixtureIndex.Binding binding = existing.get(0);
        if (existing.size() != 1 || binding.entityId().isEmpty()
                || !binding.entityId().get().equals(pending.entity().getUniqueId())) {
            result.block(pending.address(), "existing mechanics entity binding conflicts with fixture");
        }
    }

    private void auditItemPdc(ItemStack item, Address address, Accumulator result) {
        if (item == null || !item.hasItemMeta()) {
            result.block(address, "component item has no metadata/PDC");
            return;
        }
        auditTags(item.getItemMeta().getPersistentDataContainer(), address, result);
    }

    private void ensureTaggedMarker(Resolved resolved, boolean interactive,
                                    boolean mutate, Accumulator result) {
        List<Entity> matches = taggedMarkers(resolved);
        if (!matches.isEmpty()) {
            if (matches.size() > 1) {
                if (!mutate) {
                    result.block(resolved.address(), "duplicate tag marker entities are present");
                } else {
                    for (int index = 1; index < matches.size(); index++) matches.get(index).remove();
                    result.repaired += matches.size() - 1;
                }
            }
            return;
        }
        if (!mutate) {
            result.block(resolved.address(), "tag marker entity is missing");
            return;
        }
        Location at = resolved.location().clone().add(0.5, 0.5, 0.5);
        Entity marker;
        if (interactive) {
            Interaction interaction = at.getWorld().spawn(at, Interaction.class);
            interaction.setInteractionWidth(0.8f);
            interaction.setInteractionHeight(0.8f);
            interaction.setResponsive(true);
            marker = interaction;
        } else {
            marker = at.getWorld().spawn(at, Marker.class);
        }
        marker.setPersistent(true);
        tag(marker, resolved.address());
        result.placed++;
    }

    private boolean hasTaggedMarker(Resolved resolved) {
        return !taggedMarkers(resolved).isEmpty();
    }

    private PersistentDataHolder taggedMarker(Resolved resolved) {
        List<Entity> markers = taggedMarkers(resolved);
        return markers.isEmpty() ? null : markers.get(0);
    }

    private List<Entity> taggedMarkers(Resolved resolved) {
        Location at = resolved.location().clone().add(0.5, 0.5, 0.5);
        List<Entity> result = new ArrayList<>();
        for (Entity entity : at.getWorld().getNearbyEntities(at, 0.55, 0.55, 0.55)) {
            if ((entity instanceof Interaction || entity instanceof Marker)
                    && tagged(entity, resolved.address())) result.add(entity);
        }
        return result;
    }

    private boolean pdcMatches(PersistentDataContainer pdc, Map<String, String> expected) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actual = pdc.get(key(entry.getKey()), PersistentDataType.STRING);
            if (!entry.getValue().equals(actual)) return false;
        }
        return !expected.isEmpty();
    }

    private boolean tagged(PersistentDataHolder holder, Address address) {
        String components = holder.getPersistentDataContainer().get(
                key("v5_component_ids"), PersistentDataType.STRING);
        return components != null && List.of(components.split(",")).contains(
                address.nodeId() + ":" + address.componentId());
    }

    private static ItemFrame selectTagged(List<ItemFrame> frames, Address address) {
        for (ItemFrame frame : frames) {
            String components = frame.getPersistentDataContainer().get(
                    new NamespacedKey("observance", "v5_component_ids"), PersistentDataType.STRING);
            if (components != null && List.of(components.split(",")).contains(
                    address.nodeId() + ":" + address.componentId())) return frame;
        }
        return null;
    }

    private ItemFrame selectPdcMatching(List<ItemFrame> frames, Address address) {
        ItemFrame match = null;
        for (ItemFrame frame : frames) {
            if (!itemMatchesPdc(frame.getItem(), address.pdc())) continue;
            if (match != null) return null;
            match = frame;
        }
        return match;
    }

    private List<ItemFrame> matchingFramesNear(Resolved resolved, Address address, double radius) {
        Location center = resolved.location().clone().add(0.5, 0.5, 0.5);
        List<ItemFrame> result = new ArrayList<>();
        for (ItemFrame frame : center.getWorld().getNearbyEntitiesByType(ItemFrame.class, center, radius)) {
            boolean exactItem = itemMatchesPdc(frame.getItem(), address.pdc());
            // Repeated frame components share node/component tags. Their per-piece item PDC is the
            // discriminator; otherwise the neighboring frame is mistaken for a displaced copy.
            if (exactItem || (address.pdc().isEmpty() && tagged(frame, address))) result.add(frame);
        }
        return result;
    }

    private static boolean itemMatchesPdc(ItemStack item, Map<String, String> expected) {
        if (expected.isEmpty() || item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actual = pdc.get(new NamespacedKey("observance", entry.getKey().toLowerCase(Locale.ROOT)),
                    PersistentDataType.STRING);
            if (!entry.getValue().equals(actual)) return false;
        }
        return true;
    }

    private boolean isInstallerOwned(PersistentDataHolder holder, ItemStack item) {
        String nodes = holder.getPersistentDataContainer().get(key("v5_node_ids"), PersistentDataType.STRING);
        if (nodes != null && !nodes.isBlank()) return true;
        if (item != null && item.hasItemMeta()) {
            String itemNodes = item.getItemMeta().getPersistentDataContainer().get(
                    key("v5_node_ids"), PersistentDataType.STRING);
            return itemNodes != null && !itemNodes.isBlank();
        }
        return false;
    }

    private Anchor resolveAnchor(String siteId, Location mouth) {
        if (mouth != null && mouth.getWorld() != null) {
            DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(siteId);
            if (fixture != null) return new Anchor(mouth.clone().add(fixture.x(), fixture.y(), fixture.z()),
                    cardinal(fixture.front()), fixture.radius(), fixture.verticalRadius());
            for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
                if (station.id().equals(siteId)) return new Anchor(
                        mouth.clone().add(station.x(), station.y(), station.z()),
                        cardinal(station.front()), 8, 6);
            }
        }
        Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
        Location location = site == null ? null : site.location();
        if (location == null || location.getWorld() == null) return null;
        Cardinal front = switch (siteId) {
            case "release_record" -> Cardinal.SOUTH;
            case "npc_wren_anchor" -> Cardinal.WEST;
            default -> externalFront(siteId, location);
        };
        if (front == null) return null;
        return new Anchor(location.getBlock().getLocation(), front,
                Math.max(2, site.radius()), Math.max(3, site.verticalRadius()));
    }

    private Cardinal externalFront(String siteId, Location location) {
        Location center = location.getBlock().getLocation().add(0.5, 0.25, 0.5);
        for (Marker marker : location.getWorld().getNearbyEntitiesByType(Marker.class, center, 0.7)) {
            String bound = marker.getPersistentDataContainer().get(key("v5_site_anchor"),
                    PersistentDataType.STRING);
            String front = marker.getPersistentDataContainer().get(key("v5_fixture_front"),
                    PersistentDataType.STRING);
            if (siteId.equals(bound) && front != null && !front.isBlank()) return cardinal(front);
        }
        if (!siteId.startsWith("unlit_house_")) return Cardinal.NORTH;
        Block base = location.getBlock();
        if ("unlit_house_lamp".equals(siteId) && base.getBlockData() instanceof Directional directional) {
            return cardinal(directional.getFacing().name());
        }
        Material forwardMarker = switch (siteId) {
            case "unlit_house_cairn" -> Material.COBBLED_DEEPSLATE_SLAB;
            case "unlit_house_coop" -> Material.LIGHT_GRAY_CARPET;
            case "unlit_house_well", "unlit_house_watch" -> Material.LECTERN;
            case "unlit_house_warm" -> Material.RED_CONCRETE;
            case "unlit_house_threshold" -> Material.POLISHED_BLACKSTONE_PRESSURE_PLATE;
            default -> null;
        };
        if (forwardMarker == null) return null;
        Cardinal found = null;
        for (Cardinal candidate : Cardinal.values()) {
            if (base.getRelative(face(candidate)).getType() != forwardMarker) continue;
            if (found != null) return null;
            found = candidate;
        }
        return found;
    }

    private static Location toWorld(Anchor anchor, Address address) {
        BlockPos pos = FixtureTransform.toWorld(blockPos(anchor.location()),
                anchor.front(), address.offset());
        return new Location(anchor.location().getWorld(), pos.x(), pos.y(), pos.z());
    }

    private static BlockPos blockPos(Location location) {
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static Cardinal cardinal(String value) {
        try {
            return Cardinal.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Cardinal.NORTH;
        }
    }

    private static BlockFace face(Cardinal cardinal) {
        return BlockFace.valueOf(cardinal.name());
    }

    private static BlockFace cardinalFace(BlockFace facing) {
        return switch (facing) {
            case NORTH, EAST, SOUTH, WEST -> facing;
            default -> BlockFace.NORTH;
        };
    }

    private static void orientBlock(Block block, Cardinal front) {
        if (block.getBlockData() instanceof Directional directional
                && directional.getFaces().contains(face(front))) {
            directional.setFacing(face(front));
            block.setBlockData(directional, false);
        } else if (block.getBlockData() instanceof Rotatable rotatable) {
            rotatable.setRotation(face(front));
            block.setBlockData(rotatable, false);
        }
    }

    private BlockFace itemFrameFacing(Address address, Anchor anchor) {
        BlockFace authoredFront = face(anchor.front());
        try {
            JsonObject component = JsonParser.parseString(address.rawComponentJson()).getAsJsonObject();
            if ("reverse_face".equalsIgnoreCase(string(component, "must_be_viewed_from"))) {
                return authoredFront.getOppositeFace();
            }
        } catch (RuntimeException ignored) {
            // Static catalog validation reports malformed component JSON before world mutation.
        }
        // ITEM_FRAME offsets name their backing block, but an adjacent authored component can
        // legitimately occupy the nominal hanging cell in a dense shared fixture. Mount on the
        // reverse face in that case; the matching interaction audit uses this same facing.
        LocalOffset at = address.offset();
        boolean authoredFrontOccupied = catalog.addresses().stream().anyMatch(candidate ->
                candidate != address && candidate.siteId().equals(address.siteId())
                        && (candidate.kind() == AddressKind.BLOCK
                        || candidate.kind() == AddressKind.ITEM_FRAME
                        || candidate.kind() == AddressKind.ITEM_DISPLAY)
                        && candidate.offset().right() == at.right()
                        && candidate.offset().up() == at.up()
                        && candidate.offset().front() == at.front() + 1);
        if (authoredFrontOccupied) return authoredFront.getOppositeFace();
        return authoredFront;
    }

    private static Location frameEntityPlane(Location block, BlockFace facing) {
        FixtureTransform.FramePlane plane = FixtureTransform.framePlane(
                blockPos(block), cardinal(facing.name()));
        return new Location(block.getWorld(), plane.x(), plane.y(), plane.z());
    }

    private static Location frameSpawnAnchor(Location supportingBlock, BlockFace facing) {
        FixtureTransform.FrameSpawnAnchor anchor = FixtureTransform.frameSpawnAnchor(
                blockPos(supportingBlock), cardinal(facing.name()));
        return new Location(supportingBlock.getWorld(), anchor.x(), anchor.y(), anchor.z());
    }

    /** Configure direction before Paper adds the entity, using its adjacent-air-cell spawn contract. */
    private static ItemFrame spawnItemFrame(Location supportingBlock, BlockFace facing) {
        Location spawnAt = frameSpawnAnchor(supportingBlock, facing);
        if (!spawnAt.getBlock().isPassable()) {
            throw new IllegalStateException("item-frame hanging cell is blocked at "
                    + blockKey(spawnAt.getBlock()));
        }
        return spawnAt.getWorld().spawn(spawnAt, ItemFrame.class, spawned -> {
            if (!spawned.setFacingDirection(facing, true)) {
                throw new IllegalStateException("Paper refused item-frame mount facing " + facing);
            }
        });
    }

    private static Rotation rotation(int ordinal) {
        return Rotation.values()[Math.floorMod(ordinal, Rotation.values().length)];
    }

    private static boolean isOperable(Material material) {
        String name = material.name();
        return material == Material.LEVER || name.endsWith("_BUTTON")
                || name.endsWith("PRESSURE_PLATE") || name.endsWith("_SIGN")
                || name.endsWith("_HANGING_SIGN") || material == Material.SCULK_CATALYST;
    }

    private static boolean requiresFrontInteraction(Resolved resolved) {
        Address address = resolved.address();
        if ("BI07".equals(address.nodeId()) && "outer_lever".equals(address.componentId())) {
            return false; // Authority explicitly requires this decoy control to be unreachable.
        }
        if (address.kind() == AddressKind.ITEM_FRAME) return true;
        if (address.kind() != AddressKind.BLOCK) return false;
        Block block = resolved.location().getBlock();
        if (block.getState() instanceof InventoryHolder || block.getState() instanceof Sign) return true;
        Material material = block.getType();
        return !material.name().endsWith("PRESSURE_PLATE") && isOperable(material);
    }

    private static void ensureSupport(Block support, Address address, String label, Mode mode,
                                      boolean mutate, Accumulator result) {
        if (support.getType().isSolid()) return;
        if (mutate && safeReplace(support, mode)) {
            support.setType(Material.POLISHED_DEEPSLATE, false);
            result.repaired++;
        } else {
            result.block(address, label + " has no solid support at " + blockKey(support));
        }
    }

    private static boolean safeReplace(Block block) {
        if (block.getState() instanceof TileState) return false;
        return block.getType().isAir()
                || block.getType() == Material.POLISHED_DEEPSLATE
                || block.getType() == Material.DEEPSLATE_TILES
                || block.getType() == Material.DEEPSLATE_BRICKS
                || block.getType() == Material.CHISELED_TUFF
                || block.getType() == Material.TUFF_BRICKS;
    }

    private static boolean safeReplace(Block block, Mode mode) {
        if (mode != Mode.FRESH_INSTALL) return safeReplace(block);
        if (block.getState() instanceof InventoryHolder holder && !holder.getInventory().isEmpty()) {
            return false;
        }
        return switch (block.getType()) {
            case BEDROCK, BARRIER, END_PORTAL, END_PORTAL_FRAME, COMMAND_BLOCK,
                    CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, STRUCTURE_BLOCK, JIGSAW -> false;
            default -> true;
        };
    }

    private static boolean itemPdcEquals(ItemStack item, String key, String expected) {
        if (item == null || !item.hasItemMeta()) return false;
        return expected.equals(item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey("observance", key), PersistentDataType.STRING));
    }

    private static Map<String, JsonObject> components(JsonObject predicate) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        JsonArray array = predicate.getAsJsonArray("components");
        for (JsonElement element : array) {
            JsonObject component = element.getAsJsonObject();
            result.put(string(component, "id"), component);
        }
        return result;
    }

    private static Map<String, String> readPdc(JsonObject spec) {
        JsonObject pdc = object(spec, "pdc");
        if (pdc == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : pdc.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(result);
    }

    private static Resolved findResolved(Map<String, Resolved> resolved,
                                         String nodeId, String componentId) {
        Resolved exact = resolved.get(nodeId + ":" + componentId);
        if (exact != null) return exact;
        for (Map.Entry<String, Resolved> entry : resolved.entrySet()) {
            if (entry.getKey().startsWith(nodeId + ":" + componentId + "_")) return entry.getValue();
        }
        return null;
    }

    private static String instanceKey(Address address) {
        return address.nodeId() + ":" + address.instanceId();
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? value.getAsInt() : fallback;
    }

    private static String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static final class Accumulator {
        private int nodes;
        private int addresses;
        private int placed;
        private int repaired;
        private int preserved;
        private int seeded;
        private int logicalOnly;
        private final List<Finding> findings = new ArrayList<>();

        private boolean hasBlocker() {
            return findings.stream().anyMatch(finding -> finding.severity() == Severity.BLOCKER);
        }

        private void block(Address address, String message) {
            findings.add(new Finding(Severity.BLOCKER, address.nodeId(), address.componentId(), message));
        }

        private Report report() {
            return new Report(nodes, addresses, placed, repaired, preserved, seeded,
                    logicalOnly, List.copyOf(findings));
        }
    }
}
