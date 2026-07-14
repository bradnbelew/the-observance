package com.observance.watcher.v5runtime.ritual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Exact-PDC Bukkit proof collector for RP05; branch values never enter this adapter. */
public final class FinaleBukkitListener implements Listener {
    public interface PlayerEligibility {
        boolean isLinked(Player player);

        boolean atExactConfirmCell(Player player);
    }

    private record Hold(long startedTick) {
    }

    private final Plugin plugin;
    private final FinaleRite finale;
    private final FinaleBukkitPhaseRunner phaseRunner;
    private final PlayerEligibility eligibility;
    private final NamespacedKey controlKey;
    private final Map<UUID, Hold> holds = new LinkedHashMap<>();

    public FinaleBukkitListener(
            Plugin plugin, FinaleRite finale, FinaleBukkitPhaseRunner phaseRunner,
            PlayerEligibility eligibility) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.finale = Objects.requireNonNull(finale, "finale");
        this.phaseRunner = Objects.requireNonNull(phaseRunner, "phaseRunner");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.controlKey = new NamespacedKey(plugin, "v5_finale_control");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public synchronized void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && eligibility.atExactConfirmCell(player)) {
            holds.put(player.getUniqueId(), new Hold(currentTick()));
        } else {
            holds.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public synchronized void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || !eligibility.atExactConfirmCell(player)) {
            holds.remove(player.getUniqueId());
        } else {
            holds.putIfAbsent(player.getUniqueId(), new Hold(currentTick()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public synchronized void onControl(PlayerInteractEntityEvent event) {
        Entity control = event.getRightClicked();
        String value = control.getPersistentDataContainer().get(controlKey, PersistentDataType.STRING);
        if (!"sever_record".equals(value)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Hold hold = holds.get(player.getUniqueId());
        long ticks = hold == null ? 0L : Math.max(0L, currentTick() - hold.startedTick());
        FinaleRite.PlayerCommitProof proof = new FinaleRite.PlayerCommitProof(
                player.getUniqueId(),
                player.isOnline(),
                eligibility.isLinked(player),
                player.getGameMode() == GameMode.SPECTATOR,
                eligibility.atExactConfirmCell(player),
                true,
                player.isSneaking(),
                ticks);
        FinaleRite.ConfirmResult result = finale.confirm(proof);
        player.sendMessage(Component.text(result.detail(),
                result.status() == FinaleRite.ConfirmStatus.COMMITTED
                        ? NamedTextColor.DARK_RED : NamedTextColor.RED));
        if (result.status() == FinaleRite.ConfirmStatus.COMMITTED) {
            holds.clear();
            phaseRunner.startOrResume();
        }
    }

    @EventHandler
    public synchronized void onQuit(PlayerQuitEvent event) {
        holds.remove(event.getPlayer().getUniqueId());
    }

    private long currentTick() {
        return plugin.getServer().getCurrentTick();
    }
}
