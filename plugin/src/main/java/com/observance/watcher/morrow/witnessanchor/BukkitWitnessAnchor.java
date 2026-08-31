package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.CellId;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.MaterialChoice;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Progress;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Result;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorInstaller.Origin;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorManifest.Cell;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M07's arbitrary six-cell anchor and immutable reconstruction audit. */
public final class BukkitWitnessAnchor implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Key CLEAR = Key.key("observance:morrow/m07-clear");
    private static final Key COMMIT = Key.key("observance:morrow/m07-commit");
    private static final Key KEEP_EDITING = Key.key("observance:morrow/m07-keep-editing");
    private static final Map<MaterialChoice, Key> MATERIAL_KEYS = Map.of(
            MaterialChoice.COPPER_SQUARE, Key.key("observance:morrow/m07-copper-square"),
            MaterialChoice.CARVED_SHELF, Key.key("observance:morrow/m07-carved-shelf"),
            MaterialChoice.TARGET_RING, Key.key("observance:morrow/m07-target-ring"),
            MaterialChoice.TUFF_BRICKS, Key.key("observance:morrow/m07-tuff-bricks"),
            MaterialChoice.HONEYCOMB, Key.key("observance:morrow/m07-honeycomb"),
            MaterialChoice.WEATHERED_CUT, Key.key("observance:morrow/m07-weathered-cut"));

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final MorrowLocalState state;
    private final WitnessAnchorManifest manifest = new WitnessAnchorManifest();
    private final WitnessAnchorProgressStore store;
    private final NamespacedKey ownedKey;
    private final Map<UUID, PendingEdit> pendingEdits = new HashMap<>();
    private final Map<UUID, Long> pendingCommits = new HashMap<>();
    private final Map<CellId, TextDisplay> cellDisplays = new EnumMap<>(CellId.class);
    private final List<TextDisplay> displays = new ArrayList<>();
    private TextDisplay statusDisplay;
    private Progress progress;
    private boolean started;

    public BukkitWitnessAnchor(JavaPlugin plugin, World world, Origin origin, String releaseId,
                               MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new WitnessAnchorProgressStore(
                Objects.requireNonNull(dataDirectory, "dataDirectory").resolve("morrow-witness-anchor.progress"),
                releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_witness_anchor_owned");
    }

    public void start() throws IOException {
        if (started) return;
        progress = store.load();
        Result recovery = WitnessAnchorAuthority.recover(progress, state.snapshot());
        if (recovery.status() == WitnessAnchorAuthority.Status.READY_TO_COMMIT) commit(recovery);
        syncBlocks();
        spawnDisplays();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        plugin.getLogger().info("MORROW_M07_READY manifest=" + manifest.manifestSha256()
                + " anchor=" + (progress.anchor() == null ? "draft" : progress.anchor().originalSha256())
                + " identified=" + progress.identified() + " displays=" + displays.size());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAnchorInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        CellId editCell = originalBase(block);
        if (editCell != null) {
            event.setCancelled(true);
            openMaterialChoice(player, editCell);
            return;
        }
        if (matches(block, manifest.commitConsole())) {
            event.setCancelled(true);
            openCommitChoice(player);
            return;
        }
        CellId reconstructionCell = reconstruction(block);
        if (reconstructionCell == null) return;
        event.setCancelled(true);
        try {
            Result result = WitnessAnchorAuthority.identify(
                    progress, state.snapshot(), reconstructionCell, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsEvent()) commit(result);
            syncBlocks();
            refreshDisplays();
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The M07 comparison halted safely. Both versions remain unchanged.", NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M07 comparison failed safely: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();
        MaterialChoice material = material(event.getIdentifier());
        if (material != null || CLEAR.equals(event.getIdentifier())) {
            applyEditChoice(player, material);
            return;
        }
        if (COMMIT.equals(event.getIdentifier()) || KEEP_EDITING.equals(event.getIdentifier())) {
            applyCommitChoice(player, COMMIT.equals(event.getIdentifier()));
        }
    }

    private void applyEditChoice(Player player, MaterialChoice material) {
        PendingEdit pending = pendingEdits.remove(player.getUniqueId());
        if (pending == null || System.currentTimeMillis() > pending.expiresAt() || !inside(player)) {
            player.sendMessage(Component.text("That M07 cell choice expired or left the chamber. Nothing changed.",
                    NamedTextColor.YELLOW));
            return;
        }
        try {
            Result result = WitnessAnchorAuthority.edit(
                    progress, state.snapshot(), pending.cell(), material, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            syncBlocks();
            refreshDisplays();
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text("The M07 edit halted safely. The prior draft remains authoritative.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M07 edit failed safely: " + safe(failure.getMessage()));
        }
    }

    private void applyCommitChoice(Player player, boolean commitChoice) {
        Long expires = pendingCommits.remove(player.getUniqueId());
        if (expires == null || System.currentTimeMillis() > expires || !inside(player)) {
            player.sendMessage(Component.text("That M07 commit choice expired or left the chamber. Nothing changed.",
                    NamedTextColor.YELLOW));
            return;
        }
        if (!commitChoice) {
            player.sendMessage(Component.text("Version 1 remains editable. No hash was committed.",
                    NamedTextColor.AQUA));
            return;
        }
        try {
            Result result = WitnessAnchorAuthority.commit(progress, state.snapshot(), player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            syncBlocks();
            refreshDisplays();
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text("The M07 commit halted safely. No partial version was published.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M07 commit failed safely: " + safe(failure.getMessage()));
        }
    }

    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), WitnessAnchorAuthority.payload(result));
    }

    private void openMaterialChoice(Player player, CellId cell) {
        pendingEdits.put(player.getUniqueId(),
                new PendingEdit(cell, System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        List<ActionButton> buttons = new ArrayList<>();
        for (MaterialChoice choice : MaterialChoice.values()) {
            buttons.add(button(choice.displayName(), choice.minecraftKey(), MATERIAL_KEYS.get(choice)));
        }
        buttons.add(button("Clear cell", "Returns this uncommitted cell to air.", CLEAR));
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Witness anchor // " + cell.label()))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Choose any material. This is your arbitrary input; it stays editable until version 1 is committed."), 400)))
                            .build())
                    .type(DialogType.multiAction(buttons).columns(2).build())));
        } catch (LinkageError | RuntimeException unavailable) {
            pendingEdits.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native M07 material chooser could not open. Nothing changed.",
                    NamedTextColor.RED));
        }
    }

    private void openCommitChoice(Player player) {
        pendingCommits.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        String filled = progress.draft().size() + "/6 cells filled";
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Commit witness anchor version 1"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    filled + ". Committing freezes the original arrangement permanently, hashes it, and asks Morrow to reconstruct it."), 420)))
                            .build())
                    .type(DialogType.multiAction(List.of(
                            button("Commit version 1", "Freeze all six cells and publish their hash.", COMMIT),
                            button("Keep editing", "Close without creating a version.", KEEP_EDITING)))
                            .columns(1).build())));
        } catch (LinkageError | RuntimeException unavailable) {
            pendingCommits.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native M07 commit dialog could not open. Nothing changed.",
                    NamedTextColor.RED));
        }
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(220)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncBlocks() {
        for (CellId cell : CellId.values()) {
            MaterialChoice original = progress.anchor() == null
                    ? progress.draft().get(cell) : progress.anchor().original().get(cell);
            setMaterial(manifest.originalCells().get(cell), original);
            MaterialChoice reconstruction = progress.anchor() == null
                    ? null : progress.anchor().reconstruction().get(cell);
            setMaterial(manifest.reconstructionCells().get(cell), reconstruction);
        }
        boolean lit = state.snapshot().committedEvents().contains(WitnessAnchorAuthority.EVENT);
        world.getBlockAt(origin.x() + manifest.commitLamp().x(), origin.y() + manifest.commitLamp().y(),
                        origin.z() + manifest.commitLamp().z())
                .setBlockData(Bukkit.createBlockData(
                        "minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }

    private void setMaterial(Cell cell, MaterialChoice material) {
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData(
                        material == null ? "minecraft:air" : material.minecraftKey()), false);
    }

    private void spawnDisplays() {
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        }
        displays.add(spawn(new Location(world, origin.x() - 4 + .5, origin.y() + 4.7, origin.z() - .5),
                "COMMITTED ORIGINAL\nYOUR SIX-CELL WITNESS", NamedTextColor.AQUA, 280));
        displays.add(spawn(new Location(world, origin.x() + 4 + .5, origin.y() + 4.7, origin.z() - .5),
                "MORROW RECONSTRUCTION\nEXACTLY ONE CELL IS WRONG", NamedTextColor.GOLD, 280));
        for (CellId cell : CellId.values()) {
            Cell base = manifest.originalBases().get(cell);
            TextDisplay display = spawn(new Location(world, origin.x() + base.x() + .5,
                    origin.y() + 3.35, origin.z() + base.z() + .5), cellText(cell),
                    NamedTextColor.WHITE, 180);
            cellDisplays.put(cell, display);
            displays.add(display);
        }
        statusDisplay = spawn(new Location(world, origin.x() + .5, origin.y() + 3.2, origin.z() + 4.5),
                statusText(), NamedTextColor.GREEN, 360);
        displays.add(statusDisplay);
    }

    private TextDisplay spawn(Location location, String text, NamedTextColor color, int width) {
        return world.spawn(location, TextDisplay.class, display -> {
            display.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId);
            display.text(Component.text(text, color));
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setLineWidth(width);
            display.setSeeThrough(false);
            display.setViewRange(20.0F);
        });
    }

    private void refreshDisplays() {
        for (CellId cell : CellId.values()) {
            TextDisplay display = cellDisplays.get(cell);
            if (display != null && display.isValid()) display.text(Component.text(cellText(cell), NamedTextColor.WHITE));
        }
        if (statusDisplay != null && statusDisplay.isValid()) {
            statusDisplay.text(Component.text(statusText(), NamedTextColor.GREEN));
        }
    }

    private String cellText(CellId cell) {
        MaterialChoice original = progress.anchor() == null
                ? progress.draft().get(cell) : progress.anchor().original().get(cell);
        MaterialChoice reconstruction = progress.anchor() == null
                ? null : progress.anchor().reconstruction().get(cell);
        String left = original == null ? "empty" : original.displayName();
        String right = reconstruction == null ? "not built" : reconstruction.displayName();
        return cell.label() + "\nORIGINAL: " + left + "\nRECON: " + right;
    }

    private String statusText() {
        if (state.snapshot().committedEvents().contains(WitnessAnchorAuthority.EVENT)) {
            return "ANCHOR AUTHENTICATED // VERSION 1\nSOURCE RETAINED + MISMATCH FILED";
        }
        if (progress.anchor() != null) {
            return "VERSION 1 // " + progress.anchor().originalSha256().substring(0, 12)
                    + "\nRIGHT-CLICK THE WRONG RECONSTRUCTION CELL";
        }
        return "EDIT: RIGHT-CLICK SIX LEFT BASES\nCOMMIT: RIGHT-CLICK CENTER CONSOLE // "
                + progress.draft().size() + "/6";
    }

    private CellId originalBase(Block block) {
        for (Map.Entry<CellId, Cell> entry : manifest.originalBases().entrySet()) {
            if (matches(block, entry.getValue())) return entry.getKey();
        }
        return null;
    }
    private CellId reconstruction(Block block) {
        for (Map.Entry<CellId, Cell> entry : manifest.reconstructionCells().entrySet()) {
            if (matches(block, entry.getValue())) return entry.getKey();
        }
        return null;
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
        var bounds = WitnessAnchorManifest.BOUNDS;
        return x >= bounds.minimumX() && x <= bounds.maximumX()
                && y >= bounds.minimumY() && y <= bounds.maximumY()
                && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }
    private static MaterialChoice material(Key key) {
        for (Map.Entry<MaterialChoice, Key> entry : MATERIAL_KEYS.entrySet()) {
            if (entry.getValue().equals(key)) return entry.getKey();
        }
        return null;
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case READY_TO_COMMIT -> NamedTextColor.GREEN;
            case LOCKED, INCOMPLETE, WRONG_CELL -> NamedTextColor.YELLOW;
            default -> NamedTextColor.AQUA;
        };
    }
    public int ownedEntityCount() { return displays.size(); }

    @Override public void close() {
        HandlerList.unregisterAll(this);
        pendingEdits.clear();
        pendingCommits.clear();
        for (TextDisplay display : displays) if (display.isValid()) display.remove();
        displays.clear();
        cellDisplays.clear();
        statusDisplay = null;
        started = false;
    }
    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }
    private record PendingEdit(CellId cell, long expiresAt) { }
}
