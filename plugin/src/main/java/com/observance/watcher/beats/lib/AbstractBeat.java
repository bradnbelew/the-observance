package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.Beat;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatRequest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared base for library beats. Provides:
 * <ul>
 *   <li>idempotency tracking keyed on beat id (persistent effects never double-apply in-process),</li>
 *   <li>safe material/enum parsing from the payload (null-safe, validated),</li>
 *   <li>a witness-safe block-mutation helper (reveal discipline + optional retry),</li>
 *   <li>target/anchor resolution helpers.</li>
 * </ul>
 *
 * <p>All world-touching helpers are MAIN-thread only (the engine calls {@code enact} on main).
 * Subclasses implement {@link #doEnact}; this base wraps the idempotency guard around it.
 */
public abstract class AbstractBeat implements Beat {

    /** Beat ids already applied this process run (in-memory idempotency; DB status is the durable guard). */
    private final Set<String> applied = ConcurrentHashMap.newKeySet();

    /** Subclasses do the real work here. Base handles the idempotency guard. */
    protected abstract com.observance.watcher.beats.BeatResult doEnact(BeatContext ctx, BeatRequest req);

    @Override
    public final com.observance.watcher.beats.BeatResult enact(BeatContext ctx, BeatRequest req) {
        if (ctx == null || req == null) {
            return com.observance.watcher.beats.BeatResult.failed("null-args");
        }
        // Idempotency: a persistent beat with a real id never re-applies within this process.
        String id = req.beatId();
        boolean trackable = id != null && !id.isBlank() && !"synthetic".equals(id);
        if (trackable && applied.contains(id)) {
            return com.observance.watcher.beats.BeatResult.skipped("already-applied");
        }
        com.observance.watcher.beats.BeatResult r = doEnact(ctx, req);
        if (trackable && r != null
                && r.kind() == com.observance.watcher.beats.BeatResult.Kind.FIRED) {
            applied.add(id);
        }
        return r == null ? com.observance.watcher.beats.BeatResult.failed("null-result") : r;
    }

    /* ------------------------------------------------------------------ */
    /* Resolution helpers                                                  */
    /* ------------------------------------------------------------------ */

    /** Online target player, or null. */
    protected static Player target(BeatRequest req) {
        return req.hasTarget() ? req.targetPlayer() : null;
    }

    /**
     * Resolve the world anchor for a world-located beat. Priority:
     * 1) explicit x/y/z in payload, 2) the resolved site location, 3) the target player's location.
     * Returns null if none resolvable. MAIN thread (touches Bukkit).
     */
    protected static Location anchor(BeatContext ctx, BeatRequest req) {
        var p = req.payload();
        if (p.has("x") && p.has("y") && p.has("z")) {
            String worldName = p.string("world", null);
            org.bukkit.World world = null;
            if (worldName != null) world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null && req.hasSite() && req.site().location() != null) {
                world = req.site().location().getWorld();
            }
            if (world == null && req.hasTarget()) world = req.targetPlayer().getWorld();
            if (world != null) {
                return new Location(world,
                        p.number("x", 0), p.number("y", 0), p.number("z", 0));
            }
        }
        if (req.hasSite()) {
            Location loc = req.site().location();
            if (loc != null) return loc;
        }
        if (req.hasTarget()) {
            return req.targetPlayer().getLocation();
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Parsing helpers                                                     */
    /* ------------------------------------------------------------------ */

    /** Parse a Material by name (case-insensitive), with a default. Never throws. */
    protected static Material material(String name, Material def) {
        if (name == null || name.isBlank()) return def;
        try {
            Material m = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            return m == null ? def : m;
        } catch (Throwable t) {
            return def;
        }
    }

    /**
     * Resolve a Bukkit {@link org.bukkit.Sound} by name, or null. Accepts either a constant-style
     * name ("AMBIENT_CAVE") or a registry key ("ambient.cave" / "minecraft:ambient.cave"). Uses the
     * SOUNDS registry (future-proof; {@code Sound} is no longer a plain enum). Never throws.
     */
    protected static org.bukkit.Sound sound(String name) {
        if (name == null || name.isBlank()) return null;
        String raw = name.trim();
        // Try as a registry key first (lower-case, dots).
        try {
            String keyish = raw.toLowerCase(Locale.ROOT);
            org.bukkit.NamespacedKey nk = keyish.contains(":")
                    ? org.bukkit.NamespacedKey.fromString(keyish)
                    : org.bukkit.NamespacedKey.minecraft(keyish.replace('_', '.'));
            if (nk != null) {
                org.bukkit.Sound s = org.bukkit.Registry.SOUNDS.get(nk);
                if (s != null) return s;
            }
        } catch (Throwable ignored) { }
        // Fallback: legacy constant-name lookup.
        try {
            return org.bukkit.Sound.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Throwable t) {
            return null;
        }
    }

    /** Parse a Particle enum by name, or a default. Never throws. */
    protected static org.bukkit.Particle particle(String name, org.bukkit.Particle def) {
        if (name == null || name.isBlank()) return def;
        try {
            return org.bukkit.Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable t) {
            return def;
        }
    }

    /** A namespaced key under the plugin's namespace for PDC tags / advancements. */
    protected static NamespacedKey key(BeatContext ctx, String sub) {
        String safe = (sub == null ? "k" : sub.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
        return new NamespacedKey(ctx.namespace(), safe);
    }

    /* ------------------------------------------------------------------ */
    /* Reveal-disciplined mutation                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Run a block mutation only when the block is hidden from all players (reveal discipline). If a
     * player can currently witness it, retry later up to the configured attempts; if still witnessed
     * after the last attempt, give up quietly (soft-pressure — the beat just doesn't happen).
     *
     * @return true if the mutation was scheduled/applied, false if no anchor.
     */
    protected boolean mutateWhenUnwitnessed(BeatContext ctx, Block block, Runnable mutation) {
        if (block == null || mutation == null) return false;
        attemptHidden(ctx, block, mutation, 0);
        return true;
    }

    private void attemptHidden(BeatContext ctx, Block block, Runnable mutation, int attempt) {
        // MAIN thread already. Check witness; if clear, mutate; else retry later.
        boolean hidden = ctx.safety().call("beat.reveal.check",
                () -> ctx.reveal().isHidden(block), Boolean.TRUE);
        if (Boolean.TRUE.equals(hidden)) {
            ctx.safety().run("beat.reveal.mutate", mutation);
            return;
        }
        int max = ctx.config().revealRetryMaxAttempts();
        if (attempt >= max) {
            return; // witnessed too long — abandon silently (no jank, no half-applied state)
        }
        long delay = ctx.config().revealRetryDelayTicks();
        ctx.scheduler().runLaterSafe("beat.reveal.retry", delay,
                () -> attemptHidden(ctx, block, mutation, attempt + 1));
    }

    /**
     * The PER-PLAYER half of the two-path reveal (MF-9). Runs a private mutation — typically a
     * client-side illusion via {@code player.sendBlockChange} — only when the target is hidden from
     * THAT player, so each member of a convened group discovers the change without seeing it appear,
     * even when a globally-unwitnessed instant ({@link #mutateWhenUnwitnessed}) never comes because the
     * group is standing together. Retries on the same cadence and abandons quietly. Use this for group
     * scenes; the real-world {@code mutateWhenUnwitnessed} stays the path for solo / unattended changes.
     *
     * @return true if delivery was scheduled, false if no player/block.
     */
    protected boolean privateRevealWhenUnwitnessed(BeatContext ctx, Player player, Block block, Runnable mutation) {
        if (player == null || block == null || mutation == null) return false;
        attemptHiddenFrom(ctx, player, block, mutation, 0);
        return true;
    }

    private void attemptHiddenFrom(BeatContext ctx, Player player, Block block, Runnable mutation, int attempt) {
        if (player == null || !player.isOnline()) return;     // left the scene — abandon
        boolean hidden = ctx.safety().call("beat.reveal.checkFrom",
                () -> ctx.reveal().isHiddenFrom(player, block), Boolean.TRUE);
        if (Boolean.TRUE.equals(hidden)) {
            ctx.safety().run("beat.reveal.mutateFrom", mutation);
            return;
        }
        int max = ctx.config().revealRetryMaxAttempts();
        if (attempt >= max) {
            return; // they kept looking — abandon silently (no pop-in)
        }
        long delay = ctx.config().revealRetryDelayTicks();
        ctx.scheduler().runLaterSafe("beat.reveal.retryFrom", delay,
                () -> attemptHiddenFrom(ctx, player, block, mutation, attempt + 1));
    }

    /** Forget idempotency state (e.g. on reload), so the same row can re-fire after a wipe. */
    public void clearAppliedState() {
        applied.clear();
    }
}
