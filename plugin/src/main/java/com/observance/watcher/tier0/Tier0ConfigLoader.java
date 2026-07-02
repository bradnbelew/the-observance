package com.observance.watcher.tier0;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a {@link Tier0Config} from the {@code tier0:} block of {@code config.yml}. Bukkit-facing
 * (so it is kept OUT of the pure {@link Tier0Selector} self-test path). Every value degrades to the
 * matching {@link Tier0Config#defaults()} field, so a missing or malformed block still yields a
 * sane, working Tier-0 rather than a crash.
 */
public final class Tier0ConfigLoader {

    private Tier0ConfigLoader() { }

    public static Tier0Config from(FileConfiguration c) {
        Tier0Config d = Tier0Config.defaults();
        if (c == null) return d;
        ConfigurationSection t = c.getConfigurationSection("tier0");
        if (t == null) return d;

        boolean enabled = t.getBoolean("enabled", d.enabled);

        double soloRatio = t.getDouble("thresholds.solo-mining-ratio-min", d.soloMiningRatioMin);
        long soloSecs = t.getLong("thresholds.solo-mining-seconds-min", d.soloMiningSecondsMin);
        double farthest = t.getDouble("thresholds.farthest-distance-min", d.farthestDistanceMin);
        double hoard = t.getDouble("thresholds.hoard-score-min", d.hoardScoreMin);
        long deaths = t.getLong("thresholds.deaths-min", d.deathsMin);
        int deepDeathY = t.getInt("thresholds.deep-death-y-max", d.deepDeathYMax);
        int deeperY = t.getInt("thresholds.deeper-y-max", d.deeperYMax);
        long blocks = t.getLong("thresholds.blocks-mined-min", d.blocksMinedMin);
        double takerOre = t.getDouble("thresholds.taker-ore-ratio-max", d.takerOreRatioMax);
        int cooldown = t.getInt("per-observation-cooldown-minutes", d.perObservationCooldownMinutes);

        // Lines: tier0.lines.<observation_key> may be a single string OR a list of strings.
        Map<Tier0Observation, List<String>> lines = new EnumMap<>(Tier0Observation.class);
        ConfigurationSection linesSec = t.getConfigurationSection("lines");
        if (linesSec != null) {
            for (Tier0Observation obs : Tier0Observation.values()) {
                List<String> got = readLines(linesSec, obs.key());
                if (!got.isEmpty()) lines.put(obs, got);
            }
        }

        return new Tier0Config(
                enabled,
                soloRatio, soloSecs,
                farthest,
                hoard,
                deaths, deepDeathY,
                deeperY,
                blocks, takerOre,
                cooldown,
                lines);
    }

    /** Read a config value that may be a single string or a list of strings; trims + drops blanks. */
    private static List<String> readLines(ConfigurationSection sec, String key) {
        List<String> out = new ArrayList<>();
        if (sec == null || key == null) return out;
        if (sec.isList(key)) {
            for (String s : sec.getStringList(key)) {
                if (s != null && !s.isBlank()) out.add(clamp(s.trim()));
            }
        } else {
            String s = sec.getString(key, null);
            if (s != null && !s.isBlank()) out.add(clamp(s.trim()));
        }
        return out;
    }

    /** Bound a line so a malformed config can never push an unbounded title onto a client. */
    private static String clamp(String s) {
        String t = s.toLowerCase(Locale.ROOT);   // Watcher register: lowercase, always
        return t.length() > 120 ? t.substring(0, 120) : t;
    }
}
