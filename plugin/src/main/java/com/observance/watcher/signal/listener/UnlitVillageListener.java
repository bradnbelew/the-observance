package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Prototype-ready runtime for the Unlit village pillar. This is intentionally separate from
 * {@link UnlitDeepListener}: that listener owns the existing custom/restraint latch, while this one owns
 * the mirrored-village expedition sandbox.
 */
public final class UnlitVillageListener implements Listener {

    private static final String ENTRY_SITE = "unlit_entry";
    private static final String SPAWN_SITE = "unlit_spawn_mirror";
    private static final String EXIT_SITE = "unlit_exit";
    private static final Set<String> HOUSE_IDS = Set.of(
            "unlit_house_lamp",
            "unlit_house_cairn",
            "unlit_house_coop",
            "unlit_house_well",
            "unlit_house_watch",
            "unlit_house_warm",
            "unlit_house_threshold",
            "unlit_house_base"
    );

    private final ObservancePlugin plugin;
    private final Supplier<SitesConfig> sites;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final NamespacedKey lightKey;
    private final NamespacedKey returnKey;
    private final NamespacedKey figureKey;

    private final Map<UUID, StoredInventory> storedInventories = new HashMap<>();
    private final Map<UUID, Integer> darknessExposure = new HashMap<>();
    /** Per-player apparition pressure: 0 glimpse, 1 stalk, 2 takes light, 3 hunt. */
    private final Map<UUID, Integer> figureStage = new HashMap<>();
    private final Map<UUID, Long> lastFigureMs = new HashMap<>();
    private final Map<LightKey, Long> liveLights = new HashMap<>();
    private final Set<String> reportedDiscoveries = new HashSet<>();
    private final List<Entity> figureBodies = new ArrayList<>();
    private BukkitTask tickTask;

    public UnlitVillageListener(ObservancePlugin plugin,
                                Supplier<SitesConfig> sites,
                                SupabaseClient supabase,
                                RateLimiter rateLimiter,
                                Scheduler scheduler,
                                Safety safety,
                                String namespace) {
        this.plugin = plugin;
        this.sites = sites;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        String ns = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
        this.lightKey = new NamespacedKey(ns, "unlit_light");
        this.returnKey = new NamespacedKey(ns, "unlit_return");
        this.figureKey = new NamespacedKey(ns, "unlit_figure");
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = scheduler.runTimerSafe("unlit.village.tick", 20L, 20L, this::tick);
    }

    public void stop() {
        Scheduler.cancel(tickTask);
        tickTask = null;
        for (Entity e : List.copyOf(figureBodies)) {
            try {
                if (e != null && !e.isDead()) e.remove();
            } catch (Throwable ignored) {
                // Best-effort shutdown cleanup.
            }
        }
        figureBodies.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isActive(player)) restoreAndReturn(player, false, false);
        }
        storedInventories.clear();
        darknessExposure.clear();
        figureStage.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isReturnToken(item) && isActive(player)) {
            event.setCancelled(true);
            restoreAndReturn(player, true, true);
            return;
        }

        if (atSite(player, ENTRY_SITE) && !isUnlitWorld(player.getWorld())) {
            event.setCancelled(true);
            enter(player);
            return;
        }

        if (isUnlitBuildMode(player)) return;

        if (isUnlitWorld(player.getWorld()) && isBannedItem(item)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("That kind of light does not hold here.", NamedTextColor.DARK_GRAY));
            return;
        }

        Block clicked = event.getClickedBlock();
        if (isUnlitWorld(player.getWorld()) && clicked != null
                && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL)
                && isRouteCheeseBlock(clicked.getType())) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("The copy refuses that way.", NamedTextColor.DARK_GRAY));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player == null || !isActive(player) || !isUnlitWorld(player.getWorld())) return;
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || sameBlock(from, to)) return;

        if (atSite(to, EXIT_SITE)) {
            restoreAndReturn(player, true, true);
            return;
        }

        for (String houseId : HOUSE_IDS) {
            if (atSite(to, houseId)) {
                recordDiscovery(player, houseId);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player == null || !isActive(player)) return;
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        Player player = event.getPlayer();
        if (isUnlitBuildMode(player)) return;
        if (isUnlitWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        Player player = event.getPlayer();
        if (!isUnlitWorld(player.getWorld())) return;
        if (isUnlitBuildMode(player)) return;

        ItemStack item = event.getItemInHand();
        if (!isActive(player) || !isBorrowedLight(item)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Only the borrowed lantern catches here.", NamedTextColor.DARK_GRAY));
            return;
        }

        Block block = event.getBlockPlaced();
        liveLights.put(LightKey.of(block.getLocation()), System.currentTimeMillis() + lightLifetimeSeconds() * 1000L);
        scheduler.runLaterSafe("unlit.light.burnout", lightLifetimeSeconds() * 20L, () -> burnOut(block.getLocation()));
        player.sendActionBar(Component.text("The borrowed lantern catches. It will not come back.", NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        if (isUnlitBuildMode(event.getPlayer())) return;
        if (isUnlitWorld(event.getPlayer().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        if (isUnlitBuildMode(event.getPlayer())) return;
        if (isActive(event.getPlayer()) || isUnlitWorld(event.getPlayer().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (!enabled() || event.getPlayer() == null) return;
        if (isUnlitBuildMode(event.getPlayer())) return;
        if (isActive(event.getPlayer()) || isUnlitWorld(event.getPlayer().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!enabled() || !(event.getPlayer() instanceof Player player)) return;
        if (!isActive(player) && !isUnlitWorld(player.getWorld())) return;
        if (isUnlitBuildMode(player)) return;
        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.LECTERN || type == InventoryType.PLAYER) return;
        event.setCancelled(true);
        player.sendActionBar(Component.text("The copy keeps its contents.", NamedTextColor.DARK_GRAY));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null || !isActive(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        scheduler.runLaterSafe("unlit.death.restore", 1L, () -> restoreAndReturn(player, true, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!enabled()
                || event.getLocation() == null
                || !isUnlitWorld(event.getLocation().getWorld())
                || !plugin.getConfig().getBoolean("unlit.disable-regular-mob-spawns", true)) {
            return;
        }
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player != null && isActive(player)) restoreAndReturn(player, true, true);
    }

    private void enter(Player player) {
        if (isActive(player)) return;
        Location spawn = siteLocation(SPAWN_SITE);
        if (spawn == null || spawn.getWorld() == null) {
            player.sendMessage("Observance: unlit_spawn_mirror is not placed or its world is not loaded.");
            return;
        }
        forceUnlitNight(spawn.getWorld());
        PlayerInventory inv = player.getInventory();
        storedInventories.put(player.getUniqueId(), StoredInventory.capture(inv, player.getGameMode()));
        inv.clear();
        inv.setArmorContents(null);
        inv.addItem(borrowedLightStack(lightBudget()));
        inv.setItem(8, returnToken());
        player.setGameMode(GameMode.ADVENTURE);
        darknessExposure.put(player.getUniqueId(), 0);
        figureStage.put(player.getUniqueId(), 0);
        player.teleport(spawn);
        mergeFlag("unlit_open", true);
        mergeFlag("unlit_last_entry", SupabaseClient.timestampNow());
        player.sendMessage(Component.text("You step into the village unkept.", NamedTextColor.DARK_GRAY));
    }

    private void restoreAndReturn(Player player, boolean teleportOut, boolean log) {
        if (player == null) return;
        StoredInventory stored = storedInventories.remove(player.getUniqueId());
        if (stored != null) {
            PlayerInventory inv = player.getInventory();
            inv.clear();
            inv.setContents(stored.contents());
            inv.setArmorContents(stored.armor());
            inv.setExtraContents(stored.extra());
            player.setGameMode(stored.gameMode());
        }
        darknessExposure.remove(player.getUniqueId());
        figureStage.remove(player.getUniqueId());
        if (teleportOut) {
            Location exit = siteLocation(ENTRY_SITE);
            if (exit == null) exit = siteLocation(EXIT_SITE);
            if (exit != null && exit.getWorld() != null) player.teleport(exit);
        }
        if (log) {
            mergeFlag("unlit_last_exit", SupabaseClient.timestampNow());
            player.sendMessage(Component.text("The dark gives you back.", NamedTextColor.GRAY));
        }
    }

    private void tick() {
        if (!enabled()) return;
        World unlitWorld = Bukkit.getWorld(unlitWorldName());
        if (unlitWorld != null) maintainUnlitWorldRules(unlitWorld);
        cleanupLights();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isActive(player) || !isUnlitWorld(player.getWorld())) continue;
            boolean safe = isSafe(player.getLocation());
            int exposure = darknessExposure.getOrDefault(player.getUniqueId(), 0);
            exposure = safe ? Math.max(0, exposure - 2) : exposure + 1;
            darknessExposure.put(player.getUniqueId(), exposure);

            if (safe) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,
                        safeDarknessEffectTicks(), safeDarknessEffectAmplifier(), false, false, false));
                if (rateLimiter.tryCooldown("unlit:status:safe:" + player.getUniqueId(), 7_000L)) {
                    player.sendActionBar(Component.text("The borrowed light holds.", NamedTextColor.GRAY));
                }
                if (exposure == 0 && rateLimiter.tryCooldown(
                        "unlit:figure:decay:" + player.getUniqueId(), 35_000L)) {
                    figureStage.computeIfPresent(player.getUniqueId(), (id, stage) -> Math.max(0, stage - 1));
                }
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,
                        darknessEffectTicks(), darknessEffectAmplifier(), false, false, false));
                if (exposure >= darknessGraceSeconds()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, false));
                    if (rateLimiter.tryCooldown("unlit:status:found:" + player.getUniqueId(), 7_000L)) {
                        player.sendActionBar(Component.text("The dark is finding you.", NamedTextColor.DARK_GRAY));
                    }
                } else if (rateLimiter.tryCooldown("unlit:status:close:" + player.getUniqueId(), 7_000L)) {
                    player.sendActionBar(Component.text("The dark presses close.", NamedTextColor.DARK_GRAY));
                }
                if (exposure >= darknessDamageSeconds()) {
                    player.damage(darknessDamage());
                }
            }
            maybeMoveFigure(player, safe, exposure);
        }
    }

    private void maybeMoveFigure(Player player, boolean safe, int exposure) {
        long now = System.currentTimeMillis();
        int stage = Math.max(0, Math.min(3, figureStage.getOrDefault(player.getUniqueId(), 0)));
        int requiredExposure = switch (stage) {
            case 0 -> 6;
            case 1 -> 10;
            case 2 -> 14;
            default -> 18;
        };
        if (safe || exposure < requiredExposure) return;
        long last = lastFigureMs.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = (figureCooldownSeconds() + (stage * 6L)) * 1000L;
        if (now - last < cooldownMs) return;
        lastFigureMs.put(player.getUniqueId(), now);
        figureStage.put(player.getUniqueId(), Math.min(3, stage + 1));

        int distance = switch (stage) { case 0 -> 14; case 1 -> 11; case 2 -> 8; default -> 6; };
        double side = ((player.getUniqueId().hashCode() + stage) & 1) == 0 ? 3.5 : -3.5;
        Location standAt = chooseFigureSpot(player.getLocation(), distance, side);
        List<Entity> figure = spawnFigure(standAt, player, stage);
        if (figure.isEmpty()) return;
        figureBodies.addAll(figure);
        playFigureSound(player, standAt, stage == 0 ? Sound.BLOCK_SCULK_SENSOR_CLICKING
                : Sound.ENTITY_ENDERMAN_AMBIENT, stage == 0 ? 0.35f : 0.65f, stage == 0 ? 0.6f : 0.5f);
        if (stage == 0) mergeFlag("unlit_figure_seen", true);
        if (stage == 3) mergeFlag("unlit_figure_hunt", true);

        if (stage >= 1) {
            scheduler.runLaterSafe("unlit.figure.shift", 14L,
                    () -> shiftFigureNearPlayer(figure, player, stage >= 2 ? 5.5 : 8.0, -side));
        }
        if (stage >= 3) {
            scheduler.runLaterSafe("unlit.figure.swoop", 30L, () -> maybeSwoopFigureAtPlayer(figure, player));
        }

        if (stage >= 2) {
            LightKey nearest = nearestLight(player.getLocation());
            if (nearest != null) {
                scheduler.runLaterSafe("unlit.figure.rush", 14L, () -> {
                    Location loc = nearest.location();
                    if (loc != null) rushFigureToLight(figure, loc);
                });
                scheduler.runLaterSafe("unlit.figure.extinguish", 28L, () -> {
                    Location loc = nearest.location();
                    if (loc != null) {
                        burnOut(loc);
                        playFigureSound(player, loc, Sound.ENTITY_PHANTOM_SWOOP, 0.8f, 0.42f);
                        mergeFlag("unlit_light_taken", true);
                    }
                });
            }
        }
        scheduler.runLaterSafe("unlit.figure.vanish", figureVisibleTicks(), () -> {
            if (player.isOnline()) playFigureSound(player, player.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 0.45f, 0.55f);
            figureBodies.removeAll(figure);
            for (Entity entity : figure) {
                if (entity != null && !entity.isDead()) entity.remove();
            }
        });
    }

    private List<Entity> spawnFigure(Location loc, Player target, int stage) {
        if (loc == null || loc.getWorld() == null) return List.of();
        World world = loc.getWorld();
        List<Entity> parts = new ArrayList<>();
        try {
            Location base = loc.clone();
            base.setPitch(0f);
            if (target != null) {
                Location look = target.getLocation().clone().subtract(base);
                base.setDirection(look.toVector());
                base.setPitch(0f);
            }

            // Phase-two figure model: server-side display entities, not a vanilla mob silhouette.
            // Each piece is small and self-contained so a failed part leaves no persistent orphan.
            parts.add(spawnBlockPart(world, base, 0.00, 2.86, 0.00, 0.42f, 0.54f, 0.36f)); // narrow head
            parts.add(spawnBlockPart(world, base, 0.00, 1.88, 0.00, 0.46f, 1.45f, 0.28f)); // lank torso
            parts.add(spawnBlockPart(world, base, -0.52, 1.55, 0.04, 0.14f, 1.85f, 0.18f)); // long left arm
            parts.add(spawnBlockPart(world, base, 0.52, 1.55, 0.04, 0.14f, 1.85f, 0.18f)); // long right arm
            parts.add(spawnBlockPart(world, base, -0.17, 0.72, 0.00, 0.16f, 1.28f, 0.18f)); // long left leg
            parts.add(spawnBlockPart(world, base, 0.17, 0.72, 0.00, 0.16f, 1.28f, 0.18f)); // long right leg
            if (stage >= 1) parts.add(spawnEyeDisplay(world, figureOffset(base, 0.0, 3.00, 0.26)));
            for (Entity part : parts) {
                tagFigure(part);
                if (target != null) target.showEntity(plugin, part);
            }
            return parts;
        } catch (Throwable t) {
            for (Entity part : parts) {
                try {
                    if (part != null && !part.isDead()) part.remove();
                } catch (Throwable ignored) {
                    // Best-effort cleanup.
                }
            }
            return List.of();
        }
    }

    private BlockDisplay spawnBlockPart(World world, Location base, double x, double y, double z,
                                        float sx, float sy, float sz) {
        Location loc = figureOffset(base, x, y, z);
        return world.spawn(loc, BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.setVisibleByDefault(false);
            display.setBlock(Material.BLACK_CONCRETE.createBlockData());
            display.setBrightness(new Display.Brightness(3, 3));
            display.setViewRange(1.1f);
            display.setTransformation(new Transformation(
                    new Vector3f(-sx / 2f, -sy / 2f, -sz / 2f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(sx, sy, sz),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });
    }

    private Location figureOffset(Location base, double side, double up, double forward) {
        Location loc = base.clone();
        Vector dir = base.getDirection().clone();
        dir.setY(0.0);
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vector(0, 0, 1);
        } else {
            dir.normalize();
        }
        Vector right = new Vector(-dir.getZ(), 0.0, dir.getX());
        loc.add(right.multiply(side));
        loc.add(dir.multiply(forward));
        loc.add(0.0, up, 0.0);
        return loc;
    }

    private void rushFigureToLight(List<Entity> figure, Location lightLoc) {
        if (figure == null || figure.isEmpty() || lightLoc == null || lightLoc.getWorld() == null) return;
        Entity root = figure.get(0);
        if (root == null || root.isDead() || root.getWorld() == null) return;
        Location from = root.getLocation();
        Location to = lightLoc.clone().add(0.0, 1.2, 0.0);
        Vector delta = to.toVector().subtract(from.toVector());
        for (Entity entity : figure) {
            if (entity == null || entity.isDead() || !entity.getWorld().equals(lightLoc.getWorld())) continue;
            Location next = entity.getLocation().clone().add(delta);
            try {
                entity.teleport(next);
            } catch (Throwable ignored) {
                // One display part failing to move should not break the scare or leave the light safe.
            }
        }
        // The caller owns the private sound cue; movement itself remains silent.
    }

    private void shiftFigureNearPlayer(List<Entity> figure, Player player, double distance, double sideOffset) {
        if (figure == null || figure.isEmpty() || player == null || !player.isOnline()) return;
        if (!isActive(player) || !isUnlitWorld(player.getWorld())) return;
        Location target = player.getLocation().clone();
        Vector dir = target.getDirection().clone();
        dir.setY(0.0);
        if (dir.lengthSquared() < 1.0e-6) dir = new Vector(0, 0, 1);
        else dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0.0, dir.getX());
        target.add(dir.multiply(-distance));
        target.add(right.multiply(sideOffset));
        World world = target.getWorld();
        if (world != null) target.setY(Math.max(target.getY(), world.getHighestBlockYAt(target) + 1));
        target.setDirection(player.getLocation().toVector().subtract(target.toVector()));
        moveFigureTo(figure, target);
    }

    private void maybeSwoopFigureAtPlayer(List<Entity> figure, Player player) {
        if (figure == null || figure.isEmpty() || player == null || !player.isOnline()) return;
        if (!isActive(player) || !isUnlitWorld(player.getWorld()) || isSafe(player.getLocation())) return;
        if (Math.random() > figureSwoopChance()) return;
        Location target = player.getLocation().clone().add(0.0, 0.15, 0.0);
        moveFigureTo(figure, target);
        playFigureSound(player, target, Sound.ENTITY_PHANTOM_SWOOP, 0.85f, 0.48f);
        double damage = figureSwoopDamage();
        if (damage > 0.0) player.damage(damage);
    }

    private void moveFigureTo(List<Entity> figure, Location target) {
        if (figure == null || figure.isEmpty() || target == null || target.getWorld() == null) return;
        Entity root = figure.get(0);
        if (root == null || root.isDead() || !root.getWorld().equals(target.getWorld())) return;
        Vector delta = target.toVector().subtract(root.getLocation().toVector());
        for (Entity entity : figure) {
            if (entity == null || entity.isDead() || !entity.getWorld().equals(target.getWorld())) continue;
            try {
                entity.teleport(entity.getLocation().clone().add(delta));
            } catch (Throwable ignored) {
                // A failed display teleport should not leave the scare stuck onscreen.
            }
        }
    }

    private void playFigureSound(Player target, Location loc, Sound sound, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null || sound == null) return;
        try {
            if (target != null && target.isOnline()) target.playSound(loc, sound, volume, pitch);
        } catch (Throwable ignored) {
            // Sound names are best-effort across minor server builds.
        }
    }

    private TextDisplay spawnEyeDisplay(World world, Location loc) {
        return world.spawn(loc, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setVisibleByDefault(false);
            display.text(Component.text("·  ·", NamedTextColor.DARK_RED));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(false);
            display.setDefaultBackground(false);
            try { display.setBackgroundColor(org.bukkit.Color.fromARGB(0)); } catch (Throwable ignored) { }
            display.setBrightness(new Display.Brightness(7, 7));
            display.setViewRange(1.6f);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1.25f, 1.25f, 1.25f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });
    }

    private void tagFigure(Entity entity) {
        if (entity == null) return;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(figureKey, PersistentDataType.BYTE, (byte) 1);
    }

    private Location chooseFigureSpot(Location playerLoc, int distance, double sideOffset) {
        Location loc = playerLoc.clone();
        float yaw = loc.getYaw();
        double radians = Math.toRadians(yaw + 180.0);
        loc.add(-Math.sin(radians) * distance, 0.0, Math.cos(radians) * distance);
        Vector forward = playerLoc.getDirection().setY(0.0);
        if (forward.lengthSquared() > 1.0e-6) {
            forward.normalize();
            loc.add(new Vector(-forward.getZ(), 0.0, forward.getX()).multiply(sideOffset));
        }
        World world = loc.getWorld();
        if (world != null) {
            int y = world.getHighestBlockYAt(loc) + 1;
            loc.setY(Math.max(loc.getY(), y));
        }
        return loc;
    }

    private boolean isSafe(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (loc.getBlock().getLightLevel() >= authoredLightThreshold()) return true;
        for (Site site : currentSites().placed()) {
            if (site.type().equals("unlit_safe")
                    && site.contains(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ())) {
                return true;
            }
        }
        double radius2 = lightRadius() * lightRadius();
        for (LightKey key : liveLights.keySet()) {
            if (!key.world().equals(loc.getWorld().getName())) continue;
            double dx = key.x() + 0.5 - loc.getX();
            double dy = key.y() + 0.5 - loc.getY();
            double dz = key.z() + 0.5 - loc.getZ();
            if ((dx * dx) + (dy * dy) + (dz * dz) <= radius2) return true;
        }
        return false;
    }

    private void recordDiscovery(Player player, String houseId) {
        String house = houseId.substring("unlit_house_".length());
        String key = "unlit_seen_" + house;
        if (!reportedDiscoveries.add(key)) return;
        mergeFlag(key, true);
        if (player != null) {
            player.sendActionBar(Component.text(discoveryMessage(house), NamedTextColor.GRAY));
        }
    }

    private String discoveryMessage(String house) {
        return switch (house) {
            case "lamp" -> "The lamp account is recovered.";
            case "well" -> "The reflection gives back a missing line.";
            case "watch" -> "The dark hour is recorded.";
            case "base" -> "The copied village is filed.";
            case "threshold" -> "The low threshold is remembered.";
            case "cairn" -> "The bowl records what cannot be kept.";
            case "coop" -> "The missing call is recorded.";
            case "warm" -> "The false warmth is marked.";
            default -> "Something in this house remembers you.";
        };
    }

    private void burnOut(Location loc) {
        LightKey key = LightKey.of(loc);
        liveLights.remove(key);
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();
        Material type = block.getType();
        if (type == Material.SOUL_TORCH || type == Material.SOUL_WALL_TORCH
                || type == Material.TORCH || type == Material.WALL_TORCH
                || type == Material.SOUL_LANTERN || type == Material.LANTERN
                || type == Material.LIGHT) {
            block.setType(Material.BLACK_CANDLE, false);
        }
    }

    private void cleanupLights() {
        long now = System.currentTimeMillis();
        List<LightKey> expired = new ArrayList<>();
        for (Map.Entry<LightKey, Long> entry : liveLights.entrySet()) {
            if (entry.getValue() <= now) expired.add(entry.getKey());
        }
        for (LightKey key : expired) {
            Location loc = key.location();
            if (loc != null) burnOut(loc);
            else liveLights.remove(key);
        }
    }

    private LightKey nearestLight(Location loc) {
        if (loc == null || loc.getWorld() == null || liveLights.isEmpty()) return null;
        return liveLights.keySet().stream()
                .filter(k -> k.world().equals(loc.getWorld().getName()))
                .min(Comparator.comparingDouble(k -> {
                    double dx = k.x() - loc.getX();
                    double dy = k.y() - loc.getY();
                    double dz = k.z() - loc.getZ();
                    return (dx * dx) + (dy * dy) + (dz * dz);
                }))
                .orElse(null);
    }

    private boolean isBorrowedLight(ItemStack item) {
        if (item == null || item.getType() != Material.SOUL_LANTERN) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(lightKey, PersistentDataType.BYTE);
    }

    private boolean isReturnToken(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(returnKey, PersistentDataType.BYTE);
    }

    private ItemStack borrowedLightStack(int amount) {
        ItemStack stack = new ItemStack(Material.SOUL_LANTERN, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Borrowed lantern");
            meta.getPersistentDataContainer().set(lightKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack returnToken() {
        ItemStack stack = new ItemStack(Material.ECHO_SHARD, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("A way back");
            meta.getPersistentDataContainer().set(returnKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean isBannedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (isBorrowedLight(item) || isReturnToken(item)) return false;
        Material m = item.getType();
        String name = m.name();
        if (m.isBlock()) return true;
        return name.contains("TORCH")
                || name.contains("LANTERN")
                || name.contains("CANDLE")
                || name.contains("GLOWSTONE")
                || name.contains("FROGLIGHT")
                || name.contains("SHROOMLIGHT")
                || name.contains("BUCKET")
                || name.equals("FLINT_AND_STEEL")
                || name.equals("FIRE_CHARGE")
                || name.equals("ENDER_PEARL")
                || name.equals("CHORUS_FRUIT")
                || name.equals("FIREWORK_ROCKET")
                || name.equals("WIND_CHARGE")
                || name.contains("BOAT")
                || name.contains("MINECART")
                || name.equals("ELYTRA");
    }

    private boolean isRouteCheeseBlock(Material material) {
        if (material == null || material == Material.AIR) return false;
        String name = material.name();
        return name.contains("TRAPDOOR")
                || name.contains("FENCE_GATE")
                || name.endsWith("_BUTTON")
                || name.contains("PRESSURE_PLATE")
                || name.equals("LEVER")
                || name.contains("BED")
                || name.equals("CHEST")
                || name.equals("TRAPPED_CHEST")
                || name.equals("BARREL")
                || name.equals("ENDER_CHEST")
                || name.contains("SHULKER_BOX")
                || name.equals("DISPENSER")
                || name.equals("DROPPER")
                || name.equals("HOPPER")
                || name.equals("CRAFTING_TABLE")
                || name.equals("FURNACE")
                || name.equals("BLAST_FURNACE")
                || name.equals("SMOKER")
                || name.equals("BREWING_STAND")
                || name.equals("ANVIL")
                || name.equals("CHIPPED_ANVIL")
                || name.equals("DAMAGED_ANVIL");
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("unlit.enabled", true);
    }

    private String unlitWorldName() {
        return plugin.getConfig().getString("unlit.world", "observance_unlit");
    }

    private boolean isUnlitWorld(World world) {
        return world != null && world.getName().equals(unlitWorldName());
    }

    private boolean isUnlitBuildMode(Player player) {
        return player != null
                && isUnlitWorld(player.getWorld())
                && plugin.getConfig().getBoolean("unlit.buildmode", false)
                && (player.isOp() || player.hasPermission("observance.admin"));
    }

    private void forceUnlitNight(World world) {
        if (world == null || !plugin.getConfig().getBoolean("unlit.force-night", true)) return;
        long nightTime = plugin.getConfig().getLong("unlit.night-time", 18000L);
        world.setTime(Math.floorMod(nightTime, 24000L));
    }

    private void maintainUnlitWorldRules(World world) {
        if (world == null) return;
        forceUnlitNight(world);
        if (plugin.getConfig().getBoolean("unlit.disable-regular-mob-spawns", true)) {
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        }
    }

    private boolean isActive(Player player) {
        return player != null && storedInventories.containsKey(player.getUniqueId());
    }

    private SitesConfig currentSites() {
        SitesConfig cfg = sites == null ? null : sites.get();
        return cfg == null ? SitesConfig.empty() : cfg;
    }

    private Location siteLocation(String id) {
        Site site = currentSites().get(id);
        return site == null ? null : site.location();
    }

    private boolean atSite(Player player, String id) {
        return player != null && atSite(player.getLocation(), id);
    }

    private boolean atSite(Location loc, String id) {
        Site site = currentSites().get(id);
        return site != null && loc != null && loc.getWorld() != null
                && site.contains(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    private static boolean sameBlock(Location a, Location b) {
        return a != null && b != null
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && a.getWorld() == b.getWorld();
    }

    private void mergeFlag(String key, Object value) {
        if (supabase == null || key == null || key.isBlank()) return;
        scheduler.runAsyncSafe("unlit.flag." + key, () -> {
            JsonObject flags = new JsonObject();
            if (value instanceof Boolean b) flags.addProperty(key, b);
            else flags.addProperty(key, String.valueOf(value));
            supabase.mergeArcFlags(flags);
        });
    }

    private int lightBudget() {
        return plugin.getConfig().getInt("unlit.light-budget", 7);
    }

    private int lightRadius() {
        return Math.max(2, plugin.getConfig().getInt("unlit.light-radius", 5));
    }

    private int lightLifetimeSeconds() {
        return Math.max(10, plugin.getConfig().getInt("unlit.light-lifetime-seconds", 180));
    }

    private int authoredLightThreshold() {
        return Math.max(1, plugin.getConfig().getInt("unlit.authored-light-threshold", 8));
    }

    private int darknessGraceSeconds() {
        return Math.max(0, plugin.getConfig().getInt("unlit.darkness-grace-seconds", 8));
    }

    private int darknessEffectTicks() {
        return Math.max(60, plugin.getConfig().getInt("unlit.darkness-effect-seconds", 7) * 20);
    }

    private int darknessEffectAmplifier() {
        return Math.max(0, plugin.getConfig().getInt("unlit.darkness-effect-amplifier", 1));
    }

    private int safeDarknessEffectTicks() {
        return Math.max(40, plugin.getConfig().getInt("unlit.safe-darkness-effect-seconds", 4) * 20);
    }

    private int safeDarknessEffectAmplifier() {
        return Math.max(0, plugin.getConfig().getInt("unlit.safe-darkness-effect-amplifier", 0));
    }

    private int darknessDamageSeconds() {
        return Math.max(darknessGraceSeconds() + 1, plugin.getConfig().getInt("unlit.darkness-damage-seconds", 22));
    }

    private double darknessDamage() {
        return Math.max(0.0, plugin.getConfig().getDouble("unlit.darkness-damage", 1.0));
    }

    private int figureCooldownSeconds() {
        return Math.max(4, plugin.getConfig().getInt("unlit.figure-cooldown-seconds", 10));
    }

    private long figureVisibleTicks() {
        return Math.max(24L, plugin.getConfig().getLong("unlit.figure-visible-ticks", 48L));
    }

    private double figureSwoopDamage() {
        return Math.max(0.0, plugin.getConfig().getDouble("unlit.figure-swoop-damage", 1.0));
    }

    private double figureSwoopChance() {
        return Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("unlit.figure-swoop-chance", 0.65)));
    }

    private record StoredInventory(ItemStack[] contents, ItemStack[] armor, ItemStack[] extra, GameMode gameMode) {
        static StoredInventory capture(PlayerInventory inv, GameMode mode) {
            return new StoredInventory(
                    inv.getContents().clone(),
                    inv.getArmorContents().clone(),
                    inv.getExtraContents().clone(),
                    mode == null ? GameMode.SURVIVAL : mode
            );
        }
    }

    private record LightKey(String world, int x, int y, int z) {
        static LightKey of(Location loc) {
            String worldName = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "";
            return new LightKey(worldName, loc == null ? 0 : loc.getBlockX(), loc == null ? 0 : loc.getBlockY(),
                    loc == null ? 0 : loc.getBlockZ());
        }

        Location location() {
            World worldRef = Bukkit.getWorld(world);
            return worldRef == null ? null : new Location(worldRef, x, y, z);
        }
    }
}
