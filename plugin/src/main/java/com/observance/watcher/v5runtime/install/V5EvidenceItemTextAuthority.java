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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact readable payload authority for portable V5 evidence books. */
public final class V5EvidenceItemTextAuthority {
    public static final String RESOURCE = "v5/authority/evidence-item-text.json";
    public static final int EXPECTED_ITEMS = 7;

    public record Entry(String id, String nodeId, String material, String title,
                        String author, List<String> pages) {
        public Entry {
            id = clean(id);
            nodeId = clean(nodeId);
            material = clean(material).toUpperCase();
            title = clean(title);
            author = clean(author);
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    public record Catalog(int schemaVersion, Map<String, Entry> byId, List<String> issues) {
        public Catalog {
            byId = Map.copyOf(byId);
            issues = List.copyOf(issues);
        }

        public boolean valid() {
            return issues.isEmpty();
        }

        public Entry get(String id) {
            return byId.get(clean(id));
        }
    }

    private V5EvidenceItemTextAuthority() { }

    public static Catalog loadDefault() {
        List<String> issues = new ArrayList<>();
        try (InputStream stream = V5EvidenceItemTextAuthority.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return new Catalog(0, Map.of(), List.of("missing packaged " + RESOURCE));
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
            if (schema != 1) issues.add("unsupported evidence text schemaVersion " + schema);
            JsonArray items = root.has("items") && root.get("items").isJsonArray()
                    ? root.getAsJsonArray("items") : new JsonArray();
            Map<String, Entry> entries = new LinkedHashMap<>();
            Set<String> nodeIds = new LinkedHashSet<>();
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    issues.add("non-object evidence text entry");
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String id = string(item, "id");
                String nodeId = string(item, "nodeId");
                String material = string(item, "material");
                String title = string(item, "title");
                String author = string(item, "author");
                List<String> pages = strings(item, "pages");
                Entry entry = new Entry(id, nodeId, material, title, author, pages);
                if (id.isBlank()) issues.add("blank evidence text id");
                else if (entries.putIfAbsent(id, entry) != null) issues.add("duplicate evidence text " + id);
                if (nodeId.isBlank()) issues.add(id + " has blank nodeId");
                else nodeIds.add(nodeId);
                if (!"WRITTEN_BOOK".equals(entry.material())) issues.add(id + " material is not WRITTEN_BOOK");
                if (title.isBlank() || title.length() > 32) issues.add(id + " title is blank/over 32 characters");
                if (author.isBlank()) issues.add(id + " author is blank");
                if (pages.isEmpty() || pages.stream().anyMatch(String::isBlank)) {
                    issues.add(id + " pages are blank/missing");
                }
            }
            if (entries.size() != EXPECTED_ITEMS) issues.add("expected " + EXPECTED_ITEMS
                    + " evidence texts, found " + entries.size());
            if (!nodeIds.equals(Set.of("WR01", "BI01", "CW07"))) {
                issues.add("evidence text node coverage is " + nodeIds);
            }
            return new Catalog(schema, entries, issues);
        } catch (IOException | RuntimeException failure) {
            issues.add("cannot parse " + RESOURCE + ": " + failure.getClass().getSimpleName()
                    + ": " + failure.getMessage());
            return new Catalog(0, Map.of(), issues);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive()) result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
