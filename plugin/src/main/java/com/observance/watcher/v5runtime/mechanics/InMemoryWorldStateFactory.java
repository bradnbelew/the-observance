package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Builds a manifest-derived in-memory world snapshot for exhaustive adapter contract tests. */
public final class InMemoryWorldStateFactory {
    private InMemoryWorldStateFactory() {
    }

    public static MechanicObservation correct(PhysicalPredicateAuthority.Node node, UUID actor) {
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        Map<String, JsonObject> components = components(predicate);
        MechanicObservation.Builder result = MechanicObservation.builder(actor, node.siteId());
        Map<String, Map<Integer, MechanicItem>> inventories = new HashMap<>();
        Map<String, Map<Integer, MechanicItem>> shelves = new HashMap<>();

        for (Map.Entry<String, JsonObject> entry : components.entrySet()) {
            String id = entry.getKey();
            JsonObject component = entry.getValue();
            result.bind(id, component.has("block") ? component.get("block").getAsString() : "VIRTUAL");
            if ("ITEM_FRAME".equals(optional(component, "block"))) {
                result.frames(id, frameItems(component), frameRotations(component));
            }
            if (component.has("book_pdc")) {
                result.book(id, item("WRITTEN_BOOK", stringMap(
                        component.getAsJsonObject("book_pdc")), 1));
            }
            if ("BARREL".equals(optional(component, "block"))) {
                inventories.put(id, new HashMap<>());
            }
            if ("CHISELED_BOOKSHELF".equals(optional(component, "block"))) {
                shelves.put(id, new HashMap<>());
            }
        }
        if (predicate.has("accepted")) {
            result.answer(predicate.getAsJsonArray("accepted").get(0).getAsString());
        }

        int calibrationIndex = 0;
        for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = operationElement.getAsJsonObject();
            String op = operation.get("op").getAsString();
            switch (op) {
                case "map_cutouts_reveal_exact" ->
                        result.stringList(op, strings(operation.getAsJsonArray("values")));
                case "landmark_relation" -> {
                    result.stringFact(op, operation.get("reveals").getAsString());
                    result.stringList(op + ":north_of",
                            strings(operation.getAsJsonArray("north_of")));
                }
                case "water_present_below_frames" -> operation.getAsJsonArray("offsets")
                        .forEach(value -> result.water(offset(value.getAsJsonArray())));
                case "bookshelf_mask_exact" -> fillMask(
                        shelves.computeIfAbsent(component(operation), ignored -> new HashMap<>()),
                        operation.get("value").getAsString(), "bi01_wick_segment_", 0);
                case "bookshelf_masks_exact" -> {
                    JsonObject values = operation.getAsJsonObject("values");
                    for (Map.Entry<String, JsonElement> mask : values.entrySet()) {
                        calibrationIndex = fillMask(
                                shelves.computeIfAbsent(mask.getKey(), ignored -> new HashMap<>()),
                                mask.getValue().getAsString(), "hs04_calibration_", calibrationIndex);
                    }
                }
                case "actor_read_or_inspected_exact_sources",
                        "actor_opened_or_inspected_exact_station_source" ->
                        operation.getAsJsonArray("components")
                                .forEach(value -> result.openedSource(value.getAsString()));
                case "player_opened_exact_sources" -> operation.getAsJsonArray("ids")
                        .forEach(value -> result.openedSource(value.getAsString()));
                case "player_opened_exact_book" -> {
                    if (operation.has("components")) {
                        operation.getAsJsonArray("components")
                                .forEach(value -> result.openedBook(value.getAsString()));
                    } else {
                        result.openedBook(component(operation));
                    }
                }
                case "slots_contain_unique_pdc_receipts" -> receipts(
                        node.nodeId(), actor, operation, components, inventories, "receipt_ids");
                case "slots_contain_receipts_in_exact_order" -> receipts(
                        node.nodeId(), actor, operation, components, inventories, "ids");
                case "slots_match_fields" -> fields(operation, components, inventories);
                case "slot_exact_item" -> exactSlot(operation, components, inventories);
                case "bookshelf_slot_exact_item" -> shelves
                        .computeIfAbsent(component(operation), ignored -> new HashMap<>())
                        .put(operation.get("slot").getAsInt(), item("CLOCK",
                                stringMap(operation.getAsJsonObject("item_pdc")), 1));
                case "selector_value" -> result.selector(
                        component(operation), operation.get("value").getAsString());
                case "handle_belongs_to" -> result.handle(component(operation));
                case "session_event_seen" -> result.sessionEvent(component(operation));
                case "player_in_cell_posture_for_ticks" -> {
                    String component = component(operation);
                    result.booleanFact(PhysicalPredicateEvaluator.factKey(op, component), true);
                    result.integerFact(PhysicalPredicateEvaluator.factKey("hold_ticks", component),
                            components.get(component).get("hold_ticks").getAsInt());
                }
                case "route_sequence_exact" -> result.booleanFact(
                        PhysicalPredicateEvaluator.factKey(op, component(operation)), true);
                case "actor_operates" -> result.operated(component(operation));
                case "actor_did_not_operate" -> result.didNotOperate(component(operation));
                case "actor_view_side" -> result.viewSide(operation.get("value").getAsString());
                case "destination_inventory_has_capacity" ->
                        result.integerFact(op, operation.get("amount").getAsInt());
                case "player_in_cell" -> result.booleanFact(
                        PhysicalPredicateEvaluator.factKey(op, component(operation)), true);
                case "player_posture" -> result.stringFact(op, operation.get("value").getAsString());
                case "ray_intersects_mark_sequence" ->
                        result.stringList(op, strings(operation.getAsJsonArray("values")));
                case "continuous_ticks" ->
                        result.integerFact(op, operation.get("value").getAsInt());
                case "normalized_answer_in", "frame_sequence_exact", "frame_rotations_exact",
                        "frame_rotation_exact", "frame_ids_exact_in_order",
                        "bookshelf_slot_empty", "all_manifest_items_present_once",
                        "prior_receipt_true" -> { }
                default -> throw new AssertionError("unhandled test operation " + op);
            }
        }
        inventories.forEach(result::inventory);
        shelves.forEach(result::bookshelf);
        return result.build();
    }

    public static List<String> rewardArtifactIds(PhysicalPredicateAuthority.Node node) {
        JsonObject reward = JsonParser.parseString(node.rewardJson()).getAsJsonObject();
        return strings(reward.getAsJsonArray("artifact_ids"));
    }

    public static List<String> acceptedAnswers(PhysicalPredicateAuthority.Node node) {
        JsonObject predicate = JsonParser.parseString(
                node.predicate().canonicalJson()).getAsJsonObject();
        return predicate.has("accepted")
                ? strings(predicate.getAsJsonArray("accepted")) : List.of();
    }

    private static List<MechanicItem> frameItems(JsonObject component) {
        String material = optional(component, "material");
        if (material.isEmpty()) {
            material = optional(component, "item_material");
        }
        List<MechanicItem> result = new ArrayList<>();
        if (component.has("required_items")) {
            String key = optional(component, "pdc_key");
            for (JsonElement value : component.getAsJsonArray("required_items")) {
                if (value.isJsonObject()) {
                    JsonObject expected = value.getAsJsonObject();
                    result.add(item(expected.get("material").getAsString(),
                            expected.has("pdc") ? stringMap(expected.getAsJsonObject("pdc")) : Map.of(),
                            1));
                } else {
                    result.add(item(material, Map.of(key, value.getAsString()), 1));
                }
            }
            return result;
        }
        if (component.has("pdc_values")) {
            String key = optional(component, "pdc_key");
            if (key.isEmpty()) {
                key = "v5_control_id";
            }
            for (JsonElement value : component.getAsJsonArray("pdc_values")) {
                result.add(item(material, Map.of(key, value.getAsString()), 1));
            }
            return result;
        }
        int count = component.has("count") ? component.get("count").getAsInt() : 1;
        if (component.has("offsets")) {
            count = component.getAsJsonArray("offsets").size();
        } else if (component.has("ordered_offsets")) {
            count = component.getAsJsonArray("ordered_offsets").size();
        } else if (component.has("site_series")) {
            count = component.getAsJsonArray("site_series").size();
        }
        Map<String, String> direct = component.has("pdc")
                ? stringMap(component.getAsJsonObject("pdc")) : Map.of();
        String prefix = optional(component, "pdc_prefix");
        for (int index = 0; index < count; index++) {
            Map<String, String> pdc = direct;
            if (!prefix.isEmpty()) {
                pdc = Map.of("v5_control_id", prefix + (index + 1));
            }
            result.add(item(material, pdc, 1));
        }
        return result;
    }

    private static List<Integer> frameRotations(JsonObject component) {
        if (component.has("required_rotations")) {
            List<Integer> result = new ArrayList<>();
            component.getAsJsonArray("required_rotations")
                    .forEach(value -> result.add(value.getAsInt()));
            return List.copyOf(result);
        }
        return List.of(component.has("required_rotation")
                ? component.get("required_rotation").getAsInt() : 0);
    }

    private static int fillMask(
            Map<Integer, MechanicItem> shelf,
            String mask,
            String prefix,
            int startIndex) {
        int index = startIndex;
        for (int slot = 0; slot < mask.length(); slot++) {
            if (mask.charAt(slot) == '1') {
                index++;
                String material = prefix.startsWith("hs04_") ? "WRITTEN_BOOK" : "BOOK";
                shelf.put(slot, item(material, Map.of("v5_evidence_id", prefix + index), 1));
            }
        }
        return index;
    }

    private static void receipts(
            String nodeId,
            UUID actor,
            JsonObject operation,
            Map<String, JsonObject> components,
            Map<String, Map<Integer, MechanicItem>> inventories,
            String listKey) {
        String component = component(operation);
        JsonObject slots = components.get(component).getAsJsonObject("slots");
        Map<Integer, MechanicItem> inventory = inventories.computeIfAbsent(
                component, ignored -> new HashMap<>());
        for (JsonElement idElement : operation.getAsJsonArray(listKey)) {
            String id = idElement.getAsString();
            inventory.put(slots.get(id).getAsInt(), item("PAPER", Map.of(
                    "v5_receipt_id", id,
                    "v5_receipt_node", nodeId,
                    "v5_receipt_actor", actor.toString()), 1));
        }
    }

    private static void fields(
            JsonObject operation,
            Map<String, JsonObject> components,
            Map<String, Map<Integer, MechanicItem>> inventories) {
        String component = component(operation);
        JsonObject definition = components.get(component);
        JsonObject slots = definition.getAsJsonObject("slots");
        Map<Integer, MechanicItem> inventory = inventories.computeIfAbsent(
                component, ignored -> new HashMap<>());
        for (JsonElement requiredElement : definition.getAsJsonArray("required")) {
            JsonObject required = requiredElement.getAsJsonObject();
            Map<String, String> pdc = stringMap(required.getAsJsonObject("pdc"));
            String shortName = pdc.get("v5_audit_field").replace("_missing", "");
            inventory.put(slots.get(shortName).getAsInt(), item(
                    required.get("material").getAsString(), pdc, 1));
        }
    }

    private static void exactSlot(
            JsonObject operation,
            Map<String, JsonObject> components,
            Map<String, Map<Integer, MechanicItem>> inventories) {
        String componentId = component(operation);
        JsonObject component = components.get(componentId);
        int slot = component.has("slot") ? component.get("slot").getAsInt() : 13;
        MechanicItem item;
        if (operation.has("item_pdc")) {
            String material = component.has("required_item")
                    ? component.getAsJsonObject("required_item").get("material").getAsString()
                    : "WRITTEN_BOOK";
            item = item(material, stringMap(operation.getAsJsonObject("item_pdc")),
                    operation.get("amount").getAsInt());
        } else {
            JsonObject named = components.get(operation.get("item").getAsString());
            item = item(named.get("material").getAsString(),
                    named.has("pdc") ? stringMap(named.getAsJsonObject("pdc")) : Map.of(),
                    operation.get("amount").getAsInt());
        }
        inventories.computeIfAbsent(componentId, ignored -> new HashMap<>()).put(slot, item);
    }

    private static MechanicItem item(String material, Map<String, String> sourcePdc, int amount) {
        Map<String, String> pdc = new LinkedHashMap<>(sourcePdc);
        Optional<UUID> instance = Optional.empty();
        if (pdc.containsKey(MechanicItem.ARTIFACT_ID)) {
            UUID value = UUID.randomUUID();
            pdc.put(MechanicItem.ARTIFACT_INSTANCE, value.toString());
            instance = Optional.of(value);
        }
        return new MechanicItem(material.isEmpty() ? "PAPER" : material, amount, pdc, instance);
    }

    private static Map<String, JsonObject> components(JsonObject predicate) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        predicate.getAsJsonArray("components").forEach(value -> {
            JsonObject component = value.getAsJsonObject();
            result.put(component.get("id").getAsString(), component);
        });
        return result;
    }

    private static String component(JsonObject operation) {
        return operation.get("component").getAsString();
    }

    private static String optional(JsonObject value, String key) {
        return value.has(key) ? value.get(key).getAsString() : "";
    }

    private static Map<String, String> stringMap(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return result;
    }

    private static List<String> strings(JsonArray values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }

    private static LocalOffset offset(JsonArray values) {
        return new LocalOffset(
                values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
    }
}
