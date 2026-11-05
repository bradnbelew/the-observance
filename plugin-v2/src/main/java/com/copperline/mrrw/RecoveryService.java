package com.copperline.mrrw;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.google.gson.JsonElement;

/**
 * What the restore job does to the world while players are around.
 *
 * Act 2 (restoring): blocks players break inside a resident's build come back at dawn,
 *   with "[Recovery] N block(s) restored (cairn storehouse)". It feels protective.
 * Acts 2-8 (the personal tidy): every chest or barrel a player placed is sorted at dawn, stacks
 *   merged and ordered by item type. Nothing is removed; it is just no longer theirs.
 * Act 3+ (tidying): blocks players placed near the old builds are "normalised" away at dawn,
 *   and one of cairn's tally chests per dawn is merged into tidy stacks, erasing the weeks.
 * Waxed signs are never touched (Morrow treats waxed as archived). After the stop: nothing.
 *
 * Pending restorations, placements and player chests persist in plugins/mrrw/recovery.yml, so a
 * restart before dawn loses nothing.
 */
public final class RecoveryService implements Listener {
    private static final int RADIUS = 14;
    private static final String[][] BUILDS = {
            {"rookery_house", "rook's"}, {"cairn_storehouse_door_2011", "cairn storehouse"}, {"cairn_storehouse_jog", "cairn storehouse"},
            {"cairn_farm", "the farms"}, {"sheep_pen", "the farms"},
            {"june_house", "june's point"}, {"june_tower", "the tower"}, {"patchcord_relay_hut", "patch's hut"},
            {"finch_hub", "rail hub"}, {"finch_spur", "the spur"}, {"spawn", "spawn"}, {"spawn_creeper", "spawn"},
            {"lowpath_place", "lowpath's place"},
            {"theo_museum", "the archive"}, {"theo_reading_room", "the archive"}, {"theo_map_wall", "the archive"},
            {"rookery_ladder_room", "the ladder room"}, {"maintenance_annex", "the annex"},
    };

    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final File file;
    private final Map<Location, BlockData> broken = new LinkedHashMap<>();
    private final Map<Location, String> placed = new LinkedHashMap<>();
    private final Map<Location, String> playerChests = new LinkedHashMap<>();
    private final List<Consumer<World>> dawnHooks = new ArrayList<>();
    private boolean dirty;
    private long lastDawnDay = -1;

    public RecoveryService(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.file = new File(plugin.getDataFolder(), "recovery.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    /** Other dawn jobs (fixture re-assertion) run inside the same dawn, including /mrrw dawn. */
    public void onDawn(Consumer<World> hook) {
        dawnHooks.add(hook);
    }

    private String buildAt(Location loc) {
        for (String[] b : BUILDS) {
            int[] a = canon.anchor(b[0]);
            if (Math.abs(loc.getBlockX() - a[0]) <= RADIUS && Math.abs(loc.getBlockZ() - a[2]) <= RADIUS
                    && Math.abs(loc.getBlockY() - a[1]) <= RADIUS) return b[1];
        }
        return null;
    }

    private boolean active() {
        return !state.done("stopped");
    }

    private static boolean isTidyContainer(Material m) {
        return m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!active() || state.act() < 2) return;
        Block b = event.getBlock();
        if (playerChests.remove(b.getLocation()) != null) dirty = true;
        if (placed.remove(b.getLocation()) != null) {  // breaking your own block is fine
            dirty = true;
            return;
        }
        if (buildAt(b.getLocation()) != null && broken.putIfAbsent(b.getLocation(), b.getBlockData().clone()) == null) dirty = true;
    }

    /** A sign, banner, chest or lectern in a build keeps its contents: the restore can only bring back a blank one. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreakTile(BlockBreakEvent event) {
        if (!active() || state.act() < 2 || event.getPlayer().isOp()) return;
        Block b = event.getBlock();
        if (placed.containsKey(b.getLocation()) || buildAt(b.getLocation()) == null) return;
        if (b.getState() instanceof org.bukkit.block.TileState) event.setCancelled(true);
    }

    /** Explosions in a build are restored like breaks; fire is not allowed to start on a build. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        recordBlast(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        recordBlast(event.blockList());
    }

    private void recordBlast(List<Block> blocks) {
        if (!active() || state.act() < 2) return;
        for (Block b : blocks) {
            if (placed.remove(b.getLocation()) != null) { dirty = true; continue; }
            if (buildAt(b.getLocation()) != null && broken.putIfAbsent(b.getLocation(), b.getBlockData().clone()) == null) dirty = true;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(org.bukkit.event.block.BlockBurnEvent event) {
        if (active() && buildAt(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!active()) return;
        int act = state.act();
        Block b = event.getBlock();
        if (act >= 2 && act <= 8 && isTidyContainer(b.getType())) {
            playerChests.put(b.getLocation(), event.getPlayer().getName());
            dirty = true;
        }
        if (act < 3 || act >= 9) return;  // from Act 9 players count as residents
        String build = buildAt(b.getLocation());
        if (build != null) {
            placed.put(b.getLocation(), build);
            dirty = true;
            // Act 8: a bed at lowpath's place is the lesson made visible the moment it happens
            if (act == 8 && b.getType().name().endsWith("_BED") && build.equals("lowpath's place")) announce(engine("bed_queued"));
        }
    }

    private String engine(String key) {
        return canon.object("engine_lines").get(key).getAsString();
    }

    /** Iona's key chest: the first time it is opened, Morrow queues it, and the next dawn merges its stacks. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!active() || state.done("iona_chest.opened") || event.getInventory().getLocation() == null) return;
        int[] k = canon.anchor("iona_key_chest");
        Location l = event.getInventory().getLocation();
        if (l.getBlockY() != k[1] || l.getBlockZ() != k[2] || l.getBlockX() < k[0] - 1 || l.getBlockX() > k[0] + 1) return;
        state.markDone("iona_chest.opened");
        JsonObject d = new JsonObject();
        d.addProperty("by", event.getPlayer().getName());
        d.addProperty("act", state.act());
        journal.write("morrow.iona_chest_opened", d);
    }

    private void tidyIonaChest(World world) {
        if (!state.done("iona_chest.opened") || state.done("iona_chest.tidied")) return;
        int[] k = canon.anchor("iona_key_chest");
        Block b = world.getBlockAt(k[0], k[1], k[2]);
        if (!(b.getState() instanceof Chest chest)) return;
        Inventory inv = chest.getInventory();
        ItemStack[] after = sorted(inv.getContents());
        if (after != null) inv.setContents(after);
        state.markDone("iona_chest.tidied");
        announce(engine("chest_normalised"));
        journal.write("morrow.iona_chest_tidied", new JsonObject());
    }

    /** Rook's newbie chest at spawn: whatever was taken comes back at dawn. */
    private void refillKitChest(World world) {
        int[] a = canon.anchor("spawn_kit_chest");
        Block b = world.getBlockAt(a[0], a[1], a[2]);
        if (!(b.getState() instanceof Chest chest)) return;
        Inventory inv = chest.getInventory();
        com.google.gson.JsonArray kit = canon.array("kit_chest");
        boolean changed = false;
        for (int i = 0; i < kit.size() && i < inv.getSize(); i++) {
            JsonObject it = kit.get(i).getAsJsonObject();
            Material m = Material.matchMaterial(it.get("item").getAsString());
            if (m == null) continue;
            int n = it.get("count").getAsInt();
            ItemStack have = inv.getItem(i);
            if (have != null && have.getType() == m && have.getAmount() >= n) continue;
            inv.setItem(i, new ItemStack(m, n));
            changed = true;
        }
        if (changed) announce(engine("kit_restored"));
    }

    /**
     * Act 5: the first dawn a resident is online after the act opens, Morrow "normalises" the three
     * rows of Rook's floor nearest the ladder into a colour run, says so, and leaves its note on the
     * back of his sign. LogBlock still holds every original block.
     */
    private void tidyFloor(World world) {
        if (state.act() < 5 || state.done("act5.floor_tidy")) return;
        if (world.getPlayers().stream().noneMatch(p -> !p.isOp() || plugin.getConfig().getBoolean("auto_advance_count_ops", false))) return;
        JsonObject floor = canon.object("floor");
        com.google.gson.JsonArray tidied = floor.getAsJsonArray("tidied"), rows = floor.getAsJsonArray("tidied_rows"), dz = floor.getAsJsonArray("row_dz");
        int[] f = canon.anchor("rookery_cellar_floor");
        int n = 0;
        for (JsonElement re : rows) {
            int r = re.getAsInt();
            com.google.gson.JsonArray row = tidied.get(r).getAsJsonArray();
            for (int c = 0; c < row.size(); c++) {
                Block b = world.getBlockAt(f[0] + c, f[1], f[2] + dz.get(r).getAsInt());
                Material want = WOOL[row.get(c).getAsInt()];
                if (b.getType() != want) {
                    b.setType(want, false);
                    n++;
                }
            }
        }
        state.markDone("act5.floor_tidy");
        announce(engine("floor_tidy"));
        int[] s = canon.anchor("rookery_floor_sign");
        List<String> note = canon.signBackNotes.get("rookery_floor_sign");
        if (note != null && !ActDirector.noteOnBack(world.getBlockAt(s[0], s[1], s[2]), note)) {
            plugin.getLogger().warning("the floor sign's note could not be made readable");
        }
        JsonObject d = new JsonObject();
        d.addProperty("blocks_changed", n);
        journal.write("morrow.floor_tidy", d);
    }

    static final Material[] WOOL = {Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
            Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL,
            Material.PURPLE_WOOL, Material.BLUE_WOOL, Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL};

    private void tick() {
        if (dirty) save();
        if (!active()) return;
        World world = Bukkit.getWorlds().get(0);
        long time = world.getTime();
        long day = world.getFullTime() / 24000L;
        if (time < 200 && day != lastDawnDay) {
            lastDawnDay = day;
            dawn(world);
        }
    }

    /** Runs once per in-game sunrise. */
    public void dawn(World world) {
        if (!active()) return;
        // players' blocks go first, so a hole they refilled is open again when the original comes back
        Map<String, Integer> normalised = new LinkedHashMap<>();
        for (var e : placed.entrySet()) {
            Block b = e.getKey().getBlock();
            if (!b.getType().isAir()) {
                removeWhole(b);
                normalised.merge(e.getValue(), 1, Integer::sum);
            }
        }
        placed.clear();
        Map<String, Integer> restored = new LinkedHashMap<>();
        for (var it = broken.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Block b = e.getKey().getBlock();
            if (!b.getType().isAir()) continue;  // something else stands there: the original is kept for a later dawn
            b.setBlockData(e.getValue(), false);
            restored.merge(buildAt(e.getKey()), 1, Integer::sum);
            it.remove();
        }
        restored.forEach((where, n) -> announce(n + (n == 1 ? " block" : " blocks") + " restored (" + where + ")"));
        normalised.forEach((where, n) -> announce(n + (n == 1 ? " block" : " blocks") + " normalised (" + where + ")"));
        int act = state.act();
        if (act >= 2 && act <= 8) {
            int sorted = tidyPlayerChests();
            if (sorted > 0) announce(sorted + (sorted == 1 ? " chest" : " chests") + " normalised");
        }
        if (act >= 3) tidyOneTallyChest(world);
        refillKitChest(world);
        tidyFloor(world);
        tidyIonaChest(world);
        if (!restored.isEmpty() || !normalised.isEmpty()) {
            JsonObject d = new JsonObject();
            d.addProperty("restored", restored.toString());
            d.addProperty("normalised", normalised.toString());
            journal.write("morrow.dawn", d);
        }
        for (Consumer<World> hook : dawnHooks) {
            try {
                hook.accept(world);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("dawn hook failed: " + ex);
            }
        }
        save();
    }

    /** Removes a block, and the other half of a bed or door, so nothing is left half-standing. */
    static void removeWhole(Block b) {
        BlockData d = b.getBlockData();
        Block other = null;
        if (d instanceof Bed bed) {
            other = bed.getPart() == Bed.Part.HEAD ? b.getRelative(bed.getFacing().getOppositeFace()) : b.getRelative(bed.getFacing());
        } else if (d instanceof Bisected bi && !(d instanceof Stairs) && !(d instanceof TrapDoor)) {
            other = bi.getHalf() == Bisected.Half.BOTTOM ? b.getRelative(0, 1, 0) : b.getRelative(0, -1, 0);
        }
        if (other != null && other.getType() == b.getType()) other.setType(Material.AIR, false);
        b.setType(Material.AIR, false);
    }

    private void announce(String text) {
        Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY)));
        journal.recovery(text);
    }

    // ---- Acts 2-8: the players' own chests, squared off -------------------------------------------

    private int tidyPlayerChests() {
        int changed = 0;
        for (var it = playerChests.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Location loc = e.getKey();
            if (loc.getWorld() == null) continue;  // (loads the chunk if needed: a handful of chests, once a day)
            Block b = loc.getBlock();
            if (!isTidyContainer(b.getType()) || !(b.getState() instanceof Container c)) {
                it.remove();
                dirty = true;
                continue;
            }
            Inventory inv = c instanceof Chest chest ? chest.getInventory() : c.getInventory();  // whole double chest
            ItemStack[] before = inv.getContents();
            ItemStack[] after = sorted(before);
            if (after == null || Arrays.equals(before, after)) continue;
            inv.setContents(after);
            changed++;
            JsonObject d = new JsonObject();
            d.addProperty("owner", e.getValue());
            d.addProperty("at", loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            d.addProperty("stacks_before", Arrays.stream(before).filter(s -> s != null && !s.getType().isAir()).count());
            d.addProperty("stacks_after", Arrays.stream(after).filter(s -> s != null && !s.getType().isAir()).count());
            journal.write("morrow.tidy_player_chest", d);
        }
        return changed;
    }

    /**
     * Merge similar stacks (same item AND same data: names, enchantments, contents are kept),
     * order them by item type (stable: first-seen order among equals), pack from slot 0.
     * Returns null if it would not fit (overstacked input), so the chest is left alone.
     */
    static ItemStack[] sorted(ItemStack[] contents) {
        List<ItemStack> reps = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (ItemStack s : contents) {
            if (s == null || s.getType().isAir()) continue;
            int found = -1;
            for (int i = 0; i < reps.size(); i++) if (reps.get(i).isSimilar(s)) { found = i; break; }
            if (found < 0) {
                ItemStack rep = s.clone();
                rep.setAmount(1);
                reps.add(rep);
                totals.add(s.getAmount());
            } else {
                totals.set(found, totals.get(found) + s.getAmount());
            }
        }
        Integer[] order = new Integer[reps.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparing((Integer i) -> reps.get(i).getType().getKey().toString()));
        List<ItemStack> out = new ArrayList<>();
        for (int i : order) {
            ItemStack rep = reps.get(i);
            int max = Math.max(1, rep.getMaxStackSize());
            for (int left = totals.get(i); left > 0; left -= max) {
                ItemStack s = rep.clone();
                s.setAmount(Math.min(left, max));
                out.add(s);
            }
        }
        if (out.size() > contents.length) return null;
        ItemStack[] result = new ItemStack[contents.length];
        for (int i = 0; i < out.size(); i++) result[i] = out.get(i);
        return result;
    }

    /** cairn's tally chests, in join order: merge one chest's weekly stacks into tidy full stacks. */
    private void tidyOneTallyChest(World world) {
        int[] door = canon.anchor("cairn_storehouse_door_2011");
        for (int row = 0; row < 6; row++) {
            String flag = "tidy.tally." + row;
            if (state.done(flag)) continue;
            Block block = world.getBlockAt(door[0] + 2, door[1], door[2] + 2 + row * 2);
            if (!(block.getState() instanceof Chest chest)) {
                state.markDone(flag);
                continue;
            }
            Inventory inv = chest.getBlockInventory();
            Map<Material, Integer> totals = new LinkedHashMap<>();
            for (ItemStack it : inv.getContents()) if (it != null) totals.merge(it.getType(), it.getAmount(), Integer::sum);
            inv.clear();
            List<ItemStack> tidy = new ArrayList<>();
            totals.forEach((m, n) -> {
                for (int left = n; left > 0; left -= m.getMaxStackSize()) tidy.add(new ItemStack(m, Math.min(left, m.getMaxStackSize())));
            });
            tidy.forEach(inv::addItem);
            state.markDone(flag);
            JsonObject d = new JsonObject();
            d.addProperty("row", row);
            d.addProperty("totals", totals.toString());
            journal.write("morrow.tidy_tally", d);
            return;  // one chest per dawn: a soft race, not a wipe
        }
    }

    // ---- persistence ------------------------------------------------------------------------------

    private static Map<String, Object> key(Location l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", l.getWorld() == null ? "" : l.getWorld().getName());
        m.put("x", l.getBlockX());
        m.put("y", l.getBlockY());
        m.put("z", l.getBlockZ());
        return m;
    }

    private static Location loc(Map<?, ?> m) {
        World w = Bukkit.getWorld(String.valueOf(m.get("world")));
        if (w == null) return null;
        return new Location(w, ((Number) m.get("x")).intValue(), ((Number) m.get("y")).intValue(), ((Number) m.get("z")).intValue());
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> m : y.getMapList("broken")) {
            Location l = loc(m);
            if (l == null) continue;
            try {
                broken.put(l, Bukkit.createBlockData(String.valueOf(m.get("data"))));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("recovery.yml: unreadable block data " + m.get("data"));
            }
        }
        for (Map<?, ?> m : y.getMapList("placed")) {
            Location l = loc(m);
            if (l != null) placed.put(l, String.valueOf(m.get("build")));
        }
        for (Map<?, ?> m : y.getMapList("player_chests")) {
            Location l = loc(m);
            if (l != null) playerChests.put(l, String.valueOf(m.get("owner")));
        }
        plugin.getLogger().info("recovery: " + broken.size() + " pending restorations, " + placed.size()
                + " placements, " + playerChests.size() + " player chests carried over");
    }

    /** Also called on disable. */
    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("broken", broken.entrySet().stream().map(e -> {
            Map<String, Object> m = key(e.getKey());
            m.put("data", e.getValue().getAsString());
            return m;
        }).toList());
        y.set("placed", placed.entrySet().stream().map(e -> {
            Map<String, Object> m = key(e.getKey());
            m.put("build", e.getValue());
            return m;
        }).toList());
        y.set("player_chests", playerChests.entrySet().stream().map(e -> {
            Map<String, Object> m = key(e.getKey());
            m.put("owner", e.getValue());
            return m;
        }).toList());
        try {
            file.getParentFile().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("could not save " + file + ": " + ex.getMessage());
        }
    }
}
