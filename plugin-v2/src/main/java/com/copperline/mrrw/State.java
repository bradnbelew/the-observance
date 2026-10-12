package com.copperline.mrrw;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Campaign act plus one-shot flags, persisted to plugins/mrrw/state.yml. Restart-safe. */
public final class State {
    private final File file;
    private int act = 1;
    private final Set<String> done = new HashSet<>();

    public State(File dataFolder) {
        this.file = new File(dataFolder, "state.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            act = Math.max(1, Math.min(10, y.getInt("act", 1)));
            done.addAll(y.getStringList("done"));
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

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("act", act);
        y.set("done", List.copyOf(done));
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("could not save " + file, e);
        }
    }
}
