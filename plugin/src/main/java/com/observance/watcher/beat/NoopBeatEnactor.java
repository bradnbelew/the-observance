package com.observance.watcher.beat;

import com.observance.watcher.data.rows.BeatQueueRow;
import com.observance.watcher.util.Safety;

/**
 * Phase-0 placeholder enactor: logs the beat it WOULD enact and reports FIRED so the full
 * poll → decide → mark-fired pipeline can be exercised end-to-end with no real world effect.
 * Replaced by the Haunting-Engine subsystem agent's real enactor.
 */
public final class NoopBeatEnactor implements BeatEnactor {

    private final Safety safety;

    public NoopBeatEnactor(Safety safety) {
        this.safety = safety;
    }

    @Override
    public EnactResult enact(BeatQueueRow beat) {
        if (beat == null) return EnactResult.FAILED;
        safety.info("beat.noop",
                "would enact beat id=" + beat.id + " type=" + beat.type
                        + (beat.mcUuid != null ? (" target=" + beat.mcUuid) : "")
                        + (beat.siteId != null ? (" site=" + beat.siteId) : ""));
        return EnactResult.FIRED;
    }
}
