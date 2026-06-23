package com.observance.watcher.signal;

import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Scheduled inventory/hoard scanner (DESIGN §2.1). Runs on the MAIN thread (reads live player
 * inventories) on a slow cadence and computes a per-player <b>hoard score</b> = sum over carried
 * items of {@code count * weight(material)}, capped. The score feeds the dossier and lets the
 * engine recognize "the solo-miner who hoards" (the Hoarder rhyme).
 *
 * PURE TRACKING: reads inventories, writes only in-memory signals. No world effects, no item
 * mutation. The body is invoked via the Safety-wrapped scheduler; the per-player loop is guarded
 * so one odd inventory can't abort the sweep.
 */
public final class InventoryScanner {

    private final SignalTracker tracker;
    private final Safety safety;

    public InventoryScanner(SignalTracker tracker, Safety safety) {
        this.tracker = tracker;
        this.safety = safety;
    }

    /** MAIN-thread scan tick. */
    public void scanTick() {
        TrackerConfig cfg = tracker.config();
        if (!cfg.enabled()) return;

        // Build a fast material→weight lookup once per tick.
        Map<Material, Double> weights = buildWeightMap(cfg);
        if (weights.isEmpty()) return;
        double cap = cfg.hoardScoreCap();

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        for (Player p : online) {
            safety.run("sampler.inventory.player", () -> scanPlayer(p, weights, cap));
        }
    }

    private void scanPlayer(Player p, Map<Material, Double> weights, double cap) {
        if (p == null || !p.isOnline()) return;
        PlayerInventory inv = p.getInventory();
        if (inv == null) return;

        double score = 0.0;
        // Explicitly cover storage + armor + offhand so the count is complete regardless of
        // getContents() platform semantics, and de-dup is unnecessary (disjoint slot arrays).
        score += scoreContents(inv.getStorageContents(), weights);  // main + hotbar (36)
        score += scoreContents(inv.getArmorContents(), weights);    // 4 armor slots
        score += scoreItem(inv.getItemInOffHand(), weights);        // offhand
        if (score > cap) score = cap;

        PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
        ps.setHoardedScore(score);
    }

    private double scoreContents(ItemStack[] contents, Map<Material, Double> weights) {
        if (contents == null) return 0.0;
        double sum = 0.0;
        for (ItemStack stack : contents) {
            sum += scoreItem(stack, weights);
        }
        return sum;
    }

    private double scoreItem(ItemStack stack, Map<Material, Double> weights) {
        if (stack == null) return 0.0;
        Material m = stack.getType();
        if (m == null || m == Material.AIR) return 0.0;
        Double w = weights.get(m);
        if (w == null) return 0.0;
        int amount = Math.max(0, stack.getAmount());
        return amount * w;
    }

    private Map<Material, Double> buildWeightMap(TrackerConfig cfg) {
        Map<Material, Double> map = new HashMap<>();
        for (TrackerConfig.HoardWeight hw : cfg.hoardWeights()) {
            Material m = matchMaterial(hw.material());
            if (m != null) map.put(m, hw.weight());
        }
        return map;
    }

    /** Resolve a material name defensively (unknown names are skipped, never thrown). */
    private Material matchMaterial(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable t) {
            return null;
        }
    }
}
