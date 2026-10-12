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
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the restore job does to the world while players are around.
 *
 * Act 2 (restoring): blocks players break inside a resident's build come back at dawn,
 *   with "[Recovery] N block(s) restored (cairn storehouse)". It feels protective.
 * Act 3+ (tidying): blocks players placed near the old builds are "normalised" away at dawn,
 *   and one of cairn's tally chests per dawn is merged into tidy stacks, erasing the weeks.
 * Waxed signs are never touched (Morrow treats waxed as archived). After the stop: nothing.
 */
public final class RecoveryService implements Listener {
    private static final int RADIUS = 14;
    private static final String[][] BUILDS = {
            {"rookery_house", "rook's"}, {"cairn_storehouse_door_2011", "cairn storehouse"}, {"cairn_farm", "the farms"},
            {"june_house", "june's point"}, {"june_tower", "the tower"}, {"patchcord_relay_hut", "patch's hut"},
            {"finch_hub", "rail hub"}, {"spawn", "spawn"},
    };

    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final Map<Location, BlockData> broken = new LinkedHashMap<>();
    private final Map<Location, String> placed = new LinkedHashMap<>();
    private long lastDawnDay = -1;

    public RecoveryService(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
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
        return !(state.act() >= 10 && state.done("stopped"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!active() || state.act() < 2) return;
        Block b = event.getBlock();
        if (placed.remove(b.getLocation()) != null) return;  // breaking your own block is fine
        if (buildAt(b.getLocation()) != null) broken.putIfAbsent(b.getLocation(), b.getBlockData().clone());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!active() || state.act() < 3 || state.act() >= 9) return;  // from Act 9 players count as residents
        String build = buildAt(event.getBlock().getLocation());
        if (build != null) placed.put(event.getBlock().getLocation(), build);
    }

    private void tick() {
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
        Map<String, Integer> restored = new LinkedHashMap<>();
        for (var e : broken.entrySet()) {
            Block b = e.getKey().getBlock();
            if (b.getType() == Material.AIR || b.getType().isAir()) {
                b.setBlockData(e.getValue(), false);
                restored.merge(buildAt(e.getKey()), 1, Integer::sum);
            }
        }
        broken.clear();
        Map<String, Integer> normalised = new LinkedHashMap<>();
        for (var e : placed.entrySet()) {
            Block b = e.getKey().getBlock();
            if (!b.getType().isAir()) {
                b.setType(Material.AIR, false);
                normalised.merge(e.getValue(), 1, Integer::sum);
            }
        }
        placed.clear();
        restored.forEach((where, n) -> announce(n + (n == 1 ? " block" : " blocks") + " restored (" + where + ")"));
        normalised.forEach((where, n) -> announce(n + (n == 1 ? " block" : " blocks") + " normalised (" + where + ")"));
        if (state.act() >= 3) tidyOneTallyChest(world);
        if (!restored.isEmpty() || !normalised.isEmpty()) {
            JsonObject d = new JsonObject();
            d.addProperty("restored", restored.toString());
            d.addProperty("normalised", normalised.toString());
            journal.write("morrow.dawn", d);
        }
    }

    private void announce(String text) {
        Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY)));
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
}
