package com.observance.watcher.command;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.structure.StructureTemplates;
import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /observance} admin command. Read-only status + safe controls (reload, local sleep
 * toggle). The body is wrapped in Safety so a bad command never propagates an exception.
 */
public final class ObservanceCommand implements CommandExecutor, TabCompleter {

    private final ObservancePlugin plugin;
    private final Safety safety;

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
            case "repair" -> handleRepair(sender);
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
            case "placelab" -> handlePlaceLab(sender, args);
            case "fullrun" -> handleFullRun(sender, args);
            case "prepworld" -> handlePrepWorld(sender, args);
            case "placeprologue" -> handlePlacePrologue(sender, args);
            case "lens" -> handleLens(sender, args);
            case "wren" -> handleWren(sender, args);
            case "keeper" -> handleKeeper(sender, args);
            case "townsfolk" -> handleTownsfolk(sender, args);
            case "test" -> handleTest(sender, args);
            case "needle" -> handleNeedle(sender, args);
            case "finale" -> handleFinaleMarkers(sender);
            case "reading" -> handleReadingCarvings(sender);
            default -> sender.sendMessage("Unknown subcommand. Use: status | audit | repair | reload | sleep <on|off> | flag <set|clear|list> | site set <siteId> | placeworld | placeroom <keeperId> | placeregion | placedeep | placelecterns | placelab | fullrun | prepworld | placeprologue | lens give [player] | wren <spawn|despawn|reckoning> | keeper <spawn|despawn> [node] | townsfolk <spawn|despawn> [id] | test <menu|preset> [player] | needle [player] | finale | reading");
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
        if (!op.equals("set")) {
            sender.sendMessage("Usage: /observance site set <siteId>   (try tab-complete; keeper spine: " + keeperIdList() + ")");
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
        boolean beacon = existing != null && existing.beacon();
        String world = loc.getWorld().getName();

        // Record the sender's block position as the survey anchor. placeworld terrain-re-seats on the
        // surface at this X/Z, so the stored Y need only be in the right column.
        Site site = new Site(siteId, siteType, world,
                (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                radius, verticalRadius, protect, enabled, puzzleKey, beacon);
        plugin.registerRuntimeSite(site); // also persists to sites.yml (idempotent — re-survey overwrites)

        sender.sendMessage("Observance: surveyed '" + siteId + "' -> "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " in " + world
                + " (type " + siteType + ", r" + radius + ").");
        sender.sendMessage(row != null
                ? "  Saved to sites.yml. Run /observance placeworld to stamp the large set-pieces."
                : "  Saved to sites.yml. This smaller anchor is now live.");
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
                // Still (re)register the site so its coords are authoritative in sites.yml (keep its beacon flag).
                boolean occBeacon = existing != null && existing.beacon();
                plugin.registerRuntimeSite(new Site(siteId, siteType, worldName,
                        (double) ax, (double) ay, (double) az, radius, 6, true, true, null, occBeacon));
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

            // KEPT-LIGHT beacon: only the two canonically-lit sites (visual_beacon:true in sites.yml) get a
            // landmark beam — the fiction's "one light, somewhere below, did not go out." Read the flag off the
            // loaded config site, thread it into the runtime re-register so a reload keeps it, and stamp the beam.
            boolean beacon = existing != null && existing.beacon();
            plugin.registerRuntimeSite(new Site(siteId, siteType, worldName,
                    (double) ax, (double) ay, (double) az, radius, 6, true, true, null, beacon));
            placed++;
            String beaconNote = "";
            if (beacon) {
                boolean skyClear = StructureTemplates.keptLightBeacon(anchor, beaconTint(siteId));
                beaconNote = skyClear ? " [kept-light beacon: beam projecting]"
                        : " [kept-light beacon: sky blocked — base+light placed, no beam]";
            }
            sender.sendMessage("  " + siteId + ": " + (fromSurvey ? "SURVEYED" : "auto-scatter")
                    + " @ " + ax + "," + ay + "," + az + " -> placed." + beaconNote);
        }

        sender.sendMessage("Observance: placeworld complete — " + placed + " placed, " + occupied
                + " occupied, " + failed + " failed, " + skippedLanes + " lane(s) skipped ("
                + surveyed + " surveyed / " + auto + " auto-scattered) of "
                + KEEPER_SPINE.length + " keepers.");
        sender.sendMessage("  Scatter is deterministic (same base = same auto anchors). Re-run is idempotent. "
                + "Survey a spot with /observance site set <keeperId> to override an auto anchor.");
        if (skippedLanes > 0) {
            sender.sendMessage("  Nether/End lanes are survey-only: stand IN the Nether/End, `site set` the lane, "
                    + "then run placeworld FROM that dimension to stamp it there.");
        }
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

    /**
     * KEPT-LIGHT beam tint per site — the beacon glass reads as firelight, not a neutral waypoint.
     * Brann's watch-fire is warm orange/red ("one fire was never doused"); the unbroken_light is pale/white
     * (the one clean fire in the deep). Any other beaconed site → null (plain white beam). Never throws.
     */
    private static Material beaconTint(String siteId) {
        String id = siteId == null ? "" : siteId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "stone_brann", "brann"    -> Material.ORANGE_STAINED_GLASS;   // the watch-fire's firelight
            case "unbroken_light"          -> Material.WHITE_STAINED_GLASS;    // the one unbroken light
            default                        -> null;                            // plain white beam
        };
    }

    /** First non-null Site among the candidates, or null. Used to resolve a keeperId to its config entry. */
    private static Site firstNonNull(Site... sites) {
        if (sites != null) for (Site s : sites) if (s != null) return s;
        return null;
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

        // KEPT-LIGHT beacon: honor visual_beacon for the canonically-lit sites even on this manual path. The
        // flag is read off the config entry the keeperId maps to (accepts the bare "brann" and the slug forms),
        // so placing Brann's watch-stone or the unbroken_light by hand still raises its landmark beam.
        Site cfg = plugin.sites() == null ? null : firstNonNull(
                plugin.sites().get(siteId),           // keeper_<slug>
                plugin.sites().get("stone_" + keeperId),
                plugin.sites().get(keeperId));        // e.g. unbroken_light
        boolean beacon = cfg != null && cfg.beacon();

        // Register the pillar's base as a live keeper_stone site. The site radius (config defaults) comfortably
        // covers the sign a few blocks above, so the answer sign resolves. In-memory only (see registerRuntimeSite).
        String world = loc.getWorld().getName();
        Site site = new Site(siteId, "keeper_stone", world,
                (double) loc.getBlockX(), (double) loc.getBlockY(), (double) loc.getBlockZ(),
                6, 6, false, true, null, beacon);
        plugin.registerRuntimeSite(site);

        sender.sendMessage("Observance: placed keeper stone '" + siteId + "' at "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " in " + world + ".");
        if (beacon) {
            boolean skyClear = StructureTemplates.keptLightBeacon(loc, beaconTint(keeperId));
            sender.sendMessage("  Kept-light beacon raised (" + (skyClear
                    ? "beam projecting" : "sky blocked — base+light placed, no beam") + ").");
        }
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

            // KEPT-LIGHT beacon: unbroken_light carries visual_beacon (the one fire that never goes out). Deep
            // in a roofed undercroft the sky is blocked, so keptLightBeacon leaves the base + a real light (no
            // sky-beam) — the kept light reads on the ground where the descent ends, which is exactly right here.
            Site cfg = plugin.sites() == null ? null : plugin.sites().get(siteId);
            boolean beacon = cfg != null && cfg.beacon();
            Site site = new Site(siteId, siteType, world,
                    siteLoc.getX(), siteLoc.getY(), siteLoc.getZ(),
                    radius, 6, true, true, null, beacon);
            plugin.registerRuntimeSite(site); // also writes to sites.yml
            if (beacon) {
                boolean skyClear = StructureTemplates.keptLightBeacon(siteLoc, beaconTint(siteId));
                sender.sendMessage("  " + siteId + ": kept-light beacon "
                        + (skyClear ? "beam projecting." : "sky blocked (deep) — base+light placed, no beam."));
            }

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

    private void handlePlaceLab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Observance: /observance placelab must be run by a player (needs a location).");
            return;
        }
        if (plugin.sites() == null || plugin.sites().all().isEmpty()) {
            sender.sendMessage("Observance: no sites loaded; reload first.");
            return;
        }

        int spacing = 18;
        if (args.length >= 2) {
            try {
                spacing = Math.max(14, Math.min(36, Integer.parseInt(args[1].trim())));
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
        int platformRadius = 7;
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
                labelLabCell(base, cfg.id(), cfg.type());
                buildLabFixture(cfg, base);

                Site labSite = new Site(cfg.id(), cfg.type(), worldName,
                        (double) base.getBlockX(), (double) base.getBlockY(), (double) base.getBlockZ(),
                        cfg.radius(), cfg.verticalRadius(), cfg.protect(), true,
                        cfg.puzzleKey(), cfg.beacon());
                plugin.registerRuntimeSite(labSite);
                if (cfg.beacon()) {
                    StructureTemplates.keptLightBeacon(base, beaconTint(cfg.id()));
                }
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
        String spacing = args.length >= 2 ? args[1] : "18";
        sender.sendMessage("== Observance full-run rehearsal ==");
        sender.sendMessage("Step 1/5: building the complete floating placement lab...");
        handlePlaceLab(sender, new String[]{"placelab", spacing});

        sender.sendMessage("Step 2/5: carving the Seventh Reading into the keeper-stone lab cells...");
        handleReadingCarvings(sender);

        sender.sendMessage("Step 3/5: placing Wren reckoning and finale choice markers at your feet...");
        placeReckoningMarkers(player, sender);
        handleFinaleMarkers(sender);

        sender.sendMessage("Step 4/5: giving tester tools...");
        handleLens(sender, new String[]{"lens", "give", player.getName()});
        handleNeedle(sender, new String[]{"needle", player.getName()});

        sender.sendMessage("Step 5/5: ready. Use this order for the human test pass:");
        sender.sendMessage("  1) Read/open every lab book and sign; lecterns should already contain books.");
        sender.sendMessage("  2) Solve one fixture from each family: bow, chest, bookshelf, lecterns, frames, pool, corridor, vault.");
        sender.sendMessage("  3) Run /obs test stalker to check the stronger Watcher scare.");
        sender.sendMessage("  4) Use /obs flag set <key> when you need to jump a gate instead of replaying the whole chain.");
        sender.sendMessage("  5) Keep this world as rehearsal only; do real launch placement after the pass.");
    }

    /**
     * {@code /observance prepworld [spacing]} - compact playable-world bootstrap. Unlike fullrun, this
     * places the main spine into the current real world instead of a floating lab, so a tester can walk
     * the ARG without surveying every individual site by hand first.
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

        int spacing = 18;
        if (args.length >= 2) {
            try {
                spacing = Math.max(14, Math.min(32, Integer.parseInt(args[1].trim())));
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
        int surfacePlaced = placeCompactSpine(origin.clone().add(0, 0, spacing), surface, spacing);

        sender.sendMessage("Step 3/7: placing the deep payoff sites...");
        String[][] deep = {
                {"stone_of_reckoning", "structure", "6"},
                {"the_cold_hearth", "marker", "6"},
                {"unbroken_light", "accepting_floor", "10"},
                {"the_threshold", "the_threshold", "6"},
                {"the_unwriting", "seventh_shrine", "6"},
                {"threshold_vault", "coop_plate", "6"},
        };
        int deepPlaced = placeCompactSpine(origin.clone().add(0, 0, -spacing), deep, spacing);

        sender.sendMessage("Step 4/7: placing Mara page-lock lecterns with books...");
        int lecternsPlaced = placeMaraLecternsAt(origin.clone().add(spacing, 0, spacing * 2), 3);

        sender.sendMessage("Step 5/7: carving reading/finale markers...");
        handleReadingCarvings(sender);
        handleFinaleMarkers(sender);

        sender.sendMessage("Step 6/7: spawning NPC row where possible...");
        Location npc = origin.clone().add(-spacing, 1, -spacing);
        if (plugin.townsfolk() != null) plugin.townsfolk().spawnAll(npc);
        if (plugin.wren() != null) plugin.wren().spawn(npc.clone().add(5, 0, 0));
        if (plugin.keeper() != null) plugin.keeper().spawn(npc.clone().add(9, 0, 0), "prepworld");

        sender.sendMessage("Step 7/7: giving tester tools...");
        handleLens(sender, new String[]{"lens", "give", player.getName()});
        handleNeedle(sender, new String[]{"needle", player.getName()});

        sender.sendMessage("Observance: prepworld complete - prologue staged, " + surfacePlaced
                + "/7 surface sites, " + deepPlaced + "/6 deep sites, " + lecternsPlaced
                + "/5 Mara lecterns.");
        sender.sendMessage("  Walkable test order: prologue -> rosetta/keepers -> Mara lecterns -> deep sites -> finale.");
        sender.sendMessage("  Optional Nether/End lanes still require standing in that dimension and using /obs site set + /obs placeworld.");
        sender.sendMessage("  Run /obs test stalker or /obs test hunt for the stronger Watcher scare pass.");
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
        int[] zOffsets = {0, 5, -6, 9, -4, 7, -8};
        for (int i = 0; i < rows.length; i++) {
            String siteId = rows[i][0];
            String siteType = rows[i][1];
            int radius;
            try { radius = Integer.parseInt(rows[i][2]); } catch (NumberFormatException e) { radius = 8; }

            int sx = origin.getBlockX() + (i * spacing);
            int sz = origin.getBlockZ() + zOffsets[i % zOffsets.length];
            int sy = world.getHighestBlockYAt(sx, sz, org.bukkit.HeightMap.OCEAN_FLOOR) + 1;
            Location siteLoc = new Location(world, sx, sy, sz);

            Location answer = StructureTemplates.keeper(siteId, siteLoc);
            if (answer == null) continue;

            Site cfg = plugin.sites() == null ? null : plugin.sites().get(siteId);
            boolean beacon = cfg != null && cfg.beacon();
            String puzzleKey = cfg == null ? null : cfg.puzzleKey();
            Site site = new Site(siteId, siteType, worldName,
                    siteLoc.getX(), siteLoc.getY(), siteLoc.getZ(),
                    radius, 6, true, true, puzzleKey, beacon);
            plugin.registerRuntimeSite(site);
            if (beacon) StructureTemplates.keptLightBeacon(siteLoc, beaconTint(siteId));
            placed++;
        }
        return placed;
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
        } else if ("carve_anchor".equals(type)) {
            placeCarveWall(base);
        } else if ("soul_gallery".equals(id)) {
            placeSoulGallery(base);
        } else if ("herd_anchor".equals(id)) {
            placeHerdAnchor(base);
        } else if ("the_far_water".equals(id)) {
            placeFarWater(base);
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
            lectern.getInventory().setItem(0, book);
            lectern.update(true, false);
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
            lectern.getInventory().setItem(0, book);
            lectern.update(true, false);
        }
    }

    private void placeAnswerSign(Location base) {
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
        Block fire = world.getBlockAt(x, y, z);
        fire.setType(Material.CAMPFIRE, false);
        if (fire.getBlockData() instanceof org.bukkit.block.data.type.Campfire c) {
            c.setLit(true);
            fire.setBlockData(c, false);
        }
        world.getBlockAt(x + 1, y, z).setType(Material.LANTERN, false);
    }

    private void placeSellaPool(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.PRISMARINE_BRICKS, false);
                world.getBlockAt(x + dx, y, z + dz).setType(Material.WATER, false);
            }
        }
        world.getBlockAt(x, y, z - 2).setType(Material.DARK_PRISMARINE, false);
    }

    private void placeSellaAnchor(Location base) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
        world.getBlockAt(x, y, z).setType(Material.DARK_PRISMARINE, false);
        world.getBlockAt(x, y - 1, z + 1).setType(Material.WATER, false);
        world.getBlockAt(x, y - 1, z + 2).setType(Material.WATER, false);
    }

    private void placeChest(Location base) {
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
        for (int dy = 0; dy <= 4; dy++) world.getBlockAt(x, y + dy, z).setType(Material.DEEPSLATE_BRICKS, false);
        world.getBlockAt(x, y + 5, z).setType(Material.BELL, false);
        world.getBlockAt(x + 1, y, z).setType(Material.CAMPFIRE, false);
    }

    private void placeCoopPlate(Location base, String id) {
        org.bukkit.World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
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
        placeSellaPool(base);
        org.bukkit.World world = base.getWorld();
        if (world != null) {
            world.getBlockAt(base.getBlockX() + 2, base.getBlockY(), base.getBlockZ()).setType(Material.SEAGRASS, false);
        }
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
                payload = "{\"mode\":\"title\",\"title\":\"THE OBSERVANCE\",\"subtitle\":\"something is listening\",\"fade_in\":10,\"stay\":45,\"fade_out\":20}";
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
        sender.sendMessage("/obs test title [player]      - title/subtitle pressure");
        sender.sendMessage("/obs test sound [player]      - resource-pack whisper");
        sender.sendMessage("/obs test voice [player]      - spatial Keeper voice");
        sender.sendMessage("/obs test darkness [player]   - short darkness effect");
        sender.sendMessage("/obs test name [player]       - name appears on a wall");
        sender.sendMessage("/obs test reflection [player] - water-rune reflection");
        sender.sendMessage("/obs test mob [player]        - watcher body behind/near player");
        sender.sendMessage("/obs test stalker [player]    - humanlike danger scare sequence");
        sender.sendMessage("/obs test hunt [player]       - multi-figure pursuit scare sequence");
        sender.sendMessage("/obs test elsewhere [player]  - wrong-place sky/fog scare sequence");
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
            plugin.scheduler().runLaterSafe("command.test.stalker.title", 20L,
                    () -> runTestBeat(engine, target, testSite, "stalker-title", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"title\",\"title\":\"DON'T TURN\",\"subtitle\":\"someone is standing where you were\",\"fade_in\":0,\"stay\":35,\"fade_out\":20}"));
            plugin.scheduler().runLaterSafe("command.test.stalker.figure", 35L,
                    () -> runTestBeat(engine, target, testSite, "stalker-figure", "named_mob",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"entity\":\"WITHER_SKELETON\",\"fallback_entity\":\"STRAY\",\"name\":\"\",\"distance\":8,\"silent\":true,\"no_ai_drift\":true,\"invulnerable\":true,\"glowing\":false,\"despawn_seconds\":20,\"name_visible\":false}"));
        }
        sender.sendMessage("Observance test: stalker -> " + target.getName()
                + " -> darkness, close sound, warning title, and a tall silent figure queued.");
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
            plugin.scheduler().runLaterSafe("command.test.hunt.title", 18L,
                    () -> runTestBeat(engine, target, testSite, "hunt-title", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"title\",\"title\":\"RUN\",\"subtitle\":\"do not look for the first one\",\"fade_in\":0,\"stay\":30,\"fade_out\":15}"));
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
            plugin.scheduler().runLaterSafe("command.test.elsewhere.title", 18L,
                    () -> runTestBeat(engine, target, testSite, "elsewhere-title", "private_message",
                            com.observance.watcher.beats.BeatCategory.DIRECTED,
                            "{\"mode\":\"title\",\"title\":\"THIS IS NOT HERE\",\"subtitle\":\"the sky is wearing the wrong room\",\"fade_in\":0,\"stay\":38,\"fade_out\":20}"));
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
        sender.sendMessage(" Repair: /obs prepworld for a compact playable pass, or /obs site set <id> + /obs placeworld for curated placement.");
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
        if ("answer_sign".equals(type) && !hasSignNear(loc, Math.max(1, site.radius()))) {
            placeAnswerSign(loc);
            return true;
        }
        if ("vaun_bookshelf".equals(type) && block.getType() != Material.CHISELED_BOOKSHELF) {
            block.setType(Material.CHISELED_BOOKSHELF, false);
            return true;
        }
        if ("vaun_hoard_chest".equals(type)
                && block.getType() != Material.CHEST
                && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.BARREL) {
            placeChest(loc);
            return true;
        }
        if (isTemplateLabSite(site.id()) && block.getType() == Material.AIR) {
            return StructureTemplates.keeper(site.id(), loc) != null;
        }
        if ("first_marker_01".equals(site.id()) && block.getType() == Material.AIR) {
            placeMarker(loc, Material.CHISELED_STONE_BRICKS, Material.CANDLE, true);
            return true;
        }
        return false;
    }

    private String auditPlacedSite(Site site, Location loc) {
        Block block = loc.getBlock();
        String type = site.type();
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
        if ("vaun_hoard_chest".equals(type)
                && block.getType() != Material.CHEST
                && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.BARREL) {
            return "FAIL " + site.id() + ": expected chest/barrel hardware, found " + block.getType() + ".";
        }
        if ("answer_sign".equals(type) && !hasSignNear(loc, Math.max(1, site.radius()))) {
            return "FAIL " + site.id() + ": no sign found inside answer radius.";
        }
        if (isCoreAuditSite(site.id()) && block.getType() == Material.AIR) {
            return "FAIL " + site.id() + ": anchor block is air.";
        }
        return null;
    }

    private boolean hasSignNear(Location loc, int radius) {
        if (loc == null || loc.getWorld() == null) return false;
        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        int r = Math.max(1, Math.min(8, radius));
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

    private static boolean isCoreAuditSite(String id) {
        if (id == null) return false;
        return switch (id) {
            case "first_report_lectern_01", "first_marker_01", "rune_rosetta",
                 "stone_vaun", "stone_mara", "stone_sella", "stone_orin", "stone_brann", "stone_iss",
                 "stone_of_reckoning", "the_cold_hearth", "unbroken_light", "the_threshold",
                 "the_unwriting", "threshold_vault",
                 "mara_lectern_1", "mara_lectern_2", "mara_lectern_3", "mara_lectern_4", "mara_lectern_5" -> true;
            default -> false;
        };
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
            for (String s : new String[]{"status", "audit", "repair", "reload", "sleep", "flag", "site", "placeworld", "placeroom", "placeregion", "placedeep", "placelecterns", "placelab", "fullrun", "prepworld", "placeprologue", "lens", "wren", "keeper", "townsfolk", "test", "needle", "finale", "reading"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("site")) {
            if ("set".startsWith(args[1].toLowerCase(Locale.ROOT))) out.add("set");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("site") && args[1].equalsIgnoreCase("set")) {
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
            for (String s : new String[]{"menu", "whisper", "title", "sound", "voice", "darkness", "name", "reflection", "mob", "stalker", "hunt", "elsewhere", "torch", "decay", "drift", "particles", "toast", "sign", "needle"}) {
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
