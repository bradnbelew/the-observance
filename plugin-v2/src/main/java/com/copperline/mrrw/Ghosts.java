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
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Replayed figures (Iona's 2011 debug view, which Morrow now uses as memory).
 * A figure is a Mannequin wearing the resident's skin (skins come from config.yml: signed
 * textures, see morrow/v2/HUMAN-TODO.md), never a fake player: it is not in the tab list and
 * cannot place or break blocks. Logged chat floats above it as a TextDisplay.
 */
public final class Ghosts {
    public record Point(double t, double x, double y, double z) { }
    public record Line(double t, String text) { }

    private final Plugin plugin;
    private final List<Entity> live = new ArrayList<>();
    private final List<Run> runs = new ArrayList<>();

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
        private final long speed;
        private final boolean loop;
        private final Runnable onEnd;
        private double clock;
        private int idx, lineIdx;
        private Mannequin body;
        private BukkitTask task;
        private boolean active = true;

        Run(World world, String who, ResolvableProfile profile, List<Point> path, List<Line> lines, long speed, boolean loop, Runnable onEnd) {
            this.world = world; this.who = who; this.profile = profile; this.path = path; this.lines = lines;
            this.speed = speed; this.loop = loop; this.onEnd = onEnd;
        }

        public boolean active() {
            return active;
        }

        public Location location() {
            Point p = path.get(idx);
            return new Location(world, p.x(), p.y(), p.z());
        }

        private void tick() {
            clock += 1.0 / speed;
            while (idx < path.size() - 1 && path.get(idx + 1).t() <= clock) idx++;
            Point p = path.get(idx);
            Location to = new Location(world, p.x(), p.y(), p.z());
            if (idx < path.size() - 1) {
                Point n = path.get(idx + 1);
                double dx = n.x() - p.x(), dz = n.z() - p.z();
                if (dx != 0 || dz != 0) to.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            }
            if (world.isChunkLoaded(to.getBlockX() >> 4, to.getBlockZ() >> 4)) {
                if (body == null || !body.isValid()) body = spawnBody(to);
                else body.teleport(to);
            } else if (body != null) {
                remove(body);
                body = null;
            }
            while (lineIdx < lines.size() && lines.get(lineIdx).t() <= clock) {
                String text = lines.get(lineIdx++).text();
                if (body != null) say(body, who, text);
            }
            if (clock >= path.get(path.size() - 1).t()) {
                if (loop) {
                    clock = 0;
                    idx = 0;
                    lineIdx = 0;
                } else {
                    cancel();
                    if (onEnd != null) onEnd.run();
                }
            }
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
            if (body != null) remove(body);
            body = null;
        }
    }

    public Run play(World world, String who, ResolvableProfile profile, List<Point> path, List<Line> lines,
                    long speed, boolean loop, Runnable onEnd) {
        if (path.isEmpty()) return null;
        Run run = new Run(world, who, profile, path, lines, speed, loop, onEnd);
        run.task = Bukkit.getScheduler().runTaskTimer(plugin, run::tick, 1L, 1L);
        runs.add(run);
        return run;
    }

    private void say(Entity anchor, String who, String text) {
        TextDisplay d = anchor.getWorld().spawn(anchor.getLocation().add(0, 2.35, 0), TextDisplay.class, t -> {
            t.text(Component.text("<" + who + "> ", NamedTextColor.GRAY).append(Component.text(text, NamedTextColor.WHITE)));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setSeeThrough(false);
        });
        anchor.addPassenger(d);
        live.add(d);
        Bukkit.getScheduler().runTaskLater(plugin, () -> remove(d), 20L * 6);
    }

    public void remove(Entity e) {
        e.getPassengers().forEach(Entity::remove);
        e.remove();
        live.remove(e);
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
