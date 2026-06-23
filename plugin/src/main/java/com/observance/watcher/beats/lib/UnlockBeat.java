package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.Beat;
import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatLibrary;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;

/**
 * DIRECTED — the {@code unlock} step the bot/dashboard enqueues when a puzzle is solved (FLOW §1,
 * cross-surface handoff). It enacts "the next step": typically a world change the group earned —
 * a door opening, a structure appearing, a clue revealing, an advancement granted. Implemented as a
 * DISPATCHER over the rest of the library: the payload names a {@code "step"} (another beat type)
 * and carries that beat's own payload, so unlocks compose the existing palette with no new code.
 *
 * <p>Payload:
 * <pre>{@code
 * { "step":"door_open", "step_payload": { "radius":3, "open":true } }
 * }</pre>
 * If {@code step_payload} is omitted the top-level payload (minus "step") is reused.
 */
public final class UnlockBeat extends AbstractBeat {

    private final BeatLibrary library;

    public UnlockBeat(BeatLibrary library) {
        this.library = library;
    }

    @Override public String name() { return "unlock"; }
    @Override public String description() { return "Enacts an earned next-step (delegates to another beat type)."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Beat delegate = resolveDelegate(req);
        if (delegate == null) return false;
        BeatRequest sub = subRequest(req, delegate);
        return ctx.safety().call("beat.unlock.canEnact",
                () -> delegate.canEnact(ctx, sub), Boolean.FALSE);
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Beat delegate = resolveDelegate(req);
        if (delegate == null) return BeatResult.skipped("unknown-step");
        if (delegate == this || "unlock".equals(delegate.name())) {
            return BeatResult.skipped("no-self-delegate");   // guard against recursion
        }
        BeatRequest sub = subRequest(req, delegate);
        BeatResult r = ctx.safety().call("beat.unlock.enact",
                () -> delegate.enact(ctx, sub), BeatResult.failed("delegate-threw"));
        return r == null ? BeatResult.failed("null-delegate-result") : r;
    }

    private Beat resolveDelegate(BeatRequest req) {
        if (library == null) return null;
        String step = req.payload().string("step", null);
        if (step == null || step.isBlank()) return null;
        return library.get(step);
    }

    /** Build the sub-request: same id/target/site, but the delegate's payload. */
    private static BeatRequest subRequest(BeatRequest req, Beat delegate) {
        BeatPayload sp = req.payload().object("step_payload");
        // If no explicit step_payload, fall back to the whole payload (delegate ignores "step").
        BeatPayload effective = sp.raw().size() > 0 ? sp : req.payload();
        return new BeatRequest(req.beatId(), delegate.name(), delegate.category(),
                req.targetPlayer(), req.site(), effective);
    }
}
