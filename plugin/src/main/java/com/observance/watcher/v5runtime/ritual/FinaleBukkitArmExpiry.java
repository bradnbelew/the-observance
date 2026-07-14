package com.observance.watcher.v5runtime.ritual;

import java.io.IOException;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Schedules the persisted ARMED cutoff; expiry can never commit or start theater. */
public final class FinaleBukkitArmExpiry {
    private final Plugin plugin;
    private final FinaleRite finale;
    private BukkitTask task;

    public FinaleBukkitArmExpiry(Plugin plugin, FinaleRite finale) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.finale = Objects.requireNonNull(finale, "finale");
    }

    public synchronized void scheduleFromDurableState() {
        cancelTask();
        FinaleStateStore.Snapshot state = finale.snapshot();
        if (state.phase() != FinaleStateStore.Phase.ARMED) {
            return;
        }
        long remainingMillis = Math.max(0L, state.cancelCutoffAt() - System.currentTimeMillis());
        long delayTicks = Math.max(1L, (remainingMillis + 49L) / 50L);
        task = plugin.getServer().getScheduler().runTaskLater(plugin, this::expire, delayTicks);
    }

    public synchronized void stop() {
        cancelTask();
    }

    private void expire() {
        synchronized (this) {
            task = null;
        }
        try {
            FinaleRite.ArmResult result = finale.expireIfNeeded();
            if (result.status() == FinaleRite.ArmStatus.EXPIRED) {
                plugin.getLogger().info("RP05 arm expired fail-closed to IDLE");
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("RP05 arm expiry could not persist: " + failure.getMessage());
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
