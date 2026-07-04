package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.config.Site;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import com.observance.watcher.util.TextFit;

import java.util.ArrayList;
import java.util.List;

/**
 * ITEMS — THE KEPT NEEDLE (the recovery-compass, "the keepers' way home").
 *
 * <p><b>FICTION.</b> A late-earned needle whose point settles toward the light that did not go out —
 * the canonical kept light below ({@code unbroken_light}, the descent to the Seventh). Diegetically it
 * is the keepers' way home: no player is ever un-findably lost once they hold it. It is a REWARD, not an
 * early crutch — the record hands it over only after the Seventh is named, so it points at a thing the
 * finder has already earned the right to know. Register: cold, sparse, declarative — never "compass",
 * "waypoint", or any game word (those live only on the item's name + lore, the diegetic content).
 *
 * <p><b>TRIGGER (gating is the caller's job).</b> This beat is the pure enactor; the LATE gate lives
 * upstream, exactly like every other gated row: the storylet/seed carries {@code requires_flags:
 * {seventh_named:true}} (post-reveal, FACT 10b) so the row is CLOSED until the Seventh is named, and
 * {@link com.observance.watcher.oracle.FlagGate} only lets it enqueue once the flag is truthy. The
 * plugin never re-reads arc_state here (a beat's {@code doEnact} is synchronous/main-thread; the flag
 * is a DB read done async by the showrunner/oracle before the row is queued). The manual test path
 * {@code /observance needle [player]} in {@code ObservanceCommand} enacts this directly for in-world
 * testing; the showrunner should queue a {@code beat_queue} row of type {@code kept_needle} gated on
 * {@code seventh_named} (payload below) for the real, earned grant.
 *
 * <p><b>INTERACTION.</b> Builds a {@link Material#COMPASS} with {@link CompassMeta}: the lodestone is
 * set to the target site's location with {@code setLodestoneTracked(false)} so the needle points there
 * even without a real lodestone block in the world (a needle that just knows the way). It resolves the
 * target site id from the payload (default {@code unbroken_light}) via {@link BeatContext#sites()}. It is
 * given to the target player, or to every present player when {@code "to":"all"} — never destructively:
 * an empty inventory slot only, so nothing hard-won is displaced (the decency floor).
 *
 * <p>Payload (all optional; defaults are the keeper register):
 * <pre>{@code
 * { "site": "unbroken_light",                 // target site id the needle settles toward
 *   "to": "target" | "all",                    // single active player (default) or all present
 *   "name": "a kept needle",                   // diegetic display name (keeper register, lowercase)
 *   "lore": [ "it points at the light that did not go out." ] }
 * }</pre>
 */
public final class KeptNeedleBeat extends AbstractBeat {

    /** The canonical kept light: the one fire that never went out, the descent to the Seventh. */
    private static final String DEFAULT_SITE = "unbroken_light";
    /** Diegetic defaults — the keeper register (lowercase, declarative). Overridable by the showrunner. */
    private static final String DEFAULT_NAME = "a kept needle";
    private static final String DEFAULT_LORE = "it points at the light that did not go out.";

    @Override public String name() { return "kept_needle"; }
    @Override public String description() { return "Gives a lodestone-needle item that points at the kept light."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Needs recipients and a resolvable, placed target site (else the needle would point nowhere).
        if (!"all".equalsIgnoreCase(req.payload().string("to", "target")) && !req.hasTarget()) return false;
        return targetLocation(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location dest = targetLocation(ctx, req);
        if (dest == null) return BeatResult.skipped("no-target-site");

        List<Player> recipients = recipients(ctx, req);
        if (recipients.isEmpty()) return BeatResult.skipped("no-recipients");

        BeatPayload p = req.payload();
        String name = p.string("name", DEFAULT_NAME);
        List<String> lore = p.stringList("lore");
        if (lore.isEmpty()) lore = java.util.List.of(DEFAULT_LORE);

        int given = 0;
        for (Player pl : recipients) {
            if (pl == null || !pl.isOnline()) continue;
            int slot = pl.getInventory().firstEmpty();
            if (slot < 0) continue;                         // never displace a held item (decency floor)
            pl.getInventory().setItem(slot, buildNeedle(dest, name, lore));
            given++;
        }
        if (given == 0) return BeatResult.skipped("no-empty-slot");
        return BeatResult.fired("kept-needle-given:" + given);
    }

    /** Build the needle: a COMPASS whose lodestone is the kept light, tracked=false so it points untethered. */
    private static ItemStack buildNeedle(Location dest, String name, List<String> lore) {
        ItemStack needle = new ItemStack(Material.COMPASS);
        if (needle.getItemMeta() instanceof CompassMeta meta) {
            meta.setLodestone(dest);
            meta.setLodestoneTracked(false);   // point at dest without a real lodestone block in the world
            meta.displayName(Component.text(clamp(name, TextFit.TOOLTIP_LINE_CHARS)));
            List<Component> comps = new ArrayList<>(lore.size());
            for (String line : lore) comps.add(Component.text(clamp(line == null ? "" : line, TextFit.TOOLTIP_LINE_CHARS)));
            meta.lore(comps);
            needle.setItemMeta(meta);
        }
        return needle;
    }

    /**
     * Resolve the target site's Location: the payload {@code site} (default {@code unbroken_light}) via the
     * sites config, else the request's own site. Null if unresolved or unplaced. MAIN thread (touches Bukkit).
     */
    private static Location targetLocation(BeatContext ctx, BeatRequest req) {
        String id = req.payload().string("site", DEFAULT_SITE);
        if (ctx != null && ctx.sites() != null) {
            Site s = ctx.sites().get(id);
            if (s != null && s.location() != null) return s.location();
        }
        // Fall back to the request's bound site if it is the one we were pointed at.
        if (req.hasSite() && req.site().location() != null) return req.site().location();
        return null;
    }

    /** The recipients: every present player when {@code to=all}, else the single active target. */
    private static List<Player> recipients(BeatContext ctx, BeatRequest req) {
        List<Player> out = new ArrayList<>();
        if ("all".equalsIgnoreCase(req.payload().string("to", "target"))) {
            if (ctx != null && ctx.plugin() != null) {
                out.addAll(ctx.plugin().getServer().getOnlinePlayers());
            }
            return out;
        }
        Player t = target(req);
        if (t != null) out.add(t);
        return out;
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
