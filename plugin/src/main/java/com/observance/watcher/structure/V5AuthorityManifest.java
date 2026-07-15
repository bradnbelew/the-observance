package com.observance.watcher.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Packaged, executable parity contract for the authored V5 ARG sources.
 *
 * <p>The Gradle resource task copies the current canonical runtime resources into
 * {@code v5/authority/} without rewriting them. A Hold plan/build calls
 * {@link DeepHoldV5Manifest#validate()}, which delegates here and fails before block mutation if
 * node, room, fixture, gate, artifact, book, NPC, media, or predicate authority is absent or
 * internally inconsistent. The spoiler casebook is test-only and is never packaged in production.
 */
public final class V5AuthorityManifest {

    public static final String NODE_RESOURCE = "v5/authority/ARG-V5-NODE-MANIFEST.csv";
    public static final String ROOM_RESOURCE = "v5/authority/ARG-V5-ROOM-ASSIGNMENTS.csv";
    public static final String ARTIFACT_RESOURCE = "v5/authority/ARG-V5-ARTIFACT-MANIFEST.csv";
    public static final String BOOK_PLACEMENT_RESOURCE = "v5/authority/ARG-V5-BOOK-PLACEMENT.csv";
    public static final String FIXTURE_RESOURCE = "v5/authority/ARG-V5-FIXTURE-OWNERSHIP.csv";
    public static final String RECORD_OWNERSHIP_RESOURCE = "v5/authority/ARG-V5-RECORD-OWNERSHIP.csv";
    public static final String RUNTIME_BINDING_RESOURCE = "v5/authority/ARG-V5-RUNTIME-BINDINGS.csv";
    public static final String PHYSICAL_PREDICATE_RESOURCE =
            "v5/authority/ARG-V5-PHYSICAL-PREDICATES.json";
    public static final String GATE_RESOURCE = "v5/authority/DEEP-HOLD-GATE-MANIFEST.csv";
    public static final String RECORD_STATION_RESOURCE =
            "v5/authority/DEEP-HOLD-RECORD-STATION-MANIFEST.csv";
    public static final String BOOK_RESOURCE = "v5/authority/minecraft-books.json";
    public static final String NPC_RESOURCE = "v5/authority/npc-dialogue.json";
    public static final String MEDIA_RESOURCE = "v5/authority/media-manifest.json";
    public static final String EVIDENCE_TEXT_RESOURCE =
            "v5/authority/evidence-item-text.json";
    public static final String EVIDENCE_APPEARANCE_RESOURCE =
            "v5/authority/evidence-item-appearance.json";
    public static final String MAP_ART_MANIFEST_RESOURCE =
            "v5/authority/map-art-manifest.json";
    public static final String CASEBOOK_RESOURCE = "v5/authority/SOLUTION-CASEBOOK.md";

    public static final List<String> RESOURCES = List.of(
            NODE_RESOURCE, ROOM_RESOURCE, ARTIFACT_RESOURCE, BOOK_PLACEMENT_RESOURCE,
            FIXTURE_RESOURCE, RECORD_OWNERSHIP_RESOURCE, RUNTIME_BINDING_RESOURCE,
            PHYSICAL_PREDICATE_RESOURCE, GATE_RESOURCE, RECORD_STATION_RESOURCE,
            BOOK_RESOURCE, NPC_RESOURCE, MEDIA_RESOURCE, EVIDENCE_TEXT_RESOURCE,
            EVIDENCE_APPEARANCE_RESOURCE, MAP_ART_MANIFEST_RESOURCE);

    public record RuntimeBinding(String nodeId, String owner, String handler, String siteId,
                                 String completionFlag, String replayPolicy) {
        public RuntimeBinding {
            nodeId = value(nodeId);
            owner = clean(owner);
            handler = clean(handler);
            siteId = clean(siteId);
            completionFlag = value(completionFlag);
            replayPolicy = clean(replayPolicy);
        }
    }

    public record BookPlacement(String bookId, String nodeId, String holderKind, String holderId,
                                String mount, String expectedFront, String availabilityFlag,
                                String artifactId) {
        public BookPlacement {
            bookId = clean(bookId);
            nodeId = value(nodeId);
            holderKind = clean(holderKind);
            holderId = clean(holderId);
            mount = clean(mount);
            expectedFront = value(expectedFront).toUpperCase(Locale.ROOT);
            availabilityFlag = value(availabilityFlag);
            artifactId = clean(artifactId);
        }
    }

    /** Exact node-critical artifact authority; the recovery flag is parsed from its contract. */
    public record ArtifactEntry(String id, String earnedNode, String material, String displayName,
                                String pdcKey, String pdcValue, String consumer,
                                String recoveryFlag, String recoveryContract) {
        public ArtifactEntry {
            id = clean(id);
            earnedNode = value(earnedNode);
            material = value(material).toUpperCase(Locale.ROOT);
            displayName = value(displayName);
            pdcKey = clean(pdcKey);
            pdcValue = clean(pdcValue);
            consumer = value(consumer);
            recoveryFlag = value(recoveryFlag);
            recoveryContract = value(recoveryContract);
        }
    }

    /** Exact authored written-book payload, ready for a Bukkit adapter to materialize. */
    public record BookEntry(String id, String nodeId, String title, String author, String roomId,
                            String unlockFlag, List<String> pages) {
        public BookEntry {
            id = clean(id);
            nodeId = value(nodeId);
            title = value(title);
            author = value(author);
            roomId = clean(roomId);
            unlockFlag = value(unlockFlag);
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    /** Immutable result exposed to runtime preflight, commands, and dependency-light tests. */
    public record Report(int nodeCount, int roomCount, int fixtureCount, int gateCount, int bookCount,
                         int bookPlacementCount, int artifactCount, int recordCount,
                         int runtimeBindingCount, int physicalPredicateCount,
                         int townspersonCount, int mediaCount, String authorityHash,
                         List<String> issues) {
        public Report {
            authorityHash = value(authorityHash);
            issues = issues == null ? List.of() : List.copyOf(issues);
        }

        public boolean valid() {
            return issues.isEmpty();
        }
    }

    private record Snapshot(Report report, List<BookEntry> books, List<ArtifactEntry> artifacts,
                            List<BookPlacement> bookPlacements, List<RuntimeBinding> runtimeBindings,
                            Set<String> nodeIds) { }

    private static volatile Snapshot cached;

    private V5AuthorityManifest() { }

    public static Report inspect() {
        return snapshot().report();
    }

    public static List<BookEntry> books() {
        return snapshot().books();
    }

    public static BookEntry book(String id) {
        String wanted = clean(id);
        for (BookEntry book : books()) if (book.id().equals(wanted)) return book;
        return null;
    }

    public static List<ArtifactEntry> artifacts() {
        return snapshot().artifacts();
    }

    public static ArtifactEntry artifact(String id) {
        String wanted = clean(id);
        for (ArtifactEntry artifact : artifacts()) if (artifact.id().equals(wanted)) return artifact;
        return null;
    }

    public static List<RuntimeBinding> runtimeBindings() {
        return snapshot().runtimeBindings();
    }

    public static List<BookPlacement> bookPlacements() {
        return snapshot().bookPlacements();
    }

    /** Test-only answer-key coverage. The resource is intentionally absent from production JARs. */
    public static List<String> casebookIssues() {
        List<String> issues = new ArrayList<>();
        byte[] bytes = readResource(CASEBOOK_RESOURCE, issues);
        if (!issues.isEmpty()) return List.copyOf(issues);
        validateCasebook(bytes, snapshot().nodeIds(), issues);
        return List.copyOf(issues);
    }

    private static Snapshot snapshot() {
        Snapshot local = cached;
        if (local != null) return local;
        synchronized (V5AuthorityManifest.class) {
            local = cached;
            if (local == null) {
                local = load();
                cached = local;
            }
        }
        return local;
    }

    private static Snapshot load() {
        List<String> issues = new ArrayList<>();
        Map<String, byte[]> payloads = new LinkedHashMap<>();
        for (String resource : RESOURCES) {
            byte[] bytes = readResource(resource, issues);
            if (bytes.length > 0) payloads.put(resource, bytes);
        }
        String authorityHash = hashResources(payloads);

        List<Map<String, String>> nodes = csv(payloads.get(NODE_RESOURCE), NODE_RESOURCE, issues);
        List<Map<String, String>> rooms = csv(payloads.get(ROOM_RESOURCE), ROOM_RESOURCE, issues);
        List<Map<String, String>> artifacts = csv(payloads.get(ARTIFACT_RESOURCE), ARTIFACT_RESOURCE, issues);
        List<Map<String, String>> bookPlacements = csv(
                payloads.get(BOOK_PLACEMENT_RESOURCE), BOOK_PLACEMENT_RESOURCE, issues);
        List<Map<String, String>> fixtures = csv(payloads.get(FIXTURE_RESOURCE), FIXTURE_RESOURCE, issues);
        List<Map<String, String>> recordOwnership = csv(
                payloads.get(RECORD_OWNERSHIP_RESOURCE), RECORD_OWNERSHIP_RESOURCE, issues);
        List<Map<String, String>> runtimeRows = csv(
                payloads.get(RUNTIME_BINDING_RESOURCE), RUNTIME_BINDING_RESOURCE, issues);
        List<Map<String, String>> gates = csv(payloads.get(GATE_RESOURCE), GATE_RESOURCE, issues);
        List<Map<String, String>> recordStations = csv(
                payloads.get(RECORD_STATION_RESOURCE), RECORD_STATION_RESOURCE, issues);

        Set<String> nodeIds = validateNodes(nodes, issues);
        Map<String, Map<String, String>> nodesById = new HashMap<>();
        for (Map<String, String> node : nodes) nodesById.put(value(node.get("node_id")), node);
        Set<String> completionFlags = new HashSet<>();
        for (Map<String, String> node : nodes) completionFlags.add(value(node.get("completion_flag")));
        validateRooms(rooms, issues);
        validateFixtures(fixtures, nodeIds, issues);
        validateGates(gates, completionFlags, issues);
        List<ArtifactEntry> artifactEntries = validateArtifacts(
                artifacts, nodeIds, completionFlags, issues);
        List<BookEntry> books = validateBooks(payloads.get(BOOK_RESOURCE), nodeIds, completionFlags, issues);
        List<BookPlacement> placementEntries = validateBookPlacements(
                bookPlacements, books, artifactEntries, issues);
        validatePhysicalBookMountContracts(placementEntries, issues);
        validateRecordStations(recordStations, recordOwnership, nodeIds, issues);
        List<RuntimeBinding> runtimeBindings = validateRuntimeBindings(runtimeRows, nodesById, issues);
        int physicalPredicates = validatePhysicalPredicates(
                payloads.get(PHYSICAL_PREDICATE_RESOURCE), runtimeBindings, issues);
        int townsfolk = validateNpc(payloads.get(NPC_RESOURCE), issues);
        int media = validateMedia(payloads.get(MEDIA_RESOURCE), nodeIds, completionFlags, issues);

        Report report = new Report(nodes.size(), rooms.size(), fixtures.size(), gates.size(), books.size(),
                bookPlacements.size(), artifactEntries.size(), recordStations.size(), runtimeBindings.size(),
                physicalPredicates, townsfolk, media, authorityHash, issues);
        return new Snapshot(report, List.copyOf(books), List.copyOf(artifactEntries),
                List.copyOf(placementEntries), List.copyOf(runtimeBindings), Set.copyOf(nodeIds));
    }

    private static byte[] readResource(String resource, List<String> issues) {
        try (InputStream stream = V5AuthorityManifest.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                issues.add("missing packaged V5 authority resource " + resource);
                return new byte[0];
            }
            byte[] bytes = stream.readAllBytes();
            if (bytes.length == 0) issues.add("empty packaged V5 authority resource " + resource);
            return bytes;
        } catch (IOException failure) {
            issues.add("cannot read packaged V5 authority resource " + resource + ": " + failure.getMessage());
            return new byte[0];
        }
    }

    private static Set<String> validateNodes(List<Map<String, String>> rows, List<String> issues) {
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_NODES) {
            issues.add("V5 node authority expected " + DeepHoldV5Manifest.EXPECTED_NODES
                    + " rows, found " + rows.size());
        }
        Set<String> ids = new LinkedHashSet<>();
        Set<String> flags = new HashSet<>();
        for (Map<String, String> row : rows) {
            String id = value(row.get("node_id"));
            String flag = value(row.get("completion_flag"));
            if (id.isBlank()) issues.add("V5 node row has blank node_id");
            else if (!ids.add(id)) issues.add("duplicate V5 node_id " + id);
            if (value(row.get("case_id")).isBlank() || value(row.get("title")).isBlank()
                    || value(row.get("room_id")).isBlank() || value(row.get("modality")).isBlank()
                    || value(row.get("input_surface")).isBlank()) {
                issues.add("V5 node " + id + " has a blank required field");
            }
            if (flag.isBlank()) issues.add("V5 node " + id + " has no completion_flag");
            else if (!flags.add(flag)) issues.add("duplicate V5 completion_flag " + flag);
        }
        for (String required : List.of("LS01", "LC06", "KV03", "KM03", "KS03", "KO03",
                "KB03", "KI03", "BI08", "HS07", "A10", "WR05", "AR08", "RP06")) {
            if (!ids.contains(required)) issues.add("V5 node authority is missing required node " + required);
        }
        return ids;
    }

    private static void validateRooms(List<Map<String, String>> rows, List<String> issues) {
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_ROOMS) {
            issues.add("V5 room authority expected " + DeepHoldV5Manifest.EXPECTED_ROOMS
                    + " rows, found " + rows.size());
        }
        Map<String, DeepHoldV4Plan.Room> planned = new HashMap<>();
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) planned.put(room.id(), room);
        Set<String> seen = new HashSet<>();
        for (Map<String, String> row : rows) {
            String id = clean(row.get("room_id"));
            if (!seen.add(id)) issues.add("duplicate V5 room assignment " + id);
            DeepHoldV4Plan.Room room = planned.get(id);
            if (room == null) {
                issues.add("V5 room assignment " + id + " has no physical room");
                continue;
            }
            Integer floor = integer(row.get("floor_y"), "room " + id + " floor_y", issues);
            if (floor != null && floor != room.floorY()) {
                issues.add("V5 room " + id + " floor drift: " + floor + " != " + room.floorY());
            }
            if (value(row.get("node_ownership")).isBlank() || value(row.get("route_contract")).isBlank()) {
                issues.add("V5 room " + id + " lacks node ownership or route contract");
            }
        }
        for (String id : planned.keySet()) if (!seen.contains(id)) issues.add("physical room " + id
                + " has no V5 authority assignment");
    }

    private static void validateFixtures(List<Map<String, String>> rows, Set<String> nodeIds,
                                         List<String> issues) {
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_FIXTURES) {
            issues.add("V5 fixture authority expected " + DeepHoldV5Manifest.EXPECTED_FIXTURES
                    + " rows, found " + rows.size());
        }
        Set<String> planned = new HashSet<>();
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) planned.add(fixture.id());
        Set<String> seen = new HashSet<>();
        for (Map<String, String> row : rows) {
            String id = clean(row.get("site_id"));
            if (id.isBlank() || !seen.add(id)) issues.add("blank or duplicate V5 fixture ownership " + id);
            if (!planned.contains(id)) issues.add("V5 fixture ownership " + id + " has no physical fixture");
            String ownedNodes = value(row.get("node_ids"));
            if (ownedNodes.isBlank()) issues.add("V5 fixture " + id + " has no node ownership");
            for (String node : ownedNodes.split(";")) {
                String candidate = value(node);
                if (!candidate.isBlank() && !nodeExpressionExists(candidate, nodeIds)) {
                    issues.add("V5 fixture " + id + " references unknown node " + candidate);
                }
            }
            if (value(row.get("v5_role")).isBlank() || value(row.get("critical_payload")).isBlank()) {
                issues.add("V5 fixture " + id + " lacks role or critical payload declaration");
            }
        }
        for (String id : planned) if (!seen.contains(id)) issues.add("physical fixture " + id
                + " has no V5 ownership row");
    }

    private static boolean nodeExpressionExists(String expression, Set<String> nodeIds) {
        if (nodeIds.contains(expression)) return true;
        int dash = expression.indexOf('-');
        if (dash < 1) return false;
        String first = expression.substring(0, dash);
        String last = expression.substring(dash + 1);
        int firstDigit = first.length();
        while (firstDigit > 0 && Character.isDigit(first.charAt(firstDigit - 1))) firstDigit--;
        int lastDigit = last.length();
        while (lastDigit > 0 && Character.isDigit(last.charAt(lastDigit - 1))) lastDigit--;
        String prefix = first.substring(0, firstDigit);
        if (!prefix.equals(last.substring(0, lastDigit))) return false;
        try {
            int start = Integer.parseInt(first.substring(firstDigit));
            int end = Integer.parseInt(last.substring(lastDigit));
            int width = first.length() - firstDigit;
            if (end < start) return false;
            for (int i = start; i <= end; i++) {
                if (!nodeIds.contains(prefix + String.format(Locale.ROOT, "%0" + width + "d", i))) return false;
            }
            return true;
        } catch (NumberFormatException failure) {
            return false;
        }
    }

    private static List<ArtifactEntry> validateArtifacts(List<Map<String, String>> rows,
                                                         Set<String> nodeIds,
                                                         Set<String> completionFlags,
                                                         List<String> issues) {
        List<ArtifactEntry> artifacts = new ArrayList<>();
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_V5_ARTIFACTS) {
            issues.add("V5 artifact authority expected " + DeepHoldV5Manifest.EXPECTED_V5_ARTIFACTS
                    + " rows, found " + rows.size());
        }
        Set<String> ids = new HashSet<>();
        for (Map<String, String> row : rows) {
            String contract = value(row.get("recovery_contract"));
            String recoveryFlag = recoveryFlag(contract);
            ArtifactEntry artifact = new ArtifactEntry(row.get("artifact_id"), row.get("earned_node"),
                    row.get("material"), row.get("display_name"), row.get("pdc_key"),
                    row.get("pdc_value"), row.get("consumer"), recoveryFlag, contract);
            artifacts.add(artifact);
            if (artifact.id().isBlank() || !ids.add(artifact.id())) {
                issues.add("blank or duplicate V5 artifact id " + artifact.id());
            }
            if (!nodeIds.contains(artifact.earnedNode())) issues.add("V5 artifact " + artifact.id()
                    + " references unknown earned node " + artifact.earnedNode());
            if (!"v5_artifact_id".equals(artifact.pdcKey()) || !artifact.id().equals(artifact.pdcValue())) {
                issues.add("V5 artifact " + artifact.id() + " must use v5_artifact_id=self");
            }
            if (artifact.displayName().isBlank() || artifact.material().isBlank()
                    || artifact.consumer().isBlank() || artifact.recoveryContract().isBlank()) {
                issues.add("V5 artifact " + artifact.id() + " has a blank required field");
            }
            if (artifact.recoveryFlag().isBlank() || !completionFlags.contains(artifact.recoveryFlag())) {
                issues.add("V5 artifact " + artifact.id() + " has unknown recovery flag "
                        + artifact.recoveryFlag());
            }
            DeepHoldV5Manifest.Artifact runtime = DeepHoldV5Manifest.artifact(artifact.id());
            if (runtime == null) {
                issues.add("V5 artifact " + artifact.id() + " is absent from the runtime registry");
            } else if (!runtime.material().equals(artifact.material())
                    || !runtime.markerKey().equals(artifact.pdcKey())
                    || !runtime.markerValue().equals(artifact.pdcValue())) {
                issues.add("V5 artifact " + artifact.id() + " runtime material/PDC drifts from authority");
            }
        }
        long runtimeV5 = DeepHoldV5Manifest.ARTIFACTS.stream()
                .filter(artifact -> "v5_artifact_id".equals(artifact.markerKey())).count();
        if (runtimeV5 != rows.size()) issues.add("runtime V5 artifact registry has " + runtimeV5
                + " entries; authority has " + rows.size());
        return artifacts;
    }

    private static String recoveryFlag(String contract) {
        String lower = value(contract).toLowerCase(Locale.ROOT);
        int end = lower.indexOf(" after duplicate scan");
        if (end < 0) return "";
        int from = lower.lastIndexOf(" from ", end);
        if (from < 0) return "";
        int start = from + " from ".length();
        return end < 0 ? "" : value(lower.substring(start, end));
    }

    private static void validateGates(List<Map<String, String>> rows, Set<String> completionFlags,
                                      List<String> issues) {
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_GATES) {
            issues.add("V5 gate authority expected " + DeepHoldV5Manifest.EXPECTED_GATES
                    + " rows, found " + rows.size());
        }
        Map<String, DeepHoldV4Plan.Gate> planned = new HashMap<>();
        for (DeepHoldV4Plan.Gate gate : DeepHoldV4Plan.GATES) planned.put(gate.id(), gate);
        Set<String> seen = new HashSet<>();
        for (Map<String, String> row : rows) {
            String id = clean(row.get("gate_id"));
            if (!seen.add(id)) issues.add("duplicate V5 gate authority " + id);
            DeepHoldV5Manifest.GateContract contract = DeepHoldV5Manifest.gateContract(id);
            if (contract == null) {
                issues.add("unknown V5 authority gate " + id);
                continue;
            }
            String condition = value(row.get("open_condition"));
            if (!condition.equals(contract.openCondition())) {
                issues.add("V5 gate " + id + " condition drift: " + condition + " != "
                        + contract.openCondition());
            }
            for (String flag : contract.requiredFlags()) if (!completionFlags.contains(flag)) {
                issues.add("V5 gate " + id + " references unknown completion flag " + flag);
            }
            if (!"sealed".equals(clean(row.get("initial_state")))) {
                issues.add("V5 gate " + id + " must initially be sealed");
            }
            DeepHoldV4Plan.Gate plan = planned.get(contract.planId());
            if (plan != null) validateGateBounds(row, id, plan, issues);
        }
        for (DeepHoldV5Manifest.GateContract contract : DeepHoldV5Manifest.GATE_CONTRACTS) {
            if (!seen.contains(contract.siteId())) issues.add("missing V5 gate authority " + contract.siteId());
        }
    }

    private static void validateGateBounds(Map<String, String> row, String id, DeepHoldV4Plan.Gate gate,
                                           List<String> issues) {
        int expectedMinX = gate.acrossX() ? gate.x() - gate.halfAcross() : gate.x();
        int expectedMaxX = gate.acrossX() ? gate.x() + gate.halfAcross() : gate.x() + gate.depth();
        int expectedMinZ = gate.acrossX() ? gate.z() : gate.z() - gate.halfAcross();
        int expectedMaxZ = gate.acrossX() ? gate.z() + gate.depth() : gate.z() + gate.halfAcross();
        int[] expected = {expectedMinX, expectedMaxX, gate.y(), gate.y() + gate.height() - 1,
                expectedMinZ, expectedMaxZ};
        String[] columns = {"min_x", "max_x", "min_y", "max_y", "min_z", "max_z"};
        for (int i = 0; i < columns.length; i++) {
            Integer actual = integer(row.get(columns[i]), "gate " + id + " " + columns[i], issues);
            if (actual != null) {
                int runtime = compactLegacyGateBound(row, columns[i], actual, gate, issues);
                if (runtime != expected[i]) issues.add("V5 gate " + id + " " + columns[i]
                        + " runtime drift: " + runtime + " != " + expected[i]);
            }
        }
    }

    /** Gate CSV coordinates remain the stable legacy authoring grid; runtime plans are compact. */
    private static int compactLegacyGateBound(Map<String, String> row, String column, int actual,
                                              DeepHoldV4Plan.Gate gate, List<String> issues) {
        if ("min_y".equals(column) || "max_y".equals(column)) return actual;
        if ("min_x".equals(column)) return DeepHoldV4Plan.compactX(actual);
        if ("max_x".equals(column)) {
            if (gate.acrossX()) return DeepHoldV4Plan.compactX(actual);
            Integer legacyMin = integer(row.get("min_x"), "gate legacy min_x", issues);
            return legacyMin == null ? Integer.MIN_VALUE
                    : DeepHoldV4Plan.compactX(legacyMin) + gate.depth();
        }
        if (!gate.acrossX() && ("min_z".equals(column) || "max_z".equals(column))) {
            Integer legacyMin = integer(row.get("min_z"), "gate legacy min_z", issues);
            Integer legacyMax = integer(row.get("max_z"), "gate legacy max_z", issues);
            if (legacyMin == null || legacyMax == null) return Integer.MIN_VALUE;
            int center = DeepHoldV4Plan.compactZ(gate.y(), (legacyMin + legacyMax) / 2);
            return "min_z".equals(column) ? center - gate.halfAcross() : center + gate.halfAcross();
        }
        if ("min_z".equals(column)) return DeepHoldV4Plan.compactZ(gate.y(), actual);
        if (gate.acrossX()) {
            Integer legacyMin = integer(row.get("min_z"), "gate legacy min_z", issues);
            return legacyMin == null ? Integer.MIN_VALUE
                    : DeepHoldV4Plan.compactZ(gate.y(), legacyMin) + gate.depth();
        }
        return DeepHoldV4Plan.compactZ(gate.y(), actual);
    }

    private static List<BookEntry> validateBooks(byte[] bytes, Set<String> nodeIds,
                                                  Set<String> completionFlags, List<String> issues) {
        List<BookEntry> books = new ArrayList<>();
        JsonObject root = jsonObject(bytes, BOOK_RESOURCE, issues);
        if (root == null) return books;
        validateStoryVersion(root, BOOK_RESOURCE, issues);
        JsonArray entries = array(root, "books", BOOK_RESOURCE, issues);
        if (entries == null) return books;
        Set<String> ids = new HashSet<>();
        Set<String> roomIds = new HashSet<>();
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) roomIds.add(room.id());
        roomIds.add("unlit_base");
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                issues.add("V5 book entry is not an object");
                continue;
            }
            JsonObject value = element.getAsJsonObject();
            List<String> pages = strings(value.get("pages"));
            BookEntry book = new BookEntry(string(value, "id"), string(value, "nodeId"),
                    string(value, "title"), string(value, "author"), string(value, "roomId"),
                    string(value, "unlockFlag"), pages);
            books.add(book);
            if (book.id().isBlank() || !ids.add(book.id())) issues.add("blank or duplicate V5 book id " + book.id());
            if (!nodeIds.contains(book.nodeId())) issues.add("V5 book " + book.id()
                    + " references unknown node " + book.nodeId());
            if (!roomIds.contains(book.roomId())) issues.add("V5 book " + book.id()
                    + " references unknown room " + book.roomId());
            if (book.title().isBlank() || book.author().isBlank()) issues.add("V5 book " + book.id()
                    + " has blank title or author");
            if (book.title().codePointCount(0, book.title().length()) > 32) issues.add("V5 book "
                    + book.id() + " title exceeds 32 characters");
            if (book.pages().isEmpty()) issues.add("V5 book " + book.id() + " has no pages");
            for (int i = 0; i < book.pages().size(); i++) {
                String page = book.pages().get(i);
                if (page.isBlank()) issues.add("V5 book " + book.id() + " page " + (i + 1) + " is blank");
                if (page.codePointCount(0, page.length()) > 240) issues.add("V5 book " + book.id()
                        + " page " + (i + 1) + " exceeds 240 characters");
            }
            if (!completionFlags.contains(book.unlockFlag())
                    && !Set.of("v5_coda_publish", "v5_coda_unfiled").contains(book.unlockFlag())) {
                issues.add("V5 book " + book.id() + " has unknown unlock flag " + book.unlockFlag());
            }
        }
        if (books.size() != DeepHoldV5Manifest.EXPECTED_BOOKS) issues.add("V5 book authority expected "
                + DeepHoldV5Manifest.EXPECTED_BOOKS + " entries, found " + books.size());
        return books;
    }

    private static List<BookPlacement> validateBookPlacements(List<Map<String, String>> rows,
                                                              List<BookEntry> books,
                                                              List<ArtifactEntry> artifacts,
                                                              List<String> issues) {
        List<BookPlacement> placements = new ArrayList<>();
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_BOOKS) issues.add("V5 book placement expected "
                + DeepHoldV5Manifest.EXPECTED_BOOKS + " rows, found " + rows.size());
        Map<String, BookEntry> booksById = new HashMap<>();
        for (BookEntry book : books) booksById.put(book.id(), book);
        Set<String> artifactIds = new HashSet<>();
        for (ArtifactEntry artifact : artifacts) artifactIds.add(artifact.id());
        Set<String> seen = new HashSet<>();
        int affidavitArtifacts = 0;
        for (Map<String, String> row : rows) {
            String id = clean(row.get("book_id"));
            if (id.isBlank() || !seen.add(id)) issues.add("blank or duplicate V5 book placement " + id);
            BookEntry book = booksById.get(id);
            if (book == null) {
                issues.add("V5 placement references unknown book " + id);
                continue;
            }
            if (!book.nodeId().equals(value(row.get("node_id")))) issues.add("V5 book " + id
                    + " placement node drifts from catalog");
            if (!id.equals(clean(row.get("book_pdc_id")))) issues.add("V5 book " + id
                    + " must use book_pdc_id=self");
            String availability = clean(row.get("availability"));
            String unlock = availability.replaceFirst("^(visible_when_|issue_when_|visible_only_when_)", "");
            placements.add(new BookPlacement(id, row.get("node_id"), row.get("holder_kind"),
                    row.get("holder_id"), row.get("mount"), row.get("expected_front"), unlock,
                    row.get("artifact_id")));
            if (!book.unlockFlag().equals(unlock)) issues.add("V5 book " + id + " availability "
                    + availability + " drifts from unlock " + book.unlockFlag());
            String kind = clean(row.get("holder_kind"));
            if (!Set.of("locked_lectern", "earned_artifact", "branch_lectern").contains(kind)) {
                issues.add("V5 book " + id + " has unsupported holder kind " + kind);
            }
            if (clean(row.get("holder_id")).isBlank() || clean(row.get("mount")).isBlank()
                    || clean(row.get("expected_front")).isBlank()) {
                issues.add("V5 book " + id + " has incomplete holder/mount/front data");
            }
            String artifact = clean(row.get("artifact_id"));
            if ("earned_artifact".equals(kind)) {
                affidavitArtifacts++;
                if (!artifactIds.contains(artifact) || !artifact.startsWith("affidavit_")) {
                    issues.add("V5 earned book " + id + " has no linked affidavit artifact");
                }
            } else if (!"none".equals(artifact)) {
                issues.add("V5 non-earned book " + id + " unexpectedly links artifact " + artifact);
            }
        }
        if (affidavitArtifacts != 6) issues.add("V5 book placement requires six affidavit artifacts, found "
                + affidavitArtifacts);
        for (String id : booksById.keySet()) if (!seen.contains(id)) issues.add("V5 book " + id
                + " has no exact placement row");
        return placements;
    }

    private static void validateRecordStations(List<Map<String, String>> stations,
                                               List<Map<String, String>> ownership,
                                               Set<String> nodeIds, List<String> issues) {
        if (stations.size() != 7 || ownership.size() != 7) issues.add("V5 record authority needs 7 stations/7 "
                + "ownership rows, found " + stations.size() + "/" + ownership.size());
        Map<String, DeepHoldV4Plan.RecordStation> planned = new HashMap<>();
        for (DeepHoldV4Plan.RecordStation station : DeepHoldV4Plan.RECORD_STATIONS) {
            planned.put(station.id(), station);
        }
        Set<String> stationIds = new HashSet<>();
        for (Map<String, String> row : stations) {
            String id = clean(row.get("station_id"));
            if (id.isBlank() || !stationIds.add(id)) issues.add("blank or duplicate V5 record station " + id);
            DeepHoldV4Plan.RecordStation plan = planned.get(id);
            if (plan == null) {
                issues.add("V5 record station " + id + " has no physical plan");
                continue;
            }
            String[] columns = {"x", "y", "z"};
            int[] expected = {plan.x(), plan.y(), plan.z()};
            for (int i = 0; i < columns.length; i++) {
                Integer actual = integer(row.get(columns[i]), "record " + id + " " + columns[i], issues);
                if (actual != null) {
                    int runtime = switch (columns[i]) {
                        case "x" -> DeepHoldV4Plan.compactX(actual);
                        case "z" -> DeepHoldV4Plan.compactZ(plan.y(), actual);
                        default -> actual;
                    };
                    if (runtime != expected[i]) issues.add("V5 record " + id + " "
                            + columns[i] + " runtime drift: " + runtime + " != " + expected[i]);
                }
            }
            for (String stand : List.of("stand_x", "stand_y", "stand_z")) {
                integer(row.get(stand), "record " + id + " " + stand, issues);
            }
            if (!plan.roomId().equals(clean(row.get("room_id")))
                    || !plan.front().equalsIgnoreCase(value(row.get("expected_front")))
                    || !plan.title().equals(value(row.get("title")))) {
                issues.add("V5 record station " + id + " room/front/title drifts from physical plan");
            }
            if (value(row.get("required_fragment")).isBlank()) issues.add("V5 record station " + id
                    + " has no required copy fragment");
        }
        Set<String> owned = new HashSet<>();
        for (Map<String, String> row : ownership) {
            String id = clean(row.get("station_id"));
            if (!owned.add(id) || !stationIds.contains(id)) issues.add("V5 record ownership has unknown/duplicate " + id);
            for (String expression : value(row.get("node_ids")).split(";")) {
                if (!nodeExpressionExists(value(expression), nodeIds)) issues.add("V5 record " + id
                        + " references unknown node expression " + expression);
            }
            if (value(row.get("v5_role")).isBlank() || value(row.get("critical_payload")).isBlank()) {
                issues.add("V5 record " + id + " lacks role/payload ownership");
            }
        }
        for (String id : stationIds) if (!owned.contains(id)) issues.add("V5 record " + id
                + " has no ownership row");
    }

    private static List<RuntimeBinding> validateRuntimeBindings(List<Map<String, String>> rows,
                                                               Map<String, Map<String, String>> nodes,
                                                               List<String> issues) {
        List<RuntimeBinding> bindings = new ArrayList<>();
        if (rows.size() != DeepHoldV5Manifest.EXPECTED_NODES) issues.add("V5 runtime bindings expected "
                + DeepHoldV5Manifest.EXPECTED_NODES + " rows, found " + rows.size());
        Set<String> seen = new HashSet<>();
        Set<String> owners = Set.of("website", "discord", "plugin", "plugin_unlit", "plugin_finale");
        for (Map<String, String> row : rows) {
            RuntimeBinding binding = new RuntimeBinding(row.get("node_id"), row.get("owner"),
                    row.get("handler"), row.get("site_id"), row.get("completion_flag"),
                    row.get("replay_policy"));
            bindings.add(binding);
            if (!seen.add(binding.nodeId())) issues.add("duplicate V5 runtime binding " + binding.nodeId());
            Map<String, String> node = nodes.get(binding.nodeId());
            if (node == null) {
                issues.add("V5 runtime binding references unknown node " + binding.nodeId());
                continue;
            }
            if (!binding.completionFlag().equals(value(node.get("completion_flag")))) issues.add("V5 runtime "
                    + binding.nodeId() + " completion flag drifts from node authority");
            if (!owners.contains(binding.owner()) || binding.handler().isBlank()
                    || binding.siteId().isBlank() || binding.replayPolicy().isBlank()) {
                issues.add("V5 runtime binding " + binding.nodeId() + " has an invalid owner/handler/site/replay");
            }
        }
        for (String node : nodes.keySet()) if (!seen.contains(node)) issues.add("V5 node " + node
                + " has no runtime binding");
        return bindings;
    }

    private static int validatePhysicalPredicates(byte[] bytes, List<RuntimeBinding> bindings,
                                                  List<String> issues) {
        JsonObject root = jsonObject(bytes, PHYSICAL_PREDICATE_RESOURCE, issues);
        if (root == null) return 0;
        if (!"1".equals(string(root, "schema_version"))) {
            issues.add("V5 physical predicate schema_version must be 1");
        }
        JsonElement profiles = root.get("durability_profiles");
        if (profiles == null || !profiles.isJsonObject()) {
            issues.add("V5 physical predicates lack durability_profiles");
        } else {
            for (String required : List.of("minecraft_local_primary", "finale_local_primary")) {
                if (!profiles.getAsJsonObject().has(required)) {
                    issues.add("V5 physical predicates lack durability profile " + required);
                }
            }
        }
        JsonArray nodes = array(root, "nodes", PHYSICAL_PREDICATE_RESOURCE, issues);
        if (nodes == null) return 0;
        if (nodes.size() != 60) issues.add("V5 physical predicate authority expected 60 nodes, found "
                + nodes.size());

        Map<String, RuntimeBinding> physicalBindings = new LinkedHashMap<>();
        Set<String> allowedOwners = Set.of("plugin", "plugin_unlit", "plugin_finale");
        for (RuntimeBinding binding : bindings) {
            if (allowedOwners.contains(binding.owner())) physicalBindings.put(binding.nodeId(), binding);
        }
        if (physicalBindings.size() != 60) issues.add("V5 runtime bindings expected 60 physical owners, found "
                + physicalBindings.size());

        Set<String> seen = new LinkedHashSet<>();
        JsonObject rp05 = null;
        JsonObject rp06 = null;
        for (JsonElement element : nodes) {
            if (!element.isJsonObject()) {
                issues.add("V5 physical predicate entry is not an object");
                continue;
            }
            JsonObject node = element.getAsJsonObject();
            String id = value(string(node, "node_id"));
            String owner = clean(string(node, "owner"));
            String site = clean(string(node, "site_id"));
            String handler = clean(string(node, "handler"));
            String flag = value(string(node, "completion_flag"));
            if (id.isBlank() || !seen.add(id)) issues.add("blank or duplicate V5 physical node " + id);
            if (!allowedOwners.contains(owner)) issues.add("V5 physical node " + id
                    + " has non-physical owner " + owner);
            RuntimeBinding binding = physicalBindings.get(id);
            if (binding == null) {
                issues.add("V5 physical node " + id + " has no physical runtime binding");
            } else {
                if (!binding.owner().equals(owner)) issues.add("V5 physical node " + id + " owner drift");
                if (!binding.siteId().equals(site)) issues.add("V5 physical node " + id + " site drift");
                if (!binding.handler().equals(handler)) issues.add("V5 physical node " + id + " handler drift");
                if (!binding.completionFlag().equals(flag)) issues.add("V5 physical node " + id
                        + " completion flag drift");
            }
            if (!node.has("prerequisites") || !node.get("prerequisites").isJsonArray()
                    || !node.has("predicate") || !node.get("predicate").isJsonObject()
                    || !node.has("wrong_input") || !node.get("wrong_input").isJsonObject()
                    || !node.has("reward") || !node.get("reward").isJsonObject()
                    || !node.has("reset_repair_recovery")
                    || !node.get("reset_repair_recovery").isJsonObject()
                    || !node.has("concurrency_replay") || !node.get("concurrency_replay").isJsonObject()
                    || string(node, "durability_profile").isBlank()) {
                issues.add("V5 physical node " + id + " lacks a required predicate/recovery contract");
            }
            if ("RP05".equals(id)) rp05 = node;
            if ("RP06".equals(id)) rp06 = node;
        }
        for (String id : physicalBindings.keySet()) {
            if (!seen.contains(id)) issues.add("V5 physical runtime binding " + id
                    + " has no packaged predicate");
        }
        validateFinalePredicates(rp05, rp06, issues);
        return nodes.size();
    }

    private static void validatePhysicalBookMountContracts(List<BookPlacement> placements,
                                                           List<String> issues) {
        Map<String, BookPlacement> byBook = new HashMap<>();
        for (BookPlacement placement : placements) byBook.put(placement.bookId(), placement);
        BookPlacement school = byBook.get("lc_school_day");
        if (school == null || !"orientation_register".equals(school.holderId())
                || !"lectern_left".equals(school.mount()) || !"WEST".equals(school.expectedFront())) {
            issues.add("LC03 school-day book must occupy orientation_register[-2,0,0] on its WEST left mount");
        }
        BookPlacement ledger = byBook.get("vaun_quartermaster_ledger");
        if (ledger == null || !"vaun_hoard_chest".equals(ledger.holderId())
                || !"lectern_left".equals(ledger.mount()) || !"EAST".equals(ledger.expectedFront())) {
            issues.add("KV01 quartermaster ledger must occupy vaun_hoard_chest[-1,0,0] on its EAST left mount");
        }
    }

    private static void validateFinalePredicates(JsonObject rp05, JsonObject rp06, List<String> issues) {
        if (rp05 == null || rp06 == null) {
            issues.add("V5 physical predicates require RP05 and RP06");
            return;
        }
        if (!"release_record".equals(clean(string(rp05, "site_id")))
                || !"armed_confirmation".equals(clean(string(rp05, "handler")))) {
            issues.add("RP05 must bind release_record to armed_confirmation");
        }
        JsonObject rp05Predicate = rp05.getAsJsonObject("predicate");
        JsonObject operatorArm = physicalComponent(rp05Predicate, "operator_arm");
        if (operatorArm == null
                || !"/obs finale arm [15-600 seconds]".equals(string(operatorArm, "command"))
                || operatorArm.get("default_seconds") == null
                || operatorArm.get("default_seconds").getAsInt() != 120
                || operatorArm.get("branch_arguments_allowed") == null
                || operatorArm.get("branch_arguments_allowed").getAsBoolean()) {
            issues.add("RP05 operator arm contract must be branchless /obs finale arm [15-600 seconds], default 120");
        }
        JsonObject durable = physicalComponent(rp05Predicate, "durable_fields");
        List<String> exactFields = List.of("phase", "wren-outcome", "name-treatment", "conduct-verdict",
                "armed-by", "armed-at", "cancel-cutoff-at", "committed-at");
        if (durable == null || !"2".equals(string(durable, "schema"))
                || !strings(durable.get("fields")).equals(exactFields)) {
            issues.add("RP05 durable schema 2 fields drift from finale state machine");
        }
        if (!"release_record".equals(clean(string(rp06, "site_id")))
                || !"automatic_finale".equals(clean(string(rp06, "handler")))) {
            issues.add("RP06 must bind release_record to automatic_finale");
        }
        JsonObject rp06Predicate = rp06.getAsJsonObject("predicate");
        JsonObject matrix = physicalComponent(rp06Predicate, "ending_matrix");
        if (matrix == null || matrix.get("six_combinations") == null
                || !matrix.get("six_combinations").isJsonArray()
                || matrix.getAsJsonArray("six_combinations").size() != 6) {
            issues.add("RP06 must define all six Wren/name ending combinations");
        }
        JsonObject conduct = physicalComponent(rp06Predicate, "conduct_clauses");
        Map<String, String> expected = Map.of(
                "solo", "you carried every name alone. they still arrived together.",
                "unanimous", "you came to one answer without becoming one voice.",
                "divided", "you disagreed and continued together. the record never knew how to write that.",
                "persistent", "you did not agree quickly. you stayed until the evidence did."
        );
        if (conduct == null) {
            issues.add("RP06 lacks conduct clauses");
        } else {
            for (Map.Entry<String, String> clause : expected.entrySet()) {
                if (!clause.getValue().equals(string(conduct, clause.getKey()))) {
                    issues.add("RP06 " + clause.getKey() + " conduct clause drift");
                }
            }
        }
    }

    private static JsonObject physicalComponent(JsonObject predicate, String id) {
        if (predicate == null || predicate.get("components") == null
                || !predicate.get("components").isJsonArray()) return null;
        for (JsonElement element : predicate.getAsJsonArray("components")) {
            if (element.isJsonObject() && id.equals(string(element.getAsJsonObject(), "id"))) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private static int validateNpc(byte[] bytes, List<String> issues) {
        JsonObject root = jsonObject(bytes, NPC_RESOURCE, issues);
        if (root == null) return 0;
        validateStoryVersion(root, NPC_RESOURCE, issues);
        JsonArray townsfolk = array(root, "townsfolk", NPC_RESOURCE, issues);
        if (townsfolk == null) return 0;
        Set<String> ids = new HashSet<>();
        for (JsonElement element : townsfolk) {
            if (!element.isJsonObject()) {
                issues.add("V5 townsfolk entry is not an object");
                continue;
            }
            JsonObject npc = element.getAsJsonObject();
            String id = clean(string(npc, "id"));
            if (id.isBlank() || !ids.add(id)) issues.add("blank or duplicate V5 townsperson id " + id);
            if (string(npc, "displayName").isBlank() || string(npc, "anchorSite").isBlank()) {
                issues.add("V5 townsperson " + id + " lacks display name or anchor site");
            }
            validateDialogueLines(npc.get("lines"), "townsperson " + id, issues);
        }
        if (townsfolk.size() != 5) issues.add("V5 NPC authority expected 5 townsfolk, found " + townsfolk.size());
        JsonElement wrenElement = root.get("wren");
        if (wrenElement == null || !wrenElement.isJsonObject()) {
            issues.add("V5 NPC authority is missing Wren");
        } else {
            validateDialogueLines(wrenElement.getAsJsonObject().get("lines"), "Wren", issues);
        }
        return townsfolk.size();
    }

    private static void validateDialogueLines(JsonElement element, String owner, List<String> issues) {
        if (element == null || !element.isJsonObject() || element.getAsJsonObject().entrySet().isEmpty()) {
            issues.add("V5 " + owner + " has no dialogue groups");
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            List<String> lines = strings(entry.getValue());
            if (lines.isEmpty() || lines.stream().anyMatch(String::isBlank)) {
                issues.add("V5 " + owner + " dialogue group " + entry.getKey() + " is empty");
            }
        }
    }

    private static int validateMedia(byte[] bytes, Set<String> nodeIds, Set<String> completionFlags,
                                     List<String> issues) {
        JsonObject root = jsonObject(bytes, MEDIA_RESOURCE, issues);
        if (root == null) return 0;
        validateStoryVersion(root, MEDIA_RESOURCE, issues);
        JsonArray assets = array(root, "assets", MEDIA_RESOURCE, issues);
        if (assets == null) return 0;
        Set<String> ids = new HashSet<>();
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                issues.add("V5 media entry is not an object");
                continue;
            }
            JsonObject asset = element.getAsJsonObject();
            String id = clean(string(asset, "id"));
            if (id.isBlank() || !ids.add(id)) issues.add("blank or duplicate V5 media id " + id);
            if (!nodeIds.contains(string(asset, "nodeId"))) issues.add("V5 media " + id
                    + " references unknown node " + string(asset, "nodeId"));
            String flag = string(asset, "completionFlag");
            if (!completionFlags.contains(flag)) issues.add("V5 media " + id
                    + " references unknown completion flag " + flag);
            if ((string(asset, "url").isBlank() && string(asset, "archiveUrl").isBlank())
                    || string(asset, "expectedPayload").isBlank()) {
                issues.add("V5 media " + id + " lacks URL or expected payload");
            }
        }
        if (assets.size() != 5) issues.add("V5 media authority expected 5 assets, found " + assets.size());
        return assets.size();
    }

    private static void validateCasebook(byte[] bytes, Set<String> nodeIds, List<String> issues) {
        if (bytes == null || bytes.length == 0) return;
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!text.contains("The Observance V5") || !text.contains("Director Solution Casebook")) {
            issues.add("V5 solution casebook has no canonical title");
        }
        for (int i = 1; i <= 10; i++) {
            String caseId = String.format(Locale.ROOT, "C%02d", i);
            if (!text.contains(caseId)) issues.add("V5 solution casebook is missing " + caseId);
        }
        for (String nodeId : nodeIds) {
            if (!text.contains(nodeId)) issues.add("V5 solution casebook is missing node " + nodeId);
        }
    }

    private static JsonObject jsonObject(byte[] bytes, String resource, List<String> issues) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                issues.add(resource + " root is not an object");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException failure) {
            issues.add(resource + " is invalid JSON: " + failure.getMessage());
            return null;
        }
    }

    private static void validateStoryVersion(JsonObject root, String resource, List<String> issues) {
        if (!"5.0.0".equals(string(root, "storyVersion"))) {
            issues.add(resource + " storyVersion must be 5.0.0");
        }
    }

    private static JsonArray array(JsonObject root, String key, String resource, List<String> issues) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonArray()) {
            issues.add(resource + " is missing array " + key);
            return null;
        }
        return element.getAsJsonArray();
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return element == null || element.isJsonNull() || !element.isJsonPrimitive()
                ? "" : value(element.getAsString());
    }

    private static List<String> strings(JsonElement element) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            values.add(entry == null || entry.isJsonNull() || !entry.isJsonPrimitive()
                    ? "" : value(entry.getAsString()));
        }
        return values;
    }

    private static List<Map<String, String>> csv(byte[] bytes, String resource, List<String> issues) {
        if (bytes == null || bytes.length == 0) return List.of();
        List<List<String>> records = csvRecords(new String(bytes, StandardCharsets.UTF_8), issues, resource);
        if (records.isEmpty()) {
            issues.add(resource + " has no CSV header");
            return List.of();
        }
        List<String> header = new ArrayList<>(records.get(0));
        if (!header.isEmpty()) header.set(0, header.get(0).replace("\uFEFF", ""));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.size() == 1 && record.get(0).isBlank()) continue;
            if (record.size() != header.size()) {
                issues.add(resource + " CSV row " + (i + 1) + " has " + record.size()
                        + " columns; expected " + header.size());
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) row.put(header.get(c), record.get(c));
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> csvRecords(String text, List<String> issues, String resource) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"' && field.length() == 0) {
                quoted = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (quoted) issues.add(resource + " has an unterminated quoted CSV field");
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    private static Integer integer(String raw, String label, List<String> issues) {
        try {
            return Integer.valueOf(value(raw));
        } catch (NumberFormatException failure) {
            issues.add(label + " is not an integer: " + value(raw));
            return null;
        }
    }

    private static String hashResources(Map<String, byte[]> payloads) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            payloads.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(entry -> {
                        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                        digest.update(entry.getValue());
                        digest.update((byte) '\n');
                    });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String clean(String value) {
        return value(value).toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
