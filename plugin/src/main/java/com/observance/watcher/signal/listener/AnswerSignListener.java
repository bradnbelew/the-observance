package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.function.Supplier;

/**
 * THE IN-WORLD ANSWER VERB. A player submits a clue answer by editing an "answer sign" at a
 * configured keeper-stone site (sites.yml type {@code answer_sign} or {@code keeper_stone}). This is
 * the world-surface twin of the bot's Discord {@code #the-record} scan — both feed the SAME shared
 * {@link OracleResolver} against the SAME {@code puzzles} table, so the loop closes on either surface.
 *
 * <p>Mirrors {@link CustomComplianceListener} exactly: Safety-wrapped body, all Bukkit reads on the
 * MAIN thread, a {@link RateLimiter} guard, MONITOR priority, sites resolved live via a
 * {@link Supplier} so a reload is picked up. The network/resolve work is hopped ASYNC.
 *
 * <p><b>Submission semantics.</b> The sign is an input slot, not a billboard: the typed lines are
 * captured on the main thread and the sign is then BLANKED (the player never sees their guess persist,
 * so a wrong answer leaves no trace and a correct answer's "voice" is the in-world reward beat, not
 * sign text). A miss/withheld/duplicate produces NO feedback — silence, never an error or a tell.
 */
public final class AnswerSignListener implements Listener {

    /** Site types whose signs are treated as answer-submission slots. */
    private static final String TYPE_ANSWER_SIGN = "answer_sign";
    private static final String TYPE_KEEPER_STONE = "keeper_stone";

    /** A coarse per-player submit cooldown — defense in depth on top of the resolver's own limiter.
     *  Config-driven (tracker.answer-sign.cooldown-seconds via TrackerConfig#answerSignCooldownMs),
     *  matching the sibling cooldown pattern (dark-hours/kept-light/etc.); 3000ms is the fallback if
     *  a non-positive value somehow reaches the constructor, preserving the old hardcoded behavior. */
    private static final long DEFAULT_SUBMIT_COOLDOWN_MS = 3_000L;

    private final OracleResolver resolver;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final long submitCooldownMs;

    public AnswerSignListener(OracleResolver resolver, Supplier<SitesConfig> sitesSupplier,
                              RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                              long submitCooldownMs) {
        this.resolver = resolver;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.submitCooldownMs = submitCooldownMs > 0 ? submitCooldownMs : DEFAULT_SUBMIT_COOLDOWN_MS;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        safety.run("signal.AnswerSign.change", () -> {
            Player p = event.getPlayer();
            if (p == null) return;

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Block block = event.getBlock();
            if (block == null) return;
            Location loc = block.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            // Is this sign at a configured answer site? (answer_sign first, then keeper_stone.)
            Site site = nearestAnswerSite(sites, world, loc.getX(), loc.getY(), loc.getZ());
            if (site == null) return;   // an ordinary sign — not a submission slot, ignore entirely

            // Read the typed lines on the MAIN thread (event thread) and join them into one answer.
            String raw = joinLines(event.getLines());

            // Blank the sign so the guess never persists (input slot, not billboard). Done on the
            // event itself = main thread, safe. Even an empty/gibberish submission clears cleanly.
            blank(event);

            if (raw.isBlank()) return;  // nothing plausibly an answer — stay silent, don't even hop

            // Coarse anti-spam cooldown per player+site (the resolver also rate-limits durably).
            String cdKey = "answersign:" + p.getUniqueId() + ":" + site.id();
            if (!rateLimiter.tryCooldown(cdKey, submitCooldownMs)) {
                return; // submitting too fast → withhold silently
            }

            // Snapshot the identity off the Bukkit object before going async (never touch p async).
            final String mcUuid = p.getUniqueId().toString();
            final String name = p.getName();
            final String boundKey = site.puzzleKey(); // null = match all open puzzles

            // Hop ASYNC for all the network/resolve work. The resolver never throws (Safety-wrapped)
            // and never touches Bukkit; any in-world reward arrives later as an enqueued beat.
            scheduler.runAsyncSafe("oracle.answersign.resolve", () -> {
                OracleResolver.Result r = resolver.resolveWorld(mcUuid, name, raw, boundKey);
                if (r == OracleResolver.Result.SOLVED) {
                    safety.info("oracle.solved",
                            "world solve by " + name + " at site " + site.id());
                }
                // All other results (MISS/WITHHELD/ALREADY_SOLVED/IGNORED/UNAVAILABLE) → silence.
            });
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    /**
     * Nearest placed answer-submission site containing the point. Prefers {@code answer_sign} sites,
     * then {@code keeper_stone}. Uses {@link Site#contains} (snapshot coords, no Bukkit) so it is
     * cheap and exact.
     */
    private Site nearestAnswerSite(SitesConfig sites, String world, double x, double y, double z) {
        Site best = bestOfType(sites, TYPE_ANSWER_SIGN, world, x, y, z);
        if (best != null) return best;
        return bestOfType(sites, TYPE_KEEPER_STONE, world, x, y, z);
    }

    private Site bestOfType(SitesConfig sites, String type,
                            String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location center = s.location();
            if (center == null) { if (best == null) best = s; continue; }
            double dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }

    /** Join the four sign lines with spaces; normalization happens in the resolver. Null-safe. */
    private static String joinLines(String[] lines) {
        if (lines == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(line);
        }
        return sb.toString();
    }

    /** Clear the typed lines so the submitted guess never persists on the sign. */
    private static void blank(SignChangeEvent event) {
        try {
            for (int i = 0; i < 4; i++) {
                event.setLine(i, "");
            }
        } catch (Throwable ignored) {
            // a quirky sign impl must never crash the listener
        }
    }
}
