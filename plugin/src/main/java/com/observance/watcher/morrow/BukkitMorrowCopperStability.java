package com.observance.watcher.morrow;

import com.observance.watcher.ObservancePlugin;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Prevents ambient oxidation from changing authored provenance inside enabled Morrow structures. */
public final class BukkitMorrowCopperStability implements Listener, AutoCloseable {
    private final ObservancePlugin plugin;
    private final List<Bounds> bounds = new ArrayList<>();
    private boolean started;

    public BukkitMorrowCopperStability(ObservancePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void addBounds(
            World world,
            int originX,
            int originY,
            int originZ,
            int minimumX,
            int maximumX,
            int minimumY,
            int maximumY,
            int minimumZ,
            int maximumZ) {
        if (started) throw new IllegalStateException("Morrow copper bounds are already active");
        bounds.add(new Bounds(
                Objects.requireNonNull(world, "world").getUID(),
                Math.addExact(originX, minimumX), Math.addExact(originX, maximumX),
                Math.addExact(originY, minimumY), Math.addExact(originY, maximumY),
                Math.addExact(originZ, minimumZ), Math.addExact(originZ, maximumZ)));
    }

    public void start() {
        if (started || bounds.isEmpty()) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        if (!contains(block)) return;
        String before = block.getType().getKey().toString();
        String after = event.getNewState().getType().getKey().toString();
        if (CopperAgingPolicy.isNaturalAging(before, after)) event.setCancelled(true);
    }

    private boolean contains(Block block) {
        UUID worldId = block.getWorld().getUID();
        return bounds.stream().anyMatch(bound -> bound.contains(
                worldId, block.getX(), block.getY(), block.getZ()));
    }

    @Override
    public void close() {
        if (started) HandlerList.unregisterAll(this);
        started = false;
    }

    private record Bounds(
            UUID worldId,
            int minimumX,
            int maximumX,
            int minimumY,
            int maximumY,
            int minimumZ,
            int maximumZ) {
        private boolean contains(UUID candidateWorld, int x, int y, int z) {
            return worldId.equals(candidateWorld)
                    && x >= minimumX && x <= maximumX
                    && y >= minimumY && y <= maximumY
                    && z >= minimumZ && z <= maximumZ;
        }
    }
}
