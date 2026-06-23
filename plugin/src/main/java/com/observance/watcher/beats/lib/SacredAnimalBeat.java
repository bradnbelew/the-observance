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

/**
 * MOBS — a sacred animal the herd watches (the Haunted Herd side-mystery). Tags one nearby animal as
 * the Sacred Beast: marks it in PDC, makes it persistent + silent + (optionally) faintly glowing so
 * it reads as "different". Killing a tagged Sacred Beast is a tracked transgression (a companion
 * listener records it); protecting it earns a quiet boon — both handled elsewhere. This beat just
 * establishes the tag, idempotently (won't re-tag if one already exists nearby).
 *
 * <p>Payload:
 * <pre>{@code { "match_type":"COW", "radius":12, "glow":true, "name":"" } }</pre>
 */
public final class SacredAnimalBeat extends AbstractBeat {

    @Override public String name() { return "sacred_animal"; }
    @Override public String description() { return "Tags a nearby herd animal as the Sacred Beast (watched, persistent, marked)."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        return findCandidate(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
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
}
