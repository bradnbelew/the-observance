package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps three puzzle fixtures honest against ordinary play (2026-10 audit):
 *  - Morrow's dust corridor (D13): redstone dust written at exact power levels with no source.
 *    Any neighbour update would recompute it to 0, so redstone/physics changes at those exact
 *    blocks are refused, the corridor can't be dug, built in, flooded or pushed, and the levels
 *    are re-asserted every few seconds and at dawn. F3 still shows "power: N" on each dot.
 *  - cairn's sealed room (C16): sculk shell that would otherwise mine instantly. The shell and
 *    its iron door can't be broken, blown up or pushed; the door ignores redstone (the room
 *    opens only for the closing routine). The calibrated sensors still hear and still output.
 *  - patchcord's note-block wall (C2): right-click would re-tune it and destroy BAGGAGE. Right-
 *    click tuning is refused (left-click still plays), the wall and its base can't be broken,
 *    and the pitches are restored at dawn.
 * Operators in creative mode can still edit everything. After the stop, nothing is enforced.
 */
public final class Fixtures implements Listener {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final File file;
    private List<Integer> tune;

    public Fixtures(Plugin plugin, Canon canon, State state, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.file = new File(plugin.getDataFolder(), "fixtures.yml");
        Bukkit.getScheduler().runTaskTimer(plugin, this::watchCorridor, 100L, 100L);
    }

    private boolean active() {
        return !state.done("stopped");
    }

    // ---- regions ---------------------------------------------------------------------------------

    private int corridorLength() {
        return canon.array("light_corridor").size();
    }

    /** Wire tile i, or -1. */
    private int corridorTile(Block b) {
        int[] a = canon.anchor("dust_corridor");
        int i = b.getX() - a[0];
        if (b.getY() != a[1] + 1 || b.getZ() != a[2] || i < 0 || i >= corridorLength()) return -1;
        return i;
    }

    private boolean inCorridor(Block b) {
        if (!state.done("install.dust_corridor")) return false;
        int[] a = canon.anchor("dust_corridor");
        int i = b.getX() - a[0], dy = b.getY() - a[1], dz = b.getZ() - a[2];
        return i >= 0 && i < corridorLength() && dy >= 0 && dy <= 3 && dz >= -1 && dz <= 1;
    }

    private boolean inShell(Block b) {
        if (!state.done("install.listening_room")) return false;
        int[] a = canon.anchor("listening_room");
        int dx = b.getX() - a[0], dy = b.getY() - a[1], dz = b.getZ() - a[2];
        if (dx < 0 || dx > 4 || dy < 0 || dy > 3 || dz < 0 || dz > 4) return false;
        return dx == 0 || dx == 4 || dz == 0 || dz == 4 || dy == 0 || dy == 3;
    }

    private boolean isRoomDoor(Block b) {
        if (!state.done("install.listening_room")) return false;
        int[] a = canon.anchor("listening_room");
        return b.getX() == a[0] + 2 && b.getZ() == a[2] && (b.getY() == a[1] + 1 || b.getY() == a[1] + 2);
    }

    /** Note block i of patchcord's wall, or -1. */
    private int noteIndex(Block b) {
        List<Integer> t = tune();
        if (t == null) return -1;
        int[] p = canon.anchor("patchcord_relay_hut");
        int i = b.getX() - p[0];
        if (b.getY() != p[1] + 1 || b.getZ() != p[2] + 2 || i < 0 || i >= t.size()) return -1;
        return i;
    }

    private boolean inNoteWall(Block b) {
        List<Integer> t = tune();
        if (t == null) return false;
        int[] p = canon.anchor("patchcord_relay_hut");
        int i = b.getX() - p[0];
        return b.getZ() == p[2] + 2 && (b.getY() == p[1] || b.getY() == p[1] + 1) && i >= 0 && i < t.size();
    }

    boolean isProtected(Block b) {
        return active() && (inCorridor(b) || inShell(b) || inNoteWall(b));
    }

    private static boolean operatorEditing(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission("mrrw.operator");
    }

    // ---- the tune: canon if it carries one, otherwise a snapshot of the wall as first found ---------

    private List<Integer> tune() {
        if (tune != null) return tune.isEmpty() ? null : tune;
        tune = new ArrayList<>();
        JsonElement fromCanon = canon.raw.get("tune");
        if (fromCanon != null && fromCanon.isJsonArray()) {
            for (JsonElement e : fromCanon.getAsJsonArray()) {
                if (e.isJsonArray()) tune.add(e.getAsJsonArray().get(0).getAsInt());
                else if (e.isJsonObject()) tune.add(e.getAsJsonObject().get("pitch").getAsInt());
                else tune.add(e.getAsInt());
            }
            return tune;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        tune.addAll(y.getIntegerList("tune"));
        if (!tune.isEmpty()) return tune;
        // first sight: read the wall that inject.py wrote into the Beta world, before players reach it
        World w = Bukkit.getWorlds().get(0);
        int[] p = canon.anchor("patchcord_relay_hut");
        for (int i = 0; i < 24; i++) {
            if (!(w.getBlockAt(p[0] + i, p[1] + 1, p[2] + 2).getBlockData() instanceof NoteBlock nb)) break;
            tune.add((int) nb.getNote().getId());
        }
        if (tune.isEmpty()) return null;
        y.set("tune", tune);
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("could not save " + file + ": " + ex.getMessage());
        }
        JsonObject d = new JsonObject();
        d.addProperty("tune_snapshot", tune.toString());
        journal.write("fixtures.tune", d);
        plugin.getLogger().info("patchcord's tune snapshot " + tune + " (expect the bass line to spell BAGGAGE)");
        return tune;
    }

    // ---- protection --------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (isProtected(e.getBlock()) && !operatorEditing(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (active() && inCorridor(e.getBlock()) && !operatorEditing(e.getPlayer())) e.setCancelled(true);
    }

    /** Right-click re-tunes a note block; refuse it on the wall. Left-click still plays it. */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || !active()) return;
        Block b = e.getClickedBlock();
        if (b.getType() == Material.NOTE_BLOCK && noteIndex(b) >= 0 && !operatorEditing(e.getPlayer())) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent e) {
        if (!active()) return;
        Block b = e.getBlock();
        int i = corridorTile(b);
        if (i >= 0 && state.done("install.dust_corridor")) {
            e.setNewCurrent(canon.array("light_corridor").get(i).getAsInt());
        } else if (isRoomDoor(b)) {
            e.setNewCurrent(0);  // the room opens for cairn's routine, not for a button
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        Block b = e.getBlock();
        if (b.getType() == Material.REDSTONE_WIRE && active() && state.done("install.dust_corridor") && corridorTile(b) >= 0) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (isProtected(e.getToBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(this::isProtected) || isProtected(e.getBlock().getRelative(e.getDirection()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(this::isProtected)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isProtected);
    }

    // ---- re-assertion ------------------------------------------------------------------------------

    /** Every 5 s while the corridor is loaded: put any dust level back exactly as written. */
    private void watchCorridor() {
        if (!active() || !state.done("install.dust_corridor")) return;
        World w = Bukkit.getWorlds().get(0);
        int[] a = canon.anchor("dust_corridor");
        if (!w.isChunkLoaded(a[0] >> 4, a[2] >> 4)) return;
        int fixed = assertCorridor(w, false);
        if (fixed > 0) {
            JsonObject d = new JsonObject();
            d.addProperty("dust_reasserted", fixed);
            journal.write("fixtures.corridor", d);
        }
    }

    /** Returns how many blocks it had to put back. With shell, the floor/walls/ceiling too. */
    int assertCorridor(World w, boolean shell) {
        JsonArray levels = canon.array("light_corridor");
        int fixed = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (shell) {
                for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 3; dy++) {
                    if (dy == 1 && dz == 0) continue;
                    Block b = at(w, "dust_corridor", i, dy, dz);
                    Material want = dy == 0 || dy == 3 || dz != 0 ? Material.DEEPSLATE_TILES : Material.AIR;
                    if (b.getType() != want) {
                        b.setType(want, false);
                        fixed++;
                    }
                }
            }
            Block dust = at(w, "dust_corridor", i, 1, 0);
            int want = levels.get(i).getAsInt();
            if (dust.getBlockData() instanceof AnaloguePowerable ap && dust.getType() == Material.REDSTONE_WIRE && ap.getPower() == want) continue;
            if (dust.getType() != Material.REDSTONE_WIRE && !dust.getType().isAir()) dust.setType(Material.AIR, false);
            dust.setBlockData(ModernLayer.dust(want), false);  // no physics: the level stays exactly as written
            fixed++;
        }
        return fixed;
    }

    /** Puts patchcord's pitches back. Returns how many were re-tuned. */
    int restoreTune(World w) {
        List<Integer> t = tune();
        if (t == null) return 0;
        int[] p = canon.anchor("patchcord_relay_hut");
        int fixed = 0;
        for (int i = 0; i < t.size(); i++) {
            Block b = w.getBlockAt(p[0] + i, p[1] + 1, p[2] + 2);
            BlockData data = b.getType() == Material.NOTE_BLOCK ? b.getBlockData() : Material.NOTE_BLOCK.createBlockData();
            if (!(data instanceof NoteBlock nb)) continue;
            if (b.getType() == Material.NOTE_BLOCK && nb.getNote().getId() == t.get(i)) continue;
            nb.setNote(new Note(t.get(i)));
            b.setBlockData(nb, false);
            fixed++;
        }
        return fixed;
    }

    /** Dawn: the corridor exactly as written, the tune as patchcord left it. */
    public void dawn(World w) {
        if (!active()) return;
        int corridor = state.done("install.dust_corridor") ? assertCorridor(w, true) : 0;
        int notes = restoreTune(w);
        if (corridor + notes > 0) {
            JsonObject d = new JsonObject();
            d.addProperty("corridor_blocks", corridor);
            d.addProperty("notes_retuned", notes);
            journal.write("fixtures.dawn", d);
        }
    }

    private Block at(World w, String anchor, int dx, int dy, int dz) {
        int[] a = canon.anchor(anchor);
        return w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
    }
}
