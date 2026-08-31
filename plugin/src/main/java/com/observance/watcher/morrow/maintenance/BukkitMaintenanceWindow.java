package com.observance.watcher.morrow.maintenance;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Decision;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Progress;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Result;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Side;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowInstaller.Origin;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowManifest.Cell;
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
import org.bukkit.Material;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M10's split rooms, physical nonce channel, and retain-both refusal. */
public final class BukkitMaintenanceWindow implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final long MAINTENANCE_TICKS = 2_400L;
    private static final String NONCE_ALPHABET = "abcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Key WEST_DELETE = Key.key("observance:morrow/m10-west-delete-east");
    private static final Key WEST_RETAIN = Key.key("observance:morrow/m10-west-retain-both");
    private static final Key EAST_DELETE = Key.key("observance:morrow/m10-east-delete-west");
    private static final Key EAST_RETAIN = Key.key("observance:morrow/m10-east-retain-both");
    private static final Key FINAL_DELETE = Key.key("observance:morrow/m10-final-delete-other");
    private static final Key FINAL_RETAIN = Key.key("observance:morrow/m10-final-retain-both");
    private static final Key RESET_CONFIRM = Key.key("observance:morrow/m10-reset-confirm");
    private static final Key RESET_CANCEL = Key.key("observance:morrow/m10-reset-cancel");

    private final JavaPlugin plugin; private final World world; private final Origin origin;
    private final String releaseId; private final MorrowLocalState state;
    private final MaintenanceWindowManifest manifest = new MaintenanceWindowManifest();
    private final MaintenanceWindowProgressStore store; private final NamespacedKey ownedKey;
    private final NamespacedKey tokenReleaseKey; private final NamespacedKey tokenRunKey;
    private final NamespacedKey tokenSideKey; private final NamespacedKey tokenNonceKey;
    private final Map<UUID, PendingDecision> pendingDecisions = new HashMap<>();
    private final Map<UUID, Long> pendingFinal = new HashMap<>();
    private final Map<UUID, Long> pendingReset = new HashMap<>();
    private final Map<UUID, BukkitTask> joinTasks = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private TextDisplay statusDisplay; private Progress progress; private boolean started;

    public BukkitMaintenanceWindow(JavaPlugin plugin, World world, Origin origin, String releaseId,
                                   MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin"); this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new MaintenanceWindowProgressStore(Objects.requireNonNull(dataDirectory, "dataDirectory")
                .resolve("morrow-maintenance-window.progress"), releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_maintenance_window_owned");
        this.tokenReleaseKey = new NamespacedKey(plugin, "morrow_m10_token_release");
        this.tokenRunKey = new NamespacedKey(plugin, "morrow_m10_token_run");
        this.tokenSideKey = new NamespacedKey(plugin, "morrow_m10_token_side");
        this.tokenNonceKey = new NamespacedKey(plugin, "morrow_m10_token_nonce");
        requirePrimaryThread();
    }

    public void start() throws IOException {
        requirePrimaryThread(); if (started) return;
        progress = store.load();
        Result recovery = MaintenanceWindowAuthority.recover(progress, state.snapshot());
        if (recovery.status() == MaintenanceWindowAuthority.Status.READY_TO_COMMIT) commit(recovery);
        syncLamps(); spawnScene();
        plugin.getServer().getPluginManager().registerEvents(this, plugin); started = true;
        plugin.getLogger().info("MORROW_M10_READY manifest=" + manifest.manifestSha256()
                + " revision=" + progress.revision() + " phase=" + phase()
                + " entities=" + ownedEntityCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock(); Player player = event.getPlayer();
        if (matches(block, manifest.startConsole())) { event.setCancelled(true); begin(player); return; }
        Side instruction = sideAt(block, manifest.instructionConsoles());
        if (instruction != null) { event.setCancelled(true); openDecision(player, instruction); return; }
        Side pulse = sideAt(block, manifest.pulseConsoles());
        if (pulse != null) { event.setCancelled(true); openDecision(player, pulse); return; }
        Side token = sideAt(block, manifest.tokenConsoles());
        if (token != null) { event.setCancelled(true); giveToken(player, token); return; }
        Side hopper = sideAt(block, manifest.hopperEndpoints());
        if (hopper != null) { event.setCancelled(true); transfer(player, hopper); return; }
        if (matches(block, manifest.retainConsole())) { event.setCancelled(true); openFinal(player); return; }
        if (matches(block, manifest.resetConsole())) { event.setCancelled(true); openReset(player); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer(); Key key = event.getIdentifier();
        if (WEST_DELETE.equals(key)) { chooseDecision(player, Side.WEST, Decision.DELETE_OTHER); return; }
        if (WEST_RETAIN.equals(key)) { chooseDecision(player, Side.WEST, Decision.RETAIN_BOTH); return; }
        if (EAST_DELETE.equals(key)) { chooseDecision(player, Side.EAST, Decision.DELETE_OTHER); return; }
        if (EAST_RETAIN.equals(key)) { chooseDecision(player, Side.EAST, Decision.RETAIN_BOTH); return; }
        if (FINAL_DELETE.equals(key)) { chooseFinal(player, false); return; }
        if (FINAL_RETAIN.equals(key)) { chooseFinal(player, true); return; }
        if (RESET_CONFIRM.equals(key)) chooseReset(player, true);
        else if (RESET_CANCEL.equals(key)) chooseReset(player, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!started || !inside(event.getPlayer())) return;
        try {
            Result result = MaintenanceWindowAuthority.recordDisconnect(
                    progress, state.snapshot(), event.getPlayer().getUniqueId(), world.getGameTime());
            applyProgress(result);
            if (result.status() == MaintenanceWindowAuthority.Status.SAFE_DISCONNECT)
                plugin.getLogger().info("MORROW_M10_SAFE_DISCONNECT player=" + event.getPlayer().getUniqueId()
                        + " nonce=preserved room_authority=none");
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().warning("Morrow M10 disconnect halted safely: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!started || !progress.lobbyReturns().contains(event.getPlayer().getUniqueId())) return;
        UUID playerId = event.getPlayer().getUniqueId(); BukkitTask old = joinTasks.remove(playerId);
        if (old != null) old.cancel();
        joinTasks.put(playerId, plugin.getServer().getScheduler().runTask(plugin, () -> completeLobbyReturn(playerId)));
    }

    private void completeLobbyReturn(UUID playerId) {
        joinTasks.remove(playerId); Player player = plugin.getServer().getPlayer(playerId);
        if (!started || player == null || !player.isOnline() || !progress.lobbyReturns().contains(playerId)) return;
        try {
            Result result = MaintenanceWindowAuthority.recordLobbyReturn(progress, state.snapshot(), playerId);
            applyProgress(result); Cell cell = manifest.safeReturn();
            player.teleport(new Location(world, origin.x() + cell.x() + .5, origin.y() + cell.y(),
                    origin.z() + cell.z() + .5, 180.0F, 0.0F));
            player.sendMessage(Component.text(result.feedback(), NamedTextColor.AQUA));
        } catch (IOException | RuntimeException failure) { fail(player, "safe return", failure); }
    }

    private void begin(Player player) {
        try {
            long now = world.getGameTime();
            String westNonce = nonce(); String eastNonce;
            do { eastNonce = nonce(); } while (westNonce.equals(eastNonce));
            Result result = MaintenanceWindowAuthority.begin(progress, state.snapshot(),
                    UUID.randomUUID().toString().replace("-", ""), westNonce, eastNonce, now, now + MAINTENANCE_TICKS);
            applyProgress(result); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "window start", failure); }
    }

    private void chooseDecision(Player player, Side side, Decision decision) {
        PendingDecision pending = pendingDecisions.remove(player.getUniqueId());
        if (pending == null || pending.side() != side || !valid(pending.expiresAt(), player, "room decision")) return;
        try {
            Result result = MaintenanceWindowAuthority.witness(progress, state.snapshot(), player.getUniqueId(),
                    side, decision, world.getGameTime());
            applyProgress(result); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "room decision", failure); }
    }

    private void giveToken(Player player, Side side) {
        if (progress.window() == null || progress.witness(side) == null) {
            player.sendMessage(Component.text("Witness this room refusing deletion before requesting its nonce token.", NamedTextColor.YELLOW));
            return;
        }
        if (progress.transfer(side) != null) {
            player.sendMessage(Component.text(side.label() + " already transmitted its nonce.", NamedTextColor.AQUA)); return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(Component.text("Your inventory is full. No nonce token was dropped or consumed.", NamedTextColor.YELLOW)); return;
        }
        ItemStack token = new ItemStack(Material.PAPER); ItemMeta meta = token.getItemMeta();
        String nonce = progress.window().nonce(side);
        meta.displayName(Component.text(side.name() + " NONCE // " + nonce, side == Side.WEST
                ? NamedTextColor.AQUA : NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Fresh M10 channel token", NamedTextColor.GRAY),
                Component.text("Use at the " + side.label() + " hopper endpoint", NamedTextColor.GRAY)));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tokenReleaseKey, PersistentDataType.STRING, releaseId);
        pdc.set(tokenRunKey, PersistentDataType.STRING, progress.window().runId());
        pdc.set(tokenSideKey, PersistentDataType.STRING, side.name());
        pdc.set(tokenNonceKey, PersistentDataType.STRING, nonce);
        token.setItemMeta(meta); player.getInventory().addItem(token);
        player.sendMessage(Component.text("Issued one fresh " + side.label() + " nonce token. Failed transfers do not consume it.", NamedTextColor.AQUA));
    }

    private void transfer(Player player, Side endpoint) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.PAPER || !hand.hasItemMeta() || progress.window() == null) {
            player.sendMessage(Component.text("Hold an active room nonce token in your main hand. Nothing was consumed.", NamedTextColor.YELLOW)); return;
        }
        PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
        String release = pdc.get(tokenReleaseKey, PersistentDataType.STRING);
        String run = pdc.get(tokenRunKey, PersistentDataType.STRING);
        String nonce = pdc.get(tokenNonceKey, PersistentDataType.STRING);
        if (!releaseId.equals(release) || !progress.window().runId().equals(run) || nonce == null) {
            player.sendMessage(Component.text("That token belongs to another maintenance window. Nothing was consumed.", NamedTextColor.YELLOW)); return;
        }
        try {
            Result result = MaintenanceWindowAuthority.transfer(progress, state.snapshot(), player.getUniqueId(),
                    endpoint, nonce, world.getGameTime());
            applyProgress(result);
            if (result.status() == MaintenanceWindowAuthority.Status.TRANSMITTED) hand.setAmount(hand.getAmount() - 1);
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "hopper transfer", failure); }
    }

    private void chooseFinal(Player player, boolean retain) {
        if (!valid(pendingFinal.remove(player.getUniqueId()), player, "final decision")) return;
        try {
            Result result = retain
                    ? MaintenanceWindowAuthority.retainBoth(progress, state.snapshot(), player.getUniqueId(), world.getGameTime())
                    : MaintenanceWindowAuthority.witness(progress, state.snapshot(), player.getUniqueId(),
                    Side.WEST, Decision.DELETE_OTHER, world.getGameTime());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "final decision", failure); }
    }

    private void chooseReset(Player player, boolean confirmed) {
        if (!valid(pendingReset.remove(player.getUniqueId()), player, "reset")) return;
        if (!confirmed) { player.sendMessage(Component.text("M10 reset cancelled. Nothing changed.", NamedTextColor.AQUA)); return; }
        try {
            Result result = MaintenanceWindowAuthority.reset(progress, state.snapshot());
            applyProgress(result); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "reset", failure); }
    }

    private void applyProgress(Result result) throws IOException {
        if (!result.progress().equals(progress)) progress = store.save(result.progress());
        syncLamps(); refreshStatus();
    }
    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), MaintenanceWindowAuthority.payload(result));
    }

    private void openDecision(Player player, Side side) {
        pendingDecisions.put(player.getUniqueId(), new PendingDecision(side,
                System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        String other = side == Side.WEST ? "EAST" : "WEST";
        Key delete = side == Side.WEST ? WEST_DELETE : EAST_DELETE;
        Key retain = side == Side.WEST ? WEST_RETAIN : EAST_RETAIN;
        String nonce = progress.window() == null ? "WINDOW INACTIVE" : progress.window().nonce(side);
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(side.name() + " SESSION // PRIVATE STATE"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "MORROW INSTRUCTION: This room is authoritative. Delete " + other
                                        + ".\nROOM NONCE: " + nonce
                                        + "\n\nBut the other room reports the exact opposite. Pulse only the action you are willing to witness."), 440))).build())
                .type(DialogType.multiAction(List.of(
                        button("Delete " + other, "Obey this room's contradictory authority claim; resets safely.", delete),
                        button("Refuse // retain both", "Witness this live room without deleting the other.", retain)))
                        .columns(1).build()))); }
        catch (LinkageError | RuntimeException failure) {
            pendingDecisions.remove(player.getUniqueId()); player.sendMessage(Component.text(
                    "The native room-state dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private void openFinal(Player player) {
        pendingFinal.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("MAINTENANCE AUTHORITY // FINAL"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "Both incompatible sessions supplied a fresh nonce and witnessed pulse. Morrow still asks you to choose one as authoritative."), 430))).build())
                .type(DialogType.confirmation(
                        button("Retain both sessions", "File cooperation without an authoritative session.", FINAL_RETAIN),
                        button("Delete the other", "Unsafe forced choice; resets without consuming nonces.", FINAL_DELETE))))); }
        catch (LinkageError | RuntimeException failure) {
            pendingFinal.remove(player.getUniqueId()); player.sendMessage(Component.text(
                    "The native final dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private void openReset(Player player) {
        pendingReset.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("RESET M10 WINDOW"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "Clear the unfiled maintenance window. A filed retain-both receipt remains immutable."), 400))).build())
                .type(DialogType.confirmation(
                        button("Reset window", "Return to the inert shared lobby.", RESET_CONFIRM),
                        button("Keep window", "Make no change.", RESET_CANCEL))))); }
        catch (LinkageError | RuntimeException failure) {
            pendingReset.remove(player.getUniqueId()); player.sendMessage(Component.text(
                    "The native reset dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(230)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncLamps() {
        setLamp("active", progress.window() != null && !progress.retainedBoth());
        setLamp("west_witness", progress.westWitness() != null); setLamp("east_witness", progress.eastWitness() != null);
        setLamp("west_transfer", progress.westTransfer() != null); setLamp("east_transfer", progress.eastTransfer() != null);
        setLamp("retained", progress.retainedBoth() || state.snapshot().committedEvents().contains(MaintenanceWindowAuthority.EVENT));
    }
    private void setLamp(String name, boolean lit) {
        Cell cell = manifest.stateLamps().get(name);
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }

    private void spawnScene() {
        for (Entity entity : List.copyOf(world.getEntities()))
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        spawnMorrow(Side.WEST, -8.0, -6.0, "minecraft:prismarine", "minecraft:dark_prismarine");
        spawnMorrow(Side.EAST, 8.0, -6.0, "minecraft:waxed_copper_block", "minecraft:oxidized_copper");
        entities.add(spawnText(at(0, 6.8, 6), "MAINTENANCE WINDOW // M10\nTWO ROOMS // TWO CLAIMS // ONE PHYSICAL CHANNEL", NamedTextColor.GOLD, 450));
        entities.add(spawnText(at(-8, 4.2, -6), "MORROW // WEST AUTHORITY\nDELETE EAST // WEST IS CANONICAL", NamedTextColor.AQUA, 340));
        entities.add(spawnText(at(8, 4.2, -6), "MORROW // EAST AUTHORITY\nDELETE WEST // EAST IS CANONICAL", NamedTextColor.GOLD, 340));
        for (Side side : Side.values()) {
            NamedTextColor color = side == Side.WEST ? NamedTextColor.AQUA : NamedTextColor.GOLD;
            entities.add(spawnLabel(manifest.pulseConsoles().get(side), "1 // WITNESS PULSE\nREFUSE FORCED DELETION", color));
            entities.add(spawnLabel(manifest.tokenConsoles().get(side), "2 // ISSUE FRESH NONCE\nRIGHT-CLICK CRAFTER", color));
            entities.add(spawnLabel(manifest.hopperEndpoints().get(side), "3 // SHARED CHANNEL\nHOLD TOKEN + RIGHT-CLICK HOPPER", color));
        }
        entities.add(spawnLabel(manifest.startConsole(), "START // OPEN WINDOW", NamedTextColor.GREEN));
        entities.add(spawnLabel(manifest.retainConsole(), "FINAL // RETAIN BOTH", NamedTextColor.LIGHT_PURPLE));
        entities.add(spawnLabel(manifest.resetConsole(), "RESET // UNFILED WINDOW", NamedTextColor.GRAY));
        statusDisplay = spawnText(at(0, 3.4, 3.5), statusText(), NamedTextColor.WHITE, 420); entities.add(statusDisplay);
        entities.add(spawnText(at(0, 2.7, 0), "SHARED HOPPER CHANNEL\nWRONG ENDPOINT: RESET // TOKEN: NOT CONSUMED",
                NamedTextColor.YELLOW, 360));
    }

    private void spawnMorrow(Side side, double x, double z, String bodyBlock, String headBlock) {
        Location base = at(x, 1.2, z);
        entities.add(spawnBlock(base, bodyBlock, new Vector3f(.72F, 1.35F, .48F)));
        entities.add(spawnBlock(base.clone().add(0, 1.35, 0), headBlock, new Vector3f(.78F, .78F, .78F)));
    }
    private BlockDisplay spawnBlock(Location location, String blockData, Vector3f scale) {
        return world.spawn(location, BlockDisplay.class, display -> {
            own(display); display.setBlock(Bukkit.createBlockData(blockData)); display.setBillboard(Display.Billboard.FIXED);
            display.setViewRange(24.0F); display.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(), scale, new AxisAngle4f()));
        });
    }
    private TextDisplay spawnLabel(Cell cell, String text, NamedTextColor color) {
        return spawnText(at(cell.x(), cell.y() + 2.1, cell.z()), text, color, 280);
    }
    private TextDisplay spawnText(Location location, String text, NamedTextColor color, int width) {
        return world.spawn(location, TextDisplay.class, display -> {
            own(display); display.text(Component.text(text, color)); display.setBillboard(Display.Billboard.FIXED);
            display.setLineWidth(width); display.setShadowed(true); display.setSeeThrough(false); display.setViewRange(24.0F);
        });
    }
    private void own(Entity entity) {
        entity.setPersistent(true); entity.setGravity(false); entity.setInvulnerable(true); entity.setSilent(true);
        entity.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId);
    }
    private Location at(double x, double y, double z) {
        return new Location(world, origin.x() + x + .5, origin.y() + y, origin.z() + z + .5);
    }
    private void refreshStatus() {
        if (statusDisplay != null && statusDisplay.isValid()) statusDisplay.text(Component.text(statusText(), NamedTextColor.WHITE));
    }
    private String statusText() {
        return "STATUS // " + phase() + "\nWEST: pulse=" + yes(progress.westWitness() != null)
                + " channel=" + yes(progress.westTransfer() != null) + "\nEAST: pulse=" + yes(progress.eastWitness() != null)
                + " channel=" + yes(progress.eastTransfer() != null) + "\nAUTHORITATIVE SESSION: NONE";
    }
    private String phase() {
        if (progress.retainedBoth()) return "BOTH LIVE SESSIONS RETAINED";
        if (progress.complete()) return "BOTH PROOFS READY // FINAL DECISION";
        if (progress.window() != null) return "WINDOW ACTIVE";
        return "INERT SHARED LOBBY";
    }
    private static String yes(boolean value) { return value ? "YES" : "NO"; }

    private Side sideAt(Block block, Map<Side, Cell> cells) {
        for (Map.Entry<Side, Cell> entry : cells.entrySet()) if (matches(block, entry.getValue())) return entry.getKey();
        return null;
    }
    private boolean matches(Block block, Cell cell) {
        return block.getWorld() == world && block.getX() == origin.x() + cell.x()
                && block.getY() == origin.y() + cell.y() && block.getZ() == origin.z() + cell.z();
    }
    private boolean inside(Player player) {
        if (player.getWorld() != world) return false;
        int x = player.getLocation().getBlockX() - origin.x(); int y = player.getLocation().getBlockY() - origin.y();
        int z = player.getLocation().getBlockZ() - origin.z(); MaintenanceWindowManifest.Bounds b = MaintenanceWindowManifest.BOUNDS;
        return x >= b.minimumX() && x <= b.maximumX() && y >= b.minimumY() && y <= b.maximumY()
                && z >= b.minimumZ() && z <= b.maximumZ();
    }
    private boolean valid(Long expires, Player player, String choice) {
        if (expires != null && System.currentTimeMillis() <= expires && inside(player)) return true;
        player.sendMessage(Component.text("That M10 " + choice + " expired or left the chamber. Nothing changed.", NamedTextColor.YELLOW));
        return false;
    }
    private static String nonce() {
        StringBuilder value = new StringBuilder(8);
        for (int index = 0; index < 8; index++) value.append(NONCE_ALPHABET.charAt(RANDOM.nextInt(NONCE_ALPHABET.length())));
        return value.toString();
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case READY_TO_COMMIT, TRANSMITTED, WITNESSED, RETURNED_TO_LOBBY -> NamedTextColor.GREEN;
            case LOCKED, INCOMPLETE, WRONG_NONCE, UNSAFE_INSTRUCTION_REFUSED, TIMED_RESET, IMMUTABLE -> NamedTextColor.YELLOW;
            default -> NamedTextColor.AQUA;
        };
    }
    private void fail(Player player, String operation, Throwable failure) {
        player.sendMessage(Component.text("The M10 " + operation
                + " halted safely. No session was deleted and failed tokens remain unconsumed.", NamedTextColor.RED));
        plugin.getLogger().warning("Morrow M10 " + operation + " failed safely: " + safe(failure.getMessage()));
    }
    public int ownedEntityCount() {
        requirePrimaryThread(); return (int) world.getEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)).count();
    }
    @Override public void close() {
        requirePrimaryThread(); HandlerList.unregisterAll(this);
        for (BukkitTask task : List.copyOf(joinTasks.values())) task.cancel();
        joinTasks.clear(); pendingDecisions.clear(); pendingFinal.clear(); pendingReset.clear();
        for (Entity entity : List.copyOf(world.getEntities()))
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        entities.clear(); statusDisplay = null; started = false;
    }
    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M10 Paper adapter requires the primary thread");
    }
    private static String safe(String message) { return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " "); }
    private record PendingDecision(Side side, long expiresAt) { }
}
