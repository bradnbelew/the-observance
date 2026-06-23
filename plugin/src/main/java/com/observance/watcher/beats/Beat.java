package com.observance.watcher.beats;

/**
 * One named, reusable interaction in the beat library (the interaction palette: text /
 * clue-discovery / items / world / mobs / sensory / ack).
 *
 * <h2>Contract every beat MUST honor (the anti-jank contract, DESIGN §3)</h2>
 * <ul>
 *   <li><b>Main thread:</b> {@link #enact} runs on the MAIN tick (world mutation). The engine
 *       guarantees this. Beats may schedule async follow-ups for I/O via {@code ctx.scheduler()}.</li>
 *   <li><b>Never throws:</b> a beat returns a {@link BeatResult}; the engine also wraps it in
 *       Safety as defense in depth. Validate/normalize/null-check ALL inputs.</li>
 *   <li><b>Placement validation:</b> world beats use {@code Placement} (support/surface/context
 *       gating) — no floaters in rock/air, douse torches only where torches exist.</li>
 *   <li><b>Reveal discipline:</b> block-mutating beats use {@code ctx.reveal()} and only mutate
 *       outside any player's line of sight (or self-retry later via the engine).</li>
 *   <li><b>Per-player targeting:</b> sensory beats use {@code PerPlayer} so they land privately.</li>
 *   <li><b>Idempotent + reversible:</b> ephemeral effects revert; nothing griefs the world or
 *       destroys hard-won progress; protected fixtures resist being broken.</li>
 *   <li><b>Lore-agnostic:</b> ALL text comes from {@code req.payload()} (Supabase/config), never
 *       hardcoded.</li>
 * </ul>
 */
public interface Beat {

    /** Stable library key (lower-snake), e.g. {@code "lectern_fill"}. Matches {@code beat_queue.type}. */
    String name();

    /** One-line human description for the catalog / dashboard. No story text. */
    String description();

    /** How this beat counts against the drama budget. */
    BeatCategory category();

    /**
     * Cheap, side-effect-free precheck (MAIN thread): can this beat plausibly run right now for this
     * request? E.g. requires an online target, requires a placed site, requires a torch nearby.
     * Returning false → the engine skips it WITHOUT consuming budget. Must never throw.
     */
    boolean canEnact(BeatContext ctx, BeatRequest req);

    /**
     * Realize the beat in-world (MAIN thread). Must be idempotent w.r.t. {@code req.beatId()} where
     * the effect is persistent, validate placement + reveal discipline, and never throw.
     *
     * @return FIRED / SKIPPED / FAILED with a short reason.
     */
    BeatResult enact(BeatContext ctx, BeatRequest req);
}
