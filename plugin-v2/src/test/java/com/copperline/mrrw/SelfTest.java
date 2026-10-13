package com.copperline.mrrw;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Pure-logic checks for Morrow's voice (no server). Run: gradlew selftest */
public final class SelfTest {
    private static int fails;

    private static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            System.out.println("FAIL " + what);
        }
    }

    private static Corpus baseCorpus() throws Exception {
        Corpus corpus = new Corpus();
        try (var in = SelfTest.class.getClassLoader().getResourceAsStream("canon/corpus.json")) {
            for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
                corpus.add(e.getAsJsonObject().get("text").getAsString(), e.getAsJsonObject().get("source").getAsString(), false);
            }
        }
        return corpus;
    }

    public static void main(String[] args) throws Exception {
        JsonObject canon;
        try (var in = SelfTest.class.getClassLoader().getResourceAsStream("canon/canon.json")) {
            canon = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        // ---- the corpus law ------------------------------------------------------------------
        Corpus corpus = baseCorpus();
        corpus.add("i will never tell anyone the secret word pineapple", "player, in Mossfield", true);
        for (int act = 3; act <= 9; act++) {
            for (String heard : new String[] {"hello?", "who is at the farm", "is anyone there", "what happened to the tower", "rook"}) {
                Corpus.Reply r = corpus.reply(heard, act, heard.hashCode() * 31L + act);
                check(corpus.obeysLaw(r, act), "law: " + r.text());
                check(act >= 7 || !r.text().contains("pineapple"), "player words before act 7");
            }
        }
        Corpus.Reply sample = corpus.reply("who is at the farm", 4, 42);
        System.out.println("sample act 4 reply: " + sample.text());
        sample.fragments().forEach(f -> System.out.println("   " + f.words() + "  <-  " + f.source()));

        // ---- fix 10: Kit's line, ingested at Act 8, actually reaches players ----------------------
        String kit = canon.get("kit_line").getAsString();
        Corpus withKit = baseCorpus();
        long now = 1_800_000_000_000L, ingested = now - 2L * 24 * 3_600_000;  // two days into Act 8
        withKit.add(kit, "theo's inbox, 2019-03-02", false, ingested);
        String kitNorm = Corpus.normalise(kit);
        int withFresh = 0, without = 0, n = 400;
        String[] chat = {"hello?", "who is at the farm", "is anyone there", "kit are you there", "what is this", "turn it off", "rook", "where is lowpath"};
        for (int i = 0; i < n; i++) {
            String heard = chat[i % chat.length];
            Corpus.Reply fresh = withKit.reply(heard, 8, i * 7919L, now - 7L * 24 * 3_600_000);
            Corpus.Reply plain = withKit.reply(heard, 8, i * 7919L);
            check(withKit.obeysLaw(fresh, 8), "law with fresh lines: " + fresh.text());
            if (fresh.fragments().stream().anyMatch(f -> f.source().startsWith("theo's inbox"))) withFresh++;
            if (plain.fragments().stream().anyMatch(f -> f.source().startsWith("theo's inbox"))) without++;
        }
        System.out.printf("kit's line in act 8 replies: %d/%d preferring fresh lines (was %d/%d)%n", withFresh, n, without, n);
        check(withFresh >= n * 0.3, "kit's line too rare even when fresh: " + withFresh + "/" + n);
        Corpus.Reply stale = withKit.reply("kit are you there", 8, 5, now + 1);  // nothing ingested after "now"
        check(stale.text().equals(withKit.reply("kit are you there", 8, 5).text()), "no fresh lines must mean the old behaviour");
        check(kitNorm.split(" ").length >= 3, "kit line too short for fragments");

        // ---- fix 1: the sound replay decodes, by subtitle and by vibration --------------------------
        List<Integer> freqs = new ArrayList<>();
        for (JsonElement e : canon.getAsJsonArray("sound_replay")) freqs.add(e.getAsJsonObject().get("frequency").getAsInt());
        String decoded = Vibrations.decode(freqs);
        System.out.println("sound replay: " + freqs.size() + " events, " + Vibrations.SPACING_TICKS + " ticks apart, decodes to " + decoded);
        check(decoded.equals("MIC-5-18-B.WAV"), "sound replay decodes to " + decoded);
        for (int f : freqs) {
            String[] pair = Vibrations.REPLAY.get(f);
            check(pair != null, "no sound for frequency " + f);
            if (pair == null) continue;
            Integer byEvent = Vibrations.VANILLA_FREQUENCY.get(pair[1]);
            Integer bySubtitle = Vibrations.VANILLA_FREQUENCY.get(Vibrations.SOUND_NATURAL_EVENT.get(pair[0]));
            check(byEvent != null && byEvent == f, "game event " + pair[1] + " is not frequency " + f);
            check(bySubtitle != null && bySubtitle == f, "sound " + pair[0] + " suggests frequency " + bySubtitle + ", not " + f);
        }
        for (int f = 1; f <= 15; f++) check(Vibrations.REPLAY.containsKey(f), "frequency " + f + " has no replay sound");
        check(Vibrations.SPACING_TICKS >= Vibrations.MIN_SPACING_TICKS, "events closer than a sculk sensor's 40-tick cycle");
        check(Vibrations.tickFor(freqs.size() - 1) / 20.0 < 180, "replay longer than its 180 s cooldown");
        // the audit's wrong picks really were wrong (bucket fill = fluid_pickup 12, generic equip = 5)
        check(Vibrations.VANILLA_FREQUENCY.get(Vibrations.SOUND_NATURAL_EVENT.get("item.bucket.fill")) == 12, "bucket fill table");
        check(Vibrations.VANILLA_FREQUENCY.get(Vibrations.SOUND_NATURAL_EVENT.get("item.armor.equip_generic")) == 5, "generic equip table");

        // ---- fix 6: stop-secret fingerprint (the website computes the same) -------------------------
        String fp = StopCode.fingerprint("mrrw-dev-stop-secret");
        System.out.println("fingerprint(mrrw-dev-stop-secret) = " + fp);
        check(fp.matches("[0-9a-f]{8}"), "fingerprint format " + fp);
        check(!fp.equals(StopCode.fingerprint("something-else")), "fingerprint must depend on the secret");

        // ---- the bridge: request signatures (the website and bot sign the same way) ---------------
        String vector = BridgeLogic.sign("test-stop-secret", "GET", "/state", "1700000000", "");
        System.out.println("bridge sig vector: key=test-stop-secret GET /state ts=1700000000 body=\"\" -> " + vector);
        check(vector.matches("[0-9a-f]{64}"), "sig format " + vector);
        byte[] none = new byte[0];
        check(BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, vector, 1_700_000_000L), "good sig rejected");
        check(BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, vector, 1_700_000_120L), "120 s skew must pass");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, vector, 1_700_000_121L), "121 s skew must fail");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, vector, 1_699_999_879L), "-121 s skew must fail");
        check(!BridgeLogic.verify("other-secret", "GET", "/state", "1700000000", none, vector, 1_700_000_000L), "wrong key accepted");
        check(!BridgeLogic.verify("test-stop-secret", "POST", "/state", "1700000000", none, vector, 1_700_000_000L), "wrong method accepted");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state?x=1", "1700000000", none, vector, 1_700_000_000L), "wrong path accepted");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", "x".getBytes(StandardCharsets.UTF_8), vector, 1_700_000_000L), "wrong body accepted");
        String tampered = vector.substring(0, 63) + (vector.endsWith("0") ? "1" : "0");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, tampered, 1_700_000_000L), "tampered sig accepted");
        check(BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, vector.toUpperCase(), 1_700_000_000L), "uppercase hex tolerated");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "soon", none, vector, 1_700_000_000L), "non-numeric ts accepted");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", null, none, vector, 1_700_000_000L), "missing ts accepted");
        check(!BridgeLogic.verify("test-stop-secret", "GET", "/state", "1700000000", none, null, 1_700_000_000L), "missing sig accepted");
        String body = "{\"kind\":\"desk\",\"ref\":\"unmask\",\"reply_at\":null,\"voice\":\"T-04a\",\"subject\":\"Re: who's Theo\"}";
        String postSig = BridgeLogic.sign("test-stop-secret", "POST", "/event", "1700000000", body);
        System.out.println("bridge sig vector: key=test-stop-secret POST /event ts=1700000000 body=" + body + " -> " + postSig);
        check(BridgeLogic.verify("test-stop-secret", "POST", "/event", "1700000000", body.getBytes(StandardCharsets.UTF_8), postSig, 1_700_000_000L), "post sig");
        String q = "/corpus?heard=hello%20there&persona=rookery&seed=7";
        String qSig = BridgeLogic.sign("test-stop-secret", "GET", q, "1700000000", "");
        System.out.println("bridge sig vector: key=test-stop-secret GET " + q + " ts=1700000000 body=\"\" -> " + qSig);
        var parsed = Bridge.query("heard=hello%20there&persona=rookery&seed=7");
        check("hello there".equals(parsed.get("heard")) && "rookery".equals(parsed.get("persona")) && "7".equals(parsed.get("seed")), "query parse " + parsed);

        // ---- the USB group vote ---------------------------------------------------------------------
        java.util.Map<String, String> ballots = new java.util.LinkedHashMap<>();
        ballots.put("1", "clean");
        ballots.put("2", "corpus");
        ballots.put("3", "corpus");
        ballots.put("4", "bogus");
        var tally = BridgeLogic.tally(ballots);
        check(tally.get("clean") == 1 && tally.get("corpus") == 2 && tally.get("recovered") == 0, "tally " + tally);
        check("corpus".equals(BridgeLogic.winner(tally, 6)), "3 of 6 voted, corpus leads");
        check(BridgeLogic.winner(tally, 7) == null, "3 of 7 is under half: no winner");
        check(BridgeLogic.winner(BridgeLogic.tally(java.util.Map.of("a", "clean", "b", "corpus")), 2) == null, "a tie has no winner");
        check(BridgeLogic.winner(BridgeLogic.tally(java.util.Map.of()), 0) == null, "no votes, no winner");
        check("recovered".equals(BridgeLogic.winner(BridgeLogic.tally(java.util.Map.of("a", "Recovered")), 1)), "1 of 1, case-insensitive");
        check("clean".equals(BridgeLogic.winner(BridgeLogic.tally(java.util.Map.of("a", "clean", "b", "clean", "c", "corpus")), 4)), "2-1 among 3 of 4");
        File usbDir = java.nio.file.Files.createTempDirectory("mrrw-usb").toFile();
        File usbFile = new File(usbDir, "usb-votes.json");
        check(BridgeLogic.readUsb(usbFile, 3).winner() == null && !BridgeLogic.readUsb(usbFile, 3).open(), "no file: closed, no winner");
        java.nio.file.Files.writeString(usbFile.toPath(), "{\"open\": true, \"votes\": {\"111\": \"clean\", \"222\": \"clean\", \"333\": \"recovered\"}}");
        BridgeLogic.Usb usb = BridgeLogic.readUsb(usbFile, 5);
        System.out.println("usb json (5 players): " + usb.json());
        check(usb.open() && "clean".equals(usb.winner()), "usb file winner");
        java.nio.file.Files.writeString(usbFile.toPath(), "{\"open\": tr");
        check(BridgeLogic.readUsb(usbFile, 1).winner() == null, "broken file counts as no votes");

        // ---- the Act 10 skip line -------------------------------------------------------------------
        java.time.ZoneId utc = java.time.ZoneOffset.UTC;
        long d1 = java.time.Instant.parse("2026-11-02T20:15:00Z").toEpochMilli(), d2 = d1 + 86_400_000L;
        List<BridgeLogic.Said> said = new ArrayList<>(List.of(
                new BridgeLogic.Said("ash", "where is the rail going", d1 - 5000),
                new BridgeLogic.Said("bryn", "honestly just LEAVE IT, the south rail looks better unfinished and a bit uneven like finch wanted it", d2),
                new BridgeLogic.Said("cole", "Don’t finish it", d1)));
        BridgeLogic.Said pick = BridgeLogic.earliestUnfinished(said);
        check(pick != null && pick.name().equals("cole"), "earliest unfinished line by time: " + pick);
        String skip = BridgeLogic.skipLine(pick, utc);
        System.out.println("skip line: " + skip);
        check(skip.equals("skip mossfield:south_rail (unfinished) reason=\"Don’t finish it\" (cole, 2026-11-02)"), "skip line " + skip);
        String longSkip = BridgeLogic.skipLine(said.get(1), utc);
        System.out.println("skip line (long): " + longSkip);
        check(longSkip.equals("skip mossfield:south_rail (unfinished) reason=\"honestly just LEAVE IT, the south rail looks better unfinish\" (bryn, 2026-11-03)"), "60-char trim " + longSkip);
        check(BridgeLogic.skipLine(BridgeLogic.earliestUnfinished(List.of(new BridgeLogic.Said("ash", "finish the rail!", d1))), utc)
                .equals("skip mossfield:south_rail (unfinished) reason=none"), "no such line keeps reason=none");
        for (String yes : new String[] {"it's not done", "Dont Finish", "UNFINISHED", "uneven is fine", "leave it"}) check(BridgeLogic.unfinished(yes), "should match: " + yes);
        for (String no : new String[] {"finish it", "leave", "done", "even"}) check(!BridgeLogic.unfinished(no), "should not match: " + no);
        List<String> canonLog = Canon.strings(canon.getAsJsonArray("stop_log"));
        List<String> played = BridgeLogic.stopLog(canonLog, skip);
        check(played.size() == canonLog.size(), "stop log keeps its length");
        check(played.stream().anyMatch(l -> l.equals("morrow: " + skip)), "skip line printed with the morrow: prefix");
        check(BridgeLogic.stopLog(canonLog, BridgeLogic.SKIP_NONE).equals(canonLog), "no reason = canon log unchanged");
        check(played.get(played.size() - 1).contains("exit 0"), "exit 0 still last");
        List<String> noSkip = BridgeLogic.stopLog(List.of("morrow: a", "morrow: exit 0"), skip);
        check(noSkip.size() == 3 && noSkip.get(1).equals("morrow: " + skip) && noSkip.get(2).equals("morrow: exit 0"), "skip line added before exit 0");

        // ---- the auto-advance rule table ------------------------------------------------------------
        check(BridgeLogic.joinTarget(1) == 2 && BridgeLogic.joinTarget(2) == 0, "join: 1 -> 2 only");
        long sess = 1_000_000L;
        check(BridgeLogic.dawnTarget(2, sess, sess + 1) == 3, "dawn after 45 min: 2 -> 3");
        check(BridgeLogic.dawnTarget(2, sess, sess - 1) == 0, "dawn before the session mark");
        check(BridgeLogic.dawnTarget(2, 0, sess) == 0, "no session yet");
        check(BridgeLogic.dawnTarget(3, sess, sess + 1) == 0, "dawn rule only in act 2");
        Object[][] table = {
                {3, "logblock_morrow", 4}, {3, "morrow", 4}, {3, "found_morrow", 4}, {4, "unmask", 5}, {5, "floor", 6}, {5, "moss_0518", 6},
                {6, "restart", 7}, {7, "key", 8}, {8, "kit", 9}, {8, "ingest", 9}, {9, "brief", 10}, {9, "BRIEF", 10},
        };
        for (Object[] row : table) {
            int act = (Integer) row[0];
            String ref = (String) row[1];
            check(BridgeLogic.deskTarget(act, ref) == (Integer) row[2], "desk " + ref + " at act " + act);
            check(BridgeLogic.deskTarget(act - 1, ref) == 0 && BridgeLogic.deskEarly(act - 1, ref), ref + " one act early must wait");
            check(BridgeLogic.deskTarget(act + 1, ref) == 0 && !BridgeLogic.deskEarly(act + 1, ref), ref + " one act late does nothing");
        }
        for (int act = 1; act <= 10; act++) check(BridgeLogic.deskTarget(act, "nonsense") == 0, "unknown ref moves nothing");
        check(BridgeLogic.deskTarget(10, "brief") == 0, "nothing past act 10");
        check(BridgeLogic.THEO_VOICES.equals(java.util.Set.of("T-04a", "T-06a", "T-08a", "T-09a", "T-09b")), "Theo voices");
        System.out.println("auto-advance: " + BridgeLogic.DESK_ADVANCES);

        // ---- the bridge's persona-biased corpus reply -----------------------------------------------
        Corpus pc = baseCorpus();
        pc.add("leave the south branch alone it is fine", "bryn, in Mossfield", true);
        int own = 0;
        for (int i = 0; i < 100; i++) {
            Corpus.Reply r = pc.reply("is the rail done", 7, i, Long.MIN_VALUE, "rookery");
            check(pc.obeysLaw(r, 7), "persona law: " + r.text());
            check(Corpus.sourceIs(r.fragments().get(0).source(), "rookery"), "persona listening piece from " + r.fragments().get(0).source());
            own += (int) r.fragments().stream().filter(f -> Corpus.sourceIs(f.source(), "rookery")).count();
        }
        System.out.println("persona rookery: " + own + " own fragments in 100 replies");
        Corpus.Reply bryn = pc.reply("hello", 7, 3, Long.MIN_VALUE, "bryn");
        check(Corpus.sourceIs(bryn.fragments().get(0).source(), "bryn"), "player persona from act 7");
        check(pc.reply("hello", 6, 3, Long.MIN_VALUE, "bryn").text().equals(pc.reply("hello", 6, 3, Long.MIN_VALUE).text()), "player persona before act 7 = plain reply");
        check(pc.reply("hello", 7, 3, Long.MIN_VALUE, "nobody").text().equals(pc.reply("hello", 7, 3, Long.MIN_VALUE).text()), "unknown persona = plain reply");

        System.out.println(fails == 0 ? "all plugin self-tests passed" : fails + " FAILURES");
        if (fails > 0) System.exit(1);
    }
}
