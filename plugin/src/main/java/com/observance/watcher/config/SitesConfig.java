package com.observance.watcher.config;

import com.observance.watcher.structure.DeepHoldV4Plan;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads + holds the named placements from {@code sites.yml}. Immutable snapshot; rebuilt on
 * reload. Parsing is null-safe and defensive: a malformed site entry is skipped (with the issue
 * surfaced to the caller's logger), never fatal.
 */
public final class SitesConfig {

    private final Map<String, Site> byId;          // insertion-ordered
    private final String defaultWorld;
    private final List<String> warnings;           // human-readable parse issues

    private SitesConfig(Map<String, Site> byId, String defaultWorld, List<String> warnings) {
        this.byId = byId;
        this.defaultWorld = defaultWorld;
        this.warnings = warnings;
    }

    public static SitesConfig from(FileConfiguration c) {
        Map<String, Site> map = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        String defaultWorld = c.getString("default-world", "world");

        ConfigurationSection defaults = c.getConfigurationSection("defaults");
        int defRadius = defaults != null ? defaults.getInt("radius", 6) : 6;
        boolean defProtect = defaults != null ? defaults.getBoolean("protect", true) : true;
        int defVertical = defaults != null ? defaults.getInt("vertical-radius", 4) : 4;

        ConfigurationSection sites = c.getConfigurationSection("sites");
        if (sites != null) {
            for (String id : sites.getKeys(false)) {
                ConfigurationSection s = sites.getConfigurationSection(id);
                if (s == null) {
                    warnings.add("site '" + id + "' is not a section — skipped");
                    continue;
                }
                try {
                    String type = s.getString("type", "unknown");
                    String world = s.getString("world", defaultWorld);
                    Double x = readNullableDouble(s, "x");
                    Double y = readNullableDouble(s, "y");
                    Double z = readNullableDouble(s, "z");
                    int radius = s.getInt("radius", defRadius);
                    int vertical = s.getInt("vertical-radius", defVertical);
                    boolean protect = s.getBoolean("protect", defProtect);
                    boolean enabled = s.getBoolean("enabled", true);
                    // Optional: bind an answer-sign site to a single puzzle_key. Null = match all
                    // open puzzles (the non-linear web default).
                    String puzzleKey = s.getString("puzzle-key", s.getString("puzzle_key", null));
                    // Optional KEPT-LIGHT landmark: project a real beacon beam at this site's top (only
                    // the two canonically-lit sites set it; every other site defaults dark). See Site#beacon.
                    boolean beacon = s.getBoolean("visual_beacon", s.getBoolean("visual-beacon", false));

                    Site site = new Site(id, type, world, x, y, z, radius, vertical, protect, enabled,
                            puzzleKey, beacon);
                    map.put(id, site);
                } catch (Exception ex) {
                    warnings.add("site '" + id + "' failed to parse (" + ex.getClass().getSimpleName()
                            + ") — skipped");
                }
            }
        }

        return new SitesConfig(map, defaultWorld, warnings);
    }

    /** Empty config (used as a safe fallback if sites.yml is missing/corrupt). */
    public static SitesConfig empty() {
        return new SitesConfig(Collections.emptyMap(), "world", Collections.emptyList());
    }

    /**
     * Returns a NEW snapshot with {@code site} added (or replacing an existing site of the same id).
     * Immutable-copy semantics: this config is unchanged. Used for runtime site registration (e.g. the
     * {@code /observance placeroom} admin command) — the added site lives in memory only and does NOT
     * survive a reload/restart, because {@link #from} rebuilds solely from sites.yml. Null-safe: a null
     * site yields the same logical config.
     */
    public SitesConfig withSite(Site site) {
        if (site == null) return this;
        Map<String, Site> next = new LinkedHashMap<>(byId);
        next.put(site.id(), site);
        return new SitesConfig(next, defaultWorld, warnings);
    }

    /**
     * Make executable Deep Hold contracts authoritative over stale sites.yml metadata. Coordinates
     * remain the operator's persisted placement, but type, radius, vertical extent, enabled state,
     * and protection cannot be weakened or reshaped outside the V5 manifest/build controller.
     */
    public SitesConfig withCanonicalDeepHoldContracts() {
        Map<String, Site> next = new LinkedHashMap<>(byId);
        boolean changed = false;
        for (Site site : byId.values()) {
            Site canonical = canonicalDeepHoldSite(site);
            if (canonical == site) continue;
            next.put(site.id(), canonical);
            changed = true;
        }
        if (!changed) return this;
        List<String> nextWarnings = new ArrayList<>(warnings);
        nextWarnings.add("managed Deep Hold site metadata was normalized to the V5 executable contract");
        return new SitesConfig(next, defaultWorld, nextWarnings);
    }

    private Site canonicalDeepHoldSite(Site site) {
        if (site == null || site.id() == null) return site;
        DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(site.id());
        if (fixture != null) {
            return canonical(site, fixture.type(), fixture.radius(), fixture.verticalRadius());
        }
        if ("deep_hold_region".equals(site.id())) return canonical(site, "hold_region", 230, 66);
        if ("deep_hold_entry_stair".equals(site.id())) return canonical(site, "hold_region", 72, 34);
        if (site.id().startsWith("hold_answer_")) return canonical(site, "answer_sign", 1, 2);
        if (site.id().startsWith("hold_gate_")) {
            int[] span = switch (site.id().substring("hold_gate_".length())) {
                case "keeper", "archive", "deep" -> new int[]{12, 22};
                case "undercroft" -> new int[]{12, 20};
                case "prior", "dread" -> new int[]{8, 18};
                case "accepting", "coda" -> new int[]{14, 24};
                default -> null;
            };
            if (span != null) return canonical(site, "hold_gate", span[0], span[1]);
        }
        return site;
    }

    private Site canonical(Site source, String type, int radius, int vertical) {
        if (type.equals(source.type()) && radius == source.radius()
                && vertical == source.verticalRadius() && source.protect() && source.enabled()
                && !source.beacon()) return source;
        return new Site(source.id(), type, source.worldName(), source.x(), source.y(), source.z(),
                radius, vertical, true, true, source.puzzleKey(), false);
    }

    /** A site by id, or null. */
    public Site get(String id) {
        return id == null ? null : byId.get(id);
    }

    /** All sites (insertion order), including unplaced/disabled ones. */
    public List<Site> all() {
        return new ArrayList<>(byId.values());
    }

    /** Only the sites that are placed + enabled (engine-eligible). */
    public List<Site> placed() {
        List<Site> out = new ArrayList<>();
        for (Site s : byId.values()) {
            if (s.isPlaced()) out.add(s);
        }
        return out;
    }

    /** Placed + enabled sites of a given type (e.g. "bow_marker", "offering_cairn"). */
    public List<Site> placedOfType(String type) {
        List<Site> out = new ArrayList<>();
        if (type == null) return out;
        for (Site s : byId.values()) {
            if (s.isPlaced() && type.equals(s.type())) out.add(s);
        }
        return out;
    }

    public String defaultWorld() { return defaultWorld; }
    public int size() { return byId.size(); }
    public int placedCount() { return placed().size(); }
    public List<String> warnings() { return Collections.unmodifiableList(warnings); }

    private static Double readNullableDouble(ConfigurationSection s, String key) {
        if (!s.isSet(key)) return null;
        Object raw = s.get(key);
        if (raw == null) return null;            // explicit null sentinel = placeholder
        if (raw instanceof Number n) return n.doubleValue();
        // tolerate a stringified number
        try {
            return Double.parseDouble(raw.toString().trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
