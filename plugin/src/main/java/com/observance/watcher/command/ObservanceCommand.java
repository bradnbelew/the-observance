package com.observance.watcher.command;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.data.rows.AnswerAttemptReadRow;
import com.observance.watcher.data.rows.HintRow;
import com.observance.watcher.data.rows.PuzzleRow;
import com.observance.watcher.data.rows.SolveReadRow;
import com.observance.watcher.oracle.FlagGate;
import com.observance.watcher.structure.StructureTemplates;
import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code /observance} admin command. Read-only status + safe controls (reload, local sleep
 * toggle). The body is wrapped in Safety so a bad command never propagates an exception.
 */
public final class ObservanceCommand implements CommandExecutor, TabCompleter {

    private final ObservancePlugin plugin;
    private final Safety safety;
    private final Map<String, Integer> rehearsalProgress = new HashMap<>();
    private final Map<String, Integer> visitProgress = new HashMap<>();
    private final AtomicBoolean automaticHoldSyncInFlight = new AtomicBoolean(false);

    public ObservanceCommand(ObservancePlugin plugin, Safety safety) {
        this.plugin = plugin;
        this.safety = safety;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        safety.run("command.observance", () -> handle(sender, args));
        return true;
    }

    private void handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("observance.admin")) {
            sender.sendMessage("You do not have permission.");
            return;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "status" -> sendStatus(sender);
            case "audit" -> handleAudit(sender);
            case "visualaudit" -> handleVisualAudit(sender);
            case "preflight" -> handlePreflight(sender);
            case "dialogueaudit" -> handleDialogueAudit(sender);
            case "repair" -> handleRepair(sender);
            case "coverage" -> handleCoverage(sender);
            case "visit" -> handleVisit(sender, args);
            case "director" -> handleDirectorStart(sender, args);
            case "reload" -> {
                boolean ok = plugin.reloadAll();
                sender.sendMessage(ok ? "Observance: config + sites reloaded."
                        : "Observance: reload hit an error (see console/event_log).");
            }
            case "sleep" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /observance sleep <on|off>  (currently "
                            + (plugin.isLocallyAsleep() ? "on" : "off") + ")");
                    return;
                }
                String v = args[1].toLowerCase(Locale.ROOT);
                boolean on = v.equals("on") || v.equals("true") || v.equals("1");
                plugin.setLocallyAsleep(on);
                sender.sendMessage("Observance: local watcher-sleep " + (on ? "ENABLED (muted)" : "disabled"));
            }
            case "flag" -> handleFlag(sender, args);
            case "site" -> handleSite(sender, args);
            case "placeworld" -> handlePlaceWorld(sender, args);
            case "placeroom" -> handlePlaceRoom(sender, args);
            case "placeregion" -> handlePlaceRegion(sender, args);
            case "placedeep" -> handlePlaceDeep(sender, args);
            case "placelecterns" -> handlePlaceLecterns(sender, args);
            case "placehold" -> handlePlaceHold(sender, args);
            case "placelab" -> handlePlaceLab(sender, args);
            case "fullrun" -> handleFullRun(sender, args);
            case "prepworld" -> handlePrepWorld(sender, args);
            case "descentproof" -> handleDescentProof(sender, args);
            case "sidepass" -> handleSidePass(sender, args);
            case "puzzlepass" -> handlePuzzlePass(sender, args);
            case "dreadpass" -> handleDreadPass(sender, args);
            case "unlit" -> handleUnlit(sender, args);
            case "runbook" -> handleRunbook(sender, args);
            case "rehearse" -> handleRehearse(sender, args);
            case "placeprologue" -> handlePlacePrologue(sender, args);
            case "lens" -> handleLens(sender, args);
            case "wren" -> handleWren(sender, args);
            case "keeper" -> handleKeeper(sender, args);
            case "townsfolk" -> handleTownsfolk(sender, args);
            case "test" -> handleTest(sender, args);
            case "needle" -> handleNeedle(sender, args);
            case "finale" -> handleFinaleMarkers(sender);
            case "reading" -> handleReadingCarvings(sender);
            default -> sender.sendMessage("Unknown subcommand. Use: status | director <state|progress|world|lab> [spacing] | audit | visualaudit | dialogueaudit | preflight | repair | coverage | visit <next|back|list|siteId|lane> | runbook [setup|spine|side|puzzle|scare|unlit|ops] | rehearse <start|status|done|next|back|reset|list> | reload | sleep <on|off> | flag <set|clear|list> | site <todo|next|plan|launch|list|set> [siteId|lane] | unlit <site|clue|pass|audit|darken|border|buildmode|ready> | placeworld | placeroom <keeperId> | placeregion | placedeep | placelecterns | placehold <build|audit|seal|open|sync> | placelab | fullrun | prepworld | descentproof | sidepass | puzzlepass [gates] | dreadpass [stage|run] [player] | placeprologue | lens give [player] | wren <spawn|despawn|reckoning> | keeper <spawn|despawn> [node] | townsfolk <spawn|despawn> [id] | test <menu|preset> [player] | needle [player] | finale | reading");
        }
    }

    /**
     * {@code /observance flag <set|clear|list> [key] [true|false]} — the storylet-gate admin control
     * (OVERHAUL.md §6 Phase 1). Sets/clears a key in {@code arc_state.flags} via the atomic merge RPC
     * so a tester can OPEN a gated branch (e.g. {@code iss_caught}) to prove gating in-world before the
     * flag producers are all built, or read the current flags. The blocking Supabase I/O runs async;
     * the reply is scheduled back on the main thread.
     */
    private void handleFlag(CommandSender sender, String[] args) {
        var sb = plugin.supabase();
        if (sb == null) {
            sender.sendMessage("Observance: supabase unavailable.");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (op) {
            case "set", "clear" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /observance flag " + op + " <key>"
                            + (op.equals("set") ? " [true|false]" : ""));
                    return;
                }
                String key = args[2];
                // clear → false; set → true unless an explicit false/0 is given.
                boolean val = op.equals("set")
                        && !(args.length >= 4 && (args[3].equalsIgnoreCase("false") || args[3].equals("0")));
                JsonObject flags = new JsonObject();
                flags.addProperty(key, val);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    sb.mergeArcFlags(flags);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage("Observance: flag '" + key + "' = " + val + " (merged into arc_state.flags)."));
                });
            }
            case "list" -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                var r = sb.fetchArcState();
                Map<String, Object> map = (r.ok() && r.value() != null)
                        ? r.value().flagsMap() : Collections.emptyMap();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("== arc_state.flags ==");
                    if (map.isEmpty()) {
                        sender.sendMessage(" (none set)");
                    } else {
                        map.forEach((k, v) -> sender.sendMessage(" " + k + " = " + v));
                    }
                });
            });
            default -> sender.sendMessage("Usage: /observance flag <set|clear|list> [key] [true|false]");
        }
    }

    /**
     * The canonical keeper-site spine — the full set of terrain-scatterable set-pieces (WORLD-BUILD §4).
     * Each row is {@code [siteId, siteType, radius]}. This is the SINGLE source of truth shared by
     * {@link #handleSite} (survey — validates the keeperId + records an anchor) and {@link #handlePlaceWorld}
     * (placement — reads the surveyed anchor or auto-scatters). Types/radii mirror {@code sites.yml} so the
     * listeners that key off a site's type resolve a placeworld-placed site exactly as the config would. The
     * seven surface sites come first (rosetta + the six keepers), then the six deep-half payoff sites.
     */
    private static final String[][] KEEPER_SPINE = {
        // --- surface spine ---
        { "rune_rosetta",       "structure",       "6"  },   // literacy gate / IgnitionListener anchor
        { "stone_vaun",         "keeper_stone",    "8"  },
        { "stone_mara",         "keeper_stone",    "8"  },
        { "stone_sella",        "keeper_stone",    "8"  },
        { "stone_orin",         "keeper_stone",    "8"  },
        { "stone_brann",        "keeper_stone",    "8"  },
        { "stone_iss",          "keeper_stone",    "8"  },
        // --- deep-half payoff sites ---
        { "stone_of_reckoning", "structure",       "6"  },   // the digit/sign-glyph Rosetta
        { "the_cold_hearth",    "marker",          "6"  },   // Iss's false-warm dead shrine
        { "unbroken_light",     "accepting_floor", "10" },   // the Undercroft Accepting floor (climax)
        { "the_threshold",      "the_threshold",   "6"  },   // the grave that opens from the inside
        { "the_unwriting",      "seventh_shrine",  "6"  },   // the Seventh's chamber (payoff)
        { "threshold_vault",    "coop_plate",      "6"  },   // the co-op vault room
        // --- the two DEEPENING LANES (real Nether + End; approach A). Cross-dimension: these are
        //     SURVEY-ONLY (see LANE_SITE_IDS + handlePlaceWorld) — placeworld stamps them ONLY at a
        //     surveyed anchor in their target dimension, and SKIPS them (never auto-scatters into the
        //     overworld) when unsurveyed, mirroring sites.yml's "silently skip an unplaced site" rule.
        { "nether_forge",       "answer_sign",     "5"  },   // the Nether forge-pocket (the-fire-kept-me)
        { "end_seventh_shrine", "answer_sign",     "6"  },   // the End exile-shrine (the-name-i-cut-myself)
    };

    /**
     * Survey-only story fixtures that placeworld stamps after the terrain-scattered spine. These never
     * auto-scatter: a false lead or dialogue proof has to live exactly where the director surveyed it.
     */
    private static final String[] PLACEWORLD_SURVEY_FIXTURES = {
            "first_report_lectern_01",
            "bow_marker_01",
            "offering_cairn_01",
            "kept_light_home_01",
            "the_far_water",
            "school_stand",
            "markers_row",
            "cistern_7",
            "watch_floor",
            "set_apart_shelf",
            "undercroft_seal",
            "forgotten_mouth",
            "keeper_altar",
            "coop_plate",
            "vaun_hoard_chest",
            "vaun_bookshelf",
            "mara_lectern_1",
            "mara_lectern_2",
            "mara_lectern_3",
            "mara_lectern_4",
            "mara_lectern_5",
            "mara_map_marker",
            "sella_pool",
            "sella_anchor",
            "orin_marker_1",
            "orin_marker_2",
            "orin_marker_3",
            "orin_marker_4",
            "orin_marker_5",
            "orin_marker_6",
            "orin_frame_dial_1",
            "orin_frame_dial_2",
            "orin_frame_dial_3",
            "orin_frame_dial_4",
            "orin_frame_dial_5",
            "orin_frame_dial_6",
            "brann_toll_tower",
            "brann_corridor_start",
            "brann_corridor_end",
            "lampworks_stair",
            "third_lamp_stand",
            "painted_line",
            "dead_stall",
            "deep_bird_coops",
            "deep_market",
            "ration_table",
            "third_bay_breach",
            "warm_town_collapse",
            "dread_route_start",
            "dread_route_elsewhere",
            "dread_route_figure",
            "dread_route_exit"
    };

    /** The launch blocker list shared with tools/check_world_build_readiness.ps1 -Launch. */
    private static final String[] LAUNCH_REQUIRED_SITES = {
            "first_report_lectern_01",
            "rune_rosetta",
            "stone_vaun",
            "stone_mara",
            "stone_sella",
            "stone_orin",
            "stone_brann",
            "stone_iss",
            "stone_of_reckoning",
            "vaun_hoard_chest",
            "vaun_bookshelf",
            "mara_lectern_1",
            "mara_lectern_2",
            "mara_lectern_3",
            "mara_lectern_4",
            "mara_lectern_5",
            "mara_map_marker",
            "sella_pool",
            "sella_anchor",
            "orin_marker_1",
            "orin_marker_2",
            "orin_marker_3",
            "orin_marker_4",
            "orin_marker_5",
            "orin_marker_6",
            "orin_frame_dial_1",
            "orin_frame_dial_2",
            "orin_frame_dial_3",
            "orin_frame_dial_4",
            "orin_frame_dial_5",
            "orin_frame_dial_6",
            "brann_toll_tower",
            "brann_corridor_start",
            "brann_corridor_end",
            "bow_marker_01",
            "offering_cairn_01",
            "kept_light_home_01",
            "the_far_water",
            "school_stand",
            "markers_row",
            "cistern_7",
            "watch_floor",
            "set_apart_shelf",
            "undercroft_seal",
            "forgotten_mouth",
            "the_cold_hearth",
            "unbroken_light",
            "the_threshold",
            "the_unwriting",
            "keeper_altar",
            "coop_plate",
            "threshold_vault",
            "lampworks_stair",
            "third_lamp_stand",
            "painted_line",
            "dead_stall",
            "deep_bird_coops",
            "deep_market",
            "ration_table",
            "third_bay_breach",
            "warm_town_collapse",
            "dread_route_start",
            "dread_route_elsewhere",
            "dread_route_figure",
            "dread_route_exit",
            "nether_forge",
            "end_seventh_shrine"
    };

    private record PlacementLane(String id, String label, String[] sites) { }

    private static final PlacementLane[] PLACEMENT_LANES = {
            new PlacementLane("prologue", "Prologue / first literacy", new String[]{
                    "first_report_lectern_01", "rune_rosetta"
            }),
            new PlacementLane("keepers", "Six keeper evidence sites", new String[]{
                    "stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss",
                    "stone_of_reckoning",
                    "vaun_hoard_chest", "vaun_bookshelf",
                    "mara_lectern_1", "mara_lectern_2", "mara_lectern_3", "mara_lectern_4", "mara_lectern_5",
                    "mara_map_marker",
                    "sella_pool", "sella_anchor",
                    "orin_marker_1", "orin_marker_2", "orin_marker_3", "orin_marker_4", "orin_marker_5", "orin_marker_6",
                    "orin_frame_dial_1", "orin_frame_dial_2", "orin_frame_dial_3", "orin_frame_dial_4", "orin_frame_dial_5", "orin_frame_dial_6",
                    "brann_toll_tower", "brann_corridor_start", "brann_corridor_end"
            }),
            new PlacementLane("customs", "Body customs and home proof", new String[]{
                    "bow_marker_01", "offering_cairn_01", "kept_light_home_01"
            }),
            new PlacementLane("human", "Human-history side proof web", new String[]{
                    "the_far_water", "school_stand", "markers_row", "cistern_7", "watch_floor",
                    "set_apart_shelf", "undercroft_seal", "forgotten_mouth"
            }),
            new PlacementLane("deep", "Deep route, market, and finale", new String[]{
                    "the_cold_hearth", "unbroken_light", "the_threshold", "the_unwriting", "keeper_altar",
                    "coop_plate", "threshold_vault", "lampworks_stair", "third_lamp_stand", "painted_line",
                    "dead_stall", "deep_bird_coops", "deep_market", "ration_table", "third_bay_breach",
                    "warm_town_collapse"
            }),
            new PlacementLane("dread", "Watcher dread route", new String[]{
                    "dread_route_start", "dread_route_elsewhere", "dread_route_figure", "dread_route_exit"
            }),
            new PlacementLane("dimensions", "Optional Nether / End deepening lanes", new String[]{
                    "nether_forge", "end_seventh_shrine"
            })
    };

    /**
     * The cross-dimension DEEPENING-LANE site ids — the two lanes that live in the real Nether/End rather
     * than the overworld the surface/deep spines scatter across. These are SURVEY-ONLY in {@code placeworld}:
     * they are stamped ONLY at a surveyed anchor whose world matches the dimension the operator is standing
     * in, and are SKIPPED (never auto-scattered) otherwise — so running {@code placeworld} in the overworld
     * never drops a Nether/End set-piece into the overworld. FLOW: stand in the Nether/End, run
     * {@code /observance site set <id>}, then {@code /observance placeworld} FROM that dimension.
     */
    private static boolean isLaneSite(String siteId) {
        return "nether_forge".equals(siteId) || "end_seventh_shrine".equals(siteId);
    }

    private static String dimensionLanePlacementFlag(String siteId) {
        return switch (siteId) {
            case "nether_forge" -> "nether_forge_placed";
            case "end_seventh_shrine" -> "end_seventh_shrine_placed";
            default -> null;
        };
    }

    private static boolean isDeepHoldLaunchSite(String siteId) {
        if (siteId == null) return false;
        for (HoldSite row : DEEP_HOLD_SITES) {
            if (row.id().equals(siteId)) return true;
        }
        return false;
    }

    private static HoldSite deepHoldSiteById(String siteId) {
        if (siteId == null) return null;
        for (HoldSite row : DEEP_HOLD_SITES) {
            if (row.id().equals(siteId)) return row;
        }
        return null;
    }

    private boolean isDeepHoldBuilt() {
        if (plugin.sites() == null) return false;
        Site region = plugin.sites().get(HOLD_REGION_SITE_ID);
        return region != null && region.enabled() && region.isPlaced() && region.location() != null;
    }

    private static boolean isPlaceWorldSurveyFixture(String siteId) {
        if (siteId == null) return false;
        for (String fixture : PLACEWORLD_SURVEY_FIXTURES) {
            if (fixture.equals(siteId)) return true;
        }
        return false;
    }

    private static final String[][] PUZZLE_PASS_SITES = {
            {"bow_marker_01", "bow_marker", "5", "4"},
            {"offering_cairn_01", "offering_cairn", "5", "4"},
            {"answer_sign_01", "answer_sign", "5", "4"},
            {"vaun_hoard_chest", "vaun_hoard_chest", "3", "3"},
            {"vaun_bookshelf", "vaun_bookshelf", "3", "3"},
            {"mara_lectern_1", "mara_lectern", "2", "2"},
            {"mara_lectern_2", "mara_lectern", "2", "2"},
            {"mara_lectern_3", "mara_lectern", "2", "2"},
            {"mara_lectern_4", "mara_lectern", "2", "2"},
            {"mara_lectern_5", "mara_lectern", "2", "2"},
            {"mara_map_marker", "mara_map_marker", "5", "4"},
            {"sella_pool", "sella_pool", "5", "4"},
            {"sella_anchor", "sella_anchor", "5", "4"},
            {"orin_marker_1", "orin_marker", "4", "4"},
            {"orin_marker_2", "orin_marker", "4", "4"},
            {"orin_marker_3", "orin_marker", "4", "4"},
            {"orin_marker_4", "orin_marker", "4", "4"},
            {"orin_marker_5", "orin_marker", "4", "4"},
            {"orin_marker_6", "orin_marker", "4", "4"},
            {"orin_frame_dial_1", "orin_frame_dial", "3", "3"},
            {"orin_frame_dial_2", "orin_frame_dial", "3", "3"},
            {"orin_frame_dial_3", "orin_frame_dial", "3", "3"},
            {"orin_frame_dial_4", "orin_frame_dial", "3", "3"},
            {"orin_frame_dial_5", "orin_frame_dial", "3", "3"},
            {"orin_frame_dial_6", "orin_frame_dial", "3", "3"},
            {"brann_toll_tower", "brann_toll_tower", "5", "6"},
            {"brann_corridor_start", "brann_corridor_start", "4", "4"},
            {"brann_corridor_end", "brann_corridor_end", "4", "4"},
            {"coop_plate", "coop_plate", "5", "4"},
            {"threshold_vault", "coop_plate", "6", "6"},
    };

    private static final String[][] DREAD_PASS_SITES = {
            {"dread_route_start", "dread_route"},
            {"dread_route_elsewhere", "dread_route"},
            {"dread_route_figure", "dread_route"},
            {"dread_route_exit", "dread_route"},
    };

    private record HoldSite(String id, String type, int radius, int vertical,
                            int x, int y, int z, int halfX, int halfZ) { }

    private record HoldGate(String id, int x, int y, int z, boolean openInitially, String label) { }

    private record HoldBuildAnchor(Location base, Location surfaceMouth) { }

    private record HoldGateSpan(boolean acrossX, int halfAcross, int height, int depth, int doorHalf) { }

    private record HoldRecordStation(String id, int x, int y, int z, BlockFace facing,
                                     String title, String requiredFragment,
                                     int signX, int signY, int signZ) { }

    private static final String HOLD_REGION_SITE_ID = "deep_hold_region";
    private static final String HOLD_ENTRY_REGION_SITE_ID = "deep_hold_entry_stair";

    /**
     * Production Deep Hold layout. Nether/End lanes intentionally remain out-of-hold; those must still be
     * surveyed and placed in their real dimensions.
     */
    private static final HoldSite[] DEEP_HOLD_SITES = {
            new HoldSite("undercroft_seal", "undercroft_seal", 11, 7, -34, 0, -126, 0, 0),
            new HoldSite("forgotten_mouth", "forgotten_mouth", 11, 7, 34, 0, -126, 0, 0),
            new HoldSite("rune_rosetta", "structure", 8, 7, 0, 0, -108, 15, 12),
            new HoldSite("bow_marker_01", "bow_marker", 4, 4, -24, 0, -86, 0, 0),
            new HoldSite("offering_cairn_01", "offering_cairn", 4, 4, 0, 0, -86, 0, 0),
            new HoldSite("kept_light_home_01", "kept_light", 5, 4, 24, 0, -86, 0, 0),

            new HoldSite("stone_vaun", "keeper_stone", 8, 7, -82, 0, -42, 0, 0),
            new HoldSite("vaun_hoard_chest", "vaun_hoard_chest", 2, 3, -94, 0, -34, 0, 0),
            new HoldSite("vaun_bookshelf", "vaun_bookshelf", 2, 3, -94, 0, -50, 0, 0),
            new HoldSite("stone_mara", "keeper_stone", 8, 7, 82, 0, -42, 0, 0),
            new HoldSite("mara_lectern_1", "mara_lectern", 2, 2, 70, 0, -29, 0, 0),
            new HoldSite("mara_lectern_2", "mara_lectern", 2, 2, 76, 0, -29, 0, 0),
            new HoldSite("mara_lectern_3", "mara_lectern", 2, 2, 82, 0, -29, 0, 0),
            new HoldSite("mara_lectern_4", "mara_lectern", 2, 2, 88, 0, -29, 0, 0),
            new HoldSite("mara_lectern_5", "mara_lectern", 2, 2, 94, 0, -29, 0, 0),
            new HoldSite("mara_route_marker_1", "mara_route_marker", 2, 3, 70, 0, -36, 0, 0),
            new HoldSite("mara_route_marker_2", "mara_route_marker", 2, 3, 78, 0, -49, 0, 0),
            new HoldSite("mara_route_marker_3", "mara_route_marker", 2, 3, 90, 0, -54, 0, 0),
            new HoldSite("mara_route_marker_4", "mara_route_marker", 2, 3, 100, 0, -50, 0, 0),
            new HoldSite("mara_map_marker", "mara_map_marker", 4, 4, 102, 0, -44, 0, 0),
            new HoldSite("stone_sella", "keeper_stone", 8, 7, 82, 0, 0, 0, 0),
            new HoldSite("sella_pool", "sella_pool", 5, 4, 94, 0, 0, 0, 0),
            new HoldSite("sella_anchor", "sella_anchor", 3, 5, 94, 4, 8, 0, 0),
            new HoldSite("sella_lectern_1", "sella_lectern", 2, 2, 66, 0, 12, 0, 0),
            new HoldSite("sella_lectern_2", "sella_lectern", 2, 2, 72, 0, 12, 0, 0),
            new HoldSite("sella_lectern_3", "sella_lectern", 2, 2, 78, 0, 12, 0, 0),
            new HoldSite("sella_lectern_4", "sella_lectern", 2, 2, 84, 0, 12, 0, 0),
            new HoldSite("sella_lectern_5", "sella_lectern", 2, 2, 90, 0, 12, 0, 0),

            new HoldSite("stone_orin", "keeper_stone", 8, 7, 82, 0, 42, 0, 0),
            new HoldSite("orin_marker_1", "orin_marker", 3, 4, 72, 0, 28, 0, 0),
            new HoldSite("orin_marker_2", "orin_marker", 3, 4, 82, 0, 28, 0, 0),
            new HoldSite("orin_marker_3", "orin_marker", 3, 4, 92, 0, 28, 0, 0),
            new HoldSite("orin_marker_4", "orin_marker", 3, 4, 72, 0, 35, 0, 0),
            new HoldSite("orin_marker_5", "orin_marker", 3, 4, 82, 0, 35, 0, 0),
            new HoldSite("orin_marker_6", "orin_marker", 3, 4, 92, 0, 35, 0, 0),
            new HoldSite("orin_frame_dial_1", "orin_frame_dial", 2, 3, 67, 0, 53, 0, 0),
            new HoldSite("orin_frame_dial_2", "orin_frame_dial", 2, 3, 73, 0, 53, 0, 0),
            new HoldSite("orin_frame_dial_3", "orin_frame_dial", 2, 3, 79, 0, 53, 0, 0),
            new HoldSite("orin_frame_dial_4", "orin_frame_dial", 2, 3, 85, 0, 53, 0, 0),
            new HoldSite("orin_frame_dial_5", "orin_frame_dial", 2, 3, 91, 0, 53, 0, 0),
            new HoldSite("orin_frame_dial_6", "orin_frame_dial", 2, 3, 97, 0, 53, 0, 0),
            new HoldSite("stone_brann", "keeper_stone", 8, 7, -82, 0, 42, 0, 0),
            new HoldSite("brann_toll_tower", "brann_toll_tower", 5, 7, -96, 0, 34, 0, 0),
            new HoldSite("brann_corridor_start", "brann_corridor_start", 3, 4, -100, 0, 55, 0, 0),
            new HoldSite("brann_corridor_end", "brann_corridor_end", 3, 4, -64, 0, 55, 0, 0),
            new HoldSite("stone_iss", "keeper_stone", 8, 7, -82, 0, 0, 0, 0),
            new HoldSite("the_cold_hearth", "marker", 8, 7, -112, 0, 0, 16, 13),
            new HoldSite("case_board", "case_board", 8, 6, 0, 0, 24, 0, 0),
            new HoldSite("prior_camp", "prior_camp", 28, 12, 0, 0, 112, 26, 18),

            new HoldSite("school_stand", "school_stand", 14, 8, -140, 0, 98, 0, 0),
            new HoldSite("markers_row", "markers_row", 15, 8, -95, 0, 130, 0, 0),
            new HoldSite("cistern_7", "cistern_7", 15, 9, -145, 0, 160, 0, 0),
            new HoldSite("watch_floor", "watch_floor", 14, 9, -92, 0, 192, 0, 0),
            new HoldSite("set_apart_shelf", "set_apart_shelf", 14, 8, -145, 0, 218, 0, 0),
            new HoldSite("the_far_water", "far_water", 18, 9, -88, 0, 238, 0, 0),
            new HoldSite("deep_market", "deep_market", 20, 10, 140, 0, 100, 0, 0),
            new HoldSite("ration_table", "ration_table", 13, 8, 90, 0, 145, 0, 0),
            new HoldSite("third_bay_breach", "third_bay_breach", 16, 9, 145, 0, 172, 0, 0),
            new HoldSite("warm_town_collapse", "warm_town_collapse", 18, 10, 120, 0, 215, 0, 0),

            new HoldSite("lampworks_stair", "lampworks_stair", 20, 16, 0, -5, 265, 0, 0),
            new HoldSite("third_lamp_stand", "lamp_stand", 4, 5, -14, -12, 286, 0, 0),
            new HoldSite("painted_line", "painted_line", 5, 5, 0, -19, 308, 0, 0),
            new HoldSite("dead_stall", "dead_stall", 10, 7, 150, 0, 238, 16, 12),
            new HoldSite("deep_bird_coops", "bird_coops", 12, 7, 70, 0, 238, 18, 14),
            new HoldSite("stone_of_reckoning", "structure", 10, 9, -72, -28, 365, 20, 16),
            new HoldSite("the_threshold", "the_threshold", 10, 9, 72, -28, 365, 20, 16),
            new HoldSite("threshold_vault", "coop_plate", 9, 8, 72, -28, 420, 17, 14),
            new HoldSite("failed_accepting", "failed_accepting", 20, 10, 0, -28, 420, 27, 19),
            new HoldSite("unbroken_light", "accepting_floor", 24, 12, 0, -28, 515, 33, 25),
            new HoldSite("keeper_altar", "keeper_altar", 10, 8, -65, -28, 540, 16, 13),
            new HoldSite("coop_plate", "coop_plate", 9, 7, 65, -28, 540, 16, 13),
            new HoldSite("the_unwriting", "seventh_shrine", 18, 10, 0, -28, 625, 29, 21),

            new HoldSite("dread_route_start", "dread_route", 5, 5, 135, -28, 360, 0, 0),
            new HoldSite("dread_route_elsewhere", "dread_route", 5, 5, 170, -28, 400, 0, 0),
            new HoldSite("dread_route_figure", "dread_route", 5, 5, 160, -28, 440, 0, 0),
            new HoldSite("dread_route_exit", "dread_route", 5, 5, 128, -28, 468, 0, 0),
    };

    private static final HoldGate[] DEEP_HOLD_GATES = {
            new HoldGate("entry", 0, 0, -160, true, "old mouth"),
            new HoldGate("keeper", 0, 0, -75, true, "six-hand court"),
            new HoldGate("archive", 0, 0, 72, false, "split archives"),
            new HoldGate("prior", 0, 0, 88, false, "failed camp"),
            new HoldGate("deep", 0, 0, 250, false, "lampworks"),
            new HoldGate("dread", 120, -28, 360, false, "side hush"),
            new HoldGate("threshold", 0, -28, 330, false, "lower reckoning"),
            new HoldGate("accepting", 0, -28, 470, false, "last warm"),
    };

    private static final HoldRecordStation[] DEEP_HOLD_RECORD_STATIONS = {
            new HoldRecordStation("mouth_register", 12, 0, -142, BlockFace.WEST,
                    "mouth register", "One row stayed blank", 16, 0, -142),
            new HoldRecordStation("court_census", -22, 0, -16, BlockFace.EAST,
                    "court census", "trust the physical room", -26, 0, -16),
            new HoldRecordStation("intake_rail", -48, 0, 82, BlockFace.SOUTH,
                    "intake rail", "evidence, not decoration", -44, 0, 81),
            new HoldRecordStation("prior_roster", 10, 0, 82, BlockFace.WEST,
                    "prior roster", "no witness", 14, 0, 82),
            new HoldRecordStation("closure_docket", 48, 0, 82, BlockFace.SOUTH,
                    "closure docket", "before the collapse", 44, 0, 81),
            new HoldRecordStation("lamp_count", -12, -3, 260, BlockFace.EAST,
                    "lamp count", "Do not break the wall", -16, -3, 260),
            new HoldRecordStation("threshold_hands", -18, -28, 348, BlockFace.EAST,
                    "threshold hands", "Plate. Name. Word.", -22, -28, 348),
            new HoldRecordStation("side_hush", 110, -28, 350, BlockFace.WEST,
                    "side hush", "uncrossed word", 114, -28, 350),
    };

    private static final Set<String> DEEP_HOLD_LORE_SEEDS = Set.of(
            "the_far_water", "school_stand", "markers_row", "cistern_7", "watch_floor",
            "set_apart_shelf", "undercroft_seal", "forgotten_mouth", "deep_market",
            "ration_table", "third_bay_breach", "warm_town_collapse", "dead_stall",
            "deep_bird_coops"
    );

    /** Look up a keeper row by its canonical siteId (case-insensitive; accepts the bare form too). */
    private static String[] keeperRow(String rawId) {
        if (rawId == null) return null;
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        for (String[] row : KEEPER_SPINE) {
            if (row[0].equals(id)) return row;
            // Accept the bare form (vaun == stone_vaun, threshold == the_threshold, reckoning == stone_of_reckoning).
            String bare = row[0]
                    .replaceFirst("^stone_of_", "")
                    .replaceFirst("^stone_", "")
                    .replaceFirst("^rune_", "")
                    .replaceFirst("^the_", "");
            if (bare.equals(id)) return row;
        }
        return null;
    }

    /** The valid-keeper-id hint line, e.g. for a bad {@code site set} argument. */
    private static String keeperIdList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < KEEPER_SPINE.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(KEEPER_SPINE[i][0]);
        }
        return sb.toString();
    }

    private List<String> siteIdSuggestions(String prefix) {
        String want = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> ids = new ArrayList<>();
        for (String[] row : KEEPER_SPINE) {
            if (row[0].startsWith(want) && !ids.contains(row[0])) ids.add(row[0]);
        }
        if (plugin.sites() != null) {
            for (Site site : plugin.sites().all()) {
                if (site.id().startsWith(want) && !ids.contains(site.id())) ids.add(site.id());
            }
        }
        Collections.sort(ids);
        return ids;
    }

    /**
     * {@code /observance site set <siteId>} — SURVEY. Records the sender's CURRENT location (the block
     * they are standing on) as {@code siteId}'s site anchor and persists it to {@code sites.yml} via
     * {@link ObservancePlugin#registerRuntimeSite} (idempotent — re-surveying overwrites). This lets the
     * operator walk to a good, hidden, terrain-fitting spot pre-session and mark it, so {@link
     * #handlePlaceWorld} later stamps the set-piece exactly there instead of in a visible cluster.
     *
     * <p>Keeper-spine ids use the canonical {@code type}/{@code radius} from {@link #KEEPER_SPINE}. Every
     * other id must already exist in {@code sites.yml}; the survey preserves its configured type, radius,
     * vertical radius, protection, enabled state, puzzle key, and beacon flag.
     */
    private void handleSite(CommandSender sender, String[] args) {
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (op.equals("todo") || op.equals("launch") || op.equals("list")) {
            handleSiteTodo(sender);
            return;
        }
        if (op.equals("next")) {
            handleSiteNext(sender, args);
            return;
        }
        if (op.equals("plan")) {
            handleSitePlan(sender, args);
            return;
        }
        if (!op.equals("set")) {
            sender.sendMessage("Usage: /observance site <todo|next|plan|launch|list|set> [siteId|lane]   (keeper spine: " + keeperIdList() + ")");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance site set must be run by a player (needs a location).");
            return;
        }
        if (args.length < 3 || args[2].isBlank()) {
            sender.sendMessage("Usage: /observance site set <siteId>   (try tab-complete; keeper spine: " + keeperIdList() + ")");
            return;
        }
        String requestedId = args[2].trim().toLowerCase(Locale.ROOT);
        String[] row = keeperRow(requestedId);
        Site existing = plugin.sites() != null ? plugin.sites().get(requestedId) : null;
        if (row == null && existing == null) {
            sender.sendMessage("Observance: unknown site '" + requestedId
                    + "'. Use tab-complete, or add the id to sites.yml first.");
            return;
        }
        Location loc = player.getLocation();
        if (loc == null || loc.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        String siteId = row != null ? row[0] : existing.id();
        String siteType = row != null ? row[1] : existing.type();
        int radius = existing != null ? existing.radius() : 8;
        if (row != null) {
            try { radius = Integer.parseInt(row[2]); } catch (NumberFormatException e) { radius = 8; }
        }
        int verticalRadius = existing != null ? existing.verticalRadius() : 6;
        boolean protect = existing == null || existing.protect();
        boolean enabled = existing == null || existing.enabled();
        String puzzleKey = existing != null ? existing.puzzleKey() : null;
        String world = loc.getWorld().getName();

        // Record the sender's block position as the survey anchor. placeworld terrain-re-seats on the
        // surface at this X/Z, so the stored Y need only be in the right column.
        Site site = new Site(siteId, siteType, world,
                (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                radius, verticalRadius, protect, enabled, puzzleKey, false);
        plugin.registerRuntimeSite(site); // also persists to sites.yml (idempotent — re-survey overwrites)

        sender.sendMessage("Observance: surveyed '" + siteId + "' -> "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " in " + world
                + " (type " + siteType + ", r" + radius + ").");
        sender.sendMessage(row != null
                ? "  Saved to sites.yml. Run /observance placeworld to stamp the large set-pieces."
                : (isPlaceWorldSurveyFixture(siteId)
                    ? "  Saved to sites.yml. Run /observance placeworld to stamp this surveyed fixture."
                    : "  Saved to sites.yml. This smaller anchor is now live."));
        sendLaunchRemaining(sender);
    }

    private void handleUnlit(CommandSender sender, String[] args) {
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "audit";
        switch (op) {
            case "site" -> {
                if (args.length < 3 || args[2].isBlank()) {
                    sender.sendMessage("Usage: /observance unlit site <entry|spawn|exit|lamp|cairn|coop|well|watch|warm|threshold|base|siteId>");
                    return;
                }
                String siteId = unlitSiteId(args[2]);
                handleSite(sender, new String[]{"site", "set", siteId});
            }
            case "audit" -> handleUnlitAudit(sender);
            case "border" -> handleUnlitBorder(sender, args);
            case "darken", "scrub" -> handleUnlitDarken(sender, args);
            case "buildmode", "build" -> handleUnlitBuildMode(sender, args);
            case "clue" -> handleUnlitClue(sender, args);
            case "pass" -> handleUnlitPass(sender, args);
            case "ready", "playtest" -> handleUnlitReady(sender);
            default -> sender.sendMessage("Usage: /observance unlit <site|clue|pass|audit|darken|border|buildmode|ready>");
        }
    }

    private void handleUnlitBuildMode(CommandSender sender, String[] args) {
        String mode = args.length > 2 ? args[2].toLowerCase(Locale.ROOT).trim() : "status";
        Boolean set = switch (mode) {
            case "on", "true", "enable", "enabled" -> Boolean.TRUE;
            case "off", "false", "disable", "disabled" -> Boolean.FALSE;
            case "status", "" -> null;
            default -> {
                sender.sendMessage("Usage: /obs unlit buildmode <on|off|status>");
                yield null;
            }
        };
        if (!mode.equals("status") && !mode.isBlank() && set == null) return;

        if (set != null) {
            plugin.getConfig().set("unlit.buildmode", set);
            plugin.saveConfig();
        }

        boolean enabled = plugin.getConfig().getBoolean("unlit.buildmode", false);
        sender.sendMessage("Observance: Unlit buildmode is " + (enabled ? "ON" : "OFF") + ".");
        sender.sendMessage(enabled
                ? "  Ops/admins can edit blocks, inventories, and containers inside the Unlit world. Turn it off before playtest."
                : "  Player expedition restrictions are live.");
    }

    private void handleUnlitClue(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance unlit clue must be run by a player inside the mirrored village.");
            return;
        }
        if (args.length < 3 || args[2].isBlank()) {
            sender.sendMessage("Usage: /observance unlit clue <lamp|cairn|coop|well|watch|warm|threshold|base>");
            return;
        }
        String siteId = unlitSiteId(args[2]);
        if (!siteId.startsWith("unlit_house_")) {
            sender.sendMessage("Observance: clue fixtures are for Unlit houses only. Use /obs unlit site for entry/spawn/exit.");
            return;
        }
        handleSite(sender, new String[]{"site", "set", siteId});
        String note = stampUnlitClue(player.getLocation(), siteId);
        sender.sendMessage("Observance: stamped " + unlitShortId(siteId) + " clue fixture. " + note);
    }

    private void handleUnlitPass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance unlit pass must be run by a player at the test rig origin.");
            return;
        }
        String variant = args.length > 2 ? args[2].toLowerCase(Locale.ROOT).trim() : "light";
        if (variant.isBlank()) variant = "light";
        Location base = player.getLocation().getBlock().getLocation();
        if (base.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your world.");
            return;
        }
        String unlitWorld = plugin.getConfig().getString("unlit.world", "observance_unlit");
        if (!base.getWorld().getName().equals(unlitWorld)) {
            sender.sendMessage("Note: this pass is best staged inside " + unlitWorld
                    + "; the Unlit runtime only pressures players in that world.");
        }

        switch (variant) {
            case "light", "dark", "damage" -> {
                buildUnlitPassLane(base, 14);
                registerUnlitSite(base, "unlit_spawn_mirror", "unlit_spawn", 5, 4);
                registerUnlitSite(base.clone().add(6, 0, 0), "unlit_safe_01", "unlit_safe", 4, 4);
                registerUnlitSite(base.clone().add(14, 0, 0), "unlit_exit", "unlit_exit", 5, 4);
                setBlock(base.clone().add(6, 0, 1), Material.SOUL_LANTERN);
                placeStandingSign(base.clone().add(1, 0, 1), BlockFace.SOUTH,
                        new String[]{"light pass", "spend lamp", "cross dark", "exit ahead"});
                sender.sendMessage("Unlit pass staged: light/dark pressure lane with one authored safe pocket.");
            }
            case "stalker", "figure" -> {
                registerUnlitSite(base, "unlit_spawn_mirror", "unlit_spawn", 5, 4);
                registerUnlitSite(base.clone().add(8, 0, 0), "unlit_exit", "unlit_exit", 5, 4);
                sender.sendMessage("Unlit pass staged without blocks/signs: enter at this spawn, stand in the dark, watch the figure stalk and vanish.");
            }
            case "extinguish", "breaklight" -> {
                registerUnlitSite(base, "unlit_spawn_mirror", "unlit_spawn", 5, 4);
                registerUnlitSite(base.clone().add(8, 0, 0), "unlit_exit", "unlit_exit", 5, 4);
                sender.sendMessage("Unlit pass staged without blocks/signs: enter here, place one borrowed lantern nearby, then stand outside safety until the figure breaks it.");
            }
            case "house", "clue" -> {
                Location lamp = base.clone();
                Location coop = base.clone().add(7, 0, 0);
                Location threshold = base.clone().add(14, 0, 0);
                registerUnlitSite(lamp, "unlit_house_lamp", "unlit_house", 6, 5);
                registerUnlitSite(coop, "unlit_house_coop", "unlit_house", 6, 5);
                registerUnlitSite(threshold, "unlit_house_threshold", "unlit_house", 6, 5);
                stampUnlitClue(lamp, "unlit_house_lamp");
                stampUnlitClue(coop, "unlit_house_coop");
                stampUnlitClue(threshold, "unlit_house_threshold");
                sender.sendMessage("Unlit pass staged: three different house clue fixtures, all non-linear.");
            }
            case "extract", "retreat" -> {
                buildUnlitPassLane(base, 10);
                registerUnlitSite(base, "unlit_spawn_mirror", "unlit_spawn", 5, 4);
                registerUnlitSite(base.clone().add(10, 0, 0), "unlit_exit", "unlit_exit", 5, 4);
                setBlock(base.clone().add(10, 0, 0), Material.REINFORCED_DEEPSLATE);
                placeStandingSign(base.clone().add(9, 0, 1), BlockFace.SOUTH,
                        new String[]{"exit pass", "walk here", "or use shard", "inventory back"});
                sender.sendMessage("Unlit pass staged: retreat/extraction and inventory-restore proof.");
            }
            default -> sender.sendMessage("Usage: /obs unlit pass <light|stalker|extinguish|house|extract>");
        }
    }

    private void handleUnlitAudit(CommandSender sender) {
        sender.sendMessage("== Unlit village readiness ==");
        String[] required = unlitRequiredSites();
        int placed = 0;
        int proven = 0;
        for (String id : required) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(id);
            Location loc = site == null ? null : site.location();
            if (site != null && site.isPlaced() && loc != null) {
                placed++;
                String issue = unlitFixtureIssue(id, loc);
                if (issue == null) {
                    proven++;
                    sender.sendMessage(" OK   " + id + " @ " + site.worldName());
                } else {
                    sender.sendMessage(" WARN " + id + " @ " + site.worldName() + " - " + issue);
                }
            } else if (site != null) {
                sender.sendMessage(" TODO " + id + " (" + site.type() + ") - run /obs unlit site " + unlitShortId(id));
            } else {
                sender.sendMessage(" MISS " + id + " - add placeholder to sites.yml");
            }
        }
        boolean enabled = plugin.getConfig().getBoolean("unlit.enabled", true);
        boolean buildmode = plugin.getConfig().getBoolean("unlit.buildmode", false);
        String world = plugin.getConfig().getString("unlit.world", "observance_unlit");
        org.bukkit.World loaded = Bukkit.getWorld(world);
        sender.sendMessage(" config enabled: " + enabled + "; buildmode: " + (buildmode ? "ON" : "OFF")
                + "; world: " + world + " (" + (loaded == null ? "not loaded" : "loaded") + ")");
        String borderIssue = unlitBorderIssue(loaded);
        sender.sendMessage(borderIssue == null
                ? " border: OK"
                : " border: TODO - " + borderIssue);
        String strayLightIssue = unlitStrayLightIssue(loaded);
        sender.sendMessage(strayLightIssue == null
                ? " stray light: OK"
                : " stray light: WARN - " + strayLightIssue);
        sender.sendMessage(" placed: " + placed + "/" + required.length
                + "; fixture proof: " + proven + "/" + required.length
                + ". House order is intentionally non-linear.");
    }

    private void handleUnlitDarken(CommandSender sender, String[] args) {
        int radius = 10;
        boolean fullBorder = args.length >= 3
                && (args[2].equalsIgnoreCase("all") || args[2].equalsIgnoreCase("border"));
        int radiusArg = fullBorder ? 3 : 2;
        if (args.length >= 3) {
            try {
                if (fullBorder) {
                    radius = plugin.getConfig().getInt("unlit.border-radius", 96);
                    if (args.length >= 4) radius = Integer.parseInt(args[3].trim());
                    radius = Math.max(16, Math.min(256, radius));
                } else if (args.length > radiusArg) {
                    radius = Math.max(4, Math.min(32, Integer.parseInt(args[2].trim())));
                }
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Observance: darken radius must be a number.");
                return;
            }
        }

        String configuredWorld = plugin.getConfig().getString("unlit.world", "observance_unlit");
        World world = Bukkit.getWorld(configuredWorld);
        if (world == null) {
            sender.sendMessage("Observance: load " + configuredWorld + " before running /obs unlit darken.");
            return;
        }

        Set<String> touched = new HashSet<>();
        int changed = 0;
        int anchors = 0;
        if (fullBorder) {
            Site spawn = plugin.sites() == null ? null : plugin.sites().get("unlit_spawn_mirror");
            Location center = spawn == null ? null : spawn.location();
            if (center == null || center.getWorld() == null || !center.getWorld().getName().equals(world.getName())) {
                sender.sendMessage("Observance: place unlit_spawn_mirror in " + configuredWorld
                        + " before running /obs unlit darken all.");
                return;
            }
            changed = darkenUnlitStrayLightsInBorder(center, radius, touched);
            sender.sendMessage("Observance: Unlit full darken scrub checked the border area at radius " + radius
                    + " and removed/dimmed " + changed + " unauthorized light source(s).");
        } else {
            for (String id : unlitRequiredSites()) {
                Site site = plugin.sites() == null ? null : plugin.sites().get(id);
                Location loc = site == null ? null : site.location();
                if (site == null || !site.isPlaced() || loc == null || loc.getWorld() == null) continue;
                if (!loc.getWorld().getName().equals(world.getName())) continue;
                anchors++;
                changed += darkenUnlitStrayLightsNear(loc, radius, touched);
            }
            sender.sendMessage("Observance: Unlit anchor darken scrub checked " + anchors
                    + " anchors at radius " + radius + " and removed/dimmed " + changed
                    + " unauthorized light source(s).");
        }
        sender.sendMessage("  Run /obs unlit audit next; authored safe zones inside unlit_safe sites are left alone.");
    }

    private void handleUnlitBorder(CommandSender sender, String[] args) {
        int radius = plugin.getConfig().getInt("unlit.border-radius", 96);
        if (args.length >= 3) {
            try {
                radius = Math.max(16, Math.min(512, Integer.parseInt(args[2].trim())));
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Observance: border radius must be a number.");
                return;
            }
        }

        Location center = null;
        Site spawn = plugin.sites() == null ? null : plugin.sites().get("unlit_spawn_mirror");
        if (spawn != null) center = spawn.location();
        if (center == null && sender instanceof Player player) center = player.getLocation();
        if (center == null || center.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve an Unlit border center. Place unlit_spawn_mirror first.");
            return;
        }

        org.bukkit.WorldBorder border = center.getWorld().getWorldBorder();
        border.setCenter(center);
        border.setSize(radius * 2.0);
        sender.sendMessage("Observance: Unlit border set to radius " + radius + " around "
                + center.getBlockX() + "," + center.getBlockZ() + " in " + center.getWorld().getName() + ".");
    }

    private void handleUnlitReady(CommandSender sender) {
        handleUnlitAudit(sender);
        UnlitAuditSnapshot snapshot = collectUnlitAuditSnapshot();
        sender.sendMessage("== Unlit playtest handoff ==");
        if (snapshot.ready()) {
            sender.sendMessage("  Gate: READY - Unlit audit is clean enough for a non-op player proof pass.");
        } else {
            sender.sendMessage("  Gate: NOT READY - fix these before handing the Unlit to players:");
            int shown = 0;
            for (String blocker : snapshot.blockers()) {
                if (shown++ >= 8) {
                    sender.sendMessage("    - ...and " + (snapshot.blockers().size() - shown + 1) + " more.");
                    break;
                }
                sender.sendMessage("    - " + blocker);
            }
        }
        sender.sendMessage("  1) Run /obs unlit buildmode off before any player-facing test.");
        sender.sendMessage("  2) Confirm /obs unlit darken all has been run and audit says stray light: OK.");
        sender.sendMessage("  3) Fill a live rehearsal packet: tools\\new_rehearsal_packet.ps1");
        sender.sendMessage("  4) Include Unlit clip + house screenshots: approach, borrowed lantern route, light radius, clue readable, exit, failed-cheese.");
        sender.sendMessage("  5) Include proof that the figure breaks an exposed borrowed lantern and retreat remains readable.");
        sender.sendMessage("  6) Run: tools\\check_unlit_playtest_ready.ps1 -PacketDir rehearsals\\<date>");
        sender.sendMessage("  Finish line: when it prints 'unlit playtest readiness: OK', stop building and let Nano playtest.");
    }

    private static String unlitSiteId(String raw) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return switch (id) {
            case "entry" -> "unlit_entry";
            case "spawn", "mirror" -> "unlit_spawn_mirror";
            case "exit", "retreat" -> "unlit_exit";
            case "lamp" -> "unlit_house_lamp";
            case "cairn" -> "unlit_house_cairn";
            case "coop", "bird", "birds" -> "unlit_house_coop";
            case "well", "reflection" -> "unlit_house_well";
            case "watch", "hours" -> "unlit_house_watch";
            case "warm", "iss" -> "unlit_house_warm";
            case "threshold", "bow" -> "unlit_house_threshold";
            case "base", "record" -> "unlit_house_base";
            default -> id.startsWith("unlit_") ? id : "unlit_house_" + id;
        };
    }

    private static String unlitShortId(String siteId) {
        if (siteId == null) return "";
        if (siteId.equals("unlit_entry")) return "entry";
        if (siteId.equals("unlit_spawn_mirror")) return "spawn";
        if (siteId.equals("unlit_exit")) return "exit";
        if (siteId.startsWith("unlit_house_")) return siteId.substring("unlit_house_".length());
        return siteId;
    }

    private static String[] unlitRequiredSites() {
        return new String[]{
                "unlit_entry",
                "unlit_spawn_mirror",
                "unlit_exit",
                "unlit_house_lamp",
                "unlit_house_cairn",
                "unlit_house_coop",
                "unlit_house_well",
                "unlit_house_watch",
                "unlit_house_warm",
                "unlit_house_threshold",
                "unlit_house_base"
        };
    }

    private UnlitAuditSnapshot collectUnlitAuditSnapshot() {
        String[] required = unlitRequiredSites();
        int placed = 0;
        int proven = 0;
        List<String> blockers = new ArrayList<>();
        for (String id : required) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(id);
            Location loc = site == null ? null : site.location();
            if (site == null || !site.isPlaced() || loc == null || loc.getWorld() == null) {
                blockers.add(id + " is not placed");
                continue;
            }
            placed++;
            String issue = unlitFixtureIssue(id, loc);
            if (issue == null) {
                proven++;
            } else {
                blockers.add(id + ": " + issue);
            }
        }

        boolean enabled = plugin.getConfig().getBoolean("unlit.enabled", true);
        boolean buildmode = plugin.getConfig().getBoolean("unlit.buildmode", false);
        String worldName = plugin.getConfig().getString("unlit.world", "observance_unlit");
        World loaded = Bukkit.getWorld(worldName);
        String borderIssue = unlitBorderIssue(loaded);
        String strayLightIssue = unlitStrayLightIssue(loaded);

        if (!enabled) blockers.add("unlit.enabled is false");
        if (buildmode) blockers.add("buildmode is ON; run /obs unlit buildmode off");
        if (loaded == null) blockers.add("world " + worldName + " is not loaded/imported");
        if (borderIssue != null) blockers.add("border: " + borderIssue);
        if (strayLightIssue != null) blockers.add("stray light: " + strayLightIssue);

        return new UnlitAuditSnapshot(required.length, placed, proven, enabled, buildmode,
                worldName, loaded != null, borderIssue, strayLightIssue, blockers);
    }

    private record UnlitAuditSnapshot(int required, int placed, int proven,
                                      boolean enabled, boolean buildmode,
                                      String worldName, boolean worldLoaded,
                                      String borderIssue, String strayLightIssue,
                                      List<String> blockers) {
        boolean ready() {
            return blockers == null || blockers.isEmpty();
        }
    }

    private String unlitFixtureIssue(String siteId, Location loc) {
        if (siteId == null || loc == null || loc.getWorld() == null) return "location is not resolved";
        if (!siteId.startsWith("unlit_house_")) return null;
        int radius = 4;
        String house = unlitShortId(siteId);
        return switch (house) {
            case "lamp" -> (!hasMaterialNear(loc, radius, Material.LECTERN)
                    || !hasMaterialNear(loc, radius, Material.BLACK_CANDLE))
                    ? "expected ledger lectern and black candle evidence" : null;
            case "cairn" -> (!hasMaterialNear(loc, radius, Material.CAULDRON)
                    || !hasMaterialNear(loc, radius, Material.COBBLED_DEEPSLATE)
                    || !hasMaterialNear(loc, radius, Material.POLISHED_DEEPSLATE))
                    ? "expected offering bowl and deepslate return stones" : null;
            case "coop" -> (!hasMaterialNear(loc, radius, Material.HAY_BLOCK)
                    || !hasMaterialNear(loc, radius, Material.IRON_BARS)
                    || !hasMaterialNear(loc, radius, Material.OAK_FENCE))
                    ? "expected silent perch, cage bars, and hay" : null;
            case "well" -> (!hasMaterialNear(loc, radius, Material.WATER_CAULDRON)
                    || !hasMaterialNear(loc, radius, Material.DARK_PRISMARINE)
                    || !hasMaterialNear(loc, radius, Material.POLISHED_BLACKSTONE)
                    || !hasMaterialNear(loc, radius, Material.LECTERN))
                    ? "expected water/reflection bowl, dark prismarine, blackstone, and a written well note" : null;
            case "watch" -> (!hasMaterialNear(loc, radius, Material.BELL)
                    || !hasMaterialNear(loc, radius, Material.BLACK_CARPET)
                    || !hasMaterialNear(loc, radius, Material.LECTERN))
                    ? "expected bell, dark watch marks, and a written watch log" : null;
            case "warm" -> (!hasMaterialNear(loc, radius, Material.CAMPFIRE)
                    || !hasMaterialNear(loc, radius, Material.RED_WOOL)
                    || !hasMaterialNear(loc, radius, Material.BLUE_ICE))
                    ? "expected unlit campfire and false warmth/cold contrast" : null;
            case "threshold" -> (!hasMaterialNear(loc, radius, Material.POLISHED_BLACKSTONE)
                    || !hasMaterialNear(loc, radius, Material.DEEPSLATE_BRICK_SLAB)
                    || !hasMaterialNear(loc, radius, Material.BLACK_CARPET))
                    ? "expected low lintel and black threshold marks" : null;
            case "base" -> (!hasMaterialNear(loc, radius, Material.BARREL)
                    || !hasMaterialNear(loc, radius, Material.LECTERN))
                    ? "expected copied-base barrel and docket lectern" : null;
            default -> "unknown Unlit house id";
        };
    }

    private String unlitBorderIssue(org.bukkit.World world) {
        if (world == null) return "load observance_unlit, then run /obs unlit border";
        Site spawn = plugin.sites() == null ? null : plugin.sites().get("unlit_spawn_mirror");
        Location center = spawn == null ? null : spawn.location();
        if (center == null || center.getWorld() == null) return "place unlit_spawn_mirror, then run /obs unlit border";
        org.bukkit.WorldBorder border = world.getWorldBorder();
        Location actual = border.getCenter();
        double expectedSize = plugin.getConfig().getInt("unlit.border-radius", 96) * 2.0;
        double dx = actual.getX() - center.getX();
        double dz = actual.getZ() - center.getZ();
        if (!actual.getWorld().equals(center.getWorld())
                || (dx * dx) + (dz * dz) > 4.0
                || Math.abs(border.getSize() - expectedSize) > 1.0) {
            return "run /obs unlit border after final spawn placement";
        }
        return null;
    }

    private String unlitStrayLightIssue(World world) {
        if (world == null) return null;
        UnlitLightScan scan = scanUnlitStrayLights(world);
        if (scan.count == 0) return null;
        return scan.count + " unauthorized light source(s) inside the Unlit border/anchor scan; first "
                + scan.firstType + " at " + scan.firstX + "," + scan.firstY + "," + scan.firstZ
                + ". Run /obs unlit darken all, then audit again.";
    }

    private UnlitLightScan scanUnlitStrayLights(World world) {
        UnlitLightScan scan = new UnlitLightScan();
        Set<String> seen = new HashSet<>();
        Site spawn = plugin.sites() == null ? null : plugin.sites().get("unlit_spawn_mirror");
        Location center = spawn == null ? null : spawn.location();
        if (center != null && center.getWorld() != null && center.getWorld().getName().equals(world.getName())) {
            int radius = Math.max(16, Math.min(256, plugin.getConfig().getInt("unlit.border-radius", 96)));
            scanUnlitStrayLightsInBorder(center, radius, seen, scan);
            return scan;
        }
        for (String id : unlitRequiredSites()) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(id);
            Location loc = site == null ? null : site.location();
            if (site == null || !site.isPlaced() || loc == null || loc.getWorld() == null) continue;
            if (!loc.getWorld().getName().equals(world.getName())) continue;
            scanUnlitStrayLightsNear(loc, 10, seen, scan);
        }
        return scan;
    }

    private void scanUnlitStrayLightsInBorder(Location center, int radius, Set<String> seen, UnlitLightScan scan) {
        if (center == null || center.getWorld() == null || scan == null) return;
        World world = center.getWorld();
        int r = Math.max(16, Math.min(256, radius));
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), cy - 32);
        int maxY = Math.min(world.getMaxHeight() - 1, cy + 56);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(cx + dx, y, cz + dz);
                    String key = blockKey(block);
                    if (!seen.add(key)) continue;
                    if (isInsideUnlitSafe(block.getLocation())) continue;
                    if (!isUnauthorizedUnlitLight(block)) continue;
                    scan.count++;
                    if (scan.firstType == null) {
                        scan.firstType = block.getType().name();
                        scan.firstX = block.getX();
                        scan.firstY = block.getY();
                        scan.firstZ = block.getZ();
                    }
                }
            }
        }
    }

    private void scanUnlitStrayLightsNear(Location loc, int radius, Set<String> seen, UnlitLightScan scan) {
        if (loc == null || loc.getWorld() == null || scan == null) return;
        World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(4, Math.min(32, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 8; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    String key = blockKey(block);
                    if (!seen.add(key)) continue;
                    if (isInsideUnlitSafe(block.getLocation())) continue;
                    if (!isUnauthorizedUnlitLight(block)) continue;
                    scan.count++;
                    if (scan.firstType == null) {
                        scan.firstType = block.getType().name();
                        scan.firstX = block.getX();
                        scan.firstY = block.getY();
                        scan.firstZ = block.getZ();
                    }
                }
            }
        }
    }

    private int darkenUnlitStrayLightsNear(Location loc, int radius, Set<String> seen) {
        if (loc == null || loc.getWorld() == null) return 0;
        World world = loc.getWorld();
        int changed = 0;
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(4, Math.min(32, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 8; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    String key = blockKey(block);
                    if (!seen.add(key)) continue;
                    if (isInsideUnlitSafe(block.getLocation())) continue;
                    if (!isUnauthorizedUnlitLight(block)) continue;
                    darkenUnlitLightBlock(block);
                    changed++;
                }
            }
        }
        return changed;
    }

    private int darkenUnlitStrayLightsInBorder(Location center, int radius, Set<String> seen) {
        if (center == null || center.getWorld() == null) return 0;
        World world = center.getWorld();
        int changed = 0;
        int r = Math.max(16, Math.min(256, radius));
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), cy - 32);
        int maxY = Math.min(world.getMaxHeight() - 1, cy + 56);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(cx + dx, y, cz + dz);
                    String key = blockKey(block);
                    if (!seen.add(key)) continue;
                    if (isInsideUnlitSafe(block.getLocation())) continue;
                    if (!isUnauthorizedUnlitLight(block)) continue;
                    darkenUnlitLightBlock(block);
                    changed++;
                }
            }
        }
        return changed;
    }

    private boolean isInsideUnlitSafe(Location loc) {
        if (loc == null || loc.getWorld() == null || plugin.sites() == null) return false;
        for (Site site : plugin.sites().placed()) {
            if (!"unlit_safe".equals(site.type())) continue;
            if (site.contains(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ())) return true;
        }
        return false;
    }

    private boolean isUnauthorizedUnlitLight(Block block) {
        if (block == null || block.getType() == Material.AIR) return false;
        Material material = block.getType();
        String name = material.name();
        try {
            org.bukkit.block.data.BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.Lightable lightable
                    && (name.contains("CANDLE") || name.contains("CAMPFIRE"))) {
                return lightable.isLit();
            }
        } catch (Throwable ignored) { }

        return material == Material.LIGHT
                || material == Material.GLOWSTONE
                || material == Material.SEA_LANTERN
                || material == Material.SHROOMLIGHT
                || material == Material.END_ROD
                || material == Material.JACK_O_LANTERN
                || name.contains("FROGLIGHT")
                || name.contains("TORCH")
                || name.contains("LANTERN");
    }

    private void darkenUnlitLightBlock(Block block) {
        if (block == null) return;
        Material material = block.getType();
        String name = material.name();
        try {
            org.bukkit.block.data.BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.Lightable lightable
                    && (name.contains("CANDLE") || name.contains("CAMPFIRE"))) {
                lightable.setLit(false);
                block.setBlockData(lightable, false);
                return;
            }
        } catch (Throwable ignored) { }

        if (material == Material.GLOWSTONE
                || material == Material.SEA_LANTERN
                || material == Material.SHROOMLIGHT
                || material == Material.JACK_O_LANTERN
                || name.contains("FROGLIGHT")) {
            block.setType(Material.BLACKSTONE, false);
        } else {
            block.setType(Material.AIR, false);
        }
    }

    private static String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static final class UnlitLightScan {
        int count;
        String firstType;
        int firstX;
        int firstY;
        int firstZ;
    }

    private String stampUnlitClue(Location loc, String siteId) {
        if (loc == null || loc.getWorld() == null) return "No world location resolved.";
        Location base = loc.getBlock().getLocation();
        BlockFace facing = cardinalFacing(loc.getYaw());
        String house = unlitShortId(siteId);
        switch (house) {
            case "lamp" -> {
                setBlock(base, Material.LECTERN);
                faceDirectional(base, facing);
                fillWrittenLecternBook(base.getBlock(), "borrowed lantern", "the record", List.of(
                        "oil is counted here.",
                        "names are counted here.",
                        "borrowed lanterns are counted last.",
                        "what burns in the copy is never returned."
                ));
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CANDLE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
                return "Reads as a ledger and a burnt accounting mark, not a signboard.";
            }
            case "cairn" -> {
                setBlock(base, Material.CAULDRON);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.COBBLED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.COBBLED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, 0, -1, 0), Material.POLISHED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.COBBLED_DEEPSLATE_SLAB);
                return "Use as an offering bowl; the return is a shape, not written instructions.";
            }
            case "coop" -> {
                setBlock(base, Material.HAY_BLOCK);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.IRON_BARS);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.IRON_BARS);
                setBlock(base.clone().add(0, 1, 0), Material.OAK_FENCE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.LIGHT_GRAY_CARPET);
                return "Sacred-beast clue; the untouched perch and missing call are the evidence.";
            }
            case "well" -> {
                setBlock(base, Material.WATER_CAULDRON);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.DARK_PRISMARINE);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.DARK_PRISMARINE);
                setBlock(offsetFrom(base, facing, 0, -1, 0), Material.POLISHED_BLACKSTONE);
                placeUnlitEvidenceBook(base, facing, "well copy", List.of(
                        "the upper line is always the liar.",
                        "read what the water keeps under it.",
                        "one copy came back wrong."
                ));
                return "Reflection clue; the readable surface is now a found well note.";
            }
            case "watch" -> {
                setBlock(base, Material.BELL);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLACK_CARPET);
                placeUnlitEvidenceBook(base, facing, "watch floor", List.of(
                        "no moon entered the count.",
                        "no bed relieved the watch.",
                        "four names slept. one name stood."
                ));
                return "Dark-hours clue now lives in a watch log.";
            }
            case "warm" -> {
                setBlock(base, Material.CAMPFIRE);
                try {
                    org.bukkit.block.data.BlockData data = base.getBlock().getBlockData();
                    if (data instanceof org.bukkit.block.data.Lightable lit) {
                        lit.setLit(false);
                        base.getBlock().setBlockData(lit, false);
                    }
                } catch (Throwable ignored) { }
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.RED_WOOL);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLUE_ICE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.RED_CONCRETE);
                return "False-warmth clue; the too-bright red mark is the lie.";
            }
            case "threshold" -> {
                setBlock(base, Material.POLISHED_BLACKSTONE);
                setBlock(base.clone().add(0, 1, 0), Material.DEEPSLATE_BRICK_SLAB);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
                return "Threshold/bow clue; the low lintel teaches the verb without a sign.";
            }
            case "base" -> {
                setBlock(base, Material.BARREL);
                Location lectern = offsetFrom(base, facing, 1, 0, 0);
                setBlock(lectern, Material.LECTERN);
                faceDirectional(lectern, facing);
                fillWrittenLecternBook(lectern.getBlock(), "copy docket", "the record", List.of(
                        "the copy keeps the player-made marks.",
                        "a fence turned, a stair repaired, a door left open.",
                        "the village remembers the surface without carrying its warmth."
                ));
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.CALIBRATED_SCULK_SENSOR);
                return "Surface-copy clue; anchors the mirror-village premise without a signboard.";
            }
            default -> {
                placeStandingSign(base, facing,
                        new String[]{"unlit house", house, "author clue", "place by hand"});
                return "Unknown house id; stamped a neutral marker.";
            }
        }
    }

    private void placeUnlitEvidenceBook(Location base, BlockFace facing, String title, List<String> pages) {
        Location lectern = offsetFrom(base, facing, 0, 1, 0);
        placeEvidenceLectern(lectern, facing, title, pages);
    }

    private void placeEvidenceLectern(Location loc, BlockFace facing, String title, List<String> pages) {
        if (loc == null || loc.getWorld() == null) return;
        setBlock(loc, Material.LECTERN);
        faceDirectional(loc, facing);
        fillWrittenLecternBook(loc.getBlock(), title, "the record", pages);
    }

    private static Location offsetFrom(Location base, BlockFace facing, int right, int forward, int up) {
        Location loc = base.clone();
        BlockFace f = cardinalOnly(facing);
        BlockFace r = rightOf(f);
        loc.add(r.getModX() * right + f.getModX() * forward,
                up,
                r.getModZ() * right + f.getModZ() * forward);
        return loc;
    }

    private static BlockFace cardinalFacing(float yaw) {
        float wrapped = ((yaw % 360f) + 360f) % 360f;
        if (wrapped >= 45f && wrapped < 135f) return BlockFace.WEST;
        if (wrapped >= 135f && wrapped < 225f) return BlockFace.NORTH;
        if (wrapped >= 225f && wrapped < 315f) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }

    private static BlockFace cardinalOnly(BlockFace facing) {
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> facing;
            default -> BlockFace.SOUTH;
        };
    }

    private static BlockFace rightOf(BlockFace facing) {
        return switch (cardinalOnly(facing)) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    private static void faceDirectional(Location loc, BlockFace facing) {
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();
        if (block.getBlockData() instanceof Directional d) {
            d.setFacing(cardinalOnly(facing));
            block.setBlockData(d, false);
        }
    }

    private static void setBlock(Location loc, Material material) {
        if (loc == null || loc.getWorld() == null || material == null) return;
        loc.getBlock().setType(material, false);
    }

    private void registerUnlitSite(Location loc, String siteId, String type, int radius, int verticalRadius) {
        if (loc == null || loc.getWorld() == null) return;
        Site site = new Site(siteId, type, loc.getWorld().getName(),
                (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                radius, verticalRadius, true, true);
        plugin.registerRuntimeSite(site);
    }

    private void buildUnlitPassLane(Location base, int length) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();
        for (int x = 0; x <= length; x++) {
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(bx + x, by - 1, bz + z).setType(Material.POLISHED_BLACKSTONE, false);
                if (Math.abs(z) == 1 && x % 4 == 0) {
                    world.getBlockAt(bx + x, by, bz + z).setType(Material.BLACKSTONE_WALL, false);
                }
            }
        }
    }

    private void handleSiteTodo(CommandSender sender) {
        sender.sendMessage("== Observance launch proof rows ==");
        if (plugin.sites() == null) {
            sender.sendMessage("Observance: sites config unavailable. Run /obs reload or check sites.yml.");
            return;
        }
        int placed = 0;
        int holdMissing = 0;
        List<String> missing = new ArrayList<>();
        for (String id : LAUNCH_REQUIRED_SITES) {
            String issue = launchSiteIssue(id);
            if (issue == null) {
                placed++;
            } else if (isDeepHoldLaunchSite(id) && !isDeepHoldBuilt()) {
                holdMissing++;
            } else {
                missing.add(issue);
            }
        }
        sender.sendMessage("Proofed/placed: " + placed + "/" + LAUNCH_REQUIRED_SITES.length
                + " launch-required rows.");
        if (holdMissing > 0) {
            sender.sendMessage("  - " + holdMissing
                    + " Deep Hold row(s) are generated together: run /obs placehold build, then /obs placehold audit; record GeneratedProof in coords-capture.csv.");
        }
        if (missing.isEmpty()) {
            if (holdMissing == 0) {
                sender.sendMessage("[OK] Launch proof rows are complete. Next: /obs preflight, then live rehearsal packet.");
            } else {
                sender.sendMessage("Next: /obs placehold build, then /obs placehold audit.");
            }
            return;
        }
        int shown = Math.min(12, missing.size());
        for (int i = 0; i < shown; i++) {
            sender.sendMessage("  - " + missing.get(i));
        }
        if (missing.size() > shown) {
            sender.sendMessage("  ... " + (missing.size() - shown) + " more. Use /obs site next for the next outside-Hold survey target.");
        }
        sender.sendMessage(holdMissing > 0
                ? "Next: /obs placehold build, then /obs placehold audit."
                : "Next: stand at the intended site and run /obs site set " + nextLaunchMissingId() + ".");
        sender.sendMessage("Lane focus: /obs site plan lanes, /obs placehold audit for Hold rooms, or /obs site next dimensions.");
    }

    private void handleSiteNext(CommandSender sender, String[] args) {
        String laneId = args.length > 2 ? args[2].toLowerCase(Locale.ROOT).trim() : "";
        PlacementLane lane = laneId.isBlank() ? null : placementLane(laneId);
        if (!laneId.isBlank() && lane == null) {
            sender.sendMessage("Observance: unknown placement lane '" + laneId + "'. Use /obs site plan lanes.");
            return;
        }
        String id = lane == null ? nextLaunchMissingId() : nextLaunchMissingId(lane);
        if (id == null) {
            sender.sendMessage(lane == null
                    ? "[OK] Every launch-required proof row is complete. Run /obs preflight and the rehearsal packet."
                    : "[OK] Placement lane '" + lane.id() + "' is complete. Run /obs site next for the next global target.");
            return;
        }
        sender.sendMessage("Next launch proof row" + (lane == null ? "" : " in " + lane.id()) + ": " + id + ".");
        if (isDeepHoldLaunchSite(id) && !isDeepHoldBuilt()) {
            sender.sendMessage("This is Deep Hold-owned. Do not survey it by hand; run /obs placehold build, then /obs placehold audit, then record GeneratedProof.");
            sendPlacementBrief(sender, id, false);
            return;
        }
        sender.sendMessage("Stand at its real anchor and run: /obs site set " + id);
        sender.sendMessage("Director rule: place one site, build/prove it, then move on. Do not batch unknown structures.");
        sendPlacementBrief(sender, id, false);
        if (isDeepHoldLaunchSite(id)) {
            sender.sendMessage("  Deep Hold-owned: if this is still missing after build, rerun /obs placehold audit before hand-fixing.");
            return;
        }
        if (isLaneSite(id)) {
            sender.sendMessage("  Dimension lane: do this while standing in the correct Nether/End world, then run /obs placeworld there.");
        } else if (keeperRow(id) != null) {
            sender.sendMessage("  Spine set-piece: after surveying, run /obs placeworld from the build world.");
        } else if (isPlaceWorldSurveyFixture(id)) {
            sender.sendMessage("  Surveyed fixture: after surveying, run /obs placeworld from the same world to build it.");
        }
    }

    private void handleSitePlan(CommandSender sender, String[] args) {
        String raw = args.length > 2 ? args[2].toLowerCase(Locale.ROOT).trim() : "next";
        if (raw.isBlank() || raw.equals("next")) {
            String next = nextLaunchMissingId();
            if (next == null) {
                sender.sendMessage("[OK] Every launch-required proof row is complete. Use /obs site plan all for the full route brief.");
                return;
            }
            sender.sendMessage("Next placement plan: " + next);
            sendPlacementBrief(sender, next, true);
            return;
        }
        if (raw.equals("lanes")) {
            sender.sendMessage("== Observance placement lanes ==");
            for (PlacementLane lane : PLACEMENT_LANES) {
                int placed = 0;
                for (String siteId : lane.sites()) {
                    if (launchSiteIssue(siteId) == null) placed++;
                }
                sender.sendMessage("[" + lane.id() + "] " + lane.label() + " - " + placed + "/" + lane.sites().length + " placed");
                String next = nextLaunchMissingId(lane);
                if (next != null) sender.sendMessage("  next: /obs site next " + lane.id() + " -> " + next);
            }
            return;
        }
        if (raw.equals("all")) {
            sender.sendMessage("== Observance launch placement plan ==");
            for (PlacementLane lane : PLACEMENT_LANES) {
                sender.sendMessage("[" + lane.id() + "] " + lane.label());
                for (String id : lane.sites()) {
                    PlacementBrief brief = placementBrief(id);
                    String status = launchSiteIssue(id) == null ? (isDeepHoldLaunchSite(id) ? "[generated]" : "[placed]") : "[todo]";
                    sender.sendMessage("  " + status + " " + id + " - " + brief.intent);
                }
            }
            sender.sendMessage("Use /obs site plan <siteId> for placement rule + proof shots.");
            sender.sendMessage("Recommended flow: generate packets -> /obs placehold build/audit -> /obs site plan lanes -> outside-Hold /obs site next <lane> -> /obs site set + /obs placeworld.");
            return;
        }
        PlacementLane lane = placementLane(raw);
        if (lane != null) {
            sender.sendMessage("== placement lane: " + lane.id() + " ==");
            sender.sendMessage(lane.label());
            for (String id : lane.sites()) {
                PlacementBrief brief = placementBrief(id);
                String status = launchSiteIssue(id) == null ? (isDeepHoldLaunchSite(id) ? "[generated]" : "[placed]") : "[todo]";
                sender.sendMessage(status + " " + id + " - " + brief.intent);
            }
            String next = nextLaunchMissingId(lane);
            sender.sendMessage(next == null
                    ? "[OK] Lane placed. Use /obs preflight or continue another lane."
                    : "Next in lane: /obs site next " + lane.id() + " -> " + next);
            return;
        }
        if (!isLaunchRequiredSite(raw)) {
            sender.sendMessage("Observance: '" + raw + "' is not in the launch-required placement plan.");
            sender.sendMessage("Try /obs site plan lanes, /obs site plan all, /obs site plan human, or a launch site id.");
            return;
        }
        sendPlacementBrief(sender, raw, true);
    }

    private void sendPlacementBrief(CommandSender sender, String id, boolean includeStatus) {
        PlacementBrief brief = placementBrief(id);
        if (includeStatus) {
            String issue = launchSiteIssue(id);
            sender.sendMessage("== " + id + " ==");
            sender.sendMessage("Status: " + (issue == null ? (isDeepHoldLaunchSite(id) ? "generated/proof-ready" : "placed") : issue));
            String lane = placementLaneId(id);
            if (!lane.isBlank()) sender.sendMessage("Lane: " + lane + "   (use /obs site next " + lane + ")");
        }
        sender.sendMessage("  Intent: " + brief.intent);
        if (isDeepHoldLaunchSite(id)) {
            sender.sendMessage("  Placement owner: /obs placehold build (generated city); proof this room in place, do not scatter it.");
            sender.sendMessage("  Place: generated inside the Deep Hold district plan; use the intent as the room-proof lens.");
            sender.sendMessage("  Capture: coords-capture.csv GeneratedProof + four room proof shots.");
        } else {
            sender.sendMessage("  Place: " + brief.placeRule);
        }
        sender.sendMessage("  Proof: " + brief.proof);
    }

    private record PlacementBrief(String intent, String placeRule, String proof) { }

    private PlacementBrief placementBrief(String id) {
        return switch (id) {
            case "first_report_lectern_01" -> new PlacementBrief(
                    "cold-open record handoff; the first readable lie/truth surface",
                    "near the first believable player disturbance, not in a spawn plaza",
                    "approach shows the report as found, focal shot shows the lectern/book, exit points toward first_marker_01");
            case "rune_rosetta" -> new PlacementBrief(
                    "alphabet literacy; the first place that says this world has rules",
                    "visible after the first marker but not on a straight road; give it a silhouette",
                    "approach, full rune ring, blank answer surface, and return view toward keeper scatter");
            case "stone_vaun" -> keeperPlacementBrief("Vaun", "hoarding/counting", "near storage, ore, or a pinched resource route");
            case "stone_mara" -> keeperPlacementBrief("Mara", "reading/deferral", "near shelves, ruins, map clutter, or a place players already inspect");
            case "stone_sella" -> keeperPlacementBrief("Sella", "water/mirror/wandering", "near water with a safe approach and an uneasy sightline");
            case "stone_orin" -> keeperPlacementBrief("Orin", "bow/refusal/smallness", "near a low threshold, crouchable marker, or compressed passage");
            case "stone_brann" -> keeperPlacementBrief("Brann", "watching/sleep/light", "near a high watch angle or dark route, away from cozy beds");
            case "stone_iss" -> keeperPlacementBrief("Iss", "warm lie/catch", "near warmth that later feels suspicious, not beside the real payoff");
            case "stone_of_reckoning" -> new PlacementBrief(
                    "digit literacy and count logic; the number language becomes physical",
                    "place where players can circle it and compare marks without mobs interrupting",
                    "wide shot of count geometry, focal shot of digit key, answer/action surface, exit toward deep route");
            case "vaun_hoard_chest" -> new PlacementBrief(
                    "Vaun theory proof: the empty given-back column becomes an inventory act",
                    "inside or beside Vaun's storage geometry; the empty return chest must be readable before use",
                    "approach to hoard, empty given-back chest, deposited deep item, and opened cache/state");
            case "vaun_bookshelf" -> new PlacementBrief(
                    "Vaun theory proof: all taken / none given back becomes a six-slot count",
                    "near the hoard chest but not touching it; let the shelf read as ledger storage, not a code pad",
                    "shelf face, six-slot fill pattern, ledger clue nearby, and solved/open state");
            case "mara_lectern_1" -> maraLecternPlacementBrief(1);
            case "mara_lectern_2" -> maraLecternPlacementBrief(2);
            case "mara_lectern_3" -> maraLecternPlacementBrief(3);
            case "mara_lectern_4" -> maraLecternPlacementBrief(4);
            case "mara_lectern_5" -> maraLecternPlacementBrief(5);
            case "mara_map_marker" -> new PlacementBrief(
                    "Mara theory proof: the map has to be walked by the group",
                    "at the marker row destination with enough level ground for every present player to bow together",
                    "approach from marker row, group bow positions, map/route clue, and solved cue");
            case "sella_pool" -> new PlacementBrief(
                    "Sella theory proof: the first bearing exists only as a water reflection",
                    "at a real shore or still pool where looking down is natural and safe",
                    "approach to water, reflection sightline, readable mirrored rune, and path toward far water");
            case "sella_anchor" -> new PlacementBrief(
                    "Sella theory proof: the shore scatter resolves only from one worn standing place",
                    "above or beside the far-water pool; the bird-over-water silhouette must appear from this block",
                    "anchor block, view-down alignment, resolved bird scatter, and return path");
            case "orin_marker_1" -> orinMarkerPlacementBrief(1);
            case "orin_marker_2" -> orinMarkerPlacementBrief(2);
            case "orin_marker_3" -> orinMarkerPlacementBrief(3);
            case "orin_marker_4" -> orinMarkerPlacementBrief(4);
            case "orin_marker_5" -> orinMarkerPlacementBrief(5);
            case "orin_marker_6" -> orinMarkerPlacementBrief(6);
            case "orin_frame_dial_1" -> orinDialPlacementBrief(1);
            case "orin_frame_dial_2" -> orinDialPlacementBrief(2);
            case "orin_frame_dial_3" -> orinDialPlacementBrief(3);
            case "orin_frame_dial_4" -> orinDialPlacementBrief(4);
            case "orin_frame_dial_5" -> orinDialPlacementBrief(5);
            case "orin_frame_dial_6" -> orinDialPlacementBrief(6);
            case "brann_toll_tower" -> new PlacementBrief(
                    "Brann theory proof: the black-moon toll is heard, not decoded from a plaque",
                    "a high or exposed watch point where night changes the read; keep bed/shelter comfort away",
                    "night approach, bell/toll surface, watch position, and heard/awake proof");
            case "brann_corridor_start" -> new PlacementBrief(
                    "Brann theory proof: silence begins here",
                    "at the mouth of a sculk corridor with an obvious quiet path and no jump-scare clutter",
                    "start threshold, visible sculk listening surfaces, crouch entry, and route to far end");
            case "brann_corridor_end" -> new PlacementBrief(
                    "Brann theory proof: silence must survive to the far door",
                    "at the corridor's far threshold; it should be reachable only by staying quiet through the route",
                    "far door, corridor back-view, solved/open state, and exit into Brann evidence");
            case "bow_marker_01" -> new PlacementBrief(
                    "teaches bowing as a custom before it is demanded",
                    "at a threshold players naturally slow at; leave room for multiple bodies",
                    "show standing approach, crouch position, marker detail, and resulting cue/state");
            case "offering_cairn_01" -> new PlacementBrief(
                    "teaches giving back through a drop action",
                    "near a route players revisit with inventory, not hidden in clutter",
                    "show cairn silhouette, dropped item position, accepted state, and route away");
            case "kept_light_home_01" -> new PlacementBrief(
                    "the one kept-light home proof; warm light as evidence, not decoration",
                    "domestic or civic remnant where a kept lamp feels wrong but plausible",
                    "show warm/cold contrast, lamp focal shot, nearby record surface, and exit darkness");
            case "the_far_water" -> new PlacementBrief(
                    "Sella mirror/count side proof",
                    "on a real shoreline or pool edge where looking into water is natural",
                    "mirror water, copybook shelf, six stones plus grey seventh, and return path");
            case "school_stand" -> new PlacementBrief(
                    "child-scale rule copying; the human thread gets a physical classroom scar",
                    "small civic remnant, not a full schoolhouse; the slate must face approach",
                    "slate, copy-line, six stones, grey seventh marker, and exit shot");
            case "markers_row" -> new PlacementBrief(
                    "bow-count contradiction; six marks and the missing seventh",
                    "linear path where players can count while walking, with space to crouch",
                    "full row, worn bow marks, hollow seventh, and view back along the row");
            case "cistern_7" -> new PlacementBrief(
                    "light fouled by water; Cistern 7 as a false-good utility place",
                    "low wet chamber or reservoir edge; black water must be visible on approach",
                    "black water, pale arch, good-oil jars, lying-lamp reflection, and exit");
            case "watch_floor" -> new PlacementBrief(
                    "Brann's dark-hours proof; a place for not sleeping",
                    "dark overlook or room with a central log; avoid cozy shelter language",
                    "watch log, black-moon warning, finished kept line, and night approach");
            case "set_apart_shelf" -> new PlacementBrief(
                    "entry five: a warm lamp counted with cold ones",
                    "near archive/storage, after watch-floor logic, where a shelf can be inspected slowly",
                    "cold shelf, warm entry-five lamp, redacted count sign, and approach/exit shots");
            case "undercroft_seal" -> new PlacementBrief(
                    "Orin's seal and the low line; bowing matters as reading posture",
                    "at a sealed or blocked route where the low sign is truly low to the body",
                    "standing seal line, low bow-to-read text, door face, and route consequence");
            case "forgotten_mouth" -> new PlacementBrief(
                    "the rumored true way up; surface healing seen from below",
                    "at an upward opening that can be believed as a route, not a random skylight",
                    "mouth silhouette, healed surface, last draft/return mark, and exit cue");
            case "the_cold_hearth" -> new PlacementBrief(
                    "Iss's false-warm dead shrine; warmth becomes suspect",
                    "where a hearth would be comforting if it were not wrong",
                    "dead hearth, false warmth clue, answer surface, and route back to catch");
            case "case_board" -> new PlacementBrief(
                    "post-keeper casework; decoded stones must become corroborated evidence",
                    "on the route from keeper court to archive galleries, before any finale-facing gate",
                    "both lecterns, proof-category signs, filed shelves/storage, and route into side evidence");
            case "unbroken_light" -> new PlacementBrief(
                    "the accepting floor and one fire; main ritual gravity",
                    "largest controlled deep chamber; should feel staged before it becomes a mechanic",
                    "approach scale, central light, action floor, and group standing positions");
            case "the_threshold" -> new PlacementBrief(
                    "grave/threshold that opens from inside; future appointment made spatial",
                    "quiet threshold with room for a grave marker and a clear before/after view",
                    "closed threshold, carved/date surface, opened state, and exit");
            case "the_unwriting" -> new PlacementBrief(
                    "Seventh/unwriting chamber; absence made spatial",
                    "isolated from ordinary route clutter; the missing count must be the first read",
                    "approach emptiness, focal mark, choice surface, and return view");
            case "keeper_altar" -> new PlacementBrief(
                    "presiding keeper surface; social lore becomes embodied",
                    "near but not inside the main route, where an NPC can face players cleanly",
                    "altar silhouette, keeper body sightline, interaction range, and exit");
            case "coop_plate" -> new PlacementBrief(
                    "co-op plate / three-hands gate; group presence becomes required",
                    "flat enough for bodies, framed enough to read as a threshold",
                    "all body positions, plate glyphs, solved/open state, and route forward");
            case "threshold_vault" -> new PlacementBrief(
                    "vault payoff; answer becomes a room, not a sign",
                    "behind the co-op gate with a strong threshold and no accidental access",
                    "door/plate, vault face, input surface, opened/interior view");
            case "lampworks_stair" -> new PlacementBrief(
                    "the big Stair and lamp-house route promised by NPC dialogue",
                    "actual descent with a readable top, middle, and lower landing",
                    "top approach, third lamp sightline, painted line below, and return up-stair");
            case "third_lamp_stand" -> new PlacementBrief(
                    "Coll's third-lamp errand; light action must have a body",
                    "on the Lamp-works route where 'third' can be counted",
                    "three-lamp sequence, target stand, action state, and route to line");
            case "painted_line" -> new PlacementBrief(
                    "the crossing that sets painted_line_crossed",
                    "across the actual descent path, impossible to miss but possible to hesitate at",
                    "line from approach, feet-on-line action, cue/result, and what lies beyond");
            case "dead_stall" -> new PlacementBrief(
                    "Wenna's dead-stall offering; food grief becomes an action",
                    "near market/lampworks remnants, with a clear drop spot",
                    "stall silhouette, offering position, accepted state, and return line");
            case "deep_bird_coops" -> new PlacementBrief(
                    "Aro's bird/coops rumor; visible cages instead of empty air",
                    "off the descent route where the old-bird story can feel discoverable",
                    "empty cages, bars, entry sign, and exit back to main route");
            case "deep_market" -> new PlacementBrief(
                    "inhabited market proof before the warm-town contradiction",
                    "a wider civic pocket; must read as a real used place, not a signpost",
                    "18-stall read, central board, ration/route adjacency, and warm-town exit");
            case "ration_table" -> new PlacementBrief(
                    "human ration grief; a count with a body missing from it",
                    "inside or beside market route, not isolated as a puzzle pad",
                    "half-loaf/no-head setting, child line, table detail, and exit");
            case "third_bay_breach" -> new PlacementBrief(
                    "Deep Line taboo and downward break",
                    "on a route where 'do not cross' can be physically disobeyed",
                    "broken line, cold lamp, downward breach, and no-road warning");
            case "warm_town_collapse" -> new PlacementBrief(
                    "the one blunt false lead with teeth; Aro's warm town collapses",
                    "east of Deep Market or wherever the clue honestly points; not a random ruin",
                    "approach promise, collapsed gallery, dead lamp/notice, and belief-change exit");
            case "dread_route_start" -> dreadPlacementBrief("start gate", "clear beginning without an admin label");
            case "dread_route_elsewhere" -> dreadPlacementBrief("wrong room / elsewhere pressure", "a room that feels displaced, not just dark");
            case "dread_route_figure" -> dreadPlacementBrief("figure niche", "one controlled sightline for a rare humanoid presence");
            case "dread_route_exit" -> dreadPlacementBrief("exit threshold", "release point with aftertaste, not a teleport pad");
            case "nether_forge" -> new PlacementBrief(
                    "Nether fire-pocket deepening lane",
                    "survey in the real Nether, in a pocket that feels found and dangerous",
                    "approach, forge focal, answer/sign surface, and route back");
            case "end_seventh_shrine" -> new PlacementBrief(
                    "End exile-shrine; the far side of being cast out",
                    "survey in the End with a clean horizon and strong isolation",
                    "approach emptiness, shrine focal, answer/sign surface, and return");
            default -> new PlacementBrief(
                    "launch-required site",
                    "place where the clue honestly points and the structure can be read from approach",
                    "approach, focal object, action/answer surface, and exit/return");
        };
    }

    private PlacementBrief keeperPlacementBrief(String keeper, String motif, String placeRule) {
        return new PlacementBrief(
                keeper + " keeper evidence site; " + motif + " becomes a readable place",
                placeRule + "; scatter away from sibling evidence sites so it is found, not farmed",
                "approach silhouette, keeper-specific focal object, answer surface, and route away");
    }

    private PlacementBrief maraLecternPlacementBrief(int index) {
        return new PlacementBrief(
                "Mara theory proof: lectern " + index + " of five in the annotated page-lock",
                "place with the other Mara lecterns as one readable shelf, not as five isolated stations",
                "shelf approach, lectern " + index + " page, lamp/comparator state, and whole five-lectern view");
    }

    private PlacementBrief orinMarkerPlacementBrief(int index) {
        return new PlacementBrief(
                "Orin theory proof: fall-order bow marker " + index + " of six",
                "space the six markers as a walkable row; each must have room for a crouch without falling or fighting mobs",
                "marker " + index + " approach, crouch position, facing/mark detail, and view to the next marker");
    }

    private PlacementBrief orinDialPlacementBrief(int index) {
        return new PlacementBrief(
                "Orin theory proof: frame dial " + index + " of six echoes the marker facings",
                "group the six dials in one niche or wall so players compare them with the marker walk",
                "dial " + index + " face, intended rotation, full six-dial wall, and opened niche/state");
    }

    private PlacementBrief dreadPlacementBrief(String role, String placeRule) {
        return new PlacementBrief(
                "dread route " + role + "; scary because of space and timing, not text",
                placeRule + "; keep signs diegetic or absent",
                "approach, focal pressure object, player action/body position, and exit/aftertaste");
    }

    private String nextLaunchMissingId() {
        if (plugin.sites() == null) return LAUNCH_REQUIRED_SITES[0];
        for (String id : LAUNCH_REQUIRED_SITES) {
            if (launchSiteIssue(id) != null) return id;
        }
        return null;
    }

    private String nextLaunchMissingId(PlacementLane lane) {
        if (lane == null) return nextLaunchMissingId();
        if (plugin.sites() == null) return lane.sites().length > 0 ? lane.sites()[0] : null;
        for (String id : lane.sites()) {
            if (launchSiteIssue(id) != null) return id;
        }
        return null;
    }

    private static PlacementLane placementLane(String raw) {
        if (raw == null) return null;
        String want = raw.trim().toLowerCase(Locale.ROOT);
        if (want.isBlank()) return null;
        for (PlacementLane lane : PLACEMENT_LANES) {
            if (lane.id().equals(want)) return lane;
        }
        return null;
    }

    private static String placementLaneId(String siteId) {
        if (siteId == null) return "";
        for (PlacementLane lane : PLACEMENT_LANES) {
            for (String id : lane.sites()) {
                if (id.equals(siteId)) return lane.id();
            }
        }
        return "";
    }

    private String launchSiteIssue(String id) {
        if (plugin.sites() == null) return id + ": sites config unavailable";
        Site site = plugin.sites().get(id);
        if (site == null) return id + ": missing from sites.yml";
        if (!site.enabled()) return id + ": disabled";
        if (!site.isPlaced()) {
            return isDeepHoldLaunchSite(id) && !isDeepHoldBuilt()
                    ? id + ": Deep Hold not built (/obs placehold build)"
                    : id + ": unplaced";
        }
        Location loc = site.location();
        if (loc == null || loc.getWorld() == null) return id + ": world not loaded (" + site.worldName() + ")";
        if (requiresPlaceWorldStamp(id) && !placeWorldStampPresent(id, site)) {
            return id + ": surveyed; needs /obs placeworld stamp";
        }
        return null;
    }

    private static boolean requiresPlaceWorldStamp(String siteId) {
        return keeperRow(siteId) != null || isPlaceWorldSurveyFixture(siteId);
    }

    private boolean placeWorldStampPresent(String siteId, Site site) {
        if (site == null || site.location() == null || site.location().getWorld() == null) return false;
        Location loc = site.location();
        org.bukkit.World world = loc.getWorld();
        int ax = loc.getBlockX();
        int az = loc.getBlockZ();
        int ay;
        try {
            ay = world.getHighestBlockYAt(ax, az, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        } catch (Throwable ignored) {
            ay = loc.getBlockY();
        }
        if (looksAlreadyPlaced(world, ax, ay, az) || looksAlreadyPlaced(world, ax, loc.getBlockY(), az)) {
            return true;
        }
        return hasPlaceWorldStampMaterials(world, ax, ay, az, siteId, site.type())
                || hasPlaceWorldStampMaterials(world, ax, loc.getBlockY(), az, siteId, site.type());
    }

    private static boolean hasPlaceWorldStampMaterials(org.bukkit.World world, int x, int y, int z,
                                                       String siteId, String siteType) {
        if (world == null) return false;
        try {
            Material center = world.getBlockAt(x, y, z).getType();
            String type = siteType == null ? "" : siteType;
            if (type.equals("report_lectern") || type.equals("mara_lectern")) {
                return center == Material.LECTERN;
            }
            if (type.equals("kept_light")) {
                return center == Material.CAMPFIRE || center == Material.SOUL_CAMPFIRE;
            }
            if (type.equals("vaun_bookshelf")) {
                return center == Material.CHISELED_BOOKSHELF;
            }
            if (type.equals("answer_sign") && isSignMaterial(center)) {
                return true;
            }

            int deliberate = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        Material m = world.getBlockAt(x + dx, y + dy, z + dz).getType();
                        if (isPlaceWorldStampMaterial(m)) {
                            deliberate++;
                            if (deliberate >= 4) return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static boolean isSignMaterial(Material material) {
        return material != null && material.name().endsWith("_SIGN");
    }

    private static boolean isPlaceWorldStampMaterial(Material material) {
        if (material == null || material.isAir()) return false;
        return switch (material) {
            case COBBLED_DEEPSLATE, POLISHED_DEEPSLATE, CHISELED_DEEPSLATE, DEEPSLATE_BRICKS,
                 CRACKED_DEEPSLATE_BRICKS, DEEPSLATE_TILES, TUFF_BRICKS, CHISELED_TUFF,
                 POLISHED_BLACKSTONE, POLISHED_BLACKSTONE_BRICKS, CRACKED_POLISHED_BLACKSTONE_BRICKS,
                 CHISELED_POLISHED_BLACKSTONE, CHISELED_STONE_BRICKS, BLACKSTONE, BASALT,
                 POLISHED_BASALT, SMOOTH_BASALT, REINFORCED_DEEPSLATE, END_STONE_BRICKS,
                 PURPUR_BLOCK, PURPUR_PILLAR, OBSIDIAN, DARK_PRISMARINE, PRISMARINE_BRICKS,
                 SEA_LANTERN, LECTERN, CHISELED_BOOKSHELF, BARREL, CAMPFIRE, SOUL_CAMPFIRE,
                 SOUL_LANTERN, LANTERN, SCULK, SCULK_SENSOR, SCULK_SHRIEKER -> true;
            default -> isSignMaterial(material);
        };
    }

    private void sendLaunchRemaining(CommandSender sender) {
        if (plugin.sites() == null) return;
        int remaining = 0;
        for (String id : LAUNCH_REQUIRED_SITES) {
            if (launchSiteIssue(id) != null) remaining++;
        }
        sender.sendMessage("  Launch proof rows remaining: " + remaining + "/" + LAUNCH_REQUIRED_SITES.length
                + (remaining == 0 ? ". Run /obs preflight." : ". Next: /obs placehold audit or /obs site next."));
    }

    /**
     * {@code /observance placeworld} — the REAL world-placement path (Wave W2). Places each keeper set-piece
     * ({@link #KEEPER_SPINE}) at its SURVEYED anchor (from {@code sites.yml}, set via {@code site set}), each
     * terrain-followed (seated on the surface via the OCEAN_FLOOR heightmap) and reveal-safe — NOT clustered.
     *
     * <p>For any keeper WITHOUT a surveyed anchor, it AUTO-SCATTERS: it derives a distant anchor far from the
     * group's base ({@link com.observance.watcher.signal.BaseDetector#primaryBase()}, else world spawn) at a
     * large per-keeper radius (hundreds–thousands of blocks) and its own bearing, so the sites do not line up
     * or read as a cluster. The scatter is deterministic (seeded from the base cell + the keeper id via the
     * Wang bit-mixer), so a re-run picks the SAME auto anchor — idempotent: same anchor -> same coords, and a
     * site already placed at those coords is not double-placed (footprint occupancy sweep on re-seat).
     *
     * <p>Reuses the existing structure beats ({@link StructureTemplates#keeper}) and terrain-seat + reveal
     * guards (command-placed, never toward an approaching player). Reports per-keeper: surveyed vs auto,
     * coords, and placed/occupied/witnessed. Player-only (needs a world to resolve spawn as the fallback base).
     */
    private void handlePlaceWorld(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placeworld must be run by a player (needs a world).");
            return;
        }
        org.bukkit.World world = player.getWorld();
        if (world == null) {
            sender.sendMessage("Observance: could not resolve your world.");
            return;
        }
        String worldName = world.getName();

        // Resolve the base cell we scatter AWAY from: prefer the live detected base, else world spawn.
        int baseX, baseZ;
        String baseSource;
        int[] base = resolveScatterBase(world);
        baseX = base[0]; baseZ = base[1];
        baseSource = base[2] == 1 ? "detected base" : "world spawn";

        int surveyed = 0, auto = 0, placed = 0, occupied = 0, failed = 0, skippedLanes = 0;
        sender.sendMessage("== placeworld — scattering keepers away from " + baseSource
                + " " + baseX + "," + baseZ + " in " + worldName + " ==");

        for (String[] row : KEEPER_SPINE) {
            String siteId   = row[0];
            String siteType = row[1];
            int radius;
            try { radius = Integer.parseInt(row[2]); } catch (NumberFormatException e) { radius = 8; }

            // 1) Prefer a surveyed anchor (real coords already in sites.yml for this id).
            Site existing = plugin.sites() == null ? null : plugin.sites().get(siteId);
            int ax, az;
            boolean fromSurvey;
            if (existing != null && existing.isPlaced() && existing.location() != null
                    && worldName.equals(existing.worldName())) {
                Location el = existing.location();
                ax = el.getBlockX();
                az = el.getBlockZ();
                fromSurvey = true;
                surveyed++;
            } else if (isLaneSite(siteId)) {
                // 2a) CROSS-DIMENSION LANE (nether_forge / end_seventh_shrine): never auto-scatter — that would
                // drop a Nether/End set-piece into the overworld. It is placed ONLY at a surveyed anchor in its
                // OWN dimension. Unsurveyed here (or surveyed in a different world) → SKIP (sites.yml's "silently
                // skip an unplaced site" rule). Survey it in-dimension, then run placeworld FROM that dimension.
                skippedLanes++;
                boolean surveyedElsewhere = existing != null && existing.isPlaced();
                sender.sendMessage("  " + siteId + ": cross-dimension lane -> skipped ("
                        + (surveyedElsewhere
                            ? "surveyed in " + existing.worldName() + ", not " + worldName
                            : "not surveyed") + "). Stand in the "
                        + (siteId.startsWith("nether") ? "Nether" : "End")
                        + ", run `/observance site set " + siteId + "`, then placeworld there.");
                continue;
            } else {
                // 2b) Auto-scatter: a distant, per-keeper anchor far from the base on its own bearing.
                int[] scatter = autoScatterAnchor(baseX, baseZ, siteId);
                ax = scatter[0];
                az = scatter[1];
                fromSurvey = false;
                auto++;
            }

            // Terrain-seat: highest solid block (OCEAN_FLOOR skips water + leaves), + 1 — same seat logic the
            // dev placers use, so the set-piece sits ON the surface rather than floating or buried.
            int ay = world.getHighestBlockYAt(ax, az, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
            Location anchor = new Location(world, ax, ay, az);

            // Idempotent occupancy sweep: if a set-piece is already stamped here (re-run at the same anchor),
            // skip the re-place so we never double-stamp. We detect it by the set-piece's signature block at
            // the pillar base column already matching a non-natural placed block.
            if (looksAlreadyPlaced(world, ax, ay, az)) {
                occupied++;
                sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "surveyed" : "auto")
                        + " @ " + ax + "," + ay + "," + az + " -> occupied (already placed; skipped).");
                // Still (re)register the site so its coords are authoritative in sites.yml.
                plugin.registerRuntimeSite(new Site(siteId, siteType, worldName,
                        (double) ax, (double) ay, (double) az, radius, 6, true, true, null, false));
                markDimensionLanePlaced(siteId, sender);
                continue;
            }

            // Build the distinct per-keeper set-piece (reuses the existing beats — no new generator).
            Location signLoc = StructureTemplates.keeper(siteId, anchor);
            if (signLoc == null) {
                failed++;
                sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "surveyed" : "auto")
                        + " @ " + ax + "," + ay + "," + az + " -> FAILED (chunk unavailable).");
                continue;
            }

            plugin.registerRuntimeSite(new Site(siteId, siteType, worldName,
                    (double) ax, (double) ay, (double) az, radius, 6, true, true, null, false));
            markDimensionLanePlaced(siteId, sender);
            placed++;
            sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "SURVEYED" : "auto-scatter")
                    + " @ " + ax + "," + ay + "," + az + " -> placed.");
        }

        sender.sendMessage("Observance: placeworld complete — " + placed + " placed, " + occupied
                + " occupied, " + failed + " failed, " + skippedLanes + " lane(s) skipped ("
                + surveyed + " surveyed / " + auto + " auto-scattered) of "
                + KEEPER_SPINE.length + " keepers.");
        int fixturePlaced = placeSurveyedFixtures(world, worldName, sender);
        if (fixturePlaced > 0) {
            sender.sendMessage("  Surveyed fixtures stamped: " + fixturePlaced + "/" + PLACEWORLD_SURVEY_FIXTURES.length + ".");
        }
        sender.sendMessage("  Scatter is deterministic (same base = same auto anchors). Re-run is idempotent. "
                + "Survey a spot with /observance site set <keeperId> to override an auto anchor.");
        if (skippedLanes > 0) {
            sender.sendMessage("  Nether/End lanes are survey-only: stand IN the Nether/End, `site set` the lane, "
                    + "then run placeworld FROM that dimension to stamp it there.");
        }
    }

    private void markDimensionLanePlaced(String siteId, CommandSender sender) {
        String flag = dimensionLanePlacementFlag(siteId);
        if (flag == null) return;
        var sb = plugin.supabase();
        if (sb == null) {
            sender.sendMessage("  " + siteId + ": placement flag " + flag
                    + " not recorded (Supabase unavailable).");
            return;
        }
        JsonObject flags = new JsonObject();
        flags.addProperty(flag, true);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var result = sb.mergeArcFlags(flags);
            String status = result.ok()
                    ? "recorded"
                    : ("queued-offline".equals(result.error())
                        ? "queued until Supabase is configured/reachable"
                        : "not recorded (" + result.error() + ")");
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage("  " + siteId + ": " + flag + " " + status + "."));
        });
    }

    private int placeSurveyedFixtures(org.bukkit.World world, String worldName, CommandSender sender) {
        if (world == null || plugin.sites() == null) return 0;
        int placed = 0;
        int skipped = 0;
        for (String siteId : PLACEWORLD_SURVEY_FIXTURES) {
            Site cfg = plugin.sites().get(siteId);
            if (cfg == null || !cfg.enabled() || !cfg.isPlaced() || cfg.location() == null) {
                skipped++;
                continue;
            }
            if (!worldName.equals(cfg.worldName())) {
                skipped++;
                continue;
            }

            Location configured = cfg.location();
            int ax = configured.getBlockX();
            int az = configured.getBlockZ();
            int ay = world.getHighestBlockYAt(ax, az, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
            Location base = new Location(world, ax, ay, az);
            try {
                buildLabFixture(cfg, base);
                Site live = new Site(cfg.id(), cfg.type(), worldName,
                        (double) ax, (double) ay, (double) az,
                        cfg.radius(), cfg.verticalRadius(), cfg.protect(), true,
                        cfg.puzzleKey(), false);
                plugin.registerRuntimeSite(live);
                repairPlacedSite(live, base);
                placed++;
                if (sender != null) {
                    sender.sendMessage("  " + siteId + ": surveyed fixture -> stamped @ " + ax + "," + ay + "," + az + ".");
                }
            } catch (Throwable t) {
                skipped++;
                if (sender != null) {
                    sender.sendMessage("  " + siteId + ": surveyed fixture -> FAILED (" + t.getClass().getSimpleName() + ").");
                }
            }
        }
        if (sender != null && placed == 0 && skipped > 0) {
            sender.sendMessage("  Surveyed fixtures: none stamped in this world yet.");
        }
        return placed;
    }

    /**
     * Resolve the cell keepers scatter AWAY from. Prefers the live detected group base
     * ({@link com.observance.watcher.signal.BaseDetector#primaryBase()}); falls back to the world spawn.
     * Returns {@code [x, z, sourceFlag]} where {@code sourceFlag} is 1 for a detected base, 0 for spawn.
     * Null/quirk-safe: any failure falls through to spawn.
     */
    private int[] resolveScatterBase(org.bukkit.World world) {
        try {
            com.observance.watcher.signal.SignalTracker tracker = plugin.signalTracker();
            if (tracker != null && tracker.baseDetector() != null) {
                com.observance.watcher.signal.BaseDetector.Anchor a = tracker.baseDetector().primaryBase();
                if (a != null && world.getName().equals(a.world)) {
                    return new int[]{ a.x, a.z, 1 };
                }
            }
        } catch (Throwable ignored) { /* fall through to spawn */ }
        Location spawn = world.getSpawnLocation();
        return new int[]{ spawn.getBlockX(), spawn.getBlockZ(), 0 };
    }

    /**
     * Derive a distant, per-keeper auto-scatter anchor around the base cell. Each keeper gets its OWN
     * radius (hundreds–thousands of blocks) and its OWN bearing, both deterministically from the keeper id
     * mixed with the base cell (Wang bit-mixer — no {@code Math.random()}, no wall-clock), so the sites are
     * spread across the map at varied distances and directions (never a line, never a cluster) and a re-run
     * reproduces the SAME anchor (idempotent). Returns {@code [x, z]} block coords.
     */
    private static int[] autoScatterAnchor(int baseX, int baseZ, String keeperId) {
        long seed = wangHash(((long) baseX * 73856093L) ^ ((long) baseZ * 19349663L) ^ idHash(keeperId));
        // Radius in [512 .. 512+2560] = 512..3072 blocks — a large, varied ring per keeper.
        int radius = 512 + (int) ((seed >>> 8) & 0x7FFL) * 2560 / 0x7FF;
        // Bearing: full 0..2π, from independent bits of the seed.
        double bearing = (((seed >>> 20) & 0xFFFFL) / (double) 0x10000) * (Math.PI * 2.0);
        int x = baseX + (int) Math.round(Math.cos(bearing) * radius);
        int z = baseZ + (int) Math.round(Math.sin(bearing) * radius);
        return new int[]{ x, z };
    }

    /** Deterministic 64-bit hash of a keeper id (used to seed its scatter). */
    private static long idHash(String s) {
        long h = 1125899906842597L; // prime
        if (s != null) for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return wangHash(h);
    }

    /**
     * Cheap idempotency probe: has a keeper set-piece already been stamped at this anchor column? Every
     * keeper builder seats a non-natural, deliberately-placed block at (or just below) the pillar base, so a
     * re-run over an already-built site sees deepslate/blackstone/brick family blocks here rather than the
     * natural surface. We treat those as "already placed" and skip the re-stamp. Conservative + null-safe:
     * on any doubt it returns false (allow the place) so we never wrongly skip a genuine first placement.
     */
    private static boolean looksAlreadyPlaced(org.bukkit.World world, int x, int y, int z) {
        try {
            if (world == null) return false;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
            // Sample the pillar base + one below (builders carve a hollow floor / plinth here).
            for (int dy = 0; dy >= -1; dy--) {
                Material m = world.getBlockAt(x, y + dy, z).getType();
                switch (m) {
                    case COBBLED_DEEPSLATE, POLISHED_DEEPSLATE, CHISELED_DEEPSLATE, DEEPSLATE_BRICKS,
                         DEEPSLATE_TILES, TUFF_BRICKS, POLISHED_BLACKSTONE, POLISHED_BLACKSTONE_BRICKS,
                         CHISELED_POLISHED_BLACKSTONE, CHISELED_STONE_BRICKS -> { return true; }
                    default -> { /* keep sampling */ }
                }
            }
        } catch (Throwable ignored) { /* treat as not-placed on any error */ }
        return false;
    }

    /**
     * {@code /observance placeroom <keeperId>} — stamps a code-templated keeper stone at the sender's
     * location and registers the spot as a live {@code keeper_stone} answer-submission site so
     * {@code AnswerSignListener} resolves answers typed on the sign. Player-only (needs a world location).
     * The site is persisted to {@code plugins/Observance/sites.yml} so it survives a reload or restart.
     */
    private void handlePlaceRoom(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placeroom must be run by a player (needs a location).");
            return;
        }
        if (args.length < 2 || args[1].isBlank()) {
            sender.sendMessage("Usage: /observance placeroom <keeperId>");
            return;
        }
        // Site id: sanitize to a safe slug so it maps cleanly to a sites.yml-style key.
        String keeperId = args[1].trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        String siteId = "keeper_" + keeperId;

        Location loc = player.getLocation();
        if (loc == null || loc.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }

        // Build the distinct per-keeper set-piece at the sender's feet. Dispatches by keeperId; returns
        // the diegetic answer surface (recessed sign / lectern) location.
        Location signLoc = StructureTemplates.keeper(keeperId, loc);
        if (signLoc == null) {
            sender.sendMessage("Observance: could not place structure (world/chunk unavailable here).");
            return;
        }

        // Register the pillar's base as a live keeper_stone site. The site radius (config defaults) comfortably
        // covers the sign a few blocks above, so the answer sign resolves. In-memory only (see registerRuntimeSite).
        String world = loc.getWorld().getName();
        Site site = new Site(siteId, "keeper_stone", world,
                (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                6, 6, false, true, null, false);
        plugin.registerRuntimeSite(site);

        sender.sendMessage("Observance: placed keeper stone '" + siteId + "' at "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " in " + world + ".");
        sender.sendMessage("  Answer sign is live. Site persisted to sites.yml (survives reload/restart).");
    }

    /**
     * {@code /observance placeregion} — stamps the ENTIRE starter spine in one command: the
     * {@code rune_rosetta} structure stone + the six keeper stones (vaun, mara, sella, orin, brann,
     * iss). Per WORLD-BUILD §4 ("a FIELD not a row"; "one vertical descent; the descent IS the dread")
     * the sites are NOT stamped in a straight east–west line at one Y. Instead they follow a
     * deterministic BRANCHING DESCENT: each subsequent site steps {@code spacing} blocks east, offset
     * north/south by an alternating zig-zag so the eye reads a branching field, and a few blocks LOWER
     * than the last so the whole spine sinks into the ground as you walk it. The rosetta (index 0) sits
     * at the mouth; each keeper descends past it.
     *
     * <p>Each structure is placed with {@link com.observance.watcher.structure.StructureTemplates#keeper}
     * (each keeper gets its own distinct set-piece), registered live via {@code registerRuntimeSite},
     * and persisted to {@code sites.yml}. Player-only. Run from where you want the MOUTH of the descent
     * to be — the field extends east + down from a little west of the sender.
     *
     * <p>Usage: {@code /observance placeregion [spacing]} (spacing defaults to 12 blocks; min 9 so the
     * dense per-keeper set-pieces don't overlap). Reveal-safe (command-placed, never toward a player).
     */
    private void handlePlaceRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placeregion must be run by a player (needs a location).");
            return;
        }

        // Set-pieces are dense (~5x5 to 7x7 footprints), so centres must sit far enough apart that
        // adjacent builds don't stamp over each other. Default 12; floor 9 to clear the 7x7 rosetta.
        int spacing = 12; // default gap between set-piece centres
        if (args.length >= 2) {
            try {
                spacing = Math.max(9, Math.min(24, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        Location centre = player.getLocation();
        if (centre == null || centre.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        String world = centre.getWorld().getName();

        // The spine order: rosetta first (index 0), then the six keepers (indices 1-6).
        // Arranged in a straight east–west row centred on the sender.
        // Each element: [siteId, siteType] — the type controls how the engine treats the site.
        String[][] spine = {
            { "rune_rosetta",  "structure"    },   // literacy gate / IgnitionListener anchor
            { "stone_vaun",    "keeper_stone" },
            { "stone_mara",    "keeper_stone" },
            { "stone_sella",   "keeper_stone" },
            { "stone_orin",    "keeper_stone" },
            { "stone_brann",   "keeper_stone" },
            { "stone_iss",     "keeper_stone" },
        };

        int count = spine.length;
        // WAVE R6 — NON-UNIFORM SCATTER (replaces the east-march corridor).
        //
        // Old behaviour: constant east step (spacing), alternating ±4-block N/S wobble → a straight
        // corridor that read as "walk these in order."
        //
        // New behaviour: sites feel independently-placed, as if they were always-there in the world.
        // Three levers, ALL deterministic (seeded from siteIndex + origin coords, NOT Math.random()):
        //
        //   1. NON-UNIFORM X intervals — base step + a per-site delta from siteHash, so the east gaps
        //      are irregular (half-step … full-step … one-and-a-half-step). Sites are never too close
        //      (cumulative east advance always ≥ i * minStep so the field still spreads east overall).
        //
        //   2. LARGE IRREGULAR Z spread — ±zRange (= ~60 % of spacing), per-site from hash. This is
        //      ~6–10× the old ±4-block wobble. Sites land at clearly different N/S positions instead
        //      of hugging the centre-line.
        //
        //   3. MINIMUM PAIRWISE SEPARATION — after each candidate position is chosen, we check every
        //      already-placed site. If the XZ distance is < minSep (= 9 blocks = worst-case footprint),
        //      we nudge the candidate N or S by minSep until it clears. This guarantees no two
        //      set-pieces overlap regardless of the hash values produced.
        //
        // Y follows the terrain (OCEAN_FLOOR heightmap + 1) — unchanged from before.
        // Total site count stays 7 (rosetta + 6 keepers) — unchanged.

        // Stable per-site seed: mix the site index with the block-coord origin so the same survey
        // origin always produces the same layout, but a different origin produces a different layout.
        // We use the classic Wang hash (bit-mixing, no floating-point, no wall-clock).
        long originSeed = wangHash((long) centre.getBlockX() * 73856093L ^ (long) centre.getBlockZ() * 19349663L);

        int minStep  = Math.max(8, spacing - 4);   // minimum east advance per site
        int maxDelta = Math.max(0, spacing - minStep); // extra east randomisation range
        int zRange   = Math.max(5, (int)(spacing * 0.6)); // ±lateral range (~60 % of spacing)
        int minSep   = 9;                           // minimum pairwise XZ clearance (worst footprint ≈ 7×7 + margin)

        // Accumulate already-placed XZ positions for the overlap check.
        int[] placedXs = new int[count];
        int[] placedZs = new int[count];
        int overlapCount = 0;

        double curX = centre.getX() - spacing;     // first site starts one base-step west of sender
        int placed = 0;
        for (int i = 0; i < count; i++) {
            String siteId   = spine[i][0];
            String siteType = spine[i][1];

            // Per-site hash (Wang mix of originSeed + index so each site is independent).
            long h = wangHash(originSeed ^ wangHash((long) i + 1));

            // 1. Non-uniform east advance (always positive, min minStep).
            int xDelta = (int)(h & 0x7FFFFFFFL) % (maxDelta + 1);   // 0 … maxDelta
            curX += minStep + xDelta;

            // 2. Irregular lateral (Z) spread in range [-zRange … +zRange].
            // Use the upper bits of h for the Z draw (independent of the X delta bits).
            int hZ = (int)((h >>> 32) & 0x7FFFFFFFL);
            int sz = centre.getBlockZ() + (hZ % (2 * zRange + 1)) - zRange;

            int sx = (int) Math.floor(curX);

            // 3. Minimum pairwise separation — nudge Z if needed.
            for (int k = 0; k < overlapCount; k++) {
                int dx = Math.abs(sx - placedXs[k]);
                int dz = Math.abs(sz - placedZs[k]);
                if (dx < minSep && dz < minSep) {
                    // Too close: push Z away from the conflicting site, then re-check from scratch.
                    sz = placedZs[k] + ((sz >= placedZs[k]) ? minSep : -minSep);
                    k = -1;
                }
            }

            // Terrain-seat: highest solid block (OCEAN_FLOOR skips water + leaves). Unchanged from before.
            int sy = centre.getWorld().getHighestBlockYAt(sx, sz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;

            // Record position for subsequent overlap checks.
            placedXs[overlapCount] = sx;
            placedZs[overlapCount] = sz;
            overlapCount++;

            Location pillarLoc = new Location(centre.getWorld(), sx, sy, sz);

            // Dispatch by siteId so each keeper gets its own distinct set-piece (rosetta / vaun / mara /
            // sella / orin / brann / iss). The builder strips the "stone_"/"rune_" prefix internally.
            Location signLoc = com.observance.watcher.structure.StructureTemplates.keeper(siteId, pillarLoc);
            if (signLoc == null) {
                sender.sendMessage("  [!] Could not place '" + siteId + "' — chunk unavailable at "
                        + pillarLoc.getBlockX() + "," + sy + "," + sz);
                continue;
            }

            // radius: rosetta = 6 (tighter literacy gate), keepers = 8
            int radius = siteType.equals("structure") ? 6 : 8;
            Site site = new Site(siteId, siteType, world,
                    pillarLoc.getX(), pillarLoc.getY(), pillarLoc.getZ(),
                    radius, 6, true, true, null);
            plugin.registerRuntimeSite(site); // also writes to sites.yml

            placed++;
        }

        sender.sendMessage("Observance: placeregion complete — " + placed + "/" + count
                + " spine sites placed and persisted.");
        sender.sendMessage("  Field scatter origin " + centre.getBlockX() + "," + centre.getBlockZ()
                + " in " + world + " — non-uniform east intervals, ±" + zRange + "z spread, minSep=" + minSep
                + " (spacing=" + spacing + "). Reproducible: same origin = same layout.");
        if (placed < count) {
            sender.sendMessage("  WARNING: " + (count - placed) + " site(s) skipped (chunks unloaded?).");
        }
    }

    /**
     * {@code /observance placedeep} — stamps the six DEEP-HALF set-pieces (the payoff sites the descent
     * leads to) in one command: {@code stone_of_reckoning} (the digit/sign-glyph Rosetta), {@code the_cold_hearth}
     * (Iss's false-warm dead shrine), {@code unbroken_light} (the Undercroft Accepting floor), {@code the_threshold}
     * (the future-dated grave that opens from the inside), {@code the_unwriting} (the Seventh's chamber), and
     * {@code threshold_vault} (the co-op vault room). Mirrors {@link #handlePlaceRegion}: each set-piece is built
     * with {@link StructureTemplates#keeper}, arranged as a BRANCHING DESCENT (WORLD-BUILD §4 — "a FIELD
     * not a row"; "the descent IS the dread") that sinks FURTHER and STEEPER than the region half above it:
     * each site steps east, zig-zags north/south off the centre-line, and drops several blocks lower than
     * the last so the deep half reads as the bottom of the world. Registered live via {@code registerRuntimeSite}
     * and PERSISTED to {@code sites.yml}.
     *
     * <p>Each site keeps the TYPE its listeners key off (from sites.yml): {@code unbroken_light} =
     * {@code accepting_floor} (AcceptingRiteListener), {@code the_unwriting} = {@code seventh_shrine},
     * {@code threshold_vault} = {@code coop_plate} (ThresholdVaultListener keys on the id), {@code the_threshold}
     * = {@code the_threshold}, {@code stone_of_reckoning} = {@code structure}, {@code the_cold_hearth} =
     * {@code marker}. Player-only. Reveal-safe (placed by command, never toward an approaching player).
     *
     * <p>Usage: {@code /observance placedeep [spacing]} (spacing defaults to 16 blocks — the Undercroft floor
     * is 11×11 — min 13 so the wide Accepting floor never overlaps its neighbours). Deterministic
     * (re-runnable without desync). Reveal-safe (command-placed, never toward an approaching player).
     */
    private void handlePlaceDeep(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placedeep must be run by a player (needs a location).");
            return;
        }

        // The deep set-pieces are the biggest builds in the plugin (the Accepting floor is 11×11), so centres
        // must sit far apart. Default 16; floor 13 to clear the wide floor + its lamp ring.
        int spacing = 16;
        if (args.length >= 2) {
            try {
                spacing = Math.max(13, Math.min(32, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        Location centre = player.getLocation();
        if (centre == null || centre.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        String world = centre.getWorld().getName();

        // The deep-half spine. Each element: [siteId, siteType, radius]. Types + radii mirror sites.yml so the
        // listeners that watch these types (AcceptingRite / ThresholdVault / seventh_shrine / the_threshold)
        // resolve the placed site exactly as the config would.
        String[][] deep = {
            { "stone_of_reckoning", "structure",       "6"  },   // the digit/sign-glyph Rosetta
            { "the_cold_hearth",    "marker",          "6"  },   // Iss's false-warm dead shrine
            { "unbroken_light",     "accepting_floor", "10" },   // the Undercroft Accepting floor (climax)
            { "the_threshold",      "the_threshold",   "6"  },   // the grave that opens from the inside
            { "the_unwriting",      "seventh_shrine",  "6"  },   // the Seventh's chamber (payoff)
            { "threshold_vault",    "coop_plate",      "6"  },   // the co-op vault room
        };

        int count = deep.length;
        // WAVE R6 — NON-UNIFORM SCATTER (mirrors the placeregion change; same algorithm, wider minSep).
        //
        // The deep set-pieces are the biggest builds (unbrokenLight is 11×11), so minSep is raised to
        // 12 (vs 9 for the surface) to guarantee no overlap between the wide builds. Everything else
        // is identical to placeregion: non-uniform X intervals, large irregular ±Z spread, deterministic
        // seed from origin coords + site index. Terrain-seating unchanged (OCEAN_FLOOR + 1).

        long originSeed = wangHash((long) centre.getBlockX() * 73856093L ^ (long) centre.getBlockZ() * 19349663L);
        // Offset the deep seed so the deep scatter is independent from the surface scatter even if run
        // from the same origin (avoids all six deep sites landing on top of the seven surface sites).
        originSeed = wangHash(originSeed ^ 0xDEEDFACEDEADBEEFL);

        int minStep  = Math.max(11, spacing - 5);
        int maxDelta = Math.max(0, spacing - minStep);
        int zRange   = Math.max(7, (int)(spacing * 0.65));  // slightly wider spread for the deep half
        int minSep   = 12;                                   // wider clearance for the 11×11 floor

        int[] placedXs = new int[count];
        int[] placedZs = new int[count];
        int overlapCount = 0;

        double curX = centre.getX() - spacing;
        int placed = 0;
        for (int i = 0; i < count; i++) {
            String siteId   = deep[i][0];
            String siteType = deep[i][1];
            int radius;
            try { radius = Integer.parseInt(deep[i][2]); } catch (NumberFormatException e) { radius = 8; }

            long h = wangHash(originSeed ^ wangHash((long) i + 1));

            int xDelta = (int)(h & 0x7FFFFFFFL) % (maxDelta + 1);
            curX += minStep + xDelta;

            int hZ = (int)((h >>> 32) & 0x7FFFFFFFL);
            int sz = centre.getBlockZ() + (hZ % (2 * zRange + 1)) - zRange;

            int sx = (int) Math.floor(curX);

            // Minimum pairwise separation (same nudge logic as placeregion).
            for (int k = 0; k < overlapCount; k++) {
                int dx = Math.abs(sx - placedXs[k]);
                int dz = Math.abs(sz - placedZs[k]);
                if (dx < minSep && dz < minSep) {
                    sz = placedZs[k] + ((sz >= placedZs[k]) ? minSep : -minSep);
                    k = -1;
                }
            }

            int sy = centre.getWorld().getHighestBlockYAt(sx, sz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;

            placedXs[overlapCount] = sx;
            placedZs[overlapCount] = sz;
            overlapCount++;

            Location siteLoc = new Location(centre.getWorld(), sx, sy, sz);

            // Dispatch by siteId so each deep site gets its own distinct set-piece. The builder strips the
            // "the_"/"stone_of_" prefixes internally.
            Location signLoc = StructureTemplates.keeper(siteId, siteLoc);
            if (signLoc == null) {
                sender.sendMessage("  [!] Could not place '" + siteId + "' — chunk unavailable at "
                        + siteLoc.getBlockX() + "," + sy + "," + sz);
                continue;
            }

            Site site = new Site(siteId, siteType, world,
                    siteLoc.getX(), siteLoc.getY(), siteLoc.getZ(),
                    radius, 6, true, true, null, false);
            plugin.registerRuntimeSite(site); // also writes to sites.yml

            placed++;
        }

        sender.sendMessage("Observance: placedeep complete — " + placed + "/" + count
                + " deep-half set-pieces placed and persisted.");
        sender.sendMessage("  Deep scatter origin " + centre.getBlockX() + "," + centre.getBlockZ()
                + " in " + world + " — non-uniform east intervals, ±" + zRange + "z spread, minSep=" + minSep
                + " (spacing=" + spacing + "). Reproducible: same origin = same layout.");
        if (placed < count) {
            sender.sendMessage("  WARNING: " + (count - placed) + " site(s) skipped (chunks unloaded?).");
        }
    }

    /**
     * {@code /observance placelecterns [spacing]} - stamps the five Mara page-lock lecterns as a
     * compact, readable test rig. This is the real-world counterpart to the lab fixture: it creates
     * actual lectern blocks, writes turnable books into them, registers the five mara_lectern sites,
     * and persists those coordinates to sites.yml so LecternLockListener can read the open pages.
     */
    private void handlePlaceLecterns(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placelecterns must be run by a player (needs a location).");
            return;
        }
        Location centre = player.getLocation();
        if (centre == null || centre.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }

        int spacing = 3;
        if (args.length >= 2) {
            try {
                spacing = Math.max(3, Math.min(8, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        int placed = placeMaraLecternsAt(centre, spacing);

        sender.sendMessage("Observance: placed " + placed + "/5 Mara page-lock lecterns with books.");
        sender.sendMessage("  Test pages: lecterns 1-5 should be turned to 1, 2, 4, 4, 6.");
        sender.sendMessage("  Sites were persisted to sites.yml; re-run /observance reload after server restart.");
    }

    private int placeMaraLecternsAt(Location centre, int spacing) {
        if (centre == null || centre.getWorld() == null) return 0;
        org.bukkit.World world = centre.getWorld();
        String worldName = world.getName();
        int[] markedPages = {1, 2, 4, 4, 6};
        int startX = centre.getBlockX() - (2 * spacing);
        int z = centre.getBlockZ();
        int placed = 0;

        for (int i = 1; i <= 5; i++) {
            int x = startX + ((i - 1) * spacing);
            int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
            Block b = world.getBlockAt(x, y, z);
            placeReadableLectern(b, BlockFace.SOUTH);
            fillMaraLockBook(b, i, markedPages[i - 1]);

            Site site = new Site("mara_lectern_" + i, "mara_lectern", worldName,
                    (double) x, (double) y, (double) z, 2, 2, true, true, null);
            plugin.registerRuntimeSite(site);
            placed++;
        }
        return placed;
    }

    /**
     * {@code /observance placehold} - production placement for the underground Deep Hold. This is not the
     * rehearsal board; it carves a walkable megastructure, registers the normal site ids, and adds physical
     * gates that can be opened manually or synced from live story flags.
     */
    private void handlePlaceHold(CommandSender sender, String[] args) {
        String action = args.length >= 2 ? args[1].trim().toLowerCase(Locale.ROOT) : "build";
        switch (action) {
            case "build" -> {
                HoldBuildAnchor anchor = resolvePlaceHoldAnchor(sender, args);
                if (anchor == null || anchor.base() == null) return;
                Location base = anchor.base();
                sender.sendMessage("== Observance Deep Hold ==");
                sender.sendMessage("Building production underground hold at "
                        + base.getWorld().getName() + " "
                        + base.getBlockX() + "," + base.getBlockY() + "," + base.getBlockZ() + ".");
                if (anchor.surfaceMouth() != null) {
                    Location mouth = anchor.surfaceMouth();
                    sender.sendMessage("  Surface mouth: " + mouth.getBlockX() + ","
                            + mouth.getBlockY() + "," + mouth.getBlockZ()
                            + " (the court anchor is offset underground from that entrance).");
                }
                int placed = buildDeepHold(base, anchor.surfaceMouth(), sender);
                sender.sendMessage("Observance: Deep Hold build complete - " + placed + "/"
                        + DEEP_HOLD_SITES.length + " ARG sites registered inside the hold.");
                sender.sendMessage("  Initial gates: entry + keeper open; archive, deep, dread, threshold, accepting sealed.");
                sender.sendMessage("  First report / first marker remain prologue setup, not production Hold rooms.");
                sender.sendMessage("  Next: /obs placehold audit, then /obs placehold sync once Supabase flags are live.");
            }
            case "audit", "status" -> handlePlaceHoldAudit(sender);
            case "seal", "open" -> {
                String gate = args.length >= 3 ? args[2].trim().toLowerCase(Locale.ROOT) : "all";
                boolean sealed = action.equals("seal");
                int changed = applyHoldGateSelection(sender, gate, sealed);
                sender.sendMessage("Observance: Deep Hold " + (sealed ? "sealed " : "opened ")
                        + changed + " gate(s).");
            }
            case "sync" -> syncPlaceHoldGates(sender);
            default -> sender.sendMessage("Usage: /observance placehold <build|audit|seal|open|sync> "
                    + "[gate]  (player build: [depth]; console build: <world> <x> <y> <z>)");
        }
    }

    private HoldBuildAnchor resolvePlaceHoldAnchor(CommandSender sender, String[] args) {
        if (args.length >= 6) {
            World world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage("Observance: unknown world '" + args[2] + "'.");
                return null;
            }
            Integer x = parseHoldInt(args[3]);
            Integer y = parseHoldInt(args[4]);
            Integer z = parseHoldInt(args[5]);
            if (x == null || y == null || z == null) {
                sender.sendMessage("Usage: /observance placehold build <world> <x> <y> <z>");
                return null;
            }
            return new HoldBuildAnchor(new Location(world, x, clampHoldY(world, y), z), null);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: console build needs /observance placehold build <world> <x> <y> <z>.");
            return null;
        }
        Location here = player.getLocation();
        if (here == null || here.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return null;
        }
        int depth = 392;
        if (args.length >= 3) {
            Integer parsed = parseHoldInt(args[2]);
            if (parsed != null) depth = Math.max(340, Math.min(520, parsed));
        }
        World world = here.getWorld();
        int y = clampHoldY(world, here.getBlockY() - depth);
        Location mouth = new Location(world, here.getBlockX(), here.getBlockY(), here.getBlockZ());
        Location base = new Location(world, here.getBlockX(), y, here.getBlockZ() + depth + 360);
        return new HoldBuildAnchor(base, mouth);
    }

    private Integer parseHoldInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int clampHoldY(World world, int y) {
        if (world == null) return y;
        int min = world.getMinHeight() + 20;
        int max = world.getMaxHeight() - 64;
        if (max < min) return Math.max(world.getMinHeight() + 8, y);
        return Math.max(min, Math.min(max, y));
    }

    private int buildDeepHold(Location base, Location surfaceMouth, CommandSender sender) {
        if (base == null || base.getWorld() == null) return 0;
        World world = base.getWorld();
        String worldName = world.getName();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        plugin.beginRuntimeSiteBatch();
        try {
        loadHoldChunks(world, bx, bz);
        if (sender != null) sender.sendMessage("  sealing surrounding caves/water into controlled stone...");
        buildHoldGeologyEnvelope(world, bx, by, bz);
        if (sender != null) sender.sendMessage("  carving civic caverns, galleries, and gated districts...");
        buildHoldSpine(world, bx, by, bz);
        if (surfaceMouth != null) {
            buildHoldSurfaceMouth(surfaceMouth, base);
        }
        placeHoldPrologueEcho(world, bx, by, bz);
        registerHoldRegion(worldName, bx, by, bz, surfaceMouth);

        int placed = 0;
        int step = 0;
        for (HoldSite row : DEEP_HOLD_SITES) {
            step++;
            Location loc = new Location(world, bx + row.x(), by + row.y(), bz + row.z());
            try {
                loc.getChunk().load(true);
                boolean framed = needsHoldDedicatedRoom(row);
                boolean nativeChamber = isHoldNativeChamber(row);
                boolean templateSite = isTemplateLabSite(row.id());
                if (framed) {
                    buildHoldRoomShell(world, loc, row);
                }
                Site live = configuredHoldSite(row, loc, worldName);
                placeHoldFixture(live, loc, row);
                if (framed && templateSite && !nativeChamber) {
                    buildHoldTemplateAlcoveFrame(world, loc, row);
                }
                if (framed) {
                    if (row.x() == 0) {
                        connectHoldCentralRoom(world, bx, by, bz, row);
                    } else {
                        connectHoldRoom(world, bx, by, bz, row);
                    }
                }
                ensureHoldAnchorVisual(live, loc);
                plugin.registerRuntimeSite(live);
                placed++;
                if (sender != null && (step % 12 == 0 || step == DEEP_HOLD_SITES.length)) {
                    sender.sendMessage("  placed " + placed + "/" + DEEP_HOLD_SITES.length + " hold sites...");
                }
            } catch (Throwable t) {
                if (sender != null) {
                    sender.sendMessage("  [!] Deep Hold skipped " + row.id() + " ("
                            + t.getClass().getSimpleName() + ").");
                }
            }
        }

        registerHoldFocusedAnswerSlots(worldName, bx, by, bz);

        for (HoldGate gate : DEEP_HOLD_GATES) {
            Location loc = new Location(world, bx + gate.x(), by + gate.y(), bz + gate.z());
            HoldGateSpan span = holdGateSpan(gate);
            plugin.registerRuntimeSite(new Site(holdGateSiteId(gate.id()), "hold_gate", worldName,
                    loc.getX(), loc.getY(), loc.getZ(), Math.max(8, span.halfAcross()), span.height() + 2,
                    true, true, null, false));
            setHoldGate(gate, loc, !gate.openInitially());
            placeHoldGateLabel(gate, loc);
        }
        placeHoldWayfinding(world, bx, by, bz);
        return placed;
        } finally {
            plugin.endRuntimeSiteBatch();
        }
    }

    private void registerHoldFocusedAnswerSlots(String worldName, int bx, int by, int bz) {
        if (worldName == null || worldName.isBlank()) return;
        Object[][] slots = {
                {"hold_answer_prior_absence", bx, by, bz + 28, "prior-absence"},
                {"hold_answer_prior_camp", bx, by, bz + 105, "prior-camp-refusal"},
                {"hold_answer_prior_vaun", bx - 12, by, bz + 114, "prior-vaun-correction"},
                {"hold_answer_prior_mara", bx - 10, by, bz + 117, "prior-mara-correction"},
                {"hold_answer_prior_sella", bx - 4, by, bz + 118, "prior-sella-correction"},
                {"hold_answer_prior_orin", bx + 4, by, bz + 118, "prior-orin-correction"},
                {"hold_answer_prior_brann", bx + 10, by, bz + 117, "prior-brann-correction"},
                {"hold_answer_prior_iss", bx + 12, by, bz + 114, "prior-iss-correction"},
                {"hold_answer_witness", bx + 3, by - 28, bz + 425, "prior-witness-before-accepting"}
        };
        for (Object[] slot : slots) {
            plugin.registerRuntimeSite(new Site((String) slot[0], "answer_sign", worldName,
                    ((Number) slot[1]).doubleValue(), ((Number) slot[2]).doubleValue(),
                    ((Number) slot[3]).doubleValue(), 1, 2, true, true, (String) slot[4], false));
        }
    }

    private void loadHoldChunks(World world, int bx, int bz) {
        if (world == null) return;
        world.getChunkAt(bx >> 4, bz >> 4).load(true);
        for (HoldSite row : DEEP_HOLD_SITES) {
            world.getChunkAt((bx + row.x()) >> 4, (bz + row.z()) >> 4).load(true);
        }
        for (HoldGate gate : DEEP_HOLD_GATES) {
            world.getChunkAt((bx + gate.x()) >> 4, (bz + gate.z()) >> 4).load(true);
        }
    }

    private Site configuredHoldSite(HoldSite row, Location loc, String worldName) {
        Site cfg = plugin.sites() == null ? null : plugin.sites().get(row.id());
        String type = cfg == null ? row.type() : cfg.type();
        int radius = cfg == null ? row.radius() : cfg.radius();
        int vertical = cfg == null ? row.vertical() : cfg.verticalRadius();
        boolean protect = cfg == null || cfg.protect();
        String puzzleKey = cfg == null ? null : cfg.puzzleKey();
        return new Site(row.id(), type, worldName,
                loc.getX(), loc.getY(), loc.getZ(), radius, vertical, protect, true, puzzleKey, false);
    }

    private void placeHoldFixture(Site site, Location loc, HoldSite row) {
        if (site == null || loc == null || loc.getWorld() == null) return;
        String id = site.id();
        String type = site.type();
        shapeHoldFixtureSetting(site, loc, row);
        if ("first_marker_01".equals(id)) {
            placeMarker(loc, Material.CHISELED_STONE_BRICKS, Material.CANDLE, true);
        } else if ("mara_lectern".equals(type)) {
            Block b = loc.getBlock();
            placeReadableLectern(b, BlockFace.SOUTH);
            int index = trailingIndex(id, 1);
            int[] markedPages = {1, 2, 4, 4, 6};
            int marked = markedPages[Math.max(0, Math.min(markedPages.length - 1, index - 1))];
            fillMaraLockBook(b, index, marked);
        } else if ("sella_lectern".equals(type)) {
            Block b = loc.getBlock();
            placeReadableLectern(b, BlockFace.NORTH);
            int index = trailingIndex(id, 1);
            int[] ringPages = {2, 3, 5, 7, 11};
            int marked = ringPages[Math.max(0, Math.min(ringPages.length - 1, index - 1))];
            fillSellaLockBook(b, index, marked);
        } else if (buildHoldIntegratedFixture(site, loc, row)) {
            // Production Hold fixtures are dressed into the district shell instead of pasting lab rooms.
        } else {
            buildLabFixture(site, loc);
        }

        stabilizeHoldAuditAnchor(id, loc.getBlock());
        if ("vaun_bookshelf".equals(type)) {
            placeMechanicBookshelf(loc.getBlock());
        }
        if (DEEP_HOLD_LORE_SEEDS.contains(id)) {
            seedFixtureLore(loc, holdLoreSeed(id));
        }
        enhanceHoldVisual(site, loc);
        removeRetiredBeaconNear(loc);
    }

    private void shapeHoldFixtureSetting(Site site, Location loc, HoldSite row) {
        if (site == null || loc == null || loc.getWorld() == null || row == null) return;
        if (needsHoldDedicatedRoom(row)) return;
        String type = site.type();
        // Clustered chapel mechanics share the grand chapel shell. Giving every lectern, marker,
        // or dial its own 11x10 bay made later fixtures erase earlier ones during the same build.
        if (Set.of("mara_lectern", "sella_lectern", "mara_route_marker", "orin_marker",
                "orin_frame_dial", "vaun_hoard_chest", "vaun_bookshelf", "sella_pool",
                "sella_anchor", "brann_toll_tower", "brann_corridor_start",
                "brann_corridor_end", "lampworks_stair", "lamp_stand", "painted_line").contains(type)) return;
        if ("mara_lectern".equals(type) || "sella_lectern".equals(type)) {
            shapeHoldWorkTableBay(loc, row, Material.DEEPSLATE_TILES);
        } else if ("keeper_stone".equals(type)) {
            shapeHoldKeeperApse(loc, row);
        } else if ("orin_marker".equals(type) || "orin_frame_dial".equals(type) || "bow_marker".equals(type)
                || "mara_route_marker".equals(type)) {
            shapeHoldLowThresholdBay(loc, row);
        } else if ("kept_light".equals(type) || "offering_cairn".equals(type) || "marker".equals(type)) {
            shapeHoldEvidenceBay(loc, row, Material.CHISELED_DEEPSLATE);
        } else if ("vaun_hoard_chest".equals(type) || "vaun_bookshelf".equals(type)
                || "sella_pool".equals(type) || "sella_anchor".equals(type)
                || "brann_toll_tower".equals(type) || "brann_corridor_start".equals(type)
                || "brann_corridor_end".equals(type)) {
            shapeHoldEvidenceBay(loc, row, Material.CHISELED_DEEPSLATE);
        } else if (isArchiveAlcove(row)) {
            shapeHoldEvidenceBay(loc, row, Material.DEEPSLATE_TILES);
        } else if (isMarketAlcove(row)) {
            shapeHoldEvidenceBay(loc, row, Material.POLISHED_DEEPSLATE);
        } else if (row.z() >= 140 || row.y() < 0 || "lampworks_stair".equals(type)
                || "lamp_stand".equals(type) || "painted_line".equals(type)) {
            shapeHoldLowerBay(loc, row);
        }
    }

    private boolean buildHoldIntegratedFixture(Site site, Location loc, HoldSite row) {
        if (site == null || loc == null || loc.getWorld() == null || row == null) return false;
        String id = site.id();
        String type = site.type();
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        if ("case_board".equals(id)) {
            buildHoldCaseBoardCore(world, bx, by, bz);
            return true;
        } else if ("prior_camp".equals(id)) {
            buildHoldPriorCampCore(world, bx, by, bz);
            return true;
        } else if ("failed_accepting".equals(id)) {
            buildHoldFailedAcceptingCore(world, bx, by, bz);
            return true;
        } else if ("keeper_altar".equals(id)) {
            buildHoldKeeperAltarCore(world, bx, by, bz);
            return true;
        } else if ("coop_plate".equals(id)) {
            buildHoldCoopPlateCore(world, bx, by, bz);
            return true;
        }
        return switch (type) {
            case "kept_light" -> {
                buildHoldHearthCore(world, bx, by, bz);
                yield true;
            }
            case "offering_cairn" -> {
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.COBBLED_DEEPSLATE, Material.CANDLE);
                yield true;
            }
            case "bow_marker", "mara_map_marker" -> {
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.CHISELED_DEEPSLATE, Material.BLACK_CANDLE);
                yield true;
            }
            case "orin_marker" -> {
                int rank = trailingIndex(id, 1);
                int[] rotations = {0, 2, 4, 6, 1, 5};
                String[] bearings = {"north", "east", "south", "west", "north-east", "south-west"};
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.CHISELED_DEEPSLATE, Material.BLACK_CANDLE);
                placeStandingSign(new Location(world, bx, by, bz + 2), BlockFace.NORTH,
                        new String[]{"fall mark " + rank, "faces " + bearings[rank - 1],
                                "dial turn " + rotations[rank - 1], "bow in order"});
                yield true;
            }
            case "mara_route_marker" -> {
                int rank = trailingIndex(id, 1);
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.POLISHED_DEEPSLATE, Material.GRAY_CANDLE);
                placeStandingSign(new Location(world, bx, by, bz + 2), BlockFace.NORTH,
                        new String[]{"walked mark " + rank, "follow in order", rank == 4 ? "then bow" : "do not skip", ""});
                yield true;
            }
            case "vaun_hoard_chest" -> {
                buildHoldVaunHoardCore(world, bx, by, bz);
                yield true;
            }
            case "vaun_bookshelf" -> {
                buildHoldVaunShelfCore(world, bx, by, bz);
                yield true;
            }
            case "sella_pool" -> {
                buildHoldWaterMirrorCore(world, bx, by, bz);
                yield true;
            }
            case "sella_anchor" -> {
                buildHoldWaterAnchorCore(world, bx, by, bz);
                yield true;
            }
            case "orin_frame_dial" -> {
                buildHoldFrameDialCore(world, bx, by, bz, trailingIndex(id, 1));
                yield true;
            }
            case "brann_corridor_start", "brann_corridor_end" -> {
                buildHoldBrannCorridorCore(world, bx, by, bz, "brann_corridor_end".equals(type));
                yield true;
            }
            case "brann_toll_tower" -> {
                buildHoldBrannTollCore(world, bx, by, bz);
                yield true;
            }
            case "lampworks_stair" -> {
                buildHoldLampworksCore(world, bx, by, bz);
                yield true;
            }
            case "lamp_stand" -> {
                buildHoldLampStandCore(world, bx, by, bz, false);
                yield true;
            }
            case "painted_line" -> {
                buildHoldPaintedLineCore(world, bx, by, bz);
                yield true;
            }
            case "school_stand" -> {
                buildHoldSchoolCore(world, bx, by, bz);
                yield true;
            }
            case "markers_row" -> {
                buildHoldMarkersRowCore(world, bx, by, bz);
                yield true;
            }
            case "cistern_7" -> {
                buildHoldCisternCore(world, bx, by, bz);
                yield true;
            }
            case "watch_floor" -> {
                buildHoldWatchFloorCore(world, bx, by, bz);
                yield true;
            }
            case "set_apart_shelf" -> {
                buildHoldSetApartShelfCore(world, bx, by, bz);
                yield true;
            }
            case "far_water" -> {
                buildHoldFarWaterCore(world, bx, by, bz);
                yield true;
            }
            case "undercroft_seal" -> {
                buildHoldUndercroftSealCore(world, bx, by, bz);
                yield true;
            }
            case "forgotten_mouth" -> {
                buildHoldForgottenMouthCore(world, bx, by, bz);
                yield true;
            }
            case "deep_market" -> {
                buildHoldMarketCore(world, bx, by, bz);
                yield true;
            }
            case "ration_table" -> {
                buildHoldRationCore(world, bx, by, bz);
                yield true;
            }
            case "third_bay_breach" -> {
                buildHoldThirdBayCore(world, bx, by, bz);
                yield true;
            }
            case "warm_town_collapse" -> {
                buildHoldWarmCollapseCore(world, bx, by, bz);
                yield true;
            }
            case "dead_stall" -> {
                buildHoldDeadStallCore(world, bx, by, bz);
                yield true;
            }
            case "bird_coops" -> {
                buildHoldBirdCoopsCore(world, bx, by, bz);
                yield true;
            }
            case "dread_route" -> {
                buildHoldDreadCore(world, bx, by, bz, id);
                yield true;
            }
            default -> false;
        };
    }

    private void buildHoldHearthCore(World world, int bx, int by, int bz) {
        Block fire = world.getBlockAt(bx, by, bz);
        fire.setType(Material.CAMPFIRE, false);
        if (fire.getBlockData() instanceof org.bukkit.block.data.type.Campfire c) {
            c.setLit(true);
            fire.setBlockData(c, false);
        }
        world.getBlockAt(bx - 2, by, bz + 1).setType(Material.DARK_OAK_STAIRS, false);
        world.getBlockAt(bx + 2, by, bz + 1).setType(Material.DARK_OAK_STAIRS, false);
        world.getBlockAt(bx, by, bz + 3).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 4, by, bz - 1), BlockFace.EAST,
                "kept light", List.of(
                        "a kept light is tended, not owned.",
                        "warmth was public before the lower count made it private."
                ));
    }

    private void buildHoldCaseBoardCore(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 10 || Math.abs(dz) == 7;
                boolean table = Math.abs(dx) <= 4 && Math.abs(dz) <= 1;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (table ? Material.DARK_OAK_PLANKS : Material.DEEPSLATE_TILES), false);
                if (rim && Math.floorMod(dx + dz, 3) == 0) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                }
            }
        }

        for (int dx = -4; dx <= 4; dx++) {
            world.getBlockAt(bx + dx, by, bz).setType(Material.DARK_OAK_SLAB, false);
            world.getBlockAt(bx + dx, by, bz + 1).setType(Material.DARK_OAK_SLAB, false);
        }
        Block leftIndex = world.getBlockAt(bx - 7, by, bz - 4);
        leftIndex.setType(Material.BARREL, false);
        faceDirectional(leftIndex.getLocation(), BlockFace.EAST);
        putWrittenBook(leftIndex, 0, "keeper index", "the record", List.of(
                "Vaun: debt before inventory. Mara: walked ground before filed ground.",
                "Sella: later ink against the neat count. Orin: posture without possession.",
                "Brann: duration after warning. Iss: the cold land against the warm account."
        ));
        Block rightIndex = world.getBlockAt(bx + 7, by, bz - 4);
        rightIndex.setType(Material.BARREL, false);
        faceDirectional(rightIndex.getLocation(), BlockFace.WEST);
        putWrittenBook(rightIndex, 0, "open evidence", "the record", List.of(
                "School. Far water. Markers. Cistern. Watch. Shelf.",
                "Seal. Mouth. Market. Ration. Third bay. Warm collapse. Coops.",
                "A room is not filed because it was visited. Say what changed."
        ));
        placeDecorativeBookshelf(world.getBlockAt(bx - 9, by, bz + 5), 113);
        placeDecorativeBookshelf(world.getBlockAt(bx + 9, by, bz + 5), 127);
        world.getBlockAt(bx - 8, by + 1, bz - 5).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx + 8, by + 1, bz - 5).setType(Material.LANTERN, false);
        world.getBlockAt(bx, by, bz + 5).setType(Material.LIGHT_GRAY_CARPET, false);

        placeStandingSign(new Location(world, bx - 5, by, bz + 4), BlockFace.NORTH,
                new String[]{"after the six", "do not enter", "with one kind", "of proof"});
        placeEditableStandingSign(new Location(world, bx, by, bz + 4), BlockFace.NORTH,
                new String[]{"file", "missing", "condition", ""});
        placeStandingSign(new Location(world, bx + 5, by, bz + 4), BlockFace.NORTH,
                new String[]{"decode", "witness", "compare", "then carry"});

        placeEvidenceLectern(new Location(world, bx - 9, by, bz - 2), BlockFace.EAST,
                "case board", List.of(
                        "The six stones are not the case. They are the index.\n\n" +
                                "Before the lower rite opens, the record wants four kinds of proof: " +
                                "keeper theory, Unlit recovery, side-site evidence, and human kindness.",
                        "Keeper theory: say what each keeper broke, and which later place proves or corrects it.\n\n" +
                                "Do not file a solved cipher as a theory unless the world also supports it.",
                        "Side-site evidence: school, far water, markers, cistern, watch floor, shelf, seal, " +
                                "mouth, market, ration table, third bay, warm collapse, and coops.\n\n" +
                                "A visit is not enough if no one can say what changed.",
                        "The failed camp beyond this board is not another keeper.\n\n" +
                                "It is the control group: they had answers, but the record says no witness.",
                        "The unwaxed sign between the labels is a filing slit, not a notice board.\n\n" +
                                "Use it when the record asks for a condition rather than another solved cipher."
                ));
        placeEvidenceLectern(new Location(world, bx + 9, by, bz - 2), BlockFace.WEST,
                "open rows", List.of(
                        "Parallel work is expected.\n\n" +
                                "One group can trace Sella through water and school while another checks Brann's " +
                                "watch floor, or Iss against the warm-town collapse.",
                        "The Unlit mirror village is not a bonus room. It is the correction pass.\n\n" +
                                "Bring back what the copied houses prove about the customs, not only where they were.",
                        "When the evidence is filed, the accepting floor stops being a finale button and becomes " +
                                "the only honest next act: a group lowering itself together."
                ));
    }

    private void buildHoldPriorCampCore(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int dx = -25; dx <= 25; dx++) {
            for (int dz = -17; dz <= 17; dz++) {
                boolean path = Math.abs(dx) <= 2 || dz == 0;
                Material floor = path ? Material.POLISHED_DEEPSLATE
                        : (Math.floorMod(dx + dz, 2) == 0 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE_BRICKS);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
            }
        }

        Block fire = world.getBlockAt(bx, by, bz);
        fire.setType(Material.CAMPFIRE, false);
        if (fire.getBlockData() instanceof org.bukkit.block.data.Lightable c) {
            c.setLit(false);
            fire.setBlockData(c, false);
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.BLACKSTONE, false);

        for (int x : new int[]{bx - 18, bx + 18}) {
            for (int z : new int[]{bz - 10, bz, bz + 10}) {
                buildPriorCampBay(world, x, by, z);
            }
        }
        placePriorBedroll(world, bx - 18, by, bz - 10, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET);
        placePriorBedroll(world, bx - 18, by, bz, Material.BROWN_CARPET, Material.LIGHT_GRAY_CARPET);
        placePriorBedroll(world, bx - 18, by, bz + 10, Material.BLACK_CARPET, Material.GRAY_CARPET);
        placePriorBedroll(world, bx + 18, by, bz - 10, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET);
        placePriorBedroll(world, bx + 18, by, bz, Material.BROWN_CARPET, Material.LIGHT_GRAY_CARPET);
        placePriorBedroll(world, bx + 18, by, bz + 10, Material.BLACK_CARPET, Material.GRAY_CARPET);

        placeEvidenceLectern(new Location(world, bx, by, bz - 3), BlockFace.SOUTH,
                "failed inventory", List.of(
                        "Copied after the refusal.\n\nSix stones solved. Six answers carried. Six tokens wrapped.",
                        "No line names a person who could stand outside the finish and still say what happened.",
                        "The old group brought a complete packet and no outside truth. That is the difference this camp preserves."
                ));

        placePriorBedrollPacket(world, bx - 21, by, bz - 10, BlockFace.EAST,
                "vaun packet", List.of(
                        "Three tally knots. One ration tag rubbed clean.",
                        "Their note starts with what Vaun held back. It never asks who the first share belonged to.",
                        "Compare market debt before inventory."
                ));
        placePriorBedrollPacket(world, bx - 21, by, bz, BlockFace.EAST,
                "mara packet", List.of(
                        "A copied route, folded flat. No mud. No broken fern stem. No return scratch on the map.",
                        "Their note proves the sentence travelled. It does not prove anyone did.",
                        "Compare walked ground before filed ground."
                ));
        placePriorBedrollPacket(world, bx - 21, by, bz + 10, BlockFace.EAST,
                "sella packet", List.of(
                        "The grey seventh was circled as a copyist's stain.",
                        "Their note trusts clean ink over a child, a cistern, and the water's copy.",
                        "Compare later ink before neat count."
                ));
        placePriorBedrollPacket(world, bx + 21, by, bz - 10, BlockFace.WEST,
                "orin packet", List.of(
                        "The banner is folded above eye height.",
                        "Their note calls the bow a price paid to the room. The low frames say it was how the room could be read.",
                        "Compare posture before possession."
                ));
        placePriorBedrollPacket(world, bx + 21, by, bz, BlockFace.WEST,
                "brann packet", List.of(
                        "The toll is marked heard. The bedroll is still warm.",
                        "Their note treats warning as completion. The watch floor keeps counting after the bell.",
                        "Compare duration before alarm."
                ));
        placePriorBedrollPacket(world, bx + 21, by, bz + 10, BlockFace.WEST,
                "iss packet", List.of(
                        "The warm wall sentence is copied twice. The cold shelf mark is not copied at all.",
                        "Their note asks whether comfort sounded kind. It never asks whether the land agreed.",
                        "Compare cold proof before warm speech."
                ));

        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(bx + dx, by, bz + 7).setType(Material.LIGHT_GRAY_CARPET, false);
        }
        placeStandingSign(new Location(world, bx, by, bz + 6), BlockFace.NORTH,
                new String[]{"blank place", "not empty", "unwitnessed", ""});

        placeStandingSign(new Location(world, bx - 4, by, bz - 7), BlockFace.NORTH,
                new String[]{"same answers", "same stones", "no witness", ""});
        placeEditableStandingSign(new Location(world, bx, by, bz - 7), BlockFace.NORTH,
                new String[]{"file", "correction", "here", ""});
        placeStandingSign(new Location(world, bx + 4, by, bz - 7), BlockFace.NORTH,
                new String[]{"correct", "the file", "before rite", ""});

        placeEvidenceLectern(new Location(world, bx - 23, by, bz - 10), BlockFace.EAST,
                "accepting record, failed", List.of(
                        "Six arrived with six stones and six answers. They called that enough.",
                        "The floor took their tokens and returned nothing.\n\nThe seventh place was not empty. It was unwitnessed.",
                        "Correction line: no witness is not a missing person. It is a missing relation.\n\nNo one outside the finish could say what was true."
                ));
        placeEvidenceLectern(new Location(world, bx + 23, by, bz - 10), BlockFace.WEST,
                "witness condition", List.of(
                        "A witness cannot be another token in the bowl.",
                        "A witness stands outside the wish to finish and still carries the truth back.",
                        "If the next rite repeats this camp, the Hold will count it as another kept thing.",
                        "The unwaxed signs by the repair files are filing slits.\n\n" +
                                "Do not file a correction until this camp file and a living site both agree."
                ));

        placePriorCorrectionBarrel(world, bx - 13, by, bz + 2, BlockFace.EAST,
                "vaun correction", List.of(
                        "Prior file: Vaun held three back. The old group treated the held count as the lesson and called the hoard holy.",
                        "Market and ration proof disagree. The first share was already owed before anyone made an inventory.",
                        "File the repair as order, not math: return first before count."
                ));
        placeEditableStandingSign(new Location(world, bx - 12, by, bz + 2), BlockFace.EAST,
                new String[]{"file", "vaun", "repair", ""});
        placePriorCorrectionBarrel(world, bx - 10, by, bz + 6, BlockFace.NORTH,
                "mara correction", List.of(
                        "Prior file: Mara's sentence was copied cleanly into the packet.",
                        "The map markers disagree with clean copying. The route becomes proof only when feet take the risk the page describes.",
                        "File the repair as action before archive: walk it before filing it."
                ));
        placeEditableStandingSign(new Location(world, bx - 10, by, bz + 5), BlockFace.NORTH,
                new String[]{"file", "mara", "repair", ""});
        placePriorCorrectionBarrel(world, bx - 4, by, bz + 7, BlockFace.NORTH,
                "sella correction", List.of(
                        "Prior file: the grey seventh was marked later, therefore false.",
                        "School, water, and cistern evidence disagree. Later ink is how a living absence finally entered the record.",
                        "File the repair as count order: count the seventh before the six."
                ));
        placeEditableStandingSign(new Location(world, bx - 4, by, bz + 6), BlockFace.NORTH,
                new String[]{"file", "sella", "repair", ""});
        placePriorCorrectionBarrel(world, bx + 4, by, bz + 7, BlockFace.NORTH,
                "orin correction", List.of(
                        "Prior file: the bow was logged as payment.",
                        "The low frames disagree. Crouching makes the mark legible without letting the reader stand over it.",
                        "File the repair as posture, not tribute: bowing is proof, not payment."
                ));
        placeEditableStandingSign(new Location(world, bx + 4, by, bz + 6), BlockFace.NORTH,
                new String[]{"file", "orin", "repair", ""});
        placePriorCorrectionBarrel(world, bx + 10, by, bz + 6, BlockFace.NORTH,
                "brann correction", List.of(
                        "Prior file: the toll was heard, so the watch was complete.",
                        "The corridor and watch floor disagree. A bell only starts the duty; sleep after the warning is still absence.",
                        "File the repair as duration: the watch must be kept."
                ));
        placeEditableStandingSign(new Location(world, bx + 10, by, bz + 5), BlockFace.NORTH,
                new String[]{"file", "brann", "repair", ""});
        placePriorCorrectionBarrel(world, bx + 13, by, bz + 2, BlockFace.WEST,
                "iss correction", List.of(
                        "Prior file: the warm wall was mercy.",
                        "The cold land, shelf, and collapse disagree. Comfort can be cover when it refuses the count outside itself.",
                        "File the repair as cross-check: test warmth against the land."
                ));
        placeEditableStandingSign(new Location(world, bx + 12, by, bz + 2), BlockFace.WEST,
                new String[]{"file", "iss", "repair", ""});

        for (int dx : new int[]{-24, 24}) {
            for (int dz : new int[]{-15, 0, 15}) {
                world.getBlockAt(bx + dx, by, bz + dz).setType(Material.SOUL_LANTERN, false);
            }
        }
        placeDecorativeBookshelf(world.getBlockAt(bx - 23, by, bz + 15), 233);
        placeDecorativeBookshelf(world.getBlockAt(bx + 23, by, bz + 15), 241);
    }

    private void buildPriorCampBay(World world, int cx, int y, int cz) {
        if (world == null) return;
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-3, 3}) {
                world.getBlockAt(cx + dx, y, cz + dz).setType(Material.DARK_OAK_FENCE, false);
                world.getBlockAt(cx + dx, y + 1, cz + dz).setType(Material.DARK_OAK_FENCE, false);
                world.getBlockAt(cx + dx, y + 2, cz + dz).setType(Material.DARK_OAK_FENCE, false);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 5) continue;
                world.getBlockAt(cx + dx, y + 3, cz + dz).setType(
                        Math.floorMod(dx + dz, 3) == 0 ? Material.GRAY_WOOL : Material.BLACK_WOOL, false);
            }
        }
    }

    private void buildHoldFailedAcceptingCore(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean ring = Math.abs(dx) == 10 || Math.abs(dz) == 6;
                boolean cross = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(ring ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (cross ? Material.CHISELED_DEEPSLATE : Material.DEEPSLATE_TILES), false);
            }
        }

        int[][] slots = {{-6, -3}, {-6, 3}, {0, -5}, {0, 5}, {6, -3}, {6, 3}};
        Material[] marks = {
                Material.COPPER_BLOCK, Material.BOOKSHELF, Material.PRISMARINE_BRICKS,
                Material.CHISELED_DEEPSLATE, Material.GOLD_BLOCK, Material.SMOOTH_RED_SANDSTONE
        };
        for (int i = 0; i < slots.length; i++) {
            int x = bx + slots[i][0];
            int z = bz + slots[i][1];
            world.getBlockAt(x, by, z).setType(Material.POLISHED_BLACKSTONE_BRICK_SLAB, false);
            world.getBlockAt(x, by + 1, z).setType(marks[i], false);
            world.getBlockAt(x, by + 2, z).setType(Material.BLACK_CANDLE, false);
        }
        world.getBlockAt(bx, by, bz).setType(Material.CHISELED_TUFF, false);
        world.getBlockAt(bx, by + 1, bz).setType(Material.LIGHT_GRAY_CARPET, false);
        placeStandingSign(new Location(world, bx, by, bz + 3), BlockFace.NORTH,
                new String[]{"six tokens", "were present", "witness was", "not"});
        placeEditableStandingSign(new Location(world, bx + 3, by, bz + 5), BlockFace.NORTH,
                new String[]{"file", "witness", "condition", ""});

        placeEvidenceLectern(new Location(world, bx - 9, by, bz - 4), BlockFace.EAST,
                "failed accepting floor", List.of(
                        "This is not a rehearsal room. It is the old result.",
                        "Six tokens reached the floor. The room refused them because every hand inside the circle wanted the same finish.",
                        "The correction is not another token.\n\nBring witness before accepting."
                ));
        placeEvidenceLectern(new Location(world, bx + 9, by, bz - 4), BlockFace.WEST,
                "before the last warm", List.of(
                        "The accepting floor is not asking whether the case is complete.",
                        "It asks whether completion can be checked by someone who is not trying to own it.",
                        "When every correction has a file, the filing sign is no longer asking for another token.\n\n" +
                                "The condition is the order of the rite: witness before accepting."
                ));
    }

    private void placePriorBedroll(World world, int x, int y, int z, Material blanket, Material pillow) {
        if (world == null) return;
        Material cover = blanket == null ? Material.GRAY_CARPET : blanket;
        Material head = pillow == null ? Material.LIGHT_GRAY_CARPET : pillow;
        world.getBlockAt(x, y, z).setType(head, false);
        world.getBlockAt(x, y, z + 1).setType(cover, false);
        world.getBlockAt(x, y, z + 2).setType(cover, false);
    }

    private void placePriorBedrollPacket(World world, int x, int y, int z,
                                         BlockFace facing, String title, List<String> pages) {
        if (world == null) return;
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.BARREL, false);
        faceDirectional(new Location(world, x, y, z), facing);
        putWrittenBook(block, 0, title, "prior packet", pages);
        Block candle = world.getBlockAt(x, y + 1, z);
        if (candle.getType().isAir()) {
            candle.setType(Material.GRAY_CANDLE, false);
        }
    }

    private void placePriorCorrectionBarrel(World world, int x, int y, int z,
                                            BlockFace facing, String title, List<String> pages) {
        if (world == null) return;
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.BARREL, false);
        faceDirectional(new Location(world, x, y, z), facing);
        putWrittenBook(block, 0, title, "prior hand", pages);
    }

    private void buildHoldSmallEvidenceCore(World world, int bx, int by, int bz, Material body, Material top) {
        world.getBlockAt(bx, by, bz).setType(body == null ? Material.CHISELED_DEEPSLATE : body, false);
        if (top != null && top != Material.AIR) {
            world.getBlockAt(bx, by + 1, bz).setType(top, false);
        }
        for (int dz = -2; dz <= 2; dz++) {
            world.getBlockAt(bx - 2, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(bx + 2, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
        }
    }

    private void buildHoldVaunHoardCore(World world, int bx, int by, int bz) {
        Block chest = world.getBlockAt(bx, by, bz);
        chest.setType(Material.CHEST, false);
        if (chest.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.SOUTH);
            chest.setBlockData(d, false);
        }
        if (chest.getState() instanceof InventoryHolder holder) holder.getInventory().clear();
        int sourceIndex = 0;
        for (int dx : new int[]{-3, 3}) {
            Block source = world.getBlockAt(bx + dx, by, bz);
            source.setType(Material.BARREL, false);
            if (source.getState() instanceof InventoryHolder holder) {
                holder.getInventory().clear();
                holder.getInventory().setItem(13, new ItemStack(
                        sourceIndex++ == 0 ? Material.DEEPSLATE : Material.COBBLED_DEEPSLATE, 8));
            }
            world.getBlockAt(bx + dx, by + 1, bz).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(bx + dx, by + 2, bz).setType(Material.SOUL_LANTERN, false);
        }
        placeStandingSign(new Location(world, bx, by, bz + 1), BlockFace.NORTH,
                new String[]{"GIVEN BACK", "return one", "both sources", "then close"});
        placeEvidenceLectern(new Location(world, bx, by, bz + 3), BlockFace.NORTH,
                "hoard tally", List.of(
                        "The two source barrels hold the first of the deep. Put one of each into the empty GIVEN BACK chest.",
                        "Vaun's guilt begins as inventory, not greed."
                ));
    }

    private void buildHoldVaunShelfCore(World world, int bx, int by, int bz) {
        for (int dx = -2; dx <= 2; dx++) {
            if (dx == 0) {
                placeMechanicBookshelf(world.getBlockAt(bx, by, bz));
            } else {
                placeDecorativeBookshelf(world.getBlockAt(bx + dx, by, bz), 71 + dx);
            }
            if (Math.abs(dx) <= 1) placeDecorativeBookshelf(world.getBlockAt(bx + dx, by + 1, bz), 89 + dx);
        }
        world.getBlockAt(bx, by, bz + 2).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 3, by, bz + 1), BlockFace.EAST,
                "shelf tally", List.of(
                        "three shelf marks agree with the hoard.",
                        "one useful thing was filed as sacred after it was missing."
                ));
    }

    private void buildHoldWaterMirrorCore(World world, int bx, int by, int bz) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean water = Math.abs(dx) <= 3 && Math.abs(dz) <= 3;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(water ? ((Math.abs(dx) + Math.abs(dz)) % 2 == 0
                                ? Material.PRISMARINE_BRICKS : Material.DARK_PRISMARINE)
                                : Material.DEEPSLATE_TILES, false);
                world.getBlockAt(bx + dx, by, bz + dz).setType(water ? Material.WATER : Material.AIR, false);
            }
        }
        // Five readable ripple tallies are the page-lock key, ordered inner-to-outer.
        int[] ringPages = {2, 3, 5, 7, 11};
        for (int i = 0; i < ringPages.length; i++) {
            placeStandingSign(new Location(world, bx - 4 + (i * 2), by, bz - 6), BlockFace.SOUTH,
                    new String[]{"ripple " + (i + 1), "count " + ringPages[i], "inner to outer", ""});
        }
        placeEvidenceLectern(new Location(world, bx - 6, by, bz - 1), BlockFace.EAST,
                "water copy", List.of(
                        "Five dropped-stone rings are copied inner to outer. Turn the five loose books to those tallies.",
                        "Sella kept drawings because words were easy to recut. The pool gives the order; the pages give the lock."
                ));
        world.getBlockAt(bx + 5, by, bz + 5).setType(Material.BARREL, false);
    }

    private void buildHoldWaterAnchorCore(World world, int bx, int by, int bz) {
        // A real four-rise viewing stair: adventure-mode players can reach the worn vantage.
        for (int step = 0; step <= 4; step++) {
            int y = by - 4 + step;
            int z = bz + 4 - step;
            for (int dx = -2; dx <= 2; dx++) {
                world.getBlockAt(bx + dx, y - 1, z).setType(Material.DARK_PRISMARINE, false);
                if (Math.abs(dx) == 2) world.getBlockAt(bx + dx, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
            }
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(bx - 1, by, bz).setType(Material.WHITE_CARPET, false);
        world.getBlockAt(bx + 1, by, bz).setType(Material.GRAY_CARPET, false);
        placeStandingSign(new Location(world, bx + 2, by, bz), BlockFace.WEST,
                new String[]{"worn overlook", "stand here", "look down", "only the bird"});
    }

    private void buildHoldFrameDialCore(World world, int bx, int by, int bz, int index) {
        placeFrameDial(new Location(world, bx, by, bz));
        world.getBlockAt(bx - 2, by, bz + 1).setType(index % 2 == 0 ? Material.GRAY_CARPET : Material.BLACK_CARPET, false);
        world.getBlockAt(bx + 2, by, bz + 1).setType(Material.DARK_OAK_BUTTON, false);
    }

    private void buildHoldBrannCorridorCore(World world, int bx, int by, int bz, boolean end) {
        for (int dx = -4; dx <= 4; dx++) {
            world.getBlockAt(bx + dx, by, bz - 4).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(bx + dx, by, bz + 4).setType(Material.DEEPSLATE_BRICK_WALL, false);
        }
        world.getBlockAt(bx, by, bz).setType(end ? Material.BLACKSTONE : Material.POLISHED_DEEPSLATE, false);
        world.getBlockAt(bx, by + 1, bz).setType(end ? Material.BLACK_CANDLE : Material.SOUL_LANTERN, false);
    }

    private void buildHoldBrannTollCore(World world, int bx, int by, int bz) {
        for (int dy = 0; dy <= 5; dy++) {
            world.getBlockAt(bx - 2, by + dy, bz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
            world.getBlockAt(bx + 2, by + dy, bz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        }
        world.getBlockAt(bx, by, bz).setType(Material.SCULK_SENSOR, false);
        placeEvidenceLectern(new Location(world, bx, by, bz + 3), BlockFace.NORTH,
                "black toll", List.of(
                        "the toll was paid in silence.",
                        "Brann called the door public because everyone was afraid to refuse it."
                ));
    }

    private void buildHoldLampworksCore(World world, int bx, int by, int bz) {
        // The grand spine owns the continuous 84-block staircase. This fixture dresses it without
        // recarving a second, steeper ramp through the walkable surface.
        placeEvidenceLectern(new Location(world, bx - 10, by, bz - 2), BlockFace.EAST,
                "lamp count", List.of(
                        "first line kept. second line borrowed. third line went dry.",
                        "the ready mark was copied after the lamp was gone."
                ));
        for (int dx : new int[]{-6, 6}) {
            world.getBlockAt(bx + dx, by, bz).setType(Material.POLISHED_BASALT, false);
            world.getBlockAt(bx + dx, by + 1, bz).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void buildHoldLampStandCore(World world, int bx, int by, int bz, boolean lit) {
        world.getBlockAt(bx, by - 1, bz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        world.getBlockAt(bx, by, bz).setType(Material.DEEPSLATE_BRICK_WALL, false);
        Block lamp = world.getBlockAt(bx, by + 1, bz);
        lamp.setType(Material.COPPER_BULB, false);
        if (lamp.getBlockData() instanceof org.bukkit.block.data.Lightable lightable) {
            lightable.setLit(lit);
            lamp.setBlockData(lightable, false);
        }
        Block supply = world.getBlockAt(bx - 2, by, bz);
        supply.setType(Material.BARREL, false);
        if (supply.getState() instanceof InventoryHolder holder) {
            holder.getInventory().clear();
            holder.getInventory().setItem(13, new ItemStack(Material.SOUL_TORCH, 3));
        }
        world.getBlockAt(bx + 2, by, bz).setType(Material.POLISHED_DEEPSLATE, false);
        placeStandingSign(new Location(world, bx + 2, by, bz + 1), BlockFace.NORTH,
                new String[]{"THIRD STAND", "take a wick", "touch dark bulb", "do not build"});
    }

    private void buildHoldPaintedLineCore(World world, int bx, int by, int bz) {
        for (int dx = -8; dx <= 8; dx++) {
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.DEEPSLATE_BRICKS, false);
            world.getBlockAt(bx + dx, by, bz).setType(Material.BLACK_CARPET, false);
        }
        placeEvidenceLectern(new Location(world, bx + 9, by, bz), BlockFace.WEST,
                "line count", List.of(
                        "crossing it is a choice, not a road.",
                        "Aro's warm answer stops here."
                ));
    }

    private void buildHoldSchoolCore(World world, int bx, int by, int bz) {
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by, bz - 5).setType(Material.BLACK_CONCRETE, false);
        }
        for (int row = 0; row < 3; row++) {
            int z = bz - 3 + (row * 3);
            for (int x = bx - 4; x <= bx + 4; x += 4) {
                world.getBlockAt(x, by, z).setType(Material.DARK_OAK_SLAB, false);
                world.getBlockAt(x, by - 1, z).setType(Material.DARK_OAK_PLANKS, false);
                world.getBlockAt(x, by, z + 1).setType(Material.WHITE_CARPET, false);
            }
        }
        placeDecorativeBookshelf(world.getBlockAt(bx - 5, by, bz + 4), 11);
        world.getBlockAt(bx + 4, by, bz - 4).setType(Material.GRAY_CONCRETE, false);
        world.getBlockAt(bx + 5, by, bz + 4).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.EAST,
                "copy line", List.of(
                        "keep your light.",
                        "keep your light.",
                        "six stones copied the line. the grey one did not."
                ));
    }

    private void buildHoldMarkersRowCore(World world, int bx, int by, int bz) {
        for (int i = 0; i < 6; i++) {
            int x = bx - 6 + (i * 2);
            world.getBlockAt(x, by, bz).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(x, by + 1, bz).setType(Material.COBBLED_DEEPSLATE_WALL, false);
            world.getBlockAt(x, by, bz - 1).setType(Material.BROWN_CARPET, false);
        }
        world.getBlockAt(bx + 7, by - 1, bz).setType(Material.SOUL_SAND, false);
        world.getBlockAt(bx + 7, by, bz).setType(Material.GRAY_CONCRETE, false);
        world.getBlockAt(bx + 7, by + 1, bz).setType(Material.GRAY_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx - 8, by, bz + 2), BlockFace.SOUTH,
                "marker row", List.of(
                        "six stones were set for bowing.",
                        "one hollow was left at the end.",
                        "count again after the winter mark."
                ));
    }

    private void buildHoldCisternCore(World world, int bx, int by, int bz) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean water = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(water ? Material.PRISMARINE_BRICKS : Material.DARK_PRISMARINE, false);
                world.getBlockAt(bx + dx, by, bz + dz).setType(water ? Material.WATER : Material.AIR, false);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by + 2, bz - 5).setType(Material.END_STONE_BRICKS, false);
        }
        world.getBlockAt(bx - 5, by, bz + 3).setType(Material.BARREL, false);
        world.getBlockAt(bx - 4, by, bz + 3).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.EAST,
                "cistern seven", List.of(
                        "good oil: two jars.",
                        "the water gave light back wrong.",
                        "the seventh mark is under the dark surface."
                ));
    }

    private void buildHoldWatchFloorCore(World world, int bx, int by, int bz) {
        Block lectern = world.getBlockAt(bx, by, bz);
        placeReadableLectern(lectern, BlockFace.SOUTH);
        fillWrittenLecternBook(lectern, "watch floor", "the record", List.of(
                "black moon.",
                "do not sleep on the floor of the watch.",
                "the log stopped writing before the keepers stopped standing."
        ));
        for (int dx : new int[]{-5, 5}) {
            for (int dz : new int[]{-4, 4}) {
                world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                world.getBlockAt(bx + dx, by + 1, bz + dz).setType(Material.SOUL_LANTERN, false);
            }
        }
        placeDecorativeBookshelf(world.getBlockAt(bx - 2, by, bz), 17);
        world.getBlockAt(bx + 2, by, bz).setType(Material.DAYLIGHT_DETECTOR, false);
        world.getBlockAt(bx, by, bz + 2).setType(Material.BLACK_CANDLE, false);
    }

    private void buildHoldSetApartShelfCore(World world, int bx, int by, int bz) {
        for (int i = 0; i < 5; i++) {
            int x = bx - 4 + (i * 2);
            world.getBlockAt(x, by, bz).setType(Material.BARREL, false);
            if (i == 4) {
                world.getBlockAt(x, by + 1, bz).setType(Material.LANTERN, false);
                world.getBlockAt(x, by, bz + 1)
                        .setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
            } else {
                placeDecorativeBookshelf(world.getBlockAt(x, by, bz + 1), i + 23);
                world.getBlockAt(x, by + 1, bz).setType(Material.SOUL_LANTERN, false);
            }
        }
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "entry five shelf", List.of(
                        "entry five was set apart with a warm lamp.",
                        "do not price it.",
                        "do not count it with the cold shelf."
                ));
    }

    private void buildHoldFarWaterCore(World world, int bx, int by, int bz) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean water = Math.abs(dx) <= 5 && Math.abs(dz) <= 2;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(water ? Material.DARK_PRISMARINE : Material.POLISHED_BLACKSTONE_BRICKS, false);
                world.getBlockAt(bx + dx, by, bz + dz).setType(water ? Material.WATER : Material.AIR, false);
            }
        }
        world.getBlockAt(bx, by, bz - 3).setType(Material.SEA_LANTERN, false);
        placeDecorativeBookshelf(world.getBlockAt(bx - 7, by, bz - 3), 41);
        world.getBlockAt(bx + 7, by, bz + 3).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 6, by, bz + 4), BlockFace.SOUTH,
                "far water", List.of(
                        "copy the count from the water, not the shelf.",
                        "the far copy was kept because the near copy was recut."
                ));
    }

    private void buildHoldUndercroftSealCore(World world, int bx, int by, int bz) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                boolean frame = Math.abs(dx) == 4 || dy == 5;
                world.getBlockAt(bx + dx, by + dy, bz + 3)
                        .setType(frame ? Material.POLISHED_DEEPSLATE : Material.REINFORCED_DEEPSLATE, false);
            }
        }
        world.getBlockAt(bx, by, bz + 2).setType(Material.IRON_BARS, false);
        world.getBlockAt(bx, by + 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(bx, by + 2, bz).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx - 3, by, bz - 1).setType(Material.GRAY_CARPET, false);
        world.getBlockAt(bx - 2, by, bz - 1).setType(Material.GRAY_CARPET, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "mason's rest", List.of(
                        "the seal was entered from the wrong side.",
                        "the mason cut the last line low.",
                        "bow to read what was not small."
                ));
    }

    private void buildHoldForgottenMouthCore(World world, int bx, int by, int bz) {
        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by - 1, bz + 4).setType(Material.GRASS_BLOCK, false);
        }
        for (int dz = -5; dz <= 3; dz++) {
            for (int dx : new int[]{-3, 3}) {
                int height = dz < -1 ? 5 : 3;
                for (int dy = 0; dy <= height; dy++) {
                    world.getBlockAt(bx + dx, by + dy, bz + dz).setType(Material.BLACKSTONE, false);
                }
            }
        }
        world.getBlockAt(bx, by, bz - 5).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(bx, by, bz + 4).setType(Material.GLOWSTONE, false);
        world.getBlockAt(bx - 1, by, bz + 3).setType(Material.OAK_LEAVES, false);
        world.getBlockAt(bx + 1, by, bz + 3).setType(Material.OAK_LEAVES, false);
        placeEvidenceLectern(new Location(world, bx - 3, by, bz - 5), BlockFace.SOUTH,
                "way up draft", List.of(
                        "the way up was real.",
                        "it cost the line.",
                        "the last return mark healed where the surface could still remember it."
                ));
    }

    private void buildHoldMarketCore(World world, int bx, int by, int bz) {
        int stall = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                stall++;
                int sx = bx - 6 + (col * 3);
                int sz = bz - 4 + (row * 4);
                if (stall % 5 == 0) {
                    placeDecorativeBookshelf(world.getBlockAt(sx, by, sz), stall + 41);
                } else {
                    world.getBlockAt(sx, by, sz).setType(Material.BARREL, false);
                }
                world.getBlockAt(sx + 1, by, sz).setType(switch (stall % 6) {
                    case 1, 2, 3 -> Material.HAY_BLOCK;
                    case 4 -> Material.CAULDRON;
                    case 5 -> Material.ANVIL;
                    default -> Material.DARK_OAK_SLAB;
                }, false);
                world.getBlockAt(sx, by + 1, sz).setType(stall % 4 == 0 ? Material.CANDLE : Material.SOUL_LANTERN, false);
            }
        }
        Block lectern = world.getBlockAt(bx + 6, by, bz + 5);
        placeReadableLectern(lectern, BlockFace.WEST);
        fillWrittenLecternBook(lectern, "market tallies", "the record", List.of(
                "eighteen stalls were counted before the warm road closed.",
                "bread was traded for salt, oil, mending, and a watched lamp.",
                "one lamp could be minded for a token while the owner ate."
        ));
        placeDecorativeBookshelf(world.getBlockAt(bx + 5, by, bz + 5), 31);
        placeDecorativeBookshelf(world.getBlockAt(bx + 7, by, bz + 5), 37);
    }

    private void buildHoldRationCore(World world, int bx, int by, int bz) {
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by, bz).setType(Material.DARK_OAK_SLAB, false);
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.DARK_OAK_PLANKS, false);
        }
        world.getBlockAt(bx - 4, by, bz + 1).setType(Material.BARREL, false);
        world.getBlockAt(bx + 4, by, bz + 1).setType(Material.BARREL, false);
        world.getBlockAt(bx, by, bz + 2).setType(Material.CAKE, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "ration table", List.of(
                        "warm bread was issued after the road was already closed.",
                        "that means the warm-town story was written to explain an absence."
                ));
    }

    private void buildHoldThirdBayCore(World world, int bx, int by, int bz) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean pit = Math.abs(dx) <= 2 && Math.abs(dz) <= 1;
                if (pit) {
                    world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.SCULK, false);
                    world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.AIR, false);
                } else {
                    world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.CRACKED_DEEPSLATE_BRICKS, false);
                }
            }
        }
        for (int dx = -5; dx <= 5; dx++) {
            world.getBlockAt(bx + dx, by, bz + 3).setType(Material.BLACK_CONCRETE, false);
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.SCULK_SENSOR, false);
        Block coldLamp = world.getBlockAt(bx, by, bz - 3);
        coldLamp.setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (coldLamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            coldLamp.setBlockData(light, false);
        }
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "third bay mark", List.of(
                        "mark 33: the line broke downward here.",
                        "this is not a road.",
                        "the lamp was set apart after the floor began answering."
                ));
    }

    private void buildHoldWarmCollapseCore(World world, int bx, int by, int bz) {
        for (int dx = -6; dx <= 6; dx++) {
            int height = 1 + Math.floorMod(dx * 3, 5);
            for (int dy = 0; dy <= height; dy++) {
                world.getBlockAt(bx + dx, by + dy, bz + 5)
                        .setType(dy == height && Math.abs(dx) % 2 == 0
                                ? Material.GRAVEL : Material.COBBLED_DEEPSLATE, false);
            }
        }
        world.getBlockAt(bx - 3, by, bz).setType(Material.BARREL, false);
        world.getBlockAt(bx - 2, by, bz).setType(Material.BARREL, false);
        world.getBlockAt(bx - 1, by, bz + 1).setType(Material.DARK_OAK_FENCE, false);
        world.getBlockAt(bx - 2, by, bz + 1).setType(Material.HAY_BLOCK, false);
        Block deadLamp = world.getBlockAt(bx + 3, by + 1, bz + 1);
        deadLamp.setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (deadLamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            deadLamp.setBlockData(light, false);
        }
        placeEvidenceLectern(new Location(world, bx, by, bz + 2), BlockFace.SOUTH,
                "warden 3 closure", List.of(
                        "east market closed under WARDEN-3.",
                        "no bread remained warm.",
                        "all hands returned except the one sent for the lamp."
                ));
    }

    private void buildHoldDeadStallCore(World world, int bx, int by, int bz) {
        world.getBlockAt(bx - 2, by, bz).setType(Material.BARREL, false);
        world.getBlockAt(bx - 1, by, bz).setType(Material.DARK_OAK_SLAB, false);
        world.getBlockAt(bx, by, bz).setType(Material.HAY_BLOCK, false);
        Block lamp = world.getBlockAt(bx + 2, by + 1, bz);
        lamp.setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (lamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            lamp.setBlockData(light, false);
        }
        placeEvidenceLectern(new Location(world, bx - 4, by, bz + 2), BlockFace.SOUTH,
                "dead stall", List.of(
                        "the stall was counted after the owner stopped coming.",
                        "the missing bird was filed as spoilage."
                ));
    }

    private void buildHoldBirdCoopsCore(World world, int bx, int by, int bz) {
        for (int i = 0; i < 4; i++) {
            int x = bx - 6 + (i * 4);
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x, by, bz + dz).setType(Material.OAK_FENCE, false);
                world.getBlockAt(x, by + 1, bz + dz).setType(Material.OAK_TRAPDOOR, false);
                if (dz != 0) world.getBlockAt(x + 1, by, bz + dz).setType(Material.IRON_BARS, false);
            }
            world.getBlockAt(x + 1, by, bz).setType(i == 2 ? Material.GRAY_CARPET : Material.WHITE_CARPET, false);
        }
        world.getBlockAt(bx, by, bz + 3).setType(Material.BARREL, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz + 3), BlockFace.SOUTH,
                "coop count", List.of(
                        "six coops were paid for.",
                        "the seventh mark is not a coop. It is a witness space."
                ));
    }

    private void buildHoldDreadCore(World world, int bx, int by, int bz, String id) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 2;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(edge ? Material.POLISHED_BLACKSTONE_BRICKS
                                : ((dx == 0 || dz == 0) ? Material.SCULK : Material.POLISHED_DEEPSLATE), false);
                if (edge && Math.floorMod(dx + dz, 2) == 0) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                }
            }
        }
        world.getBlockAt(bx, by, bz).setType(Material.SCULK_SENSOR, false);
        world.getBlockAt(bx - 1, by, bz + 1).setType(Material.COBWEB, false);
        world.getBlockAt(bx + 1, by, bz - 1).setType(Material.SOUL_TORCH, false);
        if (id != null && id.contains("figure")) {
            world.getBlockAt(bx - 2, by, bz).setType(Material.REDSTONE_TORCH, false);
            world.getBlockAt(bx - 3, by + 1, bz).setType(Material.BLACKSTONE, false);
        } else if (id != null && id.contains("exit")) {
            world.getBlockAt(bx, by, bz + 1).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(bx - 1, by, bz).setType(Material.BLACK_CANDLE, false);
        }
        placeDreadLabel(new Location(world, bx, by, bz - 3), dreadAnchorLines(id));
    }

    private boolean isHoldNativeChamber(HoldSite row) {
        if (row == null) return false;
        String id = row.id();
        return "prior_camp".equals(id)
                || "failed_accepting".equals(id)
                || "keeper_altar".equals(id)
                || "coop_plate".equals(id);
    }

    private void buildHoldRosettaCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.CHISELED_TUFF);
        for (int i = 0; i < 7; i++) {
            int x = bx - 6 + (i * 2);
            Material mat = i == 6 ? Material.GRAY_CONCRETE : Material.CHISELED_DEEPSLATE;
            world.getBlockAt(x, by, bz).setType(mat, false);
            world.getBlockAt(x, by + 1, bz).setType(i == 6 ? Material.BLACK_CANDLE : Material.WHITE_CANDLE, false);
        }
        placeEvidenceLectern(new Location(world, bx - 7, by, bz + 4), BlockFace.EAST,
                "rosetta cover", List.of(
                        "The runes are not a secret alphabet. They are a clerk's shortcut for things the Keepers already knew.",
                        "Six hands were copied clean. The grey seventh was copied after the room was built.",
                        "Read the stone, then read the copy. The order is the first lie."
                ));
        placeStandingSign(new Location(world, bx + 7, by, bz + 4), BlockFace.WEST,
                new String[]{"six copied", "one added", "low hand", "low truth"});
        world.getBlockAt(bx, by, bz - 2).setType(Material.CHISELED_TUFF, false);
    }

    private void buildHoldReckoningCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_BLACKSTONE_BRICKS);
        for (int dz = -4; dz <= 4; dz += 2) {
            world.getBlockAt(bx, by, bz + dz).setType(dz == 0 ? Material.CHISELED_TUFF : Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx - 3, by, bz + dz).setType(Material.BLACK_CONCRETE, false);
            world.getBlockAt(bx + 3, by, bz + dz).setType(Material.GRAY_CONCRETE, false);
        }
        placeEvidenceLectern(new Location(world, bx - 7, by, bz), BlockFace.EAST,
                "reckoning copy", List.of(
                        "The reckoning stone repeats the Rosetta, but the line has been turned toward judgment.",
                        "No single keeper owns the answer. The room asks whether the record can survive being corrected.",
                        "The old hands did not read this standing tall. That was the point."
                ));
        placeStandingSign(new Location(world, bx + 7, by, bz), BlockFace.WEST,
                new String[]{"not trial", "reckoning", "read the", "turned line"});
    }

    private void buildHoldThresholdCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.SCULK);
        for (int dx = -5; dx <= 5; dx++) {
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
            if (Math.abs(dx) >= 3) world.getBlockAt(bx + dx, by, bz).setType(Material.SCULK, false);
        }
        world.getBlockAt(bx, by, bz).setType(Material.REINFORCED_DEEPSLATE, false);
        world.getBlockAt(bx, by + 1, bz).setType(Material.BLACK_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx - 7, by, bz + 2), BlockFace.EAST,
                "threshold note", List.of(
                        "This door was written as a grave so no one would ask who was still moving behind it.",
                        "The date is not prophecy. It is appointment language.",
                        "If the group reaches this alone, the room should feel wrong."
                ));
        placeStandingSign(new Location(world, bx + 7, by, bz + 2), BlockFace.WEST,
                new String[]{"the door", "opens from", "inside", ""});
    }

    private void buildHoldThresholdVaultCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_BLACKSTONE_BRICKS);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 5 || Math.abs(dz) == 3) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.SCULK, false);
                }
            }
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(bx, by, bz).setType(Material.STONE_PRESSURE_PLATE, false);
        for (int[] p : new int[][]{{-3, -2}, {3, -2}, {-3, 2}, {3, 2}}) {
            world.getBlockAt(bx + p[0], by, bz + p[1]).setType(Material.CHISELED_TUFF, false);
        }
        Block sign = world.getBlockAt(bx, by, bz + 4);
        sign.setType(Material.OAK_SIGN, false);
        if (sign.getBlockData() instanceof Rotatable r) {
            r.setRotation(BlockFace.SOUTH);
            sign.setBlockData(r, false);
        }
        setSignLines(sign, false, new String[]{"", "", "", ""});
        placeEvidenceLectern(new Location(world, bx - 7, by, bz + 3), BlockFace.EAST,
                "vault split", List.of(
                        "The vault was built for more than one reader. A whole answer seen by one person is a forged answer.",
                        "Stand together. Read apart. Argue before writing.",
                        "The sign accepts the assembled form, not the feeling of certainty."
                ));
    }

    private void buildHoldAcceptingCore(World world, int bx, int by, int bz) {
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean ring = Math.abs(dx) == 8 || Math.abs(dz) == 6;
                boolean cross = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(ring ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (cross ? Material.SEA_LANTERN : Material.POLISHED_DEEPSLATE), false);
                if (ring && Math.floorMod(dx + dz, 4) == 0) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                }
            }
        }
        world.getBlockAt(bx, by, bz).setType(Material.SEA_LANTERN, false);
        placeEvidenceLectern(new Location(world, bx - 8, by, bz + 5), BlockFace.EAST,
                "accepting floor", List.of(
                        "Accepting was never solitary. The floor listens for a present group, not a perfect one.",
                        "Bow together when the record has made the lie too small to keep.",
                        "The light stays unbroken because no one person is allowed to own it."
                ));
        placeStandingSign(new Location(world, bx + 8, by, bz + 5), BlockFace.WEST,
                new String[]{"whole room", "one lowering", "no hero", ""});
    }

    private void buildHoldKeeperAltarCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_DEEPSLATE);
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by, bz).setType(Material.DEEPSLATE_BRICK_WALL, false);
            if (Math.abs(dx) == 3) world.getBlockAt(bx + dx, by + 1, bz).setType(Material.SOUL_LANTERN, false);
        }
        world.getBlockAt(bx, by + 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
        placeEvidenceLectern(new Location(world, bx - 6, by, bz + 3), BlockFace.EAST,
                "last keeper", List.of(
                        "The altar is not worship. It is where the keepers stopped pretending procedure was mercy.",
                        "A name can be restored only after the room admits it was removed.",
                        "Leave space for silence here. The next room is not a puzzle panel."
                ));
    }

    private void buildHoldCoopPlateCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_DEEPSLATE);
        world.getBlockAt(bx, by - 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(bx, by, bz).setType(Material.STONE_PRESSURE_PLATE, false);
        world.getBlockAt(bx + 3, by, bz).setType(Material.CHISELED_TUFF, false);
        world.getBlockAt(bx - 3, by, bz).setType(Material.CHISELED_TUFF, false);
        placeStandingSign(new Location(world, bx, by, bz + 4), BlockFace.SOUTH,
                new String[]{"one foot", "one mark", "one witness", ""});
        placeEvidenceLectern(new Location(world, bx - 7, by, bz + 2), BlockFace.EAST,
                "witness plate", List.of(
                        "One body on the plate is only pressure.",
                        "A second hand on the mark is testimony.",
                        "The room opens for coordination, not speed."
                ));
    }

    private void buildHoldUnwritingCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.SCULK);
        for (int dx = -6; dx <= 6; dx++) {
            world.getBlockAt(bx + dx, by, bz - 3).setType(dx == 0 ? Material.SCULK_SHRIEKER : Material.BLACK_CONCRETE, false);
            if (Math.abs(dx) == 6) world.getBlockAt(bx + dx, by + 1, bz - 3).setType(Material.SOUL_LANTERN, false);
        }
        world.getBlockAt(bx, by, bz).setType(Material.SCULK_SHRIEKER, false);
        placeEvidenceLectern(new Location(world, bx - 8, by, bz + 4), BlockFace.EAST,
                "unwriting", List.of(
                        "The missing name was not lost. It was made administratively blank.",
                        "Restore is not forgiveness. Erase is not mercy. Both are records.",
                        "After the choice, the last act is whether the record is allowed to stop."
                ));
        placeStandingSign(new Location(world, bx + 8, by, bz + 4), BlockFace.WEST,
                new String[]{"restore", "erase", "then release", ""});
        placeHoldFinaleMarkers(new Location(world, bx - 2, by, bz + 1));
    }

    private void buildHoldStoneReadingFloor(World world, int bx, int by, int bz, Material accent) {
        if (world == null) return;
        Material a = accent == null ? Material.DEEPSLATE_TILES : accent;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean rim = Math.abs(dx) == 7 || Math.abs(dz) == 5;
                boolean center = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (center ? a : Material.POLISHED_DEEPSLATE), false);
                if (rim && Math.floorMod(dx + dz, 5) == 0) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                }
            }
        }
    }

    private void placeHoldFinaleMarkers(Location base) {
        if (base == null || base.getWorld() == null) return;
        var seventhKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.SeventhChoiceListener.PDC_SEVENTH_CHOICE);
        var releaseKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.ReleaseRiteListener.PDC_RELEASE);
        for (org.bukkit.entity.ArmorStand old : base.getWorld().getNearbyEntitiesByType(
                org.bukkit.entity.ArmorStand.class, base, 6.0)) {
            try {
                var pdc = old.getPersistentDataContainer();
                if (pdc.has(seventhKey, org.bukkit.persistence.PersistentDataType.STRING)
                        || pdc.has(releaseKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    old.remove();
                }
            } catch (Throwable ignored) { }
        }
        Object[][] markers = {
                {seventhKey, "restore", "Read the blank back"},
                {seventhKey, "erase", "Leave the blank blank"},
                {releaseKey, "release", "Close the record"}
        };
        for (int i = 0; i < markers.length; i++) {
            Location at = base.clone().add(i * 2.0, 0, 0);
            try {
                var as = (org.bukkit.entity.ArmorStand)
                        at.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.ARMOR_STAND);
                as.customName(net.kyori.adventure.text.Component.text(
                        (String) markers[i][2], net.kyori.adventure.text.format.NamedTextColor.GRAY));
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setBasePlate(true);
                as.setInvulnerable(true);
                as.setPersistent(true);
                as.getPersistentDataContainer().set((org.bukkit.NamespacedKey) markers[i][0],
                        org.bukkit.persistence.PersistentDataType.STRING, (String) markers[i][1]);
            } catch (Throwable ignored) { }
        }
    }

    private void shapeHoldKeeperApse(Location loc, HoldSite row) {
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        BlockFace back = front.getOppositeFace();
        for (int a = -6; a <= 6; a++) {
            for (int d = -1; d <= 5; d++) {
                int x = bx + (back.getModX() == 0 ? a : -back.getModX() * d);
                int z = bz + (back.getModZ() == 0 ? a : -back.getModZ() * d);
                boolean rear = d == 5;
                boolean side = Math.abs(a) == 6;
                world.getBlockAt(x, by - 1, z).setType(Math.abs(a) <= 2
                        ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE, false);
                for (int dy = 0; dy <= 7; dy++) {
                    Block block = world.getBlockAt(x, by + dy, z);
                    if (rear || side || dy == 7) {
                        block.setType(dy == 7 ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.floorMod(a + d + dy, 5) == 0
                                ? Material.CHISELED_DEEPSLATE : Material.DEEPSLATE_BRICKS), false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void shapeHoldWorkTableBay(Location loc, HoldSite row, Material accent) {
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -4; dz <= 5; dz++) {
                boolean rim = Math.abs(dx) == 5 || dz == -4 || dz == 5;
                boolean frontDoor = isHoldBayDoor(front, dx, dz, 5, 5, 3);
                boolean wall = rim && !frontDoor;
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(
                        rim ? Material.POLISHED_BLACKSTONE_BRICKS : accent, false);
                for (int dy = 0; dy <= 6; dy++) {
                    world.getBlockAt(bx + dx, by + dy, bz + dz)
                            .setType((wall || dy == 6) ? Material.DEEPSLATE_BRICKS : Material.AIR, false);
                }
            }
        }
        for (int dx : new int[]{-4, 0, 4}) {
            world.getBlockAt(bx + dx, by, bz + 4).setType(Material.DARK_OAK_SLAB, false);
            world.getBlockAt(bx + dx, by + 1, bz + 4).setType(Material.BLACK_CANDLE, false);
        }
    }

    private void shapeHoldEvidenceBay(Location loc, HoldSite row, Material accent) {
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        BlockFace back = front.getOppositeFace();
        for (int a = -7; a <= 7; a++) {
            for (int d = -1; d <= 6; d++) {
                int x = bx + (back.getModX() == 0 ? a : -back.getModX() * d);
                int z = bz + (back.getModZ() == 0 ? a : -back.getModZ() * d);
                boolean rear = d == 6;
                world.getBlockAt(x, by - 1, z).setType(Math.abs(a) >= 6
                        ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
                for (int dy = 0; dy <= 8; dy++) {
                    Block block = world.getBlockAt(x, by + dy, z);
                    if (rear || Math.abs(a) == 7 || dy == 8) {
                        block.setType(dy == 8 ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (rear && (dy == 2 || dy == 5) ? accent : Material.DEEPSLATE_BRICKS), false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void shapeHoldLowThresholdBay(Location loc, HoldSite row) {
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean rim = Math.abs(dx) == 6 || Math.abs(dz) == 5;
                boolean frontDoor = isHoldBayDoor(front, dx, dz, 6, 5, 4);
                boolean wall = rim && !frontDoor;
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(
                        Math.abs(dx) <= 1 ? Material.DEEPSLATE_TILES
                                : (rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy <= 6; dy++) {
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (wall || dy == 6) {
                        block.setType(dy == 6 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            world.getBlockAt(bx + dx, by + 3, bz - 4).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        }
    }

    private void shapeHoldLowerBay(Location loc, HoldSite row) {
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 11) continue;
                boolean rim = Math.abs(dx) + Math.abs(dz) >= 11;
                boolean frontDoor = isHoldBayDoor(front, dx, dz, 7, 7, 4);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(
                        Math.floorMod(dx + dz, 3) == 0 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                for (int dy = 0; dy <= 8; dy++) {
                    if ((rim && !frontDoor) || dy == 8) {
                        world.getBlockAt(bx + dx, by + dy, bz + dz)
                                .setType(dy == 8 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.BLACKSTONE, false);
                    } else {
                        world.getBlockAt(bx + dx, by + dy, bz + dz).setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void ensureHoldAnchorVisual(Site site, Location loc) {
        if (site == null || loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        String id = site.id();
        String type = site.type();
        if ("rune_rosetta".equals(id)) {
            placeHoldAnchorLantern(world, bx + 4, by, bz - 4, Material.CHISELED_DEEPSLATE);
            world.getBlockAt(bx + 3, by, bz - 4).setType(Material.AMETHYST_BLOCK, false);
            return;
        }
        if ("keeper_altar".equals(type)) {
            placeHoldAnchorLantern(world, bx + 2, by, bz - 3, Material.POLISHED_DEEPSLATE);
            world.getBlockAt(bx - 2, by, bz + 2).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx - 2, by + 1, bz + 2).setType(Material.BLACK_CANDLE, false);
            return;
        }
        if ("coop_plate".equals(type)) {
            placeHoldAnchorLantern(world, bx - 2, by, bz - 3, Material.CHISELED_TUFF);
            world.getBlockAt(bx + 2, by, bz + 1).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx + 2, by + 1, bz + 1).setType(Material.BLACK_CANDLE, false);
        }
    }

    private void placeHoldAnchorLantern(World world, int x, int y, int z, Material support) {
        if (world == null) return;
        world.getBlockAt(x, y, z).setType(support == null ? Material.POLISHED_DEEPSLATE : support, false);
        world.getBlockAt(x, y + 1, z).setType(Material.SOUL_LANTERN, false);
    }

    private void stabilizeHoldAuditAnchor(String id, Block anchor) {
        if (id == null || anchor == null || !anchor.getType().isAir()) return;
        Material marker = switch (id) {
            case "undercroft_seal" -> Material.GRAY_CARPET;
            case "forgotten_mouth" -> Material.MOSS_CARPET;
            case "school_stand" -> Material.WHITE_CARPET;
            case "warm_town_collapse" -> Material.BLACK_CARPET;
            default -> null;
        };
        if (marker != null) anchor.setType(marker, false);
    }

    private String holdLoreSeed(String id) {
        return "the_far_water".equals(id) ? "far_water" : id;
    }

    private void enhanceHoldVisual(Site site, Location loc) {
        if (site == null || loc == null || loc.getWorld() == null) return;
        if ("rune_rosetta".equals(site.id())) {
            placeHoldWitnessRib(site, loc, -3, 3);
            return;
        }
        if (!needsVisualLight(site) && !isMajorVisualSite(site)) return;
        int hash = Math.floorMod(site.id().hashCode(), 4);
        int ox = (hash & 1) == 0 ? -3 : 3;
        int oz = (hash & 2) == 0 ? -3 : 3;
        placeHoldWitnessRib(site, loc, ox, oz);
    }

    private void placeHoldWitnessRib(Site site, Location loc, int ox, int oz) {
        if (site == null || loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        for (int dy = 0; dy <= 2; dy++) {
            world.getBlockAt(bx + ox, by + dy, bz + oz)
                    .setType(dy == 2 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
        }
        world.getBlockAt(bx + ox, by + 3, bz + oz).setType(Material.SOUL_LANTERN, false);
        if (needsFocalObject(site)) {
            Block focus = world.getBlockAt(bx + ox - Integer.signum(ox), by, bz + oz);
            if (focus.getType().isAir()) {
                focus.setType(Material.BLACK_CANDLE, false);
            }
        }
    }

    private void trimSignsNear(Location loc, int radius, int keep) {
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        List<Block> signs = new ArrayList<>();
        int r = Math.max(2, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (block.getState() instanceof Sign) signs.add(block);
                }
            }
        }
        if (signs.size() <= keep) return;
        signs.sort(Comparator.comparingInt(b -> Math.abs(b.getX() - bx) + Math.abs(b.getZ() - bz)
                + Math.abs(b.getY() - by)));
        for (int i = keep; i < signs.size(); i++) {
            signs.get(i).setType(Material.AIR, false);
        }
    }

    private void buildHoldGeologyEnvelope(World world, int bx, int by, int bz) {
        // The old generator rewrote a 40M+ block cuboid before carving it again. Besides freezing a
        // live server, that made a rebuild preserve arbitrary old deepslate structures because the
        // fill pass skipped materials that looked Hold-like. The grand plan owns explicit 3-block
        // floors, wall bands, and roofs for every hall/corridor instead, so caves and fluids are
        // excluded by the authored shells rather than by replacing an entire geological volume.
        if (world == null) return;
    }

    private boolean isHoldGeologyFillMaterial(Material material) {
        return material == Material.DEEPSLATE
                || material == Material.COBBLED_DEEPSLATE
                || material == Material.POLISHED_DEEPSLATE
                || material == Material.DEEPSLATE_BRICKS
                || material == Material.CRACKED_DEEPSLATE_BRICKS
                || material == Material.DEEPSLATE_TILES
                || material == Material.TUFF
                || material == Material.SMOOTH_BASALT
                || material == Material.BLACKSTONE
                || material == Material.POLISHED_BLACKSTONE
                || material == Material.POLISHED_BLACKSTONE_BRICKS;
    }

    private Material holdGeologyMaterial(int x, int y, int z, int bx, int by, int bz) {
        int relZ = z - bz;
        int h = holdHash(x, y, z);
        if (relZ > 142 && h % 9 == 0) return Material.POLISHED_BLACKSTONE;
        if (relZ > 142 && h % 5 == 0) return Material.BLACKSTONE;
        if (y < by - 12 && h % 7 == 0) return Material.COBBLED_DEEPSLATE;
        if (h % 31 == 0) return Material.SMOOTH_BASALT;
        if (h % 23 == 0) return Material.TUFF;
        if (h % 17 == 0) return Material.COBBLED_DEEPSLATE;
        if (h % 13 == 0) return Material.CRACKED_DEEPSLATE_BRICKS;
        return Material.DEEPSLATE;
    }

    private int holdHash(int x, int y, int z) {
        int n = x * 73428767 ^ y * 912271 ^ z * 42331;
        n ^= (n >>> 13);
        n *= 1274126177;
        n ^= (n >>> 16);
        return Math.floorMod(n, 1000);
    }

    private void buildHoldSurfaceMouth(Location surfaceMouth, Location base) {
        if (surfaceMouth == null || base == null || surfaceMouth.getWorld() == null
                || base.getWorld() == null || surfaceMouth.getWorld() != base.getWorld()) return;
        World world = surfaceMouth.getWorld();
        int sx = surfaceMouth.getBlockX();
        int sy = surfaceMouth.getBlockY();
        int sz = surfaceMouth.getBlockZ();
        int bottomY = base.getBlockY();
        int bz = base.getBlockZ();
        int entryZ = Math.max(sz + 96, bz - 186);
        int total = Math.max(1, entryZ - sz);

        int minChunkX = (sx - 48) >> 4;
        int maxChunkX = (sx + 48) >> 4;
        int minChunkZ = (sz - 48) >> 4;
        int maxChunkZ = (entryZ + 86) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                world.getChunkAt(cx, cz).load(true);
            }
        }

        buildHoldSurfaceApron(world, sx, sy, sz);
        int previousY = sy;
        for (int z = sz; z <= entryZ; z++) {
            int step = z - sz;
            double t = step / (double) total;
            int y = (int) Math.round(sy + ((bottomY - sy) * t));
            buildHoldEntryStairSlice(world, sx, y, z, step);
            if (y < previousY) setHoldEntryStairTread(world, sx, y, z, 16);
            previousY = y;
        }
        for (int z = entryZ + 1; z <= bz - 132; z++) {
            buildHoldEntryStairSlice(world, sx, bottomY, z, z - sz);
        }
        buildHoldEntryLanding(world, sx, bottomY, bz - 142);
        buildHoldCorridorZ(world, sx, bottomY, bz - 150, bz - 96, 20, 16);
        buildHoldArch(world, sx, bottomY, entryZ + 4, 19, 15);
        buildHoldArch(world, sx, bottomY, bz - 126, 22, 16);
        placeStandingSign(new Location(world, sx - 5, sy, sz - 6), BlockFace.SOUTH,
                new String[]{"RETURN MOUTH", "count before", "descending", ""});
        placeStandingSign(new Location(world, sx + 6, bottomY, entryZ + 6), BlockFace.WEST,
                new String[]{"the stair", "ends where", "the count", "turns"});
    }

    private void registerHoldRegion(String worldName, int bx, int by, int bz, Location surfaceMouth) {
        int vertical = 148;
        if (surfaceMouth != null) {
            vertical = Math.max(vertical, Math.abs(surfaceMouth.getBlockY() - (by + 8)) + 36);
        }
        plugin.registerRuntimeSite(new Site(HOLD_REGION_SITE_ID, "hold_region", worldName,
                (double) bx, (double) (by + 2), (double) (bz + 245), 472, vertical,
                true, true, null, false));
        if (surfaceMouth != null && surfaceMouth.getWorld() != null) {
            int sx = surfaceMouth.getBlockX();
            int sy = surfaceMouth.getBlockY();
            int sz = surfaceMouth.getBlockZ();
            int entryZ = Math.max(sz + 96, bz - 186);
            int centerZ = (sz + entryZ) / 2;
            int centerY = (sy + by) / 2;
            int radius = Math.max(126, Math.abs(entryZ - sz) / 2 + 58);
            int entryVertical = Math.max(96, Math.abs(sy - by) / 2 + 52);
            plugin.registerRuntimeSite(new Site(HOLD_ENTRY_REGION_SITE_ID, "hold_region", worldName,
                    (double) sx, (double) centerY, (double) centerZ, radius, entryVertical,
                    true, true, null, false));
        }
    }

    private void buildHoldSurfaceApron(World world, int sx, int sy, int sz) {
        if (world == null) return;
        for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -16; dz <= 30; dz++) {
                double d = Math.sqrt((dx * dx) + ((dz - 3) * (dz - 3) * 0.72));
                if (d > 24.2) continue;
                Material floor = d > 18.6 ? Material.MOSSY_COBBLESTONE
                        : (d > 10.0 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(sx + dx, sy - 2, sz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(sx + dx, sy - 1, sz + dz).setType(floor, false);
                for (int dy = 0; dy <= 8; dy++) {
                    boolean mouth = Math.abs(dx) <= 13 && dz >= -3 && dz <= 19;
                    boolean brokenRim = d > 16.4 && d < 23.1 && dy <= 3 && Math.floorMod(dx * 7 + dz, 4) == 0;
                    Block block = world.getBlockAt(sx + dx, sy + dy, sz + dz);
                    if (mouth) {
                        block.setType(Material.AIR, false);
                    } else if (brokenRim) {
                        block.setType(dy == 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS, false);
                    } else if (dy == 0 && d > 10.2 && Math.floorMod(dx - dz, 5) == 0) {
                        block.setType(Material.MOSSY_COBBLESTONE, false);
                    }
                }
            }
        }
        buildHoldArch(world, sx, sy, sz + 8, 15, 10);
    }

    private void buildHoldEntryStairSlice(World world, int cx, int y, int z, int step) {
        if (world == null) return;
        int outer = 24;
        int inner = 16;
        int height = 16;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dy = -3; dy <= height + 2; dy++) {
                boolean walk = Math.abs(dx) <= inner && dy >= 0 && dy < height;
                boolean floor = Math.abs(dx) <= inner && dy == -1;
                boolean ceiling = Math.abs(dx) <= inner && dy == height;
                boolean sideFace = Math.abs(dx) >= inner + 1 && dy >= 0 && dy <= height;
                Block block = world.getBlockAt(cx + dx, y + dy, z);
                if (floor) {
                    Material floorMat = Math.floorMod(step + dx, 9) == 0
                            ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE;
                    block.setType(floorMat, false);
                } else if (walk) {
                    block.setType(Material.AIR, false);
                } else if (ceiling || sideFace) {
                    block.setType((step % 10 == 0 || Math.abs(dx) == outer)
                            ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS, false);
                } else {
                    block.setType(holdGeologyMaterial(cx + dx, y + dy, z, cx, y, z), false);
                }
            }
        }
        if (step % 10 == 0) {
            Material chain = materialOr(Material.IRON_BARS, "CHAIN");
            world.getBlockAt(cx - inner + 1, y + height - 1, z).setType(chain, false);
            world.getBlockAt(cx - inner + 1, y + height - 2, z).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(cx + inner - 1, y + height - 1, z).setType(chain, false);
            world.getBlockAt(cx + inner - 1, y + height - 2, z).setType(Material.SOUL_LANTERN, false);
        }
        if (step % 18 == 0) {
            for (int dx : new int[]{-20, 20}) {
                for (int dy = -1; dy <= height + 1; dy++) {
                    world.getBlockAt(cx + dx, y + dy, z).setType(dy == height + 1
                            ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
                }
            }
        }
    }

    private void setHoldEntryStairTread(World world, int cx, int y, int z, int halfWidth) {
        if (world == null) return;
        for (int dx = -Math.max(2, halfWidth) + 1; dx <= Math.max(2, halfWidth) - 1; dx++) {
            Block stair = world.getBlockAt(cx + dx, y - 1, z);
            stair.setType(Material.POLISHED_DEEPSLATE_STAIRS, false);
            if (stair.getBlockData() instanceof Directional directional) {
                directional.setFacing(BlockFace.NORTH);
                stair.setBlockData(directional, false);
            }
        }
    }

    private void buildHoldEntryLanding(World world, int cx, int y, int cz) {
        if (world == null) return;
        for (int dx = -34; dx <= 34; dx++) {
            for (int dz = -24; dz <= 30; dz++) {
                boolean rim = Math.abs(dx) == 34 || dz == -24 || dz == 30;
                boolean archPost = Math.abs(dx) == 22 && dz >= -12 && dz <= 24;
                world.getBlockAt(cx + dx, y - 2, cz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(cx + dx, y - 1, cz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.abs(dx) <= 2 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy <= 17; dy++) {
                    Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    if (rim || archPost || dy == 17) {
                        Material material = (dy == 17 || rim)
                                ? Material.POLISHED_BLACKSTONE_BRICKS
                                : Material.DEEPSLATE_BRICKS;
                        block.setType(material, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (int dz : new int[]{-16, 0, 16, 28}) {
            buildHoldArch(world, cx, y, cz + dz, 22, 16);
        }
        placeStandingSign(new Location(world, cx - 10, y, cz + 8), BlockFace.EAST,
                new String[]{"return stair", "behind you", "city below", ""});
        placeStandingSign(new Location(world, cx + 10, y, cz + 8), BlockFace.WEST,
                new String[]{"do not dig", "the hold", "opens by", "record"});
    }

    private void placeHoldPrologueEcho(World world, int bx, int by, int bz) {
        if (world == null) return;
        placeEvidenceLectern(new Location(world, bx - 14, by, bz - 102), BlockFace.EAST,
                "covered copy", List.of(
                        "This is a copy, not the first report.\n\nThe real first report belongs above, where the living first allow themselves to be counted.",
                        "The Hold kept only a cover mark: the record was carried underground after the opening, not before it.",
                        "A descent without the first report is a descent without a name. Return to the mouth before reading the city."
                ));
        placeDecorativeBookshelf(world.getBlockAt(bx - 16, by, bz - 102), 83);
        placeStandingSign(new Location(world, bx - 18, by, bz - 104), BlockFace.EAST,
                new String[]{"copied cover", "original", "stays above", ""});
    }

    private void placeHoldDistrictRecords(World world, int bx, int by, int bz) {
        if (world == null) return;
        placeHoldRecordStation(world, bx + 12, by, bz - 142, BlockFace.WEST,
                "mouth register", List.of(
                        "Entry-mouth census.\n\nSix signed before the covered descent. A seventh mark was added later in grey ink and never matched a hand.",
                        "Do not call the grey line a prophecy. It is a correction made after the stair was sealed.",
                        "Later copies turned the six hand marks into rows for entry. One row stayed blank, but the lock still counted it.",
                        "The first report stays above because the first count was public. The Hold keeps the copy that proves the public story changed."
                ), 101);
        placeStandingSign(new Location(world, bx + 16, by, bz - 142), BlockFace.WEST,
                new String[]{"first count", "above", "copy below", ""});

        placeHoldRecordStation(world, bx - 22, by, bz - 16, BlockFace.EAST,
                "court census", List.of(
                        "Keeper court seating.\n\nVaun, Mara, Sella, Orin, Brann, Iss. Six chairs were cut into the ring before any lower work began.",
                        "Margin correction: one place was not cut. It was reserved by leaving the count unfinished.",
                        "When a player finds seven where six were built, trust the physical room over the speech."
                ), 117);
        placeStandingSign(new Location(world, bx - 26, by, bz - 16), BlockFace.EAST,
                new String[]{"six seats", "one margin", "count the", "stone"});

        placeHoldRecordStation(world, bx - 48, by, bz + 82, BlockFace.SOUTH,
                "intake rail", List.of(
                        "Archive intake rail.\n\nSchool, cistern, watch, shelf, water. These were separated so no single reader could see the lower pattern at once.",
                        "Read the side rooms as evidence, not decoration. Each one changes who looks guilty and who only looks useful.",
                        "Mara filed the copies by consequence. Brann filed the doors by convenience."
                ), 131);
        placeStandingSign(new Location(world, bx - 44, by, bz + 81), BlockFace.SOUTH,
                new String[]{"records split", "on purpose", "follow what", "changed"});

        placeHoldRecordStation(world, bx + 10, by, bz + 82, BlockFace.WEST,
                "prior roster", List.of(
                        "Prior accepting roster.\n\nSix names copied clean. Six keeper answers filed. Six tokens prepared.",
                        "Seventh line: no witness.\n\nDo not correct this to no seventh. The failed run had no one outside the finish.",
                        "The camp beyond this gate is not locked because it is sacred. It is locked because the same mistake should not be rehearsed twice.",
                        "Open condition to file: no witness."
                ), 139);
        placeStandingSign(new Location(world, bx + 14, by, bz + 82), BlockFace.WEST,
                new String[]{"prior run", "six ready", "no witness", ""});

        placeHoldRecordStation(world, bx + 48, by, bz + 82, BlockFace.SOUTH,
                "closure docket", List.of(
                        "Market closure docket WARDEN-3.\n\nPublic reason: unsafe wall. Private reason: the ration account proved light was moved before the collapse.",
                        "The warm-town story depends on smoke, but the ledgers depend on delivery. Compare which one had to be rewritten.",
                        "If the market feels too ordinary, keep reading. Ordinary records are how the lie survived."
                ), 149);
        placeStandingSign(new Location(world, bx + 44, by, bz + 81), BlockFace.SOUTH,
                new String[]{"warden file", "counts goods", "before smoke", ""});

        placeHoldRecordStation(world, bx - 12, by - 3, bz + 260, BlockFace.EAST,
                "lamp count", List.of(
                        "Lampworks maintenance.\n\nFirst line kept. Second line borrowed. Third line went dry and was still marked ready.",
                        "Complaint note: a dark stand is not a missing stand. Do not break the wall for it. Bring light to the cup.",
                        "The black step is not a warning sign. It is an accounting mark for the place where the lower work stops being public.",
                        "Do not hurry past the descent. The lamps tell who paid, who carried, and who pretended not to know."
                ), 173);
        placeStandingSign(new Location(world, bx - 16, by - 3, bz + 260), BlockFace.EAST,
                new String[]{"lampworks", "counts debt", "not light", ""});

        placeHoldRecordStation(world, bx - 18, by - 28, bz + 348, BlockFace.EAST,
                "threshold hands", List.of(
                        "Threshold work note.\n\nThree actions were required so no single keeper could make the last door look like consent.",
                        "Plate. Name. Word. The order matters less than the fact that the room hears more than one person.",
                        "A group should argue here. If everyone agrees too quickly, they probably missed the earlier contradiction."
                ), 191);
        placeStandingSign(new Location(world, bx - 22, by - 28, bz + 348), BlockFace.EAST,
                new String[]{"three hands", "before the", "last warm", "floor"});

        placeHoldRecordStation(world, bx + 110, by - 28, bz + 350, BlockFace.WEST,
                "side hush", List.of(
                        "Side hush report.\n\nThis passage is not part of the formal count. That is why the formal count keeps failing.",
                        "A witness used the word elsewhere three times and crossed it out twice. The uncrossed word is the useful one.",
                        "Bring this back to the court only after the lower lamps make the first lie too small."
                ), 211);
        placeStandingSign(new Location(world, bx + 114, by - 28, bz + 350), BlockFace.WEST,
                new String[]{"not counted", "still true", "bring it", "back"});
    }

    private void placeHoldRecordStation(World world, int x, int y, int z, BlockFace facing,
                                        String title, List<String> pages, int shelfSeed) {
        if (world == null) return;
        BlockFace front = facing == null ? BlockFace.SOUTH : facing;
        placeEvidenceLectern(new Location(world, x, y, z), front, title, pages);
        BlockFace back = front.getOppositeFace();
        BlockFace side = holdLeftOf(front);
        for (int s : new int[]{-1, 1}) {
            int sx = x + back.getModX() + (side.getModX() * s);
            int sz = z + back.getModZ() + (side.getModZ() * s);
            placeDecorativeBookshelf(world.getBlockAt(sx, y, sz), shelfSeed + s);
            placeDecorativeBookshelf(world.getBlockAt(sx, y + 1, sz), shelfSeed + 9 + s);
        }
    }

    private BlockFace holdLeftOf(BlockFace facing) {
        return switch (facing == null ? BlockFace.SOUTH : facing) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.EAST;
        };
    }

    private void buildHoldSpine(World world, int bx, int by, int bz) {
        if (world == null) return;

        // One civic complex, eight authored shells. Each shell owns its floor, wall band, and roof;
        // corridors are cut only after every shell exists, so the only holes through a district wall
        // are declared approaches and gate portals.
        buildGrandHoldVault(world, bx, by, bz - 120, 56, 43, 27,
                Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE);
        buildGrandHoldVault(world, bx, by, bz, 118, 87, 35,
                Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES);
        buildGrandHoldVault(world, bx - 125, by, bz + 162, 82, 98, 25,
                Material.DEEPSLATE_TILES, Material.DARK_PRISMARINE);
        buildGrandHoldVault(world, bx + 125, by, bz + 162, 82, 98, 25,
                Material.POLISHED_DEEPSLATE, Material.TUFF_BRICKS);
        buildGrandHoldVault(world, bx, by, bz + 228, 70, 38, 25,
                Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE);
        buildGrandHoldVault(world, bx, by - 28, bz + 405, 122, 94, 31,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_DEEPSLATE);
        buildGrandHoldVault(world, bx, by - 28, bz + 520, 110, 74, 39,
                Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES);
        buildGrandHoldVault(world, bx, by - 28, bz + 625, 74, 54, 29,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.SCULK);

        // Keeper chapels are real buildings within the court, not loose props on its floor.
        for (int[] chapel : new int[][]{
                {-82, -42}, {82, -42}, {82, 0}, {82, 42}, {-82, 42}, {-82, 0}
        }) {
            buildGrandHoldVault(world, bx + chapel[0], by, bz + chapel[1], 23, 20, 20,
                    Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE);
            int sign = chapel[0] < 0 ? -1 : 1;
            buildHoldCorridorX(world, by, bz + chapel[1], bx + (sign * 18),
                    bx + (sign * 63), 5, 10);
        }
        // Brann's start/end sites bound one continuous, roofed sculk passage—not open court floor.
        buildHoldCorridorX(world, by, bz + 55, bx - 100, bx - 64, 4, 7);

        // Processional route and the two evidence loops.
        buildHoldCorridorZ(world, bx, by, bz - 176, bz - 76, 11, 16);
        buildHoldCorridorZ(world, bx, by, bz - 84, bz - 66, 10, 14);
        buildHoldCorridorZ(world, bx, by, bz + 64, bz + 94, 11, 14);
        buildHoldCorridorX(world, by, bz + 88, bx - 132, bx - 22, 7, 11);
        buildHoldCorridorX(world, by, bz + 88, bx + 22, bx + 132, 7, 11);
        buildHoldCorridorX(world, by, bz + 235, bx - 132, bx - 42, 8, 12);
        buildHoldCorridorX(world, by, bz + 235, bx + 42, bx + 132, 8, 12);
        buildHoldCorridorZ(world, bx, by, bz + 205, bz + 260, 12, 15);

        // Broad Lampworks descent: one block down per three blocks forward, always roofed.
        for (int step = 0; step <= 84; step++) {
            int z = bz + 250 + step;
            int y = by - Math.min(28, step / 3);
            buildHoldRampSlice(world, bx, y, z, 19, 15, step);
        }
        buildHoldCorridorZ(world, bx, by - 28, bz + 326, bz + 350, 12, 15);
        buildHoldCorridorZ(world, bx, by - 28, bz + 448, bz + 480, 13, 17);
        buildHoldCorridorZ(world, bx, by - 28, bz + 552, bz + 590, 11, 14);

        // Permanent crosswalls make the authored portal the only path between progression bands.
        buildGrandGatehouseWall(world, bx, by, bz - 160, true, 19, 16);
        buildGrandGatehouseWall(world, bx, by, bz - 75, true, 66, 27);
        buildGrandGatehouseWall(world, bx, by, bz + 72, true, 72, 25);
        buildGrandGatehouseWall(world, bx, by, bz + 88, true, 19, 14);
        buildGrandGatehouseWall(world, bx, by, bz + 250, true, 76, 24);
        buildGrandGatehouseWall(world, bx, by - 28, bz + 330, true, 82, 24);
        buildGrandGatehouseWall(world, bx, by - 28, bz + 470, true, 96, 30);

        // The optional Dread is a closed loop wholly inside the lower progression band.
        buildHoldCorridorX(world, by - 28, bz + 360, bx + 96, bx + 170, 5, 9);
        buildHoldCorridorZ(world, bx + 170, by - 28, bz + 360, bz + 440, 5, 9);
        buildHoldCorridorX(world, by - 28, bz + 440, bx + 118, bx + 170, 5, 9);
        buildHoldCorridorZ(world, bx + 118, by - 28, bz + 430, bz + 468, 5, 9);
        buildGrandGatehouseWall(world, bx + 120, by - 28, bz + 360, false, 16, 12);

        dressGrandKeeperCourt(world, bx, by, bz);
        dressGrandEvidenceWings(world, bx, by, bz);
        dressGrandLampworks(world, bx, by, bz);
        dressGrandLowerHold(world, bx, by, bz);
        placeHoldDistrictRecords(world, bx, by, bz);
    }

    /** Build a fully sealed rounded civic vault. The wall band and roof are always at least 3 blocks
     * thick; the interior is cleared only inside that owned shell. */
    private void buildGrandHoldVault(World world, int cx, int y, int cz, int radiusX, int radiusZ,
                                     int height, Material floorA, Material floorB) {
        if (world == null) return;
        int rx = Math.max(14, radiusX);
        int rz = Math.max(14, radiusZ);
        int h = Math.max(12, height);
        double rx2 = (double) rx * rx;
        double rz2 = (double) rz * rz;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double n = (dx * dx) / rx2 + (dz * dz) / rz2;
                if (n > 1.0) continue;
                boolean wallBand = n >= 0.84;
                Material floor = n > 0.76 ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (Math.floorMod(dx + dz, 4) == 0 ? floorB : floorA);
                for (int fy = -3; fy <= -1; fy++) {
                    world.getBlockAt(cx + dx, y + fy, cz + dz)
                            .setType(fy == -1 ? floor : Material.DEEPSLATE, false);
                }
                for (int dy = 0; dy < h; dy++) {
                    Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    if (wallBand) {
                        Material wall = (dy == 0 || dy >= h - 3)
                                ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.floorMod(dx + dz + dy, 11) == 0
                                ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS);
                        block.setType(wall, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
                for (int roof = 0; roof < 3; roof++) {
                    world.getBlockAt(cx + dx, y + h + roof, cz + dz)
                            .setType(roof == 0 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE, false);
                }
            }
        }
        // Monumental ribs establish scale without filling the walkable floor with random pillars.
        for (int z = cz - rz + 14; z <= cz + rz - 14; z += 24) {
            buildHoldVaultRib(world, cx, y, z, Math.max(10, rx - 8), Math.max(10, h - 3));
        }
    }

    private void dressGrandKeeperCourt(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z : new int[]{-58, -24, 12, 48}) {
            buildLowDivider(world, bx, by, bz + z, 34, true);
        }
        for (int[] p : new int[][]{{-30, -62}, {30, -62}, {-30, 58}, {30, 58}}) {
            for (int dy = 0; dy <= 18; dy++) {
                world.getBlockAt(bx + p[0], by + dy, bz + p[1]).setType(
                        dy == 18 ? Material.CHISELED_DEEPSLATE : Material.POLISHED_BASALT, false);
            }
            world.getBlockAt(bx + p[0], by + 16, bz + p[1]).setType(Material.SOUL_LANTERN, false);
        }
        placeGrandFloorLightLineZ(world, bx - 5, by, bz - 72, bz + 70, 12);
        placeGrandFloorLightLineZ(world, bx + 5, by, bz - 72, bz + 70, 12);
        for (int z : new int[]{bz - 42, bz, bz + 42}) {
            placeGrandFloorLightLineX(world, by, z, bx - 70, bx + 70, 12);
        }
    }

    private void dressGrandEvidenceWings(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z : new int[]{100, 136, 172, 208, 240}) {
            buildLowDivider(world, bx - 125, by, bz + z, 50, true);
            buildLowDivider(world, bx + 125, by, bz + z, 50, true);
        }
        for (int z = 92; z <= 236; z += 24) {
            buildArchiveTable(world, bx - 125, by, bz + z);
            buildMarketCounter(world, bx + 125, by, bz + z);
        }
        placeGrandFloorLightLineZ(world, bx - 125, by, bz + 88, bz + 248, 12);
        placeGrandFloorLightLineZ(world, bx + 125, by, bz + 88, bz + 248, 12);
        placeGrandFloorLightLineZ(world, bx, by, bz + 202, bz + 252, 10);
    }

    private void dressGrandLampworks(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int i = 0; i < 3; i++) {
            int z = bz + 270 + (i * 18);
            int y = by - Math.min(28, (z - (bz + 250)) / 3);
            buildLampStand(world, bx - 14 + (i * 14), y, z, i != 0);
        }
        for (int step = 6; step <= 84; step += 12) {
            int z = bz + 250 + step;
            int y = by - Math.min(28, step / 3);
            world.getBlockAt(bx - 8, y - 1, z).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(bx + 8, y - 1, z).setType(Material.SEA_LANTERN, false);
        }
    }

    private void dressGrandLowerHold(World world, int bx, int by, int bz) {
        if (world == null) return;
        int y = by - 28;
        for (int z : new int[]{350, 390, 435, 485, 530, 580, 625}) {
            buildHoldArch(world, bx, y, bz + z, z >= 485 ? 34 : 28, z >= 485 ? 24 : 19);
        }
        buildLowDivider(world, bx, y, bz + 450, 72, true);
        buildLowDivider(world, bx, y, bz + 562, 46, true);
        placeGrandFloorLightLineZ(world, bx - 6, y, bz + 332, bz + 650, 12);
        placeGrandFloorLightLineZ(world, bx + 6, y, bz + 332, bz + 650, 12);
        placeGrandFloorLightLineZ(world, bx + 170, y, bz + 360, bz + 440, 10);
    }

    private void placeGrandFloorLightLineZ(World world, int x, int y, int z1, int z2, int spacing) {
        if (world == null) return;
        int min = Math.min(z1, z2), max = Math.max(z1, z2), step = Math.max(6, spacing);
        for (int z = min; z <= max; z += step) {
            world.getBlockAt(x, y - 1, z).setType(Material.SEA_LANTERN, false);
        }
    }

    private void placeGrandFloorLightLineX(World world, int y, int z, int x1, int x2, int spacing) {
        if (world == null) return;
        int min = Math.min(x1, x2), max = Math.max(x1, x2), step = Math.max(6, spacing);
        for (int x = min; x <= max; x += step) {
            world.getBlockAt(x, y - 1, z).setType(Material.SEA_LANTERN, false);
        }
    }

    private void buildGrandGatehouseWall(World world, int cx, int y, int cz, boolean acrossX,
                                         int halfWidth, int height) {
        if (world == null) return;
        int half = Math.max(10, halfWidth);
        int h = Math.max(10, height);
        for (int a = -half; a <= half; a++) {
            for (int depth = -2; depth <= 5; depth++) {
                for (int dy = -2; dy <= h + 3; dy++) {
                    Block block = holdAxisBlockAt(world, cx, y, cz, acrossX, a, dy, depth);
                    Material material;
                    if (dy <= -1) {
                        material = dy == -1 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE;
                    } else if (dy >= h) {
                        material = dy == h ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE;
                    } else if (Math.abs(a) % 11 <= 1) {
                        material = Material.POLISHED_BASALT;
                    } else {
                        material = Math.floorMod(a + dy + depth, 9) == 0
                                ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS;
                    }
                    block.setType(material, false);
                }
            }
        }
    }

    private void buildHoldCivicVault(World world, int bx, int by, int bz) {
        if (world == null) return;
        carveHoldEllipsoid(world, bx, by + 15, bz - 8, 132, 39, 156);
        floorHoldOval(world, bx, by, bz - 8, 116, 140,
                Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.POLISHED_BLACKSTONE_BRICKS);
        buildHoldTerrace(world, bx, by + 6, bz - 18, -108, 110, -1);
        buildHoldTerrace(world, bx, by + 6, bz - 18, -108, 110, 1);
        buildHoldUpperRibs(world, bx, by, bz);
    }

    private void buildHoldTerrace(World world, int bx, int y, int bz, int z1, int z2, int side) {
        if (world == null) return;
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int outerX = bx + (side * 84);
        int innerX = bx + (side * 54);
        for (int z = bz + minZ; z <= bz + maxZ; z++) {
            for (int x = Math.min(innerX, outerX); x <= Math.max(innerX, outerX); x++) {
                int relX = Math.abs(x - bx);
                boolean rail = relX == 54 || relX == 84;
                world.getBlockAt(x, y - 2, z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(x, y - 1, z).setType(rail
                        ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (Math.floorMod(x + z, 5) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy <= 5; dy++) {
                    Block block = world.getBlockAt(x, y + dy, z);
                    if (rail && dy <= 1) {
                        block.setType(Material.DEEPSLATE_BRICK_WALL, false);
                    } else if (dy == 5 && Math.floorMod(z - bz, 18) == 0 && relX >= 76) {
                        block.setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
            if (Math.floorMod(z - bz, 18) == 0) {
                for (int dy = -1; dy <= 8; dy++) {
                    Material material = dy == 8 ? Material.POLISHED_BLACKSTONE_BRICKS
                            : (dy < 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
                    world.getBlockAt(outerX, y + dy, z).setType(material, false);
                }
                world.getBlockAt(outerX - side, y + 6, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildHoldUpperRibs(World world, int bx, int by, int bz) {
        if (world == null) return;
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int z : new int[]{-144, -120, -96, -72, -48, -24, 0, 24, 48, 72, 96, 120, 144}) {
            int half = 100 - (Math.abs(z) / 6);
            for (int x = bx - half; x <= bx + half; x += 8) {
                int rel = Math.abs(x - bx);
                int crown = by + 30 - Math.max(0, rel / 13);
                for (int dy = 0; dy <= 2; dy++) {
                    world.getBlockAt(x, crown + dy, bz + z)
                            .setType(dy == 2 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS, false);
                }
                if (rel >= half - 8) {
                    world.getBlockAt(x, crown - 1, bz + z).setType(chain, false);
                    world.getBlockAt(x, crown - 2, bz + z).setType(Material.SOUL_LANTERN, false);
                }
            }
        }
    }

    private void buildHoldGrandCivicWorks(World world, int bx, int by, int bz) {
        if (world == null) return;
        buildHoldProcessional(world, bx, by, bz, -170, 172, 34, 18);
        buildHoldProcessional(world, bx, by - 16, bz, 172, 344, 20, 15);
        buildHoldSideGalleryFront(world, bx - 50, by, bz, 30, 192, -1);
        buildHoldSideGalleryFront(world, bx + 50, by, bz, 30, 178, 1);
        buildHoldSideGalleryFront(world, bx - 92, by + 6, bz, -64, 78, -1);
        buildHoldSideGalleryFront(world, bx + 92, by + 6, bz, -64, 78, 1);
        for (int z : new int[]{-160, -138, -116, -92, -68, -44, -20, 8, 36, 66, 96, 126, 154, 172}) {
            buildHoldArch(world, bx, by, bz + z, 22, 16);
        }
        for (int z : new int[]{184, 208, 232, 256, 284, 312, 336}) {
            buildHoldArch(world, bx, by - 16, bz + z, 18, 14);
        }
        for (int z : new int[]{44, 72, 100, 128, 156, 184}) {
            buildHoldArch(world, bx - 92, by, bz + z, 16, 11);
            buildHoldArch(world, bx + 92, by, bz + z, 16, 11);
        }
    }

    private void buildHoldCivicCurtains(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int side : new int[]{-1, 1}) {
            buildHoldCurtainWall(world, bx + (side * 126), by, bz, -164, 24, side, 28);
            buildHoldCurtainWall(world, bx + (side * 106), by - 16, bz, 184, 342, side, 25);
            buildHoldGrandShoulder(world, bx + (side * 44), by, bz - 70, side, 18);
            buildHoldGrandShoulder(world, bx + (side * 44), by, bz + 34, side, 18);
            buildHoldGrandShoulder(world, bx + (side * 34), by, bz + 138, side, 16);
            buildHoldGrandShoulder(world, bx + (side * 28), by - 16, bz + 198, side, 15);
            buildHoldGrandShoulder(world, bx + (side * 28), by - 16, bz + 248, side, 15);
        }
        for (int z : new int[]{-156, -132, -108, -84, -60, -36, -12, 12, 36, 68, 100, 132, 164}) {
            buildHoldVaultRib(world, bx, by, bz + z, 36, 20);
        }
        for (int z : new int[]{196, 224, 252, 280, 308, 336}) {
            buildHoldVaultRib(world, bx, by - 16, bz + z, 24, 16);
        }
    }

    private void buildHoldCurtainWall(World world, int x, int y, int bz, int relZ1, int relZ2, int side, int height) {
        int minZ = Math.min(relZ1, relZ2);
        int maxZ = Math.max(relZ1, relZ2);
        for (int z = bz + minZ; z <= bz + maxZ; z++) {
            boolean pier = Math.floorMod(z - bz, 16) == 0;
            for (int dx = 0; dx <= 2; dx++) {
                int xx = x + (side * dx);
                world.getBlockAt(xx, y - 2, z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(xx, y - 1, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                for (int dy = 0; dy <= height; dy++) {
                    Material material;
                    if (dy == height || pier) {
                        material = Material.POLISHED_BLACKSTONE_BRICKS;
                    } else if (Math.floorMod(z + dy + dx, 9) == 0) {
                        material = Material.CRACKED_DEEPSLATE_BRICKS;
                    } else {
                        material = Material.DEEPSLATE_BRICKS;
                    }
                    world.getBlockAt(xx, y + dy, z).setType(material, false);
                }
            }
            if (pier) {
                world.getBlockAt(x - side, y + height - 3, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildHoldGrandShoulder(World world, int x, int y, int z, int side, int height) {
        for (int dz = -5; dz <= 5; dz++) {
            for (int dx = 0; dx <= 4; dx++) {
                int xx = x + (side * dx);
                world.getBlockAt(xx, y - 2, z + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(xx, y - 1, z + dz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                for (int dy = 0; dy <= height; dy++) {
                    boolean edge = dz == -5 || dz == 5 || dx == 4 || dy == height;
                    Material material = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                            : (Math.floorMod(xx + z + dz + dy, 6) == 0
                            ? Material.CHISELED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
                    world.getBlockAt(xx, y + dy, z + dz).setType(material, false);
                }
            }
        }
    }

    private void buildHoldVaultRib(World world, int cx, int y, int z, int halfWidth, int height) {
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int dx = -halfWidth; dx <= halfWidth; dx += 4) {
            int crown = y + height - Math.max(0, Math.abs(dx) / 7);
            for (int dy = 0; dy <= 2; dy++) {
                world.getBlockAt(cx + dx, crown + dy, z)
                        .setType(dy == 2 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS, false);
            }
            if (Math.abs(dx) >= halfWidth - 8) {
                world.getBlockAt(cx + dx, crown - 1, z).setType(chain, false);
                world.getBlockAt(cx + dx, crown - 2, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildHoldDistrictForecourts(World world, int bx, int by, int bz) {
        if (world == null) return;
        buildHoldGatehouse(world, bx, by, bz - 118, true, 34, 18, 17, Material.CHISELED_DEEPSLATE);
        buildHoldGatehouse(world, bx, by, bz - 72, true, 42, 20, 20, Material.DEEPSLATE_TILES);
        buildHoldGatehouse(world, bx, by, bz + 32, true, 42, 18, 20, Material.CRACKED_DEEPSLATE_BRICKS);
        buildHoldGatehouse(world, bx, by, bz + 138, true, 36, 18, 18, Material.POLISHED_DEEPSLATE);
        buildHoldGatehouse(world, bx + 52, by - 16, bz + 188, false, 22, 16, 15, Material.BLACKSTONE);
        buildHoldGatehouse(world, bx, by - 16, bz + 198, true, 34, 18, 18, Material.SCULK);
        buildHoldGatehouse(world, bx, by - 16, bz + 248, true, 34, 18, 18, Material.POLISHED_BLACKSTONE_BRICKS);
    }

    private void buildHoldGatehouse(World world, int cx, int y, int cz, boolean acrossX,
                                    int halfAcross, int halfDepth, int height, Material accent) {
        if (world == null) return;
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        int hw = Math.max(10, halfAcross);
        int hd = Math.max(8, halfDepth);
        int h = Math.max(10, height);
        for (int across = -hw; across <= hw; across++) {
            for (int depth = -hd; depth <= hd; depth++) {
                boolean side = Math.abs(across) >= hw - 1;
                boolean rib = Math.floorMod(depth, 8) == 0;
                boolean centerWalk = Math.abs(across) <= Math.max(8, hw / 3);
                Block floorBase = holdAxisBlockAt(world, cx, y, cz, acrossX, across, -2, depth);
                Block floor = holdAxisBlockAt(world, cx, y, cz, acrossX, across, -1, depth);
                floorBase.setType(Material.DEEPSLATE, false);
                floor.setType(side ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (centerWalk ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy <= h; dy++) {
                    Block block = holdAxisBlockAt(world, cx, y, cz, acrossX, across, dy, depth);
                    boolean ceiling = dy == h;
                    boolean upperRib = rib && dy >= h - 2;
                    if (side || ceiling || upperRib) {
                        Material material = ceiling || side ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.floorMod(across + depth + dy, 5) == 0
                                ? accent : Material.DEEPSLATE_BRICKS);
                        block.setType(material, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
                if (rib && Math.abs(across) == hw - 3) {
                    holdAxisBlockAt(world, cx, y, cz, acrossX, across, h - 1, depth).setType(chain, false);
                    holdAxisBlockAt(world, cx, y, cz, acrossX, across, h - 2, depth)
                            .setType(Material.SOUL_LANTERN, false);
                }
            }
        }
    }

    private static Block holdAxisBlockAt(World world, int cx, int y, int cz, boolean acrossX,
                                         int across, int dy, int depth) {
        if (acrossX) return world.getBlockAt(cx + across, y + dy, cz + depth);
        return world.getBlockAt(cx + depth, y + dy, cz + across);
    }

    private void buildKeeperCourtBays(World world, int bx, int by, int bz) {
        if (world == null) return;
        buildKeeperBay(world, bx - 42, by, bz - 34, BlockFace.EAST, Material.CHISELED_DEEPSLATE);
        buildKeeperBay(world, bx - 48, by, bz - 8, BlockFace.EAST, Material.DEEPSLATE_TILES);
        buildKeeperBay(world, bx - 38, by, bz + 20, BlockFace.EAST, Material.POLISHED_DEEPSLATE);
        buildKeeperBay(world, bx + 42, by, bz - 34, BlockFace.WEST, Material.CHISELED_DEEPSLATE);
        buildKeeperBay(world, bx + 48, by, bz - 8, BlockFace.WEST, Material.DEEPSLATE_TILES);
        buildKeeperBay(world, bx + 38, by, bz + 20, BlockFace.WEST, Material.POLISHED_DEEPSLATE);

        for (int z : new int[]{-46, -22, 2, 26}) {
            buildLowDivider(world, bx, by, bz + z, 18, true);
        }
        placeStandingSign(new Location(world, bx, by, bz - 58), BlockFace.SOUTH,
                new String[]{"six cut seats", "one margin", "not a seventh", "chair"});
    }

    private void buildKeeperBay(World world, int cx, int y, int cz, BlockFace facing, Material accent) {
        BlockFace back = facing == null ? BlockFace.WEST : facing.getOppositeFace();
        int bx = cx + back.getModX() * 5;
        int bz = cz + back.getModZ() * 5;
        for (int a = -5; a <= 5; a++) {
            for (int d = 0; d <= 4; d++) {
                int x = bx + (back.getModX() == 0 ? a : -back.getModX() * d);
                int z = bz + (back.getModZ() == 0 ? a : -back.getModZ() * d);
                world.getBlockAt(x, y - 1, z).setType(Math.abs(a) >= 4
                        ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                for (int dy = 0; dy <= 7; dy++) {
                    boolean side = Math.abs(a) == 5;
                    boolean rear = d == 0;
                    boolean top = dy == 7;
                    Block block = world.getBlockAt(x, y + dy, z);
                    if (rear || side || top) {
                        block.setType(top ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (rear && dy == 2 ? accent : Material.DEEPSLATE_BRICKS), false);
                    } else if (dy <= 4) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        world.getBlockAt(cx, y, cz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(cx, y + 1, cz).setType(Material.BLACK_CANDLE, false);
    }

    private void buildLowDivider(World world, int cx, int y, int cz, int half, boolean acrossX) {
        if (world == null) return;
        for (int a = -half; a <= half; a++) {
            int x = acrossX ? cx + a : cx;
            int z = acrossX ? cz : cz + a;
            if (Math.abs(a) <= 3) continue;
            world.getBlockAt(x, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
            if (Math.floorMod(a, 6) == 0) {
                world.getBlockAt(x, y + 1, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildArchiveEvidenceRails(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z = bz + 48; z <= bz + 170; z += 24) {
            buildArchiveTable(world, bx - 62, by, z);
            for (int x : new int[]{bx - 92, bx - 84, bx - 40, bx - 32}) {
                for (int y = by; y <= by + 2; y++) {
                    placeDecorativeBookshelf(world.getBlockAt(x, y, z), x + y + z);
                }
                world.getBlockAt(x, by + 3, z).setType(Material.POLISHED_DEEPSLATE, false);
            }
        }
        for (int z : new int[]{bz + 60, bz + 108, bz + 156}) {
            buildLowDivider(world, bx - 62, by, z, 22, true);
        }
        placeStandingSign(new Location(world, bx - 90, by, bz + 32), BlockFace.EAST,
                new String[]{"archive rail", "read changes", "not labels", ""});
    }

    private void buildArchiveTable(World world, int cx, int y, int cz) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, y, cz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                world.getBlockAt(cx + dx, y + 1, cz + dz).setType(Material.DARK_OAK_SLAB, false);
            }
        }
        placeDecorativeBookshelf(world.getBlockAt(cx - 6, y, cz), cx + cz);
        world.getBlockAt(cx + 6, y, cz).setType(Material.BARREL, false);
    }

    private void buildMarketEvidenceFloor(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z = bz + 52; z <= bz + 128; z += 18) {
            for (int x : new int[]{bx + 52, bx + 72, bx + 92}) {
                buildMarketCounter(world, x, by, z);
            }
        }
        for (int z : new int[]{bz + 68, bz + 104, bz + 138}) {
            buildLowDivider(world, bx + 72, by, z, 24, true);
        }
        placeStandingSign(new Location(world, bx + 92, by, bz + 34), BlockFace.WEST,
                new String[]{"market files", "count goods", "before smoke", ""});
    }

    private void buildMarketCounter(World world, int cx, int y, int cz) {
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(cx + dx, y, cz - 1).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(cx + dx, y + 1, cz - 1).setType(Material.DARK_OAK_SLAB, false);
            if (Math.abs(dx) == 3) {
                world.getBlockAt(cx + dx, y + 2, cz - 1).setType(chain, false);
                world.getBlockAt(cx + dx, y + 1, cz).setType(Material.SOUL_LANTERN, false);
            }
        }
        world.getBlockAt(cx, y, cz + 1).setType(Material.BARREL, false);
    }

    private void buildLampworksAccountingMachine(World world, int bx, int by, int bz) {
        if (world == null) return;
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int i = 0; i < 3; i++) {
            int x = bx - 18 + (i * 18);
            int z = bz + 160 + (i * 7);
            buildLampStand(world, x, by - (i == 0 ? 4 : (i == 1 ? 6 : 8)), z, i != 0);
        }
        for (int z = bz + 148; z <= bz + 178; z += 6) {
            world.getBlockAt(bx - 5, by + 2, z).setType(chain, false);
            world.getBlockAt(bx + 5, by + 2, z).setType(chain, false);
        }
        placeStandingSign(new Location(world, bx + 12, by, bz + 150), BlockFace.WEST,
                new String[]{"ready mark", "is not the", "same as lit", ""});
    }

    private void buildLampStand(World world, int x, int y, int z, boolean lit) {
        world.getBlockAt(x, y - 1, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        world.getBlockAt(x, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
        world.getBlockAt(x, y + 1, z).setType(lit ? Material.SOUL_LANTERN : Material.CAULDRON, false);
        for (int dx : new int[]{-2, 2}) {
            world.getBlockAt(x + dx, y, z).setType(Material.POLISHED_DEEPSLATE, false);
        }
    }

    private void buildThresholdArgumentFloor(World world, int bx, int by, int bz) {
        if (world == null) return;
        int y = by - 16;
        for (int[] p : new int[][]{{-20, 214}, {0, 222}, {20, 214}}) {
            int cx = bx + p[0];
            int cz = bz + p[1];
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 5) continue;
                    world.getBlockAt(cx + dx, y - 1, cz + dz).setType(Material.DEEPSLATE_TILES, false);
                    if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                        world.getBlockAt(cx + dx, y, cz + dz).setType(Material.SCULK, false);
                    }
                }
            }
        }
        buildLowDivider(world, bx, y, bz + 236, 34, true);
        placeStandingSign(new Location(world, bx - 23, y, bz + 224), BlockFace.EAST,
                new String[]{"plate", "name", "word", "together"});
    }

    private void buildHoldProcessional(World world, int bx, int y, int bz, int relZ1, int relZ2,
                                       int halfWidth, int height) {
        if (world == null) return;
        int minZ = Math.min(relZ1, relZ2);
        int maxZ = Math.max(relZ1, relZ2);
        for (int z = bz + minZ; z <= bz + maxZ; z++) {
            int rel = z - bz;
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                boolean curb = Math.abs(dx) >= halfWidth - 1;
                boolean centerLine = Math.abs(dx) <= 1 || Math.floorMod(rel, 18) == 0;
                world.getBlockAt(bx + dx, y - 2, z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + dx, y - 1, z).setType(curb ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (centerLine ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy < height; dy++) {
                    if (!curb) world.getBlockAt(bx + dx, y + dy, z).setType(Material.AIR, false);
                }
                if (curb) {
                    for (int dy = 0; dy <= 2; dy++) {
                        world.getBlockAt(bx + dx, y + dy, z)
                                .setType(dy == 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS, false);
                    }
                }
            }
        }
    }

    private void buildHoldSideGalleryFront(World world, int x, int y, int bz, int relZ1, int relZ2, int sign) {
        if (world == null) return;
        int minZ = Math.min(relZ1, relZ2);
        int maxZ = Math.max(relZ1, relZ2);
        for (int z = bz + minZ; z <= bz + maxZ; z++) {
            boolean pier = Math.floorMod(z - bz, 18) == 0;
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x, y - 1, z + dz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
            }
            if (pier) {
                for (int dy = 0; dy <= 8; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(dy == 8
                            ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
                    world.getBlockAt(x + sign, y + dy, z).setType(dy == 8
                            ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS, false);
                }
                world.getBlockAt(x - sign, y + 7, z).setType(Material.SOUL_LANTERN, false);
            } else if (Math.floorMod(z - bz, 6) == 0) {
                world.getBlockAt(x, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
            }
        }
    }

    private void buildReturnMouthDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx, by + 10, bz - 108, 76, 24, 56);
        floorHoldOval(world, bx, by, bz - 108, 66, 46,
                Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE, Material.POLISHED_BLACKSTONE_BRICKS);
        buildHoldCorridorZ(world, bx, by, bz - 156, bz - 70, 17, 13);
        buildHoldArch(world, bx, by, bz - 136, 22, 15);
        buildHoldArch(world, bx, by, bz - 94, 22, 15);
        buildHoldRingColumns(world, bx, by, bz - 108, 54, 36, 15);
    }

    private void buildKeeperCourtDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx, by + 13, bz - 10, 108, 32, 96);
        floorHoldOval(world, bx, by, bz - 10, 94, 84,
                Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.POLISHED_BLACKSTONE_BRICKS);
        buildHoldCorridorZ(world, bx, by, bz - 90, bz + 42, 18, 14);
        buildHoldRingColumns(world, bx, by, bz - 10, 78, 70, 18);
        for (int dx : new int[]{-20, 20}) {
            for (int dz : new int[]{-54, -30, -6, 18, 34}) {
                world.getBlockAt(bx + dx, by + 11, bz + dz).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildArchiveDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx - 88, by + 10, bz + 112, 72, 26, 126);
        floorHoldOval(world, bx - 88, by, bz + 112, 64, 112,
                Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE, Material.CRACKED_DEEPSLATE_BRICKS);
        buildHoldCorridorX(world, by, bz + 42, bx - 12, bx - 88, 11, 12);
        buildHoldCorridorZ(world, bx - 88, by, bz + 30, bz + 202, 13, 12);
        buildHoldArch(world, bx - 88, by, bz + 62, 17, 12);
        buildHoldArch(world, bx - 88, by, bz + 134, 17, 12);
        buildArchiveStacks(world, bx, by, bz);
        buildHoldWaterBasin(world, bx - 62, by, bz + 174, 22, 13);
    }

    private void buildMarketDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx + 88, by + 10, bz + 96, 72, 26, 110);
        floorHoldOval(world, bx + 88, by, bz + 96, 64, 96,
                Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS);
        buildHoldCorridorX(world, by, bz + 42, bx + 12, bx + 88, 11, 12);
        buildHoldCorridorZ(world, bx + 88, by, bz + 30, bz + 166, 13, 12);
        buildHoldArch(world, bx + 88, by, bz + 64, 17, 12);
        buildHoldArch(world, bx + 88, by, bz + 128, 17, 12);
        buildMarketStalls(world, bx, by, bz);
    }

    private void buildLampworksDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx, by + 7, bz + 156, 66, 25, 62);
        floorHoldOval(world, bx, by, bz + 156, 56, 52,
                Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE, Material.POLISHED_BLACKSTONE_BRICKS);
        buildHoldCorridorZ(world, bx, by, bz + 116, bz + 154, 18, 13);
        buildHoldArch(world, bx, by, bz + 144, 20, 13);
        for (int step = 0; step <= 82; step++) {
            int z = bz + 146 + step;
            int y = by - Math.min(16, step / 4);
            buildHoldRampSlice(world, bx, y, z, 17, 13, step);
        }
    }

    private void buildThresholdDistrict(World world, int bx, int by, int bz) {
        carveHoldEllipsoid(world, bx, by - 6, bz + 252, 112, 34, 108);
        floorHoldOval(world, bx, by - 16, bz + 252, 98, 94,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES);
        buildHoldCorridorZ(world, bx, by - 16, bz + 180, bz + 342, 18, 15);
        buildHoldRingColumns(world, bx, by - 16, bz + 252, 84, 82, 20);
        buildHoldArch(world, bx, by - 16, bz + 198, 20, 14);
        buildHoldArch(world, bx, by - 16, bz + 248, 20, 14);
        buildHoldArch(world, bx, by - 16, bz + 306, 20, 14);
    }

    private void buildDreadDistrict(World world, int bx, int by, int bz) {
        buildHoldCorridorX(world, by - 16, bz + 188, bx + 10, bx + 86, 8, 10);
        carveHoldEllipsoid(world, bx + 76, by - 10, bz + 214, 34, 18, 48);
        floorHoldOval(world, bx + 76, by - 16, bz + 214, 28, 40,
                Material.BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS);
        buildHoldCorridorZ(world, bx + 68, by - 16, bz + 184, bz + 242, 8, 10);
    }

    private void buildHoldRoomShell(World world, Location loc, HoldSite row) {
        if (world == null || loc == null) return;
        int halfX = holdRoomHalfX(row);
        int halfZ = holdRoomHalfZ(row);
        int height = holdRoomHeight(row);
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -halfZ; dz <= halfZ; dz++) {
                boolean west = dx == -halfX;
                boolean east = dx == halfX;
                boolean north = dz == -halfZ;
                boolean south = dz == halfZ;
                boolean rim = west || east || north || south;
                boolean frontWall = (front == BlockFace.EAST && east)
                        || (front == BlockFace.WEST && west)
                        || (front == BlockFace.NORTH && north)
                        || (front == BlockFace.SOUTH && south);
                boolean frontDoor = frontWall && holdRoomDoorColumn(row, front, dx, dz, halfX, halfZ);
                boolean wall = rim && !frontDoor;
                boolean frontReturn = frontWall && !frontDoor;
                Material floor = rim ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((Math.abs(dx) <= 2 || Math.abs(dz) <= 2) ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                for (int dy = 0; dy <= height; dy++) {
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (dy == height) {
                        Material ceiling = (rim || Math.floorMod(dx + dz, 6) == 0)
                                ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES;
                        block.setType(ceiling, false);
                    } else if (wall) {
                        Material wallMat = (dy == height - 1 || frontReturn)
                                ? Material.POLISHED_DEEPSLATE
                                : (Math.floorMod(dx + dz + dy, 7) == 0
                                ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS);
                        block.setType(wallMat, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (int dx : new int[]{-halfX, halfX}) {
            for (int dz : new int[]{-halfZ, halfZ}) {
                for (int dy = 0; dy <= 6; dy++) {
                    world.getBlockAt(bx + dx, by + dy, bz + dz)
                            .setType(dy == 6 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
                }
                world.getBlockAt(bx + dx, by + 5, bz + dz).setType(Material.SOUL_LANTERN, false);
            }
        }
        decorateHoldAlcove(world, loc, row, halfX, halfZ, height, front);
    }

    private boolean needsHoldDedicatedRoom(HoldSite row) {
        if (row == null || row.halfX() <= 0 || row.halfZ() <= 0) return false;
        String id = row.id();
        return "rune_rosetta".equals(id)
                || "stone_of_reckoning".equals(id)
                || "the_threshold".equals(id)
                || "threshold_vault".equals(id)
                || "prior_camp".equals(id)
                || "failed_accepting".equals(id)
                || "unbroken_light".equals(id)
                || "keeper_altar".equals(id)
                || "coop_plate".equals(id)
                || "the_unwriting".equals(id)
                || "dead_stall".equals(id)
                || "deep_bird_coops".equals(id);
    }

    private void buildHoldTemplateAlcoveFrame(World world, Location loc, HoldSite row) {
        if (world == null || loc == null || row == null) return;
        int halfX = holdRoomHalfX(row);
        int halfZ = holdRoomHalfZ(row);
        int height = holdRoomHeight(row);
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace front = holdRoomFront(row);
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -halfZ; dz <= halfZ; dz++) {
                boolean west = dx == -halfX;
                boolean east = dx == halfX;
                boolean north = dz == -halfZ;
                boolean south = dz == halfZ;
                boolean rim = west || east || north || south;
                boolean frontWall = (front == BlockFace.EAST && east)
                        || (front == BlockFace.WEST && west)
                        || (front == BlockFace.NORTH && north)
                        || (front == BlockFace.SOUTH && south);
                boolean frontDoor = frontWall && holdRoomDoorColumn(row, front, dx, dz, halfX, halfZ);
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.DEEPSLATE, false);
                if (rim) {
                    world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                } else if (Math.abs(dx) >= halfX - 2 || Math.abs(dz) >= halfZ - 2) {
                    world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.POLISHED_DEEPSLATE, false);
                }
                if (rim && !frontDoor) {
                    for (int dy = 0; dy < height; dy++) {
                        Material material = (dy == height - 1)
                                ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.floorMod(dx + dz + dy, 6) == 0
                                ? Material.CRACKED_DEEPSLATE_BRICKS : holdAlcoveAccent(row));
                        world.getBlockAt(bx + dx, by + dy, bz + dz).setType(material, false);
                    }
                }
                world.getBlockAt(bx + dx, by + height, bz + dz)
                        .setType((rim || Math.floorMod(dx + dz, 5) == 0)
                                ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
            }
        }
        decorateHoldAlcove(world, loc, row, halfX, halfZ, height, front);
    }

    private int holdRoomHalfX(HoldSite row) {
        if (row == null) return 8;
        return Math.max(12, row.halfX() + 3);
    }

    private int holdRoomHalfZ(HoldSite row) {
        if (row == null) return 7;
        return Math.max(11, row.halfZ() + 3);
    }

    private int holdRoomHeight(HoldSite row) {
        if (row == null) return 8;
        return Math.max(isTemplateLabSite(row.id()) ? 18 : 15, row.vertical() + 8);
    }

    private BlockFace holdRoomFront(HoldSite row) {
        if (row == null) return BlockFace.NORTH;
        if (row.x() < 0) return BlockFace.EAST;
        if (row.x() > 0) return BlockFace.WEST;
        return row.z() < 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private boolean holdRoomDoorColumn(HoldSite row, BlockFace front, int dx, int dz, int halfX, int halfZ) {
        boolean broadAlcove = row != null && (isTemplateLabSite(row.id()) || isThresholdAlcove(row));
        int doorHalf = broadAlcove
                ? Math.max(5, ((front == BlockFace.EAST || front == BlockFace.WEST) ? halfZ : halfX) - 3)
                : 5;
        return switch (front) {
            case EAST, WEST -> Math.abs(dz) <= doorHalf;
            case NORTH, SOUTH -> Math.abs(dx) <= doorHalf;
            default -> false;
        };
    }

    private boolean isHoldBayDoor(BlockFace front, int dx, int dz, int halfX, int halfZ, int doorHalf) {
        if (front == null) return false;
        int open = Math.max(2, doorHalf);
        return switch (front) {
            case EAST -> dx == halfX && Math.abs(dz) <= open;
            case WEST -> dx == -halfX && Math.abs(dz) <= open;
            case SOUTH -> dz == halfZ && Math.abs(dx) <= open;
            case NORTH -> dz == -halfZ && Math.abs(dx) <= open;
            default -> false;
        };
    }

    private void decorateHoldAlcove(World world, Location loc, HoldSite row,
                                    int halfX, int halfZ, int height, BlockFace front) {
        if (world == null || loc == null || row == null) return;
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        BlockFace back = front.getOppositeFace();
        int backX = bx + (back == BlockFace.EAST ? halfX : (back == BlockFace.WEST ? -halfX : 0));
        int backZ = bz + (back == BlockFace.SOUTH ? halfZ : (back == BlockFace.NORTH ? -halfZ : 0));
        Material accent = holdAlcoveAccent(row);

        for (int i = -3; i <= 3; i += 3) {
            int x = backX + (back.getModX() == 0 ? i : -back.getModX());
            int z = backZ + (back.getModZ() == 0 ? i : -back.getModZ());
            for (int dy = 0; dy <= height - 2; dy++) {
                world.getBlockAt(x, by + dy, z).setType(dy == height - 2 ? Material.CHISELED_DEEPSLATE : accent, false);
            }
            world.getBlockAt(x, by + height - 3, z).setType(Material.SOUL_LANTERN, false);
        }

        if (isArchiveAlcove(row)) {
            for (int i = -4; i <= 4; i += 2) {
                int x = backX + (back.getModX() == 0 ? i : -back.getModX());
                int z = backZ + (back.getModZ() == 0 ? i : -back.getModZ());
                placeDecorativeBookshelf(world.getBlockAt(x, by, z), row.id().hashCode() + i);
                placeDecorativeBookshelf(world.getBlockAt(x, by + 1, z), row.id().hashCode() + i + 11);
            }
        } else if (isMarketAlcove(row)) {
            for (int i : new int[]{-4, 4}) {
                int x = bx + (front.getModZ() == 0 ? 0 : i);
                int z = bz + (front.getModX() == 0 ? 0 : i);
                Block barrel = world.getBlockAt(x, by, z);
                if (barrel.getType().isAir()) barrel.setType(Material.BARREL, false);
            }
        } else if ("keeper_stone".equals(row.type())) {
            int x = backX - back.getModX();
            int z = backZ - back.getModZ();
            world.getBlockAt(x, by, z).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(x, by + 1, z).setType(Material.BLACK_CANDLE, false);
        } else if (isThresholdAlcove(row)) {
            for (int i = -3; i <= 3; i += 3) {
                int x = backX + (back.getModX() == 0 ? i : -back.getModX());
                int z = backZ + (back.getModZ() == 0 ? i : -back.getModZ());
                world.getBlockAt(x, by, z).setType(Material.SCULK, false);
                world.getBlockAt(x, by + 1, z).setType(Material.BLACKSTONE, false);
            }
        }
    }

    private Material holdAlcoveAccent(HoldSite row) {
        if (row == null) return Material.DEEPSLATE_BRICKS;
        if (isThresholdAlcove(row)) return Material.POLISHED_BLACKSTONE_BRICKS;
        if (isMarketAlcove(row)) return Material.POLISHED_DEEPSLATE;
        if (isArchiveAlcove(row)) return Material.DEEPSLATE_TILES;
        if ("keeper_stone".equals(row.type())) return Material.CHISELED_DEEPSLATE;
        return Material.DEEPSLATE_BRICKS;
    }

    private boolean isArchiveAlcove(HoldSite row) {
        if (row == null) return false;
        String type = row.type();
        return "structure".equals(type) || "school_stand".equals(type) || "markers_row".equals(type)
                || "cistern_7".equals(type) || "watch_floor".equals(type)
                || "set_apart_shelf".equals(type) || "far_water".equals(type)
                || "undercroft_seal".equals(type) || "forgotten_mouth".equals(type);
    }

    private boolean isMarketAlcove(HoldSite row) {
        if (row == null) return false;
        String type = row.type();
        return "deep_market".equals(type) || "ration_table".equals(type)
                || "third_bay_breach".equals(type) || "warm_town_collapse".equals(type)
                || "dead_stall".equals(type) || "bird_coops".equals(type);
    }

    private boolean isThresholdAlcove(HoldSite row) {
        if (row == null) return false;
        return row.y() < 0 || "coop_plate".equals(row.type()) || "accepting_floor".equals(row.type())
                || "keeper_altar".equals(row.type()) || "seventh_shrine".equals(row.type());
    }

    private void connectHoldRoom(World world, int bx, int by, int bz, HoldSite row) {
        if (world == null || row.x() == 0 || row.halfX() <= 0) return;
        int sign = row.x() > 0 ? 1 : -1;
        int y = by + row.y();
        int roomHalf = holdRoomHalfX(row);
        int roomWallX = bx + row.x() - (sign * roomHalf);
        int corridorEnd = roomWallX - sign;
        int corridorStart = bx + (sign * 13);
        buildHoldCorridorX(world, y, bz + row.z(), corridorStart, corridorEnd, 6, 8);
        for (int step = 0; step <= 3; step++) {
            int x = roomWallX + (sign * step);
            for (int dz = -5; dz <= 5; dz++) {
                world.getBlockAt(x, y - 1, bz + row.z() + dz).setType(Material.DEEPSLATE_TILES, false);
                for (int dy = 0; dy <= 7; dy++) {
                    world.getBlockAt(x, y + dy, bz + row.z() + dz).setType(Material.AIR, false);
                }
            }
        }
    }

    private void connectHoldCentralRoom(World world, int bx, int by, int bz, HoldSite row) {
        if (world == null || row.halfX() <= 0 || row.halfZ() <= 0) return;
        int y = by + row.y();
        int halfZ = holdRoomHalfZ(row);
        boolean southFront = holdRoomFront(row) == BlockFace.SOUTH;
        int frontWallZ = bz + row.z() + (southFront ? halfZ : -halfZ);
        for (int z = frontWallZ - 3; z <= frontWallZ + 3; z++) {
            for (int dx = -5; dx <= 5; dx++) {
                world.getBlockAt(bx + dx, y - 1, z).setType(Material.DEEPSLATE_TILES, false);
                for (int dy = 0; dy <= 7; dy++) {
                    world.getBlockAt(bx + dx, y + dy, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void carveHoldEllipsoid(World world, int cx, int cy, int cz, int rx, int ry, int rz) {
        if (world == null) return;
        double rx2 = Math.max(1, rx * rx);
        double ry2 = Math.max(1, ry * ry);
        double rz2 = Math.max(1, rz * rz);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    double n = (dx * dx) / rx2 + (dy * dy) / ry2 + (dz * dz) / rz2;
                    if (n <= 1.0) {
                        world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void floorHoldOval(World world, int cx, int y, int cz, int rx, int rz,
                               Material floorA, Material floorB, Material trim) {
        if (world == null) return;
        double rx2 = Math.max(1, rx * rx);
        double rz2 = Math.max(1, rz * rz);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double n = (dx * dx) / rx2 + (dz * dz) / rz2;
                if (n > 1.0) continue;
                Material floor = n > 0.84 ? trim : (((dx + dz) & 1) == 0 ? floorA : floorB);
                if (Math.abs(dx) <= 2 || Math.abs(dz) <= 2) floor = Material.DEEPSLATE_TILES;
                world.getBlockAt(cx + dx, y - 2, cz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(cx + dx, y - 1, cz + dz).setType(floor, false);
                world.getBlockAt(cx + dx, y, cz + dz).setType(Material.AIR, false);
                world.getBlockAt(cx + dx, y + 1, cz + dz).setType(Material.AIR, false);
            }
        }
    }

    private void buildHoldRingColumns(World world, int cx, int y, int cz, int rx, int rz, int height) {
        if (world == null) return;
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2.0 * i) / 16.0;
            int x = cx + (int) Math.round(Math.cos(angle) * rx);
            int z = cz + (int) Math.round(Math.sin(angle) * rz);
            for (int dy = -1; dy <= height; dy++) {
                Material mat = dy == height ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (Math.floorMod(i, 3) == 0 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(x, y + dy, z).setType(mat, false);
            }
            world.getBlockAt(x, y + height - 1, z).setType(chain, false);
            world.getBlockAt(x, y + height - 2, z).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void buildHoldArch(World world, int cx, int y, int z, int halfWidth, int height) {
        if (world == null) return;
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dy = 0; dy <= height; dy++) {
                boolean arch = Math.abs(dx) == halfWidth || dy == height
                        || (dy >= height - 2 && Math.abs(dx) >= halfWidth - 2);
                if (arch) {
                    world.getBlockAt(cx + dx, y + dy, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                } else {
                    world.getBlockAt(cx + dx, y + dy, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void buildArchiveStacks(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z = bz + 48; z <= bz + 168; z += 16) {
            for (int x : new int[]{bx - 88, bx - 80, bx - 64, bx - 56}) {
                for (int y = by; y <= by + 2; y++) {
                    placeDecorativeBookshelf(world.getBlockAt(x, y, z), x + z + y);
                }
                world.getBlockAt(x, by + 3, z).setType(Material.POLISHED_DEEPSLATE, false);
            }
        }
    }

    private void buildMarketStalls(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (int z = bz + 50; z <= bz + 130; z += 20) {
            for (int x : new int[]{bx + 56, bx + 78}) {
                for (int dx = -3; dx <= 3; dx++) {
                    world.getBlockAt(x + dx, by, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
                    world.getBlockAt(x + dx, by + 3, z).setType(Material.DARK_OAK_SLAB, false);
                }
                world.getBlockAt(x, by + 1, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildHoldWaterBasin(World world, int cx, int y, int cz, int rx, int rz) {
        if (world == null) return;
        double rx2 = Math.max(1, rx * rx);
        double rz2 = Math.max(1, rz * rz);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double n = (dx * dx) / rx2 + (dz * dz) / rz2;
                if (n > 1.0) continue;
                Material rim = n > 0.72 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.WATER;
                world.getBlockAt(cx + dx, y - 1, cz + dz).setType(Material.DEEPSLATE_TILES, false);
                world.getBlockAt(cx + dx, y, cz + dz).setType(rim, false);
                world.getBlockAt(cx + dx, y + 1, cz + dz).setType(Material.AIR, false);
            }
        }
    }

    private void buildHoldRampSlice(World world, int cx, int y, int z, int halfWidth, int height, int step) {
        if (world == null) return;
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            boolean wall = Math.abs(dx) == halfWidth;
            world.getBlockAt(cx + dx, y - 2, z).setType(Material.DEEPSLATE, false);
            Block floor = world.getBlockAt(cx + dx, y - 1, z);
            floor.setType(wall ? Material.POLISHED_BLACKSTONE_BRICKS
                    : (step > 0 && step % 3 == 0 ? Material.POLISHED_DEEPSLATE_STAIRS
                    : (Math.floorMod(step + dx, 5) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE)), false);
            if (!wall && step > 0 && step % 3 == 0 && floor.getBlockData() instanceof Directional directional) {
                directional.setFacing(BlockFace.NORTH);
                floor.setBlockData(directional, false);
            }
            for (int dy = 0; dy < height; dy++) {
                world.getBlockAt(cx + dx, y + dy, z).setType(wall ? Material.DEEPSLATE_BRICKS : Material.AIR, false);
            }
            world.getBlockAt(cx + dx, y + height, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        }
        if (step % 12 == 0) {
            world.getBlockAt(cx - halfWidth + 1, y + height - 1, z).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(cx + halfWidth - 1, y + height - 1, z).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void buildHoldCorridorZ(World world, int centerX, int y, int z1, int z2, int halfWidth, int height) {
        if (world == null) return;
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int hw = Math.max(2, halfWidth);
        int h = Math.max(4, height);
        for (int z = minZ; z <= maxZ; z++) {
            for (int dx = -hw; dx <= hw; dx++) {
                int x = centerX + dx;
                boolean wall = Math.abs(dx) == hw;
                world.getBlockAt(x, y - 2, z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(x, y - 1, z).setType(wall ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (Math.floorMod(z, 7) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy < h; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(wall ? Material.DEEPSLATE_BRICKS : Material.AIR, false);
                }
                world.getBlockAt(x, y + h, z).setType(wall ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
            }
            if (Math.floorMod(z, 14) == 0) {
                world.getBlockAt(centerX - hw + 1, y + h - 1, z).setType(Material.SOUL_LANTERN, false);
                world.getBlockAt(centerX + hw - 1, y + h - 1, z).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void buildHoldCorridorX(World world, int y, int centerZ, int x1, int x2, int halfWidth, int height) {
        if (world == null) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int hw = Math.max(2, halfWidth);
        int h = Math.max(4, height);
        for (int x = minX; x <= maxX; x++) {
            for (int dz = -hw; dz <= hw; dz++) {
                int z = centerZ + dz;
                boolean wall = Math.abs(dz) == hw;
                world.getBlockAt(x, y - 2, z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(x, y - 1, z).setType(wall ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (Math.floorMod(x, 7) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy < h; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(wall ? Material.DEEPSLATE_BRICKS : Material.AIR, false);
                }
                world.getBlockAt(x, y + h, z).setType(wall ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
            }
            if (Math.floorMod(x, 14) == 0) {
                world.getBlockAt(x, y + h - 1, centerZ - hw + 1).setType(Material.SOUL_LANTERN, false);
                world.getBlockAt(x, y + h - 1, centerZ + hw - 1).setType(Material.SOUL_LANTERN, false);
            }
        }
    }

    private void placeHoldSupportLights(World world, int bx, int by, int bz) {
        if (world == null) return;
        Material chain = materialOr(Material.IRON_BARS, "CHAIN");
        for (int z : new int[]{-124, -104, -78, -54, -30, -6, 24, 54, 84, 114, 144}) {
            world.getBlockAt(bx - 3, by + 2, bz + z).setType(chain, false);
            world.getBlockAt(bx - 3, by + 1, bz + z).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(bx + 3, by + 2, bz + z).setType(chain, false);
            world.getBlockAt(bx + 3, by + 1, bz + z).setType(Material.SOUL_LANTERN, false);
        }
        for (int z : new int[]{184, 206, 228, 250, 272, 292, 310}) {
            world.getBlockAt(bx, by - 11, bz + z).setType(chain, false);
            world.getBlockAt(bx, by - 12, bz + z).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void placeHoldWayfinding(World world, int bx, int by, int bz) {
        if (world == null) return;
        placeStandingSign(new Location(world, bx - 8, by, bz - 168), BlockFace.EAST,
                new String[]{"THE DEEP", "HOLD", "walk low", "count first"});
        placeStandingSign(new Location(world, bx + 14, by, bz + 64), BlockFace.WEST,
                new String[]{"the archive", "does not open", "for noise", ""});
        placeStandingSign(new Location(world, bx + 15, by, bz + 242), BlockFace.WEST,
                new String[]{"below here", "the lamps", "keep accounts", ""});
        placeStandingSign(new Location(world, bx - 15, by - 28, bz + 322), BlockFace.EAST,
                new String[]{"threshold", "waits for", "three hands", ""});
    }

    private static String holdGateSiteId(String gateId) {
        return "hold_gate_" + gateId;
    }

    private static HoldGate holdGateById(String gateId) {
        if (gateId == null) return null;
        String want = gateId.toLowerCase(Locale.ROOT);
        for (HoldGate gate : DEEP_HOLD_GATES) {
            if (gate.id().equals(want)) return gate;
        }
        return null;
    }

    private List<String> holdGateSuggestions(String prefix) {
        String want = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if ("all".startsWith(want)) out.add("all");
        for (HoldGate gate : DEEP_HOLD_GATES) {
            if (gate.id().startsWith(want)) out.add(gate.id());
        }
        return out;
    }

    private int applyHoldGateSelection(CommandSender sender, String gateName, boolean sealed) {
        if (gateName == null || gateName.isBlank() || gateName.equalsIgnoreCase("all")) {
            int changed = 0;
            for (HoldGate gate : DEEP_HOLD_GATES) {
                if (applyHoldGateByName(sender, gate.id(), sealed)) changed++;
            }
            return changed;
        }
        return applyHoldGateByName(sender, gateName, sealed) ? 1 : 0;
    }

    private boolean applyHoldGateByName(CommandSender sender, String gateName, boolean sealed) {
        HoldGate gate = holdGateById(gateName);
        if (gate == null) {
            if (sender != null) sender.sendMessage("  [!] unknown hold gate: " + gateName);
            return false;
        }
        Site site = plugin.sites() == null ? null : plugin.sites().get(holdGateSiteId(gate.id()));
        Location loc = site == null ? null : site.location();
        if (loc == null || loc.getWorld() == null) {
            if (sender != null) sender.sendMessage("  [!] gate " + gate.id() + " is not placed yet.");
            return false;
        }
        setHoldGate(gate, loc, sealed);
        return true;
    }

    private void setHoldGate(HoldGate gate, Location loc, boolean sealed) {
        if (gate == null || loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int doorHeight = Math.min(span.height() - 3, Math.max(6, span.doorHalf() + 1));
        for (int a = -span.halfAcross(); a <= span.halfAcross(); a++) {
            boolean doorColumn = Math.abs(a) <= span.doorHalf();
            for (int d = 0; d <= span.depth(); d++) {
                holdGateBlockAt(world, cx, y, cz, span, a, -2, d).setType(Material.DEEPSLATE, false);
                holdGateBlockAt(world, cx, y, cz, span, a, -1, d).setType(
                        doorColumn ? Material.DEEPSLATE_TILES : Material.POLISHED_BLACKSTONE_BRICKS, false);
                holdGateBlockAt(world, cx, y, cz, span, a, span.height(), d)
                        .setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                for (int dy = 0; dy < span.height(); dy++) {
                    Block block = holdGateBlockAt(world, cx, y, cz, span, a, dy, d);
                    boolean door = doorColumn && dy <= doorHeight;
                    if (!door) {
                        block.setType(holdGateWallMaterial(a, dy, d, span), false);
                    } else if (sealed) {
                        if (d == 0) {
                            block.setType((Math.abs(a) == span.doorHalf() || dy == doorHeight)
                                    ? Material.POLISHED_BLACKSTONE_BRICK_WALL : Material.IRON_BARS, false);
                        } else {
                            block.setType(Material.BARRIER, false);
                        }
                    } else if (block.getType() == Material.BARRIER || isHoldGateMaterial(block.getType())) {
                        block.setType(Material.AIR, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        buildHoldGateReturnWalls(world, cx, y, cz, span);
    }

    private void buildHoldGateReturnWalls(World world, int cx, int y, int cz, HoldGateSpan span) {
        if (world == null || span == null) return;
        int returnWidth = holdGateReturnWidth(span);
        for (int side : new int[]{-1, 1}) {
            for (int a = span.halfAcross() + 1; a <= span.halfAcross() + returnWidth; a++) {
                int across = side * a;
                for (int d = -4; d <= span.depth() + 6; d++) {
                    holdGateBlockAt(world, cx, y, cz, span, across, -2, d).setType(Material.DEEPSLATE, false);
                    holdGateBlockAt(world, cx, y, cz, span, across, -1, d)
                            .setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                    for (int dy = 0; dy <= span.height() + 4; dy++) {
                        Material material = (dy >= span.height() || Math.abs(a - span.halfAcross()) >= returnWidth - 10)
                                ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.floorMod(across + d + dy, 6) == 0
                                ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS);
                        holdGateBlockAt(world, cx, y, cz, span, across, dy, d).setType(material, false);
                    }
                }
            }
        }
        for (int across = -span.halfAcross() - returnWidth; across <= span.halfAcross() + returnWidth; across++) {
            for (int d = -3; d <= span.depth() + 5; d++) {
                for (int dy = span.height() + 1; dy <= span.height() + 5; dy++) {
                    Material material = Math.floorMod(across + d + dy, 7) == 0
                            ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_BLACKSTONE_BRICKS;
                    holdGateBlockAt(world, cx, y, cz, span, across, dy, d).setType(material, false);
                }
            }
        }
    }

    private static int holdGateReturnWidth(HoldGateSpan span) {
        if (span == null) return 8;
        return Math.max(8, Math.min(12, span.halfAcross() / 2));
    }

    private static boolean isHoldGateMaterial(Material material) {
        return material == Material.IRON_BARS
                || material == Material.BARRIER
                || material == Material.POLISHED_BLACKSTONE_BRICK_WALL;
    }

    private static HoldGateSpan holdGateSpan(HoldGate gate) {
        if (gate == null) return new HoldGateSpan(true, 8, 8, 1, 4);
        return switch (gate.id()) {
            case "entry" -> new HoldGateSpan(true, 13, 13, 3, 6);
            case "keeper" -> new HoldGateSpan(true, 14, 14, 3, 6);
            case "archive" -> new HoldGateSpan(true, 14, 13, 3, 5);
            case "prior" -> new HoldGateSpan(true, 12, 11, 3, 4);
            case "deep" -> new HoldGateSpan(true, 15, 14, 3, 6);
            case "threshold" -> new HoldGateSpan(true, 15, 14, 3, 6);
            case "accepting" -> new HoldGateSpan(true, 17, 16, 3, 7);
            case "dread" -> new HoldGateSpan(false, 10, 10, 3, 4);
            default -> new HoldGateSpan(true, 10, 9, 1, 4);
        };
    }

    private static Material holdGateWallMaterial(int across, int dy, int depth, HoldGateSpan span) {
        boolean edge = Math.abs(across) == span.halfAcross() || dy == span.height() - 1 || dy == 0;
        if (edge || depth > 0) return Material.POLISHED_BLACKSTONE_BRICKS;
        return Math.floorMod(across + dy, 5) == 0 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS;
    }

    private static Block holdGateBlockAt(World world, int cx, int y, int cz, HoldGateSpan span,
                                         int across, int dy, int depth) {
        if (span.acrossX()) {
            return world.getBlockAt(cx + across, y + dy, cz + depth);
        }
        return world.getBlockAt(cx + depth, y + dy, cz + across);
    }

    private void placeHoldGateLabel(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return;
        HoldGateSpan span = holdGateSpan(gate);
        Location signLoc = span.acrossX() ? loc.clone().add(-span.doorHalf() - 2, 0, -2)
                : loc.clone().add(-2, 0, -span.doorHalf() - 2);
        placeStandingSign(signLoc, span.acrossX() ? BlockFace.NORTH : BlockFace.WEST,
                new String[]{gate.label(), "held until", "the record", "turns"});
    }

    private void syncPlaceHoldGates(CommandSender sender) {
        var sb = plugin.supabase();
        if (sb == null || !sb.isConfigured()) {
            sender.sendMessage("Observance: Supabase is not configured; use /obs placehold open|seal manually.");
            return;
        }
        sender.sendMessage("Observance: syncing Deep Hold gates from arc_state.flags...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var r = sb.fetchArcState();
            Map<String, Object> flags = (r.ok() && r.value() != null)
                    ? r.value().flagsMap() : Collections.emptyMap();
            Bukkit.getScheduler().runTask(plugin, () -> {
                int changed = 0;
                changed += applyHoldGateByName(sender, "entry", false) ? 1 : 0;
                changed += applyHoldGateByName(sender, "keeper", false) ? 1 : 0;
                boolean archiveOpen = directorFlag(flags, "undercroft_open");
                boolean priorOpen = directorFlag(flags, "prior_absence_known")
                        || directorFlag(flags, "prior_camp_read")
                        || directorFlag(flags, "prior_witness_ready");
                boolean deepOpen = directorFlag(flags, "deep_gate_open")
                        || (directorFlag(flags, "iss_caught") && directorFlag(flags, "seventh_suspected"));
                boolean dreadOpen = directorFlag(flags, "iss_caught") || directorFlag(flags, "seventh_suspected");
                boolean thresholdOpen = directorFlag(flags, "deep_gate_open")
                        || directorFlag(flags, "threshold_open")
                        || directorFlag(flags, "seventh_named");
                boolean acceptingReady = directorFlag(flags, "prior_witness_ready");
                boolean acceptingOpen = acceptingReady && (directorFlag(flags, "accepting_onramp_open")
                        || directorFlag(flags, "threshold_open"));
                changed += applyHoldGateByName(sender, "archive", !archiveOpen) ? 1 : 0;
                changed += applyHoldGateByName(sender, "prior", !priorOpen) ? 1 : 0;
                changed += applyHoldGateByName(sender, "deep", !deepOpen) ? 1 : 0;
                changed += applyHoldGateByName(sender, "dread", !dreadOpen) ? 1 : 0;
                changed += applyHoldGateByName(sender, "threshold", !thresholdOpen) ? 1 : 0;
                changed += applyHoldGateByName(sender, "accepting", !acceptingOpen) ? 1 : 0;
                sender.sendMessage("Observance: Deep Hold sync applied to " + changed + " gate(s).");
                sender.sendMessage("  archive=" + yesNo(archiveOpen) + ", prior=" + yesNo(priorOpen)
                        + ", deep=" + yesNo(deepOpen)
                        + ", dread=" + yesNo(dreadOpen) + ", threshold=" + yesNo(thresholdOpen)
                        + ", accepting=" + yesNo(acceptingOpen));
            });
        });
    }

    /** Periodic, silent live sync used by the plugin scheduler. It is inert until a Hold is placed. */
    public void syncPlaceHoldGatesAutomatically() {
        if (!isDeepHoldBuilt() || !automaticHoldSyncInFlight.compareAndSet(false, true)) return;
        var sb = plugin.supabase();
        if (sb == null || !sb.isConfigured()) {
            automaticHoldSyncInFlight.set(false);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var result = sb.fetchArcState();
            Map<String, Object> flags = (result.ok() && result.value() != null)
                    ? result.value().flagsMap() : null;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (flags == null) return; // fail closed and preserve the last known physical state
                    applyHoldGateByName(null, "entry", false);
                    applyHoldGateByName(null, "keeper", false);
                    boolean archiveOpen = directorFlag(flags, "undercroft_open");
                    boolean priorOpen = directorFlag(flags, "prior_absence_known")
                            || directorFlag(flags, "prior_camp_read")
                            || directorFlag(flags, "prior_witness_ready");
                    boolean deepOpen = directorFlag(flags, "deep_gate_open")
                            || (directorFlag(flags, "iss_caught") && directorFlag(flags, "seventh_suspected"));
                    boolean dreadOpen = directorFlag(flags, "iss_caught") || directorFlag(flags, "seventh_suspected");
                    boolean thresholdOpen = directorFlag(flags, "deep_gate_open")
                            || directorFlag(flags, "threshold_open")
                            || directorFlag(flags, "seventh_named");
                    boolean acceptingOpen = directorFlag(flags, "prior_witness_ready")
                            && (directorFlag(flags, "accepting_onramp_open")
                            || directorFlag(flags, "threshold_open"));
                    applyHoldGateByName(null, "archive", !archiveOpen);
                    applyHoldGateByName(null, "prior", !priorOpen);
                    applyHoldGateByName(null, "deep", !deepOpen);
                    applyHoldGateByName(null, "dread", !dreadOpen);
                    applyHoldGateByName(null, "threshold", !thresholdOpen);
                    applyHoldGateByName(null, "accepting", !acceptingOpen);
                } finally {
                    automaticHoldSyncInFlight.set(false);
                }
            });
        });
    }

    private void handlePlaceHoldAudit(CommandSender sender) {
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance Deep Hold audit: no sites loaded. Run /obs reload or build the hold first.");
            return;
        }
        int placed = 0;
        int critical = 0;
        int recordStations = 0;
        String entryRouteStatus = "not built";
        List<String> notes = new ArrayList<>();
        for (HoldSite row : DEEP_HOLD_SITES) {
            Site site = plugin.sites().get(row.id());
            Location loc = site == null ? null : site.location();
            if (site == null || loc == null || loc.getWorld() == null) {
                critical++;
                addAuditIssue(notes, row.id() + " missing");
                continue;
            }
            placed++;
            String issue = auditPlacedSite(site, loc);
            if (issue != null) {
                critical++;
                addAuditIssue(notes, issue);
            }
            if (!hasHoldNearbyPlayerSpace(loc, 5, 10)) {
                critical++;
                addAuditIssue(notes, row.id() + " has too little nearby player standing room.");
            }
            if (needsHoldDedicatedRoom(row) && !hasHoldWalkableSpace(loc, row.halfX(), row.halfZ())) {
                critical++;
                addAuditIssue(notes, row.id() + " room has too little walkable two-block air.");
            }
            if (needsHoldDedicatedRoom(row) && !hasHoldRoomCeiling(loc, row)) {
                critical++;
                addAuditIssue(notes, row.id() + " room shell has an open/missing ceiling.");
            }
            if (hasMaterialNear(loc, Math.max(3, site.radius()), Material.BEACON)) {
                critical++;
                addAuditIssue(notes, row.id() + " still has retired beacon material.");
            }
        }

        int gates = 0;
        int sealed = 0;
        for (HoldGate gate : DEEP_HOLD_GATES) {
            Site site = plugin.sites().get(holdGateSiteId(gate.id()));
            Location loc = site == null ? null : site.location();
            if (loc == null || loc.getWorld() == null) {
                critical++;
                addAuditIssue(notes, "gate " + gate.id() + " missing");
                continue;
            }
            gates++;
            if (isHoldGateSealed(gate, loc)) sealed++;
            String gateIssue = auditHoldGateIntegrity(gate, loc);
            if (gateIssue != null) {
                critical++;
                addAuditIssue(notes, gateIssue);
            }
        }

        Site region = plugin.sites().get(HOLD_REGION_SITE_ID);
        Location regionLoc = region == null ? null : region.location();
        if (region == null || regionLoc == null || regionLoc.getWorld() == null) {
            critical++;
            addAuditIssue(notes, "deep hold protection region missing");
        } else if (!"hold_region".equals(region.type()) || !region.protect()) {
            critical++;
            addAuditIssue(notes, "deep hold protection region is not protected/type=hold_region");
        } else {
            int unexpectedFluids = sampleUnexpectedHoldFluids(regionLoc);
            if (unexpectedFluids > 0) {
                critical++;
                addAuditIssue(notes, "controlled geology has unexpected sampled fluid blocks (" + unexpectedFluids + ")");
            }
        }
        Site entryRegion = plugin.sites().get(HOLD_ENTRY_REGION_SITE_ID);
        if (entryRegion != null) {
            Location entryLoc = entryRegion.location();
            if (entryLoc == null || entryLoc.getWorld() == null) {
                critical++;
                addAuditIssue(notes, "deep hold entry stair protection region is not placed");
            } else if (!"hold_region".equals(entryRegion.type()) || !entryRegion.protect()) {
                critical++;
                addAuditIssue(notes, "deep hold entry stair region is not protected/type=hold_region");
            } else if (regionLoc != null && regionLoc.getWorld() != null) {
                String entryIssue = auditHoldEntryRoute(regionLoc, entryRegion);
                if (entryIssue == null) {
                    entryRouteStatus = "walkable";
                } else {
                    entryRouteStatus = "blocked";
                    critical++;
                    addAuditIssue(notes, entryIssue);
                }
            }
        }
        if (regionLoc != null && regionLoc.getWorld() != null) {
            int bx = regionLoc.getBlockX();
            int by = regionLoc.getBlockY() - 2;
            int bz = regionLoc.getBlockZ() - 88;
            String civicIssue = auditHoldCivicShell(regionLoc.getWorld(), bx, by, bz);
            if (civicIssue != null) {
                critical++;
                addAuditIssue(notes, civicIssue);
            }
            String routeIssue = auditHoldEarlyRoute(regionLoc.getWorld(), bx, by, bz);
            if (routeIssue != null) {
                critical++;
                addAuditIssue(notes, routeIssue);
            }
            String enclosureIssue = auditHoldDistrictEnclosure(regionLoc.getWorld(), bx, by, bz);
            if (enclosureIssue != null) {
                critical++;
                addAuditIssue(notes, enclosureIssue);
            }
            String prologueIssue = auditHoldPrologueEcho(regionLoc.getWorld(), bx, by, bz);
            if (prologueIssue == null) {
                recordStations++;
            } else {
                critical++;
                addAuditIssue(notes, prologueIssue);
            }
            for (HoldRecordStation station : DEEP_HOLD_RECORD_STATIONS) {
                String recordIssue = auditHoldRecordStation(regionLoc.getWorld(), bx, by, bz, station);
                if (recordIssue == null) {
                    recordStations++;
                } else {
                    critical++;
                    addAuditIssue(notes, recordIssue);
                }
            }
        }

        sender.sendMessage("== Observance Deep Hold audit ==");
        sender.sendMessage(" hold sites: " + placed + "/" + DEEP_HOLD_SITES.length);
        sender.sendMessage(" gates:      " + gates + "/" + DEEP_HOLD_GATES.length + " (" + sealed + " sealed)");
        sender.sendMessage(" records:    " + recordStations + "/" + (DEEP_HOLD_RECORD_STATIONS.length + 1));
        sender.sendMessage(" region:     " + (regionLoc == null ? "missing" : "protected")
                + (entryRegion == null ? "" : " + entry stair"));
        sender.sendMessage(" entry:      " + entryRouteStatus);
        sender.sendMessage(" critical:   " + critical);
        if (!notes.isEmpty()) {
            sender.sendMessage(" Findings:");
            for (String note : notes) sender.sendMessage("  - " + note);
            if (notes.size() >= 12) sender.sendMessage("  - ...showing first 12 findings only.");
        }
        sender.sendMessage(critical == 0
                ? " Deep Hold looks launch-placeable. Run /obs preflight for whole-plugin readiness."
                : " Fix findings, then rerun /obs placehold audit and /obs preflight.");
    }

    private String auditHoldEntryRoute(Location regionLoc, Site entryRegion) {
        if (regionLoc == null || regionLoc.getWorld() == null || entryRegion == null) {
            return "deep hold entry stair cannot be audited.";
        }
        Location entryLoc = entryRegion.location();
        if (entryLoc == null || entryLoc.getWorld() == null) {
            return "deep hold entry stair protection region is not placed.";
        }
        World world = regionLoc.getWorld();
        if (entryLoc.getWorld() != world) {
            return "deep hold entry stair is in a different world from the Hold.";
        }

        int bx = regionLoc.getBlockX();
        int by = regionLoc.getBlockY() - 2;
        int bz = regionLoc.getBlockZ() - 88;
        int sx = entryLoc.getBlockX();
        int centerY = entryLoc.getBlockY();
        int centerZ = entryLoc.getBlockZ();
        int halfRun = Math.max(30, entryRegion.radius() - 46);
        int mouthZ = centerZ - halfRun;
        int rampEndZ = centerZ + halfRun;
        int surfaceY = centerY + Math.max(8, entryRegion.verticalRadius() - 42);
        int routeEndZ = bz - 98;
        int total = Math.max(1, rampEndZ - mouthZ);

        for (int z = mouthZ + 2; z <= routeEndZ; z += 6) {
            int y = by;
            if (z <= rampEndZ) {
                double t = (z - mouthZ) / (double) total;
                y = (int) Math.round(surfaceY + ((by - surfaceY) * t));
            }
            if (!hasHoldEntryWalkableSlice(world, sx, y, z)) {
                return "deep hold entry stair is blocked or missing walkable clearance near "
                        + sx + "," + y + "," + z + ".";
            }
        }

        int[] landingChecks = {bz - 142, bz - 134, bz - 126, bz - 118, bz - 108, bz - 98};
        for (int z : landingChecks) {
            if (!hasHoldEntryWalkableSlice(world, bx, by, z)) {
                return "deep hold lower landing is blocked before the Return Mouth near "
                        + bx + "," + by + "," + z + ".";
            }
        }
        return null;
    }

    private boolean hasHoldEntryWalkableSlice(World world, int cx, int y, int z) {
        if (world == null) return false;
        for (int dy = -1; dy <= 1; dy++) {
            int yy = y + dy;
            int walkable = 0;
            for (int dx = -6; dx <= 6; dx++) {
                Block floor = world.getBlockAt(cx + dx, yy - 1, z);
                Block feet = world.getBlockAt(cx + dx, yy, z);
                Block head = world.getBlockAt(cx + dx, yy + 1, z);
                if (floor.getType().isSolid()
                        && feet.getType().isAir()
                        && head.getType().isAir()) {
                    walkable++;
                }
            }
            if (walkable >= 7) return true;
        }
        return false;
    }

    private boolean hasHoldWalkableSpace(Location loc, int halfX, int halfZ) {
        if (loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        int open = 0;
        for (int dx = -Math.max(2, halfX - 2); dx <= Math.max(2, halfX - 2); dx++) {
            for (int dz = -Math.max(2, halfZ - 2); dz <= Math.max(2, halfZ - 2); dz++) {
                if (world.getBlockAt(bx + dx, by, bz + dz).getType().isAir()
                        && world.getBlockAt(bx + dx, by + 1, bz + dz).getType().isAir()) {
                    open++;
                    if (open >= 36) return true;
                }
            }
        }
        return false;
    }

    private boolean hasHoldNearbyPlayerSpace(Location loc, int radius, int required) {
        if (loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        int open = 0;
        int r = Math.max(2, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((dx * dx) + (dz * dz) > r * r) continue;
                Block floor = world.getBlockAt(bx + dx, by - 1, bz + dz);
                Block feet = world.getBlockAt(bx + dx, by, bz + dz);
                Block head = world.getBlockAt(bx + dx, by + 1, bz + dz);
                if (floor.getType().isSolid()
                        && feet.getType().isAir()
                        && head.getType().isAir()) {
                    open++;
                    if (open >= Math.max(1, required)) return true;
                }
            }
        }
        return false;
    }

    private boolean hasHoldRoomCeiling(Location loc, HoldSite row) {
        if (loc == null || loc.getWorld() == null || row == null) return false;
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        int halfX = holdRoomHalfX(row);
        int halfZ = holdRoomHalfZ(row);
        int y = by + holdRoomHeight(row);
        int solid = 0;
        int holes = 0;
        for (int dx = -halfX + 1; dx <= halfX - 1; dx += 3) {
            for (int dz = -halfZ + 1; dz <= halfZ - 1; dz += 3) {
                Material material = world.getBlockAt(bx + dx, y, bz + dz).getType();
                if (material.isAir() || material == Material.WATER || material == Material.LAVA) holes++;
                else solid++;
            }
        }
        return solid >= 8 && holes == 0;
    }

    private boolean isHoldGateSealed(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int doorHeight = Math.min(span.height() - 2, 5);
        for (int a = -span.doorHalf(); a <= span.doorHalf(); a++) {
            for (int d = 0; d <= span.depth(); d++) {
                for (int dy = 0; dy <= doorHeight; dy++) {
                    Material material = holdGateBlockAt(world, cx, y, cz, span, a, dy, d).getType();
                    if (material == Material.IRON_BARS || material == Material.BARRIER) return true;
                }
            }
        }
        return false;
    }

    private String auditHoldGateIntegrity(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return "gate missing";
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int doorHeight = Math.min(span.height() - 2, 5);
        boolean sealed = isHoldGateSealed(gate, loc);
        boolean sawDoorBlocker = false;
        for (int a = -span.halfAcross(); a <= span.halfAcross(); a++) {
            boolean doorColumn = Math.abs(a) <= span.doorHalf();
            for (int d = 0; d <= span.depth(); d++) {
                for (int dy = 0; dy < span.height(); dy++) {
                    Material material = holdGateBlockAt(world, cx, y, cz, span, a, dy, d).getType();
                    boolean door = doorColumn && dy <= doorHeight;
                    if (!door && (material.isAir() || material == Material.WATER || material == Material.LAVA)) {
                        return "gate " + gate.id() + " has an air/fluid bypass in the bulkhead.";
                    }
                    if (door && sealed && (material == Material.IRON_BARS || material == Material.BARRIER)) {
                        sawDoorBlocker = true;
                    }
                }
            }
        }
        if (sealed && !sawDoorBlocker) return "gate " + gate.id() + " is marked sealed without door blockers.";
        if (!sealed && !hasHoldGateOpenClearance(gate, loc)) {
            return "gate " + gate.id() + " is open but does not have group-width walkable clearance.";
        }
        if (sealed && hasHoldGateSideBypass(gate, loc)) {
            return "gate " + gate.id() + " has walkable air around the sealed return wall.";
        }
        if (sealed && hasHoldGateOverBypass(gate, loc)) {
            return "gate " + gate.id() + " has walkable air over the sealed bulkhead.";
        }
        return null;
    }

    private boolean hasHoldGateOpenClearance(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int clearSlices = 0;
        for (int d = 0; d <= span.depth(); d++) {
            int walkable = 0;
            for (int a = -span.doorHalf(); a <= span.doorHalf(); a++) {
                Block floor = holdGateBlockAt(world, cx, y, cz, span, a, -1, d);
                Block feet = holdGateBlockAt(world, cx, y, cz, span, a, 0, d);
                Block head = holdGateBlockAt(world, cx, y, cz, span, a, 1, d);
                if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                    walkable++;
                }
            }
            if (walkable >= 7) clearSlices++;
        }
        return clearSlices >= Math.max(1, span.depth());
    }

    private boolean hasHoldGateSideBypass(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int returnWidth = holdGateReturnWidth(span);
        for (int side : new int[]{-1, 1}) {
            for (int a = span.halfAcross() + 1; a <= span.halfAcross() + returnWidth; a++) {
                int across = side * a;
                for (int d = -10; d <= span.depth() + 14; d++) {
                    Block feet = holdGateBlockAt(world, cx, y, cz, span, across, 0, d);
                    Block head = holdGateBlockAt(world, cx, y, cz, span, across, 1, d);
                    Block floor = holdGateBlockAt(world, cx, y, cz, span, across, -1, d);
                    if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasHoldGateOverBypass(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int y = loc.getBlockY();
        int cz = loc.getBlockZ();
        HoldGateSpan span = holdGateSpan(gate);
        int returnWidth = holdGateReturnWidth(span);
        for (int across = -span.halfAcross() - returnWidth; across <= span.halfAcross() + returnWidth; across += 2) {
            for (int d = -8; d <= span.depth() + 12; d += 2) {
                for (int dy = span.height(); dy <= span.height() + 14; dy++) {
                    Block floor = holdGateBlockAt(world, cx, y, cz, span, across, dy, d);
                    Block feet = holdGateBlockAt(world, cx, y, cz, span, across, dy + 1, d);
                    Block head = holdGateBlockAt(world, cx, y, cz, span, across, dy + 2, d);
                    if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String auditHoldCivicShell(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold civic shell has no world loaded.";
        int openFloor = 0;
        for (int[] p : new int[][]{
                {-54, -42}, {0, -42}, {54, -42},
                {-62, -8}, {0, -8}, {62, -8},
                {-48, 34}, {0, 34}, {48, 34}
        }) {
            int x = bx + p[0];
            int z = bz + p[1];
            if (world.getBlockAt(x, by - 1, z).getType().isSolid()
                    && world.getBlockAt(x, by, z).getType().isAir()
                    && world.getBlockAt(x, by + 1, z).getType().isAir()
                    && world.getBlockAt(x, by + 6, z).getType().isAir()) {
                openFloor++;
            }
        }
        if (openFloor < 7) {
            return "deep hold civic shell is too cramped or blocked (" + openFloor
                    + "/9 grand-court samples walkable).";
        }

        int terrace = 0;
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{bz - 54, bz - 18, bz + 18, bz + 54}) {
                int x = bx + (side * 60);
                if (world.getBlockAt(x, by + 5, z).getType().isSolid()
                        && world.getBlockAt(x, by + 6, z).getType().isAir()
                        && world.getBlockAt(x, by + 7, z).getType().isAir()) {
                    terrace++;
                }
            }
        }
        if (terrace < 6) {
            return "deep hold civic terraces are missing or blocked (" + terrace
                    + "/8 samples walkable).";
        }
        return null;
    }

    private String auditHoldEarlyRoute(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold early route has no world loaded.";
        int checked = 0;
        int open = 0;
        for (int z = bz - 142; z <= bz + 32; z += 6) {
            checked++;
            if (hasHoldGroupWalkableSlice(world, bx, by, z, 8, 11)) open++;
        }
        if (open < checked) {
            return "deep hold early route is blocked or too narrow between the lower landing and court ("
                    + open + "/" + checked + " samples group-walkable).";
        }
        for (int[] point : new int[][]{{bx - 42, bz - 34}, {bx + 42, bz - 34}, {bx - 38, bz + 20}, {bx + 38, bz + 20}}) {
            if (!hasHoldGroupWalkableSlice(world, point[0], by, point[1], 3, 5)) {
                return "deep hold Keeper Court approach is blocked near "
                        + point[0] + "," + by + "," + point[1] + ".";
            }
        }
        return null;
    }

    private boolean hasHoldGroupWalkableSlice(World world, int cx, int y, int z, int halfWidth, int required) {
        if (world == null) return false;
        int walkable = 0;
        for (int dx = -Math.max(1, halfWidth); dx <= Math.max(1, halfWidth); dx++) {
            Block floor = world.getBlockAt(cx + dx, y - 1, z);
            Block feet = world.getBlockAt(cx + dx, y, z);
            Block head = world.getBlockAt(cx + dx, y + 1, z);
            if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                walkable++;
            }
        }
        return walkable >= Math.max(1, required);
    }

    private String auditHoldDistrictEnclosure(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold enclosure has no world loaded.";
        for (int[] p : new int[][]{
                {-190, -132, 6}, {190, -132, 6},
                {-190, -32, 18}, {190, -32, 18},
                {-190, 64, 18}, {190, 64, 18},
                {-174, 158, 14}, {174, 158, 14},
                {-144, 224, -8}, {144, 224, -8},
                {-144, 312, -8}, {144, 312, -8}
        }) {
            Material material = world.getBlockAt(bx + p[0], by + p[2], bz + p[1]).getType();
            if (material.isAir() || material == Material.WATER || material == Material.LAVA) {
                return "deep hold side envelope is open near "
                        + (bx + p[0]) + "," + (by + p[2]) + "," + (bz + p[1]) + ".";
            }
        }
        for (int[] p : new int[][]{
                {0, -108, 36}, {0, -8, 58}, {-88, 112, 42}, {88, 96, 42},
                {0, 156, 36}, {0, 252, 34}, {76, 214, 22}
        }) {
            Material material = world.getBlockAt(bx + p[0], by + p[2], bz + p[1]).getType();
            if (material.isAir() || material == Material.WATER || material == Material.LAVA) {
                return "deep hold district ceiling is open near "
                        + (bx + p[0]) + "," + (by + p[2]) + "," + (bz + p[1]) + ".";
            }
        }
        return null;
    }

    private String auditHoldPrologueEcho(World world, int bx, int by, int bz) {
        if (world == null) return "covered copy record has no world loaded.";
        String lecternIssue = auditWrittenLecternAt(world.getBlockAt(bx - 5, by, bz - 74),
                "covered copy", "original belongs above", "covered copy");
        if (lecternIssue != null) return lecternIssue;
        if (!isOccupiedChiseledShelf(world.getBlockAt(bx - 7, by, bz - 74))) {
            return "covered copy shelf is missing or empty.";
        }
        if (!isStandingSignAt(world.getBlockAt(bx - 8, by, bz - 76))) {
            return "covered copy sign is missing.";
        }
        return null;
    }

    private String auditHoldRecordStation(World world, int bx, int by, int bz, HoldRecordStation station) {
        if (world == null || station == null) return "district record station cannot be audited.";
        int x = bx + station.x();
        int y = by + station.y();
        int z = bz + station.z();
        String lecternIssue = auditWrittenLecternAt(world.getBlockAt(x, y, z),
                station.title(), station.requiredFragment(), station.id());
        if (lecternIssue != null) return lecternIssue;

        BlockFace back = station.facing().getOppositeFace();
        BlockFace side = holdLeftOf(station.facing());
        int occupiedShelves = 0;
        for (int s : new int[]{-1, 1}) {
            int sx = x + back.getModX() + (side.getModX() * s);
            int sz = z + back.getModZ() + (side.getModZ() * s);
            if (isOccupiedChiseledShelf(world.getBlockAt(sx, y, sz))) occupiedShelves++;
            if (isOccupiedChiseledShelf(world.getBlockAt(sx, y + 1, sz))) occupiedShelves++;
        }
        if (occupiedShelves < 4) {
            return "district record " + station.id() + " has missing/empty decorative shelves ("
                    + occupiedShelves + "/4).";
        }
        if (!isStandingSignAt(world.getBlockAt(bx + station.signX(), by + station.signY(), bz + station.signZ()))) {
            return "district record " + station.id() + " sign is missing.";
        }
        return null;
    }

    private String auditWrittenLecternAt(Block block, String expectedTitle,
                                         String requiredFragment, String label) {
        if (block == null) return "record " + label + " lectern is missing.";
        if (block.getType() != Material.LECTERN) {
            return "record " + label + " expected a lectern, found " + block.getType() + ".";
        }
        if (!(block.getState() instanceof Lectern lectern)) {
            return "record " + label + " lectern state did not load.";
        }
        ItemStack book = lectern.getInventory().getItem(0);
        if (book == null || book.getType() != Material.WRITTEN_BOOK) {
            return "record " + label + " lectern has no written book.";
        }
        if (expectedTitle != null && book.getItemMeta() instanceof BookMeta meta) {
            String title = meta.getTitle();
            if (title == null || !title.equalsIgnoreCase(expectedTitle)) {
                return "record " + label + " lectern has wrong book title: "
                        + (title == null ? "none" : title) + ".";
            }
            if (requiredFragment != null && !requiredFragment.isBlank()) {
                String pages = String.join("\n", meta.getPages());
                if (!pages.toLowerCase(Locale.ROOT).contains(requiredFragment.toLowerCase(Locale.ROOT))) {
                    return "record " + label + " lectern book is missing required text fragment: "
                            + requiredFragment + ".";
                }
            }
        }
        return null;
    }

    private boolean isStandingSignAt(Block block) {
        return block != null && block.getState() instanceof Sign;
    }

    private boolean isOccupiedChiseledShelf(Block block) {
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) return false;
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf)) return false;
        int max = Math.max(1, shelf.getMaximumOccupiedSlots());
        for (int slot = 0; slot < max; slot++) {
            if (shelf.isSlotOccupied(slot)) return true;
        }
        return false;
    }

    private boolean isMechanicBookshelfEmpty(Block block) {
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) return false;
        if (block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf) {
            int max = Math.max(1, shelf.getMaximumOccupiedSlots());
            for (int slot = 0; slot < max; slot++) {
                if (shelf.isSlotOccupied(slot)) return false;
            }
        }
        if (block.getState() instanceof org.bukkit.block.ChiseledBookshelf cbs) {
            Inventory inv = cbs.getInventory();
            if (inv != null) {
                for (int slot = 0; slot < inv.getSize(); slot++) {
                    ItemStack item = inv.getItem(slot);
                    if (item != null && item.getType() != Material.AIR) return false;
                }
            }
        }
        return true;
    }

    private int sampleUnexpectedHoldFluids(Location regionLoc) {
        if (regionLoc == null || regionLoc.getWorld() == null) return 0;
        World world = regionLoc.getWorld();
        int bx = regionLoc.getBlockX();
        int by = regionLoc.getBlockY() - 2;
        int bz = regionLoc.getBlockZ() - 88;
        int found = 0;
        for (int x = bx - 222; x <= bx + 222; x += 3) {
            for (int z = bz - 234; z <= bz + 434; z += 3) {
                for (int y = by - 60; y <= by + 80; y += 2) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (material != Material.WATER && material != Material.LAVA) continue;
                    if (material == Material.WATER && isAuthoredHoldWater(x, y, z, bx, by, bz)) continue;
                    found++;
                    if (found >= 20) return found;
                }
            }
        }
        return found;
    }

    private boolean isAuthoredHoldWater(int x, int y, int z, int bx, int by, int bz) {
        if (Math.abs(y - by) > 10) return false;
        if (Math.abs(x - (bx - 62)) <= 20 && Math.abs(z - (bz + 170)) <= 16) return true;
        if (Math.abs(x - (bx - 52)) <= 12 && Math.abs(z - (bz + 20)) <= 12) return true;
        if (Math.abs(x - (bx - 62)) <= 12 && Math.abs(z - (bz + 96)) <= 12) return true;
        if (Math.abs(x - (bx - 62)) <= 20 && Math.abs(z - (bz + 170)) <= 18) return true;
        return false;
    }

    private void handlePlaceLab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placelab must be run by a player (needs a location).");
            return;
        }
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance: no sites loaded; reload first.");
            return;
        }

        int spacing = 36;
        if (args.length >= 2) {
            try {
                spacing = Math.max(28, Math.min(56, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        Location origin = player.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int cols = 8;
        int platformRadius = 18;
        int placed = 0;
        int skipped = 0;

        for (Site cfg : plugin.sites().all()) {
            if (cfg == null || !cfg.enabled()) {
                skipped++;
                continue;
            }
            int col = placed % cols;
            int row = placed / cols;
            Location base = new Location(world,
                    origin.getBlockX() + (col * spacing),
                    origin.getBlockY(),
                    origin.getBlockZ() + (row * spacing));

            try {
                base.getChunk().load(true);
                clearLabCell(base, platformRadius, 9);
                buildLabPlatform(base, platformRadius);
                buildLabFixture(cfg, base);
                labelLabCell(base, cfg.id(), cfg.type());

                Site labSite = new Site(cfg.id(), cfg.type(), worldName,
                        (double) base.getBlockX(), (double) base.getBlockY(), (double) base.getBlockZ(),
                        cfg.radius(), cfg.verticalRadius(), cfg.protect(), true,
                        cfg.puzzleKey(), false);
                plugin.registerRuntimeSite(labSite);
                placed++;
            } catch (Throwable t) {
                skipped++;
                sender.sendMessage("  [!] lab skipped " + cfg.id() + " (" + t.getClass().getSimpleName() + ")");
            }
        }

        Location npc = new Location(world, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ() - spacing);
        buildLabPlatform(npc, 7);
        labelLabCell(npc, "npc row", "townsfolk/wren/keeper");
        if (plugin.townsfolk() != null) {
            plugin.townsfolk().spawnAll(npc.clone().add(-3, 1, 0));
        }
        if (plugin.wren() != null) {
            plugin.wren().spawn(npc.clone().add(3, 1, 0));
        }
        if (plugin.keeper() != null) {
            plugin.keeper().spawn(npc.clone().add(6, 1, 0), "lab");
        }

        sender.sendMessage("Observance: placelab complete - " + placed
                + " enabled sites placed in a floating grid, " + skipped + " skipped/disabled.");
        sender.sendMessage("  Origin " + origin.getBlockX() + "," + origin.getBlockY() + ","
                + origin.getBlockZ() + " in " + worldName + "; spacing=" + spacing + ".");
        sender.sendMessage("  This is a test lab. Reset world/plugin data before the real launch placement.");
    }

    /**
     * {@code /observance fullrun [spacing]} - one-command director rehearsal. It creates the floating
     * all-site lab, then adds the late-game staging pieces that used to require several separate commands.
     * This is intentionally a TEST WORLD path: it rewrites runtime site anchors to the lab cells, gives the
     * acting tester the Lens/Needle, places reckoning/finale markers, and carves the Seventh Reading.
     */
    private void handleFullRun(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance fullrun must be run by a player (needs a location).");
            return;
        }
        String spacing = args.length >= 2 ? args[1] : "36";
        sender.sendMessage("== Observance full-run rehearsal ==");
        sender.sendMessage("Step 1/8: building the complete floating placement lab...");
        handlePlaceLab(sender, new String[]{"placelab", spacing});

        sender.sendMessage("Step 2/8: carving the Seventh Reading into the keeper-stone lab cells...");
        handleReadingCarvings(sender);

        sender.sendMessage("Step 3/8: placing Wren reckoning and finale choice markers at your feet...");
        placeReckoningMarkers(player, sender);
        handleFinaleMarkers(sender);

        sender.sendMessage("Step 4/8: giving tester tools...");
        handleLens(sender, new String[]{"lens", "give", player.getName()});
        handleNeedle(sender, new String[]{"needle", player.getName()});

        sender.sendMessage("Step 5/8: staging side/lore NPCs...");
        handleSidePass(sender, new String[]{"sidepass"});

        sender.sendMessage("Step 6/8: staging the puzzle mechanics grid...");
        handlePuzzlePass(sender, new String[]{"puzzlepass", spacing});

        sender.sendMessage("Step 7/8: staging the Watcher dread route...");
        handleDreadPass(sender, new String[]{"dreadpass", "stage", player.getName()});

        sender.sendMessage("Step 8/8: repair + readiness checks...");
        handleRepair(sender);
        handleAudit(sender);
        handleCoverage(sender);

        sender.sendMessage("Observance: fullrun ready. Use this order for the human test pass:");
        sender.sendMessage("  1) Read/open every lab book and sign; lecterns should already contain books.");
        sender.sendMessage("  2) Solve one fixture from each family: bow, chest, bookshelf, lecterns, frames, pool, corridor, vault.");
        sender.sendMessage("  3) Walk /obs visit scare, then run /obs dreadpass run when ready to fire the scare pass.");
        sender.sendMessage("  4) Use /obs puzzlepass gates when you need to jump solved gates instead of replaying the whole chain.");
        sender.sendMessage("  5) Use /obs runbook puzzle, /obs coverage, /obs rehearse start, and /obs visit next; keep this world as rehearsal only.");
    }

    /**
     * {@code /observance prepworld [spacing]} - compact rehearsal-only board. Unlike fullrun, this
     * stays on terrain for fast visual checks, but production clustering is owned by
     * {@code /observance placehold build}.
     */
    private void handlePrepWorld(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance prepworld must be run by a player (needs a location).");
            return;
        }
        Location origin = player.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }

        int spacing = 36;
        if (args.length >= 2) {
            try {
                spacing = Math.max(34, Math.min(48, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        sender.sendMessage("== Observance prepworld ==");
        sender.sendMessage("Step 1/7: staging the prologue lectern and first marker...");
        placeDirectPrologue(origin);

        sender.sendMessage("Step 2/7: placing the surface spine...");
        String[][] surface = {
                {"rune_rosetta", "structure", "6"},
                {"stone_vaun", "keeper_stone", "8"},
                {"stone_mara", "keeper_stone", "8"},
                {"stone_sella", "keeper_stone", "8"},
                {"stone_orin", "keeper_stone", "8"},
                {"stone_brann", "keeper_stone", "8"},
                {"stone_iss", "keeper_stone", "8"},
        };
        int surfacePlaced = placeCompactSpine(origin.clone().add(spacing, 0, 0), surface, spacing);

        sender.sendMessage("Step 3/7: placing the deep payoff sites...");
        String[][] deep = {
                {"stone_of_reckoning", "structure", "6"},
                {"the_cold_hearth", "marker", "6"},
                {"unbroken_light", "accepting_floor", "10"},
                {"the_threshold", "the_threshold", "6"},
                {"the_unwriting", "seventh_shrine", "6"},
                {"threshold_vault", "coop_plate", "6"},
        };
        int deepPlaced = placeCompactSpine(origin.clone().add(spacing, 0, spacing), deep, spacing);

        sender.sendMessage("Step 4/9: placing the Lamp-works/Stair dialogue proof chain...");
        int descentProofPlaced = placeDescentProofChain(origin.clone().add(spacing, 0, -spacing), spacing);

        sender.sendMessage("Step 5/9: placing side-destination proofs as two readable parallel rows...");
        int sideDestinationProofPlaced = 0;
        sideDestinationProofPlaced += placeSchoolStandProof(compactGridCell(origin, 0, 6, spacing, 2));
        sideDestinationProofPlaced += placeFarWaterProof(compactGridCell(origin, 1, 6, spacing, 2));
        sideDestinationProofPlaced += placeMarkersRowProof(compactGridCell(origin, 2, 6, spacing, 2));
        sideDestinationProofPlaced += placeCisternProof(compactGridCell(origin, 3, 6, spacing, 2));
        sideDestinationProofPlaced += placeWatchFloorProof(compactGridCell(origin, 4, 6, spacing, 2));
        sideDestinationProofPlaced += placeSetApartProof(compactGridCell(origin, 5, 6, spacing, 2));
        sideDestinationProofPlaced += placeUndercroftSealProof(compactGridCell(origin, 0, 6, spacing, 3));
        sideDestinationProofPlaced += placeForgottenMouthProof(compactGridCell(origin, 1, 6, spacing, 3));
        sideDestinationProofPlaced += placeDeepMarketProof(compactGridCell(origin, 2, 6, spacing, 3));
        sideDestinationProofPlaced += placeRationTableProof(compactGridCell(origin, 3, 6, spacing, 3));
        sideDestinationProofPlaced += placeThirdBayProof(compactGridCell(origin, 4, 6, spacing, 3));
        sideDestinationProofPlaced += placeWarmTownProof(compactGridCell(origin, 5, 6, spacing, 3));

        sender.sendMessage("Step 6/9: placing Mara page-lock lecterns with books...");
        int lecternsPlaced = placeMaraLecternsAt(origin.clone().add(spacing, 0, spacing * 4), 3);

        sender.sendMessage("Step 7/9: carving reading/finale markers...");
        handleReadingCarvings(sender);
        handleFinaleMarkers(sender);

        sender.sendMessage("Step 8/9: spawning NPC row where possible...");
        Location npc = origin.clone().add(0, 1, -spacing);
        if (plugin.townsfolk() != null) plugin.townsfolk().spawnAll(npc);
        if (plugin.wren() != null) plugin.wren().spawn(npc.clone().add(5, 0, 0));
        if (plugin.keeper() != null) plugin.keeper().spawn(npc.clone().add(9, 0, 0), "prepworld");

        sender.sendMessage("Step 9/9: giving tester tools...");
        handleLens(sender, new String[]{"lens", "give", player.getName()});
        handleNeedle(sender, new String[]{"needle", player.getName()});

        sender.sendMessage("Observance: prepworld complete - prologue staged, " + surfacePlaced
                + "/7 surface sites, " + deepPlaced + "/6 deep sites, " + descentProofPlaced
                + "/5 dialogue-proof sites, " + sideDestinationProofPlaced + "/12 side-destination proofs, "
                + lecternsPlaced + "/5 Mara lecterns.");
        sender.sendMessage("  Compact layout: facing east, rows are Lamp-works proof (north), surface keepers, deep payoff, side proof A, side proof B, Mara books; default spacing avoids platform overlap.");
        sender.sendMessage("  Walkable test order: prologue -> surface row -> side proof rows -> Lamp-works/Stair line -> Mara lecterns -> deep row -> finale.");
        sender.sendMessage("  Optional Nether/End lanes still require standing in that dimension and using /obs site set + /obs placeworld.");
        sender.sendMessage("  Run /obs coverage, /obs rehearse start, and /obs visit next for the walk-through pass.");
    }

    private void handleDescentProof(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance descentproof must be run by a player (needs a location).");
            return;
        }
        Location origin = player.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        int spacing = 18;
        if (args.length >= 2) {
            try {
                spacing = Math.max(12, Math.min(28, Integer.parseInt(args[1].trim())));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }
        int placed = placeDescentProofChain(origin, spacing);
        sender.sendMessage("Observance: descent proof chain staged - " + placed
                + "/5 sites registered: Lamp-works stair, third lamp, painted line, dead-stall, bird coops.");
        sender.sendMessage("  Cross the painted line once, then /obs flag list should show painted_line_crossed.");
        sender.sendMessage("  Visit the coops once: Aro's bird rumor should now point at visible empty cages, not empty air.");
    }

    private int placeDescentProofChain(Location origin, int spacing) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int bx = origin.getBlockX();
        int bz = origin.getBlockZ();
        int step = Math.max(34, spacing);

        Location stair = compactSurfaceCell(world, bx, bz);
        Location thirdLamp = compactSurfaceCell(world, bx + step, bz);
        Location line = compactSurfaceCell(world, bx + (step * 2), bz);
        Location deadStall = compactSurfaceCell(world, bx + (step * 3), bz);
        Location birdCoops = compactSurfaceCell(world, bx + (step * 4), bz);
        prepareCompactCell(stair, 15, 12);
        prepareCompactCell(thirdLamp, 8, 7);
        prepareCompactCell(line, 8, 7);
        prepareCompactCell(deadStall, 10, 7);
        prepareCompactCell(birdCoops, 11, 8);
        buildLampworksStair(stair);
        placePaintedLineFixture(line);
        buildDeadStall(deadStall);
        buildBirdCoops(birdCoops);

        int placed = 0;
        placed += registerRouteProofSite("lampworks_stair", "lampworks_stair", worldName, stair, 14, 12);
        placed += registerRouteProofSite("third_lamp_stand", "lamp_stand", worldName, thirdLamp, 4, 5);
        placed += registerRouteProofSite("painted_line", "painted_line", worldName, line, 4, 5);
        placed += registerRouteProofSite("dead_stall", "dead_stall", worldName, deadStall, 5, 5);
        placed += registerRouteProofSite("deep_bird_coops", "bird_coops", worldName, birdCoops, 6, 5);
        return placed;
    }

    private int registerRouteProofSite(String id, String type, String worldName, Location loc, int radius, int vertical) {
        if (id == null || loc == null || loc.getWorld() == null) return 0;
        plugin.registerRuntimeSite(new Site(id, type, worldName,
                loc.getX(), loc.getY(), loc.getZ(), radius, vertical, true, true, null));
        return 1;
    }

    private int placeFarWaterProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 14, 7);
        placeFarWater(loc);
        seedFixtureLore(loc, "far_water");
        plugin.registerRuntimeSite(new Site("the_far_water", "far_water", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 12, 6, true, true, null));
        return 1;
    }

    private int placeSchoolStandProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 12, 7);
        buildSchoolStand(loc);
        seedFixtureLore(loc, "school_stand");
        plugin.registerRuntimeSite(new Site("school_stand", "school_stand", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 10, 6, true, true, null));
        return 1;
    }

    private int placeMarkersRowProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 14, 7);
        buildMarkersRow(loc);
        seedFixtureLore(loc, "markers_row");
        plugin.registerRuntimeSite(new Site("markers_row", "markers_row", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 12, 6, true, true, null));
        return 1;
    }

    private int placeCisternProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 7);
        buildCisternSeven(loc);
        seedFixtureLore(loc, "cistern_7");
        plugin.registerRuntimeSite(new Site("cistern_7", "cistern_7", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 6, true, true, null));
        return 1;
    }

    private int placeWatchFloorProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 8);
        buildWatchFloor(loc);
        seedFixtureLore(loc, "watch_floor");
        plugin.registerRuntimeSite(new Site("watch_floor", "watch_floor", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 7, true, true, null));
        return 1;
    }

    private int placeSetApartProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 7);
        buildSetApartShelf(loc);
        seedFixtureLore(loc, "set_apart_shelf");
        plugin.registerRuntimeSite(new Site("set_apart_shelf", "set_apart_shelf", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 6, true, true, null));
        return 1;
    }

    private int placeUndercroftSealProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 8);
        buildUndercroftSeal(loc);
        seedFixtureLore(loc, "undercroft_seal");
        plugin.registerRuntimeSite(new Site("undercroft_seal", "undercroft_seal", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 7, true, true, null));
        return 1;
    }

    private int placeForgottenMouthProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 8);
        buildForgottenMouth(loc);
        seedFixtureLore(loc, "forgotten_mouth");
        plugin.registerRuntimeSite(new Site("forgotten_mouth", "forgotten_mouth", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 7, true, true, null));
        return 1;
    }

    private int placeWarmTownProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 13, 8);
        buildWarmTownCollapse(loc);
        seedFixtureLore(loc, "warm_town_collapse");
        plugin.registerRuntimeSite(new Site("warm_town_collapse", "warm_town_collapse", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 11, 7, true, true, null));
        return 1;
    }

    private int placeDeepMarketProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 15, 8);
        buildDeepMarket(loc);
        seedFixtureLore(loc, "deep_market");
        plugin.registerRuntimeSite(new Site("deep_market", "deep_market", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 13, 7, true, true, null));
        return 1;
    }

    private int placeRationTableProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 12, 7);
        buildRationTable(loc);
        seedFixtureLore(loc, "ration_table");
        plugin.registerRuntimeSite(new Site("ration_table", "ration_table", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 10, 6, true, true, null));
        return 1;
    }

    private int placeThirdBayProof(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location loc = new Location(world, x, y, z);
        prepareCompactCell(loc, 14, 8);
        buildThirdBayBreach(loc);
        seedFixtureLore(loc, "third_bay_breach");
        plugin.registerRuntimeSite(new Site("third_bay_breach", "third_bay_breach", world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), 12, 7, true, true, null));
        return 1;
    }

    private void seedFixtureLore(Location base, String id) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        String[] fragments = fixtureLoreFragments(key);
        if (fragments.length == 0) return;

        // Explicit room-local manifests replace the former "first four inventories in a 25x25 scan".
        // Evidence now stays in its authored room and a rebuild clears stale contents deterministically.
        int[][] targets = fixtureLoreTargets(key);
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        int written = Math.min(Math.min(4, fragments.length), targets.length);
        for (int i = 0; i < written; i++) {
            int[] p = targets[i];
            Block block = world.getBlockAt(bx + p[0], by + p[1], bz + p[2]);
            block.setType(Material.BARREL, false);
            faceDirectional(block.getLocation(), p[0] < 0 ? BlockFace.EAST : BlockFace.WEST);
            if (!(block.getState() instanceof InventoryHolder holder)) continue;
            Inventory inv = holder.getInventory();
            if (inv == null) continue;
            inv.clear();
            inv.setItem(0, fixtureLoreBook(key, i, fragments[i]));
            try { block.getState().update(true, false); } catch (Throwable ignored) { }
        }
    }

    private int[][] fixtureLoreTargets(String id) {
        return switch (id == null ? "" : id) {
            case "far_water" -> new int[][]{{7, 0, 3}, {7, 0, 0}, {7, 0, -3}, {4, 0, 4}};
            case "school_stand" -> new int[][]{{5, 0, 4}, {5, 0, 1}, {5, 0, -2}, {3, 0, 4}};
            case "markers_row" -> new int[][]{{8, 0, 2}, {8, 0, 0}, {8, 0, -2}, {6, 0, 3}};
            case "cistern_7" -> new int[][]{{-5, 0, 3}, {-4, 0, 3}, {5, 0, 3}, {4, 0, 3}};
            case "watch_floor" -> new int[][]{{-4, 0, -4}, {-4, 0, 4}, {4, 0, -4}, {4, 0, 4}};
            case "set_apart_shelf" -> new int[][]{{-4, 0, 0}, {-2, 0, 0}, {0, 0, 0}, {2, 0, 0}};
            case "undercroft_seal" -> new int[][]{{5, 0, -2}, {5, 0, 0}, {5, 0, 2}, {3, 0, -3}};
            case "forgotten_mouth" -> new int[][]{{4, 0, -3}, {4, 0, -1}, {4, 0, 1}, {4, 0, 3}};
            case "deep_market" -> new int[][]{{-6, 0, -4}, {-3, 0, -4}, {0, 0, -4}, {3, 0, -4}};
            case "ration_table" -> new int[][]{{-4, 0, 1}, {4, 0, 1}, {-4, 0, -2}, {4, 0, -2}};
            case "third_bay_breach" -> new int[][]{{6, 0, -2}, {6, 0, 0}, {6, 0, 2}, {4, 0, -3}};
            case "warm_town_collapse" -> new int[][]{{-3, 0, 0}, {-2, 0, 0}, {-3, 0, 2}, {-2, 0, 2}};
            case "dead_stall" -> new int[][]{{-2, 0, 0}, {-2, 0, 2}, {2, 0, 2}, {2, 0, 0}};
            case "deep_bird_coops" -> new int[][]{{0, 0, 3}, {-5, 0, 3}, {5, 0, 3}, {0, 0, -3}};
            default -> new int[0][0];
        };
    }

    private int firstEmptyFixtureSlot(Inventory inv) {
        if (inv == null) return -1;
        int size = Math.min(inv.getSize(), 12);
        for (int i = 0; i < size; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) return i;
        }
        return -1;
    }

    private ItemStack fixtureLoreBook(String id, int index, String body) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (book.getItemMeta() instanceof BookMeta meta) {
            meta.setTitle(com.observance.watcher.util.TextFit.clampLine(fixtureLoreTitle(id, index), 32));
            meta.setAuthor("field record");
            String text = body == null ? "" : body;
            if (text.length() <= com.observance.watcher.util.TextFit.BOOK_PAGE_CHARS) {
                meta.addPage(text);
            } else {
                for (String page : com.observance.watcher.util.TextFit.paginate(text)) {
                    meta.addPage(page);
                }
            }
            book.setItemMeta(meta);
        }
        return book;
    }

    private void putWrittenBook(Block block, int slot, String title, String author, List<String> pages) {
        if (block == null) return;
        var state = block.getState();
        if (!(state instanceof InventoryHolder holder)) return;
        Inventory inv = holder.getInventory();
        if (inv == null || inv.getSize() <= 0) return;
        int target = Math.max(0, Math.min(inv.getSize() - 1, slot));
        inv.setItem(target, writtenRecordBook(title, author, pages));
        try { state.update(true, false); } catch (Throwable ignored) { }
    }

    private ItemStack writtenRecordBook(String title, String author, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (book.getItemMeta() instanceof BookMeta meta) {
            meta.setTitle(com.observance.watcher.util.TextFit.clampLine(title == null ? "record" : title, 32));
            meta.setAuthor(com.observance.watcher.util.TextFit.clampLine(author == null ? "the record" : author, 32));
            List<String> realPages = pages == null || pages.isEmpty() ? List.of("") : pages;
            for (String page : realPages) {
                String body = page == null ? "" : page;
                if (body.length() <= com.observance.watcher.util.TextFit.BOOK_PAGE_CHARS) {
                    meta.addPage(body);
                } else {
                    for (String real : com.observance.watcher.util.TextFit.paginate(body)) {
                        meta.addPage(real);
                    }
                }
            }
            book.setItemMeta(meta);
        }
        return book;
    }

    private String fixtureLoreTitle(String id, int index) {
        String prefix = switch (id) {
            case "far_water" -> "shore note";
            case "school_stand" -> "copybook";
            case "markers_row" -> "marker count";
            case "cistern_7" -> "cistern slip";
            case "watch_floor" -> "watch log";
            case "set_apart_shelf" -> "shelf card";
            case "undercroft_seal" -> "mason rest";
            case "forgotten_mouth" -> "route draft";
            case "warm_town_collapse" -> "warden file";
            case "deep_market" -> "market ledger";
            case "ration_table" -> "ration form";
            case "third_bay_breach" -> "bay report";
            case "dead_stall" -> "stall docket";
            case "deep_bird_coops" -> "coop register";
            default -> "field note";
        };
        return prefix + " " + (index + 1);
    }

    private String[] fixtureLoreFragments(String id) {
        return switch (id) {
            case "far_water" -> new String[]{
                    "Shore watch note:\n\nAt dusk the far pool reflects six pale stones and one grey. Count the real bank first; the water is only useful after the land count is honest.",
                    "Teacher to Sella:\n\n\"Your letters are correct. Your order is not. Bring the slate back before rain gets into it.\"",
                    "Found under a wet plank:\n\nSmall prints at the reeds, adult boots after. No return marks on the mud side.",
                    "Archive tag:\n\nFile with Sella, not with weather. This place proves the grey seventh was seen before anyone called it rumor."
            };
            case "school_stand" -> new String[]{
                    "Copybook exercise:\n\nKeep your light.\nCount your light.\nReturn your light.\n\nThe last line is crossed out by a different hand.",
                    "Teacher's slate:\n\nIf a child copies a rule backward, check whether the room taught it backward first. This is before the far-water mistake.",
                    "Dismissal note:\n\nSella stayed after class. Brann waited by the door but did not come in.",
                    "Conversation copied by Nessa:\n\n\"She only wrote what she saw.\"\n\"Then move what she saw.\""
            };
            case "markers_row" -> new String[]{
                    "Mason tally:\n\nSix bow stones set level. One grey marker added after payment and not in the first order.",
                    "Road crew note:\n\nDo not repaint the arrow until Orin confirms whether the low mark is a mistake. His hall repeats the count lower.",
                    "Complaint:\n\nTravelers bow at the wrong place because the grey marker looks official.",
                    "Count card:\n\nBegin at the chipped edge. Starting clean changes the answer."
            };
            case "cistern_7" -> new String[]{
                    "Cistern receipt:\n\nGood oil: two jars.\nSpoiled oil: one jar.\nSeven cups reserved for the lower work.",
                    "Utility notice:\n\nThe seventh measure is held back until all lamps are named. Vaun objected in writing.",
                    "Complaint left in the drain:\n\nSomeone keeps calling it waste. It is not waste if the stair stays lit. The first dark was managed, not weather.",
                    "Inspection line:\n\nCopper bulb replaced. Water line remains below safe mark."
            };
            case "watch_floor" -> new String[]{
                    "Shift log:\n\nBlack moon watch. No bed on floor. Three bell tolls required before dawn.",
                    "Handoff note:\n\nBrann took the second watch. Chair warm, lamp low, door still barred.",
                    "Witness line:\n\nI saw smoke before I heard the bell. Do not write that the other way around; it changes who failed the watch.",
                    "Floor chalk:\n\nThe sleeping mark was scrubbed once. You can still see it by the west post."
            };
            case "set_apart_shelf" -> new String[]{
                    "Shelf card, entry five:\n\nSet apart with warm lamp. Do not shelve with public copies; this is where the warm story starts to split.",
                    "Archivist note:\n\nReaders keep grabbing entry five because it looks important. It is important because it was isolated.",
                    "Iss to Mara:\n\n\"Leave the correction where a careful reader can find it, not where a loud one can wave it around.\"",
                    "Lamp record:\n\nCold shelves: four. Warm shelf: one. Missing: one book, often."
            };
            case "undercroft_seal" -> new String[]{
                    "Mason work order:\n\nSeal readable only when approached low from the east side. Do not raise the line.",
                    "Apprentice note:\n\nI asked why the writing sits under eye level. Orin said proud people miss useful things. This is the bow before the door.",
                    "Repair ticket:\n\nCrack at lower mark is old. Leave it; the crack points to the correct side.",
                    "Delivery slip:\n\nTwo slabs refused for being too tall."
            };
            case "forgotten_mouth" -> new String[]{
                    "Route draft:\n\nThe way up was real. Surface entry later filled and called natural collapse.",
                    "Surveyor correction:\n\nDo not write forgotten. The mouth was covered while people still remembered it.",
                    "Conversation in margin:\n\n\"If they can come back up, they can contradict us.\"\n\"Then the mouth was never there.\"\n\nThis is not a wall. It is a cover story.",
                    "Tool tally:\n\nFour shovels returned. One pick missing. Dirt still fresh on all handles."
            };
            case "warm_town_collapse" -> new String[]{
                    "WARDEN-3 closure:\n\nEast market sealed after staged hearth report. Public reason: unsafe wall.",
                    "Witness refusal:\n\nI will not sign that the room was warm when I found it.",
                    "Supply ledger:\n\nFresh wood delivered after the collapse. Iss approved payment anyway. Compare this to his safe-road notice.",
                    "Inspection note:\n\nSmoke stain runs the wrong direction for the story filed."
            };
            case "deep_market" -> new String[]{
                    "Stall ledger:\n\nThree heads counted at ration table. One name paid twice. One name missing. This is before WARDEN-3 closed the warm road.",
                    "Market argument:\n\n\"You cannot sell light.\"\n\"Then stop asking me to keep yours.\"",
                    "Crate label:\n\nLamp glass for lower work. Do not stack near bread.",
                    "Closing note:\n\nMarket bell failed. Watch floor bell answered late."
            };
            case "ration_table" -> new String[]{
                    "Ration form R14:\n\nThree heads. One and a half loaves. Half loaf kept for the absent watcher.",
                    "Table scratch:\n\nIf the count includes the absent, the bread proves who was expected.",
                    "Receipt:\n\nMara took no bread. Sella took half. Vaun disputed both entries. The table ties people to the market, not to myth.",
                    "Kitchen note:\n\nDo not let Iss rewrite ration forms after serving."
            };
            case "third_bay_breach" -> new String[]{
                    "Incident report:\n\nMark 33: line broke downward. Third bay took water first. This is why the deep line is evidence, not scenery.",
                    "Repair crew note:\n\nDo not patch over the carved number. The number is why we know which bay lied.",
                    "Witness line:\n\nI heard the lower room before the wall opened.",
                    "Tool return:\n\nThree buckets lost, two ropes cut, one lantern recovered still warm."
            };
            case "dead_stall" -> new String[]{
                    "Stall docket:\n\nThe owner stopped arriving before the market closed. The stall remained in the paid count.",
                    "Spoilage form:\n\nOne missing bird was entered as loss after the cage had already been found open.",
                    "Neighbor statement:\n\nThe lamp was cold at opening. Do not let the closure copy call it abandoned later.",
                    "Counter scratch:\n\nPaid, absent, counted. The three words were written by different hands."
            };
            case "deep_bird_coops" -> new String[]{
                    "Coop register:\n\nSix enclosures paid. Six birds entered. The grey mark beside them is not another cage.",
                    "Feed tally:\n\nThe seventh share was carried past the coops and never entered as feed.",
                    "Keeper note:\n\nA witness place is not an empty pen. Stop correcting the count to seven cages.",
                    "Latch report:\n\nThird latch opened from outside. No feather trail returns through the market."
            };
            default -> new String[0];
        };
    }

    private void buildLampworksStair(Location start) {
        org.bukkit.World world = start.getWorld();
        if (world == null) return;
        int bx = start.getBlockX();
        int by = start.getBlockY();
        int bz = start.getBlockZ();

        placeRouteLanding(world, bx, by, bz - 3, 5, 4);
        for (int i = 0; i <= 36; i++) {
            int y = by - (i / 3);
            int z = bz + i;
            for (int dx = -3; dx <= 3; dx++) {
                Material tread = (i == 24) ? Material.BLACK_CONCRETE : Material.POLISHED_DEEPSLATE_STAIRS;
                placeStairTread(world.getBlockAt(bx + dx, y, z), tread);
                world.getBlockAt(bx + dx, y - 1, z).setType(Material.DEEPSLATE_BRICKS, false);
                if (dx == -3 || dx == 3 || (dx == 0 && i % 3 == 0)) {
                    supportToGround(world, bx + dx, y - 1, z, supportMaterial(i + dx));
                }
            }
            world.getBlockAt(bx - 4, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(bx + 4, y, z).setType(Material.DEEPSLATE_BRICK_WALL, false);
            if (i % 3 == 0) {
                supportToGround(world, bx - 4, y, z, Material.POLISHED_BLACKSTONE_BRICK_WALL);
                supportToGround(world, bx + 4, y, z, Material.POLISHED_BLACKSTONE_BRICK_WALL);
            }
            if (i == 4 || i == 10 || i == 18 || i == 24 || i == 30 || i == 36) {
                placeRouteRib(world, bx, y, z);
            }
        }
        placeRouteLanding(world, bx, by - 12, bz + 38, 5, 5);

        placeLampStand(new Location(world, bx - 4, by, bz + 3), 1, true);
        placeLampStand(new Location(world, bx + 4, by - 2, bz + 9), 2, true);
        placeLampStand(new Location(world, bx - 4, by - 4, bz + 18), 3, false);
        placeLampStand(new Location(world, bx + 4, by - 7, bz + 27), 4, true);
        placeLampStand(new Location(world, bx - 4, by - 10, bz + 33), 5, true);

        placeEvidenceLectern(new Location(world, bx + 5, by + 1, bz + 2), BlockFace.WEST,
                "lampworks ledger", List.of(
                        "oil was counted at the upper stair.",
                        "stand three was a dry cup.",
                        "do not count below the black step."
                ));
        world.getBlockAt(bx + 5, by - 7, bz + 24).setType(Material.BLACK_CANDLE, false);
    }

    private void placeStairTread(Block block, Material material) {
        if (block == null) return;
        block.setType(material, false);
        if (material.name().endsWith("_STAIRS") && block.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.SOUTH);
            block.setBlockData(d, false);
        }
    }

    private void placeLampStand(Location loc, int number, boolean lit) {
        if (loc == null || loc.getWorld() == null) return;
        org.bukkit.World world = loc.getWorld();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        supportToGround(world, x, y, z, Material.CUT_COPPER);
        world.getBlockAt(x, y, z).setType(Material.CUT_COPPER, false);
        world.getBlockAt(x, y + 1, z).setType(Material.IRON_BARS, false);
        Block lamp = world.getBlockAt(x, y + 2, z);
        lamp.setType(lit ? Material.LANTERN : materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (!lit && lamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            lamp.setBlockData(light, false);
        }
        world.getBlockAt(x, y, z - 1).setType(lit ? Material.CUT_COPPER : Material.WEATHERED_CUT_COPPER, false);
    }

    private void buildDeadStall(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        // Larger market alcove around the offering counter, so Wenna's errand points at a real place.
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                boolean rim = Math.abs(dx) == 5 || dz == -3 || dz == 4;
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.COBBLED_DEEPSLATE, false);
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                if (rim) {
                    for (int dy = 0; dy <= 3; dy++) {
                        world.getBlockAt(bx + dx, by + dy, bz + dz)
                                .setType(dy == 3 ? Material.BLACKSTONE : Material.DEEPSLATE_BRICKS, false);
                    }
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || dz == -1 || dz == 2;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(edge ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                if (edge && (dx == -2 || dx == 2) && (dz == -1 || dz == 2)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-1, 2}) {
                world.getBlockAt(bx + dx, by, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
                world.getBlockAt(bx + dx, by + 1, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
                world.getBlockAt(bx + dx, by + 2, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean lip = Math.abs(dx) == 2 || dz == -1 || dz == 2;
                world.getBlockAt(bx + dx, by + 3, bz + dz)
                        .setType(lip ? Material.DARK_OAK_SLAB : Material.SPRUCE_SLAB, false);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(bx + dx, by, bz + 2).setType(Material.BARREL, false);
            placeDecorativeBookshelf(world.getBlockAt(bx + dx, by + 1, bz + 2), dx + 7);
        }
        world.getBlockAt(bx - 2, by, bz).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx + 2, by, bz).setType(Material.CANDLE, false);
        world.getBlockAt(bx, by, bz).setType(Material.CAKE, false);
        placeEvidenceLectern(new Location(world, bx, by, bz - 1), BlockFace.SOUTH,
                "stall slate", List.of(
                        "leave a crust.",
                        "take nothing.",
                        "the dead stall records giving, not buying."
                ));
    }

    private void buildBirdCoops(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        // Larger coop room: empty cages should be visible from the approach, not just a three-block prop row.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -3; dz <= 5; dz++) {
                boolean rim = Math.abs(dx) == 6 || dz == -3 || dz == 5;
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.COBBLED_DEEPSLATE, false);
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        world.getBlockAt(bx + dx, by + dy, bz + dz)
                                .setType(dy == 4 ? Material.BLACKSTONE
                                        : (dy == 1 ? Material.IRON_BARS : Material.DEEPSLATE_BRICKS), false);
                    }
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 3 || dz == -1 || dz == 2;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(edge ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
                if (edge && (dx == -3 || dx == 3 || dz == -1 || dz == 2)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            int x = bx - 2 + (i * 2);
            world.getBlockAt(x, by, bz).setType(Material.IRON_BARS, false);
            world.getBlockAt(x, by + 1, bz).setType(Material.IRON_BARS, false);
            world.getBlockAt(x, by + 2, bz).setType(Material.IRON_BARS, false);
            world.getBlockAt(x, by, bz + 1).setType(Material.DARK_OAK_FENCE, false);
            world.getBlockAt(x, by + 1, bz + 1).setType(Material.IRON_BARS, false);
            world.getBlockAt(x, by, bz + 2).setType(i == 2 ? Material.BARREL : Material.HAY_BLOCK, false);
        }
        world.getBlockAt(bx + 3, by, bz).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx, by, bz + 1).setType(Material.BROWN_CARPET, false);
        world.getBlockAt(bx - 4, by, bz + 3).setType(Material.BONE_BLOCK, false);
        world.getBlockAt(bx - 3, by, bz + 3).setType(Material.WHITE_CARPET, false);
        world.getBlockAt(bx + 4, by, bz + 3).setType(Material.CALCITE, false);
        world.getBlockAt(bx, by, bz - 1).setType(Material.LIGHT_GRAY_CARPET, false);
    }

    private void buildSchoolStand(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 8, 6, 6, 5,
                Material.DEEPSLATE_TILES, Material.SMOOTH_BASALT,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.LANTERN);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 3;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx + dz) % 4 == 0 ? Material.SMOOTH_BASALT : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 4 || Math.abs(dz) == 3)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by, bz + 3).setType(Material.BLACK_CONCRETE, false);
            world.getBlockAt(bx + dx, by + 1, bz + 3).setType(Material.BLACK_CONCRETE, false);
            if (Math.abs(dx) == 3) {
                world.getBlockAt(bx + dx, by + 2, bz + 3).setType(Material.DEEPSLATE_BRICK_WALL, false);
            }
        }
        world.getBlockAt(bx - 3, by + 2, bz + 3).setType(Material.LANTERN, false);

        for (int dx = -3; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by, bz + 1).setType(Material.COBBLESTONE, false);
        }
        world.getBlockAt(bx + 3, by, bz + 1).setType(Material.GRAY_CONCRETE, false);
        world.getBlockAt(bx + 3, by + 1, bz + 1).setType(Material.GRAY_CANDLE, false);

        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by, bz - 1).setType(Material.DARK_OAK_SLAB, false);
            world.getBlockAt(bx + dx, by, bz - 2).setType(Material.DARK_OAK_SLAB, false);
        }
        placeDecorativeBookshelf(world.getBlockAt(bx - 3, by, bz - 2), 11);
        world.getBlockAt(bx + 3, by, bz - 2).setType(Material.BARREL, false);
        world.getBlockAt(bx, by + 1, bz + 3).setType(Material.WHITE_CARPET, false);

        placeEvidenceLectern(new Location(world, bx - 4, by, bz), BlockFace.EAST,
                "copy line", List.of(
                        "keep your light.",
                        "keep your light.",
                        "six stones copied the line. the grey one did not."
                ));
        world.getBlockAt(bx + 4, by, bz).setType(Material.LIGHT_GRAY_CARPET, false);
    }

    private void buildMarkersRow(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 10, 4, 4, 5,
                Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_BRICKS,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.SOUL_LANTERN);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 2;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (dz == 0 ? Material.DEEPSLATE_TILES : Material.CRACKED_DEEPSLATE_BRICKS);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 7 || Math.abs(dz) == 2)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = bx - 6 + (i * 2);
            world.getBlockAt(x, by, bz).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(x, by + 1, bz).setType(Material.COBBLED_DEEPSLATE_WALL, false);
            world.getBlockAt(x, by, bz - 1).setType(Material.BROWN_CARPET, false);
        }
        world.getBlockAt(bx + 7, by - 1, bz).setType(Material.SOUL_SAND, false);
        world.getBlockAt(bx + 7, by, bz).setType(Material.GRAY_CONCRETE, false);
        world.getBlockAt(bx + 7, by, bz - 1).setType(Material.AIR, false);
        world.getBlockAt(bx + 7, by + 1, bz).setType(Material.GRAY_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx - 7, by, bz + 2), BlockFace.SOUTH,
                "marker row", List.of(
                        "six stones were set for bowing.",
                        "one hollow was left at the end.",
                        "count again after the winter mark."
                ));
        world.getBlockAt(bx + 4, by, bz + 2).setType(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, false);
    }

    private void buildCisternSeven(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 9, 9, 9, 5,
                Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE,
                Material.PRISMARINE, Material.POLISHED_BLACKSTONE_BRICKS, Material.SEA_LANTERN);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                int dist = Math.abs(dx) + Math.abs(dz);
                boolean rim = dist >= 6 || Math.abs(dx) == 5 || Math.abs(dz) == 5;
                boolean water = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                Material floor = water ? Material.DARK_PRISMARINE : (rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.PRISMARINE_BRICKS);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (water) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.WATER, false);
                } else if (!rim) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DARK_PRISMARINE, false);
                }
                if (rim && (Math.abs(dx) == 5 || Math.abs(dz) == 5)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by + 2, bz - 5).setType(Material.END_STONE_BRICKS, false);
            if (Math.abs(dx) == 3) {
                world.getBlockAt(bx + dx, by + 1, bz - 5).setType(Material.END_STONE_BRICKS, false);
                world.getBlockAt(bx + dx, by, bz - 5).setType(Material.END_STONE_BRICKS, false);
            }
        }
        world.getBlockAt(bx - 4, by, bz + 3).setType(Material.BARREL, false);
        world.getBlockAt(bx - 3, by, bz + 3).setType(Material.BARREL, false);
        world.getBlockAt(bx + 3, by, bz + 3).setType(Material.LANTERN, false);
        world.getBlockAt(bx + 3, by - 1, bz + 1).setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.EAST,
                "cistern seven", List.of(
                        "good oil: two jars.",
                        "the water gave light back wrong.",
                        "the seventh mark is under the dark surface."
                ));
        world.getBlockAt(bx + 5, by, bz - 2).setType(Material.DARK_PRISMARINE_SLAB, false);
    }

    private void buildWatchFloor(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 9, 7, 7, 6,
                Material.BLACKSTONE, Material.DEEPSLATE_TILES,
                Material.COBBLED_DEEPSLATE, Material.BLACK_CONCRETE, Material.SOUL_LANTERN);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 4;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx + dz) % 3 == 0 ? Material.BLACKSTONE : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 5 || Math.abs(dz) == 4)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        Block lectern = world.getBlockAt(bx, by, bz);
        lectern.setType(Material.LECTERN, false);
        if (lectern.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.SOUTH);
            lectern.setBlockData(d, false);
        }
        fillWrittenLecternBook(lectern, "watch floor", "the record", List.of(
                "black moon.",
                "do not sleep on the floor of the watch.",
                "the log stopped writing before the keepers stopped standing."
        ));
        for (int dx : new int[]{-4, 4}) {
            for (int dz : new int[]{-3, 3}) {
                world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                world.getBlockAt(bx + dx, by + 1, bz + dz).setType(Material.SOUL_LANTERN, false);
            }
        }
        placeDecorativeBookshelf(world.getBlockAt(bx - 2, by, bz), 17);
        world.getBlockAt(bx + 2, by, bz).setType(Material.DAYLIGHT_DETECTOR, false);
        world.getBlockAt(bx, by, bz + 2).setType(Material.BLACK_CANDLE, false);
        world.getBlockAt(bx - 4, by, bz - 4).setType(Material.BLACK_CARPET, false);
        world.getBlockAt(bx + 4, by, bz - 4).setType(Material.LIGHT_GRAY_CARPET, false);
    }

    private void buildSetApartShelf(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 9, 6, 7, 5,
                Material.POLISHED_DEEPSLATE, Material.OXIDIZED_COPPER,
                Material.CUT_COPPER, Material.WEATHERED_CUT_COPPER, Material.LANTERN);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 5 || dz == -3 || dz == 4;
                Material floor = edge ? Material.CUT_COPPER
                        : ((dx + dz) % 3 == 0 ? Material.OXIDIZED_COPPER : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 5 || dz == 4)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.CUT_COPPER);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            int x = bx - 4 + (i * 2);
            world.getBlockAt(x, by, bz).setType(Material.BARREL, false);
            world.getBlockAt(x, by + 1, bz).setType(i == 4 ? Material.LANTERN : Material.SOUL_LANTERN, false);
            if (i == 4) {
                world.getBlockAt(x, by, bz + 1).setType(
                        materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
            } else {
                placeDecorativeBookshelf(world.getBlockAt(x, by, bz + 1), i + 23);
            }
        }
        world.getBlockAt(bx, by, bz - 1).setType(Material.REDSTONE_LAMP, false);
        world.getBlockAt(bx + 4, by, bz + 3).setType(Material.BLACK_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "entry five shelf", List.of(
                        "entry five was set apart with a warm lamp.",
                        "do not price it.",
                        "do not count it with the cold shelf."
                ));
        world.getBlockAt(bx + 5, by, bz + 2).setType(Material.WEATHERED_CUT_COPPER, false);
    }

    private void buildUndercroftSeal(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 9, 6, 7, 6,
                Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_BRICKS,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.REINFORCED_DEEPSLATE, Material.SOUL_LANTERN);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -3; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 5 || dz == -3 || dz == 4;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 5 || dz == 4)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                boolean frame = Math.abs(dx) == 3 || dy == 4;
                Material mat = frame ? Material.POLISHED_DEEPSLATE : Material.CHISELED_DEEPSLATE;
                world.getBlockAt(bx + dx, by + dy, bz + 2).setType(mat, false);
            }
        }
        world.getBlockAt(bx, by, bz + 1).setType(Material.IRON_BARS, false);
        world.getBlockAt(bx - 1, by, bz + 1).setType(Material.BLACKSTONE, false);
        world.getBlockAt(bx + 1, by, bz + 1).setType(Material.BLACKSTONE, false);
        world.getBlockAt(bx, by + 1, bz - 1).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx - 4, by, bz).setType(Material.GRAY_CARPET, false);
        world.getBlockAt(bx - 3, by, bz).setType(Material.GRAY_CARPET, false);
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "mason's rest", List.of(
                        "the seal was entered from the wrong side.",
                        "the mason cut the last line low.",
                        "bow to read what was not small."
                ));
        world.getBlockAt(bx + 4, by - 1, bz).setType(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, false);
    }

    private void buildForgottenMouth(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 8, 8, 9, 6,
                Material.POLISHED_BLACKSTONE, Material.GRASS_BLOCK,
                Material.BLACKSTONE, Material.DEEPSLATE_BRICKS, Material.SEA_LANTERN);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 5;
                Material floor = edge ? Material.DEEPSLATE_BRICKS
                        : (dz >= 2 ? Material.GRASS_BLOCK : Material.POLISHED_BLACKSTONE);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 4 || Math.abs(dz) == 5)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.DEEPSLATE_BRICKS);
                }
            }
        }

        for (int dz = -4; dz <= 1; dz++) {
            for (int dx : new int[]{-2, 2}) {
                int height = dz < -1 ? 4 : 3;
                for (int dy = 0; dy <= height; dy++) {
                    world.getBlockAt(bx + dx, by + dy, bz + dz).setType(Material.BLACKSTONE, false);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by + 4, bz - 4).setType(Material.CHISELED_DEEPSLATE, false);
        }
        world.getBlockAt(bx, by, bz - 5).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(bx, by, bz + 4).setType(Material.GLOWSTONE, false);
        world.getBlockAt(bx - 1, by, bz + 3).setType(Material.OAK_LEAVES, false);
        world.getBlockAt(bx + 1, by, bz + 3).setType(Material.OAK_LEAVES, false);
        world.getBlockAt(bx, by, bz + 5).setType(Material.LANTERN, false);
        placeEvidenceLectern(new Location(world, bx - 3, by, bz - 5), BlockFace.SOUTH,
                "way up draft", List.of(
                        "the way up was real.",
                        "it cost the line.",
                        "the last return mark healed where the surface could still remember it."
                ));
        world.getBlockAt(bx + 3, by, bz + 4).setType(Material.MOSS_CARPET, false);
    }

    private void buildDeepMarket(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 11, 8, 9, 6,
                Material.DEEPSLATE_TILES, Material.SMOOTH_BASALT,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.LANTERN);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -5; dz <= 6; dz++) {
                boolean edge = Math.abs(dx) == 7 || dz == -5 || dz == 6;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx + dz) % 5 == 0 ? Material.SMOOTH_BASALT : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && ((Math.abs(dx) == 7 && dz % 3 == 0) || (Math.abs(dz) == 5 && dx % 3 == 0))) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        int stall = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                stall++;
                int sx = bx - 5 + (col * 2);
                int sz = bz - 3 + (row * 3);
                buildMarketStall(world, sx, by, sz, stall);
            }
        }

        Block lectern = world.getBlockAt(bx + 5, by, bz + 5);
        lectern.setType(Material.LECTERN, false);
        if (lectern.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.WEST);
            lectern.setBlockData(d, false);
        }
        placeDecorativeBookshelf(world.getBlockAt(bx + 4, by, bz + 5), 31);
        placeDecorativeBookshelf(world.getBlockAt(bx + 6, by, bz + 5), 37);
        world.getBlockAt(bx + 5, by + 1, bz + 5).setType(Material.LANTERN, false);
        fillWrittenLecternBook(lectern, "market tallies", "the record", List.of(
                "eighteen stalls were counted before the warm road closed.",
                "bread was traded for salt, oil, mending, and a watched lamp.",
                "one lamp could be minded for a token while the owner ate."
        ));
        world.getBlockAt(bx, by, bz - 6).setType(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, false);
        world.getBlockAt(bx - 6, by, bz + 5).setType(Material.BARREL, false);
    }

    private void buildMarketStall(org.bukkit.World world, int x, int y, int z, int stall) {
        Material marker = switch (stall % 6) {
            case 1, 2, 3, 4 -> Material.HAY_BLOCK;
            case 5 -> Material.CAULDRON;
            default -> Material.ANVIL;
        };
        if (stall % 6 == 0) {
            placeDecorativeBookshelf(world.getBlockAt(x, y, z), stall + 41);
        } else {
            world.getBlockAt(x, y, z).setType(Material.BARREL, false);
        }
        world.getBlockAt(x + 1, y, z).setType(marker, false);
        world.getBlockAt(x, y + 1, z).setType(stall % 4 == 0 ? Material.CANDLE : Material.SOUL_LANTERN, false);
        if (stall == 4 || stall == 9 || stall == 14 || stall == 18) {
            world.getBlockAt(x - 1, y, z).setType(Material.DARK_OAK_FENCE, false);
        }
    }

    private void buildRationTable(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 8, 5, 6, 5,
                Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_BRICKS,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.DEEPSLATE_BRICKS, Material.LANTERN);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 3;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx + dz) % 3 == 0 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 4 || Math.abs(dz) == 3)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by, bz).setType(Material.DARK_OAK_SLAB, false);
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.DARK_OAK_PLANKS, false);
        }
        world.getBlockAt(bx - 2, by, bz - 1).setType(Material.BARREL, false);
        world.getBlockAt(bx - 1, by, bz - 1).setType(Material.CAKE, false);
        world.getBlockAt(bx, by, bz - 1).setType(Material.CAKE, false);
        world.getBlockAt(bx + 1, by, bz - 1).setType(Material.CANDLE, false);
        world.getBlockAt(bx + 2, by, bz - 1).setType(Material.AIR, false);

        world.getBlockAt(bx, by + 1, bz).setType(Material.LIGHT_GRAY_CARPET, false);
        world.getBlockAt(bx - 3, by, bz + 2).setType(Material.GRAY_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx + 3, by, bz + 2), BlockFace.WEST,
                "ration form r14", List.of(
                        "three heads. one and a half loaves.",
                        "the hand was filled correctly.",
                        "the crossed line is where the child stopped being a tally."
                ));
        world.getBlockAt(bx, by + 2, bz - 2).setType(Material.LANTERN, false);
    }

    private void buildThirdBayBreach(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 10, 5, 5, 6,
                Material.DEEPSLATE_TILES, Material.BLACK_CONCRETE,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.SOUL_LANTERN);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 3;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : (dz == 0 ? Material.BLACK_CONCRETE : Material.DEEPSLATE_TILES);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 6 || Math.abs(dz) == 3)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx = -6; dx <= 6; dx++) {
            Material line = Math.abs(dx) <= 1 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.BLACK_CONCRETE;
            world.getBlockAt(bx + dx, by, bz).setType(line, false);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.SCULK, false);
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.AIR, false);
                world.getBlockAt(bx + dx, by - 3, bz + dz).setType(Material.AIR, false);
            }
        }

        for (int dx : new int[]{-3, 3}) {
            for (int dy = 0; dy <= 3; dy++) {
                world.getBlockAt(bx + dx, by + dy, bz).setType(dy == 3 ? Material.BLACKSTONE : Material.DEEPSLATE_BRICKS, false);
            }
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.SCULK_SENSOR, false);
        world.getBlockAt(bx, by, bz + 2).setType(Material.IRON_BARS, false);
        Block coldLamp = world.getBlockAt(bx, by - 1, bz - 2);
        coldLamp.setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (coldLamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            coldLamp.setBlockData(light, false);
        }
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2), BlockFace.SOUTH,
                "third bay mark", List.of(
                        "mark 33: the line broke downward here.",
                        "this is not a road.",
                        "the lamp was set apart after the floor began answering."
                ));
        world.getBlockAt(bx + 5, by, bz + 2).setType(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, false);
    }

    private void buildWarmTownCollapse(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        buildProofChamber(world, bx, by, bz, 9, 5, 7, 6,
                Material.CRACKED_DEEPSLATE_BRICKS, Material.GRAVEL,
                Material.DEEPSLATE_BRICKS, Material.BLACKSTONE, Material.SOUL_LANTERN);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -2; dz <= 5; dz++) {
                boolean edge = Math.abs(dx) == 5 || dz == -2 || dz == 5;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx + dz) % 4 == 0 ? Material.GRAVEL : Material.CRACKED_DEEPSLATE_BRICKS);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (edge && (Math.abs(dx) == 5 || dz == 5)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx : new int[]{-5, 5}) {
            for (int dy = 0; dy <= 5; dy++) {
                Material pillar = dy >= 4 ? Material.BLACKSTONE : Material.DEEPSLATE_BRICKS;
                world.getBlockAt(bx + dx, by + dy, bz + 1).setType(pillar, false);
            }
        }

        for (int dx = -5; dx <= 5; dx++) {
            int height = 2 + Math.floorMod(dx * 3, 4);
            for (int dy = 0; dy <= height; dy++) {
                Material rubble = (dy == height && Math.abs(dx) % 2 == 0)
                        ? Material.GRAVEL
                        : (dy % 2 == 0 ? Material.COBBLED_DEEPSLATE : Material.DEEPSLATE);
                world.getBlockAt(bx + dx, by + dy, bz + 5).setType(rubble, false);
            }
        }

        for (int i = 0; i < 4; i++) {
            world.getBlockAt(bx - 1 + i, by, bz + 3).setType(i % 2 == 0 ? Material.GRAVEL : Material.COBBLED_DEEPSLATE, false);
            world.getBlockAt(bx + 2 - i, by + 1, bz + 4).setType(Material.COBWEB, false);
        }

        world.getBlockAt(bx - 3, by, bz).setType(Material.BARREL, false);
        world.getBlockAt(bx - 2, by, bz).setType(Material.BARREL, false);
        world.getBlockAt(bx - 3, by + 1, bz).setType(Material.DARK_OAK_SLAB, false);
        world.getBlockAt(bx - 1, by, bz + 1).setType(Material.DARK_OAK_FENCE, false);
        world.getBlockAt(bx - 2, by, bz + 1).setType(Material.HAY_BLOCK, false);

        Block deadLamp = world.getBlockAt(bx + 3, by + 1, bz + 1);
        deadLamp.setType(materialOr(Material.WEATHERED_CUT_COPPER, "COPPER_BULB", "OXIDIZED_COPPER_BULB"), false);
        if (deadLamp.getBlockData() instanceof org.bukkit.block.data.Lightable light) {
            light.setLit(false);
            deadLamp.setBlockData(light, false);
        }
        world.getBlockAt(bx + 3, by, bz + 1).setType(Material.IRON_BARS, false);
        world.getBlockAt(bx + 4, by, bz + 2).setType(Material.BLACK_CANDLE, false);
        placeEvidenceLectern(new Location(world, bx, by, bz + 2), BlockFace.SOUTH,
                "warden 3 closure", List.of(
                        "east market closed under WARDEN-3.",
                        "no bread remained warm.",
                        "all hands returned except the one sent for the lamp."
                ));
        world.getBlockAt(bx - 4, by, bz - 1).setType(Material.BLACK_CANDLE, false);
    }

    private void placePaintedLineFixture(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        for (int dx = -5; dx <= 5; dx++) {
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.DEEPSLATE_BRICKS, false);
            world.getBlockAt(bx + dx, by, bz).setType(Material.BLACK_CONCRETE, false);
            if (Math.abs(dx) == 5 || dx == 0) {
                supportToGround(world, bx + dx, by - 1, bz, Material.DEEPSLATE_BRICKS);
            }
        }
        for (int dz = -1; dz <= 1; dz += 2) {
            for (int dx = -5; dx <= 5; dx++) {
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.CRACKED_DEEPSLATE_BRICKS, false);
            }
        }
        placeEvidenceLectern(new Location(world, bx + 6, by, bz), BlockFace.WEST,
                "line count", List.of(
                        "the line is counted.",
                        "crossing it is a choice, not a road.",
                        "Aro's warm answer stops here."
                ));
    }

    private void placeRouteLanding(org.bukkit.World world, int cx, int y, int z, int halfWidth, int depth) {
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                boolean edge = Math.abs(dx) == halfWidth || dz == 0 || dz == depth - 1;
                Material floor = edge ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES;
                world.getBlockAt(cx + dx, y, z + dz).setType(floor, false);
                world.getBlockAt(cx + dx, y - 1, z + dz).setType(Material.DEEPSLATE_BRICKS, false);
                if (edge && (dx == -halfWidth || dx == halfWidth || dz == 0 || dz == depth - 1)) {
                    supportToGround(world, cx + dx, y - 1, z + dz, floor);
                }
            }
        }
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            world.getBlockAt(cx + dx, y + 1, z).setType(Material.IRON_BARS, false);
        }
    }

    private void placeRouteRib(org.bukkit.World world, int cx, int y, int z) {
        for (int dx : new int[]{-3, 3}) {
            world.getBlockAt(cx + dx, y + 1, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
            world.getBlockAt(cx + dx, y + 2, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
            world.getBlockAt(cx + dx, y + 3, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
        }
        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(cx + dx, y + 3, z).setType(Material.IRON_CHAIN, false);
        }
    }

    private void supportToGround(org.bukkit.World world, int x, int topY, int z, Material material) {
        if (world == null || material == null) return;
        int groundY = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR);
        int minY = Math.max(world.getMinHeight() + 1, topY - 14);
        int startY = Math.max(groundY + 1, minY);
        for (int y = startY; y < topY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() || y >= topY - 2) {
                block.setType(material, false);
            }
        }
    }

    private void placeDecorativeBookshelf(Block block, int seed) {
        if (block == null) return;
        try {
            block.setType(Material.CHISELED_BOOKSHELF, false);
            int[] occupied = new int[3];
            if (block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf) {
                int max = Math.max(1, shelf.getMaximumOccupiedSlots());
                int base = Math.floorMod(seed, max);
                occupied[0] = base;
                occupied[1] = Math.floorMod(base + 2, max);
                occupied[2] = Math.floorMod(base + 4, max);
                for (int slot = 0; slot < max; slot++) {
                    boolean used = slot == occupied[0] || slot == occupied[1] || slot == occupied[2];
                    shelf.setSlotOccupied(slot, used);
                }
                block.setBlockData(shelf, false);
            }
            if (block.getState() instanceof org.bukkit.block.ChiseledBookshelf cbs) {
                Inventory inv = cbs.getInventory();
                if (inv != null) {
                    inv.clear();
                    for (int slot : occupied) inv.setItem(slot, new ItemStack(Material.BOOK));
                }
                cbs.update(true, false);
            }
        } catch (Throwable ignored) { }
    }

    private void placeMechanicBookshelf(Block block) {
        if (block == null) return;
        try {
            block.setType(Material.CHISELED_BOOKSHELF, false);
            if (block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf) {
                int max = Math.max(1, shelf.getMaximumOccupiedSlots());
                for (int slot = 0; slot < max; slot++) {
                    shelf.setSlotOccupied(slot, false);
                }
                block.setBlockData(shelf, false);
            }
            if (block.getState() instanceof org.bukkit.block.ChiseledBookshelf cbs) {
                Inventory inv = cbs.getInventory();
                if (inv != null) inv.clear();
                cbs.update(true, false);
            }
        } catch (Throwable ignored) { }
    }

    private void buildProofChamber(org.bukkit.World world, int bx, int by, int bz,
                                   int halfX, int backZ, int frontZ, int height,
                                   Material floorA, Material floorB, Material wall, Material trim,
                                   Material accentLight) {
        if (world == null) return;
        int topY = by + Math.max(4, height);
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -backZ; dz <= frontZ; dz++) {
                boolean rim = Math.abs(dx) == halfX || dz == -backZ || dz == frontZ;
                boolean axis = Math.abs(dx) <= 2 || dz == 0;
                Material floor = rim ? trim : (axis ? floorA : (((dx + dz) & 1) == 0 ? floorA : floorB));
                world.getBlockAt(bx + dx, by - 2, bz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (rim) {
                    for (int dy = 0; dy <= height; dy++) {
                        Material mat = dy == height ? trim
                                : ((dy == 1 && Math.floorMod(dx + dz, 5) == 0) ? floorB : wall);
                        world.getBlockAt(bx + dx, by + dy, bz + dz).setType(mat, false);
                    }
                    if ((Math.abs(dx) == halfX && Math.floorMod(dz, 4) == 0)
                            || ((dz == -backZ || dz == frontZ) && Math.floorMod(dx, 4) == 0)) {
                        supportToGround(world, bx + dx, by - 1, bz + dz, trim);
                    }
                } else {
                    for (int dy = 0; dy < height; dy++) {
                        world.getBlockAt(bx + dx, by + dy, bz + dz).setType(Material.AIR, false);
                    }
                    world.getBlockAt(bx + dx, topY, bz + dz).setType(trim, false);
                }
            }
        }
        if (accentLight != null && accentLight != Material.AIR) {
            for (int dx : new int[]{-Math.max(3, halfX - 3), Math.max(3, halfX - 3)}) {
                for (int dz : new int[]{-Math.max(3, backZ - 2), Math.max(3, frontZ - 2)}) {
                    world.getBlockAt(bx + dx, topY - 1, bz + dz).setType(accentLight, false);
                }
            }
        }
    }

    private Material supportMaterial(int seed) {
        int choice = Math.floorMod(seed, 4);
        if (choice == 0) return Material.DEEPSLATE_BRICKS;
        if (choice == 1) return Material.CRACKED_DEEPSLATE_BRICKS;
        if (choice == 2) return Material.POLISHED_BLACKSTONE_BRICKS;
        return Material.POLISHED_DEEPSLATE;
    }

    private Material materialOr(Material fallback, String... names) {
        if (names != null) {
            for (String name : names) {
                try {
                    Material material = Material.matchMaterial(name);
                    if (material != null) return material;
                } catch (Throwable ignored) { }
            }
        }
        return fallback;
    }

    private void placeStandingSign(Location loc, BlockFace rotation, String[] lines) {
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();
        block.setType(Material.SPRUCE_SIGN, false);
        if (block.getBlockData() instanceof Rotatable r) {
            r.setRotation(rotation == null ? BlockFace.SOUTH : rotation);
            block.setBlockData(r, false);
        }
        setSignLines(block, true, lines);
    }

    private void placeEditableStandingSign(Location loc, BlockFace rotation) {
        placeEditableStandingSign(loc, rotation, new String[]{"", "", "", ""});
    }

    private void placeEditableStandingSign(Location loc, BlockFace rotation, String[] lines) {
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();
        block.setType(Material.SPRUCE_SIGN, false);
        if (block.getBlockData() instanceof Rotatable r) {
            r.setRotation(rotation == null ? BlockFace.SOUTH : rotation);
            block.setBlockData(r, false);
        }
        setSignLines(block, false, lines);
    }

    /**
     * {@code /observance director [world|lab] [spacing]} - one-command rehearsal startup. World mode
     * builds a compact rehearsal course; lab mode builds the full floating proof surface.
     */
    private void handleDirectorStart(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String raw = args[1].toLowerCase(Locale.ROOT).trim();
            if (raw.equals("state") || raw.equals("run") || raw.equals("leads") || raw.equals("open")) {
                handleDirectorState(sender);
                return;
            }
            if (raw.equals("progress") || raw.equals("players") || raw.equals("stuck") || raw.equals("hints")) {
                handleDirectorProgress(sender);
                return;
            }
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance director must be run by a player (needs a location).");
            return;
        }
        String mode = "world";
        String spacing = "18";
        if (args.length >= 2 && !args[1].isBlank()) {
            String raw = args[1].toLowerCase(Locale.ROOT).trim();
            if (raw.equals("lab") || raw.equals("full") || raw.equals("fullrun") || raw.equals("float")) {
                mode = "lab";
                if (args.length >= 3 && !args[2].isBlank()) spacing = args[2];
            } else if (raw.equals("world") || raw.equals("compact") || raw.equals("prep")) {
                mode = "world";
                if (args.length >= 3 && !args[2].isBlank()) spacing = args[2];
            } else {
                spacing = args[1];
            }
        }
        sender.sendMessage("== Observance director startup ==");
        sender.sendMessage("Mode: " + mode + ". This runs rehearsal placement, repair, coverage, and starts the guided rehearsal.");
        sender.sendMessage("Production setup uses /obs placehold build/audit first; prepworld/fullrun stay disposable.");

        if (mode.equals("lab")) {
            handleFullRun(sender, new String[]{"fullrun", spacing});
            sender.sendMessage("== Director placement add-on: dread route ==");
            handleDreadPass(sender, new String[]{"dreadpass", "stage"});
        } else {
            handlePrepWorld(sender, new String[]{"prepworld", spacing});
            sender.sendMessage("== Director placement add-on: puzzle mechanics grid ==");
            handlePuzzlePass(sender, new String[]{"puzzlepass", spacing});
            sender.sendMessage("== Director placement add-on: dread route ==");
            handleDreadPass(sender, new String[]{"dreadpass", "stage"});
        }
        sender.sendMessage("== Director check 1/4: first audit ==");
        handleAudit(sender);
        sender.sendMessage("== Director check 2/4: repair pass ==");
        handleRepair(sender);
        sender.sendMessage("== Director check 3/4: second audit ==");
        handleAudit(sender);
        sender.sendMessage("== Director check 4/4: coverage ==");
        handleCoverage(sender);
        handleRehearse(sender, new String[]{"rehearse", "start"});

        visitProgress.put(rehearsalKey(sender), -1);
        sender.sendMessage("Director startup ready for " + player.getName() + ".");
        sender.sendMessage("Next: /obs visit next, then /obs rehearse done after each tested stage.");
        sender.sendMessage("Scare pass when ready: /obs dreadpass run");
    }

    /**
     * {@code /observance director state} - console-safe run direction. It compresses the same live
     * flag logic as the author dashboard into Minecraft chat so setup operators can see what is open,
     * what is missing, and what to do next without hunting through docs.
     */
    private void handleDirectorState(CommandSender sender) {
        sender.sendMessage("== Observance director state ==");
        String missingLaunch = nextLaunchMissingId();
        int launchReady = launchPlacedCount();
        sender.sendMessage("Setup: launch sites " + launchReady + "/" + LAUNCH_REQUIRED_SITES.length
                + (missingLaunch == null ? " ready" : ", next missing " + missingLaunch
                + " (" + placementLaneId(missingLaunch) + ")"));

        int townsfolkTotal = com.observance.watcher.npc.TownsfolkNpc.TOWNSFOLK.size();
        int townsfolkSpawned = plugin.townsfolk() == null ? 0 : plugin.townsfolk().spawnedCount();
        boolean wrenSpawned = plugin.wren() != null && plugin.wren().isSpawned();
        boolean keeperSpawned = plugin.keeper() != null && plugin.keeper().isSpawned();
        sender.sendMessage("NPC proof: townsfolk " + townsfolkSpawned + "/" + townsfolkTotal
                + ", Wren " + yesNo(wrenSpawned) + ", Keeper " + yesNo(keeperSpawned));

        sender.sendMessage("Run risk: sleep " + (plugin.isLocallyAsleep() ? "ON" : "off")
                + ", drama " + yesNo(plugin.config() != null && plugin.config().dramaEnabled()));
        sendPackStatus(sender);

        var sb = plugin.supabase();
        if (sb == null || !sb.isConfigured()) {
            sender.sendMessage("Open player leads: live flags unavailable; start with Hold/Rosetta proof.");
            sender.sendMessage("Media gates: unknown until Supabase is configured.");
            sender.sendMessage("Finale readiness: unknown; run /obs preflight after setup.");
            sender.sendMessage("Next operator move: " + localDirectorNextMove(missingLaunch, townsfolkSpawned,
                    townsfolkTotal, wrenSpawned, keeperSpawned, -1, Collections.emptyMap()));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var r = sb.fetchArcState();
            Map<String, Object> flags = (r.ok() && r.value() != null)
                    ? r.value().flagsMap() : Collections.emptyMap();
            Bukkit.getScheduler().runTask(plugin, () -> sendDirectorFlagState(sender, flags, missingLaunch,
                    townsfolkSpawned, townsfolkTotal, wrenSpawned, keeperSpawned));
        });
    }

    private void sendDirectorFlagState(CommandSender sender, Map<String, Object> flags, String missingLaunch,
                                       int townsfolkSpawned, int townsfolkTotal,
                                       boolean wrenSpawned, boolean keeperSpawned) {
        int theories = directorCountFlags(flags, "vaun_theory", "mara_theory", "sella_theory",
                "orin_theory", "brann_theory", "iss_theory");
        int sideProofs = directorCountFlags(flags, "site_seen_deep_market", "site_seen_cistern_7",
                "site_seen_watch_floor", "site_seen_third_bay_breach", "site_seen_warm_town_collapse");
        int mediaReady = directorCountFlags(flags, "media_clip_01_ready", "media_clip_02_ready",
                "media_clip_03_ready", "media_clip_04_ready", "recovered_archive_ready");

        sender.sendMessage("Story state: keeper theories " + theories + "/6, side proofs " + sideProofs
                + "/5, seventh named " + yesNo(directorFlag(flags, "seventh_named")));
        sender.sendMessage("Open player leads:");
        for (String lead : directorOpenLeads(flags, theories)) {
            sender.sendMessage(" - " + lead);
        }
        sender.sendMessage("Media gates: " + mediaReady + "/5 ready"
                + (directorFlag(flags, "recovered_archive_ready") ? ", archive ready" : ", archive not ready"));
        sender.sendMessage("Finale readiness: accepting " + yesNo(directorFlag(flags, "accepting_onramp_open"))
                + ", tokens " + yesNo(directorFlag(flags, "tokens_laid"))
                + ", bowed as one " + yesNo(directorFlag(flags, "bowed_as_one")));
        sender.sendMessage("Next operator move: " + localDirectorNextMove(missingLaunch, townsfolkSpawned,
                townsfolkTotal, wrenSpawned, keeperSpawned, theories, flags));
    }

    private List<String> directorOpenLeads(Map<String, Object> flags, int theoryCount) {
        List<String> leads = new ArrayList<>();
        if (!directorFlag(flags, "rosetta_known")) {
            leads.add("Cold open/Rosetta literacy is still live.");
        }
        if (directorFlag(flags, "rosetta_known") && theoryCount < 6) {
            leads.add("Keeper investigations remain open; use evidence clusters, not stone-only solves.");
        }
        if (theoryCount >= 3 && !directorFlag(flags, "iss_caught")) {
            leads.add("Iss contradiction can surface once players compare enough keeper evidence.");
        }
        if (directorFlag(flags, "iss_caught") && !directorFlag(flags, "seventh_suspected")) {
            leads.add("Seventh suspicion should point back through Sella/far-water proof.");
        }
        if (directorFlag(flags, "seventh_suspected") && !directorFlag(flags, "seventh_named")) {
            leads.add("Seventh name is a comparison solve, not a fresh random cipher.");
        }
        if (directorFlag(flags, "seventh_named") && !directorFlag(flags, "accepting_onramp_open")) {
            leads.add("Threshold/Unlit route is next after the seventh lands.");
        }
        if (directorFlag(flags, "accepting_onramp_open") && !directorFlag(flags, "tokens_laid")) {
            leads.add("Accepting token work is open; keep it physical and group-owned.");
        }
        if (directorFlag(flags, "tokens_laid") && !directorFlag(flags, "bowed_as_one")) {
            leads.add("Finale gate is synchronized bow, not a typed phrase.");
        }
        if (directorFlag(flags, "bowed_as_one")) {
            leads.add("Release/debrief delivery is active.");
        }
        if (leads.isEmpty()) {
            leads.add("No clear live flag lead yet; prove Hold, first report, and Rosetta.");
        }
        return leads;
    }

    private String localDirectorNextMove(String missingLaunch, int townsfolkSpawned, int townsfolkTotal,
                                         boolean wrenSpawned, boolean keeperSpawned,
                                         int theoryCount, Map<String, Object> flags) {
        if (plugin.sites() == null || plugin.sites().get(HOLD_REGION_SITE_ID) == null) {
            return "/obs placehold build, then /obs placehold audit";
        }
        if (missingLaunch != null) {
            String lane = placementLaneId(missingLaunch);
            return lane.isBlank() ? "/obs site next" : "/obs site next " + lane;
        }
        if (townsfolkSpawned < townsfolkTotal) return "/obs townsfolk spawn";
        if (!wrenSpawned) return "/obs wren spawn";
        if (!keeperSpawned) return "/obs keeper spawn";
        if (plugin.isLocallyAsleep()) return "/obs sleep off";
        if (theoryCount >= 0 && theoryCount < 6) return "/obs site todo, then /obs visit next";
        if (directorFlag(flags, "accepting_onramp_open") && !directorFlag(flags, "bowed_as_one")) {
            return "/obs unlit ready, then /obs finale";
        }
        return "/obs preflight, then /obs rehearse start";
    }

    private int launchPlacedCount() {
        int ready = 0;
        for (String id : LAUNCH_REQUIRED_SITES) {
            if (launchSiteIssue(id) == null) ready++;
        }
        return ready;
    }

    private static int directorCountFlags(Map<String, Object> flags, String... keys) {
        int n = 0;
        for (String key : keys) {
            if (directorFlag(flags, key)) n++;
        }
        return n;
    }

    private static boolean directorFlag(Map<String, Object> flags, String key) {
        if (flags == null || key == null) return false;
        Object value = flags.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof String s) return s.equalsIgnoreCase("true") || s.equals("1");
        return false;
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    /**
     * {@code /observance director progress} - read-only live progress and stuck-player hint view.
     * It does not deliver hints; it tells the operator what the data says so they can decide whether
     * to observe, approve a beat, nudge with the Record terminal, or fix a broken traversal surface.
     */
    private void handleDirectorProgress(CommandSender sender) {
        var sb = plugin.supabase();
        if (sb == null || !sb.isConfigured()) {
            sender.sendMessage("Observance: Supabase is not configured; player progress and stuck hints are unavailable.");
            return;
        }
        sender.sendMessage("== Observance director progress ==");
        sender.sendMessage("Reading recent solves, attempts, open puzzles, and authored hints...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var arc = sb.fetchArcState();
            Map<String, Object> flags = (arc.ok() && arc.value() != null)
                    ? arc.value().flagsMap() : Collections.emptyMap();
            List<PuzzleRow> puzzles = sb.fetchOpenPuzzles(220).value();
            List<SolveReadRow> solves = sb.fetchRecentSolves(160).value();
            List<AnswerAttemptReadRow> attempts = sb.fetchRecentAnswerAttempts(180).value();
            List<HintRow> hints = sb.fetchHints(500).value();
            Bukkit.getScheduler().runTask(plugin, () -> sendDirectorProgress(sender,
                    flags,
                    puzzles == null ? Collections.emptyList() : puzzles,
                    solves == null ? Collections.emptyList() : solves,
                    attempts == null ? Collections.emptyList() : attempts,
                    hints == null ? Collections.emptyList() : hints));
        });
    }

    private void sendDirectorProgress(CommandSender sender, Map<String, Object> flags,
                                      List<PuzzleRow> puzzles, List<SolveReadRow> solves,
                                      List<AnswerAttemptReadRow> attempts, List<HintRow> hints) {
        Map<String, Integer> solveCountByUuid = new HashMap<>();
        Map<String, Integer> missCountByUuid = new HashMap<>();
        Set<String> solvedPuzzleKeys = new HashSet<>();

        for (SolveReadRow solve : solves) {
            if (solve == null) continue;
            if (solve.puzzleKey != null && !solve.puzzleKey.isBlank()) {
                solvedPuzzleKeys.add(solve.puzzleKey);
            }
            if (solve.mcUuid != null && !solve.mcUuid.isBlank()) {
                solveCountByUuid.put(solve.mcUuid, solveCountByUuid.getOrDefault(solve.mcUuid, 0) + 1);
            }
        }
        int recentMisses = 0;
        Map<String, Integer> missesByPuzzle = new HashMap<>();
        for (AnswerAttemptReadRow attempt : attempts) {
            if (attempt == null || Boolean.TRUE.equals(attempt.matched)) continue;
            recentMisses++;
            if (attempt.mcUuid != null && !attempt.mcUuid.isBlank()) {
                missCountByUuid.put(attempt.mcUuid, missCountByUuid.getOrDefault(attempt.mcUuid, 0) + 1);
            }
            if (attempt.puzzleKey != null && !attempt.puzzleKey.isBlank()) {
                missesByPuzzle.put(attempt.puzzleKey, missesByPuzzle.getOrDefault(attempt.puzzleKey, 0) + 1);
            }
        }

        List<PuzzleRow> openUnsolved = new ArrayList<>();
        for (PuzzleRow puzzle : puzzles) {
            if (puzzle == null || puzzle.puzzleKey == null || puzzle.puzzleKey.isBlank()) continue;
            if (!Boolean.TRUE.equals(puzzle.active)) continue;
            if (!FlagGate.satisfied(puzzle.requiresFlagsMap(), flags)) continue;
            if (!solvedPuzzleKeys.contains(puzzle.puzzleKey)) openUnsolved.add(puzzle);
        }
        openUnsolved.sort(Comparator
                .comparingInt((PuzzleRow p) -> p.movement == null ? 999 : p.movement)
                .thenComparing(p -> p.puzzleKey == null ? "" : p.puzzleKey));

        Map<String, List<HintRow>> hintsByPuzzle = new LinkedHashMap<>();
        for (HintRow hint : hints) {
            if (hint == null || hint.puzzleKey == null || hint.puzzleKey.isBlank()) continue;
            hintsByPuzzle.computeIfAbsent(hint.puzzleKey, k -> new ArrayList<>()).add(hint);
        }
        for (List<HintRow> rows : hintsByPuzzle.values()) {
            rows.sort(Comparator.comparingInt(h -> h.tier == null ? 99 : h.tier));
        }

        sender.sendMessage("Player progress summary:");
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            sender.sendMessage(" - no online players; showing database-wide recent activity below.");
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String uuid = player.getUniqueId().toString();
                sender.sendMessage(" - " + player.getName()
                        + ": recent solves " + solveCountByUuid.getOrDefault(uuid, 0)
                        + ", recent misses " + missCountByUuid.getOrDefault(uuid, 0));
            }
        }

        sender.sendMessage("Recent solves:");
        int shown = 0;
        for (SolveReadRow solve : solves) {
            if (solve == null || solve.puzzleKey == null) continue;
            sender.sendMessage(" - " + shortKey(solve.puzzleKey) + " by " + playerLabel(solve.mcUuid)
                    + " (" + compactTime(solve.solvedAt) + ")");
            if (++shown >= 6) break;
        }
        if (shown == 0) sender.sendMessage(" - none in the recent window.");

        sender.sendMessage("Stuck-player hint view:");
        sender.sendMessage(" - open unsolved gated puzzles: " + openUnsolved.size()
                + "; recent unmatched attempts: " + recentMisses);
        shown = 0;
        for (PuzzleRow puzzle : openUnsolved) {
            List<HintRow> rows = hintsByPuzzle.getOrDefault(puzzle.puzzleKey, Collections.emptyList());
            if (rows.isEmpty()) continue;
            HintRow next = rows.get(0);
            sender.sendMessage(" - " + shortKey(puzzle.puzzleKey)
                    + " [M" + (puzzle.movement == null ? "?" : puzzle.movement) + "]"
                    + " misses=" + missesByPuzzle.getOrDefault(puzzle.puzzleKey, 0)
                    + " next hint t" + (next.tier == null ? "?" : next.tier)
                    + ": " + compactText(next.body, 86));
            if (++shown >= 8) break;
        }
        if (shown == 0) {
            sender.sendMessage(" - no open unsolved puzzle with authored hints is visible under current flags.");
        }

        sender.sendMessage("Recent wrong inputs:");
        shown = 0;
        for (AnswerAttemptReadRow attempt : attempts) {
            if (attempt == null || Boolean.TRUE.equals(attempt.matched)) continue;
            String text = attempt.normalized == null || attempt.normalized.isBlank()
                    ? attempt.raw : attempt.normalized;
            sender.sendMessage(" - " + playerLabel(attempt.mcUuid) + " @ " + compactTime(attempt.at)
                    + ": " + compactText(text, 70));
            if (++shown >= 5) break;
        }
        if (shown == 0) sender.sendMessage(" - none in the recent window.");

        String nextMove;
        if (recentMisses >= 6 && !openUnsolved.isEmpty()) {
            nextMove = "watch the group solve path; if the surface is fair, let Record/hints breathe; if not, fix traversal.";
        } else if (solves.isEmpty()) {
            nextMove = "/obs director state, then verify Hold/Rosetta/first report are actually reachable.";
        } else {
            nextMove = "keep observing; use /obs director state for the next story lead.";
        }
        sender.sendMessage("Next operator move: " + nextMove);
    }

    private static String playerLabel(String mcUuid) {
        if (mcUuid == null || mcUuid.isBlank()) return "unknown";
        try {
            Player p = Bukkit.getPlayer(UUID.fromString(mcUuid));
            if (p != null) return p.getName();
        } catch (Throwable ignored) { }
        String s = mcUuid.trim();
        return s.length() <= 8 ? s : s.substring(0, 8);
    }

    private static String shortKey(String key) {
        if (key == null || key.isBlank()) return "unknown";
        return key.length() <= 34 ? key : key.substring(0, 31) + "...";
    }

    private static String compactTime(String iso) {
        if (iso == null || iso.isBlank()) return "time?";
        String s = iso.trim();
        if (s.length() >= 19 && s.charAt(10) == 'T') {
            return s.substring(0, 10) + " " + s.substring(11, 19);
        }
        return compactText(s, 24);
    }

    private static String compactText(String text, int max) {
        if (text == null || text.isBlank()) return "(blank)";
        String s = text.replace('\n', ' ').replace('\r', ' ').trim();
        while (s.contains("  ")) s = s.replace("  ", " ");
        int limit = Math.max(8, max);
        return s.length() <= limit ? s : s.substring(0, limit - 3) + "...";
    }

    private static final RehearsalStage[] REHEARSAL_STAGES = {
            new RehearsalStage("setup", "Build the rehearsal world", "setup",
                    new String[]{
                            "Production setup starts with the Deep Hold; compact boards are rehearsal only.",
                            "Use prepworld/fullrun only when you want a disposable test surface.",
                            "Confirm the tester has the Lens and kept needle.",
                            "Keep rehearsal surfaces separate from final production placement."
                    },
                    new String[]{"/obs placehold build", "/obs placehold audit", "/obs prepworld", "/obs fullrun", "/obs lens give <player>", "/obs needle <player>"}),
            new RehearsalStage("hardware", "Catch and repair boring blockers", "setup",
                    new String[]{
                            "Run audit before and after repair.",
                            "Lecterns must contain written books.",
                            "Answer signs, Vaun chest/bookshelf, and core anchors must exist."
                    },
                    new String[]{"/obs audit", "/obs repair", "/obs audit"}),
            new RehearsalStage("puzzles", "Prove puzzle mechanics", "puzzle",
                    new String[]{
                            "Stage the compact puzzle grid if you are not using the full lab.",
                            "Test one detector from each family: bow, offering, sign, chest, shelf, lectern, frame, pool, corridor, vault.",
                            "Use gates only for rehearsal shortcuts, not production."
                    },
                    new String[]{"/obs puzzlepass", "/obs puzzlepass gates", "/obs runbook puzzle"}),
            new RehearsalStage("spine", "Play the main story spine", "spine",
                    new String[]{
                            "Open the prologue report and check the first marker.",
                            "Touch/read each keeper site and test one answer surface.",
                            "Walk Mara lecterns, deep payoff sites, Wren, and finale markers."
                    },
                    new String[]{"/obs runbook spine", "/obs flag list", "/obs flag set companion_revealed"}),
            new RehearsalStage("side", "Prove side-story and lore surfaces", "side",
                    new String[]{
                            "Right-click all five townsfolk.",
                            "Stage and interact with Wren and a Keeper node.",
                            "If time allows, survey the Nether and End side lanes in their real dimensions."
                    },
                    new String[]{"/obs sidepass", "/obs runbook side", "/obs flag list"}),
            new RehearsalStage("scare", "Run the Watcher scare pass", "scare",
                    new String[]{
                            "Walk the staged dread route and fire the humanlike/danger sequence on a real tester.",
                            "Listen for close sounds and watch for darkness, ash, wrong sky, dimming, and figures.",
                            "Mute with sleep if the test space gets too noisy."
                    },
                    new String[]{"/obs dreadpass run", "/obs test stalker", "/obs test hunt", "/obs test elsewhere", "/obs sleep on", "/obs sleep off"}),
            new RehearsalStage("ops", "Check dashboard and final placement path", "ops",
                    new String[]{
                            "Dashboard should show mode, pending approvals, armed beats, and failed beats.",
                            "In-game state should show open leads, setup risk, media gates, and the next operator move.",
                            "In-game progress should show recent solves, wrong-answer pressure, and available stuck hints.",
                            "Use manual mode when approving beats by hand.",
                            "For production geography, survey anchors with site set before placeworld."
                    },
                    new String[]{"/obs director state", "/obs director progress", "/obs status", "/obs runbook ops", "/obs site set <siteId>", "/obs placeworld"})
    };

    /**
     * {@code /observance rehearse <start|status|done|next|back|reset|list|stage>} - per-operator,
     * in-memory guided progress for a live test pass. It intentionally does not auto-run destructive
     * placement commands; it tells the director what to do next and lets them confirm each stage.
     */
    private void handleRehearse(CommandSender sender, String[] args) {
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT).trim() : "status";
        if (op.isBlank()) op = "status";
        String key = rehearsalKey(sender);
        switch (op) {
            case "start", "reset" -> {
                rehearsalProgress.put(key, 0);
                sender.sendMessage("Observance rehearsal: started at stage 1/" + REHEARSAL_STAGES.length + ".");
                sendRehearsalStage(sender, 0);
            }
            case "status" -> sendRehearsalStage(sender, rehearsalProgress.getOrDefault(key, 0));
            case "done", "next" -> {
                int current = rehearsalProgress.getOrDefault(key, 0);
                int next = Math.min(REHEARSAL_STAGES.length - 1, current + 1);
                rehearsalProgress.put(key, next);
                if (current >= REHEARSAL_STAGES.length - 1) {
                    sender.sendMessage("Observance rehearsal: final stage already reached. Run /obs audit one more time before launch.");
                } else {
                    sender.sendMessage("Observance rehearsal: advanced to stage " + (next + 1) + "/" + REHEARSAL_STAGES.length + ".");
                }
                sendRehearsalStage(sender, next);
            }
            case "back" -> {
                int current = rehearsalProgress.getOrDefault(key, 0);
                int prev = Math.max(0, current - 1);
                rehearsalProgress.put(key, prev);
                sendRehearsalStage(sender, prev);
            }
            case "list" -> {
                sender.sendMessage("== Observance rehearsal stages ==");
                for (int i = 0; i < REHEARSAL_STAGES.length; i++) {
                    RehearsalStage stage = REHEARSAL_STAGES[i];
                    sender.sendMessage(" " + (i + 1) + ") " + stage.id + " - " + stage.title);
                }
                sender.sendMessage("Jump: /obs rehearse <stageId>. Advance: /obs rehearse done.");
            }
            default -> {
                int idx = rehearsalStageIndex(op.equals("puzzle") ? "puzzles" : op);
                if (idx >= 0) {
                    rehearsalProgress.put(key, idx);
                    sendRehearsalStage(sender, idx);
                } else {
                    sender.sendMessage("Usage: /obs rehearse <start|status|done|next|back|reset|list|setup|hardware|puzzles|spine|side|scare|ops>");
                }
            }
        }
    }

    private String rehearsalKey(CommandSender sender) {
        if (sender instanceof Player player) {
            UUID id = player.getUniqueId();
            return id == null ? "player:" + player.getName().toLowerCase(Locale.ROOT) : "player:" + id;
        }
        return "sender:" + sender.getName().toLowerCase(Locale.ROOT);
    }

    private int rehearsalStageIndex(String id) {
        for (int i = 0; i < REHEARSAL_STAGES.length; i++) {
            if (REHEARSAL_STAGES[i].id.equals(id)) return i;
        }
        return -1;
    }

    private void sendRehearsalStage(CommandSender sender, int rawIndex) {
        int index = Math.max(0, Math.min(REHEARSAL_STAGES.length - 1, rawIndex));
        RehearsalStage stage = REHEARSAL_STAGES[index];
        sender.sendMessage("== Rehearsal " + (index + 1) + "/" + REHEARSAL_STAGES.length + ": " + stage.title + " ==");
        sender.sendMessage(" Runbook: /obs runbook " + stage.runbookPage);
        sender.sendMessage(" Checks:");
        for (String check : stage.checks) sender.sendMessage("  - " + check);
        sender.sendMessage(" Commands:");
        for (String command : stage.commands) sender.sendMessage("  " + command);
        sender.sendMessage(index >= REHEARSAL_STAGES.length - 1
                ? " Finish: /obs audit, then inspect dashboard failed beats."
                : " Advance when satisfied: /obs rehearse done");
    }

    private record RehearsalStage(String id, String title, String runbookPage,
                                  String[] checks, String[] commands) { }

    private static final CoverageLane[] COVERAGE_LANES = {
            new CoverageLane("prologue", "Prologue and first marker", "setup", false,
                    new String[]{"first_report_lectern_01", "first_marker_01"},
                    "Open first report, confirm marker exists."),
            new CoverageLane("surface", "Rosetta and keeper evidence sites", "spine", false,
                    new String[]{"rune_rosetta", "stone_vaun", "stone_mara", "stone_sella",
                            "stone_orin", "stone_brann", "stone_iss"},
                    "Read/touch each keeper site; submit at one blank answer surface."),
            new CoverageLane("dialogue_route", "Lamp-works/Stair dialogue proof", "side", false,
                    new String[]{"lampworks_stair", "third_lamp_stand", "painted_line", "dead_stall", "deep_bird_coops"},
                    "Run /obs descentproof; cross painted_line and confirm painted_line_crossed; inspect the empty bird coops."),
            new CoverageLane("side_destinations", "Side destination proof", "side", false,
                    new String[]{"school_stand", "the_far_water", "markers_row", "cistern_7", "watch_floor",
                            "set_apart_shelf", "undercroft_seal", "forgotten_mouth",
                            "deep_market", "ration_table", "third_bay_breach", "warm_town_collapse"},
                    "Inspect school, far water, marker row, Cistern 7, watch-floor, set-apart shelf, seal, way-up, market/ration/third bay/warm collapse."),
            new CoverageLane("mara", "Mara page-lock lecterns", "spine", false,
                    new String[]{"mara_lectern_1", "mara_lectern_2", "mara_lectern_3",
                            "mara_lectern_4", "mara_lectern_5"},
                    "Open all five lecterns; every lectern must hold a written book."),
            new CoverageLane("mechanics", "Puzzle mechanic fixtures", "puzzle", false,
                    puzzlePassSiteIds(),
                    "Run /obs puzzlepass, then test one detector from each family."),
            new CoverageLane("deep", "Deep payoff and finale spine", "spine", false,
                    new String[]{"stone_of_reckoning", "the_cold_hearth", "unbroken_light",
                            "the_threshold", "the_unwriting", "threshold_vault"},
                    "Walk reckoning, cold hearth, Accepting floor, threshold, unwriting, vault."),
            new CoverageLane("dimensions", "Nether and End side lanes", "side", true,
                    new String[]{"nether_forge", "end_seventh_shrine"},
                    "Optional for first pass; survey in the real Nether/End before placeworld."),
    };

    /**
     * {@code /observance coverage} - readiness by launch lane, not by raw site list. This is the
     * director-facing answer to "what parts of the ARG are actually testable right now?"
     */
    private void handleCoverage(CommandSender sender) {
        sender.sendMessage("== Observance rehearsal coverage ==");
        int requiredReady = 0;
        int requiredTotal = 0;
        for (CoverageLane lane : COVERAGE_LANES) {
            CoverageState state = coverageState(lane);
            if (!lane.optional) {
                requiredTotal++;
                if (state.ready) requiredReady++;
            }
            String prefix = state.ready ? "[OK]" : lane.optional ? "[OPT]" : "[MISS]";
            sender.sendMessage(prefix + " " + lane.title + " - " + state.ok + "/" + state.total + " ready");
            if (state.firstIssue != null) sender.sendMessage("  first issue: " + state.firstIssue);
            sender.sendMessage("  test: " + lane.testInstruction);
            sender.sendMessage("  help: /obs runbook " + lane.runbookPage);
        }

        int townsfolkTotal = com.observance.watcher.npc.TownsfolkNpc.TOWNSFOLK.size();
        int townsfolkSpawned = plugin.townsfolk() == null ? 0 : plugin.townsfolk().spawnedCount();
        boolean wrenSpawned = plugin.wren() != null && plugin.wren().isSpawned();
        boolean keeperSpawned = plugin.keeper() != null && plugin.keeper().isSpawned();
        boolean npcReady = townsfolkSpawned >= townsfolkTotal && wrenSpawned && keeperSpawned;
        if (npcReady) requiredReady++;
        requiredTotal++;
        sender.sendMessage((npcReady ? "[OK] " : "[MISS] ") + "NPC side/lore surfaces - "
                + "townsfolk=" + townsfolkSpawned + "/" + townsfolkTotal
                + ", wren=" + wrenSpawned
                + ", keeper=" + keeperSpawned);
        sender.sendMessage("  test: /obs sidepass, then right-click each NPC body");
        sender.sendMessage("  help: /obs runbook side");

        CoverageState dreadRoute = coverageState(new CoverageLane("dread", "Dread scare route", "scare", false,
                dreadPassSiteIds(), "Run /obs dreadpass stage, then /obs dreadpass run when ready."));
        boolean dramaReady = plugin.config() != null && plugin.config().dramaEnabled();
        boolean scareReady = dramaReady && dreadRoute.ready;
        if (scareReady) requiredReady++;
        requiredTotal++;
        sender.sendMessage((scareReady ? "[OK] " : "[MISS] ") + "Watcher scare lane - drama enabled="
                + dramaReady + ", dread route=" + dreadRoute.ok + "/" + dreadRoute.total);
        if (dreadRoute.firstIssue != null) sender.sendMessage("  first issue: " + dreadRoute.firstIssue);
        sender.sendMessage("  test: /obs dreadpass run, or focused checks: stalker, hunt, elsewhere");
        sender.sendMessage("  help: /obs runbook scare");

        CoverageState unlit = coverageState(new CoverageLane("unlit", "Unlit expansion village", "unlit", true,
                unlitRequiredSites(), "Place/paste observance_unlit, run /obs unlit clue <house>, then /obs unlit audit."));
        sender.sendMessage((unlit.ready ? "[OK] " : "[EXP] ") + "Unlit expansion lane - "
                + unlit.ok + "/" + unlit.total + " ready");
        if (unlit.firstIssue != null) sender.sendMessage("  first issue: " + unlit.firstIssue);
        sender.sendMessage("  test: /obs unlit pass light|stalker|extinguish|house|extract");
        sender.sendMessage("  help: /obs runbook unlit");

        sender.sendMessage("Required launch lanes ready: " + requiredReady + "/" + requiredTotal + ".");
        if (requiredReady == requiredTotal) {
            sender.sendMessage("Next: /obs rehearse start, then advance with /obs rehearse done.");
        } else {
            sender.sendMessage("Next: /obs placehold build for production Hold placement, or /obs prepworld/fullrun for rehearsal; then /obs audit -> /obs repair -> /obs coverage.");
        }
    }

    private CoverageState coverageState(CoverageLane lane) {
        int total = lane.siteIds.length;
        int ok = 0;
        String firstIssue = null;
        for (String siteId : lane.siteIds) {
            String issue = coverageIssue(siteId);
            if (issue == null) {
                ok++;
            } else if (firstIssue == null) {
                firstIssue = issue;
            }
        }
        return new CoverageState(ok == total, ok, total, firstIssue);
    }

    private String coverageIssue(String siteId) {
        if (plugin.sites() == null) return siteId + ": sites config unavailable.";
        Site site = plugin.sites().get(siteId);
        if (site == null) return siteId + ": missing from sites.yml.";
        if (!site.enabled()) return siteId + ": disabled.";
        if (!site.isPlaced()) return siteId + ": unplaced.";
        Location loc = site.location();
        if (loc == null || loc.getWorld() == null) return siteId + ": world not loaded.";
        String hardware = auditPlacedSite(site, loc);
        return hardware == null ? null : hardware;
    }

    private record CoverageLane(String id, String title, String runbookPage, boolean optional,
                                String[] siteIds, String testInstruction) { }

    private record CoverageState(boolean ready, int ok, int total, String firstIssue) { }

    private static String[] puzzlePassSiteIds() {
        String[] ids = new String[PUZZLE_PASS_SITES.length];
        for (int i = 0; i < PUZZLE_PASS_SITES.length; i++) ids[i] = PUZZLE_PASS_SITES[i][0];
        return ids;
    }

    private static String[] dreadPassSiteIds() {
        String[] ids = new String[DREAD_PASS_SITES.length];
        for (int i = 0; i < DREAD_PASS_SITES.length; i++) ids[i] = DREAD_PASS_SITES[i][0];
        return ids;
    }

    private static final String[] VISIT_ROUTE = {
            "first_report_lectern_01", "first_marker_01",
            "rune_rosetta", "stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss",
            "school_stand", "the_far_water", "markers_row", "cistern_7", "watch_floor", "set_apart_shelf", "undercroft_seal", "forgotten_mouth", "lampworks_stair", "third_lamp_stand", "painted_line", "dead_stall", "deep_bird_coops", "deep_market", "ration_table", "third_bay_breach", "warm_town_collapse",
            "mara_lectern_1", "mara_lectern_2", "mara_lectern_3", "mara_lectern_4", "mara_lectern_5",
            "bow_marker_01", "offering_cairn_01", "answer_sign_01",
            "vaun_hoard_chest", "vaun_bookshelf", "mara_map_marker",
            "sella_pool", "sella_anchor",
            "orin_marker_1", "orin_marker_2", "orin_marker_3", "orin_marker_4", "orin_marker_5", "orin_marker_6",
            "orin_frame_dial_1", "orin_frame_dial_2", "orin_frame_dial_3",
            "orin_frame_dial_4", "orin_frame_dial_5", "orin_frame_dial_6",
            "brann_toll_tower", "brann_corridor_start", "brann_corridor_end", "coop_plate",
            "stone_of_reckoning", "the_cold_hearth", "unbroken_light", "the_threshold", "the_unwriting", "threshold_vault",
            "dread_route_start", "dread_route_elsewhere", "dread_route_figure", "dread_route_exit",
            "nether_forge", "end_seventh_shrine"
    };

    /**
     * {@code /observance visit <next|back|list|siteId|lane>} - admin rehearsal teleport between placed
     * story sites. This cuts down the operator burden when verifying large-world placement.
     */
    private void handleVisit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /obs visit must be run by a player (needs teleport target).");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT).trim() : "next";
        if (op.isBlank()) op = "next";

        if (op.equals("list")) {
            sender.sendMessage("== Observance visit route ==");
            for (int i = 0; i < VISIT_ROUTE.length; i++) {
                sender.sendMessage(" " + (i + 1) + ") " + VISIT_ROUTE[i] + visitSuffix(VISIT_ROUTE[i]));
            }
            sender.sendMessage("Lanes: prologue | surface | mara | puzzle | deep | scare | dimensions. Step: /obs visit next");
            return;
        }

        String key = rehearsalKey(sender);
        int targetIndex;
        if (op.equals("next")) {
            targetIndex = nextPlacedVisitIndex(visitProgress.getOrDefault(key, -1), 1);
        } else if (op.equals("back") || op.equals("prev") || op.equals("previous")) {
            targetIndex = nextPlacedVisitIndex(visitProgress.getOrDefault(key, 0), -1);
        } else {
            targetIndex = visitIndexFor(op);
            if (targetIndex < 0) {
                sender.sendMessage("Usage: /obs visit <next|back|list|siteId|prologue|surface|mara|puzzle|deep|scare|dimensions>");
                return;
            }
        }

        if (targetIndex < 0 || targetIndex >= VISIT_ROUTE.length) {
            sender.sendMessage("Observance: no placed visit targets found. Production starts with /obs placehold build/audit; rehearsal uses /obs prepworld or /obs fullrun.");
            return;
        }
        String siteId = VISIT_ROUTE[targetIndex];
        if (!teleportToSite(player, siteId)) {
            sender.sendMessage("Observance: '" + siteId + "' is not placed or its world is not loaded.");
            sender.sendMessage("Next: /obs placehold audit for clustered Hold sites, or /obs site set " + siteId + " for bespoke/outside-Hold anchors.");
            return;
        }
        visitProgress.put(key, targetIndex);
        sender.sendMessage("Observance visit " + (targetIndex + 1) + "/" + VISIT_ROUTE.length + ": " + siteId);
        sender.sendMessage("  Continue: /obs visit next   Back: /obs visit back   Context: /obs coverage");
    }

    private int nextPlacedVisitIndex(int current, int dir) {
        if (plugin.sites() == null || VISIT_ROUTE.length == 0) return -1;
        int step = dir < 0 ? -1 : 1;
        int start = Math.max(-1, Math.min(VISIT_ROUTE.length, current));
        for (int i = start + step; i >= 0 && i < VISIT_ROUTE.length; i += step) {
            Site site = plugin.sites().get(VISIT_ROUTE[i]);
            if (site != null && site.enabled() && site.isPlaced() && site.location() != null) return i;
        }
        return -1;
    }

    private int visitIndexFor(String raw) {
        String id = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
        String first = switch (id) {
            case "prologue", "start", "setup" -> "first_report_lectern_01";
            case "surface", "keepers", "spine" -> "rune_rosetta";
            case "mara", "lecterns" -> "mara_lectern_1";
            case "puzzle", "puzzles", "mechanic", "mechanics" -> "bow_marker_01";
            case "deep", "payoff", "finale" -> "stone_of_reckoning";
            case "scare", "dread", "watcher" -> "dread_route_start";
            case "dimensions", "dimension", "nether", "end" -> "nether_forge";
            default -> id;
        };
        for (int i = 0; i < VISIT_ROUTE.length; i++) {
            if (VISIT_ROUTE[i].equals(first)) return i;
        }
        return -1;
    }

    private boolean teleportToSite(Player player, String siteId) {
        if (plugin.sites() == null) return false;
        Site site = plugin.sites().get(siteId);
        if (site == null || !site.enabled()) return false;
        Location loc = site.location();
        if (loc == null || loc.getWorld() == null) return false;
        Location dest = loc.clone().add(0.5, 1.2, 0.5);
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        return player.teleport(dest);
    }

    private String visitSuffix(String siteId) {
        if (plugin.sites() == null) return " [no sites]";
        Site site = plugin.sites().get(siteId);
        if (site == null) return " [missing]";
        if (!site.enabled()) return " [disabled]";
        if (!site.isPlaced()) return " [unplaced]";
        return site.location() == null ? " [world unloaded]" : " [placed]";
    }

    private List<String> visitSuggestions(String prefix) {
        String want = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : new String[]{"next", "back", "list", "prologue", "surface", "mara", "puzzle", "mechanics", "deep", "scare", "dread", "dimensions"}) {
            if (s.startsWith(want)) out.add(s);
        }
        for (String siteId : VISIT_ROUTE) {
            if (siteId.startsWith(want) && !out.contains(siteId)) out.add(siteId);
        }
        return out;
    }

    /**
     * {@code /observance runbook [page]} - the in-world director cheat sheet. It keeps the launch
     * rehearsal order inside the jar so a tester does not have to alt-tab through design docs during a run.
     */
    private void handleRunbook(CommandSender sender, String[] args) {
        String page = args.length > 1 ? args[1].toLowerCase(Locale.ROOT).trim() : "all";
        if (page.isBlank()) page = "all";
        sender.sendMessage("== Observance director runbook ==");
        switch (page) {
            case "setup" -> sendSetupRunbook(sender);
            case "spine" -> sendSpineRunbook(sender);
            case "puzzle", "puzzles", "mechanics" -> sendPuzzleRunbook(sender);
            case "side", "lore" -> sendSideRunbook(sender);
            case "scare", "watcher" -> sendScareRunbook(sender);
            case "unlit", "village" -> sendUnlitRunbook(sender);
            case "ops", "dashboard" -> sendOpsRunbook(sender);
            case "all" -> {
                sendSetupRunbook(sender);
                sendSpineRunbook(sender);
                sendPuzzleRunbook(sender);
                sendSideRunbook(sender);
                sendScareRunbook(sender);
                sendUnlitRunbook(sender);
                sendOpsRunbook(sender);
                sender.sendMessage("Pages: /obs runbook setup | spine | puzzle | side | scare | unlit | ops");
            }
            default -> {
                sender.sendMessage("Usage: /obs runbook [setup|spine|puzzle|side|scare|unlit|ops]");
                sender.sendMessage("Tip: run /obs runbook spine during the playable pass.");
            }
        }
    }

    private void sendSetupRunbook(CommandSender sender) {
        sender.sendMessage("[setup]");
        sender.sendMessage("  1) Production Hold: stand at the surface mouth, /obs placehold build, then /obs placehold audit.");
        sender.sendMessage("  2) Rehearsal lab: /obs director lab or /obs prepworld only in a disposable test area.");
        sender.sendMessage("  3) Proof packets: run tools\\new_launch_placement_packet.ps1 and tools\\new_rehearsal_packet.ps1 before surveying.");
        sender.sendMessage("  4) Lane plan: /obs site plan lanes, then place outside-Hold prologue/surface/Nether/End/Unlit anchors.");
        sender.sendMessage("  5) Bespoke placement: read one lane brief, stand at one anchor, /obs site set <siteId>, then /obs placeworld.");
        sender.sendMessage("  6) Underground rule: placehold owns clustered Hold pockets; use judgment on entrances, sightlines, and surface context.");
        sender.sendMessage("  7) Proof loop after each lane: fill coords-capture proof shots, /obs preflight, fix REPLACE issues, then /obs rehearse done.");
        sender.sendMessage("  8) Focused tests: /obs puzzlepass for mechanics; /obs sidepass for NPC/lore; /obs dreadpass run for scares; /obs unlit ready for the mirror village.");
        sender.sendMessage("  9) Keep Watcher manual/muted with /obs sleep on while setting up; rearm with /obs sleep off.");
    }

    private void sendSpineRunbook(CommandSender sender) {
        sender.sendMessage("[main spine]");
        sender.sendMessage("  Prologue: open/read first lectern; confirm the first marker exists.");
        sender.sendMessage("  Rosetta/keepers: inspect books, lecterns, objects, and answer surfaces; edit blank answer signs only to submit tests.");
        sender.sendMessage("  Vaun: use hoard chest and chiseled bookshelf fixtures.");
        sender.sendMessage("  Mara: open all five page-lock lecterns; books must be written, not blank.");
        sender.sendMessage("  Sella: test reflection/pool fixture and Lens visibility.");
        sender.sendMessage("  Orin: test bow marker/frame-dial style fixtures.");
        sender.sendMessage("  Brann/Iss: inspect kept-light/cold-hearth sites and deep markers.");
        sender.sendMessage("  Wren: /obs wren spawn, right-click him, then /obs flag set companion_revealed for reckoning tests.");
        sender.sendMessage("  Finale: touch reckoning/finale markers once; use /obs flag list to confirm branches.");
    }

    private void sendPuzzleRunbook(CommandSender sender) {
        sender.sendMessage("[puzzle mechanics]");
        sender.sendMessage("  Stage: /obs puzzlepass. Shortcut gates, only in rehearsal: /obs puzzlepass gates.");
        sender.sendMessage("  Early customs: crouch at bow_marker_01; drop deepslate/cobbled deepslate at offering_cairn_01; type a known answer on answer_sign_01.");
        sender.sendMessage("  Vaun: put deepslate + cobbled deepslate in vaun_hoard_chest and close it; fill all six vaun_bookshelf slots.");
        sender.sendMessage("  Mara: turn lecterns 1-5 to pages 1, 2, 4, 4, 6; then bow together at mara_map_marker.");
        sender.sendMessage("  Sella: stand at sella_pool and look down into water; after sella_overlay_read, gaze from sella_anchor.");
        sender.sendMessage("  Orin/Brann: bow orin_marker_1..6 in order; set frames to rotation 0; sneak from brann_corridor_start to brann_corridor_end.");
        sender.sendMessage("  Vault: after deep_gate_open, stand at threshold_vault and enter v8k3 mq2n x6w1 t4d9 c7s5 on the vault sign.");
    }

    private void sendSideRunbook(CommandSender sender) {
        sender.sendMessage("[side/lore lanes]");
        sender.sendMessage("  Fast pass: /obs sidepass, then right-click aro, wenna, coll, dob, old-pell, Wren, and the Keeper.");
        sender.sendMessage("  Dialogue proof: /obs descentproof, then verify Aro/Coll/Wenna lines point to real places.");
        sender.sendMessage("  School proof: inspect school_stand; the slate, copy-line, and grey seventh must read without narration.");
        sender.sendMessage("  Far-water proof: inspect the_far_water; Sella's mirror/count evidence must read from water, stones, and the seventh marker.");
        sender.sendMessage("  Count/light/watch proof: inspect markers_row, cistern_7, and watch_floor; each must teach a custom through a place, not a paragraph.");
        sender.sendMessage("  Entry/seal/way-up proof: inspect set_apart_shelf, undercroft_seal, and forgotten_mouth; the line, low read, and return mark must be physically legible.");
        sender.sendMessage("  Bird proof: inspect deep_bird_coops; Aro's old-bird/coops rumor must resolve to visible empty cages.");
        sender.sendMessage("  Market proof: inspect deep_market before warm_town_collapse; the market must feel inhabited, not like a signpost.");
        sender.sendMessage("  Ration proof: inspect ration_table; the half-loaf and crossed child line must be readable without explanation.");
        sender.sendMessage("  Third-bay proof: inspect third_bay_breach; the Deep Line must be visibly broken downward, not just named.");
        sender.sendMessage("  Warm-town proof: inspect generated deep_market + warm_town_collapse after /obs placehold audit; confirm the lie has a visible collapse.");
        sender.sendMessage("  Quest proof: drop bread at dead_stall; place/touch light at third_lamp_stand; return for done lines.");
        sender.sendMessage("  Focused town test: /obs townsfolk spawn, then right-click each townsperson.");
        sender.sendMessage("  Focused keeper test: /obs keeper spawn <node>, right-click, then despawn when done.");
        sender.sendMessage("  Nether lane: stand in Nether, /obs site set nether_forge, then /obs placeworld.");
        sender.sendMessage("  End lane: stand in End, /obs site set end_seventh_shrine, then /obs placeworld.");
        sender.sendMessage("  Jump gates safely with /obs flag set <key>; inspect with /obs flag list.");
        sender.sendMessage("  After any placement lane: /obs audit, /obs repair, /obs audit.");
    }

    private void sendScareRunbook(CommandSender sender) {
        sender.sendMessage("[watcher scare pass]");
        sender.sendMessage("  Full rehearsal: /obs dreadpass run.");
        sender.sendMessage("  Stage only: /obs dreadpass stage. Then walk /obs visit scare.");
        sender.sendMessage("  Focus presets: /obs test stalker, /obs test hunt, /obs test elsewhere.");
        sender.sendMessage("  Focus checks: /obs test darkness, /obs test sound, /obs test mob, /obs test particles.");
        sender.sendMessage("  Live scares should avoid full-screen commands; use sound, ash, darkness, actionbar whispers, world marks, and rare humanoid figures.");
        sender.sendMessage("  If scare testing gets noisy: /obs sleep on. When ready again: /obs sleep off.");
    }

    private void sendUnlitRunbook(CommandSender sender) {
        sender.sendMessage("[unlit expansion]");
        sender.sendMessage("  1) Build/paste the dark village in observance_unlit.");
        sender.sendMessage("  2) Survey entry/spawn/exit: /obs unlit site entry, /obs unlit site spawn, /obs unlit site exit.");
        sender.sendMessage("  3) Stand in each chosen house and run /obs unlit clue lamp|cairn|coop|well|watch|warm|threshold|base.");
        sender.sendMessage("  4) For edits: /obs unlit buildmode on. Before tests: /obs unlit buildmode off.");
        sender.sendMessage("  5) Remove inherited village light: /obs unlit darken all [radius].");
        sender.sendMessage("  6) Fence it: /obs unlit border [radius]. Then verify: /obs unlit audit.");
        sender.sendMessage("  7) Rehearse pieces: /obs unlit pass light, stalker, extinguish, house, extract.");
        sender.sendMessage("  8) Handoff check: /obs unlit ready, then tools\\check_unlit_playtest_ready.ps1 -PacketDir rehearsals\\<date>.");
        sender.sendMessage("  Fixture rule: required houses read through ledgers/books/objects. Plain signs are fallback markers, not the house language.");
        sender.sendMessage("  Rule: houses are non-linear. Do not write clue text that assumes expedition numbers.");
    }

    private void sendOpsRunbook(CommandSender sender) {
        sender.sendMessage("[ops/dashboard]");
        sender.sendMessage("  Live direction: /obs director state summarizes open leads, setup risk, media gates, finale readiness, and next operator move.");
        sender.sendMessage("  Player pressure: /obs director progress summarizes recent solves, wrong inputs, open unsolved puzzles, and authored stuck hints.");
        sender.sendMessage("  Dashboard setup flow shows the intended order: rehearsal lab, proof packets, real placement, launch proof.");
        sender.sendMessage("  Dashboard shows mode, pending approvals, armed beats, failed beats, Unlit evidence, and keeper theories.");
        sender.sendMessage("  Pending approvals live in the beat queue; failed beats mean inspect dashboard/console.");
        sender.sendMessage("  Vercel dashboard does not fix blocks. Empty lecterns are fixed by jar + /obs repair.");
        sender.sendMessage("  Production placement: read /obs site plan next, survey with /obs site set <siteId>, then /obs placeworld.");
    }

    private void placeDirectPrologue(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int lx = origin.getBlockX();
        int lz = origin.getBlockZ();
        int ly = world.getHighestBlockYAt(lx, lz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location lecternLoc = new Location(world, lx, ly, lz);
        fillPrologueLecternBook(lecternLoc);
        plugin.registerRuntimeSite(new Site("first_report_lectern_01", "report_lectern", worldName,
                lecternLoc.getX(), lecternLoc.getY(), lecternLoc.getZ(), 4, 4, true, true, null));

        int mx = lx + 2;
        int mz = lz;
        int my = world.getHighestBlockYAt(mx, mz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        Location markerLoc = new Location(world, mx, my, mz);
        placeMarker(markerLoc, Material.CHISELED_STONE_BRICKS, Material.CANDLE, true);
        plugin.registerRuntimeSite(new Site("first_marker_01", "structure", worldName,
                markerLoc.getX(), markerLoc.getY(), markerLoc.getZ(), 6, 4, true, true, null));
    }

    private int placeCompactSpine(Location origin, String[][] rows, int spacing) {
        if (origin == null || origin.getWorld() == null || rows == null) return 0;
        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int placed = 0;
        for (int i = 0; i < rows.length; i++) {
            String siteId = rows[i][0];
            String siteType = rows[i][1];
            int radius;
            try { radius = Integer.parseInt(rows[i][2]); } catch (NumberFormatException e) { radius = 8; }

            int sx = origin.getBlockX() + (i * spacing);
            int sz = origin.getBlockZ();
            int sy = world.getHighestBlockYAt(sx, sz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
            Location siteLoc = new Location(world, sx, sy, sz);

            prepareCompactCell(siteLoc, Math.max(12, radius + 4), 9);
            Location answer = StructureTemplates.keeper(siteId, siteLoc);
            if (answer == null) continue;
            ensureAuditAnchor(siteId, siteLoc);

            Site cfg = plugin.sites() == null ? null : plugin.sites().get(siteId);
            String puzzleKey = cfg == null ? null : cfg.puzzleKey();
            Site site = new Site(siteId, siteType, worldName,
                    siteLoc.getX(), siteLoc.getY(), siteLoc.getZ(),
                    radius, 6, true, true, puzzleKey, false);
            plugin.registerRuntimeSite(site);
            placed++;
        }
        return placed;
    }

    private Location compactGridCell(Location origin, int index, int columns, int spacing, int row) {
        if (origin == null) return null;
        int safeColumns = Math.max(1, columns);
        int col = Math.floorMod(index, safeColumns);
        int extraRow = Math.floorDiv(index, safeColumns);
        return origin.clone().add((col + 1) * spacing, 0, (row + extraRow) * spacing);
    }

    private Location compactSurfaceCell(org.bukkit.World world, int x, int z) {
        if (world == null) return null;
        int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
        return new Location(world, x, y, z);
    }

    private void prepareCompactCell(Location base, int radius, int height) {
        if (base == null || base.getWorld() == null) return;
        clearLabCell(base, Math.max(8, radius), Math.max(6, height));
        buildLabPlatform(base, Math.max(8, radius));
    }

    private void clearLabCell(Location base, int radius, int height) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= height; dy++) {
                    world.getBlockAt(bx + dx, by + dy, bz + dz).setType(Material.AIR, false);
                }
            }
        }
    }

    private void buildLabPlatform(Location base, int radius) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean rim = Math.abs(dx) == radius || Math.abs(dz) == radius;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.SMOOTH_STONE, false);
            }
        }
        world.getBlockAt(bx, by - 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
    }

    private void labelLabCell(Location base, String id, String type) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        Block signBlock = world.getBlockAt(base.getBlockX() - 6, base.getBlockY(), base.getBlockZ() - 6);
        signBlock.setType(Material.OAK_SIGN, false);
        if (signBlock.getBlockData() instanceof Rotatable r) {
            r.setRotation(BlockFace.SOUTH);
            signBlock.setBlockData(r, false);
        }
        setSignLines(signBlock, true, new String[]{
                "LAB",
                id,
                type == null ? "" : type,
                "anchor: center"
        });
    }

    private void buildLabFixture(Site cfg, Location base) {
        String id = cfg.id();
        String type = cfg.type();
        if (isTemplateLabSite(id)) {
            StructureTemplates.keeper(id, base);
            ensureAuditAnchor(id, base);
            return;
        }

        if ("first_report_lectern_01".equals(id) || "report_lectern".equals(type)) {
            placeLabLectern(base, id, 4);
        } else if ("mara_lectern".equals(type)) {
            placeLabLectern(base, id, 10);
        } else if ("answer_sign".equals(type)) {
            placeAnswerSign(base);
        } else if ("kept_light".equals(type)) {
            placeKeptLight(base);
        } else if ("offering_cairn".equals(type)) {
            placeMarker(base, Material.COBBLESTONE, Material.CANDLE, true);
        } else if ("bow_marker".equals(type) || "orin_marker".equals(type) || "mara_map_marker".equals(type)) {
            placeMarker(base, Material.CHISELED_STONE_BRICKS, Material.CANDLE, false);
        } else if ("far_water".equals(type) || "the_far_water".equals(id)) {
            placeFarWater(base);
        } else if ("sella_pool".equals(type)) {
            placeSellaPool(base);
        } else if ("sella_anchor".equals(type)) {
            placeSellaAnchor(base);
        } else if ("vaun_hoard_chest".equals(type)) {
            placeChest(base);
        } else if ("vaun_bookshelf".equals(type)) {
            base.getBlock().setType(Material.CHISELED_BOOKSHELF, false);
        } else if ("orin_frame_dial".equals(type)) {
            placeFrameDial(base);
        } else if ("brann_corridor_start".equals(type) || "brann_corridor_end".equals(type)) {
            placeCorridorMarker(base, "brann_corridor_end".equals(type));
        } else if ("brann_toll_tower".equals(type)) {
            placeTollTower(base);
        } else if ("keeper_altar".equals(type)) {
            placeMarker(base, Material.POLISHED_DEEPSLATE, Material.SOUL_LANTERN, true);
        } else if ("coop_plate".equals(type)) {
            placeCoopPlate(base, id);
        } else if ("lampworks_stair".equals(type)) {
            buildLampworksStair(base);
        } else if ("lamp_stand".equals(type)) {
            placeLampStand(base, 3, false);
        } else if ("painted_line".equals(type)) {
            placePaintedLineFixture(base);
        } else if ("dead_stall".equals(type)) {
            buildDeadStall(base);
        } else if ("bird_coops".equals(type)) {
            buildBirdCoops(base);
        } else if ("school_stand".equals(type)) {
            buildSchoolStand(base);
        } else if ("markers_row".equals(type)) {
            buildMarkersRow(base);
        } else if ("cistern_7".equals(type)) {
            buildCisternSeven(base);
        } else if ("watch_floor".equals(type)) {
            buildWatchFloor(base);
        } else if ("set_apart_shelf".equals(type)) {
            buildSetApartShelf(base);
        } else if ("undercroft_seal".equals(type)) {
            buildUndercroftSeal(base);
        } else if ("forgotten_mouth".equals(type)) {
            buildForgottenMouth(base);
        } else if ("deep_market".equals(type)) {
            buildDeepMarket(base);
        } else if ("ration_table".equals(type)) {
            buildRationTable(base);
        } else if ("third_bay_breach".equals(type)) {
            buildThirdBayBreach(base);
        } else if ("warm_town_collapse".equals(type)) {
            buildWarmTownCollapse(base);
        } else if ("dread_route".equals(type)) {
            placeDreadRouteAnchor(base, id);
        } else if ("carve_anchor".equals(type)) {
            placeCarveWall(base);
        } else if ("soul_gallery".equals(id)) {
            placeSoulGallery(base);
        } else if ("herd_anchor".equals(id)) {
            placeHerdAnchor(base);
        } else {
            placeMarker(base, Material.CHISELED_DEEPSLATE, Material.AIR, false);
        }
    }

    private boolean isTemplateLabSite(String id) {
        return keeperRow(id) != null;
    }

    private void placeMarker(Location base, Material body, Material top, boolean lit) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_TILES, false);
            }
        }
        world.getBlockAt(x - 1, y, z + 1).setType(Material.GRAY_CARPET, false);
        world.getBlockAt(x + 1, y, z + 1).setType(Material.GRAY_CARPET, false);
        world.getBlockAt(x, y, z).setType(body, false);
        world.getBlockAt(x, y + 1, z).setType(Material.CHISELED_DEEPSLATE, false);
        if (top != null && top != Material.AIR) {
            Block b = world.getBlockAt(x, y + 2, z);
            b.setType(top, false);
            if (lit && b.getBlockData() instanceof org.bukkit.block.data.Lightable l) {
                l.setLit(true);
                b.setBlockData(l, false);
            }
        }
    }

    private void placeDreadRouteAnchor(Location base, String id) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 2;
                Material floor = rim ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((dx == 0 || dz == 0) ? Material.SCULK : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (rim && (dx + dz) % 2 == 0) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DEEPSLATE_BRICK_WALL, false);
                }
            }
        }

        for (int dx : new int[]{-3, 3}) {
            for (int dy = 0; dy <= 3; dy++) {
                Material material = dy == 3 ? Material.BLACKSTONE
                        : (dy == 1 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS);
                world.getBlockAt(bx + dx, by + dy, bz).setType(material, false);
            }
            supportToGround(world, bx + dx, by - 1, bz, Material.POLISHED_BLACKSTONE_BRICKS);
        }
        placeDreadRib(world, bx, by, bz);
        world.getBlockAt(bx, by, bz).setType(Material.SCULK_SENSOR, false);
        world.getBlockAt(bx - 1, by, bz + 1).setType(Material.COBWEB, false);
        world.getBlockAt(bx + 1, by, bz - 1).setType(Material.SOUL_TORCH, false);
        world.getBlockAt(bx, by + 3, bz).setType(Material.SOUL_LANTERN, false);

        if (id != null && id.contains("figure")) {
            world.getBlockAt(bx - 2, by, bz).setType(Material.REDSTONE_TORCH, false);
            world.getBlockAt(bx - 3, by + 1, bz).setType(Material.BLACKSTONE, false);
        } else if (id != null && id.contains("elsewhere")) {
            world.getBlockAt(bx, by - 1, bz).setType(Material.SCULK, false);
            world.getBlockAt(bx, by + 1, bz).setType(Material.IRON_CHAIN, false);
        } else if (id != null && id.contains("exit")) {
            world.getBlockAt(bx, by, bz + 1).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(bx - 1, by, bz).setType(Material.BLACK_CANDLE, false);
        }

        placeDreadLabel(base.clone().add(0, 0, -3), dreadAnchorLines(id));
    }

    private String[] dreadAnchorLines(String id) {
        if (id != null && id.contains("elsewhere")) {
            return new String[]{"the sky", "keeps the", "wrong room", ""};
        }
        if (id != null && id.contains("figure")) {
            return new String[]{"the place", "behind you", "is occupied", ""};
        }
        if (id != null && id.contains("exit")) {
            return new String[]{"count who", "left with", "their shadow", ""};
        }
        return new String[]{"the way is", "already", "listening", ""};
    }

    private void placeLabLectern(Location base, String title, int pages) {
        Block b = base.getBlock();
        placeReadableLectern(b, BlockFace.SOUTH);
        fillLabLecternBook(b, title, pages);
        if (plugin.scheduler() != null) {
            plugin.scheduler().runLaterSafe("command.placelab.lectern.book", 1L,
                    () -> fillLabLecternBook(b, title, pages));
        }
    }

    private void placeReadableLectern(Block b, BlockFace facing) {
        if (b == null) return;
        b.setType(Material.LECTERN, false);
        if (b.getBlockData() instanceof Directional d) {
            d.setFacing(facing == null ? BlockFace.SOUTH : facing);
            b.setBlockData(d, false);
        }
        b.getState().update(true, false);
    }

    private void fillLabLecternBook(Block b, String title, int pages) {
        if (b == null || b.getType() != Material.LECTERN) return;
        if (b.getState() instanceof Lectern lectern) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            if (book.getItemMeta() instanceof BookMeta meta) {
                meta.setTitle(com.observance.watcher.util.TextFit.clampLine(title, 32));
                meta.setAuthor("the lab");
                for (int i = 1; i <= Math.max(1, pages); i++) {
                    meta.addPage("page " + i + "\n\n" + title + "\n\nturn me for page-lock testing.");
                }
                book.setItemMeta(meta);
            }
            writeLecternBook(b, lectern, book);
        }
    }

    private void fillMaraLockBook(Block b, int index, int markedPage) {
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            boolean marked = page == markedPage;
            pages.add((marked ? "[a pressed-dark margin]\n\n" : "")
                    + "Mara shelf " + index + "\npage " + page + "\n\n"
                    + (marked
                    ? "this is the page her hand left open."
                    : "the line continues elsewhere, but not here."));
        }
        fillWrittenLecternBook(b, "mara shelf " + index, "mara", pages);
    }

    private void fillSellaLockBook(Block b, int index, int markedPage) {
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= 12; page++) {
            boolean marked = page == markedPage;
            pages.add((marked ? "[a water-ring circles this tally]\n\n" : "")
                    + "Sella loose page " + index + "\npage " + page + "\n\n"
                    + (marked
                    ? "the dropped stone made this ring before the next."
                    : "the wet graphite runs into an unfinished bird."));
        }
        fillWrittenLecternBook(b, "sella ring " + index, "sella", pages);
    }

    private void fillPrologueLecternBook(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Block b = loc.getBlock();
        placeReadableLectern(b, BlockFace.SOUTH);
        fillWrittenLecternBook(b, "the record opens", "the record", List.of(
                "the record opens.\n\nit was opened before. it is opened again, as it is opened for any who come and stay.",
                "so the count begins. the living are written here, each by the name they answer to, and against each name a column is left open.",
                "nothing is owed yet. but the column is open, and an open column is a thing that fills."));
    }

    private void fillWrittenLecternBook(Block b, String title, String author, List<String> pages) {
        if (b == null || b.getType() != Material.LECTERN) return;
        if (b.getState() instanceof Lectern lectern) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            if (book.getItemMeta() instanceof BookMeta meta) {
                meta.setTitle(com.observance.watcher.util.TextFit.clampLine(title == null ? "record" : title, 32));
                meta.setAuthor(com.observance.watcher.util.TextFit.clampLine(author == null ? "the record" : author, 32));
                List<String> realPages = pages == null || pages.isEmpty() ? List.of("") : pages;
                for (String page : realPages) {
                    String body = page == null ? "" : page;
                    if (body.length() <= com.observance.watcher.util.TextFit.BOOK_PAGE_CHARS) {
                        meta.addPage(body);
                    } else {
                        for (String real : com.observance.watcher.util.TextFit.paginate(body)) {
                            meta.addPage(real);
                        }
                    }
                }
                book.setItemMeta(meta);
            }
            writeLecternBook(b, lectern, book);
        }
    }

    private void writeLecternBook(Block block, Lectern lectern, ItemStack book) {
        if (block == null || lectern == null || book == null) return;
        try {
            lectern.getSnapshotInventory().setItem(0, book.clone());
            lectern.update(true, false);
        } catch (Throwable ignored) { }
        if (block.getState() instanceof Lectern live) {
            live.getInventory().setItem(0, book.clone());
        }
    }

    private void ensureAuditAnchor(String siteId, Location loc) {
        if (siteId == null || loc == null || loc.getWorld() == null) return;
        if (!isCoreAuditSite(siteId)) return;
        Block block = loc.getBlock();
        if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR
                && block.getType() != Material.VOID_AIR) {
            return;
        }
        Material anchor = switch (siteId) {
            case "rune_rosetta", "stone_of_reckoning" -> Material.CHISELED_TUFF;
            case "stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss" -> Material.CHISELED_DEEPSLATE;
            case "the_cold_hearth" -> Material.SOUL_CAMPFIRE;
            case "unbroken_light" -> Material.SEA_LANTERN;
            case "the_threshold", "threshold_vault" -> Material.REINFORCED_DEEPSLATE;
            case "the_unwriting" -> Material.SCULK_SHRIEKER;
            default -> Material.CHISELED_DEEPSLATE;
        };
        block.setType(anchor, false);
        if (block.getBlockData() instanceof org.bukkit.block.data.Lightable lightable) {
            lightable.setLit(true);
            block.setBlockData(lightable, false);
        }
    }

    private void placeAnswerSign(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world != null) {
            int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    world.getBlockAt(x + dx, y - 1, z + dz)
                            .setType(rim ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_TILES, false);
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                world.getBlockAt(x + dx, y, z + 2).setType(Material.CHISELED_DEEPSLATE, false);
                world.getBlockAt(x + dx, y + 1, z + 2).setType(Material.POLISHED_DEEPSLATE, false);
            }
        }
        Block b = base.getBlock();
        b.setType(Material.OAK_SIGN, false);
        if (b.getBlockData() instanceof Rotatable r) {
            r.setRotation(BlockFace.SOUTH);
            b.setBlockData(r, false);
        }
        setSignLines(b, false, new String[]{"", "", "", ""});
    }

    private void placeKeptLight(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(rim ? Material.BRICKS : Material.PACKED_MUD, false);
            }
        }
        Block fire = world.getBlockAt(x, y, z);
        fire.setType(Material.CAMPFIRE, false);
        if (fire.getBlockData() instanceof org.bukkit.block.data.type.Campfire c) {
            c.setLit(true);
            fire.setBlockData(c, false);
        }
        world.getBlockAt(x + 1, y, z).setType(Material.LANTERN, false);
        world.getBlockAt(x - 1, y, z).setType(Material.DARK_OAK_STAIRS, false);
        world.getBlockAt(x, y, z + 2).setType(Material.BARREL, false);
    }

    private void placeSellaPool(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean water = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                Material floor = water ? Material.PRISMARINE_BRICKS
                        : (Math.abs(dx) == 3 || Math.abs(dz) == 3 ? Material.DARK_PRISMARINE : Material.PRISMARINE);
                world.getBlockAt(x + dx, y - 1, z + dz).setType(floor, false);
                if (!water) world.getBlockAt(x + dx, y, z + dz).setType(Material.AIR, false);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.PRISMARINE_BRICKS, false);
                world.getBlockAt(x + dx, y, z + dz).setType(Material.WATER, false);
            }
        }
        world.getBlockAt(x, y, z - 2).setType(Material.DARK_PRISMARINE, false);
        world.getBlockAt(x, y + 1, z - 2).setType(Material.BLUE_CANDLE, false);
    }

    private void placeSellaAnchor(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 2 || dz == -1 || dz == 3;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(edge ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS, false);
            }
        }
        world.getBlockAt(x, y, z).setType(Material.DARK_PRISMARINE, false);
        world.getBlockAt(x, y - 1, z + 1).setType(Material.WATER, false);
        world.getBlockAt(x, y - 1, z + 2).setType(Material.WATER, false);
        world.getBlockAt(x - 1, y, z).setType(Material.WHITE_CARPET, false);
        world.getBlockAt(x + 1, y, z).setType(Material.GRAY_CANDLE, false);
    }

    private void placeChest(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world != null) {
            int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    world.getBlockAt(x + dx, y - 1, z + dz)
                            .setType(rim ? Material.POLISHED_TUFF : Material.TUFF_BRICKS, false);
                }
            }
            world.getBlockAt(x - 1, y, z + 1).setType(Material.BARREL, false);
            world.getBlockAt(x + 1, y, z + 1).setType(Material.TRAPPED_CHEST, false);
        }
        Block b = base.getBlock();
        b.setType(Material.CHEST, false);
        if (b.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.SOUTH);
            b.setBlockData(d, false);
        }
    }

    private void placeFrameDial(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(Math.abs(dx) == 2 || dz == 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_TILES, false);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(x + dx, y, z).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(x + dx, y + 1, z).setType(Material.POLISHED_DEEPSLATE, false);
        }
        world.getBlockAt(x, y, z).setType(Material.CHISELED_DEEPSLATE, false);
        for (ItemFrame f : world.getNearbyEntitiesByType(ItemFrame.class, base, 3.0)) {
            f.remove();
        }
        Location frameLoc = new Location(world, x + 0.5, y + 1.5, z + 1.0);
        ItemFrame frame = world.spawn(frameLoc, ItemFrame.class);
        frame.setFacingDirection(BlockFace.SOUTH, true);
        frame.setItem(new ItemStack(Material.ARROW));
        frame.setRotation(Rotation.NONE);
    }

    private void placeCorridorMarker(Location base, boolean end) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean wall = Math.abs(dx) == 2;
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.SCULK, false);
                if (wall) {
                    world.getBlockAt(x + dx, y, z + dz).setType(Material.DEEPSLATE_BRICKS, false);
                    world.getBlockAt(x + dx, y + 1, z + dz).setType(Material.DEEPSLATE_BRICKS, false);
                }
            }
        }
        for (int dz = -3; dz <= 3; dz++) {
            world.getBlockAt(x, y - 1, z + dz).setType(Material.SCULK, false);
        }
        world.getBlockAt(x, y, z).setType(end ? Material.REDSTONE_LAMP : Material.SCULK_SENSOR, false);
        Block sign = world.getBlockAt(x + 1, y, z);
        sign.setType(Material.OAK_SIGN, false);
        if (sign.getBlockData() instanceof Rotatable r) {
            r.setRotation(BlockFace.SOUTH);
            sign.setBlockData(r, false);
        }
        setSignLines(sign, true, new String[]{end ? "END" : "START", "sneak only", "", ""});
    }

    private void placeTollTower(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(rim ? Material.COBBLED_DEEPSLATE : Material.BLACK_CONCRETE, false);
            }
        }
        for (int dy = 0; dy <= 4; dy++) world.getBlockAt(x, y + dy, z).setType(Material.DEEPSLATE_BRICKS, false);
        world.getBlockAt(x, y + 5, z).setType(Material.BELL, false);
        world.getBlockAt(x + 1, y, z).setType(Material.CAMPFIRE, false);
        world.getBlockAt(x - 1, y, z).setType(Material.DAYLIGHT_DETECTOR, false);
    }

    private void placeCoopPlate(Location base, String id) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                world.getBlockAt(x + dx, y - 1, z + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.CHISELED_TUFF, false);
            }
        }
        world.getBlockAt(x, y - 1, z).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(x, y, z).setType(Material.STONE_PRESSURE_PLATE, false);
        world.getBlockAt(x + 1, y, z).setType(Material.CHISELED_TUFF, false);
        if (id != null && id.contains("vault")) {
            Block sign = world.getBlockAt(x, y, z + 2);
            sign.setType(Material.OAK_SIGN, false);
            if (sign.getBlockData() instanceof Rotatable r) {
                r.setRotation(BlockFace.SOUTH);
                sign.setBlockData(r, false);
            }
            setSignLines(sign, false, new String[]{"", "", "", ""});
        }
    }

    private void placeCarveWall(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(x + dx, y - 1, z - 1).setType(Material.DEEPSLATE_TILES, false);
            world.getBlockAt(x + dx, y - 1, z).setType(Material.POLISHED_DEEPSLATE, false);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                world.getBlockAt(x + dx, y + dy, z).setType(Material.POLISHED_DEEPSLATE, false);
            }
        }
    }

    private void placeSoulGallery(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.SOUL_SAND, false);
        }
        world.getBlockAt(x, y, z).setType(Material.SOUL_FIRE, false);
    }

    private void placeHerdAnchor(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        world.getBlockAt(x, y, z).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(x + 1, y, z).setType(Material.MOSS_BLOCK, false);
        world.getBlockAt(x - 1, y, z).setType(Material.MOSS_BLOCK, false);
    }

    private void placeFarWater(Location base) {
        if (base == null || base.getWorld() == null) return;
        org.bukkit.World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();

        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -4; dz <= 5; dz++) {
                boolean shore = Math.abs(dx) == 6 || dz == -4 || dz == 5;
                boolean still = Math.abs(dx) <= 3 && dz >= -2 && dz <= 2;
                Material floor = still ? Material.DARK_PRISMARINE
                        : (shore ? Material.POLISHED_BLACKSTONE_BRICKS : Material.PRISMARINE_BRICKS);
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(floor, false);
                if (still) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.WATER, false);
                } else if (!shore) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.SEAGRASS, false);
                }
                if (shore && (Math.abs(dx) == 6 || dz == 5)) {
                    supportToGround(world, bx + dx, by - 1, bz + dz, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            Material marker = dx == 3 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.CHISELED_DEEPSLATE;
            world.getBlockAt(bx + dx, by, bz + 4).setType(marker, false);
            if (dx == 3) world.getBlockAt(bx + dx, by + 1, bz + 4).setType(Material.GRAY_CANDLE, false);
        }

        for (int dy = 0; dy <= 3; dy++) {
            world.getBlockAt(bx - 5, by + dy, bz - 2).setType(dy == 3 ? Material.SEA_LANTERN : Material.DARK_PRISMARINE, false);
            world.getBlockAt(bx + 5, by + dy, bz + 3).setType(dy == 3 ? Material.SEA_LANTERN : Material.DARK_PRISMARINE, false);
        }
        world.getBlockAt(bx, by + 1, bz - 3).setType(Material.IRON_CHAIN, false);
        world.getBlockAt(bx, by, bz - 3).setType(Material.SOUL_LANTERN, false);
        placeDecorativeBookshelf(world.getBlockAt(bx - 2, by, bz - 3), 53);
        placeEvidenceLectern(new Location(world, bx + 2, by, bz - 3), BlockFace.WEST,
                "far water copy", List.of(
                        "six stones and one grey.",
                        "face the water and read what holds still.",
                        "the line is not straight when the copy gives it back."
                ));
        world.getBlockAt(bx - 4, by, bz + 4).setType(Material.LIGHT_GRAY_CARPET, false);
        world.getBlockAt(bx + 4, by, bz - 3).setType(Material.DARK_PRISMARINE_SLAB, false);
    }

    private void setSignLines(Block b, boolean waxed, String[] lines) {
        if (b == null) return;
        try {
            if (b.getState() instanceof Sign sign) {
                var front = sign.getSide(Side.FRONT);
                for (int i = 0; i < 4; i++) {
                    String line = (lines != null && i < lines.length && lines[i] != null) ? lines[i] : "";
                    front.setLine(i, com.observance.watcher.util.TextFit.clampLine(line,
                            com.observance.watcher.util.TextFit.SIGN_LINE_CHARS));
                }
                try { sign.setWaxed(waxed); } catch (Throwable ignored) { }
                sign.update(true, false);
            }
        } catch (Throwable ignored) { }
    }

    /**
     * {@code /observance placeprologue} — stages the COLD-START PROLOGUE anomaly in the group's OWN base
     * (cold-start-prologue §1.1–1.2 / §7 item 6): the first-report lectern + a lit marker that "wasn't
     * there yesterday," discovered out of sight. This is the screenshot→ignition hook.
     *
     * <p><b>Retarget to the live base.</b> The two prologue sites ({@code first_report_lectern_01},
     * {@code first_marker_01}) ship with null coords. This command resolves the group's most-confident
     * base cell live via {@link com.observance.watcher.signal.BaseDetector#primaryBase()} (the bed/
     * placement centroid — "the most-trafficked block the group passes through") and stamps BOTH sites
     * beside it, persisting them via {@code registerRuntimeSite}. If no base has accrued enough signal
     * yet (fresh world / first session), it falls back to the sender's own location so the prologue is
     * still placeable — the base retarget simply improves aim when the data exists.
     *
     * <p><b>Reveal-disciplined.</b> Both objects are placed through the existing library beats under
     * {@code mutateWhenUnwitnessed}: the report via {@link com.observance.watcher.beats.lib.LecternFillBeat}
     * ({@code place_if_missing}) and the lit marker (one carved stone + one candle) via
     * {@link com.observance.watcher.beats.lib.SmallStructureBeat}. Nothing is witnessed changing; the
     * group discovers an after-state. Idempotent: the beats' footprint sweep + in-process applied-set
     * mean a re-run never double-places, and the retarget writes the same coords for the same base.
     *
     * <p>The lectern carries the un-named FACT-1 collective marquee (§2.2 default-safe form) — the
     * conditional named inflection is the showrunner's job, never guessed here. Player-only. Requires
     * the beat engine to be active (it enacts through the shared {@link
     * com.observance.watcher.beats.BeatContext}).
     */
    private void handlePlacePrologue(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placeprologue must be run by a player (needs a location).");
            return;
        }
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || !engine.isActive() || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine is not active — cannot stage the prologue.");
            return;
        }

        // 1) Resolve the base anchor: prefer the live detected base cell, fall back to the sender.
        Location anchor = null;
        String source = "your location (no base detected yet)";
        try {
            com.observance.watcher.signal.SignalTracker tracker = plugin.signalTracker();
            if (tracker != null && tracker.baseDetector() != null) {
                com.observance.watcher.signal.BaseDetector.Anchor a = tracker.baseDetector().primaryBase();
                if (a != null) {
                    org.bukkit.World w = Bukkit.getWorld(a.world);
                    if (w != null) {
                        anchor = new Location(w, a.x, a.y, a.z);
                        source = "detected base " + a.x + "," + a.y + "," + a.z
                                + " (confidence " + String.format(Locale.ROOT, "%.2f", a.confidence) + ")";
                    }
                }
            }
        } catch (Throwable ignored) { /* fall back below */ }
        if (anchor == null) {
            anchor = player.getLocation();
        }
        if (anchor == null || anchor.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve an anchor for the prologue.");
            return;
        }
        final org.bukkit.World world = anchor.getWorld();
        final String worldName = world.getName();

        // 2) Seat the lectern on the surface at the anchor, and the marker one block to the side (east),
        //    also surface-seated. Reveal discipline is enforced by the beats themselves at mutation time.
        int lx = anchor.getBlockX(), lz = anchor.getBlockZ();
        int ly = world.getHighestBlockYAt(lx, lz, org.bukkit.HeightMap.MOTION_BLOCKING) + 1;
        Location lecternLoc = new Location(world, lx, ly, lz);

        int mx = lx + 1, mz = lz;
        int my = world.getHighestBlockYAt(mx, mz, org.bukkit.HeightMap.MOTION_BLOCKING) + 1;
        Location markerLoc = new Location(world, mx, my, mz);

        // 3) Retarget + persist BOTH prologue sites to these coords (idempotent — same base = same coords).
        plugin.registerRuntimeSite(new Site("first_report_lectern_01", "report_lectern", worldName,
                lecternLoc.getX(), lecternLoc.getY(), lecternLoc.getZ(), 4, 4, true, true, null));
        plugin.registerRuntimeSite(new Site("first_marker_01", "structure", worldName,
                markerLoc.getX(), markerLoc.getY(), markerLoc.getZ(), 6, 4, true, true, null));

        com.observance.watcher.beats.BeatContext ctx = engine.context();
        com.observance.watcher.beats.BeatLibrary lib = engine.library();

        // 4a) The first report — un-named FACT-1 collective marquee (§2.2 default-safe). LecternFillBeat
        //     places the lectern if missing and fills the book, all out of line of sight.
        String lecternPayload = "{"
                + "\"place_if_missing\":true,"
                + "\"title\":\"the record opens\","
                + "\"author\":\"the record\","
                + "\"pages\":["
                + "\"the record opens.\\n\\nit was opened before. it is opened again, as it is opened for any who come and stay.\","
                + "\"so the count begins. the living are written here, each by the name they answer to, and against each name a column is left open.\","
                + "\"nothing is owed yet. but the column is open, and an open column is a thing that fills.\""
                + "]}";
        Site lecternSite = plugin.sites().get("first_report_lectern_01");
        com.observance.watcher.beats.BeatResult rLectern = enactDirected(ctx, lib,
                "lectern_fill", "prologue-lectern", lecternSite,
                com.observance.watcher.beats.BeatPayload.parse(lecternPayload));
        fillPrologueLecternBook(lecternLoc);
        if (plugin.scheduler() != null) {
            plugin.scheduler().runLaterSafe("command.placeprologue.lectern.book", 1L,
                    () -> fillPrologueLecternBook(lecternLoc));
        }

        // 4b) The lit marker — one carved stone + one candle "that wasn't there." SmallStructureBeat
        //     footprint-checks + places out of sight; require_floor so it never floats.
        String markerPayload = "{"
                + "\"require_floor\":true,"
                + "\"blocks\":["
                + "{\"dx\":0,\"dy\":0,\"dz\":0,\"material\":\"CHISELED_STONE_BRICKS\"},"
                + "{\"dx\":0,\"dy\":1,\"dz\":0,\"material\":\"CANDLE\"}"
                + "]}";
        Site markerSite = plugin.sites().get("first_marker_01");
        com.observance.watcher.beats.BeatResult rMarker = enactDirected(ctx, lib,
                "small_structure", "prologue-marker", markerSite,
                com.observance.watcher.beats.BeatPayload.parse(markerPayload));

        sender.sendMessage("Observance: prologue staged at " + source + ".");
        sender.sendMessage("  lectern @ " + lx + "," + ly + "," + lz + " -> " + describe(rLectern));
        sender.sendMessage("  lit marker @ " + mx + "," + my + "," + mz + " -> " + describe(rMarker));
        sender.sendMessage("  (Placement is reveal-disciplined: if a player has line of sight now, the beat");
        sender.sendMessage("   retries out of sight — re-run if it reports witnessed/occupied.)");
    }

    /** Enact one directed library beat by name against a resolved site, Safety-wrapped. Null-safe. */
    private com.observance.watcher.beats.BeatResult enactDirected(
            com.observance.watcher.beats.BeatContext ctx,
            com.observance.watcher.beats.BeatLibrary lib,
            String beatType, String beatId, Site site,
            com.observance.watcher.beats.BeatPayload payload) {
        com.observance.watcher.beats.Beat beat = lib.get(beatType);
        if (beat == null) return com.observance.watcher.beats.BeatResult.skipped("no-beat:" + beatType);
        com.observance.watcher.beats.BeatRequest req = new com.observance.watcher.beats.BeatRequest(
                beatId, beatType, com.observance.watcher.beats.BeatCategory.DIRECTED, null, site, payload);
        return safety.call("command.prologue.enact." + beatType,
                () -> beat.enact(ctx, req),
                com.observance.watcher.beats.BeatResult.failed("threw"));
    }

    private static String describe(com.observance.watcher.beats.BeatResult r) {
        if (r == null) return "null";
        return r.kind() + (r.reason() == null ? "" : " (" + r.reason() + ")");
    }

    /**
     * {@code /observance needle [player]} — the MANUAL TEST PATH for THE KEPT NEEDLE (the recovery-compass).
     * Grants one recipient the lodestone-needle that points at the kept light ({@code unbroken_light}) so it
     * can be verified in-world. This bypasses the LATE gate on purpose (admin test only); the REAL, earned
     * grant is a directed {@code beat_queue} row of type {@code kept_needle} gated on {@code seventh_named}
     * (the record hands it over post-reveal) — see {@link com.observance.watcher.beats.lib.KeptNeedleBeat}.
     *
     * <p>SHOWRUNNER WIRE-UP (what the director finishes): enqueue a beat_queue row
     * {@code {type:"kept_needle", requires_flags:{seventh_named:true}, payload:{"site":"unbroken_light",
     * "to":"target"}}} bound to the earning player — FlagGate keeps it closed until the Seventh is named, then
     * the poller enacts it exactly like this command does. No plugin-side flag re-read is needed or wanted.
     */
    private void handleNeedle(CommandSender sender, String[] args) {
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || !engine.isActive() || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine is not active — cannot grant the needle.");
            return;
        }
        // Recipient: an explicit player name, else the sender (must be a player).
        Player target;
        if (args.length >= 2 && !args[1].isBlank()) {
            target = Bukkit.getPlayerExact(args[1].trim());
            if (target == null) {
                sender.sendMessage("Observance: player '" + args[1].trim() + "' is not online.");
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage("Observance: /observance needle needs a player (run in-game or pass a name).");
            return;
        }

        // Confirm the kept light is placed, so the needle points somewhere (the beat also guards this).
        Site kept = plugin.sites() == null ? null : plugin.sites().get("unbroken_light");
        if (kept == null || kept.location() == null) {
            sender.sendMessage("Observance: the kept light (unbroken_light) is not placed yet — run placedeep first.");
            return;
        }

        // Enact the kept_needle beat directly at the target (DIRECTED; test-only, gate bypassed by intent).
        com.observance.watcher.beats.BeatContext ctx = engine.context();
        com.observance.watcher.beats.Beat beat = engine.library().get("kept_needle");
        if (beat == null) {
            sender.sendMessage("Observance: kept_needle beat is not registered.");
            return;
        }
        com.observance.watcher.beats.BeatRequest req = new com.observance.watcher.beats.BeatRequest(
                "needle-test", "kept_needle", com.observance.watcher.beats.BeatCategory.DIRECTED,
                target, kept, com.observance.watcher.beats.BeatPayload.parse(
                        "{\"site\":\"unbroken_light\",\"to\":\"target\"}"));
        com.observance.watcher.beats.BeatResult r = safety.call("command.needle.enact",
                () -> beat.enact(ctx, req), com.observance.watcher.beats.BeatResult.failed("threw"));

        sender.sendMessage("Observance: kept needle -> " + target.getName() + " -> " + describe(r));
        sender.sendMessage("  (Test grant — bypasses the seventh_named gate. The needle points at unbroken_light @ "
                + kept.location().getBlockX() + "," + kept.location().getBlockY() + "," + kept.location().getBlockZ() + ".)");
    }

    /**
     * {@code /observance lens give [player]} — hands out {@link com.observance.watcher.lens.LensItem
     * the Lens} relic (INTEGRATION §SIGNATURE #3 "second sight"). With no player argument, gives it to
     * the sender (must be a player). Refreshes the recipient's gated-rune visibility immediately so a
     * rune placed before they held the Lens appears the moment it lands in an empty hand.
     */
    /**
     * {@code /observance test <menu|preset> [player]} - director controls for forcing late-game
     * hauntings and story beats during staging. These call the real beat engine with a temporary
     * local site, so the test catches resource-pack, visibility, and reveal-discipline problems.
     */
    private void handleTest(CommandSender sender, String[] args) {
        String preset = args.length > 1 ? args[1].trim().toLowerCase(Locale.ROOT) : "menu";
        if (preset.isBlank() || preset.equals("menu") || preset.equals("help")) {
            sendTestMenu(sender);
            return;
        }

        Player target = testTarget(sender, args);
        if (target == null) return;
        Location anchor = testAnchor(sender, target);
        if (anchor == null || anchor.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve a test location.");
            return;
        }

        if (preset.equals("needle")) {
            handleNeedle(sender, target.equals(sender) ? new String[]{"needle"} : new String[]{"needle", target.getName()});
            return;
        }
        if (preset.equals("stalker") || preset.equals("figure") || preset.equals("danger")) {
            handleStalkerTest(sender, target, anchor);
            return;
        }
        if (preset.equals("hunt") || preset.equals("hunted") || preset.equals("ambush")) {
            handleHuntTest(sender, target, anchor);
            return;
        }
        if (preset.equals("elsewhere") || preset.equals("void") || preset.equals("dimension")) {
            handleElsewhereTest(sender, target, anchor);
            return;
        }
        if (preset.equals("gauntlet") || preset.equals("nightmare") || preset.equals("scarepass")) {
            handleScareGauntlet(sender, target);
            return;
        }

        String beatType;
        com.observance.watcher.beats.BeatCategory category = com.observance.watcher.beats.BeatCategory.AMBIENT;
        String payload;
        switch (preset) {
            case "whisper" -> {
                beatType = "private_message";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"mode\":\"actionbar\",\"text\":\"the record notices where you stand\"}";
            }
            case "title" -> {
                beatType = "private_message";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"mode\":\"actionbar\",\"text\":\"something is listening\"}";
            }
            case "sound" -> {
                beatType = "private_sound";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"named_sound\":\"observance:whisper\",\"volume\":0.7,\"pitch\":0.85,\"behind\":true,\"offset\":4.0}";
            }
            case "voice" -> {
                beatType = "spatial_voice";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"named_sound\":\"observance:keeper_voice\",\"volume\":0.75,\"pitch\":1.0,\"behind\":true,\"offset\":5.0}";
            }
            case "dark", "darkness" -> {
                beatType = "private_darkness";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"seconds\":4,\"amplifier\":0}";
            }
            case "name", "wall" -> {
                prepareWallTest(anchor);
                beatType = "name_on_wall";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"distance\":3,\"seconds\":8,\"look_away_despawn\":true,\"rune_font\":true,\"text\":\"%name%\",\"color\":\"#8a1c1c\",\"billboard\":true,\"glow\":false}";
            }
            case "reflection", "water" -> {
                prepareReflectionTest(anchor);
                beatType = "reflection";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"text\":\"FAR WATER\",\"rune_font\":true,\"seconds\":12,\"look_watch\":false,\"search_radius\":6}";
            }
            case "mob", "watcher" -> {
                beatType = "named_mob";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"entity\":\"WARDEN\",\"name\":\"the watcher\",\"distance\":12,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":0,\"retreating\":false}";
            }
            case "torch" -> {
                prepareTorchTest(anchor);
                beatType = "torch_gutter";
                payload = "{\"radius\":6,\"max_torches\":3,\"relight_seconds\":30,\"permanent\":false}";
            }
            case "decay" -> {
                prepareWallTest(anchor);
                beatType = "decay_creep";
                payload = "{\"radius\":4,\"count\":3,\"material\":\"COBWEB\",\"needs_support\":true}";
            }
            case "drift" -> {
                prepareWallTest(anchor);
                beatType = "world_drift";
                payload = "{\"radius\":4,\"count\":3,\"vein_material\":\"SCULK_VEIN\",\"floor_material\":\"MOSS_CARPET\"}";
            }
            case "particles", "particle" -> {
                beatType = "private_particle";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"particle\":\"SMOKE\",\"count\":18,\"spread\":0.45,\"speed\":0.0,\"height\":1.0,\"near_player\":true}";
            }
            case "toast" -> {
                beatType = "advancement_toast";
                category = com.observance.watcher.beats.BeatCategory.DIRECTED;
                payload = "{\"advancement\":\"observance:record_notes_you\",\"fallback_title\":\"The record notes you\",\"fallback_subtitle\":\"This is only a test.\"}";
            }
            case "sign" -> {
                prepareWallTest(anchor);
                beatType = "sign_write";
                payload = "{\"lines\":[\"THE RECORD\",\"LEAVES MARKS\",\"WHERE YOU\",\"LOOK AWAY\"],\"side\":\"front\",\"place_if_missing\":true,\"material\":\"OAK_WALL_SIGN\",\"glowing\":false}";
            }
            default -> {
                sender.sendMessage("Observance: unknown test preset '" + preset + "'.");
                sendTestMenu(sender);
                return;
            }
        }

        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null) {
            sender.sendMessage("Observance: beat engine unavailable.");
            return;
        }
        com.observance.watcher.beats.Beat beat = engine.library().get(beatType);
        if (beat == null) {
            sender.sendMessage("Observance: beat '" + beatType + "' is not registered.");
            return;
        }
        Site testSite = new Site("test_" + preset, "test", anchor.getWorld().getName(),
                (double) anchor.getBlockX(), (double) anchor.getBlockY(), (double) anchor.getBlockZ(),
                8, 8, false, true, null);
        com.observance.watcher.beats.BeatRequest req = new com.observance.watcher.beats.BeatRequest(
                "admin-test-" + preset + "-" + System.nanoTime(), beatType, category,
                target, testSite, com.observance.watcher.beats.BeatPayload.parse(payload));
        com.observance.watcher.beats.BeatResult r = safety.call("command.test." + preset,
                () -> beat.enact(engine.context(), req), com.observance.watcher.beats.BeatResult.failed("threw"));

        sender.sendMessage("Observance test: " + preset + " -> " + target.getName() + " -> " + describe(r));
        if (preset.equals("reflection") || preset.equals("torch") || preset.equals("decay") || preset.equals("drift")
                || preset.equals("sign")) {
            sender.sendMessage("  Step back or look away for a few seconds if the change is reveal-disciplined.");
        }
    }

    private void sendTestMenu(CommandSender sender) {
        sender.sendMessage("== Observance test presets ==");
        sender.sendMessage("/obs test whisper [player]    - private actionbar text");
        sender.sendMessage("/obs test title [player]      - quiet perception text (actionbar)");
        sender.sendMessage("/obs test sound [player]      - resource-pack whisper");
        sender.sendMessage("/obs test voice [player]      - spatial Keeper voice");
        sender.sendMessage("/obs test darkness [player]   - short darkness effect");
        sender.sendMessage("/obs test name [player]       - name appears on a wall");
        sender.sendMessage("/obs test reflection [player] - water-rune reflection");
        sender.sendMessage("/obs test mob [player]        - watcher body behind/near player");
        sender.sendMessage("/obs test stalker [player]    - humanlike danger scare sequence");
        sender.sendMessage("/obs test hunt [player]       - multi-figure pursuit scare sequence");
        sender.sendMessage("/obs test elsewhere [player]  - wrong-place sky/fog scare sequence");
        sender.sendMessage("/obs test gauntlet [player]   - timed Watcher scare rehearsal");
        sender.sendMessage("/obs test torch [player]      - nearby torches gutter");
        sender.sendMessage("/obs test decay [player]      - cobweb creep on support blocks");
        sender.sendMessage("/obs test drift [player]      - sculk/moss world drift");
        sender.sendMessage("/obs test particles [player]  - private smoke particles");
        sender.sendMessage("/obs test toast [player]      - story advancement toast");
        sender.sendMessage("/obs test sign [player]       - forced sign writing");
        sender.sendMessage("/obs test needle [player]     - recovery needle to kept light");
    }

    private Player testTarget(CommandSender sender, String[] args) {
        if (args.length >= 3 && !args[2].isBlank()) {
            Player p = Bukkit.getPlayerExact(args[2].trim());
            if (p == null) {
                sender.sendMessage("Observance: player '" + args[2].trim() + "' not found / offline.");
            }
            return p;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage("Observance: console must name a player.");
        return null;
    }

    private void handleStalkerTest(CommandSender sender, Player target, Location anchor) {
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine unavailable.");
            return;
        }
        Site testSite = new Site("test_stalker", "test", anchor.getWorld().getName(),
                (double) anchor.getBlockX(), (double) anchor.getBlockY(), (double) anchor.getBlockZ(),
                12, 8, false, true, null);
        runTestBeat(engine, target, testSite, "stalker-dark", "private_darkness",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"effect\":\"DARKNESS\",\"seconds\":6,\"amplifier\":0}");
        runTestBeat(engine, target, testSite, "stalker-sound", "private_sound",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"sound\":\"ENTITY_WARDEN_HEARTBEAT\",\"volume\":1.4,\"pitch\":0.55,\"behind\":true,\"offset\":2.5}");
        if (plugin.scheduler() != null) {
            plugin.scheduler().runLaterSafe("command.test.stalker.pressure", 20L,
                    () -> runTestBeat(engine, target, testSite, "stalker-pressure", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"actionbar\",\"text\":\"someone is standing where you were\"}"));
            plugin.scheduler().runLaterSafe("command.test.stalker.figure", 35L,
                    () -> runTestBeat(engine, target, testSite, "stalker-figure", "named_mob",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"entity\":\"WITHER_SKELETON\",\"fallback_entity\":\"STRAY\",\"name\":\"\",\"distance\":8,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":20,\"name_visible\":false}"));
        }
        sender.sendMessage("Observance test: stalker -> " + target.getName()
                + " -> darkness, close sound, quiet pressure text, and a tall silent figure queued.");
    }

    private void handleHuntTest(CommandSender sender, Player target, Location anchor) {
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine unavailable.");
            return;
        }
        Site testSite = new Site("test_hunt", "test", anchor.getWorld().getName(),
                (double) anchor.getBlockX(), (double) anchor.getBlockY(), (double) anchor.getBlockZ(),
                16, 10, false, true, null);
        runTestBeat(engine, target, testSite, "hunt-dark", "private_darkness",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"effect\":\"DARKNESS\",\"seconds\":10,\"amplifier\":0}");
        runTestBeat(engine, target, testSite, "hunt-breath", "private_sound",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"sound\":\"ENTITY_ENDERMAN_STARE\",\"volume\":1.1,\"pitch\":0.65,\"behind\":true,\"offset\":2.0}");
        if (plugin.scheduler() != null) {
            plugin.scheduler().runLaterSafe("command.test.hunt.pressure", 18L,
                    () -> runTestBeat(engine, target, testSite, "hunt-pressure", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"actionbar\",\"text\":\"do not look for the first one\"}"));
            plugin.scheduler().runLaterSafe("command.test.hunt-figure-a", 25L,
                    () -> runTestBeat(engine, target, testSite, "hunt-figure-a", "named_mob",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"entity\":\"WITHER_SKELETON\",\"fallback_entity\":\"STRAY\",\"name\":\"\",\"distance\":6,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":18,\"name_visible\":false}"));
            plugin.scheduler().runLaterSafe("command.test.hunt-figure-b", 45L,
                    () -> runTestBeat(engine, target, testSite, "hunt-figure-b", "named_mob",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"entity\":\"STRAY\",\"fallback_entity\":\"ZOMBIE\",\"name\":\"\",\"distance\":9,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":18,\"name_visible\":false}"));
            plugin.scheduler().runLaterSafe("command.test.hunt-close", 60L,
                    () -> runTestBeat(engine, target, testSite, "hunt-close", "private_sound",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"sound\":\"ENTITY_WARDEN_HEARTBEAT\",\"volume\":1.6,\"pitch\":0.5,\"behind\":true,\"offset\":1.5}"));
        }
        sender.sendMessage("Observance test: hunt -> " + target.getName()
                + " -> darkness, hostile close sounds, and two silent figures queued.");
    }

    private void handleElsewhereTest(CommandSender sender, Player target, Location anchor) {
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine unavailable.");
            return;
        }
        Site testSite = new Site("test_elsewhere", "test", anchor.getWorld().getName(),
                (double) anchor.getBlockX(), (double) anchor.getBlockY(), (double) anchor.getBlockZ(),
                16, 10, false, true, null);
        runTestBeat(engine, target, testSite, "elsewhere-sky", "private_time_shift",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"mode\":\"both\",\"time\":18000,\"weather\":\"DOWNFALL\",\"seconds\":16}");
        runTestBeat(engine, target, testSite, "elsewhere-dark", "private_darkness",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"effect\":\"DARKNESS\",\"seconds\":9,\"amplifier\":0}");
        runTestBeat(engine, target, testSite, "elsewhere-smoke", "private_particle",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"particle\":\"ASH\",\"count\":80,\"spread\":1.1,\"speed\":0.01,\"height\":1.0,\"near_player\":true,\"offset\":1.5}");
        runTestBeat(engine, target, testSite, "elsewhere-sound", "private_sound",
                com.observance.watcher.beats.BeatCategory.DIRECTED,
                "{\"sound\":\"AMBIENT_CAVE\",\"volume\":1.4,\"pitch\":0.45,\"behind\":true,\"offset\":2.0}");
        if (plugin.scheduler() != null) {
            plugin.scheduler().runLaterSafe("command.test.elsewhere.pressure", 18L,
                    () -> runTestBeat(engine, target, testSite, "elsewhere-pressure", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"actionbar\",\"text\":\"the sky is wearing the wrong room\"}"));
            plugin.scheduler().runLaterSafe("command.test.elsewhere-figure", 40L,
                    () -> runTestBeat(engine, target, testSite, "elsewhere-figure", "named_mob",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"entity\":\"WITHER_SKELETON\",\"fallback_entity\":\"STRAY\",\"name\":\"\",\"distance\":7,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":16,\"name_visible\":false,\"retreating\":true}"));
            plugin.scheduler().runLaterSafe("command.test.elsewhere-close", 58L,
                    () -> runTestBeat(engine, target, testSite, "elsewhere-close", "private_sound",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"sound\":\"ENTITY_ENDERMAN_TELEPORT\",\"volume\":1.2,\"pitch\":0.55,\"behind\":true,\"offset\":1.0}"));
        }
        sender.sendMessage("Observance test: elsewhere -> " + target.getName()
                + " -> wrong sky/weather, fog, ash, close sound, and a retreating figure queued.");
    }

    private void handleScareGauntlet(CommandSender sender, Player target) {
        if (target == null || !target.isOnline()) {
            sender.sendMessage("Observance: target player is not online.");
            return;
        }
        com.observance.watcher.beats.BeatEngine engine = plugin.beatEngine();
        if (engine == null || engine.context() == null || engine.library() == null) {
            sender.sendMessage("Observance: beat engine unavailable.");
            return;
        }
        sender.sendMessage("Observance test: gauntlet -> " + target.getName()
                + " -> elsewhere, stalking figure, and pursuit beats queued.");

        Location start = testAnchor(sender, target);
        if (start == null || start.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve a test location.");
            return;
        }
        handleElsewhereTest(sender, target, start);

        if (plugin.scheduler() == null) {
            handleStalkerTest(sender, target, start);
            handleHuntTest(sender, target, start);
            return;
        }

        plugin.scheduler().runLaterSafe("command.test.gauntlet.stalker", 90L, () -> {
            if (target.isOnline()) handleStalkerTest(sender, target, target.getLocation());
        });
        plugin.scheduler().runLaterSafe("command.test.gauntlet.hunt", 170L, () -> {
            if (target.isOnline()) handleHuntTest(sender, target, target.getLocation());
        });
        plugin.scheduler().runLaterSafe("command.test.gauntlet.after", 245L, () -> {
            if (!target.isOnline()) return;
            Location now = target.getLocation();
            if (now == null || now.getWorld() == null) return;
            Site site = new Site("test_gauntlet_after", "test", now.getWorld().getName(),
                    (double) now.getBlockX(), (double) now.getBlockY(), (double) now.getBlockZ(),
                    12, 8, false, true, null);
            runTestBeat(engine, target, site, "gauntlet-after", "private_message",
                    com.observance.watcher.beats.BeatCategory.DIRECTED,
                    "{\"mode\":\"actionbar\",\"text\":\"it learned the route you took\"}");
            runTestBeat(engine, target, site, "gauntlet-last-sound", "private_sound",
                    com.observance.watcher.beats.BeatCategory.DIRECTED,
                    "{\"sound\":\"ENTITY_WARDEN_HEARTBEAT\",\"volume\":1.1,\"pitch\":0.45,\"behind\":true,\"offset\":1.0}");
        });
    }

    /**
     * {@code /observance dreadpass [stage|run] [player]} - builds the scary rehearsal route and,
     * optionally, fires the full Watcher scare sequence through it. The route is a physical proof
     * surface: dark corridor, sculk floor, wrong signs, dead light, and named visit anchors.
     */
    private void handleDreadPass(CommandSender sender, String[] args) {
        boolean run = false;
        Player namedTarget = null;
        for (int i = 1; i < args.length; i++) {
            String raw = args[i] == null ? "" : args[i].trim();
            if (raw.isBlank()) continue;
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.equals("run") || lower.equals("fire") || lower.equals("start") || lower.equals("gauntlet")) {
                run = true;
            } else if (lower.equals("stage") || lower.equals("build") || lower.equals("place")) {
                run = false;
            } else {
                Player p = Bukkit.getPlayerExact(raw);
                if (p != null) namedTarget = p;
            }
        }

        Player anchorPlayer = sender instanceof Player self ? self : namedTarget;
        if (anchorPlayer == null) {
            sender.sendMessage("Observance: /observance dreadpass needs an in-game operator or an online player.");
            return;
        }
        Player target = namedTarget == null ? anchorPlayer : namedTarget;
        Location origin = anchorPlayer.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve the dreadpass location.");
            return;
        }

        int sites = buildDreadRoute(origin);
        sender.sendMessage("== Observance dread pass ==");
        sender.sendMessage("Dread route staged with " + sites + "/" + DREAD_PASS_SITES.length + " anchors.");
        sender.sendMessage("Walk it: /obs visit scare, then /obs visit next through the dread anchors.");
        sender.sendMessage("Run scare: /obs dreadpass run" + (target.equals(anchorPlayer) ? "" : " " + target.getName()) + ".");
        sender.sendMessage("Expected: wrong sky, ash, darkness, close sound, a retreating figure, then pursuit.");
        if (run) handleScareGauntlet(sender, target);
    }

    private int buildDreadRoute(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int bx = origin.getBlockX();
        int by = origin.getBlockY();
        int bz = origin.getBlockZ() + 8;

        for (int dz = 0; dz <= 30; dz++) {
            boolean narrow = dz >= 13 && dz <= 18;
            for (int dx = -3; dx <= 3; dx++) {
                int x = bx + dx;
                int z = bz + dz;
                boolean passage = Math.abs(dx) <= (narrow ? 1 : 2);
                boolean wall = !passage || Math.abs(dx) == (narrow ? 1 : 2);
                Material floor = (dz % 7 == 0 || (dz >= 18 && dx == 0))
                        ? Material.SCULK : ((dz % 5 == 0) ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE);
                if (passage) {
                    world.getBlockAt(x, by - 1, z).setType(floor, false);
                    if (Math.abs(dx) == (narrow ? 1 : 2) || (dx == 0 && dz % 4 == 0)) {
                        supportToGround(world, x, by - 1, z, Material.DEEPSLATE_BRICKS);
                    }
                }
                for (int dy = 0; dy <= 3; dy++) {
                    Block b = world.getBlockAt(x, by + dy, z);
                    if (wall) {
                        b.setType(dy == 3 ? Material.BLACKSTONE : dreadWallMaterial(dz, dy), false);
                    } else {
                        b.setType(Material.AIR, false);
                    }
                }
            }
            if (dz % 5 == 0) {
                placeDreadRib(world, bx, by, bz + dz);
            }
            if (dz % 6 == 0) {
                world.getBlockAt(bx - 1, by, bz + dz).setType(Material.SOUL_TORCH, false);
                world.getBlockAt(bx + 1, by, bz + dz).setType(Material.COBWEB, false);
            }
        }
        placeDreadGate(world, bx, by, bz);
        placeDreadElsewhereRoom(world, bx, by, bz + 10);
        placeDreadFigureNiche(world, bx, by, bz + 20);
        placeDreadGate(world, bx, by, bz + 29);

        Location start = new Location(world, bx, by, bz + 1);
        Location elsewhere = new Location(world, bx, by, bz + 10);
        Location figure = new Location(world, bx, by, bz + 20);
        Location exit = new Location(world, bx, by, bz + 29);
        placeDreadLabel(start.clone().add(0, 0, -1), new String[]{"the way is", "already", "listening", ""});
        placeDreadLabel(elsewhere.clone().add(0, 0, -1), new String[]{"the sky", "keeps the", "wrong room", ""});
        placeDreadLabel(figure.clone().add(0, 0, -1), new String[]{"the place", "behind you", "is occupied", ""});
        placeDreadLabel(exit.clone().add(0, 0, -1), new String[]{"count who", "left with", "their shadow", ""});
        world.getBlockAt(elsewhere.getBlockX(), elsewhere.getBlockY(), elsewhere.getBlockZ()).setType(Material.SCULK_SENSOR, false);
        world.getBlockAt(figure.getBlockX(), figure.getBlockY(), figure.getBlockZ()).setType(Material.REDSTONE_TORCH, false);
        world.getBlockAt(exit.getBlockX(), exit.getBlockY(), exit.getBlockZ()).setType(Material.SOUL_LANTERN, false);

        Location[] anchors = {start, elsewhere, figure, exit};
        int placed = 0;
        for (int i = 0; i < DREAD_PASS_SITES.length; i++) {
            Location loc = anchors[i];
            plugin.registerRuntimeSite(new Site(DREAD_PASS_SITES[i][0], DREAD_PASS_SITES[i][1], worldName,
                    (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                    5, 4, false, true, null));
            placed++;
        }
        return placed;
    }

    private Material dreadWallMaterial(int dz, int dy) {
        if (dy == 0 && dz % 7 == 0) return Material.SCULK;
        if ((dz + dy) % 5 == 0) return Material.CRACKED_DEEPSLATE_BRICKS;
        if ((dz + dy) % 3 == 0) return Material.DEEPSLATE_TILES;
        return Material.DEEPSLATE_BRICKS;
    }

    private void placeDreadRib(org.bukkit.World world, int bx, int by, int z) {
        for (int x = bx - 4; x <= bx + 4; x++) {
            world.getBlockAt(x, by + 3, z).setType(Material.BLACKSTONE, false);
        }
        for (int x = bx - 2; x <= bx + 2; x++) {
            world.getBlockAt(x, by + 2, z).setType(Material.IRON_CHAIN, false);
        }
        world.getBlockAt(bx - 4, by, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
        world.getBlockAt(bx + 4, by, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
    }

    private void placeDreadGate(org.bukkit.World world, int bx, int by, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by - 1, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
            world.getBlockAt(bx + dx, by + 4, z).setType(Material.BLACKSTONE, false);
        }
        for (int dx : new int[]{-3, 3}) {
            for (int dy = 0; dy <= 4; dy++) {
                world.getBlockAt(bx + dx, by + dy, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
            }
            supportToGround(world, bx + dx, by - 1, z, Material.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    private void placeDreadElsewhereRoom(org.bukkit.World world, int bx, int by, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 2;
                world.getBlockAt(bx + dx, by - 1, z + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.SCULK, false);
                if (rim) {
                    world.getBlockAt(bx + dx, by, z + dz).setType(Material.DEEPSLATE_BRICKS, false);
                    world.getBlockAt(bx + dx, by + 1, z + dz).setType(Material.CRACKED_DEEPSLATE_BRICKS, false);
                } else {
                    world.getBlockAt(bx + dx, by, z + dz).setType(Material.AIR, false);
                    world.getBlockAt(bx + dx, by + 1, z + dz).setType(Material.AIR, false);
                }
            }
        }
        world.getBlockAt(bx, by + 2, z).setType(Material.IRON_CHAIN, false);
        world.getBlockAt(bx, by + 3, z).setType(Material.SOUL_LANTERN, false);
    }

    private void placeDreadFigureNiche(org.bukkit.World world, int bx, int by, int z) {
        for (int dx = -3; dx <= -1; dx++) {
            world.getBlockAt(bx + dx, by - 1, z).setType(dx == -3 ? Material.BLACKSTONE : Material.SCULK, false);
            world.getBlockAt(bx + dx, by, z).setType(Material.AIR, false);
            world.getBlockAt(bx + dx, by + 1, z).setType(Material.AIR, false);
        }
        world.getBlockAt(bx - 4, by, z).setType(Material.BLACKSTONE, false);
        world.getBlockAt(bx - 4, by + 1, z).setType(Material.BLACKSTONE, false);
        world.getBlockAt(bx - 3, by, z).setType(Material.REDSTONE_TORCH, false);
    }

    private void placeDreadLabel(Location loc, String[] lines) {
        if (loc == null || loc.getWorld() == null) return;
        Block sign = loc.getBlock();
        sign.setType(Material.DARK_OAK_SIGN, false);
        if (sign.getBlockData() instanceof Rotatable r) {
            r.setRotation(BlockFace.SOUTH);
            sign.setBlockData(r, false);
        }
        setSignLines(sign, true, lines);
    }

    private void runTestBeat(
            com.observance.watcher.beats.BeatEngine engine,
            Player target,
            Site site,
            String beatId,
            String beatType,
            com.observance.watcher.beats.BeatCategory category,
            String payload) {
        com.observance.watcher.beats.Beat beat = engine.library().get(beatType);
        if (beat == null) return;
        com.observance.watcher.beats.BeatRequest req = new com.observance.watcher.beats.BeatRequest(
                "admin-test-" + beatId + "-" + System.nanoTime(), beatType, category,
                target, site, com.observance.watcher.beats.BeatPayload.parse(payload));
        safety.call("command.test." + beatId,
                () -> beat.enact(engine.context(), req),
                com.observance.watcher.beats.BeatResult.failed("threw"));
    }

    private Location testAnchor(CommandSender sender, Player target) {
        if (sender instanceof Player p && p.getWorld() != null) return p.getLocation();
        return target.getLocation();
    }

    private void prepareReflectionTest(Location anchor) {
        Location base = anchor.clone().add(0, -1, 0);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                base.clone().add(dx, -1, dz).getBlock().setType(Material.SMOOTH_STONE, false);
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                base.clone().add(dx, 0, dz).getBlock().setType(rim ? Material.POLISHED_DEEPSLATE : Material.WATER, false);
                base.clone().add(dx, 1, dz).getBlock().setType(Material.AIR, false);
            }
        }
    }

    private void prepareTorchTest(Location anchor) {
        Location base = anchor.clone().add(0, -1, 0);
        for (int i = -1; i <= 1; i++) {
            base.clone().add(i * 2, 0, 2).getBlock().setType(Material.STONE, false);
            base.clone().add(i * 2, 1, 2).getBlock().setType(Material.TORCH, false);
        }
    }

    private void prepareWallTest(Location anchor) {
        Location base = anchor.clone().add(0, -1, 3);
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                base.clone().add(x, y, 0).getBlock().setType(Material.SMOOTH_STONE, false);
            }
        }
    }

    private void handleLens(CommandSender sender, String[] args) {
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "give";
        if (!op.equals("give")) {
            sender.sendMessage("Usage: /observance lens give [player]");
            return;
        }
        Player target;
        if (args.length >= 3 && !args[2].isBlank()) {
            target = Bukkit.getPlayerExact(args[2].trim());
            if (target == null) {
                sender.sendMessage("Observance: player '" + args[2].trim() + "' not found / offline.");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("Observance: /observance lens give <player> (console must name a player).");
            return;
        }

        var lens = com.observance.watcher.lens.LensItem.create("observance");
        var leftover = target.getInventory().addItem(lens);
        if (!leftover.isEmpty()) {
            // Inventory full — drop at their feet rather than silently eating the relic.
            if (target.getLocation() != null && target.getLocation().getWorld() != null) {
                leftover.values().forEach(it ->
                        target.getLocation().getWorld().dropItemNaturally(target.getLocation(), it));
            }
        }
        // Reveal any already-placed gated runes if the Lens is now in hand.
        var listener = plugin.lensListener();
        if (listener != null) listener.refresh(target);

        sender.sendMessage("Observance: gave the Lens to " + target.getName() + ".");
        if (!target.equals(sender)) {
            target.sendMessage("Something is pressed into your pack. Hold it, and look again.");
        }
    }

    /**
     * {@code /observance wren <spawn|despawn|reckoning>} — places the one group-scoped companion NPC
     * (Wren) and, optionally, the three reckoning-choice markers.
     *
     * <ul>
     *   <li>{@code spawn} — spawn (or relocate) Wren at the sender's location. Uses Citizens2 when
     *       present (pass-as-human), else a PDC-tagged armor-stand fallback. Does NOT require a
     *       sites.yml entry.</li>
     *   <li>{@code despawn} — remove the current Wren body.</li>
     *   <li>{@code reckoning} — place three reckoning-choice marker armor stands (condemn / understand /
     *       free) in a short row at the sender's feet, each tagged so a right-click enters that one line
     *       into the record (post-reveal, once). This is the in-world CHOICE mechanism for M5.</li>
     * </ul>
     */
    private void handleWren(CommandSender sender, String[] args) {
        var wren = plugin.wren();
        if (wren == null) {
            sender.sendMessage("Observance: companion subsystem unavailable.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance wren must be run by a player (needs a location).");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        switch (op) {
            case "spawn" -> {
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    sender.sendMessage("Observance: could not resolve your location.");
                    return;
                }
                var body = wren.spawn(loc);
                if (body == null) {
                    sender.sendMessage("Observance: could not spawn Wren here (world/chunk unavailable?).");
                    return;
                }
                sender.sendMessage("Observance: Wren is here (" + wren.backend() + "). Right-click him to speak.");
            }
            case "despawn" -> {
                wren.despawn();
                sender.sendMessage("Observance: Wren has stepped out.");
            }
            case "reckoning" -> placeReckoningMarkers(player, sender);
            default -> sender.sendMessage("Usage: /observance wren <spawn|despawn|reckoning>");
        }
    }

    /**
     * {@code /observance keeper <spawn|despawn> [node]} — places the one group-scoped presiding-Keeper
     * NPC {@link com.observance.watcher.signal.listener.KeeperNpcListener} watches for.
     *
     * <ul>
     *   <li>{@code spawn [node]} — spawn (or relocate) the Keeper at the sender's location, standing at
     *       a rite-side site ({@code the_threshold} / {@code keeper_altar} in sites.yml — the listener
     *       ignores a click on him anywhere else). The optional {@code node} arg is stamped into his PDC
     *       marker as an entry-node hint for the showrunner's dossier branch; omitted/blank lets the
     *       showrunner decide purely from {@code arc_state.flags} + {@code punishment_state}. Uses
     *       Citizens2 when present (pass-as-human), else a PDC-tagged armor-stand fallback. Does NOT
     *       require a sites.yml entry to spawn — but he only OPENS when standing inside one.</li>
     *   <li>{@code despawn} — remove the current Keeper body.</li>
     * </ul>
     */
    /**
     * {@code /observance sidepass} - focused side/lore staging. It places every human-facing NPC surface
     * near the operator and prints the exact right-click checklist, so this lane can be rehearsed as one
     * concrete pass instead of three separate commands.
     */
    private void handleSidePass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance sidepass must be run by a player (needs a location).");
            return;
        }
        Location origin = player.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }

        Location row = origin.clone().add(0, 1, 4);
        int townsfolkTotal = com.observance.watcher.npc.TownsfolkNpc.TOWNSFOLK.size();
        sender.sendMessage("== Observance side/lore pass ==");

        if (plugin.townsfolk() != null) {
            int placed = plugin.townsfolk().spawnAll(row);
            sender.sendMessage("Townsfolk: " + placed + "/" + townsfolkTotal + " placed ("
                    + plugin.townsfolk().backend() + ").");
            sender.sendMessage("  Right-click: aro, wenna, coll, dob, old-pell.");
        } else {
            sender.sendMessage("Townsfolk: unavailable.");
        }

        if (plugin.wren() != null) {
            var body = plugin.wren().spawn(row.clone().add(12, 0, 0));
            sender.sendMessage("Wren: " + (body == null ? "not spawned" : "placed")
                    + " (" + plugin.wren().backend() + "). Right-click him several times.");
        } else {
            sender.sendMessage("Wren: unavailable.");
        }

        if (plugin.keeper() != null) {
            var body = plugin.keeper().spawn(row.clone().add(15, 0, 0), "sidepass");
            sender.sendMessage("Keeper: " + (body == null ? "not spawned" : "placed")
                    + " (" + plugin.keeper().backend() + "). Right-click to test the keeper lane.");
        } else {
            sender.sendMessage("Keeper: unavailable.");
        }

        int sideProofPlaced = placeSchoolStandProof(origin.clone().add(-24, 0, 72));
        sideProofPlaced += placeFarWaterProof(origin.clone().add(-24, 0, 54));
        sideProofPlaced += placeMarkersRowProof(origin.clone().add(-24, 0, 90));
        sideProofPlaced += placeCisternProof(origin.clone().add(-24, 0, 108));
        sideProofPlaced += placeWatchFloorProof(origin.clone().add(-24, 0, 126));
        sideProofPlaced += placeSetApartProof(origin.clone().add(-24, 0, 144));
        sideProofPlaced += placeUndercroftSealProof(origin.clone().add(-24, 0, 162));
        sideProofPlaced += placeForgottenMouthProof(origin.clone().add(-24, 0, 180));
        sideProofPlaced += placeDeepMarketProof(origin.clone().add(-24, 0, 18));
        sideProofPlaced += placeRationTableProof(origin.clone().add(-24, 0, 0));
        sideProofPlaced += placeThirdBayProof(origin.clone().add(-24, 0, -18));
        sideProofPlaced += placeWarmTownProof(origin.clone().add(-24, 0, 36));
        sender.sendMessage("Side-destination proof: " + sideProofPlaced
                + "/12 staged. School stand, far water, marker row, Cistern 7, watch-floor, set-apart shelf, undercroft seal, way-up, market, ration table, third bay, and Aro's warm-town lie should resolve to visible places.");
        sender.sendMessage("Checklist: right-click all five townsfolk, Wren, then the Keeper; watch chat/dialogue.");
        sender.sendMessage("Gates: /obs flag list, or /obs flag set companion_revealed for reckoning tests.");
        sender.sendMessage("Then run /obs coverage; side/lore should show all NPC bodies present.");
    }

    /**
     * {@code /observance puzzlepass [gates] [spacing]} - compact mechanical proof surface. It stages
     * the detector families that are easy to forget during a full ARG rehearsal, without needing the
     * operator to hand-place each individual site.
     */
    private void handlePuzzlePass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance puzzlepass must be run by a player (needs a location).");
            return;
        }
        Location origin = player.getLocation();
        if (origin == null || origin.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }

        int spacing = 12;
        boolean setGates = false;
        for (int i = 1; i < args.length; i++) {
            String raw = args[i] == null ? "" : args[i].trim().toLowerCase(Locale.ROOT);
            if (raw.isBlank()) continue;
            if (raw.equals("gates") || raw.equals("gate") || raw.equals("flags")) {
                setGates = true;
                continue;
            }
            try {
                spacing = Math.max(9, Math.min(18, Integer.parseInt(raw)));
            } catch (NumberFormatException ignored) { /* keep default */ }
        }

        org.bukkit.World world = origin.getWorld();
        String worldName = world.getName();
        int cols = 6;
        int platformRadius = 5;
        int placed = 0;
        int skipped = 0;

        sender.sendMessage("== Observance puzzle mechanic pass ==");
        for (int i = 0; i < PUZZLE_PASS_SITES.length; i++) {
            String[] row = PUZZLE_PASS_SITES[i];
            String id = row[0];
            String fallbackType = row[1];
            int fallbackRadius = parseSmallInt(row[2], 4);
            int fallbackVertical = parseSmallInt(row[3], 4);

            Site cfg = plugin.sites() == null ? null : plugin.sites().get(id);
            String type = cfg == null ? fallbackType : cfg.type();
            int radius = cfg == null ? fallbackRadius : cfg.radius();
            int vertical = cfg == null ? fallbackVertical : cfg.verticalRadius();
            boolean protect = cfg == null || cfg.protect();
            String puzzleKey = cfg == null ? null : cfg.puzzleKey();

            int col = i % cols;
            int gridRow = i / cols;
            Location base = new Location(world,
                    origin.getBlockX() + (col * spacing),
                    origin.getBlockY(),
                    origin.getBlockZ() + (gridRow * spacing));
            try {
                base.getChunk().load(true);
                clearLabCell(base, platformRadius, 8);
                buildLabPlatform(base, platformRadius);
                labelLabCell(base, id, type);

                Site live = new Site(id, type, worldName,
                        (double) base.getBlockX(), (double) base.getBlockY(), (double) base.getBlockZ(),
                        radius, vertical, protect, true, puzzleKey, false);
                buildLabFixture(live, base);
                plugin.registerRuntimeSite(live);
                repairPlacedSite(live, base);
                placed++;
            } catch (Throwable t) {
                skipped++;
                sender.sendMessage("  [!] puzzle fixture skipped " + id + " (" + t.getClass().getSimpleName() + ")");
            }
        }

        sender.sendMessage("Puzzlepass placed " + placed + "/" + PUZZLE_PASS_SITES.length
                + " fixtures" + (skipped > 0 ? " (" + skipped + " skipped)" : "") + ".");
        sender.sendMessage("Next: /obs runbook puzzle, then /obs coverage.");
        sender.sendMessage("Optional rehearsal shortcut: /obs puzzlepass gates.");
        sendPuzzlePassChecklist(sender);
        if (setGates) setPuzzlePassGates(sender);
    }

    private void sendPuzzlePassChecklist(CommandSender sender) {
        sender.sendMessage("Checklist:");
        sender.sendMessage("  1) bow/offering/sign: crouch at bow_marker_01; drop deepslate+cobbled deepslate at offering_cairn_01; type a known answer on answer_sign_01.");
        sender.sendMessage("  2) Vaun: put deepslate+cobbled deepslate in vaun_hoard_chest and close it; fill all six vaun_bookshelf slots.");
        sender.sendMessage("  3) Mara/Sella: lectern pages 1,2,4,4,6; bow at mara_map_marker; look down into sella_pool and from sella_anchor.");
        sender.sendMessage("  4) Orin/Brann: bow orin_marker_1..6; right-click a frame after all six are rotation 0; sneak start-to-end through Brann corridor.");
        sender.sendMessage("  5) Vault: after deep_gate_open, stand at threshold_vault and enter: v8k3 mq2n x6w1 t4d9 c7s5");
    }

    private void setPuzzlePassGates(CommandSender sender) {
        var sb = plugin.supabase();
        if (sb == null) {
            sender.sendMessage("Puzzlepass gates: supabase unavailable.");
            return;
        }
        String[] gates = {
                "vaun_cache_open",
                "mara_alcove_open",
                "sella_overlay_read",
                "orin_bowed",
                "brann_toll_heard",
                "iss_key_turned",
                "deep_gate_open",
                "threshold_open",
                "seventh_named",
                "bowed_as_one"
        };
        JsonObject flags = new JsonObject();
        for (String gate : gates) flags.addProperty(gate, true);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sb.mergeArcFlags(flags);
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage("Puzzlepass gates set for rehearsal: vaun/mara/sella/orin/brann/deep/vault/finale."));
        });
    }

    private static int parseSmallInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** Place or remove the group-scoped Keeper NPC used by the Keeper interaction listener. */
    private void handleKeeper(CommandSender sender, String[] args) {
        var keeper = plugin.keeper();
        if (keeper == null) {
            sender.sendMessage("Observance: keeper subsystem unavailable.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance keeper must be run by a player (needs a location).");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        switch (op) {
            case "spawn" -> {
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    sender.sendMessage("Observance: could not resolve your location.");
                    return;
                }
                String node = args.length > 2 ? args[2] : "";
                var body = keeper.spawn(loc, node);
                if (body == null) {
                    sender.sendMessage("Observance: could not spawn the Keeper here (world/chunk unavailable?).");
                    return;
                }
                sender.sendMessage("Observance: the Keeper is here (" + keeper.backend() + ")"
                        + (node.isBlank() ? "" : ", node=" + node)
                        + ". Right-click him at a rite-side site to open him.");
            }
            case "despawn" -> {
                keeper.despawn();
                sender.sendMessage("Observance: the Keeper has stepped back.");
            }
            default -> sender.sendMessage("Usage: /observance keeper <spawn|despawn> [node]");
        }
    }

    /**
     * Place the three reckoning markers in a short east-west row at the player's feet, each an
     * invulnerable, gravity-less armor stand named for its choice and tagged with the
     * {@code observance:wren_reckoning} PDC value the {@code WrenNpcListener} reads. A right-click on
     * one, after {@code companion_revealed}, sets exactly one of the reckoning flags, once.
     */
    /**
     * {@code /observance finale} — place the three FINALE-RITE markers at the player's feet (stand at the
     * Seventh's chamber, {@code the_unwriting}, and run this). Two Seventh-choice markers (restore/erase,
     * read by {@link com.observance.watcher.signal.listener.SeventhChoiceListener}) + the RELEASE marker
     * (read by {@link com.observance.watcher.signal.listener.ReleaseRiteListener}). Closes the "producer
     * built, no way to place its marker" gap for the WHOLE finale (both the Seventh choice and the release
     * were previously unstageable — no command spawned their markers). Each is an invulnerable, gravity-less,
     * persistent armor stand tagged with the PDC value its listener reads.
     */
    /**
     * {@code /observance reading} — carve the six SEVENTH READING fragments at the keeper sites
     * (design/THE-SEVENTH-READING.md). Each is a persistent {@link org.bukkit.entity.TextDisplay} at that
     * keeper's placed site: the letter-cipher fragments (Vaun/Sella/Orin/Brann) in the {@code
     * observance:runes} font (illegible without the pack, carved with it); Mara's book-cipher refs and
     * Iss's warm-prose acrostic in plain text (numbers + readable words, as their stones are). Read in
     * fall-order the six letters spell AVERYN; saying it triggers the release. Automates "carve the six
     * fragments" so it is one command, not hand-carving. Skips any keeper site not yet placed (a no-op).
     *
     * <p>The ciphertexts are the exact strings the discord capstone-integrity guard
     * ({@code seventh-reading.selftest}) verifies decode back to the confessions — kept in sync with
     * THE-SEVENTH-READING.md §3 (the carving spec; the discord {@code seventh-reading.ts} is the source).
     */
    private void handleReadingCarvings(CommandSender sender) {
        if (plugin.sites() == null) {
            sender.sendMessage("Observance: no sites loaded — run placeregion/placedeep first.");
            return;
        }
        final net.kyori.adventure.key.Key RUNES = net.kyori.adventure.key.Key.key("observance:runes");
        // { siteId, ciphertext (\n = line break), runeFont? }
        Object[][] fragments = {
            { "stone_vaun",  "D WKH ILUVW RI WKHLU QDPH L NHSW LW DQG JDYH QRQH EDFN", true },
            { "stone_mara",  "1-1-1  1-1-5  2-1-2  2-1-3  1-1-7  1-1-8  1-1-9  3-1-4  2-1-3  3-1-6  3-1-7", false },
            { "stone_sella", "V R PVKG RG ZG GSV UZI DZGVI", true },
            { "stone_orin",  "R I WOULD NOT BOW TO GIVE IT AND GIVE IT NOW", true },
            { "stone_brann", "YK LBHNI  ETI I YTEOEFRIPTT   E", true },
            { "stone_iss",   "i told you the last of it was m\ntake the first mark of each line down\n"
                             + "see what the warm words were laid over\nn is the letter i cut and called m", false },
        };
        var tagKey = new org.bukkit.NamespacedKey("observance", "reading_fragment");
        int placed = 0, skipped = 0;
        for (Object[] frag : fragments) {
            String siteId = (String) frag[0];
            String text = (String) frag[1];
            boolean runeFont = (Boolean) frag[2];
            com.observance.watcher.config.Site site = plugin.sites().get(siteId);
            if (site == null || !site.isPlaced() || site.location() == null) {
                sender.sendMessage("  · " + siteId + " — not placed yet, skipped.");
                skipped++;
                continue;
            }
            Location at = site.location().clone().add(0.5, 1.0, 0.5); // centered, at head height above the stone
            if (at.getWorld() == null
                    || !at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                sender.sendMessage("  · " + siteId + " — chunk not loaded, skipped.");
                skipped++;
                continue;
            }
            net.kyori.adventure.text.Component label = net.kyori.adventure.text.Component.text(text);
            if (runeFont) label = label.font(RUNES);
            final net.kyori.adventure.text.Component finalLabel = label;
            try {
                at.getWorld().spawn(at, org.bukkit.entity.TextDisplay.class, td -> {
                    td.text(finalLabel);
                    td.setPersistent(true);              // a world carving — survives restarts (visible to all)
                    // CENTER not FIXED: Site stores no yaw, so a FIXED display here has no real facing to
                    // carve at and could render edge-on/backward from a stone's approach side (capstone risk).
                    td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                    td.setSeeThrough(false);
                    td.setShadowed(true);
                    td.setDefaultBackground(false);
                    try { td.setBackgroundColor(org.bukkit.Color.fromARGB(0)); } catch (Throwable ignored) {}
                    td.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                    td.getPersistentDataContainer().set(tagKey,
                            org.bukkit.persistence.PersistentDataType.STRING, siteId);
                });
                placed++;
            } catch (Throwable t) {
                sender.sendMessage("  [!] could not carve at " + siteId + ".");
                skipped++;
            }
        }
        sender.sendMessage("Observance: THE SEVENTH READING — carved " + placed + "/6 fragments"
                + (skipped > 0 ? " (" + skipped + " skipped — place those keeper sites first)" : "")
                + ". Read in fall-order (vaun·mara·sella·orin·brann·iss) they spell the name; saying it ends it.");
    }

    private void handleFinaleMarkers(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance finale must be run by a player (needs a location).");
            return;
        }
        Location base = player.getLocation();
        if (base == null || base.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        var seventhKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.SeventhChoiceListener.PDC_SEVENTH_CHOICE);
        var releaseKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.ReleaseRiteListener.PDC_RELEASE);
        // { pdc-key-selector, pdc-value, label }
        Object[][] markers = {
            { seventhKey, "restore", "Restore — read the seventh's name back in" },
            { seventhKey, "erase",   "Erase — leave the blank a blank" },
            { releaseKey, "release", "Let it stop — close the record (the last act)" },
        };
        int placed = 0;
        for (int i = 0; i < markers.length; i++) {
            Location at = base.clone().add(i * 2.0, 0, 0);   // 2-block spacing, east
            try {
                var as = (org.bukkit.entity.ArmorStand)
                        at.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.ARMOR_STAND);
                as.customName(net.kyori.adventure.text.Component.text(
                        (String) markers[i][2], net.kyori.adventure.text.format.NamedTextColor.GRAY));
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setBasePlate(true);
                as.setInvulnerable(true);
                as.setPersistent(true);
                as.getPersistentDataContainer().set((org.bukkit.NamespacedKey) markers[i][0],
                        org.bukkit.persistence.PersistentDataType.STRING, (String) markers[i][1]);
                placed++;
            } catch (Throwable t) {
                sender.sendMessage("  [!] could not place finale marker '" + markers[i][1] + "'.");
            }
        }
        sender.sendMessage("Observance: placed " + placed + "/3 finale markers at your feet. "
                + "The Seventh-choice pair resolves after deep_gate_open; the release resolves after "
                + "bowed_as_one — each once. Stand these at the unwriting wall.");
    }

    private void placeReckoningMarkers(Player player, CommandSender sender) {
        Location base = player.getLocation();
        if (base == null || base.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return;
        }
        var key = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.WrenNpcListener.PDC_RECKONING);
        String[][] choices = {
            { "condemn",    "Condemn — write him as what his acts were" },
            { "understand", "Understand — write him whole, uncollapsed" },
            { "free",       "Free — let him go, unfed, on his own terms" },
        };
        int placed = 0;
        for (int i = 0; i < choices.length; i++) {
            Location at = base.clone().add(i * 2.0, 0, 0);   // 2-block spacing, east
            try {
                var as = (org.bukkit.entity.ArmorStand)
                        at.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.ARMOR_STAND);
                as.customName(net.kyori.adventure.text.Component.text(
                        choices[i][1], net.kyori.adventure.text.format.NamedTextColor.GRAY));
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setBasePlate(true);
                as.setInvulnerable(true);
                as.setPersistent(true);
                as.getPersistentDataContainer().set(key,
                        org.bukkit.persistence.PersistentDataType.STRING, choices[i][0]);
                placed++;
            } catch (Throwable t) {
                sender.sendMessage("  [!] could not place reckoning marker '" + choices[i][0] + "'.");
            }
        }
        sender.sendMessage("Observance: placed " + placed + "/3 reckoning markers. "
                + "They resolve only after companion_revealed, and only once.");
    }

    /**
     * {@code /observance townsfolk <spawn|despawn> [id]} — places (or removes) the five surface townsfolk
     * NPCs for the surface town. Mirrors {@link #handleWren}: uses Citizens2 when present (pass-as-human),
     * else PDC-tagged armor-stand fallbacks. Right-click any of them to hear their authored SET-A lines.
     *
     * <ul>
     *   <li>{@code spawn} — place all five in a short row at the sender's feet (or, with an {@code id},
     *       just that one: {@code aro|wenna|coll|dob|old-pell}). A re-spawn relocates rather than clones.</li>
     *   <li>{@code despawn} — remove all townsfolk bodies (or, with an {@code id}, just that one).</li>
     * </ul>
     */
    private void handleTownsfolk(CommandSender sender, String[] args) {
        var townsfolk = plugin.townsfolk();
        if (townsfolk == null) {
            sender.sendMessage("Observance: townsfolk subsystem unavailable.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance townsfolk must be run by a player (needs a location).");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        String id = args.length > 2 && !args[2].isBlank() ? args[2].trim().toLowerCase(Locale.ROOT) : null;
        if (id == null && com.observance.watcher.npc.TownsfolkNpc.byId(op) != null) {
            id = op;
            op = "spawn";
        }
        switch (op) {
            case "spawn" -> {
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    sender.sendMessage("Observance: could not resolve your location.");
                    return;
                }
                if (id != null) {
                    if (com.observance.watcher.npc.TownsfolkNpc.byId(id) == null) {
                        sender.sendMessage("Observance: unknown townsperson '" + id
                                + "'. One of: aro | wenna | coll | dob | old-pell.");
                        return;
                    }
                    var body = townsfolk.spawnOne(id, loc);
                    if (body == null) {
                        sender.sendMessage("Observance: could not spawn '" + id + "' here (world/chunk unavailable?).");
                        return;
                    }
                    sender.sendMessage("Observance: " + id + " is here (" + townsfolk.backend()
                            + "). Right-click to talk.");
                } else {
                    int placed = townsfolk.spawnAll(loc);
                    sender.sendMessage("Observance: placed " + placed + "/"
                            + com.observance.watcher.npc.TownsfolkNpc.TOWNSFOLK.size()
                            + " townsfolk (" + townsfolk.backend() + "). Right-click any to talk.");
                }
            }
            case "despawn" -> {
                if (id != null) {
                    if (com.observance.watcher.npc.TownsfolkNpc.byId(id) == null) {
                        sender.sendMessage("Observance: unknown townsperson '" + id
                                + "'. One of: aro | wenna | coll | dob | old-pell.");
                        return;
                    }
                    int n = townsfolk.despawnOne(id);
                    sender.sendMessage("Observance: removed " + n + " body/bodies for '" + id + "'.");
                } else {
                    int n = townsfolk.despawnAll();
                    sender.sendMessage("Observance: the townsfolk have gone in (" + n + " removed).");
                }
            }
            default -> sender.sendMessage("Usage: /observance townsfolk <spawn|despawn> [aro|wenna|coll|dob|old-pell]");
        }
    }

    private void handleAudit(CommandSender sender) {
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance audit: no sites loaded. Run /obs reload or check sites.yml.");
            return;
        }

        int enabled = 0;
        int placed = 0;
        int unplaced = 0;
        int ok = 0;
        int failed = 0;
        int coreMissing = 0;
        List<String> issues = new ArrayList<>();

        for (Site site : plugin.sites().all()) {
            if (site == null || !site.enabled()) continue;
            enabled++;
            if (!site.isPlaced()) {
                unplaced++;
                if (isCoreAuditSite(site.id())) {
                    coreMissing++;
                    addAuditIssue(issues, "MISSING core site '" + site.id() + "' is not placed.");
                }
                continue;
            }

            Location loc = site.location();
            if (loc == null || loc.getWorld() == null) {
                failed++;
                addAuditIssue(issues, "FAIL " + site.id() + ": world not loaded or location unresolved.");
                continue;
            }

            placed++;
            String issue = auditPlacedSite(site, loc);
            if (issue == null) {
                ok++;
            } else {
                failed++;
                addAuditIssue(issues, issue);
            }
        }

        sender.sendMessage("== Observance readiness audit ==");
        sender.sendMessage(" enabled sites: " + enabled);
        sender.sendMessage(" placed sites:  " + placed);
        sender.sendMessage(" unplaced:      " + unplaced + " (" + coreMissing + " core)");
        sender.sendMessage(" hardware ok:   " + ok);
        sender.sendMessage(" failures:      " + failed);
        if (issues.isEmpty()) {
            sender.sendMessage(" Result: no obvious placement hardware problems found.");
            sender.sendMessage(" Next: run /obs test stalker and /obs test hunt for scare checks.");
            return;
        }
        sender.sendMessage(" Issues:");
        for (String issue : issues) {
            sender.sendMessage("  - " + issue);
        }
        if (issues.size() >= 12) {
            sender.sendMessage("  - ...showing first 12 issues only.");
        }
        sender.sendMessage(" Repair: /obs placehold build/audit for the production Deep Hold, /obs prepworld for compact rehearsal, or /obs site set <id> + /obs placeworld for bespoke placement.");
    }

    private void handlePreflight(CommandSender sender) {
        sender.sendMessage("== Observance preflight ==");
        handleAudit(sender);
        handleVisualAudit(sender);
        handleDialogueAudit(sender);
        handleCoverage(sender);
        sender.sendMessage("Preflight rule: hardware green is not enough; visualaudit must not report REPLACE issues, and NPC claims must have world/mechanic proof before live placement.");
        sender.sendMessage("Setup rule: placehold is the production Deep Hold shortcut; prepworld/fullrun are rehearsal surfaces; bespoke scatter still uses site set + placeworld.");
    }

    private void handleDialogueAudit(CommandSender sender) {
        sender.sendMessage("== Observance dialogue-world audit ==");
        sender.sendMessage("Rule: NPC factual claims are contracts. Places, routes, marks, rules, and consequences must exist in-world.");
        sender.sendMessage("For each risky NPC line, prove:");
        sender.sendMessage("  1) the route/landmark is physically findable from where the player hears it;");
        sender.sendMessage("  2) the named object or mark exists with a clear visual identity;");
        sender.sendMessage("  3) doing the implied action matters through a flag, beat, puzzle, scare, reward, or rewrite;");
        sender.sendMessage("  4) side hints lead to payoff, not flavor that players can safely ignore.");
        sender.sendMessage("High-risk current checks: school stand, far water, marker row, Cistern 7, watch-floor, set-apart entry 5, undercroft seal, forgotten way-up, the big Stair, painted line, lamp-house/Lamp-works, third lamp stand, dead-stall, bird coops, Deep Market, ration table, third bay, warm-town lie, bowing stones, and black-moon sleep rule.");
        sender.sendMessage("Example: if an NPC says to cross a painted line down the stairs, there must be stairs, a visible line, and a consequence for crossing it.");
        sender.sendMessage("Tool: /obs descentproof stages the Stair proof chain plus empty bird coops; /obs prepworld or sidepass stages side-destination proof sites.");
        sender.sendMessage("Doc: design/DIALOGUE-WORLD-AUDIT.md");
    }

    private void handleVisualAudit(CommandSender sender) {
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance visualaudit: no sites loaded. Run /obs reload or check sites.yml.");
            return;
        }

        int checked = 0;
        int passed = 0;
        int reshape = 0;
        int replace = 0;
        int skipped = 0;
        List<String> issues = new ArrayList<>();

        for (Site site : plugin.sites().all()) {
            if (site == null || !site.enabled() || !site.isPlaced()) {
                skipped++;
                continue;
            }
            if (!isVisualAuditSite(site)) {
                skipped++;
                continue;
            }
            Location loc = site.location();
            if (loc == null || loc.getWorld() == null) {
                skipped++;
                continue;
            }
            checked++;
            String issue = visualIssue(site, loc);
            if (issue == null) {
                passed++;
            } else {
                if (issue.startsWith("REPLACE")) replace++;
                else reshape++;
                addAuditIssue(issues, issue);
            }
        }

        sender.sendMessage("== Observance visual audit ==");
        sender.sendMessage(" checked:  " + checked);
        sender.sendMessage(" keep:     " + passed);
        sender.sendMessage(" reshape:  " + reshape);
        sender.sendMessage(" replace:  " + replace);
        sender.sendMessage(" skipped:  " + skipped + " (minor hardware/unplaced/disabled)");
        if (issues.isEmpty()) {
            sender.sendMessage(" Result: no obvious test-prop visual failures found.");
            sender.sendMessage(" Next: still do a human screenshot pass using design/VISUAL-RESCUE.md.");
            return;
        }
        sender.sendMessage(" Visual issues:");
        for (String issue : issues) sender.sendMessage("  - " + issue);
        if (issues.size() >= 12) sender.sendMessage("  - ...showing first 12 visual issues only.");
        sender.sendMessage(" Rule: REPLACE means the physical form is too weak for launch; RESHAPE means rebuild/scale/light before live placement.");
    }

    private String visualIssue(Site site, Location loc) {
        VisualScan scan = scanVisualSite(loc, isMajorVisualSite(site) ? 6 : 4, isMajorVisualSite(site) ? 5 : 3);
        boolean major = isMajorVisualSite(site);
        List<String> notes = new ArrayList<>();
        if (scan.nonAir < (major ? 24 : 8)) notes.add("too little built form");
        if (scan.footprint < (major ? 10 : 4)) notes.add("tiny footprint");
        if (scan.maxDy < (major ? 3 : 1)) notes.add("flat/no silhouette");
        if (scan.materials < (major ? 4 : 2)) notes.add("low material variety");
        if (major && needsDeepHoldPalette(site) && scan.deepHoldMaterials == 0) notes.add("no Deep Hold palette anchor");
        if (major && scan.paletteClashes > Math.max(4, scan.nonAir / 5)) notes.add("palette feels non-cohesive");
        if (major && scan.signs > 4) notes.add("too many signs / reads like labels");
        if (needsVisualLight(site) && !scan.hasLight) notes.add("no intentional light");
        if (needsAnswerSurface(site) && scan.editableSigns == 0) notes.add("no editable answer surface");
        if (needsFocalObject(site) && scan.focalObjects < 1) notes.add("no focal object to inspect");
        if (needsRouteShape(site) && Math.max(scan.spanX, scan.spanZ) < 6) notes.add("no route shape / approach vector");
        if (needsGatherableBodySpace(site) && scan.bodySpace < 6) notes.add("no gatherable body space");
        if ("painted_line".equals(site.type()) && scan.lineBlocks < 3) notes.add("painted line is not visibly crossable");
        if ("dread_route".equals(site.type()) && (Math.max(scan.spanX, scan.spanZ) < 5 || scan.bodySpace < 4)) {
            notes.add("dread beat has no sightline or exit space");
        }
        if (scan.operatorLabels > 0) notes.add("operator/test labels visible");
        if (notes.isEmpty()) return null;
        String severity = (major && (scan.nonAir < 12 || scan.footprint < 6 || scan.operatorLabels > 0
                || (needsAnswerSurface(site) && scan.editableSigns == 0)
                || (needsGatherableBodySpace(site) && scan.bodySpace < 3)
                || ("painted_line".equals(site.type()) && scan.lineBlocks < 2)))
                ? "REPLACE " : "RESHAPE ";
        return severity + site.id() + ": " + String.join("; ", notes) + ".";
    }

    private VisualScan scanVisualSite(Location loc, int radius, int vertical) {
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(2, Math.min(8, radius));
        int v = Math.max(2, Math.min(6, vertical));
        Set<Material> materials = new HashSet<>();
        Set<String> footprint = new HashSet<>();
        int nonAir = 0;
        int maxDy = 0;
        boolean hasLight = false;
        int signs = 0;
        int editableSigns = 0;
        int operatorLabels = 0;
        int deepHoldMaterials = 0;
        int paletteClashes = 0;
        int focalObjects = 0;
        int lineBlocks = 0;
        int bodySpace = 0;
        int minDx = Integer.MAX_VALUE;
        int maxDx = Integer.MIN_VALUE;
        int minDz = Integer.MAX_VALUE;
        int maxDz = Integer.MIN_VALUE;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((dx * dx) + (dz * dz) > r * r) continue;
                Block floor = world.getBlockAt(bx + dx, by - 1, bz + dz);
                Block feet = world.getBlockAt(bx + dx, by, bz + dz);
                Block head = world.getBlockAt(bx + dx, by + 1, bz + dz);
                if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                    bodySpace++;
                }
                for (int dy = -1; dy <= v; dy++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    Material type = b.getType();
                    if (type.isAir()) continue;
                    nonAir++;
                    maxDy = Math.max(maxDy, dy);
                    minDx = Math.min(minDx, dx);
                    maxDx = Math.max(maxDx, dx);
                    minDz = Math.min(minDz, dz);
                    maxDz = Math.max(maxDz, dz);
                    materials.add(type);
                    footprint.add(dx + "," + dz);
                    if (isDeepHoldPalette(type)) deepHoldMaterials++;
                    if (isPaletteClash(type)) paletteClashes++;
                    if (isVisualLight(type)) hasLight = true;
                    if (isFocalObject(type)) focalObjects++;
                    if (isLineMaterial(type) && Math.abs(dy) <= 1) lineBlocks++;
                    if (b.getState() instanceof Sign sign) {
                        signs++;
                        if (!sign.isWaxed()) editableSigns++;
                        focalObjects++;
                        if (hasOperatorLabel(sign)) operatorLabels++;
                    }
                }
            }
        }
        int spanX = nonAir == 0 ? 0 : (maxDx - minDx + 1);
        int spanZ = nonAir == 0 ? 0 : (maxDz - minDz + 1);
        return new VisualScan(nonAir, footprint.size(), materials.size(), maxDy,
                hasLight, signs, editableSigns, operatorLabels, deepHoldMaterials, paletteClashes,
                spanX, spanZ, bodySpace, focalObjects, lineBlocks);
    }

    private record VisualScan(int nonAir, int footprint, int materials, int maxDy,
                              boolean hasLight, int signs, int editableSigns, int operatorLabels,
                              int deepHoldMaterials, int paletteClashes,
                              int spanX, int spanZ, int bodySpace, int focalObjects,
                              int lineBlocks) { }

    private static boolean isVisualAuditSite(Site site) {
        if (site == null) return false;
        String id = site.id();
        String type = site.type();
        return isCoreAuditSite(id)
                || isLaunchRequiredSite(id)
                || "dread_route".equals(type)
                || "answer_sign".equals(type)
                || "keeper_stone".equals(type)
                || "structure".equals(type)
                || "marker".equals(type)
                || "accepting_floor".equals(type)
                || "seventh_shrine".equals(type)
                || "the_threshold".equals(type)
                || "keeper_altar".equals(type)
                || "coop_plate".equals(type)
                || "lampworks_stair".equals(type)
                || "lamp_stand".equals(type)
                || "painted_line".equals(type)
                || "dead_stall".equals(type)
                || "far_water".equals(type)
                || "bird_coops".equals(type)
                || "school_stand".equals(type)
                || "markers_row".equals(type)
                || "cistern_7".equals(type)
                || "watch_floor".equals(type)
                || "set_apart_shelf".equals(type)
                || "undercroft_seal".equals(type)
                || "forgotten_mouth".equals(type)
                || "deep_market".equals(type)
                || "ration_table".equals(type)
                || "third_bay_breach".equals(type)
                || "warm_town_collapse".equals(type);
    }

    private static boolean isMajorVisualSite(Site site) {
        if (site == null || site.id() == null) return false;
        String id = site.id();
        String type = site.type();
        return isLaunchRequiredSite(id)
                || id.startsWith("stone_")
                || id.equals("rune_rosetta")
                || id.equals("stone_of_reckoning")
                || id.equals("the_threshold")
                || id.equals("the_cold_hearth")
                || id.equals("unbroken_light")
                || id.equals("the_unwriting")
                || id.equals("threshold_vault")
                || id.equals("nether_forge")
                || id.equals("end_seventh_shrine")
                || id.equals("lampworks_stair")
                || id.equals("dead_stall")
                || id.equals("the_far_water")
                || id.equals("school_stand")
                || id.equals("markers_row")
                || id.equals("cistern_7")
                || id.equals("watch_floor")
                || id.equals("set_apart_shelf")
                || id.equals("undercroft_seal")
                || id.equals("forgotten_mouth")
                || id.equals("deep_bird_coops")
                || id.equals("deep_market")
                || id.equals("ration_table")
                || id.equals("third_bay_breach")
                || id.equals("warm_town_collapse")
                || "dread_route".equals(type)
                || "accepting_floor".equals(type)
                || "seventh_shrine".equals(type)
                || "lampworks_stair".equals(type)
                || "dead_stall".equals(type)
                || "far_water".equals(type)
                || "bird_coops".equals(type)
                || "school_stand".equals(type)
                || "markers_row".equals(type)
                || "cistern_7".equals(type)
                || "watch_floor".equals(type)
                || "set_apart_shelf".equals(type)
                || "undercroft_seal".equals(type)
                || "forgotten_mouth".equals(type)
                || "deep_market".equals(type)
                || "ration_table".equals(type)
                || "third_bay_breach".equals(type)
                || "warm_town_collapse".equals(type);
    }

    private static boolean needsAnswerSurface(Site site) {
        if (site == null) return false;
        String type = site.type();
        return "answer_sign".equals(type)
                || "keeper_stone".equals(type)
                || "case_board".equals(type)
                || "prior_camp".equals(type)
                || "failed_accepting".equals(type);
    }

    private static boolean needsVisualLight(Site site) {
        if (site == null) return false;
        String type = site.type();
        return isMajorVisualSite(site)
                || "report_lectern".equals(type)
                || "mara_lectern".equals(type)
                || "dread_route".equals(type)
                || "lampworks_stair".equals(type)
                || "lamp_stand".equals(type)
                || "dead_stall".equals(type)
                || "far_water".equals(type)
                || "bird_coops".equals(type)
                || "school_stand".equals(type)
                || "markers_row".equals(type)
                || "cistern_7".equals(type)
                || "watch_floor".equals(type)
                || "set_apart_shelf".equals(type)
                || "undercroft_seal".equals(type)
                || "forgotten_mouth".equals(type)
                || "deep_market".equals(type)
                || "ration_table".equals(type)
                || "third_bay_breach".equals(type)
                || "warm_town_collapse".equals(type);
    }

    private static boolean needsDeepHoldPalette(Site site) {
        if (site == null) return false;
        String id = site.id();
        String type = site.type();
        if ("first_report_lectern_01".equals(id)
                || "first_marker_01".equals(id)
                || "report_lectern".equals(type)
                || "mara_lectern".equals(type)) {
            return false;
        }
        return isMajorVisualSite(site);
    }

    private static boolean needsFocalObject(Site site) {
        if (site == null) return false;
        String type = site.type();
        return isMajorVisualSite(site)
                || "report_lectern".equals(type)
                || "mara_lectern".equals(type)
                || "vaun_hoard_chest".equals(type)
                || "vaun_bookshelf".equals(type);
    }

    private static boolean needsRouteShape(Site site) {
        if (site == null) return false;
        String type = site.type();
        return "lampworks_stair".equals(type)
                || "forgotten_mouth".equals(type)
                || "deep_market".equals(type)
                || "third_bay_breach".equals(type)
                || "warm_town_collapse".equals(type)
                || "dread_route".equals(type);
    }

    private static boolean needsGatherableBodySpace(Site site) {
        if (site == null) return false;
        String type = site.type();
        return "accepting_floor".equals(type)
                || "keeper_altar".equals(type)
                || "coop_plate".equals(type)
                || "the_threshold".equals(type)
                || "seventh_shrine".equals(type)
                || "dread_route".equals(type);
    }

    private static boolean isVisualLight(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.contains("TORCH")
                || n.contains("LANTERN")
                || n.contains("CANDLE")
                || n.contains("CAMPFIRE")
                || n.contains("FIRE")
                || n.contains("GLOWSTONE")
                || n.contains("SEA_LANTERN")
                || n.contains("SHROOMLIGHT")
                || n.contains("FROGLIGHT")
                || n.equals("BEACON")
                || n.equals("END_ROD")
                || n.equals("LIGHT");
    }

    private static boolean isFocalObject(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.contains("SIGN")
                || n.contains("LECTERN")
                || n.contains("BOOKSHELF")
                || n.contains("CHEST")
                || n.contains("BARREL")
                || n.contains("CAULDRON")
                || n.contains("BELL")
                || n.contains("CANDLE")
                || n.contains("LANTERN")
                || n.contains("CAMPFIRE")
                || n.contains("CHAIN")
                || n.contains("BARS")
                || n.contains("SKULL")
                || n.contains("HEAD")
                || n.equals("LODESTONE")
                || n.equals("RESPAWN_ANCHOR")
                || n.equals("BEACON")
                || n.equals("CHISELED_BOOKSHELF")
                || n.equals("CALIBRATED_SCULK_SENSOR");
    }

    private static boolean isLineMaterial(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.equals("BLACK_CONCRETE")
                || n.equals("BLACK_CARPET")
                || n.equals("BLACKSTONE")
                || n.equals("POLISHED_BLACKSTONE")
                || n.equals("POLISHED_BLACKSTONE_SLAB")
                || n.equals("POLISHED_BLACKSTONE_PRESSURE_PLATE")
                || n.equals("DEEPSLATE_TILE_SLAB")
                || n.equals("DEEPSLATE_BRICK_SLAB");
    }

    private static boolean isDeepHoldPalette(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.contains("DEEPSLATE")
                || n.contains("BLACKSTONE")
                || n.contains("BASALT")
                || n.contains("TUFF")
                || n.contains("COPPER")
                || n.contains("SCULK")
                || n.contains("SOUL")
                || n.contains("PRISMARINE")
                || n.contains("END_STONE")
                || n.equals("OBSIDIAN")
                || n.equals("CRYING_OBSIDIAN")
                || n.equals("BEDROCK");
    }

    private static boolean isPaletteClash(Material type) {
        if (type == null) return false;
        String n = type.name();
        if (n.contains("REDSTONE")) return false;
        if (n.equals("BLACK_CONCRETE") || n.equals("GRAY_CONCRETE") || n.equals("LIGHT_GRAY_CONCRETE")
                || n.equals("BROWN_CARPET") || n.equals("GRAY_CARPET") || n.equals("BLACK_CANDLE")) return false;
        if (n.equals("GRASS_BLOCK") || n.equals("OAK_LEAVES") || n.equals("HAY_BLOCK")
                || n.equals("CAKE") || n.equals("WATER")) return false;
        return n.contains("WOOL")
                || n.contains("TERRACOTTA")
                || n.contains("GLAZED")
                || (n.contains("CONCRETE") && !n.contains("BLACK") && !n.contains("GRAY") && !n.contains("BROWN"))
                || n.equals("DIAMOND_BLOCK")
                || n.equals("GOLD_BLOCK")
                || n.equals("EMERALD_BLOCK")
                || n.equals("LAPIS_BLOCK")
                || n.equals("NETHERITE_BLOCK");
    }

    private static boolean hasOperatorLabel(Sign sign) {
        if (sign == null) return false;
        for (int i = 0; i < 4; i++) {
            String line = sign.getLine(i);
            if (line == null) continue;
            String s = line.toLowerCase(Locale.ROOT);
            if (s.contains("dread route")
                    || s.contains("walk slowly")
                    || s.contains("sound on")
                    || s.equals("figure")
                    || s.equals("exit")
                    || s.contains("look back once")
                    || s.contains("then move")
                    || s.contains("test")) {
                return true;
            }
        }
        return false;
    }

    private void handleRepair(CommandSender sender) {
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance repair: no sites loaded. Run /obs reload or check sites.yml.");
            return;
        }
        int repaired = 0;
        int skipped = 0;
        List<String> notes = new ArrayList<>();

        for (Site site : plugin.sites().all()) {
            if (site == null || !site.enabled() || !site.isPlaced()) {
                skipped++;
                continue;
            }
            Location loc = site.location();
            if (loc == null || loc.getWorld() == null) {
                skipped++;
                continue;
            }
            boolean changed = repairPlacedSite(site, loc);
            if (changed) {
                repaired++;
                addAuditIssue(notes, site.id() + " repaired");
            }
        }

        sender.sendMessage("== Observance repair ==");
        sender.sendMessage(" repaired: " + repaired);
        sender.sendMessage(" skipped:  " + skipped + " (unplaced/unloaded/disabled)");
        if (!notes.isEmpty()) {
            sender.sendMessage(" Repairs:");
            for (String note : notes) sender.sendMessage("  - " + note);
            if (notes.size() >= 12) sender.sendMessage("  - ...showing first 12 repairs only.");
        }
        sender.sendMessage(" Next: run /obs audit again.");
    }

    private boolean repairPlacedSite(Site site, Location loc) {
        String type = site.type();
        Block block = loc.getBlock();
        boolean changed = removeRetiredBeaconNear(loc);
        if ("report_lectern".equals(type) || "first_report_lectern_01".equals(site.id())) {
            fillPrologueLecternBook(loc);
            return true;
        }
        if ("mara_lectern".equals(type)) {
            int index = trailingIndex(site.id(), 1);
            int[] markedPages = {1, 2, 4, 4, 6};
            int marked = markedPages[Math.max(0, Math.min(markedPages.length - 1, index - 1))];
            placeReadableLectern(block, BlockFace.SOUTH);
            fillMaraLockBook(block, index, marked);
            return true;
        }
        if (needsAnswerSurface(site) && !hasEditableSignNear(loc, Math.max(1, site.radius()))) {
            HoldSite answerHoldRow = deepHoldSiteById(site.id());
            if (answerHoldRow != null) {
                placeHoldFixture(site, loc, answerHoldRow);
            } else {
                placeAnswerSign(loc);
            }
            return true;
        }
        HoldSite holdRow = deepHoldSiteById(site.id());
        if (holdRow != null) {
            placeHoldFixture(site, loc, holdRow);
            return true;
        }
        if ("lampworks_stair".equals(type)) {
            buildLampworksStair(loc);
            return true;
        }
        if ("lamp_stand".equals(type)) {
            placeLampStand(loc, 3, false);
            return true;
        }
        if ("painted_line".equals(type)) {
            placePaintedLineFixture(loc);
            return true;
        }
        if ("dead_stall".equals(type)) {
            buildDeadStall(loc);
            return true;
        }
        if ("far_water".equals(type)) {
            placeFarWater(loc);
            return true;
        }
        if ("bird_coops".equals(type)) {
            buildBirdCoops(loc);
            return true;
        }
        if ("school_stand".equals(type)) {
            buildSchoolStand(loc);
            return true;
        }
        if ("markers_row".equals(type)) {
            buildMarkersRow(loc);
            return true;
        }
        if ("cistern_7".equals(type)) {
            buildCisternSeven(loc);
            return true;
        }
        if ("watch_floor".equals(type)) {
            buildWatchFloor(loc);
            return true;
        }
        if ("set_apart_shelf".equals(type)) {
            buildSetApartShelf(loc);
            return true;
        }
        if ("undercroft_seal".equals(type)) {
            buildUndercroftSeal(loc);
            return true;
        }
        if ("forgotten_mouth".equals(type)) {
            buildForgottenMouth(loc);
            return true;
        }
        if ("deep_market".equals(type)) {
            buildDeepMarket(loc);
            return true;
        }
        if ("ration_table".equals(type)) {
            buildRationTable(loc);
            return true;
        }
        if ("third_bay_breach".equals(type)) {
            buildThirdBayBreach(loc);
            return true;
        }
        if ("warm_town_collapse".equals(type)) {
            buildWarmTownCollapse(loc);
            return true;
        }
        if ("vaun_bookshelf".equals(type) && block.getType() != Material.CHISELED_BOOKSHELF) {
            placeMechanicBookshelf(block);
            return true;
        }
        if ("vaun_hoard_chest".equals(type)
                && block.getType() != Material.CHEST
                && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.BARREL) {
            placeChest(loc);
            return true;
        }
        if (isTemplateLabSite(site.id()) && (block.getType() == Material.AIR
                || block.getType() == Material.CAVE_AIR
                || block.getType() == Material.VOID_AIR)) {
            boolean built = StructureTemplates.keeper(site.id(), loc) != null;
            ensureAuditAnchor(site.id(), loc);
            Material fixed = loc.getBlock().getType();
            return built || (fixed != Material.AIR && fixed != Material.CAVE_AIR && fixed != Material.VOID_AIR);
        }
        if ("first_marker_01".equals(site.id()) && block.getType() == Material.AIR) {
            placeMarker(loc, Material.CHISELED_STONE_BRICKS, Material.CANDLE, true);
            return true;
        }
        return changed;
    }

    private String auditPlacedSite(Site site, Location loc) {
        Block block = loc.getBlock();
        String type = site.type();
        if (hasMaterialNear(loc, Math.max(3, site.radius()), Material.BEACON)) {
            return "FAIL " + site.id() + ": retired beacon block still present near site; run /observance repair.";
        }
        if ("report_lectern".equals(type) || "mara_lectern".equals(type)) {
            if (block.getType() != Material.LECTERN) {
                return "FAIL " + site.id() + ": expected a lectern, found " + block.getType() + ".";
            }
            if (!(block.getState() instanceof Lectern lectern)) {
                return "FAIL " + site.id() + ": lectern state did not load.";
            }
            ItemStack book = lectern.getInventory().getItem(0);
            if (book == null || book.getType() != Material.WRITTEN_BOOK) {
                return "FAIL " + site.id() + ": lectern has no written book.";
            }
            return null;
        }
        if ("vaun_bookshelf".equals(type) && block.getType() != Material.CHISELED_BOOKSHELF) {
            return "FAIL " + site.id() + ": expected chiseled bookshelf, found " + block.getType() + ".";
        }
        if ("vaun_bookshelf".equals(type) && !isMechanicBookshelfEmpty(block)) {
            return "FAIL " + site.id() + ": Vaun tally shelf is pre-filled; mechanic-owned shelf must start empty.";
        }
        if ("orin_frame_dial".equals(type) && !hasItemFrameNear(loc, 2.0)) {
            return "FAIL " + site.id() + ": expected an item-frame dial entity with an arrow.";
        }
        if ("rune_rosetta".equals(site.id())
                && (block.getType() != Material.CHISELED_TUFF
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAY_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected Hold-native Rosetta stones, grey seventh mark, and record lectern.";
        }
        if ("stone_of_reckoning".equals(site.id())
                && (block.getType() != Material.CHISELED_TUFF
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected Hold-native reckoning line, judgement marks, and record lectern.";
        }
        if ("the_threshold".equals(site.id())
                && (block.getType() != Material.REINFORCED_DEEPSLATE
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SCULK)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected named threshold slab, sculk grave line, and threshold note.";
        }
        if ("threshold_vault".equals(site.id())
                && (block.getType() != Material.STONE_PRESSURE_PLATE
                || !hasSignNear(loc, Math.max(3, site.radius()))
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_TUFF))) {
            return "FAIL " + site.id() + ": expected vault pressure plate, editable combination sign, and four hand marks.";
        }
        if ("unbroken_light".equals(site.id())
                && (block.getType() != Material.SEA_LANTERN
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected group-sized Accepting floor with unbroken-light anchor and record lectern.";
        }
        if ("case_board".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasSignNear(loc, Math.max(3, site.radius()))
                || !hasEditableSignNear(loc, Math.max(3, site.radius()))
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF))) {
            return "FAIL " + site.id() + ": expected case board lecterns, filed storage, shelves, proof-category signs, and editable filing sign.";
        }
        if ("prior_camp".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CAMPFIRE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LIGHT_GRAY_CARPET)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAY_CANDLE)
                || countEditableSignsNear(loc, Math.max(3, site.radius())) < 7)) {
            return "FAIL " + site.id() + ": expected prior-run campfire, blank witness place, bedroll packets, correction barrels, records, and seven editable filing signs.";
        }
        if ("failed_accepting".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_TUFF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CANDLE)
                || !hasEditableSignNear(loc, Math.max(3, site.radius())))) {
            return "FAIL " + site.id() + ": expected failed accepting floor, six old token marks, witness blank, record lecterns, and editable filing sign.";
        }
        if ("keeper_altar".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.SOUL_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected Hold-native keeper altar lights and last-keeper record.";
        }
        if ("coop_plate".equals(site.id())
                && (block.getType() != Material.STONE_PRESSURE_PLATE
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_TUFF))) {
            return "FAIL " + site.id() + ": expected co-op witness plate and carve marks.";
        }
        if ("the_unwriting".equals(site.id())
                && (block.getType() != Material.SCULK_SHRIEKER
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasHoldFinaleMarkersNear(loc))) {
            return "FAIL " + site.id() + ": expected unwriting wall, finale lectern, and tagged choice/release markers.";
        }
        if ("vaun_hoard_chest".equals(type)
                && block.getType() != Material.CHEST
                && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.BARREL) {
            return "FAIL " + site.id() + ": expected chest/barrel hardware, found " + block.getType() + ".";
        }
        if (needsAnswerSurface(site) && !hasEditableSignNear(loc, Math.max(1, site.radius()))) {
            return "FAIL " + site.id() + ": no editable answer sign found inside answer radius.";
        }
        if ("painted_line".equals(type) && block.getType() != Material.BLACK_CONCRETE) {
            return "FAIL " + site.id() + ": expected black line block, found " + block.getType() + ".";
        }
        if ("bird_coops".equals(type) && !hasMaterialNear(loc, Math.max(2, site.radius()), Material.IRON_BARS)) {
            return "FAIL " + site.id() + ": expected visible cage bars inside coops radius.";
        }
        if ("far_water".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.DARK_PRISMARINE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SEA_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected far-water mirror pool, count stones, copybook shelf, and far-water copybook inside radius.";
        }
        if ("school_stand".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAY_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected school slate, copybook shelf, six stones, grey seventh marker, and copy-line book inside radius.";
        }
        if ("markers_row".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_DEEPSLATE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAY_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BROWN_CARPET)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected six bow-stones, worn bow marks, grey seventh hollow, and marker-row book inside radius.";
        }
        if ("cistern_7".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.DARK_PRISMARINE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.END_STONE_BRICKS)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected black water, pale arch, good-oil jars, and cistern record inside radius.";
        }
        if ("watch_floor".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SOUL_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CANDLE))) {
            return "FAIL " + site.id() + ": expected watch-log lectern, black-moon lights, and dark-hours proof inside radius.";
        }
        if ("set_apart_shelf".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected entry-5 shelf, cold/warm lamp contrast, redacted count, and set-apart record inside radius.";
        }
        if ("undercroft_seal".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_DEEPSLATE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.POLISHED_DEEPSLATE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SOUL_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected sealed door, mason line, low bow-to-read line, and mason record inside radius.";
        }
        if ("forgotten_mouth".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRASS_BLOCK)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GLOWSTONE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SEA_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected true way-up mouth, healed surface, return mark, and way-up draft inside radius.";
        }
        if ("deep_market".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected market stalls, lectern-shelf ledger, and lived-in trade objects inside radius.";
        }
        if ("ration_table".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CAKE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected ration table, half-loaf marker, and written R14 ration form inside radius.";
        }
        if ("third_bay_breach".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SCULK)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected broken Deep Line, downward breach, and third-bay incident note inside radius.";
        }
        if ("warm_town_collapse".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAVEL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected collapse rubble and WARDEN-3 closure record inside warm-town radius.";
        }
        if (isCoreAuditSite(site.id()) && block.getType() == Material.AIR) {
            return "FAIL " + site.id() + ": anchor block is air.";
        }
        return null;
    }

    private boolean hasMaterialNear(Location loc, int radius, Material material) {
        if (loc == null || loc.getWorld() == null || material == null) return false;
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(1, Math.min(8, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (world.getBlockAt(bx + dx, by + dy, bz + dz).getType() == material) return true;
                }
            }
        }
        return false;
    }

    private boolean hasItemFrameNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return false;
        double r = Math.max(0.5, Math.min(4.0, radius));
        return !loc.getWorld().getNearbyEntitiesByType(ItemFrame.class, loc, r).isEmpty();
    }

    private boolean hasHoldFinaleMarkersNear(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        var seventhKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.SeventhChoiceListener.PDC_SEVENTH_CHOICE);
        var releaseKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.ReleaseRiteListener.PDC_RELEASE);
        boolean restore = false;
        boolean erase = false;
        boolean release = false;
        for (org.bukkit.entity.ArmorStand stand : loc.getWorld().getNearbyEntitiesByType(
                org.bukkit.entity.ArmorStand.class, loc, 8.0)) {
            try {
                var pdc = stand.getPersistentDataContainer();
                String choice = pdc.get(seventhKey, org.bukkit.persistence.PersistentDataType.STRING);
                String rel = pdc.get(releaseKey, org.bukkit.persistence.PersistentDataType.STRING);
                if ("restore".equalsIgnoreCase(choice)) restore = true;
                if ("erase".equalsIgnoreCase(choice)) erase = true;
                if ("release".equalsIgnoreCase(rel)) release = true;
            } catch (Throwable ignored) { }
        }
        return restore && erase && release;
    }

    private boolean removeRetiredBeaconNear(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        boolean changed = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 4; dy <= 9; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block beacon = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (beacon.getType() != Material.BEACON) continue;
                    beacon.setType(Material.AIR, false);
                    changed = true;
                    int baseY = beacon.getY() - 1;
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            Block base = world.getBlockAt(beacon.getX() + ox, baseY, beacon.getZ() + oz);
                            if (base.getType() == Material.IRON_BLOCK) {
                                base.setType(Material.AIR, false);
                            }
                        }
                    }
                    for (int oy = 1; oy <= 2; oy++) {
                        Block cap = world.getBlockAt(beacon.getX(), beacon.getY() + oy, beacon.getZ());
                        String n = cap.getType().name();
                        if (n.endsWith("_STAINED_GLASS") || cap.getType() == Material.SHROOMLIGHT) {
                            cap.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
        return changed;
    }

    private boolean hasSignNear(Location loc, int radius) {
        if (loc == null || loc.getWorld() == null) return false;
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(1, Math.min(24, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (b.getState() instanceof Sign) return true;
                }
            }
        }
        return false;
    }

    private boolean hasEditableSignNear(Location loc, int radius) {
        return countEditableSignsNear(loc, radius, 8) > 0;
    }

    private int countEditableSignsNear(Location loc, int radius) {
        return countEditableSignsNear(loc, radius, 24);
    }

    private int countEditableSignsNear(Location loc, int radius, int cap) {
        int count = 0;
        if (loc == null || loc.getWorld() == null) return 0;
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(1, Math.min(Math.max(1, cap), radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (b.getState() instanceof Sign sign && !sign.isWaxed()) count++;
                }
            }
        }
        return count;
    }

    private static boolean isCoreAuditSite(String id) {
        if (id == null) return false;
        return switch (id) {
            case "first_report_lectern_01", "first_marker_01", "rune_rosetta",
                 "stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss",
                 "stone_of_reckoning", "the_cold_hearth", "unbroken_light", "the_threshold",
                 "the_unwriting", "threshold_vault",
                 "lampworks_stair", "third_lamp_stand", "painted_line", "dead_stall", "the_far_water", "school_stand", "markers_row", "cistern_7", "watch_floor", "set_apart_shelf", "undercroft_seal", "forgotten_mouth", "deep_bird_coops", "deep_market", "ration_table", "third_bay_breach", "warm_town_collapse",
                 "mara_lectern_1", "mara_lectern_2", "mara_lectern_3", "mara_lectern_4", "mara_lectern_5" -> true;
            default -> false;
        };
    }

    private static boolean isLaunchRequiredSite(String id) {
        if (id == null) return false;
        for (String required : LAUNCH_REQUIRED_SITES) {
            if (required.equals(id)) return true;
        }
        return false;
    }

    private static void addAuditIssue(List<String> issues, String issue) {
        if (issues != null && issue != null && issues.size() < 12) issues.add(issue);
    }

    private static int trailingIndex(String id, int fallback) {
        if (id == null || id.isBlank()) return fallback;
        int end = id.length() - 1;
        while (end >= 0 && Character.isDigit(id.charAt(end))) end--;
        if (end == id.length() - 1) return fallback;
        try {
            return Integer.parseInt(id.substring(end + 1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void sendStatus(CommandSender sender) {
        var sb = plugin.supabase();
        boolean configured = sb != null && sb.isConfigured();
        sender.sendMessage("== The Observance ==");
        sender.sendMessage(" supabase configured: " + configured);
        if (sb != null) {
            sender.sendMessage(" last db call ok:    " + sb.lastCallSucceeded());
            sender.sendMessage(" queued writes:      " + sb.queuedWriteCount());
        }
        sender.sendMessage(" local sleep:        " + plugin.isLocallyAsleep());
        sender.sendMessage(" sites placed:       " + plugin.placedSiteCount());
        sender.sendMessage(" drama enabled:      " + (plugin.config() != null && plugin.config().dramaEnabled()));
        sendPackStatus(sender);
        var wren = plugin.wren();
        if (wren != null) {
            sender.sendMessage(" wren:               " + (wren.isSpawned() ? "present" : "not spawned")
                    + " (" + wren.backend() + ")");
        }
        var keeper = plugin.keeper();
        if (keeper != null) {
            sender.sendMessage(" keeper:             " + (keeper.isSpawned() ? "present" : "not spawned")
                    + " (" + keeper.backend() + ")");
        }
    }

    private void sendPackStatus(CommandSender sender) {
        var cfg = plugin.config();
        var tracker = plugin.resourcePack();
        if (cfg == null || tracker == null) {
            sender.sendMessage(" resource pack:      unavailable");
            return;
        }

        boolean configured = cfg.resourcePackUrl() != null && !cfg.resourcePackUrl().isBlank();
        boolean hashed = cfg.resourcePackSha1() != null
                && cfg.resourcePackSha1().trim().matches("(?i)[0-9a-f]{40}");
        java.util.Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        int onlineCount = online == null ? 0 : online.size();
        int loaded = 0;
        java.util.EnumMap<com.observance.watcher.signal.ResourcePackTracker.PackStatus, Integer> counts =
                new java.util.EnumMap<>(com.observance.watcher.signal.ResourcePackTracker.PackStatus.class);
        java.util.List<String> notReady = new java.util.ArrayList<>();

        if (online != null) {
            for (Player p : online) {
                if (p == null) continue;
                var status = tracker.status(p.getUniqueId());
                counts.put(status, counts.getOrDefault(status, 0) + 1);
                if (status == com.observance.watcher.signal.ResourcePackTracker.PackStatus.LOADED) {
                    loaded++;
                } else {
                    notReady.add(p.getName() + "=" + status);
                }
            }
        }

        sender.sendMessage(" resource pack:      " + (configured ? "configured" : "url unset")
                + ", sha1 " + (hashed ? "set" : "missing")
                + ", required " + cfg.resourcePackRequired());
        sender.sendMessage(" pack readiness:     " + loaded + "/" + onlineCount + " online loaded"
                + packStatusCounts(counts));
        if (!notReady.isEmpty()) {
            sender.sendMessage(" pack not ready:     " + joinFirst(notReady, 6));
        }
    }

    private static String packStatusCounts(java.util.EnumMap<com.observance.watcher.signal.ResourcePackTracker.PackStatus, Integer> counts) {
        if (counts == null || counts.isEmpty()) return "";
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (var status : com.observance.watcher.signal.ResourcePackTracker.PackStatus.values()) {
            int n = counts.getOrDefault(status, 0);
            if (n > 0) parts.add(status + "=" + n);
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private static String joinFirst(java.util.List<String> values, int limit) {
        if (values == null || values.isEmpty()) return "";
        int n = Math.max(0, Math.min(limit, values.size()));
        String joined = String.join(", ", values.subList(0, n));
        int remaining = values.size() - n;
        return remaining > 0 ? joined + ", +" + remaining + " more" : joined;
    }

    /**
     * Wang hash (integer bit-mixer). Produces a well-distributed 64-bit value from any long seed.
     * Used by the Wave-R6 placement scatter to derive per-site X interval and Z offsets without
     * Math.random() or wall-clock time, guaranteeing that the same survey origin always produces
     * the same layout (reproducible, ARG-safe). This is a standard finalisation step from the
     * 64-bit variant of the Wang hash (public domain).
     */
    private static long wangHash(long x) {
        x = (~x) + (x << 21);
        x ^= (x >>> 24);
        x = (x + (x << 3)) + (x << 8);
        x ^= (x >>> 14);
        x = (x + (x << 2)) + (x << 4);
        x ^= (x >>> 28);
        x += (x << 31);
        return x;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"status", "director", "audit", "visualaudit", "dialogueaudit", "preflight", "repair", "coverage", "visit", "runbook", "rehearse", "reload", "sleep", "flag", "site", "unlit", "placeworld", "placeroom", "placeregion", "placedeep", "placelecterns", "placehold", "placelab", "fullrun", "prepworld", "descentproof", "sidepass", "puzzlepass", "dreadpass", "placeprologue", "lens", "wren", "keeper", "townsfolk", "test", "needle", "finale", "reading"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("placehold")) {
            for (String s : new String[]{"build", "audit", "status", "seal", "open", "sync"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("placehold")
                && (args[1].equalsIgnoreCase("seal") || args[1].equalsIgnoreCase("open"))) {
            for (String s : holdGateSuggestions(args[2])) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("director")) {
            for (String s : new String[]{"state", "progress", "players", "stuck", "hints", "world", "lab"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            for (String s : visitSuggestions(args[1])) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("runbook")) {
            for (String s : new String[]{"setup", "spine", "puzzle", "side", "scare", "unlit", "ops"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("rehearse")) {
            for (String s : new String[]{"start", "status", "done", "next", "back", "reset", "list", "setup", "hardware", "puzzle", "puzzles", "spine", "side", "scare", "ops"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("puzzlepass")) {
            for (String s : new String[]{"gates", "12", "14", "18"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("dreadpass")) {
            for (String s : new String[]{"stage", "run"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("dreadpass")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("unlit")) {
            for (String s : new String[]{"site", "clue", "pass", "audit", "darken", "border", "buildmode", "ready"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("unlit")
                && (args[1].equalsIgnoreCase("site") || args[1].equalsIgnoreCase("clue"))) {
            for (String s : new String[]{"entry", "spawn", "exit", "lamp", "cairn", "coop", "well", "watch", "warm", "threshold", "base"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("unlit") && args[1].equalsIgnoreCase("border")) {
            for (String s : new String[]{"64", "96", "128"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("unlit") && args[1].equalsIgnoreCase("buildmode")) {
            for (String s : new String[]{"on", "off", "status"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("unlit") && args[1].equalsIgnoreCase("darken")) {
            for (String s : new String[]{"all", "border", "8", "10", "16", "24", "32"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("unlit") && args[1].equalsIgnoreCase("darken")
                && (args[2].equalsIgnoreCase("all") || args[2].equalsIgnoreCase("border"))) {
            for (String s : new String[]{"64", "96", "128", "160"}) {
                if (s.startsWith(args[3].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("unlit") && args[1].equalsIgnoreCase("pass")) {
            for (String s : new String[]{"light", "stalker", "extinguish", "house", "extract"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("site")) {
            for (String s : new String[]{"todo", "next", "plan", "launch", "list", "set"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("site") && args[1].equalsIgnoreCase("next")) {
            for (PlacementLane lane : PLACEMENT_LANES) {
                if (lane.id().startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(lane.id());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("site") && args[1].equalsIgnoreCase("set")) {
            out.addAll(siteIdSuggestions(args[2]));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("site") && args[1].equalsIgnoreCase("plan")) {
            for (String s : new String[]{"next", "all", "lanes"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
            for (PlacementLane lane : PLACEMENT_LANES) {
                if (lane.id().startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(lane.id());
            }
            out.addAll(siteIdSuggestions(args[2]));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sleep")) {
            out.add("on");
            out.add("off");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            for (String s : new String[]{"set", "clear", "list"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("lens")) {
            if ("give".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("wren")) {
            for (String s : new String[]{"spawn", "despawn", "reckoning"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("keeper")) {
            for (String s : new String[]{"spawn", "despawn"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("townsfolk")) {
            for (String s : new String[]{"spawn", "despawn", "aro", "wenna", "coll", "dob", "old-pell"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("townsfolk")) {
            for (String s : new String[]{"aro", "wenna", "coll", "dob", "old-pell"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            for (String s : new String[]{"menu", "whisper", "title", "sound", "voice", "darkness", "name", "reflection", "mob", "stalker", "hunt", "elsewhere", "gauntlet", "nightmare", "torch", "decay", "drift", "particles", "toast", "sign", "needle"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("lens")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
