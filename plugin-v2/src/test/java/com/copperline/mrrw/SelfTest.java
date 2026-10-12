package com.copperline.mrrw;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Pure-logic checks for Morrow's voice (no server). Run: gradlew selftest */
public final class SelfTest {
    public static void main(String[] args) throws Exception {
        Corpus corpus = new Corpus();
        try (var in = SelfTest.class.getClassLoader().getResourceAsStream("canon/corpus.json")) {
            for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
                corpus.add(e.getAsJsonObject().get("text").getAsString(), e.getAsJsonObject().get("source").getAsString(), false);
            }
        }
        corpus.add("i will never tell anyone the secret word pineapple", "player, in Mossfield", true);
        int fails = 0;
        for (int act = 3; act <= 9; act++) {
            for (String heard : new String[] {"hello?", "who is at the farm", "is anyone there", "what happened to the tower", "rook"}) {
                Corpus.Reply r = corpus.reply(heard, act, heard.hashCode() * 31L + act);
                if (!corpus.obeysLaw(r, act)) { fails++; System.out.println("FAIL law: " + r.text()); }
                if (act < 7 && r.text().contains("pineapple")) { fails++; System.out.println("FAIL player words before act 7"); }
            }
        }
        Corpus.Reply sample = corpus.reply("who is at the farm", 4, 42);
        System.out.println("sample act 4 reply: " + sample.text());
        sample.fragments().forEach(f -> System.out.println("   " + f.words() + "  <-  " + f.source()));
        System.out.println(fails == 0 ? "all plugin self-tests passed" : fails + " FAILURES");
        if (fails > 0) System.exit(1);
    }
}
