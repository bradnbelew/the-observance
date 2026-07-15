package com.observance.watcher.structure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Production metadata wrapped around the compact V5 physical plan.
 *
 * <p>The compatibility class name remains {@link DeepHoldV4Plan}, but its current revision is the
 * smaller shared-library/keeper/camp layout. This manifest fixes the canonical orientation, declares
 * every recoverable key item, fingerprints both content and physical revision, and validates before
 * the first block is mutated.
 */
public final class DeepHoldV5Manifest {

    public static final String RUNTIME_VERSION = "5";
    public static final String CANONICAL_ORIENTATION = "+Z";
    public static final int EXPECTED_ROOMS = 32;
    public static final int EXPECTED_FIXTURES = 76;
    public static final int EXPECTED_GATES = 8;
    public static final int EXPECTED_NODES = 82;
    public static final int EXPECTED_BOOKS = 44;
    public static final int EXPECTED_V5_ARTIFACTS = 21;

    /** A canonical PDC-backed item that an operator can recover without guessing its NBT. */
    public record Artifact(String id, String material, String markerKey, String markerType,
                           String markerValue) {
        public Artifact {
            id = normalized(id);
            material = normalized(material).toUpperCase(Locale.ROOT);
            markerKey = normalized(markerKey);
            markerType = normalized(markerType).toLowerCase(Locale.ROOT);
            markerValue = markerValue == null ? "" : markerValue.trim();
        }
    }

    /** Exact build-time content requirement for a mechanic family. */
    public record ContentContract(String id, String fixtureType, int exactCount, String payload) {
        public ContentContract {
            id = normalized(id);
            fixtureType = normalized(fixtureType);
            payload = payload == null ? "" : payload.trim();
        }
    }

    /** One physical progression gate and the exact V5 flags that must all be true to open it. */
    public record GateContract(String siteId, String planId, String openCondition,
                               List<String> requiredFlags) {
        public GateContract {
            siteId = normalized(siteId);
            planId = normalized(planId);
            openCondition = openCondition == null ? "" : openCondition.trim();
            requiredFlags = requiredFlags == null ? List.of() : List.copyOf(requiredFlags);
        }
    }

    public static final List<Artifact> ARTIFACTS = List.of(
            // Exact V5 node-critical rewards. No V4 token/relic is recoverable in production.
            // The duplicated marker value is intentional: runtime recovery audits both the
            // compatibility artifact_id and the V5 authority marker.
            new Artifact("orientation_key", "compass", "v5_artifact_id", "string", "orientation_key"),
            new Artifact("survey_seal", "paper", "v5_artifact_id", "string", "survey_seal"),
            new Artifact("affidavit_vaun", "written_book", "v5_artifact_id", "string", "affidavit_vaun"),
            new Artifact("affidavit_mara", "written_book", "v5_artifact_id", "string", "affidavit_mara"),
            new Artifact("affidavit_sella", "written_book", "v5_artifact_id", "string", "affidavit_sella"),
            new Artifact("affidavit_orin", "written_book", "v5_artifact_id", "string", "affidavit_orin"),
            new Artifact("affidavit_brann", "written_book", "v5_artifact_id", "string", "affidavit_brann"),
            new Artifact("affidavit_iss", "written_book", "v5_artifact_id", "string", "affidavit_iss"),
            new Artifact("cistern_seal", "prismarine_shard", "v5_artifact_id", "string", "cistern_seal"),
            new Artifact("breach_plate", "heavy_weighted_pressure_plate", "v5_artifact_id", "string", "breach_plate"),
            new Artifact("filter_cartridge", "copper_ingot", "v5_artifact_id", "string", "filter_cartridge"),
            new Artifact("system_key", "tripwire_hook", "v5_artifact_id", "string", "system_key"),
            new Artifact("deep_access_plate", "echo_shard", "v5_artifact_id", "string", "deep_access_plate"),
            new Artifact("witness_spool", "string", "v5_artifact_id", "string", "witness_spool"),
            new Artifact("protocol_bridge", "copper_ingot", "v5_artifact_id", "string", "protocol_bridge"),
            new Artifact("averyn_fragment_a", "paper", "v5_artifact_id", "string", "averyn_fragment_a"),
            new Artifact("averyn_fragment_v", "paper", "v5_artifact_id", "string", "averyn_fragment_v"),
            new Artifact("averyn_fragment_e", "paper", "v5_artifact_id", "string", "averyn_fragment_e"),
            new Artifact("averyn_fragment_r", "paper", "v5_artifact_id", "string", "averyn_fragment_r"),
            new Artifact("averyn_fragment_y", "paper", "v5_artifact_id", "string", "averyn_fragment_y"),
            new Artifact("averyn_fragment_n", "paper", "v5_artifact_id", "string", "averyn_fragment_n")
    );

    public static final List<ContentContract> CONTENT = List.of(
            new ContentContract("mara_lock", "mara_lectern", 5, "written_book:title+author+pages"),
            new ContentContract("sella_overlay", "sella_lectern", 5, "written_book:title+author+pages"),
            new ContentContract("orin_dials", "orin_frame_dial", 6,
                    "unique compass+pdc+facing+deterministic rotation+backing"),
            new ContentContract("vaun_shelf", "vaun_bookshelf", 1, "six exact shelf slots"),
            new ContentContract("keeper_affidavits", "keeper_stone", 6,
                    "answer surface plus idempotent exact affidavit reward; no preseeded token"),
            new ContentContract("answer_surfaces", "answer_sign", 9, "focused-puzzle-key+backing+stand"),
            new ContentContract("district_records", "record_station", 7,
                    "unlock-controlled exact authority book+readable sign")
    );

    /** Canonical linear gate sequence from design/DEEP-HOLD-GATE-MANIFEST.csv. */
    public static final List<GateContract> GATE_CONTRACTS = List.of(
            gate("keeper", "g1", "v5_case_c02_complete"),
            gate("archive", "g2", "v5_kv03_affidavit", "v5_km03_affidavit",
                    "v5_ks03_affidavit", "v5_ko03_affidavit", "v5_kb03_affidavit",
                    "v5_ki03_affidavit"),
            gate("undercroft", "g3", "v5_case_c04_complete", "v5_case_c05_complete"),
            gate("deep", "g4", "v5_case_c06_complete"),
            gate("prior", "prior", "v5_a01_location"),
            gate("dread", "dread", "v5_case_c07_complete"),
            gate("accepting", "g5", "v5_case_c08_complete"),
            gate("coda", "g6", "v5_case_c09_complete")
    );

    private static final Map<String, GateContract> GATES_BY_SITE = GATE_CONTRACTS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(GateContract::siteId, gate -> gate));

    private DeepHoldV5Manifest() { }

    /** Validate the complete V5 metadata plus every inherited spatial invariant. */
    public static List<String> validate() {
        List<String> issues = new ArrayList<>(DeepHoldV4Plan.validate());
        if (!CANONICAL_ORIENTATION.equals("+Z")) issues.add("canonical orientation must remain +Z");
        if (DeepHoldV4Plan.ROOMS.size() != EXPECTED_ROOMS) {
            issues.add("expected " + EXPECTED_ROOMS + " rooms, found " + DeepHoldV4Plan.ROOMS.size());
        }
        if (DeepHoldV4Plan.FIXTURES.size() != EXPECTED_FIXTURES) {
            issues.add("expected " + EXPECTED_FIXTURES + " fixtures, found " + DeepHoldV4Plan.FIXTURES.size());
        }
        if (DeepHoldV4Plan.GATES.size() != EXPECTED_GATES) {
            issues.add("expected " + EXPECTED_GATES + " gates, found " + DeepHoldV4Plan.GATES.size());
        }
        for (GateContract contract : GATE_CONTRACTS) {
            DeepHoldV4Plan.Gate planGate = DeepHoldV4Plan.GATES.stream()
                    .filter(gate -> gate.id().equals(contract.planId())).findFirst().orElse(null);
            if (planGate == null) {
                issues.add("gate contract " + contract.siteId() + " has no plan gate " + contract.planId());
            } else if (!planGate.openCondition().equals(contract.openCondition())) {
                issues.add("gate " + contract.siteId() + " condition drift: " + planGate.openCondition()
                        + " != " + contract.openCondition());
            }
        }

        Set<String> artifactIds = new HashSet<>();
        for (Artifact artifact : ARTIFACTS) {
            if (artifact.id().isBlank() || artifact.markerKey().isBlank() || artifact.material().isBlank()) {
                issues.add("artifact has a blank required field: " + artifact);
            }
            if (!artifactIds.add(artifact.id())) issues.add("duplicate artifact id " + artifact.id());
            if (!Set.of("byte", "integer", "string").contains(artifact.markerType())) {
                issues.add("artifact " + artifact.id() + " has unsupported marker type " + artifact.markerType());
            }
            if (artifact.markerType().equals("string") && artifact.markerValue().isBlank()) {
                issues.add("string artifact " + artifact.id() + " has a blank marker value");
            }
        }
        if (ARTIFACTS.size() != EXPECTED_V5_ARTIFACTS) {
            issues.add("expected " + EXPECTED_V5_ARTIFACTS + " V5 artifacts, found " + ARTIFACTS.size());
        }

        Set<String> contractIds = new HashSet<>();
        for (ContentContract contract : CONTENT) {
            if (!contractIds.add(contract.id())) issues.add("duplicate content contract " + contract.id());
            if (contract.exactCount() <= 0) issues.add("content contract " + contract.id() + " has no instances");
            if (contract.payload().isBlank()) issues.add("content contract " + contract.id() + " has no payload rule");
        }
        verifyFixtureCount(issues, "mara_lectern", 5);
        verifyFixtureCount(issues, "sella_lectern", 5);
        verifyFixtureCount(issues, "orin_frame_dial", 6);
        verifyFixtureCount(issues, "vaun_bookshelf", 1);
        verifyFixtureCount(issues, "keeper_stone", 6);
        issues.addAll(V5AuthorityManifest.inspect().issues());
        return List.copyOf(issues);
    }

    /** Stable SHA-256 over all geometry-facing and key-content contracts. */
    public static String contentHash() {
        List<String> lines = new ArrayList<>();
        lines.add("runtime=" + RUNTIME_VERSION);
        lines.add("geometry=" + DeepHoldV4Plan.GEOMETRY_REVISION);
        lines.add("orientation=" + CANONICAL_ORIENTATION);
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) lines.add("room=" + room);
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) lines.add("fixture=" + fixture);
        for (DeepHoldV4Plan.Gate gate : DeepHoldV4Plan.GATES) lines.add("gate=" + gate);
        for (Artifact artifact : ARTIFACTS) lines.add("artifact=" + artifact);
        for (ContentContract contract : CONTENT) lines.add("content=" + contract);
        for (GateContract contract : GATE_CONTRACTS) lines.add("gate-contract=" + contract);
        lines.add("authority=" + V5AuthorityManifest.inspect().authorityHash());
        lines.sort(Comparator.naturalOrder());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format(Locale.ROOT, "%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static Artifact artifact(String id) {
        String wanted = normalized(id);
        for (Artifact artifact : ARTIFACTS) if (artifact.id().equals(wanted)) return artifact;
        return null;
    }

    public static GateContract gateContract(String siteId) {
        return GATES_BY_SITE.get(normalized(siteId));
    }

    public static List<String> gateRequiredFlags(String siteId) {
        GateContract contract = gateContract(siteId);
        return contract == null ? List.of() : contract.requiredFlags();
    }

    /** Local offset behind a fixture front; deliberately has no world-origin input. */
    public static int[] behindFixture(String front, int distance) {
        int d = Math.max(1, distance);
        return switch (front == null ? "" : front.trim().toUpperCase(Locale.ROOT)) {
            case "NORTH" -> new int[]{0, d};
            case "SOUTH" -> new int[]{0, -d};
            case "EAST" -> new int[]{-d, 0};
            case "WEST" -> new int[]{d, 0};
            default -> throw new IllegalArgumentException("front must be NORTH, SOUTH, EAST, or WEST");
        };
    }

    public static Set<String> managedSiteIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) ids.add(fixture.id());
        ids.add("deep_hold_region");
        ids.add("deep_hold_entry_stair");
        return Set.copyOf(ids);
    }

    private static void verifyFixtureCount(List<String> issues, String type, int expected) {
        long actual = DeepHoldV4Plan.FIXTURES.stream().filter(f -> type.equals(f.type())).count();
        if (actual != expected) issues.add("fixture type " + type + " expected " + expected + ", found " + actual);
    }

    private static GateContract gate(String siteId, String planId, String... requiredFlags) {
        List<String> flags = List.of(requiredFlags);
        return new GateContract(siteId, planId, String.join("_and_", flags), flags);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
