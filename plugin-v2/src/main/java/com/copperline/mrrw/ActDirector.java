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
            once("act3.creeper", () -> creeper(world));
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

    /** Rook's "dont touch it" sign gets its note when the floor is tidied (RecoveryService, Act 5), not now. */
    private static final java.util.Set<String> LATER_NOTES = java.util.Set.of("rookery_floor_sign");

    private void signBacks(World w) {
        JsonObject offsets = canon.raw.has("sign_back_offsets") ? canon.object("sign_back_offsets") : new JsonObject();
        canon.signBackNotes.forEach((anchor, lines) -> {
            if (LATER_NOTES.contains(anchor)) return;
            int[] a = canon.anchor(anchor);
            if (offsets.has(anchor)) {  // the exact sign the canon names (the lived-in world has many signs near each anchor)
                com.google.gson.JsonArray o = offsets.getAsJsonArray(anchor);
                if (!noteOnBack(w.getBlockAt(a[0] + o.get(0).getAsInt(), a[1] + o.get(1).getAsInt(), a[2] + o.get(2).getAsInt()), lines)) {
                    plugin.getLogger().warning("sign back note for " + anchor + " could not be made readable");
                }
                return;
            }
            for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                if (noteOnBack(w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz), lines)) {
                    return;
                }
            }
        });
    }

    /**
     * Morrow's note goes on a sign's back. A wall sign's back is inside the wall it hangs on, so Morrow
     * re-hangs it as a post one block out, front text kept, and writes on the back of that. Returns
     * false when the sign is waxed, not a sign, or the block in front is not free.
     */
    static boolean noteOnBack(Block b, List<String> note) {
        if (!(b.getState() instanceof Sign s) || s.isWaxed()) return false;
        Sign target = s;
        if (b.getBlockData() instanceof org.bukkit.block.data.type.WallSign ws) {
            // one block out from the wall, or beside that spot along the wall when something stands there
            Block out = null;
            org.bukkit.block.BlockFace f = ws.getFacing();
            org.bukkit.block.BlockFace side = f == org.bukkit.block.BlockFace.NORTH || f == org.bukkit.block.BlockFace.SOUTH
                    ? org.bukkit.block.BlockFace.EAST : org.bukkit.block.BlockFace.NORTH;
            for (Block c : List.of(b.getRelative(f), b.getRelative(f).getRelative(side), b.getRelative(f).getRelative(side.getOppositeFace()))) {
                if (c.getType().isAir()) { out = c; break; }
            }
            if (out == null) return false;
            Material post = Material.matchMaterial(b.getType().name().replace("_WALL_SIGN", "_SIGN"));
            if (post == null) return false;
            List<Component> front = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) front.add(s.getSide(Side.FRONT).line(i));
            out.setType(post, false);
            org.bukkit.block.data.Rotatable rot = (org.bukkit.block.data.Rotatable) out.getBlockData();
            rot.setRotation(ws.getFacing());
            out.setBlockData(rot, false);
            if (!(out.getState() instanceof Sign moved)) return false;
            for (int i = 0; i < 4; i++) moved.getSide(Side.FRONT).line(i, front.get(i));
            b.setType(Material.AIR, false);
            target = moved;
        }
        for (int i = 0; i < 4; i++) target.getSide(Side.BACK).line(i, Component.text(note.get(i)));
        target.update(true, false);
        return true;
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

    /** Rook's half-finished creeper by spawn: Morrow lays the bottom rows from the pattern it inferred. */
    private void creeper(World w) {
        JsonObject art = canon.object("creeper");
        com.google.gson.JsonArray rows = art.getAsJsonArray("rows");
        int missing = art.get("missing_rows").getAsInt(), n = 0;
        for (int r = rows.size() - missing; r < rows.size(); r++) {   // the top rows: it gives the thing its eyes
            com.google.gson.JsonArray row = rows.get(r).getAsJsonArray();
            for (int c = 0; c < row.size(); c++) {
                Block b = at(w, "spawn_creeper", c, r, 0);
                Material want = RecoveryService.WOOL[row.get(c).getAsInt()];
                if (b.getType() != want) {
                    b.setType(want, false);
                    n++;
                }
            }
        }
        String line = canon.object("engine_lines").get("creeper_done").getAsString();
        Bukkit.broadcast(Component.text("[Recovery] ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY).append(Component.text(line, net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        journal.recovery(line);
        JsonObject d = new JsonObject();
        d.addProperty("blocks", n);
        journal.write("morrow.creeper", d);
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
        if (flasher != null || state.done("act10.lamp_dark")) return;
        Block lamp = at(w, "june_tower", 0, 22, 0);
        once("act3.tower_lamp", () -> lamp.setType(Material.REDSTONE_LAMP, false));
        final long[] tick = {0};
        flasher = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.done("stopped")) return;
            if (!w.isChunkLoaded(lamp.getX() >> 4, lamp.getZ() >> 4)) return;  // never load chunks just to blink
            if (lamp.getType() != Material.REDSTONE_LAMP) lamp.setType(Material.REDSTONE_LAMP, false);  // broken or built over: C4 must keep flashing
            if (!(lamp.getBlockData() instanceof Lightable data)) return;
            boolean on = tick[0]++ % 100 < 20;  // 1 s on, 4 s off, every 100 ticks = 5 s
            if (data.isLit() != on) {
                data.setLit(on);
                lamp.setBlockData(data, false);
            }
        }, 20L, 1L);
    }

    /**
     * The south branch: Morrow "finishes" three more blocks every act (+z, toward lowpath's place),
     * past finch's genuine 2011 sign ("south branch / not done / leave it"). It only lays track on
     * open ground: it never overwrites a sign, a player's block or anything else in the way. In Act
     * 10 it skips it, and the stop log says so ("skip mossfield:south_rail (unfinished) reason=none").
     */
    private void extendSouthRail(World w, int act) {
        int[] a = canon.anchor("south_rail_end");
        com.google.gson.JsonArray dir = canon.array("south_rail_dir");
        int dx = dir.get(0).getAsInt(), dz = dir.get(1).getAsInt();
        int stopZ = canon.raw.has("south_rail_stop_z") ? canon.raw.get("south_rail_stop_z").getAsInt() : Integer.MAX_VALUE;
        int laid = 0;
        for (int i = 0; i < 3; i++) {
            int n = (act - 2) * 3 + i + 1;
            int x = a[0] + dx * n, z = a[2] + dz * n;
            if (dz > 0 && z > stopZ) continue;  // one segment short of lowpath's hill: never through its house
            // the track follows the ground (lowpath's hill rises across its line), one block per block at most
            int y = railY(w, x, z), prev = railY(w, x - dx, z - dz);
            Block rail = w.getBlockAt(x, y, z);
            if (!railCanGo(rail) || Math.abs(y - prev) > 1) continue;  // never over a sign, a container or a build; never a step
            Block bed = rail.getRelative(0, -1, 0);
            if (bed.getType().isAir()) bed.setType(Material.GRAVEL, false);
            org.bukkit.block.data.Rail data = (org.bukkit.block.data.Rail) Material.POWERED_RAIL.createBlockData();
            data.setShape(straight(dx));
            if (y < prev) data.setShape(ascending(-dx, -dz));      // climbs back up toward the last rail
            rail.setBlockData(data, false);
            Block last = w.getBlockAt(x - dx, prev, z - dz);
            if (y > prev && last.getBlockData() instanceof org.bukkit.block.data.Rail lr) {  // the last rail climbs to this one
                lr.setShape(ascending(dx, dz));
                last.setBlockData(lr, false);
            }
            laid++;
        }
        JsonObject d = new JsonObject();
        d.addProperty("south_rail_laid", laid);
        d.addProperty("act", act);
        journal.write("morrow.south_rail", d);
    }

    /** Where a rail at x, z sits: on the rail already there, else on the ground (trees and plants don't count). */
    private static int railY(World w, int x, int z) {
        int g = Terrain.ground(w, x, z);
        return org.bukkit.Tag.RAILS.isTagged(w.getBlockAt(x, g, z).getType()) ? g : g + 1;
    }

    private static org.bukkit.block.data.Rail.Shape straight(int dx) {
        return dx != 0 ? org.bukkit.block.data.Rail.Shape.EAST_WEST : org.bukkit.block.data.Rail.Shape.NORTH_SOUTH;
    }

    private static org.bukkit.block.data.Rail.Shape ascending(int dx, int dz) {
        if (dx > 0) return org.bukkit.block.data.Rail.Shape.ASCENDING_EAST;
        if (dx < 0) return org.bukkit.block.data.Rail.Shape.ASCENDING_WEST;
        return dz > 0 ? org.bukkit.block.data.Rail.Shape.ASCENDING_SOUTH : org.bukkit.block.data.Rail.Shape.ASCENDING_NORTH;
    }

    /** Open ground, plants, loose terrain or old track: yes. Signs (finch's), anything with a block entity, builds: no. */
    private static boolean railCanGo(Block b) {
        Material m = b.getType();
        if (m.isAir() || org.bukkit.Tag.RAILS.isTagged(m)) return true;
        if (org.bukkit.Tag.ALL_SIGNS.isTagged(m) || b.getState() instanceof org.bukkit.block.TileState) return false;
        return b.isReplaceable() || org.bukkit.Tag.DIRT.isTagged(m) || org.bukkit.Tag.SAND.isTagged(m)
                || org.bukkit.Tag.BASE_STONE_OVERWORLD.isTagged(m) || m == Material.GRAVEL || m == Material.SNOW_BLOCK;
    }

    /**
     * The stop begins: the flasher stops for good (flag act10.lamp_dark, so nothing restarts it after
     * a restart) and June's lamp goes dark, waiting for its one last flash.
     */
    public void quietLamp() {
        if (flasher != null) {
            flasher.cancel();
            flasher = null;
        }
        state.markDone("act10.lamp_dark");
        Block lamp = at(Bukkit.getWorlds().get(0), "june_tower", 0, 22, 0);
        setLit(lamp, false);
    }

    /** The stop's lights-out: June's lamp flashes once more (1 s), then goes dark for good. */
    public void lastFlash() {
        quietLamp();
        Block lamp = at(Bukkit.getWorlds().get(0), "june_tower", 0, 22, 0);
        if (!(lamp.getBlockData() instanceof Lightable)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> setLit(lamp, true), 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setLit(lamp, false);
            JsonObject d = new JsonObject();
            d.addProperty("lamp", "dark");
            journal.write("morrow.lamp_dark", d);
        }, 30L);
    }

    private static void setLit(Block lamp, boolean lit) {
        if (lamp.getBlockData() instanceof Lightable data && data.isLit() != lit) {
            data.setLit(lit);
            lamp.setBlockData(data, false);
        }
    }

    public void stop() {
        if (flasher != null) flasher.cancel();
    }
}
