package com.observance.watcher.m2runtime;

import com.google.gson.JsonObject;
import com.observance.watcher.data.rows.BeatQueueRow;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Proves A0/A1 automatic boundary and exact, expiring approval envelopes for A2/A3. */
public final class AutomationApprovalPolicySelfTest {
    private AutomationApprovalPolicySelfTest() { }

    public static void main(String[] args) {
        Instant now = Instant.parse("2026-07-16T00:00:00Z");
        AutomationApprovalPolicy policy = new AutomationApprovalPolicy(Clock.fixed(now, ZoneOffset.UTC));
        check(AutomationApprovalPolicy.mayRunAutomatically(AutomationApprovalPolicy.RiskClass.A0), "A0 auto");
        check(AutomationApprovalPolicy.mayRunAutomatically(AutomationApprovalPolicy.RiskClass.A1), "A1 auto");
        for (AutomationApprovalPolicy.RiskClass risk : new AutomationApprovalPolicy.RiskClass[]{
                AutomationApprovalPolicy.RiskClass.A2, AutomationApprovalPolicy.RiskClass.A3,
                AutomationApprovalPolicy.RiskClass.A4, AutomationApprovalPolicy.RiskClass.A5}) {
            check(!AutomationApprovalPolicy.mayRunAutomatically(risk), risk + " cannot auto");
        }

        BeatQueueRow missing = new BeatQueueRow();
        missing.type = "name_on_wall";
        missing.payload = new JsonObject();
        check(!policy.permitsQueued(missing), "name_on_wall without approval fails closed");
        check(policy.permitsQueued(approved("name_on_wall", "A2", "2026-07-16T00:05:00Z")),
                "exact unexpired A2 approval passes");
        BeatQueueRow changed = approved("name_on_wall", "A2", "2026-07-16T00:05:00Z");
        changed.payload.getAsJsonObject().getAsJsonObject("authored_payload").addProperty("name", "changed");
        check(!policy.permitsQueued(changed), "changed authored bytes fail");
        check(!policy.permitsQueued(approved("name_on_wall", "A3", "2026-07-16T00:05:00Z")),
                "wrong approval class fails");
        check(!policy.permitsQueued(approved("hint_whisper", "A3", "2026-07-15T23:59:59Z")),
                "expired hint approval fails");
        System.out.println("M2 automation approval policy self-test passed");
    }

    private static BeatQueueRow approved(String type, String risk, String expiry) {
        BeatQueueRow row = new BeatQueueRow();
        row.type = type;
        JsonObject payload = new JsonObject();
        JsonObject authored = new JsonObject();
        authored.addProperty("name", "averyn");
        payload.addProperty("approval_id", "approval-1");
        payload.addProperty("approval_class", risk);
        payload.addProperty("approval_scope", "group:test/finding:test");
        payload.addProperty("authored_payload_sha256", PredicateAuthorityVersion.semanticSha256(authored));
        payload.add("authored_payload", authored);
        payload.addProperty("approval_expires_at", expiry);
        row.payload = payload;
        return row;
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
