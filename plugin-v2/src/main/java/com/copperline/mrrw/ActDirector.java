package com.copperline.mrrw;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * One-shot world changes that mark Morrow's progression, applied when the act reaches them
 * and recorded as done so a restart never repeats them. Each change writes a journal receipt.
 *
 * Act 2: Morrow's notes appear on the BACKS of 2011 signs (C17; 2011 signs had no back).
 * Act 3: rules sign 2 line 1 becomes "3 ask before you" (16 chars, the C6 key break);
 *        the "Storehouse tally (recovered)" book (Copy of a Copy, with lowpath) on cairn's desk;
 *        two copper golems at the storehouse; June's lamp flashes Fl W 5s (C4).
 */
public final class ActDirector {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private BukkitTask flasher;

    public ActDirector(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
    }

    public void apply() {
        World world = Bukkit.getWorlds().get(0);
        int act = state.act();
        if (act >= 2) once("act2.sign_backs", () -> signBacks(world));
        for (int a = 2; a <= Math.min(act, 9); a++) {
            final int step = a;
            once("rail.act" + a, () -> extendSouthRail(world, step));
        }
        if (act >= 3) {
            once("act3.rules_edit", () -> rulesEdit(world));
            once("act3.tally_book", () -> tallyBook(world));
            once("act3.golems", () -> golems(world));
            startFlasher(world);
        }
    }

    private void once(String flag, Runnable change) {
        if (state.done(flag)) return;
        change.run();
        state.markDone(flag);
        JsonObject d = new JsonObject();
        d.addProperty("change", flag);
        d.addProperty("act", state.act());
        journal.write("morrow.world_change", d);
    }

    private Block at(World w, String anchor, int dx, int dy, int dz) {
        int[] a = canon.anchor(anchor);
        return w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
    }

    private void signBacks(World w) {
        canon.signBackNotes.forEach((anchor, lines) -> {
            int[] a = canon.anchor(anchor);
            for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                if (w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz).getState() instanceof Sign sign && !sign.isWaxed()) {
                    for (int i = 0; i < 4; i++) sign.getSide(Side.BACK).line(i, Component.text(lines.get(i)));
                    sign.update(true, false);
                    return;
                }
            }
        });
    }

    private void rulesEdit(World w) {
        Block b = at(w, "rules_signs", 1, 0, 0);
        if (b.getState() instanceof Sign sign && !sign.isWaxed()) {
            List<String> lines = canon.rulesSignsMorrow.get(1);
            for (int i = 0; i < 4; i++) sign.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            sign.update(true, false);
        }
    }

    private void tallyBook(World w) {
        Block b = at(w, "cairn_storehouse_door_2011", 4, 0, 1);
        b.setType(Material.LECTERN, false);
        if (!(b.getState() instanceof Lectern lectern)) return;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Storehouse tally (recovered)");
        meta.setAuthor("cairn");
        meta.setGeneration(BookMeta.Generation.COPY_OF_COPY);
        meta.addPages(Component.text("weeks: 04-25 05-02 05-09 05-16\n\n" + String.join("\n", canon.tallyBookMorrow)));
        book.setItemMeta(meta);
        lectern.getInventory().setItem(0, book);
    }

    private void golems(World w) {
        try {
            EntityType golem = EntityType.valueOf("COPPER_GOLEM");
            for (int i = 0; i < 2; i++) w.spawnEntity(at(w, "cairn_storehouse_door_2011", 3 + i, 1, 3).getLocation().add(.5, 0, .5), golem);
        } catch (IllegalArgumentException missing) {
            plugin.getLogger().warning("COPPER_GOLEM not available on this server version; skipping golems");
        }
    }

    /** June's lamp: one short white flash every 5 seconds (the Sambro characteristic, Fl W 5s). */
    private void startFlasher(World w) {
        if (flasher != null) return;
        Block lamp = at(w, "june_tower", 0, 22, 0);
        once("act3.tower_lamp", () -> lamp.setType(Material.REDSTONE_LAMP, false));
        final long[] tick = {0};
        flasher = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.done("stopped")) return;
            if (!w.isChunkLoaded(lamp.getX() >> 4, lamp.getZ() >> 4)) return;  // never load chunks just to blink
            if (!(lamp.getBlockData() instanceof Lightable data)) return;
            boolean on = tick[0]++ % 100 < 20;  // 1 s on, 4 s off, every 100 ticks = 5 s
            if (data.isLit() != on) {
                data.setLit(on);
                lamp.setBlockData(data, false);
            }
        }, 20L, 1L);
    }

    /** The south branch: Morrow "finishes" three more blocks every act. In Act 10 it skips it. */
    private void extendSouthRail(World w, int act) {
        int[] a = canon.anchor("south_rail_end");
        com.google.gson.JsonArray dir = canon.array("south_rail_dir");
        int dx = dir.get(0).getAsInt(), dz = dir.get(1).getAsInt();
        for (int i = 0; i < 3; i++) {
            int n = (act - 2) * 3 + i + 1;
            Block bed = w.getBlockAt(a[0] + dx * n, a[1] - 1, a[2] + dz * n);
            if (bed.getType().isAir()) bed.setType(Material.GRAVEL, false);
            Block rail = bed.getRelative(0, 1, 0);
            rail.setType(Material.POWERED_RAIL, false);
        }
    }

    public void stop() {
        if (flasher != null) flasher.cancel();
    }
}
