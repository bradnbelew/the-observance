package com.observance.watcher.morrow;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Canonical Minecraft copper-weathering families used by bounded Morrow structures. */
public final class CopperAgingPolicy {
    private static final Map<String, Stage> STAGES = Map.ofEntries(
            entry("copper_block", "block", 0),
            entry("exposed_copper", "block", 1),
            entry("weathered_copper", "block", 2),
            entry("oxidized_copper", "block", 3),
            entry("cut_copper", "cut", 0),
            entry("exposed_cut_copper", "cut", 1),
            entry("weathered_cut_copper", "cut", 2),
            entry("oxidized_cut_copper", "cut", 3),
            entry("chiseled_copper", "chiseled", 0),
            entry("exposed_chiseled_copper", "chiseled", 1),
            entry("weathered_chiseled_copper", "chiseled", 2),
            entry("oxidized_chiseled_copper", "chiseled", 3),
            entry("copper_grate", "grate", 0),
            entry("exposed_copper_grate", "grate", 1),
            entry("weathered_copper_grate", "grate", 2),
            entry("oxidized_copper_grate", "grate", 3),
            entry("copper_bulb", "bulb", 0),
            entry("exposed_copper_bulb", "bulb", 1),
            entry("weathered_copper_bulb", "bulb", 2),
            entry("oxidized_copper_bulb", "bulb", 3));

    private CopperAgingPolicy() { }

    /** True only for a later natural oxidation stage in the same unwaxed material family. */
    public static boolean isNaturalAging(String before, String after) {
        Stage source = stage(before);
        Stage target = stage(after);
        return source != null && target != null
                && source.family().equals(target.family())
                && target.index() > source.index();
    }

    public static boolean isWeatherable(String blockData) {
        Stage stage = stage(blockData);
        return stage != null && stage.index() < 3;
    }

    /** Restores only later-stage ambient aging; foreign, waxed, and reverse-stage drift is untouched. */
    public static <C> int restoreNaturalAging(
            Map<C, String> manifest,
            Function<C, String> reader,
            BiConsumer<C, String> writer) {
        int repairs = 0;
        for (Map.Entry<C, String> entry : manifest.entrySet()) {
            String actual = reader.apply(entry.getKey());
            if (isNaturalAging(entry.getValue(), actual)) {
                writer.accept(entry.getKey(), entry.getValue());
                repairs++;
            }
        }
        return repairs;
    }

    private static Stage stage(String blockData) {
        if (blockData == null) return null;
        int properties = blockData.indexOf('[');
        String key = properties < 0 ? blockData : blockData.substring(0, properties);
        if (key.startsWith("minecraft:")) key = key.substring("minecraft:".length());
        if (key.startsWith("waxed_")) return null;
        return STAGES.get(key);
    }

    private static Map.Entry<String, Stage> entry(String key, String family, int index) {
        return Map.entry(key, new Stage(family, index));
    }

    private record Stage(String family, int index) { }
}
