package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.PlayerProgress;
import com.observance.watcher.v5runtime.V5ProgressStore;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Idempotent actor-bound receipt issuance with durable full-inventory fallback. */
public final class BukkitReceiptService {
    private final Plugin plugin;
    private final BukkitDurableItemEscrow escrow;
    private final V5ProgressStore progress;
    private final GeneratedReceiptCatalog catalog;

    public BukkitReceiptService(
            Plugin plugin,
            BukkitDurableItemEscrow escrow,
            V5ProgressStore progress,
            PhysicalPredicateAuthority authority) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.escrow = Objects.requireNonNull(escrow, "escrow");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.catalog = new GeneratedReceiptCatalog(
                Objects.requireNonNull(authority, "authority"));
    }

    public boolean issue(Player player, String nodeId, String receiptId) throws IOException {
        Objects.requireNonNull(player, "player");
        String bit = issuedBit(nodeId, receiptId);
        boolean physicallyPendingOrHeld = hasReceipt(player, nodeId, receiptId)
                || escrow.containsReceipt(player.getUniqueId(), nodeId, receiptId);
        if (issued(player.getUniqueId(), bit)) {
            // A prior crash may have left the already-authorized copy in protected escrow.
            escrow.deliverPending(player);
            return false;
        }
        if (physicallyPendingOrHeld) {
            persistIssued(player.getUniqueId(), bit);
            escrow.deliverPending(player);
            return false;
        }
        ItemStack receipt = new ItemStack(Material.PAPER, 1);
        var meta = receipt.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(key("v5_receipt_id"), PersistentDataType.STRING, receiptId);
        pdc.set(key("v5_receipt_node"), PersistentDataType.STRING, nodeId);
        pdc.set(key("v5_receipt_actor"), PersistentDataType.STRING,
                player.getUniqueId().toString());
        String shortId = receiptId.replace('_', ' ').replace('-', ' ');
        meta.displayName(net.kyori.adventure.text.Component.text(
                "Source slip · " + nodeId + " · " + shortId));
        meta.lore(List.of(
                net.kyori.adventure.text.Component.text("A distinct copy from an opened record."),
                net.kyori.adventure.text.Component.text("File only where this slip's mark is requested."),
                net.kyori.adventure.text.Component.text("Reader: "
                        + player.getName() + " · " + receiptId)));
        receipt.setItemMeta(meta);
        // Journal first, then commit the durable issued bit, then deliver. A crash at either edge is
        // reconciled without ever minting a second copy.
        escrow.deposit(player.getUniqueId(), receipt,
                "generated receipt " + nodeId + ':' + receiptId);
        persistIssued(player.getUniqueId(), bit);
        escrow.deliverPending(player);
        return true;
    }

    public int recover(Player player) throws IOException {
        // Close the only cross-file crash window (journal forced, issued bit not yet forced) before
        // delivery can remove a journal. This makes later filing/consumption non-remintable.
        for (GeneratedReceiptCatalog.ReceiptKey key : catalog.allReceipts()) {
            if (escrow.containsReceipt(player.getUniqueId(), key.nodeId(), key.receiptId())) {
                persistIssued(player.getUniqueId(), issuedBit(key.nodeId(), key.receiptId()));
            }
        }
        return escrow.deliverPending(player);
    }

    private boolean hasReceipt(Player player, String nodeId, String receiptId) {
        UUID actor = player.getUniqueId();
        if (hasReceipt(player.getInventory().getContents(), actor, nodeId, receiptId)) return true;
        return hasReceipt(player.getEnderChest().getContents(), actor, nodeId, receiptId);
    }

    private boolean hasReceipt(
            ItemStack[] contents, UUID actor, String nodeId, String receiptId) {
        for (ItemStack item : contents) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (receiptId.equals(pdc.get(key("v5_receipt_id"), PersistentDataType.STRING))
                    && nodeId.equals(pdc.get(key("v5_receipt_node"), PersistentDataType.STRING))
                    && actor.toString().equals(
                            pdc.get(key("v5_receipt_actor"), PersistentDataType.STRING))) {
                return true;
            }
        }
        return false;
    }

    private boolean issued(UUID playerId, String bit) {
        PlayerProgress player = progress.snapshot().players().get(playerId.toString());
        return player != null && player.inspections().contains(bit);
    }

    private void persistIssued(UUID playerId, String bit) throws IOException {
        progress.transact(editor -> {
            editor.addPlayerBit(playerId, PlayerBitDomain.INSPECTION, bit);
            return null;
        });
    }

    private static String issuedBit(String nodeId, String receiptId) {
        if (nodeId == null || nodeId.isBlank() || receiptId == null || receiptId.isBlank()) {
            throw new IllegalArgumentException("receipt identity cannot be blank");
        }
        return "RECEIPT_ISSUED:" + nodeId + ':' + receiptId;
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
