package com.copperline.mrrw;

import java.util.List;
import java.util.Map;

/**
 * D11, the sound replay in patchcord's hut: one sound + one real vibration per nibble of the
 * filename. Each frequency is replayed with a game event whose vanilla Java vibration frequency
 * IS that nibble, and with a sound whose subtitle names an action that naturally emits that same
 * game event (so reading subtitles and reading a sculk sensor/comparator agree).
 * Pure data, no Bukkit types: checked by {@code SelfTest}.
 *
 * Frequencies: Java Edition 1.20+ vibration table (minecraft.wiki/w/Sculk_Sensor), re-checked live
 * on Paper 1.21.11 against a sculk sensor's last_vibration_frequency.
 */
public final class Vibrations {
    private Vibrations() { }

    /** A normal sculk sensor is active 30 ticks then cools down 10; leave a margin so it hears every event. */
    public static final int SPACING_TICKS = 50;
    public static final int MIN_SPACING_TICKS = 45;
    public static final int LEAD_IN_TICKS = 40;

    /** frequency -> {sound key, game event key}. The sound's natural game event is the same event. */
    public static final Map<Integer, String[]> REPLAY = Map.ofEntries(
            Map.entry(1, new String[] {"block.stone.step", "step"}),                 // Footsteps
            Map.entry(2, new String[] {"entity.generic.small_fall", "hit_ground"}),  // Something trips
            Map.entry(3, new String[] {"entity.arrow.shoot", "projectile_shoot"}),   // Arrow fired
            Map.entry(4, new String[] {"entity.wolf.shake", "entity_action"}),       // Wolf shakes
            Map.entry(5, new String[] {"item.armor.equip_leather", "equip"}),        // Leather armor rustles
            Map.entry(6, new String[] {"entity.sheep.shear", "shear"}),              // Shears click
            Map.entry(7, new String[] {"entity.player.hurt", "entity_damage"}),      // Player hurts
            Map.entry(8, new String[] {"entity.generic.eat", "eat"}),                // Eating
            Map.entry(9, new String[] {"block.chest.close", "container_close"}),     // Chest closes
            Map.entry(10, new String[] {"block.chest.open", "container_open"}),      // Chest opens
            Map.entry(11, new String[] {"block.comparator.click", "block_change"}),  // Comparator clicks
            Map.entry(12, new String[] {"block.stone.break", "block_destroy"}),      // Block broken
            Map.entry(13, new String[] {"block.stone.place", "block_place"}),        // Block placed
            Map.entry(14, new String[] {"entity.item_frame.place", "entity_place"}), // Item Frame placed
            Map.entry(15, new String[] {"entity.player.death", "entity_die"}));      // Player dies

    /** Vanilla Java vibration frequencies for the game events above (and the old wrong picks, for the test). */
    public static final Map<String, Integer> VANILLA_FREQUENCY = Map.ofEntries(
            Map.entry("step", 1), Map.entry("hit_ground", 2), Map.entry("projectile_shoot", 3),
            Map.entry("item_interact_finish", 3), Map.entry("entity_action", 4), Map.entry("unequip", 4),
            Map.entry("equip", 5), Map.entry("shear", 6), Map.entry("entity_interact", 6),
            Map.entry("entity_damage", 7), Map.entry("eat", 8), Map.entry("container_close", 9),
            Map.entry("container_open", 10), Map.entry("block_activate", 10), Map.entry("block_change", 11),
            Map.entry("block_destroy", 12), Map.entry("fluid_pickup", 12), Map.entry("block_place", 13),
            Map.entry("entity_place", 14), Map.entry("entity_die", 15));

    /** The game event each replayed sound's real-world action emits (what a player hearing it would expect). */
    public static final Map<String, String> SOUND_NATURAL_EVENT = Map.ofEntries(
            Map.entry("block.stone.step", "step"), Map.entry("entity.generic.small_fall", "hit_ground"),
            Map.entry("entity.arrow.shoot", "projectile_shoot"), Map.entry("entity.wolf.shake", "entity_action"),
            Map.entry("item.armor.equip_leather", "equip"), Map.entry("entity.sheep.shear", "shear"),
            Map.entry("entity.player.hurt", "entity_damage"), Map.entry("entity.generic.eat", "eat"),
            Map.entry("block.chest.close", "container_close"), Map.entry("block.chest.open", "container_open"),
            Map.entry("block.comparator.click", "block_change"), Map.entry("block.stone.break", "block_destroy"),
            Map.entry("block.stone.place", "block_place"), Map.entry("entity.item_frame.place", "entity_place"),
            Map.entry("entity.player.death", "entity_die"),
            // the 2026-10 audit's wrong picks, kept so the self-test can show why they were wrong
            Map.entry("item.bucket.fill", "fluid_pickup"), Map.entry("item.armor.equip_generic", "equip"),
            Map.entry("block.lever.click", "block_activate"));

    public static long tickFor(int index) {
        return LEAD_IN_TICKS + (long) index * SPACING_TICKS;
    }

    /** What a careful listener decodes: two frequencies per ASCII byte. */
    public static String decode(List<Integer> frequencies) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < frequencies.size(); i += 2) sb.append((char) (frequencies.get(i) * 16 + frequencies.get(i + 1)));
        return sb.toString();
    }
}
