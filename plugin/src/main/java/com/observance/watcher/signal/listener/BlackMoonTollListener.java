package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Brann — {@code brann-black-moon-toll} (design/PUZZLE-DESIGNS.md §6.1). "The toll that only rings in
 * the dark." Unlike the other producers, this puzzle's answer is a TYPED phrase ({@code awake}) — but
 * the seed row is TEMPORALLY GATED: it only becomes answerable once the toll has been HEARD on the
 * in-game black moon. This producer opens that temporal window. It does NOT post a solve (the phrase
 * is typed at a keeper-stone and resolved by {@link com.observance.watcher.signal.listener.AnswerSignListener}
 * against the OPEN row). Its whole job is:
 *
 * <ol>
 *   <li>detect a keeper standing at the {@code brann_toll_tower} site while the world is on a black
 *       moon (a taboo moon phase, default phase 0 — the same "black moon" the DarkHoursListener uses);</li>
 *   <li>play the toll to that keeper (a private bell cue, so they HEAR it — the morse word is
 *       foreshadowed in Brann's plaintext journal, §6.1);</li>
 *   <li>set the temporal gate flag {@code brann_toll_heard} in {@code arc_state.flags} (idempotent
 *       merge, exactly the {@link IgnitionListener} idiom), which unlocks the seed row so {@code awake}
 *       can be typed at a keeper-stone.</li>
 * </ol>
 *
 * <p>Off the black moon the tower is silent and the flag is never set (the record's "come when it is
 * dark" nudge, §6.1, means a daylight group is only delayed, not stuck). Rate-limited per keeper so a
 * stationary player at the tower fires the toll once per window, not every move tick. Reveal-safe
 * (never cancels, never mutates the world), fault-isolated (Safety). Sites live via a {@link Supplier}.
 */
public final class BlackMoonTollListener implements Listener {

    /** The temporal gate flag the seed row requires. Must match the DB seed exactly. */
    public static final String FLAG_KEY = "brann_toll_heard";

    private static final String TOWER_TYPE = "brann_toll_tower";
    /** One toll + flag-set per keeper per this window while they linger at the tower. */
    private static final long TOLL_COOLDOWN_MS = 60_000L;

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    /** The moon phases that count as the "black moon" (0..7; default {0}). */
    private final Set<Integer> blackMoonPhases;

    public BlackMoonTollListener(SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                                 RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                 java.util.List<Integer> blackMoonPhases) {
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.blackMoonPhases = parsePhases(blackMoonPhases);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        Player p = event.getPlayer();
        if (p == null || supabase == null) return;

        // Hot-path: bail before Safety unless it's the black moon (the tower is silent otherwise).
        World world = to.getWorld();
        if (!isBlackMoon(world)) return;

        safety.run("brann.toll.move", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            String worldName = world.getName();

            Site tower = nearestPlacedOfType(sites, TOWER_TYPE, worldName, to.getX(), to.getY(), to.getZ());
            if (tower == null) return;                        // not at Brann's watch-tower

            // One toll per keeper per window.
            if (!rateLimiter.tryCooldown("brann_toll:" + p.getUniqueId(), TOLL_COOLDOWN_MS)) return;

            // Play the toll privately to the keeper (they HEAR the morse-carrying bell). Best-effort.
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.6f);
            } catch (Throwable ignored) { /* sound is a nicety; never fail the flag on it */ }

            final String name = p.getName();
            scheduler.runAsyncSafe("brann.toll.flag", () -> {
                JsonObject flags = new JsonObject();
                flags.addProperty(FLAG_KEY, true);
                supabase.mergeArcFlags(flags);
                safety.info("brann.toll",
                        FLAG_KEY + " set — " + name + " heard the toll on the black moon at " + tower.id());
            });
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    /** Vanilla moon phase 0..7 (0 = full/"black" moon) from the world's full day count. */
    private boolean isBlackMoon(World world) {
        try {
            long days = world.getFullTime() / 24000L;
            int phase = (int) (days % 8L);
            if (phase < 0) phase += 8;
            return blackMoonPhases.contains(phase);
        } catch (Throwable t) {
            return false;
        }
    }

    static Set<Integer> parsePhases(java.util.List<Integer> phases) {
        Set<Integer> out = new HashSet<>();
        if (phases != null) {
            for (Integer ph : phases) {
                if (ph == null) continue;
                int v = ph % 8;
                if (v < 0) v += 8;
                out.add(v);
            }
        }
        if (out.isEmpty()) out.add(0);                        // default: the full/"black" moon
        return out;
    }

    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) { if (best == null) best = s; continue; }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }
}
