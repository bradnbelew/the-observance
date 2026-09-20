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
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.Orientable;
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

/** Player-facing G04-G05 slice: a recovered settlement, not a puzzle room. */
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
            player.sendActionBar(Component.text(
                    "The four inventory tabs settle back into their old order.", NamedTextColor.GRAY));
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
            player.sendActionBar(Component.text("A book settles into the desk lectern.", NamedTextColor.GRAY));
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
        buildFarm();
        buildHomes();
        buildLighthouse();
        buildRuinsAndTrees();
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
            if (Math.abs(x) + Math.abs(z + 31) <= 8) set(x, 5, z, Material.SPRUCE_PLANKS);
        }
        sign(-4, 2, -31, List.of("MOSSFIELD", "freight / post", "service suspended", ""));
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
        sign(-20, 1, -35, List.of("survey 03", "north edge", "do not move", ""));
    }

    private void buildServiceOffice() {
        house(-12, -10, 9, 8, Material.COBBLESTONE, Material.SPRUCE_PLANKS);
        sign(-8, 2, -10, List.of("SERVICE OFFICE", "keys / post", "leave returns", "inside"));
        lectern(-9, 1, -6, book("Mount notice", "Iona", List.of(
                "Recovery image 03\n\nThe south rail is incomplete. Do not repair it from memory. If a structure disagrees with a photograph, keep both records.",
                "Cairn's storehouse inventory was left intact. His desk index is filed by routine, not by date.")));
        barrel(-14, 1, -7, "returns", named(Material.NAME_TAG, "key tag: cairn / storehouse"));
    }

    private void buildStorehouse() {
        house(13, 5, 14, 13, Material.MOSSY_COBBLESTONE, Material.SPRUCE_PLANKS);
        sign(19, 2, 5, List.of("CAIRN", "tools / seed", "hinge sticks", "lift, don't kick"));
        lectern(14, 1, 7, book("Bench notes", "cairn", List.of(
                "If I am late: lot first. Hinge second. Chalk every returned crate. Desk lamp last.",
                "Do not sort by shine. Half these tools belonged to people before they belonged to jobs.")));
        barrel(STOREHOUSE.get("survey"), "survey", named(Material.COMPASS, "worn lot compass"),
                named(Material.STRING, "survey chain, 18 links"));
        barrel(STOREHOUSE.get("mend"), "mend", named(Material.IRON_HOE, "bent hinge spanner"),
                named(Material.IRON_NUGGET, "three saved hinge pins"));
        barrel(STOREHOUSE.get("mark"), "mark", named(Material.WHITE_DYE, "return chalk"),
                named(Material.PAPER, "crate tally, rain damaged"));
        barrel(STOREHOUSE.get("light"), "light", named(Material.FLINT_AND_STEEL, "desk taper striker"),
                named(Material.STRING, "lantern wick"));
        set(INDEX_LECTERN.x(), INDEX_LECTERN.y(), INDEX_LECTERN.z(), Material.LECTERN);
        set(20, 2, 10, Material.IRON_CHAIN);
        set(20, 3, 10, Material.LANTERN);
        barrel(26, 1, 14, "private", named(Material.FEATHER, "cairn's old signing feather"));
    }

    private void buildFarm() {
        for (int x = -27; x <= -12; x++) for (int z = 9; z <= 23; z++) {
            boolean fence = x == -27 || x == -12 || z == 9 || z == 23;
            set(x, 0, z, fence ? Material.OAK_FENCE : ((x + z) % 5 == 0 ? Material.WATER : Material.FARMLAND));
            if (!fence && (x + z) % 5 != 0 && (x * 3 + z) % 4 != 0) set(x, 1, z, Material.WHEAT);
        }
        sign(-20, 2, 9, List.of("JUNE'S LOWER", "replant west", "water gate leaks", ""));
        set(-10, 0, 18, Material.CAMPFIRE);
    }

    private void buildHomes() {
        house(-28, -5, 8, 9, Material.COBBLESTONE, Material.OAK_PLANKS);
        house(5, 18, 10, 9, Material.STONE_BRICKS, Material.SPRUCE_PLANKS);
        sign(-24, 2, -5, List.of("ROOKERY", "maps upstairs", "boots by stove", ""));
        sign(9, 2, 18, List.of("JUNE", "lens cloth", "in blue chest", ""));
        barrel(10, 1, 23, "june", named(Material.SPYGLASS, "June's salt-stained spyglass"));
    }

    private void buildLighthouse() {
        for (int x = 28; x <= 40; x++) for (int z = 28; z <= 40; z++) set(x, -1, z, Material.WATER);
        for (int x = 32; x <= 36; x++) for (int z = 32; z <= 36; z++) set(x, -1, z, Material.STONE);
        for (int y = 0; y <= 11; y++) for (int x = 32; x <= 36; x++) for (int z = 32; z <= 36; z++) {
            boolean wall = x == 32 || x == 36 || z == 32 || z == 36;
            if (wall) set(x, y, z, y % 4 == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
        }
        for (int x = 31; x <= 37; x++) for (int z = 31; z <= 37; z++) set(x, 12, z,
                (x == 31 || x == 37 || z == 31 || z == 37) ? Material.IRON_BARS : Material.SMOOTH_STONE);
        set(34, 12, 34, Material.SEA_LANTERN);
        sign(34, 2, 31, List.of("NORTH LIGHT", "June / Rookery", "lens replaced", "09-14"));
    }

    private void buildRuinsAndTrees() {
        for (int x = -5; x <= 5; x++) {
            set(x, 0, 31, x % 3 == 0 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
            if (x % 2 == 0) set(x, 1, 31, Material.COBWEB);
        }
        for (Cell tree : List.of(new Cell(-35, 0, 4), new Cell(-34, 0, 28), new Cell(2, 0, 25),
                new Cell(23, 0, -15), new Cell(37, 0, -7), new Cell(9, 0, -19))) tree(tree.x(), tree.z());
        for (Cell lantern : List.of(new Cell(0, 0, -12), new Cell(-7, 0, 3), new Cell(9, 0, 5))) {
            set(lantern.x(), 0, lantern.z(), Material.OAK_FENCE);
            set(lantern.x(), 1, lantern.z(), Material.OAK_FENCE);
            set(lantern.x(), 2, lantern.z(), Material.LANTERN);
        }
    }

    private void house(int x0, int z0, int width, int depth, Material foundation, Material wall) {
        for (int x = x0; x < x0 + width; x++) for (int z = z0; z < z0 + depth; z++) {
            set(x, 0, z, Material.SPRUCE_PLANKS);
            boolean edge = x == x0 || x == x0 + width - 1 || z == z0 || z == z0 + depth - 1;
            if (edge) for (int y = 1; y <= 4; y++) set(x, y, z, y == 1 ? foundation : wall);
            set(x, 5, z, Material.SPRUCE_SLAB);
        }
        int doorX = x0 + width / 2;
        set(doorX, 1, z0, Material.AIR);
        set(doorX, 2, z0, Material.AIR);
        for (int x : List.of(x0 + 1, x0 + width - 2)) {
            set(x, 2, z0, Material.GLASS_PANE);
        }
        for (int x : List.of(x0, x0 + width - 1)) for (int z : List.of(z0, z0 + depth - 1)) {
            pillar(x, z, 1, 5, Material.STRIPPED_SPRUCE_LOG);
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
                "Rookery took the maintenance copy below the old survey cairn.\n\nFrom the service-office door: 11 west, 24 north.",
                "The first page is not a date. Read row, page, line. Keep the notebook dry.")));
    }

    private void revealMaintenanceCache(boolean revealed) {
        if (!revealed) {
            set(MAINTENANCE_CACHE.x(), MAINTENANCE_CACHE.y(), MAINTENANCE_CACHE.z(), Material.ROOTED_DIRT);
            return;
        }
        barrel(MAINTENANCE_CACHE, "maintenance cache",
                book("Maintenance copy", "rookery", List.of(
                        "03 / 02 / 04\n\nMorrow restored the south signal shed from a planning image. We never built it. Iona says keep the image and the world both.",
                        "June's north-light photograph is older than the lens repair. Stand where her blue chest faces the water and compare the horizon.",
                        "Shutdown fragment A\n\nKEEP THE LAST CLEAN COPY OUTSIDE THE RECOVERY TREE.")),
                named(Material.RECOVERY_COMPASS, "Rookery's retained locator"),
                named(Material.PAPER, "incident fragment / copy A"));
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

    private void lectern(int x, int y, int z, ItemStack book) {
        set(x, y, z, Material.LECTERN);
        if (at(x, y, z).getState() instanceof Lectern lectern) lectern.getInventory().setItem(0, book);
    }

    private void sign(int x, int y, int z, List<String> lines) {
        set(x, y, z, Material.SPRUCE_SIGN);
        if (!(at(x, y, z).getState() instanceof Sign sign)) return;
        SignSide front = sign.getSide(Side.FRONT);
        for (int index = 0; index < 4; index++) front.line(index, Component.text(lines.get(index)));
        sign.setWaxed(true);
        sign.update(true, false);
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
