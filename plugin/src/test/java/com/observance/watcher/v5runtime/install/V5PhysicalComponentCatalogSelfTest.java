package com.observance.watcher.v5runtime.install;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.Address;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog.AddressKind;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-light executable contract test for the 60-node physical layout expansion. */
public final class V5PhysicalComponentCatalogSelfTest {
    private V5PhysicalComponentCatalogSelfTest() { }

    public static void main(String[] args) {
        V5PhysicalComponentCatalog.Catalog catalog = V5PhysicalComponentCatalog.loadDefault();
        check(catalog.nodes().size() == PhysicalPredicateAuthority.REQUIRED_NODE_COUNT,
                "catalog must contain exactly 60 node plans");
        check(!catalog.authoritySha256().isBlank(), "authority hash must be retained");
        check(catalog.addresses().size() >= 220,
                "expanded catalog unexpectedly small: " + catalog.addresses().size());

        Set<String> plannedNodes = new HashSet<>();
        catalog.addresses().forEach(address -> plannedNodes.add(address.nodeId()));
        check(plannedNodes.size() == PhysicalPredicateAuthority.REQUIRED_NODE_COUNT,
                "every authority node needs a physical or explicitly logical address");

        check(catalog.valid(), "physical authority must be collision/bounds clean: " + catalog.findings());
        check(catalog.findings().isEmpty(), "static catalog must have no unresolved findings");

        V5EvidenceItemAppearanceAuthority.Catalog appearances =
                V5EvidenceItemAppearanceAuthority.loadDefault();
        check(appearances.valid(), "appearance authority must be exact: " + appearances.issues());
        check(appearances.byId().size() == V5EvidenceItemAppearanceAuthority.EXPECTED_ITEMS,
                "all non-book evidence must have player-visible text");
        check(appearances.containerLabels().size() == V5EvidenceItemAppearanceAuthority.EXPECTED_LABELS,
                "all split input containers must have labels");
        check(appearances.stationLabels().size()
                        == V5EvidenceItemAppearanceAuthority.EXPECTED_STATION_LABELS,
                "all A02 stations must have visible labels");

        require(catalog, "WR02", "reader_eject", AddressKind.BLOCK, "STONE_BUTTON");
        for (int release = 3; release <= 7; release++) {
            require(catalog, "AR0" + release, "reader_control", AddressKind.BLOCK, "STONE_BUTTON");
        }
        require(catalog, "KM01", "e3", AddressKind.BLOCK, "LEVER");
        require(catalog, "HS02", "housing_latch", AddressKind.BLOCK, "LEVER");
        require(catalog, "CW07", "cache_seal", AddressKind.BLOCK, "LEVER");

        check(!V5MovableFramePolicy.mayInferDisplacementFromItemIdentity(true),
                "movable shuffled pieces must never be stolen as displaced neighboring targets");
        check(V5MovableFramePolicy.mayInferDisplacementFromItemIdentity(false),
                "ordinary fixed frames retain bounded displaced-item recovery");

        check(count(catalog, "RP04", "sectors") == 12,
                "RP04 needs all 12 authored sector plates");
        check(count(catalog, "RP04", "sector_handle") == 12,
                "RP04 needs one distinct confirmation control per sector");
        check(count(catalog, "RP04", "sector_lamp") == 12,
                "RP04 needs one visible indicator per sector");
        check(catalog.addressesForNode("RP04").stream()
                        .filter(address -> address.pdc().containsKey("v5_rp04_bridge_control"))
                        .count() == 6,
                "RP04 needs all six branch-specific Bridge custody housings");
        require(catalog, "RP05", "sever_control_interaction", AddressKind.MARKER, "");

        require(catalog, "A02", "wren_station", AddressKind.ITEM_DISPLAY, "NAME_TAG");
        check(catalog.addressesForNode("A02").stream()
                        .filter(address -> address.pdc().containsKey("v5_station_label_for"))
                        .count() == 4,
                "A02 needs four exact visible station labels");

        check(catalog.addresses().stream()
                        .filter(address -> address.pdc().containsKey("v5_label_for"))
                        .count() == V5EvidenceItemAppearanceAuthority.EXPECTED_LABELS,
                "every container label authority row needs a physical sign address");

        // Command-level visual builders establish a focal block first; these exact runtime
        // components intentionally replace that block at local [0,0,0]. Keep the complete set
        // explicit so a final readback can accept the authoritative overlay without silently
        // tolerating an arbitrary anchor material.
        Map<String, String> fixtureOriginOverrides = Map.ofEntries(
                Map.entry("forgotten_mouth", "BARREL"),
                Map.entry("dread_route_elsewhere", "BARREL"),
                Map.entry("the_threshold", "POLISHED_DEEPSLATE"),
                Map.entry("threshold_vault", "BARREL"),
                Map.entry("the_unwriting", "BARREL"),
                Map.entry("cistern_7", "LECTERN"),
                Map.entry("the_far_water", "CHEST"),
                Map.entry("dead_stall", "BARREL"),
                Map.entry("offering_cairn_01", "BARREL"),
                Map.entry("stone_of_reckoning", "BARREL"));
        Map<String, String> actualOriginOverrides = catalog.addresses().stream()
                .filter(address -> address.kind() == AddressKind.BLOCK)
                .filter(address -> address.offset().right() == 0
                        && address.offset().up() == 0 && address.offset().front() == 0)
                .filter(address -> com.observance.watcher.structure.DeepHoldV4Plan.fixture(
                        address.siteId()) != null)
                .collect(java.util.stream.Collectors.toMap(Address::siteId, Address::material,
                        (left, right) -> {
                            check(left.equals(right), "incompatible origin override materials");
                            return left;
                        }));
        check(actualOriginOverrides.equals(fixtureOriginOverrides),
                "fixture origin overlays drifted: " + actualOriginOverrides);

        // Vaun's registered focal cell is a stone, not a legacy chest. KV01/KV02 own distinct
        // offset containers around it, and no physical address may silently reclaim the origin.
        check(catalog.addresses().stream().noneMatch(address -> address.siteId().equals("vaun_hoard_chest")
                        && address.kind() == AddressKind.BLOCK && address.offset().right() == 0
                        && address.offset().up() == 0 && address.offset().front() == 0),
                "Vaun focal stone must remain free of a legacy origin container");
        requireAt(catalog, "KV01", "stores_ledger", -1, 0, 0, "LECTERN");
        requireAt(catalog, "KV01", "cistern_receipts", 1, 0, 0, "BARREL");
        requireAt(catalog, "KV01", "audit_tray", 0, 0, 1, "BARREL");
        requireAt(catalog, "KV02", "input_lot_a", -1, 0, 2, "BARREL");
        requireAt(catalog, "KV02", "input_lot_b", 1, 0, 2, "BARREL");

        System.out.println("V5PhysicalComponentCatalogSelfTest OK - " + catalog.nodes().size()
                + " nodes, " + catalog.addresses().size() + " expanded addresses, "
                + catalog.findings().size() + " static findings");
        catalog.findings().forEach(finding -> System.out.println("  " + finding.severity() + " "
                + finding.nodeId() + "/" + finding.componentId() + ": " + finding.message()));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static long count(V5PhysicalComponentCatalog.Catalog catalog,
                              String nodeId, String componentId) {
        return catalog.addressesForNode(nodeId).stream()
                .filter(address -> address.componentId().equals(componentId)).count();
    }

    private static void require(V5PhysicalComponentCatalog.Catalog catalog, String nodeId,
                                String componentId, AddressKind kind, String material) {
        List<Address> matches = catalog.addressesForNode(nodeId).stream()
                .filter(address -> address.componentId().equals(componentId))
                .filter(address -> address.kind() == kind)
                .filter(address -> material.isBlank() || address.material().equals(material))
                .toList();
        check(!matches.isEmpty(), nodeId + '/' + componentId + " is missing exact " + kind
                + (material.isBlank() ? "" : " " + material));
    }

    private static void requireAt(V5PhysicalComponentCatalog.Catalog catalog, String nodeId,
                                  String componentId, int right, int up, int front,
                                  String material) {
        boolean found = catalog.addressesForNode(nodeId).stream()
                .anyMatch(address -> address.componentId().equals(componentId)
                        && address.kind() == AddressKind.BLOCK
                        && address.offset().right() == right && address.offset().up() == up
                        && address.offset().front() == front && address.material().equals(material));
        check(found, nodeId + '/' + componentId + " is missing at [" + right + ',' + up + ','
                + front + "] as " + material);
    }
}
