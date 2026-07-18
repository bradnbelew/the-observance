package com.observance.watcher.v5runtime.ritual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Fail-closed current authority layered over the byte-pinned historical M2 predicates. */
public final class P12AnySubsetAuthority {
    private static final String RESOURCE = "v5/authority/ARG-P12-ANY-SUBSET-OVERLAY.json";
    private static final String BASE_SHA256 =
            "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a";
    private static final Map<String, String> EFFECTIVE_KINDS = Map.of(
            "RP03", "protected_any_subset_name_choice",
            "RP04", "any_subset_branch_confirmation");

    private P12AnySubsetAuthority() {
    }

    public static void validate(PhysicalPredicateAuthority base) {
        Objects.requireNonNull(base, "base");
        if (!BASE_SHA256.equals(base.sha256())) {
            throw new IllegalStateException("P12 overlay base authority hash drifted");
        }
        JsonObject root = read();
        JsonObject baseReceipt = object(root, "base_physical_authority");
        require(BASE_SHA256.equals(text(baseReceipt, "raw_sha256")),
                "P12 overlay base receipt drifted");
        require(baseReceipt.get("bytes_immutable").getAsBoolean(),
                "P12 overlay must preserve immutable M2 bytes");
        JsonObject rules = object(root, "global_rules");
        require(!rules.get("source_observation_gates_correctness").getAsBoolean(),
                "P12 overlay cannot gate correctness on observations");
        require(!rules.get("nearby_online_or_absent_players_required").getAsBoolean(),
                "P12 overlay cannot require nearby or absent players");
        require(rules.get("one_linked_participant_sufficient").getAsBoolean(),
                "P12 overlay must permit one participant");
        require(!rules.get("timer_required").getAsBoolean(),
                "P12 overlay cannot require a timer");

        Map<String, JsonObject> nodes = new LinkedHashMap<>();
        JsonArray rows = root.getAsJsonArray("nodes");
        require(rows != null, "P12 overlay nodes are missing");
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            String nodeId = text(row, "node_id");
            require(nodes.putIfAbsent(nodeId, row) == null,
                    "duplicate P12 overlay node " + nodeId);
        }
        require(nodes.keySet().equals(Set.of("RP03", "RP04")),
                "P12 overlay must cover exactly RP03 and RP04");
        EFFECTIVE_KINDS.forEach((nodeId, effectiveKind) -> {
            JsonObject row = nodes.get(nodeId);
            require(base.requireNode(nodeId).predicate().kind().equals(text(row, "legacy_kind")),
                    nodeId + " legacy kind does not match M2 authority");
            require(effectiveKind.equals(text(row, "effective_kind")),
                    nodeId + " effective kind drifted");
        });
        JsonObject consequence = object(nodes.get("RP03"), "consequence_book");
        require(!consequence.get("required_open_before_choice").getAsBoolean()
                        && consequence.get("observation_receipt_non_gating").getAsBoolean(),
                "RP03 book observation became a prerequisite");
        JsonObject sectors = object(nodes.get("RP04"), "sector_rule");
        require(sectors.get("required_reachable_lit_sectors").getAsInt() == 1
                        && !sectors.get("time_limit").getAsBoolean(),
                "RP04 must require one untimed reachable sector");
    }

    private static JsonObject read() {
        try (InputStream stream = P12AnySubsetAuthority.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("missing packaged " + RESOURCE);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("invalid packaged P12 any-subset authority", failure);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        require(value != null && value.isJsonObject(), "P12 overlay missing object " + key);
        return value.getAsJsonObject();
    }

    private static String text(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        require(value != null && value.isJsonPrimitive() && !value.getAsString().isBlank(),
                "P12 overlay missing text " + key);
        return value.getAsString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
