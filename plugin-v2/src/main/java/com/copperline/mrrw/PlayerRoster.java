package com.copperline.mrrw;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Players who have joined during the campaign (names only), and the chat lines they knowingly
 * typed in the ARG. Morrow may reuse those lines from Act 7 and list the players as residents
 * from Act 9. Nothing outside the server is ever recorded.
 *
 * Each line is stored as "name\0text\0epochMillis" (older entries have no time). The first line
 * that asks for something to be left unfinished is kept on its own as well, so the Act 10 stop
 * log can quote it even after the 2000-line window has moved past it.
 */
public final class PlayerRoster {
    private final File file;
    private final Set<String> names = new LinkedHashSet<>();
    private final List<String> lines = new ArrayList<>();
    private final Set<String> excluded = new LinkedHashSet<>();          // /mrrw residents remove: never a resident again
    private final java.util.Map<String, Long> lastSeen = new java.util.LinkedHashMap<>();  // for the USB quorum
    private String firstUnfinished;

    public PlayerRoster(File dataFolder) {
        file = new File(dataFolder, "roster.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            names.addAll(y.getStringList("names"));
            lines.addAll(y.getStringList("lines"));
            firstUnfinished = y.getString("first_unfinished");
            excluded.addAll(y.getStringList("excluded"));
            var seen = y.getConfigurationSection("last_seen");
            if (seen != null) for (String k : seen.getKeys(false)) lastSeen.put(k, seen.getLong(k));
        }
    }

    /**
     * One rule for "resident" everywhere (act triggers, roster, corpus, quorum, figures): a player who
     * is not op (or any player with auto_advance_count_ops), or one the operator added by name; never
     * one the operator removed.
     */
    public boolean resident(org.bukkit.plugin.Plugin plugin, org.bukkit.entity.Player p) {
        if (excluded.contains(p.getName())) return false;
        return !p.isOp() || plugin.getConfig().getBoolean("auto_advance_count_ops", false) || names.contains(p.getName());
    }

    /** /mrrw residents add: counts from now on, op or not. */
    public void add(String name) {
        excluded.remove(name);
        names.add(name);
        lastSeen.putIfAbsent(name, System.currentTimeMillis());
        save();
    }

    /** /mrrw residents remove: out of the roster and the quorum, and not re-added on the next join. */
    public boolean remove(String name) {
        boolean had = names.remove(name);
        excluded.add(name);
        save();
        return had;
    }

    public Set<String> excluded() {
        return excluded;
    }

    public java.util.Map<String, Long> lastSeen() {
        return lastSeen;
    }

    /** A resident was on the server just now (join and quit). */
    public void seen(String name) {
        if (!names.contains(name)) return;
        lastSeen.put(name, System.currentTimeMillis());
        save();
    }

    public Set<String> names() {
        return names;
    }

    /** Every remembered line, oldest first. */
    public List<BridgeLogic.Said> said() {
        List<BridgeLogic.Said> out = new ArrayList<>();
        for (String l : lines) {
            BridgeLogic.Said s = parse(l);
            if (s != null) out.add(s);
        }
        return out;
    }

    /** The earliest line any resident typed asking for something to be left unfinished; null if none. */
    public BridgeLogic.Said firstUnfinished() {
        List<BridgeLogic.Said> all = said();
        BridgeLogic.Said kept = firstUnfinished == null ? null : parse(firstUnfinished);
        if (kept != null) all.add(0, kept);
        return BridgeLogic.earliestUnfinished(all);
    }

    private static BridgeLogic.Said parse(String l) {
        String[] parts = l.split("\u0000", 3);
        if (parts.length < 2) return null;
        long at = 0;
        if (parts.length == 3) {
            try {
                at = Long.parseLong(parts[2]);
            } catch (NumberFormatException ignored) {
                // an untimed line
            }
        }
        return new BridgeLogic.Said(parts[0], parts[1], at);
    }

    public void join(String name) {
        if (excluded.contains(name)) return;
        names.add(name);
        seen(name);
    }

    public void said(String name, String text) {
        String entry = name + "\u0000" + text + "\u0000" + System.currentTimeMillis();
        lines.add(entry);
        if (lines.size() > 2000) lines.remove(0);
        if (firstUnfinished == null && BridgeLogic.unfinished(text)) firstUnfinished = entry;
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("names", List.copyOf(names));
        y.set("lines", lines);
        if (firstUnfinished != null) y.set("first_unfinished", firstUnfinished);
        y.set("excluded", List.copyOf(excluded));
        lastSeen.forEach((k, v) -> y.set("last_seen." + k, v));
        try {
            State.saveAtomic(y, file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
