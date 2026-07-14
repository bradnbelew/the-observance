package com.observance.watcher.v5runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, validated view of ARG-V5-PHYSICAL-PREDICATES.json. */
public final class PhysicalPredicateAuthority {
    public static final int REQUIRED_SCHEMA_VERSION = 1;
    public static final int REQUIRED_NODE_COUNT = 60;
    public static final String CAMPAIGN_VERSION = "v5";

    private final int schemaVersion;
    private final String authorityText;
    private final CoordinateConvention coordinateConvention;
    private final Map<String, DurabilityProfile> durabilityProfiles;
    private final GlobalRules globalRules;
    private final List<Node> nodes;
    private final Map<String, Node> nodesById;
    private final Map<String, Node> nodesByCompletionFlag;
    private final String sha256;

    PhysicalPredicateAuthority(
            int schemaVersion,
            String authorityText,
            CoordinateConvention coordinateConvention,
            Map<String, DurabilityProfile> durabilityProfiles,
            GlobalRules globalRules,
            List<Node> nodes,
            String sha256) {
        this.schemaVersion = schemaVersion;
        this.authorityText = Objects.requireNonNull(authorityText, "authorityText");
        this.coordinateConvention = Objects.requireNonNull(coordinateConvention, "coordinateConvention");
        this.durabilityProfiles = Map.copyOf(durabilityProfiles);
        this.globalRules = Objects.requireNonNull(globalRules, "globalRules");
        this.nodes = List.copyOf(nodes);
        this.sha256 = Objects.requireNonNull(sha256, "sha256");

        Map<String, Node> byId = new LinkedHashMap<>();
        Map<String, Node> byFlag = new LinkedHashMap<>();
        for (Node node : nodes) {
            byId.put(node.nodeId(), node);
            byFlag.put(node.completionFlag(), node);
        }
        this.nodesById = Map.copyOf(byId);
        this.nodesByCompletionFlag = Map.copyOf(byFlag);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String authorityText() {
        return authorityText;
    }

    public CoordinateConvention coordinateConvention() {
        return coordinateConvention;
    }

    public Map<String, DurabilityProfile> durabilityProfiles() {
        return durabilityProfiles;
    }

    public GlobalRules globalRules() {
        return globalRules;
    }

    public List<Node> nodes() {
        return nodes;
    }

    public Map<String, Node> nodesById() {
        return nodesById;
    }

    public Optional<Node> findNode(String nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    public Node requireNode(String nodeId) {
        Node node = nodesById.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Unknown V5 physical node: " + nodeId);
        }
        return node;
    }

    public Optional<Node> findByCompletionFlag(String completionFlag) {
        return Optional.ofNullable(nodesByCompletionFlag.get(completionFlag));
    }

    public String sha256() {
        return sha256;
    }

    public record CoordinateConvention(
            List<String> offsetOrder, String origin, String front, String rotation) {
        public CoordinateConvention {
            offsetOrder = List.copyOf(offsetOrder);
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(front, "front");
            Objects.requireNonNull(rotation, "rotation");
        }
    }

    public record DurabilityProfile(
            String authoritative, String localCommit, String supabase, String reconcile) {
        public DurabilityProfile {
            Objects.requireNonNull(authoritative, "authoritative");
            Objects.requireNonNull(localCommit, "localCommit");
            Objects.requireNonNull(supabase, "supabase");
            Objects.requireNonNull(reconcile, "reconcile");
        }
    }

    public record GlobalRules(
            String protection,
            String itemIdentity,
            String atomicity,
            String wrongItems,
            String recovery,
            String noTouchCompletion,
            String answerNormalization,
            String prerequisites,
            ConductRules conductVerdict) {
        public GlobalRules {
            Objects.requireNonNull(protection, "protection");
            Objects.requireNonNull(itemIdentity, "itemIdentity");
            Objects.requireNonNull(atomicity, "atomicity");
            Objects.requireNonNull(wrongItems, "wrongItems");
            Objects.requireNonNull(recovery, "recovery");
            Objects.requireNonNull(noTouchCompletion, "noTouchCompletion");
            Objects.requireNonNull(answerNormalization, "answerNormalization");
            Objects.requireNonNull(prerequisites, "prerequisites");
            Objects.requireNonNull(conductVerdict, "conductVerdict");
        }
    }

    public record ConductRules(
            String source,
            List<String> recordedInputsPerVote,
            String firstBallot,
            List<String> precedence,
            String totality,
            String persistedField,
            List<String> allowedValues) {
        public ConductRules {
            Objects.requireNonNull(source, "source");
            recordedInputsPerVote = List.copyOf(recordedInputsPerVote);
            Objects.requireNonNull(firstBallot, "firstBallot");
            precedence = List.copyOf(precedence);
            Objects.requireNonNull(totality, "totality");
            Objects.requireNonNull(persistedField, "persistedField");
            allowedValues = List.copyOf(allowedValues);
        }
    }

    public record PredicateDefinition(
            String kind,
            String evaluationTrigger,
            List<String> acceptedNormalizedAnswers,
            String canonicalJson) {
        public PredicateDefinition {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(evaluationTrigger, "evaluationTrigger");
            acceptedNormalizedAnswers = List.copyOf(acceptedNormalizedAnswers);
            Objects.requireNonNull(canonicalJson, "canonicalJson");
        }
    }

    public record Node(
            String nodeId,
            String owner,
            List<String> prerequisites,
            String siteId,
            String handler,
            String completionFlag,
            PredicateDefinition predicate,
            String wrongInputJson,
            String rewardJson,
            String resetRepairRecoveryJson,
            String concurrencyReplayJson,
            String durabilityProfile) {
        public Node {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(owner, "owner");
            prerequisites = List.copyOf(prerequisites);
            Objects.requireNonNull(siteId, "siteId");
            Objects.requireNonNull(handler, "handler");
            Objects.requireNonNull(completionFlag, "completionFlag");
            Objects.requireNonNull(predicate, "predicate");
            Objects.requireNonNull(wrongInputJson, "wrongInputJson");
            Objects.requireNonNull(rewardJson, "rewardJson");
            Objects.requireNonNull(resetRepairRecoveryJson, "resetRepairRecoveryJson");
            Objects.requireNonNull(concurrencyReplayJson, "concurrencyReplayJson");
            Objects.requireNonNull(durabilityProfile, "durabilityProfile");
        }
    }
}
