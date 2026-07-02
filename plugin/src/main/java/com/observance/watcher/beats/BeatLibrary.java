package com.observance.watcher.beats;

import com.observance.watcher.beats.lib.AbstractBeat;
import com.observance.watcher.beats.lib.AdvancementToastBeat;
import com.observance.watcher.beats.lib.BookAppearsBeat;
import com.observance.watcher.beats.lib.BossBarBeat;
import com.observance.watcher.beats.lib.ChestArrangeBeat;
import com.observance.watcher.beats.lib.ComposureBeat;
import com.observance.watcher.beats.lib.DecayCreepBeat;
import com.observance.watcher.beats.lib.DoorOpenBeat;
import com.observance.watcher.beats.lib.FakeBlockBeat;
import com.observance.watcher.beats.lib.GroupBeat;
import com.observance.watcher.beats.lib.HintWhisperBeat;
import com.observance.watcher.beats.lib.ItemRelabelBeat;
import com.observance.watcher.beats.lib.ItemSwapBeat;
import com.observance.watcher.beats.lib.KeeperNpcBeat;
import com.observance.watcher.beats.lib.LecternFillBeat;
import com.observance.watcher.beats.lib.MapMarkBeat;
import com.observance.watcher.beats.lib.ModeledMobBeat;
import com.observance.watcher.beats.lib.NameOnWallBeat;
import com.observance.watcher.beats.lib.NamedMobBeat;
import com.observance.watcher.beats.lib.PrivateDarknessBeat;
import com.observance.watcher.beats.lib.PrivateMessageBeat;
import com.observance.watcher.beats.lib.PrivateParticleBeat;
import com.observance.watcher.beats.lib.PrivateSoundBeat;
import com.observance.watcher.beats.lib.PrivateTimeShiftBeat;
import com.observance.watcher.beats.lib.ReflectionBeat;
import com.observance.watcher.beats.lib.RevealBeat;
import com.observance.watcher.beats.lib.RoomSwapBeat;
import com.observance.watcher.beats.lib.SacredAnimalBeat;
import com.observance.watcher.beats.lib.SignWriteBeat;
import com.observance.watcher.beats.lib.SmallStructureBeat;
import com.observance.watcher.beats.lib.SpatialVoiceBeat;
import com.observance.watcher.beats.lib.TorchGutterBeat;
import com.observance.watcher.beats.lib.UnlockBeat;
import com.observance.watcher.beats.lib.WhisperTollBeat;
import com.observance.watcher.util.Safety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The named, reusable catalog of beats (the interaction palette). Maps a {@code beat_queue.type}
 * (and ambient-synthesis key) to a concrete {@link Beat}. Registration order is the catalog order;
 * lookups are O(1). Every registered beat is independently validated + fault-isolated by the engine.
 *
 * <p>The library is also where ambient-eligible beats are selected from under the drama budget: it
 * exposes the AMBIENT-category beats so the {@code AmbientBeatGenerator} can pick one that
 * {@code canEnact} for a given request.
 */
public final class BeatLibrary {

    private final Map<String, Beat> byName = new LinkedHashMap<>();
    private final Safety safety;

    public BeatLibrary(Safety safety) {
        this.safety = safety;
        registerDefaults();
    }

    /** Register the full Phase-0 catalog. */
    private void registerDefaults() {
        // TEXT
        register(new LecternFillBeat());
        register(new BookAppearsBeat());
        register(new SignWriteBeat());
        register(new ItemRelabelBeat());
        register(new MapMarkBeat());
        // CLUE-DISCOVERY
        register(new ChestArrangeBeat());
        // ITEMS
        register(new ItemSwapBeat());
        // WORLD
        register(new TorchGutterBeat());
        register(new DoorOpenBeat());
        register(new DecayCreepBeat());
        register(new SmallStructureBeat());
        // PRODUCER TRIAD (design manifest): reveal (flip existing, per-player OR world) + room_swap
        // (sealed-door + teleport-on-reentry) alongside small_structure (paste). Reworked to fire for
        // co-located groups (reveal) / avoid in-place overwrite (room_swap) before registration (§10 fence).
        register(new RevealBeat());
        register(new RoomSwapBeat());
        // MOBS
        register(new NamedMobBeat());
        register(new SacredAnimalBeat());
        // MOBS (optional garnish) — a NamedMobBeat that wears a ModelEngine rig; degrades to exactly
        // NamedMobBeat when ModelEngine is absent/unknown-model (reflection-isolated, never gates).
        register(new ModeledMobBeat());
        // PERSONALIZED — the signature per-player illusion ("it knows ME")
        register(new NameOnWallBeat());
        // OBSERVER TIER-0 (BUILD-PLAN §13) — behavior-only "it knows you": a grounded implication line
        // to one player, derived purely from their measured signals. AMBIENT (paced by the drama budget).
        register(new ComposureBeat());
        // PERSONALIZED — Sella's rune, legible only in water's reflection (INTEGRATION §SIGNATURE #7)
        register(new ReflectionBeat());
        // SENSORY (per-player)
        register(new PrivateSoundBeat());
        register(new PrivateParticleBeat());
        register(new PrivateMessageBeat());
        register(new PrivateDarknessBeat());
        register(new BossBarBeat());
        register(new FakeBlockBeat());
        register(new PrivateTimeShiftBeat());
        // SENSORY (per-player) — the Ear's reply: a keeper voice clip spatialized behind the target.
        register(new SpatialVoiceBeat());
        // ACK
        register(new AdvancementToastBeat());
        // DIRECTED specials (bot/dashboard enqueued)
        register(new WhisperTollBeat());
        // DIRECTED — in-world hint delivery (findability): private whisper + optional floating note
        register(new HintWhisperBeat());
        // DIRECTED — the presiding Keeper / Wren speaks a resolved dialogue node to one player (the NPC
        // dialogue path; WrenNpcListener + KeeperNpcListener enqueue this with bound lines). Safe + used.
        register(new KeeperNpcBeat());
        register(new UnlockBeat(this));     // dispatcher — delegates to another beat type
        register(new GroupBeat(this));      // dispatcher — fans a delegate to every player in a scene (gather-events)
    }

    /** Register (or override) a beat by its {@link Beat#name()}. Null-safe. */
    public void register(Beat beat) {
        if (beat == null) return;
        String name = beat.name();
        if (name == null || name.isBlank()) return;
        byName.put(name.trim().toLowerCase(java.util.Locale.ROOT), beat);
    }

    /** Look up a beat by type/name, or null if unknown. */
    public Beat get(String type) {
        if (type == null) return null;
        return byName.get(type.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean has(String type) {
        return get(type) != null;
    }

    /** All registered beats, in catalog order. */
    public List<Beat> all() {
        return new ArrayList<>(byName.values());
    }

    /** Names in catalog order (for the dashboard / manifest). */
    public List<String> names() {
        return new ArrayList<>(byName.keySet());
    }

    /** AMBIENT-category beats (eligible for budget-driven synthesis). */
    public List<Beat> ambientBeats() {
        List<Beat> out = new ArrayList<>();
        for (Beat b : byName.values()) {
            if (b.category() == BeatCategory.AMBIENT) out.add(b);
        }
        return out;
    }

    /**
     * Pick a random AMBIENT beat (shuffled order) that {@code canEnact} for this request. MAIN
     * thread (canEnact touches Bukkit). Returns null if none can fire right now. Each candidate's
     * canEnact is Safety-wrapped so a misbehaving precheck can't break selection.
     */
    public Beat pickAmbient(BeatContext ctx, BeatRequest req) {
        List<Beat> candidates = ambientBeats();
        Collections.shuffle(candidates, new java.util.Random(ThreadLocalRandom.current().nextLong()));
        for (Beat b : candidates) {
            final Beat beat = b;
            boolean ok = safety.call("beat.lib.canEnact." + beat.name(),
                    () -> beat.canEnact(ctx, req), Boolean.FALSE);
            if (Boolean.TRUE.equals(ok)) return beat;
        }
        return null;
    }

    /** Clear in-process idempotency state across all beats (e.g. on reload). */
    public void clearAppliedState() {
        for (Beat b : byName.values()) {
            if (b instanceof AbstractBeat ab) ab.clearAppliedState();
        }
    }

    public int size() {
        return byName.size();
    }
}
