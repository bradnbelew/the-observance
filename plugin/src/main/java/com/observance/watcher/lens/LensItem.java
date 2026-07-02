package com.observance.watcher.lens;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The Lens — the "second sight" relic (INTEGRATION §SIGNATURE #3, config item conventions). A held
 * {@code custom_model_data} item that, WHILE HELD, reveals per-player runes/clues otherwise invisible
 * (the {@link LensRegistry}-gated displays). This class is the single source of truth for what the item
 * IS: its material, model-data, display name/lore, and — load-bearing — its PDC tag. Recognition is by
 * the PDC tag, NEVER by display name or model-data, so a renamed or resource-packless copy still counts
 * as the Lens (and a vanilla item that merely shares the model-data never does).
 *
 * <p>Lore-agnostic base: the visible name/lore below are a neutral default; the resource pack supplies
 * the actual model via {@link #MODEL_DATA}. Story text elsewhere still lives in Supabase/payloads.
 */
public final class LensItem {

    private LensItem() { }

    /** Base material of the relic — a spyglass reads as "look through me" and already has a use-anim. */
    public static final Material MATERIAL = Material.SPYGLASS;

    /** custom_model_data the resource pack keys the Lens model off of (config item convention). */
    public static final int MODEL_DATA = 4711;

    /** PDC sub-key: the durable "this IS the Lens" marker. Value is a schema version for forward-compat. */
    private static final String PDC_SUB = "lens";

    /** Current Lens item schema version (bumpable if the item's contract changes). */
    private static final int VERSION = 1;

    /** The PDC key under the plugin namespace. */
    public static NamespacedKey key(String namespace) {
        return new NamespacedKey(namespace == null || namespace.isBlank() ? "observance" : namespace, PDC_SUB);
    }

    /**
     * Build a fresh Lens {@link ItemStack}. PDC-tagged (the recognition marker), custom-model-data set
     * (for the pack), named + lored with a neutral default. Never throws; returns a plain spyglass if
     * meta is somehow unavailable (still tagged where possible).
     *
     * @param namespace the plugin namespace for the PDC key (e.g. "observance").
     */
    public static ItemStack create(String namespace) {
        ItemStack item = new ItemStack(MATERIAL, 1);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        key(namespace), PersistentDataType.INTEGER, VERSION);
                try { meta.setCustomModelData(MODEL_DATA); } catch (Throwable ignored) { }
                meta.displayName(Component.text("The Lens")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("Hold it, and look again.")
                                .color(NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, true)));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // A failure to decorate must never prevent handing out a (functional) relic.
        }
        return item;
    }

    /**
     * True iff {@code item} carries the Lens PDC marker. Tolerant of null / no-meta / no-PDC; recognition
     * is by tag ONLY, so renamed copies still count and look-alikes never do. Never throws.
     */
    public static boolean isLens(ItemStack item, String namespace) {
        if (item == null || item.getType() != MATERIAL) return false;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;
            return meta.getPersistentDataContainer().has(key(namespace), PersistentDataType.INTEGER);
        } catch (Throwable t) {
            return false;
        }
    }
}
