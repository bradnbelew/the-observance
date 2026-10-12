package com.copperline.mrrw;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Act 5+: each in-game dusk, the night of 18 May 2011 replays in place (figures walk and "say"
 *         their logged lines). Once the incident has played, a seventh figure walks the route one
 *         of the PLAYERS walked in Act 2, wearing that player's own skin.
 * Act 8+: lowpath walks its stitched route all night, every night, and never gets home, until
 *         someone puts a bed where its route ends. Then: "session lowpath: end (slept)".
 */
public final class Replays {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final Ghosts ghosts;
    private final PathRecorder recorder;
    private long lastDusk = -1;
    private Ghosts.Run lowpath;

    public Replays(Plugin plugin, Canon canon, State state, Journal journal, Ghosts ghosts, PathRecorder recorder) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.ghosts = ghosts;
        this.recorder = recorder;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    private boolean silent() {
        return state.done("stopped");
    }

    private void tick() {
        if (silent()) return;
        World w = Bukkit.getWorlds().get(0);
        long time = w.getTime();
        long day = w.getFullTime() / 24000L;
        boolean night = time >= 13000 && time < 23000;
        if (state.act() >= 5 && time >= 12500 && time < 13500 && day != lastDusk) {
            lastDusk = day;
            incident(w);
        }
        if (state.act() >= 8 && night && !state.done("lowpath.session_ended") && (lowpath == null || !lowpath.active())) {
            startLowpath(w);
        }
        if (!night && lowpath != null && lowpath.active()) {
            lowpath.cancel();
            lowpath = null;
        }
    }

    /** The incident night, compressed (1 canon second = 4 ticks). */
    public void incident(World w) {
        for (JsonElement e : canon.array("incident_replay")) {
            JsonObject f = e.getAsJsonObject();
            String who = f.get("who").getAsString();
            ghosts.play(w, who, ghosts.profileFor(who), Ghosts.path(f.getAsJsonArray("path")), Ghosts.lines(f.getAsJsonArray("lines")), 4, false, null);
        }
        journal.write("morrow.replay.incident", new JsonObject());
        Bukkit.getScheduler().runTaskLater(plugin, () -> playerGhost(w), 20L * 60);
    }

    /** The Act 5 hook: one player's own Act 2 walk, replayed in their skin. */
    private void playerGhost(World w) {
        if (silent()) return;
        for (Map.Entry<String, List<double[]>> e : recorder.paths().entrySet()) {
            if (e.getValue().size() < 5) continue;
            List<Ghosts.Point> pts = new ArrayList<>();
            int t = 0;
            for (double[] p : e.getValue()) pts.add(new Ghosts.Point(t++ * 5, p[0], p[1], p[2]));
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            var profile = op.getPlayerProfile();
            ghosts.play(w, e.getKey(), io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile),
                    pts, List.of(), 2, false, null);
            JsonObject d = new JsonObject();
            d.addProperty("player", e.getKey());
            journal.write("morrow.replay.player", d);
            return;  // one at a time
        }
    }

    private void startLowpath(World w) {
        List<Ghosts.Point> route = new ArrayList<>();
        for (JsonElement leg : canon.array("lowpath_route")) route.addAll(Ghosts.path(leg.getAsJsonObject().getAsJsonArray("path")));
        lowpath = ghosts.play(w, "lowpath", ghosts.profileFor("lowpath"), route, List.of(), 6, false, () -> arrived(w));
    }

    /** End of route: if a bed stands at lowpath's place, the session ends. Otherwise it waits, then starts over. */
    private void arrived(World w) {
        int[] a = canon.anchor("lowpath_place");
        if (state.act() >= 9 && bedNear(w.getBlockAt(a[0], a[1], a[2]), 3)) {  // a home only counts once players are residents
            state.markDone("lowpath.session_ended");
            Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text("session lowpath: end (slept)", NamedTextColor.GRAY)));
            journal.write("morrow.lowpath.session_ended", new JsonObject());
        }
        lowpath = null;
    }

    static boolean bedNear(Block center, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -r; dz <= r; dz++) {
            Material m = center.getRelative(dx, dy, dz).getType();
            if (m.name().endsWith("_BED")) return true;
        }
        return false;
    }

    public Location lowpathLocation() {
        return lowpath != null && lowpath.active() ? lowpath.location() : null;
    }
}
