package com.observance.watcher.v5runtime.unlit;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.v5runtime.unlit.UnlitCopyProofState.CommitStatus;
import com.observance.watcher.v5runtime.unlit.UnlitCopyProofState.Snapshot;
import com.observance.watcher.v5runtime.unlit.UnlitCopyProofState.Token;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Real Paper 1.21.11 input adapter for the bounded player-authored surface-to-Unlit copy proof.
 * Six protected selectors are the primary physical verb; {@code /obscopy} is the keyboard/recovery
 * fallback with the same predicate. Installation is an explicit admin-only disposable/staging step.
 */
public final class UnlitCopyProofRuntime implements Listener, AutoCloseable {
    private static final String LAYOUT_VERSION = "unlit-copy-layout-v1";
    private static final int WIDTH = 7;
    private static final int DEPTH = 4;
    private static final int[][] CELLS = {{0, 0}, {2, 0}, {4, 0}, {0, 2}, {2, 2}, {4, 2}};
    private static final int CONFIRM_X = 6;
    private static final int CONFIRM_Z = 0;
    private static final int RESET_X = 6;
    private static final int RESET_Z = 2;
    private static final int LECTERN_X = 6;
    private static final int LECTERN_Z = 3;
    private static final List<Token> DEFAULT_PATTERN = List.of(
            Token.WATER, Token.HEAT, Token.RECORD,
            Token.WATCH, Token.WATER, Token.HEAT);
    private static final Map<Token, Material> TOKEN_MATERIALS = tokenMaterials();
    private static final Map<Material, Token> MATERIAL_TOKENS = materialTokens();
    private static final Set<Material> FRESH_REPLACEABLE = Set.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.STONE, Material.DEEPSLATE, Material.BEDROCK,
            Material.SHORT_GRASS, Material.TALL_GRASS, Material.SNOW);
    private static final Set<Material> MANAGED_MATERIALS = managedMaterials();

    private final ObservancePlugin plugin;
    private final Path layoutPath;
    private final UnlitCopyProofState state;
    private Layout layout;

    public UnlitCopyProofRuntime(ObservancePlugin plugin) throws IOException {
        this.plugin = plugin;
        Path data = plugin.getDataFolder().toPath();
        this.layoutPath = data.resolve("unlit-copy-proof-layout.txt");
        this.state = UnlitCopyProofState.open(data.resolve("unlit-copy-proof.journal"));
        this.layout = loadLayout(layoutPath);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("obscopy", "Bounded Unlit copy register and accessibility fallback",
                    List.of("observancecopy"), new PlayerCommand());
            event.registrar().register("obscopyproof", "Install and audit the bounded Unlit copy proof",
                    List.of(), new AdminCommand());
        });
        if (layout != null) {
            try {
                loadLayoutWorlds(layout);
                reconcile(true);
                Snapshot committed = state.committed();
                if (committed != null) commitCampaignEvent(committed);
            } catch (RuntimeException failure) {
                plugin.getLogger().severe("Unlit copy proof failed closed during recovery: "
                        + safe(failure.getMessage()));
            }
        }
        plugin.getLogger().info("UNLIT_COPY_PROOF_READY installed=" + (layout != null)
                + " committed=" + (state.committed() != null)
                + " input=/obscopy observation_gating=false personalized_input=false");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (layout == null || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null
                || (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_BLOCK)) return;
        Block block = event.getClickedBlock();
        Panel panel = panelAt(block.getLocation());
        if (panel == null) return;
        event.setCancelled(true);
        if (panel.kind() == PanelKind.UNLIT) {
            event.getPlayer().sendActionBar(Component.text(
                    state.committed() == null
                            ? "The return register is blank."
                            : "This is the returned copy. It does not take entries.",
                    NamedTextColor.DARK_GRAY));
            return;
        }
        if (state.committed() != null) {
            event.getPlayer().sendActionBar(Component.text(
                    "The surface register has already sent its one copy.", NamedTextColor.GRAY));
            return;
        }
        int relativeX = block.getX() - panel.anchor().x();
        int relativeZ = block.getZ() - panel.anchor().z();
        int cell = cellIndex(relativeX, relativeZ);
        try {
            if (cell >= 0 && block.getY() == panel.anchor().y()) {
                cycle(block, event.getPlayer(), cell);
            } else if (relativeX == CONFIRM_X && relativeZ == CONFIRM_Z
                    && block.getY() == panel.anchor().y()) {
                confirm(event.getPlayer());
            } else if (relativeX == RESET_X && relativeZ == RESET_Z
                    && block.getY() == panel.anchor().y()) {
                reset(event.getPlayer());
            }
        } catch (IOException | IllegalStateException failure) {
            event.getPlayer().sendMessage(Component.text(
                    "The register failed safely. Nothing was sent. Use /obscopy status.",
                    NamedTextColor.RED));
            plugin.getLogger().severe("Unlit copy proof input failed: " + safe(failure.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (panelAt(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (panelAt(event.getBlockPlaced().getLocation()) != null) event.setCancelled(true);
    }

    private void cycle(Block block, Player player, int cell) {
        Token current = MATERIAL_TOKENS.get(block.getType());
        if (current == null) {
            player.sendActionBar(Component.text("That selector is damaged. Use /obscopy status.",
                    NamedTextColor.RED));
            return;
        }
        Token next = Token.values()[(current.ordinal() + 1) % Token.values().length];
        block.setType(TOKEN_MATERIALS.get(next), false);
        player.sendActionBar(Component.text(
                "Selector " + (cell + 1) + ": " + next.wire() + ".",
                NamedTextColor.GRAY));
    }

    private void confirm(Player player) throws IOException {
        requireNearSurface(player);
        List<Token> proposed = readSurfacePattern();
        UnlitCopyProofState.CommitOutcome outcome;
        try {
            outcome = state.commit(proposed);
        } catch (IllegalArgumentException invalid) {
            player.sendMessage(Component.text(
                    "The register needs six marks, at least three civic roles, and a record mark. Nothing was sent.",
                    NamedTextColor.GRAY));
            return;
        }
        projectCommitted(outcome.snapshot());
        commitCampaignEvent(outcome.snapshot());
        player.sendMessage(Component.text(switch (outcome.status()) {
            case COMMITTED -> "The surface register sends one bounded copy. Check the return register in the Unlit.";
            case IDEMPOTENT -> "That exact copy was already sent. The return register is unchanged.";
            case LOCKED -> "A different copy is already fixed in the return register. Nothing changed.";
        }, outcome.status() == CommitStatus.LOCKED ? NamedTextColor.GRAY : NamedTextColor.DARK_AQUA));
    }

    private void reset(Player player) {
        requireNearSurface(player);
        if (state.committed() != null) {
            player.sendMessage(Component.text("The sent pattern is fixed. Reset cannot change it.",
                    NamedTextColor.GRAY));
            return;
        }
        writePattern(layout.surface(), DEFAULT_PATTERN);
        player.sendActionBar(Component.text("The six selectors return to the register's starting marks.",
                NamedTextColor.GRAY));
    }

    private void set(Player player, int oneBasedCell, Token token) {
        requireNearSurface(player);
        if (state.committed() != null) {
            player.sendMessage(Component.text("The sent pattern is fixed.", NamedTextColor.GRAY));
            return;
        }
        if (oneBasedCell < 1 || oneBasedCell > CELLS.length) {
            throw new IllegalArgumentException("cell must be 1 through 6");
        }
        Anchor at = layout.surface();
        int[] offset = CELLS[oneBasedCell - 1];
        World world = requireWorld(at.world());
        world.getBlockAt(at.x() + offset[0], at.y(), at.z() + offset[1])
                .setType(TOKEN_MATERIALS.get(token), false);
        player.sendMessage(Component.text(
                "Selector " + oneBasedCell + " is now " + token.wire() + ".",
                NamedTextColor.GRAY));
    }

    private void commitCampaignEvent(Snapshot snapshot) {
        if (plugin.v5Runtime() == null) return;
        plugin.v5Runtime().commitUnlitCopyProof(snapshot.patternSha256());
    }

    private void projectCommitted(Snapshot snapshot) {
        writePattern(layout.surface(), snapshot.surface());
        writePattern(layout.unlit(), snapshot.unlit());
    }

    private List<Token> readSurfacePattern() {
        Anchor at = layout.surface();
        World world = requireWorld(at.world());
        List<Token> result = new ArrayList<>(CELLS.length);
        for (int[] offset : CELLS) {
            Material material = world.getBlockAt(at.x() + offset[0], at.y(), at.z() + offset[1]).getType();
            Token token = MATERIAL_TOKENS.get(material);
            if (token == null) throw new IllegalStateException("surface selector has unclassified material " + material);
            result.add(token);
        }
        return List.copyOf(result);
    }

    private void reconcile(boolean recover) {
        if (layout == null) return;
        installPanel(layout.surface(), PanelKind.SURFACE, recover);
        installPanel(layout.unlit(), PanelKind.UNLIT, recover);
        Snapshot committed = state.committed();
        if (committed == null) {
            writePattern(layout.surface(), DEFAULT_PATTERN);
            writeBlank(layout.unlit());
        } else {
            projectCommitted(committed);
        }
    }

    private void installPanel(Anchor anchor, PanelKind kind, boolean recover) {
        World world = requireWorld(anchor.world());
        world.getChunkAt(anchor.x() >> 4, anchor.z() >> 4).load(true);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                Block support = world.getBlockAt(anchor.x() + x, anchor.y() - 1, anchor.z() + z);
                requireReplaceable(support, recover);
                support.setType((x == WIDTH - 1) ? Material.CUT_COPPER : Material.POLISHED_DEEPSLATE, false);
                Block standing = world.getBlockAt(anchor.x() + x, anchor.y(), anchor.z() + z);
                if (cellIndex(x, z) < 0 && !(kind == PanelKind.SURFACE
                        && ((x == CONFIRM_X && z == CONFIRM_Z) || (x == RESET_X && z == RESET_Z)))
                        && !(x == LECTERN_X && z == LECTERN_Z)) {
                    requireReplaceable(standing, recover);
                    standing.setType(Material.AIR, false);
                }
            }
        }
        if (kind == PanelKind.SURFACE) {
            setFloorLever(world.getBlockAt(anchor.x() + CONFIRM_X, anchor.y(), anchor.z() + CONFIRM_Z));
            setFloorLever(world.getBlockAt(anchor.x() + RESET_X, anchor.y(), anchor.z() + RESET_Z));
        }
        setLectern(world.getBlockAt(anchor.x() + LECTERN_X, anchor.y(), anchor.z() + LECTERN_Z), kind);
    }

    private void requireReplaceable(Block block, boolean recover) {
        Material material = block.getType();
        if (FRESH_REPLACEABLE.contains(material)) return;
        if (recover && MANAGED_MATERIALS.contains(material)) return;
        throw new IllegalStateException("refused to overwrite " + material + " at "
                + block.getWorld().getName() + ':' + block.getX() + ',' + block.getY() + ',' + block.getZ());
    }

    private void setFloorLever(Block block) {
        requireReplaceable(block, true);
        block.setType(Material.LEVER, false);
        Switch data = (Switch) block.getBlockData();
        data.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
        data.setFacing(BlockFace.NORTH);
        data.setPowered(false);
        block.setBlockData(data, false);
    }

    private void setLectern(Block block, PanelKind kind) {
        requireReplaceable(block, true);
        block.setType(Material.LECTERN, false);
        Directional data = (Directional) block.getBlockData();
        data.setFacing(BlockFace.SOUTH);
        block.setBlockData(data, false);
        Lectern lectern = (Lectern) block.getState();
        lectern.getInventory().setItem(0, registerBook(kind));
        lectern.update(true, false);
    }

    private static ItemStack registerBook(PanelKind kind) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (kind == PanelKind.SURFACE) {
            meta.title(Component.text("Pattern register"));
            meta.author(Component.text("Hold works office"));
            meta.pages(List.of(
                    Component.text("PATTERN REGISTER\n\nSet six civic marks.\nTouch a mark to change it.\n\nThe brass handle sends one copy.\nThe stone handle clears it."),
                    Component.text("MARKS\n\nPrismarine: water\nCut copper: heat\nLodestone: watch\nChiseled stone: record\n\nIf the panel is hard to use:\n/obscopy help")));
        } else {
            meta.title(Component.text("Returned copy"));
            meta.author(Component.text("No listed office"));
            meta.pages(List.of(Component.text(
                    "RETURN REGISTER\n\nThis desk does not take entries.\n\nCompare these six marks with the pattern sent from the surface.")));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void writePattern(Anchor anchor, List<Token> tokens) {
        World world = requireWorld(anchor.world());
        for (int index = 0; index < CELLS.length; index++) {
            int[] offset = CELLS[index];
            world.getBlockAt(anchor.x() + offset[0], anchor.y(), anchor.z() + offset[1])
                    .setType(TOKEN_MATERIALS.get(tokens.get(index)), false);
        }
    }

    private void writeBlank(Anchor anchor) {
        World world = requireWorld(anchor.world());
        for (int[] offset : CELLS) {
            world.getBlockAt(anchor.x() + offset[0], anchor.y(), anchor.z() + offset[1])
                    .setType(Material.TINTED_GLASS, false);
        }
    }

    public String auditSummary() {
        if (layout == null) return "UNLIT_COPY_PROOF_AUDIT BLOCKED layout-not-installed";
        List<String> faults = new ArrayList<>();
        auditPanel(layout.surface(), PanelKind.SURFACE, faults);
        auditPanel(layout.unlit(), PanelKind.UNLIT, faults);
        Snapshot snapshot = state.committed();
        if (snapshot != null) {
            try {
                if (!readPattern(layout.surface()).equals(snapshot.surface())) faults.add("surface-pattern-drift");
                if (!readPattern(layout.unlit()).equals(snapshot.unlit())) faults.add("unlit-copy-drift");
            } catch (RuntimeException failure) {
                faults.add("pattern-read:" + safe(failure.getMessage()));
            }
        }
        if (!faults.isEmpty()) return "UNLIT_COPY_PROOF_AUDIT BLOCKED " + String.join(";", faults);
        return "UNLIT_COPY_PROOF_AUDIT PASS cells=6 bounded=true personalized=false committed="
                + (snapshot != null) + " pattern_sha256="
                + (snapshot == null ? "none" : snapshot.patternSha256());
    }

    private void auditPanel(Anchor anchor, PanelKind kind, List<String> faults) {
        World world = Bukkit.getWorld(anchor.world());
        if (world == null) {
            faults.add(kind.name().toLowerCase(Locale.ROOT) + "-world-unloaded");
            return;
        }
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                Material expected = x == WIDTH - 1 ? Material.CUT_COPPER : Material.POLISHED_DEEPSLATE;
                Material actual = world.getBlockAt(anchor.x() + x, anchor.y() - 1, anchor.z() + z).getType();
                if (actual != expected) faults.add(kind + "-support-" + x + '-' + z + '=' + actual);
            }
        }
        if (world.getBlockAt(anchor.x() + LECTERN_X, anchor.y(), anchor.z() + LECTERN_Z).getType()
                != Material.LECTERN) faults.add(kind + "-lectern-missing");
        if (kind == PanelKind.SURFACE) {
            for (int[] control : List.of(new int[]{CONFIRM_X, CONFIRM_Z}, new int[]{RESET_X, RESET_Z})) {
                if (world.getBlockAt(anchor.x() + control[0], anchor.y(), anchor.z() + control[1]).getType()
                        != Material.LEVER) faults.add("surface-control-missing-" + Arrays.toString(control));
            }
        }
        for (int[] offset : CELLS) {
            Material actual = world.getBlockAt(anchor.x() + offset[0], anchor.y(), anchor.z() + offset[1]).getType();
            if (kind == PanelKind.SURFACE && !MATERIAL_TOKENS.containsKey(actual)) {
                faults.add("surface-unclassified-token=" + actual);
            }
            if (kind == PanelKind.UNLIT && state.committed() == null && actual != Material.TINTED_GLASS) {
                faults.add("unlit-uncommitted-cell=" + actual);
            }
        }
    }

    private List<Token> readPattern(Anchor anchor) {
        World world = requireWorld(anchor.world());
        List<Token> result = new ArrayList<>(CELLS.length);
        for (int[] offset : CELLS) {
            Token token = MATERIAL_TOKENS.get(world.getBlockAt(
                    anchor.x() + offset[0], anchor.y(), anchor.z() + offset[1]).getType());
            if (token == null) throw new IllegalStateException("unclassified token");
            result.add(token);
        }
        return List.copyOf(result);
    }

    private void requireNearSurface(Player player) {
        if (layout == null) throw new IllegalStateException("copy proof is not installed");
        Anchor at = layout.surface();
        Location location = player.getLocation();
        if (location.getWorld() == null || !location.getWorld().getName().equals(at.world())
                || location.distanceSquared(new Location(location.getWorld(), at.x() + 3.0, at.y(), at.z() + 1.5)) > 144.0) {
            throw new IllegalStateException("stand at the surface pattern register to change or send it");
        }
    }

    private Panel panelAt(Location location) {
        if (layout == null || location == null || location.getWorld() == null) return null;
        if (inside(location, layout.surface())) return new Panel(PanelKind.SURFACE, layout.surface());
        if (inside(location, layout.unlit())) return new Panel(PanelKind.UNLIT, layout.unlit());
        return null;
    }

    private static boolean inside(Location location, Anchor anchor) {
        return location.getWorld().getName().equals(anchor.world())
                && location.getBlockX() >= anchor.x() && location.getBlockX() < anchor.x() + WIDTH
                && location.getBlockZ() >= anchor.z() && location.getBlockZ() < anchor.z() + DEPTH
                && location.getBlockY() >= anchor.y() - 1 && location.getBlockY() <= anchor.y() + 1;
    }

    private static int cellIndex(int x, int z) {
        for (int index = 0; index < CELLS.length; index++) {
            if (CELLS[index][0] == x && CELLS[index][1] == z) return index;
        }
        return -1;
    }

    private World requireWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) throw new IllegalStateException("world is not loaded: " + name);
        return world;
    }

    private void install(CommandSender sender, String[] args) throws IOException {
        if (args.length != 9) {
            sender.sendMessage("Usage: /obscopyproof install <surfaceWorld> <x> <y> <z> <unlitWorld> <x> <y> <z>");
            return;
        }
        World surfaceWorld = Bukkit.getWorld(args[1]);
        if (surfaceWorld == null) throw new IllegalArgumentException("surface world is not loaded: " + args[1]);
        String unlitWorldName = safeWorldName(args[5]);
        World unlitWorld = Bukkit.getWorld(unlitWorldName);
        if (unlitWorld == null) {
            unlitWorld = WorldCreator.name(unlitWorldName).type(WorldType.FLAT)
                    .generateStructures(false).seed(9137L).createWorld();
        }
        if (unlitWorld == null) throw new IllegalStateException("could not create/load Unlit world");
        Layout proposed = new Layout(
                new Anchor(surfaceWorld.getName(), integer(args[2]), integer(args[3]), integer(args[4])),
                new Anchor(unlitWorld.getName(), integer(args[6]), integer(args[7]), integer(args[8])));
        if (layout != null && !layout.equals(proposed)) {
            throw new IllegalStateException("a different copy-proof layout is already installed");
        }
        if (layout == null) {
            saveLayout(layoutPath, proposed);
            layout = proposed;
        }
        reconcile(layoutPath.toFile().exists());
        sender.sendMessage("UNLIT_COPY_PROOF_INSTALL PASS surface=" + proposed.surface().wire()
                + " unlit=" + proposed.unlit().wire());
        sender.sendMessage(auditSummary());
    }

    private void exerciseDefault(CommandSender sender) throws IOException {
        if (!plugin.getConfig().getBoolean("unlit.copy-proof.disposable-audit-enabled", false)) {
            throw new IllegalStateException("disposable copy-proof exercise is disabled");
        }
        if (layout == null) throw new IllegalStateException("install the copy proof first");
        UnlitCopyProofState.CommitOutcome outcome = state.commit(DEFAULT_PATTERN);
        if (outcome.status() == CommitStatus.LOCKED) {
            throw new IllegalStateException("a different pattern is already committed");
        }
        projectCommitted(outcome.snapshot());
        commitCampaignEvent(outcome.snapshot());
        sender.sendMessage("UNLIT_COPY_PROOF_EXERCISE PASS status="
                + outcome.status().name().toLowerCase(Locale.ROOT)
                + " pattern_sha256=" + outcome.snapshot().patternSha256());
        sender.sendMessage(auditSummary());
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("coordinates must be integers"); }
    }

    private static String safeWorldName(String value) {
        String name = value == null ? "" : value.trim();
        if (!name.matches("[A-Za-z0-9_.-]{1,64}")) throw new IllegalArgumentException("invalid world name");
        return name;
    }

    private static Layout loadLayout(Path path) throws IOException {
        if (!Files.exists(path)) return null;
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 3 || !LAYOUT_VERSION.equals(lines.get(0))) {
            throw new IOException("invalid Unlit copy proof layout");
        }
        Anchor surface = Anchor.parse(lines.get(1));
        Anchor unlit = Anchor.parse(lines.get(2));
        if (surface.world().equals(unlit.world())) throw new IOException("surface and Unlit worlds must differ");
        return new Layout(surface, unlit);
    }

    private static void saveLayout(Path path, Layout layout) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), "unlit-copy-layout-", ".tmp");
        try {
            Files.writeString(temp, LAYOUT_VERSION + "\n" + layout.surface().wire() + "\n"
                    + layout.unlit().wire() + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("filesystem does not support atomic Unlit layout commit", exception);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void loadLayoutWorlds(Layout layout) {
        for (String worldName : List.of(layout.surface().world(), layout.unlit().world())) {
            if (Bukkit.getWorld(worldName) == null) {
                World world = WorldCreator.name(worldName).type(WorldType.FLAT)
                        .generateStructures(false).seed(9137L).createWorld();
                if (world == null) throw new IllegalStateException("could not load layout world " + worldName);
            }
        }
    }

    private final class PlayerCommand implements BasicCommand {
        @Override public void execute(CommandSourceStack source, String[] args) {
            CommandSender sender = source.getSender();
            if (!(sender instanceof Player player)) {
                sender.sendMessage("/obscopy is a player accessibility/recovery surface.");
                return;
            }
            String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
            try {
                switch (action) {
                    case "help" -> player.sendMessage(Component.text(
                            "At the pattern register: /obscopy set <1-6> <water|heat|watch|record>, /obscopy confirm, /obscopy reset, or /obscopy status."));
                    case "status" -> player.sendMessage(Component.text(auditSummary()));
                    case "set" -> {
                        if (args.length != 3) throw new IllegalArgumentException(
                                "usage: /obscopy set <1-6> <water|heat|watch|record>");
                        set(player, integer(args[1]), Token.parse(args[2]));
                    }
                    case "confirm" -> confirm(player);
                    case "reset" -> reset(player);
                    default -> throw new IllegalArgumentException("unknown action; use /obscopy help");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
                player.sendMessage(Component.text(safe(failure.getMessage()), NamedTextColor.RED));
            }
        }

        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
            if (args.length <= 1) return List.of("help", "status", "set", "confirm", "reset");
            if (args.length == 2 && "set".equalsIgnoreCase(args[0])) return List.of("1", "2", "3", "4", "5", "6");
            if (args.length == 3 && "set".equalsIgnoreCase(args[0])) return List.of("water", "heat", "watch", "record");
            return List.of();
        }

        @Override public boolean canUse(CommandSender sender) {
            return sender.hasPermission("observance.unlit.copy");
        }
    }

    private final class AdminCommand implements BasicCommand {
        @Override public void execute(CommandSourceStack source, String[] args) {
            CommandSender sender = source.getSender();
            String action = args.length == 0 ? "audit" : args[0].toLowerCase(Locale.ROOT);
            try {
                switch (action) {
                    case "install" -> install(sender, args);
                    case "audit" -> sender.sendMessage(auditSummary());
                    case "exercise-default" -> exerciseDefault(sender);
                    default -> sender.sendMessage("Usage: /obscopyproof <install|audit|exercise-default>");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
                sender.sendMessage("UNLIT_COPY_PROOF_" + action.toUpperCase(Locale.ROOT)
                        + " BLOCKED " + safe(failure.getMessage()));
            }
        }

        @Override public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return args.length <= 1 ? List.of("audit", "install", "exercise-default") : List.of();
        }

        @Override public boolean canUse(CommandSender sender) {
            return sender.hasPermission("observance.admin");
        }
    }

    private enum PanelKind { SURFACE, UNLIT }
    private record Panel(PanelKind kind, Anchor anchor) { }
    private record Layout(Anchor surface, Anchor unlit) { }
    private record Anchor(String world, int x, int y, int z) {
        private Anchor {
            world = safeWorldName(world);
        }
        String wire() { return world + ',' + x + ',' + y + ',' + z; }
        static Anchor parse(String value) {
            String[] fields = value.split(",", -1);
            if (fields.length != 4) throw new IllegalArgumentException("invalid layout anchor");
            return new Anchor(fields[0], integer(fields[1]), integer(fields[2]), integer(fields[3]));
        }
    }

    private static Map<Token, Material> tokenMaterials() {
        Map<Token, Material> result = new EnumMap<>(Token.class);
        result.put(Token.WATER, Material.PRISMARINE);
        result.put(Token.HEAT, Material.CUT_COPPER);
        result.put(Token.WATCH, Material.LODESTONE);
        result.put(Token.RECORD, Material.CHISELED_STONE_BRICKS);
        return Map.copyOf(result);
    }

    private static Map<Material, Token> materialTokens() {
        Map<Material, Token> result = new java.util.EnumMap<>(Material.class);
        TOKEN_MATERIALS.forEach((token, material) -> result.put(material, token));
        return Map.copyOf(result);
    }

    private static Set<Material> managedMaterials() {
        Set<Material> result = new HashSet<>(TOKEN_MATERIALS.values());
        result.addAll(Set.of(Material.TINTED_GLASS, Material.POLISHED_DEEPSLATE,
                Material.CUT_COPPER, Material.LEVER, Material.LECTERN, Material.AIR));
        return Set.copyOf(result);
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[^A-Za-z0-9_.:/=;,()' -]", "_");
    }

    @Override public void close() {
        // No repeating task or external lease. Bukkit unregisters listeners with the owning plugin.
    }
}
