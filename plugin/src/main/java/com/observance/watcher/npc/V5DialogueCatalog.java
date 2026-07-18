package com.observance.watcher.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.structure.V5AuthorityManifest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Exact synchronous Minecraft dialogue loaded from the packaged V5 authority. */
public final class V5DialogueCatalog {

    public record Npc(String id, String displayName, String anchorSite,
                      Map<String, List<String>> states) {
        public Npc {
            id = normalize(id);
            displayName = text(displayName);
            anchorSite = normalize(anchorSite);
            Map<String, List<String>> copy = new LinkedHashMap<>();
            if (states != null) states.forEach((key, lines) ->
                    copy.put(normalize(key), List.copyOf(lines)));
            states = Map.copyOf(copy);
        }

        public List<String> lines(String state) {
            return states.getOrDefault(normalize(state), List.of());
        }
    }

    private record Catalog(Map<String, Npc> townsfolk, Npc wren, int lineCount) { }

    private static volatile Catalog cached;

    private V5DialogueCatalog() { }

    public static Map<String, Npc> townsfolk() {
        return catalog().townsfolk();
    }

    public static Npc townsperson(String id) {
        return townsfolk().get(normalize(id));
    }

    public static Npc wren() {
        return catalog().wren();
    }

    public static int lineCount() {
        return catalog().lineCount();
    }

    private static Catalog catalog() {
        Catalog local = cached;
        if (local != null) return local;
        synchronized (V5DialogueCatalog.class) {
            local = cached;
            if (local == null) {
                local = load();
                cached = local;
            }
        }
        return local;
    }

    private static Catalog load() {
        try (InputStream stream = V5DialogueCatalog.class.getClassLoader()
                .getResourceAsStream(V5AuthorityManifest.NPC_RESOURCE)) {
            if (stream == null) throw new IllegalStateException("packaged V5 NPC authority is missing");
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"5.0.0".equals(root.get("storyVersion").getAsString())) {
                throw new IllegalStateException("V5 NPC authority storyVersion is not 5.0.0");
            }
            Map<String, Npc> townsfolk = new LinkedHashMap<>();
            int lines = 0;
            for (JsonElement element : root.getAsJsonArray("townsfolk")) {
                Npc npc = parseNpc(element.getAsJsonObject());
                if (townsfolk.put(npc.id(), npc) != null) {
                    throw new IllegalStateException("duplicate V5 townsperson " + npc.id());
                }
                lines += countLines(npc);
            }
            Npc wren = parseNpc(root.getAsJsonObject("wren"));
            lines += countLines(wren);
            if (townsfolk.size() != 5 || lines != 91) {
                throw new IllegalStateException("V5 NPC authority expected 5 townsfolk/91 lines, found "
                        + townsfolk.size() + "/" + lines);
            }
            return new Catalog(Map.copyOf(townsfolk), wren, lines);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("cannot parse packaged V5 NPC dialogue", failure);
        } catch (Exception failure) {
            throw new IllegalStateException("cannot read packaged V5 NPC dialogue", failure);
        }
    }

    private static Npc parseNpc(JsonObject object) {
        Map<String, List<String>> states = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("lines").entrySet()) {
            List<String> lines = new ArrayList<>();
            entry.getValue().getAsJsonArray().forEach(line -> lines.add(line.getAsString()));
            if (lines.isEmpty() || lines.stream().anyMatch(String::isBlank)) {
                throw new IllegalStateException("empty dialogue state " + entry.getKey());
            }
            states.put(entry.getKey(), List.copyOf(lines));
        }
        return new Npc(object.get("id").getAsString(), object.get("displayName").getAsString(),
                object.get("anchorSite").getAsString(), states);
    }

    private static int countLines(Npc npc) {
        return npc.states().values().stream().mapToInt(List::size).sum();
    }

    private static String normalize(String value) {
        return text(value).toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
