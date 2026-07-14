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
 * WREN — the single group-scoped companion NPC. This manager OWNS Wren's in-world body: it spawns him
 * (once), tracks the backing entity, and stamps the {@code observance:wren_npc} PersistentDataContainer
 * marker so {@link WrenNpcListener} recognises a right-click on him through the ordinary Bukkit interact
 * event — never the Citizens API (Path-A self-containment, mirrors {@link
 * com.observance.watcher.signal.listener.KeeperNpcListener}).
 *
 * <p><b>Two bodies, one marker.</b> If Citizens2 is present (string-probed like the FAWE probe in
 * {@link com.observance.watcher.beats.lib.Schematics}; never hard-imported), Wren is a player-skinned
 * Citizens NPC that reads as a real peer. Absent Citizens, he degrades to a PDC-tagged ARMOR STAND
 * given a player head and a name — the same "back an NPC with a real entity, tag it in PDC" idea the
 * Keeper uses. Either body carries the identical marker, so everything downstream (interaction,
 * proximity, despawn-during-a-Watcher-beat) works uniformly.
 *
 * <p><b>Group-scoped: exactly ONE Wren.</b> A spawn call first removes any existing marked Wren in the
 * loaded worlds, then creates a fresh one — a re-spawn relocates him rather than cloning him. The
 * tracked {@link #entityId} is the authority for the current body.
 *
 * <p>All world mutation here is MAIN thread only (Bukkit contract). Every entry point is defensive
 * (null / world-unloaded / entity-dead guarded) and never throws into a caller.
 */
public final class WrenNpc {

    /** PDC sub-key marking an entity as Wren (value = a node hint the showrunner may branch on). */
    public static final String PDC_WREN = "wren_npc";

    /** Wren's display name. A short, warm, old-sounding peer name (design brief §working-name). */
    public static final String DISPLAY_NAME = "Wren";

    private final NamespacedKey markerKey;
    private final NamespacedKey v5NpcKey;
    private final boolean citizensAvailable;
    private final CitizensBridge citizens;   // null unless Citizens is present

    /** UUID of the currently spawned Wren body (null when not spawned). */
    private volatile UUID entityId;

    public WrenNpc(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.markerKey = new NamespacedKey(ns, PDC_WREN);
        this.v5NpcKey = new NamespacedKey(ns, "v5_npc_id");
        this.citizensAvailable = probeCitizens();
        this.citizens = citizensAvailable ? new CitizensBridge() : null;
    }

    /* ------------------------------------------------------------------ */
    /*  Spawn / placement                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Spawn (or relocate) the one group-scoped Wren at {@code loc}. Removes any pre-existing marked
     * Wren first, then places a Citizens NPC when available, else a PDC-tagged armor stand. Returns
     * the spawned entity, or {@code null} on failure. MAIN thread only.
     */
    public Entity spawn(Location loc) {
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

        tag(body, "");            // stamp the marker (blank node hint → showrunner branches)
        this.entityId = body.getUniqueId();
        return body;
    }

    /**
     * The Citizens-absent body: a stable armor stand wearing a player head, named "Wren", with no
     * gravity and no base plate so it stands like a person. Not a perfect human, but a clearly-present,
     * interactable figure that carries the marker. Kept persistent + invulnerable so it isn't griefed.
     */
    private Entity spawnFallback(Location loc) {
        try {
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            as.customName(net.kyori.adventure.text.Component.text(DISPLAY_NAME,
                    net.kyori.adventure.text.format.NamedTextColor.WHITE));
            as.setCustomNameVisible(true);
            as.setGravity(false);
            as.setBasePlate(false);
            as.setArms(true);
            as.setInvulnerable(true);
            as.setPersistent(true);
            as.setCanPickupItems(false);
            // A player head reads as a face rather than a blank helmet.
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

    /** The currently spawned Wren body, or null if none / dead / world unloaded. MAIN thread only. */
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

    /** True if a live Wren body currently exists. */
    public boolean isSpawned() {
        return body() != null;
    }

    /**
     * Remove the current Wren body (and any stray marked Wren in loaded worlds, defensively). Used by
     * {@code spawn()} to enforce the singleton and by the "never present when the Watcher manifests"
     * tell (a Watcher beat relocates/despawns him). MAIN thread only. Never throws.
     */
    public void despawn() {
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    if (isWren(e)) {
                        removeBody(e);
                    }
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort
        }
        this.entityId = null;
    }

    /** Remove a single Wren entity, dismissing the Citizens NPC cleanly when it is one. */
    private void removeBody(Entity e) {
        if (e == null) return;
        // If Citizens backs this entity, ask Citizens to destroy the NPC so it isn't respawned.
        if (citizens != null && !destroyCitizensIfNpc(e)) {
            // not a Citizens NPC (or drift) → fall through to a plain entity removal
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

    /** Stamp the Wren marker (with an optional node hint) onto an entity's PDC. */
    public void tag(Entity e, String nodeHint) {
        if (e == null) return;
        try {
            e.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    nodeHint == null ? "" : nodeHint);
            // V5's dialogue rite uses one exact, story-versioned identity. Keep the historical
            // body marker only as a body-manager compatibility tag; it is never a solve surface.
            e.getPersistentDataContainer().set(v5NpcKey, PersistentDataType.STRING, "wren");
        } catch (Throwable ignored) {
            // some entity impls reject PDC — the fallback armor stand does not
        }
    }

    /** True if the entity carries the Wren marker. */
    public boolean isWren(Entity e) {
        if (e == null) return false;
        try {
            return e.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)
                    || "wren".equals(e.getPersistentDataContainer().get(
                    v5NpcKey, PersistentDataType.STRING));
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

    /**
     * Probe for Citizens2 exactly like {@link com.observance.watcher.beats.lib.Schematics} probes FAWE:
     * plugin-enabled check + {@link Class#forName} string lookups so this class's constant pool stays
     * free of any {@code net.citizensnpcs.*} type. Absent Citizens ⇒ false ⇒ the PDC fallback path.
     */
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
