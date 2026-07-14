package com.observance.watcher.structure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Single factory and recognizer for every V5 recoverable PDC-backed artifact. */
public final class CanonicalArtifactRegistry {

    public static final String PDC_ARTIFACT_ID = "artifact_id";
    private static final String NAMESPACE = "observance";

    private CanonicalArtifactRegistry() { }

    public static Set<String> ids() {
        Set<String> ids = new LinkedHashSet<>();
        for (DeepHoldV5Manifest.Artifact artifact : DeepHoldV5Manifest.ARTIFACTS) ids.add(artifact.id());
        return Set.copyOf(ids);
    }

    /** Namespaced resource-pack atmosphere for V5 Keeper affidavit issuance; never a solve signal. */
    public static String atmosphereSound(String requestedId) {
        String id = resolveId(requestedId);
        if (id == null || !id.startsWith("affidavit_")) return null;
        return "observance:keeper_voice." + id.substring("affidavit_".length());
    }

    public static String resolveId(String raw) {
        if (raw == null) return null;
        String id = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(':', '_');
        return DeepHoldV5Manifest.artifact(id) == null ? null : id;
    }

    /** Build an exact canonical V5 artifact. The location parameter is retained for API stability. */
    public static ItemStack create(String requestedId, Location keptLight) {
        String id = resolveId(requestedId);
        if (id == null) return null;
        V5AuthorityManifest.ArtifactEntry authority = V5AuthorityManifest.artifact(id);
        ItemStack item = authority == null ? null : v5Artifact(authority);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(artifactIdKey(), PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isArtifact(ItemStack item, String requestedId) {
        String id = resolveId(requestedId);
        if (id == null || item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String canonical = pdc.get(artifactIdKey(), PersistentDataType.STRING);
        if (id.equals(canonical)) return true;
        DeepHoldV5Manifest.Artifact artifact = DeepHoldV5Manifest.artifact(id);
        return artifact != null && markerMatches(item, artifact);
    }

    private static boolean markerMatches(ItemStack item, DeepHoldV5Manifest.Artifact artifact) {
        if (item == null || artifact == null || !item.hasItemMeta()
                || item.getType() != Material.matchMaterial(artifact.material())) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey marker = new NamespacedKey(NAMESPACE, artifact.markerKey());
        return switch (artifact.markerType()) {
            case "byte" -> pdc.has(marker, PersistentDataType.BYTE);
            case "integer" -> pdc.has(marker, PersistentDataType.INTEGER);
            case "string" -> artifact.markerValue().equals(pdc.get(marker, PersistentDataType.STRING));
            default -> false;
        };
    }

    /** Exact diagnostics used by build audits and the recovery command. */
    public static List<String> audit(ItemStack item, String requestedId) {
        List<String> issues = new ArrayList<>();
        String id = resolveId(requestedId);
        if (id == null) return List.of("unknown canonical artifact " + requestedId);
        DeepHoldV5Manifest.Artifact artifact = DeepHoldV5Manifest.artifact(id);
        if (item == null || item.getType() == Material.AIR) return List.of(id + " is missing");
        Material expected = Material.matchMaterial(artifact.material());
        if (item.getType() != expected) issues.add(id + " material is " + item.getType() + ", expected " + expected);
        if (item.getAmount() != 1) issues.add(id + " amount is " + item.getAmount() + ", expected 1");
        if (!markerMatches(item, artifact)) issues.add(id + " is missing its exact PDC marker/value");
        if (!item.hasItemMeta() || !id.equals(item.getItemMeta().getPersistentDataContainer()
                .get(artifactIdKey(), PersistentDataType.STRING))) {
            issues.add(id + " is missing observance:" + PDC_ARTIFACT_ID);
        }
        V5AuthorityManifest.ArtifactEntry authority = V5AuthorityManifest.artifact(id);
        if (authority != null && (item.getItemMeta() == null
                || !Component.text(authority.displayName()).equals(item.getItemMeta().displayName()))) {
            issues.add(id + " display name does not match V5 authority");
        }
        return List.copyOf(issues);
    }

    private static ItemStack v5Artifact(V5AuthorityManifest.ArtifactEntry authority) {
        Material material = Material.matchMaterial(authority.material());
        if (material == null) return null;
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta instanceof BookMeta bookMeta && authority.id().startsWith("affidavit_")) {
                String keeper = authority.id().substring("affidavit_".length());
                V5AuthorityManifest.BookEntry book = V5AuthorityManifest.book(
                        keeper + "_sealed_affidavit");
                if (book == null) return null;
                bookMeta.setTitle(book.title());
                bookMeta.setAuthor(book.author());
                for (String page : book.pages()) bookMeta.addPages(Component.text(page));
                bookMeta.getPersistentDataContainer().set(new NamespacedKey(NAMESPACE, "book_id"),
                        PersistentDataType.STRING, book.id());
                bookMeta.getPersistentDataContainer().set(new NamespacedKey(NAMESPACE, "story_version"),
                        PersistentDataType.STRING, "5.0.0");
            }
            meta.displayName(Component.text(authority.displayName()));
            meta.lore(List.of(Component.text("V5 evidence: " + authority.earnedNode())
                    .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(new NamespacedKey(NAMESPACE, authority.pdcKey()),
                    PersistentDataType.STRING, authority.pdcValue());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static NamespacedKey artifactIdKey() {
        return new NamespacedKey(NAMESPACE, PDC_ARTIFACT_ID);
    }
}
