package com.observance.watcher.command;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.Site;
import com.observance.watcher.structure.StructureTemplates;
import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
            case "placeroom" -> handlePlaceRoom(sender, args);
            case "placeregion" -> handlePlaceRegion(sender, args);
            case "placedeep" -> handlePlaceDeep(sender, args);
            case "lens" -> handleLens(sender, args);
            case "wren" -> handleWren(sender, args);
            default -> sender.sendMessage("Unknown subcommand. Use: status | reload | sleep <on|off> | flag <set|clear|list> | placeroom <keeperId> | placeregion | placedeep | lens give [player] | wren <spawn|despawn|reckoning>");
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
                6, 6, false, true, null);
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
                    radius, 6, true, true, null);
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
     * {@code /observance lens give [player]} — hands out {@link com.observance.watcher.lens.LensItem
     * the Lens} relic (INTEGRATION §SIGNATURE #3 "second sight"). With no player argument, gives it to
     * the sender (must be a player). Refreshes the recipient's gated-rune visibility immediately so a
     * rune placed before they held the Lens appears the moment it lands in an empty hand.
     */
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
     * Place the three reckoning markers in a short east-west row at the player's feet, each an
     * invulnerable, gravity-less armor stand named for its choice and tagged with the
     * {@code observance:wren_reckoning} PDC value the {@code WrenNpcListener} reads. A right-click on
     * one, after {@code companion_revealed}, sets exactly one of the reckoning flags, once.
     */
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
            for (String s : new String[]{"status", "reload", "sleep", "flag", "placeroom", "placeregion", "placedeep", "lens", "wren"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
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
