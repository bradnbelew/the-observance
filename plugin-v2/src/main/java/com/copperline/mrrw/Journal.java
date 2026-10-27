package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only operator receipts (plugins/mrrw/journal.jsonl): every world change Morrow makes.
 *
 * Also the Discord bot's feed (plugins/mrrw/feed.jsonl): Mossfield chat, Morrow's [Recovery]
 * broadcasts and Theo's released replies, which the bot reads over the bridge (GET /feed) instead
 * of from this machine's disk. An entry's number is its line number, so the bot's cursor survives a
 * restart of either side.
 */
public final class Journal {
    private final File file;
    private final File feedFile;
    private final List<JsonObject> feed = new ArrayList<>();

    public Journal(File dataFolder) {
        this.file = new File(dataFolder, "journal.jsonl");
        this.feedFile = new File(dataFolder, "feed.jsonl");
        file.getParentFile().mkdirs();
        if (feedFile.isFile()) {
            try {
                for (String line : Files.readAllLines(feedFile.toPath(), StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    try {
                        feed.add(JsonParser.parseString(line).getAsJsonObject());
                    } catch (Exception torn) {
                        feed.add(new JsonObject());  // a torn line still takes its number
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("feed read failed", e);
            }
        }
    }

    public synchronized void write(String kind, JsonObject detail) {
        JsonObject o = new JsonObject();
        o.addProperty("at", Instant.now().toString());
        o.addProperty("kind", kind);
        o.add("detail", detail);
        append(file, o);
    }

    /** One feed entry: "chat" {name, text}, "recovery" {text}, "theo.reply" {id, subject, at}. */
    public synchronized void feed(String kind, JsonObject detail) {
        JsonObject o = detail.deepCopy();
        o.addProperty("n", feed.size() + 1);
        o.addProperty("kind", kind);
        if (!o.has("at")) o.addProperty("at", Instant.now().toString());
        append(feedFile, o);
        feed.add(o);
    }

    public void recovery(String text) {
        JsonObject d = new JsonObject();
        d.addProperty("text", text);
        feed("recovery", d);
    }

    /**
     * Up to max entries numbered after {@code after}, and the number to ask after next. after < 0
     * (a bot's first run) returns nothing and the current end, so a new bot doesn't replay history.
     */
    public synchronized JsonObject feedAfter(long after, int max) {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        long next = after < 0 ? feed.size() : Math.min(after, feed.size());
        for (long i = next; after >= 0 && i < feed.size() && a.size() < max; i++) {
            a.add(feed.get((int) i));
            next = i + 1;
        }
        o.add("entries", a);
        o.addProperty("next", next);
        o.addProperty("end", feed.size());
        return o;
    }

    private static void append(File f, JsonObject o) {
        try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8, true)) {
            w.write(o.toString());
            w.write('\n');
        } catch (IOException e) {
            throw new IllegalStateException("write failed: " + f.getName(), e);
        }
    }
}
