package com.observance.watcher.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Thin, intention-revealing wrapper over the Bukkit scheduler.
 *
 * <p>THREADING CONTRACT (non-negotiable):
 * <ul>
 *   <li>{@code runMain*} — touch Bukkit/world objects ONLY here (main tick).</li>
 *   <li>{@code runAsync*} — network / Supabase / blocking I/O ONLY here. NEVER touch
 *       a Bukkit world object inside an async body.</li>
 * </ul>
 *
 * <p>Every task body handed to this class is responsibility of the caller to wrap in
 * {@link Safety}; convenience {@code *Safe} variants do the wrapping for you. Prefer the
 * {@code *Safe} variants for anything attached to a repeating timer or an event follow-up.
 */
public final class Scheduler {

    private final Plugin plugin;
    private final Safety safety;

    public Scheduler(Plugin plugin, Safety safety) {
        this.plugin = plugin;
        this.safety = safety;
    }

    /* ------------------------------------------------------------------ */
    /* Main-thread (world-mutating) execution                              */
    /* ------------------------------------------------------------------ */

    /** Run on the main thread as soon as possible. If already on main, runs after current tick. */
    public BukkitTask runMain(Runnable body) {
        return Bukkit.getScheduler().runTask(plugin, body);
    }

    /** Run on the main thread, body wrapped in Safety (logs+swallows, never propagates). */
    public BukkitTask runMainSafe(String context, Runnable body) {
        return Bukkit.getScheduler().runTask(plugin, () -> safety.run(context, body));
    }

    /** Run on the main thread after {@code delayTicks} ticks (20 ticks = 1s). */
    public BukkitTask runLater(long delayTicks, Runnable body) {
        return Bukkit.getScheduler().runTaskLater(plugin, body, Math.max(0L, delayTicks));
    }

    /** Run on the main thread after a delay, body wrapped in Safety. */
    public BukkitTask runLaterSafe(String context, long delayTicks, Runnable body) {
        return Bukkit.getScheduler().runTaskLater(plugin,
                () -> safety.run(context, body), Math.max(0L, delayTicks));
    }

    /** Repeating main-thread timer, body wrapped in Safety. Returns the task for cancellation. */
    public BukkitTask runTimerSafe(String context, long delayTicks, long periodTicks, Runnable body) {
        return Bukkit.getScheduler().runTaskTimer(plugin,
                () -> safety.run(context, body),
                Math.max(0L, delayTicks), Math.max(1L, periodTicks));
    }

    /* ------------------------------------------------------------------ */
    /* Async (network / I/O) execution                                     */
    /* ------------------------------------------------------------------ */

    /** Run off the main thread ASAP. Use ONLY for I/O; never touch Bukkit world objects. */
    public BukkitTask runAsync(Runnable body) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, body);
    }

    /** Run off the main thread, body wrapped in Safety. */
    public BukkitTask runAsyncSafe(String context, Runnable body) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> safety.run(context, body));
    }

    /** Run off the main thread after a delay, body wrapped in Safety. */
    public BukkitTask runAsyncLaterSafe(String context, long delayTicks, Runnable body) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> safety.run(context, body), Math.max(0L, delayTicks));
    }

    /** Repeating async timer (e.g. the beat-queue poller), body wrapped in Safety. */
    public BukkitTask runAsyncTimerSafe(String context, long delayTicks, long periodTicks, Runnable body) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                () -> safety.run(context, body),
                Math.max(0L, delayTicks), Math.max(1L, periodTicks));
    }

    /* ------------------------------------------------------------------ */
    /* Bridges                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Run blocking work async, then hop the result back to the main thread.
     * The async supplier MUST NOT touch Bukkit objects; the main consumer is where
     * world mutation happens. Both halves are Safety-wrapped.
     */
    public <T> void asyncThenMain(String context, Supplier<T> asyncWork, java.util.function.Consumer<T> mainConsumer) {
        runAsyncSafe(context + ".async", () -> {
            T result = asyncWork.get();
            runMainSafe(context + ".main", () -> mainConsumer.accept(result));
        });
    }

    /** Convenience: run a supplier on an async thread and complete a future with the result. */
    public <T> CompletableFuture<T> supplyAsync(String context, Supplier<T> asyncWork) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsyncSafe(context, () -> {
            try {
                future.complete(asyncWork.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /** True if the calling thread is the server main thread. */
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    /** Cancel a task quietly (null-safe). */
    public static void cancel(BukkitTask task) {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // never propagate
            }
        }
    }

    /** Cancel a BukkitRunnable quietly (null-safe). */
    public static void cancel(BukkitRunnable runnable) {
        if (runnable != null) {
            try {
                runnable.cancel();
            } catch (Throwable ignored) {
                // never propagate
            }
        }
    }
}
