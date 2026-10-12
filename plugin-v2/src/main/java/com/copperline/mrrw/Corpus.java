package com.copperline.mrrw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Morrow's voice: Theo's 2011 word-chain helper. It can only say words somebody already said.
 *
 * A reply is two or three contiguous fragments (3-6 words each) lifted from corpus lines,
 * joined with ". ". Fragments that share a word with what the player said are preferred,
 * which makes it feel like it is listening. Every fragment keeps its source so the
 * operator journal can prove the corpus law (and players can, with the forum and logs).
 * Pure logic, no Bukkit types: tested by {@code SelfTest}.
 */
public final class Corpus {
    public record Line(String text, String source, boolean fromPlayers) { }
    public record Fragment(String words, String source) { }
    public record Reply(String text, List<Fragment> fragments) { }

    private final List<Line> lines = new ArrayList<>();

    public void add(String text, String source, boolean fromPlayers) {
        String clean = normalise(text);
        if (!clean.isBlank()) lines.add(new Line(clean, source, fromPlayers));
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

    /** All fragments of 3-6 consecutive words available at this act. */
    List<Fragment> fragments(int act) {
        List<Fragment> out = new ArrayList<>();
        for (Line line : lines) {
            if (line.fromPlayers() && act < 7) continue;  // players' own words only from Act 7
            String[] w = line.text().split(" ");
            for (int len = 3; len <= 6; len++) {
                for (int i = 0; i + len <= w.length; i++) {
                    out.add(new Fragment(String.join(" ", java.util.Arrays.copyOfRange(w, i, i + len)), line.source()));
                }
            }
        }
        return out;
    }

    public Reply reply(String heard, int act, long seed) {
        List<Fragment> all = fragments(act);
        if (all.isEmpty()) return new Reply("", List.of());
        Random rng = new Random(seed);
        String[] heardWords = normalise(heard).split(" ");
        List<Fragment> scored = new ArrayList<>(all);
        java.util.Collections.shuffle(scored, rng);  // random tie-break, then a stable sort
        scored.sort(Comparator.comparingInt((Fragment f) -> -overlap(f.words(), heardWords)));
        int pieces = 2 + rng.nextInt(2);
        List<Fragment> chosen = new ArrayList<>();
        chosen.add(scored.get(rng.nextInt(Math.min(6, scored.size()))));
        while (chosen.size() < pieces) {
            Fragment f = all.get(rng.nextInt(all.size()));
            if (!chosen.contains(f)) chosen.add(f);
        }
        StringBuilder sb = new StringBuilder();
        for (Fragment f : chosen) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(f.words());
        }
        return new Reply(sb.toString(), chosen);
    }

    private static int overlap(String fragment, String[] heard) {
        int n = 0;
        for (String w : heard) {
            if (w.length() > 3 && (" " + fragment + " ").contains(" " + w + " ")) n++;
        }
        return n;
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
