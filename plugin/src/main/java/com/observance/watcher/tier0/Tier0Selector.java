package com.observance.watcher.tier0;

import com.observance.watcher.signal.SignalSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OBSERVER TIER-0 — the composure/observation SELECTOR (BUILD-PLAN §13). Given ONE player's real,
 * measured {@link SignalSnapshot}, it derives at most one GROUNDED observation: an implication that is
 * demonstrably TRUE of that specific player from their tracked behavior. If nothing is clearly true,
 * it returns {@link Optional#empty()} — SILENCE, never a guess. This is the grounding invariant made
 * mechanical: the watcher only ever names what it has actually seen.
 *
 * <p>PURE — no Bukkit, no I/O, no randomness that touches the world. Deterministic given a snapshot +
 * {@link Tier0Config}, so it is unit-self-testable with {@code javac} alone (mirrors {@code FlagGate}).
 *
 * <h2>Selection</h2>
 * Every candidate whose signal clears its threshold is scored by how far past the threshold it sits
 * (a normalized "how sure are we" strength). The single STRONGEST true observation is returned, so the
 * one line the player hears is the most defensible thing the watcher can say about them right now. A
 * tie breaks by the enum's declaration order (stable, deterministic).
 */
public final class Tier0Selector {

    private final Tier0Config cfg;

    public Tier0Selector(Tier0Config cfg) {
        this.cfg = cfg == null ? Tier0Config.defaults() : cfg;
    }

    public Tier0Config config() { return cfg; }

    /**
     * Derive the single strongest grounded observation for this snapshot, or empty (silence).
     * Null snapshot ⇒ empty. Disabled config ⇒ empty.
     */
    public Optional<Tier0Observation> select(SignalSnapshot s) {
        if (s == null || !cfg.enabled) return Optional.empty();

        List<Scored> hits = new ArrayList<>();
        for (Tier0Observation obs : Tier0Observation.values()) {
            double strength = strength(obs, s);
            if (strength > 0.0) hits.add(new Scored(obs, strength));
        }
        if (hits.isEmpty()) return Optional.empty();

        Scored best = hits.get(0);
        for (Scored h : hits) {
            if (h.strength > best.strength) best = h;
            // exact ties keep the earlier (lower ordinal) — hits is built in enum order, so no swap needed
        }
        return Optional.of(best.obs);
    }

    /**
     * How strongly the snapshot supports {@code obs}: {@code 0.0} = NOT true (below threshold → never
     * spoken), {@code > 0.0} = true, larger = more sure (further past the bar). Each branch requires
     * REAL substance so a fresh/idle player never trips a line. Every value read here is measured,
     * never inferred.
     */
    public double strength(Tier0Observation obs, SignalSnapshot s) {
        if (obs == null || s == null) return 0.0;
        return switch (obs) {

            // Mines alone AND has actually gone deep — the loner in the dark.
            case KEEPS_TO_THE_DARK -> {
                if (s.soloMiningSeconds() < cfg.soloMiningSecondsMin) yield 0.0;
                if (s.soloMiningRatio() < cfg.soloMiningRatioMin) yield 0.0;
                yield over(s.soloMiningRatio(), cfg.soloMiningRatioMin, 1.0);
            }

            // Farthest from the group (last sample). distanceFromGroup < 0 = unknown/alone-untracked.
            case ALWAYS_THE_FARTHEST -> {
                double d = s.distanceFromGroup();
                if (d < 0 || d < cfg.farthestDistanceMin) yield 0.0;
                yield over(d, cfg.farthestDistanceMin, cfg.farthestDistanceMin * 3.0);
            }

            // Carries a heavy hoard.
            case KEEPS_WHAT_IS_NEVER_USED -> {
                if (s.hoardedScore() < cfg.hoardScoreMin) yield 0.0;
                yield over(s.hoardedScore(), cfg.hoardScoreMin, cfg.hoardScoreMin * 3.0);
            }

            // Repeated deaths AND a genuinely deep run (deepestY at/below the dark line).
            case DIES_IN_THE_DARK -> {
                if (s.deaths() < cfg.deathsMin) yield 0.0;
                if (s.deepestY() > cfg.deepDeathYMax) yield 0.0;   // never got into the dark → not grounded
                yield over(s.deaths(), cfg.deathsMin, cfg.deathsMin * 4.0);
            }

            // Goes deeper than the depth line (lower Y = deeper; more-negative = stronger).
            case ALWAYS_GOES_DEEPER -> {
                if (s.deepestY() > cfg.deeperYMax) yield 0.0;
                // depth below the line, normalized against a further 64 blocks down.
                double below = (double) (cfg.deeperYMax - s.deepestY());
                yield over(below, 0.0, 64.0) + 1e-3;   // any block past the line is a (weak) true hit
            }

            // Takes a great deal, gives (mines as ore) almost none.
            case TAKES_AND_TAKES -> {
                if (s.blocksMined() < cfg.blocksMinedMin) yield 0.0;
                double oreRatio = s.blocksMined() == 0 ? 0.0
                        : (double) s.oresMined() / (double) s.blocksMined();
                if (oreRatio > cfg.takerOreRatioMax) yield 0.0;
                // more blocks mined past the floor = stronger.
                yield over(s.blocksMined(), cfg.blocksMinedMin, cfg.blocksMinedMin * 4.0);
            }
        };
    }

    /**
     * Normalize a value that has cleared {@code floor} into a strength in (0, 1], scaling up to
     * {@code ceil}. Guarantees a strictly-positive result for any value at/above the floor so a bare
     * pass still counts as a (weak) true hit; saturates at 1.0 by {@code ceil}.
     */
    private static double over(double value, double floor, double ceil) {
        double span = ceil - floor;
        if (span <= 0.0) return 1.0;
        double t = (value - floor) / span;
        if (t < 0.0) t = 0.0;
        if (t > 1.0) t = 1.0;
        // map [0,1] -> (0,1]: a bare-threshold pass yields a small but nonzero strength.
        return 0.05 + 0.95 * t;
    }

    private record Scored(Tier0Observation obs, double strength) { }
}
