package com.observance.watcher.morrow.room04.staticrestore;

import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Candidate;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Pass;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Restart-derived, transactional three-pass world engine for the six M02 cells. */
public final class StaticRestoreEngine {
    public interface WorldPort {
        String blockData(Cell relative);
        void setBlockData(Cell relative, String blockData);
    }

    public enum StepStatus { LOCKED, APPLIED, COMPLETE }

    public record StepResult(StepStatus status, int pass, int restoredCells) {
        public StepResult {
            Objects.requireNonNull(status, "status");
            if (pass < 0 || pass > 3 || restoredCells < 0 || restoredCells > 6) {
                throw new IllegalArgumentException("invalid Static Restore step result");
            }
        }
    }

    private final StaticRestoreManifest manifest;
    private final WorldPort world;

    public StaticRestoreEngine(StaticRestoreManifest manifest, WorldPort world) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.world = Objects.requireNonNull(world, "world");
    }

    public synchronized StepResult step(boolean proposalAuthenticated) throws IOException {
        if (!proposalAuthenticated) {
            auditBaseline();
            return new StepResult(StepStatus.LOCKED, 0, 0);
        }
        Inspection inspection = inspect();
        if (inspection.complete()) return new StepResult(StepStatus.COMPLETE, 3, 6);
        Pass next = manifest.passes().get(inspection.completedPasses());
        applyTransaction(next);
        int restored = next.number() * 2;
        return new StepResult(
                restored == 6 ? StepStatus.COMPLETE : StepStatus.APPLIED,
                next.number(),
                restored);
    }

    public synchronized boolean complete() throws IOException {
        return inspect().complete();
    }

    public synchronized int completedPasses() throws IOException {
        return inspect().completedPasses();
    }

    /** Exact bounded rollback used by the authored Reset control. */
    public synchronized void reset(boolean proposalAuthenticated) throws IOException {
        if (!proposalAuthenticated) throw new IllegalStateException("Static Restore proposal is not authenticated");
        LinkedHashMap<Cell, String> before = snapshotAll();
        try {
            for (Candidate candidate : manifest.candidates().values()) {
                world.setBlockData(candidate.cell(), StaticRestoreManifest.BASELINE_BLOCK);
            }
            auditBaseline();
        } catch (Throwable failure) {
            restore(before, failure);
        }
    }

    public synchronized void auditBaseline() throws IOException {
        for (Candidate candidate : manifest.candidates().values()) {
            String actual = world.blockData(candidate.cell());
            if (!StaticRestoreManifest.BASELINE_BLOCK.equals(actual)) {
                throw new IOException("Static Restore baseline drift at " + candidate.id() + ": " + actual);
            }
        }
    }

    public synchronized void auditComplete() throws IOException {
        Inspection inspection = inspect();
        if (!inspection.complete()) throw new IOException("Static Restore is incomplete");
    }

    private Inspection inspect() throws IOException {
        int completed = 0;
        boolean incompleteSeen = false;
        for (Pass pass : manifest.passes()) {
            int restored = 0;
            int baseline = 0;
            for (Candidate candidate : pass.candidates()) {
                String actual = world.blockData(candidate.cell());
                if (candidate.restoredBlock().equals(actual)) restored++;
                else if (StaticRestoreManifest.BASELINE_BLOCK.equals(actual)) baseline++;
                else throw new IOException("Static Restore foreign drift at " + candidate.id() + ": " + actual);
            }
            if (restored == pass.candidates().size()) {
                if (incompleteSeen) throw new IOException("Static Restore pass order drift at pass " + pass.number());
                completed++;
            } else {
                if (incompleteSeen && restored > 0) {
                    throw new IOException("Static Restore later pass started before its prerequisite at pass "
                            + pass.number());
                }
                incompleteSeen = true;
                // A crash may leave one cell of the current two-cell pass. The next transaction snapshots
                // and completes that pass; later passes must remain baseline.
                if (restored + baseline != pass.candidates().size()) {
                    throw new IOException("Static Restore invalid partial pass " + pass.number());
                }
            }
        }
        return new Inspection(completed, completed == manifest.passes().size());
    }

    private void applyTransaction(Pass pass) throws IOException {
        LinkedHashMap<Cell, String> before = new LinkedHashMap<>();
        pass.candidates().forEach(candidate -> before.put(candidate.cell(), world.blockData(candidate.cell())));
        try {
            for (Candidate candidate : pass.candidates()) {
                world.setBlockData(candidate.cell(), candidate.restoredBlock());
            }
            for (Candidate candidate : pass.candidates()) {
                if (!candidate.restoredBlock().equals(world.blockData(candidate.cell()))) {
                    throw new IOException("Static Restore pass read-back failed at " + candidate.id());
                }
            }
        } catch (Throwable failure) {
            restore(before, failure);
        }
    }

    private LinkedHashMap<Cell, String> snapshotAll() {
        LinkedHashMap<Cell, String> before = new LinkedHashMap<>();
        manifest.candidates().values().forEach(candidate -> before.put(candidate.cell(), world.blockData(candidate.cell())));
        return before;
    }

    private void restore(Map<Cell, String> before, Throwable failure) throws IOException {
        Throwable rollbackFailure = null;
        for (Map.Entry<Cell, String> entry : before.entrySet()) {
            try {
                world.setBlockData(entry.getKey(), entry.getValue());
            } catch (Throwable problem) {
                if (rollbackFailure == null) rollbackFailure = problem;
                else rollbackFailure.addSuppressed(problem);
            }
        }
        for (Map.Entry<Cell, String> entry : before.entrySet()) {
            if (!entry.getValue().equals(world.blockData(entry.getKey()))) {
                IOException mismatch = new IOException(
                        "Static Restore rollback read-back failed at " + entry.getKey());
                if (rollbackFailure == null) rollbackFailure = mismatch;
                else rollbackFailure.addSuppressed(mismatch);
            }
        }
        if (rollbackFailure != null) failure.addSuppressed(rollbackFailure);
        if (failure instanceof IOException io) throw io;
        if (failure instanceof RuntimeException runtime) throw runtime;
        throw new IOException("Static Restore transaction failed", failure);
    }

    private record Inspection(int completedPasses, boolean complete) { }
}
