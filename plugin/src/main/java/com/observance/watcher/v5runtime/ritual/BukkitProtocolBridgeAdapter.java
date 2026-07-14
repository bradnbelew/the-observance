package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Captures and retags the same in-memory copper ItemStack; it never manufactures a replacement. */
public final class BukkitProtocolBridgeAdapter {
    private final NamespacedKey artifactKey;
    private final NamespacedKey instanceKey;
    private final NamespacedKey compatibilityInstanceKey;
    private final NamespacedKey outcomeKey;
    private final NamespacedKey riteMarkerKey;

    public BukkitProtocolBridgeAdapter(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        artifactKey = new NamespacedKey(plugin, ProtocolBridge.ARTIFACT_KEY);
        instanceKey = new NamespacedKey(plugin, "v5_artifact_instance");
        compatibilityInstanceKey = new NamespacedKey(plugin, "v5_item_instance");
        outcomeKey = new NamespacedKey(plugin, ProtocolBridge.OUTCOME_KEY);
        riteMarkerKey = new NamespacedKey(plugin, "v5_rite_marker");
    }

    public ProtocolBridge captureSame(ItemStack stack) {
        requireExactStack(stack);
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String artifact = pdc.get(artifactKey, PersistentDataType.STRING);
        if (!ProtocolBridge.ARTIFACT_VALUE.equals(artifact)) {
            throw new IllegalArgumentException("copper item is not the Protocol Bridge");
        }
        String instanceText = pdc.get(instanceKey, PersistentDataType.STRING);
        if (instanceText == null) {
            instanceText = pdc.get(compatibilityInstanceKey, PersistentDataType.STRING);
        }
        UUID instance;
        if (instanceText == null) {
            instance = UUID.randomUUID();
            pdc.set(instanceKey, PersistentDataType.STRING, instance.toString());
            pdc.set(compatibilityInstanceKey, PersistentDataType.STRING, instance.toString());
            stack.setItemMeta(meta);
        } else {
            instance = UUID.fromString(instanceText);
            pdc.set(instanceKey, PersistentDataType.STRING, instance.toString());
            pdc.set(compatibilityInstanceKey, PersistentDataType.STRING, instance.toString());
            stack.setItemMeta(meta);
        }
        Map<String, String> logicalPdc = new LinkedHashMap<>();
        logicalPdc.put(ProtocolBridge.ARTIFACT_KEY, ProtocolBridge.ARTIFACT_VALUE);
        String outcome = pdc.get(outcomeKey, PersistentDataType.STRING);
        if (outcome != null) {
            logicalPdc.put(ProtocolBridge.OUTCOME_KEY, outcome);
        }
        return new ProtocolBridge(instance, sha256(stack.serializeAsBytes()), logicalPdc);
    }

    public void retagSame(ItemStack original, ProtocolBridge resolved) {
        requireExactStack(original);
        Objects.requireNonNull(resolved, "resolved");
        ItemMeta meta = original.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String instance = pdc.get(instanceKey, PersistentDataType.STRING);
        if (instance == null) {
            instance = pdc.get(compatibilityInstanceKey, PersistentDataType.STRING);
        }
        if (!resolved.instanceId().toString().equals(instance)) {
            throw new IllegalStateException("attempted to replace the Protocol Bridge identity");
        }
        WrenOutcome outcome = resolved.outcome().orElseThrow(
                () -> new IllegalStateException("resolved Bridge has no Wren outcome"));
        pdc.set(outcomeKey, PersistentDataType.STRING, outcome.wireValue());
        pdc.set(riteMarkerKey, PersistentDataType.STRING, ProtocolBridge.ARTIFACT_VALUE);
        pdc.set(instanceKey, PersistentDataType.STRING, resolved.instanceId().toString());
        pdc.set(compatibilityInstanceKey, PersistentDataType.STRING,
                resolved.instanceId().toString());
        original.setItemMeta(meta);
    }

    private static void requireExactStack(ItemStack stack) {
        if (stack == null || stack.getType() != Material.COPPER_INGOT || stack.getAmount() != 1) {
            throw new IllegalArgumentException("Protocol Bridge must be exactly one copper ingot");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
