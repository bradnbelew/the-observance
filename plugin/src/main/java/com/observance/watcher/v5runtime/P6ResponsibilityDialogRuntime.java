package com.observance.watcher.v5runtime;

import com.observance.watcher.ObservancePlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Real Paper 1.21.11 structured input for P6's shared responsibility docket.
 *
 * <p>The six professional rooms carry evidence; they never grant eligibility. A player who has a
 * defensible model may open this form directly, including when another player supplied the clues.
 * The command recovery path calls the same coordinator predicate.</p>
 */
public final class P6ResponsibilityDialogRuntime implements Listener, AutoCloseable {
    static final Key SUBMIT_CLICK = Key.key("observance:p6/responsibility-docket");
    private static final long REFUSAL_WINDOW_MILLIS = 60_000L;
    private static final int REFUSAL_LIMIT = 3;

    private final ObservancePlugin plugin;
    private final V5RuntimeCoordinator coordinator;
    private final Map<UUID, RefusalWindow> refusals = new ConcurrentHashMap<>();
    private boolean started;

    public P6ResponsibilityDialogRuntime(
            ObservancePlugin plugin, V5RuntimeCoordinator coordinator) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    public void start() {
        if (started) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
    }

    /** Opens the primary form. Failure falls back to the proper bounded command, never chat. */
    public void open(Player player) {
        Objects.requireNonNull(player, "player");
        if (!coordinator.storyInputsEnabled()) {
            player.sendMessage(Component.text("The responsibility docket is closed. Nothing changed."));
            return;
        }
        try {
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Keeper responsibility docket"))
                            .canCloseWithEscape(true)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "For each person, note what they proved, what they compromised, and the later correction they left. Use your own short words. There is no hidden sentence. Esc cancels."))))
                            .inputs(List.of(
                                    row("vaun", "Vaun — proof / compromise / correction"),
                                    row("mara", "Mara — proof / compromise / correction"),
                                    row("sella", "Sella — proof / compromise / correction"),
                                    row("orin", "Orin — proof / compromise / correction"),
                                    row("brann", "Brann — proof / compromise / correction"),
                                    row("iss", "Iss — proof / compromise / correction")))
                            .build())
                    .type(DialogType.notice(ActionButton.builder(Component.text("Attach these findings"))
                            .tooltip(Component.text(
                                    "Checks six separate responsibility rows. Source clicks are not required."))
                            .width(180)
                            .action(DialogAction.customClick(SUBMIT_CLICK, null))
                            .build())));
            player.showDialog(dialog);
        } catch (LinkageError | RuntimeException unavailable) {
            plugin.getLogger().warning("P6 Paper Dialog unavailable; using command recovery: "
                    + safe(unavailable.getMessage()));
            player.sendMessage(Component.text(
                    "The docket form could not open. Use /obsfinding p6-recovery <Vaun | Mara | Sella | Orin | Brann | Iss>."));
        }
    }

    private static TextDialogInput row(String key, String label) {
        return DialogInput.text(key, Component.text(label))
                .width(320)
                .maxLength(P6ResponsibilityPredicate.MAX_FIELD_LENGTH)
                .labelVisible(true)
                .build();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDialogResponse(PlayerCustomClickEvent event) {
        if (!SUBMIT_CLICK.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();
        DialogResponseView response = event.getDialogResponseView();
        if (response == null) {
            player.sendMessage(Component.text(
                    "The docket returned no findings. Nothing changed. Use /obsfinding p6-recovery as recovery."));
            return;
        }
        submit(player, new P6ResponsibilityPredicate.Matrix(
                response.getText("vaun"), response.getText("mara"),
                response.getText("sella"), response.getText("orin"),
                response.getText("brann"), response.getText("iss")));
    }

    private void submit(Player player, P6ResponsibilityPredicate.Matrix matrix) {
        RefusalWindow window = refusals.compute(player.getUniqueId(), (ignored, current) ->
                current == null || current.expiresAt < System.currentTimeMillis()
                        ? new RefusalWindow(System.currentTimeMillis() + REFUSAL_WINDOW_MILLIS, 0)
                        : current);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage(Component.text(
                    "The docket is throttled for a short time. Evidence and world state are unchanged."));
            return;
        }
        if (blank(matrix.vaun()) || blank(matrix.mara()) || blank(matrix.sella())
                || blank(matrix.orin()) || blank(matrix.brann()) || blank(matrix.iss())) {
            player.sendMessage(Component.text(
                    "Incomplete. Give one short responsibility row for each person. Nothing changed."));
            return;
        }
        switch (coordinator.submitP6ResponsibilityMatrix(matrix)) {
            case ACCEPTED -> {
                refusals.remove(player.getUniqueId());
                player.sendMessage(Component.text(
                        "Accepted. Six separate responsibilities are retained locally. No source-click or affidavit receipt was required."));
            }
            case ALREADY_ACCEPTED -> player.sendMessage(Component.text(
                    "That responsibility model is already accepted and retained locally."));
            case NOT_READY -> player.sendMessage(Component.text(
                    "The P5 public-record correction must be complete before this model can be attached. Nothing changed."));
            case WRONG -> {
                window.count++;
                player.sendMessage(Component.text(
                        "At least one row lacks a distinct proof, compromise, or later correction, or belongs to another person. Nothing changed."));
            }
            case FAILED -> player.sendMessage(Component.text(
                    "The docket failed safely. Nothing changed; use /obsfinding p6-recovery after recovery."));
        }
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        refusals.clear();
        started = false;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[\\r\\n]+", " ");
    }

    private static final class RefusalWindow {
        private final long expiresAt;
        private int count;

        private RefusalWindow(long expiresAt, int count) {
            this.expiresAt = expiresAt;
            this.count = count;
        }
    }
}
