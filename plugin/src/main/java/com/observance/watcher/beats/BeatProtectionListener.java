package com.observance.watcher.beats;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enforces {@link ProtectedRegistry} + {@code sites.yml protect}: any attempt to break, burn,
 * explode, piston-push, flow over, or mob-mutate a beat-placed / protected block is cancelled, or
 * (config) restored on the next tick out of line of sight. Players cannot destroy or weaponize
 * authored content even if they discover it.
 *
 * <p>Every handler is fully Safety-wrapped and null-guarded. A failure here NEVER cancels the wrong
 * block or crashes; on doubt it does nothing (fails open for ordinary blocks, closed for protected).
 */
public final class BeatProtectionListener implements Listener {

    private final ProtectedRegistry registry;
    private final Scheduler scheduler;
    private final Safety safety;
    private final boolean cancelBreaks;
    private final boolean restoreBroken;
    private final long restoreDelayTicks;

    /**
     * Precomputed immutable set of "world:x:y:z" block keys for protected sites, snapshotted at
     * construction. This keeps the hot guard path O(1) instead of iterating every placed site +
     * cloning Locations on EVERY block-break / liquid-flow / piston / explosion event (BlockFromToEvent
     * alone fires constantly). The engine rebuilds this listener on reload, so the snapshot stays fresh.
     */
    private final Set<String> protectedSiteKeys;

    public BeatProtectionListener(ProtectedRegistry registry, SitesConfig sites,
                                  Scheduler scheduler, Safety safety,
                                  boolean cancelBreaks, boolean restoreBroken, long restoreDelayTicks) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.safety = safety;
        this.cancelBreaks = cancelBreaks;
        this.restoreBroken = restoreBroken;
        this.restoreDelayTicks = Math.max(0L, restoreDelayTicks);
        this.protectedSiteKeys = snapshotProtectedSites(sites);
    }

    /** Snapshot the protected-site anchor block keys once (MAIN thread at construction). */
    private static Set<String> snapshotProtectedSites(SitesConfig sites) {
        Set<String> out = new HashSet<>();
        if (sites == null) return out;
        try {
            for (Site s : sites.placed()) {
                if (!s.protect()) continue;
                Location anchor = s.location();
                if (anchor == null || anchor.getWorld() == null) continue;
                out.add(anchor.getWorld().getName() + ":" + anchor.getBlockX()
                        + ":" + anchor.getBlockY() + ":" + anchor.getBlockZ());
            }
        } catch (Throwable ignored) {
            // a bad site never makes protection throw — fall back to whatever we gathered
        }
        return out;
    }

    /* ------------------------------ break ----------------------------- */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        safety.run("beat.protect.break", () -> {
            Block b = e.getBlock();
            if (b == null) return;
            if (!isGuarded(b.getLocation())) return;
            if (cancelBreaks) {
                e.setCancelled(true);            // simplest, jank-free: it just won't break
            } else {
                scheduleRestore(b);              // let it break, restore next tick out of sight
            }
        });
    }

    /* --------------------------- explosions --------------------------- */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        safety.run("beat.protect.entityExplode", () -> {
            List<Block> blocks = e.blockList();
            if (blocks == null || blocks.isEmpty()) return;
            blocks.removeIf(b -> b != null && isGuarded(b.getLocation()));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        safety.run("beat.protect.blockExplode", () -> {
            List<Block> blocks = e.blockList();
            if (blocks == null || blocks.isEmpty()) return;
            blocks.removeIf(b -> b != null && isGuarded(b.getLocation()));
        });
    }

    /* ------------------------------ burn ------------------------------ */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        safety.run("beat.protect.burn", () -> {
            if (e.getBlock() != null && isGuarded(e.getBlock().getLocation())) e.setCancelled(true);
        });
    }

    /* ----------------------- liquid flow / pistons -------------------- */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent e) {
        safety.run("beat.protect.fromto", () -> {
            if (e.getToBlock() != null && isGuarded(e.getToBlock().getLocation())) e.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        safety.run("beat.protect.pistonExtend", () -> {
            if (anyGuarded(e.getBlocks())) e.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        safety.run("beat.protect.pistonRetract", () -> {
            if (anyGuarded(e.getBlocks())) e.setCancelled(true);
        });
    }

    /* --------------------- mob block-change (endermen) ---------------- */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        safety.run("beat.protect.entityChange", () -> {
            if (e.getBlock() != null && isGuarded(e.getBlock().getLocation())) e.setCancelled(true);
        });
    }

    /* --------------------------- maintenance -------------------------- */

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        safety.run("beat.protect.worldUnload", () -> {
            if (e.getWorld() != null) registry.clearWorld(e.getWorld().getName());
        });
    }

    /* ------------------------------ helpers --------------------------- */

    private boolean isGuarded(Location loc) {
        if (loc == null) return false;
        if (registry.isProtected(loc)) return true;
        // sites.yml protected sites: O(1) lookup against the precomputed anchor-key set.
        if (!protectedSiteKeys.isEmpty()) {
            org.bukkit.World w = loc.getWorld();
            if (w != null) {
                String key = w.getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY()
                        + ":" + loc.getBlockZ();
                if (protectedSiteKeys.contains(key)) return true;
            }
        }
        return false;
    }

    private boolean anyGuarded(List<Block> blocks) {
        if (blocks == null) return false;
        for (Block b : blocks) {
            if (b != null && isGuarded(b.getLocation())) return true;
        }
        return false;
    }

    private void scheduleRestore(Block broken) {
        if (!restoreBroken) return;
        String intended = registry.intendedData(broken.getLocation());
        final Location loc = broken.getLocation().clone();
        scheduler.runLaterSafe("beat.protect.restore", restoreDelayTicks, () -> {
            if (loc.getWorld() == null) return;
            Block b = loc.getBlock();
            if (intended != null) {
                try {
                    BlockData data = org.bukkit.Bukkit.createBlockData(intended);
                    b.setBlockData(data, false);
                } catch (Throwable t) {
                    safety.warn("beat.protect.restore", "bad intended data: " + intended);
                }
            }
        });
    }
}
