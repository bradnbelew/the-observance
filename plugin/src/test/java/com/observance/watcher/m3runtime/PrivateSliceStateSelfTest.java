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
import java.util.List;

/** Proves v5 content-dependent reports, page budgets, negative flows, replay, and exact A2 approval. */
public final class PrivateSliceStateSelfTest {
    private static final String WATCHER_HASH =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";

    private PrivateSliceStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-m3-v5-slice-");
        Path path = dir.resolve("slice.journal");
        try {
            PrivateSliceState state = PrivateSliceState.open(path);
            check(!state.gateOpen(), "gate begins closed");
            selectCorrectReport(state, "no-records");
            expectIllegalState(() -> state.lodgeReport("no-records", 1_789_000_000L));

            observeAllAnySubset(state);
            selectEveryPrintedClause(state, "naive");
            expectReportRefusal(() -> state.lodgeReport("naive", 1_789_000_010L));
            check(PrivateSliceState.BASE_FINDINGS.stream().noneMatch(state::findingCommitted)
                    && !state.gateOpen(), "naive click-through advances nothing");

            PrivateSliceState afterNaiveRestart = PrivateSliceState.open(path);
            check(afterNaiveRestart.refusalCount("naive") == 1,
                    "naive refusal survives restart without solution payload");
            for (int attempt = 0; attempt < PrivateSliceState.MAX_REFUSALS_PER_WINDOW; attempt++) {
                selectWrongReport(afterNaiveRestart, "blind");
                long epoch = 1_789_000_020L + attempt;
                expectReportRefusal(() -> afterNaiveRestart.lodgeReport("blind", epoch));
            }
            selectCorrectReport(afterNaiveRestart, "blind");
            expectThrottle(() -> afterNaiveRestart.lodgeReport("blind", 1_789_000_024L));
            check(PrivateSliceState.BASE_FINDINGS.stream().noneMatch(afterNaiveRestart::findingCommitted),
                    "bounded brute force cannot advance a finding");

            selectCorrectReport(afterNaiveRestart, "brad");
            afterNaiveRestart.lodgeReport("brad", 1_789_001_000L);
            for (String finding : PrivateSliceState.BASE_FINDINGS) {
                check(afterNaiveRestart.findingCommitted(finding), finding + " committed from exact report");
                check(PrivateSliceState.EXACT_CONCLUSIONS.get(finding)
                        .equals(afterNaiveRestart.committedConclusion(finding)),
                        finding + " retains its content-dependent conclusion");
            }
            check(!afterNaiveRestart.gateOpen(), "four-clause report does not bypass synthesis");

            long now = 1_789_001_100L;
            afterNaiveRestart.approveWatcher("brad-a2-v5", "WestReviewer", "EastReviewer", now + 600, now);
            expectIllegalState(() -> afterNaiveRestart.consumeWatcher(
                    "brad-a2-v5", "Wrong", "EastReviewer", now + 1));
            afterNaiveRestart.consumeWatcher("brad-a2-v5", "WestReviewer", "EastReviewer", now + 1);
            expectIllegalState(() -> afterNaiveRestart.consumeWatcher(
                    "brad-a2-v5", "WestReviewer", "EastReviewer", now + 2));

            afterNaiveRestart.selectDraft(PrivateSliceState.SYNTHESIS,
                    PrivateSliceState.CONCLUSION_OPTIONS.get(PrivateSliceState.SYNTHESIS).get(3), "brad");
            expectReportRefusal(() -> afterNaiveRestart.lodgeSynthesis("brad", now + 10));
            check(!afterNaiveRestart.gateOpen(), "wrong synthesis leaves gate closed");
            long cursor = afterNaiveRestart.catchUpAfter(0).size();
            afterNaiveRestart.selectDraft(PrivateSliceState.SYNTHESIS,
                    PrivateSliceState.EXACT_CONCLUSIONS.get(PrivateSliceState.SYNTHESIS), "brad");
            afterNaiveRestart.lodgeSynthesis("brad", now + 11);
            check(afterNaiveRestart.gateOpen(), "exact synthesis opens local gate");
            check(afterNaiveRestart.catchUpAfter(cursor).stream()
                    .anyMatch(receipt -> "gate_opened".equals(receipt.eventType())),
                    "cursor catch-up contains physical gate receipt");

            PrivateSliceState restarted = PrivateSliceState.open(path);
            check(restarted.gateOpen() && restarted.findingCommitted(PrivateSliceState.SYNTHESIS),
                    "restart re-derives committed gate and synthesis");
            check(PrivateSliceState.EXACT_CONCLUSIONS.get(PrivateSliceState.SYNTHESIS)
                    .equals(restarted.committedConclusion(PrivateSliceState.SYNTHESIS)),
                    "restart retains exact synthesis conclusion");
            check(restarted.observedSources("P4.F2").containsAll(List.of("mason_mark", "revision_letter")),
                    "restart preserves any-subset physical observation custody");

            approvalBoundary();
            bookPageBudget();
            protectionSurface();
            System.out.println("M3 private-slice v5 state self-test passed");
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(dir);
        }
    }

    private static void observeAllAnySubset(PrivateSliceState state) throws Exception {
        state.commitObservation("P4.F3", "ration_tally", "sela");
        state.commitObservation("P4.F1", "cart_rut_tag", "orris");
        state.commitObservation("P4.F4", "engineer_letter", "eda");
        state.commitObservation("P4.F2", "mason_mark", "toma");
        state.commitObservation("P4.F1", "drainage_plan", "neri");
        state.commitObservation("P4.F4", "pump_gauge", "iven");
        state.commitObservation("P4.F2", "revision_letter", "eda");
        state.commitObservation("P4.F3", "berth_register", "lio");
    }

    private static void selectEveryPrintedClause(PrivateSliceState state, String contributor) {
        for (String finding : PrivateSliceState.BASE_FINDINGS) {
            for (String option : PrivateSliceState.CONCLUSION_OPTIONS.get(finding)) {
                state.selectDraft(finding, option, contributor);
            }
        }
    }

    private static void selectWrongReport(PrivateSliceState state, String contributor) {
        PrivateSliceState.BASE_FINDINGS.forEach(finding -> state.selectDraft(finding,
                PrivateSliceState.CONCLUSION_OPTIONS.get(finding).get(0), contributor));
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
        List<BookPageLayout.Option> options = List.of(
                new BookPageLayout.Option("A temporary quarry shelter abandoned after one winter.", "a"),
                new BookPageLayout.Option("A planned civic intake for 294, not one emergency shelter.", "b"),
                new BookPageLayout.Option("A private archive with no public refuge role.", "c"),
                new BookPageLayout.Option("A natural cave mistaken for civic works.", "d"));
        List<BookPageLayout.OptionPage> pages = BookPageLayout.optionPages(
                "ACCOUNT SUPPORTED", "P4.F5", "b", options);
        check(pages.size() == 4 && pages.stream().allMatch(page -> page.budget().fits()),
                "every full synthesis clause has its own render-safe page");
        check(pages.stream().map(BookPageLayout.OptionPage::command).distinct().count() == 4,
                "every clause has one unique reachable filing command");
        check(pages.get(1).marker().equals("[X] "), "selected clause remains visibly marked");
    }

    private static void protectionSurface() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/observance/watcher/m3runtime/PrivateSliceProtectionListener.java"));
        for (String required : List.of("BlockBreakEvent", "BlockPlaceEvent", "PlayerBucketEmptyEvent",
                "PlayerInteractEntityEvent", "InventoryClickEvent", "PlayerTeleportEvent", "PlayerMoveEvent",
                "GameMode.ADVENTURE", "beyondClosedGate")) {
            check(source.contains(required), "protected review runtime missing " + required);
        }
        String interaction = Files.readString(Path.of("src/main/java/com/observance/watcher/m3runtime/PrivateSliceInteractionListener.java"));
        for (String required : List.of("commitObservation", "openEvidenceBook", "openReferenceBook",
                "openFilingLedger", "Presentation.NATIVE_BOOK", "sendActionBar")) {
            check(interaction.contains(required), "player-facing interaction missing " + required);
        }
        check(!interaction.contains("sendMessage("), "v5 evidence and filing feedback must not use chat");
        String runtime = Files.readString(Path.of("src/main/java/com/observance/watcher/m3runtime/PrivateSliceReviewRuntime.java"));
        for (String required : List.of("naiveNegative", "bruteNegative", "counter_proximity_only",
                "ReportRefusedException", "FilingThrottleException")) {
            check(runtime.contains(required), "v5 negative/security runtime missing " + required);
        }
        String world = Files.readString(Path.of("src/main/java/com/observance/watcher/m3runtime/PrivateSliceWorld.java"));
        for (String required : List.of("exactly two purpose-specific lecterns", "addChoicePages",
                "VISIBLE_ENVIRONMENTAL_RECORD", "nearFilingLedger", "checkWaterworks",
                "checkCorridor", "checkImmersiveText", "occupiedShelfData", "checkSeats",
                "GATE_CLOSED_COLLISION_CELLS = 88")) {
            check(world.contains(required), "v5 authored world gate missing " + required);
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
