package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.npc.WrenNpc;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exact V5 Wren dialogue and one-once WR05 reckoning choice. */
public final class WrenNpcListener implements Listener {

    public static final String FLAG_CONDEMN = "reckoning_condemn";
    public static final String FLAG_UNDERSTAND = "reckoning_understand";
    public static final String FLAG_FREE = "reckoning_free";
    public static final String PDC_RECKONING = "wren_reckoning";

    private static final long OPEN_COOLDOWN_MS = 1_500L;
    private static final int LINE_DELAY_TICKS = 35;

    private final SupabaseClient supabase;
    private final WrenNpc wren;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final NamespacedKey reckoningKey;
    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private final AtomicBoolean choiceInFlight = new AtomicBoolean(false);
    private volatile Map<String, Object> arcFlags = Map.of();

    public WrenNpcListener(SupabaseClient supabase, WrenNpc wren, RateLimiter rateLimiter,
                           Scheduler scheduler, Safety safety, String namespace) {
        this.supabase = supabase;
        this.wren = wren;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        String ns = namespace == null || namespace.isBlank() ? "observance" : namespace;
        this.reckoningKey = new NamespacedKey(ns, PDC_RECKONING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("signal.wren.v5.interact", () -> {
            Player player = event.getPlayer();
            Entity clicked = event.getRightClicked();
            if (player == null || clicked == null) return;
            String choice = readReckoning(clicked);
            if (choice != null) {
                handleReckoning(player, choice);
                return;
            }
            if (wren == null || !wren.isWren(clicked)) return;
            if (rateLimiter == null || !rateLimiter.tryCooldown(
                    "wren:v5:" + player.getUniqueId(), OPEN_COOLDOWN_MS)) return;

            String state = dialogueState(arcFlags);
            V5DialogueCatalog.Npc npc = V5DialogueCatalog.wren();
            List<String> lines = npc.lines(state);
            if (lines.isEmpty()) lines = npc.lines("before_c07");
            speak(player, npc.displayName(), lines);
            if ("confession".equals(state) && !truthy(arcFlags.get("v5_wr03_confession"))) {
                mergeFlag("v5_wr03_confession");
            }
            refreshFlags();
        });
    }

    private String dialogueState(Map<String, Object> flags) {
        String choice = selectedChoice(flags);
        if (truthy(flags.get("v5_case_c10_complete")) && choice != null) return "coda_" + choice;
        if (choice != null) return "reckoning_" + choice;
        if (truthy(flags.get("v5_wr02_index")) || truthy(flags.get("v5_wr03_confession"))) {
            return "confession";
        }
        if (truthy(flags.get("v5_wr01_quotes"))) return "evidence_names";
        if (truthy(flags.get("v5_case_c07_complete"))) return "evidence_bridge";
        return "before_c07";
    }

    private void handleReckoning(Player player, String choice) {
        if (supabase == null || scheduler == null || rateLimiter == null) return;
        if (!rateLimiter.tryCooldown("wren:v5:choice:" + player.getUniqueId(), OPEN_COOLDOWN_MS)) return;
        if (!choiceInFlight.compareAndSet(false, true)) {
            player.sendMessage(Component.text("another hand is already writing.", NamedTextColor.DARK_GRAY));
            return;
        }
        String flag = choiceFlag(choice);
        if (flag == null) {
            choiceInFlight.set(false);
            return;
        }
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        scheduler.runAsyncSafe("wren.v5.reckoning", () -> {
            try {
                var state = supabase.fetchArcState();
                Map<String, Object> flags = state.ok() && state.value() != null
                        ? state.value().flagsMap() : Map.of();
                if (!truthy(flags.get("v5_wr04_bridge"))) {
                    notify(uuid, "the Protocol Bridge has not reached this room.");
                    return;
                }
                if (selectedChoice(flags) != null) {
                    notify(uuid, "the reckoning is already entered.");
                    return;
                }
                JsonObject write = new JsonObject();
                write.addProperty(flag, true);
                write.addProperty("v5_wren_outcome", choice);
                write.addProperty("v5_case_c08_complete", true);
                var saved = supabase.mergeArcFlags(write);
                if (!saved.ok()) {
                    notify(uuid, "the reckoning did not persist. choose again when the record is reachable.");
                    return;
                }
                supabase.insertEventLog(new EventLogRow("companion", "reckoning." + choice,
                        name + " entered the V5 Wren reckoning: " + choice,
                        uuid.toString(), "{\"choice\":\"" + choice + "\"}",
                        SupabaseClient.timestampNow()));
                Map<String, Object> updated = new HashMap<>(flags);
                updated.put(flag, true);
                updated.put("v5_wren_outcome", choice);
                updated.put("v5_case_c08_complete", true);
                arcFlags = Collections.unmodifiableMap(updated);
                notify(uuid, V5DialogueCatalog.wren().lines("reckoning_" + choice).get(0));
            } finally {
                choiceInFlight.set(false);
            }
        });
    }

    private String readReckoning(Entity entity) {
        try {
            String value = entity.getPersistentDataContainer().get(reckoningKey, PersistentDataType.STRING);
            String choice = value == null ? null : value.trim().toLowerCase(Locale.ROOT);
            return choiceFlag(choice) == null ? null : choice;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String choiceFlag(String choice) {
        return switch (choice == null ? "" : choice) {
            case "condemn" -> FLAG_CONDEMN;
            case "understand" -> FLAG_UNDERSTAND;
            case "free" -> FLAG_FREE;
            default -> null;
        };
    }

    private static String selectedChoice(Map<String, Object> flags) {
        Object canonical = flags.get("v5_wren_outcome");
        if (canonical != null) {
            String value = canonical.toString().trim().toLowerCase(Locale.ROOT);
            if (choiceFlag(value) != null) return value;
        }
        if (truthy(flags.get(FLAG_CONDEMN))) return "condemn";
        if (truthy(flags.get(FLAG_UNDERSTAND))) return "understand";
        if (truthy(flags.get(FLAG_FREE))) return "free";
        return null;
    }

    private void speak(Player player, String displayName, List<String> lines) {
        UUID uuid = player.getUniqueId();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Runnable send = () -> {
                Player live = Bukkit.getPlayer(uuid);
                if (live == null || !live.isOnline()) return;
                live.sendMessage(Component.text(displayName, NamedTextColor.YELLOW));
                live.sendMessage(Component.text(line, NamedTextColor.WHITE));
            };
            if (i == 0) send.run();
            else scheduler.runLaterSafe("wren.v5.line", (long) i * LINE_DELAY_TICKS, send);
        }
    }

    private void notify(UUID uuid, String message) {
        scheduler.runMainSafe("wren.v5.notify", () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text(message, NamedTextColor.DARK_GRAY));
            }
        });
    }

    private void mergeFlag(String key) {
        if (supabase == null || scheduler == null) return;
        scheduler.runAsyncSafe("wren.v5.flag." + key, () -> {
            JsonObject write = new JsonObject();
            write.addProperty(key, true);
            var saved = supabase.mergeArcFlags(write);
            if (saved.ok()) {
                Map<String, Object> updated = new HashMap<>(arcFlags);
                updated.put(key, true);
                arcFlags = Collections.unmodifiableMap(updated);
            }
        });
    }

    private void refreshFlags() {
        if (supabase == null || scheduler == null || !refreshInFlight.compareAndSet(false, true)) return;
        scheduler.runAsyncSafe("wren.v5.flags", () -> {
            try {
                var state = supabase.fetchArcState();
                if (state.ok() && state.value() != null) {
                    arcFlags = Collections.unmodifiableMap(new HashMap<>(state.value().flagsMap()));
                }
            } finally {
                refreshInFlight.set(false);
            }
        });
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0.0;
        if (value == null) return false;
        return java.util.Set.of("true", "1", "yes", "on")
                .contains(value.toString().trim().toLowerCase(Locale.ROOT));
    }
}
