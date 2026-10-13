package com.copperline.trailhead;

import com.copperline.mrrw.Corpus;

import java.util.ArrayList;
import java.util.List;

/** Pure-logic checks for the trailhead (no server). Run: gradlew selftest */
public final class SelfTest {
    public static void main(String[] args) throws Exception {
        int fails = 0;
        List<String> key = List.of("copperline hosting", "case 6118", "mossfield", "records kept until",
                "31 dec 2026", "copperlinehosting", ".com");

        for (int n = 2; n <= 10; n++) {
            List<List<String>> sectors = new ArrayList<>();
            for (int i = 0; i < n; i++) sectors.add(KeyFragments.sector(key, n, i));
            if (!KeyFragments.overlay(sectors).equals(key)) { fails++; System.out.println("FAIL overlay n=" + n); }
            for (List<String> s : sectors) {
                String joined = String.join(" ", s);
                if (joined.contains("copper") || joined.contains("hosting") || joined.contains("6118")) {
                    fails++;
                    System.out.println("FAIL a lone sector reads too much, n=" + n + ": " + joined);
                }
                for (int l = 0; l < key.size(); l++) {
                    if (s.get(l).length() != key.get(l).length()) { fails++; System.out.println("FAIL line length n=" + n); }
                }
            }
            // without one player the key must not be complete
            if (KeyFragments.overlay(sectors.subList(1, n)).equals(key)) { fails++; System.out.println("FAIL key complete without sector 1, n=" + n); }
        }
        System.out.println("sector 2 of 5:");
        KeyFragments.sector(key, 5, 1).forEach(l -> System.out.println("   " + l));

        Voice voice;
        try (var in = SelfTest.class.getClassLoader().getResourceAsStream("canon/corpus.json")) {
            voice = new Voice(in, List.of("bracken", "biscuit", "haltmrrw", "6118"));
        }
        String table = "ok who voted for me. i was literally at the farm the whole time. the lights went out again";
        for (long seed = 0; seed < 300; seed++) {
            Corpus.Reply r = voice.line(table, seed);
            if (r == null) { fails++; System.out.println("FAIL no line, seed " + seed); continue; }
            if (voice.mentionsAvoided(r.text()) || r.text().matches(".*\\d.*")) { fails++; System.out.println("FAIL avoided word: " + r.text()); }
        }
        Corpus.Reply sample = voice.line(table, 7);
        System.out.println("sample line: [Recovery] " + sample.text());
        sample.fragments().forEach(f -> System.out.println("   " + f.words() + "  <-  " + f.source()));

        System.out.println(fails == 0 ? "all trailhead self-tests passed" : fails + " FAILURES");
        if (fails > 0) System.exit(1);
    }
}
