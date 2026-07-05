package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * The Unlit Deep — the ONE collective-restraint group latch (INV-17; config.yml {@code customs.unlit-deep}
 * + the {@code restraint.enabled} master kill). Unlike the seven {@link TrackerConfig#CUSTOM_BOW}-style
 * customs (per-player honored/violated tallies, see {@link CustomComplianceListener}'s own note on this
 * exact boundary), this is GROUP-scoped and negative: a thing kept by not-doing. Reward/withdrawal is a
 * downstream beat's job (borrowed warmth from the Undercroft fire, once that site exists) — this listener's
 * whole job is the DETECTION + the one group-scoped flag write, mirroring {@link CoopPlateListener} /
 * {@link BlackMoonTollListener}'s direct-to-Supabase pattern (never the per-player tracker pathway).
 *
 * <p>Detected on EXPLICIT flame acts only — {@link BlockPlaceEvent} (placing a configured flame material)
 * or a player-attributed {@link BlockIgniteEvent} (flint-and-steel / fire charge — the "held-flame edge"),
 * NEVER ambient light sampling or fire spread/lava/lightning ignition (precision over recall, matching the
 * config's own "EXPLICIT flame acts only" comment). Armed only at/below {@code deep-line-y} on a taboo moon
 * phase (empty config ⇒ reuses Dark Hours' taboo set).
 *
 * <p>One latch-edge per GROUP per cooldown (not per-player) — "the latch is a state, not a spammable
 * counter." On a fresh edge, merges {@code unlit_deep_broken_at} (epoch ms) + {@code unlit_deep_broken_by}
 * (the acting player's name) into {@code arc_state.flags} — recorded, never spoken (no chat message, no
 * world mutation; a downstream beat/website owns any telling). REVERSIBLE by construction: a plain flag
 * write, not a one-way ratchet — nothing here stops a future re-light from clearing it.
 *
 * <p>Pure + safe: never cancels the event, never messages a player; body in Safety; the flag write is async.
 * Config-gated ({@code customs.unlit-deep.enabled} AND {@code restraint.enabled}) → a clean no-op when off.
 */
public final class UnlitDeepListener implements Listener {

    /** The shared marker Discord/dashboard read: epoch ms of the latch's last break. */
    public static final String FLAG_BROKEN_AT = "unlit_deep_broken_at";
    /** Recorded-not-spoken: who broke it, for the archive — never chat-announced. */
    public static final String FLAG_BROKEN_BY = "unlit_deep_broken_by";

    private final SignalTracker tracker;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    public UnlitDeepListener(SignalTracker tracker, SupabaseClient supabase,
                             RateLimiter rateLimiter, Scheduler scheduler, Safety safety) {
        this.tracker = tracker;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        safety.run("unlit_deep.place", () -> {
            TrackerConfig cfg = tracker.config();
            if (!armed(cfg)) return;
            if (!cfg.isUnlitDeepFlameMaterial(event.getBlockPlaced().getType().name())) return;
            tryLatch(cfg, event.getBlock().getLocation(), event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        safety.run("unlit_deep.ignite", () -> {
            TrackerConfig cfg = tracker.config();
            if (!armed(cfg)) return;
            // Explicit acts only — a held flame lit by a player, never spread/lava/lightning.
            BlockIgniteEvent.IgniteCause cause = event.getCause();
            if (cause != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                    && cause != BlockIgniteEvent.IgniteCause.FIREBALL) return;
            Player p = event.getPlayer();
            if (p == null) return;
            tryLatch(cfg, event.getBlock().getLocation(), p);
        });
    }

    /* ----------------------------- shared latch ---------------------------- */

    private boolean armed(TrackerConfig cfg) {
        return cfg.unlitDeepEnabled() && cfg.restraintEnabled()
                && tracker != null && supabase != null && scheduler != null && rateLimiter != null;
    }

    private void tryLatch(TrackerConfig cfg, Location at, Player p) {
        if (at == null || p == null) return;
        World world = at.getWorld();
        if (world == null) return;
        if (at.getBlockY() > cfg.unlitDeepDeepLineY()) return;      // not below the deep line
        if (!isTabooMoon(world, cfg)) return;                       // not the black moon

        // One latch-edge per GROUP per cooldown — a single shared key, not per-player.
        if (!rateLimiter.tryCooldown("unlit_deep:latch", cfg.unlitDeepCooldownMs())) return;

        final String name = p.getName();
        final long now = System.currentTimeMillis();
        scheduler.runAsyncSafe("unlit_deep.latch", () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(FLAG_BROKEN_AT, now);
            flags.addProperty(FLAG_BROKEN_BY, name);
            supabase.mergeArcFlags(flags);
            safety.info("unlit_deep", "the deep was lit on the black moon — kept no longer (recorded, not spoken)");
        });
    }

    /** Vanilla moon phase 0..7 (0 = full/"black" moon) from the world's full day count. */
    private boolean isTabooMoon(World world, TrackerConfig cfg) {
        try {
            long days = world.getFullTime() / 24000L;
            int phase = (int) (days % 8L);
            if (phase < 0) phase += 8;
            return cfg.isUnlitDeepTabooMoonPhase(phase);
        } catch (Throwable t) {
            return false;
        }
    }
}
