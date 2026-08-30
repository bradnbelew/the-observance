package com.observance.watcher.morrow.dialog;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Action;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Decision;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.View;
import com.observance.watcher.morrow.presentation.BukkitMorrowBody;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest;
import com.observance.watcher.morrow.room04.staticrestore.BukkitStaticRestore;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Candidate;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.CandidateId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Evidence;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Provenance;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestorePredicate;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Native Paper dialog adapter for the four authored Room 04 exchanges. */
public final class BukkitMorrowDialogs implements Listener, AutoCloseable {
    private static final long CLASSIFICATION_SELECTION_MILLIS = 60_000L;
    private static final Key ACKNOWLEDGE = Key.key("observance:morrow/acknowledge-room04");
    private static final Key AUTHENTICATE_PROPOSAL = Key.key("observance:morrow/authenticate-proposal");
    private static final Key REVIEW_EVIDENCE = Key.key("observance:morrow/review-evidence");
    private static final Key RESET_STATIC_RESTORE = Key.key("observance:morrow/reset-static-restore");
    private static final Key AUTHORIZE_REPLAY = Key.key("observance:morrow/authorize-entity-replay");
    private static final Key DECLINE_REPLAY = Key.key("observance:morrow/decline-entity-replay");

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final MorrowLocalState state;
    private final BukkitMorrowBody body;
    private final BukkitStaticRestore staticRestore;
    private final StaticRestorePredicate staticRestorePredicate;
    private final Map<Key, ClassificationInput> classificationInputs;
    private final Map<UUID, PendingClassification> pendingClassifications = new HashMap<>();
    private boolean started;

    public BukkitMorrowDialogs(
            JavaPlugin plugin,
            World world,
            Origin origin,
            MorrowLocalState state,
            BukkitMorrowBody body,
            BukkitStaticRestore staticRestore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.state = Objects.requireNonNull(state, "state");
        this.body = Objects.requireNonNull(body, "body");
        this.staticRestore = Objects.requireNonNull(staticRestore, "staticRestore");
        this.staticRestorePredicate = new StaticRestorePredicate(staticRestore.manifest());
        this.classificationInputs = classificationInputs();
    }

    public void start() {
        if (started) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
    }

    public void openCurrent(Player player) {
        Objects.requireNonNull(player, "player");
        pendingClassifications.remove(player.getUniqueId());
        body.focus(player);
        View view = MorrowDialogAuthority.currentView(state.snapshot());
        try {
            player.showDialog(switch (view) {
                case TERMINAL_GREETING -> terminalGreeting();
                case RESTORATION_PROPOSAL -> restorationProposal();
                case EVIDENCE_REVIEW -> evidenceReview();
                case ENTITY_REPLAY_AUTHORIZATION -> entityReplayAuthorization();
            });
        } catch (LinkageError | RuntimeException unavailable) {
            player.sendMessage(Component.text(
                    "The Morrow terminal could not open its native form. Nothing changed; retry after server recovery.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow Paper Dialog failed safely: " + safe(unavailable.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTerminal(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;
        Candidate candidate = staticRestore.candidate(event.getClickedBlock());
        if (candidate != null) {
            event.setCancelled(true);
            openClassification(event.getPlayer(), candidate);
            return;
        }
        if (!isTerminal(event.getClickedBlock())) return;
        event.setCancelled(true);
        openCurrent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBody(PlayerInteractEntityEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND) return;
        Evidence evidence = staticRestore.evidence(event.getRightClicked());
        Candidate candidate = staticRestore.candidate(event.getRightClicked());
        if (evidence == null && candidate == null && !body.isOwnedInteraction(event.getRightClicked())) return;
        event.setCancelled(true);
        if (evidence != null) {
            openEvidence(event.getPlayer(), evidence);
            return;
        }
        if (candidate != null) {
            openClassification(event.getPlayer(), candidate);
            return;
        }
        openCurrent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDialogAction(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        ClassificationInput classification = classificationInputs.get(event.getIdentifier());
        if (classification != null) {
            Player player = connection.getPlayer();
            body.focus(player);
            PendingClassification pending = pendingClassifications.remove(player.getUniqueId());
            if (!validPending(player, classification, pending)) {
                player.sendMessage(Component.text(
                        "That marker selection expired or was not made at the physical B01–B06 diff. Nothing was filed.",
                        NamedTextColor.YELLOW));
                return;
            }
            applyClassification(player, classification);
            return;
        }
        Action action = action(event.getIdentifier());
        if (action == null) return;
        Player player = connection.getPlayer();
        body.focus(player);
        apply(player, action);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        pendingClassifications.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        pendingClassifications.remove(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!started || !isInsideRoom(player)) return;
            try {
                staticRestore.reconcile(state.snapshot());
                if (state.snapshot().committedEvents().contains(MorrowDialogAuthority.PROPOSAL_AUTHENTICATED)) {
                    player.sendMessage(Component.text(
                            state.snapshot().committedEvents().contains(MorrowDialogAuthority.INTENTION_ERROR_PROVEN)
                                    ? "Room 04 catch-up: B06 remains retained as inferred; no receipt duplicated."
                                    : "Room 04 catch-up: the six-cell Static Restore resumed from physical audit.",
                            NamedTextColor.GRAY));
                }
            } catch (IOException | RuntimeException failure) {
                player.sendMessage(Component.text(
                        "Room 04 catch-up halted on a physical audit mismatch. No receipt or block was overwritten.",
                        NamedTextColor.RED));
                plugin.getLogger().warning("Morrow Room 04 join catch-up halted: " + safe(failure.getMessage()));
            }
        });
    }

    private void apply(Player player, Action action) {
        Decision decision = MorrowDialogAuthority.decide(action, state.snapshot());
        if (decision.status() == MorrowDialogAuthority.Status.NOT_READY) {
            player.sendMessage(Component.text(decision.feedback(), NamedTextColor.YELLOW));
            return;
        }
        if (!decision.commitsReceipt()) {
            if (action == Action.RESET_STATIC_RESTORE
                    && decision.status() == MorrowDialogAuthority.Status.READY) {
                try {
                    staticRestore.resetAndReplay(state.snapshot());
                } catch (IOException | RuntimeException failure) {
                    player.sendMessage(Component.text(
                            "The six-cell reset failed safely; its current pass was rolled back. Retry at the terminal.",
                            NamedTextColor.RED));
                    plugin.getLogger().warning("Morrow Static Restore reset failed safely: " + safe(failure.getMessage()));
                    return;
                }
            }
            player.sendMessage(Component.text(decision.feedback(), NamedTextColor.GRAY));
            return;
        }
        MorrowLocalState.CommitResult result;
        try {
            result = state.commit(
                    decision.eventKey(), decision.idempotencyKey(), decision.payload());
        } catch (IOException | IllegalStateException | IllegalArgumentException failure) {
            player.sendMessage(Component.text(
                    "The terminal failed safely. No new receipt or world change was created.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow dialog action failed safely: " + safe(failure.getMessage()));
            return;
        }
        try {
            staticRestore.reconcile(result.snapshot());
            body.applySnapshot(result.snapshot());
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The local receipt is retained, but its bounded scene halted safely. Use Reset to resume from audit.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow dialog consequence halted safely: " + safe(failure.getMessage()));
            return;
        }
        player.sendMessage(Component.text(
                result.created() ? decision.feedback() : "That group action is already retained; nothing was duplicated.",
                NamedTextColor.AQUA));
    }

    private void applyClassification(Player player, ClassificationInput input) {
        StaticRestorePredicate.Attempt attempt;
        try {
            attempt = staticRestorePredicate.evaluate(
                    state.snapshot(), input.candidate().id(), input.provenance(), staticRestore.complete());
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The physical six-cell diff failed audit. No marker or receipt was filed; use Reset to recover.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M02 classification audit failed safely: " + safe(failure.getMessage()));
            return;
        }
        if (!attempt.commitsProof()) {
            player.sendMessage(Component.text(
                    attempt.feedback(),
                    attempt.status() == StaticRestorePredicate.Status.WRONG
                            ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            return;
        }
        MorrowLocalState.CommitResult result;
        try {
            result = state.commit(attempt.eventKey(), attempt.idempotencyKey(), attempt.payload());
        } catch (IOException | IllegalStateException | IllegalArgumentException failure) {
            player.sendMessage(Component.text(
                    "The factual marker could not be retained. No proof was created; the restored blocks are unchanged.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M02 proof commit failed safely: " + safe(failure.getMessage()));
            return;
        }
        try {
            staticRestore.reconcile(result.snapshot());
            body.applySnapshot(result.snapshot());
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The M02 proof receipt is retained, but its provenance displays need a safe retry at the terminal.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M02 proof consequence failed safely: " + safe(failure.getMessage()));
            return;
        }
        player.sendMessage(Component.text(
                result.created() ? attempt.feedback() : "B06 is already retained as the group inference proof; nothing duplicated.",
                NamedTextColor.AQUA));
    }

    private void openEvidence(Player player, Evidence evidence) {
        pendingClassifications.remove(player.getUniqueId());
        body.focus(player);
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(base(evidence.title(), evidence.custody() + ". " + evidence.content()))
                    .type(DialogType.notice(button(
                            "Close source",
                            "Returns to the physical evidence bay. Creates no receipt.",
                            REVIEW_EVIDENCE)))));
        } catch (LinkageError | RuntimeException unavailable) {
            player.sendMessage(Component.text(
                    evidence.title() + " — " + evidence.custody() + ". " + evidence.content(),
                    NamedTextColor.WHITE));
        }
    }

    private void openClassification(Player player, Candidate candidate) {
        pendingClassifications.remove(player.getUniqueId());
        body.focus(player);
        try {
            List<ActionButton> choices = java.util.Arrays.stream(Provenance.values())
                    .map(provenance -> button(
                            "File " + provenance.key(),
                            provenance.accessibleMark() + ". The physical cell and this class form one attempt.",
                            classificationKey(candidate, provenance)))
                    .toList();
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(base(
                            "Mark restored cell " + candidate.id(),
                            candidate.accessibleDescription() + ". Select one authored provenance class. "
                                    + "Wrong attempts change no blocks and create no progression receipt."))
                    .type(DialogType.multiAction(choices).columns(2).build())));
            pendingClassifications.put(player.getUniqueId(), new PendingClassification(
                    candidate.id(), System.currentTimeMillis() + CLASSIFICATION_SELECTION_MILLIS));
        } catch (LinkageError | RuntimeException unavailable) {
            player.sendMessage(Component.text(
                    "The classification control could not open. No marker or receipt was filed.",
                    NamedTextColor.RED));
        }
    }

    private Dialog terminalGreeting() {
        return Dialog.create(builder -> builder.empty()
                .base(base(
                        "Recovery Room 04",
                        "MORROW: Recovery source received. This room is bounded and reversible. "
                                + "Acknowledge only that you are present; no restoration will run."))
                .type(DialogType.notice(button(
                        "Acknowledge Room 04",
                        "Creates one group-scoped room witness receipt. It changes no blocks.",
                        ACKNOWLEDGE))));
    }

    private Dialog restorationProposal() {
        return Dialog.create(builder -> builder.empty()
                .base(base(
                        "Static restoration proposal",
                        "MORROW: Proposed region: Recovery Room 04 comparison bay. "
                                + "Applying writes one receipt first, then restores exactly six cells in three visible passes. "
                                + "Reset returns only those six cells to their Room 04 baseline and safely replays."))
                .type(DialogType.multiAction(List.of(
                                button("Apply bounded proposal",
                                        "Creates the proposal receipt, then applies three audited two-cell passes.",
                                        AUTHENTICATE_PROPOSAL),
                                button("Review listed evidence",
                                        "Read-only review. Creates no receipt.", REVIEW_EVIDENCE)))
                        .columns(1)
                        .build()));
    }

    private Dialog evidenceReview() {
        return Dialog.create(builder -> builder.empty()
                .base(base(
                        "Evidence review",
                        "Four independently openable stations stand in the east bay: current room, archived coordinate image, "
                                + "authenticated block manifest, and Finch planning post. Click a B01–B06 physical marker, then "
                                + "file one provenance class. Dialogue cannot decide the result."))
                .type(DialogType.multiAction(List.of(
                                button("Close without filing",
                                        "Closes the review. No receipt is created.", REVIEW_EVIDENCE),
                                button("Reset six-cell restore",
                                        "Rolls only B01–B06 to baseline, audits them, then replays all three passes.",
                                        RESET_STATIC_RESTORE)))
                        .columns(1)
                        .build()));
    }

    private Dialog entityReplayAuthorization() {
        ActionButton authorize = button(
                "Authorize Entity Replay",
                "Stores bounded local scene samples only; display echoes cannot alter blocks. "
                        + "Stop at the terminal; rollback removes the display echoes.",
                AUTHORIZE_REPLAY);
        ActionButton decline = button(
                "Not now",
                "Keeps Entity Replay disabled and preserves all current evidence.",
                DECLINE_REPLAY);
        return Dialog.create(builder -> builder.empty()
                .base(base(
                        "Entity Replay authorization",
                        "Authorize only the recorded-action replay inside Recovery Room 04. "
                                + "No account impersonation, remote movement streaming, or block mutation is permitted."))
                .type(DialogType.confirmation(authorize, decline)));
    }

    private static DialogBase base(String title, String message) {
        return DialogBase.builder(Component.text(title))
                .canCloseWithEscape(true)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(message), 360)))
                .build();
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label))
                .tooltip(Component.text(tooltip))
                .width(200)
                .action(DialogAction.customClick(key, null))
                .build();
    }

    private boolean isTerminal(Block block) {
        if (block.getWorld() != world) return false;
        int expectedX = origin.x() + RecoveryRoom04Manifest.TERMINAL_CELL.x();
        int expectedZ = origin.z() + RecoveryRoom04Manifest.TERMINAL_CELL.z();
        int baseY = origin.y() + RecoveryRoom04Manifest.TERMINAL_CELL.y();
        return block.getX() == expectedX && block.getZ() == expectedZ
                && (block.getY() == baseY || block.getY() == baseY + 1);
    }

    private boolean isInsideRoom(Player player) {
        if (player.getWorld() != world) return false;
        var bounds = RecoveryRoom04Manifest.BOUNDS;
        int x = player.getLocation().getBlockX() - origin.x();
        int y = player.getLocation().getBlockY() - origin.y();
        int z = player.getLocation().getBlockZ() - origin.z();
        return x >= bounds.minimumX() && x <= bounds.maximumX()
                && y >= bounds.minimumY() && y <= bounds.maximumY()
                && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }

    private static Action action(Key key) {
        if (ACKNOWLEDGE.equals(key)) return Action.ACKNOWLEDGE_ROOM;
        if (AUTHENTICATE_PROPOSAL.equals(key)) return Action.AUTHENTICATE_PROPOSAL;
        if (REVIEW_EVIDENCE.equals(key)) return Action.REVIEW_EVIDENCE;
        if (RESET_STATIC_RESTORE.equals(key)) return Action.RESET_STATIC_RESTORE;
        if (AUTHORIZE_REPLAY.equals(key)) return Action.AUTHORIZE_ENTITY_REPLAY;
        if (DECLINE_REPLAY.equals(key)) return Action.DECLINE_ENTITY_REPLAY;
        return null;
    }

    private static Key classificationKey(Candidate candidate, Provenance provenance) {
        return Key.key("observance:morrow/classify-"
                + candidate.id().name().toLowerCase(java.util.Locale.ROOT)
                + "-" + provenance.key());
    }

    private Map<Key, ClassificationInput> classificationInputs() {
        LinkedHashMap<Key, ClassificationInput> inputs = new LinkedHashMap<>();
        for (Candidate candidate : staticRestore.manifest().candidates().values()) {
            for (Provenance provenance : Provenance.values()) {
                inputs.put(classificationKey(candidate, provenance), new ClassificationInput(candidate, provenance));
            }
        }
        return Map.copyOf(inputs);
    }

    private boolean validPending(
            Player player,
            ClassificationInput input,
            PendingClassification pending) {
        if (pending == null || pending.candidate() != input.candidate().id()
                || System.currentTimeMillis() > pending.expiresAtMillis()
                || player.getWorld() != world) {
            return false;
        }
        var cell = input.candidate().cell();
        double x = origin.x() + cell.x() + 0.5;
        double y = origin.y() + cell.y() + 0.5;
        double z = origin.z() + cell.z() + 0.5;
        return player.getLocation().distanceSquared(new org.bukkit.Location(world, x, y, z)) <= 36.0;
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        pendingClassifications.clear();
        started = false;
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }

    private record ClassificationInput(Candidate candidate, Provenance provenance) { }
    private record PendingClassification(CandidateId candidate, long expiresAtMillis) { }
}
