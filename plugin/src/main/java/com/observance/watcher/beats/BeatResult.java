package com.observance.watcher.beats;

import com.observance.watcher.beat.BeatEnactor;

/**
 * Outcome of one beat attempt. Mirrors {@link BeatEnactor.EnactResult} but adds a short reason
 * for the audit trail. A beat NEVER throws — it returns one of these (and the engine wraps it in
 * {@link com.observance.watcher.util.Safety} as defense in depth).
 */
public final class BeatResult {

    public enum Kind {
        /** Realized in-world. → status "fired". */
        FIRED,
        /** Deliberately not enacted now (no witness clearance, target offline, no valid site). → "skipped". */
        SKIPPED,
        /** Tried and failed (validation passed but a mutation errored). → "failed". */
        FAILED
    }

    private final Kind kind;
    private final String reason;

    private BeatResult(Kind kind, String reason) {
        this.kind = kind;
        this.reason = reason == null ? "" : reason;
    }

    public static BeatResult fired() { return new BeatResult(Kind.FIRED, "ok"); }
    public static BeatResult fired(String reason) { return new BeatResult(Kind.FIRED, reason); }
    public static BeatResult skipped(String reason) { return new BeatResult(Kind.SKIPPED, reason); }
    public static BeatResult failed(String reason) { return new BeatResult(Kind.FAILED, reason); }

    public Kind kind() { return kind; }
    public String reason() { return reason; }

    /** Map to the enactor's coarse result the poller records back to {@code beat_queue}. */
    public BeatEnactor.EnactResult toEnactResult() {
        switch (kind) {
            case FIRED:   return BeatEnactor.EnactResult.FIRED;
            case SKIPPED: return BeatEnactor.EnactResult.SKIPPED;
            case FAILED:
            default:      return BeatEnactor.EnactResult.FAILED;
        }
    }

    @Override
    public String toString() {
        return kind + "(" + reason + ")";
    }
}
