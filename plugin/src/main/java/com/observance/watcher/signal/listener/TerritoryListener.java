package com.observance.watcher.signal.listener;

import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.util.Safety;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Feeds the {@link com.observance.watcher.signal.BaseDetector} (DESIGN §2.2) from real events:
 * <ul>
 *   <li>{@link BlockPlaceEvent} — building density → cluster centroid;</li>
 *   <li>{@link PlayerBedEnterEvent} — a bed/respawn anchor ("home");</li>
 *   <li>{@link InventoryOpenEvent} on a block container — container density → confidence bump.</li>
 * </ul>
 *
 * PURE TRACKING: no world effects, no cancellation. All Bukkit reads on the MAIN thread; the
 * detector holds plain data and is drained by the async base pass. Bodies wrapped in Safety.
 */
public final class TerritoryListener implements Listener {

    private final SignalTracker tracker;
    private final Safety safety;

    public TerritoryListener(SignalTracker tracker, Safety safety) {
        this.tracker = tracker;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        safety.run("signal.BlockPlace", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            if (!tracker.config().enabled()) return;
            Block b = event.getBlockPlaced();
            if (b == null || b.getWorld() == null) return;
            tracker.baseDetector().recordPlacement(
                    p.getUniqueId(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        safety.run("signal.BedEnter", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            if (!tracker.config().enabled()) return;
            Block bed = event.getBed();
            if (bed == null || bed.getWorld() == null) return;
            tracker.baseDetector().recordBedAnchor(
                    p.getUniqueId(), bed.getWorld().getName(), bed.getX(), bed.getY(), bed.getZ());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerOpen(InventoryOpenEvent event) {
        safety.run("signal.ContainerOpen", () -> {
            if (!tracker.config().enabled()) return;
            if (!(event.getPlayer() instanceof Player p)) return;
            // Only count BLOCK containers (chests/barrels/etc.), not the player's own inventory or
            // transient menus — those don't indicate a base.
            InventoryHolder holder = event.getInventory().getHolder();
            if (!(holder instanceof org.bukkit.block.BlockState)
                    && !(holder instanceof org.bukkit.block.DoubleChest)) {
                return;
            }
            tracker.baseDetector().recordContainer(p.getUniqueId());
        });
    }
}
