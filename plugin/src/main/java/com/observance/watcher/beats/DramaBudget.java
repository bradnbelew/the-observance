package com.observance.watcher.beats;

import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.util.RateLimiter;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.LongSupplier;

/**
 * The L4D-style Director (pure deterministic — NO ML, Phase 0). Enforces the pacing contract so
 * the world feels ALMOST normal: sparse but consistent, never a phenomenon every 2 minutes, and
 * physically incapable of spamming. Restraint IS the horror.
 *
 * <h2>Gates (all from {@link ObservanceConfig})</h2>
 * <ul>
 *   <li><b>watcher_sleep / master kill:</b> suppress everything (checked by the engine, not here).</li>
 *   <li><b>Ambient global spacing:</b> ≥ {@code ambient-global-cooldown-minutes} between ANY two
 *       ambient beats server-wide.</li>
 *   <li><b>Ambient per-player cooldown:</b> ≥ {@code ambient-cooldown-minutes} between ambient
 *       beats aimed at the same player (≈ ≤1/hour of play).</li>
 *   <li><b>Personalized:</b> ≤ {@code personalized-max-per-session} per player per session, with a
 *       hard ≥ {@code personalized-cooldown-minutes} (floor 20) cooldown.</li>
 *   <li><b>Rolling window cap:</b> ≤ {@code window-max-beats} total (ambient + personalized) per
 *       {@code window-minutes} window, server-wide. The absolute spam ceiling.</li>
 * </ul>
 *
 * <p>DIRECTED beats (explicitly queued+approved by the bot/dashboard) BYPASS the ambient spacing
 * gates — the showrunner already paced them — but still count toward the rolling window so a flood
 * of approvals can't overrun the server, and still record into the window for honesty.
 *
 * <p>Thread-safe. The engine calls {@link #tryReserve} from the MAIN thread right before enacting;
 * if the beat then SKIPs/FAILs the engine calls {@link #refund} so a no-op doesn't burn budget.
 */
public final class DramaBudget {

    private final ObservanceConfig config;
    private final RateLimiter limiter;     // reused for per-player + global cooldowns
    private final LongSupplier clock;

    /** Timestamps (ms) of recently fired beats, for the rolling-window cap. */
    private final Deque<Long> windowFires = new ConcurrentLinkedDeque<>();

    /** Per-player personalized-beat counts for the current session (reset on join/quit by engine). */
    private final ConcurrentHashMap<UUID, Integer> personalizedThisSession = new ConcurrentHashMap<>();

    private static final String K_GLOBAL_AMBIENT = "drama:ambient:global";

    public DramaBudget(ObservanceConfig config, RateLimiter limiter) {
        this(config, limiter, System::currentTimeMillis);
    }

    public DramaBudget(ObservanceConfig config, RateLimiter limiter, LongSupplier clock) {
        this.config = config;
        this.limiter = limiter == null ? new RateLimiter() : limiter;
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    /**
     * Atomically attempt to reserve budget for a beat. Returns true (and records the reservation)
     * if all applicable gates pass; false otherwise. Never throws.
     *
     * <p>The reservation is provisional: the engine MUST call {@link #refund} if the beat ends up
     * not firing (SKIPPED/FAILED), so a non-event doesn't consume the rare drama budget.
     *
     * @param category beat category
     * @param target   target player uuid (per-player gates), or null for world-wide ambient
     */
    public synchronized boolean tryReserve(BeatCategory category, UUID target) {
        if (category == null) return false;
        if (!config.dramaEnabled()) return false;

        // Rolling window cap applies to EVERYTHING (the hard ceiling).
        if (!windowHasRoom()) {
            return false;
        }

        switch (category) {
            case DIRECTED:
                // Showrunner-paced: only the window cap applies. Record + go.
                recordWindowFire();
                return true;

            case AMBIENT: {
                // PEEK both cooldowns first (read-only), then arm only if both are ready — so a
                // player on cooldown can't burn the global slot and starve others.
                long perPlayerMs = minutesToMs(config.ambientCooldownMinutes());
                String playerKey = target == null ? null : ("drama:ambient:" + target);
                if (playerKey != null && perPlayerMs > 0 && limiter.remainingMs(playerKey) > 0) {
                    return false; // this player had an ambient beat too recently
                }
                long globalMs = minutesToMs(config.ambientGlobalCooldownMinutes());
                if (globalMs > 0 && limiter.remainingMs(K_GLOBAL_AMBIENT) > 0) {
                    return false; // too soon since the last ambient beat anywhere
                }
                // Both ready → arm them now.
                if (globalMs > 0) limiter.tryCooldown(K_GLOBAL_AMBIENT, globalMs);
                if (playerKey != null && perPlayerMs > 0) limiter.tryCooldown(playerKey, perPlayerMs);
                recordWindowFire();
                return true;
            }

            case PERSONALIZED: {
                int maxPerSession = config.personalizedMaxPerSession();
                if (maxPerSession <= 0) return false;
                if (target != null) {
                    int used = personalizedThisSession.getOrDefault(target, 0);
                    if (used >= maxPerSession) return false;
                    long cdMs = minutesToMs(config.personalizedCooldownMinutes());
                    if (cdMs > 0 && !limiter.tryCooldown("drama:personalized:" + target, cdMs)) {
                        return false; // inside the hard ≥20-min cooldown
                    }
                    personalizedThisSession.merge(target, 1, Integer::sum);
                }
                recordWindowFire();
                return true;
            }

            default:
                return false;
        }
    }

    /**
     * Undo a reservation that didn't result in a fired beat. We can't un-consume a one-shot cooldown
     * cleanly, but we DO return the rolling-window slot and the per-session personalized count so a
     * skipped personalized beat doesn't permanently burn the player's one-per-session allowance.
     */
    public synchronized void refund(BeatCategory category, UUID target) {
        try {
            // Return the window slot (pop the most recent fire).
            windowFires.pollLast();
            if (category == BeatCategory.PERSONALIZED && target != null) {
                personalizedThisSession.computeIfPresent(target, (k, v) -> v <= 1 ? null : v - 1);
            }
        } catch (Throwable ignored) {
            // never propagate
        }
    }

    /** Reset a player's per-session personalized allowance (engine calls on join). */
    public void resetSession(UUID player) {
        if (player != null) personalizedThisSession.remove(player);
    }

    /** Drop all per-session state (e.g. on reload). */
    public void clear() {
        personalizedThisSession.clear();
        windowFires.clear();
    }

    /* ------------------------------------------------------------------ */
    /* Introspection (dashboard health view)                               */
    /* ------------------------------------------------------------------ */

    public synchronized int beatsInWindow() {
        pruneWindow();
        return windowFires.size();
    }

    public int windowCap() {
        return config.windowMaxBeats();
    }

    public long globalAmbientRemainingMs() {
        return limiter.remainingMs(K_GLOBAL_AMBIENT);
    }

    /* ------------------------------------------------------------------ */
    /* Internals                                                           */
    /* ------------------------------------------------------------------ */

    private boolean windowHasRoom() {
        pruneWindow();
        return windowFires.size() < Math.max(1, config.windowMaxBeats());
    }

    private void recordWindowFire() {
        windowFires.addLast(clock.getAsLong());
    }

    private void pruneWindow() {
        long cutoff = clock.getAsLong() - minutesToMs(config.windowMinutes());
        Long head;
        while ((head = windowFires.peekFirst()) != null && head < cutoff) {
            windowFires.pollFirst();
        }
    }

    private static long minutesToMs(int minutes) {
        return Math.max(0L, (long) minutes) * 60_000L;
    }
}
