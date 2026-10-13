package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The Theo-era and Morrow-era layer that already exists when players first arrive. Installed once
 * on the upgraded world with /mrrw install (idempotent; each part is a one-shot flag). 2011 objects
 * come from the Beta world itself (morrow/world/inject.py); nothing here is 2011-palette except the
 * storehouse routine fixtures, which cairn would have had.
 */
public final class ModernLayer {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;

    public ModernLayer(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
    }

    private Block at(World w, String anchor, int dx, int dy, int dz) {
        int[] a = canon.anchor(anchor);
        return w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
    }

    public List<String> install(World w) {
        List<String> done = new ArrayList<>();
        part("install.museum", done, () -> museum(w));
        part("install.reading_room", done, () -> readingRoom(w));
        part("install.map_wall", done, () -> mapWall(w));
        part("install.map_copies", done, () -> mapCopies(w));
        part("install.flags", done, () -> flags(w));
        part("install.flag_tribute", done, () -> flagTribute(w));
        part("install.meadow", done, () -> meadow(w, null));
        part("install.lowpath_place", done, () -> lowpathPlace(w));
        part("install.dust_corridor", done, () -> dustCorridor(w));
        part("install.listening_room", done, () -> listeningRoom(w));
        part("install.maint_sign", done, () -> maintSign(w));
        part("install.finch_lock", done, () -> new FinchLock(canon).build(w));
        part("install.nether_hub_sign", done, this::netherHubSign);
        part("install.container_repair", done, () -> repairContainers(w));
        if (canon.raw.has("finch_south_sign")) part("install.south_rail_sign", done, () -> southRailSign(w));
        reattachMaps();
        return done;
    }

    /**
     * Name a container. Must happen BEFORE items go in: BlockState.update() writes the whole
     * snapshot back, so an update() after getInventory().setItem() wiped the items (the 2026-10
     * audit found the drafts barrel empty: no genuine 2015 map, so C11 could not be finished).
     */
    static void named(Block b, String name) {
        if (b.getState() instanceof org.bukkit.block.Container c) {
            c.customName(Component.text(name));
            c.update(true, false);
        }
    }

    /** Worlds installed before the fix: put back what the update() wiped (genuine 2015, hinge tin, books). */
    private void repairContainers(World w) {
        List<Integer> ids = plugin.getConfig().getIntegerList("map_ids");
        Block drafts = at(w, "theo_reading_room", 4, -2, 2);
        if (!ids.isEmpty() && drafts.getState() instanceof org.bukkit.block.Barrel barrel && barrel.getInventory().isEmpty()) {
            @SuppressWarnings("deprecation") MapView g = Bukkit.getMap(ids.get(0));
            if (g != null) barrel.getInventory().setItem(0, mapItem(g, canon.object("genuine_2015_map").get("label").getAsString()));
        }
        if (at(w, "listening_room", -2, 0, -2).getState() instanceof Chest c && c.getBlockInventory().isEmpty() && !state.done("act8.listening_room_open")) {
            c.getBlockInventory().setItem(0, new ItemStack(Material.IRON_NUGGET, 3));
        }
        JsonArray shelves = canon.array("braille_shelves");
        for (int i = 0; i < shelves.size(); i++) {
            if (!(at(w, "theo_reading_room", i, 0, 0).getState() instanceof ChiseledBookshelf shelf) || !shelf.getInventory().isEmpty()) continue;
            JsonArray slots = shelves.get(i).getAsJsonArray();
            for (int s = 0; s < 6; s++) if (slots.get(s).getAsBoolean()) shelf.getInventory().setItem(s, new ItemStack(Material.BOOK));
        }
    }

    private void part(String flag, List<String> done, Runnable r) {
        if (state.done(flag)) return;
        r.run();
        state.markDone(flag);
        done.add(flag);
        JsonObject d = new JsonObject();
        d.addProperty("part", flag);
        journal.write("install", d);
    }

    // ---- Theo's museum: waxed copper, the waxed sign, lodestone compasses ----------------------

    private void museum(World w) {
        Material[] stages = {Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER, Material.WAXED_WEATHERED_COPPER, Material.WAXED_OXIDIZED_COPPER};
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) at(w, "theo_museum", x, -1, z).setType(stages[(x + z * 2) % 4], false);
        Block s = at(w, "theo_museum", 2, 0, 0);
        s.setType(Material.OAK_SIGN, false);
        if (s.getState() instanceof Sign sign) {
            List<String> lines = Canon.strings(canon.array("museum_sign"));
            for (int i = 0; i < 4; i++) sign.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            sign.setWaxed(true);
            sign.update(true, false);
        }
        Block c = at(w, "theo_museum", 4, 0, 4);
        c.setType(Material.CHEST, false);
        if (!(c.getState() instanceof Chest chest)) return;
        String[][] doors = {{"rookery_house", "rookery"}, {"cairn_storehouse_door_2011", "cairn"}, {"finch_hub", "finchline"},
                {"june_house", "juni_lantern"}, {"patchcord_relay_hut", "patchcord"}, {"kit_genuine_door", "kitbag"}};
        int slot = 0;
        for (String[] d : doors) {
            int[] a = canon.anchor(d[0]);
            Location lode = new Location(w, a[0], a[1] - 3, a[2]);
            lode.getBlock().setType(Material.LODESTONE, false);
            chest.getBlockInventory().setItem(slot++, compass(d[1] + "'s door", lode, true));
        }
        int[] lp = canon.anchor("lowpath_place");
        chest.getBlockInventory().setItem(slot, compass("lowpath's door", new Location(w, lp[0], lp[1] - 40, lp[2]), true));
    }

    private static ItemStack compass(String name, Location lodestone, boolean tracked) {
        ItemStack it = new ItemStack(Material.COMPASS);
        CompassMeta m = (CompassMeta) it.getItemMeta();
        m.setLodestone(lodestone);
        m.setLodestoneTracked(tracked);  // no lodestone at the target = the needle spins
        m.customName(Component.text(name));
        it.setItemMeta(m);
        return it;
    }

    // ---- Theo's reading room: chiseled bookshelves as Braille (C10) -------------------------------

    private void readingRoom(World w) {
        JsonArray shelves = canon.array("braille_shelves");
        for (int i = 0; i < shelves.size(); i++) {
            Block b = at(w, "theo_reading_room", i, 0, 0);
            b.setType(Material.CHISELED_BOOKSHELF, false);
            if (b.getBlockData() instanceof org.bukkit.block.data.Directional dir) {
                dir.setFacing(BlockFace.SOUTH);
                b.setBlockData(dir, false);
            }
            JsonArray slots = shelves.get(i).getAsJsonArray();
            // the visible books are block-state flags; set them explicitly (the inventory alone doesn't)
            if (b.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf data) {
                for (int s = 0; s < 6; s++) data.setSlotOccupied(s, slots.get(s).getAsBoolean());
                b.setBlockData(data, false);
            }
            // then the real books, into the LIVE inventory (a state.update() after setItem wiped them)
            if (b.getState() instanceof ChiseledBookshelf shelf) {
                for (int s = 0; s < 6; s++) if (slots.get(s).getAsBoolean()) shelf.getInventory().setItem(s, new ItemStack(Material.BOOK));
            }
        }
        Block sign = at(w, "theo_reading_room", 4, 2, 1);
        sign.setType(Material.OAK_WALL_SIGN, false);
        if (sign.getState() instanceof Sign s) {
            s.getSide(Side.FRONT).line(0, Component.text("reading room"));
            s.getSide(Side.FRONT).line(1, Component.text(". :  :. ."));
            s.getSide(Side.FRONT).line(2, Component.text("2023"));
            s.setWaxed(true);
            s.update(true, false);
        }
    }

    // ---- Theo's map wall (C11): the forged "2015" is made last, so its map id is the highest ------

    private void mapWall(World w) {
        JsonObject genuine = canon.object("genuine_2015_map");
        List<Integer> ids = new ArrayList<>();
        MapView g = Bukkit.createMap(w);
        ids.add(g.getId());
        Block drafts = at(w, "theo_reading_room", 4, -2, 2);  // under the floor: Theo's drafts
        drafts.setType(Material.BARREL, false);
        named(drafts, "drafts");
        if (drafts.getState() instanceof org.bukkit.block.Barrel barrel) barrel.getInventory().setItem(0, mapItem(g, genuine.get("label").getAsString()));
        int col = 0;
        for (JsonElement e : canon.array("map_wall")) {
            JsonObject m = e.getAsJsonObject();
            MapView view = Bukkit.createMap(w);
            ids.add(view.getId());
            Location loc = at(w, "theo_map_wall", col % 6, 1 + col / 6, 0).getLocation();
            loc.getBlock().getRelative(BlockFace.NORTH).setType(Material.SPRUCE_PLANKS, false);
            ItemStack item = mapItem(view, m.get("label").getAsString());
            w.spawn(loc, ItemFrame.class, f -> {
                f.setFacingDirection(BlockFace.SOUTH, true);
                f.setItem(item);
                f.setFixed(true);
            });
            col++;
        }
        plugin.getConfig().set("map_ids", ids);
        plugin.saveConfig();
    }

    static ItemStack mapItem(MapView view, String label) {
        ItemStack it = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) it.getItemMeta();
        meta.setMapView(view);
        meta.customName(Component.text(label));
        it.setItemMeta(meta);
        return it;
    }

    // ---- C11 made checkable: Theo's copies of the wall maps ----------------------------------------
    // The framed maps are fixed (the wall can't be looted or knocked down), but the map number only
    // shows on an item. So a chest beside the wall holds a copy of each one (a copy of a map IS the
    // same map number). Players take them, read #N, compare with the genuine 2015 in the drafts
    // barrel. Morrow puts back any copy that leaves the chest at dawn.

    private Block copiesChest(World w) {
        return at(w, "theo_map_wall", 6, 0, 0);
    }

    private void mapCopies(World w) {
        Block c = copiesChest(w);
        if (c.getRelative(BlockFace.DOWN).getType().isAir()) c.getRelative(BlockFace.DOWN).setType(Material.SPRUCE_PLANKS, false);
        c.setType(Material.CHEST, false);
        named(c, "copies");
        state.markDone("install.map_copies");  // so the refill below runs inside the install
        refillMapCopies(w);
    }

    /** Returns how many copies it put back. */
    public int refillMapCopies(World w) {
        if (!state.done("install.map_copies")) return 0;
        List<Integer> ids = plugin.getConfig().getIntegerList("map_ids");
        JsonArray wall = canon.array("map_wall");
        if (ids.size() < 2 || !(copiesChest(w).getState() instanceof Chest chest)) return 0;
        var inv = chest.getBlockInventory();
        int put = 0;
        for (int i = 1; i < ids.size() && i - 1 < wall.size(); i++) {
            int id = ids.get(i);
            boolean present = false;
            for (ItemStack it : inv.getContents()) {
                if (it != null && it.getItemMeta() instanceof MapMeta mm && mm.hasMapView() && mm.getMapView() != null && mm.getMapView().getId() == id) {
                    present = true;
                    break;
                }
            }
            if (present) continue;
            @SuppressWarnings("deprecation") MapView view = Bukkit.getMap(id);
            if (view == null) continue;
            ItemStack copy = mapItem(view, wall.get(i - 1).getAsJsonObject().get("label").getAsString());
            if (inv.getItem(i - 1) == null) inv.setItem(i - 1, copy);
            else if (!inv.addItem(copy).isEmpty()) continue;
            put++;
        }
        return put;
    }

    /** Map renderers are not saved by the server; reattach the pictures after every restart. */
    public void reattachMaps() {
        List<Integer> ids = plugin.getConfig().getIntegerList("map_ids");
        if (ids.isEmpty()) return;
        JsonArray wall = canon.array("map_wall");
        for (int i = 0; i < ids.size(); i++) {
            MapView view = Bukkit.getMap(ids.get(i));
            if (view == null) continue;
            boolean lowpath = i == 0 ? false : wall.get(i - 1).getAsJsonObject().get("lowpath").getAsBoolean();
            view.getRenderers().forEach(view::removeRenderer);
            view.addRenderer(new MapArt.Renderer(MapArt.town(canon, lowpath, null)));
            view.setLocked(true);
            view.setTrackingPosition(false);
        }
    }

    // ---- June's gallery flags (C12/D7) -----------------------------------------------------------

    private void flags(World w) {
        JsonObject flags = canon.object("flags");
        String word = "K" + canon.string("flag_word");
        for (int i = 0; i < word.length(); i++) {
            String letter = String.valueOf(word.charAt(i));
            JsonArray spec = flags.getAsJsonArray(letter);
            Block b = at(w, "june_tower", -5 + i, 21, -2);
            b.getRelative(BlockFace.DOWN).setType(Material.STRIPPED_SPRUCE_LOG, false);
            DyeColor base = DyeColor.valueOf(spec.get(0).getAsString());
            b.setType(Material.valueOf(base.name() + "_BANNER"), false);
            if (b.getState() instanceof Banner banner) {
                List<Pattern> patterns = new ArrayList<>();
                for (JsonElement p : spec.get(1).getAsJsonArray()) {
                    JsonArray pa = p.getAsJsonArray();
                    patterns.add(new Pattern(DyeColor.valueOf(pa.get(1).getAsString()), patternType(pa.get(0).getAsString())));
                }
                banner.setPatterns(patterns);
                banner.update(true, false);
            }
        }
    }

    /**
     * C12 kept solvable: Theo's spare set of the 2017 tribute banners, in hanging order, in a barrel
     * in his museum ("the tribute photo" made physical). Morrow sorts the gallery in Act 8; this
     * barrel keeps the true order readable (banner items show their patterns in the inventory).
     */
    private void flagTribute(World w) {
        JsonObject flags = canon.object("flags");
        String word = "K" + canon.string("flag_word");
        Block b = at(w, "theo_museum", 0, 0, 4);
        b.setType(Material.BARREL, false);
        named(b, "tribute 2017 (spares)");
        if (!(b.getState() instanceof org.bukkit.block.Barrel barrel)) return;
        for (int i = 0; i < word.length() && i < 27; i++) {
            JsonArray spec = flags.getAsJsonArray(String.valueOf(word.charAt(i)));
            ItemStack banner = new ItemStack(Material.valueOf(DyeColor.valueOf(spec.get(0).getAsString()).name() + "_BANNER"));
            org.bukkit.inventory.meta.BannerMeta meta = (org.bukkit.inventory.meta.BannerMeta) banner.getItemMeta();
            for (JsonElement p : spec.get(1).getAsJsonArray()) {
                JsonArray pa = p.getAsJsonArray();
                meta.addPattern(new Pattern(DyeColor.valueOf(pa.get(1).getAsString()), patternType(pa.get(0).getAsString())));
            }
            meta.customName(Component.text("gallery " + (i + 1) + " of " + word.length()));
            banner.setItemMeta(meta);
            barrel.getInventory().setItem(i, banner);
        }
    }

    @SuppressWarnings("deprecation")
    private static PatternType patternType(String name) {
        return org.bukkit.Registry.BANNER_PATTERN.get(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
    }

    // ---- The meadow (D8): map art laid in the ground ---------------------------------------------

    public void meadow(World w, List<double[]> extra) {
        int[] c = canon.anchor("meadow_center");
        int x0 = Math.floorDiv(c[0] + 64, 128) * 128 - 64, z0 = Math.floorDiv(c[2] + 64, 128) * 128 - 64;  // aligned to the map grid
        MapArt.layMeadow(w, x0, c[1], z0, MapArt.town(canon, extra != null, extra));
    }

    // ---- lowpath's place: modern blocks, on a hill nature never made, and no bed -----------------

    private void lowpathPlace(World w) {
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            int h = 4 - Math.max(Math.abs(dx), Math.abs(dz)) / 2;
            for (int dy = -h; dy <= 0; dy++) at(w, "lowpath_place", dx, dy - 1, dz).setType(dy == 0 ? Material.GRASS_BLOCK : Material.DIRT, false);
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 3; dy++) {
            boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            Block b = at(w, "lowpath_place", dx, dy, dz);
            if (dy == 3) b.setType(Material.SPRUCE_SLAB, false);
            else if (dy == 0) b.setType(Material.SPRUCE_PLANKS, false);
            else b.setType(wall ? Material.SPRUCE_PLANKS : Material.AIR, false);
        }
        at(w, "lowpath_place", 0, 1, 2).setType(Material.AIR, false);
        at(w, "lowpath_place", 0, 2, 2).setType(Material.AIR, false);
        Block lower = at(w, "lowpath_place", 0, 1, 2);
        Door door = (Door) Material.SPRUCE_DOOR.createBlockData();
        door.setFacing(BlockFace.SOUTH);
        door.setHalf(Bisected.Half.BOTTOM);
        lower.setBlockData(door, false);
        Door top = (Door) door.clone();
        top.setHalf(Bisected.Half.TOP);
        lower.getRelative(BlockFace.UP).setBlockData(top, false);
        at(w, "lowpath_place", -1, 1, -1).setType(Material.BARREL, false);
        at(w, "lowpath_place", 1, 2, -1).setType(Material.LANTERN, false);
        at(w, "lowpath_place", 1, 1, -1).setType(Material.SPRUCE_PLANKS, false);
        Block sign = at(w, "lowpath_place", 1, 1, 3);
        sign.setType(Material.OAK_SIGN, false);
        if (sign.getState() instanceof Sign s) {
            s.getSide(Side.FRONT).line(0, Component.text("lowpath's place"));
            s.update(true, false);
        }
    }

    // ---- Morrow's maintenance trace (D13): redstone dust at exact power levels ---------------------

    private void dustCorridor(World w) {
        JsonArray levels = canon.array("light_corridor");
        for (int i = 0; i < levels.size(); i++) {
            for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 3; dy++) {
                Block b = at(w, "dust_corridor", i, dy, dz);
                b.setType(dy == 0 || dy == 3 || dz != 0 ? Material.DEEPSLATE_TILES : Material.AIR, false);
            }
            Block dust = at(w, "dust_corridor", i, 1, 0);
            dust.setBlockData(dust(levels.get(i).getAsInt()), false);  // no physics: the levels stay exactly as written
        }
    }

    /** A dot of dust at a fixed power. Fixtures keeps it there (refuses updates, re-asserts at dawn). */
    static BlockData dust(int power) {
        AnaloguePowerable data = (AnaloguePowerable) Material.REDSTONE_WIRE.createBlockData();
        data.setPower(power);
        return data;
    }

    // ---- cairn's sealed back room (C16) and his closing-routine fixtures -------------------------

    private void listeningRoom(World w) {
        for (int dx = 0; dx <= 4; dx++) for (int dz = 0; dz <= 4; dz++) for (int dy = 0; dy <= 3; dy++) {
            boolean shell = dx == 0 || dx == 4 || dz == 0 || dz == 4 || dy == 0 || dy == 3;
            Block b = at(w, "listening_room", dx, dy, dz);
            b.setType(shell ? ((dx + dy + dz) % 3 == 0 ? Material.CALIBRATED_SCULK_SENSOR : Material.SCULK) : Material.AIR, false);
        }
        Block door = at(w, "listening_room", 2, 1, 0);
        Door d = (Door) Material.IRON_DOOR.createBlockData();
        d.setFacing(BlockFace.NORTH);
        d.setHalf(Bisected.Half.BOTTOM);
        door.setBlockData(d, false);
        Door t = (Door) d.clone();
        t.setHalf(Bisected.Half.TOP);
        door.getRelative(BlockFace.UP).setBlockData(t, false);
        Block chest = at(w, "listening_room", 2, 1, 3);
        chest.setType(Material.CHEST, false);
        if (chest.getState() instanceof Chest c) c.getBlockInventory().setItem(13, new ItemStack(Material.RED_BED));
        Block cairnSign = at(w, "listening_room", 1, 2, 3);
        cairnSign.setType(Material.OAK_WALL_SIGN, false);
        if (cairnSign.getState() instanceof Sign s) {
            String[] lines = {"cairn's pw", "hingetin", "dont tell rook", ""};
            for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(lines[i]));
            s.update(true, false);
        }
        // the routine fixtures, outside the door
        at(w, "listening_room", -4, 0, -2).setType(Material.STONE_PRESSURE_PLATE, false);  // walk the lot
        Block hinge = at(w, "listening_room", -2, 0, -2);                                    // the hinge tin
        hinge.setType(Material.CHEST, false);
        named(hinge, "hinge tin");
        if (hinge.getState() instanceof Chest c) c.getBlockInventory().setItem(0, new ItemStack(Material.IRON_NUGGET, 3));
        Block returns = at(w, "listening_room", 0, 0, -2);                                   // the returns
        returns.setType(Material.CHEST, false);
        named(returns, "returns");
        Block lever = at(w, "listening_room", 1, 1, -2);                                     // the desk lamp
        lever.getRelative(BlockFace.DOWN).setType(Material.OAK_PLANKS, false);
        Switch sw = (Switch) Material.LEVER.createBlockData();
        sw.setAttachedFace(org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR);
        sw.setPowered(true);
        lever.setBlockData(sw, false);
    }

    /**
     * finch's genuine 2011 sign at the south rail end ("south branch / not done / leave it"). It is a
     * 2011 object, so it belongs in the Beta world (morrow/world/inject.py); this only fills in when
     * the world has no sign there, from canon "finch_south_sign" {anchor, offset, lines}.
     */
    private void southRailSign(World w) {
        JsonObject spec = canon.object("finch_south_sign");
        int[] a = canon.anchor(spec.get("anchor").getAsString());
        if (Corrections.findSign(w, a, 2) != null) return;  // the world already has it (inject.py)
        JsonArray off = spec.getAsJsonArray("offset");
        Block b = w.getBlockAt(a[0] + off.get(0).getAsInt(), a[1] + off.get(1).getAsInt(), a[2] + off.get(2).getAsInt());
        if (!b.getType().isAir()) return;
        List<String> lines = Canon.strings(spec.getAsJsonArray("lines"));
        b.setType(Material.OAK_SIGN, false);
        if (b.getState() instanceof Sign s) {
            for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(i < lines.size() ? lines.get(i) : ""));
            s.update(true, false);
        }
    }

    /** C5: Rookery's 2011 sign at his Nether hub. Multiply by 8 to find the ladder room. */
    private void netherHubSign() {
        World nether = Bukkit.getWorlds().stream().filter(x -> x.getEnvironment() == World.Environment.NETHER).findFirst().orElse(null);
        if (nether == null) return;
        int[] h = canon.anchor("nether_hub");
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            nether.getBlockAt(h[0] + dx, h[1] - 1, h[2] + dz).setType(Material.NETHERRACK, false);
            for (int dy = 0; dy <= 2; dy++) nether.getBlockAt(h[0] + dx, h[1] + dy, h[2] + dz).setType(Material.AIR, false);
        }
        Block b = nether.getBlockAt(h[0], h[1], h[2]);
        b.setType(Material.OAK_SIGN, false);
        if (b.getState() instanceof Sign s) {
            List<String> lines = Canon.strings(canon.array("nether_hub_sign"));
            for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            s.update(true, false);
        }
    }

    private void maintSign(World w) {
        Block b = at(w, "maint_sign", 0, 0, 0);
        b.setType(Material.OAK_SIGN, false);
        if (b.getState() instanceof Sign s) {
            s.getSide(Side.FRONT).line(0, Component.text("MAINT OFF"));
            for (int i = 1; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.empty());
            s.update(true, false);
        }
    }
}
