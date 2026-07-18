package com.observance.watcher.campaign;

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
import java.util.HexFormat;
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
        byte[] bytes = read(issues);
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
        JsonArray cases = array(root, "cases", issues);
        Set<String> phases = new LinkedHashSet<>();
        Set<String> caseIds = new LinkedHashSet<>();
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
                evidence += validateEvidence(optionalArray(dossier, "evidence"), phase, issues);
            }
            JsonArray authoredSpaces = optionalArray(authored, "spaces");
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
        return new Report(cases.size(), conclusions, evidence, spaces, sha256(bytes), issues);
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

    private static byte[] read(List<String> issues) {
        try (InputStream stream = AuthoredCampaignAuthority.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                issues.add("missing packaged campaign authority " + RESOURCE);
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
