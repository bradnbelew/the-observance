package com.observance.watcher.v5runtime.container;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.ImplementationFamily;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PredicateCoverageCatalog;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.ItemDisposition;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Parsed executable contract for family C. Construction validates the authored operations and every
 * load-bearing count/slot/PDC field before the listener can be registered.
 */
public final class ContainerAuthorityContract {
    public enum TriggerKind { HANDLE, SOURCE_CLAIM, PORTABLE_CLAIM }

    public record ItemRequirement(String material, Map<String, String> pdc) {
        public ItemRequirement {
            material = requireText(material, "material").toUpperCase(Locale.ROOT);
            pdc = Map.copyOf(Objects.requireNonNull(pdc, "pdc"));
        }

        public boolean matches(ContainerItem item) {
            return item != null && item.matches(material, 1, pdc)
                    && item.hasUniqueArtifactIdentity();
        }

        public String identityId() {
            String artifact = pdc.get(ContainerItem.ARTIFACT_ID);
            if (artifact != null) {
                return artifact;
            }
            String evidence = pdc.get(ContainerItem.EVIDENCE_ID);
            if (evidence != null) {
                return evidence;
            }
            throw new IllegalStateException("required item has no durable identity");
        }

        public String identityType() {
            return pdc.containsKey(ContainerItem.ARTIFACT_ID) ? "artifact" : "evidence";
        }
    }

    public record SlotRef(String component, int slot, ItemRequirement requirement) {
        public SlotRef {
            component = requireText(component, "component");
            if (slot < 0) {
                throw new IllegalArgumentException("slot cannot be negative");
            }
            Objects.requireNonNull(requirement, "requirement");
        }
    }

    public record NodeRule(
            PhysicalPredicateAuthority.Node authorityNode,
            TriggerKind solveTrigger,
            Set<String> triggerComponents,
            Map<String, Map<Integer, ItemRequirement>> exactSlots,
            Map<String, ItemRequirement> acceptedIdentities,
            Set<String> stagingComponents,
            Set<String> immutableSourceComponents,
            Map<String, Set<Integer>> classSlots,
            Map<String, Map<String, Integer>> classMaterialCounts,
            Map<String, String> classValues,
            Map<String, Map<Integer, ItemRequirement>> portableClaims,
            String wrongFeedback) {
        public NodeRule {
            Objects.requireNonNull(authorityNode, "authorityNode");
            Objects.requireNonNull(solveTrigger, "solveTrigger");
            triggerComponents = Set.copyOf(triggerComponents);
            exactSlots = deepCopy(exactSlots);
            acceptedIdentities = Map.copyOf(acceptedIdentities);
            stagingComponents = Set.copyOf(stagingComponents);
            immutableSourceComponents = Set.copyOf(immutableSourceComponents);
            Map<String, Set<Integer>> copiedClassSlots = new HashMap<>();
            classSlots.forEach((key, value) -> copiedClassSlots.put(key, Set.copyOf(value)));
            classSlots = Map.copyOf(copiedClassSlots);
            Map<String, Map<String, Integer>> copiedCounts = new HashMap<>();
            classMaterialCounts.forEach((key, value) -> copiedCounts.put(key, Map.copyOf(value)));
            classMaterialCounts = Map.copyOf(copiedCounts);
            classValues = Map.copyOf(classValues);
            portableClaims = deepCopy(portableClaims);
            wrongFeedback = requireText(wrongFeedback, "wrongFeedback");
        }

        public String nodeId() {
            return authorityNode.nodeId();
        }

        public String completionFlag() {
            return authorityNode.completionFlag();
        }

        public String siteId() {
            return authorityNode.siteId();
        }

        private static Map<String, Map<Integer, ItemRequirement>> deepCopy(
                Map<String, Map<Integer, ItemRequirement>> source) {
            Map<String, Map<Integer, ItemRequirement>> copied = new HashMap<>();
            source.forEach((key, value) -> copied.put(key, Map.copyOf(value)));
            return Map.copyOf(copied);
        }
    }

    private static final Map<String, List<String>> EXPECTED_OPERATIONS = Map.ofEntries(
            Map.entry("LS06", List.of("slot_exact_item",
                    "unique_item_absent_from_source_after_claim")),
            Map.entry("A03", List.of("slots_contain_exact_evidence_ids",
                    "all_manifest_items_present_once")),
            Map.entry("A09", List.of("prior_receipt_true", "slot_exact_item",
                    "destination_inventory_has_capacity")),
            Map.entry("RP02", List.of("named_slots_match_exact_affidavit_pdc", "slot_empty",
                    "slot_exact_item", "slot_exact_item", "slot_exact_item")),
            Map.entry("CW02", List.of("all_manifest_items_present_once",
                    "every_sample_matches_container_intake_depth_and_slot_cycle",
                    "disputed_pair_present")),
            Map.entry("CW07", List.of("same_linked_player_held_each_unique_item",
                    "slots_contain_exact_evidence_ids")),
            Map.entry("BI02", List.of("slots_contain_exact_evidence_ids",
                    "all_manifest_items_present_once")),
            Map.entry("BI03", List.of("slots_contain_exact_evidence_ids",
                    "all_manifest_items_present_once", "frame_rotation_exact")),
            Map.entry("BI06", List.of("slots_contain_exact_evidence_ids", "every_item_pdc_equals")),
            Map.entry("HS01", List.of("slot_exact_item", "destination_inventory_has_capacity")),
            Map.entry("HS02", List.of("slot_exact_item")),
            Map.entry("HS07", List.of("flags_all_true", "slot_exact_item")),
            Map.entry("KV02", List.of("all_manifest_items_present_once",
                    "every_item_in_matching_pdc_container", "container_material_counts_exact")));

    private static final Map<String, String> EXPECTED_HANDLERS = Map.ofEntries(
            Map.entry("LS06", "tagged_deposit"), Map.entry("A03", "container_order"),
            Map.entry("A09", "tagged_locker"), Map.entry("RP02", "tagged_release_configuration"),
            Map.entry("CW02", "tagged_barrel_sort"), Map.entry("CW07", "tagged_cache"),
            Map.entry("BI02", "tagged_fragments"), Map.entry("BI03", "item_arrangement"),
            Map.entry("BI06", "tagged_sample_deposit"), Map.entry("HS01", "tagged_item_claim"),
            Map.entry("HS02", "exact_tagged_deposit"), Map.entry("HS07", "tagged_key_console"),
            Map.entry("KV02", "tagged_container_sort"));

    private static final Map<String, String> EXPECTED_KINDS = Map.ofEntries(
            Map.entry("LS06", "exact_tagged_deposit"),
            Map.entry("A03", "twelve_lot_category_then_receipt_order"),
            Map.entry("A09", "receipt_gated_unique_locker_claim"),
            Map.entry("RP02", "six_named_testimonies_empty_averyn_plus_three_artifacts"),
            Map.entry("CW02", "eight_sample_three_axis_sort"),
            Map.entry("CW07", "three_item_claim_and_file"),
            Map.entry("BI02", "four_fragment_stratigraphy"),
            Map.entry("BI03", "three_instrument_tags_plus_time"),
            Map.entry("BI06", "three_dated_viability_samples"),
            Map.entry("HS01", "atomic_unique_artifact_claim"),
            Map.entry("HS02", "exact_artifact_install"),
            Map.entry("HS07", "five_subsystem_flags_plus_key"),
            Map.entry("KV02", "complete_unique_item_sort"));

    private final PhysicalPredicateAuthority authority;
    private final Map<String, NodeRule> rules;
    private final Map<String, ItemRequirement> recognizedIdentities;

    public ContainerAuthorityContract(PhysicalPredicateAuthority authority) {
        this.authority = Objects.requireNonNull(authority, "authority");
        ContainerPredicateCoverage.validateAgainst(authority);
        Map<String, NodeRule> built = new LinkedHashMap<>();
        Map<String, ItemRequirement> recognized = new LinkedHashMap<>();
        for (String nodeId : ContainerPredicateCoverage.orderedNodeIds()) {
            NodeRule rule = parseRule(authority.requireNode(nodeId));
            built.put(nodeId, rule);
            rule.acceptedIdentities().forEach((id, requirement) -> {
                ItemRequirement previous = recognized.putIfAbsent(id, requirement);
                if (previous != null && !previous.equals(requirement)) {
                    throw new IllegalStateException("identity " + id + " has conflicting item contracts");
                }
            });
        }
        recognized.put("deep_access_plate", new ItemRequirement("ECHO_SHARD", Map.of(
                ContainerItem.ARTIFACT_ID, "deep_access_plate")));
        this.rules = Map.copyOf(built);
        this.recognizedIdentities = Map.copyOf(recognized);
    }

    public PhysicalPredicateAuthority authority() {
        return authority;
    }

    public NodeRule rule(String nodeId) {
        NodeRule rule = rules.get(nodeId);
        if (rule == null) {
            throw new IllegalArgumentException("not a V5 container node: " + nodeId);
        }
        return rule;
    }

    public Map<String, NodeRule> rules() {
        return rules;
    }

    public boolean recognizes(ContainerItem item) {
        if (item == null || item.amount() != 1) {
            return false;
        }
        return item.durableIdentity().map(encoded -> {
            int separator = encoded.indexOf(':');
            ItemRequirement requirement = recognizedIdentities.get(encoded.substring(separator + 1));
            return requirement != null && requirement.matches(item);
        }).orElse(false);
    }

    public boolean prerequisitesMet(String nodeId, Set<String> trueFlags) {
        return trueFlags.containsAll(rule(nodeId).authorityNode().prerequisites());
    }

    public Optional<String> firstMissingPrerequisite(String nodeId, Set<String> trueFlags) {
        return rule(nodeId).authorityNode().prerequisites().stream()
                .filter(flag -> !trueFlags.contains(flag)).findFirst();
    }

    public boolean isTrigger(String nodeId, TriggerKind kind, String component, int slot) {
        NodeRule rule = rule(nodeId);
        if (kind != rule.solveTrigger() || !rule.triggerComponents().contains(component)) {
            return false;
        }
        if (kind != TriggerKind.SOURCE_CLAIM) {
            return true;
        }
        return rule.exactSlots().getOrDefault(component, Map.of()).containsKey(slot);
    }

    public boolean isPortableClaim(String nodeId, String component, int slot) {
        return rule(nodeId).portableClaims().getOrDefault(component, Map.of()).containsKey(slot);
    }

    public Optional<ItemRequirement> portableClaimRequirement(
            String nodeId, String component, int slot) {
        return Optional.ofNullable(rule(nodeId).portableClaims()
                .getOrDefault(component, Map.of()).get(slot));
    }

    public boolean isManagedComponent(String nodeId, String component) {
        NodeRule rule = rule(nodeId);
        return rule.exactSlots().containsKey(component)
                || rule.stagingComponents().contains(component)
                || rule.classSlots().containsKey(component)
                || rule.portableClaims().containsKey(component);
    }

    public boolean allowsInsertion(String nodeId, String component, int slot, ContainerItem item) {
        NodeRule rule = rule(nodeId);
        if (item == null || item.amount() != 1 || !item.hasUniqueArtifactIdentity()) {
            return false;
        }
        ItemRequirement exact = rule.exactSlots().getOrDefault(component, Map.of()).get(slot);
        if (exact != null) {
            return exact.matches(item);
        }
        if (rule.stagingComponents().contains(component)) {
            return item.durableIdentity().map(encoded -> {
                String id = encoded.substring(encoded.indexOf(':') + 1);
                ItemRequirement accepted = rule.acceptedIdentities().get(id);
                return accepted != null && accepted.matches(item);
            }).orElse(false);
        }
        if (rule.classSlots().getOrDefault(component, Set.of()).contains(slot)) {
            String expectedClass = rule.classValues().get(component);
            return expectedClass != null && expectedClass.equals(item.pdc().get("v5_sort_class"))
                    && rule.classMaterialCounts().getOrDefault(component, Map.of())
                    .containsKey(item.material())
                    && item.evidenceId().map(rule.acceptedIdentities()::containsKey).orElse(false);
        }
        return false;
    }

    public boolean allowsExtraction(String nodeId, String component, int slot, ContainerItem item) {
        NodeRule rule = rule(nodeId);
        if (item == null || !recognizes(item) || rule.immutableSourceComponents().contains(component)
                || isPortableClaim(nodeId, component, slot)
                || rule.solveTrigger() == TriggerKind.SOURCE_CLAIM
                && rule.triggerComponents().contains(component)) {
            return false;
        }
        return rule.stagingComponents().contains(component)
                || rule.exactSlots().getOrDefault(component, Map.of()).containsKey(slot)
                || rule.classSlots().getOrDefault(component, Set.of()).contains(slot);
    }

    /** Evaluates the exact authored operations; no radius/touch fallback exists. */
    public ContainerAttempt evaluate(
            String nodeId, TriggerKind trigger, String component, int slot,
            ContainerObservation observation) {
        NodeRule rule = rule(nodeId);
        if (!nodeId.equals(observation.nodeId()) || !isTrigger(nodeId, trigger, component, slot)) {
            return ContainerAttempt.of(ContainerAttempt.Status.WRONG,
                    "That is not this filing's marked control.");
        }
        if (!observation.trueFlags().containsAll(rule.authorityNode().prerequisites())) {
            return ContainerAttempt.of(ContainerAttempt.Status.LOCKED,
                    "The preceding record is not yet complete.");
        }
        boolean valid = switch (nodeId) {
            case "LS06" -> exactAndUnique(rule, observation)
                    && observation.item("orientation_key_source", 13).isEmpty();
            case "A03", "CW02", "BI02", "BI06" -> exactAndUnique(rule, observation);
            case "A09", "HS01" -> exactAndUnique(rule, observation)
                    && observation.destinationCapacity() >= 1;
            case "RP02" -> exactAndUnique(rule, observation)
                    && observation.item("averyn_slot", 13).isEmpty();
            case "CW07" -> exactAndUnique(rule, observation)
                    && observation.actorLinked()
                    && allHeld(rule, observation);
            case "BI03" -> exactAndUnique(rule, observation)
                    && observation.frameRotations().getOrDefault("time_dial", -1) == 7;
            case "HS02" -> exactAndUnique(rule, observation);
            case "HS07" -> exactAndUnique(rule, observation)
                    && observation.trueFlags().containsAll(Set.of(
                    "v5_hs02_installed", "v5_hs03_lamps", "v5_hs04_pressure",
                    "v5_hs05_dials", "v5_hs06_passage"))
                    && observation.identityCount("artifact", "deep_access_plate") == 0;
            case "KV02" -> kv02Valid(rule, observation);
            default -> throw new IllegalStateException("unhandled container node " + nodeId);
        };
        return valid ? ContainerAttempt.ready(commitPlan(rule, observation))
                : ContainerAttempt.of(ContainerAttempt.Status.WRONG, rule.wrongFeedback());
    }

    public String heldBit(String nodeId, String identityId) {
        return "v5_held:" + nodeId.toLowerCase(Locale.ROOT) + ':' + identityId;
    }

    private boolean allHeld(NodeRule rule, ContainerObservation observation) {
        for (ItemRequirement requirement : rule.acceptedIdentities().values()) {
            if ("evidence".equals(requirement.identityType())
                    && !observation.actorHeldEvidenceBits().contains(
                    heldBit(rule.nodeId(), requirement.identityId()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean exactAndUnique(NodeRule rule, ContainerObservation observation) {
        for (Map.Entry<String, Map<Integer, ItemRequirement>> component : rule.exactSlots().entrySet()) {
            for (Map.Entry<Integer, ItemRequirement> slot : component.getValue().entrySet()) {
                ContainerItem actual = observation.item(component.getKey(), slot.getKey()).orElse(null);
                if (!slot.getValue().matches(actual)
                        || observation.identityCount(slot.getValue().identityType(),
                        slot.getValue().identityId()) != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean kv02Valid(NodeRule rule, ContainerObservation observation) {
        Map<String, Integer> seen = new HashMap<>();
        int total = 0;
        for (String component : rule.classSlots().keySet()) {
            Map<String, Integer> materials = new HashMap<>();
            String expectedClass = rule.classValues().get(component);
            for (int slot : rule.classSlots().get(component)) {
                ContainerItem item = observation.item(component, slot).orElse(null);
                if (item == null) {
                    continue;
                }
                String evidenceId = item.evidenceId().orElse("");
                ItemRequirement expected = rule.acceptedIdentities().get(evidenceId);
                if (expected == null || !expected.matches(item)
                        || !expectedClass.equals(item.pdc().get("v5_sort_class"))) {
                    return false;
                }
                seen.merge(evidenceId, 1, Integer::sum);
                materials.merge(item.material(), 1, Integer::sum);
                total++;
            }
            if (!materials.equals(rule.classMaterialCounts().get(component))) {
                return false;
            }
        }
        if (total != 36 || seen.size() != 36 || seen.values().stream().anyMatch(count -> count != 1)) {
            return false;
        }
        for (String id : rule.acceptedIdentities().keySet()) {
            if (observation.identityCount("evidence", id) != 1) {
                return false;
            }
        }
        return true;
    }

    private static ContainerCommitPlan commitPlan(NodeRule rule, ContainerObservation observation) {
        List<ItemDisposition> dispositions = new ArrayList<>();
        switch (rule.nodeId()) {
            case "LS06" -> dispositions.add(existingDisposition(rule, observation,
                    "orientation_file", 13, Mode.DELIVER_EXISTING_TO_ACTOR, "return"));
            case "A09" -> dispositions.add(existingDisposition(rule, observation,
                    "locker", 13, Mode.DELIVER_EXISTING_TO_ACTOR, "claim"));
            case "RP02" -> addAllExact(rule, observation, dispositions, Mode.HOLD_IN_FIXTURE, "release");
            case "CW07" -> addAllExact(rule, observation, dispositions, Mode.HOLD_IN_FIXTURE, "seal");
            case "BI06" -> addAllExact(rule, observation, dispositions, Mode.HOLD_IN_FIXTURE, "seal");
            case "HS01" -> dispositions.add(existingDisposition(rule, observation,
                    "source", 13, Mode.DELIVER_EXISTING_TO_ACTOR, "claim"));
            case "HS02" -> addAllExact(rule, observation, dispositions, Mode.HOLD_IN_FIXTURE, "install");
            case "HS07" -> {
                dispositions.add(existingDisposition(rule, observation,
                        "key_console", 13, Mode.DELIVER_EXISTING_TO_ACTOR, "return"));
                UUID instance = UUID.randomUUID();
                dispositions.add(new ItemDisposition(Mode.DELIVER_NEW_ARTIFACT_TO_ACTOR,
                        "", -1, "deep_access_plate", "ECHO_SHARD", Optional.empty(),
                        Optional.of(instance), "container:HS07:reward:deep_access_plate:" + instance));
            }
            default -> {
                // Ordering/sort nodes latch their protected inventories without consuming them.
            }
        }
        return new ContainerCommitPlan(dispositions, true);
    }

    private static void addAllExact(
            NodeRule rule, ContainerObservation observation, List<ItemDisposition> destination,
            Mode mode, String stage) {
        rule.exactSlots().forEach((component, slots) -> slots.keySet().stream().sorted()
                .forEach(slot -> destination.add(existingDisposition(
                        rule, observation, component, slot, mode, stage))));
    }

    private static ItemDisposition existingDisposition(
            NodeRule rule, ContainerObservation observation, String component, int slot,
            Mode mode, String stage) {
        ContainerItem item = observation.item(component, slot).orElseThrow();
        String identity = item.artifactId().or(() -> item.evidenceId()).orElseThrow();
        String suffix = item.artifactInstance().map(UUID::toString)
                .orElseGet(() -> item.fingerprintSha256().substring(0, 16));
        return new ItemDisposition(mode, component, slot, identity, item.material(),
                Optional.of(item), Optional.empty(), "container:" + rule.nodeId() + ':'
                + stage + ':' + identity + ':' + suffix);
    }

    private NodeRule parseRule(PhysicalPredicateAuthority.Node node) {
        String nodeId = node.nodeId();
        if (PredicateCoverageCatalog.require(nodeId).family() != ImplementationFamily.C
                || !EXPECTED_HANDLERS.get(nodeId).equals(node.handler())
                || !EXPECTED_KINDS.get(nodeId).equals(node.predicate().kind())) {
            throw new IllegalStateException("container handler/kind drift for " + nodeId);
        }
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        List<String> operations = new ArrayList<>();
        predicate.getAsJsonArray("all_of").forEach(value ->
                operations.add(value.getAsJsonObject().get("op").getAsString()));
        if (!EXPECTED_OPERATIONS.get(nodeId).equals(operations)) {
            throw new IllegalStateException(nodeId + " container operations drifted: " + operations);
        }
        Map<String, JsonObject> components = components(predicate);
        Map<String, Map<Integer, ItemRequirement>> exact = new LinkedHashMap<>();
        Map<String, ItemRequirement> accepted = new LinkedHashMap<>();
        Set<String> staging = new LinkedHashSet<>();
        Set<String> immutable = new LinkedHashSet<>();
        Map<String, Set<Integer>> classSlots = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> classCounts = new LinkedHashMap<>();
        Map<String, String> classValues = new LinkedHashMap<>();
        Map<String, Map<Integer, ItemRequirement>> portable = new LinkedHashMap<>();
        TriggerKind trigger = TriggerKind.HANDLE;
        Set<String> triggers = ContainerTriggerBindings.acceptedComponents(
                nodeId, components.containsKey("handle"));

        switch (nodeId) {
            case "LS06" -> {
                ItemRequirement key = requirement(components.get("orientation_key"));
                put(exact, "orientation_file", 13, key);
                put(portable, "orientation_key_source", 13, key);
                accepted.put(key.identityId(), key);
                immutable.add("orientation_key_source");
                requireSlot(components.get("orientation_key_source"), 13, nodeId);
                requireSlot(components.get("orientation_file"), 13, nodeId);
            }
            case "A03" -> {
                JsonArray required = components.get("lots").getAsJsonArray("required");
                if (required.size() != 12) fail(nodeId, "must define 12 lots");
                for (int index = 0; index < required.size(); index++) {
                    JsonObject item = required.get(index).getAsJsonObject();
                    Map<String, String> pdc = Map.of(
                            "v5_evidence_id", text(item, "id"),
                            "v5_lot_category", text(item, "category"),
                            "v5_receipt_number", item.get("receipt").getAsString());
                    ItemRequirement requirement = new ItemRequirement(text(item, "material"), pdc);
                    put(exact, "manifest_barrel", index, requirement);
                    accepted.put(requirement.identityId(), requirement);
                }
                staging.add("lot_staging");
            }
            case "A09" -> {
                ItemRequirement spool = requirement(components.get("spool"));
                put(exact, "locker", 13, spool);
                accepted.put(spool.identityId(), spool);
                immutable.add("locker");
                trigger = TriggerKind.SOURCE_CLAIM;
                triggers = Set.of("locker");
                requireSlot(components.get("locker"), 13, nodeId);
            }
            case "RP02" -> {
                JsonArray containers = components.get("testimony_bank").getAsJsonArray("containers");
                if (containers.size() != 6) fail(nodeId, "must define six testimony housings");
                for (JsonElement element : containers) {
                    JsonObject item = element.getAsJsonObject();
                    String name = text(item, "name").toLowerCase(Locale.ROOT);
                    ItemRequirement requirement = requirement(item);
                    put(exact, "testimony_bank:" + name, requireInt(item, "slot"), requirement);
                    accepted.put(requirement.identityId(), requirement);
                }
                for (String component : List.of("cistern", "system", "bridge")) {
                    JsonObject housing = components.get(component);
                    ItemRequirement requirement = requirement(housing.getAsJsonObject("required_item"));
                    put(exact, component, requireInt(housing, "slot"), requirement);
                    accepted.put(requirement.identityId(), requirement);
                }
                requireSlot(components.get("averyn_slot"), 13, nodeId);
                immutable.addAll(exact.keySet());
                immutable.add("averyn_slot");
            }
            case "CW02" -> {
                JsonObject sampleSet = components.get("sample_set");
                List<String> ids = strings(sampleSet.getAsJsonArray("required_ids"));
                if (!ids.equals(List.of("A3T", "A3L", "A4T", "A4L",
                        "B3T", "B3L", "B4T", "B4L"))) fail(nodeId, "sample IDs drifted");
                Map<String, ItemRequirement> samples = new LinkedHashMap<>();
                for (String id : ids) {
                    samples.put(id, new ItemRequirement("POTION", Map.of(
                            "v5_evidence_id", id,
                            "v5_intake", id.substring(0, 1),
                            "v5_cycle", id.substring(1, 2),
                            "v5_depth", id.endsWith("T") ? "TOP" : "LOWER")));
                }
                putSlotsById(exact, components, samples, "A_top", "A_lower", "B_top", "B_lower");
                accepted.putAll(samples);
                staging.add("sample_source");
            }
            case "CW07" -> {
                Map<String, ItemRequirement> evidence = Map.of(
                        "cw07_genuine_filter", requirement(components.get("filter")),
                        "cw07_purchase_receipt", requirement(components.get("receipt")),
                        "cw07_discipline_drafts", requirement(components.get("drafts")));
                putSlotsById(exact, components, evidence, "filing_barrel");
                putSlotsById(portable, components, evidence, "cache");
                accepted.putAll(evidence);
                immutable.add("cache");
            }
            case "BI02" -> {
                Map<String, ItemRequirement> evidence = requirementsById(components,
                        "old1", "old2", "old3", "dust");
                putSlotsById(exact, components, evidence, "rail");
                accepted.putAll(evidence);
                staging.add("fragment_source");
            }
            case "BI03" -> {
                Map<String, ItemRequirement> evidence = requirementsById(components,
                        "feed", "water", "cover");
                putSlotsById(exact, components, evidence, "instrument_tray");
                accepted.putAll(evidence);
                staging.add("instrument_source");
                if (requireInt(components.get("time_dial"), "required_rotation") != 7) {
                    fail(nodeId, "time dial must be rotation 7");
                }
            }
            case "BI06" -> {
                Map<String, ItemRequirement> evidence = requirementsById(components,
                        "reed", "water", "air");
                putSlotsById(exact, components, evidence, "tray");
                accepted.putAll(evidence);
                staging.add("sample_source");
            }
            case "HS01" -> {
                ItemRequirement cartridge = requirement(components.get("cartridge"));
                put(exact, "source", 13, cartridge);
                accepted.put(cartridge.identityId(), cartridge);
                immutable.add("source");
                trigger = TriggerKind.SOURCE_CLAIM;
                triggers = Set.of("source");
                requireSlot(components.get("source"), 13, nodeId);
            }
            case "HS02" -> {
                ItemRequirement cartridge = requirement(components.get("cartridge"));
                put(exact, "housing", 13, cartridge);
                accepted.put(cartridge.identityId(), cartridge);
                requireSlot(components.get("housing"), 13, nodeId);
            }
            case "HS07" -> {
                ItemRequirement key = requirement(components.get("system_key"));
                put(exact, "key_console", 13, key);
                accepted.put(key.identityId(), key);
                requireSlot(components.get("key_console"), 13, nodeId);
                if (!"hs07_sync".equals(pdc(components.get("handle")).get("v5_control_id"))) {
                    fail(nodeId, "synchronization handle PDC drifted");
                }
            }
            case "KV02" -> {
                int lotA = requireInt(components.get("input_lot_a"), "required_unique_count");
                int lotB = requireInt(components.get("input_lot_b"), "required_unique_count");
                if (lotA != 18 || lotB != 18) fail(nodeId, "input lots must each contain 18 items");
                staging.addAll(Set.of("input_lot_a", "input_lot_b"));
                for (String component : List.of("cistern", "public_heat", "private_heat", "condemned")) {
                    JsonObject definition = components.get(component);
                    Map<String, Integer> counts = integerMap(definition.getAsJsonObject("required_counts"));
                    Set<Integer> slots = new LinkedHashSet<>();
                    definition.getAsJsonArray("allowed_slots").forEach(value -> slots.add(value.getAsInt()));
                    int requiredCount = counts.values().stream().mapToInt(Integer::intValue).sum();
                    if (slots.size() != requiredCount) fail(nodeId, component + " slot/count mismatch");
                    JsonObject classPdc = definition.getAsJsonObject("class_pdc");
                    if (classPdc == null || !classPdc.has("v5_sort_class")) {
                        fail(nodeId, component + " lacks class PDC");
                    }
                    String sortClass = classPdc.get("v5_sort_class").getAsString();
                    classSlots.put(component, slots);
                    classCounts.put(component, counts);
                    classValues.put(component, sortClass);
                    for (Map.Entry<String, Integer> count : counts.entrySet()) {
                        for (int index = 1; index <= count.getValue(); index++) {
                            String id = "kv02_" + component + '_'
                                    + count.getKey().toLowerCase(Locale.ROOT) + '_' + index;
                            accepted.put(id, new ItemRequirement(count.getKey(), Map.of(
                                    "v5_evidence_id", id, "v5_sort_class", sortClass)));
                        }
                    }
                }
                if (accepted.size() != 36) fail(nodeId, "must derive 36 unique evidence items");
            }
            default -> throw new IllegalStateException("unhandled C rule " + nodeId);
        }
        String wrongFeedback = JsonParser.parseString(node.wrongInputJson()).getAsJsonObject()
                .get("feedback").getAsString();
        return new NodeRule(node, trigger, triggers, exact, accepted, staging, immutable,
                classSlots, classCounts, classValues, portable, wrongFeedback);
    }

    private static Map<String, JsonObject> components(JsonObject predicate) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement element : predicate.getAsJsonArray("components")) {
            JsonObject component = element.getAsJsonObject();
            String id = text(component, "id");
            if (result.put(id, component) != null) {
                throw new IllegalStateException("duplicate component " + id);
            }
        }
        return result;
    }

    private static Map<String, ItemRequirement> requirementsById(
            Map<String, JsonObject> components, String... componentIds) {
        Map<String, ItemRequirement> result = new LinkedHashMap<>();
        for (String componentId : componentIds) {
            ItemRequirement requirement = requirement(components.get(componentId));
            result.put(requirement.identityId(), requirement);
        }
        return result;
    }

    private static ItemRequirement requirement(JsonObject value) {
        if (value == null) throw new IllegalStateException("missing item definition");
        return new ItemRequirement(text(value, "material"), pdc(value));
    }

    private static Map<String, String> pdc(JsonObject value) {
        JsonObject pdc = value == null ? null : value.getAsJsonObject("pdc");
        if (pdc == null) throw new IllegalStateException("item/component is missing exact PDC");
        Map<String, String> result = new LinkedHashMap<>();
        pdc.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return Map.copyOf(result);
    }

    private static void putSlotsById(
            Map<String, Map<Integer, ItemRequirement>> destination,
            Map<String, JsonObject> components,
            Map<String, ItemRequirement> requirements,
            String... componentIds) {
        for (String componentId : componentIds) {
            JsonObject slots = components.get(componentId).getAsJsonObject("slots");
            if (slots == null) throw new IllegalStateException(componentId + " lacks slots");
            for (Map.Entry<String, JsonElement> entry : slots.entrySet()) {
                ItemRequirement requirement = requirements.get(entry.getKey());
                if (requirement == null) throw new IllegalStateException(
                        componentId + " references unknown identity " + entry.getKey());
                put(destination, componentId, entry.getValue().getAsInt(), requirement);
            }
        }
    }

    private static void put(
            Map<String, Map<Integer, ItemRequirement>> destination,
            String component, int slot, ItemRequirement requirement) {
        Map<Integer, ItemRequirement> slots = destination.computeIfAbsent(
                component, ignored -> new LinkedHashMap<>());
        ItemRequirement previous = slots.put(slot, requirement);
        if (previous != null) throw new IllegalStateException(component + " slot " + slot + " duplicated");
    }

    private static void requireSlot(JsonObject value, int expected, String nodeId) {
        if (requireInt(value, "slot") != expected) fail(nodeId, "required slot drifted");
    }

    private static int requireInt(JsonObject value, String key) {
        if (value == null || !value.has(key)) throw new IllegalStateException("missing integer " + key);
        return value.get(key).getAsInt();
    }

    private static Map<String, Integer> integerMap(JsonObject object) {
        Map<String, Integer> result = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return Map.copyOf(result);
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        array.forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }

    private static String text(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("missing text " + key);
        }
        return object.get(key).getAsString();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    private static void fail(String nodeId, String message) {
        throw new IllegalStateException(nodeId + ' ' + message);
    }
}
