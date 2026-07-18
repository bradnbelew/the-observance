package com.observance.watcher.arg;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
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
import org.bukkit.scheduler.BukkitTask;

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
    private BukkitTask mirrorTask;

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
        mirrorKnownStateAsync();
        if (plugin instanceof ObservancePlugin observance) {
            mirrorTask = observance.scheduler().runAsyncTimerSafe(
                    "arg.event.mirror", 20L * 10L, 20L * 10L, this::mirrorKnownStateBlocking);
        }
    }

    public void projectState() {
        world.setGate(state.theoryEarned());
        world.setP5CurationState(state.serviceCardsPublic(), state.penaltyCopiesInCustody(), state.curated());
    }

    private void mirrorKnownStateAsync() {
        if (!(plugin instanceof ObservancePlugin observance)) return;
        observance.scheduler().runAsyncSafe("arg.event.mirror.now", this::mirrorKnownStateBlocking);
    }

    /** Network thread only. Every payload/idempotency pair is canonical and restart-stable. */
    private void mirrorKnownStateBlocking() {
        if (!(plugin instanceof ObservancePlugin observance) || observance.supabase() == null
                || !observance.supabase().isConfigured()) return;
        if (state.theoryEarned()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("purpose", "ordinary_refuge");
            payload.addProperty("change", "safety_to_control");
            payload.addProperty("anomaly", "copy_before_source");
            observance.supabase().recordArgEvent(
                    ArgVerticalSliceState.THEORY_EVENT,
                    "minecraft:p4:control-reversal-earned",
                    "minecraft", null, payload);
        }
        if (state.copyOrderTested()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("method", "barcode-and-node-clock");
            payload.addProperty("guest_metadata", "excluded");
            observance.supabase().recordArgEvent(
                    ArgVerticalSliceState.COPY_TEST_EVENT,
                    "minecraft:p4:barcode-node-clock-test",
                    "minecraft", null, payload);
        }
        if (state.serviceChronologyShared()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("service_cards", "public");
            payload.addProperty("penalty_copies", "evidence_custody");
            observance.supabase().recordArgEvent(
                    ArgVerticalSliceState.CHRONOLOGY_EVENT,
                    "minecraft:p5:service-chronology-shared",
                    "minecraft", null, payload);
        }
        if (state.curated()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("gallery", "recurated");
            observance.supabase().recordArgEvent(
                    ArgVerticalSliceState.CURATED_EVENT,
                    "minecraft:p5:civic-gallery-recurated",
                    "minecraft", null, payload);
        }
    }

    public boolean gateOpen() { return state.theoryEarned(); }
    public boolean curated() { return state.curated(); }

    /** Console-only disposable harness entry; uses the same predicate as Dialog and command input. */
    public ArgVerticalSliceState.TheoryResult auditTheory(String theory) throws IOException {
        ArgVerticalSliceState.TheoryResult result = state.submitConclusion(theory, "disposable-paper-audit");
        if (result == ArgVerticalSliceState.TheoryResult.ACCEPTED) {
            projectState();
            mirrorKnownStateAsync();
        }
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
        if (committed) mirrorKnownStateAsync();
        return result;
    }

    public String auditState() {
        return "theory=" + state.theoryEarned() + " copy_test=" + state.copyOrderTested()
                + " service_public=" + state.serviceCardsPublic()
                + " penalty_custody=" + state.penaltyCopiesInCustody() + " curated=" + state.curated()
                + " receipts=" + state.receipts().size();
    }

    public void openDesk(Player player) {
        if (!dialogEnabled) {
            player.sendMessage(Component.text("Use /obscase conclude <purpose> | <change> | <anomaly>. Use /obscase help for the input rules."));
            return;
        }
        try {
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Mouth copy review"))
                            .canCloseWithEscape(true)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "Answer three separate questions in your own words. There is no hidden sentence. Esc cancels. Source clicks are not required."))))
                            .inputs(List.of(
                                    DialogInput.text("purpose", Component.text("What was the Mouth for?"))
                                            .width(320).maxLength(64).labelVisible(true).build(),
                                    DialogInput.text("change", Component.text("What changed between the copies?"))
                                            .width(320).maxLength(96).labelVisible(true).build(),
                                    DialogInput.text("anomaly", Component.text("What remains unexplained?"))
                                            .width(320).maxLength(64).labelVisible(true).build()))
                            .build())
                    .type(DialogType.notice(ActionButton.builder(Component.text("Test these conclusions"))
                            .tooltip(Component.text("The server checks three meaning components and reports accepted, wrong, incomplete, or failed."))
                            .width(160).action(DialogAction.customClick(THEORY_CLICK, null)).build())));
            player.showDialog(dialog);
        } catch (LinkageError | RuntimeException unavailable) {
            plugin.getLogger().warning("Paper Dialog unavailable; using stable command fallback: " + safe(unavailable.getMessage()));
            player.sendMessage(Component.text("The review form could not open. Use /obscase conclude <purpose> | <change> | <anomaly>."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDialogResponse(PlayerCustomClickEvent event) {
        if (!THEORY_CLICK.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();
        DialogResponseView response = event.getDialogResponseView();
        if (response == null) {
            player.sendMessage(Component.text("The form returned no conclusions. Nothing changed. Use /obscase conclude as recovery."));
            return;
        }
        handleConclusion(player, response.getText("purpose"), response.getText("change"), response.getText("anomaly"));
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
            if (curated) mirrorKnownStateAsync();
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

    private void handleConclusion(Player player, String purpose, String change, String anomaly) {
        RefusalWindow window = refusals.compute(player.getUniqueId(), (ignored, current) ->
                current == null || current.expiresAt < System.currentTimeMillis()
                        ? new RefusalWindow(System.currentTimeMillis() + REFUSAL_WINDOW_MILLIS, 0) : current);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage(Component.text("The desk is throttled for a short time. Your evidence and world state are unchanged."));
            return;
        }
        try {
            ArgVerticalSliceState.TheoryResult result = state.submitConclusion(
                    purpose, change, anomaly, player.getUniqueId().toString());
            switch (result) {
                case ACCEPTED -> {
                    refusals.remove(player.getUniqueId());
                    world.setGate(true);
                    mirrorKnownStateAsync();
                    player.sendMessage(Component.text("Accepted. Your three claims identify the ordinary refuge, the change from safety to control, and the unresolved copy order. The civic threshold is open."));
                }
                case INCOMPLETE -> player.sendMessage(Component.text("Incomplete. Answer all three questions. Nothing changed in the world."));
                case WRONG -> {
                    window.count++;
                    player.sendMessage(Component.text("One or more claims do not fit the surviving evidence. Nothing changed."));
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
                case "help" -> player.sendMessage(Component.text("Use /obscase test-copy to run the local cartridge-order test; /obscase open or conclude records three short claims; status and replay read back shared state. No source-click receipt is required."));
                case "open" -> openDesk(player);
                case "test-copy" -> {
                    try {
                        state.testCopyOrder(player.getUniqueId().toString());
                        mirrorKnownStateAsync();
                        player.sendMessage(Component.text("Test complete. Cartridge 03 precedes 04 on the barcode record, and the independent node clock stays stable. Guest filenames and modified times were excluded."));
                    } catch (IOException failure) {
                        player.sendMessage(Component.text("The custody test failed safely. Nothing changed; retry is safe."));
                        plugin.getLogger().warning("P4 copy test commit failed: " + safe(failure.getMessage()));
                    }
                }
                case "status" -> player.sendMessage(Component.text("P4 copy test: " + (state.copyOrderTested() ? "complete" : "available")
                        + ". P4 theory: " + (state.theoryEarned() ? "accepted" : "open")
                        + ". P5 curation: " + (state.curated() ? "complete" : state.theoryEarned() ? "available beyond the gate" : "not yet available")
                        + ". Dialog: " + (dialogEnabled ? "enabled" : "command fallback only") + "."));
                case "conclude" -> {
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Incomplete. Usage: /obscase conclude <purpose> | <change> | <anomaly>"));
                    } else {
                        String[] fields = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).split("\\|", -1);
                        if (fields.length != 3) player.sendMessage(Component.text("Incomplete. Separate the three answers with | characters."));
                        else handleConclusion(player, fields[0], fields[1], fields[2]);
                    }
                }
                case "replay" -> {
                    if (state.copyOrderTested()) player.sendMessage(Component.text(
                            "Retained test: barcode 03 precedes 04; the recovery-node clock is stable; damaged guest metadata cannot reverse that order."));
                    player.sendMessage(Component.text(state.theoryEarned()
                            ? "Changed place: the civic threshold is open. Work cards and penalty copies can now be separated beyond it."
                            : "No accepted account has changed the civic threshold yet."));
                }
                default -> player.sendMessage(Component.text("Unknown action. Use /obscase help."));
            }
        }

        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
            if (args.length <= 1) return List.of("help", "open", "test-copy", "status", "conclude", "replay");
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

    @Override public void close() {
        if (mirrorTask != null) mirrorTask.cancel();
        mirrorTask = null;
    }
}
