package com.observance.watcher.structure;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.Locale;

/**
 * Code-templated in-world set-pieces — the procedural-craft layer of the "director places structures, not
 * the human" build system (D7w / CHANGE-MANIFEST A11). No schematic files: each keeper gets a distinct,
 * hand-crafted method that stamps a small dense set-piece from the FULL 1.21 block palette, so the seven
 * keeper places are instantly distinguishable and none is a bare "deepslate pillar + oak sign".
 *
 * <p><b>Art direction (WORLD-BIBLE §1, CHANGE-MANIFEST A11 craft levers), enforced everywhere:</b>
 * <ul>
 *   <li><b>Carved-never-default</b> — polished/chiseled/cracked/tuff/deepslate/blackstone, not plain stone.</li>
 *   <li><b>Dark-default, earned light</b> — sparse candles / soul lanterns / amethyst / a single lantern.</li>
 *   <li><b>Per-keeper signature palette + prop + lighting</b> so each place reads at a glance.</li>
 *   <li><b>Wrongness</b> — one or two deliberately-off touches per site (a cobweb, a crack, a wrong-facing
 *       marker).</li>
 * </ul>
 *
 * <p><b>MAIN THREAD ONLY</b> — every method touches Bukkit block state directly. Callers must already be on
 * the server thread (the admin command runs synchronously in the command handler).
 *
 * <p><b>The answer surface.</b> Every set-piece places at least ONE editable (UNWAXED) sign OR a lectern
 * inside the site radius, integrated diegetically (recessed in carved stone, hung on chains, a reading
 * lectern). {@code AnswerSignListener} fires on a {@code SignChangeEvent} for any sign in the radius, so the
 * player overwrites that sign to submit — meaning the answer surface must be left blank / clearly a
 * submission slot, and any *label* text lives on a SEPARATE waxed sign so it never pollutes an answer. The
 * builders return the {@link Location} of that answer surface.
 *
 * <p>Deliberately unprotected: these are permanent admin-placed fixtures, not fragile beat illusions.
 */
public final class StructureTemplates {

    private StructureTemplates() { }

    /* ================================================================================================
     * VERSION-SAFE MATERIAL RESOLVERS. The deep sites lean on a few 1.20.5+/1.21 blocks (VAULT, COPPER_BULB,
     * copper/deepslate wall-signs). Resolve them by name at call time with a graceful fallback so the plugin
     * still builds + runs if a name is missing on the running server (a bad name never NPEs a build). Cached
     * so the lookup is one-time per key.
     * ============================================================================================== */
    private static final java.util.Map<String, Material> MAT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** First existing material among {@code names}, else {@code fallback}. Cached by the first name tried. */
    private static Material matOr(Material fallback, String... names) {
        String key = names.length > 0 ? names[0] : String.valueOf(fallback);
        return MAT_CACHE.computeIfAbsent(key, k -> {
            for (String n : names) {
                try {
                    Material m = Material.matchMaterial(n);
                    if (m != null) return m;
                } catch (Throwable ignored) { }
            }
            return fallback;
        });
    }

    private static Material deepslateWallSign() { return matOr(Material.DARK_OAK_WALL_SIGN, "DEEPSLATE_WALL_SIGN", "DARK_OAK_WALL_SIGN"); }
    private static Material copperWallSign()    { return matOr(Material.DARK_OAK_WALL_SIGN, "COPPER_WALL_SIGN", "OXIDIZED_COPPER_WALL_SIGN", "DARK_OAK_WALL_SIGN"); }
    private static Material vaultBlock()        { return matOr(Material.CHISELED_TUFF, "VAULT", "TRIAL_SPAWNER", "REINFORCED_DEEPSLATE", "CHISELED_TUFF"); }
    private static Material copperBulbLit()     { return matOr(Material.SHROOMLIGHT, "COPPER_BULB", "SHROOMLIGHT"); }
    private static Material copperBulbDead()    { return matOr(Material.WEATHERED_CUT_COPPER, "OXIDIZED_COPPER_BULB", "WEATHERED_COPPER_BULB", "COPPER_BULB", "WEATHERED_CUT_COPPER"); }

    /* ================================================================================================
     * PUBLIC DISPATCH
     * ============================================================================================== */

    /**
     * Build the distinct set-piece for {@code keeperId} at {@code base}. Dispatches to the per-keeper
     * builder; unknown ids fall back to the generic keeper stone. Returns the answer-surface location
     * (the block a player edits to submit), or {@code null} if the world/chunk is unavailable.
     */
    public static Location keeper(String keeperId, Location base) {
        if (base == null) return null;
        World world = base.getWorld();
        if (world == null) return null;
        if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) return null;

        String id = keeperId == null ? "" : keeperId.toLowerCase(Locale.ROOT).trim();
        // Accept both bare ("vaun") and slug ("stone_vaun" / "keeper_vaun") forms.
        if (id.startsWith("keeper_")) id = id.substring("keeper_".length());
        if (id.startsWith("stone_")) id = id.substring("stone_".length());

        Pen pen = new Pen(world);
        return switch (id) {
            case "rosetta", "rune_rosetta", "rune" -> rosetta(pen, base);
            case "vaun"   -> vaun(pen, base);
            case "mara"   -> mara(pen, base);
            case "sella"  -> sella(pen, base);
            case "orin"   -> orin(pen, base);
            case "brann"  -> brann(pen, base);
            case "iss"    -> iss(pen, base);
            // --- THE DEEP-HALF SET-PIECES (the payoff sites the descent leads to) ---
            case "of_reckoning", "reckoning", "stone_of_reckoning" -> reckoning(pen, base);
            case "cold_hearth", "the_cold_hearth"                  -> coldHearth(pen, base);
            case "unbroken_light", "accepting", "undercroft"       -> unbrokenLight(pen, base);
            case "threshold", "the_threshold"                      -> threshold(pen, base);
            case "unwriting", "the_unwriting", "seventh"           -> unwriting(pen, base);
            case "threshold_vault", "vault"                        -> thresholdVault(pen, base);
            // --- THE TWO DEEPENING LANES (real Nether + End; approach A) ---
            case "nether_forge", "forge"                           -> netherForge(pen, base);
            case "end_seventh_shrine", "seventh_shrine", "end_shrine" -> endShrine(pen, base);
            default        -> keeperStone(base); // generic fallback
        };
    }

    /**
     * Back-compat generic "keeper stone": a small carved shrine with a diegetic recessed answer sign.
     * Retained so any old call site keeps working; the per-keeper builders above are the real content.
     */
    public static Location keeperStone(Location base) {
        if (base == null) return null;
        World world = base.getWorld();
        if (world == null) return null;
        if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) return null;
        Pen pen = new Pen(world);
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();

        // Seat it INTO the ground: a shallow 3x3 cobbled-deepslate hollow (WORLD-BUILD §4 palette +
        // "dark is the default") so the shrine sits carved into the earth, not as a bright pillar on grass.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pen.set(bx + dx, by - 1, bz + dz, Material.COBBLED_DEEPSLATE);   // hollow floor
            }
        }
        // A carved plinth of tuff/deepslate with a chiseled cap, rising from the hollow.
        pen.set(bx, by, bz, Material.TUFF_BRICKS);
        pen.set(bx, by + 1, bz, Material.POLISHED_DEEPSLATE);
        pen.set(bx, by + 2, bz, Material.CHISELED_DEEPSLATE);
        // One guttering candle — the single earned light (sparse, not a bright cap).
        pen.candle(bx, by + 3, bz, Material.CANDLE, true);

        // Answer sign recessed on the south face at reading height (blank submission slot).
        Location answer = pen.wallSign(bx, by + 2, bz + 1, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN);
        return answer;
    }

    /* ================================================================================================
     * KEPT-LIGHT BEACON — the landmark beam for the two canonically-lit sites (Site#beacon()).
     * ------------------------------------------------------------------------------------------------
     * FICTION: "one light, somewhere below, did not go out." A KEPT LIGHT — Brann's watch-fire that was
     * never doused, or the one fire of the unbroken_light — is meant to be seen from far off across the
     * black, so a scattered world is never lost (Dark-Souls legible geography). The beam is diegetically
     * the kept light itself, NEVER a game waypoint marker.
     * TRIGGER: stamped by {@code /observance placeworld} / {@code placeroom} / {@code placedeep} ONLY for a
     * site whose sites.yml carries {@code visual_beacon: true} (read via {@link Site#beacon()}). Every other
     * site stays dark — this is not a beam on every marker.
     * INTERACTION: a real vanilla beacon on the minimum 3x3 mineral base, capped with firelight-tinted glass
     * ({@code tint}: e.g. ORANGE_STAINED_GLASS for Brann's watch-fire). The beam projects only with sky
     * access; when the sky is blocked (a deep/roofed site) we still leave the base + a real light source so
     * the kept light reads on the ground, and return {@code false} so the caller can note it. Never throws.
     *
     * @param base    the site anchor (the set-piece's ground cell); the beacon rises a few courses above it
     *                so it clears the set-piece and reads as the light at the site's top.
     * @param tint    stained-glass material tinting the beam (firelight); null → no tint (plain white beam).
     * @return true if the beam has clear sky above (it will project); false if the sky is blocked (base +
     *         light still placed so the kept light reads; caller may log the note).
     */
    public static boolean keptLightBeacon(Location base, Material tint) {
        if (base == null) return false;
        World world = base.getWorld();
        if (world == null) return false;
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        if (!world.isChunkLoaded(bx >> 4, bz >> 4)) return false;

        Pen pen = new Pen(world);
        // Raise the beacon above the set-piece: the tallest keeper caps around by+4, so seat the mineral base
        // at by+5 and the beacon on top. This lifts the kept light to the site's top, clear of the structure.
        int baseY = by + 5;
        // The minimum pyramid: a single 3x3 iron tier directly under the beacon (one tier = a projecting beam).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pen.set(bx + dx, baseY, bz + dz, Material.IRON_BLOCK);
            }
        }
        int beaconY = baseY + 1;
        pen.set(bx, beaconY, bz, Material.BEACON);
        // Firelight tint one course above the beacon (the glass the beam passes through takes its colour).
        if (tint != null) pen.set(bx, beaconY + 1, bz, tint);

        // Sky-access probe: a beacon only projects with an unobstructed column to the world height. Walk up
        // from just above the beacon; any non-passable block blocks the beam. Passable = air / glass tint /
        // transparent decor. Conservative: on any doubt we treat the column as blocked (return false).
        boolean skyClear = true;
        try {
            int top = world.getMaxHeight();
            for (int y = beaconY + 1; y < top; y++) {
                Material m = world.getBlockAt(bx, y, bz).getType();
                if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR) continue;
                if (tint != null && y == beaconY + 1 && m == tint) continue;   // our own tint glass is beam-transparent
                if (isBeamTransparent(m)) continue;
                skyClear = false;
                break;
            }
        } catch (Throwable ignored) {
            skyClear = false;   // couldn't verify → treat as blocked (place the ground light below either way)
        }

        // Graceful when the sky is blocked (a deep/roofed site): the beam won't project, so drop a real light
        // source ON the tint cap so the kept light still reads on the ground — never a crash, never darkness.
        if (!skyClear) {
            pen.setIfAir(bx, beaconY + 2, bz, Material.SHROOMLIGHT);
        }
        return skyClear;
    }

    /** Glass/leaves/thin blocks a beacon beam passes through (so a stained-glass roof doesn't count as blocking). */
    private static boolean isBeamTransparent(Material m) {
        if (m == null) return false;
        String n = m.name();
        // NB: TINTED_GLASS is deliberately EXCLUDED — vanilla tinted glass blocks the beacon beam (and light),
        // unlike normal/stained glass which it passes through. The firelight tint we cap the beacon with is
        // stained glass (ORANGE/WHITE), so it is beam-transparent; a tinted-glass roof would correctly block.
        return n.endsWith("_STAINED_GLASS") || n.endsWith("_STAINED_GLASS_PANE")
                || n.equals("GLASS") || n.equals("GLASS_PANE")
                || n.endsWith("_LEAVES") || n.equals("BEACON");
    }

    /* ================================================================================================
     * ROSETTA — the literacy gate. A ring of 6 short inscribed polished-blackstone pillars around a
     * central lectern on a sculk-veined deepslate-brick dais; an amethyst cluster at the heart (the
     * learning-light). Answer = the central lectern.
     * Palette: polished blackstone + deepslate brick. Prop: 6 way-mark pillars w/ hanging signs. Light:
     * amethyst (earned, cool). Wrongness: one pillar's hanging sign faces inward/blank + a cobweb.
     * ============================================================================================== */
    private static Location rosetta(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: this is now a full underground learning chamber, not a 7x7 pad.
        // The dense original ring below remains the functional core; this outer shell gives it scale,
        // approach, group space, and a darker onward route.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                int m = Math.max(Math.abs(dx), Math.abs(dz));
                boolean rim = m == 8 || (Math.abs(dx) == 7 && Math.abs(dz) >= 5)
                        || (Math.abs(dz) == 7 && Math.abs(dx) >= 5);
                Material floor = rim ? Material.POLISHED_BLACKSTONE_BRICKS
                        : ((Math.abs(dx) <= 4 && Math.abs(dz) <= 4) ? Material.DEEPSLATE_BRICKS : Material.DEEPSLATE_TILES);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 3; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 3 ? Material.BLACKSTONE
                                        : (dy == 1 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS));
                    }
                }
            }
        }
        for (int[] rib : new int[][]{{-8, 0}, {8, 0}, {0, -8}, {0, 8}, {-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            int px = cx + rib[0], pz = cz + rib[1];
            for (int dy = 0; dy <= 5; dy++) {
                pen.set(px, cy + dy, pz, dy == 5 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BASALT);
            }
            pen.hangingLantern(px, cy + 4, pz, true);
        }
        for (int dz = -10; dz <= -8; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.POLISHED_DEEPSLATE);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy - 1, cz + 8, Material.POLISHED_DEEPSLATE);
        }

        // 7x7 dais: deepslate-brick floor with sculk-vein veining, a chiseled rim.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
            }
        }
        // Sculk-vein veining on the floor (learning creeping in), a few cells only.
        pen.veinFloor(cx, cy, cz, new int[][]{{-1, 0}, {1, 1}, {0, -2}, {2, -1}});

        // Central dais step + lectern (the answer). Lectern sits on a chiseled block, facing south.
        pen.set(cx, cy, cz, Material.CHISELED_DEEPSLATE);
        Location answer = pen.lectern(cx, cy + 1, cz, BlockFace.SOUTH);
        // Amethyst cluster at the heart — the learning-light — placed on the dais front edge.
        pen.set(cx, cy, cz + 1, Material.BUDDING_AMETHYST);
        pen.clusterOn(cx, cy + 1, cz + 1, BlockFace.UP);

        // Ring of 6 inscribed way-mark pillars around the dais (hex-ish placement on the 7x7).
        // RESHAPE R0: marks are worn/partial carvings — NOT sequential Roman numerals (that reads as a
        // tutorial list). Index 4 (the abraded pillar) uses a cracked block + a subtly different mark
        // so its content differs; the "wrong" outward-facing pillar is now the abraded one (i==4) so it
        // is physically distinct (cracked cap + different mark), not just blank+outward.
        int[][] ring = {{0, -3}, {3, -1}, {3, 2}, {0, 3}, {-3, 2}, {-3, -1}};
        // Worn/partial marks — non-sequential, fragmentary, like worn carvings (not a numbered list).
        String[] marks = {"·", "··", "·", "···", "·̃", "··"};
        for (int i = 0; i < ring.length; i++) {
            int px = cx + ring[i][0], pz = cz + ring[i][1];
            pen.set(px, cy, pz, Material.POLISHED_BLACKSTONE_BRICKS);
            pen.set(px, cy + 1, pz, Material.POLISHED_BLACKSTONE);
            // Abraded pillar (index 4): cracked cap — physically distinct, content subtly differs.
            boolean abraded = i == 4;
            pen.set(px, cy + 2, pz, abraded ? Material.CRACKED_POLISHED_BLACKSTONE_BRICKS
                    : Material.CHISELED_POLISHED_BLACKSTONE);
            // A hanging sign on chains under a small overhang block, facing the centre (a way-mark).
            pen.set(px, cy + 4, pz, Material.POLISHED_BLACKSTONE_SLAB);
            BlockFace toCentre = pen.toward(px, pz, cx, cz);
            // Abraded pillar faces OUT (its face is turned away, the mark worn) — the wrongness detail.
            pen.hangingSign(px, cy + 3, pz, abraded ? toCentre.getOppositeFace() : toCentre,
                    new String[]{"·", marks[i], "·", ""});
        }
        // Wrongness: a cobweb clinging in one corner of the dais.
        pen.setIfAir(cx - 3, cy, cz - 3, Material.COBWEB);

        // RESHAPE R0: label REDUCED — cut "read, then answer" (English instruction); title only.
        // The teaching happens through the crib-pair signs below; the title stays spare so this does not
        // read like an English tutorial board.
        pen.labelWallSign(cx, cy, cz + 3, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"the rosetta", "", "", ""});

        // Earned-literacy key made concrete: three rune/plaintext
        // crib PAIRS on the clear way-mark pillars — the same word in runes over its plain letters — so the
        // rosetta actually TEACHES the alphabet (the "oh, these are letters" turn), show-not-tell. Placed on
        // the outward pillar face at eye level, physics-free so they persist. Not the abraded pillar (i==4).
        String[] cribWords = {"KEPT", "STONE", "NAME"};
        int[] cribPillars = {0, 1, 2};
        for (int c = 0; c < cribWords.length; c++) {
            int[] rp = ring[cribPillars[c]];
            int px = cx + rp[0], pz = cz + rp[1];
            BlockFace out = pen.toward(px, pz, cx, cz).getOppositeFace();
            pen.runeCribPair(px + out.getModX(), cy + 1, pz + out.getModZ(), out,
                    Material.DARK_OAK_WALL_SIGN, cribWords[c]);
        }
        return answer;
    }

    /* ================================================================================================
     * VAUN (the hoarder / founder who kept everyone's lamps). Crammed barrels + (trapped)chests +
     * decorated pots w/ sherds + oxidized copper (wealth rotting) + cobwebs; a carved ledger-stone as
     * the answer surface; one dim lantern.
     * Palette: oxidized/exposed copper + spruce + tuff. Prop: hoard (barrels/chests/pots). Light: one
     * dim lantern. Wrongness: cobwebs over the hoard + a cracked pot.
     * ============================================================================================== */
    private static Location vaun(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: treasury chamber around the original hoard core.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 8 || dz == -6 || dz == 7;
                boolean aisle = Math.abs(dx) <= 2 && dz >= 0;
                Material floor = rim ? Material.POLISHED_TUFF
                        : aisle ? Material.TUFF_BRICKS
                        : ((dx + dz) % 3 == 0 ? Material.TUFF : Material.TUFF_BRICKS);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        Material wall = dy == 4 ? Material.TUFF_BRICKS
                                : ((Math.abs(dx) + dz + dy) % 4 == 0 ? Material.OXIDIZED_COPPER : Material.POLISHED_TUFF);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        for (int x = -6; x <= 6; x += 3) {
            pen.set(cx + x, cy, cz - 5, Material.BARREL);
            pen.set(cx + x, cy + 1, cz - 5, Material.BARREL);
            pen.set(cx + x, cy + 2, cz - 5, Material.POLISHED_TUFF);
        }
        for (int x = -5; x <= 5; x += 5) {
            pen.set(cx + x, cy + 4, cz + 3, Material.SPRUCE_PLANKS);
            pen.hangingLantern(cx + x, cy + 3, cz + 3, false);
        }

        // A cramped 5x5 alcove floor: tuff bricks, copper-oxide corners (wealth gone green).
        pen.floor(cx, cy - 1, cz, 2, Material.TUFF_BRICKS);
        pen.set(cx - 2, cy - 1, cz - 2, Material.OXIDIZED_COPPER);
        pen.set(cx + 2, cy - 1, cz + 2, Material.OXIDIZED_CUT_COPPER);
        // Back wall of exposed copper + tuff — the treasury wall, corroding.
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy,     cz - 2, Material.EXPOSED_CUT_COPPER);
            pen.set(cx + dx, cy + 1, cz - 2, Material.WEATHERED_CUT_COPPER);
        }

        // The hoard, crammed against the back and sides.
        pen.set(cx - 2, cy, cz - 1, Material.BARREL);
        pen.set(cx - 2, cy + 1, cz - 1, Material.BARREL);        // stacked barrels
        pen.set(cx - 1, cy, cz - 1, Material.CHEST);
        pen.set(cx + 1, cy, cz - 1, Material.TRAPPED_CHEST);      // a trapped chest among them
        pen.set(cx + 2, cy, cz - 1, Material.BARREL);
        // Decorated pots with sherds (the salvaged relics of a life).
        pen.decoratedPot(cx - 2, cy, cz + 1, "MOURNER", "HEART", "PLENTY", "DANGER");
        pen.decoratedPot(cx + 2, cy, cz + 1, "ARMS_UP", "SKULL", "PRIZE", "BURN");
        // A cracked pot on the floor — the wrongness: the hoard is failing.
        pen.set(cx, cy, cz + 2, Material.DECORATED_POT); // plain (no sherds) reads as a chipped pot
        pen.set(cx + 1, cy, cz + 2, Material.CRACKED_STONE_BRICKS);

        // One dim lantern hung from the ceiling beam — the only light.
        pen.set(cx, cy + 3, cz, Material.SPRUCE_PLANKS);
        pen.hangingLantern(cx, cy + 2, cz, false);

        // Cobwebs draped over the hoard (wrongness — long undisturbed).
        pen.setIfAir(cx - 2, cy + 1, cz + 1, Material.COBWEB);
        pen.setIfAir(cx + 2, cy + 1, cz - 1, Material.COBWEB);

        // The carved ledger-stone: a chiseled block with a recessed answer sign (blank submission slot).
        pen.set(cx, cy, cz - 1, Material.CHISELED_TUFF_BRICKS);
        pen.set(cx, cy + 1, cz - 1, Material.CHISELED_TUFF);
        Location answer = pen.wallSign(cx, cy + 1, cz, BlockFace.SOUTH, Material.SPRUCE_WALL_SIGN);
        // RESHAPE R0: label CUT. FOLD — the ledger line is moved into a partly-worn carving on the
        // chiseled ledger stone itself. The final line is left blank (visible erasure — the tally that
        // was never completed, not an empty "fill-me-in" box). The carving is on the adjacent face of
        // the ledger-stone so it reads as worn into the stone, not as a posted notice.
        pen.labelWallSign(cx - 1, cy + 1, cz - 1, BlockFace.EAST, Material.SPRUCE_WALL_SIGN,
                new String[]{"all of it kept", "none of it", "spent —", ""});

        // RUNE-CRIB "KEPT" — on the hoard barrels (the thing Vaun did: kept it all). Mounted on the SOUTH face
        // of the stacked barrels at (cx-2,cy+1,cz-1), facing the room. crib: the referent is the barrel-hoard
        // directly north of this sign.
        pen.runeCrib(cx - 2, cy + 1, cz, BlockFace.SOUTH, Material.SPRUCE_WALL_SIGN, "KEPT");
        // RUNE-CRIB "DEEP" — placed here (Vaun has no descent-mouth) on the corroding copper back wall, per the
        // task's fallback. Mounted on the SOUTH face of the weathered-cut-copper back wall at (cx+1,cy+1,cz-2),
        // facing the room. crib: the referent is the treasury back wall (the hoard walled deep behind copper).
        pen.runeCrib(cx + 1, cy + 1, cz - 1, BlockFace.SOUTH, Material.SPRUCE_WALL_SIGN, "DEEP");
        // GIVE — OMITTED at Vaun: this site is a hoard (barrels/chests/pots), with NO offering/deposit area.
        // Vaun kept everything and gave nothing, so there is no diegetic give-surface here to label (the only
        // deposit-slot in these methods is the Seventh's empty offering-slot in unwriting(), a different site).
        return answer;
    }

    /* ================================================================================================
     * MARA (the reader who never walked / the lampwright). Chiseled bookshelves (some empty), lecterns
     * with books, a chair (stairs+trapdoor), reading-candles, heavy cobwebs/dust; answer = a lectern-
     * book + a "margin-note" sign.
     * Palette: chiseled bookshelf + dark oak + deepslate tile. Prop: reading nook + chair. Light:
     * reading candles. Wrongness: gaps in the shelves + heavy cobweb.
     * ============================================================================================== */
    private static Location mara(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: reading hall around the original study core.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 6; dz++) {
                boolean rim = Math.abs(dx) == 7 || dz == -7 || dz == 6;
                boolean readingAisle = Math.abs(dx) <= 2 && dz >= -1;
                Material floor = readingAisle ? Material.DEEPSLATE_TILES
                        : ((dx + dz) % 4 == 0 ? Material.DARK_OAK_PLANKS : Material.POLISHED_DEEPSLATE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        Material wall = (dy == 1 && Math.abs(dx) == 7 && dz % 2 == 0)
                                ? Material.CHISELED_BOOKSHELF
                                : (dy == 4 ? Material.DARK_OAK_PLANKS : Material.DEEPSLATE_TILES);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        for (int z = -5; z <= 3; z += 4) {
            pen.set(cx - 5, cy + 4, cz + z, Material.DARK_OAK_PLANKS);
            pen.set(cx + 5, cy + 4, cz + z, Material.DARK_OAK_PLANKS);
            pen.hangingLantern(cx - 5, cy + 3, cz + z, false);
            pen.hangingLantern(cx + 5, cy + 3, cz + z, false);
        }

        // 5x5 study floor: deepslate tile, dark oak trim.
        pen.floor(cx, cy - 1, cz, 2, Material.DEEPSLATE_TILES);
        // Bookshelf back wall (chiseled bookshelves), with two deliberately EMPTY gaps.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                boolean gap = (dx == -1 && dy == 1) || (dx == 2 && dy == 2); // missing volumes
                pen.set(cx + dx, cy + dy, cz - 2, gap ? Material.DARK_OAK_PLANKS : Material.CHISELED_BOOKSHELF);
            }
        }
        // Side shelves.
        for (int dz = -1; dz <= 1; dz++) {
            pen.set(cx - 2, cy, cz + dz, Material.BOOKSHELF);
            pen.set(cx - 2, cy + 1, cz + dz, Material.CHISELED_BOOKSHELF);
        }

        // The reading chair: dark-oak stairs (seat) + a trapdoor backrest.
        pen.stairs(cx + 1, cy, cz + 1, Material.DARK_OAK_STAIRS, BlockFace.NORTH, false);
        pen.trapdoorBack(cx + 1, cy + 1, cz, BlockFace.NORTH);

        // The reading lectern (THE ANSWER) — the margin-note framing now lives IN the book text (RESHAPE R0
        // fold: the margin note belongs on the diegetic surface, not a posted label). Book updated to carry
        // both the text and "her hand" attribution as a genuine margin note in the prose.
        Location answer = pen.lectern(cx, cy, cz + 1, BlockFace.NORTH);
        pen.putBook(cx, cy, cz + 1, "the lampwright's hand",
                "i can't keep them all lit —\nthe words stay when the\nlamps do not.\n\n" +
                        "[in the margin, her hand:]\n\"read it back to me —\"");

        // Reading candles on a small deepslate-tile side table — sparse, warm.
        pen.set(cx - 1, cy, cz + 1, Material.DEEPSLATE_TILE_SLAB);
        pen.candle(cx - 1, cy + 1, cz + 1, Material.CANDLE, true);

        // RUNE-CRIB "LIGHT" — beside the reading candle (the lampwright's earned light). Mounted facing EAST on
        // the west side-shelf block (cx-2,cy+1,cz), at (cx-1,cy+1,cz) — immediately beside the candle at
        // (cx-1,cy+1,cz+1). crib: the referent is the reading candle one cell south of this sign.
        pen.runeCrib(cx - 1, cy + 1, cz, BlockFace.EAST, Material.DARK_OAK_WALL_SIGN, "LIGHT");

        // A second lectern with no book (a place someone stopped reading). RESHAPE R0 fold: the submission
        // surface IS this empty lectern + the bookshelf gap — "return the missing volume" reads as the
        // diegetic action, not a blank "fill-me-in" box. The label sign is CUT (margin-note now lives in
        // the first lectern's book text above). The gap in the bookshelf (dx==-1, dy==1) is the answer's
        // physical shape: a volume that belongs there and isn't.
        pen.lectern(cx + 2, cy, cz - 1, BlockFace.WEST);

        // Heavy cobwebs/dust — the wrongness (a room read in, never left).
        pen.setIfAir(cx - 2, cy + 2, cz - 2, Material.COBWEB);
        pen.setIfAir(cx + 2, cy + 2, cz + 1, Material.COBWEB);
        pen.setIfAir(cx, cy + 2, cz, Material.COBWEB);

        // RESHAPE R0: margin-note label sign CUT — framing folded into the lectern book text above.
        return answer;
    }

    /* ================================================================================================
     * SELLA (the drowned child). Set AT/BY water: prismarine + dark-prismarine, dimmed sea-lanterns, a
     * child's cairn (cobblestone + poppies/lily-of-the-valley), a copybook lectern, kelp/seagrass, a
     * still reflecting pool; answer = a slab readable in the water + a "far marker" sign.
     * Palette: prismarine + dark prismarine. Prop: cairn + reflecting pool. Light: dim sea lanterns.
     * Wrongness: a single poppy floating / a drowned copybook.
     * ============================================================================================== */
    private static Location sella(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: drowned cistern chamber around the original reflecting pool.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                boolean rim = Math.abs(dx) == 8 || Math.abs(dz) == 8;
                boolean channel = Math.abs(dx) <= 2 || Math.abs(dz) <= 2;
                Material floor = channel ? Material.PRISMARINE_BRICKS
                        : ((Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? Material.DARK_PRISMARINE : Material.PRISMARINE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        Material wall = dy == 4 ? Material.DARK_PRISMARINE
                                : ((dx + dz + dy) % 4 == 0 ? Material.SEA_LANTERN : Material.PRISMARINE_BRICKS);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        for (int z = -6; z <= 6; z += 6) {
            pen.water(cx - 5, cy, cz + z);
            pen.water(cx + 5, cy, cz + z);
            pen.set(cx - 5, cy - 1, cz + z, Material.SEA_LANTERN);
            pen.set(cx + 5, cy - 1, cz + z, Material.SEA_LANTERN);
        }

        // 5x5 prismarine surround with a recessed 3x3 reflecting pool at the centre.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                pen.set(cx + dx, cy - 1, cz + dz, edge ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS);
            }
        }
        // Carve the pool one deep and fill with water (still reflecting pool).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.PRISMARINE);
                pen.water(cx + dx, cy, cz + dz);
            }
        }
        // Kelp / seagrass in the pool (a child's water-world).
        pen.waterlogged(cx - 1, cy, cz - 1, Material.SEAGRASS);
        pen.set(cx + 1, cy, cz + 1, Material.KELP_PLANT); pen.set(cx + 1, cy + 1, cz + 1, Material.KELP);

        // Dim sea-lanterns set INTO the pool floor (light from below the water — drowned light).
        pen.set(cx - 1, cy - 1, cz + 1, Material.SEA_LANTERN);
        pen.set(cx + 1, cy - 1, cz - 1, Material.SEA_LANTERN);

        // The child's cairn: a small heap of cobblestone with flowers, off to one side.
        pen.set(cx + 2, cy, cz + 2, Material.MOSSY_COBBLESTONE);
        pen.set(cx + 2, cy + 1, cz + 2, Material.COBBLESTONE);
        pen.set(cx + 1, cy, cz + 2, Material.COBBLESTONE);
        pen.setIfAir(cx + 2, cy + 2, cz + 2, Material.POPPY);
        pen.setIfAir(cx + 1, cy + 1, cz + 2, Material.LILY_OF_THE_VALLEY);
        // Wrongness: a single poppy floating on the far side of the pool.
        pen.setIfAir(cx - 2, cy, cz - 2, Material.POPPY);

        // RUNE-CRIB "NAME" — beside the child's cairn, where a name would be cut. Mounted on the SOUTH face of
        // the cobblestone cairn stone (cx+2,cy+1,cz+2), facing out at head height by the flowers — the carved
        // word for the thing a grave-marker holds. crib: the referent is the flowered cairn directly north.
        pen.runeCrib(cx + 2, cy + 1, cz + 3, BlockFace.SOUTH, Material.WARPED_WALL_SIGN, "NAME");

        // The copybook lectern at the pool's edge (a child practicing letters).
        pen.lectern(cx - 2, cy, cz, BlockFace.EAST);
        pen.putBook(cx - 2, cy, cz, "sella's copybook",
                "i drew the dark before\nthey would look at it.\n\na a a  b b b");

        // THE ANSWER: the "far marker" at the pool's DRY north rim (outside the 3x3 water). A dark-prismarine
        // marker-post rises from the dry north-edge block; the answer sign is on its NORTH face at head height
        // (cy+1), facing OUT away from the pool toward a dry standing spot on solid prismarine — so a player
        // reads/edits it standing on land, never in the water (blank submission slot). Sella's reflecting-pool
        // identity is kept: this is the far marker read across the still water from the dry rim.
        pen.set(cx, cy, cz - 2, Material.DARK_PRISMARINE);        // marker-post base (backing, body height)
        pen.set(cx, cy + 1, cz - 2, Material.DARK_PRISMARINE);    // marker-post: backing behind the answer sign
        pen.set(cx, cy + 2, cz - 2, Material.DARK_PRISMARINE);    // marker-post: backing behind the label
        pen.set(cx, cy - 1, cz - 3, Material.DARK_PRISMARINE);    // dry standing ground north of the pool
        pen.set(cx, cy - 1, cz - 4, Material.DARK_PRISMARINE);    // the dry approach step the reader stands on
        Location answer = pen.wallSign(cx, cy + 1, cz - 3, BlockFace.NORTH, Material.WARPED_WALL_SIGN);

        // RUNE-CRIB "WATER" — at the reflecting pool's dry SOUTH rim, labelling the pool itself. A short dark-
        // prismarine rim-post rises on the south edge (cx,cy,cz+2, dry rim — outside the 3x3 water) and the crib
        // is mounted on its SOUTH face at rim height, read from the dry standing spot south of the pool. Small,
        // carved, easy to miss. crib: the referent is the reflecting pool immediately north of this rim-post.
        pen.set(cx, cy, cz + 2, Material.DARK_PRISMARINE);        // rim-post backing for the water crib
        pen.runeCrib(cx, cy, cz + 3, BlockFace.SOUTH, Material.WARPED_WALL_SIGN, "WATER");

        // RESHAPE R0: label CUT. FOLD — the "far marker" label is replaced with a partly-worn carved name
        // on the marker post. The name is partially legible (some letters worn smooth), which is the
        // diegetic surface: a child's name cut into stone and then worn by water, not a posted sign.
        // The reflecting pool + child's copybook already tell it; the blank label was the only wrong note.
        pen.labelWallSign(cx, cy + 2, cz - 3, BlockFace.NORTH, Material.WARPED_WALL_SIGN,
                new String[]{"s e l  ·  ·", "", "", ""});
        return answer;
    }

    /* ================================================================================================
     * ORIN (won't bow). A threshold with a LOW STONE LINTEL you must SNEAK under (the bow built into the
     * architecture), an unfinished/broken carving, iron bars, cold rigid deepslate; answer = a carved
     * threshold-stone whose sign you must stoop to read.
     * Palette: deepslate brick + stone brick + iron bars. Prop: the low lintel gate. Light: minimal —
     * one soul lantern past the threshold. Wrongness: an unfinished carving + a cracked lintel block.
     * ============================================================================================== */
    private static Location orin(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: mason vestibule and forced-bow sightline around the old lintel.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 6 || dz == -7 || dz == 7;
                boolean centerRun = Math.abs(dx) <= 1;
                Material floor = centerRun ? Material.POLISHED_DEEPSLATE
                        : ((Math.abs(dx) + dz) % 3 == 0 ? Material.STONE_BRICKS : Material.DEEPSLATE_BRICKS);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        Material wall = dy == 4 ? Material.CHISELED_DEEPSLATE
                                : (dy == 1 && dz == 7 && Math.abs(dx) % 3 == 0
                                ? Material.CHISELED_STONE_BRICKS : Material.DEEPSLATE_BRICKS);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        for (int z = -6; z <= 6; z += 3) {
            pen.set(cx - 4, cy, cz + z, Material.IRON_BARS);
            pen.set(cx + 4, cy, cz + z, Material.IRON_BARS);
        }

        // A short cold corridor of deepslate/stone brick — 3 wide, walls 3 tall.
        for (int dz = -1; dz <= 2; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.POLISHED_DEEPSLATE);   // cold rigid floor
            }
            // side walls
            for (int dy = 0; dy <= 2; dy++) {
                pen.set(cx - 1, cy + dy, cz + dz, Material.DEEPSLATE_BRICKS);
                pen.set(cx + 1, cy + dy, cz + dz, Material.DEEPSLATE_BRICKS);
            }
        }

        // THE LOW LINTEL at z=0: the ceiling drops to head height so you must SNEAK under to pass.
        // Floor at cy-1, so a normal 2-block gap is cy..cy+1. We lower the lintel to cy+1 (a 1-block
        // opening at cy) — the bow built into the architecture.
        pen.set(cx, cy + 1, cz, Material.CHISELED_DEEPSLATE);   // the lintel block, dead centre
        pen.set(cx - 1, cy + 1, cz, Material.CRACKED_DEEPSLATE_BRICKS); // wrongness: a cracked lintel stone
        pen.set(cx + 1, cy + 1, cz, Material.DEEPSLATE_BRICKS);
        pen.set(cx, cy + 2, cz, Material.DEEPSLATE_BRICKS);
        // Iron bars framing the threshold (cold, rigid, a gate that won't yield).
        pen.set(cx - 1, cy, cz, Material.IRON_BARS);
        pen.set(cx + 1, cy, cz, Material.IRON_BARS);

        // RUNE-CRIB "BOW" — under/beside the low lintel you must stoop through (the bow built into the door).
        // Mounted LOW at crouch height (cy) at (cx,cy,cz-1), facing EAST so it hangs on the solid west corridor
        // wall (cx-1,cy,cz-1) — iron bars can't hold a sign, the stone wall can. A carved word read at a stoop
        // right at the lintel. crib: the referent is the low lintel at (cx,cy+1,cz), one cell north of this sign.
        pen.runeCrib(cx, cy, cz - 1, BlockFace.EAST, Material.DARK_OAK_WALL_SIGN, "BOW");

        // THE SIX KEEPER BANNERS (§5.2 orin-banner-heraldry) — "his mason-hall": the entry bay IS the
        // mason-hall for placement purposes (the corridor's own widened threshold). Three banners per
        // side wall, at z-rows cz-1/cz+1/cz+2 — deliberately SKIPPING the z=cz lintel row, which
        // already carries the cracked-lintel/lintel-filler blocks (no collision with the low-lintel
        // set-piece). Readable in fall-order (Vaun, Mara, Sella / Orin, Brann, Iss — canon-spine §8.1)
        // as the group walks up to and past the lintel: WEST wall (cx-1) carries Vaun/Mara/Sella
        // front-to-back, EAST wall (cx+1) carries Orin/Brann/Iss front-to-back — so reading west-then-
        // east, near-to-far, IS fall-order. Each banner's pattern echoes that keeper's own maker's-mark
        // LETTER from the UNKEPT acrostic (canon-spine §8.5: Vaun=U, Mara=N, Sella=K, Orin=E, Brann=P,
        // Iss=T) via a simple, distinct vanilla glyph built from SQUARE_* patterns — not a new cipher
        // (the acrostic already exists in each stone's own framing; this is the SAME six-symbol set,
        // echoed here so the hall reads as "these six belong together," per the design's "matches the
        // maker's-marks the group has been collecting"). Orin's OWN banner (east wall, front position)
        // is additionally marked with the mason's-square motif (a bordered diagonal square) — his
        // sigil, "the key" the design calls out by name. The real mechanical unlock stays what
        // cipher-plaintexts.md already binds: stone-orin's run is the plain rune alphabet, read with
        // the Rosetta (no separate per-puzzle key to build); the banners are the LEGIBLE path ("these
        // mean something, go read the stone") with the Rosetta brute-force backup already covered by
        // design (PUZZLE-DESIGNS.md §5.2: "if they ignore the banner they can still brute the
        // substitution — the fragile solution has a backup").
        pen.wallBanner(cx - 1, cy + 1, cz - 1, BlockFace.EAST, DyeColor.BLUE, keeperMark('U'));      // Vaun
        pen.wallBanner(cx - 1, cy + 1, cz + 1, BlockFace.EAST, DyeColor.PURPLE, keeperMark('N'));    // Mara
        pen.wallBanner(cx - 1, cy + 1, cz + 2, BlockFace.EAST, DyeColor.CYAN, keeperMark('K'));      // Sella
        pen.wallBanner(cx + 1, cy + 1, cz - 1, BlockFace.WEST, DyeColor.LIGHT_GRAY, masonSquareMark()); // Orin
        pen.wallBanner(cx + 1, cy + 1, cz + 1, BlockFace.WEST, DyeColor.ORANGE, keeperMark('P'));    // Brann
        pen.wallBanner(cx + 1, cy + 1, cz + 2, BlockFace.WEST, DyeColor.RED, keeperMark('T'));       // Iss

        // Beyond the threshold: an unfinished/broken carving on the back wall (Orin carving the same line).
        for (int dx = -1; dx <= 1; dx++) {
            pen.set(cx + dx, cy,     cz + 3, Material.STONE_BRICKS);
            pen.set(cx + dx, cy + 1, cz + 3, Material.STONE_BRICKS);
        }
        pen.set(cx, cy + 1, cz + 3, Material.CHISELED_STONE_BRICKS);
        pen.set(cx - 1, cy, cz + 3, Material.CRACKED_STONE_BRICKS);   // the unfinished carving
        pen.set(cx + 1, cy + 1, cz + 3, Material.STONE_BRICK_STAIRS); // a chisel-stroke left mid-cut

        // RUNE-CRIB "STONE" — on the unfinished carving wall (Orin carving the same line into stone). Mounted on
        // the NORTH face of the back stone-brick wall at (cx-1,cy,cz+3, the cracked unfinished carving), facing
        // up the passage at (cx-1,cy,cz+2). crib: the referent is the unfinished stone carving-wall behind it.
        pen.runeCrib(cx - 1, cy, cz + 2, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN, "STONE");

        // One soul lantern past the threshold — cold blue light (earned, sparse).
        pen.set(cx, cy + 2, cz + 2, Material.DEEPSLATE_BRICKS);
        pen.hangingLantern(cx, cy + 1, cz + 2, true);

        // THE ANSWER: a carved threshold-stone on the back wall just past the lintel. The single-file passage
        // is walled cx-1/cx+1, so the readable face must point up the passage (NORTH) toward the player: the
        // sign hangs on the solid stone-brick back wall (cz+3) facing NORTH, read from the dry solid floor at
        // (cx,cy-1,cz+1) — clear of the lintel and below the soul-lantern. Left LOW at cy so Orin's stoop is
        // built into reading it too (blank submission slot). Backing = the back wall; standing cell open.
        Location answer = pen.wallSign(cx, cy, cz + 2, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN);
        // RESHAPE R0: label CUT — the low-lintel mechanic speaks for itself; the backing block is kept
        // so the approach wall exists (do not remove the structural block).
        pen.set(cx, cy + 2, cz - 2, Material.DEEPSLATE_BRICKS);
        return answer;
    }

    /**
     * One keeper's banner pattern for the mason-hall banners ({@code orin-banner-heraldry}, §5.2): a
     * single distinct glyph built from vanilla {@link PatternType#SQUARE_TOP_LEFT}/{@code
     * SQUARE_BOTTOM_RIGHT}-family patterns, chosen per the keeper's own maker's-mark LETTER from the
     * {@code UNKEPT} acrostic (canon-spine §8.5) so the six banners' marks are the SAME six symbols
     * the group is already collecting from each keeper-stone's carved framing — not a fresh, arbitrary
     * set. A simple corner/quadrant reading keeps each mark visually distinct at a glance without
     * needing the custom rune bitmap font (banners can't carry a font glyph the way signs can).
     */
    private static List<Pattern> keeperMark(char unkeptLetter) {
        DyeColor mark = DyeColor.WHITE;
        return switch (Character.toUpperCase(unkeptLetter)) {
            // Vaun = U: a single bottom-quadrant square (a mark low and grounded — the hoarder kept low).
            case 'U' -> List.of(new Pattern(mark, PatternType.SQUARE_BOTTOM_LEFT),
                                new Pattern(mark, PatternType.SQUARE_BOTTOM_RIGHT));
            // Mara = N: two opposite top corners (the reader who never walked — a mark that doesn't meet).
            case 'N' -> List.of(new Pattern(mark, PatternType.SQUARE_TOP_LEFT),
                                new Pattern(mark, PatternType.SQUARE_TOP_RIGHT));
            // Sella = K: a single top-quadrant square (the drowned child — a mark that stayed at the surface).
            case 'K' -> List.of(new Pattern(mark, PatternType.SQUARE_TOP_LEFT),
                                new Pattern(mark, PatternType.SQUARE_TOP_RIGHT),
                                new Pattern(mark, PatternType.STRIPE_TOP));
            // Brann = P: a border ring (the watcher who kept the perimeter, watching all night).
            case 'P' -> List.of(new Pattern(mark, PatternType.BORDER));
            // Iss = T: a straight cross (the liar's mark cancels itself — a symbol that crosses out).
            case 'T' -> List.of(new Pattern(mark, PatternType.STRAIGHT_CROSS));
            // Fallback: a plain center stripe (never reached for the six named keepers above).
            default -> List.of(new Pattern(mark, PatternType.STRIPE_CENTER));
        };
    }

    /**
     * Orin's own sigil — the mason's square (§5.2: "his own sigil ... is the substitution alphabet
     * key"). Built as a bordered diagonal-corner motif (a carpenter's/mason's set-square read as two
     * meeting quadrants inside a frame), distinct from every {@link #keeperMark} glyph so it reads at
     * a glance as "this one is different — this one is the key," even before a player can name why.
     */
    private static List<Pattern> masonSquareMark() {
        DyeColor mark = DyeColor.BLACK;
        return List.of(
                new Pattern(mark, PatternType.BORDER),
                new Pattern(mark, PatternType.SQUARE_BOTTOM_LEFT),
                new Pattern(mark, PatternType.SQUARE_TOP_RIGHT));
    }

    /* ================================================================================================
     * BRANN (the black-moon watcher). A small watch-point/platform, a sheltered fire that never goes out
     * (campfire in a lantern-shelter), tally-marks (item-frames or carved), amethyst (the moon), a bell
     * (toll); dark concrete/deepslate sky-motif; answer = a watch-stone sign the fire lights.
     * Palette: black concrete + deepslate + amethyst. Prop: watch platform + bell. Light: a sheltered
     * campfire + amethyst. Wrongness: uneven tally marks + a cracked block underfoot.
     * ============================================================================================== */
    private static Location brann(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: black-moon watch chamber around the original watch platform.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 8 || Math.abs(dz) == 7;
                boolean platform = Math.abs(dx) <= 4 && Math.abs(dz) <= 4;
                Material floor = platform ? Material.BLACK_CONCRETE
                        : ((Math.abs(dx) + Math.abs(dz)) % 2 == 0 ? Material.COBBLED_DEEPSLATE : Material.DEEPSLATE_TILES);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 5; dy++) {
                        Material wall = dy == 5 ? Material.BLACKSTONE
                                : (dy == 2 && (dx + dz) % 5 == 0 ? Material.AMETHYST_BLOCK : Material.COBBLED_DEEPSLATE);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        for (int x = -6; x <= 6; x += 6) {
            pen.set(cx + x, cy + 5, cz, Material.DEEPSLATE_TILES);
            pen.hangingLantern(cx + x, cy + 4, cz, true);
        }

        // Raised 5x5 watch platform: black concrete floor (a piece of night sky) w/ deepslate rim.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.COBBLED_DEEPSLATE : Material.BLACK_CONCRETE);
            }
        }
        // Amethyst set into the black floor — the moon in the dark sky (the black-moon motif).
        pen.set(cx, cy - 1, cz, Material.AMETHYST_BLOCK);
        // A cracked deepslate underfoot at the watch-post — wrongness.
        pen.set(cx - 1, cy - 1, cz + 1, Material.CRACKED_DEEPSLATE_TILES);

        // The sheltered fire that never goes out: a campfire under a slab shelter on deepslate posts.
        pen.set(cx - 1, cy, cz - 1, Material.DEEPSLATE_TILES);
        pen.set(cx + 1, cy, cz - 1, Material.DEEPSLATE_TILES);
        pen.set(cx, cy, cz - 1, Material.CAMPFIRE);               // the watch-fire
        pen.set(cx - 1, cy + 2, cz - 1, Material.DEEPSLATE_TILE_SLAB);
        pen.set(cx + 1, cy + 2, cz - 1, Material.DEEPSLATE_TILE_SLAB);
        pen.set(cx, cy + 2, cz - 1, Material.DEEPSLATE_TILE_SLAB); // the shelter over the fire

        // The bell (the toll) on a post at the platform edge.
        pen.set(cx + 2, cy, cz + 2, Material.DEEPSLATE_BRICK_WALL);
        pen.bell(cx + 2, cy + 1, cz + 2, BlockFace.NORTH);

        // Tally-marks carved as an UNEVEN row of item-frame-free chiseled stripes on the back wall.
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy, cz + 2, Material.DEEPSLATE_BRICKS);
            pen.set(cx + dx, cy + 1, cz + 2, dx % 2 == 0 ? Material.CHISELED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
        }
        // Amethyst bud on the back wall — a sliver of moon (earned cool light).
        pen.clusterOn(cx + 1, cy + 2, cz + 2, BlockFace.SOUTH);

        // THE ANSWER: a watch-stone the fire still lights — the answer sign is mounted on the tally back-wall
        // (cz+2, solid) at head height, facing NORTH out over the open platform so a player standing on the
        // dry watch-floor (at cz, never in the campfire at cz-1) faces it head-on (blank submission slot). The
        // watch-stone plinth stays below it at (cx,cy,cz+1); firelight from the sheltered fire still falls here.
        pen.set(cx, cy, cz + 1, Material.CHISELED_DEEPSLATE);
        Location answer = pen.wallSign(cx, cy + 1, cz + 1, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN);
        // RUNE-CRIB "MOON" — beside the amethyst set into the watch-floor (the black-moon in the night-sky
        // floor). Mounted low at (cx,cy,cz) facing NORTH, backed by the watch-stone plinth just set at
        // (cx,cy,cz+1) — a small carved word right beside the amethyst floor-stud. Placed AFTER the plinth so
        // the backing exists. crib: the referent is the amethyst at (cx,cy-1,cz), directly under this sign.
        pen.runeCrib(cx, cy, cz, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN, "MOON");
        // RESHAPE R0: label CUT — "count the black moons — do not sleep" was a posted instruction for a
        // mechanic that now speaks for itself: BlackMoonTollListener tolls the bell for real on the actual
        // black moon (a live world-fact, not a prompt), and the uneven tally + amethyst moon already read
        // as "something is being counted, unevenly." The backing block is kept so the watch-post's approach
        // wall exists (do not remove the structural block).
        pen.set(cx - 2, cy + 1, cz - 3, Material.COBBLED_DEEPSLATE);
        return answer;
    }

    /* ================================================================================================
     * ISS (the liar). A hearth that LOOKS warm and IS cold: a cozy-looking fireplace whose "fire" is
     * soul-fire / a magma block behind glass that gives no warmth, a comfortable seat, warm-toned blocks
     * (bricks) curdling to cold (blackstone/soul) at the edges; answer = a warm-looking sign on a cold
     * dead hearth. Deception built into the blocks.
     * Palette: brick (warm) curdling to blackstone/soul (cold). Prop: the false hearth + a seat. Light:
     * soul-fire (cold blue light that looks like fire but isn't warm). Wrongness: soul-fire where flame
     * should be + the hearth's edges gone black.
     * ============================================================================================== */
    private static Location iss(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: false-warmth parlor around the original hearth.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -6; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 7 || dz == -6 || dz == 7;
                boolean warmSide = dz <= -1;
                Material floor = warmSide
                        ? ((dx + dz) % 3 == 0 ? Material.PACKED_MUD : Material.BRICKS)
                        : ((dx + dz) % 2 == 0 ? Material.BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS);
                pen.set(cx + dx, cy - 2, cz + dz, Material.DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        Material wall = warmSide
                                ? (dy == 4 ? Material.BRICKS : Material.PACKED_MUD)
                                : (dy == 4 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.BLACKSTONE);
                        pen.set(cx + dx, cy + dy, cz + dz, wall);
                    }
                }
            }
        }
        pen.set(cx - 5, cy + 4, cz - 3, Material.BRICKS);
        pen.set(cx + 5, cy + 4, cz - 3, Material.BRICKS);
        pen.hangingLantern(cx - 5, cy + 3, cz - 3, false);
        pen.hangingLantern(cx + 5, cy + 3, cz - 3, false);

        // 5x5 room floor: warm bricks at the hearth, curdling to cold blackstone at the far edge.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // The lie decays with distance FROM the hearth (which is at dz=-2, the warm back).
                Material floor = (dz <= -1) ? Material.BRICKS
                        : (dz == 0) ? Material.POLISHED_BLACKSTONE_BRICKS
                        : Material.BLACKSTONE;    // the cold truth at the front
                pen.set(cx + dx, cy - 1, cz + dz, floor);
            }
        }

        // The false hearth against the back wall: a warm brick surround...
        for (int dx = -1; dx <= 1; dx++) {
            pen.set(cx + dx, cy,     cz - 2, Material.BRICKS);
            pen.set(cx + dx, cy + 1, cz - 2, Material.BRICKS);
            pen.set(cx + dx, cy + 2, cz - 2, Material.BRICK_STAIRS); // a cozy mantel
        }
        // ...whose FIRE is cold: a magma block behind glass, lit by SOUL FIRE (blue, gives no warmth).
        pen.set(cx, cy, cz - 2, Material.MAGMA_BLOCK);          // "embers" that are not embers
        pen.set(cx, cy, cz - 1, Material.TINTED_GLASS);         // behind glass — you can't feel it
        pen.setIfAir(cx, cy + 1, cz - 2, Material.SOUL_FIRE);   // cold blue "flame"
        // The edges of the hearth already gone black — the lie curdling.
        pen.set(cx - 1, cy, cz - 2, Material.BLACKSTONE);
        pen.set(cx + 1, cy + 1, cz - 2, Material.GILDED_BLACKSTONE); // a false glint of warmth

        // A comfortable-looking seat facing the cold fire (spruce stairs + a soul-lantern that chills).
        pen.stairs(cx + 2, cy, cz, Material.SPRUCE_STAIRS, BlockFace.WEST, false);
        pen.set(cx + 2, cy, cz + 1, Material.SOUL_LANTERN);      // a "warm" lamp that burns cold

        // Wrongness: soul-soil creeping onto the warm brick floor near the seat.
        pen.set(cx + 1, cy - 1, cz, Material.SOUL_SOIL);

        // THE KEEPSAKE LAMP (§7.2 iss-nbt-falsified-entry, NBT-heavy item stego) — a small chest beside
        // the seat, as if left for a guest: Iss's "warm-worded gift" sits where a visitor would find it
        // without being told to look, framed by the warm brick corner (not the cold front edge). A found
        // object, not an admin hand-out — the datamine is the find (PUZZLES.md §1/§5-Iss, D7).
        pen.chestWithItem(cx + 2, cy, cz + 2, BlockFace.WEST, IssKeepsakeLampItem.create("observance"));

        // THE ANSWER: a warm-looking sign on the cold dead hearth (a birch/warm-wood sign against black
        // stone — the deception). Blank submission slot at reading height on the hearth face.
        pen.set(cx - 1, cy + 1, cz - 1, Material.CHISELED_POLISHED_BLACKSTONE);
        Location answer = pen.wallSign(cx - 1, cy + 1, cz, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN);

        // RUNE-CRIB "FIRE" — beside the false hearth's soul-fire/magma-behind-glass. Mounted on the SOUTH face
        // of the solid brick hearth-surround block at (cx+1,cy,cz-2), facing the room, right beside the "fire".
        // crib: the referent is the magma/soul-fire directly left of this sign.
        pen.runeCrib(cx + 1, cy, cz - 1, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN, "FIRE");
        // RUNE-CRIB "COLD" — the contradiction crib: low and subtle, on the cold-black front floor of the same
        // hearth (the "warm" place labels itself cold once readable). A small cold-black backing block at floor
        // level (cx+1,cy,cz+2 is BLACKSTONE floor edge) carries it; sign one course up facing SOUTH, easy to miss.
        // crib: sits low over the blackstone (the cold truth) that curdled out from under the warm brick.
        pen.set(cx + 1, cy, cz + 1, Material.POLISHED_BLACKSTONE); // low backing for the subtle cold crib
        pen.runeCrib(cx + 1, cy, cz + 2, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN, "COLD");

        // RESHAPE R0: label CUT entirely (including "(it lies)" spoiler) — the soul-fire-behind-glass +
        // soul-soil creeping onto warm brick do the lying; narration kills the deception.
        return answer;
    }

    /* ================================================================================================
     * ============================  THE DEEP-HALF SET-PIECES  =========================================
     * The payoff sites the descent leads to. Same craft law as the keepers (carved-never-default,
     * dark-default earned light, per-place palette+prop+light, one or two wrongness touches), but these
     * are LOAD-BEARING moments, not doors: the group reads/answers/bows/reunites here. Where a site is a
     * plain marker/atmosphere anchor (cold hearth, threshold grave, the Seventh chamber) the returned
     * "answer surface" is the lectern/sign the lore hangs on — harmless if no answer-listener watches it.
     * ============================================================================================== */

    /* ================================================================================================
     * STONE OF RECKONING — the digit/sign-glyph Rosetta (companion to the rune-ring). Teaches the number-
     * glyphs and the four coordinate sign-marks (N / S / E / down) every coordinate clue depends on. A
     * squared reckoning-table: a chiseled-deepslate slab bearing an INSCRIBED answer surface, flanked by a
     * counting-row of amethyst studs (i..vi) and a four-armed compass of sign-marks around it. Cool, exact.
     * Palette: polished deepslate + chiseled deepslate + calibrated feel (amethyst studs). Prop: the
     * counting row + the four sign-mark arms. Light: amethyst (earned, cool, learning). Wrongness: one arm's
     * mark is scored through (a cracked block) — a sign someone disputed the reckoning.
     * ============================================================================================== */
    private static Location reckoning(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: a long ledger hall around the low table. This lets a group stand
        // back and read the count wall before approaching the answer surface.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -5; dz <= 9; dz++) {
                boolean rim = Math.abs(dx) == 7 || dz == -5 || dz == 9;
                Material floor = rim ? Material.CHISELED_TUFF
                        : ((dz >= 5) ? Material.POLISHED_TUFF : Material.POLISHED_DEEPSLATE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 4 ? Material.BLACKSTONE
                                        : (dy == 2 ? Material.POLISHED_BASALT : Material.DEEPSLATE_TILES));
                    }
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            pen.set(cx + dx, cy, cz + 7, dx == 0 ? Material.CHISELED_DEEPSLATE : Material.CHISELED_TUFF);
            pen.set(cx + dx, cy + 1, cz + 7, Material.WEATHERED_CUT_COPPER);
            pen.set(cx + dx, cy + 2, cz + 7, dx == 4 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE);
        }
        for (int i = 0; i < 7; i++) {
            int x = cx - 3 + i;
            pen.set(x, cy, cz + 6, i == 6 ? Material.GRAY_CANDLE : Material.AMETHYST_BLOCK);
            if (i != 6) pen.clusterOn(x, cy + 1, cz + 6, BlockFace.UP);
        }

        // 7x7 squared floor: polished-deepslate field with a chiseled rim (a workman's exact square).
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.CHISELED_DEEPSLATE : Material.POLISHED_DEEPSLATE);
            }
        }

        // The reckoning-table: a low chiseled slab you stoop to read (the bow built in), centred.
        pen.set(cx, cy - 1, cz, Material.CHISELED_DEEPSLATE);
        pen.stairs(cx, cy, cz, Material.POLISHED_DEEPSLATE_STAIRS, BlockFace.SOUTH, false); // the canted reading face
        // THE ANSWER: the inscribed slab-face — a blank unwaxed submission slot on the table's FRONT. Placed one
        // cell forward (south) of the canted stair so BOTH survive: the stair (cx,cy,cz) is the solid backing,
        // the sign faces SOUTH and is read stooped from the dry floor at (cx,cy-1,cz+2). (Backing block behind
        // the table kept at cz-1.)
        pen.set(cx, cy, cz - 1, Material.CHISELED_DEEPSLATE);
        Location answer = pen.wallSign(cx, cy, cz + 1, BlockFace.SOUTH, deepslateWallSign());

        // The counting-row: six amethyst studs set into the north rim — the digit-glyphs, i..vi, cool light.
        int[] cols = {-3, -2, -1, 1, 2, 3};
        for (int c : cols) {
            pen.set(cx + c, cy - 1, cz - 2, Material.POLISHED_DEEPSLATE);
            pen.set(cx + c, cy, cz - 2, Material.AMETHYST_BLOCK);
        }
        pen.clusterOn(cx, cy, cz - 2, BlockFace.UP); // the learning-light at the row's head

        // The four sign-mark arms — a compass of the coordinate marks (N/S/E/down) squared around the table.
        pen.set(cx, cy, cz - 3, Material.CHISELED_DEEPSLATE);         // N arm
        pen.set(cx, cy, cz + 3, Material.CHISELED_DEEPSLATE);         // S arm
        pen.set(cx, cy + 1, cz + 3, Material.CHISELED_DEEPSLATE);     // S arm raised — solid backing for the heading
        pen.set(cx + 3, cy, cz, Material.CHISELED_DEEPSLATE);         // E arm
        // (down arm at (cx,cy-1,cz) is the table's base block, already set above — no redundant re-set.)
        // Wrongness: the E arm scored through — a cracked block, someone disputed the reckoning.
        pen.set(cx + 3, cy, cz + 1, Material.CRACKED_DEEPSLATE_TILES);

        // RESHAPE R0: label REDUCED — cut "count the marks, then the way — north, down, read" (a walkthrough).
        // The cracked/disputed compass arm is the detail to study; the title stays as a site anchor.
        // Coordinate marks are now carried by the room geometry itself (the six studs and disputed arm);
        // the title stays spare so the compass remains a puzzle surface, not a caption.
        pen.labelWallSign(cx, cy + 1, cz + 2, BlockFace.NORTH, deepslateWallSign(),
                new String[]{"the reckoning", "", "", ""});
        return answer;
    }

    /* ================================================================================================
     * THE COLD HEARTH — Iss's false-warm dead shrine, the false-way-up's cold end (the endpoint of the
     * liar's coordinate). Echoes the Iss KEEPER palette (warm brick curdling to blackstone/soul) but built
     * as a whole dead ROOM lived in once and let go: a doused hearth (no fire), a worn bench, a house-name
     * slab where a kept home would have cut its name, the way past it grown over and STOPPED. Colder,
     * emptier, sadder than the Iss keeper-stone — this is where the warm man's word runs out of world.
     * Palette: brick (warm, cooling) → polished blackstone → blackstone/soul (cold truth). Prop: the doused
     * hearth + the bench + the house-name slab. Light: NONE earned — a single unlit candle, a doused hearth.
     * Wrongness: cold ash where fire should be + the path grown shut (a wall of dead leaves/roots).
     * ============================================================================================== */
    private static Location coldHearth(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: make the cold hearth a room players enter, not a 5x5 hearth prop.
        // The old composition remains as the central dead-domestic focal point.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean rim = Math.abs(dx) == 8 || Math.abs(dz) == 6;
                Material floor = dz <= -2 ? Material.BRICKS
                        : (dz <= 1 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.BLACKSTONE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.CRACKED_DEEPSLATE_BRICKS : floor);
                if (rim) {
                    for (int dy = 0; dy <= 3; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 3 ? Material.BLACKSTONE
                                        : (dz <= -3 ? Material.BRICKS : Material.POLISHED_BLACKSTONE_BRICKS));
                    }
                }
            }
        }
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int dy = 0; dy <= 3; dy++) {
                pen.set(x, cy + dy, cz + 6, dy == 0 ? Material.HANGING_ROOTS : Material.OAK_LEAVES);
            }
        }
        pen.candle(cx - 6, cy, cz - 4, Material.GRAY_CANDLE, false);
        pen.candle(cx + 6, cy, cz - 4, Material.GRAY_CANDLE, false);

        // 5x5 room floor: brick at the hearth (warm, but cooling) curdling to cold blackstone at the front.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Material floor = (dz <= -1) ? Material.BRICKS
                        : (dz == 0) ? Material.POLISHED_BLACKSTONE_BRICKS
                        : Material.BLACKSTONE;   // the cold truth toward the doorway
                pen.set(cx + dx, cy - 1, cz + dz, floor);
            }
        }
        // Low side walls of cooling brick (a home, human-scaled, unlike the wrong-scaled deep below it).
        for (int dz = -2; dz <= 2; dz++) {
            pen.set(cx - 2, cy, cz + dz, Material.BRICKS);
            pen.set(cx + 2, cy, cz + dz, Material.BRICKS);
        }

        // The DOUSED hearth against the back wall — a brick surround with a cold, dead firebox (no fire).
        for (int dx = -1; dx <= 1; dx++) {
            pen.set(cx + dx, cy,     cz - 2, Material.BRICKS);
            pen.set(cx + dx, cy + 1, cz - 2, Material.BRICKS);
        }
        pen.set(cx, cy + 2, cz - 2, Material.BRICK_STAIRS);              // the mantel
        pen.campfire(cx, cy, cz - 2, false);                            // the fire, gone out
        pen.set(cx - 1, cy, cz - 1, Material.GRAY_CONCRETE_POWDER);     // cold ash spilled onto the hearthstone (wrongness)
        // The lie curdling: the hearth's edges already gone black, a false glint of gilding.
        pen.set(cx - 1, cy + 1, cz - 2, Material.BLACKSTONE);
        pen.set(cx + 1, cy + 1, cz - 2, Material.GILDED_BLACKSTONE);

        // The worn bench facing the dead fire — someone sat here and waited for warmth that never came.
        pen.stairs(cx + 1, cy, cz, Material.SPRUCE_STAIRS, BlockFace.WEST, false);
        pen.stairs(cx + 1, cy, cz + 1, Material.SPRUCE_STAIRS, BlockFace.WEST, false);
        // A single UNLIT candle on the mantel — no light is earned here.
        pen.candle(cx + 1, cy + 1, cz - 2, Material.CANDLE, false);

        // Wrongness: the way OUT past the shrine grown over and stopped (dead roots/leaves choke the doorway).
        for (int dx = -1; dx <= 1; dx++) {
            pen.setIfAir(cx + dx, cy, cz + 2, Material.HANGING_ROOTS);
            pen.setIfAir(cx + dx, cy + 1, cz + 2, Material.HANGING_ROOTS);
        }

        // The house-name slab, where a KEPT home would have cut its house-name — here cut on cold blackstone,
        // the lectern the false-walk arrival word is read at. Returned as the "answer surface" (harmless if
        // no listener watches this marker site; the cold_hearth is type `marker`).
        pen.set(cx, cy, cz - 1, Material.CHISELED_POLISHED_BLACKSTONE);
        Location answer = pen.lectern(cx, cy + 1, cz - 1, BlockFace.SOUTH);
        pen.putBook(cx, cy + 1, cz - 1, "the house at the end of the path",
                "we came out here on a\nwarm man's word.\n\nthe fire is out. there is\nno door up. only the\nwalk back.");
        // RESHAPE R0: label CUT entirely — the doused hearth + roots-grown-over-the-door + cold-ash spill
        // deliver the emotional beat silently; "he sent you out" explains what should be felt, not told.
        return answer;
    }

    /* ================================================================================================
     * UNBROKEN LIGHT — the Undercroft Accepting floor: the ONE eternal kept fire in the vast dark, a wide
     * squared floor where the whole present group bows as one (the AcceptingRiteListener climax site).
     * Sacred, sparse, enormous-feeling: a large dark polished-blackstone floor with a single warm fire at
     * its exact centre and a ring of inward-facing markers — the last lamps, still holding, all turned in.
     * Deliberately UN-cluttered: the emptiness is the point (room for 6-8 to gather and bow). No wrongness
     * here — this is the one place the dark was never let win; the only imperfection allowed is a single
     * doused lamp in the ring (the seventh place, kept open, unlit — waiting).
     * Palette: polished blackstone + deepslate (the deep line) + a lit hearth-fire. Prop: the central kept
     * fire + the inward-facing lamp ring. Light: ONE warm fire + soul-lanterns turned inward (earned, held).
     * ============================================================================================== */
    private static Location unbrokenLight(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: a full underground climax chamber around the functional 11x11 floor.
        // The original ring remains centered for the Accepting listener; this shell makes it a real reveal.
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                int m = Math.max(Math.abs(dx), Math.abs(dz));
                boolean outer = m == 15 || (Math.abs(dx) >= 13 && Math.abs(dz) >= 11)
                        || (Math.abs(dz) >= 13 && Math.abs(dx) >= 11);
                Material floor = outer ? Material.DEEPSLATE_BRICKS
                        : (m <= 5 ? Material.POLISHED_BLACKSTONE : Material.POLISHED_DEEPSLATE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (outer) {
                    for (int dy = 0; dy <= 7; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 7 ? Material.BLACKSTONE
                                        : (dy % 3 == 0 ? Material.POLISHED_BASALT : Material.DEEPSLATE_TILES));
                    }
                }
            }
        }
        for (int[] rib : new int[][]{{-12, -12}, {0, -15}, {12, -12}, {15, 0}, {12, 12}, {0, 15}, {-12, 12}, {-15, 0}}) {
            int px = cx + rib[0], pz = cz + rib[1];
            for (int dy = 0; dy <= 9; dy++) {
                pen.set(px, cy + dy, pz, dy == 9 ? Material.CHISELED_DEEPSLATE : Material.POLISHED_BASALT);
            }
            if (!(rib[0] == -15 && rib[1] == 0)) pen.hangingLantern(px, cy + 6, pz, true);
        }
        for (int dz = -22; dz <= -16; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.POLISHED_DEEPSLATE);
                if (Math.abs(dx) == 2) {
                    for (int dy = 0; dy <= 4; dy++) pen.set(cx + dx, cy + dy, cz + dz, Material.DEEPSLATE_BRICKS);
                }
            }
        }

        // A wide 11x11 dark floor: polished blackstone field, deepslate-brick rim (the gather-room; big
        // enough for the Accepting quorum to stand and bow together — the emptiness IS the design).
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean rim = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.DEEPSLATE_BRICKS : Material.POLISHED_BLACKSTONE);
            }
        }
        // The pressure-glyph walked on the floor: a small inlaid cross of polished-deepslate at the centre
        // (the rune the descent's floor-walk reads — subtle, dark-on-dark).
        for (int d = -1; d <= 1; d++) {
            pen.set(cx + d, cy - 1, cz, Material.POLISHED_DEEPSLATE);
            pen.set(cx, cy - 1, cz + d, Material.POLISHED_DEEPSLATE);
        }

        // THE ONE FIRE that never went out, at the exact centre, raised one course on a chiseled plinth so
        // it reads across the whole dark floor — the single warm light in vast dark.
        pen.set(cx, cy - 1, cz, Material.CHISELED_DEEPSLATE);
        pen.set(cx, cy, cz, Material.CAMPFIRE);                  // lit — the kept fire

        // The last-lamps ring: soul-lanterns on deepslate posts at the four quarters, all turned inward,
        // still holding. One place in the ring is kept open and UNLIT — the seventh seat, waiting.
        int[][] lamps = {{0, -4}, {4, 0}, {0, 4}, {-4, 0}};
        for (int i = 0; i < lamps.length; i++) {
            int px = cx + lamps[i][0], pz = cz + lamps[i][1];
            pen.set(px, cy, pz, Material.DEEPSLATE_BRICK_WALL);       // the lamp-post
            if (i == 3) {
                pen.set(px, cy + 1, pz, Material.DEEPSLATE_TILES);    // the seventh place: post capped, lamp doused
            } else {
                pen.hangingLantern(px, cy + 1, pz, true);            // a held soul-lamp
            }
        }

        // THE ANSWER SURFACE: this floor's rite is the wordless group bow (opaque token, not typed), so the
        // only editable surface is a witness-slab at the fire's edge — a lectern carrying the Accepting text.
        // (accepting_floor listener watches the crouch, not this lectern; harmless either way.)
        Location answer = pen.lectern(cx, cy, cz - 1, BlockFace.NORTH);
        pen.putBook(cx, cy, cz - 1, "the accepting",
                "the dark cannot be killed\nor escaped. only witnessed.\n\nbow, together, and be the\ncompany the kept were.");
        // RESHAPE R0: label CUT entirely — "bow as one" is a stage direction; AcceptingRiteListener handles
        // the crouch-detection and the lectern book carries the text; the label is redundant narration.
        return answer;
    }

    /* ================================================================================================
     * THE THRESHOLD — the deep threshold / the future-dated grave that OPENS FROM THE INSIDE (the reunion
     * image, the-seventh-below §"the reunion"). A low sealed lintel you crouch through (Orin's stoop),
     * beyond it a single grave-slab cut with a date not yet come — and its capstone shoved ajar FROM WITHIN,
     * because the one who set the appointment has been waiting on the other side. Not a graveyard: ONE grave.
     * The door standing open (against the cold hearth's door grown shut) — the true walk's answer-lintel.
     * Palette: deepslate brick + polished blackstone (the sealed deep) + a warm seam of soul-light leaking
     * UP from inside the ajar grave (the warmth is below, and someone is there). Prop: the low lintel + the
     * ajar grave. Light: a thin warm seam from within (earned — the reunion). Wrongness (right kind here):
     * the grave opened from the WRONG side — the capstone pushed out, not in.
     * ============================================================================================== */
    private static Location threshold(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: widen this into a lintel room with an approach and landing beyond.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 8; dz++) {
                boolean rim = Math.abs(dx) == 6 || dz == -6 || dz == 8;
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_DEEPSLATE);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 4 ? Material.BLACKSTONE
                                        : (dy == 2 ? Material.POLISHED_BASALT : Material.DEEPSLATE_BRICKS));
                    }
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            pen.set(cx + dx, cy, cz, Material.CHISELED_DEEPSLATE);
            pen.set(cx + dx, cy + 1, cz, Material.POLISHED_BLACKSTONE_BRICKS);
        }

        // A short sealed corridor of deepslate brick — 3 wide, to the low lintel.
        for (int dz = -1; dz <= 3; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.POLISHED_DEEPSLATE);
            }
            for (int dy = 0; dy <= 2; dy++) {
                pen.set(cx - 1, cy + dy, cz + dz, Material.DEEPSLATE_BRICKS);
                pen.set(cx + 1, cy + dy, cz + dz, Material.DEEPSLATE_BRICKS);
            }
        }
        // THE LOW LINTEL at z=0: ceiling drops to a 1-block opening — you must crouch through (Orin's stoop).
        pen.set(cx, cy + 1, cz, Material.CHISELED_DEEPSLATE);
        pen.set(cx - 1, cy + 1, cz, Material.DEEPSLATE_BRICKS);
        pen.set(cx + 1, cy + 1, cz, Material.DEEPSLATE_BRICKS);
        pen.set(cx, cy + 2, cz, Material.DEEPSLATE_BRICKS);

        // Beyond the lintel: the single grave. A polished-blackstone box set into the floor, its CAPSTONE
        // shoved AJAR — a slab left open, pushed from below (opened from the inside).
        pen.set(cx, cy - 2, cz + 2, Material.POLISHED_BLACKSTONE_BRICKS);      // the grave's floor (one deeper)
        for (int dx = -1; dx <= 1; dx++) {
            pen.set(cx + dx, cy - 1, cz + 2, dx == 0 ? Material.AIR : Material.POLISHED_BLACKSTONE_BRICKS);
        }
        // The capstone, ajar: a blackstone slab shoved half off, resting on the rim (the reunion tell —
        // the stone opens toward the living, from within).
        pen.set(cx, cy - 1, cz + 3, Material.POLISHED_BLACKSTONE);            // the shoved-off cap, landed askew
        pen.stairs(cx + 1, cy - 1, cz + 2, Material.POLISHED_BLACKSTONE_STAIRS, BlockFace.WEST, false);

        // The warm seam: a soul-lantern down IN the open grave, so a thin warm light leaks UP from inside —
        // the warmth is below, and someone is there (the-seventh-below: "further down is where the warmth is").
        pen.hangingLantern(cx, cy - 2, cz + 2, true);

        // THE ANSWER-LINTEL: the true-walk arrival word, cut on the corridor's east inner wall (cx+1, solid
        // deepslate brick) just past the lintel, facing WEST into the passage at head height. A player reads/
        // edits it standing on the dry solid floor at (cx,cy-1,cz+1) — clear of the open grave (the pit is one
        // block further south at cz+2) and past the crouch-gap. The east wall is the backing; the passage
        // column is the open standing cell (blank submission slot). Returned as the answer surface.
        Location answer = pen.wallSign(cx, cy + 1, cz + 1, BlockFace.WEST, Material.DARK_OAK_WALL_SIGN);
        // RESHAPE R0: grave-marker REDUCED to date-only in world-notation (unglossed).
        // CUT "the stone is open from the inside" — the shoved-out capstone lit from within IS the story.
        // Unglossed date-mark: deliberately bare, a world mark to revisit after the future-grave beat, not
        // an English explanation.
        pen.set(cx, cy, cz + 2, Material.POLISHED_BLACKSTONE_BRICKS);
        pen.labelWallSign(cx, cy, cz + 3, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"· · — · · ·", "", "", ""});
        return answer;
    }

    /* ================================================================================================
     * THE UNWRITING — the Seventh's chamber, the hearth-DEEP beneath the cold hearth (WORLD-BIBLE §11.1
     * chamber 2/3). THE emotional payoff space. Two truths, layered: the effaced-name WALL (the Seventh's
     * name and looking scraped away — deliberately, by a blade taken to it AFTER writing) and, at the
     * bottom, the Seventh's own UNDEGRADED hand (they kept every way; their voice is not degraded — the
     * clearest of the seven) beside a hearth-stone never lit and a deposit-slot for an offering never
     * received. Wrong-scaled in a way no other deep is: not too-tall — UNFINISHED, dug and then STOPPED,
     * because the hand that cut it was cast out before it could finish. Bittersweet: the wait made physical.
     * Palette: deepslate/blackstone (the effacement) + ONE undegraded clean-cut chiseled slab (the Seventh's
     * hand, unlike everything around it) + amethyst (a single kept light, the deep-bird's cool). Prop: the
     * scraped wall + the undegraded hand-slab + the unlit hearth + the empty deposit-slot. Light: a single
     * amethyst (the one thing down here that still holds). Wrongness: the deliberate scrape marks + the
     * unfinished, stopped-mid-cut ceiling.
     * ============================================================================================== */
    private static Location unwriting(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: wall-scale Seventh chamber. The original scrape/clean-hand focal
        // remains below, but the chamber now has real breadth, height, and an approach groove.
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                boolean rim = Math.abs(dx) == 12 || Math.abs(dz) == 8;
                Material floor = rim ? Material.BLACKSTONE
                        : ((Math.abs(dx) <= 2 || dz <= -3) ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_TILES);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 7; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 7 ? Material.BLACKSTONE
                                        : (dy == 3 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.DEEPSLATE_BRICKS));
                    }
                }
            }
        }
        for (int dx = -7; dx <= 7; dx++) {
            pen.set(cx + dx, cy, cz - 7, dx == 0 ? Material.CALCITE : Material.CHISELED_DEEPSLATE);
            pen.set(cx + dx, cy + 1, cz - 7, Math.abs(dx) == 6 ? Material.CRACKED_DEEPSLATE_BRICKS : Material.POLISHED_BLACKSTONE);
            if (dx != 2) {
                pen.set(cx + dx, cy + 2, cz - 7, Material.DEEPSLATE_TILES);
                pen.set(cx + dx, cy + 3, cz - 7, Material.BLACKSTONE);
            }
        }

        // 5x7 chamber floor: cobbled/cracked deepslate — the effacement underfoot, older than the keepers'.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                pen.set(cx + dx, cy - 1, cz + dz, (Math.abs(dx) + Math.abs(dz)) % 3 == 0
                        ? Material.CRACKED_DEEPSLATE_BRICKS : Material.COBBLED_DEEPSLATE);
            }
        }

        // THE UNWRITING WALL (back, chamber 2): a blackstone wall the name was scraped OFF of — the six
        // rail-fence rows (Brann's taught literacy, rails=6) recovered from the effacement. Deliberately
        // half-effaced: chiseled where it survives, gouged (cracked/blank) where the blade took it after.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                boolean scraped = (dx == -1 && dy == 1) || (dx == 1 && dy == 2) || (dx == 0 && dy == 0);
                pen.set(cx + dx, cy + dy, cz - 3, scraped ? Material.CRACKED_POLISHED_BLACKSTONE_BRICKS
                        : Material.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        // The scrape itself: a course of raw blackstone smeared across where the name-line was (the wrongness).
        pen.set(cx - 2, cy + 1, cz - 3, Material.BLACKSTONE);
        pen.set(cx + 2, cy + 1, cz - 3, Material.BLACKSTONE);

        // The unfinished, STOPPED ceiling — dug and then abandoned mid-cut (this deep alone reads as halted,
        // not as too-tall). A chisel-stroke left mid-air: a stair jutting from unfinished rock.
        pen.set(cx, cy + 3, cz - 2, Material.DEEPSLATE_BRICKS);
        pen.stairs(cx, cy + 3, cz - 1, Material.DEEPSLATE_BRICK_STAIRS, BlockFace.SOUTH, true); // the stopped stroke

        // THE SEVENTH'S UNDEGRADED HAND (chamber 3): one clean-cut chiseled-deepslate slab, unlike everything
        // scraped and cracked around it — the clearest, most direct of the seven, kept perfectly. Low, canted,
        // stooped-to-read. The lectern here carries the Seventh's own words (the-seventh-below), the emotional
        // core; returned as the answer/read surface (the seventh_shrine listener handles the staged rite).
        pen.set(cx, cy - 1, cz, Material.CHISELED_DEEPSLATE);
        pen.stairs(cx, cy, cz, Material.POLISHED_DEEPSLATE_STAIRS, BlockFace.SOUTH, false); // the canted clean face
        Location answer = pen.lectern(cx, cy + 1, cz, BlockFace.SOUTH);
        pen.putBook(cx, cy + 1, cz, "the seventh's hand",
                "i kept every way, the\nwhole of my life, and i\nwas not kept.\n\nyou came down here. that\nis the other keeping. i\nam not where it left me.");

        // The unlit hearth-stone (never warmed) + the empty deposit-slot (an offering never received) — the
        // wait made physical. A cold firebox and a slot cut for a gift that never came.
        pen.set(cx - 2, cy, cz + 2, Material.CHISELED_POLISHED_BLACKSTONE);
        pen.campfire(cx - 2, cy + 1, cz + 2, false);                        // the hearth, never lit
        pen.set(cx + 2, cy, cz + 2, Material.POLISHED_BLACKSTONE_BRICKS);
        pen.set(cx + 2, cy, cz + 2 - 1, Material.AIR);                       // the deposit-slot, cut and empty
        pen.set(cx + 2, cy - 1, cz + 1, Material.CHISELED_POLISHED_BLACKSTONE);

        // The ONE kept light down here: a single amethyst, the deep-bird's cool — the one thing that still
        // holds, over the Seventh's undegraded hand. Bittersweet: light, but only a little, and only one.
        pen.set(cx, cy + 2, cz - 1, Material.DEEPSLATE_BRICKS);
        pen.clusterOn(cx, cy + 1, cz - 1, BlockFace.DOWN);

        // RESHAPE R0: label CUT entirely — scraped wall vs one clean slab + the stopped ceiling already
        // carry the full narrative; narration of "the seal is a name / read it back" spoils what should be
        // the strongest room in the build.
        return answer;
    }

    /* ================================================================================================
     * THRESHOLD VAULT — the asymmetric co-op vault (design/PUZZLE-DESIGNS §8.2, INTEGRATION signature #2;
     * ThresholdVaultListener site). A real vault/trial-chamber feel: a sealed round vault chamber of
     * chiseled tuff + copper, a heavy trial-chambers vault BLOCK as the door's heart, a rune-wall the group
     * reads their per-player fragments off of, and the vault SIGN they type the assembled combination at.
     * The fragments are the puzzle (shown per-player by the listener), the vault is the payoff. Grand, cold,
     * mechanical — the one place that reads like a made lock, not a grave.
     * Palette: chiseled tuff + polished tuff + copper (weathered/cut) — the trial-chamber palette. Prop: the
     * VAULT block + the copper-bulb + the fragment rune-wall. Light: copper bulbs (mechanical, cold). Prop
     * wrongness: one copper bulb dead (unlit) + a cracked tuff block — a lock that has been tried before.
     * ============================================================================================== */
    private static Location thresholdVault(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: larger asymmetric co-op vault around the original lock core.
        // Side niches give per-player fragments a real place to be read from.
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean rim = Math.abs(dx) == 11 || Math.abs(dz) == 7;
                Material floor = rim ? Material.CHISELED_TUFF
                        : ((Math.abs(dx) >= 7 && Math.abs(dz) <= 3) ? Material.CUT_COPPER : Material.POLISHED_TUFF);
                pen.set(cx + dx, cy - 2, cz + dz, Material.COBBLED_DEEPSLATE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 5; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 5 ? Material.CHISELED_TUFF
                                        : (dy == 2 ? Material.WEATHERED_CUT_COPPER : Material.POLISHED_TUFF));
                    }
                }
            }
        }
        for (int[] niche : new int[][]{{-9, -4}, {-9, 0}, {-9, 4}, {9, -4}, {9, 0}, {9, 4}}) {
            int px = cx + niche[0], pz = cz + niche[1];
            pen.set(px, cy, pz, Material.CHISELED_TUFF);
            pen.set(px, cy + 1, pz, Material.POLISHED_TUFF);
            pen.set(px, cy + 2, pz, Material.COPPER_GRATE);
            pen.hangingLantern(px, cy + 3, pz, false);
        }

        // 7x7 vault chamber: polished-tuff floor, chiseled-tuff rim (a made room, squared and sealed).
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.CHISELED_TUFF : Material.POLISHED_TUFF);
            }
        }
        // The vault walls (back + sides), chiseled tuff banded with cut copper — the trial-chamber look.
        for (int dx = -3; dx <= 3; dx++) {
            pen.set(cx + dx, cy,     cz - 3, Material.CHISELED_TUFF);
            pen.set(cx + dx, cy + 1, cz - 3, Material.CUT_COPPER);
            pen.set(cx + dx, cy + 2, cz - 3, Material.CHISELED_TUFF);
        }
        for (int dz = -2; dz <= 2; dz++) {
            pen.set(cx - 3, cy,     cz + dz, Material.CHISELED_TUFF);
            pen.set(cx + 3, cy,     cz + dz, Material.CHISELED_TUFF);
            pen.set(cx - 3, cy + 1, cz + dz, Material.WEATHERED_CUT_COPPER);
            pen.set(cx + 3, cy + 1, cz + dz, Material.WEATHERED_CUT_COPPER);
        }

        // THE VAULT — the trial-chambers vault block as the door's heart, set into the back wall on a
        // chiseled-tuff plinth (a real, made lock). Flanked by a copper bulb each side (cold mechanical light).
        pen.set(cx, cy - 1, cz - 2, Material.CHISELED_TUFF);
        pen.set(cx, cy, cz - 2, vaultBlock());                          // the vault (or heavy-core fallback)
        pen.set(cx - 1, cy + 1, cz - 3, copperBulbLit());              // a live bulb
        pen.set(cx + 1, cy + 1, cz - 3, copperBulbDead());            // a DEAD bulb (wrongness — tried before)
        pen.set(cx + 1, cy, cz + 2, Material.CRACKED_STONE_BRICKS);    // a cracked block — the lock has been tried

        // The fragment rune-wall: a broad chiseled-tuff face the listener hangs each player's per-player rune
        // fragment on (read aloud + combined = the code). Left clean so the text-displays read against it.
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy, cz + 3, Material.CHISELED_TUFF);
            pen.set(cx + dx, cy + 1, cz + 3, Material.POLISHED_TUFF);
        }

        // THE VAULT SIGN — the group types the assembled combination here (blank unwaxed submission slot on
        // the vault's plinth, facing the room). The ThresholdVaultListener resolves the SignChangeEvent here.
        Location answer = pen.wallSign(cx, cy, cz - 1, BlockFace.SOUTH, copperWallSign());
        // RESHAPE R0: label REDUCED — cut "each holds one rune — read them as one" (narrates the mechanic).
        // Replaced with an untranslatable cipher inscription so the vault reads as a made lock, not a
        // tutorial. The honest co-op mechanic is allowed to show, but not to narrate itself.
        // Unglossed lock inscription: the dots/dashes make the vault read as a made lock without narrating
        // the co-op mechanic in English.
        pen.labelWallSign(cx, cy + 1, cz + 2, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"· — ·· — ···", "·· — · — ··", "", ""});
        return answer;
    }

    /* ================================================================================================
     * ============================  THE TWO DEEPENING LANES  ==========================================
     * The Nether forge-pocket + the End Seventh-shrine, built in the REAL vanilla Nether/End (approach A —
     * no custom dimensions; sites.yml targets world_nether / world_the_end). Same craft law as the keepers
     * (carved-never-default, dark-default earned light, per-place palette+prop+light, one or two wrongness
     * touches). Additive + protected: they claim only the footprint they carve, terrain-agnostic (they seat
     * their own floor/walls), and are placed AT BUILD TIME at the surveyed/scattered spot — never pasted
     * toward an approaching player (reveal-safe, like every other builder here).
     * ============================================================================================== */

    /* ================================================================================================
     * NETHER FORGE — the deep fire-source made walkable ("below the below"; sites.yml `nether_forge`). A
     * small ruined pocket-room just past a lit portal (a DELVE, not a build-out): a prior keeper's remains
     * laid on a DEEPSLATE SLAB, a DOUSED soul-lantern, and the on-site WORD surface — a blank answer-sign
     * the AnswerSignListener reads at the slab (INV-14; the-fire-kept-me fiction). Nether-palette, modest.
     * Palette: blackstone + basalt + deepslate (the fire-scorched deep). Prop: the keeper's remains on the
     * slab + the doused soul-lantern + a decaying journal (lectern book). Light: NONE earned — the lantern
     * is out; one dim glow-block glimmer only. Wrongness: the doused lantern where fire should be + a
     * scorch of magma/soul-fire the room was built around.
     * ============================================================================================== */
    private static Location netherForge(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: a forge pocket with approach apron and blocked fire-source recess.
        // The existing slab/remains remain as the functional answer core.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean rim = Math.abs(dx) == 8 || Math.abs(dz) == 6;
                Material floor = rim ? Material.BASALT
                        : ((dz <= -2) ? Material.POLISHED_BASALT : Material.POLISHED_BLACKSTONE);
                pen.set(cx + dx, cy - 2, cz + dz, Material.BLACKSTONE);
                pen.set(cx + dx, cy - 1, cz + dz, floor);
                if (rim) {
                    for (int dy = 0; dy <= 4; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 4 ? Material.BLACKSTONE
                                        : (dy == 1 ? Material.CRACKED_POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_BLACKSTONE_BRICKS));
                    }
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy - 1, cz + 6, Material.SOUL_SAND);
            pen.setIfAir(cx + dx, cy, cz + 6, Material.SOUL_FIRE);
        }

        // 5x5 pocket floor: polished-blackstone field with a basalt rim (a ruined room carved into the
        // fire-deep, not a bright build on the surface). The builder seats its own floor so it is terrain-
        // agnostic in the broken Nether ground (nether-rack / lava-edge) — additive, claims only this cell.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.BASALT : Material.POLISHED_BLACKSTONE);
            }
        }
        // Low back + side walls of blackstone brick, curdling to raw basalt columns at the corners (the
        // fire-scorched deep). Two courses tall — a delve, not a hall.
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy,     cz - 2, Material.POLISHED_BLACKSTONE_BRICKS);
            pen.set(cx + dx, cy + 1, cz - 2, Material.BLACKSTONE);
        }
        pen.set(cx - 2, cy, cz - 1, Material.POLISHED_BASALT);
        pen.set(cx + 2, cy, cz - 1, Material.POLISHED_BASALT);
        pen.set(cx - 2, cy + 1, cz - 1, Material.BLACKSTONE);
        pen.set(cx + 2, cy + 1, cz - 1, Material.BLACKSTONE);

        // The scorch the room was built around: a magma seam behind glass in the back wall, with a smear of
        // soul-fire above it (cold blue "flame" over the dead-warm fire — the-fire-kept-me, and it went out).
        pen.set(cx, cy, cz - 2, Material.MAGMA_BLOCK);              // the ember-seam that gave no lasting warmth
        pen.setIfAir(cx, cy + 1, cz - 2, Material.SOUL_FIRE);       // wrongness: cold flame over dead embers

        // THE PRIOR KEEPER'S REMAINS on a DEEPSLATE SLAB — the fire-forge slab (sites.yml design). A low
        // chiseled-deepslate plinth capped with a deepslate slab; the remains (a skull) laid on it. Placed
        // AT BUILD TIME, never toward a player (reveal-safe). This is the slab the WORD is read + answered at.
        pen.set(cx, cy - 1, cz, Material.CHISELED_DEEPSLATE);       // the slab's plinth
        pen.set(cx, cy, cz, Material.COBBLED_DEEPSLATE_SLAB);       // THE DEEPSLATE SLAB (the fire-forge slab)
        pen.setIfAir(cx, cy + 1, cz, Material.SKELETON_SKULL);      // the prior keeper's remains laid on the slab

        // The DOUSED soul-lantern — cold, no light earned (the-fire-kept-me: the keeping was a carrying, and
        // it stopped). A soul-lantern block that reads as a snuffed lamp, hung on a basalt post at the slab's side.
        pen.set(cx - 1, cy, cz, Material.POLISHED_BASALT);
        pen.set(cx - 1, cy + 2, cz, Material.BLACKSTONE);          // the beam it hangs from
        pen.hangingLantern(cx - 1, cy + 1, cz, true);              // soul-lantern (cold blue, the doused keep-light)

        // The decaying journal — the-fire-kept-me — on a lectern at the slab's other side (flavour, not an
        // answer surface; the answer is the blank sign below). The origin: the keeping was always a carrying.
        pen.set(cx + 1, cy, cz, Material.CHISELED_POLISHED_BLACKSTONE);
        pen.lectern(cx + 1, cy + 1, cz, BlockFace.WEST);
        pen.putBook(cx + 1, cy + 1, cz, "the fire kept me",
                "i went down to keep the\nfire and the fire kept\nme instead.\n\n" +
                        "the keeping was always\na carrying. read it back\nand carry it up.");

        // RUNE-CRIB "FIRE" — carved beside the magma/soul-fire seam (the deep fire-source this pocket is).
        // Mounted on the SOUTH face of the solid blackstone-brick back wall at (cx+1,cy,cz-2), facing the room,
        // right beside the ember-seam. crib: the referent is the magma/soul-fire directly west of this sign.
        pen.runeCrib(cx + 1, cy, cz - 1, BlockFace.SOUTH, deepslateWallSign(), "FIRE");

        // THE ANSWER: a blank UNWAXED answer-sign recessed on the slab's front, at the DEEPSLATE SLAB — the
        // AnswerSignListener reads a player's overwrite here (INV-14; the on-site WORD is read off the room —
        // the journal + the FIRE crib — never off the coordinate, and typed at the slab). Backed by the slab
        // plinth just south of the slab; a player reads/edits it standing on the dry floor at (cx,cy-1,cz+2).
        pen.set(cx, cy - 1, cz + 1, Material.CHISELED_DEEPSLATE);   // backing plinth for the answer sign
        pen.set(cx, cy, cz + 1, Material.DEEPSLATE_TILES);          // the sign's solid backing block
        Location answer = pen.wallSign(cx, cy, cz + 2, BlockFace.SOUTH, deepslateWallSign());

        // One faint glimmer only — a single glowstone stud low in the far corner, so the pocket is not pitch
        // black but the earned light is gone (the lantern is out). Sparse, dark-default.
        pen.setIfAir(cx - 2, cy, cz + 2, Material.SHROOMLIGHT);
        return answer;
    }

    /* ================================================================================================
     * END SHRINE — the Seventh's exile-shrine OUTSIDE the record (sites.yml `end_seventh_shrine`). An
     * end-stone/purpur/obsidian shrine holding the "the-name-i-cut-myself" leaf: a carving-slab where the
     * Seventh cut their own name, the-name-i-cut-myself. The Seventh's exile made a place — no kept fire,
     * no markers, no count. Built to the Seventh's unfinished, wrong-scaled hand (the_unwriting signature).
     * Modest, end-palette. Additive: seats its own end-stone plinth so it stands over the void-adjacent End
     * ground without overwriting real terrain beyond the footprint (S7: additive onto verified-clear cells).
     * Palette: end-stone brick + purpur + obsidian (the exile-stone) + a single amethyst (the deep-bird's
     * cool — the one light carried out). Prop: the carving-slab (the cut name) + the unfinished stopped edge.
     * Light: ONE amethyst (earned, cool, the only light outside the record). Wrongness: the shrine unfinished,
     * stopped mid-cut (the hand cast out before it could finish — the_unwriting's stopped-ceiling echo).
     * ============================================================================================== */
    private static Location endShrine(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

        // Post-Unlit visual overhaul: void-edge approach and elongated shrine body. The original 5x5
        // carving stays at the focal end so the answer surface remains stable.
        for (int dz = -12; dz <= -3; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                pen.set(cx + dx, cy - 1, cz + dz, Material.END_STONE_BRICKS);
            }
            if (dz % 3 == 0) {
                pen.set(cx - 2, cy - 1, cz + dz, Material.PURPUR_BLOCK);
                pen.set(cx + 2, cy - 1, cz + dz, Material.PURPUR_BLOCK);
            }
        }
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -2; dz <= 8; dz++) {
                boolean rim = Math.abs(dx) == 6 || dz == 8;
                pen.set(cx + dx, cy - 2, cz + dz, Material.END_STONE);
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.PURPUR_BLOCK : Material.END_STONE_BRICKS);
                if (rim) {
                    for (int dy = 0; dy <= 3; dy++) {
                        pen.set(cx + dx, cy + dy, cz + dz,
                                dy == 3 ? Material.OBSIDIAN
                                        : (dy == 1 ? Material.PURPUR_PILLAR : Material.END_STONE_BRICKS));
                    }
                }
            }
        }

        // 5x5 shrine floor: end-stone-brick field, purpur rim (the exile-stone; a made place with no count).
        // The builder seats its own floor so it stands over the End's void-adjacent ground — additive, it
        // claims only this footprint (S7: pasted onto its own verified-clear cells, never a void overwrite).
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                pen.set(cx + dx, cy - 1, cz + dz, rim ? Material.PURPUR_BLOCK : Material.END_STONE_BRICKS);
            }
        }
        // The back wall: end-stone brick banded with obsidian (the exile-stone), two courses. UNFINISHED —
        // one upper block left as raw end-stone and a chisel-stroke stair jutting mid-air (the Seventh's hand
        // cast out before it could finish; the_unwriting's stopped-cut signature, re-skinned end-palette).
        for (int dx = -2; dx <= 2; dx++) {
            pen.set(cx + dx, cy,     cz - 2, Material.END_STONE_BRICKS);
            pen.set(cx + dx, cy + 1, cz - 2, dx == 2 ? Material.END_STONE : Material.OBSIDIAN); // the stopped, unfinished course
        }
        pen.set(cx + 1, cy + 2, cz - 2, Material.PURPUR_STAIRS);   // a chisel-stroke left mid-cut (the unfinished hand)

        // THE CARVING-SLAB — the "the-name-i-cut-myself" leaf: a low canted purpur slab the Seventh cut their
        // own name on. A chiseled-purpur plinth capped with a canted purpur stair (stooped-to-read, the same
        // low reading-stone the deep sites use). This is the on-site read surface.
        pen.set(cx, cy - 1, cz, Material.END_STONE_BRICKS);
        pen.stairs(cx, cy, cz, Material.PURPUR_STAIRS, BlockFace.SOUTH, false);   // the canted carving face
        // The Seventh's own words on a lectern at the slab (the-name-i-cut-myself; flavour, the emotional read).
        pen.set(cx, cy, cz - 1, Material.CHISELED_QUARTZ_BLOCK);   // a clean pale backing for the cut name (unlike the dark deep)
        pen.lectern(cx, cy + 1, cz - 1, BlockFace.SOUTH);
        pen.putBook(cx, cy + 1, cz - 1, "the name i cut myself",
                "no one kept me, so i\ncut my own name here,\noutside the count.\n\n" +
                        "the record does not\nreach this far. only\nyou did.");

        // RUNE-CRIB "NAME" — carved beside the carving-slab (the name the Seventh cut for themselves). Mounted
        // on the SOUTH face of the pale quartz backing block at (cx,cy,cz-1), facing the room at the slab.
        // crib: the referent is the carving-slab directly south of this sign (the-name-i-cut-myself).
        pen.runeCrib(cx - 1, cy, cz - 1, BlockFace.SOUTH, Material.WARPED_WALL_SIGN, "NAME");

        // THE ANSWER: a blank UNWAXED answer-sign at the carving-slab — the AnswerSignListener reads the on-
        // site read here (INV-14; the WORD is read off the room — the leaf-book + the NAME crib — and typed at
        // the slab, never the coordinate). Recessed on the slab's front, backed by a purpur post; a player
        // reads/edits it standing on the dry end-stone at (cx,cy-1,cz+2), clear of the shrine's front edge.
        pen.set(cx, cy - 1, cz + 1, Material.PURPUR_PILLAR);       // backing post for the answer sign
        pen.set(cx, cy, cz + 1, Material.PURPUR_BLOCK);            // the sign's solid backing block
        Location answer = pen.wallSign(cx, cy, cz + 2, BlockFace.SOUTH, Material.WARPED_WALL_SIGN);

        // The ONE kept light carried this far out: a single amethyst on the back wall (the deep-bird's cool —
        // no kept fire out here, only this one cold light the Seventh brought). Earned, sparse, the only light.
        pen.set(cx - 2, cy + 1, cz - 2, Material.OBSIDIAN);
        pen.clusterOn(cx - 2, cy + 1, cz - 1, BlockFace.SOUTH);
        return answer;
    }

    /* ================================================================================================
     * PEN — the reusable stamping helper. Physics-off block sets, directional block-data (stairs, wall
     * signs, candles, bells, hanging signs), sign/lectern/book/decorated-pot builders, water + veining.
     * Every method is null/quirk-safe: a bad material or odd block never throws (Safety is the caller's
     * job, but a set-piece must never half-explode mid-build).
     * ============================================================================================== */
    private static final class Pen {
        private final World world;
        Pen(World world) { this.world = world; }

        /** Set a block with physics off (no cascade updates while stamping). Null/quirk-safe. */
        void set(int x, int y, int z, Material mat) {
            if (world == null || mat == null) return;
            try { world.getBlockAt(x, y, z).setType(mat, false); } catch (Throwable ignored) { }
        }

        /** Set only if the current block is air (used for delicate props: cobwebs, flowers). */
        void setIfAir(int x, int y, int z, Material mat) {
            if (world == null || mat == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() == Material.AIR) b.setType(mat, false);
            } catch (Throwable ignored) { }
        }

        /** A flat square floor of radius r (odd square) at height y. */
        void floor(int cx, int y, int cz, int r, Material mat) {
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    set(cx + dx, y, cz + dz, mat);
        }

        /** Water source block (still). */
        void water(int x, int y, int z) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.WATER, false);
                if (b.getBlockData() instanceof Levelled lv) { lv.setLevel(0); b.setBlockData(lv, false); }
            } catch (Throwable ignored) { }
        }

        /** A waterlogged plant/block (seagrass etc.) with waterlogged=true where supported. */
        void waterlogged(int x, int y, int z, Material mat) {
            if (world == null || mat == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(mat, false);
                if (b.getBlockData() instanceof Waterlogged wl) { wl.setWaterlogged(true); b.setBlockData(wl, false); }
            } catch (Throwable ignored) { }
        }

        /** A candle block, optionally lit. Null-safe on non-candle materials. */
        void candle(int x, int y, int z, Material mat, boolean lit) {
            if (world == null || mat == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(mat, false);
                if (b.getBlockData() instanceof Candle c) { c.setLit(lit); b.setBlockData(c, false); }
            } catch (Throwable ignored) { }
        }

        /** A campfire (regular), optionally lit. A doused campfire (lit=false) reads as a dead hearth. */
        void campfire(int x, int y, int z, boolean lit) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.CAMPFIRE, false);
                if (b.getBlockData() instanceof org.bukkit.block.data.type.Campfire c) {
                    c.setLit(lit);
                    if (b.getBlockData() instanceof Waterlogged wl) { wl.setWaterlogged(false); }
                    b.setBlockData(c, false);
                }
            } catch (Throwable ignored) { }
        }

        /** An amethyst cluster growing on a face (the learning/moon light). */
        void clusterOn(int x, int y, int z, BlockFace face) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.AMETHYST_CLUSTER, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(face); b.setBlockData(d, false); }
            } catch (Throwable ignored) { }
        }

        /** A few sculk-vein cells on the floor (top face), for veining a dais. */
        void veinFloor(int cx, int cy, int cz, int[][] cells) {
            if (world == null || cells == null) return;
            for (int[] c : cells) {
                try {
                    Block b = world.getBlockAt(cx + c[0], cy, cz + c[1]);
                    if (b.getType() != Material.AIR) continue;
                    b.setType(Material.SCULK_VEIN, false);
                } catch (Throwable ignored) { }
            }
        }

        /** Stairs facing a direction, upper or lower half. */
        void stairs(int x, int y, int z, Material mat, BlockFace facing, boolean top) {
            if (world == null || mat == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(mat, false);
                if (b.getBlockData() instanceof Stairs s) {
                    s.setFacing(facing);
                    s.setHalf(top ? org.bukkit.block.data.Bisected.Half.TOP : org.bukkit.block.data.Bisected.Half.BOTTOM);
                    b.setBlockData(s, false);
                }
            } catch (Throwable ignored) { }
        }

        /** A trapdoor as a chair-back: open, hinged against the given face. */
        void trapdoorBack(int x, int y, int z, BlockFace facing) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.DARK_OAK_TRAPDOOR, false);
                if (b.getBlockData() instanceof org.bukkit.block.data.type.TrapDoor td) {
                    td.setFacing(facing);
                    td.setOpen(true);
                    td.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
                    b.setBlockData(td, false);
                }
            } catch (Throwable ignored) { }
        }

        /** A bell attached to a wall, facing a direction. */
        void bell(int x, int y, int z, BlockFace facing) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.BELL, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
            } catch (Throwable ignored) { }
        }

        /**
         * A WALL banner: a base color + an ordered list of patterns, attached to the block behind
         * {@code facing} (mirrors the wall-sign helpers' "attach to the solid backing wall" idiom).
         * Used for keeper-heraldry set-pieces (Orin's mason-hall, {@code orin-banner-heraldry}) where
         * the banner ITSELF — its base color and pattern order — is the legible content, not text on
         * a sign. Null/quirk-safe like the other block-prop helpers.
         */
        void wallBanner(int x, int y, int z, BlockFace facing, DyeColor baseColor, List<Pattern> patterns) {
            if (world == null) return;
            try {
                Material mat = matOr(Material.WHITE_WALL_BANNER, baseColor.name() + "_WALL_BANNER");
                Block b = world.getBlockAt(x, y, z);
                b.setType(mat, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                if (b.getState() instanceof Banner banner) {
                    banner.setBaseColor(baseColor);
                    if (patterns != null) banner.setPatterns(patterns);
                    banner.update(true, false);
                }
            } catch (Throwable ignored) { }
        }

        /** A hanging lantern (attached below a block), optionally the soul variant. */
        void hangingLantern(int x, int y, int z, boolean soul) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(soul ? Material.SOUL_LANTERN : Material.LANTERN, false);
                if (b.getBlockData() instanceof org.bukkit.block.data.type.Lantern l) {
                    l.setHanging(true);
                    b.setBlockData(l, false);
                }
            } catch (Throwable ignored) { }
        }

        /**
         * A WALL sign attached to the block behind {@code facing}, at reading height, left UNWAXED and
         * BLANK — the diegetic answer-submission slot. Returns its location for site-radius resolution.
         */
        Location wallSign(int x, int y, int z, BlockFace facing, Material wallSignMat) {
            if (world == null) return null;
            try {
                Block b = world.getBlockAt(x, y, z);
                Material mat = wallSignMat != null && wallSignMat.name().contains("WALL_SIGN")
                        ? wallSignMat : Material.DARK_OAK_WALL_SIGN;
                b.setType(mat, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                // Leave blank + UNWAXED so AnswerSignListener resolves a player's overwrite as a submission.
                if (b.getState() instanceof Sign sign) {
                    var front = sign.getSide(Side.FRONT);
                    for (int i = 0; i < 4; i++) front.setLine(i, "");
                    try { sign.setWaxed(false); } catch (Throwable ignored) { }
                    try { sign.update(true, false); } catch (Throwable ignored) { }
                }
                return b.getLocation();
            } catch (Throwable t) {
                return null;
            }
        }

        /**
         * A WAXED label wall-sign carrying flavour text. WAXED so a player cannot edit it — meaning it is
         * NOT treated as a submission (it never pollutes answers; only the blank unwaxed slot does).
         */
        void labelWallSign(int x, int y, int z, BlockFace facing, Material wallSignMat, String[] lines) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                Material mat = wallSignMat != null && wallSignMat.name().contains("WALL_SIGN")
                        ? wallSignMat : Material.DARK_OAK_WALL_SIGN;
                b.setType(mat, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                if (b.getState() instanceof Sign sign) {
                    var front = sign.getSide(Side.FRONT);
                    for (int i = 0; i < 4; i++) front.setLine(i, clamp(lines, i));
                    try { sign.setWaxed(true); } catch (Throwable ignored) { }
                    try { sign.update(true, false); } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
        }

        /**
         * A RUNE-CRIB: a small WAXED wall-sign whose single word is rendered in the {@code observance:runes}
         * bitmap font (ASCII A–Z / 0–9 map 1:1 to rune glyphs), so it displays as a carved rune label beside
         * the thing it names — the earned-literacy "crib" (Chants-of-Sennaar / Tunic style). WAXED so it is
         * un-editable → never a submission slot and never pollutes an answer; it reads only as a carved word.
         *
         * <p>Uses the Paper rich-text Sign API ({@code side.line(i, Component)}) with the rune {@link
         * net.kyori.adventure.key.Key} — the SAME font key ({@code observance:runes}) the rune beats use
         * (see {@code NameOnWallBeat} / {@code KeeperNpcBeat}). Word is upper-cased so it hits the A–Z/0–9
         * glyph range of the font. Null/quirk-safe like the other sign helpers (try/catch Throwable).
         *
         * @param word the plain-ASCII word to carve (e.g. "FIRE"); rendered in rune glyphs.
         */
        void runeCrib(int x, int y, int z, BlockFace facing, Material wallSignMat, String word) {
            if (world == null || word == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                Material mat = wallSignMat != null && wallSignMat.name().contains("WALL_SIGN")
                        ? wallSignMat : Material.DARK_OAK_WALL_SIGN;
                b.setType(mat, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                if (b.getState() instanceof Sign sign) {
                    var side = sign.getSide(Side.FRONT);
                    // Rune glyphs: the font maps ASCII A–Z / 0–9 1:1, so upper-case the word first.
                    net.kyori.adventure.text.Component glyphs = net.kyori.adventure.text.Component
                            .text(word.toUpperCase(Locale.ROOT))
                            .font(net.kyori.adventure.key.Key.key("observance", "runes"));
                    side.line(0, net.kyori.adventure.text.Component.empty());
                    side.line(1, glyphs);
                    side.line(2, net.kyori.adventure.text.Component.empty());
                    side.line(3, net.kyori.adventure.text.Component.empty());
                    try { sign.setWaxed(true); } catch (Throwable ignored) { } // a carved label, never a slot
                    try { sign.update(true, false); } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
        }

        /**
         * A RUNE-CRIB PAIR — the Rosetta key itself: one waxed wall-sign carrying the SAME word twice, the
         * rune glyphs ({@code observance:runes}) on top and the plain letters below, so a player reads the
         * mapping directly ("oh — these glyphs ARE letters"). This is the earned-literacy turn made concrete
         * at the rosetta (the literacy gate); the rune-only {@link #runeCrib} is for practice elsewhere.
         * Waxed → never a submission slot. Null/quirk-safe like the other sign helpers.
         */
        void runeCribPair(int x, int y, int z, BlockFace facing, Material wallSignMat, String word) {
            if (world == null || word == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                Material mat = wallSignMat != null && wallSignMat.name().contains("WALL_SIGN")
                        ? wallSignMat : Material.DARK_OAK_WALL_SIGN;
                b.setType(mat, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                if (b.getState() instanceof Sign sign) {
                    var side = sign.getSide(Side.FRONT);
                    net.kyori.adventure.text.Component glyphs = net.kyori.adventure.text.Component
                            .text(word.toUpperCase(Locale.ROOT))
                            .font(net.kyori.adventure.key.Key.key("observance", "runes"));
                    side.line(0, net.kyori.adventure.text.Component.empty());
                    side.line(1, glyphs);                                            // the runes (the unknown)
                    side.line(2, net.kyori.adventure.text.Component.text(word.toLowerCase(Locale.ROOT))); // the key
                    side.line(3, net.kyori.adventure.text.Component.empty());
                    try { sign.setWaxed(true); } catch (Throwable ignored) { }
                    try { sign.update(true, false); } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
        }

        /** A hanging sign on chains under an overhang, facing a direction, carrying a small label. */
        void hangingSign(int x, int y, int z, BlockFace facing, String[] lines) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.WARPED_HANGING_SIGN, false); // wall/ceiling hanging sign
                if (b.getBlockData() instanceof Rotatable r) {
                    r.setRotation(facing);
                    b.setBlockData(r, false);
                }
                if (b.getState() instanceof Sign sign) {
                    var front = sign.getSide(Side.FRONT);
                    for (int i = 0; i < 4; i++) front.setLine(i, clamp(lines, i));
                    try { sign.setWaxed(true); } catch (Throwable ignored) { } // way-marks are labels, not slots
                    try { sign.update(true, false); } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
        }

        /** A lectern facing a direction. Returns its location (a valid answer surface for the listener). */
        Location lectern(int x, int y, int z, BlockFace facing) {
            if (world == null) return null;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.LECTERN, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                return b.getLocation();
            } catch (Throwable t) {
                return null;
            }
        }

        /** Put a written book on an existing lectern (flavour; not an answer surface). Title is clamped to
         *  a realistic tooltip width. Authored pages here use explicit {@code \n} for intentional
         *  line/paragraph breaks (Minecraft's book client honors a literal newline as a hard break — e.g.
         *  Mara's book sets off "[in the margin, her hand:]" from the main text this way), so the raw text
         *  is kept AS-authored whenever it fits the real visible-page budget. Only a page that actually
         *  exceeds that budget falls back to {@link com.observance.watcher.util.TextFit#paginate}, which
         *  would otherwise flatten those intentional breaks — this closes the real future-overflow risk
         *  without altering any currently-authored book's layout. */
        void putBook(int x, int y, int z, String title, String page) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                if (!(b.getState() instanceof Lectern lectern)) return;
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                if (book.getItemMeta() instanceof BookMeta meta) {
                    meta.setTitle(com.observance.watcher.util.TextFit.clampLine(title == null ? "—" : title, 32));
                    meta.setAuthor("the kept");
                    String body = page == null ? "" : page;
                    if (body.length() <= com.observance.watcher.util.TextFit.BOOK_PAGE_CHARS) {
                        meta.addPage(body);
                    } else {
                        for (String real : com.observance.watcher.util.TextFit.paginate(body)) {
                            meta.addPage(real);
                        }
                    }
                    book.setItemMeta(meta);
                }
                lectern.getInventory().setItem(0, book);
                lectern.update(true, false);
            } catch (Throwable ignored) { }
        }

        /**
         * A small chest facing a direction, holding a single found-object {@link ItemStack} in its
         * first slot — the "a found container holds a found item" placement idiom (mirrors
         * {@link #putBook}'s "place a prop, then fill it" shape, but for a normal chest instead of a
         * lectern). Used for set-piece finds that must be discoverable in the world rather than
         * handed out by an admin command (e.g. Iss's keepsake lamp). Null/quirk-safe.
         */
        void chestWithItem(int x, int y, int z, BlockFace facing, ItemStack contents) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.CHEST, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(facing); b.setBlockData(d, false); }
                if (contents != null && b.getState() instanceof org.bukkit.block.Chest chest) {
                    chest.getBlockInventory().setItem(0, contents);
                    chest.update(true, false);
                }
            } catch (Throwable ignored) { }
        }

        /** A decorated pot with up to four sherds (by Sherds pattern-name; unknown names are skipped). */
        void decoratedPot(int x, int y, int z, String back, String left, String right, String front) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                b.setType(Material.DECORATED_POT, false);
                if (b.getBlockData() instanceof Directional d) { d.setFacing(BlockFace.SOUTH); b.setBlockData(d, false); }
                if (b.getState() instanceof org.bukkit.block.DecoratedPot pot) {
                    sherd(pot, org.bukkit.block.DecoratedPot.Side.BACK, back);
                    sherd(pot, org.bukkit.block.DecoratedPot.Side.LEFT, left);
                    sherd(pot, org.bukkit.block.DecoratedPot.Side.RIGHT, right);
                    sherd(pot, org.bukkit.block.DecoratedPot.Side.FRONT, front);
                    pot.update(true, false);
                }
            } catch (Throwable ignored) { }
        }

        private void sherd(org.bukkit.block.DecoratedPot pot, org.bukkit.block.DecoratedPot.Side side, String name) {
            if (name == null || name.isBlank()) return;
            try {
                Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT) + "_POTTERY_SHERD");
                if (m != null) pot.setSherd(side, m);
            } catch (Throwable ignored) { }
        }

        /** Nearest cardinal BlockFace pointing from (fx,fz) toward (tx,tz). */
        BlockFace toward(int fx, int fz, int tx, int tz) {
            int dx = tx - fx, dz = tz - fz;
            if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? BlockFace.EAST : BlockFace.WEST;
            return dz >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }

        private static String clamp(String[] lines, int i) {
            if (lines == null || i >= lines.length || lines[i] == null) return "";
            // Route through TextFit's real vanilla sign-line limit (15 chars, matching the sign-edit
            // screen itself) instead of a stale local 100-char safety ceiling — every authored line here
            // happens to fit under 15 today, but a future longer line would have silently overflowed
            // the sign with no warning under the old ceiling.
            return com.observance.watcher.util.TextFit.clampLine(lines[i], com.observance.watcher.util.TextFit.SIGN_LINE_CHARS);
        }
    }
}
