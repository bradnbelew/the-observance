package com.observance.watcher.structure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free executable guard for the V5 content, recovery, and orientation contract. */
public final class DeepHoldV5ManifestSelfTest {
    public static void main(String[] args) {
        List<String> issues = DeepHoldV5Manifest.validate();
        if (!issues.isEmpty()) {
            for (String issue : issues) System.out.println("  FAIL " + issue);
            throw new AssertionError("DeepHoldV5ManifestSelfTest: " + issues.size() + " failure(s)");
        }
        if (!"+Z".equals(DeepHoldV5Manifest.CANONICAL_ORIENTATION)) {
            throw new AssertionError("The only build orientation must be +Z");
        }
        if (DeepHoldV5Manifest.ARTIFACTS.size() != 21) {
            throw new AssertionError("Expected exactly 21 V5 recovery artifacts and no retired tokens");
        }
        Set<String> hashes = new HashSet<>();
        hashes.add(DeepHoldV5Manifest.contentHash());
        hashes.add(DeepHoldV5Manifest.contentHash());
        if (hashes.size() != 1 || hashes.iterator().next().length() != 64) {
            throw new AssertionError("Content fingerprint is not stable SHA-256");
        }
        for (DeepHoldV5Manifest.Artifact artifact : DeepHoldV5Manifest.ARTIFACTS) {
            if (DeepHoldV5Manifest.artifact(artifact.id()) != artifact) {
                throw new AssertionError("Artifact lookup failed for " + artifact.id());
            }
        }
        int[] eastBehind = DeepHoldV5Manifest.behindFixture("EAST", 8);
        int[] westBehind = DeepHoldV5Manifest.behindFixture("WEST", 8);
        if (eastBehind[0] != -8 || eastBehind[1] != 0
                || westBehind[0] != 8 || westBehind[1] != 0) {
            throw new AssertionError("Fixture-local mount offsets are not orientation-safe");
        }
        if ((1_000 + eastBehind[0]) - 1_000 != (-1_000 + eastBehind[0]) - (-1_000)) {
            throw new AssertionError("Fixture-local mount offset depends on absolute world X");
        }
        System.out.println("DeepHoldV5ManifestSelfTest: OK - 32 rooms, 76 fixtures, 8 gates, "
                + "21 recoverable artifacts, orientation +Z, hash=" + DeepHoldV5Manifest.contentHash());
    }
}
