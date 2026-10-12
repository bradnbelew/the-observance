package com.copperline.mrrw;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

/** Append-only operator receipts (plugins/mrrw/journal.jsonl): every world change Morrow makes. */
public final class Journal {
    private final File file;

    public Journal(File dataFolder) {
        this.file = new File(dataFolder, "journal.jsonl");
        file.getParentFile().mkdirs();
    }

    public synchronized void write(String kind, JsonObject detail) {
        JsonObject o = new JsonObject();
        o.addProperty("at", Instant.now().toString());
        o.addProperty("kind", kind);
        o.add("detail", detail);
        try (FileWriter w = new FileWriter(file, true)) {
            w.write(o.toString());
            w.write('\n');
        } catch (IOException e) {
            throw new IllegalStateException("journal write failed", e);
        }
    }
}
