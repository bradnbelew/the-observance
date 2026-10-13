package com.copperline.mrrw;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Campaign act plus one-shot flags, persisted to plugins/mrrw/state.yml. Restart-safe. */
public final class State {
    private final File file;
    private int act = 1;
    private final Set<String> done = new HashSet<>();
    private final Map<String, Long> stamps = new LinkedHashMap<>();

    public State(File dataFolder) {
        this.file = new File(dataFolder, "state.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            act = Math.max(1, Math.min(10, y.getInt("act", 1)));
            done.addAll(y.getStringList("done"));
            var st = y.getConfigurationSection("stamps");
            if (st != null) for (String k : st.getKeys(false)) stamps.put(k, st.getLong(k));
        }
    }

    public int act() {
        return act;
    }

    public void setAct(int act) {
        this.act = Math.max(1, Math.min(10, act));
        save();
    }

    public boolean done(String flag) {
        return done.contains(flag);
    }

    public void markDone(String flag) {
        if (done.add(flag)) save();
    }

    /** A remembered moment (epoch ms), e.g. when Morrow ingested Kit's line; 0 if never. */
    public long stamp(String key) {
        return stamps.getOrDefault(key, 0L);
    }

    /** Records the moment once; returns the stored value. */
    public long stampOnce(String key, long millis) {
        Long had = stamps.putIfAbsent(key, millis);
        if (had == null) save();
        return had == null ? millis : had;
    }

    /** Sets (or moves) a remembered moment, e.g. "act_started" on every act change. */
    public void setStamp(String key, long millis) {
        stamps.put(key, millis);
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("act", act);
        y.set("done", List.copyOf(done));
        stamps.forEach((k, v) -> y.set("stamps." + k, v));
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("could not save " + file, e);
        }
    }
}
