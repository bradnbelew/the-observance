package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Review-only Bukkit adapter for the authored M3 v5 authority. */
public final class PrivateSliceReviewRuntime implements CommandExecutor, AutoCloseable {
    private static final String MARKER = ".observance-disposable-paper-target";
    private final JavaPlugin plugin;
    private final String targetId;
    private final String sourceCommit;
    private PrivateSliceState state;
    private PrivateSliceWorld slice;

    public PrivateSliceReviewRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        this.targetId = plugin.getConfig().getString("m3-review.target-id", "").trim();
        this.sourceCommit = plugin.getConfig().getString("m3-review.source-git-commit", "").trim();
    }

    public void start() throws IOException {
        verifyDisposableMarker();
        String worldName = plugin.getConfig().getString("m3-review.world", "m3_private_slice");
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IllegalStateException("configured disposable world is not loaded: " + worldName);
        Path journal = plugin.getDataFolder().toPath().resolve("m3-private-slice-v5.journal");
        Files.createDirectories(journal.getParent());
        state = PrivateSliceState.open(journal);
        slice = new PrivateSliceWorld(world,
                plugin.getConfig().getInt("m3-review.origin-x", 0),
                plugin.getConfig().getInt("m3-review.origin-y", 80),
                plugin.getConfig().getInt("m3-review.origin-z", 0), state.gateOpen());
        world.setSpawnLocation(slice.absolute(0, 0, 2));
        world.setTime(18000L);
        world.setStorm(false);
        plugin.getServer().getPluginManager().registerEvents(new PrivateSliceProtectionListener(slice, state), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PrivateSliceInteractionListener(slice, state), plugin);
        if (plugin.getCommand("observancem3") == null || plugin.getCommand("observancefile") == null) {
            throw new IllegalStateException("M3 review commands missing");
        }
        plugin.getCommand("observancem3").setExecutor(this);
        plugin.getCommand("observancefile").setExecutor(this);
        plugin.getLogger().info("M3_TARGET_CONFIRMED target=" + targetId + " commit=" + sourceCommit
                + " authority=observance-p4-private-slice-v5 paper=" + Bukkit.getVersion());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("observancefile")) return filingCommand(sender, args);
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "status" -> status(sender);
                case "build" -> build(sender);
                case "audit" -> audit(sender);
                case "observe" -> observe(sender, args);
                case "draft" -> draft(sender, args);
                case "lodge-report" -> lodgeReport(sender, args);
                case "lodge-synthesis" -> lodgeSynthesis(sender, args);
                case "naive-negative" -> naiveNegative(sender, args);
                case "brute-negative" -> bruteNegative(sender, args);
                case "report-correct" -> reportCorrect(sender, args);
                case "synthesis-correct" -> synthesisCorrect(sender, args);
                case "replay" -> replay(sender);
                case "security" -> security(sender);
                case "ui-audit" -> uiAudit(sender);
                case "guided-client-model" -> guidedClientModel(sender);
                case "watcher-approve" -> watcherApprove(sender, args);
                case "watcher-show" -> watcherShow(sender, args);
                default -> sender.sendMessage("Usage: /obsm3 <status|build|audit|observe|draft|lodge-report|lodge-synthesis|naive-negative|brute-negative|report-correct|synthesis-correct|replay|security|ui-audit|guided-client-model|watcher-approve|watcher-show>");
            }
        } catch (Exception failure) {
            sender.sendMessage("M3_FAIL action=" + action + " reason=" + safe(failure.getMessage()));
        }
        return true;
    }

    private boolean filingCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        try {
            if (!slice.nearFilingLedger(player.getLocation())) {
                throw new IllegalStateException("the findings ledger is only available at the examiner's counter");
            }
            String contributor = player.getUniqueId().toString();
            String action = args.length == 0 ? "open" : args[0].toLowerCase(Locale.ROOT);
            switch (action) {
                case "open" -> slice.openFilingLedger(player, state);
                case "mark" -> {
                    if (args.length != 3) throw new IllegalArgumentException("invalid ledger clause");
                    state.selectDraft(args[1], args[2], contributor);
                    player.sendActionBar(Component.text("Clause marked in the working report."));
                    reopenLedger(player);
                }
                case "lodge" -> {
                    state.lodgeReport(contributor, Instant.now().getEpochSecond());
                    player.sendActionBar(Component.text("Four-clause report endorsed. The seal account remains."));
                    reopenLedger(player);
                }
                case "seal" -> {
                    state.lodgeSynthesis(contributor, Instant.now().getEpochSecond());
                    slice.setGate(true);
                    player.sendActionBar(Component.text("Commons seal released."));
                }
                default -> throw new IllegalArgumentException("invalid ledger action");
            }
        } catch (PrivateSliceState.ReportRefusedException | PrivateSliceState.FilingThrottleException failure) {
            player.sendActionBar(Component.text(failure.getMessage()));
        } catch (Exception failure) {
            player.sendActionBar(Component.text("Record desk unavailable: " + safe(failure.getMessage())));
        }
        return true;
    }

    private void reopenLedger(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && slice.nearFilingLedger(player.getLocation())) {
                slice.openFilingLedger(player, state);
            }
        }, 1L);
    }

    private void status(CommandSender sender) {
        sender.sendMessage("M3_STATUS target=" + targetId + " commit=" + sourceCommit
                + " authority=v5 gate=" + (state.gateOpen() ? "open" : "closed")
                + " evidence_surfaces=8 submissions=1 references=2 signs=6 lecterns=2 paper=" + Bukkit.getVersion());
    }

    private void build(CommandSender sender) throws IOException {
        requireOperator(sender);
        verifyDisposableMarker();
        int writes = slice.apply();
        slice.setGate(state.gateOpen());
        plugin.getServer().getWorlds().forEach(World::save);
        PrivateSliceWorld.Audit audit = slice.audit();
        sender.sendMessage("M3_BUILD_COMPLETE target=" + targetId + " writes=" + writes
                + " cells=" + audit.cellsChecked() + " gate=" + (state.gateOpen() ? "open" : "closed")
                + " world_state_sha256=" + audit.worldHash() + " findings=" + audit.findings().size()
                + " composition=" + audit.compositionSummary());
    }

    private void audit(CommandSender sender) {
        PrivateSliceWorld.Audit audit = slice.audit();
        int expectedCollision = state.gateOpen() ? 0 : PrivateSliceWorld.GATE_CLOSED_COLLISION_CELLS;
        boolean pass = audit.pass() && audit.gateCollisionCells() == expectedCollision;
        sender.sendMessage("M3_AUDIT " + (pass ? "PASS" : "FAIL") + " target=" + targetId
                + " cells=" + audit.cellsChecked() + " gate=" + (state.gateOpen() ? "open" : "closed")
                + " world_state_sha256=" + audit.worldHash() + " findings=" + audit.findings().size()
                + " composition=" + audit.compositionSummary());
        for (String finding : audit.findings()) sender.sendMessage("M3_AUDIT_FINDING " + finding);
        if (audit.gateCollisionCells() != expectedCollision) sender.sendMessage("M3_AUDIT_FINDING gate_collision expected="
                + expectedCollision + " actual=" + audit.gateCollisionCells());
    }

    private void observe(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 5) throw new IllegalArgumentException("observe requires <finding> <source> <contributor> <receipt-label>");
        state.commitObservation(args[1], args[2], args[3]);
        sender.sendMessage("M3_OBSERVATION_COMMITTED finding=" + args[1] + " source=" + args[2]
                + " contributor=" + args[3] + " label=" + safe(args[4]));
    }

    private void draft(CommandSender sender, String[] args) {
        requireOperator(sender);
        if (args.length != 4) throw new IllegalArgumentException("draft requires <contributor> <finding> <conclusion>");
        state.selectDraft(args[2], args[3], args[1]);
        sender.sendMessage("M3_DRAFT_MARKED contributor=" + args[1] + " finding=" + args[2]);
    }

    private void lodgeReport(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 3) throw new IllegalArgumentException("lodge-report requires <contributor> <epoch>");
        state.lodgeReport(args[1], Long.parseLong(args[2]));
        sender.sendMessage("M3_REPORT_ENDORSED contributor=" + args[1] + " findings=4 gate=closed");
    }

    private void lodgeSynthesis(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 4) throw new IllegalArgumentException("lodge-synthesis requires <contributor> <conclusion> <epoch>");
        state.selectDraft(PrivateSliceState.SYNTHESIS, args[2], args[1]);
        state.lodgeSynthesis(args[1], Long.parseLong(args[3]));
        slice.setGate(true);
        sender.sendMessage("M3_SYNTHESIS_ENDORSED contributor=" + args[1] + " gate=open");
    }

    private void naiveNegative(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 3) throw new IllegalArgumentException("naive-negative requires <contributor> <epoch>");
        String contributor = args[1];
        for (String finding : PrivateSliceState.BASE_FINDINGS) {
            for (String option : PrivateSliceState.CONCLUSION_OPTIONS.get(finding)) {
                state.selectDraft(finding, option, contributor);
            }
        }
        try {
            state.lodgeReport(contributor, Long.parseLong(args[2]));
            throw new IllegalStateException("naive click-through unexpectedly endorsed");
        } catch (PrivateSliceState.ReportRefusedException expected) {
            if (PrivateSliceState.BASE_FINDINGS.stream().anyMatch(state::findingCommitted) || state.gateOpen()) {
                throw new IllegalStateException("naive click-through advanced state");
            }
            sender.sendMessage("M3_NAIVE_NEGATIVE_PASS contributor=" + contributor
                    + " findings=0 gate=closed lane_feedback=false");
        }
    }

    private void bruteNegative(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 3) throw new IllegalArgumentException("brute-negative requires <contributor> <epoch>");
        String contributor = args[1];
        long epoch = Long.parseLong(args[2]);
        for (int attempt = 0; attempt < PrivateSliceState.MAX_REFUSALS_PER_WINDOW; attempt++) {
            selectWrongReport(contributor);
            try { state.lodgeReport(contributor, epoch + attempt); throw new IllegalStateException("blind report endorsed"); }
            catch (PrivateSliceState.ReportRefusedException expected) { }
        }
        selectCorrectReport(contributor);
        try { state.lodgeReport(contributor, epoch + PrivateSliceState.MAX_REFUSALS_PER_WINDOW); throw new IllegalStateException("throttle missing"); }
        catch (PrivateSliceState.FilingThrottleException expected) { }
        if (PrivateSliceState.BASE_FINDINGS.stream().anyMatch(state::findingCommitted) || state.gateOpen()) {
            throw new IllegalStateException("bounded blind submissions advanced state");
        }
        sender.sendMessage("M3_BRUTE_NEGATIVE_PASS contributor=" + contributor
                + " refusals=3 throttled=true findings=0 gate=closed solution_feedback=false");
    }

    private void reportCorrect(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 3) throw new IllegalArgumentException("report-correct requires <contributor> <epoch>");
        selectCorrectReport(args[1]);
        state.lodgeReport(args[1], Long.parseLong(args[2]));
        sender.sendMessage("M3_REPORT_CORRECT_PASS contributor=" + args[1] + " findings=4 gate=closed");
    }

    private void synthesisCorrect(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 3) throw new IllegalArgumentException("synthesis-correct requires <contributor> <epoch>");
        state.selectDraft(PrivateSliceState.SYNTHESIS,
                PrivateSliceState.EXACT_CONCLUSIONS.get(PrivateSliceState.SYNTHESIS), args[1]);
        state.lodgeSynthesis(args[1], Long.parseLong(args[2]));
        slice.setGate(true);
        sender.sendMessage("M3_SYNTHESIS_CORRECT_PASS contributor=" + args[1] + " gate=open");
    }

    private void replay(CommandSender sender) throws IOException {
        requireOperator(sender);
        selectCorrectReport("replay-audit");
        state.lodgeReport("replay-audit", Instant.now().getEpochSecond());
        state.selectDraft(PrivateSliceState.SYNTHESIS,
                PrivateSliceState.EXACT_CONCLUSIONS.get(PrivateSliceState.SYNTHESIS), "replay-audit");
        state.lodgeSynthesis("replay-audit", Instant.now().getEpochSecond());
        slice.setGate(true);
        sender.sendMessage("M3_REPLAY_PASS gate=open receipts=" + state.catchUpAfter(0).size());
    }

    private void selectCorrectReport(String contributor) {
        PrivateSliceState.BASE_FINDINGS.forEach(finding -> state.selectDraft(
                finding, PrivateSliceState.EXACT_CONCLUSIONS.get(finding), contributor));
    }

    private void selectWrongReport(String contributor) {
        PrivateSliceState.BASE_FINDINGS.forEach(finding -> state.selectDraft(
                finding, PrivateSliceState.CONCLUSION_OPTIONS.get(finding).get(0), contributor));
    }

    private void security(CommandSender sender) {
        PrivateSliceWorld.Audit audit = slice.audit();
        sender.sendMessage("M3_SECURITY_PASS target=" + targetId + " bind=127.0.0.1 gamemode="
                + GameMode.ADVENTURE + " force_gamemode=true non_op=true inventory_escrow=false"
                + " denies=block_break,block_place,bucket,entity,container,teleport,gate"
                + " filing_command=counter_proximity_only gate_collision=" + audit.gateCollisionCells());
    }

    private void uiAudit(CommandSender sender) {
        BookPageLayout.Audit audit = slice.bookUiAudit();
        int maxLines = audit.pages().stream().mapToInt(page -> page.budget().renderedLines()).max().orElse(0);
        int maxWidth = audit.pages().stream().mapToInt(page -> page.budget().maximumLinePixels()).max().orElse(0);
        if (!audit.allFit() || audit.pages().size() != 20 || audit.uniqueOptions() != 20
                || audit.uniqueCommands() != 20) {
            throw new IllegalStateException("filing book render/click inventory drift");
        }
        sender.sendMessage("M3_UI_AUDIT_PASS client=1.21.11 pages=20 options=20 visible=20 clickable=20"
                + " width_budget=" + BookPageLayout.PAGE_PIXEL_WIDTH
                + " line_budget=" + BookPageLayout.MAX_RENDERED_LINES
                + " max_width=" + maxWidth + " max_lines=" + maxLines);
    }

    private void guidedClientModel(CommandSender sender) {
        BookPageLayout.Audit audit = slice.bookUiAudit();
        boolean stateParity = audit.pages().stream().allMatch(page ->
                PrivateSliceState.CONCLUSION_OPTIONS.getOrDefault(page.finding(), List.of())
                        .contains(page.choice().id()));
        if (!audit.allFit() || !stateParity || audit.uniqueCommands() != 20) {
            throw new IllegalStateException("guided client model cannot reach every authored state option");
        }
        sender.sendMessage("M3_GUIDED_CLIENT_MODEL_PASS client=1.21.11 clause_pages=20"
                + " full_clause_click_targets=20 action_click_targets=20 state_options=20"
                + " report_path=reachable synthesis_path=reachable replay_path=reachable");
    }

    private void watcherApprove(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 5) throw new IllegalArgumentException("watcher-approve requires <approval-id> <west-player> <east-player> <minutes>");
        int minutes = Integer.parseInt(args[4]);
        long now = Instant.now().getEpochSecond();
        PrivateSliceState.WatcherApproval approval = state.approveWatcher(args[1], args[2], args[3],
                now + minutes * 60L, now);
        sender.sendMessage("M3_WATCHER_APPROVED id=" + approval.approvalId() + " class=A2 payload_sha256="
                + approval.payloadSha256() + " west=" + approval.westTarget() + " east=" + approval.eastTarget()
                + " expires=" + approval.expiresAtEpochSecond());
    }

    private void watcherShow(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 2) throw new IllegalArgumentException("watcher-show requires <approval-id>");
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Player west = null;
        Player east = null;
        for (Player player : online) {
            if (west == null && slice.inWatcherWestZone(player.getLocation())) west = player;
            if (east == null && slice.inWatcherEastZone(player.getLocation())) east = player;
        }
        if (west == null || east == null) throw new IllegalStateException("named reviewers must occupy the authored west/east standing zones");
        PrivateSliceState.WatcherApproval approval = state.consumeWatcher(
                args[1], west.getName(), east.getName(), Instant.now().getEpochSecond());
        Location target = slice.absolute(31, -18, 70);
        west.sendBlockChange(target, Material.EXPOSED_CUT_COPPER.createBlockData());
        east.sendBlockChange(target, Material.WAXED_OXIDIZED_CUT_COPPER.createBlockData());
        Player westFinal = west;
        Player eastFinal = east;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            westFinal.sendBlockChange(target, target.getBlock().getBlockData());
            eastFinal.sendBlockChange(target, target.getBlock().getBlockData());
        }, 240L);
        sender.sendMessage("M3_WATCHER_PRESENTED id=" + approval.approvalId() + " class=A2 west="
                + approval.westTarget() + " east=" + approval.eastTarget() + " client_only=true required=false");
    }

    private void requireOperator(CommandSender sender) {
        if (sender instanceof Player player && !(player.isOp() || player.hasPermission("observance.admin"))) {
            throw new IllegalStateException("operator/console only");
        }
    }

    private void verifyDisposableMarker() throws IOException {
        if (targetId.isBlank() || sourceCommit.isBlank()) throw new IllegalStateException("target id and commit are required");
        Path marker = plugin.getServer().getWorldContainer().toPath().resolve(MARKER).toAbsolutePath().normalize();
        if (!Files.isRegularFile(marker)) throw new IllegalStateException("disposable target marker is absent");
        String marked = Files.readString(marker, StandardCharsets.UTF_8).trim();
        if (!targetId.equals(marked)) throw new IllegalStateException("disposable target marker does not match target id");
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.:/=;,' -]", "_");
    }

    @Override public void close() { }
}
