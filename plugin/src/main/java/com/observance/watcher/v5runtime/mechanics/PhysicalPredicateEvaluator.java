package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.AnswerNormalizer;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Strict, Paper-independent interpreter for all S/I/F/L/R authority operations. */
public final class PhysicalPredicateEvaluator {
    private static final Set<String> SUPPORTED_OPERATIONS = Set.of(
            "actor_did_not_operate",
            "actor_opened_or_inspected_exact_station_source",
            "actor_operates",
            "actor_read_or_inspected_exact_sources",
            "actor_view_side",
            "all_eligible_players_confirmed_within_window",
            "all_eligible_players_on_distinct_sectors",
            "all_manifest_items_present_once",
            "bookshelf_mask_exact",
            "bookshelf_masks_exact",
            "bookshelf_slot_empty",
            "bookshelf_slot_exact_item",
            "branch_specific_bridge_operation_complete",
            "continuous_ticks",
            "destination_inventory_has_capacity",
            "frame_ids_exact_in_order",
            "frame_rotation_exact",
            "frame_rotations_exact",
            "frame_sequence_exact",
            "handle_belongs_to",
            "landmark_relation",
            "map_cutouts_reveal_exact",
            "normalized_answer_in",
            "player_in_cell",
            "player_in_cell_posture_for_ticks",
            "player_opened_exact_book",
            "player_opened_exact_sources",
            "player_posture",
            "prior_receipt_true",
            "ray_intersects_mark_sequence",
            "route_sequence_exact",
            "selector_value",
            "session_event_seen",
            "slot_exact_item",
            "slots_contain_receipts_in_exact_order",
            "slots_contain_unique_pdc_receipts",
            "slots_match_fields",
            "water_present_below_frames");

    public PredicateEvaluation evaluate(
            PhysicalPredicateAuthority.Node node,
            MechanicObservation observation,
            EvaluationEnvironment environment) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(environment, "environment");
        if (!AssignedPhysicalNodes.implementedNodeIds().contains(node.nodeId())) {
            throw new IllegalArgumentException("node is not owned by this mechanics engine: "
                    + node.nodeId());
        }
        if (!node.siteId().equals(observation.siteId())) {
            return PredicateEvaluation.failure("site_identity");
        }

        JsonObject predicate = parseObject(node.predicate().canonicalJson(), "predicate");
        Map<String, JsonObject> components = components(predicate);
        JsonArray allOf = requireArray(predicate, "all_of");
        if (allOf.isEmpty()) {
            throw new IllegalStateException(node.nodeId() + " has no all_of operations");
        }
        for (JsonElement element : allOf) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException(node.nodeId() + " all_of entry is not an object");
            }
            JsonObject operation = element.getAsJsonObject();
            String op = string(operation, "op");
            if (!SUPPORTED_OPERATIONS.contains(op)) {
                throw new IllegalStateException(node.nodeId() + " uses unsupported operation " + op);
            }
            if (!evaluateOperation(
                    node, predicate, components, operation, observation, environment, op)) {
                return PredicateEvaluation.failure(op);
            }
        }
        return PredicateEvaluation.success();
    }

    public static Set<String> supportedOperations() {
        return SUPPORTED_OPERATIONS;
    }

    /** Static coverage proof: every owned node has only implemented operations. */
    public static void validateCoverage(PhysicalPredicateAuthority authority) {
        AssignedPhysicalNodes.validateAgainst(authority);
        for (String nodeId : AssignedPhysicalNodes.implementedNodeIds()) {
            PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
            JsonObject predicate = parseObject(node.predicate().canonicalJson(), nodeId);
            JsonArray allOf = requireArray(predicate, "all_of");
            if (allOf.isEmpty()) {
                throw new IllegalStateException(nodeId + " has no all_of operations");
            }
            for (JsonElement entry : allOf) {
                String operation = string(entry.getAsJsonObject(), "op");
                if (!SUPPORTED_OPERATIONS.contains(operation)) {
                    throw new IllegalStateException(
                            nodeId + " has no executable adapter for operation " + operation);
                }
            }
        }
    }

    private boolean evaluateOperation(
            PhysicalPredicateAuthority.Node node,
            JsonObject predicate,
            Map<String, JsonObject> components,
            JsonObject operation,
            MechanicObservation observation,
            EvaluationEnvironment environment,
            String op) {
        return switch (op) {
            case "normalized_answer_in" -> normalizedAnswer(predicate, observation);
            case "frame_sequence_exact" -> frameSequence(
                    component(operation), operation, components, observation);
            case "frame_rotations_exact" -> frameRotations(operation, components, observation);
            case "frame_rotation_exact" -> singleFrameRotation(operation, components, observation);
            case "frame_ids_exact_in_order" -> frameIds(operation, components, observation);
            case "map_cutouts_reveal_exact" -> observation.stringLists()
                    .getOrDefault(op, List.of()).equals(strings(operation.getAsJsonArray("values")));
            case "landmark_relation" -> landmarkRelation(operation, observation);
            case "water_present_below_frames" -> water(operation, observation);
            case "bookshelf_mask_exact" -> shelfMask(
                    component(operation), string(operation, "value"), components, observation);
            case "bookshelf_masks_exact" -> shelfMasks(operation, components, observation);
            case "all_manifest_items_present_once" -> manifestShelfItems(
                    operation, components, observation);
            case "actor_read_or_inspected_exact_sources",
                    "actor_opened_or_inspected_exact_station_source" ->
                    openedComponents(node.nodeId(), operation, observation, environment);
            case "player_opened_exact_sources" ->
                    openedIds(node.nodeId(), operation, observation, environment);
            case "player_opened_exact_book" ->
                    openedBooks(node.nodeId(), operation, observation, environment);
            case "slots_contain_unique_pdc_receipts" ->
                    uniqueReceipts(node.nodeId(), operation, components, observation);
            case "slots_contain_receipts_in_exact_order" ->
                    orderedReceipts(node.nodeId(), operation, components, observation);
            case "slots_match_fields" -> fields(operation, components, observation);
            case "slot_exact_item" -> exactSlot(operation, components, observation);
            case "bookshelf_slot_exact_item" -> exactShelfSlot(operation, components, observation);
            case "bookshelf_slot_empty" -> emptyShelfSlot(operation, components, observation);
            case "selector_value" -> string(operation, "value").equals(
                    observation.selectorValues().get(component(operation)));
            case "handle_belongs_to" -> observation.handleComponent()
                    .filter(component(operation)::equals).isPresent();
            case "session_event_seen" -> eventSeen(
                    node.nodeId(), component(operation), observation, environment);
            case "prior_receipt_true" -> environment.trueFlags().contains(string(operation, "flag"));
            case "player_in_cell_posture_for_ticks" -> poseForTicks(
                    component(operation), components, observation);
            case "route_sequence_exact" -> observation.booleanFact(factKey(op, component(operation)));
            case "actor_operates" -> observation.actorOperated().contains(component(operation));
            case "actor_did_not_operate" -> observation.actorDidNotOperate()
                    .contains(component(operation));
            case "actor_view_side" -> observation.actorViewSide()
                    .filter(string(operation, "value")::equals).isPresent();
            case "destination_inventory_has_capacity" -> observation.integerFact(op)
                    >= integer(operation, "amount");
            case "all_eligible_players_on_distinct_sectors",
                    "all_eligible_players_confirmed_within_window",
                    "branch_specific_bridge_operation_complete" -> observation.booleanFact(op);
            case "player_in_cell" -> observation.booleanFact(factKey(op, component(operation)));
            case "player_posture" -> string(operation, "value")
                    .equals(observation.stringFacts().get(op));
            case "ray_intersects_mark_sequence" -> observation.stringLists()
                    .getOrDefault(op, List.of()).equals(strings(operation.getAsJsonArray("values")));
            case "continuous_ticks" -> observation.integerFact(op) >= integer(operation, "value");
            default -> throw new IllegalStateException("unreachable operation " + op);
        };
    }

    private static boolean normalizedAnswer(
            JsonObject predicate, MechanicObservation observation) {
        if (observation.answer().isEmpty()) {
            return false;
        }
        String normalized = AnswerNormalizer.normalize(observation.answer().orElseThrow());
        for (String accepted : strings(requireArray(predicate, "accepted"))) {
            if (normalized.equals(AnswerNormalizer.normalize(accepted))) {
                return true;
            }
        }
        return false;
    }

    private static boolean frameSequence(
            String componentId,
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        if (!frameIdentity(componentId, components, observation)) {
            return false;
        }
        MechanicObservation.FrameState actual = observation.frames().get(componentId);
        return valuesForItems(actual.items()).equals(strings(operation.getAsJsonArray("item_order")))
                && actual.rotations().equals(integers(operation.getAsJsonArray("rotations")));
    }

    private static boolean frameRotations(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        JsonElement values = operation.get("values");
        if (values == null) {
            return false;
        }
        if (values.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : values.getAsJsonObject().entrySet()) {
                String componentId = entry.getKey();
                if (!frameIdentity(componentId, components, observation)) {
                    return false;
                }
                MechanicObservation.FrameState state = observation.frames().get(componentId);
                if (state.rotations().size() != 1
                        || state.rotations().getFirst() != entry.getValue().getAsInt()) {
                    return false;
                }
            }
            return true;
        }
        String componentId = operation.has("component")
                ? component(operation) : onlyFrameComponent(components);
        return frameIdentity(componentId, components, observation)
                && observation.frames().get(componentId).rotations()
                        .equals(integers(values.getAsJsonArray()));
    }

    private static boolean singleFrameRotation(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        if (!frameIdentity(componentId, components, observation)) {
            return false;
        }
        List<Integer> rotations = observation.frames().get(componentId).rotations();
        return rotations.size() == 1 && rotations.getFirst() == integer(operation, "value");
    }

    private static boolean frameIds(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = operation.has("component")
                ? component(operation) : onlyFrameComponent(components);
        return frameIdentity(componentId, components, observation)
                && valuesForItems(observation.frames().get(componentId).items())
                        .equals(strings(operation.getAsJsonArray("ids")));
    }

    private static boolean frameIdentity(
            String componentId,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        JsonObject definition = components.get(componentId);
        MechanicObservation.FrameState actual = observation.frames().get(componentId);
        if (definition == null || actual == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        String block = optionalString(definition, "block");
        if (!block.isEmpty() && !"ITEM_FRAME".equals(block)) {
            return false;
        }
        List<ExpectedItem> expected = expectedFrameItems(definition);
        if (expected.size() != actual.items().size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            if (!expected.get(index).matches(actual.items().get(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<ExpectedItem> expectedFrameItems(JsonObject definition) {
        String material = optionalString(definition, "material");
        if (material.isEmpty()) {
            material = optionalString(definition, "item_material");
        }
        if (definition.has("required_items")) {
            List<ExpectedItem> result = new ArrayList<>();
            String pdcKey = optionalString(definition, "pdc_key");
            for (JsonElement entry : definition.getAsJsonArray("required_items")) {
                if (entry.isJsonObject()) {
                    result.add(expected(entry.getAsJsonObject()));
                } else {
                    result.add(new ExpectedItem(material, Map.of(pdcKey, entry.getAsString()), ""));
                }
            }
            return result;
        }
        if (definition.has("pdc_values")) {
            String key = optionalString(definition, "pdc_key");
            if (key.isEmpty()) {
                key = "v5_control_id";
            }
            List<ExpectedItem> result = new ArrayList<>();
            for (String value : strings(definition.getAsJsonArray("pdc_values"))) {
                result.add(new ExpectedItem(material, Map.of(key, value), ""));
            }
            return result;
        }
        int count = definition.has("count") ? definition.get("count").getAsInt() : 1;
        if (definition.has("offsets")) {
            count = definition.getAsJsonArray("offsets").size();
        }
        if (definition.has("ordered_offsets")) {
            count = definition.getAsJsonArray("ordered_offsets").size();
        }
        if (definition.has("site_series")) {
            count = definition.getAsJsonArray("site_series").size();
        }
        List<ExpectedItem> result = new ArrayList<>();
        Map<String, String> pdc = definition.has("pdc")
                ? stringMap(definition.getAsJsonObject("pdc")) : Map.of();
        String prefix = optionalString(definition, "pdc_prefix");
        for (int index = 0; index < count; index++) {
            result.add(new ExpectedItem(material, pdc, prefix));
        }
        return result;
    }

    private static boolean water(JsonObject operation, MechanicObservation observation) {
        for (JsonElement entry : requireArray(operation, "offsets")) {
            List<Integer> offset = integers(entry.getAsJsonArray());
            if (offset.size() != 3 || !observation.waterOffsets().contains(
                    new LocalOffset(offset.get(0), offset.get(1), offset.get(2)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean shelfMask(
            String componentId,
            String expected,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        if (!observation.boundComponents().contains(componentId)
                || !components.containsKey(componentId)) {
            return false;
        }
        Map<Integer, MechanicItem> slots = observation.bookshelfSlots()
                .getOrDefault(componentId, Map.of());
        StringBuilder mask = new StringBuilder(6);
        for (int slot = 0; slot < 6; slot++) {
            mask.append(slots.containsKey(slot) ? '1' : '0');
        }
        return mask.toString().equals(expected);
    }

    private static boolean shelfMasks(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        for (Map.Entry<String, JsonElement> entry : operation.getAsJsonObject("values").entrySet()) {
            if (!shelfMask(entry.getKey(), entry.getValue().getAsString(), components, observation)) {
                return false;
            }
        }
        return true;
    }

    private static boolean manifestShelfItems(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        int uniqueCount = integer(operation, "unique_count");
        JsonObject definition = components.values().stream()
                .filter(value -> value.has("pdc_prefix") && value.has("required_unique_count"))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "all_manifest_items_present_once lacks a calibration component"));
        String material = string(definition, "material");
        String prefix = string(definition, "pdc_prefix");
        Set<String> identities = new HashSet<>();
        int occupied = 0;
        for (Map.Entry<String, Map<Integer, MechanicItem>> shelf :
                observation.bookshelfSlots().entrySet()) {
            if (!components.containsKey(shelf.getKey())
                    || !"CHISELED_BOOKSHELF".equals(optionalString(components.get(shelf.getKey()), "block"))) {
                continue;
            }
            for (MechanicItem item : shelf.getValue().values()) {
                occupied++;
                if (!item.material().equals(material)) {
                    return false;
                }
                String identity = prefixedPdcValue(item, prefix);
                if (identity == null || !identities.add(identity)) {
                    return false;
                }
            }
        }
        return occupied == uniqueCount && identities.size() == uniqueCount;
    }

    private static boolean openedComponents(
            String nodeId,
            JsonObject operation,
            MechanicObservation observation,
            EvaluationEnvironment environment) {
        for (String source : strings(operation.getAsJsonArray("components"))) {
            if (!seen(nodeId, source, observation.openedSources(), environment.inspectionBits())) {
                return false;
            }
        }
        return true;
    }

    private static boolean openedIds(
            String nodeId,
            JsonObject operation,
            MechanicObservation observation,
            EvaluationEnvironment environment) {
        for (String source : strings(operation.getAsJsonArray("ids"))) {
            if (!seen(nodeId, source, observation.openedSources(), environment.inspectionBits())) {
                return false;
            }
        }
        return true;
    }

    private static boolean openedBooks(
            String nodeId,
            JsonObject operation,
            MechanicObservation observation,
            EvaluationEnvironment environment) {
        List<String> required = operation.has("components")
                ? strings(operation.getAsJsonArray("components"))
                : List.of(component(operation));
        for (String source : required) {
            if (!seen(nodeId, source, observation.openedBooks(), environment.inspectionBits())) {
                return false;
            }
        }
        return true;
    }

    private static boolean uniqueReceipts(
            String nodeId,
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        JsonObject definition = components.get(componentId);
        if (definition == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        Map<Integer, MechanicItem> slots = observation.inventories()
                .getOrDefault(componentId, Map.of());
        JsonObject slotMap = definition.getAsJsonObject("slots");
        Set<String> seen = new HashSet<>();
        for (String receipt : strings(operation.getAsJsonArray("receipt_ids"))) {
            int slot = slotMap.get(receipt).getAsInt();
            MechanicItem item = slots.get(slot);
            if (!receiptItem(item, nodeId, observation.actor(), receipt) || !seen.add(receipt)) {
                return false;
            }
        }
        return true;
    }

    private static boolean orderedReceipts(
            String nodeId,
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        JsonObject definition = components.get(componentId);
        if (definition == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        Map<Integer, MechanicItem> slots = observation.inventories()
                .getOrDefault(componentId, Map.of());
        JsonObject slotMap = definition.getAsJsonObject("slots");
        for (String id : strings(operation.getAsJsonArray("ids"))) {
            JsonElement slot = slotMap.get(id);
            if (slot == null || !receiptItem(
                    slots.get(slot.getAsInt()), nodeId, observation.actor(), id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean fields(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        JsonObject definition = components.get(componentId);
        if (definition == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        Map<String, JsonObject> requiredByField = new HashMap<>();
        for (JsonElement entry : definition.getAsJsonArray("required")) {
            JsonObject requirement = entry.getAsJsonObject();
            requiredByField.put(requirement.getAsJsonObject("pdc")
                    .get("v5_audit_field").getAsString(), requirement);
        }
        JsonObject slots = definition.getAsJsonObject("slots");
        Map<Integer, MechanicItem> inventory = observation.inventories()
                .getOrDefault(componentId, Map.of());
        for (Map.Entry<String, JsonElement> expected :
                operation.getAsJsonObject("values").entrySet()) {
            JsonObject requirement = requiredByField.get(expected.getKey());
            if (requirement == null) {
                return false;
            }
            String shortName = expected.getKey().replace("_missing", "");
            JsonElement slotElement = slots.get(shortName);
            if (slotElement == null || !expected(requirement)
                    .matches(inventory.get(slotElement.getAsInt()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean exactSlot(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        JsonObject component = components.get(componentId);
        if (component == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        int slot = component.has("slot") ? component.get("slot").getAsInt() : 13;
        MechanicItem item = observation.inventories().getOrDefault(componentId, Map.of()).get(slot);
        ExpectedItem expected;
        if (operation.has("item_pdc")) {
            String material = component.has("required_item")
                    ? optionalString(component.getAsJsonObject("required_item"), "material") : "";
            expected = new ExpectedItem(material,
                    stringMap(operation.getAsJsonObject("item_pdc")), "");
        } else {
            JsonObject itemComponent = components.get(string(operation, "item"));
            if (itemComponent == null) {
                return false;
            }
            expected = expected(itemComponent);
        }
        return item != null && item.amount() == integer(operation, "amount")
                && expected.matches(item);
    }

    private static boolean exactShelfSlot(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        if (!components.containsKey(componentId) || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        int slot = integer(operation, "slot");
        MechanicItem item = observation.bookshelfSlots().getOrDefault(componentId, Map.of()).get(slot);
        return item != null && new ExpectedItem("", stringMap(
                operation.getAsJsonObject("item_pdc")), "").matches(item);
    }

    private static boolean emptyShelfSlot(
            JsonObject operation,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        String componentId = component(operation);
        return components.containsKey(componentId)
                && observation.boundComponents().contains(componentId)
                && !observation.bookshelfSlots().getOrDefault(componentId, Map.of())
                        .containsKey(integer(operation, "slot"));
    }

    private static boolean poseForTicks(
            String componentId,
            Map<String, JsonObject> components,
            MechanicObservation observation) {
        JsonObject component = components.get(componentId);
        if (component == null || !observation.boundComponents().contains(componentId)) {
            return false;
        }
        int ticks = component.has("hold_ticks") ? component.get("hold_ticks").getAsInt() : 1;
        return observation.booleanFact(factKey("player_in_cell_posture_for_ticks", componentId))
                && observation.integerFact(factKey("hold_ticks", componentId)) >= ticks;
    }

    private static boolean landmarkRelation(
            JsonObject operation, MechanicObservation observation) {
        String reveal = string(operation, "reveals");
        return reveal.equals(observation.stringFacts().get("landmark_relation"))
                && observation.stringLists().getOrDefault("landmark_relation:north_of", List.of())
                        .equals(strings(operation.getAsJsonArray("north_of")));
    }

    private static boolean eventSeen(
            String nodeId,
            String component,
            MechanicObservation observation,
            EvaluationEnvironment environment) {
        return observation.sessionEvents().contains(component)
                || environment.inspectionBits().contains(sessionBit(nodeId, component));
    }

    public static String inspectionBit(String nodeId, String source) {
        return "v5:" + nodeId + ":inspection:" + source;
    }

    public static String sessionBit(String nodeId, String event) {
        return "v5:" + nodeId + ":session:" + event;
    }

    public static String routeBit(String nodeId) {
        return "v5:" + nodeId + ":route_complete";
    }

    public static String factKey(String operation, String component) {
        return operation + ':' + component;
    }

    private static boolean seen(
            String nodeId, String source, Set<String> transientSeen, Set<String> persisted) {
        return transientSeen.contains(source) || persisted.contains(inspectionBit(nodeId, source));
    }

    private static boolean receiptItem(
            MechanicItem item, String nodeId, java.util.UUID actor, String receiptId) {
        return item != null && "PAPER".equals(item.material()) && item.amount() == 1
                && receiptId.equals(item.pdc().get("v5_receipt_id"))
                && nodeId.equals(item.pdc().get("v5_receipt_node"))
                && actor.toString().equals(item.pdc().get("v5_receipt_actor"));
    }

    private static String shelfMask(Map<Integer, MechanicItem> slots) {
        StringBuilder result = new StringBuilder(6);
        for (int slot = 0; slot < 6; slot++) {
            result.append(slots.containsKey(slot) ? '1' : '0');
        }
        return result.toString();
    }

    private static String prefixedPdcValue(MechanicItem item, String prefix) {
        String match = null;
        for (String value : item.pdc().values()) {
            if (value.startsWith(prefix)) {
                if (match != null) {
                    return null;
                }
                match = value;
            }
        }
        return match;
    }

    private static List<String> valuesForItems(List<MechanicItem> items) {
        List<String> values = new ArrayList<>();
        for (MechanicItem item : items) {
            String value = item.pdc().get("v5_evidence_id");
            if (value == null) {
                value = item.pdc().get("v5_control_id");
            }
            if (value == null) {
                return List.of();
            }
            values.add(value);
        }
        return values;
    }

    private static ExpectedItem expected(JsonObject definition) {
        String material = optionalString(definition, "material");
        Map<String, String> pdc = definition.has("pdc")
                ? stringMap(definition.getAsJsonObject("pdc")) : Map.of();
        return new ExpectedItem(material, pdc, optionalString(definition, "pdc_prefix"));
    }

    private record ExpectedItem(String material, Map<String, String> pdc, String prefix) {
        private ExpectedItem {
            material = material == null ? "" : material;
            pdc = Map.copyOf(pdc);
            prefix = prefix == null ? "" : prefix;
        }

        private boolean matches(MechanicItem actual) {
            if (actual == null || (!material.isEmpty() && !material.equals(actual.material()))) {
                return false;
            }
            for (Map.Entry<String, String> entry : pdc.entrySet()) {
                if (!entry.getValue().equals(actual.pdc().get(entry.getKey()))) {
                    return false;
                }
            }
            return prefix.isEmpty() || prefixedPdcValue(actual, prefix) != null;
        }
    }

    private static Map<String, JsonObject> components(JsonObject predicate) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement element : requireArray(predicate, "components")) {
            JsonObject component = element.getAsJsonObject();
            String id = string(component, "id");
            if (result.put(id, component) != null) {
                throw new IllegalStateException("duplicate predicate component " + id);
            }
        }
        return Map.copyOf(result);
    }

    private static String onlyFrameComponent(Map<String, JsonObject> components) {
        List<String> frames = components.entrySet().stream()
                .filter(entry -> "ITEM_FRAME".equals(optionalString(entry.getValue(), "block")))
                .map(Map.Entry::getKey).toList();
        if (frames.size() != 1) {
            throw new IllegalStateException("operation has no unambiguous frame component: " + frames);
        }
        return frames.getFirst();
    }

    private static String component(JsonObject operation) {
        return string(operation, "component");
    }

    private static JsonObject parseObject(String json, String label) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) {
            throw new IllegalStateException(label + " is not a JSON object");
        }
        return parsed.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException(key + " is not an array");
        }
        return value.getAsJsonArray();
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(key + " is not a string");
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject object, String key) {
        return object.has(key) ? string(object, key) : "";
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException(key + " is not an integer");
        }
        return value.getAsInt();
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement value : array) {
            result.add(value.getAsString());
        }
        return List.copyOf(result);
    }

    private static List<Integer> integers(JsonArray array) {
        List<Integer> result = new ArrayList<>();
        for (JsonElement value : array) {
            result.add(value.getAsInt());
        }
        return List.copyOf(result);
    }

    private static Map<String, String> stringMap(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return Map.copyOf(result);
    }
}
