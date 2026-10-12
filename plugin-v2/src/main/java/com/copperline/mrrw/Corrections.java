package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.DecoratedPot;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Act 6 (Correction): Morrow rewrites the record to match its version, and gets caught by 2011's
 * rules. Sign rewrites run over 15 characters a line; the transcription book cites a day the
 * server was offline; the rune signs decode to word-chain nonsense; the sheep census is bleached.
 * Waxed signs are never touched. Act 7: June's flags are "tidied" into alphabetical order.
 */
public final class Corrections {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;

    public Corrections(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        Bukkit.getScheduler().runTaskTimer(plugin, this::nightly, 200L, 20L * 60);
    }

    public void apply(World w) {
        int act = state.act();
        if (act >= 6) {
            once("act6.sign_rewrites", () -> signRewrites(w));
            once("act6.book", () -> book(w));
            once("act6.rune_signs", () -> runeSigns(w));
            once("act6.pots", () -> pots(w));
            once("act6.sheep", () -> sheep(w));
        }
        if (act >= 7) once("act7.flags_tidied", () -> tidyFlags(w));
    }

    /** Morrow keeps "correcting": anything a player put back on an unwaxed sign is rewritten again. */
    private void nightly() {
        if (state.done("stopped") || state.act() < 6) return;
        World w = Bukkit.getWorlds().get(0);
        long t = w.getTime();
        if (t >= 18000 && t < 19300) signRewrites(w);
    }

    private void once(String flag, Runnable r) {
        if (state.done(flag)) return;
        r.run();
        state.markDone(flag);
        JsonObject d = new JsonObject();
        d.addProperty("change", flag);
        journal.write("morrow.world_change", d);
    }

    private Block near(World w, String anchor, int dx, int dy, int dz) {
        int[] a = canon.anchor(anchor);
        return w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
    }

    static Sign findSign(World w, int[] a, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -r; dz <= r; dz++) {
            if (w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz).getState() instanceof Sign s) return s;
        }
        return null;
    }

    private void signRewrites(World w) {
        for (JsonElement e : canon.array("sign_rewrites")) {
            JsonObject rw = e.getAsJsonObject();
            Sign s = findSign(w, canon.anchor(rw.get("anchor").getAsString()), 4);
            if (s == null || s.isWaxed()) continue;
            List<String> lines = Canon.strings(rw.getAsJsonArray("lines"));
            for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            s.update(true, false);
        }
    }

    /** C18: Morrow's transcription; hover each line to see the source it claims. */
    private void book(World w) {
        JsonObject b = canon.array("transcription_books").get(0).getAsJsonObject();
        Block lec = near(w, "cairn_storehouse_door_2011", 6, 0, 1);
        lec.setType(Material.LECTERN, false);
        if (!(lec.getState() instanceof Lectern lectern)) return;
        ItemStack it = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) it.getItemMeta();
        meta.setTitle(b.get("title").getAsString());
        meta.setAuthor(b.get("author").getAsString());
        meta.setGeneration(BookMeta.Generation.COPY_OF_COPY);
        Component page = Component.empty();
        for (JsonElement l : b.getAsJsonArray("lines")) {
            JsonArray pair = l.getAsJsonArray();
            page = page.append(Component.text(pair.get(0).getAsString() + "\n\n")
                    .hoverEvent(HoverEvent.showText(Component.text("source: " + pair.get(1).getAsString(), NamedTextColor.GRAY))));
        }
        meta.addPages(page);
        it.setItemMeta(meta);
        lectern.getInventory().setItem(0, it);
    }

    /** D14: code-shaped nonsense in the enchanting-table alphabet. */
    private void runeSigns(World w) {
        JsonArray signs = canon.array("sga_signs");
        for (int i = 0; i < signs.size(); i++) {
            Block b = near(w, "spawn", -3 - i, 0, 4);
            b.setType(Material.OAK_SIGN, false);
            if (b.getState() instanceof Sign s) {
                List<String> lines = Canon.strings(signs.get(i).getAsJsonArray());
                for (int k = 0; k < 4; k++) s.getSide(Side.FRONT).line(k, Component.text(lines.get(k)).font(Key.key("minecraft", "alt")));
                s.update(true, false);
            }
        }
    }

    /** C15: decorated pots label what Morrow buried as "suspicious". */
    private void pots(World w) {
        for (JsonElement e : canon.array("pots")) {
            JsonObject p = e.getAsJsonObject();
            JsonArray off = p.getAsJsonArray("offset");
            Block pot = near(w, p.get("anchor").getAsString(), off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
            placePot(pot, Canon.strings(p.getAsJsonArray("sherds")), Material.valueOf(p.get("buried").getAsString()));
        }
    }

    static void placePot(Block pot, List<String> sherds, Material buried) {
        pot.setType(Material.DECORATED_POT, false);
        if (pot.getState() instanceof DecoratedPot dp) {
            DecoratedPot.Side[] sides = {DecoratedPot.Side.FRONT, DecoratedPot.Side.RIGHT, DecoratedPot.Side.BACK, DecoratedPot.Side.LEFT};
            for (int i = 0; i < 4; i++) dp.setSherd(sides[i], Material.valueOf(sherds.get(i) + "_POTTERY_SHERD"));
            dp.update(true, false);
        }
        Block sand = pot.getRelative(0, -1, 0);
        sand.setType(Material.SUSPICIOUS_SAND, false);
        if (buried != null && sand.getState() instanceof BrushableBlock bb) {
            bb.setItem(new ItemStack(buried));
            bb.update(true, false);
        }
    }

    /** D5: the census flock is "normalised" to white. June's screenshot is the backup. */
    private void sheep(World w) {
        int[] a = canon.anchor("sheep_pen");
        w.getNearbyEntitiesByType(Sheep.class, new org.bukkit.Location(w, a[0], a[1], a[2]), 16).forEach(s -> s.setColor(DyeColor.WHITE));
    }

    /** Act 7: Morrow sorts June's flags alphabetically. Theo's tribute photo keeps the real order. */
    private void tidyFlags(World w) {
        int[] t = canon.anchor("june_tower");
        List<Banner> banners = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            if (w.getBlockAt(t[0] - 5 + i, t[1] + 21, t[2] - 2).getState() instanceof Banner b) banners.add(b);
        }
        if (banners.size() < 2) return;
        record Snap(Material type, List<org.bukkit.block.banner.Pattern> patterns, String key) { }
        List<Snap> snaps = new ArrayList<>();
        for (Banner b : banners) snaps.add(new Snap(b.getType(), b.getPatterns(), b.getType().name() + b.getPatterns().size()));
        snaps.sort(Comparator.comparing(Snap::key));
        for (int i = 0; i < banners.size(); i++) {
            Block blk = banners.get(i).getBlock();
            blk.setType(snaps.get(i).type(), false);
            if (blk.getState() instanceof Banner nb) {
                nb.setPatterns(snaps.get(i).patterns());
                nb.update(true, false);
            }
        }
    }
}
