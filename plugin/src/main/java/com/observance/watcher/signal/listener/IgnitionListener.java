package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.Supplier;

/**
 * IgnitionListener — the in-world trigger for {@code prologue_ignited}.
 *
 * <p>Right now the arc can only be ignited via {@code /observance flag set prologue_ignited true}.
 * This listener adds two real in-world triggers so players can fire it themselves:
 *
 * <ol>
 *   <li><b>Lectern read</b> — a player RIGHT-clicks (opens) the lectern at any
 *       {@code report_lectern} site (specifically {@code first_report_lectern_01}, but ALL placed
 *       {@code report_lectern} sites qualify so future lecterns work without code changes).</li>
 *   <li><b>Rosetta touch</b> — a player RIGHT-clicks the carved stone at any site whose id is
 *       {@code rune_rosetta} or {@code stone_of_reckoning} (the two literacy gates). These are type
 *       {@code structure} in sites.yml; the trigger is proximity-based (within the site's radius)
 *       rather than block-face-exact, so the player just has to interact with ANY block while
 *       standing near the stone — a natural "examine the stone" gesture.</li>
 * </ol>
 *
 * <p>The flag is set <b>idempotently</b>: once {@code prologue_ignited} is true in
 * {@code arc_state.flags}, a second trigger costs only one extra no-op DB call (the RPC is an
 * {@code INSERT … ON CONFLICT DO UPDATE USING jsonb_merge_patch}). An in-memory guard ensures at
 * most one async call is in-flight at a time per player, and a short rate-limit prevents spam.
 *
 * <p>Pure — never cancels the event, never mutates the world, never messages the player. Body in Safety.
 * Sites resolved live via a {@link Supplier} so a reload or {@code /observance placeregion} is
 * picked up without re-registering.
 */
public final class IgnitionListener implements Listener {

    /** The flag written when a trigger fires. Must match the DB seed exactly. */
    public static final String FLAG_KEY = "prologue_ignited";

    /** Site types / ids that act as triggers. */
    private static final String TYPE_REPORT_LECTERN = "report_lectern";
    private static final String SITE_RUNE_ROSETTA   = "rune_rosetta";
    private static final String SITE_RECKONING      = "stone_of_reckoning";

    /**
     * Per-player cooldown: once a player fires the trigger, suppress further firing for this long.
     * 60 s is more than enough for the async mergeArcFlags round-trip to complete; also prevents
     * hammering if the player keeps clicking.
     */
    private static final long TRIGGER_COOLDOWN_MS = 60_000L;

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    public IgnitionListener(SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                            RateLimiter rateLimiter, Scheduler scheduler, Safety safety) {
        this.supabase      = supabase;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter   = rateLimiter;
        this.scheduler     = scheduler;
        this.safety        = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        safety.run("signal.Ignition.interact", () -> {
            // Only RIGHT-CLICK actions (main-hand or off-hand).
            Action action = event.getAction();
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            Player player = event.getPlayer();
            if (player == null) return;

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = player.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();
            double px = loc.getX(), py = loc.getY(), pz = loc.getZ();

            // --- Trigger 1: lectern read ---
            // Check whether the clicked block is a lectern AND it is inside a placed report_lectern site.
            if (action == Action.RIGHT_CLICK_BLOCK) {
                Block clicked = event.getClickedBlock();
                if (clicked != null && clicked.getState() instanceof Lectern) {
                    Location blk = clicked.getLocation();
                    String bWorld = blk.getWorld() == null ? world : blk.getWorld().getName();
                    for (Site s : sites.placedOfType(TYPE_REPORT_LECTERN)) {
                        if (s.contains(bWorld, blk.getX(), blk.getY(), blk.getZ())) {
                            fireIgnition(player, "lectern:" + s.id());
                            return;
                        }
                    }
                }
            }

            // --- Trigger 2: rosetta / reckoning stone SNEAK-touch ---
            // A right-click near the rosetta/reckoning stone counts as "examining the stone" ONLY when
            // the player is SNEAKING — a deliberate gesture. This keeps the proximity radius (we check
            // by site ID, not block type, since the pillar is common deepslate/blackstone) but ANDs it
            // with sneak so incidental interaction near the stone (placing a torch, opening a chest)
            // can no longer ignite the arc before the group has read anything.
            if (!player.isSneaking()) return;
            for (String siteId : new String[]{SITE_RUNE_ROSETTA, SITE_RECKONING}) {
                Site s = sites.get(siteId);
                if (s != null && s.isPlaced() && s.contains(world, px, py, pz)) {
                    fireIgnition(player, "rosetta:" + siteId);
                    return;
                }
            }
        });
    }

    /* ----------------------------- helpers ---------------------------------- */

    /**
     * Rate-limit + enqueue the async flag write. Idempotent: if {@code prologue_ignited} is already
     * true in the DB, the RPC is a no-op. The in-memory rate-limit ensures we only dispatch once per
     * player per {@link #TRIGGER_COOLDOWN_MS} window regardless of how many times they click.
     */
    private void fireIgnition(Player player, String triggerLabel) {
        if (supabase == null) return;

        String cdKey = "ignition:" + player.getUniqueId();
        if (!rateLimiter.tryCooldown(cdKey, TRIGGER_COOLDOWN_MS)) return;  // already in-flight / just fired

        final String name = player.getName();
        final String uuid = player.getUniqueId().toString();

        scheduler.runAsyncSafe("oracle.ignition.set", () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(FLAG_KEY, true);
            supabase.mergeArcFlags(flags);
            safety.info("oracle.ignition",
                    "prologue_ignited set by " + name + " via " + triggerLabel);
        });
    }
}
