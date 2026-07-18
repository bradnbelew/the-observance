package com.observance.watcher.m3runtime;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.function.BooleanSupplier;

/** Fail-closed private-slice protection with explicit full-gate crossing enforcement. */
public final class PrivateSliceProtectionListener implements Listener {
    private final PrivateSliceWorld slice;
    private final BooleanSupplier gateOpen;

    public PrivateSliceProtectionListener(PrivateSliceWorld slice, BooleanSupplier gateOpen) {
        this.slice = slice;
        this.gateOpen = gateOpen;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlockPlaced().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (slice.inside(event.getEntity().getLocation()) && event.getDamager() instanceof Player player
                && !bypass(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!bypass(event.getPlayer()) && slice.inside(event.getRightClicked().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player && !bypass(player)
                && slice.inside(event.getEntity().getLocation())) event.setCancelled(true);
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
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        if (slice.inside(event.getFrom()) || slice.inside(event.getTo())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (gateOpen.getAsBoolean() || bypass(event.getPlayer()) || event.getTo() == null) return;
        if (!slice.beyondClosedGate(event.getFrom()) && slice.beyondClosedGate(event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player != null && !player.isOp()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(slice.absolute(0, 0, 2));
        }
    }

    private static boolean bypass(Player player) {
        return player != null && (player.isOp() || player.hasPermission("observance.admin"));
    }
}
