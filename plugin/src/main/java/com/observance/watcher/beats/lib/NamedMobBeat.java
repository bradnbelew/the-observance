package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * MOBS — a named mob appears that won't despawn, is silent, and stares at / follows ONE player. The
 * canonical "the watcher". Spawned a short distance away, out of line of sight (discovered, not
 * witnessed appearing), on valid ground (no floaters/suffocation). Tagged in PDC so it's recognized
 * as a beat entity (for cleanup + anti-grief), set persistent + silent + invulnerable-by-default so
 * players can't trivially kill/weaponize it. When {@code no_ai_drift} (the default), its AI is fully
 * DISABLED so it cannot path, wander, or attack — it simply stands and stares (bestiary rule #1:
 * never lunge, never approach). It keeps the facing it spawned with (turned toward the player).
 *
 * <p><b>Offline-skin apparition (B3 / WEB-MASTER §P2.5, INV-16).</b> Two optional payload fields turn this
 * into the rare "wearing an offline player's shape" glimpse:
 * <ul>
 *   <li>{@code skin_player} — the name/UUID of the player whose shape this apparition rhymes (the worn
 *       skin). Cache-first resolve; if the texture can't be applied (no ModelEngine model yet, profile not
 *       cached) it falls back to the dark WARDEN/STRAY <b>silhouette</b> — never a wrong green zombie.</li>
 *   <li>{@code offline_only} (default true when {@code skin_player} is set) — the SEPARATION LAW: the
 *       apparition is suppressed if the worn player is currently ONLINE, and never wears the TARGET's own
 *       shape. Re-checked at fire, not just at enqueue, so a logout/login race can't co-locate a player
 *       with their own apparition (the on-camera break this prevents).</li>
 * </ul>
 * A {@code retreating} apparition (the Seventh glimpse) is <b>un-targeted</b> — it spawns facing AWAY from
 * the player and never paths/follows (the principled INV precision exception: a glimpse of a shape turning
 * away, witnessed by no one as it moves, because it does not move). Despawn-on-rejoin of the worn player is
 * handled by {@code PresenceListener.despawnApparitionsWearing(uuid)} (reveal-disciplined) — this beat tags
 * the apparition with the worn UUID so that sweep can find it.
 *
 * <p>Payload:
 * <pre>{@code
 * { "entity":"ZOMBIE", "name":"...", "distance":12, "silent":true, "no_ai_drift":true,
 *   "invulnerable":true, "glowing":false, "despawn_seconds":0,
 *   "skin_player":"Brann", "offline_only":true, "retreating":false }
 * }</pre>
 */
public final class NamedMobBeat extends AbstractBeat {

    @Override public String name() { return "named_mob"; }
    @Override public String description() { return "A named, silent, persistent mob appears nearby and watches one player."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        EntityType type = resolveEntity(req.payload());
        if (!type.isSpawnable() || !type.isAlive()) return false;
        // Offline-skin separation law: if a worn player is named and (by default) must be offline,
        // a worn player who is online OR is the target makes this beat un-enactable. Cheap pre-check;
        // re-verified at fire in doEnact (the authoritative gate against a join/leave race).
        if (!wornPlayerEligible(req)) return false;
        return findSpawn(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        EntityType type = resolveEntity(p);
        if (!type.isSpawnable() || !type.isAlive()) return BeatResult.skipped("bad-entity");

        // Re-check the offline-skin separation law AT FIRE (authoritative): the worn player may have
        // logged in since this beat was enqueued. If so, drop it silently — never co-locate a player
        // with an apparition wearing their own shape.
        if (!wornPlayerEligible(req)) return BeatResult.skipped("worn-online");

        Location spawn = findSpawn(ctx, req);
        if (spawn == null) return BeatResult.skipped("no-spawn-spot");

        final boolean silent = p.bool("silent", true);
        final boolean invulnerable = p.bool("invulnerable", true);
        final boolean glowing = p.bool("glowing", false);
        final boolean noDrift = p.bool("no_ai_drift", true);
        final String displayName = p.string("name", null);
        final long despawnTicks = Math.max(0L, p.integer("despawn_seconds", 0) * 20L);

        Entity spawned;
        try {
            spawned = spawn.getWorld().spawnEntity(spawn, type);
        } catch (Throwable t) {
            return BeatResult.failed("spawn-error");
        }
        if (!(spawned instanceof LivingEntity living)) {
            try { spawned.remove(); } catch (Throwable ignored) { }
            return BeatResult.failed("not-living");
        }

        // Configure the apparition.
        living.setRemoveWhenFarAway(false);
        living.setPersistent(true);
        living.setSilent(silent);
        living.setInvulnerable(invulnerable);
        living.setCanPickupItems(false);
        try { living.setGlowing(glowing); } catch (Throwable ignored) { }
        if (displayName != null && !displayName.isBlank()) {
            living.customName(Component.text(displayName.length() > 64 ? displayName.substring(0, 64) : displayName));
            living.setCustomNameVisible(p.bool("name_visible", false));
        }
        // Tag as a beat entity for cleanup + anti-grief recognition.
        try {
            living.getPersistentDataContainer().set(
                    key(ctx, "beat_entity"), PersistentDataType.STRING, req.beatId());
            living.getPersistentDataContainer().set(
                    key(ctx, "beat_owner"), PersistentDataType.STRING, req.targetUuid().toString());
        } catch (Throwable ignored) { }

        // Offline-skin: try to wear the named player's shape (cache-first; silhouette fallback). Tag the
        // worn UUID so PresenceListener.despawnApparitionsWearing(uuid) can sweep it the instant the worn
        // player rejoins (reveal-disciplined). applyWornSkin NEVER swaps the silhouette to a wrong mob.
        java.util.UUID wornUuid = wornPlayerUuid(p);
        if (wornUuid != null) {
            try {
                living.getPersistentDataContainer().set(
                        key(ctx, "worn_skin"), PersistentDataType.STRING, wornUuid.toString());
            } catch (Throwable ignored) { }
            applyWornSkin(ctx, living, wornUuid);
        }

        // The Seventh glimpse: a RETREATING apparition is un-targeted and faces AWAY (a shape turning its
        // back). It still never moves (AI is frozen below) — the retreat is read in the facing, not in
        // pathfinding (B3 CUTs walking/following). Flip the spawn yaw 180° so it presents its back.
        final boolean retreating = p.bool("retreating", false);
        if (retreating) {
            try {
                Location l = living.getLocation();
                l.setYaw(l.getYaw() + 180f);
                living.teleport(l);
            } catch (Throwable ignored) { }
        }

        if (noDrift && living instanceof Mob mob) {
            // Stand and stare — never path, never lunge, never attack (bestiary rule #1). Disabling AI
            // freezes the mob entirely: it cannot pathfind toward, drift into, or strike the player. It
            // keeps the yaw it spawned with (findSpawn faced the player, or 180° away when retreating), so
            // it reads as a silent watcher / a shape turned away, not a hostile drifting in to swing. We
            // deliberately do NOT setTarget: a live target on a hostile type (WARDEN/DROWNED/ZOMBIE) makes
            // it path to and ATTACK the player — the exact opposite of no-drift, and the bug this replaces.
            try { mob.setAware(false); } catch (Throwable ignored) { }
            try { mob.setAI(false); } catch (Throwable ignored) { }
        }

        if (despawnTicks > 0) {
            final java.util.UUID eid = living.getUniqueId();
            ctx.scheduler().runLaterSafe("beat.mob.despawn", despawnTicks, () -> {
                Entity e = org.bukkit.Bukkit.getEntity(eid);
                if (e != null && e.isValid()) {
                    boolean hidden = ctx.safety().call("beat.mob.despawn.check",
                            () -> ctx.reveal().isHidden(e.getLocation().getBlock()), Boolean.TRUE);
                    if (Boolean.TRUE.equals(hidden)) e.remove();
                    else {
                        // try again shortly so it vanishes unwitnessed
                        ctx.scheduler().runLaterSafe("beat.mob.despawn.retry",
                                ctx.config().revealRetryDelayTicks(), () -> {
                                    Entity e2 = org.bukkit.Bukkit.getEntity(eid);
                                    if (e2 != null && e2.isValid()) e2.remove();
                                });
                    }
                }
            });
        }
        return BeatResult.fired("mob-spawned");
    }

    /** Find a valid standing spot ~distance blocks from the player, out of LoS. MAIN thread. */
    private static Location findSpawn(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return null;
        var pl = req.targetPlayer();
        Location eye = pl.getEyeLocation();
        int distance = Math.max(4, Math.min(48, req.payload().integer("distance", 12)));
        // Try a ring of candidate angles; pick the first that is on valid ground AND hidden.
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            double dx = Math.cos(rad) * distance;
            double dz = Math.sin(rad) * distance;
            Location cand = pl.getLocation().clone().add(dx, 0, dz);
            org.bukkit.block.Block surface = Placement.findSurfaceSpot(cand.clone().add(0, 2, 0), 6);
            if (surface == null) continue;
            Location spot = surface.getLocation().add(0.5, 1.0, 0.5);
            // headroom check: the cell above the floor and one more must be passable
            if (!surface.getRelative(org.bukkit.block.BlockFace.UP).isPassable()) continue;
            if (!surface.getRelative(0, 2, 0).isPassable()) continue;
            boolean hidden = ctx.safety().call("beat.mob.spawn.check",
                    () -> ctx.reveal().isHidden(spot), Boolean.TRUE);
            if (Boolean.TRUE.equals(hidden)) {
                // face the player
                org.bukkit.util.Vector dir = pl.getLocation().toVector().subtract(spot.toVector());
                spot.setDirection(dir);
                return spot;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Offline-skin apparition                                            */
    /* ------------------------------------------------------------------ */

    /**
     * The SEPARATION-LAW gate (B3): an apparition that wears {@code skin_player} may only appear when
     * that worn player is OFFLINE (the default when a skin is named), and never wears the TARGET's own
     * shape. Returns true when the beat is free to fire. Beats with no {@code skin_player} are always
     * eligible (this only governs the worn-shape variant). Fail-OPEN only for an unresolvable name with
     * {@code offline_only} relaxed — never spawns a worn shape we couldn't verify is offline.
     */
    private static boolean wornPlayerEligible(BeatRequest req) {
        BeatPayload p = req.payload();
        java.util.UUID worn = wornPlayerUuid(p);
        if (worn == null) return true;                       // not a worn-skin apparition → unrestricted
        if (req.hasTarget() && worn.equals(req.targetUuid())) return false; // never your own shape
        boolean offlineOnly = p.bool("offline_only", true);  // worn shapes default to offline-only
        if (!offlineOnly) return true;
        try {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(worn);
            return online == null || !online.isOnline();     // suppress while the worn player is on
        } catch (Throwable t) {
            return false;                                    // can't verify → don't risk co-location
        }
    }

    /** Resolve {@code skin_player} (a UUID string or a name) to a UUID, or null if absent/unresolvable.
     *  Name resolution is cache-first (no blocking Mojang call on the main thread). */
    private static java.util.UUID wornPlayerUuid(BeatPayload p) {
        String s = p.string("skin_player", null);
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        // Try a literal UUID first.
        try { return java.util.UUID.fromString(s); } catch (Throwable ignored) { }
        // Else resolve by name via the offline cache (no network on main thread).
        try {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayerIfCached(s);
            if (off != null) return off.getUniqueId();
        } catch (Throwable ignored) { }
        return null;
    }

    /**
     * Apply the worn player's shape to the apparition, cache-first, with a guaranteed SILHOUETTE fallback.
     * A real player-skin texture on a mob requires the ModelEngine/NPC layer (go-live asset); until that
     * lands this method establishes the worn identity (custom name = the worn player's last-known name,
     * if cached) and leaves the entity as the dark WARDEN/STRAY silhouette {@link #resolveEntity} already
     * chose — never a wrong, recognizable vanilla mob. It NEVER blocks (no Mojang fetch) and NEVER throws.
     * The worn-UUID PDC tag (set by the caller) is what {@code PresenceListener} keys its rejoin-sweep on.
     */
    private static void applyWornSkin(BeatContext ctx, LivingEntity apparition, java.util.UUID wornUuid) {
        if (apparition == null || wornUuid == null) return;
        try {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(wornUuid);
            String wornName = off == null ? null : off.getName();   // cached last-known name, may be null
            if (wornName != null && !wornName.isBlank() && apparition.customName() == null) {
                // Only set the worn name if the payload didn't already supply one (payload wins).
                apparition.customName(Component.text(
                        wornName.length() > 64 ? wornName.substring(0, 64) : wornName));
            }
            // ModelEngine/NPC texture application is go-live residue; the silhouette is the safe stand-in.
            if (ctx != null && ctx.config().debug()) {
                ctx.safety().info("beat.mob.worn_skin",
                        "apparition wears " + (wornName == null ? wornUuid.toString() : wornName)
                                + " (silhouette until ModelEngine layer)");
            }
        } catch (Throwable ignored) {
            // Any failure → leave the silhouette as-is. Never a wrong mob, never a crash.
        }
    }

    /** Parse a vanilla EntityType by name, or NULL if unparseable (a "mythicmob:…" id, a typo) —
     *  so the caller can FALL BACK instead of silently spawning the wrong creature. */
    private static EntityType entityType(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return EntityType.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resolve the apparition's entity (red-team MF-10). Tries the authored {@code entity}, then the
     * {@code fallback_entity} (default WARDEN — the tall, silent, dark silhouette the stand-and-stare
     * read depends on), then STRAY. NEVER a silent fall to ZOMBIE: a short green zombie standing in
     * for the Watcher is the exact on-camera break this prevents. So a {@code mythicmob:watcher} id
     * (unspawnable by vanilla here) renders as a WARDEN until the ModelEngine layer lands — never a
     * zombie. Always returns a spawnable, living type.
     */
    private static EntityType resolveEntity(BeatPayload p) {
        EntityType t = entityType(p.string("entity", null));
        if (t == null) t = entityType(p.string("fallback_entity", "WARDEN"));
        if (t == null || !t.isSpawnable() || !t.isAlive()) t = EntityType.STRAY;
        return t;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure worn-skin payload parsing the offline-skin gate leans on: a literal UUID resolves;
     * a no-skin payload resolves to null (so non-worn apparitions stay unrestricted); a bare name without
     * a cached profile resolves to null on this path (no blocking lookup). A regression here would either
     * NPE the eligibility gate or let an unverifiable worn shape spawn.
     */
    static boolean wornSkinSelfTest() {
        java.util.UUID u = java.util.UUID.fromString("00000000-0000-0000-0000-000000000abc");
        BeatPayload withUuid = BeatPayload.parse("{\"skin_player\":\"" + u + "\"}");
        if (!u.equals(wornPlayerUuid(withUuid))) return false;
        if (wornPlayerUuid(BeatPayload.empty()) != null) return false;           // no skin → null
        if (wornPlayerUuid(BeatPayload.parse("{\"skin_player\":\"\"}")) != null) return false; // blank → null
        // A non-UUID name resolves only via the offline cache; with no server context the parse path
        // returns null rather than throwing (the Bukkit call is wrapped). We assert it does not throw.
        try { wornPlayerUuid(BeatPayload.parse("{\"skin_player\":\"NotAUuidName\"}")); }
        catch (Throwable t) { return false; }
        return true;
    }
}
