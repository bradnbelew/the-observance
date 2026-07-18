package com.observance.watcher.v5runtime;

import java.nio.file.Files;
import java.nio.file.Path;

public final class P5CurationStateSelfTest {
    private P5CurationStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("observance-p5-curation-");
        try {
            Path journal = directory.resolve("progress.json");
            PhysicalPredicateAuthority authority = PhysicalPredicateAuthorityLoader.loadDefault();
            V5ProgressStore progress = V5ProgressStore.open(journal, authority);
            require(P5CurationRuntime.select(progress, P5CurationRuntime.Choice.SERVICE_PUBLIC)
                    == P5CurationRuntime.SelectionResult.NOT_READY, "sealed case must refuse");
            progress.completeIfAbsent("v5_case_c02_complete");
            require(P5CurationRuntime.select(progress, P5CurationRuntime.Choice.PENALTY_CUSTODY)
                    == P5CurationRuntime.SelectionResult.SELECTED, "either side may be first");
            require(!progress.snapshot().isComplete(P5CurationRuntime.CURATION_EVENT),
                    "one side cannot finish curation");
            V5ProgressStore restarted = V5ProgressStore.open(journal, authority);
            require(restarted.snapshot().isComplete(P5CurationRuntime.PENALTY_SELECTED),
                    "partial choice must survive restart");
            require(P5CurationRuntime.select(restarted, P5CurationRuntime.Choice.SERVICE_PUBLIC)
                    == P5CurationRuntime.SelectionResult.COMPLETE, "other side completes in either order");
            require(restarted.snapshot().isComplete(P5CurationRuntime.CHRONOLOGY_EVENT)
                    && restarted.snapshot().isComplete(P5CurationRuntime.CURATION_EVENT),
                    "both exact story events must commit together");
            require(P5CurationRuntime.select(restarted, P5CurationRuntime.Choice.SERVICE_PUBLIC)
                    == P5CurationRuntime.SelectionResult.ALREADY, "retry must be idempotent");
            V5ProgressStore finalRestart = V5ProgressStore.open(journal, authority);
            require(finalRestart.snapshot().isComplete(P5CurationRuntime.CURATION_EVENT),
                    "completed curation must survive a second restart");
            System.out.println("P5CurationStateSelfTest OK - sealed refusal, any-order partial state, idempotency, and restart pass");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
