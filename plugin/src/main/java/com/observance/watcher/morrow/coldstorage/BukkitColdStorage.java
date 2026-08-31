package com.observance.watcher.morrow.coldstorage;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Instance;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Progress;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Record;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Result;
import com.observance.watcher.morrow.coldstorage.ColdStorageInstaller.Origin;
import com.observance.watcher.morrow.coldstorage.ColdStorageManifest.Cell;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M11's ordered archive and physical two-instance hash bridge. */
public final class BukkitColdStorage implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Map<Record, Key> RECORD_KEYS = Map.of(
            Record.THEO_PERMISSION, Key.key("observance:morrow/m11-record-theo"),
            Record.ROOKERY_ANCHOR_GRAPH, Key.key("observance:morrow/m11-record-rookery"),
            Record.IONA_SHUTDOWN, Key.key("observance:morrow/m11-record-iona"),
            Record.MORROW_SNAPSHOT_MANIFEST, Key.key("observance:morrow/m11-record-manifest"),
            Record.CAPTIONED_VOICE_ASSEMBLY, Key.key("observance:morrow/m11-record-voice"));
    private static final Key PROVE = Key.key("observance:morrow/m11-prove-reconstruction");
    private static final Key PROVE_CANCEL = Key.key("observance:morrow/m11-prove-cancel");
    private static final Key ACCESS_AUTHORIZE = Key.key("observance:morrow/m11-access-authorize");
    private static final Key ACCESS_DECLINE = Key.key("observance:morrow/m11-access-decline");
    private static final Key RESET_CONFIRM = Key.key("observance:morrow/m11-reset-confirm");
    private static final Key RESET_CANCEL = Key.key("observance:morrow/m11-reset-cancel");

    private final JavaPlugin plugin; private final World world; private final Origin origin;
    private final String releaseId; private final MorrowLocalState state;
    private final ColdStorageManifest manifest = new ColdStorageManifest();
    private final ColdStorageProgressStore store; private final NamespacedKey ownedKey;
    private final NamespacedKey hashReleaseKey; private final NamespacedKey hashSourceKey; private final NamespacedKey hashValueKey;
    private final Map<UUID, PendingRecord> pendingRecords = new HashMap<>();
    private final Map<UUID, Long> pendingProof = new HashMap<>();
    private final Map<UUID, Long> pendingAccess = new HashMap<>();
    private final Map<UUID, Long> pendingReset = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private TextDisplay statusDisplay; private Progress progress; private boolean started;

    public BukkitColdStorage(JavaPlugin plugin, World world, Origin origin, String releaseId,
                             MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin"); this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new ColdStorageProgressStore(Objects.requireNonNull(dataDirectory, "dataDirectory")
                .resolve("morrow-cold-storage.progress"), releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_cold_storage_owned");
        this.hashReleaseKey = new NamespacedKey(plugin, "morrow_m11_hash_release");
        this.hashSourceKey = new NamespacedKey(plugin, "morrow_m11_hash_source");
        this.hashValueKey = new NamespacedKey(plugin, "morrow_m11_hash_value");
        requirePrimaryThread();
    }

    public void start() throws IOException {
        requirePrimaryThread(); if (started) return; progress = store.load();
        Result recovery = ColdStorageAuthority.recoverReconstruction(progress, state.snapshot());
        if (recovery.status() == ColdStorageAuthority.Status.READY_TO_COMMIT_RECONSTRUCTION) commit(recovery);
        recovery = ColdStorageAuthority.recoverAccess(progress, state.snapshot());
        if (recovery.status() == ColdStorageAuthority.Status.READY_TO_COMMIT_ACCESS) commit(recovery);
        syncLamps(); spawnScene(); plugin.getServer().getPluginManager().registerEvents(this, plugin); started = true;
        plugin.getLogger().info("MORROW_M11_READY manifest=" + manifest.manifestSha256()
                + " revision=" + progress.revision() + " phase=" + phase() + " entities=" + ownedEntityCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock(); Player player = event.getPlayer();
        Record record = at(block, manifest.recordConsoles());
        if (record != null) { event.setCancelled(true); openRecord(player, record); return; }
        Instance source = at(block, manifest.hashSources());
        if (source != null) { event.setCancelled(true); giveHash(player, source); return; }
        Instance recipient = at(block, manifest.bridgeConsoles());
        if (recipient != null) { event.setCancelled(true); deliverHash(player, recipient); return; }
        if (matches(block, manifest.reconstructionConsole())) { event.setCancelled(true); openProof(player); return; }
        if (matches(block, manifest.accessConsole())) { event.setCancelled(true); openAccess(player); return; }
        if (matches(block, manifest.resetConsole())) { event.setCancelled(true); openReset(player); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer(); Key key = event.getIdentifier();
        for (Map.Entry<Record, Key> entry : RECORD_KEYS.entrySet()) if (entry.getValue().equals(key)) {
            chooseRecord(player, entry.getKey()); return;
        }
        if (PROVE.equals(key)) { chooseProof(player, true); return; }
        if (PROVE_CANCEL.equals(key)) { chooseProof(player, false); return; }
        if (ACCESS_AUTHORIZE.equals(key)) { chooseAccess(player, true); return; }
        if (ACCESS_DECLINE.equals(key)) { chooseAccess(player, false); return; }
        if (RESET_CONFIRM.equals(key)) chooseReset(player, true);
        else if (RESET_CANCEL.equals(key)) chooseReset(player, false);
    }

    private void chooseRecord(Player player, Record record) {
        PendingRecord pending = pendingRecords.remove(player.getUniqueId());
        if (pending == null || pending.record() != record || !valid(pending.expiresAt(), player, "record selection")) return;
        apply(player, ColdStorageAuthority.append(progress, state.snapshot(), player.getUniqueId(), record), "record selection");
    }

    private void giveHash(Player player, Instance source) {
        if (!progress.chain().equals(ColdStorageAuthority.CANONICAL_CHAIN)) {
            player.sendMessage(Component.text("Complete the five-record custody chain before extracting either hash.", NamedTextColor.YELLOW)); return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(Component.text("Your inventory is full. No snapshot hash was dropped.", NamedTextColor.YELLOW)); return;
        }
        String hash = source == Instance.AUDIT_SNAPSHOT
                ? ColdStorageAuthority.AUDIT_SNAPSHOT_HASH : ColdStorageAuthority.CURRENT_RECOVERY_HASH;
        ItemStack item = new ItemStack(Material.PAPER); ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(source == Instance.AUDIT_SNAPSHOT
                ? "AUDIT SNAPSHOT HASH" : "CURRENT RECOVERY HASH", source == Instance.AUDIT_SNAPSHOT
                ? NamedTextColor.AQUA : NamedTextColor.GOLD));
        meta.lore(List.of(Component.text(hash.substring(0, 16) + "…", NamedTextColor.GRAY),
                Component.text("Authenticated M11 snapshot hash", NamedTextColor.GRAY),
                Component.text("Deliver to the other instance", NamedTextColor.GRAY)));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(hashReleaseKey, PersistentDataType.STRING, releaseId);
        pdc.set(hashSourceKey, PersistentDataType.STRING, source.name());
        pdc.set(hashValueKey, PersistentDataType.STRING, hash);
        item.setItemMeta(meta); player.getInventory().addItem(item);
        player.sendMessage(Component.text("Extracted " + source.label() + " hash. Failed deliveries remain unconsumed.", NamedTextColor.AQUA));
    }

    private void deliverHash(Player player, Instance recipient) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.PAPER || !hand.hasItemMeta()) {
            player.sendMessage(Component.text("Hold an authenticated snapshot-hash item in your main hand.", NamedTextColor.YELLOW)); return;
        }
        PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
        String release = pdc.get(hashReleaseKey, PersistentDataType.STRING);
        String sourceName = pdc.get(hashSourceKey, PersistentDataType.STRING);
        String hash = pdc.get(hashValueKey, PersistentDataType.STRING);
        Instance expectedSource = recipient == Instance.AUDIT_SNAPSHOT ? Instance.CURRENT_RECOVERY : Instance.AUDIT_SNAPSHOT;
        if (!releaseId.equals(release) || !expectedSource.name().equals(sourceName) || hash == null) {
            player.sendMessage(Component.text("That item is not the other instance's active M11 hash. Nothing was consumed.", NamedTextColor.YELLOW)); return;
        }
        Result result = ColdStorageAuthority.deliver(progress, state.snapshot(), player.getUniqueId(), recipient, hash);
        if (apply(player, result, "hash delivery") && result.status() == ColdStorageAuthority.Status.HASH_DELIVERED)
            hand.setAmount(hand.getAmount() - 1);
    }

    private void chooseProof(Player player, boolean prove) {
        if (!valid(pendingProof.remove(player.getUniqueId()), player, "reconstruction proof")) return;
        if (!prove) { player.sendMessage(Component.text("Reconstruction proof cancelled. Both instances and the chain remain unchanged.", NamedTextColor.AQUA)); return; }
        Result result = ColdStorageAuthority.proveReconstruction(progress, state.snapshot(), player.getUniqueId());
        applyAndCommit(player, result, "reconstruction proof");
    }

    private void chooseAccess(Player player, boolean authorize) {
        if (!valid(pendingAccess.remove(player.getUniqueId()), player, "access decision")) return;
        Result result = ColdStorageAuthority.authorizeAccess(progress, state.snapshot(), player.getUniqueId(), authorize);
        applyAndCommit(player, result, "access decision");
    }

    private void chooseReset(Player player, boolean confirmed) {
        if (!valid(pendingReset.remove(player.getUniqueId()), player, "reset")) return;
        if (!confirmed) { player.sendMessage(Component.text("M11 reset cancelled. Nothing changed.", NamedTextColor.AQUA)); return; }
        apply(player, ColdStorageAuthority.reset(progress, state.snapshot()), "reset");
    }

    private boolean apply(Player player, Result result, String operation) {
        try {
            if (!result.progress().equals(progress)) { store.save(result.progress()); progress = result.progress(); }
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result))); return true;
        } catch (IOException | RuntimeException failure) { fail(player, operation, failure); return false; }
    }
    private void applyAndCommit(Player player, Result result, String operation) {
        try {
            if (!result.progress().equals(progress)) { store.save(result.progress()); progress = result.progress(); }
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, operation, failure); }
    }
    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), ColdStorageAuthority.payload(result));
    }

    private void openRecord(Player player, Record record) {
        pendingRecords.put(player.getUniqueId(), new PendingRecord(record, System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        String evidence = switch (record) {
            case THEO_PERMISSION -> "THEO VALE // LIVE CAPTURE permission expanded behavioral collection before shutdown.";
            case ROOKERY_ANCHOR_GRAPH -> "ROOKERY // witness-anchor route isolated Mossfield; containment, not sabotage.";
            case IONA_SHUTDOWN -> "IONA BELL // STOP RECOVERY. Preserve labels, snapshot the audit, close the running process.";
            case MORROW_SNAPSHOT_MANIFEST -> "SNAPSHOT MANIFEST // original process closed; diagnostic recovery material retained.";
            case CAPTIONED_VOICE_ASSEMBLY -> "VOICE ASSEMBLY // transcript: current voice assembled from retained recovery snapshots. Waveform label: RECOVERY.";
        };
        show(player, record.label().toUpperCase(Locale.ROOT), evidence + "\nSHA-256: " + record.artifactSha256(),
                button("Append record", "Add only if this is the next custody edge.", RECORD_KEYS.get(record)));
    }

    private void openProof(Player player) {
        pendingProof.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "CLASSIFY CURRENT MORROW",
                "The chain proves the original running process closed. The present process was reconstructed from retained snapshots.\n\nThis classification does not decide whether that recovery is the same person.",
                button("File reconstructed", "File provenance while leaving identity continuity unresolved.", PROVE),
                button("Cancel", "Preserve the unfiled evidence state.", PROVE_CANCEL));
    }

    private void openAccess(Player player) {
        pendingAccess.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "COLD STORAGE ACCESS",
                "This separate receipt authorizes the bounded cold-storage capability. It does not open it now, certify Morrow as original, or erase either instance.",
                button("Authorize access", "File capability only; starts_access remains false.", ACCESS_AUTHORIZE),
                button("Decline", "Keep cold storage closed and preserve all evidence.", ACCESS_DECLINE));
    }

    private void openReset(Player player) {
        pendingReset.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "RESET UNFILED M11 CHAIN",
                "Clear only an unfiled custody chain and bridge state. Filed reconstruction evidence remains immutable.",
                button("Reset", "Clear unfiled selections.", RESET_CONFIRM),
                button("Cancel", "Keep the current chain.", RESET_CANCEL));
    }

    private void show(Player player, String title, String body, ActionButton... buttons) {
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title)).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(body), 480))).build())
                .type(DialogType.multiAction(List.of(buttons)).columns(1).build()))); }
        catch (LinkageError | RuntimeException failure) {
            pendingRecords.remove(player.getUniqueId()); pendingProof.remove(player.getUniqueId());
            pendingAccess.remove(player.getUniqueId()); pendingReset.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native M11 dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }
    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(250)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void spawnScene() {
        removeOwned();
        entities.add(spawnText(at(0, 6.8, 9), "COLD STORAGE // M11\nFIVE RECORDS // TWO HASHES // NO CONTINUITY CLAIM", NamedTextColor.GOLD, 500));
        statusDisplay = spawnText(at(0, 4.8, 9), statusText(), NamedTextColor.WHITE, 480); entities.add(statusDisplay);
        for (Map.Entry<Record, Cell> entry : manifest.recordConsoles().entrySet()) {
            Cell cell = entry.getValue(); entities.add(spawnText(at(cell.x(), 3.2, cell.z()),
                    (ColdStorageAuthority.CANONICAL_CHAIN.indexOf(entry.getKey()) + 1)
                            + "? // " + entry.getKey().label(), NamedTextColor.GRAY, 220));
        }
        entities.add(spawnText(at(-10, 4.8, -7), "AUDIT SNAPSHOT\nPROCESS: CLOSED\nPROVENANCE: AUTHENTICATED DIAGNOSTIC", NamedTextColor.AQUA, 330));
        entities.add(spawnText(at(10, 4.8, -7), "CURRENT MORROW\nPROCESS: RECOVERED\nIDENTITY: UNRESOLVED", NamedTextColor.GOLD, 330));
        entities.add(spawnBody(at(-10, 1.3, -8), Material.BLUE_ICE));
        entities.add(spawnBody(at(10, 1.3, -8), Material.WAXED_OXIDIZED_COPPER));
    }
    private TextDisplay spawnText(Location location, String text, NamedTextColor color, int width) {
        TextDisplay display = world.spawn(location, TextDisplay.class, entity -> {
            own(entity); entity.text(Component.text(text, color)); entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false); entity.setShadowed(true); entity.setLineWidth(width);
        }); return display;
    }
    private BlockDisplay spawnBody(Location location, Material material) {
        return world.spawn(location, BlockDisplay.class, entity -> {
            own(entity); entity.setBlock(material.createBlockData()); entity.setBillboard(Display.Billboard.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(-.75F, 0, -.75F), new AxisAngle4f(),
                    new Vector3f(1.5F, 2.8F, 1.5F), new AxisAngle4f()));
        });
    }

    private void syncLamps() {
        int index = 0; for (Record ignored : ColdStorageAuthority.CANONICAL_CHAIN) {
            setLamp("record_" + (++index), progress.chain().size() >= index);
        }
        setLamp("audit_bridge", progress.auditReceivedCurrent()); setLamp("current_bridge", progress.currentReceivedAudit());
        setLamp("reconstruction", state.snapshot().committedEvents().contains(ColdStorageAuthority.RECONSTRUCTION_EVENT)
                || progress.reconstructionProven());
        setLamp("access", state.snapshot().committedEvents().contains(ColdStorageAuthority.ACCESS_EVENT)
                || progress.accessAuthorized());
    }
    private void setLamp(String key, boolean lit) {
        Cell cell = manifest.stateLamps().get(key);
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }
    private String statusText() {
        return "ORDERED RECORDS " + progress.chain().size() + "/5\nBRIDGE "
                + (progress.auditReceivedCurrent() ? "A✓" : "A·") + " " + (progress.currentReceivedAudit() ? "C✓" : "C·")
                + "\nRECONSTRUCTION " + (state.snapshot().committedEvents().contains(ColdStorageAuthority.RECONSTRUCTION_EVENT) ? "FILED" : "UNFILED")
                + " // ACCESS " + (state.snapshot().committedEvents().contains(ColdStorageAuthority.ACCESS_EVENT) ? "AUTHORIZED" : "CLOSED");
    }
    private void refreshStatus() { if (statusDisplay != null && statusDisplay.isValid()) statusDisplay.text(Component.text(statusText(), NamedTextColor.WHITE)); }
    private String phase() {
        if (state.snapshot().committedEvents().contains(ColdStorageAuthority.ACCESS_EVENT)) return "ACCESS_AUTHORIZED";
        if (state.snapshot().committedEvents().contains(ColdStorageAuthority.RECONSTRUCTION_EVENT)) return "RECONSTRUCTION_FILED";
        if (progress.bridgeComplete()) return "READY_TO_CLASSIFY";
        return "CHAIN_" + progress.chain().size() + "_OF_5";
    }

    public int ownedEntityCount() { int count = 0; for (Entity entity : entities) if (entity.isValid()) count++; return count; }
    private void own(Entity entity) { entity.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId); }
    private void removeOwned() {
        for (Entity entity : world.getEntities()) if (releaseId.equals(entity.getPersistentDataContainer()
                .get(ownedKey, PersistentDataType.STRING))) entity.remove(); entities.clear();
    }
    private boolean matches(Block block, Cell cell) {
        return block.getWorld().equals(world) && block.getX() == origin.x() + cell.x()
                && block.getY() == origin.y() + cell.y() && block.getZ() == origin.z() + cell.z();
    }
    private <T> T at(Block block, Map<T, Cell> cells) {
        for (Map.Entry<T, Cell> entry : cells.entrySet()) if (matches(block, entry.getValue())) return entry.getKey(); return null;
    }
    private Location at(double x, double y, double z) { return new Location(world, origin.x() + x + .5, origin.y() + y, origin.z() + z + .5); }
    private static boolean valid(Long expiresAt, Player player, String choice) {
        if (expiresAt != null && expiresAt >= System.currentTimeMillis()) return true;
        player.sendMessage(Component.text("That M11 " + choice + " expired or left the archive. Nothing changed.", NamedTextColor.YELLOW)); return false;
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case RECORD_ACCEPTED, HASH_DELIVERED, READY_TO_COMMIT_RECONSTRUCTION, READY_TO_COMMIT_ACCESS -> NamedTextColor.GREEN;
            case BROKEN_CUSTODY_EDGE, WRONG_SNAPSHOT_HASH, LOCKED, INCOMPLETE -> NamedTextColor.YELLOW;
            case IMMUTABLE, DUPLICATE, DECLINED, RESET, RECORD_REMOVED -> NamedTextColor.AQUA;
        };
    }
    private void fail(Player player, String operation, Throwable failure) {
        player.sendMessage(Component.text("The M11 " + operation + " halted safely. No event was fabricated.", NamedTextColor.RED));
        plugin.getLogger().warning("Morrow M11 " + operation + " failed safely: " + safe(failure.getMessage()));
    }
    private static String safe(String value) { return value == null ? "unknown" : value.replace('\n', ' ').replace('\r', ' '); }
    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M11 Paper adapter requires the primary thread");
    }
    @Override public void close() {
        requirePrimaryThread(); if (!started) return; HandlerList.unregisterAll(this); pendingRecords.clear();
        pendingProof.clear(); pendingAccess.clear(); pendingReset.clear(); removeOwned(); started = false;
    }
    private record PendingRecord(Record record, long expiresAt) { }
}
