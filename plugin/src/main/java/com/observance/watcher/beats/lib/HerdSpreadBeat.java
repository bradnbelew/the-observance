package com.observance.watcher.beats.lib;

import com.google.gson.JsonObject;
import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;

/**
 * MOBS — the named beat {@code herd_spread}, exactly the type {@code discord/src/showrunner/
 * autonomy.run.ts}'s {@code paceHerd()} glue already enqueues (design/ideas/herd-conversion.md
 * §4.5 item 2). Previously UNREGISTERED here — the enqueue silently went nowhere (unrecognized
 * beat type → the row sits UNHANDLED, no log), which was the reported bug.
 *
 * <p>This is a thin, single-purpose DELEGATOR (the same idiom as {@link UnlockBeat} /
 * {@link GroupBeat}, minus the dynamic step-name lookup those need): it forces
 * {@code mode:"spread"} onto the payload and forwards straight to {@link SacredAnimalBeat}, which
 * owns the actual spread logic (idempotent, capped, unwitnessed, {@code pale_cosmetic}-only —
 * never {@code sacred_beast} / {@code sacred_fork_arm}). Picked over re-registering
 * {@code sacred_animal} itself under a second payload convention because it keeps the TS enqueue
 * call (`enqueueBeat('herd_spread', ...)`) untouched — zero changes needed on the Discord side —
 * while {@code sacred_animal}'s own registration and payload contract (mode-based dispatch) stay
 * exactly as {@link SacredAnimalBeat} defines them. A one-line name mapping, nothing else.
 */
public final class HerdSpreadBeat extends AbstractBeat {

    private final SacredAnimalBeat delegate;

    public HerdSpreadBeat(SacredAnimalBeat delegate) {
        this.delegate = delegate;
    }

    @Override public String name() { return "herd_spread"; }
    @Override public String description() { return "The between-session Pale-herd conversion pass (forces SacredAnimalBeat mode:\"spread\")."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        return delegate.canEnact(ctx, forceSpread(req));
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        return delegate.enact(ctx, forceSpread(req));
    }

    /** Same id/target/site, but the payload with {@code mode} forced to {@code "spread"} — even if
     *  the enqueuer omitted it or (implausibly) sent a different mode, this beat's NAME is the
     *  authority: a row named {@code herd_spread} always means the spread branch, never "single". */
    private static BeatRequest forceSpread(BeatRequest req) {
        JsonObject merged = req.payload().raw().deepCopy();
        merged.addProperty("mode", "spread");
        BeatPayload forced = BeatPayload.of(merged);
        return new BeatRequest(req.beatId(), "sacred_animal", req.category(),
                req.targetPlayer(), req.site(), forced);
    }
}
