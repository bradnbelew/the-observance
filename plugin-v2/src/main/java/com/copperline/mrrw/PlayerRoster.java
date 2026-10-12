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
 */
public final class PlayerRoster {
    private final File file;
    private final Set<String> names = new LinkedHashSet<>();
    private final List<String> lines = new ArrayList<>();

    public PlayerRoster(File dataFolder) {
        file = new File(dataFolder, "roster.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            names.addAll(y.getStringList("names"));
            lines.addAll(y.getStringList("lines"));
        }
    }

    public Set<String> names() {
        return names;
    }

    public List<String> lines() {
        return lines;
    }

    public void join(String name) {
        if (names.add(name)) save();
    }

    public void said(String name, String text) {
        lines.add(name + "\u0000" + text);
        if (lines.size() > 2000) lines.remove(0);
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("names", List.copyOf(names));
        y.set("lines", lines);
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
