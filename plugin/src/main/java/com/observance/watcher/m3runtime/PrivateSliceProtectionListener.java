package com.observance.watcher.m3runtime;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Fail-closed private-slice protections. Operator bypass is deliberately not player evidence. */
public final class PrivateSliceProtectionListener implements Listener {
    private final PrivateSliceWorld slice;

    public PrivateSliceProtectionListener(PrivateSliceWorld slice) { this.slice = slice; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlockPlaced().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (slice.inside(event.getEntity().getLocation()) && event.getDamager() instanceof Player player
                && !bypass(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainer(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || bypass(player)) return;
        if (event.getInventory().getLocation() != null && slice.inside(event.getInventory().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (bypass(event.getPlayer())) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && !"CHORUS_FRUIT".equals(event.getCause().name())) return;
        if (slice.inside(event.getFrom()) || slice.inside(event.getTo())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player != null && !player.isOp()) player.setGameMode(org.bukkit.GameMode.ADVENTURE);
    }

    private static boolean bypass(Player player) {
        return player != null && (player.isOp() || player.hasPermission("observance.admin"));
    }
}
