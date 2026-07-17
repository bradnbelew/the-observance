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

/** Local-primary evidence, content-dependent report, synthesis, and A2 state for M3 v4. */
public final class PrivateSliceState {
    public static final List<String> BASE_FINDINGS = List.of("P4.F1", "P4.F2", "P4.F3", "P4.F4");
    public static final String SYNTHESIS = "P4.F5";
    public static final String GATE_IDEMPOTENCY = "m3:p4:gate:P4_INTAKE_GATE:open:v4";
    public static final int REFUSAL_WINDOW_SECONDS = 300;
    public static final int MAX_REFUSALS_PER_WINDOW = 3;
    public static final String WATCHER_PAYLOAD_SHA256 =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";

    public static final Map<String, Set<String>> ALLOWED_SOURCES = Map.of(
            "P4.F1", Set.of("drainage_plan", "cart_rut_tag"),
            "P4.F2", Set.of("mason_mark", "revision_letter"),
            "P4.F3", Set.of("berth_register", "ration_tally"),
            "P4.F4", Set.of("pump_gauge", "engineer_letter"));

    public static final Map<String, List<String>> CONCLUSION_OPTIONS = Map.of(
            "P4.F1", List.of(
                    "two_separate_public_roads",
                    "one_road_loaded_down_empty_return",
                    "drainage_crews_only",
                    "no_cart_road_crossed"),
            "P4.F2", List.of(
                    "single_emergency_build",
                    "two_campaigns_shelter_and_office",
                    "four_unrelated_repairs",
                    "three_campaigns_shelter_intake_commons"),
            "P4.F3", List.of(
                    "328_people_including_work_berths",
                    "294_refuge_places_work_berths_excluded",
                    "286_people_no_infirmary",
                    "300_people_equal_to_water_gauge"),
            "P4.F4", List.of(
                    "concealment_from_public_road",
                    "access_to_a_second_entrance",
                    "downcut_for_stable_cover_shorter_winter_service",
                    "accidental_quarry_breakthrough"),
            "P4.F5", List.of(
                    "temporary_quarry_shelter_abandoned_after_one_winter",
                    "planned_civic_intake_for_294_not_single_emergency_shelter",
                    "private_archive_with_no_public_refuge_role",
                    "natural_cave_later_mistaken_for_civic_works"));

    public static final Map<String, String> EXACT_CONCLUSIONS = Map.of(
            "P4.F1", "one_road_loaded_down_empty_return",
            "P4.F2", "three_campaigns_shelter_intake_commons",
            "P4.F3", "294_refuge_places_work_berths_excluded",
            "P4.F4", "downcut_for_stable_cover_shorter_winter_service",
            "P4.F5", "planned_civic_intake_for_294_not_single_emergency_shelter");

    private final LocalPrimaryJournal journal;
    private final Map<String, Set<String>> observationsByFinding = new LinkedHashMap<>();
    private final Map<String, Set<String>> sourcesByFinding = new LinkedHashMap<>();
    private final Map<String, String> conclusionsByFinding = new LinkedHashMap<>();
    private final Map<String, Set<String>> contributorsByFinding = new LinkedHashMap<>();
    private final Map<String, List<Long>> refusalsByContributor = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> draftsByContributor = new LinkedHashMap<>();
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
                "m3:p4:observation:" + sourceId + ":v4", "observation_committed",
                bytes(findingId + "\n" + sourceId));
        journal.append("m3:p4:observation-contribution:" + sourceId + ":" + contributorId + ":v4",
                "observation_contribution_recorded", bytes(findingId + "\n" + sourceId + "\n" + contributorId));
        resetDerivedState();
        replay(journal.after(0));
        return observation;
    }

    public synchronized void selectDraft(String findingId, String conclusionId, String contributorId) {
        requireFinding(findingId);
        requireContributor(contributorId);
        if (!CONCLUSION_OPTIONS.get(findingId).contains(conclusionId)) {
            throw new IllegalArgumentException("conclusion is not printed in the examiner ledger");
        }
        draftsByContributor.computeIfAbsent(contributorId, ignored -> new LinkedHashMap<>())
                .put(findingId, conclusionId);
    }

    public synchronized Map<String, String> draft(String contributorId) {
        requireContributor(contributorId);
        return Map.copyOf(draftsByContributor.getOrDefault(contributorId, Map.of()));
    }

    public synchronized List<LocalPrimaryJournal.Receipt> lodgeReport(
            String contributorId, long nowEpochSecond) throws IOException {
        requireContributor(contributorId);
        requireObservedPairs();
        enforceRefusalThrottle(contributorId, nowEpochSecond);
        Map<String, String> draft = draftsByContributor.getOrDefault(contributorId, Map.of());
        Map<String, String> submitted = new LinkedHashMap<>();
        for (String finding : BASE_FINDINGS) {
            String conclusion = draft.get(finding);
            if (conclusion != null) submitted.put(finding, conclusion);
        }
        if (submitted.size() != BASE_FINDINGS.size()
                || BASE_FINDINGS.stream().anyMatch(finding -> !EXACT_CONCLUSIONS.get(finding)
                        .equals(submitted.get(finding)))) {
            recordRefusal(contributorId, nowEpochSecond, "four_clause_report");
            draftsByContributor.remove(contributorId);
            throw new ReportRefusedException("report returned without endorsement; compare all four record pairs");
        }

        List<LocalPrimaryJournal.Receipt> committed = new ArrayList<>();
        for (String finding : BASE_FINDINGS) {
            committed.add(appendFinding(finding, ALLOWED_SOURCES.get(finding), submitted.get(finding), contributorId));
        }
        draftsByContributor.remove(contributorId);
        resetDerivedState();
        replay(journal.after(0));
        return List.copyOf(committed);
    }

    public synchronized LocalPrimaryJournal.Receipt lodgeSynthesis(
            String contributorId, long nowEpochSecond) throws IOException {
        requireContributor(contributorId);
        if (!sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
            throw new IllegalStateException("the four-clause report has not been endorsed");
        }
        enforceRefusalThrottle(contributorId, nowEpochSecond);
        String submitted = draftsByContributor.getOrDefault(contributorId, Map.of()).get(SYNTHESIS);
        if (!EXACT_CONCLUSIONS.get(SYNTHESIS).equals(submitted)) {
            recordRefusal(contributorId, nowEpochSecond, "synthesis");
            Map<String, String> draft = draftsByContributor.get(contributorId);
            if (draft != null) draft.remove(SYNTHESIS);
            throw new ReportRefusedException("seal endorsement returned; the report does not support that account");
        }
        LocalPrimaryJournal.Receipt receipt = appendFinding(SYNTHESIS, new LinkedHashSet<>(BASE_FINDINGS),
                submitted, contributorId);
        journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
        draftsByContributor.remove(contributorId);
        resetDerivedState();
        replay(journal.after(0));
        return receipt;
    }

    private LocalPrimaryJournal.Receipt appendFinding(String findingId, Set<String> sources,
            String conclusionId, String contributorId) throws IOException {
        if (!EXACT_CONCLUSIONS.get(findingId).equals(conclusionId)) {
            throw new IllegalArgumentException("unsupported conclusion");
        }
        List<String> sorted = new ArrayList<>(sources);
        sorted.sort(String::compareTo);
        byte[] findingPayload = bytes(findingId + "\n" + String.join(",", sorted) + "\n" + conclusionId);
        LocalPrimaryJournal.Receipt finding = journal.append(
                "m3:p4:finding:" + findingId + ":v4", "finding_committed", findingPayload);
        journal.append("m3:p4:contribution:" + findingId + ":" + contributorId + ":v4",
                "contribution_recorded", bytes(findingId + "\n" + contributorId));
        return finding;
    }

    private void requireObservedPairs() {
        for (String finding : BASE_FINDINGS) {
            if (!observationsByFinding.getOrDefault(finding, Set.of()).containsAll(ALLOWED_SOURCES.get(finding))) {
                throw new IllegalStateException("the examiner report is incomplete; surviving records remain unentered");
            }
        }
    }

    private void enforceRefusalThrottle(String contributorId, long nowEpochSecond) {
        long floor = nowEpochSecond - REFUSAL_WINDOW_SECONDS;
        long recent = refusalsByContributor.getOrDefault(contributorId, List.of()).stream()
                .filter(epoch -> epoch >= floor).count();
        if (recent >= MAX_REFUSALS_PER_WINDOW) {
            throw new FilingThrottleException("returned papers are under examiner review; the desk will reopen shortly");
        }
    }

    private void recordRefusal(String contributorId, long nowEpochSecond, String stage) throws IOException {
        long ordinal = journal.after(0).size() + 1L;
        journal.append("m3:p4:report-refused:" + contributorId + ":" + ordinal + ":v4",
                "report_refused", bytes(contributorId + "\n" + nowEpochSecond + "\n" + stage));
        resetDerivedState();
        replay(journal.after(0));
    }

    public synchronized WatcherApproval approveWatcher(String approvalId, String westTarget,
            String eastTarget, long expiresAtEpochSecond, long nowEpochSecond) throws IOException {
        requireToken(approvalId, "approval id");
        requireToken(westTarget, "west target");
        requireToken(eastTarget, "east target");
        if (westTarget.equalsIgnoreCase(eastTarget)) throw new IllegalArgumentException("A2 targets must be distinct");
        if (!findingCommitted("P4.F3") || findingCommitted(SYNTHESIS)) {
            throw new IllegalStateException("A2 approval requires the endorsed report and an unreleased seal");
        }
        long duration = expiresAtEpochSecond - nowEpochSecond;
        if (duration < 60 || duration > 1800) throw new IllegalArgumentException("A2 approval must expire in 1-30 minutes");
        WatcherApproval approval = new WatcherApproval(approvalId, WATCHER_PAYLOAD_SHA256,
                "m3-private-slice-v4/named-test-players", westTarget, eastTarget, expiresAtEpochSecond);
        journal.append("m3:p4:watcher-approval:" + approvalId + ":v4", "watcher_approval_recorded",
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
        journal.append("m3:p4:watcher-approval:" + approvalId + ":consumed:v4",
                "watcher_approval_consumed", bytes(approval.serialize()));
        resetDerivedState();
        replay(journal.after(0));
        return approval;
    }

    public synchronized boolean findingCommitted(String findingId) {
        return sourcesByFinding.containsKey(findingId);
    }

    public synchronized String committedConclusion(String findingId) {
        return conclusionsByFinding.get(findingId);
    }

    public synchronized boolean gateOpen() { return gateOpen; }

    public synchronized Set<String> observedSources(String findingId) {
        return Set.copyOf(observationsByFinding.getOrDefault(findingId, Set.of()));
    }

    public synchronized Set<String> contributors(String findingId) {
        return Set.copyOf(contributorsByFinding.getOrDefault(findingId, Set.of()));
    }

    public synchronized int refusalCount(String contributorId) {
        return refusalsByContributor.getOrDefault(contributorId, List.of()).size();
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
                case "report_refused" -> replayRefusal(payload);
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
        if (fields.length != 3) throw new IOException("invalid M3 v4 finding receipt");
        requireFinding(fields[0]);
        Set<String> sources = normalizeSources(List.of(fields[1].split(",", -1)));
        if (!EXACT_CONCLUSIONS.get(fields[0]).equals(fields[2])) {
            throw new IOException("finding conclusion is not the authored content-dependent conclusion");
        }
        if (SYNTHESIS.equals(fields[0])) {
            if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))
                    || !sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                throw new IOException("invalid M3 synthesis ordering/provenance");
            }
        } else if (!sources.equals(new LinkedHashSet<>(normalizeSources(new ArrayList<>(ALLOWED_SOURCES.get(fields[0])))))
                || !observationsByFinding.getOrDefault(fields[0], Set.of()).containsAll(sources)) {
            throw new IOException("invalid M3 observed-source finding receipt");
        }
        Set<String> previous = sourcesByFinding.putIfAbsent(fields[0], sources);
        if (previous != null && !previous.equals(sources)) throw new IOException("conflicting M3 finding receipt");
        String previousConclusion = conclusionsByFinding.putIfAbsent(fields[0], fields[2]);
        if (previousConclusion != null && !previousConclusion.equals(fields[2])) {
            throw new IOException("conflicting M3 conclusion receipt");
        }
    }

    private void replayContribution(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 2 || fields[1].isBlank()) throw new IOException("invalid M3 contributor receipt");
        requireFinding(fields[0]);
        contributorsByFinding.computeIfAbsent(fields[0], ignored -> new LinkedHashSet<>()).add(fields[1]);
    }

    private void replayRefusal(String payload) throws IOException {
        String[] fields = payload.split("\n", -1);
        if (fields.length != 3 || fields[0].isBlank()) throw new IOException("invalid report refusal receipt");
        long epoch;
        try { epoch = Long.parseLong(fields[1]); }
        catch (NumberFormatException failure) { throw new IOException("invalid refusal epoch", failure); }
        if (!Set.of("four_clause_report", "synthesis").contains(fields[2])) {
            throw new IOException("invalid refusal stage");
        }
        refusalsByContributor.computeIfAbsent(fields[0], ignored -> new ArrayList<>()).add(epoch);
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
        conclusionsByFinding.clear();
        contributorsByFinding.clear();
        refusalsByContributor.clear();
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

    public static final class ReportRefusedException extends IllegalStateException {
        public ReportRefusedException(String message) { super(message); }
    }

    public static final class FilingThrottleException extends IllegalStateException {
        public FilingThrottleException(String message) { super(message); }
    }

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
