package com.observance.watcher.m3runtime;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Review-only Bukkit adapter for the exact M3 authority. */
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
        plugin.getServer().getPluginManager().registerEvents(new PrivateSliceProtectionListener(slice), plugin);
        if (plugin.getCommand("observancem3") == null) throw new IllegalStateException("observancem3 command missing");
        plugin.getCommand("observancem3").setExecutor(this);
        plugin.getLogger().info("M3_TARGET_CONFIRMED target=" + targetId + " commit=" + sourceCommit
                + " paper=" + Bukkit.getVersion());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "status" -> status(sender);
                case "build" -> build(sender);
                case "audit" -> audit(sender);
                case "finding" -> finding(sender, args);
                case "replay" -> replay(sender);
                default -> sender.sendMessage("Usage: /obsm3 <status|build|audit|finding|replay>");
            }
        } catch (Exception failure) {
            sender.sendMessage("M3_FAIL action=" + action + " reason=" + safe(failure.getMessage()));
        }
        return true;
    }

    private void status(CommandSender sender) {
        sender.sendMessage("M3_STATUS target=" + targetId + " commit=" + sourceCommit
                + " gate=" + (state.gateOpen() ? "open" : "closed") + " paper=" + Bukkit.getVersion());
    }

    private void build(CommandSender sender) throws IOException {
        verifyDisposableMarker();
        int writes = slice.apply();
        slice.setGate(state.gateOpen());
        plugin.getServer().getWorlds().forEach(World::save);
        PrivateSliceWorld.Audit audit = slice.audit();
        sender.sendMessage("M3_BUILD_COMPLETE target=" + targetId + " writes=" + writes
                + " cells=" + audit.cellsChecked() + " gate=" + (state.gateOpen() ? "open" : "closed")
                + " world_state_sha256=" + audit.worldHash() + " findings=" + audit.findings().size());
    }

    private void audit(CommandSender sender) {
        PrivateSliceWorld.Audit audit = slice.audit();
        sender.sendMessage("M3_AUDIT " + (audit.pass() ? "PASS" : "FAIL") + " target=" + targetId
                + " cells=" + audit.cellsChecked() + " gate=" + (state.gateOpen() ? "open" : "closed")
                + " world_state_sha256=" + audit.worldHash() + " findings=" + audit.findings().size());
        for (String finding : audit.findings()) sender.sendMessage("M3_AUDIT_FINDING " + finding);
    }

    private void finding(CommandSender sender, String[] args) throws IOException {
        if (args.length != 4) throw new IllegalArgumentException("finding requires <id> <contributor> <source1,source2>");
        List<String> sources = List.of(args[3].split(",", -1));
        state.commitFinding(args[1], sources, args[2]);
        slice.setGate(state.gateOpen());
        sender.sendMessage("M3_FINDING_COMMITTED id=" + args[1] + " contributor=" + args[2]
                + " gate=" + (state.gateOpen() ? "open" : "closed"));
    }

    private void replay(CommandSender sender) throws IOException {
        state.commitFinding(PrivateSliceState.SYNTHESIS, PrivateSliceState.BASE_FINDINGS, "replay-audit");
        slice.setGate(state.gateOpen());
        sender.sendMessage("M3_REPLAY_PASS gate=open receipts=" + state.catchUpAfter(0).size());
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
