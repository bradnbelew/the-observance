package com.observance.watcher.beats.lib;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.signal.SignalSnapshot;
import com.observance.watcher.tier0.Tier0Config;
import com.observance.watcher.tier0.Tier0Observation;
import com.observance.watcher.tier0.Tier0Selector;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OBSERVER TIER-0 delivery — "it knows you" with NO chat/voice/LLM (BUILD-PLAN §13 / CHANGE-MANIFEST
 * A2). An AMBIENT beat that reads ONE player's real {@link SignalSnapshot} from the signal tracker,
 * runs the {@link Tier0Selector} to derive a GROUNDED observation, and speaks a Watcher-register
 * IMPLICATION line to that player alone (a fading title or an action-bar line).
 *
 * <h2>Why this is safe + rare</h2>
 * <ul>
 *   <li><b>Grounded:</b> the line is only spoken when the selector finds an observation demonstrably
 *       TRUE of the player from measured signals; otherwise the beat SKIPs (silence). Never a guess.</li>
 *   <li><b>Behavior-only:</b> reads no chat/voice; the cleanest tier by the design's consent posture.</li>
 *   <li><b>Rare:</b> it is an AMBIENT beat, so it passes the {@link com.observance.watcher.beats.DramaBudget}
 *       (per-player + global ambient cooldowns + the rolling-window cap) like every ambient fire. On top
 *       of that it applies a per-observation per-player cooldown (via the shared RateLimiter) so the same
 *       implication is not repeated to the same player within {@code tier0.per-observation-cooldown-minutes}.
 *       Rare = uncanny; frequent = creepy spam.</li>
 *   <li><b>Fault-isolated + per-player:</b> pure {@code PerPlayer} title/action-bar; never touches the
 *       world or other players; a failure degrades to a skip.</li>
 * </ul>
 *
 * <p>Payload (all optional): {@code {"mode":"title"|"actionbar", "prefix":"…"}}. The line itself is
 * chosen by Tier-0 from config; the payload only tunes presentation.
 */
public final class ComposureBeat extends AbstractBeat {

    /** In-process fallback selector used only if the plugin/tracker can't be reached (keeps canEnact honest). */
    private volatile Tier0Selector fallbackSelector;

    @Override public String name() { return "composure"; }
    @Override public String description() {
        return "Tier-0: speaks a grounded, behavior-only implication ('it knows you') to one player.";
    }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        Player pl = target(req);
        if (pl == null) return false;
        Resolved r = resolve(ctx);
        if (r == null || !r.selector.config().enabled) return false;

        SignalSnapshot snap = r.snapshot(pl.getUniqueId());
        if (snap == null) return false;
        Tier0Observation obs = r.selector.select(snap).orElse(null);
        if (obs == null) return false;                 // nothing clearly true → stay silent
        return !onCooldown(ctx, pl, r.selector.config(), obs);   // don't re-speak the same line too soon
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        Resolved r = resolve(ctx);
        if (r == null || !r.selector.config().enabled) return BeatResult.skipped("tier0-off");

        SignalSnapshot snap = r.snapshot(pl.getUniqueId());
        if (snap == null) return BeatResult.skipped("no-signals");

        Tier0Observation obs = r.selector.select(snap).orElse(null);
        if (obs == null) return BeatResult.skipped("silence");     // grounding: degrade to silence, never guess

        Tier0Config cfg = r.selector.config();
        if (onCooldown(ctx, pl, cfg, obs)) return BeatResult.skipped("cooldown");
        armCooldown(ctx, pl, cfg, obs);

        String line = pickLine(cfg, obs);
        if (line == null || line.isBlank()) return BeatResult.skipped("empty-line");

        BeatPayload p = req.payload();
        String mode = p.string("mode", "title").trim().toLowerCase(Locale.ROOT);
        if (mode.equals("actionbar")) {
            PerPlayer.actionBar(pl, line);
        } else {
            // A soft, empty-ish lead line above the implication (the Watcher does not shout).
            String prefix = p.string("prefix", "");
            PerPlayer.title(pl, line, prefix, 12, 60, 24);
        }

        // Audit out-of-band by observation KEY only (no lore text, no chat) so the dashboard sees Tier-0
        // is alive without a story leak. Capture the name on main.
        final String focusName = pl.getName();
        final String obsKey = obs.key();
        ctx.scheduler().runAsyncSafe("beat.composure.audit",
                () -> ctx.safety().info("beat.composure", "tier0 target=" + focusName + " obs=" + obsKey));

        return BeatResult.fired("tier0-" + obs.key());
    }

    /* ------------------------------------------------------------------ */
    /*  Resolution + cooldown                                             */
    /* ------------------------------------------------------------------ */

    /** The selector + a snapshot source, resolved from the live plugin (preferred) or a fallback. */
    private Resolved resolve(BeatContext ctx) {
        try {
            if (ctx.plugin() instanceof ObservancePlugin op && op.signalTracker() != null) {
                Tier0Selector sel = op.tier0Selector();
                if (sel != null) {
                    return new Resolved(sel, op.signalTracker()::snapshot);
                }
            }
        } catch (Throwable ignored) { }
        // Fallback: default-config selector + no snapshot source (canEnact will find null → silence).
        Tier0Selector sel = fallbackSelector;
        if (sel == null) {
            sel = new Tier0Selector(Tier0Config.defaults());
            fallbackSelector = sel;
        }
        return new Resolved(sel, uuid -> null);
    }

    private static boolean onCooldown(BeatContext ctx, Player pl, Tier0Config cfg, Tier0Observation obs) {
        long ms = (long) cfg.perObservationCooldownMinutes * 60_000L;
        if (ms <= 0) return false;
        return ctx.rateLimiter().remainingMs(cooldownKey(pl, obs)) > 0;
    }

    private static void armCooldown(BeatContext ctx, Player pl, Tier0Config cfg, Tier0Observation obs) {
        long ms = (long) cfg.perObservationCooldownMinutes * 60_000L;
        if (ms > 0) ctx.rateLimiter().tryCooldown(cooldownKey(pl, obs), ms);
    }

    private static String cooldownKey(Player pl, Tier0Observation obs) {
        return "tier0:" + pl.getUniqueId() + ":" + obs.key();
    }

    /** Pick one line for the observation (random among authored candidates; else the enum default). */
    private static String pickLine(Tier0Config cfg, Tier0Observation obs) {
        List<String> lines = cfg.linesFor(obs);
        if (lines.isEmpty()) return obs.defaultLine();
        if (lines.size() == 1) return lines.get(0);
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    /** A resolved selector plus a snapshot-by-uuid source. */
    private static final class Resolved {
        final Tier0Selector selector;
        final java.util.function.Function<java.util.UUID, SignalSnapshot> source;
        Resolved(Tier0Selector selector, java.util.function.Function<java.util.UUID, SignalSnapshot> source) {
            this.selector = selector;
            this.source = source;
        }
        SignalSnapshot snapshot(java.util.UUID uuid) {
            try { return source.apply(uuid); } catch (Throwable t) { return null; }
        }
    }
}
