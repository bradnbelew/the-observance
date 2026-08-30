package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/** Main-thread-only Paper adapter for the pure transactional Room 04 installer. */
public final class BukkitRecoveryRoom04World implements RecoveryRoom04Installer.WorldPort {
    private final World world;
    private final Origin origin;
    private final String binding;

    public BukkitRecoveryRoom04World(World world, Origin origin) {
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.binding = world.getName() + ":" + world.getUID();
        requirePrimaryThread();
    }

    @Override
    public String binding() {
        return binding;
    }

    @Override
    public String blockData(Cell relative) {
        requirePrimaryThread();
        return block(relative).getBlockData().getAsString(true);
    }

    @Override
    public boolean isAir(Cell relative) {
        requirePrimaryThread();
        return block(relative).getType().isAir();
    }

    @Override
    public void setBlockData(Cell relative, String blockData) {
        requirePrimaryThread();
        block(relative).setBlockData(Bukkit.createBlockData(blockData), false);
    }

    private Block block(Cell relative) {
        return world.getBlockAt(
                Math.addExact(origin.x(), relative.x()),
                Math.addExact(origin.y(), relative.y()),
                Math.addExact(origin.z(), relative.z()));
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Recovery Room 04 world access requires the Paper primary thread");
        }
    }
}
