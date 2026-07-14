package com.observance.watcher.v5runtime.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable, hash-checked authority for every V5 filled-map clue image. */
public final class V5MapArtAuthority {
    public static final String MANIFEST_RESOURCE = "v5/authority/map-art-manifest.json";
    public static final int EXPECTED_MAPS = 9;

    public record Entry(String id, String nodeId, String componentId, String resource,
                        int requiredFrameRotation, String sha256, BufferedImage image) {
        public Entry {
            id = clean(id);
            nodeId = clean(nodeId);
            componentId = clean(componentId);
            resource = clean(resource);
            sha256 = clean(sha256).toLowerCase();
        }
    }

    public record Catalog(Map<String, Entry> byId, Map<String, Entry> byComponent,
                          List<String> issues) {
        public Catalog {
            byId = Map.copyOf(byId);
            byComponent = Map.copyOf(byComponent);
            issues = List.copyOf(issues);
        }

        public boolean valid() {
            return issues.isEmpty();
        }

        public Entry byId(String id) {
            return byId.get(clean(id));
        }

        public Entry byComponent(String nodeId, String componentId) {
            return byComponent.get(clean(nodeId) + ":" + clean(componentId));
        }
    }

    private V5MapArtAuthority() { }

    public static Catalog loadDefault() {
        List<String> issues = new ArrayList<>();
        Map<String, Entry> byId = new LinkedHashMap<>();
        Map<String, Entry> byComponent = new LinkedHashMap<>();
        try (InputStream stream = V5MapArtAuthority.class.getClassLoader()
                .getResourceAsStream(MANIFEST_RESOURCE)) {
            if (stream == null) {
                return new Catalog(Map.of(), Map.of(), List.of("missing packaged " + MANIFEST_RESOURCE));
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
            if (schema != 1) issues.add("unsupported map-art schemaVersion " + schema);
            JsonArray maps = root.has("maps") && root.get("maps").isJsonArray()
                    ? root.getAsJsonArray("maps") : new JsonArray();
            for (JsonElement element : maps) {
                if (!element.isJsonObject()) {
                    issues.add("non-object map-art entry");
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                String id = string(row, "id");
                String nodeId = string(row, "nodeId");
                String componentId = string(row, "componentId");
                String sourceFile = string(row, "file").replace('\\', '/');
                String filename = sourceFile.substring(sourceFile.lastIndexOf('/') + 1);
                String resource = "v5/authority/map-art/" + filename;
                String expectedHash = string(row, "sha256").toLowerCase();
                int rotation = row.has("requiredFrameRotation")
                        ? row.get("requiredFrameRotation").getAsInt() : -1;
                byte[] png = readResource(resource, issues);
                String actualHash = png == null ? "" : sha256(png);
                BufferedImage image = png == null ? null : ImageIO.read(new ByteArrayInputStream(png));
                Entry entry = new Entry(id, nodeId, componentId, resource, rotation,
                        expectedHash, image);
                if (id.isBlank()) issues.add("blank map-art id");
                else if (byId.putIfAbsent(id, entry) != null) issues.add("duplicate map-art id " + id);
                String componentKey = nodeId + ":" + componentId;
                if (nodeId.isBlank() || componentId.isBlank()) issues.add(id + " has blank binding");
                else if (byComponent.putIfAbsent(componentKey, entry) != null) {
                    issues.add("duplicate map-art component binding " + componentKey);
                }
                if (!expectedHash.matches("[0-9a-f]{64}")) issues.add(id + " has invalid SHA-256");
                else if (!expectedHash.equals(actualHash)) issues.add(id + " PNG SHA-256 mismatch");
                if (image == null || image.getWidth() != 128 || image.getHeight() != 128) {
                    issues.add(id + " PNG is not readable 128x128 art");
                }
                if (rotation < 0 || rotation > 7) issues.add(id + " frame rotation is outside 0..7");
            }
        } catch (IOException | RuntimeException failure) {
            issues.add("cannot parse map-art authority: " + failure.getClass().getSimpleName()
                    + ": " + failure.getMessage());
        }
        if (byId.size() != EXPECTED_MAPS) issues.add("expected " + EXPECTED_MAPS
                + " map-art rows, found " + byId.size());
        return new Catalog(byId, byComponent, issues);
    }

    private static byte[] readResource(String resource, List<String> issues) throws IOException {
        try (InputStream stream = V5MapArtAuthority.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                issues.add("missing packaged " + resource);
                return null;
            }
            return stream.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
