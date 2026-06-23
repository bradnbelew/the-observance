package com.observance.watcher.listener;

import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.PlayerRow;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Foundation listener: upserts the {@code players} row on join (mc_uuid, name, last_seen) and on
 * quit. This is the REFERENCE PATTERN every subsystem listener must follow:
 *
 * <ul>
 *   <li>Handler body is wrapped in {@link Safety} (exception logged + swallowed).</li>
 *   <li>Bukkit objects (the Player) are read on the MAIN thread (inside the event).</li>
 *   <li>The Supabase write is dispatched to an ASYNC task — never block the event thread.</li>
 * </ul>
 */
public final class PresenceListener implements Listener {

    private final SupabaseClient supabase;
    private final Scheduler scheduler;
    private final Safety safety;
    private final SignalTracker tracker;     // nullable in theory; always set in Phase 0

    public PresenceListener(SupabaseClient supabase, Scheduler scheduler, Safety safety,
                            SignalTracker tracker) {
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.safety = safety;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        safety.run("listener.PlayerJoin", () -> {
            Player p = event.getPlayer();
            // Snapshot the values we need on the main thread...
            final String uuid = p.getUniqueId().toString();
            final String name = p.getName();
            // Start the player's tracker session (resets session-scoped flags; main thread, cheap).
            if (tracker != null) tracker.onJoin(p.getUniqueId(), name);
            // ...then write off-thread.
            scheduler.runAsyncSafe("listener.PlayerJoin.write", () ->
                    supabase.upsertPlayer(new PlayerRow(uuid, name, SupabaseClient.timestampNow())));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        safety.run("listener.PlayerQuit", () -> {
            Player p = event.getPlayer();
            final String uuid = p.getUniqueId().toString();
            final String name = p.getName();
            // Eagerly flush this player's dossier off-thread so a quit doesn't lose recent signals.
            if (tracker != null) tracker.onQuit(p.getUniqueId());
            scheduler.runAsyncSafe("listener.PlayerQuit.write", () ->
                    supabase.upsertPlayer(new PlayerRow(uuid, name, SupabaseClient.timestampNow())));
        });
    }
}
