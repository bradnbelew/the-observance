package com.observance.watcher.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coarse-grid visit-count accumulator (DESIGN §2.2 heatmap). Block coords are quantized to CELL
 * units ({@code floorDiv(block, cellSize)}); each sampled presence bumps the cell's counter.
 *
 * <p>Pure data — holds NO Bukkit references. The {@link LocationSampler} feeds it on the main
 * thread (cheap atomic adds); the flusher drains DIRTY cells async and upserts {@code heatmap_cells}.
 *
 * <p><b>Why a cumulative running total, not a delta:</b> the client's upsert uses
 * {@code resolution=merge-duplicates}, which OVERWRITES the row's {@code visits} column rather than
 * adding to it. To stay correct without a read-modify-write race, this accumulator keeps the
 * authoritative running total per cell in memory and writes the ABSOLUTE value. The DB is a mirror
 * of memory, so a failed flush simply re-flushes next cycle (idempotent) and never double-counts.
 * The in-memory total is seeded from the DB once on first touch via {@link #seed} if desired; if
 * never seeded it starts from this process's lifetime, which is acceptable for a heatmap (it
 * converges and only ever grows).
 *
 * <p>Thread-safe via a {@link ConcurrentHashMap} of {@link AtomicLong}. A bounded distinct-cell cap
 * prevents unbounded memory if the DB is down for a very long session.
 */
public final class HeatmapAccumulator {

    /** Cell key — value object usable as a map key (equals/hashCode on all three fields). */
    public static final class CellKey {
        public final String world;
        public final int cellX;
        public final int cellZ;

        public CellKey(String world, int cellX, int cellZ) {
            this.world = world == null ? "" : world;
            this.cellX = cellX;
            this.cellZ = cellZ;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CellKey k)) return false;
            return cellX == k.cellX && cellZ == k.cellZ && world.equals(k.world);
        }
        @Override public int hashCode() {
            int h = world.hashCode();
            h = 31 * h + cellX;
            h = 31 * h + cellZ;
            return h;
        }
    }

    private final int cellSize;
    private final int maxCells;
    private final Map<CellKey, AtomicLong> totals = new ConcurrentHashMap<>();
    // Cells whose total changed since the last flush (so we only upsert what moved).
    private final Set<CellKey> dirty = ConcurrentHashMap.newKeySet();

    public HeatmapAccumulator(int cellSize, int maxCells) {
        this.cellSize = Math.max(1, cellSize);
        this.maxCells = Math.max(64, maxCells);
    }

    /** Seed a cell's known total from the DB (optional; safe to skip). */
    public void seed(String world, int cellX, int cellZ, long knownTotal) {
        if (world == null || world.isEmpty() || knownTotal < 0) return;
        CellKey key = new CellKey(world, cellX, cellZ);
        totals.computeIfAbsent(key, k -> new AtomicLong()).updateAndGet(cur -> Math.max(cur, knownTotal));
    }

    /** Bump the visit count for the cell containing this block position. Null-safe. */
    public void bump(String world, int blockX, int blockZ) {
        if (world == null || world.isEmpty()) return;
        CellKey key = new CellKey(world, Math.floorDiv(blockX, cellSize), Math.floorDiv(blockZ, cellSize));
        AtomicLong counter = totals.get(key);
        if (counter == null) {
            if (totals.size() >= maxCells) return; // refuse new keys when full; never OOM
            counter = totals.computeIfAbsent(key, k -> new AtomicLong());
        }
        counter.incrementAndGet();
        dirty.add(key);
    }

    /**
     * Snapshot the dirty cells with their ABSOLUTE running totals, and clear the dirty set. The
     * caller upserts each as {@code heatmap_cells(world,cell_x,cell_z,visits=total)}. If the flush
     * fails the caller calls {@link #markDirty} to re-queue them.
     */
    public List<CellSnapshot> drainDirty() {
        if (dirty.isEmpty()) return List.of();
        List<CellSnapshot> out = new ArrayList<>(dirty.size());
        // Copy keys first so concurrent bumps during iteration re-dirty cleanly.
        List<CellKey> keys = new ArrayList<>(dirty);
        for (CellKey key : keys) {
            dirty.remove(key);
            AtomicLong c = totals.get(key);
            if (c != null) {
                out.add(new CellSnapshot(key, c.get()));
            }
        }
        return out;
    }

    /** Re-mark cells dirty after a failed flush so they retry next cycle. */
    public void markDirty(List<CellSnapshot> snapshots) {
        if (snapshots == null) return;
        for (CellSnapshot s : snapshots) {
            if (s != null) dirty.add(s.key);
        }
    }

    public int distinctCells() { return totals.size(); }
    public int dirtyCount() { return dirty.size(); }
    public int cellSize() { return cellSize; }

    /** A cell + its absolute running visit total. */
    public static final class CellSnapshot {
        public final CellKey key;
        public final long total;
        public CellSnapshot(CellKey key, long total) {
            this.key = key;
            this.total = total;
        }
    }
}
