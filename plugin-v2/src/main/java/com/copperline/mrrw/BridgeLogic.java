package com.copperline.mrrw;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The plugin side of the site/bot bridge, as pure logic (no Bukkit types): checked by {@code SelfTest}.
 *
 *  - request signatures: X-Mrrw-Sig = hex HMAC-SHA256(key = stop secret,
 *    METHOD + "\n" + PATH_WITH_QUERY + "\n" + TS + "\n" + BODY), |now - ts| <= 120 s;
 *  - the auto-advance rule table (which desk ref moves which act);
 *  - the USB group vote (strict plurality among votes cast by at least half the residents);
 *  - the Act 10 skip line (a resident's own "leave it unfinished" line, if anyone ever typed one).
 */
final class BridgeLogic {
    private BridgeLogic() { }

    // ---- signatures ---------------------------------------------------------------------------

    static final long MAX_SKEW_SECONDS = 120;

    /** Lowercase hex HMAC-SHA256(key = secret, METHOD \n PATH_WITH_QUERY \n TS \n BODY), all UTF-8. */
    static String sign(String secret, String method, String pathWithQuery, String ts, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((method + "\n" + pathWithQuery + "\n" + ts + "\n").getBytes(StandardCharsets.UTF_8));
            mac.update(body == null ? new byte[0] : body);
            return java.util.HexFormat.of().formatHex(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String sign(String secret, String method, String pathWithQuery, String ts, String body) {
        return sign(secret, method, pathWithQuery, ts, body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    /** True when ts is a unix-seconds integer within MAX_SKEW_SECONDS of now and sig matches (constant time). */
    static boolean verify(String secret, String method, String pathWithQuery, String ts, byte[] body, String sig, long nowSeconds) {
        if (ts == null || sig == null) return false;
        long t;
        try {
            t = Long.parseLong(ts.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(nowSeconds - t) > MAX_SKEW_SECONDS) return false;
        byte[] want = sign(secret, method, pathWithQuery, ts.trim(), body).getBytes(StandardCharsets.US_ASCII);
        byte[] got = sig.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(want, got);
    }

    // ---- auto-advance -------------------------------------------------------------------------

    /** Desk refs that move the act, as ref -> the act they finish (the advance is from -> from + 1). */
    static final Map<String, Integer> DESK_ADVANCES = new LinkedHashMap<>();
    static {
        DESK_ADVANCES.put("logblock_morrow", 3);
        DESK_ADVANCES.put("morrow", 3);
        DESK_ADVANCES.put("found_morrow", 3);
        DESK_ADVANCES.put("unmask", 4);
        DESK_ADVANCES.put("floor", 5);
        DESK_ADVANCES.put("moss_0518", 5);
        DESK_ADVANCES.put("restart", 6);
        DESK_ADVANCES.put("key", 7);
        DESK_ADVANCES.put("kit", 8);
        DESK_ADVANCES.put("ingest", 8);
        DESK_ADVANCES.put("brief", 9);
    }

    /** Theo voices the bot posts to #general when their reply time passes. */
    static final Set<String> THEO_VOICES = Set.of("T-04a", "T-06a", "T-08a", "T-09a", "T-09b");

    /** Minutes one resident must have been online (from Act 2) before the next dawn moves 2 -> 3. */
    static final long SESSION_MINUTES = 45;

    /** The act this desk ref moves the campaign to from {@code act}, or 0 when it moves nothing now. */
    static int deskTarget(int act, String ref) {
        Integer from = ref == null ? null : DESK_ADVANCES.get(ref.trim().toLowerCase(Locale.ROOT));
        return from != null && from == act ? act + 1 : 0;
    }

    /** True when a ref has an advance rule whose act is still ahead (it may apply later, if re-sent). */
    static boolean deskEarly(int act, String ref) {
        Integer from = ref == null ? null : DESK_ADVANCES.get(ref.trim().toLowerCase(Locale.ROOT));
        return from != null && act < from;
    }

    /** 1 -> 2: the first resident joins. */
    static int joinTarget(int act) {
        return act == 1 ? 2 : 0;
    }

    /** 2 -> 3: someone reached SESSION_MINUTES online at {@code sessionAt}, and a dawn job ran after that. */
    static int dawnTarget(int act, long sessionAtMillis, long dawnAtMillis) {
        return act == 2 && sessionAtMillis > 0 && dawnAtMillis > sessionAtMillis ? 3 : 0;
    }

    // ---- the USB vote -------------------------------------------------------------------------

    static final List<String> CHOICES = List.of("clean", "recovered", "corpus");

    record Usb(boolean open, Map<String, Integer> votes, String winner) {
        JsonObject json() {
            JsonObject o = new JsonObject();
            JsonObject v = new JsonObject();
            votes.forEach(v::addProperty);
            o.add("votes", v);
            if (winner == null) o.add("winner", com.google.gson.JsonNull.INSTANCE);
            else o.addProperty("winner", winner);
            o.addProperty("open", open);
            return o;
        }
    }

    /** Counts per choice; unknown choices are ignored. */
    static Map<String, Integer> tally(Map<String, String> ballots) {
        Map<String, Integer> n = new LinkedHashMap<>();
        for (String c : CHOICES) n.put(c, 0);
        for (String choice : ballots.values()) {
            String c = choice == null ? "" : choice.trim().toLowerCase(Locale.ROOT);
            if (n.containsKey(c)) n.merge(c, 1, Integer::sum);
        }
        return n;
    }

    /** Strict plurality among the votes cast, if at least half of the residents voted; else null. */
    static String winner(Map<String, Integer> votes, int players) {
        int cast = votes.values().stream().mapToInt(Integer::intValue).sum();
        if (cast == 0 || cast * 2 < players) return null;
        String best = null;
        int top = -1;
        boolean tie = false;
        for (var e : votes.entrySet()) {
            if (e.getValue() > top) {
                top = e.getValue();
                best = e.getKey();
                tie = false;
            } else if (e.getValue() == top) {
                tie = true;
            }
        }
        return tie ? null : best;
    }

    /** plugins/mrrw/usb-votes.json, written by the Discord bot: {"open": bool, "votes": {"<id>": choice}}. */
    static Usb readUsb(File file, int players) {
        boolean open = false;
        Map<String, String> ballots = new LinkedHashMap<>();
        if (file.isFile()) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                open = o.has("open") && o.get("open").getAsBoolean();
                if (o.has("votes") && o.get("votes").isJsonObject()) {
                    for (var e : o.getAsJsonObject("votes").entrySet()) {
                        JsonElement v = e.getValue();
                        if (v.isJsonPrimitive()) ballots.put(e.getKey(), v.getAsString());
                    }
                }
            } catch (Exception malformed) {
                // a half-written or broken file counts as no votes; the bot rewrites it on the next vote
            }
        }
        Map<String, Integer> votes = tally(ballots);
        return new Usb(open, votes, winner(votes, players));
    }

    // ---- the Act 10 skip line -----------------------------------------------------------------

    private static final List<String> UNFINISHED = List.of("uneven", "leave it", "don't finish", "dont finish", "unfinished", "not done");

    /** A resident's line that asks for something to be left unfinished. */
    static boolean unfinished(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT).replace('’', '\'').replace('‘', '\'');
        for (String p : UNFINISHED) if (t.contains(p)) return true;
        return false;
    }

    record Said(String name, String text, long at) { }

    /** The earliest (by time, then by order) line that asks for the unfinished; null if none. */
    static Said earliestUnfinished(List<Said> said) {
        Said best = null;
        for (Said s : said) {
            if (!unfinished(s.text())) continue;
            if (best == null || (s.at() > 0 && (best.at() <= 0 || s.at() < best.at()))) best = s;
        }
        return best;
    }

    static final String SKIP_NONE = "skip mossfield:south_rail (unfinished) reason=none";

    /** The stop log's skip line, with the resident's own words if one asked for it. */
    static String skipLine(Said s, ZoneId zone) {
        if (s == null) return SKIP_NONE;
        String text = s.text().trim();
        if (text.length() > 60) text = text.substring(0, 60).trim();
        LocalDate day = LocalDate.ofInstant(Instant.ofEpochMilli(s.at() > 0 ? s.at() : System.currentTimeMillis()), zone);
        return "skip mossfield:south_rail (unfinished) reason=\"" + text + "\" (" + s.name() + ", " + day + ")";
    }

    /** The canon stop log with its south_rail line set to {@code skip} (the line is added if the canon lacks it). */
    static List<String> stopLog(List<String> canonLog, String skip) {
        List<String> log = new java.util.ArrayList<>(canonLog);
        boolean found = false;
        for (int i = 0; i < log.size(); i++) {
            String l = log.get(i);
            int at = l.indexOf("skip mossfield:south_rail");
            if (at >= 0) {
                log.set(i, l.substring(0, at) + skip);
                found = true;
            }
        }
        if (!found) log.add(Math.max(0, log.size() - 1), "morrow: " + skip);
        return log;
    }
}
