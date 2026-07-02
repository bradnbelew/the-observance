package com.observance.watcher.signal;

import com.observance.watcher.tier0.Tier0Config;
import com.observance.watcher.tier0.Tier0Observation;
import com.observance.watcher.tier0.Tier0Selector;

import java.util.Optional;
import java.util.UUID;

/**
 * Self-test for OBSERVER TIER-0's grounded observation {@link Tier0Selector}. Proves the core
 * invariant of the cheapest tier: an observation is emitted ONLY when it is demonstrably TRUE of the
 * player's measured signals, and NOTHING (silence) is emitted otherwise — the watcher never guesses.
 *
 * <p>Lives in package {@code com.observance.watcher.signal} so it can use the package-private
 * {@link SignalSnapshot} constructor to fabricate signal states. Dependency-free + {@code main()}-
 * runnable (no Paper/gson/JUnit), mirroring {@code FlagGateSelfTest}:
 *
 *   javac -d out plugin/.../signal/SignalSnapshot.java plugin/.../tier0/*.java \
 *         plugin/.../signal/Tier0SelectorSelfTest.java
 *   java  -cp out com.observance.watcher.signal.Tier0SelectorSelfTest
 *
 * Exits non-zero on any failed assertion.
 */
public final class Tier0SelectorSelfTest {

    private static int failures = 0;

    private static void check(String label, boolean cond) {
        if (cond) {
            System.out.println("  ok   " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label);
        }
    }

    /** Minimal snapshot builder over the package-private constructor — everything zero/neutral by default. */
    private static final class B {
        long blocksMined, oresMined, deaths, mobKills, soloMiningSeconds, sessionPlaySeconds;
        double hoardedScore, distanceFromGroup = -1.0, soloMiningRatio;
        int deepestY = 0;
        long forbiddenWordHits, chatMessages;
        double chatSentiment;

        B solo(double ratio, long secs) { this.soloMiningRatio = ratio; this.soloMiningSeconds = secs; return this; }
        B distance(double d) { this.distanceFromGroup = d; return this; }
        B hoard(double h) { this.hoardedScore = h; return this; }
        B deaths(long d) { this.deaths = d; return this; }
        B deepestY(int y) { this.deepestY = y; return this; }
        B mined(long total, long ore) { this.blocksMined = total; this.oresMined = ore; return this; }

        SignalSnapshot build() {
            return new SignalSnapshot(
                    UUID.randomUUID(), "tester",
                    blocksMined, oresMined, deaths, mobKills,
                    soloMiningSeconds, sessionPlaySeconds,
                    hoardedScore, distanceFromGroup, soloMiningRatio,
                    deepestY, forbiddenWordHits, chatSentiment, chatMessages,
                    null, 0, 0, 0, 0L,
                    false, false,
                    null);
        }
    }

    public static void main(String[] args) {
        Tier0Config cfg = Tier0Config.defaults();
        Tier0Selector sel = new Tier0Selector(cfg);

        // 1) SILENCE is the default: a fresh/idle player trips NOTHING.
        check("empty signals -> silence", sel.select(new B().build()).isEmpty());
        check("null snapshot -> silence", sel.select(null).isEmpty());

        // 2) Disabled Tier-0 -> always silence, even on a signal that would otherwise fire.
        Tier0Selector off = new Tier0Selector(new Tier0Config(false,
                cfg.soloMiningRatioMin, cfg.soloMiningSecondsMin, cfg.farthestDistanceMin, cfg.hoardScoreMin,
                cfg.deathsMin, cfg.deepDeathYMax, cfg.deeperYMax, cfg.blocksMinedMin, cfg.takerOreRatioMax,
                cfg.perObservationCooldownMinutes, null));
        check("disabled -> silence", off.select(new B().hoard(9999).build()).isEmpty());

        // 3) Each observation fires when — and only when — its signal clearly clears the bar.

        // KEEPS_TO_THE_DARK: needs BOTH ratio and seconds.
        check("solo ratio high but too few seconds -> silence",
                sel.select(new B().solo(0.95, 60).build()).isEmpty());
        check("solo seconds high but ratio low -> silence",
                sel.select(new B().solo(0.5, 5000).build()).isEmpty());
        check("solo ratio+seconds both clear -> KEEPS_TO_THE_DARK",
                is(sel.select(new B().solo(0.95, 5000).build()), Tier0Observation.KEEPS_TO_THE_DARK));

        // ALWAYS_THE_FARTHEST: distanceFromGroup < 0 (unknown) must NOT fire.
        check("distance unknown (-1) -> silence",
                sel.select(new B().distance(-1).build()).isEmpty());
        check("distance below floor -> silence",
                sel.select(new B().distance(10).build()).isEmpty());
        check("distance far -> ALWAYS_THE_FARTHEST",
                is(sel.select(new B().distance(120).build()), Tier0Observation.ALWAYS_THE_FARTHEST));

        // KEEPS_WHAT_IS_NEVER_USED: heavy hoard.
        check("small hoard -> silence", sel.select(new B().hoard(100).build()).isEmpty());
        check("heavy hoard -> KEEPS_WHAT_IS_NEVER_USED",
                is(sel.select(new B().hoard(2000).build()), Tier0Observation.KEEPS_WHAT_IS_NEVER_USED));

        // DIES_IN_THE_DARK: deaths AND a deep run.
        check("deaths but shallow -> silence",
                sel.select(new B().deaths(9).deepestY(100).build()).isEmpty());
        check("deaths + deep -> DIES_IN_THE_DARK",
                is(sel.select(new B().deaths(9).deepestY(-40).build()), Tier0Observation.DIES_IN_THE_DARK));

        // ALWAYS_GOES_DEEPER: deepest Y past the line, with no other signal present.
        check("deepestY past the line -> ALWAYS_GOES_DEEPER",
                is(sel.select(new B().deepestY(-40).build()), Tier0Observation.ALWAYS_GOES_DEEPER));
        check("deepestY above the line -> silence",
                sel.select(new B().deepestY(0).build()).isEmpty());

        // TAKES_AND_TAKES: much broken, tiny ore ratio.
        check("few blocks -> silence", sel.select(new B().mined(50, 0).build()).isEmpty());
        check("many blocks but ore-rich -> silence",
                sel.select(new B().mined(5000, 4000).build()).isEmpty());
        check("many blocks + little ore -> TAKES_AND_TAKES",
                is(sel.select(new B().mined(5000, 10).build()), Tier0Observation.TAKES_AND_TAKES));

        // 4) STRONGEST wins: a huge hoard beats a bare deepest-Y pass (a much stronger true signal).
        Optional<Tier0Observation> both = sel.select(new B().hoard(5000).deepestY(-17).build());
        check("competing true signals -> the strongest (hoard) wins",
                is(both, Tier0Observation.KEEPS_WHAT_IS_NEVER_USED));

        // 5) Grounding at the boundary: exactly-at-threshold is a true hit (>=), just-under is silence.
        check("hoard exactly at threshold -> fires",
                is(sel.select(new B().hoard(cfg.hoardScoreMin).build()),
                        Tier0Observation.KEEPS_WHAT_IS_NEVER_USED));
        check("hoard one under threshold -> silence",
                sel.select(new B().hoard(cfg.hoardScoreMin - 0.01).build()).isEmpty());

        // 6) Every observation has a non-blank line to speak (no NPE / empty title path).
        for (Tier0Observation obs : Tier0Observation.values()) {
            check("line present for " + obs.key(),
                    !cfg.linesFor(obs).isEmpty() && cfg.linesFor(obs).get(0) != null
                            && !cfg.linesFor(obs).get(0).isBlank());
        }

        if (failures > 0) {
            System.out.println("\nTier0SelectorSelfTest: " + failures + " FAILED");
            System.exit(1);
        }
        System.out.println("\nTier0SelectorSelfTest: OK — Tier-0 speaks only what is grounded, else silence.");
    }

    private static boolean is(Optional<Tier0Observation> got, Tier0Observation want) {
        return got.isPresent() && got.get() == want;
    }
}
