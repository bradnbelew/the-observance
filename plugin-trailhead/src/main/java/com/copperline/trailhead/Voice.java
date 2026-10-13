package com.copperline.trailhead;

import com.copperline.mrrw.Corpus;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Morrow's one line at game night. Same law as in Mossfield: every fragment is lifted verbatim
 * from the 2011 corpus. It prefers fragments that share words with what the table just said,
 * so it sounds like it was listening. The players' own words are never used here (that is Act 7).
 * Pure logic, no Bukkit types: tested by {@code SelfTest}.
 */
public final class Voice {
    private static final int ACT = 3;  // Mossfield's first speaking act: 2011 corpus only

    private final Corpus corpus = new Corpus();
    private final List<String> avoid;

    public Voice(InputStream corpusJson, List<String> avoidWords) {
        for (JsonElement e : JsonParser.parseReader(new InputStreamReader(corpusJson, StandardCharsets.UTF_8)).getAsJsonArray()) {
            corpus.add(e.getAsJsonObject().get("text").getAsString(), e.getAsJsonObject().get("source").getAsString(), false);
        }
        avoid = avoidWords.stream().map(w -> w.toLowerCase(Locale.ROOT)).toList();
    }

    public int size() {
        return corpus.size();
    }

    /** A line that obeys the corpus law and never says an avoided word, or null if none was found. */
    public Corpus.Reply line(String heard, long seed) {
        Random rng = new Random(seed);
        for (int tries = 0; tries < 200; tries++) {
            Corpus.Reply r = corpus.reply(heard, ACT, rng.nextLong());
            if (r.text().isBlank() || !corpus.obeysLaw(r, ACT) || mentionsAvoided(r.text())) continue;
            return r;
        }
        return null;
    }

    boolean mentionsAvoided(String text) {
        String clean = Corpus.normalise(text);  // substring match: "brackens" counts too
        if (clean.matches(".*\\d.*")) return true;  // numbers in the corpus are cipher data
        for (String w : avoid) if (clean.contains(w)) return true;
        return false;
    }
}
