package com.observance.watcher.beats.lib;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Location;

import java.io.File;

/**
 * The ONLY class in the plugin that imports {@code com.sk89q.*}. Instantiated lazily by
 * {@link Schematics} ONLY when FAWE/WorldEdit is confirmed present, so it never class-loads on a
 * server without FAWE. WorldEdit is a {@code compileOnly} dependency (build.gradle) implemented by
 * FAWE 2.15.x at runtime. Every public method catches {@code Throwable} and degrades to a no-op /
 * {@code false} — a missing class, a method-signature drift, or an edit error becomes a silent skip,
 * never a crash (DESIGN anti-jank #5: graceful degradation).
 */
final class FaweSchematicPaster implements SchematicPaster {

    /** Loaded clipboards cached by absolute path — a .schem is parsed once, pasted many times. */
    private final java.util.concurrent.ConcurrentHashMap<String,
            com.sk89q.worldedit.extent.clipboard.Clipboard> cache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private com.sk89q.worldedit.extent.clipboard.Clipboard load(File f) {
        if (f == null || !f.isFile()) {
            return null;
        }
        try {
            return cache.computeIfAbsent(f.getAbsolutePath(), k -> {
                try {
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat fmt =
                            com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(f);
                    if (fmt == null) {
                        return null;
                    }
                    try (java.io.FileInputStream in = new java.io.FileInputStream(f);
                         com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = fmt.getReader(in)) {
                        return reader.read();
                    }
                } catch (Throwable t) {
                    return null;
                }
            });
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public int[] dimensions(File schemFile) {
        com.sk89q.worldedit.extent.clipboard.Clipboard c = load(schemFile);
        if (c == null) {
            return null;
        }
        try {
            com.sk89q.worldedit.math.BlockVector3 d = c.getDimensions();
            return new int[] { d.x(), d.y(), d.z() };
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean pasteAtMinCorner(File schemFile, Location base, boolean ignoreAir) {
        if (base == null || base.getWorld() == null) {
            return false;
        }
        com.sk89q.worldedit.extent.clipboard.Clipboard c = load(schemFile);
        if (c == null) {
            return false;
        }
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(base.getWorld());

            // Paste so the region's MIN corner lands exactly at `base`, regardless of the clipboard's
            // origin (where the author stood at //copy). dest = base + (origin - regionMin).
            com.sk89q.worldedit.math.BlockVector3 origin = c.getOrigin();
            com.sk89q.worldedit.math.BlockVector3 min = c.getRegion().getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 dest =
                    com.sk89q.worldedit.math.BlockVector3
                            .at(base.getBlockX(), base.getBlockY(), base.getBlockZ())
                            .add(origin.subtract(min));

            try (EditSession edit = WorldEdit.getInstance()
                    .newEditSessionBuilder().world(weWorld).build()) {
                com.sk89q.worldedit.function.operation.Operation op =
                        new com.sk89q.worldedit.session.ClipboardHolder(c)
                                .createPaste(edit)
                                .to(dest)
                                .ignoreAirBlocks(ignoreAir)
                                .copyEntities(false)
                                .build();
                com.sk89q.worldedit.function.operation.Operations.complete(op);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
