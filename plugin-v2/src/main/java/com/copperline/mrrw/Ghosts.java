package com.copperline.mrrw;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Replayed figures (Iona's 2011 debug view, which Morrow now uses as memory).
 * A figure is a Mannequin wearing the resident's skin (skins come from config.yml: signed
 * textures, see morrow/v2/HUMAN-TODO.md), never a fake player: it is not in the tab list and
 * cannot place or break blocks. Logged chat floats above it as a TextDisplay that follows it.
 *
 * A run plays in REAL seconds: the canon's waypoints are [t, x, y, z] and the figure is
 * interpolated between them every tick, so it walks at a player's pace and never jumps. A run
 * remembers who stood within WITNESS_RADIUS of it while it ran, so the journal can say whether
 * anyone saw it.
 */
public final class Ghosts {
    public record Point(double t, double x, double y, double z) { }
    public record Line(double t, String text) { }

    static final double WITNESS_RADIUS = 24;
    private static final double BUBBLE_HEIGHT = 2.6;

    private final Plugin plugin;
    private final List<Entity> live = new ArrayList<>();
    private final List<Run> runs = new ArrayList<>();
    private boolean frozen;

    public Ghosts(Plugin plugin) {
        this.plugin = plugin;
    }

    public static List<Point> path(JsonArray arr) {
        List<Point> out = new ArrayList<>();
        for (JsonElement e : arr) {
            JsonArray p = e.getAsJsonArray();
            out.add(new Point(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble(), p.get(3).getAsDouble()));
        }
        return out;
    }

    public static List<Line> lines(JsonArray arr) {
        List<Line> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement e : arr) out.add(new Line(e.getAsJsonArray().get(0).getAsDouble(), e.getAsJsonArray().get(1).getAsString()));
        return out;
    }

    /** A straight walk from a to b at WALK_SPEED, starting at t = 0: two waypoints. */
    public static List<Point> walk(Location a, Location b, double blocksPerSecond) {
        double d = Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
        return List.of(new Point(0, a.getX(), a.getY(), a.getZ()), new Point(Math.max(0.5, d / blocksPerSecond), b.getX(), b.getY(), b.getZ()));
    }

    ResolvableProfile profileFor(String name) {
        ConfigurationSection skins = plugin.getConfig().getConfigurationSection("skins");
        PlayerProfile p = Bukkit.createProfile(UUID.nameUUIDFromBytes(("ghost:" + name).getBytes(StandardCharsets.UTF_8)), name.length() > 16 ? name.substring(0, 16) : name);
        if (skins != null && skins.isConfigurationSection(name)) {
            String value = skins.getString(name + ".value", "");
            String sig = skins.getString(name + ".signature", "");
            if (!value.isEmpty()) p.setProperty(new ProfileProperty("textures", value, sig.isEmpty() ? null : sig));
        }
        return ResolvableProfile.resolvableProfile(p);
    }

    /** A running replay. Its clock always advances; the figure exists only where chunks are loaded. */
    public final class Run {
        private final World world;
        private final String who;
        private final ResolvableProfile profile;
        private final List<Point> path;
        private final List<Line> lines;
        private final double from, until;
        private final boolean loop;
        private final Runnable onEnd;
        private final Set<String> witnessed = new LinkedHashSet<>();
        private double clock;
        private int lineIdx;
        private float yaw;
        private Mannequin body;
        private TextDisplay bubble;
        private double bubbleUntil;
        private BukkitTask task;
        private boolean active = true;
        private boolean sleeping;

        Run(World world, String who, ResolvableProfile profile, List<Point> path, List<Line> lines, double from, double until, boolean loop, Runnable onEnd) {
            this.world = world; this.who = who; this.profile = profile; this.path = path; this.lines = lines;
            this.from = from; this.until = until; this.loop = loop; this.onEnd = onEnd;
        }

        public boolean active() {
            return active;
        }

        public String who() {
            return who;
        }

        /** Players who stood within WITNESS_RADIUS of the figure at any point of the run. */
        public Set<String> witnessed() {
            return witnessed;
        }

        /** Where the figure is (or would be) now. */
        public Location location() {
            double[] p = at(clock);
            return new Location(world, p[0], p[1], p[2], yaw, 0);
        }

        /** Where the figure begins. */
        public Location start() {
            Point p = path.get(0);
            return new Location(world, p.x(), p.y(), p.z());
        }

        private double[] at(double t) {
            if (t <= path.get(0).t()) {
                Point p = path.get(0);
                return new double[] {p.x(), p.y(), p.z()};
            }
            for (int i = 0; i < path.size() - 1; i++) {
                Point a = path.get(i), b = path.get(i + 1);
                if (t >= a.t() && t < b.t()) {
                    double f = b.t() > a.t() ? (t - a.t()) / (b.t() - a.t()) : 1;
                    return new double[] {a.x() + (b.x() - a.x()) * f, a.y() + (b.y() - a.y()) * f, a.z() + (b.z() - a.z()) * f};
                }
            }
            Point p = path.get(path.size() - 1);
            return new double[] {p.x(), p.y(), p.z()};
        }

        private void tick() {
            double before = clock;
            clock += 0.05;
            boolean present = clock >= from && (until <= 0 || clock < until);
            if (!present) {
                if (body != null) {
                    remove(body);
                    body = null;
                }
                if (until > 0 && clock >= until) end();
                return;
            }
            double[] p = at(clock), q = at(before);
            double dx = p[0] - q[0], dz = p[2] - q[2];
            if (dx * dx + dz * dz > 1e-6) yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            Location to = new Location(world, p[0], p[1], p[2], yaw, 0);
            if (world.isChunkLoaded(to.getBlockX() >> 4, to.getBlockZ() >> 4)) {
                if (body == null || !body.isValid()) body = spawnBody(to);
                else body.teleport(to);
                for (Player pl : world.getPlayers()) {
                    if (pl.getLocation().distanceSquared(to) <= WITNESS_RADIUS * WITNESS_RADIUS) witnessed.add(pl.getName());
                }
            } else if (body != null) {
                remove(body);
                body = null;
            }
            if (bubble != null) {
                if (clock >= bubbleUntil || body == null || !bubble.isValid()) {
                    if (bubble.isValid()) remove(bubble);
                    bubble = null;
                } else {
                    bubble.teleport(to.clone().add(0, BUBBLE_HEIGHT, 0));
                }
            }
            while (lineIdx < lines.size() && lines.get(lineIdx).t() <= clock) {
                String text = lines.get(lineIdx++).text();
                if (body != null) say(to, text);
            }
            if (clock >= path.get(path.size() - 1).t() && (until <= 0 || clock >= until)) {
                if (loop) {
                    clock = 0;
                    lineIdx = 0;
                } else {
                    end();
                }
            }
        }

        private void end() {
            cancel();
            if (onEnd != null) onEnd.run();
        }

        private void say(Location at, String text) {
            if (bubble != null && bubble.isValid()) remove(bubble);
            bubble = world.spawn(at.clone().add(0, BUBBLE_HEIGHT, 0), TextDisplay.class, t -> {
                t.text(Component.text("<" + who + "> ", NamedTextColor.GRAY).append(Component.text(text, NamedTextColor.WHITE)));
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.setSeeThrough(false);
                t.setShadowed(true);
                t.setLineWidth(200);
                t.setTeleportDuration(1);
            });
            live.add(bubble);
            bubbleUntil = clock + 6;
        }

        /** The figure lies down where it is (the bed scene); gone after `seconds`. */
        public void sleep(double seconds, Runnable then) {
            sleeping = true;
            if (task != null) task.cancel();
            task = null;
            if (body != null && body.isValid()) {
                try {
                    body.setPose(Pose.SLEEPING, true);
                } catch (Throwable notSupported) {
                    // older API: it stops on the bed instead
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cancel();
                if (then != null) then.run();
            }, (long) (seconds * 20));
        }

        private Mannequin spawnBody(Location at) {
            Mannequin m = world.spawn(at, Mannequin.class, e -> {
                e.setProfile(profile);
                e.setImmovable(true);
                e.setPersistent(false);
                e.setInvulnerable(true);
                e.setSilent(true);
                e.setDescription(Component.empty());
            });
            live.add(m);
            return m;
        }

        public void cancel() {
            active = false;
            if (task != null) task.cancel();
            task = null;
            if (bubble != null) remove(bubble);
            bubble = null;
            if (body != null) remove(body);
            body = null;
            runs.remove(this);
        }
    }

    /**
     * Start a run. from/until: seconds before which the figure is absent / after which it leaves
     * (0 = never). Path times are real seconds from the run's start.
     */
    public Run play(World world, String who, ResolvableProfile profile, List<Point> path, List<Line> lines,
                    double from, double until, boolean loop, Runnable onEnd) {
        if (path.isEmpty() || frozen) return null;
        Run run = new Run(world, who, profile, path, lines, from, until, loop, onEnd);
        run.task = Bukkit.getScheduler().runTaskTimer(plugin, run::tick, 1L, 1L);
        runs.add(run);
        return run;
    }

    public List<Run> running() {
        return List.copyOf(runs);
    }

    public void remove(Entity e) {
        e.getPassengers().forEach(Entity::remove);
        e.remove();
        live.remove(e);
    }

    /**
     * The stop's lights-out: every figure stops mid-step where it stands (its clock stops, its body
     * stays), and nothing new starts walking. Returns how many figures were standing.
     * {@link #clearAll()} removes them.
     */
    public int freezeAll() {
        frozen = true;
        int standing = 0;
        for (Run r : runs) {
            if (r.task != null) r.task.cancel();
            if (r.body != null && r.body.isValid()) standing++;
        }
        return standing;
    }

    /** The finale's going-home walks start after the freeze; let them. */
    public void thaw() {
        frozen = false;
    }

    /** After the stop, and on disable: nothing Morrow made stays walking. */
    public void clearAll() {
        new ArrayList<>(runs).forEach(Run::cancel);
        runs.clear();
        new ArrayList<>(live).forEach(e -> {
            if (e.isValid()) e.remove();
        });
        live.clear();
    }
}
