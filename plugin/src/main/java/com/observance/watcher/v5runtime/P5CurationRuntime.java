package com.observance.watcher.v5runtime;

import com.observance.watcher.ObservancePlugin;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/** Main-campaign P5 civic records counter: a physical consequence, never an answer form. */
public final class P5CurationRuntime implements Listener, AutoCloseable {
    public static final String SERVICE_SELECTED = "p5.service_cards_public_selected";
    public static final String PENALTY_SELECTED = "p5.penalty_copies_custody_selected";
    public static final String CHRONOLOGY_EVENT = "p5.service_chronology_shared";
    public static final String CURATION_EVENT = "p5.civic_gallery_recurated";
    private static final String PREREQUISITE = "v5_case_c02_complete";
    private static final int LOCAL_X = 18;
    private static final int LOCAL_Y = -68;
    private static final int LOCAL_Z = 280;

    private final ObservancePlugin plugin;
    private final V5ProgressStore progress;
    private final Runnable onChanged;
    private boolean started;

    public P5CurationRuntime(ObservancePlugin plugin, V5ProgressStore progress, Runnable onChanged) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public void start() {
        if (started) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        started = true;
        project();
    }

    public void project() {
        Location anchor = anchor();
        if (anchor == null) return;
        ProgressSnapshot snapshot = progress.snapshot();
        boolean service = snapshot.isComplete(SERVICE_SELECTED);
        boolean penalty = snapshot.isComplete(PENALTY_SELECTED);
        buildCounter(anchor, service, penalty);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Location anchor = anchor();
        if (anchor == null) return;
        Block clicked = event.getClickedBlock();
        Choice choice = choiceAt(anchor, clicked);
        if (choice == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!progress.snapshot().isComplete(PREREQUISITE)) {
            player.sendActionBar(Component.text("The civic archive is still sealed. Nothing changed."));
            return;
        }
        try {
            SelectionResult result = select(progress, choice);
            if (result == SelectionResult.WRONG) {
                if (choice == Choice.SERVICE_SEALED) {
                    player.sendActionBar(Component.text(
                            "Sealing the work cards repeats the later system: households lose the rules used on them. Nothing changed."));
                } else {
                    player.sendActionBar(Component.text(
                            "Publishing penalty copies as valid findings repeats the accusation. Preserve them as evidence instead. Nothing changed."));
                }
                return;
            }
            project();
            onChanged.run();
            ProgressSnapshot after = progress.snapshot();
            if (after.isComplete(CURATION_EVENT)) {
                player.sendActionBar(Component.text(
                        "Service cards remain public. Penalty copies are retained in evidence custody."));
            } else if (choice == Choice.SERVICE_PUBLIC) {
                player.sendActionBar(Component.text(
                        "Service cards retained for public use. The penalty copies still need a custody decision."));
            } else {
                player.sendActionBar(Component.text(
                        "Penalty copies moved to evidence custody. The service cards still need a public-use decision."));
            }
            if (result == SelectionResult.ALREADY) {
                player.sendMessage(Component.text("That curatorial action was already retained."));
            }
        } catch (IOException | RuntimeException failure) {
            player.sendActionBar(Component.text("The archive counter failed safely. Nothing was consumed."));
            plugin.getLogger().warning("P5 curation commit failed: " + safe(failure.getMessage()));
        }
    }

    public Audit audit() {
        Location anchor = anchor();
        if (anchor == null) return new Audit(false, "Hold mouth or world unavailable");
        ProgressSnapshot snapshot = progress.snapshot();
        boolean service = snapshot.isComplete(SERVICE_SELECTED);
        boolean penalty = snapshot.isComplete(PENALTY_SELECTED);
        boolean events = snapshot.isComplete(CHRONOLOGY_EVENT) && snapshot.isComplete(CURATION_EVENT);
        boolean levers = powered(at(anchor, -4, 1, 2)) == service
                && !powered(at(anchor, -2, 1, 2))
                && !powered(at(anchor, 2, 1, 2))
                && powered(at(anchor, 4, 1, 2)) == penalty;
        boolean publicRack = occupiedShelf(at(anchor, -3, 0, 0));
        boolean penaltyState = penalty
                ? at(anchor, 3, 0, 0).getType() == Material.WAXED_COPPER_GRATE
                : occupiedShelf(at(anchor, 3, 0, 0));
        boolean eventParity = events == (service && penalty);
        return new Audit(levers && publicRack && penaltyState && eventParity,
                "service=" + service + " penalty=" + penalty + " events=" + events
                        + " levers=" + levers + " public_rack=" + publicRack
                        + " penalty_state=" + penaltyState);
    }

    private void buildCounter(Location anchor, boolean service, boolean penalty) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -1; z <= 4; z++) {
                at(anchor, x, -1, z).setType(z == 4 || Math.abs(x) == 5
                        ? Material.TUFF_BRICKS : Material.POLISHED_DEEPSLATE, false);
            }
        }
        buildBay(anchor, -3, true, service);
        buildBay(anchor, 3, false, penalty);
        for (int x = -1; x <= 1; x++) {
            at(anchor, x, 0, 0).setType(Material.DEEPSLATE_BRICKS, false);
            at(anchor, x, 1, 0).setType(Material.COPPER_GRATE, false);
            at(anchor, x, 2, 0).setType(x == 0 ? Material.LANTERN : Material.POLISHED_DEEPSLATE_WALL, false);
        }
    }

    private void buildBay(Location anchor, int x, boolean publicCards, boolean selected) {
        Block shelf = at(anchor, x, 0, 0);
        if (!publicCards && selected) {
            shelf.setType(Material.WAXED_COPPER_GRATE, false);
            at(anchor, x + 1, 0, 0).setType(Material.BARREL, false);
            if (at(anchor, x + 1, 0, 0).getState() instanceof org.bukkit.block.Barrel barrel) {
                barrel.getInventory().clear();
                barrel.getInventory().setItem(0, recordBook(false));
                barrel.update(true, false);
            }
        } else {
            shelf.setType(Material.CHISELED_BOOKSHELF, false);
            if (shelf.getBlockData() instanceof Directional directional) {
                directional.setFacing(org.bukkit.block.BlockFace.SOUTH);
                shelf.setBlockData(directional, false);
            }
            if (shelf.getState() instanceof ChiseledBookshelf books) {
                books.getInventory().clear();
                books.getInventory().setItem(0, recordBook(publicCards));
                books.getInventory().setItem(1, recordBook(publicCards));
                books.update(true, false);
            }
        }
        at(anchor, x, 1, 0).setType(publicCards ? Material.WAXED_CUT_COPPER : Material.OXIDIZED_CUT_COPPER, false);
            at(anchor, x, 2, 0).setType(Material.DEEPSLATE_BRICKS, false);
        placeWallSign(at(anchor, x, 2, 1), publicCards
                ? List.of("SERVICE CARDS", "PUBLIC RACK", "", "")
                : List.of("PENALTY COPIES", "EVIDENCE DRAWER", "", ""));
        for (int optionX : List.of(x - 1, x + 1)) {
            at(anchor, optionX, 0, 2).setType(publicCards ? Material.WAXED_CUT_COPPER : Material.OXIDIZED_CUT_COPPER, false);
            at(anchor, optionX, 1, 0).setType(Material.DEEPSLATE_BRICKS, false);
        }
        if (publicCards) {
            placeWallSign(at(anchor, x - 1, 1, 1), List.of("SERVICE CARDS", "PUBLIC USE", "", ""));
            placeWallSign(at(anchor, x + 1, 1, 1), List.of("SERVICE CARDS", "SEALED FILE", "", ""));
            placeLever(at(anchor, x - 1, 1, 2), selected);
            placeLever(at(anchor, x + 1, 1, 2), false);
        } else {
            placeWallSign(at(anchor, x - 1, 1, 1), List.of("PENALTY COPIES", "PUBLIC FINDING", "", ""));
            placeWallSign(at(anchor, x + 1, 1, 1), List.of("PENALTY COPIES", "EVIDENCE ONLY", "", ""));
            placeLever(at(anchor, x - 1, 1, 2), false);
            placeLever(at(anchor, x + 1, 1, 2), selected);
        }
        at(anchor, x, 0, 3).setType(publicCards ? Material.LIGHT_BLUE_CARPET : Material.BROWN_CARPET, false);
    }

    private ItemStack recordBook(boolean publicCard) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        if (item.getItemMeta() instanceof BookMeta meta) {
            meta.setTitle(publicCard ? "Service card" : "Penalty copy");
            meta.setAuthor(publicCard ? "Civic works" : "Council clerk");
            meta.pages(List.of(Component.text(publicCard
                    ? "Keep this card where households and workers can compare the service rule with the work performed."
                    : "Retain this copy with its earlier service card. Do not replace either version.")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void placeLever(Block block, boolean powered) {
        block.setType(Material.LEVER, false);
        BlockData data = block.getBlockData();
        if (data instanceof FaceAttachable attachable) attachable.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
        if (data instanceof Directional directional) directional.setFacing(org.bukkit.block.BlockFace.SOUTH);
        if (data instanceof Powerable powerable) powerable.setPowered(powered);
        block.setBlockData(data, false);
    }

    private static void placeWallSign(Block block, List<String> lines) {
        block.setType(Material.OAK_WALL_SIGN, false);
        if (block.getBlockData() instanceof Directional directional) {
            directional.setFacing(org.bukkit.block.BlockFace.SOUTH);
            block.setBlockData(directional, false);
        }
        if (block.getState() instanceof Sign sign) {
            SignSide front = sign.getSide(Side.FRONT);
            for (int index = 0; index < 4; index++) front.line(index, Component.text(lines.get(index)));
            sign.setWaxed(true);
            sign.update(true, false);
        }
    }

    private Location anchor() {
        Location mouth = plugin.v5HoldMouth();
        if (mouth == null || mouth.getWorld() == null) return null;
        return mouth.clone().add(LOCAL_X, LOCAL_Y, LOCAL_Z).getBlock().getLocation();
    }

    private static Block at(Location anchor, int x, int y, int z) {
        return anchor.getWorld().getBlockAt(anchor.getBlockX() + x, anchor.getBlockY() + y, anchor.getBlockZ() + z);
    }

    private static boolean same(Block left, Block right) {
        return left.getWorld().equals(right.getWorld()) && left.getX() == right.getX()
                && left.getY() == right.getY() && left.getZ() == right.getZ();
    }

    private static Choice choiceAt(Location anchor, Block clicked) {
        if (same(clicked, at(anchor, -4, 1, 2))) return Choice.SERVICE_PUBLIC;
        if (same(clicked, at(anchor, -2, 1, 2))) return Choice.SERVICE_SEALED;
        if (same(clicked, at(anchor, 2, 1, 2))) return Choice.PENALTY_PUBLIC;
        if (same(clicked, at(anchor, 4, 1, 2))) return Choice.PENALTY_CUSTODY;
        return null;
    }

    private static boolean powered(Block block) {
        return block.getBlockData() instanceof Powerable powerable && powerable.isPowered();
    }

    private static boolean occupiedShelf(Block block) {
        if (!(block.getState() instanceof ChiseledBookshelf shelf)) return false;
        return shelf.getInventory().getItem(0) != null && shelf.getInventory().getItem(1) != null;
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[^A-Za-z0-9_.:/=;,' -]", "_");
    }

    @Override public void close() {
        HandlerList.unregisterAll(this);
        started = false;
    }

    /** Dependency-light state transition used by the live listener and restart/idempotency tests. */
    public static SelectionResult select(V5ProgressStore progress, Choice choice) throws IOException {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(choice, "choice");
        ProgressSnapshot before = progress.snapshot();
        if (!before.isComplete(PREREQUISITE)) return SelectionResult.NOT_READY;
        if (choice == Choice.SERVICE_SEALED || choice == Choice.PENALTY_PUBLIC) {
            return SelectionResult.WRONG;
        }
        return progress.transact(editor -> {
            boolean selected = choice == Choice.SERVICE_PUBLIC
                    ? editor.setBooleanTrue(SERVICE_SELECTED)
                    : editor.setBooleanTrue(PENALTY_SELECTED);
            boolean serviceReady = choice == Choice.SERVICE_PUBLIC || before.isComplete(SERVICE_SELECTED);
            boolean penaltyReady = choice == Choice.PENALTY_CUSTODY || before.isComplete(PENALTY_SELECTED);
            if (serviceReady && penaltyReady) {
                boolean chronology = editor.setBooleanTrue(CHRONOLOGY_EVENT);
                boolean curation = editor.setBooleanTrue(CURATION_EVENT);
                return selected || chronology || curation ? SelectionResult.COMPLETE : SelectionResult.ALREADY;
            }
            return selected ? SelectionResult.SELECTED : SelectionResult.ALREADY;
        });
    }

    public enum Choice { SERVICE_PUBLIC, SERVICE_SEALED, PENALTY_PUBLIC, PENALTY_CUSTODY }
    public enum SelectionResult { NOT_READY, WRONG, SELECTED, COMPLETE, ALREADY }

    public record Audit(boolean pass, String summary) { }
}
