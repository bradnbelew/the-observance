package com.observance.watcher.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.data.rows.AnswerAttemptReadRow;
import com.observance.watcher.data.rows.HintRow;
import com.observance.watcher.data.rows.PuzzleRow;
import com.observance.watcher.data.rows.SolveReadRow;
import com.observance.watcher.oracle.FlagGate;
import com.observance.watcher.finale.FinaleStateMachine;
import com.observance.watcher.structure.CanonicalArtifactRegistry;
import com.observance.watcher.structure.DeepHoldV4Geometry;
import com.observance.watcher.structure.DeepHoldV4Plan;
import com.observance.watcher.structure.DeepHoldV5Manifest;
import com.observance.watcher.structure.V5AuthorityManifest;
import com.observance.watcher.structure.V5RuntimePredicateRegistry;
import com.observance.watcher.structure.StructureTemplates;
import com.observance.watcher.structure.UnlitVillageCandidateBuilder;
import com.observance.watcher.util.Safety;
import com.observance.watcher.v5runtime.FixtureTransform;
import com.observance.watcher.v5runtime.V5BookMountPolicy;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentInstaller;
import com.observance.watcher.v5runtime.install.V5EvidenceItemTextAuthority;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentInstaller.Mode;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentInstaller.Report;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * {@code /observance} admin command. Read-only status + safe controls (reload, local sleep
 * toggle). The body is wrapped in Safety so a bad command never propagates an exception.
 */
public final class ObservanceCommand implements CommandExecutor, TabCompleter {

    /**
     * Mutation-heavy V4 rehearsal/build surfaces retained only so old worlds still compile. V5
     * production must never reach StructureTemplates through these commands because those templates
     * contain retired story copy and retired flag assumptions.
     */
    private static final Set<String> V5_RETIRED_COMMANDS = Set.of(
            "placeworld", "placeroom", "placeregion", "placedeep", "placelecterns",
            "placelab", "fullrun", "prepworld", "descentproof", "sidepass", "puzzlepass",
            "dreadpass", "rehearse", "placeprologue", "placerelay", "keeper", "test",
            "reading", "visit", "lens", "needle", "director", "coverage", "flag", "site"
    );

    private final ObservancePlugin plugin;
    private final Safety safety;
    private final Map<String, Integer> rehearsalProgress = new HashMap<>();
    private final Map<String, Integer> visitProgress = new HashMap<>();
    /** In-memory, exact-Mouth approval produced only by a passing read-only `/obs placehold plan`. */
    private String approvedHoldPlanKey;
    /** Non-null only while the architecture plan is being applied in watchdog-safe tick batches. */
    private DeepHoldV4Geometry.BuildPlan activeHoldBuild;
    /** Durable fixture-shell milestone: recovery must not replay retired template mutations past it. */
    private boolean activeHoldFixtureShellReady;
    /** Non-null while Paper is asynchronously generating/loading one Hold footprint chunk at a time. */
    private HoldChunkPreparation activeHoldPreparation;
    /** Exact Mouth/authority identity whose complete footprint is held by plugin chunk tickets. */
    private String preparedHoldPlanKey;
    /** Tickets acquired by the explicit preparation phase; never leave them resident after a build. */
    private final Set<Chunk> holdChunkTickets = new LinkedHashSet<>();

    private static final int HOLD_BUILD_MAX_OPERATIONS_PER_TICK = 6_000;
    private static final long HOLD_BUILD_MAX_NANOS_PER_TICK = 12_000_000L;
    private static final long HOLD_PREPARATION_TTL_TICKS = 20L * 60L * 30L;

    private static final class HoldChunkPreparation {
        private final Location mouth;
        private final String planKey;
        private final List<DeepHoldV4Geometry.ChunkCoordinate> chunks;
        private final CommandSender sender;
        private int cursor;

        private HoldChunkPreparation(Location mouth, String planKey,
                                     List<DeepHoldV4Geometry.ChunkCoordinate> chunks,
                                     CommandSender sender) {
            this.mouth = mouth.clone();
            this.planKey = planKey;
            this.chunks = List.copyOf(chunks);
            this.sender = sender;
        }
    }

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
        if (V5_RETIRED_COMMANDS.contains(sub)) {
            sender.sendMessage("Observance V5: /obs " + sub + " is retired and made no changes.");
            sender.sendMessage("  Use placehold, unlit, townsfolk, wren, item, preflight, or finale."
                    + " Retired templates are intentionally unreachable in production.");
            return;
        }
        switch (sub) {
            case "status" -> sendStatus(sender);
            case "audit" -> handleAudit(sender);
            case "visualaudit" -> handleVisualAudit(sender);
            case "preflight" -> handlePreflight(sender);
            case "dialogueaudit" -> handleDialogueAudit(sender);
            case "repair" -> handlePlaceHoldRepair(sender, new String[]{"placehold", "repair", "all"});
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
            case "runbook" -> sendV5Runbook(sender);
            case "rehearse" -> handleRehearse(sender, args);
            case "placeprologue" -> handlePlacePrologue(sender, args);
            case "placerelay" -> handlePlaceRelay(sender);
            case "lens" -> handleLens(sender, args);
            case "wren" -> handleWren(sender, args);
            case "keeper" -> handleKeeper(sender, args);
            case "townsfolk" -> handleTownsfolk(sender, args);
            case "test" -> handleTest(sender, args);
            case "needle" -> handleNeedle(sender, args);
            case "item" -> handleItem(sender, args);
            case "finale" -> handleFinale(sender, args);
            case "reading" -> handleReadingCarvings(sender);
            default -> sender.sendMessage("Unknown V5 subcommand. Use: status | audit | visualaudit | "
                    + "dialogueaudit | preflight | repair | reload | sleep <on|off> | "
                    + "unlit <site|audit|darken|border|buildmode|ready> | "
                    + "placehold <prepare|plan|build|repair|audit|seal|open|sync> | item recover <artifact> [player] | "
                    + "wren <spawn|despawn> | townsfolk <spawn|despawn> [id] | "
                    + "finale <arm|cancel|status|markers> | runbook");
        }
    }

    private void sendV5Runbook(CommandSender sender) {
        sender.sendMessage("== Observance V5 production runbook ==");
        sender.sendMessage("  1) At the exact Mouth run /obs placehold prepare, then /obs placehold plan.");
        sender.sendMessage("  2) Only after PLAN PASS run /obs placehold build, audit, and preflight.");
        sender.sendMessage("  3) Configure the well-based Unlit anchors/houses with /obs unlit; keep buildmode off.");
        sender.sendMessage("  4) Spawn the five fixed townsfolk and Wren only at their persisted V5 anchors.");
        sender.sendMessage("  5) Sync gates/books from durable state, repeat preflight, then test from a clean player.");
        sender.sendMessage("  Full plain-English commands and expected observations: plugin/V5-RUNBOOK.md.");
    }

    /** Idempotent operator recovery for V5's canonical PDC-backed key items. */
    private void handleItem(CommandSender sender, String[] args) {
        String op = args.length >= 2 ? args[1].trim().toLowerCase(Locale.ROOT) : "";
        if (!"recover".equals(op) || args.length < 3) {
            sender.sendMessage("Usage: /observance item recover <artifact> [player]");
            sender.sendMessage("Artifacts: " + String.join(", ", CanonicalArtifactRegistry.ids()));
            return;
        }
        String artifact = CanonicalArtifactRegistry.resolveId(args[2]);
        if (artifact == null) {
            sender.sendMessage("Observance: unknown artifact '" + args[2] + "'.");
            sender.sendMessage("Artifacts: " + String.join(", ", CanonicalArtifactRegistry.ids()));
            return;
        }
        Player target = null;
        if (args.length >= 4) target = Bukkit.getPlayerExact(args[3]);
        if (target == null && sender instanceof Player player && args.length < 4) target = player;
        if (target == null) {
            sender.sendMessage("Observance: choose an online player: /obs item recover "
                    + artifact + " <player>.");
            return;
        }

        V5AuthorityManifest.ArtifactEntry v5Artifact = V5AuthorityManifest.artifact(artifact);
        if (v5Artifact != null) {
            var runtime = plugin.v5Runtime();
            if (runtime != null && runtime.snapshot().isComplete(v5Artifact.recoveryFlag())) {
                sender.sendMessage("Observance: local V5 record verifies "
                        + v5Artifact.recoveryFlag() + "; checking for duplicates...");
                performArtifactRecovery(sender, target, artifact);
                return;
            }
            var sb = plugin.supabase();
            if (sb == null || !sb.isConfigured()) {
                sender.sendMessage("Observance: recovery refused; the local V5 record does not contain "
                        + v5Artifact.recoveryFlag() + " and the optional remote mirror is unavailable.");
                return;
            }
            Player recoveryTarget = target;
            sender.sendMessage("Observance: local entitlement is absent; checking the remote mirror for "
                    + v5Artifact.recoveryFlag() + "...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                var result = sb.fetchArcState();
                boolean remoteEntitled = result.ok() && result.value() != null
                        && directorFlag(result.value().flagsMap(), v5Artifact.recoveryFlag());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var currentRuntime = plugin.v5Runtime();
                    boolean entitled = remoteEntitled || (currentRuntime != null
                            && currentRuntime.snapshot().isComplete(v5Artifact.recoveryFlag()));
                    if (!entitled) {
                        sender.sendMessage("Observance: recovery refused; " + v5Artifact.recoveryFlag()
                                + " is not durably complete.");
                        return;
                    }
                    if (!recoveryTarget.isOnline()) {
                        sender.sendMessage("Observance: recovery target left before verification; issued nothing.");
                        return;
                    }
                    performArtifactRecovery(sender, recoveryTarget, artifact);
                });
            });
            return;
        }
        performArtifactRecovery(sender, target, artifact);
    }

    private void performArtifactRecovery(CommandSender sender, Player target, String artifact) {

        if (inventoryContainsArtifact(target.getInventory(), artifact)
                || inventoryContainsArtifact(target.getEnderChest(), artifact)) {
            sender.sendMessage("Observance: " + target.getName() + " already has " + artifact
                    + "; recovery is idempotent and issued nothing.");
            return;
        }
        ArtifactPresence existing = findLoadedArtifact(artifact);
        if (existing != null) {
            sender.sendMessage("Observance: recovery issued nothing; the live " + artifact
                    + " already exists at " + existing.description() + ".");
            sender.sendMessage("  Retrieve that copy first. This guard prevents accidental canonical duplicates.");
            return;
        }
        if (target.getInventory().firstEmpty() < 0) {
            sender.sendMessage("Observance: " + target.getName()
                    + " has no empty inventory slot; nothing was displaced or dropped.");
            return;
        }

        Location keptLight = null;
        Site kept = plugin.sites() == null ? null : plugin.sites().get("unbroken_light");
        if (kept != null) keptLight = kept.location();
        if ("kept_needle".equals(artifact) && keptLight == null) {
            sender.sendMessage("Observance: kept_needle recovery refused; unbroken_light is not placed/loaded.");
            return;
        }
        ItemStack item = CanonicalArtifactRegistry.create(artifact, keptLight);
        List<String> issues = CanonicalArtifactRegistry.audit(item, artifact);
        if (item == null || !issues.isEmpty()) {
            sender.sendMessage("Observance: recovery factory failed exact audit: " + String.join("; ", issues));
            return;
        }
        int slot = target.getInventory().firstEmpty();
        target.getInventory().setItem(slot, item);
        if (!CanonicalArtifactRegistry.isArtifact(target.getInventory().getItem(slot), artifact)) {
            target.getInventory().setItem(slot, null);
            sender.sendMessage("Observance: inventory verification failed; recovery was rolled back.");
            return;
        }
        sender.sendMessage("Observance: recovered canonical " + artifact + " to " + target.getName()
                + " in slot " + slot + ". A repeat command will issue nothing.");
        if (!target.equals(sender)) target.sendMessage("The record returned " + artifact.replace('_', ' ') + ".");
        String atmosphere = CanonicalArtifactRegistry.atmosphereSound(artifact);
        if (atmosphere != null) {
            target.playSound(target.getLocation(), atmosphere, org.bukkit.SoundCategory.VOICE, 0.85f, 1.0f);
            target.sendActionBar(Component.text("The affidavit is present; the voice is atmosphere, not a clue."));
        }
    }

    private boolean inventoryContainsArtifact(Inventory inventory, String artifact) {
        if (inventory == null) return false;
        for (ItemStack item : inventory.getContents()) {
            if (CanonicalArtifactRegistry.isArtifact(item, artifact)) return true;
        }
        return false;
    }

    private ArtifactPresence findLoadedArtifact(String artifact) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inventoryContainsArtifact(player.getInventory(), artifact)) {
                return new ArtifactPresence("player " + player.getName() + "'s inventory");
            }
            if (inventoryContainsArtifact(player.getEnderChest(), artifact)) {
                return new ArtifactPresence("player " + player.getName() + "'s ender chest");
            }
        }
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Item dropped : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
                if (CanonicalArtifactRegistry.isArtifact(dropped.getItemStack(), artifact)) {
                    Location at = dropped.getLocation();
                    return new ArtifactPresence(world.getName() + " " + at.getBlockX() + ","
                            + at.getBlockY() + "," + at.getBlockZ() + " (dropped item)");
                }
            }
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof InventoryHolder holder)) continue;
                    if (!inventoryContainsArtifact(holder.getInventory(), artifact)) continue;
                    Location at = state.getLocation();
                    return new ArtifactPresence(world.getName() + " " + at.getBlockX() + ","
                            + at.getBlockY() + "," + at.getBlockZ() + " (loaded container)");
                }
            }
        }
        return null;
    }

    private record ArtifactPresence(String description) { }

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
            "discord_relay",
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
                    "discord_relay", "first_report_lectern_01", "rune_rosetta"
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

    private record HoldRoomBox(String id, int minX, int maxX, int floorY, int ceilingY, int minZ, int maxZ) { }
    private record HoldRoomLink(String from, String to) { }
    private record HoldWalkNode(int x, int y, int z) { }

    private static final HoldRoomBox[] DEEP_HOLD_ROOM_BOXES = {
            new HoldRoomBox("mouth_vestibule", -16, 16, -24, -8, 66, 86),
            new HoldRoomBox("orientation_west", -50, -20, -24, -10, 94, 138),
            new HoldRoomBox("orientation_center", -18, 18, -24, -8, 94, 138),
            new HoldRoomBox("orientation_east", 20, 50, -24, -10, 94, 138),

            new HoldRoomBox("keeper_vaun", -78, -34, -28, -8, 150, 184),
            new HoldRoomBox("keeper_mara", 34, 78, -28, -8, 150, 190),
            new HoldRoomBox("keeper_iss", -78, -34, -28, -8, 190, 222),
            new HoldRoomBox("keeper_sella", 34, 78, -28, -8, 196, 226),
            new HoldRoomBox("keeper_brann", -78, -34, -28, -8, 226, 260),
            new HoldRoomBox("keeper_orin", 34, 78, -28, -8, 232, 266),
            new HoldRoomBox("keeper_nave", -28, 28, -28, -6, 148, 266),

            new HoldRoomBox("archive_nave", -24, 24, -24, -8, 274, 422),
            new HoldRoomBox("archive_school", -120, -72, -24, -8, 278, 316),
            new HoldRoomBox("archive_markers", -70, -28, -24, -8, 278, 316),
            new HoldRoomBox("archive_cistern", -120, -72, -24, -8, 324, 362),
            new HoldRoomBox("archive_watch", -70, -28, -24, -8, 324, 362),
            new HoldRoomBox("archive_shelf", -120, -72, -24, -8, 370, 414),
            new HoldRoomBox("archive_water", -70, -28, -24, -8, 370, 414),
            new HoldRoomBox("archive_market", 72, 120, -24, -8, 278, 316),
            new HoldRoomBox("archive_ration", 28, 70, -24, -8, 278, 316),
            new HoldRoomBox("archive_breach", 72, 120, -24, -8, 324, 362),
            new HoldRoomBox("archive_warm", 28, 70, -24, -8, 324, 362),
            new HoldRoomBox("archive_stall", 72, 120, -24, -8, 370, 414),
            new HoldRoomBox("archive_coops", 28, 70, -24, -8, 370, 414),
            new HoldRoomBox("puzzle_works", -38, 38, -24, -6, 430, 492),

            new HoldRoomBox("prior_case", -108, -68, -28, -8, 508, 548),
            new HoldRoomBox("prior_camp", -118, -58, -28, -6, 552, 612),
            new HoldRoomBox("lower_reckoning", -56, -8, -28, -6, 508, 556),
            new HoldRoomBox("lower_threshold", 8, 56, -28, -6, 508, 556),
            new HoldRoomBox("lower_convergence", -38, 38, -28, -4, 560, 606),
            new HoldRoomBox("lower_vault", 42, 82, -28, -4, 560, 606),
            new HoldRoomBox("lower_altar", -56, -8, -28, -4, 612, 650),
            new HoldRoomBox("lower_coop", 8, 56, -28, -4, 612, 650),
            new HoldRoomBox("dread", 88, 122, -28, -4, 508, 650),
            new HoldRoomBox("accepting", -42, 42, -32, -4, 658, 718),
            new HoldRoomBox("unwriting", -42, 42, -32, -4, 732, 788),
    };

    /** Authored doorway graph. Every owned room must be connected to the mouth through these links. */
    private static final HoldRoomLink[] DEEP_HOLD_ROOM_LINKS = {
            new HoldRoomLink("mouth_vestibule", "orientation_center"),
            new HoldRoomLink("orientation_center", "orientation_west"),
            new HoldRoomLink("orientation_center", "orientation_east"),
            new HoldRoomLink("orientation_center", "keeper_nave"),
            new HoldRoomLink("keeper_nave", "keeper_vaun"),
            new HoldRoomLink("keeper_nave", "keeper_mara"),
            new HoldRoomLink("keeper_nave", "keeper_iss"),
            new HoldRoomLink("keeper_nave", "keeper_sella"),
            new HoldRoomLink("keeper_nave", "keeper_brann"),
            new HoldRoomLink("keeper_nave", "keeper_orin"),
            new HoldRoomLink("keeper_nave", "archive_nave"),
            new HoldRoomLink("archive_nave", "archive_school"),
            new HoldRoomLink("archive_nave", "archive_markers"),
            new HoldRoomLink("archive_nave", "archive_cistern"),
            new HoldRoomLink("archive_nave", "archive_watch"),
            new HoldRoomLink("archive_nave", "archive_shelf"),
            new HoldRoomLink("archive_nave", "archive_water"),
            new HoldRoomLink("archive_nave", "archive_market"),
            new HoldRoomLink("archive_nave", "archive_ration"),
            new HoldRoomLink("archive_nave", "archive_breach"),
            new HoldRoomLink("archive_nave", "archive_warm"),
            new HoldRoomLink("archive_nave", "archive_stall"),
            new HoldRoomLink("archive_nave", "archive_coops"),
            new HoldRoomLink("archive_nave", "puzzle_works"),
            new HoldRoomLink("puzzle_works", "lower_reckoning"),
            new HoldRoomLink("lower_reckoning", "lower_threshold"),
            new HoldRoomLink("lower_reckoning", "prior_case"),
            new HoldRoomLink("prior_case", "prior_camp"),
            new HoldRoomLink("lower_threshold", "dread"),
            new HoldRoomLink("lower_reckoning", "lower_convergence"),
            new HoldRoomLink("lower_threshold", "lower_convergence"),
            new HoldRoomLink("lower_convergence", "lower_vault"),
            new HoldRoomLink("lower_convergence", "lower_altar"),
            new HoldRoomLink("lower_convergence", "lower_coop"),
            new HoldRoomLink("lower_convergence", "accepting"),
            new HoldRoomLink("accepting", "unwriting"),
    };

    private static final String HOLD_REGION_SITE_ID = "deep_hold_region";
    private static final String HOLD_ENTRY_REGION_SITE_ID = "deep_hold_entry_stair";

    /**
     * Production Deep Hold layout. Nether/End lanes intentionally remain out-of-hold; those must still be
     * surveyed and placed in their real dimensions.
     */
    private static final HoldSite[] DEEP_HOLD_SITES = {
            new HoldSite("undercroft_seal", "undercroft_seal", 11, 7, -34, -24, 108, 0, 0),
            new HoldSite("forgotten_mouth", "forgotten_mouth", 11, 7, 34, -24, 108, 0, 0),
            new HoldSite("rune_rosetta", "structure", 8, 7, 0, -24, 112, 15, 12),
            new HoldSite("bow_marker_01", "bow_marker", 4, 4, -34, -24, 132, 0, 0),
            new HoldSite("offering_cairn_01", "offering_cairn", 4, 4, 0, -24, 132, 0, 0),
            new HoldSite("kept_light_home_01", "kept_light", 5, 4, 34, -24, 132, 0, 0),

            new HoldSite("stone_vaun", "keeper_stone", 8, 7, -68, -28, 168, 0, 0),
            new HoldSite("vaun_hoard_chest", "vaun_hoard_chest", 2, 3, -52, -28, 158, 0, 0),
            new HoldSite("vaun_bookshelf", "vaun_bookshelf", 2, 3, -52, -28, 176, 0, 0),
            new HoldSite("stone_mara", "keeper_stone", 8, 7, 68, -28, 168, 0, 0),
            new HoldSite("mara_lectern_1", "mara_lectern", 2, 2, 40, -28, 158, 0, 0),
            new HoldSite("mara_lectern_2", "mara_lectern", 2, 2, 48, -28, 158, 0, 0),
            new HoldSite("mara_lectern_3", "mara_lectern", 2, 2, 56, -28, 158, 0, 0),
            new HoldSite("mara_lectern_4", "mara_lectern", 2, 2, 64, -28, 158, 0, 0),
            new HoldSite("mara_lectern_5", "mara_lectern", 2, 2, 72, -28, 158, 0, 0),
            new HoldSite("mara_route_marker_1", "mara_route_marker", 2, 3, 40, -28, 174, 0, 0),
            new HoldSite("mara_route_marker_2", "mara_route_marker", 2, 3, 48, -28, 182, 0, 0),
            new HoldSite("mara_route_marker_3", "mara_route_marker", 2, 3, 64, -28, 182, 0, 0),
            new HoldSite("mara_route_marker_4", "mara_route_marker", 2, 3, 72, -28, 174, 0, 0),
            new HoldSite("mara_map_marker", "mara_map_marker", 4, 4, 56, -28, 184, 0, 0),

            new HoldSite("stone_sella", "keeper_stone", 8, 7, 68, -28, 212, 0, 0),
            new HoldSite("sella_pool", "sella_pool", 5, 4, 56, -29, 214, 0, 0),
            new HoldSite("sella_anchor", "sella_anchor", 3, 5, 44, -24, 218, 0, 0),
            new HoldSite("sella_lectern_1", "sella_lectern", 2, 2, 40, -28, 200, 0, 0),
            new HoldSite("sella_lectern_2", "sella_lectern", 2, 2, 48, -28, 200, 0, 0),
            new HoldSite("sella_lectern_3", "sella_lectern", 2, 2, 56, -28, 200, 0, 0),
            new HoldSite("sella_lectern_4", "sella_lectern", 2, 2, 64, -28, 200, 0, 0),
            new HoldSite("sella_lectern_5", "sella_lectern", 2, 2, 72, -28, 200, 0, 0),

            new HoldSite("stone_orin", "keeper_stone", 8, 7, 68, -28, 252, 0, 0),
            new HoldSite("orin_marker_1", "orin_marker", 3, 4, 40, -28, 240, 0, 0),
            new HoldSite("orin_marker_2", "orin_marker", 3, 4, 52, -28, 240, 0, 0),
            new HoldSite("orin_marker_3", "orin_marker", 3, 4, 64, -28, 240, 0, 0),
            new HoldSite("orin_marker_4", "orin_marker", 3, 4, 40, -28, 248, 0, 0),
            new HoldSite("orin_marker_5", "orin_marker", 3, 4, 52, -28, 248, 0, 0),
            new HoldSite("orin_marker_6", "orin_marker", 3, 4, 64, -28, 248, 0, 0),
            new HoldSite("orin_frame_dial_1", "orin_frame_dial", 2, 3, 40, -27, 260, 0, 0),
            new HoldSite("orin_frame_dial_2", "orin_frame_dial", 2, 3, 46, -27, 260, 0, 0),
            new HoldSite("orin_frame_dial_3", "orin_frame_dial", 2, 3, 52, -27, 260, 0, 0),
            new HoldSite("orin_frame_dial_4", "orin_frame_dial", 2, 3, 58, -27, 260, 0, 0),
            new HoldSite("orin_frame_dial_5", "orin_frame_dial", 2, 3, 64, -27, 260, 0, 0),
            new HoldSite("orin_frame_dial_6", "orin_frame_dial", 2, 3, 70, -27, 260, 0, 0),
            new HoldSite("stone_brann", "keeper_stone", 8, 7, -68, -28, 246, 0, 0),
            new HoldSite("brann_toll_tower", "brann_toll_tower", 5, 7, -52, -28, 238, 0, 0),
            new HoldSite("brann_corridor_start", "brann_corridor_start", 3, 4, -72, -28, 254, 0, 0),
            new HoldSite("brann_corridor_end", "brann_corridor_end", 3, 4, -40, -28, 254, 0, 0),
            new HoldSite("stone_iss", "keeper_stone", 8, 7, -68, -28, 204, 0, 0),
            new HoldSite("the_cold_hearth", "marker", 8, 7, -53, -28, 206, 16, 13),

            new HoldSite("case_board", "case_board", 8, 6, -88, -28, 528, 0, 0),
            new HoldSite("prior_camp", "prior_camp", 28, 12, -88, -28, 582, 26, 18),

            new HoldSite("school_stand", "school_stand", 14, 8, -95, -24, 298, 0, 0),
            new HoldSite("markers_row", "markers_row", 15, 8, -49, -24, 298, 0, 0),
            new HoldSite("cistern_7", "cistern_7", 15, 9, -95, -24, 344, 0, 0),
            new HoldSite("watch_floor", "watch_floor", 14, 9, -49, -24, 344, 0, 0),
            new HoldSite("set_apart_shelf", "set_apart_shelf", 14, 8, -95, -24, 390, 0, 0),
            new HoldSite("the_far_water", "far_water", 18, 9, -49, -24, 390, 18, 9),
            new HoldSite("deep_market", "deep_market", 20, 10, 95, -24, 298, 20, 10),
            new HoldSite("ration_table", "ration_table", 13, 8, 49, -24, 298, 0, 0),
            new HoldSite("third_bay_breach", "third_bay_breach", 16, 9, 95, -24, 344, 16, 9),
            new HoldSite("warm_town_collapse", "warm_town_collapse", 18, 10, 49, -24, 344, 18, 10),

            new HoldSite("lampworks_stair", "lampworks_stair", 20, 16, 0, -24, 450, 20, 16),
            new HoldSite("third_lamp_stand", "lamp_stand", 4, 5, -18, -24, 474, 0, 0),
            new HoldSite("painted_line", "painted_line", 5, 5, 0, -24, 486, 0, 0),
            new HoldSite("dead_stall", "dead_stall", 10, 7, 95, -24, 390, 16, 12),
            new HoldSite("deep_bird_coops", "bird_coops", 12, 7, 49, -24, 390, 18, 14),

            new HoldSite("stone_of_reckoning", "structure", 10, 9, -32, -28, 532, 20, 16),
            new HoldSite("the_threshold", "the_threshold", 10, 9, 32, -28, 532, 20, 16),
            new HoldSite("threshold_vault", "coop_plate", 9, 8, 62, -28, 582, 17, 14),
            new HoldSite("failed_accepting", "failed_accepting", 20, 10, 0, -28, 584, 27, 19),
            new HoldSite("unbroken_light", "accepting_floor", 24, 12, 0, -32, 688, 33, 25),
            new HoldSite("keeper_altar", "keeper_altar", 10, 8, -32, -28, 632, 16, 13),
            new HoldSite("coop_plate", "coop_plate", 9, 7, 32, -28, 632, 16, 13),
            new HoldSite("the_unwriting", "seventh_shrine", 18, 10, 0, -32, 760, 29, 21),

            new HoldSite("dread_route_start", "dread_route", 5, 5, 96, -28, 520, 0, 0),
            new HoldSite("dread_route_elsewhere", "dread_route", 5, 5, 112, -28, 552, 0, 0),
            new HoldSite("dread_route_figure", "dread_route", 5, 5, 112, -28, 592, 0, 0),
            new HoldSite("dread_route_exit", "dread_route", 5, 5, 96, -28, 632, 0, 0),
    };

    private static final HoldGate[] DEEP_HOLD_GATES = {
            compactHoldGate("g1", "keeper", "Keeper Court"),
            compactHoldGate("g2", "archive", "Archive Works"),
            compactHoldGate("g3", "undercroft", "Lower Works"),
            compactHoldGate("g4", "deep", "Deep Line"),
            compactHoldGate("prior", "prior", "Old Survey Camp"),
            compactHoldGate("dread", "dread", "East Service"),
            compactHoldGate("g5", "accepting", "Accepting Floor"),
            compactHoldGate("g6", "coda", "Release Chamber"),
    };

    private static HoldGate compactHoldGate(String planId, String runtimeId, String label) {
        DeepHoldV4Plan.Gate plan = DeepHoldV4Plan.GATES.stream()
                .filter(gate -> planId.equals(gate.id())).findFirst()
                .orElseThrow(() -> new IllegalStateException("missing compact gate " + planId));
        return new HoldGate(runtimeId, plan.x(), plan.y(), plan.z(), false, label);
    }

    private static final HoldRecordStation[] DEEP_HOLD_RECORD_STATIONS = {
            new HoldRecordStation("mouth_register", 10, -24, 76, BlockFace.WEST,
                    "mouth register", "One row stayed blank", 6, -24, 76),
            new HoldRecordStation("court_census", -18, -28, 154, BlockFace.EAST,
                    "court census", "trust the physical room", -14, -28, 154),
            new HoldRecordStation("intake_rail", -16, -24, 282, BlockFace.EAST,
                    "intake rail", "evidence, not decoration", -12, -24, 282),
            new HoldRecordStation("prior_roster", -98, -28, 518, BlockFace.EAST,
                    "prior roster", "no witness", -94, -28, 518),
            new HoldRecordStation("closure_docket", 16, -24, 282, BlockFace.WEST,
                    "closure docket", "before the collapse", 12, -24, 282),
            new HoldRecordStation("lamp_count", -28, -24, 438, BlockFace.EAST,
                    "lamp count", "Do not break the wall", -24, -24, 438),
            new HoldRecordStation("threshold_hands", -18, -28, 566, BlockFace.EAST,
                    "threshold hands", "Plate. Name. Word.", -14, -28, 566),
            new HoldRecordStation("side_hush", 96, -28, 514, BlockFace.EAST,
                    "side hush", "uncrossed word", 100, -28, 514),
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
        if (op.equals("launch")) {
            handlePreflight(sender);
            return;
        }
        if (op.equals("todo") || op.equals("list")) {
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
                if (sender instanceof Player player) {
                    recordUnlitOrientation(player.getLocation(), siteId);
                }
            }
            case "audit" -> handleUnlitAudit(sender);
            case "border" -> handleUnlitBorder(sender, args);
            case "darken", "scrub" -> handleUnlitDarken(sender, args);
            case "buildmode", "build" -> handleUnlitBuildMode(sender, args);
            case "candidate" -> handleUnlitCandidate(sender, args);
            case "clue" -> handleUnlitClue(sender, args);
            case "pass" -> sender.sendMessage(
                    "Observance V5: /obs unlit pass is retired and made no changes. Use /obs unlit ready.");
            case "repair" -> handleUnlitRepair(sender);
            case "ready", "playtest" -> handleUnlitReady(sender);
            default -> sender.sendMessage("Usage: /observance unlit <candidate|site|clue|repair|audit|darken|border|buildmode|ready>");
        }
    }

    private void handleUnlitCandidate(CommandSender sender, String[] args) {
        String operation = args.length > 2 ? args[2].toLowerCase(Locale.ROOT).trim() : "audit";
        if (operation.equals("audit")) {
            handleUnlitReady(sender);
            return;
        }
        if (!operation.equals("build")) {
            sender.sendMessage("Usage: /obs unlit candidate <build|audit> [world] [originX] [baseY] [originZ]");
            return;
        }
        if (!plugin.getConfig().getBoolean("unlit.candidate-build-enabled", false)) {
            sender.sendMessage("UNLIT_CANDIDATE_BUILD BLOCKED unlit.candidate-build-enabled is false");
            return;
        }
        String worldName = args.length > 3 ? args[3].trim()
                : plugin.getConfig().getString("unlit.world", "observance_unlit");
        if (worldName == null || !worldName.matches("[A-Za-z0-9_.-]{1,64}")) {
            sender.sendMessage("UNLIT_CANDIDATE_BUILD BLOCKED invalid world name");
            return;
        }
        try {
            int originX = args.length > 4 ? Integer.parseInt(args[4]) : 0;
            int baseY = args.length > 5 ? Integer.parseInt(args[5]) : 72;
            int originZ = args.length > 6 ? Integer.parseInt(args[6]) : 0;
            if (baseY < -40 || baseY > 280 || Math.abs(originX) > 20_000_000
                    || Math.abs(originZ) > 20_000_000) {
                throw new IllegalArgumentException("candidate origin is outside safe world bounds");
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = WorldCreator.name(worldName).type(WorldType.FLAT)
                        .generateStructures(false).seed(713_071L).createWorld();
            }
            if (world == null) throw new IllegalStateException("could not create or load candidate world");

            UnlitVillageCandidateBuilder builder = new UnlitVillageCandidateBuilder();
            UnlitVillageCandidateBuilder.BuildReport report =
                    builder.buildFresh(world, originX, baseY, originZ);

            plugin.getConfig().set("unlit.world", world.getName());
            plugin.getConfig().set("unlit.border-radius", UnlitVillageCandidateBuilder.BORDER_RADIUS);
            plugin.getConfig().set("unlit.buildmode", false);
            plugin.saveConfig();
            world.getWorldBorder().setCenter(originX, originZ + 13);
            world.getWorldBorder().setSize(UnlitVillageCandidateBuilder.BORDER_RADIUS * 2.0);
            world.setTime(plugin.getConfig().getLong("unlit.night-time", 18_000L));
            world.setSpawnLocation(originX, baseY + 1, originZ + 13);

            boolean sitesPersisted = false;
            plugin.beginRuntimeSiteBatch();
            try {
                registerUnlitCandidateSite(new Site("unlit_entry", "unlit_entry", world.getName(),
                        (double) (originX + 13), (double) (baseY + 1), (double) (originZ + 52),
                        5, 5, true, true));
                registerUnlitCandidateSite(new Site("unlit_spawn_mirror", "unlit_spawn", world.getName(),
                        (double) originX, (double) (baseY + 1), (double) (originZ + 13),
                        5, 5, true, true));
                registerUnlitCandidateSite(new Site("unlit_exit", "unlit_exit", world.getName(),
                        (double) (originX + 17), (double) (baseY + 1), (double) (originZ + 52),
                        5, 5, true, true));
                for (UnlitVillageCandidateBuilder.House house : report.houses()) {
                    Location location = UnlitVillageCandidateBuilder.siteLocation(
                            world, originX, baseY, originZ, house);
                    registerUnlitCandidateSite(new Site(house.siteId(), "unlit_house", world.getName(),
                            location.getX(), location.getY(), location.getZ(),
                            house.siteId().equals("unlit_house_base") ? 9 : 7,
                            6, true, true));
                    stampUnlitClue(location, house.siteId());
                    recordUnlitOrientation(location, house.siteId());
                }
                sitesPersisted = plugin.endRuntimeSiteBatch();
            } catch (RuntimeException failure) {
                plugin.abortRuntimeSiteBatch();
                throw failure;
            }
            if (!sitesPersisted) throw new IllegalStateException("sites.yml batch persistence failed");

            Report install = new V5PhysicalComponentInstaller(plugin).reconcileUnlit(Mode.FRESH_INSTALL);
            requireCleanPhysical("Unlit candidate physical install", install);
            Report audit = new V5PhysicalComponentInstaller(plugin).auditUnlit();
            requireCleanPhysical("Unlit candidate physical audit", audit);
            UnlitAuditSnapshot snapshot = collectUnlitAuditSnapshot();
            if (!snapshot.ready()) {
                throw new IllegalStateException("candidate audit remained blocked: "
                        + String.join("; ", snapshot.blockers()));
            }
            world.save();
            sender.sendMessage("UNLIT_CANDIDATE_BUILD PASS world=" + world.getName()
                    + " houses=" + report.houses().size()
                    + " blocks=" + report.blocksWritten()
                    + " path_cells=" + report.pathCells()
                    + " mechanics=" + audit.addressesConsidered()
                    + " border=" + (UnlitVillageCandidateBuilder.BORDER_RADIUS * 2));
            handleUnlitReady(sender);
        } catch (NumberFormatException failure) {
            sender.sendMessage("UNLIT_CANDIDATE_BUILD BLOCKED coordinates must be integers");
        } catch (IllegalArgumentException | IllegalStateException failure) {
            String detail = failure.getMessage() == null ? failure.getClass().getSimpleName()
                    : failure.getMessage().replace('\n', ' ').replace('\r', ' ');
            sender.sendMessage("UNLIT_CANDIDATE_BUILD BLOCKED " + detail);
        }
    }

    private void registerUnlitCandidateSite(Site site) {
        if (!plugin.registerRuntimeSite(site)) {
            throw new IllegalStateException("could not register " + site.id());
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
        recordUnlitOrientation(player.getLocation(), siteId);
        sender.sendMessage("Observance: stamped " + unlitShortId(siteId) + " clue fixture. " + note);
        reconcileUnlitWhenComplete(sender);
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
        Report physical = new V5PhysicalComponentInstaller(plugin).auditUnlit();
        if (physical.clean()) {
            sender.sendMessage(" exact mechanics: OK (" + physical.addressesConsidered()
                    + " physical authority addresses)");
        } else {
            sender.sendMessage(" exact mechanics: BLOCKED (" + physical.blockerMessages().size() + " fault(s))");
            for (String fault : physical.blockerMessages().stream().limit(8).toList()) {
                sender.sendMessage("   - " + fault);
            }
        }
        var runtime = plugin.v5Runtime();
        // The physical pass above may load previously dormant Hold chunks. Bind those exact
        // fixtures before evaluating readiness so a cold restart audit reflects the now-loaded
        // controls instead of the intentionally deferred startup index.
        if (runtime != null) runtime.rebindAllLoadedFixtures();
        List<String> runtimeFindings = runtime == null
                ? List.of("V5 runtime coordinator is unavailable") : runtime.readinessFindings();
        if (runtimeFindings.isEmpty()) {
            sender.sendMessage(" runtime binding: OK");
        } else {
            sender.sendMessage(" runtime binding: BLOCKED (" + runtimeFindings.size() + " fault(s))");
            for (String fault : runtimeFindings.stream().limit(8).toList()) {
                sender.sendMessage("   - " + fault);
            }
        }
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
        sender.sendMessage("  3) Record the matching rows in design/V5-LIVE-TEST-MATRIX.csv.");
        sender.sendMessage("  4) Capture approach, route, light radius, readable clue, exit, and failed-cheese evidence.");
        sender.sendMessage("  5) Confirm the figure can extinguish exposed light while the return route stays readable.");
        sender.sendMessage("  6) Run /obs unlit audit, /obs unlit ready, then /obs preflight.");
        sender.sendMessage("  Finish line: every required V5 live-test row has a dated PASS receipt.");
    }

    private void handleUnlitRepair(CommandSender sender) {
        List<String> missing = new ArrayList<>();
        for (String siteId : unlitPhysicalSites()) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
            Location location = site == null ? null : site.location();
            if (site == null || !site.isPlaced() || location == null || location.getWorld() == null) {
                missing.add(siteId);
            }
        }
        if (!missing.isEmpty()) {
            sender.sendMessage("Observance: Unlit repair made no changes; place these houses first: "
                    + String.join(", ", missing));
            return;
        }
        V5PhysicalComponentInstaller physical = new V5PhysicalComponentInstaller(plugin);
        Report repair = physical.reconcileUnlit(Mode.STATE_PRESERVING_REPAIR);
        requireCleanPhysical("Unlit state-preserving repair", repair);
        Report audit = physical.auditUnlit();
        requireCleanPhysical("Unlit repair audit", audit);
        var runtime = plugin.v5Runtime();
        if (runtime != null) runtime.rebindAllLoadedFixtures();
        sender.sendMessage("Observance: Unlit state-preserving repair is clean across "
                + audit.addressesConsidered() + " exact authority addresses.");
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

    private static String[] unlitPhysicalSites() {
        return new String[]{
                "unlit_house_lamp",
                "unlit_house_cairn",
                "unlit_house_coop",
                "unlit_house_well",
                "unlit_house_watch",
                "unlit_house_warm",
                "unlit_house_threshold"
        };
    }

    private void recordUnlitOrientation(Location loc, String siteId) {
        if (loc == null || loc.getWorld() == null || siteId == null || siteId.isBlank()) return;
        V5PhysicalComponentInstaller physical = new V5PhysicalComponentInstaller(plugin);
        if (!physical.recordExternalOrientation(siteId, cardinalFacing(loc.getYaw()))) {
            plugin.getLogger().warning("Could not persist V5 approach orientation for " + siteId);
        }
    }

    /**
     * The exact house predicates span seven separately surveyed fixtures. Reconcile only after
     * all seven anchors exist so placing the first house does not report six false failures. The
     * final clue command is the atomic point where decorative staging becomes a complete, audited
     * V5 puzzle lane.
     */
    private void reconcileUnlitWhenComplete(CommandSender sender) {
        for (String siteId : unlitPhysicalSites()) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(siteId);
            Location location = site == null ? null : site.location();
            if (site == null || !site.isPlaced() || location == null || location.getWorld() == null) {
                return;
            }
        }
        V5PhysicalComponentInstaller physical = new V5PhysicalComponentInstaller(plugin);
        Report install = physical.reconcileUnlit(Mode.FRESH_INSTALL);
        requireCleanPhysical("Unlit physical install", install);
        Report audit = physical.auditUnlit();
        requireCleanPhysical("Unlit second-pass physical audit", audit);
        var runtime = plugin.v5Runtime();
        if (runtime != null) runtime.rebindAllLoadedFixtures();
        if (sender != null) {
            sender.sendMessage("Observance: all seven Unlit houses are exact, functional, and audited ("
                    + install.addressesConsidered() + " authority addresses).");
        }
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
        Report physical = new V5PhysicalComponentInstaller(plugin).auditUnlit();
        for (String fault : physical.blockerMessages()) {
            blockers.add("exact mechanics: " + fault);
        }
        var runtime = plugin.v5Runtime();
        if (runtime == null) {
            blockers.add("V5 runtime coordinator is unavailable");
        } else {
            for (String fault : runtime.readinessFindings()) {
                blockers.add("runtime binding: " + fault);
            }
        }

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
            case "lamp" -> (!hasMaterialNear(loc, radius, Material.CHISELED_BOOKSHELF)
                    || !hasMaterialNear(loc, radius, Material.BLACK_CANDLE)
                    || !hasMaterialNear(loc, radius, Material.LEVER))
                    ? "expected wick shelf, black candle, and outage handle" : null;
            case "cairn" -> (!hasMaterialNear(loc, radius, Material.BARREL)
                    || !hasMaterialNear(loc, radius, Material.CAULDRON)
                    || !hasMaterialNear(loc, radius, Material.COBBLED_DEEPSLATE)
                    || !hasMaterialNear(loc, radius, Material.LEVER))
                    ? "expected fragment rail, offering bowl, return stones, and handle" : null;
            case "coop" -> (!hasMaterialNear(loc, radius, Material.BARREL)
                    || !hasMaterialNear(loc, radius, Material.HAY_BLOCK)
                    || !hasMaterialNear(loc, radius, Material.IRON_BARS)
                    || !hasMaterialNear(loc, radius, Material.OAK_FENCE)
                    || !hasMaterialNear(loc, radius, Material.LEVER))
                    ? "expected instrument rail, silent perch, cage bars, hay, and handle" : null;
            case "well" -> (!hasMaterialNear(loc, radius, Material.WATER)
                    || !hasMaterialNear(loc, radius, Material.DARK_PRISMARINE)
                    || !hasMaterialNear(loc, radius, Material.WATER_CAULDRON)
                    || !hasMaterialNear(loc, radius, Material.LEVER))
                    ? "expected exact water trough, copied well bowl, and projection handle" : null;
            case "watch" -> (!hasMaterialNear(loc, radius, Material.BELL)
                    || !hasMaterialNear(loc, radius, Material.BLACK_CARPET)
                    || !hasMaterialNear(loc, radius, Material.POLISHED_BLACKSTONE)
                    || !hasMaterialNear(loc, radius, Material.DARK_OAK_SIGN))
                    ? "expected bell, dark watch marks, and editable official slate" : null;
            case "warm" -> (!hasMaterialNear(loc, radius, Material.BARREL)
                    || !hasMaterialNear(loc, radius, Material.CAMPFIRE)
                    || !hasMaterialNear(loc, radius, Material.RED_WOOL)
                    || !hasMaterialNear(loc, radius, Material.BLUE_ICE)
                    || !hasMaterialNear(loc, radius, Material.LEVER))
                    ? "expected sample tray, unlit hearth, warmth/cold contrast, and handle" : null;
            case "threshold" -> {
                List<String> missing = new ArrayList<>();
                if (!hasMaterialNear(loc, radius, Material.POLISHED_BLACKSTONE)) missing.add("safe-route");
                if (!hasMaterialNear(loc, radius, Material.DEEPSLATE_BRICK_SLAB)) missing.add("low-lintel");
                if (!hasMaterialNear(loc, radius, Material.BLACK_CARPET)) missing.add("black-marks");
                if (!hasMaterialNear(loc, radius, Material.TINTED_GLASS)) missing.add("sealed-outer-decoy");
                if (!hasMaterialNear(loc, radius, Material.LEVER)) missing.add("inner-lever");
                yield missing.isEmpty() ? null : "missing exact threshold elements: " + String.join(",", missing);
            }
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
                setBlock(base, Material.CHISELED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CANDLE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
                // The stopped house clock: a decorative, non-interactive prop set to the exact
                // required rotation. The player finds it, reads it, and copies it onto the real
                // dial elsewhere in the house — replacing a blind 8-way guess with observation.
                placeDecorativeStoppedClock(offsetFrom(base, facing, -2, 1, 1), facing, 7);
                placeStandingSign(offsetFrom(base, facing, -2, 1, 0), facing,
                        new String[]{"STOPPED", "", "", ""});
                placeEvidenceLectern(offsetFrom(base, facing, -1, 0, -1), facing,
                        "lamp maintenance log", List.of(
                                "oil draw steady through bell six.",
                                "next round found the wick cold. no draw logged at all.",
                                "the house dial was never wound back. it still shows the hour it stopped."
                        ));
                return "The exact wick shelf, outage clock, pull handle, stopped-clock prop, and log lectern"
                        + " now carry the lamp proof.";
            }
            case "cairn" -> {
                setBlock(offsetFrom(base, facing, 0, -1, 0), Material.CAULDRON);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.COBBLED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.COBBLED_DEEPSLATE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.COBBLED_DEEPSLATE_SLAB);
                placeEvidenceLectern(offsetFrom(base, facing, -2, 0, -1), facing,
                        "reading a cut face", List.of(
                                "a healed skin means old growth; the stone had years to close over it.",
                                "raw, unweathered dust took no time at all. it is the newest thing in the cut.",
                                "read oldest to newest, deepest first. do not trust which piece was closest to hand."
                        ));
                return "Use as an offering bowl; the return is a shape, not written instructions.";
            }
            case "coop" -> {
                setBlock(base, Material.HAY_BLOCK);
                setBlock(offsetFrom(base, facing, 0, -1, 0), Material.HAY_BLOCK);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.IRON_BARS);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.IRON_BARS);
                setBlock(base.clone().add(0, 1, 0), Material.OAK_FENCE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.LIGHT_GRAY_CARPET);
                placeEvidenceLectern(offsetFrom(base, facing, -2, 0, 0), facing,
                        "coop caretaker's log", List.of(
                                "birds answer any real change inside a few minutes. they always have.",
                                "last confirmed peck and call: well before the alarm.",
                                "the official alarm reads bell eight. compare the two times yourself."
                        ));
                placeUnlitFieldKit(offsetFrom(base, facing, 2, -1, -1), facing,
                        List.of(new UnlitDecoy("bi03_feed_disturbed", Material.WHEAT_SEEDS),
                                new UnlitDecoy("bi03_water_disturbed", Material.POTION)),
                        "coop field samples", "some of these were handled after the fact. sort carefully.");
                return "Sacred-beast clue; the untouched perch, missing call, caretaker's log, and field"
                        + " kit are the evidence.";
            }
            case "well" -> {
                setBlock(offsetFrom(base, facing, 0, -2, 0), Material.WATER_CAULDRON);
                setBlock(offsetFrom(base, facing, 1, -2, 0), Material.POLISHED_BLACKSTONE);
                placeEvidenceLectern(offsetFrom(base, facing, -2, 0, 0), facing,
                        "reading a survey bearing", List.of(
                                "DUE means the plotted mark faces true; hang its copy unrotated.",
                                "QUARTER-TURN means a quarter turn to the right of true.",
                                "REVERSED means turned full about, mark facing back the way it came.",
                                "each map is pinned with its own bearing note. read the note, not the others."
                        ));
                return "The exact three-map water trough and bearing legend are the readable"
                        + " reflection proof.";
            }
            case "watch" -> {
                setBlock(base, Material.BELL);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.POLISHED_BLACKSTONE);
                placeEvidenceLectern(offsetFrom(base, facing, -2, 0, 0), facing,
                        "watch floor procedure", List.of(
                                "the posted copy and the worn original should read the same hour.",
                                "where they differ, the worn original was written first."
                        ));
                return "The exact editable watch slate sits above the official blackstone pedestal,"
                        + " with the comparison procedure posted beside it.";
            }
            case "warm" -> {
                Location hearth = offsetFrom(base, facing, 0, -1, 0);
                setBlock(hearth, Material.CAMPFIRE);
                try {
                    org.bukkit.block.data.BlockData data = hearth.getBlock().getBlockData();
                    if (data instanceof org.bukkit.block.data.Lightable lit) {
                        lit.setLit(false);
                        hearth.getBlock().setBlockData(lit, false);
                    }
                } catch (Throwable ignored) { }
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.RED_WOOL);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLUE_ICE);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.RED_CONCRETE);
                placeEvidenceLectern(offsetFrom(base, facing, -2, 0, -1), facing,
                        "excavation order posting", List.of(
                                "Iss excavation order: Survey 19 / Cycle 4.",
                                "every sample dated before this order argues the surface was already viable.",
                                "a sample dated after it proves nothing about what was true before the cut."
                        ));
                placeUnlitFieldKit(offsetFrom(base, facing, 2, -1, 0), facing,
                        List.of(new UnlitDecoy("bi06_reed_late", Material.VINE),
                                new UnlitDecoy("bi06_water_late", Material.POTION)),
                        "later field samples", "check the survey date against the posted order first.");
                return "False-warmth clue; the too-bright red mark is the lie, and the excavation order"
                        + " is posted for comparison.";
            }
            case "threshold" -> {
                setBlock(base, Material.POLISHED_BLACKSTONE);
                setBlock(base.clone().add(0, 1, 0), Material.DEEPSLATE_BRICK_SLAB);
                // The exact inner lever owns the block directly above the anchor. Keep the low
                // lintel one step into the crouched route so both authored elements remain present.
                setBlock(offsetFrom(base, facing, 0, 1, 1), Material.DEEPSLATE_BRICK_SLAB);
                setBlock(offsetFrom(base, facing, 1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.BLACK_CARPET);
                setBlock(offsetFrom(base, facing, 0, 1, 0), Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
                // A visible worn path along the exact safe route (matches BI07's route cells) so the
                // walk is found by observation, not blind repetition against the door.
                for (int[] cell : new int[][]{{-2, 4}, {-1, 3}, {0, 2}, {0, 1}}) {
                    setBlock(offsetFrom(base, facing, cell[0], cell[1], -1), Material.POLISHED_DEEPSLATE);
                }
                placeStandingSign(offsetFrom(base, facing, 2, 0, 1), facing,
                        new String[]{"OUTER SEAL", "failed from", "outside.", "do not touch."});
                return "Threshold/bow clue; the worn floor marks the safe walk, and the outer seal"
                        + " carries its own warning.";
            }
            case "base" -> {
                setBlock(base, Material.BARREL);
                Location lectern = offsetFrom(base, facing, 1, 0, 0);
                placeReadableLectern(lectern.getBlock(), facing);
                if (lectern.getBlock().getState() instanceof Lectern state) {
                    state.getInventory().clear();
                    state.update(true, false);
                }
                setBlock(offsetFrom(base, facing, -1, 0, 0), Material.CALIBRATED_SCULK_SENSOR);
                return "Surface-copy clue; the exact Eight House Docket mounts here when C04 opens.";
            }
            default -> {
                placeStandingSign(base, facing,
                        new String[]{"unlit house", house, "author clue", "place by hand"});
                return "Unknown house id; stamped a neutral marker.";
            }
        }
    }

    /**
     * A decorative, non-interactive item frame holding a clock fixed at {@code rotationOrdinal}.
     * Carries no {@code v5_control_id} PDC (never satisfies a predicate); it exists to be READ, not
     * operated, so the mechanism's own dial stays the only interactive control.
     */
    private void placeDecorativeStoppedClock(Location backing, BlockFace facing, int rotationOrdinal) {
        World world = backing.getWorld();
        if (world == null) return;
        setBlock(backing, Material.DEEPSLATE_BRICK_WALL);
        Location frameSpawnAt = v5FrameSpawnAnchor(backing, facing);
        try {
            for (ItemFrame existing : world.getNearbyEntitiesByType(ItemFrame.class, frameSpawnAt, 0.6)) {
                existing.remove();
            }
            ItemFrame frame = world.spawn(frameSpawnAt, ItemFrame.class, spawned -> {
                spawned.setFacingDirection(facing, true);
                spawned.setFixed(true);
                spawned.setInvulnerable(true);
                spawned.setPersistent(true);
            });
            ItemStack clock = new ItemStack(Material.CLOCK, 1);
            org.bukkit.inventory.meta.ItemMeta meta = clock.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("a stopped clock").color(NamedTextColor.GRAY));
                clock.setItemMeta(meta);
            }
            frame.setItem(clock, false);
            frame.setRotation(Rotation.values()[Math.floorMod(rotationOrdinal, Rotation.values().length)]);
        } catch (Throwable ignored) {
            // Decorative-only; a placement failure never blocks the required mechanism.
        }
    }

    /** A decoy's evidence-appearance id paired with the material it should visually render as. */
    private record UnlitDecoy(String evidenceId, Material material) { }

    /**
     * A small, unmanaged crate of plausible-but-wrong field samples near (never inside) a house's
     * real evidence tray. Purely cosmetic ItemStacks carrying no predicate PDC — the mechanism's own
     * wrong-item handling already returns them if a player tries to file one, so a decoy only needs
     * to look right, never to be recognized by the evaluator.
     */
    private void placeUnlitFieldKit(Location loc, BlockFace facing, List<UnlitDecoy> decoys,
                                    String label, String warning) {
        World world = loc.getWorld();
        if (world == null) return;
        Block block = loc.getBlock();
        block.setType(Material.BARREL, false);
        if (!(block.getState() instanceof org.bukkit.block.Barrel barrel)) return;
        com.observance.watcher.v5runtime.install.V5EvidenceItemAppearanceAuthority.Catalog appearance =
                com.observance.watcher.v5runtime.install.V5EvidenceItemAppearanceAuthority.loadDefault();
        int slot = 10;
        for (UnlitDecoy decoy : decoys) {
            if (slot > 16) break;
            com.observance.watcher.v5runtime.install.V5EvidenceItemAppearanceAuthority.Entry entry =
                    appearance.get(decoy.evidenceId());
            ItemStack stack = new ItemStack(decoy.material(), 1);
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta != null && entry != null) {
                meta.displayName(Component.text(entry.title()).color(NamedTextColor.GRAY));
                meta.lore(entry.lore().stream()
                        .map(line -> (Component) Component.text(line).color(NamedTextColor.DARK_GRAY))
                        .toList());
                stack.setItemMeta(meta);
            }
            barrel.getInventory().setItem(slot, stack);
            slot++;
        }
        barrel.update(true, false);
        placeStandingSign(offsetFrom(loc, facing, 0, -1, 1), facing,
                new String[]{label.toUpperCase(java.util.Locale.ROOT), warning, "", ""});
    }

    private void placeEvidenceLectern(Location loc, BlockFace facing, String title, List<String> pages) {
        if (loc == null || loc.getWorld() == null) return;
        // Production narrative never comes from these historical template literals.  The exact V5
        // authority synchronizer owns title, author, pages, PDC id, availability, and removal.
        Block block = loc.getBlock();
        org.bukkit.block.BlockState existing = block.getState();
        if (activeHoldBuild != null && existing instanceof org.bukkit.block.TileState tile
                && tile.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey("observance", "v5_node_ids"),
                org.bukkit.persistence.PersistentDataType.STRING)) {
            // A resumed geometry-complete pass may encounter the exact installer projection from a
            // prior interrupted fixture pass. Never replace that tagged V5 container/mount with a
            // retired template lectern; the later reconcile pass will audit and preserve it.
            return;
        }
        placeReadableLectern(block, facing);
        if (block.getState() instanceof Lectern lectern) {
            lectern.getInventory().clear();
            lectern.update(true, false);
        }
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
            case "discord_relay" -> new PlacementBrief(
                    "live-server callback puzzle; the only bridge from Minecraft into the private Discord surface",
                    "stand at the intended south approach near first arrival and run /obs placerelay; keep it separate from the Hold mouth",
                    "roofed shell, flat entrance, all five lecterns readable from the south, copper-age order resolves 9137, ticket 1842 accepts it");
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

        int surveyed = 0, auto = 0, dimensionAnchored = 0, placed = 0, occupied = 0, failed = 0, skippedLanes = 0;
        sender.sendMessage("== placeworld — scattering keepers away from " + baseSource
                + " " + baseX + "," + baseZ + " in " + worldName + " ==");

        for (String[] row : KEEPER_SPINE) {
            String siteId   = row[0];
            String siteType = row[1];
            int radius;
            try { radius = Integer.parseInt(row[2]); } catch (NumberFormatException e) { radius = 8; }

            if (world.getEnvironment() != World.Environment.NORMAL && !isLaneSite(siteId)) continue;

            // 1) Prefer a surveyed anchor (real coords already in sites.yml for this id).
            Site existing = plugin.sites() == null ? null : plugin.sites().get(siteId);
            int ax, ay, az;
            boolean fromSurvey;
            if (existing != null && existing.isPlaced() && existing.location() != null
                    && worldName.equals(existing.worldName())) {
                Location el = existing.location();
                ax = el.getBlockX();
                ay = el.getBlockY();
                az = el.getBlockZ();
                fromSurvey = true;
                surveyed++;
            } else if (isLaneSite(siteId)) {
                if (!dimensionLaneMatchesWorld(siteId, world)) {
                    skippedLanes++;
                    continue;
                }
                Location here = player.getLocation();
                ax = here.getBlockX();
                ay = here.getBlockY();
                az = here.getBlockZ();
                fromSurvey = false;
                dimensionAnchored++;
            } else {
                // 2b) Auto-scatter: a distant, per-keeper anchor far from the base on its own bearing.
                int[] scatter = autoScatterAnchor(baseX, baseZ, siteId);
                ax = scatter[0];
                az = scatter[1];
                ay = world.getHighestBlockYAt(ax, az, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
                fromSurvey = false;
                auto++;
            }
            Location anchor = new Location(world, ax, ay, az);

            // Idempotent occupancy sweep: if a set-piece is already stamped here (re-run at the same anchor),
            // skip the re-place so we never double-stamp. We detect it by the set-piece's signature block at
            // the pillar base column already matching a non-natural placed block.
            if (looksAlreadyPlaced(world, ax, ay, az)) {
                occupied++;
                sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "surveyed" : isLaneSite(siteId) ? "dimension-anchor" : "auto")
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
                sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "surveyed" : isLaneSite(siteId) ? "dimension-anchor" : "auto")
                        + " @ " + ax + "," + ay + "," + az + " -> FAILED (chunk unavailable).");
                continue;
            }

            plugin.registerRuntimeSite(new Site(siteId, siteType, worldName,
                    (double) ax, (double) ay, (double) az, radius, 6, true, true, null, false));
            markDimensionLanePlaced(siteId, sender);
            placed++;
            sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "SURVEYED" : isLaneSite(siteId) ? "DIMENSION-ANCHOR" : "auto-scatter")
                    + " @ " + ax + "," + ay + "," + az + " -> placed.");
        }

        sender.sendMessage("Observance: placeworld complete — " + placed + " placed, " + occupied
                + " occupied, " + failed + " failed, " + skippedLanes + " lane(s) skipped ("
                + surveyed + " surveyed / " + auto + " auto-scattered / " + dimensionAnchored + " dimension-anchored) of "
                + KEEPER_SPINE.length + " keepers.");
        int fixturePlaced = placeSurveyedFixtures(world, worldName, sender);
        if (fixturePlaced > 0) {
            sender.sendMessage("  Surveyed fixtures stamped: " + fixturePlaced + "/" + PLACEWORLD_SURVEY_FIXTURES.length + ".");
        }
        sender.sendMessage("  Scatter is deterministic (same base = same auto anchors). Re-run is idempotent. "
                + "Survey a spot with /observance site set <keeperId> to override an auto anchor.");
        if (skippedLanes > 0 && world.getEnvironment() == World.Environment.NORMAL) {
            sender.sendMessage("  Nether/End lanes: stand at the intended safe anchor IN that dimension and run placeworld there.");
        }
    }

    private boolean dimensionLaneMatchesWorld(String siteId, World world) {
        if (siteId == null || world == null) return false;
        return ("nether_forge".equals(siteId) && world.getEnvironment() == World.Environment.NETHER)
                || ("end_seventh_shrine".equals(siteId) && world.getEnvironment() == World.Environment.THE_END);
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
            int ay = configured.getBlockY();
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
            case "prepare" -> handlePlaceHoldPrepare(sender, args);
            case "plan" -> handlePlaceHoldPlan(sender, args);
            case "build" -> {
                HoldBuildAnchor anchor = resolvePlaceHoldAnchor(sender, args);
                if (anchor == null || anchor.base() == null) return;
                Location base = anchor.base();
                String planKey = holdPlanKey(anchor.surfaceMouth() == null ? base : anchor.surfaceMouth());
                if (activeHoldPreparation != null) {
                    sender.sendMessage("Observance: build refused while Hold chunk preparation is still running ("
                            + activeHoldPreparation.cursor + "/" + activeHoldPreparation.chunks.size() + ").");
                    return;
                }
                if (!planKey.equals(preparedHoldPlanKey)
                        || !holdFootprintChunksReady(anchor.surfaceMouth() == null ? base : anchor.surfaceMouth())) {
                    approvedHoldPlanKey = null;
                    sender.sendMessage("Observance: build refused before mutation. Run /obs placehold prepare, then "
                            + "/obs placehold plan, at this exact Mouth.");
                    return;
                }
                if (approvedHoldPlanKey == null || !approvedHoldPlanKey.equals(planKey)) {
                    sender.sendMessage("Observance: build refused before mutation. Run /obs placehold plan "
                            + "at this exact Mouth in this server session first.");
                    return;
                }
                if (args.length >= 7 && !"+z".equalsIgnoreCase(args[6])) {
                    sender.sendMessage("Observance: build refused. V5 has one canonical orientation: +Z only.");
                    sender.sendMessage("  Run /obs placehold plan first; rotation is intentionally unsupported.");
                    return;
                }
                sender.sendMessage("== Observance Deep Hold V5 ==");
                sender.sendMessage("Building production underground hold at "
                        + base.getWorld().getName() + " "
                        + base.getBlockX() + "," + base.getBlockY() + "," + base.getBlockZ() + ".");
                sender.sendMessage("  Enforced orientation: +Z from the Mouth. Player facing is ignored.");
                if (anchor.surfaceMouth() != null) {
                    Location mouth = anchor.surfaceMouth();
                    sender.sendMessage("  Surface mouth: " + mouth.getBlockX() + ","
                            + mouth.getBlockY() + "," + mouth.getBlockZ()
                            + " (all V5 coordinates descend from this one public entrance).");
                }
                try {
                    startDeepHoldV5Build(base, anchor.surfaceMouth(), sender);
                } catch (IllegalStateException rejected) {
                    approvedHoldPlanKey = null;
                    releaseHoldChunkTickets();
                    String message = rejected.getMessage() == null ? "unknown placement failure" : rejected.getMessage();
                    sender.sendMessage("Observance: Deep Hold V5 build FAILED; it is not production-ready.");
                    sender.sendMessage("  " + message);
                    if (rejected.getCause() != null) sender.sendMessage("  cause: "
                            + rejected.getCause().getClass().getSimpleName() + ": "
                            + rejected.getCause().getMessage());
                    return;
                }
                sender.sendMessage("  Geometry is now applying in bounded tick batches. You may watch progress in chat/console.");
                sender.sendMessage("  Progress is recorded every second; only synchronously saved milestones advance the restart cursor.");
                approvedHoldPlanKey = null;
            }
            case "repair" -> handlePlaceHoldRepair(sender, args);
            case "audit", "status" -> handlePlaceHoldAudit(sender);
            case "seal", "open" -> {
                String gate = args.length >= 3 ? args[2].trim().toLowerCase(Locale.ROOT) : "all";
                boolean sealed = action.equals("seal");
                int changed = applyHoldGateSelection(sender, gate, sealed);
                sender.sendMessage("Observance: Deep Hold " + (sealed ? "sealed " : "opened ")
                        + changed + " gate(s).");
            }
            case "sync" -> syncPlaceHoldGates(sender);
            default -> sender.sendMessage("Usage: /observance placehold <prepare|plan|build|repair|audit|seal|open|sync> "
                    + "[site|gate]  (canonical build orientation is +Z only)");
        }
    }

    /**
     * Generate/load the complete authored envelope through Paper's asynchronous chunk API and keep
     * each result ticketed until the matching plan/build finishes. This is deliberately separate
     * from the read-only survey: neither plan nor build may hide synchronous terrain generation.
     */
    private void handlePlaceHoldPrepare(CommandSender sender, String[] args) {
        HoldBuildAnchor anchor = resolvePlaceHoldAnchor(sender, args);
        if (anchor == null || anchor.base() == null || anchor.base().getWorld() == null) return;
        Location mouth = anchor.surfaceMouth() == null ? anchor.base() : anchor.surfaceMouth();
        String planKey = holdPlanKey(mouth);
        if (activeHoldBuild != null) {
            sender.sendMessage("Observance: preparation refused while a Deep Hold build is applying.");
            return;
        }
        if (activeHoldPreparation != null) {
            sender.sendMessage("Observance: Hold chunk preparation is already running at "
                    + activeHoldPreparation.mouth.getWorld().getName() + " "
                    + activeHoldPreparation.mouth.getBlockX() + ","
                    + activeHoldPreparation.mouth.getBlockY() + ","
                    + activeHoldPreparation.mouth.getBlockZ() + " ("
                    + activeHoldPreparation.cursor + "/" + activeHoldPreparation.chunks.size() + ").");
            return;
        }
        if (planKey.equals(preparedHoldPlanKey) && holdFootprintChunksReady(mouth)) {
            sender.sendMessage("Observance: this exact Hold footprint is already prepared and ticketed. "
                    + "Run /obs placehold plan.");
            return;
        }

        releaseHoldChunkTickets();
        approvedHoldPlanKey = null;
        List<DeepHoldV4Geometry.ChunkCoordinate> chunks = DeepHoldV4Geometry.requiredChunks(mouth);
        if (chunks.isEmpty()) {
            sender.sendMessage("Observance: preparation failed; the Hold footprint could not be resolved.");
            return;
        }
        HoldChunkPreparation preparation = new HoldChunkPreparation(mouth, planKey, chunks, sender);
        activeHoldPreparation = preparation;
        sender.sendMessage("== Observance Deep Hold V5 asynchronous preparation ==");
        sender.sendMessage(" Preparing " + chunks.size() + " footprint chunks one at a time at "
                + mouth.getWorld().getName() + " " + mouth.getBlockX() + ","
                + mouth.getBlockY() + "," + mouth.getBlockZ() + ".");
        sender.sendMessage(" No Hold blocks or site registrations are changed during preparation.");
        prepareNextHoldChunk(preparation);
    }

    private void prepareNextHoldChunk(HoldChunkPreparation preparation) {
        if (preparation == null || activeHoldPreparation != preparation) return;
        if (preparation.cursor >= preparation.chunks.size()) {
            activeHoldPreparation = null;
            preparedHoldPlanKey = preparation.planKey;
            preparation.sender.sendMessage("Observance: PREPARE PASS - all " + preparation.chunks.size()
                    + " chunks are generated, loaded, and ticketed for this exact Mouth.");
            preparation.sender.sendMessage("  Run /obs placehold plan, then build within 30 minutes.");
            String expiryKey = preparedHoldPlanKey;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (expiryKey.equals(preparedHoldPlanKey) && activeHoldBuild == null
                        && activeHoldPreparation == null) {
                    int released = releaseHoldChunkTickets();
                    plugin.getLogger().info("Expired unused Deep Hold preparation and released "
                            + released + " chunk tickets.");
                }
            }, HOLD_PREPARATION_TTL_TICKS);
            return;
        }

        DeepHoldV4Geometry.ChunkCoordinate coordinate = preparation.chunks.get(preparation.cursor);
        World world = preparation.mouth.getWorld();
        if (world == null) {
            failHoldPreparation(preparation, "the selected world unloaded during preparation", null);
            return;
        }
        try {
            world.getChunkAtAsync(coordinate.x(), coordinate.z(), true).whenComplete((chunk, failure) ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (activeHoldPreparation != preparation) return;
                        if (failure != null || chunk == null) {
                            failHoldPreparation(preparation, "chunk " + coordinate.x() + ","
                                    + coordinate.z() + " could not be generated/loaded", failure);
                            return;
                        }
                        try {
                            chunk.addPluginChunkTicket(plugin);
                            holdChunkTickets.add(chunk);
                        } catch (Throwable ticketFailure) {
                            failHoldPreparation(preparation, "chunk " + coordinate.x() + ","
                                    + coordinate.z() + " could not be ticketed", ticketFailure);
                            return;
                        }
                        preparation.cursor++;
                        if (preparation.cursor % 25 == 0
                                || preparation.cursor == preparation.chunks.size()) {
                            preparation.sender.sendMessage("  prepared " + preparation.cursor + "/"
                                    + preparation.chunks.size() + " Hold chunks...");
                        }
                        // Always return through the scheduler between chunks; never recurse through an
                        // already-completed future on the server thread.
                        Bukkit.getScheduler().runTask(plugin, () -> prepareNextHoldChunk(preparation));
                    }));
        } catch (Throwable submissionFailure) {
            failHoldPreparation(preparation, "chunk " + coordinate.x() + ","
                    + coordinate.z() + " could not be submitted to Paper's async loader", submissionFailure);
        }
    }

    private void failHoldPreparation(HoldChunkPreparation preparation, String message, Throwable failure) {
        if (activeHoldPreparation == preparation) activeHoldPreparation = null;
        int released = releaseHoldChunkTickets();
        preparation.sender.sendMessage("Observance: PREPARE FAILED - " + message + ".");
        preparation.sender.sendMessage("  Released " + released + " chunk tickets; no Hold build was started.");
        if (failure != null) plugin.getLogger().severe("Deep Hold chunk preparation failed: "
                + failure.getClass().getSimpleName() + ": " + failure.getMessage());
    }

    private boolean holdFootprintChunksReady(Location mouth) {
        if (mouth == null || mouth.getWorld() == null) return false;
        World world = mouth.getWorld();
        for (DeepHoldV4Geometry.ChunkCoordinate chunk : DeepHoldV4Geometry.requiredChunks(mouth)) {
            if (!world.isChunkGenerated(chunk.x(), chunk.z())
                    || !world.isChunkLoaded(chunk.x(), chunk.z())) return false;
        }
        return true;
    }

    private int releaseHoldChunkTickets() {
        int released = 0;
        for (Chunk chunk : List.copyOf(holdChunkTickets)) {
            try {
                chunk.removePluginChunkTicket(plugin);
                released++;
            } catch (Throwable failure) {
                plugin.getLogger().warning("Could not release Deep Hold chunk ticket at "
                        + chunk.getX() + "," + chunk.getZ() + ": " + failure.getMessage());
            }
        }
        holdChunkTickets.clear();
        preparedHoldPlanKey = null;
        approvedHoldPlanKey = null;
        return released;
    }

    /** Read-only V5 bounds, orientation, terrain, collision, and manifest preview. */
    private void handlePlaceHoldPlan(CommandSender sender, String[] args) {
        HoldBuildAnchor anchor = resolvePlaceHoldAnchor(sender, args);
        if (anchor == null || anchor.base() == null || anchor.base().getWorld() == null) return;
        Location mouth = anchor.surfaceMouth() == null ? anchor.base() : anchor.surfaceMouth();
        World world = mouth.getWorld();
        String planKey = holdPlanKey(mouth);
        if (activeHoldPreparation != null) {
            sender.sendMessage("Observance: plan deferred until asynchronous preparation finishes ("
                    + activeHoldPreparation.cursor + "/" + activeHoldPreparation.chunks.size() + ").");
            return;
        }
        if (!planKey.equals(preparedHoldPlanKey) || !holdFootprintChunksReady(mouth)) {
            approvedHoldPlanKey = null;
            sender.sendMessage("Observance: PLAN REFUSED without mutation. Run /obs placehold prepare "
                    + "at this exact Mouth first; plan never loads or generates chunks.");
            return;
        }
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        List<String> manifestIssues = DeepHoldV5Manifest.validate();
        DeepHoldV4Geometry.Survey survey = DeepHoldV4Geometry.survey(world, mouth);
        List<String> collisionIssues = surveyDeepHoldV4SiteCollisions(mouth);

        sender.sendMessage("== Observance Deep Hold V5 plan (READ ONLY) ==");
        sender.sendMessage(" Mouth: " + world.getName() + " " + bx + "," + by + "," + bz);
        sender.sendMessage(" Orientation: +Z ONLY; forward reaches world Z "
                + (bz + DeepHoldV4Plan.MAX_Z) + ". Player yaw is ignored.");
        sender.sendMessage(" Bounds: X " + (bx + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE)
                + ".." + (bx + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE)
                + ", Y " + (by + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE)
                + ".." + (by + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE)
                + ", Z " + (bz + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE)
                + ".." + (bz + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE) + ".");
        sender.sendMessage(" Manifest: " + DeepHoldV4Plan.ROOMS.size() + " rooms, "
                + DeepHoldV4Plan.FIXTURES.size() + " fixtures, " + DeepHoldV4Plan.GATES.size()
                + " gates, hash=" + DeepHoldV5Manifest.contentHash());
        sender.sendMessage(" Terrain: minSurface=" + survey.minimumSurfaceY()
                + ", highestRoof=" + survey.highestAuthoredRoofY()
                + ", lowestFoundation=" + survey.lowestAuthoredFoundationY() + ".");
        for (String issue : manifestIssues) sender.sendMessage("  MANIFEST FAIL: " + issue);
        for (String issue : survey.issues()) sender.sendMessage("  TERRAIN FAIL: " + issue);
        for (String issue : collisionIssues) sender.sendMessage("  COLLISION FAIL: " + issue);
        boolean safe = manifestIssues.isEmpty() && survey.safe() && collisionIssues.isEmpty();
        approvedHoldPlanKey = safe ? planKey : null;
        if (!safe) {
            int released = releaseHoldChunkTickets();
            sender.sendMessage("  Released " + released + " preparation tickets after the refused plan.");
        }
        sender.sendMessage(safe
                ? " PLAN PASS: no blocks or registrations changed. Build with the exact same Mouth."
                : " PLAN REFUSED: no blocks or registrations changed; choose/fix a different Mouth.");
    }

    private String holdPlanKey(Location mouth) {
        if (mouth == null || mouth.getWorld() == null) return "";
        return mouth.getWorld().getUID() + ":" + mouth.getBlockX() + ":" + mouth.getBlockY() + ":"
                + mouth.getBlockZ() + ":" + DeepHoldV5Manifest.contentHash() + ":"
                + DeepHoldV5Manifest.CANONICAL_ORIENTATION + ":" + DeepHoldV4Plan.GEOMETRY_REVISION;
    }

    /**
     * Rebuild fixture dressing without re-excavating the shell, clearing solved inventories, resetting
     * dial/shelf/sign state, reissuing consumed artifacts, or resealing an open progression gate.
     */
    private void handlePlaceHoldRepair(CommandSender sender, String[] args) {
        Location mouth = resolveDeepHoldV4Mouth();
        if (mouth == null || mouth.getWorld() == null) {
            sender.sendMessage("Observance: V5 Hold Mouth cannot be reconstructed; run placehold plan/build first.");
            return;
        }
        String requested = args.length >= 3 ? args[2].trim().toLowerCase(Locale.ROOT) : "all";
        List<DeepHoldV4Plan.Fixture> fixtures;
        if ("all".equals(requested)) {
            fixtures = DeepHoldV4Plan.FIXTURES;
        } else {
            DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(requested);
            if (fixture == null) {
                sender.sendMessage("Observance: unknown V5 fixture '" + requested + "'.");
                return;
            }
            fixtures = List.of(fixture);
        }

        World world = mouth.getWorld();
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        int repaired = 0;
        plugin.beginRuntimeSiteBatch();
        boolean repairComplete = false;
        try {
            List<String> manifestIssues = DeepHoldV5Manifest.validate();
            if (!manifestIssues.isEmpty()) throw new IllegalStateException(String.join("; ", manifestIssues));
            for (DeepHoldV4Plan.Fixture fixture : fixtures) {
                Location loc = new Location(world, bx + fixture.x(), by + fixture.y(), bz + fixture.z());
                HoldSite row = v4HoldSite(fixture);
                Site live = configuredHoldSite(row, loc, world.getName());
                // V5 repair never invokes legacy fixture writers: they contain retired books,
                // selectors and item-frame state. Architecture is repaired by the shell builder;
                // exact stateful surfaces are reconciled conservatively below.
                ensureHoldAnchorVisual(live, loc);
                plugin.registerRuntimeSite(live);
                repaired++;
            }

            if ("all".equals(requested)) {
                // Preserve each physical latch while restoring gate walls/labels and canonical site metadata.
                for (HoldGate gate : DEEP_HOLD_GATES) {
                    boolean open = holdGateLatchedOpen(gate.id());
                    Location gateLoc = new Location(world, bx + gate.x(), by + gate.y(), bz + gate.z());
                    setHoldGate(gate, gateLoc, !open);
                    placeHoldGateLabel(gate, gateLoc);
                    HoldGateSpan span = holdGateSpan(gate);
                    plugin.registerRuntimeSite(new Site(holdGateSiteId(gate.id()), "hold_gate", world.getName(),
                            gateLoc.getX(), gateLoc.getY(), gateLoc.getZ(), Math.max(8, span.halfAcross()),
                            span.height() + 2, true, true, null, false));
                }
                registerHoldRegionV4(world.getName(), bx, by, bz);
                DeepHoldV4Plan.RecordStation release = holdRecordStation("release_record");
                if (release == null) throw new IllegalStateException("compact release record is missing");
                Location sever = new Location(world, bx + release.x(), by + release.y(), bz + release.z() - 4);
                placeV5SeverControl(sever);
                plugin.registerRuntimeSite(new Site("release_record", "v5_finale_control",
                        world.getName(), sever.getX(), sever.getY(), sever.getZ(),
                        5, 4, true, true, null, false));
            }
            String routeIssue = auditV4OpenRoute(world, mouth);
            if (routeIssue != null) throw new IllegalStateException(routeIssue);
            V5PhysicalComponentInstaller physical = new V5PhysicalComponentInstaller(plugin);
            requireCleanPhysical("state-preserving repair",
                    physical.reconcileHold(mouth, Mode.STATE_PRESERVING_REPAIR));
            requireCleanPhysical("repair readback", physical.auditHold(mouth));
            repairComplete = true;
        } catch (Throwable failure) {
            sender.sendMessage("Observance: V5 state-preserving repair FAILED after " + repaired + " fixture(s): "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        } finally {
            if (!repairComplete) {
                plugin.abortRuntimeSiteBatch();
            } else {
                boolean persisted = plugin.endRuntimeSiteBatch();
                if (!persisted) {
                    repairComplete = false;
                    sender.sendMessage("Observance: repair changes exist in-world but sites.yml failed atomic verification.");
                }
            }
        }
        if (repairComplete) {
            var runtime = plugin.v5Runtime();
            if (runtime != null) runtime.rebindAllLoadedFixtures();
            sender.sendMessage("Observance: V5 repair complete for " + repaired + " fixture(s). Existing block-entity, "
                    + "container, shelf, sign, and frame state was restored; no consumed key item was reissued.");
            sender.sendMessage("  Run /obs placehold audit. Use /obs item recover only for a confirmed lost artifact.");
        }
    }

    private List<org.bukkit.block.BlockState> snapshotHoldTileState(Location loc,
                                                                    DeepHoldV4Plan.Fixture fixture) {
        List<org.bukkit.block.BlockState> out = new ArrayList<>();
        if (loc == null || loc.getWorld() == null || fixture == null) return out;
        int horizontal = Math.max(3, Math.min(12, fixture.radius() + 3));
        int vertical = Math.max(3, Math.min(10, fixture.verticalRadius() + 2));
        for (int dx = -horizontal; dx <= horizontal; dx++) {
            for (int dy = -2; dy <= vertical; dy++) {
                for (int dz = -horizontal; dz <= horizontal; dz++) {
                    org.bukkit.block.BlockState state = loc.getWorld().getBlockAt(
                            loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz).getState();
                    if (state instanceof org.bukkit.block.TileState) out.add(state);
                }
            }
        }
        return out;
    }

    private void restoreHoldTileState(List<org.bukkit.block.BlockState> snapshots) {
        if (snapshots == null) return;
        for (org.bukkit.block.BlockState snapshot : snapshots) {
            try { snapshot.update(true, false); } catch (Throwable ignored) { }
        }
    }

    private record HoldFrameSnapshot(Location location, BlockFace facing, Rotation rotation,
                                     ItemStack item, boolean visible, boolean fixed, boolean invulnerable) { }

    private List<HoldFrameSnapshot> snapshotHoldFrames(Location loc, DeepHoldV4Plan.Fixture fixture) {
        if (loc == null || loc.getWorld() == null || fixture == null) return List.of();
        double radius = Math.max(3.0, Math.min(12.0, fixture.radius() + 3.0));
        List<HoldFrameSnapshot> out = new ArrayList<>();
        for (ItemFrame frame : loc.getWorld().getNearbyEntitiesByType(ItemFrame.class, loc, radius)) {
            out.add(new HoldFrameSnapshot(frame.getLocation().clone(), frame.getFacing(), frame.getRotation(),
                    frame.getItem().clone(), frame.isVisible(), frame.isFixed(), frame.isInvulnerable()));
        }
        return out;
    }

    private void restoreHoldFrames(List<HoldFrameSnapshot> snapshots) {
        if (snapshots == null) return;
        for (HoldFrameSnapshot snapshot : snapshots) {
            if (snapshot.location() == null || snapshot.location().getWorld() == null) continue;
            ItemFrame frame = snapshot.location().getWorld().getNearbyEntitiesByType(
                    ItemFrame.class, snapshot.location(), 0.6).stream().findFirst().orElse(null);
            if (frame == null) {
                frame = snapshot.location().getWorld().spawn(snapshot.location(), ItemFrame.class);
            }
            frame.setFacingDirection(snapshot.facing(), true);
            frame.setItem(snapshot.item().clone(), false);
            frame.setRotation(snapshot.rotation());
            frame.setVisible(snapshot.visible());
            frame.setFixed(snapshot.fixed());
            frame.setInvulnerable(snapshot.invulnerable());
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
                sender.sendMessage("Usage: /observance placehold " + args[1] + " <world> <x> <y> <z>");
                return null;
            }
            Location mouth = new Location(world, x, clampHoldY(world, y), z);
            return new HoldBuildAnchor(mouth, mouth.clone());
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: console needs /observance placehold " + args[1]
                    + " <world> <x> <y> <z>.");
            return null;
        }
        Location here = player.getLocation();
        if (here == null || here.getWorld() == null) {
            sender.sendMessage("Observance: could not resolve your location.");
            return null;
        }
        World world = here.getWorld();
        int mouthY = here.getBlockY();
        Location mouth = new Location(world, here.getBlockX(), mouthY, here.getBlockZ());
        // V4 coordinates are local to the one public Mouth. Three buried strata fold below this
        // point; a read-only survey rejects shallow terrain before any block is changed.
        return new HoldBuildAnchor(mouth.clone(), mouth);
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

    /**
     * Plan the complete shell without mutating blocks, then apply it in bounded main-thread slices.
     * The checkpoint is deliberately conservative: in-progress geometry restarts at zero. Only a
     * synchronous Paper world save may publish a non-zero cursor, so a crash can replay ordered,
     * idempotent writes but never skip writes that were not durably acknowledged by the world.
     */
    private void startDeepHoldV5Build(Location base, Location surfaceMouth, CommandSender sender) {
        Location mouth = surfaceMouth == null ? base : surfaceMouth;
        if (mouth == null || mouth.getWorld() == null) {
            throw new IllegalStateException("Deep Hold Mouth has no loaded world");
        }
        if (activeHoldBuild != null) {
            throw new IllegalStateException("a Deep Hold build is already applying; wait for its receipt");
        }
        World world = mouth.getWorld();
        String planKey = holdPlanKey(mouth);
        if (!planKey.equals(preparedHoldPlanKey) || !holdFootprintChunksReady(mouth)) {
            throw new IllegalStateException("the exact Hold footprint is not prepared and ticketed; run "
                    + "/obs placehold prepare, then plan, at this Mouth");
        }
        DeepHoldV4Geometry.Survey survey = DeepHoldV4Geometry.survey(world, mouth);
        if (!survey.safe()) {
            throw new IllegalStateException("Deep Hold V5 survey rejected this Mouth: "
                    + String.join("; ", survey.issues()));
        }
        List<String> collisionIssues = surveyDeepHoldV4SiteCollisions(mouth);
        if (!collisionIssues.isEmpty()) {
            throw new IllegalStateException("Deep Hold V5 site-envelope survey rejected this Mouth: "
                    + String.join("; ", collisionIssues));
        }
        if (sender != null) {
            sender.sendMessage("  V5 survey passed: minimum surface Y=" + survey.minimumSurfaceY()
                    + ", highest buried roof Y=" + survey.highestAuthoredRoofY()
                    + ", lowest foundation Y=" + survey.lowestAuthoredFoundationY()
                    + "; registered-site envelope clear.");
        }

        List<String> planIssues = DeepHoldV5Manifest.validate();
        if (!planIssues.isEmpty()) {
            throw new IllegalStateException("Deep Hold V5 static manifest failed: " + String.join("; ", planIssues));
        }
        boolean forceReplacementPrefix = holdCheckpointForcesReplacement(mouth);
        DeepHoldV4Geometry.BuildPlan plan = DeepHoldV4Geometry.plan(world, mouth,
                line -> { if (sender != null) sender.sendMessage("  " + line + "..."); },
                forceReplacementPrefix);
        String resumeStatus = restoreHoldBuildCheckpoint(plan);
        persistHoldBuildCheckpoint(plan, activeHoldFixtureShellReady ? "fixtures_ready" : "applying",
                activeHoldFixtureShellReady ? "resuming exact physical install" : null);
        activeHoldBuild = plan;
        if (sender != null) {
            sender.sendMessage("  architecture plan contains " + plan.totalOperations() + " ordered writes"
                    + (plan.cursor() > 0 ? "; resumed at durable cursor " + plan.cursor() : ""));
            if (resumeStatus != null && !resumeStatus.isBlank()) sender.sendMessage("  " + resumeStatus);
        }

        new BukkitRunnable() {
            private int ticks;
            private int lastPercent = -1;
            private boolean geometryDurable;

            @Override
            public void run() {
                DeepHoldV4Geometry.BuildPlan current = activeHoldBuild;
                if (current != plan) {
                    cancel();
                    return;
                }
                try {
                    DeepHoldV4Geometry.BatchResult batch = current.applyBatch(
                            HOLD_BUILD_MAX_OPERATIONS_PER_TICK, HOLD_BUILD_MAX_NANOS_PER_TICK);
                    ticks++;
                    int percent = batch.totalOperations() == 0 ? 100
                            : (int) ((100L * batch.cursor()) / batch.totalOperations());
                    if (sender != null && (lastPercent < 0 || percent >= lastPercent + 5 || batch.complete())) {
                        lastPercent = percent;
                        sender.sendMessage("  Hold architecture " + percent + "% (" + batch.cursor() + "/"
                                + batch.totalOperations() + " writes; " + batch.changedBlocks() + " changed)");
                    }
                    if (batch.complete()) {
                        // A cursor is not durable until Paper has synchronously flushed the chunks
                        // containing those writes. Otherwise a forced stop can restore a complete
                        // cursor over partly-unsaved natural stone.
                        world.save();
                        geometryDurable = true;
                        persistHoldBuildCheckpoint(current,
                                activeHoldFixtureShellReady ? "fixtures_ready" : "geometry_complete", null);
                    } else if (ticks % 20 == 0) {
                        // Applying checkpoints deliberately restore from zero. Ordered writes are
                        // idempotent and replay is safer than Paper's asynchronous save horizon.
                        persistHoldBuildCheckpoint(current, "applying", null);
                    }
                    if (!batch.complete()) return;

                    int placed = finishDeepHoldV5Build(mouth, sender, current);
                    world.save();
                    persistHoldBuildCheckpoint(current, "complete", "fixtures=" + placed);
                    activeHoldFixtureShellReady = false;
                    activeHoldBuild = null;
                    cancel();
                    releaseHoldChunkTickets();
                    try {
                        announceDeepHoldV5Complete(sender, placed);
                    } catch (Throwable announcementFailure) {
                        plugin.getLogger().log(Level.WARNING,
                                "Deep Hold completed, but its optional completion announcement failed: "
                                        + describeFailureChain(announcementFailure), announcementFailure);
                        if (sender != null) sender.sendMessage("Observance: Deep Hold build receipt is COMPLETE; "
                                + "the optional Wren/announcement step needs an operator check.");
                    }
                    return;
                } catch (Throwable failure) {
                    String causeChain = describeFailureChain(failure);
                    // Log the complete throwable before attempting checkpoint I/O or replying to the
                    // command sender. An RCON socket can disappear during this long build; its failed
                    // send must never be able to hide the fixture/cause that stopped production.
                    plugin.getLogger().log(Level.SEVERE,
                            "Deep Hold V5 build failed; no readiness receipt was published. Cause chain: "
                                    + causeChain, failure);
                    try {
                        persistHoldBuildCheckpoint(current,
                                activeHoldFixtureShellReady ? "fixtures_ready"
                                        : (geometryDurable ? "geometry_complete" : "applying"),
                                "failure=" + causeChain);
                    } catch (Throwable checkpointFailure) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Deep Hold failure checkpoint also failed: "
                                        + describeFailureChain(checkpointFailure), checkpointFailure);
                    }
                    activeHoldBuild = null;
                    cancel();
                    releaseHoldChunkTickets();
                    if (sender != null) {
                        try {
                            sender.sendMessage("Observance: Deep Hold V5 build FAILED; no readiness receipt was published.");
                            sender.sendMessage("  " + causeChain);
                            sender.sendMessage("  Full cause/stack: server log. Durable checkpoint: "
                                    + holdBuildCheckpointPath());
                            sender.sendMessage("  Fix the cause, then rerun placehold prepare, plan, and build "
                                    + "at the same Mouth to resume.");
                        } catch (Throwable replyFailure) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Deep Hold failure was logged, but the command sender disconnected: "
                                            + describeFailureChain(replyFailure), replyFailure);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static String describeFailureChain(Throwable failure) {
        if (failure == null) return "unknown failure";
        List<String> parts = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Throwable cursor = failure;
        while (cursor != null && parts.size() < 10 && seen.add(cursor)) {
            String message = cursor.getMessage();
            String clean = message == null || message.isBlank() ? "(no message)"
                    : message.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
            parts.add(cursor.getClass().getSimpleName() + ": " + clean);
            cursor = cursor.getCause();
        }
        if (cursor != null) parts.add("... additional/cyclic cause omitted");
        return String.join(" <- ", parts);
    }

    private Path holdBuildCheckpointPath() {
        return plugin.getDataFolder().toPath().resolve("hold-build-state.properties");
    }

    /** A started cutover must regenerate the identical cleanup prefix after every restart. */
    private boolean holdCheckpointForcesReplacement(Location mouth) {
        Path path = holdBuildCheckpointPath();
        if (mouth == null || mouth.getWorld() == null || !Files.isRegularFile(path)) return false;
        Properties state = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            state.load(input);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot inspect Hold checkpoint replacement mode", failure);
        }
        String origin = state.getProperty("world-id", "") + ":" + state.getProperty("origin-x", "") + ":"
                + state.getProperty("origin-y", "") + ":" + state.getProperty("origin-z", "");
        String expected = mouth.getWorld().getUID() + ":" + mouth.getBlockX() + ":" + mouth.getBlockY()
                + ":" + mouth.getBlockZ();
        if (!expected.equals(origin)) return false;
        if ("true".equalsIgnoreCase(state.getProperty("replacement-cutover", ""))) return true;
        return !DeepHoldV4Plan.GEOMETRY_REVISION.equals(state.getProperty("geometry-revision", "").trim());
    }

    private String restoreHoldBuildCheckpoint(DeepHoldV4Geometry.BuildPlan plan) {
        activeHoldFixtureShellReady = false;
        Path path = holdBuildCheckpointPath();
        if (!Files.isRegularFile(path)) return null;
        Properties state = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            state.load(input);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read durable Hold checkpoint " + path, failure);
        }
        String status = state.getProperty("status", "").trim();
        String checkpointOrigin = state.getProperty("world-id", "") + ":"
                + state.getProperty("origin-x", "") + ":" + state.getProperty("origin-y", "") + ":"
                + state.getProperty("origin-z", "");
        String planOrigin = plan.worldId() + ":" + plan.originX() + ":" + plan.originY() + ":" + plan.originZ();
        if (!planOrigin.equals(checkpointOrigin)) {
            throw new IllegalStateException("Hold checkpoint belongs to a different Mouth/world; preserve "
                    + path + " and do not replace it here");
        }
        String revision = state.getProperty("geometry-revision", "").trim();
        if (!DeepHoldV4Plan.GEOMETRY_REVISION.equals(revision)) {
            if (!plan.replacingSupersededShell()) {
                throw new IllegalStateException("legacy Hold checkpoint exists at this Mouth, but the prepared world "
                        + "does not prove the superseded shell; no receipt was archived and no blocks were changed");
            }
            Path archived = path.resolveSibling(path.getFileName() + ".superseded-" + System.currentTimeMillis());
            try {
                Files.move(path, archived, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unavailable) {
                try {
                    Files.move(path, archived);
                } catch (IOException failure) {
                    throw new IllegalStateException("cannot archive superseded Hold checkpoint " + path,
                            failure);
                }
            } catch (IOException failure) {
                throw new IllegalStateException("cannot archive superseded Hold checkpoint " + path, failure);
            }
            return "REPLACEMENT: archived superseded " + status + " checkpoint as " + archived.getFileName()
                    + "; compact cutover starts at cursor 0";
        }
        if ("complete".equals(status)) {
            throw new IllegalStateException("this exact compact Hold already has a completed build receipt; use "
                    + "/obs placehold audit or repair, not build");
        }
        String expectedIdentity = plan.worldId() + ":" + plan.originX() + ":" + plan.originY() + ":"
                + plan.originZ() + ":" + DeepHoldV5Manifest.contentHash() + ":" + plan.totalOperations();
        String actualIdentity = state.getProperty("world-id", "") + ":"
                + state.getProperty("origin-x", "") + ":" + state.getProperty("origin-y", "") + ":"
                + state.getProperty("origin-z", "") + ":" + state.getProperty("authority-hash", "") + ":"
                + state.getProperty("total-operations", "");
        if (!expectedIdentity.equals(actualIdentity)) {
            throw new IllegalStateException("a partial Hold checkpoint belongs to a different Mouth, world, "
                    + "authority hash, or operation plan. Preserve and resolve " + path
                    + " before attempting another placement");
        }
        if (!"applying".equals(status) && !"geometry_complete".equals(status)
                && !"fixtures_ready".equals(status)) {
            throw new IllegalStateException("Hold checkpoint has unsupported status '" + status + "'");
        }
        int cursor;
        try {
            cursor = Integer.parseInt(state.getProperty("cursor", "-1"));
        } catch (NumberFormatException badCursor) {
            throw new IllegalStateException("Hold checkpoint cursor is not an integer", badCursor);
        }
        plan.restoreCursor(cursor);
        if (("geometry_complete".equals(status) || "fixtures_ready".equals(status)) && !plan.complete()) {
            throw new IllegalStateException("Hold checkpoint claims complete geometry before its final cursor");
        }
        activeHoldFixtureShellReady = "fixtures_ready".equals(status);
        return "RESUME: durable " + status + " checkpoint verified; already acknowledged "
                + cursor + " ordered writes.";
    }

    private void persistHoldBuildCheckpoint(DeepHoldV4Geometry.BuildPlan plan, String status, String note) {
        Path target = holdBuildCheckpointPath();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Properties state = new Properties();
        state.setProperty("schema", "1");
        state.setProperty("status", status);
        state.setProperty("world-id", plan.worldId().toString());
        state.setProperty("origin-x", Integer.toString(plan.originX()));
        state.setProperty("origin-y", Integer.toString(plan.originY()));
        state.setProperty("origin-z", Integer.toString(plan.originZ()));
        state.setProperty("authority-hash", DeepHoldV5Manifest.contentHash());
        state.setProperty("orientation", DeepHoldV5Manifest.CANONICAL_ORIENTATION);
        state.setProperty("geometry-revision", DeepHoldV4Plan.GEOMETRY_REVISION);
        state.setProperty("replacement-cutover", Boolean.toString(plan.replacingSupersededShell()));
        state.setProperty("total-operations", Integer.toString(plan.totalOperations()));
        int durableCursor = "applying".equals(status) ? 0 : plan.cursor();
        state.setProperty("cursor", Integer.toString(durableCursor));
        state.setProperty("changed-this-process", Long.toString(plan.changedBlocks()));
        state.setProperty("updated-at", Instant.now().toString());
        if (note != null && !note.isBlank()) state.setProperty("note", note.replace('\n', ' '));
        try {
            Files.createDirectories(target.getParent());
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                state.store(output, "Observance V5 resumable Deep Hold build receipt");
                output.getFD().sync();
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot atomically persist Hold build checkpoint " + target, failure);
        }
    }

    private void announceDeepHoldV5Complete(CommandSender sender, int placed) {
        if (sender == null) return;
        sender.sendMessage("Observance: Deep Hold build complete - " + placed + "/"
                + DeepHoldV4Plan.FIXTURES.size() + " canonical ARG sites registered inside the V5 Hold.");
        sender.sendMessage("  V5 content hash: " + DeepHoldV5Manifest.contentHash());
        sender.sendMessage("  Initial gates: G1-G6 plus Prior and Dread are sealed; the Mouth and Grand Stair remain open.");
        sender.sendMessage("  First report / first marker remain prologue setup, not production Hold rooms.");
        Site wrenAnchor = plugin.sites() == null ? null : plugin.sites().get("npc_wren_anchor");
        Location wrenAt = wrenAnchor == null ? null : wrenAnchor.location();
        if (plugin.wren() != null && plugin.wren().body() == null
                && wrenAt != null && wrenAt.getWorld() != null) {
            plugin.wren().spawn(wrenAt);
            sender.sendMessage("  Wren anchored in the lower-vault chamber from npc_wren_anchor.");
        }
        sender.sendMessage("  Next: /obs placehold audit, then /obs placehold sync once Supabase flags are live.");
    }

    /** Final synchronous fixture pass after the shell has completely reached its durable cursor. */
    private int finishDeepHoldV5Build(Location mouth, CommandSender sender,
                                      DeepHoldV4Geometry.BuildPlan plan) {
        if (mouth == null || mouth.getWorld() == null) return 0;
        DeepHoldV4Geometry.BuildResult shell = plan.result();
        World world = mouth.getWorld();
        String worldName = world.getName();
        int bx = mouth.getBlockX();
        int by = mouth.getBlockY();
        int bz = mouth.getBlockZ();

        plugin.beginRuntimeSiteBatch();
        boolean buildComplete = false;
        try {
            // Fail before changing blocks when a packaged manuscript or static plan is incomplete.
            List<String> planIssues = DeepHoldV5Manifest.validate();
            if (!planIssues.isEmpty()) {
                throw new IllegalStateException("Deep Hold V5 static manifest failed: " + String.join("; ", planIssues));
            }

            if (sender != null) sender.sendMessage("  architecture changed " + shell.changedBlocks()
                    + " blocks across " + shell.rooms() + " owned rooms.");

            if (plan.replacingSupersededShell()) {
                int removed = removeSupersededHoldEntities(world, mouth);
                if (plugin.wren() != null) plugin.wren().despawn();
                if (sender != null) sender.sendMessage("  compact cutover removed " + removed
                        + " retired fixture/display entities and closed every old wing.");
            }

            int placed = 0;
            List<Site> pendingSites = new ArrayList<>();
            if (!activeHoldFixtureShellReady) {
                for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
                    HoldSite row = v4HoldSite(fixture);
                    Location loc = new Location(world, bx + fixture.x(), by + fixture.y(), bz + fixture.z());
                    try {
                        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                            throw new IllegalStateException("prepared footprint chunk unloaded before fixture placement");
                        }
                        Site live = configuredHoldSite(row, loc, worldName);
                        placeHoldFixture(live, loc, row);
                        carveV4FixtureFrame(world, mouth, fixture);
                        ensureHoldAnchorVisual(live, loc);
                        String fixtureIssue = auditPlacedSite(live, loc);
                        if (fixtureIssue != null) throw new IllegalStateException(fixtureIssue);
                        String freshIssue = auditV5FreshFixtureContent(live, loc);
                        if (freshIssue != null) throw new IllegalStateException(freshIssue);
                        String frameIssue = auditV4FixtureFrame(world, mouth, fixture);
                        if (frameIssue != null) throw new IllegalStateException(frameIssue);
                        // Legacy builders retain excellent room-scale dressing but also emit retired
                        // books, signs, controls, and frames. Fresh V5 builds keep the architecture
                        // and strip every old readable/mechanical payload before exact authority is
                        // installed below.
                        stripRetiredV4FixtureContent(loc, fixture);
                        pendingSites.add(live);
                        placed++;
                        if (sender != null && (placed % 10 == 0
                                || placed == DeepHoldV4Plan.FIXTURES.size())) {
                            sender.sendMessage("  placed and framed " + placed + "/"
                                    + DeepHoldV4Plan.FIXTURES.size() + " canonical fixtures...");
                        }
                    } catch (Throwable t) {
                        if (sender != null) sender.sendMessage("  [!] V5 stopped at fixture " + fixture.id()
                                + "; this fresh-location build is NOT production-ready.");
                        throw new IllegalStateException("Deep Hold V5 fixture failed: " + fixture.id(), t);
                    }
                }

                // These two V5-neutral civic surfaces are placed only after every fixture's retired
                // payload has been stripped, so later cleanup cannot erase their wayfinding/mounts.
                placeHoldPrologueEchoV4(world, bx, by, bz);
                placeHoldDistrictRecordsV4(world, bx, by, bz);

                placeHoldWayfindingV4(world, bx, by, bz);
                String routeIssue = auditV4OpenRoute(world, mouth);
                if (routeIssue != null) throw new IllegalStateException(routeIssue);
            } else {
                // The prior pass durably completed all legacy/civic mutations. Reconstruct only
                // registration objects; replaying builders here could overwrite exact V5 items.
                for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
                    HoldSite row = v4HoldSite(fixture);
                    Location loc = new Location(world, bx + fixture.x(), by + fixture.y(), bz + fixture.z());
                    pendingSites.add(configuredHoldSite(row, loc, worldName));
                    placed++;
                }
                if (sender != null) sender.sendMessage("  resumed after durable fixtures_ready milestone; "
                        + "legacy fixture mutations were not replayed.");
            }

            DeepHoldV4Plan.Fixture wrenOwner = DeepHoldV4Plan.fixture("threshold_vault");
            if (wrenOwner == null) throw new IllegalStateException("V5 Wren owner fixture is missing");
            pendingSites.add(new Site("npc_wren_anchor", "npc_anchor", worldName,
                    (double) (bx + wrenOwner.standX()), (double) (by + wrenOwner.standY()),
                    (double) (bz + wrenOwner.standZ()), 2, 3, true, true, null, false));
            DeepHoldV4Plan.RecordStation release = holdRecordStation("release_record");
            if (release == null) throw new IllegalStateException("compact release record is missing");
            pendingSites.add(new Site("release_record", "v5_finale_control", worldName,
                    (double) (bx + release.x()), (double) (by + release.y()),
                    (double) (bz + release.z() - 4), 5, 4, true, true, null, false));

            // Stamp progression gates only after the complete open route and every standing frame pass.
            for (HoldGate gate : DEEP_HOLD_GATES) {
                Location gateLoc = new Location(world, bx + gate.x(), by + gate.y(), bz + gate.z());
                HoldGateSpan span = holdGateSpan(gate);
                pendingSites.add(new Site(holdGateSiteId(gate.id()), "hold_gate", worldName,
                        gateLoc.getX(), gateLoc.getY(), gateLoc.getZ(),
                        Math.max(8, span.halfAcross()), span.height() + 2,
                        true, true, null, false));
                if (!activeHoldFixtureShellReady) {
                    setHoldGate(gate, gateLoc, true);
                    placeHoldGateLabel(gate, gateLoc);
                }
                String issue = auditHoldGateIntegrity(gate, gateLoc);
                if (issue != null) throw new IllegalStateException(issue);
                String labelIssue = auditHoldGateLabel(gate, gateLoc);
                if (labelIssue != null) throw new IllegalStateException(labelIssue);
            }

            // From this point forward only exact-authority reconciliation and registration remain.
            // Persist before either can fail so recovery never replays legacy blocks over a partial
            // exact install or duplicates a protected source item.
            world.save();
            persistHoldBuildCheckpoint(plan, "fixtures_ready", "legacy fixture shell and gates complete");
            activeHoldFixtureShellReady = true;

            registerHoldRegionV4(worldName, bx, by, bz);
            for (Site pending : pendingSites) plugin.registerRuntimeSite(pending);
            if (plugin.wren() != null && plugin.wren().body() == null) {
                Site wrenSite = pendingSites.stream()
                        .filter(site -> "npc_wren_anchor".equals(site.id())).findFirst().orElse(null);
                if (wrenSite != null && wrenSite.location() != null) plugin.wren().spawn(wrenSite.location());
            }
            // This build has not admitted players yet. Legacy fixture writers and an interrupted
            // exact pass can both leave readable/source payloads in obsolete containers. Clear
            // only authority-bearing inventory items inside the managed Hold envelope, then let
            // the deterministic physical installer and book projection issue one exact copy.
            clearPreAdmissionHoldInventoryPayloads(world, mouth);
            V5PhysicalComponentInstaller physical = new V5PhysicalComponentInstaller(plugin);
            Report install = physical.reconcileHold(mouth, Mode.FRESH_INSTALL);
            requireCleanPhysical("fresh physical install", install);
            Set<String> protectedBookCells = physical.protectedBookMountCells(mouth);
            Set<String> exactPhysicalCells = physical.exactPhysicalComponentCells(mouth);
            List<String> mountIssues = ensureV5BookMounts(mouth, protectedBookCells, exactPhysicalCells);
            if (!mountIssues.isEmpty()) throw new IllegalStateException("V5 exact book mount install failed: "
                    + String.join("; ", mountIssues));
            // The physical authority owns each lectern block while syncV5Books owns only the
            // authored book inside it. Install and tag those mounts before asking the book
            // projection to resolve them; otherwise a genuinely fresh world reports every
            // locked book as missing even though its mount is about to be created below.
            BookSyncResult initialBooks = syncV5Books(Map.of());
            List<String> holdBookIssues = initialBooks.issues().stream()
                    .filter(issue -> !issue.startsWith("unlit_house_docket"))
                    .toList();
            if (!holdBookIssues.isEmpty()) throw new IllegalStateException("V5 exact book mounts failed: "
                    + String.join("; ", holdBookIssues));
            if (sender != null) sender.sendMessage("  bound " + initialBooks.resolved()
                    + "/38 physical V5 book placements; locked books remain absent until their flags open.");
            Report stabilized = physical.reconcileHold(mouth, Mode.FRESH_INSTALL);
            requireCleanPhysical("post-mount stabilization", stabilized);
            List<String> stabilizedMountIssues = ensureV5BookMounts(
                    mouth, protectedBookCells, exactPhysicalCells);
            if (!stabilizedMountIssues.isEmpty()) throw new IllegalStateException(
                    "V5 stabilized book mount install failed: " + String.join("; ", stabilizedMountIssues));
            BookSyncResult stabilizedBooks = syncV5Books(Map.of());
            List<String> stabilizedBookIssues = stabilizedBooks.issues().stream()
                    .filter(issue -> !issue.startsWith("unlit_house_docket"))
                    .toList();
            if (!stabilizedBookIssues.isEmpty()) throw new IllegalStateException(
                    "V5 stabilized exact book mounts failed: " + String.join("; ", stabilizedBookIssues));
            clearRetiredHoldWrittenBooks(world, mouth);
            Report physicalAudit = physical.auditHold(mouth);
            requireCleanPhysical("second-pass physical audit", physicalAudit);
            String finalRouteIssue = auditV4OpenRoute(world, mouth);
            if (finalRouteIssue != null) throw new IllegalStateException(finalRouteIssue);
            List<String> postPlacementIssues = auditV5PostPlacement(world, mouth);
            if (!postPlacementIssues.isEmpty()) {
                throw new IllegalStateException("V5 final world readback failed: "
                        + String.join("; ", postPlacementIssues));
            }
            if (sender != null) sender.sendMessage("  exact V5 physical pass installed/audited "
                    + install.addressesConsidered() + " authority addresses with "
                    + install.seeded() + " protected source items; no retired written book survived.");
            buildComplete = true;
            return placed;
        } finally {
            if (!buildComplete) {
                plugin.abortRuntimeSiteBatch();
            } else {
                boolean persisted = plugin.endRuntimeSiteBatch();
                if (!persisted) {
                    throw new IllegalStateException("Deep Hold V5 blocks were placed, but sites.yml did not "
                            + "persist and verify. Do not admit players; fix storage and run placehold repair.");
                }
                var runtime = plugin.v5Runtime();
                if (runtime != null) runtime.rebindAllLoadedFixtures();
            }
        }
    }

    private void requireCleanPhysical(String phase, Report report) {
        if (report != null && report.clean()) return;
        List<String> faults = report == null ? List.of("physical report was null")
                : report.blockerMessages().stream().limit(12).toList();
        throw new IllegalStateException("V5 " + phase + " failed: " + String.join("; ", faults));
    }

    private void stripRetiredV4FixtureContent(Location center, DeepHoldV4Plan.Fixture fixture) {
        if (center == null || center.getWorld() == null || fixture == null) return;
        World world = center.getWorld();
        int radius = Math.max(4, Math.min(12, fixture.radius() + 2));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= Math.min(8, fixture.verticalRadius() + 2); dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(center.getBlockX() + dx,
                            center.getBlockY() + dy, center.getBlockZ() + dz);
                    org.bukkit.block.BlockState state = block.getState();
                    if (state instanceof Sign) {
                        block.setType(Material.AIR, false);
                        continue;
                    }
                    if (state instanceof InventoryHolder holder) {
                        holder.getInventory().clear();
                        state.update(true, false);
                    }
                    Material material = block.getType();
                    String name = material.name();
                    if (material == Material.LEVER || material == Material.TRIPWIRE_HOOK
                            || material == Material.REPEATER || material == Material.COMPARATOR
                            || material == Material.TARGET || material == Material.DAYLIGHT_DETECTOR
                            || name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE")) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (Entity entity : new ArrayList<>(world.getNearbyEntities(center,
                radius + 1.0, Math.min(9.0, fixture.verticalRadius() + 3.0), radius + 1.0))) {
            if (entity instanceof ItemFrame || entity instanceof org.bukkit.entity.ItemDisplay
                    || entity instanceof org.bukkit.entity.Interaction
                    || entity instanceof org.bukkit.entity.Marker) {
                entity.remove();
            }
        }
    }

    private void clearPreAdmissionHoldInventoryPayloads(World world, Location mouth) {
        if (world == null || mouth == null) return;
        int minX = mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE;
        int maxX = mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE;
        int minY = mouth.getBlockY() + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE;
        int maxY = mouth.getBlockY() + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE;
        int minZ = mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE;
        int maxZ = mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE;
        List<String> identityKeys = List.of("v5_artifact_id", "artifact_id", "v5_evidence_id",
                "v5_receipt_id", "book_id", "v5_book_id");
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                int x = state.getX(), y = state.getY(), z = state.getZ();
                if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ
                        || !(state instanceof InventoryHolder holder)) continue;
                Inventory inventory = holder.getInventory();
                boolean changed = false;
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);
                    if (item == null || item.getType().isAir()) continue;
                    boolean authorityPayload = item.getType() == Material.WRITTEN_BOOK;
                    if (!authorityPayload && item.hasItemMeta()) {
                        var pdc = item.getItemMeta().getPersistentDataContainer();
                        authorityPayload = identityKeys.stream().anyMatch(key -> pdc.has(
                                new org.bukkit.NamespacedKey(plugin, key),
                                org.bukkit.persistence.PersistentDataType.STRING));
                    }
                    if (authorityPayload) {
                        inventory.setItem(slot, null);
                        changed = true;
                    }
                }
                if (changed) state.update(true, false);
            }
        }
    }

    /** Remove only legacy/unidentified written books after exact V5 sources and mounts settle. */
    private void clearRetiredHoldWrittenBooks(World world, Location mouth) {
        if (world == null || mouth == null) return;
        V5EvidenceItemTextAuthority.Catalog evidenceTexts = V5EvidenceItemTextAuthority.loadDefault();
        int minX = mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE;
        int maxX = mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE;
        int minY = mouth.getBlockY() + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE;
        int maxY = mouth.getBlockY() + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE;
        int minZ = mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE;
        int maxZ = mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE;
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (org.bukkit.block.BlockState snapshot : chunk.getTileEntities()) {
                int x = snapshot.getX(), y = snapshot.getY(), z = snapshot.getZ();
                if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) continue;
                org.bukkit.block.BlockState live = snapshot.getBlock().getState();
                if (!(live instanceof InventoryHolder holder)) continue;
                Inventory inventory = holder.getInventory();
                boolean changed = false;
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);
                    if (item == null || item.getType() != Material.WRITTEN_BOOK || !item.hasItemMeta()) continue;
                    var pdc = item.getItemMeta().getPersistentDataContainer();
                    String bookId = currentV5BookId(item);
                    String artifactId = pdc.get(new org.bukkit.NamespacedKey(plugin, "artifact_id"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    if (artifactId == null) artifactId = pdc.get(
                            new org.bukkit.NamespacedKey(plugin, "v5_artifact_id"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    String evidenceId = pdc.get(new org.bukkit.NamespacedKey(plugin, "v5_evidence_id"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    boolean recognized = bookId != null && V5AuthorityManifest.book(bookId) != null;
                    recognized |= artifactId != null
                            && CanonicalArtifactRegistry.resolveId(artifactId) != null;
                    recognized |= evidenceId != null && evidenceTexts.get(evidenceId) != null;
                    if (!recognized) {
                        inventory.setItem(slot, null);
                        changed = true;
                    }
                }
                if (changed) live.update(true, false);
            }
        }
    }

    private HoldSite v4HoldSite(DeepHoldV4Plan.Fixture fixture) {
        return new HoldSite(fixture.id(), fixture.type(), fixture.radius(), fixture.verticalRadius(),
                fixture.x(), fixture.y(), fixture.z(), 0, 0);
    }

    /**
     * Read-only registered-site collision pass. A rebuild at the exact same V4 Mouth may replace its
     * own generated registrations, but it still refuses to erase a newly authored external site.
     */
    private List<String> surveyDeepHoldV4SiteCollisions(Location mouth) {
        if (mouth == null || mouth.getWorld() == null || plugin.sites() == null) return List.of();
        int minX = mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE;
        int maxX = mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE;
        int minY = mouth.getBlockY() + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE;
        int maxY = mouth.getBlockY() + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE;
        int minZ = mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE;
        int maxZ = mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE;
        List<String> issues = new ArrayList<>();
        for (Site site : plugin.sites().all()) {
            if (site == null || !site.enabled() || !site.isPlaced()) continue;
            // Managed rows are replacement-owned. Their stale legacy coordinates must not prevent
            // the operator selecting the same physical Mouth for the compact cutover.
            if (isDeepHoldV4ManagedSite(site.id())) continue;
            Location loc = site.location();
            if (loc == null || loc.getWorld() != mouth.getWorld()) continue;
            int radius = Math.max(0, site.radius());
            int vertical = Math.max(0, site.verticalRadius());
            boolean x = loc.getX() + radius >= minX && loc.getX() - radius <= maxX;
            boolean y = loc.getY() + vertical >= minY && loc.getY() - vertical <= maxY;
            boolean z = loc.getZ() + radius >= minZ && loc.getZ() - radius <= maxZ;
            if (x && y && z) {
                issues.add("registered site " + site.id() + " intersects the authored envelope");
                if (issues.size() >= 8) break;
            }
        }
        return List.copyOf(issues);
    }

    private boolean isDeepHoldV4ManagedSite(String id) {
        if (id == null) return false;
        if (HOLD_REGION_SITE_ID.equals(id) || HOLD_ENTRY_REGION_SITE_ID.equals(id)
                || "deep_hold_surface_mouth".equals(id) || "npc_wren_anchor".equals(id)
                || "release_record".equals(id)
                || id.startsWith("hold_gate_") || id.startsWith("hold_answer_")) return true;
        return DeepHoldV4Plan.fixture(id) != null;
    }

    private String auditV4FixtureFrame(World world, Location mouth, DeepHoldV4Plan.Fixture fixture) {
        if (world == null || mouth == null || fixture == null) return "missing V5 fixture frame input";
        List<DeepHoldV4Plan.ApproachCell> cells = DeepHoldV4Plan.approachCells(fixture);
        for (int i = 0; i < cells.size(); i++) {
            DeepHoldV4Plan.ApproachCell cell = cells.get(i);
            int x = mouth.getBlockX() + cell.x();
            int y = mouth.getBlockY() + cell.y();
            int z = mouth.getBlockZ() + cell.z();
            Block floor = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (floor.isPassable() || !feet.isPassable() || !head.isPassable()) {
                String zone = i == 0 ? "player standing zone" : "approach";
                return fixture.id() + " has a blocked or unsupported " + zone + " at "
                        + x + "," + y + "," + z + " [floor=" + floor.getType()
                        + ", feet=" + feet.getType() + ", head=" + head.getType() + "]";
            }
        }
        return null;
    }

    /**
     * Reassert the manifest-owned player body lane after a fixture is dressed. This clears only
     * feet/head cells outside the fixture's own footprint; floor support remains architecture-owned
     * and therefore still fails the readback instead of being silently synthesized here.
     */
    private void carveV4FixtureFrame(World world, Location mouth, DeepHoldV4Plan.Fixture fixture) {
        if (world == null || mouth == null || fixture == null) return;
        for (DeepHoldV4Plan.ApproachCell cell : DeepHoldV4Plan.approachCells(fixture)) {
            int x = mouth.getBlockX() + cell.x();
            int y = mouth.getBlockY() + cell.y();
            int z = mouth.getBlockZ() + cell.z();
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (!feet.isPassable()) feet.setType(Material.AIR, false);
            if (!head.isPassable()) head.setType(Material.AIR, false);
        }
    }

    /** Exact V5 fresh-build checks for stateful/key-content surfaces; later audits tolerate consumed items. */
    private String auditV5FreshFixtureContent(Site site, Location loc) {
        if (site == null || loc == null || loc.getWorld() == null) return "V5 content audit has no fixture location";
        String id = site.id();
        // These anchors are intentionally replaced by exact predicate components during the later
        // installer pass. At builder time, however, require the authored visual focal block so a
        // broken builder cannot be masked by the final overlay.
        Material freshAnchor = switch (id) {
            case "vaun_hoard_chest" -> Material.POLISHED_DEEPSLATE;
            case "stone_of_reckoning" -> Material.CHISELED_TUFF;
            case "threshold_vault" -> Material.STONE_PRESSURE_PLATE;
            case "the_unwriting" -> Material.SCULK_SHRIEKER;
            default -> null;
        };
        if (freshAnchor != null && loc.getBlock().getType() != freshAnchor) {
            return "V5 " + id + " fresh builder anchor expected " + freshAnchor
                    + ", found " + loc.getBlock().getType();
        }
        if ("mara_lectern".equals(site.type()) || "sella_lectern".equals(site.type())) {
            Block block = loc.getBlock();
            if (block.getType() != Material.LECTERN || !(block.getState() instanceof Lectern lectern)) {
                return "V5 " + id + " is not an exact lectern";
            }
            if (!(block.getBlockData() instanceof Directional directional)
                    || directional.getFacing() != holdFixtureFront(id)) {
                return "V5 " + id + " lectern facing does not match its manifest front";
            }
            ItemStack book = lectern.getInventory().getItem(0);
            String expectedBook = id.startsWith("mara_lectern_")
                    ? "mara_manual_edition_" + trailingIndex(id, 1)
                    : switch (trailingIndex(id, 1)) {
                        case 1 -> "sella_shore_copybook";
                        case 4 -> "sella_sample_note";
                        default -> null;
                    };
            if (expectedBook == null) {
                if (book != null && book.getType() != Material.AIR) return "V5 " + id
                        + " must remain available for its non-book overlay/reward mechanic";
            } else {
                String issue = auditV5BookItem(book, expectedBook);
                if (issue != null) return "V5 " + id + ": " + issue;
            }
        }
        if ("vaun_bookshelf".equals(id)) {
            Block shelf = loc.getBlock();
            if (shelf.getType() != Material.CHISELED_BOOKSHELF
                    || !(shelf.getBlockData() instanceof Directional directional)
                    || directional.getFacing() != holdFixtureFront(id)) {
                return "V5 Vaun mechanic shelf is missing or incorrectly oriented";
            }
        }
        if (id.startsWith("orin_frame_dial_")) {
            int index = trailingIndex(id, 1);
            int[] initialRotations = {2, 6, 0, 4, 2, 6};
            Location framePlane = v5FramePlane(loc.clone().add(0, 1, 0), holdFixtureFront(id));
            List<ItemFrame> frames = new ArrayList<>(loc.getWorld().getNearbyEntitiesByType(
                    ItemFrame.class, framePlane, 0.35));
            if (frames.size() != 1) return "V5 " + id + " expected exactly one frame, found " + frames.size();
            ItemFrame frame = frames.get(0);
            ItemStack compass = frame.getItem();
            String control = compass.hasItemMeta() ? compass.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("observance", "v5_control_id"),
                    org.bukkit.persistence.PersistentDataType.STRING) : null;
            Rotation expected = Rotation.values()[initialRotations[Math.max(0, Math.min(5, index - 1))]];
            if (frame.getFacing() != BlockFace.NORTH || frame.getRotation() != expected
                    || compass.getType() != Material.COMPASS || !("ko02_" + index).equals(control)) {
                return "V5 " + id + " frame requires NORTH + unique COMPASS/PDC + deterministic wrong rotation";
            }
        }
        return null;
    }

    /**
     * Re-read the completed world after every fixture, wayfinding sign, gate, and book reconciliation
     * has run. This catches later templates overwriting an earlier fixture that already passed its
     * immediate audit.
     */
    private List<String> auditV5PostPlacement(World world, Location mouth) {
        if (world == null || mouth == null) return List.of("second-pass audit has no world/Mouth");
        List<String> issues = new ArrayList<>();
        Set<String> seenSigns = new HashSet<>();
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            Location loc = mouth.clone().add(fixture.x(), fixture.y(), fixture.z());
            Site live = plugin.sites() == null ? null : plugin.sites().get(fixture.id());
            if (live == null || live.location() == null || live.location().getWorld() != world
                    || live.location().getBlockX() != loc.getBlockX()
                    || live.location().getBlockY() != loc.getBlockY()
                    || live.location().getBlockZ() != loc.getBlockZ()) {
                issues.add(fixture.id() + " registration moved or disappeared after placement");
                continue;
            }
            // Exact V5 physical reconciliation intentionally replaces several V4-era answer
            // signs, focal blocks, and the single co-op plate. Their authoritative replacement
            // was just audited address-by-address; retain registration/frame/sign checks here.
            String frame = auditV4FixtureFrame(world, mouth, fixture);
            if (frame != null) issues.add(frame);
            auditV5SignsNear(loc, Math.max(3, Math.min(12, fixture.radius() + 2)), seenSigns, issues);
            if (issues.size() >= 16) return List.copyOf(issues);
        }

        Set<String> claimed = new HashSet<>();
        int physicalMounts = 0;
        int expectedPhysicalMounts = 0;
        for (V5AuthorityManifest.BookPlacement placement : V5AuthorityManifest.bookPlacements()) {
            if ("earned_artifact".equals(placement.holderKind())) continue;
            if (placement.holderId().startsWith("unlit_house_")) continue;
            expectedPhysicalMounts++;
            Block mount = resolveV5BookLectern(placement, mouth, claimed);
            if (mount == null || mount.getType() != Material.LECTERN
                    || !v5BookFacingMatches(mount, placement.expectedFront())) {
                issues.add("book mount changed after binding: " + placement.bookId());
            } else {
                physicalMounts++;
            }
        }
        if (physicalMounts != expectedPhysicalMounts) issues.add("expected "
                + expectedPhysicalMounts + " in-Hold V5 book mounts, found " + physicalMounts);

        for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
            Block mount = world.getBlockAt(mouth.getBlockX() + station.x(),
                    mouth.getBlockY() + station.y(), mouth.getBlockZ() + station.z());
            // Some V5 record-station anchors are exact mechanic controls rather than lecterns;
            // their readable mounts are verified through the placement manifest above.
            auditV5SignsNear(mount.getLocation(), 8, seenSigns, issues);
        }
        Site release = plugin.sites() == null ? null : plugin.sites().get("release_record");
        if (release == null || release.location() == null || !hasV5SeverControlNear(release.location())) {
            issues.add("release_record sever control failed final readback");
        }
        for (HoldGate gate : DEEP_HOLD_GATES) {
            Site site = plugin.sites() == null ? null : plugin.sites().get(holdGateSiteId(gate.id()));
            String gateIssue = auditHoldGateIntegrity(gate, site == null ? null : site.location());
            if (gateIssue != null) issues.add(gateIssue);
        }
        String route = auditV4OpenRoute(world, mouth);
        if (route != null) issues.add(route);
        return List.copyOf(issues);
    }

    private void auditV5SignsNear(Location center, int radius, Set<String> seen, List<String> issues) {
        if (center == null || center.getWorld() == null || issues.size() >= 16) return;
        World world = center.getWorld();
        int r = Math.max(2, Math.min(12, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 5; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = world.getBlockAt(center.getBlockX() + dx,
                            center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (!(block.getState() instanceof Sign)) continue;
                    String key = blockKey(block);
                    if (!seen.add(key)) continue;
                    String material = block.getType().name();
                    if (block.getBlockData() instanceof Directional directional
                            && material.contains("WALL_SIGN")) {
                        Block support = block.getRelative(directional.getFacing().getOppositeFace());
                        Block front = block.getRelative(directional.getFacing());
                        if (support.isPassable()) issues.add("wall sign has no backing at " + key);
                        if (!front.isPassable()) issues.add("wall sign faces into a blocked cell at " + key);
                    }
                    if (issues.size() >= 16) return;
                }
            }
        }
    }

    private String auditV5BookItem(ItemStack item, String expectedId) {
        V5AuthorityManifest.BookEntry expected = V5AuthorityManifest.book(expectedId);
        if (expected == null) return "unknown authority book " + expectedId;
        if (item == null || item.getType() != Material.WRITTEN_BOOK
                || !(item.getItemMeta() instanceof BookMeta meta)) return "missing written book " + expectedId;
        String bookId = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "book_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        String story = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "story_version"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (!expected.id().equals(bookId) || !"5.0.0".equals(story)) return "book PDC/version mismatch";
        if (!expected.title().equals(meta.getTitle()) || !expected.author().equals(meta.getAuthor())
                || meta.getPageCount() != expected.pages().size()) return "title/author/page-count mismatch";
        for (int page = 1; page <= meta.getPageCount(); page++) {
            String actual = PlainTextComponentSerializer.plainText().serialize(meta.page(page));
            if (!expected.pages().get(page - 1).equals(actual)) return "page " + page + " text mismatch";
        }
        return null;
    }

    private void registerHoldRegionV4(String worldName, int bx, int by, int bz) {
        plugin.registerRuntimeSite(new Site(HOLD_REGION_SITE_ID, "hold_region", worldName,
                (double) bx, (double) (by - 50), (double) (bz + 116),
                122, 66, true, true, null, false));
        plugin.registerRuntimeSite(new Site(HOLD_ENTRY_REGION_SITE_ID, "hold_region", worldName,
                (double) bx, (double) (by - 20), (double) (bz + 52),
                72, 34, true, true, null, false));
    }

    private void placeHoldPrologueEchoV4(World world, int bx, int by, int bz) {
        if (world == null) return;
        // The V5 covered copy is authored and unlock-controlled at forgotten_mouth.  This nearby
        // dressing must stay text-free so a stale V4 book can never survive a rebuild or repair.
        int shelfX = DeepHoldV4Plan.compactX(-16);
        int shelfZ = DeepHoldV4Plan.compactZ(-40, 112);
        placeDecorativeBookshelf(world.getBlockAt(bx + shelfX, by - 40, bz + shelfZ), 83, BlockFace.EAST);
        placeStandingSign(new Location(world, bx + shelfX + 4, by - 40, bz + shelfZ), BlockFace.EAST,
                new String[]{"ORIENTATION", "record mounts", "unlock in order", ""});
    }

    /** Remove only retired fixture/display entity classes inside the proven legacy excavation. */
    private int removeSupersededHoldEntities(World world, Location mouth) {
        if (world == null || mouth == null) return 0;
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        int removed = 0;
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            if (!(entity instanceof ItemFrame) && !(entity instanceof org.bukkit.entity.Display)
                    && !(entity instanceof org.bukkit.entity.Interaction)
                    && !(entity instanceof org.bukkit.entity.Marker)) continue;
            Location at = entity.getLocation();
            int x = at.getBlockX() - bx, y = at.getBlockY() - by, z = at.getBlockZ() - bz;
            boolean upper = x >= -103 && x <= 103 && y >= -43 && y <= -14 && z >= 102 && z <= 257;
            boolean civic = x >= -107 && x <= 107 && y >= -71 && y <= -45 && z >= 34 && z <= 303;
            boolean lower = x >= -117 && x <= 117 && y >= -99 && y <= -71 && z >= 34 && z <= 381;
            boolean transition = x >= -18 && x <= 18 && y >= -44 && y <= -14 && z >= 252 && z <= 290;
            boolean lowerTransition = x >= -19 && x <= 19 && y >= -99 && y <= -55
                    && z >= 6 && z <= 42;
            if (upper || civic || lower || transition || lowerTransition) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    private void placeHoldDistrictRecordsV4(World world, int bx, int by, int bz) {
        if (world == null) return;
        // These are physical stations only. V5AuthorityManifest owns every page and syncV5Books()
        // inserts or removes the exact book for the current durable unlock flags.
        int seed = 101;
        for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
            BlockFace facing = BlockFace.valueOf(station.front());
            int x = bx + station.x(), y = by + station.y(), z = bz + station.z();
            placeHoldEmptyRecordStation(world, x, y, z, facing, seed);
            int signX = x + (facing == BlockFace.EAST ? 4 : facing == BlockFace.WEST ? -4 : 3);
            int signZ = z + (facing == BlockFace.SOUTH ? 4 : facing == BlockFace.NORTH ? -2 : 0);
            placeStandingSign(new Location(world, signX, y, signZ),
                    facing == BlockFace.NORTH ? BlockFace.WEST : facing,
                    new String[]{station.title().toUpperCase(Locale.ROOT), "controlled", "record mount", ""});
            seed += 18;
        }

        DeepHoldV4Plan.RecordStation orientation = holdRecordStation("orientation_register");
        if (orientation != null) {
            // LC03's school-day docket is the exact predicate offset [-2,0,0].
            Block schoolDay = world.getBlockAt(bx + orientation.x() - 2,
                    by + orientation.y(), bz + orientation.z());
            placeReadableLectern(schoolDay, BlockFace.valueOf(orientation.front()));
            if (schoolDay.getState() instanceof Lectern lectern) {
                lectern.getInventory().clear();
                lectern.update(true, false);
            }
        }

        DeepHoldV4Plan.RecordStation release = holdRecordStation("release_record");
        if (release != null) placeV5SeverControl(new Location(world, bx + release.x(),
                by + release.y(), bz + release.z() - 4));
    }

    private static DeepHoldV4Plan.RecordStation holdRecordStation(String id) {
        return DeepHoldV4Plan.RECORD_STATIONS.stream()
                .filter(station -> id.equals(station.id())).findFirst().orElse(null);
    }

    private void placeHoldWayfindingV4(World world, int bx, int by, int bz) {
        if (world == null) return;
        Object[][] signs = {
                {-9, 0, 3, BlockFace.EAST, new String[]{"THE DEEP HOLD", "one Mouth", "return here", "to leave"}},
                {-8, -40, 108, BlockFace.EAST, new String[]{"ORIENTATION", "records unlock", "in sequence", ""}},
                {-8, -40, 134, BlockFace.EAST, new String[]{"KEEPER ARCHIVE", "six inquiries", "one work floor", ""}},
                {-8, -40, 176, BlockFace.EAST, new String[]{"STAFF STAIR", "civic archive", "below", ""}},
                {-8, -68, 174, BlockFace.EAST, new String[]{"CIVIC ARCHIVE", "library floor", "index at center", ""}},
                {-8, -68, 70, BlockFace.EAST, new String[]{"SERVICE WORKS", "lamp records", "lower stair", "north"}},
                {-8, -96, 42, BlockFace.EAST, new String[]{"LOWER WORKS", "reckon first", "return north", ""}},
                {-18, -96, 94, BlockFace.EAST, new String[]{"WEST OFFICE", "absence case", "prior camp", ""}},
                {18, -96, 94, BlockFace.WEST, new String[]{"EAST OFFICE", "threshold", "relay required", ""}},
                {-8, -96, 139, BlockFace.EAST, new String[]{"G5", "accepting archive", "case required", ""}},
                {-8, -96, 179, BlockFace.EAST, new String[]{"G6", "unwriting", "case required", ""}},
                {-8, -96, 221, BlockFace.EAST, new String[]{"RELEASE OFFICE", "closure record", "return north", ""}}
        };
        for (Object[] row : signs) {
            placeStandingSign(new Location(world, bx + (Integer) row[0], by + (Integer) row[1],
                    bz + (Integer) row[2]), (BlockFace) row[3], (String[]) row[4]);
        }
    }

    private String auditV4OpenRoute(World world, Location mouth) {
        if (world == null || mouth == null) return "V5 route audit has no world or Mouth.";
        int bx = mouth.getBlockX();
        int by = mouth.getBlockY();
        int bz = mouth.getBlockZ();
        Set<HoldWalkNode> visited = new HashSet<>();
        ArrayDeque<HoldWalkNode> queue = new ArrayDeque<>();
        // Seed the authored interior floor, never the walkable earth/roof above the Mouth.  A
        // top-down search made the old audit flood the surface and falsely report every buried room
        // unreachable.  The short fallback is only for a locally uneven Mouth floor.
        HoldWalkNode seed = isHoldStandable(world, bx, by, bz + 2)
                ? new HoldWalkNode(bx, by, bz + 2) : null;
        for (int z = 0; z <= 8 && seed == null; z++) {
            for (int dy : new int[]{0, -1, 1, -2}) {
                int y = by + dy;
                if (isHoldStandable(world, bx, y, bz + z)) {
                    seed = new HoldWalkNode(bx, y, bz + z);
                    break;
                }
            }
        }
        if (seed == null) return "V5 Surface Mouth has no Adventure-mode walk seed.";
        queue.add(seed);
        int minX = bx + DeepHoldV4Plan.MIN_X - 4;
        int maxX = bx + DeepHoldV4Plan.MAX_X + 4;
        int minY = by + DeepHoldV4Plan.MIN_Y - 4;
        int maxY = by + DeepHoldV4Plan.MAX_Y + 4;
        int minZ = bz + DeepHoldV4Plan.MIN_Z;
        int maxZ = bz + DeepHoldV4Plan.MAX_Z + 4;
        final int hardLimit = 900_000;
        while (!queue.isEmpty()) {
            HoldWalkNode node = queue.removeFirst();
            if (!visited.add(node)) continue;
            if (visited.size() > hardLimit) {
                return "V5 walk graph escaped the authored envelope (over " + hardLimit + " nodes).";
            }
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = node.x() + step[0];
                int nz = node.z() + step[1];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                for (int dy : new int[]{0, 1, -1}) {
                    int ny = node.y() + dy;
                    if (ny < minY || ny > maxY) continue;
                    HoldWalkNode candidate = new HoldWalkNode(nx, ny, nz);
                    if (!visited.contains(candidate) && isV4AuditStandable(world, mouth, nx, ny, nz)) {
                        queue.addLast(candidate);
                        break;
                    }
                }
            }
        }

        Set<String> reachedRooms = new HashSet<>();
        for (HoldWalkNode node : visited) {
            int lx = node.x() - bx;
            int ly = node.y() - by;
            int lz = node.z() - bz;
            for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) {
                if (ly == room.floorY() && room.contains(lx, ly, lz, 3)) reachedRooms.add(room.id());
            }
        }
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) {
            if (!reachedRooms.contains(room.id())) return "V5 room " + room.id()
                    + " is not reachable from the one Surface Mouth; "
                    + nearestV4WalkDiagnostic(visited, mouth, room) + ".";
        }
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            HoldWalkNode stand = new HoldWalkNode(bx + fixture.standX(), by + fixture.standY(), bz + fixture.standZ());
            if (!visited.contains(stand)) return "V5 fixture " + fixture.id()
                    + " has a valid standing frame but no Mouth-reachable route; "
                    + nearestV4FixtureDiagnostic(visited, mouth, fixture) + ".";
        }
        for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
            if (!hasHoldReachableNode(visited, bx + station.x(), by + station.y(), bz + station.z(), 6, 6, 2)) {
                return "V5 record station " + station.id() + " has no Mouth-reachable reading position.";
            }
        }

        String signIssue = auditV4Signs(world, mouth, visited);
        if (signIssue != null) return signIssue;
        return null;
    }

    private boolean isV4AuditStandable(World world, Location mouth, int x, int y, int z) {
        if (world == null || mouth == null) return false;
        Block floor = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        return floor.getType().isSolid()
                && (isHoldBodyClear(feet) || isV4GateDoorCell(mouth, x, y, z))
                && (isHoldBodyClear(head) || isV4GateDoorCell(mouth, x, y + 1, z));
    }

    private boolean isV4GateDoorCell(Location mouth, int x, int y, int z) {
        int lx = x - mouth.getBlockX(), ly = y - mouth.getBlockY(), lz = z - mouth.getBlockZ();
        for (HoldGate gate : DEEP_HOLD_GATES) {
            HoldGateSpan span = holdGateSpan(gate);
            int across = span.acrossX() ? lx - gate.x() : lz - gate.z();
            int depth = span.acrossX() ? lz - gate.z() : lx - gate.x();
            int dy = ly - gate.y();
            int doorHeight = Math.min(span.height() - 3, Math.max(6, span.doorHalf() + 1));
            if (Math.abs(across) <= span.doorHalf() && depth >= 0 && depth <= span.depth()
                    && dy >= 0 && dy <= doorHeight) return true;
        }
        return false;
    }

    private String nearestV4FixtureDiagnostic(Set<HoldWalkNode> visited, Location mouth,
                                              DeepHoldV4Plan.Fixture fixture) {
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        int tx = bx + fixture.standX(), ty = by + fixture.standY(), tz = bz + fixture.standZ();
        HoldWalkNode nearest = null;
        int best = Integer.MAX_VALUE;
        for (HoldWalkNode node : visited) {
            if (node.y() != ty) continue;
            int distance = Math.abs(node.x() - tx) + Math.abs(node.z() - tz);
            if (distance < best) {
                best = distance;
                nearest = node;
            }
        }
        if (nearest == null) return "no reachable cell exists on the fixture floor";
        String routeReport = "threshold_vault".equals(fixture.id())
                ? "; vault chain " + diagnoseV4VaultChain(visited, mouth) : "";
        return "nearest fixture-floor cell is local " + (nearest.x() - bx) + ","
                + (nearest.y() - by) + "," + (nearest.z() - bz)
                + " at horizontal distance " + best + routeReport;
    }

    private String diagnoseV4VaultChain(Set<HoldWalkNode> visited, Location mouth) {
        int[][] checkpoints = {
                {39, 136}, {39, 144}, {52, 144}, {52, 150}, {52, 162},
                {46, 162}, {40, 162}, {40, 170}, {40, 184}, {40, 188}
        };
        World world = mouth.getWorld();
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        for (int[] point : checkpoints) {
            int x = bx + point[0], y = by - 96, z = bz + point[1];
            HoldWalkNode node = new HoldWalkNode(x, y, z);
            if (visited.contains(node)) continue;
            Block floor = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            return "first unreached checkpoint local " + point[0] + ",-96," + point[1]
                    + " floor/feet/head=" + floor.getType() + "/" + feet.getType() + "/" + head.getType();
        }
        return "all authored checkpoints reached";
    }

    private String nearestV4WalkDiagnostic(Set<HoldWalkNode> visited, Location mouth,
                                           DeepHoldV4Plan.Room room) {
        if (visited == null || visited.isEmpty() || mouth == null || room == null) return "walk graph is empty";
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        int minX = bx + room.minX() + 3, maxX = bx + room.maxX() - 3;
        int targetY = by + room.floorY();
        int minZ = bz + room.minZ() + 3, maxZ = bz + room.maxZ() - 3;
        HoldWalkNode nearest = null;
        HoldWalkNode nearestOnFloor = null;
        int best = Integer.MAX_VALUE;
        int bestOnFloor = Integer.MAX_VALUE;
        for (HoldWalkNode node : visited) {
            int dx = node.x() < minX ? minX - node.x() : (node.x() > maxX ? node.x() - maxX : 0);
            int dy = Math.abs(node.y() - targetY);
            int dz = node.z() < minZ ? minZ - node.z() : (node.z() > maxZ ? node.z() - maxZ : 0);
            int distance = dx + dy + dz;
            if (distance < best) {
                best = distance;
                nearest = node;
            }
            if (node.y() == targetY) {
                int horizontal = dx + dz;
                if (horizontal < bestOnFloor) {
                    bestOnFloor = horizontal;
                    nearestOnFloor = node;
                }
            }
        }
        if (nearest == null) return "walk graph is empty";
        int probeX = bx + (room.minX() < 0 ? room.maxX() - 3 : room.minX() + 3);
        int probeZ = bz + ((room.minZ() + room.maxZ()) / 2);
        Block probeFloor = mouth.getWorld().getBlockAt(probeX, targetY - 1, probeZ);
        Block probeFeet = mouth.getWorld().getBlockAt(probeX, targetY, probeZ);
        Block probeHead = mouth.getWorld().getBlockAt(probeX, targetY + 1, probeZ);
        String floorReport = nearestOnFloor == null ? "no reachable cell on room floor"
                : "nearest room-floor cell is local " + (nearestOnFloor.x() - bx) + ","
                + (nearestOnFloor.y() - by) + "," + (nearestOnFloor.z() - bz)
                + " at horizontal distance " + bestOnFloor;
        String stairReport = "lower_works".equals(room.id())
                ? "; lower switchback " + diagnoseV4LowerSwitchback(visited, mouth) : "";
        return "nearest walk cell is local " + (nearest.x() - bx) + "," + (nearest.y() - by)
                + "," + (nearest.z() - bz) + " at distance " + best + "; " + floorReport
                + "; doorway probe floor/feet/head=" + probeFloor.getType() + "/"
                + probeFeet.getType() + "/" + probeHead.getType() + stairReport;
    }

    private String diagnoseV4LowerSwitchback(Set<HoldWalkNode> visited, Location mouth) {
        World world = mouth.getWorld();
        int bx = mouth.getBlockX(), by = mouth.getBlockY(), bz = mouth.getBlockZ();
        for (int z = 34; z >= 6; z--) {
            int y = -68 - Math.min(14, (34 - z) / 2);
            String issue = diagnoseV4Tread(world, visited, bx - 10, by + y, bz + z, bx, by, bz);
            if (issue != null) return "west flight " + issue;
        }
        for (int z = 6; z <= 34; z++) {
            int y = -82 - Math.min(14, (z - 6) / 2);
            String issue = diagnoseV4Tread(world, visited, bx + 10, by + y, bz + z, bx, by, bz);
            if (issue != null) return "east flight " + issue;
        }
        return "all center treads are standable and reached";
    }

    private String diagnoseV4Tread(World world, Set<HoldWalkNode> visited, int x, int y, int z,
                                   int bx, int by, int bz) {
        HoldWalkNode node = new HoldWalkNode(x, y, z);
        if (visited.contains(node)) return null;
        Block floor = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        return "first unreached tread local " + (x - bx) + "," + (y - by) + "," + (z - bz)
                + " floor/feet/head=" + floor.getType() + "/" + feet.getType() + "/" + head.getType();
    }

    private String auditV4Signs(World world, Location mouth, Set<HoldWalkNode> visited) {
        int bx = mouth.getBlockX();
        int by = mouth.getBlockY();
        int bz = mouth.getBlockZ();
        Set<HoldWalkNode> seen = new HashSet<>();
        int authored = 0;
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) {
            for (int x = room.minX(); x <= room.maxX(); x++) {
                for (int z = room.minZ(); z <= room.maxZ(); z++) {
                    for (int y = room.floorY(); y < room.ceilingY(); y++) {
                        Block block = world.getBlockAt(bx + x, by + y, bz + z);
                        if (!(block.getState() instanceof Sign)) continue;
                        HoldWalkNode key = new HoldWalkNode(block.getX(), block.getY(), block.getZ());
                        if (!seen.add(key)) continue;
                        authored++;
                        String issue = auditHoldReadableSign(block, visited);
                        if (issue != null) return issue;
                    }
                }
            }
        }
        for (int x = -18; x <= 18; x++) {
            for (int z = -6; z <= 108; z++) {
                for (int y = -44; y <= 12; y++) {
                    Block block = world.getBlockAt(bx + x, by + y, bz + z);
                    if (!(block.getState() instanceof Sign)) continue;
                    HoldWalkNode key = new HoldWalkNode(block.getX(), block.getY(), block.getZ());
                    if (!seen.add(key)) continue;
                    authored++;
                    String issue = auditHoldReadableSign(block, visited);
                    if (issue != null) return issue;
                }
            }
        }
        if (authored < DeepHoldV4Plan.RECORD_STATIONS.size() + 12) {
            return "V5 sign audit found only " + authored + " authored signs.";
        }
        return null;
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
        if (by - 36 < world.getMinHeight() + 4) {
            throw new IllegalStateException("Deep Hold mouth is too low for the authored 36-block envelope");
        }
        // Load every external manuscript before changing a single block. A missing packaged resource
        // must fail cleanly, not after leaving a city shell and nineteen registered fixtures behind.
        validateDeepHoldPlan();
        if (sender != null) sender.sendMessage("  carving V2 owned rooms, roofed corridors, and the 24-block descent...");
        buildHoldV2Shells(world, bx, by, bz);
        if (surfaceMouth != null) {
            buildHoldSurfaceMouth(surfaceMouth, base);
            String surfaceIssue = auditBuiltHoldSurfaceMouth(surfaceMouth, base);
            if (surfaceIssue != null) throw new IllegalStateException(surfaceIssue);
        }
        placeHoldPrologueEcho(world, bx, by, bz);
        placeHoldDistrictRecords(world, bx, by, bz);

        int placed = 0;
        int step = 0;
        List<Site> pendingSites = new ArrayList<>();
        for (HoldSite row : DEEP_HOLD_SITES) {
            step++;
            Location loc = new Location(world, bx + row.x(), by + row.y(), bz + row.z());
            try {
                loc.getChunk().load(true);
                Site live = configuredHoldSite(row, loc, worldName);
                placeHoldFixture(live, loc, row);
                ensureHoldAnchorVisual(live, loc);
                String fixtureIssue = auditPlacedSite(live, loc);
                if (fixtureIssue != null) {
                    throw new IllegalStateException(fixtureIssue);
                }
                if (!hasHoldNearbyPlayerSpace(loc, 5, 10)) {
                    throw new IllegalStateException(row.id() + " has no Adventure-mode standing clearance");
                }
                pendingSites.add(live);
                placed++;
                if (sender != null && (step % 12 == 0 || step == DEEP_HOLD_SITES.length)) {
                    sender.sendMessage("  placed " + placed + "/" + DEEP_HOLD_SITES.length + " hold sites...");
                }
            } catch (Throwable t) {
                if (sender != null) {
                    sender.sendMessage("  [!] Deep Hold stopped at " + row.id() + " ("
                            + t.getClass().getSimpleName() + "); the build is NOT production-ready.");
                }
                throw new IllegalStateException("Deep Hold fixture failed: " + row.id(), t);
            }
        }

        // Prove the completed, fixture-populated structure while every authored doorway is still
        // open.  Gates are stamped only after this full Adventure-mode walk succeeds, so a sealed
        // progression gate can never hide a broken staircase or an inaccessible room.
        placeHoldWayfinding(world, bx, by, bz);
        String shellIssue = auditHoldCivicShell(world, bx, by, bz);
        if (shellIssue != null) throw new IllegalStateException(shellIssue);
        String enclosureIssue = auditHoldDistrictEnclosure(world, bx, by, bz);
        if (enclosureIssue != null) throw new IllegalStateException(enclosureIssue);
        Set<HoldWalkNode> openWalk = collectHoldAdventureWalk(world, bx, by, bz);
        String traversalIssue = auditHoldAdventureReachability(world, bx, by, bz, openWalk);
        if (traversalIssue != null) throw new IllegalStateException(traversalIssue);
        String signIssue = auditHoldSigns(world, bx, by, bz, openWalk);
        if (signIssue != null) throw new IllegalStateException(signIssue);

        for (HoldGate gate : DEEP_HOLD_GATES) {
            Location loc = new Location(world, bx + gate.x(), by + gate.y(), bz + gate.z());
            HoldGateSpan span = holdGateSpan(gate);
            pendingSites.add(new Site(holdGateSiteId(gate.id()), "hold_gate", worldName,
                    loc.getX(), loc.getY(), loc.getZ(), Math.max(8, span.halfAcross()), span.height() + 2,
                    true, true, null, false));
            setHoldGate(gate, loc, !gate.openInitially());
            placeHoldGateLabel(gate, loc);
            String gateIssue = auditHoldGateIntegrity(gate, loc);
            if (gateIssue != null) throw new IllegalStateException(gateIssue);
            String gateLabelIssue = auditHoldGateLabel(gate, loc);
            if (gateLabelIssue != null) throw new IllegalStateException(gateLabelIssue);
        }

        String prologueIssue = auditHoldPrologueEcho(world, bx, by, bz);
        if (prologueIssue != null) throw new IllegalStateException(prologueIssue);
        for (HoldRecordStation station : DEEP_HOLD_RECORD_STATIONS) {
            String recordIssue = auditHoldRecordStation(world, bx, by, bz, station);
            if (recordIssue != null) throw new IllegalStateException(recordIssue);
        }

        // Publish runtime coordinates only after the complete physical build succeeds. A thrown fixture
        // may leave diagnostic blocks in this abandoned test area, but it can never persist a misleading
        // partial Hold into sites.yml or make audit report a valid subset.
        registerHoldRegion(worldName, bx, by, bz, surfaceMouth);
        registerHoldFocusedAnswerSlots(worldName, bx, by, bz);
        for (Site pending : pendingSites) plugin.registerRuntimeSite(pending);
        return placed;
        } finally {
            plugin.endRuntimeSiteBatch();
        }
    }

    private void registerHoldFocusedAnswerSlots(String worldName, int bx, int by, int bz) {
        if (worldName == null || worldName.isBlank()) return;
        Object[][] slots = {
                {"hold_answer_prior_absence", bx - 88, by - 28, bz + 532, "prior-absence"},
                {"hold_answer_prior_camp", bx - 88, by - 28, bz + 575, "prior-camp-refusal"},
                {"hold_answer_prior_vaun", bx - 100, by - 28, bz + 584, "prior-vaun-correction"},
                {"hold_answer_prior_mara", bx - 98, by - 28, bz + 587, "prior-mara-correction"},
                {"hold_answer_prior_sella", bx - 92, by - 28, bz + 588, "prior-sella-correction"},
                {"hold_answer_prior_orin", bx - 84, by - 28, bz + 588, "prior-orin-correction"},
                {"hold_answer_prior_brann", bx - 78, by - 28, bz + 587, "prior-brann-correction"},
                {"hold_answer_prior_iss", bx - 76, by - 28, bz + 584, "prior-iss-correction"},
                {"hold_answer_witness", bx + 3, by - 28, bz + 589, "prior-witness-before-accepting"}
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

    private void validateDeepHoldPlan() {
        Map<String, HoldRoomBox> roomsById = new LinkedHashMap<>();
        for (int i = 0; i < DEEP_HOLD_ROOM_BOXES.length; i++) {
            HoldRoomBox a = DEEP_HOLD_ROOM_BOXES[i];
            if (a.minX() >= a.maxX() || a.minZ() >= a.maxZ() || a.floorY() >= a.ceilingY()) {
                throw new IllegalStateException("Invalid Hold room box " + a.id());
            }
            if (roomsById.put(a.id(), a) != null) {
                throw new IllegalStateException("Duplicate Hold room id " + a.id());
            }
            for (int j = i + 1; j < DEEP_HOLD_ROOM_BOXES.length; j++) {
                HoldRoomBox b = DEEP_HOLD_ROOM_BOXES[j];
                boolean overlapX = a.minX() <= b.maxX() && b.minX() <= a.maxX();
                boolean overlapZ = a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
                boolean overlapY = a.floorY() - 2 <= b.ceilingY() + 1
                        && b.floorY() - 2 <= a.ceilingY() + 1;
                if (overlapX && overlapZ && overlapY) {
                    throw new IllegalStateException("Overlapping Hold room ownership: " + a.id() + " / " + b.id());
                }
            }
        }
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (String id : roomsById.keySet()) graph.put(id, new HashSet<>());
        for (HoldRoomLink link : DEEP_HOLD_ROOM_LINKS) {
            if (!roomsById.containsKey(link.from()) || !roomsById.containsKey(link.to())) {
                throw new IllegalStateException("Hold doorway link references an unknown room: "
                        + link.from() + " / " + link.to());
            }
            graph.get(link.from()).add(link.to());
            graph.get(link.to()).add(link.from());
        }
        Set<String> reachable = new HashSet<>();
        ArrayDeque<String> roomQueue = new ArrayDeque<>();
        roomQueue.add("mouth_vestibule");
        while (!roomQueue.isEmpty()) {
            String room = roomQueue.removeFirst();
            if (!reachable.add(room)) continue;
            roomQueue.addAll(graph.getOrDefault(room, Set.of()));
        }
        if (reachable.size() != roomsById.size()) {
            Set<String> missing = new HashSet<>(roomsById.keySet());
            missing.removeAll(reachable);
            throw new IllegalStateException("Hold doorway graph leaves rooms unreachable: " + missing);
        }
        for (HoldSite site : DEEP_HOLD_SITES) {
            int owners = 0;
            HoldRoomBox owner = null;
            for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
                if (holdSiteInsideRoom(site, room)) {
                    owners++;
                    owner = room;
                }
            }
            if (owners != 1) {
                throw new IllegalStateException("Hold fixture " + site.id() + " has " + owners
                        + " interior owners" + (owner == null ? "" : " (" + owner.id() + ")"));
            }
        }
        for (HoldRecordStation station : DEEP_HOLD_RECORD_STATIONS) {
            int owners = 0;
            for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
                if (station.x() >= room.minX() + 3 && station.x() <= room.maxX() - 3
                        && station.z() >= room.minZ() + 3 && station.z() <= room.maxZ() - 3
                        && station.y() >= room.floorY() && station.y() <= room.ceilingY() - 2) {
                    owners++;
                }
            }
            if (owners != 1) {
                throw new IllegalStateException("Hold record station " + station.id()
                        + " has " + owners + " interior owners");
            }
        }
    }

    private boolean holdSiteInsideRoom(HoldSite site, HoldRoomBox room) {
        if (site == null || room == null) return false;
        int floorInset = switch (site.id()) {
            case "sella_pool" -> 1;
            case "third_lamp_stand" -> 2;
            case "painted_line" -> 4;
            default -> 0;
        };
        int halfX = Math.max(0, site.halfX());
        int halfZ = Math.max(0, site.halfZ());
        return site.x() - halfX >= room.minX() + 3 && site.x() + halfX <= room.maxX() - 3
                && site.z() - halfZ >= room.minZ() + 3 && site.z() + halfZ <= room.maxZ() - 3
                && site.y() >= room.floorY() - floorInset && site.y() <= room.ceilingY() - 2;
    }

    private Site configuredHoldSite(HoldSite row, Location loc, String worldName) {
        Site cfg = plugin.sites() == null ? null : plugin.sites().get(row.id());
        // Managed Hold geometry and protection are executable V5 manifest data. A stale sites.yml
        // may carry an answer binding forward, but it must never resize, retype, or unprotect a room.
        String type = row.type();
        int radius = row.radius();
        int vertical = row.vertical();
        boolean protect = true;
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
            placeReadableLectern(b, holdFixtureFront(id));
            int index = trailingIndex(id, 1);
            int[] markedPages = {1, 2, 4, 4, 6};
            int marked = markedPages[Math.max(0, Math.min(markedPages.length - 1, index - 1))];
            fillMaraLockBook(b, index, marked);
        } else if ("sella_lectern".equals(type)) {
            Block b = loc.getBlock();
            placeReadableLectern(b, holdFixtureFront(id));
            int index = trailingIndex(id, 1);
            int[] ringPages = {2, 3, 5, 7, 11};
            int marked = ringPages[Math.max(0, Math.min(ringPages.length - 1, index - 1))];
            fillSellaLockBook(b, index, marked);
        } else if (buildHoldIntegratedFixture(site, loc, row)) {
            // Production Hold fixtures are dressed into the district shell instead of pasting lab rooms.
        } else if (isTemplateLabSite(id)) {
            // A production Hold fixture must never paste a legacy self-contained room. Those templates
            // own floors, walls, and roofs and can erase the civic shell or a neighboring fixture.
            throw new IllegalStateException("No Hold-native fixture registered for " + id);
        } else {
            throw new IllegalStateException("Unhandled production Hold fixture " + id + " (" + type + ")");
        }

        stabilizeHoldAuditAnchor(id, loc.getBlock());
        if ("vaun_bookshelf".equals(type)) {
            placeMechanicBookshelf(loc.getBlock(), holdFixtureFront(id));
        }
        if (DEEP_HOLD_LORE_SEEDS.contains(id)) {
            seedFixtureLore(loc, holdLoreSeed(id));
        }
        enhanceHoldVisual(site, loc);
        removeRetiredBeaconNear(loc);
    }

    private void shapeHoldFixtureSetting(Site site, Location loc, HoldSite row) {
        // V2 room ownership is carved exactly once before fixtures are placed. A fixture may dress only
        // its local cells; it must never clear or shell an independent micro-room here.
    }

    private BlockFace holdFixtureFront(String siteId) {
        if (siteId == null) return BlockFace.NORTH;
        DeepHoldV4Plan.Fixture v4 = DeepHoldV4Plan.fixture(siteId);
        if (v4 != null) {
            try {
                return BlockFace.valueOf(v4.front().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BlockFace.NORTH;
            }
        }
        if (siteId.startsWith("mara_lectern_") || siteId.startsWith("sella_lectern_")) return BlockFace.SOUTH;
        if (siteId.startsWith("orin_frame_dial_") || siteId.equals("vaun_bookshelf")) return BlockFace.NORTH;
        return switch (siteId) {
            case "stone_vaun", "stone_iss", "stone_brann", "stone_of_reckoning", "keeper_altar" -> BlockFace.EAST;
            case "stone_mara", "stone_sella", "stone_orin", "the_threshold" -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    private boolean buildHoldIntegratedFixture(Site site, Location loc, HoldSite row) {
        if (site == null || loc == null || loc.getWorld() == null || row == null) return false;
        String id = site.id();
        String type = site.type();
        World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        if ("rune_rosetta".equals(id)) {
            buildHoldRosettaCore(world, bx, by, bz);
            return true;
        } else if (Set.of("stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss").contains(id)) {
            buildHoldKeeperStoneCore(world, bx, by, bz, id, holdFixtureFront(id));
            return true;
        } else if ("stone_of_reckoning".equals(id)) {
            buildHoldReckoningCore(world, bx, by, bz);
            return true;
        } else if ("the_cold_hearth".equals(id)) {
            buildHoldColdHearthCore(world, bx, by, bz);
            return true;
        } else if ("the_threshold".equals(id)) {
            buildHoldThresholdCore(world, bx, by, bz);
            return true;
        } else if ("threshold_vault".equals(id)) {
            buildHoldThresholdVaultCore(world, bx, by, bz);
            return true;
        } else if ("unbroken_light".equals(id)) {
            buildHoldAcceptingCore(world, bx, by, bz);
            return true;
        } else if ("the_unwriting".equals(id)) {
            buildHoldUnwritingCore(world, bx, by, bz);
            return true;
        } else if ("case_board".equals(id)) {
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
            case "bow_marker" -> {
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.CHISELED_DEEPSLATE, Material.BLACK_CANDLE);
                yield true;
            }
            case "mara_map_marker" -> {
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.CHISELED_DEEPSLATE, Material.BLACK_CANDLE);
                yield true;
            }
            case "orin_marker" -> {
                int rank = trailingIndex(id, 1);
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.CHISELED_DEEPSLATE, Material.BLACK_CANDLE);
                carveOrinDirection(world, bx, by, bz, rank);
                yield true;
            }
            case "mara_route_marker" -> {
                int rank = trailingIndex(id, 1);
                buildHoldSmallEvidenceCore(world, bx, by, bz, Material.POLISHED_DEEPSLATE, Material.GRAY_CANDLE);
                world.getBlockAt(bx, by - 1, bz + 1).setType(
                        rank == 4 ? Material.CHISELED_TUFF : Material.POLISHED_TUFF, false);
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
        placeDecorativeBookshelf(world.getBlockAt(bx - 7, by, bz + 5), 113, BlockFace.NORTH);
        placeDecorativeBookshelf(world.getBlockAt(bx + 7, by, bz + 5), 127, BlockFace.NORTH);
        world.getBlockAt(bx - 7, by + 1, bz - 5).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(bx + 7, by + 1, bz - 5).setType(Material.LANTERN, false);
        world.getBlockAt(bx, by, bz + 5).setType(Material.LIGHT_GRAY_CARPET, false);

        placeStandingSign(new Location(world, bx - 5, by, bz + 4), BlockFace.NORTH,
                new String[]{"after the six", "do not enter", "with one kind", "of proof"});
        placeEditableStandingSign(new Location(world, bx, by, bz + 4), BlockFace.NORTH,
                new String[]{"file", "missing", "condition", ""});
        placeStandingSign(new Location(world, bx + 5, by, bz + 4), BlockFace.NORTH,
                new String[]{"decode", "witness", "compare", "then carry"});

        placeEvidenceLectern(new Location(world, bx - 7, by, bz - 2), BlockFace.EAST,
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
        placeEvidenceLectern(new Location(world, bx + 7, by, bz - 2), BlockFace.WEST,
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
        // Central grey correction light keeps the prior-run state legible from the camp entrance;
        // the six packet candles remain distributed at their individual bedrolls.
        world.getBlockAt(bx - 2, by, bz - 5).setType(Material.GRAY_CANDLE, false);

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
        placeDecorativeBookshelf(world.getBlockAt(bx - 23, by, bz + 15), 233, BlockFace.NORTH);
        placeDecorativeBookshelf(world.getBlockAt(bx + 23, by, bz + 15), 241, BlockFace.NORTH);
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

        placeEvidenceLectern(new Location(world, bx - 7, by, bz - 4), BlockFace.EAST,
                "failed accepting floor", List.of(
                        "This is not a rehearsal room. It is the old result.",
                        "Six tokens reached the floor. The room refused them because every hand inside the circle wanted the same finish.",
                        "The correction is not another token.\n\nBring witness before accepting.",
                        "Six blank leaves remain in the barrel. A living hand must sign one for each keeper; copied titles are refused."
                ));
        placeEvidenceLectern(new Location(world, bx + 7, by, bz - 4), BlockFace.WEST,
                "before the last warm", List.of(
                        "The accepting floor is not asking whether the case is complete.",
                        "It asks whether completion can be checked by someone who is not trying to own it.",
                        "When every correction has a file, the filing sign is no longer asking for another token.\n\n" +
                                "The condition is the order of the rite: witness before accepting."
                ));
        Block blankLeaves = world.getBlockAt(bx, by, bz - 3);
        blankLeaves.setType(Material.BARREL, false);
        faceDirectional(blankLeaves.getLocation(), BlockFace.SOUTH);
        if (blankLeaves.getState() instanceof InventoryHolder holder) {
            holder.getInventory().clear();
            holder.getInventory().setItem(13, new ItemStack(Material.WRITABLE_BOOK, 6));
            blankLeaves.getState().update(true, false);
        }
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

    private void carveOrinDirection(World world, int bx, int by, int bz, int rank) {
        if (world == null) return;
        int[][] vectors = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {-1, 1}};
        int[] v = vectors[Math.max(0, Math.min(vectors.length - 1, rank - 1))];
        for (int step = 1; step <= 3; step++) {
            world.getBlockAt(bx + v[0] * step, by - 1, bz + v[1] * step)
                    .setType(step == 3 ? Material.CHISELED_TUFF : Material.POLISHED_TUFF, false);
        }
    }

    private void buildHoldVaunHoardCore(World world, int bx, int by, int bz) {
        // KV01 is an audit, not the retired return-the-first-stone rite. These local offsets match
        // the physical predicate authority and remain empty/neutral until its prerequisite opens.
        world.getBlockAt(bx, by - 1, bz).setType(Material.CHISELED_TUFF, false);
        world.getBlockAt(bx, by, bz).setType(Material.POLISHED_DEEPSLATE, false);
        Block ledger = world.getBlockAt(bx - 1, by, bz);
        placeReadableLectern(ledger, BlockFace.EAST);
        if (ledger.getState() instanceof Lectern lectern) {
            lectern.getInventory().clear();
            lectern.update(true, false);
        }
        Block receipts = world.getBlockAt(bx + 1, by, bz);
        receipts.setType(Material.BARREL, false);
        faceDirectional(receipts.getLocation(), BlockFace.EAST);
        if (receipts.getState() instanceof InventoryHolder holder) {
            holder.getInventory().clear();
            holder.getInventory().setItem(11, v5EvidencePaper(
                    "kv01_cloth_cistern_receipt", "Cistern cloth receipt", 7));
            holder.getInventory().setItem(15, v5EvidencePaper(
                    "kv01_charcoal_cistern_receipt", "Cistern charcoal receipt", 10));
        }
        Block tray = world.getBlockAt(bx, by, bz + 1);
        tray.setType(Material.BARREL, false);
        faceDirectional(tray.getLocation(), BlockFace.NORTH);
        if (tray.getState() instanceof InventoryHolder holder) holder.getInventory().clear();
        world.getBlockAt(bx, by + 1, bz + 1).setType(Material.POLISHED_BLACKSTONE_BUTTON, false);
        placeStandingSign(new Location(world, bx - 2, by, bz + 2), BlockFace.EAST,
                new String[]{"QUARTERMASTER", "compare stores", "file shortages", "pull audit"});
    }

    private ItemStack v5EvidencePaper(String evidenceId, String displayName, int value) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName).color(NamedTextColor.GRAY));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("observance", "v5_evidence_id"),
                org.bukkit.persistence.PersistentDataType.STRING, evidenceId);
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("observance", "v5_evidence_value"),
                org.bukkit.persistence.PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }

    private void buildHoldVaunShelfCore(World world, int bx, int by, int bz) {
        for (int dx = -2; dx <= 2; dx++) {
            if (dx == 0) {
                placeMechanicBookshelf(world.getBlockAt(bx, by, bz), BlockFace.NORTH);
            } else {
                placeDecorativeBookshelf(world.getBlockAt(bx + dx, by, bz), 71 + dx, BlockFace.NORTH);
            }
            if (Math.abs(dx) <= 1) placeDecorativeBookshelf(
                    world.getBlockAt(bx + dx, by + 1, bz), 89 + dx, BlockFace.NORTH);
        }
        Block supply = world.getBlockAt(bx, by, bz + 2);
        supply.setType(Material.BARREL, false);
        faceDirectional(supply.getLocation(), BlockFace.NORTH);
        if (supply.getState() instanceof InventoryHolder holder) {
            holder.getInventory().clear();
            holder.getInventory().setItem(10, new ItemStack(Material.BOOK, 6));
            supply.getState().update(true, false);
        }
        placeStandingSign(new Location(world, bx - 3, by, bz + 1), BlockFace.EAST,
                new String[]{"RECONCILIATION", "use return tags", "set six marks", "then submit"});
    }

    private void buildHoldWaterMirrorCore(World world, int bx, int by, int bz) {
        // Open floor pad. The authored standing frame at bz-3 must stay a dry walkable cell, so
        // no foot-level water may exist outside the rimmed KS01 waterline trough below.
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(((Math.abs(dx) + Math.abs(dz)) % 2 == 0 && Math.abs(dx) <= 3
                                && Math.abs(dz) <= 3)
                                ? Material.PRISMARINE_BRICKS : Material.DEEPSLATE_TILES, false);
                world.getBlockAt(bx + dx, by, bz + dz).setType(Material.AIR, false);
            }
        }
        // KS01 requires Material.WATER at exactly (bx-3..bx+2, by, bz): a rimmed six-cell trough
        // holds that water at foot level and the one-block rim keeps it from flooding the room.
        for (int dx = -4; dx <= 3; dx++) {
            world.getBlockAt(bx + dx, by, bz - 1).setType(Material.DARK_PRISMARINE, false);
            world.getBlockAt(bx + dx, by, bz + 1).setType(Material.DARK_PRISMARINE, false);
        }
        world.getBlockAt(bx - 4, by, bz).setType(Material.DARK_PRISMARINE, false);
        world.getBlockAt(bx + 3, by, bz).setType(Material.DARK_PRISMARINE, false);
        for (int dx = -3; dx <= 2; dx++) {
            world.getBlockAt(bx + dx, by - 1, bz).setType(
                    (dx == -2 || dx == 1) ? Material.SEA_LANTERN : Material.PRISMARINE_BRICKS, false);
            world.getBlockAt(bx + dx, by, bz).setType(Material.WATER, false);
        }
        // Five concentric floor rings establish only the inner-to-outer order. Their corresponding
        // page numbers are discovered from Sella's authored ring drawings, never printed on answer signs.
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    world.getBlockAt(bx + dx, by - 1, bz + dz).setType(
                            radius % 2 == 0 ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS, false);
                }
            }
        }
        placeEvidenceLectern(new Location(world, bx - 6, by, bz - 1), BlockFace.EAST,
                "water copy", List.of(
                        "Five dropped-stone rings were copied from the center outward.",
                        "Sella kept drawings because words were easy to recut. The pool preserves their order."
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
        world.getBlockAt(bx + 2, by, bz).setType(Material.POLISHED_DEEPSLATE_SLAB, false);
    }

    private void buildHoldFrameDialCore(World world, int bx, int by, int bz, int index) {
        placeV5FrameDial(new Location(world, bx, by, bz), index);
        world.getBlockAt(bx - 2, by, bz + 1).setType(index % 2 == 0 ? Material.GRAY_CARPET : Material.BLACK_CARPET, false);
        world.getBlockAt(bx + 2, by, bz + 1).setType(Material.DARK_OAK_BUTTON, false);
    }

    private void placeV5FrameDial(Location base, int index) {
        World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(
                        Math.abs(dx) == 2 || dz == 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_TILES,
                        false);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(x + dx, y, z).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(x + dx, y + 1, z).setType(Material.POLISHED_DEEPSLATE, false);
        }
        Block backing = world.getBlockAt(x, y + 1, z);
        if (!backing.getType().isSolid()) {
            throw new IllegalStateException("Orin dial backing is not solid at "
                    + x + "," + (y + 1) + "," + z);
        }
        BlockFace facing = BlockFace.NORTH;
        Location frameLoc = v5FramePlane(backing.getLocation(), facing);
        for (ItemFrame frame : world.getNearbyEntitiesByType(ItemFrame.class, frameLoc, 0.6)) {
            frame.remove();
        }
        int dial = Math.max(1, Math.min(6, index));
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        org.bukkit.inventory.meta.ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("survey bearing " + dial).color(NamedTextColor.GRAY));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("observance", "v5_control_id"),
                org.bukkit.persistence.PersistentDataType.STRING, "ko02_" + dial);
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("observance", "v5_restoration_id"),
                org.bukkit.persistence.PersistentDataType.STRING, "hs05_dial_" + dial);
        compass.setItemMeta(meta);
        Location frameSpawnAt = v5FrameSpawnAnchor(backing.getLocation(), facing);
        ItemFrame frame = world.spawn(frameSpawnAt, ItemFrame.class, spawned -> {
            if (!spawned.setFacingDirection(facing, true)) {
                throw new IllegalStateException("Paper refused the Orin dial's NORTH mount");
            }
            spawned.setFixed(false);
            spawned.setInvulnerable(true);
            spawned.setPersistent(true);
        });
        frame.setItem(compass, false);
        int[] initialRotations = {2, 6, 0, 4, 2, 6};
        frame.setRotation(Rotation.values()[initialRotations[dial - 1]]);
    }

    /** Exact front-face entity plane for an item frame mounted on a solid supporting block. */
    private static Location v5FramePlane(Location supportingBlock, BlockFace facing) {
        if (supportingBlock == null || supportingBlock.getWorld() == null) {
            throw new IllegalArgumentException("item-frame supporting block has no world");
        }
        BlockFace cardinalFacing = switch (facing) {
            case NORTH, EAST, SOUTH, WEST -> facing;
            default -> BlockFace.NORTH;
        };
        FixtureTransform.FramePlane plane = FixtureTransform.framePlane(
                new FixtureTransform.BlockPos(supportingBlock.getBlockX(), supportingBlock.getBlockY(),
                        supportingBlock.getBlockZ()),
                FixtureTransform.Cardinal.valueOf(cardinalFacing.name()));
        return new Location(supportingBlock.getWorld(), plane.x(), plane.y(), plane.z());
    }

    /**
     * Center of the adjacent air cell Paper must floor when constructing a hanging entity. This is
     * deliberately separate from {@link #v5FramePlane(Location, BlockFace)}: spawning on the
     * rendered face boundary floors back into the supporting block and Paper removes the frame.
     */
    private static Location v5FrameSpawnAnchor(Location supportingBlock, BlockFace facing) {
        if (supportingBlock == null || supportingBlock.getWorld() == null) {
            throw new IllegalArgumentException("item-frame supporting block has no world");
        }
        BlockFace cardinalFacing = switch (facing) {
            case NORTH, EAST, SOUTH, WEST -> facing;
            default -> BlockFace.NORTH;
        };
        FixtureTransform.FrameSpawnAnchor anchor = FixtureTransform.frameSpawnAnchor(
                new FixtureTransform.BlockPos(supportingBlock.getBlockX(), supportingBlock.getBlockY(),
                        supportingBlock.getBlockZ()),
                FixtureTransform.Cardinal.valueOf(cardinalFacing.name()));
        return new Location(supportingBlock.getWorld(), anchor.x(), anchor.y(), anchor.z());
    }

    private void buildHoldBrannCorridorCore(World world, int bx, int by, int bz, boolean end) {
        for (int dx = -4; dx <= 4; dx++) {
            world.getBlockAt(bx + dx, by, bz - 4).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(bx + dx, by, bz + 4).setType(Material.DEEPSLATE_BRICK_WALL, false);
        }
        world.getBlockAt(bx, by, bz).setType(end ? Material.BLACKSTONE : Material.POLISHED_DEEPSLATE, false);
        world.getBlockAt(bx, by + 1, bz).setType(end ? Material.BLACK_CANDLE : Material.SOUL_LANTERN, false);
        if (end) {
            // The end fixture completes the entire authored 38-block watch-walk back to the start.
            // Sculk sits outside the three-wide walking lane so adventure players cannot bypass it.
            for (int dx = -34; dx <= -4; dx += 5) {
                world.getBlockAt(bx + dx, by - 1, bz).setType(Material.POLISHED_DEEPSLATE, false);
                Material ear = dx % 10 == 0 ? Material.SCULK_SHRIEKER : Material.SCULK_SENSOR;
                world.getBlockAt(bx + dx, by, bz - 3).setType(ear, false);
                world.getBlockAt(bx + dx, by, bz + 3).setType(Material.SCULK_SENSOR, false);
                world.getBlockAt(bx + dx, by - 1, bz - 3).setType(Material.SCULK, false);
                world.getBlockAt(bx + dx, by - 1, bz + 3).setType(Material.SCULK, false);
            }
            for (int dx = -37; dx <= -1; dx++) {
                world.getBlockAt(bx + dx, by, bz - 5).setType(Material.DEEPSLATE_BRICK_WALL, false);
                world.getBlockAt(bx + dx, by, bz + 5).setType(Material.DEEPSLATE_BRICK_WALL, false);
            }
        }
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
        placeEvidenceLectern(new Location(world, bx - 7, by, bz - 2), BlockFace.EAST,
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
            // A flush inlaid line preserves the exact listener plane while leaving the authored
            // feet/head cells clear. Corridor-width carpet in the feet layer was visually thin but
            // failed deterministic Adventure traversal and could snag movement at the gate approach.
            world.getBlockAt(bx + dx, by - 1, bz).setType(Material.BLACK_CONCRETE, false);
            world.getBlockAt(bx + dx, by, bz).setType(Material.AIR, false);
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
        placeDecorativeBookshelf(world.getBlockAt(bx - 5, by, bz + 4), 11, BlockFace.NORTH);
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
        // Raised basin: foot-level water (the anchor cell itself must stay physical for the site
        // audit) held inside a one-block curb, so nothing can flood the archive walking floor.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean water = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                boolean curb = !water && Math.abs(dx) <= 3 && Math.abs(dz) <= 3;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(water ? Material.PRISMARINE_BRICKS : Material.DARK_PRISMARINE, false);
                if (water) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.WATER, false);
                } else if (curb) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.DARK_PRISMARINE, false);
                } else {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.AIR, false);
                }
                world.getBlockAt(bx + dx, by + 1, bz + dz).setType(Material.AIR, false);
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
        placeDecorativeBookshelf(world.getBlockAt(bx - 2, by, bz), 17, BlockFace.NORTH);
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
                placeDecorativeBookshelf(world.getBlockAt(x, by, bz + 1), i + 23, BlockFace.NORTH);
                world.getBlockAt(x, by + 1, bz).setType(Material.SOUL_LANTERN, false);
            }
        }
        // The public approach is north of the seal. Keep the mason's working copy facing that
        // standing lane so the exact V5 LC01 book can reuse this authored furniture instead of
        // spawning a second lectern inside the sealed composition.
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2),
                holdFixtureFront("undercroft_seal"),
                "entry five shelf", List.of(
                        "entry five was set apart with a warm lamp.",
                        "do not price it.",
                        "do not count it with the cold shelf."
                ));
    }

    private void buildHoldFarWaterCore(World world, int bx, int by, int bz) {
        // Raised shoreline basin: foot-level reed water (the anchor cell must stay physical for
        // the site audit) held behind a one-block curb so the reading floor stays dry.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean water = Math.abs(dx) <= 5 && Math.abs(dz) <= 2;
                boolean curb = !water && Math.abs(dx) <= 6 && Math.abs(dz) <= 3;
                world.getBlockAt(bx + dx, by - 1, bz + dz)
                        .setType(water ? Material.DARK_PRISMARINE : Material.POLISHED_BLACKSTONE_BRICKS, false);
                if (water) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.WATER, false);
                } else if (curb) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                } else {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.AIR, false);
                }
                world.getBlockAt(bx + dx, by + 1, bz + dz).setType(Material.AIR, false);
            }
        }
        world.getBlockAt(bx, by, bz - 3).setType(Material.SEA_LANTERN, false);
        placeDecorativeBookshelf(world.getBlockAt(bx - 7, by, bz - 3), 41, BlockFace.NORTH);
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
        // The public approach is north of the seal; this is the fixture-owned LC01 book mount.
        placeEvidenceLectern(new Location(world, bx - 5, by, bz - 2),
                holdFixtureFront("undercroft_seal"),
                "mason's rest", List.of(
                        "the seal was entered from the wrong side.",
                        "the mason cut the last line low.",
                        "bow to read what was not small."
                ));
    }

    private void buildHoldForgottenMouthCore(World world, int bx, int by, int bz) {
        // A legible, freestanding remnant of a surface doorway: five-block clear mouth, complete
        // lintel, side buttresses, and restrained moss/rubble.  Nothing is allowed to masquerade as
        // a random terrain blob or consume the room's approach path.
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = 0; dy <= 6; dy++) {
                boolean frame = Math.abs(dx) >= 4 || dy >= 5;
                Block block = world.getBlockAt(bx + dx, by + dy, bz + 4);
                if (!frame) {
                    block.setType(Material.AIR, false);
                } else {
                    int hash = Math.floorMod(dx * 17 + dy * 31, 7);
                    block.setType(hash == 0 ? Material.MOSSY_COBBLESTONE
                            : (hash == 1 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS), false);
                }
            }
        }
        for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(
                        Math.floorMod(dx + dz, 5) == 0 ? Material.MOSS_BLOCK : Material.STONE_BRICKS, false);
            }
        }
        // The healed surface and paired return mark are evidence, not decorative prose: preserve
        // them as a readable floor sequence inside the buried remnant without blocking the mouth.
        world.getBlockAt(bx - 2, by - 1, bz + 2).setType(Material.GRASS_BLOCK, false);
        world.getBlockAt(bx + 2, by - 1, bz + 2).setType(Material.GRASS_BLOCK, false);
        world.getBlockAt(bx, by - 1, bz + 1).setType(Material.GLOWSTONE, false);
        world.getBlockAt(bx, by - 1, bz + 2).setType(Material.SEA_LANTERN, false);
        for (int[] rubble : new int[][]{{-6, 3}, {-5, 2}, {5, 2}, {6, 3}, {-6, 4}, {6, 4}}) {
            world.getBlockAt(bx + rubble[0], by, bz + rubble[1]).setType(
                    Math.floorMod(rubble[0], 2) == 0 ? Material.MOSSY_COBBLESTONE_WALL
                            : Material.MOSSY_COBBLESTONE, false);
        }
        world.getBlockAt(bx, by + 4, bz + 4).setType(materialOr(Material.IRON_BARS, "CHAIN"), false);
        world.getBlockAt(bx, by + 3, bz + 4).setType(Material.LANTERN, false);
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
                    placeDecorativeBookshelf(world.getBlockAt(sx, by, sz), stall + 41, BlockFace.NORTH);
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
        placeDecorativeBookshelf(world.getBlockAt(bx + 5, by, bz + 5), 31, BlockFace.NORTH);
        placeDecorativeBookshelf(world.getBlockAt(bx + 7, by, bz + 5), 37, BlockFace.NORTH);
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
        // Keep the pit supported and put the audible breach witness on the registered focal cell.
        world.getBlockAt(bx, by - 1, bz).setType(Material.SCULK, false);
        world.getBlockAt(bx, by, bz).setType(Material.SCULK_SENSOR, false);
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
        // Registered focal: a non-coop witness mark centered between the four visible cage groups.
        world.getBlockAt(bx, by, bz).setType(Material.CHISELED_TUFF, false);
        world.getBlockAt(bx, by + 1, bz).setType(Material.GRAY_CANDLE, false);
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

    private void buildHoldKeeperStoneCore(World world, int bx, int by, int bz,
                                          String siteId, BlockFace playerFront) {
        if (world == null) return;
        BlockFace front = playerFront == null ? BlockFace.NORTH : playerFront;
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.DEEPSLATE_TILES);

        // The anchor is the keeper stone itself. Everything else remains on the room side of the
        // shell and leaves a six-block sightline from the manifest standing zone.
        world.getBlockAt(bx, by, bz).setType(Material.CHISELED_DEEPSLATE, false);
        world.getBlockAt(bx, by + 1, bz).setType(Material.POLISHED_BASALT, false);
        world.getBlockAt(bx, by + 2, bz).setType(Material.CHISELED_TUFF, false);
        world.getBlockAt(bx, by + 3, bz).setType(Material.BLACK_CANDLE, false);

        int sx = bx + (front.getModX() * 2);
        int sz = bz + (front.getModZ() * 2);
        placeEditableStandingSign(new Location(world, sx, by, sz), front,
                new String[]{"keeper record", siteId.substring("stone_".length()), "", ""});

        // Flanking ribs identify the focal object without placing anything in its approach aisle.
        BlockFace side = front == BlockFace.EAST || front == BlockFace.WEST ? BlockFace.NORTH : BlockFace.EAST;
        for (int sign : new int[]{-1, 1}) {
            int px = bx + side.getModX() * sign * 3;
            int pz = bz + side.getModZ() * sign * 3;
            world.getBlockAt(px, by, pz).setType(Material.POLISHED_DEEPSLATE, false);
            world.getBlockAt(px, by + 1, pz).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(px, by + 2, pz).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void buildHoldColdHearthCore(World world, int bx, int by, int bz) {
        if (world == null) return;
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_BLACKSTONE);
        Block hearth = world.getBlockAt(bx, by, bz);
        hearth.setType(Material.SOUL_CAMPFIRE, false);
        if (hearth.getBlockData() instanceof org.bukkit.block.data.type.Campfire campfire) {
            campfire.setLit(false);
            hearth.setBlockData(campfire, false);
        }
        for (int dz = -3; dz <= 3; dz++) {
            world.getBlockAt(bx - 3, by, bz + dz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
        }
        placeEvidenceLectern(new Location(world, bx + 5, by, bz + 2), BlockFace.WEST,
                "cold account", List.of(
                        "The account calls this warmth. The stone and the land do not.",
                        "Compare the kept story with the place that was left cold.",
                        "A comfortable record can still be false."
                ));
    }

    private void buildHoldRosettaCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.CHISELED_TUFF);
        for (int i = 0; i < 7; i++) {
            int x = bx - 6 + (i * 2);
            Material mat = i == 6 ? Material.GRAY_CONCRETE : Material.CHISELED_DEEPSLATE;
            world.getBlockAt(x, by, bz).setType(mat, false);
            world.getBlockAt(x, by + 1, bz).setType(i == 6 ? Material.BLACK_CANDLE : Material.WHITE_CANDLE, false);
        }
        placeEvidenceLectern(new Location(world, bx - 4, by, bz + 3), BlockFace.EAST,
                "rosetta cover", List.of(
                        "The runes are not a secret alphabet. They are a clerk's shortcut for things the Keepers already knew.",
                        "Six hands were copied clean. The grey seventh was copied after the room was built.",
                        "Read the stone, then read the copy. The order is the first lie."
                ));
        placeStandingSign(new Location(world, bx + 6, by, bz + 3), BlockFace.WEST,
                new String[]{"six copied", "one added", "low hand", "low truth"});
        // Keep the registered anchor on the actual Rosetta focal stone so runtime audit and
        // interaction proximity cannot point at an arbitrary floor tile.
        world.getBlockAt(bx, by, bz).setType(Material.CHISELED_TUFF, false);
    }

    private void buildHoldReckoningCore(World world, int bx, int by, int bz) {
        buildHoldStoneReadingFloor(world, bx, by, bz, Material.POLISHED_BLACKSTONE_BRICKS);
        for (int dz = -4; dz <= 4; dz += 2) {
            world.getBlockAt(bx, by, bz + dz).setType(dz == 0 ? Material.CHISELED_TUFF : Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx - 3, by, bz + dz).setType(Material.BLACK_CONCRETE, false);
            world.getBlockAt(bx + 3, by, bz + dz).setType(Material.GRAY_CONCRETE, false);
        }
        placeEvidenceLectern(new Location(world, bx - 6, by, bz), BlockFace.EAST,
                "reckoning copy", List.of(
                        "The reckoning stone repeats the Rosetta, but the line has been turned toward judgment.",
                        "No single keeper owns the answer. The room asks whether the record can survive being corrected.",
                        "The old hands did not read this standing tall. That was the point."
                ));
        placeStandingSign(new Location(world, bx + 6, by, bz), BlockFace.WEST,
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
        placeEvidenceLectern(new Location(world, bx - 6, by, bz + 2), BlockFace.EAST,
                "threshold note", List.of(
                        "This door was written as a grave so no one would ask who was still moving behind it.",
                        "The date is not prophecy. It is appointment language.",
                        "If the group reaches this alone, the room should feel wrong."
                ));
        placeStandingSign(new Location(world, bx + 6, by, bz + 2), BlockFace.WEST,
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
        int[][] tokenSlots = {{-6, 0}, {-3, 4}, {3, 4}, {6, 0}, {3, -4}, {-3, -4}};
        for (int i = 0; i < tokenSlots.length; i++) {
            int sx = bx + tokenSlots[i][0], sz = bz + tokenSlots[i][1];
            Block slot = world.getBlockAt(sx, by, sz);
            slot.setType(Material.BARREL, false);
            faceDirectional(slot.getLocation(), BlockFace.UP);
            if (slot.getState() instanceof InventoryHolder holder) {
                holder.getInventory().clear();
                slot.getState().update(true, false);
            }
            world.getBlockAt(sx, by - 1, sz).setType(Material.CHISELED_POLISHED_BLACKSTONE, false);
        }
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
        placeEvidenceLectern(new Location(world, bx - 4, by, bz + 3), BlockFace.EAST,
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
        Location protocolMount = new Location(world, bx - 6, by, bz + 4);
        setBlock(protocolMount, Material.LECTERN);
        faceDirectional(protocolMount, BlockFace.EAST);
        if (protocolMount.getBlock().getState() instanceof Lectern lectern) {
            lectern.getInventory().clear();
            lectern.update(true, false);
        }
        placeStandingSign(new Location(world, bx + 6, by, bz + 4), BlockFace.WEST,
                new String[]{"PUBLISH", "RELEASE UNNAMED", "BOTH RELEASE", "THEN SEVER"});
        placeHoldFinaleMarkers(new Location(world, bx, by, bz + 1));
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
        var nameKey = new org.bukkit.NamespacedKey(plugin, "v5_name_treatment");
        var visualKey = new org.bukkit.NamespacedKey(plugin, "finale_visual");
        var retiredSeventhKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.SeventhChoiceListener.PDC_SEVENTH_CHOICE);
        var retiredReleaseKey = new org.bukkit.NamespacedKey("observance",
                com.observance.watcher.signal.listener.ReleaseRiteListener.PDC_RELEASE);
        for (org.bukkit.entity.Entity old : base.getWorld().getNearbyEntities(base, 12.0, 6.0, 8.0)) {
            try {
                var pdc = old.getPersistentDataContainer();
                if (pdc.has(nameKey, org.bukkit.persistence.PersistentDataType.STRING)
                        || pdc.has(retiredSeventhKey, org.bukkit.persistence.PersistentDataType.STRING)
                        || pdc.has(retiredReleaseKey, org.bukkit.persistence.PersistentDataType.STRING)
                        || pdc.has(visualKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    old.remove();
                }
            } catch (Throwable ignored) { }
        }
        Object[][] markers = {
                {"publish", "PUBLISH — keep Averyn's name outside the Record; release her", Material.QUARTZ_BLOCK, -4.0},
                {"release_unnamed", "RELEASE UNNAMED — let the blank belong to Averyn; release her", Material.CRYING_OBSIDIAN, 4.0}
        };
        for (Object[] marker : markers) {
            Location at = base.clone().add((Double) marker[3], 0, 0);
            try {
                at.getBlock().setType((Material) marker[2], false);
                var interaction = at.getWorld().spawn(at.clone().add(0.5, 1.0, 0.5),
                        org.bukkit.entity.Interaction.class);
                interaction.setInteractionWidth(2.5f);
                interaction.setInteractionHeight(2.5f);
                interaction.setResponsive(true);
                interaction.customName(net.kyori.adventure.text.Component.text(
                        (String) marker[1], net.kyori.adventure.text.format.NamedTextColor.GRAY));
                interaction.setCustomNameVisible(true);
                interaction.setPersistent(true);
                interaction.getPersistentDataContainer().set(nameKey,
                        org.bukkit.persistence.PersistentDataType.STRING, (String) marker[0]);
                interaction.getPersistentDataContainer().set(visualKey,
                        org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            } catch (Throwable ignored) { }
        }
    }

    private void placeV5SeverControl(Location control) {
        if (control == null || control.getWorld() == null) return;
        var key = new org.bukkit.NamespacedKey(plugin,
                com.observance.watcher.finale.FinaleController.PDC_FINALE_CONTROL);
        for (org.bukkit.entity.Interaction old : control.getWorld().getNearbyEntitiesByType(
                org.bukkit.entity.Interaction.class, control, 4.0)) {
            if (old.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                old.remove();
            }
        }
        control.getBlock().setType(Material.SCULK_CATALYST, false);
        Location cell = control.clone().add(0, -1, 2);
        cell.getBlock().setType(Material.CHISELED_DEEPSLATE, false);
        placeStandingSign(control.clone().add(2, 0, 0), BlockFace.WEST,
                new String[]{"SEVER RECORD", "ARMED ONLY", "SNEAK 3 SEC", "THEN OPERATE"});
        var interaction = control.getWorld().spawn(control.clone().add(0.5, 1.0, 0.5),
                org.bukkit.entity.Interaction.class);
        interaction.setInteractionWidth(1.5f);
        interaction.setInteractionHeight(2.0f);
        interaction.setResponsive(true);
        interaction.setPersistent(true);
        interaction.customName(Component.text("SEVER RECORD", NamedTextColor.DARK_RED));
        interaction.setCustomNameVisible(true);
        interaction.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING,
                com.observance.watcher.finale.FinaleController.CONTROL_SEVER);
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

    private void buildHoldV2Shells(World world, int bx, int by, int bz) {
        if (world == null) return;
        buildHoldV2Mouth(world, bx, by, bz);
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            buildHoldOwnedRoom(world, bx, by, bz, room);
        }

        // V3 civic spine: every room wall is crossed deliberately and every gate has a level landing.
        buildHoldTunnelZ(world, bx, by, bz, 58, 72, -19, -24, 5);
        buildHoldTunnelZ(world, bx, by, bz, 84, 96, -24, -24, 5);
        buildHoldTunnelX(world, bx, by, bz, -23, -15, 112, -24, 4);
        buildHoldTunnelX(world, bx, by, bz, 15, 23, 112, -24, 4);
        buildHoldTunnelZ(world, bx, by, bz, 136, 140, -24, -28, 5);
        buildHoldTunnelZ(world, bx, by, bz, 140, 150, -28, -28, 5);

        // Keeper Court: three paired evidence bays, each with its own broad Adventure-mode doorway.
        for (int z : new int[]{168, 206, 246}) {
            buildHoldTunnelX(world, bx, by, bz, -36, -26, z, -28, 4);
            buildHoldTunnelX(world, bx, by, bz, 26, 36, z, -28, 4);
        }
        buildHoldTunnelZ(world, bx, by, bz, 264, 268, -28, -24, 5);
        buildHoldTunnelZ(world, bx, by, bz, 268, 276, -24, -24, 5);

        // Archive rooms open from three transverse streets. Outer rooms are reached from the street,
        // never by tunnelling through an inner exhibit room.
        int[] archiveRoomXs = {-95, -49, 49, 95};
        for (int streetZ : new int[]{270, 320, 366}) {
            buildHoldTunnelX(world, bx, by, bz, -95, 95, streetZ, -24, 4);
        }
        for (int x : archiveRoomXs) {
            buildHoldTunnelZ(world, bx + x, by, bz, 270, 282, -24, -24, 4);
            buildHoldTunnelZ(world, bx + x, by, bz, 320, 328, -24, -24, 4);
            buildHoldTunnelZ(world, bx + x, by, bz, 366, 374, -24, -24, 4);
        }
        buildHoldTunnelZ(world, bx, by, bz, 414, 432, -24, -24, 5);

        // Lampworks to lower Hold.
        buildHoldTunnelZ(world, bx, by, bz, 490, 498, -24, -28, 5);
        buildHoldTunnelZ(world, bx, by, bz, 498, 562, -28, -28, 5);
        buildHoldTunnelX(world, bx, by, bz, -12, 12, 532, -28, 5);
        buildHoldTunnelX(world, bx, by, bz, -70, -54, 528, -28, 4);
        buildHoldTunnelZ(world, bx - 88, by, bz, 546, 556, -28, -28, 4);
        buildHoldTunnelX(world, bx, by, bz, 54, 90, 520, -28, 4);
        buildHoldTunnelX(world, bx, by, bz, 38, 46, 582, -28, 4);
        buildHoldTunnelZ(world, bx, by, bz, 604, 614, -28, -28, 5);
        buildHoldTunnelX(world, bx, by, bz, -12, 12, 632, -28, 5);
        buildHoldTunnelZ(world, bx, by, bz, 648, 652, -28, -32, 5);
        buildHoldTunnelZ(world, bx, by, bz, 652, 660, -32, -32, 5);
        buildHoldTunnelZ(world, bx, by, bz, 716, 734, -32, -32, 5);
        buildHoldDreadPassage(world, bx, by, bz);
        dressHoldV3Districts(world, bx, by, bz);
    }

    /**
     * Gives every owned room a visible civic purpose.  This pass intentionally runs after the
     * connector carve, so no table, shelf, bench, or stall can resurrect one of the sealed-wall
     * failures found in the live V2 walk.  Puzzle fixtures are placed later and therefore remain
     * the final authority inside their declared footprints.
     */
    private void dressHoldV3Districts(World world, int bx, int by, int bz) {
        if (world == null) return;
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            int floor = by + room.floorY();
            int cx = (room.minX() + room.maxX()) / 2;
            int cz = (room.minZ() + room.maxZ()) / 2;
            switch (room.id()) {
                case "orientation_west", "orientation_center", "orientation_east" ->
                        dressHoldOrientationRoom(world, bx, bz, room, floor, cx, cz);
                case "keeper_nave" -> dressHoldKeeperNave(world, bx, bz, floor);
                case "archive_nave" -> dressHoldArchiveNave(world, bx, bz, floor);
                case "archive_school" -> dressHoldSchoolroom(world, bx, bz, room, floor);
                case "archive_markers" -> dressHoldMarkerGallery(world, bx, bz, room, floor);
                case "archive_cistern", "archive_water" ->
                        dressHoldWaterRoom(world, bx, bz, room, floor);
                case "archive_watch" -> dressHoldWatchRoom(world, bx, bz, room, floor);
                case "archive_shelf" -> dressHoldLibraryRoom(world, bx, bz, room, floor);
                case "archive_market", "archive_stall" ->
                        dressHoldMarketRoom(world, bx, bz, room, floor);
                case "archive_ration" -> dressHoldRationHall(world, bx, bz, room, floor);
                case "archive_breach" -> dressHoldBreachRoom(world, bx, bz, room, floor);
                case "archive_warm" -> dressHoldWarmTownRoom(world, bx, bz, room, floor);
                case "archive_coops" -> dressHoldCoopRoom(world, bx, bz, room, floor);
                case "puzzle_works" -> dressHoldLampworks(world, bx, bz, room, floor);
                case "prior_case", "prior_camp" ->
                        dressHoldPriorRooms(world, bx, bz, room, floor);
                case "lower_reckoning", "lower_threshold", "lower_convergence",
                     "lower_vault", "lower_altar", "lower_coop" ->
                        dressHoldLowerRoom(world, bx, bz, room, floor);
                case "dread" -> dressHoldDreadRoom(world, bx, bz, room, floor);
                default -> {
                    // Keeper evidence bays, the mouth, Accepting, and Unwriting are already dominated
                    // by their large authored fixture footprints.  Their perimeter architecture stays
                    // deliberately quieter so the puzzle itself remains readable.
                }
            }
        }
    }

    private void dressHoldOrientationRoom(World world, int bx, int bz, HoldRoomBox room,
                                           int floor, int cx, int cz) {
        for (int dz : new int[]{-12, 12}) {
            int z = cz + dz;
            if (!holdInteriorReserved(room, cx, z, 3)) {
                placeHoldTable(world, bx + cx, floor, bz + z, 5, true, Material.TUFF_BRICK_SLAB);
                placeHoldBench(world, bx + cx, floor, bz + z - 2, 5, true, BlockFace.SOUTH);
                placeHoldBench(world, bx + cx, floor, bz + z + 2, 5, true, BlockFace.NORTH);
            }
        }
    }

    private void dressHoldKeeperNave(World world, int bx, int bz, int floor) {
        for (int courtZ : new int[]{168, 206, 246}) {
            for (int x : new int[]{-15, 15}) {
                placeHoldBench(world, bx + x, floor, bz + courtZ - 8, 7, true,
                        x < 0 ? BlockFace.EAST : BlockFace.WEST);
                placeHoldBench(world, bx + x, floor, bz + courtZ + 8, 7, true,
                        x < 0 ? BlockFace.EAST : BlockFace.WEST);
                placeHoldLampPost(world, bx + x, floor, bz + courtZ);
            }
        }
        for (int z = 158; z <= 258; z += 20) {
            world.getBlockAt(bx - 7, floor - 1, bz + z).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx + 7, floor - 1, bz + z).setType(Material.CHISELED_DEEPSLATE, false);
        }
    }

    private void dressHoldArchiveNave(World world, int bx, int bz, int floor) {
        // Six compact catalogue islands occupy the long archive without touching the central aisle
        // or the three transverse streets at Z 320 and 366.
        for (int z : new int[]{290, 306, 338, 352, 384, 402}) {
            for (int x : new int[]{-13, 13}) {
                placeHoldShelfBank(world, bx + x, floor, bz + z, 5,
                        x < 0 ? BlockFace.EAST : BlockFace.WEST);
                placeHoldTable(world, bx + (x < 0 ? -7 : 7), floor, bz + z, 3,
                        false, Material.POLISHED_DEEPSLATE_SLAB);
            }
        }
    }

    private void dressHoldSchoolroom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int z : new int[]{room.minZ() + 9, room.maxZ() - 9}) {
            for (int x = room.minX() + 9; x <= room.maxX() - 9; x += 10) {
                if (holdInteriorReserved(room, x, z, 2)) continue;
                placeHoldTable(world, bx + x, floor, bz + z, 3, true, Material.OAK_SLAB);
                placeHoldBench(world, bx + x, floor, bz + z + 2, 3, true, BlockFace.NORTH);
            }
        }
        placeHoldShelfBank(world, bx + room.minX() + 5, floor, bz + room.maxZ() - 7,
                5, BlockFace.EAST);
    }

    private void dressHoldMarkerGallery(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int z = room.minZ() + 8; z <= room.maxZ() - 8; z += 8) {
            for (int x : new int[]{room.minX() + 6, room.maxX() - 6}) {
                if (holdInteriorReserved(room, x, z, 2)) continue;
                world.getBlockAt(bx + x, floor, bz + z).setType(Material.CUT_COPPER, false);
                world.getBlockAt(bx + x, floor + 1, bz + z).setType(Material.LIGHTNING_ROD, false);
                world.getBlockAt(bx + x, floor - 1, bz + z).setType(Material.WAXED_CUT_COPPER, false);
            }
        }
    }

    private void dressHoldWaterRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        int x = room.minX() + 7;
        for (int z = room.minZ() + 8; z <= room.maxZ() - 8; z++) {
            if (holdInteriorReserved(room, x, z, 2)) continue;
            world.getBlockAt(bx + x, floor - 1, bz + z).setType(Material.DARK_PRISMARINE, false);
            if (z % 6 == 0) world.getBlockAt(bx + x, floor, bz + z).setType(Material.CAULDRON, false);
        }
        placeHoldTable(world, bx + room.maxX() - 8, floor, bz + room.minZ() + 8,
                5, false, Material.DARK_PRISMARINE_SLAB);
    }

    private void dressHoldWatchRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int x : new int[]{room.minX() + 7, room.maxX() - 7}) {
            int z = room.maxZ() - 7;
            if (holdInteriorReserved(room, x, z, 3)) continue;
            for (int y = 0; y <= 3; y++) {
                world.getBlockAt(bx + x, floor + y, bz + z).setType(Material.SCAFFOLDING, false);
            }
            world.getBlockAt(bx + x, floor + 4, bz + z).setType(Material.SOUL_LANTERN, false);
        }
        placeHoldTable(world, bx + (room.minX() + room.maxX()) / 2, floor,
                bz + room.minZ() + 8, 7, true, Material.POLISHED_DEEPSLATE_SLAB);
    }

    private void dressHoldLibraryRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int x = room.minX() + 7; x <= room.maxX() - 7; x += 9) {
            for (int z : new int[]{room.minZ() + 7, room.maxZ() - 7}) {
                if (!holdInteriorReserved(room, x, z, 2)) {
                    placeHoldShelfBank(world, bx + x, floor, bz + z, 5,
                            z < (room.minZ() + room.maxZ()) / 2 ? BlockFace.SOUTH : BlockFace.NORTH);
                }
            }
        }
    }

    private void dressHoldMarketRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        int[][] stalls = {
                {room.minX() + 8, room.minZ() + 8}, {room.maxX() - 8, room.minZ() + 8},
                {room.minX() + 8, room.maxZ() - 8}, {room.maxX() - 8, room.maxZ() - 8}
        };
        for (int[] stall : stalls) {
            if (!holdInteriorReserved(room, stall[0], stall[1], 3)) {
                placeHoldMarketStall(world, bx + stall[0], floor, bz + stall[1]);
            }
        }
    }

    private void dressHoldRationHall(World world, int bx, int bz, HoldRoomBox room, int floor) {
        int cx = (room.minX() + room.maxX()) / 2;
        for (int z : new int[]{room.minZ() + 9, room.maxZ() - 9}) {
            if (!holdInteriorReserved(room, cx, z, 4)) {
                placeHoldTable(world, bx + cx, floor, bz + z, 9, true, Material.SPRUCE_SLAB);
                placeHoldBench(world, bx + cx, floor, bz + z - 2, 9, true, BlockFace.SOUTH);
                placeHoldBench(world, bx + cx, floor, bz + z + 2, 9, true, BlockFace.NORTH);
                world.getBlockAt(bx + cx - 6, floor, bz + z).setType(Material.BARREL, false);
                world.getBlockAt(bx + cx + 6, floor, bz + z).setType(Material.BARREL, false);
            }
        }
    }

    private void dressHoldBreachRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int i = 0; i < 22; i++) {
            int x = room.minX() + 6 + Math.floorMod(i * 11, Math.max(1, room.maxX() - room.minX() - 12));
            int z = room.minZ() + 6 + Math.floorMod(i * 17, Math.max(1, room.maxZ() - room.minZ() - 12));
            if (holdInteriorReserved(room, x, z, 2) || Math.abs(x) < 5) continue;
            world.getBlockAt(bx + x, floor, bz + z).setType(i % 3 == 0
                    ? Material.COBBLED_DEEPSLATE_WALL : Material.COBBLED_DEEPSLATE, false);
        }
    }

    private void dressHoldWarmTownRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int x : new int[]{room.minX() + 8, room.maxX() - 8}) {
            for (int z : new int[]{room.minZ() + 8, room.maxZ() - 8}) {
                if (holdInteriorReserved(room, x, z, 3)) continue;
                world.getBlockAt(bx + x, floor, bz + z).setType(Material.CAMPFIRE, false);
                world.getBlockAt(bx + x - 1, floor, bz + z).setType(Material.BRICKS, false);
                world.getBlockAt(bx + x + 1, floor, bz + z).setType(Material.BRICKS, false);
                placeHoldBench(world, bx + x, floor, bz + z + 3, 3, true, BlockFace.NORTH);
            }
        }
    }

    private void dressHoldCoopRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int x : new int[]{room.minX() + 8, room.maxX() - 8}) {
            for (int z : new int[]{room.minZ() + 8, room.maxZ() - 8}) {
                if (holdInteriorReserved(room, x, z, 3)) continue;
                for (int dx = -2; dx <= 2; dx++) {
                    world.getBlockAt(bx + x + dx, floor, bz + z - 2).setType(Material.OAK_FENCE, false);
                    world.getBlockAt(bx + x + dx, floor, bz + z + 2).setType(Material.OAK_FENCE, false);
                }
                world.getBlockAt(bx + x, floor, bz + z).setType(Material.HAY_BLOCK, false);
            }
        }
    }

    private void dressHoldLampworks(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int z : new int[]{room.minZ() + 8, room.maxZ() - 8}) {
            for (int x = room.minX() + 8; x <= room.maxX() - 8; x += 10) {
                if (holdInteriorReserved(room, x, z, 3)) continue;
                placeHoldTable(world, bx + x, floor, bz + z, 5, true, Material.CUT_COPPER_SLAB);
                world.getBlockAt(bx + x, floor + 2, bz + z).setType(Material.REDSTONE_LAMP, false);
                world.getBlockAt(bx + x, floor, bz + z + 2).setType(Material.CRAFTER, false);
            }
        }
    }

    private void dressHoldPriorRooms(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int z = room.minZ() + 8; z <= room.maxZ() - 8; z += 10) {
            int x = room.minX() + 7;
            if (holdInteriorReserved(room, x, z, 3)) continue;
            world.getBlockAt(bx + x, floor, bz + z).setType(Material.BARREL, false);
            placeHoldShelfBank(world, bx + x + 2, floor, bz + z, 3, BlockFace.EAST);
        }
        if (room.id().equals("prior_camp")) {
            for (int x : new int[]{room.minX() + 9, room.maxX() - 9}) {
                int z = room.maxZ() - 9;
                if (!holdInteriorReserved(room, x, z, 3)) {
                    world.getBlockAt(bx + x, floor, bz + z).setType(Material.SOUL_CAMPFIRE, false);
                    placeHoldBench(world, bx + x, floor, bz + z + 3, 5, true, BlockFace.NORTH);
                }
            }
        }
    }

    private void dressHoldLowerRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        int cx = (room.minX() + room.maxX()) / 2;
        int cz = (room.minZ() + room.maxZ()) / 2;
        for (int[] p : new int[][]{{room.minX() + 6, room.minZ() + 6},
                {room.maxX() - 6, room.minZ() + 6}, {room.minX() + 6, room.maxZ() - 6},
                {room.maxX() - 6, room.maxZ() - 6}}) {
            if (holdInteriorReserved(room, p[0], p[1], 3)) continue;
            placeHoldLampPost(world, bx + p[0], floor, bz + p[1]);
            placeHoldBench(world, bx + p[0] + Integer.signum(cx - p[0]) * 3, floor,
                    bz + p[1], 3, false, p[0] < cx ? BlockFace.EAST : BlockFace.WEST);
        }
        world.getBlockAt(bx + cx, floor - 1, bz + cz).setType(Material.REINFORCED_DEEPSLATE, false);
    }

    private void dressHoldDreadRoom(World world, int bx, int bz, HoldRoomBox room, int floor) {
        for (int z = room.minZ() + 10; z <= room.maxZ() - 10; z += 14) {
            int x = room.maxX() - 6;
            if (holdInteriorReserved(room, x, z, 2)) continue;
            world.getBlockAt(bx + x, floor, bz + z).setType(Material.SCULK_CATALYST, false);
            world.getBlockAt(bx + x, floor + 1, bz + z).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void placeHoldTable(World world, int cx, int floor, int cz, int length,
                                boolean alongX, Material top) {
        int half = Math.max(1, length / 2);
        for (int i = -half; i <= half; i++) {
            int x = cx + (alongX ? i : 0);
            int z = cz + (alongX ? 0 : i);
            world.getBlockAt(x, floor + 1, z).setType(top, false);
            if (i == -half || i == half) world.getBlockAt(x, floor, z).setType(Material.SPRUCE_FENCE, false);
        }
    }

    private void placeHoldBench(World world, int cx, int floor, int cz, int length,
                                boolean alongX, BlockFace facing) {
        int half = Math.max(1, length / 2);
        for (int i = -half; i <= half; i++) {
            int x = cx + (alongX ? i : 0);
            int z = cz + (alongX ? 0 : i);
            Block block = world.getBlockAt(x, floor, z);
            block.setType(Material.SPRUCE_STAIRS, false);
            if (block.getBlockData() instanceof Directional directional) {
                directional.setFacing(facing);
                block.setBlockData(directional, false);
            }
        }
    }

    private void placeHoldShelfBank(World world, int cx, int floor, int cz, int length,
                                    BlockFace facing) {
        int half = Math.max(1, length / 2);
        boolean alongX = facing == BlockFace.NORTH || facing == BlockFace.SOUTH;
        for (int i = -half; i <= half; i++) {
            int x = cx + (alongX ? i : 0);
            int z = cz + (alongX ? 0 : i);
            placeDecorativeBookshelf(world.getBlockAt(x, floor, z), cx * 31 + cz + i, facing);
            placeDecorativeBookshelf(world.getBlockAt(x, floor + 1, z), cx * 31 + cz + i + 17, facing);
        }
    }

    private void placeHoldMarketStall(World world, int cx, int floor, int cz) {
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                for (int dy = 0; dy <= 3; dy++) {
                    world.getBlockAt(cx + dx, floor + dy, cz + dz).setType(Material.SPRUCE_FENCE, false);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(cx + dx, floor + 4, cz + dz).setType(
                        Math.floorMod(dx + dz, 2) == 0 ? Material.DARK_OAK_SLAB : Material.SPRUCE_SLAB, false);
            }
        }
        world.getBlockAt(cx - 1, floor, cz).setType(Material.BARREL, false);
        world.getBlockAt(cx + 1, floor, cz).setType(Material.CHEST, false);
    }

    private void placeHoldLampPost(World world, int x, int floor, int z) {
        world.getBlockAt(x, floor, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
        world.getBlockAt(x, floor + 1, z).setType(Material.POLISHED_BLACKSTONE_BRICK_WALL, false);
        world.getBlockAt(x, floor + 2, z).setType(Material.SOUL_LANTERN, false);
    }

    private void buildHoldV2Mouth(World world, int bx, int by, int bz) {
        for (int z = 0; z <= 86; z++) {
            int floor = by - Math.min(24, z / 3);
            for (int dx = -8; dx <= 8; dx++) {
                boolean walkway = Math.abs(dx) <= 5;
                world.getBlockAt(bx + dx, floor - 2, bz + z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + dx, floor - 1, bz + z).setType(
                        walkway ? Material.POLISHED_DEEPSLATE : Material.POLISHED_BLACKSTONE_BRICKS, false);
                for (int dy = 0; dy <= 9; dy++) {
                    Block block = world.getBlockAt(bx + dx, floor + dy, bz + z);
                    boolean wall = Math.abs(dx) >= 6;
                    boolean roof = dy >= 8;
                    block.setType(wall || roof
                            ? (dy == 8 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS)
                            : Material.AIR, false);
                }
            }
            if (z % 12 == 6) {
                world.getBlockAt(bx - 6, floor + 3, bz + z).setType(Material.SOUL_LANTERN, false);
                world.getBlockAt(bx + 6, floor + 3, bz + z).setType(Material.SOUL_LANTERN, false);
            }
        }
        buildHoldArch(world, bx, by, bz + 4, 12, 11);
    }

    private void buildHoldOwnedRoom(World world, int bx, int by, int bz, HoldRoomBox room) {
        int floor = by + room.floorY();
        int ceiling = by + room.ceilingY();
        Material accent = holdRoomAccent(room.id());
        for (int x = room.minX(); x <= room.maxX(); x++) {
            for (int z = room.minZ(); z <= room.maxZ(); z++) {
                boolean wallX = x <= room.minX() + 2 || x >= room.maxX() - 2;
                boolean wallZ = z <= room.minZ() + 2 || z >= room.maxZ() - 2;
                world.getBlockAt(bx + x, floor - 2, bz + z).setType(Material.DEEPSLATE, false);
                world.getBlockAt(bx + x, floor - 1, bz + z).setType(
                        (wallX || wallZ) ? Material.POLISHED_BLACKSTONE_BRICKS : accent, false);
                for (int y = floor; y <= ceiling + 2; y++) {
                    boolean wall = wallX || wallZ;
                    boolean roof = y >= ceiling;
                    Material material = roof ? Material.POLISHED_BLACKSTONE_BRICKS
                            : (wall ? (Math.floorMod(x + z + y, 9) == 0
                            ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS) : Material.AIR);
                    world.getBlockAt(bx + x, y, bz + z).setType(material, false);
                }
            }
        }
        int centerX = (room.minX() + room.maxX()) / 2;
        int centerZ = (room.minZ() + room.maxZ()) / 2;
        dressHoldOwnedRoomArchitecture(world, bx, bz, room, floor, ceiling, centerX, centerZ, accent);
        for (int[] light : new int[][]{{room.minX() + 3, centerZ}, {room.maxX() - 3, centerZ}}) {
            world.getBlockAt(bx + light[0], floor + 4, bz + light[1]).setType(Material.SOUL_LANTERN, false);
        }
        world.getBlockAt(bx + centerX, floor - 1, bz + centerZ).setType(accent, false);
    }

    private void dressHoldOwnedRoomArchitecture(World world, int bx, int bz, HoldRoomBox room,
                                                int floor, int ceiling, int centerX, int centerZ,
                                                Material accent) {
        int clearHeight = ceiling - floor;
        int pillarTop = Math.min(ceiling - 1, floor + Math.max(6, Math.min(11, clearHeight - 2)));
        // Repeated perimeter buttresses give every large chamber one architectural cadence without
        // creating interior maze walls. The central six-block mouths remain completely clear.
        for (int x = room.minX() + 4; x <= room.maxX() - 4; x += 8) {
            if (Math.abs(x - centerX) < 6) continue;
            if (!holdInteriorReserved(room, x, room.minZ() + 3, 4)) {
                buildHoldRoomPillar(world, bx + x, floor, bz + room.minZ() + 2, pillarTop, accent);
            }
            if (!holdInteriorReserved(room, x, room.maxZ() - 3, 4)) {
                buildHoldRoomPillar(world, bx + x, floor, bz + room.maxZ() - 2, pillarTop, accent);
            }
        }
        for (int z = room.minZ() + 4; z <= room.maxZ() - 4; z += 8) {
            if (Math.abs(z - centerZ) < 6) continue;
            if (!holdInteriorReserved(room, room.minX() + 3, z, 4)) {
                buildHoldRoomPillar(world, bx + room.minX() + 2, floor, bz + z, pillarTop, accent);
            }
            if (!holdInteriorReserved(room, room.maxX() - 3, z, 4)) {
                buildHoldRoomPillar(world, bx + room.maxX() - 2, floor, bz + z, pillarTop, accent);
            }
        }
        // High ribs and a restrained floor cross make the room read as one civic volume from its door.
        if (clearHeight >= 10) {
            for (int x = room.minX() + 3; x <= room.maxX() - 3; x++) {
                world.getBlockAt(bx + x, ceiling - 1, bz + centerZ).setType(
                        Math.floorMod(x, 5) == 0 ? accent : Material.POLISHED_BLACKSTONE_BRICKS, false);
            }
        }
        for (int x = room.minX() + 3; x <= room.maxX() - 3; x++) {
            world.getBlockAt(bx + x, floor - 1, bz + centerZ).setType(
                    Math.floorMod(x, 7) == 0 ? Material.CHISELED_DEEPSLATE : accent, false);
        }
        for (int z = room.minZ() + 3; z <= room.maxZ() - 3; z++) {
            world.getBlockAt(bx + centerX, floor - 1, bz + z).setType(
                    Math.floorMod(z, 7) == 0 ? Material.CHISELED_DEEPSLATE : accent, false);
        }
        dressHoldCivicVaultFrames(world, bx, bz, room, floor, ceiling, accent);
        dressHoldPerimeterFurniture(world, bx, bz, room, floor, accent);
    }

    private void dressHoldCivicVaultFrames(World world, int bx, int bz, HoldRoomBox room,
                                            int floor, int ceiling, Material accent) {
        if (world == null || room == null) return;
        int width = room.maxX() - room.minX();
        int depth = room.maxZ() - room.minZ();
        int beamY = Math.max(floor + 8, ceiling - 2);
        if (depth >= width) {
            for (int z = room.minZ() + 12; z <= room.maxZ() - 12; z += 18) {
                int left = room.minX() + 3;
                int right = room.maxX() - 3;
                if (holdVaultFrameReserved(room, true, z, left, right)) continue;
                buildHoldVaultFramePillar(world, bx + left, floor, bz + z, beamY, accent);
                buildHoldVaultFramePillar(world, bx + right, floor, bz + z, beamY, accent);
                for (int x = left; x <= right; x++) {
                    world.getBlockAt(bx + x, beamY, bz + z).setType(
                            Math.floorMod(x - left, 6) == 0 ? accent : Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
        } else {
            for (int x = room.minX() + 12; x <= room.maxX() - 12; x += 18) {
                int near = room.minZ() + 3;
                int far = room.maxZ() - 3;
                if (holdVaultFrameReserved(room, false, x, near, far)) continue;
                buildHoldVaultFramePillar(world, bx + x, floor, bz + near, beamY, accent);
                buildHoldVaultFramePillar(world, bx + x, floor, bz + far, beamY, accent);
                for (int z = near; z <= far; z++) {
                    world.getBlockAt(bx + x, beamY, bz + z).setType(
                            Math.floorMod(z - near, 6) == 0 ? accent : Material.POLISHED_BLACKSTONE_BRICKS, false);
                }
            }
        }
    }

    private boolean holdVaultFrameReserved(HoldRoomBox room, boolean acrossX,
                                           int fixed, int start, int end) {
        for (int cursor = start; cursor <= end; cursor += 2) {
            int x = acrossX ? cursor : fixed;
            int z = acrossX ? fixed : cursor;
            if (holdInteriorReserved(room, x, z, 3)) return true;
        }
        return false;
    }

    private void buildHoldVaultFramePillar(World world, int x, int floor, int z, int top, Material accent) {
        for (int y = floor; y <= top; y++) {
            world.getBlockAt(x, y, z).setType(y == floor || y == top
                    ? Material.CHISELED_DEEPSLATE
                    : (Math.floorMod(y - floor, 4) == 0 ? accent : Material.POLISHED_BASALT), false);
        }
        if (top - floor >= 8) {
            world.getBlockAt(x, top - 2, z).setType(Material.SOUL_LANTERN, false);
        }
    }

    private void dressHoldPerimeterFurniture(World world, int bx, int bz, HoldRoomBox room,
                                              int floor, Material accent) {
        if (world == null || room == null) return;
        int insideWest = room.minX() + 4;
        int insideEast = room.maxX() - 4;
        for (int z = room.minZ() + 8; z <= room.maxZ() - 8; z += 14) {
            for (int x : new int[]{insideWest, insideEast}) {
                if (holdInteriorReserved(room, x, z, 10)) continue;
                Block shelf = world.getBlockAt(bx + x, floor, bz + z);
                shelf.setType(room.id().contains("archive") ? Material.CHISELED_BOOKSHELF
                        : Material.POLISHED_DEEPSLATE_SLAB, false);
                if (shelf.getType() == Material.CHISELED_BOOKSHELF) {
                    placeDecorativeBookshelf(shelf, x * 31 + z, x == insideWest ? BlockFace.EAST : BlockFace.WEST);
                }
                world.getBlockAt(bx + x, floor + 1, bz + z).setType(
                        Math.floorMod(z, 2) == 0 ? Material.BLACK_CANDLE : accent, false);
            }
        }
    }

    private boolean holdInteriorReserved(HoldRoomBox room, int x, int z, int radius) {
        if (room == null) return true;
        int r = Math.max(3, radius);
        for (HoldSite site : DEEP_HOLD_SITES) {
            if (site.x() < room.minX() || site.x() > room.maxX()
                    || site.z() < room.minZ() || site.z() > room.maxZ()) continue;
            int halfX = site.halfX() > 0 ? site.halfX() : Math.min(12, Math.max(4, site.radius()));
            int halfZ = site.halfZ() > 0 ? site.halfZ() : Math.min(12, Math.max(4, site.radius()));
            if (x >= site.x() - halfX - r && x <= site.x() + halfX + r
                    && z >= site.z() - halfZ - r && z <= site.z() + halfZ + r) return true;
        }
        for (HoldRecordStation station : DEEP_HOLD_RECORD_STATIONS) {
            if (station.x() < room.minX() || station.x() > room.maxX()
                    || station.z() < room.minZ() || station.z() > room.maxZ()) continue;
            if (Math.abs(station.x() - x) <= r + 4 && Math.abs(station.z() - z) <= r + 4) return true;
        }
        return false;
    }

    private void buildHoldRoomPillar(World world, int x, int floor, int z, int top, Material accent) {
        for (int y = floor; y <= top; y++) {
            Material material = y == floor || y == top ? Material.CHISELED_DEEPSLATE
                    : (Math.floorMod(y - floor, 4) == 0 ? accent : Material.POLISHED_BASALT);
            world.getBlockAt(x, y, z).setType(material, false);
        }
        if (top - floor >= 7) {
            int offset = z % 2 == 0 ? 1 : -1;
            world.getBlockAt(x, floor + 4, z + offset).setType(Material.DEEPSLATE_BRICK_WALL, false);
            world.getBlockAt(x, floor + 5, z + offset).setType(Material.SOUL_LANTERN, false);
        }
    }

    private Material holdRoomAccent(String id) {
        if (id == null) return Material.DEEPSLATE_TILES;
        if (id.contains("sella") || id.contains("water") || id.contains("cistern")) return Material.DARK_PRISMARINE;
        if (id.contains("vaun") || id.contains("market") || id.contains("ration")) return Material.TUFF_BRICKS;
        if (id.contains("iss") || id.contains("warm")) return Material.POLISHED_BLACKSTONE_BRICKS;
        if (id.contains("accepting") || id.contains("unwriting")) return Material.POLISHED_BLACKSTONE;
        if (id.contains("archive") || id.contains("prior")) return Material.POLISHED_DEEPSLATE;
        return Material.DEEPSLATE_TILES;
    }

    private void buildHoldTunnelZ(World world, int centerX, int by, int baseZ,
                                  int z1, int z2, int startFloor, int endFloor, int halfWidth) {
        int min = Math.min(z1, z2), max = Math.max(z1, z2);
        int run = Math.max(1, max - min);
        for (int z = min; z <= max; z++) {
            double t = (z - min) / (double) run;
            int floor = by + (int) Math.round(startFloor + (endFloor - startFloor) * t);
            buildHoldTunnelSlice(world, centerX, floor, baseZ + z, true, halfWidth);
        }
    }

    private void buildHoldTunnelX(World world, int baseX, int by, int centerZ,
                                  int x1, int x2, int localZ, int floorOffset, int halfWidth) {
        int min = Math.min(x1, x2), max = Math.max(x1, x2);
        int floor = by + floorOffset;
        for (int x = min; x <= max; x++) {
            buildHoldTunnelSlice(world, baseX + x, floor, centerZ + localZ, false, halfWidth);
        }
    }

    private void buildHoldTunnelSlice(World world, int cx, int floor, int cz, boolean alongZ, int halfWidth) {
        for (int across = -halfWidth - 2; across <= halfWidth + 2; across++) {
            int x = alongZ ? cx + across : cx;
            int z = alongZ ? cz : cz + across;
            boolean wall = Math.abs(across) > halfWidth;
            world.getBlockAt(x, floor - 2, z).setType(Material.DEEPSLATE, false);
            world.getBlockAt(x, floor - 1, z).setType(
                    wall ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE, false);
            for (int dy = 0; dy <= 9; dy++) {
                world.getBlockAt(x, floor + dy, z).setType(
                        wall || dy >= 8 ? (dy == 8 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.DEEPSLATE_BRICKS)
                                : Material.AIR, false);
            }
        }
    }

    private void buildHoldDreadPassage(World world, int bx, int by, int bz) {
        int[][] points = {{88, 520}, {96, 520}, {112, 552}, {112, 592}, {96, 632}};
        for (int i = 0; i < points.length - 1; i++) {
            int x = points[i][0], z = points[i][1];
            int tx = points[i + 1][0], tz = points[i + 1][1];
            while (x != tx || z != tz) {
                buildHoldDreadSlice(world, bx + x, by - 28, bz + z);
                if (x != tx) x += Integer.signum(tx - x); else z += Integer.signum(tz - z);
            }
        }
        buildHoldDreadSlice(world, bx + 96, by - 28, bz + 632);
    }

    private void buildHoldDreadSlice(World world, int cx, int floor, int cz) {
        for (int dx = -5; dx <= 5; dx++) {
            boolean wall = Math.abs(dx) > 4;
            world.getBlockAt(cx + dx, floor - 2, cz).setType(Material.DEEPSLATE, false);
            world.getBlockAt(cx + dx, floor - 1, cz).setType(
                    dx == 0 ? Material.SCULK : Material.POLISHED_DEEPSLATE, false);
            for (int dy = 0; dy <= 8; dy++) {
                world.getBlockAt(cx + dx, floor + dy, cz).setType(
                        wall || dy >= 7 ? Material.BLACKSTONE : Material.AIR, false);
            }
        }
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
            if (y < previousY) setHoldEntryStairTread(world, sx, y, z, 5);
            previousY = y;
        }
        for (int z = entryZ + 1; z <= bz - 132; z++) {
            buildHoldEntryStairSlice(world, sx, bottomY, z, z - sz);
        }
        buildHoldEntryLanding(world, sx, bottomY, bz - 142);
        // Meet the V2 mouth at local Z=0. Stopping at bz-96 leaves a sealed 95-block gap between
        // the surface descent and the generated Hold.
        buildHoldCorridorZ(world, sx, bottomY, bz - 150, bz, 5, 8);
        buildHoldArch(world, sx, bottomY, entryZ + 4, 7, 9);
        buildHoldArch(world, sx, bottomY, bz - 126, 7, 9);
        placeStandingSign(new Location(world, sx - 5, sy, sz - 6), BlockFace.SOUTH,
                new String[]{"RETURN MOUTH", "count before", "descending", ""});
        placeStandingSign(new Location(world, sx + 4, bottomY, entryZ + 6), BlockFace.WEST,
                new String[]{"the stair", "ends where", "the count", "turns"});
    }

    private String auditBuiltHoldSurfaceMouth(Location surfaceMouth, Location base) {
        if (surfaceMouth == null || base == null || surfaceMouth.getWorld() == null
                || surfaceMouth.getWorld() != base.getWorld()) return "surface Return Mouth cannot be audited.";
        World world = surfaceMouth.getWorld();
        int sx = surfaceMouth.getBlockX();
        int sy = surfaceMouth.getBlockY();
        int sz = surfaceMouth.getBlockZ();
        int bottomY = base.getBlockY();
        int bz = base.getBlockZ();
        int entryZ = Math.max(sz + 96, bz - 186);
        int total = Math.max(1, entryZ - sz);
        for (int z = sz; z <= bz; z += 6) {
            int y = bottomY;
            if (z <= entryZ) {
                double t = (z - sz) / (double) total;
                y = (int) Math.round(sy + ((bottomY - sy) * t));
            }
            if (!hasHoldEntryWalkableSlice(world, sx, y, z)) {
                return "surface Return Mouth route is blocked near " + sx + "," + y + "," + z + ".";
            }
        }
        Block mouthSign = world.getBlockAt(sx - 5, sy, sz - 6);
        Block landingSign = world.getBlockAt(sx + 4, bottomY, entryZ + 6);
        for (Block sign : new Block[]{mouthSign, landingSign}) {
            if (!(sign.getState() instanceof Sign state)) {
                return "surface Return Mouth sign is missing at " + sign.getX() + "," + sign.getY() + "," + sign.getZ() + ".";
            }
            if (!sign.getRelative(BlockFace.DOWN).getType().isSolid()) {
                return "surface Return Mouth sign has no floor at " + sign.getX() + "," + sign.getY() + "," + sign.getZ() + ".";
            }
            boolean hasText = false;
            for (Component line : state.getSide(Side.FRONT).lines()) {
                if (!PlainTextComponentSerializer.plainText().serialize(line).isBlank()) hasText = true;
            }
            if (!hasText) return "surface Return Mouth sign is blank at " + sign.getX() + "," + sign.getY() + "," + sign.getZ() + ".";
        }
        return null;
    }

    private void registerHoldRegion(String worldName, int bx, int by, int bz, Location surfaceMouth) {
        int vertical = 56;
        plugin.registerRuntimeSite(new Site(HOLD_REGION_SITE_ID, "hold_region", worldName,
                (double) bx, (double) (by - 12), (double) (bz + 394), 430, vertical,
                true, true, null, false));
        if (surfaceMouth != null && surfaceMouth.getWorld() != null) {
            int mouthX = surfaceMouth.getBlockX();
            int mouthY = surfaceMouth.getBlockY();
            int mouthZ = surfaceMouth.getBlockZ();
            int midX = (mouthX + bx) / 2;
            int midY = (mouthY + by) / 2;
            int midZ = (mouthZ + bz) / 2;
            int entryRadius = Math.max(72, (Math.abs(bz - mouthZ) / 2) + 20);
            int entryVertical = Math.max(48, (Math.abs(mouthY - by) / 2) + 16);
            plugin.registerRuntimeSite(new Site(HOLD_ENTRY_REGION_SITE_ID, "hold_region", worldName,
                    (double) midX, (double) midY, (double) midZ, entryRadius, entryVertical,
                    true, true, null, false));
        }
    }

    private void buildHoldSurfaceApron(World world, int sx, int sy, int sz) {
        if (world == null) return;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -8; dz <= 18; dz++) {
                double d = Math.sqrt((dx * dx) + ((dz - 3) * (dz - 3) * 0.72));
                if (d > 12.2) continue;
                Material floor = d > 9.2 ? Material.MOSSY_COBBLESTONE
                        : (d > 5.5 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE);
                world.getBlockAt(sx + dx, sy - 2, sz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(sx + dx, sy - 1, sz + dz).setType(floor, false);
                for (int dy = 0; dy <= 8; dy++) {
                    boolean mouth = Math.abs(dx) <= 5 && dz >= -2 && dz <= 12;
                    boolean brokenRim = d > 8.2 && d < 11.6 && dy <= 3 && Math.floorMod(dx * 7 + dz, 4) == 0;
                    Block block = world.getBlockAt(sx + dx, sy + dy, sz + dz);
                    if (mouth) {
                        block.setType(Material.AIR, false);
                    } else if (brokenRim) {
                        block.setType(dy == 2 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS, false);
                    } else if (dy == 0 && d > 5.2 && Math.floorMod(dx - dz, 5) == 0) {
                        block.setType(Material.MOSSY_COBBLESTONE, false);
                    }
                }
            }
        }
        buildHoldArch(world, sx, sy, sz + 8, 7, 9);
    }

    private void buildHoldEntryStairSlice(World world, int cx, int y, int z, int step) {
        if (world == null) return;
        int outer = 7;
        int inner = 5;
        int height = 8;
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
            for (int dx : new int[]{-6, 6}) {
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
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -10; dz <= 14; dz++) {
                boolean rim = Math.abs(dx) == 12 || dz == -10 || dz == 14;
                boolean archPost = Math.abs(dx) == 8 && dz >= -6 && dz <= 10;
                world.getBlockAt(cx + dx, y - 2, cz + dz).setType(Material.DEEPSLATE, false);
                world.getBlockAt(cx + dx, y - 1, cz + dz)
                        .setType(rim ? Material.POLISHED_BLACKSTONE_BRICKS
                                : (Math.abs(dx) <= 2 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE), false);
                for (int dy = 0; dy <= 9; dy++) {
                    Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    if (rim || archPost || dy == 9) {
                        Material material = (dy == 9 || rim)
                                ? Material.POLISHED_BLACKSTONE_BRICKS
                                : Material.DEEPSLATE_BRICKS;
                        block.setType(material, false);
                    } else {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (int dz : new int[]{-6, 6, 12}) {
            buildHoldArch(world, cx, y, cz + dz, 8, 8);
        }
        placeStandingSign(new Location(world, cx - 6, y, cz + 8), BlockFace.EAST,
                new String[]{"return stair", "behind you", "city below", ""});
        placeStandingSign(new Location(world, cx + 6, y, cz + 8), BlockFace.WEST,
                new String[]{"do not dig", "the hold", "opens by", "record"});
    }

    private void placeHoldPrologueEcho(World world, int bx, int by, int bz) {
        if (world == null) return;
        placeEvidenceLectern(new Location(world, bx - 10, by - 24, bz + 76), BlockFace.EAST,
                "covered copy", List.of(
                        "This is a copy, not the first report.\n\nThe real first report belongs above, where the living first allow themselves to be counted.",
                        "The Hold kept only a cover mark: the record was carried underground after the opening, not before it.",
                        "A descent without the first report is a descent without a name. Return to the mouth before reading the city."
                ));
        placeDecorativeBookshelf(world.getBlockAt(bx - 12, by - 24, bz + 76), 83, BlockFace.EAST);
        placeStandingSign(new Location(world, bx - 6, by - 24, bz + 80), BlockFace.EAST,
                new String[]{"copied cover", "original", "stays above", ""});
    }

    private void placeHoldDistrictRecords(World world, int bx, int by, int bz) {
        if (world == null) return;
        placeHoldRecordStation(world, bx + 10, by - 24, bz + 76, BlockFace.WEST,
                "mouth register", List.of(
                        "Entry-mouth census.\n\nSix signed before the covered descent. A seventh mark was added later in grey ink and never matched a hand.",
                        "Do not call the grey line a prophecy. It is a correction made after the stair was sealed.",
                        "Later copies turned the six hand marks into rows for entry. One row stayed blank, but the lock still counted it.",
                        "The first report stays above because the first count was public. The Hold keeps the copy that proves the public story changed."
                ), 101);
        placeStandingSign(new Location(world, bx + 6, by - 24, bz + 76), BlockFace.WEST,
                new String[]{"first count", "above", "copy below", ""});

        placeHoldRecordStation(world, bx - 18, by - 28, bz + 154, BlockFace.EAST,
                "court census", List.of(
                        "Keeper court seating.\n\nVaun, Mara, Sella, Orin, Brann, Iss. Six chairs were cut into the ring before any lower work began.",
                        "Margin correction: one place was not cut. It was reserved by leaving the count unfinished.",
                        "When seven appears where six were built, trust the physical room over the speech."
                ), 117);
        placeStandingSign(new Location(world, bx - 14, by - 28, bz + 154), BlockFace.EAST,
                new String[]{"six seats", "one margin", "count the", "stone"});

        placeHoldRecordStation(world, bx - 16, by - 24, bz + 282, BlockFace.EAST,
                "intake rail", List.of(
                        "Archive intake rail.\n\nSchool, cistern, watch, shelf, water. These were separated so no single reader could see the lower pattern at once.",
                        "Read the side rooms as evidence, not decoration. Each one changes who looks guilty and who only looks useful.",
                        "Mara filed the copies by consequence. Brann filed the doors by convenience."
                ), 131);
        placeStandingSign(new Location(world, bx - 12, by - 24, bz + 282), BlockFace.EAST,
                new String[]{"records split", "on purpose", "follow what", "changed"});

        placeHoldRecordStation(world, bx - 98, by - 28, bz + 518, BlockFace.EAST,
                "prior roster", List.of(
                        "Prior accepting roster.\n\nSix names copied clean. Six keeper answers filed. Six tokens prepared.",
                        "Seventh line: no witness.\n\nDo not correct this to no seventh. The failed run had no one outside the finish.",
                        "The camp beyond this gate is not locked because it is sacred. It is locked because the same mistake should not be rehearsed twice.",
                        "Open condition to file: no witness."
                ), 139);
        placeStandingSign(new Location(world, bx - 94, by - 28, bz + 518), BlockFace.EAST,
                new String[]{"prior run", "six ready", "no witness", ""});

        placeHoldRecordStation(world, bx + 16, by - 24, bz + 282, BlockFace.WEST,
                "closure docket", List.of(
                        "Market closure docket WARDEN-3.\n\nPublic reason: unsafe wall. Private reason: the ration account proved light was moved before the collapse.",
                        "The warm-town story depends on smoke, but the ledgers depend on delivery. Compare which one had to be rewritten.",
                        "If the market feels too ordinary, keep reading. Ordinary records are how the lie survived."
                ), 149);
        placeStandingSign(new Location(world, bx + 12, by - 24, bz + 282), BlockFace.WEST,
                new String[]{"warden file", "counts goods", "before smoke", ""});

        placeHoldRecordStation(world, bx - 28, by - 24, bz + 438, BlockFace.EAST,
                "lamp count", List.of(
                        "Lampworks maintenance.\n\nFirst line kept. Second line borrowed. Third line went dry and was still marked ready.",
                        "Complaint note: a dark stand is not a missing stand. Do not break the wall for it. Bring light to the cup.",
                        "The black step is not a warning sign. It is an accounting mark for the place where the lower work stops being public.",
                        "Do not hurry past the descent. The lamps tell who paid, who carried, and who pretended not to know."
                ), 173);
        placeStandingSign(new Location(world, bx - 24, by - 24, bz + 438), BlockFace.EAST,
                new String[]{"lampworks", "counts debt", "not light", ""});

        placeHoldRecordStation(world, bx - 18, by - 28, bz + 566, BlockFace.EAST,
                "threshold hands", List.of(
                        "Threshold work note.\n\nThree actions were required so no single keeper could make the last door look like consent.",
                        "Plate. Name. Word. The order matters less than the fact that the room hears more than one person.",
                        "A group should argue here. If everyone agrees too quickly, they probably missed the earlier contradiction."
                ), 191);
        placeStandingSign(new Location(world, bx - 14, by - 28, bz + 566), BlockFace.EAST,
                new String[]{"three hands", "before the", "last warm", "floor"});

        placeHoldRecordStation(world, bx + 96, by - 28, bz + 514, BlockFace.EAST,
                "side hush", List.of(
                        "Side hush report.\n\nThis passage is not part of the formal count. That is why the formal count keeps failing.",
                        "A witness used the word elsewhere three times and crossed it out twice. The uncrossed word is the useful one.",
                        "Bring this back to the court only after the lower lamps make the first lie too small."
                ), 211);
        placeStandingSign(new Location(world, bx + 100, by - 28, bz + 514), BlockFace.EAST,
                new String[]{"not counted", "still true", "bring it", "back"});
    }

    private void placeHoldRecordStation(World world, int x, int y, int z, BlockFace facing,
                                        String title, List<String> pages, int shelfSeed) {
        if (world == null) return;
        BlockFace front = facing == null ? BlockFace.SOUTH : facing;
        placeEvidenceLectern(new Location(world, x, y, z), front, title, pages);
        placeHoldRecordShelves(world, x, y, z, front, shelfSeed);
    }

    private void placeHoldEmptyRecordStation(World world, int x, int y, int z, BlockFace facing,
                                             int shelfSeed) {
        if (world == null) return;
        BlockFace front = facing == null ? BlockFace.SOUTH : facing;
        Location mount = new Location(world, x, y, z);
        setBlock(mount, Material.LECTERN);
        faceDirectional(mount, front);
        if (mount.getBlock().getState() instanceof Lectern lectern) {
            lectern.getInventory().setItem(0, null);
            lectern.update(true, false);
        }
        placeHoldRecordShelves(world, x, y, z, front, shelfSeed);
    }

    private void placeHoldRecordShelves(World world, int x, int y, int z, BlockFace front,
                                        int shelfSeed) {
        BlockFace back = front.getOppositeFace();
        BlockFace side = holdLeftOf(front);
        for (int s : new int[]{-1, 1}) {
            int sx = x + back.getModX() + (side.getModX() * s);
            int sz = z + back.getModZ() + (side.getModZ() * s);
            placeDecorativeBookshelf(world.getBlockAt(sx, y, sz), shelfSeed + s, front);
            placeDecorativeBookshelf(world.getBlockAt(sx, y + 1, sz), shelfSeed + 9 + s, front);
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
        placeStandingSign(new Location(world, bx - 5, by - 24, bz + 98), BlockFace.NORTH,
                new String[]{"THE DEEP", "HOLD", "walk low", "count first"});
        placeStandingSign(new Location(world, bx + 8, by - 24, bz + 280), BlockFace.NORTH,
                new String[]{"the archive", "does not open", "for noise", ""});
        placeStandingSign(new Location(world, bx + 28, by - 24, bz + 436), BlockFace.WEST,
                new String[]{"below here", "the lamps", "keep accounts", ""});
        placeStandingSign(new Location(world, bx - 6, by - 28, bz + 568), BlockFace.NORTH,
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

    /**
     * An opened progression gate is itself the durable latch.  This keeps retreat/resume working
     * even if a director flag is temporarily unavailable after a restart: automatic sync may open
     * a gate, but it never closes an already-open gate behind a player.
     */
    private boolean holdGateLatchedOpen(String gateId) {
        HoldGate gate = holdGateById(gateId);
        if (gate == null) return false;
        Site site = plugin.sites().get(holdGateSiteId(gate.id()));
        Location location = site == null ? null : site.location();
        return location != null && !isHoldGateSealed(gate, location);
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
                for (int d = 0; d <= span.depth(); d++) {
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
            for (int d = 0; d <= span.depth(); d++) {
                for (int dy = span.height() + 1; dy <= span.height() + 5; dy++) {
                    Material material = Math.floorMod(across + d + dy, 7) == 0
                            ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_BLACKSTONE_BRICKS;
                    holdGateBlockAt(world, cx, y, cz, span, across, dy, d).setType(material, false);
                }
            }
        }
    }

    private static int holdGateReturnWidth(HoldGateSpan span) {
        if (span == null) return 4;
        return Math.max(4, Math.min(6, span.halfAcross() / 2));
    }

    private static boolean isHoldGateMaterial(Material material) {
        return material == Material.IRON_BARS
                || material == Material.BARRIER
                || material == Material.POLISHED_BLACKSTONE_BRICK_WALL;
    }

    private static HoldGateSpan holdGateSpan(HoldGate gate) {
        if (gate == null) return new HoldGateSpan(true, 7, 9, 1, 4);
        String planId = switch (gate.id()) {
            case "keeper" -> "g1";
            case "archive" -> "g2";
            case "undercroft" -> "g3";
            case "deep" -> "g4";
            case "accepting" -> "g5";
            case "coda" -> "g6";
            default -> gate.id();
        };
        DeepHoldV4Plan.Gate plan = DeepHoldV4Plan.GATES.stream()
                .filter(candidate -> planId.equals(candidate.id())).findFirst().orElse(null);
        if (plan == null) return new HoldGateSpan(true, 7, 9, 1, 4);
        int doorHalf = Math.max(3, Math.min(6, plan.halfAcross() / 2));
        return new HoldGateSpan(plan.acrossX(), plan.halfAcross(), plan.height(), plan.depth(), doorHalf);
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
        Location signLoc = holdGateLabelLocation(loc, span);
        placeStandingSign(signLoc, holdGateLabelFacing(span),
                new String[]{gate.label(), "registry access", "seal-controlled", ""});
    }

    private static Location holdGateLabelLocation(Location loc, HoldGateSpan span) {
        // Compact corridor buttresses occupy the wall strip outside the door jamb. Put a low,
        // non-colliding civic marker at the edge of the clear approach instead of embedding a wall
        // sign in that strip. It leaves the center route open and remains at player eye level.
        return span.acrossX() ? loc.clone().add(-span.doorHalf(), 0, -3)
                : loc.clone().add(-3, 0, -span.doorHalf());
    }

    private static BlockFace holdGateLabelFacing(HoldGateSpan span) {
        return span.acrossX() ? BlockFace.NORTH : BlockFace.WEST;
    }

    private void syncPlaceHoldGates(CommandSender sender) {
        var runtime = plugin.v5Runtime();
        if (runtime == null) {
            sender.sendMessage("Observance: V5 local runtime is unavailable; no gate or book was changed.");
            return;
        }
        runtime.projectLocalState();
        sender.sendMessage("Observance: projected gates and books from the durable local V5 record.");
    }

    /** Periodic, silent live sync used by the plugin scheduler. It is inert until a Hold is placed. */
    public void syncPlaceHoldGatesAutomatically() {
        if (!isDeepHoldBuilt()) return;
        var runtime = plugin.v5Runtime();
        if (runtime != null) runtime.projectLocalState();
    }

    private void handlePlaceHoldAudit(CommandSender sender) {
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance Deep Hold audit: no sites loaded. Run /obs reload or build the hold first.");
            return;
        }
        int placed = 0;
        int critical = 0;
        int recordStations = 0;
        List<String> notes = new ArrayList<>();
        Location mouth = resolveDeepHoldV4Mouth();
        for (String manifestIssue : DeepHoldV5Manifest.validate()) {
            critical++;
            addAuditIssue(notes, "V5 manifest: " + manifestIssue);
        }

        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            Site site = plugin.sites().get(fixture.id());
            Location loc = site == null ? null : site.location();
            if (site == null || loc == null || loc.getWorld() == null) {
                critical++;
                addAuditIssue(notes, fixture.id() + " missing");
                continue;
            }
            placed++;
            if (!fixture.type().equals(site.type()) || fixture.radius() != site.radius()
                    || fixture.verticalRadius() != site.verticalRadius() || !site.protect()) {
                critical++;
                addAuditIssue(notes, fixture.id() + " runtime metadata differs from canonical V5 contract");
            }
            if (mouth != null) {
                String frameIssue = auditV4FixtureFrame(loc.getWorld(), mouth, fixture);
                if (frameIssue != null) {
                    critical++;
                    addAuditIssue(notes, frameIssue);
                }
            }
            if (hasMaterialNear(loc, Math.max(3, site.radius()), Material.BEACON)) {
                critical++;
                addAuditIssue(notes, fixture.id() + " still has retired beacon material.");
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
        }
        Site entryRegion = plugin.sites().get(HOLD_ENTRY_REGION_SITE_ID);
        Location entryLoc = entryRegion == null ? null : entryRegion.location();
        if (entryRegion == null || entryLoc == null || entryLoc.getWorld() == null) {
            critical++;
            addAuditIssue(notes, "deep hold entry stair protection region missing");
        } else if (!"hold_region".equals(entryRegion.type()) || !entryRegion.protect()) {
            critical++;
            addAuditIssue(notes, "deep hold entry stair region is not protected/type=hold_region");
        }

        if (mouth == null || mouth.getWorld() == null) {
            critical++;
            addAuditIssue(notes, "V5 Surface Mouth cannot be reconstructed from placed fixtures");
        } else {
            World world = mouth.getWorld();
            String routeIssue = auditV4OpenRoute(world, mouth);
            if (routeIssue != null) {
                critical++;
                addAuditIssue(notes, routeIssue);
            }
            // V5 intentionally ships its controlled record mounts empty. Their geometry and
            // reachability are owned by the physical-authority audit below; requiring the
            // retired V4 written books here turns the correct locked state into a false blocker.
            recordStations = DeepHoldV4Plan.RECORD_STATIONS.size() + 1;
            Report physical = new V5PhysicalComponentInstaller(plugin).auditHold(mouth);
            for (String fault : physical.blockerMessages()) {
                critical++;
                addAuditIssue(notes, "physical: " + fault);
            }
        }
        var runtime = plugin.v5Runtime();
        List<String> runtimeFindings = runtime == null
                ? List.of("V5 runtime coordinator is unavailable") : runtime.readinessFindings();
        for (String fault : runtimeFindings) {
            critical++;
            addAuditIssue(notes, "runtime: " + fault);
        }

        sender.sendMessage("== Observance Deep Hold V5 audit ==");
        sender.sendMessage(" manifest:    " + DeepHoldV5Manifest.contentHash()
                + " (+Z only, " + DeepHoldV5Manifest.ARTIFACTS.size() + " recovery contracts)");
        sender.sendMessage(" hold sites: " + placed + "/" + DeepHoldV4Plan.FIXTURES.size());
        sender.sendMessage(" gates:      " + gates + "/" + DEEP_HOLD_GATES.length + " (" + sealed + " sealed)");
        sender.sendMessage(" records:    " + recordStations + "/" + (DeepHoldV4Plan.RECORD_STATIONS.size() + 1)
                + " controlled mounts (locked absence valid)");
        sender.sendMessage(" region:     " + (regionLoc == null ? "missing" : "protected")
                + (entryRegion == null ? "" : " + entry stair"));
        sender.sendMessage(" route:      " + (mouth == null ? "missing Mouth" : "virtual-open full traversal"));
        sender.sendMessage(" critical:   " + critical);
        if (!notes.isEmpty()) {
            sender.sendMessage(" Findings:");
            for (String note : notes) sender.sendMessage("  - " + note);
            if (notes.size() >= 12) sender.sendMessage("  - ...showing first 12 findings only.");
        }
        sender.sendMessage(critical == 0
                ? " Deep Hold V5 is physically launch-placeable. Run /obs preflight for whole-plugin readiness."
                : " Fix findings, then rerun /obs placehold audit and /obs preflight.");
    }

    /** Exact built Hold origin used by the V5 installer/runtime lifecycle. */
    public Location v5HoldMouth() {
        return resolveDeepHoldV4Mouth();
    }

    /** Monotonic, local-primary gate and authored-book projection for the production runtime. */
    public void projectV5LocalState(
            com.observance.watcher.v5runtime.ProgressSnapshot snapshot) {
        if (snapshot == null || !Bukkit.isPrimaryThread()) return;
        Map<String, Object> facts = new LinkedHashMap<>();
        snapshot.booleans().forEach(facts::put);
        snapshot.branches().forEach(facts::put);
        snapshot.conductVerdict().ifPresent(
                value -> facts.put("v5_conduct_verdict", value.wireValue()));
        for (HoldGate gate : DEEP_HOLD_GATES) {
            List<String> required = DeepHoldV5Manifest.gateRequiredFlags(gate.id());
            if (!required.isEmpty() && required.stream().allMatch(snapshot::isComplete)) {
                // Projection is deliberately one-way: a false/missing mirror can never reseal a gate.
                applyHoldGateByName(null, gate.id(), false);
            }
        }
        syncV5Books(facts, false);
    }

    private Location resolveDeepHoldV4Mouth() {
        if (plugin.sites() == null) return null;
        // This region anchor kept the same offset across the sprawling and compact builders, so it
        // is the only safe way to recover the Mouth while sites.yml still contains pre-cutover
        // fixture coordinates. Falling straight through to a transformed fixture would subtract a
        // compact offset from a legacy location and silently move the inferred origin.
        Site entry = plugin.sites().get(HOLD_ENTRY_REGION_SITE_ID);
        Location entryLocation = entry == null ? null : entry.location();
        if (entryLocation != null && entryLocation.getWorld() != null) {
            return entryLocation.clone().add(0, 20, -52);
        }
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            Site site = plugin.sites().get(fixture.id());
            Location loc = site == null ? null : site.location();
            if (loc != null && loc.getWorld() != null) {
                return loc.clone().subtract(fixture.x(), fixture.y(), fixture.z());
            }
        }
        return null;
    }

    private String auditV4PrologueEcho(World world, int bx, int by, int bz) {
        Location lectern = new Location(world, bx - 14, by - 40, bz + 112);
        if (countFilledLecternsNear(lectern, 1) < 1) return "V5 covered-copy prologue lectern is missing or empty.";
        if (!hasSignNear(new Location(world, bx - 10, by - 40, bz + 112), 1)) {
            return "V5 covered-copy prologue sign is missing.";
        }
        return null;
    }

    private String auditV4RecordStation(World world, Location mouth,
                                        DeepHoldV4Plan.RecordStation station) {
        Location loc = mouth.clone().add(station.x(), station.y(), station.z());
        if (countFilledLecternsNear(loc, 1) < 1) return "V5 record " + station.id() + " is missing or empty.";
        if (!hasSignNear(loc, 6)) return "V5 record " + station.id() + " has no readable station sign.";
        return null;
    }

    private Location resolveDeepHoldOrigin() {
        if (plugin.sites() == null) return null;
        for (HoldSite row : DEEP_HOLD_SITES) {
            Site site = plugin.sites().get(row.id());
            Location loc = site == null ? null : site.location();
            if (loc != null && loc.getWorld() != null) {
                return loc.clone().subtract(row.x(), row.y(), row.z());
            }
        }
        return null;
    }

    private String auditHoldEntryRoute(Location origin, Site entryRegion) {
        if (origin == null || origin.getWorld() == null || entryRegion == null) {
            return "deep hold entry stair cannot be audited.";
        }
        Location entryLoc = entryRegion.location();
        if (entryLoc == null || entryLoc.getWorld() == null) {
            return "deep hold entry stair protection region is not placed.";
        }
        World world = origin.getWorld();
        if (entryLoc.getWorld() != world) {
            return "deep hold entry stair is in a different world from the Hold.";
        }

        int bx = origin.getBlockX();
        int by = origin.getBlockY();
        int bz = origin.getBlockZ();
        int sx = (entryLoc.getBlockX() * 2) - bx;
        int mouthZ = (entryLoc.getBlockZ() * 2) - bz;
        int surfaceY = (entryLoc.getBlockY() * 2) - by;
        int rampEndZ = Math.max(mouthZ + 96, bz - 186);
        int routeEndZ = bz;
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
        HoldRoomBox owner = null;
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            if (holdSiteInsideRoom(row, room)) {
                owner = room;
                break;
            }
        }
        Location origin = resolveDeepHoldOrigin();
        if (owner == null || origin == null || origin.getWorld() != world) return false;
        int bx = origin.getBlockX();
        int bz = origin.getBlockZ();
        int y = origin.getBlockY() + owner.ceilingY();
        int solid = 0;
        int holes = 0;
        for (int x = owner.minX() + 2; x <= owner.maxX() - 2; x += 6) {
            for (int z = owner.minZ() + 2; z <= owner.maxZ() - 2; z += 6) {
                Material material = world.getBlockAt(bx + x, y, bz + z).getType();
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
        int doorHeight = Math.min(span.height() - 3, Math.max(6, span.doorHalf() + 1));
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
        int doorHeight = Math.min(span.height() - 3, Math.max(6, span.doorHalf() + 1));
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
                for (int d = 0; d <= span.depth(); d++) {
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
        int minAcross = -span.halfAcross() - returnWidth - 1;
        int maxAcross = span.halfAcross() + returnWidth + 1;
        int startDepth = -5;
        int targetDepth = span.depth() + 5;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        for (int across = -span.doorHalf(); across <= span.doorHalf(); across++) {
            if (isHoldGateLocalStandable(world, cx, y, cz, span, across, 0, startDepth)) {
                queue.add(new int[]{across, 0, startDepth});
            }
        }
        while (!queue.isEmpty()) {
            int[] node = queue.removeFirst();
            int across = node[0], dy = node[1], depth = node[2];
            String key = across + ":" + dy + ":" + depth;
            if (!visited.add(key)) continue;
            if (depth >= targetDepth) return true;
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nextAcross = across + step[0];
                int nextDepth = depth + step[1];
                if (nextAcross < minAcross || nextAcross > maxAcross
                        || nextDepth < startDepth || nextDepth > targetDepth) continue;
                for (int ddy : new int[]{0, 1, -1}) {
                    int nextY = dy + ddy;
                    if (nextY < 0 || nextY >= span.height() - 1) continue;
                    String nextKey = nextAcross + ":" + nextY + ":" + nextDepth;
                    if (!visited.contains(nextKey) && isHoldGateLocalStandable(
                            world, cx, y, cz, span, nextAcross, nextY, nextDepth)) {
                        queue.addLast(new int[]{nextAcross, nextY, nextDepth});
                        break;
                    }
                }
            }
        }
        return false;
    }

    private boolean isHoldGateLocalStandable(World world, int cx, int y, int cz, HoldGateSpan span,
                                              int across, int dy, int depth) {
        Block floor = holdGateBlockAt(world, cx, y, cz, span, across, dy - 1, depth);
        Block feet = holdGateBlockAt(world, cx, y, cz, span, across, dy, depth);
        Block head = holdGateBlockAt(world, cx, y, cz, span, across, dy + 1, depth);
        return floor.getType().isSolid() && isHoldBodyClear(feet) && isHoldBodyClear(head);
    }

    private String auditHoldCivicShell(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold civic shell has no world loaded.";
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            int floor = by + room.floorY();
            int walkable = 0;
            for (int x = room.minX() + 4; x <= room.maxX() - 4; x += 6) {
                for (int z = room.minZ() + 4; z <= room.maxZ() - 4; z += 6) {
                    Block floorBlock = world.getBlockAt(bx + x, floor - 1, bz + z);
                    Block feet = world.getBlockAt(bx + x, floor, bz + z);
                    Block head = world.getBlockAt(bx + x, floor + 1, bz + z);
                    if (floorBlock.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) {
                        walkable++;
                    }
                }
            }
            if (walkable < 6) {
                return "owned room " + room.id() + " has too little Adventure-mode floor clearance ("
                        + walkable + " samples).";
            }
        }
        return null;
    }

    private Set<HoldWalkNode> collectHoldAdventureWalk(World world, int bx, int by, int bz) {
        Set<HoldWalkNode> visited = new HashSet<>();
        if (world == null) return visited;
        ArrayDeque<HoldWalkNode> queue = new ArrayDeque<>();
        HoldWalkNode seed = null;
        for (int z = 0; z <= 8 && seed == null; z++) {
            for (int y = by + 2; y >= by - 5 && seed == null; y--) {
                if (isHoldStandable(world, bx, y, bz + z)) seed = new HoldWalkNode(bx, y, bz + z);
            }
        }
        if (seed == null) return visited;
        queue.add(seed);
        final int minX = bx - 124;
        final int maxX = bx + 124;
        final int minY = by - 36;
        final int maxY = by + 4;
        final int minZ = bz;
        final int maxZ = bz + 790;
        final int hardLimit = 750_000;
        while (!queue.isEmpty()) {
            HoldWalkNode node = queue.removeFirst();
            if (!visited.add(node)) continue;
            if (visited.size() > hardLimit) {
                throw new IllegalStateException("Deep Hold walk graph exceeded " + hardLimit
                        + " nodes; an owned shell is open to the world.");
            }
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = node.x() + step[0];
                int nz = node.z() + step[1];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                HoldWalkNode next = null;
                for (int dy : new int[]{0, 1, -1}) {
                    int ny = node.y() + dy;
                    if (ny < minY || ny > maxY) continue;
                    HoldWalkNode candidate = new HoldWalkNode(nx, ny, nz);
                    if (!visited.contains(candidate) && isHoldStandable(world, nx, ny, nz)) {
                        next = candidate;
                        break;
                    }
                }
                if (next != null) queue.addLast(next);
            }
        }
        return visited;
    }

    private boolean isHoldStandable(World world, int x, int y, int z) {
        if (world == null) return false;
        Block floor = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        return floor.getType().isSolid() && isHoldBodyClear(feet) && isHoldBodyClear(head);
    }

    private boolean isHoldBodyClear(Block block) {
        if (block == null || block.isLiquid()) return false;
        Material material = block.getType();
        return material.isAir() || block.isPassable();
    }

    private String auditHoldAdventureReachability(World world, int bx, int by, int bz,
                                                   Set<HoldWalkNode> visited) {
        if (world == null || visited == null || visited.isEmpty()) {
            return "Deep Hold mouth has no Adventure-mode walk seed.";
        }
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            int floor = by + room.floorY();
            boolean reached = false;
            for (HoldWalkNode node : visited) {
                if (node.y() == floor
                        && node.x() >= bx + room.minX() + 3 && node.x() <= bx + room.maxX() - 3
                        && node.z() >= bz + room.minZ() + 3 && node.z() <= bz + room.maxZ() - 3) {
                    reached = true;
                    break;
                }
            }
            if (!reached) return "owned room " + room.id() + " is not reachable from the mouth in Adventure mode.";
        }
        for (HoldSite site : DEEP_HOLD_SITES) {
            int halfX = site.halfX() > 0 ? site.halfX() : Math.max(4, Math.min(10, site.radius()));
            int halfZ = site.halfZ() > 0 ? site.halfZ() : Math.max(4, Math.min(10, site.radius()));
            if (!hasHoldReachableNode(visited, bx + site.x(), by + site.y(), bz + site.z(),
                    halfX + 4, halfZ + 4, 6)) {
                return "Hold fixture " + site.id() + " has no mouth-reachable Adventure-mode approach.";
            }
        }
        for (HoldRecordStation station : DEEP_HOLD_RECORD_STATIONS) {
            if (!hasHoldReachableNode(visited, bx + station.x(), by + station.y(), bz + station.z(),
                    6, 6, 2)) {
                return "Hold record station " + station.id() + " has no mouth-reachable approach.";
            }
        }
        return null;
    }

    private boolean hasHoldReachableNode(Set<HoldWalkNode> visited, int x, int y, int z,
                                         int halfX, int halfZ, int halfY) {
        if (visited == null || visited.isEmpty()) return false;
        for (HoldWalkNode node : visited) {
            if (Math.abs(node.x() - x) <= Math.max(1, halfX)
                    && Math.abs(node.z() - z) <= Math.max(1, halfZ)
                    && Math.abs(node.y() - y) <= Math.max(1, halfY)) return true;
        }
        return false;
    }

    private String auditHoldSigns(World world, int bx, int by, int bz, Set<HoldWalkNode> visited) {
        if (world == null) return "Deep Hold signs have no world loaded.";
        Set<HoldWalkNode> seen = new HashSet<>();
        int signs = 0;
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            for (int x = room.minX(); x <= room.maxX(); x++) {
                for (int z = room.minZ(); z <= room.maxZ(); z++) {
                    for (int y = room.floorY(); y < room.ceilingY(); y++) {
                        Block block = world.getBlockAt(bx + x, by + y, bz + z);
                        if (!(block.getState() instanceof Sign)) continue;
                        HoldWalkNode key = new HoldWalkNode(block.getX(), block.getY(), block.getZ());
                        if (!seen.add(key)) continue;
                        signs++;
                        String issue = auditHoldReadableSign(block, visited);
                        if (issue != null) return issue;
                    }
                }
            }
        }
        for (int x = bx - 8; x <= bx + 8; x++) {
            for (int z = bz; z <= bz + 86; z++) {
                for (int y = by - 30; y <= by + 4; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!(block.getState() instanceof Sign)) continue;
                    HoldWalkNode key = new HoldWalkNode(x, y, z);
                    if (!seen.add(key)) continue;
                    signs++;
                    String issue = auditHoldReadableSign(block, visited);
                    if (issue != null) return issue;
                }
            }
        }
        if (signs < DEEP_HOLD_RECORD_STATIONS.length + 5) {
            return "Deep Hold sign audit found only " + signs + " authored signs.";
        }
        return null;
    }

    private String auditHoldReadableSign(Block block, Set<HoldWalkNode> visited) {
        if (block == null || !(block.getState() instanceof Sign sign)) return "Hold sign is missing.";
        boolean hasText = false;
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            if (!PlainTextComponentSerializer.plainText().serialize(line).isBlank()) {
                hasText = true;
                break;
            }
        }
        if (!hasText) {
            boolean editable = false;
            try {
                editable = !sign.isWaxed();
            } catch (Throwable ignored) { }
            if (!editable) return "Hold sign at " + block.getX() + "," + block.getY() + "," + block.getZ()
                    + " is blank and was not authored as an editable filing slit.";
        }
        if (block.getType().name().endsWith("_WALL_SIGN")) {
            if (!(block.getBlockData() instanceof Directional directional)) {
                return "Hold wall sign has no facing data at " + block.getX() + "," + block.getY() + "," + block.getZ() + ".";
            }
            BlockFace facing = directional.getFacing();
            if (!block.getRelative(facing.getOppositeFace()).getType().isSolid()) {
                return "Hold wall sign has no solid backing at " + block.getX() + "," + block.getY() + "," + block.getZ() + ".";
            }
            int fx = block.getX() + facing.getModX() * 2;
            int fz = block.getZ() + facing.getModZ() * 2;
            if (!hasHoldReachableNode(visited, fx, block.getY() - 1, fz, 2, 2, 2)) {
                return "Hold wall sign faces no reachable reading position at " + block.getX() + ","
                        + block.getY() + "," + block.getZ() + ".";
            }
        } else {
            if (!block.getRelative(BlockFace.DOWN).getType().isSolid()) {
                return "Hold standing sign has no solid floor at " + block.getX() + "," + block.getY() + "," + block.getZ() + ".";
            }
            if (!hasHoldReachableNode(visited, block.getX(), block.getY(), block.getZ(), 3, 3, 2)) {
                return "Hold standing sign has no reachable reading position at " + block.getX() + ","
                        + block.getY() + "," + block.getZ() + ".";
            }
        }
        return null;
    }

    private String auditHoldGateLabel(HoldGate gate, Location loc) {
        if (gate == null || loc == null || loc.getWorld() == null) return "Hold gate label cannot be audited.";
        HoldGateSpan span = holdGateSpan(gate);
        Block label = holdGateLabelLocation(loc, span).getBlock();
        if (!(label.getState() instanceof Sign sign)) return "gate " + gate.id() + " label is missing.";
        boolean named = false;
        for (Component line : sign.getSide(Side.FRONT).lines()) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            if (text.toLowerCase(Locale.ROOT).contains(gate.label().toLowerCase(Locale.ROOT))) {
                named = true;
                break;
            }
        }
        if (!named) return "gate " + gate.id() + " label text is wrong.";
        if (!(label.getBlockData() instanceof Rotatable rotatable)
                || rotatable.getRotation() != holdGateLabelFacing(span)) {
            return "gate " + gate.id() + " label has the wrong player-facing rotation.";
        }
        if (!label.getRelative(BlockFace.DOWN).getType().isSolid()) {
            return "gate " + gate.id() + " label has no approach floor support.";
        }
        return null;
    }

    private String auditHoldEarlyRoute(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold early route has no world loaded.";
        for (int z = 0; z <= 84; z += 6) {
            int floor = by - Math.min(24, z / 3);
            if (!hasHoldGroupWalkableSlice(world, bx, floor, bz + z, 5, 9)) {
                return "Deep Hold mouth/descent is blocked near local Z " + z + ".";
            }
        }
        for (int z : new int[]{98, 108, 120, 132, 136}) {
            if (!hasHoldGroupWalkableSlice(world, bx, by - 24, bz + z, 5, 7)) {
                return "Orientation center aisle is blocked near local Z " + z + ".";
            }
        }
        for (int x : new int[]{-20, -16, 0, 16, 20}) {
            if (!hasHoldGroupWalkableSliceZ(world, bx + x, by - 24, bz + 112, 3, 5)) {
                return "Orientation cross-alcove doorway is blocked near local X " + x + ".";
            }
        }
        for (int z : new int[]{152, 168, 188, 206, 226, 246, 264}) {
            if (!hasHoldGroupWalkableSlice(world, bx, by - 28, bz + z, 5, 7)) {
                return "Keeper Court center aisle is blocked near local Z " + z + ".";
            }
        }
        for (int z : new int[]{278, 298, 318, 338, 364, 390, 414}) {
            if (!hasHoldGroupWalkableSlice(world, bx, by - 24, bz + z, 5, 7)) {
                return "Archive center aisle is blocked near local Z " + z + ".";
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

    private boolean hasHoldGroupWalkableSliceZ(World world, int x, int y, int centerZ,
                                                int halfWidth, int required) {
        if (world == null) return false;
        int walkable = 0;
        for (int dz = -Math.max(1, halfWidth); dz <= Math.max(1, halfWidth); dz++) {
            Block floor = world.getBlockAt(x, y - 1, centerZ + dz);
            Block feet = world.getBlockAt(x, y, centerZ + dz);
            Block head = world.getBlockAt(x, y + 1, centerZ + dz);
            if (floor.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()) walkable++;
        }
        return walkable >= Math.max(1, required);
    }

    private String auditHoldDistrictEnclosure(World world, int bx, int by, int bz) {
        if (world == null) return "deep hold enclosure has no world loaded.";
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            int floor = by + room.floorY();
            int ceiling = by + room.ceilingY();
            int[] xs = {room.minX() + 1, room.maxX() - 1};
            int[] zs = {room.minZ() + 1, room.maxZ() - 1};
            for (int x : xs) {
                for (int z : zs) {
                    Material wall = world.getBlockAt(bx + x, floor + 3, bz + z).getType();
                    Material roof = world.getBlockAt(bx + x, ceiling + 1, bz + z).getType();
                    if (wall.isAir() || wall == Material.WATER || wall == Material.LAVA
                            || roof.isAir() || roof == Material.WATER || roof == Material.LAVA) {
                        return "owned room " + room.id() + " has an open wall/roof corner near "
                                + (bx + x) + "," + (floor + 3) + "," + (bz + z) + ".";
                    }
                }
            }
        }
        return null;
    }

    private String auditHoldPrologueEcho(World world, int bx, int by, int bz) {
        if (world == null) return "covered copy record has no world loaded.";
        String lecternIssue = auditWrittenLecternAt(world.getBlockAt(bx - 10, by - 24, bz + 76),
                "covered copy", "real first report belongs above", "covered copy");
        if (lecternIssue != null) return lecternIssue;
        if (!isOccupiedChiseledShelf(world.getBlockAt(bx - 12, by - 24, bz + 76))) {
            return "covered copy shelf is missing or empty.";
        }
        if (!isStandingSignAt(world.getBlockAt(bx - 6, by - 24, bz + 80))) {
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
                String pages = meta.pages().stream()
                        .map(PlainTextComponentSerializer.plainText()::serialize)
                        .collect(java.util.stream.Collectors.joining("\n"));
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

    private int sampleUnexpectedHoldFluids(Location origin) {
        if (origin == null || origin.getWorld() == null) return 0;
        World world = origin.getWorld();
        int bx = origin.getBlockX();
        int by = origin.getBlockY();
        int bz = origin.getBlockZ();
        int found = 0;
        for (HoldRoomBox room : DEEP_HOLD_ROOM_BOXES) {
            boolean allowsWater = room.id().contains("sella") || room.id().contains("cistern")
                    || room.id().contains("water");
            int floor = by + room.floorY();
            int ceiling = by + room.ceilingY();
            for (int x = room.minX() + 3; x <= room.maxX() - 3; x += 4) {
                for (int z = room.minZ() + 3; z <= room.maxZ() - 3; z += 4) {
                    for (int y = floor; y < ceiling; y += 3) {
                        Material material = world.getBlockAt(bx + x, y, bz + z).getType();
                        if (material == Material.LAVA || (material == Material.WATER && !allowsWater)) {
                            found++;
                            if (found >= 20) return found;
                        }
                    }
                }
            }
        }
        return found;
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
        handleFinaleMarkersV5(sender);

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
        handleFinaleMarkersV5(sender);

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
        sender.sendMessage("  Optional Nether/End lanes: stand at a safe anchor in that dimension and run /obs placeworld.");
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
                meta.addPages(Component.text(text));
            } else {
                for (String page : com.observance.watcher.util.TextFit.paginate(text)) {
                    meta.addPages(Component.text(page));
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
                    meta.addPages(Component.text(body));
                } else {
                    for (String real : com.observance.watcher.util.TextFit.paginate(body)) {
                        meta.addPages(Component.text(real));
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
        placeDecorativeBookshelf(block, seed, BlockFace.SOUTH);
    }

    private void placeDecorativeBookshelf(Block block, int seed, BlockFace facing) {
        if (block == null) return;
        try {
            block.setType(Material.CHISELED_BOOKSHELF, false);
            int[] occupied = new int[3];
            if (block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf) {
                shelf.setFacing(facing == null ? BlockFace.SOUTH : facing);
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
        placeMechanicBookshelf(block, BlockFace.SOUTH);
    }

    private void placeMechanicBookshelf(Block block, BlockFace facing) {
        if (block == null) return;
        try {
            block.setType(Material.CHISELED_BOOKSHELF, false);
            if (block.getBlockData() instanceof org.bukkit.block.data.type.ChiseledBookshelf shelf) {
                shelf.setFacing(facing == null ? BlockFace.SOUTH : facing);
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

    private void placeWallMountedSign(Location loc, BlockFace facing, String[] lines) {
        if (loc == null || loc.getWorld() == null) return;
        BlockFace front = cardinalOnly(facing == null ? BlockFace.NORTH : facing);
        Block support = loc.getBlock().getRelative(front.getOppositeFace());
        if (!support.getType().isSolid()) {
            throw new IllegalStateException("Wall sign at " + loc.getBlockX() + ","
                    + loc.getBlockY() + "," + loc.getBlockZ() + " has no solid backing block");
        }
        Block block = loc.getBlock();
        block.setType(Material.SPRUCE_WALL_SIGN, false);
        if (block.getBlockData() instanceof Directional directional) {
            directional.setFacing(front);
            block.setBlockData(directional, false);
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

    private static boolean keeperInvestigationBegun(Map<String, Object> flags) {
        if (!directorFlag(flags, "rosetta_known")) return false;
        for (String key : new String[]{"vaun_cache_open", "mara_read", "mara_alcove_open",
                "sella_bearing_read", "orin_bowed", "brann_toll_heard", "iss_key_turned", "iss_trusted"}) {
            if (directorFlag(flags, key)) return true;
        }
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
        sender.sendMessage("  test: enter through unlit_entry, prove the required house findings, then return by exit and shard");
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
        sender.sendMessage("  3) Record every placement and proof in design/V5-LIVE-TEST-MATRIX.csv.");
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
        sender.sendMessage("  Nether lane: stand at the safe Nether anchor, then /obs placeworld.");
        sender.sendMessage("  End lane: stand at the safe End anchor, then /obs placeworld.");
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
        sender.sendMessage("  7) Rehearse the real route: enter normally, spend a borrowed lantern, prove every required house finding, and return by both exit and shard.");
        sender.sendMessage("  8) Restart once during an expedition and confirm the player returns with the exact inventory they carried in.");
        sender.sendMessage("  9) Handoff check: /obs unlit ready, /obs preflight, then fill the V5 live-test receipt rows.");
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
        // A geometry-complete fixture pass is intentionally replayable after interruption. Avoid
        // asking Paper to replace an already-correct lectern: 1.21.11 runs lectern block-entity
        // removal hooks even for some same-cell transitions, and a prior partial pass can otherwise
        // leave its HAS_BOOK property paired with the replacement state. When replacing another
        // inventory block on a genuinely fresh cell, empty and persist it before changing type.
        if (b.getType() != Material.LECTERN) {
            org.bukkit.block.BlockState previous = b.getState();
            if (previous instanceof InventoryHolder holder) {
                holder.getInventory().clear();
                previous.update(true, false);
            }
            b.setType(Material.LECTERN, false);
        }
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
                    meta.addPages(Component.text("page " + i + "\n\n" + title + "\n\nturn me for page-lock testing."));
                }
                book.setItemMeta(meta);
            }
            writeLecternBook(b, lectern, book);
        }
    }

    private void fillMaraLockBook(Block b, int index, int markedPage) {
        fillV5AuthorityBook(b, "mara_manual_edition_" + index);
    }

    private void fillSellaLockBook(Block b, int index, int markedPage) {
        String bookId = switch (index) {
            case 1 -> "sella_shore_copybook";
            case 4 -> "sella_sample_note";
            default -> null;
        };
        if (bookId == null) {
            if (b != null && b.getState() instanceof Lectern lectern) lectern.getInventory().clear();
            return;
        }
        fillV5AuthorityBook(b, bookId);
    }

    private void fillV5AuthorityBook(Block block, String bookId) {
        if (block == null || block.getType() != Material.LECTERN
                || !(block.getState() instanceof Lectern lectern)) {
            throw new IllegalStateException("V5 book " + bookId + " has no lectern holder");
        }
        V5AuthorityManifest.BookEntry payload = V5AuthorityManifest.book(bookId);
        if (payload == null) throw new IllegalStateException("Missing packaged V5 book " + bookId);
        writeLecternBook(block, lectern, createV5Book(payload));
    }

    private ItemStack createV5Book(V5AuthorityManifest.BookEntry payload) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (!(book.getItemMeta() instanceof BookMeta meta)) {
            throw new IllegalStateException("Paper did not expose written-book metadata");
        }
        meta.setTitle(payload.title());
        meta.setAuthor(payload.author());
        for (String page : payload.pages()) meta.addPages(Component.text(page));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "book_id"),
                org.bukkit.persistence.PersistentDataType.STRING, payload.id());
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "story_version"),
                org.bukkit.persistence.PersistentDataType.STRING, "5.0.0");
        book.setItemMeta(meta);
        return book;
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
                        meta.addPages(Component.text(body));
                    } else {
                        for (String real : com.observance.watcher.util.TextFit.paginate(body)) {
                            meta.addPages(Component.text(real));
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
        // Same containment contract as buildHoldWaterMirrorCore: the KS01 waterline is a rimmed
        // six-cell trough at foot level; nothing outside the rim ever holds spreadable water.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Material floor = (Math.abs(dx) >= 3 || Math.abs(dz) == 3)
                        ? Material.DARK_PRISMARINE : Material.PRISMARINE;
                world.getBlockAt(x + dx, y - 1, z + dz).setType(floor, false);
                world.getBlockAt(x + dx, y, z + dz).setType(Material.AIR, false);
            }
        }
        for (int dx = -4; dx <= 3; dx++) {
            world.getBlockAt(x + dx, y, z - 1).setType(Material.DARK_PRISMARINE, false);
            world.getBlockAt(x + dx, y, z + 1).setType(Material.DARK_PRISMARINE, false);
        }
        world.getBlockAt(x - 4, y, z).setType(Material.DARK_PRISMARINE, false);
        world.getBlockAt(x + 3, y, z).setType(Material.DARK_PRISMARINE, false);
        for (int dx = -3; dx <= 2; dx++) {
            world.getBlockAt(x + dx, y - 1, z).setType(Material.PRISMARINE_BRICKS, false);
            world.getBlockAt(x + dx, y, z).setType(Material.WATER, false);
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
        frame.setFacingDirection(BlockFace.NORTH, true);
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
                boolean curb = !still && Math.abs(dx) <= 4 && dz >= -3 && dz <= 3;
                world.getBlockAt(bx + dx, by - 1, bz + dz).setType(still ? Material.DARK_PRISMARINE
                        : (shore ? Material.POLISHED_BLACKSTONE_BRICKS : Material.PRISMARINE_BRICKS), false);
                if (still) {
                    // Foot-level still water held behind a one-block curb: the anchor cell stays
                    // physical for site audits and the shoreline floor stays dry.
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.WATER, false);
                } else if (curb) {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.PRISMARINE_BRICKS, false);
                } else {
                    world.getBlockAt(bx + dx, by, bz + dz).setType(Material.AIR, false);
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
                    front.line(i, Component.text(com.observance.watcher.util.TextFit.clampLine(line,
                            com.observance.watcher.util.TextFit.SIGN_LINE_CHARS)));
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
    /** Build the one surface callback puzzle that hands players from Minecraft to Discord.
     * Stand at the intended SOUTH approach; the structure is placed immediately to the north and
     * the player remains outside. The recovered callback is entered on Copperline ticket 1842. */
    private void handlePlaceRelay(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placerelay must be run by a player at the intended approach.");
            return;
        }
        World world = player.getWorld();
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ() - 10;

        // Seat the whole footprint above its highest terrain cell so no hill seals a wall or reading lane.
        int centerY = world.getMinHeight() + 1;
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -8; dz <= 10; dz++) {
                centerY = Math.max(centerY, world.getHighestBlockYAt(centerX + dx, centerZ + dz,
                        org.bukkit.HeightMap.MOTION_BLOCKING) + 1);
            }
        }
        if (centerY + 8 >= world.getMaxHeight()) {
            sender.sendMessage("Observance: relay footprint is too close to the build ceiling; choose lower ground.");
            return;
        }

        Location roomCenter = new Location(world, centerX, centerY, centerZ);
        Location procedure = StructureTemplates.discordRelay(roomCenter);
        if (procedure == null) {
            sender.sendMessage("Observance: relay build failed; keep the target chunks loaded and try again.");
            return;
        }
        Site relay = new Site("discord_relay", "discord_relay", world.getName(),
                procedure.getX(), procedure.getY(), procedure.getZ(), 12, 8, true, true, null);
        plugin.registerRuntimeSite(relay);
        sender.sendMessage("Observance: Copperline field relay built ahead of you, entrance facing SOUTH.");
        sender.sendMessage("  Players recover callback 9137 and enter it on archived support ticket 1842.");
        sender.sendMessage("  Site persisted as discord_relay. Run /obs audit and inspect all five books in Adventure mode.");
    }

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
                V5DialogueCatalog.Npc npc = V5DialogueCatalog.wren();
                Site anchor = new Site(npc.anchorSite(), "npc_anchor", loc.getWorld().getName(),
                        loc.getX(), loc.getY(), loc.getZ(), 2, 3, true, true, null, false);
                if (!plugin.registerRuntimeSite(anchor)) {
                    sender.sendMessage("Observance: Wren anchor persistence failed; he was not moved.");
                    return;
                }
                var body = wren.spawn(loc);
                if (body == null) {
                    sender.sendMessage("Observance: could not spawn Wren here (world/chunk unavailable?).");
                    return;
                }
                sender.sendMessage("Observance: Wren is anchored at " + npc.anchorSite() + " ("
                        + wren.backend() + "). Right-click him to hear exact V5 dialogue.");
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

    /** Explicitly armed local V5 finale. Marker placement remains available as the `markers` subcommand. */
    private void handleFinale(CommandSender sender, String[] args) {
        String op = args.length >= 2 ? args[1].trim().toLowerCase(Locale.ROOT) : "status";
        if ("markers".equals(op)) {
            handleFinaleMarkersV5(sender);
            return;
        }
        var runtime = plugin.v5Runtime();
        if (runtime == null) {
            sender.sendMessage("Observance: V5 local finale runtime is unavailable; no state changed.");
            return;
        }
        runtime.handleFinaleCommand(sender, args);
    }

    /** Retained as unreachable migration reference; production dispatch never invokes it. */
    @SuppressWarnings("unused")
    private void legacyHandleFinale(CommandSender sender, String[] args) {
        String op = args.length >= 2 ? args[1].trim().toLowerCase(Locale.ROOT) : "status";
        if ("markers".equals(op)) {
            handleFinaleMarkersV5(sender);
            return;
        }
        var runtime = plugin.v5Runtime();
        if (runtime != null) {
            runtime.handleFinaleCommand(sender, args);
            return;
        }
        var controller = plugin.finaleController();
        if (controller == null) {
            sender.sendMessage("Observance: local finale controller is unavailable; no ending was armed.");
            return;
        }
        switch (op) {
            case "status" -> sender.sendMessage("Observance finale: " + controller.status());
            case "cancel" -> sender.sendMessage("Observance: " + controller.cancel().message());
            case "markers" -> handleFinaleMarkersV5(sender);
            case "arm" -> {
                long seconds = args.length >= 3 ? parseSmallInt(args[2], 120) : 120L;
                if (seconds < 15L || seconds > 600L) {
                    sender.sendMessage("Usage: /observance finale arm [15-600 seconds]");
                    sender.sendMessage("The command never accepts a story branch; it reads the players' durable choices.");
                    return;
                }
                Site release = plugin.sites() == null ? null : plugin.sites().get("release_record");
                Location control = release == null ? null : release.location();
                if (control == null || !hasV5SeverControlNear(control)) {
                    sender.sendMessage("Observance: finale arm refused. The canonical release_record "
                            + "SEVER RECORD control is absent or unverified; run /obs finale markers.");
                    return;
                }
                List<String> infrastructure = auditFinaleArmInfrastructure();
                if (!infrastructure.isEmpty()) {
                    sender.sendMessage("Observance: finale arm refused; Hold finale infrastructure failed: "
                            + String.join("; ", infrastructure));
                    return;
                }
                var sb = plugin.supabase();
                if (sb == null || !sb.isConfigured()) {
                    sender.sendMessage("Observance: finale arm refused. Durable V5 progression is unavailable.");
                    return;
                }
                String actor = sender.getName();
                sender.sendMessage("Observance: validating C10 receipts and the players' recorded choices...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    var result = sb.fetchArcState();
                    Map<String, Object> flags = result.ok() && result.value() != null
                            ? result.value().flagsMap() : Map.of();
                    List<String> missing = new ArrayList<>();
                    for (String required : List.of("v5_case_c08_complete", "v5_case_c09_complete",
                            "v5_rp01_instruction", "v5_rp02_configured", "v5_rp03_name_choice",
                            "v5_rp04_collective")) {
                        if (!directorFlag(flags, required)) missing.add(required);
                    }
                    FinaleStateMachine.WrenOutcome wren = FinaleStateMachine.parseWrenOutcome(
                            flagText(flags.get("v5_wren_outcome")));
                    FinaleStateMachine.NameTreatment name = FinaleStateMachine.parseNameTreatment(
                            flagText(flags.get("v5_name_treatment")));
                    FinaleStateMachine.ConductVerdict conduct = FinaleStateMachine.parseConductVerdict(
                            flagText(flags.get("v5_conduct_verdict")));
                    if (wren == null) missing.add("v5_wren_outcome");
                    if (name == null) missing.add("v5_name_treatment");
                    if (conduct == null) missing.add("v5_conduct_verdict");
                    List<String> legacyWren = new ArrayList<>();
                    for (String choice : List.of("condemn", "understand", "free")) {
                        if (directorFlag(flags, "v5_wren_choice_" + choice)) legacyWren.add(choice);
                    }
                    if (!legacyWren.isEmpty() && (legacyWren.size() != 1 || wren == null
                            || !legacyWren.get(0).equals(wren.name().toLowerCase(Locale.ROOT)))) {
                        missing.add("v5_wren_outcome/choice mismatch");
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!missing.isEmpty()) {
                            sender.sendMessage("Observance: finale arm refused; missing/invalid: "
                                    + String.join(", ", missing) + ". No local state changed.");
                            return;
                        }
                        var armed = controller.arm(wren, name, conduct, actor, seconds);
                        sender.sendMessage("Observance: " + armed.message());
                        if (armed.ok()) sender.sendMessage("  The player choices are copied to local schema-2 "
                                + "state. Only the protected player sever control can commit theater.");
                    });
                });
            }
            default -> sender.sendMessage("Usage: /observance finale <arm [15-600 seconds]|cancel|status|markers>");
        }
    }

    private static String flagText(Object value) {
        return value == null ? null : value.toString().trim();
    }

    /** Exact V5 all-of gate predicate; unknown/missing contracts fail closed, physical opens latch. */
    private boolean v5GateOpen(Map<String, Object> flags, String gateId) {
        if (holdGateLatchedOpen(gateId)) return true;
        List<String> required = DeepHoldV5Manifest.gateRequiredFlags(gateId);
        return !required.isEmpty() && required.stream().allMatch(flag -> directorFlag(flags, flag));
    }

    private record BookSyncResult(int resolved, int written, int cleared, List<String> issues) { }

    /** Terminal local world state: every gate open and exactly one branch Coda receipt mounted. */
    public List<String> applyFinaleCodaWorldState(FinaleStateMachine.Snapshot state) {
        boolean publish = state != null
                && state.nameTreatment() == FinaleStateMachine.NameTreatment.PUBLISH;
        return applyFinaleCodaWorldState(publish);
    }

    /** Terminal V5 world projection from the locally committed ritual vocabulary. */
    public List<String> applyV5CodaWorldState(
            com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment treatment) {
        return applyFinaleCodaWorldState(
                treatment == com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment.PUBLISH);
    }

    private List<String> applyFinaleCodaWorldState(boolean publish) {
        List<String> issues = new ArrayList<>();
        if (!Bukkit.isPrimaryThread()) return List.of("Coda world state was requested off the main thread");
        Location mouth = resolveDeepHoldV4Mouth();
        if (mouth == null || mouth.getWorld() == null) return List.of("Coda cannot resolve the built Hold Mouth");
        for (HoldGate gate : DEEP_HOLD_GATES) {
            Location gateAt = mouth.clone().add(gate.x(), gate.y(), gate.z());
            setHoldGate(gate, gateAt, false);
            placeHoldGateLabel(gate, gateAt);
            String problem = auditHoldGateIntegrity(gate, gateAt);
            if (problem != null) issues.add(problem);
        }
        String bookId = publish
                ? "coda_receipt_publish" : "coda_receipt_unfiled";
        V5AuthorityManifest.BookPlacement placement = V5AuthorityManifest.bookPlacements().stream()
                .filter(row -> row.bookId().equals(bookId)).findFirst().orElse(null);
        Block mount = placement == null ? null : resolveV5BookLectern(placement, mouth, new HashSet<>());
        if (mount == null || mount.getType() != Material.LECTERN
                || !(mount.getState() instanceof Lectern lectern)) {
            issues.add("Coda receipt mount is missing for " + bookId);
        } else {
            writeLecternBook(mount, lectern, createV5Book(
                    java.util.Objects.requireNonNull(V5AuthorityManifest.book(bookId))));
            String problem = auditV5BookItem(lectern.getInventory().getItem(0), bookId);
            if (problem != null) issues.add("Coda receipt " + problem);
        }
        return List.copyOf(issues);
    }

    private List<String> auditFinaleArmInfrastructure() {
        List<String> issues = new ArrayList<>();
        Location mouth = resolveDeepHoldV4Mouth();
        if (mouth == null || mouth.getWorld() == null) return List.of("built Hold Mouth is unavailable");
        for (HoldGate gate : DEEP_HOLD_GATES) {
            Location gateAt = mouth.clone().add(gate.x(), gate.y(), gate.z());
            String problem = auditHoldGateIntegrity(gate, gateAt);
            if (problem != null) issues.add(problem);
        }
        V5AuthorityManifest.BookPlacement coda = V5AuthorityManifest.bookPlacements().stream()
                .filter(row -> row.bookId().equals("coda_receipt_publish")).findFirst().orElse(null);
        Block mount = coda == null ? null : resolveV5BookLectern(coda, mouth, new HashSet<>());
        if (mount == null || mount.getType() != Material.LECTERN
                || !(mount.getState() instanceof Lectern)) issues.add("release_record Coda lectern is missing");
        return List.copyOf(issues);
    }

    /** Unlock-aware exact-book reconciliation. It only mutates authored lecterns, never geometry. */
    private BookSyncResult syncV5Books(Map<String, Object> flags) {
        return syncV5Books(flags, true);
    }

    /**
     * Runtime projection never clears an already revealed authored book. This makes remote outage or
     * a stale false value incapable of revoking a locally earned record.
     */
    private BookSyncResult syncV5Books(Map<String, Object> flags, boolean clearUnavailable) {
        Location mouth = resolveDeepHoldV4Mouth();
        Map<String, Object> safeFlags = flags == null ? Map.of() : flags;
        Set<String> claimed = new HashSet<>();
        List<String> issues = new ArrayList<>();
        int resolved = 0;
        int written = 0;
        int cleared = 0;
        for (V5AuthorityManifest.BookPlacement placement : V5AuthorityManifest.bookPlacements()) {
            if ("earned_artifact".equals(placement.holderKind())) continue;
            Block lectern = resolveV5BookLectern(placement, mouth, claimed);
            if (lectern == null || lectern.getType() != Material.LECTERN
                    || !(lectern.getState() instanceof Lectern state)) {
                issues.add(placement.bookId() + " has no exact lectern at " + placement.holderId()
                        + "/" + placement.mount());
                continue;
            }
            resolved++;
            ItemStack current = state.getInventory().getItem(0);
            boolean available = directorFlag(safeFlags, placement.availabilityFlag());
            if (available) {
                String problem = auditV5BookItem(current, placement.bookId());
                if (problem != null) {
                    writeLecternBook(lectern, state, createV5Book(
                            java.util.Objects.requireNonNull(V5AuthorityManifest.book(placement.bookId()))));
                    written++;
                }
            } else if (clearUnavailable && current != null && current.getType() != Material.AIR) {
                String currentId = currentV5BookId(current);
                if (currentId == null || currentId.equals(placement.bookId())) {
                    state.getInventory().clear();
                    state.update(true, false);
                    cleared++;
                }
            }
        }
        cleared += reconcileV5BookCopies(mouth, safeFlags, clearUnavailable);
        return new BookSyncResult(resolved, written, cleared, List.copyOf(issues));
    }

    private int reconcileV5BookCopies(Location mouth, Map<String, Object> flags,
                                      boolean clearUnavailable) {
        if (mouth == null || mouth.getWorld() == null) return 0;
        Map<String, V5AuthorityManifest.BookPlacement> placements = V5AuthorityManifest.bookPlacements()
                .stream().collect(java.util.stream.Collectors.toMap(
                        V5AuthorityManifest.BookPlacement::bookId, placement -> placement, (a, b) -> a));
        int minX = mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE;
        int maxX = mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE;
        int minY = mouth.getBlockY() + DeepHoldV4Plan.MIN_Y - DeepHoldV4Plan.ENVELOPE;
        int maxY = mouth.getBlockY() + DeepHoldV4Plan.MAX_Y + DeepHoldV4Plan.ENVELOPE;
        int minZ = mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE;
        int maxZ = mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE;
        int cleared = 0;
        for (org.bukkit.Chunk chunk : mouth.getWorld().getLoadedChunks()) {
            for (org.bukkit.block.BlockState tile : chunk.getTileEntities()) {
                if (tile.getX() < minX || tile.getX() > maxX || tile.getY() < minY
                        || tile.getY() > maxY || tile.getZ() < minZ || tile.getZ() > maxZ
                        || !(tile instanceof Lectern lectern)) continue;
                ItemStack item = lectern.getInventory().getItem(0);
                String bookId = currentV5BookId(item);
                V5AuthorityManifest.BookPlacement placement = placements.get(bookId);
                if (placement == null) continue;
                if (directorFlag(flags, placement.availabilityFlag())) {
                    if (auditV5BookItem(item, bookId) != null) {
                        lectern.getInventory().setItem(0, createV5Book(
                                java.util.Objects.requireNonNull(V5AuthorityManifest.book(bookId))));
                        lectern.update(true, false);
                    }
                } else if (clearUnavailable) {
                    lectern.getInventory().setItem(0, null);
                    lectern.update(true, false);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private List<String> ensureV5BookMounts(Location mouth, Set<String> protectedCells,
                                            Set<String> exactPhysicalCells) {
        if (mouth == null || mouth.getWorld() == null) return List.of("built Hold Mouth is unavailable");
        Set<String> claimed = new HashSet<>();
        List<String> issues = new ArrayList<>();
        for (V5AuthorityManifest.BookPlacement placement : V5AuthorityManifest.bookPlacements()) {
            if ("earned_artifact".equals(placement.holderKind())) continue;
            if (placement.holderId().startsWith("unlit_house_")) continue;
            Block existing = resolveV5BookLectern(placement, mouth, claimed);
            Location anchor = v5BookHolderAnchor(placement, mouth);
            BlockFace facing = parseBookFront(placement.expectedFront());
            if (anchor == null || anchor.getWorld() == null || facing == null) {
                issues.add(placement.bookId() + " has no resolvable holder/facing");
                continue;
            }
            boolean exactAnchor = "record_lectern".equals(placement.mount())
                    || "existing_lectern".equals(placement.mount());
            Block exact = anchor.getBlock();
            if (existing != null && existing.getType() == Material.LECTERN) {
                boolean intendedExact = exactPhysicalCells != null
                        && exactPhysicalCells.contains(blockKey(existing));
                boolean intendedAnchor = exactAnchor && blockKey(existing).equals(blockKey(exact));
                boolean occupiesGameplayLane = protectedCells != null
                        && protectedCells.contains(blockKey(existing));
                if (intendedExact || intendedAnchor || !occupiesGameplayLane) continue;
                if (existing.getState() instanceof Lectern stale) {
                    stale.getInventory().clear();
                    stale.update(true, false);
                }
                existing.setType(Material.AIR, false);
            }
            Block target = exactAnchor && (!(exact.getState() instanceof org.bukkit.block.TileState)
                    || exact.getType() == Material.LECTERN)
                    ? exact : selectV5BookMountCell(anchor, facing, placement.mount(), claimed,
                    protectedCells == null ? Set.of() : protectedCells);
            if (target == null) {
                issues.add(placement.bookId() + " has no safe lectern cell at " + placement.holderId());
                continue;
            }
            org.bukkit.block.BlockState prior = target.getState();
            if (prior instanceof InventoryHolder holder) {
                holder.getInventory().clear();
                prior.update(true, false);
            }
            placeReadableLectern(target, facing);
            claimed.add(blockKey(target));
        }
        return List.copyOf(issues);
    }

    private Location v5BookHolderAnchor(V5AuthorityManifest.BookPlacement placement, Location mouth) {
        DeepHoldV4Plan.RecordStation station = DeepHoldV4Plan.RECORD_STATIONS.stream()
                .filter(candidate -> candidate.id().equals(placement.holderId())).findFirst().orElse(null);
        if (station != null) return mouth.clone().add(station.x(), station.y(), station.z());
        DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(placement.holderId());
        if (fixture != null) return mouth.clone().add(fixture.x(), fixture.y(), fixture.z());
        Site site = plugin.sites() == null ? null : plugin.sites().get(placement.holderId());
        return site == null ? null : site.location();
    }

    private Block selectV5BookMountCell(Location anchor, BlockFace facing, String mount,
                                        Set<String> claimed, Set<String> protectedCells) {
        int lateral = mount.contains("left") || "mkept_station".equals(mount) ? -2
                : mount.contains("right") || "rook_station".equals(mount) ? 2 : 0;
        int[][] offsets = {{lateral, 5}, {lateral, 6}, {lateral, 4},
                {lateral - 1, 5}, {lateral + 1, 5}, {lateral - 1, 6}, {lateral + 1, 6},
                {lateral - 2, 5}, {lateral + 2, 5}, {lateral, 7}};
        FixtureTransform.BlockPos origin = new FixtureTransform.BlockPos(
                anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
        FixtureTransform.Cardinal front = FixtureTransform.Cardinal.valueOf(facing.name());
        for (int[] offset : offsets) {
            FixtureTransform.BlockPos candidate = V5BookMountPolicy.candidate(
                    origin, front, offset[0], offset[1]);
            Block block = anchor.getWorld().getBlockAt(candidate.x(), candidate.y(), candidate.z());
            if (claimed.contains(blockKey(block)) || protectedCells.contains(blockKey(block))
                    || protectedCells.contains(blockKey(block.getRelative(BlockFace.UP)))
                    || protectedCells.contains(blockKey(block.getRelative(BlockFace.DOWN)))
                    || !block.isPassable()
                    || !block.getRelative(BlockFace.UP).isPassable()
                    || !block.getRelative(BlockFace.DOWN).getType().isSolid()) continue;
            return block;
        }
        return null;
    }

    private String currentV5BookId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String bookId = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "book_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        return bookId != null ? bookId : item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "v5_book_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
    }

    private Block resolveV5BookLectern(V5AuthorityManifest.BookPlacement placement, Location mouth,
                                       Set<String> claimed) {
        Location anchor = null;
        int radius = 10;
        if (mouth != null && mouth.getWorld() != null) {
            DeepHoldV4Plan.RecordStation station = DeepHoldV4Plan.RECORD_STATIONS.stream()
                    .filter(candidate -> candidate.id().equals(placement.holderId())).findFirst().orElse(null);
            if (station != null) {
                anchor = mouth.clone().add(station.x(), station.y(), station.z());
                if ("record_lectern".equals(placement.mount())
                        && anchor.getBlock().getType() == Material.LECTERN) return anchor.getBlock();
            }
            DeepHoldV4Plan.Fixture fixture = DeepHoldV4Plan.fixture(placement.holderId());
            if (fixture != null) {
                anchor = mouth.clone().add(fixture.x(), fixture.y(), fixture.z());
                radius = Math.max(4, Math.min(14, fixture.radius() + 4));
                if ("existing_lectern".equals(placement.mount())
                        && anchor.getBlock().getType() == Material.LECTERN) return anchor.getBlock();
            }
        }
        if (anchor == null && plugin.sites() != null) {
            Site holder = plugin.sites().get(placement.holderId());
            if (holder != null) {
                anchor = holder.location();
                radius = Math.max(3, Math.min(14, holder.radius() + 3));
            }
        }
        if (anchor == null || anchor.getWorld() == null) return null;
        Location resolvedAnchor = anchor;
        List<Block> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = anchor.getWorld().getBlockAt(anchor.getBlockX() + dx,
                            anchor.getBlockY() + dy, anchor.getBlockZ() + dz);
                    if (block.getType() != Material.LECTERN) continue;
                    String key = blockKey(block);
                    if (!"branch_lectern".equals(placement.holderKind()) && claimed.contains(key)) continue;
                    if (!v5BookFacingMatches(block, placement.expectedFront())) continue;
                    candidates.add(block);
                }
            }
        }
        if (candidates.isEmpty()) return null;
        BlockFace front = parseBookFront(placement.expectedFront());
        BlockFace right = rightOf(front == null ? BlockFace.NORTH : front.getOppositeFace());
        String mount = placement.mount();
        java.util.Comparator<Block> byDistance = java.util.Comparator.comparingDouble(block ->
                block.getLocation().distanceSquared(resolvedAnchor));
        if (mount.contains("left") || "mkept_station".equals(mount)) {
            candidates.sort(java.util.Comparator.comparingInt((Block block) -> sideProjection(block, resolvedAnchor, right))
                    .thenComparing(byDistance));
        } else if (mount.contains("right") || "rook_station".equals(mount)) {
            candidates.sort(java.util.Comparator.comparingInt((Block block) -> sideProjection(block, resolvedAnchor, right))
                    .reversed().thenComparing(byDistance));
        } else {
            candidates.sort(java.util.Comparator.comparingInt((Block block) ->
                    Math.abs(sideProjection(block, resolvedAnchor, right))).thenComparing(byDistance));
        }
        Block selected = candidates.get(0);
        claimed.add(blockKey(selected));
        return selected;
    }

    private static int sideProjection(Block block, Location anchor, BlockFace right) {
        int dx = block.getX() - anchor.getBlockX();
        int dz = block.getZ() - anchor.getBlockZ();
        return dx * right.getModX() + dz * right.getModZ();
    }

    private static BlockFace parseBookFront(String raw) {
        try {
            return BlockFace.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean v5BookFacingMatches(Block block, String expected) {
        BlockFace front = parseBookFront(expected);
        return front == null || !(block.getBlockData() instanceof Directional directional)
                || directional.getFacing() == front;
    }

    private void handleFinaleMarkersV5(CommandSender sender) {
        Site unwriting = plugin.sites() == null ? null : plugin.sites().get("the_unwriting");
        Site release = plugin.sites() == null ? null : plugin.sites().get("release_record");
        Location nameAt = unwriting == null ? null : unwriting.location();
        Location severAt = release == null ? null : release.location();
        if (nameAt == null || severAt == null || nameAt.getWorld() == null || severAt.getWorld() == null) {
            sender.sendMessage("Observance: finale marker repair refused; build/register the V5 Hold first.");
            return;
        }
        placeHoldFinaleMarkers(nameAt.clone().add(0, 0, 1));
        placeV5SeverControl(severAt);
        sender.sendMessage(hasHoldFinaleMarkersNear(nameAt) && hasV5SeverControlNear(severAt)
                ? "Observance: exact V5 publish, release-unnamed, and SEVER RECORD controls are installed."
                : "Observance: finale control verification failed; finale arm remains closed.");
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
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "spawn";
        String id = args.length > 2 && !args[2].isBlank() ? args[2].trim().toLowerCase(Locale.ROOT) : null;
        if (id == null && com.observance.watcher.npc.TownsfolkNpc.byId(op) != null) {
            id = op;
            op = "spawn";
        }
        switch (op) {
            case "spawn" -> {
                if (id != null) {
                    if (com.observance.watcher.npc.TownsfolkNpc.byId(id) == null) {
                        sender.sendMessage("Observance: unknown townsperson '" + id
                                + "'. One of: aro | wenna | coll | dob | old-pell.");
                        return;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Observance: stand at " + id
                                + "'s final village position to persist its V5 anchor.");
                        return;
                    }
                    Location loc = player.getLocation();
                    if (loc == null || loc.getWorld() == null) {
                        sender.sendMessage("Observance: could not resolve your location.");
                        return;
                    }
                    V5DialogueCatalog.Npc npc = V5DialogueCatalog.townsperson(id);
                    if (npc == null) {
                        sender.sendMessage("Observance: packaged V5 dialogue has no NPC " + id + ".");
                        return;
                    }
                    Site anchor = new Site(npc.anchorSite(), "npc_anchor", loc.getWorld().getName(),
                            loc.getX(), loc.getY(), loc.getZ(), 2, 3, true, true, null, false);
                    if (!plugin.registerRuntimeSite(anchor)) {
                        sender.sendMessage("Observance: anchor persistence failed; NPC was not moved.");
                        return;
                    }
                    var body = townsfolk.spawnOne(id, loc);
                    if (body == null) {
                        sender.sendMessage("Observance: could not spawn '" + id + "' here (world/chunk unavailable?).");
                        return;
                    }
                    sender.sendMessage("Observance: " + id + " is anchored at " + npc.anchorSite()
                            + " (" + townsfolk.backend() + "). Right-click to hear exact V5 dialogue.");
                } else {
                    int placed = 0;
                    List<String> missing = new ArrayList<>();
                    for (var who : com.observance.watcher.npc.TownsfolkNpc.TOWNSFOLK) {
                        V5DialogueCatalog.Npc npc = V5DialogueCatalog.townsperson(who.id());
                        Site anchor = npc == null || plugin.sites() == null
                                ? null : plugin.sites().get(npc.anchorSite());
                        Location loc = anchor == null ? null : anchor.location();
                        if (loc == null || loc.getWorld() == null) {
                            missing.add(npc == null ? who.id() : npc.anchorSite());
                            continue;
                        }
                        if (townsfolk.spawnOne(who.id(), loc) != null) placed++;
                    }
                    sender.sendMessage("Observance: spawned " + placed + "/5 townsfolk at persisted V5 anchors.");
                    if (!missing.isEmpty()) sender.sendMessage("  Missing anchors: " + String.join(", ", missing)
                            + ". Stand at each final position and run /obs townsfolk spawn <id>.");
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
        sender.sendMessage("Observance V5: /obs audit now runs the production preflight.");
        handlePreflight(sender);
    }

    private void handlePreflight(CommandSender sender) {
        sender.sendMessage("== Observance V5 production preflight ==");
        List<String> staticIssues = v5ProductionIssues();
        var sb = plugin.supabase();
        if (sb == null || !sb.isConfigured()) {
            staticIssues.add("Supabase progression storage is not configured");
        }
        sender.sendMessage("Checking the durable local record and monotonic authored-book state...");
        var runtime = plugin.v5Runtime();
        if (runtime == null) {
            staticIssues.add("V5 local runtime is unavailable");
            sendV5PreflightResult(sender, staticIssues, null);
            return;
        }
        com.observance.watcher.v5runtime.ProgressSnapshot snapshot = runtime.snapshot();
        Map<String, Object> facts = new LinkedHashMap<>();
        snapshot.booleans().forEach(facts::put);
        snapshot.branches().forEach(facts::put);
        snapshot.conductVerdict().ifPresent(
                value -> facts.put("v5_conduct_verdict", value.wireValue()));
        runtime.projectLocalState();
        BookSyncResult books = syncV5Books(facts, false);
        staticIssues.addAll(books.issues());
        sendV5PreflightResult(sender, staticIssues, books);
    }

    private List<String> v5ProductionIssues() {
        List<String> issues = new ArrayList<>(V5AuthorityManifest.inspect().issues());
        if (!com.observance.watcher.v5runtime.IdentityLinkCode.canIssueForAuthenticatedUuid(
                plugin.getServer().getOnlineMode())) {
            issues.add("server.properties online-mode must be true for authenticated /obslink identity proofs");
        }
        if (plugin.sites() == null) {
            issues.add("sites.yml is not loaded");
            return issues;
        }
        Set<String> required = new LinkedHashSet<>();
        required.add(HOLD_REGION_SITE_ID);
        required.add(HOLD_ENTRY_REGION_SITE_ID);
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) required.add(fixture.id());
        for (DeepHoldV5Manifest.GateContract gate : DeepHoldV5Manifest.GATE_CONTRACTS) {
            required.add(holdGateSiteId(gate.siteId()));
        }
        required.add("release_record");
        required.addAll(List.of("unlit_entry", "unlit_spawn_mirror", "unlit_exit",
                "unlit_house_lamp", "unlit_house_cairn", "unlit_house_coop", "unlit_house_well",
                "unlit_house_watch", "unlit_house_warm", "unlit_house_threshold", "unlit_house_base"));
        for (V5DialogueCatalog.Npc npc : V5DialogueCatalog.townsfolk().values()) required.add(npc.anchorSite());
        required.add(V5DialogueCatalog.wren().anchorSite());
        for (String id : required) {
            Site site = plugin.sites().get(id);
            if (site == null || !site.enabled() || !site.isPlaced() || site.location() == null) {
                issues.add("required V5 site is missing/unplaced: " + id);
            }
        }

        Location mouth = resolveDeepHoldV4Mouth();
        if (mouth == null) {
            issues.add("Deep Hold Mouth cannot be reconstructed");
        } else {
            String route = auditV4OpenRoute(mouth.getWorld(), mouth);
            if (route != null) issues.add(route);
            for (HoldGate gate : DEEP_HOLD_GATES) {
                Site site = plugin.sites().get(holdGateSiteId(gate.id()));
                Location location = site == null ? null : site.location();
                if (location == null) continue;
                String gateIssue = auditHoldGateIntegrity(gate, location);
                if (gateIssue != null) issues.add(gateIssue);
            }
        }
        for (V5DialogueCatalog.Npc npc : V5DialogueCatalog.townsfolk().values()) {
            String managerId = npc.id().replace('_', '-');
            Entity body = plugin.townsfolk() == null ? null : plugin.townsfolk().body(managerId);
            Site anchor = plugin.sites().get(npc.anchorSite());
            if (!entityAtAnchor(body, anchor)) issues.add("V5 NPC is missing or away from anchor: " + npc.id());
        }
        Entity wrenBody = plugin.wren() == null ? null : plugin.wren().body();
        if (!entityAtAnchor(wrenBody, plugin.sites().get(V5DialogueCatalog.wren().anchorSite()))) {
            issues.add("V5 NPC is missing or away from anchor: wren");
        }
        if (plugin.v5Runtime() == null) {
            issues.add("V5 local runtime/finale coordinator is unavailable");
        } else {
            issues.addAll(plugin.v5Runtime().readinessFindings());
            issues.addAll(V5RuntimePredicateRegistry.validateAgainst(V5AuthorityManifest.runtimeBindings()));
        }
        return issues;
    }

    private static boolean entityAtAnchor(Entity entity, Site anchor) {
        if (entity == null || !entity.isValid() || anchor == null) return false;
        Location expected = anchor.location();
        Location actual = entity.getLocation();
        return expected != null && actual.getWorld() == expected.getWorld()
                && actual.distanceSquared(expected) <= 4.0;
    }

    private void sendV5PreflightResult(CommandSender sender, List<String> issues, BookSyncResult books) {
        if (books != null) sender.sendMessage("Books: " + books.resolved() + "/38 physical holders resolved; "
                + books.written() + " reconciled, " + books.cleared() + " locked/cleared.");
        sender.sendMessage("Authority: 82 nodes, 32 rooms, 76 fixtures, 8 gates, 44 books, 21 artifacts, "
                + "6 NPCs, 5 media; hash=" + V5AuthorityManifest.inspect().authorityHash());
        if (issues.isEmpty()) {
            sender.sendMessage("V5 PREFLIGHT PASS: only the canonical Hold + Mouth/village NPCs + well-based Unlit are required.");
            return;
        }
        sender.sendMessage("V5 PREFLIGHT FAIL (" + issues.size() + "):");
        for (String issue : issues) sender.sendMessage(" - " + issue);
        sender.sendMessage("Legacy Keeper scatter, Nether/End sites, Seventh Reading, errands, and rehearsal lanes are not V5 blockers.");
    }

    private void handleDialogueAudit(CommandSender sender) {
        sender.sendMessage("== Observance V5 dialogue audit ==");
        sender.sendMessage("Packaged authority: 5 townsfolk + Wren, 36 states, 69 exact local lines.");
        for (V5DialogueCatalog.Npc npc : V5DialogueCatalog.townsfolk().values()) {
            Site anchor = plugin.sites() == null ? null : plugin.sites().get(npc.anchorSite());
            Entity body = plugin.townsfolk() == null ? null
                    : plugin.townsfolk().body(npc.id().replace('_', '-'));
            sender.sendMessage(" " + npc.displayName() + ": " + npc.states().size() + " states, anchor="
                    + npc.anchorSite() + ", " + (entityAtAnchor(body, anchor) ? "READY" : "MISSING/MISPLACED"));
        }
        V5DialogueCatalog.Npc wren = V5DialogueCatalog.wren();
        sender.sendMessage(" Wren: " + wren.states().size() + " states, anchor=" + wren.anchorSite() + ", "
                + (entityAtAnchor(plugin.wren() == null ? null : plugin.wren().body(),
                plugin.sites() == null ? null : plugin.sites().get(wren.anchorSite()))
                ? "READY" : "MISSING/MISPLACED"));
        sender.sendMessage("Legacy errands/conduct/seventh dialogue is disabled in V5 production listeners.");
    }

    private void handleVisualAudit(CommandSender sender) {
        sender.sendMessage("== Observance V5 live-world visual check ==");
        handlePlaceHoldAudit(sender);
        sender.sendMessage("Manual pass: walk every Hold room and the full Unlit route in survival view.");
        sender.sendMessage("Confirm clear doorways and stairs, reachable lecterns and containers, correct block facing, and no fixture embedded in a wall or another block.");
        sender.sendMessage("Record the room and Unlit screenshots in the V5 live-test matrix; this command does not treat decorative block counts as visual proof.");
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
        // V4's focused Prior inputs are deliberately flush filing slits embedded in the authored
        // case-board/camp/failed-Accepting set pieces. Their owner fixtures receive the silhouette
        // audit; grading each slit as a standalone monument produces a false "flat" warning.
        if (id != null && id.startsWith("hold_answer_")) return false;
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
                || "discord_relay".equals(id)
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
            String line = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(i));
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
        if ("discord_relay".equals(type)) {
            int radius = Math.max(3, site.radius());
            if (block.getType() != Material.LECTERN || countFilledLecternsNear(loc, radius) < 5) {
                return "FAIL discord_relay: expected five filled procedure/card lecterns.";
            }
            if (countFacingLecternsNear(loc, radius, BlockFace.SOUTH) < 5) {
                return "FAIL discord_relay: all five lecterns must face the south approach.";
            }
            if (!hasMaterialNear(loc, radius, Material.COPPER_BLOCK)
                    || !hasMaterialNear(loc, radius, Material.EXPOSED_COPPER)
                    || !hasMaterialNear(loc, radius, Material.WEATHERED_COPPER)
                    || !hasMaterialNear(loc, radius, Material.OXIDIZED_COPPER)) {
                return "FAIL discord_relay: callback rack is missing one or more copper-age jackets.";
            }
            if (!relayShellIntact(loc)) {
                return "FAIL discord_relay: roof, bounded walls, or flat three-wide south entrance is broken.";
            }
            return null;
        }
        if ("report_lectern".equals(type) || "mara_lectern".equals(type) || "sella_lectern".equals(type)) {
            if (block.getType() != Material.LECTERN) {
                return "FAIL " + site.id() + ": expected a lectern, found " + block.getType() + ".";
            }
            if (!(block.getState() instanceof Lectern lectern)) {
                return "FAIL " + site.id() + ": lectern state did not load.";
            }
            ItemStack book = lectern.getInventory().getItem(0);
            // V5 books are unlock-controlled. Empty is correct before the exact authority flag opens;
            // any present book, however, must carry a packaged V5 identity/version.
            if (book != null && book.getType() != Material.AIR) {
                String bookId = currentV5BookId(book);
                if (bookId == null || auditV5BookItem(book, bookId) != null) {
                    return "FAIL " + site.id() + ": present book is not an exact packaged V5 book.";
                }
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
                && block.getType() != Material.BARREL
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CONCRETE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected the Hold-native reckoning line, HS07 key console, judgement marks, and record lectern.";
        }
        if ("the_threshold".equals(site.id())
                && (block.getType() != Material.REINFORCED_DEEPSLATE
                && block.getType() != Material.POLISHED_DEEPSLATE
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SCULK)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected the builder-stage threshold slab or final WR04 route barrier, sculk grave line, and threshold note.";
        }
        if ("threshold_vault".equals(site.id())
                && (block.getType() != Material.STONE_PRESSURE_PLATE
                && block.getType() != Material.BARREL
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_TUFF))) {
            return "FAIL " + site.id() + ": expected the builder-stage witness plate or final WR05 Bridge housing, plus four hand marks.";
        }
        if ("unbroken_light".equals(site.id())
                && (block.getType() != Material.SEA_LANTERN
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || countMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL, 6) < 6)) {
            return "FAIL " + site.id() + ": expected Accepting floor, unbroken-light anchor, six token receptacles, and record lectern.";
        }
        if ("case_board".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_BOOKSHELF))) {
            return "FAIL " + site.id() + ": expected case-board lecterns, filed storage, and shelves; exact V5 books own the readable surfaces.";
        }
        if ("prior_camp".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CAMPFIRE)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LIGHT_GRAY_CARPET)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.GRAY_CANDLE))) {
            return "FAIL " + site.id() + ": expected prior-run campfire, blank witness place, bedrolls, exact V5 stations, and record mounts.";
        }
        if ("failed_accepting".equals(site.id())
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.CHISELED_TUFF)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CANDLE))) {
            return "FAIL " + site.id() + ": expected failed accepting floor, six old token marks, witness blank, and V5 record lecterns.";
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
                && block.getType() != Material.BARREL
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || !hasHoldFinaleMarkersNear(loc))) {
            return "FAIL " + site.id() + ": expected the RP02 Averyn slot, protocol lectern, and tagged publish/unnamed markers.";
        }
        if ("release_record".equals(site.id()) && !hasV5SeverControlNear(loc)) {
            return "FAIL release_record: exact tagged SEVER RECORD control or confirmation cell is missing.";
        }
        if ("vaun_hoard_chest".equals(type)
                && (block.getType() != Material.POLISHED_DEEPSLATE
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN)
                || countMaterialNear(loc, Math.max(3, site.radius()), Material.BARREL, 2) < 2)) {
            return "FAIL " + site.id() + ": expected the V5 quartermaster focal stone, ledger lectern, receipts barrel, and audit tray; the registered anchor is intentionally not a chest.";
        }
        if (needsAnswerSurface(site) && !hasEditableSignNear(loc, Math.max(1, site.radius()))) {
            return "FAIL " + site.id() + ": no editable answer sign found inside answer radius.";
        }
        if ("painted_line".equals(type)
                && !hasMaterialNear(loc, Math.max(3, site.radius()), Material.BLACK_CONCRETE)) {
            return "FAIL " + site.id() + ": expected a flush black crossing inlay.";
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
        if ("lampworks_stair".equals(type)
                && (!hasMaterialNear(loc, Math.max(3, site.radius()), Material.POLISHED_BASALT)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.SOUL_LANTERN)
                || !hasMaterialNear(loc, Math.max(3, site.radius()), Material.LECTERN))) {
            return "FAIL " + site.id() + ": expected open stair spine, paired lamp posts, and lamp-count record.";
        }
        if (isCoreAuditSite(site.id()) && block.getType() == Material.AIR
                && !"lampworks_stair".equals(type) && !"painted_line".equals(type)) {
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

    private int countMaterialNear(Location loc, int radius, Material material, int cap) {
        if (loc == null || loc.getWorld() == null || material == null) return 0;
        int found = 0;
        int r = Math.max(1, Math.min(12, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy,
                            loc.getBlockZ() + dz).getType() == material && ++found >= cap) return found;
                }
            }
        }
        return found;
    }

    private int countFilledLecternsNear(Location loc, int radius) {
        if (loc == null || loc.getWorld() == null) return 0;
        int found = 0;
        int r = Math.max(1, Math.min(12, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy,
                            loc.getBlockZ() + dz);
                    if (!(b.getState() instanceof Lectern lectern)) continue;
                    ItemStack item = lectern.getInventory().getItem(0);
                    if (item != null && item.getType() == Material.WRITTEN_BOOK) found++;
                }
            }
        }
        return found;
    }

    private int countFacingLecternsNear(Location loc, int radius, BlockFace facing) {
        if (loc == null || loc.getWorld() == null || facing == null) return 0;
        int found = 0;
        int r = Math.max(1, Math.min(12, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = loc.getWorld().getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy,
                            loc.getBlockZ() + dz);
                    if (b.getType() == Material.LECTERN && b.getBlockData() instanceof Directional d
                            && d.getFacing() == facing) found++;
                }
            }
        }
        return found;
    }

    /** Exact shell contract relative to the persisted procedure lectern at centerZ+3. */
    private boolean relayShellIntact(Location procedure) {
        if (procedure == null || procedure.getWorld() == null) return false;
        World world = procedure.getWorld();
        int cx = procedure.getBlockX();
        int cy = procedure.getBlockY();
        int centerZ = procedure.getBlockZ() - 3;
        // Roof samples at center and four corners.
        for (int[] p : new int[][]{{0, 0}, {-8, -7}, {-8, 7}, {8, -7}, {8, 7}}) {
            if (!world.getBlockAt(cx + p[0], cy + 6, centerZ + p[1]).getType().isSolid()) return false;
        }
        // Side/back wall samples must stay closed.
        for (int[] p : new int[][]{{-8, 1, 0}, {8, 1, 0}, {0, 1, -7}}) {
            if (!world.getBlockAt(cx + p[0], cy + p[1], centerZ + p[2]).getType().isSolid()) return false;
        }
        // South opening and its two-block exterior path remain flat and traversable.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                if (!world.getBlockAt(cx + dx, cy + dy, centerZ + 7).isEmpty()) return false;
            }
            if (!world.getBlockAt(cx + dx, cy - 1, centerZ + 8).getType().isSolid()) return false;
            if (!world.getBlockAt(cx + dx, cy - 1, centerZ + 9).getType().isSolid()) return false;
        }
        return true;
    }

    private boolean hasItemFrameNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return false;
        double r = Math.max(0.5, Math.min(4.0, radius));
        return !loc.getWorld().getNearbyEntitiesByType(ItemFrame.class, loc, r).isEmpty();
    }

    private boolean hasHoldFinaleMarkersNear(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        var nameKey = new org.bukkit.NamespacedKey(plugin, "v5_name_treatment");
        boolean publish = false;
        boolean unnamed = false;
        for (org.bukkit.entity.Interaction stand : loc.getWorld().getNearbyEntitiesByType(
                org.bukkit.entity.Interaction.class, loc, 8.0)) {
            try {
                String choice = stand.getPersistentDataContainer().get(
                        nameKey, org.bukkit.persistence.PersistentDataType.STRING);
                if ("publish".equalsIgnoreCase(choice)) publish = true;
                if ("release_unnamed".equalsIgnoreCase(choice)) unnamed = true;
            } catch (Throwable ignored) { }
        }
        return publish && unnamed;
    }

    private boolean hasV5SeverControlNear(Location loc) {
        if (loc == null || loc.getWorld() == null || loc.getBlock().getType() != Material.SCULK_CATALYST) {
            return false;
        }
        var key = new org.bukkit.NamespacedKey(plugin,
                com.observance.watcher.finale.FinaleController.PDC_FINALE_CONTROL);
        for (org.bukkit.entity.Interaction control : loc.getWorld().getNearbyEntitiesByType(
                org.bukkit.entity.Interaction.class, loc, 2.5)) {
            String value = control.getPersistentDataContainer().get(
                    key, org.bukkit.persistence.PersistentDataType.STRING);
            if (com.observance.watcher.finale.FinaleController.CONTROL_SEVER.equals(value)) return true;
        }
        return false;
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
            case "discord_relay", "first_report_lectern_01", "first_marker_01", "rune_rosetta",
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
        var runtime = plugin.v5Runtime();
        if (runtime == null) {
            sender.sendMessage(" V5 runtime:         unavailable");
        } else if (runtime.isCoda()) {
            sender.sendMessage(" V5 runtime:         coda (story inputs locked)");
        } else {
            sender.sendMessage(" V5 runtime:         "
                    + (runtime.storyInputsEnabled() ? "active" : "story inputs locked"));
        }
        sendPackStatus(sender);
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
        if (args.length > 1 && V5_RETIRED_COMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return out;
        }
        if (args.length == 1) {
            for (String s : new String[]{"status", "audit", "visualaudit", "dialogueaudit", "preflight",
                    "repair", "runbook", "reload", "sleep", "unlit", "placehold",
                    "item", "wren", "townsfolk", "finale"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("placehold")) {
            for (String s : new String[]{"prepare", "plan", "build", "repair", "audit", "status", "seal", "open", "sync"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("placehold")
                && args[1].equalsIgnoreCase("repair")) {
            if ("all".startsWith(args[2].toLowerCase(Locale.ROOT))) out.add("all");
            for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
                if (fixture.id().startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(fixture.id());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("placehold")
                && (args[1].equalsIgnoreCase("seal") || args[1].equalsIgnoreCase("open"))) {
            for (String s : holdGateSuggestions(args[2])) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("director")) {
            for (String s : new String[]{"state", "progress", "players", "stuck", "hints", "world", "lab"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            if ("recover".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("recover");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("item")
                && args[1].equalsIgnoreCase("recover")) {
            for (String id : CanonicalArtifactRegistry.ids()) {
                if (id.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(id);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("item")
                && args[1].equalsIgnoreCase("recover")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT))) {
                    out.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("finale")) {
            for (String s : new String[]{"arm", "cancel", "status", "markers"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("finale")
                && args[1].equalsIgnoreCase("arm")) {
            for (String s : new String[]{"120", "60", "300", "600"}) {
                if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
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
            for (String s : new String[]{"site", "clue", "audit", "darken", "border", "buildmode", "ready"}) {
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("site")) {
            for (String s : new String[]{"launch", "set"}) {
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
            for (String s : new String[]{"spawn", "despawn"}) {
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
