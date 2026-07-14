package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.mechanics.BukkitDurableItemEscrow.SourceSlot;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.Trigger;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent;

/**
 * Real Paper event adapter for the 41 non-ritual S/I/F/L/R nodes. All lookups are exact fixture
 * bindings; there is no nearest-site interaction path.
 */
public final class V5PhysicalMechanicsListener implements Listener, AutoCloseable {
    private static final Duration SIGN_LEASE = Duration.ofSeconds(30);
    private final Plugin plugin;
    private final PhysicalPredicateAuthority authority;
    private final V5MechanicsEngine engine;
    private final BukkitFixtureIndex fixtures;
    private final BukkitMechanicState live;
    private final BukkitRouteController routes;
    private final PredicateInputRules inputRules;
    private final GeneratedReceiptCatalog receiptCatalog;
    private final BukkitReceiptService receipts;
    private final BukkitDurableItemEscrow itemEscrow;
    private final LeaseBook signLeases = new LeaseBook();
    private BukkitTask tickTask;

    public V5PhysicalMechanicsListener(
            Plugin plugin,
            PhysicalPredicateAuthority authority,
            V5MechanicsEngine engine,
            BukkitFixtureIndex fixtures,
            BukkitMechanicState live,
            BukkitReceiptService receipts,
            BukkitDurableItemEscrow itemEscrow) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        this.authority = java.util.Objects.requireNonNull(authority, "authority");
        this.engine = java.util.Objects.requireNonNull(engine, "engine");
        this.fixtures = java.util.Objects.requireNonNull(fixtures, "fixtures");
        this.live = java.util.Objects.requireNonNull(live, "live");
        this.receipts = java.util.Objects.requireNonNull(receipts, "receipts");
        this.itemEscrow = java.util.Objects.requireNonNull(itemEscrow, "itemEscrow");
        this.inputRules = new PredicateInputRules(authority);
        this.receiptCatalog = new GeneratedReceiptCatalog(authority);
        this.routes = new BukkitRouteController(authority, fixtures, live, engine);
    }

    /** Starts the route sampler only after the lifecycle owner has registered every adapter. */
    public void start() {
        if (tickTask == null) {
            tickTask = plugin.getServer().getScheduler()
                    .runTaskTimer(plugin, routes::tick, 1L, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        List<Binding> answerBindings = owned(fixtures.resolve(event.getBlock())).stream()
                .filter(binding -> "answer_sign".equals(binding.componentId()))
                .toList();
        if (answerBindings.size() != 1) {
            return;
        }
        Binding binding = answerBindings.getFirst();
        String scope = "v5:sign:" + binding.siteId() + ':' + binding.nodeId();
        Optional<LeaseBook.Token> token = signLeases.tryAcquire(
                scope, event.getPlayer().getUniqueId().toString(), SIGN_LEASE);
        if (token.isEmpty()) {
            clearSign(event);
            return;
        }
        try (LeaseBook.Token ignored = token.orElseThrow()) {
            String answer = event.lines().stream()
                    .map(PlainTextComponentSerializer.plainText()::serialize)
                    .reduce("", (left, right) -> left + ' ' + right).trim();
            live.answer(event.getPlayer().getUniqueId(), binding.nodeId(), answer);
            engine.evaluate(binding.nodeId(), event.getPlayer().getUniqueId(), Trigger.SIGN_SUBMIT);
            clearSign(event);
            live.clearNode(event.getPlayer().getUniqueId(), binding.nodeId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        for (Binding binding : owned(fixtures.resolve(event.getClickedBlock()))) {
            if (event.getClickedBlock().getState()
                    instanceof org.bukkit.block.ChiseledBookshelf shelf
                    && inputRules.isManagedInventory(binding.nodeId(), binding.componentId())) {
                if (engine.isComplete(binding.nodeId())) {
                    event.setCancelled(true);
                    continue;
                }
                Location interaction = event.getInteractionPoint();
                if (interaction == null) {
                    event.setCancelled(true);
                    continue;
                }
                int slot = shelf.getSlot(interaction.toVector().subtract(
                        event.getClickedBlock().getLocation().toVector()));
                ItemStack held = event.getItem();
                if (held != null && !held.getType().isAir()) {
                    MechanicItem item;
                    try {
                        item = BukkitWorldStateEvaluator.item(held);
                    } catch (IllegalArgumentException exception) {
                        event.setCancelled(true);
                        continue;
                    }
                    if (!inputRules.allows(binding.nodeId(), binding.componentId(), slot,
                            item, player.getUniqueId())) {
                        event.setCancelled(true);
                        continue;
                    }
                }
            }
            if (routes.operate(player, binding)) {
                return;
            }
            if ("AR07".equals(binding.nodeId()) && "false_m".equals(binding.componentId())) {
                try {
                    engine.recordSessionEvent("AR07", player.getUniqueId(), "false_m");
                } catch (IOException exception) {
                    plugin.getLogger().severe("Unable to persist AR07 session event: "
                            + exception.getMessage());
                }
            }
            inspect(player, binding);
            if (event.getClickedBlock().getState() instanceof org.bukkit.block.Lectern lectern) {
                issuePageReceipt(player, binding, lectern.getPage());
            }
            if ("true".equals(binding.metadata().get("trigger"))) {
                String handle = binding.metadata().getOrDefault(
                        "handle_component", binding.componentId());
                live.handle(player.getUniqueId(), binding.nodeId(), handle);
                plugin.getServer().getScheduler().runTask(plugin, () -> engine.evaluate(
                        binding.nodeId(), player.getUniqueId(), Trigger.HANDLE));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        List<Binding> exactBindings = owned(fixtures.resolve(event.getRightClicked()));
        if (exactBindings.isEmpty()) {
            return;
        }
        if (!(event.getRightClicked() instanceof ItemFrame)) {
            event.setCancelled(true);
            for (Binding binding : exactBindings) {
                inspect(event.getPlayer(), binding);
                if ("true".equals(binding.metadata().get("trigger"))) {
                    String handle = binding.metadata().getOrDefault(
                            "handle_component", binding.componentId());
                    live.handle(event.getPlayer().getUniqueId(), binding.nodeId(), handle);
                    plugin.getServer().getScheduler().runTask(plugin, () -> engine.evaluate(
                            binding.nodeId(), event.getPlayer().getUniqueId(), Trigger.HANDLE));
                }
            }
            return;
        }
        for (Binding binding : exactBindings) {
            routes.recordReverseFrameView(event.getPlayer(), binding);
            if (engine.isComplete(binding.nodeId())) {
                event.setCancelled(true);
                return;
            }
            ItemFrame frame = (ItemFrame) event.getRightClicked();
            if (frame.getItem().getType().isAir()) {
                ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
                MechanicItem item;
                try {
                    item = BukkitWorldStateEvaluator.item(held);
                } catch (IllegalArgumentException exception) {
                    event.setCancelled(true);
                    return;
                }
                if (!inputRules.frameReorderable(binding.nodeId(), binding.componentId())
                        || held.getType().isAir()
                        || !inputRules.allowsFrameItem(
                                binding.nodeId(), binding.componentId(), item)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        List<Binding> bindings = inventoryBindings(top);
        if (bindings.isEmpty()) {
            return;
        }
        bindings.forEach(binding -> inspect(player, binding));
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
            ItemStack selected = top.getItem(event.getRawSlot());
            for (Binding binding : bindings) {
                if (inspectEvidenceItem(player, binding, selected)
                        && selected != null && selected.getType() == org.bukkit.Material.WRITTEN_BOOK) {
                    // Immutable source books remain in custody, but the player still receives the
                    // normal readable book UI. Opening is deferred until the inventory click ends.
                    ItemStack readable = selected.clone();
                    event.setCancelled(true);
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> player.openBook(readable));
                    return;
                }
            }
        }
        if (bindings.stream().anyMatch(binding -> engine.isComplete(binding.nodeId()))) {
            event.setCancelled(true);
            return;
        }
        boolean immutable = bindings.stream().anyMatch(binding ->
                inputRules.isImmutableSource(binding.nodeId(), binding.componentId()));
        if (immutable && (event.isShiftClick() && event.getRawSlot() >= top.getSize()
                || event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize())) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick() && event.getRawSlot() >= top.getSize()) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) {
            return;
        }
        ItemStack incoming = incoming(event, player);
        if (incoming == null || incoming.getType().isAir()) {
            return;
        }
        MechanicItem item;
        try {
            item = BukkitWorldStateEvaluator.item(incoming);
        } catch (IllegalArgumentException exception) {
            event.setCancelled(true);
            return;
        }
        int slot = event.getRawSlot();
        for (Binding binding : bindings) {
            if (!inputRules.isManagedInventory(binding.nodeId(), binding.componentId())) {
                continue;
            }
            if (!inputRules.allows(
                    binding.nodeId(), binding.componentId(), slot, item, player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        List<Binding> bindings = inventoryBindings(top);
        if (bindings.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, ItemStack> dragged : event.getNewItems().entrySet()) {
            if (dragged.getKey() >= top.getSize()) {
                continue;
            }
            MechanicItem item;
            try {
                item = BukkitWorldStateEvaluator.item(dragged.getValue());
            } catch (IllegalArgumentException exception) {
                event.setCancelled(true);
                return;
            }
            for (Binding binding : bindings) {
                if (engine.isComplete(binding.nodeId())
                        || inputRules.isManagedInventory(binding.nodeId(), binding.componentId())
                        && !inputRules.allows(binding.nodeId(), binding.componentId(),
                                dragged.getKey(), item, player.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getView().getTopInventory();
        Location location = inventory.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        Block block = location.getBlock();
        List<Binding> bindings = inventoryBindings(inventory);
        Set<Integer> handled = new HashSet<>();
        for (Binding binding : bindings) {
            if (!inputRules.isManagedInventory(binding.nodeId(), binding.componentId())) {
                continue;
            }
            for (int slot : inputRules.managedSlots(binding.nodeId(), binding.componentId())) {
                if (!handled.add(slot)) {
                    continue;
                }
                ItemStack stack = inventory.getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                MechanicItem item;
                try {
                    item = BukkitWorldStateEvaluator.item(stack);
                } catch (IllegalArgumentException exception) {
                    item = null;
                }
                if (item != null && inputRules.allows(binding.nodeId(), binding.componentId(),
                        slot, item, player.getUniqueId())) {
                    continue;
                }
                UUID intended = intendedPlayer(item).orElse(player.getUniqueId());
                try {
                    itemEscrow.depositFromSlot(intended, stack,
                            "wrong V5 input " + binding.nodeId() + ':' + binding.componentId(),
                            new SourceSlot(block.getWorld().getUID(), block.getX(), block.getY(),
                                    block.getZ(), slot));
                    inventory.setItem(slot, null);
                    Player recipient = plugin.getServer().getPlayer(intended);
                    if (recipient != null) {
                        itemEscrow.deliverPending(recipient);
                    }
                } catch (IOException exception) {
                    plugin.getLogger().severe("Wrong-item escrow failed; item retained in fixture: "
                            + exception.getMessage());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!inventoryBindings(event.getSource()).isEmpty()
                || !inventoryBindings(event.getDestination()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    // Run before the region-wide HIGHEST protection guard. This exact binding/allowlist handler
    // moves only an authored reorderable clue item, cancels the damage, and leaves the frame intact.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFrameDamage(EntityDamageByEntityEvent event) {
        List<Binding> bindings = owned(fixtures.resolve(event.getEntity()));
        if (bindings.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getEntity() instanceof ItemFrame frame)
                || !(event.getDamager() instanceof Player player)
                || frame.getItem().getType().isAir()
                || !player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        for (Binding binding : bindings) {
            if (engine.isComplete(binding.nodeId())
                    || !inputRules.frameReorderable(binding.nodeId(), binding.componentId())) {
                return;
            }
        }
        ItemStack protectedItem = frame.getItem().clone();
        frame.setItem(null, false);
        player.getInventory().setItemInMainHand(protectedItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!owned(fixtures.resolve(event.getEntity())).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeLecternBook(PlayerTakeLecternBookEvent event) {
        if (owned(fixtures.resolve(event.getLectern().getBlock())).stream().anyMatch(binding ->
                inputRules.isImmutableSource(binding.nodeId(), binding.componentId()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLecternPage(PlayerLecternPageChangeEvent event) {
        for (Binding binding : owned(fixtures.resolve(event.getLectern().getBlock()))) {
            inspect(event.getPlayer(), binding);
            issuePageReceipt(event.getPlayer(), binding, event.getNewPage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!owned(fixtures.atCoordinate(event.getBlock())).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!owned(fixtures.atCoordinate(event.getBlockPlaced())).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.blockList().stream()
                .anyMatch(block -> !owned(fixtures.atCoordinate(block)).isEmpty())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.blockList().stream()
                .anyMatch(block -> !owned(fixtures.atCoordinate(block)).isEmpty())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // The one-tick controller is authoritative; this listener guarantees immediate posture/cell polling.
        routes.tick();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        routes.tick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        reset(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        reset(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        try {
            receipts.recover(event.getPlayer());
        } catch (IOException exception) {
            plugin.getLogger().severe("V5 item recovery remains pending for "
                    + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void inspect(Player player, Binding binding) {
        String source = binding.metadata().getOrDefault("source_id", binding.componentId());
        try {
            engine.recordInspection(binding.nodeId(), player.getUniqueId(), source);
            receiptCatalog.receiptFor(binding.nodeId(), source).ifPresent(receipt -> {
                try {
                    receipts.issue(player, binding.nodeId(), receipt);
                } catch (IOException exception) {
                    plugin.getLogger().severe("Receipt escrow failed for " + binding.nodeId()
                            + ':' + receipt + ": " + exception.getMessage());
                }
            });
        } catch (IllegalArgumentException ignored) {
            // Exact bound controls that are not authored sources do not create inspection bits.
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to persist source inspection " + binding.nodeId()
                    + ':' + source + ": " + exception.getMessage());
        }
    }

    /**
     * Records the identity of the actual evidence item the player selected. Container bindings name
     * the source barrel, not its individual packets; using only the barrel component would make the
     * WR01/WR02 exact-source predicates impossible to satisfy.
     */
    private boolean inspectEvidenceItem(Player player, Binding binding, ItemStack selected) {
        if (selected == null || selected.getType().isAir() || !selected.hasItemMeta()) {
            return false;
        }
        String evidenceId = selected.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "v5_evidence_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (evidenceId == null || evidenceId.isBlank()) {
            return false;
        }
        try {
            engine.recordInspection(binding.nodeId(), player.getUniqueId(), evidenceId);
            receiptCatalog.receiptFor(binding.nodeId(), evidenceId).ifPresent(receipt -> {
                try {
                    receipts.issue(player, binding.nodeId(), receipt);
                } catch (IOException exception) {
                    plugin.getLogger().severe("Receipt escrow failed for " + binding.nodeId()
                            + ':' + receipt + ": " + exception.getMessage());
                }
            });
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to persist evidence inspection "
                    + binding.nodeId() + ':' + evidenceId + ": " + exception.getMessage());
            return false;
        }
    }

    private void issuePageReceipt(Player player, Binding binding, int page) {
        receiptCatalog.pageReceipt(binding.nodeId(), binding.componentId(), page)
                .ifPresent(receipt -> {
                    try {
                        receipts.issue(player, binding.nodeId(), receipt);
                    } catch (IOException exception) {
                        plugin.getLogger().severe("Page receipt escrow failed for "
                                + binding.nodeId() + ':' + receipt + ": " + exception.getMessage());
                    }
                });
    }

    private void reset(Player player) {
        routes.resetPlayer(player.getUniqueId());
        try {
            engine.clearTransientPlayerState(player.getUniqueId());
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to clear transient V5 state for "
                    + player.getName() + ": " + exception.getMessage());
        }
    }

    private List<Binding> inventoryBindings(Inventory inventory) {
        Location location = inventory.getLocation();
        return location == null || location.getWorld() == null
                ? List.of() : owned(fixtures.resolve(location.getBlock()));
    }

    private List<Binding> owned(List<Binding> source) {
        return source.stream().filter(binding ->
                AssignedPhysicalNodes.implementedNodeIds().contains(binding.nodeId())).toList();
    }

    private static ItemStack incoming(InventoryClickEvent event, Player player) {
        if (event.getHotbarButton() >= 0) {
            return player.getInventory().getItem(event.getHotbarButton());
        }
        ItemStack cursor = event.getCursor();
        return cursor == null || cursor.getType().isAir() ? null : cursor;
    }

    private static Optional<UUID> intendedPlayer(MechanicItem item) {
        if (item == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(item.pdc().get("v5_receipt_actor")).map(UUID::fromString);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void clearSign(SignChangeEvent event) {
        for (int line = 0; line < event.lines().size(); line++) {
            event.line(line, Component.empty());
        }
    }
}
