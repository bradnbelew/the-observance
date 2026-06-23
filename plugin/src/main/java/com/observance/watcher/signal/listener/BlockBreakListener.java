package com.observance.watcher.signal.listener;

import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.Safety;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Tracks block-breaking signals (DESIGN §2.1, §2.4):
 * <ul>
 *   <li>blocks mined (+ ore subset), deepest Y reached this run;</li>
 *   <li>first ore of session (The Offering hook — flags non-compliance until honored elsewhere);</li>
 *   <li>The Deep Line custom: breaking below the depth threshold is a tracked VIOLATION.</li>
 * </ul>
 *
 * PURE TRACKING: this listener mutates only in-memory signals; it never cancels the break, never
 * mutates the world, never messages the player. The break is read on the MAIN thread (the event)
 * and the persistence happens later via the tracker's async flush. Body fully wrapped in Safety.
 *
 * <p>Runs at MONITOR + ignoreCancelled so we only count breaks that actually happened (protected
 * blocks cancelled by the protection listener never reach us).
 */
public final class BlockBreakListener implements Listener {

    private final SignalTracker tracker;
    private final Safety safety;

    public BlockBreakListener(SignalTracker tracker, Safety safety) {
        this.tracker = tracker;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        safety.run("signal.BlockBreak", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            Block block = event.getBlock();
            if (block == null) return;

            TrackerConfig cfg = tracker.config();
            if (!cfg.enabled()) return;

            String matName = block.getType().name();
            boolean isOre = cfg.isOre(matName);
            int y = block.getY();

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.addBlockMined(y, isOre);

            long now = System.currentTimeMillis();

            // The Offering: flag the first ore of the session. Compliance (honoring) is detected
            // by the drop listener; here we only record that the session "owes" an offering.
            if (isOre && ps.markFirstOreThisSession()) {
                // We do NOT mark a violation yet — non-compliance is resolved at session end /
                // next descent by the custom engine reading offeringHonoredThisSession. The flag
                // simply records "first ore taken, not yet given back."
                safetyInfo("first ore of session", p);
            }

            // The Deep Line: breaking below the threshold "bare" is a violation. Phase 0 treats
            // any break below the line as the violation signal (the "bare" nuance — no offering /
            // ward — is a Phase-1 refinement). Honoring is the absence of deep breaks.
            if (cfg.deepLineEnabled() && y < cfg.deepLineY()) {
                ps.violate(TrackerConfig.CUSTOM_DEEP_LINE, now);
            }
        });
    }

    private void safetyInfo(String what, Player p) {
        // Lightweight, rate-naturally-limited (once/session) info breadcrumb. Never throws.
        try {
            safety.info("signal.offering", what + " for " + p.getName());
        } catch (Throwable ignored) { /* never propagate */ }
    }
}
