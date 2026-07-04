package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
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

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * THE RELEASE RITE PRODUCER — the final act (design/FINALE-THE-RELEASE.md). The group has reached the
 * end: the Accepting is made, the Seventh is named/chosen, the mask has come off (the Watcher was the
 * Seventh all along). The one act the record was built to prevent is to let it STOP — to un-keep it. This
 * listener produces that act: a PDC-tagged release marker stands at the Seventh's chamber ({@code
 * the_unwriting}, type {@code seventh_shrine}, stamped by {@code /observance placedeep}); a player
 * right-clicks it — a real, in-world, witnessed gesture on behalf of the group — and it sets {@code
 * record_released=true}. The showrunner's release pass then composes the mask-off farewell and enqueues
 * {@code the_closing} (the world dies + the kick).
 *
 * <p><b>Gate (only after the Accepting).</b> Fail-CLOSED on {@code bowed_as_one} (the Accepting rite's
 * flag): the release cannot be performed until the group has bowed as one. An unwired / failed / unknown
 * read withholds it silently, exactly like a miss — the ending can never fire early.
 *
 * <p><b>One, once.</b> The world ends exactly once: if {@code record_released} is already set (checked
 * against a fresh arc_state read so the contract survives restarts + many players), a second touch
 * no-ops. The in-memory rate-limit only debounces a double-click.
 *
 * <p><b>Pure / fault-isolated / reveal-safe.</b> Mirrors {@link SeventhChoiceListener}: body in {@link
 * Safety}, MONITOR priority, a {@link RateLimiter} guard, all DB work hopped ASYNC, silent on any failure.
 * Never cancels the event, never mutates the world here (the death theater is the plugin beat's job),
 * never messages the room except a private in-voice line to the acting player. Sites resolved live via a
 * {@link Supplier} so a reload / {@code placedeep} is picked up without re-registering.
 */
public final class ReleaseRiteListener implements Listener {

    /** The site type the release marker lives under (the Seventh's chamber; stamped by placedeep). */
    private static final String RITE_SITE_TYPE = "seventh_shrine";

    /** PDC sub-key marking a release marker entity (any non-blank string value counts). */
    public static final String PDC_RELEASE = "release";

    /** The flag written when the group performs the release — the showrunner's release-pass trigger. */
    public static final String FLAG_RECORD_RELEASED = "record_released";
    /** The gate: the Accepting rite must be made first (the release is the LAST act). */
    public static final String FLAG_BOWED_AS_ONE = "bowed_as_one";

    /** Per-player cooldown — debounces a double-click; long enough for the async round-trip. */
    private static final long RELEASE_COOLDOWN_MS = 8_000L;

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final NamespacedKey releaseKey;
    private final boolean enabled;

    public ReleaseRiteListener(SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                               RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                               String namespace, boolean enabled) {
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.releaseKey = new NamespacedKey(ns, PDC_RELEASE);
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("rite.release.interact", () -> {
            if (!enabled) return;
            Player p = event.getPlayer();
            if (p == null || supabase == null || scheduler == null) return;

            Entity clicked = event.getRightClicked();
            if (clicked == null) return;
            if (!isReleaseMarker(clicked)) return;             // not our marker — ignore silently
            if (!markerAtRiteSite(clicked)) return;            // defense in depth: only at the chamber

            String cdKey = "release:" + p.getUniqueId();
            if (!rateLimiter.tryCooldown(cdKey, RELEASE_COOLDOWN_MS)) return; // debounce a double-click

            final String name = p.getName();
            final String uuid = p.getUniqueId().toString();

            scheduler.runAsyncSafe("rite.release.resolve", () -> {
                Map<String, Object> flags = readFlags();

                // Gate: the Accepting must be made first (fail-closed). Silent-ish when not — one in-voice line.
                if (!truthyFlag(flags.get(FLAG_BOWED_AS_ONE))) {
                    notify(p, Component.text("it is not finished. the hands are not yet in.",
                            NamedTextColor.DARK_GRAY));
                    return;
                }

                // One, once — the world ends exactly once (fresh read; survives restarts + many players).
                if (truthyFlag(flags.get(FLAG_RECORD_RELEASED))) {
                    notify(p, Component.text("it is already done. it is closing.", NamedTextColor.DARK_GRAY));
                    return;
                }

                JsonObject write = new JsonObject();
                write.addProperty(FLAG_RECORD_RELEASED, true);
                supabase.mergeArcFlags(write);

                supabase.insertEventLog(new EventLogRow(
                        "finale", "record.released",
                        name + " performed the release at the unwriting — the record is let to stop",
                        uuid, "{}", SupabaseClient.timestampNow()));

                safety.info("rite.release", name + " set record_released — the showrunner will compose the farewell + fire the_closing");
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /** True iff the clicked entity carries the release PDC tag (any non-blank string value). */
    private boolean isReleaseMarker(Entity e) {
        try {
            var pdc = e.getPersistentDataContainer();
            if (!pdc.has(releaseKey, PersistentDataType.STRING)) return false;
            String v = pdc.get(releaseKey, PersistentDataType.STRING);
            return v != null && !v.trim().isEmpty();
        } catch (Throwable t) {
            return false;
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

    /** JS-equivalent truthiness for an arc flag (null/false/0/"" → false). */
    static boolean truthyFlag(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) return !s.trim().isEmpty();
        return false;
    }

    /** Hop a private chat line back to the main thread for a player (async caller). Null-safe. */
    private void notify(Player p, Component msg) {
        final UUID id = p.getUniqueId();
        scheduler.runMainSafe("rite.release.notify", () -> {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null && pl.isOnline()) pl.sendMessage(msg);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the "one, once" + gate truthiness the finale leans on: an unset/blank/false flag must read
     * OPEN (release not yet done / Accepting not yet made); a genuinely-set flag must read closed. A
     * regression would either lock the ending out or let the world end twice.
     */
    static boolean releaseGateSelfTest() {
        if (truthyFlag(null)) return false;        // unset → open
        if (truthyFlag("")) return false;          // blank → open
        if (truthyFlag("   ")) return false;       // whitespace → open
        if (truthyFlag(Boolean.FALSE)) return false;
        if (truthyFlag(0)) return false;
        if (!truthyFlag(Boolean.TRUE)) return false;
        if (!truthyFlag(1)) return false;
        if (!truthyFlag("true")) return false;     // string flag set
        return true;
    }
}
