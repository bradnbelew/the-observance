package com.observance.watcher.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * THE PRESIDING KEEPER — the single group-scoped rite NPC {@link com.observance.watcher.signal.listener.
 * KeeperNpcListener} listens for. This manager OWNS the Keeper's in-world body: it spawns him (once),
 * tracks the backing entity, and stamps the {@code observance:keeper_npc} PersistentDataContainer marker
 * the listener reads (mirrors {@link WrenNpc} exactly — same "real entity, PDC tag, no hard Citizens
 * dependency" shape, same singleton discipline).
 *
 * <p><b>Two bodies, one marker.</b> If Citizens2 is present (string-probed, never hard-imported), the
 * Keeper is a Citizens NPC. Absent Citizens, he degrades to a PDC-tagged ARMOR STAND given a player head
 * and a name — identical fallback to Wren's. Either body carries the identical marker.
 *
 * <p><b>The node hint.</b> {@code KeeperNpcListener} reads the marker's STRING value as an optional
 * "entry node" override (blank = let the showrunner branch from the dossier per {@code arc_state.flags} +
 * {@code punishment_state}, per its own javadoc). {@link #spawn(Location, String)} accepts that hint so
 * a site-specific placement (the Threshold vs. the Undercroft altar) can carry a different bound entry
 * point without any code change — purely a placement-time value.
 *
 * <p><b>Group-scoped: exactly ONE Keeper body.</b> A spawn call first removes any existing marked Keeper
 * in the loaded worlds, then creates a fresh one — a re-spawn relocates him rather than cloning him,
 * exactly like Wren.
 *
 * <p>All world mutation here is MAIN thread only (Bukkit contract). Every entry point is defensive
 * (null / world-unloaded / entity-dead guarded) and never throws into a caller.
 */
public final class KeeperNpc {

    /** PDC sub-key marking an entity as the Keeper (value = the node hint {@link KeeperNpcListener}
     *  reads — must match {@code KeeperNpcListener.PDC_KEEPER} exactly). */
    public static final String PDC_KEEPER = "keeper_npc";

    /** The Keeper's display name — deliberately unspecific; the dossier branch decides which keeper's
     *  bound lines actually speak once the showrunner resolves the open. */
    public static final String DISPLAY_NAME = "the keeper";

    private final NamespacedKey markerKey;
    private final boolean citizensAvailable;
    private final CitizensBridge citizens;   // null unless Citizens is present

    /** UUID of the currently spawned Keeper body (null when not spawned). */
    private volatile UUID entityId;

    public KeeperNpc(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.markerKey = new NamespacedKey(ns, PDC_KEEPER);
        this.citizensAvailable = probeCitizens();
        this.citizens = citizensAvailable ? new CitizensBridge() : null;
    }

    /* ------------------------------------------------------------------ */
    /*  Spawn / placement                                                  */
    /* ------------------------------------------------------------------ */

    /** Spawn (or relocate) the Keeper at {@code loc} with no node-hint override (blank — the showrunner
     *  branches purely from the dossier). Convenience overload of {@link #spawn(Location, String)}. */
    public Entity spawn(Location loc) {
        return spawn(loc, "");
    }

    /**
     * Spawn (or relocate) the one group-scoped Keeper at {@code loc}, stamping {@code nodeHint} into the
     * PDC marker. Removes any pre-existing marked Keeper first, then places a Citizens NPC when
     * available, else a PDC-tagged armor stand. Returns the spawned entity, or {@code null} on failure.
     * MAIN thread only.
     */
    public Entity spawn(Location loc, String nodeHint) {
        if (loc == null || loc.getWorld() == null) return null;

        // Enforce the singleton: clear any previous body (this or a prior session left one).
        despawn();

        Entity body = null;
        if (citizens != null) {
            body = citizens.spawnPlayerNpc(DISPLAY_NAME, loc);
        }
        if (body == null) {
            body = spawnFallback(loc);
        }
        if (body == null) return null;

        tag(body, nodeHint == null ? "" : nodeHint);
        this.entityId = body.getUniqueId();
        return body;
    }

    /**
     * The Citizens-absent body: a stable armor stand wearing a player head, named "the keeper", with no
     * gravity and no base plate so it stands like a person. Kept persistent + invulnerable so it isn't
     * griefed. Mirrors {@link WrenNpc#spawnFallback(Location)} exactly.
     */
    private Entity spawnFallback(Location loc) {
        try {
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            as.customName(net.kyori.adventure.text.Component.text(DISPLAY_NAME,
                    net.kyori.adventure.text.format.NamedTextColor.GRAY));
            as.setCustomNameVisible(true);
            as.setGravity(false);
            as.setBasePlate(false);
            as.setArms(true);
            as.setInvulnerable(true);
            as.setPersistent(true);
            as.setCanPickupItems(false);
            try {
                var head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
                as.getEquipment().setHelmet(head);
            } catch (Throwable ignored) {
                // equipment quirk — cosmetic only
            }
            return as;
        } catch (Throwable t) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Body lifecycle                                                     */
    /* ------------------------------------------------------------------ */

    /** The currently spawned Keeper body, or null if none / dead / world unloaded. MAIN thread only. */
    public Entity body() {
        UUID id = this.entityId;
        if (id == null) return null;
        try {
            Entity e = Bukkit.getEntity(id);
            return (e != null && e.isValid()) ? e : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** True if a live Keeper body currently exists. */
    public boolean isSpawned() {
        return body() != null;
    }

    /**
     * Remove the current Keeper body (and any stray marked Keeper in loaded worlds, defensively). MAIN
     * thread only. Never throws.
     */
    public void despawn() {
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    if (isKeeper(e)) {
                        removeBody(e);
                    }
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort
        }
        this.entityId = null;
    }

    /** Remove a single Keeper entity, dismissing the Citizens NPC cleanly when it is one. */
    private void removeBody(Entity e) {
        if (e == null) return;
        if (citizens != null) {
            destroyCitizensIfNpc(e);
        }
        try {
            if (e.isValid()) e.remove();
        } catch (Throwable ignored) {
            // ignore
        }
    }

    /* ------------------------------------------------------------------ */
    /*  PDC marker                                                         */
    /* ------------------------------------------------------------------ */

    /** Stamp the Keeper marker (with an optional node hint) onto an entity's PDC. */
    public void tag(Entity e, String nodeHint) {
        if (e == null) return;
        try {
            e.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    nodeHint == null ? "" : nodeHint);
        } catch (Throwable ignored) {
            // some entity impls reject PDC — the fallback armor stand does not
        }
    }

    /** True if the entity carries the Keeper marker. */
    public boolean isKeeper(Entity e) {
        if (e == null) return false;
        try {
            return e.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean citizensAvailable() { return citizensAvailable; }

    /** Human-readable NPC backend, for the admin status line. */
    public String backend() { return citizensAvailable ? "Citizens" : "armor-stand fallback"; }

    /* ------------------------------------------------------------------ */
    /*  Citizens capability probe (string-based, no hard import)          */
    /* ------------------------------------------------------------------ */

    /** Probe for Citizens2 exactly like {@link WrenNpc#probeCitizens()}. */
    private static boolean probeCitizens() {
        try {
            boolean present = Bukkit.getPluginManager().isPluginEnabled("Citizens");
            if (!present) return false;
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Class.forName("net.citizensnpcs.api.npc.NPC");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * If {@code e} is the backing entity of a Citizens NPC, destroy that NPC (so Citizens doesn't
     * respawn it) and return true. All reflective so no Citizens type is referenced at compile time.
     */
    private static boolean destroyCitizensIfNpc(Entity e) {
        try {
            Class<?> apiCls = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = apiCls.getMethod("getNPCRegistry").invoke(null);
            if (registry == null) return false;
            Object npc = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, e);
            if (npc == null) return false;
            npc.getClass().getMethod("destroy").invoke(npc);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
