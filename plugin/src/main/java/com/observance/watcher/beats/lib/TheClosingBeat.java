package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.finale.FinaleStateMachine;

/**
 * Compatibility acknowledgement for legacy {@code the_closing} showrunner rows.
 *
 * <p>V5 deliberately gives this remote beat no world-changing authority. The explicit local command
 * {@code /obs finale arm <condemn|understand|free>} owns commitment, theater, kick, world save,
 * shutdown, and persistent CODA. Thus a delayed, duplicated, or stale queue row cannot end the server
 * without a durable local arm and cannot replay an already committed ending.
 */
public final class TheClosingBeat extends AbstractBeat {

    @Override public String name() { return "the_closing"; }
    @Override public String description() {
        return "Acknowledges a remote closing row; the durable local V5 finale controller owns theater.";
    }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        return true; // consume the remote row cleanly; doEnact applies the local authority gate
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        if (ctx == null || !(ctx.plugin() instanceof com.observance.watcher.ObservancePlugin observance)
                || observance.finaleController() == null) {
            return BeatResult.skipped("local-finale-controller-unavailable");
        }
        FinaleStateMachine.Snapshot snapshot = observance.finaleController().snapshot();
        if (snapshot.phase() == FinaleStateMachine.Phase.IDLE
                || snapshot.phase() == FinaleStateMachine.Phase.FAULT) {
            ctx.safety().warn("closing", "Ignored the_closing queue row: local finale is not explicitly armed.");
            return BeatResult.skipped("local-finale-not-armed");
        }
        ctx.safety().info("closing", "Remote the_closing acknowledged; durable local finale owns theater ("
                + snapshot.phase().name().toLowerCase(java.util.Locale.ROOT) + ").");
        return BeatResult.fired("durable-local-finale-owns-theater");
    }
}
