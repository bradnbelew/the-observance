package com.copperline.mrrw;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The campaign clock: acts advance by themselves from what the game and the site already log
 * (config auto_advance, default true). /mrrw act N still works and always wins.
 *
 *   1 -> 2  the first resident joins Mossfield
 *   2 -> 3  a resident has been online 45 minutes (from Act 2), and a dawn job has run since
 *   3 -> 4  desk ref logblock_morrow, morrow or found_morrow
 *   4 -> 5  desk ref unmask
 *   5 -> 6  desk ref floor or moss_0518
 *   6 -> 7  desk ref restart
 *   7 -> 8  desk ref key
 *   8 -> 9  desk ref kit or ingest
 *   9 -> 10 desk ref brief
 *
 * Desk refs arrive from the website over the bridge (POST /event). A ref with a Theo voice
 * (T-04a, T-06a, T-08a, T-09a, T-09b) queues a "theo.reply" journal entry for its reply_at,
 * which the Discord bot posts; the advance happens at the same moment (or at once, with no
 * reply_at). Everything is persisted in plugins/mrrw/bridge-state.json, so a restart loses nothing.
 * "Resident" = a player who is not op, unless auto_advance_count_ops is true (so the operator can
 * look around at Act 1 without starting the game).
 */
public final class Campaign implements Listener {
    private final Plugin plugin;
    private final State state;
    private final Journal journal;
    private final Runnable applyAct;
    private final File file;
    private JsonObject data;

    public Campaign(Plugin plugin, State state, Journal journal, Runnable applyAct) {
        this.plugin = plugin;
        this.state = state;
        this.journal = journal;
        this.applyAct = applyAct;
        this.file = new File(plugin.getDataFolder(), "bridge-state.json");
        load();
        if (state.stamp("act_started") == 0) state.stampOnce("act_started", System.currentTimeMillis());
        Bukkit.getScheduler().runTaskTimer(plugin, this::releaseDue, 40L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::minute, 1200L, 1200L);
    }

    // ---- persistence ----------------------------------------------------------------------------

    private void load() {
        data = new JsonObject();
        if (file.isFile()) {
            try {
                data = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                plugin.getLogger().warning("bridge-state.json unreadable, starting empty: " + e);
            }
        }
        if (!data.has("desk")) data.add("desk", new JsonObject());
        if (!data.has("online_minutes")) data.add("online_minutes", new JsonObject());
    }

    private void save() {
        try {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("could not save bridge-state.json: " + e);
        }
    }

    private JsonObject desk() {
        return data.getAsJsonObject("desk");
    }

    // ---- advancing ------------------------------------------------------------------------------

    public boolean autoAdvance() {
        return plugin.getConfig().getBoolean("auto_advance", true);
    }

    private boolean resident(Player p) {
        return !p.isOp() || plugin.getConfig().getBoolean("auto_advance_count_ops", false);
    }

    /** Moves the act to {@code to} if it is exactly the next act. Returns true if it moved. */
    boolean advance(int to, String reason) {
        int from = state.act();
        if (to <= 0 || to != from + 1 || state.done("stopped")) return false;
        if (!autoAdvance()) {
            JsonObject d = new JsonObject();
            d.addProperty("from", from);
            d.addProperty("to", to);
            d.addProperty("reason", reason);
            journal.write("act.advance_held", d);
            plugin.getLogger().info("auto_advance is off: would have moved act " + from + " -> " + to + " (" + reason + ")");
            return false;
        }
        long now = System.currentTimeMillis();
        state.setAct(to);
        state.setStamp("act_started", now);
        JsonObject d = new JsonObject();
        d.addProperty("from", from);
        d.addProperty("to", to);
        d.addProperty("reason", reason);
        d.addProperty("by", "auto");
        d.addProperty("act_started", iso(now));
        journal.write("act.advance", d);
        plugin.getLogger().info("act " + from + " -> " + to + " (" + reason + ")");
        applyAct.run();
        return true;
    }

    /** /mrrw act N [started]: the operator always wins. */
    void operatorSet(int act, String by, long startedMillis) {
        int from = state.act();
        state.setAct(act);
        state.setStamp("act_started", startedMillis);
        JsonObject d = new JsonObject();
        d.addProperty("from", from);
        d.addProperty("to", state.act());
        d.addProperty("reason", "operator");
        d.addProperty("by", by);
        d.addProperty("act_started", iso(startedMillis));
        journal.write("act.advance", d);
    }

    public long actStarted() {
        return state.stamp("act_started");
    }

    // ---- plugin-side triggers -------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!resident(p) || BridgeLogic.joinTarget(state.act()) == 0) return;
        String name = p.getName();
        Bukkit.getScheduler().runTask(plugin, () -> advance(BridgeLogic.joinTarget(state.act()), "first resident joined: " + name));
    }

    /** Every minute: residents' online time, counted from Act 2 (the first real session). */
    private void minute() {
        if (state.act() < 2 || state.done("stopped")) return;
        JsonObject mins = data.getAsJsonObject("online_minutes");
        boolean changed = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!resident(p)) continue;
            int m = (mins.has(p.getName()) ? mins.get(p.getName()).getAsInt() : 0) + 1;
            mins.addProperty(p.getName(), m);
            changed = true;
            if (!data.has("session_at") && m >= BridgeLogic.SESSION_MINUTES) {
                long now = System.currentTimeMillis();
                data.addProperty("session_at", now);
                data.addProperty("session_by", p.getName());
                JsonObject d = new JsonObject();
                d.addProperty("player", p.getName());
                d.addProperty("minutes", m);
                journal.write("auto.first_session", d);
            }
        }
        if (changed) save();
    }

    /** A dawn job ran (in-game sunrise or /mrrw dawn). */
    void onDawn() {
        long at = data.has("session_at") ? data.get("session_at").getAsLong() : 0;
        int to = BridgeLogic.dawnTarget(state.act(), at, System.currentTimeMillis());
        if (to == 0) return;
        String by = data.has("session_by") ? data.get("session_by").getAsString() : "a resident";
        Bukkit.getScheduler().runTask(plugin, () -> advance(to, "first real session (" + by + ", "
                + BridgeLogic.SESSION_MINUTES + " min online) and a dawn since"));
    }

    // ---- the site's desk events -----------------------------------------------------------------

    record DeskResult(boolean duplicate, int act) { }

    /** POST /event kind=desk. Main thread. Idempotent per ref once applied. */
    DeskResult desk(String ref, Long replyAtMillis, String voice, String subject) {
        String key = ref.trim().toLowerCase(Locale.ROOT);
        JsonObject e = desk().getAsJsonObject(key);
        JsonObject j = new JsonObject();
        j.addProperty("ref", key);
        j.addProperty("act", state.act());
        if (e != null) {
            j.addProperty("duplicate", true);
            journal.write("site.desk", j);
            // a ref that arrived before its act did not move anything; a repeat may move it now
            if (e.get("released").getAsBoolean() && !e.get("applied").getAsBoolean()) {
                tryAdvance(key, e);
                save();
            }
            return new DeskResult(true, state.act());
        }
        long now = System.currentTimeMillis();
        e = new JsonObject();
        e.addProperty("received", now);
        if (replyAtMillis == null) e.add("reply_at", JsonNull.INSTANCE);
        else e.addProperty("reply_at", replyAtMillis);
        if (voice == null) e.add("voice", JsonNull.INSTANCE);
        else e.addProperty("voice", voice);
        if (subject == null) e.add("subject", JsonNull.INSTANCE);
        else e.addProperty("subject", subject);
        e.addProperty("released", false);
        e.addProperty("applied", false);
        desk().add(key, e);
        j.add("reply_at", replyAtMillis == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(iso(replyAtMillis)));
        j.add("voice", e.get("voice"));
        j.add("subject", e.get("subject"));
        journal.write("site.desk", j);
        save();
        releaseDue();
        return new DeskResult(false, state.act());
    }

    /** Releases every desk event whose reply time has come: Theo's reply, then the advance. */
    private void releaseDue() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (var entry : desk().entrySet()) {
            JsonObject e = entry.getValue().getAsJsonObject();
            if (e.get("released").getAsBoolean()) continue;
            JsonElement ra = e.get("reply_at");
            if (ra != null && !ra.isJsonNull() && ra.getAsLong() > now) continue;
            e.addProperty("released", true);
            changed = true;
            String voice = str(e.get("voice"));
            if (voice != null && BridgeLogic.THEO_VOICES.contains(voice)) {
                JsonObject d = new JsonObject();
                d.addProperty("id", voice);
                d.addProperty("ref", entry.getKey());
                d.add("subject", e.get("subject"));
                d.addProperty("at", iso(replyAt(e)));
                journal.write("theo.reply", d);
            }
            tryAdvance(entry.getKey(), e);
        }
        if (changed) save();
    }

    private void tryAdvance(String ref, JsonObject e) {
        int act = state.act();
        int to = BridgeLogic.deskTarget(act, ref);
        if (to > 0) {
            e.addProperty("applied", advance(to, "desk ref " + ref));
        } else if (BridgeLogic.deskEarly(act, ref)) {
            e.addProperty("applied", false);  // its act is still ahead: a repeat of this ref may apply it later
            JsonObject d = new JsonObject();
            d.addProperty("ref", ref);
            d.addProperty("act", act);
            d.addProperty("needs_act", BridgeLogic.DESK_ADVANCES.get(ref));
            journal.write("act.advance_early", d);
        } else {
            e.addProperty("applied", true);  // no rule, or its act is already past
        }
    }

    private static long replyAt(JsonObject e) {
        JsonElement ra = e.get("reply_at");
        return ra == null || ra.isJsonNull() ? e.get("received").getAsLong() : ra.getAsLong();
    }

    /** Scheduled and announced Theo replies, oldest first, the last 20. */
    JsonArray theo() {
        List<JsonObject> out = new ArrayList<>();
        for (var entry : desk().entrySet()) {
            JsonObject e = entry.getValue().getAsJsonObject();
            String voice = str(e.get("voice"));
            if (voice == null || !BridgeLogic.THEO_VOICES.contains(voice)) continue;
            JsonObject t = new JsonObject();
            t.addProperty("id", voice);
            t.addProperty("at", iso(replyAt(e)));
            t.add("subject", e.get("subject"));
            t.addProperty("released", e.get("released").getAsBoolean());
            t.addProperty("_sort", replyAt(e));
            out.add(t);
        }
        out.sort(Comparator.comparingLong(o -> o.get("_sort").getAsLong()));
        JsonArray a = new JsonArray();
        for (JsonObject o : out.subList(Math.max(0, out.size() - 20), out.size())) {
            o.remove("_sort");
            a.add(o);
        }
        return a;
    }

    private static String str(JsonElement e) {
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    static String iso(long millis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** ISO-8601 with an offset (or Z); null for null/blank. Throws on anything else. */
    static Long parseIso(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s.trim()).toInstant().toEpochMilli();
        } catch (java.time.format.DateTimeParseException e) {
            return Instant.parse(s.trim()).toEpochMilli();
        }
    }
}
