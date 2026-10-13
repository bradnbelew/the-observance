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
    private String firstUnfinished;

    public PlayerRoster(File dataFolder) {
        file = new File(dataFolder, "roster.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            names.addAll(y.getStringList("names"));
            lines.addAll(y.getStringList("lines"));
            firstUnfinished = y.getString("first_unfinished");
        }
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
        if (names.add(name)) save();
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
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
