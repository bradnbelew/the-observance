package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

/** Maps exact inspected source identities to their authored generated receipt identity. */
public final class GeneratedReceiptCatalog {
    private final Map<String, Map<String, String>> receipts;
    private final Map<String, Map<String, java.util.List<String>>> pageReceipts;

    public GeneratedReceiptCatalog(PhysicalPredicateAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, Map<String, java.util.List<String>>> pageResult = new HashMap<>();
        for (String nodeId : AssignedPhysicalNodes.implementedNodeIds()) {
            JsonObject predicate = JsonParser.parseString(
                    authority.requireNode(nodeId).predicate().canonicalJson()).getAsJsonObject();
            Map<String, String> nodeReceipts = new HashMap<>();
            Map<String, java.util.List<String>> nodePages = new HashMap<>();
            Set<String> filedReceiptIds = new HashSet<>();
            for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
                JsonObject operation = operationElement.getAsJsonObject();
                String op = operation.get("op").getAsString();
                if ("slots_contain_unique_pdc_receipts".equals(op)) {
                    operation.getAsJsonArray("receipt_ids")
                            .forEach(id -> filedReceiptIds.add(id.getAsString()));
                } else if ("slots_contain_receipts_in_exact_order".equals(op)) {
                    operation.getAsJsonArray("ids")
                            .forEach(id -> filedReceiptIds.add(id.getAsString()));
                }
            }
            for (JsonElement element : predicate.getAsJsonArray("components")) {
                JsonObject component = element.getAsJsonObject();
                String componentId = component.get("id").getAsString();
                if (component.has("issues_receipt")) {
                    nodeReceipts.put(componentId, component.get("issues_receipt").getAsString());
                }
                if (component.has("page_receipts")) {
                    java.util.List<String> orderedPages = new java.util.ArrayList<>();
                    for (JsonElement receipt : component.getAsJsonArray("page_receipts")) {
                        String id = receipt.getAsJsonObject().getAsJsonObject("pdc")
                                .get("v5_receipt_id").getAsString();
                        nodeReceipts.put(id, id);
                        orderedPages.add(id);
                    }
                    nodePages.put(componentId, java.util.List.copyOf(orderedPages));
                }
            }
            for (JsonElement operationElement : predicate.getAsJsonArray("all_of")) {
                JsonObject operation = operationElement.getAsJsonObject();
                if ("player_opened_exact_sources".equals(operation.get("op").getAsString())
                        && operation.has("ids")) {
                    for (JsonElement source : operation.getAsJsonArray("ids")) {
                        String id = source.getAsString();
                        if (filedReceiptIds.contains(id)) {
                            nodeReceipts.putIfAbsent(id, id);
                        }
                    }
                }
            }
            result.put(nodeId, Map.copyOf(nodeReceipts));
            pageResult.put(nodeId, Map.copyOf(nodePages));
        }
        receipts = Map.copyOf(result);
        pageReceipts = Map.copyOf(pageResult);
    }

    public Optional<String> receiptFor(String nodeId, String exactSourceId) {
        return Optional.ofNullable(receipts.getOrDefault(nodeId, Map.of()).get(exactSourceId));
    }

    public Optional<String> pageReceipt(String nodeId, String componentId, int zeroBasedPage) {
        java.util.List<String> pages = pageReceipts.getOrDefault(nodeId, Map.of())
                .getOrDefault(componentId, java.util.List.of());
        return zeroBasedPage < 0 || zeroBasedPage >= pages.size()
                ? Optional.empty() : Optional.of(pages.get(zeroBasedPage));
    }

    /** Exact generated receipt identities, used to reconcile issuance journals before delivery. */
    public Set<ReceiptKey> allReceipts() {
        Set<ReceiptKey> result = new HashSet<>();
        receipts.forEach((nodeId, values) -> values.values().forEach(
                receiptId -> result.add(new ReceiptKey(nodeId, receiptId))));
        return Set.copyOf(result);
    }

    public record ReceiptKey(String nodeId, String receiptId) {
        public ReceiptKey {
            if (nodeId == null || nodeId.isBlank() || receiptId == null || receiptId.isBlank()) {
                throw new IllegalArgumentException("receipt key cannot be blank");
            }
        }
    }
}
