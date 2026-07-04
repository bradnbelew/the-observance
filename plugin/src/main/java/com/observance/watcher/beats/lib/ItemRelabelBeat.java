package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.observance.watcher.util.TextFit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ITEMS / SENSORY — an item the target is carrying is quietly renamed and/or re-lored. The presence
 * "notices" something they hold. Targets a slot: {@code "slot":"hand"|"offhand"|<int>}. Optionally
 * gated to a specific material so it only relabels e.g. their map or their pickaxe.
 *
 * <p>Non-destructive: only display name + lore change; the item's identity/type/count are untouched.
 *
 * <p>Payload:
 * <pre>{@code
 * { "slot":"hand", "name":"the cold tooth", "lore":[ "...","..." ], "match_material":"IRON_PICKAXE" }
 * }</pre>
 */
public final class ItemRelabelBeat extends AbstractBeat {

    @Override public String name() { return "item_relabel"; }
    @Override public String description() { return "An item the player carries is quietly renamed / re-lored."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        BeatPayload p = req.payload();
        boolean hasName = p.has("name");
        boolean hasLore = !p.stringList("lore").isEmpty();
        return hasName || hasLore;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();

        ItemStack item = resolveSlotItem(pl, p.string("slot", "hand"));
        if (item == null || item.getType().isAir()) return BeatResult.skipped("empty-slot");

        String matchMat = p.string("match_material", null);
        if (matchMat != null && !item.getType().name().equalsIgnoreCase(matchMat.trim())) {
            return BeatResult.skipped("material-mismatch");
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return BeatResult.skipped("no-meta");

        if (p.has("name")) {
            String name = p.string("name", "");
            meta.displayName(Component.text(clamp(name, TextFit.TOOLTIP_LINE_CHARS)));
        }
        List<String> lore = p.stringList("lore");
        if (!lore.isEmpty()) {
            List<Component> comps = new ArrayList<>(lore.size());
            for (String line : lore) comps.add(Component.text(clamp(line == null ? "" : line, TextFit.TOOLTIP_LINE_CHARS)));
            meta.lore(comps);
        }
        item.setItemMeta(meta);
        return BeatResult.fired("item-relabeled");
    }

    private static ItemStack resolveSlotItem(Player pl, String slot) {
        if (slot == null) slot = "hand";
        switch (slot.trim().toLowerCase(Locale.ROOT)) {
            case "offhand": return pl.getInventory().getItemInOffHand();
            case "hand": return pl.getInventory().getItemInMainHand();
            default:
                try {
                    int idx = Integer.parseInt(slot.trim());
                    if (idx >= 0 && idx < pl.getInventory().getSize()) {
                        return pl.getInventory().getItem(idx);
                    }
                } catch (NumberFormatException ignored) { }
                return pl.getInventory().getItemInMainHand();
        }
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
