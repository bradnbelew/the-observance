package com.observance.watcher.signal;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable read-model of one player's tracked signals — the surface the beat engine, custom
 * engine, and the scalpel consume.
 *
 * <p>Why a snapshot: the live {@link PlayerSignals} is mutated on the main thread by listeners
 * and read by samplers; downstream engines (which may run async, e.g. the scalpel's confidence
 * gate) must NEVER read mutable internals across threads. {@link SignalTracker#snapshot(UUID)}
 * hands out one of these — a frozen, defensively-copied view that is safe to pass anywhere.
 *
 * <p>This is the contract that decouples the tracker from everything downstream. It is
 * lore-AGNOSTIC: it carries measured numbers and opaque {@code custom_key} tallies only — never
 * any authored story text. Confidence/targeting decisions are made by the consumer.
 */
public final class SignalSnapshot {

    private final UUID uuid;
    private final String name;

    // --- core measured signals (DESIGN §2.1) ---
    private final long blocksMined;
    private final long oresMined;
    private final long deaths;
    private final long mobKills;
    private final long soloMiningSeconds;
    private final long sessionPlaySeconds;
    private final double hoardedScore;
    private final double distanceFromGroup;          // last sampled, blocks; -1 if unknown/alone
    private final double soloMiningRatio;             // 0..1 fraction of sampled mining done alone
    private final int deepestY;                       // lowest Y a block was broken at this run
    private final long forbiddenWordHits;             // The Unspoken: count of forbidden chat words
    private final double chatSentiment;               // running mean sentiment, -1..+1 (0 neutral)
    private final long chatMessages;

    // --- last-known location snapshot (for targeting; may be stale) ---
    private final String lastWorld;
    private final int lastX;
    private final int lastY;
    private final int lastZ;
    private final long lastLocationMs;

    // --- per-session volatile flags ---
    private final boolean firstOreThisSessionTaken;   // already broke first ore this session
    private final boolean offeringHonoredThisSession; // gave back at a cairn this session

    // --- custom compliance tallies, keyed by opaque custom_key ---
    private final Map<String, ComplianceTally> compliance;

    SignalSnapshot(UUID uuid, String name,
                   long blocksMined, long oresMined, long deaths, long mobKills,
                   long soloMiningSeconds, long sessionPlaySeconds,
                   double hoardedScore, double distanceFromGroup, double soloMiningRatio,
                   int deepestY, long forbiddenWordHits, double chatSentiment, long chatMessages,
                   String lastWorld, int lastX, int lastY, int lastZ, long lastLocationMs,
                   boolean firstOreThisSessionTaken, boolean offeringHonoredThisSession,
                   Map<String, ComplianceTally> compliance) {
        this.uuid = uuid;
        this.name = name;
        this.blocksMined = blocksMined;
        this.oresMined = oresMined;
        this.deaths = deaths;
        this.mobKills = mobKills;
        this.soloMiningSeconds = soloMiningSeconds;
        this.sessionPlaySeconds = sessionPlaySeconds;
        this.hoardedScore = hoardedScore;
        this.distanceFromGroup = distanceFromGroup;
        this.soloMiningRatio = soloMiningRatio;
        this.deepestY = deepestY;
        this.forbiddenWordHits = forbiddenWordHits;
        this.chatSentiment = chatSentiment;
        this.chatMessages = chatMessages;
        this.lastWorld = lastWorld;
        this.lastX = lastX;
        this.lastY = lastY;
        this.lastZ = lastZ;
        this.lastLocationMs = lastLocationMs;
        this.firstOreThisSessionTaken = firstOreThisSessionTaken;
        this.offeringHonoredThisSession = offeringHonoredThisSession;
        this.compliance = compliance == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(compliance);
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }

    public long blocksMined() { return blocksMined; }
    public long oresMined() { return oresMined; }
    public long deaths() { return deaths; }
    public long mobKills() { return mobKills; }
    public long soloMiningSeconds() { return soloMiningSeconds; }
    public long sessionPlaySeconds() { return sessionPlaySeconds; }
    public double hoardedScore() { return hoardedScore; }

    /** Last sampled distance to the nearest other player, in blocks; -1 if unknown / solo. */
    public double distanceFromGroup() { return distanceFromGroup; }

    /** Fraction (0..1) of sampled mining ticks the player was alone for. High = the loner. */
    public double soloMiningRatio() { return soloMiningRatio; }

    public int deepestY() { return deepestY; }
    public long forbiddenWordHits() { return forbiddenWordHits; }

    /** Running mean chat sentiment, clamped -1..+1 (0 = neutral / unknown). */
    public double chatSentiment() { return chatSentiment; }
    public long chatMessages() { return chatMessages; }

    public String lastWorld() { return lastWorld; }
    public int lastX() { return lastX; }
    public int lastY() { return lastY; }
    public int lastZ() { return lastZ; }
    public long lastLocationMs() { return lastLocationMs; }

    public boolean firstOreThisSessionTaken() { return firstOreThisSessionTaken; }
    public boolean offeringHonoredThisSession() { return offeringHonoredThisSession; }

    /** Unmodifiable view of compliance tallies, keyed by opaque custom_key. Never null. */
    public Map<String, ComplianceTally> compliance() { return compliance; }

    /** Tally for one custom_key, or a zeroed tally if untracked (never null). */
    public ComplianceTally complianceFor(String customKey) {
        ComplianceTally t = compliance.get(customKey);
        return t == null ? ComplianceTally.ZERO : t;
    }

    /** Immutable honored/violated tally for a single custom. */
    public static final class ComplianceTally {
        public static final ComplianceTally ZERO = new ComplianceTally(0L, 0L, 0L);
        private final long honored;
        private final long violated;
        private final long lastEventMs;

        public ComplianceTally(long honored, long violated, long lastEventMs) {
            this.honored = honored;
            this.violated = violated;
            this.lastEventMs = lastEventMs;
        }

        public long honored() { return honored; }
        public long violated() { return violated; }
        public long lastEventMs() { return lastEventMs; }
        public long total() { return honored + violated; }

        /** Violated / total, 0 if no events. A 0.85+ here is "this player keeps breaking it." */
        public double violationRatio() {
            long t = total();
            return t == 0 ? 0.0 : (double) violated / (double) t;
        }
    }
}
