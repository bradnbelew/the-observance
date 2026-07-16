package com.observance.watcher.m3runtime;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Isolated M3 proof for P4 receipt, gate, restart, and catch-up behavior.
 *
 * <p>This class is not wired to Bukkit or production. It binds the exact private-slice finding set to
 * the M2 local-primary journal so a later disposable Paper adapter cannot invent a second progression
 * authority.</p>
 */
public final class PrivateSliceState {
    public static final List<String> BASE_FINDINGS = List.of("P4.F1", "P4.F2", "P4.F3", "P4.F4");
    public static final String SYNTHESIS = "P4.F5";
    public static final String GATE_IDEMPOTENCY = "m3:p4:gate:P4_INTAKE_GATE:open:v1";

    private final LocalPrimaryJournal journal;
    private final Map<String, Set<String>> sourcesByFinding = new LinkedHashMap<>();
    private final Map<String, Set<String>> contributorsByFinding = new LinkedHashMap<>();
    private boolean gateOpen;

    private PrivateSliceState(LocalPrimaryJournal journal) throws IOException {
        this.journal = journal;
        replay(journal.after(0));
        if (sourcesByFinding.containsKey(SYNTHESIS) && !gateOpen) {
            journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
            replay(journal.after(0));
        }
    }

    public static PrivateSliceState open(Path journalPath) throws IOException {
        return new PrivateSliceState(LocalPrimaryJournal.open(journalPath));
    }

    public synchronized LocalPrimaryJournal.Receipt commitFinding(
            String findingId, List<String> sourceReceiptIds, String contributorId) throws IOException {
        requireFinding(findingId);
        if (contributorId == null || contributorId.isBlank()) {
            throw new IllegalArgumentException("contributor identity is required for provenance");
        }
        Set<String> sources = normalizeSources(sourceReceiptIds);
        if (SYNTHESIS.equals(findingId)) {
            if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))) {
                throw new IllegalArgumentException("P4.F5 requires the four exact finding receipts");
            }
            if (!sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                throw new IllegalStateException("P4.F5 cannot commit before P4.F1-P4.F4");
            }
        } else if (sources.size() < 2) {
            throw new IllegalArgumentException(findingId + " requires at least two independent sources");
        }

        byte[] findingPayload = bytes(findingId + "\n" + String.join(",", sources));
        LocalPrimaryJournal.Receipt finding = journal.append(
                "m3:p4:finding:" + findingId + ":v1", "finding_committed", findingPayload);
        journal.append("m3:p4:contribution:" + findingId + ":" + contributorId,
                "contribution_recorded", bytes(findingId + "\n" + contributorId));
        if (SYNTHESIS.equals(findingId)) {
            journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
        }
        resetDerivedState();
        replay(journal.after(0));
        return finding;
    }

    public synchronized boolean findingCommitted(String findingId) {
        return sourcesByFinding.containsKey(findingId);
    }

    public synchronized boolean gateOpen() {
        return gateOpen;
    }

    public synchronized Set<String> contributors(String findingId) {
        return Set.copyOf(contributorsByFinding.getOrDefault(findingId, Set.of()));
    }

    public synchronized List<LocalPrimaryJournal.Receipt> catchUpAfter(long sequence) {
        return journal.after(sequence);
    }

    private void replay(List<LocalPrimaryJournal.Receipt> receipts) throws IOException {
        for (LocalPrimaryJournal.Receipt receipt : receipts) {
            String payload = new String(receipt.payload(), StandardCharsets.UTF_8);
            if ("finding_committed".equals(receipt.eventType())) {
                String[] fields = payload.split("\n", -1);
                if (fields.length != 2) throw new IOException("invalid M3 finding receipt");
                requireFinding(fields[0]);
                Set<String> sources = normalizeSources(List.of(fields[1].split(",", -1)));
                if (SYNTHESIS.equals(fields[0])) {
                    if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))
                            || !sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                        throw new IOException("invalid M3 synthesis ordering/provenance");
                    }
                } else if (sources.size() < 2) {
                    throw new IOException("invalid M3 independent-source receipt");
                }
                Set<String> previous = sourcesByFinding.putIfAbsent(fields[0], sources);
                if (previous != null && !previous.equals(sources)) {
                    throw new IOException("conflicting M3 finding receipt");
                }
            } else if ("contribution_recorded".equals(receipt.eventType())) {
                String[] fields = payload.split("\n", -1);
                if (fields.length != 2 || fields[1].isBlank()) throw new IOException("invalid M3 contributor receipt");
                requireFinding(fields[0]);
                contributorsByFinding.computeIfAbsent(fields[0], ignored -> new LinkedHashSet<>()).add(fields[1]);
            } else if ("gate_opened".equals(receipt.eventType())) {
                if (!"P4_INTAKE_GATE".equals(payload) || !sourcesByFinding.containsKey(SYNTHESIS)) {
                    throw new IOException("physical gate opened without local P4.F5 receipt");
                }
                gateOpen = true;
            } else {
                throw new IOException("unexpected M3 journal event: " + receipt.eventType());
            }
        }
    }

    private void resetDerivedState() {
        sourcesByFinding.clear();
        contributorsByFinding.clear();
        gateOpen = false;
    }

    private static Set<String> normalizeSources(List<String> sourceReceiptIds) {
        if (sourceReceiptIds == null) throw new IllegalArgumentException("source receipts are required");
        List<String> sorted = new ArrayList<>();
        for (String source : sourceReceiptIds) {
            if (source == null || source.isBlank() || source.indexOf(',') >= 0 || source.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("source receipt ids must be non-empty canonical tokens");
            }
            sorted.add(source);
        }
        sorted.sort(String::compareTo);
        return new LinkedHashSet<>(sorted);
    }

    private static void requireFinding(String findingId) {
        if (!BASE_FINDINGS.contains(findingId) && !SYNTHESIS.equals(findingId)) {
            throw new IllegalArgumentException("finding is outside the exact P4 slice: " + findingId);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
