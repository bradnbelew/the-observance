package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * THE FINALE RITE PRODUCER — the Seventh's restore-or-erase choice (INTEGRATION-V2 A1 / FACT 10b /
 * {@code seventh-choice}, {@code seventh-unwriting}). The reunion mechanic's missing hand: the site
 * {@code the_unwriting} (type {@code seventh_shrine}, stamped by {@code /observance placedeep}) hosts the
 * Seventh-choice rite at the effaced-name wall — restore the name that was cut out, or leave the blank a
 * blank — and until now NO listener produced it, so the finale was unreachable in-world.
 *
 * <p><b>The staged act (never typeable).</b> Modelled on {@link WrenNpcListener}'s reckoning markers and
 * on {@link AcceptingRiteListener} / {@link ThresholdVaultListener}'s opaque-token producers. Two
 * PDC-tagged choice markers stand at the effaced-name wall (stamped by {@code /observance placedeep} or a
 * world build), tagged {@code observance:seventh_choice} with value {@code restore} or {@code erase}.
 * A player right-clicks one — a real, in-world, witnessed gesture at the wall — and:
 * <ul>
 *   <li><b>restore</b> — the name is read back in. Sets {@code seventh_named=true} (FACT 10b, the
 *       {@code seventh-unwriting} payoff) AND {@code seventh_choice="restore"} via {@link
 *       SupabaseClient#mergeArcFlags} (idempotent shallow merge), then posts the OPAQUE restore token to
 *       the oracle so the showrunner's Seventh-choice sentinel branch composes the codicil / tints the
 *       close.</li>
 *   <li><b>erase</b> — the blank stays a blank (the Seventh's own request, honoured). Sets
 *       {@code seventh_choice="erase"} and posts the OPAQUE erase token. Does NOT set {@code seventh_named}
 *       (nothing is written back).</li>
 * </ul>
 * The two tokens ({@code seventh-choice.restore-token} / {@code erase-token} in config) byte-match the
 * seed's {@code accepted_answers} for {@code seventh-choice} — never a phrase a player could type at a
 * sign or in Discord (no-leaked-sentinel, B-5). The plugin never branches the ending; it only records
 * WHICH marker was touched. Which close composes is the showrunner's M5 job.
 *
 * <p><b>Gate (only after the deep is open).</b> Both branches are gated on {@code deep_gate_open} (the
 * same fail-CLOSED live arc-flag read {@link ThresholdVaultListener} uses). Read from a fresh arc_state
 * pull on the async worker (never on the event thread); an unknown / failed / unwired read withholds the
 * finale silently, exactly like a miss. The deep-half payoff can never fire before the descent is open.
 *
 * <p><b>One, once.</b> The choice is entered into the record EXACTLY ONCE. The write worker re-reads
 * {@code arc_state} and, if {@code seventh_choice} is already set, no-ops (a decision, once written, is
 * final — the same "the record does not take a second hand" contract as the reckoning). This is checked
 * against a fresh read so the contract holds across restarts and multiple players; the in-memory
 * rate-limit only debounces a double-click.
 *
 * <p><b>Pure / fault-isolated / reveal-safe.</b> Body in {@link Safety}; MONITOR priority; a {@link
 * RateLimiter} guard per player; all DB + oracle work hopped ASYNC; silent on any failure. Never cancels
 * the event, never mutates the world, never messages the room. Sites resolved live via a {@link Supplier}
 * so a reload / {@code placedeep} is picked up without re-registering.
 */
public final class SeventhChoiceListener implements Listener {

    /** The site type the finale rite lives under (stamped by {@code /observance placedeep}). */
    private static final String RITE_SITE_TYPE = "seventh_shrine";

    /** PDC sub-key marking a Seventh-choice marker entity; its STRING value is {@code restore|erase}. */
    public static final String PDC_SEVENTH_CHOICE = "seventh_choice";

    /** Arc flags. {@code seventh_named} is the restore payoff (FACT 10b); {@code seventh_choice} the fork. */
    public static final String FLAG_SEVENTH_NAMED  = "seventh_named";
    public static final String FLAG_SEVENTH_CHOICE = "seventh_choice";

    public static final String CHOICE_RESTORE = "restore";
    public static final String CHOICE_ERASE   = "erase";

    /** Per-player cooldown — debounces a double-click; long enough for the async round-trip. */
    private static final long CHOICE_COOLDOWN_MS = 8_000L;

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final NamespacedKey choiceKey;

    private final boolean enabled;
    /** OPAQUE tokens posted on a real choice (byte-match the seed's seventh-choice accepted_answers). */
    private final String restoreToken;
    private final String eraseToken;
    private final String puzzleKey;

    /**
     * Live {@code deep_gate_open} reader (fail-CLOSED). Null = UNWIRED → the finale never fires (closed),
     * so an un-wired deployment can't leak the reunion early. When wired, the read is done on the async
     * worker (a blocking arc_state pull), never on the event thread.
     */
    private final Supplier<Boolean> deepGateOpen;

    public SeventhChoiceListener(SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                                 OracleResolver oracle, RateLimiter rateLimiter, Scheduler scheduler,
                                 Safety safety, String namespace, boolean enabled,
                                 String restoreToken, String eraseToken, String puzzleKey,
                                 Supplier<Boolean> deepGateOpen) {
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.choiceKey = new NamespacedKey(ns, PDC_SEVENTH_CHOICE);
        this.enabled = enabled;
        this.restoreToken = restoreToken == null ? "" : restoreToken.trim();
        this.eraseToken = eraseToken == null ? "" : eraseToken.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "seventh-choice" : puzzleKey.trim();
        this.deepGateOpen = deepGateOpen;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("rite.seventh.interact", () -> {
            if (!enabled) return;
            Player p = event.getPlayer();
            if (p == null || supabase == null || scheduler == null) return;

            Entity clicked = event.getRightClicked();
            if (clicked == null) return;

            String choice = readChoice(clicked);       // restore | erase | null (not our marker)
            if (choice == null) return;

            // The marker must actually stand at a PLACED seventh_shrine site (defense in depth: a marker
            // dropped elsewhere is inert — the finale only fires at the unwriting wall).
            if (!markerAtRiteSite(clicked)) return;

            String cdKey = "seventh:" + p.getUniqueId();
            if (!rateLimiter.tryCooldown(cdKey, CHOICE_COOLDOWN_MS)) return;   // debounce a double-click

            final String name = p.getName();
            final String uuid = p.getUniqueId().toString();
            final boolean restore = CHOICE_RESTORE.equals(choice);
            final String token = restore ? restoreToken : eraseToken;

            scheduler.runAsyncSafe("rite.seventh.resolve", () -> {
                // Gate: the deep must be open (fail-CLOSED). Silent when closed — no tell.
                if (!isGateOpen()) {
                    notify(p, Component.text("the seal is a name. it is not time to read it.",
                            NamedTextColor.DARK_GRAY));
                    return;
                }

                // One, once — checked against a fresh read so the contract survives restarts / many players.
                Map<String, Object> flags = readFlags();
                if (truthyChoice(flags.get(FLAG_SEVENTH_CHOICE))) {
                    notify(p, Component.text("the name is already answered. the record does not take a second hand.",
                            NamedTextColor.DARK_GRAY));
                    return;
                }

                // Write the fork (idempotent shallow merge). restore ALSO names the Seventh (FACT 10b).
                JsonObject write = new JsonObject();
                write.addProperty(FLAG_SEVENTH_CHOICE, choice);
                if (restore) write.addProperty(FLAG_SEVENTH_NAMED, true);
                supabase.mergeArcFlags(write);

                supabase.insertEventLog(new EventLogRow(
                        "finale", "seventh.choice." + choice,
                        name + " " + (restore ? "read the seventh's name back in" : "left the blank a blank")
                                + " at the unwriting",
                        uuid, "{\"choice\":\"" + choice + "\"}", SupabaseClient.timestampNow()));

                safety.info("rite.seventh", name + " chose seventh_choice=" + choice
                        + (restore ? " (+seventh_named)" : ""));

                // Post the OPAQUE token so the showrunner's Seventh-choice sentinel branch composes the
                // codicil / tints the close. Never the combination — the marker touch IS the only producer.
                if (oracle != null && !token.isBlank()) {
                    oracle.resolveWorld(uuid, name, token, puzzleKey);
                }
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Gate (deep_gate_open) — fail-closed, read on the async worker      */
    /* ------------------------------------------------------------------ */

    /** True iff the deep gate is open. Fail-CLOSED: an unwired / unknown / thrown read reads as closed. */
    private boolean isGateOpen() {
        if (deepGateOpen == null) return false;   // unwired → closed (never leak the reunion early)
        Boolean ok = safety.call("rite.seventh.gate", deepGateOpen::get, Boolean.FALSE);
        return Boolean.TRUE.equals(ok);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /** Read the Seventh-choice value off a clicked entity, normalized, or null if it isn't a marker. */
    private String readChoice(Entity e) {
        try {
            var pdc = e.getPersistentDataContainer();
            if (!pdc.has(choiceKey, PersistentDataType.STRING)) return null;
            String v = pdc.get(choiceKey, PersistentDataType.STRING);
            if (v == null) return null;
            String t = v.trim().toLowerCase(Locale.ROOT);
            return (t.equals(CHOICE_RESTORE) || t.equals(CHOICE_ERASE)) ? t : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** True iff the clicked marker stands within a PLACED {@code seventh_shrine} site. */
    private boolean markerAtRiteSite(Entity e) {
        SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
        if (sites == null) return false;
        Location loc = e.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        String world = loc.getWorld().getName();
        for (Site s : sites.placedOfType(RITE_SITE_TYPE)) {
            if (s.contains(world, loc.getX(), loc.getY(), loc.getZ())) return true;
        }
        return false;
    }

    /** Read the full arc flags map (empty on any failure). Async-safe (DB read). */
    private Map<String, Object> readFlags() {
        try {
            var r = supabase.fetchArcState();
            if (r == null || !r.ok() || r.value() == null) return java.util.Collections.emptyMap();
            return r.value().flagsMap();
        } catch (Throwable t) {
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * "The choice is already made" test. {@code seventh_choice} is a STRING flag ({@code restore|erase}),
     * so any non-blank string counts as set; also tolerant of a boolean/number for robustness.
     */
    static boolean truthyChoice(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) return !s.trim().isEmpty();
        return false;
    }

    /** Hop a private chat line back to the main thread for a player (async caller). Null-safe. */
    private void notify(Player p, Component msg) {
        final UUID id = p.getUniqueId();
        scheduler.runMainSafe("rite.seventh.notify", () -> {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null && pl.isOnline()) pl.sendMessage(msg);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the "one, once" gate the finale leans on: only a genuinely-set {@code seventh_choice} (a
     * non-blank fork string, or a truthy bool/number) blocks a second hand; an unset / blank / false flag
     * must leave the choice OPEN. A regression here would either lock the finale out entirely (a blank
     * read counted as "already answered") or let both forks be written (double-answer), corrupting the
     * ending the M5 composer reads.
     */
    static boolean choiceGateSelfTest() {
        if (truthyChoice(null)) return false;              // unset → open
        if (truthyChoice("")) return false;                // blank → open
        if (truthyChoice("   ")) return false;             // whitespace → open
        if (truthyChoice(Boolean.FALSE)) return false;     // false → open
        if (truthyChoice(0)) return false;                 // 0 → open
        if (!truthyChoice(CHOICE_RESTORE)) return false;   // "restore" → answered
        if (!truthyChoice(CHOICE_ERASE)) return false;     // "erase" → answered
        if (!truthyChoice(Boolean.TRUE)) return false;     // true → answered
        if (!truthyChoice(1)) return false;                // 1 → answered
        return true;
    }
}
