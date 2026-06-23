package com.observance.watcher.beats;

import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.util.Safety;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Session/anti-grief glue for the beat subsystem:
 * <ul>
 *   <li>resets a player's per-session personalized drama allowance on join (so "one big beat per
 *       session" is per actual session),</li>
 *   <li>records when a tagged Sacred Beast is killed — a tracked transgression (the herd watched) —
 *       to {@code event_log} for the dossier/showrunner, without ever blocking normal play.</li>
 * </ul>
 *
 * <p>Every handler is fully Safety-wrapped; failures are logged + swallowed.
 */
public final class BeatSessionListener implements Listener {

    private final BeatContext ctx;
    private final DramaBudget budget;
    private final Safety safety;

    public BeatSessionListener(BeatContext ctx, DramaBudget budget, Safety safety) {
        this.ctx = ctx;
        this.budget = budget;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        safety.run("beat.session.join", () -> {
            Player p = e.getPlayer();
            if (p != null) budget.resetSession(p.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        safety.run("beat.session.quit", () -> {
            Player p = e.getPlayer();
            if (p != null) {
                // Clear rate-limiter state for this player to bound memory (foundation also prunes).
                ctx.rateLimiter().clear("drama:ambient:" + p.getUniqueId());
                ctx.rateLimiter().clear("drama:personalized:" + p.getUniqueId());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        safety.run("beat.session.entityDeath", () -> {
            org.bukkit.entity.LivingEntity entity = e.getEntity();
            if (entity == null) return;
            NamespacedKey sacredKey = new NamespacedKey(ctx.namespace(), "sacred_beast");
            boolean sacred;
            try {
                sacred = entity.getPersistentDataContainer().has(sacredKey, PersistentDataType.BYTE);
            } catch (Throwable t) {
                return;
            }
            if (!sacred) return;

            // Who killed it (if a player)?
            String killerUuid = null;
            try {
                Player killer = entity.getKiller();
                if (killer != null) killerUuid = killer.getUniqueId().toString();
            } catch (Throwable ignored) { }

            final String killer = killerUuid;
            // Record the transgression to event_log async (never blocks the death).
            ctx.scheduler().runAsyncSafe("beat.session.sacredKill.write", () -> {
                SupabaseClient sb = ctx.supabase();
                if (sb == null) return;
                sb.insertEventLog(new EventLogRow(
                        "transgression",
                        "beat.sacred_beast_killed",
                        "Sacred Beast slain" + (killer != null ? " by " + killer : ""),
                        killer,
                        "{\"custom\":\"sacred_beast\"}",
                        SupabaseClient.timestampNow()));
            });
        });
    }
}
