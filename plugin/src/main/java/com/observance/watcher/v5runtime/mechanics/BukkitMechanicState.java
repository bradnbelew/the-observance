package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player transient facts; reset paths never touch durable story choices or solved flags. */
public final class BukkitMechanicState implements BukkitLiveFacts {
    private final ConcurrentHashMap<Key, Facts> facts = new ConcurrentHashMap<>();

    public void answer(UUID actor, String nodeId, String answer) {
        state(actor, nodeId).answer = answer;
    }

    public void handle(UUID actor, String nodeId, String component) {
        state(actor, nodeId).handle = component;
    }

    public void viewSide(UUID actor, String nodeId, String side) {
        state(actor, nodeId).viewSide = side;
    }

    public void booleanFact(UUID actor, String nodeId, String key, boolean value) {
        state(actor, nodeId).booleans.put(key, value);
    }

    public void integerFact(UUID actor, String nodeId, String key, int value) {
        state(actor, nodeId).integers.put(key, value);
    }

    public void stringFact(UUID actor, String nodeId, String key, String value) {
        state(actor, nodeId).strings.put(key, value);
    }

    public void stringList(UUID actor, String nodeId, String key, List<String> values) {
        state(actor, nodeId).lists.put(key, List.copyOf(values));
    }

    public void operated(UUID actor, String nodeId, String component) {
        state(actor, nodeId).operated.add(component);
    }

    public void didNotOperate(UUID actor, String nodeId, String component) {
        state(actor, nodeId).notOperated.add(component);
    }

    public void clearNode(UUID actor, String nodeId) {
        facts.remove(new Key(actor, nodeId));
    }

    public void clearPlayer(UUID actor) {
        facts.keySet().removeIf(key -> key.actor().equals(actor));
    }

    @Override
    public void enrich(
            PhysicalPredicateAuthority.Node node,
            UUID actor,
            MechanicObservation.Builder observation) {
        Facts value = facts.get(new Key(actor, node.nodeId()));
        if (value == null) {
            return;
        }
        Optional.ofNullable(value.answer).ifPresent(observation::answer);
        Optional.ofNullable(value.handle).ifPresent(observation::handle);
        Optional.ofNullable(value.viewSide).ifPresent(observation::viewSide);
        value.booleans.forEach(observation::booleanFact);
        value.integers.forEach(observation::integerFact);
        value.strings.forEach(observation::stringFact);
        value.lists.forEach(observation::stringList);
        value.operated.forEach(observation::operated);
        value.notOperated.forEach(observation::didNotOperate);
    }

    private Facts state(UUID actor, String nodeId) {
        Objects.requireNonNull(actor, "actor");
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId cannot be blank");
        }
        return facts.computeIfAbsent(new Key(actor, nodeId), ignored -> new Facts());
    }

    private record Key(UUID actor, String nodeId) {
    }

    private static final class Facts {
        private volatile String answer;
        private volatile String handle;
        private volatile String viewSide;
        private final Map<String, Boolean> booleans = new ConcurrentHashMap<>();
        private final Map<String, Integer> integers = new ConcurrentHashMap<>();
        private final Map<String, String> strings = new ConcurrentHashMap<>();
        private final Map<String, List<String>> lists = new ConcurrentHashMap<>();
        private final Set<String> operated = ConcurrentHashMap.newKeySet();
        private final Set<String> notOperated = ConcurrentHashMap.newKeySet();
    }
}
