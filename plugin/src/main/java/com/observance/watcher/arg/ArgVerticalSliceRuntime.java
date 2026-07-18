package com.observance.watcher.arg;

import com.observance.watcher.m3runtime.PrivateSliceWorld;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Disposable-only P4/P5 ARG adapter with a real Paper Dialog and Brigadier fallback. */
public final class ArgVerticalSliceRuntime implements Listener, AutoCloseable {
    private static final Key THEORY_CLICK = Key.key("observance:p4/theory");
    private static final long REFUSAL_WINDOW_MILLIS = 60_000L;
    private static final int REFUSAL_LIMIT = 3;

    private final JavaPlugin plugin;
    private final PrivateSliceWorld world;
    private final ArgVerticalSliceState state;
    private final boolean dialogEnabled;
    private final Map<UUID, RefusalWindow> refusals = new ConcurrentHashMap<>();

    public ArgVerticalSliceRuntime(JavaPlugin plugin, PrivateSliceWorld world) throws IOException {
        this.plugin = plugin;
        this.world = world;
        Path journal = plugin.getDataFolder().toPath().resolve("arg-p4-p5-vertical-slice.journal");
        this.state = ArgVerticalSliceState.open(journal);
        this.dialogEnabled = plugin.getConfig().getBoolean("m3-review.arg-experience.dialog-enabled", true);
    }

    public void start() {
        if (world.isInstalled()) projectState();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("obscase", "The Observance P4/P5 investigation input and recovery surface",
                        List.of("observancecase"), new CaseCommand()));
        plugin.getLogger().info("ARG_VERTICAL_SLICE_READY dialog=" + dialogEnabled
                + " command=/obscase observation_gating=false theory=" + state.theoryEarned()
                + " curated=" + state.curated());
    }

    public void projectState() {
        world.setGate(state.theoryEarned());
        world.setP5CurationState(state.serviceCardsPublic(), state.penaltyCopiesInCustody(), state.curated());
    }

    public boolean gateOpen() { return state.theoryEarned(); }
    public boolean curated() { return state.curated(); }

    /** Console-only disposable harness entry; uses the same predicate as Dialog and command input. */
    public ArgVerticalSliceState.TheoryResult auditTheory(String theory) throws IOException {
        ArgVerticalSliceState.TheoryResult result = state.submitTheory(theory, "disposable-paper-audit");
        if (result == ArgVerticalSliceState.TheoryResult.ACCEPTED) projectState();
        return result;
    }

    /** Console-only disposable harness entry; projects the same protected physical selection. */
    public ArgVerticalSliceState.SelectionResult auditSelect(PrivateSliceWorld.P5Control control) throws IOException {
        ArgVerticalSliceState.SelectionResult result = switch (control) {
            case SERVICE_PUBLIC -> state.selectServiceCards("disposable-paper-audit");
            case PENALTY_CUSTODY -> state.selectPenaltyCustody("disposable-paper-audit");
        };
        boolean committed = state.commitCuration("disposable-paper-audit");
        world.setP5CurationState(state.serviceCardsPublic(), state.penaltyCopiesInCustody(),
                committed || state.curated());
        return result;
    }

    public String auditState() {
        return "theory=" + state.theoryEarned() + " service_public=" + state.serviceCardsPublic()
                + " penalty_custody=" + state.penaltyCopiesInCustody() + " curated=" + state.curated()
                + " receipts=" + state.receipts().size();
    }

    public void openDesk(Player player) {
        if (!dialogEnabled) {
            player.sendMessage(Component.text("Use /obscase theory <your short account>. Use /obscase help for the input rules."));
            return;
        }
        try {
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Mouth copy review"))
                            .canCloseWithEscape(true)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "State what the disagreeing copies show. Keep it short. Esc cancels. The desk does not require a source checklist."))))
                            .inputs(List.of(DialogInput.text("theory", Component.text("Account"))
                                    .width(320).maxLength(96).labelVisible(true).build()))
                            .build())
                    .type(DialogType.notice(ActionButton.builder(Component.text("Test this account"))
                            .tooltip(Component.text("The server checks the meaning and reports accepted, wrong, incomplete, or failed."))
                            .width(160).action(DialogAction.customClick(THEORY_CLICK, null)).build())));
            player.showDialog(dialog);
        } catch (LinkageError | RuntimeException unavailable) {
            plugin.getLogger().warning("Paper Dialog unavailable; using stable command fallback: " + safe(unavailable.getMessage()));
            player.sendMessage(Component.text("The review form could not open. Use /obscase theory <your short account>."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDialogResponse(PlayerCustomClickEvent event) {
        if (!THEORY_CLICK.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();
        DialogResponseView response = event.getDialogResponseView();
        if (response == null) {
            player.sendMessage(Component.text("The form returned no account. Nothing changed. Use /obscase theory as recovery."));
            return;
        }
        String theory = response.getText("theory");
        handleTheory(player, theory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onP5Controls(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        PrivateSliceWorld.P5Control control = world.p5ControlAt(event.getClickedBlock().getLocation());
        if (control == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            ArgVerticalSliceState.SelectionResult result = switch (control) {
                case SERVICE_PUBLIC -> state.selectServiceCards(player.getUniqueId().toString());
                case PENALTY_CUSTODY -> state.selectPenaltyCustody(player.getUniqueId().toString());
            };
            if (result == ArgVerticalSliceState.SelectionResult.NOT_READY) {
                player.sendActionBar(Component.text("The civic threshold is still closed."));
                return;
            }
            boolean curated = state.commitCuration(player.getUniqueId().toString());
            world.setP5CurationState(state.serviceCardsPublic(), state.penaltyCopiesInCustody(), curated || state.curated());
            player.sendActionBar(Component.text(curated
                    ? "Work cards stay public. Penalty copies move into evidence custody."
                    : control == PrivateSliceWorld.P5Control.SERVICE_PUBLIC
                    ? "Work cards marked for public use. The penalty copies still need a custody decision."
                    : "Penalty copies marked for evidence custody. The work cards still need a public-use decision."));
        } catch (IOException failure) {
            player.sendActionBar(Component.text("The curation action failed safely. Nothing was consumed."));
            plugin.getLogger().warning("P5 curation commit failed: " + safe(failure.getMessage()));
        }
    }

    private void handleTheory(Player player, String theory) {
        RefusalWindow window = refusals.compute(player.getUniqueId(), (ignored, current) ->
                current == null || current.expiresAt < System.currentTimeMillis()
                        ? new RefusalWindow(System.currentTimeMillis() + REFUSAL_WINDOW_MILLIS, 0) : current);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage(Component.text("The desk is throttled for a short time. Your evidence and world state are unchanged."));
            return;
        }
        try {
            ArgVerticalSliceState.TheoryResult result = state.submitTheory(theory, player.getUniqueId().toString());
            switch (result) {
                case ACCEPTED -> {
                    refusals.remove(player.getUniqueId());
                    world.setGate(true);
                    player.sendMessage(Component.text("Accepted. The Hold was a refuge before safety language became control. The earlier copy remains unexplained. The civic threshold is open."));
                }
                case INCOMPLETE -> player.sendMessage(Component.text("Incomplete. State what the place was and what changed. Nothing changed in the world."));
                case WRONG -> {
                    window.count++;
                    player.sendMessage(Component.text("That account does not explain the ordinary refuge, the later rule, and the earlier copy together. Nothing changed."));
                }
            }
        } catch (IOException | IllegalStateException failure) {
            player.sendMessage(Component.text("The desk failed safely. Nothing changed. Use /obscase status, then retry."));
            plugin.getLogger().warning("P4 theory commit failed: " + safe(failure.getMessage()));
        }
    }

    private final class CaseCommand implements BasicCommand {
        @Override public void execute(CommandSourceStack source, String[] args) {
            CommandSender sender = source.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage("/obscase is a player investigation/recovery surface.");
                return;
            }
            String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
            switch (action) {
                case "help" -> player.sendMessage(Component.text("Use /obscase open, /obscase theory <short account>, /obscase status, or /obscase replay. No source-click receipt is required."));
                case "open" -> openDesk(player);
                case "status" -> player.sendMessage(Component.text("P4 theory: " + (state.theoryEarned() ? "accepted" : "open")
                        + ". P5 curation: " + (state.curated() ? "complete" : state.theoryEarned() ? "available beyond the gate" : "not yet available")
                        + ". Dialog: " + (dialogEnabled ? "enabled" : "command fallback only") + "."));
                case "theory" -> {
                    if (args.length < 2) player.sendMessage(Component.text("Incomplete. Usage: /obscase theory <short account>"));
                    else handleTheory(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                }
                case "replay" -> player.sendMessage(Component.text(state.theoryEarned()
                        ? "Changed place: the civic threshold is open. Work cards and penalty copies can now be separated beyond it."
                        : "No accepted account has changed the civic threshold yet."));
                default -> player.sendMessage(Component.text("Unknown action. Use /obscase help."));
            }
        }

        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
            if (args.length <= 1) return List.of("help", "open", "status", "theory", "replay");
            return List.of();
        }

        @Override public boolean canUse(CommandSender sender) {
            return sender.hasPermission("observance.arg.case");
        }
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[^A-Za-z0-9_.:/=;,' -]", "_");
    }

    private static final class RefusalWindow {
        private final long expiresAt;
        private int count;
        private RefusalWindow(long expiresAt, int count) { this.expiresAt = expiresAt; this.count = count; }
    }

    @Override public void close() { }
}
