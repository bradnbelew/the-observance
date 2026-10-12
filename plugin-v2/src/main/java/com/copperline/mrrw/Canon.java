package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generated canon (morrow/canon/build.py -> resources/canon/*.json). Never hand-edited. */
public final class Canon {
    public final Map<String, int[]> anchors = new LinkedHashMap<>();
    public final Map<Integer, String> motdByAct = new LinkedHashMap<>();
    public final List<List<String>> rulesSigns2011 = new ArrayList<>();
    public final List<List<String>> rulesSignsMorrow = new ArrayList<>();
    public final List<String> tallyBookMorrow = new ArrayList<>();
    public final Map<String, List<String>> signBackNotes = new LinkedHashMap<>();
    public final List<String> statusLines = new ArrayList<>();
    public final List<String> listedPlayersEarly = new ArrayList<>();
    public final Corpus corpus = new Corpus();
    public boolean provisionalLayout;
    /** The whole generated canon, for the Acts 4-10 services that read structured data directly. */
    public JsonObject raw;

    public static Canon load(ClassLoader loader) {
        Canon c = new Canon();
        JsonObject root = read(loader, "canon/canon.json").getAsJsonObject();
        c.raw = root;
        root.getAsJsonObject("anchors").entrySet().forEach(e -> {
            JsonArray a = e.getValue().getAsJsonArray();
            c.anchors.put(e.getKey(), new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
        });
        root.getAsJsonObject("motd_by_act").entrySet()
                .forEach(e -> c.motdByAct.put(Integer.parseInt(e.getKey()), e.getValue().getAsString()));
        signs(root.getAsJsonArray("rules_signs_2011"), c.rulesSigns2011);
        signs(root.getAsJsonArray("rules_signs_morrow"), c.rulesSignsMorrow);
        root.getAsJsonArray("tally_book_morrow").forEach(e -> c.tallyBookMorrow.add(e.getAsString()));
        root.getAsJsonObject("sign_back_notes").entrySet().forEach(e -> {
            List<String> lines = new ArrayList<>();
            e.getValue().getAsJsonArray().forEach(l -> lines.add(l.getAsString()));
            c.signBackNotes.put(e.getKey(), lines);
        });
        root.getAsJsonArray("status_lines").forEach(e -> c.statusLines.add(e.getAsString()));
        root.getAsJsonArray("listed_players_early").forEach(e -> c.listedPlayersEarly.add(e.getAsString()));
        c.provisionalLayout = root.get("provisional_layout").getAsBoolean();
        for (JsonElement e : read(loader, "canon/corpus.json").getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            c.corpus.add(o.get("text").getAsString(), o.get("source").getAsString(), false);
        }
        return c;
    }

    private static void signs(JsonArray in, List<List<String>> out) {
        for (JsonElement sign : in) {
            List<String> lines = new ArrayList<>();
            sign.getAsJsonArray().forEach(l -> lines.add(l.getAsString()));
            out.add(lines);
        }
    }

    private static JsonElement read(ClassLoader loader, String path) {
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing generated resource " + path + "; run python -m morrow.canon.build");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public JsonArray array(String key) {
        return raw.getAsJsonArray(key);
    }

    public JsonObject object(String key) {
        return raw.getAsJsonObject(key);
    }

    public String string(String key) {
        return raw.get(key).getAsString();
    }

    public static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList<>();
        a.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    public int[] anchor(String name) {
        int[] a = anchors.get(name);
        if (a == null) throw new IllegalArgumentException("unknown anchor " + name);
        return a;
    }
}
