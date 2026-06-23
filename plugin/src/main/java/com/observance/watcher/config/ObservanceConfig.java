package com.observance.watcher.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, immutable snapshot of {@code config.yml}. Built once on enable / reload; readers get
 * plain getters with sane defaults so a malformed config can never NPE a subsystem.
 *
 * <p>The Supabase service key is resolved here (env var first, then config value) but is NEVER
 * logged or exposed by any accessor that prints; {@link #hasServiceKey()} only reveals presence.
 */
public final class ObservanceConfig {

    // --- supabase ---
    private final String supabaseUrl;
    private final String serviceKey;        // resolved; sensitive — do not log
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int maxRetries;
    private final long retryBackoffMs;
    private final int offlineQueueMax;

    // --- beat queue ---
    private final int beatPollIntervalSeconds;
    private final int beatMaxPerPoll;

    // --- samplers ---
    private final int locationSampleSeconds;
    private final int heatmapCellSize;
    private final int baseDetectSeconds;
    private final int inventoryScanSeconds;
    private final int presenceHeartbeatSeconds;

    // --- drama ---
    private final boolean dramaEnabled;
    private final int personalizedCooldownMinutes;
    private final int personalizedMaxPerSession;
    private final int ambientCooldownMinutes;
    private final int ambientGlobalCooldownMinutes;
    private final int windowMinutes;
    private final int windowMaxBeats;
    private final double personalizedConfidenceMin;

    // --- rate limit ---
    private final int oracleCooldownSeconds;
    private final int oracleBurst;
    private final int oracleRefillSeconds;

    // --- protection ---
    private final boolean restoreBrokenMarkers;
    private final long restoreDelayTicks;
    private final boolean cancelProtectedBreaks;

    // --- reveal ---
    private final int witnessRadius;
    private final int revealRetryDelayTicks;
    private final int revealRetryMaxAttempts;

    // --- logging ---
    private final boolean debug;

    private ObservanceConfig(FileConfiguration c, String resolvedKey) {
        this.supabaseUrl = stripTrailingSlash(c.getString("supabase.url", ""));
        this.serviceKey = resolvedKey == null ? "" : resolvedKey;
        this.connectTimeoutMs = clampInt(c.getInt("supabase.connect-timeout-ms", 5000), 500, 60000);
        this.readTimeoutMs = clampInt(c.getInt("supabase.read-timeout-ms", 8000), 500, 60000);
        this.maxRetries = clampInt(c.getInt("supabase.max-retries", 2), 0, 6);
        this.retryBackoffMs = clampLong(c.getLong("supabase.retry-backoff-ms", 750L), 0L, 30000L);
        this.offlineQueueMax = clampInt(c.getInt("supabase.offline-queue-max", 500), 0, 100000);

        this.beatPollIntervalSeconds = clampInt(c.getInt("beat-queue.poll-interval-seconds", 30), 5, 3600);
        this.beatMaxPerPoll = clampInt(c.getInt("beat-queue.max-per-poll", 3), 1, 50);

        this.locationSampleSeconds = clampInt(c.getInt("samplers.location-sample-seconds", 20), 5, 3600);
        this.heatmapCellSize = clampInt(c.getInt("samplers.heatmap-cell-size", 16), 1, 256);
        this.baseDetectSeconds = clampInt(c.getInt("samplers.base-detect-seconds", 120), 10, 7200);
        this.inventoryScanSeconds = clampInt(c.getInt("samplers.inventory-scan-seconds", 90), 10, 7200);
        this.presenceHeartbeatSeconds = clampInt(c.getInt("samplers.presence-heartbeat-seconds", 60), 10, 7200);

        this.dramaEnabled = c.getBoolean("drama.enabled", true);
        // Hard floor of 20 on the personalized cooldown — the anti-spam contract.
        this.personalizedCooldownMinutes = clampInt(c.getInt("drama.personalized-cooldown-minutes", 20), 20, 1440);
        this.personalizedMaxPerSession = clampInt(c.getInt("drama.personalized-max-per-session", 1), 0, 10);
        this.ambientCooldownMinutes = clampInt(c.getInt("drama.ambient-cooldown-minutes", 60), 1, 1440);
        this.ambientGlobalCooldownMinutes = clampInt(c.getInt("drama.ambient-global-cooldown-minutes", 12), 0, 1440);
        this.windowMinutes = clampInt(c.getInt("drama.window-minutes", 60), 5, 1440);
        this.windowMaxBeats = clampInt(c.getInt("drama.window-max-beats", 4), 1, 100);
        this.personalizedConfidenceMin = clampDouble(c.getDouble("drama.personalized-confidence-min", 0.85), 0.0, 1.0);

        this.oracleCooldownSeconds = clampInt(c.getInt("rate-limit.oracle-cooldown-seconds", 5), 0, 3600);
        this.oracleBurst = clampInt(c.getInt("rate-limit.oracle-burst", 3), 1, 100);
        this.oracleRefillSeconds = clampInt(c.getInt("rate-limit.oracle-refill-seconds", 30), 1, 3600);

        this.restoreBrokenMarkers = c.getBoolean("protection.restore-broken-markers", true);
        this.restoreDelayTicks = clampLong(c.getLong("protection.restore-delay-ticks", 1L), 0L, 200L);
        this.cancelProtectedBreaks = c.getBoolean("protection.cancel-protected-breaks", true);

        this.witnessRadius = clampInt(c.getInt("reveal.witness-radius", 64), 4, 256);
        this.revealRetryDelayTicks = clampInt(c.getInt("reveal.retry-delay-ticks", 40), 1, 1200);
        this.revealRetryMaxAttempts = clampInt(c.getInt("reveal.retry-max-attempts", 10), 0, 200);

        this.debug = c.getBoolean("logging.debug", false);
    }

    /**
     * Build from a loaded config plus the environment, resolving the service key.
     * @param envLookup function mapping an env var name → value (normally System::getenv)
     */
    public static ObservanceConfig from(FileConfiguration c, java.util.function.Function<String, String> envLookup) {
        String envName = c.getString("supabase.service-key-env", "OBSERVANCE_SUPABASE_KEY");
        String key = null;
        if (envName != null && !envName.isBlank() && envLookup != null) {
            String fromEnv = envLookup.apply(envName);
            if (fromEnv != null && !fromEnv.isBlank()) {
                key = fromEnv.trim();
            }
        }
        if (key == null) {
            String fromConfig = c.getString("supabase.service-key", "");
            if (fromConfig != null && !fromConfig.isBlank()) {
                key = fromConfig.trim();
            }
        }
        return new ObservanceConfig(c, key);
    }

    /* ----------------------------- getters ---------------------------- */

    public String supabaseUrl() { return supabaseUrl; }
    /** The resolved service key. Treat as SECRET. Empty string when unset. */
    public String serviceKey() { return serviceKey; }
    /** Presence check that never reveals the key itself. */
    public boolean hasServiceKey() { return serviceKey != null && !serviceKey.isBlank(); }
    public boolean isConfigured() { return !supabaseUrl.isBlank() && hasServiceKey(); }

    public int connectTimeoutMs() { return connectTimeoutMs; }
    public int readTimeoutMs() { return readTimeoutMs; }
    public int maxRetries() { return maxRetries; }
    public long retryBackoffMs() { return retryBackoffMs; }
    public int offlineQueueMax() { return offlineQueueMax; }

    public int beatPollIntervalSeconds() { return beatPollIntervalSeconds; }
    public int beatMaxPerPoll() { return beatMaxPerPoll; }

    public int locationSampleSeconds() { return locationSampleSeconds; }
    public int heatmapCellSize() { return heatmapCellSize; }
    public int baseDetectSeconds() { return baseDetectSeconds; }
    public int inventoryScanSeconds() { return inventoryScanSeconds; }
    public int presenceHeartbeatSeconds() { return presenceHeartbeatSeconds; }

    public boolean dramaEnabled() { return dramaEnabled; }
    public int personalizedCooldownMinutes() { return personalizedCooldownMinutes; }
    public int personalizedMaxPerSession() { return personalizedMaxPerSession; }
    public int ambientCooldownMinutes() { return ambientCooldownMinutes; }
    public int ambientGlobalCooldownMinutes() { return ambientGlobalCooldownMinutes; }
    public int windowMinutes() { return windowMinutes; }
    public int windowMaxBeats() { return windowMaxBeats; }
    public double personalizedConfidenceMin() { return personalizedConfidenceMin; }

    public int oracleCooldownSeconds() { return oracleCooldownSeconds; }
    public int oracleBurst() { return oracleBurst; }
    public int oracleRefillSeconds() { return oracleRefillSeconds; }

    public boolean restoreBrokenMarkers() { return restoreBrokenMarkers; }
    public long restoreDelayTicks() { return restoreDelayTicks; }
    public boolean cancelProtectedBreaks() { return cancelProtectedBreaks; }

    public int witnessRadius() { return witnessRadius; }
    public int revealRetryDelayTicks() { return revealRetryDelayTicks; }
    public int revealRetryMaxAttempts() { return revealRetryMaxAttempts; }

    public boolean debug() { return debug; }

    /* ----------------------------- helpers ---------------------------- */

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static long clampLong(long v, long lo, long hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clampDouble(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
