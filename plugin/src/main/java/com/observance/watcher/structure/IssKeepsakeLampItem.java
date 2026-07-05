package com.observance.watcher.structure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * The keepsake lamp — Iss's NBT-heavy item stego (design/PUZZLE-DESIGNS.md §7.2, {@code
 * iss-nbt-falsified-entry}). A warm-worded "gift" whose custom NBT hides the falsified record
 * entry Iss wrote about the Seventh (the lie that the Seventh was spared / shown a mercy). Built
 * to the SAME idiom as {@link com.observance.watcher.lens.LensItem}: a plain material + a durable
 * PersistentDataContainer tag, decorated defensively (a decoration failure must never prevent
 * handing out a functional item). Unlike the Lens, this is a FOUND OBJECT (placed once in Iss's
 * site by {@link StructureTemplates#keeper}, never handed out by an admin command) — the datamine
 * is the find (PUZZLES.md §1/§5-Iss, D7: "leave a message for the xrayer").
 *
 * <p><b>The hidden field.</b> A base64 string under a custom PDC key decodes to a short in-world
 * "found document" line followed by the doctored record line itself — the exact text a vanilla-
 * savvy player who inspects the item's NBT (F3+I, or any NBT viewer) will read once decoded. The
 * doctored-record line is byte-identical to one of {@code iss-nbt-falsified-entry}'s seeded
 * {@code accepted_answers} (puzzles_seed.sql), so re-submitting the decoded line at any existing
 * answer surface (the same {@code AnswerSignListener} verb every other world puzzle uses) resolves
 * the puzzle — no new submission mechanism, no Discord-side change needed.
 *
 * <p>Lore-agnostic at the class level (mirrors LensItem): the encoded STRING is the one place this
 * class carries authored narrative text, because it must byte-match the seed row it answers.
 */
public final class IssKeepsakeLampItem {

    private IssKeepsakeLampItem() { }

    /** A small warm light — a lamp is quite literally what the design calls it. */
    public static final Material MATERIAL = Material.LANTERN;

    /** PDC sub-key: the durable "this IS the keepsake lamp" recognition marker (schema version). */
    private static final String PDC_SUB_MARKER = "iss_keepsake_lamp";

    /** PDC sub-key: the hidden base64 field a datamining player decodes (the puzzle's stego payload). */
    private static final String PDC_SUB_PAYLOAD = "iss_keepsake_lamp_data";

    /** Current schema version (bumpable if the item's NBT contract changes). */
    private static final int VERSION = 1;

    /**
     * The doctored record entry — byte-identical to one of {@code iss-nbt-falsified-entry}'s
     * accepted_answers in puzzles_seed.sql (discord-side). If that seed row's accepted_answers
     * ever changes, THIS STRING must change to match, or the decoded NBT will no longer resolve
     * the puzzle when re-submitted at an answer sign.
     */
    private static final String DOCTORED_ENTRY = "he wrote the seventh a mercy it was not";

    /**
     * The decoded payload a datamining player sees: the flavor line PUZZLES.md quotes verbatim
     * ("meant to be inspected; the datamine is the find") followed by the doctored entry itself,
     * so the same read that rewards the datamine ALSO hands the player the exact phrase to submit.
     */
    private static String decodedPayload() {
        return "you looked. good. he counted on no one looking.\n" + DOCTORED_ENTRY;
    }

    /** The PDC key under the plugin namespace for the recognition marker. */
    public static NamespacedKey markerKey(String namespace) {
        return new NamespacedKey(namespace == null || namespace.isBlank() ? "observance" : namespace, PDC_SUB_MARKER);
    }

    /** The PDC key under the plugin namespace for the hidden base64 payload. */
    public static NamespacedKey payloadKey(String namespace) {
        return new NamespacedKey(namespace == null || namespace.isBlank() ? "observance" : namespace, PDC_SUB_PAYLOAD);
    }

    /**
     * Build a fresh keepsake-lamp {@link ItemStack}: PDC-tagged (recognition marker + the hidden
     * base64 payload), named/lored with an in-world-plausible "gift" framing that never spoils the
     * decode. Never throws; returns a plain lantern if meta is somehow unavailable (still tagged
     * where possible), matching {@code LensItem.create}'s defensive contract.
     *
     * @param namespace the plugin namespace for the PDC keys (e.g. "observance").
     */
    public static ItemStack create(String namespace) {
        ItemStack item = new ItemStack(MATERIAL, 1);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                var pdc = meta.getPersistentDataContainer();
                pdc.set(markerKey(namespace), PersistentDataType.INTEGER, VERSION);
                String b64 = Base64.getEncoder().encodeToString(
                        decodedPayload().getBytes(StandardCharsets.UTF_8));
                pdc.set(payloadKey(namespace), PersistentDataType.STRING, b64);
                meta.displayName(Component.text("Keepsake Lamp")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("A small warm gift, from the warmest keeper.")
                                .color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, true),
                        Component.text("It carries more than it shows.")
                                .color(NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, true)));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // A failure to decorate must never prevent placing a (functional) find.
        }
        return item;
    }

    /**
     * True iff {@code item} carries the keepsake-lamp PDC marker. Recognition is by tag ONLY (mirrors
     * {@code LensItem.isLens}), tolerant of null / no-meta / no-PDC. Never throws.
     */
    public static boolean isKeepsakeLamp(ItemStack item, String namespace) {
        if (item == null || item.getType() != MATERIAL) return false;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;
            return meta.getPersistentDataContainer().has(markerKey(namespace), PersistentDataType.INTEGER);
        } catch (Throwable t) {
            return false;
        }
    }
}
