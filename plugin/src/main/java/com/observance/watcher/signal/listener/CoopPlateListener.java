package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.Supplier;

/**
 * The three-hands coop gate — the IV→V hinge (seed {@code m4-three-hands}; design/PUZZLE-DESIGNS.md §A6).
 *
 * <p>The Threshold does not open to a word. It opens to THREE acts done in one short window (three acts,
 * not necessarily three people): a FOOT on the plate + a CARVE at the mark + a WORD posted in Discord — the
 * convergence word the catch yields ("the one who turned away"). It is the one CROSS-SURFACE coop gate: the
 * two world legs are witnessed here, the word leg is witnessed in #the-record.
 *
 * <p><b>This listener owns only the two WORLD legs.</b> On a foot (a {@code PHYSICAL} step on the plate) or
 * a carve (a {@code LEFT_CLICK_BLOCK} punch of the mark) inside the {@code coop_plate} site, it records the
 * leg in memory; when BOTH are fresh together (within the window) it writes one timestamped arc flag,
 * {@code coop_world_ready_at}. The Discord side is the SOLE closer: when the convergence word is posted
 * while that flag is fresh, it submits the puzzle's opaque token to the oracle (which opens the Threshold).
 * One closer → no double-fire; the join is self-healing (redo any leg to bring it back in-window).
 *
 * <p>Pure + safe: never cancels the event, never mutates the world, never messages players; body in Safety;
 * the flag write is async (network) and throttled so plate-spam can't hammer the RPC. Unplaced/disabled →
 * a clean no-op (go-live safe). Sites resolved live via a {@link Supplier} so a reload is picked up.
 *
 * <p>Both world-leg trackers are keyed by {@link Site#id()}, so if more than one {@code coop_plate} site
 * is ever placed, a foot at one site and a carve at a different site can never combine into a false
 * "both fresh together" — freshness is judged per-site, not globally.
 */
public final class CoopPlateListener implements Listener {

    private static final String SITE_TYPE = "coop_plate";
    /** The shared marker Discord reads to close the gate on the WORD leg: epoch ms of the last time both
     *  world legs (a foot on the plate + a carve) were fresh together. Kept in sync with the discord twin
     *  (discord/src/showrunner/coop-gate.ts READY_FLAG). */
    private static final String READY_FLAG = "coop_world_ready_at";
    /** At most one ready-marker write per this interval — plate steps fire constantly; don't hammer the RPC. */
    private static final long PUBLISH_THROTTLE_MS = 3_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final boolean enabled;
    /** Both world legs must fall within this window of each other to publish the ready marker. */
    private final long windowMs;

    // The two world legs, in memory, KEYED BY SITE ID so multiple placed coop_plate sites can't
    // cross-contaminate freshness (a foot at site A + a carve at site B must NOT register as "both
    // fresh together"). Today's only real deployment has exactly one placed site, so this is a
    // behavior-preserving generalization of the old single-`long` fields, not a gate-logic rewrite.
    // ConcurrentHashMap: writes happen on whichever thread fires the event; ready to be read/written
    // from concurrent callbacks without external synchronization.
    private final java.util.Map<String, Long> lastFootMsBySite = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Long> lastCarveMsBySite = new java.util.concurrent.ConcurrentHashMap<>();

    public CoopPlateListener(Supplier<SitesConfig> sitesSupplier, SupabaseClient supabase,
                             RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                             boolean enabled, long windowMs) {
        this.sitesSupplier = sitesSupplier;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.windowMs = Math.max(60_000L, windowMs);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        safety.run("coop.plate.interact", () -> {
            if (!enabled || supabase == null || scheduler == null || rateLimiter == null) return;
            Action action = event.getAction();
            boolean foot = action == Action.PHYSICAL;          // a step on the plate
            boolean carve = action == Action.LEFT_CLICK_BLOCK; // a punch of the mark ("cut")
            if (!foot && !carve) return;

            Player p = event.getPlayer();
            if (p == null) return;
            Block b = event.getClickedBlock();
            if (b == null) return;
            Location loc = b.getLocation();
            if (loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            Site site = sites.get("coop_plate");
            if (site == null || !site.isPlaced() || !SITE_TYPE.equals(site.type())) return;
            Location center = site.location();
            if (center == null || center.getWorld() == null || !world.equals(center.getWorld().getName())) return;
            boolean exactFoot = foot && b.getX() == center.getBlockX() && b.getY() == center.getBlockY()
                    && b.getZ() == center.getBlockZ() && b.getType() == org.bukkit.Material.STONE_PRESSURE_PLATE;
            boolean exactCarve = carve && b.getX() == center.getBlockX() + 3 && b.getY() == center.getBlockY()
                    && b.getZ() == center.getBlockZ() && b.getType() == org.bukkit.Material.CHISELED_TUFF;
            if (!exactFoot && !exactCarve) return;

            long now = System.currentTimeMillis();
            String siteId = site.id();
            if (exactFoot) lastFootMsBySite.put(siteId, now); else lastCarveMsBySite.put(siteId, now);

            if (bothFresh(siteId, now)) {
                sendLegFeedback(p, siteId, "the square waits on the word.", 0.55f);
                tryPublish(now);
            } else {
                sendLegFeedback(p, siteId, exactFoot ? "one hand stands." : "one hand marks.", exactFoot ? 0.45f : 0.5f);
            }
        });
    }

    /** True iff both world legs have fired at THIS site and both fall within the window ending now.
     *  Keyed by site id so a foot at one placed coop_plate site and a carve at a different placed
     *  coop_plate site never combine into a false "both fresh together". */
    private boolean bothFresh(String siteId, long now) {
        Long foot = lastFootMsBySite.get(siteId);
        Long carve = lastCarveMsBySite.get(siteId);
        return foot != null && carve != null
                && (now - foot) <= windowMs && (now - carve) <= windowMs;
    }

    /** Publish coop_world_ready_at=now (throttled, async). The Discord word leg closes the gate. */
    private void tryPublish(long now) {
        if (!rateLimiter.tryCooldown("coop:plate:publish", PUBLISH_THROTTLE_MS)) return;
        JsonObject flags = new JsonObject();
        flags.addProperty(READY_FLAG, now);
        safety.info("coop.plate", "world legs held (foot + carve) — the threshold waits on the word");
        scheduler.runAsyncSafe("coop.plate.publish", () -> supabase.mergeArcFlags(flags));
    }

    private void sendLegFeedback(Player p, String siteId, String text, float pitch) {
        if (p == null || text == null || text.isBlank()) return;
        if (!rateLimiter.tryCooldown("coop:plate:feedback:" + siteId + ":" + p.getUniqueId(), 1_500L)) return;
        try {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.14f, pitch);
        } catch (Throwable ignored) {
            // atmospheric only
        }
        try {
            p.sendActionBar(Component.text(text, NamedTextColor.DARK_GRAY));
        } catch (Throwable ignored) {
            // older clients or proxy shims may not support action bars
        }
    }

    /** The placed coop_plate the block sits inside, or null (mirrors AcceptingRiteListener). */
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
