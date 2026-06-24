package com.observance.watcher.beats.lib;

import org.bukkit.Bukkit;

/**
 * Capability gate + lazy provider for the optional FAWE schematic-paste path.
 *
 * <p>This class references NO {@code com.sk89q.*} type directly — it probes for FAWE by plugin name
 * and by {@link Class#forName} (string-based, so this class's own constant pool stays FAWE-free). Only
 * when the probe passes does it instantiate {@link FaweSchematicPaster} (the single class that imports
 * WorldEdit). On a server without FAWE, {@link #paster} returns {@code null} and {@code FaweSchematicPaster}
 * is never class-loaded — so no {@code NoClassDefFoundError} can ever surface. Construction is wrapped
 * in {@code catch (Throwable)} as a final belt-and-suspenders.
 */
final class Schematics {

    private Schematics() { }

    /** Cached capability result (probed once; FAWE presence doesn't change without a server restart). */
    private static volatile Boolean available;

    /**
     * A paster if FAWE/WorldEdit is present and usable, else {@code null}. Callers treat {@code null}
     * as "schematic path unavailable → skip" (never an error).
     */
    static SchematicPaster paster() {
        if (!isAvailable()) {
            return null;
        }
        try {
            return new FaweSchematicPaster();
        } catch (Throwable t) {
            // Class-load / link failure despite the probe — degrade silently.
            return null;
        }
    }

    private static boolean isAvailable() {
        Boolean a = available;
        if (a != null) {
            return a;
        }
        boolean ok;
        try {
            boolean pluginPresent = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")
                    || Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
            ok = pluginPresent && probeClasses();
        } catch (Throwable t) {
            ok = false;
        }
        available = ok;
        return ok;
    }

    /** Confirm the WorldEdit API classes we use are actually on the classpath (string lookup only). */
    private static boolean probeClasses() {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
            Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
