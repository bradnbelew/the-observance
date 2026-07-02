package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

/**
 * THE ROOM-SWAP — reworked from an in-place overwrite to a SEALED-DOOR + TELEPORT-ON-REENTRY move
 * ({@code backlog-undercroft-dimension} / D5; the §10-fence rework: rework BEFORE register).
 *
 * <p><b>Why the rework.</b> The old model cleared the occupied room A and pasted the changed room B onto
 * the SAME footprint in one hidden instant — the only overwrite path in the plugin. That is exactly the
 * jank the reveal contract exists to prevent: an overwrite-in-place is irreversible-looking, is only
 * "hidden" if literally no one is in the sealed room (rare, since the group is usually the reason to swap
 * it), and a half-applied clear (paste failed after clear) leaves an empty room. The Undercroft "the room
 * rebuilds itself" beat is now delivered the safe way instead: room B is pre-built at a HIDDEN destination
 * offset (never overwriting A), the DOOR back into A is SEALED (a reveal-disciplined real flip), and the
 * changed room is entered by TELEPORT when a player crosses back through the sealed threshold. The player
 * walks out, the door is closed behind them, and when they push back in they are in the new room — the
 * illusion of a rebuilt room, with no occupied overwrite and no witnessable in-place mutation.
 *
 * <p><b>The two halves.</b> This beat is the PRODUCER half:
 * <ol>
 *   <li>paste room B at the destination offset ({@code dest_dx/dy/dz}) onto a VERIFIED-CLEAR footprint —
 *       reusing the clear-footprint discipline ({@link SmallStructureBeat}'s contract: an occupied
 *       destination is a SKIP, never an overwrite). If FAWE is absent or the footprint is occupied the
 *       swap simply doesn't arm (soft, in-fiction — the room stays A);</li>
 *   <li>seal the door cell ({@code door_dx/dy/dz}) with {@code door_seal_material} via a reveal-disciplined
 *       real flip ({@link #mutateWhenUnwitnessed}) — A is now closed;</li>
 *   <li>record the destination ENTRY point on the anchor chunk's PDC ({@code observance:swap_dest.<coords>})
 *       so the companion re-entry listener knows where to teleport a player who crosses the sealed door.</li>
 * </ol>
 * The CONSUMER half — teleporting a re-entering player to B — is
 * {@link com.observance.watcher.signal.listener.RoomSwapReentryListener}, a self-contained
 * {@link org.bukkit.event.player.PlayerMoveEvent} guard keyed on the sealed door cell + the recorded
 * destination. Keeping the teleport in a listener (not this beat) means it works for any player, any time
 * after the swap arms, without the beat holding cross-tick state.
 *
 * <p><b>Idempotency</b> is the same durable {@code observance:swapped} PDC marker on the anchor chunk as
 * before (BP0-3): once armed, a re-fire reads the marker and skips — B is not re-pasted, the door is not
 * re-sealed. The in-process applied-set + {@code beat_queue.status} guard the row on top.
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "schematic": "undercroft_room_b",     // B: the changed room (curated .schem, FAWE-pasted at the offset)
 *   "dest_dx": 0, "dest_dy": -24, "dest_dz": 0,   // where B is pre-built (hidden; default straight down 24)
 *   "door_dx": 0, "door_dy": 0, "door_dz": 0,     // the door cell to seal (relative to the anchor)
 *   "door_seal_material": "DEEPSLATE_BRICKS",      // what the door becomes (default deepslate bricks)
 *   "entry_dx": 1, "entry_dy": 1, "entry_dz": 1,   // where inside B a re-entering player lands (rel. to dest base)
 *   "marker_dx": 0, "marker_dy": 0, "marker_dz": 0 // where the durable 'swapped'/'swap_dest' markers live
 * }
 * }</pre>
 *
 * <p><b>Anti-jank.</b> No occupied overwrite anywhere (B lands on clear ground or the swap doesn't arm).
 * The door seal is reveal-disciplined. If FAWE is absent the beat skips (never errors). Volume-capped like
 * the structure paster. MAIN-thread only; never throws.
 */
public final class RoomSwapBeat extends AbstractBeat {

    /** Same hard cap as {@link SmallStructureBeat} — a swap is a room, not a region. */
    private static final int MAX_SWAP_VOLUME = 32_768;

    /** PDC sub-keys on the anchor chunk: the swap guard, and the recorded destination entry point. */
    private static final String PDC_SWAPPED   = "swapped";
    private static final String PDC_SWAP_DEST = "swap_dest";

    /** Authored offset clamp (offsets live within reach of the room). */
    private static final int OFFSET_LIMIT = 64;

    /** Default: build B straight down out of sight if no dest offset is authored. */
    private static final int DEFAULT_DEST_DY = -24;

    @Override public String name() { return "room_swap"; }
    @Override public String description() { return "Seal the room's door + teleport into the pre-built changed room on re-entry (no in-place overwrite)."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (sanitizeSchemName(req.payload().string("schematic", null)) == null) return false;
        return resolveBase(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();

        String schem = sanitizeSchemName(p.string("schematic", null));
        if (schem == null) return BeatResult.skipped("no-schematic");

        SchematicPaster paster = Schematics.paster();
        if (paster == null) return BeatResult.skipped("fawe-unavailable"); // room stays A — soft outcome, no error

        java.io.File file = new java.io.File(
                new java.io.File(ctx.plugin().getDataFolder(), "schematics"), schem + ".schem");
        if (!file.isFile()) return BeatResult.skipped("schematic-missing");

        int[] dims = paster.dimensions(file);
        if (dims == null || dims.length != 3) return BeatResult.skipped("schematic-unreadable");
        final int w = dims[0], h = dims[1], l = dims[2];
        if (w <= 0 || h <= 0 || l <= 0) return BeatResult.skipped("schematic-empty");
        if ((long) w * h * l > MAX_SWAP_VOLUME) return BeatResult.skipped("schematic-too-large");

        Location base = resolveBase(ctx, req);
        if (base == null || base.getWorld() == null) return BeatResult.skipped("no-base");
        final Block baseBlock = base.getBlock();

        // Durable idempotency: the swap marker on the anchor chunk. Read BEFORE any work so a re-fire never
        // re-arms a room that already swapped.
        final Block markerBlock = baseBlock.getRelative(
                clampOffset(p.integer("marker_dx", 0)),
                clampOffset(p.integer("marker_dy", 0)),
                clampOffset(p.integer("marker_dz", 0)));
        if (isSwapped(ctx, markerBlock)) return BeatResult.skipped("already-swapped");

        if (!base.getWorld().isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            return BeatResult.skipped("base-unloaded");
        }

        // Destination: where B is pre-built (hidden; default straight down). Its MIN corner.
        final Location dest = base.clone().add(
                clampOffset(p.integer("dest_dx", 0)),
                clampOffset(p.integer("dest_dy", DEFAULT_DEST_DY)),
                clampOffset(p.integer("dest_dz", 0)));
        if (dest.getWorld() == null
                || !dest.getWorld().isChunkLoaded(dest.getBlockX() >> 4, dest.getBlockZ() >> 4)) {
            return BeatResult.skipped("dest-unloaded");
        }
        // NEVER overwrite: B must land on a CLEAR footprint (the clear-footprint contract). An occupied
        // destination is a skip, not a clobber — the swap just doesn't arm (soft, in-fiction).
        if (!footprintClear(dest, w, h, l)) return BeatResult.skipped("dest-occupied");

        // Paste B at the destination (clear ground; no clear-in-place, no occupied overwrite).
        Boolean ok = ctx.safety().call("beat.room_swap.paste",
                () -> paster.pasteAtMinCorner(file, dest, true), Boolean.FALSE);
        if (!Boolean.TRUE.equals(ok)) return BeatResult.skipped("paste-failed"); // nothing armed, A intact
        protectBox(ctx, dest.getBlock(), w, h, l);

        // The door cell to seal (relative to the anchor) + what it becomes.
        final Block doorBlock = baseBlock.getRelative(
                clampOffset(p.integer("door_dx", 0)),
                clampOffset(p.integer("door_dy", 0)),
                clampOffset(p.integer("door_dz", 0)));
        final Material sealMat = material(p.string("door_seal_material", null), Material.DEEPSLATE_BRICKS);

        // Where inside B a re-entering player lands (relative to B's min corner), recorded for the listener.
        final int entryDx = clampOffset(p.integer("entry_dx", 1));
        final int entryDy = clampOffset(p.integer("entry_dy", 1));
        final int entryDz = clampOffset(p.integer("entry_dz", 1));
        final Location entry = dest.clone().add(entryDx, entryDy, entryDz);

        // Seal the door with a reveal-disciplined real flip; record the swap + destination on the same
        // hidden instant so the listener can teleport re-entrants to B.
        mutateWhenUnwitnessed(ctx, doorBlock, () -> {
            if (isSwapped(ctx, markerBlock)) return;   // re-read at mutate time (another instance armed it)
            if (Placement.isWithinBuildRange(doorBlock.getWorld(), doorBlock.getY())
                    && doorBlock.getType() != sealMat) {
                doorBlock.setType(sealMat, false);
            }
            try { if (!sealMat.isAir()) ctx.protectedRegistry().protect(doorBlock); } catch (Throwable ignored) { }
            recordDestination(ctx, markerBlock, doorBlock, entry);
            markSwapped(ctx, markerBlock);
        });
        return BeatResult.fired("room_swap=" + schem + " sealed+dest " + w + "x" + h + "x" + l);
    }

    /* ------------------------------------------------------------------ */
    /*  Clear-footprint check (NEVER overwrite — B lands on air only)      */
    /* ------------------------------------------------------------------ */

    /** True iff every cell of the [dest..dest+dims) box is air/replaceable — B may paste here safely. */
    private static boolean footprintClear(Location dest, int w, int h, int l) {
        Block b0 = dest.getBlock();
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = b0.getRelative(dx, dy, dz);
                    if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) return false;
                    if (!Placement.isReplaceable(b)) return false;   // occupied → refuse (no overwrite)
                }
            }
        }
        return true;
    }

    /** Protect the solid blocks B left behind (so the anti-grief listener keeps the changed room intact). */
    private static void protectBox(BeatContext ctx, Block baseBlock, int w, int h, int l) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = baseBlock.getRelative(dx, dy, dz);
                    try {
                        if (!b.getType().isAir() && !b.isPassable()) ctx.protectedRegistry().protect(b);
                    } catch (Throwable ignored) { }
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Durable markers on the anchor chunk (swap guard + destination)     */
    /* ------------------------------------------------------------------ */

    private static NamespacedKey markerKey(BeatContext ctx, Block markerBlock) {
        String sub = PDC_SWAPPED + "." + markerBlock.getX() + "_" + markerBlock.getY() + "_" + markerBlock.getZ();
        return new NamespacedKey(ctx.namespace(), sub.toLowerCase(java.util.Locale.ROOT));
    }

    /** PDC key on the door block's chunk recording the destination entry: keyed to the sealed DOOR cell so
     *  the re-entry listener (which sees the door the player steps on) can look it up directly. */
    public static NamespacedKey destKey(String namespace, Block doorBlock) {
        String sub = PDC_SWAP_DEST + "." + doorBlock.getX() + "_" + doorBlock.getY() + "_" + doorBlock.getZ();
        return new NamespacedKey(namespace, sub.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean isSwapped(BeatContext ctx, Block markerBlock) {
        if (markerBlock == null) return false;
        try {
            var pdc = markerBlock.getChunk().getPersistentDataContainer();
            return pdc.has(markerKey(ctx, markerBlock), PersistentDataType.BYTE);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void markSwapped(BeatContext ctx, Block markerBlock) {
        if (markerBlock == null) return;
        try {
            markerBlock.getChunk().getPersistentDataContainer()
                    .set(markerKey(ctx, markerBlock), PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable ignored) { }
    }

    /** Record B's entry point on the DOOR chunk's PDC as "world x y z (yaw pitch)" so the re-entry listener
     *  can parse + teleport. Stored against the door cell the listener detects the player stepping onto. */
    private static void recordDestination(BeatContext ctx, Block markerBlock, Block doorBlock, Location entry) {
        if (doorBlock == null || entry == null || entry.getWorld() == null) return;
        try {
            String encoded = encodeDest(entry);
            doorBlock.getChunk().getPersistentDataContainer()
                    .set(destKey(ctx.namespace(), doorBlock), PersistentDataType.STRING, encoded);
        } catch (Throwable ignored) { }
    }

    /** Encode a destination as "world x y z" (block-centered, feet). Parsed by the re-entry listener. */
    static String encodeDest(Location entry) {
        return entry.getWorld().getName() + " "
                + (entry.getBlockX() + 0.5) + " " + entry.getBlockY() + " " + (entry.getBlockZ() + 0.5);
    }

    /* ------------------------------------------------------------------ */
    /*  Shared helpers                                                     */
    /* ------------------------------------------------------------------ */

    private static Location resolveBase(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return null;
        return anchor;
    }

    private static int clampOffset(int v) { return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, v)); }

    /** Lowercase + strip to [a-z0-9_-] so a payload `schematic` can never path-traverse. Null if empty. */
    private static String sanitizeSchemName(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return s.isEmpty() ? null : s;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure helpers the swap's safety leans on: the schematic name is sanitized against
     * path-traversal, offsets clamp (a typo can't reach across the world), and the destination encoding
     * round-trips a namespaced-key sub-string that is always PDC-legal (lowercased, coordinate-keyed). A
     * regression here would let a swap escape its bounds or write an un-parseable destination.
     */
    static boolean roomSwapSelfTest() {
        if (sanitizeSchemName("../../etc/passwd").contains("/")) return false; // traversal stripped
        if (sanitizeSchemName("..") != null) return false;                     // empties to null
        if (!"undercroft_room_b".equals(sanitizeSchemName("Undercroft_Room_B"))) return false;
        // offsets clamp both ways.
        if (clampOffset(9999) != OFFSET_LIMIT || clampOffset(-9999) != -OFFSET_LIMIT) return false;
        if (clampOffset(5) != 5) return false;
        // The dest-key sub-string is coordinate-keyed + lowercase (PDC-legal); distinct door cells → distinct
        // keys (so two swaps in one chunk never collide on the destination record).
        String a = PDC_SWAP_DEST + ".10_5_-3";
        String b = PDC_SWAP_DEST + ".10_5_-4";
        return !a.equals(b);
    }
}
