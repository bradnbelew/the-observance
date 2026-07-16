package com.observance.watcher.m2runtime;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main-driven isolated proof for exact historical bytes, atomic activation, rollback, and recovery. */
public final class AtomicPredicateRuntimeSelfTest {
    private AtomicPredicateRuntimeSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        byte[] canonical = loadCanonicalBytes();
        check(PredicateAuthorityVersion.rawSha256(canonical).equals(HistoricalPredicateBytes.CANONICAL_LF_SHA256),
                "packaged canonical predicate must be LF 16de bytes");
        check(PredicateAuthorityVersion.semanticSha256(canonical).equals(HistoricalPredicateBytes.SEMANTIC_SHA256),
                "canonical semantic hash");
        byte[] historical = HistoricalPredicateBytes.reconstruct(canonical);
        check(PredicateAuthorityVersion.semanticSha256(historical).equals(HistoricalPredicateBytes.SEMANTIC_SHA256),
                "historical and LF bytes must be semantically identical");

        PredicateAuthorityVersion live = new PredicateAuthorityVersion(
                "v5-live-receipt-2026-07-14",
                HistoricalPredicateBytes.HISTORICAL_RAW_SHA256,
                HistoricalPredicateBytes.SEMANTIC_SHA256,
                null,
                null);
        PredicateAuthorityVersion canonicalVersion = new PredicateAuthorityVersion(
                "v5-git-lf-3ef5486",
                HistoricalPredicateBytes.CANONICAL_LF_SHA256,
                HistoricalPredicateBytes.SEMANTIC_SHA256,
                HistoricalPredicateBytes.HISTORICAL_RAW_SHA256,
                HistoricalPredicateBytes.HISTORICAL_RAW_SHA256);

        AtomicPredicateRuntime runtime = new AtomicPredicateRuntime(historical, live);
        AtomicPredicateRuntime.ActivationReceipt forward = runtime.activate(
                canonical, canonicalVersion, live.rawSha256(),
                AtomicPredicateRuntime.Transition.FORWARD, "m2:predicate:37020-to-16de");
        check(runtime.snapshot().version().rawSha256().equals(canonicalVersion.rawSha256()), "forward active");
        check(runtime.activate(canonical, canonicalVersion, live.rawSha256(),
                AtomicPredicateRuntime.Transition.FORWARD, "m2:predicate:37020-to-16de") == forward,
                "same active transition is idempotent");

        byte[] damaged = canonical.clone();
        damaged[100] ^= 1;
        expectFailure(() -> runtime.activate(damaged, canonicalVersion, canonicalVersion.rawSha256(),
                AtomicPredicateRuntime.Transition.FORWARD, "m2:damaged"), "damaged bytes fail before swap");
        check(runtime.snapshot().version().rawSha256().equals(canonicalVersion.rawSha256()),
                "failed candidate leaves active snapshot intact");

        runtime.activate(historical, live, canonicalVersion.rawSha256(),
                AtomicPredicateRuntime.Transition.ROLLBACK, "m2:predicate:16de-to-37020");
        check(runtime.snapshot().version().rawSha256().equals(live.rawSha256()), "rollback active");
        expectFailure(() -> runtime.activate(canonical, canonicalVersion, live.rawSha256(),
                AtomicPredicateRuntime.Transition.FORWARD, "m2:predicate:37020-to-16de"),
                "old idempotency key cannot mask a later rollback");

        proveConcurrentSnapshots(runtime, historical, live, canonical, canonicalVersion);
        System.out.println("M2 atomic predicate runtime self-test passed");
    }

    private static void proveConcurrentSnapshots(
            AtomicPredicateRuntime runtime,
            byte[] historical,
            PredicateAuthorityVersion live,
            byte[] canonical,
            PredicateAuthorityVersion canonicalVersion) throws InterruptedException {
        AtomicBoolean invalid = new AtomicBoolean(false);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> readers = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            Thread reader = new Thread(() -> {
                try {
                    start.await();
                    for (int pass = 0; pass < 10_000; pass++) {
                        AtomicPredicateRuntime.Snapshot snapshot = runtime.snapshot();
                        if (!snapshot.version().rawSha256().equals(snapshot.authority().sha256())) invalid.set(true);
                        if (snapshot.authority().nodes().size() != 60) invalid.set(true);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    invalid.set(true);
                }
            }, "m2-predicate-reader-" + index);
            readers.add(reader);
            reader.start();
        }
        start.countDown();
        runtime.activate(canonical, canonicalVersion, live.rawSha256(),
                AtomicPredicateRuntime.Transition.FORWARD, "m2:predicate:forward-recovery");
        runtime.activate(historical, live, canonicalVersion.rawSha256(),
                AtomicPredicateRuntime.Transition.ROLLBACK, "m2:predicate:rollback-rehearsal-2");
        for (Thread reader : readers) reader.join();
        check(!invalid.get(), "readers observed only complete immutable snapshots");
    }

    private static byte[] loadCanonicalBytes() throws IOException {
        try (InputStream input = PhysicalPredicateAuthorityLoader.class.getResourceAsStream(
                PhysicalPredicateAuthorityLoader.RESOURCE_PATH)) {
            if (input == null) throw new IOException("missing predicate test resource");
            return input.readAllBytes();
        }
    }

    private static void expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not fail");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
