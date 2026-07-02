package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * THE PRODUCER BOUNDARY, FAWE-FREE HALF ({@code backlog-unlockbeat-producers} / D6; INV-20).
 *
 * <p>A {@code reveal} flips block-state on blocks that <b>already exist</b> — it opens a slot, lights a
 * sconce, drops a barrier, swaps a single block's material — when a clue is solved and the group has
 * earned the next step. It NEVER pastes geometry (that is {@link SmallStructureBeat}'s schematic branch)
 * and NEVER overwrites a room (that is {@link RoomSwapBeat}). The three are the producer triad the
 * manifest declares: {@code reveal} (flip, no FAWE) vs {@code small_structure} (paste) vs
 * {@code room_swap} (clear-then-paste). This class imports no {@code com.sk89q.*} type and depends on
 * nothing optional — so {@code UnlockBeat}'s {@code step:"reveal"} works on a bare 1.21.x server, and
 * the M5 rite ships as a {@code reveal} slot-lighting interim until the FAWE set-piece lands (D6/D10).
 *
 * <p><b>Three flip kinds</b> (one per cell, mixed freely in a {@code cells[]} list):
 * <ul>
 *   <li><b>set</b> — change the cell to {@code material} (a single authored block: a barrier→air gate,
 *       a stone→sea-lantern slot). {@code material:"AIR"} is the canonical "open the way" flip.</li>
 *   <li><b>lit</b> — toggle a {@link Lightable} block's lit state ({@code lit:true|false}) without
 *       changing its type (light a furnace/redstone-lamp/sea-pickle that is already placed). A cell
 *       whose existing block is not lightable degrades to a no-op for that cell (never an error).</li>
 *   <li><b>state</b> — apply an authored block-data string verbatim ({@code data:"minecraft:..."}),
 *       for the rare slot whose orientation/waterlogged/level matters. Unparseable data → that cell is
 *       skipped, the rest still apply (all-or-nothing only within a cell, never across the list).</li>
 * </ul>
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "cells": [
 *     { "dx":0, "dy":0, "dz":0, "kind":"set", "material":"AIR" },          // open a barrier gate
 *     { "dx":1, "dy":2, "dz":0, "kind":"lit", "lit":true },                // light a placed lamp
 *     { "dx":0, "dy":0, "dz":1, "kind":"state", "data":"minecraft:lever[face=wall,powered=true]" }
 *   ],
 *   "require_existing": true     // (default true) a 'lit'/'state' cell that is currently air is skipped
 * }
 * }</pre>
 *
 * <p><b>Two reveal modes — the co-located-group fix (MF-9).</b> The old model was global-only: every flip
 * ran inside {@link #mutateWhenUnwitnessed}, which waits for an instant when NO player can see the block —
 * an instant that never comes when the group is standing together at the slot, so the reveal simply never
 * fired for a convened group (the whole point of a rite). This beat now has two modes:
 * <ul>
 *   <li><b>per-player illusion</b> (when the request carries a target player) — the flips are delivered as
 *       CLIENT-ONLY {@code sendBlockChange} to that one player via {@link #privateRevealWhenUnwitnessed}
 *       (the per-player half of the two-path reveal — the primitive now exists on {@link AbstractBeat}).
 *       Fanned over a scene by {@link GroupBeat}, every member of a co-located group discovers the change
 *       on their own client without anyone seeing it appear. The real world is never touched, so it is
 *       reveal-safe by construction and needs no protection registry entry;</li>
 *   <li><b>global world flip</b> (when there is no target — a site/world beat) — the original path: a real
 *       block-state flip gated on {@link #mutateWhenUnwitnessed}, for solo / unattended reveals.</li>
 * </ul>
 * The showrunner picks the mode by targeting: a group rite enqueues a per-player {@code reveal} (or wraps
 * it in a {@code group} beat); an unattended slot-lighting stays a world {@code reveal}.
 *
 * <p><b>Anti-jank.</b> Reveal-disciplined: in the world mode every flip runs inside {@link
 * #mutateWhenUnwitnessed} keyed on the FIRST cell's block, so the change is discovered, never witnessed
 * mutating; the per-player mode uses {@link #privateRevealWhenUnwitnessed} on the target. Idempotent two ways —
 * {@link AbstractBeat}'s in-process applied-set + the durable {@code beat_queue.status='fired'} guard the
 * row, and each flip is itself convergent (setting a block that is already the target material, or lighting
 * a lamp that is already lit, is a harmless re-apply). Bounded to {@value #MAX_CELLS} cells (a reveal is a
 * handful of slots, not a build). Touched blocks are handed to the {@link com.observance.watcher.beats.ProtectedRegistry}
 * so the anti-grief listener keeps an opened way open. MAIN-thread only; never throws.
 *
 * <p>{@link BeatCategory#DIRECTED}: a reveal is the earned consequence of a solved clue (enqueued by the
 * showrunner / {@link UnlockBeat}), not ambient drama on the budget.
 */
public final class RevealBeat extends AbstractBeat {

    /** A reveal opens a few slots, not a structure — bound the main-thread flip cost hard. */
    private static final int MAX_CELLS = 64;

    /** Offsets are authored relative to the anchor; clamp so a typo can't reach across the world. */
    private static final int OFFSET_LIMIT = 32;

    private enum Kind { SET, LIT, STATE }

    private record Flip(int dx, int dy, int dz, Kind kind,
                        Material material, Boolean lit, BlockData data) { }

    @Override public String name() { return "reveal"; }
    @Override public String description() { return "Flips block-state on existing blocks (open a slot, light a sconce) — no FAWE."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (parseFlips(req.payload()).isEmpty()) return false;
        return anchor(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        final List<Flip> flips = parseFlips(p);
        if (flips.isEmpty()) return BeatResult.skipped("no-cells");

        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return BeatResult.skipped("no-anchor");
        if (!anchor.getWorld().isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) {
            return BeatResult.skipped("anchor-unloaded");
        }
        final Block anchorBlock = anchor.getBlock();
        final boolean requireExisting = p.bool("require_existing", true);

        Flip lead = flips.get(0);
        Block leadBlock = anchorBlock.getRelative(lead.dx(), lead.dy(), lead.dz());

        // PER-PLAYER illusion mode (co-located-group fix): a targeted reveal lands as a client-only
        // sendBlockChange to that one player, gated on their own witness. The real world is untouched.
        Player targetPlayer = target(req);
        if (targetPlayer != null) {
            privateRevealWhenUnwitnessed(ctx, targetPlayer, leadBlock, () -> {
                for (Flip f : flips) {
                    Block b = anchorBlock.getRelative(f.dx(), f.dy(), f.dz());
                    if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) continue;
                    applyClientFlip(targetPlayer, b, f, requireExisting);
                }
            });
            return BeatResult.fired("reveal=" + flips.size() + " per-player");
        }

        // WORLD mode (solo / unattended): reveal-disciplined real flip. Flip the whole set only when the
        // FIRST cell is hidden. A reveal is small and local; one witness gate over the lead cell keeps the
        // change unwitnessed without the cost of gating each cell independently (which could half-apply
        // across retries).
        mutateWhenUnwitnessed(ctx, leadBlock, () -> {
            for (Flip f : flips) {
                Block b = anchorBlock.getRelative(f.dx(), f.dy(), f.dz());
                if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) continue;
                applyFlip(ctx, b, f, requireExisting);
            }
        });
        return BeatResult.fired("reveal=" + flips.size());
    }

    /* ------------------------------------------------------------------ */
    /*  Per-player client flip (illusion; the real world is untouched)     */
    /* ------------------------------------------------------------------ */

    /**
     * Deliver one flip as a CLIENT-ONLY block change to a single player. The real block never changes, so
     * a co-located group can each discover the reveal privately (reveal-safe by construction). A {@code lit}
     * flip is rendered by toggling the lit flag on the block's CURRENT data (so the client sees the same
     * lamp lit/unlit); a {@code set}/{@code state} flip sends the authored material/data. MAIN thread;
     * never throws (per-cell guarded — one bad cell never aborts the rest).
     */
    private static void applyClientFlip(Player pl, Block b, Flip f, boolean requireExisting) {
        try {
            switch (f.kind()) {
                case SET -> {
                    Material m = f.material();
                    if (m == null) return;
                    PerPlayer.fakeBlock(pl, b.getLocation(), m.createBlockData());
                }
                case LIT -> {
                    if (requireExisting && b.getType().isAir()) return;
                    BlockData bd = b.getBlockData();
                    if (bd instanceof Lightable lightable) {
                        boolean want = f.lit() != null && f.lit();
                        lightable.setLit(want);
                        PerPlayer.fakeBlock(pl, b.getLocation(), lightable);
                    }
                    // Non-lightable existing block → no-op (a 'lit' flip on stone is a misconfig, not a crash).
                }
                case STATE -> {
                    BlockData data = f.data();
                    if (data == null) return;
                    if (requireExisting && b.getType().isAir() && data.getMaterial().isAir()) return;
                    PerPlayer.fakeBlock(pl, b.getLocation(), data);
                }
            }
        } catch (Throwable ignored) {
            // One bad cell never aborts the rest — degrade that flip to a no-op.
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Flip application (per cell; convergent / idempotent)               */
    /* ------------------------------------------------------------------ */

    /** Apply one flip to one already-resolved block. MAIN thread. Never throws (per-cell guarded). */
    private static void applyFlip(BeatContext ctx, Block b, Flip f, boolean requireExisting) {
        try {
            switch (f.kind()) {
                case SET -> {
                    Material m = f.material();
                    if (m == null) return;
                    if (b.getType() == m) { protectIfSolid(ctx, b); return; } // already there — converge
                    b.setType(m, false);
                    protectIfSolid(ctx, b);
                }
                case LIT -> {
                    // Toggle the lit flag on an existing lightable block; never place one.
                    if (requireExisting && b.getType().isAir()) return;
                    BlockData bd = b.getBlockData();
                    if (bd instanceof Lightable lightable) {
                        boolean want = f.lit() != null && f.lit();
                        if (lightable.isLit() == want) return;        // already in the wanted state
                        lightable.setLit(want);
                        b.setBlockData(lightable, false);
                    }
                    // Non-lightable existing block → no-op (a 'lit' flip on stone is a misconfig, not a crash).
                }
                case STATE -> {
                    BlockData data = f.data();
                    if (data == null) return;
                    if (requireExisting && b.getType().isAir() && data.getMaterial().isAir()) return;
                    if (b.getBlockData().matches(data)) { protectIfSolid(ctx, b); return; } // converge
                    b.setBlockData(data, false);
                    protectIfSolid(ctx, b);
                }
            }
        } catch (Throwable ignored) {
            // One bad cell never aborts the rest — degrade that flip to a no-op.
        }
    }

    private static void protectIfSolid(BeatContext ctx, Block b) {
        try {
            if (!b.getType().isAir() && !b.isPassable()) ctx.protectedRegistry().protect(b);
        } catch (Throwable ignored) { }
    }

    /* ------------------------------------------------------------------ */
    /*  Parsing                                                            */
    /* ------------------------------------------------------------------ */

    static List<Flip> parseFlips(BeatPayload p) {
        List<Flip> out = new ArrayList<>();
        for (BeatPayload cell : p.objectList("cells")) {
            Flip f = parseCell(cell);
            if (f != null) out.add(f);
            if (out.size() >= MAX_CELLS) break;
        }
        return out;
    }

    /** Parse one cell into a Flip, or null if it carries no actionable change. Never throws. */
    private static Flip parseCell(BeatPayload c) {
        int dx = clampOffset(c.integer("dx", 0));
        int dy = clampOffset(c.integer("dy", 0));
        int dz = clampOffset(c.integer("dz", 0));
        Kind kind = parseKind(c.string("kind", null), c);
        switch (kind) {
            case LIT -> {
                if (!c.has("lit")) return null;
                return new Flip(dx, dy, dz, Kind.LIT, null, c.bool("lit", true), null);
            }
            case STATE -> {
                BlockData data = parseData(c.string("data", null));
                if (data == null) return null;
                return new Flip(dx, dy, dz, Kind.STATE, null, null, data);
            }
            case SET -> {
                Material m = matOrNull(c.string("material", null));
                if (m == null) return null;
                return new Flip(dx, dy, dz, Kind.SET, m, null, null);
            }
            default -> { return null; }
        }
    }

    /** Resolve the flip kind: explicit {@code kind}, else inferred from which field is present. */
    private static Kind parseKind(String raw, BeatPayload c) {
        if (raw != null && !raw.isBlank()) {
            switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "lit":   return Kind.LIT;
                case "state": return Kind.STATE;
                case "set":   return Kind.SET;
                default:      break; // fall through to inference
            }
        }
        if (c.has("lit"))      return Kind.LIT;
        if (c.has("data"))     return Kind.STATE;
        if (c.has("material")) return Kind.SET;
        return Kind.SET;
    }

    private static Material matOrNull(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable t) { return null; }
    }

    /** Parse an authored block-data string verbatim, or null if Bukkit rejects it. Never throws. */
    private static BlockData parseData(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return org.bukkit.Bukkit.createBlockData(s.trim());
        } catch (Throwable t) {
            return null;
        }
    }

    private static int clampOffset(int v) { return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, v)); }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure parsing the beat leans on: a {@code set} cell yields a material flip; a {@code lit}
     * cell without a {@code lit} field is dropped (no silent always-on); offsets clamp; a cell with no
     * actionable field is null. A regression here would either NPE the flip loop or flip the wrong cell.
     * Block-data parsing is exercised only when a server is present (it touches Bukkit), so it is not
     * asserted here — {@link #parseData} is wrapped and returns null off-server.
     */
    static boolean revealParseSelfTest() {
        // A set cell parses to a SET flip with the named material (AIR = "open the way").
        List<Flip> set = parseFlips(BeatPayload.parse(
                "{\"cells\":[{\"dx\":0,\"dy\":0,\"dz\":0,\"kind\":\"set\",\"material\":\"AIR\"}]}"));
        if (set.size() != 1 || set.get(0).kind() != Kind.SET || set.get(0).material() != Material.AIR) return false;

        // A lit cell with the flag parses; without the flag it is dropped (can't infer on/off).
        List<Flip> lit = parseFlips(BeatPayload.parse("{\"cells\":[{\"kind\":\"lit\",\"lit\":true}]}"));
        if (lit.size() != 1 || lit.get(0).kind() != Kind.LIT || !Boolean.TRUE.equals(lit.get(0).lit())) return false;
        if (!parseFlips(BeatPayload.parse("{\"cells\":[{\"kind\":\"lit\"}]}")).isEmpty()) return false;

        // Empty / missing cells → empty (canEnact returns false).
        if (!parseFlips(BeatPayload.empty()).isEmpty()) return false;

        // Offsets clamp to the limit (a typo can't reach across the world).
        List<Flip> far = parseFlips(BeatPayload.parse(
                "{\"cells\":[{\"dx\":9999,\"dy\":-9999,\"dz\":0,\"material\":\"STONE\"}]}"));
        if (far.size() != 1) return false;
        return far.get(0).dx() == OFFSET_LIMIT && far.get(0).dy() == -OFFSET_LIMIT;
    }
}
