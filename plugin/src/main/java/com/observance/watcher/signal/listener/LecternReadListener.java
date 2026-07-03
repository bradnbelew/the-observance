package com.observance.watcher.signal.listener;

import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Tracks the READS signal (DESIGN §2.1, Tier-0 "studies the lore — idle at lecterns"): the sixth
 * habit axis that previously had no source. A "read" is the player right-clicking a LECTERN that
 * holds a book (opening it to read), OR opening a WRITTEN book they're holding — the cleanest
 * faithful signal for "studies the lore." Each read bumps the per-player {@code lecternReads}
 * counter, which flows snapshot → the dossier {@code extra.lectern_reads}.
 *
 * <p>PURE TRACKING: this listener mutates only in-memory signals; it never cancels the interact,
 * never mutates the world, never messages the player. The interact is read on the MAIN thread (the
 * event) and persisted later via the tracker's async flush. Body fully wrapped in Safety.
 *
 * <p>Runs at MONITOR + ignoreCancelled so we only count reads that actually happened. A short
 * per-player cooldown collapses rapid re-clicks (page-flipping / spam) into one measured read.
 */
public final class LecternReadListener implements Listener {

    /** Per-player cooldown so a flurry of clicks / page-turns collapses to one measured read. */
    private static final long READ_COOLDOWN_MS = 3_000L;

    private final SignalTracker tracker;
    private final RateLimiter rateLimiter;
    private final Safety safety;

    public LecternReadListener(SignalTracker tracker, RateLimiter rateLimiter, Safety safety) {
        this.tracker = tracker;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // Cheap pre-filter off the safety path: only a lectern click or a book-in-hand is a "read".
        if (!isReadInteraction(event)) return;

        safety.run("signal.LecternRead", () -> {
            Player p = event.getPlayer();
            if (p == null) return;

            TrackerConfig cfg = tracker.config();
            if (!cfg.enabled()) return;

            // Anti-spam: one read per player per cooldown window (precision over recall — we want
            // "this player studies the lore", not a tally inflated by re-clicking one lectern).
            if (!rateLimiter.tryCooldown("lectern_read:" + p.getUniqueId(), READ_COOLDOWN_MS)) return;

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.addLecternRead();
        });
    }

    /** True if this interact is opening a lectern's book, or opening a written book in hand. */
    private static boolean isReadInteraction(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (b != null && b.getType() == Material.LECTERN) return true;   // opening a lectern to read
        ItemStack item = event.getItem();
        return item != null
                && (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK);
    }
}
