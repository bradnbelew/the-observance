package com.observance.watcher.morrow.consensus;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Classification;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Progress;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Result;
import com.observance.watcher.morrow.consensus.ConsensusAuditInstaller.Origin;
import com.observance.watcher.morrow.consensus.ConsensusAuditManifest.Cell;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Paper adapter for M06 private receipt reading, public dissent marks, and native classification. */
public final class BukkitConsensusAudit implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Key UNANIMOUS = Key.key("observance:morrow/m06-unanimous");
    private static final Key FORGED = Key.key("observance:morrow/m06-forged");
    private static final Key INCOMPLETE = Key.key("observance:morrow/m06-incomplete-consensus");

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final MorrowLocalState state;
    private final BooleanSupplier m05Preserved;
    private final ConsensusAuditManifest manifest = new ConsensusAuditManifest();
    private final ConsensusAuditProgressStore store;
    private final NamespacedKey ownedKey;
    private final Map<UUID, Long> pendingChoices = new HashMap<>();
    private final List<TextDisplay> displays = new ArrayList<>();
    private TextDisplay resultBoard;
    private Progress progress;
    private boolean started;

    public BukkitConsensusAudit(JavaPlugin plugin, World world, Origin origin, String releaseId,
                                MorrowLocalState state, BooleanSupplier m05Preserved,
                                Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.m05Preserved = Objects.requireNonNull(m05Preserved, "m05Preserved");
        this.store = new ConsensusAuditProgressStore(
                Objects.requireNonNull(dataDirectory, "dataDirectory").resolve("morrow-consensus-audit.progress"),
                releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_consensus_audit_owned");
    }

    public void start() throws IOException {
        if (started) return;
        progress = store.load();
        syncLamps();
        spawnDisplays();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        plugin.getLogger().info("MORROW_M06_READY manifest=" + manifest.manifestSha256()
                + " receipts=" + progress.custodians().size() + " marked=" + progress.selected().size()
                + " displays=" + displays.size());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConsole(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (matches(block, manifest.auditConsole())) {
            event.setCancelled(true);
            if (action == Action.RIGHT_CLICK_BLOCK) openClassification(event.getPlayer());
            return;
        }
        String receiptId = terminal(block);
        if (receiptId == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            Result result = action == Action.RIGHT_CLICK_BLOCK
                    ? ConsensusAuditAuthority.inspect(progress, state.snapshot(), m05Preserved.getAsBoolean(),
                    receiptId, player.getUniqueId())
                    : ConsensusAuditAuthority.toggle(progress, state.snapshot(), m05Preserved.getAsBoolean(),
                    receiptId, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            syncLamps();
            if (result.receipt() != null) {
                player.sendMessage(Component.text("PRIVATE VOTE RECEIPT // "
                        + result.receipt().vote().summary(), NamedTextColor.AQUA));
            }
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text("The vote terminal halted safely. No receipt, mark, or vote changed.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M06 terminal failed safely: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClassification(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Classification classification = classification(event.getIdentifier());
        if (classification == null) return;
        Player player = connection.getPlayer();
        Long expires = pendingChoices.remove(player.getUniqueId());
        if (expires == null || System.currentTimeMillis() > expires || !inside(player)) {
            player.sendMessage(Component.text("That M06 classification expired or left the chamber. Nothing changed.",
                    NamedTextColor.YELLOW));
            return;
        }
        try {
            Result result = ConsensusAuditAuthority.submit(progress, state.snapshot(),
                    m05Preserved.getAsBoolean(), classification, player.getUniqueId());
            if (result.commitsEvent()) {
                state.commit(result.eventKey(), result.idempotencyKey(), ConsensusAuditAuthority.payload(result));
                syncLamps();
                refreshResultBoard();
            }
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text("The M06 classification halted safely. All six votes remain immutable.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M06 classification failed safely: " + safe(failure.getMessage()));
        }
    }

    private void openClassification(Player player) {
        pendingChoices.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        String selected = progress.selected().isEmpty() ? "none" : String.join(", ", new java.util.TreeSet<>(progress.selected()));
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("The vote Morrow completed"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Published: " + ConsensusAuditAuthority.PUBLISHED_RESULT + "\nPolicy: "
                                            + ConsensusAuditAuthority.POLICY + "\nMarked receipts: " + selected), 420)))
                            .build())
                    .type(DialogType.multiAction(List.of(
                            button("Confirm unanimous", "Claims every signed voter agreed.", UNANIMOUS),
                            button("Report forged votes", "Claims Morrow altered signatures or vote text.", FORGED),
                            button("Classify incomplete consensus",
                                    "Preserves all votes and identifies the changed denominator.", INCOMPLETE)))
                            .columns(1).build())));
        } catch (LinkageError | RuntimeException unavailable) {
            pendingChoices.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native M06 classification could not open. Nothing changed; retry the audit console.",
                    NamedTextColor.RED));
        }
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(220)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncLamps() {
        for (Map.Entry<String, Cell> entry : manifest.lamps().entrySet()) {
            setLamp(entry.getValue(), progress.selected().contains(entry.getKey()));
        }
        setLamp(manifest.auditLamp(), state.snapshot().committedEvents().contains(ConsensusAuditAuthority.EVENT));
    }
    private void setLamp(Cell cell, boolean lit) {
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData(
                        "minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }

    private void spawnDisplays() {
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        }
        for (ConsensusAuditAuthority.VoteReceipt vote : ConsensusAuditAuthority.VOTES) {
            Cell terminal = manifest.terminals().get(vote.id());
            displays.add(spawn(new Location(world, origin.x() + terminal.x() + .5,
                    origin.y() + 3.15, origin.z() + terminal.z() + .5),
                    vote.id() + "\nRIGHT: PRIVATE READ\nLEFT: MARK / UNMARK", NamedTextColor.AQUA, 130));
        }
        resultBoard = spawn(new Location(world, origin.x() + .5, origin.y() + 3.8, origin.z() + 1.5),
                resultText(), NamedTextColor.GOLD, 300);
        displays.add(resultBoard);
        displays.add(spawn(new Location(world, origin.x() + .5, origin.y() + 2.65, origin.z() + 3.5),
                "AUDIT CONSOLE // RIGHT-CLICK\n" + ConsensusAuditAuthority.POLICY,
                NamedTextColor.GRAY, 360));
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
    private void refreshResultBoard() {
        if (resultBoard != null && resultBoard.isValid()) {
            resultBoard.text(Component.text(resultText(), NamedTextColor.GOLD));
        }
    }
    private String resultText() {
        return state.snapshot().committedEvents().contains(ConsensusAuditAuthority.EVENT)
                ? "PUBLIC CORRECTION\nINCOMPLETE CONSENSUS\n4 CONTINUE / 2 EXCLUDED SHUT DOWN"
                : "PUBLISHED RESULT\n" + ConsensusAuditAuthority.PUBLISHED_RESULT;
    }

    private String terminal(Block block) {
        for (Map.Entry<String, Cell> entry : manifest.terminals().entrySet()) {
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
        var bounds = ConsensusAuditManifest.BOUNDS;
        return x >= bounds.minimumX() && x <= bounds.maximumX()
                && y >= bounds.minimumY() && y <= bounds.maximumY()
                && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }
    private static Classification classification(Key key) {
        if (UNANIMOUS.equals(key)) return Classification.UNANIMOUS;
        if (FORGED.equals(key)) return Classification.FORGED_VOTES;
        if (INCOMPLETE.equals(key)) return Classification.INCOMPLETE_CONSENSUS;
        return null;
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case COMMIT -> NamedTextColor.GREEN;
            case LOCKED, INCOMPLETE, WRONG_RECEIPTS, WRONG_CLASSIFICATION -> NamedTextColor.YELLOW;
            default -> NamedTextColor.AQUA;
        };
    }
    public int ownedEntityCount() { return displays.size(); }

    @Override public void close() {
        HandlerList.unregisterAll(this);
        pendingChoices.clear();
        for (TextDisplay display : displays) if (display.isValid()) display.remove();
        displays.clear();
        resultBoard = null;
        started = false;
    }
    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }
}
