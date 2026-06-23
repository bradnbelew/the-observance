package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * CLUE-DISCOVERY — a filled map renders a meaningful mark: an X, a cursor at a coordinate, a glyph.
 * Given to the target's inventory (or placed in a chest at the anchor). The renderer paints a small
 * deterministic mark + optional cursor pointing at an authored coordinate, so the map "means
 * something" — it points at a place to go (call to action).
 *
 * <p>Payload:
 * <pre>{@code
 * { "dest":"inventory", "mark_x":64, "mark_z":64, "mark_color":18,
 *   "cursor_world":"world", "cursor_x":120, "cursor_z":-340, "cursor_type":"RED_X" }
 * }</pre>
 * mark_x / mark_z are pixel coords on the 128x128 canvas. A coarse blob is drawn around them.
 */
public final class MapMarkBeat extends AbstractBeat {

    @Override public String name() { return "map_mark"; }
    @Override public String description() { return "A filled map renders a meaningful mark / cursor — a clue pointing somewhere."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        String dest = req.payload().string("dest", "inventory");
        if ("inventory".equalsIgnoreCase(dest)) return req.hasTarget();
        Location a = anchor(ctx, req);
        return a != null && a.getWorld() != null
                && a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4);
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        Location anchor = anchor(ctx, req);
        org.bukkit.World world = anchor != null ? anchor.getWorld()
                : (req.hasTarget() ? req.targetPlayer().getWorld() : null);
        if (world == null) return BeatResult.skipped("no-world");

        MapView view;
        try {
            view = org.bukkit.Bukkit.createMap(world);
        } catch (Throwable t) {
            return BeatResult.failed("create-map");
        }
        if (view == null) return BeatResult.failed("null-map");

        // Strip default renderers so only our mark shows; keep it deterministic + cheap.
        try {
            for (MapRenderer r : view.getRenderers()) view.removeRenderer(r);
        } catch (Throwable ignored) { }

        final int markX = clampByte(p.integer("mark_x", 64));
        final int markZ = clampByte(p.integer("mark_z", 64));
        final byte color = (byte) p.integer("mark_color", 18); // 18 ≈ dark red in legacy map palette
        final MapCursor.Type cursorType = cursorType(p.string("cursor_type", null));
        final boolean hasCursor = p.has("cursor_x") && p.has("cursor_z");
        final int cursorPx = clampByte(p.integer("cursor_px", markX));
        final int cursorPz = clampByte(p.integer("cursor_pz", markZ));

        view.addRenderer(new MapRenderer(false) {
            private boolean drawn = false;
            @Override
            public void render(MapView mv, MapCanvas canvas, Player player) {
                if (drawn) return;     // render once — deterministic, no per-tick churn
                drawn = true;
                // a coarse blob around the mark
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                        int x = markX + dx, z = markZ + dz;
                        if (x >= 0 && x < 128 && z >= 0 && z < 128) {
                            try { canvas.setPixel(x, z, color); } catch (Throwable ignored) { }
                        }
                    }
                }
                if (hasCursor && cursorType != null) {
                    try {
                        MapCursor cur = new MapCursor((byte) (cursorPx - 64), (byte) (cursorPz - 64),
                                (byte) 8, cursorType, true);
                        canvas.getCursors().addCursor(cur);
                    } catch (Throwable ignored) { }
                }
            }
        });

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        if (mapItem.getItemMeta() instanceof MapMeta meta) {
            meta.setMapView(view);
            mapItem.setItemMeta(meta);
        }

        // Deliver.
        String dest = p.string("dest", "inventory");
        if ("inventory".equalsIgnoreCase(dest)) {
            Player pl = target(req);
            if (pl == null) return BeatResult.skipped("no-target");
            int slot = pl.getInventory().firstEmpty();
            if (slot < 0) return BeatResult.skipped("inventory-full");
            pl.getInventory().setItem(slot, mapItem);
            return BeatResult.fired("map-in-inventory");
        } else {
            if (anchor == null) return BeatResult.skipped("no-anchor");
            org.bukkit.block.Block b = anchor.getBlock();
            if (!(b.getState() instanceof org.bukkit.block.Container)) return BeatResult.skipped("no-container");
            mutateWhenUnwitnessed(ctx, b, () -> {
                if (anchor.getBlock().getState() instanceof org.bukkit.block.Container c) {
                    int slot = c.getInventory().firstEmpty();
                    if (slot >= 0) c.getInventory().setItem(slot, mapItem);
                }
            });
            return BeatResult.fired("map-in-chest");
        }
    }

    private static MapCursor.Type cursorType(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return MapCursor.Type.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Throwable t) {
            return MapCursor.Type.RED_X;
        }
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(127, v));
    }
}
