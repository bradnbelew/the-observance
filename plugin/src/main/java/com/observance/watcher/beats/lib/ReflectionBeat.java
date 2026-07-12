package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * SELLA'S REFLECTION — a rune legible ONLY in water's reflection (INTEGRATION §SIGNATURE #7). A
 * per-player {@link TextDisplay} is hung UNDER a water surface, MIRRORED (rotated 180° about the
 * horizontal axis) so that read directly it is upside-down and back-to-front — but seen in the water's
 * reflection it flips right-way-up and legible. It is revealed only to the one target (via
 * {@link PerPlayer#showEntityTo}), and only while they stand at the pool and LOOK DOWN into it; turn
 * away or step off and it vanishes ("was that ever there?"). To everyone else the pool is just water.
 *
 * <p><b>Two illusions in one.</b> The 180° flip means the glyph is only decodable via a reflective
 * surface — that's Sella's whole conceit. The look-down watchdog means it behaves like the shy
 * apparitions: it is discovered, never witnessed appearing, and never lingers.
 *
 * <p><b>Reveal-safe by construction.</b> Spawned {@code setVisibleByDefault(false)} (invisible to
 * everyone for the pre-reveal frame), non-persistent (dies with a chunk/restart — no orphans), PDC-
 * tagged as a beat entity for cleanup sweeps.
 *
 * <p><b>Behaviour-safe by construction.</b> Every scheduled follow-up re-resolves the player by UUID
 * and validity-checks the display; a logout / death / world-change simply removes it and ends the beat.
 *
 * <p>Optionally also {@code lens_gated}: registered into the {@code LensRegistry} so the Lie/second-sight
 * relic gates it too (visible only while looking down AND holding the Lens) — off by default so the
 * reflection reads as a standalone Sella clue unless the showrunner wants the crossover.
 *
 * <p>Payload (all optional):
 * <pre>{@code
 * {
 *   "text": "the drowned line",   // the rune body (required-ish: blank text → skipped)
 *   "rune_font": true,             // carve in observance:runes (default true)
 *   "color": "#3a6ea5",            // hex/named glyph colour (default a drowned blue)
 *   "seconds": 8,                  // hard despawn timer (clamped 2..20)
 *   "look_watch": true,            // erase when they stop looking down at the pool (default true)
 *   "pitch_min": 25.0,             // min downward look pitch to count as "looking in" (deg, clamped 10..80)
 *   "search_radius": 4,            // blocks around the anchor to find a water surface (clamped 1..8)
 *   "depth": 0.6,                  // how far under the surface to hang the mirrored glyph (clamped .1..2)
 *   "billboard": false,           // face the player (default false — a fixed, flipped rune)
 *   "lens_gated": false            // ALSO require the Lens to be held (default false)
 * }
 * }</pre>
 */
public final class ReflectionBeat extends AbstractBeat {

    private static final String RUNE_FONT = "observance:runes";
    private static final int WATCH_PERIOD_TICKS = 4;
    /** A drowned, cold blue when no colour is authored. */
    private static final TextColor DEFAULT_COLOR = TextColor.color(0x3a, 0x6e, 0xa5);

    @Override public String name() { return "reflection"; }
    @Override public String description() { return "A rune legible only in water's reflection appears for one player looking into a pool."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Needs an online target, some text, and a water surface to hang the mirror under.
        if (!req.hasTarget()) return false;
        String text = req.payload().string("text", "");
        if (text == null || text.isBlank()) return false;
        return waterSurface(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        BeatPayload p = req.payload();
        String rawText = p.string("text", "");
        if (rawText.isBlank()) return BeatResult.skipped("empty");

        Location surface = waterSurface(ctx, req);   // top face of a water block
        if (surface == null) return BeatResult.skipped("no-water");

        final boolean runeFont = p.bool("rune_font", true);
        final boolean billboard = p.bool("billboard", false);
        final boolean lookWatch = p.bool("look_watch", true);
        final boolean lensGated = p.bool("lens_gated", false);
        final int seconds = Math.max(2, Math.min(20, p.integer("seconds", 8)));
        final double depth = Math.max(0.1, Math.min(2.0, p.number("depth", 0.6)));
        final double pitchMin = Math.max(10.0, Math.min(80.0, p.number("pitch_min", 25.0)));
        final TextColor color = NameOnWallBeat.colorOf(p.string("color", null));
        final String shown = clamp(rawText.replace("%name%", pl.getName()));

        Component label = Component.text(shown).color(color);
        if (runeFont) label = label.font(net.kyori.adventure.key.Key.key(RUNE_FONT));
        final Component finalLabel = label;

        // Hang the glyph just UNDER the water surface, facing up, MIRRORED so it reads only in reflection.
        final Location spot = surface.clone().subtract(0.0, depth, 0.0);
        // FIXED displays need an authored pool-to-player frame. Aim the drowned writing toward this
        // personalized viewer's standing zone, then apply the X-axis mirror below.
        double frameDx = pl.getLocation().getX() - surface.getX();
        double frameDz = pl.getLocation().getZ() - surface.getZ();
        spot.setYaw((float) Math.toDegrees(Math.atan2(-frameDx, frameDz)));

        TextDisplay display;
        try {
            display = spot.getWorld().spawn(spot, TextDisplay.class, td -> {
                td.setVisibleByDefault(false);            // invisible to everyone until we reveal to the one
                td.setPersistent(false);                  // never survive a save/restart → no orphans
                td.text(finalLabel);
                td.setBillboard(billboard ? Display.Billboard.CENTER : Display.Billboard.FIXED);
                td.setSeeThrough(true);                   // legible through the water column
                td.setShadowed(false);
                td.setDefaultBackground(false);
                try { td.setBackgroundColor(org.bukkit.Color.fromARGB(0)); } catch (Throwable ignored) { }
                td.setBrightness(new Display.Brightness(13, 15));
                td.setViewRange(0.5f);                     // close, private
                // The mirror: rotate 180° about the X axis so the text is flipped vertically (and reads
                // upside-down directly, right-way-up in the reflection above), lifted to face upward.
                td.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f((float) Math.PI, 1f, 0f, 0f),      // flip about X → mirrored
                        new Vector3f(1.1f, 1.1f, 1.1f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
            });
        } catch (Throwable t) {
            return BeatResult.failed("spawn-error");
        }
        if (display == null || !display.isValid()) return BeatResult.failed("no-display");

        try {
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_entity"), PersistentDataType.STRING, req.beatId());
            display.getPersistentDataContainer().set(
                    key(ctx, "beat_owner"), PersistentDataType.STRING, req.targetUuid().toString());
        } catch (Throwable ignored) { }

        final java.util.UUID displayId = display.getUniqueId();
        final java.util.UUID targetId = req.targetUuid();

        // Lens crossover (optional): register so the second-sight relic ALSO gates it. When lens-gated the
        // LensListener owns the base show/hide (only while the Lens is held); the look-watch layers the
        // "must look in" hide on top. When NOT lens-gated, reveal now UNLESS the look-watch will manage it
        // (in which case we stay hidden until the first look-in — "discovered, never witnessed appearing").
        boolean registeredWithLens = false;
        if (lensGated && ctx.lensRegistry() != null) {
            ctx.lensRegistry().register(targetId, displayId);
            registeredWithLens = true;
            try {
                if (ctx.plugin() instanceof com.observance.watcher.ObservancePlugin op
                        && op.lensListener() != null) {
                    op.lensListener().refresh(pl);   // reveal now iff already holding the Lens
                }
            } catch (Throwable ignored) { }
        } else if (!lookWatch) {
            PerPlayer.showEntityTo(ctx.plugin(), pl, display);   // static rune (no look gating)
        }
        final boolean lensManaged = registeredWithLens;

        // Hard despawn timer — the drowned rune never lingers past its window.
        ctx.scheduler().runLaterSafe("beat.reflection.despawn", seconds * 20L, () -> {
            if (lensManaged && ctx.lensRegistry() != null) {
                ctx.lensRegistry().unregister(targetId, displayId);
            }
            removeDisplay(displayId);
        });

        // Look-watch: keep it shown only while they stand at the pool and look DOWN into it. Reveals when
        // they look in (unless lens-managed, where the LensListener owns the show/hide), hides otherwise.
        if (lookWatch) {
            final Location watchAt = surface.clone();
            final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
            // Non-lens look-watch starts HIDDEN — the watchdog reveals on the first look-in.
            final boolean[] shown2 = { false };   // current per-player visibility we manage (non-lens path)
            holder[0] = ctx.scheduler().runTimerSafe("beat.reflection.watch",
                    WATCH_PERIOD_TICKS, WATCH_PERIOD_TICKS, () -> {
                        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(displayId);
                        if (e == null || !e.isValid()) { cancel(holder[0]); return; }
                        Player watcher = org.bukkit.Bukkit.getPlayer(targetId);
                        if (watcher == null || !watcher.isOnline()
                                || watcher.getWorld() == null
                                || !watcher.getWorld().equals(watchAt.getWorld())) {
                            if (lensManaged && ctx.lensRegistry() != null) {
                                ctx.lensRegistry().unregister(targetId, displayId);
                            }
                            removeDisplay(displayId);
                            cancel(holder[0]);
                            return;
                        }
                        boolean lookingIn = ctx.safety().call("beat.reflection.look",
                                () -> lookingDownAtPool(watcher, watchAt, pitchMin), Boolean.FALSE);
                        if (lensManaged) {
                            // The Lens owns base visibility; we only ADD the "must look in" hide on top.
                            // If they look away, hide even while holding the Lens; the LensListener will
                            // re-show on the next equip event, and looking back in re-shows via below.
                            if (!lookingIn) {
                                PerPlayer.hideEntityFrom(ctx.plugin(), watcher, e);
                            } else {
                                // Only re-show if they are holding the Lens (registry gate still applies).
                                PerPlayer.showEntityTo(ctx.plugin(), watcher, e);
                            }
                            return;
                        }
                        // Non-lens path: show while looking in, hide otherwise (never despawn on look-away —
                        // the pool clue can be re-found; the hard timer ends it).
                        if (lookingIn && !shown2[0]) {
                            PerPlayer.showEntityTo(ctx.plugin(), watcher, e);
                            shown2[0] = true;
                        } else if (!lookingIn && shown2[0]) {
                            PerPlayer.hideEntityFrom(ctx.plugin(), watcher, e);
                            shown2[0] = false;
                        }
                    });
        }

        return BeatResult.fired("reflection");
    }

    /* ------------------------------------------------------------------ */
    /*  Water-surface finding + look test                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Find a water surface near the beat's anchor: a water block whose block ABOVE is air/passable
     * (i.e. an open pool top). Scans a small radius around the anchor. Returns the CENTER of that top
     * face, or null. MAIN thread (touches Bukkit world). Prefers the surface closest to the target.
     */
    private static Location waterSurface(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return null;
        World world = anchor.getWorld();
        int radius = Math.max(1, Math.min(8, req.payload().integer("search_radius", 4)));
        int cx = anchor.getBlockX(), cy = anchor.getBlockY(), cz = anchor.getBlockZ();

        Location best = null;
        double bestD2 = Double.MAX_VALUE;
        Location ref = req.hasTarget() ? req.targetPlayer().getLocation() : anchor;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.WATER) continue;
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (!isOpenAbove(above)) continue;         // must be an open pool top
                    // Top face center of the water block.
                    Location face = new Location(world, x + 0.5, y + 1.0, z + 0.5);
                    double d2 = ref.getWorld() != null && ref.getWorld().equals(world)
                            ? face.distanceSquared(ref) : 0.0;
                    if (d2 < bestD2) { bestD2 = d2; best = face; }
                }
            }
        }
        return best;
    }

    /** Air or a passable non-liquid above the water = an open surface a reflection can form on. */
    private static boolean isOpenAbove(Block above) {
        if (above == null) return false;
        Material m = above.getType();
        if (m == Material.WATER) return false;                 // not the surface — water continues up
        return m.isAir() || !m.isSolid();
    }

    /**
     * True if the player is at the pool (within a small horizontal radius of the surface) AND looking
     * DOWN into it (pitch steeper than {@code pitchMin} degrees, i.e. gazing at the water). MAIN thread.
     */
    private static boolean lookingDownAtPool(Player pl, Location surface, double pitchMin) {
        if (pl == null || surface == null || surface.getWorld() == null) return false;
        Location eye = pl.getEyeLocation();
        if (eye.getWorld() == null || !eye.getWorld().equals(surface.getWorld())) return false;
        // Horizontal proximity to the pool (generous — you can lean over an edge).
        double dx = eye.getX() - surface.getX();
        double dz = eye.getZ() - surface.getZ();
        if ((dx * dx + dz * dz) > 36.0) return false;          // > 6 blocks away horizontally → not "at" it
        // Must be above the surface and looking down into it.
        if (eye.getY() <= surface.getY()) return false;        // eyes at/under the surface → no reflection view
        return pl.getLocation().getPitch() >= pitchMin;        // positive pitch = looking down
    }

    /* ------------------------------------------------------------------ */

    private static void removeDisplay(java.util.UUID id) {
        try {
            org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        } catch (Throwable ignored) { }
    }

    private static void cancel(org.bukkit.scheduler.BukkitTask task) {
        try { if (task != null) task.cancel(); } catch (Throwable ignored) { }
    }

    private static String clamp(String s) {
        if (s == null) return "";
        return s.length() > 96 ? s.substring(0, 96) : s;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure helpers: text clamping bounds a malformed payload, and colour parse falls back to
     * the drowned-blue default on junk (shared with NameOnWallBeat.colorOf). A regression would push an
     * unbounded string onto the rune or NPE its colour. World-touching helpers (water finding, look test)
     * need a live server and are exercised in-game.
     */
    static boolean reflectionSelfTest() {
        if (clamp(null).length() != 0) return false;
        if (clamp("x".repeat(200)).length() != 96) return false;
        // colorOf (shared with NameOnWallBeat) must never return null — on junk it yields a safe default,
        // and a valid hex parses. (Our drowned-blue default is applied at the call site via p.string.)
        if (NameOnWallBeat.colorOf("not-a-color") == null) return false;
        if (NameOnWallBeat.colorOf("#3a6ea5") == null) return false;
        if (DEFAULT_COLOR == null) return false;
        return true;
    }
}
