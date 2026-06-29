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
 * <p><b>The permanence fork (INV-13, WEB-MASTER §3.4 / A11+A12):</b> two distinct PDC populations must
 * NOT be conflated here, or the precision contract ("it knows you" only on a measured signal) and the
 * fairly-avoidable-fork guarantee both break:
 * <ul>
 *   <li>the <b>fork-arming Sacred Beast</b> — the ONE glowing animal, additionally tagged
 *       {@code sacred_fork_arm} by {@code SacredAnimalBeat} on the last-tagged beast. Its kill is the
 *       irreversible Fork A act → records the sacred-beast violation AND signals the one-way
 *       {@code sacred_beast_broken} promotion (first-writer-wins is enforced downstream, set-once);</li>
 *   <li>the <b>cosmetic Pale herd</b> — the between-session conversion, tagged {@code pale_cosmetic} and
 *       (by law) NEVER glowing. These are decoration. Killing one is <b>ignored for conduct</b> — never a
 *       violation, never fork-arming (INV-13 precision guard). A {@code pale_cosmetic} byte short-circuits
 *       the sacred check even if a stale {@code sacred_beast} tag is somehow also present.</li>
 * </ul>
 *
 * PURE TRACKING: no world effects, no DB writes. All Bukkit reads on the MAIN thread; persistence is
 * deferred to the tracker's async flush. The {@code sacred_beast_broken} arc-flag itself is set
 * first-writer-wins by the showrunner/oracle from this measured conduct — never written from here. Body
 * fully wrapped in Safety.
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
    /** The byte {@code SacredAnimalBeat} sets on the LAST tagged (glowing) beast — the fork-arming one.
     *  Only its death arms Fork A; a second cow death is a no-op (the last-tagged-only rule). */
    private final NamespacedKey forkArmKey;
    /** The cosmetic herd-conversion byte. A beast carrying this is decoration: ignored for conduct,
     *  never a violation, never fork-arming (INV-13). Namespace matches the beat's BeatContext. */
    private final NamespacedKey paleCosmeticKey;

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
        // Fork-arm + pale-cosmetic bytes — same "observance" namespace SacredAnimalBeat tags under.
        NamespacedKey arm, pale;
        try { arm = new NamespacedKey("observance", "sacred_fork_arm"); }
        catch (Throwable t) { arm = null; }
        try { pale = new NamespacedKey("observance", "pale_cosmetic"); }
        catch (Throwable t) { pale = null; }
        this.forkArmKey = arm;
        this.paleCosmeticKey = pale;
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

            // INV-13 precision guard: a cosmetic Pale (herd-conversion decoration) is NEVER conduct.
            // Killing one is just a mob kill — no violation, no fork. Short-circuit before any sacred
            // check so a stale/co-tagged sacred byte can't mis-fire "it knows you" on decoration.
            if (isPaleCosmetic(dead)) return;

            // The Sacred Beast: if the dead entity carries the sacred-beast PDC tag, killing it is
            // a violation for the killer.
            if (isSacredBeast(dead)) {
                ps.violate(TrackerConfig.CUSTOM_SACRED_BEAST, System.currentTimeMillis());
                // Fork A (INV-12/13): ONLY the last-tagged glowing beast arms the irreversible fork.
                // We record the same measured violation; the distinct info marker tells the showrunner
                // this kill was the fork-arming one, which it promotes to `sacred_beast_broken`
                // first-writer-wins (set-once, downstream). A non-arming sacred kill is a plain violation.
                if (isForkArming(dead)) {
                    safety.info("signal.sacred_beast.fork_arm",
                            killer.getName() + " killed the kept one — fork A armed");
                } else {
                    safety.info("signal.sacred_beast", killer.getName() + " killed the pale one");
                }
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

    /** The fork-arming (glowing, last-tagged) beast — its kill arms the irreversible Fork A. */
    private boolean isForkArming(LivingEntity entity) {
        try {
            return hasFlag(entity.getPersistentDataContainer(), forkArmKey);
        } catch (Throwable t) {
            return false;
        }
    }

    /** A cosmetic Pale (herd conversion) — decoration, ignored for conduct (INV-13). */
    private boolean isPaleCosmetic(LivingEntity entity) {
        try {
            return hasFlag(entity.getPersistentDataContainer(), paleCosmeticKey);
        } catch (Throwable t) {
            return false;
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
