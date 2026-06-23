package com.observance.watcher.signal.listener;

import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.Safety;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Tracks death signals (DESIGN §2.1, §2.4):
 * <ul>
 *   <li>{@link PlayerDeathEvent} → player death count;</li>
 *   <li>{@link EntityDeathEvent} → mob kills credited to the killer; and The Sacred Beast custom —
 *       killing a PDC-tagged "pale one" is a tracked VIOLATION for the killer.</li>
 * </ul>
 *
 * PURE TRACKING: no world effects. All Bukkit reads on the MAIN thread; persistence is deferred to
 * the tracker's async flush. Body fully wrapped in Safety.
 */
public final class DeathListener implements Listener {

    private final SignalTracker tracker;
    private final Safety safety;
    private final NamespacedKey sacredBeastKey;
    /**
     * The canonical key the {@code sacred_animal} beat actually tags with:
     * namespace "observance", key "sacred_beast". The config key ({@link #sacredBeastKey}) is a
     * Phase-1 alias; we check BOTH so the violation fires regardless of which path set the tag.
     * (Cross-package desync fix: SacredAnimalBeat writes "sacred_beast"; the config default is
     * "observance_sacred_beast", which previously never matched.)
     */
    private final NamespacedKey beatSacredKey;

    public DeathListener(Plugin plugin, SignalTracker tracker, Safety safety) {
        this.tracker = tracker;
        this.safety = safety;
        // The PDC key the Sacred Beast is tagged with (set by the beast-tagging subsystem, Phase 1).
        NamespacedKey key;
        try {
            key = new NamespacedKey(plugin, safeKey(tracker.config().sacredBeastPdcKey()));
        } catch (Throwable t) {
            key = new NamespacedKey(plugin, "observance_sacred_beast");
        }
        this.sacredBeastKey = key;
        // The key the in-process SacredAnimalBeat uses (BeatContext namespace "observance").
        NamespacedKey beatKey;
        try {
            beatKey = new NamespacedKey("observance", "sacred_beast");
        } catch (Throwable t) {
            beatKey = key;
        }
        this.beatSacredKey = beatKey;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        safety.run("signal.PlayerDeath", () -> {
            Player p = event.getEntity();
            if (p == null) return;
            if (!tracker.config().enabled()) return;
            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.addDeath();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        safety.run("signal.EntityDeath", () -> {
            LivingEntity dead = event.getEntity();
            if (dead == null) return;
            if (!tracker.config().enabled()) return;
            if (dead instanceof Player) return; // handled by PlayerDeath

            Player killer = dead.getKiller();
            if (killer == null) return; // environmental death — not credited to anyone

            PlayerSignals ps = tracker.signals(killer.getUniqueId(), killer.getName());
            ps.addMobKill();

            // The Sacred Beast: if the dead entity carries the sacred-beast PDC tag, killing it is
            // a violation for the killer.
            if (isSacredBeast(dead)) {
                ps.violate(TrackerConfig.CUSTOM_SACRED_BEAST, System.currentTimeMillis());
                safety.info("signal.sacred_beast", killer.getName() + " killed the pale one");
            }
        });
    }

    private boolean isSacredBeast(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (hasFlag(pdc, sacredBeastKey)) return true;
            // Also honor the canonical beat tag (namespace "observance", key "sacred_beast").
            return beatSacredKey != null && !beatSacredKey.equals(sacredBeastKey)
                    && hasFlag(pdc, beatSacredKey);
        } catch (Throwable t) {
            return false; // never let a PDC quirk crash the handler
        }
    }

    private static boolean hasFlag(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc == null || key == null) return false;
        try {
            Byte flag = pdc.get(key, PersistentDataType.BYTE);
            return flag != null && flag != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String safeKey(String raw) {
        if (raw == null || raw.isBlank()) return "observance_sacred_beast";
        // NamespacedKey keys must be [a-z0-9._-]; normalize defensively.
        String k = raw.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return k.isBlank() ? "observance_sacred_beast" : k;
    }
}
