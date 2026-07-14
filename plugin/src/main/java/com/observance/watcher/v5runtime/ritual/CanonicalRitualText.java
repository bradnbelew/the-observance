package com.observance.watcher.v5runtime.ritual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.v5runtime.ConductVerdict;
import com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenTopic;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical text adapter. All player-facing prose comes from packaged npc-dialogue.json or the
 * RP06 predicate authority; this class contains only stable state keys.
 */
public final class CanonicalRitualText {
    public record EndingText(List<String> nameLines, String wrenLine, String conductLine,
                             List<String> universalLines, String codaKey) {
        public EndingText {
            nameLines = nonEmptyLines(nameLines, "nameLines");
            wrenLine = nonBlank(wrenLine, "wrenLine");
            conductLine = nonBlank(conductLine, "conductLine");
            universalLines = nonEmptyLines(universalLines, "universalLines");
            codaKey = nonBlank(codaKey, "codaKey");
        }

        public List<String> completeGoodbye() {
            List<String> result = new ArrayList<>(nameLines);
            result.add(wrenLine);
            result.add(conductLine);
            result.addAll(universalLines);
            return List.copyOf(result);
        }
    }

    private static final Map<WrenTopic, String> TOPIC_STATES = Map.of(
            WrenTopic.BRIDGE_REVISION, "evidence_bridge",
            WrenTopic.NAMES_AND_FEARS, "evidence_names",
            WrenTopic.PRIOR_COMPANY_DISAPPEARANCE, "before_c07");
    private static final String CLOSING_STATE = "confession";

    private final Map<NameTreatment, List<String>> nameClauses;
    private final Map<WrenOutcome, String> wrenClauses;
    private final Map<ConductVerdict, String> conductClauses;
    private final List<String> universalLines;
    private final Map<String, String> codaKeys;

    public CanonicalRitualText(RitualAuthorityContract contract) {
        Objects.requireNonNull(contract, "contract");
        JsonObject predicate = JsonParser.parseString(
                contract.node("RP06").predicate().canonicalJson()).getAsJsonObject();
        Map<String, JsonObject> components = componentsById(predicate.getAsJsonArray("components"));

        nameClauses = new EnumMap<>(NameTreatment.class);
        JsonObject names = requireComponent(components, "name_clauses");
        for (NameTreatment choice : NameTreatment.values()) {
            nameClauses.put(choice, strings(names.getAsJsonArray(choice.wireValue())));
        }

        wrenClauses = new EnumMap<>(WrenOutcome.class);
        JsonObject wrens = requireComponent(components, "wren_clauses");
        for (WrenOutcome choice : WrenOutcome.values()) {
            wrenClauses.put(choice, nonBlank(
                    wrens.get(choice.wireValue()).getAsString(), "wren clause"));
        }

        conductClauses = new EnumMap<>(ConductVerdict.class);
        JsonObject conduct = requireComponent(components, "conduct_clauses");
        for (ConductVerdict verdict : ConductVerdict.values()) {
            String key = verdict.name().toLowerCase(Locale.ROOT);
            conductClauses.put(verdict, nonBlank(conduct.get(key).getAsString(), "conduct clause"));
        }

        universalLines = strings(requireComponent(components, "universal_lines")
                .getAsJsonArray("exact"));
        codaKeys = parseMatrix(requireComponent(components, "ending_matrix")
                .getAsJsonArray("six_combinations"));
        if (codaKeys.size() != 6) {
            throw new IllegalStateException("RP06 must contain exactly six Wren/name endings");
        }
        validateWrenDialogue();
    }

    public String wrenDisplayName() {
        return V5DialogueCatalog.wren().displayName();
    }

    public List<String> wrenTopicReply(WrenTopic topic) {
        String state = TOPIC_STATES.get(Objects.requireNonNull(topic, "topic"));
        return requiredDialogueState(state);
    }

    public List<String> wrenClosingReply() {
        return requiredDialogueState(CLOSING_STATE);
    }

    public EndingText ending(RitualChoices.EndingDimensions dimensions) {
        Objects.requireNonNull(dimensions, "dimensions");
        String matrixKey = matrixKey(dimensions.wrenOutcome(), dimensions.nameTreatment());
        String coda = codaKeys.get(matrixKey);
        if (coda == null) {
            throw new IllegalStateException("authority has no exact ending for " + matrixKey);
        }
        return new EndingText(
                nameClauses.get(dimensions.nameTreatment()),
                wrenClauses.get(dimensions.wrenOutcome()),
                conductClauses.get(dimensions.conductVerdict()),
                universalLines,
                coda);
    }

    private static Map<String, JsonObject> componentsById(JsonArray source) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement element : source) {
            JsonObject component = element.getAsJsonObject();
            if (component.has("id")) {
                String id = component.get("id").getAsString();
                if (result.put(id, component) != null) {
                    throw new IllegalStateException("duplicate RP06 component " + id);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static JsonObject requireComponent(Map<String, JsonObject> components, String id) {
        JsonObject result = components.get(id);
        if (result == null) {
            throw new IllegalStateException("RP06 is missing component " + id);
        }
        return result;
    }

    private static Map<String, String> parseMatrix(JsonArray entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            WrenOutcome wren = WrenOutcome.fromWireValue(entry.get("wren_outcome").getAsString());
            NameTreatment name = NameTreatment.fromWireValue(
                    entry.get("name_treatment").getAsString());
            String key = matrixKey(wren, name);
            String coda = nonBlank(entry.get("coda").getAsString(), "coda");
            if (result.put(key, coda) != null) {
                throw new IllegalStateException("duplicate RP06 matrix entry " + key);
            }
        }
        return Map.copyOf(result);
    }

    private static String matrixKey(WrenOutcome wren, NameTreatment name) {
        return wren.wireValue() + ":" + name.wireValue();
    }

    private static List<String> strings(JsonArray values) {
        if (values == null) {
            throw new IllegalStateException("canonical line array is missing");
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(nonBlank(value.getAsString(), "canonical line")));
        return nonEmptyLines(result, "canonical lines");
    }

    private static List<String> requiredDialogueState(String state) {
        List<String> lines = V5DialogueCatalog.wren().lines(state);
        if (lines.isEmpty()) {
            throw new IllegalStateException("canonical Wren state is missing: " + state);
        }
        return lines;
    }

    private static void validateWrenDialogue() {
        for (String state : TOPIC_STATES.values()) {
            requiredDialogueState(state);
        }
        requiredDialogueState(CLOSING_STATE);
    }

    private static String nonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " cannot be blank");
        }
        return value;
    }

    private static List<String> nonEmptyLines(List<String> values, String label) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(
                value -> value == null || value.isBlank())) {
            throw new IllegalStateException(label + " must contain nonblank text");
        }
        return List.copyOf(values);
    }
}
