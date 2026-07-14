package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.ImplementationFamily;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.PlayerProgress;
import com.observance.watcher.v5runtime.PredicateCoverageCatalog;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.ArtifactDelivery;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.AsyncMirror;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.ExternalFlagSnapshot;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.PlayerFeedback;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.Trigger;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.WorldMutation;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.WorldState;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Atomic local-primary execution engine for all S/I/F/L/R physical predicates. */
public final class V5MechanicsEngine {
    private static final Duration NO_SERVER_THREAD_WAIT = Duration.ZERO;
    private static final String FALLBACK_REJECTION = "The mechanism rejects that filing.";

    private final PhysicalPredicateAuthority authority;
    private final V5ProgressStore progress;
    private final PhysicalPredicateEvaluator evaluator;
    private final ExternalFlagSnapshot externalFlags;
    private final WorldState worldState;
    private final WorldMutation worldMutation;
    private final ArtifactDelivery artifacts;
    private final AsyncMirror mirror;
    private final PlayerFeedback feedback;
    private final SiteMutexes mutexes;

    public V5MechanicsEngine(
            PhysicalPredicateAuthority authority,
            V5ProgressStore progress,
            ExternalFlagSnapshot externalFlags,
            WorldState worldState,
            WorldMutation worldMutation,
            ArtifactDelivery artifacts,
            AsyncMirror mirror,
            PlayerFeedback feedback) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.externalFlags = Objects.requireNonNull(externalFlags, "externalFlags");
        this.worldState = Objects.requireNonNull(worldState, "worldState");
        this.worldMutation = Objects.requireNonNull(worldMutation, "worldMutation");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.mirror = Objects.requireNonNull(mirror, "mirror");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.evaluator = new PhysicalPredicateEvaluator();
        this.mutexes = new SiteMutexes();
        PhysicalPredicateEvaluator.validateCoverage(authority);
        if (!progress.manifestSha256().equals(authority.sha256())) {
            throw new IllegalArgumentException("progress store authority hash mismatch");
        }
    }

    public Set<String> implementedNodeIds() {
        return AssignedPhysicalNodes.implementedNodeIds();
    }

    public boolean isComplete(String nodeId) {
        PhysicalPredicateAuthority.Node node = requireOwned(nodeId);
        return progress.snapshot().isComplete(node.completionFlag());
    }

    public MechanicOutcome evaluate(String nodeId, UUID actor, Trigger trigger) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(trigger, "trigger");
        PhysicalPredicateAuthority.Node node = requireOwned(nodeId);
        ProgressSnapshot initial = progress.snapshot();
        if (initial.isComplete(node.completionFlag())) {
            return MechanicOutcome.of(MechanicOutcome.Status.ALREADY_COMPLETE);
        }
        if (!prerequisitesMet(node, initial)) {
            return MechanicOutcome.of(MechanicOutcome.Status.PREREQUISITE_MISSING);
        }

        final Optional<SiteMutexes.Guard> acquired;
        try {
            acquired = mutexes.tryAcquire(node.siteId(), NO_SERVER_THREAD_WAIT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return MechanicOutcome.of(MechanicOutcome.Status.SITE_BUSY);
        }
        if (acquired.isEmpty()) {
            return MechanicOutcome.of(MechanicOutcome.Status.SITE_BUSY);
        }

        try (SiteMutexes.Guard ignored = acquired.orElseThrow()) {
            ProgressSnapshot before = progress.snapshot();
            if (before.isComplete(node.completionFlag())) {
                return MechanicOutcome.of(MechanicOutcome.Status.ALREADY_COMPLETE);
            }
            if (!prerequisitesMet(node, before)) {
                return MechanicOutcome.of(MechanicOutcome.Status.PREREQUISITE_MISSING);
            }
            MechanicObservation observation = worldState.capture(node, actor, trigger);
            EvaluationEnvironment environment = environment(node, actor, before);
            PredicateEvaluation evaluation = evaluator.evaluate(node, observation, environment);
            if (!evaluation.satisfied()) {
                feedback.send(actor, safeWrongFeedback(node));
                return MechanicOutcome.diagnostic(
                        MechanicOutcome.Status.WRONG_INPUT,
                        evaluation.failedOperation().orElse("predicate"));
            }

            List<PendingReward> rewards = prepareRewards(node, actor, before);
            if (rewards == null) {
                return MechanicOutcome.of(MechanicOutcome.Status.DUPLICATE_ARTIFACT_BLOCKED);
            }
            final boolean committed;
            try {
                committed = progress.transact(editor -> {
                    boolean won = editor.compareAndSetCompletion(
                            node.completionFlag(), false, true);
                    if (!won) {
                        return false;
                    }
                    if (PredicateCoverageCatalog.require(node.nodeId()).family()
                            == ImplementationFamily.R) {
                        editor.addPlayerBit(
                                actor, PlayerBitDomain.ROUTE,
                                PhysicalPredicateEvaluator.routeBit(node.nodeId()));
                    }
                    for (PendingReward reward : rewards) {
                        editor.putEscrowOnce(reward.entry());
                    }
                    return true;
                });
            } catch (IOException | RuntimeException exception) {
                return MechanicOutcome.diagnostic(
                        MechanicOutcome.Status.LOCAL_COMMIT_FAILED,
                        exception.getClass().getSimpleName());
            }
            if (!committed) {
                return MechanicOutcome.of(MechanicOutcome.Status.ALREADY_COMPLETE);
            }

            boolean recoveryPending = false;
            try {
                worldMutation.applyAfterLocalCommit(node, actor, observation);
            } catch (Exception exception) {
                recoveryPending = true;
            }
            for (PendingReward reward : rewards) {
                try {
                    if (artifacts.deliverOrKeepEscrow(actor, reward.entry())) {
                        markDelivered(reward.entry());
                    } else {
                        recoveryPending = true;
                    }
                } catch (Exception exception) {
                    recoveryPending = true;
                }
            }
            mirror.enqueue(node, progress.snapshot().revision());
            return MechanicOutcome.of(recoveryPending
                    ? MechanicOutcome.Status.WORLD_RECOVERY_PENDING
                    : MechanicOutcome.Status.COMPLETED);
        }
    }

    /** Persists an exact source-open bit before issuing or accepting any generated receipt. */
    public boolean recordInspection(String nodeId, UUID actor, String exactSource) throws IOException {
        PhysicalPredicateAuthority.Node node = requireOwned(nodeId);
        requireSource(node, exactSource);
        return progress.transact(editor -> editor.addPlayerBit(
                actor, PlayerBitDomain.INSPECTION,
                PhysicalPredicateEvaluator.inspectionBit(nodeId, exactSource)));
    }

    /** AR07's false-M action is session-scoped and is removed on quit/teleport/death. */
    public boolean recordSessionEvent(String nodeId, UUID actor, String exactEvent) throws IOException {
        PhysicalPredicateAuthority.Node node = requireOwned(nodeId);
        if (!"AR07".equals(node.nodeId()) || !"false_m".equals(exactEvent)) {
            throw new IllegalArgumentException("unregistered session event " + nodeId + ':' + exactEvent);
        }
        return progress.transact(editor -> editor.addPlayerBit(
                actor, PlayerBitDomain.SESSION,
                PhysicalPredicateEvaluator.sessionBit(nodeId, exactEvent)));
    }

    public void clearTransientPlayerState(UUID actor) throws IOException {
        PlayerProgress player = progress.snapshot().players().get(actor.toString());
        if (player == null || player.sessionBits().isEmpty()) {
            return;
        }
        progress.transact(editor -> {
            for (String bit : player.sessionBits()) {
                editor.clearSessionBit(actor, bit);
            }
            return null;
        });
    }

    /** Reprojects every committed world latch and retries every pending unique reward. */
    public List<String> recoverCommittedWorld() {
        List<String> failures = new ArrayList<>();
        ProgressSnapshot snapshot = progress.snapshot();
        for (String nodeId : AssignedPhysicalNodes.implementedNodeIds()) {
            PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
            if (!snapshot.isComplete(node.completionFlag())) {
                continue;
            }
            try {
                worldMutation.recoverCommitted(node);
            } catch (Exception exception) {
                failures.add(nodeId + ":world");
            }
        }
        for (EscrowEntry entry : snapshot.escrow().values()) {
            if (entry.status() != EscrowStatus.DELIVERY_PENDING
                    || !"reward".equals(entry.metadata().get("kind"))) {
                continue;
            }
            try {
                UUID actor = entry.intendedPlayer().orElseThrow();
                if (artifacts.deliverOrKeepEscrow(actor, entry)) {
                    markDelivered(entry);
                } else {
                    failures.add(entry.escrowId() + ":delivery");
                }
            } catch (Exception exception) {
                failures.add(entry.escrowId() + ":delivery");
            }
        }
        return List.copyOf(failures);
    }

    private PhysicalPredicateAuthority.Node requireOwned(String nodeId) {
        if (!AssignedPhysicalNodes.implementedNodeIds().contains(nodeId)) {
            throw new IllegalArgumentException("node not owned by S/I/F/L/R mechanics: " + nodeId);
        }
        return authority.requireNode(nodeId);
    }

    private boolean prerequisitesMet(
            PhysicalPredicateAuthority.Node node, ProgressSnapshot snapshot) {
        for (String prerequisite : node.prerequisites()) {
            if (!snapshot.isComplete(prerequisite)
                    && !Boolean.TRUE.equals(snapshot.booleans().get(prerequisite))
                    && !externalFlags.isTrue(prerequisite)) {
                return false;
            }
        }
        return true;
    }

    private EvaluationEnvironment environment(
            PhysicalPredicateAuthority.Node node, UUID actor, ProgressSnapshot snapshot) {
        Set<String> flags = new HashSet<>();
        snapshot.booleans().forEach((key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                flags.add(key);
            }
        });
        JsonObject predicate = parseObject(node.predicate().canonicalJson(), node.nodeId());
        for (JsonElement operation : predicate.getAsJsonArray("all_of")) {
            JsonObject value = operation.getAsJsonObject();
            if ("prior_receipt_true".equals(value.get("op").getAsString())) {
                String flag = value.get("flag").getAsString();
                if (externalFlags.isTrue(flag)) {
                    flags.add(flag);
                }
            }
        }
        PlayerProgress player = snapshot.players().getOrDefault(actor.toString(), PlayerProgress.empty());
        Set<String> bits = new HashSet<>(player.inspections());
        bits.addAll(player.sessionBits());
        return new EvaluationEnvironment(flags, bits);
    }

    private List<PendingReward> prepareRewards(
            PhysicalPredicateAuthority.Node node, UUID actor, ProgressSnapshot snapshot) {
        JsonObject reward = parseObject(node.rewardJson(), node.nodeId() + " reward");
        JsonArray ids = reward.getAsJsonArray("artifact_ids");
        List<PendingReward> result = new ArrayList<>();
        for (JsonElement value : ids) {
            String artifactId = value.getAsString();
            String escrowId = rewardEscrowId(artifactId);
            EscrowEntry existing = snapshot.escrow().get(escrowId);
            if (existing != null) {
                if (!node.nodeId().equals(existing.metadata().get("node_id"))) {
                    return null;
                }
                result.add(new PendingReward(existing));
                continue;
            }
            if (!artifacts.scanInstances(artifactId).isEmpty()) {
                return null;
            }
            UUID instanceId = UUID.randomUUID();
            MechanicItem template = artifacts.template(artifactId, instanceId);
            if (!artifactId.equals(template.pdc().get(MechanicItem.ARTIFACT_ID))
                    || template.artifactInstance().filter(instanceId::equals).isEmpty()) {
                throw new IllegalStateException("artifact template identity mismatch for " + artifactId);
            }
            long now = System.currentTimeMillis();
            EscrowEntry pending = new EscrowEntry(
                    escrowId,
                    artifactId,
                    Optional.of(actor),
                    node.siteId(),
                    -1,
                    template.fingerprintSha256(),
                    1,
                    now,
                    now,
                    EscrowStatus.DELIVERY_PENDING,
                    Map.of(
                            "kind", "reward",
                            "node_id", node.nodeId(),
                            "instance_uuid", instanceId.toString()));
            result.add(new PendingReward(pending));
        }
        return List.copyOf(result);
    }

    private void markDelivered(EscrowEntry pending) throws IOException {
        long now = Math.max(System.currentTimeMillis(), pending.updatedAtEpochMillis());
        EscrowEntry delivered = new EscrowEntry(
                pending.escrowId(), pending.artifactId(), pending.intendedPlayer(),
                pending.sourceSiteId(), pending.sourceSlot(), pending.itemFingerprintSha256(),
                pending.amount(), pending.createdAtEpochMillis(), now, EscrowStatus.DELIVERED,
                pending.metadata());
        progress.transact(editor -> editor.transitionEscrow(
                pending.escrowId(), EscrowStatus.DELIVERY_PENDING, delivered));
    }

    private static String safeWrongFeedback(PhysicalPredicateAuthority.Node node) {
        JsonObject wrong = parseObject(node.wrongInputJson(), node.nodeId() + " wrong_input");
        String candidate = wrong.get("feedback").getAsString();
        String normalizedFeedback = com.observance.watcher.v5runtime.AnswerNormalizer
                .normalize(candidate);
        JsonObject predicate = parseObject(node.predicate().canonicalJson(), node.nodeId());
        if (predicate.has("accepted")) {
            for (JsonElement accepted : predicate.getAsJsonArray("accepted")) {
                String normalizedAnswer = com.observance.watcher.v5runtime.AnswerNormalizer
                        .normalize(accepted.getAsString());
                if (!normalizedAnswer.isBlank() && normalizedFeedback.contains(normalizedAnswer)) {
                    return FALLBACK_REJECTION;
                }
            }
        }
        return candidate;
    }

    private static void requireSource(
            PhysicalPredicateAuthority.Node node, String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source cannot be blank");
        }
        JsonObject predicate = parseObject(node.predicate().canonicalJson(), node.nodeId());
        String canonical = predicate.toString();
        if (!canonical.contains('"' + source + '"')) {
            throw new IllegalArgumentException(
                    source + " is not an exact source for " + node.nodeId());
        }
    }

    private static JsonObject parseObject(String json, String label) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) {
            throw new IllegalStateException(label + " is not a JSON object");
        }
        return parsed.getAsJsonObject();
    }

    private static String rewardEscrowId(String artifactId) {
        return "v5_reward_" + artifactId;
    }

    private record PendingReward(EscrowEntry entry) {
    }
}
