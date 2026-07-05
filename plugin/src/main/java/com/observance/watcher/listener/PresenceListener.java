package com.observance.watcher.listener;

import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.PlayerRow;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

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
    private final String namespace;

    /** The PDC sub-key {@code NamedMobBeat} tags an offline-skin apparition with (the worn player's
     *  UUID, as a string) — see NamedMobBeat's {@code key(ctx, "worn_skin")} write, under the beat
     *  engine's "observance" namespace. Kept in sync by literal here, matching the sibling constant
     *  idiom this class doesn't otherwise use (PresenceListener has no BeatContext of its own). */
    private static final String WORN_SKIN_SUB_KEY = "worn_skin";

    public PresenceListener(SupabaseClient supabase, Scheduler scheduler, Safety safety,
                            SignalTracker tracker) {
        this(supabase, scheduler, safety, tracker, "observance");
    }

    public PresenceListener(SupabaseClient supabase, Scheduler scheduler, Safety safety,
                            SignalTracker tracker, String namespace) {
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.safety = safety;
        this.tracker = tracker;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
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
            // Reveal-disciplined precision fix (INV-16 / bestiary §2.7): the instant the impersonated
            // player rejoins, sweep for and despawn any offline-skin apparition wearing their shape —
            // "a wrong wearing is worse than none." Runs on the MAIN thread (entity removal), before
            // the async write below, so a fast reconnect can never see its own worn-skin apparition.
            despawnApparitionsWearing(p.getUniqueId());
            // ...then write off-thread.
            scheduler.runAsyncSafe("listener.PlayerJoin.write", () ->
                    supabase.upsertPlayer(new PlayerRow(uuid, name, SupabaseClient.timestampNow())));
        });
    }

    /**
     * Despawn any offline-skin apparition ({@code NamedMobBeat}, B3/WEB-MASTER §P2.5, INV-16) tagged
     * as wearing {@code wornUuid}'s shape, immediately — the precision law that a wrong wearing is
     * worse than none. Sweeps all loaded worlds' entities for the {@code worn_skin} PDC tag (the same
     * tag {@code NamedMobBeat} sets via {@code key(ctx, "worn_skin")}) matching the joining player's
     * UUID, and removes each match. MAIN THREAD ONLY (entity removal); mirrors the existing
     * {@code KeeperNpc.despawn()} / {@code TownsfolkNpc} world-sweep idiom used elsewhere in this
     * codebase. Never throws; a world-iteration quirk is swallowed (best-effort, reveal-disciplined —
     * this is a belt-and-suspenders instant despawn on top of any periodic recheck, not the only path).
     */
    public void despawnApparitionsWearing(java.util.UUID wornUuid) {
        if (wornUuid == null) return;
        try {
            NamespacedKey key = new NamespacedKey(namespace, WORN_SKIN_SUB_KEY);
            String wornStr = wornUuid.toString();
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    try {
                        String tagged = e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        if (tagged != null && tagged.equals(wornStr)) {
                            e.remove();
                        }
                    } catch (Throwable ignored) {
                        // a single quirky entity must never abort the sweep
                    }
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort, never crash the join handler
        }
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
