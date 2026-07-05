package com.observance.watcher.signal.listener;

import com.observance.watcher.beats.Beat;
import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatEngine;
import com.observance.watcher.beats.BeatLibrary;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.beats.DramaBudget;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.lens.LensItem;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sella — {@code sella-reflection-bearing} (design/PUZZLE-DESIGNS.md §4.1). THE TRIGGER SIDE of the
 * {@link com.observance.watcher.beats.lib.ReflectionBeat "reflection"} beat, which existed fully built
 * and self-tested but was never fired by anything (bug found in the 2026-07-05 cohesion pass — see the
 * beat's own javadoc). This listener is the missing "site-proximity → personalized beat, fired directly
 * through the beat engine, no Discord round-trip" producer that {@code ShoreMemorialListener} (Sella's
 * OWN neighbor puzzle) models for exactly this kind of purely in-world, environmental clue.
 *
 * <p><b>Why direct-fire, not the oracle.</b> {@code ShoreMemorialListener} and its siblings detect an
 * act and post an OPAQUE TOKEN to the shared oracle so the Discord side can resolve a typed/behavior
 * answer. The reflection bearing is different: it has no "detect the solve" moment — it is the DELIVERY
 * of the clue itself (the rune a player reads off the water), which the group then carries to the
 * `dest-far-water` site by hand (an on-site read, not a cipher; PUZZLES-DESIGNS.md §4.1 / clue-specs.ts).
 * There is nothing for the oracle to resolve here; the beat only needs to be shown. So this listener
 * builds a {@link BeatRequest} the same way {@link com.observance.watcher.beats.AmbientBeatGenerator}
 * does for ambient beats, and calls {@code beat.enact(ctx, req)} directly — reserving/refunding the
 * {@link DramaBudget} for the beat's own {@link BeatCategory#PERSONALIZED} category exactly as the real
 * enactor would, so the reflection still respects the same per-player pacing as any other personalized
 * "it knows me" beat.
 *
 * <p><b>Detection.</b> A heavily-guarded {@link PlayerMoveEvent}, mirroring {@code ShoreMemorialListener}:
 * cheap early-outs before a site lookup, a per-player-per-site cooldown collapses the move-event stream
 * at the pool into one measured consideration per window (the beat is naturally re-discoverable — its
 * own hard despawn timer ends each showing, not this listener — so a cooldown, not a one-shot flag, is
 * the correct gate; PUZZLE-DESIGNS.md §4.1 / ReflectionBeat's own "can be re-found" behavior). The pitch
 * threshold matches {@code ReflectionBeat}'s own default {@code pitch_min} so "looking down enough to
 * matter" is judged the same way on both the trigger and delivery sides.
 *
 * <p><b>Payload.</b> The bearing rune text is the AUTHORED line from PUZZLE-DESIGNS.md §4.1 (reused
 * verbatim, not invented here): "south, by the far water, where the reeds fold back." {@code lens_gated}
 * is explicitly {@code false} on this base fragment — the correct default per {@code ReflectionBeat}'s
 * own javadoc, so Sella's base puzzle never requires the Lens (that would be a difficulty/accessibility
 * regression).
 *
 * <p><b>The Lens's bonus use (INTEGRATION §SIGNATURE #3, bug fix).</b> The Lens item/registry/listener
 * were fully built and self-tested but had zero reachable purpose in a live game — nothing anywhere ever
 * set {@code lens_gated:true}. When the SAME player is ALSO holding the Lens while looking in, this
 * listener fires a SECOND, distinct {@code reflection} instance at the same pool with
 * {@code lens_gated:true}: a mirror-wrong fragment reusing the copybook's own authored "seven"/"nevees"
 * motif (see {@code arc/corpus/npc-and-watcher-voice.md} BN17, {@code reflection.lensBonus.sella} — not
 * invented here). This is flavor/foreshadow only — never a required step of {@code sella-reflection-
 * bearing}, whose answer stays the destination word found at the far water.
 *
 * <p>Idempotent-enough (budget + cooldown gate re-firing), fault-isolated (Safety), reveal-safe (never
 * cancels, mutates, or messages — the beat itself owns all in-world effects). Sites resolved live via a
 * {@link Supplier} so a reload / late {@code site set} is picked up without re-registering.
 */
public final class SellaReflectionListener implements Listener {

    private static final String POOL_TYPE = "sella_pool";
    /** One consideration per player per pool per window (collapses the move-event stream at the pool). */
    private static final long COOLDOWN_MS = 15_000L;

    /** The copybook's own "seven"/"nevees" motif — reused verbatim, never invented here (BN17). */
    private static final String DEFAULT_BONUS_TEXT =
            "she counted seven here. the water kept its own count and gave back nevees. "
            + "a child could not tell the true seven from the given-back one. "
            + "count again, and mind which one you are holding.";

    private final Supplier<SitesConfig> sitesSupplier;
    private final Supplier<BeatEngine> beatEngineSupplier;
    private final RateLimiter rateLimiter;
    private final Safety safety;
    private final String namespace;

    private final boolean enabled;
    private final String bearingText;
    private final String bonusText;
    private final double pitchMin;

    public SellaReflectionListener(Supplier<SitesConfig> sitesSupplier,
                                    Supplier<BeatEngine> beatEngineSupplier,
                                    RateLimiter rateLimiter, Safety safety, String namespace,
                                    boolean enabled, String bearingText, String bonusText,
                                    double pitchMin) {
        this.sitesSupplier = sitesSupplier;
        this.beatEngineSupplier = beatEngineSupplier;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.enabled = enabled;
        this.bearingText = (bearingText == null || bearingText.isBlank())
                ? "south, by the far water, where the reeds fold back" : bearingText.trim();
        this.bonusText = (bonusText == null || bonusText.isBlank())
                ? DEFAULT_BONUS_TEXT : bonusText.trim();
        this.pitchMin = Math.max(10.0, Math.min(80.0, pitchMin));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Cheapest possible early-outs BEFORE entering Safety (this event is very hot).
        if (!enabled) return;
        Location to = event.getTo();
        if (to == null) return;
        if (to.getPitch() < pitchMin) return;               // not looking down enough to matter

        safety.run("sella.reflection.move", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            BeatEngine engine = beatEngineSupplier == null ? null : beatEngineSupplier.get();
            if (engine == null || !engine.isActive()) return;

            if (to.getWorld() == null) return;
            String world = to.getWorld().getName();

            Site pool = nearestPlacedOfType(sites, world, to.getX(), to.getY(), to.getZ());
            if (pool == null) return;                        // not standing at Sella's shore pool

            // Collapse the move-event stream at the pool into one measured consideration per window.
            if (!rateLimiter.tryCooldown("sella_reflection:" + p.getUniqueId() + ":" + pool.id(),
                    COOLDOWN_MS)) return;

            // The base bearing — Sella's standalone clue, never Lens-gated (BUG A fix).
            fireReflection(engine, p, pool, bearingText, false, "sella.reflection");

            // The Lens's bonus use (BUG B fix): a SECOND, distinct fragment, ONLY while the same
            // player is ALSO holding the relic right now. Independently rate-limited so holding the
            // Lens never doubles the base fragment's own pacing.
            if (isHoldingLens(p) && rateLimiter.tryCooldown(
                    "sella_reflection_bonus:" + p.getUniqueId() + ":" + pool.id(), COOLDOWN_MS)) {
                fireReflection(engine, p, pool, bonusText, true, "sella.reflection.bonus");
            }
        });
    }

    /* ----------------------------- firing ------------------------------ */

    /** Build the request + fire "reflection" directly through the beat engine, budget-gated. MAIN thread. */
    private void fireReflection(BeatEngine engine, Player player, Site site,
                                 String text, boolean lensGated, String auditContext) {
        BeatContext ctx = engine.context();
        BeatLibrary library = engine.library();
        DramaBudget budget = engine.budget();
        if (ctx == null || library == null || budget == null) return;

        Beat beat = library.get("reflection");
        if (beat == null) return;   // unbuilt in this jar — degrade quietly

        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("text", text);
        obj.addProperty("lens_gated", lensGated);
        BeatPayload payload = BeatPayload.of(obj);

        BeatRequest req = new BeatRequest(
                "sella-reflection-" + UUID.randomUUID(), "reflection",
                BeatCategory.PERSONALIZED, player, site, payload);

        boolean can = ctx.safety().call(auditContext + ".canEnact",
                () -> beat.canEnact(ctx, req), Boolean.FALSE);
        if (!Boolean.TRUE.equals(can)) return;

        UUID target = player.getUniqueId();
        if (!budget.tryReserve(BeatCategory.PERSONALIZED, target)) return;   // pacing says not now

        BeatResult result = ctx.safety().call(auditContext + ".enact",
                () -> beat.enact(ctx, req), BeatResult.failed("threw"));
        if (result == null) result = BeatResult.failed("null");

        if (result.kind() != BeatResult.Kind.FIRED) {
            budget.refund(BeatCategory.PERSONALIZED, target);
        } else {
            final String name = player.getName();
            final String ctxLabel = auditContext;
            ctx.scheduler().runAsyncSafe(auditContext + ".audit", () ->
                    safety.info(ctxLabel, name + " looked into the shore pool — fired reflection"));
        }
    }

    /** True iff the player currently holds the Lens in either hand. Never throws. */
    private boolean isHoldingLens(Player player) {
        try {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (LensItem.isLens(main, namespace)) return true;
            ItemStack off = player.getInventory().getItemInOffHand();
            return LensItem.isLens(off, namespace);
        } catch (Throwable t) {
            return false;
        }
    }

    /* ----------------------------- helpers ---------------------------- */

    private Site nearestPlacedOfType(SitesConfig sites, String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(POOL_TYPE)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) { if (best == null) best = s; continue; }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }
}
