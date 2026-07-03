package com.observance.watcher.signal;

import com.observance.watcher.data.rows.BaseRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight base detector (DESIGN §2.2). Aggregates two cheap, real signals into per-owner
 * clusters: {@code BlockPlaceEvent} positions (building density) and bed/respawn anchors
 * ({@code PlayerBedEnterEvent}). A container-density bump strengthens confidence. No heavy
 * geometry — a running centroid + count + bed anchor is enough to answer "this is Dana's base
 * around X,Z" so beats can prefer it.
 *
 * <p>Pure data — holds NO Bukkit references; fed on the main thread, drained async. Per-owner
 * single-cluster model (Phase 0): we track ONE primary cluster per player (their main base),
 * snapped/merged by a coarse radius so wandering builds don't fragment it. Multiple bases per
 * player is a Phase-1 refinement; the schema ({@code bases.id} = owner uuid here) already allows it.
 *
 * <p>Confidence = a saturating function of placement count, with a bonus for a bed anchor and
 * container density. Below the configured floor we simply don't emit a row (precision over recall).
 */
public final class BaseDetector {

    private final TrackerConfig cfg;
    private final Map<UUID, Cluster> clusters = new ConcurrentHashMap<>();

    public BaseDetector(TrackerConfig cfg) {
        this.cfg = cfg == null ? TrackerConfig.defaults() : cfg;
    }

    /** Record a block placement by a player (main thread). Null-safe. */
    public void recordPlacement(UUID owner, String world, int x, int y, int z) {
        if (owner == null || world == null || world.isEmpty()) return;
        Cluster c = clusters.computeIfAbsent(owner, k -> new Cluster(world, x, y, z));
        synchronized (c) {
            // If this placement is far from the current centroid, only adopt it once the player
            // has clearly relocated (we keep the densest cluster; a stray far block is ignored
            // unless it repeats). Coarse radius gate keeps the centroid stable.
            if (!c.world.equals(world)) {
                // Different world: only switch if the new world is accruing more activity.
                c.foreignHits++;
                if (c.foreignHits > c.placements) {
                    c.reset(world, x, y, z);
                }
                return;
            }
            long dx = x - c.cx, dz = z - c.cz;
            double dist2 = (double) dx * dx + (double) dz * dz;
            double r = cfg.baseClusterRadius();
            if (dist2 > (r * r) * 9.0) {       // far outside (3x radius): likely a different site
                c.foreignHits++;
                if (c.foreignHits > c.placements / 2 + 4) {
                    c.reset(world, x, y, z);   // player has relocated; re-anchor
                }
                return;
            }
            c.foreignHits = 0;
            c.placements++;
            // Running mean centroid (integer-safe).
            c.cx += (x - c.cx) / Math.max(1, c.placements);
            c.cy += (y - c.cy) / Math.max(1, c.placements);
            c.cz += (z - c.cz) / Math.max(1, c.placements);
            // Track spread for a sensible radius.
            c.maxR = Math.max(c.maxR, (int) Math.ceil(Math.sqrt(dist2)));
        }
    }

    /** Record a bed/respawn anchor — strong evidence of a home (main thread). */
    public void recordBedAnchor(UUID owner, String world, int x, int y, int z) {
        if (owner == null || world == null || world.isEmpty()) return;
        Cluster c = clusters.computeIfAbsent(owner, k -> new Cluster(world, x, y, z));
        synchronized (c) {
            c.hasBed = true;
            c.bedX = x; c.bedY = y; c.bedZ = z;
            if (c.placements == 0) {      // anchor an empty cluster to the bed
                c.world = world; c.cx = x; c.cy = y; c.cz = z;
            }
        }
    }

    /** Record a container open/place near a cluster — bumps confidence (main thread). */
    public void recordContainer(UUID owner) {
        if (owner == null) return;
        Cluster c = clusters.get(owner);
        if (c != null) synchronized (c) { c.containers++; }
    }

    /**
     * Build {@code BaseRow}s for every cluster confident enough to publish. The owner uuid is the
     * row id (stable → idempotent upsert; re-running never duplicates). Called from the async base
     * pass; reads a consistent copy under each cluster's lock.
     */
    public List<BaseRow> buildRows(String nowIso) {
        List<BaseRow> rows = new ArrayList<>();
        for (Map.Entry<UUID, Cluster> e : clusters.entrySet()) {
            Cluster c = e.getValue();
            BaseRow row;
            synchronized (c) {
                if (c.placements < cfg.baseMinPlacements() && !c.hasBed) continue;
                double conf = confidence(c);
                if (conf < cfg.baseConfidenceFloor()) continue;
                row = new BaseRow();
                // Upsert conflict-targets owner_uuid (see SupabaseClient.upsertBase); NEVER set id — a
                // UUID string cannot go into the bigint PK, and Gson omits the null id from the body.
                row.ownerUuid = e.getKey().toString();
                row.world = c.world;
                // Prefer the bed as the human "center" if present (where they sleep is "home").
                row.centerX = c.hasBed ? c.bedX : c.cx;
                row.centerY = c.hasBed ? c.bedY : c.cy;
                row.centerZ = c.hasBed ? c.bedZ : c.cz;
                row.radius = Math.max(cfg.baseClusterRadius() / 2, Math.min(c.maxR, cfg.baseClusterRadius() * 4));
                row.confidence = conf;
                row.updatedAt = nowIso;
            }
            rows.add(row);
        }
        return rows;
    }

    /** Confidence 0..1: placements saturate toward 0.8, bed +0.15, containers small bonus. */
    private double confidence(Cluster c) {
        double placeTerm = 0.8 * (1.0 - Math.exp(-(double) c.placements / (2.0 * Math.max(1, cfg.baseMinPlacements()))));
        double bedTerm = c.hasBed ? 0.15 : 0.0;
        double contTerm = Math.min(0.05, c.containers * 0.01);
        return Math.max(0.0, Math.min(1.0, placeTerm + bedTerm + contTerm));
    }

    /** Forget a player's cluster (e.g. admin reset). Null-safe. */
    public void forget(UUID owner) {
        if (owner != null) clusters.remove(owner);
    }

    public int clusterCount() { return clusters.size(); }

    /**
     * Live read of the group's single most-confident base cell — the prologue's retarget anchor
     * (cold-start §1.1: "the most-trafficked block the group passes through"). Returns the highest-
     * confidence cluster's human center (the bed if one is known, else the placement centroid) as
     * an {@link Anchor}, or {@code null} if no cluster yet clears the same floors {@link #buildRows}
     * uses (precision over recall — we never point the prologue at a stray build). Pure snapshot
     * under each cluster's lock; holds NO Bukkit references.
     */
    public Anchor primaryBase() {
        Anchor best = null;
        double bestConf = -1.0;
        for (Cluster c : clusters.values()) {
            synchronized (c) {
                if (c.placements < cfg.baseMinPlacements() && !c.hasBed) continue;
                double conf = confidence(c);
                if (conf < cfg.baseConfidenceFloor()) continue;
                if (conf <= bestConf) continue;
                bestConf = conf;
                int x = c.hasBed ? c.bedX : c.cx;
                int y = c.hasBed ? c.bedY : c.cy;
                int z = c.hasBed ? c.bedZ : c.cz;
                best = new Anchor(c.world, x, y, z, conf);
            }
        }
        return best;
    }

    /** Immutable snapshot of a base's anchor cell (world + block coords + confidence). */
    public static final class Anchor {
        public final String world;
        public final int x, y, z;
        public final double confidence;
        public Anchor(String world, int x, int y, int z, double confidence) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.confidence = confidence;
        }
    }

    private static final class Cluster {
        String world;
        int cx, cy, cz;          // running centroid
        int maxR;                // observed spread radius
        long placements;
        long containers;
        long foreignHits;        // placements far from centroid (relocation hysteresis)
        boolean hasBed;
        int bedX, bedY, bedZ;

        Cluster(String world, int x, int y, int z) {
            reset(world, x, y, z);
        }
        void reset(String world, int x, int y, int z) {
            this.world = world;
            this.cx = x; this.cy = y; this.cz = z;
            this.maxR = 0;
            this.placements = 0;
            this.foreignHits = 0;
            // bed + containers persist across a re-anchor on purpose? No — relocation = new home.
            this.hasBed = false;
            this.containers = 0;
        }
    }
}
