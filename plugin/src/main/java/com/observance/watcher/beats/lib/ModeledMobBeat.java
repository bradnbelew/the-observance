package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * OPTIONAL LATE-ARC GARNISH ({@code backlog-modeled-mob-and-voice} / D11, P2). A near-clone of
 * {@link NamedMobBeat} that attaches a ModelEngine R4 rig to the apparition AFTER it spawns, so the
 * Watcher can wear a custom model instead of the WARDEN/STRAY silhouette — and <b>degrades to exactly
 * {@link NamedMobBeat}</b> when ModelEngine is absent, throws, or the model id is unknown. Never gates
 * anything: if the rig can't attach, the silhouette stands in and the beat still FIRED.
 *
 * <p><b>Composition, not inheritance.</b> {@link NamedMobBeat} is {@code final}, so this beat OWNS a
 * private {@code NamedMobBeat} delegate and forwards {@code canEnact}/{@code doEnact} to it verbatim —
 * the spawn, the offline-skin separation law, the no-AI-drift freeze, the despawn timer, all of it are
 * the delegate's, byte-for-byte. The ONLY thing this class adds is a post-spawn rig attach on the entity
 * the delegate just tagged ({@code observance:beat_entity}). With no {@code model} field, or with
 * ModelEngine missing, this beat IS {@code NamedMobBeat} with a different registry name.
 *
 * <p><b>ModelEngine is never compiled against.</b> All ModelEngine access is reflection + a plugin-name
 * probe (mirrors {@link Schematics}'s FAWE gate), so this class never class-loads a {@code com.ticxo.*}
 * type and the plugin builds + runs with ModelEngine entirely absent. A probe miss, a reflection failure,
 * or an unknown model id all collapse to the silhouette — no error reaches the engine.
 *
 * <p>Payload — every {@link NamedMobBeat} field, plus:
 * <pre>{@code
 * { "entity":"WARDEN", "name":"...", "distance":12, "no_ai_drift":true, ...,
 *   "model":"watcher" }     // ModelEngine R4 model id; absent/unknown → silhouette (exact NamedMobBeat)
 * }</pre>
 */
public final class ModeledMobBeat extends AbstractBeat {

    /** The spawn + configure + offline-skin + freeze + despawn behavior, reused verbatim. */
    private final NamedMobBeat delegate = new NamedMobBeat();

    /** Ticks to wait before attaching the rig — let the delegate finish spawning/configuring first. */
    private static final long RIG_ATTACH_DELAY_TICKS = 2L;

    @Override public String name() { return "modeled_mob"; }
    @Override public String description() { return "A NamedMobBeat that wears a ModelEngine rig; degrades to exactly NamedMobBeat."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Identical eligibility to NamedMobBeat — the rig is cosmetic, never a gate.
        return delegate.canEnact(ctx, req);
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        // Spawn exactly as NamedMobBeat would (same id, so the same PDC beat_entity tag lands).
        BeatResult r = delegate.enact(ctx, req);
        if (r == null || r.kind() != BeatResult.Kind.FIRED) {
            return r == null ? BeatResult.failed("delegate-null") : r;
        }

        // Optional rig attach. No model field → we ARE NamedMobBeat; return its result untouched.
        final String model = sanitizeModelId(req.payload().string("model", null));
        if (model == null) return r;
        if (!ModelEngineBridge.available()) {
            if (ctx.config().debug()) {
                ctx.safety().info("beat.modeled_mob",
                        "model '" + model + "' requested but ModelEngine absent — silhouette stands in");
            }
            return r; // exact NamedMobBeat behavior
        }

        // Find the entity the delegate just spawned+tagged and attach the rig on a short delayed MAIN task
        // (the entity is fully initialized by then). The attach is reveal-irrelevant — it re-skins an entity
        // that already exists out of line of sight; no block/world mutation, so no reveal gate is needed.
        final String beatId = req.beatId();
        final java.util.UUID owner = req.targetUuid();
        ctx.scheduler().runLaterSafe("beat.modeled_mob.rig", RIG_ATTACH_DELAY_TICKS, () -> {
            Entity e = findSpawnedApparition(ctx, beatId, owner);
            if (e == null) return; // already despawned / not found → silhouette stands (still FIRED)
            boolean ok = ModelEngineBridge.attach(e, model);
            if (!ok && ctx.config().debug()) {
                ctx.safety().info("beat.modeled_mob",
                        "rig attach for model '" + model + "' failed — silhouette stands in");
            }
        });
        return r;
    }

    /* ------------------------------------------------------------------ */
    /*  Locate the delegate's just-spawned, tagged apparition             */
    /* ------------------------------------------------------------------ */

    /**
     * Find the entity {@link NamedMobBeat} just spawned for this {@code (beatId, owner)} by reading the
     * {@code observance:beat_entity} + {@code observance:beat_owner} PDC tags it sets. Scans the target
     * player's nearby entities (the apparition spawns within ~48 blocks). Returns null if none match
     * (the rig simply doesn't attach — the silhouette stands). MAIN thread. Never throws.
     */
    private static Entity findSpawnedApparition(BeatContext ctx, String beatId, java.util.UUID owner) {
        if (beatId == null || owner == null) return null;
        try {
            org.bukkit.entity.Player pl = Bukkit.getPlayer(owner);
            if (pl == null || !pl.isOnline()) return null;
            NamespacedKey idKey = new NamespacedKey(ctx.namespace(), "beat_entity");
            NamespacedKey ownerKey = new NamespacedKey(ctx.namespace(), "beat_owner");
            String ownerStr = owner.toString();
            for (Entity e : pl.getNearbyEntities(64, 64, 64)) {
                if (e == null) continue;
                var pdc = e.getPersistentDataContainer();
                if (!pdc.has(idKey, PersistentDataType.STRING)) continue;
                if (!beatId.equals(pdc.get(idKey, PersistentDataType.STRING))) continue;
                String tagOwner = pdc.get(ownerKey, PersistentDataType.STRING);
                if (tagOwner == null || tagOwner.equals(ownerStr)) return e;
            }
        } catch (Throwable ignored) { }
        return null;
    }

    /** Lowercase + strip to [a-z0-9_-] so a model id can never carry surprises into reflection. Null if empty. */
    static String sanitizeModelId(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return s.isEmpty() ? null : s;
    }

    /* ------------------------------------------------------------------ */
    /*  ModelEngine R4 reflection bridge (FAWE-style isolation)            */
    /* ------------------------------------------------------------------ */

    /**
     * String-only, reflection-only access to ModelEngine R4 — references NO {@code com.ticxo.*} type, so
     * this plugin never class-loads ModelEngine and builds/runs with it absent. Probes by plugin name
     * once (cached), then attaches a rig via reflection. Every path catches {@code Throwable} and degrades
     * to {@code false} (silhouette stands). The exact R4 entry point can drift across builds; this calls
     * the stable {@code ModelEngineAPI.createModeledEntity(Entity)} →
     * {@code ActiveModel = ModelEngineAPI.createActiveModel(String)} → {@code modeledEntity.addModel(model)}
     * shape and falls back to {@code false} if any step is unavailable.
     */
    static final class ModelEngineBridge {
        private ModelEngineBridge() { }

        private static volatile Boolean available;

        static boolean available() {
            Boolean a = available;
            if (a != null) return a;
            boolean ok;
            try {
                ok = Bukkit.getPluginManager().isPluginEnabled("ModelEngine")
                        && probe();
            } catch (Throwable t) {
                ok = false;
            }
            available = ok;
            return ok;
        }

        private static boolean probe() {
            try {
                Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                return true;
            } catch (Throwable t) {
                return false;
            }
        }

        /** Attach {@code modelId} to {@code entity} via reflection. Returns false on any failure. MAIN thread. */
        static boolean attach(Entity entity, String modelId) {
            if (entity == null || modelId == null || modelId.isBlank()) return false;
            try {
                Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");

                Object modeled = api.getMethod("createModeledEntity", Entity.class).invoke(null, entity);
                if (modeled == null) return false;

                Object active = api.getMethod("createActiveModel", String.class).invoke(null, modelId);
                if (active == null) return false; // unknown model id → silhouette stands

                // modeledEntity.addModel(activeModel, boolean) is the common R4 signature; try it, then the
                // single-arg form, then give up. We never hard-depend on a signature that may have drifted.
                // invokeAddModel returns non-null when a matching overload was found and called (a void
                // return is normalized to TRUE), or null when no addModel overload accepted the model.
                return invokeAddModel(modeled, active) != null;
            } catch (Throwable t) {
                return false; // any reflection/link failure → silhouette stands (exact NamedMobBeat)
            }
        }

        /** Try the two-arg {@code addModel(model, boolean)} then the one-arg form. Returns a non-null on call. */
        private static Object invokeAddModel(Object modeled, Object active) throws Exception {
            Class<?> me = modeled.getClass();
            // Search for an addModel method whose first param accepts the active-model object.
            for (java.lang.reflect.Method m : me.getMethods()) {
                if (!"addModel".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                try {
                    if (ps.length == 2 && ps[0].isInstance(active)
                            && (ps[1] == boolean.class || ps[1] == Boolean.class)) {
                        Object r = m.invoke(modeled, active, true);
                        return r == null ? Boolean.TRUE : r;
                    }
                    if (ps.length == 1 && ps[0].isInstance(active)) {
                        Object r = m.invoke(modeled, active);
                        return r == null ? Boolean.TRUE : r;
                    }
                } catch (Throwable ignored) {
                    // try the next overload
                }
            }
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure model-id sanitizer (the only logic this class adds that runs off-server). A
     * regression would let a malformed id reach reflection or null-out a valid one. The reflection bridge
     * itself is exercised only with ModelEngine present (server-side); off-server {@link ModelEngineBridge#available()}
     * is asserted to be false-safe (never throws).
     */
    static boolean modeledMobSelfTest() {
        if (sanitizeModelId(null) != null) return false;
        if (sanitizeModelId("  ") != null) return false;
        if (!"watcher".equals(sanitizeModelId("Watcher"))) return false;
        if (!"the_keeper-01".equals(sanitizeModelId("the_keeper-01"))) return false;
        if (sanitizeModelId("../evil").contains("/")) return false;   // path chars stripped
        // The availability probe must never throw off-server (no ModelEngine plugin present in a test JVM).
        try { ModelEngineBridge.available(); } catch (Throwable t) { return false; }
        return true;
    }
}
