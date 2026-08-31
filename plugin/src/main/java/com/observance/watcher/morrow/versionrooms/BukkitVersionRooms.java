package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Choice;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Progress;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Result;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.RoomVersion;
import com.observance.watcher.morrow.versionrooms.VersionRoomsInstaller.Origin;
import com.observance.watcher.morrow.versionrooms.VersionRoomsManifest.Cell;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Real Paper adapter for M05's three room consoles, private receipts, lamps, and final native choice. */
public final class BukkitVersionRooms implements Listener, AutoCloseable {
    private static final long CHOICE_WINDOW_MILLIS = 60_000L;
    private static final Key CERTIFY_DAMAGED = Key.key("observance:morrow/m05-certify-damaged");
    private static final Key CERTIFY_COMPLETED = Key.key("observance:morrow/m05-certify-completed");
    private static final Key CERTIFY_SCAFFOLD = Key.key("observance:morrow/m05-certify-scaffold");
    private static final Key PRESERVE = Key.key("observance:morrow/m05-preserve-contradiction");

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final MorrowLocalState state;
    private final VersionRoomsManifest manifest;
    private final VersionRoomsProgressStore store;
    private final NamespacedKey ownedKey;
    private final Map<UUID, Long> pendingChoices = new HashMap<>();
    private final List<TextDisplay> labels = new ArrayList<>();
    private Progress progress;
    private boolean started;

    public BukkitVersionRooms(JavaPlugin plugin, World world, Origin origin, String releaseId,
                              MorrowLocalState state, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.state = Objects.requireNonNull(state, "state");
        this.manifest = new VersionRoomsManifest();
        this.store = new VersionRoomsProgressStore(
                Objects.requireNonNull(dataDirectory, "dataDirectory").resolve("morrow-version-rooms.progress"),
                releaseId);
        this.ownedKey = new NamespacedKey(plugin, "morrow_version_rooms_owned");
    }

    public void start() throws IOException {
        if (started) return;
        progress = store.load();
        Result catchUp = VersionRoomsAuthority.authenticate(progress, state.snapshot());
        if (catchUp.status() == VersionRoomsAuthority.Status.READY_TO_COMMIT) commit(catchUp);
        syncLamps();
        spawnLabels();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        plugin.getLogger().info("MORROW_M05_READY manifest=" + manifest.manifestSha256()
                + " route=" + routeText() + " labels=" + labels.size());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConsole(PlayerInteractEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        RoomVersion version = console(event.getClickedBlock());
        if (version == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            Result result = VersionRoomsAuthority.route(progress, state.snapshot(), version, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            syncLamps();
            if (result.receipt() != null) {
                player.sendMessage(Component.text("PRIVATE CUSTODY // " + result.receipt().summary(),
                        NamedTextColor.AQUA));
            }
            if (result.status() == VersionRoomsAuthority.Status.READY_TO_COMMIT) commit(result);
            player.sendMessage(Component.text(result.feedback(), color(result)));
            if (progress.route().equals(VersionRoomsAuthority.SIGNAL_ROUTE)
                    && state.snapshot().committedEvents().contains(
                    VersionRoomsAuthority.FRAGMENTS_AUTHENTICATED)
                    && !progress.preserved()) {
                openChoice(player);
            }
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The version-room signal halted safely. No unverified route or receipt was filed.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M05 console failed safely: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChoice(PlayerCustomClickEvent event) {
        if (!started || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Choice choice = choice(event.getIdentifier());
        if (choice == null) return;
        Player player = connection.getPlayer();
        Long expires = pendingChoices.remove(player.getUniqueId());
        if (expires == null || System.currentTimeMillis() > expires || !inside(player)) {
            player.sendMessage(Component.text(
                    "That M05 choice expired or was not opened inside the three-room bay. Nothing changed.",
                    NamedTextColor.YELLOW));
            return;
        }
        try {
            Result result = VersionRoomsAuthority.choose(progress, state.snapshot(), choice, player.getUniqueId());
            if (!result.progress().equals(progress)) progress = store.save(result.progress());
            if (result.commitsReceipt()) commit(result);
            player.sendMessage(Component.text(result.feedback(), color(result)));
        } catch (IOException | RuntimeException failure) {
            player.sendMessage(Component.text(
                    "The preservation choice halted safely. All three sources remain unchanged.",
                    NamedTextColor.RED));
            plugin.getLogger().warning("Morrow M05 choice failed safely: " + safe(failure.getMessage()));
        }
    }

    private void commit(Result result) throws IOException {
        state.commit(result.eventKey(), result.idempotencyKey(), VersionRoomsAuthority.payload(result));
    }

    private void openChoice(Player player) {
        pendingChoices.put(player.getUniqueId(), System.currentTimeMillis() + CHOICE_WINDOW_MILLIS);
        try {
            player.showDialog(Dialog.create(builder -> builder.empty()
                    .base(DialogBase.builder(Component.text("Three true rooms"))
                            .canCloseWithEscape(true).pause(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .body(List.of(DialogBody.plainMessage(Component.text(
                                    "All three rooms are authentic. Certifying one version would destroy unique evidence in the other two. Choose what the archive retains."), 360)))
                            .build())
                    .type(DialogType.multiAction(List.of(
                            button("Certify damaged", Choice.CERTIFY_DAMAGED.warning(), CERTIFY_DAMAGED),
                            button("Certify completed", Choice.CERTIFY_COMPLETED.warning(), CERTIFY_COMPLETED),
                            button("Certify scaffold", Choice.CERTIFY_SCAFFOLD.warning(), CERTIFY_SCAFFOLD),
                            button("Preserve contradiction",
                                    "Retains all three authentic sources and their incompatible truths.", PRESERVE)))
                            .columns(1).build())));
        } catch (LinkageError | RuntimeException unavailable) {
            pendingChoices.remove(player.getUniqueId());
            player.sendMessage(Component.text(
                    "The native M05 choice could not open. Nothing changed; right-click a room console to retry.",
                    NamedTextColor.RED));
        }
    }

    private static ActionButton button(String label, String tooltip, Key key) {
        return ActionButton.builder(Component.text(label)).tooltip(Component.text(tooltip)).width(220)
                .action(DialogAction.customClick(key, null)).build();
    }

    private void syncLamps() {
        for (RoomVersion version : RoomVersion.values()) {
            Cell cell = manifest.lamps().get(version);
            boolean lit = progress.route().contains(version);
            world.getBlockAt(origin.x() + cell.x(), origin.y() + cell.y(), origin.z() + cell.z())
                    .setBlockData(Bukkit.createBlockData(
                            "minecraft:copper_bulb[lit=" + lit + ",powered=false]"), false);
        }
    }

    private void spawnLabels() {
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(ownedKey, PersistentDataType.STRING)) entity.remove();
        }
        for (RoomVersion version : RoomVersion.values()) {
            Cell console = manifest.consoles().get(version);
            Location location = new Location(world, origin.x() + console.x() + .5,
                    origin.y() + console.y() + 2.2, origin.z() + console.z() + .5);
            TextDisplay label = world.spawn(location, TextDisplay.class, display -> {
                display.getPersistentDataContainer().set(ownedKey, PersistentDataType.STRING, releaseId);
                display.text(Component.text(version.displayName().toUpperCase(java.util.Locale.ROOT)
                        + "\nRIGHT-CLICK CONSOLE // PRIVATE INVENTORY", NamedTextColor.AQUA));
                display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                display.setLineWidth(220);
                display.setSeeThrough(false);
                display.setViewRange(18.0F);
            });
            labels.add(label);
        }
    }

    private RoomVersion console(Block block) {
        if (block.getWorld() != world) return null;
        for (Map.Entry<RoomVersion, Cell> entry : manifest.consoles().entrySet()) {
            Cell cell = entry.getValue();
            if (block.getX() == origin.x() + cell.x() && block.getY() == origin.y() + cell.y()
                    && block.getZ() == origin.z() + cell.z()) return entry.getKey();
        }
        return null;
    }

    private boolean inside(Player player) {
        if (player.getWorld() != world) return false;
        int x = player.getLocation().getBlockX() - origin.x();
        int y = player.getLocation().getBlockY() - origin.y();
        int z = player.getLocation().getBlockZ() - origin.z();
        var bounds = VersionRoomsManifest.BOUNDS;
        return x >= bounds.minimumX() && x <= bounds.maximumX()
                && y >= bounds.minimumY() && y <= bounds.maximumY()
                && z >= bounds.minimumZ() && z <= bounds.maximumZ();
    }

    private static Choice choice(Key key) {
        if (CERTIFY_DAMAGED.equals(key)) return Choice.CERTIFY_DAMAGED;
        if (CERTIFY_COMPLETED.equals(key)) return Choice.CERTIFY_COMPLETED;
        if (CERTIFY_SCAFFOLD.equals(key)) return Choice.CERTIFY_SCAFFOLD;
        if (PRESERVE.equals(key)) return Choice.PRESERVE_CONTRADICTION;
        return null;
    }
    private static NamedTextColor color(Result result) {
        return switch (result.status()) {
            case WRONG_RESET, WRONG_CHOICE, LOCKED -> NamedTextColor.YELLOW;
            case PRESERVED, READY_TO_COMMIT -> NamedTextColor.GREEN;
            default -> NamedTextColor.AQUA;
        };
    }
    private String routeText() {
        return progress.route().stream().map(RoomVersion::key).reduce((a, b) -> a + "/" + b).orElse("empty");
    }
    public int ownedEntityCount() { return labels.size(); }
    public boolean sourcesPreserved() { return progress != null && progress.preserved(); }

    @Override public void close() {
        HandlerList.unregisterAll(this);
        pendingChoices.clear();
        for (TextDisplay label : labels) if (label.isValid()) label.remove();
        labels.clear();
        started = false;
    }
    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }
}
