package com.observance.watcher.morrow.dialog;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Action;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Decision;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.View;
import com.observance.watcher.morrow.presentation.BukkitMorrowBody;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Native Paper dialog adapter for the four authored Room 04 exchanges. */
public final class BukkitMorrowDialogs implements Listener, AutoCloseable {
    private static final Key ACKNOWLEDGE = Key.key("observance:morrow/acknowledge-room04");
    private static final Key AUTHENTICATE_PROPOSAL = Key.key("observance:morrow/authenticate-proposal");
    private static final Key REVIEW_EVIDENCE = Key.key("observance:morrow/review-evidence");
    private static final Key AUTHORIZE_REPLAY = Key.key("observance:morrow/authorize-entity-replay");
    private static final Key DECLINE_REPLAY = Key.key("observance:morrow/decline-entity-replay");

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final MorrowLocalState state;
    private final BukkitMorrowBody body;
    private boolean started;

    public BukkitMorrowDialogs(
            JavaPlugin plugin,
            World world,
            Origin origin,
            MorrowLocalState state,
            BukkitMorrowBody body) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.state = Objects.requireNonNull(state, "state");
        this.body = Objects.requireNonNull(body, "body");
    }

    public void start() {
        if (started) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
    }

    public void openCurrent(Player player) {
        Objects.requireNonNull(player, "player");
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
                || event.getClickedBlock() == null || !isTerminal(event.getClickedBlock())) return;
        event.setCancelled(true);
        openCurrent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBody(PlayerInteractEntityEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || !body.isOwnedInteraction(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        openCurrent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDialogAction(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Action action = action(event.getIdentifier());
        if (action == null) return;
        Player player = connection.getPlayer();
        body.focus(player);
        apply(player, action);
    }

    private void apply(Player player, Action action) {
        Decision decision = MorrowDialogAuthority.decide(action, state.snapshot());
        if (decision.status() == MorrowDialogAuthority.Status.NOT_READY) {
            player.sendMessage(Component.text(decision.feedback(), NamedTextColor.YELLOW));
            return;
        }
        if (!decision.commitsReceipt()) {
            player.sendMessage(Component.text(decision.feedback(), NamedTextColor.GRAY));
            return;
        }
        try {
            MorrowLocalState.CommitResult result = state.commit(
                    decision.eventKey(), decision.idempotencyKey(), decision.payload());
            body.applySnapshot(result.snapshot());
            player.sendMessage(Component.text(
                    result.created() ? decision.feedback() : "That group action is already retained; nothing was duplicated.",
                    NamedTextColor.AQUA));
        } catch (IOException | IllegalStateException | IllegalArgumentException failure) {
            player.sendMessage(Component.text(
                    "The terminal failed safely. No new receipt or world change was created.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow dialog action failed safely: " + safe(failure.getMessage()));
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
                                + "Authenticating records the bounded manifest only. It does not apply the restoration."))
                .type(DialogType.multiAction(List.of(
                                button("Authenticate bounded proposal",
                                        "Creates the static-proposal receipt; world mutation remains false.",
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
                        "Compare four independent sources: the current room, archived coordinate image, "
                                + "block manifest, and Finch planning note. Classification must be filed physically in the room. "
                                + "Reviewing this list creates no proof and changes nothing."))
                .type(DialogType.notice(button(
                        "Return without filing",
                        "Closes the read-only review. No receipt is created.",
                        REVIEW_EVIDENCE))));
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

    private static Action action(Key key) {
        if (ACKNOWLEDGE.equals(key)) return Action.ACKNOWLEDGE_ROOM;
        if (AUTHENTICATE_PROPOSAL.equals(key)) return Action.AUTHENTICATE_PROPOSAL;
        if (REVIEW_EVIDENCE.equals(key)) return Action.REVIEW_EVIDENCE;
        if (AUTHORIZE_REPLAY.equals(key)) return Action.AUTHORIZE_ENTITY_REPLAY;
        if (DECLINE_REPLAY.equals(key)) return Action.DECLINE_ENTITY_REPLAY;
        return null;
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        started = false;
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }
}
