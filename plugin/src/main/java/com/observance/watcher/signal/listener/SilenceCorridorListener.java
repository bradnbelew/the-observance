package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Brann — {@code brann-silence-corridor} (design/PUZZLE-DESIGNS.md §6.2). "The corridor that hears
 * you": a calibrated-sculk watch-walk passable ONLY in silence. This producer detects the physical
 * enactment of Brann's vigil — pass from the {@code brann_corridor_start} site to the
 * {@code brann_corridor_end} site while SNEAKING the whole way, making no vibration (no sprint, no
 * block-break, no attack). Reaching the far door in silence posts the puzzle's OPAQUE token to the
 * shared oracle. No typing — the quiet traversal is the solve.
 *
 * <p>Per-player run state: entering the start site WHILE SNEAKING arms a silent run for that keeper.
 * A "vibration" — starting to sprint, breaking a block, or attacking an entity — VOIDS the run (this
 * is the plugin's stand-in for the sculk hearing you; vanilla shriekers give the in-world tell, D8).
 * Un-sneaking while in the corridor also voids the run (you must stay quiet). Reaching the end site
 * with the run still armed and still sneaking = solve.
 *
 * <p>Detection is a guarded {@link PlayerMoveEvent} for zone entry/exit plus a few cheap vibration
 * events. The move handler early-returns hard before touching Safety when the player is neither in a
 * corridor site nor holding a run. Idempotent (oracle {@code insertSolveIfNew}), fault-isolated,
 * reveal-safe (never cancels, mutates, or messages). Sites resolved live via a {@link Supplier}.
 *
 * <p>Observer tie-in (design §6.2, later phase): a voice-chat vibration could also void the run. That
 * layer is not built; this producer degrades gracefully without it (silence = the physical acts only).
 */
public final class SilenceCorridorListener implements Listener {

    private static final String START_TYPE = "brann_corridor_start";
    private static final String END_TYPE = "brann_corridor_end";
    private static final long SOLVE_COOLDOWN_MS = 2_000L;
    private static final long RUN_TIMEOUT_MS = 90_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;

    /** Keepers with an armed silent run (entered the start sneaking, no vibration since). */
    private final Set<UUID> silentRun = new HashSet<>();
    private final Map<UUID, Long> runStartedAt = new HashMap<>();

    public SilenceCorridorListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                   RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                   boolean enabled, String token, String puzzleKey) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "brann-silence-corridor" : puzzleKey.trim();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled || token.isBlank()) return;
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        Player p = event.getPlayer();
        if (p == null) return;
        UUID id = p.getUniqueId();
        boolean armed = silentRun.contains(id);
        // Hot-path guard: do nothing unless there's a run to advance/void. Cheap membership + a site
        // scan only when it could matter. When not armed we still must catch ENTERING the start.
        safety.run("brann.corridor.move", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            String world = to.getWorld().getName();

            if (!armed) {
                // Arm a silent run when the keeper enters the start site WHILE sneaking.
                Site start = nearestPlacedOfType(sites, START_TYPE, world, to.getX(), to.getY(), to.getZ());
                if (start != null && p.isSneaking()) {
                    silentRun.add(id);
                    runStartedAt.put(id, System.currentTimeMillis());
                }
                return;
            }

            Long started = runStartedAt.get(id);
            if (started == null || System.currentTimeMillis() - started > RUN_TIMEOUT_MS
                    || !p.isSneaking() || !insideAuthoredCorridor(sites, world, to)) {
                reset(id);
                return;
            }

            // Reached the far door in silence?
            Site end = nearestPlacedOfType(sites, END_TYPE, world, to.getX(), to.getY(), to.getZ());
            if (end == null) return;                          // still traversing

            reset(id);                                       // consume the run
            if (!rateLimiter.tryCooldown("brann_corridor:" + id, SOLVE_COOLDOWN_MS)) return;
            final String mc = id.toString();
            final String name = p.getName();
            safety.info("brann.corridor", name + " passed the corridor in silence — posting brann-silence-corridor");
            scheduler.runAsyncSafe("brann.corridor.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* --------------------------- vibrations --------------------------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!enabled) return;
        if (!event.isSprinting()) return;                     // only STARTING to sprint is a vibration
        Player p = event.getPlayer();
        if (p != null) reset(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        if (p != null) reset(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!enabled) return;
        if (event.getDamager() instanceof Player p) reset(p.getUniqueId());
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { reset(event.getPlayer().getUniqueId()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { reset(event.getEntity().getUniqueId()); }
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) { reset(event.getPlayer().getUniqueId()); }

    /* ----------------------------- helpers ---------------------------- */

    private void reset(UUID id) {
        silentRun.remove(id);
        runStartedAt.remove(id);
    }

    private boolean insideAuthoredCorridor(SitesConfig sites, String world, Location point) {
        Site start = sites.get("brann_corridor_start");
        Site end = sites.get("brann_corridor_end");
        if (start == null || end == null || !start.isPlaced() || !end.isPlaced()) return false;
        Location a = start.location();
        Location b = end.location();
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        if (!world.equals(a.getWorld().getName()) || !world.equals(b.getWorld().getName())) return false;
        double minX = Math.min(a.getX(), b.getX()) - 5.0, maxX = Math.max(a.getX(), b.getX()) + 5.0;
        double minY = Math.min(a.getY(), b.getY()) - 2.0, maxY = Math.max(a.getY(), b.getY()) + 6.0;
        double minZ = Math.min(a.getZ(), b.getZ()) - 5.0, maxZ = Math.max(a.getZ(), b.getZ()) + 5.0;
        return point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY
                && point.getZ() >= minZ && point.getZ() <= maxZ;
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
