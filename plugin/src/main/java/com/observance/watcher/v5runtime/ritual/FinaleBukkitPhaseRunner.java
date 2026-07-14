package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ritual.FinaleStateStore.Phase;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Single-runner Bukkit scheduler for the durable RP06 phases. */
public final class FinaleBukkitPhaseRunner {
    private final Plugin plugin;
    private final FinaleRite finale;
    private final FinaleRite.FinaleEffects effects;
    private BukkitTask pending;
    private boolean running;

    public FinaleBukkitPhaseRunner(
            Plugin plugin, FinaleRite finale, FinaleRite.FinaleEffects effects) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.finale = Objects.requireNonNull(finale, "finale");
        this.effects = Objects.requireNonNull(effects, "effects");
    }

    public synchronized boolean startOrResume() {
        Phase phase = finale.snapshot().phase();
        if (phase == Phase.IDLE || phase == Phase.ARMED || phase == Phase.FAULT
                || phase == Phase.CODA || running) {
            return false;
        }
        running = true;
        schedule(0L);
        return true;
    }

    public synchronized void stop() {
        running = false;
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
    }

    private synchronized void schedule(long delayTicks) {
        pending = plugin.getServer().getScheduler().runTaskLater(plugin, this::advance,
                Math.max(0L, delayTicks));
    }

    private void advance() {
        try {
            Phase phase = finale.resumeOnePhase(effects);
            synchronized (this) {
                pending = null;
                if (phase == Phase.CODA) {
                    running = false;
                    return;
                }
                schedule(finale.recommendedDelayTicksBeforeNextPhase());
            }
        } catch (Exception failure) {
            synchronized (this) {
                pending = null;
                running = false;
            }
            plugin.getLogger().severe("RP06 paused safely at durable phase "
                    + finale.snapshot().phase() + ": " + failure.getMessage());
        }
    }
}
