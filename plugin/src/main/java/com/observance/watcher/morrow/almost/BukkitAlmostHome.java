package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.ContinuityDecision;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.DecodeChoice;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.EvidenceId;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Progress;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.ProvenanceChoice;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Result;
import com.observance.watcher.morrow.almost.AlmostHomeInstaller.Origin;
import com.observance.watcher.morrow.almost.AlmostHomeManifest.Cell;
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
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Paper adapter for M08 source chronology, cipher, provenance, and bounded continuity consent. */
public final class BukkitAlmostHome implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Key NOTE_INFERRED = Key.key("observance:morrow/m08-note-inferred");
    private static final Key NOTE_HISTORY = Key.key("observance:morrow/m08-note-history");
    private static final Key NOTE_RETURNED = Key.key("observance:morrow/m08-note-returned");
    private static final Key PROVENANCE_AUTHENTICATED = Key.key("observance:morrow/m08-authenticated");
    private static final Key PROVENANCE_INFERRED = Key.key("observance:morrow/m08-inferred");
    private static final Key PROVENANCE_UNKNOWN = Key.key("observance:morrow/m08-unknown");
    private static final Key CONTINUITY_AUTHORIZE = Key.key("observance:morrow/m08-authorize-continuity");
    private static final Key CONTINUITY_DECLINE = Key.key("observance:morrow/m08-decline-continuity");

    private final JavaPlugin plugin; private final World world; private final Origin origin;
    private final String releaseId; private final MorrowLocalState state;
    private final AlmostHomeManifest manifest = new AlmostHomeManifest();
    private final AlmostHomeProgressStore store; private final NamespacedKey ownedKey;
    private final Map<UUID, Long> pendingDecode = new HashMap<>();
    private final Map<UUID, Long> pendingProvenance = new HashMap<>();
    private final Map<UUID, Long> pendingContinuity = new HashMap<>();
    private final List<TextDisplay> displays = new ArrayList<>();
    private TextDisplay statusDisplay; private Progress progress; private boolean started;

    public BukkitAlmostHome(JavaPlugin plugin, World world, Origin origin, String releaseId,
                            MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin"); this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.store = new AlmostHomeProgressStore(Objects.requireNonNull(dataDirectory, "dataDirectory")
                .resolve("morrow-almost-home.progress"), releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_almost_home_owned");
    }
    public void start() throws IOException {
        if (started) return;
        progress = store.load();
        Result recovery = AlmostHomeAuthority.recoverAlmostHome(progress, state.snapshot());
        if (recovery.status() == AlmostHomeAuthority.Status.READY_TO_COMMIT) commit(recovery);
        syncLamps(); spawnDisplays();
        plugin.getServer().getPluginManager().registerEvents(this, plugin); started = true;
        plugin.getLogger().info("MORROW_M08_READY manifest=" + manifest.manifestSha256()
                + " observed=" + progress.observed().size() + " chronology=" + progress.chronology().size()
                + " proven=" + progress.proven() + " displays=" + displays.size());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock(); Player player = event.getPlayer();
        EvidenceId evidence = terminal(block);
        if (evidence != null) {
            event.setCancelled(true);
            try {
                Result result = action == Action.RIGHT_CLICK_BLOCK
                        ? AlmostHomeAuthority.inspect(progress, state.snapshot(), evidence, player.getUniqueId())
                        : AlmostHomeAuthority.order(progress, state.snapshot(), evidence, player.getUniqueId());
                applyProgress(result);
                if (result.receipt() != null)
                    player.sendMessage(Component.text(result.feedback(), NamedTextColor.AQUA));
                else player.sendMessage(Component.text(result.feedback(), color(result)));
            } catch (IOException | RuntimeException failure) { fail(player, "source terminal", failure); }
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) return;
        if (matches(block, manifest.cipherConsole())) {
            event.setCancelled(true); openDecode(player);
        } else if (matches(block, manifest.provenanceConsole())) {
            event.setCancelled(true); openProvenance(player);
        } else if (matches(block, manifest.continuityConsole())) {
            event.setCancelled(true); openContinuity(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer(); Key key = event.getIdentifier();
        DecodeChoice decode = decodeChoice(key);
        if (decode != null) { chooseDecode(player, decode); return; }
        ProvenanceChoice provenance = provenanceChoice(key);
        if (provenance != null) { chooseProvenance(player, provenance); return; }
        ContinuityDecision continuity = continuityDecision(key);
        if (continuity != null) chooseContinuity(player, continuity);
    }
    private void chooseDecode(Player player, DecodeChoice choice) {
        if (!valid(pendingDecode.remove(player.getUniqueId()), player, "cipher")) return;
        try {
            Result result = AlmostHomeAuthority.decode(progress, state.snapshot(), choice, player.getUniqueId());
            applyProgress(result); player.sendMessage(Component.text(result.feedback(), color(result)));
        }
        catch (IOException | RuntimeException failure) { fail(player, "cipher", failure); }
    }
    private void chooseProvenance(Player player, ProvenanceChoice choice) {
        if (!valid(pendingProvenance.remove(player.getUniqueId()), player, "provenance")) return;
        try {
            Result result = AlmostHomeAuthority.classify(progress, state.snapshot(), choice, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "provenance", failure); }
    }
    private void chooseContinuity(Player player, ContinuityDecision decision) {
        if (!valid(pendingContinuity.remove(player.getUniqueId()), player, "continuity")) return;
        try {
            Result result = AlmostHomeAuthority.decideContinuity(progress, state.snapshot(), decision, player.getUniqueId());
            if (result.commitsEvent()) commit(result);
            syncLamps(); refreshStatus(); player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) { fail(player, "continuity", failure); }
    }
    private void applyProgress(Result result) throws IOException {
        if (!result.progress().equals(progress)) progress = store.save(result.progress());
        syncLamps(); refreshStatus();
    }
    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), AlmostHomeAuthority.payload(result));
    }

    private void openDecode(Player player) {
        pendingDecode.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        player.sendMessage(Component.text("SELECTABLE CIPHER // KEY: " + AlmostHomeAuthority.OLD_MOTD
                + " // TEXT: " + AlmostHomeAuthority.CIPHERTEXT, NamedTextColor.AQUA));
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Staff note // Vigenere"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text("KEY: " + AlmostHomeAuthority.OLD_MOTD
                                + "\nCIPHERTEXT: " + AlmostHomeAuthority.CIPHERTEXT), 420))).build())
                .type(DialogType.multiAction(List.of(
                        button("LABEL COMPLETION INFERRED", "Preserve the completion while labeling its provenance.", NOTE_INFERRED),
                        button("CERTIFY PLAN AS HISTORY", "Treat the future-tense post as proof of placement.", NOTE_HISTORY),
                        button("HOUSE PROVES FINCH RETURNED", "Treat current polish as an identity receipt.", NOTE_RETURNED)))
                        .columns(1).build()))); }
        catch (LinkageError | RuntimeException failure) {
            pendingDecode.remove(player.getUniqueId()); player.sendMessage(Component.text("The native cipher dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }
    private void openProvenance(Player player) {
        pendingProvenance.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("The house nobody built"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "The house is beautiful and real now. Decide only what the retained sources authenticate about its completed history."), 420))).build())
                .type(DialogType.multiAction(List.of(
                        button("Authenticated", "Claims Finch completed this exact house before failure.", PROVENANCE_AUTHENTICATED),
                        button("Inferred", "Preserves the house while separating Morrow's synthesis from witnessed history.", PROVENANCE_INFERRED),
                        button("Unknown", "Claims the sources cannot distinguish the current completion.", PROVENANCE_UNKNOWN)))
                        .columns(1).build()))); }
        catch (LinkageError | RuntimeException failure) {
            pendingProvenance.remove(player.getUniqueId()); player.sendMessage(Component.text("The native provenance dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }
    private void openContinuity(Player player) {
        pendingContinuity.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try { player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Account Continuity // later test"))
                        .canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "This separate receipt authorizes only M09's bounded disconnect test. It does not continue a session, record a player, or start the test now."), 430))).build())
                .type(DialogType.confirmation(
                        button("Authorize bounded test", "Commits permission only; starts_continuation is false.", CONTINUITY_AUTHORIZE),
                        button("Not now", "Creates no receipt and starts nothing.", CONTINUITY_DECLINE))))); }
        catch (LinkageError | RuntimeException failure) {
            pendingContinuity.remove(player.getUniqueId()); player.sendMessage(Component.text("The native continuity dialog could not open. Nothing changed.", NamedTextColor.RED));
        }
    }
    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(230)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncLamps() {
        for (Map.Entry<EvidenceId, Cell> entry : manifest.chronologyLamps().entrySet())
            setLamp(entry.getValue(), progress.chronology().contains(entry.getKey()));
        setLamp(manifest.provenanceLamp(), state.snapshot().committedEvents().contains(AlmostHomeAuthority.ALMOST_HOME_EVENT));
        setLamp(manifest.continuityLamp(), state.snapshot().committedEvents().contains(AlmostHomeAuthority.CONTINUITY_EVENT));
    }
    private void setLamp(Cell cell, boolean lit) {
        world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                .setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
    }
    private void spawnDisplays() {
        for (Entity entity : world.getEntities())
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        for (Map.Entry<EvidenceId, Cell> entry : manifest.terminals().entrySet()) {
            Cell cell = entry.getValue(); EvidenceId evidence = entry.getKey();
            displays.add(spawn(new Location(world, origin.x() + cell.x() + .5, origin.y() + 3.15,
                    origin.z() + cell.z() + .5), evidence.name().replace('_', ' ')
                    + "\nRIGHT: PRIVATE READ\nLEFT: PLACE IN CHRONOLOGY", NamedTextColor.AQUA, 250));
        }
        displays.add(spawn(new Location(world, origin.x() + .5, origin.y() + 8.2, origin.z() + .5),
                "ALMOST HOME\nGLASS ROOF // SPRUCE LOFT // COPPER CHIMNEY // EAST MAP WALL",
                NamedTextColor.GOLD, 390));
        displays.add(spawn(new Location(world, origin.x() + .5, origin.y() + 3.1, origin.z() + 7.5),
                "VIGENERE // RIGHT-CLICK LECTERN\nKEY: " + AlmostHomeAuthority.OLD_MOTD
                        + "\n" + AlmostHomeAuthority.CIPHERTEXT, NamedTextColor.WHITE, 380));
        statusDisplay = spawn(new Location(world, origin.x() + 7.5, origin.y() + 3.2, origin.z() + 7.5),
                statusText(), NamedTextColor.GREEN, 340); displays.add(statusDisplay);
        displays.add(spawn(new Location(world, origin.x() - 6.5, origin.y() + 3.2, origin.z() + 7.5),
                "ACCOUNT CONTINUITY\nRIGHT-CLICK ONLY AFTER PROVENANCE\nAUTHORIZATION STARTS NOTHING",
                NamedTextColor.GRAY, 340));
    }
    private TextDisplay spawn(Location location, String text, NamedTextColor color, int width) {
        return world.spawn(location, TextDisplay.class, display -> {
            display.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId);
            display.text(Component.text(text, color)); display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setLineWidth(width); display.setSeeThrough(false); display.setViewRange(24.0F);
        });
    }
    private void refreshStatus() {
        if (statusDisplay != null && statusDisplay.isValid())
            statusDisplay.text(Component.text(statusText(), NamedTextColor.GREEN));
    }
    private String statusText() {
        boolean proven = state.snapshot().committedEvents().contains(AlmostHomeAuthority.ALMOST_HOME_EVENT);
        boolean continuity = state.snapshot().committedEvents().contains(AlmostHomeAuthority.CONTINUITY_EVENT);
        if (continuity) return "INFERRED HOUSE PRESERVED\nACCOUNT CONTINUITY: BOUNDED TEST AUTHORIZED";
        if (proven) return "PROVENANCE: INFERRED\nHOUSE PRESERVED // CONTINUITY DECISION OPEN";
        if (progress.noteDecoded()) return "NOTE: LABEL COMPLETION INFERRED\nRIGHT-CLICK PROVENANCE CONSOLE";
        return "SOURCES " + progress.observed().size() + "/4 // ORDER " + progress.chronology().size()
                + "/4\nHOUSE REMAINS INTACT";
    }

    private EvidenceId terminal(Block block) {
        for (Map.Entry<EvidenceId, Cell> entry : manifest.terminals().entrySet())
            if (matches(block, entry.getValue())) return entry.getKey();
        return null;
    }
    private boolean matches(Block block, Cell cell) {
        return block.getWorld() == world && block.getX() == origin.x() + cell.x()
                && block.getY() == origin.y() + cell.y() && block.getZ() == origin.z() + cell.z();
    }
    private boolean inside(Player player) {
        if (player.getWorld() != world) return false;
        int x = player.getLocation().getBlockX() - origin.x(), y = player.getLocation().getBlockY() - origin.y();
        int z = player.getLocation().getBlockZ() - origin.z(); var bounds = AlmostHomeManifest.BOUNDS;
        return x >= bounds.minimumX() && x <= bounds.maximumX() && y >= bounds.minimumY()
                && y <= bounds.maximumY() && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }
    private boolean valid(Long expires, Player player, String choice) {
        if (expires != null && System.currentTimeMillis() <= expires && inside(player)) return true;
        player.sendMessage(Component.text("That M08 " + choice + " choice expired or left the gallery. Nothing changed.", NamedTextColor.YELLOW));
        return false;
    }
    private static DecodeChoice decodeChoice(Key key) {
        if (NOTE_INFERRED.equals(key)) return DecodeChoice.LABEL_COMPLETION_INFERRED;
        if (NOTE_HISTORY.equals(key)) return DecodeChoice.CERTIFY_PLAN_AS_HISTORY;
        if (NOTE_RETURNED.equals(key)) return DecodeChoice.HOUSE_PROVES_FINCH_RETURNED; return null;
    }
    private static ProvenanceChoice provenanceChoice(Key key) {
        if (PROVENANCE_AUTHENTICATED.equals(key)) return ProvenanceChoice.AUTHENTICATED;
        if (PROVENANCE_INFERRED.equals(key)) return ProvenanceChoice.INFERRED;
        if (PROVENANCE_UNKNOWN.equals(key)) return ProvenanceChoice.UNKNOWN; return null;
    }
    private static ContinuityDecision continuityDecision(Key key) {
        if (CONTINUITY_AUTHORIZE.equals(key)) return ContinuityDecision.AUTHORIZE_BOUNDED_TEST;
        if (CONTINUITY_DECLINE.equals(key)) return ContinuityDecision.NOT_NOW; return null;
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case READY_TO_COMMIT -> NamedTextColor.GREEN;
            case LOCKED, UNOBSERVED, WRONG_ORDER, INCOMPLETE, WRONG_NOTE, WRONG_PROVENANCE -> NamedTextColor.YELLOW;
            default -> NamedTextColor.AQUA;
        };
    }
    private void fail(Player player, String operation, Throwable failure) {
        player.sendMessage(Component.text("The M08 " + operation + " halted safely. The house and prior receipts remain unchanged.", NamedTextColor.RED));
        plugin.getLogger().warning("Morrow M08 " + operation + " failed safely: " + safe(failure.getMessage()));
    }
    public int ownedEntityCount() { return displays.size(); }
    @Override public void close() {
        HandlerList.unregisterAll(this); pendingDecode.clear(); pendingProvenance.clear(); pendingContinuity.clear();
        for (TextDisplay display : displays) if (display.isValid()) display.remove();
        displays.clear(); statusDisplay = null; started = false;
    }
    private static String safe(String message) { return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " "); }
}
