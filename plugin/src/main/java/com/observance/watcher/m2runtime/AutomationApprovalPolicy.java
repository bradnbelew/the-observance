package com.observance.watcher.m2runtime;

import com.google.gson.JsonObject;
import com.observance.watcher.data.rows.BeatQueueRow;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Fail-closed M2 A0-A5 policy for queued, director-approved effects. */
public final class AutomationApprovalPolicy {
    public enum RiskClass { A0, A1, A2, A3, A4, A5 }

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> APPROVABLE_TYPES =
            Set.of("name_on_wall", "hint_whisper", "whisper_toll");
    private final Clock clock;

    public AutomationApprovalPolicy(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public boolean permitsQueued(BeatQueueRow beat) {
        if (beat == null || beat.type == null) return false;
        String type = beat.type.trim().toLowerCase(Locale.ROOT);
        if (!APPROVABLE_TYPES.contains(type)) return false;
        JsonObject payload = beat.payloadObject();
        if (payload == null) return false;
        try {
            String approvalId = payload.get("approval_id").getAsString();
            String approvalClass = payload.get("approval_class").getAsString();
            String payloadHash = payload.get("authored_payload_sha256").getAsString();
            String scope = payload.get("approval_scope").getAsString();
            Instant expires = Instant.parse(payload.get("approval_expires_at").getAsString());
            if (approvalId.isBlank() || scope.isBlank() || !SHA256.matcher(payloadHash).matches()) return false;
            if (!riskClass(type).name().equals(approvalClass)) return false;
            if (!payloadHash.equals(PredicateAuthorityVersion.semanticSha256(payload.get("authored_payload")))) {
                return false;
            }
            return expires.isAfter(clock.instant());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean mayRunAutomatically(RiskClass riskClass) {
        return riskClass == RiskClass.A0 || riskClass == RiskClass.A1;
    }

    public static RiskClass riskClass(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "health_readback", "receipt_projection" -> RiskClass.A0;
            case "private_sound", "private_particle", "proximity_dim" -> RiskClass.A1;
            case "name_on_wall" -> RiskClass.A2;
            case "hint_whisper", "whisper_toll", "discord_post" -> RiskClass.A3;
            case "world_change", "route_change", "artifact_recovery" -> RiskClass.A4;
            case "choice_commit", "release_commit", "shutdown" -> RiskClass.A5;
            default -> RiskClass.A4;
        };
    }
}
