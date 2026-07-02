package com.observance.watcher.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;

/**
 * The Citizens2 driver — reached ENTIRELY by reflection, so this class imports NO
 * {@code net.citizensnpcs.*} type and adds NO compile-time dependency on Citizens. That matters for
 * two reasons: (1) the plugin must load and run on a server with no Citizens installed (the
 * "never hard-import" mandate — D4 / plugin.yml note), and (2) the build is self-contained (Citizens
 * is not a declared dependency, so we could not compile against it even if we wanted to). This is the
 * same isolation contract as {@link com.observance.watcher.beats.lib.FaweSchematicPaster}, taken one
 * step stricter: not even a {@code compileOnly} type reference.
 *
 * <p>Instantiated lazily by {@link WrenNpc} ONLY after the string-based probe confirms Citizens is
 * enabled and its API classes resolve. Every method catches {@code Throwable} and degrades to a
 * {@code null}/no-op — a missing class, a method-signature drift, or a spawn error becomes a silent
 * fall-through to the PDC-fallback NPC, never a crash.
 *
 * <p>Wren "passes as a human" via a Citizens PLAYER-type NPC with its nameplate hidden. We return the
 * NPC's backing Bukkit {@link Entity} so the caller can stamp the same {@code observance:wren_npc}
 * PersistentDataContainer marker the fallback uses — {@link WrenNpcListener} then recognises the click
 * through the ordinary Bukkit interact event, never the Citizens API (Path-A self-containment).
 */
final class CitizensBridge {

    /**
     * Create + spawn the single group-scoped Wren as a player-skinned Citizens NPC at {@code loc},
     * returning its backing Bukkit entity (or {@code null} on any failure). MAIN thread only.
     */
    Entity spawnPlayerNpc(String name, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        try {
            // registry = CitizensAPI.getNPCRegistry()
            Class<?> apiCls = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = apiCls.getMethod("getNPCRegistry").invoke(null);
            if (registry == null) return null;

            // npc = registry.createNPC(EntityType.PLAYER, name)
            Method createNpc = registry.getClass()
                    .getMethod("createNPC", EntityType.class, String.class);
            Object npc = createNpc.invoke(registry,
                    EntityType.PLAYER, (name == null || name.isBlank()) ? "Wren" : name);
            if (npc == null) return null;

            hideNameplate(npc);

            // npc.spawn(loc)  — Citizens has both spawn(Location) and spawn(Location, SpawnReason);
            // prefer the one-arg form, which every 2.0.x line exposes.
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // return npc.getEntity()
            Object ent = npc.getClass().getMethod("getEntity").invoke(npc);
            return (ent instanceof Entity) ? (Entity) ent : null;
        } catch (Throwable t) {
            return null;   // any drift → let WrenNpc fall back to the PDC armor-stand
        }
    }

    /**
     * Best-effort hide of the Citizens hologram nameplate so Wren reads as a peer, not a signposted
     * NPC. Uses {@code npc.data().setPersistent("nameplate-visible", false)} — the string data key is
     * stable across Citizens versions even as the typed Metadata enum has churned. Purely cosmetic:
     * any failure is swallowed.
     */
    private void hideNameplate(Object npc) {
        try {
            Object data = npc.getClass().getMethod("data").invoke(npc);
            Method setPersistent = data.getClass()
                    .getMethod("setPersistent", String.class, Object.class);
            setPersistent.invoke(data, "nameplate-visible", false);
        } catch (Throwable ignored) {
            // cosmetic only
        }
    }
}
