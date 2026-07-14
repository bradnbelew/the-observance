package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.Trigger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** 41-node table: correct, wrong, duplicate, replay, disconnect, prerequisite, and concurrency. */
public final class V5MechanicsEngineSelfTest {
    private V5MechanicsEngineSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        PhysicalPredicateAuthority authority = PhysicalPredicateAuthorityLoader.loadDefault();
        PhysicalPredicateEvaluator.validateCoverage(authority);
        check(AssignedPhysicalNodes.implementedNodeIds().size() == 41,
                "expected 41 nodes with RP04 ritual-owned");
        int scenarios = 0;
        for (String nodeId : AssignedPhysicalNodes.implementedNodeIds().stream().sorted().toList()) {
            PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
            UUID actor = UUID.nameUUIDFromBytes(("actor:" + nodeId).getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));

            Harness correct = harness(authority, node, actor, true, true);
            MechanicOutcome completed = correct.engine.evaluate(nodeId, actor, trigger(nodeId));
            check(Set.of(MechanicOutcome.Status.COMPLETED,
                    MechanicOutcome.Status.WORLD_RECOVERY_PENDING).contains(completed.status()),
                    nodeId + " correct path failed: " + completed);
            check(correct.store.snapshot().isComplete(node.completionFlag()),
                    nodeId + " did not commit completion");
            scenarios++;

            int rewardsAfterFirst = correct.artifacts.delivered.size();
            MechanicOutcome replay = correct.engine.evaluate(nodeId, actor, trigger(nodeId));
            check(replay.status() == MechanicOutcome.Status.ALREADY_COMPLETE,
                    nodeId + " replay was not idempotent");
            check(correct.artifacts.delivered.size() == rewardsAfterFirst,
                    nodeId + " replay duplicated a reward");
            scenarios++;

            Harness wrong = harness(authority, node, actor, true, false);
            MechanicOutcome rejected = wrong.engine.evaluate(nodeId, actor, trigger(nodeId));
            check(rejected.status() == MechanicOutcome.Status.WRONG_INPUT,
                    nodeId + " wrong input did not fail closed: " + rejected);
            check(!wrong.store.snapshot().isComplete(node.completionFlag()),
                    nodeId + " wrong input committed");
            check(wrong.artifacts.delivered.isEmpty(), nodeId + " wrong input rewarded");
            scenarios++;

            Harness prerequisite = harness(authority, node, actor, false, true);
            MechanicOutcome blocked = prerequisite.engine.evaluate(nodeId, actor, trigger(nodeId));
            if (node.prerequisites().isEmpty()) {
                check(blocked.status() != MechanicOutcome.Status.PREREQUISITE_MISSING,
                        nodeId + " invented a prerequisite");
            } else {
                check(blocked.status() == MechanicOutcome.Status.PREREQUISITE_MISSING,
                        nodeId + " bypassed its prerequisite");
                check(!prerequisite.store.snapshot().isComplete(node.completionFlag()),
                        nodeId + " prerequisite path mutated completion");
            }
            scenarios++;

            Harness disconnect = harness(authority, node, actor, true, true);
            if ("AR07".equals(nodeId)) {
                disconnect.engine.recordSessionEvent("AR07", actor, "false_m");
            }
            disconnect.engine.clearTransientPlayerState(actor);
            check(!disconnect.store.snapshot().isComplete(node.completionFlag()),
                    nodeId + " disconnect completed a puzzle");
            check(disconnect.store.snapshot().escrow().isEmpty(),
                    nodeId + " disconnect invented escrow");
            scenarios++;

            Harness duplicate = harness(authority, node, actor, true, true);
            String reward = rewardId(node);
            if (reward != null) {
                duplicate.artifacts.preexisting.add(reward);
                MechanicOutcome duplicateResult = duplicate.engine.evaluate(
                        nodeId, actor, trigger(nodeId));
                check(duplicateResult.status()
                                == MechanicOutcome.Status.DUPLICATE_ARTIFACT_BLOCKED,
                        nodeId + " duplicate artifact was not blocked");
                check(!duplicate.store.snapshot().isComplete(node.completionFlag()),
                        nodeId + " duplicate artifact path committed");
            } else {
                MechanicOutcome first = duplicate.engine.evaluate(nodeId, actor, trigger(nodeId));
                MechanicOutcome second = duplicate.engine.evaluate(nodeId, actor, trigger(nodeId));
                check(first.status() == MechanicOutcome.Status.COMPLETED
                                && second.status() == MechanicOutcome.Status.ALREADY_COMPLETE,
                        nodeId + " duplicate/replay latch failed");
            }
            scenarios++;

            Harness concurrent = harness(authority, node, actor, true, true);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<MechanicOutcome> left = new AtomicReference<>();
            AtomicReference<MechanicOutcome> right = new AtomicReference<>();
            Thread one = Thread.ofPlatform().start(() -> runConcurrent(
                    concurrent.engine, nodeId, actor, start, left));
            Thread two = Thread.ofPlatform().start(() -> runConcurrent(
                    concurrent.engine, nodeId, actor, start, right));
            start.countDown();
            one.join();
            two.join();
            check(concurrent.store.snapshot().isComplete(node.completionFlag()),
                    nodeId + " concurrent path did not complete");
            long winners = java.util.stream.Stream.of(left.get(), right.get())
                    .filter(value -> value != null && Set.of(
                            MechanicOutcome.Status.COMPLETED,
                            MechanicOutcome.Status.WORLD_RECOVERY_PENDING).contains(value.status()))
                    .count();
            check(winners == 1, nodeId + " concurrent winners=" + winners
                    + " outcomes=" + left.get() + "," + right.get());
            check(concurrent.artifacts.delivered.size() <= 1,
                    nodeId + " concurrency duplicated reward");
            scenarios++;
        }
        check(scenarios == 41 * 7, "scenario matrix incomplete: " + scenarios);
        System.out.println("V5 mechanics self-test PASS: 41 nodes, " + scenarios
                + " node scenarios, 38 exact operation adapters, RP04 ritual-owned.");
    }

    private static Harness harness(
            PhysicalPredicateAuthority authority,
            PhysicalPredicateAuthority.Node node,
            UUID actor,
            boolean prerequisites,
            boolean correct) throws Exception {
        Path directory = Files.createTempDirectory("observance-v5-mechanics-");
        V5ProgressStore store = V5ProgressStore.open(directory.resolve("progress.json"), authority);
        ArtifactStub artifacts = new ArtifactStub();
        AtomicInteger mutations = new AtomicInteger();
        V5MechanicsEngine engine = new V5MechanicsEngine(
                authority,
                store,
                flag -> prerequisites,
                (requested, requestedActor, trigger) -> correct
                        ? InMemoryWorldStateFactory.correct(requested, requestedActor)
                        : MechanicObservation.builder(requestedActor, requested.siteId()).build(),
                new MechanicPorts.WorldMutation() {
                    @Override
                    public void applyAfterLocalCommit(
                            PhysicalPredicateAuthority.Node requested,
                            UUID requestedActor,
                            MechanicObservation observation) {
                        mutations.incrementAndGet();
                    }

                    @Override
                    public void recoverCommitted(PhysicalPredicateAuthority.Node requested) {
                    }
                },
                artifacts,
                (requested, revision) -> { },
                (requestedActor, message) -> check(!containsAccepted(node, message),
                        node.nodeId() + " feedback exposed an accepted answer"));
        return new Harness(engine, store, artifacts, mutations);
    }

    private static void runConcurrent(
            V5MechanicsEngine engine,
            String nodeId,
            UUID actor,
            CountDownLatch start,
            AtomicReference<MechanicOutcome> result) {
        try {
            start.await();
            result.set(engine.evaluate(nodeId, actor, trigger(nodeId)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static Trigger trigger(String nodeId) {
        if (Set.of("LC02", "LC04", "A04", "WR01", "CW04", "BI05", "KV03", "KM02",
                "KS03", "KI01", "KB02").contains(nodeId)) {
            return Trigger.SIGN_SUBMIT;
        }
        if (Set.of("BI07", "HS06", "KM03").contains(nodeId)) {
            return Trigger.ROUTE_COMPLETE;
        }
        return "KO01".equals(nodeId) ? Trigger.SIGHTLINE_TIMER : Trigger.HANDLE;
    }

    private static String rewardId(PhysicalPredicateAuthority.Node node) {
        var rewards = InMemoryWorldStateFactory.rewardArtifactIds(node);
        return rewards.isEmpty() ? null : rewards.getFirst();
    }

    private static boolean containsAccepted(
            PhysicalPredicateAuthority.Node node, String feedback) {
        String normalized = com.observance.watcher.v5runtime.AnswerNormalizer.normalize(feedback);
        for (String answer : InMemoryWorldStateFactory.acceptedAnswers(node)) {
            if (normalized.contains(com.observance.watcher.v5runtime.AnswerNormalizer
                    .normalize(answer))) {
                return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Harness(
            V5MechanicsEngine engine,
            V5ProgressStore store,
            ArtifactStub artifacts,
            AtomicInteger mutations) {
    }

    private static final class ArtifactStub implements MechanicPorts.ArtifactDelivery {
        private final Set<String> preexisting = java.util.concurrent.ConcurrentHashMap.newKeySet();
        private final Set<String> delivered = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public MechanicItem template(String artifactId, UUID instanceId) {
            return new MechanicItem("PAPER", 1, Map.of(
                    MechanicItem.ARTIFACT_ID, artifactId,
                    MechanicItem.ARTIFACT_INSTANCE, instanceId.toString()), Optional.of(instanceId));
        }

        @Override
        public Set<UUID> scanInstances(String artifactId) {
            return preexisting.contains(artifactId)
                    ? Set.of(UUID.nameUUIDFromBytes(artifactId.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8))) : Set.of();
        }

        @Override
        public boolean deliverOrKeepEscrow(UUID actor, EscrowEntry pending) {
            return delivered.add(pending.artifactId());
        }
    }
}
