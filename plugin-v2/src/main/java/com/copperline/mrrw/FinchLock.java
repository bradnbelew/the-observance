package com.copperline.mrrw;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * D1: finch's 2011 torch lock on her spur station. Real redstone, built in the open so the wiring
 * can be read. Lever i stands on a stone pad; its dust runs south either straight into the
 * collector (bit 0) or through a block with a redstone torch on its far side (bit 1, an inverter).
 * The collector ORs the eight channels; a final torch inverts it into the iron door. The door is
 * powered only when every channel is off, i.e. when lever i is ON exactly where the bit is 1.
 * Each flip arms a 20-second "booster" lockout, so guessing all 256 combinations takes hours.
 */
public final class FinchLock implements Listener {
    private static final long LOCKOUT_MS = 20_000;
    private final Canon canon;
    private long lockedUntil;

    public FinchLock(Canon canon) {
        this.canon = canon;
    }

    private int[] origin() {
        return canon.anchor("finch_spur");
    }

    public void build(World w) {
        int[] o = origin();
        String bits = canon.string("finch_lock_bits");
        int x0 = o[0], y = o[1], z0 = o[2];
        // floor and clear room
        for (int x = x0 - 2; x <= x0 + 16; x++) for (int z = z0 - 2; z <= z0 + 12; z++) {
            w.getBlockAt(x, y - 1, z).setType(Material.STONE, false);
            for (int dy = 0; dy <= 3; dy++) w.getBlockAt(x, y + dy, z).setType(Material.AIR, false);
        }
        for (int i = 0; i < 8; i++) {
            int x = x0 + 2 * i;
            w.getBlockAt(x, y, z0).setType(Material.STONE, false);  // the pad the lever stands on
            Switch lever = (Switch) Material.LEVER.createBlockData();
            lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
            lever.setFacing(BlockFace.SOUTH);
            lever.setPowered(false);
            w.getBlockAt(x, y + 1, z0).setBlockData(lever, false);
            if (bits.charAt(i) == '1') {  // inverter channel: dust, block, torch on the far side, dust
                wire(w, x, y, z0 + 1, BlockFace.NORTH, BlockFace.SOUTH);
                w.getBlockAt(x, y, z0 + 2).setType(Material.STONE, false);
                RedstoneWallTorch t = (RedstoneWallTorch) Material.REDSTONE_WALL_TORCH.createBlockData();
                t.setFacing(BlockFace.SOUTH);
                w.getBlockAt(x, y, z0 + 3).setBlockData(t, false);
            } else {  // direct channel
                for (int dz = 1; dz <= 3; dz++) wire(w, x, y, z0 + dz, BlockFace.NORTH, BlockFace.SOUTH);
            }
            wire(w, x, y, z0 + 4, BlockFace.NORTH, BlockFace.SOUTH);
        }
        int xc = x0 + 7;
        for (int x = x0; x <= x0 + 14; x++) {  // collector (OR): an east-west line, joined by each channel and the branch
            boolean joined = (x - x0) % 2 == 0 || x == xc;
            if (joined) wire(w, x, y, z0 + 5, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, x == xc ? BlockFace.SOUTH : null);
            else wire(w, x, y, z0 + 5, BlockFace.EAST, BlockFace.WEST);
        }
        wire(w, xc, y, z0 + 6, BlockFace.NORTH, BlockFace.SOUTH);
        w.getBlockAt(xc, y, z0 + 7).setType(Material.STONE, false);
        RedstoneWallTorch fin = (RedstoneWallTorch) Material.REDSTONE_WALL_TORCH.createBlockData();
        fin.setFacing(BlockFace.SOUTH);
        w.getBlockAt(xc, y, z0 + 8).setBlockData(fin, false);
        // the door, and finch's room behind it: a closed cell in the station's south-west corner. The
        // spur tunnel (build2011.py) comes in at the south wall east of the cell, so you reach the levers
        // first and the door last (before 2026-10 the door stood in a short free wall you could walk round).
        int cellEast = x0 + 8;
        for (int x = x0 - 2; x <= cellEast; x++) for (int dy = 0; dy <= 3; dy++) w.getBlockAt(x, y + dy, z0 + 9).setType(Material.COBBLESTONE, false);
        for (int z = z0 + 10; z <= z0 + 12; z++) for (int dy = 0; dy <= 3; dy++) w.getBlockAt(cellEast, y + dy, z).setType(Material.COBBLESTONE, false);
        // torches: the clear above took the ones the 2011 station had
        int[][] torches = {{x0 - 2, z0 - 2}, {x0 + 16, z0 - 2}, {x0 - 2, z0 + 7}, {x0 + 16, z0 + 7}, {x0 + 12, z0 + 12}, {x0 + 16, z0 + 12}, {x0 - 1, z0 + 12}};
        for (int[] t : torches) w.getBlockAt(t[0], y, t[1]).setType(Material.TORCH, false);
        Door door = (Door) Material.IRON_DOOR.createBlockData();
        door.setFacing(BlockFace.SOUTH);
        door.setHalf(Bisected.Half.BOTTOM);
        w.getBlockAt(xc, y, z0 + 9).setBlockData(door, false);
        Door top = (Door) door.clone();
        top.setHalf(Bisected.Half.TOP);
        w.getBlockAt(xc, y + 1, z0 + 9).setBlockData(top, false);
        Block sign = w.getBlockAt(xc, y, z0 + 11);
        sign.setType(Material.OAK_SIGN, false);
        if (sign.getState() instanceof Sign s) {
            List<String> lines = Canon.strings(canon.array("finch_spur_sign"));
            for (int i = 0; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            s.update(true, false);
        }
        // let redstone settle from the levers' current (all off) state
        for (int i = 0; i < 8; i++) {
            Block lever = w.getBlockAt(x0 + 2 * i, y + 1, z0);
            lever.setBlockData(lever.getBlockData(), true);
        }
    }

    /** Redstone dust placed by code is a "dot" that powers nothing; give it explicit connections. */
    private static void wire(World w, int x, int y, int z, BlockFace... faces) {
        RedstoneWire data = (RedstoneWire) Material.REDSTONE_WIRE.createBlockData();
        for (BlockFace f : faces) if (f != null) data.setFace(f, RedstoneWire.Connection.SIDE);
        w.getBlockAt(x, y, z).setBlockData(data, false);
    }

    public boolean doorOpen(World w) {
        int[] o = origin();
        return w.getBlockAt(o[0] + 7, o[1], o[2] + 9).getBlockData() instanceof Door d && d.isOpen();
    }

    @EventHandler
    public void onLever(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getHand() == EquipmentSlot.OFF_HAND || e.getClickedBlock().getType() != Material.LEVER) return;
        int[] o = origin();
        Location l = e.getClickedBlock().getLocation();
        if (l.getBlockY() != o[1] + 1 || l.getBlockZ() != o[2] || l.getBlockX() < o[0] || l.getBlockX() > o[0] + 14) return;
        long now = System.currentTimeMillis();
        if (now < lockedUntil) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("the booster is still running", NamedTextColor.GRAY));
            e.getPlayer().playSound(l, Sound.ENTITY_MINECART_RIDING, 0.6f, 1f);
            return;
        }
        lockedUntil = now + LOCKOUT_MS;
    }
}
