package com.observance.watcher.config;

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

                    Site site = new Site(id, type, world, x, y, z, radius, vertical, protect, enabled);
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
