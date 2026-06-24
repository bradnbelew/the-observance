package com.observance.watcher.beats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player ATTENTION accumulator (red-team #3 — the creepy-beat selection brain + a restraint
 * guardrail). A bounded {@code [0,1]} score that RISES when a player is in a vulnerable, watch-worthy
 * situation (alone, deep, in the dark, at night) and DECAYS toward calm otherwise. The
 * {@link AmbientBeatGenerator} biases its focus toward the highest-attention player — the lonely deep
 * miner draws the Watcher, so the haunting feels like "it knows me" rather than random — and uses the
 * {@link Tier} to keep RESTRAINT: at {@code CALM} most considerations are skipped (the world stays
 * almost-normal, which IS the horror); only as attention climbs does something happen, and only then
 * may a bolder beat fire.
 *
 * <p>This is selection AND anti-spam: boldness is EARNED by situation, never random, and the score
 * decays so a scared moment doesn't snowball into a haywire cascade. Pure + in-memory + deterministic
 * given its inputs (so the scenario harness can exercise it). Confined to the MAIN-thread generator
 * tick, but backed by a concurrent map defensively.
 */
public final class Attention {

    public enum Tier { CALM, UNEASE, DREAD }

    /** Below this, a player is effectively calm and is pruned from the map. */
    private static final double FLOOR = 0.01;

    private final double decayPerTick;          // multiplicative decay toward 0, applied each tick
    private final double unease;                // tier threshold
    private final double dread;                 // tier threshold
    private final Map<UUID, Double> score = new ConcurrentHashMap<>();

    public Attention(double decayPerTick) {
        this(decayPerTick, 0.33, 0.66);
    }

    public Attention(double decayPerTick, double uneaseAt, double dreadAt) {
        this.decayPerTick = clamp01(decayPerTick <= 0 || decayPerTick >= 1 ? 0.82 : decayPerTick);
        this.unease = clamp01(uneaseAt);
        this.dread = clamp01(Math.max(uneaseAt, dreadAt));
    }

    /** Raise (or lower) a player's attention by {@code delta}, clamped to {@code [0,1]}. Returns the new score. */
    public double add(UUID id, double delta) {
        if (id == null) return 0.0;
        double v = clamp01(score.getOrDefault(id, 0.0) + delta);
        score.put(id, v);
        return v;
    }

    /** One decay step across ALL tracked players (call once per tick, before considering). Prunes calm ones. */
    public void decayAll() {
        score.replaceAll((k, v) -> v * decayPerTick);
        score.entrySet().removeIf(e -> e.getValue() < FLOOR);
    }

    public double score(UUID id) {
        return id == null ? 0.0 : score.getOrDefault(id, 0.0);
    }

    public Tier tier(UUID id) {
        double s = score(id);
        if (s >= dread) return Tier.DREAD;
        if (s >= unease) return Tier.UNEASE;
        return Tier.CALM;
    }

    /** Fraction of considerations to SKIP at this tier (restraint): calm is mostly quiet, dread never skips. */
    public static double skipProbability(Tier tier) {
        switch (tier) {
            case DREAD:  return 0.0;    // attention is high — let the moment happen
            case UNEASE: return 0.35;
            case CALM:
            default:     return 0.78;   // almost-normal; only rarely does something stir
        }
    }

    public void clear(UUID id) { if (id != null) score.remove(id); }
    public void clearAll() { score.clear(); }
    public int tracked() { return score.size(); }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
