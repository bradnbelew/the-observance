package com.observance.watcher.v5runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Applies the current P11 behavior without rewriting the byte-pinned M2 authority. */
public final class P11IdentityAuthority {
    private static final String RESOURCE = "v5/authority/ARG-P11-INDEPENDENT-IDENTITY-OVERLAY.json";
    private static final String BASE_SHA256 =
            "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a";
    private static final Set<String> NODE_IDS =
            Set.of("AR02", "AR03", "AR04", "AR05", "AR06", "AR07");

    private P11IdentityAuthority() {
    }

    public static PhysicalPredicateAuthority apply(PhysicalPredicateAuthority base) {
        Objects.requireNonNull(base, "base");
        require(BASE_SHA256.equals(base.sha256()), "P11 overlay base authority hash drifted");
        JsonObject root = read();
        JsonObject receipt = object(root, "base_physical_authority");
        require(BASE_SHA256.equals(text(receipt, "raw_sha256")), "P11 overlay receipt drifted");
        require(receipt.get("bytes_immutable").getAsBoolean(), "P11 overlay must preserve M2 bytes");
        JsonObject rules = object(root, "global_rules");
        require(rules.get("routes_independent").getAsBoolean()
                        && rules.get("routes_any_order").getAsBoolean(),
                "P11 routes must remain independent and any-order");
        require(!rules.get("source_receipts_gate_identity").getAsBoolean()
                        && rules.get("correct_shared_identity_passes").getAsBoolean(),
                "P11 shared correct identity became observation gated");

        Map<String, JsonObject> replacements = new LinkedHashMap<>();
        for (JsonElement element : array(root, "nodes")) {
            JsonObject row = element.getAsJsonObject();
            String nodeId = text(row, "node_id");
            require(replacements.putIfAbsent(nodeId, row) == null,
                    "duplicate P11 overlay node " + nodeId);
        }
        require(replacements.keySet().equals(NODE_IDS), "P11 overlay scope drifted");

        boolean alreadyEffective = "affidavit_plus_verified_bearing_and_low_sightline".equals(
                base.requireNode("AR05").predicate().kind());
        List<PhysicalPredicateAuthority.Node> effective = new ArrayList<>();
        for (PhysicalPredicateAuthority.Node node : base.nodes()) {
            JsonObject row = replacements.get(node.nodeId());
            if (row == null) {
                effective.add(node);
                continue;
            }
            String expectedKind = text(row, alreadyEffective ? "effective_kind" : "legacy_kind");
            require(expectedKind.equals(node.predicate().kind()),
                    node.nodeId() + " P11 overlay kind drifted");
            List<String> prerequisites = strings(array(row, "effective_prerequisites"));
            require(prerequisites.size() == 2 && prerequisites.contains("v5_ar01_not_kept"),
                    node.nodeId() + " lacks independent archive-context prerequisites");
            PhysicalPredicateAuthority.PredicateDefinition predicate = node.predicate();
            if (row.has("effective_predicate")) {
                JsonObject definition = object(row, "effective_predicate");
                predicate = new PhysicalPredicateAuthority.PredicateDefinition(
                        text(definition, "kind"), text(definition, "evaluation_trigger"),
                        definition.has("accepted")
                                ? strings(definition.getAsJsonArray("accepted")) : List.of(),
                        definition.toString());
            }
            String wrong = row.has("effective_wrong_input")
                    ? object(row, "effective_wrong_input").toString() : node.wrongInputJson();
            String recovery = row.has("effective_reset_repair_recovery")
                    ? object(row, "effective_reset_repair_recovery").toString()
                    : node.resetRepairRecoveryJson();
            effective.add(new PhysicalPredicateAuthority.Node(
                    node.nodeId(), node.owner(), prerequisites, node.siteId(), node.handler(),
                    node.completionFlag(), predicate, wrong, node.rewardJson(), recovery,
                    node.concurrencyReplayJson(), node.durabilityProfile()));
        }
        return new PhysicalPredicateAuthority(
                base.schemaVersion(), base.authorityText(), base.coordinateConvention(),
                base.durabilityProfiles(), base.globalRules(), effective, base.sha256());
    }

    private static JsonObject read() {
        try (InputStream stream = P11IdentityAuthority.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("missing packaged " + RESOURCE);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("invalid packaged P11 identity authority", failure);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        require(value != null && value.isJsonObject(), "P11 overlay missing object " + key);
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        require(value != null && value.isJsonArray(), "P11 overlay missing array " + key);
        return value.getAsJsonArray();
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement value : array) {
            require(value.isJsonPrimitive() && !value.getAsString().isBlank(),
                    "P11 overlay contains blank text");
            result.add(value.getAsString());
        }
        return List.copyOf(result);
    }

    private static String text(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        require(value != null && value.isJsonPrimitive() && !value.getAsString().isBlank(),
                "P11 overlay missing text " + key);
        return value.getAsString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
