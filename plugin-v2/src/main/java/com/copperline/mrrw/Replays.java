package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Act 5+: each in-game dusk, the night of 18 May 2011 replays in place (figures walk at a player's
 *         pace and "say" their logged lines at real intervals, about eight minutes). It starts when
 *         a resident is near one of the figures' starting points, so the first thing seen is a figure
 *         beginning, not one mid-walk; if nobody comes near for three minutes it starts anyway, and if
 *         nobody is online it waits. The engine prints a cue line first (Iona's debug view template).
 *         Once the incident has played, a seventh figure walks the route one of the PLAYERS walked in
 *         Act 2, wearing that player's own skin. The journal records who witnessed each replay.
 * Act 8+: lowpath walks its stitched route every night (about five minutes, with the residents'
 *         stops) and never gets home, until someone puts a bed where its route ends. A bed placed in
 *         Act 8 is queued and removed at dawn (RecoveryService); from Act 9 it is kept, and that night
 *         lowpath walks only its last leg in, lies down, and "session lowpath: end (slept)".
 */
public final class Replays implements Listener {
    private static final double NEAR = 64;
    private static final long WAIT_FOR_WITNESS_MS = 180_000;
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final Ghosts ghosts;
    private final PathRecorder recorder;
    private long lastDusk = -1;
    private long pendingSince;
    private boolean pendingIncident;
    private final List<Ghosts.Run> incident = new ArrayList<>();
    private Ghosts.Run lowpath;
    private boolean lowpathPending;
    private long lowpathPendingSince;

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

    private String engine(String key) {
        return canon.object("engine_lines").get(key).getAsString();
    }

    private void announce(String text) {
        Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY)));
        journal.recovery(text);
    }

    private static boolean anyoneNear(World w, List<Location> points) {
        for (Player p : w.getPlayers()) {
            for (Location l : points) if (p.getWorld() == l.getWorld() && p.getLocation().distanceSquared(l) <= NEAR * NEAR) return true;
        }
        return false;
    }

    private void tick() {
        if (silent()) return;
        World w = Bukkit.getWorlds().get(0);
        long time = w.getTime();
        long day = w.getFullTime() / 24000L;
        boolean night = time >= 13000 && time < 23000;
        if (state.act() >= 5 && time >= 12500 && time < 13500 && day != lastDusk) {
            lastDusk = day;
            pendingIncident = true;
            pendingSince = System.currentTimeMillis();
        }
        if (pendingIncident) {
            if (!night && time >= 23000) {
                pendingIncident = false;
                JsonObject d = new JsonObject();
                d.addProperty("waited_all_night", true);
                journal.write("morrow.replay.incident", d);
            } else if (!w.getPlayers().isEmpty() && (anyoneNear(w, incidentStarts(w)) || System.currentTimeMillis() - pendingSince > WAIT_FOR_WITNESS_MS)) {
                pendingIncident = false;
                incident(w);
            }
        }
        boolean bedKept = state.done("lowpath.bed_kept");
        if (state.act() >= 8 && night && !state.done("lowpath.session_ended") && (lowpath == null || !lowpath.active()) && !lowpathPending) {
            lowpathPending = true;
            lowpathPendingSince = System.currentTimeMillis();
        }
        if (lowpathPending && night && (lowpath == null || !lowpath.active())) {
            List<Location> start = List.of(bedKept ? routeStart(canon.array("lowpath_last_leg")) : routeStart(canon.array("lowpath_route").get(0).getAsJsonObject().getAsJsonArray("path")));
            if (!w.getPlayers().isEmpty() && (anyoneNear(w, start) || System.currentTimeMillis() - lowpathPendingSince > 60_000)) {
                lowpathPending = false;
                startLowpath(w, bedKept);
            }
        }
        if (!night) {
            lowpathPending = false;
            if (lowpath != null && lowpath.active() && !bedKept) {
                lowpath.cancel();
                lowpath = null;
            }
        }
    }

    private Location routeStart(JsonArray path) {
        JsonArray p = path.get(0).getAsJsonArray();
        return new Location(Bukkit.getWorlds().get(0), p.get(1).getAsDouble(), p.get(2).getAsDouble(), p.get(3).getAsDouble());
    }

    private List<Location> incidentStarts(World w) {
        List<Location> out = new ArrayList<>();
        for (JsonElement e : canon.array("incident_replay")) {
            JsonArray p = e.getAsJsonObject().getAsJsonArray("path").get(0).getAsJsonArray();
            out.add(new Location(w, p.get(1).getAsDouble(), p.get(2).getAsDouble(), p.get(3).getAsDouble()));
        }
        return out;
    }

    /** The incident night, in real time, after the cue. Also /mrrw replay. */
    public void incident(World w) {
        announce(engine("replay_cue"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (silent()) return;
            incident.clear();
            for (JsonElement e : canon.array("incident_replay")) {
                JsonObject f = e.getAsJsonObject();
                String who = f.get("who").getAsString();
                double from = f.has("from") ? f.get("from").getAsDouble() : 0;
                double until = f.has("until") ? f.get("until").getAsDouble() : 0;
                Ghosts.Run r = ghosts.play(w, who, ghosts.profileFor(who), Ghosts.path(f.getAsJsonArray("path")), Ghosts.lines(f.getAsJsonArray("lines")),
                        from, until, false, this::incidentFigureDone);
                if (r != null) incident.add(r);
            }
            JsonObject d = new JsonObject();
            d.addProperty("started", true);
            d.addProperty("figures", incident.size());
            journal.write("morrow.replay.incident", d);
            Bukkit.getScheduler().runTaskLater(plugin, () -> playerGhost(w), 20L * 60);
        }, 60L);
    }

    private void incidentFigureDone() {
        if (incident.stream().anyMatch(Ghosts.Run::active)) return;
        Set<String> names = new LinkedHashSet<>();
        for (Ghosts.Run r : incident) names.addAll(r.witnessed());
        JsonObject d = new JsonObject();
        d.addProperty("ended", true);
        JsonArray w = new JsonArray();
        names.forEach(w::add);
        d.add("witnessed", w);
        d.addProperty("witnessed_by_nobody", names.isEmpty());  // it replays again next dusk
        journal.write("morrow.replay.incident", d);
        incident.clear();
    }

    /** The Act 5 hook: one player's own Act 2 walk, replayed in their skin at the pace it was recorded (one sample per 5 s). */
    private void playerGhost(World w) {
        if (silent()) return;
        for (Map.Entry<String, List<double[]>> e : recorder.paths().entrySet()) {
            if (e.getValue().size() < 5) continue;
            List<Ghosts.Point> pts = new ArrayList<>();
            int t = 0;
            for (double[] p : e.getValue()) pts.add(new Ghosts.Point(t++ * 5, p[0], p[1], p[2]));
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            var profile = op.getPlayerProfile();
            Ghosts.Run r = ghosts.play(w, e.getKey(), io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile),
                    pts, List.of(), 0, 0, false, null);
            JsonObject d = new JsonObject();
            d.addProperty("player", e.getKey());
            d.addProperty("seconds", pts.get(pts.size() - 1).t());
            journal.write("morrow.replay.player", d);
            if (r != null) return;  // one at a time
        }
    }

    private void startLowpath(World w, boolean lastLegOnly) {
        List<Ghosts.Point> route = new ArrayList<>();
        if (lastLegOnly) route.addAll(Ghosts.path(canon.array("lowpath_last_leg")));
        else for (JsonElement leg : canon.array("lowpath_route")) route.addAll(Ghosts.path(leg.getAsJsonObject().getAsJsonArray("path")));
        double t0 = route.get(0).t();
        List<Ghosts.Point> shifted = new ArrayList<>();
        for (Ghosts.Point p : route) shifted.add(new Ghosts.Point(p.t() - t0, p.x(), p.y(), p.z()));
        announce(engine("lowpath_cue"));
        lowpath = ghosts.play(w, "lowpath", ghosts.profileFor("lowpath"), shifted, List.of(), 3, 0, false, () -> arrived(w));
        JsonObject d = new JsonObject();
        d.addProperty("last_leg_only", lastLegOnly);
        journal.write("morrow.replay.lowpath", d);
    }

    /** End of route: with a kept bed at lowpath's place the session ends; otherwise it stands there until dawn. */
    private void arrived(World w) {
        int[] a = canon.anchor("lowpath_place");
        Ghosts.Run run = lowpath;
        if (state.act() >= 9 && run != null && bedNear(w.getBlockAt(a[0], a[1], a[2]), 3)) {  // a home only counts once players are residents
            Ghosts.Run stay = ghosts.play(w, "lowpath", ghosts.profileFor("lowpath"),
                    List.of(new Ghosts.Point(0, a[0] - 1.0, a[1], a[2] + 0.5), new Ghosts.Point(600, a[0] - 1.0, a[1], a[2] + 0.5)), List.of(), 0, 0, false, null);
            Runnable slept = () -> {
                state.markDone("lowpath.session_ended");
                announce(engine("lowpath_slept"));
                JsonObject d = new JsonObject();
                JsonArray wit = new JsonArray();
                run.witnessed().forEach(wit::add);
                d.add("witnessed", wit);
                journal.write("morrow.lowpath.session_ended", d);
                lowpath = null;
            };
            if (stay != null) Bukkit.getScheduler().runTaskLater(plugin, () -> stay.sleep(3, slept), 20L);
            else slept.run();
            return;
        }
        // it stands at its place until dawn (the run ended at the last waypoint; keep a body there)
        lowpath = ghosts.play(w, "lowpath", ghosts.profileFor("lowpath"),
                List.of(new Ghosts.Point(0, a[0] + 0.5, a[1], a[2] + 0.5), new Ghosts.Point(600, a[0] + 0.5, a[1], a[2] + 0.5)), List.of(), 0, 0, false, null);
    }

    static boolean bedNear(Block center, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -r; dz <= r; dz++) {
            Material m = center.getRelative(dx, dy, dz).getType();
            if (m.name().endsWith("_BED")) return true;
        }
        return false;
    }

    /** Act 9+: a bed placed at lowpath's place is kept, and the engine says so at once. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (silent() || state.act() < 9 || !e.getBlock().getType().name().endsWith("_BED")) return;
        int[] a = canon.anchor("lowpath_place");
        Block b = e.getBlock();
        if (Math.abs(b.getX() - a[0]) > 3 || Math.abs(b.getZ() - a[2]) > 3 || Math.abs(b.getY() - a[1]) > 2) return;
        if (state.done("lowpath.bed_kept") || state.done("lowpath.session_ended")) return;
        state.markDone("lowpath.bed_kept");
        announce(engine("bed_kept"));
        JsonObject d = new JsonObject();
        d.addProperty("by", e.getPlayer().getName());
        journal.write("morrow.lowpath.bed_kept", d);
        if (lowpath != null && lowpath.active()) {  // walking now: let it finish at its place and lie down there
            return;
        }
        lowpathPending = true;
        lowpathPendingSince = System.currentTimeMillis();
    }

    public Location lowpathLocation() {
        return lowpath != null && lowpath.active() ? lowpath.location() : null;
    }
}
