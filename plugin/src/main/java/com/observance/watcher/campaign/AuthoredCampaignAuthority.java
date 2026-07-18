package com.observance.watcher.campaign;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.structure.V5AuthorityManifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fail-closed executable contract for the projected P5-P12 campaign authority.
 *
 * <p>This is deliberately mechanism-agnostic. It validates evidence, conclusion, event, theory,
 * projection, accessibility, and durability contracts without turning authored investigations
 * into a closed enum of puzzle types. Observation receipts remain useful provenance, but can
 * never become an answer predicate.
 */
public final class AuthoredCampaignAuthority {

    public static final String RESOURCE = "campaign/p5-p12.json";
    public static final String MINECRAFT_BINDING_RESOURCE =
            "campaign/p5-p12-minecraft-bindings.json";
    public static final String EXTERNAL_BINDING_RESOURCE =
            "campaign/p5-p12-external-bindings.json";
    private static final List<String> PHASES =
            List.of("P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12");

    public record Report(int caseCount, int conclusionCount, int evidenceCount,
                         int spaceCount, String contentHash, List<String> issues) {
        public Report {
            issues = List.copyOf(issues);
        }

        public boolean valid() {
            return issues.isEmpty();
        }
    }

    private AuthoredCampaignAuthority() { }

    public static Report inspect() {
        List<String> issues = new ArrayList<>();
        byte[] bytes = readResource(RESOURCE, issues);
        if (bytes.length == 0) return new Report(0, 0, 0, 0, "", issues);

        JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException failure) {
            issues.add("campaign authority is not valid JSON: " + failure.getMessage());
            return new Report(0, 0, 0, 0, sha256(bytes), issues);
        }

        requireFalse(root, "closed_mechanism_taxonomy", issues);
        requireFalse(root, "observation_receipts_gate_answers", issues);
        if (!"offline_redesigned_not_human_approved".equals(
                root.has("experiential_status") ? root.get("experiential_status").getAsString() : "")) {
            issues.add("campaign experiential status must remain offline/not human-approved");
        }
        if (!root.has("brad_approval") || !root.get("brad_approval").isJsonNull()) {
            issues.add("campaign must retain brad_approval=null");
        }
        JsonObject choreography = object(root, "arg_state_choreography", "campaign", issues);
        String choreographyStatus = choreography.has("status")
                ? choreography.get("status").getAsString() : "";
        Set<String> allowedChoreographyStatuses = Set.of(
                "offline_authored_not_deployed",
                "local_file_runtime_and_deployable_supabase_discord_adapter_implemented_external_staging_unverified");
        if (!allowedChoreographyStatuses.contains(choreographyStatus)) {
            issues.add("ARG state choreography status is unsupported or overclaims deployment");
        }
        if (!"offline_authored_not_deployed".equals(choreographyStatus)) {
            JsonObject projectionRuntime = object(
                    choreography, "projection_runtime", "campaign choreography", issues);
            requireTrue(projectionRuntime, "canonical_commit_before_projection",
                    "campaign choreography", issues);
            requireText(projectionRuntime, "unknown_event_behavior", "campaign choreography", issues);
            requireText(projectionRuntime, "proof", "campaign choreography", issues);
            if (!projectionRuntime.has("external_staging_receipt")
                    || !projectionRuntime.get("external_staging_receipt").isJsonNull()) {
                issues.add("implemented projection runtime must retain external_staging_receipt=null");
            }
            JsonObject timing = object(
                    projectionRuntime, "worker_timing", "campaign choreography", issues);
            if (!timing.has("maximum_attempts")
                    || timing.get("maximum_attempts").getAsInt() != 8) {
                issues.add("implemented projection runtime must retain the eight-attempt ceiling");
            }
        }
        JsonArray cases = array(root, "cases", issues);
        Set<String> phases = new LinkedHashSet<>();
        Set<String> caseIds = new LinkedHashSet<>();
        Set<String> authoredSpaceIds = new LinkedHashSet<>();
        java.util.Map<String, String> evidenceSurfaces = new LinkedHashMap<>();
        int conclusions = 0;
        int evidence = 0;
        int spaces = 0;

        for (JsonElement element : cases) {
            if (!element.isJsonObject()) {
                issues.add("campaign case entry is not an object");
                continue;
            }
            JsonObject authored = element.getAsJsonObject();
            String phase = text(authored, "phase", issues);
            String caseId = text(authored, "case_id", issues);
            if (!phases.add(phase)) issues.add("duplicate phase " + phase);
            if (!caseIds.add(caseId)) issues.add("duplicate case_id " + caseId);
            requireText(authored, "player_facing_question", phase, issues);
            requireText(authored, "plausible_initial_belief", phase, issues);
            requireText(authored, "human_stake", phase, issues);
            requireText(authored, "earned_belief", phase, issues);
            requireText(authored, "required_inference", phase, issues);
            requireText(authored, "callback", phase, issues);
            requireText(authored, "novelty_comparison", phase, issues);

            JsonObject experience = object(authored, "arg_experience", phase, issues);
            if (!"ARG".equals(experience.has("experience_classification")
                    ? experience.get("experience_classification").getAsString() : "")) {
                issues.add(phase + " campaign-level experience is not ARG");
            }
            for (String key : List.of("inciting_anomaly", "live_unknown",
                    "provenance_authentication", "collaborative_asymmetric_paths",
                    "cross_surface_consequence", "delayed_callback_reinterpretation",
                    "earned_final_belief", "answer_input_role", "novelty_against_adjacent")) {
                requireText(experience, key, phase, issues);
            }
            for (String key : List.of("competing_hypotheses", "distributed_fragments",
                    "player_initiated_actions", "authored_reactivity")) {
                if (array(experience, key, phase, issues).isEmpty()) {
                    issues.add(phase + " ARG experience has no " + key);
                }
            }
            requireFalse(experience, "direct_source_restatement_core", issues);
            requireFalse(experience, "single_surface_bounded_case", issues);
            requireFalse(experience, "conclusion_printed_verbatim", issues);
            requireFalse(experience, "interaction_free", issues);
            requireTrue(experience, "player_caused_world_response", phase, issues);
            for (JsonElement actionElement : optionalArray(experience, "player_initiated_actions")) {
                if (!actionElement.isJsonObject()) {
                    issues.add(phase + " ARG action is not an object");
                    continue;
                }
                JsonObject action = actionElement.getAsJsonObject();
                requireText(action, "verb", phase, issues);
                requireText(action, "action", phase, issues);
                requireText(action, "response_event", phase, issues);
                requireFalse(action, "receipt_gate", issues);
            }
            for (JsonElement reactionElement : optionalArray(experience, "authored_reactivity")) {
                if (!reactionElement.isJsonObject()) {
                    issues.add(phase + " authored reaction is not an object");
                    continue;
                }
                JsonObject reaction = reactionElement.getAsJsonObject();
                requireText(reaction, "event", phase, issues);
                requireText(reaction, "exact_trigger", phase, issues);
                requireText(reaction, "response", phase, issues);
                requireText(reaction, "catch_up", phase, issues);
                requireTrue(reaction, "idempotent", phase, issues);
                String automation = text(reaction, "automation", issues);
                if (!Set.of("A0", "A1").contains(automation)) {
                    issues.add(phase + " authored reaction exceeds A0/A1: " + automation);
                }
            }

            JsonObject runtime = object(authored, "runtime", phase, issues);
            requireFalse(runtime, "answer_requires_observation_receipts", issues);
            requireTrue(runtime, "any_subset", phase, issues);
            requireTrue(runtime, "local_primary", phase, issues);
            requireText(runtime, "restart", phase, issues);
            requireText(runtime, "replay", phase, issues);
            requireText(runtime, "outage", phase, issues);

            JsonObject hints = object(authored, "hints", phase, issues);
            for (String tier : List.of("H0", "H1", "H2", "H3")) {
                requireText(hints, tier, phase, issues);
            }
            JsonArray wrong = array(authored, "meaningful_wrong_theories", phase, issues);
            if (wrong.isEmpty()) issues.add(phase + " has no meaningful wrong theories");
            for (JsonElement theoryElement : wrong) {
                if (!theoryElement.isJsonObject()) {
                    issues.add(phase + " wrong theory is not an object");
                    continue;
                }
                JsonObject theory = theoryElement.getAsJsonObject();
                requireText(theory, "theory", phase, issues);
                requireText(theory, "response", phase, issues);
            }

            JsonArray directEvidence = optionalArray(authored, "evidence");
            collectEvidenceSurfaces(directEvidence, evidenceSurfaces, phase, issues);
            evidence += validateEvidence(directEvidence, phase, issues);
            JsonArray directConclusions = optionalArray(authored, "conclusions");
            conclusions += validateConclusions(directConclusions, phase, issues);
            if (authored.has("group_conclusion")) {
                conclusions += validateConclusion(authored.get("group_conclusion"), phase, issues);
            }
            JsonArray dossiers = optionalArray(authored, "dossiers");
            for (JsonElement dossierElement : dossiers) {
                if (!dossierElement.isJsonObject()) {
                    issues.add(phase + " dossier is not an object");
                    continue;
                }
                JsonObject dossier = dossierElement.getAsJsonObject();
                requireText(dossier, "operation", phase, issues);
                requireText(dossier, "finding", phase, issues);
                JsonArray dossierEvidence = optionalArray(dossier, "evidence");
                collectEvidenceSurfaces(dossierEvidence, evidenceSurfaces, phase, issues);
                evidence += validateEvidence(dossierEvidence, phase, issues);
            }
            JsonArray authoredSpaces = optionalArray(authored, "spaces");
            for (JsonElement spaceElement : authoredSpaces) {
                if (spaceElement.isJsonObject() && spaceElement.getAsJsonObject().has("id")) {
                    JsonObject space = spaceElement.getAsJsonObject();
                    String spaceId = space.get("id").getAsString();
                    if (!authoredSpaceIds.add(spaceId)) issues.add("duplicate authored space " + spaceId);
                    for (JsonElement evidenceId : optionalArray(space, "evidence_surfaces")) {
                        if (!evidenceId.isJsonPrimitive()
                                || !evidenceSurfaces.containsKey(evidenceId.getAsString())) {
                            issues.add(spaceId + " references unknown evidence surface " + evidenceId);
                        }
                    }
                }
            }
            spaces += validateSpaces(authoredSpaces, phase, issues);
            if (authoredSpaces.isEmpty()) issues.add(phase + " has no functional Minecraft spaces");
            if (directConclusions.isEmpty() && !authored.has("group_conclusion")) {
                issues.add(phase + " has no answer contract");
            }
        }

        if (!new ArrayList<>(phases).equals(PHASES)) {
            issues.add("campaign phases must be exact ordered P5-P12, found " + phases);
        }
        if (cases.size() != PHASES.size()) issues.add("expected 8 cases, found " + cases.size());
        validateMinecraftBindings(authoredSpaceIds, evidenceSurfaces, issues);
        validateExternalBindings(evidenceSurfaces, issues);
        return new Report(cases.size(), conclusions, evidence, spaces, sha256(bytes), issues);
    }

    private static void validateExternalBindings(java.util.Map<String, String> evidenceSurfaces,
                                                 List<String> issues) {
        byte[] bytes = readResource(EXTERNAL_BINDING_RESOURCE, issues);
        if (bytes.length == 0) return;
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException failure) {
            issues.add("external binding authority is not valid JSON: " + failure.getMessage());
            return;
        }
        JsonElement receipt = root.get("fresh_external_client_receipt");
        if (receipt == null || !receipt.isJsonPrimitive() || receipt.getAsBoolean()) {
            issues.add("offline external binding must record fresh_external_client_receipt=false");
        }
        Set<String> expected = new LinkedHashSet<>();
        evidenceSurfaces.forEach((id, surface) -> {
            String value = surface.toLowerCase(java.util.Locale.ROOT);
            if (List.of("copperline", "discord", "media", "npc", "archive", "web", "image")
                    .stream().anyMatch(value::contains)) expected.add(id);
        });
        Set<String> bound = new LinkedHashSet<>();
        for (JsonElement element : array(root, "bindings", "external evidence", issues)) {
            if (!element.isJsonObject()) {
                issues.add("external evidence binding is not an object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String evidenceId = text(row, "evidence_id", issues);
            if (!bound.add(evidenceId)) issues.add("duplicate external evidence binding " + evidenceId);
            if (!expected.contains(evidenceId)) issues.add("local-only or unknown external binding " + evidenceId);
            requireText(row, "delivery_status", evidenceId, issues);
            JsonArray carriers = array(row, "carriers", evidenceId, issues);
            if (carriers.isEmpty()) issues.add(evidenceId + " has no concrete external carrier");
            for (JsonElement carrierElement : carriers) {
                if (!carrierElement.isJsonPrimitive()) {
                    issues.add(evidenceId + " external carrier is not text");
                    continue;
                }
                String carrier = carrierElement.getAsString();
                if (!(carrier.startsWith("web:") || carrier.startsWith("discord:")
                        || carrier.startsWith("media:") || carrier.startsWith("npc:"))) {
                    issues.add(evidenceId + " has unsupported external carrier " + carrier);
                }
            }
        }
        if (!bound.equals(expected)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(bound);
            Set<String> extra = new LinkedHashSet<>(bound);
            extra.removeAll(expected);
            issues.add("external evidence carrier coverage differs: missing=" + missing + " extra=" + extra);
        }
    }

    private static void validateMinecraftBindings(Set<String> authoredSpaceIds,
                                                   java.util.Map<String, String> evidenceSurfaces,
                                                   List<String> issues) {
        byte[] bytes = readResource(MINECRAFT_BINDING_RESOURCE, issues);
        if (bytes.length == 0) return;
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException failure) {
            issues.add("Minecraft binding authority is not valid JSON: " + failure.getMessage());
            return;
        }
        JsonElement receipt = root.get("fresh_client_receipt");
        if (receipt == null || !receipt.isJsonPrimitive() || receipt.getAsBoolean()) {
            issues.add("offline Minecraft binding must record fresh_client_receipt=false");
        }
        Set<String> bound = new LinkedHashSet<>();
        Set<String> fixtureIds = new LinkedHashSet<>();
        for (JsonElement element : array(root, "bindings", "Minecraft binding", issues)) {
            if (!element.isJsonObject()) {
                issues.add("Minecraft binding entry is not an object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String spaceId = text(row, "space_id", issues);
            if (!bound.add(spaceId)) issues.add("duplicate Minecraft binding " + spaceId);
            if (array(row, "room_ids", spaceId, issues).isEmpty()) {
                issues.add(spaceId + " has no exact room binding");
            }
            if (array(row, "fixture_ids", spaceId, issues).isEmpty()) {
                issues.add(spaceId + " has no exact fixture binding");
            }
            for (JsonElement fixture : optionalArray(row, "fixture_ids")) {
                if (fixture.isJsonPrimitive()) fixtureIds.add(fixture.getAsString());
            }
            requireText(row, "placement_mode", spaceId, issues);
        }
        if (!bound.equals(authoredSpaceIds)) {
            issues.add("Minecraft binding coverage differs from authored spaces");
        }
        validateEvidenceBindings(root, evidenceSurfaces, fixtureIds, issues);
    }

    private static void validateEvidenceBindings(JsonObject root,
                                                  java.util.Map<String, String> evidenceSurfaces,
                                                  Set<String> fixtureIds,
                                                  List<String> issues) {
        Set<String> expected = new LinkedHashSet<>();
        evidenceSurfaces.forEach((id, surface) -> {
            String value = surface.toLowerCase(java.util.Locale.ROOT);
            if (value.contains("minecraft") || value.contains("npc_dialogue")
                    || value.contains("npc_memory")) expected.add(id);
        });
        Set<String> bound = new LinkedHashSet<>();
        for (JsonElement element : array(root, "evidence_bindings", "Minecraft evidence", issues)) {
            if (!element.isJsonObject()) {
                issues.add("Minecraft evidence binding is not an object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String evidenceId = text(row, "evidence_id", issues);
            if (!bound.add(evidenceId)) issues.add("duplicate Minecraft evidence binding " + evidenceId);
            if (!expected.contains(evidenceId)) issues.add("non-local or unknown Minecraft evidence binding " + evidenceId);
            JsonArray carriers = array(row, "carriers", evidenceId, issues);
            if (carriers.isEmpty()) issues.add(evidenceId + " has no concrete runtime carrier");
            boolean concrete = false;
            for (JsonElement carrierElement : carriers) {
                if (!carrierElement.isJsonPrimitive()) {
                    issues.add(evidenceId + " carrier is not text");
                    continue;
                }
                String carrier = carrierElement.getAsString();
                if (carrier.startsWith("book:")) {
                    concrete = true;
                    String bookId = carrier.substring("book:".length());
                    if (V5AuthorityManifest.book(bookId) == null) {
                        issues.add(evidenceId + " references missing authored book " + bookId);
                    }
                } else if (carrier.startsWith("fixture:")) {
                    concrete = true;
                    String fixtureId = carrier.substring("fixture:".length());
                    if (!fixtureIds.contains(fixtureId)) {
                        issues.add(evidenceId + " references fixture outside campaign bindings " + fixtureId);
                    }
                } else if (carrier.equals("npc:wren") || carrier.startsWith("runtime:")) {
                    concrete = true;
                } else {
                    issues.add(evidenceId + " has unsupported carrier " + carrier);
                }
            }
            if (!concrete) issues.add(evidenceId + " exists only as campaign JSON");
        }
        if (!bound.equals(expected)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(bound);
            Set<String> extra = new LinkedHashSet<>(bound);
            extra.removeAll(expected);
            issues.add("Minecraft evidence carrier coverage differs: missing=" + missing + " extra=" + extra);
        }
    }

    private static void collectEvidenceSurfaces(JsonArray rows,
                                                java.util.Map<String, String> surfaces,
                                                String phase, List<String> issues) {
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            if (!row.has("id") || !row.has("surface")) continue;
            String id = row.get("id").getAsString();
            String previous = surfaces.putIfAbsent(id, row.get("surface").getAsString());
            if (previous != null) issues.add(phase + " duplicate evidence id " + id);
        }
    }

    private static int validateEvidence(JsonArray rows, String phase, List<String> issues) {
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) {
                issues.add(phase + " evidence is not an object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            for (String key : List.of("id", "medium", "surface", "provenance", "content")) {
                requireText(row, key, phase, issues);
            }
            requireTrue(row, "observation_receipt_non_gating", phase, issues);
            JsonArray roles = array(row, "roles", phase, issues);
            if (roles.isEmpty()) issues.add(phase + " evidence has no dramatic role");
        }
        return rows.size();
    }

    private static int validateConclusions(JsonArray rows, String phase, List<String> issues) {
        int count = 0;
        for (JsonElement element : rows) count += validateConclusion(element, phase, issues);
        return count;
    }

    private static int validateConclusion(JsonElement element, String phase, List<String> issues) {
        if (element == null || !element.isJsonObject()) {
            issues.add(phase + " conclusion is not an object");
            return 0;
        }
        JsonObject row = element.getAsJsonObject();
        requireText(row, "id", phase, issues);
        requireText(row, "prompt", phase, issues);
        requireTrue(row, "zero_observation_acceptance", phase, issues);
        if (array(row, "accepted_answers", phase, issues).isEmpty()) {
            issues.add(phase + " conclusion has no accepted answer");
        }
        if (array(row, "required_concepts", phase, issues).isEmpty()) {
            issues.add(phase + " conclusion has no meaning contract");
        }
        JsonObject input = object(row, "input_contract", phase, issues);
        for (String key : List.of("id", "platform", "visible_trigger", "input_shape",
                "acceptance_owner", "event", "implementation_status")) {
            requireText(input, key, phase, issues);
        }
        String conclusionId = row.has("id") ? row.get("id").getAsString() : "";
        if (input.has("id") && !conclusionId.equals(input.get("id").getAsString())) {
            issues.add(phase + " input contract ID differs from conclusion " + conclusionId);
        }
        requireTrue(input, "zero_observation_acceptance", phase, issues);
        boolean exact = input.has("runtime_exact_phrase")
                && input.get("runtime_exact_phrase").isJsonPrimitive()
                && input.get("runtime_exact_phrase").getAsBoolean();
        boolean interpretive = input.has("interpretive")
                && input.get("interpretive").isJsonPrimitive()
                && input.get("interpretive").getAsBoolean();
        if (exact && interpretive) {
            issues.add(phase + " interpretive conclusion cannot require exact prose: " + conclusionId);
        }
        if (!exact) {
            requireFalse(input, "runtime_exact_phrase", issues);
            requireTrue(input, "accepted_answers_are_human_examples", phase, issues);
        }
        return 1;
    }

    private static int validateSpaces(JsonArray rows, String phase, List<String> issues) {
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) {
                issues.add(phase + " space is not an object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            for (String key : List.of("id", "job", "composition", "navigation", "affordance")) {
                requireText(row, key, phase, issues);
            }
        }
        return rows.size();
    }

    private static JsonObject object(JsonObject parent, String key, String phase,
                                     List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonObject()) {
            issues.add(phase + " missing object " + key);
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String key, List<String> issues) {
        return array(parent, key, "campaign", issues);
    }

    private static JsonArray array(JsonObject parent, String key, String phase,
                                   List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonArray()) {
            issues.add(phase + " missing array " + key);
            return new JsonArray();
        }
        return element.getAsJsonArray();
    }

    private static JsonArray optionalArray(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static String text(JsonObject parent, String key, List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive()
                || element.getAsString().trim().isEmpty()) {
            issues.add("campaign missing text " + key);
            return "";
        }
        return element.getAsString().trim();
    }

    private static void requireText(JsonObject parent, String key, String phase,
                                    List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive()
                || element.getAsString().trim().isEmpty()) {
            issues.add(phase + " missing text " + key);
        }
    }

    private static void requireTrue(JsonObject parent, String key, String phase,
                                    List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsBoolean()) {
            issues.add(phase + " requires " + key + "=true");
        }
    }

    private static void requireFalse(JsonObject parent, String key, List<String> issues) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive() || element.getAsBoolean()) {
            issues.add("campaign requires " + key + "=false");
        }
    }

    private static byte[] readResource(String resource, List<String> issues) {
        try (InputStream stream = AuthoredCampaignAuthority.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (stream == null) {
                issues.add("missing packaged campaign authority " + resource);
                return new byte[0];
            }
            return stream.readAllBytes();
        } catch (IOException failure) {
            issues.add("cannot read packaged campaign authority: " + failure.getMessage());
            return new byte[0];
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
