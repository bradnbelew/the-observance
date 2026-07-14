package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.util.Safety;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Guards production Deep Hold regions. BeatProtectionListener protects authored anchor blocks; this
 * listener protects the whole carved placement so normal players cannot tunnel around gates, flood
 * rooms, or build over evidence. Coll's third-lamp proof is an interaction with the authored copper
 * bulb, not a radius-wide placement exemption; all player block placement remains denied.
 */
public final class HoldProtectionListener implements Listener {

    private static final String HOLD_REGION_TYPE = "hold_region";
    private final Supplier<SitesConfig> sites;
    private final Safety safety;

    public HoldProtectionListener(Supplier<SitesConfig> sites, Safety safety) {
        this.sites = sites;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        safety.run("hold.protect.break", () -> {
            if (canBypass(event.getPlayer())) return;
            Block block = event.getBlock();
            if (block == null || !insideHold(block.getLocation())) return;
            event.setCancelled(true);
            deny(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        safety.run("hold.protect.place", () -> {
            if (canBypass(event.getPlayer())) return;
            Block block = event.getBlockPlaced();
            if (block == null || !insideHold(block.getLocation())) return;
            event.setCancelled(true);
            deny(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        safety.run("hold.protect.bucketEmpty", () -> {
            if (canBypass(event.getPlayer())) return;
            Block clicked = event.getBlockClicked();
            if (clicked != null && insideHold(clicked.getLocation())) {
                event.setCancelled(true);
                deny(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        safety.run("hold.protect.bucketFill", () -> {
            if (canBypass(event.getPlayer())) return;
            Block clicked = event.getBlockClicked();
            if (clicked != null && insideHold(clicked.getLocation())) {
                event.setCancelled(true);
                deny(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent event) {
        safety.run("hold.protect.fromto", () -> {
            Block to = event.getToBlock();
            Block from = event.getBlock();
            if ((to != null && insideHold(to.getLocation()))
                    || (from != null && insideHold(from.getLocation()))) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        safety.run("hold.protect.burn", () -> {
            Block block = event.getBlock();
            if (block != null && insideHold(block.getLocation())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        safety.run("hold.protect.ignite", () -> {
            Block block = event.getBlock();
            if (block != null && insideHold(block.getLocation())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        safety.run("hold.protect.entityExplode", () -> removeHoldBlocks(event.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        safety.run("hold.protect.blockExplode", () -> removeHoldBlocks(event.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        safety.run("hold.protect.pistonExtend", () -> {
            if (event.getBlock() != null && insideHold(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
            if (anyInsideHold(event.getBlocks())) {
                event.setCancelled(true);
                return;
            }
            for (Block moved : event.getBlocks()) {
                if (moved != null && insideHold(moved.getRelative(event.getDirection()).getLocation())) {
                    event.setCancelled(true);
                    return;
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        safety.run("hold.protect.pistonRetract", () -> {
            if (event.getBlock() != null && insideHold(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
            if (anyInsideHold(event.getBlocks())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        safety.run("hold.protect.entityChange", () -> {
            Block block = event.getBlock();
            if (block != null && insideHold(block.getLocation())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAuthoredEntityDamage(EntityDamageByEntityEvent event) {
        safety.run("hold.protect.entityDamage", () -> {
            if (!(event.getEntity() instanceof Hanging) && !(event.getEntity() instanceof ArmorStand)) return;
            if (!insideHold(event.getEntity().getLocation())) return;
            if (event.getDamager() instanceof Player player && canBypass(player)) return;
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) deny(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        safety.run("hold.protect.hangingBreak", () -> {
            if (event.getEntity() != null && insideHold(event.getEntity().getLocation())) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        safety.run("hold.protect.hangingPlace", () -> {
            if (event.getEntity() == null || !insideHold(event.getEntity().getLocation())) return;
            Player player = event.getPlayer();
            if (canBypass(player)) return;
            event.setCancelled(true);
            deny(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        safety.run("hold.protect.armorStand", () -> {
            if (event.getRightClicked() == null || !insideHold(event.getRightClicked().getLocation())) return;
            if (canBypass(event.getPlayer())) return;
            event.setCancelled(true);
            deny(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        safety.run("hold.protect.inventoryMove", () -> {
            Location source = event.getSource() == null ? null : event.getSource().getLocation();
            Location destination = event.getDestination() == null ? null : event.getDestination().getLocation();
            if ((source != null && insideHold(source)) || (destination != null && insideHold(destination))) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        safety.run("hold.protect.teleport", () -> {
            if (canBypass(event.getPlayer())) return;
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                    && !"CHORUS_FRUIT".equals(event.getCause().name())) return;
            if ((event.getFrom() != null && insideHold(event.getFrom()))
                    || (event.getTo() != null && insideHold(event.getTo()))) {
                event.setCancelled(true);
                deny(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeLecternBook(PlayerTakeLecternBookEvent event) {
        safety.run("hold.protect.lecternBook", () -> {
            if (canBypass(event.getPlayer())) return;
            Block lectern = event.getLectern() == null ? null : event.getLectern().getBlock();
            if (lectern == null || !insideHold(lectern.getLocation())) return;
            event.setCancelled(true);
            deny(event.getPlayer());
        });
    }

    private void removeHoldBlocks(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;
        blocks.removeIf(block -> block != null && insideHold(block.getLocation()));
    }

    private boolean anyInsideHold(List<Block> blocks) {
        if (blocks == null) return false;
        for (Block block : blocks) {
            if (block != null && insideHold(block.getLocation())) return true;
        }
        return false;
    }

    private boolean insideHold(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        SitesConfig cfg = currentSites();
        if (cfg == null) return false;
        String world = loc.getWorld().getName();
        for (Site site : cfg.all()) {
            if (site == null || !HOLD_REGION_TYPE.equals(site.type())) continue;
            if (site.contains(world, loc.getX(), loc.getY(), loc.getZ())) return true;
        }
        return false;
    }

    private SitesConfig currentSites() {
        try {
            return sites == null ? null : sites.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean canBypass(Player player) {
        return player != null && (player.isOp() || player.hasPermission("observance.admin"));
    }

    private void deny(Player player) {
        if (player != null) player.sendMessage("The Hold does not give.");
    }

}
