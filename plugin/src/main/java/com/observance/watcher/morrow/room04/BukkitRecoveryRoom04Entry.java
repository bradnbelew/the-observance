package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Entry.AbsoluteCell;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/** Owns world spawn, unsafe-join recovery, and respawns for the isolated Room 04 runtime. */
public final class BukkitRecoveryRoom04Entry implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final World world;
    private final Location safeLocation;
    private boolean started;

    public BukkitRecoveryRoom04Entry(JavaPlugin plugin, World world, Origin origin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        AbsoluteCell safe = RecoveryRoom04Entry.safeFeet(Objects.requireNonNull(origin, "origin"));
        this.safeLocation = new Location(world, safe.x() + 0.5D, safe.y(), safe.z() + 0.5D, 0.0F, 0.0F);
    }

    public void start() {
        requirePrimaryThread();
        if (started) return;
        if (!world.setSpawnLocation(safeLocation)) {
            throw new IllegalStateException("Paper refused the Recovery Room 04 safe world spawn");
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        for (Player player : world.getPlayers()) enter(player, "startup");
        plugin.getLogger().info("MORROW_PLAYER_SAFE_ENTRY_READY world=" + world.getName()
                + " spawn=" + coordinates(safeLocation));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        enter(event.getPlayer(), "join");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!started || (!world.equals(event.getPlayer().getWorld())
                && !world.equals(event.getRespawnLocation().getWorld()))) return;
        event.setRespawnLocation(safeLocation.clone());
        plugin.getLogger().info("MORROW_PLAYER_SAFE_ENTRY player=" + event.getPlayer().getName()
                + " reason=respawn action=bound spawn=" + coordinates(safeLocation)
                + " inventory_mutations=0");
    }

    private void enter(Player player, String reason) {
        if (!started || player == null || !world.equals(player.getWorld())) return;
        Location before = player.getLocation();
        boolean recover = unsafe(before);
        if (recover && !player.teleport(safeLocation.clone())) {
            refuseUnsafeEntry(player, "Paper refused the safe teleport");
            return;
        }
        Location after = player.getLocation();
        if (unsafe(after)) {
            refuseUnsafeEntry(player, "the destination failed its physical clearance audit");
            return;
        }
        plugin.getLogger().info("MORROW_PLAYER_SAFE_ENTRY player=" + player.getName()
                + " reason=" + reason
                + " action=" + (recover ? "recovered" : "accepted")
                + " from=" + coordinates(before)
                + " to=" + coordinates(after)
                + " inventory_mutations=0");
    }

    private void refuseUnsafeEntry(Player player, String reason) {
        plugin.getLogger().severe("MORROW_PLAYER_SAFE_ENTRY_REFUSED player=" + player.getName()
                + " reason=" + reason.replaceAll("[^A-Za-z0-9 ._-]", "_"));
        player.kick(Component.text(
                "Room 04 could not establish a safe entry. Nothing was changed; please reconnect.",
                NamedTextColor.RED));
    }

    private boolean unsafe(Location location) {
        Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        return RecoveryRoom04Entry.shouldRecover(
                world.equals(location.getWorld()),
                feet.isPassable(),
                head.isPassable(),
                !floor.isPassable());
    }

    private static String coordinates(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Recovery Room 04 entry requires the Paper primary thread");
        }
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        started = false;
    }
}
