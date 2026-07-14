package com.observance.watcher.v5runtime.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Exact player-visible names, lore, and container labels for non-book V5 evidence. */
public final class V5EvidenceItemAppearanceAuthority {
    public static final String RESOURCE = "v5/authority/evidence-item-appearance.json";
    public static final int EXPECTED_ITEMS = 111;
    public static final int EXPECTED_LABELS = 8;
    public static final int EXPECTED_STATION_LABELS = 4;

    public record Entry(String id, String nodeId, String title, List<String> lore) {
        public Entry {
            id = clean(id);
            nodeId = clean(nodeId);
            title = clean(title);
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    public record Catalog(int schemaVersion, Map<String, Entry> byId,
                          Map<String, List<String>> containerLabels,
                          Map<String, List<String>> stationLabels, List<String> issues) {
        public Catalog {
            byId = Map.copyOf(byId);
            containerLabels = Map.copyOf(containerLabels);
            stationLabels = Map.copyOf(stationLabels);
            issues = List.copyOf(issues);
        }

        public boolean valid() {
            return issues.isEmpty();
        }

        public Entry get(String id) {
            return byId.get(clean(id));
        }

        public List<String> label(String nodeId, String componentId) {
            return containerLabels.getOrDefault(clean(nodeId) + ':' + clean(componentId), List.of());
        }

        public List<String> stationLabel(String nodeId, String componentId) {
            return stationLabels.getOrDefault(clean(nodeId) + ':' + clean(componentId), List.of());
        }
    }

    private V5EvidenceItemAppearanceAuthority() { }

    public static Catalog loadDefault() {
        List<String> issues = new ArrayList<>();
        try (InputStream stream = V5EvidenceItemAppearanceAuthority.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) return new Catalog(0, Map.of(), Map.of(), Map.of(),
                    List.of("missing packaged " + RESOURCE));
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return new Catalog(0, Map.of(), Map.of(), Map.of(),
                    List.of("appearance authority root is not an object"));
            JsonObject root = parsed.getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
            if (schema != 1) issues.add("unsupported appearance schemaVersion " + schema);

            Map<String, Entry> items = new LinkedHashMap<>();
            JsonArray itemArray = root.has("items") && root.get("items").isJsonArray()
                    ? root.getAsJsonArray("items") : new JsonArray();
            Map<String, Integer> nodeCounts = new LinkedHashMap<>();
            for (JsonElement element : itemArray) {
                if (!element.isJsonObject()) {
                    issues.add("non-object appearance entry");
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                Entry entry = new Entry(string(object, "id"), string(object, "nodeId"),
                        string(object, "title"), strings(object, "lore"));
                if (entry.id().isBlank()) issues.add("blank appearance id");
                else if (items.putIfAbsent(entry.id(), entry) != null) {
                    issues.add("duplicate appearance id " + entry.id());
                }
                if (entry.nodeId().isBlank()) issues.add(entry.id() + " has blank nodeId");
                else nodeCounts.merge(entry.nodeId(), 1, Integer::sum);
                if (entry.title().isBlank()) issues.add(entry.id() + " has blank title");
                if (entry.lore().isEmpty() || entry.lore().stream().anyMatch(String::isBlank)) {
                    issues.add(entry.id() + " has blank/missing lore");
                }
                String visible = (entry.title() + ' ' + String.join(" ", entry.lore()))
                        .toLowerCase(Locale.ROOT);
                if (visible.contains("v5_sort_class") || visible.contains("kv02_return_")) {
                    issues.add(entry.id() + " exposes an internal identity/class key");
                }
            }
            if (items.size() != EXPECTED_ITEMS) issues.add("expected " + EXPECTED_ITEMS
                    + " item appearances, found " + items.size());
            Map<String, Integer> exactCounts = Map.ofEntries(
                    Map.entry("A02", 1), Map.entry("A03", 12), Map.entry("AR06", 1),
                    Map.entry("BI02", 4), Map.entry("BI03", 3), Map.entry("BI06", 3),
                    Map.entry("CW02", 8), Map.entry("CW07", 2), Map.entry("KI02", 1),
                    Map.entry("KS01", 6), Map.entry("KV01", 22), Map.entry("KV02", 36),
                    Map.entry("LC01", 3), Map.entry("LC03", 2), Map.entry("WR02", 4),
                    Map.entry("WR04", 3));
            if (!nodeCounts.equals(exactCounts)) issues.add("appearance node coverage is " + nodeCounts);

            Map<String, List<String>> labels = new LinkedHashMap<>();
            JsonObject groups = root.has("containerLabels") && root.get("containerLabels").isJsonObject()
                    ? root.getAsJsonObject("containerLabels") : new JsonObject();
            for (Map.Entry<String, JsonElement> nodeEntry : groups.entrySet()) {
                if (!nodeEntry.getValue().isJsonObject()) {
                    issues.add("containerLabels." + nodeEntry.getKey() + " is not an object");
                    continue;
                }
                for (Map.Entry<String, JsonElement> componentEntry
                        : nodeEntry.getValue().getAsJsonObject().entrySet()) {
                    if (!componentEntry.getValue().isJsonArray()) {
                        issues.add("container label " + nodeEntry.getKey() + ':'
                                + componentEntry.getKey() + " is not an array");
                        continue;
                    }
                    List<String> lines = strings(componentEntry.getValue().getAsJsonArray());
                    String key = clean(nodeEntry.getKey()) + ':' + clean(componentEntry.getKey());
                    if (lines.size() != 2 || lines.stream().anyMatch(String::isBlank)) {
                        issues.add("container label " + key + " must have exactly two nonblank lines");
                    } else if (labels.putIfAbsent(key, lines) != null) {
                        issues.add("duplicate container label " + key);
                    }
                }
            }
            if (labels.size() != EXPECTED_LABELS) issues.add("expected " + EXPECTED_LABELS
                    + " container labels, found " + labels.size());
            if (!labels.keySet().equals(Set.of(
                    "KV02:cistern", "KV02:public_heat", "KV02:private_heat", "KV02:condemned",
                    "CW02:A_top", "CW02:A_lower", "CW02:B_top", "CW02:B_lower"))) {
                issues.add("container label coverage is " + labels.keySet());
            }

            Map<String, List<String>> stationLabels = readLabels(root, "stationLabels", issues);
            if (stationLabels.size() != EXPECTED_STATION_LABELS) issues.add("expected "
                    + EXPECTED_STATION_LABELS + " station labels, found " + stationLabels.size());
            if (!stationLabels.keySet().equals(Set.of("A02:mkept_station", "A02:ash_station",
                    "A02:rook_station", "A02:wren_station"))) {
                issues.add("station label coverage is " + stationLabels.keySet());
            }
            return new Catalog(schema, items, labels, stationLabels, issues);
        } catch (IOException | RuntimeException failure) {
            issues.add("cannot parse " + RESOURCE + ": " + failure.getClass().getSimpleName()
                    + ": " + failure.getMessage());
            return new Catalog(0, Map.of(), Map.of(), Map.of(), issues);
        }
    }

    private static Map<String, List<String>> readLabels(JsonObject root, String rootKey,
                                                         List<String> issues) {
        Map<String, List<String>> labels = new LinkedHashMap<>();
        JsonObject groups = root.has(rootKey) && root.get(rootKey).isJsonObject()
                ? root.getAsJsonObject(rootKey) : new JsonObject();
        for (Map.Entry<String, JsonElement> nodeEntry : groups.entrySet()) {
            if (!nodeEntry.getValue().isJsonObject()) {
                issues.add(rootKey + '.' + nodeEntry.getKey() + " is not an object");
                continue;
            }
            for (Map.Entry<String, JsonElement> componentEntry
                    : nodeEntry.getValue().getAsJsonObject().entrySet()) {
                String key = clean(nodeEntry.getKey()) + ':' + clean(componentEntry.getKey());
                if (!componentEntry.getValue().isJsonArray()) {
                    issues.add(rootKey + " label " + key + " is not an array");
                    continue;
                }
                List<String> lines = strings(componentEntry.getValue().getAsJsonArray());
                if (lines.size() != 2 || lines.stream().anyMatch(String::isBlank)) {
                    issues.add(rootKey + " label " + key
                            + " must have exactly two nonblank lines");
                } else if (labels.putIfAbsent(key, lines) != null) {
                    issues.add("duplicate " + rootKey + " label " + key);
                }
            }
        }
        return Map.copyOf(labels);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? strings(value.getAsJsonArray()) : List.of();
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
