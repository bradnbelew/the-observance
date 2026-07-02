package com.observance.watcher.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * THE SURFACE TOWNSFOLK — the five ordinary people of the surface town (Aro, Wenna, Coll, Dob, Old
 * Pell). This manager OWNS their in-world bodies exactly the way {@link WrenNpc} owns Wren's: it spawns
 * each one (Citizens2 player-skinned NPC when Citizens is present + enabled, else a PDC-tagged ARMOR
 * STAND fallback) and stamps the {@code observance:townsfolk_npc} PersistentDataContainer marker whose
 * STRING value is the townsperson's id ({@code aro|wenna|coll|dob|old-pell}). {@link
 * com.observance.watcher.signal.listener.TownsfolkNpcListener} then recognises a right-click through the
 * ordinary Bukkit interact event and reads the id back off the marker (Path-A self-containment — never
 * the Citizens API), so it knows which townsperson was clicked.
 *
 * <p><b>Register note.</b> The townsfolk are SURFACE NPCs in an ORDINARY human register — explicitly
 * NOT the sacred Watcher voice (that is SET B, {@code voice.ts}, protected by INV-1). Their dialogue is
 * SET A, authored verbatim in {@code discord/src/voice.archive.ts} {@code npcLines}, and the listener
 * speaks it immediately in-world with no showrunner round-trip. This manager only owns the bodies; the
 * lines live in the listener's embedded SET-A table (copied verbatim from that source of truth).
 *
 * <p><b>Group-scoped: at most ONE of each townsperson.</b> A spawn call for a given id first removes any
 * existing marked body of that id in the loaded worlds, then creates a fresh one — a re-spawn relocates
 * rather than clones. {@code spawnAll} places all five in a short row; {@code spawnOne} places a single
 * named one.
 *
 * <p>All world mutation here is MAIN thread only (Bukkit contract). Every entry point is defensive
 * (null / world-unloaded / entity-dead guarded, {@code try/catch Throwable}) and never throws into a
 * caller — additive + quirk-safe, mirroring {@link WrenNpc}.
 */
public final class TownsfolkNpc {

    /** PDC sub-key marking an entity as a townsperson (value = the townsperson id). */
    public static final String PDC_TOWNSFOLK = "townsfolk_npc";

    /** A townsperson: the marker id (lowercase slug) + the display name shown above the body. */
    public record Townsperson(String id, String displayName) {}

    /**
     * The five surface townsfolk, in the order {@code voice.archive.ts} lists them. The ids are the
     * {@code npc_key} slugs from {@code design/content/npc-dialogue.md} (also the prefix of every SET-A
     * key, e.g. {@code aro.greet.neutral}); {@code old-pell} keeps its hyphen to match the key prefix
     * {@code old-pell.*}. The display names are ordinary human names (surface register), not the
     * Watcher's.
     */
    public static final List<Townsperson> TOWNSFOLK = List.of(
            new Townsperson("aro", "Aro"),
            new Townsperson("wenna", "Wenna"),
            new Townsperson("coll", "Coll"),
            new Townsperson("dob", "Dob"),
            new Townsperson("old-pell", "Old Pell"));

    private final NamespacedKey markerKey;
    private final boolean citizensAvailable;
    private final CitizensBridge citizens;   // null unless Citizens is present

    public TownsfolkNpc(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.markerKey = new NamespacedKey(ns, PDC_TOWNSFOLK);
        this.citizensAvailable = probeCitizens();
        this.citizens = citizensAvailable ? new CitizensBridge() : null;
    }

    /* ------------------------------------------------------------------ */
    /*  Lookups                                                            */
    /* ------------------------------------------------------------------ */

    /** Resolve a townsperson by id (case-insensitive), or null if it is not one of the five. */
    public static Townsperson byId(String id) {
        if (id == null) return null;
        String want = id.trim().toLowerCase(Locale.ROOT);
        for (Townsperson t : TOWNSFOLK) {
            if (t.id().equals(want)) return t;
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Spawn / placement                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Spawn (or relocate) all five townsfolk in a short east-west row starting at {@code loc}, each
     * spaced two blocks apart. Returns the number placed (0–5). MAIN thread only. Never throws.
     */
    public int spawnAll(Location loc) {
        if (loc == null || loc.getWorld() == null) return 0;
        int placed = 0;
        for (int i = 0; i < TOWNSFOLK.size(); i++) {
            Location at = loc.clone().add(i * 2.0, 0, 0);
            if (spawnOne(TOWNSFOLK.get(i).id(), at) != null) placed++;
        }
        return placed;
    }

    /**
     * Spawn (or relocate) the one townsperson with {@code id} at {@code loc}. Removes any pre-existing
     * marked body of that id first (singleton per id), then places a Citizens NPC when available, else a
     * PDC-tagged armor stand. Returns the spawned entity, or {@code null} on failure / unknown id. MAIN
     * thread only.
     */
    public Entity spawnOne(String id, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        Townsperson who = byId(id);
        if (who == null) return null;

        // Enforce the per-id singleton: clear any previous body of this townsperson.
        despawnOne(who.id());

        Entity body = null;
        if (citizens != null) {
            body = citizens.spawnPlayerNpc(who.displayName(), loc);
        }
        if (body == null) {
            body = spawnFallback(who.displayName(), loc);
        }
        if (body == null) return null;

        tag(body, who.id());
        return body;
    }

    /**
     * The Citizens-absent body: a stable armor stand wearing a player head, named for the townsperson,
     * with no gravity and no base plate so it stands like a person. Kept persistent + invulnerable so it
     * isn't griefed. Mirrors {@link WrenNpc#spawnFallback}.
     */
    private Entity spawnFallback(String displayName, Location loc) {
        try {
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            as.customName(net.kyori.adventure.text.Component.text(displayName,
                    net.kyori.adventure.text.format.NamedTextColor.WHITE));
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

    /** True if a live body exists for the townsperson {@code id}. MAIN thread only. */
    public boolean isSpawned(String id) {
        Townsperson who = byId(id);
        if (who == null) return false;
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    if (who.id().equals(idOf(e)) && e.isValid()) return true;
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort
        }
        return false;
    }

    /** Count how many of the five townsfolk currently have a live body. */
    public int spawnedCount() {
        int n = 0;
        for (Townsperson t : TOWNSFOLK) {
            if (isSpawned(t.id())) n++;
        }
        return n;
    }

    /**
     * Remove ALL marked townsfolk bodies in the loaded worlds (any id). Returns the number removed.
     * MAIN thread only. Never throws.
     */
    public int despawnAll() {
        List<Entity> doomed = new ArrayList<>();
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    if (idOf(e) != null) doomed.add(e);
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort
        }
        int removed = 0;
        for (Entity e : doomed) {
            removeBody(e);
            removed++;
        }
        return removed;
    }

    /** Remove the current body (or stray bodies) of the townsperson {@code id}. Returns count removed. */
    public int despawnOne(String id) {
        Townsperson who = byId(id);
        if (who == null) return 0;
        List<Entity> doomed = new ArrayList<>();
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity e : w.getEntities()) {
                    if (who.id().equals(idOf(e))) doomed.add(e);
                }
            }
        } catch (Throwable ignored) {
            // world iteration quirk — best effort
        }
        int removed = 0;
        for (Entity e : doomed) {
            removeBody(e);
            removed++;
        }
        return removed;
    }

    /** Remove a single townsperson entity, dismissing the Citizens NPC cleanly when it is one. */
    private void removeBody(Entity e) {
        if (e == null) return;
        if (citizens != null) {
            destroyCitizensIfNpc(e);   // no-op / false when not a Citizens NPC
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

    /** Stamp the townsfolk marker (value = the townsperson id) onto an entity's PDC. */
    public void tag(Entity e, String id) {
        if (e == null || id == null) return;
        try {
            e.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, id);
        } catch (Throwable ignored) {
            // some entity impls reject PDC — the fallback armor stand does not
        }
    }

    /** The townsperson id stamped on an entity, or null if it carries no townsfolk marker. */
    public String idOf(Entity e) {
        if (e == null) return null;
        try {
            var pdc = e.getPersistentDataContainer();
            if (!pdc.has(markerKey, PersistentDataType.STRING)) return null;
            String v = pdc.get(markerKey, PersistentDataType.STRING);
            return v == null ? null : v.trim().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean citizensAvailable() { return citizensAvailable; }

    /** Human-readable NPC backend, for the admin status line. */
    public String backend() { return citizensAvailable ? "Citizens" : "armor-stand fallback"; }

    /* ------------------------------------------------------------------ */
    /*  Citizens capability probe (string-based, no hard import)          */
    /* ------------------------------------------------------------------ */

    /** Probe for Citizens2 exactly like {@link WrenNpc} does (string lookups, no hard import). */
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

    /** If {@code e} is the backing entity of a Citizens NPC, destroy that NPC. All reflective. */
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
