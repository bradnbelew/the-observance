package com.observance.watcher.beats;

import com.observance.watcher.beat.BeatEnactor;
import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.m2runtime.AutomationApprovalPolicy;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;

/**
 * V5 guard over the real beat enactor. Only an explicit allowlist of directed beat types may enact
 * from the {@code beat_queue}; every other type — including any stale V4 directed row that could
 * still be sitting in the table — is dropped as {@link EnactResult#SKIPPED} without touching the
 * world.
 *
 * <p>This is what lets the V5 production campaign re-enable the queue poller (so the {@code /whisper}
 * candidate directed effects require exact, unexpired approval envelopes whose authored payload
 * hashes match. SKIPPED marks a rejected row decided so it is not re-fetched or retried.
 */
public final class V5SafeBeatEnactor implements BeatEnactor {

    private final BeatEnactor delegate;
    private final Set<String> allowed;
    private final AutomationApprovalPolicy approvalPolicy;

    public V5SafeBeatEnactor(BeatEnactor delegate, Set<String> allowedTypes) {
        this.delegate = delegate;
        this.allowed = Set.copyOf(allowedTypes);
        this.approvalPolicy = new AutomationApprovalPolicy(Clock.systemUTC());
    }

    @Override
    public EnactResult enact(BeatQueueRow beat) {
        if (beat == null || beat.type == null || beat.type.isBlank()) {
            return EnactResult.SKIPPED;
        }
        String type = beat.type.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(type) || !approvalPolicy.permitsQueued(beat)) {
            // Retired or unknown to the V5 campaign — drop it, never enact.
            return EnactResult.SKIPPED;
        }
        return delegate.enact(beat);
    }

    /** Candidate directed types; each row still needs an exact unexpired approval envelope. */
    public static Set<String> v5AllowedTypes() {
        return Set.of("whisper_toll", "hint_whisper", "name_on_wall");
    }
}
