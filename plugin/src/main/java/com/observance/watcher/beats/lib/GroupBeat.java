package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.Beat;
import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatLibrary;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.config.Site;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * GROUP — fan ONE delegate beat out to EVERY player in a scene (red-team MF-8). The backbone of the
 * gather-events (THE COUNT, the un-lighting, the Accepting rehearsal): one authored beat felt by the
 * whole convened group at the same instant, so a moment lands on 4-6 friends together instead of one
 * lucky target. A dispatcher like {@link UnlockBeat} — the payload names a {@code beat} and carries
 * its {@code beat_payload}; the {@code scene} selects which players receive it.
 *
 * <p>Payload:
 * <pre>{@code
 * { "beat": "private_sound",
 *   "scene": "site" | "target" | "all",   // default: "site" when a site is set, else "target"
 *   "radius": 24,                          // scene=site: override (else use the site's own radius);
 *                                          // scene=target: ring around the target (default 24)
 *   "min": 1,                              // require at least this many in scene, else SKIP (no half-fire)
 *   "beat_payload": { ... } }              // the delegate's payload (falls back to this payload)
 * }</pre>
 *
 * <p>Per-player isolation: each fan-out runs under Safety so one player's delegate failure never
 * aborts the rest; the result reports how many actually fired. A delegate that itself targets a
 * single player (the PRIVATE_* sensory beats) is the intended use — GroupBeat simply gives each
 * member their own private copy of the same moment. Never throws; never self-delegates (no
 * group→group / group→unlock recursion).
 */
public final class GroupBeat extends AbstractBeat {

    private final BeatLibrary library;

    public GroupBeat(BeatLibrary library) {
        this.library = library;
    }

    @Override public String name() { return "group"; }
    @Override public String description() { return "Fans a delegate beat to every player in a scene (gather-events)."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Beat delegate = resolveDelegate(req);
        if (delegate == null || isSelf(delegate)) return false;
        return !scenePlayers(ctx, req).isEmpty();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Beat delegate = resolveDelegate(req);
        if (delegate == null) return BeatResult.skipped("unknown-beat");
        if (isSelf(delegate)) return BeatResult.skipped("no-self-delegate");

        List<Player> scene = scenePlayers(ctx, req);
        int min = Math.max(1, req.payload().integer("min", 1));
        if (scene.size() < min) return BeatResult.skipped("scene-too-small(" + scene.size() + "<" + min + ")");

        BeatPayload sub = subPayload(req);
        int fired = 0;
        for (Player p : scene) {
            // CRITICAL (audit): each fan-out gets a UNIQUE id. The delegate (an AbstractBeat) keys its
            // idempotency guard on beatId — reusing req.beatId() for all players would mark it applied on
            // the FIRST and skip the other 6 as "already-applied", so a gather-event would hit one person.
            // Per-player id keeps it idempotent per (group beat, player) — correct on a re-fire.
            final BeatRequest pr = new BeatRequest(
                    req.beatId() + ":" + p.getUniqueId(), delegate.name(), delegate.category(), p, req.site(), sub);
            boolean can = ctx.safety().call("beat.group.canEnact",
                    () -> delegate.canEnact(ctx, pr), Boolean.FALSE);
            if (!can) continue;
            BeatResult r = ctx.safety().call("beat.group.enact",
                    () -> delegate.enact(ctx, pr), BeatResult.failed("delegate-threw"));
            if (r != null && r.kind() == BeatResult.Kind.FIRED) fired++;
        }
        if (fired == 0) return BeatResult.skipped("no-fan-fired");
        return BeatResult.fired("group:" + delegate.name() + " x" + fired + "/" + scene.size());
    }

    /* ------------------------------------------------------------------ */

    private Beat resolveDelegate(BeatRequest req) {
        if (library == null) return null;
        String b = req.payload().string("beat", null);
        return (b == null || b.isBlank()) ? null : library.get(b);
    }

    /** Refuse to delegate to the dispatchers themselves (recursion / budget bypass). */
    private boolean isSelf(Beat delegate) {
        String n = delegate.name();
        return delegate == this || "group".equals(n) || "unlock".equals(n);
    }

    /** The delegate's payload — explicit {@code beat_payload}, else this payload (delegate ignores group keys). */
    private static BeatPayload subPayload(BeatRequest req) {
        BeatPayload bp = req.payload().object("beat_payload");
        return bp.raw().size() > 0 ? bp : req.payload();
    }

    /** Resolve the scene's online players: site-contained, ringed around the target, or all online. */
    private static List<Player> scenePlayers(BeatContext ctx, BeatRequest req) {
        List<Player> out = new ArrayList<>();
        if (ctx == null || ctx.plugin() == null) return out;
        var server = ctx.plugin().getServer();
        String scene = req.payload().string("scene", req.hasSite() ? "site" : "target");
        double radius = req.payload().number("radius", 0);

        if ("all".equalsIgnoreCase(scene)) {
            out.addAll(server.getOnlinePlayers());
            return out;
        }

        if ("site".equalsIgnoreCase(scene) && req.hasSite()) {
            Site site = req.site();
            for (Player p : server.getOnlinePlayers()) {
                Location l = playerLoc(p);
                if (l == null) continue;
                if (radius > 0) {
                    Location c = site.location();
                    if (c == null || c.getWorld() == null || !c.getWorld().equals(l.getWorld())) continue;
                    if (c.distanceSquared(l) <= radius * radius) out.add(p);
                } else if (site.contains(l.getWorld().getName(), l.getX(), l.getY(), l.getZ())) {
                    out.add(p);
                }
            }
            return out;
        }

        // scene = target: a ring around the target player (default 24 blocks).
        Player t = req.targetPlayer();
        Location tl = (t != null && t.isOnline()) ? playerLoc(t) : null;
        if (tl == null) return out;
        double r = radius > 0 ? radius : 24.0;
        for (Player p : server.getOnlinePlayers()) {
            Location l = playerLoc(p);
            if (l == null || !l.getWorld().equals(tl.getWorld())) continue;
            if (tl.distanceSquared(l) <= r * r) out.add(p);
        }
        return out;
    }

    private static Location playerLoc(Player p) {
        if (p == null || !p.isOnline()) return null;
        Location l = p.getLocation();
        return (l == null || l.getWorld() == null) ? null : l;
    }
}
