package com.observance.watcher.beats;

import com.observance.watcher.config.Site;
import com.observance.watcher.data.SupabaseResult;
import com.observance.watcher.data.rows.ArcStateRow;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * THE WORLD-DRIFT CLOCK — the world quietly AGES on its own between sessions, so a place you left is a
 * little more lost when you return. Sculk creeps up the walls of a room you cleared, the deep leaks a
 * shade higher, and time you spent away is written into the edges of the places you already know.
 *
 * <p>This is the autonomous DRIVER; the enactment lives in {@link com.observance.watcher.beats.lib.WorldDriftBeat}
 * (strictly additive, protected, capped, out of sight). Each tick this:
 * <ol>
 *   <li>bails immediately if no site is placed yet, or drama is off / the watcher is asleep,</li>
 *   <li>reads the last-drift timestamp from {@code arc_state.flags.world_drift_last_ms} (async I/O),</li>
 *   <li>scales the amount of creep by how much REAL time has elapsed since then — a longer absence
 *       ages the world a little more — but HARD-CAPS the scale so a long gap can never carpet a place,</li>
 *   <li>rotates through a small subset of ALREADY-PLACED sites and drifts a LITTLE near each (on main),</li>
 *   <li>stamps {@code world_drift_last_ms = now} back into the same flat flags blob (async merge RPC).</li>
 * </ol>
 *
 * <p>Persistence: the clock keeps NO new table. It stores its one number in the SAME store the arc's
 * other persistent flags use — {@code arc_state.flags} — read via {@link com.observance.watcher.data.SupabaseClient#fetchArcState()}
 * and written via the atomic {@link com.observance.watcher.data.SupabaseClient#mergeArcFlags} RPC (a
 * shallow merge of a flat {@code {world_drift_last_ms: <epoch-ms>}}), which queues gracefully offline.
 *
 * <p>Reveal-safe by construction: drift is ONLY ever anchored at a site's own location (a place the
 * players have already found), and {@link com.observance.watcher.beats.lib.WorldDriftBeat} only writes
 * into empty cells that already have support out of sight — so the drift never reveals or points at an
 * un-found site; it ages the edges of what's already known.
 *
 * <p>Threading mirrors the ambient path: the DB read/write is ASYNC; the per-site enactment hops to
 * MAIN. Fully Safety-wrapped by the scheduler, so a failure degrades to "no drift this tick", never a
 * crash. Anchored to placed sites only, so it no-ops entirely until a seed is placed.
 */
public final class WorldDriftClock {

    /** The single flat flag the clock persists into {@code arc_state.flags} (epoch millis of last drift). */
    static final String FLAG_LAST_MS = "world_drift_last_ms";

    /** Time-scale unit: one "step" of extra creep per this many elapsed real-time hours. */
    private static final long HOURS_PER_STEP = 12L;

    /** Baseline drift when time has barely passed (or on the very first drift with no stored timestamp). */
    private static final int BASE_COUNT = 1;

    /** Absolute ceiling on the scaled per-site count the clock will ask for — the beat clamps again
     *  independently, but the clock refuses to even request more than this, so a months-long absence
     *  drifts exactly the same modest amount as a few-days one (no carpet, ever). */
    private static final int MAX_SCALED_COUNT = 4;

    /** How many placed sites to age per tick (a rotating subset — the world drifts gradually, not all
     *  at once). We advance the rotation cursor each tick so coverage is even over time. */
    private static final int SITES_PER_TICK = 2;

    /** Anchor radius handed to the beat (how far from the site's centre the creep may land). */
    private static final int DRIFT_RADIUS = 4;

    private final BeatContext ctx;
    private final BeatLibrary library;
    private final BooleanSupplier localSleep;
    private final BooleanSupplier dramaEnabled;

    /** Rotating cursor over the placed-site list so successive ticks age different places. */
    private int rotation = 0;

    public WorldDriftClock(BeatContext ctx, BeatLibrary library,
                           BooleanSupplier localSleep, BooleanSupplier dramaEnabled) {
        this.ctx = ctx;
        this.library = library;
        this.localSleep = localSleep == null ? () -> false : localSleep;
        this.dramaEnabled = dramaEnabled == null ? () -> true : dramaEnabled;
    }

    /**
     * One clock consideration. Entry point runs on MAIN (cheap local gates + placed-site read), then
     * hops ASYNC to read/write the timestamp, then back to MAIN to enact. Never throws (the scheduler
     * wraps this in Safety, and every hop is a {@code *Safe} variant).
     */
    public void tick() {
        // Cheap local gates first (main thread). The world only drifts while the game is "awake".
        if (!dramaEnabled.getAsBoolean()) return;
        if (localSleep.getAsBoolean()) return;
        if (ctx.sites() == null) return;

        // Snapshot the placed sites on MAIN (Site#location touches Bukkit). No sites placed → no-op.
        List<Site> placed = ctx.sites().placed();
        if (placed.isEmpty()) return;

        // Read the last-drift timestamp off-thread, then hop back to main to drift + persist.
        ctx.scheduler().runAsyncSafe("world.drift.read", () -> {
            long lastMs = readLastDriftMs();
            long now = System.currentTimeMillis();
            int scaled = scaleCount(now, lastMs);
            ctx.scheduler().runMainSafe("world.drift.enact", () -> enactNear(placed, scaled, now));
        });
    }

    /**
     * Drift a LITTLE near a rotating subset of the placed sites, then persist the new timestamp. MAIN
     * thread (touches worlds). Only writes the timestamp if at least one site actually drifted, so a
     * tick where nothing was loaded/clear doesn't "use up" the elapsed time.
     */
    private void enactNear(List<Site> placed, int scaledCount, long now) {
        Beat beat = library.get("world_drift");
        if (beat == null) return;                                  // not registered → nothing to do

        int fired = 0;
        int n = placed.size();
        for (int i = 0; i < SITES_PER_TICK && i < n; i++) {
            Site site = placed.get(Math.floorMod(rotation + i, n));
            if (site == null || !site.isPlaced()) continue;
            if (site.location() == null) continue;                 // world not loaded → skip this one

            BeatPayload payload = BeatPayload.of(driftPayload(scaledCount));
            // A synthetic id keeps this out of the persistent idempotency set — the drift is meant to
            // recur over time, not fire once. Site-anchored, no target player (world/ambient beat).
            BeatRequest req = new BeatRequest(
                    "world-drift-" + UUID.randomUUID(),
                    beat.name(),
                    BeatCategory.AMBIENT,
                    null,
                    site,
                    payload);

            // Precheck (chunk loaded, a clear cell exists) then enact — both Safety-wrapped.
            boolean canEnact = ctx.safety().call("world.drift.canEnact",
                    () -> beat.canEnact(ctx, req), Boolean.FALSE);
            if (!Boolean.TRUE.equals(canEnact)) continue;

            BeatResult r = ctx.safety().call("world.drift.enact." + beat.name(),
                    () -> beat.enact(ctx, req), BeatResult.failed("threw"));
            if (r != null && r.kind() == BeatResult.Kind.FIRED) fired++;
        }

        // Advance the rotation so the NEXT tick ages different sites (even coverage over time).
        rotation = (n == 0) ? 0 : Math.floorMod(rotation + SITES_PER_TICK, n);

        if (fired > 0) {
            persistDriftMs(now);
            if (ctx.config() != null && ctx.config().debug()) {
                final int f = fired;
                final int c = scaledCount;
                ctx.scheduler().runAsyncSafe("world.drift.audit",
                        () -> ctx.safety().info("world.drift", "aged=" + f + " site(s) count=" + c));
            }
        }
    }

    /** The per-site payload: the scaled creep count + the aging materials. Flat + lore-agnostic. */
    private static com.google.gson.JsonObject driftPayload(int scaledCount) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("radius", DRIFT_RADIUS);
        o.addProperty("count", scaledCount);
        o.addProperty("vein_material", "SCULK_VEIN");
        o.addProperty("floor_material", "MOSS_CARPET");
        return o;
    }

    /**
     * Scale the creep amount by REAL elapsed time since the last drift: a longer absence ages the world
     * a bit more. Hard-capped at {@link #MAX_SCALED_COUNT} so a gap of any length can never carpet a
     * place. A missing/zero/future stored timestamp → the modest baseline (never a spike).
     */
    private static int scaleCount(long now, long lastMs) {
        if (lastMs <= 0L || lastMs > now) return BASE_COUNT;       // first drift or clock skew → baseline
        long elapsedMs = now - lastMs;
        long hours = elapsedMs / (60L * 60L * 1000L);
        long steps = hours / HOURS_PER_STEP;                       // one extra step of creep per window
        long count = BASE_COUNT + steps;
        if (count < BASE_COUNT) count = BASE_COUNT;
        if (count > MAX_SCALED_COUNT) count = MAX_SCALED_COUNT;    // the clock never asks for more
        return (int) count;
    }

    /* ------------------------------------------------------------------ */
    /* Persistence — the SAME flat arc_state.flags store, no new table.    */
    /* ------------------------------------------------------------------ */

    /** Read {@code arc_state.flags.world_drift_last_ms} (epoch ms), or 0 if absent/unreachable. ASYNC. */
    private long readLastDriftMs() {
        try {
            if (ctx.supabase() == null || !ctx.supabase().isConfigured()) return 0L;
            SupabaseResult<ArcStateRow> r = ctx.supabase().fetchArcState();
            if (r == null || !r.ok() || r.value() == null || r.value().flags == null) return 0L;
            Object v = r.value().flagsMap().get(FLAG_LAST_MS);
            if (v instanceof Number num) return num.longValue();
            if (v != null) {
                try { return Long.parseLong(v.toString().trim()); } catch (NumberFormatException ignored) { }
            }
        } catch (Throwable ignored) {
            // any failure → treat as "no record" (baseline drift); never propagate
        }
        return 0L;
    }

    /**
     * Stamp {@code world_drift_last_ms = now} into {@code arc_state.flags} via the atomic merge RPC (a
     * shallow merge of one flat key — the twin of how in-world solves advance the arc). Off-thread I/O;
     * queues gracefully if offline. Never throws.
     */
    private void persistDriftMs(long now) {
        final long stamp = now;
        ctx.scheduler().runAsyncSafe("world.drift.persist", () -> {
            try {
                if (ctx.supabase() == null) return;
                com.google.gson.JsonObject flags = new com.google.gson.JsonObject();
                flags.addProperty(FLAG_LAST_MS, stamp);
                ctx.supabase().mergeArcFlags(flags);
            } catch (Throwable ignored) {
                // best-effort: a failed stamp just means the next tick re-reads an older time (bounded
                // by the clock cap), never a crash and never a runaway.
            }
        });
    }
}
