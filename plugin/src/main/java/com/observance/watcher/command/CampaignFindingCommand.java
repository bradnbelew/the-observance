package com.observance.watcher.command;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.v5runtime.P6ResponsibilityPredicate;
import com.observance.watcher.v5runtime.P7NessaCorrectionPredicate;
import com.observance.watcher.v5runtime.P8InterventionPlanPredicate;
import com.observance.watcher.v5runtime.P9CampPredicate;
import com.observance.watcher.v5runtime.P10WrenTransmissionPredicate;
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
                player.sendMessage("P3 resident dispatch: " + (runtime.p3DispatchAccepted()
                        ? "accepted; both accounts remain open." : "not yet accepted."));
                player.sendMessage("P7 Nessa correction: " + (runtime.p7NessaCleared()
                        ? "accepted and public." : "not yet accepted."));
                player.sendMessage("P8 intervention plan: " + (runtime.p8InterventionPlanAccepted()
                        ? "accepted and retained locally." : "not yet accepted."));
                player.sendMessage("P9 camp owners / private window: "
                        + (runtime.p9BiographiesAccepted() ? "owners accepted" : "owners open") + " / "
                        + (runtime.p9LeakWindowAccepted() ? "window accepted." : "window open."));
                player.sendMessage("P10 supported attribution: " + (runtime.p10WrenFindingAccepted()
                        ? "accepted; Wren's response is available." : "not yet accepted."));
                player.sendMessage("P11 identity artifact: " + (runtime.p11AverynIdentified()
                        ? "accepted and retained locally." : "not yet accepted."));
            }
            case "replay" -> {
                player.sendMessage(runtime.p6ResponsibilityAccepted()
                        ? "Retained P6 model: six different people, professions, proofs, compromises, and later corrections; no single Keeper mind."
                        : "No accepted P6 responsibility model is available to replay.");
                player.sendMessage(runtime.p3DispatchAccepted()
                        ? "Retained P3 action: the changed practical mark has conflicting resident accounts; both remain preserved without an official winner."
                        : "No P3 open-disagreement dispatch is retained.");
                player.sendMessage(runtime.p8InterventionPlanAccepted()
                        ? "Retained P8 plan: the pre-Break record-edit pattern remains in the interacting-cause model; Iss's surface evidence remains valid while his route was unsafe; copy behavior is proven while the Dark remains unidentified; works order is filter, paired light, pressure bypass, then staff route."
                        : "No accepted P8 intervention plan is available to replay.");
                player.sendMessage(runtime.p7NessaCleared()
                        ? "Retained P7 correction: genuine cloth diverted; substitute failed first at the lower intake; relief and complaint chronology edited; Nessa followed procedure and reported before failure."
                        : "No accepted P7 public correction is retained.");
                player.sendMessage(runtime.p9LeakWindowAccepted()
                        ? "Retained P9 finding: four people restored; private counter-mark, Witness Spool intake, then public upload; inside access proven, sender still open."
                        : "P9 replay remains available on Copperline; no complete local private-window finding is retained.");
                player.sendMessage(runtime.p10WrenFindingAccepted()
                        ? "Retained P10 finding: progressive private knowledge and a missing physical counter-mark identify Wren; fear explains the choice but does not excuse it."
                        : "No supported P10 transmission finding is retained.");
                player.sendMessage(runtime.p11AverynIdentified()
                        ? "Retained P11 identity: AVERYN, a person and registrar rather than a seventh Keeper."
                        : "No P11 identity artifact is retained.");
            }
            case "p3-dispatch" -> submitP3Dispatch(player, args, runtime);
            case "p6-docket" -> runtime.openP6ResponsibilityDocket(player);
            case "p6-recovery" -> submitP6Recovery(player, label, args, runtime);
            case "p7-nessa" -> submitP7Nessa(player, args, runtime);
            case "p8" -> submitP8(player, label, args, runtime);
            case "p9-people" -> submitP9People(player, label, args, runtime);
            case "p9-window" -> submitP9Window(player, label, args, runtime);
            case "p10-wren" -> submitP10Wren(player, args, runtime);
            case "p11-name" -> submitP11Name(player, args, runtime);
            default -> help(player, label);
        }
        return true;
    }

    private void submitP3Dispatch(Player player, String[] args, V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String finding = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (finding.isBlank()) {
            player.sendMessage("Incomplete. Name the disagreement and say that both accounts remain open.");
            return;
        }
        respond(player, runtime.submitP3SettlementDispatch(finding), window,
                "Dispatch accepted. Both resident accounts stay preserved and the covered survey can proceed.",
                "The resident accounts must be available in the live world first.",
                "The note must identify a practical disagreement and preserve both accounts without choosing an official version.");
    }

    private void submitP7Nessa(Player player, String[] args, V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = fields(args);
        if (fields.length != 3) {
            player.sendMessage("Incomplete. Give material cause/place | record changes | Nessa's conduct/timing.");
            return;
        }
        var finding = new P7NessaCorrectionPredicate.Finding(fields[0], fields[1], fields[2]);
        respond(player, runtime.submitP7NessaCorrection(finding), window,
                "The public correction is fixed. Nessa is cleared by cause, record history, and conduct.",
                "The material comparison must establish the failed cloth and first failure place.",
                "The correction does not yet separate material cause, edited chronology, and Nessa's conduct.");
    }

    private void submitP10Wren(Player player, String[] args, V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String[] fields = fields(args);
        if (fields.length != 3) {
            player.sendMessage("Incomplete. Give provenance proof | packet progression | motive boundary.");
            return;
        }
        var finding = new P10WrenTransmissionPredicate.Finding(fields[0], fields[1], fields[2]);
        respond(player, runtime.submitP10WrenTransmission(finding), window,
                "The supported attribution is fixed. Wren answers it; remembrance remains a separate choice.",
                "The private version window must be preserved first.",
                P10WrenTransmissionPredicate.response(finding).message());
    }

    private void submitP11Name(Player player, String[] args, V5RuntimeCoordinator runtime) {
        RefusalWindow window = refusalWindow(player);
        if (window.count >= REFUSAL_LIMIT) {
            player.sendMessage("The finding desk is throttled for a short time. Evidence and world state are unchanged.");
            return;
        }
        String artifact = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        respond(player, runtime.submitP11AverynIdentity(artifact), window,
                "Identity restored: Averyn is retained as a person and registrar, not a seventh Keeper.",
                "The group's Wren remembrance must be committed first.",
                "That artifact does not match the six independent routes.");
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
        if (fields.length != 3) {
            player.sendMessage("Incomplete. Give archive treatment | readiness | claim boundary.");
            return;
        }
        var finding = new P9CampPredicate.Window(
                fields[0], fields[1], fields[2]);
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
                String rows = String.join(", ", P6ResponsibilityPredicate.unsupportedRows(matrix));
                player.sendMessage("Review these rows: " + rows
                        + ". Each must connect that person's proof, compromise, and later correction. Nothing changed.");
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
                player.sendMessage("Review: " + String.join(", ",
                        P8InterventionPlanPredicate.unsupportedComponents(plan)) + ". Nothing changed.");
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
        player.sendMessage("P3 accepts one short open finding: /" + label
                + " p3-dispatch <what the accounts disagree about and why both stay open>.");
        player.sendMessage("P6 uses a six-row Paper form: /" + label + " p6-docket. Each row states that person's proof, compromise, and later correction.");
        player.sendMessage("If the form is unavailable, use /" + label + " p6-recovery <Vaun | Mara | Sella | Orin | Brann | Iss>.");
        player.sendMessage("P7 accepts three short findings: /" + label
                + " p7-nessa <material cause/place> | <record changes> | <Nessa conduct/timing>.");
        player.sendMessage("P8 accepts four short findings, not one exact sentence:");
        player.sendMessage("/" + label + " p8 <interacting causes and earlier correction> | <Iss evidence and unsafe act> | <what the copy proves and leaves open> | <safe works order>");
        player.sendMessage("P9 local recovery mirrors Copperline's real forms: /" + label + " p9-people <mkept | Ash | Rook | Wren traces>.");
        player.sendMessage("Then /" + label + " p9-window <archive treatment | readiness | strongest supported claim>.");
        player.sendMessage("P10 accepts three short findings: /" + label
                + " p10-wren <private-channel proof> | <packet progression> | <motive boundary>.");
        player.sendMessage("P11 accepts the six-letter artifact: /" + label + " p11-name <artifact>.");
        player.sendMessage("Use /" + label + " status or /" + label + " replay. Source clicks are never required.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) return List.of(
                "help", "status", "replay", "p3-dispatch", "p6-docket", "p6-recovery", "p7-nessa", "p8", "p9-people", "p9-window",
                "p10-wren", "p11-name");
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
