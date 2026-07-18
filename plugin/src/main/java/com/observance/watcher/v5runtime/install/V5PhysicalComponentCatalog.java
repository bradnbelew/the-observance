package com.observance.watcher.v5runtime.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.structure.DeepHoldV4Plan;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import com.observance.watcher.v5runtime.P11IdentityAuthority;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure, immutable spatial index for every component declared by the V5 predicate authority.
 *
 * <p>The catalog does not infer solve semantics. It expands exact offsets, series, marker arrays,
 * route cells and volumes into addresses that an installer/auditor can reconcile. Components with
 * no authored position receive a deterministic position inside their owner fixture. Any incompatible
 * exact overlap remains a blocker; it is never silently moved.</p>
 */
public final class V5PhysicalComponentCatalog {
    private static final Pattern TRIGGER_OFFSET = Pattern.compile(
            "\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]");
    private static final List<LocalOffset> HANDLE_CANDIDATES = List.of(
            new LocalOffset(2, 1, 0), new LocalOffset(-2, 1, 0),
            new LocalOffset(0, 1, 2), new LocalOffset(0, 1, -2),
            new LocalOffset(3, 1, 0), new LocalOffset(-3, 1, 0),
            new LocalOffset(1, 1, 2), new LocalOffset(-1, 1, 2),
            new LocalOffset(1, 1, 1), new LocalOffset(-1, 1, 1),
            new LocalOffset(0, 1, 1), new LocalOffset(0, 1, -1));
    private static final List<LocalOffset> SECTOR_OFFSETS = List.of(
            new LocalOffset(-3, 0, 3), new LocalOffset(0, 0, 3),
            new LocalOffset(3, 0, 3), new LocalOffset(-4, 0, 1),
            new LocalOffset(4, 0, 1), new LocalOffset(-4, 0, -2),
            new LocalOffset(4, 0, -2), new LocalOffset(-2, 0, -4),
            new LocalOffset(1, 0, -4), new LocalOffset(3, 0, -3),
            new LocalOffset(-1, 0, 0), new LocalOffset(1, 0, 0));
    private static final List<LocalOffset> RP04_HANDLE_OFFSETS = List.of(
            new LocalOffset(-3, 1, 4), new LocalOffset(0, 1, 4),
            new LocalOffset(3, 1, 4), new LocalOffset(-5, 1, 1),
            new LocalOffset(5, 1, 1), new LocalOffset(-5, 1, -2),
            new LocalOffset(5, 1, -2), new LocalOffset(-2, 1, -5),
            new LocalOffset(1, 1, -5), new LocalOffset(3, 1, -4),
            new LocalOffset(-2, 1, 0), new LocalOffset(2, 1, 0));

    /**
     * The six Release Record readers have authored dial/slot predicates but the authority's
     * prose trigger predates an explicit reader-control component.  These positions put a
     * distinct, reachable button directly in front of each reader's own backing block.  AR07's
     * authored {@code false_m} button is deliberately not one of these controls: it records the
     * required rejection event and must never double as the final evaluation trigger.
     */
    private static final Map<String, LocalOffset> RELEASE_READER_CONTROLS = Map.of(
            "AR03", new LocalOffset(-3, 2, 1),
            "AR04", new LocalOffset(-1, 2, 1),
            "AR05", new LocalOffset(2, 3, 1),
            "AR06", new LocalOffset(3, 2, 2),
            "AR07", new LocalOffset(5, 2, 1));
    private static final Map<String, Map<String, LocalOffset>> CONTAINER_LABEL_OFFSETS = Map.of(
            "KV02", Map.of(
                    "cistern", new LocalOffset(-3, 1, 0),
                    "public_heat", new LocalOffset(-2, 1, 0),
                    "private_heat", new LocalOffset(2, 1, 0),
                    "condemned", new LocalOffset(3, 1, 0)),
            "CW02", Map.of(
                    "A_top", new LocalOffset(-3, 1, 0),
                    "A_lower", new LocalOffset(-1, 1, 0),
                    "B_top", new LocalOffset(1, 1, 0),
                    "B_lower", new LocalOffset(3, 1, 0)));

    /** Deterministic protected staging blocks required by an unpositioned portable component. */
    private static final Map<String, SyntheticBlock> SYNTHETIC_BLOCKS = Map.ofEntries(
            Map.entry("KV01", new SyntheticBlock("audit_slip_source", new LocalOffset(0, 0, -2))),
            Map.entry("A03", new SyntheticBlock("lot_staging", new LocalOffset(0, 0, 8))),
            Map.entry("WR01", new SyntheticBlock("quotation_card_source", new LocalOffset(0, 0, -1))),
            Map.entry("WR02", new SyntheticBlock("packet_source", new LocalOffset(-2, 0, 0))),
            Map.entry("CW02", new SyntheticBlock("sample_source", new LocalOffset(0, 0, 4))),
            Map.entry("BI02", new SyntheticBlock("fragment_source", new LocalOffset(0, 0, 2))),
            Map.entry("BI03", new SyntheticBlock("instrument_source", new LocalOffset(0, 0, 2))),
            Map.entry("BI06", new SyntheticBlock("sample_source", new LocalOffset(0, 0, 2))),
            Map.entry("KI02", new SyntheticBlock("reed_source", new LocalOffset(0, 0, 2))));

    public enum AddressKind {
        BLOCK,
        ITEM_FRAME,
        ITEM_DISPLAY,
        MARKER,
        ENTITY_REFERENCE,
        INTERACTION_CELL,
        BOOK_REFERENCE,
        PORTABLE_ITEM,
        LOGICAL_ONLY
    }

    public enum Severity { BLOCKER, WARNING, NOTE }

    public record Finding(Severity severity, String nodeId, String componentId, String message) {
        public Finding {
            Objects.requireNonNull(severity, "severity");
            nodeId = clean(nodeId);
            componentId = clean(componentId);
            message = Objects.requireNonNullElse(message, "").trim();
        }
    }

    /** One expanded physical/logical address. rawComponentJson remains exact authority JSON. */
    public record Address(String nodeId, String owner, String siteId, String componentId,
                          String instanceId, AddressKind kind, LocalOffset offset,
                          String material, Map<String, String> pdc, Integer requiredRotation,
                          Integer resetRotation, Integer inventorySlot, String bookId,
                          boolean synthetic, String rawComponentJson) {
        public Address {
            nodeId = clean(nodeId);
            owner = clean(owner);
            siteId = clean(siteId);
            componentId = clean(componentId);
            instanceId = clean(instanceId);
            Objects.requireNonNull(kind, "kind");
            offset = offset == null ? new LocalOffset(0, 0, 0) : offset;
            material = clean(material).toUpperCase(Locale.ROOT);
            pdc = pdc == null ? Map.of() : Map.copyOf(pdc);
            bookId = clean(bookId);
            rawComponentJson = Objects.requireNonNullElse(rawComponentJson, "{}");
        }

        public String addressKey() {
            return siteId + ":" + offset.right() + ":" + offset.up() + ":" + offset.front();
        }
    }

    public record NodePlan(PhysicalPredicateAuthority.Node node, String predicateJson,
                           List<Address> addresses) {
        public NodePlan {
            Objects.requireNonNull(node, "node");
            predicateJson = Objects.requireNonNull(predicateJson, "predicateJson");
            addresses = List.copyOf(addresses);
        }
    }

    public record Catalog(String authoritySha256, List<NodePlan> nodes, List<Address> addresses,
                          List<Finding> findings) {
        public Catalog {
            authoritySha256 = clean(authoritySha256);
            nodes = List.copyOf(nodes);
            addresses = List.copyOf(addresses);
            findings = List.copyOf(findings);
        }

        public boolean valid() {
            return findings.stream().noneMatch(finding -> finding.severity() == Severity.BLOCKER);
        }

        public List<Address> addressesForNode(String nodeId) {
            return addresses.stream().filter(address -> address.nodeId().equals(nodeId)).toList();
        }
    }

    private record SyntheticBlock(String id, LocalOffset offset) { }

    private V5PhysicalComponentCatalog() { }

    public static Catalog loadDefault() {
        return build(PhysicalPredicateAuthorityLoader.loadDefault());
    }

    public static Catalog build(PhysicalPredicateAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        PhysicalPredicateAuthority effectiveAuthority = P11IdentityAuthority.apply(authority);
        List<NodePlan> nodePlans = new ArrayList<>();
        List<Address> all = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();
        Set<String> globalOccupied = new LinkedHashSet<>();

        for (PhysicalPredicateAuthority.Node node : effectiveAuthority.nodes()) {
            JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
            List<Address> addresses = expandNode(node, predicate, findings, globalOccupied);
            nodePlans.add(new NodePlan(node, node.predicate().canonicalJson(), addresses));
            all.addAll(addresses);
            addresses.stream().filter(address -> address.kind() == AddressKind.BLOCK
                            || address.kind() == AddressKind.ITEM_FRAME
                            || address.kind() == AddressKind.ITEM_DISPLAY)
                    .map(Address::addressKey).forEach(globalOccupied::add);
        }
        all = relocateUnpositionedSyntheticAddresses(all, effectiveAuthority);
        all = addEvaluationHandleSupports(all, findings);
        Map<String, List<Address>> byNode = new LinkedHashMap<>();
        for (Address address : all) byNode.computeIfAbsent(address.nodeId(), ignored -> new ArrayList<>()).add(address);
        nodePlans = effectiveAuthority.nodes().stream().map(node -> new NodePlan(node,
                node.predicate().canonicalJson(), byNode.getOrDefault(node.nodeId(), List.of()))).toList();
        inspectBounds(all, findings);
        inspectRequiredMaterials(all, findings);
        inspectCollisions(all, findings);
        return new Catalog(effectiveAuthority.sha256(), nodePlans, all, findings);
    }

    private static List<Address> relocateUnpositionedSyntheticAddresses(
            List<Address> source, PhysicalPredicateAuthority authority) {
        Map<String, PhysicalPredicateAuthority.Node> nodes = authority.nodesById();
        Set<String> occupied = new LinkedHashSet<>();
        source.stream().filter(address -> !address.synthetic())
                .filter(address -> address.kind() == AddressKind.BLOCK
                        || address.kind() == AddressKind.ITEM_FRAME
                        || address.kind() == AddressKind.ITEM_DISPLAY)
                .map(Address::addressKey).forEach(occupied::add);
        // A posture/route cell owns both player body blocks even though neither is a placed
        // component. Reserve that envelope before relocating generated handles so an earlier
        // node's synthetic control cannot occupy a later node's crouch/head cell.
        source.stream().filter(address -> !address.synthetic())
                .filter(address -> address.kind() == AddressKind.INTERACTION_CELL)
                .forEach(address -> {
                    occupied.add(address.addressKey());
                    LocalOffset cell = address.offset();
                    occupied.add(address.siteId() + ":" + cell.right() + ":"
                            + (cell.up() + 1) + ":" + cell.front());
                });
        List<Address> result = new ArrayList<>(source.size());
        for (Address address : source) {
            if (!address.synthetic() || (address.kind() != AddressKind.BLOCK
                    && address.kind() != AddressKind.ITEM_FRAME
                    && address.kind() != AddressKind.ITEM_DISPLAY)) {
                result.add(address);
                continue;
            }
            PhysicalPredicateAuthority.Node node = nodes.get(address.nodeId());
            boolean authoredTriggerOffset = "evaluation_handle".equals(address.componentId())
                    && node != null && parseTriggerOffset(node.predicate().evaluationTrigger()) != null;
            Address resolved = address;
            if (!authoredTriggerOffset && occupied.contains(address.addressKey())) {
                LocalOffset free = firstFree(address.siteId(), occupied);
                resolved = new Address(address.nodeId(), address.owner(), address.siteId(),
                        address.componentId(), address.instanceId(), address.kind(), free,
                        address.material(), address.pdc(), address.requiredRotation(),
                        address.resetRotation(), address.inventorySlot(), address.bookId(),
                        true, address.rawComponentJson());
            }
            result.add(resolved);
            occupied.add(resolved.addressKey());
        }
        return List.copyOf(result);
    }

    private static List<Address> addEvaluationHandleSupports(List<Address> source,
                                                              List<Finding> findings) {
        List<Address> result = new ArrayList<>(source);
        Set<String> occupied = source.stream()
                .filter(address -> address.kind() == AddressKind.BLOCK
                        || address.kind() == AddressKind.ITEM_FRAME
                        || address.kind() == AddressKind.ITEM_DISPLAY)
                .map(Address::addressKey).collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new));
        for (Address handle : source) {
            if (!"evaluation_handle".equals(handle.componentId())) continue;
            LocalOffset at = handle.offset();
            LocalOffset ceiling = new LocalOffset(at.right(), at.up() + 1, at.front());
            LocalOffset floor = new LocalOffset(at.right(), at.up() - 1, at.front());
            LocalOffset chosen = !occupied.contains(offsetKey(handle.siteId(), ceiling))
                    ? ceiling : !occupied.contains(offsetKey(handle.siteId(), floor)) ? floor : null;
            if (chosen == null) {
                findings.add(new Finding(Severity.BLOCKER, handle.nodeId(),
                        "evaluation_handle_support",
                        "no exact floor or ceiling support cell is free for " + handle.addressKey()));
                continue;
            }
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "evaluation_handle_support");
            raw.addProperty("block", "POLISHED_DEEPSLATE");
            raw.addProperty("synthetic_from", handle.nodeId() + " relocated evaluation handle");
            Address support = new Address(handle.nodeId(), handle.owner(), handle.siteId(),
                    "evaluation_handle_support", "evaluation_handle_support", AddressKind.BLOCK,
                    chosen, "POLISHED_DEEPSLATE",
                    Map.of("v5_supports_control", handle.nodeId() + " evaluation handle"),
                    null, null, null, "", false, raw.toString());
            result.add(support);
            occupied.add(support.addressKey());
        }
        return List.copyOf(result);
    }

    private static String offsetKey(String siteId, LocalOffset offset) {
        return siteId + ':' + offset.right() + ':' + offset.up() + ':' + offset.front();
    }

    private static List<Address> expandNode(PhysicalPredicateAuthority.Node node, JsonObject predicate,
                                            List<Finding> findings, Set<String> globalOccupied) {
        List<Address> out = new ArrayList<>();
        Set<String> occupied = new LinkedHashSet<>();
        JsonArray components = array(predicate, "components");
        List<Integer> resetRotations = parseResetRotations(node.resetRepairRecoveryJson());
        int rotationCursor = 0;

        for (JsonElement element : components) {
            JsonObject component = element.getAsJsonObject();
            String componentId = string(component, "id");
            List<Address> expanded = expandComponent(node, predicate, component, resetRotations,
                    rotationCursor, findings);
            rotationCursor += (int) expanded.stream().filter(a -> a.requiredRotation() != null).count();
            for (Address address : expanded) {
                out.add(address);
                if (address.kind() == AddressKind.BLOCK || address.kind() == AddressKind.ITEM_FRAME
                        || address.kind() == AddressKind.ITEM_DISPLAY) {
                    occupied.add(address.addressKey());
                }
            }
            if (expanded.isEmpty()) {
                out.add(address(node, componentId, componentId, node.siteId(), AddressKind.LOGICAL_ONLY,
                        new LocalOffset(0, 0, 0), "", pdc(component, "pdc"), null, null,
                        null, bookId(component), false, component));
            }
        }

        SyntheticBlock synthetic = SYNTHETIC_BLOCKS.get(node.nodeId());
        if (synthetic != null) {
            JsonObject raw = new JsonObject();
            raw.addProperty("id", synthetic.id());
            raw.addProperty("block", "BARREL");
            Map<String, String> sourcePdc = new LinkedHashMap<>();
            sourcePdc.put("v5_source_node", node.nodeId());
            if ("KV01".equals(node.nodeId())) {
                sourcePdc.put("v5_container_title", "Numbered Audit Slips");
            }
            Address address = address(node, synthetic.id(), synthetic.id(), node.siteId(),
                    AddressKind.BLOCK, synthetic.offset(), "BARREL",
                    sourcePdc, null, null, null, "", true, raw);
            out.add(address);
            occupied.add(address.addressKey());
        }

        // WR02's authored "reader eject" is a physical action, but its predicate predates the
        // explicit control field. Bind one reachable button rather than weakening it into a generic
        // container click or leaving the packet reader with no completion action.
        if ("WR02".equals(node.nodeId())) {
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "reader_eject");
            raw.addProperty("block", "STONE_BUTTON");
            raw.addProperty("synthetic_from", "reader eject evaluation_trigger");
            Address eject = address(node, "reader_eject", "reader_eject", node.siteId(),
                    AddressKind.BLOCK, new LocalOffset(1, 1, 0), "STONE_BUTTON",
                    Map.of("v5_control_id", "wr02_reader_eject", "v5_handle_node", "WR02"),
                    null, null, null, "", true, raw);
            out.add(eject);
            occupied.add(eject.addressKey());
        }

        LocalOffset readerOffset = RELEASE_READER_CONTROLS.get(node.nodeId());
        if (readerOffset != null) {
            String controlId = node.nodeId().toLowerCase(Locale.ROOT) + "_reader";
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "reader_control");
            raw.addProperty("block", "STONE_BUTTON");
            raw.addProperty("synthetic_from", "pull reader evaluation_trigger");
            Address reader = address(node, "reader_control", "reader_control", node.siteId(),
                    AddressKind.BLOCK, readerOffset, "STONE_BUTTON",
                    Map.of("v5_control_id", controlId, "v5_handle_node", node.nodeId()),
                    null, null, null, "", true, raw);
            out.add(reader);
            occupied.add(reader.addressKey());
        }

        // KM01's handle belongs to Edition 3 itself.  Binding it as the generic
        // "evaluation_handle" loses the component relation required by handle_belongs_to(e3).
        if ("KM01".equals(node.nodeId())) {
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "e3");
            raw.addProperty("block", "LEVER");
            raw.addProperty("synthetic_from", "verification handle beside selected lectern");
            Address selector = address(node, "e3", "e3_selector", "mara_lectern_3",
                    AddressKind.BLOCK, new LocalOffset(1, 1, 0), "LEVER",
                    Map.of("v5_control_id", "km01_e3_selector",
                            "v5_handle_node", "KM01",
                            "v5_selection", "KM01_E3",
                            "selector_value", "KM01_E3"),
                    null, null, null, "", true, raw);
            out.add(selector);
            occupied.add(selector.addressKey());
        }

        // Map.of intentionally has unspecified iteration order. These generated labels can be
        // relocated around authored components, so order must be stable across JVM restarts or
        // the same four signs can be assigned different authority identities after reboot.
        for (Map.Entry<String, LocalOffset> label
                : CONTAINER_LABEL_OFFSETS.getOrDefault(node.nodeId(), Map.of()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            String componentId = label.getKey() + "_label";
            JsonObject raw = new JsonObject();
            raw.addProperty("id", componentId);
            raw.addProperty("block", "OAK_SIGN");
            raw.addProperty("synthetic_from", "player-visible container label authority");
            Address sign = address(node, componentId, componentId, node.siteId(), AddressKind.BLOCK,
                    label.getValue(), "OAK_SIGN",
                    Map.of("v5_label_for", label.getKey()), null, null, null, "", true, raw);
            out.add(sign);
            occupied.add(sign.addressKey());
        }

        // Two predicates use physical pull nouns other than "handle".  Keep their trigger bound
        // to the component named by the predicate, so mechanics records the same component that
        // the authority evaluates instead of an unrelated generic handle.
        if ("HS02".equals(node.nodeId())) {
            out.add(pullControl(node, "housing_latch", "housing_latch", new LocalOffset(1, 1, 0),
                    "hs02_housing_latch", "cartridge housing latch"));
            out.add(controlSupport(node, "housing_latch_support", new LocalOffset(1, 2, 0),
                    "cartridge housing latch"));
            occupied.add(node.siteId() + ":1:1:0");
            occupied.add(node.siteId() + ":1:2:0");
        }
        if ("CW07".equals(node.nodeId())) {
            out.add(pullControl(node, "cache_seal", "cache_seal", new LocalOffset(2, 1, 0),
                    "cw07_cache_seal", "cache seal"));
            out.add(controlSupport(node, "cache_seal_support", new LocalOffset(2, 2, 0),
                    "cache seal"));
            occupied.add(node.siteId() + ":2:1:0");
            occupied.add(node.siteId() + ":2:2:0");
        }

        if ("RP04".equals(node.nodeId())) {
            for (int index = 0; index < SECTOR_OFFSETS.size(); index++) {
                LocalOffset plate = SECTOR_OFFSETS.get(index);
                Map<String, String> sectorPdc = Map.of("v5_rp04_sector", Integer.toString(index));
                Address handle = syntheticBlock(node, "sector_handle", "sector_handle_" + index,
                        RP04_HANDLE_OFFSETS.get(index), "STONE_BUTTON", sectorPdc,
                        "distinct RP04 sector confirmation");
                Address lamp = syntheticBlock(node, "sector_lamp", "sector_lamp_" + index,
                        new LocalOffset(plate.right(), plate.up() - 1, plate.front()),
                        "REDSTONE_LAMP", sectorPdc, "visible RP04 sector indicator");
                out.add(handle);
                out.add(lamp);
                occupied.add(handle.addressKey());
                occupied.add(lamp.addressKey());
            }
            addRp04BridgeHardware(node, out, occupied);
        }

        if ("RP05".equals(node.nodeId())) {
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "sever_control_interaction");
            raw.addProperty("synthetic_from", "clickable fallback for non-tile sever control");
            Address interaction = address(node, "sever_control_interaction",
                    "sever_control_interaction", node.siteId(), AddressKind.MARKER,
                    new LocalOffset(0, 1, 0), "",
                    Map.of("v5_finale_control", "sever_record"),
                    null, null, null, "", true, raw);
            out.add(interaction);
        }

        String trigger = node.predicate().evaluationTrigger().toLowerCase(Locale.ROOT);
        boolean needsHandle = trigger.contains("handle") && out.stream().noneMatch(address ->
                (address.material().equals("LEVER") || address.material().endsWith("_BUTTON"))
                        && (address.pdc().containsKey("v5_control_id")
                        || address.pdc().containsKey("v5_handle_node")));
        if (needsHandle) {
            LocalOffset explicit = parseTriggerOffset(node.predicate().evaluationTrigger());
            Set<String> unavailable = new LinkedHashSet<>(globalOccupied);
            unavailable.addAll(occupied);
            LocalOffset chosen = explicit == null ? firstFree(node.siteId(), unavailable) : explicit;
            JsonObject raw = new JsonObject();
            raw.addProperty("id", "evaluation_handle");
            raw.addProperty("block", "LEVER");
            raw.addProperty("synthetic_from", "evaluation_trigger");
            Address handle = address(node, "evaluation_handle", "evaluation_handle", node.siteId(),
                    AddressKind.BLOCK, chosen, "LEVER", Map.of("v5_handle_node", node.nodeId()),
                    null, null, null, "", true, raw);
            out.add(handle);
        }
        return List.copyOf(out);
    }

    private static Address controlSupport(PhysicalPredicateAuthority.Node node,
                                          String componentId, LocalOffset offset,
                                          String authoredName) {
        JsonObject raw = new JsonObject();
        raw.addProperty("id", componentId);
        raw.addProperty("block", "POLISHED_DEEPSLATE");
        raw.addProperty("synthetic_from", authoredName + " exact ceiling support");
        return address(node, componentId, componentId, node.siteId(), AddressKind.BLOCK, offset,
                "POLISHED_DEEPSLATE", Map.of("v5_supports_control", authoredName),
                null, null, null, "", false, raw);
    }

    private static Address pullControl(PhysicalPredicateAuthority.Node node, String componentId,
                                       String instanceId, LocalOffset offset, String controlId,
                                       String authoredName) {
        JsonObject raw = new JsonObject();
        raw.addProperty("id", componentId);
        raw.addProperty("block", "LEVER");
        raw.addProperty("synthetic_from", authoredName + " evaluation_trigger");
        return address(node, componentId, instanceId, node.siteId(), AddressKind.BLOCK, offset,
                "LEVER", Map.of("v5_control_id", controlId,
                        "v5_handle_node", node.nodeId()),
                null, null, null, "", false, raw);
    }

    private static Address syntheticBlock(PhysicalPredicateAuthority.Node node, String componentId,
                                          String instanceId, LocalOffset offset, String material,
                                          Map<String, String> pdc, String source) {
        JsonObject raw = new JsonObject();
        raw.addProperty("id", componentId);
        raw.addProperty("block", material);
        raw.addProperty("synthetic_from", source);
        return address(node, componentId, instanceId, node.siteId(), AddressKind.BLOCK, offset,
                material, pdc, null, null, null, "", true, raw);
    }

    private static void addRp04BridgeHardware(PhysicalPredicateAuthority.Node node,
                                               List<Address> out, Set<String> occupied) {
        record Hardware(String id, LocalOffset offset, String suffix, String title,
                        String baseMaterial) { }
        List<Hardware> hardware = List.of(
                new Hardware("bridge_start_housing", new LocalOffset(0, 0, -2), "start",
                        "BRIDGE WINDOW START", "COPPER_BLOCK"),
                new Hardware("bridge_condemn_black", new LocalOffset(-6, 0, 4), "condemn_black",
                        "BLACK FIXED HOUSING", "BLACK_CONCRETE"),
                new Hardware("bridge_understand_origin", new LocalOffset(-3, 0, 6),
                        "understand_origin", "AMBER HANDOFF ORIGIN", "ORANGE_CONCRETE"),
                new Hardware("bridge_understand_amber", new LocalOffset(0, 0, 6),
                        "understand_amber", "AMBER HANDOFF HOUSING", "ORANGE_CONCRETE"),
                new Hardware("bridge_free_center", new LocalOffset(3, 0, 6), "free_center",
                        "CENTER BRIDGE PICKUP", "QUARTZ_BLOCK"),
                new Hardware("bridge_free_white", new LocalOffset(6, 0, 4), "free_white",
                        "WHITE RELEASE TROUGH", "WHITE_CONCRETE"));
        for (Hardware fixture : hardware) {
            Map<String, String> pdc = Map.of(
                    "v5_rp04_bridge_control", fixture.suffix(),
                    "v5_container_title", fixture.title());
            Address housing = syntheticBlock(node, fixture.id(), fixture.id(), fixture.offset(),
                    "BARREL", pdc, "RP04 branch-specific Bridge custody");
            housing = new Address(housing.nodeId(), housing.owner(), housing.siteId(),
                    housing.componentId(), housing.instanceId(), housing.kind(), housing.offset(),
                    housing.material(), housing.pdc(), housing.requiredRotation(),
                    housing.resetRotation(), 13, housing.bookId(), housing.synthetic(),
                    housing.rawComponentJson());
            Address base = syntheticBlock(node, fixture.id() + "_base", fixture.id() + "_base",
                    new LocalOffset(fixture.offset().right(), fixture.offset().up() - 1,
                            fixture.offset().front()), fixture.baseMaterial(),
                    Map.of("v5_rp04_bridge_visual", fixture.suffix()),
                    "visible RP04 Bridge housing color");
            out.add(housing);
            out.add(base);
            occupied.add(housing.addressKey());
            occupied.add(base.addressKey());
        }
    }

    private static List<Address> expandComponent(PhysicalPredicateAuthority.Node node,
                                                 JsonObject predicate, JsonObject component,
                                                 List<Integer> resetRotations, int rotationCursor,
                                                 List<Finding> findings) {
        String id = string(component, "id");
        String block = string(component, "block");
        String anchorMaterial = string(component, "anchor_material");
        String material = !block.isBlank() ? block : (!anchorMaterial.isBlank()
                ? anchorMaterial : string(component, "material"));
        Map<String, String> basePdc = mergedPdc(component);
        String bookId = bookId(component);
        List<Address> out = new ArrayList<>();

        if (component.has("containers")) {
            int index = 0;
            for (JsonElement entryElement : component.getAsJsonArray("containers")) {
                JsonObject entry = entryElement.getAsJsonObject();
                String name = string(entry, "name").toLowerCase(Locale.ROOT);
                // Entry material/PDC describe the deposited testimony item, not the barrel tile.
                // The exact item contract remains available in rawComponentJson to evaluators.
                Map<String, String> entryPdc = Map.of("v5_testimony_name", name);
                out.add(address(node, id, id + "_" + name, node.siteId(), AddressKind.BLOCK,
                        offset(entry.getAsJsonArray("offset")), "BARREL", entryPdc, null, null,
                        integer(entry, "slot"), "", false, entry));
                index++;
            }
            return out;
        }

        if (component.has("markers")) {
            int index = 0;
            String pdcKey = string(component, "pdc_key");
            for (JsonElement markerElement : component.getAsJsonArray("markers")) {
                JsonObject marker = markerElement.getAsJsonObject();
                String value = string(marker, "value");
                String markerMaterial = string(marker, "material");
                Map<String, String> markerPdc = pdcKey.isBlank()
                        ? Map.of() : Map.of(pdcKey, value);
                AddressKind kind = knownBlock(markerMaterial)
                        ? AddressKind.BLOCK : AddressKind.ITEM_DISPLAY;
                out.add(address(node, id, id + "_" + index, node.siteId(), kind,
                        offset(marker.getAsJsonArray("offset")), markerMaterial, markerPdc,
                        null, null, null, "", false, marker));
                index++;
            }
            return out;
        }

        if (component.has("offset_volume")) {
            JsonArray bounds = component.getAsJsonArray("offset_volume");
            LocalOffset a = offset(bounds.get(0).getAsJsonArray());
            LocalOffset b = offset(bounds.get(1).getAsJsonArray());
            String volumeMaterial = firstString(component, "block_set", "POLISHED_DEEPSLATE");
            int index = 0;
            for (int right = Math.min(a.right(), b.right()); right <= Math.max(a.right(), b.right()); right++) {
                for (int up = Math.min(a.up(), b.up()); up <= Math.max(a.up(), b.up()); up++) {
                    for (int front = Math.min(a.front(), b.front()); front <= Math.max(a.front(), b.front()); front++) {
                        out.add(address(node, id, id + "_" + index++, node.siteId(), AddressKind.BLOCK,
                                new LocalOffset(right, up, front), volumeMaterial, basePdc,
                                null, null, null, "", false, component));
                    }
                }
            }
            return out;
        }

        if (component.has("cells")) {
            int index = 0;
            for (JsonElement cellElement : component.getAsJsonArray("cells")) {
                JsonObject cell = cellElement.getAsJsonObject();
                String floor = string(cell, "block");
                Map<String, String> cellPdc = Map.of("v5_cell_step",
                        Integer.toString(integer(cell, "step") == null ? index + 1 : integer(cell, "step")));
                out.add(address(node, id, id + "_" + index, node.siteId(),
                        AddressKind.INTERACTION_CELL, offset(cell.getAsJsonArray("offset")),
                        floor, cellPdc, null, null, null, "", false, cell));
                index++;
            }
            return out;
        }

        if (component.has("site_series")) {
            JsonArray series = component.getAsJsonArray("site_series");
            List<Integer> rotations = integers(component, "required_rotations");
            List<String> pdcValues = strings(component, "pdc_values");
            List<String> pdcSequence = strings(component, "pdc_sequence");
            String pdcKey = string(component, "pdc_key");
            if (pdcKey.isBlank() && !pdcValues.isEmpty()) pdcKey = inferredPdcValueKey(node.nodeId());
            if (pdcKey.isBlank() && !pdcSequence.isEmpty()) pdcKey = "v5_mark_value";
            String pdcPrefix = string(component, "pdc_prefix");
            for (int index = 0; index < series.size(); index++) {
                String site = series.get(index).getAsString();
                Map<String, String> itemPdc = new LinkedHashMap<>(basePdc);
                if (!pdcKey.isBlank() && index < pdcValues.size()) itemPdc.put(pdcKey, pdcValues.get(index));
                if (!pdcKey.isBlank() && index < pdcSequence.size()) itemPdc.put(pdcKey, pdcSequence.get(index));
                if (!pdcPrefix.isBlank()) itemPdc.put(prefixPdcKey(node.nodeId()), pdcPrefix + (index + 1));
                AddressKind kind = "ITEM_FRAME".equals(block)
                        ? AddressKind.ITEM_FRAME : AddressKind.MARKER;
                Integer required = index < rotations.size() ? rotations.get(index) : null;
                Integer reset = resetRotation(required, resetRotations, rotationCursor + index);
                out.add(address(node, id, id + "_" + index, site, kind,
                        kind == AddressKind.ITEM_FRAME ? new LocalOffset(0, 1, 0)
                                : new LocalOffset(0, 0, 0),
                        "ITEM_FRAME".equals(block) ? string(component, "material") : material,
                        itemPdc, required, reset, null, "", false, component));
            }
            return out;
        }

        List<LocalOffset> offsets = new ArrayList<>();
        if (component.has("offset")) offsets.add(offset(component.getAsJsonArray("offset")));
        if (component.has("offsets")) {
            for (JsonElement element : component.getAsJsonArray("offsets")) {
                offsets.add(offset(element.getAsJsonArray()));
            }
        }
        if (component.has("ordered_offsets")) {
            for (JsonElement element : component.getAsJsonArray("ordered_offsets")) {
                offsets.add(offset(element.getAsJsonArray()));
            }
        }
        if ("sectors".equals(id) && "LIGHT_WEIGHTED_PRESSURE_PLATE".equals(block)) {
            offsets.addAll(SECTOR_OFFSETS);
        }
        if (offsets.isEmpty() && !block.isBlank()) {
            LocalOffset trigger = parseTriggerOffset(node.predicate().evaluationTrigger());
            offsets.add(trigger == null ? new LocalOffset(0, 0, 0) : trigger);
        }

        List<Integer> rotations = integers(component, "required_rotations");
        Integer singleRotation = integer(component, "required_rotation");
        List<String> pdcValues = strings(component, "pdc_values");
        List<String> requiredItemIds = strings(component, "required_items");
        String pdcKey = string(component, "pdc_key");
        if (pdcKey.isBlank() && !pdcValues.isEmpty()) pdcKey = inferredPdcValueKey(node.nodeId());
        String pdcPrefix = string(component, "pdc_prefix");
        List<JsonObject> requiredItems = objects(component, "required_items");
        for (int index = 0; index < offsets.size(); index++) {
            Map<String, String> itemPdc = new LinkedHashMap<>(basePdc);
            String itemMaterial = string(component, "material");
            if (itemMaterial.isBlank()) itemMaterial = string(component, "item_material");
            if (index < requiredItems.size()) {
                JsonObject item = requiredItems.get(index);
                itemMaterial = string(item, "material");
                itemPdc.putAll(mergedPdc(item));
            }
            if (index < requiredItemIds.size()) {
                String itemKey = pdcKey.isBlank() ? "v5_evidence_id" : pdcKey;
                itemPdc.put(itemKey, requiredItemIds.get(index));
            }
            if (!pdcKey.isBlank() && index < pdcValues.size()) itemPdc.put(pdcKey, pdcValues.get(index));
            if (!pdcPrefix.isBlank()) itemPdc.put(prefixPdcKey(node.nodeId()), pdcPrefix + (index + 1));
            Integer required = index < rotations.size() ? rotations.get(index) : singleRotation;
            Integer reset = resetRotation(required, resetRotations, rotationCursor + index);
            AddressKind kind = addressKind(block, anchorMaterial, material, bookId);
            if (kind == AddressKind.LOGICAL_ONLY) {
                if (component.has("posture") || component.has("stance")) {
                    kind = AddressKind.INTERACTION_CELL;
                } else if (component.has("required_clear_ray")
                        || component.has("max_yaw_error_degrees")
                        || component.has("max_pitch_error_degrees")) {
                    kind = AddressKind.MARKER;
                }
            }
            String placedMaterial = kind == AddressKind.ITEM_FRAME ? itemMaterial
                    : (!block.isBlank() ? block : material);
            out.add(address(node, id, id + (offsets.size() == 1 ? "" : "_" + index),
                    node.siteId(), kind, offsets.get(index), placedMaterial, itemPdc,
                    required, reset, integer(component, "slot"), bookId, false, component));
        }

        if ("A02".equals(node.nodeId()) && id.endsWith("_station") && !offsets.isEmpty()) {
            LocalOffset station = offsets.getFirst();
            // Only the exact source surface owns the authority component ID.  Decorative station
            // anchors remain bound for protection/audit but cannot satisfy inspection by themselves.
            List<Address> renamed = new ArrayList<>();
            for (Address current : out) {
                if (current.kind() == AddressKind.BOOK_REFERENCE) {
                    renamed.add(current);
                } else {
                    renamed.add(new Address(current.nodeId(), current.owner(), current.siteId(),
                            id + "_anchor", current.instanceId() + "_anchor", current.kind(),
                            current.offset(), current.material(), current.pdc(),
                            current.requiredRotation(), current.resetRotation(), current.inventorySlot(),
                            current.bookId(), current.synthetic(), current.rawComponentJson()));
                }
            }
            out.clear();
            out.addAll(renamed);
            if ("mkept_station".equals(id)) {
                out.add(address(node, id + "_anchor", id + "_anchor", node.siteId(),
                        AddressKind.BLOCK, station, anchorMaterial, basePdc,
                        null, null, null, "", true, component));
            }

            boolean labelIsSource = component.has("source_label");
            JsonObject labelRaw = new JsonObject();
            labelRaw.addProperty("id", id + "_label");
            labelRaw.addProperty("block", "OAK_SIGN");
            labelRaw.addProperty("synthetic_from", "A02 station-label authority");
            out.add(address(node, labelIsSource ? id : id + "_label", id + "_label",
                    node.siteId(), AddressKind.BLOCK,
                    // Give the standing source label its own floor cell in front of the station.
                    // Putting it above the station makes its support overwrite comparators and
                    // item-display source cells, neither of which has a sturdy top face.
                    new LocalOffset(station.right(), station.up(), station.front() + 1),
                    "OAK_SIGN", Map.of("v5_station_label_for", id),
                    null, null, null, "", true, labelRaw));

            JsonObject sourceItem = object(component, "source_item");
            if (sourceItem != null) {
                out.add(address(node, id, id + "_source", node.siteId(), AddressKind.ITEM_DISPLAY,
                        new LocalOffset(station.right(), station.up() + 1, station.front() + 1),
                        string(sourceItem, "material"), mergedPdc(sourceItem),
                        null, null, null, "", true, sourceItem));
            }
        }

        if (out.isEmpty() && component.has("entity_pdc")) {
            out.add(address(node, id, id, node.siteId(), AddressKind.ENTITY_REFERENCE,
                    new LocalOffset(0, 0, 0), "", basePdc, null, null,
                    null, "", false, component));
        } else if (out.isEmpty() && !bookId.isBlank()) {
            out.add(address(node, id, id, node.siteId(), AddressKind.BOOK_REFERENCE,
                    new LocalOffset(0, 0, 0), "LECTERN", basePdc, null, null,
                    null, bookId, false, component));
        } else if (out.isEmpty() && !material.isBlank()) {
            out.add(address(node, id, id, node.siteId(), AddressKind.PORTABLE_ITEM,
                    new LocalOffset(0, 0, 0), material, basePdc, null, null,
                    null, "", false, component));
        } else if (out.isEmpty() && (component.has("required_ids") || component.has("required"))) {
            out.add(address(node, id, id, node.siteId(), AddressKind.PORTABLE_ITEM,
                    new LocalOffset(0, 0, 0), material, basePdc, null, null,
                    null, "", false, component));
        }
        return out;
    }

    private static Address address(PhysicalPredicateAuthority.Node node, String componentId,
                                   String instanceId, String siteId, AddressKind kind,
                                   LocalOffset offset, String material, Map<String, String> pdc,
                                   Integer requiredRotation, Integer resetRotation,
                                   Integer inventorySlot, String bookId, boolean synthetic,
                                   JsonObject raw) {
        return new Address(node.nodeId(), node.owner(), siteId, componentId, instanceId, kind,
                offset, material, pdc, requiredRotation, resetRotation, inventorySlot, bookId,
                synthetic, raw.toString());
    }

    private static AddressKind addressKind(String block, String anchorMaterial,
                                           String material, String bookId) {
        if (!bookId.isBlank() && block.isBlank()) return AddressKind.BOOK_REFERENCE;
        if ("ITEM_FRAME".equals(block)) return AddressKind.ITEM_FRAME;
        if (!block.isBlank()) return knownBlock(block) ? AddressKind.BLOCK : AddressKind.ITEM_DISPLAY;
        if (!anchorMaterial.isBlank()) return knownBlock(anchorMaterial)
                ? AddressKind.BLOCK : AddressKind.ITEM_DISPLAY;
        if (!material.isBlank()) return AddressKind.PORTABLE_ITEM;
        return AddressKind.LOGICAL_ONLY;
    }

    private static void inspectBounds(List<Address> addresses, List<Finding> findings) {
        for (Address address : addresses) {
            if (address.kind() == AddressKind.LOGICAL_ONLY
                    || address.kind() == AddressKind.PORTABLE_ITEM
                    || address.kind() == AddressKind.BOOK_REFERENCE) continue;
            int radius = siteRadius(address.siteId());
            int vertical = siteVerticalRadius(address.siteId());
            if (Math.max(Math.abs(address.offset().right()), Math.abs(address.offset().front())) > radius
                    || Math.abs(address.offset().up()) > vertical) {
                findings.add(new Finding(Severity.BLOCKER, address.nodeId(), address.componentId(),
                        "offset " + address.offset() + " exceeds owned fixture " + address.siteId()
                                + " radius=" + radius + "/vertical=" + vertical));
            }
        }
    }

    private static void inspectCollisions(List<Address> addresses, List<Finding> findings) {
        Map<String, List<Address>> at = new LinkedHashMap<>();
        for (Address address : addresses) {
            if (address.kind() != AddressKind.BLOCK && address.kind() != AddressKind.ITEM_FRAME
                    && address.kind() != AddressKind.ITEM_DISPLAY) continue;
            at.computeIfAbsent(address.addressKey(), ignored -> new ArrayList<>()).add(address);
        }
        for (Map.Entry<String, List<Address>> entry : at.entrySet()) {
            List<Address> occupants = entry.getValue();
            if (occupants.size() < 2 || compatibleSharedBlock(occupants)) continue;
            String details = occupants.stream().map(address -> address.nodeId() + "/"
                    + address.componentId() + "=" + address.kind() + ":" + address.material()).toList().toString();
            Address first = occupants.get(0);
            findings.add(new Finding(Severity.BLOCKER, first.nodeId(), first.componentId(),
                    "incompatible exact component collision at " + entry.getKey() + ": " + details));
        }
    }

    private static void inspectRequiredMaterials(List<Address> addresses, List<Finding> findings) {
        for (Address address : addresses) {
            if (address.kind() != AddressKind.BLOCK && address.kind() != AddressKind.ITEM_FRAME
                    && address.kind() != AddressKind.ITEM_DISPLAY) continue;
            if (!address.material().isBlank()) continue;
            findings.add(new Finding(Severity.BLOCKER, address.nodeId(), address.componentId(),
                    "physical " + address.kind() + " has no authored material"));
        }
    }

    private static boolean compatibleSharedBlock(List<Address> occupants) {
        if (occupants.stream().allMatch(address -> address.kind() == AddressKind.ITEM_FRAME)) {
            String material = occupants.get(0).material();
            Integer rotation = occupants.get(0).requiredRotation();
            return occupants.stream().allMatch(address -> address.material().equals(material)
                    && Objects.equals(address.requiredRotation(), rotation))
                    && pdcCompatible(occupants);
        }
        String node = occupants.get(0).nodeId();
        String material = occupants.get(0).material();
        if (occupants.stream().anyMatch(address -> address.kind() != AddressKind.BLOCK
                || !address.nodeId().equals(node) || !address.material().equals(material))) return false;
        if (!"CHISELED_BOOKSHELF".equals(material)) return false;
        Set<Integer> slots = new HashSet<>();
        for (Address address : occupants) {
            if (address.inventorySlot() == null || !slots.add(address.inventorySlot())) return false;
        }
        return true;
    }

    private static boolean pdcCompatible(List<Address> occupants) {
        Map<String, String> values = new HashMap<>();
        for (Address address : occupants) {
            for (Map.Entry<String, String> entry : address.pdc().entrySet()) {
                String previous = values.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous != null && !previous.equals(entry.getValue())) return false;
            }
        }
        return true;
    }

    private static String prefixPdcKey(String nodeId) {
        return "HS05".equals(nodeId) ? "v5_restoration_id" : "v5_control_id";
    }

    private static String inferredPdcValueKey(String nodeId) {
        return "WR04".equals(nodeId) ? "v5_evidence_id" : "v5_control_id";
    }

    private static int siteRadius(String siteId) {
        DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(siteId);
        if (fixture != null) return fixture.radius();
        for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
            if (station.id().equals(siteId)) return 8;
        }
        return siteId.startsWith("unlit_house_") ? 6 : 8;
    }

    private static int siteVerticalRadius(String siteId) {
        DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(siteId);
        if (fixture != null) return fixture.verticalRadius();
        return 6;
    }

    private static LocalOffset firstFree(String siteId, Set<String> occupied) {
        for (LocalOffset candidate : HANDLE_CANDIDATES) {
            String key = siteId + ":" + candidate.right() + ":" + candidate.up() + ":" + candidate.front();
            if (!occupied.contains(key)) return candidate;
        }
        return new LocalOffset(0, 1, 3);
    }

    private static LocalOffset parseTriggerOffset(String trigger) {
        Matcher matcher = TRIGGER_OFFSET.matcher(Objects.requireNonNullElse(trigger, ""));
        if (!matcher.find()) return null;
        return new LocalOffset(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    private static List<Integer> parseResetRotations(String recoveryJson) {
        JsonObject root = JsonParser.parseString(recoveryJson).getAsJsonObject();
        String text = string(root, "reset");
        Matcher matcher = Pattern.compile("\\[([0-7](?:\\s*,\\s*[0-7])+)\\]").matcher(text);
        if (!matcher.find()) return List.of();
        List<Integer> values = new ArrayList<>();
        for (String value : matcher.group(1).split(",")) values.add(Integer.parseInt(value.trim()));
        return List.copyOf(values);
    }

    private static Integer resetRotation(Integer required, List<Integer> authored, int index) {
        if (index >= 0 && index < authored.size()) return authored.get(index);
        return required == null ? null : Math.floorMod(required + 2, 8);
    }

    private static String bookId(JsonObject component) {
        JsonObject pdc = object(component, "book_pdc");
        if (pdc != null) {
            String v5 = string(pdc, "v5_book_id");
            String bound = v5.isBlank() ? string(pdc, "book_id") : v5;
            if (!bound.isBlank()) return bound;
        }
        return string(component, "source_book");
    }

    private static Map<String, String> mergedPdc(JsonObject value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.putAll(pdc(value, "pdc"));
        result.putAll(pdc(value, "class_pdc"));
        result.putAll(pdc(value, "required_pdc"));
        result.putAll(pdc(value, "entity_pdc"));
        result.putAll(pdc(value, "selector_pdc"));
        return Map.copyOf(result);
    }

    private static Map<String, String> pdc(JsonObject value, String key) {
        JsonObject pdc = object(value, key);
        if (pdc == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : pdc.entrySet()) {
            result.put(entry.getKey(), entry.getValue().isJsonPrimitive()
                    ? entry.getValue().getAsString() : entry.getValue().toString());
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> with(Map<String, String> input, String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(input);
        copy.put(key, value);
        return Map.copyOf(copy);
    }

    private static List<JsonObject> objects(JsonObject value, String key) {
        JsonArray array = array(value, key);
        if (array == null) return List.of();
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : array) if (element.isJsonObject()) result.add(element.getAsJsonObject());
        return List.copyOf(result);
    }

    private static List<String> strings(JsonObject value, String key) {
        JsonArray array = array(value, key);
        if (array == null) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) if (element.isJsonPrimitive()) result.add(element.getAsString());
        return List.copyOf(result);
    }

    private static List<Integer> integers(JsonObject value, String key) {
        JsonArray array = array(value, key);
        if (array == null) return List.of();
        List<Integer> result = new ArrayList<>();
        for (JsonElement element : array) if (element.isJsonPrimitive()) result.add(element.getAsInt());
        return List.copyOf(result);
    }

    private static String firstString(JsonObject value, String key, String fallback) {
        JsonArray array = array(value, key);
        return array == null || array.isEmpty() ? fallback : array.get(0).getAsString();
    }

    private static JsonArray array(JsonObject value, String key) {
        JsonElement element = value.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonObject object(JsonObject value, String key) {
        JsonElement element = value.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject value, String key) {
        JsonElement element = value.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static Integer integer(JsonObject value, String key) {
        JsonElement element = value.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt() : null;
    }

    private static LocalOffset offset(JsonArray value) {
        if (value == null || value.size() != 3) return new LocalOffset(0, 0, 0);
        return new LocalOffset(value.get(0).getAsInt(), value.get(1).getAsInt(), value.get(2).getAsInt());
    }

    private static boolean knownBlock(String material) {
        if (material == null || material.isBlank()) return false;
        return !Set.of("ITEM_FRAME", "PAPER", "BOOK", "WRITTEN_BOOK", "WRITABLE_BOOK",
                "MAP", "FILLED_MAP", "COMPASS", "CLOCK", "MAGMA_CREAM", "INK_SAC",
                "TRIPWIRE_HOOK", "COPPER_INGOT", "STRING", "POTION", "GLASS_BOTTLE",
                "WHEAT_SEEDS", "VINE", "REDSTONE_TORCH", "SPYGLASS").contains(material);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
