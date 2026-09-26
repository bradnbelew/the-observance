package com.observance.watcher.morrow.mossfield;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.Result;
import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.State;
import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Fence;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Player-facing G04-G06 slice in the recovered Mossfield settlement. */
public final class BukkitMossfieldExperience implements Listener, AutoCloseable {
    private static final int MIN_X = -42;
    private static final int MAX_X = 42;
    private static final int MIN_Z = -38;
    private static final int MAX_Z = 42;
    private static final Map<String, Cell> STOREHOUSE = Map.of(
            "survey", new Cell(15, 1, 8),
            "mend", new Cell(24, 1, 12),
            "mark", new Cell(17, 1, 15),
            "light", new Cell(24, 1, 7));
    private static final Cell INDEX_LECTERN = new Cell(20, 1, 10);
    private static final Cell MAINTENANCE_CACHE = new Cell(-19, 0, -34);

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final MorrowLocalState state;
    private final Map<UUID, State> trails = new HashMap<>();
    private Location spawn;
    private boolean started;

    public BukkitMossfieldExperience(JavaPlugin plugin, World world, Origin origin, MorrowLocalState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.state = Objects.requireNonNull(state, "state");
    }

    public void start() {
        if (started) return;
        verifyGround();
        build();
        boolean complete = state.snapshot().committedEvents().contains(
                MossfieldArrivalAuthority.STOREHOUSE_TRAIL);
        revealIndex(complete);
        revealMaintenanceCache(complete);
        spawn = at(0, 0, -31).getLocation().add(.5, 0, .5);
        spawn.setYaw(0.0F);
        if (!world.setSpawnLocation(spawn)) {
            throw new IllegalStateException("Paper refused the Mossfield arrival spawn");
        }
        world.setTime(6500L);
        world.setStorm(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        for (Player player : world.getPlayers()) enter(player, "startup");
        plugin.getLogger().info("MORROW_MOSSFIELD_READY world=" + world.getName()
                + " spawn=" + coordinates(spawn) + " storehouse=" + (complete ? "recovered" : "waiting"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        enter(event.getPlayer(), "join");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (started && event.getRespawnLocation().getWorld() == world) {
            event.setRespawnLocation(spawn.clone());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!started || !(event.getPlayer() instanceof Player player) || player.getWorld() != world
                || !(event.getInventory().getHolder() instanceof Barrel barrel)) return;
        String action = storehouseAction(barrel.getLocation());
        if (action == null) {
            if (sameBlock(barrel.getLocation(), at(MAINTENANCE_CACHE).getLocation())) {
                discoverMaintenanceCache(player);
            }
            return;
        }
        boolean complete = state.snapshot().committedEvents().contains(
                MossfieldArrivalAuthority.STOREHOUSE_TRAIL);
        State current = trails.getOrDefault(player.getUniqueId(), State.initial(complete));
        Result result = MossfieldArrivalAuthority.observe(current, action);
        trails.put(player.getUniqueId(), result.state());
        if (result.status() == Status.CONTINUE) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, .35F, .8F);
            return;
        }
        if (result.status() == Status.RESET) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, .45F, .72F);
            player.sendActionBar(Component.text("Cairn's desk is still empty.", NamedTextColor.GRAY));
            return;
        }
        if (result.status() == Status.COMPLETE) completeStorehouse(player);
    }

    private void enter(Player player, String reason) {
        if (!started || player == null || player.getWorld() != world) return;
        player.setGameMode(GameMode.ADVENTURE);
        if (!inside(player.getLocation()) || unsafe(player.getLocation())) player.teleport(spawn.clone());
        trails.put(player.getUniqueId(), State.initial(state.snapshot().committedEvents().contains(
                MossfieldArrivalAuthority.STOREHOUSE_TRAIL)));
        try {
            if (!state.snapshot().committedEvents().contains(MossfieldArrivalAuthority.FIRST_CONNECTION)) {
                state.commit(MossfieldArrivalAuthority.FIRST_CONNECTION,
                        "paper:mossfield:g04:first-connection:v1",
                        ("{\"surface\":\"minecraft\",\"place\":\"mossfield\",\"player_count\":"
                                + world.getPlayers().size() + ",\"production_mutation\":false}")
                                .getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().warning("Mossfield first connection remained local: " + safe(failure.getMessage()));
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.getWorld() != world) return;
            player.sendMessage(Component.text("Copperline Recovery", NamedTextColor.DARK_GRAY)
                    .append(Component.text(" // image mounted; last clean save unavailable", NamedTextColor.GRAY)));
        }, 30L);
        plugin.getLogger().info("MORROW_MOSSFIELD_ENTRY player=" + player.getName() + " reason=" + reason
                + " at=" + coordinates(player.getLocation()) + " inventory_mutations=0");
    }

    private void completeStorehouse(Player player) {
        try {
            MorrowLocalState.CommitResult committed = state.commit(
                    MossfieldArrivalAuthority.STOREHOUSE_TRAIL,
                    "paper:mossfield:g05:storehouse:v1",
                    "{\"routine\":[\"survey\",\"mend\",\"mark\",\"light\"],\"source\":\"cairn_storehouse\",\"world_mutation\":\"lectern_index_revealed\",\"production_mutation\":false}"
                            .getBytes(StandardCharsets.UTF_8));
            revealIndex(true);
            revealMaintenanceCache(true);
            player.playSound(player.getLocation(), Sound.BLOCK_CHISELED_BOOKSHELF_INSERT, .8F, .8F);
            player.sendActionBar(Component.text("Something's on Cairn's desk now.", NamedTextColor.GRAY));
            plugin.getLogger().info("MORROW_MOSSFIELD_STOREHOUSE player=" + player.getName()
                    + " created=" + committed.created() + " receipt=" + committed.eventHash());
        } catch (IOException | RuntimeException failure) {
            trails.put(player.getUniqueId(), State.initial(false));
            player.sendActionBar(Component.text("The desk remains empty. The local copy was not changed.",
                    NamedTextColor.YELLOW));
            plugin.getLogger().warning("Mossfield storehouse halted safely: " + safe(failure.getMessage()));
        }
    }

    private void build() {
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                for (int y = 0; y <= 15; y++) set(x, y, z, Material.AIR);
                set(x, -2, z, Material.DIRT);
                set(x, -1, z, edgeGround(x, z));
            }
        }
        buildPaths();
        buildRailStop();
        buildSurveyCairn();
        buildServiceOffice();
        buildStorehouse();
        buildSignalShed();
        buildFarm();
        buildHomes();
        buildLighthouse();
        buildRuinsAndTrees();
        buildGroundCover();
        auditBuild();
    }

    private void verifyGround() {
        Block boundary = at(MAX_X + 8, -1, 0);
        if (!boundary.getType().isSolid() || !at(MAX_X + 8, 0, 0).getType().isAir()) {
            throw new IllegalStateException("Mossfield needs level ground at origin Y-1 beyond its build area; "
                    + "check the world generator and origin before enabling the settlement");
        }
    }

    private void buildPaths() {
        for (int z = -32; z <= 3; z++) path(0, z, z % 7 == 0 ? Material.GRAVEL : Material.DIRT_PATH);
        for (int x = 0; x <= 20; x++) path(x, 3 + Math.max(0, x - 10) / 3, Material.DIRT_PATH);
        for (int x = -20; x <= 0; x++) path(x, 3 + Math.abs(x) / 4, Material.COARSE_DIRT);
        for (int n = 0; n <= 28; n++) path(5 + n, 5 + n, n % 5 == 0 ? Material.GRAVEL : Material.DIRT_PATH);
        for (int x = -4; x <= 4; x++) for (int z = -2; z <= 5; z++) {
            if ((x + z) % 4 == 0) set(x, -1, z, Material.GRAVEL);
        }
    }

    private void buildRailStop() {
        for (int x = -4; x <= 4; x++) for (int z = -35; z <= -27; z++) {
            set(x, -1, z, Material.STONE_BRICKS);
        }
        for (int z = -38; z <= -24; z++) {
            set(-2, 0, z, z > -27 ? Material.AIR : Material.RAIL);
            set(2, 0, z, Material.RAIL);
        }
        for (int x : List.of(-4, 4)) for (int z : List.of(-35, -27)) {
            pillar(x, z, 0, 4, Material.STRIPPED_SPRUCE_LOG);
        }
        for (int x = -5; x <= 5; x++) for (int z = -36; z <= -26; z++) {
            if (Math.abs(x) + Math.abs(z + 31) <= 8) set(x, 5, z, Material.SPRUCE_SLAB);
        }
        set(-4, 0, -31, Material.STRIPPED_SPRUCE_LOG);
        set(-4, 1, -31, Material.STRIPPED_SPRUCE_LOG);
        postSign(-4, 2, -31, BlockFace.EAST, List.of("MOSSFIELD", "freight + post", "carts by the", "back wall pls"));
        set(-4, 0, -29, Material.SPRUCE_STAIRS);
        set(3, 0, -29, Material.SPRUCE_STAIRS);
        set(-3, -1, -29, Material.SPRUCE_PLANKS);
        barrel(-3, 0, -29, "arrival", named(Material.COMPASS, "Copperline recovery compass"));
    }

    private void buildSurveyCairn() {
        set(-19, -1, -34, Material.COBBLESTONE);
        set(-20, 0, -34, Material.MOSSY_COBBLESTONE);
        set(-18, 0, -34, Material.COBBLESTONE);
        set(-19, 0, -35, Material.COBBLESTONE);
        set(-19, 0, -33, Material.MOSSY_COBBLESTONE);
        set(-19, 1, -35, Material.SPRUCE_FENCE);
        set(-19, 2, -35, Material.SOUL_LANTERN);
        set(-21, 0, -35, Material.STRIPPED_SPRUCE_LOG);
        postSign(-21, 1, -35, BlockFace.SOUTH, List.of("cairn no. 3", "Rook put this", "back crooked", "again"));
    }

    private void buildServiceOffice() {
        house(-12, -10, 9, 8, Material.COBBLESTONE, Material.SPRUCE_PLANKS);
        wallSign(-9, 2, -11, BlockFace.NORTH, List.of("OFFICE", "keys go in", "the box, Rook", "- Iona"));
        set(-11, 1, -4, Material.BOOKSHELF);
        set(-5, 1, -4, Material.FURNACE);
        set(-8, 1, -5, Material.CRAFTING_TABLE);
        set(-6, 1, -6, Material.OAK_FENCE);
        set(-6, 2, -6, Material.LANTERN);
        lectern(-9, 1, -6, book("Iona's note", "Iona", List.of(
                "Moved the copy over this morning. The south rail still ends in the grass. Please don't 'finish' it from that old picture again. It was a sketch, not a build list. - I",
                "Cairn: your four work barrels came through. Your desk book didn't. I left the key box where you can actually find it this time.")));
        barrel(-10, 1, -4, "key box", named(Material.NAME_TAG, "Cairn's shed key"));
    }

    private void buildStorehouse() {
        house(13, 5, 14, 13, Material.MOSSY_COBBLESTONE, Material.SPRUCE_PLANKS);
        wallSign(19, 2, 4, BlockFace.NORTH, List.of("CAIRN'S SHED", "shut the door", "properly pls", ""));
        set(14, 1, 14, Material.CRAFTING_TABLE);
        set(25, 1, 15, Material.CHEST);
        set(14, 2, 11, Material.BOOKSHELF);
        lectern(14, 1, 7, book("Cairn's list", "cairn", List.of(
                "Closing up: walk the lot, sort the hinge, chalk the returns, then kill the desk lamp. If June's borrowed my chalk AGAIN she can count crates herself.",
                "Rook, the pins go in the hinge tin. The seed barrel is not a hinge tin. We have had this conversation.")));
        barrel(STOREHOUSE.get("survey"), "lot bag", named(Material.COMPASS, "worn lot compass"),
                named(Material.STRING, "survey chain, 18 links"));
        barrel(STOREHOUSE.get("mend"), "hinge tin", named(Material.IRON_HOE, "bent hinge spanner"),
                named(Material.IRON_NUGGET, "three saved hinge pins"));
        barrel(STOREHOUSE.get("mark"), "returns", named(Material.WHITE_DYE, "return chalk"),
                named(Material.PAPER, "crate tally, rain damaged"));
        barrel(STOREHOUSE.get("light"), "lamp bits", named(Material.FLINT_AND_STEEL, "desk taper striker"),
                named(Material.STRING, "lantern wick"));
        set(INDEX_LECTERN.x(), INDEX_LECTERN.y(), INDEX_LECTERN.z(), Material.LECTERN);
        set(21, 1, 10, Material.OAK_FENCE);
        set(21, 2, 10, Material.LANTERN);
        barrel(25, 1, 14, "Cairn's things", named(Material.FEATHER, "Cairn's old signing feather"));
    }

    private void buildSignalShed() {
        house(7, -24, 7, 7, Material.STONE_BRICKS, Material.OAK_PLANKS);
        wallSign(9, 2, -25, BlockFace.NORTH, List.of("SOUTH SIGNAL", "leave the", "switch alone", "- Rook"));
        set(8, 1, -19, Material.REDSTONE_LAMP);
        set(12, 1, -19, Material.LEVER);
        set(11, 1, -20, Material.CHEST);
        for (int z = -26; z <= -21; z++) set(4, 0, z, Material.RAIL);
    }

    private void buildFarm() {
        for (int x = -27; x <= -12; x++) for (int z = 9; z <= 23; z++) {
            boolean fence = x == -27 || x == -12 || z == 9 || z == 23;
            boolean gate = x == -20 && z == 9;
            set(x, 0, z, gate ? Material.OAK_FENCE_GATE
                    : fence ? Material.OAK_FENCE
                    : ((x + z) % 5 == 0 ? Material.WATER : Material.FARMLAND));
            if (!fence && (x + z) % 5 != 0 && (x * 3 + z) % 4 != 0) set(x, 1, z, Material.WHEAT);
        }
        connectFences(-27, -12, 9, 23);
        set(-19, 0, 7, Material.STRIPPED_OAK_LOG);
        postSign(-19, 1, 7, BlockFace.NORTH, List.of("JUNE'S PATCH", "latch sticks", "lift it first", ""));
        set(-31, 0, 15, Material.COMPOSTER);
        set(-31, 0, 16, Material.HAY_BLOCK);
        set(-30, 0, 16, Material.HAY_BLOCK);
        set(-10, 0, 18, Material.CAMPFIRE);
    }

    private void buildHomes() {
        house(-28, -5, 8, 9, Material.COBBLESTONE, Material.OAK_PLANKS);
        house(5, 18, 10, 9, Material.STONE_BRICKS, Material.SPRUCE_PLANKS);
        wallSign(-25, 2, -6, BlockFace.NORTH, List.of("ROOK'S", "boots off", "yes, yours too", ""));
        wallSign(9, 2, 17, BlockFace.NORTH, List.of("JUNE", "back after", "the tide", ""));
        set(-27, 1, 1, Material.FURNACE);
        set(-22, 1, 1, Material.BOOKSHELF);
        set(-26, 1, 1, Material.CHEST);
        stair(-25, 1, 1, Material.OAK_STAIRS, BlockFace.NORTH);
        stair(6, 1, 24, Material.SPRUCE_STAIRS, BlockFace.NORTH);
        set(-23, 1, 0, Material.OAK_FENCE);
        set(-23, 2, 0, Material.LANTERN);
        set(8, 1, 23, Material.OAK_FENCE);
        set(8, 2, 23, Material.LANTERN);
        set(13, 1, 24, Material.BOOKSHELF);
        set(10, 0, 23, Material.BLUE_WOOL);
        chest(10, 1, 23, named(Material.GLASS_PANE, "spare lens glass"),
                named(Material.PAPER, "photo envelope / keep dry"));
        set(17, 0, 22, Material.OAK_FENCE);
        set(17, 1, 22, Material.LANTERN);
    }

    private void buildLighthouse() {
        for (int x = 28; x <= 40; x++) for (int z = 28; z <= 40; z++) set(x, -1, z, Material.WATER);
        for (int x = 32; x <= 36; x++) for (int z = 32; z <= 36; z++) set(x, -1, z, Material.STONE);
        for (int y = 0; y <= 11; y++) for (int x = 32; x <= 36; x++) for (int z = 32; z <= 36; z++) {
            boolean wall = x == 32 || x == 36 || z == 32 || z == 36;
            if (wall) set(x, y, z, y % 4 == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
            else if (y == 0) set(x, y, z, Material.STONE_BRICKS);
        }
        set(34, 1, 32, Material.AIR);
        set(34, 2, 32, Material.AIR);
        set(34, 0, 31, Material.STONE_BRICK_STAIRS);
        for (int n = 0; n <= 7; n++) {
            int x = 26 + n;
            int z = 26 + n;
            if (n >= 2) set(x, -1, z, Material.STRIPPED_SPRUCE_LOG);
            set(x, 0, z, Material.SPRUCE_PLANKS);
            set(x + 1, 0, z, Material.SPRUCE_PLANKS);
            set(x, 0, z + 1, Material.SPRUCE_PLANKS);
        }
        for (int x = 32; x <= 34; x++) set(x, 0, 31, Material.SPRUCE_PLANKS);
        for (int x = 31; x <= 37; x++) for (int z = 31; z <= 37; z++) set(x, 12, z,
                (x == 31 || x == 37 || z == 31 || z == 37) ? Material.IRON_BARS : Material.SMOOTH_STONE);
        set(34, 12, 34, Material.SEA_LANTERN);
        wallSign(33, 2, 31, BlockFace.NORTH, List.of("NORTH LIGHT", "June says the", "lens is fine", "ask Rook"));
    }

    private void buildRuinsAndTrees() {
        for (int x = -5; x <= 5; x++) {
            set(x, 0, 31, x % 3 == 0 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
            if (x % 2 == 0) set(x, 1, 31, Material.COBWEB);
        }
        for (Cell tree : List.of(new Cell(-35, 0, 4), new Cell(-34, 0, 28), new Cell(2, 0, 25),
                new Cell(23, 0, -15), new Cell(37, 0, -7), new Cell(18, 0, -22),
                new Cell(-38, 0, -20), new Cell(-39, 0, 18), new Cell(4, 0, 39))) tree(tree.x(), tree.z());
        for (Cell lantern : List.of(new Cell(0, 0, -12), new Cell(-7, 0, 3), new Cell(9, 0, 5))) {
            set(lantern.x(), 0, lantern.z(), Material.OAK_FENCE);
            set(lantern.x(), 1, lantern.z(), Material.OAK_FENCE);
            set(lantern.x(), 2, lantern.z(), Material.LANTERN);
        }
    }

    private void buildGroundCover() {
        for (int x = MIN_X + 2; x <= MAX_X - 2; x++) for (int z = MIN_Z + 2; z <= MAX_Z - 2; z++) {
            if (Math.abs(x) <= 5 && z < -24) continue;
            if (Math.floorMod(x * 17 + z * 13, 19) != 0) continue;
            if (at(x, 0, z).getType() != Material.AIR
                    || at(x, -1, z).getType() != Material.GRASS_BLOCK) continue;
            int patch = Math.floorMod(x * 7 - z * 11, 23);
            set(x, 0, z, patch == 0 ? Material.DANDELION
                    : patch == 1 ? Material.POPPY : Material.SHORT_GRASS);
        }
    }

    private void house(int x0, int z0, int width, int depth, Material foundation, Material wall) {
        for (int x = x0; x < x0 + width; x++) for (int z = z0; z < z0 + depth; z++) {
            set(x, 0, z, Material.SPRUCE_PLANKS);
            boolean edge = x == x0 || x == x0 + width - 1 || z == z0 || z == z0 + depth - 1;
            if (edge) for (int y = 1; y <= 4; y++) set(x, y, z, y == 1 ? foundation : wall);
        }
        int doorX = x0 + width / 2;
        set(doorX, 1, z0, Material.AIR);
        set(doorX, 2, z0, Material.AIR);
        stair(doorX, 0, z0 - 1, Material.SPRUCE_STAIRS, BlockFace.SOUTH);
        for (int x : List.of(x0 + 1, x0 + width - 2)) {
            set(x, 2, z0, Material.GLASS_PANE);
        }
        for (int x : List.of(x0, x0 + width - 1)) for (int z : List.of(z0, z0 + depth - 1)) {
            pillar(x, z, 1, 5, Material.STRIPPED_SPRUCE_LOG);
        }
        for (int z = z0 - 1; z <= z0 + depth; z++) {
            for (int inset = 0; inset < (width + 2) / 2; inset++) {
                int roofY = 5 + inset;
                stair(x0 - 1 + inset, roofY, z, Material.SPRUCE_STAIRS, BlockFace.EAST);
                stair(x0 + width - inset, roofY, z, Material.SPRUCE_STAIRS, BlockFace.WEST);
            }
            if (width % 2 == 1) set(x0 + width / 2, 5 + (width + 2) / 2, z, Material.SPRUCE_SLAB);
        }
        for (int x = x0; x < x0 + width; x++) {
            for (int y = 5; y <= 5 + width / 2; y++) {
                int edgeDistance = Math.min(x - x0, x0 + width - 1 - x);
                if (y <= 5 + edgeDistance) {
                    set(x, y, z0, wall);
                    set(x, y, z0 + depth - 1, wall);
                }
            }
        }
    }

    private void tree(int x, int z) {
        pillar(x, z, 0, 5, Material.SPRUCE_LOG);
        for (int y = 3; y <= 7; y++) {
            int radius = y >= 6 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= radius + 1) set(x + dx, y, z + dz, Material.SPRUCE_LEAVES);
            }
        }
    }

    private void revealIndex(boolean revealed) {
        Block block = at(INDEX_LECTERN);
        if (!(block.getState() instanceof Lectern lectern)) return;
        lectern.getInventory().clear();
        if (revealed) lectern.getInventory().setItem(0, book("Storehouse index", "cairn", List.of(
                "Rook nicked the spare log again. Says cairn no. 3 keeps it drier than my desk. From Iona's office step: 11 west, 24 north. If he buried it under the cobbles, he can dig it up himself.",
                "Those three numbers on the cover are row / page / line, not a date. Please stop asking me.")));
    }

    private void revealMaintenanceCache(boolean revealed) {
        if (!revealed) {
            set(MAINTENANCE_CACHE.x(), MAINTENANCE_CACHE.y(), MAINTENANCE_CACHE.z(), Material.ROOTED_DIRT);
            return;
        }
        barrel(MAINTENANCE_CACHE, "Rook's dry box",
                book("The spare log", "Rookery", List.of(
                        "03 / 02 / 04\n\nThe south signal shed is back. I drew that thing once. We never built it. Iona still has the sketch, thank god. Keep both copies until we know where this one came from.",
                        "June took a picture from her front step before the lens job. Find the envelope in her blue box. Look at the shore behind her, not the tower.",
                        "If we have to pull the plug, the last clean copy stays OUTSIDE the recovery folder. I mean it, Theo.")),
                named(Material.COMPASS, "Rook's old locator"),
                named(Material.PAPER, "folded incident note"));
    }

    private void discoverMaintenanceCache(Player player) {
        if (!state.snapshot().committedEvents().contains(MossfieldArrivalAuthority.STOREHOUSE_TRAIL)
                || state.snapshot().committedEvents().contains(MossfieldArrivalAuthority.MAINTENANCE_CACHE)) return;
        try {
            MorrowLocalState.CommitResult committed = state.commit(
                    MossfieldArrivalAuthority.MAINTENANCE_CACHE,
                    "paper:mossfield:g06:maintenance-cache:v1",
                    "{\"source\":\"rookery_maintenance_copy\",\"coordinate_origin\":\"service_office_door\",\"offset\":\"11_west_24_north\",\"production_mutation\":false}"
                            .getBytes(StandardCharsets.UTF_8));
            player.playSound(player.getLocation(), Sound.BLOCK_CHISELED_BOOKSHELF_PICKUP_ENCHANTED,
                    .7F, .75F);
            plugin.getLogger().info("MORROW_MOSSFIELD_CACHE player=" + player.getName()
                    + " created=" + committed.created() + " receipt=" + committed.eventHash());
        } catch (IOException | RuntimeException failure) {
            player.sendActionBar(Component.text("The cache stays intact; the local record did not change.",
                    NamedTextColor.YELLOW));
            plugin.getLogger().warning("Mossfield maintenance cache halted safely: " + safe(failure.getMessage()));
        }
    }

    private String storehouseAction(Location location) {
        for (Map.Entry<String, Cell> entry : STOREHOUSE.entrySet()) {
            Block expected = at(entry.getValue());
            if (expected.getX() == location.getBlockX() && expected.getY() == location.getBlockY()
                    && expected.getZ() == location.getBlockZ()) return entry.getKey();
        }
        return null;
    }

    private static boolean sameBlock(Location left, Location right) {
        return left.getWorld() == right.getWorld() && left.getBlockX() == right.getBlockX()
                && left.getBlockY() == right.getBlockY() && left.getBlockZ() == right.getBlockZ();
    }

    private void barrel(Cell cell, String name, ItemStack... contents) {
        barrel(cell.x(), cell.y(), cell.z(), name, contents);
    }

    private void barrel(int x, int y, int z, String name, ItemStack... contents) {
        set(x, y, z, Material.BARREL);
        if (!(at(x, y, z).getState() instanceof Barrel barrel)) return;
        barrel.customName(Component.text(name, NamedTextColor.DARK_GRAY));
        barrel.getInventory().clear();
        for (int index = 0; index < contents.length; index++) barrel.getInventory().setItem(index, contents[index]);
        barrel.update(true, false);
    }

    private void chest(int x, int y, int z, ItemStack... contents) {
        set(x, y, z, Material.CHEST);
        if (!(at(x, y, z).getState() instanceof Chest chest)) return;
        chest.getInventory().clear();
        for (int index = 0; index < contents.length; index++) chest.getInventory().setItem(index, contents[index]);
        chest.update(true, false);
    }

    private void lectern(int x, int y, int z, ItemStack book) {
        set(x, y, z, Material.LECTERN);
        if (at(x, y, z).getState() instanceof Lectern lectern) lectern.getInventory().setItem(0, book);
    }

    private void wallSign(int x, int y, int z, BlockFace facing, List<String> lines) {
        set(x, y, z, Material.SPRUCE_WALL_SIGN);
        Directional direction = (Directional) at(x, y, z).getBlockData();
        direction.setFacing(facing);
        at(x, y, z).setBlockData(direction, false);
        writeSign(x, y, z, lines);
    }

    private void postSign(int x, int y, int z, BlockFace facing, List<String> lines) {
        set(x, y, z, Material.SPRUCE_SIGN);
        Rotatable rotation = (Rotatable) at(x, y, z).getBlockData();
        rotation.setRotation(facing);
        at(x, y, z).setBlockData(rotation, false);
        writeSign(x, y, z, lines);
    }

    private void writeSign(int x, int y, int z, List<String> lines) {
        if (!(at(x, y, z).getState() instanceof Sign sign)) return;
        SignSide front = sign.getSide(Side.FRONT);
        for (int index = 0; index < 4; index++) front.line(index, Component.text(lines.get(index)));
        sign.setWaxed(true);
        sign.update(true, false);
    }

    private void stair(int x, int y, int z, Material material, BlockFace facing) {
        set(x, y, z, material);
        Directional direction = (Directional) at(x, y, z).getBlockData();
        direction.setFacing(facing);
        at(x, y, z).setBlockData(direction, false);
    }

    private void connectFences(int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            Block block = at(x, 0, z);
            if (!(block.getBlockData() instanceof Fence fence)) continue;
            for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                Material neighbor = block.getRelative(face).getType();
                fence.setFace(face, neighbor == Material.OAK_FENCE || neighbor == Material.OAK_FENCE_GATE);
            }
            block.setBlockData(fence, false);
        }
    }

    private void auditBuild() {
        requireBlock(-9, 2, -11, Material.SPRUCE_WALL_SIGN);
        requireBlock(19, 2, 4, Material.SPRUCE_WALL_SIGN);
        requireBlock(-10, 1, -4, Material.BARREL);
        requireBlock(25, 1, 14, Material.BARREL);
        requireBlock(10, 1, 23, Material.CHEST);
        requireBlock(20, 1, 10, Material.LECTERN);
        requireBlock(21, 2, 10, Material.LANTERN);
        requireBlock(21, 1, 10, Material.OAK_FENCE);
        requireBlock(34, 1, 32, Material.AIR);
        requireBlock(10, 1, -20, Material.AIR);
        for (Cell cell : STOREHOUSE.values()) {
            requireBlock(cell.x(), cell.y(), cell.z(), Material.BARREL);
            if (at(cell.x(), cell.y() - 1, cell.z()).getType().isAir()) {
                throw new IllegalStateException("Mossfield storehouse barrel lacks a floor at " + cell);
            }
        }
        Fence fence = (Fence) at(-21, 0, 9).getBlockData();
        if (!fence.hasFace(BlockFace.EAST) || !fence.hasFace(BlockFace.WEST)) {
            throw new IllegalStateException("Mossfield farm fence is disconnected");
        }
    }

    private void requireBlock(int x, int y, int z, Material expected) {
        if (at(x, y, z).getType() != expected) {
            throw new IllegalStateException("Mossfield build mismatch at " + x + "," + y + "," + z
                    + ": expected " + expected + " but found " + at(x, y, z).getType());
        }
    }

    private static ItemStack book(String title, String author, List<String> pages) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.setTitle(title);
        meta.setAuthor(author);
        meta.pages(pages.stream().map(page -> (Component) Component.text(page)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    private void path(int x, int z, Material material) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 1) set(x + dx, -1, z + dz, material);
        }
    }

    private void pillar(int x, int z, int fromY, int toY, Material material) {
        for (int y = fromY; y <= toY; y++) {
            set(x, y, z, material);
            if (at(x, y, z).getBlockData() instanceof Orientable axis) {
                axis.setAxis(Axis.Y);
                at(x, y, z).setBlockData(axis, false);
            }
        }
    }

    private Material edgeGround(int x, int z) {
        int hash = Math.floorMod(x * 31 + z * 17, 29);
        if (hash == 0) return Material.MOSS_BLOCK;
        if (hash <= 2) return Material.COARSE_DIRT;
        return Material.GRASS_BLOCK;
    }

    private boolean inside(Location location) {
        if (location.getWorld() != world) return false;
        int x = location.getBlockX() - origin.x();
        int z = location.getBlockZ() - origin.z();
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    private boolean unsafe(Location location) {
        Block feet = location.getBlock();
        return !feet.isPassable() || !feet.getRelative(0, 1, 0).isPassable()
                || feet.getRelative(0, -1, 0).isPassable();
    }

    private Block at(Cell cell) { return at(cell.x(), cell.y(), cell.z()); }
    private Block at(int x, int y, int z) {
        return world.getBlockAt(origin.x() + x, origin.y() + y, origin.z() + z);
    }
    private void set(int x, int y, int z, Material material) { at(x, y, z).setType(material, false); }
    private static String coordinates(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }

    @Override public void close() {
        HandlerList.unregisterAll(this);
        trails.clear();
        started = false;
    }

    public record Origin(int x, int y, int z) { }
    private record Cell(int x, int y, int z) { }
}
