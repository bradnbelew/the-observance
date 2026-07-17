package com.observance.watcher.m3runtime;

import com.observance.watcher.m2runtime.LocalPrimaryJournal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Local-primary evidence custody, exact conclusions, synthesis, and A2 state for M3 vNext. */
public final class PrivateSliceState {
    public static final List<String> BASE_FINDINGS = List.of("P4.F1", "P4.F2", "P4.F3", "P4.F4");
    public static final String SYNTHESIS = "P4.F5";
    public static final String GATE_IDEMPOTENCY = "m3:p4:gate:P4_INTAKE_GATE:open:vnext";
    public static final int REFUSAL_WINDOW_SECONDS = 300;
    public static final int MAX_REFUSALS_PER_WINDOW = 3;
    public static final String WATCHER_PAYLOAD_SHA256 =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";

    /** Custody surfaces are useful but never answer prerequisites. */
    public static final Map<String, Set<String>> ALLOWED_SOURCES = Map.of(
            "P4.F1", Set.of("child_copybook", "early_smoke_notice"),
            "P4.F2", Set.of("hinge_repair", "market_note"),
            "P4.F3", Set.of("early_smoke_notice", "late_attendance_ruling"),
            "P4.F4", Set.of("bell_register", "node_clock_extract"));

    public static final Map<String, String> EXACT_CONCLUSIONS = Map.of(
            "P4.F1", "THE MOUTH MARKS WERE SHIFT INSTRUCTIONS",
            "P4.F2", "THE HOLD SHELTERED FAMILIES NOT INITIATES",
            "P4.F3", "THE LATER COPY TURNED ADVICE INTO DUTY",
            "P4.F4", "THE REGISTER REPEATED BEFORE THE BELL",
            "P4.F5", "REFUGE BEFORE RITE SAFETY BECAME OBEDIENCE");

    public static final Map<String, Set<String>> ACCEPTED_CONCLUSIONS = Map.of(
            "P4.F1", normalizedSet("THE MOUTH MARKS WERE SHIFT INSTRUCTIONS",
                    "THE MARKS WERE WORK INSTRUCTIONS", "THE MARKS WERE SAFETY INSTRUCTIONS"),
            "P4.F2", normalizedSet("THE HOLD SHELTERED FAMILIES NOT INITIATES",
                    "THE HOLD WAS A LIVED IN REFUGE", "FAMILIES LIVED IN THE HOLD"),
            "P4.F3", normalizedSet("THE LATER COPY TURNED ADVICE INTO DUTY",
                    "SAFETY ADVICE BECAME ENFORCED DUTY", "THE WAYS TURNED SAFETY INTO OBEDIENCE"),
            "P4.F4", normalizedSet("THE REGISTER REPEATED BEFORE THE BELL",
                    "THE RECORD COPIED THE ENTRY BEFORE IT WAS WRITTEN", "THE COPY PREDATES THE SOURCE"),
            "P4.F5", normalizedSet("REFUGE BEFORE RITE SAFETY BECAME OBEDIENCE",
                    "THE HOLD WAS A REFUGE BEFORE IT WAS A RITE SAFETY BECAME OBEDIENCE",
                    "THE HOLD WAS A RATIONAL REFUGE BEFORE RITUAL INSTITUTION"));

    public static final Map<String, String> MEANINGFUL_NON_DOOR_READS = Map.of(
            normalizeAnswer("THE MOUTH WAS A PUBLIC ROAD"),
            "The road is real. It does not explain the children, the altered duty, or the impossible copy.",
            normalizeAnswer("VENT EAST BEFORE SECOND BELL"),
            "That instruction survives. The inquiry asks what kind of language it was, and what later hands made it do.",
            normalizeAnswer("THE HOLD HAD 294 PLACES"),
            "The count belongs to the refuge. A capacity does not explain what the Mouth became.",
            normalizeAnswer("THE REGISTER CLOCK WAS WRONG"),
            "A reasonable fault theory. The independent node and cartridge sequence do not move with that clock.");

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
                "m3:p4:observation:" + findingId + ":" + sourceId + ":vnext", "observation_committed",
                bytes(findingId + "\n" + sourceId));
        journal.append("m3:p4:observation-contribution:" + findingId + ":" + sourceId + ":" + contributorId + ":vnext",
                "observation_contribution_recorded", bytes(findingId + "\n" + sourceId + "\n" + contributorId));
        reload();
        return observation;
    }

    /** Stores working wording only; it does not require or infer source observations. */
    public synchronized void selectDraft(String findingId, String answer, String contributorId) {
        requireFinding(findingId);
        requireContributor(contributorId);
        String normalized = normalizeAnswer(answer);
        if (normalized.isBlank()) throw new IllegalArgumentException("a concise conclusion is required");
        draftsByContributor.computeIfAbsent(contributorId, ignored -> new LinkedHashMap<>())
                .put(findingId, normalized);
    }

    public synchronized Map<String, String> draft(String contributorId) {
        requireContributor(contributorId);
        return Map.copyOf(draftsByContributor.getOrDefault(contributorId, Map.of()));
    }

    /** Files one defensible conclusion. Correct wording passes even with zero observation receipts. */
    public synchronized LocalPrimaryJournal.Receipt lodgeFinding(
            String findingId, String answer, String contributorId, long nowEpochSecond) throws IOException {
        requireBaseFinding(findingId);
        requireContributor(contributorId);
        if (findingCommitted(findingId)) throw new IllegalStateException("that finding is already on file");
        enforceRefusalThrottle(contributorId, nowEpochSecond);
        String normalized = normalizeAnswer(answer);
        if (!ACCEPTED_CONCLUSIONS.get(findingId).contains(normalized)) {
            recordRefusal(contributorId, nowEpochSecond, "finding:" + findingId);
            throw new ReportRefusedException(refusalMessage(normalized));
        }
        LocalPrimaryJournal.Receipt receipt = appendFinding(findingId,
                observationsByFinding.getOrDefault(findingId, Set.of()),
                EXACT_CONCLUSIONS.get(findingId), contributorId);
        reload();
        return receipt;
    }

    /** Atomic harness path for all four findings; still independent of observation custody. */
    public synchronized List<LocalPrimaryJournal.Receipt> lodgeReport(
            String contributorId, long nowEpochSecond) throws IOException {
        requireContributor(contributorId);
        enforceRefusalThrottle(contributorId, nowEpochSecond);
        Map<String, String> draft = draftsByContributor.getOrDefault(contributorId, Map.of());
        boolean correct = BASE_FINDINGS.stream().allMatch(finding ->
                ACCEPTED_CONCLUSIONS.get(finding).contains(draft.getOrDefault(finding, "")));
        if (!correct) {
            recordRefusal(contributorId, nowEpochSecond, "four_finding_report");
            draftsByContributor.remove(contributorId);
            throw new ReportRefusedException("The report is returned as one account. A retrieved phrase is not yet an explanation.");
        }
        List<LocalPrimaryJournal.Receipt> committed = new ArrayList<>();
        for (String finding : BASE_FINDINGS) {
            if (!findingCommitted(finding)) {
                committed.add(appendFinding(finding,
                        observationsByFinding.getOrDefault(finding, Set.of()),
                        EXACT_CONCLUSIONS.get(finding), contributorId));
            }
        }
        draftsByContributor.remove(contributorId);
        reload();
        return List.copyOf(committed);
    }

    public synchronized LocalPrimaryJournal.Receipt lodgeSynthesis(
            String contributorId, long nowEpochSecond) throws IOException {
        requireContributor(contributorId);
        if (!sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
            throw new IllegalStateException("four findings must be on file before the account can close");
        }
        enforceRefusalThrottle(contributorId, nowEpochSecond);
        String submitted = draftsByContributor.getOrDefault(contributorId, Map.of()).getOrDefault(SYNTHESIS, "");
        if (!ACCEPTED_CONCLUSIONS.get(SYNTHESIS).contains(submitted)) {
            recordRefusal(contributorId, nowEpochSecond, "synthesis");
            Map<String, String> draft = draftsByContributor.get(contributorId);
            if (draft != null) draft.remove(SYNTHESIS);
            throw new ReportRefusedException(refusalMessage(submitted));
        }
        LocalPrimaryJournal.Receipt receipt = appendFinding(SYNTHESIS,
                new LinkedHashSet<>(BASE_FINDINGS), EXACT_CONCLUSIONS.get(SYNTHESIS), contributorId);
        journal.append(GATE_IDEMPOTENCY, "gate_opened", bytes("P4_INTAKE_GATE"));
        draftsByContributor.remove(contributorId);
        reload();
        return receipt;
    }

    private LocalPrimaryJournal.Receipt appendFinding(String findingId, Set<String> sources,
            String canonicalConclusion, String contributorId) throws IOException {
        if (!EXACT_CONCLUSIONS.get(findingId).equals(canonicalConclusion)) {
            throw new IllegalArgumentException("unsupported conclusion");
        }
        List<String> sorted = new ArrayList<>(sources);
        sorted.sort(String::compareTo);
        LocalPrimaryJournal.Receipt finding = journal.append(
                "m3:p4:finding:" + findingId + ":vnext", "finding_committed",
                bytes(findingId + "\n" + String.join(",", sorted) + "\n" + canonicalConclusion));
        journal.append("m3:p4:contribution:" + findingId + ":" + contributorId + ":vnext",
                "contribution_recorded", bytes(findingId + "\n" + contributorId));
        return finding;
    }

    private void enforceRefusalThrottle(String contributorId, long nowEpochSecond) {
        long floor = nowEpochSecond - REFUSAL_WINDOW_SECONDS;
        long recent = refusalsByContributor.getOrDefault(contributorId, List.of()).stream()
                .filter(epoch -> epoch >= floor).count();
        if (recent >= MAX_REFUSALS_PER_WINDOW) {
            throw new FilingThrottleException("Returned papers are under review; the desk will reopen shortly.");
        }
    }

    private void recordRefusal(String contributorId, long nowEpochSecond, String stage) throws IOException {
        long ordinal = journal.after(0).size() + 1L;
        journal.append("m3:p4:report-refused:" + contributorId + ":" + ordinal + ":vnext",
                "report_refused", bytes(contributorId + "\n" + nowEpochSecond + "\n" + stage));
        reload();
    }

    public static String refusalMessage(String answer) {
        return MEANINGFUL_NON_DOOR_READS.getOrDefault(normalizeAnswer(answer),
                "That account may fit one surviving detail, but it does not yet explain the disagreement between copies.");
    }

    public synchronized WatcherApproval approveWatcher(String approvalId, String westTarget,
            String eastTarget, long expiresAtEpochSecond, long nowEpochSecond) throws IOException {
        requireToken(approvalId, "approval id");
        requireToken(westTarget, "west target");
        requireToken(eastTarget, "east target");
        if (westTarget.equalsIgnoreCase(eastTarget)) throw new IllegalArgumentException("A2 targets must be distinct");
        if (!findingCommitted("P4.F3") || findingCommitted(SYNTHESIS)) {
            throw new IllegalStateException("A2 approval requires the changed-copy finding and an unreleased seal");
        }
        long duration = expiresAtEpochSecond - nowEpochSecond;
        if (duration < 60 || duration > 1800) throw new IllegalArgumentException("A2 approval must expire in 1-30 minutes");
        WatcherApproval approval = new WatcherApproval(approvalId, WATCHER_PAYLOAD_SHA256,
                "m3-private-slice-vnext/named-test-players", westTarget, eastTarget, expiresAtEpochSecond);
        journal.append("m3:p4:watcher-approval:" + approvalId + ":vnext", "watcher_approval_recorded",
                bytes(approval.serialize()));
        reload();
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
        journal.append("m3:p4:watcher-approval:" + approvalId + ":consumed:vnext",
                "watcher_approval_consumed", bytes(approval.serialize()));
        reload();
        return approval;
    }

    public synchronized boolean findingCommitted(String findingId) { return sourcesByFinding.containsKey(findingId); }
    public synchronized String committedConclusion(String findingId) { return conclusionsByFinding.get(findingId); }
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
    public synchronized List<LocalPrimaryJournal.Receipt> catchUpAfter(long sequence) { return journal.after(sequence); }

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
        if (fields.length != 3) throw new IOException("invalid M3 vNext finding receipt");
        requireFinding(fields[0]);
        Set<String> sources = fields[1].isBlank() ? new LinkedHashSet<>()
                : normalizeSources(List.of(fields[1].split(",", -1)));
        if (!EXACT_CONCLUSIONS.get(fields[0]).equals(fields[2])) {
            throw new IOException("finding conclusion is not the authored exact conclusion");
        }
        if (SYNTHESIS.equals(fields[0])) {
            if (!sources.equals(new LinkedHashSet<>(BASE_FINDINGS))
                    || !sourcesByFinding.keySet().containsAll(BASE_FINDINGS)) {
                throw new IOException("invalid M3 synthesis ordering/provenance");
            }
        } else {
            if (!ALLOWED_SOURCES.get(fields[0]).containsAll(sources)) {
                throw new IOException("finding cites an unauthored source");
            }
            if (!observationsByFinding.getOrDefault(fields[0], Set.of()).containsAll(sources)) {
                throw new IOException("finding cites custody that was never observed");
            }
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
        if (!(fields[2].equals("four_finding_report") || fields[2].equals("synthesis")
                || fields[2].startsWith("finding:P4.F"))) {
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

    private void reload() throws IOException {
        resetDerivedState();
        replay(journal.after(0));
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

    public static String normalizeAnswer(String answer) {
        if (answer == null) return "";
        String normalized = Normalizer.normalize(answer, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
        return normalized.replaceAll("[^A-Z0-9]+", " ").trim().replaceAll(" +", " ");
    }

    private static Set<String> normalizedSet(String... answers) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String answer : answers) set.add(normalizeAnswer(answer));
        return Set.copyOf(set);
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

    private static void requireContributor(String contributorId) { requireToken(contributorId, "contributor identity"); }

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
