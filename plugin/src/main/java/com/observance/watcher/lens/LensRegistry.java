package com.observance.watcher.lens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry of Lens-gated per-player displays (INTEGRATION §SIGNATURE #3 "second sight"). Any beat
 * that wants a rune/clue to be visible ONLY while a player holds the Lens registers its {@link Entity}
 * here, keyed by the OWNER player's UUID. The {@link LensListener} is the single authority that reveals
 * (via {@code Player#showEntity}) or hides (via {@code Player#hideEntity}) these displays as the owner
 * equips or holsters the Lens.
 *
 * <p><b>Why a registry, not per-beat watchdogs.</b> Multiple beats can place Lens-gated glyphs for the
 * same player. Centralising the set here means the listener flips them all in lockstep on one held-item
 * change, and a logout / display-despawn is cleaned from ONE place. Beats stay tiny: they spawn a hidden
 * display, register it, and forget it.
 *
 * <p><b>Behaviour-safe by construction.</b> Entities are stored by UUID (never a live handle that could
 * strand a reference), every read re-resolves + validity-checks via {@link Bukkit#getEntity}, and stale
 * ids are pruned on access. Thread-safe (a {@link ConcurrentHashMap} of concurrent sets) though all
 * Bukkit-touching reveal/hide calls happen on the MAIN thread through the listener.
 */
public final class LensRegistry {

    /** owner uuid -> set of gated display entity ids owned by that player. */
    private final Map<UUID, Set<UUID>> gatedByOwner = new ConcurrentHashMap<>();

    /**
     * Register a display as Lens-gated for {@code owner}. Idempotent. The entity is expected to already
     * be spawned {@code setVisibleByDefault(false)} (invisible to everyone) so it never flashes before
     * the Lens reveals it. Null-safe.
     */
    public void register(UUID owner, Entity display) {
        if (owner == null || display == null) return;
        gatedByOwner.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet())
                .add(display.getUniqueId());
    }

    /** Register by raw entity id (used when only the id is at hand). Null-safe. */
    public void register(UUID owner, UUID displayId) {
        if (owner == null || displayId == null) return;
        gatedByOwner.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(displayId);
    }

    /** Stop tracking one gated display (e.g. the beat's despawn timer fired). Null-safe. */
    public void unregister(UUID owner, UUID displayId) {
        if (owner == null || displayId == null) return;
        Set<UUID> set = gatedByOwner.get(owner);
        if (set == null) return;
        set.remove(displayId);
        if (set.isEmpty()) gatedByOwner.remove(owner);
    }

    /** Drop ALL tracking for an owner (e.g. on logout). Returns the ids that were tracked. Null-safe. */
    public Set<UUID> clearOwner(UUID owner) {
        if (owner == null) return Set.of();
        Set<UUID> set = gatedByOwner.remove(owner);
        return set == null ? Set.of() : set;
    }

    /** Forget everything (reload / shutdown). */
    public void clearAll() {
        gatedByOwner.clear();
    }

    /**
     * The still-valid gated display entities for this owner, re-resolved from their ids. Prunes any id
     * whose entity has despawned. MAIN thread (touches {@link Bukkit#getEntity}). Never null.
     */
    public List<Entity> validDisplays(UUID owner) {
        if (owner == null) return Collections.emptyList();
        Set<UUID> ids = gatedByOwner.get(owner);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        java.util.List<Entity> out = new java.util.ArrayList<>(ids.size());
        java.util.Iterator<UUID> it = ids.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity e = safeEntity(id);
            if (e != null && e.isValid()) {
                out.add(e);
            } else {
                it.remove();                       // prune the dead id
            }
        }
        if (ids.isEmpty()) gatedByOwner.remove(owner);
        return out;
    }

    /** Number of tracked (possibly-stale) ids for an owner — diagnostic. */
    public int count(UUID owner) {
        if (owner == null) return 0;
        Set<UUID> ids = gatedByOwner.get(owner);
        return ids == null ? 0 : ids.size();
    }

    /** Total owners currently tracked — diagnostic. */
    public int ownerCount() {
        return gatedByOwner.size();
    }

    private static Entity safeEntity(UUID id) {
        try {
            return Bukkit.getEntity(id);
        } catch (Throwable t) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure book-keeping the Lens leans on: register/unregister, per-owner isolation, and the
     * clear paths. Never touches Bukkit (validDisplays needs a live server), so it runs with javac alone.
     * A regression here would leak gated displays across players or strand tracking after logout.
     */
    static boolean lensRegistrySelfTest() {
        LensRegistry r = new LensRegistry();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID d1 = UUID.randomUUID();
        UUID d2 = UUID.randomUUID();
        UUID d3 = UUID.randomUUID();

        // Null-safety must never throw or record anything.
        r.register((UUID) null, d1);
        r.register(a, (UUID) null);
        if (r.ownerCount() != 0) return false;

        r.register(a, d1);
        r.register(a, d2);
        r.register(b, d3);
        if (r.count(a) != 2) return false;
        if (r.count(b) != 1) return false;
        if (r.ownerCount() != 2) return false;

        // Owners are isolated — clearing b leaves a intact.
        Set<UUID> clearedB = r.clearOwner(b);
        if (clearedB.size() != 1 || !clearedB.contains(d3)) return false;
        if (r.count(b) != 0) return false;
        if (r.count(a) != 2) return false;

        // Unregister prunes the owner entry when its last display goes.
        r.unregister(a, d1);
        if (r.count(a) != 1) return false;
        r.unregister(a, d2);
        if (r.count(a) != 0) return false;
        if (r.ownerCount() != 0) return false;

        // clearOwner on an unknown owner is empty, never null.
        if (!r.clearOwner(UUID.randomUUID()).isEmpty()) return false;

        r.register(a, d1);
        r.clearAll();
        return r.ownerCount() == 0;
    }
}
