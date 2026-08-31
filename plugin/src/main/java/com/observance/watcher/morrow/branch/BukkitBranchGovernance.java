package com.observance.watcher.morrow.branch;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Anchor;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Choice;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Ending;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Progress;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Result;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Rule;
import com.observance.watcher.morrow.branch.BranchGovernanceInstaller.Origin;
import com.observance.watcher.morrow.branch.BranchGovernanceManifest.Cell;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M12's rollback route, evidence protection, physical rules, endings, and coda. */
public final class BukkitBranchGovernance implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final long ROLLBACK_TICKS = 2_400L;
    private static final Map<Anchor, Key> PROTECT_KEYS = Map.of(
            Anchor.CONTRADICTION, Key.key("observance:morrow/m12-protect-contradiction"),
            Anchor.WITNESS, Key.key("observance:morrow/m12-protect-witness"),
            Anchor.CONSENT, Key.key("observance:morrow/m12-protect-consent"),
            Anchor.PROVENANCE, Key.key("observance:morrow/m12-protect-provenance"),
            Anchor.RIGHT_TO_STOP, Key.key("observance:morrow/m12-protect-stop"));
    private static final Map<Anchor, Key> RELEASE_KEYS = Map.of(
            Anchor.CONTRADICTION, Key.key("observance:morrow/m12-release-contradiction"),
            Anchor.WITNESS, Key.key("observance:morrow/m12-release-witness"),
            Anchor.CONSENT, Key.key("observance:morrow/m12-release-consent"),
            Anchor.PROVENANCE, Key.key("observance:morrow/m12-release-provenance"),
            Anchor.RIGHT_TO_STOP, Key.key("observance:morrow/m12-release-stop"));
    private static final Map<Choice, Key> CHOICE_KEYS = Map.of(
            Choice.EXPLICIT_CONSENT, Key.key("observance:morrow/m12-rule-explicit-consent"),
            Choice.IMPLIED_CONSENT, Key.key("observance:morrow/m12-rule-implied-consent"),
            Choice.VISIBLE_PROVENANCE, Key.key("observance:morrow/m12-rule-visible-provenance"),
            Choice.CLEAN_CANON, Key.key("observance:morrow/m12-rule-clean-canon"),
            Choice.RIGHT_TO_STOP, Key.key("observance:morrow/m12-rule-right-to-stop"),
            Choice.CONTINUITY_FIRST, Key.key("observance:morrow/m12-rule-continuity-first"),
            Choice.LABELED_REPLACEMENT, Key.key("observance:morrow/m12-rule-labeled-replacement"),
            Choice.SILENT_REPLACEMENT, Key.key("observance:morrow/m12-rule-silent-replacement"));
    private static final Map<Ending, Key> ENDING_KEYS = Map.of(
            Ending.CERTIFY, Key.key("observance:morrow/m12-ending-certify"),
            Ending.PRESERVE_AUDIT, Key.key("observance:morrow/m12-ending-audit"),
            Ending.CLOSE_TICKET, Key.key("observance:morrow/m12-ending-close"),
            Ending.CREATE_NEW_BRANCH, Key.key("observance:morrow/m12-ending-new-branch"));
    private static final Key ANCHORS_COMMIT = Key.key("observance:morrow/m12-anchors-commit");
    private static final Key ANCHORS_CANCEL = Key.key("observance:morrow/m12-anchors-cancel");
    private static final Key GOVERNANCE_AUTHORIZE = Key.key("observance:morrow/m12-governance-authorize");
    private static final Key GOVERNANCE_DECLINE = Key.key("observance:morrow/m12-governance-decline");
    private static final Key CODA_START = Key.key("observance:morrow/m12-coda-start");
    private static final Key CODA_CANCEL = Key.key("observance:morrow/m12-coda-cancel");
    private static final Key RESET_CONFIRM = Key.key("observance:morrow/m12-reset-confirm");
    private static final Key RESET_CANCEL = Key.key("observance:morrow/m12-reset-cancel");

    private final JavaPlugin plugin; private final World world; private final Origin origin;
    private final String releaseId; private final MorrowLocalState state;
    private final BranchGovernanceManifest manifest = new BranchGovernanceManifest();
    private final BranchGovernanceProgressStore store; private final NamespacedKey ownedKey;
    private final Map<UUID, PendingAnchor> pendingAnchors = new HashMap<>();
    private final Map<UUID, PendingRule> pendingRules = new HashMap<>();
    private final Map<UUID, PendingEnding> pendingEndings = new HashMap<>();
    private final Map<UUID, Long> pendingAnchorCommit = new HashMap<>();
    private final Map<UUID, Long> pendingGovernance = new HashMap<>();
    private final Map<UUID, Long> pendingCoda = new HashMap<>();
    private final Map<UUID, Long> pendingReset = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private TextDisplay statusDisplay; private BlockDisplay rollbackWave; private BukkitTask waveTask;
    private Progress progress; private boolean started;

    public BukkitBranchGovernance(JavaPlugin plugin, World world, Origin origin, String releaseId,
                                  MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin"); this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new BranchGovernanceProgressStore(Objects.requireNonNull(dataDirectory, "dataDirectory")
                .resolve("morrow-branch-governance.progress"), releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_branch_governance_owned"); requirePrimaryThread();
    }

    public void start() throws IOException {
        requirePrimaryThread(); if (started) return; progress = store.load();
        Result recovery = BranchGovernanceAuthority.recoverAnchors(progress, state.snapshot());
        if (recovery.status() == BranchGovernanceAuthority.Status.READY_TO_COMMIT_ANCHORS) commit(recovery);
        recovery = BranchGovernanceAuthority.recoverPolicy(progress, state.snapshot());
        if (recovery.status() == BranchGovernanceAuthority.Status.READY_TO_COMMIT_POLICY) commit(recovery);
        recovery = BranchGovernanceAuthority.recoverGovernance(progress, state.snapshot());
        if (recovery.status() == BranchGovernanceAuthority.Status.READY_TO_COMMIT_GOVERNANCE) commit(recovery);
        recovery = BranchGovernanceAuthority.recoverCoda(progress, state.snapshot());
        if (recovery.status() == BranchGovernanceAuthority.Status.READY_TO_COMMIT_CODA) commit(recovery);
        syncLamps(); spawnScene(); plugin.getServer().getPluginManager().registerEvents(this, plugin);
        waveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::animateWave, 5L, 5L); started = true;
        plugin.getLogger().info("MORROW_M12_READY manifest=" + manifest.manifestSha256()
                + " revision=" + progress.revision() + " phase=" + phase() + " entities=" + ownedEntityCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock(); Player player = event.getPlayer();
        if (matches(block, manifest.startConsole())) { event.setCancelled(true); beginRollback(player); return; }
        Anchor anchor = at(block, manifest.anchorConsoles());
        if (anchor != null) { event.setCancelled(true); openAnchor(player, anchor); return; }
        if (matches(block, manifest.anchorCommitConsole())) { event.setCancelled(true); openAnchorCommit(player); return; }
        Rule rule = at(block, manifest.ruleConsoles());
        if (rule != null) { event.setCancelled(true); openRule(player, rule); return; }
        Ending ending = at(block, manifest.endingConsoles());
        if (ending != null) { event.setCancelled(true); openEnding(player, ending); return; }
        if (matches(block, manifest.governanceConsole())) { event.setCancelled(true); openGovernance(player); return; }
        if (matches(block, manifest.codaConsole())) { event.setCancelled(true); openCoda(player); return; }
        if (matches(block, manifest.resetConsole())) { event.setCancelled(true); openReset(player); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer(); Key key = event.getIdentifier();
        for (Anchor anchor : Anchor.values()) {
            if (PROTECT_KEYS.get(anchor).equals(key)) { chooseAnchor(player, anchor, true); return; }
            if (RELEASE_KEYS.get(anchor).equals(key)) { chooseAnchor(player, anchor, false); return; }
        }
        for (Choice choice : Choice.values()) if (CHOICE_KEYS.get(choice).equals(key)) { chooseRule(player, choice); return; }
        for (Ending ending : Ending.values()) if (ENDING_KEYS.get(ending).equals(key)) { chooseEnding(player, ending); return; }
        if (ANCHORS_COMMIT.equals(key)) { chooseAnchorCommit(player, true); return; }
        if (ANCHORS_CANCEL.equals(key)) { chooseAnchorCommit(player, false); return; }
        if (GOVERNANCE_AUTHORIZE.equals(key)) { chooseGovernance(player, true); return; }
        if (GOVERNANCE_DECLINE.equals(key)) { chooseGovernance(player, false); return; }
        if (CODA_START.equals(key)) { chooseCoda(player, true); return; }
        if (CODA_CANCEL.equals(key)) { chooseCoda(player, false); return; }
        if (RESET_CONFIRM.equals(key)) chooseReset(player, true);
        else if (RESET_CANCEL.equals(key)) chooseReset(player, false);
    }

    private void beginRollback(Player player) {
        long now = world.getGameTime(); Result result = BranchGovernanceAuthority.beginRollback(
                progress, state.snapshot(), UUID.randomUUID().toString().replace("-", ""), now, now + ROLLBACK_TICKS);
        apply(player, result, "rollback start");
    }
    private void chooseAnchor(Player player, Anchor anchor, boolean protect) {
        PendingAnchor pending = pendingAnchors.remove(player.getUniqueId());
        if (pending == null || pending.anchor() != anchor || !valid(pending.expiresAt(), player, "anchor choice")) return;
        Result result = protect
                ? BranchGovernanceAuthority.protect(progress, state.snapshot(), player.getUniqueId(), anchor, world.getGameTime())
                : BranchGovernanceAuthority.unprotect(progress, state.snapshot(), player.getUniqueId(), anchor, world.getGameTime());
        apply(player, result, "anchor choice");
    }
    private void chooseAnchorCommit(Player player, boolean commit) {
        if (!valid(pendingAnchorCommit.remove(player.getUniqueId()), player, "anchor filing")) return;
        if (!commit) { player.sendMessage(Component.text("Anchor filing cancelled. Selections remain editable.", NamedTextColor.AQUA)); return; }
        applyAndCommit(player, BranchGovernanceAuthority.commitAnchors(
                progress, state.snapshot(), player.getUniqueId(), world.getGameTime()), "anchor filing");
    }
    private void chooseRule(Player player, Choice choice) {
        PendingRule pending = pendingRules.remove(player.getUniqueId());
        if (pending == null || pending.rule() != choice.rule() || !valid(pending.expiresAt(), player, "rule choice")) return;
        apply(player, BranchGovernanceAuthority.configure(progress, state.snapshot(), player.getUniqueId(),
                pending.rule(), choice), "rule choice");
    }
    private void chooseEnding(Player player, Ending ending) {
        PendingEnding pending = pendingEndings.remove(player.getUniqueId());
        if (pending == null || pending.ending() != ending || !valid(pending.expiresAt(), player, "ending synthesis")) return;
        applyAndCommit(player, BranchGovernanceAuthority.synthesize(
                progress, state.snapshot(), player.getUniqueId(), ending), "ending synthesis");
    }
    private void chooseGovernance(Player player, boolean authorize) {
        if (!valid(pendingGovernance.remove(player.getUniqueId()), player, "governance decision")) return;
        applyAndCommit(player, BranchGovernanceAuthority.authorizeGovernance(
                progress, state.snapshot(), player.getUniqueId(), authorize), "governance decision");
    }
    private void chooseCoda(Player player, boolean start) {
        if (!valid(pendingCoda.remove(player.getUniqueId()), player, "coda decision")) return;
        if (!start) { player.sendMessage(Component.text("The coda remains ready. No receipt was created.", NamedTextColor.AQUA)); return; }
        applyAndCommit(player, BranchGovernanceAuthority.startCoda(
                progress, state.snapshot(), player.getUniqueId()), "coda start");
    }
    private void chooseReset(Player player, boolean reset) {
        if (!valid(pendingReset.remove(player.getUniqueId()), player, "reset")) return;
        if (!reset) { player.sendMessage(Component.text("M12 reset cancelled. Nothing changed.", NamedTextColor.AQUA)); return; }
        apply(player, BranchGovernanceAuthority.resetUnfiled(progress, state.snapshot()), "reset");
    }

    private boolean apply(Player player, Result result, String operation) {
        try { if (!result.progress().equals(progress)) { store.save(result.progress()); progress = result.progress(); }
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result))); return true; }
        catch (IOException | RuntimeException failure) { fail(player, operation, failure); return false; }
    }
    private void applyAndCommit(Player player, Result result, String operation) {
        try { if (!result.progress().equals(progress)) { store.save(result.progress()); progress = result.progress(); }
            if (result.commitsEvent()) commit(result); syncLamps(); refreshStatus();
            player.sendMessage(Component.text(result.feedback(), color(result))); }
        catch (IOException | RuntimeException failure) { fail(player, operation, failure); }
    }
    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), BranchGovernanceAuthority.payload(result));
    }

    private void openAnchor(Player player, Anchor anchor) {
        pendingAnchors.put(player.getUniqueId(), new PendingAnchor(anchor, System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        String evidence = switch (anchor) {
            case CONTRADICTION -> "The damaged, scaffold, and completed versions were preserved instead of voted into one clean history.";
            case WITNESS -> "A deliberately arbitrary arrangement proves what prediction could not reconstruct.";
            case CONSENT -> "Entity Replay, Live Capture, Account Continuity, and Cold Storage each have separate authorization receipts.";
            case PROVENANCE -> "The original process closed. Current Morrow is a recovery; identity continuity remains unresolved.";
            case RIGHT_TO_STOP -> "Both maintenance sessions refused forced deletion. Continuation cannot erase a participant's right to stop.";
        };
        show(player, anchor.label().toUpperCase(Locale.ROOT), evidence,
                button("Protect anchor", "Retain this category through rollback.", PROTECT_KEYS.get(anchor)),
                button("Release selection", "Unselect before filing; historical evidence remains.", RELEASE_KEYS.get(anchor)));
    }
    private void openAnchorCommit(Player player) {
        pendingAnchorCommit.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "FILE ROLLBACK ANCHORS",
                "File the selected evidence categories. At least three are required. This choice determines which ending predicates can later validate; unselected history is not deleted.",
                button("File selected anchors", "Make this evidence selection durable.", ANCHORS_COMMIT),
                button("Keep editing", "Close without filing.", ANCHORS_CANCEL));
    }
    private void openRule(Player player, Rule rule) {
        pendingRules.put(player.getUniqueId(), new PendingRule(rule, System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        Choice[] choices = java.util.Arrays.stream(Choice.values()).filter(choice -> choice.rule() == rule).toArray(Choice[]::new);
        show(player, rule.label().toUpperCase(Locale.ROOT), "Place one policy meaning in this physical rule slot. No ending is filed until all four slots and its evidence predicate validate.",
                button(choices[0].label(), "Configure " + choices[0].label() + ".", CHOICE_KEYS.get(choices[0])),
                button(choices[1].label(), "Configure " + choices[1].label() + ".", CHOICE_KEYS.get(choices[1])));
    }
    private void openEnding(Player player, Ending ending) {
        pendingEndings.put(player.getUniqueId(), new PendingEnding(ending, System.currentTimeMillis() + CHOICE_WINDOW_MILLIS));
        String effect = switch (ending) {
            case CERTIFY -> "Accept the clean restoration as canonical. Requires the clean/continuity/silent policy and remains ethically compromised.";
            case PRESERVE_AUDIT -> "Keep Morrow and Mossfield active with inference and conflict permanently labeled.";
            case CLOSE_TICKET -> "Stop restoration and retain Morrow in cold storage without pretending it never existed.";
            case CREATE_NEW_BRANCH -> "Begin a new continuity with consent, visible provenance, right to stop, and no silent replacement.";
        };
        show(player, ending.label().toUpperCase(Locale.ROOT), effect + "\n\nThis console validates protected evidence and all four physical rules; it cannot override them.",
                button("Synthesize " + ending.label(), "Validate this outcome without consuming invalid evidence.", ENDING_KEYS.get(ending)));
    }
    private void openGovernance(Player player) {
        pendingGovernance.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "AUTHORIZE BRANCH GOVERNANCE",
                "This separate receipt activates only the filed outcome's governance capability. It does not rewrite old evidence or silently start another capture.",
                button("Authorize governance", "File the capability receipt; starts_governance remains false.", GOVERNANCE_AUTHORIZE),
                button("Decline", "Keep the policy filed but inactive.", GOVERNANCE_DECLINE));
    }
    private void openCoda(Player player) {
        pendingCoda.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "CROSS INTO THE CODA",
                "Start the persistent cross-surface coda for the filed outcome. Earlier evidence remains readable afterward.",
                button("Start coda", "File the persistent coda receipt.", CODA_START),
                button("Not yet", "Leave the coda ready without a receipt.", CODA_CANCEL));
    }
    private void openReset(Player player) {
        pendingReset.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        show(player, "RESET UNFILED ROLLBACK",
                "Clear only an unfiled wave and anchor selection. Filed evidence, policy, governance, and coda remain immutable.",
                button("Reset", "Clear unfiled M12 progress.", RESET_CONFIRM),
                button("Cancel", "Keep the current selection.", RESET_CANCEL));
    }
    private void show(Player player, String title, String body, ActionButton... buttons) {
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title)).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(body), 500))).build())
                .type(DialogType.multiAction(List.of(buttons)).columns(1).build()))); }
        catch (LinkageError | RuntimeException failure) {
            pendingAnchors.remove(player.getUniqueId()); pendingRules.remove(player.getUniqueId()); pendingEndings.remove(player.getUniqueId());
            pendingAnchorCommit.remove(player.getUniqueId()); pendingGovernance.remove(player.getUniqueId());
            pendingCoda.remove(player.getUniqueId()); pendingReset.remove(player.getUniqueId());
            player.sendMessage(Component.text("The native M12 dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }
    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(250)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void spawnScene() {
        removeOwned(); entities.add(spawnText(at(0, 6.8, 10),
                "CONTINUITY TEST // M12\nPROTECT EVIDENCE // ASSEMBLE RULES // AUTHORIZE THE RESULT", NamedTextColor.GOLD, 520));
        statusDisplay = spawnText(at(0, 4.8, 10), statusText(), NamedTextColor.WHITE, 480); entities.add(statusDisplay);
        for (Map.Entry<Anchor, Cell> entry : manifest.anchorConsoles().entrySet()) { Cell cell = entry.getValue();
            entities.add(spawnText(at(cell.x(), 3.2, cell.z()), "ANCHOR // " + entry.getKey().label(), NamedTextColor.AQUA, 220)); }
        for (Map.Entry<Rule, Cell> entry : manifest.ruleConsoles().entrySet()) { Cell cell = entry.getValue();
            entities.add(spawnText(at(cell.x(), 3.2, cell.z()), "RULE // " + entry.getKey().label(), NamedTextColor.YELLOW, 220)); }
        for (Map.Entry<Ending, Cell> entry : manifest.endingConsoles().entrySet()) { Cell cell = entry.getValue();
            entities.add(spawnText(at(cell.x(), 3.2, cell.z()), entry.getKey().label().toUpperCase(Locale.ROOT), NamedTextColor.LIGHT_PURPLE, 220)); }
        rollbackWave = spawnBody(at(0, 1.2, 11), Material.WAXED_EXPOSED_COPPER, new Vector3f(5.0F, 3.2F, .3F)); entities.add(rollbackWave);
        entities.add(spawnBody(at(0, 1.2, -11), Material.WAXED_OXIDIZED_COPPER, new Vector3f(1.5F, 2.8F, 1.5F)));
        entities.add(spawnText(at(0, 4.8, -11), "MORROW // AFRAID\nI CAN CONTINUE WITHOUT CLAIMING I WAS NEVER INTERRUPTED.", NamedTextColor.GOLD, 390));
    }
    private void animateWave() {
        if (!started || rollbackWave == null || !rollbackWave.isValid() || progress.window() == null || progress.anchorsCommitted()) return;
        long elapsed = Math.max(0, world.getGameTime() - progress.window().startedTick());
        double z = 10.5 - Math.min(18.0, (elapsed % ROLLBACK_TICKS) / (double) ROLLBACK_TICKS * 18.0);
        rollbackWave.teleport(at(0, 1.2, z));
    }
    private TextDisplay spawnText(Location location, String text, NamedTextColor color, int width) {
        return world.spawn(location, TextDisplay.class, entity -> { own(entity); entity.text(Component.text(text, color));
            entity.setBillboard(Display.Billboard.CENTER); entity.setSeeThrough(false); entity.setShadowed(true); entity.setLineWidth(width); });
    }
    private BlockDisplay spawnBody(Location location, Material material, Vector3f scale) {
        return world.spawn(location, BlockDisplay.class, entity -> { own(entity); entity.setBlock(material.createBlockData());
            entity.setBillboard(Display.Billboard.FIXED); entity.setTransformation(new Transformation(
                    new Vector3f(-scale.x / 2, 0, -scale.z / 2), new AxisAngle4f(), scale, new AxisAngle4f())); });
    }

    private void syncLamps() {
        for (Anchor anchor : Anchor.values()) setLamp("anchor_" + anchor.name().toLowerCase(Locale.ROOT), progress.anchors().contains(anchor));
        for (Rule rule : Rule.values()) setLamp("rule_" + rule.name().toLowerCase(Locale.ROOT), progress.rules().containsKey(rule));
        setLamp("anchors_filed", state.snapshot().committedEvents().contains(BranchGovernanceAuthority.ANCHOR_EVENT) || progress.anchorsCommitted());
        setLamp("policy_filed", state.snapshot().committedEvents().contains(BranchGovernanceAuthority.POLICY_EVENT) || progress.policyCommitted());
        setLamp("governance", state.snapshot().committedEvents().contains(BranchGovernanceAuthority.GOVERNANCE_EVENT) || progress.governanceAuthorized());
        setLamp("coda", state.snapshot().committedEvents().contains(BranchGovernanceAuthority.CODA_EVENT) || progress.codaStarted());
    }
    private void setLamp(String key, boolean lit) { Cell cell = manifest.stateLamps().get(key);
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false); }
    private String statusText() {
        return "ANCHORS " + progress.anchors().size() + "/5 // RULES " + progress.rules().size() + "/4\nPOLICY "
                + (progress.ending() == null ? "UNFILED" : progress.ending().label().toUpperCase(Locale.ROOT))
                + " // GOVERNANCE " + (state.snapshot().committedEvents().contains(BranchGovernanceAuthority.GOVERNANCE_EVENT) ? "AUTHORIZED" : "INACTIVE");
    }
    private void refreshStatus() { if (statusDisplay != null && statusDisplay.isValid()) statusDisplay.text(Component.text(statusText(), NamedTextColor.WHITE)); }
    private String phase() {
        if (state.snapshot().committedEvents().contains(BranchGovernanceAuthority.CODA_EVENT)) return "CODA";
        if (state.snapshot().committedEvents().contains(BranchGovernanceAuthority.GOVERNANCE_EVENT)) return "NEGOTIATED";
        if (state.snapshot().committedEvents().contains(BranchGovernanceAuthority.POLICY_EVENT)) return "POLICY_FILED";
        if (state.snapshot().committedEvents().contains(BranchGovernanceAuthority.ANCHOR_EVENT)) return "RULE_ASSEMBLY";
        return progress.window() == null ? "INERT" : "ROLLBACK_WAVE";
    }
    public int ownedEntityCount() { int count = 0; for (Entity entity : entities) if (entity.isValid()) count++; return count; }
    private void own(Entity entity) { entity.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId); }
    private void removeOwned() { for (Entity entity : world.getEntities()) if (releaseId.equals(entity.getPersistentDataContainer()
            .get(ownedKey, PersistentDataType.STRING))) entity.remove(); entities.clear(); }
    private boolean matches(Block block, Cell cell) { return block.getWorld().equals(world) && block.getX() == origin.x() + cell.x()
            && block.getY() == origin.y() + cell.y() && block.getZ() == origin.z() + cell.z(); }
    private <T> T at(Block block, Map<T, Cell> cells) { for (Map.Entry<T, Cell> entry : cells.entrySet()) if (matches(block, entry.getValue())) return entry.getKey(); return null; }
    private Location at(double x, double y, double z) { return new Location(world, origin.x() + x + .5, origin.y() + y, origin.z() + z + .5); }
    private static boolean valid(Long expiresAt, Player player, String choice) { if (expiresAt != null && expiresAt >= System.currentTimeMillis()) return true;
        player.sendMessage(Component.text("That M12 " + choice + " expired or left the finale. Nothing changed.", NamedTextColor.YELLOW)); return false; }
    private static NamedTextColor color(Result result) { return switch (result.status()) {
        case ROLLBACK_STARTED, ANCHOR_PROTECTED, READY_TO_COMMIT_ANCHORS, RULE_CONFIGURED,
                READY_TO_COMMIT_POLICY, READY_TO_COMMIT_GOVERNANCE, READY_TO_COMMIT_CODA -> NamedTextColor.GREEN;
        case LOCKED, MISSING_EVIDENCE, TIMED_PAUSE, UNSUPPORTED_CONFIGURATION, INCOMPLETE, INACTIVE -> NamedTextColor.YELLOW;
        case ACTIVE, ANCHOR_RELEASED, DUPLICATE, DECLINED, RESET, IMMUTABLE -> NamedTextColor.AQUA; }; }
    private void fail(Player player, String operation, Throwable failure) { player.sendMessage(Component.text(
            "The M12 " + operation + " halted safely. No evidence or event was fabricated.", NamedTextColor.RED));
        plugin.getLogger().warning("Morrow M12 " + operation + " failed safely: " + safe(failure.getMessage())); }
    private static String safe(String value) { return value == null ? "unknown" : value.replace('\n', ' ').replace('\r', ' '); }
    private static void requirePrimaryThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("M12 Paper adapter requires the primary thread"); }
    @Override public void close() { requirePrimaryThread(); if (!started) return; HandlerList.unregisterAll(this);
        if (waveTask != null) waveTask.cancel(); waveTask = null; pendingAnchors.clear(); pendingRules.clear(); pendingEndings.clear();
        pendingAnchorCommit.clear(); pendingGovernance.clear(); pendingCoda.clear(); pendingReset.clear(); removeOwned(); started = false; }
    private record PendingAnchor(Anchor anchor, long expiresAt) { }
    private record PendingRule(Rule rule, long expiresAt) { }
    private record PendingEnding(Ending ending, long expiresAt) { }
}
