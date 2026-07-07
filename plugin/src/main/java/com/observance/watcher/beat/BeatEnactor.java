package com.observance.watcher.beat;

import com.observance.watcher.data.rows.BeatQueueRow;

/**
 * Seam for the Haunting-Engine subsystem agent. The foundation's poller pulls actionable beats
 * (async), runs them past the drama budget, and hands each approved beat to an enactor to be
 * realized in-world. The enactor is invoked on the MAIN thread (world mutation), wrapped in
 * Safety by the caller.
 *
 * <p>Startup installs a fallback {@link NoopBeatEnactor} first so the queue poller always has a target.
 * The Haunting Engine then replaces it with the real enactor through {@code ObservancePlugin#setBeatEnactor}.
 */
public interface BeatEnactor {

    /**
     * Enact one beat in the world. MUST be called on the main thread. Implementations must be
     * idempotent w.r.t. the beat id (never double-apply) and must not throw — but the caller also
     * wraps this in Safety as defense in depth.
     *
     * @param beat the beat row (type + payload + optional target)
     * @return the outcome the poller records back to Supabase
     */
    EnactResult enact(BeatQueueRow beat);

    /** Outcome of an enact attempt — maps to the beat_queue status the poller writes. */
    enum EnactResult {
        /** Beat realized in-world. → status "fired". */
        FIRED,
        /** Deliberately not enacted now (e.g. no witness clearance, target offline). → "skipped". */
        SKIPPED,
        /** Tried and failed. → "failed". */
        FAILED,
        /**
         * Type is unknown to this enactor (e.g. a future beat type). The poller LEAVES it in the
         * queue (does not mark decided) so a later build can handle it. No status write.
         */
        UNHANDLED
    }
}
