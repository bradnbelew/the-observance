package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

/**
 * TEXT — a lectern book fills itself. Finds a lectern at the anchor (or places one if the payload
 * authorizes it and placement is valid), then sets a written book whose pages come ENTIRELY from
 * the payload (lore-agnostic). Mutation happens only out of line of sight (reveal discipline) so the
 * group discovers a record that "wrote itself".
 *
 * <p>Payload shape:
 * <pre>{@code
 * {
 *   "title": "...", "author": "...",
 *   "pages": [ "page one text", "page two text", ... ],
 *   "place_if_missing": false        // optional: place a lectern if none at anchor (validated)
 * }
 * }</pre>
 */
public final class LecternFillBeat extends AbstractBeat {

    @Override public String name() { return "lectern_fill"; }
    @Override public String description() { return "A lectern's book fills itself with an authored record, out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return false;
        if (!anchor.getWorld().isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) return false;
        BeatPayload p = req.payload();
        if (p.stringList("pages").isEmpty()) return false;       // nothing authored = nothing to do
        Block b = anchor.getBlock();
        return b.getType() == Material.LECTERN || p.bool("place_if_missing", false);
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        List<String> pages = p.stringList("pages");
        if (pages.isEmpty()) return BeatResult.skipped("no-pages");

        Block block = anchor.getBlock();

        // Ensure a lectern exists (place one only if valid + authorized).
        if (block.getType() != Material.LECTERN) {
            if (!p.bool("place_if_missing", false)) return BeatResult.skipped("no-lectern");
            if (!com.observance.watcher.util.Placement.canStandOn(block)) {
                return BeatResult.skipped("no-support");
            }
        }

        final String title = p.string("title", "Record");
        final String author = p.string("author", "the record");

        mutateWhenUnwitnessed(ctx, block, () -> {
            Block target = anchor.getBlock();
            if (target.getType() != Material.LECTERN) {
                target.setType(Material.LECTERN, false);
                ctx.protectedRegistry().protect(target);
            }
            if (!(target.getState() instanceof Lectern lectern)) return;
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            if (book.getItemMeta() instanceof BookMeta meta) {
                meta.setTitle(clamp(title, 32));
                meta.setAuthor(clamp(author, 32));
                for (String page : pages) {
                    meta.addPage(clamp(page == null ? "" : page, 1024));
                }
                book.setItemMeta(meta);
            }
            lectern.getInventory().setItem(0, book);
            lectern.update(true, false);
        });
        return BeatResult.fired("lectern-filled");
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
