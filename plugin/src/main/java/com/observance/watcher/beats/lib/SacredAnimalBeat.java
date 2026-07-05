package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

/**
 * MOBS — a sacred animal the herd watches (the Haunted Herd side-mystery), AND the slow, cosmetic
 * "pale herd" conversion that dresses it (design/ideas/herd-conversion.md). Two payload modes, ONE
 * beat class, kept strictly apart at the PDC level (INV-13 / DeathListener precision guard):
 *
 * <ul>
 *   <li>{@code mode:"single"} (the DEFAULT — unchanged M1 behavior) — tags the ONE nearby untagged
 *       animal as the Sacred Beast: {@code sacred_beast} PDC, persistent + silent + optionally
 *       glowing. Killing it is a tracked transgression; protecting it earns a quiet boon (handled
 *       elsewhere). Idempotent: no-ops if a Sacred Beast already exists nearby.</li>
 *   <li>{@code mode:"spread"} (the between-session herd-conversion pass, A12) — tags ONE FRESH
 *       nearby animal (not already carrying {@code sacred_beast}, {@code sacred_fork_arm}, OR
 *       {@code pale_cosmetic}) with {@code pale_cosmetic} ONLY: persistent, silent, faced to the
 *       canonical bearing, NEVER glowing. Idempotent against the payload's {@code pale_target}
 *       (counts existing {@code pale_cosmetic} near the anchor; no-ops once at target) and hard-
 *       capped at {@value #PALE_CAP} regardless of what the pacer asks for. Routed through the
 *       same never-witnessed-mutating discipline every other beat uses ({@code Reveal.isHidden} +
 *       a scheduled retry, mirroring {@link AbstractBeat#mutateWhenUnwitnessed} but for a live
 *       entity instead of a block) — if no unwitnessed candidate exists this pass, it skips and
 *       carries the deficit forward (the pacer's monotone target already accounts for this; a
 *       skipped spread just means the field is one below its target until a later pass catches
 *       up). This branch MUST NEVER tag {@code sacred_beast} or {@code sacred_fork_arm} — that
 *       precision split is LAW (design doc §2.3 / INV-13), not a style choice.</li>
 * </ul>
 *
 * <p>Payload (single, unchanged):
 * <pre>{@code { "match_type":"COW", "radius":12, "glow":true, "name":"" } }</pre>
 *
 * <p>Payload (spread):
 * <pre>{@code
 * { "mode":"spread", "match_type":"COW", "radius":16, "pale_target":3, "bearing":90.0 }
 * }</pre>
 */
public final class SacredAnimalBeat extends AbstractBeat {

    /** Absolute ceiling on the pale field (design doc §1.1/§2.6 — a hard cap, never forced to). */
    static final int PALE_CAP = 16;

    @Override public String name() { return "sacred_animal"; }
    @Override public String description() { return "Tags a nearby herd animal as the Sacred Beast (watched, persistent, marked), or spreads the cosmetic Pale field one head at a time."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (isSpread(req)) return canSpread(ctx, req);
        return findCandidate(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        if (isSpread(req)) return doSpread(ctx, req);
        return doSingle(ctx, req);
    }

    private static boolean isSpread(BeatRequest req) {
        return "spread".equalsIgnoreCase(req.payload().string("mode", "single"));
    }

    /* ------------------------------------------------------------------ */
    /*  mode:"single" — the ORIGINAL M1 Sacred Beast placement (unchanged) */
    /* ------------------------------------------------------------------ */

    private static BeatResult doSingle(BeatContext ctx, BeatRequest req) {
        LivingEntity animal = findCandidate(ctx, req);
        if (animal == null) return BeatResult.skipped("no-animal");
        BeatPayload p = req.payload();

        try {
            animal.getPersistentDataContainer().set(
                    key(ctx, "sacred_beast"), PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable t) {
            return BeatResult.failed("tag-error");
        }
        animal.setRemoveWhenFarAway(false);
        animal.setPersistent(true);
        animal.setSilent(true);
        if (p.bool("glow", false)) {
            try { animal.setGlowing(true); } catch (Throwable ignored) { }
        }
        String nm = p.string("name", null);
        if (nm != null && !nm.isBlank()) {
            animal.customName(net.kyori.adventure.text.Component.text(
                    nm.length() > 64 ? nm.substring(0, 64) : nm));
            animal.setCustomNameVisible(p.bool("name_visible", false));
        }
        return BeatResult.fired("sacred-tagged");
    }

    private static LivingEntity findCandidate(BeatContext ctx, BeatRequest req) {
        Location at = anchor(ctx, req);
        if (at == null || at.getWorld() == null) return null;
        if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) return null;
        BeatPayload p = req.payload();
        double radius = Math.max(2, Math.min(48, p.integer("radius", 12)));
        String matchType = p.string("match_type", null);

        List<Entity> near;
        try {
            near = at.getWorld().getNearbyEntities(at, radius, radius, radius).stream().toList();
        } catch (Throwable t) {
            return null;
        }
        LivingEntity best = null;
        for (Entity e : near) {
            if (!(e instanceof Animals animal)) continue;
            if (!animal.isValid() || animal.isDead()) continue;
            // skip if some animal nearby is already the Sacred Beast (idempotent-ish)
            try {
                if (animal.getPersistentDataContainer().has(
                        key(ctx, "sacred_beast"), PersistentDataType.BYTE)) {
                    return null; // already established
                }
            } catch (Throwable ignored) { }
            if (matchType != null && !matchType.isBlank()
                    && !animal.getType().name().equalsIgnoreCase(matchType.trim())) {
                continue;
            }
            if (best == null) best = animal;
        }
        return best;
    }

    /* ------------------------------------------------------------------ */
    /*  mode:"spread" — the between-session Pale-herd conversion (A12)     */
    /* ------------------------------------------------------------------ */

    /** Cheap precheck: a spread pass can fire iff we're under cap/target AND a fresh candidate exists. */
    private static boolean canSpread(BeatContext ctx, BeatRequest req) {
        Location at = anchor(ctx, req);
        if (at == null || at.getWorld() == null) return false;
        if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) return false;
        int target = clampTarget(req.payload());
        int existing = countPaleNear(ctx, at, radiusOf(req.payload()));
        if (existing >= target || existing >= PALE_CAP) return false; // idempotent: already at target/cap
        return findFreshCandidate(ctx, req, at) != null;
    }

    private static BeatResult doSpread(BeatContext ctx, BeatRequest req) {
        Location at = anchor(ctx, req);
        if (at == null || at.getWorld() == null) return BeatResult.skipped("no-anchor");
        if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
            return BeatResult.skipped("anchor-unloaded");
        }
        BeatPayload p = req.payload();
        double radius = radiusOf(p);

        // IDEMPOTENT: count what's already pale near the anchor; no-op if the pass's target (or the
        // hard cap) is already met. A re-run of the same pacer window never over-adds (monotone,
        // never re-adds — design doc §4.5 item 1 / §1.1's cap).
        int target = clampTarget(p);
        int existing = countPaleNear(ctx, at, radius);
        if (existing >= target || existing >= PALE_CAP) {
            return BeatResult.skipped("pale-at-target");
        }

        LivingEntity animal = findFreshCandidate(ctx, req, at);
        if (animal == null) {
            // No unwitnessed-eligible / untagged candidate this pass — skip and carry the deficit.
            // The pacer's monotone target is unaffected; a later pass tries again (design doc §2.2).
            return BeatResult.skipped("no-fresh-animal");
        }

        final java.util.UUID targetId = animal.getUniqueId();
        final float bearing = (float) p.number("bearing", 90.0); // default: FACT-15 canonical "east"
        final org.bukkit.World world = at.getWorld();

        // Never-witnessed-mutating discipline for a LIVE ENTITY (mirrors AbstractBeat#mutateWhenUnwitnessed,
        // which is block-only): retag/placement only happens once ctx.reveal().isHidden(location) is true
        // for the entity's current position, re-checked at each attempt (the entity may have moved, or a
        // player may have walked up). Gives up quietly (skip + carry the deficit) after the configured max
        // attempts — never a half-applied tag, never a mutation caught on camera.
        attemptSpreadTag(ctx, world, targetId, bearing, 0);
        return BeatResult.fired("pale-spread-queued");
    }

    private static void attemptSpreadTag(BeatContext ctx, org.bukkit.World world,
                                          java.util.UUID targetId, float bearing, int attempt) {
        Entity e = org.bukkit.Bukkit.getEntity(targetId);
        if (!(e instanceof LivingEntity animal) || !animal.isValid() || animal.isDead()) {
            return; // candidate vanished (died/unloaded) between enqueue and fire — skip silently
        }
        boolean hidden = ctx.safety().call("beat.herd.spread.check",
                () -> ctx.reveal().isHidden(animal.getLocation()), Boolean.TRUE);
        if (Boolean.TRUE.equals(hidden)) {
            ctx.safety().run("beat.herd.spread.tag", () -> applyPaleTag(ctx, animal, bearing));
            return;
        }
        int max = ctx.config().revealRetryMaxAttempts();
        if (attempt >= max) {
            return; // witnessed too long — abandon silently, deficit carried to the next pacer pass
        }
        long delay = ctx.config().revealRetryDelayTicks();
        ctx.scheduler().runLaterSafe("beat.herd.spread.retry", delay,
                () -> attemptSpreadTag(ctx, world, targetId, bearing, attempt + 1));
    }

    /**
     * The actual retag, applied ONLY once unwitnessed. Sets {@code pale_cosmetic} — and ONLY
     * {@code pale_cosmetic} — persistent + silent, faced to the canonical bearing. NEVER sets
     * {@code sacred_beast} or {@code sacred_fork_arm}, and NEVER calls {@code setGlowing(true)}
     * (INV-13 precision law: a pale is decoration, conduct-blind, and must never be mistaken for
     * the one tracked, glowing Sacred Beast).
     */
    private static void applyPaleTag(BeatContext ctx, LivingEntity animal, float bearing) {
        try {
            animal.getPersistentDataContainer().set(
                    key(ctx, "pale_cosmetic"), PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable t) {
            return; // tag failed — leave the animal untouched, no partial state
        }
        animal.setRemoveWhenFarAway(false);
        animal.setPersistent(true);
        animal.setSilent(true);
        // Faced to the canonical bearing (the FACT-15 visual — "the pale ones face the same compass
        // bearing"), same idiom NamedMobBeat uses to set spawn facing: mutate a Location's yaw, teleport.
        try {
            Location facing = animal.getLocation();
            facing.setYaw(bearing);
            animal.teleport(facing);
        } catch (Throwable ignored) { }
        // Deliberately NOT glowing, NOT sacred_beast, NOT sacred_fork_arm — the precision split is law.
    }

    /** A fresh candidate for the spread: an ANIMAL not already carrying ANY of the three tags
     *  (sacred_beast / sacred_fork_arm / pale_cosmetic), so the spread never re-tags an existing
     *  Sacred Beast or a prior Pale, and never risks a stale co-tag (INV-13). */
    private static LivingEntity findFreshCandidate(BeatContext ctx, BeatRequest req, Location at) {
        BeatPayload p = req.payload();
        double radius = radiusOf(p);
        String matchType = p.string("match_type", null);

        List<Entity> near;
        try {
            near = at.getWorld().getNearbyEntities(at, radius, radius, radius).stream().toList();
        } catch (Throwable t) {
            return null;
        }
        for (Entity e : near) {
            if (!(e instanceof Animals animal)) continue;
            if (!animal.isValid() || animal.isDead()) continue;
            if (matchType != null && !matchType.isBlank()
                    && !animal.getType().name().equalsIgnoreCase(matchType.trim())) {
                continue;
            }
            if (isAlreadyTagged(ctx, animal)) continue;
            // Only offer a candidate that COULD be tagged unwitnessed right now; if every nearby
            // untagged animal is currently witnessed, doSpread's retry loop will keep trying THIS
            // one rather than hunting for a different candidate each attempt (stable target).
            return animal;
        }
        return null;
    }

    /** True if this animal already carries sacred_beast, sacred_fork_arm, or pale_cosmetic. */
    private static boolean isAlreadyTagged(BeatContext ctx, LivingEntity animal) {
        try {
            var pdc = animal.getPersistentDataContainer();
            if (pdc.has(key(ctx, "sacred_beast"), PersistentDataType.BYTE)) return true;
            if (pdc.has(key(ctx, "sacred_fork_arm"), PersistentDataType.BYTE)) return true;
            if (pdc.has(key(ctx, "pale_cosmetic"), PersistentDataType.BYTE)) return true;
        } catch (Throwable ignored) { }
        return false;
    }

    /** Count animals near the anchor already carrying {@code pale_cosmetic} (idempotency read). */
    private static int countPaleNear(BeatContext ctx, Location at, double radius) {
        List<Entity> near;
        try {
            near = at.getWorld().getNearbyEntities(at, radius, radius, radius).stream().toList();
        } catch (Throwable t) {
            return 0;
        }
        int count = 0;
        for (Entity e : near) {
            if (!(e instanceof LivingEntity le)) continue;
            try {
                if (le.getPersistentDataContainer().has(key(ctx, "pale_cosmetic"), PersistentDataType.BYTE)) {
                    count++;
                }
            } catch (Throwable ignored) { }
        }
        return count;
    }

    private static double radiusOf(BeatPayload p) {
        return Math.max(2, Math.min(64, p.integer("radius", 16)));
    }

    /** The pass's target pale count, clamped to the hard cap regardless of what the pacer asks for. */
    private static int clampTarget(BeatPayload p) {
        int asked = p.integer("pale_target", 1);
        return Math.max(0, Math.min(PALE_CAP, asked));
    }
}
