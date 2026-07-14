package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ritual.RitualChoices.ClosingChoice;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenTopic;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Unwired exact-PDC Bukkit adapter for the durable WR03 conversation. */
public final class WrenDialogueBukkitListener implements Listener {
    public static final String NPC_KEY = "v5_npc_id";
    public static final String WREN_VALUE = "wren";
    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private final Plugin plugin;
    private final WrenDialogueRite rite;
    private final CanonicalRitualText text;
    private final NamespacedKey npcKey;

    public WrenDialogueBukkitListener(
            Plugin plugin, WrenDialogueRite rite, CanonicalRitualText text) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.rite = Objects.requireNonNull(rite, "rite");
        this.text = Objects.requireNonNull(text, "text");
        this.npcKey = new NamespacedKey(plugin, NPC_KEY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (!isExactWren(entity) || !sameWorldWithinRange(player, entity)) {
            return;
        }
        event.setCancelled(true);
        try {
            WrenDialogueRite.BeginResult result;
            if (rite.missingTopics(player.getUniqueId()).isEmpty()) {
                if (!player.isSneaking()) {
                    player.sendMessage(Component.text(
                            "Crouch and speak to Wren to choose: YOU CHOSE TO SEND THEM.",
                            NamedTextColor.GRAY));
                    return;
                }
                result = rite.beginClosing(
                        player.getUniqueId(), ClosingChoice.YOU_CHOSE_TO_SEND_THEM);
            } else {
                WrenTopic next = rite.missingTopics(player.getUniqueId()).stream()
                        .min(Comparator.comparingInt(Enum::ordinal)).orElseThrow();
                result = rite.beginTopic(player.getUniqueId(), next);
            }
            if (result.turn().isPresent()) {
                deliver(player, result.turn().orElseThrow());
            } else {
                player.sendMessage(Component.text(result.status().name().toLowerCase(),
                        NamedTextColor.DARK_GRAY));
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("WR03 dialogue failed closed: " + failure.getMessage());
            player.sendMessage(Component.text(
                    "The reply did not persist. Speak again after the record is repaired.",
                    NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rite.disconnect(event.getPlayer().getUniqueId());
    }

    private void deliver(Player player, WrenDialogueRite.DialogueTurn turn) {
        UUID playerId = player.getUniqueId();
        for (int index = 0; index < turn.canonicalLines().size(); index++) {
            String line = turn.canonicalLines().get(index);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Player live = plugin.getServer().getPlayer(playerId);
                if (live != null && live.isOnline()) {
                    live.sendMessage(Component.text(text.wrenDisplayName(), NamedTextColor.YELLOW));
                    live.sendMessage(Component.text(line, NamedTextColor.WHITE));
                }
            }, (long) index * 20L);
        }
        long completionDelay = (long) turn.canonicalLines().size() * 20L;
        plugin.getServer().getScheduler().runTaskLater(
                plugin, () -> completeWhenReady(playerId, turn.id()), completionDelay);
    }

    private void completeWhenReady(UUID playerId, UUID turnId) {
        Player live = plugin.getServer().getPlayer(playerId);
        if (live == null || !live.isOnline()) {
            rite.disconnect(playerId);
            return;
        }
        try {
            WrenDialogueRite.CompleteStatus status = rite.completeReply(playerId, turnId);
            if (status == WrenDialogueRite.CompleteStatus.TOO_EARLY) {
                plugin.getServer().getScheduler().runTaskLater(
                        plugin, () -> completeWhenReady(playerId, turnId), 1L);
            } else if (status == WrenDialogueRite.CompleteStatus.COMPLETED_WR03) {
                live.sendMessage(Component.text("Wren's admission is now in the local record.",
                        NamedTextColor.GOLD));
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("WR03 reply commit failed closed: " + failure.getMessage());
        }
    }

    private boolean isExactWren(Entity entity) {
        return WREN_VALUE.equals(entity.getPersistentDataContainer().get(
                npcKey, PersistentDataType.STRING));
    }

    private static boolean sameWorldWithinRange(Player player, Entity entity) {
        return player.getWorld().equals(entity.getWorld())
                && player.getLocation().distanceSquared(entity.getLocation()) <= MAX_DISTANCE_SQUARED;
    }
}
