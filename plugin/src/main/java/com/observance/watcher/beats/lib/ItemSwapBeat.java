package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * ITEMS — an item swaps in the player's hand (the classic torch→redstone-torch). Deniable + uncanny:
 * "that makes no sense." Only swaps when the held item matches {@code from}; produces {@code to} at
 * the SAME amount, so nothing is gained or lost (decency floor; no exploit to dupe/destroy).
 *
 * <p>By default this is reversible-flavored (the showrunner can queue the reverse swap later). It
 * does NOT touch enchantments/identity beyond type, keeping it a pure "did the world blink?" beat.
 *
 * <p>Payload:
 * <pre>{@code { "slot":"hand", "from":"TORCH", "to":"REDSTONE_TORCH" } }</pre>
 */
public final class ItemSwapBeat extends AbstractBeat {

    @Override public String name() { return "item_swap"; }
    @Override public String description() { return "An item swaps type in the player's hand (e.g. torch → redstone torch)."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        BeatPayload p = req.payload();
        Material to = material(p.string("to", null), null);
        if (to == null || to.isAir()) return false;
        ItemStack item = slotItem(req.targetPlayer(), p.string("slot", "hand"));
        Material from = material(p.string("from", null), null);
        if (item == null || item.getType().isAir()) return false;
        // if 'from' is specified, current item must match it
        return from == null || item.getType() == from;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        String slot = p.string("slot", "hand");
        ItemStack item = slotItem(pl, slot);
        if (item == null || item.getType().isAir()) return BeatResult.skipped("empty-slot");

        Material from = material(p.string("from", null), null);
        if (from != null && item.getType() != from) return BeatResult.skipped("from-mismatch");

        Material to = material(p.string("to", null), null);
        if (to == null || to.isAir()) return BeatResult.skipped("no-to");

        int amount = Math.max(1, Math.min(to.getMaxStackSize(), item.getAmount()));
        ItemStack swapped = new ItemStack(to, amount);
        setSlotItem(pl, slot, swapped);
        return BeatResult.fired("item-swapped");
    }

    private static ItemStack slotItem(Player pl, String slot) {
        if (slot != null && slot.trim().equalsIgnoreCase("offhand")) {
            return pl.getInventory().getItemInOffHand();
        }
        if (slot != null) {
            try {
                int idx = Integer.parseInt(slot.trim());
                if (idx >= 0 && idx < pl.getInventory().getSize()) return pl.getInventory().getItem(idx);
            } catch (NumberFormatException ignored) { }
        }
        return pl.getInventory().getItemInMainHand();
    }

    private static void setSlotItem(Player pl, String slot, ItemStack item) {
        if (slot != null && slot.trim().equalsIgnoreCase("offhand")) {
            pl.getInventory().setItemInOffHand(item);
            return;
        }
        if (slot != null) {
            try {
                int idx = Integer.parseInt(slot.trim());
                if (idx >= 0 && idx < pl.getInventory().getSize()) {
                    pl.getInventory().setItem(idx, item);
                    return;
                }
            } catch (NumberFormatException ignored) { }
        }
        pl.getInventory().setItemInMainHand(item);
    }
}
