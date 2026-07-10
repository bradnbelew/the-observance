package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.npc.WrenNpc;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE COMPANION FLAG PRODUCERS (the missing piece — audit #5). A player right-clicks Wren's body →
 * this drives his arc forward, idempotently, via {@code arc_state.flags}:
 *
 * <ul>
 *   <li>{@code companion_introduced} — set true on the FIRST interaction with Wren.</li>
 *   <li>{@code companion_trust} (int) — incremented each interaction, hard-capped at {@link #TRUST_CAP}.
 *       Read-then-merge (the RPC merge is shallow, so we compute the next value from the current
 *       {@code arc_state} and write it back). Group-scoped: trust is one shared number.</li>
 *   <li>{@code reckoning_condemn | reckoning_understand | reckoning_free} — a right-click on one of the
 *       three PDC-tagged reckoning-choice markers ({@link com.observance.watcher.npc.WrenNpc} stamps
 *       them via {@code /observance wren reckoning}) sets EXACTLY ONE, ONCE, and only after
 *       {@code companion_revealed}. Choosing again is a no-op (a decision, once entered into the record,
 *       is final).</li>
 * </ul>
 *
 * <p><b>Dialogue delivery — reuse the Keeper path.</b> Wren's SEEDED lines are the showrunner's to
 * bind (exactly like the Keeper's — no story in the engine, INV-1). So a valid Wren open ALSO posts one
 * {@code event_log} row (type {@code companion}, context {@code npc.open}) that the showrunner reads to
 * resolve which {@code companion.*} node opens and enqueue the {@link
 * com.observance.watcher.beats.lib.KeeperNpcBeat} with the bound lines — the same NPC-dialogue delivery
 * path the Keeper uses. To guarantee Wren *speaks in-world even before that DB branch is wired*, this
 * listener ALSO speaks a restrained built-in companion line to the interacting player (private chat,
 * per-player, reveal-trivially-safe). These fallback lines are launch-grade companion beats; the
 * showrunner's bound lines remain the richer canonical branch when available.
 *
 * <p>Mirrors {@link KeeperNpcListener} / {@link IgnitionListener}: Safety-wrapped body, MONITOR
 * priority, a {@link RateLimiter} guard, all writes hopped ASYNC, silent on any DB failure. Never
 * cancels the event, never mutates the world.
 */
public final class WrenNpcListener implements Listener {

    public static final String FLAG_INTRODUCED = "companion_introduced";
    public static final String FLAG_TRUST      = "companion_trust";
    public static final String FLAG_REVEALED   = "companion_revealed";
    public static final String FLAG_CONDEMN    = "reckoning_condemn";
    public static final String FLAG_UNDERSTAND = "reckoning_understand";
    public static final String FLAG_FREE       = "reckoning_free";

    /** Hard cap on trust so repeated clicking can't run it away. */
    private static final int TRUST_CAP = 10;

    /** Per-player interaction cooldown (also long enough for the async round-trip). */
    private static final long OPEN_COOLDOWN_MS = 4_000L;

    /** Cadence between name attribution and the fallback line (matches townsfolk / Keeper feel). */
    private static final int LINE_DELAY_TICKS = 35;

    /**
     * PDC sub-key marking a reckoning-choice marker entity; its STRING value is one of
     * {@code condemn|understand|free}.
     */
    public static final String PDC_RECKONING = "wren_reckoning";

    /** Warm, present-tense companion beats so Wren speaks even pre-showrunner-binding. */
    private static final List<String> INTRO_LINES = List.of(
            "oh - hey. you're new. you're breathing, anyway. sorry, you get careful about that down here. stay close, would you. i lost people going off alone.",
            "not that way. i mean it - not that way. i know it looks like the easy road. the easy road down here is how you lose someone.",
            "here. take it, it's nothing, it's just a spare - you'll want it before i will. no, keep it.",
            "tell me where you're headed and i'll tell you what i know. what you'd never do, where you'd run if it got bad. that's the trade. it's a good trade.",
            "i stepped out for a second. sorry. thought i heard something in the dark and went to check and it was nothing. it's always nothing. i hate not being just behind you.");

    /** Per-player fallback speech cursor so Wren advances instead of repeating a packet every click. */
    private final Map<UUID, Integer> speechCursors = new ConcurrentHashMap<>();

    private final SupabaseClient supabase;
    private final WrenNpc wren;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final NamespacedKey reckoningKey;

    public WrenNpcListener(SupabaseClient supabase, WrenNpc wren, RateLimiter rateLimiter,
                           Scheduler scheduler, Safety safety, String namespace) {
        this.supabase = supabase;
        this.wren = wren;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.reckoningKey = new NamespacedKey(ns, PDC_RECKONING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("signal.Wren.interact", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            Entity clicked = event.getRightClicked();
            if (clicked == null) return;

            // Reckoning marker takes precedence (a marker is not Wren himself).
            String choice = readReckoning(clicked);
            if (choice != null) {
                handleReckoning(p, choice);
                return;
            }

            if (wren == null || !wren.isWren(clicked)) return;   // not Wren — ignore silently

            String cdKey = "wrenopen:" + p.getUniqueId();
            if (!rateLimiter.tryCooldown(cdKey, OPEN_COOLDOWN_MS)) return;

            // Speak in-world immediately (restrained fallback); the showrunner can still deliver richer
            // bound companion lines from the event_log signal.
            speakBuiltIn(p);

            // Advance the arc + signal the showrunner (async, fault-isolated).
            advanceTrust(p);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Introduced + trust                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Read current arc_state, compute {@code companion_introduced=true} and the next capped
     * {@code companion_trust}, and merge them in one shallow RPC write. Also posts the open signal for
     * the showrunner. Idempotent at the cap (trust stops climbing; the merge is a cheap no-op-ish write).
     */
    private void advanceTrust(Player p) {
        if (supabase == null) return;
        final String name = p.getName();
        final String uuid = p.getUniqueId().toString();

        scheduler.runAsyncSafe("signal.wren.advance", () -> {
            int current = readTrust();
            int next = Math.min(TRUST_CAP, current + 1);

            JsonObject flags = new JsonObject();
            flags.addProperty(FLAG_INTRODUCED, true);
            flags.addProperty(FLAG_TRUST, next);
            supabase.mergeArcFlags(flags);

            // Signal the showrunner to resolve + enqueue Wren's bound dialogue node (the Keeper path).
            String detail = "{\"surface\":\"companion_open\",\"trust\":" + next + "}";
            supabase.insertEventLog(new EventLogRow(
                    "companion", "npc.open",
                    name + " spoke with Wren (trust=" + next + ")",
                    uuid, detail, SupabaseClient.timestampNow()));

            safety.info("companion.trust", name + " → companion_trust=" + next);
        });
    }

    /** Current {@code companion_trust} as an int (0 if unset / unreadable). Async-safe (DB read). */
    private int readTrust() {
        try {
            var r = supabase.fetchArcState();
            if (r == null || !r.ok() || r.value() == null) return 0;
            ArcStateRow row = r.value();
            Map<String, Object> flags = row.flagsMap();
            Object v = flags.get(FLAG_TRUST);
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) {
                try { return (int) Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) { return 0; }
            }
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Reckoning                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Enter EXACTLY ONE reckoning line into the record — but only once the reveal has happened, and only
     * if no reckoning flag is already set. All checks are done against a fresh arc_state read so the
     * "one, once" contract holds across restarts and multiple players.
     */
    private void handleReckoning(Player p, String choice) {
        if (supabase == null) return;
        String flagKey = switch (choice) {
            case "condemn" -> FLAG_CONDEMN;
            case "understand" -> FLAG_UNDERSTAND;
            case "free" -> FLAG_FREE;
            default -> null;
        };
        if (flagKey == null) return;

        String cdKey = "wrenreckon:" + p.getUniqueId();
        if (!rateLimiter.tryCooldown(cdKey, OPEN_COOLDOWN_MS)) return;

        final String name = p.getName();
        final String uuid = p.getUniqueId().toString();

        scheduler.runAsyncSafe("signal.wren.reckoning", () -> {
            Map<String, Object> flags = readFlags();
            boolean revealed = truthy(flags.get(FLAG_REVEALED));
            if (!revealed) {
                // The reckoning is not available until the reveal; a marker touched early does nothing.
                notify(p, Component.text("the stone is cold. it is not time.", NamedTextColor.DARK_GRAY));
                return;
            }
            boolean alreadyChosen = truthy(flags.get(FLAG_CONDEMN))
                    || truthy(flags.get(FLAG_UNDERSTAND))
                    || truthy(flags.get(FLAG_FREE));
            if (alreadyChosen) {
                notify(p, Component.text("the record is already written. it does not take a second hand.",
                        NamedTextColor.DARK_GRAY));
                return;
            }

            JsonObject write = new JsonObject();
            write.addProperty(flagKey, true);
            supabase.mergeArcFlags(write);

            supabase.insertEventLog(new EventLogRow(
                    "companion", "reckoning." + choice,
                    name + " entered the reckoning: " + choice,
                    uuid, "{\"choice\":\"" + choice + "\"}", SupabaseClient.timestampNow()));

            safety.info("companion.reckoning", name + " chose reckoning=" + choice);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Built-in in-world speech (KeeperNpc cadence)                        */
    /* ------------------------------------------------------------------ */

    /** Speak the next built-in warm line to one player on the speech cadence (private chat). */
    private void speakBuiltIn(Player p) {
        final UUID id = p.getUniqueId();
        int idx = speechCursors.merge(id, 1, Integer::sum) - 1;
        final String line = INTRO_LINES.get(Math.floorMod(idx, INTRO_LINES.size()));
        scheduler.runLaterSafe("signal.wren.name", 0, () -> {
            Player pl = org.bukkit.Bukkit.getPlayer(id);
            if (pl == null || !pl.isOnline()) return;
            pl.sendMessage(Component.text(WrenNpc.DISPLAY_NAME, NamedTextColor.YELLOW));
        });
        scheduler.runLaterSafe("signal.wren.line", LINE_DELAY_TICKS, () -> {
            Player pl = org.bukkit.Bukkit.getPlayer(id);
            if (pl == null || !pl.isOnline()) return;   // logout mid-speech -> just ends
            pl.sendMessage(Component.text(line, NamedTextColor.GRAY));
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /** Read the reckoning-choice value off a clicked entity, or null if it isn't a marker. */
    private String readReckoning(Entity e) {
        try {
            var pdc = e.getPersistentDataContainer();
            if (!pdc.has(reckoningKey, PersistentDataType.STRING)) return null;
            String v = pdc.get(reckoningKey, PersistentDataType.STRING);
            return v == null ? null : v.trim().toLowerCase(java.util.Locale.ROOT);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Read the full arc flags map (empty on any failure). Async-safe. */
    private Map<String, Object> readFlags() {
        try {
            var r = supabase.fetchArcState();
            if (r == null || !r.ok() || r.value() == null) return java.util.Collections.emptyMap();
            return r.value().flagsMap();
        } catch (Throwable t) {
            return java.util.Collections.emptyMap();
        }
    }

    /** A tiny truthiness helper matching how the arc gate reads a flag. */
    static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) {
            String t = s.trim();
            return t.equalsIgnoreCase("true") || t.equals("1");
        }
        return false;
    }

    /** Hop a chat line back to the main thread for a player (async caller). Null-safe. */
    private void notify(Player p, Component msg) {
        final java.util.UUID id = p.getUniqueId();
        scheduler.runMainSafe("signal.wren.notify", () -> {
            Player pl = org.bukkit.Bukkit.getPlayer(id);
            if (pl != null && pl.isOnline()) pl.sendMessage(msg);
        });
    }
}
