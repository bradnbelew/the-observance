package com.observance.watcher.beats;

import com.observance.watcher.beat.BeatEnactor;
import com.observance.watcher.config.Site;
import com.observance.watcher.data.rows.BeatQueueRow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * The real {@link BeatEnactor}: turns a queued {@link BeatQueueRow} into a validated
 * {@link BeatRequest}, gates it through the {@link DramaBudget}, and enacts the matching
 * {@link BeatLibrary} beat on the MAIN thread. Refunds budget if the beat doesn't fire.
 *
 * <p>Threading: {@code enact} is called by the foundation poller on the MAIN thread (the poller hops
 * there for us). Everything here is main-thread-safe; the beats themselves schedule any async I/O.
 *
 * <p>Fault isolation: this class never throws — the poller also wraps it in Safety, and we wrap the
 * beat call too. An unknown type returns {@link EnactResult#UNHANDLED} so the poller FAILS the
 * row queued for a future build (never marks a misunderstood beat fired).
 */
public final class RealBeatEnactor implements BeatEnactor {

    private final BeatContext ctx;
    private final BeatLibrary library;
    private final DramaBudget budget;

    public RealBeatEnactor(BeatContext ctx, BeatLibrary library, DramaBudget budget) {
        this.ctx = ctx;
        this.library = library;
        this.budget = budget;
    }

    @Override
    public EnactResult enact(BeatQueueRow row) {
        if (row == null || row.type == null || row.type.isBlank()) {
            return EnactResult.FAILED;
        }
        String type = row.type.trim().toLowerCase(Locale.ROOT);
        Beat beat = library.get(type);
        if (beat == null) {
            // Unknown to this build — leave it queued for a later version (no status write).
            return EnactResult.UNHANDLED;
        }

        // Resolve a normalized request on the main thread (target player + site + payload).
        BeatRequest req = buildRequest(row, beat);

        // Cheap precheck — if it can't fire now, skip WITHOUT consuming budget.
        boolean can = ctx.safety().call("beat.real.canEnact." + type,
                () -> beat.canEnact(ctx, req), Boolean.FALSE);
        if (!Boolean.TRUE.equals(can)) {
            return EnactResult.SKIPPED;
        }

        // Reserve drama budget for the beat's category. DIRECTED bypasses ambient spacing but still
        // counts toward the rolling window cap.
        UUID target = req.targetUuid();
        boolean reserved = budget.tryReserve(beat.category(), target);
        if (!reserved) {
            return EnactResult.SKIPPED; // budget says not now — soft-pressure, try later
        }

        BeatResult result = ctx.safety().call("beat.real.enact." + type,
                () -> beat.enact(ctx, req), BeatResult.failed("threw"));
        if (result == null) result = BeatResult.failed("null");

        // Refund budget if the beat didn't actually fire (a no-op must not burn the rare budget).
        if (result.kind() != BeatResult.Kind.FIRED) {
            budget.refund(beat.category(), target);
        }

        // Audit (async, never throws).
        final BeatResult fr = result;
        ctx.scheduler().runAsyncSafe("beat.real.audit", () ->
                ctx.safety().info("beat.enacted",
                        "type=" + type + " id=" + row.id + " result=" + fr));

        return result.toEnactResult();
    }

    /** MAIN thread: resolve target player + site + parse payload into a BeatRequest. */
    private BeatRequest buildRequest(BeatQueueRow row, Beat beat) {
        Player target = null;
        if (row.mcUuid != null && !row.mcUuid.isBlank()) {
            try {
                UUID uuid = UUID.fromString(row.mcUuid.trim());
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) target = p;
            } catch (IllegalArgumentException ignored) {
                // malformed uuid → no target (world/ambient beat)
            }
        }
        Site site = null;
        if (row.siteId != null && !row.siteId.isBlank() && ctx.sites() != null) {
            site = ctx.sites().get(row.siteId.trim());
        }
        // payload is a jsonb column → JsonElement on the wire. Prefer the object path (no
        // string round-trip); BeatPayload.of(null) degrades to an empty payload.
        BeatPayload payload = BeatPayload.of(row.payloadObject());
        // The category is the beat's own declared category (DIRECTED for queued specials).
        return new BeatRequest(row.id, beat.name(), beat.category(), target, site, payload);
    }
}
