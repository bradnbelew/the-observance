package com.observance.watcher.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key cooldowns + token-bucket burst control. Thread-safe; usable from any thread.
 *
 * <p>Used to rate-limit ANY player-triggerable beat (the answer oracle, sign-answer checks,
 * gesture spam) so nothing can be weaponized by spamming. Two independent mechanisms:
 *
 * <ul>
 *   <li>{@link #tryCooldown(String, long)} — simple "not more often than every N ms" gate.</li>
 *   <li>{@link #tryToken(String, int, long)} — token bucket: allow a small burst, then throttle
 *       to a steady refill rate.</li>
 * </ul>
 *
 * <p>Keys are arbitrary strings; convention is {@code "<scope>:<uuid>"} e.g.
 * {@code "oracle:" + player.getUniqueId()}. Stale buckets are pruned opportunistically.
 */
public final class RateLimiter {

    private static final class Cooldown {
        volatile long nextAllowedMs;
    }

    private static final class Bucket {
        double tokens;
        long lastRefillMs;
    }

    private final Map<String, Cooldown> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Pluggable clock for testability; defaults to wall clock. */
    private final java.util.function.LongSupplier clock;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    public RateLimiter(java.util.function.LongSupplier clock) {
        this.clock = clock;
    }

    /* ------------------------------------------------------------------ */
    /* Simple cooldown                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Returns true and arms the cooldown if at least {@code cooldownMs} has elapsed since the
     * last successful acquire for {@code key}; otherwise returns false and changes nothing.
     */
    public boolean tryCooldown(String key, long cooldownMs) {
        if (key == null) return false;
        long now = clock.getAsLong();
        Cooldown cd = cooldowns.computeIfAbsent(key, k -> new Cooldown());
        synchronized (cd) {
            if (now >= cd.nextAllowedMs) {
                cd.nextAllowedMs = now + Math.max(0L, cooldownMs);
                return true;
            }
            return false;
        }
    }

    /** Milliseconds remaining until {@code key} is allowed again (0 if ready or unknown). */
    public long remainingMs(String key) {
        if (key == null) return 0L;
        Cooldown cd = cooldowns.get(key);
        if (cd == null) return 0L;
        long rem = cd.nextAllowedMs - clock.getAsLong();
        return Math.max(0L, rem);
    }

    /* ------------------------------------------------------------------ */
    /* Token bucket                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Token-bucket acquire. The bucket holds up to {@code capacity} tokens and refills one
     * token every {@code refillMs}. Returns true (consuming one token) if a token is available.
     *
     * @param key       bucket identity
     * @param capacity  max burst (>=1)
     * @param refillMs  time to regain one token (>0)
     */
    public boolean tryToken(String key, int capacity, long refillMs) {
        if (key == null) return false;
        int cap = Math.max(1, capacity);
        long refill = Math.max(1L, refillMs);
        long now = clock.getAsLong();

        Bucket b = buckets.computeIfAbsent(key, k -> {
            Bucket nb = new Bucket();
            nb.tokens = cap;            // start full
            nb.lastRefillMs = now;
            return nb;
        });
        synchronized (b) {
            // Refill based on elapsed time.
            long elapsed = now - b.lastRefillMs;
            if (elapsed > 0) {
                double gained = (double) elapsed / (double) refill;
                if (gained > 0) {
                    b.tokens = Math.min(cap, b.tokens + gained);
                    b.lastRefillMs = now;
                }
            }
            if (b.tokens >= 1.0) {
                b.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Maintenance                                                         */
    /* ------------------------------------------------------------------ */

    /** Forget all state for a key (e.g. on player quit). Null-safe. */
    public void clear(String key) {
        if (key == null) return;
        cooldowns.remove(key);
        buckets.remove(key);
    }

    /** Drop cooldown entries that are fully elapsed and idle buckets, to bound memory. */
    public void prune() {
        long now = clock.getAsLong();
        cooldowns.entrySet().removeIf(e -> e.getValue().nextAllowedMs <= now);
        // Buckets self-cap; only prune ones that are full and untouched for a while.
        buckets.entrySet().removeIf(e -> {
            Bucket b = e.getValue();
            synchronized (b) {
                return (now - b.lastRefillMs) > 600_000L; // idle > 10 min
            }
        });
    }

    /** Remove every entry. */
    public void clearAll() {
        cooldowns.clear();
        buckets.clear();
    }
}
