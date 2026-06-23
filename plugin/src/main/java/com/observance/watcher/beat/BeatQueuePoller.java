package com.observance.watcher.beat;

import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.SupabaseResult;
import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Periodically pulls actionable beats from {@code beat_queue} and enacts the approved ones.
 *
 * <p>Flow (each tick):
 * <ol>
 *   <li><b>Async:</b> check watcher-sleep (remote setting OR local kill switch); if sleeping, do
 *       nothing. Otherwise fetch up to N actionable beats.</li>
 *   <li><b>Async→Main:</b> for each beat, hop to the main thread and call the {@link BeatEnactor}
 *       (world mutation is main-thread-only), wrapped in Safety.</li>
 *   <li><b>Main→Async:</b> based on the enact result, mark the beat decided in Supabase from an
 *       async task (network is off-main).</li>
 * </ol>
 *
 * <p>Idempotency: an in-flight set keys on beat id so the same row can't be double-enacted within
 * a process even if it's still returned by an overlapping poll. The DB status update is the durable
 * guard across restarts.
 */
public final class BeatQueuePoller {

    private final ObservanceConfig config;
    private final SupabaseClient supabase;
    private final Scheduler scheduler;
    private final Safety safety;
    private final Supplier<BeatEnactor> enactorRef;       // late-bound; subsystem may swap it
    private final Supplier<Boolean> localSleepRef;        // local kill switch (config + /obs sleep)

    /** Beats currently being processed in this JVM (prevents double-fire within a run). */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public BeatQueuePoller(ObservanceConfig config,
                           SupabaseClient supabase,
                           Scheduler scheduler,
                           Safety safety,
                           Supplier<BeatEnactor> enactorRef,
                           Supplier<Boolean> localSleepRef) {
        this.config = config;
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enactorRef = enactorRef;
        this.localSleepRef = localSleepRef;
    }

    /** One poll cycle. Runs on an ASYNC thread (scheduled by the plugin). Never throws. */
    public void pollOnce() {
        // Local kill switch first — cheapest, fully offline.
        Boolean localSleep = safety.call("beat.poller.localSleep", localSleepRef, Boolean.FALSE);
        if (Boolean.TRUE.equals(localSleep)) {
            return;
        }
        if (!supabase.isConfigured()) {
            return; // graceful: nothing to poll, no error at players
        }
        // Opportunistically flush any queued offline writes while we're doing I/O.
        safety.run("beat.poller.flush", supabase::flushOfflineQueue);

        // Remote watcher-sleep — suppress everything when true.
        if (supabase.isWatcherSleeping()) {
            return;
        }

        SupabaseResult<List<BeatQueueRow>> r =
                supabase.fetchActionableBeats(config.beatMaxPerPoll());
        if (!r.ok() || r.value() == null || r.value().isEmpty()) {
            return;
        }

        for (BeatQueueRow beat : r.value()) {
            if (beat == null || beat.id == null || beat.id.isBlank()) continue;
            if (!inFlight.add(beat.id)) {
                continue; // already being handled this run
            }
            // Hop to main for the world mutation, then back to async to record the decision.
            scheduler.runMainSafe("beat.enact", () -> enactOnMain(beat));
        }
    }

    /** MAIN thread: enact a single beat, then schedule the async status write. */
    private void enactOnMain(BeatQueueRow beat) {
        BeatEnactor enactor = enactorRef.get();
        BeatEnactor.EnactResult result;
        if (enactor == null) {
            result = BeatEnactor.EnactResult.UNHANDLED;
        } else {
            // Safety as defense in depth even though enactors promise not to throw.
            result = safety.call("beat.enactor",
                    () -> enactor.enact(beat), BeatEnactor.EnactResult.FAILED);
            if (result == null) result = BeatEnactor.EnactResult.FAILED;
        }

        final BeatEnactor.EnactResult outcome = result;
        // Record the decision off-main. UNHANDLED leaves the row untouched for a future build.
        scheduler.runAsyncSafe("beat.decide", () -> {
            try {
                switch (outcome) {
                    case FIRED -> supabase.markBeatDecided(beat.id, "fired");
                    case SKIPPED -> supabase.markBeatDecided(beat.id, "skipped");
                    case FAILED -> supabase.markBeatDecided(beat.id, "failed");
                    case UNHANDLED -> { /* leave queued; do not write status */ }
                }
            } finally {
                inFlight.remove(beat.id);
            }
        });

        // Lightweight audit trail of what fired (helps the spoiler-free health view).
        if (outcome != BeatEnactor.EnactResult.UNHANDLED) {
            scheduler.runAsyncSafe("beat.audit", () ->
                    safety.info("beat.fired", "beat=" + beat.id + " type=" + beat.type
                            + " outcome=" + outcome));
        }
    }
}
