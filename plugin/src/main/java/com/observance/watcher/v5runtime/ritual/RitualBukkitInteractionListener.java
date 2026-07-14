package com.observance.watcher.v5runtime.ritual;

import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Unwired Bukkit input adapter for ballot markers, RP03 book, and RP04 sector handles. */
public final class RitualBukkitInteractionListener implements Listener {
    public interface SectorResolver {
        OptionalInt occupiedLitSector(Player player);
    }

    private final Plugin plugin;
    private final VisibleBallotRite ballots;
    private final CollectivePresenceRite presence;
    private final SectorResolver sectors;
    private final NamespacedKey wrenVoteKey;
    private final NamespacedKey nameVoteKey;
    private final NamespacedKey sectorKey;
    private final NamespacedKey bookKey;

    public RitualBukkitInteractionListener(
            Plugin plugin,
            VisibleBallotRite ballots,
            CollectivePresenceRite presence,
            SectorResolver sectors) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.ballots = Objects.requireNonNull(ballots, "ballots");
        this.presence = Objects.requireNonNull(presence, "presence");
        this.sectors = Objects.requireNonNull(sectors, "sectors");
        wrenVoteKey = new NamespacedKey(plugin, "v5_wren_vote");
        nameVoteKey = new NamespacedKey(plugin, "v5_name_treatment");
        sectorKey = new NamespacedKey(plugin, "v5_rp04_sector");
        bookKey = new NamespacedKey(plugin, "v5_book_id");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        String wren = entity.getPersistentDataContainer().get(wrenVoteKey, PersistentDataType.STRING);
        String name = entity.getPersistentDataContainer().get(nameVoteKey, PersistentDataType.STRING);
        Integer sector = entity.getPersistentDataContainer().get(sectorKey, PersistentDataType.INTEGER);
        try {
            if (wren != null) {
                event.setCancelled(true);
                report(player, ballots.cast(VisibleBallotRite.VoteNode.WR05,
                        player.getUniqueId(), wren).status().name());
            } else if (name != null) {
                event.setCancelled(true);
                report(player, ballots.cast(VisibleBallotRite.VoteNode.RP03,
                        player.getUniqueId(), name).status().name());
            } else if (sector != null) {
                event.setCancelled(true);
                report(player, presence.confirmOwnSector(player.getUniqueId(), sector)
                        .status().name());
            }
        } catch (IOException | RuntimeException failure) {
            failClosed(player, "ritual input", failure);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.LECTERN
                || !(event.getClickedBlock().getState() instanceof Lectern lectern)) {
            return;
        }
        ItemStack book = lectern.getInventory().getItem(0);
        ItemMeta meta = book == null ? null : book.getItemMeta();
        String id = meta == null ? null : meta.getPersistentDataContainer().get(
                bookKey, PersistentDataType.STRING);
        if (!"release_protocol".equals(id)) {
            return;
        }
        try {
            ballots.markConsequenceBookRead(event.getPlayer().getUniqueId());
            event.getPlayer().sendActionBar(Component.text(
                    "consequence text read receipt recorded", NamedTextColor.GRAY));
        } catch (IOException | RuntimeException failure) {
            failClosed(event.getPlayer(), "RP03 consequence receipt", failure);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        OptionalInt sector = sectors.occupiedLitSector(event.getPlayer());
        try {
            if (sector.isPresent()) {
                presence.updatePresence(event.getPlayer().getUniqueId(), sector.getAsInt(), true);
            } else {
                presence.leaveSector(event.getPlayer().getUniqueId());
            }
        } catch (IOException | RuntimeException failure) {
            failClosed(event.getPlayer(), "RP04 presence", failure);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ballots.disconnected(VisibleBallotRite.VoteNode.WR05, event.getPlayer().getUniqueId());
        ballots.disconnected(VisibleBallotRite.VoteNode.RP03, event.getPlayer().getUniqueId());
        presence.disconnected(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ballots.reconnected(VisibleBallotRite.VoteNode.WR05, event.getPlayer().getUniqueId());
        ballots.reconnected(VisibleBallotRite.VoteNode.RP03, event.getPlayer().getUniqueId());
        presence.reconnected(event.getPlayer().getUniqueId());
    }

    /** Integration scheduler should call once per tick; errors keep the relevant rite red. */
    public void tick() {
        try {
            ballots.tick(VisibleBallotRite.VoteNode.WR05);
            ballots.tick(VisibleBallotRite.VoteNode.RP03);
            presence.tick();
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("V5 ritual tick failed closed: " + failure.getMessage());
        }
    }

    private static void report(Player player, String status) {
        player.sendActionBar(Component.text(status.toLowerCase(), NamedTextColor.GRAY));
    }

    private void failClosed(Player player, String context, Exception failure) {
        plugin.getLogger().severe(context + " failed closed: " + failure.getMessage());
        player.sendMessage(Component.text("The local record did not commit; try again after repair.",
                NamedTextColor.RED));
    }
}
