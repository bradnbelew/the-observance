package com.observance.watcher.m3runtime;

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
import java.util.Set;

/** Review-only Bukkit adapter for the authored M3 v2 authority. */
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
        Path journal = plugin.getDataFolder().toPath().resolve("m3-private-slice.journal");
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
        if (plugin.getCommand("observancem3") == null) throw new IllegalStateException("observancem3 command missing");
        plugin.getCommand("observancem3").setExecutor(this);
        plugin.getLogger().info("M3_TARGET_CONFIRMED target=" + targetId + " commit=" + sourceCommit
                + " authority=observance-p4-private-slice-v2 paper=" + Bukkit.getVersion());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "status" -> status(sender);
                case "build" -> build(sender);
                case "audit" -> audit(sender);
                case "observe" -> observe(sender, args);
                case "finding" -> finding(sender, args);
                case "replay" -> replay(sender);
                case "security" -> security(sender);
                case "watcher-approve" -> watcherApprove(sender, args);
                case "watcher-show" -> watcherShow(sender, args);
                default -> sender.sendMessage("Usage: /obsm3 <status|build|audit|observe|finding|replay|security|watcher-approve|watcher-show>");
            }
        } catch (Exception failure) {
            sender.sendMessage("M3_FAIL action=" + action + " reason=" + safe(failure.getMessage()));
        }
        return true;
    }

    private void status(CommandSender sender) {
        sender.sendMessage("M3_STATUS target=" + targetId + " commit=" + sourceCommit
                + " authority=v2 gate=" + (state.gateOpen() ? "open" : "closed")
                + " evidence_surfaces=14 submissions=6 signs=8 paper=" + Bukkit.getVersion());
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

    private void finding(CommandSender sender, String[] args) throws IOException {
        requireOperator(sender);
        if (args.length != 4) throw new IllegalArgumentException("finding requires <id> <contributor> <source1,source2>");
        List<String> sources = List.of(args[3].split(",", -1));
        state.commitFinding(args[1], sources, args[2]);
        slice.setGate(state.gateOpen());
        sender.sendMessage("M3_FINDING_COMMITTED id=" + args[1] + " contributor=" + args[2]
                + " gate=" + (state.gateOpen() ? "open" : "closed"));
    }

    private void replay(CommandSender sender) throws IOException {
        requireOperator(sender);
        state.commitFinding(PrivateSliceState.SYNTHESIS, PrivateSliceState.BASE_FINDINGS, "replay-audit");
        slice.setGate(state.gateOpen());
        sender.sendMessage("M3_REPLAY_PASS gate=open receipts=" + state.catchUpAfter(0).size());
    }

    private void security(CommandSender sender) {
        PrivateSliceWorld.Audit audit = slice.audit();
        sender.sendMessage("M3_SECURITY_PASS target=" + targetId + " bind=127.0.0.1 gamemode="
                + GameMode.ADVENTURE + " force_gamemode=true non_op=true inventory_escrow=false"
                + " denies=block_break,block_place,bucket,entity,container,teleport,gate"
                + " gate_collision=" + audit.gateCollisionCells());
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
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.:/=-]", "_");
    }

    @Override public void close() { }
}
