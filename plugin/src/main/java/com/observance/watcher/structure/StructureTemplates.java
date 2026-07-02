package com.observance.watcher.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

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
     * ROSETTA — the literacy gate. A ring of 6 short inscribed polished-blackstone pillars around a
     * central lectern on a sculk-veined deepslate-brick dais; an amethyst cluster at the heart (the
     * learning-light). Answer = the central lectern.
     * Palette: polished blackstone + deepslate brick. Prop: 6 way-mark pillars w/ hanging signs. Light:
     * amethyst (earned, cool). Wrongness: one pillar's hanging sign faces inward/blank + a cobweb.
     * ============================================================================================== */
    private static Location rosetta(Pen pen, Location base) {
        int cx = base.getBlockX(), cy = base.getBlockY(), cz = base.getBlockZ();

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
        int[][] ring = {{0, -3}, {3, -1}, {3, 2}, {0, 3}, {-3, 2}, {-3, -1}};
        String[] marks = {"i", "ii", "iii", "iv", "v", "vi"};
        for (int i = 0; i < ring.length; i++) {
            int px = cx + ring[i][0], pz = cz + ring[i][1];
            pen.set(px, cy, pz, Material.POLISHED_BLACKSTONE_BRICKS);
            pen.set(px, cy + 1, pz, Material.POLISHED_BLACKSTONE);
            pen.set(px, cy + 2, pz, Material.CHISELED_POLISHED_BLACKSTONE);
            // A hanging sign on chains under a small overhang block, facing the centre (a way-mark).
            pen.set(px, cy + 4, pz, Material.POLISHED_BLACKSTONE_SLAB);
            BlockFace toCentre = pen.toward(px, pz, cx, cz);
            boolean wrong = i == 3; // one way-mark faces OUT and is left blank — the wrongness detail
            pen.hangingSign(px, cy + 3, pz, wrong ? toCentre.getOppositeFace() : toCentre,
                    wrong ? new String[]{"", "", "", ""} : new String[]{"·", marks[i], "·", ""});
        }
        // Wrongness: a cobweb clinging in one corner of the dais.
        pen.setIfAir(cx - 3, cy, cz - 3, Material.COBWEB);

        // A separate WAXED label sign (does not pollute answers) on the dais front rim.
        pen.labelWallSign(cx, cy, cz + 3, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"the rosetta", "six ways, one", "hand — read,", "then answer"});
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
        // Separate WAXED label — the ledger's heading (does not pollute answers).
        pen.labelWallSign(cx + 1, cy + 1, cz - 1, BlockFace.SOUTH, Material.SPRUCE_WALL_SIGN,
                new String[]{"vaun's ledger", "all of it kept", "none of it", "spent"});
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

        // The reading lectern (THE ANSWER) with a book already on it — the margin-note lives on a sign.
        Location answer = pen.lectern(cx, cy, cz + 1, BlockFace.NORTH);
        pen.putBook(cx, cy, cz + 1, "the lampwright's hand",
                "i can't keep them all lit —\nthe words stay when the\nlamps do not.");

        // Reading candles on a small deepslate-tile side table — sparse, warm.
        pen.set(cx - 1, cy, cz + 1, Material.DEEPSLATE_TILE_SLAB);
        pen.candle(cx - 1, cy + 1, cz + 1, Material.CANDLE, true);

        // A second lectern with no book (a place someone stopped reading).
        pen.lectern(cx + 2, cy, cz - 1, BlockFace.WEST);

        // Heavy cobwebs/dust — the wrongness (a room read in, never left).
        pen.setIfAir(cx - 2, cy + 2, cz - 2, Material.COBWEB);
        pen.setIfAir(cx + 2, cy + 2, cz + 1, Material.COBWEB);
        pen.setIfAir(cx, cy + 2, cz, Material.COBWEB);

        // The "margin-note" answer sign — a small wall sign beside the lectern, BLANK (submission slot).
        // (The lectern is the primary answer surface; this sign is a second slot integrated as a margin.)
        pen.labelWallSign(cx + 1, cy + 1, cz - 2, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"in the margin:", "\"read it back", "to me —\"", "(her hand)"});
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

        // The copybook lectern at the pool's edge (a child practicing letters).
        pen.lectern(cx - 2, cy, cz, BlockFace.EAST);
        pen.putBook(cx - 2, cy, cz, "sella's copybook",
                "i drew the dark before\nthey would look at it.\n\na a a  b b b");

        // THE ANSWER: a dark-prismarine slab set flush at the water's edge, readable looking down into
        // the pool. Answer sign recessed low so it reads like writing under the surface (a submission slot).
        pen.set(cx, cy - 1, cz - 2, Material.DARK_PRISMARINE);
        Location answer = pen.wallSign(cx, cy, cz - 1, BlockFace.SOUTH, Material.WARPED_WALL_SIGN);

        // Separate WAXED "far marker" label sign, hung above (does not pollute answers).
        pen.labelWallSign(cx, cy + 1, cz - 2, BlockFace.SOUTH, Material.WARPED_WALL_SIGN,
                new String[]{"far marker —", "read what the", "water keeps", "still"});
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

        // Beyond the threshold: an unfinished/broken carving on the back wall (Orin carving the same line).
        for (int dx = -1; dx <= 1; dx++) {
            pen.set(cx + dx, cy,     cz + 3, Material.STONE_BRICKS);
            pen.set(cx + dx, cy + 1, cz + 3, Material.STONE_BRICKS);
        }
        pen.set(cx, cy + 1, cz + 3, Material.CHISELED_STONE_BRICKS);
        pen.set(cx - 1, cy, cz + 3, Material.CRACKED_STONE_BRICKS);   // the unfinished carving
        pen.set(cx + 1, cy + 1, cz + 3, Material.STONE_BRICK_STAIRS); // a chisel-stroke left mid-cut

        // One soul lantern past the threshold — cold blue light (earned, sparse).
        pen.set(cx, cy + 2, cz + 2, Material.DEEPSLATE_BRICKS);
        pen.hangingLantern(cx, cy + 1, cz + 2, true);

        // THE ANSWER: a carved threshold-stone LOW to the floor just past the lintel, so you must stoop
        // (bow) to read/edit it. Answer sign at cy on the back stone (blank submission slot).
        Location answer = pen.wallSign(cx, cy, cz + 2, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN);
        // Separate WAXED label ABOVE the lintel (read before you stoop) — does not pollute answers.
        pen.labelWallSign(cx, cy + 2, cz - 1, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"orin's threshold", "the low stone", "asks a bow —", "stoop to read"});
        return answer;
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

        // THE ANSWER: a watch-stone the fire lights — a chiseled block facing the campfire with the
        // answer sign (blank submission slot) so firelight falls on it.
        pen.set(cx, cy, cz + 1, Material.CHISELED_DEEPSLATE);
        Location answer = pen.wallSign(cx, cy, cz, BlockFace.NORTH, Material.DARK_OAK_WALL_SIGN);
        // Separate WAXED label sign on the watch-post (does not pollute answers).
        pen.labelWallSign(cx - 2, cy + 1, cz - 2, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"brann's watch", "count the black", "moons — do not", "sleep"});
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

        // THE ANSWER: a warm-looking sign on the cold dead hearth (a birch/warm-wood sign against black
        // stone — the deception). Blank submission slot at reading height on the hearth face.
        pen.set(cx - 1, cy + 1, cz - 1, Material.CHISELED_POLISHED_BLACKSTONE);
        Location answer = pen.wallSign(cx - 1, cy + 1, cz, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN);
        // Separate WAXED label — the inviting lie (does not pollute answers).
        pen.labelWallSign(cx + 1, cy + 1, cz - 1, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN,
                new String[]{"come and warm", "yourself —", "the fire is", "kept (it lies)"});
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
        // THE ANSWER: the inscribed slab-face — a blank unwaxed submission slot on the table's front.
        pen.set(cx, cy, cz - 1, Material.CHISELED_DEEPSLATE);
        Location answer = pen.wallSign(cx, cy, cz, BlockFace.SOUTH, deepslateWallSign());

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
        pen.set(cx + 3, cy, cz, Material.CHISELED_DEEPSLATE);         // E arm
        pen.set(cx, cy - 1, cz, Material.CHISELED_DEEPSLATE);         // down arm (under the table itself)
        // Wrongness: the E arm scored through — a cracked block, someone disputed the reckoning.
        pen.set(cx + 3, cy, cz + 1, Material.CRACKED_DEEPSLATE_TILES);

        // Separate WAXED label — the reckoning heading (does not pollute the answer slot).
        pen.labelWallSign(cx, cy, cz + 3, BlockFace.SOUTH, deepslateWallSign(),
                new String[]{"the reckoning", "count the marks,", "then the way —", "north, down, read"});
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
        // Separate WAXED label — the dead-shrine's cold truth.
        pen.labelWallSign(cx - 1, cy + 1, cz - 1, BlockFace.SOUTH, Material.BIRCH_WALL_SIGN,
                new String[]{"nothing is kept", "here. he sent", "you out —", "the fire is out"});
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
        // Separate WAXED label at the ring's edge — the one instruction, sparse.
        pen.labelWallSign(cx, cy, cz + 4, BlockFace.SOUTH, Material.WARPED_WALL_SIGN,
                new String[]{"unbroken light", "one fire, kept.", "bow as one —", "all who are here"});
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

        // THE ANSWER-LINTEL: the true-walk arrival word is cut on the OPEN lintel (a door let stand). A blank
        // unwaxed submission slot on the lintel's inner face, read stooped. Returned as the answer surface.
        Location answer = pen.wallSign(cx, cy, cz + 1, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN);
        // Separate WAXED grave-marker — the future date, and who opened it. (Does not pollute the answer.)
        pen.labelWallSign(cx, cy, cz + 3, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"the date is not", "yet come. the", "stone is open", "from the inside"});
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

        // Separate WAXED label — the seal's truth (the deep is sealed with the WITHHOLDING of a name).
        pen.labelWallSign(cx - 2, cy + 1, cz - 3, BlockFace.SOUTH, Material.WARPED_WALL_SIGN,
                new String[]{"the seal is a", "name. the wall", "was scraped —", "read it back"});
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
        // Separate WAXED label on the rune-wall — the co-op instruction (does not pollute the combination).
        pen.labelWallSign(cx, cy + 1, cz + 3, BlockFace.SOUTH, Material.DARK_OAK_WALL_SIGN,
                new String[]{"the threshold", "vault. each holds", "one rune —", "read them as one"});
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

        /** Put a written book on an existing lectern (flavour; not an answer surface). */
        void putBook(int x, int y, int z, String title, String page) {
            if (world == null) return;
            try {
                Block b = world.getBlockAt(x, y, z);
                if (!(b.getState() instanceof Lectern lectern)) return;
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                if (book.getItemMeta() instanceof BookMeta meta) {
                    meta.setTitle(title == null ? "—" : title);
                    meta.setAuthor("the kept");
                    meta.addPage(page == null ? "" : page);
                    book.setItemMeta(meta);
                }
                lectern.getInventory().setItem(0, book);
                lectern.update(true, false);
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
            String s = lines[i];
            return s.length() > 100 ? s.substring(0, 100) : s;
        }
    }
}
