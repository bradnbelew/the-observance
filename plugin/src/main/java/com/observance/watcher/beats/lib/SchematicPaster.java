package com.observance.watcher.beats.lib;

import org.bukkit.Location;

import java.io.File;

/**
 * The FAWE-free seam for the schematic-paste path. Deliberately references NO WorldEdit/FAWE type, so
 * {@link SmallStructureBeat} (and everything else) can compile and run with FAWE entirely absent. The
 * one implementation that touches {@code com.sk89q.*} ({@link FaweSchematicPaster}) is loaded lazily,
 * ONLY after {@link Schematics} confirms FAWE is installed — so a server without FAWE never even
 * class-loads it (no {@code NoClassDefFoundError} can reach the engine).
 */
interface SchematicPaster {

    /**
     * The {@code {width, height, length}} of the schematic's block region, or {@code null} if the file
     * can't be read. Read up-front so the caller can footprint-check + size-cap BEFORE pasting.
     */
    int[] dimensions(File schemFile);

    /**
     * Paste the schematic so its region's MINIMUM corner lands at {@code base} (deterministic placement
     * independent of where the author stood at {@code //copy} time). {@code ignoreAir} skips the
     * schematic's air cells (never carves the player's world); entities are never copied. Returns
     * {@code true} on success. MUST NOT throw — any failure (bad file, FAWE API mismatch, edit error)
     * returns {@code false} so the beat degrades to a silent skip.
     */
    boolean pasteAtMinCorner(File schemFile, Location base, boolean ignoreAir);
}
