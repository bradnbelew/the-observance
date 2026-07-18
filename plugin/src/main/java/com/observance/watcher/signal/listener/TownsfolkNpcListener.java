package com.observance.watcher.signal.listener;

import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.npc.TownsfolkNpc;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * V5 surface-town dialogue delivery.
 *
 * <p>Every spoken byte comes from the packaged {@code arc/v5/npc-dialogue.json} authority. Replies
 * are selected from a locally cached durable-flag snapshot and begin synchronously on interaction;
 * no showrunner round trip, legacy errand, conduct skin, or six-plus-one line remains reachable.
 */
public final class TownsfolkNpcListener implements Listener {

    private static final long OPEN_COOLDOWN_MS = 1_500L;
    private static final int LINE_DELAY_TICKS = 32;
    private static final List<String> KEEPER_AFFIDAVITS = List.of(
            "v5_kv03_affidavit", "v5_km03_affidavit", "v5_ks03_affidavit",
            "v5_ko03_affidavit", "v5_kb03_affidavit", "v5_ki03_affidavit");

    private final TownsfolkNpc townsfolk;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final SupabaseClient supabase;
    private final Supplier<Map<String, Object>> localFacts;
    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private final Map<String, Integer> introCursors = new ConcurrentHashMap<>();
    private volatile Map<String, Object> arcFlags = Map.of();

    /** Signature retained for binary/config compatibility; V5 intentionally retires legacy quests. */
    public TownsfolkNpcListener(TownsfolkNpc townsfolk, SignalTracker signals, RateLimiter rateLimiter,
                                Scheduler scheduler, Safety safety,
                                SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                                BooleanSupplier issCaught) {
        this(townsfolk, signals, rateLimiter, scheduler, safety, supabase,
                sitesSupplier, issCaught, Map::of);
    }

    public TownsfolkNpcListener(TownsfolkNpc townsfolk, SignalTracker signals, RateLimiter rateLimiter,
                                Scheduler scheduler, Safety safety,
                                SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                                BooleanSupplier issCaught,
                                Supplier<Map<String, Object>> localFacts) {
        this.townsfolk = townsfolk;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.supabase = supabase;
        this.localFacts = localFacts == null ? Map::of : localFacts;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("signal.townsfolk.v5.interact", () -> {
            Player player = event.getPlayer();
            Entity clicked = event.getRightClicked();
            if (player == null || clicked == null || townsfolk == null) return;
            String rawId = townsfolk.idOf(clicked);
            if (rawId == null) return;
            V5DialogueCatalog.Npc npc = V5DialogueCatalog.townsperson(rawId);
            if (npc == null) return;
            String cooldown = "townsfolk:v5:" + player.getUniqueId() + ":" + npc.id();
            if (rateLimiter == null || !rateLimiter.tryCooldown(cooldown, OPEN_COOLDOWN_MS)) return;

            String state = selectState(npc.id(), player.getUniqueId(), mergedFacts());
            List<String> lines = npc.lines(state);
            if (lines.isEmpty()) lines = npc.lines("arrival");
            speak(player, npc.displayName(), lines);
            refreshFlags();
        });
    }

    /** Existing plugin timer entry point; V5 uses it only to refresh the local durable flag snapshot. */
    public void completionTick() {
        refreshFlags();
    }

    private String selectState(String id, UUID player, Map<String, Object> flags) {
        if (truthy(flags.get("v5_case_c10_complete"))) return "coda";
        return switch (id) {
            case "aro" -> {
                if (truthy(flags.get("v5_case_c07_complete"))) yield "after_c07";
                if (truthy(flags.get("p7.nessa_publicly_cleared"))
                        || truthy(flags.get("v5_case_c04_complete"))) yield "after_c04";
                if (truthy(flags.get("p5.civic_gallery_recurated"))) yield "after_p5";
                if (truthy(flags.get("v5_case_c02_complete"))) yield "after_c02";
                yield introState(player, id, "mouth_lead");
            }
            case "wenna" -> {
                if (truthy(flags.get("v5_case_c05_complete"))) yield "after_c05";
                if (truthy(flags.get("p5.civic_gallery_recurated"))) yield "after_p5";
                if (keepersComplete(flags)) yield "after_c03";
                yield introState(player, id, "well_lead");
            }
            case "coll" -> {
                if (truthy(flags.get("v5_case_c07_complete"))) yield "after_c07";
                if (truthy(flags.get("v5_case_c06_complete"))) yield "camp_lead";
                if (truthy(flags.get("p5.civic_gallery_recurated"))) yield "after_p5";
                if (truthy(flags.get("v5_case_c01_complete"))) yield "after_c01";
                yield "arrival";
            }
            case "dob" -> {
                if (truthy(flags.get("v5_case_c05_complete"))) yield "after_c05";
                if (truthy(flags.get("p5.civic_gallery_recurated"))) yield "after_p5";
                if (truthy(flags.get("v5_case_c02_complete"))) yield "after_c02";
                yield introState(player, id, "mouth_lead");
            }
            case "old_pell" -> {
                if (truthy(flags.get("v5_case_c09_complete"))) yield "after_c09";
                if (truthy(flags.get("p7.nessa_publicly_cleared"))
                        || truthy(flags.get("v5_case_c04_complete"))) yield "after_c04";
                if (keepersComplete(flags)) yield "cistern_lead";
                yield "arrival";
            }
            default -> "arrival";
        };
    }

    private String introState(UUID player, String id, String lead) {
        String key = player + ":" + id;
        int visits = introCursors.merge(key, 1, Integer::sum);
        return visits == 1 ? "arrival" : lead;
    }

    private boolean keepersComplete(Map<String, Object> flags) {
        return KEEPER_AFFIDAVITS.stream().allMatch(flag -> truthy(flags.get(flag)));
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
            else scheduler.runLaterSafe("townsfolk.v5.line", (long) i * LINE_DELAY_TICKS, send);
        }
    }

    private void refreshFlags() {
        if (supabase == null || scheduler == null || !refreshInFlight.compareAndSet(false, true)) return;
        scheduler.runAsyncSafe("townsfolk.v5.flags", () -> {
            try {
                var result = supabase.fetchArcState();
                if (result.ok() && result.value() != null) {
                    arcFlags = Collections.unmodifiableMap(new HashMap<>(result.value().flagsMap()));
                }
            } finally {
                refreshInFlight.set(false);
            }
        });
    }

    /** Remote may add true facts, but the durable local image always wins and never regresses. */
    private Map<String, Object> mergedFacts() {
        Map<String, Object> merged = new HashMap<>();
        arcFlags.forEach((key, value) -> {
            if (truthy(value)) merged.put(key, Boolean.TRUE);
        });
        Map<String, Object> local = localFacts.get();
        if (local != null) local.forEach((key, value) -> {
            if (truthy(value) || value instanceof String) merged.put(key, value);
        });
        return Collections.unmodifiableMap(merged);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0.0;
        return value != null && SetLike.TRUE.contains(value.toString().trim().toLowerCase(java.util.Locale.ROOT));
    }

    private static final class SetLike {
        private static final java.util.Set<String> TRUE = java.util.Set.of("true", "1", "yes", "on");
        private SetLike() { }
    }
}
