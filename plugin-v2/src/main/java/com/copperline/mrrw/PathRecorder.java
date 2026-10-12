package com.copperline.mrrw;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What Morrow learns from the players, and nothing else: where each player walked in Mossfield
 * during Acts 2-4 (sampled every 5 s, capped), and where they first logged in. Used for the Act 5
 * player-ghost, Act 9's recovery compass and the absorption pots. In-world only.
 */
public final class PathRecorder implements Listener {
    private static final int CAP = 240;
    private final File file;
    private final State state;
    private final Map<String, List<double[]>> paths = new LinkedHashMap<>();
    private final Map<String, double[]> firstJoin = new LinkedHashMap<>();

    public PathRecorder(Plugin plugin, State state) {
        this.state = state;
        this.file = new File(plugin.getDataFolder(), "paths.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            var ps = y.getConfigurationSection("paths");
            if (ps != null) for (String k : ps.getKeys(false)) {
                List<double[]> pts = new ArrayList<>();
                for (Object o : ps.getList(k, List.of())) {
                    List<?> l = (List<?>) o;
                    pts.add(new double[] {((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue(), ((Number) l.get(2)).doubleValue()});
                }
                paths.put(k, pts);
            }
            var fj = y.getConfigurationSection("first_join");
            if (fj != null) for (String k : fj.getKeys(false)) {
                List<Double> l = fj.getDoubleList(k);
                firstJoin.put(k, new double[] {l.get(0), l.get(1), l.get(2)});
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::sample, 100L, 100L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Location l = e.getPlayer().getLocation();
        if (firstJoin.putIfAbsent(e.getPlayer().getName(), new double[] {l.getX(), l.getY(), l.getZ()}) == null) save();
    }

    private void sample() {
        int act = state.act();
        if (act < 2 || act > 4) return;
        boolean changed = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            List<double[]> pts = paths.computeIfAbsent(p.getName(), k -> new ArrayList<>());
            if (pts.size() >= CAP) continue;
            Location l = p.getLocation();
            pts.add(new double[] {Math.round(l.getX() * 10) / 10.0, Math.floor(l.getY()), Math.round(l.getZ() * 10) / 10.0});
            changed = true;
        }
        if (changed) save();
    }

    public Map<String, List<double[]>> paths() {
        return paths;
    }

    public Map<String, double[]> firstJoins() {
        return firstJoin;
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        paths.forEach((k, v) -> y.set("paths." + k, v.stream().map(a -> List.of(a[0], a[1], a[2])).toList()));
        firstJoin.forEach((k, v) -> y.set("first_join." + k, List.of(v[0], v[1], v[2])));
        try {
            y.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
