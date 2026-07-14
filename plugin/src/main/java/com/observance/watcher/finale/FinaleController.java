package com.observance.watcher.finale;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.structure.DeepHoldV4Plan;
import com.observance.watcher.util.Safety;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Bukkit adapter for the durable, locally controlled V5 finale state machine. */
public final class FinaleController implements Listener {

    public record Result(boolean ok, String message) { }
    private record SneakHold(long startedAt, UUID world, int x, int y, int z) { }

    public static final String PDC_FINALE_CONTROL = "v5_finale_control";
    public static final String CONTROL_SEVER = "sever_record";

    private static final String FILE_NAME = "finale-state.properties";
    private static final long DEFAULT_WINDOW_SECONDS = 120L;
    private static final long REQUIRED_SNEAK_MS = 3_000L;
    private static final long GOODBYE_TICKS = 140L;

    private final ObservancePlugin plugin;
    private final Safety safety;
    private final NamespacedKey finaleControlKey;
    private final Map<UUID, SneakHold> sneakHolds = new HashMap<>();
    private FinaleStateMachine machine = new FinaleStateMachine();
    private BukkitTask armExpiry;

    public FinaleController(ObservancePlugin plugin, Safety safety) {
        this.plugin = plugin;
        this.safety = safety;
        this.finaleControlKey = new NamespacedKey(plugin, PDC_FINALE_CONTROL);
    }

    /** Load durable state. COMMITTED promotes to terminal CODA without replaying theater. */
    public synchronized void load() {
        FinaleStateMachine.Snapshot restored = readSnapshot();
        machine = new FinaleStateMachine(restored);
        if (restored.phase() == FinaleStateMachine.Phase.COMMITTED) {
            machine.enterCoda();
            if (!persist(machine.snapshot())) {
                plugin.getLogger().severe("[finale] Could not promote COMMITTED to CODA on disk; "
                        + "the durable committed record still prevents replay.");
            }
        }
        if (machine.snapshot().phase() == FinaleStateMachine.Phase.ARMED) scheduleArmExpiry();
        if (isCodaMode()) {
            FinaleStateMachine.Snapshot state = machine.snapshot();
            plugin.getLogger().warning("[finale] Persistent " + state.phase().name()
                    + " mode active (wren=" + enumName(state.wrenOutcome())
                    + ", name=" + enumName(state.nameTreatment()) + "). Story schedulers remain asleep.");
        }
    }

    /** Arm with choices already made by players; this method has no branch override parameter. */
    public synchronized Result arm(FinaleStateMachine.WrenOutcome wrenOutcome,
                                   FinaleStateMachine.NameTreatment nameTreatment,
                                   FinaleStateMachine.ConductVerdict conductVerdict,
                                   String actor, long windowSeconds) {
        FinaleStateMachine.Snapshot before = machine.snapshot();
        long seconds = windowSeconds <= 0L ? DEFAULT_WINDOW_SECONDS
                : Math.max(15L, Math.min(600L, windowSeconds));
        try {
            FinaleStateMachine.Snapshot armed = machine.arm(wrenOutcome, nameTreatment,
                    conductVerdict, actor, System.currentTimeMillis(), seconds * 1_000L);
            if (!persist(armed)) {
                machine = new FinaleStateMachine(before);
                return new Result(false, "Finale was NOT armed: local branch state could not be saved.");
            }
            scheduleArmExpiry();
            return new Result(true, "Finale armed for the recorded " + enumName(wrenOutcome)
                    + " / " + enumName(nameTreatment) + " outcome. A player must hold the release cell "
                    + "for three seconds and operate SEVER RECORD within " + seconds
                    + " seconds; no automatic commit will occur.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return new Result(false, "Finale not armed: " + ex.getMessage());
        }
    }

    public synchronized Result cancel() {
        FinaleStateMachine.Snapshot before = machine.snapshot();
        try {
            FinaleStateMachine.Snapshot idle = machine.cancel();
            if (!persist(idle)) {
                machine = new FinaleStateMachine(before);
                return new Result(false, "Finale remains armed: cancellation could not be saved durably.");
            }
            cancelArmExpiry();
            sneakHolds.clear();
            return new Result(true, "Finale arm cancelled before player commitment.");
        } catch (IllegalStateException ex) {
            return new Result(false, "Finale not cancelled: " + ex.getMessage());
        }
    }

    public synchronized String status() {
        FinaleStateMachine.Snapshot state = machine.snapshot();
        long remaining = state.phase() == FinaleStateMachine.Phase.ARMED
                ? Math.max(0L, (state.cancelCutoffAtEpochMs() - System.currentTimeMillis() + 999L) / 1_000L)
                : 0L;
        return "phase=" + state.phase().name().toLowerCase(Locale.ROOT)
                + ", wren=" + enumName(state.wrenOutcome())
                + ", name=" + enumName(state.nameTreatment())
                + ", conduct=" + enumName(state.conductVerdict())
                + ", armedBy=" + (state.armedBy().isBlank() ? "-" : state.armedBy())
                + (state.phase() == FinaleStateMachine.Phase.ARMED
                    ? ", confirmWithin=" + remaining + "s, awaiting=player_sever_control" : "")
                + ", durableFile=" + stateFile().getAbsolutePath();
    }

    public synchronized boolean isCodaMode() {
        FinaleStateMachine.Phase phase = machine.snapshot().phase();
        return phase == FinaleStateMachine.Phase.CODA || phase == FinaleStateMachine.Phase.FAULT;
    }

    public synchronized FinaleStateMachine.Snapshot snapshot() {
        return machine.snapshot();
    }

    public synchronized void stop() {
        cancelArmExpiry();
        sneakHolds.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        synchronized (this) {
            if (!event.isSneaking() || machine.snapshot().phase() != FinaleStateMachine.Phase.ARMED
                    || !insideConfirmCell(player.getLocation())) {
                sneakHolds.remove(player.getUniqueId());
                return;
            }
            Location at = player.getLocation();
            sneakHolds.put(player.getUniqueId(), new SneakHold(System.currentTimeMillis(),
                    at.getWorld().getUID(), at.getBlockX(), at.getBlockY(), at.getBlockZ()));
            player.sendActionBar(Component.text("hold the release cell; operate SEVER RECORD after three seconds",
                    NamedTextColor.DARK_RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSever(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (player == null || entity == null) return;
        String control = entity.getPersistentDataContainer().get(finaleControlKey, PersistentDataType.STRING);
        if (!CONTROL_SEVER.equals(control)) return;
        event.setCancelled(true);
        Result result = confirm(player);
        player.sendMessage(Component.text(result.message(), result.ok()
                ? NamedTextColor.DARK_RED : NamedTextColor.RED));
    }

    @EventHandler
    public synchronized void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != null) sneakHolds.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (player == null || to == null) return;
        synchronized (this) {
            if (machine.snapshot().phase() != FinaleStateMachine.Phase.ARMED
                    || !player.isSneaking() || !insideConfirmCell(to)) {
                sneakHolds.remove(player.getUniqueId());
                return;
            }
            SneakHold existing = sneakHolds.get(player.getUniqueId());
            if (existing == null || to.getWorld() == null
                    || !existing.world().equals(to.getWorld().getUID())
                    || existing.x() != to.getBlockX() || existing.y() != to.getBlockY()
                    || existing.z() != to.getBlockZ()) {
                sneakHolds.put(player.getUniqueId(), new SneakHold(System.currentTimeMillis(),
                        to.getWorld().getUID(), to.getBlockX(), to.getBlockY(), to.getBlockZ()));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isCodaMode() || event.getPlayer() == null) return;
        Player player = event.getPlayer();
        FinaleStateMachine.Snapshot state = snapshot();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (state.phase() == FinaleStateMachine.Phase.FAULT) {
                player.sendMessage(Component.text("The finale record is in a maintenance lock. "
                        + "An operator must inspect finale-state.properties.", NamedTextColor.RED));
                return;
            }
            java.util.List<String> recoveryIssues = plugin.applyFinaleCodaWorldState(state);
            for (String issue : recoveryIssues) {
                plugin.getLogger().severe("[finale] Coda world recovery: " + issue);
            }
            if (recoveryIssues.isEmpty()) mirrorCommittedState(state, true);
            String coda = FinaleStateMachine.coda(state.wrenOutcome(), state.nameTreatment(),
                    state.conductVerdict());
            player.showTitle(Title.title(Component.text("CODA", NamedTextColor.DARK_GRAY),
                    Component.text("the record remains closed", NamedTextColor.GRAY)));
            sendMultiline(player, FinaleStateMachine.goodbye(state.wrenOutcome(),
                    state.nameTreatment(), state.conductVerdict()), NamedTextColor.GRAY);
            player.sendMessage(Component.text(coda, NamedTextColor.DARK_GRAY));
        }, 20L);
    }

    private synchronized Result confirm(Player player) {
        FinaleStateMachine.Snapshot before = machine.snapshot();
        if (before.phase() != FinaleStateMachine.Phase.ARMED) {
            return new Result(false, "SEVER RECORD is not armed. No state changed.");
        }
        Location at = player.getLocation();
        SneakHold hold = sneakHolds.get(player.getUniqueId());
        boolean sameCell = hold != null && at.getWorld() != null
                && hold.world().equals(at.getWorld().getUID())
                && hold.x() == at.getBlockX() && hold.y() == at.getBlockY()
                && hold.z() == at.getBlockZ();
        if (!player.isSneaking() || !insideConfirmCell(at) || !sameCell
                || System.currentTimeMillis() - hold.startedAt() < REQUIRED_SNEAK_MS) {
            return new Result(false, "Hold still while sneaking in the marked release cell for three seconds, "
                    + "then operate SEVER RECORD. No state changed.");
        }

        FinaleStateMachine.Snapshot committed;
        try {
            committed = machine.commit(System.currentTimeMillis());
        } catch (IllegalStateException expired) {
            machine = new FinaleStateMachine(before);
            return new Result(false, "Finale did not commit: " + expired.getMessage() + ". Re-arm safely.");
        }
        // Hard boundary: no title, sound, world mutation, reward, kick, or shutdown occurs first.
        if (!persist(committed)) {
            machine = new FinaleStateMachine(before);
            return new Result(false, "Finale did not commit: durable local save failed. Theater was not started.");
        }
        cancelArmExpiry();
        sneakHolds.clear();
        mirrorCommittedState(committed, false);
        Bukkit.getScheduler().runTask(plugin, () -> playTheater(committed));
        return new Result(true, "The severance is durable. The Record is closing.");
    }

    private synchronized void scheduleArmExpiry() {
        cancelArmExpiry();
        FinaleStateMachine.Snapshot state = machine.snapshot();
        if (state.phase() != FinaleStateMachine.Phase.ARMED) return;
        long waitMs = Math.max(0L, state.cancelCutoffAtEpochMs() - System.currentTimeMillis());
        long ticks = Math.max(1L, (waitMs + 49L) / 50L);
        armExpiry = Bukkit.getScheduler().runTaskLater(plugin, this::expireArm, ticks);
    }

    private synchronized void expireArm() {
        armExpiry = null;
        if (machine.snapshot().phase() != FinaleStateMachine.Phase.ARMED) return;
        FinaleStateMachine.Snapshot before = machine.snapshot();
        try {
            FinaleStateMachine.Snapshot idle = machine.cancel();
            if (!persist(idle)) {
                machine = new FinaleStateMachine(before);
                plugin.getLogger().severe("[finale] Arm expired but IDLE could not be persisted; "
                        + "remaining fail-closed in ARMED without theater.");
                // The cutoff is already in the past. Retry persistence slowly; never enter a
                // one-tick reschedule loop and never auto-commit.
                armExpiry = Bukkit.getScheduler().runTaskLater(plugin, this::expireArm, 100L);
                return;
            }
            sneakHolds.clear();
            Bukkit.broadcast(Component.text("The severance arm expired. The Record remains open.",
                    NamedTextColor.DARK_GRAY));
        } catch (IllegalStateException ignored) { }
    }

    private void playTheater(FinaleStateMachine.Snapshot state) {
        plugin.getLogger().warning("[finale] COMMITTED wren=" + enumName(state.wrenOutcome())
                + ", name=" + enumName(state.nameTreatment()) + "; starting local closing theater.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(Component.text("THE RECORD CLOSES", NamedTextColor.DARK_RED),
                    Component.text("the upper lamps are going dark", NamedTextColor.GRAY)));
            try {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.DARKNESS, 220, 0, true, false, false));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.45f);
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.55f);
                player.playSound(player.getLocation(), "observance:drone_low",
                        org.bukkit.SoundCategory.AMBIENT, 0.7f, 0.72f);
            } catch (Throwable effectFailure) {
                plugin.getLogger().warning("[finale] Sensory effect failed for " + player.getName()
                        + "; durable theater continues: " + effectFailure.getClass().getSimpleName());
            }
        }
        extinguishFixtureBand(-40);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            extinguishFixtureBand(-68);
            Bukkit.broadcast(Component.text("[RECORD: INPUT FIELDS RETURNED]", NamedTextColor.DARK_GRAY));
        }, 25L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            extinguishFixtureBand(-96);
            Bukkit.broadcast(Component.text("[RECORD: RETENTION ENDED]", NamedTextColor.DARK_GRAY));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), "observance:cold_toll",
                        org.bukkit.SoundCategory.MASTER, 0.9f, 0.7f);
            }
        }, 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String goodbye = FinaleStateMachine.goodbye(state.wrenOutcome(), state.nameTreatment(),
                    state.conductVerdict());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(Component.text("AVERYN", NamedTextColor.GRAY),
                        Component.text("the record is closed", NamedTextColor.WHITE)));
                sendMultiline(player, goodbye, NamedTextColor.GRAY);
            }
            Bukkit.getConsoleSender().sendMessage("[Observance finale]\n" + goodbye);
        }, 80L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> finishAndShutdown(state), GOODBYE_TICKS);
    }

    private void finishAndShutdown(FinaleStateMachine.Snapshot committed) {
        FinaleStateMachine.Snapshot coda;
        boolean codaDurable = false;
        synchronized (this) {
            try {
                coda = machine.enterCoda();
                if (!persist(coda)) {
                    plugin.getLogger().severe("[finale] CODA save failed; retrying safely in five seconds. "
                            + "No shutdown or C10 mirror occurred.");
                } else {
                    codaDurable = true;
                }
            } catch (IllegalStateException ex) {
                plugin.getLogger().severe("[finale] Could not enter CODA: " + ex.getMessage());
                coda = committed;
            }
        }
        if (!codaDurable) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> finishAndShutdown(committed), 100L);
            return;
        }
        java.util.List<String> worldIssues = plugin.applyFinaleCodaWorldState(coda);
        if (!worldIssues.isEmpty()) {
            plugin.getLogger().severe("[finale] CODA world projection incomplete: "
                    + String.join("; ", worldIssues) + ". Retrying before shutdown/C10 mirror.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> finishAndShutdown(committed), 100L);
            return;
        }
        mirrorCommittedState(coda, true);
        try {
            plugin.getServer().savePlayers();
            for (World world : plugin.getServer().getWorlds()) world.save();
        } catch (Throwable saveFailure) {
            plugin.getLogger().severe("[finale] World save reported "
                    + saveFailure.getClass().getSimpleName() + ": " + saveFailure.getMessage()
                    + "; terminal state is already durable.");
        }
        String goodbye = FinaleStateMachine.goodbye(committed.wrenOutcome(), committed.nameTreatment(),
                committed.conductVerdict());
        Component kick = Component.text(goodbye + "\n\nThe server is entering Coda.", NamedTextColor.GRAY);
        for (Player player : Bukkit.getOnlinePlayers()) player.kick(kick);
        boolean shutdown = plugin.getConfig().getBoolean("finale.production-shutdown", false);
        plugin.getLogger().warning("[finale] Players released into persistent CODA (shutdown=" + shutdown + ").");
        if (shutdown) plugin.getServer().shutdown();
    }

    private void mirrorCommittedState(FinaleStateMachine.Snapshot state, boolean complete) {
        if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
        JsonObject flags = new JsonObject();
        flags.addProperty("v5_wren_outcome", enumName(state.wrenOutcome()));
        flags.addProperty("v5_name_treatment", enumName(state.nameTreatment()));
        flags.addProperty("v5_conduct_verdict", enumName(state.conductVerdict()));
        flags.addProperty("v5_rp05_severed", true);
        if (complete) {
            flags.addProperty("v5_case_c10_complete", true);
            flags.addProperty(state.nameTreatment() == FinaleStateMachine.NameTreatment.PUBLISH
                    ? "v5_coda_publish" : "v5_coda_unfiled", true);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { plugin.supabase().mergeArcFlags(flags); }
            catch (Throwable failure) {
                plugin.getLogger().warning("[finale] Async remote mirror delayed; local finale remains authoritative.");
            }
        });
    }

    private boolean insideConfirmCell(Location at) {
        if (at == null || at.getWorld() == null || plugin.sites() == null) return false;
        Site site = plugin.sites().get("release_record");
        Location control = site == null ? null : site.location();
        if (control == null || control.getWorld() != at.getWorld()) return false;
        // Canonical +Z Hold orientation: the marked confirmation cell is two blocks beyond control.
        Location cell = control.clone().add(0, 0, 2);
        return Math.abs(at.getX() - (cell.getBlockX() + 0.5)) <= 1.1
                && Math.abs(at.getY() - cell.getY()) <= 1.5
                && Math.abs(at.getZ() - (cell.getBlockZ() + 0.5)) <= 1.1;
    }

    /** Extinguish only Lightable fixture blocks; never remove or replace structural blocks. */
    private void extinguishFixtureBand(int localY) {
        if (plugin.sites() == null) return;
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            if (fixture.y() != localY) continue;
            Site site = plugin.sites().get(fixture.id());
            Location center = site == null ? null : site.location();
            if (center == null || center.getWorld() == null) continue;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -1; dy <= 5; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        Block block = center.getWorld().getBlockAt(center.getBlockX() + dx,
                                center.getBlockY() + dy, center.getBlockZ() + dz);
                        if (block.getBlockData() instanceof Lightable lightable && lightable.isLit()) {
                            lightable.setLit(false);
                            block.setBlockData(lightable, false);
                        }
                    }
                }
            }
        }
    }

    private synchronized void cancelArmExpiry() {
        if (armExpiry != null) {
            armExpiry.cancel();
            armExpiry = null;
        }
    }

    private FinaleStateMachine.Snapshot readSnapshot() {
        File file = stateFile();
        if (!file.exists()) return new FinaleStateMachine().snapshot();
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            properties.load(in);
            if (!"2".equals(properties.getProperty("schema", ""))) {
                throw new IllegalStateException("unsupported/incomplete schema; V5 requires schema 2");
            }
            FinaleStateMachine.Phase phase = FinaleStateMachine.Phase.valueOf(
                    properties.getProperty("phase", "IDLE").trim().toUpperCase(Locale.ROOT));
            if (phase == FinaleStateMachine.Phase.IDLE) return new FinaleStateMachine().snapshot();
            if (phase == FinaleStateMachine.Phase.FAULT) return faultSnapshot("persisted-fault");
            FinaleStateMachine.WrenOutcome wren = FinaleStateMachine.parseWrenOutcome(
                    properties.getProperty("wren-outcome"));
            FinaleStateMachine.NameTreatment name = FinaleStateMachine.parseNameTreatment(
                    properties.getProperty("name-treatment"));
            FinaleStateMachine.ConductVerdict conduct = FinaleStateMachine.parseConductVerdict(
                    properties.getProperty("conduct-verdict"));
            return new FinaleStateMachine.Snapshot(phase, wren, name, conduct,
                    properties.getProperty("armed-by", ""), parseLong(properties.getProperty("armed-at")),
                    parseLong(properties.getProperty("cancel-cutoff-at")),
                    parseLong(properties.getProperty("committed-at")));
        } catch (Throwable corrupt) {
            plugin.getLogger().severe("[finale] Refusing corrupt/incomplete finale state "
                    + file.getAbsolutePath() + ": " + corrupt.getMessage()
                    + ". Entering fail-closed maintenance lock.");
            return faultSnapshot("corrupt-state");
        }
    }

    private boolean persist(FinaleStateMachine.Snapshot state) {
        File file = stateFile();
        File folder = file.getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) return false;
        File temp = new File(folder, FILE_NAME + ".tmp");
        Properties properties = new Properties();
        properties.setProperty("schema", "2");
        properties.setProperty("phase", state.phase().name());
        properties.setProperty("wren-outcome", enumName(state.wrenOutcome()));
        properties.setProperty("name-treatment", enumName(state.nameTreatment()));
        properties.setProperty("conduct-verdict", enumName(state.conductVerdict()));
        properties.setProperty("armed-by", state.armedBy());
        properties.setProperty("armed-at", Long.toString(state.armedAtEpochMs()));
        properties.setProperty("cancel-cutoff-at", Long.toString(state.cancelCutoffAtEpochMs()));
        properties.setProperty("committed-at", Long.toString(state.committedAtEpochMs()));
        try (FileOutputStream out = new FileOutputStream(temp)) {
            properties.store(out, "The Observance V5 finale state - do not edit while Paper is running");
            out.getFD().sync();
        } catch (IOException writeFailure) {
            safeDelete(temp);
            safety.warn("finale.persist", "Could not write durable finale state: "
                    + writeFailure.getClass().getSimpleName() + ": " + writeFailure.getMessage());
            return false;
        }
        try {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException moveFailure) {
            safeDelete(temp);
            safety.warn("finale.persist", "Could not install durable finale state: "
                    + moveFailure.getClass().getSimpleName() + ": " + moveFailure.getMessage());
            return false;
        }
    }

    private File stateFile() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }

    private static FinaleStateMachine.Snapshot faultSnapshot(String reason) {
        return new FinaleStateMachine.Snapshot(FinaleStateMachine.Phase.FAULT,
                null, null, null, reason, 0L, 0L, 0L);
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        return Long.parseLong(raw.trim());
    }

    private static String enumName(Enum<?> value) {
        return value == null ? "-" : value.name().toLowerCase(Locale.ROOT);
    }

    private static void sendMultiline(Player player, String text, NamedTextColor color) {
        if (player == null || text == null) return;
        for (String line : text.split("\\R", -1)) {
            player.sendMessage(line.isBlank() ? Component.empty() : Component.text(line, color));
        }
    }

    private static void safeDelete(File file) {
        try {
            if (file != null) Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) { }
    }
}
