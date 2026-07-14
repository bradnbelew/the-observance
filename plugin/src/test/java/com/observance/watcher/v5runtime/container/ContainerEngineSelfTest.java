package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.container.ContainerAttempt.Status;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract.ItemRequirement;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract.NodeRule;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract.TriggerKind;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.ItemDisposition;
import com.observance.watcher.v5runtime.container.ContainerCommitPlan.Mode;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.ActorFacts;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.ValidatedSnapshot;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts.World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** Main-driven exhaustive family-C transaction matrix; intentionally independent of a live server. */
public final class ContainerEngineSelfTest {
    private static final PhysicalPredicateAuthority AUTHORITY =
            PhysicalPredicateAuthorityLoader.loadDefault();
    private static final ContainerAuthorityContract CONTRACT =
            new ContainerAuthorityContract(AUTHORITY);

    private ContainerEngineSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        authorityCoverage();
        externalPrerequisiteValidation();
        playerCountTable();
        wrongPartialAndDuplicateTable();
        fullInventoryTable();
        portableClaimTable();
        disconnectAndRestartRecovery();
        concurrentDoubleOperation();
        System.out.println("ContainerEngineSelfTest: 13 nodes; 1/2/7-player, wrong, partial, "
                + "duplicate, replay, restart, disconnect, full-inventory, and concurrency matrix passed");
    }

    private static void authorityCoverage() {
        check(ContainerPredicateCoverage.implementedNodeIds().size() == 13,
                "container coverage count");
        ContainerPredicateCoverage.validateAgainst(AUTHORITY);
        check(CONTRACT.rules().keySet().equals(ContainerPredicateCoverage.implementedNodeIds()),
                "contract/coverage exact parity");
        NodeRule kv02 = CONTRACT.rule("KV02");
        check(kv02.acceptedIdentities().size() == 36, "KV02 36-item manifest");
        check(kv02.stagingComponents().equals(Set.of("input_lot_a", "input_lot_b")),
                "KV02 exact two-lot boundary");
        check(ContainerTriggerBindings.requiredSyntheticComponents().equals(Map.of(
                "CW07", "cache_seal", "HS02", "housing_latch")),
                "seal/latch controls have exact synthesized bindings");
        ContainerItem ambiguous = new ContainerItem("PAPER", 1, Map.of(
                ContainerItem.ARTIFACT_ID, "orientation_key",
                ContainerItem.ARTIFACT_ALIAS, "orientation_key",
                ContainerItem.ARTIFACT_INSTANCE, UUID.randomUUID().toString(),
                ContainerItem.EVIDENCE_ID, "cw07_purchase_receipt"));
        check(!ambiguous.hasUniqueArtifactIdentity(),
                "artifact/evidence dual identity fails closed");
    }

    private static void externalPrerequisiteValidation() throws Exception {
        Harness harness = Harness.create("LS06", 1, false);
        String flag = "v5_ls05_bound";
        check(!harness.store.snapshot().isComplete(flag), "external flag starts local-false");
        harness.externalAuthoritySha = "0".repeat(64);
        check(!harness.service.canModify("LS06"), "wrong authority hash fails closed");
        check(!harness.store.snapshot().isComplete(flag), "invalid snapshot cannot hydrate local state");
        harness.externalAuthoritySha = AUTHORITY.sha256();
        harness.externalCampaign = "v4";
        check(!harness.service.canModify("LS06"), "wrong campaign fails closed");
        harness.externalCampaign = PhysicalPredicateAuthority.CAMPAIGN_VERSION;
        check(harness.service.canModify("LS06"), "validated external prerequisite hydrates");
        check(harness.store.snapshot().isComplete(flag), "validated external fact persists locally");
        harness.externalFlags.put(flag, false);
        harness.externalAvailable = false;
        check(harness.service.canModify("LS06"), "remote false/outage cannot revoke local truth");
        harness.externalFlags.put(harness.rule.completionFlag(), true);
        check(!harness.store.snapshot().isComplete(harness.rule.completionFlag()),
                "remote snapshot cannot authorize a physical completion");
    }

    private static void playerCountTable() throws Exception {
        for (int playerCount : List.of(1, 2, 7)) {
            for (String nodeId : ContainerPredicateCoverage.orderedNodeIds()) {
                Harness harness = Harness.create(nodeId, playerCount);
                harness.populateValid();
                harness.authorizeHeldEvidence();
                UUID actor = harness.players.get(0);
                ContainerAttempt result = harness.solve(actor);
                check(result.status() == Status.COMMITTED,
                        nodeId + '/' + playerCount + " should commit, got " + result.status());
                check(harness.store.snapshot().isComplete(harness.rule.completionFlag()),
                        nodeId + " completion flag durable");
                check(harness.effects.equals(Set.of(nodeId)), nodeId + " effects once");
                check(harness.mirrors.equals(List.of(nodeId)), nodeId + " mirror once");
                if ("RP02".equals(nodeId)) {
                    long held = harness.store.snapshot().escrow().values().stream()
                            .filter(entry -> entry.status() == EscrowStatus.HELD).count();
                    check(held == 9, "RP02 exact nine-item escrow");
                }
                long revision = harness.store.snapshot().revision();
                for (UUID player : harness.players) {
                    ContainerAttempt replay = harness.solve(player);
                    check(replay.status() == Status.ALREADY_COMPLETE,
                            nodeId + " replay must be idempotent");
                }
                check(harness.store.snapshot().revision() == revision,
                        nodeId + " replay cannot mutate progress");
                harness.assertNoDuplicateIdentities();
            }
        }
    }

    private static void wrongPartialAndDuplicateTable() throws Exception {
        for (String nodeId : ContainerPredicateCoverage.orderedNodeIds()) {
            Harness partial = Harness.create(nodeId, 2);
            partial.populateValid();
            partial.authorizeHeldEvidence();
            partial.removeOneRequiredItem();
            long beforeRevision = partial.store.snapshot().revision();
            Map<String, Map<Integer, ContainerItem>> beforeWorld = partial.world.copyInventories();
            ContainerAttempt rejected = partial.solve(partial.players.get(0));
            check(rejected.status() == Status.WRONG, nodeId + " partial arrangement rejected");
            check(!partial.store.snapshot().isComplete(partial.rule.completionFlag()),
                    nodeId + " partial cannot set flag");
            check(partial.store.snapshot().revision() == beforeRevision,
                    nodeId + " partial cannot mutate progress");
            check(partial.world.copyInventories().equals(beforeWorld),
                    nodeId + " partial/wrong items are atomically retained");

            Harness duplicate = Harness.create(nodeId, 2);
            duplicate.populateValid();
            duplicate.authorizeHeldEvidence();
            ContainerItem duplicated = duplicate.firstRequiredItem();
            duplicate.world.playerItems.get(duplicate.players.get(1)).add(duplicated);
            ContainerAttempt duplicateResult = duplicate.solve(duplicate.players.get(0));
            check(duplicateResult.status() == Status.WRONG,
                    nodeId + " duplicate identity must fail closed");
            check(!duplicate.store.snapshot().isComplete(duplicate.rule.completionFlag()),
                    nodeId + " duplicate cannot set flag");

            ContainerItem ordinary = new ContainerItem("DIRT", 1, Map.of());
            SlotTarget target = duplicate.firstManagedTarget();
            check(!CONTRACT.allowsInsertion(nodeId, target.component(), target.slot(), ordinary),
                    nodeId + " ordinary wrong input denied before vanilla mutation");
        }
    }

    private static void fullInventoryTable() throws Exception {
        for (String nodeId : List.of("A09", "HS01")) {
            Harness harness = Harness.create(nodeId, 1);
            harness.populateValid();
            UUID actor = harness.players.get(0);
            harness.world.capacity.put(actor, 0);
            ContainerAttempt result = harness.solve(actor);
            check(result.status() == Status.WRONG, nodeId + " exact capacity predicate");
            check(harness.firstRequiredItem().equals(harness.firstWorldItem()),
                    nodeId + " full inventory leaves exact source item in place");
        }

        for (String nodeId : List.of("LS06", "HS07")) {
            Harness harness = Harness.create(nodeId, 1);
            harness.populateValid();
            UUID actor = harness.players.get(0);
            harness.world.capacity.put(actor, 0);
            ContainerAttempt result = harness.solve(actor);
            check(result.status() == Status.RECOVERY_PENDING,
                    nodeId + " full inventory commits to protected recovery");
            long pending = harness.store.snapshot().escrow().values().stream()
                    .filter(entry -> entry.status() == EscrowStatus.DELIVERY_PENDING).count();
            check(pending >= 1, nodeId + " pending delivery durable");
            harness.world.capacity.put(actor, 4);
            harness.service.recoverPlayer(actor);
            check(harness.store.snapshot().escrow().values().stream()
                            .noneMatch(entry -> entry.status() == EscrowStatus.DELIVERY_PENDING),
                    nodeId + " pending delivery recovers without loss");
            harness.assertNoDuplicateIdentities();
        }

        Harness portable = Harness.create("LS06", 1);
        portable.populatePortableSources();
        UUID actor = portable.players.get(0);
        portable.world.capacity.put(actor, 0);
        ContainerAttempt result = portable.service.claimPortable(
                "LS06", actor, "orientation_key_source", 13);
        check(result.status() == Status.WRONG, "portable claim requires destination capacity");
        check(portable.world.item("orientation_key_source", 13) != null,
                "full portable claim retains source");
    }

    private static void portableClaimTable() throws Exception {
        Harness ls06 = Harness.create("LS06", 1);
        ls06.populatePortableSources();
        UUID actor = ls06.players.get(0);
        ContainerAttempt claim = ls06.service.claimPortable(
                "LS06", actor, "orientation_key_source", 13);
        check(claim.status() == Status.COMMITTED, "LS06 protected source claim");
        ContainerItem key = ls06.world.takePlayerItem(actor, "orientation_key");
        ls06.world.put("orientation_file", 13, key);
        check(ls06.solve(actor).status() == Status.COMMITTED, "LS06 file after source handoff");
        check(ls06.world.countIdentity("artifact", "orientation_key") == 1,
                "LS06 same UUID-tagged key returned once");

        Harness cw07 = Harness.create("CW07", 1);
        cw07.populatePortableSources();
        UUID linked = cw07.players.get(0);
        Map<String, Integer> filingSlots = Map.of(
                "cw07_genuine_filter", 11,
                "cw07_purchase_receipt", 13,
                "cw07_discipline_drafts", 15);
        for (Map.Entry<String, Integer> entry : filingSlots.entrySet()) {
            ContainerAttempt itemClaim = cw07.service.claimPortable(
                    "CW07", linked, "cache", entry.getValue());
            check(itemClaim.status() == Status.COMMITTED, "CW07 claim " + entry.getKey());
            cw07.world.put("filing_barrel", entry.getValue(),
                    cw07.world.takePlayerItem(linked, entry.getKey()));
        }
        check(cw07.solve(linked).status() == Status.COMMITTED,
                "CW07 same linked player held all evidence");

        Harness wrongActor = Harness.create("CW07", 2);
        wrongActor.populateValid();
        UUID first = wrongActor.players.get(0);
        wrongActor.authorizeHeldEvidence(first);
        ContainerAttempt secondFiles = wrongActor.solve(wrongActor.players.get(1));
        check(secondFiles.status() == Status.WRONG,
                "CW07 held evidence is actor-bound, not group-global");
    }

    private static void disconnectAndRestartRecovery() throws Exception {
        Harness disconnect = Harness.create("LS06", 1);
        disconnect.populatePortableSources();
        UUID actor = disconnect.players.get(0);
        disconnect.world.connected.remove(actor);
        ContainerAttempt pending = disconnect.service.claimPortable(
                "LS06", actor, "orientation_key_source", 13);
        check(pending.status() == Status.RECOVERY_PENDING,
                "disconnect leaves portable custody pending");
        check(disconnect.world.item("orientation_key_source", 13) != null,
                "disconnect cannot delete source");
        disconnect.world.connected.add(actor);
        disconnect.service.recoverCommitted();
        disconnect.service.recoverPlayer(actor);
        check(disconnect.world.item("orientation_key_source", 13) == null,
                "restart recovery removes source only after durable delivery");
        check(disconnect.world.countIdentity("artifact", "orientation_key") == 1,
                "disconnect recovery no duplicate");

        Harness restart = Harness.create("HS07", 1);
        restart.populateValid();
        restart.world.failAfterLocalCommit = true;
        ContainerAttempt committed = restart.solve(restart.players.get(0));
        check(committed.status() == Status.RECOVERY_PENDING,
                "post-CAS world failure is recoverable, not rolled back");
        check(restart.store.snapshot().isComplete(restart.rule.completionFlag()),
                "restart flag committed before theater/items");
        check(restart.effects.contains("HS07"),
                "local completion projects gate/effects despite item recovery interruption");
        restart.world.failAfterLocalCommit = false;
        V5ProgressStore reopened = V5ProgressStore.open(restart.progressPath, AUTHORITY);
        ContainerSolveService recoveredService = restart.newService(reopened);
        check(recoveredService.recoverCommitted().isEmpty(), "restart reconciliation succeeds");
        recoveredService.recoverPlayer(restart.players.get(0));
        check(reopened.snapshot().escrow().values().stream()
                        .noneMatch(entry -> entry.status() == EscrowStatus.DELIVERY_PENDING),
                "restart completes pending key and reward custody");
        restart.assertNoDuplicateIdentities();

        Harness mirrorOutage = Harness.create("A03", 1);
        mirrorOutage.populateValid();
        mirrorOutage.failMirror = true;
        ContainerAttempt localWins = mirrorOutage.solve(mirrorOutage.players.get(0));
        check(localWins.status() == Status.RECOVERY_PENDING,
                "remote mirror outage is reported without undoing local solve");
        check(mirrorOutage.store.snapshot().isComplete(mirrorOutage.rule.completionFlag())
                        && mirrorOutage.effects.contains("A03"),
                "remote outage cannot strand local latch/gate projection");
        mirrorOutage.failMirror = false;
        check(mirrorOutage.service.recoverCommitted().isEmpty(),
                "restart reconciliation re-enqueues a missed remote mirror");
        check(mirrorOutage.mirrors.equals(List.of("A03")),
                "missed remote mirror is recovered exactly once");
    }

    private static void concurrentDoubleOperation() throws Exception {
        Harness harness = Harness.create("A09", 7);
        harness.populateValid();
        var executor = Executors.newFixedThreadPool(7);
        try {
            List<Callable<ContainerAttempt>> calls = harness.players.stream()
                    .<Callable<ContainerAttempt>>map(player -> () -> harness.solve(player)).toList();
            List<Future<ContainerAttempt>> futures = executor.invokeAll(calls);
            List<Status> statuses = new ArrayList<>();
            for (Future<ContainerAttempt> future : futures) statuses.add(future.get().status());
            check(statuses.stream().filter(status -> status == Status.COMMITTED).count() == 1,
                    "concurrent source claim has one winner: " + statuses);
            check(statuses.stream().allMatch(status -> status == Status.COMMITTED
                            || status == Status.ALREADY_COMPLETE),
                    "concurrent losers are replay-safe: " + statuses);
            check(harness.world.countIdentity("artifact", "witness_spool") == 1,
                    "concurrent claim cannot duplicate spool");
            check(harness.mirrors.size() == 1, "concurrent completion mirrors once");
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class Harness {
        private final NodeRule rule;
        private final List<UUID> players;
        private final Path progressPath;
        private final InMemoryWorld world;
        private final Set<UUID> linked = new HashSet<>();
        private final Set<UUID> handoff = new HashSet<>();
        private final Set<String> effects = new LinkedHashSet<>();
        private final List<String> mirrors = new ArrayList<>();
        private final AtomicLong clock = new AtomicLong(1_000_000);
        private final Map<String, Boolean> externalFlags = new HashMap<>();
        private boolean externalAvailable = true;
        private String externalCampaign = PhysicalPredicateAuthority.CAMPAIGN_VERSION;
        private String externalAuthoritySha = AUTHORITY.sha256();
        private boolean failMirror;
        private V5ProgressStore store;
        private ContainerSolveService service;

        private Harness(NodeRule rule, List<UUID> players, Path progressPath,
                        V5ProgressStore store, InMemoryWorld world) {
            this.rule = rule;
            this.players = players;
            this.progressPath = progressPath;
            this.store = store;
            this.world = world;
            linked.addAll(players);
            handoff.addAll(players);
            players.forEach(player -> {
                world.playerItems.put(player, new ArrayList<>());
                world.capacity.put(player, 36);
                world.connected.add(player);
            });
            service = newService(store);
        }

        private static Harness create(String nodeId, int playerCount) throws Exception {
            return create(nodeId, playerCount, true);
        }

        private static Harness create(String nodeId, int playerCount, boolean hydrate)
                throws Exception {
            NodeRule rule = CONTRACT.rule(nodeId);
            Path directory = Files.createTempDirectory("observance-container-" + nodeId + '-');
            Path progressPath = directory.resolve("progress.json");
            V5ProgressStore store = V5ProgressStore.open(progressPath, AUTHORITY);
            List<UUID> players = new ArrayList<>();
            for (int index = 0; index < playerCount; index++) players.add(UUID.randomUUID());
            Harness harness = new Harness(rule, List.copyOf(players), progressPath, store,
                    new InMemoryWorld());
            harness.setPrerequisites();
            if (hydrate) {
                check(harness.service.canModify(nodeId), nodeId + " prerequisites hydrate");
            }
            return harness;
        }

        private ContainerSolveService newService(V5ProgressStore value) {
            ActorFacts facts = new ActorFacts() {
                @Override
                public boolean linked(UUID actor) {
                    return linked.contains(actor);
                }

                @Override
                public boolean matchesHandoff(UUID actor, String sourceFlag) {
                    return "v5_ls05_bound".equals(sourceFlag) && handoff.contains(actor);
                }
            };
            return new ContainerSolveService(CONTRACT, value, new SiteMutexes(), world, facts,
                    () -> externalAvailable
                            ? Optional.of(new ValidatedSnapshot(
                            externalCampaign, externalAuthoritySha, externalFlags))
                            : Optional.empty(),
                    node -> effects.add(node.nodeId()),
                    (node, revision) -> {
                        if (failMirror) throw new IllegalStateException("simulated mirror outage");
                        mirrors.add(node.nodeId());
                    },
                    (actor, message) -> { }, clock::incrementAndGet);
        }

        private void setPrerequisites() throws IOException {
            for (String flag : rule.authorityNode().prerequisites()) setFlag(flag);
            if ("HS07".equals(rule.nodeId())) {
                for (String flag : List.of("v5_hs02_installed", "v5_hs03_lamps",
                        "v5_hs04_pressure", "v5_hs05_dials", "v5_hs06_passage")) setFlag(flag);
            }
        }

        private void setFlag(String flag) throws IOException {
            if (store.snapshot().isComplete(flag)) return;
            Optional<PhysicalPredicateAuthority.Node> physical = AUTHORITY.findByCompletionFlag(flag);
            if (physical.isPresent()) {
                store.compareAndSetCompletion(flag, false, true);
            } else {
                externalFlags.put(flag, true);
            }
        }

        private void populateValid() {
            rule.exactSlots().forEach((component, slots) -> slots.forEach((slot, requirement) ->
                    world.put(component, slot, item(requirement))));
            if ("BI03".equals(rule.nodeId())) world.rotations.put("time_dial", 7);
            if ("KV02".equals(rule.nodeId())) {
                for (String component : rule.classSlots().keySet()) {
                    List<Integer> slots = rule.classSlots().get(component).stream().sorted().toList();
                    List<ItemRequirement> items = rule.acceptedIdentities().values().stream()
                            .filter(requirement -> rule.classValues().get(component)
                                    .equals(requirement.pdc().get("v5_sort_class")))
                            .sorted(Comparator.comparing(ItemRequirement::identityId)).toList();
                    check(slots.size() == items.size(), "KV02 fixture count " + component);
                    for (int index = 0; index < slots.size(); index++) {
                        world.put(component, slots.get(index), item(items.get(index)));
                    }
                }
            }
        }

        private void populatePortableSources() {
            rule.portableClaims().forEach((component, slots) -> slots.forEach((slot, requirement) ->
                    world.put(component, slot, item(requirement))));
        }

        private void authorizeHeldEvidence() throws IOException {
            authorizeHeldEvidence(players.get(0));
        }

        private void authorizeHeldEvidence(UUID actor) throws IOException {
            if (!"CW07".equals(rule.nodeId())) return;
            store.transact(editor -> {
                rule.acceptedIdentities().values().forEach(requirement -> editor.addPlayerBit(
                        actor, PlayerBitDomain.INSPECTION,
                        CONTRACT.heldBit(rule.nodeId(), requirement.identityId())));
                return true;
            });
        }

        private ContainerAttempt solve(UUID actor) {
            String component = rule.triggerComponents().iterator().next();
            int slot = rule.solveTrigger() == TriggerKind.SOURCE_CLAIM
                    ? rule.exactSlots().get(component).keySet().iterator().next() : -1;
            return service.evaluate(rule.nodeId(), actor, rule.solveTrigger(), component, slot);
        }

        private void removeOneRequiredItem() {
            if (!rule.exactSlots().isEmpty()) {
                String component = rule.exactSlots().keySet().iterator().next();
                int slot = rule.exactSlots().get(component).keySet().iterator().next();
                world.inventory(component).remove(slot);
                return;
            }
            String component = rule.classSlots().keySet().iterator().next();
            int slot = rule.classSlots().get(component).iterator().next();
            world.inventory(component).remove(slot);
        }

        private ContainerItem firstRequiredItem() {
            if (!rule.exactSlots().isEmpty()) {
                String component = rule.exactSlots().keySet().iterator().next();
                int slot = rule.exactSlots().get(component).keySet().iterator().next();
                return world.item(component, slot);
            }
            return world.allWorldItems().get(0);
        }

        private ContainerItem firstWorldItem() {
            return world.allWorldItems().get(0);
        }

        private SlotTarget firstManagedTarget() {
            if (!rule.exactSlots().isEmpty()) {
                String component = rule.exactSlots().keySet().iterator().next();
                return new SlotTarget(component,
                        rule.exactSlots().get(component).keySet().iterator().next());
            }
            String component = rule.classSlots().keySet().iterator().next();
            return new SlotTarget(component, rule.classSlots().get(component).iterator().next());
        }

        private void assertNoDuplicateIdentities() {
            Map<String, Integer> counts = world.allIdentityCounts();
            counts.forEach((identity, count) -> check(count == 1,
                    rule.nodeId() + " duplicated " + identity + " count=" + count));
        }

        private static ContainerItem item(ItemRequirement requirement) {
            Map<String, String> pdc = new LinkedHashMap<>(requirement.pdc());
            if (pdc.containsKey(ContainerItem.ARTIFACT_ID)) {
                pdc.put(ContainerItem.ARTIFACT_ALIAS, pdc.get(ContainerItem.ARTIFACT_ID));
                pdc.put(ContainerItem.ARTIFACT_INSTANCE, UUID.randomUUID().toString());
            }
            return new ContainerItem(requirement.material(), 1, pdc);
        }
    }

    private static final class InMemoryWorld implements World {
        private final Map<String, Map<Integer, ContainerItem>> inventories = new LinkedHashMap<>();
        private final Map<String, Integer> rotations = new HashMap<>();
        private final Map<UUID, List<ContainerItem>> playerItems = new HashMap<>();
        private final Map<UUID, Integer> capacity = new HashMap<>();
        private final Set<UUID> connected = new HashSet<>();
        private final Map<String, Pending> pending = new LinkedHashMap<>();
        private final Set<String> latched = new HashSet<>();
        private boolean failAfterLocalCommit;

        @Override
        public synchronized ContainerObservation capture(
                NodeRule rule, UUID actor, Set<String> trueFlags, Set<String> heldEvidenceBits,
                boolean linked, boolean handoffMatch) {
            return new ContainerObservation(rule.nodeId(), actor, copyInventories(), rotations,
                    allIdentityCounts(), trueFlags, heldEvidenceBits,
                    capacity.getOrDefault(actor, 0), linked, handoffMatch);
        }

        @Override
        public synchronized Set<String> applyAfterCommit(
                NodeRule rule, UUID actor, ContainerCommitPlan plan) throws Exception {
            if (failAfterLocalCommit) throw new IOException("simulated post-CAS interruption");
            if (plan.latchInventories()) latched.add(rule.nodeId());
            Set<String> delivered = new LinkedHashSet<>();
            for (ItemDisposition disposition : plan.items()) {
                if (disposition.mode() == Mode.HOLD_IN_FIXTURE) {
                    check(disposition.existingItem().orElseThrow().equals(
                                    item(disposition.component(), disposition.slot())),
                            "held disposition identity");
                    continue;
                }
                Pending created;
                if (disposition.mode() == Mode.DELIVER_NEW_ARTIFACT_TO_ACTOR) {
                    Map<String, String> pdc = Map.of(
                            ContainerItem.ARTIFACT_ID, disposition.identityId(),
                            ContainerItem.ARTIFACT_ALIAS, disposition.identityId(),
                            ContainerItem.ARTIFACT_INSTANCE,
                            disposition.generatedInstance().orElseThrow().toString());
                    created = new Pending(actor, "", -1,
                            new ContainerItem(disposition.material(), 1, pdc));
                } else {
                    created = new Pending(actor, disposition.component(), disposition.slot(),
                            disposition.existingItem().orElseThrow());
                }
                Pending previous = pending.putIfAbsent(disposition.escrowId(), created);
                check(previous == null || previous.equals(created), "pending identity collision");
                if (deliver(disposition.escrowId())) delivered.add(disposition.escrowId());
            }
            return Set.copyOf(delivered);
        }

        @Override
        public synchronized boolean applyPortableClaim(
                NodeRule rule, UUID actor, String component, int slot, ContainerItem item,
                String progressEscrowId) {
            Pending created = new Pending(actor, component, slot, item);
            Pending previous = pending.putIfAbsent(progressEscrowId, created);
            check(previous == null || previous.equals(created), "portable pending collision");
            return deliver(progressEscrowId);
        }

        @Override
        public synchronized Set<String> recoverCommitted(NodeRule rule, ProgressSnapshot progress) {
            if (progress.isComplete(rule.completionFlag())) latched.add(rule.nodeId());
            Set<String> delivered = new LinkedHashSet<>();
            for (EscrowEntry entry : progress.escrow().values()) {
                if (!rule.nodeId().equals(entry.metadata().get("node_id"))
                        || entry.status() == EscrowStatus.HELD
                        || entry.status() == EscrowStatus.DELIVERED
                        || entry.intendedPlayer().isEmpty()) continue;
                if (!pending.containsKey(entry.escrowId())) {
                    String component = entry.metadata().getOrDefault("component", "");
                    ContainerItem image;
                    if (entry.sourceSlot() < 0) {
                        image = new ContainerItem(entry.metadata().get("material"), 1, Map.of(
                                ContainerItem.ARTIFACT_ID, entry.artifactId(),
                                ContainerItem.ARTIFACT_ALIAS, entry.artifactId(),
                                ContainerItem.ARTIFACT_INSTANCE, entry.metadata().get("instance")));
                    } else {
                        image = item(component, entry.sourceSlot());
                        if (image == null || !entry.itemFingerprintSha256()
                                .equals(image.fingerprintSha256())) continue;
                    }
                    pending.put(entry.escrowId(), new Pending(entry.intendedPlayer().orElseThrow(),
                            component, entry.sourceSlot(), image));
                }
                if (deliver(entry.escrowId())) delivered.add(entry.escrowId());
            }
            return Set.copyOf(delivered);
        }

        @Override
        public synchronized Set<String> recoverPlayer(UUID playerId) {
            Set<String> delivered = new LinkedHashSet<>();
            for (Map.Entry<String, Pending> entry : List.copyOf(pending.entrySet())) {
                if (entry.getValue().actor().equals(playerId) && deliver(entry.getKey())) {
                    delivered.add(entry.getKey());
                }
            }
            return Set.copyOf(delivered);
        }

        private boolean deliver(String escrowId) {
            Pending value = pending.get(escrowId);
            if (value == null) return true;
            if (!connected.contains(value.actor()) || capacity.getOrDefault(value.actor(), 0) < 1) {
                return false;
            }
            if (value.slot() >= 0) {
                ContainerItem source = item(value.component(), value.slot());
                if (source == null || !source.equals(value.item())) return false;
                inventory(value.component()).remove(value.slot());
            }
            playerItems.computeIfAbsent(value.actor(), ignored -> new ArrayList<>()).add(value.item());
            capacity.compute(value.actor(), (ignored, count) -> (count == null ? 0 : count) - 1);
            pending.remove(escrowId);
            return true;
        }

        private void put(String component, int slot, ContainerItem item) {
            ContainerItem previous = inventory(component).put(slot, item);
            check(previous == null, "test fixture slot collision " + component + ':' + slot);
        }

        private Map<Integer, ContainerItem> inventory(String component) {
            return inventories.computeIfAbsent(component, ignored -> new LinkedHashMap<>());
        }

        private ContainerItem item(String component, int slot) {
            return inventory(component).get(slot);
        }

        private ContainerItem takePlayerItem(UUID actor, String identityId) {
            List<ContainerItem> items = playerItems.get(actor);
            for (int index = 0; index < items.size(); index++) {
                ContainerItem item = items.get(index);
                if (item.artifactId().filter(identityId::equals).isPresent()
                        || item.evidenceId().filter(identityId::equals).isPresent()) {
                    capacity.merge(actor, 1, Integer::sum);
                    return items.remove(index);
                }
            }
            throw new AssertionError("player does not hold " + identityId);
        }

        private Map<String, Map<Integer, ContainerItem>> copyInventories() {
            Map<String, Map<Integer, ContainerItem>> copied = new LinkedHashMap<>();
            inventories.forEach((component, slots) -> copied.put(component, Map.copyOf(slots)));
            return Map.copyOf(copied);
        }

        private List<ContainerItem> allWorldItems() {
            return inventories.values().stream().flatMap(slots -> slots.values().stream()).toList();
        }

        private Map<String, Integer> allIdentityCounts() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            allWorldItems().forEach(item -> addCount(item, counts));
            playerItems.values().forEach(items -> items.forEach(item -> addCount(item, counts)));
            return Map.copyOf(counts);
        }

        private int countIdentity(String type, String id) {
            return allIdentityCounts().getOrDefault(type + ':' + id, 0);
        }

        private static void addCount(ContainerItem item, Map<String, Integer> counts) {
            item.artifactId().ifPresent(id -> counts.merge("artifact:" + id, 1, Integer::sum));
            item.evidenceId().ifPresent(id -> counts.merge("evidence:" + id, 1, Integer::sum));
        }

        private record Pending(UUID actor, String component, int slot, ContainerItem item) { }
    }

    private record SlotTarget(String component, int slot) { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
