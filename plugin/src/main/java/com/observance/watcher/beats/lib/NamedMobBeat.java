package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * MOBS — a named mob appears that won't despawn, is silent, and stares at / follows ONE player. The
 * canonical "the watcher". Spawned a short distance away, out of line of sight (discovered, not
 * witnessed appearing), on valid ground (no floaters/suffocation). Tagged in PDC so it's recognized
 * as a beat entity (for cleanup + anti-grief), set persistent + silent + invulnerable-by-default so
 * players can't trivially kill/weaponize it. It targets the player but is configured not to attack.
 *
 * <p>Payload:
 * <pre>{@code
 * { "entity":"ZOMBIE", "name":"...", "distance":12, "silent":true, "no_ai_drift":true,
 *   "invulnerable":true, "glowing":false, "despawn_seconds":0 }
 * }</pre>
 */
public final class NamedMobBeat extends AbstractBeat {

    @Override public String name() { return "named_mob"; }
    @Override public String description() { return "A named, silent, persistent mob appears nearby and watches one player."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        EntityType type = entityType(req.payload().string("entity", "ZOMBIE"));
        if (type == null || !type.isSpawnable() || !type.isAlive()) return false;
        return findSpawn(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        EntityType type = entityType(p.string("entity", "ZOMBIE"));
        if (type == null || !type.isSpawnable() || !type.isAlive()) return BeatResult.skipped("bad-entity");

        Location spawn = findSpawn(ctx, req);
        if (spawn == null) return BeatResult.skipped("no-spawn-spot");

        final boolean silent = p.bool("silent", true);
        final boolean invulnerable = p.bool("invulnerable", true);
        final boolean glowing = p.bool("glowing", false);
        final boolean noDrift = p.bool("no_ai_drift", true);
        final String displayName = p.string("name", null);
        final long despawnTicks = Math.max(0L, p.integer("despawn_seconds", 0) * 20L);

        Entity spawned;
        try {
            spawned = spawn.getWorld().spawnEntity(spawn, type);
        } catch (Throwable t) {
            return BeatResult.failed("spawn-error");
        }
        if (!(spawned instanceof LivingEntity living)) {
            try { spawned.remove(); } catch (Throwable ignored) { }
            return BeatResult.failed("not-living");
        }

        // Configure the apparition.
        living.setRemoveWhenFarAway(false);
        living.setPersistent(true);
        living.setSilent(silent);
        living.setInvulnerable(invulnerable);
        living.setCanPickupItems(false);
        try { living.setGlowing(glowing); } catch (Throwable ignored) { }
        if (displayName != null && !displayName.isBlank()) {
            living.customName(Component.text(displayName.length() > 64 ? displayName.substring(0, 64) : displayName));
            living.setCustomNameVisible(p.bool("name_visible", false));
        }
        // Tag as a beat entity for cleanup + anti-grief recognition.
        try {
            living.getPersistentDataContainer().set(
                    key(ctx, "beat_entity"), PersistentDataType.STRING, req.beatId());
            living.getPersistentDataContainer().set(
                    key(ctx, "beat_owner"), PersistentDataType.STRING, req.targetUuid().toString());
        } catch (Throwable ignored) { }

        if (noDrift && living instanceof Mob mob) {
            // Stare without wandering: face the player, set as target but the invuln+silent + later
            // cleanup keep it non-threatening. A watcher, not an attacker.
            try {
                mob.setTarget(req.targetPlayer());
            } catch (Throwable ignored) { }
            try { mob.setAware(true); } catch (Throwable ignored) { }
        }

        if (despawnTicks > 0) {
            final java.util.UUID eid = living.getUniqueId();
            ctx.scheduler().runLaterSafe("beat.mob.despawn", despawnTicks, () -> {
                Entity e = org.bukkit.Bukkit.getEntity(eid);
                if (e != null && e.isValid()) {
                    boolean hidden = ctx.safety().call("beat.mob.despawn.check",
                            () -> ctx.reveal().isHidden(e.getLocation().getBlock()), Boolean.TRUE);
                    if (Boolean.TRUE.equals(hidden)) e.remove();
                    else {
                        // try again shortly so it vanishes unwitnessed
                        ctx.scheduler().runLaterSafe("beat.mob.despawn.retry",
                                ctx.config().revealRetryDelayTicks(), () -> {
                                    Entity e2 = org.bukkit.Bukkit.getEntity(eid);
                                    if (e2 != null && e2.isValid()) e2.remove();
                                });
                    }
                }
            });
        }
        return BeatResult.fired("mob-spawned");
    }

    /** Find a valid standing spot ~distance blocks from the player, out of LoS. MAIN thread. */
    private static Location findSpawn(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return null;
        var pl = req.targetPlayer();
        Location eye = pl.getEyeLocation();
        int distance = Math.max(4, Math.min(48, req.payload().integer("distance", 12)));
        // Try a ring of candidate angles; pick the first that is on valid ground AND hidden.
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            double dx = Math.cos(rad) * distance;
            double dz = Math.sin(rad) * distance;
            Location cand = pl.getLocation().clone().add(dx, 0, dz);
            org.bukkit.block.Block surface = Placement.findSurfaceSpot(cand.clone().add(0, 2, 0), 6);
            if (surface == null) continue;
            Location spot = surface.getLocation().add(0.5, 1.0, 0.5);
            // headroom check: the cell above the floor and one more must be passable
            if (!surface.getRelative(org.bukkit.block.BlockFace.UP).isPassable()) continue;
            if (!surface.getRelative(0, 2, 0).isPassable()) continue;
            boolean hidden = ctx.safety().call("beat.mob.spawn.check",
                    () -> ctx.reveal().isHidden(spot), Boolean.TRUE);
            if (Boolean.TRUE.equals(hidden)) {
                // face the player
                org.bukkit.util.Vector dir = pl.getLocation().toVector().subtract(spot.toVector());
                spot.setDirection(dir);
                return spot;
            }
        }
        return null;
    }

    private static EntityType entityType(String name) {
        if (name == null || name.isBlank()) return EntityType.ZOMBIE;
        try {
            return EntityType.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Throwable t) {
            return EntityType.ZOMBIE;
        }
    }
}
