package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.V5RemoteStateCache;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody.SourceSlot;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/** Exact-fixture Bukkit owner for WR05, RP03, RP04, RP05 and Coda recovery. */
public final class V5RitualWorldController implements Listener, AutoCloseable {
    private record Crouch(long startedTick) { }

    private final ObservancePlugin plugin;
    private final V5ProgressStore progress;
    private final V5RemoteStateCache remote;
    private final BukkitFixtureIndex fixtures;
    private final BukkitContainerCustody custody;
    private final VisibleBallotRite ballots;
    private final CollectivePresenceRite presence;
    private final FinaleRite finale;
    private final FinaleBukkitPhaseRunner phaseRunner;
    private final FinaleBukkitArmExpiry armExpiry;
    private final FinaleOperatorCommandAdapter command;
    private final BukkitProtocolBridgeAdapter bridges;
    private final Map<UUID, Crouch> crouches = new HashMap<>();
    private final Map<UUID, Long> understandOriginAt = new HashMap<>();
    private UUID rp04Starter;
    private UUID rp04BridgeInstance;
    private UUID understandFirstCarrier;
    private UUID freeCarrier;
    private boolean rp04Active;
    private BukkitTask tickTask;
    private int visibilityTick;

    public V5RitualWorldController(
            ObservancePlugin plugin,
            V5ProgressStore progress,
            V5RemoteStateCache remote,
            BukkitFixtureIndex fixtures,
            BukkitContainerCustody custody,
            VisibleBallotRite ballots,
            CollectivePresenceRite presence,
            FinaleRite finale,
            FinaleBukkitPhaseRunner phaseRunner,
            FinaleBukkitArmExpiry armExpiry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.remote = Objects.requireNonNull(remote, "remote");
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.custody = Objects.requireNonNull(custody, "custody");
        this.ballots = Objects.requireNonNull(ballots, "ballots");
        this.presence = Objects.requireNonNull(presence, "presence");
        this.finale = Objects.requireNonNull(finale, "finale");
        this.phaseRunner = Objects.requireNonNull(phaseRunner, "phaseRunner");
        this.armExpiry = Objects.requireNonNull(armExpiry, "armExpiry");
        this.command = new FinaleOperatorCommandAdapter(finale, armExpiry, "observance.admin");
        this.bridges = new BukkitProtocolBridgeAdapter(plugin);
    }

    public void start() {
        recoverWr05Bridge();
        armExpiry.scheduleFromDurableState();
        phaseRunner.startOrResume();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public boolean handleFinaleCommand(org.bukkit.command.CommandSender sender, String[] rootArgs) {
        List<String> tail = rootArgs == null || rootArgs.length <= 1
                ? List.of() : java.util.Arrays.asList(rootArgs).subList(1, rootArgs.length);
        return command.handle(sender, tail);
    }

    public FinaleStateStore.Snapshot finaleSnapshot() {
        return finale.snapshot();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block.getState() instanceof Lectern lectern) {
            markConsequenceRead(player, lectern);
        }
        for (Binding binding : fixtures.resolve(block)) {
            if ("WR05".equals(binding.nodeId()) && "branches".equals(binding.componentId())) {
                String branch = binding.metadata().get("v5_wren_vote");
                if (branch != null) {
                    event.setCancelled(true);
                    vote(player, VisibleBallotRite.VoteNode.WR05, branch);
                    return;
                }
            }
            if ("RP04".equals(binding.nodeId())) {
                if ("sector_handle".equals(binding.componentId())) {
                    event.setCancelled(true);
                    confirmSector(player, sectorIndex(binding));
                    return;
                }
                if (binding.componentId().startsWith("bridge_")) {
                    event.setCancelled(true);
                    operateRp04Bridge(player, binding, block);
                    return;
                }
            }
            if ("RP05".equals(binding.nodeId())
                    && Set.of("sever_control", "sever_control_interaction")
                    .contains(binding.componentId())) {
                event.setCancelled(true);
                confirmFinale(player);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        for (Binding binding : fixtures.resolve(event.getRightClicked())) {
            if ("RP03".equals(binding.nodeId()) && "branches".equals(binding.componentId())) {
                String branch = binding.metadata().get("v5_name_treatment");
                if (branch != null) {
                    event.setCancelled(true);
                    startAndVoteRp03(player, branch);
                    return;
                }
            }
            if ("RP04".equals(binding.nodeId())
                    && "sector_handle".equals(binding.componentId())) {
                event.setCancelled(true);
                confirmSector(player, sectorIndex(binding));
                return;
            }
            if ("RP05".equals(binding.nodeId())
                    && Set.of("sever_control", "sever_control_interaction")
                    .contains(binding.componentId())) {
                event.setCancelled(true);
                confirmFinale(player);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Block block = inventoryBlock(event.getInventory());
        if (block == null) return;
        for (Binding binding : fixtures.resolve(block)) {
            if ("WR05".equals(binding.nodeId())
                    && "bridge_housing".equals(binding.componentId())) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> startWr05(player, block));
            } else if ("RP04".equals(binding.nodeId())
                    && "bridge_start_housing".equals(binding.componentId())) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> startRp04(player, block));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Block block = inventoryBlock(event.getView().getTopInventory());
        if (block == null) return;
        for (Binding binding : fixtures.resolve(block)) {
            if ("WR05".equals(binding.nodeId()) && ballots.view(
                    VisibleBallotRite.VoteNode.WR05).isPresent()) {
                event.setCancelled(true);
                return;
            }
            if ("RP04".equals(binding.nodeId()) && rp04Active
                    && !(event.getWhoClicked() instanceof Player player
                    && remote.linked(player.getUniqueId()))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        updateCrouch(event.getPlayer());
        updateSector(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && atConfirmCell(player)) {
            crouches.put(player.getUniqueId(), new Crouch(plugin.getServer().getCurrentTick()));
        } else {
            crouches.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        remote.hydratePlayerAsync(event.getPlayer().getUniqueId());
        ballots.reconnected(VisibleBallotRite.VoteNode.WR05, event.getPlayer().getUniqueId());
        ballots.reconnected(VisibleBallotRite.VoteNode.RP03, event.getPlayer().getUniqueId());
        presence.reconnected(event.getPlayer().getUniqueId());
        if (finale.snapshot().phase() == FinaleStateStore.Phase.CODA) {
            finale.codaReceipt().ifPresent(receipt -> event.getPlayer().sendMessage(
                    joined(receipt.exactGoodbye())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        crouches.remove(id);
        ballots.disconnected(VisibleBallotRite.VoteNode.WR05, id);
        ballots.disconnected(VisibleBallotRite.VoteNode.RP03, id);
        presence.disconnected(id);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isProtocolBridge(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBridgeDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && isProtocolBridge(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBridgeDespawn(ItemDespawnEvent event) {
        if (isProtocolBridge(event.getEntity().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;
        List<ItemStack> bridges = event.getDrops().stream().filter(this::isProtocolBridge)
                .map(ItemStack::clone).toList();
        if (bridges.isEmpty()) return;
        event.getDrops().removeIf(this::isProtocolBridge);
        event.getItemsToKeep().addAll(bridges);
    }

    private void startWr05(Player depositor, Block housing) {
        if (ballots.view(VisibleBallotRite.VoteNode.WR05).isPresent()
                || progress.snapshot().isComplete("v5_case_c08_complete")) return;
        ItemStack stack = item(housing, 13);
        if (!isProtocolBridge(stack)) return;
        try {
            ProtocolBridge bridge = bridges.captureSame(stack);
            Set<UUID> roster = visibleRoster("threshold_vault");
            VisibleBallotRite.StartResult result = ballots.startWr05(
                    depositor.getUniqueId(), roster, bridge);
            reportStart(depositor, "Wren ballot", result.status().name(), roster.size());
        } catch (IOException | RuntimeException failure) {
            fail(depositor, "WR05 start", failure);
        }
    }

    private void vote(Player player, VisibleBallotRite.VoteNode node, String branch) {
        try {
            VisibleBallotRite.VoteResult result = ballots.cast(
                    node, player.getUniqueId(), branch);
            player.sendActionBar(Component.text(
                    result.status().name().toLowerCase(), NamedTextColor.GRAY));
            handleBallotResult(result);
        } catch (IOException | RuntimeException failure) {
            fail(player, node + " vote", failure);
        }
    }

    private void startAndVoteRp03(Player player, String branch) {
        try {
            if (ballots.view(VisibleBallotRite.VoteNode.RP03).isEmpty()) {
                Set<UUID> roster = visibleRoster("the_unwriting");
                VisibleBallotRite.StartResult started = ballots.startRp03(roster);
                if (started.status() != VisibleBallotRite.StartStatus.STARTED
                        && started.status() != VisibleBallotRite.StartStatus.BUSY) {
                    if (started.status() == VisibleBallotRite.StartStatus.CONSEQUENCE_BOOK_UNREAD) {
                        player.sendMessage(Component.text(
                                "The choice waits until every visible voter opens the consequence book ("
                                        + started.unreadPlayers().size() + " unread).",
                                NamedTextColor.YELLOW));
                    } else {
                        reportStart(player, "name vote", started.status().name(), roster.size());
                    }
                    return;
                }
                broadcast(roster, "NAME TREATMENT — visible roster " + roster.size()
                        + "; every listed player must choose PUBLISH or RELEASE UNNAMED.");
            }
            vote(player, VisibleBallotRite.VoteNode.RP03, branch);
        } catch (RuntimeException failure) {
            fail(player, "RP03 start", failure);
        }
    }

    private void handleBallotResult(VisibleBallotRite.VoteResult result) throws IOException {
        if (result.resolution().isEmpty()) return;
        VisibleBallotRite.Resolution resolution = result.resolution().orElseThrow();
        if (resolution.node() == VisibleBallotRite.VoteNode.WR05) {
            ProtocolBridge returned = resolution.returnedBridge().orElseThrow();
            Block housing = onlyBlock("WR05", "bridge_housing");
            ItemStack same = item(housing, 13);
            if (!isProtocolBridge(same)) {
                throw new IllegalStateException("the deposited Protocol Bridge left its protected housing");
            }
            ProtocolBridge current = bridges.captureSame(same);
            if (!current.instanceId().equals(returned.instanceId())) {
                throw new IllegalStateException("Protocol Bridge identity changed during ballot");
            }
            bridges.retagSame(same, returned);
            inventory(housing).setItem(13, same);
            EscrowEntry entry = bridgeEscrow(returned.instanceId());
            UUID recipient = entry.intendedPlayer().orElseThrow();
            deliverBridgeFromSlot(entry, recipient, housing, 13);
        }
        plugin.v5Runtime().projectLocalState();
    }

    private void markConsequenceRead(Player player, Lectern lectern) {
        ItemStack book = lectern.getInventory().getItem(0);
        if (book == null || !book.hasItemMeta()) return;
        String id = book.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "v5_book_id"),
                PersistentDataType.STRING);
        if (!"release_protocol".equals(id)) return;
        try {
            ballots.markConsequenceBookRead(player.getUniqueId());
            player.sendActionBar(Component.text(
                    "Consequence receipt recorded.", NamedTextColor.GRAY));
        } catch (IOException | RuntimeException failure) {
            fail(player, "RP03 consequence read", failure);
        }
    }

    private void startRp04(Player starter, Block housing) {
        if (rp04Active || progress.snapshot().isComplete("v5_rp04_collective")) return;
        ItemStack stack = item(housing, 13);
        if (!isProtocolBridge(stack)) return;
        try {
            ProtocolBridge bridge = bridges.captureSame(stack);
            Set<UUID> roster = visibleRoster("coop_plate");
            int sectors = fixtures.bindings("RP04", "sectors").size();
            if (roster.isEmpty() || sectors < roster.size()) {
                starter.sendMessage(Component.text(
                        "The floor has " + sectors + " sectors for a roster of " + roster.size() + '.',
                        NamedTextColor.RED));
                return;
            }
            CollectivePresenceRite.Result result = presence.start(roster, bridge);
            if (result.status() == CollectivePresenceRite.Status.STARTED) {
                rp04Active = true;
                rp04Starter = starter.getUniqueId();
                rp04BridgeInstance = bridge.instanceId();
                understandFirstCarrier = null;
                freeCarrier = null;
                understandOriginAt.clear();
                broadcast(roster, "ACTIVE ROSTER — " + roster.size()
                        + " linked player(s). Take distinct lit sectors, perform the Bridge step,"
                        + " then confirm your own handle.");
            } else {
                reportStart(starter, "active-roster floor", result.status().name(), roster.size());
            }
        } catch (RuntimeException failure) {
            fail(starter, "RP04 start", failure);
        }
    }

    private void updateSector(Player player) {
        if (!rp04Active) return;
        Optional<Binding> sector = occupiedSector(player);
        try {
            CollectivePresenceRite.Result result = sector.isPresent()
                    ? presence.updatePresence(player.getUniqueId(),
                    sector.get().ordinal(), sectorLit(sector.get()))
                    : presence.leaveSector(player.getUniqueId());
            handlePresenceResult(result);
        } catch (IOException | RuntimeException failure) {
            fail(player, "RP04 presence", failure);
        }
    }

    private void confirmSector(Player player, int sector) {
        try {
            CollectivePresenceRite.Result result = presence.confirmOwnSector(
                    player.getUniqueId(), sector);
            player.sendActionBar(Component.text(
                    result.status().name().toLowerCase(), NamedTextColor.GRAY));
            handlePresenceResult(result);
        } catch (IOException | RuntimeException failure) {
            fail(player, "RP04 confirmation", failure);
        }
    }

    private void operateRp04Bridge(Player player, Binding binding, Block block) {
        if (!rp04Active) return;
        String id = binding.componentId();
        try {
            CollectivePresenceRite.Result result;
            if ("bridge_condemn_black".equals(id)) {
                ItemStack stack = block.getState() instanceof Container ? item(block, 13)
                        : player.getInventory().getItemInMainHand();
                requireActiveBridge(stack);
                result = presence.confirmCondemnBlackHousing(player.getUniqueId());
            } else if ("bridge_understand_origin".equals(id)) {
                ProtocolBridge bridge = requireActiveBridge(
                        player.getInventory().getItemInMainHand());
                understandFirstCarrier = player.getUniqueId();
                understandOriginAt.put(player.getUniqueId(), (long) plugin.getServer().getCurrentTick());
                player.sendActionBar(Component.text(
                        "Bridge origin witnessed; pass it to a second linked hand.",
                        NamedTextColor.GOLD));
                return;
            } else if ("bridge_understand_amber".equals(id)) {
                requireActiveBridge(player.getInventory().getItemInMainHand());
                UUID second = player.getUniqueId();
                long held = understandFirstCarrier != null && understandFirstCarrier.equals(second)
                        ? plugin.getServer().getCurrentTick()
                        - understandOriginAt.getOrDefault(second, Long.MAX_VALUE / 2) : 0L;
                result = presence.confirmUnderstandPass(
                        Objects.requireNonNullElse(understandFirstCarrier, second), second, true, held);
            } else if ("bridge_free_center".equals(id)) {
                requireActiveBridge(player.getInventory().getItemInMainHand());
                freeCarrier = player.getUniqueId();
                player.sendActionBar(Component.text(
                        "The center releases the Bridge. Carry it to the white trough.",
                        NamedTextColor.WHITE));
                return;
            } else if ("bridge_free_white".equals(id)) {
                requireActiveBridge(player.getInventory().getItemInMainHand());
                result = presence.confirmFreeCenterToWhiteTrough(
                        player.getUniqueId(), player.getUniqueId().equals(freeCarrier), true);
            } else {
                return;
            }
            player.sendActionBar(Component.text(result.status().name().toLowerCase(),
                    NamedTextColor.GRAY));
            handlePresenceResult(result);
        } catch (IOException | RuntimeException failure) {
            fail(player, "RP04 Bridge operation", failure);
        }
    }

    private void handlePresenceResult(CollectivePresenceRite.Result result) throws IOException {
        if (result.status() == CollectivePresenceRite.Status.ABORTED) {
            rp04Active = false;
            clearRp04Visuals();
            return;
        }
        if (result.status() != CollectivePresenceRite.Status.COMPLETED) return;
        rp04Active = false;
        clearRp04Visuals();
        plugin.v5Runtime().projectLocalState();
        Player recipient = rp04Starter == null ? null : Bukkit.getPlayer(rp04Starter);
        if (recipient != null && rp04BridgeInstance != null
                && !playerHasBridge(recipient, rp04BridgeInstance)) {
            ItemStack located = removeBridgeFromRp04Housing(rp04BridgeInstance);
            if (located != null) {
                custody.prepareGenerated("rp04-return-" + rp04BridgeInstance,
                        recipient.getUniqueId(), located);
                custody.deliver("rp04-return-" + rp04BridgeInstance, recipient);
            }
        }
    }

    private void confirmFinale(Player player) {
        Crouch hold = crouches.get(player.getUniqueId());
        long ticks = hold == null ? 0L
                : plugin.getServer().getCurrentTick() - hold.startedTick();
        FinaleRite.PlayerCommitProof proof = new FinaleRite.PlayerCommitProof(
                player.getUniqueId(), player.isOnline(), remote.linked(player.getUniqueId()),
                player.getGameMode() == GameMode.SPECTATOR, atConfirmCell(player), true,
                player.isSneaking(), Math.max(0L, ticks));
        FinaleRite.ConfirmResult result = finale.confirm(proof);
        player.sendMessage(Component.text(result.detail(),
                result.status() == FinaleRite.ConfirmStatus.COMMITTED
                        ? NamedTextColor.DARK_RED : NamedTextColor.RED));
        if (result.status() == FinaleRite.ConfirmStatus.COMMITTED) {
            crouches.clear();
            phaseRunner.startOrResume();
        }
    }

    private void tick() {
        try {
            handleBallotResult(ballots.tick(VisibleBallotRite.VoteNode.WR05));
            handleBallotResult(ballots.tick(VisibleBallotRite.VoteNode.RP03));
            handlePresenceResult(presence.tick());
            updateRp04Lamps();
            if (++visibilityTick >= 20) {
                visibilityTick = 0;
                showVisibleStatus();
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("V5 ritual tick paused safely: " + failure.getMessage());
        }
    }

    private void showVisibleStatus() {
        for (VisibleBallotRite.VoteNode node : VisibleBallotRite.VoteNode.values()) {
            ballots.view(node).ifPresent(view -> {
                String text = node + " — visible " + view.visibleRoster().size()
                        + ", received " + view.receivedVotes() + ", phase "
                        + view.phase().name().toLowerCase();
                broadcast(new LinkedHashSet<>(view.visibleRoster()), text);
            });
        }
        // Presence exposes its view only through results; the start message plus lit lamps and
        // confirmation handles are the continuously visible board for RP04.
    }

    private Optional<Binding> occupiedSector(Player player) {
        Block feet = player.getLocation().getBlock();
        Block below = feet.getRelative(org.bukkit.block.BlockFace.DOWN);
        return fixtures.bindings("RP04", "sectors").stream()
                .filter(binding -> binding.worldId().equals(player.getWorld().getUID()))
                .filter(binding -> sameBlock(binding, feet) || sameBlock(binding, below))
                .findFirst();
    }

    private boolean sectorLit(Binding sector) {
        World world = Bukkit.getWorld(sector.worldId());
        if (world == null) return false;
        Block plate = world.getBlockAt(sector.x(), sector.y(), sector.z());
        boolean pressed = plate.getBlockData() instanceof AnaloguePowerable power
                ? power.getPower() > 0 : !plate.getType().isAir();
        List<Binding> lamps = fixtures.bindings("RP04", "sector_lamp");
        if (sector.ordinal() >= lamps.size()) return false;
        Binding lampBinding = lamps.get(sector.ordinal());
        Block lamp = world.getBlockAt(lampBinding.x(), lampBinding.y(), lampBinding.z());
        return pressed && lamp.getType() == Material.REDSTONE_LAMP;
    }

    private void updateRp04Lamps() {
        List<Binding> sectors = fixtures.bindings("RP04", "sectors");
        List<Binding> lamps = fixtures.bindings("RP04", "sector_lamp");
        for (int index = 0; index < Math.min(sectors.size(), lamps.size()); index++) {
            Binding sector = sectors.get(index);
            World world = Bukkit.getWorld(sector.worldId());
            if (world == null) continue;
            Block plate = world.getBlockAt(sector.x(), sector.y(), sector.z());
            boolean occupied = Bukkit.getOnlinePlayers().stream().anyMatch(player ->
                    player.getWorld().equals(world) && occupiedSector(player)
                            .filter(binding -> binding.ordinal() == sector.ordinal()).isPresent());
            if (plate.getBlockData() instanceof AnaloguePowerable power) {
                occupied &= power.getPower() > 0;
            }
            Binding lampBinding = lamps.get(index);
            Block lamp = world.getBlockAt(
                    lampBinding.x(), lampBinding.y(), lampBinding.z());
            if (lamp.getBlockData() instanceof Lightable lightable
                    && lightable.isLit() != occupied) {
                lightable.setLit(occupied);
                lamp.setBlockData(lightable, false);
            }
        }
    }

    private void clearRp04Visuals() {
        for (Binding binding : fixtures.bindings("RP04", "sector_lamp")) {
            World world = Bukkit.getWorld(binding.worldId());
            if (world == null) continue;
            Block block = world.getBlockAt(binding.x(), binding.y(), binding.z());
            if (block.getBlockData() instanceof Lightable lightable && lightable.isLit()) {
                lightable.setLit(false);
                block.setBlockData(lightable, false);
            }
        }
    }

    private void updateCrouch(Player player) {
        if (!player.isSneaking() || !atConfirmCell(player)) {
            crouches.remove(player.getUniqueId());
        } else {
            crouches.putIfAbsent(player.getUniqueId(),
                    new Crouch(plugin.getServer().getCurrentTick()));
        }
    }

    private boolean atConfirmCell(Player player) {
        Block feet = player.getLocation().getBlock();
        for (Binding binding : fixtures.bindings("RP05", "confirm_cell")) {
            if (binding.worldId().equals(player.getWorld().getUID())
                    && binding.x() == feet.getX() && binding.z() == feet.getZ()
                    && Math.abs(binding.y() - feet.getY()) <= 1) return true;
        }
        return false;
    }

    private ProtocolBridge requireActiveBridge(ItemStack stack) {
        ProtocolBridge bridge = bridges.captureSame(stack);
        if (!Objects.equals(rp04BridgeInstance, bridge.instanceId())
                || bridge.outcome().isEmpty()) {
            throw new IllegalArgumentException("that is not the branch-marked active Protocol Bridge");
        }
        return bridge;
    }

    private Set<UUID> visibleRoster(String siteId) {
        Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
        if (site == null || !site.isPlaced()) return Set.of();
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .filter(player -> remote.linked(player.getUniqueId()))
                .filter(player -> site.contains(player.getWorld().getName(),
                        player.getX(), player.getY(), player.getZ()))
                .map(Player::getUniqueId).sorted().forEach(result::add);
        return Set.copyOf(result);
    }

    private void broadcast(Set<UUID> roster, String message) {
        for (UUID id : roster) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) player.sendActionBar(Component.text(message, NamedTextColor.GOLD));
        }
    }

    private void reportStart(Player player, String name, String status, int roster) {
        player.sendMessage(Component.text(name + ": " + status.toLowerCase()
                + " (visible roster " + roster + ')',
                "STARTED".equals(status) ? NamedTextColor.GOLD : NamedTextColor.RED));
    }

    private void fail(Player player, String context, Exception failure) {
        plugin.getLogger().severe(context + " failed closed: " + failure.getMessage());
        player.sendMessage(Component.text(
                "The local record did not commit; the rite remains safe to retry.",
                NamedTextColor.RED));
    }

    private static Block inventoryBlock(Inventory inventory) {
        return inventory == null || inventory.getLocation() == null
                ? null : inventory.getLocation().getBlock();
    }

    private static Inventory inventory(Block block) {
        if (block == null || !(block.getState() instanceof org.bukkit.inventory.InventoryHolder holder)) {
            throw new IllegalStateException("ritual housing inventory is missing");
        }
        return holder.getInventory();
    }

    private static ItemStack item(Block block, int slot) {
        if (block == null) return null;
        Inventory inventory = inventory(block);
        return slot < 0 || slot >= inventory.getSize() ? null : inventory.getItem(slot);
    }

    private boolean isProtocolBridge(ItemStack stack) {
        if (stack == null || stack.getType() != Material.COPPER_INGOT || !stack.hasItemMeta()) {
            return false;
        }
        String id = stack.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, ProtocolBridge.ARTIFACT_KEY),
                PersistentDataType.STRING);
        return ProtocolBridge.ARTIFACT_VALUE.equals(id) && stack.getAmount() == 1;
    }

    private Block onlyBlock(String nodeId, String component) {
        List<Binding> matches = fixtures.bindings(nodeId, component).stream()
                .filter(binding -> binding.kind() == BukkitFixtureIndex.BindingKind.BLOCK).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(nodeId + '/' + component
                    + " requires exactly one block, found " + matches.size());
        }
        Binding binding = matches.getFirst();
        World world = Bukkit.getWorld(binding.worldId());
        if (world == null) throw new IllegalStateException("ritual world is unloaded");
        return world.getBlockAt(binding.x(), binding.y(), binding.z());
    }

    private static boolean sameBlock(Binding binding, Block block) {
        return binding.worldId().equals(block.getWorld().getUID())
                && binding.x() == block.getX() && binding.y() == block.getY()
                && binding.z() == block.getZ();
    }

    private static int sectorIndex(Binding binding) {
        String raw = binding.metadata().get("v5_rp04_sector");
        if (raw == null) return binding.ordinal();
        return Integer.parseInt(raw);
    }

    private boolean playerHasBridge(Player player, UUID instance) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isProtocolBridge(item)) continue;
            if (bridges.captureSame(item).instanceId().equals(instance)) return true;
        }
        return false;
    }

    private ItemStack removeBridgeFromRp04Housing(UUID instance) {
        for (String component : List.of("bridge_start_housing", "bridge_condemn_black",
                "bridge_understand_amber", "bridge_free_center", "bridge_free_white")) {
            for (Binding binding : fixtures.bindings("RP04", component)) {
                if (binding.kind() != BukkitFixtureIndex.BindingKind.BLOCK) continue;
                World world = Bukkit.getWorld(binding.worldId());
                if (world == null) continue;
                Block block = world.getBlockAt(binding.x(), binding.y(), binding.z());
                if (!(block.getState() instanceof org.bukkit.inventory.InventoryHolder holder)) continue;
                for (int slot = 0; slot < holder.getInventory().getSize(); slot++) {
                    ItemStack item = holder.getInventory().getItem(slot);
                    if (isProtocolBridge(item)
                            && bridges.captureSame(item).instanceId().equals(instance)) {
                        holder.getInventory().setItem(slot, null);
                        return item;
                    }
                }
            }
        }
        return null;
    }

    private EscrowEntry bridgeEscrow(UUID instance) {
        EscrowEntry entry = progress.snapshot().escrow().get(
                "wr05-protocol-bridge-" + instance);
        if (entry == null) throw new IllegalStateException("Protocol Bridge escrow is absent");
        return entry;
    }

    private void deliverBridgeFromSlot(
            EscrowEntry entry, UUID recipient, Block source, int slot) throws IOException {
        ItemStack stack = item(source, slot);
        if (!isProtocolBridge(stack)) {
            throw new IOException("Protocol Bridge source is absent");
        }
        if (!custody.hasJournal(entry.escrowId())) {
            custody.prepareFromSlot(entry.escrowId(), recipient, stack,
                    new SourceSlot(source.getWorld().getUID(), source.getX(), source.getY(),
                            source.getZ(), slot));
        }
        Player player = Bukkit.getPlayer(recipient);
        if (player != null && custody.deliver(entry.escrowId(), player)) {
            ballots.acknowledgeBridgeDelivery(
                    UUID.fromString(entry.escrowId().substring(
                            "wr05-protocol-bridge-".length())));
        }
    }

    /** Restart recovery turns a transient WR05 window into an explicit safe abort and same-item return. */
    private void recoverWr05Bridge() {
        ProgressSnapshot snapshot = progress.snapshot();
        for (EscrowEntry entry : snapshot.escrow().values()) {
            if (!entry.escrowId().startsWith("wr05-protocol-bridge-")
                    || !ProtocolBridge.ARTIFACT_VALUE.equals(entry.artifactId())
                    || entry.status() == EscrowStatus.DELIVERED) continue;
            try {
                UUID instance = UUID.fromString(entry.escrowId().substring(
                        "wr05-protocol-bridge-".length()));
                if (entry.status() == EscrowStatus.HELD) {
                    String escrowId = entry.escrowId();
                    EscrowEntry returning = new EscrowEntry(
                            entry.escrowId(), entry.artifactId(), entry.intendedPlayer(),
                            entry.sourceSiteId(), entry.sourceSlot(), entry.itemFingerprintSha256(),
                            entry.amount(), entry.createdAtEpochMillis(),
                            Math.max(System.currentTimeMillis(), entry.updatedAtEpochMillis()),
                            EscrowStatus.RETURN_PENDING, Map.of("state", "restart_safe_abort"));
                    progress.transact(editor -> {
                        editor.transitionEscrow(escrowId, EscrowStatus.HELD, returning);
                        return null;
                    });
                    entry = returning;
                }
                Block housing = onlyBlock("WR05", "bridge_housing");
                ItemStack stack = item(housing, 13);
                if (isProtocolBridge(stack)) {
                    ProtocolBridge bridge = bridges.captureSame(stack);
                    if (!bridge.instanceId().equals(instance)) {
                        throw new IOException("WR05 restart Bridge instance differs");
                    }
                    String outcome = progress.snapshot().branches().get("v5_wren_outcome");
                    if (outcome != null && bridge.outcome().isEmpty()) {
                        bridges.retagSame(stack, bridge.retag(
                                RitualChoices.WrenOutcome.fromWireValue(outcome)));
                        inventory(housing).setItem(13, stack);
                    }
                    deliverBridgeFromSlot(entry, entry.intendedPlayer().orElseThrow(), housing, 13);
                } else {
                    Player recipient = entry.intendedPlayer().map(Bukkit::getPlayer).orElse(null);
                    if (recipient != null && playerHasBridge(recipient, instance)) {
                        ballots.acknowledgeBridgeDelivery(instance);
                    } else {
                        plugin.getLogger().severe("WR05 Bridge recovery remains pending: exact same "
                                + instance + " is not in housing or intended custody");
                    }
                }
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("WR05 restart recovery failed closed: "
                        + failure.getMessage());
            }
        }
    }

    private static Component joined(List<String> lines) {
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) result = result.append(Component.newline());
            result = result.append(Component.text(lines.get(index), NamedTextColor.WHITE));
        }
        return result;
    }

    @Override
    public void close() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        armExpiry.stop();
        phaseRunner.stop();
        crouches.clear();
        clearRp04Visuals();
        try {
            VisibleBallotRite.VoteResult cancelled = ballots.cancel(
                    VisibleBallotRite.VoteNode.WR05);
            handleBallotResult(cancelled);
            ballots.cancel(VisibleBallotRite.VoteNode.RP03);
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("Ritual shutdown recovery remains pending: "
                    + failure.getMessage());
        }
        presence.abort();
    }
}
