package com.observance.watcher.command;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.v5runtime.P6ResponsibilityPredicate;
import com.observance.watcher.v5runtime.P8InterventionPlanPredicate;
import com.observance.watcher.v5runtime.P9CampPredicate;
import com.observance.watcher.v5runtime.V5RuntimeCoordinator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** Stable command fallback for short structured campaign findings; ordinary chat is never parsed. */
public final class CampaignFindingCommand implements CommandExecutor, TabCompleter {
    private static final long REFUSAL_WINDOW_MILLIS = 60_000L;
    private static final int REFUSAL_LIMIT = 3;
    private final ObservancePlugin plugin;
    private final Map<UUID, RefusalWindow> refusals = new ConcurrentHashMap<>();

    public CampaignFindingCommand(ObservancePlugin plugin) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This is a player finding surface.");
            return true;
        }
        if (!player.hasPermission("observance.arg.finding")) {
            player.sendMessage("The finding desk is not available from here.");
            return true;
        }
        V5RuntimeCoordinator runtime = plugin.v5Runtime();
        if (runtime == null || !runtime.storyInputsEnabled()) {
            player.sendMessage("The finding desk is closed. Nothing changed.");
            return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "help" -> help(player, label);
            case "status" -> {
                player.sendMessage("P6 responsibility matrix: " + (runtime.p6ResponsibilityAccepted()
                        ? "accepted and retained locally." : "not yet accepted."));
                player.sendMessage("P8 intervention plan: " + (runtime.p8InterventionPlanAccepted()
                        ? "accepted and retained locally." : "not yet accepted."));
                player.sendMessage("P9 camp owners / private window: "
                        + (runtime.p9BiographiesAccepted() ? "owners accepted" : "owners open") + " / "
                        + (runtime.p9LeakWindowAccepted() ? "window accepted." : "window open."));
            }
            case "replay" -> {
                player.sendMessage(runtime.p6ResponsibilityAccepted()
                        ? "Retained P6 model: six different people, professions, proofs, compromises, and later corrections; no single Keeper mind."
                        : "No accepted P6 responsibility model is available to replay.");
                player.sendMessage(runtime.p8InterventionPlanAccepted()
                        ? "Retained P8 plan: four interacting causes; Iss's surface evidence remains valid while his route was unsafe; copy behavior is proven while the Dark remains unidentified; works order is filter, paired light, pressure bypass, then staff route."
                        : "No accepted P8 intervention plan is available to replay.");
                player.sendMessage(runtime.p9LeakWindowAccepted()
                        ? "Retained P9 finding: four people restored; private counter-mark, Witness Spool intake, then public upload; inside access proven, sender still open."
                        : "P9 replay remains available on Copperline; no complete local private-window finding is retained.");
            }
            case "p6-recovery" -> submitP6Recovery(player, label, args, runtime);
            case "p8" -> submitP8(player, label, args, runtime);
            case "p9-people" -> submitP9People(player, label, args, runtime);
            case "p9-window" -> submitP9Window(player, label, args, runtime);
            default -> help(player, label);
        }
        return true;
    }

    private void submitP9People(Player player, String label, String[] args,
                                V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = fields(args);
        if (fields.length != 4) {
            player.sendMessage("Incomplete. Give four owner traces in mkept, Ash, Rook, Wren order, separated by |.");
            return;
        }
        var people = new P9CampPredicate.People(fields[0], fields[1], fields[2], fields[3]);
        respond(player, runtime.submitP9CampPeople(people), window,
                "Four people restored to the camp record. Shared objects remain shared.",
                "Restore or correctly identify all four people before preserving the private window.",
                "At least one work/relationship trace belongs to another person.");
    }

    private void submitP9Window(Player player, String label, String[] args,
                                V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = fields(args);
        if (fields.length != 5) {
            player.sendMessage("Incomplete. Give before | crossing | after | readiness | claim boundary.");
            return;
        }
        var finding = new P9CampPredicate.Window(
                fields[0], fields[1], fields[2], fields[3], fields[4]);
        respond(player, runtime.submitP9LeakWindow(finding), window,
                "Private version window preserved. It proves inside access and leaves the sender open.",
                "The four camp owner cards must be restored first.",
                "The clock order, release state, or claim boundary conflicts with the preserved copies.");
    }

    private void submitP6Recovery(Player player, String label, String[] args,
                                  V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).split("\\|", -1);
        if (fields.length != 6) {
            player.sendMessage("Incomplete. Give six short rows in Keeper order, separated by |. Use /"
                    + label + " help.");
            return;
        }
        var matrix = new P6ResponsibilityPredicate.Matrix(
                fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
        V5RuntimeCoordinator.PlanSubmission result = runtime.submitP6ResponsibilityMatrix(matrix);
        switch (result) {
            case ACCEPTED -> {
                refusals.remove(player.getUniqueId());
                player.sendMessage("Accepted. Six separate responsibilities are retained locally. No source-click or affidavit receipt was required.");
            }
            case ALREADY_ACCEPTED -> player.sendMessage("That responsibility model is already accepted and retained locally.");
            case NOT_READY -> player.sendMessage("The P5 public-record correction must be complete before this model can be attached. Nothing changed.");
            case WRONG -> {
                window.count++;
                player.sendMessage("At least one row lacks a distinct proof, compromise, or later correction, or is assigned to the wrong person. Nothing changed.");
            }
            case FAILED -> player.sendMessage("The finding desk failed safely. Nothing changed; retry after recovery.");
        }
    }

    private void submitP8(Player player, String label, String[] args, V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).split("\\|", -1);
        if (fields.length != 4) {
            player.sendMessage("Incomplete. Give four short fields separated by |. Use /" + label + " help.");
            return;
        }
        var plan = new P8InterventionPlanPredicate.Plan(fields[0], fields[1], fields[2], fields[3]);
        V5RuntimeCoordinator.PlanSubmission result = runtime.submitP8InterventionPlan(plan);
        switch (result) {
            case ACCEPTED -> {
                refusals.remove(player.getUniqueId());
                player.sendMessage("Accepted. The bounded causal model is retained locally. No source-click receipt was required.");
            }
            case ALREADY_ACCEPTED -> player.sendMessage("That intervention plan is already accepted and retained locally.");
            case NOT_READY -> player.sendMessage("The current Nessa correction must be public before this plan can be attached. Nothing changed.");
            case WRONG -> {
                window.count++;
                player.sendMessage("One or more fields do not fit the surviving evidence, or the works order is unsafe. Nothing changed.");
            }
            case FAILED -> player.sendMessage("The finding desk failed safely. Nothing changed; retry or use Copperline when it returns.");
        }
    }

    private void respond(Player player, V5RuntimeCoordinator.PlanSubmission result,
                         RefusalWindow window, String accepted, String notReady, String wrong) {
        switch (result) {
            case ACCEPTED -> {
                refusals.remove(player.getUniqueId());
                player.sendMessage(accepted + " No source-click receipt was required.");
            }
            case ALREADY_ACCEPTED -> player.sendMessage("That finding is already accepted and retained locally.");
            case NOT_READY -> player.sendMessage(notReady + " Nothing changed.");
            case WRONG -> {
                window.count++;
                player.sendMessage(wrong + " Nothing changed.");
            }
            case FAILED -> player.sendMessage("The finding desk failed safely. Nothing changed; retry after recovery.");
        }
    }

    private static String[] fields(String[] args) {
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length)).split("\\|", -1);
    }

    private static void help(Player player, String label) {
        player.sendMessage("P6 keyboard recovery accepts six short rows in this order: Vaun | Mara | Sella | Orin | Brann | Iss.");
        player.sendMessage("Each row states that person's proof, compromise, and later correction. Use /" + label + " p6-recovery <six rows>.");
        player.sendMessage("P8 accepts four short findings, not one exact sentence:");
        player.sendMessage("/" + label + " p8 <interacting causes> | <Iss evidence and unsafe act> | <what the copy proves and leaves open> | <safe works order>");
        player.sendMessage("P9 local recovery mirrors Copperline's real forms: /" + label + " p9-people <mkept | Ash | Rook | Wren traces>.");
        player.sendMessage("Then /" + label + " p9-window <before | crossing | after | readiness | strongest supported claim>.");
        player.sendMessage("Use /" + label + " status or /" + label + " replay. Source clicks are never required.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) return List.of(
                "help", "status", "replay", "p6-recovery", "p8", "p9-people", "p9-window");
        return List.of();
    }

    private RefusalWindow refusalWindow(Player player) {
        return refusals.compute(player.getUniqueId(), (ignored, current) ->
                current == null || current.expiresAt < System.currentTimeMillis()
                        ? new RefusalWindow(System.currentTimeMillis() + REFUSAL_WINDOW_MILLIS, 0) : current);
    }

    private static final class RefusalWindow {
        private final long expiresAt;
        private int count;
        private RefusalWindow(long expiresAt, int count) { this.expiresAt = expiresAt; this.count = count; }
    }
}
