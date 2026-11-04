package com.copperline.mrrw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Morrow's voice: Theo's 2011 word-chain helper. It can only say words somebody already said.
 *
 * A reply is one whole sentence a resident said, the one sharing the most words with what the
 * player said, so it sounds like the residents are still answering (an earlier collage of 3-6
 * word fragments read as word salad in playtest). Lines ingested recently (Kit's line, fed in
 * with Theo's inbox at Act 8) are preferred too: when the caller passes a "fresh since" time, a
 * fresh sentence follows about half the time, so a single new line is heard among thousands of
 * old ones. Every fragment keeps its source so the
 * operator journal can prove the corpus law (and players can, with the forum and logs).
 * Pure logic, no Bukkit types: tested by {@code SelfTest}.
 */
public final class Corpus {
    public record Line(String text, String source, boolean fromPlayers) { }
    public record Fragment(String words, String source) { }
    public record Reply(String text, List<Fragment> fragments) { }

    /** Chance that a reply carries one fragment of a fresh line, when there is one. */
    static final double FRESH_SHARE = 0.5;

    private final List<Line> lines = new ArrayList<>();
    private final List<Long> ingestedAt = new ArrayList<>();  // parallel to lines; 0 = the base corpus
    private final List<String> rawText = new ArrayList<>();  // parallel to lines; punctuation kept for sentences

    public void add(String text, String source, boolean fromPlayers) {
        add(text, source, fromPlayers, 0L);
    }

    /** A line Morrow took in at a known time (epoch ms); see {@link #reply(String, int, long, long)}. */
    public void add(String text, String source, boolean fromPlayers, long ingestedAtMillis) {
        String clean = normalise(text);
        if (clean.isBlank()) return;
        lines.add(new Line(clean, source, fromPlayers));
        ingestedAt.add(ingestedAtMillis);
        rawText.add(text);
    }

    public int size() {
        return lines.size();
    }

    public static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Whole sentences (3-25 words) available at this act, each a Fragment so the corpus law and the
     * journal's sources work unchanged. A sentence is split on . ! ? and line breaks before
     * normalising, so its words are always contiguous in its line.
     */
    List<Fragment> sentences(int act, long freshSince) {
        List<Fragment> out = new ArrayList<>();
        for (int k = 0; k < lines.size(); k++) {
            Line line = lines.get(k);
            if (freshSince != Long.MIN_VALUE && (ingestedAt.get(k) <= 0 || ingestedAt.get(k) < freshSince)) continue;
            if (line.fromPlayers() && act < 7) continue;
            for (String raw : rawText.get(k).split("[.!?\n]+")) {
                String sentence = normalise(raw);
                int n = sentence.isEmpty() ? 0 : sentence.split(" ").length;
                if (n >= 3 && n <= 25) out.add(new Fragment(sentence, line.source()));
            }
        }
        return out;
    }

    public Reply reply(String heard, int act, long seed) {
        return reply(heard, act, seed, Long.MIN_VALUE);
    }

    /**
     * One whole sentence somebody said, the one sharing the most words with what was heard (random
     * among the best few; a random sentence when nothing matches). Lines ingested at or after
     * freshSince (epoch ms) are preferred: with probability FRESH_SHARE a fresh sentence follows it.
     * Long.MIN_VALUE = no preference (identical to the 3-argument form for the same seed).
     */
    public Reply reply(String heard, int act, long seed, long freshSince) {
        List<Fragment> all = sentences(act, Long.MIN_VALUE);
        if (all.isEmpty()) return new Reply("", List.of());
        Random rng = new Random(seed);
        List<Fragment> chosen = new ArrayList<>();
        chosen.add(best(all, heard, rng));
        if (freshSince != Long.MIN_VALUE) {
            List<Fragment> fresh = sentences(act, freshSince);
            if (!fresh.isEmpty() && chosen.stream().noneMatch(fresh::contains) && rng.nextDouble() < FRESH_SHARE) {
                chosen.add(fresh.get(rng.nextInt(fresh.size())));
            }
        }
        return join(chosen);
    }

    /**
     * A reply in one voice (the bridge's /corpus, for the Act 7 Turing-test DMs): the best-matching
     * sentence whose source is {@code persona} (the handle before the first comma, any case).
     * Without any such sentence it is exactly {@link #reply(String, int, long, long)}.
     */
    public Reply reply(String heard, int act, long seed, long freshSince, String persona) {
        if (persona == null || persona.isBlank()) return reply(heard, act, seed, freshSince);
        List<Fragment> own = new ArrayList<>();
        for (Fragment f : sentences(act, Long.MIN_VALUE)) if (sourceIs(f.source(), persona)) own.add(f);
        if (own.isEmpty()) return reply(heard, act, seed, freshSince);
        return join(List.of(best(own, heard, new Random(seed))));
    }

    /** The sentence sharing the rarest words with what was heard (a common word like "what" counts for little). */
    private static Fragment best(List<Fragment> pool, String heard, Random rng) {
        java.util.Map<String, Integer> df = new java.util.HashMap<>();
        for (String w : normalise(heard).split(" ")) {
            if (w.length() <= 3 || STOP.contains(w) || df.containsKey(w)) continue;
            int n = 0;
            for (Fragment f : pool) if (has(f.words(), w)) n++;
            df.put(w, n);
        }
        List<Fragment> scored = new ArrayList<>(pool);
        java.util.Collections.shuffle(scored, rng);  // random tie-break, then a stable sort
        java.util.Map<Fragment, Double> score = new java.util.HashMap<>();
        for (Fragment f : scored) {
            double sc = 0;
            for (var e : df.entrySet()) if (e.getValue() > 0 && has(f.words(), e.getKey())) sc += 1.0 / e.getValue();
            score.put(f, sc);
        }
        scored.sort(Comparator.comparingDouble((Fragment f) -> -score.get(f)));
        double top = score.get(scored.get(0));
        if (top == 0) return scored.get(0);
        int ties = 0;
        while (ties < scored.size() && ties < 3 && score.get(scored.get(ties)) == top) ties++;
        return scored.get(rng.nextInt(ties));
    }

    /** Question and filler words: matching on these alone made it answer "what" instead of "tower". */
    private static final java.util.Set<String> STOP = java.util.Set.of(
            "what", "that", "this", "with", "have", "there", "they", "them", "then", "your", "from", "were",
            "been", "just", "like", "about", "when", "where", "will", "would", "could", "should", "does",
            "dont", "into", "some", "here", "know", "want", "okay", "hello", "anyone", "anybody", "youre");

    private static boolean has(String words, String w) {
        return (" " + words + " ").contains(" " + w + " ");
    }

    private static Reply join(List<Fragment> chosen) {
        StringBuilder sb = new StringBuilder();
        for (Fragment f : chosen) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(f.words());
        }
        return new Reply(sb.toString(), chosen);
    }

    static boolean sourceIs(String source, String persona) {
        int comma = source.indexOf(',');
        String who = (comma < 0 ? source : source.substring(0, comma)).trim();
        return who.equalsIgnoreCase(persona.trim());
    }

    /** The corpus law: every fragment of a reply occurs verbatim in some corpus line. */
    public boolean obeysLaw(Reply reply, int act) {
        for (Fragment f : reply.fragments()) {
            boolean found = false;
            for (Line line : lines) {
                if (line.fromPlayers() && act < 7) continue;
                if ((" " + line.text() + " ").contains(" " + f.words() + " ")) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
