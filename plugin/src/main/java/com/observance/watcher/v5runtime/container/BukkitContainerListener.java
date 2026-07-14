package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.container.ContainerAuthorityContract.TriggerKind;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Exact-fixture Paper listener for family C. It deliberately refuses shift/drag/hopper automation so
 * every accepted move has one actor, one slot, and one serialized inventory transaction.
 */
public final class BukkitContainerListener implements Listener {
    private static final java.util.Set<Material> DESTRUCTIVE_NON_BLOCK_USE = java.util.Set.of(
            Material.BREAD, Material.POTION, Material.GLASS_BOTTLE,
            Material.WHEAT_SEEDS, Material.STRING);
    private final Plugin plugin;
    private final ContainerSolveService service;
    private final ContainerAuthorityContract contract;
    private final BukkitFixtureIndex fixtures;
    private final BukkitContainerWorld world;

    public BukkitContainerListener(
            Plugin plugin,
            ContainerSolveService service,
            BukkitFixtureIndex fixtures,
            BukkitContainerWorld world) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.service = Objects.requireNonNull(service, "service");
        this.contract = service.contract();
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.world = Objects.requireNonNull(world, "world");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null) {
            for (NodeComponent bound : owned(block)) {
                if (contract.isTrigger(bound.binding().nodeId(), TriggerKind.HANDLE,
                        bound.component(), -1)) {
                    event.setCancelled(true);
                    service.evaluate(bound.binding().nodeId(), event.getPlayer().getUniqueId(),
                            TriggerKind.HANDLE, bound.component(), -1);
                    return;
                }
            }
        }
        ItemStack held = event.getItem();
        if (held != null && world.isManagedItem(held)
                && (held.getType().isBlock() || DESTRUCTIVE_NON_BLOCK_USE.contains(held.getType()))) {
            boolean safeInventoryOpen = block != null
                    && (block.getState() instanceof InventoryHolder
                    || block.getType() == Material.ENDER_CHEST);
            if (!safeInventoryOpen) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (dropAction(event.getAction())
                && (world.isManagedItem(event.getCurrentItem())
                || world.isManagedItem(event.getView().getCursor()))) {
            event.setCancelled(true);
            return;
        }
        Inventory top = event.getView().getTopInventory();
        List<NodeComponent> bindings = inventoryBindings(top);
        if (bindings.isEmpty()) {
            protectFromUnmanagedInventory(event, player, top);
            return;
        }
        int topSize = top.getSize();
        boolean topSlot = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;
        boolean attemptsTopMutation = topSlot || event.isShiftClick()
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR;
        if (!attemptsTopMutation) {
            return;
        }
        boolean completed = bindings.stream().anyMatch(
                bound -> service.isComplete(bound.binding().nodeId()));
        if (completed || bindings.stream().anyMatch(
                bound -> !service.canModify(bound.binding().nodeId()))) {
            event.setCancelled(true);
            ItemStack readable = event.getCurrentItem();
            if (completed && topSlot && readable != null
                    && readable.getType() == Material.WRITTEN_BOOK
                    && world.isManagedItem(readable)) {
                ItemStack copy = readable.clone();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) player.openBook(copy);
                });
            }
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE
                || event.isShiftClick()
                || event.getClick() == ClickType.DOUBLE_CLICK
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getAction() == InventoryAction.CLONE_STACK
                || event.getHotbarButton() >= 0) {
            event.setCancelled(true);
            return;
        }
        if (!topSlot) {
            return;
        }
        int slot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean cursorEmpty = empty(cursor);
        boolean currentEmpty = empty(current);

        if (cursorEmpty && !currentEmpty && pickupAction(event.getAction())) {
            world.ensureArtifactInstance(current);
            top.setItem(slot, current);
            for (NodeComponent bound : bindings) {
                String nodeId = bound.binding().nodeId();
                if (contract.isTrigger(nodeId, TriggerKind.SOURCE_CLAIM,
                        bound.component(), slot)) {
                    event.setCancelled(true);
                    service.evaluate(nodeId, player.getUniqueId(), TriggerKind.SOURCE_CLAIM,
                            bound.component(), slot);
                    return;
                }
                if (contract.isPortableClaim(nodeId, bound.component(), slot)) {
                    event.setCancelled(true);
                    service.claimPortable(nodeId, player.getUniqueId(), bound.component(), slot);
                    return;
                }
            }
        }

        if (cursorEmpty) {
            if (currentEmpty) {
                return;
            }
            ContainerItem image;
            try {
                image = world.fromBukkitWithInstance(current);
            } catch (IllegalArgumentException exception) {
                event.setCancelled(true);
                return;
            }
            if (bindings.stream().anyMatch(bound -> !contract.allowsExtraction(
                    bound.binding().nodeId(), bound.component(), slot, image))) {
                event.setCancelled(true);
            }
            return;
        }

        world.ensureArtifactInstance(cursor);
        event.getView().setCursor(cursor);
        ContainerItem incoming;
        try {
            incoming = world.fromBukkit(cursor);
        } catch (IllegalArgumentException exception) {
            event.setCancelled(true);
            return;
        }
        if (bindings.stream().anyMatch(bound -> !contract.allowsInsertion(
                bound.binding().nodeId(), bound.component(), slot, incoming))) {
            event.setCancelled(true);
            return;
        }
        if (!currentEmpty) {
            ContainerItem outgoing;
            try {
                outgoing = world.fromBukkitWithInstance(current);
            } catch (IllegalArgumentException exception) {
                event.setCancelled(true);
                return;
            }
            if (bindings.stream().anyMatch(bound -> !contract.allowsExtraction(
                    bound.binding().nodeId(), bound.component(), slot, outgoing))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (inventoryBindings(top).isEmpty()) {
            if (!safePlayerCustody(top) && world.isManagedItem(event.getOldCursor())
                    && event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (world.isManagedItem(event.getItem())
                || !inventoryBindings(event.getSource()).isEmpty()
                || !inventoryBindings(event.getDestination()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (world.isManagedItem(event.getItem().getItemStack())
                || !inventoryBindings(event.getInventory()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (world.isManagedItem(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            return;
        }
        List<Binding> bindings = fixtures.resolve(event.getRightClicked()).stream()
                .filter(binding -> ContainerPredicateCoverage.owns(binding.nodeId())).toList();
        if (bindings.isEmpty()) {
            return;
        }
        for (Binding binding : bindings) {
            if (contract.isTrigger(binding.nodeId(), TriggerKind.HANDLE,
                    binding.componentId(), -1)) {
                event.setCancelled(true);
                service.evaluate(binding.nodeId(), event.getPlayer().getUniqueId(),
                        TriggerKind.HANDLE, binding.componentId(), -1);
                return;
            }
        }
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }
        if (bindings.stream().anyMatch(binding -> service.isComplete(binding.nodeId())
                || !service.canModify(binding.nodeId()))
                || !event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!fixtures.resolve(event.getEntity()).stream()
                .filter(binding -> ContainerPredicateCoverage.owns(binding.nodeId())).toList().isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (world.isManagedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (world.isManagedItem(event.getEntity().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && world.isManagedItem(item.getItemStack())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof ItemFrame frame && fixtures.resolve(frame).stream()
                .anyMatch(binding -> ContainerPredicateCoverage.owns(binding.nodeId()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (protectedCoordinate(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (!world.isManagedItem(item)) {
                continue;
            }
            try {
                world.escrowDeathDrop(event.getEntity().getUniqueId(), item);
                iterator.remove();
            } catch (IOException exception) {
                plugin.getLogger().severe("Protected V5 death custody failed; retaining drop: "
                        + exception.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (world.isManagedItem(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (protectedCoordinate(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (protectedCoordinate(event.getBlockPlaced())
                || world.isManagedItem(event.getItemInHand())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (protectedCoordinate(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (protectedCoordinate(event.getBlock()) || world.isManagedItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (protectedCoordinate(event.getToBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (protectedCoordinate(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.blockList().stream().anyMatch(this::protectedCoordinate)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.blockList().stream().anyMatch(this::protectedCoordinate)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> protectedCoordinate(block)
                || protectedCoordinate(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> protectedCoordinate(block)
                || protectedCoordinate(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        recover(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> recover(event.getPlayer()));
    }

    private void recover(Player player) {
        try {
            service.recoverPlayer(player.getUniqueId());
        } catch (Exception exception) {
            plugin.getLogger().severe("V5 container custody remains pending for "
                    + player.getName() + ": " + exception.getMessage());
        }
    }

    private List<NodeComponent> inventoryBindings(Inventory inventory) {
        Location location = inventory.getLocation();
        return location == null || location.getWorld() == null
                ? List.of() : owned(location.getBlock());
    }

    private List<NodeComponent> owned(Block block) {
        List<NodeComponent> result = new ArrayList<>();
        for (Binding binding : fixtures.resolve(block)) {
            if (ContainerPredicateCoverage.owns(binding.nodeId())) {
                result.add(new NodeComponent(binding, world.bindingComponent(binding, block)));
            }
        }
        return List.copyOf(result);
    }

    private boolean protectedCoordinate(Block block) {
        return fixtures.atCoordinate(block).stream()
                .anyMatch(binding -> ContainerPredicateCoverage.owns(binding.nodeId()));
    }

    private void protectFromUnmanagedInventory(
            InventoryClickEvent event, Player player, Inventory top) {
        if (safePlayerCustody(top)) {
            return;
        }
        int topSize = top.getSize();
        boolean topSlot = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;
        ItemStack cursor = event.getView().getCursor();
        if ((topSlot && world.isManagedItem(cursor))
                || (event.isShiftClick() && event.getRawSlot() >= topSize
                && world.isManagedItem(event.getCurrentItem()))
                || (event.getHotbarButton() >= 0 && topSlot
                && world.isManagedItem(player.getInventory().getItem(event.getHotbarButton())))
                || (event.getClick() == ClickType.SWAP_OFFHAND && topSlot
                && world.isManagedItem(player.getInventory().getItemInOffHand()))) {
            event.setCancelled(true);
        }
    }

    private static boolean safePlayerCustody(Inventory top) {
        return top.getType() == InventoryType.ENDER_CHEST
                || top.getType() == InventoryType.PLAYER;
    }

    private static boolean pickupAction(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_HALF
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_SOME;
    }

    private static boolean dropAction(InventoryAction action) {
        return action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_SLOT;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private record NodeComponent(Binding binding, String component) {
        private NodeComponent {
            Objects.requireNonNull(binding, "binding");
            Objects.requireNonNull(component, "component");
        }
    }
}
