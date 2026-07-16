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

/** Proves M3 any-subset evidence, local gate, replay/catch-up, restart, and exact A2 approval. */
public final class PrivateSliceStateSelfTest {
    private static final String WATCHER_HASH =
            "3a2187bdc752b583d92ae47cb0a718b15c02ea2684b2b8fd2c2c8ccf88d9c10a";

    private PrivateSliceStateSelfTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("observance-m3-slice-");
        Path path = dir.resolve("slice.journal");
        try {
            PrivateSliceState state = PrivateSliceState.open(path);
            check(!state.gateOpen(), "gate begins closed");
            state.commitFinding("P4.F3", List.of("population_board", "ration_ledger"), "brad");
            state.commitFinding("P4.F1", List.of("cart_wear", "drainage_map"), "alice");
            expectIllegalState(() -> state.commitFinding("P4.F5", PrivateSliceState.BASE_FINDINGS, "alice"));
            state.commitFinding("P4.F4", List.of("descent_heat_marks", "founding_minutes"), "bob");
            state.commitFinding("P4.F2", List.of("material_join_civic", "survey_revisions"), "alice");
            long beforeReplay = state.catchUpAfter(0).size();
            state.commitFinding("P4.F2", List.of("survey_revisions", "material_join_civic"), "bob");
            check(state.contributors("P4.F2").equals(java.util.Set.of("alice", "bob")),
                    "contributors are provenance, not eligibility");
            check(state.catchUpAfter(0).size() == beforeReplay + 1, "finding replay adds only new contribution");
            expectIllegalState(() -> state.commitFinding(
                    "P4.F2", List.of("different_source", "survey_revisions"), "carol"));

            long cursor = state.catchUpAfter(0).size();
            state.commitFinding("P4.F5", PrivateSliceState.BASE_FINDINGS, "brad");
            check(state.gateOpen(), "local synthesis opens the gate");
            check(state.catchUpAfter(cursor).stream().anyMatch(receipt -> "gate_opened".equals(receipt.eventType())),
                    "cursor catch-up contains the physical gate receipt");

            PrivateSliceState restarted = PrivateSliceState.open(path);
            check(restarted.gateOpen() && restarted.findingCommitted("P4.F5"),
                    "restart re-derives the committed gate state");
            check(restarted.catchUpAfter(0).size() == state.catchUpAfter(0).size(),
                    "restart does not duplicate receipts");
            approvalBoundary();
            System.out.println("M3 private-slice state self-test passed");
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(dir);
        }
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIllegalState(Throwing action) throws Exception {
        try { action.run(); throw new AssertionError("expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
