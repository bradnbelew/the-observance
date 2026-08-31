package com.observance.watcher.morrow.continuity;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.AnchorMark;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Progress;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Result;
import com.observance.watcher.morrow.continuity.AccountContinuityInstaller.Origin;
import com.observance.watcher.morrow.continuity.AccountContinuityManifest.Cell;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M09's private anchor, real disconnect, bounded echo, and identity proof. */
public final class BukkitAccountContinuity implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Key RESET_CONFIRM = Key.key("observance:morrow/m09-reset-confirm");
    private static final Key RESET_CANCEL = Key.key("observance:morrow/m09-reset-cancel");
    private static final Map<AnchorMark, Key> SECRET_KEYS = markKeys("secret");
    private static final Map<AnchorMark, Key> IDENTITY_KEYS = markKeys("identity");

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final MorrowLocalState state;
    private final AccountContinuityManifest manifest = new AccountContinuityManifest();
    private final AccountContinuityProgressStore store;
    private final NamespacedKey ownedKey;
    private final Map<UUID, Long> pendingSecret = new HashMap<>();
    private final Map<UUID, Long> pendingIdentity = new HashMap<>();
    private final Map<UUID, Long> pendingReset = new HashMap<>();
    private final Map<UUID, BukkitTask> joinTasks = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private TextDisplay echoStatus;
    private Progress progress;
    private boolean started;

    public BukkitAccountContinuity(JavaPlugin plugin, World world, Origin origin, String releaseId,
                                   MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new AccountContinuityProgressStore(
                Objects.requireNonNull(dataDirectory, "dataDirectory").resolve("morrow-account-continuity.progress"),
                releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_account_continuity_owned");
        requirePrimaryThread();
    }

    public void start() throws IOException {
        requirePrimaryThread();
        if (started) return;
        progress = store.load();
        Result continuationRecovery = AccountContinuityAuthority.recoverContinuation(progress, state.snapshot());
        if (continuationRecovery.status() == AccountContinuityAuthority.Status.READY_TO_COMMIT) {
            commit(continuationRecovery);
        }
        Result identityRecovery = AccountContinuityAuthority.recoverIdentity(progress, state.snapshot());
        if (identityRecovery.status() == AccountContinuityAuthority.Status.READY_TO_COMMIT) {
            commit(identityRecovery);
        }
        syncLamps();
        spawnScene();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        plugin.getLogger().info("MORROW_M09_READY manifest=" + manifest.manifestSha256()
                + " revision=" + progress.revision() + " state=" + phase()
                + " entities=" + ownedEntityCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (matches(block, manifest.secretConsole())) {
            event.setCancelled(true); openSecret(player);
        } else if (matches(block, manifest.armConsole())) {
            event.setCancelled(true); arm(player);
        } else if (matches(block, manifest.observeConsole())) {
            event.setCancelled(true); observe(player);
        } else if (matches(block, manifest.identityConsole())) {
            event.setCancelled(true); openIdentity(player);
        } else if (matches(block, manifest.resetConsole())) {
            event.setCancelled(true); openReset(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();
        AnchorMark secret = markFor(event.getIdentifier(), SECRET_KEYS);
        if (secret != null) { chooseSecret(player, secret); return; }
        AnchorMark identity = markFor(event.getIdentifier(), IDENTITY_KEYS);
        if (identity != null) { chooseIdentity(player, identity); return; }
        if (RESET_CONFIRM.equals(event.getIdentifier())) chooseReset(player, true);
        else if (RESET_CANCEL.equals(event.getIdentifier())) chooseReset(player, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!started || progress.challenge() == null || progress.disconnect() != null
                || !progress.armed() || !progress.challenge().playerId().equals(event.getPlayer().getUniqueId())) return;
        Player player = event.getPlayer();
        List<String> actions = List.of(
                "secret:sealed",
                "scene:armed",
                positionToken(player.getLocation()),
                "disconnect:quit");
        try {
            Result result = AccountContinuityAuthority.recordDisconnect(progress, state.snapshot(),
                    player.getUniqueId(), world.getGameTime(), actions);
            applyProgress(result);
            plugin.getLogger().info("MORROW_M09_DISCONNECT volunteer=" + player.getUniqueId()
                    + " actions=" + actions.size() + " secret=excluded");
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().warning("Morrow M09 disconnect halted safely: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!started || progress.challenge() == null || progress.disconnect() == null
                || progress.returnedTick() != null
                || !progress.challenge().playerId().equals(event.getPlayer().getUniqueId())) return;
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitTask previous = joinTasks.remove(playerId);
        if (previous != null) previous.cancel();
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, () -> completeReturn(playerId));
        joinTasks.put(playerId, task);
    }

    private void completeReturn(UUID playerId) {
        joinTasks.remove(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (!started || player == null || !player.isOnline() || progress.challenge() == null
                || progress.returnedTick() != null || !progress.challenge().playerId().equals(playerId)) return;
        try {
            Result result = AccountContinuityAuthority.recordReturn(
                    progress, state.snapshot(), playerId, world.getGameTime());
            applyProgress(result);
            Cell cell = manifest.returnCell();
            player.teleport(new Location(world, origin.x() + cell.x() + .5,
                    origin.y() + cell.y(), origin.z() + cell.z() + .5, 180.0F, 0.0F));
            player.sendMessage(Component.text(result.feedback(), NamedTextColor.AQUA));
        } catch (IOException | RuntimeException failure) { fail(player, "return", failure); }
    }

    private void chooseSecret(Player player, AnchorMark mark) {
        if (!valid(pendingSecret.remove(player.getUniqueId()), player, "private anchor")) return;
        try {
            String challenge = UUID.randomUUID().toString().replace("-", "");
            Result result = AccountContinuityAuthority.begin(
                    progress, state.snapshot(), player.getUniqueId(), mark, challenge);
            applyProgress(result);
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "private anchor", failure); }
    }

    private void arm(Player player) {
        try {
            Result result = AccountContinuityAuthority.arm(progress, state.snapshot(), player.getUniqueId());
            applyProgress(result);
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "arming", failure); }
    }

    private void observe(Player player) {
        try {
            Result result = AccountContinuityAuthority.observeContinuation(
                    progress, state.snapshot(), player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshEcho();
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "observation", failure); }
    }

    private void chooseIdentity(Player player, AnchorMark mark) {
        if (!valid(pendingIdentity.remove(player.getUniqueId()), player, "identity")) return;
        try {
            Result result = AccountContinuityAuthority.authenticate(
                    progress, state.snapshot(), player.getUniqueId(), mark);
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshEcho();
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "identity", failure); }
    }

    private void chooseReset(Player player, boolean confirmed) {
        if (!valid(pendingReset.remove(player.getUniqueId()), player, "reset")) return;
        if (!confirmed) {
            player.sendMessage(Component.text("M09 reset cancelled. Nothing changed.", NamedTextColor.AQUA));
            return;
        }
        try {
            Result result = AccountContinuityAuthority.reset(progress, state.snapshot(), player.getUniqueId());
            applyProgress(result);
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "reset", failure); }
    }

    private void applyProgress(Result result) throws IOException {
        if (!result.progress().equals(progress)) progress = store.save(result.progress());
        syncLamps(); refreshEcho();
    }

    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), AccountContinuityAuthority.payload(result));
    }

    private void openSecret(Player player) {
        pendingSecret.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("PRIVATE ANCHOR // VOLUNTEER ONLY"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Choose one mark privately. Only a salted hash is retained. The echo never receives the mark; remember it after reconnecting."), 430))).build())
                    .type(DialogType.multiAction(markButtons(SECRET_KEYS,
                            "Seal this private mark; do not say it aloud.")).columns(2).build())));
        } catch (LinkageError | RuntimeException failure) {
            pendingSecret.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native private-anchor dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private void openIdentity(Player player) {
        pendingIdentity.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("RETURNING IDENTITY // EXCLUDED KNOWLEDGE"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Volunteer: choose the same private mark. A match authenticates you without granting the echo access to the answer."), 430))).build())
                    .type(DialogType.multiAction(markButtons(IDENTITY_KEYS,
                            "Claim this remembered mark privately.")).columns(2).build())));
        } catch (LinkageError | RuntimeException failure) {
            pendingIdentity.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native identity dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private void openReset(Player player) {
        pendingReset.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("RESET UNFILED M09 SCENE"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Reset is available only before the continued-session observation is filed. Filed evidence remains immutable."), 420))).build())
                    .type(DialogType.confirmation(
                            button("Reset unfiled scene", "Clear the current private challenge safely.", RESET_CONFIRM),
                            button("Keep scene", "Make no change.", RESET_CANCEL)))));
        } catch (LinkageError | RuntimeException failure) {
            pendingReset.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native reset dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private static List<ActionButton> markButtons(Map<AnchorMark, Key> keys, String tooltip) {
        List<ActionButton> buttons = new ArrayList<>();
        for (AnchorMark mark : AnchorMark.values()) buttons.add(button(mark.label(), tooltip, keys.get(mark)));
        return List.copyOf(buttons);
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(210)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncLamps() {
        setLamp("armed", progress.armed());
        setLamp("disconnected", progress.disconnect() != null);
        setLamp("continued", progress.continuationObserved()
                || state.snapshot().committedEvents().contains(AccountContinuityAuthority.CONTINUED_EVENT));
        setLamp("authenticated", progress.authenticated()
                || state.snapshot().committedEvents().contains(AccountContinuityAuthority.IDENTITY_EVENT));
    }

    private void setLamp(String name, boolean lit) {
        Cell cell = manifest.stateLamps().get(name);
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }

    private void spawnScene() {
        for (Entity entity : List.copyOf(world.getEntities()))
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        Cell dais = manifest.echoDais();
        Location base = new Location(world, origin.x() + dais.x() + .5,
                origin.y() + dais.y() + .15, origin.z() + dais.z() + .5);
        entities.add(spawnBlock(base, "minecraft:weathered_copper", new Vector3f(.62F, 1.15F, .42F)));
        entities.add(spawnBlock(base.clone().add(0, 1.18, 0), "minecraft:oxidized_copper",
                new Vector3f(.7F, .7F, .7F)));
        entities.add(spawnText(new Location(world, origin.x() + .5, origin.y() + 6.35, origin.z() + .5),
                "ACCOUNT CONTINUITY // M09\nONE PERSON LEAVES. TWO PRESENCES RETURN.", NamedTextColor.GOLD, 430));
        entities.add(spawnConsoleLabel(manifest.secretConsole(), "1 // PRIVATE ANCHOR\nRIGHT-CLICK BOOKSHELF", NamedTextColor.LIGHT_PURPLE));
        entities.add(spawnConsoleLabel(manifest.armConsole(), "2 // ARM\nRIGHT-CLICK COPPER", NamedTextColor.YELLOW));
        entities.add(spawnConsoleLabel(manifest.observeConsole(), "4 // OBSERVE ECHO\nRIGHT-CLICK LODESTONE", NamedTextColor.AQUA));
        entities.add(spawnConsoleLabel(manifest.identityConsole(), "5 // PROVE IDENTITY\nRIGHT-CLICK CHISELED COPPER", NamedTextColor.GREEN));
        entities.add(spawnConsoleLabel(manifest.resetConsole(), "RESET // UNFILED ONLY\nRIGHT-CLICK TARGET", NamedTextColor.GRAY));
        echoStatus = spawnText(base.clone().add(0, 2.25, 0), echoText(), NamedTextColor.AQUA, 340);
        entities.add(echoStatus);
        entities.add(spawnText(new Location(world, origin.x() + .5, origin.y() + 2.0, origin.z() + 1.5),
                "3 // DISCONNECT NORMALLY, THEN REJOIN\nTHE ECHO REPLAYS LABELS ONLY // SECRET ACCESS: FALSE",
                NamedTextColor.WHITE, 400));
    }

    private BlockDisplay spawnBlock(Location location, String blockData, Vector3f scale) {
        return world.spawn(location, BlockDisplay.class, display -> {
            own(display); display.setBlock(Bukkit.createBlockData(blockData));
            display.setBillboard(Display.Billboard.FIXED); display.setViewRange(24.0F);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), scale, new AxisAngle4f()));
        });
    }

    private TextDisplay spawnConsoleLabel(Cell cell, String text, NamedTextColor color) {
        return spawnText(new Location(world, origin.x() + cell.x() + .5,
                origin.y() + cell.y() + 2.15, origin.z() + cell.z() + .5), text, color, 260);
    }

    private TextDisplay spawnText(Location location, String text, NamedTextColor color, int width) {
        return world.spawn(location, TextDisplay.class, display -> {
            own(display); display.text(Component.text(text, color));
            display.setBillboard(Display.Billboard.FIXED); display.setLineWidth(width);
            display.setSeeThrough(false); display.setShadowed(true); display.setViewRange(24.0F);
        });
    }

    private void own(Entity entity) {
        entity.setPersistent(true); entity.setGravity(false); entity.setInvulnerable(true); entity.setSilent(true);
        entity.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId);
    }

    private void refreshEcho() {
        if (echoStatus != null && echoStatus.isValid())
            echoStatus.text(Component.text(echoText(), NamedTextColor.AQUA));
    }

    private String echoText() {
        StringBuilder text = new StringBuilder("ECHO // ").append(phase()).append("\n");
        if (progress.disconnect() == null) return text.append("ACTION LOG: EMPTY\nPRIVATE ANCHOR: INACCESSIBLE").toString();
        text.append("BOUNDED ACTION LOG:");
        for (String action : progress.disconnect().echoActions()) text.append("\n> ").append(action);
        return text.append("\nPRIVATE ANCHOR: INACCESSIBLE").toString();
    }

    private String phase() {
        if (progress.authenticated()) return "RETURNING PLAYER AUTHENTICATED // ECHO: RECONSTRUCTION";
        if (progress.continuationObserved()) return "CONTINUATION OBSERVED // IDENTITY OPEN";
        if (progress.returnedTick() != null) return "PLAYER + BOUNDED ECHO PRESENT";
        if (progress.disconnect() != null) return "ECHO ACTIVE // VOLUNTEER ABSENT";
        if (progress.armed()) return "ARMED // WAITING FOR REAL DISCONNECT";
        if (progress.challenge() != null) return "PRIVATE ANCHOR SEALED // ARM TEST";
        return "SAFE LOBBY // INACTIVE";
    }

    private boolean matches(Block block, Cell cell) {
        return block.getWorld() == world && block.getX() == origin.x() + cell.x()
                && block.getY() == origin.y() + cell.y() && block.getZ() == origin.z() + cell.z();
    }

    private boolean inside(Player player) {
        if (player.getWorld() != world) return false;
        int x = player.getLocation().getBlockX() - origin.x();
        int y = player.getLocation().getBlockY() - origin.y();
        int z = player.getLocation().getBlockZ() - origin.z();
        AccountContinuityManifest.Bounds bounds = AccountContinuityManifest.BOUNDS;
        return x >= bounds.minimumX() && x <= bounds.maximumX()
                && y >= bounds.minimumY() && y <= bounds.maximumY()
                && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }

    private boolean valid(Long expires, Player player, String choice) {
        if (expires != null && System.currentTimeMillis() <= expires && inside(player)) return true;
        player.sendMessage(Component.text("That M09 " + choice
                + " choice expired or left the chamber. Nothing changed.", NamedTextColor.YELLOW));
        return false;
    }

    private String positionToken(Location location) {
        long relativeX = (long) location.getBlockX() - origin.x();
        long relativeZ = (long) location.getBlockZ() - origin.z();
        return "position:x" + coordinate(relativeX) + "_z" + coordinate(relativeZ);
    }

    private static String coordinate(long value) {
        return value < 0 ? "m" + Math.abs(value) : "p" + value;
    }

    private static Map<AnchorMark, Key> markKeys(String purpose) {
        Map<AnchorMark, Key> keys = new EnumMap<>(AnchorMark.class);
        for (AnchorMark mark : AnchorMark.values()) keys.put(mark, Key.key("observance:morrow/m09-"
                + purpose + "-" + mark.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        return Map.copyOf(keys);
    }

    private static AnchorMark markFor(Key key, Map<AnchorMark, Key> keys) {
        for (Map.Entry<AnchorMark, Key> entry : keys.entrySet()) if (entry.getValue().equals(key)) return entry.getKey();
        return null;
    }

    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case READY_TO_COMMIT, RETURNED -> NamedTextColor.GREEN;
            case LOCKED, INCOMPLETE, WRONG_PLAYER, WRONG_SECRET, IMMUTABLE -> NamedTextColor.YELLOW;
            default -> NamedTextColor.AQUA;
        };
    }

    private void fail(Player player, String operation, Throwable failure) {
        player.sendMessage(Component.text("The M09 " + operation
                + " halted safely. Prior receipts and both identities remain unchanged.", NamedTextColor.RED));
        plugin.getLogger().warning("Morrow M09 " + operation + " failed safely: " + safe(failure.getMessage()));
    }

    public int ownedEntityCount() {
        requirePrimaryThread();
        return (int) world.getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)).count();
    }

    @Override
    public void close() {
        requirePrimaryThread();
        HandlerList.unregisterAll(this);
        for (BukkitTask task : List.copyOf(joinTasks.values())) task.cancel();
        joinTasks.clear(); pendingSecret.clear(); pendingIdentity.clear(); pendingReset.clear();
        for (Entity entity : List.copyOf(world.getEntities()))
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        entities.clear(); echoStatus = null; started = false;
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M09 Paper adapter requires the primary thread");
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }
}
