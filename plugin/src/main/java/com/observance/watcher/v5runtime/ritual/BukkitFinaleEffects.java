package com.observance.watcher.v5runtime.ritual;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Production effect boundary; visual fixture groups remain injected and authority-owned. */
public final class BukkitFinaleEffects implements FinaleRite.FinaleEffects {
    public interface TheaterVisuals {
        void darkenDocumentedFixtureGroups(String idempotencyKey) throws Exception;

        void emitRecordSyntaxBreak(String idempotencyKey) throws Exception;
    }

    private final Plugin plugin;
    private final TheaterVisuals visuals;
    private final boolean productionShutdownEnabled;
    private final Runnable beforeSave;

    public BukkitFinaleEffects(
            Plugin plugin, TheaterVisuals visuals, boolean productionShutdownEnabled) {
        this(plugin, visuals, productionShutdownEnabled, () -> { });
    }

    public BukkitFinaleEffects(
            Plugin plugin,
            TheaterVisuals visuals,
            boolean productionShutdownEnabled,
            Runnable beforeSave) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.productionShutdownEnabled = productionShutdownEnabled;
        this.beforeSave = Objects.requireNonNull(beforeSave, "beforeSave");
    }

    @Override
    public void darken(String idempotencyKey) throws Exception {
        visuals.darkenDocumentedFixtureGroups(idempotencyKey);
    }

    @Override
    public void syntaxBreak(String idempotencyKey) throws Exception {
        visuals.emitRecordSyntaxBreak(idempotencyKey);
    }

    @Override
    public void goodbye(String idempotencyKey, List<String> exactLines) {
        // Drip the goodbye one line at a time so it reads as someone speaking, not a wall of text
        // dumped at once. The kick (SAVE_AND_CODA) is scheduled far enough after this phase that the
        // drip completes first, and the kick screen still shows the whole message regardless.
        long stepTicks = 44L; // ~2.2s between lines
        for (int index = 0; index < exactLines.size(); index++) {
            String raw = exactLines.get(index);
            if (raw == null) continue;
            final Component line = Component.text(raw, NamedTextColor.WHITE);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(line);
                }
            }, index * stepTicks);
        }
    }

    @Override
    public void savePlayersAndWorlds(String idempotencyKey) {
        // The Coda receipt/gates must be in the world save that precedes the terminal kick.
        beforeSave.run();
        Server server = plugin.getServer();
        for (Player player : server.getOnlinePlayers()) {
            player.saveData();
        }
        for (World world : server.getWorlds()) {
            world.save();
        }
    }

    @Override
    public void kickPlayers(String idempotencyKey, List<String> exactLines) {
        Component message = message(exactLines);
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            player.kick(message);
        }
    }

    @Override
    public void requestProductionShutdown(String idempotencyKey) {
        if (!productionShutdownEnabled) {
            throw new IllegalStateException(
                    "production shutdown is disabled; RP06 remains resumable and incomplete");
        }
        plugin.getLogger().warning("RP06 requested production shutdown after durable Coda commit");
        plugin.getServer().shutdown();
    }

    private static Component message(List<String> lines) {
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(Component.text(lines.get(index), NamedTextColor.WHITE));
        }
        return result;
    }
}
