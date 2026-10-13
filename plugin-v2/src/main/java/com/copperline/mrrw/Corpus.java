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
 * which makes it feel like it is listening. Lines ingested recently (Kit's line, fed in with
 * Theo's inbox at Act 8) are preferred too: when the caller passes a "fresh since" time, one
 * piece of the reply comes from a fresh line about half the time, so a single new line is heard
 * among thousands of old ones. Every fragment keeps its source so the
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

    public void add(String text, String source, boolean fromPlayers) {
        add(text, source, fromPlayers, 0L);
    }

    /** A line Morrow took in at a known time (epoch ms); see {@link #reply(String, int, long, long)}. */
    public void add(String text, String source, boolean fromPlayers, long ingestedAtMillis) {
        String clean = normalise(text);
        if (clean.isBlank()) return;
        lines.add(new Line(clean, source, fromPlayers));
        ingestedAt.add(ingestedAtMillis);
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
        return fragments(act, Long.MIN_VALUE);
    }

    /** Fragments of lines ingested at or after freshSince (base-corpus lines, time 0, never count). */
    List<Fragment> fragments(int act, long freshSince) {
        List<Fragment> out = new ArrayList<>();
        for (int k = 0; k < lines.size(); k++) {
            Line line = lines.get(k);
            if (freshSince != Long.MIN_VALUE && (ingestedAt.get(k) <= 0 || ingestedAt.get(k) < freshSince)) continue;
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
        return reply(heard, act, seed, Long.MIN_VALUE);
    }

    /**
     * As {@link #reply(String, int, long)}, but lines ingested at or after freshSince (epoch ms) are
     * preferred: with probability FRESH_SHARE one piece of the reply is a fragment of a fresh line.
     * Long.MIN_VALUE = no preference (identical to the 3-argument form for the same seed).
     */
    public Reply reply(String heard, int act, long seed, long freshSince) {
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
        if (freshSince != Long.MIN_VALUE) {
            List<Fragment> fresh = fragments(act, freshSince);
            if (!fresh.isEmpty() && chosen.stream().noneMatch(fresh::contains) && rng.nextDouble() < FRESH_SHARE) {
                Fragment f = fresh.get(rng.nextInt(fresh.size()));
                chosen.set(1 + rng.nextInt(chosen.size() - 1), f);  // never the listening (first) piece
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Fragment f : chosen) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(f.words());
        }
        return new Reply(sb.toString(), chosen);
    }

    /**
     * A reply biased toward one voice (the bridge's /corpus, for the Act 7 Turing-test DMs): when the
     * corpus has fragments whose source is {@code persona} (the handle before the first comma, any
     * case), the listening piece is one of them and each later piece is too, two times in three.
     * Otherwise it is exactly {@link #reply(String, int, long, long)}. Still only words someone said.
     */
    public Reply reply(String heard, int act, long seed, long freshSince, String persona) {
        if (persona == null || persona.isBlank()) return reply(heard, act, seed, freshSince);
        List<Fragment> all = fragments(act);
        List<Fragment> own = new ArrayList<>();
        for (Fragment f : all) if (sourceIs(f.source(), persona)) own.add(f);
        if (own.isEmpty()) return reply(heard, act, seed, freshSince);
        Random rng = new Random(seed);
        String[] heardWords = normalise(heard).split(" ");
        List<Fragment> scored = new ArrayList<>(own);
        java.util.Collections.shuffle(scored, rng);
        scored.sort(Comparator.comparingInt((Fragment f) -> -overlap(f.words(), heardWords)));
        int pieces = 2 + rng.nextInt(2);
        List<Fragment> chosen = new ArrayList<>();
        chosen.add(scored.get(rng.nextInt(Math.min(6, scored.size()))));
        for (int tries = 0; chosen.size() < pieces && tries < 200; tries++) {
            List<Fragment> pool = rng.nextInt(3) < 2 ? own : all;
            Fragment f = pool.get(rng.nextInt(pool.size()));
            if (!chosen.contains(f)) chosen.add(f);
        }
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
