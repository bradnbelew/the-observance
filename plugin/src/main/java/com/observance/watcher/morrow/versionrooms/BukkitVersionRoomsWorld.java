package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.versionrooms.VersionRoomsInstaller.Origin;
import com.observance.watcher.morrow.versionrooms.VersionRoomsManifest.Cell;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/** Primary-thread Paper world port for the bounded M05 installer. */
public final class BukkitVersionRoomsWorld implements VersionRoomsInstaller.WorldPort {
    private final World world;
    private final Origin origin;
    private final String binding;

    public BukkitVersionRoomsWorld(World world, Origin origin) {
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.binding = world.getName() + ":" + world.getUID();
        requirePrimaryThread();
    }

    @Override public String binding() { return binding; }
    @Override public String blockData(Cell relative) {
        requirePrimaryThread();
        return block(relative).getBlockData().getAsString(true);
    }
    @Override public boolean isAir(Cell relative) {
        requirePrimaryThread();
        return block(relative).getType().isAir();
    }
    @Override public void setBlockData(Cell relative, String blockData) {
        requirePrimaryThread();
        block(relative).setBlockData(Bukkit.createBlockData(blockData), false);
    }

    private Block block(Cell relative) {
        return world.getBlockAt(origin.x() + relative.x(), origin.y() + relative.y(), origin.z() + relative.z());
    }
    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M05 world access requires Paper's primary thread");
    }
}
