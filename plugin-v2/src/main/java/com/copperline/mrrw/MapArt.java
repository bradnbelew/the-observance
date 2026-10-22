package com.copperline.mrrw;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Pictures of Mossfield drawn from the canon anchors: Theo's yearly maps (C11) and the meadow
 * map art (D8). One pixel = 4 blocks over a 512x512 area centred on spawn.
 * "withLowpath" draws Morrow's house on its fabricated lot; "extra" marks (players' homes) appear
 * only when Morrow redraws the meadow in Act 9.
 */
public final class MapArt {
    static final int SIZE = 128, SCALE = 4, HALF = SIZE * SCALE / 2;
    private static final String[] HOUSES = {"rookery_house", "cairn_storehouse_door_2011", "june_house", "june_tower",
            "patchcord_relay_hut", "finch_hub", "spawn"};

    public static BufferedImage town(Canon canon, boolean withLowpath, List<double[]> extra) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(110, 160, 70));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(150, 150, 150));
        int[] spawn = canon.anchor("spawn");
        for (String h : HOUSES) {  // paths from spawn
            int[] a = canon.anchor(h);
            g.drawLine(px(spawn[0]), px(spawn[2]), px(a[0]), px(a[2]));
        }
        int[] rail = canon.anchor("south_rail_end");
        g.setColor(new Color(120, 90, 60));
        g.drawLine(px(canon.anchor("finch_hub")[0]), px(canon.anchor("finch_hub")[2]), px(rail[0]), px(rail[2]));
        g.setColor(Color.WHITE);
        for (String h : HOUSES) {
            int[] a = canon.anchor(h);
            g.fillRect(px(a[0]) - 2, px(a[2]) - 2, 4, 4);
        }
        if (withLowpath) {
            int[] l = canon.anchor("lowpath_place");
            g.setColor(new Color(200, 200, 200));
            g.fillRect(px(l[0]) - 2, px(l[2]) - 2, 4, 4);
        }
        g.setColor(new Color(240, 240, 240));
        if (extra != null) for (double[] p : extra) g.fillRect(px((int) p[0]) - 2, px((int) p[2]) - 2, 4, 4);
        g.dispose();
        return img;
    }

    static int px(int world) {
        return Math.max(0, Math.min(SIZE - 1, (world + HALF) / SCALE));
    }

    public static final class Renderer extends MapRenderer {
        private final BufferedImage img;
        private boolean drawn;

        public Renderer(BufferedImage img) {
            super(false);
            this.img = img;
        }

        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            if (drawn) return;
            canvas.drawImage(0, 0, img);
            drawn = true;
        }
    }

    /**
     * Lay an image into the ground as blocks: readable only from above, on a map (D8). The picture
     * follows the ground (2026-10 audit: laid flat at one height it was buried under every hill and
     * floated over every hollow, and the trees hid the rest from a map). Theo cleared the field for
     * it: trees rooted in the meadow are felled, plants go, and the blocks replace each column's top.
     * Rails, paths' cobble and anything else built is left standing; its own cell keeps its colour.
     * Redrawing (Act 9) lays the new picture over the old one the same way.
     */
    public static void layMeadow(World w, int x0, int y, int z0, BufferedImage img) {
        Terrain.fell(w, x0, z0, x0 + SIZE - 1, z0 + SIZE - 1, y);
        for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) {
            Color c = new Color(img.getRGB(x, z));
            Material m;
            if (c.getRed() > 230 && c.getGreen() > 230) m = Material.WHITE_WOOL;
            else if (c.getRed() > 180) m = Material.LIGHT_GRAY_WOOL;
            else if (c.getRed() == 150) m = Material.GRAVEL;
            else if (c.getRed() == 120) m = Material.DIRT_PATH;
            else m = Material.GRASS_BLOCK;
            int wx = x0 + x, wz = z0 + z;
            int g = Terrain.ground(w, wx, wz);
            for (int yy = g + 1; yy <= g + 3; yy++) {                    // grass, flowers, the old poppies
                Block above = w.getBlockAt(wx, yy, wz);
                if (Terrain.cover(above.getType()) && !above.getType().isAir()) above.setType(Material.AIR, false);
            }
            Block top = w.getBlockAt(wx, g, wz);
            if (!Terrain.natural(top.getType()) && !meadowBlock(top.getType()) && top.getType() != Material.WATER) continue;  // built: keep
            top.setType(m, false);
            if (m == Material.GRASS_BLOCK && ((x * 7 + z * 13) % 11 == 0)) w.getBlockAt(wx, g + 1, wz).setType(Material.POPPY, false);
        }
    }

    private static boolean meadowBlock(Material m) {
        return m == Material.WHITE_WOOL || m == Material.LIGHT_GRAY_WOOL;
    }
}
