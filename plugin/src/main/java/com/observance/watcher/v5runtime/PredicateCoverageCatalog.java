package com.observance.watcher.v5runtime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Exhaustive routing metadata for V5 physical predicates.
 *
 * <p>This catalog intentionally claims only that a node has a core contract and an adapter family.
 * It does not claim that Bukkit interaction code exists.</p>
 */
public final class PredicateCoverageCatalog {
    public enum AdapterStatus {
        CORE_CONTRACT_ONLY
    }

    public record Entry(String nodeId, ImplementationFamily family, AdapterStatus status) {
        public Entry {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(family, "family");
            Objects.requireNonNull(status, "status");
        }

        public boolean worldInteractionImplemented() {
            return false;
        }
    }

    private static final Map<String, Entry> ENTRIES = buildEntries();

    private PredicateCoverageCatalog() {
    }

    public static Map<String, Entry> entries() {
        return ENTRIES;
    }

    public static Entry require(String nodeId) {
        Entry entry = ENTRIES.get(nodeId);
        if (entry == null) {
            throw new IllegalArgumentException("Unregistered V5 physical node: " + nodeId);
        }
        return entry;
    }

    public static Map<ImplementationFamily, List<Entry>> byFamily() {
        Map<ImplementationFamily, List<Entry>> grouped = new EnumMap<>(ImplementationFamily.class);
        for (ImplementationFamily family : ImplementationFamily.values()) {
            grouped.put(family, new ArrayList<>());
        }
        for (Entry entry : ENTRIES.values()) {
            grouped.get(entry.family()).add(entry);
        }
        Map<ImplementationFamily, List<Entry>> immutable = new EnumMap<>(ImplementationFamily.class);
        grouped.forEach((family, entries) -> immutable.put(family, List.copyOf(entries)));
        return Map.copyOf(immutable);
    }

    public static void validateAgainst(PhysicalPredicateAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        Set<String> authorityIds = new LinkedHashSet<>(authority.nodesById().keySet());
        Set<String> catalogIds = new LinkedHashSet<>(ENTRIES.keySet());

        Set<String> missing = new LinkedHashSet<>(authorityIds);
        missing.removeAll(catalogIds);
        Set<String> stale = new LinkedHashSet<>(catalogIds);
        stale.removeAll(authorityIds);
        if (!missing.isEmpty() || !stale.isEmpty()) {
            throw new AuthorityException("V5 predicate coverage mismatch; missing=" + missing
                    + ", stale=" + stale);
        }
        if (catalogIds.size() != PhysicalPredicateAuthority.REQUIRED_NODE_COUNT) {
            throw new AuthorityException("V5 predicate catalog must contain exactly 60 nodes, found "
                    + catalogIds.size());
        }
        for (Entry entry : ENTRIES.values()) {
            if (entry.status() != AdapterStatus.CORE_CONTRACT_ONLY
                    || entry.worldInteractionImplemented()) {
                throw new AuthorityException("V5 core catalog made an unsupported world-adapter claim for "
                        + entry.nodeId());
            }
        }
    }

    private static Map<String, Entry> buildEntries() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        register(entries, ImplementationFamily.S,
                "LC02", "LC04", "A04", "WR01", "CW04", "BI05", "KV03", "KM02", "KS03", "KI01");
        register(entries, ImplementationFamily.I,
                "LC03", "KV01", "A02", "WR02", "KI02");
        register(entries, ImplementationFamily.F,
                "LC01", "LC06", "A05", "CW03", "BI01", "BI04", "HS03", "HS04", "HS05",
                "KS01", "KS02", "KO02", "KB02");
        register(entries, ImplementationFamily.L,
                "AR02", "AR03", "AR04", "AR05", "AR06", "AR07", "CW01", "KM01");
        register(entries, ImplementationFamily.R,
                "WR04", "RP04", "BI07", "HS06", "KM03", "KO01");
        register(entries, ImplementationFamily.N, "WR03");
        register(entries, ImplementationFamily.V, "WR05", "RP03", "RP05", "RP06");
        register(entries, ImplementationFamily.C,
                "LS06", "A03", "A09", "RP02", "CW02", "CW07", "BI02", "BI03", "BI06",
                "HS01", "HS02", "HS07", "KV02");
        if (entries.size() != PhysicalPredicateAuthority.REQUIRED_NODE_COUNT) {
            throw new ExceptionInInitializerError("V5 predicate catalog contains " + entries.size()
                    + " nodes instead of 60");
        }
        return Map.copyOf(entries);
    }

    private static void register(
            Map<String, Entry> entries, ImplementationFamily family, String... nodeIds) {
        for (String nodeId : nodeIds) {
            Entry previous = entries.put(nodeId,
                    new Entry(nodeId, family, AdapterStatus.CORE_CONTRACT_ONLY));
            if (previous != null) {
                throw new ExceptionInInitializerError("Duplicate V5 catalog registration: " + nodeId);
            }
        }
    }
}
