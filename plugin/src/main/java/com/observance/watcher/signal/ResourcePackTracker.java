package com.observance.watcher.signal;

import com.observance.watcher.util.Safety;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The resource-pack load gate (red-team MF-11). The Deep Hold's atmosphere — the rune alphabet on
 * the keeper-stones, the carved-glyph signs, the custom sounds/fog — only renders correctly for a
 * player whose client has actually APPLIED the pack. A player who declined or whose download failed
 * would otherwise see "tofu" (missing-glyph boxes) where the runes should be — an on-camera
 * immersion break and, worse, a clue rendered illegible.
 *
 * <p>This is a pure tracker + listener: it records each player's {@link PlayerResourcePackStatusEvent}
 * outcome into a concurrent map, exposes {@link #isLoaded(Player)} for rune-bearing beats to gate on
 * (fall back to an ASCII transliteration, or skip, when not loaded), and forwards every status change
 * to a sink so the director's dashboard can SEE who is pack-ready before a rune beat is dispatched.
 * It never cancels anything, never messages the player, never mutates the world. Body in Safety.
 *
 * <p>State is keyed by UUID and survives a config reload (the instance is created once); it is cleared
 * for a player only on quit. The status enum is intentionally coarse and switches on only the four
 * version-stable {@code Status} constants (newer ones default to {@code NONE}), so the gate compiles
 * across Paper API versions and only ever reports {@code LOADED} on a genuine SUCCESSFULLY_LOADED.
 */
public final class ResourcePackTracker implements Listener {

    /** Coarse, gate-relevant pack state. Only LOADED unlocks rune rendering. */
    public enum PackStatus { NONE, LOADING, LOADED, DECLINED, FAILED }

    /** Sink for surfacing pack status to the dashboard / event_log. Never throws back in. */
    public interface PackEventSink {
        void onPackStatus(UUID uuid, String name, PackStatus status);
    }

    private final Map<UUID, PackStatus> status = new ConcurrentHashMap<>();
    private final Safety safety;
    private final PackEventSink sink;   // nullable

    public ResourcePackTracker(Safety safety, PackEventSink sink) {
        this.safety = safety;
        this.sink = sink;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        safety.run("signal.pack.status", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            PackStatus s = map(event.getStatus());
            status.put(p.getUniqueId(), s);
            safety.info("signal.pack", p.getName() + " resource-pack: " + s);
            if (sink != null) {
                try {
                    sink.onPackStatus(p.getUniqueId(), p.getName(), s);
                } catch (Throwable ignored) {
                    // a dashboard write must never break tracking
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        safety.run("signal.pack.quit", () -> {
            Player p = event.getPlayer();
            if (p != null) status.remove(p.getUniqueId());
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Query surface for beats / the director.                           */
    /* ------------------------------------------------------------------ */

    public PackStatus status(UUID id) {
        return id == null ? PackStatus.NONE : status.getOrDefault(id, PackStatus.NONE);
    }

    /** True only once the player's client has fully applied the pack (runes are safe to render). */
    public boolean isLoaded(UUID id) {
        return status(id) == PackStatus.LOADED;
    }

    public boolean isLoaded(Player p) {
        return p != null && isLoaded(p.getUniqueId());
    }

    /** True when EVERY player in the collection has the pack — the gate for a GROUP rune beat. */
    public boolean allLoaded(Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) return false;
        for (Player p : players) {
            if (!isLoaded(p)) return false;
        }
        return true;
    }

    /** Maps the Bukkit status to our coarse gate state. Switches only on the four version-stable
     *  constants; any newer/unknown status (DOWNLOADED, INVALID_URL, FAILED_RELOAD, DISCARDED, …)
     *  defaults to NONE — so the gate stays closed until a genuine SUCCESSFULLY_LOADED. */
    private static PackStatus map(PlayerResourcePackStatusEvent.Status s) {
        if (s == null) return PackStatus.NONE;
        switch (s) {
            case SUCCESSFULLY_LOADED: return PackStatus.LOADED;
            case ACCEPTED:            return PackStatus.LOADING;
            case DECLINED:            return PackStatus.DECLINED;
            case FAILED_DOWNLOAD:     return PackStatus.FAILED;
            default:                  return PackStatus.NONE;
        }
    }
}
