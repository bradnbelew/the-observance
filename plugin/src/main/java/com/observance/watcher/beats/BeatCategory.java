package com.observance.watcher.beats;

/**
 * How a beat counts against the drama budget (FLOW §2 event rhythm).
 *
 * <ul>
 *   <li>{@link #AMBIENT} — cheap, deniable, occasional. Spaced by the ambient cooldowns and the
 *       rolling window cap (≈ ≤1 per hour of play, ≥12 min global spacing).</li>
 *   <li>{@link #PERSONALIZED} — a big "it knows ME" beat. Hard-capped per session per player with a
 *       ≥20-minute floor cooldown. Reserved for strong signals.</li>
 *   <li>{@link #DIRECTED} — explicitly queued by the bot/dashboard (a {@code whisper_toll}, an
 *       {@code unlock} step, a report excerpt). Authored intent; bypasses the AMBIENT spacing gates
 *       but still respects {@code watcher_sleep} + per-beat cooldowns. The showrunner already paced
 *       it; the engine must not silently swallow an approved directed beat.</li>
 * </ul>
 */
public enum BeatCategory {
    AMBIENT,
    PERSONALIZED,
    DIRECTED
}
