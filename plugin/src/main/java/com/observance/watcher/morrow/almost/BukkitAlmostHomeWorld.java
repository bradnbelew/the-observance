package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.almost.AlmostHomeInstaller.Origin;
import com.observance.watcher.morrow.almost.AlmostHomeManifest.Cell;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

/** Primary-thread Paper world port for the bounded M08 installer. */
public final class BukkitAlmostHomeWorld implements AlmostHomeInstaller.WorldPort {
    private final World world; private final Origin origin; private final String binding;
    public BukkitAlmostHomeWorld(World world, Origin origin) {
        this.world = Objects.requireNonNull(world, "world"); this.origin = Objects.requireNonNull(origin, "origin");
        this.binding = world.getName() + ":" + world.getUID(); requirePrimaryThread();
    }
    @Override public String binding() { return binding; }
    @Override public String blockData(Cell relative) { requirePrimaryThread(); return block(relative).getBlockData().getAsString(true); }
    @Override public boolean matches(Cell relative, String expectedBlockData) {
        requirePrimaryThread(); BlockData expected = Bukkit.createBlockData(expectedBlockData);
        return block(relative).getBlockData().matches(expected);
    }
    @Override public boolean isAir(Cell relative) { requirePrimaryThread(); return block(relative).getType().isAir(); }
    @Override public void setBlockData(Cell relative, String blockData) {
        requirePrimaryThread(); block(relative).setBlockData(Bukkit.createBlockData(blockData), false);
    }
    private Block block(Cell relative) {
        return world.getBlockAt(origin.x() + relative.x(), origin.y() + relative.y(), origin.z() + relative.z());
    }
    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M08 world access requires Paper's primary thread");
    }
}
