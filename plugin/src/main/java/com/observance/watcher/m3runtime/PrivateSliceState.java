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

/** Local-primary observation, finding, gate, catch-up, and A2 approval state for the M3 v3 slice. */
public final class PrivateSliceState {
    public static final List<String> BASE_FINDINGS = List.of("P4.F1", "P4.F2", "P4.F3", "P4.F4");
    public static final String SYNTHESIS = "P4.F5";
    public static final String GATE_IDEMPOTENCY = "m3:p4:gate:P4_INTAKE_GATE:open:v3";
    public static final String WATCHER_PAYLOAD_SHA256 =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";
    public static final Map<String, Set<String>> ALLOWED_SOURCES = Map.of(
            "P4.F1", Set.of("drainage_map", "cart_wear"),
            "P4.F2", Set.of("material_join_civic", "survey_revisions"),
            "P4.F3", Set.of("population_board", "ration_ledger"),
            "P4.F4", Set.of("founding_minutes", "descent_heat_marks"));

    private final LocalPrimaryJournal journal;
    private final Map<String, Set<String>> observationsByFinding = new LinkedHashMap<>();
    private final Map<String, Set<String>> sourcesByFinding = new LinkedHashMap<>();
    private final Map<String, Set<String>> contributorsByFinding = new LinkedHashMap<>();
    private final Map<String, WatcherApproval> watcherApprovals = new LinkedHashMap<>();
    private final Set<String> consumedWatcherApprovals = new LinkedHashSet<>();
    private boolean gateOpen;

    private PrivateSliceState(LocalPrimaryJournal journal) throws IOException {
        this.journal = journal;
        replay(journal.after(0));
        if (sourcesByFinding.containsKey(SYNTHESIS) && !gateOpen) {
            journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
            resetDerivedState();
            replay(journal.after(0));
        }
    }

    public static PrivateSliceState open(Path journalPath) throws IOException {
        return new PrivateSliceState(LocalPrimaryJournal.open(journalPath));
    }

    public synchronized LocalPrimaryJournal.Receipt commitObservation(
            String findingId, String sourceId, String contributorId) throws IOException {
        requireBaseFinding(findingId);
        requireContributor(contributorId);
        if (!ALLOWED_SOURCES.get(findingId).contains(sourceId)) {
            throw new IllegalArgumentException("source is not authored for " + findingId + ": " + sourceId);
        }
        LocalPrimaryJournal.Receipt observation = journal.append(
                "m3:p4:observation:" + sourceId + ":v3", "observation_committed",
                bytes(findingId + "\n" + sourceId));
        journal.append("m3:p4:observation-contribution:" + sourceId + ":" + contributorId + ":v3",
                "observation_contribution_recorded", bytes(findingId + "\n" + sourceId + "\n" + contributorId));
        resetDerivedState();
        replay(journal.after(0));
        return observation;
    }

    public synchronized LocalPrimaryJournal.Receipt commitFinding(
            String findingId, List<String> sourceReceiptIds, String contributorId) throws IOException {
        requireFinding(findingId);
        requireContributor(contributorId);
        Set<String> sources = normalizeSources(sourceReceiptIds);
        if (SYNTHESIS.equals(findingId)) {
            if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))) {
                throw new IllegalArgumentException("P4.F5 requires the four exact finding receipts");
            }
            if (!sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                throw new IllegalStateException("P4.F5 cannot commit before P4.F1-P4.F4");
            }
        } else {
            if (sources.size() < 2 || !ALLOWED_SOURCES.get(findingId).containsAll(sources)) {
                throw new IllegalArgumentException(findingId + " requires two authored independent sources");
            }
            if (!observationsByFinding.getOrDefault(findingId, Set.of()).containsAll(sources)) {
                throw new IllegalStateException(findingId + " cannot file before its source surfaces are inspected");
            }
        }

        byte[] findingPayload = bytes(findingId + "\n" + String.join(",", sources));
        LocalPrimaryJournal.Receipt finding = journal.append(
                "m3:p4:finding:" + findingId + ":v3", "finding_committed", findingPayload);
        journal.append("m3:p4:contribution:" + findingId + ":" + contributorId + ":v3",
                "contribution_recorded", bytes(findingId + "\n" + contributorId));
        if (SYNTHESIS.equals(findingId)) {
            journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
        }
        resetDerivedState();
        replay(journal.after(0));
        return finding;
    }

    public synchronized WatcherApproval approveWatcher(String approvalId, String westTarget,
            String eastTarget, long expiresAtEpochSecond, long nowEpochSecond) throws IOException {
        requireToken(approvalId, "approval id");
        requireToken(westTarget, "west target");
        requireToken(eastTarget, "east target");
        if (westTarget.equalsIgnoreCase(eastTarget)) throw new IllegalArgumentException("A2 targets must be distinct");
        if (!findingCommitted("P4.F3") || findingCommitted(SYNTHESIS)) {
            throw new IllegalStateException("A2 approval requires P4.F3 committed and P4.F5 open");
        }
        long duration = expiresAtEpochSecond - nowEpochSecond;
        if (duration < 60 || duration > 1800) throw new IllegalArgumentException("A2 approval must expire in 1-30 minutes");
        WatcherApproval approval = new WatcherApproval(approvalId, WATCHER_PAYLOAD_SHA256,
                "m3-private-slice-v3/named-test-players", westTarget, eastTarget, expiresAtEpochSecond);
        journal.append("m3:p4:watcher-approval:" + approvalId + ":v3", "watcher_approval_recorded",
                bytes(approval.serialize()));
        resetDerivedState();
        replay(journal.after(0));
        return approval;
    }

    public synchronized WatcherApproval consumeWatcher(String approvalId, String westTarget,
            String eastTarget, long nowEpochSecond) throws IOException {
        WatcherApproval approval = watcherApprovals.get(approvalId);
        if (approval == null || !approval.westTarget().equalsIgnoreCase(westTarget)
                || !approval.eastTarget().equalsIgnoreCase(eastTarget)
                || !WATCHER_PAYLOAD_SHA256.equals(approval.payloadSha256())) {
            throw new IllegalStateException("exact A2 approval not found for named targets");
        }
        if (approval.expiresAtEpochSecond() < nowEpochSecond) throw new IllegalStateException("A2 approval expired");
        if (consumedWatcherApprovals.contains(approvalId)) throw new IllegalStateException("A2 approval already consumed");
        if (!findingCommitted("P4.F3") || findingCommitted(SYNTHESIS)) {
            throw new IllegalStateException("A2 prerequisite no longer holds");
        }
        journal.append("m3:p4:watcher-approval:" + approvalId + ":consumed:v3",
                "watcher_approval_consumed", bytes(approval.serialize()));
        resetDerivedState();
        replay(journal.after(0));
        return approval;
    }

    public synchronized boolean findingCommitted(String findingId) {
        return sourcesByFinding.containsKey(findingId);
    }

    public synchronized boolean gateOpen() { return gateOpen; }

    public synchronized Set<String> observedSources(String findingId) {
        return Set.copyOf(observationsByFinding.getOrDefault(findingId, Set.of()));
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
            switch (receipt.eventType()) {
                case "observation_committed" -> replayObservation(payload);
                case "observation_contribution_recorded" -> replayObservationContribution(payload);
                case "finding_committed" -> replayFinding(payload);
                case "contribution_recorded" -> replayContribution(payload);
                case "gate_opened" -> {
                    if (!"P4_INTAKE_GATE".equals(payload) || !sourcesByFinding.containsKey(SYNTHESIS)) {
                        throw new IOException("physical gate opened without local P4.F5 receipt");
                    }
                    gateOpen = true;
                }
                case "watcher_approval_recorded" -> replayWatcherApproval(payload, false);
                case "watcher_approval_consumed" -> replayWatcherApproval(payload, true);
                default -> throw new IOException("unexpected M3 journal event: " + receipt.eventType());
            }
        }
    }

    private void replayObservation(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 2) throw new IOException("invalid M3 observation receipt");
        requireBaseFinding(fields[0]);
        if (!ALLOWED_SOURCES.get(fields[0]).contains(fields[1])) throw new IOException("unauthored M3 observation");
        observationsByFinding.computeIfAbsent(fields[0], ignored -> new LinkedHashSet<>()).add(fields[1]);
    }

    private void replayObservationContribution(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 3 || fields[2].isBlank()) throw new IOException("invalid observation contributor receipt");
        requireBaseFinding(fields[0]);
        if (!observationsByFinding.getOrDefault(fields[0], Set.of()).contains(fields[1])) {
            throw new IOException("observation contribution precedes observation");
        }
    }

    private void replayFinding(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 2) throw new IOException("invalid M3 finding receipt");
        requireFinding(fields[0]);
        Set<String> sources = normalizeSources(List.of(fields[1].split(",", -1)));
        if (SYNTHESIS.equals(fields[0])) {
            if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))
                    || !sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                throw new IOException("invalid M3 synthesis ordering/provenance");
            }
        } else if (sources.size() < 2 || !ALLOWED_SOURCES.get(fields[0]).containsAll(sources)
                || !observationsByFinding.getOrDefault(fields[0], Set.of()).containsAll(sources)) {
            throw new IOException("invalid M3 observed-source finding receipt");
        }
        Set<String> previous = sourcesByFinding.putIfAbsent(fields[0], sources);
        if (previous != null && !previous.equals(sources)) throw new IOException("conflicting M3 finding receipt");
    }

    private void replayContribution(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 2 || fields[1].isBlank()) throw new IOException("invalid M3 contributor receipt");
        requireFinding(fields[0]);
        contributorsByFinding.computeIfAbsent(fields[0], ignored -> new LinkedHashSet<>()).add(fields[1]);
    }

    private void replayWatcherApproval(String payload, boolean consumed) throws IOException {
        WatcherApproval approval = WatcherApproval.parse(payload);
        if (!WATCHER_PAYLOAD_SHA256.equals(approval.payloadSha256())) throw new IOException("A2 payload hash drift");
        WatcherApproval previous = watcherApprovals.putIfAbsent(approval.approvalId(), approval);
        if (previous != null && !previous.equals(approval)) throw new IOException("conflicting A2 approval");
        if (consumed) consumedWatcherApprovals.add(approval.approvalId());
    }

    private void resetDerivedState() {
        observationsByFinding.clear();
        sourcesByFinding.clear();
        contributorsByFinding.clear();
        watcherApprovals.clear();
        consumedWatcherApprovals.clear();
        gateOpen = false;
    }

    private static Set<String> normalizeSources(List<String> sourceReceiptIds) {
        if (sourceReceiptIds == null) throw new IllegalArgumentException("source receipts are required");
        List<String> sorted = new ArrayList<>();
        for (String source : sourceReceiptIds) {
            requireToken(source, "source receipt id");
            sorted.add(source);
        }
        sorted.sort(String::compareTo);
        return new LinkedHashSet<>(sorted);
    }

    private static void requireBaseFinding(String findingId) {
        if (!BASE_FINDINGS.contains(findingId)) {
            throw new IllegalArgumentException("finding is not an evidence lane: " + findingId);
        }
    }

    private static void requireFinding(String findingId) {
        if (!BASE_FINDINGS.contains(findingId) && !SYNTHESIS.equals(findingId)) {
            throw new IllegalArgumentException("finding is outside the exact P4 slice: " + findingId);
        }
    }

    private static void requireContributor(String contributorId) {
        requireToken(contributorId, "contributor identity");
    }

    private static void requireToken(String token, String label) {
        if (token == null || token.isBlank() || token.indexOf(',') >= 0 || token.indexOf('\n') >= 0
                || token.indexOf('|') >= 0) throw new IllegalArgumentException(label + " must be a canonical token");
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    public record WatcherApproval(String approvalId, String payloadSha256, String scope,
            String westTarget, String eastTarget, long expiresAtEpochSecond) {
        String serialize() {
            return String.join("|", approvalId, payloadSha256, scope, westTarget, eastTarget,
                    Long.toString(expiresAtEpochSecond));
        }

        static WatcherApproval parse(String payload) throws IOException {
            String[] fields = payload.split("\\|", -1);
            if (fields.length != 6) throw new IOException("invalid A2 approval receipt");
            try {
                return new WatcherApproval(fields[0], fields[1], fields[2], fields[3], fields[4],
                        Long.parseLong(fields[5]));
            } catch (NumberFormatException failure) {
                throw new IOException("invalid A2 expiry", failure);
            }
        }
    }
}
