package com.observance.watcher.signal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable, thread-safe per-player signal aggregate. The "dossier" in memory.
 *
 * <p>Accumulation happens from listeners on the MAIN thread (cheap counter bumps) and from the
 * location sampler (also main). The async flusher reads it to build a {@link com.observance.watcher.data.rows.DossierRow}.
 * To keep cross-thread reads honest, every mutating method is {@code synchronized} on this object,
 * and {@link #snapshot()} takes the same lock — so a snapshot is always internally consistent.
 *
 * <p>All accumulation is null-safe and saturating: counters never overflow into nonsense and a
 * bad input (NaN distance, null key) is ignored, never thrown. This object holds NO Bukkit
 * references — it is plain data, safe to touch from the async flush thread under its lock.
 */
public final class PlayerSignals {

    private final UUID uuid;
    private volatile String name;

    // --- core counters ---
    private long blocksMined;
    private long oresMined;
    private long deaths;
    private long mobKills;
    private long soloMiningSeconds;
    private long sessionPlaySeconds;
    private double hoardedScore;
    private int deepestY = Integer.MAX_VALUE;     // sentinel "no block broken yet"
    private long forbiddenWordHits;

    // chat sentiment running mean (Welford-lite: keep sum + count)
    private double sentimentSum;
    private long chatMessages;

    // solo-mining ratio bookkeeping (sampled): aloneMiningSamples / miningSamples
    private long miningSamples;
    private long aloneMiningSamples;

    // --- last sampled location + group distance ---
    private String lastWorld;
    private int lastX;
    private int lastY;
    private int lastZ;
    private long lastLocationMs;
    private double distanceFromGroup = -1.0;       // -1 = unknown/alone

    // --- per-session volatile flags (reset on (re)join) ---
    private boolean firstOreThisSessionTaken;
    private boolean offeringHonoredThisSession;
    private boolean miningNowSampledThisTick;       // helper for the sampler

    // --- custom compliance, keyed by opaque custom_key ---
    private final Map<String, Tally> compliance = new HashMap<>();

    // --- dirty tracking so the flusher only writes changed dossiers ---
    private volatile boolean dossierDirty;
    // custom_keys whose compliance changed since last flush (set of keys)
    private final Map<String, Boolean> dirtyCompliance = new ConcurrentHashMap<>();

    PlayerSignals(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }

    /** Update the display name (names can change between joins). */
    public synchronized void setName(String n) {
        if (n != null && !n.isBlank() && !n.equals(this.name)) {
            this.name = n;
            this.dossierDirty = true;
        }
    }

    /* ================================================================== */
    /*  Per-session lifecycle                                             */
    /* ================================================================== */

    /** Reset session-scoped flags on (re)join. Cumulative counters persist. */
    public synchronized void onSessionStart() {
        this.firstOreThisSessionTaken = false;
        this.offeringHonoredThisSession = false;
        this.miningNowSampledThisTick = false;
    }

    /* ================================================================== */
    /*  Accumulators (called from listeners / samplers, main thread)      */
    /* ================================================================== */

    public synchronized void addBlockMined(int y, boolean isOre) {
        blocksMined = sat(blocksMined + 1);
        if (isOre) oresMined = sat(oresMined + 1);
        if (y < deepestY) deepestY = y;
        miningNowSampledThisTick = true;
        dossierDirty = true;
    }

    public synchronized void addDeath() {
        deaths = sat(deaths + 1);
        dossierDirty = true;
    }

    public synchronized void addMobKill() {
        mobKills = sat(mobKills + 1);
        dossierDirty = true;
    }

    public synchronized void addSoloMiningSeconds(long seconds) {
        if (seconds <= 0) return;
        soloMiningSeconds = sat(soloMiningSeconds + seconds);
        dossierDirty = true;
    }

    public synchronized void addPlaySeconds(long seconds) {
        if (seconds <= 0) return;
        sessionPlaySeconds = sat(sessionPlaySeconds + seconds);
        dossierDirty = true;
    }

    /** Record a mining-activity sample for the solo-ratio (was the player alone while mining?). */
    public synchronized void recordMiningSample(boolean alone) {
        miningSamples = sat(miningSamples + 1);
        if (alone) aloneMiningSamples = sat(aloneMiningSamples + 1);
        dossierDirty = true;
    }

    /** Whether the player mined since the last sampler tick (then clears the flag). */
    public synchronized boolean consumeMinedSinceLastSample() {
        boolean v = miningNowSampledThisTick;
        miningNowSampledThisTick = false;
        return v;
    }

    /** Replace the hoard score (computed by the inventory scanner). */
    public synchronized void setHoardedScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) return;
        double clamped = Math.max(0.0, score);
        if (clamped != hoardedScore) {
            hoardedScore = clamped;
            dossierDirty = true;
        }
    }

    public synchronized void addForbiddenWordHit() {
        forbiddenWordHits = sat(forbiddenWordHits + 1);
        dossierDirty = true;
    }

    /** Fold one chat message's sentiment (clamped -1..+1) into the running mean. */
    public synchronized void addChatSentiment(double sentiment) {
        double s = Double.isNaN(sentiment) ? 0.0 : Math.max(-1.0, Math.min(1.0, sentiment));
        sentimentSum += s;
        chatMessages = sat(chatMessages + 1);
        dossierDirty = true;
    }

    /** Update last sampled location + nearest-other-player distance. */
    public synchronized void setLocationSample(String world, int x, int y, int z,
                                               double nearestOtherDistance, long nowMs) {
        this.lastWorld = world;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastLocationMs = nowMs;
        this.distanceFromGroup = (Double.isNaN(nearestOtherDistance) || nearestOtherDistance < 0)
                ? -1.0 : nearestOtherDistance;
        this.dossierDirty = true;
    }

    /* ----------------------- session flags --------------------------- */

    /** Mark first-ore taken this session; returns true if this call was the transition. */
    public synchronized boolean markFirstOreThisSession() {
        if (firstOreThisSessionTaken) return false;
        firstOreThisSessionTaken = true;
        dossierDirty = true;
        return true;
    }

    public synchronized boolean firstOreThisSessionTaken() { return firstOreThisSessionTaken; }

    public synchronized void markOfferingHonoredThisSession() {
        offeringHonoredThisSession = true;
        dossierDirty = true;
    }

    public synchronized boolean offeringHonoredThisSession() { return offeringHonoredThisSession; }

    /* ----------------------- custom compliance ----------------------- */

    /** Record a HONORED instance of a custom. Null-safe; ignores blank keys. */
    public synchronized void honor(String customKey, long nowMs) {
        if (customKey == null || customKey.isBlank()) return;
        Tally t = compliance.computeIfAbsent(customKey, k -> new Tally());
        t.honored = sat(t.honored + 1);
        t.lastEventMs = nowMs;
        dirtyCompliance.put(customKey, Boolean.TRUE);
    }

    /** Record a VIOLATED instance of a custom. Null-safe; ignores blank keys. */
    public synchronized void violate(String customKey, long nowMs) {
        if (customKey == null || customKey.isBlank()) return;
        Tally t = compliance.computeIfAbsent(customKey, k -> new Tally());
        t.violated = sat(t.violated + 1);
        t.lastEventMs = nowMs;
        dirtyCompliance.put(customKey, Boolean.TRUE);
    }

    /* ================================================================== */
    /*  Snapshots + flush bookkeeping                                     */
    /* ================================================================== */

    /** Take an internally-consistent, immutable snapshot for downstream engines. */
    public synchronized SignalSnapshot snapshot() {
        Map<String, SignalSnapshot.ComplianceTally> comp = new HashMap<>(compliance.size());
        for (Map.Entry<String, Tally> e : compliance.entrySet()) {
            Tally t = e.getValue();
            comp.put(e.getKey(), new SignalSnapshot.ComplianceTally(t.honored, t.violated, t.lastEventMs));
        }
        return new SignalSnapshot(
                uuid, name,
                blocksMined, oresMined, deaths, mobKills,
                soloMiningSeconds, sessionPlaySeconds,
                hoardedScore, distanceFromGroup, soloMiningRatio(),
                deepestY == Integer.MAX_VALUE ? 0 : deepestY,
                forbiddenWordHits, meanSentiment(), chatMessages,
                lastWorld, lastX, lastY, lastZ, lastLocationMs,
                firstOreThisSessionTaken, offeringHonoredThisSession,
                comp);
    }

    private double soloMiningRatio() {
        return miningSamples == 0 ? 0.0 : (double) aloneMiningSamples / (double) miningSamples;
    }

    private double meanSentiment() {
        return chatMessages == 0 ? 0.0 : sentimentSum / (double) chatMessages;
    }

    public boolean isDossierDirty() { return dossierDirty; }

    /** Clears the dossier-dirty flag (the flusher calls this after a successful write attempt). */
    public void clearDossierDirty() { this.dossierDirty = false; }

    /** Re-mark the dossier dirty (the flusher calls this after a hard, non-queued write failure). */
    public void markDossierDirty() { this.dossierDirty = true; }

    /** Snapshot + clear the set of custom_keys whose compliance changed since last flush. */
    public java.util.Set<String> drainDirtyComplianceKeys() {
        if (dirtyCompliance.isEmpty()) return java.util.Collections.emptySet();
        java.util.Set<String> keys = new java.util.HashSet<>(dirtyCompliance.keySet());
        for (String k : keys) dirtyCompliance.remove(k);
        return keys;
    }

    /** Read a single compliance tally (consistent copy). Never null. */
    public synchronized SignalSnapshot.ComplianceTally complianceFor(String customKey) {
        Tally t = customKey == null ? null : compliance.get(customKey);
        return t == null ? SignalSnapshot.ComplianceTally.ZERO
                : new SignalSnapshot.ComplianceTally(t.honored, t.violated, t.lastEventMs);
    }

    /**
     * Total honored / violated summed across ALL the ways this player has a tally for — the
     * whole-conduct read that surface townsfolk (e.g. Old Pell) use to judge who the player is
     * being at human scale. Returned as a {@code long[]{honored, violated}}, saturating, never
     * null. Cheap counter read under the same lock as the mutators, so it is a consistent pair.
     */
    public synchronized long[] complianceTotals() {
        long honored = 0, violated = 0;
        for (Tally t : compliance.values()) {
            honored = sat(honored + t.honored);
            violated = sat(violated + t.violated);
        }
        return new long[]{honored, violated};
    }

    /* ----------------------- internals ------------------------------- */

    /** Saturating increment guard: counters stay non-negative and never wrap. */
    private static long sat(long v) {
        return v < 0 ? Long.MAX_VALUE : v;
    }

    private static final class Tally {
        long honored;
        long violated;
        long lastEventMs;
    }
}
