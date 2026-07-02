package com.observance.watcher.lens;

import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.Safety;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * The single authority that reveals / hides {@link LensRegistry}-gated displays as a player equips or
 * holsters {@link LensItem the Lens} (INTEGRATION §SIGNATURE #3 "second sight"). While the Lens is in
 * hand, that player's gated runes are shown to THEM ONLY (via {@link PerPlayer#showEntityTo}); the
 * instant they switch away, the runes vanish (via {@link PerPlayer#hideEntityFrom}).
 *
 * <p><b>Why event-driven, not a per-tick poll.</b> Held-item changes are discrete: hotbar scroll
 * ({@link PlayerItemHeldEvent}), hand swap ({@link PlayerSwapHandItemsEvent}), and inventory close
 * (a drag can put the Lens into / out of the held slot). Reconciling on exactly those moments keeps the
 * effect instant and cheap. A short scheduled follow-up after the hotbar event settles the case where the
 * new slot's item is only known next tick.
 *
 * <p><b>Behaviour-safe by construction.</b> Every handler is Safety-wrapped and re-resolves the player;
 * the registry re-resolves + prunes stale display ids on every read, so a despawned rune can never NPE.
 * On logout we hide the player's gated displays from them (courtesy) and drop the owner's tracking, so no
 * reveal state is stranded. Reveal/hide are Paper's genuinely client-only visibility calls — the display
 * entities themselves are untouched and remain hidden-by-default to everyone else.
 */
public final class LensListener implements Listener {

    private final Plugin plugin;
    private final LensRegistry registry;
    private final Safety safety;
    private final String namespace;

    public LensListener(Plugin plugin, LensRegistry registry, Safety safety, String namespace) {
        this.plugin = plugin;
        this.registry = registry;
        this.safety = safety;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
    }

    /* ------------------------------------------------------------------ */
    /*  Equip / holster triggers                                           */
    /* ------------------------------------------------------------------ */

    /** Hotbar scroll: the newly selected slot is known via {@code getNewSlot} even before the tick ends. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        final int newSlot = event.getNewSlot();
        safety.run("lens.held", () -> reconcile(player, heldAtSlot(player, newSlot)));
        // Settle any off-hand / next-tick ambiguity once the swap is fully applied.
        scheduleReconcile(player);
    }

    /** Swapping main/off hand (F key) can move the Lens into or out of the primary hand. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        // The swapped items are on the event; either hand holding the Lens counts as "held".
        boolean holding = isLens(event.getMainHandItem()) || isLens(event.getOffHandItem());
        safety.run("lens.swap", () -> setVisible(player, holding));
        scheduleReconcile(player);
    }

    /** Closing an inventory can leave a freshly-dragged item (the Lens) in the held slot. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        safety.run("lens.invclose", () -> reconcile(player, currentlyHolding(player)));
    }

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                          */
    /* ------------------------------------------------------------------ */

    /** On join, reconcile once (they may already be holding the Lens) after the client settles. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        scheduleReconcile(player);
    }

    /** On quit, hide their gated displays from them + drop tracking so no reveal state is stranded. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        safety.run("lens.quit", () -> {
            for (Entity e : registry.validDisplays(player.getUniqueId())) {
                PerPlayer.hideEntityFrom(plugin, player, e);
            }
            registry.clearOwner(player.getUniqueId());
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Reconciliation                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Public entry a beat can call right after registering a gated display, so the rune appears
     * immediately if the player is already holding the Lens (rather than waiting for the next equip).
     * MAIN thread; Safety-wrapped internally.
     */
    public void refresh(Player player) {
        if (player == null) return;
        safety.run("lens.refresh", () -> reconcile(player, currentlyHolding(player)));
    }

    /** Reconcile visibility from an already-computed held item. MAIN thread. */
    private void reconcile(Player player, ItemStack held) {
        setVisible(player, isLens(held));
    }

    /** Show or hide ALL of this player's gated displays to/from them. MAIN thread. */
    private void setVisible(Player player, boolean visible) {
        if (player == null || !player.isOnline()) return;
        List<Entity> displays = registry.validDisplays(player.getUniqueId());
        if (displays.isEmpty()) return;
        for (Entity e : displays) {
            if (visible) {
                PerPlayer.showEntityTo(plugin, player, e);
            } else {
                PerPlayer.hideEntityFrom(plugin, player, e);
            }
        }
    }

    /** A short-delay reconcile that reads the settled held item next tick. */
    private void scheduleReconcile(Player player) {
        try {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                    () -> safety.run("lens.reconcile", () -> {
                        Player p = org.bukkit.Bukkit.getPlayer(player.getUniqueId());
                        if (p != null && p.isOnline()) reconcile(p, currentlyHolding(p));
                    }), 1L);
        } catch (Throwable ignored) { }
    }

    /* ------------------------------------------------------------------ */
    /*  Held-item resolution                                               */
    /* ------------------------------------------------------------------ */

    /** The Lens counts as held if it is in EITHER hand. MAIN thread. */
    private ItemStack currentlyHolding(Player player) {
        if (player == null) return null;
        try {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (isLens(main)) return main;
            ItemStack off = player.getInventory().getItemInOffHand();
            if (isLens(off)) return off;
            return main; // non-Lens → reconcile() will hide
        } catch (Throwable t) {
            return null;
        }
    }

    private ItemStack heldAtSlot(Player player, int slot) {
        try {
            ItemStack main = player.getInventory().getItem(slot);
            if (isLens(main)) return main;
            ItemStack off = player.getInventory().getItemInOffHand();
            if (isLens(off)) return off;
            return main;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isLens(ItemStack item) {
        return LensItem.isLens(item, namespace);
    }
}
