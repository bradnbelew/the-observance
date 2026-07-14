package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.PlayerProgress;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.container.ContainerAttempt.Status;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.ItemDisposition;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.Mode;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.ActorFacts;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.AsyncMirror;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.Clock;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.CommitEffects;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.Feedback;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.ExternalPrerequisites;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.ValidatedSnapshot;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.World;
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

/** Serialized local-primary transaction coordinator for all thirteen family-C predicates. */
public final class ContainerSolveService {
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(2);

    private final ContainerAuthorityContract contract;
    private final V5ProgressStore progress;
    private final SiteMutexes mutexes;
    private final World world;
    private final ActorFacts actors;
    private final ExternalPrerequisites externalPrerequisites;
    private final CommitEffects effects;
    private final AsyncMirror mirror;
    private final Feedback feedback;
    private final Clock clock;

    public ContainerSolveService(
            ContainerAuthorityContract contract,
            V5ProgressStore progress,
            SiteMutexes mutexes,
            World world,
            ActorFacts actors,
            ExternalPrerequisites externalPrerequisites,
            CommitEffects effects,
            AsyncMirror mirror,
            Feedback feedback,
            Clock clock) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.mutexes = Objects.requireNonNull(mutexes, "mutexes");
        this.world = Objects.requireNonNull(world, "world");
        this.actors = Objects.requireNonNull(actors, "actors");
        this.externalPrerequisites = Objects.requireNonNull(
                externalPrerequisites, "externalPrerequisites");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.mirror = Objects.requireNonNull(mirror, "mirror");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.clock = Objects.requireNonNull(clock, "clock");
        ContainerPredicateCoverage.validateAgainst(contract.authority());
        if (!progress.manifestSha256().equals(contract.authority().sha256())) {
            throw new IllegalArgumentException("container runtime/progress authority hashes differ");
        }
    }

    public ContainerSolveService(
            ContainerAuthorityContract contract,
            V5ProgressStore progress,
            SiteMutexes mutexes,
            World world,
            ActorFacts actors,
            CommitEffects effects,
            AsyncMirror mirror,
            Feedback feedback,
            Clock clock) {
        this(contract, progress, mutexes, world, actors, ExternalPrerequisites.none(),
                effects, mirror, feedback, clock);
    }

    public ContainerAuthorityContract contract() {
        return contract;
    }

    public boolean isComplete(String nodeId) {
        return progress.snapshot().isComplete(contract.rule(nodeId).completionFlag());
    }

    public Optional<String> firstMissingPrerequisite(String nodeId) {
        hydrateExternalPrerequisites(contract.rule(nodeId));
        return contract.firstMissingPrerequisite(nodeId, trueFlags(progress.snapshot()));
    }

    public boolean canModify(String nodeId) {
        hydrateExternalPrerequisites(contract.rule(nodeId));
        ProgressSnapshot snapshot = progress.snapshot();
        ContainerAuthorityContract.NodeRule rule = contract.rule(nodeId);
        return !snapshot.isComplete(rule.completionFlag())
                && contract.prerequisitesMet(nodeId, trueFlags(snapshot));
    }

    public ContainerAttempt evaluate(
            String nodeId,
            UUID actor,
            ContainerAuthorityContract.TriggerKind trigger,
            String component,
            int slot) {
        Objects.requireNonNull(actor, "actor");
        ContainerAuthorityContract.NodeRule rule = contract.rule(nodeId);
        Optional<SiteMutexes.Guard> acquired;
        try {
            acquired = mutexes.tryAcquire(rule.siteId(), LOCK_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return report(actor, ContainerAttempt.of(Status.BUSY, "The filing remains in use."));
        }
        if (acquired.isEmpty()) {
            return report(actor, ContainerAttempt.of(Status.BUSY, "The filing remains in use."));
        }
        try (SiteMutexes.Guard ignored = acquired.orElseThrow()) {
            hydrateExternalPrerequisites(rule);
            ProgressSnapshot before = progress.snapshot();
            if (before.isComplete(rule.completionFlag())) {
                return report(actor, ContainerAttempt.of(
                        Status.ALREADY_COMPLETE, "The filing is already sealed."));
            }
            Set<String> flags = trueFlags(before);
            if (!contract.prerequisitesMet(nodeId, flags)) {
                return report(actor, ContainerAttempt.of(
                        Status.LOCKED, "The preceding record is not yet complete."));
            }
            Set<String> held = heldBits(before, actor);
            boolean linked = actors.linked(actor);
            boolean handoff = actors.matchesHandoff(actor, "v5_ls05_bound");
            ContainerObservation observation = world.capture(
                    rule, actor, flags, held, linked, handoff);
            ContainerAttempt evaluated = contract.evaluate(
                    nodeId, trigger, component, slot, observation);
            if (evaluated.status() != Status.READY) {
                return report(actor, evaluated);
            }
            ContainerCommitPlan plan = evaluated.plan().orElseThrow();
            List<EscrowEntry> escrowEntries = escrowEntries(rule, actor, plan);
            boolean committed = progress.transact(editor -> {
                if (!editor.compareAndSetCompletion(rule.completionFlag(), false, true)) {
                    return false;
                }
                for (EscrowEntry entry : escrowEntries) {
                    editor.putEscrowOnce(entry);
                }
                return true;
            });
            if (!committed) {
                return report(actor, ContainerAttempt.of(
                        Status.ALREADY_COMPLETE, "The filing is already sealed."));
            }
            long committedRevision = progress.snapshot().revision();
            boolean recoveryPending = false;
            try {
                mirror.enqueue(rule, committedRevision);
            } catch (RuntimeException exception) {
                // Remote mirroring is asynchronous and can never block local world/gate projection.
                recoveryPending = true;
            }
            try {
                Set<String> delivered = world.applyAfterCommit(rule, actor, plan);
                markDelivered(delivered);
                Set<String> expectedDeliveries = new HashSet<>();
                plan.items().stream().filter(item -> item.mode() != Mode.HOLD_IN_FIXTURE)
                        .map(ItemDisposition::escrowId).forEach(expectedDeliveries::add);
                recoveryPending |= !delivered.containsAll(expectedDeliveries);
            } catch (Exception exception) {
                recoveryPending = true;
            }
            try {
                // Local completion drives gates immediately even if item/world recovery is pending.
                effects.applyAfterCommit(rule);
            } catch (Exception exception) {
                recoveryPending = true;
            }
            return report(actor, ContainerAttempt.of(
                    recoveryPending ? Status.RECOVERY_PENDING : Status.COMMITTED,
                    recoveryPending
                            ? "The record is safe; its physical readback is pending recovery."
                            : "The filing seals and the next record answers."));
        } catch (IOException exception) {
            return report(actor, ContainerAttempt.of(
                    Status.ERROR, "The filing refuses to move while its record is unsafe."));
        } catch (RuntimeException exception) {
            return report(actor, ContainerAttempt.of(
                    Status.ERROR, "The filing refuses an invalid state."));
        } catch (Exception exception) {
            return report(actor, ContainerAttempt.of(
                    Status.ERROR, "The filing cannot read its protected fixtures."));
        }
    }

    /** Claims LS06's key source or one CW07 cache item without completing the later filing. */
    public ContainerAttempt claimPortable(
            String nodeId, UUID actor, String component, int slot) {
        Objects.requireNonNull(actor, "actor");
        ContainerAuthorityContract.NodeRule rule = contract.rule(nodeId);
        Optional<ContainerAuthorityContract.ItemRequirement> required =
                contract.portableClaimRequirement(nodeId, component, slot);
        if (required.isEmpty()) {
            return report(actor, ContainerAttempt.of(Status.WRONG,
                    "That source does not release an item here."));
        }
        Optional<SiteMutexes.Guard> acquired;
        try {
            acquired = mutexes.tryAcquire(rule.siteId(), LOCK_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return report(actor, ContainerAttempt.of(Status.BUSY, "The source remains in use."));
        }
        if (acquired.isEmpty()) {
            return report(actor, ContainerAttempt.of(Status.BUSY, "The source remains in use."));
        }
        try (SiteMutexes.Guard ignored = acquired.orElseThrow()) {
            hydrateExternalPrerequisites(rule);
            ProgressSnapshot before = progress.snapshot();
            Set<String> flags = trueFlags(before);
            if (before.isComplete(rule.completionFlag())) {
                return report(actor, ContainerAttempt.of(
                        Status.ALREADY_COMPLETE, "The filing is already sealed."));
            }
            if (!contract.prerequisitesMet(nodeId, flags)) {
                return report(actor, ContainerAttempt.of(
                        Status.LOCKED, "The preceding record is not yet complete."));
            }
            boolean linked = actors.linked(actor);
            boolean handoff = actors.matchesHandoff(actor, "v5_ls05_bound");
            if (("CW07".equals(nodeId) && !linked)
                    || ("LS06".equals(nodeId) && !handoff)) {
                return report(actor, ContainerAttempt.of(
                        Status.LOCKED, "This source does not recognize that linked hand."));
            }
            ContainerObservation observation = world.capture(
                    rule, actor, flags, heldBits(before, actor), linked, handoff);
            ContainerItem item = observation.item(component, slot).orElse(null);
            ContainerAuthorityContract.ItemRequirement requirement = required.orElseThrow();
            if (!requirement.matches(item)
                    || observation.identityCount(requirement.identityType(),
                    requirement.identityId()) != 1
                    || observation.destinationCapacity() < 1) {
                return report(actor, ContainerAttempt.of(Status.WRONG, rule.wrongFeedback()));
            }
            String identity = requirement.identityId();
            String suffix = item.artifactInstance().map(UUID::toString)
                    .orElseGet(() -> item.fingerprintSha256().substring(0, 16));
            String escrowId = "container:" + nodeId + ":portable:" + identity + ':' + suffix;
            EscrowEntry existing = before.escrow().get(escrowId);
            if (existing != null && existing.intendedPlayer().filter(actor::equals).isEmpty()) {
                return report(actor, ContainerAttempt.of(
                        Status.LOCKED, "That evidence is already held for another linked hand."));
            }
            String heldBit = contract.heldBit(nodeId, identity);
            if (existing == null) {
                long now = clock.epochMillis();
                EscrowEntry pending = new EscrowEntry(
                        escrowId, identity, Optional.of(actor), rule.siteId(), slot,
                        item.fingerprintSha256(), 1, now, now, EscrowStatus.DELIVERY_PENDING,
                        Map.of("node_id", nodeId, "component", component,
                                "mode", "PORTABLE_CLAIM"));
                progress.transact(editor -> {
                    editor.putEscrowOnce(pending);
                    editor.addPlayerBit(actor, PlayerBitDomain.INSPECTION, heldBit);
                    return true;
                });
            }
            boolean delivered = world.applyPortableClaim(
                    rule, actor, component, slot, item, escrowId);
            if (delivered) {
                markDelivered(Set.of(escrowId));
            }
            return report(actor, ContainerAttempt.of(
                    delivered ? Status.COMMITTED : Status.RECOVERY_PENDING,
                    delivered
                            ? "The protected evidence passes into your custody."
                            : "The evidence is held for your next safe inventory recovery."));
        } catch (IOException exception) {
            return report(actor, ContainerAttempt.of(
                    Status.ERROR, "The source refuses to move while its record is unsafe."));
        } catch (Exception exception) {
            return report(actor, ContainerAttempt.of(
                    Status.ERROR, "The source cannot complete protected delivery."));
        }
    }

    /** Enable/restart hook. A committed local flag always wins and is reprojected idempotently. */
    public List<String> recoverCommitted() {
        ProgressSnapshot snapshot = progress.snapshot();
        List<String> failures = new ArrayList<>();
        for (String nodeId : ContainerPredicateCoverage.orderedNodeIds()) {
            ContainerAuthorityContract.NodeRule rule = contract.rule(nodeId);
            try {
                markDelivered(world.recoverCommitted(rule, snapshot));
                if (snapshot.isComplete(rule.completionFlag())) {
                    effects.applyAfterCommit(rule);
                    mirror.enqueue(rule, snapshot.revision());
                }
            } catch (Exception exception) {
                failures.add(nodeId + ": " + exception.getMessage());
            }
        }
        return List.copyOf(failures);
    }

    public void recoverPlayer(UUID playerId) throws Exception {
        markDelivered(world.recoverPlayer(playerId));
    }

    private List<EscrowEntry> escrowEntries(
            ContainerAuthorityContract.NodeRule rule, UUID actor, ContainerCommitPlan plan) {
        long now = clock.epochMillis();
        List<EscrowEntry> result = new ArrayList<>();
        for (ItemDisposition item : plan.items()) {
            ContainerItem image = item.existingItem().orElseGet(() -> new ContainerItem(
                    item.material(), 1, Map.of(
                    ContainerItem.ARTIFACT_ID, item.identityId(),
                    ContainerItem.ARTIFACT_ALIAS, item.identityId(),
                    ContainerItem.ARTIFACT_INSTANCE,
                    item.generatedInstance().orElseThrow().toString())));
            boolean delivery = item.mode() != Mode.HOLD_IN_FIXTURE;
            result.add(new EscrowEntry(
                    item.escrowId(), item.identityId(),
                    delivery ? Optional.of(actor) : Optional.empty(),
                    rule.siteId(), item.slot(), image.fingerprintSha256(), 1,
                    now, now, delivery ? EscrowStatus.DELIVERY_PENDING : EscrowStatus.HELD,
                    Map.of("node_id", rule.nodeId(), "component",
                            item.component().isBlank() ? "generated_reward" : item.component(),
                            "mode", item.mode().name(), "material", item.material(),
                            "instance", item.generatedInstance().map(UUID::toString)
                            .or(() -> image.artifactInstance().map(UUID::toString)).orElse("none"))));
        }
        return List.copyOf(result);
    }

    private void markDelivered(Set<String> escrowIds) throws IOException {
        if (escrowIds.isEmpty()) {
            return;
        }
        long now = clock.epochMillis();
        progress.transact(editor -> {
            ProgressSnapshot snapshot = progress.snapshot();
            for (String escrowId : escrowIds) {
                EscrowEntry current = snapshot.escrow().get(escrowId);
                if (current == null || current.status() == EscrowStatus.DELIVERED) {
                    continue;
                }
                if (current.status() != EscrowStatus.DELIVERY_PENDING
                        && current.status() != EscrowStatus.RETURN_PENDING) {
                    throw new IllegalStateException("non-delivery escrow cannot be delivered: " + escrowId);
                }
                EscrowEntry delivered = new EscrowEntry(
                        current.escrowId(), current.artifactId(), current.intendedPlayer(),
                        current.sourceSiteId(), current.sourceSlot(), current.itemFingerprintSha256(),
                        current.amount(), current.createdAtEpochMillis(),
                        Math.max(now, current.updatedAtEpochMillis()), EscrowStatus.DELIVERED,
                        current.metadata());
                editor.transitionEscrow(escrowId, current.status(), delivered);
            }
            return true;
        });
    }

    private ContainerAttempt report(UUID actor, ContainerAttempt attempt) {
        try {
            feedback.send(actor, attempt.message());
        } catch (RuntimeException ignored) {
            // Player messaging cannot change an already-durable result.
        }
        return attempt;
    }

    private boolean hydrateExternalPrerequisites(ContainerAuthorityContract.NodeRule rule) {
        Optional<ValidatedSnapshot> candidate;
        try {
            candidate = externalPrerequisites.current();
        } catch (RuntimeException exception) {
            return false;
        }
        if (candidate.isEmpty()) {
            return false;
        }
        ValidatedSnapshot snapshot = candidate.orElseThrow();
        if (!PhysicalPredicateAuthority.CAMPAIGN_VERSION.equals(snapshot.campaignVersion())
                || !contract.authority().sha256().equals(snapshot.authoritySha256())) {
            return false;
        }
        Set<String> eligible = new HashSet<>();
        for (String prerequisite : rule.authorityNode().prerequisites()) {
            if (contract.authority().findByCompletionFlag(prerequisite).isEmpty()
                    && Boolean.TRUE.equals(snapshot.flags().get(prerequisite))) {
                eligible.add(prerequisite);
            }
        }
        if (eligible.isEmpty()) {
            return false;
        }
        try {
            return progress.transact(editor -> {
                boolean changed = false;
                for (String flag : eligible) changed |= editor.setBooleanTrue(flag);
                return changed;
            });
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static Set<String> trueFlags(ProgressSnapshot snapshot) {
        Set<String> result = new HashSet<>();
        snapshot.booleans().forEach((flag, value) -> {
            if (Boolean.TRUE.equals(value)) {
                result.add(flag);
            }
        });
        return Set.copyOf(result);
    }

    private static Set<String> heldBits(ProgressSnapshot snapshot, UUID actor) {
        PlayerProgress player = snapshot.players().get(actor.toString());
        return player == null ? Set.of() : player.inspections();
    }
}
