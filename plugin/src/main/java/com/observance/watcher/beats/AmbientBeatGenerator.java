package com.observance.watcher.beats;

import com.observance.watcher.config.Site;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

/**
 * Generates AMBIENT beats from the drama budget when the moment is good — so something quietly
 * happens often enough that nobody's bored two days in, yet the world still feels ALMOST normal
 * (restraint IS the horror). NOT a phenomenon every two minutes: every fire passes the
 * {@link DramaBudget} (per-player + global ambient cooldowns + rolling-window cap).
 *
 * <h2>Cadence</h2>
 * The plugin schedules {@link #tick()} on a coarse timer (e.g. every couple of minutes). Each tick:
 * <ol>
 *   <li>respect the local + remote sleep switch (checked by the caller before scheduling, and again
 *       here defensively),</li>
 *   <li>pick a candidate online player (optionally biased toward a placed site they're near),</li>
 *   <li>build a {@link BeatRequest}, choose an ambient beat that {@code canEnact} now,</li>
 *   <li>reserve budget; if granted, enact on the main thread; refund if it didn't fire.</li>
 * </ol>
 *
 * <p>Threading: {@link #tick()} runs on the MAIN thread (it reads online players + their locations,
 * and enacts). The caller may pre-screen remote {@code watcher_sleep} async; the local kill switch
 * is checked here cheaply.
 *
 * <p>Content: ambient beats that need NO authored text (sound, particle, torch, door, fake-block,
 * darkness, time-shift) carry only numeric params, supplied here as safe defaults — fully
 * lore-agnostic. Text-bearing ambient flavor (titles/action-bars) is authored by the showrunner via
 * queued DIRECTED beats, not synthesized here, so the engine never invents story.
 */
public final class AmbientBeatGenerator {

    private final BeatContext ctx;
    private final BeatLibrary library;
    private final DramaBudget budget;
    private final Attention attention;
    private final BooleanSupplier localSleep;
    private final BooleanSupplier dramaEnabled;

    public AmbientBeatGenerator(BeatContext ctx, BeatLibrary library, DramaBudget budget,
                                Attention attention,
                                BooleanSupplier localSleep, BooleanSupplier dramaEnabled) {
        this.ctx = ctx;
        this.library = library;
        this.budget = budget;
        this.attention = attention == null ? new Attention(0.82) : attention;
        this.localSleep = localSleep == null ? () -> false : localSleep;
        this.dramaEnabled = dramaEnabled == null ? () -> true : dramaEnabled;
    }

    /** One ambient consideration. MAIN thread. Never throws (caller wraps in Safety too). */
    public void tick() {
        if (!dramaEnabled.getAsBoolean()) return;
        if (localSleep.getAsBoolean()) return;

        List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        // Update each player's ATTENTION from their situation (alone/deep/dark/night), decaying first so
        // a scared moment fades instead of snowballing into a haywire cascade.
        attention.decayAll();
        for (Player p : online) {
            if (p == null || !p.isOnline()) continue;
            double delta = situationDelta(p, online);
            if (delta != 0.0) attention.add(p.getUniqueId(), delta);
        }

        // Focus = a player chosen WEIGHTED by attention (with a calm floor for fairness), so the lonely
        // deep miner draws the Watcher far more than the group at base — the haunting feels like
        // "it knows me", not a dice roll.
        Player focus = pickWeightedFocus(online);
        if (focus == null || !focus.isOnline()) return;

        // RESTRAINT (the upstream "should anything happen at all" gate): at low attention, usually stay
        // quiet — the world must feel almost-normal. The DramaBudget still gates every fire below.
        Attention.Tier tier = attention.tier(focus.getUniqueId());
        if (ThreadLocalRandom.current().nextDouble() < Attention.skipProbability(tier)) return;

        // Optionally anchor to a placed site the player is currently inside (beats land where seen).
        Site site = siteContaining(focus);

        BeatRequest req = new BeatRequest(
                "ambient-" + UUID.randomUUID(),
                "ambient",
                BeatCategory.AMBIENT,
                focus,
                site,
                BeatPayload.empty());

        Beat beat = library.pickAmbient(ctx, req);
        if (beat == null) return; // nothing can fire here right now — stay quiet

        // Reserve budget keyed to the focus player.
        UUID target = focus.getUniqueId();
        if (!budget.tryReserve(BeatCategory.AMBIENT, target)) {
            return; // cooldowns/window say not now
        }

        // Re-wrap the request under the chosen beat's identity (same payload).
        BeatRequest enactReq = new BeatRequest(req.beatId(), beat.name(),
                BeatCategory.AMBIENT, focus, site, req.payload());

        BeatResult result = ctx.safety().call("beat.ambient.enact." + beat.name(),
                () -> beat.enact(ctx, enactReq), BeatResult.failed("threw"));
        if (result == null) result = BeatResult.failed("null");

        if (result.kind() != BeatResult.Kind.FIRED) {
            budget.refund(BeatCategory.AMBIENT, target);
        } else {
            // Capture the name HERE (main thread) — never read a Bukkit object inside the async body.
            final String n = beat.name();
            final String focusName = focus.getName();
            ctx.scheduler().runAsyncSafe("beat.ambient.audit", () ->
                    ctx.safety().info("beat.ambient", "fired=" + n + " focus=" + focusName));
        }
    }

    /**
     * How much this player's current SITUATION raises their attention this tick — the watch-worthy
     * signals, read cheaply from Bukkit on main: alone, deep, in the dark, at night. Capped so it ramps
     * over several considerations rather than spiking instantly.
     */
    private double situationDelta(Player p, List<? extends Player> online) {
        var loc = p.getLocation();
        if (loc == null || loc.getWorld() == null) return 0.0;
        double d = 0.0;

        // Alone — no other player within 32 blocks in the same world.
        boolean alone = true;
        double r2 = 32.0 * 32.0;
        for (Player o : online) {
            if (o == null || o == p || !o.isOnline()) continue;
            var ol = o.getLocation();
            if (ol == null || ol.getWorld() == null || !ol.getWorld().equals(loc.getWorld())) continue;
            if (ol.distanceSquared(loc) <= r2) { alone = false; break; }
        }
        if (alone) d += 0.15;

        if (loc.getY() < 8) d += 0.12;                                  // deep — toward the dark

        try {                                                          // in the dark
            if (loc.getBlock().getLightLevel() <= 4) d += 0.10;
        } catch (Throwable ignored) { }

        long time = loc.getWorld().getTime();                          // night
        if (time >= 13000L && time <= 23000L) d += 0.06;

        return Math.min(d, 0.40);
    }

    /**
     * Pick an online player weighted by attention, with a small floor so calm players still get rare
     * visits (fairness). Falls back to uniform when everyone is calm.
     */
    private Player pickWeightedFocus(List<? extends Player> online) {
        double total = 0.0;
        for (Player p : online) {
            if (p == null || !p.isOnline()) continue;
            total += 0.05 + attention.score(p.getUniqueId());          // 0.05 floor = fairness
        }
        if (total <= 0.0) {
            return online.get(ThreadLocalRandom.current().nextInt(online.size()));
        }
        double r = ThreadLocalRandom.current().nextDouble() * total;
        for (Player p : online) {
            if (p == null || !p.isOnline()) continue;
            r -= (0.05 + attention.score(p.getUniqueId()));
            if (r <= 0.0) return p;
        }
        return online.get(online.size() - 1);
    }

    /** First placed site the player currently stands inside, or null. MAIN thread. */
    private Site siteContaining(Player p) {
        if (ctx.sites() == null) return null;
        var loc = p.getLocation();
        String world = loc.getWorld() == null ? null : loc.getWorld().getName();
        if (world == null) return null;
        for (Site s : ctx.sites().placed()) {
            if (s.contains(world, loc.getX(), loc.getY(), loc.getZ())) return s;
        }
        return null;
    }
}
