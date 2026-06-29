package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
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
import java.util.function.Supplier;

/**
 * THE PRESIDING-KEEPER OPEN. A player right-clicks the Keeper NPC at a rite-side site (the Threshold
 * or the Undercroft altar) → this signals the showrunner that {@code (player, site)} opened the
 * Keeper, so the showrunner resolves the dossier branch ({@code arc_state.flags} + {@code
 * punishment_state} → which {@code keeper.*} node) and enqueues the {@link
 * com.observance.watcher.beats.lib.KeeperNpcBeat} with the bound lines. The plugin signals the
 * INTERACTION; the showrunner owns the TEXT and the BRANCH (INV-1, the voice rule — no story in the
 * engine, no branch decision here).
 *
 * <p><b>Why {@link PlayerInteractEntityEvent} + a PDC tag, not the Citizens API.</b> The Keeper is
 * driven by Citizens2/ZNPCsPlus, but binding to {@code net.citizensnpcs.api.event.NPCRightClickEvent}
 * would make this class fail to load on a server without Citizens — and the manifest's Path A keeps
 * the plugin self-contained. Instead this listens on the native Bukkit interact event and gates on a
 * {@code observance:keeper_npc} PersistentDataContainer tag carried by the NPC's underlying entity
 * (set once when the NPC is placed). Any NPC framework that backs its NPC with a real entity works,
 * and a server with no Citizens simply never has a tagged entity to click — the listener is inert,
 * never broken. Mirrors {@link AnswerSignListener} exactly: Safety-wrapped body, all Bukkit reads on
 * the MAIN thread, a {@link RateLimiter} guard, MONITOR priority, sites via a live {@link Supplier},
 * the write hopped ASYNC.
 *
 * <p><b>No feedback on a miss.</b> A right-click that isn't on a tagged Keeper at a Keeper site is
 * ignored silently (it is an ordinary interaction). A valid open writes ONE {@code event_log} row
 * (type {@code keeper}, context {@code npc.open}) with a tiny JSON {@code detail} the showrunner
 * parses; the player sees nothing until the resolved dialogue beat lands (the Keeper speaks). This
 * keeps the surface deterministic and the reveal contract trivially satisfied (no world write here).
 */
public final class KeeperNpcListener implements Listener {

    /** Rite-side site types the Keeper presides at (the Threshold + the Undercroft altar). */
    private static final String TYPE_THRESHOLD = "the_threshold";
    private static final String TYPE_KEEPER_ALTAR = "keeper_altar";

    /** PDC sub-key marking an entity as the Keeper NPC (set when the NPC is placed). */
    private static final String PDC_KEEPER = "keeper_npc";

    /** Coarse per-player open cooldown so a click-spam doesn't post a storm of opens. */
    private static final long OPEN_COOLDOWN_MS = 3_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final String namespace;

    public KeeperNpcListener(Supplier<SitesConfig> sitesSupplier, SupabaseClient supabase,
                             RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                             String namespace) {
        this.sitesSupplier = sitesSupplier;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("signal.KeeperNpc.interact", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            Entity clicked = event.getRightClicked();
            if (clicked == null) return;

            // Only a tagged Keeper NPC entity counts. Read the keeper node hint (the NPC may carry a
            // node override in PDC, e.g. a depth-/site-specific entry node); blank → showrunner decides.
            String keeperNode = readKeeperTag(clicked);
            if (keeperNode == null) return;   // not the Keeper — an ordinary interaction, ignore silently

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = clicked.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            // The Keeper presides only at rite-side sites; a Keeper standing elsewhere does not open.
            Site site = nearestRiteSite(sites, world, loc.getX(), loc.getY(), loc.getZ());
            if (site == null) return;

            // Coarse anti-spam per player+site.
            String cdKey = "keeperopen:" + p.getUniqueId() + ":" + site.id();
            if (!rateLimiter.tryCooldown(cdKey, OPEN_COOLDOWN_MS)) return;

            // Snapshot identity off the Bukkit objects before going async.
            final String mcUuid = p.getUniqueId().toString();
            final String name = p.getName();
            final String siteId = site.id();
            final String node = keeperNode;     // "" = let the showrunner branch from the dossier

            // A compact, parse-clean detail blob the showrunner reads. No story text — just the
            // interaction facts (who, where, optional NPC-supplied entry node).
            final String detail = "{\"surface\":\"keeper_open\",\"site\":\"" + esc(siteId)
                    + "\",\"node_hint\":\"" + esc(node) + "\"}";

            scheduler.runAsyncSafe("signal.keeper.open.write", () ->
                    supabase.insertEventLog(new EventLogRow(
                            "keeper", "npc.open",
                            name + " opened the keeper at " + siteId
                                    + (node.isBlank() ? "" : " (" + node + ")"),
                            mcUuid, detail, SupabaseClient.timestampNow())));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    /**
     * Read the {@code observance:keeper_npc} PDC tag off the clicked entity. Returns the tag's string
     * value (a node hint, possibly empty) when present, or null when the entity is not a Keeper. The
     * empty string is a VALID keeper marker (no node override) — distinct from null (not a keeper).
     */
    private String readKeeperTag(Entity e) {
        try {
            NamespacedKey k = new NamespacedKey(namespace, PDC_KEEPER);
            var pdc = e.getPersistentDataContainer();
            if (!pdc.has(k, PersistentDataType.STRING)) return null;
            String v = pdc.get(k, PersistentDataType.STRING);
            return v == null ? "" : v.trim();
        } catch (Throwable t) {
            return null;   // a quirky entity impl must never crash the listener
        }
    }

    /** Nearest rite-side site containing the Keeper's position: the Threshold first, then the altar. */
    private Site nearestRiteSite(SitesConfig sites, String world, double x, double y, double z) {
        Site best = bestOfType(sites, TYPE_THRESHOLD, world, x, y, z);
        if (best != null) return best;
        return bestOfType(sites, TYPE_KEEPER_ALTAR, world, x, y, z);
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

    /** Minimal JSON-string escaping for the two short fields we embed (site id, node hint). */
    static String esc(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                default:
                    if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.toString();
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (mirrors the repo's selftest idiom).    */
    /* ------------------------------------------------------------------ */

    /** Guards the JSON escaper so a site id / node hint with a quote or control char can't produce a
     *  malformed {@code detail} blob the showrunner fails to parse. */
    static boolean escSelfTest() {
        if (!"".equals(esc(null)) || !"".equals(esc(""))) return false;
        if (!"abc".equals(esc("abc"))) return false;
        if (!"a\\\"b".equals(esc("a\"b"))) return false;        // embedded quote escaped
        String ctrl = "x" + ((char) 0x01) + "y";
        if (!"x\\u0001y".equals(esc(ctrl))) return false;       // control char → \u escape
        return "a\\\\b".equals(esc("a\\b"));                     // backslash escaped
    }
}
