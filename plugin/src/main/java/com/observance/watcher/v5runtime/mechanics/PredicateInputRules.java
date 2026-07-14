package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Exact managed-slot allowlist used before Bukkit inventory mutation. */
public final class PredicateInputRules {
    private final PhysicalPredicateAuthority authority;

    public PredicateInputRules(PhysicalPredicateAuthority authority) {
        this.authority = Objects.requireNonNull(authority, "authority");
    }

    public Set<Integer> managedSlots(String nodeId, String componentId) {
        return rules(nodeId).getOrDefault(componentId, Map.of()).keySet();
    }

    public boolean allows(
            String nodeId,
            String componentId,
            int slot,
            MechanicItem item,
            java.util.UUID actor) {
        SlotRule rule = rules(nodeId).getOrDefault(componentId, Map.of()).get(slot);
        return rule != null && rule.matches(item, nodeId, actor);
    }

    public boolean isManagedInventory(String nodeId, String componentId) {
        return !rules(nodeId).getOrDefault(componentId, Map.of()).isEmpty();
    }

    public boolean isImmutableSource(String nodeId, String componentId) {
        JsonObject component = component(nodeId, componentId);
        return component != null && (component.has("book_pdc") || component.has("source_item")
                || component.has("source_items") || component.has("source_book")
                || component.has("page_receipts"));
    }

    public boolean frameReorderable(String nodeId, String componentId) {
        JsonObject predicate = predicate(nodeId);
        for (JsonElement entry : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = entry.getAsJsonObject();
            String op = operation.get("op").getAsString();
            if (("frame_sequence_exact".equals(op) || "frame_ids_exact_in_order".equals(op))
                    && (!operation.has("component")
                    || componentId.equals(operation.get("component").getAsString()))) {
                return true;
            }
        }
        return false;
    }

    public boolean allowsFrameItem(String nodeId, String componentId, MechanicItem item) {
        JsonObject component = component(nodeId, componentId);
        if (component == null || !"ITEM_FRAME".equals(optional(component, "block"))) {
            return false;
        }
        String material = optional(component, "material");
        if (material.isEmpty()) {
            material = optional(component, "item_material");
        }
        if (!material.isEmpty() && !material.equals(item.material())) {
            return false;
        }
        if (component.has("required_items")) {
            String key = optional(component, "pdc_key");
            for (JsonElement expectedElement : component.getAsJsonArray("required_items")) {
                if (expectedElement.isJsonObject()) {
                    JsonObject expected = expectedElement.getAsJsonObject();
                    if (new SlotRule(expected.get("material").getAsString(),
                            expected.has("pdc") ? stringMap(expected.getAsJsonObject("pdc"))
                                    : Map.of(), 1, false, "").matches(item, nodeId, null)) {
                        return true;
                    }
                } else if (expectedElement.getAsString().equals(item.pdc().get(key))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private Map<String, Map<Integer, SlotRule>> rules(String nodeId) {
        PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        Map<String, JsonObject> components = new HashMap<>();
        predicate.getAsJsonArray("components").forEach(value -> {
            JsonObject component = value.getAsJsonObject();
            components.put(component.get("id").getAsString(), component);
        });
        Map<String, Map<Integer, SlotRule>> result = new LinkedHashMap<>();
        for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
            JsonObject operation = operationElement.getAsJsonObject();
            String op = operation.get("op").getAsString();
            if ("slot_exact_item".equals(op)) {
                String componentId = operation.get("component").getAsString();
                JsonObject component = components.get(componentId);
                int slot = component.has("slot") ? component.get("slot").getAsInt() : 13;
                String material = "";
                Map<String, String> pdc;
                if (operation.has("item_pdc")) {
                    pdc = stringMap(operation.getAsJsonObject("item_pdc"));
                    if (component.has("required_item")) {
                        material = component.getAsJsonObject("required_item")
                                .get("material").getAsString();
                    }
                } else {
                    JsonObject named = components.get(operation.get("item").getAsString());
                    material = named.get("material").getAsString();
                    pdc = named.has("pdc") ? stringMap(named.getAsJsonObject("pdc")) : Map.of();
                }
                add(result, componentId, slot,
                        new SlotRule(material, pdc, operation.get("amount").getAsInt(), false, ""));
            } else if ("slots_contain_unique_pdc_receipts".equals(op)
                    || "slots_contain_receipts_in_exact_order".equals(op)) {
                String componentId = operation.get("component").getAsString();
                JsonObject slotMap = components.get(componentId).getAsJsonObject("slots");
                String listKey = operation.has("receipt_ids") ? "receipt_ids" : "ids";
                for (JsonElement idElement : operation.getAsJsonArray(listKey)) {
                    String receiptId = idElement.getAsString();
                    add(result, componentId, slotMap.get(receiptId).getAsInt(),
                            new SlotRule("PAPER", Map.of("v5_receipt_id", receiptId), 1,
                                    true, ""));
                }
            } else if ("slots_match_fields".equals(op)) {
                String componentId = operation.get("component").getAsString();
                JsonObject component = components.get(componentId);
                JsonObject slots = component.getAsJsonObject("slots");
                for (JsonElement requiredElement : component.getAsJsonArray("required")) {
                    JsonObject required = requiredElement.getAsJsonObject();
                    Map<String, String> pdc = stringMap(required.getAsJsonObject("pdc"));
                    String shortName = pdc.get("v5_audit_field").replace("_missing", "");
                    // The numbered audit rack deliberately offers decoys. Pre-mutation safety may
                    // constrain a slot to the correct field, but it must not reveal the correct
                    // value by rejecting every wrong number before the player pulls the handle.
                    Map<String, String> fieldOnly = Map.of(
                            "v5_audit_field", pdc.get("v5_audit_field"));
                    add(result, componentId, slots.get(shortName).getAsInt(),
                            new SlotRule(required.get("material").getAsString(), fieldOnly,
                                    1, false, ""));
                }
            } else if ("bookshelf_slot_exact_item".equals(op)) {
                add(result, operation.get("component").getAsString(), operation.get("slot").getAsInt(),
                        new SlotRule("", stringMap(operation.getAsJsonObject("item_pdc")), 1,
                                false, ""));
            } else if ("bookshelf_mask_exact".equals(op)) {
                String componentId = operation.get("component").getAsString();
                String prefix = components.get(componentId).get("pdc_book_prefix").getAsString();
                for (int slot = 0; slot < 6; slot++) {
                    add(result, componentId, slot,
                            new SlotRule("", Map.of(), 1, false, prefix));
                }
            } else if ("bookshelf_masks_exact".equals(op)) {
                JsonObject calibration = components.values().stream()
                        .filter(value -> value.has("pdc_prefix")
                                && value.has("required_unique_count"))
                        .findFirst().orElseThrow();
                String material = calibration.get("material").getAsString();
                String prefix = calibration.get("pdc_prefix").getAsString();
                for (String componentId : operation.getAsJsonObject("values").keySet()) {
                    for (int slot = 0; slot < 6; slot++) {
                        add(result, componentId, slot,
                                new SlotRule(material, Map.of(), 1, false, prefix));
                    }
                }
            }
        }
        Map<String, Map<Integer, SlotRule>> immutable = new LinkedHashMap<>();
        result.forEach((key, value) -> immutable.put(key, Map.copyOf(value)));
        return Map.copyOf(immutable);
    }

    private static void add(
            Map<String, Map<Integer, SlotRule>> target,
            String component,
            int slot,
            SlotRule rule) {
        SlotRule previous = target.computeIfAbsent(component, ignored -> new LinkedHashMap<>())
                .put(slot, rule);
        if (previous != null && !previous.equals(rule)) {
            throw new IllegalStateException("conflicting input rules for " + component + ':' + slot);
        }
    }

    private static Map<String, String> stringMap(JsonObject value) {
        Map<String, String> result = new LinkedHashMap<>();
        value.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return Map.copyOf(result);
    }

    private JsonObject predicate(String nodeId) {
        return JsonParser.parseString(authority.requireNode(nodeId).predicate().canonicalJson())
                .getAsJsonObject();
    }

    private JsonObject component(String nodeId, String componentId) {
        for (JsonElement value : predicate(nodeId).getAsJsonArray("components")) {
            JsonObject component = value.getAsJsonObject();
            if (componentId.equals(component.get("id").getAsString())) {
                return component;
            }
        }
        return null;
    }

    private static String optional(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    private record SlotRule(
            String material,
            Map<String, String> pdc,
            int amount,
            boolean actorBoundReceipt,
            String pdcPrefix) {
        private boolean matches(MechanicItem item, String nodeId, java.util.UUID actor) {
            if (item == null || item.amount() != amount
                    || (!material.isEmpty() && !material.equals(item.material()))) {
                return false;
            }
            for (Map.Entry<String, String> entry : pdc.entrySet()) {
                if (!entry.getValue().equals(item.pdc().get(entry.getKey()))) {
                    return false;
                }
            }
            if (!pdcPrefix.isEmpty() && item.pdc().values().stream()
                    .noneMatch(value -> value.startsWith(pdcPrefix))) {
                return false;
            }
            return !actorBoundReceipt || nodeId.equals(item.pdc().get("v5_receipt_node"))
                    && actor != null
                    && actor.toString().equals(item.pdc().get("v5_receipt_actor"));
        }
    }
}
