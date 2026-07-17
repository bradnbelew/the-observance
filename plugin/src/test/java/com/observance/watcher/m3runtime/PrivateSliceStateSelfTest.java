package com.observance.watcher.m3runtime;

import com.google.gson.JsonObject;
import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.m2runtime.AutomationApprovalPolicy;
import com.observance.watcher.m2runtime.PredicateAuthorityVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Proves vNext free-text deductions, zero-receipt acceptance, replay, and exact A2 approval. */
public final class PrivateSliceStateSelfTest {
    private static final String WATCHER_HASH =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";

    private PrivateSliceStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-m3-vnext-slice-");
        Path path = dir.resolve("slice.journal");
        try {
            PrivateSliceState state = PrivateSliceState.open(path);
            check(!state.gateOpen(), "gate begins closed");

            selectCorrectReport(state, "shared-answer");
            state.lodgeReport("shared-answer", 1_789_000_000L);
            for (String finding : PrivateSliceState.BASE_FINDINGS) {
                check(state.findingCommitted(finding), finding + " accepts a correct shared answer");
                check(state.observedSources(finding).isEmpty(), finding + " requires zero observation receipts");
            }
            check(!state.gateOpen(), "four findings do not bypass synthesis");
            state.selectDraft(PrivateSliceState.SYNTHESIS,
                    "THE HOLD WAS A RATIONAL REFUGE BEFORE RITUAL INSTITUTION", "shared-answer");
            state.lodgeSynthesis("shared-answer", 1_789_000_001L);
            check(state.gateOpen(), "accepted synthesis opens the gate with zero observations");

            PrivateSliceState restarted = PrivateSliceState.open(path);
            check(restarted.gateOpen() && restarted.findingCommitted(PrivateSliceState.SYNTHESIS),
                    "restart re-derives synthesis and gate");
            check(restarted.catchUpAfter(0).stream().noneMatch(receipt ->
                            "observation_committed".equals(receipt.eventType())),
                    "zero-receipt solution does not fabricate custody");

            negativeAndCustodyPaths(dir.resolve("negative.journal"));
            approvalBoundary();
            bookPageBudget();
            protectionSurface();
            System.out.println("M3 private-slice vNext state self-test passed");
        } finally {
            try (var paths = Files.list(dir)) {
                for (Path child : paths.toList()) Files.deleteIfExists(child);
            }
            Files.deleteIfExists(dir);
        }
    }

    private static void negativeAndCustodyPaths(Path path) throws Exception {
        PrivateSliceState state = PrivateSliceState.open(path);
        selectWrongReport(state, "naive");
        expectReportRefusal(() -> state.lodgeReport("naive", 1_789_000_010L));
        check(PrivateSliceState.BASE_FINDINGS.stream().noneMatch(state::findingCommitted),
                "naive phrase copying advances nothing");
        check(PrivateSliceState.refusalMessage("THE REGISTER CLOCK WAS WRONG").contains("fault theory"),
                "true-but-not-door theory receives authored acknowledgement");

        observeAllAnySubset(state);
        selectWrongReport(state, "custody-is-not-correctness");
        expectReportRefusal(() -> state.lodgeReport("custody-is-not-correctness", 1_789_000_020L));
        check(PrivateSliceState.BASE_FINDINGS.stream().noneMatch(state::findingCommitted),
                "touching every source cannot make a wrong report correct");

        for (int attempt = 0; attempt < PrivateSliceState.MAX_REFUSALS_PER_WINDOW; attempt++) {
            selectWrongReport(state, "blind");
            long epoch = 1_789_000_030L + attempt;
            expectReportRefusal(() -> state.lodgeReport("blind", epoch));
        }
        selectCorrectReport(state, "blind");
        expectThrottle(() -> state.lodgeReport("blind", 1_789_000_034L));
        PrivateSliceState afterRestart = PrivateSliceState.open(path);
        check(afterRestart.refusalCount("blind") == PrivateSliceState.MAX_REFUSALS_PER_WINDOW,
                "bounded refusal throttle survives restart");

        selectCorrectReport(afterRestart, "group-note");
        afterRestart.lodgeReport("group-note", 1_789_001_000L);
        check(afterRestart.observedSources("P4.F2").containsAll(List.of("hinge_repair", "market_note")),
                "optional any-subset custody survives accepted deductions");

        long now = 1_789_001_100L;
        afterRestart.approveWatcher("brad-a2-vnext", "WestReviewer", "EastReviewer", now + 600, now);
        expectIllegalState(() -> afterRestart.consumeWatcher(
                "brad-a2-vnext", "Wrong", "EastReviewer", now + 1));
        afterRestart.consumeWatcher("brad-a2-vnext", "WestReviewer", "EastReviewer", now + 1);
        expectIllegalState(() -> afterRestart.consumeWatcher(
                "brad-a2-vnext", "WestReviewer", "EastReviewer", now + 2));

        afterRestart.selectDraft(PrivateSliceState.SYNTHESIS, "THE HOLD HAD 294 PLACES", "group-note");
        expectReportRefusal(() -> afterRestart.lodgeSynthesis("group-note", now + 10));
        check(!afterRestart.gateOpen(), "retrieved fact is not the synthesis");
        long cursor = afterRestart.catchUpAfter(0).size();
        afterRestart.selectDraft(PrivateSliceState.SYNTHESIS,
                PrivateSliceState.EXACT_CONCLUSIONS.get(PrivateSliceState.SYNTHESIS), "group-note");
        afterRestart.lodgeSynthesis("group-note", now + 11);
        check(afterRestart.catchUpAfter(cursor).stream()
                        .anyMatch(receipt -> "gate_opened".equals(receipt.eventType())),
                "cursor catch-up contains physical gate receipt");
    }

    private static void observeAllAnySubset(PrivateSliceState state) throws Exception {
        state.commitObservation("P4.F1", "child_copybook", "orris");
        state.commitObservation("P4.F1", "early_smoke_notice", "sela");
        state.commitObservation("P4.F2", "hinge_repair", "toma");
        state.commitObservation("P4.F2", "market_note", "neri");
        state.commitObservation("P4.F3", "early_smoke_notice", "eda");
        state.commitObservation("P4.F3", "late_attendance_ruling", "iven");
        state.commitObservation("P4.F4", "bell_register", "lio");
        state.commitObservation("P4.F4", "node_clock_extract", "brann");
    }

    private static void selectWrongReport(PrivateSliceState state, String contributor) {
        PrivateSliceState.BASE_FINDINGS.forEach(finding ->
                state.selectDraft(finding, "COPY THE NEAREST PHRASE", contributor));
    }

    private static void selectCorrectReport(PrivateSliceState state, String contributor) {
        PrivateSliceState.BASE_FINDINGS.forEach(finding -> state.selectDraft(finding,
                PrivateSliceState.EXACT_CONCLUSIONS.get(finding), contributor));
    }

    private static void approvalBoundary() {
        JsonObject authored = new JsonObject();
        authored.addProperty("moment_id", "INTAKE_TALLY_RETENTION");
        authored.addProperty("west_view", "one copied capacity digit appears freshly overwritten");
        authored.addProperty("east_view", "the same digit remains worn and unchanged");
        authored.addProperty("meaning_boundary",
                "observation is selective; no source explains the Dark, Record, Watcher, or Averyn");
        check(WATCHER_HASH.equals(PredicateAuthorityVersion.semanticSha256(authored)),
                "authored Watcher payload hash matches M3 authority");

        Instant now = Instant.parse("2026-07-16T03:00:00Z");
        AutomationApprovalPolicy policy = new AutomationApprovalPolicy(Clock.fixed(now, ZoneOffset.UTC));
        BeatQueueRow missing = new BeatQueueRow();
        missing.type = "name_on_wall";
        missing.payload = authored;
        check(!policy.permitsQueued(missing), "A2 moment fails closed without approval envelope");

        BeatQueueRow approved = new BeatQueueRow();
        approved.type = "name_on_wall";
        JsonObject envelope = new JsonObject();
        envelope.addProperty("approval_id", "m3-brad-private-review-1");
        envelope.addProperty("approval_class", "A2");
        envelope.addProperty("approval_scope", "m3-private-slice/session-1/named-test-players");
        envelope.addProperty("authored_payload_sha256", WATCHER_HASH);
        envelope.add("authored_payload", authored);
        envelope.addProperty("approval_expires_at", "2026-07-16T04:00:00Z");
        approved.payload = envelope;
        check(policy.permitsQueued(approved), "exact unexpired A2 review approval passes");
        authored.addProperty("west_view", "changed");
        check(!policy.permitsQueued(approved), "changed A2 payload fails exact approval");
    }

    private static void bookPageBudget() {
        List<BookPageLayout.EntryPage> pages = new ArrayList<>();
        pages.add(BookPageLayout.entryPage("THE MOUTH MARKS", "P4.F1",
                "What were the repeated marks before they were treated as rites?", false));
        pages.add(BookPageLayout.entryPage("THE PEOPLE RECEIVED", "P4.F2",
                "Who was the Mouth and intake built to receive?", false));
        pages.add(BookPageLayout.entryPage("THE ALTERED COPY", "P4.F3",
                "What changed between the early notice and the later office copy?", false));
        pages.add(BookPageLayout.entryPage("THE EARLY REPEAT", "P4.F4",
                "Why is the repeated register not an ordinary copy or clock fault?", false));
        pages.add(BookPageLayout.entryPage("THE ACCOUNT", "P4.F5",
                "What history do the four findings establish together?", false));
        BookPageLayout.EntryAudit audit = BookPageLayout.entryAudit(pages);
        check(audit.allFit() && audit.uniqueFindings() == 5 && audit.uniqueCommands() == 5,
                "every free-text prompt is render-safe and uniquely reachable");
        check(pages.stream().allMatch(page -> page.command().length() <= 256),
                "every suggested command stays within supported client command length");
    }

    private static void protectionSurface() throws Exception {
        String protection = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/m3runtime/PrivateSliceProtectionListener.java"));
        for (String required : List.of("BlockBreakEvent", "BlockPlaceEvent", "PlayerBucketEmptyEvent",
                "PlayerInteractEntityEvent", "InventoryClickEvent", "PlayerTeleportEvent", "PlayerMoveEvent",
                "GameMode.ADVENTURE", "beyondClosedGate")) {
            check(protection.contains(required), "protected review runtime missing " + required);
        }
        String interaction = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/m3runtime/PrivateSliceInteractionListener.java"));
        for (String required : List.of("commitObservation", "openEvidenceBook", "openReferenceBook",
                "openFilingLedger", "Presentation.NATIVE_BOOK", "sendActionBar")) {
            check(interaction.contains(required), "player-facing interaction missing " + required);
        }
        check(!interaction.contains("sendMessage("), "evidence and filing feedback must not use chat");
        String runtime = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/m3runtime/PrivateSliceReviewRuntime.java"));
        for (String required : List.of("naiveNegative", "bruteNegative", "counter_proximity_only",
                "lodgeFinding", "suggest_commands=5", "observation_receipts_required=0")) {
            check(runtime.contains(required), "vNext negative/security runtime missing " + required);
        }
        String world = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/m3runtime/PrivateSliceWorld.java"));
        for (String required : List.of("Mouth Copy Inquiry", "addFilingEntry",
                "VISIBLE_ENVIRONMENTAL_RECORD", "nearFilingLedger", "checkWaterworks",
                "checkCorridor", "checkImmersiveText", "occupiedShelfData", "checkSeats",
                "GATE_CLOSED_COLLISION_CELLS = 88")) {
            check(world.contains(required), "vNext authored world gate missing " + required);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIllegalState(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
    }

    private static void expectReportRefusal(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected report refusal"); }
        catch (PrivateSliceState.ReportRefusedException expected) { }
    }

    private static void expectThrottle(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected filing throttle"); }
        catch (PrivateSliceState.FilingThrottleException expected) { }
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
