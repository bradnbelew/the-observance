package com.observance.watcher.v5runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Loads and validates the packaged V5 physical-predicate authority. */
public final class PhysicalPredicateAuthorityLoader {
    public static final String RESOURCE_PATH = "/v5/authority/ARG-V5-PHYSICAL-PREDICATES.json";
    public static final String EXPECTED_NORMALIZATION_RULE =
            "Answer-sign text is Unicode NFKC, color/control stripped, trimmed, uppercased with "
                    + "Locale.ROOT, punctuation converted to spaces, and internal whitespace collapsed. "
                    + "Only explicit accepted values below pass.";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Pattern NODE_ID = Pattern.compile("[A-Z]{1,2}[0-9]{2}");
    private static final Pattern STATE_KEY = Pattern.compile("v5_[a-z0-9_]+");
    private static final Pattern LOWER_ID = Pattern.compile("[a-z0-9_]+");
    private static final Set<String> ROOT_KEYS = Set.of(
            "schema_version", "authority", "coordinate_convention", "durability_profiles",
            "global_rules", "nodes");
    private static final Set<String> NODE_KEYS = Set.of(
            "node_id", "owner", "prerequisites", "site_id", "handler", "completion_flag",
            "predicate", "wrong_input", "reward", "reset_repair_recovery", "concurrency_replay",
            "durability_profile");
    private static final List<String> RECORDED_BALLOT_FIELDS = List.of(
            "initial_roster_count",
            "maximum_visible_roster_count",
            "first_ballot_eligible_count",
            "first_ballot_cast_count",
            "first_ballot_distinct_choices",
            "first_ballot_tied",
            "resolution_rounds",
            "disconnect_resnap_count");
    private static final List<String> CONDUCT_PRECEDENCE = List.of(
            "SOLO when max(WR05.maximum_visible_roster_count, RP03.maximum_visible_roster_count) == 1",
            "DIVIDED when either first ballot has first_ballot_distinct_choices > 1 or first_ballot_tied=true",
            "UNANIMOUS when both first ballots have cast_count == eligible_count, distinct_choices == 1, and the maximum visible roster is > 1",
            "PERSISTENT otherwise, including an incomplete first ballot followed by a valid disconnect resnapshot or later resolution round");
    private static final List<String> CONDUCT_VALUES =
            List.of("solo", "divided", "unanimous", "persistent");

    private PhysicalPredicateAuthorityLoader() {
    }

    public static PhysicalPredicateAuthority loadDefault() {
        try (InputStream input = PhysicalPredicateAuthorityLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new AuthorityException("Missing packaged V5 authority resource " + RESOURCE_PATH);
            }
            return load(input);
        } catch (IOException exception) {
            throw new AuthorityException("Unable to close packaged V5 authority resource", exception);
        }
    }

    public static PhysicalPredicateAuthority load(Path path) {
        try {
            return loadBytes(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new AuthorityException("Unable to read V5 predicate authority at " + path, exception);
        }
    }

    public static PhysicalPredicateAuthority load(InputStream input) {
        try {
            return loadBytes(input.readAllBytes());
        } catch (IOException exception) {
            throw new AuthorityException("Unable to read V5 predicate authority stream", exception);
        }
    }

    private static PhysicalPredicateAuthority loadBytes(byte[] bytes) {
        final JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new AuthorityException("V5 predicate authority root must be a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new AuthorityException("Malformed V5 predicate authority JSON", exception);
        }

        exactKeys(root, ROOT_KEYS, "root");
        int schemaVersion = integer(root, "schema_version", "root");
        if (schemaVersion != PhysicalPredicateAuthority.REQUIRED_SCHEMA_VERSION) {
            throw new AuthorityException("V5 predicate authority schema must be 1, found " + schemaVersion);
        }

        String authorityText = nonBlankString(root, "authority", "root");
        PhysicalPredicateAuthority.CoordinateConvention coordinate =
                parseCoordinate(object(root, "coordinate_convention", "root"));
        Map<String, PhysicalPredicateAuthority.DurabilityProfile> durabilityProfiles =
                parseDurabilityProfiles(object(root, "durability_profiles", "root"));
        PhysicalPredicateAuthority.GlobalRules globalRules =
                parseGlobalRules(object(root, "global_rules", "root"));
        List<PhysicalPredicateAuthority.Node> nodes =
                parseNodes(array(root, "nodes", "root"), durabilityProfiles);

        PhysicalPredicateAuthority authority = new PhysicalPredicateAuthority(
                schemaVersion,
                authorityText,
                coordinate,
                durabilityProfiles,
                globalRules,
                nodes,
                sha256(bytes));
        PredicateCoverageCatalog.validateAgainst(authority);
        return authority;
    }

    private static PhysicalPredicateAuthority.CoordinateConvention parseCoordinate(JsonObject value) {
        exactKeys(value, Set.of("offset_order", "origin", "front", "rotation"),
                "coordinate_convention");
        List<String> offsetOrder = strings(array(value, "offset_order", "coordinate_convention"),
                "coordinate_convention.offset_order");
        if (!offsetOrder.equals(List.of("right", "up", "front"))) {
            throw new AuthorityException("coordinate_convention.offset_order must be [right, up, front]");
        }
        return new PhysicalPredicateAuthority.CoordinateConvention(
                offsetOrder,
                nonBlankString(value, "origin", "coordinate_convention"),
                nonBlankString(value, "front", "coordinate_convention"),
                nonBlankString(value, "rotation", "coordinate_convention"));
    }

    private static Map<String, PhysicalPredicateAuthority.DurabilityProfile> parseDurabilityProfiles(
            JsonObject value) {
        exactKeys(value, Set.of("minecraft_local_primary", "finale_local_primary"),
                "durability_profiles");
        Map<String, PhysicalPredicateAuthority.DurabilityProfile> result = new LinkedHashMap<>();
        for (String name : List.of("minecraft_local_primary", "finale_local_primary")) {
            JsonObject profile = object(value, name, "durability_profiles");
            exactKeys(profile, Set.of("authoritative", "local_commit", "supabase", "reconcile"),
                    "durability_profiles." + name);
            PhysicalPredicateAuthority.DurabilityProfile parsed =
                    new PhysicalPredicateAuthority.DurabilityProfile(
                            nonBlankString(profile, "authoritative", name),
                            nonBlankString(profile, "local_commit", name),
                            nonBlankString(profile, "supabase", name),
                            nonBlankString(profile, "reconcile", name));
            String expectedAuthority = "minecraft_local_primary".equals(name)
                    ? "local_plugin_data" : "local_finale_state_file";
            if (!expectedAuthority.equals(parsed.authoritative())) {
                throw new AuthorityException(name + " must be authoritative from "
                        + expectedAuthority);
            }
            if (!parsed.localCommit().toLowerCase(java.util.Locale.ROOT).contains("atomic")) {
                throw new AuthorityException(name + " must require an atomic local commit");
            }
            if (!parsed.supabase().toLowerCase(java.util.Locale.ROOT).contains("asynchronous")) {
                throw new AuthorityException(name + " must keep Supabase asynchronous");
            }
            result.put(name, parsed);
        }
        return Map.copyOf(result);
    }

    private static PhysicalPredicateAuthority.GlobalRules parseGlobalRules(JsonObject value) {
        exactKeys(value, Set.of(
                "protection", "item_identity", "atomicity", "wrong_items", "recovery",
                "no_touch_completion", "answer_normalization", "prerequisites", "conduct_verdict"),
                "global_rules");
        String normalization = nonBlankString(value, "answer_normalization", "global_rules");
        if (!EXPECTED_NORMALIZATION_RULE.equals(normalization)) {
            throw new AuthorityException("global_rules.answer_normalization does not match the V5 contract");
        }

        JsonObject conduct = object(value, "conduct_verdict", "global_rules");
        exactKeys(conduct, Set.of(
                "source", "recorded_inputs_per_vote", "first_ballot", "precedence", "totality",
                "persisted_field", "allowed_values"), "global_rules.conduct_verdict");
        List<String> recorded = strings(
                array(conduct, "recorded_inputs_per_vote", "conduct_verdict"),
                "conduct_verdict.recorded_inputs_per_vote");
        List<String> precedence = strings(array(conduct, "precedence", "conduct_verdict"),
                "conduct_verdict.precedence");
        List<String> allowed = strings(array(conduct, "allowed_values", "conduct_verdict"),
                "conduct_verdict.allowed_values");
        if (!recorded.equals(RECORDED_BALLOT_FIELDS)) {
            throw new AuthorityException("conduct_verdict recorded ballot fields drifted: " + recorded);
        }
        if (!precedence.equals(CONDUCT_PRECEDENCE)) {
            throw new AuthorityException("conduct_verdict precedence drifted: " + precedence);
        }
        if (!allowed.equals(CONDUCT_VALUES)) {
            throw new AuthorityException("conduct_verdict allowed values drifted: " + allowed);
        }
        String persistedField = nonBlankString(conduct, "persisted_field", "conduct_verdict");
        if (!"v5_conduct_verdict".equals(persistedField)) {
            throw new AuthorityException("conduct_verdict persisted field must be v5_conduct_verdict");
        }
        PhysicalPredicateAuthority.ConductRules conductRules =
                new PhysicalPredicateAuthority.ConductRules(
                        nonBlankString(conduct, "source", "conduct_verdict"),
                        recorded,
                        nonBlankString(conduct, "first_ballot", "conduct_verdict"),
                        precedence,
                        nonBlankString(conduct, "totality", "conduct_verdict"),
                        persistedField,
                        allowed);

        return new PhysicalPredicateAuthority.GlobalRules(
                nonBlankString(value, "protection", "global_rules"),
                nonBlankString(value, "item_identity", "global_rules"),
                nonBlankString(value, "atomicity", "global_rules"),
                nonBlankString(value, "wrong_items", "global_rules"),
                nonBlankString(value, "recovery", "global_rules"),
                nonBlankString(value, "no_touch_completion", "global_rules"),
                normalization,
                nonBlankString(value, "prerequisites", "global_rules"),
                conductRules);
    }

    private static List<PhysicalPredicateAuthority.Node> parseNodes(
            JsonArray values,
            Map<String, PhysicalPredicateAuthority.DurabilityProfile> durabilityProfiles) {
        if (values.size() != PhysicalPredicateAuthority.REQUIRED_NODE_COUNT) {
            throw new AuthorityException("V5 predicate authority must contain exactly 60 nodes, found "
                    + values.size());
        }
        List<PhysicalPredicateAuthority.Node> nodes = new ArrayList<>(values.size());
        Set<String> ids = new HashSet<>();
        Set<String> completionFlags = new HashSet<>();
        Map<String, Integer> ownerCounts = new HashMap<>();
        for (int index = 0; index < values.size(); index++) {
            JsonElement element = values.get(index);
            if (!element.isJsonObject()) {
                throw new AuthorityException("nodes[" + index + "] must be an object");
            }
            JsonObject node = element.getAsJsonObject();
            String context = "nodes[" + index + "]";
            exactKeys(node, NODE_KEYS, context);
            String nodeId = nonBlankString(node, "node_id", context);
            if (!NODE_ID.matcher(nodeId).matches() || !ids.add(nodeId)) {
                throw new AuthorityException(context + " has invalid or duplicate node_id " + nodeId);
            }
            String owner = nonBlankString(node, "owner", context);
            if (!Set.of("plugin", "plugin_unlit", "plugin_finale").contains(owner)) {
                throw new AuthorityException(nodeId + " has invalid owner " + owner);
            }
            ownerCounts.merge(owner, 1, Integer::sum);

            List<String> prerequisites = strings(array(node, "prerequisites", context),
                    context + ".prerequisites");
            if (prerequisites.isEmpty() || new HashSet<>(prerequisites).size() != prerequisites.size()) {
                throw new AuthorityException(nodeId + " must have non-empty unique prerequisites");
            }
            for (String prerequisite : prerequisites) {
                if (!STATE_KEY.matcher(prerequisite).matches()) {
                    throw new AuthorityException(nodeId + " has invalid prerequisite " + prerequisite);
                }
            }

            String siteId = lowerId(node, "site_id", context);
            String handler = lowerId(node, "handler", context);
            String completionFlag = nonBlankString(node, "completion_flag", context);
            if (!STATE_KEY.matcher(completionFlag).matches() || !completionFlags.add(completionFlag)) {
                throw new AuthorityException(nodeId + " has invalid or duplicate completion flag "
                        + completionFlag);
            }
            String durability = lowerId(node, "durability_profile", context);
            if (!durabilityProfiles.containsKey(durability)) {
                throw new AuthorityException(nodeId + " references unknown durability profile " + durability);
            }
            String expectedDurability = "plugin_finale".equals(owner)
                    ? "finale_local_primary" : "minecraft_local_primary";
            if (!expectedDurability.equals(durability)) {
                throw new AuthorityException(nodeId + " owner " + owner + " requires "
                        + expectedDurability + ", found " + durability);
            }

            JsonObject predicate = object(node, "predicate", context);
            String kind = lowerId(predicate, "kind", context + ".predicate");
            String trigger = nonBlankString(predicate, "evaluation_trigger", context + ".predicate");
            JsonArray allOf = array(predicate, "all_of", context + ".predicate");
            if (allOf.isEmpty()) {
                throw new AuthorityException(nodeId + " predicate.all_of cannot be empty");
            }
            for (int conditionIndex = 0; conditionIndex < allOf.size(); conditionIndex++) {
                JsonElement condition = allOf.get(conditionIndex);
                if (!condition.isJsonObject()) {
                    throw new AuthorityException(nodeId + " predicate.all_of[" + conditionIndex
                            + "] must be an object");
                }
                nonBlankString(condition.getAsJsonObject(), "op",
                        context + ".predicate.all_of[" + conditionIndex + "]");
            }
            nonBlankString(predicate, "commit", context + ".predicate");
            List<String> accepted = predicate.has("accepted")
                    ? strings(array(predicate, "accepted", context + ".predicate"),
                            context + ".predicate.accepted")
                    : List.of();
            if (predicate.has("normalization") && accepted.isEmpty()) {
                throw new AuthorityException(nodeId + " declares normalization without accepted answers");
            }
            if (predicate.has("normalization")
                    && !"global answer_normalization".equals(
                            nonBlankString(predicate, "normalization", context + ".predicate"))) {
                throw new AuthorityException(nodeId + " must reference global answer_normalization");
            }
            Set<String> normalizedAnswers = new HashSet<>();
            for (String answer : accepted) {
                String normalized = AnswerNormalizer.normalize(answer);
                if (!answer.equals(normalized) || !normalizedAnswers.add(normalized)) {
                    throw new AuthorityException(nodeId + " has non-normalized or duplicate accepted answer: "
                            + answer);
                }
            }

            JsonObject wrong = object(node, "wrong_input", context);
            exactObjectWithStrings(wrong, Set.of("policy", "feedback", "state_effect"),
                    context + ".wrong_input");
            JsonObject reward = object(node, "reward", context);
            exactKeys(reward, Set.of("description", "artifact_ids", "delivery"), context + ".reward");
            nonBlankString(reward, "description", context + ".reward");
            List<String> artifactIds = strings(array(reward, "artifact_ids", context + ".reward"),
                    context + ".reward.artifact_ids");
            if (new HashSet<>(artifactIds).size() != artifactIds.size()) {
                throw new AuthorityException(nodeId + " reward artifact_ids cannot contain duplicates");
            }
            nonBlankString(reward, "delivery", context + ".reward");
            JsonObject recovery = object(node, "reset_repair_recovery", context);
            exactObjectWithStrings(recovery, Set.of("reset", "repair", "recovery"),
                    context + ".reset_repair_recovery");
            JsonObject concurrency = object(node, "concurrency_replay", context);
            exactObjectWithStrings(concurrency,
                    Set.of("scope", "lock", "replay_policy", "disconnect", "after_complete"),
                    context + ".concurrency_replay");

            nodes.add(new PhysicalPredicateAuthority.Node(
                    nodeId,
                    owner,
                    prerequisites,
                    siteId,
                    handler,
                    completionFlag,
                    new PhysicalPredicateAuthority.PredicateDefinition(
                            kind, trigger, accepted, GSON.toJson(predicate)),
                    GSON.toJson(wrong),
                    GSON.toJson(reward),
                    GSON.toJson(recovery),
                    GSON.toJson(concurrency),
                    durability));
        }

        Map<String, Integer> expectedOwners = Map.of(
                "plugin", 51, "plugin_unlit", 7, "plugin_finale", 2);
        if (!ownerCounts.equals(expectedOwners)) {
            throw new AuthorityException("V5 predicate owner counts drifted: " + ownerCounts);
        }
        return List.copyOf(nodes);
    }

    private static void exactObjectWithStrings(JsonObject object, Set<String> keys, String context) {
        exactKeys(object, keys, context);
        for (String key : keys) {
            nonBlankString(object, key, context);
        }
    }

    private static JsonObject object(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new AuthorityException(context + "." + key + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new AuthorityException(context + "." + key + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static String lowerId(JsonObject parent, String key, String context) {
        String value = nonBlankString(parent, key, context);
        if (!LOWER_ID.matcher(value).matches()) {
            throw new AuthorityException(context + "." + key + " must be a lower-snake identifier");
        }
        return value;
    }

    private static String nonBlankString(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new AuthorityException(context + "." + key + " must be a string");
        }
        String text = value.getAsString();
        if (text.isBlank()) {
            throw new AuthorityException(context + "." + key + " cannot be blank");
        }
        return text;
    }

    private static int integer(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new AuthorityException(context + "." + key + " must be an integer");
        }
        try {
            int result = value.getAsInt();
            if (value.getAsDouble() != result) {
                throw new NumberFormatException("not integral");
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new AuthorityException(context + "." + key + " must be an exact integer", exception);
        }
    }

    private static List<String> strings(JsonArray values, String context) {
        List<String> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            JsonElement value = values.get(index);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || value.getAsString().isBlank()) {
                throw new AuthorityException(context + "[" + index + "] must be a non-blank string");
            }
            result.add(value.getAsString());
        }
        return List.copyOf(result);
    }

    private static void exactKeys(JsonObject object, Set<String> expected, String context) {
        Set<String> actual = object.keySet();
        if (!actual.equals(expected)) {
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new HashSet<>(actual);
            unknown.removeAll(expected);
            throw new AuthorityException(context + " keys mismatch; missing=" + missing
                    + ", unknown=" + unknown);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("JVM has no SHA-256 provider", exception);
        }
    }
}
