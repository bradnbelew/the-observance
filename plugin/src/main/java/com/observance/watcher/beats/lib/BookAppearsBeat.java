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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.observance.watcher.util.TextFit;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;

/**
 * TEXT / CLUE-DISCOVERY — a written book quietly appears. Destination is authored:
 * {@code "dest": "chest" | "ender_chest" | "inventory"}. For a chest it goes into the container at
 * the anchor (out of sight); for ender_chest/inventory it goes to the target player's storage.
 *
 * <p>Never overwrites a held/occupied slot destructively: it finds the first empty slot; if none,
 * it skips (no item loss — decency floor). Pages are authored in the payload.
 *
 * <p>Payload:
 * <pre>{@code
 * { "dest":"chest", "title":"...", "author":"...", "pages":[ ... ] }
 * }</pre>
 */
public final class BookAppearsBeat extends AbstractBeat {

    @Override public String name() { return "book_appears"; }
    @Override public String description() { return "A written book quietly appears in a chest, ender chest, or inventory."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (req.payload().stringList("pages").isEmpty()) return false;
        String dest = dest(req);
        if (dest.equals("inventory") || dest.equals("ender_chest")) {
            return req.hasTarget();
        }
        // chest: need a loaded container at the anchor
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        return a.getBlock().getState() instanceof Container;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        List<String> pages = p.stringList("pages");
        if (pages.isEmpty()) return BeatResult.skipped("no-pages");
        ItemStack book = buildBook(p, pages);
        String dest = dest(req);

        switch (dest) {
            case "inventory": {
                Player pl = target(req);
                if (pl == null) return BeatResult.skipped("no-target");
                int slot = pl.getInventory().firstEmpty();
                if (slot < 0) return BeatResult.skipped("inventory-full");
                pl.getInventory().setItem(slot, book);
                return BeatResult.fired("book-in-inventory");
            }
            case "ender_chest": {
                Player pl = target(req);
                if (pl == null) return BeatResult.skipped("no-target");
                Inventory ec = pl.getEnderChest();
                int slot = ec.firstEmpty();
                if (slot < 0) return BeatResult.skipped("ender-full");
                ec.setItem(slot, book);
                return BeatResult.fired("book-in-ender");
            }
            case "chest":
            default: {
                Location a = anchor(ctx, req);
                if (a == null) return BeatResult.skipped("no-anchor");
                Block b = a.getBlock();
                if (!(b.getState() instanceof Container)) return BeatResult.skipped("no-container");
                // Reveal discipline: only place when no one is staring at the chest.
                final ItemStack toPlace = book;
                mutateWhenUnwitnessed(ctx, b, () -> {
                    if (a.getBlock().getState() instanceof Container c) {
                        int slot = c.getInventory().firstEmpty();
                        if (slot >= 0) {
                            c.getInventory().setItem(slot, toPlace);
                        }
                    }
                });
                return BeatResult.fired("book-in-chest");
            }
        }
    }

    private static ItemStack buildBook(BeatPayload p, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (book.getItemMeta() instanceof BookMeta meta) {
            meta.setTitle(clamp(p.string("title", "Record"), 32));
            meta.setAuthor(clamp(p.string("author", "the record"), 32));
            // Each AUTHORED "page" string may be longer than a real book page can display (a vanilla
            // page does not scroll or auto-paginate — overflow is simply invisible). Re-wrap every
            // authored page into 1+ real, client-legible pages before writing them.
            for (String page : pages) {
                for (String real : TextFit.paginate(page == null ? "" : page)) meta.addPages(Component.text(real));
            }
            book.setItemMeta(meta);
        }
        return book;
    }

    private static String dest(BeatRequest req) {
        return req.payload().string("dest", "chest").trim().toLowerCase(Locale.ROOT);
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
