package com.observance.watcher.m2runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Exact byte and semantic identity for one immutable predicate authority. */
public record PredicateAuthorityVersion(
        String versionId,
        String rawSha256,
        String semanticSha256,
        String predecessorRawSha256,
        String rollbackRawSha256) {

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public PredicateAuthorityVersion {
        requireText(versionId, "versionId");
        requireHash(rawSha256, "rawSha256");
        requireHash(semanticSha256, "semanticSha256");
        requireOptionalHash(predecessorRawSha256, "predecessorRawSha256");
        requireOptionalHash(rollbackRawSha256, "rollbackRawSha256");
    }

    public static String rawSha256(byte[] bytes) {
        return sha256(Objects.requireNonNull(bytes, "bytes"));
    }

    /** Canonical JSON hash: UTF-8, sorted object keys, arrays retained in authored order. */
    public static String semanticSha256(byte[] bytes) {
        JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        return semanticSha256(parsed);
    }

    /** Canonical hash for an already-parsed approval payload. */
    public static String semanticSha256(JsonElement parsed) {
        StringBuilder canonical = new StringBuilder();
        appendCanonical(parsed, canonical);
        return sha256(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendCanonical(JsonElement element, StringBuilder out) {
        if (element.isJsonObject()) {
            out.append('{');
            JsonObject object = element.getAsJsonObject();
            List<String> keys = new ArrayList<>(object.keySet());
            keys.sort(Comparator.naturalOrder());
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) out.append(',');
                String key = keys.get(index);
                out.append(new com.google.gson.JsonPrimitive(key)).append(':');
                appendCanonical(object.get(key), out);
            }
            out.append('}');
            return;
        }
        if (element.isJsonArray()) {
            out.append('[');
            JsonArray array = element.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                if (index > 0) out.append(',');
                appendCanonical(array.get(index), out);
            }
            out.append(']');
            return;
        }
        out.append(element);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("JVM has no SHA-256 provider", exception);
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    private static void requireHash(String value, String name) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
    }

    private static void requireOptionalHash(String value, String name) {
        if (value != null) requireHash(value, name);
    }
}
