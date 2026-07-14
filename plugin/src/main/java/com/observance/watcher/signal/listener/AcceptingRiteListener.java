package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The Accepting — the TERMINAL group rite (red-team MF-8 / seed {@code accepting-crouch}). When EVERY
 * player present on the Accepting floor (an {@code accepting_floor} site) crouches AT ONCE — "bow as
 * one" — the plugin posts the rite's OPAQUE, wordless token to the shared oracle, recording the solve
 * and firing the climax beat. The token is NEVER typeable (it is not a phrase a player could guess or
 * read off a wiki); only THIS detector — a real synchronized group bow, witnessed by the server — can
 * produce it. That is the entire point of the opaque token (B-5): the climax cannot be spoofed at a
 * sign or in Discord; it must be PERFORMED, together. This detector belonged to the retired V4
 * oracle, and V5 safe mode leaves it unreachable. Current production fairness is enforced by the V5
 * node manifest, runtime bindings, and surface checks.
 *
 * <p>Detection (anti-cheese):
 * <ul>
 *   <li>fires only inside an {@code accepting_floor} site's radius;</li>
 *   <li>requires a quorum (≥ {@code min} players present) AND that ALL present are sneaking at once;</li>
 *   <li>rate-limited per site (one attempt per cooldown) so crouch-spam can't hammer the oracle;</li>
 *   <li>resolves ASYNC (network) — the toggling player is the nominal solver; the solve is shared.</li>
 * </ul>
 *
 * <p>Pure: never cancels the event and never mutates the world. It gives only low-information
 * action-bar coordination receipts so the group can perform the literal bow without turning the
 * finale into a UI puzzle. Body in Safety. Sites resolved live via a {@link Supplier} so a config
 * reload is picked up without re-registering.
 *
 * <p><b>Cross-surface simultaneity (WEB-MASTER §1.M4):</b> the Accepting is the LAST hinge of the single
 * Iss IV→V chain — it must not be reachable before the three-hands coop gate has opened the Threshold and
 * the group has walked the true coordinate. An optional {@code readyGate} supplier carries that
 * precondition (read live, fail-CLOSED on null/unknown so a slow/offline arc-state read can never fire the
 * finale early). When the gate is not wired the listener behaves exactly as before (always-ready), so the
 * existing registration site is source-compatible. The gate is the WHOLE present-group bowing AT ONCE —
 * itself a same-instant cross-surface convergence whose opaque token cannot be typed (B-5).
 *
 * <p><b>Active-only quorum (INV-19, backlog D9 — the Accepting sentinel bridge):</b> the rite must never
 * punish the group for an ABSENT member. The configured {@code quorum} is the full cast size, but the bow
 * is gated on ACTIVE players only: {@code effectiveQuorum = min(configQuorum, activeRosterSize)}. The
 * active-roster size is read live each attempt from an optional {@link IntSupplier} (the showrunner's
 * {@code readActiveRoster(windowMs)} count, surfaced into the plugin); when that supplier is unwired it
 * falls back to the count of players currently online — which IS the active set at the instant of the bow,
 * so the listener stays source-compatible and never blocks on a DB read mid-event. The detector still
 * requires that EVERY present player on the floor is bowing at once; the clamp only ensures a smaller-than-
 * cast active group can still complete the rite together (never gated on someone who isn't here).
 *
 * <p><b>Divergent endings (WEB-MASTER §5, INV-11):</b> this detector is fate-NEUTRAL. It posts the one
 * opaque {@code accepting-crouch} token; <i>which</i> close composes (kept/cast_out/divided/refusers, the
 * codicil, the forks, the Seventh tint) is decided by the showrunner's single M5 composer from
 * already-tracked signals — never here. The plugin never branches the ending; it only records that the bow
 * happened, so the four fates stay a property of the group's measured conduct, not of this listener.
 */
public final class AcceptingRiteListener implements Listener {

    private static final String RITE_SITE_TYPE = "accepting_floor";

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    private final int quorum;
    private final long cooldownMs;
    /**
     * Cross-surface precondition: TRUE once the three-hands coop gate has opened the Threshold (arc_state
     * {@code threshold_open}). Read live each attempt so a reload/flip re-arms without re-registering.
     * A null supplier means UNWIRED → always-ready (legacy/source-compatible). A wired supplier is
     * FAIL-CLOSED: a null answer or a thrown read are treated as "not ready" — once the precondition is
     * declared, the terminal rite never fires on an unknown answer (anti-jank: no early/spurious finale).
     */
    private final Supplier<Boolean> readyGate;
    /**
     * Live ACTIVE-roster size (INV-19). When wired, this is the showrunner's {@code readActiveRoster(windowMs)}
     * count surfaced into the plugin; the effective quorum is clamped to it so an absent cast member can never
     * block the bow. A null supplier means UNWIRED → fall back to the count of players currently online (the
     * active set at the instant of the bow). Read live each attempt; never blocks (a cheap count, no DB call
     * on the event thread when unwired).
     */
    private final IntSupplier activeRosterSize;

    /** Backward-compatible constructor (always-ready, online-count roster). The existing registration site
     *  uses this; the coop precondition + active roster are wired by the overloads below. */
    public AcceptingRiteListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                 RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                 boolean enabled, String token, String puzzleKey,
                                 int quorum, long cooldownMs) {
        this(sitesSupplier, oracle, rateLimiter, scheduler, safety,
                enabled, token, puzzleKey, quorum, cooldownMs, null);
    }

    /** Coop-precondition wired, active roster defaulting to the online count (source-compatible overload). */
    public AcceptingRiteListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                 RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                 boolean enabled, String token, String puzzleKey,
                                 int quorum, long cooldownMs, Supplier<Boolean> readyGate) {
        this(sitesSupplier, oracle, rateLimiter, scheduler, safety,
                enabled, token, puzzleKey, quorum, cooldownMs, readyGate, null);
    }

    public AcceptingRiteListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                 RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                 boolean enabled, String token, String puzzleKey,
                                 int quorum, long cooldownMs, Supplier<Boolean> readyGate,
                                 IntSupplier activeRosterSize) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "accepting-crouch" : puzzleKey.trim();
        this.quorum = Math.max(1, quorum);
        this.cooldownMs = Math.max(1_000L, cooldownMs);
        this.readyGate = readyGate;
        this.activeRosterSize = activeRosterSize;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        safety.run("rite.accepting.sneak", () -> {
            if (!enabled || token.isBlank() || oracle == null || scheduler == null) return;
            if (!event.isSneaking()) return;                 // only the moment a bow BEGINS

            Player toggler = event.getPlayer();
            if (toggler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = toggler.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site floor = nearestPlacedOfType(sites, RITE_SITE_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (floor == null) return;                       // not on the Accepting floor

            // Everyone currently within the floor. The toggler counts as sneaking — the event IS their bow.
            List<Player> present = playersInSite(floor, world);
            // INV-19: clamp the configured (full-cast) quorum to the live ACTIVE roster so an absent member
            // never blocks the bow. effectiveQuorum = min(configQuorum, activeRosterSize). Read live, never
            // below 1 (a solo active keeper can still bow), and never above the configured cast size.
            int effectiveQuorum = effectiveQuorum();
            if (present.size() < effectiveQuorum) {
                sendRiteFeedback(present, "the floor waits for " + (effectiveQuorum - present.size()) + " more.");
                return;                                      // not the whole present (active) group yet
            }
            int bowed = 0;
            for (Player p : present) {
                boolean sneaking = p.equals(toggler) || p.isSneaking();
                if (sneaking) bowed++;
            }
            if (bowed < present.size()) {
                sendBowProgress(present, toggler, present.size() - bowed);
                return;                                      // someone present is NOT bowing -> not yet
            }

            // Cross-surface precondition: the Threshold must already be open (the three-hands coop gate +
            // the true walk preceded this). Fail-CLOSED — an unwired/unknown gate withholds the finale,
            // silently (no tell), exactly like a miss. The bow is reusable, so a true-later flip re-arms.
            if (!isReady()) {
                sendRiteFeedback(present, "not the hour.");
                return;
            }

            // All present are bowing as one. Fire ONCE per cooldown window for this site.
            if (!rateLimiter.tryCooldown("accepting:" + floor.id(), cooldownMs)) return;
            sendRiteFeedback(present, "the floor answers.");

            final String mc = toggler.getUniqueId().toString();
            final String name = toggler.getName();
            safety.info("rite.accepting", name + " + " + (present.size() - 1)
                    + " bowed as one on " + floor.id() + " — posting the Accepting");
            // resolveWorld does network I/O → async. The opaque token is bound to the rite's puzzle key.
            scheduler.runAsyncSafe("rite.accepting.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    private void sendBowProgress(List<Player> present, Player toggler, int missing) {
        String bowedText = "you are bowed. waiting on " + missing + ".";
        for (Player p : present) {
            boolean bowed = p.equals(toggler) || p.isSneaking();
            sendRiteFeedback(p, bowed ? bowedText : "the light waits for your bow.");
        }
    }

    private void sendRiteFeedback(List<Player> players, String text) {
        for (Player p : players) {
            sendRiteFeedback(p, text);
        }
    }

    private void sendRiteFeedback(Player p, String text) {
        if (p == null || text == null || text.isBlank()) return;
        if (!rateLimiter.tryCooldown("accepting.feedback:" + p.getUniqueId(), 1_000L)) return;
        try {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.12f, 0.45f);
        } catch (Throwable ignored) {
            // atmospheric only
        }
        try {
            p.sendActionBar(Component.text(text, NamedTextColor.DARK_GRAY));
        } catch (Throwable ignored) {
            // older clients or proxy shims may not support action bars
        }
    }

    /**
     * The active-only quorum (INV-19): {@code min(configQuorum, activeRosterSize)}, floored at 1. When the
     * roster supplier is unwired or throws, the active set is the players currently online (the real active
     * set at the instant of the bow) — never blocks, never reads the DB on the event thread. This is what
     * keeps the rite from being gated on a cast member who simply isn't here.
     */
    private int effectiveQuorum() {
        return clampQuorum(quorum, readActiveRosterSize());
    }

    /** Pure INV-19 clamp: {@code min(configQuorum, activeRosterSize)}, floored at 1. Extracted so the rule
     *  is server-free testable (a roster smaller than the cast lowers the bar; it never raises it). */
    static int clampQuorum(int configQuorum, int activeRosterSize) {
        int cfg = Math.max(1, configQuorum);
        int active = Math.max(1, activeRosterSize); // a lone active keeper can still complete the bow
        return Math.max(1, Math.min(cfg, active));
    }

    /** Live active-roster size: the wired supplier (showrunner's readActiveRoster count) or, unwired/failed,
     *  the count of currently-online players. Always ≥ 0; fail-safe (a thrown supplier falls back to online). */
    private int readActiveRosterSize() {
        if (activeRosterSize != null) {
            Integer n = safety.call("rite.accepting.roster", activeRosterSize::getAsInt, null);
            if (n != null && n >= 0) return n;
        }
        try {
            return Bukkit.getOnlinePlayers().size();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Cross-surface readiness (Threshold open). Fail-CLOSED: null gate / null answer / thrown read all
     *  read as NOT ready, so the terminal rite can never fire on an unknown precondition. */
    private boolean isReady() {
        if (readyGate == null) return true; // unwired = legacy always-ready (source-compatible default)
        Boolean ok = safety.call("rite.accepting.ready", readyGate::get, Boolean.FALSE);
        return Boolean.TRUE.equals(ok);
    }

    private List<Player> playersInSite(Site site, String world) {
        List<Player> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null) continue;
            if (!l.getWorld().getName().equals(world)) continue;
            if (site.contains(world, l.getX(), l.getY(), l.getZ())) out.add(p);
        }
        return out;
    }

    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) { if (best == null) best = s; continue; }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the INV-19 active-only quorum clamp the bow gate leans on: a full-cast roster keeps the
     * configured quorum; a SMALLER active roster lowers the bar (never punishes an absent member); a
     * LARGER roster never raises it above the cast; and the floor is always ≥ 1 (a lone active keeper can
     * still complete the bow). A regression here would either dead-lock the finale on someone who isn't
     * present, or let the rite fire below the intended convergence.
     */
    static boolean quorumClampSelfTest() {
        if (clampQuorum(4, 4) != 4) return false;   // full cast → configured quorum
        if (clampQuorum(4, 2) != 2) return false;   // smaller active set → lowered bar (never blocks absent)
        if (clampQuorum(4, 9) != 4) return false;   // larger roster never raises above the cast
        if (clampQuorum(4, 0) != 1) return false;   // empty/failed roster floors at 1, never 0
        if (clampQuorum(0, 3) != 1) return false;   // a degenerate config still floors at 1
        if (clampQuorum(1, 5) != 1) return false;   // solo cast stays solo
        return true;
    }
}
