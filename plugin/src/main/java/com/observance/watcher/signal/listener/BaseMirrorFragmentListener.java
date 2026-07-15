package com.observance.watcher.signal.listener;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Base Mirror's asymmetric synthesis dressing (BI08, C05). Three fixed observation points near
 * {@code unlit_house_base} each carry ONE physical finding, but the finding is shown ONLY to whichever
 * player is currently standing closest to that specific point — via {@link PerPlayer#showEntityTo},
 * the same per-player visibility primitive proven by {@code NameOnWallBeat}. Nobody sees all three
 * from one spot; the group has to split up, read what only they can see, and tell each other before
 * anyone can compose the Discord conclusion. Solo-accessible by walking between all three points, so
 * the "finishable with one active player" rule still holds — this rewards a group, it never requires one.
 *
 * <p>Pure world dressing: no predicate component, no PDC gating anything, no flag write. BI08 itself
 * stays exactly the Discord {@code discord_answer} node it already is; this only feeds what a player
 * can tell their group before they type the conclusion.
 */
public final class BaseMirrorFragmentListener {

    private static final String BASE_SITE = "unlit_house_base";
    private static final double CLAIM_RADIUS = 3.0;
    private static final long TICK_PERIOD = 30L;

    /** Three raw physical observations at the base mirror; none states the multi-cause conclusion. */
    private static final String[] FRAGMENTS = {
            "the copied lamp record and the copied watch record disagree by an hour that was never explained.",
            "the copied well map shows a load redirected toward the third bay weeks before any cut is dated.",
            "the copied inner seal was thrown from the inside, and the copied outer seal was never touched at all."
    };

    private final ObservancePlugin plugin;
    private final Supplier<SitesConfig> sites;
    private final Scheduler scheduler;
    private final Safety safety;

    private TextDisplay[] displays;
    private UUID[] shownTo;
    private BukkitTask tickTask;

    public BaseMirrorFragmentListener(ObservancePlugin plugin, Supplier<SitesConfig> sites,
                                      Scheduler scheduler, Safety safety) {
        this.plugin = plugin;
        this.sites = sites;
        this.scheduler = scheduler;
        this.safety = safety;
        this.displays = new TextDisplay[FRAGMENTS.length];
        this.shownTo = new UUID[FRAGMENTS.length];
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = scheduler.runTimerSafe("unlit.base.fragments", TICK_PERIOD, TICK_PERIOD, this::tick);
    }

    public void stop() {
        Scheduler.cancel(tickTask);
        tickTask = null;
        for (int i = 0; i < displays.length; i++) {
            removeDisplay(i);
        }
    }

    private void tick() {
        safety.run("unlit.base.fragments.tick", () -> {
            Location anchor = baseAnchor();
            if (anchor == null || anchor.getWorld() == null) {
                for (int i = 0; i < displays.length; i++) removeDisplay(i);
                return;
            }
            World world = anchor.getWorld();
            List<? extends Player> present = world.getPlayers();

            for (int i = 0; i < FRAGMENTS.length; i++) {
                Location point = fragmentPoint(anchor, i);
                Player nearest = nearestWithin(present, point, CLAIM_RADIUS);
                ensureDisplay(i, point);
                reveal(i, nearest);
            }
        });
    }

    private void ensureDisplay(int index, Location point) {
        if (displays[index] != null && displays[index].isValid()) return;
        World world = point.getWorld();
        if (world == null) return;
        try {
            TextDisplay display = world.spawn(point, TextDisplay.class, td -> {
                td.setVisibleByDefault(false);
                td.setPersistent(false);
                td.text(Component.text(FRAGMENTS[index]).color(NamedTextColor.GRAY));
                td.setBillboard(Display.Billboard.CENTER);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setDefaultBackground(false);
                try {
                    td.setBackgroundColor(org.bukkit.Color.fromARGB(0));
                } catch (Throwable ignored) { }
                td.setBrightness(new Display.Brightness(15, 15));
                td.setViewRange(0.4f);
                td.setLineWidth(160);
                td.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
            });
            displays[index] = display;
            shownTo[index] = null;
        } catch (Throwable ignored) {
            // Dressing-only; a failed spawn just means that point stays quiet this tick.
        }
    }

    private void reveal(int index, Player nearest) {
        TextDisplay display = displays[index];
        if (display == null || !display.isValid()) return;
        UUID nearestId = nearest == null ? null : nearest.getUniqueId();
        UUID previous = shownTo[index];
        if (java.util.Objects.equals(nearestId, previous)) return;

        if (previous != null) {
            Player oldPlayer = org.bukkit.Bukkit.getPlayer(previous);
            if (oldPlayer != null) PerPlayer.hideEntityFrom(plugin, oldPlayer, display);
        }
        if (nearest != null) {
            PerPlayer.showEntityTo(plugin, nearest, display);
        }
        shownTo[index] = nearestId;
    }

    private void removeDisplay(int index) {
        TextDisplay display = displays[index];
        if (display != null && display.isValid()) {
            try {
                display.remove();
            } catch (Throwable ignored) { }
        }
        displays[index] = null;
        shownTo[index] = null;
    }

    private static Player nearestWithin(List<? extends Player> present, Location point, double radius) {
        Player best = null;
        double bestDist = radius * radius;
        for (Player p : present) {
            if (p == null || !p.isOnline()) continue;
            Location loc = p.getLocation();
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(point.getWorld())) continue;
            double d = loc.distanceSquared(point);
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /** Three small, fixed offsets around the base anchor — safely clear of the managed dressing there. */
    private static Location fragmentPoint(Location anchor, int index) {
        double[][] offsets = {{-2.0, 0.0, 2.0}, {2.0, 0.0, 2.0}, {0.0, 0.0, -2.5}};
        double[] off = offsets[Math.floorMod(index, offsets.length)];
        return anchor.clone().add(off[0], off[1] + 1.4, off[2]);
    }

    private Location baseAnchor() {
        SitesConfig cfg = sites == null ? null : sites.get();
        if (cfg == null) return null;
        Site site = cfg.get(BASE_SITE);
        return site == null ? null : site.location();
    }
}
