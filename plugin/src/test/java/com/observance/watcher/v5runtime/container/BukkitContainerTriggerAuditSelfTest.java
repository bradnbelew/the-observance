package com.observance.watcher.v5runtime.container;

import java.util.Set;
import java.util.stream.Collectors;

/** Dependency-light matrix for strict controls and their bounded retained chunks. */
public final class BukkitContainerTriggerAuditSelfTest {
    private BukkitContainerTriggerAuditSelfTest() {
    }

    public static void main(String[] args) {
        check(ContainerTriggerBindings.requiredSyntheticComponents().keySet()
                        .equals(Set.of("CW07", "HS02")),
                "only the two exact synthetic controls receive retained neighborhoods");
        Set<ContainerTriggerChunkPolicy.ChunkCoordinate> edge =
                Set.copyOf(ContainerTriggerChunkPolicy.chunks(0.0, 124.0));
        check(edge.equals(Set.of(
                        new ContainerTriggerChunkPolicy.ChunkCoordinate(-1, 7),
                        new ContainerTriggerChunkPolicy.ChunkCoordinate(0, 7))),
                "zero-edge site retains both neighboring chunks");
        Set<String> negative = ContainerTriggerChunkPolicy.chunks(-44.0, 168.0).stream()
                .map(chunk -> chunk.x() + ":" + chunk.z()).collect(Collectors.toSet());
        check(negative.equals(Set.of("-3:10")), "interior negative site remains one exact chunk");
        check(ContainerTriggerChunkPolicy.chunks(15.0, 15.0).size() == 4,
                "corner-adjacent control retains at most four chunks");
        System.out.println("BukkitContainerTriggerAuditSelfTest PASS - critical controls retain bounded exact chunks and readiness stays strict");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
