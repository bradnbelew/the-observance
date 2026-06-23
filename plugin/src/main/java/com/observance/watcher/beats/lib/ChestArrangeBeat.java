package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * CLUE-DISCOVERY ("it's a clue!") — items in a chest are rearranged into a deliberate pattern or
 * number. The plugin lays authored items into authored slots (a glyph, a count, a sequence) so the
 * group walks up to their own storage and finds it ordered into meaning.
 *
 * <p>Anti-grief / decency: by default it writes ONLY into empty slots (never deletes the group's
 * loot). If {@code "clear_first":true} is authored (e.g. a dedicated puzzle chest at a site), it
 * clears first — use only on showrunner-owned containers, never a base chest.
 *
 * <p>Payload:
 * <pre>{@code
 * { "clear_first": false,
 *   "placements": [ {"slot":4,"material":"BONE","amount":3}, {"slot":13,"material":"BONE","amount":1} ] }
 * }</pre>
 */
public final class ChestArrangeBeat extends AbstractBeat {

    @Override public String name() { return "chest_arrange"; }
    @Override public String description() { return "Items rearrange in a chest into a deliberate pattern / number — a clue."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (req.payload().objectList("placements").isEmpty()) return false;
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        return a.getBlock().getState() instanceof Container;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null) return BeatResult.skipped("no-anchor");
        Block b = a.getBlock();
        if (!(b.getState() instanceof Container)) return BeatResult.skipped("no-container");

        BeatPayload p = req.payload();
        List<BeatPayload> placements = p.objectList("placements");
        if (placements.isEmpty()) return BeatResult.skipped("no-placements");
        final boolean clearFirst = p.bool("clear_first", false);

        mutateWhenUnwitnessed(ctx, b, () -> {
            if (!(a.getBlock().getState() instanceof Container c)) return;
            Inventory inv = c.getInventory();
            if (clearFirst) inv.clear();
            int size = inv.getSize();
            for (BeatPayload pl : placements) {
                int slot = pl.integer("slot", -1);
                if (slot < 0 || slot >= size) continue;
                Material mat = material(pl.string("material", null), null);
                if (mat == null || mat.isAir()) continue;
                int amount = Math.max(1, Math.min(mat.getMaxStackSize(), pl.integer("amount", 1)));
                // decency: don't clobber an existing item unless clear_first was set
                if (!clearFirst) {
                    ItemStack existing = inv.getItem(slot);
                    if (existing != null && !existing.getType().isAir()) continue;
                }
                ItemStack stack = new ItemStack(mat, amount);
                String nm = pl.string("name", null);
                if (nm != null) {
                    var meta = stack.getItemMeta();
                    if (meta != null) {
                        meta.displayName(net.kyori.adventure.text.Component.text(
                                nm.length() > 100 ? nm.substring(0, 100) : nm));
                        stack.setItemMeta(meta);
                    }
                }
                inv.setItem(slot, stack);
            }
        });
        return BeatResult.fired("chest-arranged");
    }
}
