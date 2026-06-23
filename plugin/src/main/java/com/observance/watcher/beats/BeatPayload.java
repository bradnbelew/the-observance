package com.observance.watcher.beats;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Null-safe, lore-AGNOSTIC reader over a beat's opaque JSON {@code payload} string.
 *
 * <p>Every concrete {@link Beat} pulls its parameters (text lines, sound keys, material names,
 * counts, coordinates) out of here. The plugin NEVER hardcodes story text — all narrative content
 * is authored by the showrunner/dashboard into Supabase {@code beat_queue.payload} and read here.
 *
 * <p>Total fault isolation: a malformed/empty payload parses to an empty object; every accessor
 * returns a caller-supplied default rather than throwing. Construction never throws.
 */
public final class BeatPayload {

    private static final Gson GSON = new Gson();

    private final JsonObject root;

    private BeatPayload(JsonObject root) {
        this.root = root == null ? new JsonObject() : root;
    }

    /** Parse a raw payload string. Null / blank / malformed → an empty payload (never throws). */
    public static BeatPayload parse(String raw) {
        if (raw == null || raw.isBlank()) return new BeatPayload(new JsonObject());
        try {
            JsonElement el = JsonParser.parseString(raw);
            if (el != null && el.isJsonObject()) {
                return new BeatPayload(el.getAsJsonObject());
            }
        } catch (Throwable ignored) {
            // malformed JSON → empty payload, beat falls back to its defaults
        }
        return new BeatPayload(new JsonObject());
    }

    /** Empty payload (handy for synthesized/ambient beats with no authored content). */
    public static BeatPayload empty() {
        return new BeatPayload(new JsonObject());
    }

    /** Wrap an already-built JsonObject (used by ambient synthesis). */
    public static BeatPayload of(JsonObject obj) {
        return new BeatPayload(obj);
    }

    /* ------------------------------------------------------------------ */
    /* Scalars                                                             */
    /* ------------------------------------------------------------------ */

    public String string(String key, String def) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonPrimitive()) {
                String s = e.getAsString();
                return s == null ? def : s;
            }
        } catch (Throwable ignored) { }
        return def;
    }

    public int integer(String key, int def) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonPrimitive()) return e.getAsInt();
        } catch (Throwable ignored) { }
        return def;
    }

    public long longValue(String key, long def) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonPrimitive()) return e.getAsLong();
        } catch (Throwable ignored) { }
        return def;
    }

    public double number(String key, double def) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonPrimitive()) {
                double d = e.getAsDouble();
                return Double.isNaN(d) ? def : d;
            }
        } catch (Throwable ignored) { }
        return def;
    }

    public float floatValue(String key, float def) {
        double d = number(key, Double.NaN);
        return Double.isNaN(d) ? def : (float) d;
    }

    public boolean bool(String key, boolean def) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonPrimitive()) return e.getAsBoolean();
        } catch (Throwable ignored) { }
        return def;
    }

    /** True iff the key is present (any type). */
    public boolean has(String key) {
        try {
            return root.has(key) && !root.get(key).isJsonNull();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Arrays                                                              */
    /* ------------------------------------------------------------------ */

    /** Read a string array (e.g. book/sign lines). Returns an empty list if absent/malformed. */
    public List<String> stringList(String key) {
        List<String> out = new ArrayList<>();
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonArray()) {
                JsonArray arr = e.getAsJsonArray();
                for (JsonElement item : arr) {
                    if (item != null && item.isJsonPrimitive()) {
                        out.add(item.getAsString());
                    } else if (item != null && !item.isJsonNull()) {
                        out.add(item.toString());
                    }
                }
            }
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
        return out;
    }

    /** Read an array of integers (e.g. a clue number sequence). Empty list if absent/malformed. */
    public List<Integer> intList(String key) {
        List<Integer> out = new ArrayList<>();
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonArray()) {
                for (JsonElement item : e.getAsJsonArray()) {
                    if (item != null && item.isJsonPrimitive()) {
                        try { out.add(item.getAsInt()); } catch (Throwable ignored) { }
                    }
                }
            }
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
        return out;
    }

    /** Read an array of sub-objects (e.g. multi-page books: list of {title, lines}). */
    public List<BeatPayload> objectList(String key) {
        List<BeatPayload> out = new ArrayList<>();
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonArray()) {
                for (JsonElement item : e.getAsJsonArray()) {
                    if (item != null && item.isJsonObject()) {
                        out.add(new BeatPayload(item.getAsJsonObject()));
                    }
                }
            }
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
        return out;
    }

    /** Nested object accessor; absent → empty payload. */
    public BeatPayload object(String key) {
        try {
            JsonElement e = root.get(key);
            if (e != null && e.isJsonObject()) return new BeatPayload(e.getAsJsonObject());
        } catch (Throwable ignored) { }
        return new BeatPayload(new JsonObject());
    }

    /** Raw root, for advanced/diagnostic use. */
    public JsonObject raw() {
        return root;
    }

    @Override
    public String toString() {
        try {
            return GSON.toJson(root);
        } catch (Throwable ignored) {
            return "{}";
        }
    }
}
