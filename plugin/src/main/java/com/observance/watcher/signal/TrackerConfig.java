package com.observance.watcher.signal;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Typed, immutable snapshot of the {@code tracker:} section of config.yml.
 *
 * <p>LORE-AGNOSTIC by construction: the only "story-adjacent" values here are opaque tuning
 * inputs — a list of forbidden WORDS (The Unspoken), which materials count as "ore", hoard
 * weighting, the depth threshold (The Deep Line), and the opaque {@code custom_key} strings the
 * tracker keys compliance under. No authored narrative text lives here; the human-readable name
 * of a custom lives in Supabase content, not in the plugin.
 *
 * <p>Every getter has a sane default so a missing/garbled config can never NPE a listener. The
 * forbidden-word set is lower-cased + normalized once, here, so the hot chat path is a cheap
 * {@code contains} on already-normalized tokens.
 */
public final class TrackerConfig {

    // custom_key constants — the Phase-0 cleanly-detectable customs (arc bible table).
    public static final String CUSTOM_BOW = "the_bow";
    public static final String CUSTOM_OFFERING = "the_offering";
    public static final String CUSTOM_UNSPOKEN = "the_unspoken";
    public static final String CUSTOM_KEPT_LIGHT = "the_kept_light";
    public static final String CUSTOM_DEEP_LINE = "the_deep_line";
    public static final String CUSTOM_SACRED_BEAST = "the_sacred_beast";
    public static final String CUSTOM_DARK_HOURS = "the_dark_hours";
    /** The ONE group-restraint latch (INV-17) — NOT one of the seven CUSTOM_KEYS above; see UnlitDeepListener. */
    public static final String CUSTOM_UNLIT_DEEP = "the_unlit_deep";

    private final boolean enabled;

    // forbidden words (The Unspoken) — normalized, lower-cased
    private final Set<String> forbiddenWords;

    // ore detection
    private final Set<String> oreMaterials;          // material names, upper-cased

    // hoard scoring: which materials count as "treasure" and their per-item weight
    private final List<HoardWeight> hoardWeights;
    private final double hoardScoreCap;

    // The Deep Line — breaking below this Y bare is a violation (depth taboo). A per-player
    // cooldown (ms) collapses one deep session into a single flag (anti-farm; precision).
    private final int deepLineY;
    private final boolean deepLineEnabled;
    private final long deepLineCooldownMs;

    // The Kept Light — per-base+player cooldown (ms) so a long night at a dark base produces one
    // measured tally per window, not one per sampler tick.
    private final long keptLightCooldownMs;

    // The Dark Hours — sleeping (PlayerBedEnterEvent) during a taboo moon phase is a violation.
    private final boolean darkHoursEnabled;
    private final Set<Integer> darkHoursMoonPhases;   // taboo phases in [0..7]; 0 = full moon
    private final long darkHoursCooldownMs;            // per-player anti-spam window

    // The Sacred Beast — entity types whose death (when tagged) is a violation; the tag itself
    // is a PDC key checked by the listener. Here we only carry the tag key name.
    private final String sacredBeastPdcKey;

    // The Unlit Deep — the ONE group-restraint latch (customs.unlit-deep + the restraint.enabled master
    // kill). A group-scoped cooldown (ms), not per-player: "one latch-edge per group per cooldown."
    private final boolean unlitDeepEnabled;
    private final boolean restraintEnabled;
    private final Set<String> unlitDeepFlameMaterials;   // material names, upper-cased
    private final int unlitDeepDeepLineY;
    private final Set<Integer> unlitDeepMoonPhases;       // taboo phases in [0..7]; empty → reuse dark-hours
    private final long unlitDeepCooldownMs;

    // base detection tuning
    private final int baseClusterRadius;             // blocks; place-events within this group up
    private final int baseMinPlacements;             // min block-places to call a cluster a base
    private final double baseConfidenceFloor;        // below this we don't upsert a base

    private TrackerConfig(boolean enabled,
                          Set<String> forbiddenWords, Set<String> oreMaterials,
                          List<HoardWeight> hoardWeights, double hoardScoreCap,
                          int deepLineY, boolean deepLineEnabled, long deepLineCooldownMs,
                          long keptLightCooldownMs,
                          boolean darkHoursEnabled, Set<Integer> darkHoursMoonPhases,
                          long darkHoursCooldownMs,
                          String sacredBeastPdcKey,
                          boolean unlitDeepEnabled, boolean restraintEnabled,
                          Set<String> unlitDeepFlameMaterials, int unlitDeepDeepLineY,
                          Set<Integer> unlitDeepMoonPhases, long unlitDeepCooldownMs,
                          int baseClusterRadius, int baseMinPlacements, double baseConfidenceFloor) {
        this.enabled = enabled;
        this.forbiddenWords = forbiddenWords;
        this.oreMaterials = oreMaterials;
        this.hoardWeights = hoardWeights;
        this.hoardScoreCap = hoardScoreCap;
        this.deepLineY = deepLineY;
        this.deepLineEnabled = deepLineEnabled;
        this.deepLineCooldownMs = deepLineCooldownMs;
        this.keptLightCooldownMs = keptLightCooldownMs;
        this.darkHoursEnabled = darkHoursEnabled;
        this.darkHoursMoonPhases = darkHoursMoonPhases;
        this.darkHoursCooldownMs = darkHoursCooldownMs;
        this.sacredBeastPdcKey = sacredBeastPdcKey;
        this.unlitDeepEnabled = unlitDeepEnabled;
        this.restraintEnabled = restraintEnabled;
        this.unlitDeepFlameMaterials = unlitDeepFlameMaterials;
        this.unlitDeepDeepLineY = unlitDeepDeepLineY;
        this.unlitDeepMoonPhases = unlitDeepMoonPhases;
        this.unlitDeepCooldownMs = unlitDeepCooldownMs;
        this.baseClusterRadius = baseClusterRadius;
        this.baseMinPlacements = baseMinPlacements;
        this.baseConfidenceFloor = baseConfidenceFloor;
    }

    /** Build from config; every read is defaulted + clamped. Never throws. */
    public static TrackerConfig from(FileConfiguration c) {
        ConfigurationSection t = c.getConfigurationSection("tracker");
        if (t == null) {
            return defaults();
        }
        boolean enabled = t.getBoolean("enabled", true);

        Set<String> forbidden = new HashSet<>();
        for (String w : t.getStringList("forbidden-words")) {
            String n = normalizeWord(w);
            if (!n.isEmpty()) forbidden.add(n);
        }

        Set<String> ores = new HashSet<>();
        List<String> oreList = t.getStringList("ore-materials");
        if (oreList.isEmpty()) {
            ores.addAll(defaultOres());
        } else {
            for (String m : oreList) {
                if (m != null && !m.isBlank()) ores.add(m.trim().toUpperCase(Locale.ROOT));
            }
        }

        List<HoardWeight> weights = new ArrayList<>();
        ConfigurationSection hw = t.getConfigurationSection("hoard-weights");
        if (hw != null) {
            for (String key : hw.getKeys(false)) {
                double weight = hw.getDouble(key, 0.0);
                if (key != null && !key.isBlank() && weight > 0) {
                    weights.add(new HoardWeight(key.trim().toUpperCase(Locale.ROOT), weight));
                }
            }
        }
        if (weights.isEmpty()) weights.addAll(defaultHoardWeights());

        double cap = clampD(t.getDouble("hoard-score-cap", 1000.0), 1.0, 1_000_000.0);

        ConfigurationSection deep = t.getConfigurationSection("deep-line");
        boolean deepEnabled = deep == null || deep.getBoolean("enabled", true);
        int deepY = deep == null ? -48 : clampI(deep.getInt("y-threshold", -48), -64, 320);
        // Per-player anti-farm window: one deep-line flag per N seconds (default 300s = 5 min).
        long deepCdMs = (deep == null ? 300 : clampI(deep.getInt("cooldown-seconds", 300), 0, 86400)) * 1000L;

        // The Kept Light — per-base+player anti-spam window (default 600s = 10 min).
        ConfigurationSection kl = t.getConfigurationSection("kept-light");
        long keptCdMs = (kl == null ? 600 : clampI(kl.getInt("cooldown-seconds", 600), 0, 86400)) * 1000L;

        // The Dark Hours — sleeping during a taboo moon phase. Default taboo phase = 0 (full moon),
        // mapped to the arc's "black moon". Phases are vanilla 0..7 from world.getFullTime()/24000 % 8.
        ConfigurationSection dh = t.getConfigurationSection("dark-hours");
        boolean dhEnabled = dh == null || dh.getBoolean("enabled", true);
        Set<Integer> dhPhases = new HashSet<>();
        if (dh != null) {
            for (int phase : dh.getIntegerList("taboo-moon-phases")) {
                if (phase >= 0 && phase <= 7) dhPhases.add(phase);
            }
        }
        if (dhPhases.isEmpty()) dhPhases.add(0);   // full moon by default
        long dhCdMs = (dh == null ? 60 : clampI(dh.getInt("cooldown-seconds", 60), 0, 86400)) * 1000L;

        String beastKey = t.getString("sacred-beast-pdc-key", "observance_sacred_beast");

        // The Unlit Deep — customs.unlit-deep + the restraint.enabled master kill are ROOT-level
        // sections (siblings of `tracker:`), not nested under it.
        boolean restraintOn = c.getBoolean("restraint.enabled", true);
        ConfigurationSection ud = c.getConfigurationSection("customs.unlit-deep");
        boolean udEnabled = ud == null || ud.getBoolean("enabled", true);
        Set<String> udMaterials = new HashSet<>();
        if (ud != null) {
            for (String m : ud.getStringList("flame-materials")) {
                if (m != null && !m.isBlank()) udMaterials.add(m.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (udMaterials.isEmpty()) udMaterials.addAll(defaultUnlitDeepFlameMaterials());
        int udY = ud == null ? -48 : clampI(ud.getInt("deep-line-y", -48), -64, 320);
        Set<Integer> udPhases = new HashSet<>();
        if (ud != null) {
            for (int phase : ud.getIntegerList("taboo-moon-phases")) {
                if (phase >= 0 && phase <= 7) udPhases.add(phase);
            }
        }
        if (udPhases.isEmpty()) udPhases.addAll(dhPhases);   // empty ⇒ reuse dark-hours' taboo set
        long udCdMs = (ud == null ? 300 : clampI(ud.getInt("cooldown-seconds", 300), 0, 86400)) * 1000L;

        ConfigurationSection base = t.getConfigurationSection("base-detection");
        int clusterR = base == null ? 24 : clampI(base.getInt("cluster-radius", 24), 4, 256);
        int minPlace = base == null ? 12 : clampI(base.getInt("min-placements", 12), 1, 100000);
        double confFloor = base == null ? 0.4 : clampD(base.getDouble("confidence-floor", 0.4), 0.0, 1.0);

        return new TrackerConfig(enabled, Collections.unmodifiableSet(forbidden),
                Collections.unmodifiableSet(ores), Collections.unmodifiableList(weights), cap,
                deepY, deepEnabled, deepCdMs, keptCdMs,
                dhEnabled, Collections.unmodifiableSet(dhPhases), dhCdMs,
                beastKey,
                udEnabled, restraintOn, Collections.unmodifiableSet(udMaterials), udY,
                Collections.unmodifiableSet(udPhases), udCdMs,
                clusterR, minPlace, confFloor);
    }

    /** All-defaults instance for when the section is entirely absent. */
    public static TrackerConfig defaults() {
        Set<Integer> dhPhases = new HashSet<>();
        dhPhases.add(0);   // full moon
        return new TrackerConfig(true,
                Collections.emptySet(),
                Collections.unmodifiableSet(new HashSet<>(defaultOres())),
                Collections.unmodifiableList(defaultHoardWeights()), 1000.0,
                -48, true, 300_000L, 600_000L,
                true, Collections.unmodifiableSet(dhPhases), 60_000L,
                "observance_sacred_beast",
                true, true, Collections.unmodifiableSet(new HashSet<>(defaultUnlitDeepFlameMaterials())),
                -48, Collections.unmodifiableSet(dhPhases), 300_000L,
                24, 12, 0.4);
    }

    /* ----------------------------- getters ---------------------------- */

    public boolean enabled() { return enabled; }
    public Set<String> forbiddenWords() { return forbiddenWords; }
    public boolean hasForbiddenWords() { return !forbiddenWords.isEmpty(); }

    public boolean isOre(String materialName) {
        return materialName != null && oreMaterials.contains(materialName.toUpperCase(Locale.ROOT));
    }

    public List<HoardWeight> hoardWeights() { return hoardWeights; }
    public double hoardScoreCap() { return hoardScoreCap; }

    public int deepLineY() { return deepLineY; }
    public boolean deepLineEnabled() { return deepLineEnabled; }
    public long deepLineCooldownMs() { return deepLineCooldownMs; }

    public long keptLightCooldownMs() { return keptLightCooldownMs; }

    public boolean darkHoursEnabled() { return darkHoursEnabled; }
    public long darkHoursCooldownMs() { return darkHoursCooldownMs; }

    /** Is the given vanilla moon phase (0..7) one of the taboo "dark hours" phases? */
    public boolean isTabooMoonPhase(int phase) {
        return darkHoursMoonPhases.contains(phase);
    }

    public String sacredBeastPdcKey() { return sacredBeastPdcKey; }

    public boolean unlitDeepEnabled() { return unlitDeepEnabled; }
    public boolean restraintEnabled() { return restraintEnabled; }
    public boolean isUnlitDeepFlameMaterial(String materialName) {
        return materialName != null && unlitDeepFlameMaterials.contains(materialName.toUpperCase(Locale.ROOT));
    }
    public int unlitDeepDeepLineY() { return unlitDeepDeepLineY; }
    public boolean isUnlitDeepTabooMoonPhase(int phase) { return unlitDeepMoonPhases.contains(phase); }
    public long unlitDeepCooldownMs() { return unlitDeepCooldownMs; }

    public int baseClusterRadius() { return baseClusterRadius; }
    public int baseMinPlacements() { return baseMinPlacements; }
    public double baseConfidenceFloor() { return baseConfidenceFloor; }

    /**
     * Scan a normalized chat message's tokens for any forbidden word. Returns true if the message
     * contains at least one. The caller passes the raw message; we tokenize + normalize here.
     */
    public boolean containsForbidden(String rawMessage) {
        if (forbiddenWords.isEmpty() || rawMessage == null || rawMessage.isEmpty()) return false;
        // Split on non-letter runs; normalize each token; cheap membership test.
        String lower = rawMessage.toLowerCase(Locale.ROOT);
        int n = lower.length();
        StringBuilder tok = new StringBuilder(16);
        for (int i = 0; i <= n; i++) {
            char ch = i < n ? lower.charAt(i) : ' ';
            if (Character.isLetterOrDigit(ch)) {
                tok.append(ch);
            } else if (tok.length() > 0) {
                if (forbiddenWords.contains(tok.toString())) return true;
                tok.setLength(0);
            }
        }
        return false;
    }

    /* ----------------------------- helpers ---------------------------- */

    private static String normalizeWord(String w) {
        if (w == null) return "";
        StringBuilder sb = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char c = Character.toLowerCase(w.charAt(i));
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static List<String> defaultOres() {
        return List.of(
                "COAL_ORE", "DEEPSLATE_COAL_ORE",
                "IRON_ORE", "DEEPSLATE_IRON_ORE",
                "COPPER_ORE", "DEEPSLATE_COPPER_ORE",
                "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "NETHER_GOLD_ORE",
                "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE",
                "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE",
                "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE",
                "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE",
                "NETHER_QUARTZ_ORE", "ANCIENT_DEBRIS");
    }

    private static List<String> defaultUnlitDeepFlameMaterials() {
        // "a sensible vanilla fire/torch/lantern/campfire default set" (config.yml customs.unlit-deep).
        return List.of(
                "FIRE", "SOUL_FIRE",
                "TORCH", "WALL_TORCH", "SOUL_TORCH", "SOUL_WALL_TORCH",
                "LANTERN", "SOUL_LANTERN",
                "CAMPFIRE", "SOUL_CAMPFIRE");
    }

    private static List<HoardWeight> defaultHoardWeights() {
        // "treasure" weighting — diamonds/netherite weigh most; the hoarder accumulates these.
        return List.of(
                new HoardWeight("NETHERITE_INGOT", 25.0),
                new HoardWeight("NETHERITE_SCRAP", 15.0),
                new HoardWeight("ANCIENT_DEBRIS", 15.0),
                new HoardWeight("DIAMOND", 10.0),
                new HoardWeight("DIAMOND_BLOCK", 90.0),
                new HoardWeight("EMERALD", 4.0),
                new HoardWeight("GOLD_INGOT", 2.0),
                new HoardWeight("GOLD_BLOCK", 18.0),
                new HoardWeight("IRON_INGOT", 1.0),
                new HoardWeight("IRON_BLOCK", 9.0));
    }

    private static int clampI(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clampD(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }

    /** One material→weight entry for hoard scoring. */
    public static final class HoardWeight {
        private final String material;   // upper-case material name
        private final double weight;
        HoardWeight(String material, double weight) {
            this.material = material;
            this.weight = weight;
        }
        public String material() { return material; }
        public double weight() { return weight; }
    }
}
