package com.observance.watcher.tier0;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * OBSERVER TIER-0 tuning: the thresholds the {@link Tier0Selector} tests against, plus the authored
 * implication line(s) per {@link Tier0Observation}. Built from the {@code tier0:} block of
 * {@code config.yml} by {@code Tier0ConfigLoader}, or via {@link #defaults()} for the self-test.
 *
 * <p>PURE DATA — no Bukkit references, so this + {@link Tier0Selector} compile and run under
 * {@code javac} alone (the repo self-test idiom). Lore text (the lines) lives in config, not code; the
 * hardcoded {@link Tier0Observation#defaultLine() default lines} are only a safety net.
 *
 * <p>All thresholds are chosen for PRECISION over recall: the selector should stay silent unless an
 * observation is clearly, defensibly true of the specific player. Silence is always a valid output.
 */
public final class Tier0Config {

    // --- master ---
    public final boolean enabled;

    // --- KEEPS_TO_THE_DARK: mines mostly alone, has gone deep ---
    public final double soloMiningRatioMin;      // fraction (0..1) of mining done alone
    public final long soloMiningSecondsMin;      // and at least this many solo-mining seconds (substance)

    // --- ALWAYS_THE_FARTHEST: last sampled distance to nearest other player ---
    public final double farthestDistanceMin;     // blocks

    // --- KEEPS_WHAT_IS_NEVER_USED: heavy hoard ---
    public final double hoardScoreMin;

    // --- DIES_IN_THE_DARK: repeated deaths + a genuinely deep run ---
    public final long deathsMin;
    public final int deepDeathYMax;              // deepest Y broken must be at/below this (proxy for "in the dark")

    // --- ALWAYS_GOES_DEEPER: deepest Y reached ---
    public final int deeperYMax;

    // --- TAKES_AND_TAKES: much broken, little of it ore/"given" ---
    public final long blocksMinedMin;
    public final double takerOreRatioMax;        // ores_mined / blocks_mined at/below this = "takes, gives little"

    // --- delivery ---
    /** Minimum minutes between two Tier-0 lines to the SAME player for the SAME observation. */
    public final int perObservationCooldownMinutes;

    /** observation.key() -> its candidate line(s). Empty list ⇒ fall back to the enum default line. */
    private final Map<Tier0Observation, List<String>> lines;

    public Tier0Config(boolean enabled,
                       double soloMiningRatioMin, long soloMiningSecondsMin,
                       double farthestDistanceMin,
                       double hoardScoreMin,
                       long deathsMin, int deepDeathYMax,
                       int deeperYMax,
                       long blocksMinedMin, double takerOreRatioMax,
                       int perObservationCooldownMinutes,
                       Map<Tier0Observation, List<String>> lines) {
        this.enabled = enabled;
        this.soloMiningRatioMin = soloMiningRatioMin;
        this.soloMiningSecondsMin = soloMiningSecondsMin;
        this.farthestDistanceMin = farthestDistanceMin;
        this.hoardScoreMin = hoardScoreMin;
        this.deathsMin = deathsMin;
        this.deepDeathYMax = deepDeathYMax;
        this.deeperYMax = deeperYMax;
        this.blocksMinedMin = blocksMinedMin;
        this.takerOreRatioMax = takerOreRatioMax;
        this.perObservationCooldownMinutes = Math.max(0, perObservationCooldownMinutes);
        this.lines = lines == null ? new EnumMap<>(Tier0Observation.class) : lines;
    }

    /**
     * The authored line(s) for an observation, or a single-element list of its
     * {@link Tier0Observation#defaultLine()} when config supplies none. Never null, never empty.
     */
    public List<String> linesFor(Tier0Observation obs) {
        if (obs == null) return List.of("");
        List<String> l = lines.get(obs);
        if (l == null || l.isEmpty()) return List.of(obs.defaultLine());
        return l;
    }

    /**
     * Deterministic, dependency-free defaults matching the shipped {@code config.yml} {@code tier0:}
     * block. Used by the self-test (no Bukkit) and as the fallback when the block is absent/malformed.
     */
    public static Tier0Config defaults() {
        return new Tier0Config(
                true,
                0.80, 600L,        // solo ratio >= 0.80 over >= 10 min of solo mining
                48.0,              // >= 48 blocks from the nearest other player
                600.0,            // hoard score >= 600
                3L, 24,            // >= 3 deaths, having broken a block at Y <= 24
                -16,               // deepest Y <= -16
                2000L, 0.02,       // >= 2000 blocks mined with <= 2% of them ore
                180,               // 3-hour per-observation cooldown per player
                new EnumMap<>(Tier0Observation.class));
    }
}
