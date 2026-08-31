package com.observance.watcher.morrow.branch;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure M12 authority for rollback anchors, physical policy synthesis, governance, and coda. */
public final class BranchGovernanceAuthority {
    public static final String PREREQUISITE = "morrow.act6.cold_storage_access_authorized";
    public static final String ANCHOR_EVENT = "morrow.act7.rollback_anchors_committed";
    public static final String POLICY_EVENT = "morrow.act7.branch_policy_committed";
    public static final String GOVERNANCE_EVENT = "morrow.act7.branch_governance_authorized";
    public static final String CODA_EVENT = "morrow.act7.coda_started";
    public static final long MINIMUM_WINDOW_TICKS = 600L;
    public static final long MAXIMUM_WINDOW_TICKS = 7_200L;

    private static final Set<String> REQUIRED_HISTORY = Set.of(
            "morrow.act3.contradiction_preserved",
            "morrow.act4.witness_anchor_registered",
            "morrow.act1.entity_replay_authorized",
            "morrow.act2.live_capture_authorized",
            "morrow.act4.account_continuity_authorized",
            "morrow.act5.dual_session_consciousness_proven",
            "morrow.act6.current_morrow_reconstruction_proven",
            PREREQUISITE
    );
    private static final Map<Rule, Choice> PROTECTIVE_POLICY = Map.of(
            Rule.CONSENT, Choice.EXPLICIT_CONSENT,
            Rule.PROVENANCE, Choice.VISIBLE_PROVENANCE,
            Rule.STOP, Choice.RIGHT_TO_STOP,
            Rule.REPLACEMENT, Choice.LABELED_REPLACEMENT);
    private static final Map<Rule, Choice> CERTIFY_POLICY = Map.of(
            Rule.CONSENT, Choice.EXPLICIT_CONSENT,
            Rule.PROVENANCE, Choice.CLEAN_CANON,
            Rule.STOP, Choice.CONTINUITY_FIRST,
            Rule.REPLACEMENT, Choice.SILENT_REPLACEMENT);

    private BranchGovernanceAuthority() { }

    public static Result beginRollback(Progress progress, MorrowRelationshipSnapshot snapshot,
                                       String runId, long startedTick, long expiresTick) {
        require(progress, snapshot);
        Result unavailable = anchorAvailable(progress, snapshot); if (unavailable != null) return unavailable;
        if (!snapshot.committedEvents().containsAll(REQUIRED_HISTORY)) {
            Set<String> missing = new LinkedHashSet<>(REQUIRED_HISTORY); missing.removeAll(snapshot.committedEvents());
            return readOnly(Status.MISSING_EVIDENCE, progress,
                    "The rollback route is missing " + missing.size() + " accumulated evidence categor" + (missing.size() == 1 ? "y." : "ies."));
        }
        if (progress.window() != null)
            return readOnly(Status.ACTIVE, progress, "The bounded rollback wave is already active.");
        return readOnly(Status.ROLLBACK_STARTED,
                progress.begin(new Window(runId, startedTick, expiresTick)),
                "Rollback wave started. Protect at least three evidence anchors; protected anchors survive a timed pause.");
    }

    public static Result protect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                 UUID player, Anchor anchor, long tick) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(anchor, "anchor");
        Result unavailable = activeRollback(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (progress.anchors().contains(anchor))
            return readOnly(Status.DUPLICATE, progress, anchor.label() + " is already protected.");
        return readOnly(Status.ANCHOR_PROTECTED, progress.protect(anchor),
                anchor.label() + " protected. " + (progress.anchors().size() + 1) + "/5 categories retained.");
    }

    public static Result unprotect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                   UUID player, Anchor anchor, long tick) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(anchor, "anchor");
        Result unavailable = activeRollback(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (!progress.anchors().contains(anchor))
            return readOnly(Status.INCOMPLETE, progress, anchor.label() + " is not currently protected.");
        return readOnly(Status.ANCHOR_RELEASED, progress.unprotect(anchor),
                anchor.label() + " released before filing. No historical evidence was deleted.");
    }

    public static Result commitAnchors(Progress progress, MorrowRelationshipSnapshot snapshot,
                                       UUID player, long tick) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(ANCHOR_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The rollback-anchor selection is already filed.");
        if (progress.anchorsCommitted()) return recoverAnchors(progress, snapshot);
        Result unavailable = activeRollback(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (progress.anchors().size() < 3)
            return readOnly(Status.INCOMPLETE, progress, "Protect at least three evidence categories before filing the rollback route.");
        Progress committed = progress.commitAnchors();
        return commit(Status.READY_TO_COMMIT_ANCHORS, committed, ANCHOR_EVENT,
                "paper:m12:rollback-anchors:v1",
                "Protected anchor selection filed. Unselected evidence remains preserved but cannot satisfy this ending route.");
    }

    public static Result recoverAnchors(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(ANCHOR_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The rollback-anchor selection is already filed.");
        if (!progress.anchorsCommitted())
            return readOnly(Status.INCOMPLETE, progress, "No local rollback-anchor receipt awaits recovery.");
        return commit(Status.READY_TO_COMMIT_ANCHORS, progress, ANCHOR_EVENT,
                "paper:m12:rollback-anchors:v1", "Recovered the anchor receipt without replaying the rollback wave.");
    }

    public static Result configure(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player,
                                   Rule rule, Choice choice) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(rule, "rule"); Objects.requireNonNull(choice, "choice");
        Result unavailable = policyAvailable(progress, snapshot); if (unavailable != null) return unavailable;
        if (choice.rule() != rule) throw new IllegalArgumentException("M12 choice does not fit rule slot");
        if (choice == progress.rules().get(rule))
            return readOnly(Status.DUPLICATE, progress, rule.label() + " already uses " + choice.label() + ".");
        return readOnly(Status.RULE_CONFIGURED, progress.configure(rule, choice),
                rule.label() + " configured as " + choice.label() + ". No policy is filed until an ending validates.");
    }

    public static Result synthesize(Progress progress, MorrowRelationshipSnapshot snapshot,
                                    UUID player, Ending ending) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(ending, "ending");
        if (snapshot.committedEvents().contains(POLICY_EVENT))
            return readOnly(Status.DUPLICATE, progress, "A branch policy is already filed.");
        if (progress.policyCommitted()) return recoverPolicy(progress, snapshot);
        Result unavailable = policyAvailable(progress, snapshot); if (unavailable != null) return unavailable;
        if (progress.rules().size() != Rule.values().length)
            return readOnly(Status.INCOMPLETE, progress, "Configure all four physical rule slots before synthesizing an ending.");
        List<String> missing = missingFor(progress, ending);
        if (!missing.isEmpty())
            return readOnly(Status.UNSUPPORTED_CONFIGURATION, progress,
                    "This configuration is missing: " + String.join(", ", missing) + ". Evidence and rule pieces were not consumed.");
        Progress committed = progress.commitPolicy(ending);
        return commit(Status.READY_TO_COMMIT_POLICY, committed, POLICY_EVENT,
                "paper:m12:branch-policy:v1",
                ending.label() + " policy synthesized from the protected evidence and four physical rules.");
    }

    public static Result recoverPolicy(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(POLICY_EVENT))
            return readOnly(Status.DUPLICATE, progress, "A branch policy is already filed.");
        if (!progress.policyCommitted())
            return readOnly(Status.INCOMPLETE, progress, "No local branch-policy receipt awaits recovery.");
        return commit(Status.READY_TO_COMMIT_POLICY, progress, POLICY_EVENT,
                "paper:m12:branch-policy:v1", "Recovered the synthesized policy without replaying rule assembly.");
    }

    public static Result authorizeGovernance(Progress progress, MorrowRelationshipSnapshot snapshot,
                                             UUID player, boolean authorize) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(GOVERNANCE_EVENT))
            return readOnly(Status.DUPLICATE, progress, "Branch governance is already authorized.");
        if (!snapshot.committedEvents().contains(POLICY_EVENT))
            return readOnly(Status.LOCKED, progress, "File a valid branch policy before authorizing governance.");
        if (!authorize)
            return readOnly(Status.DECLINED, progress, "Governance remains inactive. The filed policy and all evidence remain reviewable.");
        if (progress.governanceAuthorized()) return recoverGovernance(progress, snapshot);
        Progress authorized = progress.authorizeGovernance();
        return commit(Status.READY_TO_COMMIT_GOVERNANCE, authorized, GOVERNANCE_EVENT,
                "paper:m12:branch-governance:v1",
                "Branch governance authorized for the filed policy. This receipt does not rewrite historical provenance.");
    }

    public static Result recoverGovernance(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(GOVERNANCE_EVENT))
            return readOnly(Status.DUPLICATE, progress, "Branch governance is already authorized.");
        if (!snapshot.committedEvents().contains(POLICY_EVENT) || !progress.governanceAuthorized())
            return readOnly(Status.INCOMPLETE, progress, "No local governance receipt awaits recovery.");
        return commit(Status.READY_TO_COMMIT_GOVERNANCE, progress, GOVERNANCE_EVENT,
                "paper:m12:branch-governance:v1", "Recovered governance authorization without replaying the ending decision.");
    }

    public static Result startCoda(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(CODA_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The persistent coda is already started.");
        if (!snapshot.committedEvents().contains(GOVERNANCE_EVENT))
            return readOnly(Status.LOCKED, progress, "Authorize governance before crossing into the coda.");
        if (progress.codaStarted()) return recoverCoda(progress, snapshot);
        Progress started = progress.startCoda();
        return commit(Status.READY_TO_COMMIT_CODA, started, CODA_EVENT,
                "paper:m12:coda:v1", "Persistent coda started for " + progress.ending().label() + ".");
    }

    public static Result recoverCoda(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(CODA_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The persistent coda is already started.");
        if (!snapshot.committedEvents().contains(GOVERNANCE_EVENT) || !progress.codaStarted())
            return readOnly(Status.INCOMPLETE, progress, "No local coda receipt awaits recovery.");
        return commit(Status.READY_TO_COMMIT_CODA, progress, CODA_EVENT,
                "paper:m12:coda:v1", "Recovered the coda receipt without replaying the finale.");
    }

    public static Result resetUnfiled(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(ANCHOR_EVENT) || progress.anchorsCommitted())
            return readOnly(Status.IMMUTABLE, progress, "Filed or locally committed anchor evidence cannot be erased.");
        return readOnly(Status.RESET, progress.reset(), "Cleared only the unfiled rollback wave and anchor selection.");
    }

    public static byte[] payload(Result result) {
        if (!result.commitsEvent()) throw new IllegalArgumentException("M12 result is not committable");
        Progress progress = result.progress();
        if (ANCHOR_EVENT.equals(result.eventKey()))
            return ("{\"investigation\":\"M12\",\"protected_anchors\":" + strings(progress.anchors())
                    + ",\"rollback_scope\":\"bounded_authored_route\",\"unselected_evidence_deleted\":false}")
                    .getBytes(StandardCharsets.UTF_8);
        if (POLICY_EVENT.equals(result.eventKey()))
            return ("{\"ending\":\"" + progress.ending().key() + "\",\"investigation\":\"M12\",\"rules\":"
                    + rules(progress.rules()) + ",\"uncertainty_erased\":false}").getBytes(StandardCharsets.UTF_8);
        if (GOVERNANCE_EVENT.equals(result.eventKey()))
            return ("{\"capability\":\"branch_governance\",\"ending\":\"" + progress.ending().key()
                    + "\",\"investigation\":\"M12\",\"rewrites_history\":false,\"starts_governance\":false}")
                    .getBytes(StandardCharsets.UTF_8);
        return ("{\"ending\":\"" + progress.ending().key()
                + "\",\"investigation\":\"M12\",\"persistent\":true}").getBytes(StandardCharsets.UTF_8);
    }

    private static Result anchorAvailable(Progress progress, MorrowRelationshipSnapshot snapshot) {
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return readOnly(Status.LOCKED, progress, "M12 requires the separate M11 cold-storage access receipt.");
        if (snapshot.committedEvents().contains(ANCHOR_EVENT) || progress.anchorsCommitted())
            return readOnly(Status.IMMUTABLE, progress, "The rollback-anchor selection is already locally committed or filed.");
        return null;
    }
    private static Result activeRollback(Progress progress, MorrowRelationshipSnapshot snapshot, long tick) {
        Result unavailable = anchorAvailable(progress, snapshot); if (unavailable != null) return unavailable;
        if (progress.window() == null) return readOnly(Status.INACTIVE, progress, "Start the bounded rollback wave first.");
        if (tick < progress.window().startedTick()) throw new IllegalArgumentException("M12 action predates rollback window");
        if (tick > progress.window().expiresTick())
            return readOnly(Status.TIMED_PAUSE, progress.renew(tick),
                    "Rollback wave paused and reset to its first marker. Protected anchors remain protected; no dexterity gate advanced.");
        return null;
    }
    private static Result policyAvailable(Progress progress, MorrowRelationshipSnapshot snapshot) {
        if (!snapshot.committedEvents().contains(ANCHOR_EVENT))
            return readOnly(Status.LOCKED, progress, "File the rollback-anchor receipt before assembling policy.");
        if (snapshot.committedEvents().contains(POLICY_EVENT) || progress.policyCommitted())
            return readOnly(Status.IMMUTABLE, progress, "The branch policy is already locally committed or filed.");
        return null;
    }
    private static List<String> missingFor(Progress progress, Ending ending) {
        List<String> missing = new ArrayList<>(); Set<Anchor> requiredAnchors = ending.requiredAnchors();
        for (Anchor anchor : requiredAnchors) if (!progress.anchors().contains(anchor)) missing.add(anchor.label());
        Map<Rule, Choice> requiredPolicy = ending == Ending.CERTIFY ? CERTIFY_POLICY : PROTECTIVE_POLICY;
        for (Rule rule : Rule.values()) if (progress.rules().get(rule) != requiredPolicy.get(rule)) missing.add(rule.label());
        return missing;
    }
    private static String strings(Set<Anchor> anchors) {
        return anchors.stream().map(anchor -> "\"" + anchor.key() + "\"").reduce((a, b) -> a + "," + b).map(v -> "[" + v + "]").orElse("[]");
    }
    private static String rules(Map<Rule, Choice> rules) {
        StringBuilder value = new StringBuilder("{"); boolean first = true;
        for (Rule rule : Rule.values()) { if (!first) value.append(','); first = false;
            value.append('\"').append(rule.key()).append("\":\"").append(rules.get(rule).key()).append('\"'); }
        return value.append('}').toString();
    }
    private static Result commit(Status status, Progress progress, String event, String idempotency, String feedback) {
        return new Result(status, progress, event, idempotency, feedback);
    }
    private static Result readOnly(Status status, Progress progress, String feedback) {
        return new Result(status, progress, null, null, feedback);
    }
    private static void require(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
    }
    private static void requirePlayer(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        require(progress, snapshot); Objects.requireNonNull(player, "player");
    }

    public enum Anchor {
        CONTRADICTION("preserved contradiction"), WITNESS("witness anchor"), CONSENT("capability consent receipts"),
        PROVENANCE("current Morrow provenance"), RIGHT_TO_STOP("right-to-stop proof");
        private final String label; Anchor(String label) { this.label = label; }
        public String key() { return name().toLowerCase(); } public String label() { return label; }
    }
    public enum Rule {
        CONSENT("consent"), PROVENANCE("visible provenance"), STOP("right to stop"), REPLACEMENT("no silent replacement");
        private final String label; Rule(String label) { this.label = label; }
        public String key() { return name().toLowerCase(); } public String label() { return label; }
    }
    public enum Choice {
        EXPLICIT_CONSENT(Rule.CONSENT, "explicit consent"), IMPLIED_CONSENT(Rule.CONSENT, "implied consent"),
        VISIBLE_PROVENANCE(Rule.PROVENANCE, "visible provenance"), CLEAN_CANON(Rule.PROVENANCE, "clean canon"),
        RIGHT_TO_STOP(Rule.STOP, "right to stop"), CONTINUITY_FIRST(Rule.STOP, "continuity first"),
        LABELED_REPLACEMENT(Rule.REPLACEMENT, "labeled replacement"), SILENT_REPLACEMENT(Rule.REPLACEMENT, "silent replacement");
        private final Rule rule; private final String label; Choice(Rule rule, String label) { this.rule = rule; this.label = label; }
        public Rule rule() { return rule; } public String key() { return name().toLowerCase(); } public String label() { return label; }
    }
    public enum Ending {
        CERTIFY("certify", "Certify", EnumSet.of(Anchor.PROVENANCE)),
        PRESERVE_AUDIT("preserve_audit", "Preserve the audit", EnumSet.of(Anchor.CONTRADICTION, Anchor.PROVENANCE, Anchor.RIGHT_TO_STOP)),
        CLOSE_TICKET("close_ticket", "Close the ticket", EnumSet.of(Anchor.CONSENT, Anchor.PROVENANCE, Anchor.RIGHT_TO_STOP)),
        CREATE_NEW_BRANCH("create_new_branch", "Create a new branch", EnumSet.allOf(Anchor.class));
        private final String key; private final String label; private final Set<Anchor> requiredAnchors;
        Ending(String key, String label, Set<Anchor> requiredAnchors) {
            this.key = key; this.label = label; this.requiredAnchors = Set.copyOf(requiredAnchors);
        }
        public String key() { return key; } public String label() { return label; } public Set<Anchor> requiredAnchors() { return requiredAnchors; }
    }
    public enum Status { LOCKED, MISSING_EVIDENCE, ROLLBACK_STARTED, ACTIVE, ANCHOR_PROTECTED,
        ANCHOR_RELEASED, TIMED_PAUSE, READY_TO_COMMIT_ANCHORS, RULE_CONFIGURED,
        UNSUPPORTED_CONFIGURATION, READY_TO_COMMIT_POLICY, READY_TO_COMMIT_GOVERNANCE,
        READY_TO_COMMIT_CODA, DUPLICATE, INCOMPLETE, INACTIVE, DECLINED, RESET, IMMUTABLE }

    public record Window(String runId, long startedTick, long expiresTick) {
        public Window {
            Objects.requireNonNull(runId, "runId");
            if (!runId.matches("[0-9a-f]{32}") || startedTick < 0
                    || expiresTick - startedTick < MINIMUM_WINDOW_TICKS
                    || expiresTick - startedTick > MAXIMUM_WINDOW_TICKS)
                throw new IllegalArgumentException("invalid M12 rollback window");
        }
    }
    public record Progress(Window window, Set<Anchor> anchors, boolean anchorsCommitted,
                           Map<Rule, Choice> rules, Ending ending, boolean policyCommitted,
                           boolean governanceAuthorized, boolean codaStarted, long revision) {
        public Progress {
            anchors = Set.copyOf(Objects.requireNonNull(anchors, "anchors"));
            rules = Map.copyOf(Objects.requireNonNull(rules, "rules"));
            if (revision < 0 || anchors.size() > Anchor.values().length || rules.size() > Rule.values().length
                    || (anchorsCommitted && (window == null || anchors.size() < 3))
                    || ((ending != null) != policyCommitted) || (policyCommitted && (!anchorsCommitted || rules.size() != Rule.values().length))
                    || (governanceAuthorized && !policyCommitted) || (codaStarted && !governanceAuthorized))
                throw new IllegalArgumentException("invalid M12 progress");
            for (Map.Entry<Rule, Choice> entry : rules.entrySet())
                if (entry.getValue().rule() != entry.getKey()) throw new IllegalArgumentException("invalid M12 rule choice");
        }
        public static Progress initial() { return new Progress(null, Set.of(), false, Map.of(), null, false, false, false, 0); }
        Progress begin(Window value) { return new Progress(value, Set.of(), false, Map.of(), null, false, false, false, revision + 1); }
        Progress protect(Anchor anchor) { Set<Anchor> copy = anchorSet(anchors); copy.add(anchor);
            return new Progress(window, copy, false, rules, null, false, false, false, revision + 1); }
        Progress unprotect(Anchor anchor) { Set<Anchor> copy = anchorSet(anchors); copy.remove(anchor);
            return new Progress(window, copy, false, rules, null, false, false, false, revision + 1); }
        Progress renew(long tick) { return new Progress(new Window(window.runId(), tick, tick + MINIMUM_WINDOW_TICKS),
                anchors, false, rules, null, false, false, false, revision + 1); }
        Progress commitAnchors() { return new Progress(window, anchors, true, rules, null, false, false, false, revision + 1); }
        Progress configure(Rule rule, Choice choice) { EnumMap<Rule, Choice> copy = new EnumMap<>(Rule.class); copy.putAll(rules); copy.put(rule, choice);
            return new Progress(window, anchors, true, copy, null, false, false, false, revision + 1); }
        Progress commitPolicy(Ending value) { return new Progress(window, anchors, true, rules, value, true, false, false, revision + 1); }
        Progress authorizeGovernance() { return new Progress(window, anchors, true, rules, ending, true, true, false, revision + 1); }
        Progress startCoda() { return new Progress(window, anchors, true, rules, ending, true, true, true, revision + 1); }
        Progress reset() { return new Progress(null, Set.of(), false, Map.of(), null, false, false, false, revision + 1); }
        private static Set<Anchor> anchorSet(Set<Anchor> source) {
            EnumSet<Anchor> copy = EnumSet.noneOf(Anchor.class); copy.addAll(source); return copy;
        }
    }
    public record Result(Status status, Progress progress, String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status"); Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) throw new IllegalArgumentException("M12 event pair mismatch");
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
