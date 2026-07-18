package com.observance.watcher.v5runtime;

import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog;
import java.util.List;
import java.util.Set;

public final class P11IdentityAuthoritySelfTest {
    private static final String BASE_SHA256 =
            "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a";

    private P11IdentityAuthoritySelfTest() {
    }

    public static void main(String[] args) {
        PhysicalPredicateAuthority historical = PhysicalPredicateAuthorityLoader.loadDefault();
        require(BASE_SHA256.equals(historical.sha256()), "historical M2 bytes drifted");
        require(historical.requireNode("AR03").prerequisites().contains("v5_ar02_a"),
                "historical sequential receipt was rewritten");

        PhysicalPredicateAuthority effective = P11IdentityAuthority.apply(historical);
        require(BASE_SHA256.equals(effective.sha256()), "overlay lost base receipt identity");
        List<String> ownFlags = List.of(
                "v5_kv03_affidavit", "v5_km03_affidavit", "v5_ks03_affidavit",
                "v5_ko03_affidavit", "v5_kb03_affidavit", "v5_ki03_affidavit");
        for (int index = 0; index < ownFlags.size(); index++) {
            PhysicalPredicateAuthority.Node node = effective.requireNode("AR0" + (index + 2));
            require(Set.copyOf(node.prerequisites()).equals(
                    Set.of("v5_ar01_not_kept", ownFlags.get(index))),
                    node.nodeId() + " is not independently available");
        }
        require("affidavit_plus_verified_bearing_and_low_sightline".equals(
                        effective.requireNode("AR05").predicate().kind()),
                "effective Orin predicate still uses retired dials");
        String orin = effective.requireNode("AR05").predicate().canonicalJson();
        require(orin.contains("\"bearing\"") && !orin.contains("\"dials\""),
                "effective Orin components are wrong");
        require(P11IdentityAuthority.apply(effective).requireNode("AR05")
                        .predicate().canonicalJson().equals(orin),
                "overlay is not idempotent");

        V5PhysicalComponentCatalog.Catalog catalog = V5PhysicalComponentCatalog.build(historical);
        long orinFrames = catalog.addressesForNode("AR05").stream()
                .filter(address -> address.kind()
                        == V5PhysicalComponentCatalog.AddressKind.ITEM_FRAME)
                .count();
        require(orinFrames == 1, "effective Orin station must contain one bearing frame");
        require(catalog.addressesForNode("AR05").stream()
                        .noneMatch(address -> "dials".equals(address.componentId())),
                "retired Orin dials remain in the install catalog");
        System.out.println("P11 IDENTITY AUTHORITY: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
