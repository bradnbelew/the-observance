package com.observance.watcher.structure;

/** Dependency-light executable contract for the packaged V5 narrative authorities. */
public final class V5AuthorityManifestSelfTest {

    private V5AuthorityManifestSelfTest() { }

    public static void main(String[] args) {
        V5AuthorityManifest.Report report = V5AuthorityManifest.inspect();
        require(report.valid(), "authority issues: " + report.issues());
        require(report.nodeCount() == 82, "expected 82 nodes");
        require(report.roomCount() == 32, "expected 32 room assignments");
        require(report.fixtureCount() == 76, "expected 76 fixture ownership rows");
        require(report.gateCount() == 8, "expected 8 gates");
        require(report.bookCount() == 44, "expected 44 written books");
        require(report.bookPlacementCount() == 44, "expected 44 exact book placements");
        require(report.artifactCount() == 21, "expected 21 V5 critical artifacts");
        require(report.recordCount() == 7, "expected 7 record stations");
        require(report.runtimeBindingCount() == 82, "expected 82 runtime bindings");
        require(report.physicalPredicateCount() == 60, "expected 60 packaged physical predicates");
        require(report.townspersonCount() == 5, "expected 5 townsfolk");
        require(report.mediaCount() == 5, "expected 5 media assets");
        require(report.authorityHash().matches("[0-9a-f]{64}"), "authority hash is not SHA-256");

        DeepHoldV5Manifest.GateContract archive = DeepHoldV5Manifest.gateContract("archive");
        require(archive != null && archive.requiredFlags().size() == 6,
                "G2 must require all six Keeper affidavits");
        require(DeepHoldV5Manifest.gateRequiredFlags("coda").equals(
                java.util.List.of("v5_case_c09_complete")), "G6 must open on C09 completion");
        require(V5AuthorityManifest.book("mara_manual_edition_1") != null,
                "Mara V5 edition book is missing");
        require(V5AuthorityManifest.book("release_protocol") != null,
                "release protocol book is missing");
        V5AuthorityManifest.BookPlacement school = V5AuthorityManifest.bookPlacements().stream()
                .filter(row -> row.bookId().equals("lc_school_day")).findFirst().orElseThrow();
        require(school.holderId().equals("orientation_register") && school.mount().equals("lectern_left")
                        && school.expectedFront().equals("WEST"),
                "LC03 school-day mount drifted from its physical predicate");
        V5AuthorityManifest.BookPlacement ledger = V5AuthorityManifest.bookPlacements().stream()
                .filter(row -> row.bookId().equals("vaun_quartermaster_ledger")).findFirst().orElseThrow();
        require(ledger.holderId().equals("vaun_hoard_chest") && ledger.mount().equals("lectern_left")
                        && ledger.expectedFront().equals("EAST"),
                "KV01 ledger mount drifted from its physical predicate");
        require(V5AuthorityManifest.artifact("orientation_key") != null,
                "orientation key artifact is missing");
        require(V5AuthorityManifest.artifact("averyn_fragment_n") != null,
                "final AVERYN fragment artifact is missing");
        require(V5AuthorityManifest.casebookIssues().isEmpty(),
                "test-only casebook parity failed: " + V5AuthorityManifest.casebookIssues());

        System.out.println("V5AuthorityManifestSelfTest OK " + report.authorityHash());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
